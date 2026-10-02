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
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }
        String token = h.substring(7).trim();
        boolean admin = token.equals(adminToken);
        req.setAttribute("userId", admin ? "admin" : token);   // identity ONLY from token
        req.setAttribute("admin", admin);
        chain.doFilter(req, res);
    }
}