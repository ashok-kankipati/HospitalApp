package com.hospital.app.service;

import com.hospital.app.model.Appointment;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.hospital.app.repository.StaffRepository staffRepository;

    private void requireDoctor(Long staffId) {
        var doctor = staffId == null ? null : staffRepository.findById(staffId).orElse(null);
        if (doctor == null || !Boolean.TRUE.equals(doctor.getIsActive()) || (doctor.getPosition() == null || !"Doctor".equalsIgnoreCase(doctor.getPosition().trim())))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose an active doctor for the appointment.");
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment addAppointment(Appointment appointment) {
        requireDoctor(appointment.getStaffId());
        Appointment saved = appointmentRepository.save(appointment);
        String subject = "Appointment Reminder";
        String body = "New appointment scheduled: A" + String.format("%03d", saved.getId())
                + " on " + saved.getAppointmentDate() + " " + saved.getAppointmentTime();
        notificationService.notifyByRole("Doctor", "APPOINTMENT_REMINDER", subject, body);
        notificationService.notifyByRole("Receptionist", "APPOINTMENT_REMINDER", subject, body);
        return saved;
    }

    public Appointment updateAppointment(Appointment appointment) {
        requireDoctor(appointment.getStaffId());
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.findById(id).ifPresent(appointment -> {
            appointment.setStatus("CANCELLED");
            appointmentRepository.save(appointment);
        });
    }

    public List<Appointment> getAppointmentsByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByStaffId(Long staffId) {
        return appointmentRepository.findByStaffId(staffId);
    }

    public List<Appointment> getAppointmentsByStatus(String status) {
        return appointmentRepository.findByStatus(status);
    }

    public List<Appointment> getPatientAppointmentsByStatus(Long patientId, String status) {
        return appointmentRepository.findByPatientIdAndStatus(patientId, status);
    }

    public List<Appointment> getStaffAppointmentsByStatus(Long staffId, String status) {
        return appointmentRepository.findByStaffIdAndStatus(staffId, status);
    }
}
