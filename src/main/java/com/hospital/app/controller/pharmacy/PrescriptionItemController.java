package com.hospital.app.controller.pharmacy;

import com.hospital.app.model.pharmacy.PrescriptionItem;
import com.hospital.app.service.pharmacy.PrescriptionItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy/prescription-items")
public class PrescriptionItemController {
    @Autowired
    private PrescriptionItemService prescriptionItemService;

    @GetMapping
    public List<PrescriptionItem> getAllItems() {
        return prescriptionItemService.getAllItems();
    }

    @GetMapping("/{id}")
    public Optional<PrescriptionItem> getItemById(@PathVariable Long id) {
        return prescriptionItemService.getItemById(id);
    }

    @PostMapping
    public Map<String, Object> addItem(@jakarta.validation.Valid @RequestBody PrescriptionItem item) {
        PrescriptionItem saved = prescriptionItemService.saveItem(item);
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("prescriptionId", saved.getPrescription() != null ? saved.getPrescription().getId() : null);
        response.put("medicineBatchId", saved.getMedicineBatch() != null ? saved.getMedicineBatch().getId() : null);
        response.put("quantity", saved.getQuantity());
        response.put("instructions", saved.getInstructions());
        return response;
    }
}
