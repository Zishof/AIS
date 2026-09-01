package ais.common.newui.employ;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.RiwayatKerjaPegawai;
import ais.database.model.employ.RiwayatOrganisasiLainPegawai;
import ais.database.model.employ.RiwayatPendidikanPegawai;
import ais.database.model.payroll.PengajuanTransaksiPegawai;

/**
 * Kontrak native lima layar kepegawaian.
 *
 * <h3>Tiga layar riwayat — terkunci identitas</h3>
 * <p>Riwayat pendidikan, riwayat bekerja, dan riwayat organisasi lain adalah
 * layar swalayan: pegawai mengisi riwayatnya sendiri. Pada layar ZK, kotak
 * pemilih pegawai memang ada tetapi <b>diisi lalu dinonaktifkan</b> begitu
 * pegawai pemilik sesi diketahui, sehingga layar itu praktis terkunci pada
 * dirinya sendiri. Kontrak ini menegakkan hal yang sama secara mutlak:
 * pegawainya selalu diambil dari sesi dan tidak pernah dari parameter.</p>
 *
 * <p>Ketiganya berbagi bentuk yang hampir sama — nama, alamat, kedudukan,
 * rentang tahun, keterangan — sehingga dilayani satu kontrak dengan skema
 * kolom per mode. Klien membangun formulirnya dari skema itu, bukan dari daftar
 * kolom yang ditulis ulang di sisi klien.</p>
 *
 * <h3>Baris yang sudah diverifikasi terkunci</h3>
 * <p>Layar ZK hanya mengizinkan penghapusan baris yang <i>belum</i> berstatus
 * ok, dan penandaan status itu sendiri menuntut hak APPROVE. Kontrak ini
 * mengikutinya: baris terverifikasi tidak dapat diubah maupun dihapus, dan
 * <b>status tidak dapat diubah dari sini sama sekali</b>. Membuka penandaan
 * verifikasi lewat kontrak berarti pegawai berpotensi mengesahkan riwayatnya
 * sendiri — persis yang dicegah pemisahan hak pada layar lama.</p>
 *
 * <h3>Dua layar baca saja</h3>
 * <p><b>Pengajuan Pensiun</b> adalah daftar pegawai berstatus pensiun; pada
 * layar ZK tombol tambah pun disembunyikan untuk tampilan tersaring ini, dan
 * penyuntingan data pegawai tetap milik layar master pegawai.</p>
 *
 * <p><b>Persetujuan Pengajuan Transaksi Pegawai</b> disajikan baca saja dengan
 * alasan yang sama seperti antrean persetujuan lain: menyetujui bukan sekadar
 * menandai satu kolom melainkan melepas transaksi ke rangkaian penggajian dan
 * disposisi SOP berikutnya.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa pegawai ditolak untuk
 * layar riwayat, mutasi wajib POST beserta token CSRF.</p>
 */
public final class NewUiKepegawaianController {

    public static final String MODE_RIWAYAT_PENDIDIKAN = "riwayat_pendidikan";
    public static final String MODE_RIWAYAT_KERJA = "riwayat_kerja";
    public static final String MODE_RIWAYAT_ORGANISASI_LAIN = "riwayat_organisasi_lain";
    public static final String MODE_PENSIUN = "pensiun";
    public static final String MODE_PERSETUJUAN_TRANSAKSI = "persetujuan_transaksi_pegawai";

    private static final int BATAS_BARIS = 300;

    /** Satu kolom yang dapat diisi pengguna. */
    private static final class Kolom {
        final String nama;
        final String label;
        final boolean angka;
        final boolean wajib;

        Kolom(String nama, String label, boolean angka, boolean wajib) {
            this.nama = nama;
            this.label = label;
            this.angka = angka;
            this.wajib = wajib;
        }
    }

    private NewUiKepegawaianController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode kepegawaian tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, modul(mode), pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if (MODE_PENSIUN.equals(mode)) pensiun(json, request, action);
            else if (MODE_PERSETUJUAN_TRANSAKSI.equals(mode)) persetujuan(json, request, action);
            else riwayat(json, request, user, mode, action);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan kepegawaian. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKepegawaianController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_RIWAYAT_PENDIDIKAN.equals(mode) || MODE_RIWAYAT_KERJA.equals(mode)
                || MODE_RIWAYAT_ORGANISASI_LAIN.equals(mode) || MODE_PENSIUN.equals(mode)
                || MODE_PERSETUJUAN_TRANSAKSI.equals(mode);
    }

    /**
     * Modul penjaga rute; harus sama dengan awalan folder JSP sebelum
     * {@code /uiux/}. Layar persetujuan berada di modul payroll, sisanya employ.
     */
    static String modul(String mode) {
        return MODE_PERSETUJUAN_TRANSAKSI.equals(mode) ? "payroll" : "employ";
    }

    static String judul(String mode) {
        if (MODE_RIWAYAT_PENDIDIKAN.equals(mode)) return "Riwayat Pendidikan Pegawai";
        if (MODE_RIWAYAT_KERJA.equals(mode)) return "Riwayat Bekerja Pegawai";
        if (MODE_RIWAYAT_ORGANISASI_LAIN.equals(mode)) return "Riwayat Organisasi Lain";
        if (MODE_PENSIUN.equals(mode)) return "Pengajuan Pensiun";
        return "Persetujuan Pengajuan Transaksi Pegawai";
    }

    static Class<?> entity(String mode) {
        if (MODE_RIWAYAT_PENDIDIKAN.equals(mode)) return RiwayatPendidikanPegawai.class;
        if (MODE_RIWAYAT_KERJA.equals(mode)) return RiwayatKerjaPegawai.class;
        return RiwayatOrganisasiLainPegawai.class;
    }

    /** Kolom yang dapat diisi, per mode riwayat. */
    private static List<Kolom> kolom(String mode) {
        List<Kolom> k = new ArrayList<Kolom>();
        if (MODE_RIWAYAT_PENDIDIKAN.equals(mode)) {
            k.add(new Kolom("namaSekolah", "Nama Sekolah / Perguruan Tinggi", false, true));
            k.add(new Kolom("alamatSekolah", "Alamat", false, false));
            k.add(new Kolom("jurusan", "Jurusan", false, false));
            k.add(new Kolom("tahunMasuk", "Tahun Masuk", true, false));
            k.add(new Kolom("tahunLulus", "Tahun Lulus", true, false));
            k.add(new Kolom("noIjazah", "Nomor Ijazah", false, false));
            k.add(new Kolom("namaKepalaSekolah", "Nama Kepala Sekolah / Rektor", false, false));
            k.add(new Kolom("keterangan", "Keterangan", false, false));
            return k;
        }
        k.add(new Kolom("nama", "Nama Instansi / Organisasi", false, true));
        k.add(new Kolom("alamat", "Alamat", false, false));
        k.add(new Kolom("kedudukan", "Kedudukan / Jabatan", false, false));
        if (MODE_RIWAYAT_ORGANISASI_LAIN.equals(mode)) {
            k.add(new Kolom("periode", "Periode", false, false));
        }
        k.add(new Kolom("tahunMulai", "Tahun Mulai", true, false));
        k.add(new Kolom("tahunSelesai", "Tahun Selesai", true, false));
        k.add(new Kolom("pimpinan", "Pimpinan", false, false));
        k.add(new Kolom("keterangan", "Keterangan", false, false));
        return k;
    }

    // --------------------------------------------------------------- riwayat

    private static void riwayat(JSONObject j, HttpServletRequest request, Tbmuser user,
            String mode, String action) throws Exception {
        Pegawai pegawai = user.ambilPegawai();
        if (pegawai == null || pegawai.getId() == null) {
            throw new SecurityException("Halaman ini khusus untuk pegawai.");
        }
        List<Kolom> kolom = kolom(mode);

        if ("meta".equals(action)) {
            j.put("judul", judul(mode));
            j.put("mode", mode);
            j.put("pegawai", teks(pegawai.getNama()));
            j.put("csrfHeader", NewUiCsrfUtil.HEADER);
            j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
            JSONArray skema = new JSONArray();
            for (Kolom k : kolom) {
                skema.put(new JSONObject().put("nama", k.nama).put("label", k.label)
                        .put("angka", k.angka).put("wajib", k.wajib));
            }
            j.put("kolom", skema);
            // Status verifikasi TIDAK dapat diubah dari sini; klien menampilkannya
            // sebagai penanda, bukan sebagai kendali.
            j.put("bolehUbahStatus", false);
            j.put("catatanStatus", "Baris yang sudah diverifikasi terkunci: tidak dapat diubah "
                    + "maupun dihapus. Penandaan verifikasi dilakukan pihak berwenang di layar lama.");
            return;
        }
        if ("list".equals(action)) {
            JSONArray rows = new JSONArray();
            for (Object o : baris(mode, pegawai)) {
                rows.put(satuBaris(o, kolom));
            }
            j.put("rows", rows);
            j.put("jumlah", rows.length());
            return;
        }
        if ("save".equals(action)) {
            wajibMutasi(request);
            simpan(j, request, pegawai, mode, kolom);
            return;
        }
        if ("delete".equals(action)) {
            wajibMutasi(request);
            hapus(j, request, pegawai, mode);
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> baris(String mode, Pegawai pegawai) {
        Criteria c = HibernateUtil.currentSession().createCriteria(entity(mode))
                .add(Restrictions.eq("pegawai", pegawai))
                .addOrder(Order.desc("id"));
        c.setMaxResults(BATAS_BARIS);
        List<Object> hasil = new ArrayList<Object>();
        for (Object o : c.list()) {
            hasil.add(o);
        }
        return hasil;
    }

    private static JSONObject satuBaris(Object o, List<Kolom> kolom) throws Exception {
        JSONObject b = new JSONObject();
        b.put("id", angkaPanjang(panggil(o, "getId")));
        for (Kolom k : kolom) {
            Object v = panggil(o, getter(k.nama));
            b.put(k.nama, v == null ? (k.angka ? JSONObject.NULL : "") : v);
        }
        Object status = panggil(o, "getStatus");
        b.put("terverifikasi", Boolean.TRUE.equals(status));
        return b;
    }

    /**
     * Simpan satu baris riwayat milik pegawai pemilik sesi.
     *
     * <p>Kepemilikan diperiksa terhadap baris yang ADA di basis data, bukan
     * terhadap nilai kiriman klien: tanpa itu, mengirim id milik orang lain
     * cukup untuk menimpa riwayatnya.</p>
     */
    private static void simpan(JSONObject j, HttpServletRequest request, Pegawai pegawai,
            String mode, List<Kolom> kolom) throws Exception {
        Session session = HibernateUtil.currentSession();
        String idTeks = text(request.getParameter("id"), "").trim();
        Object baris;
        if (idTeks.length() == 0) {
            baris = entity(mode).newInstance();
            panggilSet(baris, "setPegawai", Pegawai.class, pegawai);
        } else {
            baris = session.get(entity(mode), idWajib(idTeks));
            if (baris == null) throw new IllegalArgumentException("Baris riwayat tidak ditemukan.");
            Object milik = panggil(baris, "getPegawai");
            Long pemilik = milik == null ? null : (Long) panggil(milik, "getId");
            if (pemilik == null || !pemilik.equals(pegawai.getId())) {
                throw new SecurityException("Baris riwayat ini bukan milik Anda.");
            }
            if (Boolean.TRUE.equals(panggil(baris, "getStatus"))) {
                throw new IllegalArgumentException(
                        "Baris ini sudah diverifikasi sehingga tidak dapat diubah.");
            }
        }

        for (Kolom k : kolom) {
            String nilai = request.getParameter(k.nama);
            if (nilai == null) continue;
            nilai = nilai.trim();
            if (k.wajib && nilai.length() == 0) {
                throw new IllegalArgumentException(k.label + " wajib diisi.");
            }
            if (k.angka) {
                panggilSet(baris, setter(k.nama), Integer.class, nilai.length() == 0 ? null : tahun(nilai, k.label));
            } else {
                panggilSet(baris, setter(k.nama), String.class, nilai);
            }
        }

        org.hibernate.Transaction tx = session.beginTransaction();
        try {
            Common.refreshSaveOrUpdate(session, (ais.database.model.GeneralValueObject) baris);
            session.flush();
            tx.commit();
        } catch (Exception e) {
            try { tx.rollback(); } catch (Exception ignored) { }
            throw e;
        }
        j.put("id", angkaPanjang(panggil(baris, "getId")));
        j.put("pesan", idTeks.length() == 0 ? "Riwayat ditambahkan." : "Riwayat diperbarui.");
    }

    private static void hapus(JSONObject j, HttpServletRequest request, Pegawai pegawai, String mode)
            throws Exception {
        Session session = HibernateUtil.currentSession();
        Object baris = session.get(entity(mode), idWajib(text(request.getParameter("id"), "")));
        if (baris == null) throw new IllegalArgumentException("Baris riwayat tidak ditemukan.");
        Object milik = panggil(baris, "getPegawai");
        Long pemilik = milik == null ? null : (Long) panggil(milik, "getId");
        if (pemilik == null || !pemilik.equals(pegawai.getId())) {
            throw new SecurityException("Baris riwayat ini bukan milik Anda.");
        }
        // Sama dengan layar ZK: yang sudah berstatus ok tidak boleh dihapus.
        if (Boolean.TRUE.equals(panggil(baris, "getStatus"))) {
            throw new IllegalArgumentException("Baris ini sudah diverifikasi sehingga tidak dapat dihapus.");
        }
        org.hibernate.Transaction tx = session.beginTransaction();
        try {
            session.delete(baris);
            session.flush();
            tx.commit();
        } catch (Exception e) {
            try { tx.rollback(); } catch (Exception ignored) { }
            throw e;
        }
        j.put("pesan", "Riwayat dihapus.");
    }

    // --------------------------------------------------------------- pensiun

    /** Daftar pegawai berstatus pensiun; baca saja. */
    @SuppressWarnings("unchecked")
    private static void pensiun(JSONObject j, HttpServletRequest request, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", judul(MODE_PENSIUN));
            j.put("bolehUbah", false);
            j.put("alasanBacaSaja", "Layar ini adalah tampilan tersaring atas data pegawai; "
                    + "penyuntingannya tetap di layar master pegawai. Pada layar lama pun tombol "
                    + "tambah disembunyikan untuk tampilan ini.");
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        if (ConstantValues.PENSIUN_PEGAWAI == null) {
            // Status pensiun belum dikonfigurasi; katakan apa adanya ketimbang
            // memulangkan daftar kosong yang terbaca sebagai "tidak ada pegawai".
            j.put("rows", new JSONArray());
            j.put("jumlah", 0);
            j.put("catatan", "Status pegawai \"Pensiun\" belum dikonfigurasi pada sistem ini.");
            return;
        }
        String kw = text(request.getParameter("q"), "").trim().toLowerCase();
        Criteria c = HibernateUtil.currentSession().createCriteria(Pegawai.class)
                .add(Restrictions.eq("statusPegawai", ConstantValues.PENSIUN_PEGAWAI))
                .addOrder(Order.asc("nama"));
        c.setMaxResults(BATAS_BARIS);
        JSONArray rows = new JSONArray();
        for (Object o : c.list()) {
            Pegawai p = (Pegawai) o;
            String nama = teks(p.getNama());
            String kode = teks(p.getMycode());
            if (kw.length() > 0 && !(nama.toLowerCase().contains(kw) || kode.toLowerCase().contains(kw))) {
                continue;
            }
            rows.put(new JSONObject().put("id", p.getId()).put("nama", nama).put("kode", kode)
                    .put("satuanKerja", p.getSatuanKerja() == null ? "" : teks(p.getSatuanKerja().getNama()))
                    .put("email", teks(p.getEmail())));
        }
        j.put("rows", rows);
        j.put("jumlah", rows.length());
        j.put("terpotong", rows.length() >= BATAS_BARIS);
    }

    // ----------------------------------------------------------- persetujuan

    /** Antrean pengajuan transaksi pegawai; baca saja. */
    private static void persetujuan(JSONObject j, HttpServletRequest request, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", judul(MODE_PERSETUJUAN_TRANSAKSI));
            j.put("bolehUbah", false);
            j.put("alasanBacaSaja", "Menyetujui pengajuan bukan sekadar menandai satu kolom: "
                    + "transaksi yang disetujui masuk ke rangkaian penggajian dan disposisi SOP "
                    + "berikutnya. Persetujuan tetap dilakukan di layar lama.");
            JSONArray status = new JSONArray();
            status.put("Pengajuan");
            status.put("Disetujui");
            j.put("pilihanStatus", status);
            j.put("statusBawaan", "Pengajuan");
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        String status = text(request.getParameter("status"), "Pengajuan");
        boolean disetujui = "Disetujui".equals(status);
        if (!disetujui && !"Pengajuan".equals(status)) {
            throw new IllegalArgumentException("Status tidak dikenal.");
        }
        // Persetujuan pada entity ini adalah penanda boolean beserta penyetujunya,
        // sehingga dapat disaring langsung di basis data.
        Query q = HibernateUtil.currentSession().createQuery(
                "select distinct t from PengajuanTransaksiPegawai t "
                        + "left join fetch t.pegawai p "
                        + "left join fetch t.jenisPengajuanTransaksiPegawai jp "
                        + "left join fetch t.disetujuiOleh d "
                        + "where " + (disetujui ? "t.setujui = true" : "(t.setujui is null or t.setujui = false)")
                        + " order by t.id desc");
        q.setMaxResults(BATAS_BARIS);
        JSONArray rows = new JSONArray();
        double total = 0;
        for (Object o : q.list()) {
            PengajuanTransaksiPegawai t = (PengajuanTransaksiPegawai) o;
            double nilai = t.getNilaiTransaksi() == null ? 0 : t.getNilaiTransaksi().doubleValue();
            total += nilai;
            rows.put(new JSONObject().put("id", t.getId())
                    .put("kode", teks(t.getKode()))
                    .put("nama", teks(t.getNama()))
                    .put("pegawai", t.getPegawai() == null ? "-" : teks(t.getPegawai().getNama()))
                    .put("jenis", t.getJenisPengajuanTransaksiPegawai() == null ? "-"
                            : teks(t.getJenisPengajuanTransaksiPegawai().getNama()))
                    .put("nilai", nilai)
                    .put("jumlahAngsur", t.getJumlahAngsur() == null ? 0 : t.getJumlahAngsur().intValue())
                    .put("periode", (t.getBulan() == null ? 0 : t.getBulan().intValue()) + "/"
                            + (t.getTahun() == null ? 0 : t.getTahun().intValue()))
                    .put("keterangan", teks(t.getKeterangan()))
                    .put("penyetuju", t.getDisetujuiOleh() == null ? "" : teks(t.getDisetujuiOleh().getUserId()))
                    .put("status", Boolean.TRUE.equals(t.getSetujui()) ? "Disetujui" : "Pengajuan"));
        }
        j.put("rows", rows);
        j.put("status", status);
        j.put("jumlah", rows.length());
        j.put("totalNilai", total);
        j.put("terpotong", rows.length() >= BATAS_BARIS);
    }

    // ------------------------------------------------------------------ util

    private static void wajibMutasi(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new SecurityException("Mutasi hanya dilayani lewat POST.");
        }
        if (!NewUiCsrfUtil.isValid(request)) {
            throw new SecurityException("Token CSRF tidak sah. Muat ulang halaman.");
        }
    }

    private static String getter(String nama) {
        return "get" + Character.toUpperCase(nama.charAt(0)) + nama.substring(1);
    }

    private static String setter(String nama) {
        return "set" + Character.toUpperCase(nama.charAt(0)) + nama.substring(1);
    }

    /**
     * Pemanggilan getter lewat refleksi.
     *
     * <p>Ketiga entity riwayat tidak berbagi antarmuka yang mendeklarasikan
     * kolomnya, sedangkan bentuk kontraknya sama. Refleksi menjaga kontrak tetap
     * satu; nama kolomnya sendiri tidak berasal dari klien melainkan dari skema
     * yang ditulis di kelas ini, sehingga tidak ada kolom sembarang yang dapat
     * dibaca atau ditulis lewat nama yang dikarang.</p>
     */
    private static Object panggil(Object target, String metode) {
        try {
            return target.getClass().getMethod(metode).invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static void panggilSet(Object target, String metode, Class<?> tipe, Object nilai) {
        try {
            target.getClass().getMethod(metode, tipe).invoke(target, nilai);
        } catch (Exception e) {
            throw new IllegalArgumentException("Kolom tidak dapat diisi: " + metode);
        }
    }

    private static Integer tahun(String nilai, String label) {
        try {
            int t = Integer.parseInt(nilai);
            if (t < 1900 || t > 2999) throw new NumberFormatException();
            return Integer.valueOf(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa tahun yang wajar.");
        }
    }

    private static Long idWajib(String nilai) {
        try {
            long l = Long.parseLong(text(nilai, "").trim());
            if (l <= 0) throw new NumberFormatException();
            return Long.valueOf(l);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Baris belum dipilih.");
        }
    }

    private static long angkaPanjang(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
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
