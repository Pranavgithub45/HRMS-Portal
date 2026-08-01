package com.billdesk.hrmsportal.mapper;

import com.billdesk.hrmsportal.dto.response.LeaveResponse;
import com.billdesk.hrmsportal.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LeaveMapper {

    @Mapping(target = "employeeId",     source = "employee.employeeId")
    @Mapping(target = "employeeName",   source = "employee.name")
    @Mapping(target = "approvedById",   source = "approvedBy.employeeId")
    @Mapping(target = "approvedByName", source = "approvedBy.name")
    @Mapping(target = "totalDays",
            expression = "java(java.time.temporal.ChronoUnit.DAYS.between("
                    + "leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1)")
    LeaveResponse toResponse(LeaveRequest leaveRequest);

    List<LeaveResponse> toResponseList(List<LeaveRequest> leaveRequests);
}