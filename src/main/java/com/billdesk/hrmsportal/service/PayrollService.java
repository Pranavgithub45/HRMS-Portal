package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.request.PayrollBulkRequest;
import com.billdesk.hrmsportal.dto.request.PayrollGenerateRequest;
import com.billdesk.hrmsportal.dto.response.PayrollResponse;
import com.billdesk.hrmsportal.entity.enums.Role;

import java.util.List;
import java.util.Map;

public interface PayrollService {

    PayrollResponse generate(PayrollGenerateRequest request, Role callerRole);

    Map<String, Object> generateBulk(PayrollBulkRequest request, Role callerRole);

    List<PayrollResponse> getMyPayroll(Long callerId);

    List<PayrollResponse> getByEmployee(Long employeeId, Role callerRole);

    List<PayrollResponse> getAll(Integer month, Integer year, Role callerRole);
}