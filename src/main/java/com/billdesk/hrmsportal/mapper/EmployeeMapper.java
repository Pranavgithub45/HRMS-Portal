package com.billdesk.hrmsportal.mapper;

import com.billdesk.hrmsportal.dto.response.EmployeeResponse;
import com.billdesk.hrmsportal.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "departmentId",   source = "department.departmentId")
    @Mapping(target = "departmentName", source = "department.departmentName")
    @Mapping(target = "managerId",      source = "manager.employeeId")
    @Mapping(target = "managerName",    source = "manager.name")
    EmployeeResponse toResponse(Employee employee);

    List<EmployeeResponse> toResponseList(List<Employee> employees);
}