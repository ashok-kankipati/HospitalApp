package com.hospital.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_allergies")
public class PatientAllergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allergy_id")
    private Long allergyId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "allergy_type", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    @jakarta.validation.constraints.Pattern(regexp="Food|Medicine|Environmental|Other", message="Select a valid allergy type.")
    private String allergyType;

    @Column(name = "allergy_name", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String allergyName;

    @Column(name = "reaction")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String reaction;

    @Column(name = "severity")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    @jakarta.validation.constraints.Pattern(regexp="|Mild|Moderate|Severe", message="Select a valid severity.")
    private String severity;

    @Column(name = "notes")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;

    @Column(name = "recorded_at")
    private String recordedAt;
}
