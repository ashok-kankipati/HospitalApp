package com.hospital.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="name", required=true, max=100, message="This field is required.")
    private String name;

    @Column(nullable = false, unique = true)
    @com.hospital.app.validation.InputText(kind="email", required=true, max=254, message="This field is required.")
    @jakarta.validation.constraints.Email(message="Enter a valid email address.")
    private String email;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="phone", required=true, max=30, message="This field is required.")
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    @com.hospital.app.validation.InputText(kind="birthDate", required=true, max=255, message="This field is required.")
    private String dateOfBirth;

    @Column(name = "address")
    @com.hospital.app.validation.InputText(kind="address", required=false, max=255, message="This field is required.")
    private String address;

    @Column(name = "medical_history")
    @com.hospital.app.validation.InputText(kind="text", required=false, max=255, message="This field is required.")
    private String medicalHistory;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
