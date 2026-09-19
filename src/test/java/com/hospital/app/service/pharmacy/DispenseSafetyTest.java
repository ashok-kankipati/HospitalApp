package com.hospital.app.service.pharmacy;

import com.hospital.app.dto.pharmacy.*;
import com.hospital.app.model.pharmacy.*;
import com.hospital.app.repository.pharmacy.*;
import com.hospital.app.service.billing.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DispenseSafetyTest {
    DispenseService service = new DispenseService();
    PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    PrescriptionItemRepository prescriptionItems = mock(PrescriptionItemRepository.class);
    MedicineBatchRepository batches = mock(MedicineBatchRepository.class);
    DispensedItemRepository ledger = mock(DispensedItemRepository.class);
    DispenseRepository dispenses = mock(DispenseRepository.class);
    DispenseItemRepository lines = mock(DispenseItemRepository.class);
    StockTransactionRepository stock = mock(StockTransactionRepository.class);
    InvoiceService billing = mock(InvoiceService.class);
    PrescriptionItem prescribed = new PrescriptionItem();
    List<DispensedItem> history = new ArrayList<>();

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "prescriptionRepository", prescriptions);
        ReflectionTestUtils.setField(service, "prescriptionItemRepository", prescriptionItems);
        ReflectionTestUtils.setField(service, "medicineBatchRepository", batches);
        ReflectionTestUtils.setField(service, "dispensedItemRepository", ledger);
        ReflectionTestUtils.setField(service, "dispenseRepository", dispenses);
        ReflectionTestUtils.setField(service, "dispenseItemRepository", lines);
        ReflectionTestUtils.setField(service, "stockTransactionRepository", stock);
        ReflectionTestUtils.setField(service, "invoiceService", billing);
        var prescription = new Prescription(); prescription.setId(1L);
        var medicine = new Medicine(); medicine.setId(1L); medicine.setName("DOLO");
        var batch = new MedicineBatch(); batch.setId(1L); batch.setMedicine(medicine);
        batch.setPrice(100.0); batch.setQuantity(100); batch.setIsActive(true);
        batch.setExpiryDate(LocalDate.now().plusYears(1));
        prescribed.setId(1L); prescribed.setPrescription(prescription);
        prescribed.setMedicineBatch(batch); prescribed.setQuantity(7);
        when(prescriptions.findForDispensing(1L)).thenReturn(Optional.of(prescription));
        when(prescriptionItems.findByPrescriptionId(1L)).thenReturn(List.of(prescribed));
        when(batches.findById(1L)).thenReturn(Optional.of(batch));
        when(ledger.findAll()).thenAnswer(call -> new ArrayList<>(history));
        when(ledger.save(any())).thenAnswer(call -> {
            DispensedItem item = call.getArgument(0); item.setId((long) history.size()+1);
            history.add(item); return item;
        });
        when(dispenses.save(any())).thenAnswer(call -> {
            Dispense item = call.getArgument(0); item.setId(1L); return item;
        });
    }

    DispenseCreateRequest request(int quantity, Integer expected) {
        var line = new DispenseItemRequest(); line.setPrescriptionItemId(1L);
        line.setMedicineBatchId(1L); line.setQuantity(quantity); line.setExpectedDispensedQuantity(expected);
        var request = new DispenseCreateRequest(); request.setPrescriptionId(1L);
        request.setDispensedBy(1L); request.setItems(List.of(line)); return request;
    }

    @Test void repeatedPartialSubmissionDoesNotDispenseOrBillAgain() {
        var first = request(2, 0);
        service.createDispense(first);
        assertThrows(IllegalArgumentException.class, () -> service.createDispense(first));
        assertEquals(1, history.size());
        verify(billing, times(1)).addOrUpdateDispensedItemInvoice(any());
        verify(stock, times(1)).save(any());
        var order = inOrder(prescriptions, prescriptionItems);
        order.verify(prescriptions).findForDispensing(1L);
        order.verify(prescriptionItems).findByPrescriptionId(1L);
    }

    @Test void freshRemainingQuantityCanBeDispensedOnce() {
        service.createDispense(request(5, 0));
        service.createDispense(request(2, 5));
        assertEquals(7, history.stream().mapToInt(DispensedItem::getQuantity).sum());
        assertThrows(IllegalArgumentException.class, () -> service.createDispense(request(1, 7)));
        verify(billing, times(2)).addOrUpdateDispensedItemInvoice(any());
    }

    @Test void oldClientsMustRefreshBeforeWriting() {
        assertThrows(IllegalArgumentException.class, () -> service.createDispense(request(2, null)));
        verifyNoInteractions(billing, dispenses, lines);
        verify(stock, never()).save(any());
    }

    @Test void directLedgerWriteCannotBypassValidatedDispense() {
        var legacy = new DispensedItemService();
        ReflectionTestUtils.setField(legacy, "dispensedItemRepository", ledger);
        ReflectionTestUtils.setField(legacy, "invoiceService", billing);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> legacy.saveDispensedItem(new DispensedItem()));
        verifyNoInteractions(ledger, billing);
    }
}
