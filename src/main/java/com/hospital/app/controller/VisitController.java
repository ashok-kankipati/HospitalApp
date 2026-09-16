package com.hospital.app.controller;

import com.hospital.app.dto.lab.LabOrderRequest;
import com.hospital.app.dto.visit.VisitNotesRequest;
import com.hospital.app.model.Visit;
import com.hospital.app.model.lab.LabOrder;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.service.VisitService;
import com.hospital.app.service.lab.LabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    @Autowired
    private VisitService visitService;

    @Autowired
    private LabService labService;

    @PostMapping("/from-appointment/{appointmentId}")
    public ResponseEntity<Visit> createOrGetVisit(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(visitService.createOrGetByAppointment(appointmentId));
    }

    @GetMapping
    public List<Visit> getAllVisits() {
        return visitService.getAllVisits();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Visit> getVisitById(@PathVariable Long id) {
        Optional<Visit> visit = visitService.getVisitById(id);
        return visit.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<Visit> getVisitByAppointment(@PathVariable Long appointmentId) {
        Optional<Visit> visit = visitService.getVisitByAppointmentId(appointmentId);
        return visit.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<Visit> getLatestVisitByPatient(@PathVariable Long patientId) {
        Optional<Visit> visit = visitService.getLatestVisitByPatientId(patientId);
        return visit.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<Visit> updateVisitNotes(@PathVariable Long id, @jakarta.validation.Valid @RequestBody VisitNotesRequest request) {
        return ResponseEntity.ok(visitService.updateNotes(id, request));
    }

    @PostMapping("/{visitId}/lab-orders")
    public ResponseEntity<LabOrder> createLabOrder(@PathVariable Long visitId, @jakarta.validation.Valid @RequestBody LabOrderRequest request) {
        return ResponseEntity.ok(labService.createLabOrder(visitId, request));
    }

    @GetMapping("/{visitId}/lab-reports")
    public ResponseEntity<List<LabReport>> getReportsByVisit(@PathVariable Long visitId) {
        return ResponseEntity.ok(labService.getReportsByVisit(visitId));
    }
}
