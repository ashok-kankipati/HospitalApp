package com.hospital.app.model.lab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lab_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_order_id", nullable = false)
    private Long labOrderId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(nullable = false)
    private String status;

    @Column(name = "result_value")
    private String resultValue;

    @Column(name = "result_unit")
    private String resultUnit;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "result_flag")
    private String resultFlag;

    @Column(name = "result_notes")
    private String resultNotes;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
    }
}
