package com.hospital.app.service.notification;

import org.springframework.stereotype.Component;

@Component
public class RoleEmailTemplate {
    public EmailContent render(String role, String eventType, String subject, String body) {
        String audience = audienceName(role);
        String title = titleFor(eventType, subject);
        String action = actionFor(role, eventType);
        String plainText = "Hello " + audience + ",\n\n" + body + "\n\n" + action
                + "\n\nCareFlow Hospital Operations";
        String html = """
                <!doctype html><html><body style="margin:0;background:#f2f6f5;font-family:Arial,sans-serif;color:#17312b">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f2f6f5;padding:28px 12px">
                  <tr><td align="center"><table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #dce7e3;border-radius:12px;overflow:hidden">
                    <tr><td style="background:#0d5c4d;padding:22px 28px;color:#ffffff"><div style="font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase;opacity:.8">%s</div><div style="font-size:24px;font-weight:700;margin-top:8px">%s</div></td></tr>
                    <tr><td style="padding:28px"><p style="margin:0 0 16px;font-size:16px">Hello %s,</p><div style="font-size:15px;line-height:1.7;color:#38554e">%s</div><div style="margin-top:22px;padding:15px 17px;background:#edf7f4;border-left:4px solid #19a47b;border-radius:6px;font-size:14px;font-weight:700;color:#164d40">%s</div></td></tr>
                    <tr><td style="border-top:1px solid #e6eeeb;padding:18px 28px;font-size:12px;color:#71827e">CareFlow Hospital Operations · Secure clinical notification</td></tr>
                  </table></td></tr>
                </table></body></html>
                """.formatted(escape(eyebrowFor(eventType)), escape(title), escape(audience), paragraphs(body), escape(action));
        return new EmailContent(title, plainText, html);
    }

    private String audienceName(String role) {
        return switch (role == null ? "" : role) {
            case "Doctor" -> "Doctor";
            case "Lab", "Lab Technician" -> "Lab Team";
            case "Billing", "Receptionist" -> "Reception Team";
            case "Patient" -> "Patient";
            default -> "Care Team";
        };
    }

    private String titleFor(String eventType, String fallback) {
        return switch (eventType == null ? "" : eventType) {
            case "LAB_REPORT_READY" -> "Lab report ready for review";
            case "BILLING_REMINDER" -> "New invoice ready for billing";
            case "BILLING_PAID" -> fallback == null || fallback.isBlank() ? "Payment confirmed" : fallback;
            case "APPOINTMENT_REMINDER" -> "Appointment workflow update";
            default -> fallback == null || fallback.isBlank() ? "CareFlow notification" : fallback;
        };
    }

    private String eyebrowFor(String eventType) {
        return switch (eventType == null ? "" : eventType) {
            case "LAB_REPORT_READY" -> "Clinical results";
            case "BILLING_REMINDER", "BILLING_PAID" -> "Billing update";
            case "APPOINTMENT_REMINDER" -> "Schedule update";
            default -> "Hospital update";
        };
    }

    private String actionFor(String role, String eventType) {
        if ("LAB_REPORT_READY".equals(eventType)) {
            return "Doctor".equals(role) ? "Review the report and update the patient care plan."
                    : "Confirm the report is complete and available to the clinical team.";
        }
        if ("BILLING_REMINDER".equals(eventType)) {
            return "Open Billing to review the invoice and coordinate payment with the patient.";
        }
        if ("BILLING_PAID".equals(eventType)) {
            return "Your payment is confirmed. Keep the attached documents for your records.";
        }
        return "Open CareFlow to review the latest details.";
    }

    private String paragraphs(String value) {
        return escape(value == null ? "" : value).replace("\r\n", "\n").replace("\n", "<br>");
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public record EmailContent(String subject, String plainText, String html) {}
}