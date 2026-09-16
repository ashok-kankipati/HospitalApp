package com.hospital.app.controller.lab;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class WhatsAppControllerTest {
    @Test void appointmentMessageUsesSavedSchedule() {
        var appointment = new com.hospital.app.model.Appointment();
        appointment.setId(42L);
        appointment.setAppointmentDate("2026-11-12");
        appointment.setAppointmentTime("10:30");
        String text = WhatsAppController.appointmentText(appointment);
        assertTrue(text.contains("2026-11-12"));
        assertTrue(text.contains("10:30"));
        assertTrue(text.contains("#42"));
        assertFalse(text.contains("Reply C"));
    }
    @Test void trialTemplateOmitsAllCustomContent() {
        var sid = "HXfe5ab5f00277942d4d4200328b4d403c";
        assertEquals(java.util.Map.of("ContentSid", sid), WhatsAppController.messageContent(sid, "private results", false));
        assertEquals(java.util.Map.of("Body", "results"), WhatsAppController.messageContent("", "results", true));
        assertThrows(ResponseStatusException.class, () -> WhatsAppController.messageContent("", "results", false));
        assertThrows(ResponseStatusException.class, () -> WhatsAppController.messageContent("bad", "", false));
    }
    @Test void previewRedactsCredentialsPhoneAndUrls() {
        String preview = WhatsAppController.responsePreview(
            "<html><h1>Bad Request</h1>Unsupported media secret-value +919876543210 https://host/report?token=private</html>",
            "secret-value");
        assertTrue(preview.contains("Bad Request"));
        assertTrue(preview.contains("Unsupported media"));
        assertFalse(preview.contains("secret-value"));
        assertFalse(preview.contains("9876543210"));
        assertFalse(preview.contains("token=private"));
        assertEquals("Empty response body.", WhatsAppController.responsePreview(""));
        assertEquals(600, WhatsAppController.responsePreview("word ".repeat(200)).length());
    }
    @Test void rejectionSupportsStringCodesAndXmlWithoutExposingBodies() {
        assertTrue(WhatsAppController.rejectionMessage(400, "{\"code\":\"63007\"}").contains("error 63007"));
        String result = WhatsAppController.rejectionMessage(400,
            "<TwilioResponse><RestException><Code>21606</Code><Message>private data</Message></RestException></TwilioResponse>");
        assertTrue(result.contains("error 21606"));
        assertFalse(result.contains("private data"));
    }
    @Test void rejectionShowsOnlyStatusAndNumericCode() {
        String message = WhatsAppController.rejectionMessage(400,
            "{\"code\":63007,\"message\":\"private patient and credential data\"}");
        assertTrue(message.contains("HTTP 400, error 63007"));
        assertTrue(message.contains("https://www.twilio.com/docs/api/errors/63007"));
        assertFalse(message.contains("private"));
        assertFalse(WhatsAppController.rejectionMessage(401, "private non-JSON response").contains("private"));
        assertFalse(WhatsAppController.rejectionMessage(400, "{\"code\":\"secret\"}").contains("secret"));
    }
    @Test void mediaRejectsExpiredAndForgedLinks() {
        var env = new MockEnvironment().withProperty("hospital.whatsapp.enabled", "true")
            .withProperty("hospital.whatsapp.signing-secret", "a-test-secret-that-is-at-least-32-characters");
        var controller = new WhatsAppController(env, null, null, null, null, null);
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> controller.media(1L, 1L, "forged")).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> controller.media(1L, java.time.Instant.now().plusSeconds(3600).getEpochSecond(), "forged")).getStatusCode().value());
    }
    @Test void validMediaLinkReturnsPdf() throws Exception {
        var env = new MockEnvironment().withProperty("hospital.whatsapp.enabled", "true")
            .withProperty("hospital.whatsapp.signing-secret", "a-test-secret-that-is-at-least-32-characters");
        var lab = org.mockito.Mockito.mock(com.hospital.app.service.lab.LabService.class);
        org.mockito.Mockito.when(lab.generateLabReportPdf(1L)).thenReturn(new byte[]{1,2,3});
        var controller = new WhatsAppController(env, null, null, null, lab, null);
        long expires = java.time.Instant.now().plusSeconds(3600).getEpochSecond();
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("a-test-secret-that-is-at-least-32-characters".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("1:" + expires).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var response = controller.media(1L, expires, token);
        assertArrayEquals(new byte[]{1,2,3}, response.getBody());
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
        assertThrows(ResponseStatusException.class, () -> controller.media(2L, expires, token));
    }
    @Test void normalizesIndianAndInternationalNumbers() {
        assertEquals("+919876543210", WhatsAppController.normalizePhone("98765 43210"));
        assertEquals("+14155552671", WhatsAppController.normalizePhone("+1 (415) 555-2671"));
        assertThrows(ResponseStatusException.class, () -> WhatsAppController.normalizePhone(null));
        assertThrows(ResponseStatusException.class, () -> WhatsAppController.normalizePhone("123"));
    }
    @Test void disabledIntegrationCannotSend() {
        var controller = new WhatsAppController(new MockEnvironment(), null, null, null, null, null);
        var error = assertThrows(ResponseStatusException.class, () -> controller.send(1L, "password", new WhatsAppController.SendRequest(true, true)));
        assertEquals(503, error.getStatusCode().value());
    }
    @Test void missingOperatorPasswordCannotSend() {
        var env = new MockEnvironment().withProperty("hospital.whatsapp.enabled", "true");
        var controller = new WhatsAppController(env, null, null, null, null, null);
        var error = assertThrows(ResponseStatusException.class, () -> controller.send(1L, null, new WhatsAppController.SendRequest(true, true)));
        assertEquals(401, error.getStatusCode().value());
    }
}
