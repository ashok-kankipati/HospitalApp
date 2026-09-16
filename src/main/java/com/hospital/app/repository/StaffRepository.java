package com.hospital.app.repository;

import com.hospital.app.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmail(String email);
    List<Staff> findByDepartment(String department);
    List<Staff> findByPosition(String position);
    List<Staff> findByIsActive(Boolean isActive);
}
