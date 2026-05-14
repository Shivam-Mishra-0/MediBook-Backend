package com.medibook.admin.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.admin.dto.AddAdminRequest;
import com.medibook.admin.dto.UserResponse;
import com.medibook.admin.exception.DuplicateResourceException;
import com.medibook.admin.exception.GlobalExceptionHandler;
import com.medibook.admin.exception.ResourceNotFoundException;
import com.medibook.admin.service.AdminService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminResource (Controller) Tests")
class AdminResourceTest {

    @Mock private AdminService adminService;

    @InjectMocks private AdminResource adminResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Wire global exception handler so 404/409/400 responses are handled
        mockMvc = MockMvcBuilders.standaloneSetup(adminResource)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // for LocalDateTime
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private UserResponse buildResponse(int id, String email, String role) {
        return UserResponse.builder()
                .userId(id)
                .fullName("User " + id)
                .email(email)
                .phone("0000000000")
                .role(role)
                .provider(null)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .profilePicUrl(null)
                .build();
    }

    // ── GET /admin/users ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users")
    class GetAllUsers {

        @Test
        @DisplayName("200 with user list")
        void returns200WithUsers() throws Exception {
            when(adminService.getAllUsers())
                    .thenReturn(List.of(buildResponse(1, "a@test.com", "Admin")));

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].email").value("a@test.com"));
        }

        @Test
        @DisplayName("200 with empty list when no users")
        void returns200WithEmptyList() throws Exception {
            when(adminService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /admin/users/{userId} ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users/{userId}")
    class GetUserById {

        @Test
        @DisplayName("200 with user when found")
        void returns200() throws Exception {
            when(adminService.getUserById(1)).thenReturn(buildResponse(1, "u@test.com", "Patient"));

            mockMvc.perform(get("/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.email").value("u@test.com"));
        }

        @Test
        @DisplayName("404 when user not found")
        void returns404() throws Exception {
            when(adminService.getUserById(99))
                    .thenThrow(new ResourceNotFoundException("User", "id", 99));

            mockMvc.perform(get("/admin/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value(containsString("User")));
        }
    }

    // ── GET /admin/users/role/{role} ───────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users/role/{role}")
    class GetUsersByRole {

        @Test
        @DisplayName("200 with filtered list")
        void returns200() throws Exception {
            when(adminService.getUsersByRole("Admin"))
                    .thenReturn(List.of(buildResponse(1, "admin@test.com", "Admin")));

            mockMvc.perform(get("/admin/users/role/Admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].role").value("Admin"));
        }
    }

    // ── PUT /admin/users/{userId}/deactivate ───────────────────────────────

    @Nested
    @DisplayName("PUT /admin/users/{userId}/deactivate")
    class DeactivateUser {

        @Test
        @DisplayName("200 with success message")
        void returns200() throws Exception {
            doNothing().when(adminService).deactivateUser(1);

            mockMvc.perform(put("/admin/users/1/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deactivated successfully"));
        }

        @Test
        @DisplayName("404 when user not found")
        void returns404() throws Exception {
            doThrow(new ResourceNotFoundException("User", "id", 99))
                    .when(adminService).deactivateUser(99);

            mockMvc.perform(put("/admin/users/99/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PUT /admin/users/{userId}/reactivate ───────────────────────────────

    @Nested
    @DisplayName("PUT /admin/users/{userId}/reactivate")
    class ReactivateUser {

        @Test
        @DisplayName("200 with success message")
        void returns200() throws Exception {
            doNothing().when(adminService).reactivateUser(1);

            mockMvc.perform(put("/admin/users/1/reactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User reactivated successfully"));
        }

        @Test
        @DisplayName("404 when user not found")
        void returns404() throws Exception {
            doThrow(new ResourceNotFoundException("User", "id", 88))
                    .when(adminService).reactivateUser(88);

            mockMvc.perform(put("/admin/users/88/reactivate"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /admin/admins ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/admins")
    class GetAllAdmins {

        @Test
        @DisplayName("200 with admin list")
        void returns200() throws Exception {
            when(adminService.getAllAdmins())
                    .thenReturn(List.of(buildResponse(1, "admin@test.com", "Admin")));

            mockMvc.perform(get("/admin/admins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].role").value("Admin"));
        }
    }

    // ── POST /admin/admins ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /admin/admins")
    class AddAdmin {

        private AddAdminRequest validRequest() {
            AddAdminRequest req = new AddAdminRequest();
            req.setFullName("New Admin");
            req.setEmail("newadmin@test.com");
            req.setPassword("secure123");
            return req;
        }

        @Test
        @DisplayName("201 with userId and email when valid")
        void returns201() throws Exception {
            AddAdminRequest req = validRequest();
            UserResponse resp = buildResponse(10, req.getEmail(), "Admin");
            when(adminService.addAdmin(org.mockito.ArgumentMatchers.any(AddAdminRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/admin/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Admin created successfully"))
                    .andExpect(jsonPath("$.userId").value(10))
                    .andExpect(jsonPath("$.email").value(req.getEmail()));
        }

        @Test
        @DisplayName("409 when email already exists")
        void returns409OnDuplicate() throws Exception {
            AddAdminRequest req = validRequest();
            when(adminService.addAdmin(any()))
                    .thenThrow(new DuplicateResourceException("User", "email", req.getEmail()));

            mockMvc.perform(post("/admin/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value(containsString("email")));
        }

        @Test
        @DisplayName("400 when fullName is blank")
        void returns400OnBlankName() throws Exception {
            AddAdminRequest req = new AddAdminRequest();
            req.setFullName("");
            req.setEmail("ok@test.com");
            req.setPassword("secure123");

            mockMvc.perform(post("/admin/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when email is invalid")
        void returns400OnInvalidEmail() throws Exception {
            AddAdminRequest req = new AddAdminRequest();
            req.setFullName("Admin");
            req.setEmail("not-an-email");
            req.setPassword("secure123");

            mockMvc.perform(post("/admin/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when password is too short")
        void returns400OnShortPassword() throws Exception {
            AddAdminRequest req = new AddAdminRequest();
            req.setFullName("Admin");
            req.setEmail("ok@test.com");
            req.setPassword("abc"); // < 6 chars

            mockMvc.perform(post("/admin/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /admin/ping ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/ping")
    class Ping {

        @Test
        @DisplayName("200 with service info")
        void returns200() throws Exception {
            mockMvc.perform(get("/admin/ping"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("admin-service"));
        }
    }
}
