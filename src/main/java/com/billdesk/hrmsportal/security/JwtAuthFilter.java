package com.billdesk.hrmsportal.security;

import com.billdesk.hrmsportal.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/health"
    );

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (PUBLIC_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            sendError(response, "Missing or malformed Authorization header");
            return;
        }

        String token = header.substring(7).trim();

        try {
            Claims claims = jwtUtil.parseClaims(token);

            Long employeeId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            Role role = Role.valueOf(claims.get("role", String.class));

            request.setAttribute("employeeId", employeeId);
            request.setAttribute("email", email);
            request.setAttribute("role", role);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            sendError(response, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            sendError(response, "Invalid token");
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}