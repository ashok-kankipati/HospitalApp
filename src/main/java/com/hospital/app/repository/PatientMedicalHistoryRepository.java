package com.hospital.app.repository;

import com.hospital.app.model.PatientMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistory, Long> {
    List<PatientMedicalHistory> findByPatientId(Long patientId);
}
