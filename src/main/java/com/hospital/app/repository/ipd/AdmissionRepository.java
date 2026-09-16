package com.hospital.app.repository.ipd;

import com.hospital.app.model.ipd.Admission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    List<Admission> findByStatus(String status);
    Optional<Admission> findByBedIdAndStatus(Long bedId, String status);
}
