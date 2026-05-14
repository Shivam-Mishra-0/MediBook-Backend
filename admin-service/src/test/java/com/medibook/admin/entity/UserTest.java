package com.medibook.admin.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity Tests")
class UserTest {

    // ── builder / constructor ──────────────────────────────────────────────

    @Test
    @DisplayName("Builder creates User with all fields set")
    void builder_allFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId(1)
                .fullName("Alice")
                .email("alice@test.com")
                .passwordHash("hashed")
                .phone("1234567890")
                .role("Admin")
                .provider("google")
                .isActive(true)
                .createdAt(now)
                .profilePicUrl("http://pic.url")
                .build();

        assertThat(user.getUserId()).isEqualTo(1);
        assertThat(user.getFullName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@test.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getPhone()).isEqualTo("1234567890");
        assertThat(user.getRole()).isEqualTo("Admin");
        assertThat(user.getProvider()).isEqualTo("google");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getProfilePicUrl()).isEqualTo("http://pic.url");
    }

    @Test
    @DisplayName("NoArgsConstructor creates an empty User")
    void noArgsConstructor() {
        User user = new User();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getRole()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor sets all fields")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(5, "Bob", "bob@test.com", "hash", "0000", "Patient", null, true, now, null);
        assertThat(user.getUserId()).isEqualTo(5);
        assertThat(user.getFullName()).isEqualTo("Bob");
    }

    // ── setters ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setActive toggles isActive field")
    void setActive_toggles() {
        User user = new User();
        user.setActive(true);
        assertThat(user.isActive()).isTrue();
        user.setActive(false);
        assertThat(user.isActive()).isFalse();
    }

    // ── prePersist ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("prePersist sets createdAt when null")
    void prePersist_setsCreatedAtWhenNull() {
        User user = new User();
        assertThat(user.getCreatedAt()).isNull();

        user.prePersist();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("prePersist does NOT overwrite an existing createdAt")
    void prePersist_doesNotOverwriteExistingCreatedAt() {
        LocalDateTime original = LocalDateTime.of(2020, 1, 1, 0, 0);
        User user = User.builder().createdAt(original).build();

        user.prePersist();

        assertThat(user.getCreatedAt()).isEqualTo(original);
    }

    // ── equals / hashCode / toString (Lombok @Data) ─────────────────────

    @Test
    @DisplayName("Two Users with identical fields are equal")
    void equality_sameFields() {
        LocalDateTime now = LocalDateTime.of(2025, 6, 1, 12, 0);
        User u1 = User.builder().userId(1).email("a@b.com").fullName("A")
                .role("Admin").isActive(true).createdAt(now).build();
        User u2 = User.builder().userId(1).email("a@b.com").fullName("A")
                .role("Admin").isActive(true).createdAt(now).build();

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    @DisplayName("toString is not blank")
    void toString_notBlank() {
        User user = User.builder().userId(1).email("x@y.com").fullName("X").role("Admin").build();
        assertThat(user.toString()).isNotBlank();
    }
}
