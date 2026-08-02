package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.response.EmployeeDashboardResponse;
import com.billdesk.hrmsportal.dto.response.HrDashboardResponse;
import com.billdesk.hrmsportal.dto.response.ManagerDashboardResponse;
import com.billdesk.hrmsportal.dto.response.PayrollResponse;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.enums.LeaveStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.mapper.PayrollMapper;
import com.billdesk.hrmsportal.repository.DepartmentRepository;
import com.billdesk.hrmsportal.repository.DocumentRepository;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.repository.LeaveRequestRepository;
import com.billdesk.hrmsportal.repository.PayrollRepository;
import com.billdesk.hrmsportal.service.DashboardService;
import com.billdesk.hrmsportal.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final DocumentRepository documentRepository;
    private final PayrollMapper payrollMapper;
    private final DocumentService documentService;

    @Override
    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getEmployeeDashboard(Long callerId) {

        Employee employee = employeeRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found with id: " + callerId));

        List<PayrollResponse> payrolls = payrollMapper.toResponseList(
                payrollRepository.findByEmployee_EmployeeIdOrderByYearDescMonthDesc(callerId));

        return new EmployeeDashboardResponse(
                employee.getEmployeeId(),
                employee.getName(),
                employee.getDesignation(),
                employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null,
                leaveRequestRepository.countByEmployee_EmployeeIdAndStatus(callerId, LeaveStatus.PENDING),
                leaveRequestRepository.countByEmployee_EmployeeIdAndStatus(callerId, LeaveStatus.APPROVED),
                leaveRequestRepository.countByEmployee_EmployeeIdAndStatus(callerId, LeaveStatus.REJECTED),
                payrolls.isEmpty() ? null : payrolls.get(0),
                documentRepository.existsByEmployee_EmployeeId(callerId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(Long callerId, Role callerRole) {

        requireManager(callerRole);

        long teamSize = employeeRepository.findByManager_EmployeeId(callerId).size();

        return new ManagerDashboardResponse(
                teamSize,
                leaveRequestRepository.countByEmployee_Manager_EmployeeIdAndStatus(callerId, LeaveStatus.PENDING),
                leaveRequestRepository.countByEmployee_Manager_EmployeeIdAndStatus(callerId, LeaveStatus.APPROVED),
                leaveRequestRepository.countByEmployee_Manager_EmployeeIdAndStatus(callerId, LeaveStatus.REJECTED),
                documentService.getTeamStatus(callerId, callerRole)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HrDashboardResponse getHrDashboard(Role callerRole) {

        requireHr(callerRole);

        long totalEmployees = employeeRepository.count();
        YearMonth now = YearMonth.now();
        LocalDate monthStart = now.atDay(1);
        LocalDate today = LocalDate.now();

        Map<String, Long> departmentWiseCount = new LinkedHashMap<>();
        for (Object[] row : employeeRepository.countEmployeesByDepartment()) {
            departmentWiseCount.put((String) row[0], (Long) row[1]);
        }

        return new HrDashboardResponse(
                totalEmployees,
                departmentRepository.count(),
                leaveRequestRepository.countByStatus(LeaveStatus.PENDING),
                leaveRequestRepository.countByStatus(LeaveStatus.APPROVED),
                payrollRepository.countByMonthAndYear(now.getMonthValue(), now.getYear()),
                departmentWiseCount,
                employeeRepository.findByJoinDateBetween(monthStart, today).size(),
                totalEmployees - documentRepository.countDistinctEmployeesWithDocument()
        );
    }

    private void requireManager(Role callerRole) {
        if (callerRole != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. MANAGER role required");
        }
    }

    private void requireHr(Role callerRole) {
        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }
    }
}
