package com.billdesk.hrmsportal.config;

import com.billdesk.hrmsportal.security.JwtAuthFilter;
import com.billdesk.hrmsportal.security.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<JwtAuthFilter> registration =
                new FilterRegistrationBean<>(new JwtAuthFilter(jwtUtil));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}