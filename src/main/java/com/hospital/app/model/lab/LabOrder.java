package com.hospital.app.model.lab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lab_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "ordered_by_doctor_id", nullable = false)
    private Long orderedByDoctorId;

    @Column(nullable = false)
    private String status;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    private String notes;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
    }
}
