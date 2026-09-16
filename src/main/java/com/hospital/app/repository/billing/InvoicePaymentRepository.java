package com.hospital.app.repository.billing;

import com.hospital.app.model.billing.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {
    List<InvoicePayment> findByInvoiceId(Long invoiceId);
}
