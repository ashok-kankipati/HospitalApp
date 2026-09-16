package com.hospital.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_chronic_conditions")
public class PatientChronicCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    private Long conditionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "condition_name", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String conditionName;

    @Column(name = "diagnosed_date")
    @com.hospital.app.validation.InputText(kind="pastDate", required=false, max=255, message="This field is required.")
    private String diagnosedDate;

    @Column(name = "status", nullable = false)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="ACTIVE|INACTIVE|UNDER_OBSERVATION", message="Select a valid option.")
    private String status;

    @Column(name = "notes")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;

    @Column(name = "recorded_at")
    private String recordedAt;
}
