package com.hospital.app.model.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column
    private String reference;

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
        if (paymentStatus == null) {
            paymentStatus = "PAID";
        }
    }
}
