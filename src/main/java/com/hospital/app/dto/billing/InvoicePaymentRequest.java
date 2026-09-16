package com.hospital.app.dto.billing;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoicePaymentRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.01")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal amount;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="Cash|Credit Card|Debit Card|UPI|Bank Transfer|Other", message="Select a valid option.")
    private String paymentMethod;
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String reference;
}
