package com.hospital.app.service.lab;

import com.hospital.app.dto.lab.LabOrderRequest;
import com.hospital.app.model.Appointment;
import com.hospital.app.model.Patient;
import com.hospital.app.model.Staff;
import com.hospital.app.model.Visit;
import com.hospital.app.model.lab.LabOrder;
import com.hospital.app.model.lab.LabOrderItem;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.model.lab.LabTestMaster;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.PatientRepository;
import com.hospital.app.repository.StaffRepository;
import com.hospital.app.repository.VisitRepository;
import com.hospital.app.repository.lab.LabOrderItemRepository;
import com.hospital.app.repository.lab.LabOrderRepository;
import com.hospital.app.repository.lab.LabReportRepository;
import com.hospital.app.repository.lab.LabTestMasterRepository;
import com.hospital.app.service.notification.NotificationService;
import com.hospital.app.service.document.CareFlowPdfStyle;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class LabService {
    @Autowired
    private LabTestMasterRepository labTestMasterRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabOrderItemRepository labOrderItemRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private NotificationService notificationService;

    public List<LabTestMaster> getAllTests() {
        return labTestMasterRepository.findAll();
    }

    public LabTestMaster addTest(LabTestMaster test) {
        return labTestMasterRepository.save(test);
    }

    public LabOrder createLabOrder(Long visitId, LabOrderRequest request) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        LabOrder order = new LabOrder();
        order.setVisitId(visitId);
        order.setOrderedByDoctorId(request.getOrderedByDoctorId() != null ? request.getOrderedByDoctorId() : visit.getDoctorId());
        order.setNotes(request.getNotes());
        order.setStatus("PENDING");
        LabOrder savedOrder = labOrderRepository.save(order);

        List<LabOrderItem> items = new ArrayList<>();
        if (request.getTestIds() != null) {
            for (Long testId : request.getTestIds()) {
                LabOrderItem item = new LabOrderItem();
                item.setLabOrderId(savedOrder.getId());
                item.setTestId(testId);
                item.setStatus("PENDING");
                items.add(item);
            }
            if (!items.isEmpty()) {
                labOrderItemRepository.saveAll(items);
            }
        }

        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            visit.setPriority(request.getPriority().trim());
        }
        if (!"LAB_REQUIRED".equalsIgnoreCase(visit.getStatus())) {
            visit.setStatus("LAB_REQUIRED");
        }
        visitRepository.save(visit);

        return savedOrder;
    }

    public List<LabOrder> getOrdersByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return labOrderRepository.findAll();
        }
        return labOrderRepository.findByStatus(status);
    }

    public LabOrder getOrderById(Long id) {
        return labOrderRepository.findById(id).orElse(null);
    }

    public List<LabOrderItem> getOrderItems(Long labOrderId) {
        return labOrderItemRepository.findByLabOrderId(labOrderId);
    }

    public LabOrder updateOrderStatus(Long orderId, String status) {
        LabOrder order = labOrderRepository.findById(orderId).orElseThrow();
        if (status != null && !status.trim().isEmpty()) {
            order.setStatus(status.trim());
        }
        return labOrderRepository.save(order);
    }

    public LabReport addReport(LabReport report) {
        LabReport saved = labReportRepository.save(report);
        String subject = "Lab Report Ready";
        String body = "Lab report uploaded for visit V" + String.format("%03d", report.getVisitId()) + ".";
        visitRepository.findById(report.getVisitId())
            .flatMap(visit -> appointmentRepository.findById(visit.getAppointmentId()))
            .flatMap(appointment -> staffRepository.findById(appointment.getStaffId()))
            .ifPresent(doctor -> notificationService.notifyRecipientByRole("Doctor", "LAB_REPORT_READY",
                doctor.getEmail(), subject, body));
        notificationService.notifyByRole("Lab", "LAB_REPORT_READY", subject, body);
        return saved;
    }

    public List<LabReport> getReportsByVisit(Long visitId) {
        return labReportRepository.findByVisitId(visitId);
    }

    public LabOrderItem updateOrderItemResult(Long itemId, com.hospital.app.dto.lab.LabOrderItemResultRequest request) {
        LabOrderItem item = labOrderItemRepository.findById(itemId).orElseThrow();
        if (request.getResultValue() != null) {
            item.setResultValue(request.getResultValue());
        }
        if (request.getResultUnit() != null) {
            item.setResultUnit(request.getResultUnit());
        }
        if (request.getReferenceRange() != null) {
            item.setReferenceRange(request.getReferenceRange());
        }
        if (request.getResultFlag() != null) {
            item.setResultFlag(request.getResultFlag());
        }
        if (request.getResultNotes() != null) {
            item.setResultNotes(request.getResultNotes());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            item.setStatus(request.getStatus().trim());
        }
        return labOrderItemRepository.save(item);
    }

    public byte[] generateLabReportPdf(Long reportId) {
        LabReport report = labReportRepository.findById(reportId).orElseThrow();
        Visit visit = visitRepository.findById(report.getVisitId()).orElseThrow();
        Appointment appointment = appointmentRepository.findById(visit.getAppointmentId()).orElseThrow();
        Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
        Staff doctor = staffRepository.findById(appointment.getStaffId()).orElse(null);

        List<LabOrderItem> items = new ArrayList<>();
        if (report.getLabOrderId() != null) {
            items = labOrderItemRepository.findByLabOrderId(report.getLabOrderId());
        } else {
            List<LabOrder> orders = labOrderRepository.findByVisitId(visit.getId());
            for (LabOrder order : orders) {
                items.addAll(labOrderItemRepository.findByLabOrderId(order.getId()));
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 38, 38, 44, 54);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        CareFlowPdfStyle.apply(writer, "Laboratory report");
        document.open();

        document.add(CareFlowPdfStyle.brandHeader("Laboratory report", safeText(report.getFileName()), "Final result"));
        document.add(CareFlowPdfStyle.title("Clinical results"));

        PdfPTable details = new PdfPTable(3);
        details.setWidthPercentage(100);
        details.addCell(CareFlowPdfStyle.labelValueCell("Patient", patient == null ? "" : patient.getName()));
        details.addCell(CareFlowPdfStyle.labelValueCell("Doctor", doctor == null ? "" : doctor.getName()));
        details.addCell(CareFlowPdfStyle.labelValueCell("Department", doctor == null ? "" : doctor.getDepartment()));
        details.addCell(CareFlowPdfStyle.labelValueCell("Visit", "V" + String.format("%03d", visit.getId())));
        details.addCell(CareFlowPdfStyle.labelValueCell("Appointment", "A" + String.format("%03d", appointment.getId())));
        details.addCell(CareFlowPdfStyle.labelValueCell("Report ID", String.valueOf(report.getId())));
        document.add(details);
        document.add(CareFlowPdfStyle.section("Test results"));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 3.2f, 1.3f, 1.4f, 1.1f, 2.0f});

        table.addCell(CareFlowPdfStyle.headerCell("Test ID"));
        table.addCell(CareFlowPdfStyle.headerCell("Test name"));
        table.addCell(CareFlowPdfStyle.headerCell("Result"));
        table.addCell(CareFlowPdfStyle.headerCell("Reference"));
        table.addCell(CareFlowPdfStyle.headerCell("Status"));
        table.addCell(CareFlowPdfStyle.headerCell("Notes"));

        for (LabOrderItem item : items) {
            table.addCell(CareFlowPdfStyle.dataCell(String.valueOf(item.getTestId())));
            LabTestMaster test = labTestMasterRepository.findById(item.getTestId()).orElse(null);
            table.addCell(CareFlowPdfStyle.dataCell(safeText(test != null ? test.getTestName() : "Test")));
            String resultValue = item.getResultValue() == null ? "" : item.getResultValue();
            String resultUnit = item.getResultUnit() == null ? "" : item.getResultUnit();
            String resultCombined = (resultValue + " " + resultUnit).trim();
            table.addCell(CareFlowPdfStyle.dataCell(safeText(resultCombined)));
            table.addCell(CareFlowPdfStyle.dataCell(safeText(item.getReferenceRange())));
            table.addCell(CareFlowPdfStyle.dataCell(safeText(item.getResultFlag() != null ? item.getResultFlag() : item.getStatus())));
            table.addCell(CareFlowPdfStyle.dataCell(safeText(item.getResultNotes())));
        }

        document.add(table);

        document.close();
        return outputStream.toByteArray();
    }

    public String generateLabReportText(Long reportId) {
        LabReport report = labReportRepository.findById(reportId).orElseThrow();
        List<LabOrderItem> items = new ArrayList<>();
        if (report.getLabOrderId() != null) {
            items.addAll(labOrderItemRepository.findByLabOrderId(report.getLabOrderId()));
        } else {
            for (LabOrder order : labOrderRepository.findByVisitId(report.getVisitId())) {
                items.addAll(labOrderItemRepository.findByLabOrderId(order.getId()));
            }
        }
        StringBuilder text = new StringBuilder("Medical Lab Report #" + reportId + "\n");
        for (LabOrderItem item : items) {
            LabTestMaster test = labTestMasterRepository.findById(item.getTestId()).orElse(null);
            text.append("\n").append(test == null ? "Test " + item.getTestId() : safeText(test.getTestName()));
            text.append("\nResult: ").append(item.getResultValue() == null || item.getResultValue().isBlank()
                ? "Not recorded" : item.getResultValue()).append(" ").append(safeText(item.getResultUnit()));
            text.append("\nReference: ").append(safeText(item.getReferenceRange()));
            text.append("\nStatus: ").append(safeText(item.getResultFlag() != null ? item.getResultFlag() : item.getStatus()));
            if (item.getResultNotes() != null && !item.getResultNotes().isBlank())
                text.append("\nNotes: ").append(item.getResultNotes());
            text.append("\n");
        }
        if (items.isEmpty()) text.append("\nNo test results recorded.\n");
        text.append("\nPlease contact your doctor to discuss these results.");
        return text.toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
