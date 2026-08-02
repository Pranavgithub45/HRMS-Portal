package com.billdesk.hrmsportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter @AllArgsConstructor
public class HrDashboardResponse {

    private long totalEmployees;
    private long totalDepartments;

    private long leavePending;
    private long leaveApproved;

    private long payrollGeneratedThisMonth;
    private Map<String, Long> departmentWiseEmployeeCount;

    private long employeesJoinedThisMonth;
    private long employeesMissingDocuments;
}
