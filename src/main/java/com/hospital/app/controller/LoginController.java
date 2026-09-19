package com.hospital.app.controller;

import com.hospital.app.dto.LoginRequest;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.service.UserService;
import com.hospital.app.service.DuoService;
import com.hospital.app.service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    public static final String ACCOUNT_ID = "accountId";
    @Autowired private com.hospital.app.service.AccountService accounts;
    public static final String AUTHENTICATED_USER = "authenticatedUser";
    private record PendingLogin(LoginResponse user, String state, long expiresAt) {}
        private record PendingTotp(LoginResponse user, Long userId, String enrollmentSecret, long expiresAt, int attempts) {}
        public record TotpVerification(@jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Pattern(regexp="[0-9A-Fa-f -]{6,20}", message="Enter a valid authenticator or recovery code.") String code) {}

    @Autowired
    private DuoService duo;

    @Autowired
    private TotpService totp;

    @Autowired
    private UserService userService;

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@jakarta.validation.Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        var initialUser = userService.getUserByUsername(loginRequest.getUsername());
        LoginResponse response = userService.authenticateUser(loginRequest);
        if (!response.isSuccess() || initialUser.isEmpty()) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid username or password"));
        var verifiedUser = initialUser.get();
        HttpSession session = request.getSession(true);
        if (duo.isEnabled() && totp.isEnabled()) {
            session.invalidate();
            return ResponseEntity.status(503).body(Map.of("success", false,
                "message", "MFA configuration is invalid. Enable either Duo or TOTP, not both."));
        }
        if (duo.isEnabled()) {
            try {
                String state = duo.state();
                String url = duo.authorizationUrl(response.getUsername(), state);
                session.setAttribute("pendingUserId", verifiedUser.getId());
                session.setAttribute("duoPending", new PendingLogin(response, state, System.currentTimeMillis() + 300_000));
                return ResponseEntity.ok(Map.of("success", false, "mfaRequired", true, "redirectUrl", url));
            } catch (Exception e) {
                session.invalidate();
                return ResponseEntity.status(503).body(Map.of("success", false, "message", "Duo is unavailable. Please try again."));
            }
        }
        if (totp.isEnabled()) {
            boolean enrolled = totp.isEnrolled(verifiedUser.getId());
            TotpService.Enrollment enrollment = enrolled ? null : totp.beginEnrollment(verifiedUser.getUsername());
            session.setAttribute("totpPending", new PendingTotp(response, verifiedUser.getId(),
                    enrollment == null ? null : enrollment.secret(), System.currentTimeMillis() + 300_000, 0));
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("success", false);
            result.put("mfaRequired", true);
            result.put("mfaMethod", "totp");
            result.put("enrollmentRequired", !enrolled);
            if (enrollment != null) {
                result.put("qrCode", enrollment.qrCode());
                result.put("manualKey", enrollment.manualKey());
            }
            return ResponseEntity.ok(result);
        }
        session.setAttribute(ACCOUNT_ID, verifiedUser.getId());
        session.setAttribute(AUTHENTICATED_USER, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/totp/verify")
    public ResponseEntity<?> verifyTotp(@jakarta.validation.Valid @RequestBody TotpVerification verification,
                                        HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        PendingTotp pending = session == null ? null : (PendingTotp) session.getAttribute("totpPending");
        if (!totp.isEnabled() || pending == null || pending.expiresAt() <= System.currentTimeMillis()) {
            if (session != null) session.invalidate();
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authenticator session expired. Sign in again."));
        }
        if (pending.attempts() >= 4) {
            session.invalidate();
            return ResponseEntity.status(429).body(Map.of("success", false, "message", "Too many incorrect codes. Sign in again."));
        }

        List<String> recoveryCodes = List.of();
        boolean verified;
        if (pending.enrollmentSecret() != null) {
            try {
                recoveryCodes = totp.enroll(pending.userId(), pending.enrollmentSecret(), verification.code());
                verified = true;
            } catch (IllegalArgumentException exception) {
                verified = false;
            }
        } else {
            verified = totp.verify(pending.userId(), verification.code());
        }
        if (!verified) {
            session.setAttribute("totpPending", new PendingTotp(pending.user(), pending.userId(),
                    pending.enrollmentSecret(), pending.expiresAt(), pending.attempts() + 1));
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid or expired authenticator code."));
        }

        session.removeAttribute("totpPending");
        request.changeSessionId();
        session.setAttribute(ACCOUNT_ID, pending.userId());
        session.setAttribute(AUTHENTICATED_USER, pending.user());
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("username", pending.user().getUsername());
        result.put("role", pending.user().getRole());
        result.put("email", pending.user().getEmail());
        if (!recoveryCodes.isEmpty()) result.put("recoveryCodes", recoveryCodes);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/duo/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state,
            @RequestParam(name = "duo_code", required = false) String code, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        PendingLogin pending = null;
        Object pendingId = null;
        if (session != null) {
            synchronized (session) {
                pending = (PendingLogin) session.getAttribute("duoPending");
                session.removeAttribute("duoPending");
                pendingId = session.getAttribute("pendingUserId");
                session.removeAttribute("pendingUserId");
            }
        }
        try {
            if (!duo.isEnabled() || pending == null || state == null || code == null || code.isBlank()
                    || !pending.state().equals(state) || pending.expiresAt() <= System.currentTimeMillis()) {
                return redirect("/login.html?duo=failed");
            }
            duo.verify(code, pending.user().getUsername());
            var user = userService.getUserByUsername(pending.user().getUsername());
            if (user.isEmpty() || !Boolean.TRUE.equals(user.get().getIsActive()) || pendingId == null || !pendingId.equals(user.get().getId())) return redirect("/login.html?duo=failed");
            request.changeSessionId();
            session.setAttribute(ACCOUNT_ID, user.get().getId());
            session.setAttribute(AUTHENTICATED_USER, new LoginResponse(true, "Login successful",
                    user.get().getUsername(), user.get().getRole(), user.get().getEmail()));
            return redirect("/login.html?duo=complete");
        } catch (Exception e) {
            return redirect("/login.html?duo=failed");
        }
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(303).location(URI.create(location)).header("Cache-Control", "no-store").build();
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(AUTHENTICATED_USER);
        return user == null ? ResponseEntity.status(401).build() : ResponseEntity.ok().header("Cache-Control", "no-store").body(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    /**
     * Register endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@jakarta.validation.Valid @RequestBody com.hospital.app.service.AccountService.CreateAccount body, HttpServletRequest request) {
        var account = accounts.create(AccountController.actorId(request), body);
        return ResponseEntity.ok(new LoginResponse(true, "Account created", account.username(), account.role(), account.email()));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Login API is running successfully!");
    }
}
