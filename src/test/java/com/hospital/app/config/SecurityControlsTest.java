package com.hospital.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityControlsTest {
    @Test void changingPatientInUrlCannotModifyAnotherPatientsAllergy() {
        var controller = new com.hospital.app.controller.PatientController();
        var service = org.mockito.Mockito.mock(com.hospital.app.service.PatientAllergyService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "allergyService", service);
        var record = new com.hospital.app.model.PatientAllergy(); record.setPatientId(99L);
        org.mockito.Mockito.when(service.getAllergyById(7L)).thenReturn(java.util.Optional.of(record));
        assertEquals(404, controller.updateAllergy(1L, 7L, record).getStatusCode().value());
        assertEquals(404, controller.deleteAllergy(1L, 7L).getStatusCode().value());
        org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).updateAllergy(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).deleteAllergy(org.mockito.ArgumentMatchers.anyLong());
    }
    @Test void createGuardRejectsInjectedPrimaryKey() throws Exception {
        var guard = new EntityCreateGuard();
        var patient = new com.hospital.app.model.Patient(); patient.setId(99L);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> guard.afterBodyRead(patient, null, null, null, null));
        patient.setId(null);
        assertSame(patient, guard.afterBodyRead(patient, null, null, null, null));
    }
    @Test void receptionistResponseOmitsMedicalHistoryWithoutModifyingEntity() {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(com.hospital.app.controller.LoginController.AUTHENTICATED_USER,
                new com.hospital.app.dto.LoginResponse(true, "", "reception", "Receptionist", ""));
        var patient = new com.hospital.app.model.Patient(); patient.setId(1L); patient.setMedicalHistory("private notes");
        var result = new SensitiveResponseAdvice().beforeBodyWrite(patient, null, null, null,
                new org.springframework.http.server.ServletServerHttpRequest(request), null);
        assertFalse(((java.util.Map<?, ?>) result).containsKey("medicalHistory"));
        assertEquals("private notes", patient.getMedicalHistory());
    }
    @Test void permissionsDenyUnknownAndPatientAccountsAndRestrictMutations() {
        for (String role : new String[]{"Patient", "Superuser", "ADMIN", ""}) assertFalse(ApiPermissions.allows(role, "/api/patients", "GET"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/admin/accounts", "POST"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/billing/invoices/1/payments", "POST"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/pharmacy/medicines", "GET"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/lab/orders", "GET"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/beds/summary", "GET"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/admissions", "GET"));
        assertFalse(ApiPermissions.allows("Receptionist", "/api/patients/1/medical-history", "GET"));
        assertFalse(ApiPermissions.allows("Pharmacist", "/api/pharmacy/prescriptions", "POST"));
        assertFalse(ApiPermissions.allows("Nurse", "/api/staff/1", "DELETE"));
        assertFalse(ApiPermissions.allows("Doctor", "/api/unknown-new-module", "GET"));
        assertTrue(ApiPermissions.allows("Doctor", "/api/visits/1/notes", "PUT"));
        assertTrue(ApiPermissions.allows("Receptionist", "/api/billing/invoices/1/payments", "POST"));
        assertTrue(ApiPermissions.allows("Pharmacist", "/api/pharmacy/dispense", "POST"));
        assertTrue(ApiPermissions.allows("Lab Technician", "/api/lab/orders", "GET"));
        assertTrue(ApiPermissions.allows("Receptionist", "/api/beds/summary", "GET"));
        assertTrue(ApiPermissions.allows("Admin", "/api/admin/accounts", "DELETE"));
    }
    @Test void crossSiteFormCannotMutateWithoutCustomHeader() throws Exception {
        var filter = new BrowserSecurityFilter();
        var request = new MockHttpServletRequest("POST", "/api/auth/login"); request.setServletPath("/api/auth/login");
        var rejected = new MockHttpServletResponse(); filter.doFilter(request, rejected, new MockFilterChain());
        assertEquals(403, rejected.getStatus());
        request.addHeader("X-Requested-With", "Careflow");
        var accepted = new MockHttpServletResponse(); filter.doFilter(request, accepted, new MockFilterChain());
        assertEquals(200, accepted.getStatus());
        assertEquals("nosniff", accepted.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", accepted.getHeader("X-Frame-Options"));
        assertTrue(accepted.getHeader("Content-Security-Policy").contains("frame-ancestors 'none'"));
    }
    @Test void rateLimitExpiresAndDoesNotTrustClientForwardedHeaders() {
        var limiter = new LoginRateLimitFilter();
        for (int i=0; i<30; i++) assertTrue(limiter.allow("127.0.0.1", 1));
        assertFalse(limiter.allow("127.0.0.1", 2));
        assertTrue(limiter.allow("127.0.0.1", 300_001));
    }
}
