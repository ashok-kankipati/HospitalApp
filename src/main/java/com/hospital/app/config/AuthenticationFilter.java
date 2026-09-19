package com.hospital.app.config;

import com.hospital.app.controller.LoginController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    @org.springframework.beans.factory.annotation.Autowired
    private com.hospital.app.repository.UserRepository users;
    private static final org.slf4j.Logger audit = org.slf4j.LoggerFactory.getLogger("security.audit");
    private static final Set<String> PUBLIC = Set.of("/api/auth/login", "/api/auth/duo/callback", "/api/auth/totp/verify", "/api/auth/session", "/api/auth/logout", "/api/auth/health");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        // Twilio retrieves these expiring, HMAC-protected PDFs without a browser session.
        boolean signedMedia = "GET".equals(request.getMethod()) && path.matches("/api/whatsapp/media/[0-9]+/[0-9]+/[A-Za-z0-9_-]+\\.pdf");
        if (path.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
            String origin = request.getHeader("Origin");
            String expected = request.getScheme() + "://" + request.getServerName()
                    + ((request.getServerPort() == 80 && request.getScheme().equals("http"))
                    || (request.getServerPort() == 443 && request.getScheme().equals("https")) ? "" : ":" + request.getServerPort());
            if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
                    && ("cross-site".equals(request.getHeader("Sec-Fetch-Site")) || (origin != null && !origin.equals(expected)))) {
                response.sendError(403);
                return;
            }
            var session = request.getSession(false);
            boolean protectedPath = !PUBLIC.contains(path) && !signedMedia;
            if (protectedPath || path.equals("/api/auth/session")) {
                Object id = session == null ? null : session.getAttribute(LoginController.ACCOUNT_ID);
                var user = id instanceof Long accountId && session.getAttribute(LoginController.AUTHENTICATED_USER) != null
                        ? users.findById(accountId).orElse(null) : null;
                if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                    if (session != null) session.invalidate();
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Please log in\"}");
                    return;
                }
                session.setAttribute(LoginController.AUTHENTICATED_USER, new com.hospital.app.dto.LoginResponse(true,
                        "Login successful", user.getUsername(), user.getRole(), user.getEmail()));
                if (!ApiPermissions.allows(user.getRole(), path, request.getMethod())) {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Your role does not have permission for this operation.\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
        if (path.startsWith("/api/") && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
            var session = request.getSession(false);
            Object actor = session == null ? null : session.getAttribute(LoginController.ACCOUNT_ID);
            String[] parts = path.split("/");
            audit.info("api_mutation actorId={} module={} method={} status={}", actor, parts.length > 2 ? parts[2] : "unknown", request.getMethod(), response.getStatus());
        }
    }
}
