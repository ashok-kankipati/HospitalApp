package com.hospital.app.service;

import com.hospital.app.model.Patient;
import com.hospital.app.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public Patient addPatient(Patient patient) {
        if (patient.getIsActive() == null) {
            patient.setIsActive(true);
        }
        return patientRepository.save(patient);
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findByIsActiveTrue();
    }

    public List<Patient> getAllPatientsIncludingInactive() {
        return patientRepository.findAll();
    }

    public Patient updatePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        patientRepository.findById(id).ifPresent(patient -> {
            patient.setIsActive(false);
            patientRepository.save(patient);
        });
    }

    public Optional<Patient> findPatientByEmail(String email) {
        return patientRepository.findByEmail(email);
    }

    public List<Patient> findPatientsByName(String name) {
        return patientRepository.findByName(name);
    }
}
