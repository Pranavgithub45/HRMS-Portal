package com.billdesk.hrmsportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor
public class ManagerDashboardResponse {

    private long teamSize;

    private long leavePending;
    private long leaveApproved;
    private long leaveRejected;

    private List<DocumentStatusResponse> teamDocumentStatus;
}
