package com.hospital.app.controller;

import com.hospital.app.config.AuthenticationFilter;
import com.hospital.app.dto.LoginRequest;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.model.User;
import com.hospital.app.service.DuoService;
import com.hospital.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DuoAuthenticationTest {
    LoginController controller;
    DuoService duo;
    UserService users;
    MockHttpServletRequest request;

    @BeforeEach void setup() throws Exception {
        controller = new LoginController();
        duo = mock(DuoService.class);
        users = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "duo", duo);
        ReflectionTestUtils.setField(controller, "userService", users);
        request = new MockHttpServletRequest();
        when(duo.isEnabled()).thenReturn(true);
        when(duo.state()).thenReturn("random-state");
        when(duo.authorizationUrl("alice", "random-state")).thenReturn("https://example.duosecurity.com/auth");
        when(users.authenticateUser(any())).thenReturn(new LoginResponse(true, "OK", "alice", "DOCTOR", "a@example.com"));
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole("DOCTOR");
        when(users.getUserByUsername("alice")).thenReturn(Optional.of(user));
    }

    @Test void passwordAloneCannotAccessApi() throws Exception {
        var response = controller.login(new LoginRequest("alice", "password"), request);
        assertEquals(true, ((Map<?, ?>) response.getBody()).get("mfaRequired"));
        assertNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
        request.setServletPath("/api/patients");
        var output = new MockHttpServletResponse();
        new AuthenticationFilter().doFilter(request, output, new MockFilterChain());
        assertEquals(401, output.getStatus());
    }

    @Test void callbackAuthenticatesOnceAndRotatesSession() throws Exception {
        controller.login(new LoginRequest("alice", "password"), request);
        String previousId = request.getSession().getId();
        assertEquals("/login.html?duo=complete", controller.callback("random-state", "code", request).getHeaders().getLocation().toString());
        assertNotEquals(previousId, request.getSession().getId());
        assertNotNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
        controller.callback("random-state", "code", request);
        verify(duo, times(1)).verify("code", "alice");
        controller.logout(request);
        assertEquals(401, controller.session(request).getStatusCode().value());
    }

    @Test void wrongStateConsumesPendingAttempt() throws Exception {
        controller.login(new LoginRequest("alice", "password"), request);
        controller.callback("wrong", "code", request);
        controller.callback("random-state", "code", request);
        verify(duo, never()).verify(anyString(), anyString());
        assertNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test void deniedDuoNeverAuthenticates() throws Exception {
        controller.login(new LoginRequest("alice", "password"), request);
        doThrow(new RuntimeException("denied")).when(duo).verify(anyString(), anyString());
        controller.callback("random-state", "code", request);
        assertNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test void expiredCallbackDoesNotContactDuo() throws Exception {
        controller.login(new LoginRequest("alice", "password"), request);
        Object pending = request.getSession().getAttribute("duoPending");
        var constructor = pending.getClass().getDeclaredConstructor(LoginResponse.class, String.class, long.class);
        constructor.setAccessible(true);
        request.getSession().setAttribute("duoPending", constructor.newInstance(
                new LoginResponse(true, "OK", "alice", "DOCTOR", "a@example.com"), "random-state", 0L));
        controller.callback("random-state", "code", request);
        verify(duo, never()).verify(anyString(), anyString());
        assertNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test void callbackWithoutSessionIsRejected() throws Exception {
        assertEquals("/login.html?duo=failed", controller.callback("random-state", "code", request).getHeaders().getLocation().toString());
        verify(duo, never()).verify(anyString(), anyString());
    }

    @Test void outageFailsClosed() throws Exception {
        when(duo.authorizationUrl(anyString(), anyString())).thenThrow(new RuntimeException("offline"));
        assertEquals(503, controller.login(new LoginRequest("alice", "password"), request).getStatusCode().value());
        assertEquals(401, controller.session(request).getStatusCode().value());
    }

    @Test void invalidPasswordDoesNotContactDuo() {
        when(users.authenticateUser(any())).thenReturn(new LoginResponse());
        assertEquals(401, controller.login(new LoginRequest("alice", "password"), request).getStatusCode().value());
        verifyNoInteractions(duo);
    }

    @Test void disabledDuoAllowsPasswordSession() {
        when(duo.isEnabled()).thenReturn(false);
        controller.login(new LoginRequest("alice", "password"), request);
        assertNotNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test void crossOriginMutationIsRejected() throws Exception {
        request.setServletPath("/api/auth/logout");
        request.setMethod("POST");
        request.addHeader("Origin", "https://other.example");
        var output = new MockHttpServletResponse();
        new AuthenticationFilter().doFilter(request, output, new MockFilterChain());
        assertEquals(403, output.getStatus());
    }
}
