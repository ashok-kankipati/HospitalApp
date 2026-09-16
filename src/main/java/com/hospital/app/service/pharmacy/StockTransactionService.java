package com.hospital.app.service.pharmacy;

import com.hospital.app.model.pharmacy.StockTransaction;
import com.hospital.app.repository.pharmacy.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StockTransactionService {
    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAll();
    }

    public Optional<StockTransaction> getTransactionById(Long id) {
        return stockTransactionRepository.findById(id);
    }

    public StockTransaction saveTransaction(StockTransaction transaction) {
        return stockTransactionRepository.save(transaction);
    }
}
