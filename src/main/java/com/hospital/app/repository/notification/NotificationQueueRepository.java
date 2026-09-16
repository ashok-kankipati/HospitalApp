package com.hospital.app.repository.notification;

import com.hospital.app.model.notification.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, Long> {
    List<NotificationQueue> findByStatus(String status);
    List<NotificationQueue> findTop50ByOrderByCreatedAtDesc();
    List<NotificationQueue> findTop50ByIsReadFalseOrderByCreatedAtDesc();
}
