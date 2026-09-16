package com.hospital.app.controller.pharmacy;

import com.hospital.app.model.pharmacy.StockTransaction;
import com.hospital.app.service.pharmacy.StockTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy/transactions")
public class StockTransactionController {
    @Autowired
    private StockTransactionService stockTransactionService;

    @GetMapping
    public List<StockTransaction> getAllTransactions() {
        return stockTransactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Optional<StockTransaction> getTransactionById(@PathVariable Long id) {
        return stockTransactionService.getTransactionById(id);
    }

    @PostMapping
    public StockTransaction addTransaction(@jakarta.validation.Valid @RequestBody StockTransaction transaction) {
        return stockTransactionService.saveTransaction(transaction);
    }
}
