package com.hospital.app.model.pharmacy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "dispense_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispenseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispense_id", nullable = false)
    private Long dispenseId;

    @Column(name = "medicine_batch_id", nullable = false)
    private Long medicineBatchId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal lineTotal;
}
