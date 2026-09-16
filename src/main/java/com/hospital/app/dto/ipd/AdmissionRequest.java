package com.hospital.app.dto.ipd;

import lombok.Data;

@Data
public class AdmissionRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long bedId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long patientId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long visitId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long doctorId;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;
}
