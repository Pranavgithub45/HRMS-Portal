package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.request.LeaveApplyRequest;
import com.billdesk.hrmsportal.dto.response.LeaveResponse;
import com.billdesk.hrmsportal.entity.enums.Role;

import java.util.List;

public interface LeaveService {

    LeaveResponse apply(LeaveApplyRequest request, Long callerId);

    List<LeaveResponse> getMyLeaves(Long callerId);

    List<LeaveResponse> getTeamLeaves(Long callerId, Role callerRole);

    List<LeaveResponse> getTeamPendingLeaves(Long callerId, Role callerRole);

    List<LeaveResponse> getAll(Role callerRole);

    LeaveResponse approve(Long leaveId, Long callerId, Role callerRole);

    LeaveResponse reject(Long leaveId, Long callerId, Role callerRole);
}