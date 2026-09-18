package com.hospital.app.controller.billing;

import com.hospital.app.dto.billing.InvoiceGenerateRequest;
import com.hospital.app.dto.billing.InvoicePaymentRequest;
import com.hospital.app.dto.billing.InvoiceUpiQrRequest;
import com.hospital.app.model.billing.Invoice;
import com.hospital.app.model.billing.InvoiceDocument;
import com.hospital.app.model.billing.InvoicePayment;
import com.hospital.app.service.billing.InvoiceService;
import com.hospital.app.service.billing.RazorpayPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing/invoices")
public class InvoiceController {
    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private RazorpayPaymentService razorpayPaymentService;

    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    @GetMapping("/payment-options")
    public Map<String, Object> getPaymentOptions() {
        return razorpayPaymentService.getPublicConfig();
    }

    @GetMapping("/payments")
    public List<InvoicePayment> getAllPayments() {
        return invoiceService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getInvoiceDetails(@PathVariable Long id) {
        return invoiceService.getInvoiceDetails(id);
    }

    @GetMapping("/patient/{patientId}")
    public List<Invoice> getInvoicesByPatient(@PathVariable Long patientId) {
        return invoiceService.getInvoicesByPatientId(patientId);
    }

    @PostMapping("/generate")
    public Invoice generateInvoice(@jakarta.validation.Valid @RequestBody InvoiceGenerateRequest request) {
        return invoiceService.generateInvoice(request);
    }

    @PostMapping("/{id}/payments")
    public InvoicePayment addPayment(@PathVariable Long id, @jakarta.validation.Valid @RequestBody InvoicePaymentRequest request) {
        return invoiceService.addPayment(id, request);
    }

    @PostMapping("/{id}/razorpay/order")
    public Map<String, Object> createRazorpayOrder(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        BigDecimal amount = new BigDecimal(String.valueOf(request.getOrDefault("amount", "0")));
        return razorpayPaymentService.createOrder(id, amount);
    }

    @PostMapping("/{id}/razorpay/verify")
    public InvoicePayment verifyRazorpayPayment(@PathVariable Long id,
                                              @RequestBody Map<String, String> request) {
        return razorpayPaymentService.verifyAndCapture(
                id,
                request.get("razorpay_order_id"),
                request.get("razorpay_payment_id"),
                request.get("razorpay_signature"),
                request.get("amount") == null ? null : new BigDecimal(request.get("amount"))
        );
    }

    @PostMapping("/razorpay/webhook")
    public ResponseEntity<String> razorpayWebhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                                @RequestBody String body) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
        }
        if (!razorpayPaymentService.validateWebhookSignature(body, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        razorpayPaymentService.processWebhookEvent(body);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/{id}/upi-qr")
    public InvoiceDocument generateUpiQr(@PathVariable Long id, @jakarta.validation.Valid @RequestBody InvoiceUpiQrRequest request) {
        return invoiceService.generateUpiQr(id, request.getUpiId(), request.getAmount());
    }

    @PostMapping("/backfill-dispensed")
    public Map<String, Object> backfillDispensedInvoices() {
        return invoiceService.backfillInvoicesFromDispensedItems();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        byte[] pdfBytes = invoiceService.generateInvoicePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
