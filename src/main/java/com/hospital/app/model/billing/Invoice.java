package com.hospital.app.model.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal tax;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "balance_due", nullable = false)
    private BigDecimal balanceDue;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
