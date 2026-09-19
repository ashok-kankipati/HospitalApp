package com.hospital.app.service.billing;

import com.hospital.app.model.Appointment;
import com.hospital.app.model.billing.*;
import com.hospital.app.model.pharmacy.*;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.billing.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DispensedInvoiceTest {
    @Test void dispensingReplacesPrescriptionChargeAndReplayKeepsFullyPaidInvoicePaid() {
        var service = new InvoiceService();
        var invoices = mock(InvoiceRepository.class);
        var items = mock(InvoiceItemRepository.class);
        var appointments = mock(AppointmentRepository.class);
        ReflectionTestUtils.setField(service, "invoiceRepository", invoices);
        ReflectionTestUtils.setField(service, "invoiceItemRepository", items);
        ReflectionTestUtils.setField(service, "appointmentRepository", appointments);
        var appointment = new Appointment(); appointment.setId(1L);
        var invoice = new Invoice(); invoice.setId(1L); invoice.setAmountPaid(new BigDecimal("640"));
        var fixed = new InvoiceItem(); fixed.setLineTotal(new BigDecimal("140"));
        var estimate = new InvoiceItem(); estimate.setReferenceType("PRESCRIPTION_ITEM");
        estimate.setReferenceId(10L); estimate.setLineTotal(new BigDecimal("500"));
        List<InvoiceItem> stored = new ArrayList<>(List.of(fixed, estimate));
        when(appointments.findForBilling(1L)).thenReturn(Optional.of(appointment));
        when(invoices.findByAppointmentId(1L)).thenReturn(List.of(invoice));
        when(items.findByInvoiceId(1L)).thenAnswer(call -> new ArrayList<>(stored));
        when(items.findByInvoiceIdAndReferenceTypeAndReferenceId(1L, "PRESCRIPTION_ITEM", 10L))
                .thenAnswer(call -> stored.stream().filter(i -> "PRESCRIPTION_ITEM".equals(i.getReferenceType())).toList());
        doAnswer(call -> { ((Iterable<InvoiceItem>) call.getArgument(0)).forEach(stored::remove); return null; })
                .when(items).deleteAll(any());
        when(items.existsByReferenceTypeAndReferenceId(eq("DISPENSED_ITEM"), anyLong()))
                .thenAnswer(call -> stored.stream().anyMatch(i -> "DISPENSED_ITEM".equals(i.getReferenceType())
                        && Objects.equals(i.getReferenceId(), call.getArgument(1))));
        when(items.save(any())).thenAnswer(call -> { InvoiceItem item = call.getArgument(0); stored.add(item); return item; });
        var prescription = new Prescription(); prescription.setAppointmentId(1L);
        var batch = new MedicineBatch(); batch.setPrice(100.0);
        var prescribed = new PrescriptionItem(); prescribed.setId(10L);
        prescribed.setPrescription(prescription); prescribed.setMedicineBatch(batch);
        var dispensed = new DispensedItem(); dispensed.setId(20L);
        dispensed.setPrescriptionItem(prescribed); dispensed.setQuantity(5);
        service.addOrUpdateDispensedItemInvoice(dispensed);
        service.addOrUpdateDispensedItemInvoice(dispensed);
        assertEquals(0, new BigDecimal("640").compareTo(invoice.getTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getBalanceDue()));
        assertEquals("PAID", invoice.getStatus());
        assertEquals(2, stored.size());
        verify(items, times(1)).save(any());
    }
}
