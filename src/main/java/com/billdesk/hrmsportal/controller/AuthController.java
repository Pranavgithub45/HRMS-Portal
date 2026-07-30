package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.request.LoginRequest;
import com.billdesk.hrmsportal.dto.request.RegisterRequest;
import com.billdesk.hrmsportal.dto.response.LoginResponse;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestAttribute("role") Role callerRole) {

        Long id = authService.register(request, callerRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("employeeId", id, "message", "Employee registered successfully"));
    }

    /** Protected dummy endpoint — proves the filter and request attributes work. */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute("employeeId") Long employeeId,
                                  @RequestAttribute("email") String email,
                                  @RequestAttribute("role") Role role) {
        return Map.of("employeeId", employeeId, "email", email, "role", role);
    }
}