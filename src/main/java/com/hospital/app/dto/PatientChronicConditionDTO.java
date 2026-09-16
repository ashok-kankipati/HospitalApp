package com.hospital.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientChronicConditionDTO {
    private Long conditionId;
    private Long patientId;
    private String conditionName;
    private String diagnosedDate;
    private String status;
    private String notes;
    private String recordedAt;
}
