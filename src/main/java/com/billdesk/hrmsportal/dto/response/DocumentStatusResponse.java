package com.billdesk.hrmsportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter @AllArgsConstructor
public class DocumentStatusResponse {

    private Long employeeId;
    private String employeeName;
    private boolean uploaded;
    private String documentType;
    private LocalDate uploadedDate;
}