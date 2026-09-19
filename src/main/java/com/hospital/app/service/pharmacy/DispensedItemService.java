package com.hospital.app.service.pharmacy;

import com.hospital.app.model.pharmacy.DispensedItem;
import com.hospital.app.repository.pharmacy.DispensedItemRepository;
import com.hospital.app.service.billing.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DispensedItemService {
    @Autowired
    private DispensedItemRepository dispensedItemRepository;

    @Autowired
    private InvoiceService invoiceService;

    public List<DispensedItem> getAllDispensedItems() {
        return dispensedItemRepository.findAll();
    }

    public Optional<DispensedItem> getDispensedItemById(Long id) {
        return dispensedItemRepository.findById(id);
    }

    public DispensedItem saveDispensedItem(DispensedItem item) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Use the prescription dispense workflow; direct dispense records bypass stock and billing validation.");
    }
}
