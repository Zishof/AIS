package ais.action.master.spmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
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
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;

/** Download/upload seluruh hierarki master SPMI dalam satu workbook lintas kampus. */
public final class SpmiGlobalExcelHelper {

    public static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String FORMAT_VERSION = "AIS-SPMI-GLOBAL-2026-V1";
    private static final String GUIDE = "PETUNJUK";
    private static final String JENIS = "JENIS_SPMI";
    private static final String STANDAR = "STANDAR";
    private static final String BUTIR = "BUTIR_MUTU";
    private static final String INDIKATOR = "INDIKATOR";
    private static final String SKENARIO = "SKENARIO_BUKTI";
    private static final String META = "_SPMI_META";
    private static final int HEADER_ROW = 3;
    private static final int FIRST_DATA_ROW = 4;
    private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

    private SpmiGlobalExcelHelper() {}

    @SuppressWarnings("unchecked")
    public static byte[] exportWorkbook() throws Exception {
        Session session = HibernateUtil.currentSession();
        List<JenisSPMI> jenis = ConstantValues.simpleList(session.createCriteria(JenisSPMI.class)
                .addOrder(Order.asc("nama")), JenisSPMI.class);
        List<StandarSPMI> standar = ConstantValues.simpleList(session.createCriteria(StandarSPMI.class)
                .addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")), StandarSPMI.class);
        List<ButirMutuSPMI> butir = ConstantValues.simpleList(session.createCriteria(ButirMutuSPMI.class)
                .addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")), ButirMutuSPMI.class);
        List<IndikatorSPMI> indikator = ConstantValues.simpleList(session.createCriteria(IndikatorSPMI.class)
                .addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")), IndikatorSPMI.class);
        List<SkenarioSPMI> skenario = ConstantValues.simpleList(session.createCriteria(SkenarioSPMI.class)
                .addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")), SkenarioSPMI.class);

        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Styles styles = new Styles(workbook);
            writeGuide(workbook, styles);
            Map<Long, String> jenisRef = references("JNS", ids(jenis));
            Map<Long, String> standarRef = references("STD", ids(standar));
            Map<Long, String> butirRef = references("BTR", ids(butir));
            Map<Long, String> indikatorRef = references("IND", ids(indikator));
            Map<Long, String> skenarioRef = references("SKN", ids(skenario));
            writeJenis(workbook, styles, jenis, jenisRef);
            writeStandar(workbook, styles, standar, jenisRef, standarRef);
            writeButir(workbook, styles, butir, standarRef, butirRef);
            writeIndikator(workbook, styles, indikator, butirRef, indikatorRef);
            writeSkenario(workbook, styles, skenario, indikatorRef, skenarioRef);
            writeMeta(workbook, jenis.size(), standar.size(), butir.size(), indikator.size(), skenario.size());
            workbook.setSheetHidden(workbook.getSheetIndex(META), true);
            workbook.setActiveSheet(workbook.getSheetIndex(GUIDE));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try { workbook.write(output); return output.toByteArray(); }
            finally { output.close(); }
        } finally { workbook.getPackage().close(); }
    }

    public static ImportResult importWorkbook(byte[] bytes) throws Exception {
        validateBytes(bytes);
        XSSFWorkbook workbook;
        try { workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes)); }
        catch (Exception e) { throw new IllegalArgumentException("File tidak dapat dibaca sebagai XLSX yang valid.", e); }
        try {
            validateStructure(workbook);
            List<MasterRow> jenisRows = readRows(workbook.getSheet(JENIS), JENIS, 5, false);
            List<MasterRow> standarRows = readRows(workbook.getSheet(STANDAR), STANDAR, 6, true);
            List<MasterRow> butirRows = readRows(workbook.getSheet(BUTIR), BUTIR, 6, true);
            List<MasterRow> indikatorRows = readRows(workbook.getSheet(INDIKATOR), INDIKATOR, 6, true);
            List<MasterRow> skenarioRows = readRows(workbook.getSheet(SKENARIO), SKENARIO, 6, true);
            validateReferences(jenisRows, standarRows, butirRows, indikatorRows, skenarioRows);

            Session session = null;
            Transaction transaction = null;
            try {
                session = HibernateUtil.openSession();
                transaction = session.beginTransaction();
                Counter counter = new Counter();
                Map<String, JenisSPMI> jenisMap = saveJenis(session, jenisRows, counter);
                Map<String, StandarSPMI> standarMap = saveStandar(session, standarRows, jenisMap, counter);
                Map<String, ButirMutuSPMI> butirMap = saveButir(session, butirRows, standarMap, counter);
                Map<String, IndikatorSPMI> indikatorMap = saveIndikator(session, indikatorRows, butirMap, counter);
                saveSkenario(session, skenarioRows, indikatorMap, counter);
                session.flush();
                transaction.commit();
                return new ImportResult(jenisRows.size(), standarRows.size(), butirRows.size(),
                        indikatorRows.size(), skenarioRows.size(), counter.inserted, counter.updated);
            } catch (Exception e) {
                if (transaction != null) try { transaction.rollback(); }
                catch (Exception rollbackError) {
                    ais.common.ErrorAuditUtil.record(rollbackError, "rollback upload SPMI global");
                }
                throw e;
            } finally { HibernateUtil.closeSessionQuietly(session); }
        } finally { workbook.getPackage().close(); }
    }

    public static String fileName() {
        return "Master_SPMI_Global_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
    }

    private static void validateBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) throw new IllegalArgumentException("File upload kosong atau rusak.");
        if (bytes.length > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("Ukuran file melebihi 10 MB.");
        if (bytes[0] != 'P' || bytes[1] != 'K') throw new IllegalArgumentException("File harus berformat XLSX.");
    }

    private static void validateStructure(XSSFWorkbook workbook) {
        String[] required = { GUIDE, JENIS, STANDAR, BUTIR, INDIKATOR, SKENARIO };
        for (String name : required) if (workbook.getSheet(name) == null)
            throw new IllegalArgumentException("Sheet wajib '" + name + "' tidak ditemukan.");
        XSSFSheet meta = workbook.getSheet(META);
        if (meta != null) {
            String version = text(meta, 0, 1);
            if (!FORMAT_VERSION.equals(version))
                throw new IllegalArgumentException("Versi format SPMI Global tidak didukung: " + version);
        }
    }

    private static List<MasterRow> readRows(XSSFSheet sheet, String sheetName,
            int columns, boolean hasParent) {
        Row header = sheet.getRow(HEADER_ROW);
        if (header == null || !"Kode Referensi".equalsIgnoreCase(cellText(header.getCell(0))))
            throw new IllegalArgumentException("Header sheet '" + sheetName + "' tidak sesuai format.");
        List<MasterRow> result = new ArrayList<MasterRow>();
        Set<String> refs = new HashSet<String>();
        List<String> errors = new ArrayList<String>();
        for (int r = FIRST_DATA_ROW; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r); if (row == null) continue;
            String ref = cellText(row.getCell(0));
            if (!notEmpty(ref) && !notEmpty(cellText(row.getCell(hasParent ? 3 : 2)))) continue;
            String location = sheetName + " baris " + (r + 1);
            MasterRow item = new MasterRow(); item.location = location; item.ref = key(ref);
            if (!notEmpty(item.ref)) { errors.add(location + ": Kode Referensi wajib diisi."); continue; }
            if (!refs.add(item.ref)) { errors.add(location + ": Kode Referensi duplikat '" + ref + "'."); continue; }
            if (hasParent) {
                item.parentRef = key(cellText(row.getCell(1)));
                item.order = parseInteger(row.getCell(2), location);
                item.name = cellText(row.getCell(3)); item.description = cellText(row.getCell(4));
                item.active = parseActive(cellText(row.getCell(5)), location);
                if (!notEmpty(item.parentRef)) errors.add(location + ": Kode Referensi Induk wajib diisi.");
            } else {
                item.businessCode = cellText(row.getCell(1)); item.name = cellText(row.getCell(2));
                item.description = cellText(row.getCell(3)); item.active = parseActive(cellText(row.getCell(4)), location);
            }
            if (!notEmpty(item.name)) errors.add(location + ": Nama wajib diisi.");
            result.add(item);
        }
        Set<String> naturalKeys = new HashSet<String>();
        for (MasterRow item : result) {
            String natural = hasParent ? item.parentRef + "|" + key(item.name)
                    : (notEmpty(item.businessCode) ? "kode|" + key(item.businessCode) : "nama|" + key(item.name));
            if (!naturalKeys.add(natural)) errors.add(item.location
                    + ": data bisnis duplikat dalam induk yang sama ('" + item.name + "').");
        }
        throwErrors(errors);
        return result;
    }

    private static void validateReferences(List<MasterRow> jenis, List<MasterRow> standar,
            List<MasterRow> butir, List<MasterRow> indikator, List<MasterRow> skenario) {
        if (jenis.isEmpty()) throw new IllegalArgumentException("Sheet JENIS_SPMI belum berisi data.");
        Set<String> parents = refs(jenis);
        validateParents(standar, parents, STANDAR); parents = refs(standar);
        validateParents(butir, parents, BUTIR); parents = refs(butir);
        validateParents(indikator, parents, INDIKATOR); parents = refs(indikator);
        validateParents(skenario, parents, SKENARIO);
    }

    private static void validateParents(List<MasterRow> rows, Set<String> parents, String sheet) {
        List<String> errors = new ArrayList<String>();
        for (MasterRow row : rows) if (!parents.contains(row.parentRef))
            errors.add(row.location + ": referensi induk '" + row.parentRef + "' tidak ditemukan.");
        throwErrors(errors);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, JenisSPMI> saveJenis(Session session, List<MasterRow> rows, Counter counter) {
        List<JenisSPMI> existing = ConstantValues.simpleList(session.createCriteria(JenisSPMI.class), JenisSPMI.class);
        Map<String, JenisSPMI> result = new HashMap<String, JenisSPMI>();
        for (MasterRow row : rows) {
            JenisSPMI item = findJenis(existing, row.businessCode, row.name);
            boolean insert = item == null; if (insert) item = new JenisSPMI();
            item.setKode(emptyToNull(row.businessCode)); item.setNama(row.name);
            item.setKeterangan(emptyToNull(row.description)); item.setAktif(row.active);
            if (insert) { session.save(item); existing.add(item); counter.inserted++; }
            else { session.update(item); counter.updated++; }
            result.put(row.ref, item);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, StandarSPMI> saveStandar(Session session, List<MasterRow> rows,
            Map<String, JenisSPMI> parents, Counter counter) {
        List<StandarSPMI> existing = ConstantValues.simpleList(session.createCriteria(StandarSPMI.class), StandarSPMI.class);
        Map<String, StandarSPMI> result = new HashMap<String, StandarSPMI>();
        for (MasterRow row : rows) {
            JenisSPMI parent = parents.get(row.parentRef);
            StandarSPMI item = findStandar(existing, parent, row.name);
            boolean insert = item == null; if (insert) item = new StandarSPMI();
            item.setJenisSPMI(parent); item.setNomorUrut(row.order); item.setNama(row.name);
            item.setKeterangan(emptyToNull(row.description)); item.setAktif(row.active);
            if (insert) { session.save(item); existing.add(item); counter.inserted++; } else { session.update(item); counter.updated++; }
            result.put(row.ref, item);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ButirMutuSPMI> saveButir(Session session, List<MasterRow> rows,
            Map<String, StandarSPMI> parents, Counter counter) {
        List<ButirMutuSPMI> existing = ConstantValues.simpleList(session.createCriteria(ButirMutuSPMI.class), ButirMutuSPMI.class);
        Map<String, ButirMutuSPMI> result = new HashMap<String, ButirMutuSPMI>();
        for (MasterRow row : rows) {
            StandarSPMI parent = parents.get(row.parentRef);
            ButirMutuSPMI item = findButir(existing, parent, row.name);
            boolean insert = item == null; if (insert) item = new ButirMutuSPMI();
            item.setStandarSPMI(parent); item.setNomorUrut(row.order); item.setNama(row.name);
            item.setKeterangan(emptyToNull(row.description)); item.setAktif(row.active);
            if (insert) { session.save(item); existing.add(item); counter.inserted++; } else { session.update(item); counter.updated++; }
            result.put(row.ref, item);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, IndikatorSPMI> saveIndikator(Session session, List<MasterRow> rows,
            Map<String, ButirMutuSPMI> parents, Counter counter) {
        List<IndikatorSPMI> existing = ConstantValues.simpleList(session.createCriteria(IndikatorSPMI.class), IndikatorSPMI.class);
        Map<String, IndikatorSPMI> result = new HashMap<String, IndikatorSPMI>();
        for (MasterRow row : rows) {
            ButirMutuSPMI parent = parents.get(row.parentRef);
            IndikatorSPMI item = findIndikator(existing, parent, row.name);
            boolean insert = item == null; if (insert) item = new IndikatorSPMI();
            item.setButirMutuSPMI(parent); item.setNomorUrut(row.order); item.setNama(row.name);
            item.setKeterangan(emptyToNull(row.description)); item.setAktif(row.active);
            if (insert) { session.save(item); existing.add(item); counter.inserted++; } else { session.update(item); counter.updated++; }
            result.put(row.ref, item);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void saveSkenario(Session session, List<MasterRow> rows,
            Map<String, IndikatorSPMI> parents, Counter counter) {
        List<SkenarioSPMI> existing = ConstantValues.simpleList(session.createCriteria(SkenarioSPMI.class), SkenarioSPMI.class);
        for (MasterRow row : rows) {
            IndikatorSPMI parent = parents.get(row.parentRef);
            SkenarioSPMI item = findSkenario(existing, parent, row.name);
            boolean insert = item == null; if (insert) item = new SkenarioSPMI();
            item.setIndikatorSPMI(parent); item.setNomorUrut(row.order); item.setNama(row.name);
            item.setKeterangan(emptyToNull(row.description)); item.setAktif(row.active);
            if (insert) { session.save(item); existing.add(item); counter.inserted++; } else { session.update(item); counter.updated++; }
        }
    }

    private static JenisSPMI findJenis(List<JenisSPMI> rows, String code, String name) {
        for (JenisSPMI row : rows) if (notEmpty(code) && same(row.getKode(), code)) return row;
        for (JenisSPMI row : rows) if (same(row.getNama(), name)) return row;
        return null;
    }
    private static StandarSPMI findStandar(List<StandarSPMI> rows, JenisSPMI parent, String name) {
        for (StandarSPMI row : rows) if (row.getJenisSPMI() != null && sameId(row.getJenisSPMI().getId(), parent.getId()) && same(row.getNama(), name)) return row; return null;
    }
    private static ButirMutuSPMI findButir(List<ButirMutuSPMI> rows, StandarSPMI parent, String name) {
        for (ButirMutuSPMI row : rows) if (row.getStandarSPMI() != null && sameId(row.getStandarSPMI().getId(), parent.getId()) && same(row.getNama(), name)) return row; return null;
    }
    private static IndikatorSPMI findIndikator(List<IndikatorSPMI> rows, ButirMutuSPMI parent, String name) {
        for (IndikatorSPMI row : rows) if (row.getButirMutuSPMI() != null && sameId(row.getButirMutuSPMI().getId(), parent.getId()) && same(row.getNama(), name)) return row; return null;
    }
    private static SkenarioSPMI findSkenario(List<SkenarioSPMI> rows, IndikatorSPMI parent, String name) {
        for (SkenarioSPMI row : rows) if (row.getIndikatorSPMI() != null && sameId(row.getIndikatorSPMI().getId(), parent.getId()) && same(row.getNama(), name)) return row; return null;
    }

    private static void writeGuide(XSSFWorkbook workbook, Styles styles) {
        XSSFSheet sheet = workbook.createSheet(GUIDE); sheet.setDisplayGridlines(false);
        title(sheet, styles, "DOWNLOAD / UPLOAD MASTER SPMI GLOBAL", 6);
        String[] lines = {
            "Satu workbook memuat seluruh hierarki master SPMI dan dapat dipindahkan antar kampus tanpa bergantung pada ID database.",
            "Urutan relasi: JENIS_SPMI -> STANDAR -> BUTIR_MUTU -> INDIKATOR -> SKENARIO_BUKTI.",
            "Kode Referensi harus unik pada sheet-nya. Kode Referensi Induk harus ada pada sheet satu tingkat di atas.",
            "Upload melakukan tambah/perbarui (upsert) berdasarkan Kode Jenis atau Nama dalam induk yang sama; data yang tidak ada di file tidak dihapus.",
            "Aktif diisi Ya/Tidak. No Urut harus bilangan bulat. Jangan mengganti nama sheet atau judul kolom.",
            "Seluruh workbook divalidasi dan disimpan dalam satu transaksi; satu kesalahan membatalkan semua perubahan.",
            "Simpan sebagai XLSX maksimum 10 MB. Gunakan file download terbaru sebagai template paling aman."
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.addMergedRegion(new CellRangeAddress(2 + i, 2 + i, 0, 5));
            cell(sheet, 2 + i, 0, (i + 1) + ". " + lines[i]).setCellStyle(styles.note);
        }
        widths(sheet, new int[] { 22, 24, 24, 24, 24, 24 });
    }

    private static void writeJenis(XSSFWorkbook wb, Styles st, List<JenisSPMI> rows, Map<Long, String> refs) {
        XSSFSheet sh = dataSheet(wb, st, JENIS, "MASTER JENIS SPMI",
                new String[] { "Kode Referensi", "Kode Jenis", "Nama Jenis SPMI", "Keterangan", "Aktif" });
        int r = FIRST_DATA_ROW; for (JenisSPMI item : rows) { XSSFRow row = sh.createRow(r++);
            put(row, 0, refs.get(item.getId()), st.body); put(row, 1, item.getKode(), st.input);
            put(row, 2, item.getNama(), st.input); put(row, 3, item.getKeterangan(), st.input);
            put(row, 4, active(item.getAktif()), st.inputCenter); }
        finish(sh, r, 4, new int[] { 20, 20, 45, 50, 12 });
    }

    private static void writeStandar(XSSFWorkbook wb, Styles st, List<StandarSPMI> rows,
            Map<Long, String> parentRefs, Map<Long, String> refs) {
        XSSFSheet sh = dataSheet(wb, st, STANDAR, "MASTER STANDAR SPMI / REFERENSI EKSTERNAL",
                new String[] { "Kode Referensi", "Kode Referensi Induk", "No Urut", "Nama Standar", "Keterangan", "Aktif" });
        int r = FIRST_DATA_ROW; for (StandarSPMI item : rows) { XSSFRow row = sh.createRow(r++);
            put(row, 0, refs.get(item.getId()), st.body); put(row, 1, ref(parentRefs, item.getJenisSPMI()), st.body);
            put(row, 2, item.getNomorUrut(), st.inputCenter); put(row, 3, item.getNama(), st.input);
            put(row, 4, item.getKeterangan(), st.input); put(row, 5, active(item.getAktif()), st.inputCenter); }
        finish(sh, r, 5, new int[] { 20, 24, 12, 50, 50, 12 });
    }

    private static void writeButir(XSSFWorkbook wb, Styles st, List<ButirMutuSPMI> rows,
            Map<Long, String> parentRefs, Map<Long, String> refs) {
        XSSFSheet sh = dataSheet(wb, st, BUTIR, "MASTER PERNYATAAN AYAT STANDAR / BUTIR MUTU",
                new String[] { "Kode Referensi", "Kode Referensi Induk", "No Urut", "Nama Butir Mutu", "Keterangan", "Aktif" });
        int r = FIRST_DATA_ROW; for (ButirMutuSPMI item : rows) { XSSFRow row = sh.createRow(r++);
            put(row, 0, refs.get(item.getId()), st.body); put(row, 1, ref(parentRefs, item.getStandarSPMI()), st.body);
            put(row, 2, item.getNomorUrut(), st.inputCenter); put(row, 3, item.getNama(), st.input);
            put(row, 4, item.getKeterangan(), st.input); put(row, 5, active(item.getAktif()), st.inputCenter); }
        finish(sh, r, 5, new int[] { 20, 24, 12, 55, 50, 12 });
    }

    private static void writeIndikator(XSSFWorkbook wb, Styles st, List<IndikatorSPMI> rows,
            Map<Long, String> parentRefs, Map<Long, String> refs) {
        XSSFSheet sh = dataSheet(wb, st, INDIKATOR, "MASTER INDIKATOR SPMI",
                new String[] { "Kode Referensi", "Kode Referensi Induk", "No Urut", "Nama Indikator", "Keterangan", "Aktif" });
        int r = FIRST_DATA_ROW; for (IndikatorSPMI item : rows) { XSSFRow row = sh.createRow(r++);
            put(row, 0, refs.get(item.getId()), st.body); put(row, 1, ref(parentRefs, item.getButirMutuSPMI()), st.body);
            put(row, 2, item.getNomorUrut(), st.inputCenter); put(row, 3, item.getNama(), st.input);
            put(row, 4, item.getKeterangan(), st.input); put(row, 5, active(item.getAktif()), st.inputCenter); }
        finish(sh, r, 5, new int[] { 20, 24, 12, 60, 50, 12 });
    }

    private static void writeSkenario(XSSFWorkbook wb, Styles st, List<SkenarioSPMI> rows,
            Map<Long, String> parentRefs, Map<Long, String> refs) {
        XSSFSheet sh = dataSheet(wb, st, SKENARIO, "MASTER DAFTAR TILIK / SKENARIO PERTANYAAN / BUKTI",
                new String[] { "Kode Referensi", "Kode Referensi Induk", "No Urut", "Daftar Tilik/Skenario/Bukti", "Keterangan", "Aktif" });
        int r = FIRST_DATA_ROW; for (SkenarioSPMI item : rows) { XSSFRow row = sh.createRow(r++);
            put(row, 0, refs.get(item.getId()), st.body); put(row, 1, ref(parentRefs, item.getIndikatorSPMI()), st.body);
            put(row, 2, item.getNomorUrut(), st.inputCenter); put(row, 3, item.getNama(), st.input);
            put(row, 4, item.getKeterangan(), st.input); put(row, 5, active(item.getAktif()), st.inputCenter); }
        finish(sh, r, 5, new int[] { 20, 24, 12, 65, 50, 12 });
    }

    private static XSSFSheet dataSheet(XSSFWorkbook wb, Styles st, String name, String title, String[] headers) {
        XSSFSheet sh = wb.createSheet(name); sh.setDisplayGridlines(false); sh.createFreezePane(0, FIRST_DATA_ROW);
        title(sh, st, title, headers.length); sh.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));
        cell(sh, 1, 0, "Isi sel berwarna. Kode Referensi menjaga hubungan antarsheet dan boleh dibuat sendiri selama unik.").setCellStyle(st.note);
        XSSFRow header = sh.createRow(HEADER_ROW); for (int c = 0; c < headers.length; c++) put(header, c, headers[c], st.header);
        return sh;
    }

    private static void finish(XSSFSheet sheet, int nextRow, int activeColumn, int[] widths) {
        int last = Math.max(FIRST_DATA_ROW, nextRow - 1);
        addListValidation(sheet, activeColumn, FIRST_DATA_ROW, Math.max(last, FIRST_DATA_ROW + 500), new String[] { "Ya", "Tidak" });
        sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, last, 0, activeColumn)); widths(sheet, widths);
    }

    private static void writeMeta(XSSFWorkbook wb, int jenis, int standar, int butir, int indikator, int skenario) {
        XSSFSheet sh = wb.createSheet(META); raw(sh, 0, "FORMAT_VERSION", FORMAT_VERSION);
        raw(sh, 1, "DIBUAT_PADA", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        raw(sh, 2, "JUMLAH_JENIS", jenis); raw(sh, 3, "JUMLAH_STANDAR", standar);
        raw(sh, 4, "JUMLAH_BUTIR", butir); raw(sh, 5, "JUMLAH_INDIKATOR", indikator); raw(sh, 6, "JUMLAH_SKENARIO", skenario);
    }

    private static List<Long> ids(List<? extends Object> rows) {
        List<Long> ids = new ArrayList<Long>();
        for (Object row : rows) {
            if (row instanceof JenisSPMI) ids.add(((JenisSPMI) row).getId());
            else if (row instanceof StandarSPMI) ids.add(((StandarSPMI) row).getId());
            else if (row instanceof ButirMutuSPMI) ids.add(((ButirMutuSPMI) row).getId());
            else if (row instanceof IndikatorSPMI) ids.add(((IndikatorSPMI) row).getId());
            else if (row instanceof SkenarioSPMI) ids.add(((SkenarioSPMI) row).getId());
        }
        return ids;
    }
    private static Map<Long, String> references(String prefix, List<Long> ids) { Map<Long, String> map = new HashMap<Long, String>(); int n = 1; for (Long id : ids) map.put(id, prefix + "-" + String.format("%05d", n++)); return map; }
    private static String ref(Map<Long, String> refs, Object parent) { Long id = null; if (parent instanceof JenisSPMI) id = ((JenisSPMI) parent).getId(); else if (parent instanceof StandarSPMI) id = ((StandarSPMI) parent).getId(); else if (parent instanceof ButirMutuSPMI) id = ((ButirMutuSPMI) parent).getId(); else if (parent instanceof IndikatorSPMI) id = ((IndikatorSPMI) parent).getId(); return id == null ? "" : refs.get(id); }
    private static Set<String> refs(List<MasterRow> rows) { Set<String> result = new HashSet<String>(); for (MasterRow row : rows) result.add(row.ref); return result; }
    private static Integer parseInteger(Cell cell, String location) { String value = cellText(cell); if (!notEmpty(value)) return null; try { return Integer.valueOf(value.replaceFirst("\\.0$", "")); } catch (Exception e) { throw new IllegalArgumentException(location + ": No Urut harus bilangan bulat."); } }
    private static Boolean parseActive(String value, String location) { if (!notEmpty(value) || "ya".equals(key(value)) || "true".equals(key(value)) || "1".equals(value.trim())) return true; if ("tidak".equals(key(value)) || "false".equals(key(value)) || "0".equals(value.trim())) return false; throw new IllegalArgumentException(location + ": Aktif harus Ya atau Tidak."); }
    private static String active(Boolean value) { return Boolean.FALSE.equals(value) ? "Tidak" : "Ya"; }
    private static boolean sameId(Long a, Long b) { return a != null && a.equals(b); }
    private static boolean same(String a, String b) { return key(a).equals(key(b)); }
    private static String key(String value) { return value == null ? "" : value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT); }
    private static boolean notEmpty(String value) { return value != null && value.trim().length() > 0; }
    private static String emptyToNull(String value) { return notEmpty(value) ? value.trim() : null; }
    private static String text(XSSFSheet sheet, int row, int col) { return sheet == null || sheet.getRow(row) == null ? "" : cellText(sheet.getRow(row).getCell(col)); }
    private static String cellText(Cell cell) { if (cell == null) return ""; try { if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) { double d = cell.getNumericCellValue(); return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d); } return cell.getStringCellValue().trim(); } catch (Exception e) { return cell.toString().trim(); } }
    private static void throwErrors(List<String> errors) { if (errors.isEmpty()) return; StringBuilder b = new StringBuilder("Upload SPMI Global dibatalkan:\n"); int limit = Math.min(10, errors.size()); for (int i = 0; i < limit; i++) b.append("- ").append(errors.get(i)).append('\n'); if (errors.size() > limit) b.append("- dan ").append(errors.size() - limit).append(" kesalahan lain."); throw new IllegalArgumentException(b.toString().trim()); }
    private static void title(XSSFSheet sh, Styles st, String value, int cols) { sh.addMergedRegion(new CellRangeAddress(0, 0, 0, cols - 1)); cell(sh, 0, 0, value).setCellStyle(st.title); }
    private static Cell cell(XSSFSheet sh, int r, int c, String value) { XSSFRow row = sh.getRow(r); if (row == null) row = sh.createRow(r); Cell cell = row.getCell(c); if (cell == null) cell = row.createCell(c); cell.setCellValue(value == null ? "" : value); return cell; }
    private static void put(Row row, int col, Object value, CellStyle style) { Cell cell = row.createCell(col); if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue()); else cell.setCellValue(value == null ? "" : String.valueOf(value)); cell.setCellStyle(style); }
    private static void raw(XSSFSheet sh, int row, String label, Object value) { XSSFRow r = sh.createRow(row); r.createCell(0).setCellValue(label); if (value instanceof Number) r.createCell(1).setCellValue(((Number) value).doubleValue()); else r.createCell(1).setCellValue(value == null ? "" : String.valueOf(value)); }
    private static void widths(XSSFSheet sh, int[] widths) { for (int i = 0; i < widths.length; i++) sh.setColumnWidth(i, widths[i] * 256); }
    private static void addListValidation(XSSFSheet sh, int col, int first, int last, String[] values) { DataValidationHelper helper = new XSSFDataValidationHelper(sh); DataValidationConstraint constraint = helper.createExplicitListConstraint(values); DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(first, last, col, col)); validation.setShowErrorBox(true); validation.createErrorBox("Nilai tidak valid", "Pilih Ya atau Tidak."); sh.addValidationData(validation); }

    private static final class MasterRow { String location, ref, parentRef, businessCode, name, description; Integer order; Boolean active; }
    private static final class Counter { int inserted; int updated; }

    public static final class ImportResult {
        private final int jenis, standar, butir, indikator, skenario, inserted, updated;
        ImportResult(int jenis, int standar, int butir, int indikator, int skenario, int inserted, int updated) { this.jenis = jenis; this.standar = standar; this.butir = butir; this.indikator = indikator; this.skenario = skenario; this.inserted = inserted; this.updated = updated; }
        public String message() { return "Upload SPMI Global berhasil. Jenis: " + jenis + ", standar: " + standar + ", butir: " + butir + ", indikator: " + indikator + ", skenario/bukti: " + skenario + ". Data baru: " + inserted + ", diperbarui: " + updated + "."; }
        public int getInserted() { return inserted; } public int getUpdated() { return updated; }
    }

    private static final class Styles {
        final XSSFCellStyle title, header, body, input, inputCenter, note;
        Styles(XSSFWorkbook wb) {
            XSSFFont white = wb.createFont(); white.setBold(true); white.setColor(IndexedColors.WHITE.getIndex());
            XSSFFont dark = wb.createFont(); dark.setBold(true); dark.setColor(IndexedColors.DARK_BLUE.getIndex());
            XSSFFont normal = wb.createFont();
            title = style(wb, white, IndexedColors.DARK_BLUE); title.setAlignment(CellStyle.ALIGN_CENTER);
            header = style(wb, white, IndexedColors.TEAL); header.setAlignment(CellStyle.ALIGN_CENTER);
            body = style(wb, normal, IndexedColors.WHITE); input = style(wb, normal, IndexedColors.LIGHT_YELLOW);
            inputCenter = style(wb, normal, IndexedColors.LIGHT_YELLOW); inputCenter.setAlignment(CellStyle.ALIGN_CENTER);
            note = style(wb, dark, IndexedColors.LIGHT_CORNFLOWER_BLUE);
        }
        private static XSSFCellStyle style(XSSFWorkbook wb, Font font, IndexedColors fill) { XSSFCellStyle st = wb.createCellStyle(); st.setFont(font); st.setFillForegroundColor(fill.getIndex()); st.setFillPattern(CellStyle.SOLID_FOREGROUND); st.setBorderBottom(CellStyle.BORDER_THIN); st.setBorderTop(CellStyle.BORDER_THIN); st.setBorderLeft(CellStyle.BORDER_THIN); st.setBorderRight(CellStyle.BORDER_THIN); st.setVerticalAlignment(CellStyle.VERTICAL_TOP); st.setWrapText(true); return st; }
    }
}
