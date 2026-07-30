package com.billdesk.hrmsportal.dto.request;

import com.billdesk.hrmsportal.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String designation;

    @PastOrPresent(message = "Join date cannot be in the future")
    private LocalDate joinDate;

    @NotNull(message = "Role is required")
    private Role role;

    private BigDecimal baseSalary;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private Long managerId;
}