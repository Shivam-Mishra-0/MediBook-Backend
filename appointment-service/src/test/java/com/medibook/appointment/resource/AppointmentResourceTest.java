package com.medibook.appointment.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.BadRequestException;
import com.medibook.appointment.exception.ForbiddenException;
import com.medibook.appointment.exception.GlobalExceptionHandler;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentResource Controller Tests")
class AppointmentResourceTest {

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentResource appointmentResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(appointmentResource)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private Appointment buildAppointment(int id, String status) {
        Appointment a = new Appointment();
        a.setAppointmentId(id);
        a.setPatientId(1);
        a.setProviderId(5);
        a.setSlotId(10);
        a.setServiceType("General Consultation");
        a.setAppointmentDate(LocalDate.of(2026, 6, 15));
        a.setStartTime(LocalTime.of(10, 0));
        a.setEndTime(LocalTime.of(10, 30));
        a.setModeOfConsultation("IN_PERSON");
        a.setStatus(status);
        return a;
    }

    private AppointmentRequest buildRequest() {
        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(1);
        req.setProviderId(5);
        req.setSlotId(10);
        req.setServiceType("General Consultation");
        req.setAppointmentDate(LocalDate.of(2026, 6, 15));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(10, 30));
        req.setModeOfConsultation("IN_PERSON");
        return req;
    }

    // ── POST /appointments/book ───────────────────────────────────────────────

    @Nested
    @DisplayName("POST /appointments/book")
    class BookAppointment {

        @Test
        @DisplayName("Should return 201 with appointment details on successful booking")
        void shouldReturn201OnSuccessfulBooking() throws Exception {
            AppointmentRequest req = buildRequest();
            Appointment saved = buildAppointment(100, "PENDING_PAYMENT");
            when(appointmentService.bookAppointment(org.mockito.ArgumentMatchers.any(AppointmentRequest.class))).thenReturn(saved);

            mockMvc.perform(post("/appointments/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentId").value(100))
                    .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                    .andExpect(jsonPath("$.message").value("Appointment booked successfully."));
        }

        @Test
        @DisplayName("Should return 400 when slot is already booked")
        void shouldReturn400WhenSlotAlreadyBooked() throws Exception {
            when(appointmentService.bookAppointment(org.mockito.ArgumentMatchers.any())).thenThrow(new BadRequestException("already booked"));

            mockMvc.perform(post("/appointments/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 on validation failure (missing required fields)")
        void shouldReturn400OnValidationFailure() throws Exception {
            // Empty request — all required fields missing
            mockMvc.perform(post("/appointments/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /appointments/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /appointments/{appointmentId}")
    class GetById {

        @Test
        @DisplayName("Should return 200 with appointment when found")
        void shouldReturn200WhenFound() throws Exception {
            Appointment appt = buildAppointment(1, "SCHEDULED");
            when(appointmentService.getById(1)).thenReturn(appt);

            mockMvc.perform(get("/appointments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentId").value(1))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"));
        }

        @Test
        @DisplayName("Should return 404 when appointment not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(appointmentService.getById(999))
                    .thenThrow(new ResourceNotFoundException("Appointment", "id", 999));

            mockMvc.perform(get("/appointments/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /appointments/patient/{patientId} ─────────────────────────────────

    @Nested
    @DisplayName("GET /appointments/patient/{patientId}")
    class GetByPatient {

        @Test
        @DisplayName("Should return 200 with list of appointments for patient")
        void shouldReturnAppointmentsForPatient() throws Exception {
            when(appointmentService.getByPatient(1)).thenReturn(List.of(buildAppointment(1, "SCHEDULED")));

            mockMvc.perform(get("/appointments/patient/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Should return upcoming appointments for patient")
        void shouldReturnUpcomingAppointments() throws Exception {
            when(appointmentService.getUpcomingByPatient(1)).thenReturn(List.of(buildAppointment(2, "SCHEDULED")));

            mockMvc.perform(get("/appointments/patient/1/upcoming"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // ── GET /appointments/provider/{providerId} ───────────────────────────────

    @Nested
    @DisplayName("GET /appointments/provider/{providerId}")
    class GetByProvider {

        @Test
        @DisplayName("Should return appointments for provider")
        void shouldReturnAppointmentsForProvider() throws Exception {
            when(appointmentService.getByProvider(5)).thenReturn(List.of(buildAppointment(1, "SCHEDULED")));

            mockMvc.perform(get("/appointments/provider/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Should return appointments for provider on a specific date")
        void shouldReturnAppointmentsByProviderAndDate() throws Exception {
            when(appointmentService.getByProviderAndDate(eq(5), org.mockito.ArgumentMatchers.any(LocalDate.class)))
                    .thenReturn(List.of(buildAppointment(1, "SCHEDULED")));

            mockMvc.perform(get("/appointments/provider/5/date").param("date", "2026-06-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // ── PUT /appointments/{id}/cancel ─────────────────────────────────────────

    @Nested
    @DisplayName("PUT /appointments/{appointmentId}/cancel")
    class CancelAppointment {

        @Test
        @DisplayName("Should return 200 on successful cancellation")
        void shouldReturn200OnCancel() throws Exception {
            doNothing().when(appointmentService).cancelAppointment(1);

            mockMvc.perform(put("/appointments/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("cancelled")));
        }

        @Test
        @DisplayName("Should return 400 when trying to cancel a completed appointment")
        void shouldReturn400WhenCancellingCompleted() throws Exception {
            doThrow(new BadRequestException("Cannot cancel a completed appointment."))
                    .when(appointmentService).cancelAppointment(1);

            mockMvc.perform(put("/appointments/1/cancel"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── PUT /appointments/{id}/reschedule ─────────────────────────────────────

    @Nested
    @DisplayName("PUT /appointments/{appointmentId}/reschedule")
    class RescheduleAppointment {

        @Test
        @DisplayName("Should return 200 with updated appointment")
        void shouldReturn200OnReschedule() throws Exception {
            Appointment updated = buildAppointment(1, "SCHEDULED");
            updated.setSlotId(20);
            when(appointmentService.rescheduleAppointment(
                    eq(1),
                    eq(20),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()))
                    .thenReturn(updated);

            Map<String, String> body = Map.of(
                    "newSlotId", "20",
                    "newDate", "2026-07-01",
                    "newStartTime", "11:00",
                    "newEndTime", "11:30"
            );

            mockMvc.perform(put("/appointments/1/reschedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slotId").value(20));
        }
    }

    // ── PUT /appointments/{id}/complete ──────────────────────────────────────

    @Nested
    @DisplayName("PUT /appointments/{appointmentId}/complete")
    class CompleteAppointment {

        @Test
        @DisplayName("Should return 200 on successful completion")
        void shouldReturn200OnCompletion() throws Exception {
            doNothing().when(appointmentService).completeAppointment(1, 5);

            mockMvc.perform(put("/appointments/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"providerId\": 5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("completed")));
        }

        @Test
        @DisplayName("Should return 400 when providerId is missing from request body")
        void shouldReturn400WhenProviderIdMissing() throws Exception {
            mockMvc.perform(put("/appointments/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("providerId is required in request body."));
        }

        @Test
        @DisplayName("Should return 400 when body is null/absent")
        void shouldReturn400WhenBodyIsNull() throws Exception {
            mockMvc.perform(put("/appointments/1/complete")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when wrong provider tries to complete")
        void shouldReturn403WhenForbidden() throws Exception {
            doThrow(new ForbiddenException("not authorized"))
                    .when(appointmentService).completeAppointment(1, 99);

            mockMvc.perform(put("/appointments/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"providerId\": 99}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /appointments/{id}/no-show ────────────────────────────────────────

    @Nested
    @DisplayName("PUT /appointments/{appointmentId}/no-show")
    class MarkNoShow {

        @Test
        @DisplayName("Should return 200 on successful no-show marking")
        void shouldReturn200OnNoShow() throws Exception {
            doNothing().when(appointmentService).markNoShow(1, 5);

            mockMvc.perform(put("/appointments/1/no-show")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"providerId\": 5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("NO_SHOW")));
        }

        @Test
        @DisplayName("Should return 400 when providerId is missing")
        void shouldReturn400WhenProviderIdMissing() throws Exception {
            mockMvc.perform(put("/appointments/1/no-show")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("providerId is required in request body."));
        }

        @Test
        @DisplayName("Should return 403 when wrong provider")
        void shouldReturn403WhenForbidden() throws Exception {
            doThrow(new ForbiddenException("not authorized"))
                    .when(appointmentService).markNoShow(1, 99);

            mockMvc.perform(put("/appointments/1/no-show")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"providerId\": 99}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /appointments/{id}/status ─────────────────────────────────────────

    @Nested
    @DisplayName("PUT /appointments/{appointmentId}/status")
    class UpdateStatus {

        @Test
        @DisplayName("Should return 200 with updated status message")
        void shouldReturn200OnStatusUpdate() throws Exception {
            doNothing().when(appointmentService).updateStatus(1, "NO_SHOW");

            mockMvc.perform(put("/appointments/1/status").param("status", "NO_SHOW"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("NO_SHOW")));
        }
    }

    // ── GET /appointments/provider/{providerId}/count ─────────────────────────

    @Nested
    @DisplayName("GET /appointments/provider/{providerId}/count")
    class GetCount {

        @Test
        @DisplayName("Should return appointment count for provider")
        void shouldReturnCountForProvider() throws Exception {
            when(appointmentService.getAppointmentCount(5)).thenReturn(12);

            mockMvc.perform(get("/appointments/provider/5/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.providerId").value(5))
                    .andExpect(jsonPath("$.totalAppointments").value(12));
        }
    }
}
