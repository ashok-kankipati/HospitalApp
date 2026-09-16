package com.hospital.app.dto.billing;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceUpiQrRequest {
    @com.hospital.app.validation.InputText(kind="upi", required=true, max=255, message="This field is required.")
    private String upiId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.01")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal amount;
}
