package com.hospital.app.service.lab;

import com.hospital.app.model.Appointment;
import com.hospital.app.model.Staff;
import com.hospital.app.model.Visit;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.repository.AppointmentRepository;
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
        var staff = mock(StaffRepository.class);
        var notifications = mock(NotificationService.class);
        ReflectionTestUtils.setField(service, "labReportRepository", reports);
        ReflectionTestUtils.setField(service, "visitRepository", visits);
        ReflectionTestUtils.setField(service, "appointmentRepository", appointments);
        ReflectionTestUtils.setField(service, "staffRepository", staff);
        ReflectionTestUtils.setField(service, "notificationService", notifications);

        var report = new LabReport();
        report.setVisitId(5L);
        when(reports.save(report)).thenReturn(report);
        var visit = new Visit();
        visit.setAppointmentId(7L);
        when(visits.findById(5L)).thenReturn(Optional.of(visit));
        var appointment = new Appointment();
        appointment.setStaffId(9L);
        when(appointments.findById(7L)).thenReturn(Optional.of(appointment));
        var doctor = new Staff();
        doctor.setEmail("assigned.doctor@hospital.com");
        when(staff.findById(9L)).thenReturn(Optional.of(doctor));

        service.addReport(report);

        verify(notifications).notifyRecipientByRole(eq("Doctor"), eq("LAB_REPORT_READY"),
                eq("assigned.doctor@hospital.com"), anyString(), contains("V005"));
        verify(notifications, never()).notifyByRole(eq("Doctor"), anyString(), anyString(), anyString());
        verify(notifications).notifyByRole(eq("Lab"), eq("LAB_REPORT_READY"), anyString(), anyString());
    }
}