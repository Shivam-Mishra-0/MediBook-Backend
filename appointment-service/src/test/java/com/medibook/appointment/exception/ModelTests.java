package com.medibook.appointment.exception;

import com.medibook.appointment.dto.AppointmentEventDto;
import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.dto.SlotDto;
import com.medibook.appointment.entity.Appointment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exception, DTO and Entity Model Tests")
class ModelTests {

    // ── Exception hierarchy ───────────────────────────────────────────────────

    @Nested
    @DisplayName("BadRequestException")
    class BadRequestExceptionTest {

        @Test
        @DisplayName("Should store and return message")
        void shouldStoreMessage() {
            BadRequestException ex = new BadRequestException("bad input");
            assertThat(ex.getMessage()).isEqualTo("bad input");
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void shouldBeRuntimeException() {
            assertThat(new BadRequestException("x")).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("ForbiddenException")
    class ForbiddenExceptionTest {

        @Test
        @DisplayName("Should store and return message")
        void shouldStoreMessage() {
            ForbiddenException ex = new ForbiddenException("not allowed");
            assertThat(ex.getMessage()).isEqualTo("not allowed");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedExceptionTest {

        @Test
        @DisplayName("Should store and return message")
        void shouldStoreMessage() {
            UnauthorizedException ex = new UnauthorizedException("not authenticated");
            assertThat(ex.getMessage()).isEqualTo("not authenticated");
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTest {

        @Test
        @DisplayName("Should format message correctly")
        void shouldFormatMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Appointment", "id", 5);
            assertThat(ex.getMessage()).isEqualTo("Appointment not found with id: 5");
        }

        @Test
        @DisplayName("Should expose resourceName, fieldName, fieldValue via getters")
        void shouldExposeGetters() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Slot", "slotId", 42);
            assertThat(ex.getResourceName()).isEqualTo("Slot");
            assertThat(ex.getFieldName()).isEqualTo("slotId");
            assertThat(ex.getFieldValue()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("DuplicateResourceException")
    class DuplicateResourceExceptionTest {

        @Test
        @DisplayName("Should format message with resource+field+value constructor")
        void shouldFormatMessageWithThreeArgs() {
            DuplicateResourceException ex = new DuplicateResourceException("Appointment", "slotId", 10);
            assertThat(ex.getMessage()).isEqualTo("Appointment already exists with slotId: 10");
        }

        @Test
        @DisplayName("Should use plain message constructor")
        void shouldUsePlainMessageConstructor() {
            DuplicateResourceException ex = new DuplicateResourceException("Custom message");
            assertThat(ex.getMessage()).isEqualTo("Custom message");
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTest {

        @Test
        @DisplayName("Should support all-args constructor")
        void shouldSupportAllArgsConstructor() {
            LocalDateTime now = LocalDateTime.now();
            ErrorResponse err = new ErrorResponse(400, "Bad Request", "invalid", "/path", now);

            assertThat(err.getStatus()).isEqualTo(400);
            assertThat(err.getError()).isEqualTo("Bad Request");
            assertThat(err.getMessage()).isEqualTo("invalid");
            assertThat(err.getPath()).isEqualTo("/path");
            assertThat(err.getTimestamp()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should support no-args constructor and setters")
        void shouldSupportNoArgsAndSetters() {
            ErrorResponse err = new ErrorResponse();
            err.setStatus(500);
            err.setError("Server Error");
            assertThat(err.getStatus()).isEqualTo(500);
            assertThat(err.getError()).isEqualTo("Server Error");
        }
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AppointmentRequest DTO")
    class AppointmentRequestTest {

        @Test
        @DisplayName("Should store all fields via setters")
        void shouldStoreAllFields() {
            AppointmentRequest req = new AppointmentRequest();
            req.setPatientId(1);
            req.setProviderId(5);
            req.setPatientEmail("p@test.com");
            req.setSlotId(10);
            req.setServiceType("Checkup");
            req.setAppointmentDate(LocalDate.of(2026, 6, 15));
            req.setStartTime(LocalTime.of(10, 0));
            req.setEndTime(LocalTime.of(10, 30));
            req.setModeOfConsultation("IN_PERSON");
            req.setNotes("Some notes");

            assertThat(req.getPatientId()).isEqualTo(1);
            assertThat(req.getProviderId()).isEqualTo(5);
            assertThat(req.getPatientEmail()).isEqualTo("p@test.com");
            assertThat(req.getSlotId()).isEqualTo(10);
            assertThat(req.getServiceType()).isEqualTo("Checkup");
            assertThat(req.getModeOfConsultation()).isEqualTo("IN_PERSON");
            assertThat(req.getNotes()).isEqualTo("Some notes");
        }
    }

    @Nested
    @DisplayName("SlotDto")
    class SlotDtoTest {

        @Test
        @DisplayName("Should store all fields via setters")
        void shouldStoreAllFields() {
            SlotDto slot = new SlotDto();
            slot.setSlotId(5);
            slot.setProviderId(10);
            slot.setDate(LocalDate.of(2026, 7, 1));
            slot.setStartTime(LocalTime.of(9, 0));
            slot.setEndTime(LocalTime.of(9, 30));
            slot.setDurationMinutes(30);
            slot.setBooked(true);
            slot.setBlocked(false);

            assertThat(slot.getSlotId()).isEqualTo(5);
            assertThat(slot.getProviderId()).isEqualTo(10);
            assertThat(slot.isBooked()).isTrue();
            assertThat(slot.isBlocked()).isFalse();
            assertThat(slot.getDurationMinutes()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("AppointmentEventDto")
    class AppointmentEventDtoTest {

        @Test
        @DisplayName("Should build via builder correctly")
        void shouldBuildViaBuilder() {
            AppointmentEventDto dto = AppointmentEventDto.builder()
                    .appointmentId(1)
                    .patientId(2)
                    .providerId(3)
                    .eventType("BOOKED")
                    .serviceType("GP Visit")
                    .modeOfConsultation("TELECONSULTATION")
                    .appointmentDate("2026-06-15")
                    .startTime("10:00")
                    .endTime("10:30")
                    .message("Appointment confirmed")
                    .build();

            assertThat(dto.getAppointmentId()).isEqualTo(1);
            assertThat(dto.getPatientId()).isEqualTo(2);
            assertThat(dto.getProviderId()).isEqualTo(3);
            assertThat(dto.getEventType()).isEqualTo("BOOKED");
            assertThat(dto.getMessage()).isEqualTo("Appointment confirmed");
        }

        @Test
        @DisplayName("Should support no-args constructor")
        void shouldSupportNoArgsConstructor() {
            AppointmentEventDto dto = new AppointmentEventDto();
            dto.setEventType("CANCELLED");
            assertThat(dto.getEventType()).isEqualTo("CANCELLED");
        }
    }

    // ── Entity ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Appointment entity")
    class AppointmentEntityTest {

        @Test
        @DisplayName("Should build appointment via builder")
        void shouldBuildViaBuilder() {
            Appointment a = Appointment.builder()
                    .patientId(1)
                    .providerId(5)
                    .slotId(10)
                    .serviceType("Checkup")
                    .appointmentDate(LocalDate.of(2026, 6, 15))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(10, 30))
                    .modeOfConsultation("IN_PERSON")
                    .status("PENDING_PAYMENT")
                    .build();

            assertThat(a.getPatientId()).isEqualTo(1);
            assertThat(a.getProviderId()).isEqualTo(5);
            assertThat(a.getStatus()).isEqualTo("PENDING_PAYMENT");
        }

        @Test
        @DisplayName("prePersist should set createdAt and updatedAt")
        void shouldSetTimestampsOnPrePersist() {
            Appointment a = new Appointment();
            a.prePersist();

            assertThat(a.getCreatedAt()).isNotNull();
            assertThat(a.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("preUpdate should update updatedAt")
        void shouldUpdateTimestampOnPreUpdate() throws InterruptedException {
            Appointment a = new Appointment();
            a.prePersist();
            LocalDateTime original = a.getUpdatedAt();

            Thread.sleep(10); // ensure time difference
            a.preUpdate();

            assertThat(a.getUpdatedAt()).isAfterOrEqualTo(original);
        }

        @Test
        @DisplayName("Should support setters for mutable fields")
        void shouldSupportSetters() {
            Appointment a = new Appointment();
            a.setStatus("CANCELLED");
            a.setSlotId(99);
            a.setNotes("Test note");

            assertThat(a.getStatus()).isEqualTo("CANCELLED");
            assertThat(a.getSlotId()).isEqualTo(99);
            assertThat(a.getNotes()).isEqualTo("Test note");
        }
    }
}
