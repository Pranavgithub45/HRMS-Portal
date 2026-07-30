package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.request.DepartmentRequest;
import com.billdesk.hrmsportal.dto.response.DepartmentResponse;
import com.billdesk.hrmsportal.entity.enums.Role;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse create(DepartmentRequest request, Role callerRole);

    List<DepartmentResponse> getAll();

    DepartmentResponse getById(Long departmentId);

    DepartmentResponse update(Long departmentId, DepartmentRequest request, Role callerRole);

    void delete(Long departmentId, Role callerRole);
}