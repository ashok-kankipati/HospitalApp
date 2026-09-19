package com.hospital.app.service;

import com.hospital.app.model.Appointment;
import com.hospital.app.model.Staff;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.StaffRepository;
import com.hospital.app.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentDoctorTest {
    @Test void rejectsNonDoctorsAndInactiveDoctorsBeforeSaving() {
        var service = new AppointmentService();
        var staff = mock(StaffRepository.class); var appointments = mock(AppointmentRepository.class);
        ReflectionTestUtils.setField(service, "staffRepository", staff);
        ReflectionTestUtils.setField(service, "appointmentRepository", appointments);
        var appointment = new Appointment(); appointment.setStaffId(1L);
        for (String position : new String[]{"Pharmacist", "Lab Technician", "Nurse", "Receptionist"}) {
            var member = new Staff(); member.setPosition(position); member.setIsActive(true);
            when(staff.findById(1L)).thenReturn(Optional.of(member));
            assertThrows(ResponseStatusException.class, () -> service.addAppointment(appointment));
            assertThrows(ResponseStatusException.class, () -> service.updateAppointment(appointment));
        }
        var doctor = new Staff(); doctor.setPosition("Doctor"); doctor.setIsActive(false);
        when(staff.findById(1L)).thenReturn(Optional.of(doctor));
        assertThrows(ResponseStatusException.class, () -> service.updateAppointment(appointment));
        verifyNoInteractions(appointments);
        doctor.setIsActive(true);
        when(appointments.save(appointment)).thenReturn(appointment);
        assertSame(appointment, service.updateAppointment(appointment));
    }

    @Test void notifiesOnlyTheDoctorAssignedToTheAppointment() {
        var service = new AppointmentService();
        var staff = mock(StaffRepository.class);
        var appointments = mock(AppointmentRepository.class);
        var notifications = mock(NotificationService.class);
        ReflectionTestUtils.setField(service, "staffRepository", staff);
        ReflectionTestUtils.setField(service, "appointmentRepository", appointments);
        ReflectionTestUtils.setField(service, "notificationService", notifications);

        var doctor = new Staff();
        doctor.setPosition("Doctor");
        doctor.setIsActive(true);
        doctor.setEmail("assigned.doctor@hospital.com");
        when(staff.findById(7L)).thenReturn(Optional.of(doctor));

        var appointment = new Appointment();
        appointment.setStaffId(7L);
        appointment.setAppointmentDate("2026-09-20");
        appointment.setAppointmentTime("10:00 AM");
        when(appointments.save(appointment)).thenAnswer(call -> {
            appointment.setId(42L);
            return appointment;
        });

        service.addAppointment(appointment);

        verify(notifications).notifyRecipientByRole(eq("Doctor"), eq("APPOINTMENT_REMINDER"),
                eq("assigned.doctor@hospital.com"), anyString(), contains("A042"));
        verify(notifications, never()).notifyByRole(eq("Doctor"), anyString(), anyString(), anyString());
        verify(notifications).notifyByRole(eq("Receptionist"), eq("APPOINTMENT_REMINDER"), anyString(), anyString());
    }
}
