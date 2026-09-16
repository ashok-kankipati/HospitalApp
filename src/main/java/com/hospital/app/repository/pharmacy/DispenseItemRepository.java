package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.DispenseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispenseItemRepository extends JpaRepository<DispenseItem, Long> {
    List<DispenseItem> findByDispenseId(Long dispenseId);
    long countByMedicineBatchId(Long medicineBatchId);
}
