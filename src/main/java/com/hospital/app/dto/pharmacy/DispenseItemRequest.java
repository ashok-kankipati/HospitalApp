package com.hospital.app.dto.pharmacy;

import lombok.Data;

@Data
public class DispenseItemRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long prescriptionItemId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long medicineBatchId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer quantity;
}
