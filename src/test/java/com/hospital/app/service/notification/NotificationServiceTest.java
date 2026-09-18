package com.hospital.app.service.notification;

import com.hospital.app.model.notification.NotificationQueue;
import com.hospital.app.repository.notification.NotificationQueueRepository;
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

    @BeforeEach void setup() {
        service = new NotificationService();
        client = mock(HttpClient.class);
        queue = mock(NotificationQueueRepository.class);
        ReflectionTestUtils.setField(service, "httpClient", client);
        ReflectionTestUtils.setField(service, "queueRepository", queue);
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
        assertEquals("Hello ?", payload.path("textContent").asText());
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
}
