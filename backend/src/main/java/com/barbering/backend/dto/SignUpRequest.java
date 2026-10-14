package com.Barbering.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "Name is required.")
    @Size(max = 100, message = "Name must be at most 100 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be a valid address.")
    @Size(max = 255, message = "Email must be at most 255 characters.")
    private String email;

    // BCrypt truncates input at 72 bytes; reject longer passwords up front.
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    private String password;

    @Pattern(regexp = "(?i)CUSTOMER|BARBER", message = "Role must be CUSTOMER or BARBER.")
    private String role;
}
