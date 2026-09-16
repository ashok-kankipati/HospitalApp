package com.hospital.app.controller.notification;

import com.hospital.app.model.notification.NotificationSetting;
import com.hospital.app.model.notification.NotificationQueue;
import com.hospital.app.service.notification.NotificationService;
import com.hospital.app.repository.notification.NotificationQueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationQueueRepository queueRepository;

    @GetMapping("/settings")
    public ResponseEntity<List<NotificationSetting>> getSettings() {
        return ResponseEntity.ok(notificationService.getAllSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSetting> updateSetting(@jakarta.validation.Valid @RequestBody NotificationSetting setting) {
        return ResponseEntity.ok(notificationService.upsertSetting(setting));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<NotificationQueue>> getQueue(jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(queueRepository.findTop50ByOrderByCreatedAtDesc().stream().filter(item -> allowed(request, item)).toList());
    }

    @PutMapping("/queue/{id}/read")
    public ResponseEntity<NotificationQueue> markRead(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        NotificationQueue item = queueRepository.findById(id).orElseThrow();
        if (!allowed(request, item)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        item.setIsRead(true);
        return ResponseEntity.ok(queueRepository.save(item));
    }
    private boolean allowed(jakarta.servlet.http.HttpServletRequest request, NotificationQueue item) {
        var login = (com.hospital.app.dto.LoginResponse) request.getSession().getAttribute(com.hospital.app.controller.LoginController.AUTHENTICATED_USER);
        String role = login.getRole();
        return "Admin".equals(role) || role.equals(item.getRole())
                || ("Lab Technician".equals(role) && "Lab".equals(item.getRole()))
                || ("Receptionist".equals(role) && "Billing".equals(item.getRole()));
    }
}
