package com.medibook.appointment.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.appointment.client.SlotClient;
import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.dto.SlotDto;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.BadRequestException;
import com.medibook.appointment.exception.ForbiddenException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.messaging.AppointmentEventPublisher;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.AppointmentService;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SlotClient slotClient;

    @Autowired
    private AppointmentEventPublisher eventPublisher;

    /**
     * bookAppointment creates appointment with status "PENDING_PAYMENT".
     * Slot is NOT booked yet — it will be booked only after payment confirmation
     * via updateStatus("SCHEDULED").
     * For COD: frontend calls updateStatus("SCHEDULED") directly.
     */
    @Override
    @Transactional
    public Appointment bookAppointment(AppointmentRequest request) {

        SlotDto slot = slotClient.getSlotById(request.getSlotId());

        if (slot.isBooked())
            throw new BadRequestException("This slot is already booked. Please choose another slot.");
        if (slot.isBlocked())
            throw new BadRequestException("This slot is blocked by the doctor.");
        if (slot.getProviderId() != request.getProviderId())
            throw new BadRequestException("Slot does not belong to the selected provider.");

        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .patientEmail(request.getPatientEmail())
                .slotId(request.getSlotId())
                .serviceType(request.getServiceType())
                .appointmentDate(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .modeOfConsultation(request.getModeOfConsultation())
                .notes(request.getNotes())
                // Status is PENDING_PAYMENT — slot is still FREE at this point.
                // Slot will be booked only after payment is confirmed.
                .status("PENDING_PAYMENT")
                .build();

        return appointmentRepository.save(appointment);
    }

    @Override
    public Appointment getById(int appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
    }

    @Override
    public List<Appointment> getByPatient(int patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Appointment> getUpcomingByPatient(int patientId) {
        return appointmentRepository.findUpcomingByPatientId(patientId, LocalDate.now());
    }

    @Override
    public List<Appointment> getByProvider(int providerId) {
        return appointmentRepository.findByProviderId(providerId);
    }

    @Override
    public List<Appointment> getByProviderAndDate(int providerId, LocalDate date) {
        return appointmentRepository.findByProviderIdAndAppointmentDate(providerId, date);
    }

    @Override
    @Transactional
    public void cancelAppointment(int appointmentId) {
        Appointment appointment = getById(appointmentId);

        if (appointment.getStatus().equals("COMPLETED"))
            throw new BadRequestException("Cannot cancel a completed appointment.");
        if (appointment.getStatus().equals("CANCELLED"))
            throw new BadRequestException("Appointment is already cancelled.");

        // ✅ BUG FIX: Save previousStatus BEFORE setting to CANCELLED.
        // Old code set status to CANCELLED first, then checked status — always "CANCELLED",
        // so the PENDING_PAYMENT guard never worked and slot was always released even
        // for appointments where payment never completed (slot was never booked).
        String previousStatus = appointment.getStatus();

        appointment.setStatus("CANCELLED");
        Appointment saved = appointmentRepository.save(appointment);

        // Only release slot if it was actually booked (payment had completed).
        // If previousStatus was PENDING_PAYMENT, the slot was never locked — skip release.
        if (!"PENDING_PAYMENT".equals(previousStatus)) {
            slotClient.releaseSlot(appointment.getSlotId());
        }

        try {
            eventPublisher.publishCancelled(saved);
        } catch (Exception e) {
            System.err.println("[RabbitMQ] publishCancelled failed (non-fatal): " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Appointment rescheduleAppointment(int appointmentId, int newSlotId,
            LocalDate newDate, String newStartTime, String newEndTime) {
        Appointment appointment = getById(appointmentId);

        if (!appointment.getStatus().equals("SCHEDULED") && !appointment.getStatus().equals("CONFIRMED"))
            throw new BadRequestException("Only SCHEDULED appointments can be rescheduled.");

        slotClient.releaseSlot(appointment.getSlotId());

        SlotDto newSlot = slotClient.getSlotById(newSlotId);
        if (newSlot.isBooked()) throw new BadRequestException("New slot is already booked.");

        appointment.setSlotId(newSlotId);
        appointment.setAppointmentDate(newSlot.getDate());
        appointment.setStartTime(newSlot.getStartTime());
        appointment.setEndTime(newSlot.getEndTime());

        Appointment saved = appointmentRepository.save(appointment);
        slotClient.bookSlot(newSlotId);
        return saved;
    }

    @Override
    @Transactional
    public void completeAppointment(int appointmentId, int requestingProviderId) {
        Appointment appointment = getById(appointmentId);

        if (appointment.getProviderId() != requestingProviderId) {
            throw new ForbiddenException(
                "You are not authorized to complete this appointment. " +
                "Only the assigned provider can mark it as completed."
            );
        }

        if (!appointment.getStatus().equals("SCHEDULED"))
            throw new BadRequestException("Only SCHEDULED appointments can be marked complete.");

        if (appointment.getStatus().equals("COMPLETED"))
            throw new BadRequestException("Appointment already completed.");
        if (appointment.getStatus().equals("CANCELLED"))
            throw new BadRequestException("Cannot complete a cancelled appointment.");

        appointment.setStatus("COMPLETED");
        Appointment saved = appointmentRepository.save(appointment);

        try {
            eventPublisher.publishCompleted(saved);
        } catch (Exception e) {
            System.err.println("[RabbitMQ] publishCompleted failed (non-fatal): " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void markNoShow(int appointmentId, int requestingProviderId) {
        Appointment appointment = getById(appointmentId);

        if (appointment.getProviderId() != requestingProviderId) {
            throw new ForbiddenException(
                "You are not authorized to mark this appointment as NO_SHOW. " +
                "Only the assigned provider can do this."
            );
        }

        if (appointment.getStatus().equals("COMPLETED"))
            throw new BadRequestException("Cannot mark a completed appointment as NO_SHOW.");
        if (appointment.getStatus().equals("CANCELLED"))
            throw new BadRequestException("Cannot mark a cancelled appointment as NO_SHOW.");
        if (appointment.getStatus().equals("NO_SHOW"))
            throw new BadRequestException("Appointment is already marked as NO_SHOW.");

        appointment.setStatus("NO_SHOW");
        Appointment saved = appointmentRepository.save(appointment);

        try {
            eventPublisher.publishCompleted(saved);
        } catch (Exception e) {
            System.err.println("[RabbitMQ] publishCompleted(NO_SHOW) failed (non-fatal): " + e.getMessage());
        }
    }

    /**
     * updateStatus is the single place where slot booking happens after payment.
     *
     * Called by PaymentServiceImpl after Razorpay payment verification succeeds.
     * Also called directly by frontend for COD (Cash on Delivery) appointments.
     *
     * SCHEDULED / CONFIRMED → payment verified → book slot → publish booked event
     * CANCELLED             → release slot only if it was previously booked
     */
    @Override
    @Transactional
    public void updateStatus(int appointmentId, String status) {
        Appointment appointment = getById(appointmentId);

        if ("SCHEDULED".equals(status) || "CONFIRMED".equals(status)) {

            // Guard: if already SCHEDULED, do not double-book the slot.
            if ("SCHEDULED".equals(appointment.getStatus())) {
                System.out.println("[updateStatus] Appointment " + appointmentId
                        + " is already SCHEDULED. Skipping duplicate slot booking.");
                return;
            }

            // Payment confirmed — NOW book the slot and mark appointment SCHEDULED.
            appointment.setStatus("SCHEDULED");
            appointmentRepository.save(appointment);

            // Book the slot only here — never in bookAppointment().
            slotClient.bookSlot(appointment.getSlotId());

            try {
                eventPublisher.publishBooked(appointment);
            } catch (Exception e) {
                System.err.println("[RabbitMQ] publishBooked failed (non-fatal): " + e.getMessage());
            }

        } else if ("CANCELLED".equals(status)) {

            // ✅ BUG FIX: Save previousStatus BEFORE setting to CANCELLED.
            // Same bug as cancelAppointment() — must capture status before overwriting.
            String previousStatus = appointment.getStatus();

            appointment.setStatus("CANCELLED");
            appointmentRepository.save(appointment);

            // Only release slot if it was actually booked.
            if (!"PENDING_PAYMENT".equals(previousStatus)) {
                slotClient.releaseSlot(appointment.getSlotId());
            }

            try {
                eventPublisher.publishCancelled(appointment);
            } catch (Exception e) {
                System.err.println("[RabbitMQ] publishCancelled failed (non-fatal): " + e.getMessage());
            }

        } else {
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
        }
    }

    @Override
    public int getAppointmentCount(int providerId) {
        return (int) appointmentRepository.countByProviderId(providerId);
    }
}