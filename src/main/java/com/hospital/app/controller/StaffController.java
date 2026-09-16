package com.hospital.app.controller;

import com.hospital.app.model.Staff;
import com.hospital.app.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
//
    @Autowired
    private StaffService staffService;

    @PostMapping
    public ResponseEntity<Staff> addStaff(@jakarta.validation.Valid @RequestBody Staff staff) {
        Staff savedStaff = staffService.addStaff(staff);
        return new ResponseEntity<>(savedStaff, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Staff>> getAllStaff() {
        List<Staff> staff = staffService.getAllStaff();
        return new ResponseEntity<>(staff, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Staff> getStaffById(@PathVariable Long id) {
        Optional<Staff> staff = staffService.getStaffById(id);
        return staff.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Staff> updateStaff(@PathVariable Long id, @jakarta.validation.Valid @RequestBody Staff staff) {
        Optional<Staff> existingStaff = staffService.getStaffById(id);
        if (existingStaff.isPresent()) {
            staff.setId(id);
            Staff updatedStaff = staffService.updateStaff(staff);
            return new ResponseEntity<>(updatedStaff, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<Staff>> getStaffByDepartment(@PathVariable String department) {
        List<Staff> staff = staffService.findStaffByDepartment(department);
        return new ResponseEntity<>(staff, HttpStatus.OK);
    }

    @GetMapping("/position/{position}")
    public ResponseEntity<List<Staff>> getStaffByPosition(@PathVariable String position) {
        List<Staff> staff = staffService.findStaffByPosition(position);
        return new ResponseEntity<>(staff, HttpStatus.OK);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Staff>> getActiveStaff() {
        List<Staff> staff = staffService.findActiveStaff();
        return new ResponseEntity<>(staff, HttpStatus.OK);
    }
}
