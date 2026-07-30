package com.billdesk.hrmsportal.dto.response;

import com.billdesk.hrmsportal.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long employeeId;
    private String name;
    private String email;
    private Role role;
}