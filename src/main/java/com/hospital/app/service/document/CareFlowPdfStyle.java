package com.hospital.app.service.document;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

public final class CareFlowPdfStyle {
    public static final Color INK = new Color(22, 49, 43);
    public static final Color MUTED = new Color(102, 125, 119);
    public static final Color BRAND = new Color(13, 92, 77);
    public static final Color ACCENT = new Color(25, 164, 123);
    public static final Color SURFACE = new Color(238, 247, 244);
    public static final Color BORDER = new Color(216, 229, 225);
    public static final Color WHITE = Color.WHITE;

    public static final Font BRAND_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, WHITE);
    public static final Font DOCUMENT_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(202, 235, 226));
    public static final Font TITLE_FONT = new Font(Font.HELVETICA, 17, Font.BOLD, INK);
    public static final Font SECTION_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND);
    public static final Font LABEL_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, MUTED);
    public static final Font BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, INK);
    public static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, WHITE);
    public static final Font TOTAL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, BRAND);

    private CareFlowPdfStyle() {}

    public static void apply(PdfWriter writer, String documentType) {
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter activeWriter, Document document) {
                addWatermark(activeWriter);
                addFooter(activeWriter, documentType);
            }
        });
    }

    public static PdfPTable brandHeader(String documentType, String identifier, String status) {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2.4f, 1f});
        header.setSpacingAfter(22);

        PdfPCell brand = new PdfPCell();
        brand.setBackgroundColor(BRAND);
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setPadding(18);
        brand.addElement(new Paragraph("CareFlow", BRAND_FONT));
        brand.addElement(new Paragraph(documentType.toUpperCase(), DOCUMENT_FONT));
        header.addCell(brand);

        PdfPCell meta = new PdfPCell();
        meta.setBackgroundColor(INK);
        meta.setBorder(Rectangle.NO_BORDER);
        meta.setPadding(18);
        Paragraph id = new Paragraph(identifier == null ? "" : identifier,
                new Font(Font.HELVETICA, 11, Font.BOLD, WHITE));
        id.setAlignment(Element.ALIGN_RIGHT);
        meta.addElement(id);
        Paragraph state = new Paragraph(status == null ? "" : status.toUpperCase(), DOCUMENT_FONT);
        state.setAlignment(Element.ALIGN_RIGHT);
        meta.addElement(state);
        header.addCell(meta);
        return header;
    }

    public static Paragraph title(String text) {
        Paragraph paragraph = new Paragraph(text, TITLE_FONT);
        paragraph.setSpacingAfter(14);
        return paragraph;
    }

    public static Paragraph section(String text) {
        Paragraph paragraph = new Paragraph(text.toUpperCase(), SECTION_FONT);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    public static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(BRAND);
        cell.setBorderColor(BRAND);
        cell.setPadding(8);
        return cell;
    }

    public static PdfPCell dataCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, BODY_FONT));
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public static PdfPCell labelValueCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(SURFACE);
        cell.setBorderColor(BORDER);
        cell.setPadding(10);
        cell.addElement(new Paragraph(label.toUpperCase(), LABEL_FONT));
        cell.addElement(new Paragraph(value == null || value.isBlank() ? "—" : value, BODY_FONT));
        return cell;
    }

    private static void addWatermark(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContentUnder();
        PdfGState opacity = new PdfGState();
        opacity.setFillOpacity(0.055f);
        canvas.saveState();
        canvas.setGState(opacity);
        canvas.setColorFill(BRAND);
        canvas.beginText();
        try {
            canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false), 54);
            Rectangle page = writer.getPageSize();
            canvas.showTextAligned(Element.ALIGN_CENTER, "CAREFLOW", page.getWidth() / 2,
                    page.getHeight() / 2, 38);
        } catch (Exception ignored) {
            // Standard PDF fonts are expected to be available; the document remains valid without a watermark.
        }
        canvas.endText();
        canvas.restoreState();
    }

    private static void addFooter(PdfWriter writer, String documentType) {
        PdfContentByte canvas = writer.getDirectContent();
        try {
            BaseFont font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            canvas.saveState();
            canvas.setColorStroke(BORDER);
            canvas.moveTo(42, 38);
            canvas.lineTo(writer.getPageSize().getWidth() - 42, 38);
            canvas.stroke();
            canvas.beginText();
            canvas.setColorFill(MUTED);
            canvas.setFontAndSize(font, 7);
            canvas.showTextAligned(Element.ALIGN_LEFT, "CareFlow · " + documentType, 42, 25, 0);
            canvas.showTextAligned(Element.ALIGN_RIGHT, "Page " + writer.getPageNumber(),
                    writer.getPageSize().getWidth() - 42, 25, 0);
            canvas.endText();
            canvas.restoreState();
        } catch (Exception ignored) {
            // Footer decoration must not prevent a clinical document from being generated.
        }
    }
}