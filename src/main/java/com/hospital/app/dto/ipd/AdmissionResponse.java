package com.hospital.app.dto.ipd;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdmissionResponse {
    private Long id;
    private Long bedId;
    private String bedNumber;
    private Long patientId;
    private Long visitId;
    private Long doctorId;
    private LocalDateTime admittedAt;
    private LocalDateTime dischargedAt;
    private String status;
    private String notes;
}
