package com.hospital.app.controller.pharmacy;

import com.hospital.app.model.pharmacy.Medicine;
import com.hospital.app.service.pharmacy.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy/medicines")
public class MedicineController {
    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public List<Medicine> getAllMedicines() {
        return medicineService.getAllMedicines();
    }

    @GetMapping("/{id}")
    public Optional<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getMedicineById(id);
    }

    @PostMapping
    public Medicine addMedicine(@jakarta.validation.Valid @RequestBody Medicine medicine) {
        if (medicine == null || medicine.getName() == null || medicine.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicine name is required");
        }
        medicine.setName(medicine.getName().trim());
        return medicineService.saveMedicine(medicine);
    }

    @DeleteMapping("/{id}")
    public void deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
    }
}
