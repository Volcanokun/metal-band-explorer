package com.metalexplorer.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Configuration
public class LoggingConfig {

    @Bean
    public OncePerRequestFilter traceIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String traceId = request.getHeader("X-Trace-Id");
                if (traceId == null || traceId.isBlank()) {
                    traceId = UUID.randomUUID().toString();
                }
                MDC.put("traceId", traceId);
                try {
                    chain.doFilter(request, response);
                } finally {
                    MDC.clear();
                }
            }
        };
    }
}
