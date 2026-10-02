package com.show.reservation.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private static final String ADMIN_USER_ID = "admin";
    private final String adminToken;
    public AuthFilter(@Value("${app.admin-token}") String adminToken) { this.adminToken = adminToken; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return req.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ") || h.substring(7).isBlank()) {
            writeUnauthorized(res, "A non-empty Bearer token is required.");
            return;
        }
        String token = h.substring(7).trim();
        boolean admin = token.equals(adminToken);
        if (!admin && ADMIN_USER_ID.equals(token)) {
            writeUnauthorized(res, "The user ID 'admin' is reserved for the administrator. Use a different user token.");
            return;
        }
        req.setAttribute("userId", admin ? ADMIN_USER_ID : token);
        req.setAttribute("admin", admin);
        chain.doFilter(req, res);
    }

    private void writeUnauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
    }
}