package com.hospital.app.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String role;

    @Column(name = "event_type", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String eventType;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="EMAIL|SMS|IN_APP|WHATSAPP", message="Select a valid option.")
    private String channel;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    private Boolean enabled;
}
