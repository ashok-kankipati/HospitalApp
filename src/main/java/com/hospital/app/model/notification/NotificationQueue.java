package com.hospital.app.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String channel;

    private String recipient;

    private String subject;

    @Column(columnDefinition = "TEXT")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String body;

    @Column(nullable = false)
    private String status;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (isRead == null) {
            isRead = false;
        }
    }
}
