package com.hospital.app.dto.lab;

import lombok.Data;

import java.util.List;

@Data
public class LabOrderRequest {
    @jakarta.validation.constraints.Positive
    private Long orderedByDoctorId;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;
    @jakarta.validation.constraints.NotEmpty
    @jakarta.validation.constraints.Size(max=100)
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> testIds;
    @jakarta.validation.constraints.Pattern(regexp="NORMAL|URGENT|EMERGENCY|HIGH|LOW", message="Select a valid option.")
    private String priority;
}
