package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);
    long countByMedicineBatch_Id(Long medicineBatchId);
}
