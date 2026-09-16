package com.hospital.app.dto.pharmacy;

import lombok.Data;

import java.util.List;

@Data
public class DispenseCreateRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long prescriptionId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long dispensedBy;
    @jakarta.validation.constraints.NotEmpty
    @jakarta.validation.constraints.Size(max=100)
    @jakarta.validation.Valid
    private List<@jakarta.validation.constraints.NotNull DispenseItemRequest> items;
}
