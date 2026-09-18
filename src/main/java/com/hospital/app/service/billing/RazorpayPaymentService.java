package com.hospital.app.service.billing;

import com.google.gson.Gson;
import com.hospital.app.model.billing.Invoice;
import com.hospital.app.model.billing.InvoicePayment;
import com.hospital.app.repository.billing.InvoicePaymentRepository;
import com.hospital.app.repository.billing.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RazorpayPaymentService {
    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);
    private static final URI ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Value("${hospital.razorpay.enabled:false}")
    private boolean enabled;

    @Value("${hospital.razorpay.key-id:}")
    private String keyId;

    @Value("${hospital.razorpay.key-secret:}")
    private String keySecret;

    @Value("${hospital.razorpay.webhook-secret:}")
    private String webhookSecret;

    public Map<String, Object> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();
        boolean valid = enabled && keyId != null && !keyId.isBlank();
        log.info("Razorpay public config: enabled={}, keyIdPresent={}", enabled, valid);
        config.put("enabled", valid);
        config.put("keyId", keyId == null ? "" : keyId);
        config.put("mode", "test");
        return config;
    }

    public Map<String, Object> createOrder(Long invoiceId, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        BigDecimal payable = amount == null ? invoice.getBalanceDue() : amount;
        log.info("Creating Razorpay order: invoiceId={}, amount={}, balanceDue={}, enabled={}, keyIdPresent={}, secretPresent={}",
                invoiceId, payable, invoice.getBalanceDue(), enabled,
                keyId != null && !keyId.isBlank(), keySecret != null && !keySecret.isBlank());
        if (!enabled || keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            log.error("Razorpay order blocked: disabled={}, keyIdPresent={}, secretPresent={}", enabled,
                    keyId != null && !keyId.isBlank(), keySecret != null && !keySecret.isBlank());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay is disabled or not configured.");
        }
        if (payable.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Razorpay order invalid amount: invoiceId={}, amount={}", invoiceId, payable);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero.");
        }
        if (payable.compareTo(invoice.getBalanceDue()) > 0) {
            log.warn("Razorpay order exceeds invoice balance: invoiceId={}, amount={}, balanceDue={}", invoiceId, payable, invoice.getBalanceDue());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment cannot exceed the outstanding invoice balance.");
        }

        long amountInPaise = payable.movePointRight(2).longValueExact();
        String requestBody = new Gson().toJson(Map.of(
                "amount", amountInPaise,
                "currency", "INR",
                "receipt", invoice.getInvoiceNumber(),
                "notes", Map.of("invoice_id", String.valueOf(invoiceId))
        ));
        String credentials = Base64.getEncoder().encodeToString(
                (keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(ORDERS_URI)
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try {
            HttpResponse<String> razorpayResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (razorpayResponse.statusCode() < 200 || razorpayResponse.statusCode() >= 300) {
                log.error("Razorpay order request failed: invoiceId={}, status={}", invoiceId, razorpayResponse.statusCode());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create Razorpay order.");
            }
            Map<String, Object> response = new Gson().fromJson(razorpayResponse.body(), Map.class);
            if (response == null || response.get("id") == null) {
                log.error("Razorpay order response missing order id: invoiceId={}", invoiceId);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Razorpay order response.");
            }
            log.info("Razorpay order created: invoiceId={}, orderId={}, amount={}", invoiceId, response.get("id"), amountInPaise);
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Razorpay order request interrupted: invoiceId={}", invoiceId, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay order request was interrupted.");
        } catch (IOException exception) {
            log.error("Razorpay order request failed: invoiceId={}", invoiceId, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to reach Razorpay.");
        }
    }

    public InvoicePayment verifyAndCapture(Long invoiceId, String orderId, String paymentId, String signature, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        log.info("Verifying Razorpay payment: invoiceId={}, orderId={}, paymentId={}, amount={}, enabled={}",
                invoiceId, orderId, paymentId, amount, enabled);
        if (!enabled) {
            log.error("Razorpay verification blocked: feature is disabled");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay is disabled.");
        }
        if (paymentId == null || paymentId.isBlank()) {
            log.error("Razorpay verification failed: missing payment id for invoiceId={}", invoiceId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay payment id");
        }
        if (orderId == null || orderId.isBlank()) {
            log.error("Razorpay verification failed: missing order id for invoiceId={}", invoiceId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay order id");
        }
        if (signature == null || signature.isBlank()) {
            log.error("Razorpay verification failed: missing signature for invoiceId={}, orderId={}, paymentId={}", invoiceId, orderId, paymentId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay signature");
        }

        String payload = orderId + "|" + paymentId;
        boolean signatureValid = RazorpaySignatureVerifier.isValid(payload, signature, keySecret);
        log.info("Razorpay signature check: invoiceId={}, orderId={}, paymentId={}, valid={}", invoiceId, orderId, paymentId, signatureValid);
        if (!signatureValid) {
            log.error("Razorpay signature invalid: invoiceId={}, orderId={}, paymentId={}", invoiceId, orderId, paymentId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature.");
        }

        if (invoicePaymentRepository.existsByGatewayPaymentId(paymentId)) {
            log.warn("Duplicate Razorpay payment detected: invoiceId={}, paymentId={}", invoiceId, paymentId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment was already processed.");
        }

        BigDecimal paymentAmount = amount == null ? invoice.getBalanceDue() : amount;
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Razorpay payment invalid amount: invoiceId={}, paymentId={}, amount={}", invoiceId, paymentId, paymentAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero.");
        }
        if (paymentAmount.compareTo(invoice.getBalanceDue()) > 0) {
            log.warn("Razorpay payment exceeds balance: invoiceId={}, paymentId={}, amount={}, balanceDue={}", invoiceId, paymentId, paymentAmount, invoice.getBalanceDue());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment cannot exceed the outstanding invoice balance.");
        }

        InvoicePayment payment = new InvoicePayment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(paymentAmount);
        payment.setPaymentMethod("Razorpay");
        payment.setPaymentStatus("PAID");
        payment.setGatewayOrderId(orderId);
        payment.setGatewayPaymentId(paymentId);
        payment.setGatewaySignature(signature);
        payment.setGatewayStatus("CAPTURED");
        payment.setReference("Razorpay payment " + paymentId);

        InvoicePayment recorded = invoiceService.recordGatewayPayment(invoice, payment);
        log.info("Razorpay payment recorded successfully: invoiceId={}, paymentId={}, recordedPaymentId={}", invoiceId, paymentId, recorded.getId());
        return recorded;
    }

    public static boolean isDuplicateGatewayPayment(InvoicePayment first, InvoicePayment second) {
        if (first == null || second == null) {
            return false;
        }
        String firstId = first.getGatewayPaymentId();
        String secondId = second.getGatewayPaymentId();
        return (firstId != null && firstId.equals(secondId)) || (firstId != null && firstId.equals(second.getGatewayOrderId()));
    }

    public boolean validateWebhookSignature(String payload, String signature) {
        if (payload == null || signature == null || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        return RazorpaySignatureVerifier.isValid(payload, signature, webhookSecret);
    }

    public void processWebhookEvent(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        Map<String, Object> event = new Gson().fromJson(payload, Map.class);
        if (event == null || !"payment.captured".equals(event.get("event"))) {
            return;
        }
        Map<String, Object> payloadMap = (Map<String, Object>) event.get("payload");
        if (payloadMap == null) {
            return;
        }
        Map<String, Object> paymentMap = (Map<String, Object>) payloadMap.get("payment");
        if (paymentMap == null) {
            return;
        }
        String orderId = String.valueOf(paymentMap.getOrDefault("order_id", ""));
        String paymentId = String.valueOf(paymentMap.getOrDefault("id", ""));
        if (orderId.isBlank() || paymentId.isBlank()) {
            return;
        }
        if (invoicePaymentRepository.existsByGatewayPaymentId(paymentId)) {
            return;
        }
        Object amountObj = paymentMap.get("amount");
        BigDecimal amount = amountObj instanceof Number ? BigDecimal.valueOf(((Number) amountObj).doubleValue()).movePointLeft(2) : BigDecimal.ZERO;
        InvoicePayment payment = new InvoicePayment();
        payment.setGatewayOrderId(orderId);
        payment.setGatewayPaymentId(paymentId);
        payment.setGatewayStatus("CAPTURED");
        payment.setPaymentMethod("Razorpay");
        payment.setPaymentStatus("PAID");
        payment.setAmount(amount);
        payment.setReference("Razorpay webhook " + paymentId);
        InvoicePayment existingOrder = invoicePaymentRepository.findByGatewayOrderId(orderId).orElse(null);
        if (existingOrder != null) {
            existingOrder.setGatewayPaymentId(paymentId);
            existingOrder.setGatewayStatus("CAPTURED");
            existingOrder.setPaymentStatus("PAID");
            existingOrder.setAmount(amount);
            payment = existingOrder;
        }
        Invoice invoice = orderId.startsWith("order_") ? invoiceRepository.findById(Long.parseLong(orderId.replace("order_", ""))).orElse(null) : null;
        if (invoice == null) {
            String invoiceNumber = String.valueOf(paymentMap.getOrDefault("description", "")).replace("Invoice ", "");
            for (Invoice candidate : invoiceRepository.findAll()) {
                if (candidate.getInvoiceNumber().equalsIgnoreCase(invoiceNumber) || invoiceNumber.isBlank()) {
                    invoice = candidate;
                    break;
                }
            }
        }
        if (invoice == null) {
            return;
        }
        payment.setInvoiceId(invoice.getId());
        payment.setPaymentMethod("Razorpay");
        invoiceService.recordGatewayPayment(invoice, payment);
    }

    public Optional<InvoicePayment> findByGatewayPaymentId(String paymentId) {
        return invoicePaymentRepository.findByGatewayPaymentId(paymentId);
    }
}
