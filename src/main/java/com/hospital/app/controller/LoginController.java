package com.hospital.app.controller;

import com.hospital.app.dto.LoginRequest;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.model.User;
import com.hospital.app.service.UserService;
import com.hospital.app.service.DuoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
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

    @Autowired
    private DuoService duo;

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
        session.setAttribute(ACCOUNT_ID, verifiedUser.getId());
        session.setAttribute(AUTHENTICATED_USER, response);
        return ResponseEntity.ok(response);
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
