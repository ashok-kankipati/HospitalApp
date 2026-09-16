package com.hospital.app.validation;

import com.hospital.app.controller.PatientController;
import com.hospital.app.controller.AppointmentController;
import com.hospital.app.controller.billing.InvoiceController;
import com.hospital.app.model.Patient;
import com.hospital.app.service.PatientService;
import com.hospital.app.service.AppointmentService;
import com.hospital.app.service.billing.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidationEndpointTest {
    @Test void invalidPatientNeverReachesPersistence() throws Exception {
        var controller = new PatientController(); var service = mock(PatientService.class); ReflectionTestUtils.setField(controller, "patientService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ValidationErrors(), new NormalizeInput()).build();
        mvc.perform(post("/api/patients").sessionAttr(com.hospital.app.controller.LoginController.AUTHENTICATED_USER, new com.hospital.app.dto.LoginResponse(true, "ok", "admin", "Admin", "admin@example.test")).contentType("application/json").content("{\"name\":\"  \",\"email\":\"invalid\",\"phone\":\"123\",\"dateOfBirth\":\"2025-02-29\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").exists()).andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
        verifyNoInteractions(service);
    }
    @Test void validPatientIsTrimmedBeforeSaving() throws Exception {
        var controller = new PatientController(); var service = mock(PatientService.class); ReflectionTestUtils.setField(controller, "patientService", service);
        when(service.addPatient(any())).thenAnswer(call -> call.getArgument(0));
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ValidationErrors(), new NormalizeInput()).build();
        mvc.perform(post("/api/patients").sessionAttr(com.hospital.app.controller.LoginController.AUTHENTICATED_USER, new com.hospital.app.dto.LoginResponse(true, "ok", "admin", "Admin", "admin@example.test")).contentType("application/json").content("{\"name\":\"  Li  \",\"email\":\" li@example.test \",\"phone\":\"+919876543210\",\"dateOfBirth\":\"2000-02-29\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Li"));
        var capture = org.mockito.ArgumentCaptor.forClass(Patient.class); verify(service).addPatient(capture.capture()); assertEquals("li@example.test", capture.getValue().getEmail());
    }
    @Test void pastAppointmentRejectedBeforeServiceCall() throws Exception {
        var controller = new AppointmentController(); var service = mock(AppointmentService.class); ReflectionTestUtils.setField(controller, "appointmentService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ValidationErrors()).build();
        mvc.perform(post("/api/appointments").contentType("application/json").content("{\"patientId\":1,\"staffId\":1,\"appointmentDate\":\"2000-01-01\",\"appointmentTime\":\"12:00\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Choose an appointment date and time in the future."));
        verifyNoInteractions(service);
    }
    @Test void invalidPaymentAndMalformedJsonReturnHelpfulErrors() throws Exception {
        var controller = new InvoiceController(); var service = mock(InvoiceService.class); ReflectionTestUtils.setField(controller, "invoiceService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ValidationErrors()).build();
        mvc.perform(post("/api/billing/invoices/1/payments").contentType("application/json").content("{\"amount\":-1,\"paymentMethod\":\"Cash\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.amount").exists());
        mvc.perform(post("/api/billing/invoices/1/payments").contentType("application/json").content("{\"amount\":\"not a number\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(service);
    }
}
