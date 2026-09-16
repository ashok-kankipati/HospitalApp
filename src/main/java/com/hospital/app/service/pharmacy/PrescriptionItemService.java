package com.hospital.app.service.pharmacy;

import com.hospital.app.model.pharmacy.PrescriptionItem;
import com.hospital.app.repository.pharmacy.PrescriptionItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionItemService {
    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    public List<PrescriptionItem> getAllItems() {
        return prescriptionItemRepository.findAll();
    }

    public Optional<PrescriptionItem> getItemById(Long id) {
        return prescriptionItemRepository.findById(id);
    }

    public PrescriptionItem saveItem(PrescriptionItem item) {
        return prescriptionItemRepository.save(item);
    }
}
