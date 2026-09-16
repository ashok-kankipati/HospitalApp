package com.hospital.app.service;

import com.duosecurity.Client;
import com.duosecurity.exception.DuoException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DuoService {
    private static final Logger log = LoggerFactory.getLogger(DuoService.class);
    private final Client client;

    public DuoService(Environment env) throws DuoException {
        if (env.getProperty("hospital.duo.enabled", Boolean.class, false)) {
            client = new Client.Builder(
                    env.getRequiredProperty("hospital.duo.client-id"),
                    env.getRequiredProperty("hospital.duo.client-secret"),
                    env.getRequiredProperty("hospital.duo.api-host"),
                    env.getRequiredProperty("hospital.duo.redirect-uri")).build();
            String host = env.getRequiredProperty("hospital.duo.api-host");
            log.info("Duo enabled: apiHost={}, Java={}, SDK={}",
                    host.matches("[a-zA-Z0-9.-]+") ? host : "[invalid hostname format]",
                    System.getProperty("java.version"), Client.class.getPackage().getImplementationVersion());
        } else {
            client = null;
        }
    }

    public boolean isEnabled() { return client != null; }

    public String state() { return client.generateState(); }

    public String authorizationUrl(String username, String state) throws DuoException {
        try {
            client.healthCheck();
        } catch (Exception e) {
            log.error("Duo health check failed: {}", diagnostic(e));
            throw e;
        }
        try {
            return client.createAuthUrl(username, state);
        } catch (Exception e) {
            log.error("Duo authorization URL creation failed: {}", diagnostic(e));
            throw e;
        }
    }

    // Never log exception messages or provider response bodies: they may contain credentials.
    static String diagnostic(Throwable failure) {
        StringBuilder types = new StringBuilder();
        String category = "Check Duo application credentials, API hostname and system clock.";
        for (Throwable cause = failure; cause != null && types.length() < 500; cause = cause.getCause()) {
            if (!types.isEmpty()) types.append(" -> ");
            types.append(cause.getClass().getSimpleName());
            if (cause instanceof java.net.UnknownHostException) {
                category = "DNS lookup failed. Check DUO_API_HOST and DNS/network access.";
            } else if (cause instanceof javax.net.ssl.SSLPeerUnverifiedException) {
                String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(java.util.Locale.ROOT);
                if (message.contains("certificate pinning failure")) {
                    category = "Certificate pinning failed. Check the runtime Duo SDK version and HTTPS inspection/proxy.";
                } else if (message.contains("hostname") && message.contains("not verified")) {
                    category = "Certificate hostname mismatch. Copy DUO_API_HOST exactly from the Duo application and check DNS/proxy routing.";
                } else {
                    category = "TLS peer identity could not be verified. Check the configured Duo hostname and HTTPS inspection/proxy.";
                }
            } else if (cause instanceof javax.net.ssl.SSLException) {
                category = "TLS connection failed. Check Java certificates, system clock and HTTPS inspection/proxy.";
            } else if (cause instanceof java.net.SocketTimeoutException) {
                category = "Connection timed out. Check outbound HTTPS access to Duo.";
            } else if (cause instanceof java.net.ConnectException) {
                category = "Connection failed. Check firewall/proxy and outbound HTTPS access to Duo.";
            }
            if (cause == cause.getCause()) break;
        }
        return types + ". " + category;
    }

    public void verify(String code, String username) throws DuoException {
        client.exchangeAuthorizationCodeFor2FAResult(code, username);
    }
}
