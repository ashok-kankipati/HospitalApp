package com.hospital.app.service;

import com.hospital.app.dto.visit.VisitNotesRequest;
import com.hospital.app.model.Appointment;
import com.hospital.app.model.Visit;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VisitService {
    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public java.util.List<Visit> getAllVisits() {
        return visitRepository.findAll();
    }

    public Optional<Visit> getVisitById(Long id) {
        return visitRepository.findById(id);
    }

    public Optional<Visit> getVisitByAppointmentId(Long appointmentId) {
        return visitRepository.findByAppointmentId(appointmentId);
    }

    public Optional<Visit> getLatestVisitByPatientId(Long patientId) {
        return visitRepository.findByPatientId(patientId).stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .findFirst();
    }

    public Visit createOrGetByAppointment(Long appointmentId) {
        return visitRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> {
                    Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
                    Visit visit = new Visit();
                    visit.setAppointmentId(appointment.getId());
                    visit.setPatientId(appointment.getPatientId());
                    visit.setDoctorId(appointment.getStaffId());
                    visit.setStatus("OPEN");
                    visit.setPriority("NORMAL");
                    return visitRepository.save(visit);
                });
    }

    public Visit updateNotes(Long visitId, VisitNotesRequest request) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        if (request.getSymptoms() != null) {
            visit.setSymptoms(request.getSymptoms());
        }
        if (request.getDiagnosis() != null) {
            visit.setDiagnosis(request.getDiagnosis());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            visit.setStatus(request.getStatus().trim());
        }
        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            visit.setPriority(request.getPriority().trim());
        }
        return visitRepository.save(visit);
    }
}
