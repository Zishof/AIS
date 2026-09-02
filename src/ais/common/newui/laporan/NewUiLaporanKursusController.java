package ais.common.newui.laporan;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Row;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;

/** Kontrak native laporan Pendapatan Kursus berformat XLSX. */
public final class NewUiLaporanKursusController {

    private static final String MODULE = "root/report";
    private static final String SQL =
            "select to_char(c.waktubeli, 'DD-MM-YYYY') as tanggal, "
            + "max(d.nama) as userid, count(distinct(c1.peserta_kursus)) as jumlah, "
            + "coalesce(sum(e.hargatotal), 0) as hargatotal "
            + "from peserta_punya_produk_kursus c "
            + "inner join produk_peserta c1 on c1.peserta_punya_produk_kursus = c.id "
            + "inner join peserta_kursus d on d.id = c1.peserta_kursus "
            + "inner join produk_kursus e on e.id = c1.produk_kursus "
            + "where date(c.waktubeli) between :mulai and :sampai "
            + "group by d.id, to_char(c.waktubeli, 'DD-MM-YYYY') "
            + "order by max(c.waktubeli), d.id";

    private NewUiLaporanKursusController() { }

    /** Layani metadata dan ekspor laporan; keduanya baca-saja. */
    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        String action = text(request.getParameter("action"), "meta");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            if ("meta".equals(action)) meta(json, request);
            else if ("export".equals(action)) export(json, request);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Laporan Pendapatan Kursus gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanKursusController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject json, HttpServletRequest request) throws Exception {
        Date sampai = ais.ui.util.WaktuUtil.getDate();
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.setTime(sampai);
        c.add(Calendar.WEEK_OF_YEAR, -1);
        JSONArray filter = new JSONArray();
        filter.put(new JSONObject().put("nama", "mulai").put("label", "Tanggal Mulai")
                .put("tipe", "tanggal").put("wajib", true));
        filter.put(new JSONObject().put("nama", "sampai").put("label", "Tanggal Sampai")
                .put("tipe", "tanggal").put("wajib", true));
        json.put("judul", "Pendapatan Kursus")
                .put("format", "xlsx")
                .put("filter", filter)
                .put("mulaiBawaan", format(c.getTime()))
                .put("sampaiBawaan", format(sampai))
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("bolehUbah", false)
                .put("catatan", "Rekap pembelian kursus per peserta dan tanggal.");
    }

    private static void export(JSONObject json, HttpServletRequest request) throws Exception {
        Date mulai = tanggal(request.getParameter("mulai"), "Tanggal mulai");
        Date sampai = tanggal(request.getParameter("sampai"), "Tanggal sampai");
        if (mulai.after(sampai)) {
            throw new IllegalArgumentException("Tanggal mulai tidak boleh melewati tanggal sampai.");
        }
        List<Object[]> rows = ambil(mulai, sampai);
        byte[] isi = buatXlsx(rows);
        json.put("format", "xlsx")
                .put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .put("namaFile", "REKAP_PENDAPATAN_" + format(sampai) + ".xlsx")
                .put("fileBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> ambil(Date mulai, Date sampai) {
        Session session = HibernateUtil.openSession();
        try {
            SQLQuery query = session.createSQLQuery(SQL);
            query.setDate("mulai", mulai);
            query.setDate("sampai", sampai);
            return query.list();
        } finally {
            session.close();
        }
    }

    /** Bangun workbook yang mempertahankan kolom dan subtotal layar ZK lama. */
    static byte[] buatXlsx(List<Object[]> sumber) throws Exception {
        List<Object[]> rows = sumber == null ? new ArrayList<Object[]>() : sumber;
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Rekap Pendapatan");
        CellStyle title = style(wb, true, false);
        CellStyle head = style(wb, true, true);
        CellStyle body = style(wb, false, true);
        CellStyle bold = style(wb, true, true);

        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REKAPITULASI PEMBALIAN");
        titleCell.setCellStyle(title);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        String[] headers = { "Tanggal", "Dibeli oleh", "Jumlah Barang", "Tagihan",
                "Jumlah Anggota\nPer Tanggal", "Tagihan Denda\nPer Tanggal",
                "Denda Dibayar\nPer Tanggal" };
        Row header = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(head);
        }

        int rowIndex = 3;
        String tanggal = "";
        int jumlahTotal = 0;
        int jumlahPerTanggal = 0;
        double tagihanTotal = 0.0;
        double tagihanPerTanggal = 0.0;
        for (Object[] data : rows) {
            String tanggalBaru = nilai(data, 0);
            boolean tanggalBerubah = !tanggal.equals(tanggalBaru);
            if (tanggalBerubah) {
                if (rowIndex != 3) subtotal(sheet.getRow(rowIndex - 1), bold,
                        jumlahPerTanggal, tagihanPerTanggal);
                tanggal = tanggalBaru;
                jumlahPerTanggal = 0;
                tagihanPerTanggal = 0.0;
            }
            Row row = sheet.createRow(rowIndex++);
            isi(row, 0, tanggalBerubah ? tanggalBaru : "", body);
            isi(row, 1, nilai(data, 1), body);
            int jumlah = angka(data, 2).intValue();
            double tagihan = angka(data, 3).doubleValue();
            isi(row, 2, jumlah, body);
            isi(row, 3, tagihan, body);
            isi(row, 4, "", body);
            isi(row, 5, "", body);
            isi(row, 6, "", body);
            jumlahTotal += jumlah;
            jumlahPerTanggal += jumlah;
            tagihanTotal += tagihan;
            tagihanPerTanggal += tagihan;
        }
        if (rowIndex != 3) {
            subtotal(sheet.getRow(rowIndex - 1), bold, jumlahPerTanggal, tagihanPerTanggal);
        }
        Row total = sheet.createRow(rowIndex);
        for (int i = 0; i < 7; i++) isi(total, i, "", bold);
        total.getCell(0).setCellValue("TOTAL");
        total.getCell(4).setCellValue((double) jumlahTotal);
        total.getCell(5).setCellValue(tagihanTotal);

        int[] widths = { 16, 28, 18, 18, 22, 22, 22 };
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            wb.write(out);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    private static void subtotal(Row row, CellStyle style, int jumlah, double tagihan) {
        isi(row, 4, jumlah, style);
        isi(row, 5, tagihan, style);
    }

    private static CellStyle style(XSSFWorkbook wb, boolean tebal, boolean border) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        if (tebal) font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font);
        style.setWrapText(true);
        if (border) {
            style.setBorderTop(XSSFCellStyle.BORDER_THIN);
            style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
            style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
            style.setBorderRight(XSSFCellStyle.BORDER_THIN);
        }
        return style;
    }

    private static void isi(Row row, int kolom, Object nilai, CellStyle style) {
        Cell cell = row.getCell(kolom);
        if (cell == null) cell = row.createCell(kolom);
        if (nilai instanceof Number) cell.setCellValue(((Number) nilai).doubleValue());
        else cell.setCellValue(nilai == null ? "" : nilai.toString());
        cell.setCellStyle(style);
    }

    private static String nilai(Object[] row, int i) {
        return row == null || i >= row.length || row[i] == null ? "" : row[i].toString();
    }

    private static Number angka(Object[] row, int i) {
        Object value = row == null || i >= row.length ? null : row[i];
        if (value instanceof Number) return (Number) value;
        try { return value == null ? Double.valueOf(0.0) : Double.valueOf(value.toString()); }
        catch (Exception e) { return Double.valueOf(0.0); }
    }

    static Date tanggal(String value, String label) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(label + " belum diisi.");
        }
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            f.setLenient(false);
            return f.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " harus berformat yyyy-MM-dd.");
        }
    }

    private static String format(Date value) {
        return Common.databaseDateFormat.get().format(value);
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static void fail(JSONObject json, String code, String message) throws Exception {
        json.put("ok", false).put("code", code).put("message", text(message, code));
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
