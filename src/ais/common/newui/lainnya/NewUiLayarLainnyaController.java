package ais.common.newui.lainnya;

import java.util.Calendar;
import java.util.List;

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
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.sekolah.Siswa;

/**
 * Kontrak native lima layar sisa yang tidak termasuk keluarga mana pun.
 *
 * <ul>
 *   <li><b>Publikasi Ilmiah (Artikel)</b> — daftar artikel beserta identitas
 *       penerbitannya.</li>
 *   <li><b>Alumni Siswa</b> — tampilan tersaring atas data siswa berstatus
 *       alumni.</li>
 *   <li><b>Terima Tagihan</b> — antrean persetujuan tagihan pengadaan.</li>
 *   <li><b>Dasbor Kegiatan Kemahasiswaan</b> dan <b>Kedosenan</b> — wadah tab
 *       atas layar kegiatan, organisasi, prestasi, dan karya.</li>
 * </ul>
 *
 * <h3>Tiga yang baca saja, dan alasannya berbeda-beda</h3>
 * <p><b>Alumni Siswa</b> adalah layar induk data siswa yang disaring status
 * keluarnya; penyuntingan data siswa tetap milik layar master siswa.
 * <b>Terima Tagihan</b> adalah mode persetujuan atas rantai pengadaan —
 * menyetujui tagihan melepaskan pembayaran, sehingga kontrak yang sekadar
 * menandai kolom akan memutus rangkaiannya. <b>Publikasi Ilmiah</b> disajikan
 * baca saja karena pengajuannya ditangani helper tersendiri beserta unggahan
 * berkas dan pemeriksaan plagiarisme.</p>
 *
 * <h3>Dua dasbor: direktori, bukan tab bersarang</h3>
 * <p>Keduanya wadah yang memuat layar lain. Panel "Dasbor" pada dasbor
 * kemahasiswaan dibangun langsung oleh helper sehingga tidak punya halaman
 * tersendiri; panel itu diumumkan tanpa rute beserta alasannya.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa pengguna ditolak, dan
 * seluruh aksi hanya membaca.</p>
 */
public final class NewUiLayarLainnyaController {

    public static final String MODE_ARTIKEL = "artikel";
    public static final String MODE_ALUMNI_SISWA = "alumni_siswa";
    public static final String MODE_TERIMA_TAGIHAN = "terima_tagihan";
    public static final String MODE_DASBOR_KEMAHASISWAAN = "dasbor_kemahasiswaan";
    public static final String MODE_DASBOR_KEDOSENAN = "dasbor_kedosenan";
    /**
     * Dasbor kegiatan milik mahasiswa yang sedang masuk.
     *
     * <p>Berbeda dari {@link #MODE_DASBOR_KEMAHASISWAAN}, yang melayani varian
     * Admin: di sana petugas memilih mahasiswanya di dalam tiap layar, di sini
     * seluruh tab terikat pada mahasiswa pemilik sesi. Daftar tabnya pun
     * berbeda -- varian ini punya "Form Kegiatan" yang tidak ada pada varian
     * Admin, jadi memakai ulang daftar tab Admin akan menghilangkan satu tab
     * tanpa gejala.</p>
     */
    public static final String MODE_DASBOR_KEMAHASISWAAN_SAYA = "dasbor_kemahasiswaan_saya";

    private static final int BATAS_BARIS = 200;

    /** Tab dasbor kegiatan kemahasiswaan: {label, rute}; rute kosong = tanpa halaman. */
    private static final String[][] TAB_KEMAHASISWAAN = {
            { "Dasbor", "" },
            { "Kegiatan Mahasiswa", "/pages/master/kegiatan_kemahasiswaan.zul" },
            { "Organisasi Mahasiswa", "/pages/master/organisasi_intra_kampus.zul" },
            { "Prestasi Mahasiswa", "/pages/master/prestasi_mahasiswa.zul" },
            { "Karya Mahasiswa", "/pages/master/penghargaan_mahasiswa.zul" },
            { "Catatan Mahasiswa", "/pages/master/catatan_mahasiswa.zul" },
    };

    /**
     * Tab dasbor kegiatan milik mahasiswa sendiri.
     *
     * <p>Disalin dari {@code DashboardKegiatanKemahasiswaan}, bukan dari varian
     * Admin: label dan susunannya berbeda, dan varian ini punya tab "Form
     * Kegiatan" yang tidak ada di sana. Dua tab pertama dibangun langsung oleh
     * helper ZK ({@code MahasiswaPunyaKegiatanKemahasiswaanHelper} dan
     * {@code MahasiswaPunyaOrganisasiIntraKampusHelper}) sehingga tidak punya
     * ZUL tersendiri yang dapat ditunjuk; keduanya ditandai tanpa rute, sama
     * seperti tab "Dasbor" pada varian Admin.</p>
     */
    private static final String[][] TAB_KEMAHASISWAAN_SAYA = {
            { "Kegiatan Kemahasiswaan", "" },
            { "Organisasi", "" },
            { "Prestasi", "/pages/master/prestasi_mahasiswa.zul" },
            { "Karya", "/pages/master/penghargaan_mahasiswa.zul" },
            { "Form Kegiatan", "/pages/master/formulir_kegiatan_peserta.zul" },
            { "Catatan Mahasiswa", "/pages/master/catatan_mahasiswa.zul" },
    };

    /** Label tab varian pemilik sesi; dipakai uji mandiri agar tidak menyimpang. */
    static String[] labelTabKemahasiswaanSaya() {
        String[] label = new String[TAB_KEMAHASISWAAN_SAYA.length];
        for (int i = 0; i < TAB_KEMAHASISWAAN_SAYA.length; i++) {
            label[i] = TAB_KEMAHASISWAAN_SAYA[i][0];
        }
        return label;
    }

    /** Tab dasbor kegiatan kedosenan. */
    private static final String[][] TAB_KEDOSENAN = {
            { "Kegiatan Dosen", "/pages/master/kegiatan_kedosenan.zul" },
            { "Organisasi Dosen", "/pages/master/organisasi_dosen.zul" },
            { "Prestasi Dosen", "/pages/master/prestasi_dosen.zul" },
            { "Karya Dosen", "/pages/master/penghargaan_dosen.zul" },
    };

    private NewUiLayarLainnyaController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode layar tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, modul(mode), pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if (MODE_DASBOR_KEMAHASISWAAN_SAYA.equals(mode)) {
                dasborSaya(json, user, action);
            } else if (MODE_DASBOR_KEMAHASISWAAN.equals(mode) || MODE_DASBOR_KEDOSENAN.equals(mode)) {
                dasbor(json, mode, action);
            } else if (MODE_ARTIKEL.equals(mode)) {
                artikel(json, request, action);
            } else if (MODE_ALUMNI_SISWA.equals(mode)) {
                alumni(json, request, action);
            } else {
                terimaTagihan(json, request, action);
            }
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat data. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLayarLainnyaController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_ARTIKEL.equals(mode) || MODE_ALUMNI_SISWA.equals(mode)
                || MODE_TERIMA_TAGIHAN.equals(mode) || MODE_DASBOR_KEMAHASISWAAN.equals(mode)
                || MODE_DASBOR_KEDOSENAN.equals(mode)
                || MODE_DASBOR_KEMAHASISWAAN_SAYA.equals(mode);
    }

    /** Modul penjaga rute; harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    static String modul(String mode) {
        if (MODE_ARTIKEL.equals(mode)) return "penelitiandanpengabdian";
        if (MODE_ALUMNI_SISWA.equals(mode)) return "sekolah";
        if (MODE_TERIMA_TAGIHAN.equals(mode)) return "asset";
        return "dashboard";
    }

    static String judul(String mode) {
        if (MODE_ARTIKEL.equals(mode)) return "Publikasi Ilmiah";
        if (MODE_ALUMNI_SISWA.equals(mode)) return "Alumni Siswa";
        if (MODE_TERIMA_TAGIHAN.equals(mode)) return "Terima Tagihan";
        if (MODE_DASBOR_KEMAHASISWAAN.equals(mode)) return "Kegiatan Kemahasiswaan";
        if (MODE_DASBOR_KEMAHASISWAAN_SAYA.equals(mode)) return "Kegiatan Kemahasiswaan Saya";
        return "Kegiatan Kedosenan";
    }

    // ---------------------------------------------------------------- dasbor

    private static void dasbor(JSONObject j, String mode, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        j.put("judul", judul(mode));
        j.put("mode", mode);
        String[][] tab = MODE_DASBOR_KEMAHASISWAAN.equals(mode) ? TAB_KEMAHASISWAAN : TAB_KEDOSENAN;
        JSONArray daftar = new JSONArray();
        for (int i = 0; i < tab.length; i++) {
            JSONObject o = new JSONObject().put("label", tab[i][0]);
            if (tab[i][1].length() == 0) {
                o.put("route", JSONObject.NULL);
                o.put("alasan", "Panel ini dibangun langsung oleh layar lama dan belum punya "
                        + "halaman tersendiri yang dapat dibuka native.");
            } else {
                o.put("route", tab[i][1]);
            }
            daftar.put(o);
        }
        j.put("tab", daftar);
        j.put("catatanCakupan", "Tiap panel adalah layar tersendiri. Yang tidak berrute atau di luar "
                + "hak akses peran Anda ditandai tidak tersedia.");
    }

    /**
     * Dasbor kegiatan milik mahasiswa pemilik sesi.
     *
     * <p>Seluruh tab terikat pada satu mahasiswa, dan id-nya diambil dari sesi
     * — bukan dari parameter. Layar lama pun menyusunnya begitu
     * ({@code Common.getCurrentUser().getMahasiswa()}), dan menerimanya dari
     * permintaan akan mengubah dasbor pribadi menjadi jalan membaca kegiatan
     * mahasiswa lain.</p>
     *
     * <p>Akun yang tidak terhubung ke data mahasiswa ditolak, bukan diberi
     * dasbor kosong: layar ini memang bukan untuknya, dan pesan yang jelas
     * lebih menolong daripada halaman yang tampak rusak.</p>
     */
    private static void dasborSaya(JSONObject j, Tbmuser user, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        ais.database.model.Mahasiswa mahasiswa = user == null ? null : user.getMahasiswa();
        if (mahasiswa == null || mahasiswa.getId() == null) {
            throw new SecurityException(
                    "Layar ini hanya untuk akun yang terhubung dengan data mahasiswa.");
        }
        j.put("judul", judul(MODE_DASBOR_KEMAHASISWAAN_SAYA));
        j.put("mode", MODE_DASBOR_KEMAHASISWAAN_SAYA);
        j.put("mahasiswaId", mahasiswa.getId());
        j.put("mahasiswa", mahasiswa.getNama() == null ? "" : mahasiswa.getNama());
        JSONArray daftar = new JSONArray();
        for (int i = 0; i < TAB_KEMAHASISWAAN_SAYA.length; i++) {
            JSONObject o = new JSONObject().put("label", TAB_KEMAHASISWAAN_SAYA[i][0]);
            if (TAB_KEMAHASISWAAN_SAYA[i][1].length() == 0) {
                o.put("route", JSONObject.NULL);
                o.put("alasan", "Panel ini dibangun langsung oleh layar lama dan belum punya "
                        + "halaman tersendiri yang dapat dibuka native.");
            } else {
                o.put("route", TAB_KEMAHASISWAAN_SAYA[i][1]);
                // Layar lama membuka tiap tab dengan ?mahasiswa=<id>; parameternya
                // ikut diumumkan supaya klien tidak perlu menebak namanya.
                o.put("param", new JSONObject().put("mahasiswa", mahasiswa.getId()));
            }
            daftar.put(o);
        }
        j.put("tab", daftar);
        j.put("catatanCakupan", "Tiap panel adalah layar tersendiri, terikat pada data Anda. "
                + "Yang tidak berrute atau di luar hak akses peran Anda ditandai tidak tersedia.");
    }

    // --------------------------------------------------------------- artikel

    @SuppressWarnings("unchecked")
    private static void artikel(JSONObject j, HttpServletRequest r, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", judul(MODE_ARTIKEL));
            j.put("bolehUbah", false);
            j.put("alasanBacaSaja", "Pengajuan artikel ditangani layar tersendiri beserta unggahan "
                    + "berkas dan pemeriksaan plagiarisme; pengajuan tetap dilakukan di layar lama.");
            int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
            JSONArray pilihan = new JSONArray();
            pilihan.put(0); // 0 = semua tahun
            for (int t = tahun; t >= tahun - 10; t--) {
                pilihan.put(t);
            }
            j.put("pilihanTahun", pilihan);
            j.put("tahunBawaan", 0);
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(Artikel.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .addOrder(Order.desc("id")).setMaxResults(BATAS_BARIS);
        int tahun = angka(r.getParameter("tahun"), 0);
        if (tahun > 0) c.add(Restrictions.eq("tahun", Integer.valueOf(tahun)));
        String q = text(r.getParameter("q"), "").trim();
        if (q.length() > 0) {
            c.add(Restrictions.ilike("judul", q, MatchMode.ANYWHERE));
        }
        JSONArray rows = new JSONArray();
        for (Object o : c.list()) {
            Artikel a = (Artikel) o;
            rows.put(new JSONObject().put("id", a.getId())
                    .put("judul", teks(a.getJudul()))
                    .put("tahun", a.getTahun() == null ? 0 : a.getTahun().intValue())
                    .put("issn", teks(a.getIssn()))
                    .put("volume", a.getVol() == null ? "" : String.valueOf(a.getVol()))
                    .put("nomor", teks(a.getNomor()))
                    .put("bahasa", teks(a.getBahasa()))
                    .put("status", teks(a.getStatus()))
                    .put("terindeksSitasi", Boolean.TRUE.equals(a.getTelahTerindeksSitasi()))
                    .put("tanggalPublikasi", tanggal(a.getTanggalPublikasi())));
        }
        j.put("rows", rows);
        j.put("jumlah", rows.length());
        j.put("terpotong", rows.length() >= BATAS_BARIS);
    }

    // ---------------------------------------------------------------- alumni

    /**
     * Alumni siswa: siswa dengan status keluar alumni.
     *
     * <p>Nilai {@code 1} pada status keluar adalah penanda alumni yang dipakai
     * layar ZK ({@code SiswaAction} dengan {@code alumni = true}). Angkanya
     * disalin apa adanya; menebak penanda lain akan menghasilkan daftar yang
     * tampak masuk akal namun salah orang.</p>
     */
    @SuppressWarnings("unchecked")
    private static void alumni(JSONObject j, HttpServletRequest r, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", judul(MODE_ALUMNI_SISWA));
            j.put("bolehUbah", false);
            j.put("alasanBacaSaja", "Layar ini tampilan tersaring atas data siswa; penyuntingannya "
                    + "tetap di layar master siswa.");
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(Siswa.class)
                .add(Restrictions.eq("statusKeluar.id", Long.valueOf(1L)))
                .addOrder(Order.asc("nama")).setMaxResults(BATAS_BARIS);
        String q = text(r.getParameter("q"), "").trim();
        if (q.length() > 0) {
            c.add(Restrictions.or(Restrictions.ilike("nama", q, MatchMode.ANYWHERE),
                    Restrictions.ilike("nim", q, MatchMode.ANYWHERE)));
        }
        JSONArray rows = new JSONArray();
        for (Object o : c.list()) {
            Siswa s = (Siswa) o;
            rows.put(new JSONObject().put("id", s.getId())
                    .put("nama", teks(s.getNama()))
                    .put("nis", teks(s.getNim()))
                    .put("jenisKelamin", teks(s.getJenisKelamin()))
                    .put("tahunMasuk", s.getTahunMasuk() == null ? 0 : s.getTahunMasuk().intValue())
                    .put("sekolahAsal", teks(s.getSekolahAsal()))
                    .put("email", teks(s.getAlamatEmail()))
                    .put("telepon", teks(s.getTeleponSiswa())));
        }
        j.put("rows", rows);
        j.put("jumlah", rows.length());
        j.put("terpotong", rows.length() >= BATAS_BARIS);
    }

    // --------------------------------------------------------- terima tagihan

    @SuppressWarnings("unchecked")
    private static void terimaTagihan(JSONObject j, HttpServletRequest r, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", judul(MODE_TERIMA_TAGIHAN));
            j.put("bolehUbah", false);
            j.put("alasanBacaSaja", "Menyetujui tagihan melepaskan pembayaran pada rantai pengadaan; "
                    + "kontrak yang sekadar menandai kolom akan memutus rangkaiannya. "
                    + "Persetujuan tetap dilakukan di layar lama.");
            JSONArray status = new JSONArray();
            status.put("Semua");
            status.put("Belum Disetujui");
            status.put("Disetujui");
            j.put("pilihanStatus", status);
            j.put("statusBawaan", "Belum Disetujui");
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        String status = text(r.getParameter("status"), "Belum Disetujui");
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(SaldoAwalMasterAsset.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .addOrder(Order.desc("id")).setMaxResults(BATAS_BARIS);
        if ("Disetujui".equals(status)) {
            c.add(Restrictions.isNotNull("disetujuiOleh"));
        } else if ("Belum Disetujui".equals(status)) {
            c.add(Restrictions.isNull("disetujuiOleh"));
        } else if (!"Semua".equals(status)) {
            throw new IllegalArgumentException("Status tidak dikenal.");
        }
        JSONArray rows = new JSONArray();
        double total = 0;
        for (Object o : c.list()) {
            SaldoAwalMasterAsset t = (SaldoAwalMasterAsset) o;
            double nilai = t.getNilai() == null ? 0 : t.getNilai().doubleValue();
            total += nilai;
            rows.put(new JSONObject().put("id", t.getId())
                    .put("kode", teks(t.getKode()))
                    .put("kodeTagihan", teks(t.getKodeTagihan()))
                    .put("keterangan", teks(t.getKeterangan()))
                    .put("satuanKerja", t.getSatuanKerja() == null ? "" : teks(t.getSatuanKerja().getNama()))
                    .put("nilai", nilai)
                    .put("dibayar", t.getDibayar() == null ? 0 : t.getDibayar().doubleValue())
                    .put("lunas", Boolean.TRUE.equals(t.getLunas()))
                    .put("tanggal", tanggal(t.getTanggalPembuatan()))
                    .put("tanggalTagihan", tanggal(t.getTanggalTagihan()))
                    .put("penyetuju", t.getDisetujuiOleh() == null ? ""
                            : teks(t.getDisetujuiOleh().getUserId()))
                    .put("status", t.getDisetujuiOleh() == null ? "Belum Disetujui" : "Disetujui"));
        }
        j.put("rows", rows);
        j.put("status", status);
        j.put("jumlah", rows.length());
        j.put("totalNilai", total);
        j.put("terpotong", rows.length() >= BATAS_BARIS);
    }

    // ------------------------------------------------------------------ util

    private static int angka(String nilai, int bawaan) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return bawaan;
        try {
            int t = Integer.parseInt(v);
            if (t != 0 && (t < 1900 || t > 2999)) throw new NumberFormatException();
            return t;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tahun tidak sah.");
        }
    }

    private static String tanggal(java.util.Date d) {
        return d == null ? "" : new java.text.SimpleDateFormat("dd-MM-yyyy").format(d);
    }

    private static String teks(String s) {
        return s == null ? "" : s;
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static void fail(JSONObject json, String code, String message) {
        try {
            json.put("ok", false);
            json.put("code", code);
            json.put("message", message == null ? "" : message);
        } catch (Exception ignored) { }
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
