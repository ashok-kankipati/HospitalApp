package com.hospital.app.controller.pharmacy;

import com.hospital.app.model.pharmacy.MedicineBatch;
import com.hospital.app.service.pharmacy.MedicineService;
import com.hospital.app.service.pharmacy.MedicineBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy/batches")
public class MedicineBatchController {
    @Autowired
    private MedicineBatchService batchService;
    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public List<MedicineBatch> getAllBatches() {
        return batchService.getAllBatches();
    }

    @GetMapping("/{id}")
    public Optional<MedicineBatch> getBatchById(@PathVariable Long id) {
        return batchService.getBatchById(id);
    }

    @PostMapping
    public MedicineBatch addBatch(@jakarta.validation.Valid @RequestBody MedicineBatch batch) {
        if (batch.getExpiryDate().isBefore(java.time.LocalDate.now()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "New medicine batches cannot already be expired.");
        if (batch == null || batch.getMedicine() == null || batch.getMedicine().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicine is required for batch");
        }
        if (medicineService.getMedicineById(batch.getMedicine().getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected medicine does not exist");
        }
        return batchService.saveBatch(batch);
    }

    @DeleteMapping("/{id}")
    public void deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
    }
}
