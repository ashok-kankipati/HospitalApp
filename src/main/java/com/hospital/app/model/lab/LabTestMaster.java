package com.hospital.app.model.lab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lab_tests_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabTestMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String testName;

    @Column(nullable = false, precision = 10, scale = 2)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal price;

    @Column(name = "normal_range_text")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String normalRangeText;

    @Column(name = "is_active")
    private Boolean isActive;
}
