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
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        notificationService.notifyByRole("Doctor", "LAB_REPORT_READY", subject, body);
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
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);

        Paragraph title = new Paragraph("Medical Lab Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Report: " + safeText(report.getFileName())));
        document.add(new Paragraph("Visit: V" + String.format("%03d", visit.getId())));
        document.add(new Paragraph("Appointment: A" + String.format("%03d", appointment.getId())));
        document.add(new Paragraph(" "));

        if (patient != null) {
            document.add(new Paragraph("Patient: " + safeText(patient.getName())));
            document.add(new Paragraph("Email: " + safeText(patient.getEmail())));
            document.add(new Paragraph("Phone: " + safeText(patient.getPhone())));
        }
        if (doctor != null) {
            document.add(new Paragraph("Doctor: " + safeText(doctor.getName()) + " (" + safeText(doctor.getDepartment()) + ")"));
        }
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 3.2f, 1.3f, 1.4f, 1.1f, 2.0f});

        addHeaderCell(table, "Test ID", headerFont);
        addHeaderCell(table, "Test Name", headerFont);
        addHeaderCell(table, "Result", headerFont);
        addHeaderCell(table, "Reference", headerFont);
        addHeaderCell(table, "Status", headerFont);
        addHeaderCell(table, "Notes", headerFont);

        for (LabOrderItem item : items) {
            table.addCell(String.valueOf(item.getTestId()));
            LabTestMaster test = labTestMasterRepository.findById(item.getTestId()).orElse(null);
            table.addCell(safeText(test != null ? test.getTestName() : "Test"));
            String resultValue = item.getResultValue() == null ? "" : item.getResultValue();
            String resultUnit = item.getResultUnit() == null ? "" : item.getResultUnit();
            String resultCombined = (resultValue + " " + resultUnit).trim();
            table.addCell(safeText(resultCombined));
            table.addCell(safeText(item.getReferenceRange()));
            table.addCell(safeText(item.getResultFlag() != null ? item.getResultFlag() : item.getStatus()));
            table.addCell(safeText(item.getResultNotes()));
        }

        document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Notes: " + safeText(report.getMimeType())));

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

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
