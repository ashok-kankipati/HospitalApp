package com.hospital.app.controller.lab;

import com.hospital.app.repository.lab.LabReportRepository;
import com.hospital.app.repository.VisitRepository;
import com.hospital.app.repository.PatientRepository;
import com.hospital.app.service.lab.LabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.hospital.app.repository.AppointmentRepository appointments;
    private final Environment env;
    private final LabReportRepository reports;
    private final VisitRepository visits;
    private final PatientRepository patients;
    private final LabService lab;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhatsAppController(Environment env, LabReportRepository reports, VisitRepository visits,
                              PatientRepository patients, LabService lab, ObjectMapper json) {
        this.env = env; this.reports = reports; this.visits = visits;
        this.patients = patients; this.lab = lab; this.json = json;
    }

    private String setting(String name) { return env.getProperty("hospital.whatsapp." + name, "").trim(); }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    private void authorize(String password) {
        if (!"true".equals(setting("enabled"))) throw error(HttpStatus.SERVICE_UNAVAILABLE, "WhatsApp sending is not configured.");
        String expected = setting("operator-password");
        if (expected.isBlank() || password == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8)))
            throw error(HttpStatus.UNAUTHORIZED, "Invalid WhatsApp operator password.");
    }

    public record SendRequest(boolean consentConfirmed, boolean sessionConfirmed) {}

    @GetMapping("/mode")
    public Map<String, Boolean> mode() {
        return Map.of("template", !setting("content-sid").isBlank());
    }

    @PostMapping("/appointments/{id}")
    public Map<String, String> send(@PathVariable Long id, @RequestHeader(value="X-WhatsApp-Password", required=false) String password,
                                  @jakarta.validation.Valid @RequestBody SendRequest request) throws Exception {
        authorize(password);
        if (!request.consentConfirmed()) throw error(HttpStatus.BAD_REQUEST, "Confirm patient consent before sending.");

        String account = setting("account-sid"), key = setting("api-key"), secret = setting("api-secret"), from = setting("from");
        if (!account.matches("AC[0-9a-fA-F]{32}") || key.isBlank() || secret.isBlank() || from.isBlank())
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "Configure Twilio account SID, API key, API secret and WhatsApp sender.");
        var appointment = appointments.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Appointment not found."));
        if (!Set.of("PENDING", "CONFIRMED", "SCHEDULED").contains(Objects.toString(appointment.getStatus(), "").toUpperCase(Locale.ROOT)))
            throw error(HttpStatus.BAD_REQUEST, "Reminders are only available for active appointments.");
        var patient = patients.findById(appointment.getPatientId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Patient not found."));
        String phone = normalizePhone(patient.getPhone());
        String template = setting("content-sid");
        String reportText = template.isBlank() ? appointmentText(appointment) : "";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("To", "whatsapp:" + phone);
        fields.put("From", from.startsWith("whatsapp:") ? from : "whatsapp:" + from);
        fields.putAll(messageContent(template, reportText, request.sessionConfirmed()));
        String form = fields.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).reduce((a,b) -> a + "&" + b).orElse("");
        HttpRequest outbound = HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + account + "/Messages.json"))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((key + ":" + secret).getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response;
        try { response = client.send(outbound, HttpResponse.BodyHandlers.ofString()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw error(HttpStatus.BAD_GATEWAY, "Sending interrupted. Check Twilio logs before retrying."); }
        catch (java.io.IOException e) { throw error(HttpStatus.BAD_GATEWAY, "Delivery submission could not be confirmed. Check Twilio logs before retrying."); }
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw error(HttpStatus.BAD_GATEWAY, rejectionMessage(response.statusCode(), response.body())
                + " Response: " + responsePreview(response.body(), key, secret, account, phone, from, reportText,
                    setting("signing-secret"), setting("operator-password"),
                    Base64.getEncoder().encodeToString((key + ":" + secret).getBytes(StandardCharsets.UTF_8))));
        var result = json.readTree(response.body());
        return Map.of("sid", result.path("sid").asText(), "status", result.path("status").asText("queued"));
    }

    @GetMapping("/media/{id}/{expires}/{token}.pdf")
    public ResponseEntity<byte[]> media(@PathVariable Long id, @PathVariable long expires, @PathVariable String token) throws Exception {
        if (!"true".equals(setting("enabled")) || setting("signing-secret").length() < 32 || expires <= Instant.now().getEpochSecond()
            || !MessageDigest.isEqual(signature(id, expires).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            throw error(HttpStatus.NOT_FOUND, "Report link is invalid or expired.");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Cache-Control", "no-store")
            .header("Content-Disposition", "attachment; filename=lab-report.pdf").body(lab.generateLabReportPdf(id));
    }

    static String rejectionMessage(int status, String body) {
        String code = "";
        try {
            var value = new ObjectMapper().readTree(body).path("code").asText("");
            if (value.matches("[1-9][0-9]{3,7}")) code = value;
        } catch (Exception ignored) {
            // Never expose provider response bodies, which can contain patient or account data.
        }
        if (code.isEmpty() && body != null) {
            // Some gateways return Twilio's XML error format despite the JSON endpoint.
            // Match only a numeric Code element; never parse external XML entities.
            var match = java.util.regex.Pattern.compile("<Code>\\s*([1-9][0-9]{3,7})\\s*</Code>").matcher(body);
            if (match.find()) code = match.group(1);
        }
        return "Twilio rejected the request (HTTP " + status
            + (code.isEmpty() ? "" : ", error " + code) + "). "
            + (code.isEmpty() ? "Check Twilio Console error logs." : "Details: https://www.twilio.com/docs/api/errors/" + code);
    }

    static String appointmentText(com.hospital.app.model.Appointment appointment) {
        return "Hospital appointment reminder\nAppointment #" + appointment.getId()
            + "\nDate: " + appointment.getAppointmentDate()
            + "\nTime: " + appointment.getAppointmentTime()
            + "\nPlease contact the hospital to confirm or reschedule.";
    }

    static Map<String, String> messageContent(String template, String text, boolean sessionConfirmed) {
        if (!template.isBlank()) {
            if (!template.matches("HX[0-9a-fA-F]{32}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid template SID.");
            return Map.of("ContentSid", template);
        }
        if (!sessionConfirmed) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Text-only sending requires a message from the patient within the last 24 hours.");
        validateTextLength(text);
        return Map.of("Body", text);
    }

    static void validateTextLength(String text) {
        if (text.length() > 1600) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Report exceeds the 1600-character text limit. Nothing was sent; use PDF delivery for this report.");
    }

    static String responsePreview(String body, String... sensitiveValues) {
        if (body == null || body.isBlank()) return "Empty response body.";
        String preview = body;
        for (String value : sensitiveValues) {
            if (value != null && !value.isBlank()) {
                preview = preview.replace(value, "[redacted]").replace(encode(value), "[redacted]");
            }
        }
        preview = preview.replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ")
            .replaceAll("<[^>]*>", " ")
            .replaceAll("(?i)https?://[^\\s\\\"<>]+", "[URL]")
            .replaceAll("[A-Za-z0-9_+/=-]{24,}", "[identifier]")
            .replaceAll("\\+?[0-9][0-9 ()-]{7,}[0-9]", "[number]")
            .replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        return preview.substring(0, Math.min(preview.length(), 600));
    }

    private String signature(Long id, long expires) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(setting("signing-secret").getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((id + ":" + expires).getBytes(StandardCharsets.UTF_8)));
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    static String normalizePhone(String value) {
        String phone = value == null ? "" : value.replaceAll("[\\s()\\-]", "");
        if (phone.matches("[6-9][0-9]{9}")) phone = "+91" + phone;
        if (!phone.matches("\\+[1-9][0-9]{7,14}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient phone must include a valid country code, for example +919876543210.");
        return phone;
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", Objects.requireNonNullElse(e.getReason(), "Request failed.")));
    }
}
