package com.hospital.app.repository.pharmacy;

import com.hospital.app.model.pharmacy.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    long countByBatch_Id(Long batchId);
    List<StockTransaction> findByBatch_Id(Long batchId);
}
