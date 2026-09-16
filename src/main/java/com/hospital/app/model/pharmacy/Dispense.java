package com.hospital.app.model.pharmacy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispense")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dispense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "dispensed_by", nullable = false)
    private Long dispensedBy;

    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @PrePersist
    protected void onCreate() {
        if (dispensedAt == null) {
            dispensedAt = LocalDateTime.now();
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }
}
