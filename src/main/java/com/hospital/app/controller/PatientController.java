package com.hospital.app.controller;

import com.hospital.app.model.Patient;
import com.hospital.app.model.PatientAllergy;
import com.hospital.app.model.PatientChronicCondition;
import com.hospital.app.model.PatientMedicalHistory;
import com.hospital.app.service.PatientService;
import com.hospital.app.service.PatientAllergyService;
import com.hospital.app.service.PatientChronicConditionService;
import com.hospital.app.service.PatientMedicalHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
//
    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientAllergyService allergyService;

    @Autowired
    private PatientChronicConditionService conditionService;

    @Autowired
    private PatientMedicalHistoryService historyService;

    @PostMapping
    public ResponseEntity<Patient> addPatient(@jakarta.validation.Valid @RequestBody Patient patient, jakarta.servlet.http.HttpServletRequest request) {
        if (!canWriteHistory(request)) patient.setMedicalHistory(null);
        Patient savedPatient = patientService.addPatient(patient);
        return new ResponseEntity<>(savedPatient, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients(
            @RequestParam(name = "includeInactive", required = false, defaultValue = "false") boolean includeInactive) {
        List<Patient> patients = includeInactive
                ? patientService.getAllPatientsIncludingInactive()
                : patientService.getAllPatients();
        return new ResponseEntity<>(patients, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        Optional<Patient> patient = patientService.getPatientById(id);
        return patient.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id, @jakarta.validation.Valid @RequestBody Patient patientDetails, jakarta.servlet.http.HttpServletRequest request) {
        Optional<Patient> patient = patientService.getPatientById(id);
        if (patient.isPresent()) {
            Patient existingPatient = patient.get();
            existingPatient.setName(patientDetails.getName());
            existingPatient.setEmail(patientDetails.getEmail());
            existingPatient.setPhone(patientDetails.getPhone());
            existingPatient.setDateOfBirth(patientDetails.getDateOfBirth());
            existingPatient.setAddress(patientDetails.getAddress());
            if (canWriteHistory(request)) existingPatient.setMedicalHistory(patientDetails.getMedicalHistory());
            Patient updatedPatient = patientService.updatePatient(existingPatient);
            return new ResponseEntity<>(updatedPatient, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        Optional<Patient> patient = patientService.getPatientById(id);
        if (patient.isPresent()) {
            patientService.deletePatient(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/search/email/{email}")
    public ResponseEntity<Patient> getPatientByEmail(@PathVariable String email) {
        Optional<Patient> patient = patientService.findPatientByEmail(email);
        return patient.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/search/name/{name}")
    public ResponseEntity<List<Patient>> getPatientsByName(@PathVariable String name) {
        List<Patient> patients = patientService.findPatientsByName(name);
        return new ResponseEntity<>(patients, HttpStatus.OK);
    }

    // ===== Allergy Endpoints =====
    @GetMapping("/{patientId}/allergies")
    public ResponseEntity<List<PatientAllergy>> getPatientAllergies(@PathVariable Long patientId) {
        List<PatientAllergy> allergies = allergyService.getAllergiesByPatientId(patientId);
        return new ResponseEntity<>(allergies, HttpStatus.OK);
    }

    @PostMapping("/{patientId}/allergies")
    public ResponseEntity<PatientAllergy> addAllergy(@PathVariable Long patientId, @jakarta.validation.Valid @RequestBody PatientAllergy allergy) {
        allergy.setPatientId(patientId);
        PatientAllergy savedAllergy = allergyService.createAllergy(allergy);
        return new ResponseEntity<>(savedAllergy, HttpStatus.CREATED);
    }

    @PutMapping("/{patientId}/allergies/{allergyId}")
    public ResponseEntity<PatientAllergy> updateAllergy(@PathVariable Long patientId, @PathVariable Long allergyId,
                                                        @jakarta.validation.Valid @RequestBody PatientAllergy allergyDetails) {
        if (allergyService.getAllergyById(allergyId).filter(record -> patientId.equals(record.getPatientId())).isEmpty()) return ResponseEntity.notFound().build();
        PatientAllergy updatedAllergy = allergyService.updateAllergy(allergyId, allergyDetails);
        return updatedAllergy != null ? new ResponseEntity<>(updatedAllergy, HttpStatus.OK) :
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{patientId}/allergies/{allergyId}")
    public ResponseEntity<Void> deleteAllergy(@PathVariable Long patientId, @PathVariable Long allergyId) {
        if (allergyService.getAllergyById(allergyId).filter(record -> patientId.equals(record.getPatientId())).isEmpty()) return ResponseEntity.notFound().build();
        boolean deleted = allergyService.deleteAllergy(allergyId);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // ===== Chronic Condition Endpoints =====
    @GetMapping("/{patientId}/chronic-conditions")
    public ResponseEntity<List<PatientChronicCondition>> getPatientConditions(@PathVariable Long patientId) {
        List<PatientChronicCondition> conditions = conditionService.getConditionsByPatientId(patientId);
        return new ResponseEntity<>(conditions, HttpStatus.OK);
    }

    @PostMapping("/{patientId}/chronic-conditions")
    public ResponseEntity<PatientChronicCondition> addCondition(@PathVariable Long patientId,
                                                               @jakarta.validation.Valid @RequestBody PatientChronicCondition condition) {
        condition.setPatientId(patientId);
        PatientChronicCondition savedCondition = conditionService.createCondition(condition);
        return new ResponseEntity<>(savedCondition, HttpStatus.CREATED);
    }

    @PutMapping("/{patientId}/chronic-conditions/{conditionId}")
    public ResponseEntity<PatientChronicCondition> updateCondition(@PathVariable Long patientId,
                                                                   @PathVariable Long conditionId,
                                                                   @jakarta.validation.Valid @RequestBody PatientChronicCondition conditionDetails) {
        if (conditionService.getConditionById(conditionId).filter(record -> patientId.equals(record.getPatientId())).isEmpty()) return ResponseEntity.notFound().build();
        PatientChronicCondition updatedCondition = conditionService.updateCondition(conditionId, conditionDetails);
        return updatedCondition != null ? new ResponseEntity<>(updatedCondition, HttpStatus.OK) :
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{patientId}/chronic-conditions/{conditionId}")
    public ResponseEntity<Void> deleteCondition(@PathVariable Long patientId, @PathVariable Long conditionId) {
        if (conditionService.getConditionById(conditionId).filter(record -> patientId.equals(record.getPatientId())).isEmpty()) return ResponseEntity.notFound().build();
        boolean deleted = conditionService.deleteCondition(conditionId);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // ===== Medical History Endpoints =====
    @GetMapping("/{patientId}/medical-history")
    public ResponseEntity<List<PatientMedicalHistory>> getPatientHistory(@PathVariable Long patientId) {
        List<PatientMedicalHistory> histories = historyService.getHistoryByPatientId(patientId);
        return new ResponseEntity<>(histories, HttpStatus.OK);
    }

    @PostMapping("/{patientId}/medical-history")
    public ResponseEntity<PatientMedicalHistory> addHistory(@PathVariable Long patientId,
                                                           @jakarta.validation.Valid @RequestBody PatientMedicalHistory history) {
        history.setPatientId(patientId);
        PatientMedicalHistory savedHistory = historyService.createHistory(history);
        return new ResponseEntity<>(savedHistory, HttpStatus.CREATED);
    }

    @PutMapping("/{patientId}/medical-history/{historyId}")
    public ResponseEntity<PatientMedicalHistory> updateHistory(@PathVariable Long patientId,
                                                               @PathVariable Long historyId,
                                                               @jakarta.validation.Valid @RequestBody PatientMedicalHistory historyDetails) {
        if (historyService.getHistoryById(historyId).filter(record -> patientId.equals(record.getPatientId())).isEmpty()) return ResponseEntity.notFound().build();
        PatientMedicalHistory updatedHistory = historyService.updateHistory(historyId, historyDetails);
        return updatedHistory != null ? new ResponseEntity<>(updatedHistory, HttpStatus.OK) :
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{patientId}/medical-history/{historyId}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long patientId, @PathVariable Long historyId) {
        if (historyService.getHistoryById(historyId).filter(record -> patientId.equals(record.getPatientId())).isEmpty()) return ResponseEntity.notFound().build();
        boolean deleted = historyService.deleteHistory(historyId);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    private boolean canWriteHistory(jakarta.servlet.http.HttpServletRequest request) {
        var login = (com.hospital.app.dto.LoginResponse) request.getSession().getAttribute(LoginController.AUTHENTICATED_USER);
        return java.util.Set.of("Admin", "Doctor", "Surgeon", "Nurse").contains(login.getRole());
    }
}
