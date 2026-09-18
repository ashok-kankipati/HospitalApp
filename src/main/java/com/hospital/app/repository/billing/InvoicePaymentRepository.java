package com.hospital.app.repository.billing;

import com.hospital.app.model.billing.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {
    List<InvoicePayment> findByInvoiceId(Long invoiceId);
    Optional<InvoicePayment> findByGatewayPaymentId(String gatewayPaymentId);
    Optional<InvoicePayment> findByGatewayOrderId(String gatewayOrderId);
    boolean existsByGatewayPaymentId(String gatewayPaymentId);
}
