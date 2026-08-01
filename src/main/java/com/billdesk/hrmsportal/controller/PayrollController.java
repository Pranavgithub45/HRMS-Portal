package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.request.PayrollBulkRequest;
import com.billdesk.hrmsportal.dto.request.PayrollGenerateRequest;
import com.billdesk.hrmsportal.dto.response.PayrollResponse;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    public ResponseEntity<PayrollResponse> generate(
            @Valid @RequestBody PayrollGenerateRequest request,
            @RequestAttribute("role") Role callerRole) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.generate(request, callerRole));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> generateBulk(
            @Valid @RequestBody PayrollBulkRequest request,
            @RequestAttribute("role") Role callerRole) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.generateBulk(request, callerRole));
    }

    @GetMapping("/me")
    public List<PayrollResponse> myPayroll(@RequestAttribute("employeeId") Long callerId) {
        return payrollService.getMyPayroll(callerId);
    }

    @GetMapping
    public List<PayrollResponse> getAll(@RequestParam(required = false) Integer month,
                                        @RequestParam(required = false) Integer year,
                                        @RequestAttribute("role") Role callerRole) {
        return payrollService.getAll(month, year, callerRole);
    }

    @GetMapping("/{employeeId}")
    public List<PayrollResponse> getByEmployee(@PathVariable Long employeeId,
                                               @RequestAttribute("role") Role callerRole) {
        return payrollService.getByEmployee(employeeId, callerRole);
    }
}