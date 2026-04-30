package com.financialhub.financialhubapi.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final ObjectMapper objectMapper;

    public RequestLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            logRequest(request, response.getStatus(), durationMs);
        }
    }

    private void logRequest(HttpServletRequest request, int status, long durationMs) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("method", request.getMethod());
        entry.put("path", request.getRequestURI());
        entry.put("query", request.getQueryString() != null ? request.getQueryString() : "");
        entry.put("status", status);
        entry.put("duration_ms", durationMs);
        entry.put("ip", clientIp(request));
        entry.put("user_agent", request.getHeader("User-Agent"));

        try {
            String json = objectMapper.writeValueAsString(entry);
            if (status >= 500) {
                log.error(json);
            } else if (status >= 400) {
                log.warn(json);
            } else {
                log.info(json);
            }
        } catch (Exception e) {
            log.error("Failed to serialize request log");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded : request.getRemoteAddr();
    }
}
