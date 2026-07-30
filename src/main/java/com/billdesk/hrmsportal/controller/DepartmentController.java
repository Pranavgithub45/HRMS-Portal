package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.request.DepartmentRequest;
import com.billdesk.hrmsportal.dto.response.DepartmentResponse;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody DepartmentRequest request,
            @RequestAttribute("role") Role callerRole) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.create(request, callerRole));
    }

    @GetMapping
    public List<DepartmentResponse> getAll() {
        return departmentService.getAll();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request,
            @RequestAttribute("role") Role callerRole) {

        return departmentService.update(id, request, callerRole);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long id,
            @RequestAttribute("role") Role callerRole) {

        departmentService.delete(id, callerRole);
        return ResponseEntity.ok(Map.of("message", "Department deleted successfully"));
    }
}