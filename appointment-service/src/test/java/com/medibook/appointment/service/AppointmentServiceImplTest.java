package com.medibook.appointment.service;

import com.medibook.appointment.client.SlotClient;
import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.dto.SlotDto;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.BadRequestException;
import com.medibook.appointment.exception.ForbiddenException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.messaging.AppointmentEventPublisher;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl Tests")
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SlotClient slotClient;

    @Mock
    private AppointmentEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SlotDto buildSlot(boolean isBooked, boolean isBlocked, int providerId) {
        SlotDto slot = new SlotDto();
        slot.setSlotId(10);
        slot.setProviderId(providerId);
        slot.setDate(LocalDate.of(2026, 6, 15));
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(10, 30));
        slot.setBooked(isBooked);
        slot.setBlocked(isBlocked);
        return slot;
    }

    private AppointmentRequest buildRequest() {
        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(1);
        req.setProviderId(5);
        req.setPatientEmail("patient@test.com");
        req.setSlotId(10);
        req.setServiceType("General Consultation");
        req.setAppointmentDate(LocalDate.of(2026, 6, 15));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(10, 30));
        req.setModeOfConsultation("IN_PERSON");
        req.setNotes("Test notes");
        return req;
    }

    private Appointment buildAppointment(int id, String status) {
        Appointment a = new Appointment();
        a.setAppointmentId(id);
        a.setPatientId(1);
        a.setProviderId(5);
        a.setPatientEmail("patient@test.com");
        a.setSlotId(10);
        a.setServiceType("General Consultation");
        a.setAppointmentDate(LocalDate.of(2026, 6, 15));
        a.setStartTime(LocalTime.of(10, 0));
        a.setEndTime(LocalTime.of(10, 30));
        a.setModeOfConsultation("IN_PERSON");
        a.setStatus(status);
        return a;
    }

    // ── bookAppointment ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("bookAppointment()")
    class BookAppointment {

        @Test
        @DisplayName("Should create appointment with PENDING_PAYMENT when slot is valid")
        void shouldBookAppointmentSuccessfully() {
            SlotDto slot = buildSlot(false, false, 5);
            AppointmentRequest req = buildRequest();
            Appointment saved = buildAppointment(100, "PENDING_PAYMENT");

            when(slotClient.getSlotById(10)).thenReturn(slot);
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

            Appointment result = appointmentService.bookAppointment(req);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PENDING_PAYMENT");
            verify(appointmentRepository).save(any(Appointment.class));
            // Slot must NOT be booked at this stage
            verify(slotClient, never()).bookSlot(anyInt());
        }

        @Test
        @DisplayName("Should throw BadRequestException when slot is already booked")
        void shouldThrowWhenSlotAlreadyBooked() {
            SlotDto slot = buildSlot(true, false, 5);
            when(slotClient.getSlotById(10)).thenReturn(slot);

            assertThatThrownBy(() -> appointmentService.bookAppointment(buildRequest()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already booked");
        }

        @Test
        @DisplayName("Should throw BadRequestException when slot is blocked")
        void shouldThrowWhenSlotBlocked() {
            SlotDto slot = buildSlot(false, true, 5);
            when(slotClient.getSlotById(10)).thenReturn(slot);

            assertThatThrownBy(() -> appointmentService.bookAppointment(buildRequest()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("blocked");
        }

        @Test
        @DisplayName("Should throw BadRequestException when slot provider does not match")
        void shouldThrowWhenProviderMismatch() {
            SlotDto slot = buildSlot(false, false, 99); // wrong provider
            when(slotClient.getSlotById(10)).thenReturn(slot);

            assertThatThrownBy(() -> appointmentService.bookAppointment(buildRequest()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Should set slot date, startTime, endTime from SlotDto")
        void shouldUseSlotDetailsForAppointment() {
            SlotDto slot = buildSlot(false, false, 5);
            slot.setDate(LocalDate.of(2026, 7, 20));
            slot.setStartTime(LocalTime.of(9, 0));
            slot.setEndTime(LocalTime.of(9, 30));

            AppointmentRequest req = buildRequest();
            when(slotClient.getSlotById(10)).thenReturn(slot);
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Appointment result = appointmentService.bookAppointment(req);

            assertThat(result.getAppointmentDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(result.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.getEndTime()).isEqualTo(LocalTime.of(9, 30));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Should return appointment when found")
        void shouldReturnAppointmentById() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            Appointment result = appointmentService.getById(1);

            assertThat(result).isEqualTo(appt);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when appointment not found")
        void shouldThrowWhenNotFound() {
            when(appointmentRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.getById(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ── getByPatient ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByPatient() / getUpcomingByPatient()")
    class GetByPatient {

        @Test
        @DisplayName("Should return all appointments for patient")
        void shouldReturnAllAppointmentsByPatient() {
            List<Appointment> list = List.of(buildAppointment(1, "SCHEDULED"));
            when(appointmentRepository.findByPatientId(1)).thenReturn(list);

            assertThat(appointmentService.getByPatient(1)).isEqualTo(list);
        }

        @Test
        @DisplayName("Should return upcoming appointments for patient")
        void shouldReturnUpcomingAppointments() {
            List<Appointment> list = List.of(buildAppointment(2, "SCHEDULED"));
            when(appointmentRepository.findUpcomingByPatientId(eq(1), any(LocalDate.class))).thenReturn(list);

            assertThat(appointmentService.getUpcomingByPatient(1)).isEqualTo(list);
        }
    }

    // ── getByProvider ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByProvider() / getByProviderAndDate()")
    class GetByProvider {

        @Test
        @DisplayName("Should return all appointments for provider")
        void shouldReturnAppointmentsByProvider() {
            List<Appointment> list = List.of(buildAppointment(1, "SCHEDULED"));
            when(appointmentRepository.findByProviderId(5)).thenReturn(list);

            assertThat(appointmentService.getByProvider(5)).isEqualTo(list);
        }

        @Test
        @DisplayName("Should return appointments for provider on specific date")
        void shouldReturnAppointmentsByProviderAndDate() {
            LocalDate date = LocalDate.of(2026, 6, 15);
            List<Appointment> list = List.of(buildAppointment(1, "SCHEDULED"));
            when(appointmentRepository.findByProviderIdAndAppointmentDate(5, date)).thenReturn(list);

            assertThat(appointmentService.getByProviderAndDate(5, date)).isEqualTo(list);
        }
    }

    // ── cancelAppointment ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelAppointment()")
    class CancelAppointment {

        @Test
        @DisplayName("Should cancel SCHEDULED appointment and release slot")
        void shouldCancelScheduledAppointment() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.cancelAppointment(1);

            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
            verify(slotClient).releaseSlot(10);
            verify(eventPublisher).publishCancelled(appt);
        }

        @Test
        @DisplayName("Should cancel PENDING_PAYMENT appointment WITHOUT releasing slot")
        void shouldCancelPendingPaymentWithoutReleasingSlot() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.cancelAppointment(1);

            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
            verify(slotClient, never()).releaseSlot(anyInt());
        }

        @Test
        @DisplayName("Should throw BadRequestException when appointment already COMPLETED")
        void shouldThrowWhenAlreadyCompleted() {
            Appointment appt = buildAppointment(1, "COMPLETED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.cancelAppointment(1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("Should throw BadRequestException when appointment already CANCELLED")
        void shouldThrowWhenAlreadyCancelled() {
            Appointment appt = buildAppointment(1, "CANCELLED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.cancelAppointment(1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("Should still complete cancel even if event publisher throws")
        void shouldHandlePublisherException() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);
            doThrow(new RuntimeException("RabbitMQ down")).when(eventPublisher).publishCancelled(any());

            // Should NOT throw — publisher failure is non-fatal
            assertThatCode(() -> appointmentService.cancelAppointment(1))
                    .doesNotThrowAnyException();
        }
    }

    // ── rescheduleAppointment ─────────────────────────────────────────────────

    @Nested
    @DisplayName("rescheduleAppointment()")
    class RescheduleAppointment {

        @Test
        @DisplayName("Should reschedule SCHEDULED appointment to new slot")
        void shouldRescheduleScheduledAppointment() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            SlotDto newSlot = buildSlot(false, false, 5);
            newSlot.setSlotId(20);
            newSlot.setDate(LocalDate.of(2026, 7, 1));
            newSlot.setStartTime(LocalTime.of(11, 0));
            newSlot.setEndTime(LocalTime.of(11, 30));

            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(slotClient.getSlotById(20)).thenReturn(newSlot);
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Appointment result = appointmentService.rescheduleAppointment(
                    1, 20, LocalDate.of(2026, 7, 1), "11:00", "11:30");

            assertThat(result.getSlotId()).isEqualTo(20);
            assertThat(result.getAppointmentDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            verify(slotClient).releaseSlot(10);   // old slot released
            verify(slotClient).bookSlot(20);       // new slot booked
        }

        @Test
        @DisplayName("Should reschedule CONFIRMED appointment")
        void shouldRescheduleConfirmedAppointment() {
            Appointment appt = buildAppointment(1, "CONFIRMED");
            SlotDto newSlot = buildSlot(false, false, 5);
            newSlot.setSlotId(21);
            newSlot.setDate(LocalDate.of(2026, 7, 5));
            newSlot.setStartTime(LocalTime.of(14, 0));
            newSlot.setEndTime(LocalTime.of(14, 30));

            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(slotClient.getSlotById(21)).thenReturn(newSlot);
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Appointment result = appointmentService.rescheduleAppointment(
                    1, 21, LocalDate.of(2026, 7, 5), "14:00", "14:30");

            assertThat(result.getSlotId()).isEqualTo(21);
        }

        @Test
        @DisplayName("Should throw BadRequestException when status is PENDING_PAYMENT")
        void shouldThrowWhenStatusIsPendingPayment() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() ->
                    appointmentService.rescheduleAppointment(1, 20, LocalDate.now(), "10:00", "10:30"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("SCHEDULED");
        }

        @Test
        @DisplayName("Should throw BadRequestException when new slot is already booked")
        void shouldThrowWhenNewSlotAlreadyBooked() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            SlotDto newSlot = buildSlot(true, false, 5);
            newSlot.setSlotId(20);

            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(slotClient.getSlotById(20)).thenReturn(newSlot);

            assertThatThrownBy(() ->
                    appointmentService.rescheduleAppointment(1, 20, LocalDate.now(), "10:00", "10:30"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already booked");
        }
    }

    // ── completeAppointment ───────────────────────────────────────────────────

    @Nested
    @DisplayName("completeAppointment()")
    class CompleteAppointment {

        @Test
        @DisplayName("Should complete SCHEDULED appointment by correct provider")
        void shouldCompleteAppointmentSuccessfully() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.completeAppointment(1, 5);

            assertThat(appt.getStatus()).isEqualTo("COMPLETED");
            verify(eventPublisher).publishCompleted(appt);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when wrong provider tries to complete")
        void shouldThrowWhenWrongProvider() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.completeAppointment(1, 99))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("Should throw BadRequestException when appointment is not SCHEDULED")
        void shouldThrowWhenNotScheduled() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.completeAppointment(1, 5))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("SCHEDULED");
        }

        @Test
        @DisplayName("Should handle publisher failure gracefully")
        void shouldHandlePublisherFailureGracefully() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);
            doThrow(new RuntimeException("RabbitMQ down")).when(eventPublisher).publishCompleted(any());

            assertThatCode(() -> appointmentService.completeAppointment(1, 5))
                    .doesNotThrowAnyException();
        }
    }

    // ── markNoShow ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markNoShow()")
    class MarkNoShow {

        @Test
        @DisplayName("Should mark SCHEDULED appointment as NO_SHOW by correct provider")
        void shouldMarkNoShowSuccessfully() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.markNoShow(1, 5);

            assertThat(appt.getStatus()).isEqualTo("NO_SHOW");
            verify(eventPublisher).publishCompleted(appt);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when wrong provider")
        void shouldThrowWhenWrongProvider() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.markNoShow(1, 99))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("Should throw BadRequestException when appointment already COMPLETED")
        void shouldThrowWhenCompleted() {
            Appointment appt = buildAppointment(1, "COMPLETED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.markNoShow(1, 5))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("Should throw BadRequestException when appointment already CANCELLED")
        void shouldThrowWhenCancelled() {
            Appointment appt = buildAppointment(1, "CANCELLED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.markNoShow(1, 5))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cancelled");
        }

        @Test
        @DisplayName("Should throw BadRequestException when appointment already NO_SHOW")
        void shouldThrowWhenAlreadyNoShow() {
            Appointment appt = buildAppointment(1, "NO_SHOW");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() -> appointmentService.markNoShow(1, 5))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already marked");
        }

        @Test
        @DisplayName("Should handle publisher failure gracefully")
        void shouldHandlePublisherFailureGracefully() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);
            doThrow(new RuntimeException("RabbitMQ down")).when(eventPublisher).publishCompleted(any());

            assertThatCode(() -> appointmentService.markNoShow(1, 5))
                    .doesNotThrowAnyException();
        }
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("Should book slot and publish event when status is SCHEDULED")
        void shouldScheduleAppointmentAndBookSlot() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.updateStatus(1, "SCHEDULED");

            assertThat(appt.getStatus()).isEqualTo("SCHEDULED");
            verify(slotClient).bookSlot(10);
            verify(eventPublisher).publishBooked(appt);
        }

        @Test
        @DisplayName("Should book slot and publish event when status is CONFIRMED")
        void shouldHandleConfirmedStatusSameAsScheduled() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.updateStatus(1, "CONFIRMED");

            assertThat(appt.getStatus()).isEqualTo("SCHEDULED");
            verify(slotClient).bookSlot(10);
        }

        @Test
        @DisplayName("Should skip slot booking if appointment is already SCHEDULED (idempotency)")
        void shouldSkipIfAlreadyScheduled() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));

            appointmentService.updateStatus(1, "SCHEDULED");

            verify(slotClient, never()).bookSlot(anyInt());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should cancel and release slot when previous status was SCHEDULED")
        void shouldCancelAndReleaseSlotWhenPreviouslyScheduled() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.updateStatus(1, "CANCELLED");

            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
            verify(slotClient).releaseSlot(10);
            verify(eventPublisher).publishCancelled(appt);
        }

        @Test
        @DisplayName("Should cancel WITHOUT releasing slot when previous status was PENDING_PAYMENT")
        void shouldCancelWithoutReleasingSlotWhenPendingPayment() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.updateStatus(1, "CANCELLED");

            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
            verify(slotClient, never()).releaseSlot(anyInt());
        }

        @Test
        @DisplayName("Should set status directly for other statuses (e.g. NO_SHOW)")
        void shouldSetStatusDirectlyForOtherStatuses() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);

            appointmentService.updateStatus(1, "NO_SHOW");

            assertThat(appt.getStatus()).isEqualTo("NO_SHOW");
            verify(appointmentRepository).save(appt);
        }

        @Test
        @DisplayName("Should handle publishBooked failure gracefully")
        void shouldHandlePublishBookedFailureGracefully() {
            Appointment appt = buildAppointment(1, "PENDING_PAYMENT");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);
            doThrow(new RuntimeException("Broker unavailable")).when(eventPublisher).publishBooked(any());

            assertThatCode(() -> appointmentService.updateStatus(1, "SCHEDULED"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle publishCancelled failure gracefully in updateStatus")
        void shouldHandlePublishCancelledFailureGracefully() {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentRepository.findById(1)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any())).thenReturn(appt);
            doThrow(new RuntimeException("Broker unavailable")).when(eventPublisher).publishCancelled(any());

            assertThatCode(() -> appointmentService.updateStatus(1, "CANCELLED"))
                    .doesNotThrowAnyException();
        }
    }

    // ── getAppointmentCount ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getAppointmentCount()")
    class GetAppointmentCount {

        @Test
        @DisplayName("Should return appointment count for provider")
        void shouldReturnCountForProvider() {
            when(appointmentRepository.countByProviderId(5)).thenReturn(7L);

            int count = appointmentService.getAppointmentCount(5);

            assertThat(count).isEqualTo(7);
        }

        @Test
        @DisplayName("Should return 0 when provider has no appointments")
        void shouldReturnZeroWhenNoAppointments() {
            when(appointmentRepository.countByProviderId(99)).thenReturn(0L);

            int count = appointmentService.getAppointmentCount(99);

            assertThat(count).isEqualTo(0);
        }
    }
}
