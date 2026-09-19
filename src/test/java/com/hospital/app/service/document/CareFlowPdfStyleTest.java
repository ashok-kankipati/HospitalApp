package com.hospital.app.service.document;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CareFlowPdfStyleTest {
    @Test
    void embedsBrandWatermarkAndFooterInGeneratedDocument() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 42, 42, 46, 54);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        CareFlowPdfStyle.apply(writer, "Invoice");
        document.open();
        document.add(CareFlowPdfStyle.brandHeader("Patient invoice", "INV-2026-001", "Paid"));
        document.add(CareFlowPdfStyle.title("Invoice summary"));
        document.close();

        PdfReader reader = new PdfReader(output.toByteArray());
        String text = new PdfTextExtractor(reader).getTextFromPage(1);

        assertTrue(text.contains("CareFlow"));
        assertTrue(text.contains("CAREFLOW"));
        assertTrue(text.contains("PATIENT INVOICE"));
        assertTrue(text.contains("Page 1"));
        reader.close();
    }
}