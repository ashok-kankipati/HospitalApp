package com.hospital.app.model.pharmacy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispensed_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispensedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "prescription_item_id", nullable = false)
    @JsonIgnoreProperties({"prescription"})
    @com.hospital.app.validation.EntityReference
    private PrescriptionItem prescriptionItem;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long dispensedBy; // staff id (pharmacist)

    @Column(nullable = false)
    private LocalDateTime dispensedDate;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer quantity;

}
