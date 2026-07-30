package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.request.EmployeeSelfUpdateRequest;
import com.billdesk.hrmsportal.dto.request.EmployeeUpdateRequest;
import com.billdesk.hrmsportal.dto.request.RegisterRequest;
import com.billdesk.hrmsportal.dto.response.EmployeeResponse;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.AuthService;
import com.billdesk.hrmsportal.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AuthService authService;

    /** HR creates an employee — delegates to AuthService so password hashing lives in one place. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody RegisterRequest request,
            @RequestAttribute("role") Role callerRole) {

        Long id = authService.register(request, callerRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("employeeId", id, "message", "Employee created successfully"));
    }

    @GetMapping
    public List<EmployeeResponse> getAll(@RequestAttribute("employeeId") Long callerId,
                                         @RequestAttribute("role") Role callerRole) {
        return employeeService.getAll(callerId, callerRole);
    }

    @GetMapping("/me")
    public EmployeeResponse getMe(@RequestAttribute("employeeId") Long callerId) {
        return employeeService.getMe(callerId);
    }

    @GetMapping("/team")
    public List<EmployeeResponse> getTeam(@RequestAttribute("employeeId") Long callerId,
                                          @RequestAttribute("role") Role callerRole) {
        return employeeService.getTeam(callerId, callerRole);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id,
                                    @RequestAttribute("employeeId") Long callerId,
                                    @RequestAttribute("role") Role callerRole) {
        return employeeService.getById(id, callerId, callerRole);
    }

    @PutMapping("/me")
    public EmployeeResponse updateMe(@Valid @RequestBody EmployeeSelfUpdateRequest request,
                                     @RequestAttribute("employeeId") Long callerId) {
        return employeeService.updateMe(callerId, request);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id,
                                   @Valid @RequestBody EmployeeUpdateRequest request,
                                   @RequestAttribute("role") Role callerRole) {
        return employeeService.update(id, request, callerRole);
    }

    @PutMapping("/{id}/status")
    public EmployeeResponse updateStatus(@PathVariable Long id,
                                         @RequestParam EmployeeStatus status,
                                         @RequestAttribute("employeeId") Long callerId,
                                         @RequestAttribute("role") Role callerRole) {
        return employeeService.updateStatus(id, status, callerId, callerRole);
    }
}