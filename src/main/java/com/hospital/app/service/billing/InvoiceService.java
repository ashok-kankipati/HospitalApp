package com.hospital.app.service.billing;

import com.hospital.app.dto.billing.InvoiceGenerateRequest;
import com.hospital.app.dto.billing.InvoicePaymentRequest;
import com.hospital.app.model.Appointment;
import com.hospital.app.model.Patient;
import com.hospital.app.model.billing.Invoice;
import com.hospital.app.model.billing.InvoiceItem;
import com.hospital.app.model.billing.InvoicePayment;
import com.hospital.app.model.pharmacy.MedicineBatch;
import com.hospital.app.model.pharmacy.Prescription;
import com.hospital.app.model.pharmacy.PrescriptionItem;
import com.hospital.app.model.pharmacy.DispensedItem;
import com.hospital.app.model.Visit;
import com.hospital.app.model.lab.LabOrder;
import com.hospital.app.model.lab.LabOrderItem;
import com.hospital.app.model.lab.LabTestMaster;
import com.hospital.app.model.ipd.Admission;
import com.hospital.app.model.ipd.Bed;
import com.hospital.app.repository.VisitRepository;
import com.hospital.app.repository.lab.LabReportRepository;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.service.notification.NotificationService;
import com.hospital.app.service.notification.EmailAttachment;
import com.hospital.app.service.lab.LabService;
import com.hospital.app.repository.lab.LabOrderItemRepository;
import com.hospital.app.repository.lab.LabOrderRepository;
import com.hospital.app.repository.lab.LabTestMasterRepository;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.PatientRepository;
import com.hospital.app.repository.billing.InvoiceDocumentRepository;
import com.hospital.app.repository.billing.InvoiceItemRepository;
import com.hospital.app.repository.billing.InvoicePaymentRepository;
import com.hospital.app.repository.billing.InvoiceRepository;
import com.hospital.app.repository.pharmacy.DispensedItemRepository;
import com.hospital.app.repository.pharmacy.PrescriptionItemRepository;
import com.hospital.app.repository.pharmacy.PrescriptionRepository;
import com.hospital.app.model.billing.InvoiceDocument;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private InvoiceDocumentRepository invoiceDocumentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private DispensedItemRepository dispensedItemRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabOrderItemRepository labOrderItemRepository;

    @Autowired
    private LabTestMasterRepository labTestMasterRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LabReportRepository labReportRepository;

    @Autowired
    private LabService labService;

    public List<InvoicePayment> getAllPayments() {
        return invoicePaymentRepository.findAll();
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public List<Invoice> getInvoicesByPatientId(Long patientId) {
        return invoiceRepository.findByPatientId(patientId);
    }

    public Map<String, Object> getInvoiceDetails(Long id) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow();
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(id);
        List<InvoicePayment> payments = invoicePaymentRepository.findByInvoiceId(id);
        Map<String, Object> details = new HashMap<>();
        details.put("invoice", invoice);
        details.put("items", items);
        details.put("payments", payments);
        return details;
    }

    public Invoice generateInvoice(InvoiceGenerateRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId()).orElseThrow();
        Long patientId = appointment.getPatientId();

        List<InvoiceItem> items = new ArrayList<>();
        addFeeItem(items, "CONSULTATION", "Consultation Fee", safeAmount(request.getConsultationFee()));
        addFeeItem(items, "LAB", "Lab Charges", safeAmount(request.getLabFee()));

        List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointment.getId());
        for (Prescription prescription : prescriptions) {
            List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(prescription.getId());
            for (PrescriptionItem item : prescriptionItems) {
                if (dispensedItemRepository.existsByPrescriptionItem_Id(item.getId())) {
                    continue;
                }
                MedicineBatch batch = item.getMedicineBatch();
                if (batch == null) {
                    continue;
                }
                BigDecimal unitPrice = BigDecimal.valueOf(batch.getPrice() == null ? 0.0 : batch.getPrice());
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
                BigDecimal lineTotal = unitPrice.multiply(qty);
                String description = "Pharmacy - " + (batch.getMedicine() != null ? batch.getMedicine().getName() : "Medicine")
                        + " (" + batch.getBatchNo() + ")";
                InvoiceItem invoiceItem = new InvoiceItem();
                invoiceItem.setItemType("PHARMACY");
                invoiceItem.setDescription(description);
                invoiceItem.setQuantity(item.getQuantity() == null ? 0 : item.getQuantity());
                invoiceItem.setUnitPrice(unitPrice);
                invoiceItem.setLineTotal(lineTotal);
                invoiceItem.setReferenceType("PRESCRIPTION_ITEM");
                invoiceItem.setReferenceId(item.getId());
                items.add(invoiceItem);
            }
        }

        BigDecimal subtotal = items.stream()
                .map(InvoiceItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (safeAmount(request.getDiscount()).compareTo(subtotal.add(safeAmount(request.getTax()))) > 0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Discount cannot exceed the subtotal plus tax.");
        BigDecimal tax = safeAmount(request.getTax());
        BigDecimal discount = safeAmount(request.getDiscount());
        BigDecimal total = subtotal.add(tax).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setPatientId(patientId);
        invoice.setAppointmentId(appointment.getId());
        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setDiscount(discount);
        invoice.setTotal(total);
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setBalanceDue(total);
        invoice.setStatus(total.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING");
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDueDate(LocalDate.now().plusDays(7));
        invoice.setNotes(request.getNotes());

        Invoice saved = invoiceRepository.save(invoice);

        for (InvoiceItem item : items) {
            item.setInvoiceId(saved.getId());
        }
        invoiceItemRepository.saveAll(items);

        String subject = "Billing Reminder";
        String body = "Invoice created: " + saved.getInvoiceNumber() + " Total: " + formatMoney(saved.getTotal());
        notificationService.notifyByRole("Billing", "BILLING_REMINDER", subject, body);
        return saved;
    }

    public InvoicePayment addPayment(Long invoiceId, InvoicePaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        String previousStatus = invoice.getStatus();
        BigDecimal amount = safeAmount(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (amount.compareTo(safeAmount(invoice.getBalanceDue())) > 0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Payment cannot exceed the outstanding invoice balance.");
        InvoicePayment payment = new InvoicePayment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReference(request.getReference());
        payment.setPaymentStatus("PAID");
        InvoicePayment savedPayment = invoicePaymentRepository.save(payment);

        BigDecimal newPaid = safeAmount(invoice.getAmountPaid()).add(amount);
        BigDecimal balance = safeAmount(invoice.getTotal()).subtract(newPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }

        invoice.setAmountPaid(newPaid);
        invoice.setBalanceDue(balance);
        invoice.setStatus(balance.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIAL");
        invoiceRepository.save(invoice);

        if (!"PAID".equalsIgnoreCase(previousStatus) && "PAID".equalsIgnoreCase(invoice.getStatus())) {
            sendPaidInvoiceEmail(invoice);
        }

        return savedPayment;
    }

    public void addOrUpdateDispensedItemInvoice(com.hospital.app.model.pharmacy.DispensedItem dispensedItem) {
        if (dispensedItem == null || dispensedItem.getPrescriptionItem() == null) {
            return;
        }

        PrescriptionItem prescriptionItem = dispensedItem.getPrescriptionItem();
        if (prescriptionItem.getPrescription() == null || prescriptionItem.getMedicineBatch() == null) {
            prescriptionItem = prescriptionItemRepository.findById(prescriptionItem.getId()).orElse(prescriptionItem);
        }

        Prescription prescription = prescriptionItem.getPrescription();
        if (prescription == null) {
            return;
        }

        Appointment appointment = appointmentRepository.findById(prescription.getAppointmentId()).orElse(null);
        if (appointment == null) {
            return;
        }

        Invoice invoice = invoiceRepository.findByAppointmentId(appointment.getId())
                .stream()
                .findFirst()
                .orElse(null);

        if (invoice == null) {
            invoice = new Invoice();
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setPatientId(appointment.getPatientId());
            invoice.setAppointmentId(appointment.getId());
            invoice.setSubtotal(BigDecimal.ZERO);
            invoice.setTax(BigDecimal.ZERO);
            invoice.setDiscount(BigDecimal.ZERO);
            invoice.setTotal(BigDecimal.ZERO);
            invoice.setAmountPaid(BigDecimal.ZERO);
            invoice.setBalanceDue(BigDecimal.ZERO);
            invoice.setStatus("PENDING");
            invoice.setIssuedAt(LocalDateTime.now());
            invoice.setDueDate(LocalDate.now().plusDays(7));
            invoice = invoiceRepository.save(invoice);
        }

        List<InvoiceItem> prescriptionCharges = invoiceItemRepository
                .findByInvoiceIdAndReferenceTypeAndReferenceId(invoice.getId(), "PRESCRIPTION_ITEM", prescriptionItem.getId());
        if (!prescriptionCharges.isEmpty()) {
            invoiceItemRepository.deleteAll(prescriptionCharges);
        }

        if (dispensedItem.getId() != null
                && invoiceItemRepository.existsByReferenceTypeAndReferenceId("DISPENSED_ITEM", dispensedItem.getId())) {
            recalculateInvoiceTotals(invoice);
            return;
        }

        BigDecimal unitPrice = BigDecimal.valueOf(
                prescriptionItem.getMedicineBatch() != null && prescriptionItem.getMedicineBatch().getPrice() != null
                        ? prescriptionItem.getMedicineBatch().getPrice()
                        : 0.0
        );
        int qty = dispensedItem.getQuantity() == null ? 0 : dispensedItem.getQuantity();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(invoice.getId());
        item.setItemType("PHARMACY");
        String medicineName = prescriptionItem.getMedicineBatch() != null && prescriptionItem.getMedicineBatch().getMedicine() != null
                ? prescriptionItem.getMedicineBatch().getMedicine().getName()
                : "Medicine";
        String batchNo = prescriptionItem.getMedicineBatch() != null ? prescriptionItem.getMedicineBatch().getBatchNo() : "-";
        item.setDescription("Dispensed - " + medicineName + " (" + batchNo + ")");
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.setLineTotal(lineTotal);
        item.setReferenceType("DISPENSED_ITEM");
        item.setReferenceId(dispensedItem.getId());

        invoiceItemRepository.save(item);

        recalculateInvoiceTotals(invoice);
    }

    public Map<String, Object> backfillInvoicesFromDispensedItems() {
        List<DispensedItem> dispensedItems = dispensedItemRepository.findAll();
        int created = 0;
        int skipped = 0;

        for (DispensedItem item : dispensedItems) {
            if (item.getId() != null
                    && invoiceItemRepository.existsByReferenceTypeAndReferenceId("DISPENSED_ITEM", item.getId())) {
                addOrUpdateDispensedItemInvoice(item);
                skipped++;
                continue;
            }
            addOrUpdateDispensedItemInvoice(item);
            created++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("processed", dispensedItems.size());
        result.put("created", created);
        result.put("skipped", skipped);
        return result;
    }

    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoiceId);
        List<InvoicePayment> payments = invoicePaymentRepository.findByInvoiceId(invoiceId);
        Patient patient = patientRepository.findById(invoice.getPatientId()).orElse(null);
        Appointment appointment = null;
        if (invoice.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(invoice.getAppointmentId()).orElse(null);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);

        Paragraph title = new Paragraph("Hospital Invoice", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber()));
        document.add(new Paragraph("Issued: " + (invoice.getIssuedAt() != null ? invoice.getIssuedAt() : "")));
        document.add(new Paragraph("Status: " + invoice.getStatus()));
        if (appointment != null) {
            document.add(new Paragraph("Appointment: A" + String.format("%03d", appointment.getId())));
        }
        document.add(new Paragraph(" "));

        if (patient != null) {
            document.add(new Paragraph("Patient: " + patient.getName()));
            document.add(new Paragraph("Email: " + patient.getEmail()));
            document.add(new Paragraph("Phone: " + patient.getPhone()));
        }
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 4.5f, 1.2f, 1.6f, 1.6f});

        addHeaderCell(table, "Type", headerFont);
        addHeaderCell(table, "Description", headerFont);
        addHeaderCell(table, "Qty", headerFont);
        addHeaderCell(table, "Unit", headerFont);
        addHeaderCell(table, "Total", headerFont);

        for (InvoiceItem item : items) {
            table.addCell(safeText(item.getItemType()));
            table.addCell(safeText(item.getDescription()));
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(formatMoney(item.getUnitPrice()));
            table.addCell(formatMoney(item.getLineTotal()));
        }

        document.add(table);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Subtotal: " + formatMoney(invoice.getSubtotal())));
        document.add(new Paragraph("Tax: " + formatMoney(invoice.getTax())));
        document.add(new Paragraph("Discount: " + formatMoney(invoice.getDiscount())));
        document.add(new Paragraph("Total: " + formatMoney(invoice.getTotal())));
        document.add(new Paragraph("Paid: " + formatMoney(invoice.getAmountPaid())));
        document.add(new Paragraph("Balance Due: " + formatMoney(invoice.getBalanceDue())));

        if (!payments.isEmpty()) {
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Payments", headerFont));
            for (InvoicePayment payment : payments) {
                document.add(new Paragraph("- " + formatMoney(payment.getAmount()) + " via "
                        + safeText(payment.getPaymentMethod()) + " (" + safeText(payment.getReference()) + ")"));
            }
        }

        document.close();
        return outputStream.toByteArray();
    }

    public InvoiceDocument generateUpiQr(Long invoiceId, String upiId, BigDecimal amount) {
        if (upiId == null || upiId.trim().isEmpty()) {
            throw new IllegalArgumentException("UPI ID is required");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        BigDecimal payAmount = safeAmount(amount);
        String invoiceNumber = invoice.getInvoiceNumber() == null ? "INV" : invoice.getInvoiceNumber();
        String fileName = invoiceNumber + "-UPI.png";

        List<InvoiceDocument> existing = invoiceDocumentRepository.findByInvoiceId(invoiceId);
        for (InvoiceDocument doc : existing) {
            if (fileName.equalsIgnoreCase(doc.getFileName())) {
                return doc;
            }
        }

        String upiPayload = buildUpiPayload(upiId, payAmount);
        byte[] pngBytes = generateQrPng(upiPayload, 200, 200);
        String base64 = Base64.getEncoder().encodeToString(pngBytes);
        String dataUrl = "data:image/png;base64," + base64;

        InvoiceDocument doc = new InvoiceDocument();
        doc.setInvoiceId(invoiceId);
        doc.setFileName(fileName);
        doc.setFileUrl(dataUrl);
        return invoiceDocumentRepository.save(doc);
    }

    private void addFeeItem(List<InvoiceItem> items, String type, String description, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        InvoiceItem item = new InvoiceItem();
        item.setItemType(type);
        item.setDescription(description);
        item.setQuantity(1);
        item.setUnitPrice(amount);
        item.setLineTotal(amount);
        item.setReferenceType("MANUAL");
        items.add(item);
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateInvoiceNumber() {
        int year = LocalDate.now().getYear();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "INV-" + year + "-" + suffix;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void recalculateInvoiceTotals(Invoice invoice) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());
        BigDecimal subtotal = items.stream()
                .map(InvoiceItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = safeAmount(invoice.getTax());
        BigDecimal discount = safeAmount(invoice.getDiscount());
        BigDecimal total = subtotal.add(tax).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        BigDecimal paid = safeAmount(invoice.getAmountPaid());
        BigDecimal balance = total.subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }

        invoice.setSubtotal(subtotal);
        invoice.setTotal(total);
        invoice.setBalanceDue(balance);
        invoice.setStatus(balance.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : (paid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "PENDING"));
        invoiceRepository.save(invoice);
    }

    public void addBedCharge(Admission admission, Bed bed, long days) {
        if (admission == null || bed == null) {
            return;
        }
        Visit visit = visitRepository.findById(admission.getVisitId()).orElse(null);
        if (visit == null) {
            return;
        }
        Appointment appointment = appointmentRepository.findById(visit.getAppointmentId()).orElse(null);
        if (appointment == null) {
            return;
        }

        Invoice invoice = invoiceRepository.findByAppointmentId(appointment.getId())
                .stream()
                .findFirst()
                .orElse(null);

        if (invoice == null) {
            invoice = new Invoice();
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setPatientId(appointment.getPatientId());
            invoice.setAppointmentId(appointment.getId());
            invoice.setSubtotal(BigDecimal.ZERO);
            invoice.setTax(BigDecimal.ZERO);
            invoice.setDiscount(BigDecimal.ZERO);
            invoice.setTotal(BigDecimal.ZERO);
            invoice.setAmountPaid(BigDecimal.ZERO);
            invoice.setBalanceDue(BigDecimal.ZERO);
            invoice.setStatus("PENDING");
            invoice.setIssuedAt(LocalDateTime.now());
            invoice.setDueDate(LocalDate.now().plusDays(7));
            invoice = invoiceRepository.save(invoice);
        }

        if (invoiceItemRepository.existsByReferenceTypeAndReferenceId("ADMISSION", admission.getId())) {
            return;
        }

        BigDecimal unitPrice = bed.getDailyCharge() == null ? BigDecimal.ZERO : bed.getDailyCharge();
        BigDecimal qty = BigDecimal.valueOf(Math.max(days, 1));
        BigDecimal lineTotal = unitPrice.multiply(qty);

        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(invoice.getId());
        item.setItemType("BED_CHARGE");
        item.setDescription("Bed Charge - " + bed.getBedNumber() + " (" + bed.getType() + ")");
        item.setQuantity((int) qty.longValue());
        item.setUnitPrice(unitPrice);
        item.setLineTotal(lineTotal);
        item.setReferenceType("ADMISSION");
        item.setReferenceId(admission.getId());
        invoiceItemRepository.save(item);

        recalculateInvoiceTotals(invoice);
    }

    private String buildUpiPayload(String upiId, BigDecimal amount) {
        String encodedUpiId = URLEncoder.encode(upiId.trim(), StandardCharsets.UTF_8);
        String amt = amount.setScale(2, RoundingMode.HALF_UP).toString();
        return "upi://pay?pa=" + encodedUpiId + "&pn=Hospital&am=" + amt + "&cu=INR";
    }

    private byte[] generateQrPng(String payload, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }

    public Long generateInvoiceFromVisit(Long visitId) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        Appointment appointment = appointmentRepository.findById(visit.getAppointmentId()).orElseThrow();

        Invoice invoice = invoiceRepository.findByAppointmentId(appointment.getId())
                .stream()
                .findFirst()
                .orElse(null);

        if (invoice == null) {
            invoice = new Invoice();
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setPatientId(appointment.getPatientId());
            invoice.setAppointmentId(appointment.getId());
            invoice.setSubtotal(BigDecimal.ZERO);
            invoice.setTax(BigDecimal.ZERO);
            invoice.setDiscount(BigDecimal.ZERO);
            invoice.setTotal(BigDecimal.ZERO);
            invoice.setAmountPaid(BigDecimal.ZERO);
            invoice.setBalanceDue(BigDecimal.ZERO);
            invoice.setStatus("PENDING");
            invoice.setIssuedAt(LocalDateTime.now());
            invoice.setDueDate(LocalDate.now().plusDays(7));
            invoice = invoiceRepository.save(invoice);
        }

        List<InvoiceItem> newItems = new ArrayList<>();

        // Consultation fee (default 100 if not already present)
        if (!invoiceItemRepository.existsByReferenceTypeAndReferenceId("CONSULTATION", appointment.getId())) {
            BigDecimal consultFee = BigDecimal.valueOf(100.00);
            if (consultFee.compareTo(BigDecimal.ZERO) > 0) {
                InvoiceItem consultItem = new InvoiceItem();
                consultItem.setInvoiceId(invoice.getId());
                consultItem.setItemType("CONSULTATION");
                consultItem.setDescription("Consultation Fee");
                consultItem.setQuantity(1);
                consultItem.setUnitPrice(consultFee);
                consultItem.setLineTotal(consultFee);
                consultItem.setReferenceType("CONSULTATION");
                consultItem.setReferenceId(appointment.getId());
                newItems.add(consultItem);
            }
        }

        // Lab tests
        List<LabOrder> labOrders = labOrderRepository.findByVisitId(visitId);
        for (LabOrder order : labOrders) {
            List<LabOrderItem> items = labOrderItemRepository.findByLabOrderId(order.getId());
            for (LabOrderItem item : items) {
                if (invoiceItemRepository.existsByReferenceTypeAndReferenceId("LAB_ORDER_ITEM", item.getId())) {
                    continue;
                }
                LabTestMaster test = labTestMasterRepository.findById(item.getTestId()).orElse(null);
                BigDecimal price = test != null && test.getPrice() != null ? test.getPrice() : BigDecimal.ZERO;
                String testName = test != null && test.getTestName() != null ? test.getTestName() : "Lab Test";

                InvoiceItem labItem = new InvoiceItem();
                labItem.setInvoiceId(invoice.getId());
                labItem.setItemType("LAB");
                labItem.setDescription("Lab - " + testName);
                labItem.setQuantity(1);
                labItem.setUnitPrice(price);
                labItem.setLineTotal(price);
                labItem.setReferenceType("LAB_ORDER_ITEM");
                labItem.setReferenceId(item.getId());
                newItems.add(labItem);
            }
        }

        // Pharmacy items (from prescriptions)
        List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointment.getId());
        for (Prescription prescription : prescriptions) {
            List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(prescription.getId());
            for (PrescriptionItem item : prescriptionItems) {
                if (dispensedItemRepository.existsByPrescriptionItem_Id(item.getId())) {
                    continue;
                }
                if (invoiceItemRepository.existsByReferenceTypeAndReferenceId("PRESCRIPTION_ITEM", item.getId())) {
                    continue;
                }
                MedicineBatch batch = item.getMedicineBatch();
                BigDecimal unitPrice = BigDecimal.valueOf(batch != null && batch.getPrice() != null ? batch.getPrice() : 0.0);
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                String description = "Pharmacy - " + (batch != null && batch.getMedicine() != null ? batch.getMedicine().getName() : "Medicine");

                InvoiceItem pharmItem = new InvoiceItem();
                pharmItem.setInvoiceId(invoice.getId());
                pharmItem.setItemType("PHARMACY");
                pharmItem.setDescription(description);
                pharmItem.setQuantity(qty);
                pharmItem.setUnitPrice(unitPrice);
                pharmItem.setLineTotal(lineTotal);
                pharmItem.setReferenceType("PRESCRIPTION_ITEM");
                pharmItem.setReferenceId(item.getId());
                newItems.add(pharmItem);
            }
        }

        if (!newItems.isEmpty()) {
            invoiceItemRepository.saveAll(newItems);
        }

        recalculateInvoiceTotals(invoice);
        return invoice.getId();
    }

    private void sendPaidInvoiceEmail(Invoice invoice) {
        if (invoice.getEmailSentAt() != null) {
            return;
        }
        Patient patient = patientRepository.findById(invoice.getPatientId()).orElse(null);
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            return;
        }

        List<EmailAttachment> attachments = new ArrayList<>();
        byte[] invoicePdf = generateInvoicePdf(invoice.getId());
        String invoiceName = (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "invoice") + ".pdf";
        attachments.add(new EmailAttachment(invoiceName, invoicePdf, "application/pdf"));

        if (invoice.getAppointmentId() != null) {
            visitRepository.findByAppointmentId(invoice.getAppointmentId()).ifPresent(visit -> {
                List<LabReport> reports = labReportRepository.findByVisitId(visit.getId());
                for (LabReport report : reports) {
                    byte[] reportPdf = labService.generateLabReportPdf(report.getId());
                    String reportName = (report.getFileName() != null ? report.getFileName() : ("lab-report-" + report.getId() + ".pdf"));
                    if (!reportName.toLowerCase().endsWith(".pdf")) {
                        reportName = reportName + ".pdf";
                    }
                    attachments.add(new EmailAttachment(reportName, reportPdf, "application/pdf"));
                }
            });
        }

        String subject = "Payment Received - " + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "Invoice");
        String body = "Dear " + patient.getName() + ",\n\n"
                + "Thank you for your payment. Please find your invoice and lab reports attached.\n\n"
                + "Regards,\nHospital Management";
        notificationService.notifyRecipientWithAttachments(patient.getEmail(), subject, body, attachments);

        invoice.setEmailSentAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }
}
