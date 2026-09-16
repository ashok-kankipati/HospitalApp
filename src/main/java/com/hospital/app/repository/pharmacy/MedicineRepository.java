package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
}
