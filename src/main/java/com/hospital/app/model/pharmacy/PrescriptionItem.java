package com.hospital.app.model.pharmacy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prescription_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    @JsonIgnoreProperties({"items"})
    @com.hospital.app.validation.EntityReference
    private Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "medicine_batch_id", nullable = false)
    @com.hospital.app.validation.EntityReference
    private MedicineBatch medicineBatch;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer quantity;

    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String instructions;

}
