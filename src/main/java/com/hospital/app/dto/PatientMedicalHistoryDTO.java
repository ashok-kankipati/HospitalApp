package com.hospital.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalHistoryDTO {
    private Long historyId;
    private Long patientId;
    private String historyType;
    private String description;
    private String historyDate;
    private String notes;
    private String recordedAt;
}
