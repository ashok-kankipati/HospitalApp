package com.hospital.app.dto.visit;

import lombok.Data;

@Data
public class VisitNotesRequest {
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String symptoms;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String diagnosis;
    @jakarta.validation.constraints.Pattern(regexp="OPEN|LAB_REQUIRED|LAB_DONE|IN_PROGRESS|COMPLETED|CLOSED|CANCELLED", message="Select a valid option.")
    private String status;
    @jakarta.validation.constraints.Pattern(regexp="NORMAL|URGENT|EMERGENCY|HIGH|LOW", message="Select a valid option.")
    private String priority;
}
