package ais.action.master.spmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.DataValidation;
import org.zkoss.poi.ss.usermodel.DataValidationConstraint;
import org.zkoss.poi.ss.usermodel.DataValidationHelper;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.IndexedColors;
import org.zkoss.poi.ss.usermodel.Row;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.ss.util.CellRangeAddressList;
import org.zkoss.poi.ss.util.WorkbookUtil;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.HasilSPMI;
import ais.database.model.spmi.HasilTemuanSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;

/**
 * Round-trip satu file Excel untuk lembar kerja AMI.
 *
 * Workbook sengaja memuat ID audit dan ID skenario pada area tersembunyi. ID itu
 * tidak dipakai sebagai sumber nilai, tetapi sebagai pengaman agar hasil audit
 * tidak tertukar ketika pengguna mengunggah file dari prodi/periode lain.
 */
public final class AmiExcelHelper {

    public static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String FORMAT_VERSION = "AIS-AMI-2026-V1";
    private static final String SHEET_COVER = "COVER";
    private static final String SHEET_SUMMARY = "Ringkasan Hasil Audit";
    private static final String SHEET_READINESS = "Rekap Kesiapan Bukti";
    private static final String SHEET_NONCOMPLIANCE = "Laporan Tidak Memenuhi";
    private static final String SHEET_DATA = "_DataIndikator";
    private static final String SHEET_META = "_AMI_META";
    private static final int HEADER_ROW = 3;
    private static final int FIRST_DATA_ROW = HEADER_ROW + 1;
    private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

    private AmiExcelHelper() {
    }

    public static byte[] exportWorkbook(HasilSPMI hasil) throws Exception {
        validateAudit(hasil);
        Session session = HibernateUtil.currentSession();
        List<AuditRow> rows = loadRows(session, hasil);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Jenis SPMI belum memiliki indikator/daftar tilik aktif.");
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Styles styles = new Styles(workbook);
            writeCover(workbook, styles, hasil);

            Map<Long, SheetBlock> blocks = groupByStandard(rows);
            Set<String> usedNames = new HashSet<String>();
            usedNames.add(SHEET_COVER.toLowerCase());
            int sequence = 1;
            for (SheetBlock block : blocks.values()) {
                block.sheetName = uniqueSheetName(sequence + ". " + block.standard.getNama(), usedNames);
                writeIndicatorSheet(workbook, styles, block);
                sequence++;
            }

            writeSummary(workbook, styles, blocks);
            writeReadiness(workbook, styles, blocks);
            writeNonCompliance(workbook, styles, rows);
            writeDataSheet(workbook, styles, rows);
            writeMetaSheet(workbook, hasil, rows.size());
            workbook.setSheetHidden(workbook.getSheetIndex(SHEET_DATA), true);
            workbook.setSheetHidden(workbook.getSheetIndex(SHEET_META), true);
            workbook.setActiveSheet(0);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                workbook.write(output);
                output.flush();
                return output.toByteArray();
            } finally {
                output.close();
            }
        } finally {
            workbook.getPackage().close();
        }
    }

    public static ImportResult importWorkbook(HasilSPMI hasil, byte[] bytes) throws Exception {
        validateAudit(hasil);
        validateUploadBytes(bytes);

        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("File tidak dapat dibaca sebagai Excel XLSX yang valid.", e);
        }

        try {
            validateMetadata(workbook, hasil);
            Session session = null;
            Transaction transaction = null;
            try {
                session = HibernateUtil.openSession();
                transaction = session.beginTransaction();
                HasilSPMI managedAudit = (HasilSPMI) session.get(HasilSPMI.class, hasil.getId());
                if (managedAudit == null) throw new IllegalArgumentException("Pengajuan AMI tidak ditemukan.");
                if (managedAudit.getJenisSPMI() == null
                        || !managedAudit.getJenisSPMI().getId().equals(hasil.getJenisSPMI().getId())) {
                    throw new IllegalArgumentException("Jenis SPMI pengajuan telah berubah. Unduh format terbaru.");
                }

                List<AuditRow> scope = loadRows(session, managedAudit);
                Map<Long, AuditRow> allowed = new HashMap<Long, AuditRow>();
                for (AuditRow row : scope) allowed.put(row.scenario.getId(), row);
                validateDeclaredIndicatorCount(workbook, allowed.size());

                List<ImportedRow> imported = readIndicatorSheets(workbook, allowed);
                if (imported.size() != allowed.size()) {
                    throw new IllegalArgumentException("Jumlah indikator dalam file tidak sesuai. Ditemukan "
                            + imported.size() + " dari " + allowed.size()
                            + " indikator. Unduh format terbaru dari pengajuan ini lalu ulangi upload.");
                }

                String auditor = readString(workbook.getSheet(SHEET_COVER), 12, 1);
                String auditee = readString(workbook.getSheet(SHEET_COVER), 11, 1);
                managedAudit.setAuditorNama(emptyToNull(auditor));
                managedAudit.setAuditeeNama(emptyToNull(auditee));

                int inserted = 0;
                int updated = 0;
                int skipped = 0;
                for (ImportedRow item : imported) {
                    AuditRow source = allowed.get(item.scenarioId);
                    HasilTemuanSPMI finding = source.finding;
                    boolean hasData = item.score != null || notEmpty(item.auditorNote)
                            || notEmpty(item.recommendation) || notEmpty(item.evidenceLink)
                            || notEmpty(item.readiness) || notEmpty(item.auditeeNote);
                    if (finding == null && !hasData) {
                        skipped++;
                        continue;
                    }
                    boolean isNew = finding == null;
                    if (isNew) finding = new HasilTemuanSPMI(source.scenario, managedAudit);

                    finding.setHasilSPMI(managedAudit);
                    finding.setSkenarioSPMI(source.scenario);
                    finding.setNama(nullToEmpty(item.auditorNote));
                    finding.setRekomendasi(emptyToNull(item.recommendation));
                    finding.setBuktiAuditee(emptyToNull(item.evidenceLink));
                    finding.setStatusKesiapanBukti(emptyToNull(item.readiness));
                    finding.setCatatanAuditee(emptyToNull(item.auditeeNote));
                    finding.setStatus(mapScoreToStatus(item.score, finding.getStatus()));
                    finding.setAktif(true);

                    if (isNew) {
                        session.save(finding);
                        inserted++;
                    } else {
                        updated++;
                    }
                }
                session.flush();
                transaction.commit();
                return new ImportResult(imported.size(), inserted, updated, skipped);
            } catch (Exception e) {
                if (transaction != null) {
                    try { transaction.rollback(); }
                    catch (Exception rollbackError) {
                        ais.common.ErrorAuditUtil.record(rollbackError,
                                "rollback upload format AMI Excel");
                    }
                }
                throw e;
            } finally {
                HibernateUtil.closeSessionQuietly(session);
            }
        } finally {
            workbook.getPackage().close();
        }
    }

    public static String fileName(HasilSPMI hasil) {
        String title = hasil == null ? "AMI" : safeFileName(hasil.getNama());
        String id = hasil == null || hasil.getId() == null ? "baru" : String.valueOf(hasil.getId());
        return "Format_AMI_" + id + "_" + title + ".xlsx";
    }

    private static void validateAudit(HasilSPMI hasil) {
        if (hasil == null || hasil.getId() == null) {
            throw new IllegalArgumentException("Pengajuan AMI harus disimpan terlebih dahulu.");
        }
        if (hasil.getJenisSPMI() == null || hasil.getJenisSPMI().getId() == null) {
            throw new IllegalArgumentException("Jenis SPMI pada pengajuan belum dipilih.");
        }
    }

    private static void validateUploadBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            throw new IllegalArgumentException("File upload kosong atau rusak.");
        }
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Ukuran file melebihi batas 10 MB.");
        }
        if (bytes[0] != 'P' || bytes[1] != 'K') {
            throw new IllegalArgumentException("File bukan Excel Open XML (.xlsx).");
        }
    }

    private static void validateMetadata(XSSFWorkbook workbook, HasilSPMI hasil) {
        XSSFSheet meta = workbook.getSheet(SHEET_META);
        if (meta == null) {
            throw new IllegalArgumentException("Metadata format AMI tidak ditemukan. Gunakan file hasil download dari sistem.");
        }
        String version = readString(meta, 0, 1);
        Long auditId = parseLong(readString(meta, 1, 1));
        Long typeId = parseLong(readString(meta, 2, 1));
        if (!FORMAT_VERSION.equals(version)) {
            throw new IllegalArgumentException("Versi format AMI tidak didukung: " + nullToEmpty(version));
        }
        if (auditId == null || !auditId.equals(hasil.getId())) {
            throw new IllegalArgumentException("File ini milik pengajuan AMI lain (ID "
                    + nullToEmpty(auditId == null ? null : auditId.toString()) + ").");
        }
        if (typeId == null || !typeId.equals(hasil.getJenisSPMI().getId())) {
            throw new IllegalArgumentException("Jenis SPMI dalam file tidak sama dengan pengajuan yang dipilih.");
        }
        String[] requiredSheets = new String[] { SHEET_COVER, SHEET_SUMMARY, SHEET_READINESS,
                SHEET_NONCOMPLIANCE, SHEET_DATA };
        for (String required : requiredSheets) {
            if (workbook.getSheet(required) == null) {
                throw new IllegalArgumentException("Sheet wajib '" + required
                        + "' tidak ditemukan. Unduh ulang format AMI dari sistem.");
            }
        }
    }

    private static void validateDeclaredIndicatorCount(XSSFWorkbook workbook, int expected) {
        XSSFSheet meta = workbook.getSheet(SHEET_META);
        Long declared = parseLong(readString(meta, 3, 1));
        if (declared == null || declared.longValue() != expected) {
            throw new IllegalArgumentException("Jumlah indikator pada metadata file tidak sesuai. "
                    + "File menyatakan " + nullToEmpty(declared) + ", sedangkan master aktif berisi "
                    + expected + ". Unduh format terbaru dari pengajuan ini.");
        }
    }

    private static List<ImportedRow> readIndicatorSheets(XSSFWorkbook workbook,
            Map<Long, AuditRow> allowed) {
        List<ImportedRow> result = new ArrayList<ImportedRow>();
        Set<Long> seen = new HashSet<Long>();
        List<String> errors = new ArrayList<String>();

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            XSSFSheet sheet = workbook.getSheetAt(s);
            if (isSpecialSheet(sheet.getSheetName())) continue;
            Row header = sheet.getRow(HEADER_ROW);
            if (header == null || !"No".equalsIgnoreCase(cellString(header.getCell(0)))) continue;

            for (int r = FIRST_DATA_ROW; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Long scenarioId = parseLong(cellString(row.getCell(9)));
                if (scenarioId == null) continue;
                String location = sheet.getSheetName() + " baris " + (r + 1);
                if (!allowed.containsKey(scenarioId)) {
                    errors.add(location + ": indikator tidak termasuk ruang lingkup pengajuan.");
                    continue;
                }
                if (!seen.add(scenarioId)) {
                    errors.add(location + ": indikator duplikat (ID " + scenarioId + ").");
                    continue;
                }
                try {
                    AuditRow source = allowed.get(scenarioId);
                    String indicatorText = cellString(row.getCell(1));
                    String evidenceText = cellString(row.getCell(2));
                    if (!sameText(indicatorText, source.indicator.getNama())) {
                        throw new IllegalArgumentException(location
                                + ": teks indikator tidak sesuai dengan ID skenario. "
                                + "Jangan mengubah/menukar kolom identitas indikator.");
                    }
                    if (!sameText(evidenceText, source.scenario.getNama())) {
                        throw new IllegalArgumentException(location
                                + ": bukti dokumen/daftar tilik tidak sesuai dengan master aktif.");
                    }
                    Long findingId = parseLong(cellString(row.getCell(10)));
                    Long expectedFindingId = source.finding == null ? null : source.finding.getId();
                    if (findingId == null ? expectedFindingId != null : !findingId.equals(expectedFindingId)) {
                        throw new IllegalArgumentException(location
                                + ": ID temuan tidak sesuai. Gunakan file terbaru dari pengajuan ini.");
                    }
                    ImportedRow item = new ImportedRow();
                    item.scenarioId = scenarioId;
                    item.score = parseScore(row.getCell(3), location);
                    item.auditorNote = cellString(row.getCell(4));
                    item.recommendation = cellString(row.getCell(5));
                    item.evidenceLink = cellString(row.getCell(6));
                    item.readiness = normalizeReadiness(cellString(row.getCell(7)), location);
                    item.auditeeNote = cellString(row.getCell(8));
                    result.add(item);
                } catch (IllegalArgumentException e) {
                    errors.add(e.getMessage());
                }
            }
        }
        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Upload dibatalkan karena validasi gagal:\n");
            int limit = Math.min(errors.size(), 10);
            for (int i = 0; i < limit; i++) message.append("- ").append(errors.get(i)).append("\n");
            if (errors.size() > limit) message.append("- dan ").append(errors.size() - limit).append(" kesalahan lain.");
            throw new IllegalArgumentException(message.toString().trim());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<AuditRow> loadRows(Session session, HasilSPMI hasil) {
        List<AuditRow> result = new ArrayList<AuditRow>();
        List<StandarSPMI> standards = ConstantValues.simpleList(
                session.createCriteria(StandarSPMI.class)
                        .add(Restrictions.eq("jenisSPMI", hasil.getJenisSPMI()))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .addOrder(Order.asc("nomorUrut")), StandarSPMI.class);
        for (StandarSPMI standard : standards) {
            List<ButirMutuSPMI> clauses = ConstantValues.simpleList(
                    session.createCriteria(ButirMutuSPMI.class)
                            .add(Restrictions.eq("standarSPMI", standard))
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .addOrder(Order.asc("nomorUrut")), ButirMutuSPMI.class);
            for (ButirMutuSPMI clause : clauses) {
                List<IndikatorSPMI> indicators = ConstantValues.simpleList(
                        session.createCriteria(IndikatorSPMI.class)
                                .add(Restrictions.eq("butirMutuSPMI", clause))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .addOrder(Order.asc("nomorUrut")), IndikatorSPMI.class);
                for (IndikatorSPMI indicator : indicators) {
                    List<SkenarioSPMI> scenarios = ConstantValues.simpleList(
                            session.createCriteria(SkenarioSPMI.class)
                                    .add(Restrictions.eq("indikatorSPMI", indicator))
                                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                    .addOrder(Order.asc("nomorUrut")), SkenarioSPMI.class);
                    for (SkenarioSPMI scenario : scenarios) {
                        HasilTemuanSPMI finding = (HasilTemuanSPMI) ConstantValues.simpleObject(
                                session.createCriteria(HasilTemuanSPMI.class)
                                        .add(Restrictions.eq("hasilSPMI", hasil))
                                        .add(Restrictions.eq("skenarioSPMI", scenario))
                                        .setMaxResults(1), HasilTemuanSPMI.class);
                        result.add(new AuditRow(standard, clause, indicator, scenario, finding));
                    }
                }
            }
        }
        return result;
    }

    private static Map<Long, SheetBlock> groupByStandard(List<AuditRow> rows) {
        Map<Long, SheetBlock> blocks = new LinkedHashMap<Long, SheetBlock>();
        for (AuditRow row : rows) {
            SheetBlock block = blocks.get(row.standard.getId());
            if (block == null) {
                block = new SheetBlock(row.standard);
                blocks.put(row.standard.getId(), block);
            }
            block.rows.add(row);
        }
        return blocks;
    }

    private static void writeCover(XSSFWorkbook workbook, Styles styles, HasilSPMI hasil) {
        XSSFSheet sheet = workbook.createSheet(SHEET_COVER);
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 70 * 256);
        for (int i = 2; i < 9; i++) sheet.setColumnWidth(i, 12 * 256);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
        Cell title = cell(sheet, 0, 0, "DAFTAR TILIK AUDIT MUTU INTERNAL (AMI)");
        title.setCellStyle(styles.title);
        writePair(sheet, styles, 2, "ID Audit", hasil.getId());
        writePair(sheet, styles, 3, "Judul Pengajuan", hasil.getNama());
        writePair(sheet, styles, 4, "Perguruan Tinggi", hasil.getPerguruanTinggi() == null ? "" : hasil.getPerguruanTinggi().getNama());
        writePair(sheet, styles, 5, "Fakultas", hasil.getFakultas() == null ? "" : hasil.getFakultas().getNama());
        writePair(sheet, styles, 6, "Program Studi", hasil.getJurusan() == null ? "" : hasil.getJurusan().getNama());
        writePair(sheet, styles, 7, "Tahun Akademik", hasil.getTa());
        writePair(sheet, styles, 8, "Semester", hasil.getSemester());
        writePair(sheet, styles, 9, "Jenis/Skema AMI", hasil.getJenisSPMI().getNama());
        writePair(sheet, styles, 10, "Tanggal Audit", hasil.getTanggal() == null ? "" : new SimpleDateFormat("dd-MM-yyyy").format(hasil.getTanggal()));
        writePair(sheet, styles, 11, "Nama Auditee", hasil.getAuditeeNama());
        writePair(sheet, styles, 12, "Nama Auditor/Tim Audit", hasil.getAuditorNama());
        writePair(sheet, styles, 13, "Lembaga Akreditasi/Skema", hasil.getJenisSPMI().getNama());
        writePair(sheet, styles, 14, "Jenjang Program", hasil.getJurusan() == null
                || hasil.getJurusan().getJenjang() == null ? "" : hasil.getJurusan().getJenjang().getNama());
        sheet.getRow(11).getCell(1).setCellStyle(styles.input);
        sheet.getRow(12).getCell(1).setCellStyle(styles.input);

        sheet.addMergedRegion(new CellRangeAddress(17, 17, 0, 8));
        cell(sheet, 17, 0, "PETUNJUK PENGISIAN").setCellStyle(styles.section);
        String[] notes = new String[] {
                "1. Isi seluruh hasil audit pada sheet indikator; jangan mengubah ID/baris tersembunyi.",
                "2. Skor: 1 = memenuhi dengan bukti valid, 0 = tidak memenuhi, kosong = belum dinilai.",
                "3. Status kesiapan bukti: Tersedia, Sebagian, atau Belum Tersedia.",
                "4. Upload kembali file XLSX ini pada pengajuan AMI yang sama."
        };
        for (int i = 0; i < notes.length; i++) {
            sheet.addMergedRegion(new CellRangeAddress(18 + i, 18 + i, 0, 8));
            cell(sheet, 18 + i, 0, notes[i]).setCellStyle(styles.note);
        }
    }

    private static void writeIndicatorSheet(XSSFWorkbook workbook, Styles styles, SheetBlock block) {
        XSSFSheet sheet = workbook.createSheet(block.sheetName);
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, FIRST_DATA_ROW);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
        cell(sheet, 0, 0, block.standard.getNama()).setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));
        cell(sheet, 1, 0, "Isi kolom D sampai I. Kolom J dan K adalah identitas sistem dan tidak boleh diubah.")
                .setCellStyle(styles.note);

        String[] headers = new String[] { "No", "Indikator", "Bukti Dokumen", "Skor 1/0",
                "Catatan Auditor", "Rekomendasi", "Bukti/Link Dokumen (Auditee)",
                "Status Kesiapan Bukti (Auditee)", "Catatan Auditee", "ID Skenario", "ID Temuan" };
        XSSFRow header = sheet.createRow(HEADER_ROW);
        for (int c = 0; c < headers.length; c++) {
            Cell hc = header.createCell(c);
            hc.setCellValue(headers[c]);
            hc.setCellStyle(styles.header);
        }

        int excelRow = FIRST_DATA_ROW;
        int no = 1;
        for (AuditRow item : block.rows) {
            XSSFRow row = sheet.createRow(excelRow);
            set(row, 0, Integer.valueOf(no++), styles.body);
            set(row, 1, item.indicator.getNama(), styles.body);
            set(row, 2, item.scenario.getNama(), styles.body);
            Integer score = item.finding == null ? null : item.finding.getSkorAmi();
            set(row, 3, score, styles.inputCenter);
            set(row, 4, item.finding == null ? "" : item.finding.getNama(), styles.input);
            set(row, 5, item.finding == null ? "" : item.finding.getRekomendasi(), styles.input);
            set(row, 6, item.finding == null ? "" : item.finding.getBuktiAuditee(), styles.input);
            set(row, 7, item.finding == null ? "" : item.finding.getStatusKesiapanBukti(), styles.inputCenter);
            set(row, 8, item.finding == null ? "" : item.finding.getCatatanAuditee(), styles.input);
            set(row, 9, item.scenario.getId(), styles.body);
            set(row, 10, item.finding == null ? null : item.finding.getId(), styles.body);
            item.sheetName = block.sheetName;
            item.excelRow = excelRow;
            excelRow++;
        }

        int last = Math.max(FIRST_DATA_ROW, excelRow - 1);
        addListValidation(sheet, 3, FIRST_DATA_ROW, last, new String[] { "0", "1" });
        addListValidation(sheet, 7, FIRST_DATA_ROW, last,
                new String[] { HasilTemuanSPMI.BUKTI_TERSEDIA,
                        HasilTemuanSPMI.BUKTI_SEBAGIAN, HasilTemuanSPMI.BUKTI_BELUM_TERSEDIA });
        sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, last, 0, 8));
        sheet.setColumnWidth(0, 7 * 256);
        sheet.setColumnWidth(1, 55 * 256);
        sheet.setColumnWidth(2, 55 * 256);
        sheet.setColumnWidth(3, 11 * 256);
        sheet.setColumnWidth(4, 40 * 256);
        sheet.setColumnWidth(5, 40 * 256);
        sheet.setColumnWidth(6, 40 * 256);
        sheet.setColumnWidth(7, 28 * 256);
        sheet.setColumnWidth(8, 40 * 256);
        sheet.setColumnHidden(9, true);
        sheet.setColumnHidden(10, true);

        int summaryRow = excelRow + 1;
        set(sheet.createRow(summaryRow), 0, "Jumlah Indikator", styles.summaryLabel);
        formula(sheet.getRow(summaryRow), 1, "COUNTA(B" + (FIRST_DATA_ROW + 1) + ":B" + (last + 1) + ")", styles.summaryValue);
        set(sheet.createRow(summaryRow + 1), 0, "Jumlah Skor 1", styles.summaryLabel);
        formula(sheet.getRow(summaryRow + 1), 1, "COUNTIF(D" + (FIRST_DATA_ROW + 1) + ":D" + (last + 1) + ",1)", styles.summaryValue);
        set(sheet.createRow(summaryRow + 2), 0, "Persentase Memenuhi", styles.summaryLabel);
        formula(sheet.getRow(summaryRow + 2), 1, "IF(B" + (summaryRow + 1) + "=0,0,B" + (summaryRow + 2) + "/B" + (summaryRow + 1) + ")", styles.percent);
    }

    private static void writeSummary(XSSFWorkbook workbook, Styles styles, Map<Long, SheetBlock> blocks) {
        XSSFSheet sheet = workbook.createSheet(SHEET_SUMMARY);
        sheet.setDisplayGridlines(false);
        title(sheet, styles, "RINGKASAN HASIL AUDIT", 5);
        header(sheet, styles, 2, new String[] { "No", "Standar", "Jumlah Indikator", "Skor 1", "Persentase" });
        int r = 3;
        int no = 1;
        for (SheetBlock block : blocks.values()) {
            int first = FIRST_DATA_ROW + 1;
            int last = FIRST_DATA_ROW + block.rows.size();
            XSSFRow row = sheet.createRow(r);
            set(row, 0, Integer.valueOf(no++), styles.body);
            set(row, 1, block.standard.getNama(), styles.body);
            formula(row, 2, "COUNTA(" + quote(block.sheetName) + "!B" + first + ":B" + last + ")", styles.summaryValue);
            formula(row, 3, "COUNTIF(" + quote(block.sheetName) + "!D" + first + ":D" + last + ",1)", styles.summaryValue);
            formula(row, 4, "IF(C" + (r + 1) + "=0,0,D" + (r + 1) + "/C" + (r + 1) + ")", styles.percent);
            r++;
        }
        XSSFRow total = sheet.createRow(r);
        set(total, 1, "TOTAL/RERATA", styles.summaryLabel);
        formula(total, 2, "SUM(C4:C" + r + ")", styles.summaryValue);
        formula(total, 3, "SUM(D4:D" + r + ")", styles.summaryValue);
        formula(total, 4, "IF(C" + (r + 1) + "=0,0,D" + (r + 1) + "/C" + (r + 1) + ")", styles.percent);
        widths(sheet, new int[] { 7, 55, 20, 15, 18 });
        sheet.createFreezePane(0, 3);
    }

    private static void writeReadiness(XSSFWorkbook workbook, Styles styles, Map<Long, SheetBlock> blocks) {
        XSSFSheet sheet = workbook.createSheet(SHEET_READINESS);
        sheet.setDisplayGridlines(false);
        title(sheet, styles, "REKAP KESIAPAN BUKTI", 7);
        header(sheet, styles, 2, new String[] { "No", "Standar", "Jumlah", "Tersedia",
                "Sebagian", "Belum Tersedia", "% Kesiapan" });
        int r = 3;
        int no = 1;
        for (SheetBlock block : blocks.values()) {
            int first = FIRST_DATA_ROW + 1;
            int last = FIRST_DATA_ROW + block.rows.size();
            String range = quote(block.sheetName) + "!H" + first + ":H" + last;
            XSSFRow row = sheet.createRow(r);
            set(row, 0, Integer.valueOf(no++), styles.body);
            set(row, 1, block.standard.getNama(), styles.body);
            formula(row, 2, "COUNTA(" + quote(block.sheetName) + "!B" + first + ":B" + last + ")", styles.summaryValue);
            formula(row, 3, "COUNTIF(" + range + ",\"Tersedia\")", styles.summaryValue);
            formula(row, 4, "COUNTIF(" + range + ",\"Sebagian\")", styles.summaryValue);
            formula(row, 5, "COUNTIF(" + range + ",\"Belum Tersedia\")", styles.summaryValue);
            formula(row, 6, "IF(C" + (r + 1) + "=0,0,D" + (r + 1) + "/C" + (r + 1) + ")", styles.percent);
            r++;
        }
        widths(sheet, new int[] { 7, 55, 14, 14, 14, 20, 18 });
        sheet.createFreezePane(0, 3);
    }

    private static void writeNonCompliance(XSSFWorkbook workbook, Styles styles, List<AuditRow> rows) {
        XSSFSheet sheet = workbook.createSheet(SHEET_NONCOMPLIANCE);
        sheet.setDisplayGridlines(false);
        title(sheet, styles, "LAPORAN INDIKATOR TIDAK MEMENUHI", 5);
        header(sheet, styles, 2, new String[] { "No", "Standar", "Indikator", "Catatan Auditor", "Rekomendasi" });
        int r = 3;
        int no = 1;
        for (AuditRow item : rows) {
            String q = quote(item.sheetName);
            int sourceRow = item.excelRow + 1;
            XSSFRow row = sheet.createRow(r++);
            set(row, 0, Integer.valueOf(no++), styles.body);
            formula(row, 1, "IF(" + q + "!D" + sourceRow + "=0,\"" + escapeFormulaText(item.standard.getNama()) + "\",\"\")", styles.body);
            formula(row, 2, "IF(" + q + "!D" + sourceRow + "=0," + q + "!B" + sourceRow + ",\"\")", styles.body);
            formula(row, 3, "IF(" + q + "!D" + sourceRow + "=0," + q + "!E" + sourceRow + ",\"\")", styles.body);
            formula(row, 4, "IF(" + q + "!D" + sourceRow + "=0," + q + "!F" + sourceRow + ",\"\")", styles.body);
        }
        widths(sheet, new int[] { 7, 40, 55, 45, 45 });
        sheet.createFreezePane(0, 3);
        sheet.setAutoFilter(new CellRangeAddress(2, Math.max(3, r - 1), 0, 4));
    }

    private static void writeDataSheet(XSSFWorkbook workbook, Styles styles, List<AuditRow> rows) {
        XSSFSheet sheet = workbook.createSheet(SHEET_DATA);
        header(sheet, styles, 0, new String[] { "Urut", "ID Skenario", "Sheet", "Baris",
                "Standar", "Indikator" });
        int r = 1;
        for (AuditRow item : rows) {
            XSSFRow row = sheet.createRow(r);
            set(row, 0, Integer.valueOf(r), styles.body);
            set(row, 1, item.scenario.getId(), styles.body);
            set(row, 2, item.sheetName, styles.body);
            set(row, 3, Integer.valueOf(item.excelRow + 1), styles.body);
            set(row, 4, item.standard.getNama(), styles.body);
            set(row, 5, item.indicator.getNama(), styles.body);
            r++;
        }
    }

    private static void writeMetaSheet(XSSFWorkbook workbook, HasilSPMI hasil, int count) {
        XSSFSheet sheet = workbook.createSheet(SHEET_META);
        writeRawPair(sheet, 0, "FORMAT_VERSION", FORMAT_VERSION);
        writeRawPair(sheet, 1, "HASIL_SPMI_ID", hasil.getId());
        writeRawPair(sheet, 2, "JENIS_SPMI_ID", hasil.getJenisSPMI().getId());
        writeRawPair(sheet, 3, "JUMLAH_INDIKATOR", Integer.valueOf(count));
        writeRawPair(sheet, 4, "DIBUAT_PADA", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    private static void writePair(XSSFSheet sheet, Styles styles, int rowIndex, String label, Object value) {
        XSSFRow row = sheet.createRow(rowIndex);
        set(row, 0, label, styles.summaryLabel);
        set(row, 1, value, styles.body);
    }

    private static void writeRawPair(XSSFSheet sheet, int rowIndex, String label, Object value) {
        XSSFRow row = sheet.createRow(rowIndex);
        set(row, 0, label, null);
        set(row, 1, value, null);
    }

    private static void title(XSSFSheet sheet, Styles styles, String text, int columns) {
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns - 1));
        cell(sheet, 0, 0, text).setCellStyle(styles.title);
    }

    private static void header(XSSFSheet sheet, Styles styles, int rowIndex, String[] values) {
        XSSFRow row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) set(row, i, values[i], styles.header);
    }

    private static Cell cell(XSSFSheet sheet, int rowIndex, int column, String value) {
        XSSFRow row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        Cell cell = row.getCell(column);
        if (cell == null) cell = row.createCell(column);
        cell.setCellValue(nullToEmpty(value));
        return cell;
    }

    private static void set(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value == null ? "" : String.valueOf(value));
        if (style != null) cell.setCellStyle(style);
    }

    private static void formula(Row row, int column, String formula, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellFormula(formula);
        if (style != null) cell.setCellStyle(style);
    }

    private static void addListValidation(XSSFSheet sheet, int column, int firstRow, int lastRow,
            String[] values) {
        DataValidationHelper helper = new XSSFDataValidationHelper(sheet);
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        DataValidation validation = helper.createValidation(constraint,
                new CellRangeAddressList(firstRow, lastRow, column, column));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Nilai tidak valid", "Pilih nilai dari daftar yang tersedia.");
        sheet.addValidationData(validation);
    }

    private static Integer parseScore(Cell cell, String location) {
        String text = cellString(cell);
        if (text.length() == 0) return null;
        if ("1".equals(text) || "1.0".equals(text)) return Integer.valueOf(1);
        if ("0".equals(text) || "0.0".equals(text)) return Integer.valueOf(0);
        throw new IllegalArgumentException(location + ": skor harus 1, 0, atau kosong (nilai: " + text + ").");
    }

    private static String normalizeReadiness(String value, String location) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) return null;
        if (text.equalsIgnoreCase(HasilTemuanSPMI.BUKTI_TERSEDIA)) return HasilTemuanSPMI.BUKTI_TERSEDIA;
        if (text.equalsIgnoreCase(HasilTemuanSPMI.BUKTI_SEBAGIAN)) return HasilTemuanSPMI.BUKTI_SEBAGIAN;
        if (text.equalsIgnoreCase(HasilTemuanSPMI.BUKTI_BELUM_TERSEDIA)) return HasilTemuanSPMI.BUKTI_BELUM_TERSEDIA;
        throw new IllegalArgumentException(location + ": status kesiapan bukti tidak valid (" + text + ").");
    }

    private static String mapScoreToStatus(Integer score, String current) {
        if (score == null) return null;
        if (score.intValue() == 1) {
            return HasilTemuanSPMI.LS1.equals(current) ? HasilTemuanSPMI.LS1 : HasilTemuanSPMI.S1;
        }
        if (HasilTemuanSPMI.KTS_MYR1.equals(current) || HasilTemuanSPMI.KTS_MNR1.equals(current)) return current;
        return HasilTemuanSPMI.O1;
    }

    private static String cellString(Cell cell) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                double value = cell.getNumericCellValue();
                if (value == Math.rint(value)) return String.valueOf((long) value);
                return String.valueOf(value);
            }
            if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
            if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_NUMERIC) {
                    double value = cell.getNumericCellValue();
                    if (value == Math.rint(value)) return String.valueOf((long) value);
                    return String.valueOf(value);
                }
                return nullToEmpty(cell.getStringCellValue()).trim();
            }
            return nullToEmpty(cell.getStringCellValue()).trim();
        } catch (Exception e) {
            return nullToEmpty(cell.toString()).trim();
        }
    }

    private static String readString(XSSFSheet sheet, int row, int column) {
        return sheet == null || sheet.getRow(row) == null ? "" : cellString(sheet.getRow(row).getCell(column));
    }

    private static Long parseLong(String text) {
        if (text == null || text.trim().length() == 0) return null;
        try { return Long.valueOf(text.trim()); }
        catch (Exception e) { return null; }
    }

    private static boolean isSpecialSheet(String name) {
        return SHEET_COVER.equals(name) || SHEET_SUMMARY.equals(name) || SHEET_READINESS.equals(name)
                || SHEET_NONCOMPLIANCE.equals(name) || SHEET_DATA.equals(name) || SHEET_META.equals(name);
    }

    private static String uniqueSheetName(String proposed, Set<String> used) {
        String base = WorkbookUtil.createSafeSheetName(nullToEmpty(proposed));
        if (base.length() > 31) base = base.substring(0, 31);
        if (base.length() == 0) base = "Standar";
        String candidate = base;
        int suffix = 2;
        while (used.contains(candidate.toLowerCase())) {
            String end = " (" + suffix++ + ")";
            candidate = base.substring(0, Math.min(base.length(), 31 - end.length())) + end;
        }
        used.add(candidate.toLowerCase());
        return candidate;
    }

    private static String quote(String sheetName) {
        return "'" + sheetName.replace("'", "''") + "'";
    }

    private static String escapeFormulaText(String text) {
        return nullToEmpty(text).replace("\"", "\"\"");
    }

    private static String safeFileName(String text) {
        String value = nullToEmpty(text).replaceAll("[^A-Za-z0-9_-]+", "_");
        if (value.length() == 0) value = "Audit";
        return value.length() > 60 ? value.substring(0, 60) : value;
    }

    private static void widths(XSSFSheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private static boolean notEmpty(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static boolean sameText(String left, String right) {
        return normalizeText(left).equals(normalizeText(right));
    }

    private static String normalizeText(String value) {
        return nullToEmpty(value).replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String emptyToNull(String value) {
        return notEmpty(value) ? value.trim() : null;
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static final class AuditRow {
        final StandarSPMI standard;
        final ButirMutuSPMI clause;
        final IndikatorSPMI indicator;
        final SkenarioSPMI scenario;
        final HasilTemuanSPMI finding;
        String sheetName;
        int excelRow;

        AuditRow(StandarSPMI standard, ButirMutuSPMI clause, IndikatorSPMI indicator,
                SkenarioSPMI scenario, HasilTemuanSPMI finding) {
            this.standard = standard;
            this.clause = clause;
            this.indicator = indicator;
            this.scenario = scenario;
            this.finding = finding;
        }
    }

    private static final class SheetBlock {
        final StandarSPMI standard;
        final List<AuditRow> rows = new ArrayList<AuditRow>();
        String sheetName;
        SheetBlock(StandarSPMI standard) { this.standard = standard; }
    }

    private static final class ImportedRow {
        Long scenarioId;
        Integer score;
        String auditorNote;
        String recommendation;
        String evidenceLink;
        String readiness;
        String auditeeNote;
    }

    public static final class ImportResult {
        private final int total;
        private final int inserted;
        private final int updated;
        private final int skipped;

        ImportResult(int total, int inserted, int updated, int skipped) {
            this.total = total;
            this.inserted = inserted;
            this.updated = updated;
            this.skipped = skipped;
        }

        public String message() {
            return "Upload format AMI berhasil. Indikator tervalidasi: " + total
                    + ", data baru: " + inserted + ", diperbarui: " + updated
                    + ", kosong/dilewati: " + skipped + ".";
        }
    }

    private static final class Styles {
        final XSSFCellStyle title;
        final XSSFCellStyle section;
        final XSSFCellStyle header;
        final XSSFCellStyle body;
        final XSSFCellStyle input;
        final XSSFCellStyle inputCenter;
        final XSSFCellStyle note;
        final XSSFCellStyle summaryLabel;
        final XSSFCellStyle summaryValue;
        final XSSFCellStyle percent;

        Styles(XSSFWorkbook workbook) {
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title = base(workbook, titleFont, IndexedColors.DARK_BLUE, true);
            title.setAlignment(CellStyle.ALIGN_CENTER);

            XSSFFont boldWhite = workbook.createFont();
            boldWhite.setBoldweight(Font.BOLDWEIGHT_BOLD);
            boldWhite.setColor(IndexedColors.WHITE.getIndex());
            section = base(workbook, boldWhite, IndexedColors.TEAL, true);
            header = base(workbook, boldWhite, IndexedColors.DARK_BLUE, true);
            header.setAlignment(CellStyle.ALIGN_CENTER);

            XSSFFont normal = workbook.createFont();
            normal.setFontHeightInPoints((short) 10);
            body = base(workbook, normal, null, true);
            input = base(workbook, normal, IndexedColors.LIGHT_YELLOW, true);
            inputCenter = base(workbook, normal, IndexedColors.LIGHT_YELLOW, true);
            inputCenter.setAlignment(CellStyle.ALIGN_CENTER);

            XSSFFont noteFont = workbook.createFont();
            noteFont.setItalic(true);
            noteFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
            note = base(workbook, noteFont, IndexedColors.GREY_25_PERCENT, true);

            XSSFFont bold = workbook.createFont();
            bold.setBoldweight(Font.BOLDWEIGHT_BOLD);
            summaryLabel = base(workbook, bold, IndexedColors.LIGHT_CORNFLOWER_BLUE, true);
            summaryValue = base(workbook, bold, null, true);
            summaryValue.setAlignment(CellStyle.ALIGN_CENTER);
            percent = base(workbook, bold, null, true);
            percent.setAlignment(CellStyle.ALIGN_CENTER);
            percent.setDataFormat(workbook.createDataFormat().getFormat("0%"));
        }

        private static XSSFCellStyle base(XSSFWorkbook workbook, XSSFFont font,
                IndexedColors fill, boolean border) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setVerticalAlignment(CellStyle.VERTICAL_TOP);
            style.setWrapText(true);
            if (fill != null) {
                style.setFillForegroundColor(fill.getIndex());
                style.setFillPattern(CellStyle.SOLID_FOREGROUND);
            }
            if (border) {
                style.setBorderBottom(CellStyle.BORDER_THIN);
                style.setBorderTop(CellStyle.BORDER_THIN);
                style.setBorderLeft(CellStyle.BORDER_THIN);
                style.setBorderRight(CellStyle.BORDER_THIN);
            }
            return style;
        }
    }
}
