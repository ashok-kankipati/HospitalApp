package com.hospital.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_medical_history")
public class PatientMedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "history_type", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String historyType;

    @Column(name = "description", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String description;

    @Column(name = "history_date")
    @com.hospital.app.validation.InputText(kind="pastDate", required=false, max=255, message="This field is required.")
    private String historyDate;

    @Column(name = "notes")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;

    @Column(name = "recorded_at")
    private String recordedAt;
}
