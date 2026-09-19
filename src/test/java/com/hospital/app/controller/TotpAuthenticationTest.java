package com.hospital.app.controller;

import com.hospital.app.dto.LoginRequest;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.model.User;
import com.hospital.app.service.DuoService;
import com.hospital.app.service.TotpService;
import com.hospital.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TotpAuthenticationTest {
    private LoginController controller;
    private TotpService totp;
    private MockHttpServletRequest request;

    @BeforeEach
    void setup() {
        controller = new LoginController();
        totp = mock(TotpService.class);
        var duo = mock(DuoService.class);
        var users = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "duo", duo);
        ReflectionTestUtils.setField(controller, "totp", totp);
        ReflectionTestUtils.setField(controller, "userService", users);
        when(duo.isEnabled()).thenReturn(false);
        when(totp.isEnabled()).thenReturn(true);
        when(users.authenticateUser(any())).thenReturn(new LoginResponse(true, "OK", "alice", "Doctor", "a@example.com"));
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole("Doctor");
        user.setIsActive(true);
        when(users.getUserByUsername("alice")).thenReturn(Optional.of(user));
        request = new MockHttpServletRequest();
    }

    @Test
    void firstLoginReturnsEnrollmentQrAndDoesNotAuthenticate() {
        when(totp.isEnrolled(1L)).thenReturn(false);
        when(totp.beginEnrollment("alice")).thenReturn(new TotpService.Enrollment("SECRET", "data:image/png;base64,abc", "SECR ET"));

        var response = controller.login(new LoginRequest("alice", "password"), request);
        var body = (Map<?, ?>) response.getBody();

        assertEquals(true, body.get("mfaRequired"));
        assertEquals(true, body.get("enrollmentRequired"));
        assertEquals("totp", body.get("mfaMethod"));
        assertEquals("data:image/png;base64,abc", body.get("qrCode"));
        assertNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test
    void enrollmentConfirmationAuthenticatesAndReturnsRecoveryCodes() {
        when(totp.isEnrolled(1L)).thenReturn(false);
        when(totp.beginEnrollment("alice")).thenReturn(new TotpService.Enrollment("SECRET", "qr", "SECRET"));
        when(totp.enroll(1L, "SECRET", "123456")).thenReturn(List.of("RECOVERY01", "RECOVERY02"));
        controller.login(new LoginRequest("alice", "password"), request);

        var response = controller.verifyTotp(new LoginController.TotpVerification("123456"), request);
        var body = (Map<?, ?>) response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("RECOVERY01", "RECOVERY02"), body.get("recoveryCodes"));
        assertNotNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test
    void enrolledAccountRequiresValidCode() {
        when(totp.isEnrolled(1L)).thenReturn(true);
        controller.login(new LoginRequest("alice", "password"), request);
        when(totp.verify(1L, "123456")).thenReturn(true);

        var response = controller.verifyTotp(new LoginController.TotpVerification("123456"), request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(request.getSession().getAttribute(LoginController.AUTHENTICATED_USER));
    }

    @Test
    void fiveInvalidCodesConsumeThePendingLogin() {
        when(totp.isEnrolled(1L)).thenReturn(true);
        when(totp.verify(1L, "000000")).thenReturn(false);
        controller.login(new LoginRequest("alice", "password"), request);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertEquals(401, controller.verifyTotp(new LoginController.TotpVerification("000000"), request).getStatusCode().value());
        }
        assertEquals(429, controller.verifyTotp(new LoginController.TotpVerification("000000"), request).getStatusCode().value());
        assertNull(request.getSession(false));
    }

    @Test
    void enablingDuoAndTotpTogetherFailsClosed() {
        var duo = (DuoService) ReflectionTestUtils.getField(controller, "duo");
        when(duo.isEnabled()).thenReturn(true);

        var response = controller.login(new LoginRequest("alice", "password"), request);

        assertEquals(503, response.getStatusCode().value());
        assertNull(request.getSession(false));
    }
}
