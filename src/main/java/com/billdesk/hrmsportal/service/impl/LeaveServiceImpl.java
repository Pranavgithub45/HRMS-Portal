package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.request.LeaveApplyRequest;
import com.billdesk.hrmsportal.dto.response.LeaveResponse;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.LeaveRequest;
import com.billdesk.hrmsportal.entity.enums.LeaveStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.mapper.LeaveMapper;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.repository.LeaveRequestRepository;
import com.billdesk.hrmsportal.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;

    // ---------------- APPLY ----------------

    @Override
    @Transactional
    public LeaveResponse apply(LeaveApplyRequest request, Long callerId) {

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date cannot be after end date");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot apply for leave in the past");
        }

        Employee employee = findEmployeeOrThrow(callerId);

        if (employee.getManager() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No approving manager assigned. Contact HR.");
        }

        boolean overlaps = leaveRequestRepository.existsOverlappingLeave(
                callerId,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                request.getStartDate(),
                request.getEndDate());

        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a pending or approved leave overlapping these dates");
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(request.getLeaveType());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setReason(request.getReason());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedDate(LocalDate.now());
        leave.setApprovedBy(null);

        return leaveMapper.toResponse(leaveRequestRepository.save(leave));
    }

    // ---------------- READ ----------------

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(Long callerId) {
        return leaveMapper.toResponseList(
                leaveRequestRepository.findByEmployee_EmployeeIdOrderByAppliedDateDesc(callerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getTeamLeaves(Long callerId, Role callerRole) {
        requireManagerOrHr(callerRole);
        return leaveMapper.toResponseList(
                leaveRequestRepository.findByEmployee_Manager_EmployeeIdOrderByAppliedDateDesc(callerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getTeamPendingLeaves(Long callerId, Role callerRole) {
        requireManagerOrHr(callerRole);
        return leaveMapper.toResponseList(
                leaveRequestRepository.findByEmployee_Manager_EmployeeIdAndStatus(
                        callerId, LeaveStatus.PENDING));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getAll(Role callerRole) {
        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }
        return leaveMapper.toResponseList(leaveRequestRepository.findAll());
    }

    // ---------------- ACTIONS ----------------

    @Override
    @Transactional
    public LeaveResponse approve(Long leaveId, Long callerId, Role callerRole) {
        return action(leaveId, callerId, callerRole, LeaveStatus.APPROVED);
    }

    @Override
    @Transactional
    public LeaveResponse reject(Long leaveId, Long callerId, Role callerRole) {
        return action(leaveId, callerId, callerRole, LeaveStatus.REJECTED);
    }

    private LeaveResponse action(Long leaveId, Long callerId, Role callerRole, LeaveStatus newStatus) {

        requireManagerOrHr(callerRole);

        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Leave request not found with id: " + leaveId));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Leave request is already " + leave.getStatus());
        }

        if (leave.getEmployee().getEmployeeId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot action your own leave request");
        }

        boolean isOwnManager = leave.getEmployee().getManager() != null
                && leave.getEmployee().getManager().getEmployeeId().equals(callerId);

        if (callerRole != Role.HR && !isOwnManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only action leave requests of your own team members");
        }

        leave.setStatus(newStatus);
        leave.setApprovedBy(findEmployeeOrThrow(callerId));

        return leaveMapper.toResponse(leaveRequestRepository.save(leave));
    }

    // ---------------- HELPERS ----------------

    private Employee findEmployeeOrThrow(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found with id: " + employeeId));
    }

    private void requireManagerOrHr(Role callerRole) {
        if (callerRole == Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. MANAGER or HR role required");
        }
    }
}