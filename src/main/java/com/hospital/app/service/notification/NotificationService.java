package com.hospital.app.service.notification;

import com.hospital.app.model.notification.NotificationQueue;
import com.hospital.app.model.notification.NotificationSetting;
import com.hospital.app.model.User;
import com.hospital.app.repository.notification.NotificationQueueRepository;
import com.hospital.app.repository.notification.NotificationSettingRepository;
import com.hospital.app.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import jakarta.mail.internet.InternetAddress;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationService {
    @Autowired
    private NotificationSettingRepository settingRepository;

    @Autowired
    private NotificationQueueRepository queueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RoleEmailTemplate emailTemplate;

    @Value("${hospital.mail.from:no-reply@example.com}")
    private String fromAddress;

    @Value("${hospital.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${hospital.mail.provider:smtp}")
    private String mailProvider;

    @Value("${hospital.mail.brevo-api-key:}")
    private String brevoApiKey;

    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    public List<NotificationSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    public NotificationSetting upsertSetting(NotificationSetting setting) {
        return settingRepository
                .findByRoleAndEventTypeAndChannel(setting.getRole(), setting.getEventType(), setting.getChannel())
                .map(existing -> {
                    existing.setEnabled(setting.getEnabled());
                    return settingRepository.save(existing);
                })
                .orElseGet(() -> settingRepository.save(setting));
    }

    public void notifyByRole(String role, String eventType, String subject, String body) {
        String mappedRole = mapRole(role);
        if (!isEnabled(mappedRole, eventType, "EMAIL")) {
            return;
        }
        List<User> recipients = userRepository.findByRole(mappedRole);
        RoleEmailTemplate.EmailContent content = emailTemplate.render(mappedRole, eventType, subject, body);
        for (User user : recipients) {
            sendEmail(role, eventType, user.getEmail(), content);
        }
    }

    public void notifyRecipientByRole(String role, String eventType, String recipient, String subject, String body) {
        String mappedRole = mapRole(role);
        if (recipient == null || recipient.isBlank() || !isEnabled(mappedRole, eventType, "EMAIL")) {
            return;
        }
        RoleEmailTemplate.EmailContent content = emailTemplate.render(mappedRole, eventType, subject, body);
        sendEmail(role, eventType, recipient, content);
    }

    public void notifyRecipientWithAttachments(String recipient, String subject, String body, List<EmailAttachment> attachments) {
        RoleEmailTemplate.EmailContent content = emailTemplate.render("Patient", "BILLING_PAID", subject, body);
        sendEmailWithAttachments("Patient", "BILLING_PAID", recipient, content, attachments);
    }

    private String mapRole(String role) {
        if ("Lab".equalsIgnoreCase(role)) {
            return "Lab Technician";
        }
        if ("Billing".equalsIgnoreCase(role)) {
            return "Receptionist";
        }
        return role;
    }

    private boolean isEnabled(String role, String eventType, String channel) {
        java.util.List<String> candidateRoles = java.util.stream.Stream
                .of(role, mapRole(role))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        for (String candidate : candidateRoles) {
            var setting = settingRepository.findByRoleAndEventTypeAndChannel(candidate, eventType, channel);
            if (setting.isPresent()) {
                return setting.get().getEnabled();
            }
        }
        return true;
    }

    private void sendEmail(String role, String eventType, String to, RoleEmailTemplate.EmailContent content) {
        sendEmailWithAttachments(role, eventType, to, content, List.of());
    }

    private void sendEmailWithAttachments(String role, String eventType, String to,
                                          RoleEmailTemplate.EmailContent content, List<EmailAttachment> attachments) {
        NotificationQueue log = new NotificationQueue();
        log.setRole(role);
        log.setEventType(eventType);
        log.setChannel("EMAIL");
        log.setRecipient(to);
        log.setSubject(content.subject());
        log.setBody(content.plainText());

        try {
            if (!mailEnabled) {
                log.setStatus("PENDING");
                queueRepository.save(log);
                return;
            }
            if ("brevo".equalsIgnoreCase(mailProvider)) {
                sendBrevo(to, content, attachments);
            } else if ("smtp".equalsIgnoreCase(mailProvider)) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(content.subject());
                helper.setText(content.plainText(), content.html());
                if (attachments != null) {
                    for (EmailAttachment attachment : attachments) {
                        if (attachment == null || attachment.getContent() == null) {
                            continue;
                        }
                        String name = attachment.getFilename() == null ? "attachment.pdf" : attachment.getFilename();
                        String type = attachment.getContentType() == null ? "application/pdf" : attachment.getContentType();
                        helper.addAttachment(name, new ByteArrayResource(attachment.getContent()), type);
                    }
                }
                mailSender.send(message);
            } else {
                throw new IllegalStateException("Unsupported mail provider. Use smtp or brevo.");
            }
            log.setStatus("SENT");
            log.setSentAt(java.time.LocalDateTime.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.setStatus("FAILED");
            log.setErrorMessage("Email submission interrupted; check provider logs before retrying.");
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(classifyFailure(e));
        }
        queueRepository.save(log);
    }

    private String classifyFailure(Exception exception) {
        if (exception instanceof IllegalStateException) {
            return exception.getMessage();
        }
        if (exception instanceof org.springframework.mail.MailAuthenticationException) {
            return "SMTP authentication failed. Check MAIL_USERNAME and the provider app password.";
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (message.contains("timeout") || message.contains("connect")) {
            return "Could not connect to the mail provider. Check MAIL_HOST, MAIL_PORT and network access.";
        }
        if (message.contains("sender") || message.contains("from address")) {
            return "The mail provider rejected MAIL_FROM. Use the authenticated sender or a verified alias.";
        }
        if (message.contains("recipient") || message.contains("address")) {
            return "The mail provider rejected the recipient address.";
        }
        return "Email submission failed. Check the mail provider logs and configured credentials.";
    }

    private void sendBrevo(String to, RoleEmailTemplate.EmailContent content, List<EmailAttachment> attachments)
            throws Exception {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY is missing.");
        }
        InternetAddress address = new InternetAddress(fromAddress, true);
        address.validate();
        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("email", address.getAddress());
        if (address.getPersonal() != null) sender.put("name", address.getPersonal());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(Map.of("email", to)));
        payload.put("subject", content.subject());
        payload.put("textContent", content.plainText());
        payload.put("htmlContent", content.html());
        List<Map<String, String>> files = new ArrayList<>();
        if (attachments != null) {
            for (EmailAttachment attachment : attachments) {
                if (attachment == null || attachment.getContent() == null) continue;
                String name = attachment.getFilename() == null ? "attachment.pdf" : attachment.getFilename();
                files.add(Map.of("name", name,
                        "content", Base64.getEncoder().encodeToString(attachment.getContent())));
            }
        }
        if (!files.isEmpty()) payload.put("attachment", files);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(30))
                .header("api-key", brevoApiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                .build();
        // Do not retry automatically: a timeout can occur after the provider accepts the message.
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Brevo rejected email (HTTP " + response.statusCode()
                    + "). Check API key, verified sender, account limits and Brevo logs.");
        }
    }

}
