package com.hospital.app.validation;

import com.hospital.app.model.*;
import com.hospital.app.dto.billing.*;
import com.hospital.app.dto.pharmacy.*;
import com.hospital.app.dto.lab.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InputValidationTest {
    static ValidatorFactory factory;
    static Validator validator;
    @BeforeAll static void setup() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll static void close() { factory.close(); }
    Patient patient() {
        var patient = new Patient(); patient.setName("O'Connor"); patient.setEmail("person+care@example.test");
        patient.setPhone("+91 (98765) 43210"); patient.setDateOfBirth("2000-02-29"); return patient;
    }
    boolean invalid(Object value, String field) { return validator.validate(value).stream().anyMatch(v -> v.getPropertyPath().toString().equals(field)); }
    @Test void permitsInternationalAndSingleNamesAndOptionalAddress() {
        for (String name : List.of("Li", "A", "O'Connor", "Anne-Marie", "ఆశోక్", "李", "María José")) {
            var patient = patient(); patient.setName(name); assertTrue(validator.validate(patient).isEmpty(), name);
        }
    }
    @Test void rejectsBlankNamesMalformedContactsAndShortAddress() {
        var patient = patient(); patient.setName("   "); patient.setEmail("a@bad"); patient.setPhone("123"); patient.setAddress("---");
        for (String field : List.of("name", "email", "phone", "address")) assertTrue(invalid(patient, field), field);
        patient.setName("12345"); assertTrue(invalid(patient, "name"));
        patient.setName("A".repeat(101)); assertTrue(invalid(patient, "name"));
    }
    @Test void checksRealCalendarDatesFutureBirthDatesAndAge() {
        for (String date : List.of("2025-02-29", "2000-13-01", "2000-2-1", LocalDate.now().plusDays(1).toString(), LocalDate.now().minusYears(151).toString())) {
            var patient = patient(); patient.setDateOfBirth(date); assertTrue(invalid(patient, "dateOfBirth"), date);
        }
        var history = new PatientMedicalHistory(); history.setHistoryDate(LocalDate.now().plusDays(1).toString()); assertTrue(invalid(history, "historyDate"));
    }
    @Test void validatesMoneyAndPaymentMethod() {
        var payment = new InvoicePaymentRequest(); payment.setPaymentMethod("Cash"); payment.setAmount(new BigDecimal("10.25"));
        assertTrue(validator.validate(payment).isEmpty());
        for (String amount : List.of("0", "-1", "1.001", "100000000")) { payment.setAmount(new BigDecimal(amount)); assertTrue(invalid(payment, "amount")); }
        payment.setPaymentMethod("FAKE"); assertTrue(invalid(payment, "paymentMethod"));
    }
    @Test void validatesNestedDispenseRowsAndReferenceIds() {
        var dispense = new DispenseCreateRequest(); dispense.setPrescriptionId(1L); dispense.setDispensedBy(1L);
        dispense.setItems(List.of()); assertTrue(invalid(dispense, "items"));
        var item = new DispenseItemRequest(); item.setPrescriptionItemId(1L); item.setMedicineBatchId(-1L); item.setQuantity(0); dispense.setItems(List.of(item));
        assertTrue(invalid(dispense, "items[0].quantity")); assertTrue(invalid(dispense, "items[0].medicineBatchId"));
        var batch = new com.hospital.app.model.pharmacy.MedicineBatch(); batch.setMedicine(new com.hospital.app.model.pharmacy.Medicine()); assertTrue(invalid(batch, "medicine"));
    }
    @Test void rejectsUnsafeReportUrlsAndEmptyLabOrders() {
        var report = new LabReportRequest(); report.setVisitId(1L); report.setFileName("report.pdf"); report.setFileUrl("javascript:alert(1)");
        assertTrue(invalid(report, "fileUrl")); report.setFileUrl("/relative/report.pdf"); assertTrue(invalid(report, "fileUrl")); report.setFileUrl("https://hospital.example/reports/1.pdf"); assertTrue(validator.validate(report).isEmpty());
        var order = new LabOrderRequest(); order.setTestIds(List.of()); assertTrue(invalid(order, "testIds"));
        order.setTestIds(List.of(-1L)); assertFalse(validator.validate(order).isEmpty());
    }
}
