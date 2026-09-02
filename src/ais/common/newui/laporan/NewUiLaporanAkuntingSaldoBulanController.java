package ais.common.newui.laporan;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;

/**
 * Kontrak native untuk Laporan Keuangan / saldo bulanan akuntansi.
 *
 * <p>Layar ZK mempunyai dua mode. Bila ada {@link JenisLaporan} yang memiliki
 * lampiran JRXML, hanya jenis bertemplate itu yang dapat dipilih dan JRXML-nya
 * dikompilasi saat ekspor. Bila tidak ada, laporan memakai template baku dan
 * menyediakan filter tipe, grup, serta kelompok laporan. Pemisahan mode ini
 * dipertahankan; klien tidak boleh mengirim lokasi template sendiri.</p>
 */
public final class NewUiLaporanAkuntingSaldoBulanController {

    private static final String MODULE = "root/report";
    static final String TEMPLATE = "akunting/laporan_keuangan_mutasi";
    private static final int BATAS = 100;

    private NewUiLaporanAkuntingSaldoBulanController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json);
            else if ("lookup".equals(action)) lookup(json, request);
            else if ("export".equals(action) || "export_pdf".equals(action)) cetak(json, request);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses laporan keuangan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanAkuntingSaldoBulanController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta
    private static void meta(JSONObject j) throws Exception {
        boolean kustom = !jenisBertemplate().isEmpty();
        JSONArray filter = filterUntukMode(kustom);

        Calendar awal = Calendar.getInstance();
        awal.set(Calendar.DAY_OF_MONTH, 1);
        Calendar akhir = Calendar.getInstance();
        akhir.set(Calendar.DAY_OF_MONTH, akhir.getActualMaximum(Calendar.DAY_OF_MONTH));
        j.put("judul", "Laporan Keuangan")
                .put("template", TEMPLATE)
                .put("modeTemplateKustom", kustom)
                .put("filter", filter)
                .put("pilihanTahun", new JSONArray())
                .put("pilihanBulan", new JSONArray())
                .put("mulaiBawaan", Common.databaseDateFormat.get().format(awal.getTime()))
                .put("sampaiBawaan", Common.databaseDateFormat.get().format(akhir.getTime()));
    }

    /** Deskripsi dua mode dipisahkan agar kontraknya dapat diuji tanpa database. */
    static JSONArray filterUntukMode(boolean kustom) throws Exception {
        JSONArray filter = new JSONArray();
        filter.put(deskripsi("mulai", "Tanggal Mulai", "tanggal", true));
        filter.put(deskripsi("sampai", "Tanggal Sampai", "tanggal", true));
        filter.put(deskripsi("jenis_laporan", "Jenis Laporan", "relasi", kustom));
        if (!kustom) {
            filter.put(deskripsi("nama", "Tipe Laporan", "relasi", false));
            filter.put(deskripsi("grup", "Grup Laporan", "relasi", false)
                    .put("tergantungPada", new JSONArray().put("jenis_laporan").put("nama"))
                    .put("dependensiWajib", false));
            filter.put(deskripsi("kelompok", "Kelompok Laporan", "relasi", false)
                    .put("tergantungPada", new JSONArray()
                            .put("jenis_laporan").put("nama").put("grup"))
                    .put("dependensiWajib", false));
        }
        filter.put(deskripsi("satuan_kerja", "Satuan Kerja", "relasi", false));
        return filter;
    }

    private static JSONObject deskripsi(String nama, String label, String tipe, boolean wajib)
            throws Exception {
        return new JSONObject().put("nama", nama).put("label", label)
                .put("tipe", tipe).put("wajib", wajib);
    }

    // ---------------------------------------------------------------- lookup
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String filter = text(r.getParameter("filter"), "");
        String q = text(r.getParameter("q"), "").toLowerCase();
        JSONArray pilihan = new JSONArray();
        if ("jenis_laporan".equals(filter)) {
            List<JenisLaporan> daftar = jenisBertemplate();
            if (daftar.isEmpty()) daftar = semuaJenis();
            for (JenisLaporan item : daftar) tambah(pilihan, item.getId(), label(item), q);
        } else if ("nama".equals(filter)) {
            for (String nama : semuaNama()) tambah(pilihan, nama, nama, q);
        } else if ("grup".equals(filter)) {
            Long jenis = parameterId(r, "jenis_laporan", "Jenis Laporan");
            String nama = optional(r.getParameter("nama"));
            for (MasterGrupLaporan grup : grupTersedia(jenis, nama)) {
                tambah(pilihan, grup.getId(), label(grup), q);
            }
        } else if ("kelompok".equals(filter)) {
            Long jenis = parameterId(r, "jenis_laporan", "Jenis Laporan");
            Long grup = parameterId(r, "grup", "Grup Laporan");
            for (KelompokLaporan kelompok : kelompokTersedia(jenis, grup)) {
                tambah(pilihan, kelompok.getId(), kelompok.getKeterangan(), q);
            }
        } else if ("satuan_kerja".equals(filter)) {
            for (SatuanKerja unit : satuanKerjaTersedia()) {
                tambah(pilihan, unit.getId(), unit.toString(), q);
            }
        } else {
            throw new IllegalArgumentException("Filter relasi tidak dikenal.");
        }
        j.put("filter", filter).put("pilihan", pilihan)
                .put("total", pilihan.length()).put("batas", BATAS);
    }

    private static void tambah(JSONArray pilihan, Object id, String nama, String q) throws Exception {
        if (pilihan.length() >= BATAS || id == null) return;
        String label = nama == null ? "" : nama.trim();
        if (q.length() > 0 && !label.toLowerCase().contains(q)) return;
        pilihan.put(new JSONObject().put("id", id).put("nama", label));
    }

    // ----------------------------------------------------------------- cetak
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r) throws Exception {
        Date mulai = tanggal(text(r.getParameter("mulai"), ""));
        Date sampai = tanggal(text(r.getParameter("sampai"), ""));
        if (mulai == null) throw new IllegalArgumentException("Tanggal mulai wajib diisi.");
        if (sampai == null) throw new IllegalArgumentException("Tanggal sampai wajib diisi.");

        List<JenisLaporan> kustom = jenisBertemplate();
        boolean modeKustom = !kustom.isEmpty();
        Long jenisId = parameterId(r, "jenis_laporan", "Jenis Laporan");
        JenisLaporan jenis = jenisId == null ? null : entity(JenisLaporan.class, jenisId);
        if (modeKustom && (jenis == null || !memuat(kustom, jenisId))) {
            throw new IllegalArgumentException("Jenis Laporan bertemplate wajib dipilih.");
        }
        if (jenisId != null && jenis == null) throw new IllegalArgumentException("Jenis Laporan tidak ditemukan.");

        String nama = optional(r.getParameter("nama"));
        if (modeKustom) nama = null;
        else if (nama != null && !semuaNama().contains(nama)) {
            throw new IllegalArgumentException("Tipe Laporan tidak dikenal.");
        }
        Long grupId = modeKustom ? null : parameterId(r, "grup", "Grup Laporan");
        Long kelompokId = modeKustom ? null
                : parameterId(r, "kelompok", "Kelompok Laporan");
        validasiGrupKelompok(jenisId, nama, grupId, kelompokId);

        Long unitId = parameterId(r, "satuan_kerja", "Satuan Kerja");
        if (unitId != null && !satuanKerjaIds().contains(unitId)) {
            throw new SecurityException("Satuan Kerja berada di luar akses.");
        }

        Map parameters = parameterTanggal(mulai, sampai);
        parameters.put("nama", nama == null ? "-1" : nama);
        parameters.put("grup", grupId == null ? Long.valueOf(-1L) : grupId);
        parameters.put("kelompok", kelompokId == null ? Long.valueOf(-1L) : kelompokId);
        parameters.put("satuan_kerja", unitId == null ? Long.valueOf(-1L) : unitId);
        parameters.put("jenis_laporan", jenisId == null ? Long.valueOf(-1L) : jenisId);

        if (jenis != null && Common.bolehKonfigurasi(
                "laporan_saldo_harus_berdasarkan_jenis_laporan", Konfigurasi.TIDAK_AKTIF)) {
            parameters.put("nama_laporan", TEMPLATE + "_" + jenis.getId());
        }

        File jrxml = jenis == null ? null : templateKustom(jenis.getId());
        if (modeKustom) {
            if (jrxml == null) throw new IllegalArgumentException("Template Jenis Laporan tidak tersedia.");
            parameters.put("nama_laporan", jrxml.getAbsolutePath());
            JasperPdfUtil.tulisFile(j, jrxml.getAbsolutePath(), parameters,
                    "laporan_keuangan", "Laporan Keuangan — " + label(jenis));
        } else {
            JasperPdfUtil.tulis(j, TEMPLATE, parameters, "laporan_keuangan", "Laporan Keuangan");
        }
    }

    /** Parameter tanggal persis seperti generateParameter() layar ZK. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Map parameterTanggal(Date mulai, Date sampai) {
        Map parameters = ais.common.HashMapGenerator.getRand();
        isiParameterTanggal(parameters, mulai, sampai);
        return parameters;
    }

    /** Varian tanpa pembuatan map runtime, agar perhitungan dapat diuji tanpa Hibernate. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void isiParameterTanggal(Map parameters, Date mulai, Date sampai) {
        Calendar tanggal0 = Calendar.getInstance();
        tanggal0.setTime(mulai);
        tanggal0.set(Calendar.YEAR, tanggal0.get(Calendar.YEAR) - 1);
        Calendar tanggal3 = Calendar.getInstance();
        tanggal3.setTime(sampai);
        tanggal3.set(Calendar.YEAR, tanggal3.get(Calendar.YEAR) + 1);
        Calendar saldoAwal = Calendar.getInstance();
        saldoAwal.setTime(mulai);
        saldoAwal.add(Calendar.DAY_OF_MONTH, -1);
        Calendar tanggal2_1 = Calendar.getInstance();
        tanggal2_1.setTime(sampai);
        tanggal2_1.add(Calendar.DAY_OF_MONTH, -1);

        parameters.put("tanggal0", tanggal0.getTime());
        parameters.put("tanggal3", tanggal3.getTime());
        parameters.put("tanggal1", mulai);
        parameters.put("tanggal2", sampai);
        parameters.put("tanggal1_1", saldoAwal.getTime());
        parameters.put("tanggalSaldoAwalType", saldoAwal.getTime());
        parameters.put("tanggalSaldoAwal", Common.databaseDateFormat.get().format(saldoAwal.getTime()));
        parameters.put("tanggal2_1", tanggal2_1.getTime());
    }

    private static void validasiGrupKelompok(Long jenisId, String nama, Long grupId,
            Long kelompokId) {
        if (grupId != null) {
            boolean ada = false;
            for (MasterGrupLaporan grup : grupTersedia(jenisId, nama)) {
                if (grupId.equals(grup.getId())) { ada = true; break; }
            }
            if (!ada) throw new IllegalArgumentException("Grup Laporan tidak sesuai filter.");
        }
        if (kelompokId != null) {
            boolean ada = false;
            for (KelompokLaporan kelompok : kelompokTersedia(jenisId, grupId)) {
                if (kelompokId.equals(kelompok.getId())) { ada = true; break; }
            }
            if (!ada) throw new IllegalArgumentException("Kelompok Laporan tidak sesuai filter.");
        }
    }

    // ------------------------------------------------------------ data acuan
    @SuppressWarnings("unchecked")
    private static List<JenisLaporan> semuaJenis() {
        Session s = HibernateUtil.openSession();
        try {
            return new ArrayList<JenisLaporan>(s.createCriteria(JenisLaporan.class)
                    .addOrder(Order.asc("nama")).addOrder(Order.asc("keterangan")).list());
        } finally { s.close(); }
    }

    private static List<JenisLaporan> jenisBertemplate() {
        List<JenisLaporan> hasil = new ArrayList<JenisLaporan>();
        for (JenisLaporan jenis : semuaJenis()) {
            if (templateKustom(jenis.getId()) != null) hasil.add(jenis);
        }
        return hasil;
    }

    private static File templateKustom(Long jenisId) {
        try {
            LampiranLain lampiran = LampiranLain.ambil(jenisId,
                    LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI);
            if (lampiran == null || lampiran.getId() == null) return null;
            File file = lampiran.ambilFile();
            return file != null && file.exists() && file.isFile() ? file : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean memuat(List<JenisLaporan> daftar, Long id) {
        for (JenisLaporan jenis : daftar) if (id.equals(jenis.getId())) return true;
        return false;
    }

    private static String label(JenisLaporan jenis) {
        String nama = jenis.getNama() == null ? "" : jenis.getNama();
        String ket = jenis.getKeterangan() == null ? "" : jenis.getKeterangan();
        return ket.length() == 0 ? nama : nama + " — " + ket;
    }

    private static String label(MasterGrupLaporan grup) {
        String nama = grup.getNama() == null ? "" : grup.getNama();
        String ket = grup.getKeterangan() == null ? "" : grup.getKeterangan();
        return ket.length() == 0 ? nama : nama + " — " + ket;
    }

    @SuppressWarnings("unchecked")
    private static List<String> semuaNama() {
        Session s = HibernateUtil.openSession();
        Set<String> unik = new HashSet<String>();
        try {
            for (MasterGrupLaporan grup : (List<MasterGrupLaporan>)
                    s.createCriteria(MasterGrupLaporan.class).list()) {
                if (grup.getNama() != null && grup.getNama().trim().length() > 0) unik.add(grup.getNama().trim());
            }
        } finally { s.close(); }
        if (unik.isEmpty()) {
            unik.add(MasterGrupLaporan.AKTIVA);
            unik.add(MasterGrupLaporan.KEWAJIBAN);
            unik.add(MasterGrupLaporan.PENDAPATAN);
            unik.add(MasterGrupLaporan.OPERASIONAL);
            unik.add(MasterGrupLaporan.INVESTASI);
        }
        List<String> hasil = new ArrayList<String>(unik);
        Collections.sort(hasil);
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static List<MasterGrupLaporan> grupTersedia(Long jenisId, String nama) {
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(KelompokLaporan.class)
                    .add(Restrictions.isNotNull("masterGrupLaporan"));
            if (jenisId != null) {
                c.createAlias("jenisLaporan", "jenis");
                c.add(Restrictions.eq("jenis.id", jenisId));
            }
            List<KelompokLaporan> rows = c.list();
            Set<Long> ids = new HashSet<Long>();
            List<MasterGrupLaporan> hasil = new ArrayList<MasterGrupLaporan>();
            for (KelompokLaporan row : rows) {
                MasterGrupLaporan grup = row.getMasterGrupLaporan();
                if (grup == null || grup.getId() == null || ids.contains(grup.getId())) continue;
                if (nama != null && !nama.equals(grup.getNama())) continue;
                ids.add(grup.getId());
                hasil.add(grup);
            }
            Collections.sort(hasil);
            return hasil;
        } finally { s.close(); }
    }

    @SuppressWarnings("unchecked")
    private static List<KelompokLaporan> kelompokTersedia(Long jenisId, Long grupId) {
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(KelompokLaporan.class);
            if (jenisId != null) {
                c.createAlias("jenisLaporan", "jenis");
                c.add(Restrictions.eq("jenis.id", jenisId));
            }
            if (grupId != null) {
                c.createAlias("masterGrupLaporan", "grup");
                c.add(Restrictions.eq("grup.id", grupId));
            }
            c.addOrder(Order.asc("urut")).addOrder(Order.asc("keterangan"));
            return new ArrayList<KelompokLaporan>(c.list());
        } finally { s.close(); }
    }

    private static Set<Long> satuanKerjaIds() {
        Set<Long> ids = new HashSet<Long>();
        Set<SatuanKerja> units = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
        if (units != null) for (SatuanKerja unit : units) {
            if (unit != null && unit.getId() != null) ids.add(unit.getId());
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private static List<SatuanKerja> satuanKerjaTersedia() {
        Set<Long> ids = satuanKerjaIds();
        if (ids.isEmpty()) return new ArrayList<SatuanKerja>();
        Session s = HibernateUtil.openSession();
        try {
            return new ArrayList<SatuanKerja>(s.createCriteria(SatuanKerja.class)
                    .add(Restrictions.in("id", ids)).addOrder(Order.asc("kode"))
                    .addOrder(Order.asc("nama")).list());
        } finally { s.close(); }
    }

    @SuppressWarnings("unchecked")
    private static <T> T entity(Class<T> type, Long id) {
        Session s = HibernateUtil.openSession();
        try { return (T) s.get(type, id); }
        finally { s.close(); }
    }

    // ------------------------------------------------------------------- util
    private static Date tanggal(String value) {
        try { return Common.databaseDateFormat.get().parse(value); }
        catch (Exception e) { return null; }
    }

    private static Long id(String value) {
        String bersih = optional(value);
        if (bersih == null) return null;
        try {
            Long hasil = Long.valueOf(bersih);
            return hasil.longValue() > 0L ? hasil : null;
        } catch (Exception e) { return null; }
    }

    private static Long parameterId(HttpServletRequest request, String nama, String label) {
        String mentah = optional(request.getParameter(nama));
        if (mentah == null) return null;
        Long hasil = id(mentah);
        if (hasil == null) throw new IllegalArgumentException(label + " tidak sah.");
        return hasil;
    }

    private static String optional(String value) {
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }

    private static String text(String value, String fallback) {
        String hasil = optional(value);
        return hasil == null ? fallback : hasil;
    }

    private static void fail(JSONObject j, String code, String message) throws Exception {
        j.put("ok", false).put("code", code)
                .put("message", message == null ? "Operasi ditolak." : message);
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
