package com.hospital.app.controller.pharmacy;

import com.hospital.app.dto.pharmacy.DispenseCreateRequest;
import com.hospital.app.model.pharmacy.Dispense;
import com.hospital.app.model.pharmacy.DispenseItem;
import com.hospital.app.service.pharmacy.DispenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispense")
public class DispenseController {
    @Autowired
    private DispenseService dispenseService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDispense(@jakarta.validation.Valid @RequestBody DispenseCreateRequest request) {
        try {
            Dispense dispense = dispenseService.createDispense(request);
            List<DispenseItem> items = dispenseService.getItemsByDispense(dispense.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("dispense", dispense);
            response.put("items", items);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", ex.getMessage());
            return ResponseEntity.status(409).body(response);
        }
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<List<Dispense>> getDispenseByPrescription(@PathVariable Long prescriptionId) {
        return ResponseEntity.ok(dispenseService.getDispenseByPrescription(prescriptionId));
    }
}
