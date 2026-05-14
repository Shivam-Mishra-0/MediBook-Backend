package com.medibook.admin.service;

import com.medibook.admin.dto.AddAdminRequest;
import com.medibook.admin.dto.UserResponse;
import com.medibook.admin.entity.User;
import com.medibook.admin.exception.DuplicateResourceException;
import com.medibook.admin.exception.ResourceNotFoundException;
import com.medibook.admin.repository.UserRepository;
import com.medibook.admin.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl Tests")
class AdminServiceImplTest {

    @Mock private UserRepository  userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl service;

    // ── test fixtures ──────────────────────────────────────────────────────

    private User buildUser(int id, String email, String role, boolean active) {
        return User.builder()
                .userId(id)
                .fullName("Test User " + id)
                .email(email)
                .passwordHash("hashed")
                .phone("9999999999")
                .role(role)
                .provider(null)
                .isActive(active)
                .createdAt(LocalDateTime.now())
                .profilePicUrl(null)
                .build();
    }

    // ── getAllUsers ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("Returns mapped list when users exist")
        void returnsAllUsers() {
            List<User> users = List.of(
                    buildUser(1, "a@test.com", "Admin",   true),
                    buildUser(2, "b@test.com", "Patient", true)
            );
            when(userRepository.findAll()).thenReturn(users);

            List<UserResponse> result = service.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmail()).isEqualTo("a@test.com");
            assertThat(result.get(1).getEmail()).isEqualTo("b@test.com");
        }

        @Test
        @DisplayName("Returns empty list when no users")
        void returnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());
            assertThat(service.getAllUsers()).isEmpty();
        }
    }

    // ── getUsersByRole ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUsersByRole")
    class GetUsersByRole {

        @Test
        @DisplayName("Returns only users matching the given role")
        void filtersByRole() {
            List<User> admins = List.of(buildUser(1, "admin@test.com", "Admin", true));
            when(userRepository.findAllByRole("Admin")).thenReturn(admins);

            List<UserResponse> result = service.getUsersByRole("Admin");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo("Admin");
        }

        @Test
        @DisplayName("Returns empty list for a role with no users")
        void noUsersForRole() {
            when(userRepository.findAllByRole("Provider")).thenReturn(List.of());
            assertThat(service.getUsersByRole("Provider")).isEmpty();
        }
    }

    // ── getUserById ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("Returns correct user for existing ID")
        void returnsUser() {
            User user = buildUser(5, "find@test.com", "Patient", true);
            when(userRepository.findById(5)).thenReturn(Optional.of(user));

            UserResponse resp = service.getUserById(5);

            assertThat(resp.getUserId()).isEqualTo(5);
            assertThat(resp.getEmail()).isEqualTo("find@test.com");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException for unknown ID")
        void throwsWhenNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("99");
        }
    }

    // ── deactivateUser ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("Sets isActive=false and saves")
        void deactivatesUser() {
            User user = buildUser(3, "deact@test.com", "Patient", true);
            when(userRepository.findById(3)).thenReturn(Optional.of(user));

            service.deactivateUser(3);

            assertThat(user.isActive()).isFalse();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException for unknown ID")
        void throwsWhenNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deactivateUser(99))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── reactivateUser ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("reactivateUser")
    class ReactivateUser {

        @Test
        @DisplayName("Sets isActive=true and saves")
        void reactivatesUser() {
            User user = buildUser(4, "react@test.com", "Patient", false);
            when(userRepository.findById(4)).thenReturn(Optional.of(user));

            service.reactivateUser(4);

            assertThat(user.isActive()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException for unknown ID")
        void throwsWhenNotFound() {
            when(userRepository.findById(55)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reactivateUser(55))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── addAdmin ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAdmin")
    class AddAdmin {

        private AddAdminRequest buildRequest() {
            AddAdminRequest req = new AddAdminRequest();
            req.setFullName("New Admin");
            req.setEmail("newadmin@test.com");
            req.setPassword("secure123");
            return req;
        }

        @Test
        @DisplayName("Creates admin and returns UserResponse when email is new")
        void createsAdmin() {
            AddAdminRequest req = buildRequest();
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed_pw");

            User saved = buildUser(10, req.getEmail(), "Admin", true);
            when(userRepository.save(any(User.class))).thenReturn(saved);

            UserResponse resp = service.addAdmin(req);

            assertThat(resp.getEmail()).isEqualTo(req.getEmail());
            assertThat(resp.getRole()).isEqualTo("Admin");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User persisted = captor.getValue();
            assertThat(persisted.getRole()).isEqualTo("Admin");
            assertThat(persisted.getPasswordHash()).isEqualTo("hashed_pw");
            assertThat(persisted.isActive()).isTrue();
            assertThat(persisted.getPhone()).isEqualTo("");
        }

        @Test
        @DisplayName("Throws DuplicateResourceException when email already exists")
        void throwsOnDuplicateEmail() {
            AddAdminRequest req = buildRequest();
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> service.addAdmin(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email")
                    .hasMessageContaining(req.getEmail());

            verify(userRepository, never()).save(any());
        }
    }

    // ── getAllAdmins ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllAdmins")
    class GetAllAdmins {

        @Test
        @DisplayName("Delegates to getUsersByRole('Admin')")
        void delegatesToGetUsersByRole() {
            List<User> admins = List.of(buildUser(1, "admin@test.com", "Admin", true));
            when(userRepository.findAllByRole("Admin")).thenReturn(admins);

            List<UserResponse> result = service.getAllAdmins();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo("Admin");
        }
    }

    // ── toResponse mapping ─────────────────────────────────────────────────

    @Nested
    @DisplayName("toResponse – field mapping")
    class ToResponseMapping {

        @Test
        @DisplayName("All fields are mapped correctly to UserResponse")
        void mapsAllFields() {
            LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 0);
            User user = User.builder()
                    .userId(7)
                    .fullName("John Doe")
                    .email("john@test.com")
                    .phone("1234567890")
                    .role("Provider")
                    .provider("google")
                    .isActive(false)
                    .createdAt(now)
                    .profilePicUrl("http://pic.url/img.png")
                    .build();

            when(userRepository.findById(7)).thenReturn(Optional.of(user));

            UserResponse resp = service.getUserById(7);

            assertThat(resp.getUserId()).isEqualTo(7);
            assertThat(resp.getFullName()).isEqualTo("John Doe");
            assertThat(resp.getEmail()).isEqualTo("john@test.com");
            assertThat(resp.getPhone()).isEqualTo("1234567890");
            assertThat(resp.getRole()).isEqualTo("Provider");
            assertThat(resp.getProvider()).isEqualTo("google");
            assertThat(resp.isActive()).isFalse();
            assertThat(resp.getCreatedAt()).isEqualTo(now);
            assertThat(resp.getProfilePicUrl()).isEqualTo("http://pic.url/img.png");
        }
    }
}
