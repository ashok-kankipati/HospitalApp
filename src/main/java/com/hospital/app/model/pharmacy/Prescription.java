package com.hospital.app.model.pharmacy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointmentId", nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long appointmentId;

    @Column(name = "doctorId", nullable = false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long doctorId;

    @Column(nullable = false)
    private LocalDateTime date;

    @jakarta.validation.constraints.Pattern(regexp="CREATED|FINAL|DISPENSED|PARTIALLY_DISPENSED|CANCELLED|COMPLETED", message="Select a valid option.")
    private String status;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<PrescriptionItem> items;

}
