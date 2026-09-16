package com.hospital.app.controller;

import com.hospital.app.model.pharmacy.Prescription;
import com.hospital.app.repository.pharmacy.PrescriptionRepository;
import com.hospital.app.model.Appointment;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.model.pharmacy.PrescriptionItem;
import com.hospital.app.repository.pharmacy.PrescriptionItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
public class PatientPrescriptionHistoryController {
    @Autowired
    private PrescriptionRepository prescriptionRepository;
    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    // Get all prescriptions for a patient (via appointments)
    @GetMapping("/{patientId}/prescriptions")
    public List<Map<String, Object>> getPrescriptionHistory(@PathVariable Long patientId) {
        // Get all appointments for this patient
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        Set<Long> appointmentIds = appointments.stream().map(Appointment::getId).collect(Collectors.toSet());

        // Find all prescriptions for these appointments
        List<Prescription> prescriptions = prescriptionRepository.findAll().stream()
            .filter(p -> p.getAppointmentId() != null && appointmentIds.contains(p.getAppointmentId()))
            .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Prescription presc : prescriptions) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", presc.getDate());
            entry.put("doctorId", presc.getDoctorId());
            entry.put("prescriptionId", presc.getId());
            entry.put("status", presc.getStatus());
            // Fetch prescription items
            List<PrescriptionItem> items = prescriptionItemRepository.findAll().stream()
                .filter(i -> i.getPrescription().getId().equals(presc.getId()))
                .collect(Collectors.toList());
            entry.put("medicines", items);
            result.add(entry);
        }
        return result;
    }
}
