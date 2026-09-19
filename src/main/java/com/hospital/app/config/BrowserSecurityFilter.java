package com.hospital.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Component
@Order(-100)
public class BrowserSecurityFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String path = request.getServletPath();
        boolean pdfPreview = "GET".equals(request.getMethod()) && (path.matches("/api/lab/reports/[0-9]+/pdf")
            || path.matches("/api/billing/invoices/[0-9]+/pdf"));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", pdfPreview ? "SAMEORIGIN" : "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        // Inline handlers in legacy clinical screens must be migrated before strict script-src enforcement.
        response.setHeader("Content-Security-Policy", (pdfPreview ? "frame-ancestors 'self'; " : "frame-ancestors 'none'; ")
            + "object-src 'none'; base-uri 'self'; form-action 'self'");
        if (request.isSecure()) response.setHeader("Strict-Transport-Security", "max-age=31536000");
        if (request.getServletPath().startsWith("/api/") && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
                && !"Careflow".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(403); response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing request protection header. Reload the application.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
