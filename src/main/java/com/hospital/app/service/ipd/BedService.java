package com.hospital.app.service.ipd;

import com.hospital.app.model.ipd.Bed;
import com.hospital.app.repository.ipd.BedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BedService {
    @Autowired
    private BedRepository bedRepository;

    public List<Bed> getAllBeds() {
        return bedRepository.findAll();
    }

    public Bed addBed(Bed bed) {
        if (bed.getStatus() == null) {
            bed.setStatus("AVAILABLE");
        }
        return bedRepository.save(bed);
    }

    public Bed updateBedStatus(Long bedId, String status) {
        if (status == null || !java.util.Set.of("AVAILABLE", "OCCUPIED", "MAINTENANCE").contains(status))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Select a valid bed status.");
        Bed bed = bedRepository.findById(bedId).orElseThrow();
        bed.setStatus(status);
        return bedRepository.save(bed);
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        long total = bedRepository.count();
        long available = bedRepository.countByStatus("AVAILABLE");
        long occupied = bedRepository.countByStatus("OCCUPIED");
        long icu = bedRepository.countByType("ICU");
        long general = bedRepository.countByType("GENERAL");
        long priv = bedRepository.countByType("PRIVATE");
        summary.put("total", total);
        summary.put("available", available);
        summary.put("occupied", occupied);
        summary.put("icu", icu);
        summary.put("general", general);
        summary.put("private", priv);
        return summary;
    }
}
