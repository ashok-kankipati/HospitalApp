package com.hospital.app.repository.lab;

import com.hospital.app.model.lab.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {
    List<LabOrder> findByStatus(String status);
    List<LabOrder> findByVisitId(Long visitId);
}
