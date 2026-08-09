package ais.action.master.generic.v2;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import com.lowagie.text.Document;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** PDF/DOCX/PPTX sinkron mengikuti filter, scope, sort, masking, dan row limit yang sama. */
@SuppressWarnings({ "rawtypes" })
public class GenericCrudDocumentExportService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudExportService export = new GenericCrudExportService();

    public void write(GenericCrudRequestContext context, String format, String search, List filters,
            GenericCrudSort sort, HttpServletResponse response) throws Exception {
        privilege.require(context, GenericCrudOperation.EXPORT);
        String normalized = format == null ? "" : format.toUpperCase();
        requireEnabled(context.getDefinition(), normalized);
        List fields = export.exportFields(context.getDefinition());
        List rows = export.loadRows(context, search, filters, sort, context.getDefinition().getSynchronousExportLimit());
        String summary = summary(search, filters, sort, rows.size());
        if ("PDF".equals(normalized)) writePdf(context, fields, rows, summary, response);
        else if ("DOCX".equals(normalized)) writeDocx(context, fields, rows, summary, response);
        else if ("PPTX".equals(normalized)) writePptx(context, fields, rows, summary, response);
        else throw new GenericCrudException(400, "EXPORT_FORMAT_INVALID", "Format export tidak diizinkan.");
    }

    private void writePdf(GenericCrudRequestContext context, List fields, List rows, String summary,
            HttpServletResponse response) throws Exception {
        response.reset(); response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + context.getDefinition().getPageKey() + ".pdf");
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 30, 30);
        document.setFooter(new HeaderFooter(new Phrase("Halaman "), true));
        PdfWriter.getInstance(document, response.getOutputStream()); document.open();
        document.add(new Paragraph(context.getDefinition().getDisplayName()));
        document.add(new Paragraph(summary));
        PdfPTable table = new PdfPTable(fields.size()); table.setWidthPercentage(100);
        for (int i = 0; i < fields.size(); i++) table.addCell(new Phrase(((GenericCrudFieldDefinition) fields.get(i)).getLabel()));
        for (int r = 0; r < rows.size(); r++) { Map row = (Map) rows.get(r); for (int c = 0; c < fields.size(); c++) table.addCell(text(row.get(((GenericCrudFieldDefinition) fields.get(c)).getProperty()))); }
        document.add(table); document.close(); response.getOutputStream().flush();
    }

    private void writeDocx(GenericCrudRequestContext context, List fields, List rows, String summaryText,
            HttpServletResponse response) throws Exception {
        XWPFDocument document = new XWPFDocument();
        try {
            XWPFParagraph title = document.createParagraph(); title.createRun().setText(context.getDefinition().getDisplayName());
            XWPFParagraph summary = document.createParagraph(); summary.createRun().setText(summaryText);
            XWPFTable table = document.createTable(rows.size() + 1, fields.size());
            XWPFTableRow header = table.getRow(0);
            for (int c = 0; c < fields.size(); c++) setCell(header.getCell(c), ((GenericCrudFieldDefinition) fields.get(c)).getLabel());
            for (int r = 0; r < rows.size(); r++) { Map values = (Map) rows.get(r); XWPFTableRow row = table.getRow(r + 1); for (int c = 0; c < fields.size(); c++) setCell(row.getCell(c), text(values.get(((GenericCrudFieldDefinition) fields.get(c)).getProperty()))); }
            response.reset(); response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=" + context.getDefinition().getPageKey() + ".docx");
            document.write(response.getOutputStream()); response.getOutputStream().flush();
        } finally { try { document.close(); } catch (Exception ignored) { } }
    }

    private void writePptx(GenericCrudRequestContext context, List fields, List rows, String summary,
            HttpServletResponse response) throws Exception {
        XMLSlideShow show = new XMLSlideShow();
        try {
            int perSlide = 12, slideCount = Math.max(1, (rows.size() + perSlide - 1) / perSlide);
            for (int s = 0; s < slideCount; s++) {
                XSLFSlide slide = show.createSlide();
                XSLFTextBox title = slide.createTextBox(); title.setAnchor(new Rectangle(30, 20, 650, 45));
                title.setText(context.getDefinition().getDisplayName() + " (" + (s + 1) + "/" + slideCount + ")");
                XSLFTextBox body = slide.createTextBox(); body.setAnchor(new Rectangle(30, 75, 650, 430));
                StringBuilder content = new StringBuilder(summary).append('\n'); int end = Math.min(rows.size(), (s + 1) * perSlide);
                for (int r = s * perSlide; r < end; r++) { Map values = (Map) rows.get(r); if (content.length() > 0) content.append('\n'); for (int c = 0; c < fields.size(); c++) { if (c > 0) content.append(" | "); GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(c); content.append(field.getLabel()).append(": ").append(text(values.get(field.getProperty()))); } }
                if (content.length() == 0) content.append("Tidak ada data untuk filter aktif."); body.setText(content.toString());
            }
            response.reset(); response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            response.setHeader("Content-Disposition", "attachment; filename=" + context.getDefinition().getPageKey() + ".pptx");
            show.write(response.getOutputStream()); response.getOutputStream().flush();
        } finally { try { show.close(); } catch (Exception ignored) { } }
    }

    private void requireEnabled(GenericCrudDefinition d, String format) throws GenericCrudException {
        boolean enabled = "PDF".equals(format) ? d.isExportPdfEnabled() : "DOCX".equals(format) ? d.isExportDocxEnabled() : "PPTX".equals(format) && d.isExportPptxEnabled();
        if (!enabled) throw new GenericCrudException(403, "DOCUMENT_EXPORT_DISABLED", "Export " + format + " belum diaktifkan untuk entity ini.");
    }
    private String summary(String search, List filters, GenericCrudSort sort, int total) {
        StringBuilder value = new StringBuilder("Pencarian: ").append(search == null || search.length() == 0 ? "Semua" : search);
        value.append(" | Filter: ").append(filters == null ? 0 : filters.size());
        value.append(" | Sort: ").append(sort == null ? "default" : sort.getProperty() + " " + (sort.isAscending() ? "ASC" : "DESC"));
        return value.append(" | Total: ").append(total).toString();
    }
    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private void setCell(XWPFTableCell cell, String value) { cell.setText(value == null ? "" : value); }
}
