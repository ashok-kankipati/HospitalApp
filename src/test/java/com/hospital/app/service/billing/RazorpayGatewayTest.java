package com.hospital.app.service.billing;

import com.hospital.app.model.billing.InvoicePayment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayGatewayTest {

    @Test
    void verifiesRazorpaySignatures() {
        String payload = "{\"order_id\":\"order_123\",\"payment_id\":\"pay_123\",\"amount\":5000}";
        String secret = "test_secret";
        String signature = RazorpaySignatureVerifier.sign(payload, secret);

        assertEquals(signature, RazorpaySignatureVerifier.sign(payload, secret));
        assertTrue(RazorpaySignatureVerifier.isValid(payload, signature, secret));
        assertFalse(RazorpaySignatureVerifier.isValid(payload, "bad_signature", secret));
    }

    @Test
    void duplicatesAreDetectedForGatewayPayments() {
        InvoicePayment first = new InvoicePayment();
        first.setGatewayPaymentId("pay_123");

        InvoicePayment second = new InvoicePayment();
        second.setGatewayPaymentId("pay_123");

        assertNotNull(first.getGatewayPaymentId());
        assertEquals(first.getGatewayPaymentId(), second.getGatewayPaymentId());
        assertTrue(RazorpayPaymentService.isDuplicateGatewayPayment(first, second));
    }
}
