package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.response.EmployeeDashboardResponse;
import com.billdesk.hrmsportal.dto.response.HrDashboardResponse;
import com.billdesk.hrmsportal.dto.response.ManagerDashboardResponse;
import com.billdesk.hrmsportal.entity.enums.Role;

public interface DashboardService {

    EmployeeDashboardResponse getEmployeeDashboard(Long callerId);

    ManagerDashboardResponse getManagerDashboard(Long callerId, Role callerRole);

    HrDashboardResponse getHrDashboard(Role callerRole);
}
