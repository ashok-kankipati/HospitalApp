package com.hospital.app.controller.ipd;

import com.hospital.app.model.ipd.Bed;
import com.hospital.app.service.ipd.BedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beds")
public class BedController {
    @Autowired
    private BedService bedService;

    @GetMapping
    public ResponseEntity<List<Bed>> getAllBeds() {
        return ResponseEntity.ok(bedService.getAllBeds());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(bedService.getSummary());
    }

    @PostMapping
    public ResponseEntity<Bed> addBed(@jakarta.validation.Valid @RequestBody Bed bed) {
        return ResponseEntity.ok(bedService.addBed(bed));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Bed> updateStatus(@PathVariable Long id, @jakarta.validation.Valid @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(bedService.updateBedStatus(id, status));
    }
}
