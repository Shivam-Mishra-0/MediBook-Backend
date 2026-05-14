package com.medibook.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GoogleLoginCompletionRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String picture;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "Patient|Provider", message = "Role must be Patient or Provider")
    private String role;
}
