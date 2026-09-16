package com.hospital.app.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DuoServiceTest {
    @Test void peerDiagnosticsDistinguishPinningAndHostnameWithoutPrintingMessages() {
        String pinning = DuoService.diagnostic(new javax.net.ssl.SSLPeerUnverifiedException(
                "Certificate pinning failure! private-certificate-details"));
        assertTrue(pinning.contains("Certificate pinning failed"));
        assertFalse(pinning.contains("private-certificate-details"));
        String hostname = DuoService.diagnostic(new javax.net.ssl.SSLPeerUnverifiedException(
                "Hostname private-host not verified"));
        assertTrue(hostname.contains("Certificate hostname mismatch"));
        assertFalse(hostname.contains("private-host"));
    }
    @Test void diagnosticsIdentifyNestedNetworkFailureWithoutExposingMessages() {
        var failure = new RuntimeException("client-secret-private",
                new java.net.UnknownHostException("private-host-and-token"));
        String result = DuoService.diagnostic(failure);
        assertTrue(result.contains("DNS lookup failed"));
        assertTrue(result.contains("UnknownHostException"));
        assertFalse(result.contains("client-secret-private"));
        assertFalse(result.contains("private-host-and-token"));
    }

    @Test void diagnosticsIdentifyTlsFailure() {
        assertTrue(DuoService.diagnostic(new javax.net.ssl.SSLHandshakeException("private"))
                .contains("TLS connection failed"));
    }
}
