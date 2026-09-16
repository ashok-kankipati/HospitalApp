package com.hospital.app.service.pharmacy;

import com.hospital.app.model.pharmacy.MedicineBatch;
import com.hospital.app.repository.pharmacy.MedicineBatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineBatchService {
    @Autowired
    private MedicineBatchRepository batchRepository;

    public List<MedicineBatch> getAllBatches() {
        return batchRepository.findByIsActiveTrue();
    }

    public Optional<MedicineBatch> getBatchById(Long id) {
        return batchRepository.findById(id);
    }

    public MedicineBatch saveBatch(MedicineBatch batch) {
        if (batch.getIsActive() == null) {
            batch.setIsActive(true);
        }
        return batchRepository.save(batch);
    }

    public void deleteBatch(Long id) {
        batchRepository.findById(id).ifPresent(batch -> {
            batch.setIsActive(false);
            batchRepository.save(batch);
        });
    }
}
