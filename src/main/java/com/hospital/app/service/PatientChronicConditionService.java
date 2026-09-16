package com.hospital.app.service;

import com.hospital.app.model.PatientChronicCondition;
import com.hospital.app.repository.PatientChronicConditionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PatientChronicConditionService {

    @Autowired
    private PatientChronicConditionRepository conditionRepository;

    public List<PatientChronicCondition> getConditionsByPatientId(Long patientId) {
        return conditionRepository.findByPatientId(patientId);
    }

    public Optional<PatientChronicCondition> getConditionById(Long conditionId) {
        return conditionRepository.findById(conditionId);
    }

    public PatientChronicCondition createCondition(PatientChronicCondition condition) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        condition.setRecordedAt(LocalDateTime.now().format(formatter));
        return conditionRepository.save(condition);
    }

    public PatientChronicCondition updateCondition(Long conditionId, PatientChronicCondition conditionDetails) {
        Optional<PatientChronicCondition> condition = conditionRepository.findById(conditionId);
        if (condition.isPresent()) {
            PatientChronicCondition existingCondition = condition.get();
            existingCondition.setConditionName(conditionDetails.getConditionName());
            existingCondition.setDiagnosedDate(conditionDetails.getDiagnosedDate());
            existingCondition.setStatus(conditionDetails.getStatus());
            existingCondition.setNotes(conditionDetails.getNotes());
            return conditionRepository.save(existingCondition);
        }
        return null;
    }

    public boolean deleteCondition(Long conditionId) {
        if (conditionRepository.existsById(conditionId)) {
            conditionRepository.deleteById(conditionId);
            return true;
        }
        return false;
    }
}
