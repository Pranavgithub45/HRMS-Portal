package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.response.EmployeeDashboardResponse;
import com.billdesk.hrmsportal.dto.response.HrDashboardResponse;
import com.billdesk.hrmsportal.dto.response.ManagerDashboardResponse;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/employee")
    public EmployeeDashboardResponse employeeDashboard(@RequestAttribute("employeeId") Long callerId) {
        return dashboardService.getEmployeeDashboard(callerId);
    }

    @GetMapping("/manager")
    public ManagerDashboardResponse managerDashboard(@RequestAttribute("employeeId") Long callerId,
                                                      @RequestAttribute("role") Role callerRole) {
        return dashboardService.getManagerDashboard(callerId, callerRole);
    }

    @GetMapping("/hr")
    public HrDashboardResponse hrDashboard(@RequestAttribute("role") Role callerRole) {
        return dashboardService.getHrDashboard(callerRole);
    }
}
