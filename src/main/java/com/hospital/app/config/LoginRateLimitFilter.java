package com.hospital.app.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Bounded per-process backstop. Production also needs shared rate limits at the ingress. */
@Component
@Order(-90)
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Window(long start, int count) {}
    private final Map<String, Window> attempts = new HashMap<>();
    synchronized boolean allow(String address, long now) {
        attempts.entrySet().removeIf(entry -> now - entry.getValue().start() >= 300_000);
        Window window = attempts.get(address);
        if (window == null) {
            if (attempts.size() >= 10_000) return false;
            attempts.put(address, new Window(now, 1)); return true;
        }
        if (window.count() >= 30) return false;
        attempts.put(address, new Window(window.start(), window.count() + 1)); return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getServletPath().equals("/api/auth/login") && request.getMethod().equals("POST") && !allow(request.getRemoteAddr(), System.currentTimeMillis())) {
            response.setStatus(429); response.setHeader("Retry-After", "300"); response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json"); response.getWriter().write("{\"message\":\"Too many sign-in attempts. Try again in five minutes.\"}"); return;
        }
        chain.doFilter(request, response);
    }
}
