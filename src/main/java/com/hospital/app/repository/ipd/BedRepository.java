package com.hospital.app.repository.ipd;

import com.hospital.app.model.ipd.Bed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedRepository extends JpaRepository<Bed, Long> {
    long countByStatus(String status);
    long countByType(String type);
}
