package com.billdesk.hrmsportal.mapper;

import com.billdesk.hrmsportal.dto.response.PayrollResponse;
import com.billdesk.hrmsportal.entity.Payroll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    @Mapping(target = "employeeId",   source = "employee.employeeId")
    @Mapping(target = "employeeName", source = "employee.name")
    PayrollResponse toResponse(Payroll payroll);

    List<PayrollResponse> toResponseList(List<Payroll> payrolls);
}