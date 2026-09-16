package com.hospital.app.model.pharmacy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "medicines")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String name;

    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String description;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<MedicineBatch> batches;

}
