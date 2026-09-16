package com.hospital.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="name", required=true, max=100, message="This field is required.")
    private String name;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=100, message="This field is required.")
    @jakarta.validation.constraints.Pattern(regexp="Doctor|Nurse|Pharmacist|Radiologist|Lab Technician|Receptionist|Surgeon", message="Select a valid staff position.")
    private String position;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=100, message="This field is required.")
    private String department;

    @Column(nullable = false, unique = true)
    @com.hospital.app.validation.InputText(kind="email", required=true, max=254, message="This field is required.")
    @jakarta.validation.constraints.Email(message="Enter a valid email address.")
    private String email;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="phone", required=true, max=30, message="This field is required.")
    private String phone;

    @Column(nullable = false)
    @com.hospital.app.validation.InputText(kind="text", required=true, max=255, message="This field is required.")
    private String specialization;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "joining_date")
    @com.hospital.app.validation.InputText(kind="date", required=true, max=255, message="This field is required.")
    private String joiningDate;
}
