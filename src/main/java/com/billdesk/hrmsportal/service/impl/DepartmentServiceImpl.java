package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.request.DepartmentRequest;
import com.billdesk.hrmsportal.dto.response.DepartmentResponse;
import com.billdesk.hrmsportal.entity.Department;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.mapper.DepartmentMapper;
import com.billdesk.hrmsportal.repository.DepartmentRepository;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request, Role callerRole) {

        requireHr(callerRole);

        if (departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department already exists: " + request.getDepartmentName());
        }

        Department department = new Department();
        department.setDepartmentName(request.getDepartmentName());
        department.setLocation(request.getLocation());
        department.setDescription(request.getDescription());

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentMapper.toResponseList(departmentRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long departmentId) {
        return departmentMapper.toResponse(findOrThrow(departmentId));
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long departmentId, DepartmentRequest request, Role callerRole) {

        requireHr(callerRole);

        Department department = findOrThrow(departmentId);

        // name uniqueness — only if it actually changed
        if (!department.getDepartmentName().equals(request.getDepartmentName())
                && departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department already exists: " + request.getDepartmentName());
        }

        department.setDepartmentName(request.getDepartmentName());
        department.setLocation(request.getLocation());
        department.setDescription(request.getDescription());

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public void delete(Long departmentId, Role callerRole) {

        requireHr(callerRole);

        Department department = findOrThrow(departmentId);

        // guard: employees still reference this department
        if (!employeeRepository.findByDepartment_DepartmentId(departmentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete department with assigned employees");
        }

        departmentRepository.delete(department);
    }

    private Department findOrThrow(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Department not found with id: " + departmentId));
    }

    private void requireHr(Role callerRole) {
        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }
    }
}