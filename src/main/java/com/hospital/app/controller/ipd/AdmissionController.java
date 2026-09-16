package com.hospital.app.controller.ipd;

import com.hospital.app.dto.ipd.AdmissionRequest;
import com.hospital.app.dto.ipd.AdmissionResponse;
import com.hospital.app.model.ipd.Admission;
import com.hospital.app.service.ipd.AdmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionController {
    @Autowired
    private AdmissionService admissionService;

    @PostMapping
    public ResponseEntity<?> admit(@jakarta.validation.Valid @RequestBody AdmissionRequest request) {
        try {
            Admission admission = admissionService.admit(request);
            return ResponseEntity.ok(admission);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/discharge")
    public ResponseEntity<?> discharge(@PathVariable Long id) {
        try {
            Admission admission = admissionService.discharge(id);
            return ResponseEntity.ok(admission);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<AdmissionResponse>> getActiveAdmissions() {
        return ResponseEntity.ok(admissionService.getActiveAdmissions());
    }
}
