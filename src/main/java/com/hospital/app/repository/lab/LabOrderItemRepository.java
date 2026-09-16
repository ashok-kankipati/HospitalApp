package com.hospital.app.repository.lab;

import com.hospital.app.model.lab.LabOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, Long> {
    List<LabOrderItem> findByLabOrderId(Long labOrderId);
}
