package com.hospital.app.model.pharmacy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"stockTransactions"})
    @com.hospital.app.validation.EntityReference
    private MedicineBatch batch;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="IN|OUT", message="Select a valid option.")
    private String transactionType; // IN/OUT

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer quantity;

    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String reference;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

}
