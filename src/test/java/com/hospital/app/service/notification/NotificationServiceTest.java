package com.hospital.app.service.notification;

import com.hospital.app.model.notification.NotificationQueue;
import com.hospital.app.model.User;
import com.hospital.app.repository.notification.NotificationQueueRepository;
import com.hospital.app.repository.notification.NotificationSettingRepository;
import com.hospital.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    private NotificationService service;
    private HttpClient client;
    private NotificationQueueRepository queue;
    private NotificationSettingRepository settings;
    private UserRepository users;

    @BeforeEach void setup() {
        service = new NotificationService();
        client = mock(HttpClient.class);
        queue = mock(NotificationQueueRepository.class);
        settings = mock(NotificationSettingRepository.class);
        users = mock(UserRepository.class);
        ReflectionTestUtils.setField(service, "httpClient", client);
        ReflectionTestUtils.setField(service, "queueRepository", queue);
        ReflectionTestUtils.setField(service, "settingRepository", settings);
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "emailTemplate", new RoleEmailTemplate());
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "mailProvider", "brevo");
        ReflectionTestUtils.setField(service, "brevoApiKey", "test-key");
        ReflectionTestUtils.setField(service, "fromAddress", "CareFlow <sender@example.com>");
    }

    private NotificationQueue log() {
        var capture = ArgumentCaptor.forClass(NotificationQueue.class);
        verify(queue).save(capture.capture());
        return capture.getValue();
    }

    @Test void sendsPdfAndUnicodeTextOverHttps() throws Exception {
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        service.notifyRecipientWithAttachments("patient@example.com", "Invoice", "Hello ?",
                List.of(new EmailAttachment("invoice.pdf", new byte[]{1, 2, 3}, "application/pdf")));
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(capture.capture(), any(HttpResponse.BodyHandler.class));
        var request = capture.getValue();
        assertEquals("https://api.brevo.com/v3/smtp/email", request.uri().toString());
        assertEquals("test-key", request.headers().firstValue("api-key").orElseThrow());
        var bytes = new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer b) { byte[] data = new byte[b.remaining()]; b.get(data); bytes.writeBytes(data); }
            public void onError(Throwable t) { fail(t); }
            public void onComplete() {}
        });
        var payload = new ObjectMapper().readTree(bytes.toString(StandardCharsets.UTF_8));
        assertEquals("sender@example.com", payload.at("/sender/email").asText());
        assertEquals("CareFlow", payload.at("/sender/name").asText());
        assertEquals("patient@example.com", payload.at("/to/0/email").asText());
        assertTrue(payload.path("textContent").asText().contains("Hello ?"));
        assertTrue(payload.path("htmlContent").asText().contains("Billing update"));
        assertEquals("AQID", payload.at("/attachment/0/content").asText());
        assertEquals("invoice.pdf", payload.at("/attachment/0/name").asText());
        assertEquals("SENT", log().getStatus());
    }

    @Test void rejectionIsRecordedWithoutResponseBody() throws Exception {
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        service.notifyRecipientWithAttachments("patient@example.com", "Test", "Body", null);
        var entry = log();
        assertEquals("FAILED", entry.getStatus());
        assertTrue(entry.getErrorMessage().contains("401"));
        assertFalse(entry.getErrorMessage().contains("test-key"));
        assertNull(entry.getSentAt());
    }

    @Test void disabledMailDoesNotSend() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);
        service.notifyRecipientWithAttachments("patient@example.com", "Test", "Body", null);
        verifyNoInteractions(client);
        assertEquals("PENDING", log().getStatus());
    }

    @Test void missingKeyDoesNotSend() {
        ReflectionTestUtils.setField(service, "brevoApiKey", "");
        service.notifyRecipientWithAttachments("patient@example.com", "Test", "Body", null);
        verifyNoInteractions(client);
        assertEquals("FAILED", log().getStatus());
    }

    @Test void mappedRolesUseActualRecipientRolesForNotificationSettings() throws Exception {
        var labSetting = new com.hospital.app.model.notification.NotificationSetting();
        labSetting.setRole("Lab Technician");
        labSetting.setEventType("LAB_REPORT_READY");
        labSetting.setChannel("EMAIL");
        labSetting.setEnabled(true);
        when(settings.findByRoleAndEventTypeAndChannel("Lab Technician", "LAB_REPORT_READY", "EMAIL"))
                .thenReturn(java.util.Optional.of(labSetting));

        var labUser = new User();
        labUser.setEmail("lab@example.com");
        when(users.findByRole("Lab Technician")).thenReturn(List.of(labUser));

        var billingSetting = new com.hospital.app.model.notification.NotificationSetting();
        billingSetting.setRole("Receptionist");
        billingSetting.setEventType("BILLING_REMINDER");
        billingSetting.setChannel("EMAIL");
        billingSetting.setEnabled(true);
        when(settings.findByRoleAndEventTypeAndChannel("Receptionist", "BILLING_REMINDER", "EMAIL"))
                .thenReturn(java.util.Optional.of(billingSetting));

        var receptionist = new User();
        receptionist.setEmail("reception@example.com");
        when(users.findByRole("Receptionist")).thenReturn(List.of(receptionist));

        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        service.notifyByRole("Lab", "LAB_REPORT_READY", "Lab Report Ready", "Body");
        service.notifyByRole("Billing", "BILLING_REMINDER", "Billing Reminder", "Body");

        verify(settings).findByRoleAndEventTypeAndChannel("Lab Technician", "LAB_REPORT_READY", "EMAIL");
        verify(settings).findByRoleAndEventTypeAndChannel("Receptionist", "BILLING_REMINDER", "EMAIL");
        verify(users).findByRole("Lab Technician");
        verify(users).findByRole("Receptionist");
    }

    @Test void roleTemplatesProvideDifferentClinicalActionsAndEscapeContent() {
        var template = new RoleEmailTemplate();
        var doctor = template.render("Doctor", "LAB_REPORT_READY", "Old subject", "Visit <V001> is ready.");
        var lab = template.render("Lab Technician", "LAB_REPORT_READY", "Old subject", "Visit <V001> is ready.");
        var billing = template.render("Receptionist", "BILLING_REMINDER", "Old subject", "Invoice INV-1 was created.");

        assertEquals("Lab report ready for review", doctor.subject());
        assertTrue(doctor.plainText().contains("update the patient care plan"));
        assertTrue(lab.plainText().contains("Confirm the report is complete"));
        assertEquals("New invoice ready for billing", billing.subject());
        assertTrue(doctor.html().contains("Visit &lt;V001&gt; is ready."));
        assertFalse(doctor.html().contains("Visit <V001>"));
    }

    @Test void smtpPreservesUnicodeAndPdfAttachment() throws Exception {
        var sender = mock(org.springframework.mail.javamail.JavaMailSender.class);
        var message = new jakarta.mail.internet.MimeMessage(
                jakarta.mail.Session.getInstance(new java.util.Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "mailProvider", "smtp");
        ReflectionTestUtils.setField(service, "mailSender", sender);
        String body = "Payment received: \u20B9100";
        byte[] pdf = new byte[]{1, 2, 3};
        service.notifyRecipientWithAttachments("patient@example.com", "Invoice", body,
                List.of(new EmailAttachment("invoice.pdf", pdf, "application/pdf")));
        verify(sender).send(message);
        verifyNoInteractions(client);
        message.saveChanges();
        var received = new jakarta.mail.internet.MimeMessage(
                jakarta.mail.Session.getInstance(new java.util.Properties()), serialized(message));
        assertEquals("patient@example.com", received.getAllRecipients()[0].toString());
        assertEquals("Invoice", received.getSubject());
        var mixed = (jakarta.mail.Multipart) received.getContent();
        var related = (jakarta.mail.Multipart) mixed.getBodyPart(0).getContent();
        var alternative = (jakarta.mail.Multipart) related.getBodyPart(0).getContent();
        assertTrue(alternative.getBodyPart(0).getContent().toString().contains(body));
        assertTrue(alternative.getBodyPart(1).getContent().toString().contains("Billing update"));
        assertEquals("invoice.pdf", mixed.getBodyPart(1).getFileName());
        assertArrayEquals(pdf, mixed.getBodyPart(1).getInputStream().readAllBytes());
        assertEquals("SENT", log().getStatus());
    }

    private java.io.InputStream serialized(jakarta.mail.internet.MimeMessage message) throws Exception {
        var bytes = new ByteArrayOutputStream();
        message.writeTo(bytes);
        return new java.io.ByteArrayInputStream(bytes.toByteArray());
    }

    @Test void smtpFailureIsRecordedWithoutLeakingCredentials() {
        var sender = mock(org.springframework.mail.javamail.JavaMailSender.class);
        ReflectionTestUtils.setField(service, "mailProvider", "smtp");
        ReflectionTestUtils.setField(service, "mailSender", sender);
        when(sender.createMimeMessage()).thenThrow(
                new org.springframework.mail.MailAuthenticationException("sensitive-password"));
        service.notifyRecipientWithAttachments("patient@example.com", "Invoice", "Body", null);
        var entry = log();
        assertEquals("FAILED", entry.getStatus());
        assertFalse(entry.getErrorMessage().contains("sensitive-password"));
        assertNull(entry.getSentAt());
        verifyNoInteractions(client);
    }

}
