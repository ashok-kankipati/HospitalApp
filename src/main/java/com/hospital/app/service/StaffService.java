package com.hospital.app.service;

import com.hospital.app.model.Staff;
import com.hospital.app.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    public Staff addStaff(Staff staff) {
        return staffRepository.save(staff);
    }

    public Optional<Staff> getStaffById(Long id) {
        return staffRepository.findById(id);
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Staff updateStaff(Staff staff) {
        return staffRepository.save(staff);
    }

    public void deleteStaff(Long id) {
        staffRepository.deleteById(id);
    }

    public Optional<Staff> findStaffByEmail(String email) {
        return staffRepository.findByEmail(email);
    }

    public List<Staff> findStaffByDepartment(String department) {
        return staffRepository.findByDepartment(department);
    }

    public List<Staff> findStaffByPosition(String position) {
        return staffRepository.findByPosition(position);
    }

    public List<Staff> findActiveStaff() {
        return staffRepository.findByIsActive(true);
    }
}
