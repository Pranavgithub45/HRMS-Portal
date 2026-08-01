package com.billdesk.hrmsportal.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class PayrollResponse {

    private Long payrollId;
    private Integer month;
    private Integer year;
    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal netSalary;
    private LocalDate generatedDate;

    private Long employeeId;
    private String employeeName;
}