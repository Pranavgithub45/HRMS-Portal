package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.request.LeaveApplyRequest;
import com.billdesk.hrmsportal.dto.response.LeaveResponse;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<LeaveResponse> apply(
            @Valid @RequestBody LeaveApplyRequest request,
            @RequestAttribute("employeeId") Long callerId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.apply(request, callerId));
    }

    @GetMapping("/me")
    public List<LeaveResponse> myLeaves(@RequestAttribute("employeeId") Long callerId) {
        return leaveService.getMyLeaves(callerId);
    }

    @GetMapping("/team")
    public List<LeaveResponse> teamLeaves(@RequestAttribute("employeeId") Long callerId,
                                          @RequestAttribute("role") Role callerRole) {
        return leaveService.getTeamLeaves(callerId, callerRole);
    }

    @GetMapping("/team/pending")
    public List<LeaveResponse> teamPending(@RequestAttribute("employeeId") Long callerId,
                                           @RequestAttribute("role") Role callerRole) {
        return leaveService.getTeamPendingLeaves(callerId, callerRole);
    }

    @GetMapping
    public List<LeaveResponse> getAll(@RequestAttribute("role") Role callerRole) {
        return leaveService.getAll(callerRole);
    }

    @PutMapping("/{id}/approve")
    public LeaveResponse approve(@PathVariable Long id,
                                 @RequestAttribute("employeeId") Long callerId,
                                 @RequestAttribute("role") Role callerRole) {
        return leaveService.approve(id, callerId, callerRole);
    }

    @PutMapping("/{id}/reject")
    public LeaveResponse reject(@PathVariable Long id,
                                @RequestAttribute("employeeId") Long callerId,
                                @RequestAttribute("role") Role callerRole) {
        return leaveService.reject(id, callerId, callerRole);
    }
}