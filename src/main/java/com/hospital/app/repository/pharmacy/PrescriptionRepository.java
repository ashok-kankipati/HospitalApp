package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Prescription p where p.id = :id")
    java.util.Optional<Prescription> findForDispensing(@org.springframework.data.repository.query.Param("id") Long id);

    List<Prescription> findByAppointmentId(Long appointmentId);
}
