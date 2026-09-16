package com.hospital.app.service.pharmacy;

import com.hospital.app.model.pharmacy.Prescription;
import com.hospital.app.model.Appointment;
import com.hospital.app.model.Visit;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.VisitRepository;
import com.hospital.app.repository.lab.LabReportRepository;
import com.hospital.app.repository.pharmacy.PrescriptionRepository;
import com.hospital.app.service.billing.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionService {
    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Autowired
    private InvoiceService invoiceService;

    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    public Prescription savePrescription(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    public java.util.Map<String, Object> finalizePrescription(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
        Appointment appointment = appointmentRepository.findById(prescription.getAppointmentId()).orElseThrow();

        if (appointment.getStatus() == null || !appointment.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new IllegalStateException("Appointment must be completed before finalizing prescription.");
        }

        Visit visit = visitRepository.findByAppointmentId(appointment.getId()).orElseThrow();
        List<LabReport> reports = labReportRepository.findByVisitId(visit.getId());
        if (reports == null || reports.isEmpty()) {
            throw new IllegalStateException("Lab reports must be uploaded before finalizing prescription.");
        }

        prescription.setStatus("FINAL");
        Prescription saved = prescriptionRepository.save(prescription);

        Long invoiceId = invoiceService.generateInvoiceFromVisit(visit.getId());
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Prescription finalized. Invoice created for billing.");
        response.put("prescriptionId", saved.getId());
        response.put("invoiceId", invoiceId);
        return response;
    }
}
