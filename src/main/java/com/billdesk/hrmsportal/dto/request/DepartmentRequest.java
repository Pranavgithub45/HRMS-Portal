package com.billdesk.hrmsportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Department name must be at most 100 characters")
    private String departmentName;

    @Size(max = 150, message = "Location must be at most 150 characters")
    private String location;

    private String description;
}