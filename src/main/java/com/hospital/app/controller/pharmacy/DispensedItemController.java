package com.hospital.app.controller.pharmacy;

import com.hospital.app.model.pharmacy.DispensedItem;
import com.hospital.app.service.pharmacy.DispensedItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy/dispensed-items")
public class DispensedItemController {
    @Autowired
    private DispensedItemService dispensedItemService;

    @GetMapping
    public List<DispensedItem> getAllDispensedItems() {
        return dispensedItemService.getAllDispensedItems();
    }

    @GetMapping("/{id}")
    public Optional<DispensedItem> getDispensedItemById(@PathVariable Long id) {
        return dispensedItemService.getDispensedItemById(id);
    }

    @PostMapping
    public DispensedItem addDispensedItem(@jakarta.validation.Valid @RequestBody DispensedItem item) {
        return dispensedItemService.saveDispensedItem(item);
    }
}
