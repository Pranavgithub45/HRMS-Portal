package com.billdesk.hrmsportal.dto.response;

import com.billdesk.hrmsportal.entity.enums.LeaveStatus;
import com.billdesk.hrmsportal.entity.enums.LeaveType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class LeaveResponse {

    private Long leaveId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalDays;
    private String reason;
    private LeaveStatus status;
    private LocalDate appliedDate;

    private Long employeeId;
    private String employeeName;

    private Long approvedById;
    private String approvedByName;
}