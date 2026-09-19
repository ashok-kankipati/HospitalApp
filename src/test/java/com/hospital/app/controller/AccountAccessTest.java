package com.hospital.app.controller;

import com.hospital.app.config.AuthenticationFilter;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.model.User;
import com.hospital.app.repository.UserRepository;
import com.hospital.app.service.AccountService;
import com.hospital.app.service.AccountPasswords;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountAccessTest {
    UserRepository users = mock(UserRepository.class);
    User user(String role) { var u = new User(); u.setId(1L); u.setUsername("admin"); u.setRole(role); u.setIsActive(true); return u; }
    int request(String path, User current) throws Exception {
        var filter = new AuthenticationFilter(); ReflectionTestUtils.setField(filter, "users", users);
        var request = new MockHttpServletRequest("POST", path); request.setServletPath(path);
        request.getSession().setAttribute(LoginController.ACCOUNT_ID, 1L);
        request.getSession().setAttribute(LoginController.AUTHENTICATED_USER, new LoginResponse(true, "", "admin", "Admin", ""));
        when(users.findById(1L)).thenReturn(Optional.ofNullable(current));
        var response = new MockHttpServletResponse(); filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
    @Test void liveRoleOverridesStaleAdministratorSession() throws Exception {
        assertEquals(403, request("/api/admin/accounts", user("Doctor")));
        assertEquals(403, request("/api/auth/register", user("Doctor")));
        assertEquals(200, request("/api/admin/accounts", user("Admin")));
    }
    @Test void deletedAndInactiveAccountsLoseAccess() throws Exception {
        assertEquals(401, request("/api/patients", null));
        var inactive = user("Admin"); inactive.setIsActive(false);
        assertEquals(401, request("/api/patients", inactive));
    }
    @Test void administratorCannotDeleteOrDemoteSelf() {
        when(users.lockActiveAdmins()).thenReturn(List.of(user("Admin")));
        when(users.findById(1L)).thenReturn(Optional.of(user("Admin")));
        var service = new AccountService(users);
        assertThrows(ResponseStatusException.class, () -> service.delete(1L, 1L));
        assertThrows(ResponseStatusException.class, () -> service.changeRole(1L, 1L, "Doctor"));
        verify(users, never()).delete(any());
    }
    @Test void creationHashesPasswordAndRejectsUnknownRoles() {
        when(users.lockActiveAdmins()).thenReturn(List.of(user("Admin")));
        when(users.saveAndFlush(any())).thenAnswer(invocation -> { User u = invocation.getArgument(0); u.setId(2L); return u; });
        var service = new AccountService(users);
        var account = service.create(1L, new AccountService.CreateAccount("doctor", "d@example.test", "Strong-test-password", "Doctor"));
        assertEquals("Doctor", account.role());
        var captured = org.mockito.ArgumentCaptor.forClass(User.class); verify(users).saveAndFlush(captured.capture());
        assertTrue(AccountPasswords.isEncoded(captured.getValue().getPassword()));
        assertTrue(AccountPasswords.matches("Strong-test-password", captured.getValue().getPassword()));
        assertFalse(AccountPasswords.matches("incorrect", captured.getValue().getPassword()));
        assertThrows(ResponseStatusException.class, () -> service.create(1L, new AccountService.CreateAccount("x", "x@y.test", "password", "Superuser")));
    }

    @Test void administratorCanSetOnlyTheAccountPassword() {
        User admin = user("Admin");
        User doctor = new User();
        doctor.setId(2L);
        doctor.setUsername("doctor.one");
        doctor.setEmail("doctor@example.test");
        doctor.setRole("Doctor");
        doctor.setPassword(AccountPasswords.encode("old-password"));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.findById(2L)).thenReturn(Optional.of(doctor));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new AccountService(users).resetPassword(1L, 2L, "new-secure-password");

        assertTrue(AccountPasswords.matches("new-secure-password", doctor.getPassword()));
        assertEquals("doctor.one", doctor.getUsername());
        assertEquals("doctor@example.test", doctor.getEmail());
        assertEquals("Doctor", doctor.getRole());
        verify(users).save(doctor);
    }
}
