package com.medibook.admin.config;

import com.medibook.admin.config.AdminConfig.AdminEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdminConfig Tests")
class AdminConfigTest {

    // ── AdminConfig ────────────────────────────────────────────────────────

    @Test
    @DisplayName("AdminConfig stores and retrieves admin list")
    void adminConfig_storesAdminList() {
        AdminConfig config = new AdminConfig();
        AdminEntry entry = new AdminEntry();
        entry.setFullName("Test Admin");
        entry.setEmail("admin@test.com");
        entry.setPassword("pass123");

        config.setAdmins(List.of(entry));

        assertThat(config.getAdmins()).hasSize(1);
        assertThat(config.getAdmins().get(0).getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("AdminConfig defaults to null admin list")
    void adminConfig_defaultsToNull() {
        AdminConfig config = new AdminConfig();
        assertThat(config.getAdmins()).isNull();
    }

    // ── AdminEntry ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("AdminEntry setters and getters work correctly")
    void adminEntry_settersAndGetters() {
        AdminEntry entry = new AdminEntry();
        entry.setFullName("Jane Doe");
        entry.setEmail("jane@test.com");
        entry.setPassword("mypassword");

        assertThat(entry.getFullName()).isEqualTo("Jane Doe");
        assertThat(entry.getEmail()).isEqualTo("jane@test.com");
        assertThat(entry.getPassword()).isEqualTo("mypassword");
    }

    @Test
    @DisplayName("AdminEntry equality is based on field values (Lombok @Data)")
    void adminEntry_equality() {
        AdminEntry e1 = new AdminEntry();
        e1.setFullName("Admin");
        e1.setEmail("a@test.com");
        e1.setPassword("pw");

        AdminEntry e2 = new AdminEntry();
        e2.setFullName("Admin");
        e2.setEmail("a@test.com");
        e2.setPassword("pw");

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    @DisplayName("AdminEntry toString is not null")
    void adminEntry_toString() {
        AdminEntry entry = new AdminEntry();
        entry.setFullName("X");
        entry.setEmail("x@x.com");
        entry.setPassword("pw");
        assertThat(entry.toString()).isNotBlank();
    }

    @Test
    @DisplayName("AdminConfig with empty list")
    void adminConfig_emptyList() {
        AdminConfig config = new AdminConfig();
        config.setAdmins(List.of());
        assertThat(config.getAdmins()).isEmpty();
    }

    @Test
    @DisplayName("AdminConfig with multiple entries")
    void adminConfig_multipleEntries() {
        AdminConfig config = new AdminConfig();

        AdminEntry e1 = new AdminEntry();
        e1.setFullName("Admin One"); e1.setEmail("one@test.com"); e1.setPassword("p1");

        AdminEntry e2 = new AdminEntry();
        e2.setFullName("Admin Two"); e2.setEmail("two@test.com"); e2.setPassword("p2");

        config.setAdmins(List.of(e1, e2));

        assertThat(config.getAdmins()).hasSize(2);
        assertThat(config.getAdmins()).extracting(AdminEntry::getEmail)
                .containsExactly("one@test.com", "two@test.com");
    }
}
