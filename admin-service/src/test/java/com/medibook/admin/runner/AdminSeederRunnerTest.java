package com.medibook.admin.runner;

import com.medibook.admin.config.AdminConfig;
import com.medibook.admin.config.AdminConfig.AdminEntry;
import com.medibook.admin.entity.User;
import com.medibook.admin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSeederRunner Tests")
class AdminSeederRunnerTest {

    @Mock private AdminConfig      adminConfig;
    @Mock private UserRepository   userRepository;
    @Mock private PasswordEncoder  passwordEncoder;
    @Mock private ApplicationArguments args;

    @InjectMocks
    private AdminSeederRunner seeder;

    private AdminEntry entryOf(String fullName, String email, String password) {
        AdminEntry e = new AdminEntry();
        e.setFullName(fullName);
        e.setEmail(email);
        e.setPassword(password);
        return e;
    }

    // ── null / empty config ────────────────────────────────────────────────

    @Test
    @DisplayName("Skips seeding when admin list is null")
    void nullAdminList_skipsSeeding() throws Exception {
        when(adminConfig.getAdmins()).thenReturn(null);

        seeder.run(args);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Skips seeding when admin list is empty")
    void emptyAdminList_skipsSeeding() throws Exception {
        when(adminConfig.getAdmins()).thenReturn(Collections.emptyList());

        seeder.run(args);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    // ── already-existing admin ─────────────────────────────────────────────

    @Test
    @DisplayName("Skips entry when email already exists in DB")
    void existingAdmin_skipped() throws Exception {
        AdminEntry entry = entryOf("Existing Admin", "exist@test.com", "pass123");
        when(adminConfig.getAdmins()).thenReturn(List.of(entry));
        when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);

        seeder.run(args);

        verify(userRepository, never()).save(any());
    }

    // ── new admin ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Saves new admin when email is not already in DB")
    void newAdmin_savedToRepository() throws Exception {
        AdminEntry entry = entryOf("New Admin", "new@test.com", "securePass");
        when(adminConfig.getAdmins()).thenReturn(List.of(entry));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass")).thenReturn("hashed_pw");

        seeder.run(args);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getFullName()).isEqualTo("New Admin");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_pw");
        assertThat(saved.getRole()).isEqualTo("Admin");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPhone()).isEqualTo("");
        assertThat(saved.getProvider()).isNull();
    }

    // ── mixed list ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Seeds only new entries when list has mix of new and existing")
    void mixedList_onlyNewEntriesSeeded() throws Exception {
        AdminEntry existing = entryOf("Existing", "exist@test.com", "pw1");
        AdminEntry newEntry  = entryOf("New",      "new@test.com",   "pw2");

        when(adminConfig.getAdmins()).thenReturn(List.of(existing, newEntry));
        when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pw2")).thenReturn("hashed_pw2");

        seeder.run(args);

        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Seeds all entries when none exist in DB")
    void allNew_allSeeded() throws Exception {
        AdminEntry e1 = entryOf("Admin1", "a1@test.com", "pw1");
        AdminEntry e2 = entryOf("Admin2", "a2@test.com", "pw2");

        when(adminConfig.getAdmins()).thenReturn(List.of(e1, e2));
        when(userRepository.existsByEmail("a1@test.com")).thenReturn(false);
        when(userRepository.existsByEmail("a2@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        seeder.run(args);

        verify(userRepository, times(2)).save(any());
    }
}
