package com.hospital.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientAllergyDTO {
    private Long allergyId;
    private Long patientId;
    private String allergyType;
    private String allergyName;
    private String reaction;
    private String severity;
    private String notes;
    private String recordedAt;
}
