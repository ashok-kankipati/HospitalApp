package com.hospital.app.dto.lab;

import lombok.Data;

@Data
public class LabOrderStatusRequest {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="ORDERED|PENDING|IN_PROGRESS|COMPLETED|CANCELLED|SAMPLE_COLLECTED", message="Select a valid option.")
    private String status;
}
