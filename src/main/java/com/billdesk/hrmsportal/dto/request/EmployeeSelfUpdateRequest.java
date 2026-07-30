package com.billdesk.hrmsportal.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmployeeSelfUpdateRequest {

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;
}