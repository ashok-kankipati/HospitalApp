package com.hospital.app.repository.notification;

import com.hospital.app.model.notification.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    List<NotificationSetting> findByRole(String role);
    Optional<NotificationSetting> findByRoleAndEventTypeAndChannel(String role, String eventType, String channel);
}
