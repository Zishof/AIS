package ais.common.newui.laporan;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Row;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Ruang;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;

/** Kontrak native baca-saja untuk Laporan Rekap Penilaian Mahasiswa. */
public final class NewUiLaporanRekapPenilaianController {

    private static final String MODULE = "root/report";
    private static final String SEMUA = "Semua";
    private static final String[] HEADER = { "KODE MATAKULIAH", "NAMA MATAKULIAH",
            "HARI", "WAKTU", "DOSEN", "RUANGAN", "SEMESTER", "SUDAH DINILAI",
            "BELUM DINILAI", "PROSENTASE", "TERAKHIR INPUT", "TOTAL IPS",
            "RATA-RATA IPS", "RATA-RATA NILAI" };

    private NewUiLaporanRekapPenilaianController() { }

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
                write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, request, user);
            else if ("lookup".equals(action)) lookup(json, request, user);
            else if ("export".equals(action)) export(json, request, user);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Laporan Rekap Penilaian gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanRekapPenilaianController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject json, HttpServletRequest request, Tbmuser user)
            throws Exception {
        JSONArray filter = new JSONArray();
        filter.put(relasi("fakultas", "Fakultas", Fakultas.class, false, false));
        filter.put(relasi("jurusan", "Prodi", Jurusan.class, false, false)
                .put("tergantungPada", "fakultas"));
        filter.put(pilihan("tahunAkademik", "Tahun Akademik", tahunAkademik(),
                bawaanTahun(), true));
        filter.put(relasi("dosen", "Dosen", Dosen.class, false, true));
        filter.put(pilihan("jenisSemester", "Jenis Semester",
                new String[] { Perkuliahan.GANJIL, Perkuliahan.GENAP, Perkuliahan.SP, SEMUA },
                Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP, false));
        String[] semesters = new String[16]; semesters[0] = SEMUA;
        for (int i = 1; i < semesters.length; i++) semesters[i] = String.valueOf(i);
        filter.put(pilihan("semesterKe", "Semester ke", semesters, SEMUA, false));
        filter.put(relasi("program", "Program", Program.class, false, false));
        filter.put(relasi("jenisUjian", "Jenis Ujian (UTS/UAS)",
                StatusPertemuan.class, false, false));
        filter.put(relasi("masaPerkuliahan", "Masa Perkuliahan",
                MasaPerkuliahan.class, false, true));
        filter.put(bendera("belumDinilai", "Hanya yang belum dinilai"));
        filter.put(bendera("samaSekaliBelumDinilai", "Sama sekali belum dinilai"));
        filter.put(bendera("telahDinilai", "Hanya yang telah dinilai"));

        json.put("judul", "Laporan Rekap Penilaian")
                .put("format", "xlsx")
                .put("filter", filter)
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("bolehUbah", false)
                .put("catatan", "Rekap progres input nilai per kelas perkuliahan.");
    }

    private static JSONObject relasi(String nama, String label, Class<?> entity,
            boolean wajib, boolean cari) throws Exception {
        JSONObject o = new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "relasi").put("wajib", wajib).put("entity", entity.getName());
        if (cari) o.put("cari", true);
        return o;
    }

    private static JSONObject pilihan(String nama, String label, String[] values,
            String bawaan, boolean wajib) throws Exception {
        JSONArray opsi = new JSONArray();
        for (int i = 0; i < values.length; i++) opsi.put(values[i]);
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "pilihan").put("wajib", wajib).put("opsi", opsi)
                .put("bawaan", bawaan);
    }

    private static JSONObject bendera(String nama, String label) throws Exception {
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "bendera").put("wajib", false).put("bawaan", "0");
    }

    private static String[] tahunAkademik() {
        List<String> result = new ArrayList<String>();
        for (String value : Common.tahunAngkatans) result.add(value);
        if (result.isEmpty()) result.add(bawaanTahun());
        return result.toArray(new String[result.size()]);
    }

    private static String bawaanTahun() {
        String current = Common.getCurrentTahunAkademik();
        if (current != null && current.trim().length() > 0) return current;
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        return year + "/" + (year + 1);
    }

    @SuppressWarnings("unchecked")
    private static void lookup(JSONObject json, HttpServletRequest request, Tbmuser user)
            throws Exception {
        String filter = text(request.getParameter("filter"), "");
        String q = text(request.getParameter("q"), "");
        JSONArray pilihan = new JSONArray();
        Session session = HibernateUtil.openSession();
        try {
            if ("fakultas".equals(filter)) {
                Fakultas scope = user.ambilFakultas();
                Criteria c = session.createCriteria(Fakultas.class).setMaxResults(50);
                if (scope != null) c.add(Restrictions.eq("id", scope.getId()));
                if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
                c.addOrder(Order.asc("nama"));
                for (Fakultas row : (List<Fakultas>) c.list()) option(pilihan, row.getId(), row.getNama());
            } else if ("jurusan".equals(filter)) {
                Jurusan scope = user.ambilJurusan();
                Long fakultas = idOpsional(request.getParameter("fakultas"));
                Criteria c = session.createCriteria(Jurusan.class).setMaxResults(50);
                if (scope != null) c.add(Restrictions.eq("id", scope.getId()));
                else if (fakultas != null) c.createAlias("fakultas", "f")
                        .add(Restrictions.eq("f.id", fakultas));
                if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
                c.addOrder(Order.asc("nama"));
                for (Jurusan row : (List<Jurusan>) c.list()) option(pilihan, row.getId(), row.getNama());
            } else if ("dosen".equals(filter)) {
                Criteria c = session.createCriteria(Dosen.class).setMaxResults(50);
                if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
                c.addOrder(Order.asc("nama"));
                for (Dosen row : (List<Dosen>) c.list()) option(pilihan, row.getId(), row.getNama());
            } else if ("program".equals(filter)) {
                Program scope = user.ambilProgram();
                Criteria c = session.createCriteria(Program.class).setMaxResults(50);
                c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                if (scope != null) c.add(Restrictions.eq("nama", scope.getNama()));
                if (q.length() >= 2) c.add(Restrictions.ilike("namaBaru", "%" + q + "%"));
                c.addOrder(Order.asc("num")).addOrder(Order.asc("nama"));
                for (Program row : (List<Program>) c.list())
                    option(pilihan, row.getNama(), row.getNamaBaru());
            } else if ("jenisUjian".equals(filter)) {
                Criteria c = session.createCriteria(StatusPertemuan.class).setMaxResults(50)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
                for (StatusPertemuan row : (List<StatusPertemuan>) c.list())
                    option(pilihan, row.getId(), row.getNama());
            } else if ("masaPerkuliahan".equals(filter)) {
                Criteria c = session.createCriteria(MasaPerkuliahan.class).setMaxResults(50)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
                c.addOrder(Order.desc("mulai"));
                for (MasaPerkuliahan row : (List<MasaPerkuliahan>) c.list())
                    option(pilihan, row.getId(), row.getNama());
            } else {
                throw new IllegalArgumentException("Filter relasi tidak dikenal.");
            }
        } finally { session.close(); }
        json.put("filter", filter).put("pilihan", pilihan).put("total", pilihan.length())
                .put("batas", 50);
    }

    private static void option(JSONArray array, Object id, String nama) throws Exception {
        array.put(new JSONObject().put("id", id).put("nama", text(nama, "-")));
    }

    private static void export(JSONObject json, HttpServletRequest request, Tbmuser user)
            throws Exception {
        String tahun = pilihanTahun(text(request.getParameter("tahunAkademik"), ""));
        String jenisSemester = pilihanSemester(text(request.getParameter("jenisSemester"), SEMUA));
        Integer semesterKe = semester(text(request.getParameter("semesterKe"), SEMUA));
        boolean belum = flag(request.getParameter("belumDinilai"));
        boolean belumSemua = flag(request.getParameter("samaSekaliBelumDinilai"));
        boolean telah = flag(request.getParameter("telahDinilai"));

        Session session = HibernateUtil.openSession();
        try {
            Fakultas fakultas = entity(session, Fakultas.class,
                    idOpsional(request.getParameter("fakultas")));
            Jurusan jurusan = entity(session, Jurusan.class,
                    idOpsional(request.getParameter("jurusan")));
            Dosen dosen = entity(session, Dosen.class, idOpsional(request.getParameter("dosen")));
            StatusPertemuan status = entity(session, StatusPertemuan.class,
                    idOpsional(request.getParameter("jenisUjian")));
            MasaPerkuliahan masa = entity(session, MasaPerkuliahan.class,
                    idOpsional(request.getParameter("masaPerkuliahan")));
            String program = text(request.getParameter("program"), "");
            if (program.length() > 0 && session.get(Program.class, program) == null)
                throw new IllegalArgumentException("Program tidak ditemukan.");

            Fakultas scopeF = user.ambilFakultas();
            Jurusan scopeJ = user.ambilJurusan();
            Program scopeP = user.ambilProgram();
            if (scopeF != null) {
                if (fakultas != null && !scopeF.getId().equals(fakultas.getId())) forbiddenScope();
                fakultas = entity(session, Fakultas.class, scopeF.getId());
            }
            if (scopeJ != null) {
                if (jurusan != null && !scopeJ.getId().equals(jurusan.getId())) forbiddenScope();
                jurusan = entity(session, Jurusan.class, scopeJ.getId());
            }
            if (scopeP != null) {
                if (program.length() > 0 && !scopeP.getNama().equals(program)) forbiddenScope();
                program = scopeP.getNama();
            }
            if (fakultas != null && jurusan != null && jurusan.getFakultas() != null
                    && !fakultas.getId().equals(jurusan.getFakultas().getId())) {
                throw new IllegalArgumentException("Prodi tidak berada pada fakultas yang dipilih.");
            }

            List<Baris> rows = proses(session, fakultas, jurusan, dosen, status, masa,
                    program, tahun, jenisSemester, semesterKe, belum, belumSemua, telah);
            byte[] bytes = buatXlsx(rows);
            json.put("format", "xlsx")
                    .put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .put("namaFile", "REKAP_PENILAIAN.xlsx")
                    .put("fileBase64", java.util.Base64.getEncoder().encodeToString(bytes));
        } finally { session.close(); }
    }

    private static void forbiddenScope() {
        throw new SecurityException("Pilihan berada di luar lingkup hak akses pengguna.");
    }

    @SuppressWarnings("unchecked")
    private static List<Baris> proses(Session session, Fakultas fakultas, Jurusan jurusan,
            Dosen dosen, StatusPertemuan status, MasaPerkuliahan masa, String program,
            String tahun, String jenisSemester, Integer semesterKe, boolean belum,
            boolean belumSemua, boolean telah) {
        Criteria criteria = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .createAlias("jurusan", "jurusan")
                .addOrder(Order.asc("waktuMulai"));
        if (masa != null) criteria.add(Restrictions.eq("masaPerkuliahan", masa));
        if (dosen != null) criteria.add(dosen(dosen));
        if (fakultas != null) criteria.add(Restrictions.eq("jurusan.fakultas", fakultas));
        if (jurusan != null) criteria.add(Restrictions.eq("jurusan.id", jurusan.getId()));
        if (program.length() > 0) criteria.add(Restrictions.eq("program", program));
        if (semesterKe != null) criteria.add(Restrictions.eq("semester", semesterKe));
        criteria.add(Restrictions.eq("tahunAjaran", tahun));
        if (!SEMUA.equals(jenisSemester)) {
            criteria.add(Perkuliahan.SP.equals(jenisSemester)
                    ? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
                    : Restrictions.eq("ganjilGenap", jenisSemester));
        }

        List<Baris> result = new ArrayList<Baris>();
        for (Perkuliahan kuliah : (List<Perkuliahan>) criteria.list()) {
            try {
                Baris row = hitung(session, kuliah, status);
                if (telah && row.sudah == 0) continue;
                if (belum && row.belum == 0) continue;
                if (belumSemua && row.belum != row.total) continue;
                result.add(row);
            } catch (Exception e) {
                try { ais.common.ErrorAuditUtil.record(e,
                        "NewUiLaporanRekapPenilaianController.perPerkuliahan"); }
                catch (Exception ignored) { }
            }
        }
        return result;
    }

    private static Criterion dosen(Dosen dosen) {
        Criterion result = Restrictions.sqlRestriction("false");
        for (int i = 1; i <= 10; i++) {
            result = Restrictions.or(result, Restrictions.eq("dosen" + i, dosen));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Baris hitung(Session session, Perkuliahan kuliah,
            StatusPertemuan status) {
        List<FormatNilai> formats = status == null ? null : kuliah.ambilFormatNilai(session);
        Collection<Long> ids = kuliah.ambilDetailperkuliahan();
        int sudah = 0;
        double totalIps = 0.0, totalNilai = 0.0;
        Date terakhir = null;
        for (Long id : ids) {
            Detailperkuliahan detail = (Detailperkuliahan) session.get(Detailperkuliahan.class, id);
            if (detail == null) continue;
            totalIps += aman(detail.getTotalIP());
            totalNilai += aman(detail.getTotalNilai());
            boolean dinilai = false;
            boolean relevanTanggal = status == null;
            if (status != null) {
                for (FormatNilai format : formats) {
                    if (format.getStatusPertemuan() != null
                            && status.getId().equals(format.getStatusPertemuan().getId())) {
                        relevanTanggal = true;
                        if (aman(detail.retreiveDetailNilai(format)) > 0.1) dinilai = true;
                    }
                }
            } else {
                dinilai = aman(detail.getTotalNilai()) > 0.1;
            }
            if (dinilai) sudah++;
            Date changed = detail.getTanggal_dirubah();
            if (relevanTanggal && changed != null
                    && (terakhir == null || terakhir.before(changed))) terakhir = changed;
        }
        Baris row = new Baris();
        row.kode = kuliah.getMatakuliah() == null ? "" : kuliah.getMatakuliah().getKode();
        row.matakuliah = kuliah.getMatakuliah() == null ? "" : kuliah.getMatakuliah().getNama();
        row.hari = text(kuliah.getHari(), "");
        row.waktu = text(kuliah.getWaktuMulai(), "") + "-" + text(kuliah.getWaktuSelesai(), "");
        row.dosen = dosen(kuliah);
        Ruang ruang = kuliah.getRuang(); row.ruang = ruang == null ? "-" : text(ruang.getNama(), "-");
        row.semester = String.valueOf(kuliah.getSemester())
                + (text(kuliah.getKelas(), "").length() == 0 ? "" : " " + kuliah.getKelas());
        row.total = ids.size(); row.sudah = sudah; row.belum = row.total - sudah;
        row.persen = row.total == 0 ? 0.0 : 100.0 * sudah / row.total;
        row.terakhir = terakhir == null ? "" : Common.dateFormat5.get().format(terakhir);
        row.totalIps = totalIps;
        row.rataIps = row.total == 0 ? 0.0 : totalIps / row.total;
        row.rataNilai = row.total == 0 ? 0.0 : totalNilai / row.total;
        return row;
    }

    private static String dosen(Perkuliahan kuliah) {
        Map<String, Dosen> map = new LinkedHashMap<String, Dosen>(kuliah.populateDosen());
        if (map.size() <= 1) return kuliah.getDosen1() == null ? "" : kuliah.getDosen1().getNama();
        String result = "";
        for (Dosen d : map.values()) if (d != null)
            result += result.length() == 0 ? d.getNama() : ", " + d.getNama();
        return result;
    }

    static byte[] buatXlsx(List<Baris> rows) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("PENILAIAN");
        CellStyle head = style(wb, true); CellStyle body = style(wb, false);
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADER.length; i++) cell(header, i, HEADER[i], head);
        int index = 1;
        if (rows != null) for (Baris data : rows) {
            Row row = sheet.createRow(index++);
            Object[] values = { data.kode, data.matakuliah, data.hari, data.waktu, data.dosen,
                    data.ruang, data.semester, data.sudah, data.belum, data.persen,
                    data.terakhir, data.totalIps, data.rataIps, data.rataNilai };
            for (int i = 0; i < values.length; i++) cell(row, i, values[i], body);
        }
        int[] widths = { 18, 34, 14, 18, 34, 18, 14, 18, 18, 18, 24, 18, 18, 18 };
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try { wb.write(out); return out.toByteArray(); }
        finally { out.close(); }
    }

    private static CellStyle style(XSSFWorkbook wb, boolean bold) {
        CellStyle style = wb.createCellStyle(); Font font = wb.createFont();
        if (bold) font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font); style.setWrapText(true);
        style.setBorderTop(XSSFCellStyle.BORDER_THIN);
        style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        style.setBorderRight(XSSFCellStyle.BORDER_THIN);
        return style;
    }

    private static void cell(Row row, int index, Object value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value == null ? "" : value.toString());
        cell.setCellStyle(style);
    }

    static final class Baris {
        String kode = "", matakuliah = "", hari = "", waktu = "", dosen = "",
                ruang = "", semester = "", terakhir = "";
        int total, sudah, belum;
        double persen, totalIps, rataIps, rataNilai;
    }

    private static double aman(Number value) { return value == null ? 0.0 : value.doubleValue(); }
    private static boolean flag(String value) { return "1".equals(text(value, "0")); }
    private static String pilihanTahun(String value) {
        if (value.length() == 0 || !value.matches("[0-9]{4}/[0-9]{4}"))
            throw new IllegalArgumentException("Tahun akademik tidak valid.");
        boolean dikenal = Common.tahunAngkatans.isEmpty() || Common.tahunAngkatans.contains(value);
        if (!dikenal) throw new IllegalArgumentException("Tahun akademik tidak tersedia.");
        return value;
    }
    private static String pilihanSemester(String value) {
        if (Perkuliahan.GANJIL.equals(value) || Perkuliahan.GENAP.equals(value)
                || Perkuliahan.SP.equals(value) || SEMUA.equals(value)) return value;
        throw new IllegalArgumentException("Jenis semester tidak valid.");
    }
    private static Integer semester(String value) {
        if (SEMUA.equals(value) || value.length() == 0) return null;
        try { int i = Integer.parseInt(value); if (i >= 1 && i <= 15) return Integer.valueOf(i); }
        catch (Exception ignored) { }
        throw new IllegalArgumentException("Semester ke tidak valid.");
    }
    private static Long idOpsional(String value) {
        if (value == null || value.trim().length() == 0) return null;
        try { long id = Long.parseLong(value.trim()); if (id > 0) return Long.valueOf(id); }
        catch (Exception ignored) { }
        throw new IllegalArgumentException("Pilihan relasi tidak valid.");
    }
    @SuppressWarnings("unchecked")
    private static <T> T entity(Session session, Class<T> type, Long id) {
        if (id == null) return null;
        T value = (T) session.get(type, id);
        if (value == null) throw new IllegalArgumentException("Data pilihan tidak ditemukan.");
        return value;
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
