package com.hospital.app.model.pharmacy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "medicine_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medicine_id", nullable = false)
    @JsonIgnoreProperties({"batches"})
    @com.hospital.app.validation.EntityReference
    private Medicine medicine;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=100, message="This field is required.")
    private String batchNo;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    private LocalDate expiryDate;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer quantity;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2)
    private Double price;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<StockTransaction> stockTransactions;

}
