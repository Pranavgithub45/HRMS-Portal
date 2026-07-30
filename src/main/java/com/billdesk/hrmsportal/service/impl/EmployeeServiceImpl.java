package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.request.EmployeeSelfUpdateRequest;
import com.billdesk.hrmsportal.dto.request.EmployeeUpdateRequest;
import com.billdesk.hrmsportal.dto.response.EmployeeResponse;
import com.billdesk.hrmsportal.entity.Department;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.mapper.EmployeeMapper;
import com.billdesk.hrmsportal.repository.DepartmentRepository;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;

    // ---------------- READ ----------------

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll(Long callerId, Role callerRole) {
        return switch (callerRole) {
            case HR       -> employeeMapper.toResponseList(employeeRepository.findAll());
            case MANAGER  -> employeeMapper.toResponseList(
                    employeeRepository.findByManager_EmployeeId(callerId));
            case EMPLOYEE -> List.of(employeeMapper.toResponse(findOrThrow(callerId)));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long targetId, Long callerId, Role callerRole) {

        Employee target = findOrThrow(targetId);

        if (!canView(target, callerId, callerRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. You can only view your own profile or your team members");
        }

        return employeeMapper.toResponse(target);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getMe(Long callerId) {
        return employeeMapper.toResponse(findOrThrow(callerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getTeam(Long callerId, Role callerRole) {

        if (callerRole == Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. MANAGER or HR role required");
        }

        return employeeMapper.toResponseList(employeeRepository.findByManager_EmployeeId(callerId));
    }

    // ---------------- WRITE ----------------

    @Override
    @Transactional
    public EmployeeResponse update(Long targetId, EmployeeUpdateRequest request, Role callerRole) {

        requireHr(callerRole);

        Employee employee = findOrThrow(targetId);

        // email uniqueness — only if changed
        if (!employee.getEmail().equals(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered: " + request.getEmail());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Department not found with id: " + request.getDepartmentId()));

        Employee manager = null;
        if (request.getManagerId() != null) {

            if (request.getManagerId().equals(targetId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "An employee cannot be their own manager");
            }

            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Manager not found with id: " + request.getManagerId()));

            if (manager.getRole() == Role.EMPLOYEE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Assigned manager must have MANAGER or HR role");
            }
        }

        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDesignation(request.getDesignation());
        employee.setJoinDate(request.getJoinDate());
        employee.setRole(request.getRole());
        employee.setBaseSalary(request.getBaseSalary());
        employee.setDepartment(department);
        employee.setManager(manager);

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse updateMe(Long callerId, EmployeeSelfUpdateRequest request) {

        Employee employee = findOrThrow(callerId);

        if (request.getPhone() != null)       employee.setPhone(request.getPhone());
        if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse updateStatus(Long targetId, EmployeeStatus status,
                                         Long callerId, Role callerRole) {

        requireHr(callerRole);

        if (targetId.equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot change your own status");
        }

        Employee employee = findOrThrow(targetId);

        // deactivating a manager would orphan their reports
        if (status == EmployeeStatus.INACTIVE
                && !employeeRepository.findByManager_EmployeeId(targetId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot deactivate an employee who still manages other employees. Reassign their reports first.");
        }

        employee.setStatus(status);
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    // ---------------- HELPERS ----------------

    private Employee findOrThrow(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found with id: " + employeeId));
    }

    /** HR sees all; anyone sees themselves; a MANAGER sees their own direct reports. */
    private boolean canView(Employee target, Long callerId, Role callerRole) {

        if (callerRole == Role.HR) return true;

        if (target.getEmployeeId().equals(callerId)) return true;

        return callerRole == Role.MANAGER
                && target.getManager() != null
                && target.getManager().getEmployeeId().equals(callerId);
    }

    private void requireHr(Role callerRole) {
        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }
    }
}