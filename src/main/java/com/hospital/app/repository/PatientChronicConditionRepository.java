package com.hospital.app.repository;

import com.hospital.app.model.PatientChronicCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientChronicConditionRepository extends JpaRepository<PatientChronicCondition, Long> {
    List<PatientChronicCondition> findByPatientId(Long patientId);
}
