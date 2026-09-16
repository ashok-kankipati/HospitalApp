package com.hospital.app.controller.pharmacy;

import com.hospital.app.model.pharmacy.Prescription;
import com.hospital.app.service.pharmacy.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy/prescriptions")
public class PrescriptionController {
    @Autowired
    private PrescriptionService prescriptionService;

    @GetMapping
    public List<Prescription> getAllPrescriptions() {
        return prescriptionService.getAllPrescriptions();
    }

    @GetMapping("/{id}")
    public Optional<Prescription> getPrescriptionById(@PathVariable Long id) {
        return prescriptionService.getPrescriptionById(id);
    }

    @PostMapping
    public Map<String, Object> addPrescription(@jakarta.validation.Valid @RequestBody Prescription prescription) {
        Prescription saved = prescriptionService.savePrescription(prescription);
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("appointmentId", saved.getAppointmentId());
        response.put("doctorId", saved.getDoctorId());
        response.put("date", saved.getDate());
        response.put("status", saved.getStatus());
        return response;
    }

    @PostMapping("/{id}/finalize")
    public org.springframework.http.ResponseEntity<Map<String, Object>> finalizePrescription(@PathVariable Long id) {
        try {
            return org.springframework.http.ResponseEntity.ok(prescriptionService.finalizePrescription(id));
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return org.springframework.http.ResponseEntity.badRequest().body(response);
        }
    }
}
