package com.hospital.app.dto.lab;

import lombok.Data;

@Data
public class LabReportRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long visitId;
    @jakarta.validation.constraints.Positive
    private Long labOrderId;
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String fileName;
    @com.hospital.app.validation.InputText(kind="url", required=true, max=255, message="This field is required.")
    private String fileUrl;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=100, message="This field is required.")
    private String mimeType;
}
