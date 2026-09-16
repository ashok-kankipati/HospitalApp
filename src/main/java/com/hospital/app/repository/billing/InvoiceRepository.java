package com.hospital.app.repository.billing;

import com.hospital.app.model.billing.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatientId(Long patientId);
    List<Invoice> findByAppointmentId(Long appointmentId);
}
