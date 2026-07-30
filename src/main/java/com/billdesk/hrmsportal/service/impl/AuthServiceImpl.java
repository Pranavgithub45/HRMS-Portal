package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.request.*;
import com.billdesk.hrmsportal.dto.response.LoginResponse;
import com.billdesk.hrmsportal.entity.Department;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.repository.DepartmentRepository;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.security.JwtUtil;
import com.billdesk.hrmsportal.service.AuthService;
import com.billdesk.hrmsportal.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordUtil.matches(request.getPassword(), employee.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive. Contact HR.");
        }

        return new LoginResponse(
                jwtUtil.generateToken(employee),
                employee.getEmployeeId(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole()
        );
    }

    @Override
    @Transactional
    public Long register(RegisterRequest request, Role callerRole) {

        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered: " + request.getEmail());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Department not found with id: " + request.getDepartmentId()));

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Manager not found with id: " + request.getManagerId()));

            if (manager.getRole() == Role.EMPLOYEE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Assigned manager must have MANAGER or HR role");
            }
        }

        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordUtil.hash(request.getPassword()));
        employee.setPhone(request.getPhone());
        employee.setDesignation(request.getDesignation());
        employee.setJoinDate(request.getJoinDate());
        employee.setRole(request.getRole());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setBaseSalary(request.getBaseSalary());
        employee.setDepartment(department);
        employee.setManager(manager);

        return employeeRepository.save(employee).getEmployeeId();
    }
}