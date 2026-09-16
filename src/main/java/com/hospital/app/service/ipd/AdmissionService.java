package com.hospital.app.service.ipd;

import com.hospital.app.dto.ipd.AdmissionRequest;
import com.hospital.app.dto.ipd.AdmissionResponse;
import com.hospital.app.model.ipd.Admission;
import com.hospital.app.model.ipd.Bed;
import com.hospital.app.repository.ipd.AdmissionRepository;
import com.hospital.app.repository.ipd.BedRepository;
import com.hospital.app.service.billing.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdmissionService {
    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private InvoiceService invoiceService;

    public Admission admit(AdmissionRequest request) {
        Bed bed = bedRepository.findById(request.getBedId()).orElseThrow();
        if (!"AVAILABLE".equalsIgnoreCase(bed.getStatus())) {
            throw new IllegalStateException("Bed is not available.");
        }
        admissionRepository.findByBedIdAndStatus(request.getBedId(), "ACTIVE").ifPresent(a -> {
            throw new IllegalStateException("Bed already occupied.");
        });

        Admission admission = new Admission();
        admission.setBedId(request.getBedId());
        admission.setPatientId(request.getPatientId());
        admission.setVisitId(request.getVisitId());
        admission.setDoctorId(request.getDoctorId());
        admission.setNotes(request.getNotes());
        admission.setStatus("ACTIVE");

        Admission saved = admissionRepository.save(admission);

        bed.setStatus("OCCUPIED");
        bedRepository.save(bed);

        return saved;
    }

    public Admission discharge(Long admissionId) {
        Admission admission = admissionRepository.findById(admissionId).orElseThrow();
        if ("DISCHARGED".equalsIgnoreCase(admission.getStatus())) {
            return admission;
        }
        admission.setStatus("DISCHARGED");
        admission.setDischargedAt(LocalDateTime.now());
        Admission saved = admissionRepository.save(admission);

        Bed bed = bedRepository.findById(admission.getBedId()).orElseThrow();
        bed.setStatus("AVAILABLE");
        bedRepository.save(bed);

        long days = calculateDays(admission.getAdmittedAt(), admission.getDischargedAt());
        invoiceService.addBedCharge(saved, bed, days);

        return saved;
    }

    public List<AdmissionResponse> getActiveAdmissions() {
        List<Admission> admissions = admissionRepository.findByStatus("ACTIVE");
        List<AdmissionResponse> responses = new ArrayList<>();
        for (Admission admission : admissions) {
            AdmissionResponse response = new AdmissionResponse();
            response.setId(admission.getId());
            response.setBedId(admission.getBedId());
            response.setPatientId(admission.getPatientId());
            response.setVisitId(admission.getVisitId());
            response.setDoctorId(admission.getDoctorId());
            response.setAdmittedAt(admission.getAdmittedAt());
            response.setDischargedAt(admission.getDischargedAt());
            response.setStatus(admission.getStatus());
            response.setNotes(admission.getNotes());
            bedRepository.findById(admission.getBedId()).ifPresent(bed -> response.setBedNumber(bed.getBedNumber()));
            responses.add(response);
        }
        return responses;
    }

    private long calculateDays(LocalDateTime admittedAt, LocalDateTime dischargedAt) {
        if (admittedAt == null || dischargedAt == null) {
            return 1;
        }
        long hours = Duration.between(admittedAt, dischargedAt).toHours();
        long days = (long) Math.ceil(hours / 24.0);
        return Math.max(days, 1);
    }
}
