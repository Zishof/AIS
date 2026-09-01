package ais.common.newui.laporan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import ais.database.model.Tbmuser;

/**
 * Kontrak native untuk laporan Jasper yang parameternya sederhana.
 *
 * <p><b>Mengapa satu controller untuk banyak laporan.</b> Puluhan menu laporan
 * hanya membuka jendela ZK yang menyusun beberapa parameter (periode, tahun,
 * bulan, atau satu-dua id relasi) lalu menyerahkan render ke Jasper. Menulis
 * satu controller per laporan berarti menyalin pola yang sama berulang kali;
 * yang berbeda hanya <i>template</i> dan <i>daftar filternya</i>. Karena itu
 * keduanya dijadikan data — sebuah registri — dan alur kerjanya ditulis sekali.</p>
 *
 * <p><b>Registri, bukan tebakan.</b> Setiap entri disalin dari kelas ZK-nya:
 * nama template persis seperti argumen {@code Report.generate*Report(...)}, dan
 * nama parameter persis seperti kunci {@code parameters.put(...)}. Bila sebuah
 * laporan menyusun sendiri koleksi datanya (mis. membangun {@code maps}), ia
 * TIDAK dimasukkan ke sini melainkan tetap butuh controller khusus — memaksakan
 * pola deklaratif pada laporan semacam itu hanya akan menghasilkan PDF kosong.</p>
 *
 * <p><b>Turunan otomatis.</b> Beberapa template menuntut parameter turunan yang
 * pada ZK dihitung dari filter lain: {@code nama_bulan} dari {@code bulan}
 * (indeks 1–12 pada {@link Common#BULAN}), serta {@code label_mulai}/
 * {@code label_sampai} dari periode. Turunan itu dihitung di sini agar klien
 * tidak perlu tahu detail template.</p>
 *
 * <p><b>Fail-closed.</b> Halaman yang tidak terdaftar ditolak; filter wajib yang
 * kosong ditolak dengan pesan jelas; id relasi yang tidak dipilih dikirim -1
 * seperti ZK, sehingga kueri laporan tidak pernah melebar tanpa filter.</p>
 */
public final class NewUiLaporanUmumController {

    private static final String MODULE = "root/report";

    // ------------------------------------------------------------- deskripsi
    /** Jenis filter yang dikenal klien desktop. */
    static final String TIPE_TAHUN = "tahun";
    static final String TIPE_BULAN = "bulan";
    static final String TIPE_TANGGAL = "tanggal";
    static final String TIPE_RELASI = "relasi";
    static final String TIPE_TEKS = "teks";

    /** Satu filter laporan: nama parameter Jasper + cara klien memintanya. */
    static final class Filter {
        final String nama, label, tipe, entity;
        final boolean wajib;
        /**
         * Bila diisi, controller ikut mengirim parameter bernama ini berisi
         * NAMA entity terpilih. Beberapa template mencetak nama unit pada kop
         * laporan, dan pada ZK nilainya diambil dari objek terpilih — bukan
         * dikirim klien, sehingga tidak dapat dipalsukan dari sisi aplikasi.
         */
        final String paramNama;
        Filter(String nama, String label, String tipe, boolean wajib, String entity) {
            this(nama, label, tipe, wajib, entity, null);
        }
        Filter(String nama, String label, String tipe, boolean wajib, String entity, String paramNama) {
            this.nama = nama; this.label = label; this.tipe = tipe;
            this.wajib = wajib; this.entity = entity; this.paramNama = paramNama;
        }
        /** Relasi yang sekaligus mengirim nama entity pada parameter lain. */
        static Filter relasiBernama(String nama, String label, String entity, boolean wajib,
                String paramNama) {
            return new Filter(nama, label, TIPE_RELASI, wajib, entity, paramNama);
        }
        static Filter tahun(boolean wajib) { return new Filter("tahun", "Tahun", TIPE_TAHUN, wajib, null); }
        static Filter bulan(boolean wajib) { return new Filter("bulan", "Bulan", TIPE_BULAN, wajib, null); }
        static Filter mulai() { return new Filter("mulai", "Tanggal Mulai", TIPE_TANGGAL, true, null); }
        static Filter sampai() { return new Filter("sampai", "Tanggal Sampai", TIPE_TANGGAL, true, null); }
        static Filter relasi(String nama, String label, String entity, boolean wajib) {
            return new Filter(nama, label, TIPE_RELASI, wajib, entity);
        }
        static Filter teks(String nama, String label) { return new Filter(nama, label, TIPE_TEKS, false, null); }
    }

    /** Satu laporan: judul, template Jasper, filter, dan parameter tetapnya. */
    static final class Laporan {
        final String judul, template;
        final List<Filter> filter;
        /**
         * Parameter bernilai tetap yang dituntut template namun tidak berasal
         * dari masukan pengguna — mis. {@code nama_laporan} pada laporan arus
         * harian, yang pada ZK selalu bernilai sama karena dihitung dari
         * {@code Calendar.getMaximum(DAY_OF_MONTH)}.
         */
        final Map<String, String> tetap = new LinkedHashMap<String, String>();
        Laporan(String judul, String template, Filter... filter) {
            this.judul = judul; this.template = template;
            this.filter = new ArrayList<Filter>();
            for (int i = 0; i < filter.length; i++) this.filter.add(filter[i]);
        }
        Laporan tetap(String nama, String nilai) { tetap.put(nama, nilai); return this; }
    }

    private static final Map<String, Laporan> REGISTRI = new LinkedHashMap<String, Laporan>();
    static {
        // --- Penggajian -----------------------------------------------------
        REGISTRI.put("payroll_pegawai",
                new Laporan("Daftar Pegawai", "payroll/Laporan_Pegawai"));
        REGISTRI.put("payroll_lembur",
                new Laporan("Daftar Lembur Pegawai", "payroll/Laporan_Lembur_Pegawai",
                        Filter.tahun(true), Filter.bulan(true)));
        REGISTRI.put("payroll_pendaftaran_tenaga_kerja",
                new Laporan("Pendaftaran Tenaga Kerja", "payroll/Laporan_Pendaftaran_Tenaga_Kerja",
                        Filter.tahun(true), Filter.bulan(true)));
        REGISTRI.put("payroll_upah",
                new Laporan("Daftar Upah Tenaga Kerja", "payroll/Laporan_Upah_Pegawai",
                        Filter.tahun(true)));

        // --- Tata kelola surat ---------------------------------------------
        REGISTRI.put("surat_statistik",
                new Laporan("Statistik Surat Masuk dan Keluar",
                        "surat/laporan_statistic_surat_masuk_dan_keluar",
                        Filter.mulai(), Filter.sampai()));

        // --- Perpustakaan ---------------------------------------------------
        REGISTRI.put("library_saldo_awal",
                new Laporan("Saldo Item Awal", "library/saldo_awal_semua",
                        Filter.relasi("id", "Perpustakaan", "ais.database.model.library.Perpustakaan", true)));
        REGISTRI.put("library_pemesanan_pengadaan",
                new Laporan("Pemesanan Pengadaan Item", "library/pemesanan_pengadaan_semua",
                        perpustakaan(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_penerimaan_pengadaan",
                new Laporan("Penerimaan Pengadaan Item", "library/penerimaan_pengadaan_semua",
                        perpustakaan(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_retur_pengadaan",
                new Laporan("Retur Pengadaan Item", "library/retur_pengadaan_semua",
                        perpustakaan(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_transfer_pengadaan",
                new Laporan("Transfer Item", "library/transfer_pengadaan_semua",
                        perpustakaan(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_terima_pengadaan",
                new Laporan("Terima Transfer Item", "library/terima_pengadaan_semua",
                        perpustakaan(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_peminjaman_per_anggota",
                new Laporan("Peminjaman Per Anggota", "library/peminjaman_per_anggota_pengadaan_semua",
                        perpustakaan(), anggota(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_peminjaman_belum_kembali",
                new Laporan("Peminjaman Belum Dikembalikan",
                        "library/peminjaman_per_anggota_belum_dikembalikan_pengadaan_semua",
                        perpustakaan(), anggota(), Filter.mulai(), Filter.sampai()));
        REGISTRI.put("library_pengembalian_per_anggota",
                new Laporan("Pengembalian Per Anggota", "library/pengembalian_per_anggota_pengadaan_semua",
                        perpustakaan(), anggota(), Filter.mulai(), Filter.sampai()));

        REGISTRI.put("library_stok_item",
                new Laporan("Monitor Stok Item", "library/stok_item",
                        perpustakaan(),
                        Filter.relasi("item", "Item", "ais.database.model.library.Item", false),
                        Filter.mulai(), Filter.sampai()));

        // --- Akuntansi ------------------------------------------------------
        REGISTRI.put("akunting_arus_kas",
                new Laporan("Laporan Arus Kas", "akunting/laporan_arus_12_bulan",
                        new Filter("tahun1", "Tahun", TIPE_TAHUN, true, null),
                        Filter.relasi("akun", "Akun", "ais.database.model.akunting.Akun", true),
                        Filter.relasi("satuan_kerja", "Satuan Kerja",
                                "ais.database.model.rab.SatuanKerja", false)));
        // nama_laporan pada ZK dibentuk dari Calendar.getMaximum(DAY_OF_MONTH)
        // yang nilainya selalu 31, sehingga di sini cukup ditetapkan.
        REGISTRI.put("akunting_arus_harian",
                new Laporan("Laporan Arus Harian", "akunting/laporan_arus_31_hari",
                        new Filter("tahun1", "Tahun", TIPE_TAHUN, true, null),
                        new Filter("bulan1", "Bulan", TIPE_BULAN, true, null),
                        Filter.relasi("akun", "Akun", "ais.database.model.akunting.Akun", true),
                        Filter.relasi("satuan_kerja", "Satuan Kerja",
                                "ais.database.model.rab.SatuanKerja", false))
                        .tetap("nama_laporan", "akunting/laporan_arus_31_hari"));

        // --- Penelitian dan pengabdian --------------------------------------
        REGISTRI.put("penelitian_rekap_artikel",
                new Laporan("Rekap Publikasi Ilmiah / Jurnal",
                        "penelitiandanpengabdian/Rekap_Artikel",
                        Filter.relasiBernama("fakultas", "Fakultas",
                                "ais.database.model.Fakultas", false, "fakultas_nama"),
                        Filter.relasiBernama("jurusan", "Jurusan",
                                "ais.database.model.Jurusan", false, "jurusan_nama"),
                        Filter.teks("judul", "Judul"),
                        Filter.teks("userid", "User ID")));

        // --- Anggaran (RAB) --------------------------------------------------
        REGISTRI.put("rab_realisasi_per_jenis",
                new Laporan("Realisasi Anggaran Per Jenis Item",
                        "rab/Realisasi_Anggaran_Per_Jenis_Item_Bulanan",
                        Filter.relasiBernama("satuan_kerja_id", "Satuan Kerja",
                                "ais.database.model.rab.SatuanKerja", true, "satuan_kerja"),
                        Filter.tahun(true), Filter.bulan(true),
                        new Filter("tanggal_mulai", "Tanggal Mulai", TIPE_TANGGAL, false, null),
                        new Filter("tanggal_selesai", "Tanggal Selesai", TIPE_TANGGAL, false, null)));
    }

    private static Filter perpustakaan() {
        return Filter.relasi("perpustakaan", "Perpustakaan",
                "ais.database.model.library.Perpustakaan", true);
    }

    private static Filter anggota() {
        return Filter.relasi("anggota", "Anggota", "ais.database.model.library.Anggota", false);
    }

    private NewUiLaporanUmumController() { }

    /** Daftar kunci laporan; dipakai self-test agar registri tidak menyimpang. */
    public static String[] semuaLaporan() {
        return REGISTRI.keySet().toArray(new String[0]);
    }

    /** Template Jasper sebuah laporan; dipakai self-test. */
    public static String templateUntuk(String kunci) {
        Laporan l = REGISTRI.get(kunci);
        return l == null ? null : l.template;
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String kunci, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            Laporan laporan = REGISTRI.get(kunci);
            if (laporan == null) throw new IllegalArgumentException("Laporan tidak terdaftar.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, laporan);
            else if ("lookup".equals(action)) lookup(json, request, laporan);
            else if ("export".equals(action) || "export_pdf".equals(action)) cetak(json, request, laporan, kunci);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses laporan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanUmumController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta
    /** Menjelaskan filter yang harus ditampilkan klien, beserta nilai bawaan. */
    private static void meta(JSONObject j, Laporan laporan) throws Exception {
        j.put("judul", laporan.judul).put("template", laporan.template);
        JSONArray arr = new JSONArray();
        for (Filter f : laporan.filter) {
            JSONObject d = new JSONObject();
            d.put("nama", f.nama).put("label", f.label).put("tipe", f.tipe).put("wajib", f.wajib);
            if (f.entity != null) d.put("entity", f.entity);
            arr.put(d);
        }
        j.put("filter", arr);

        Calendar c = Calendar.getInstance();
        JSONArray tahun = new JSONArray();
        for (int t = c.get(Calendar.YEAR) + 1; t >= c.get(Calendar.YEAR) - 5; t--) tahun.put(t);
        j.put("pilihanTahun", tahun);
        JSONArray bulan = new JSONArray();
        for (int i = 0; i < Common.BULAN.length; i++) {
            bulan.put(new JSONObject().put("nilai", i + 1).put("nama", Common.BULAN[i]));
        }
        j.put("pilihanBulan", bulan);
        j.put("mulaiBawaan", Common.databaseDateFormat.get().format(awalBulan()));
        j.put("sampaiBawaan", Common.databaseDateFormat.get().format(new Date()));
    }

    // ---------------------------------------------------------------- lookup
    /**
     * Pilihan untuk filter relasi. Entity dibatasi pada yang benar-benar
     * dideklarasikan laporan ini, sehingga endpoint tidak bisa dipakai
     * membaca entity sembarangan.
     */
    private static void lookup(JSONObject j, HttpServletRequest r, Laporan laporan) throws Exception {
        String nama = text(r.getParameter("filter"), "");
        Filter dipilih = null;
        for (Filter f : laporan.filter) {
            if (f.nama.equals(nama) && TIPE_RELASI.equals(f.tipe)) { dipilih = f; break; }
        }
        if (dipilih == null) throw new IllegalArgumentException("Filter relasi tidak dikenal.");

        String q = text(r.getParameter("q"), "");
        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Class<?> kelas = Class.forName(dipilih.entity);
            Criteria c = s.createCriteria(kelas).setMaxResults(50);
            if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
            try { c.addOrder(Order.asc("nama")); } catch (Exception abaikan) { }
            for (Object o : c.list()) {
                arr.put(new JSONObject()
                        .put("id", nilaiProperti(o, "getId"))
                        .put("nama", String.valueOf(nilaiProperti(o, "getNama"))));
            }
        } finally { s.close(); }
        j.put("pilihan", arr).put("filter", nama).put("total", arr.length());
    }

    /** Nama entity untuk parameter turunan; string kosong bila tidak ditemukan. */
    private static String namaEntity(String kelas, Long id) {
        if (kelas == null || id == null) return "";
        Session s = HibernateUtil.openSession();
        try {
            Object obyek = s.get(Class.forName(kelas), id);
            return obyek == null ? "" : String.valueOf(nilaiProperti(obyek, "getNama"));
        } catch (Exception e) {
            return "";
        } finally {
            try { s.close(); } catch (Exception ignored) { }
        }
    }

    private static Object nilaiProperti(Object obyek, String getter) {
        try { return obyek.getClass().getMethod(getter).invoke(obyek); }
        catch (Exception e) { return ""; }
    }

    // ----------------------------------------------------------------- cetak
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r, Laporan laporan, String kunci) throws Exception {
        Map parameters = ais.common.HashMapGenerator.getRand();
        for (Filter f : laporan.filter) {
            String mentah = text(r.getParameter(f.nama), "");
            if (mentah.length() == 0) {
                if (f.wajib) throw new IllegalArgumentException(f.label + " wajib diisi.");
                // Tidak dipilih: id relasi dikirim -1 seperti ZK; filter lain dilewati.
                if (TIPE_RELASI.equals(f.tipe)) {
                    parameters.put(f.nama, Long.valueOf(-1L));
                    if (f.paramNama != null) parameters.put(f.paramNama, "");
                }
                continue;
            }
            if (TIPE_TAHUN.equals(f.tipe) || TIPE_BULAN.equals(f.tipe)) {
                Integer angka = angka(mentah);
                if (angka == null) throw new IllegalArgumentException(f.label + " harus berupa angka.");
                parameters.put(f.nama, angka);
                if (TIPE_BULAN.equals(f.tipe)) {
                    // Template memakai nama_bulan; ZK mengambilnya dari indeks 1-12.
                    parameters.put("nama_bulan",
                            angka >= 1 && angka <= Common.BULAN.length ? Common.BULAN[angka - 1] : "");
                }
            } else if (TIPE_TANGGAL.equals(f.tipe)) {
                Date tanggal = tanggal(mentah);
                if (tanggal == null) throw new IllegalArgumentException(f.label + " bukan tanggal yang sah.");
                parameters.put(f.nama, Common.databaseDateFormat.get().format(tanggal));
                parameters.put("label_" + f.nama, Common.dateFormat4.get().format(tanggal));
            } else if (TIPE_RELASI.equals(f.tipe)) {
                Long id = id(mentah);
                if (id == null) throw new IllegalArgumentException(f.label + " tidak sah.");
                parameters.put(f.nama, id);
                if (f.paramNama != null) {
                    // Nama diambil dari basis data, bukan dari klien, supaya
                    // kop laporan tidak dapat dipalsukan lewat parameter.
                    parameters.put(f.paramNama, namaEntity(f.entity, id));
                }
            } else {
                parameters.put(f.nama, mentah);
            }
        }

        // Parameter tetap ditambahkan terakhir agar tidak dapat ditimpa masukan.
        for (Map.Entry<String, String> e : laporan.tetap.entrySet()) {
            parameters.put(e.getKey(), e.getValue());
        }

        JasperPdfUtil.tulis(j, laporan.template, parameters, kunci, laporan.judul);
    }

    // ------------------------------------------------------------------ util
    private static Date awalBulan() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    private static Date tanggal(String v) {
        try { return Common.databaseDateFormat.get().parse(v); } catch (Exception ignored) { }
        try { return new Date(Long.parseLong(v)); } catch (Exception ignored) { }
        return null;
    }

    private static Integer angka(String v) {
        try { return Integer.valueOf(v); } catch (Exception e) { return null; }
    }

    private static Long id(String v) {
        try { return Long.valueOf(v); } catch (Exception e) { return null; }
    }

    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }
}
