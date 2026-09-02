package ais.common.newui.laporan;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;

/** Kontrak native baca-saja untuk lima tab laporan denda perpustakaan. */
public final class NewUiLaporanDendaController {

    private static final String MODULE = "root/report";
    static final String REKAP_ANGGOTA = "Rekap Denda Anggota Per Tanggal";
    static final String REKAP_ITEM = "Rekap Denda Item Per Tanggal";
    static final String DATA_ANGGOTA = "Data Denda Anggota";
    static final String DATA_ITEM = "Data Denda Per Item";
    static final String BELUM_LUNAS = "Denda Belum Lunas";

    private NewUiLaporanDendaController() { }

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
            else if ("lookup".equals(action)) lookup(json, request);
            else if ("export".equals(action)) export(json, request);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Laporan Denda gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanDendaController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject json, HttpServletRequest request) throws Exception {
        Date sampai = ais.ui.util.WaktuUtil.getDate();
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.setTime(sampai);
        c.add(Calendar.WEEK_OF_YEAR, -1);

        JSONArray jenis = new JSONArray();
        jenis.put(REKAP_ANGGOTA).put(REKAP_ITEM).put(DATA_ANGGOTA)
                .put(DATA_ITEM).put(BELUM_LUNAS);
        JSONArray filter = new JSONArray();
        filter.put(new JSONObject().put("nama", "jenis").put("label", "Jenis Laporan")
                .put("tipe", "pilihan").put("wajib", true).put("opsi", jenis)
                .put("bawaan", REKAP_ANGGOTA));
        filter.put(new JSONObject().put("nama", "perpustakaan").put("label", "Perpustakaan")
                .put("tipe", "relasi").put("wajib", false)
                .put("entity", Perpustakaan.class.getName()));
        filter.put(new JSONObject().put("nama", "mulai").put("label", "Tanggal Mulai")
                .put("tipe", "tanggal").put("wajib", true));
        filter.put(new JSONObject().put("nama", "sampai").put("label", "Tanggal Sampai")
                .put("tipe", "tanggal").put("wajib", true));

        json.put("judul", "Laporan Denda")
                .put("format", "xlsx")
                .put("filter", filter)
                .put("mulaiBawaan", databaseDate(c.getTime()))
                .put("sampaiBawaan", databaseDate(sampai))
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("bolehUbah", false)
                .put("catatan", "Lima tab laporan lama tersedia melalui pilihan Jenis Laporan.");
    }

    @SuppressWarnings("unchecked")
    private static void lookup(JSONObject json, HttpServletRequest request) throws Exception {
        if (!"perpustakaan".equals(text(request.getParameter("filter"), ""))) {
            throw new IllegalArgumentException("Filter relasi tidak dikenal.");
        }
        String q = text(request.getParameter("q"), "");
        JSONArray pilihan = new JSONArray();
        Session session = HibernateUtil.openSession();
        try {
            Criteria criteria = session.createCriteria(Perpustakaan.class).setMaxResults(50);
            if (q.length() >= 2) criteria.add(Restrictions.ilike("nama", "%" + q + "%"));
            criteria.addOrder(Order.asc("nama"));
            for (Perpustakaan p : (List<Perpustakaan>) criteria.list()) {
                pilihan.put(new JSONObject().put("id", p.getId()).put("nama", p.getNama()));
            }
        } finally { session.close(); }
        json.put("filter", "perpustakaan").put("pilihan", pilihan)
                .put("total", pilihan.length()).put("batas", 50);
    }

    private static void export(JSONObject json, HttpServletRequest request) throws Exception {
        String jenis = jenis(text(request.getParameter("jenis"), ""));
        Date mulai = tanggal(request.getParameter("mulai"), "Tanggal mulai");
        Date sampai = tanggal(request.getParameter("sampai"), "Tanggal sampai");
        if (mulai.after(sampai)) {
            throw new IllegalArgumentException("Tanggal mulai tidak boleh melewati tanggal sampai.");
        }
        Long perpustakaanId = idOpsional(request.getParameter("perpustakaan"));

        byte[] bytes;
        if (REKAP_ANGGOTA.equals(jenis) || REKAP_ITEM.equals(jenis)) {
            bytes = eksporRekap(jenis, perpustakaanId, mulai, sampai);
        } else {
            bytes = eksporData(jenis, perpustakaanId, mulai, sampai);
        }
        json.put("format", "xlsx")
                .put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .put("namaFile", namaFile(jenis))
                .put("fileBase64", java.util.Base64.getEncoder().encodeToString(bytes));
    }

    private static byte[] eksporRekap(String jenis, Long perpustakaanId,
            Date mulai, Date sampai) throws Exception {
        Session session = HibernateUtil.openSession();
        try {
            Perpustakaan p = perpustakaan(session, perpustakaanId);
            boolean perItem = REKAP_ITEM.equals(jenis);
            List<Object[]> perTanggal = ambilRekap(session, perpustakaanId, mulai, sampai,
                    perItem, true);
            List<Object[]> keseluruhan = ambilRekap(session, perpustakaanId, mulai, sampai,
                    perItem, false);
            String judul = (perItem ? "REKAPITULASI DENDA ITEM" : "REKAPITULASI DENDA ANGGOTA")
                    + "\n " + labelPerpustakaan(p);
            return buatRekapXlsx(perTanggal, keseluruhan, judul,
                    perItem ? "Jumlah Item" : "Jumlah Anggota");
        } finally { session.close(); }
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> ambilRekap(Session session, Long perpustakaanId,
            Date mulai, Date sampai, boolean perItem, boolean perTanggal) {
        String hitung = perItem ? "count(distinct(c1.item))" : "count(distinct(e.anggota))";
        String selectTanggal = perTanggal
                ? "to_char(c1.tanggal, 'DD-MM-YYYY') as tanggal, " : "";
        String sql = "select " + selectTanggal
                + "max(d.usernama || ' (' || d.userid || ')') as userid, "
                + hitung + " as jumlah, coalesce(sum(c1.denda), 0) as denda, "
                + "coalesce(sum(c1.dibayarsejumlah), 0) as dibayarsejumlah "
                + "from library.kembali_pengadaan_item c "
                + "inner join library.kembali_pengadaan_item_detail c1 "
                + "on c1.kembali_pengadaan_item = c.id "
                + "inner join tbmuser d on d.userid = c.disetujui_oleh "
                + "inner join library.peminjaman_pengadaan_item e "
                + "on c.peminjaman_pengadaan_item = e.id "
                + "where date(c1.tanggal) between :mulai and :sampai "
                + (perpustakaanId == null ? "" : "and c.perpustakaan = :perpustakaan ")
                + (perTanggal
                        ? "group by d.userid, to_char(c1.tanggal, 'DD-MM-YYYY') "
                                + "order by max(c.tanggal_pembuatan), d.userid"
                        : "group by d.userid order by d.userid");
        SQLQuery query = session.createSQLQuery(sql);
        query.setDate("mulai", mulai);
        query.setDate("sampai", sampai);
        if (perpustakaanId != null) query.setLong("perpustakaan", perpustakaanId.longValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static byte[] eksporData(String jenis, Long perpustakaanId,
            Date mulai, Date sampai) throws Exception {
        Session session = HibernateUtil.openSession();
        try {
            Perpustakaan p = perpustakaan(session, perpustakaanId);
            Criteria criteria = session.createCriteria(KembaliPengadaanItemDetail.class)
                    .add(Restrictions.gt("denda", Double.valueOf(0.01)))
                    .add(Restrictions.ge("tanggal", awalHari(mulai)))
                    .add(Restrictions.lt("tanggal", hariSetelah(sampai)))
                    .createAlias("kembaliPengadaanItem", "kembaliPengadaanItem")
                    .add(Restrictions.isNotNull("kembaliPengadaanItem.disetujuiOleh"));
            if (p != null) criteria.add(Restrictions.eq("kembaliPengadaanItem.perpustakaan", p));
            if (BELUM_LUNAS.equals(jenis)) {
                criteria.add(Restrictions.sqlRestriction("this_.denda > this_.dibayarsejumlah"));
            }
            List<KembaliPengadaanItemDetail> details = criteria.setMaxResults(1048576)
                    .addOrder(Order.desc("kembaliPengadaanItem.id"))
                    .addOrder(Order.desc("id")).list();
            if (DATA_ANGGOTA.equals(jenis)) return dataAnggota(details, p);
            if (DATA_ITEM.equals(jenis)) return dataItem(details, p, false);
            return dataItem(details, p, true);
        } finally { session.close(); }
    }

    private static byte[] dataAnggota(List<KembaliPengadaanItemDetail> details,
            Perpustakaan p) throws Exception {
        List<Object[]> rows = new ArrayList<Object[]>();
        Long parentId = null;
        List<KembaliPengadaanItemDetail> kelompok = new ArrayList<KembaliPengadaanItemDetail>();
        for (KembaliPengadaanItemDetail d : details) {
            Long id = d.getKembaliPengadaanItem().getId();
            if (parentId != null && !parentId.equals(id)) {
                rows.add(barisAnggota(kelompok));
                kelompok.clear();
            }
            parentId = id;
            kelompok.add(d);
        }
        if (!kelompok.isEmpty()) rows.add(barisAnggota(kelompok));
        String[] header = { "Perpustakaan", "Anggota", "Prodi", "Waktu/Hari/Tanggal",
                "Denda", "Dibayar", "Kode Pengembalian", "Di-input oleh",
                "Item yang dipinjam" };
        return buatDataXlsx("DATA DENDA ANGGOTA\n " + labelPerpustakaan(p), header,
                rows, 3, 4, 5);
    }

    private static Object[] barisAnggota(List<KembaliPengadaanItemDetail> kelompok) {
        KembaliPengadaanItemDetail pertama = kelompok.get(0);
        KembaliPengadaanItem kembali = pertama.getKembaliPengadaanItem();
        double denda = 0.0, dibayar = 0.0;
        String items = "";
        for (KembaliPengadaanItemDetail d : kelompok) {
            denda += nominal(d.getDenda());
            dibayar += nominal(d.getDibayarSejumlah());
            String item = labelItem(d) + "=>Denda: " + Common.numberFormat.get().format(nominal(d.getDenda()))
                    + ", Dibayar: " + Common.numberFormat.get().format(nominal(d.getDibayarSejumlah()));
            items += items.length() == 0 ? item : " ," + item;
        }
        return new Object[] { nama(kembali.getPerpustakaan()), anggota(kembali), prodi(kembali),
                tampilTanggal(pertama.getTanggal()), denda, dibayar, kembali.getKode(),
                pengguna(kembali), items };
    }

    private static byte[] dataItem(List<KembaliPengadaanItemDetail> details,
            Perpustakaan p, boolean belumLunas) throws Exception {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (KembaliPengadaanItemDetail d : details) {
            KembaliPengadaanItem kembali = d.getKembaliPengadaanItem();
            if (belumLunas) {
                rows.add(new Object[] { nama(kembali.getPerpustakaan()), anggota(kembali),
                        prodi(kembali), tampilTanggal(d.getTanggal()), labelItem(d),
                        nominal(d.getDenda()), nominal(d.getDibayarSejumlah()),
                        kembali.getKode(), pengguna(kembali) });
            } else {
                rows.add(new Object[] { nama(kembali.getPerpustakaan()), anggota(kembali),
                        prodi(kembali), kembali.getPeminjamanPengadaanItem() == null ? ""
                                : tampilTanggal(kembali.getPeminjamanPengadaanItem().getTanggalPembuatan()),
                        kembali.getPeminjamanPengadaanItem() == null ? ""
                                : Common.numberFormat.get().format(
                                        kembali.getPeminjamanPengadaanItem().getJumlahHariBatas()),
                        tampilTanggal(d.getTanggal()), labelItem(d), nominal(d.getDenda()),
                        nominal(d.getDibayarSejumlah()), kembali.getKode(), pengguna(kembali) });
            }
        }
        if (belumLunas) {
            String[] header = { "Perpustakaan", "Anggota", "Prodi", "Waktu/Hari/Tanggal",
                    "Item / Buku", "Denda", "Dibayar", "Kode Pengembalian", "Di-input oleh" };
            return buatDataXlsx("DATA DENDA ANGGOTA PER ITEM\n " + labelPerpustakaan(p),
                    header, rows, 4, 5, 6);
        }
        String[] header = { "Perpustakaan", "Anggota", "Prodi", "Waktu Dipinjam",
                "Harus Kembali (hari)", "Waktu Kembali", "Item / Buku", "Denda",
                "Dibayar", "Kode Pengembalian", "Di-input oleh" };
        return buatDataXlsx("DATA DENDA ANGGOTA PER ITEM\n " + labelPerpustakaan(p),
                header, rows, 6, 7, 8);
    }

    /** Workbook rekap dua tingkat: per tanggal dan rekap total per pengguna. */
    static byte[] buatRekapXlsx(List<Object[]> harian, List<Object[]> rekap,
            String judul, String labelJumlah) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Rekap Denda");
        Styles s = new Styles(wb);
        judul(sheet, judul, 7, s);
        String[] header = { "Tanggal", "Di-input oleh", labelJumlah, "Tagihan Denda",
                "Denda Dibayar", labelJumlah + "\nPer Tanggal",
                "Tagihan Denda\nPer Tanggal", "Denda Dibayar\nPer Tanggal" };
        header(sheet, 2, header, s);

        int rowIndex = 3;
        String tanggal = "";
        int jumlahTotal = 0, jumlahTanggal = 0;
        double dendaTotal = 0.0, bayarTotal = 0.0, dendaTanggal = 0.0, bayarTanggal = 0.0;
        for (Object[] data : safe(harian)) {
            String tanggalBaru = value(data, 0);
            boolean baru = !tanggal.equals(tanggalBaru);
            if (baru) {
                if (rowIndex != 3) subtotal(sheet.getRow(rowIndex - 1), s.bold,
                        jumlahTanggal, dendaTanggal, bayarTanggal);
                tanggal = tanggalBaru;
                jumlahTanggal = 0; dendaTanggal = 0.0; bayarTanggal = 0.0;
            }
            Row row = sheet.createRow(rowIndex++);
            cell(row, 0, baru ? tanggalBaru : "", s.body);
            cell(row, 1, value(data, 1), s.body);
            int jumlah = number(data, 2).intValue();
            double denda = number(data, 3).doubleValue();
            double bayar = number(data, 4).doubleValue();
            cell(row, 2, jumlah, s.body); cell(row, 3, denda, s.body);
            cell(row, 4, bayar, s.body); cell(row, 5, "", s.body);
            cell(row, 6, "", s.body); cell(row, 7, "", s.body);
            jumlahTotal += jumlah; jumlahTanggal += jumlah;
            dendaTotal += denda; dendaTanggal += denda;
            bayarTotal += bayar; bayarTanggal += bayar;
        }
        if (rowIndex != 3) subtotal(sheet.getRow(rowIndex - 1), s.bold,
                jumlahTanggal, dendaTanggal, bayarTanggal);
        Row total = sheet.createRow(rowIndex++);
        blank(total, 8, s.bold); total.getCell(0).setCellValue("TOTAL");
        total.getCell(5).setCellValue((double) jumlahTotal);
        total.getCell(6).setCellValue(dendaTotal); total.getCell(7).setCellValue(bayarTotal);

        rowIndex += 2;
        Row rekapTitle = sheet.createRow(rowIndex++);
        cell(rekapTitle, 0, "REKAP TOTAL", s.title);
        sheet.addMergedRegion(new CellRangeAddress(rekapTitle.getRowNum(),
                rekapTitle.getRowNum(), 0, 7));
        header(sheet, rowIndex++, new String[] { "Di-input oleh", labelJumlah,
                "Tagihan Denda", "Denda Dibayar" }, s);
        jumlahTotal = 0; dendaTotal = 0.0; bayarTotal = 0.0;
        for (Object[] data : safe(rekap)) {
            Row row = sheet.createRow(rowIndex++);
            cell(row, 0, value(data, 0), s.body);
            int jumlah = number(data, 1).intValue();
            double denda = number(data, 2).doubleValue();
            double bayar = number(data, 3).doubleValue();
            cell(row, 1, jumlah, s.body); cell(row, 2, denda, s.body);
            cell(row, 3, bayar, s.body);
            jumlahTotal += jumlah; dendaTotal += denda; bayarTotal += bayar;
        }
        total = sheet.createRow(rowIndex);
        blank(total, 4, s.bold); total.getCell(0).setCellValue("TOTAL");
        total.getCell(1).setCellValue((double) jumlahTotal);
        total.getCell(2).setCellValue(dendaTotal); total.getCell(3).setCellValue(bayarTotal);
        widths(sheet, new int[] { 18, 28, 18, 18, 18, 23, 23, 23 });
        return bytes(wb);
    }

    /** Workbook rincian; tiga kolom terakhir yang diberikan memuat label dan total. */
    static byte[] buatDataXlsx(String judul, String[] headers, List<Object[]> rows,
            int labelTotalKolom, int dendaKolom, int bayarKolom) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Data Denda");
        Styles s = new Styles(wb);
        judul(sheet, judul, headers.length - 1, s);
        header(sheet, 2, headers, s);
        int rowIndex = 3;
        double denda = 0.0, bayar = 0.0;
        for (Object[] data : safe(rows)) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) cell(row, i,
                    i < data.length ? data[i] : "", s.body);
            denda += number(data, dendaKolom).doubleValue();
            bayar += number(data, bayarKolom).doubleValue();
        }
        Row total = sheet.createRow(rowIndex);
        blank(total, headers.length, s.bold);
        total.getCell(labelTotalKolom).setCellValue("Denda Total");
        total.getCell(dendaKolom).setCellValue(denda);
        total.getCell(bayarKolom).setCellValue(bayar);
        int[] lebar = new int[headers.length];
        for (int i = 0; i < lebar.length; i++) lebar[i] = 20;
        if (lebar.length > 1) lebar[1] = 30;
        if (lebar.length > labelTotalKolom) lebar[labelTotalKolom] = 30;
        widths(sheet, lebar);
        return bytes(wb);
    }

    private static void judul(Sheet sheet, String teks, int akhir, Styles s) {
        Row row = sheet.createRow(1);
        cell(row, 0, teks, s.title);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, akhir));
    }

    private static void header(Sheet sheet, int index, String[] labels, Styles s) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < labels.length; i++) cell(row, i, labels[i], s.head);
    }

    private static void subtotal(Row row, CellStyle style, int jumlah, double denda, double bayar) {
        cell(row, 5, jumlah, style); cell(row, 6, denda, style); cell(row, 7, bayar, style);
    }

    private static void blank(Row row, int count, CellStyle style) {
        for (int i = 0; i < count; i++) cell(row, i, "", style);
    }

    private static void cell(Row row, int index, Object value, CellStyle style) {
        Cell cell = row.getCell(index);
        if (cell == null) cell = row.createCell(index);
        if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value == null ? "" : value.toString());
        cell.setCellStyle(style);
    }

    private static void widths(Sheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private static byte[] bytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try { wb.write(out); return out.toByteArray(); }
        finally { out.close(); }
    }

    private static final class Styles {
        final CellStyle title, head, body, bold;
        Styles(XSSFWorkbook wb) {
            title = style(wb, true, false); title.setAlignment(CellStyle.ALIGN_CENTER);
            head = style(wb, true, true); body = style(wb, false, true);
            bold = style(wb, true, true);
        }
    }

    private static CellStyle style(XSSFWorkbook wb, boolean bold, boolean border) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        if (bold) font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font); style.setWrapText(true);
        if (border) {
            style.setBorderTop(XSSFCellStyle.BORDER_THIN);
            style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
            style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
            style.setBorderRight(XSSFCellStyle.BORDER_THIN);
        }
        return style;
    }

    private static Perpustakaan perpustakaan(Session session, Long id) {
        if (id == null) return null;
        Perpustakaan p = (Perpustakaan) session.get(Perpustakaan.class, id);
        if (p == null) throw new IllegalArgumentException("Perpustakaan tidak ditemukan.");
        return p;
    }

    private static String labelPerpustakaan(Perpustakaan p) {
        return p == null ? "SEMUA PERPUSTAKAAN" : "PERPUSTAKAAN " + nama(p).toUpperCase();
    }

    private static String anggota(KembaliPengadaanItem kembali) {
        return kembali == null || kembali.getPeminjamanPengadaanItem() == null
                || kembali.getPeminjamanPengadaanItem().getAnggota() == null ? ""
                : kembali.getPeminjamanPengadaanItem().getAnggota().toString();
    }

    private static String prodi(KembaliPengadaanItem kembali) {
        if (kembali == null || kembali.getPeminjamanPengadaanItem() == null
                || kembali.getPeminjamanPengadaanItem().getAnggota() == null) return "";
        ais.database.model.library.Anggota a = kembali.getPeminjamanPengadaanItem().getAnggota();
        if (a.getMahasiswa() != null && a.getMahasiswa().getJurusan() != null)
            return nama(a.getMahasiswa().getJurusan());
        if (a.getDosen() != null && a.getDosen().getJurusan() != null)
            return nama(a.getDosen().getJurusan());
        if (a.getSiswa() != null && a.getSiswa().getSekolah() != null)
            return nama(a.getSiswa().getSekolah());
        if (a.getGuru() != null && a.getGuru().getSekolah() != null)
            return nama(a.getGuru().getSekolah());
        return "";
    }

    private static String pengguna(KembaliPengadaanItem kembali) {
        return kembali == null || kembali.getDisetujuiOleh() == null ? ""
                : text(kembali.getDisetujuiOleh().getUserNama(), "") + " ("
                        + text(kembali.getDisetujuiOleh().getUserId(), "") + ")";
    }

    private static String labelItem(KembaliPengadaanItemDetail detail) {
        String item = detail == null || detail.getItem() == null ? "" : nama(detail.getItem());
        return detail == null || detail.getItemPunyaBarcode() == null
                ? item : text(detail.getItemPunyaBarcode().getBarcode(), "") + "-" + item;
    }

    private static String nama(Object o) {
        if (o == null) return "";
        try { return text(String.valueOf(o.getClass().getMethod("getNama").invoke(o)), ""); }
        catch (Exception e) { return ""; }
    }

    private static double nominal(Number n) { return n == null ? 0.0 : n.doubleValue(); }
    private static String tampilTanggal(Date d) { return d == null ? "" : Common.dateFormat5.get().format(d); }

    private static Date awalHari(Date value) {
        Calendar c = Calendar.getInstance(); c.setTime(value);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date hariSetelah(Date value) {
        Calendar c = Calendar.getInstance(); c.setTime(awalHari(value));
        c.add(Calendar.DAY_OF_MONTH, 1); return c.getTime();
    }

    static String jenis(String value) {
        if (REKAP_ANGGOTA.equals(value) || REKAP_ITEM.equals(value)
                || DATA_ANGGOTA.equals(value) || DATA_ITEM.equals(value)
                || BELUM_LUNAS.equals(value)) return value;
        throw new IllegalArgumentException("Jenis laporan tidak dikenal.");
    }

    static Date tanggal(String value, String label) {
        if (value == null || value.trim().length() == 0)
            throw new IllegalArgumentException(label + " belum diisi.");
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            f.setLenient(false); return f.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " harus berformat yyyy-MM-dd.");
        }
    }

    private static Long idOpsional(String value) {
        if (value == null || value.trim().length() == 0) return null;
        try { Long id = Long.valueOf(value.trim()); if (id.longValue() > 0) return id; }
        catch (Exception ignored) { }
        throw new IllegalArgumentException("Perpustakaan tidak valid.");
    }

    private static String namaFile(String jenis) {
        if (REKAP_ANGGOTA.equals(jenis)) return "REKAP_DENDA_ANGGOTA.xlsx";
        if (REKAP_ITEM.equals(jenis)) return "REKAP_DENDA_ITEM.xlsx";
        if (DATA_ANGGOTA.equals(jenis)) return "DATA_DENDA_ANGGOTA.xlsx";
        if (DATA_ITEM.equals(jenis)) return "DATA_DENDA_ITEM.xlsx";
        return "DATA_DENDA_BELUM_LUNAS.xlsx";
    }

    private static List<Object[]> safe(List<Object[]> rows) {
        return rows == null ? new ArrayList<Object[]>() : rows;
    }

    private static String value(Object[] row, int index) {
        return row == null || index >= row.length || row[index] == null ? "" : row[index].toString();
    }

    private static Number number(Object[] row, int index) {
        Object value = row == null || index >= row.length ? null : row[index];
        if (value instanceof Number) return (Number) value;
        try { return value == null ? Double.valueOf(0.0) : Double.valueOf(value.toString()); }
        catch (Exception e) { return Double.valueOf(0.0); }
    }

    private static String databaseDate(Date value) { return Common.databaseDateFormat.get().format(value); }
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
