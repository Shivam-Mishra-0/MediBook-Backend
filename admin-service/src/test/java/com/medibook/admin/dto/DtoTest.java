package com.medibook.admin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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

    // ── AddAdminRequest Validations ────────────────────────────────────────

    @Nested
    @DisplayName("AddAdminRequest Validation")
    class AddAdminRequestValidation {

        private AddAdminRequest valid() {
            AddAdminRequest r = new AddAdminRequest();
            r.setFullName("Jane Admin");
            r.setEmail("jane@admin.com");
            r.setPassword("secure123");
            return r;
        }

        @Test
        @DisplayName("Valid request has no violations")
        void valid_noViolations() {
            assertThat(validator.validate(valid())).isEmpty();
        }

        @Test
        @DisplayName("Blank fullName triggers violation")
        void blankFullName_violation() {
            AddAdminRequest r = valid();
            r.setFullName("");
            Set<ConstraintViolation<AddAdminRequest>> violations = validator.validate(r);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        }

        @Test
        @DisplayName("Null fullName triggers violation")
        void nullFullName_violation() {
            AddAdminRequest r = valid();
            r.setFullName(null);
            assertThat(validator.validate(r))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        }

        @Test
        @DisplayName("Blank email triggers violation")
        void blankEmail_violation() {
            AddAdminRequest r = valid();
            r.setEmail("");
            assertThat(validator.validate(r))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Invalid email format triggers violation")
        void invalidEmail_violation() {
            AddAdminRequest r = valid();
            r.setEmail("not-an-email");
            assertThat(validator.validate(r))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Valid email passes validation")
        void validEmail_passes() {
            AddAdminRequest r = valid();
            r.setEmail("user@example.com");
            assertThat(validator.validate(r)).isEmpty();
        }

        @Test
        @DisplayName("Blank password triggers violation")
        void blankPassword_violation() {
            AddAdminRequest r = valid();
            r.setPassword("");
            assertThat(validator.validate(r))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("Short password (< 6 chars) triggers violation")
        void shortPassword_violation() {
            AddAdminRequest r = valid();
            r.setPassword("abc");
            assertThat(validator.validate(r))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("Password of exactly 6 chars passes validation")
        void exactly6CharPassword_passes() {
            AddAdminRequest r = valid();
            r.setPassword("abcdef");
            assertThat(validator.validate(r)).isEmpty();
        }

        @Test
        @DisplayName("Lombok @Data getters/setters work")
        void lombokData_works() {
            AddAdminRequest r = new AddAdminRequest();
            r.setFullName("Bob");
            r.setEmail("bob@test.com");
            r.setPassword("password123");

            assertThat(r.getFullName()).isEqualTo("Bob");
            assertThat(r.getEmail()).isEqualTo("bob@test.com");
            assertThat(r.getPassword()).isEqualTo("password123");
        }
    }

    // ── UserResponse ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("UserResponse")
    class UserResponseTests {

        @Test
        @DisplayName("Builder creates correct object")
        void builder_works() {
            LocalDateTime now = LocalDateTime.now();
            UserResponse resp = UserResponse.builder()
                    .userId(10)
                    .fullName("Test User")
                    .email("test@user.com")
                    .phone("9876543210")
                    .role("Patient")
                    .provider("google")
                    .isActive(true)
                    .createdAt(now)
                    .profilePicUrl("http://pic.url")
                    .build();

            assertThat(resp.getUserId()).isEqualTo(10);
            assertThat(resp.getFullName()).isEqualTo("Test User");
            assertThat(resp.getEmail()).isEqualTo("test@user.com");
            assertThat(resp.getPhone()).isEqualTo("9876543210");
            assertThat(resp.getRole()).isEqualTo("Patient");
            assertThat(resp.getProvider()).isEqualTo("google");
            assertThat(resp.isActive()).isTrue();
            assertThat(resp.getCreatedAt()).isEqualTo(now);
            assertThat(resp.getProfilePicUrl()).isEqualTo("http://pic.url");
        }

        @Test
        @DisplayName("NoArgsConstructor creates object with defaults")
        void noArgsConstructor_works() {
            UserResponse resp = new UserResponse();
            assertThat(resp.getEmail()).isNull();
            assertThat(resp.getRole()).isNull();
        }

        @Test
        @DisplayName("AllArgsConstructor sets all fields")
        void allArgsConstructor_works() {
            LocalDateTime now = LocalDateTime.now();
            UserResponse resp = new UserResponse(1, "Name", "e@test.com", "123", "Admin", null, true, now, null);
            assertThat(resp.getUserId()).isEqualTo(1);
            assertThat(resp.getFullName()).isEqualTo("Name");
        }

        @Test
        @DisplayName("Setters update fields correctly")
        void setters_work() {
            UserResponse resp = new UserResponse();
            resp.setUserId(5);
            resp.setFullName("Updated Name");
            resp.setEmail("updated@test.com");
            resp.setActive(false);

            assertThat(resp.getUserId()).isEqualTo(5);
            assertThat(resp.getFullName()).isEqualTo("Updated Name");
            assertThat(resp.isActive()).isFalse();
        }

        @Test
        @DisplayName("Equality based on fields (Lombok @Data)")
        void equality_works() {
            LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
            UserResponse r1 = new UserResponse(1, "A", "a@test.com", "123", "Admin", null, true, now, null);
            UserResponse r2 = new UserResponse(1, "A", "a@test.com", "123", "Admin", null, true, now, null);
            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("toString is not blank")
        void toString_notBlank() {
            UserResponse resp = UserResponse.builder().userId(1).email("x@y.com").build();
            assertThat(resp.toString()).isNotBlank();
        }
    }
}
