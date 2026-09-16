package com.hospital.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class InputTextValidator implements ConstraintValidator<InputText, String> {
    private InputText rule;
    @Override public void initialize(InputText rule) { this.rule = rule; }
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.strip().isEmpty()) return !rule.required();
        String text = value.strip();
        String error = null;
        if (value.length() > rule.max()) error = "Use at most " + rule.max() + " characters.";
        else if (Pattern.compile("[\\p{Cc}&&[^\\n\\r\\t]]").matcher(value).find()) error = "Remove unsupported control characters.";
        else {
            switch (rule.kind()) {
                case "name":
                    if (!Pattern.compile("\\p{L}").matcher(text).find() || Pattern.compile("[<>\\r\\n\\t]").matcher(text).find()) error = "Enter a name containing letters; single and international names are welcome.";
                    break;
                case "email":
                    if (!text.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+") || text.startsWith(".") || text.contains("..") || text.substring(0, text.indexOf('@')).endsWith(".")) error = "Enter a valid email address, such as name@example.com.";
                    break;
                case "phone":
                    String digits = text.replaceAll("[^0-9]", "");
                    if (!text.matches("\\+?[0-9 ()-]+") || digits.length() < 7 || digits.length() > 15) error = "Enter 7 to 15 digits; an optional +, spaces, parentheses and hyphens are allowed.";
                    break;
                case "address":
                    if (text.length() < 5 || !Pattern.compile("[\\p{L}\\p{N}]").matcher(text).find()) error = "Enter an address of at least 5 characters.";
                    break;
                case "date": case "pastDate": case "birthDate":
                    try {
                        if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("date", text, 0);
                        LocalDate date = LocalDate.parse(text);
                        if (date.getYear() < 1) error = "Enter a valid calendar date.";
                        else if (!rule.kind().equals("date") && date.isAfter(LocalDate.now())) error = "Date cannot be in the future.";
                        else if (rule.kind().equals("birthDate") && date.isBefore(LocalDate.now().minusYears(150))) error = "Check the birth date; age cannot exceed 150 years.";
                    } catch (DateTimeParseException ex) { error = "Enter a valid calendar date (YYYY-MM-DD)."; }
                    break;
                case "time":
                    try { if (!text.matches("\\d{2}:\\d{2}(:\\d{2})?")) throw new DateTimeParseException("time", text, 0); LocalTime.parse(text); }
                    catch (DateTimeParseException ex) { error = "Enter a valid time (HH:mm)."; }
                    break;
                case "upi":
                    if (!text.matches("[A-Za-z0-9._-]{2,}@[A-Za-z][A-Za-z0-9.-]{1,}")) error = "Enter a valid UPI ID, such as hospital@bank.";
                    break;
                case "url":
                    try { var uri = java.net.URI.create(text); if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null || uri.getUserInfo() != null) error = "Enter a complete http or https URL."; }
                    catch (IllegalArgumentException ex) { error = "Enter a complete http or https URL."; }
                    break;
            }
        }
        if (error == null) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }
}
