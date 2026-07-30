package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.request.EmployeeSelfUpdateRequest;
import com.billdesk.hrmsportal.dto.request.EmployeeUpdateRequest;
import com.billdesk.hrmsportal.dto.response.EmployeeResponse;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;

import java.util.List;

public interface EmployeeService {

    List<EmployeeResponse> getAll(Long callerId, Role callerRole);

    EmployeeResponse getById(Long targetId, Long callerId, Role callerRole);

    EmployeeResponse getMe(Long callerId);

    List<EmployeeResponse> getTeam(Long callerId, Role callerRole);

    EmployeeResponse update(Long targetId, EmployeeUpdateRequest request, Role callerRole);

    EmployeeResponse updateMe(Long callerId, EmployeeSelfUpdateRequest request);

    EmployeeResponse updateStatus(Long targetId, EmployeeStatus status, Long callerId, Role callerRole);
}