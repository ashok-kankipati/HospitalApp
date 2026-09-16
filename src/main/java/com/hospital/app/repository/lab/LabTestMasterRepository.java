package com.hospital.app.repository.lab;

import com.hospital.app.model.lab.LabTestMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabTestMasterRepository extends JpaRepository<LabTestMaster, Long> {
}
