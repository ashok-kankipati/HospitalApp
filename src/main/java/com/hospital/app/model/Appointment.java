package com.hospital.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long patientId;

    @Column(name = "staff_id", nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long staffId;

    @Column(name = "appointment_date", nullable = false)
    @com.hospital.app.validation.InputText(kind="date", required=true, max=255, message="This field is required.")
    private String appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    @com.hospital.app.validation.InputText(kind="time", required=true, max=255, message="This field is required.")
    private String appointmentTime;

    @Column(name = "reason")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String reason;

    @Column(name = "status", nullable = false)
    @jakarta.validation.constraints.Pattern(regexp="PENDING|CONFIRMED|COMPLETED|CANCELLED|NO_SHOW", message="Select a valid option.")
    private String status;

    @Column(name = "notes")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
