package com.hospital.app.service;

import com.hospital.app.model.PatientAllergy;
import com.hospital.app.repository.PatientAllergyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PatientAllergyService {

    @Autowired
    private PatientAllergyRepository allergyRepository;

    public List<PatientAllergy> getAllergiesByPatientId(Long patientId) {
        return allergyRepository.findByPatientId(patientId);
    }

    public Optional<PatientAllergy> getAllergyById(Long allergyId) {
        return allergyRepository.findById(allergyId);
    }

    public PatientAllergy createAllergy(PatientAllergy allergy) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        allergy.setRecordedAt(LocalDateTime.now().format(formatter));
        return allergyRepository.save(allergy);
    }

    public PatientAllergy updateAllergy(Long allergyId, PatientAllergy allergyDetails) {
        Optional<PatientAllergy> allergy = allergyRepository.findById(allergyId);
        if (allergy.isPresent()) {
            PatientAllergy existingAllergy = allergy.get();
            existingAllergy.setAllergyType(allergyDetails.getAllergyType());
            existingAllergy.setAllergyName(allergyDetails.getAllergyName());
            existingAllergy.setReaction(allergyDetails.getReaction());
            existingAllergy.setSeverity(allergyDetails.getSeverity());
            existingAllergy.setNotes(allergyDetails.getNotes());
            return allergyRepository.save(existingAllergy);
        }
        return null;
    }

    public boolean deleteAllergy(Long allergyId) {
        if (allergyRepository.existsById(allergyId)) {
            allergyRepository.deleteById(allergyId);
            return true;
        }
        return false;
    }
}
