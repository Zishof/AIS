package ais.common.newui.laporan;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.Jurusan;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Program;
import ais.database.model.Ruang;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;

/** Kontrak native laporan daftar hadir/nilai UTS dan UAS. */
public final class NewUiLaporanDaftarHadirUjianController {

    private static final String MODULE = "root/report";
    private static final String UTS = "UTS", UAS = "UAS";
    private static final long STATUS_UTS = 3L, STATUS_UAS = 4L;

    private NewUiLaporanDaftarHadirUjianController() { }

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
            if ("meta".equals(action)) meta(json, request);
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
            fail(json, "INTERNAL_ERROR", "Laporan Daftar Hadir Ujian gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanDaftarHadirUjianController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject json, HttpServletRequest request) throws Exception {
        JSONArray filter = new JSONArray();
        filter.put(relasi("fakultas", "Fakultas", Fakultas.class, false, false));
        filter.put(relasi("jurusan", "Prodi", Jurusan.class, true, false)
                .put("tergantungPada", "fakultas").put("dependensiWajib", false));
        filter.put(pilihan("semester", "Semester", angka(1, 21), "1", true));
        filter.put(relasi("dosen", "Dosen", Dosen.class, false, true));
        filter.put(pilihan("tahunAkademik", "Tahun Akademik", tahunAkademik(),
                bawaanTahun(), true));
        filter.put(relasi("program", "Program", Program.class, true, false));
        filter.put(teks("kelas", "Kelas", false));
        filter.put(pilihan("jenisUjian", "Jenis Ujian", new String[] { UTS, UAS }, UAS, true));
        filter.put(relasi("perkuliahan", "Pilih Perkuliahan", Perkuliahan.class, true, true)
                .put("tergantungPada", new JSONArray().put("jurusan").put("semester")
                        .put("tahunAkademik").put("program")));
        filter.put(tanggal("tanggalUjian", "Tanggal Ujian", false, ""));
        filter.put(teks("waktuUjian", "Waktu", false));
        filter.put(relasi("ruang", "Ruang", Ruang.class, false, true));
        filter.put(tanggal("tanggalDibuat", "Laporan dibuat Tanggal", false,
                formatTanggal(ais.ui.util.WaktuUtil.getDate())));
        filter.put(bendera("tampilNilai", "Tampil Nilai"));
        filter.put(bendera("tampilPembobotan", "Tampil Pembobotan"));
        json.put("judul", "Laporan Daftar Hadir Ujian")
                .put("format", "pdf").put("filter", filter)
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("bolehUbah", false)
                .put("catatan", "Cetak daftar hadir atau daftar nilai UTS/UAS per kelas.");
    }

    private static JSONObject relasi(String nama, String label, Class<?> entity,
            boolean wajib, boolean cari) throws Exception {
        JSONObject o = new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "relasi").put("wajib", wajib).put("entity", entity.getName());
        if (cari) o.put("cari", true);
        return o;
    }

    private static JSONObject pilihan(String nama, String label, String[] opsi,
            String bawaan, boolean wajib) throws Exception {
        JSONArray values = new JSONArray();
        for (String value : opsi) values.put(value);
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "pilihan").put("wajib", wajib).put("opsi", values)
                .put("bawaan", bawaan);
    }

    private static JSONObject teks(String nama, String label, boolean wajib) throws Exception {
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "teks").put("wajib", wajib);
    }

    private static JSONObject tanggal(String nama, String label, boolean wajib,
            String bawaan) throws Exception {
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "tanggal").put("wajib", wajib).put("bawaan", bawaan);
    }

    private static JSONObject bendera(String nama, String label) throws Exception {
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", "bendera").put("wajib", false).put("bawaan", "0");
    }

    private static String[] angka(int mulai, int akhir) {
        String[] result = new String[akhir - mulai + 1];
        for (int i = mulai; i <= akhir; i++) result[i - mulai] = String.valueOf(i);
        return result;
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
        int year = Calendar.getInstance().get(Calendar.YEAR);
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
            if ("fakultas".equals(filter)) lookupFakultas(pilihan, session, user, q);
            else if ("jurusan".equals(filter)) lookupJurusan(pilihan, session, user, request, q);
            else if ("dosen".equals(filter)) lookupDosen(pilihan, session, q);
            else if ("program".equals(filter)) lookupProgram(pilihan, session, user, q);
            else if ("ruang".equals(filter)) lookupRuang(pilihan, session, q);
            else if ("perkuliahan".equals(filter)) lookupPerkuliahan(pilihan, session, user, request, q);
            else throw new IllegalArgumentException("Filter relasi tidak dikenal.");
        } finally { session.close(); }
        json.put("filter", filter).put("pilihan", pilihan).put("total", pilihan.length())
                .put("batas", 50);
    }

    @SuppressWarnings("unchecked")
    private static void lookupFakultas(JSONArray out, Session session, Tbmuser user,
            String q) throws Exception {
        Criteria c = session.createCriteria(Fakultas.class).setMaxResults(50);
        Fakultas scope = user.ambilFakultas();
        if (scope != null) c.add(Restrictions.eq("id", scope.getId()));
        if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
        c.addOrder(Order.asc("nama"));
        for (Fakultas row : (List<Fakultas>) c.list()) option(out, row.getId(), row.getNama());
    }

    @SuppressWarnings("unchecked")
    private static void lookupJurusan(JSONArray out, Session session, Tbmuser user,
            HttpServletRequest request, String q) throws Exception {
        Criteria c = session.createCriteria(Jurusan.class).setMaxResults(50);
        Jurusan scope = user.ambilJurusan();
        Long fakultas = idOpsional(request.getParameter("fakultas"));
        if (scope != null) c.add(Restrictions.eq("id", scope.getId()));
        else if (fakultas != null) c.createAlias("fakultas", "f")
                .add(Restrictions.eq("f.id", fakultas));
        if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
        c.addOrder(Order.asc("nama"));
        for (Jurusan row : (List<Jurusan>) c.list()) option(out, row.getId(), row.getNama());
    }

    @SuppressWarnings("unchecked")
    private static void lookupDosen(JSONArray out, Session session, String q) throws Exception {
        Criteria c = session.createCriteria(Dosen.class).setMaxResults(50);
        if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
        c.addOrder(Order.asc("nama"));
        for (Dosen row : (List<Dosen>) c.list()) option(out, row.getId(), row.getNama());
    }

    @SuppressWarnings("unchecked")
    private static void lookupProgram(JSONArray out, Session session, Tbmuser user,
            String q) throws Exception {
        Criteria c = session.createCriteria(Program.class).setMaxResults(50)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Program scope = user.ambilProgram();
        if (scope != null) c.add(Restrictions.eq("nama", scope.getNama()));
        if (q.length() >= 2) c.add(Restrictions.ilike("namaBaru", "%" + q + "%"));
        c.addOrder(Order.asc("num")).addOrder(Order.asc("nama"));
        for (Program row : (List<Program>) c.list())
            option(out, row.getNama(), text(row.getNamaBaru(), row.getNama()));
    }

    @SuppressWarnings("unchecked")
    private static void lookupRuang(JSONArray out, Session session, String q) throws Exception {
        Criteria c = session.createCriteria(Ruang.class).setMaxResults(50)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        if (q.length() >= 2) c.add(Restrictions.or(
                Restrictions.ilike("kodeRuangan", "%" + q + "%"),
                Restrictions.ilike("nama", "%" + q + "%")));
        c.addOrder(Order.asc("kodeRuangan"));
        for (Ruang row : (List<Ruang>) c.list())
            option(out, row.getId(), row.getKodeRuangan() + " - " + text(row.getNama(), ""));
    }

    @SuppressWarnings("unchecked")
    private static void lookupPerkuliahan(JSONArray out, Session session, Tbmuser user,
            HttpServletRequest request, String q) throws Exception {
        Long jurusanId = idWajib(request.getParameter("jurusan"), "Prodi");
        int semester = semester(request.getParameter("semester"));
        String tahun = tahun(request.getParameter("tahunAkademik"));
        String program = text(request.getParameter("program"), "");
        if (program.length() == 0) throw new IllegalArgumentException("Program wajib dipilih.");
        Long dosenId = idOpsional(request.getParameter("dosen"));
        String kelas = text(request.getParameter("kelas"), "");
        Jurusan jurusan = entity(session, Jurusan.class, jurusanId);
        Dosen filterDosen = entity(session, Dosen.class, dosenId);
        Criteria c = session.createCriteria(Perkuliahan.class).setMaxResults(50)
                .add(Restrictions.eq("tahunAjaran", tahun))
                .add(Restrictions.eq("semester", Integer.valueOf(semester)))
                .add(Restrictions.eq("jurusan", jurusan))
                .add(Restrictions.ilike("program", program, MatchMode.ANYWHERE));
        if (kelas.length() > 0) c.add(Restrictions.ilike("kelas", kelas, MatchMode.ANYWHERE));
        if (filterDosen != null) c.add(Restrictions.eq("dosen1", filterDosen));
        if (q.length() >= 2) {
            c.createAlias("matakuliah", "mk");
            c.add(Restrictions.or(Restrictions.ilike("mk.nama", "%" + q + "%"),
                    Restrictions.ilike("mk.kode", "%" + q + "%")));
        }
        batasiCakupan(session, user, jurusanId, program);
        c.addOrder(Order.asc("waktuMulaiD"));
        for (Perkuliahan row : (List<Perkuliahan>) c.list()) {
            String dosen = row.getDosen1() == null ? "" : row.getDosen1().getNama();
            String matakuliah = row.getMatakuliah() == null ? "-" : row.getMatakuliah().getNama();
            option(out, row.getId(), dosen + " - " + matakuliah + " (" + row.getId() + ")");
        }
    }

    private static void batasiCakupan(Session session, Tbmuser user,
            Long jurusanId, String program) {
        Jurusan scopeJ = user.ambilJurusan();
        Fakultas scopeF = user.ambilFakultas();
        Program scopeP = user.ambilProgram();
        if (scopeJ != null && !scopeJ.getId().equals(jurusanId)) forbiddenScope();
        if (scopeF != null) {
            Jurusan jurusan = (Jurusan) session.get(Jurusan.class, jurusanId);
            if (jurusan == null || jurusan.getFakultas() == null
                    || !scopeF.getId().equals(jurusan.getFakultas().getId())) forbiddenScope();
        }
        if (scopeP != null && !scopeP.getNama().equals(program)) forbiddenScope();
    }

    private static void option(JSONArray array, Object id, String nama) throws Exception {
        array.put(new JSONObject().put("id", id).put("nama", text(nama, "-")));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void export(JSONObject json, HttpServletRequest request, Tbmuser user)
            throws Exception {
        int semester = semester(request.getParameter("semester"));
        String tahun = tahun(request.getParameter("tahunAkademik"));
        String jenis = jenis(request.getParameter("jenisUjian"));
        String program = text(request.getParameter("program"), "");
        if (program.length() == 0) throw new IllegalArgumentException("Program wajib dipilih.");
        Long perkuliahanId = idWajib(request.getParameter("perkuliahan"), "Perkuliahan");
        Date tanggalInput = tanggalOpsional(request.getParameter("tanggalUjian"));
        Date tanggalDibuat = tanggalOpsional(request.getParameter("tanggalDibuat"));
        boolean tampilNilai = flag(request.getParameter("tampilNilai"));
        boolean tampilPembobotan = flag(request.getParameter("tampilPembobotan"));
        Session session = HibernateUtil.openSession();
        try {
            Perkuliahan kuliah = (Perkuliahan) session.get(Perkuliahan.class, perkuliahanId);
            if (kuliah == null) throw new IllegalArgumentException("Perkuliahan tidak ditemukan.");
            validasiKelas(session, request, user, kuliah, semester, tahun, program);
            Pertemuan jadwal = pertemuan(session, kuliah, jenis);
            Date tanggalUjian = tanggalInput != null ? tanggalInput
                    : jadwal == null || jadwal.getTanggal() == null
                            ? ais.ui.util.WaktuUtil.getDate() : jadwal.getTanggal();
            String waktu = text(request.getParameter("waktuUjian"),
                    jadwal == null ? "00.00" : text(jadwal.getWaktuMulai(), "00.00"));
            Ruang ruang = entity(session, Ruang.class, idOpsional(request.getParameter("ruang")));
            if (ruang == null) ruang = jadwal != null && jadwal.getRuang() != null
                    ? jadwal.getRuang() : kuliah.getRuang();
            List<FormatNilai> formats = Common.getFormatNilais(session, kuliah);
            if (UAS.equals(jenis) && formats.size() > 9)
                throw new IllegalArgumentException("Format nilai UAS maksimal 9 kolom untuk template cetak.");
            Map parameters = parameter(session, kuliah, semester, jenis, tanggalUjian,
                    tanggalDibuat == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDibuat,
                    waktu, ruang, formats, tampilNilai, tampilPembobotan);
            String template = "Daftar_Hadir_Ujian";
            if (UAS.equals(jenis)) {
                List<Map<String, Serializable>> maps = dataUas(session, kuliah, formats, tampilNilai);
                parameters.put("maps", maps);
                template = templateUas(formats.size());
            }
            JasperPdfUtil.tulis(json, template, parameters,
                    "DAFTAR_HADIR_UJIAN_" + jenis, "Laporan Daftar Hadir Ujian " + jenis);
        } finally { session.close(); }
    }

    private static void validasiKelas(Session session, HttpServletRequest request, Tbmuser user,
            Perkuliahan kuliah, int semester, String tahun, String program) {
        if (!Integer.valueOf(semester).equals(kuliah.getSemester())
                || !tahun.equals(kuliah.getTahunAjaran())
                || !program.equals(kuliah.getProgram()))
            throw new IllegalArgumentException("Perkuliahan tidak sesuai semester, tahun, atau program.");
        Long jurusanId = idWajib(request.getParameter("jurusan"), "Prodi");
        if (kuliah.getJurusan() == null || !jurusanId.equals(kuliah.getJurusan().getId()))
            throw new IllegalArgumentException("Perkuliahan tidak berada pada prodi yang dipilih.");
        Long fakultasId = idOpsional(request.getParameter("fakultas"));
        if (fakultasId != null && (kuliah.getJurusan().getFakultas() == null
                || !fakultasId.equals(kuliah.getJurusan().getFakultas().getId())))
            throw new IllegalArgumentException("Perkuliahan tidak berada pada fakultas yang dipilih.");
        Long dosenId = idOpsional(request.getParameter("dosen"));
        if (dosenId != null && (kuliah.getDosen1() == null
                || !dosenId.equals(kuliah.getDosen1().getId())))
            throw new IllegalArgumentException("Perkuliahan tidak diajar dosen yang dipilih.");
        batasiCakupan(session, user, jurusanId, program);
    }

    private static Pertemuan pertemuan(Session session, Perkuliahan kuliah, String jenis) {
        return (Pertemuan) session.createCriteria(Pertemuan.class)
                .add(Restrictions.eq("perkuliahan", kuliah))
                .createAlias("statusPertemuan", "sp")
                .add(Restrictions.eq("sp.id", Long.valueOf(UTS.equals(jenis) ? STATUS_UTS : STATUS_UAS)))
                .setMaxResults(1).uniqueResult();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map parameter(Session session, Perkuliahan kuliah, int semester,
            String jenis, Date tanggalUjian, Date tanggalDibuat, String waktu, Ruang ruang,
            List<FormatNilai> formats, boolean tampilNilai, boolean tampilPembobotan) {
        Map p = ais.common.HashMapGenerator.getRand();
        Staff kaprodi = (Staff) session.createCriteria(Staff.class)
                .add(Restrictions.eq("staff", Staff.KAPRODI))
                .add(Restrictions.eq("jurusan", kuliah.getJurusan()))
                .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
        SimpleDateFormat display = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);
        p.put("hari", hari(tanggalUjian)); p.put("tanggal", display.format(tanggalUjian));
        p.put("hari_tanggal", tanggalUjian); p.put("perkuliahan", kuliah.getId());
        p.put("kelas", text(kuliah.getKelas(), "")); p.put("waktu", waktu);
        p.put("ruang", ruang == null ? "" : ruang.getKodeRuangan());
        p.put("pudek_1", kaprodi == null ? "" : kaprodi.getNama());
        p.put("kaprodi", kaprodi == null ? "" : kaprodi.getNama());
        p.put("nip", kaprodi == null ? "" : text(kaprodi.getNip(), ""));
        p.put("tanggal_dibuat", display.format(tanggalDibuat));
        p.put("tampil_nilai", Integer.valueOf(tampilNilai ? 1 : 0));
        p.put("nip_dosen", kuliah.getDosen1() == null ? "" : text(kuliah.getDosen1().getCode(), ""));
        p.put("jenis_semester", jenisSemester(semester));
        p.put("dosen_pengajar", dosen(kuliah));
        if (UAS.equals(jenis)) {
            p.put("tampil_pembobotan", Integer.valueOf(tampilPembobotan ? 1 : 0));
            p.put("fakultas", kuliah.getJurusan() == null || kuliah.getJurusan().getFakultas() == null
                    ? "" : kuliah.getJurusan().getFakultas().getNama());
            p.put("tahun_ajaran", text(kuliah.getTahunAjaran(), ""));
            p.put("nama_matakuliah", kuliah.getMatakuliah() == null ? "" : kuliah.getMatakuliah().getNama());
            p.put("dosen", kuliah.getDosen1() == null ? "" : kuliah.getDosen1().getNama());
            p.put("dosen_2", kuliah.getDosen2() == null ? "" : kuliah.getDosen2().getNama());
            p.put("nip_dosen_2", kuliah.getDosen2() == null ? "" : text(kuliah.getDosen2().getCode(), ""));
            p.put("jurusan", kuliah.getJurusan() == null ? "" : kuliah.getJurusan().getNama());
            for (int i = 0; i < formats.size(); i++) {
                FormatNilai f = formats.get(i);
                p.put("col" + (i + 1), kolom(f.getNama(), f.getPersen(), tampilPembobotan));
            }
        }
        p.put("nilaiHuruf", nilaiHuruf(session));
        return p;
    }

    private static String dosen(Perkuliahan kuliah) {
        Map<String, Dosen> map = new LinkedHashMap<String, Dosen>(kuliah.populateDosen());
        if (map.size() <= 1) return kuliah.getDosen1() == null ? "" : kuliah.getDosen1().getNama();
        String result = "";
        for (Dosen d : map.values()) if (d != null)
            result += result.length() == 0 ? d.getNama() : ", " + d.getNama();
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String nilaiHuruf(Session session) {
        List<NilaiHuruf> rows = session.createCriteria(NilaiHuruf.class)
                .addOrder(Order.desc("nilaiDiIPK")).list();
        String result = "";
        for (NilaiHuruf n : rows) result += n.getNilaiHuruf() + " = "
                + Common.numberFormat.get().format(n.getMulai()) + " s.d "
                + Common.numberFormat.get().format(n.getSampai()) + "\n";
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Serializable>> dataUas(Session session,
            Perkuliahan kuliah, List<FormatNilai> formats, boolean tampilNilai) {
        List<Detailperkuliahan> details = session.createCriteria(Detailperkuliahan.class)
                .add(Restrictions.isNull("ikutiPerkuliahan"))
                .add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
                .add(Restrictions.eq("perkuliahan", kuliah))
                .createAlias("mahasiswa", "m").addOrder(Order.asc("m.nim")).list();
        List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
        for (Detailperkuliahan detail : details) {
            Map<String, Serializable> row = new HashMap<String, Serializable>();
            row.put("nim", detail.getMahasiswa().getNim());
            row.put("nama", detail.getMahasiswa().getNama());
            row.put("kode_matakuliah", detail.getPerkuliahan().getMatakuliah().getKode());
            for (int i = 0; i < formats.size(); i++) row.put("nilai_" + (i + 1),
                    tampilNilai ? Double.valueOf(detail.retreiveDetailNilai(formats.get(i))) : null);
            row.put("nilai", tampilNilai ? detail.getTotalNilai() : null);
            row.put("nilai_huruf", tampilNilai ? text(detail.getNilaiHuruf(), "") : "");
            maps.add(row);
        }
        return maps;
    }

    static String templateUas(int count) {
        if (count < 0 || count > 9)
            throw new IllegalArgumentException("Jumlah format nilai UAS tidak didukung.");
        return "Daftar_Hadir_Ujian_UAS_" + count;
    }

    static String jenisSemester(int semester) {
        if (semester < 1 || semester >= Common.ROMAWI.length)
            throw new IllegalArgumentException("Semester tidak sah.");
        return Common.ROMAWI[semester] + " / "
                + (semester % 2 == 0 ? Perkuliahan.GENAP + " " : Perkuliahan.GANJIL);
    }

    static String kolom(String nama, Number persen, boolean tampilPembobotan) {
        String label = text(nama, "");
        return tampilPembobotan ? label + "\n" + (persen == null ? 0 : persen) + "%" : label;
    }

    static String hari(Date date) {
        Calendar c = Calendar.getInstance(Common.locale); c.setTime(date);
        int index = c.get(Calendar.DAY_OF_WEEK) - 2;
        if (index < 0) index += 7;
        return Common.haris[index];
    }

    private static String jenis(String value) {
        String result = text(value, UAS).toUpperCase();
        if (!UTS.equals(result) && !UAS.equals(result))
            throw new IllegalArgumentException("Jenis ujian tidak sah.");
        return result;
    }

    private static int semester(String value) {
        try {
            int result = Integer.parseInt(text(value, ""));
            if (result < 1 || result > 21) throw new NumberFormatException();
            return result;
        } catch (Exception e) { throw new IllegalArgumentException("Semester wajib dipilih."); }
    }

    private static String tahun(String value) {
        String result = text(value, "");
        if (!result.matches("[0-9]{4}/[0-9]{4}"))
            throw new IllegalArgumentException("Tahun akademik tidak sah.");
        return result;
    }

    private static Date tanggalOpsional(String value) {
        String s = text(value, "");
        if (s.length() == 0) return null;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd"); f.setLenient(false);
            return f.parse(s);
        } catch (Exception e) { throw new IllegalArgumentException("Tanggal tidak sah."); }
    }

    private static String formatTanggal(Date value) {
        return new SimpleDateFormat("yyyy-MM-dd").format(value);
    }

    private static boolean flag(String value) {
        String s = text(value, "0");
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s);
    }

    @SuppressWarnings("unchecked")
    private static <T> T entity(Session session, Class<T> type, Long id) {
        if (id == null) return null;
        Object value = session.get(type, id);
        if (value == null) throw new IllegalArgumentException("Data filter tidak ditemukan.");
        return (T) value;
    }

    private static Long idWajib(String value, String label) {
        Long id = idOpsional(value);
        if (id == null) throw new IllegalArgumentException(label + " wajib dipilih.");
        return id;
    }

    private static Long idOpsional(String value) {
        String s = text(value, "");
        if (s.length() == 0 || "-1".equals(s)) return null;
        try {
            long id = Long.parseLong(s); if (id <= 0) throw new NumberFormatException();
            return Long.valueOf(id);
        } catch (Exception e) { throw new IllegalArgumentException("Id filter tidak sah."); }
    }

    private static void forbiddenScope() {
        throw new SecurityException("Pilihan berada di luar lingkup hak akses pengguna.");
    }

    private static String text(Object value, String fallback) {
        String s = value == null ? "" : value.toString().trim();
        return s.length() == 0 ? fallback : s;
    }

    private static void fail(JSONObject json, String code, String message) throws Exception {
        json.put("ok", false).put("code", code).put("error", text(message, code));
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
