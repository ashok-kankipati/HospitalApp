package com.hospital.app.service.lab;

import com.hospital.app.model.Appointment;
import com.hospital.app.model.Staff;
import com.hospital.app.model.Visit;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.PatientRepository;
import com.hospital.app.repository.StaffRepository;
import com.hospital.app.repository.VisitRepository;
import com.hospital.app.repository.lab.LabReportRepository;
import com.hospital.app.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LabNotificationTest {
    @Test
    void reportReadyNotifiesAssignedDoctorAndLabTeam() {
        var service = new LabService();
        var reports = mock(LabReportRepository.class);
        var visits = mock(VisitRepository.class);
        var appointments = mock(AppointmentRepository.class);
        var patients = mock(PatientRepository.class);
        var staff = mock(StaffRepository.class);
        var notifications = mock(NotificationService.class);
        ReflectionTestUtils.setField(service, "labReportRepository", reports);
        ReflectionTestUtils.setField(service, "visitRepository", visits);
        ReflectionTestUtils.setField(service, "appointmentRepository", appointments);
        ReflectionTestUtils.setField(service, "patientRepository", patients);
        ReflectionTestUtils.setField(service, "staffRepository", staff);
        ReflectionTestUtils.setField(service, "notificationService", notifications);

        var report = new LabReport();
        report.setVisitId(5L);
        report.setFileName("CBC report");
        when(reports.save(report)).thenReturn(report);
        var visit = new Visit();
        visit.setAppointmentId(7L);
        visit.setPatientId(3L);
        when(visits.findById(5L)).thenReturn(Optional.of(visit));
        var appointment = new Appointment();
        appointment.setId(7L);
        appointment.setStaffId(9L);
        when(appointments.findById(7L)).thenReturn(Optional.of(appointment));
        var doctor = new Staff();
        doctor.setName("Dr Ashok");
        doctor.setEmail("assigned.doctor@hospital.com");
        when(staff.findById(9L)).thenReturn(Optional.of(doctor));
        var patient = new com.hospital.app.model.Patient();
        patient.setId(3L);
        patient.setName("Sivakumar");
        when(patients.findById(3L)).thenReturn(Optional.of(patient));

        service.addReport(report, "labtech1 (Lab Technician)");

        verify(notifications).notifyRecipientByRole(eq("Doctor"), eq("LAB_REPORT_READY"),
            eq("assigned.doctor@hospital.com"), anyString(), argThat(body -> body.contains("Sivakumar (P003)")
                && body.contains("V005") && body.contains("A007")
                && body.contains("CBC report") && body.contains("labtech1 (Lab Technician)")
                && body.contains("Dr Ashok")));
        verify(notifications, never()).notifyByRole(eq("Doctor"), anyString(), anyString(), anyString());
        verify(notifications).notifyByRole(eq("Lab"), eq("LAB_REPORT_READY"), anyString(), anyString());
    }
}