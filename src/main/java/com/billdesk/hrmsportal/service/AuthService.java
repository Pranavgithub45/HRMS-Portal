package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.request.LoginRequest;
import com.billdesk.hrmsportal.dto.request.RegisterRequest;
import com.billdesk.hrmsportal.dto.response.LoginResponse;
import com.billdesk.hrmsportal.entity.enums.Role;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    Long register(RegisterRequest request, Role callerRole);
}