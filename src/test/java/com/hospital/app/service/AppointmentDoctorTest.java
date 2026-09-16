package com.hospital.app.service;

import com.hospital.app.model.Appointment;
import com.hospital.app.model.Staff;
import com.hospital.app.repository.AppointmentRepository;
import com.hospital.app.repository.StaffRepository;
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
}
