package com.hospital.app.repository.billing;

import com.hospital.app.model.billing.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceId(Long invoiceId);
    boolean existsByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    List<InvoiceItem> findByInvoiceIdAndReferenceTypeAndReferenceId(Long invoiceId, String referenceType, Long referenceId);
}
