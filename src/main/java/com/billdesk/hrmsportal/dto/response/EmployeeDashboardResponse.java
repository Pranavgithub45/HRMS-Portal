package com.billdesk.hrmsportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class EmployeeDashboardResponse {

    private Long employeeId;
    private String name;
    private String designation;
    private String departmentName;

    private long leavePending;
    private long leaveApproved;
    private long leaveRejected;

    private PayrollResponse latestPayroll;
    private boolean documentUploaded;
}
