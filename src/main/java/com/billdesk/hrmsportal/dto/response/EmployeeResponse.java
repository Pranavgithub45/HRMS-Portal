package com.billdesk.hrmsportal.dto.response;

import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class EmployeeResponse {

    private Long employeeId;
    private String name;
    private String email;
    private String phone;
    private String designation;
    private LocalDate joinDate;
    private Role role;
    private EmployeeStatus status;
    private BigDecimal baseSalary;

    // flattened relations — never expose the full Employee/Department entity
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
}