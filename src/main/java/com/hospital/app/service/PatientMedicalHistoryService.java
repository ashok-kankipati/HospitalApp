package com.hospital.app.service;

import com.hospital.app.model.PatientMedicalHistory;
import com.hospital.app.repository.PatientMedicalHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PatientMedicalHistoryService {

    @Autowired
    private PatientMedicalHistoryRepository historyRepository;

    public List<PatientMedicalHistory> getHistoryByPatientId(Long patientId) {
        return historyRepository.findByPatientId(patientId);
    }

    public Optional<PatientMedicalHistory> getHistoryById(Long historyId) {
        return historyRepository.findById(historyId);
    }

    public PatientMedicalHistory createHistory(PatientMedicalHistory history) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        history.setRecordedAt(LocalDateTime.now().format(formatter));
        return historyRepository.save(history);
    }

    public PatientMedicalHistory updateHistory(Long historyId, PatientMedicalHistory historyDetails) {
        Optional<PatientMedicalHistory> history = historyRepository.findById(historyId);
        if (history.isPresent()) {
            PatientMedicalHistory existingHistory = history.get();
            existingHistory.setHistoryType(historyDetails.getHistoryType());
            existingHistory.setDescription(historyDetails.getDescription());
            existingHistory.setHistoryDate(historyDetails.getHistoryDate());
            existingHistory.setNotes(historyDetails.getNotes());
            return historyRepository.save(existingHistory);
        }
        return null;
    }

    public boolean deleteHistory(Long historyId) {
        if (historyRepository.existsById(historyId)) {
            historyRepository.deleteById(historyId);
            return true;
        }
        return false;
    }
}
