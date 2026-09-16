package com.hospital.app.dto.billing;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceGenerateRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long appointmentId;
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal consultationFee;
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal labFee;
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal tax;
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal discount;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String notes;
}
