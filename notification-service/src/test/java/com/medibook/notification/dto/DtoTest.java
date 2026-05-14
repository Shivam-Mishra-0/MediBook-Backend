package com.medibook.notification.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO Tests")
class DtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ─────────────────────────────────────────────────────────────
    // NotificationRequest
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("NotificationRequest")
    class NotificationRequestTests {

        private NotificationRequest buildValidRequest() {
            NotificationRequest req = new NotificationRequest();
            req.setRecipientId(1);
            req.setType("BOOKING");
            req.setTitle("Confirmed");
            req.setMessage("Your appointment is confirmed.");
            req.setChannel("APP");
            return req;
        }

        @Test
        @DisplayName("Valid request has no violations")
        void validRequest_noViolations() {
            Set<ConstraintViolation<NotificationRequest>> violations =
                    validator.validate(buildValidRequest());
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Missing type triggers @NotBlank violation")
        void missingType_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setType(null);
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("type"));
        }

        @Test
        @DisplayName("Invalid type triggers @Pattern violation")
        void invalidType_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setType("UNKNOWN");
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("type"));
        }

        @Test
        @DisplayName("Blank title triggers @NotBlank violation")
        void blankTitle_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setTitle("   ");
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
        }

        @Test
        @DisplayName("Blank message triggers @NotBlank violation")
        void blankMessage_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setMessage("");
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("message"));
        }

        @Test
        @DisplayName("Non-positive recipient ID triggers @Positive violation")
        void nonPositiveRecipientId_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setRecipientId(0);
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipientId"));
        }

        @Test
        @DisplayName("Invalid channel triggers @Pattern violation")
        void invalidChannel_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setChannel("PUSH");
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("channel"));
        }

        @Test
        @DisplayName("Invalid email triggers @Email violation")
        void invalidEmail_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setEmail("not-an-email");
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Negative related ID triggers @PositiveOrZero violation")
        void negativeRelatedId_triggersViolation() {
            NotificationRequest req = buildValidRequest();
            req.setRelatedId(-1);
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("relatedId"));
        }

        @Test
        @DisplayName("Default channel is APP")
        void defaultChannel_isApp() {
            NotificationRequest req = new NotificationRequest();
            assertThat(req.getChannel()).isEqualTo("APP");
        }

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersAndGetters_workCorrectly() {
            NotificationRequest req = new NotificationRequest();
            req.setRecipientId(10);
            req.setType("REMINDER");
            req.setEmail("test@example.com");
            req.setTitle("Reminder");
            req.setMessage("Your appointment is in 1 hour.");
            req.setChannel("EMAIL");
            req.setRelatedId(5);
            req.setRelatedType("APPOINTMENT");

            assertThat(req.getRecipientId()).isEqualTo(10);
            assertThat(req.getType()).isEqualTo("REMINDER");
            assertThat(req.getEmail()).isEqualTo("test@example.com");
            assertThat(req.getTitle()).isEqualTo("Reminder");
            assertThat(req.getMessage()).isEqualTo("Your appointment is in 1 hour.");
            assertThat(req.getChannel()).isEqualTo("EMAIL");
            assertThat(req.getRelatedId()).isEqualTo(5);
            assertThat(req.getRelatedType()).isEqualTo("APPOINTMENT");
        }

        @Test
        @DisplayName("Multiple blank fields produce multiple violations")
        void multipleBlankFields_produceMultipleViolations() {
            NotificationRequest req = new NotificationRequest();
            req.setType(null);
            req.setTitle(null);
            req.setMessage(null);
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(req);
            assertThat(violations.size()).isGreaterThanOrEqualTo(3);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // AppointmentEventDto
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AppointmentEventDto")
    class AppointmentEventDtoTests {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersAndGetters_work() {
            AppointmentEventDto dto = new AppointmentEventDto();
            dto.setAppointmentId(1);
            dto.setPatientId(5);
            dto.setProviderId(2);
            dto.setEventType("BOOKED");
            dto.setServiceType("GENERAL");
            dto.setModeOfConsultation("IN_PERSON");
            dto.setAppointmentDate("2025-06-01");
            dto.setStartTime("09:00");
            dto.setEndTime("09:30");
            dto.setMessage("Confirmed");

            assertThat(dto.getAppointmentId()).isEqualTo(1);
            assertThat(dto.getPatientId()).isEqualTo(5);
            assertThat(dto.getProviderId()).isEqualTo(2);
            assertThat(dto.getEventType()).isEqualTo("BOOKED");
            assertThat(dto.getServiceType()).isEqualTo("GENERAL");
            assertThat(dto.getModeOfConsultation()).isEqualTo("IN_PERSON");
            assertThat(dto.getAppointmentDate()).isEqualTo("2025-06-01");
            assertThat(dto.getStartTime()).isEqualTo("09:00");
            assertThat(dto.getEndTime()).isEqualTo("09:30");
            assertThat(dto.getMessage()).isEqualTo("Confirmed");
        }

        @Test
        @DisplayName("New instance has zero/null defaults")
        void newInstance_hasDefaults() {
            AppointmentEventDto dto = new AppointmentEventDto();
            assertThat(dto.getAppointmentId()).isZero();
            assertThat(dto.getPatientId()).isZero();
            assertThat(dto.getEventType()).isNull();
        }

        @Test
        @DisplayName("Lombok @Data generates toString")
        void toString_isNotNull() {
            AppointmentEventDto dto = new AppointmentEventDto();
            dto.setAppointmentId(1);
            assertThat(dto.toString()).contains("1");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UserDto
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("UserDto")
    class UserDtoTests {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersAndGetters_work() {
            UserDto dto = new UserDto();
            dto.setUserId(10);
            dto.setFullName("John Doe");
            dto.setEmail("john@example.com");
            dto.setPhone("+919876543210");
            dto.setRole("PATIENT");
            dto.setActive(true);

            assertThat(dto.getUserId()).isEqualTo(10);
            assertThat(dto.getFullName()).isEqualTo("John Doe");
            assertThat(dto.getEmail()).isEqualTo("john@example.com");
            assertThat(dto.getPhone()).isEqualTo("+919876543210");
            assertThat(dto.getRole()).isEqualTo("PATIENT");
            assertThat(dto.isActive()).isTrue();
        }

        @Test
        @DisplayName("New instance has zero/null defaults")
        void newInstance_hasDefaults() {
            UserDto dto = new UserDto();
            assertThat(dto.getUserId()).isZero();
            assertThat(dto.getEmail()).isNull();
            assertThat(dto.isActive()).isFalse();
        }

        @Test
        @DisplayName("Lombok @Data equals and hashCode work")
        void equalsAndHashCode_work() {
            UserDto u1 = new UserDto();
            u1.setUserId(1);
            u1.setEmail("a@b.com");

            UserDto u2 = new UserDto();
            u2.setUserId(1);
            u2.setEmail("a@b.com");

            assertThat(u1).isEqualTo(u2);
            assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        }
    }
}
