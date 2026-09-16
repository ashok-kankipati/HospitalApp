package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {
    List<MedicineBatch> findByIsActiveTrue();
    List<MedicineBatch> findByMedicine_IdAndIsActiveTrueOrderByIdDesc(Long medicineId);
}
