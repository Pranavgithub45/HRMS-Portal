package com.billdesk.hrmsportal.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class DepartmentResponse {

    private Long departmentId;
    private String departmentName;
    private String location;
    private String description;
}