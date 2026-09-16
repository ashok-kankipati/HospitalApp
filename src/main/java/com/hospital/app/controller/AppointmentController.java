package com.hospital.app.controller;

import com.hospital.app.model.Appointment;
import com.hospital.app.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    @Autowired
    private AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentService.getAppointmentById(id);
        return appointment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Appointment> addAppointment(@jakarta.validation.Valid @RequestBody Appointment appointment) {
        requireFutureAppointment(appointment);
        return ResponseEntity.ok(appointmentService.addAppointment(appointment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable Long id, @jakarta.validation.Valid @RequestBody Appointment appointment) {
        Optional<Appointment> existingAppointment = appointmentService.getAppointmentById(id);
        if (existingAppointment.isPresent()) {
            if (!java.util.Objects.equals(existingAppointment.get().getAppointmentDate(), appointment.getAppointmentDate())
                    || !java.util.Objects.equals(existingAppointment.get().getAppointmentTime(), appointment.getAppointmentTime())) {
                requireFutureAppointment(appointment);
            }
            appointment.setId(id);
            return ResponseEntity.ok(appointmentService.updateAppointment(appointment));
        }
        return ResponseEntity.notFound().build();
    }

    private void requireFutureAppointment(Appointment appointment) {
        var date = java.time.LocalDate.parse(appointment.getAppointmentDate());
        var time = java.time.LocalTime.parse(appointment.getAppointmentTime());
        if (java.time.LocalDateTime.of(date, time).isBefore(java.time.LocalDateTime.now()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose an appointment date and time in the future.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentService.getAppointmentById(id);
        if (appointment.isPresent()) {
            appointmentService.deleteAppointment(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatientId(patientId));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByStaffId(@PathVariable Long staffId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByStaffId(staffId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Appointment>> getAppointmentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByStatus(status));
    }

    @GetMapping("/patient/{patientId}/status/{status}")
    public ResponseEntity<List<Appointment>> getPatientAppointmentsByStatus(
            @PathVariable Long patientId,
            @PathVariable String status) {
        return ResponseEntity.ok(appointmentService.getPatientAppointmentsByStatus(patientId, status));
    }

    @GetMapping("/staff/{staffId}/status/{status}")
    public ResponseEntity<List<Appointment>> getStaffAppointmentsByStatus(
            @PathVariable Long staffId,
            @PathVariable String status) {
        return ResponseEntity.ok(appointmentService.getStaffAppointmentsByStatus(staffId, status));
    }
}
