package com.hospital.app.repository.billing;

import com.hospital.app.model.billing.InvoiceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, Long> {
    List<InvoiceDocument> findByInvoiceId(Long invoiceId);
}
