package com.hospital.app.dto.lab;

import lombok.Data;

@Data
public class LabOrderItemResultRequest {
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String resultValue;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=50, message="This field is required.")
    private String resultUnit;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String referenceRange;
    @jakarta.validation.constraints.Pattern(regexp="|NORMAL|HIGH|LOW", message="Select a valid option.")
    private String resultFlag;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String resultNotes;
    @jakarta.validation.constraints.Pattern(regexp="PENDING|IN_PROGRESS|COMPLETED|CANCELLED", message="Select a valid option.")
    private String status;
}
