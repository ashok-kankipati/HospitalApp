package com.hospital.app.service.pharmacy;

import com.hospital.app.dto.pharmacy.DispenseCreateRequest;
import com.hospital.app.dto.pharmacy.DispenseItemRequest;
import com.hospital.app.model.pharmacy.Dispense;
import com.hospital.app.model.pharmacy.DispenseItem;
import com.hospital.app.model.pharmacy.DispensedItem;
import com.hospital.app.model.pharmacy.MedicineBatch;
import com.hospital.app.model.pharmacy.PrescriptionItem;
import com.hospital.app.model.pharmacy.StockTransaction;
import com.hospital.app.repository.pharmacy.DispenseItemRepository;
import com.hospital.app.repository.pharmacy.DispenseRepository;
import com.hospital.app.repository.pharmacy.DispensedItemRepository;
import com.hospital.app.repository.pharmacy.MedicineBatchRepository;
import com.hospital.app.repository.pharmacy.PrescriptionItemRepository;
import com.hospital.app.repository.pharmacy.StockTransactionRepository;
import com.hospital.app.service.billing.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DispenseService {
    @Autowired
    private DispenseRepository dispenseRepository;

    @Autowired
    private DispenseItemRepository dispenseItemRepository;

    @Autowired
    private MedicineBatchRepository medicineBatchRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private DispensedItemRepository dispensedItemRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private com.hospital.app.repository.pharmacy.PrescriptionRepository prescriptionRepository;

    @Transactional
    public Dispense createDispense(DispenseCreateRequest request) {
        if (request.getPrescriptionId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("A prescription and at least one dispense item are required.");
        }

        // Serialize requests before reading the dispense ledger, including stale browser retries.
        prescriptionRepository.findForDispensing(request.getPrescriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));

        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(request.getPrescriptionId());
        Map<Long, PrescriptionItem> prescriptionItemMap = prescriptionItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(PrescriptionItem::getId, Function.identity()));
        Map<Long, Integer> requestedByPrescriptionItem = request.getItems().stream()
                .filter(item -> item.getPrescriptionItemId() != null && item.getQuantity() != null)
                .collect(Collectors.groupingBy(DispenseItemRequest::getPrescriptionItemId,
                        Collectors.summingInt(DispenseItemRequest::getQuantity)));
        Map<DispenseItemRequest, MedicineBatch> resolvedBatches = new java.util.IdentityHashMap<>();

        for (DispenseItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getPrescriptionItemId() == null || itemRequest.getMedicineBatchId() == null
                    || itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each dispense item must include a positive quantity and prescription item.");
            }
                PrescriptionItem prescriptionItem = prescriptionItemMap.get(itemRequest.getPrescriptionItemId());
                MedicineBatch prescribedBatch = prescriptionItem == null ? null : prescriptionItem.getMedicineBatch();
                MedicineBatch requestedBatch = medicineBatchRepository.findById(itemRequest.getMedicineBatchId()).orElse(null);
                if (prescribedBatch == null || requestedBatch == null || requestedBatch.getMedicine() == null
                    || prescribedBatch.getMedicine() == null
                    || !prescribedBatch.getMedicine().getId().equals(requestedBatch.getMedicine().getId())) {
                throw new IllegalArgumentException("Dispense item does not belong to this prescription.");
            }
                resolvedBatches.put(itemRequest, resolveUsableBatch(prescribedBatch, itemRequest.getQuantity()));
            int alreadyDispensed = dispensedItemRepository.findAll().stream()
                    .filter(item -> item.getPrescriptionItem() != null
                            && itemRequest.getPrescriptionItemId().equals(item.getPrescriptionItem().getId()))
                    .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                    .sum();
            if (itemRequest.getExpectedDispensedQuantity() == null
                    || itemRequest.getExpectedDispensedQuantity() != alreadyDispensed) {
                throw new IllegalArgumentException("Dispensing history changed. Refresh the prescription before dispensing again.");
            }
            if (alreadyDispensed + requestedByPrescriptionItem.get(itemRequest.getPrescriptionItemId()) > prescriptionItem.getQuantity()) {
                throw new IllegalArgumentException("Dispense quantity exceeds the remaining prescription quantity.");
            }
        }

        Map<Long, Integer> requestedByResolvedBatch = request.getItems().stream()
            .collect(Collectors.groupingBy(item -> resolvedBatches.get(item).getId(),
                Collectors.summingInt(DispenseItemRequest::getQuantity)));
        for (Map.Entry<Long, Integer> entry : requestedByResolvedBatch.entrySet()) {
            MedicineBatch batch = medicineBatchRepository.findById(entry.getKey()).orElseThrow();
            int available = calculateAvailableQuantity(batch.getId(), batch.getQuantity());
            if (entry.getValue() > available) {
                throw new IllegalArgumentException("Not enough stock for " + batch.getMedicine().getName()
                        + " batch " + batch.getBatchNo() + ". Available: " + available + ", requested: " + entry.getValue());
            }
        }

        Dispense dispense = new Dispense();
        dispense.setPrescriptionId(request.getPrescriptionId());
        dispense.setDispensedBy(request.getDispensedBy());
        Dispense saved = dispenseRepository.save(dispense);

        List<DispenseItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        List<DispensedItem> dispensedItems = new ArrayList<>();
        for (DispenseItemRequest itemRequest : request.getItems()) {
                MedicineBatch batch = resolvedBatches.get(itemRequest);
                BigDecimal unitPrice = BigDecimal.valueOf(batch != null && batch.getPrice() != null ? batch.getPrice() : 0.0);
                BigDecimal qty = BigDecimal.valueOf(itemRequest.getQuantity());
                BigDecimal lineTotal = unitPrice.multiply(qty);

                DispenseItem item = new DispenseItem();
                item.setDispenseId(saved.getId());
                item.setMedicineBatchId(itemRequest.getMedicineBatchId());
                item.setQuantity(itemRequest.getQuantity());
                item.setUnitPrice(unitPrice);
                item.setLineTotal(lineTotal);
                items.add(item);
                total = total.add(lineTotal);

                StockTransaction transaction = new StockTransaction();
                transaction.setBatch(batch);
                transaction.setTransactionType("OUT");
                transaction.setQuantity(itemRequest.getQuantity());
                transaction.setReference("PRESC-" + request.getPrescriptionId());
                transaction.setTransactionDate(LocalDateTime.now());
                stockTransactionRepository.save(transaction);

                PrescriptionItem prescriptionItem = prescriptionItemMap.get(itemRequest.getPrescriptionItemId());
                DispensedItem dispensedItem = new DispensedItem();
                dispensedItem.setPrescriptionItem(prescriptionItem);
                dispensedItem.setDispensedBy(request.getDispensedBy());
                dispensedItem.setDispensedDate(LocalDateTime.now());
                dispensedItem.setQuantity(itemRequest.getQuantity());
                dispensedItems.add(dispensedItem);
        }

        if (!items.isEmpty()) {
            dispenseItemRepository.saveAll(items);
        }

        saved.setTotalAmount(total);
        for (DispensedItem dispensedItem : dispensedItems) {
            invoiceService.addOrUpdateDispensedItemInvoice(dispensedItemRepository.save(dispensedItem));
        }
        return dispenseRepository.save(saved);
    }

    private int calculateAvailableQuantity(Long batchId, Integer baseQuantity) {
        return Math.max((baseQuantity == null ? 0 : baseQuantity) + stockTransactionRepository.findByBatch_Id(batchId).stream()
                .mapToInt(transaction -> "IN".equalsIgnoreCase(transaction.getTransactionType())
                        ? transaction.getQuantity() : -transaction.getQuantity())
                .sum(), 0);
    }

    private MedicineBatch resolveUsableBatch(MedicineBatch prescribedBatch, int requestedQuantity) {
        if (isUsableBatch(prescribedBatch, requestedQuantity)) {
            return prescribedBatch;
        }

        return medicineBatchRepository.findByMedicine_IdAndIsActiveTrueOrderByIdDesc(
                        prescribedBatch.getMedicine().getId()).stream()
                .filter(batch -> isUsableBatch(batch, requestedQuantity))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No available active batch for "
                        + prescribedBatch.getMedicine().getName() + "."));
    }

    private boolean isUsableBatch(MedicineBatch batch, int requestedQuantity) {
        if (batch == null || !Boolean.TRUE.equals(batch.getIsActive()) || batch.getExpiryDate() == null
                || batch.getExpiryDate().isBefore(LocalDate.now())) {
            return false;
        }
        return calculateAvailableQuantity(batch.getId(), batch.getQuantity()) >= requestedQuantity;
    }

    public List<Dispense> getDispenseByPrescription(Long prescriptionId) {
        return dispenseRepository.findByPrescriptionId(prescriptionId);
    }

    public List<DispenseItem> getItemsByDispense(Long dispenseId) {
        return dispenseItemRepository.findByDispenseId(dispenseId);
    }
}
