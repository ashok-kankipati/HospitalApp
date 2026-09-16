package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.Dispense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispenseRepository extends JpaRepository<Dispense, Long> {
    List<Dispense> findByPrescriptionId(Long prescriptionId);
}
