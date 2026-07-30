package com.billdesk.hrmsportal.mapper;

import com.billdesk.hrmsportal.dto.response.DepartmentResponse;
import com.billdesk.hrmsportal.entity.Department;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponse toResponse(Department department);

    List<DepartmentResponse> toResponseList(List<Department> departments);
}