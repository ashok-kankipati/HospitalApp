package com.hospital.app.model.ipd;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "beds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bed_number", nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=50, message="This field is required.")
    private String bedNumber;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=100, message="This field is required.")
    private String ward;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="GENERAL|ICU|PRIVATE", message="Select a valid option.")
    private String type;

    @Column(nullable = false)
    @jakarta.validation.constraints.Pattern(regexp="AVAILABLE|OCCUPIED|MAINTENANCE", message="Select a valid option.")
    private String status;

    @Column(name = "daily_charge", nullable = false, precision = 10, scale = 2)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private BigDecimal dailyCharge;
}
