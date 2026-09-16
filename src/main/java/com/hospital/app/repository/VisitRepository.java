package com.hospital.app.repository;

import com.hospital.app.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    Optional<Visit> findByAppointmentId(Long appointmentId);
    List<Visit> findByPatientId(Long patientId);
    List<Visit> findByDoctorId(Long doctorId);
}
