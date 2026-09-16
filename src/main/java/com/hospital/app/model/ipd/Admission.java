package com.hospital.app.model.ipd;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bed_id", nullable = false)
    private Long bedId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "admitted_at")
    private LocalDateTime admittedAt;

    @Column(name = "discharged_at")
    private LocalDateTime dischargedAt;

    @Column(nullable = false)
    private String status;

    @Column
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (admittedAt == null) {
            admittedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
