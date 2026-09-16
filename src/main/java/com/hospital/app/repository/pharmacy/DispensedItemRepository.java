package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.DispensedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispensedItemRepository extends JpaRepository<DispensedItem, Long> {
	boolean existsByPrescriptionItem_Id(Long prescriptionItemId);
}
