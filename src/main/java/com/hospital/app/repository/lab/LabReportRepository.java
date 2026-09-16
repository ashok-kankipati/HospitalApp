package com.hospital.app.repository.lab;

import com.hospital.app.model.lab.LabReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabReportRepository extends JpaRepository<LabReport, Long> {
    List<LabReport> findByVisitId(Long visitId);
    List<LabReport> findByLabOrderId(Long labOrderId);
}
