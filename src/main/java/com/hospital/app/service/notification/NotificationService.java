package com.hospital.app.service.notification;

import com.hospital.app.model.notification.NotificationQueue;
import com.hospital.app.model.notification.NotificationSetting;
import com.hospital.app.model.User;
import com.hospital.app.repository.notification.NotificationQueueRepository;
import com.hospital.app.repository.notification.NotificationSettingRepository;
import com.hospital.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Value("${hospital.mail.from:no-reply@example.com}")
    private String fromAddress;

    @Value("${hospital.mail.enabled:false}")
    private boolean mailEnabled;

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
        if (!isEnabled(role, eventType, "EMAIL")) {
            return;
        }
        String mappedRole = mapRole(role);
        List<User> recipients = userRepository.findByRole(mappedRole);
        for (User user : recipients) {
            sendEmail(role, eventType, user.getEmail(), subject, body);
        }
    }

    public void notifyRecipientWithAttachments(String recipient, String subject, String body, List<EmailAttachment> attachments) {
        sendEmailWithAttachments("Patient", "BILLING_PAID", recipient, subject, body, attachments);
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
        return settingRepository
                .findByRoleAndEventTypeAndChannel(role, eventType, channel)
                .map(NotificationSetting::getEnabled)
                .orElse(true);
    }

    private void sendEmail(String role, String eventType, String to, String subject, String body) {
        NotificationQueue log = new NotificationQueue();
        log.setRole(role);
        log.setEventType(eventType);
        log.setChannel("EMAIL");
        log.setRecipient(to);
        log.setSubject(subject);
        log.setBody(body);

        try {
            if (!mailEnabled) {
                log.setStatus("PENDING");
                queueRepository.save(log);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.setStatus("SENT");
            log.setSentAt(java.time.LocalDateTime.now());
        } catch (MessagingException e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
        }
        queueRepository.save(log);
    }

    private void sendEmailWithAttachments(String role, String eventType, String to, String subject, String body, List<EmailAttachment> attachments) {
        NotificationQueue log = new NotificationQueue();
        log.setRole(role);
        log.setEventType(eventType);
        log.setChannel("EMAIL");
        log.setRecipient(to);
        log.setSubject(subject);
        log.setBody(body);

        try {
            if (!mailEnabled) {
                log.setStatus("PENDING");
                queueRepository.save(log);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
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
            log.setStatus("SENT");
            log.setSentAt(java.time.LocalDateTime.now());
        } catch (MessagingException e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
        }
        queueRepository.save(log);
    }
}
