package com.hospital.app.repository;

import com.hospital.app.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByStaffId(Long staffId);
    List<Appointment> findByStatus(String status);
    List<Appointment> findByPatientIdAndStatus(Long patientId, String status);
    List<Appointment> findByStaffIdAndStatus(Long staffId, String status);
}
