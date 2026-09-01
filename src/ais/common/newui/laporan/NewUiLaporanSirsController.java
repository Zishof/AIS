package ais.common.newui.laporan;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.TransaksiMedis;

/**
 * Kontrak native enam laporan SIRS.
 *
 * <h3>Koreksi atas anggapan sebelumnya</h3>
 * <p>Keenam laporan ini pernah dinyatakan tidak dapat dikonversi karena
 * "kelas-kelasnya tidak ada di repositori ini". <b>Anggapan itu keliru.</b>
 * Keenamnya ada, di bawah {@code ais.action.report.format1.sirs.*}. Yang saya
 * cari dulu adalah paket yang saya tebak sendiri
 * ({@code sirs.action.report.window.*}), bukan nama kelasnya. Seluruh 44 adaptor
 * laporan SIRS ternyata punya kelas sumber di pohon kode ini.</p>
 *
 * <h3>Parameter dibaca dari layar ZK, bukan dari template</h3>
 * <p>Nama dan tipe parameter Jasper diambil apa adanya dari
 * {@code generateParameter()} milik tiap jendela ZK. Menebaknya dari nama field
 * di template akan menghasilkan laporan yang tersusun tetapi salah isinya —
 * bentuk kegagalan yang tidak menimbulkan galat apa pun.</p>
 *
 * <h3>Dua keluarga laporan</h3>
 * <ul>
 *   <li><b>Periodik</b> — disaring tahun/bulan (dan status pada Ranap Per
 *       Ruangan). Tidak memerlukan pilihan entitas.</li>
 *   <li><b>Per objek</b> — menuntut satu pendaftaran, pasien, transaksi, atau
 *       pembayaran yang dipilih lebih dulu. Layar ZK memakai banbox pencari;
 *       jalur native memakai aksi {@code lookup} dengan kata kunci.</li>
 * </ul>
 *
 * <p>Seluruh aksi kontrak ini <b>hanya membaca</b>. Tidak ada yang disimpan;
 * yang dihasilkan hanyalah berkas PDF.</p>
 */
public final class NewUiLaporanSirsController {

    private static final String MODULE = "root/report";

    /** Batas jumlah baris hasil pencarian acuan. */
    private static final int BATAS_CARI = 50;

    private NewUiLaporanSirsController() { }

    // ------------------------------------------------------------- jenis

    /** Keterangan satu laporan: template Jasper, judul, dan permukaan saringannya. */
    private static final class Jenis {
        final String kode;
        final String judul;
        final String template;
        final String[] saringan;

        Jenis(String kode, String judul, String template, String[] saringan) {
            this.kode = kode;
            this.judul = judul;
            this.template = template;
            this.saringan = saringan;
        }
    }

    static final String S_TAHUN = "tahun";
    static final String S_BULAN = "bulan";
    static final String S_STATUS = "status";
    static final String S_PENDAFTARAN = "pendaftaran";
    static final String S_PASIEN = "pasien";
    static final String S_TRANSAKSI = "transaksi";
    static final String S_PEMBAYARAN = "pembayaran";

    /**
     * Enam laporan yang dilayani kontrak ini.
     *
     * <p>Kode laporan sengaja sama dengan nama template Jasper-nya supaya
     * hubungan keduanya tidak perlu ditelusuri lewat tabel lain.</p>
     */
    private static Jenis jenis(String kode) {
        if ("data_pasien_rawat_inap".equals(kode)) {
            return new Jenis(kode, "Laporan Data Pasien Rawat Inap",
                    "sirs/data_pasien_rawat_inap", new String[] { S_TAHUN, S_BULAN });
        }
        if ("ranap_laporan_perruangan".equals(kode)) {
            return new Jenis(kode, "Laporan Ranap Per Ruangan",
                    "sirs/ranap_laporan_perruangan", new String[] { S_TAHUN, S_BULAN, S_STATUS });
        }
        if ("informasi_tagihan".equals(kode)) {
            return new Jenis(kode, "Laporan Informasi Tagihan", "sirs/informasi_tagihan",
                    new String[] { S_PENDAFTARAN, S_PASIEN, S_TRANSAKSI });
        }
        if ("informasi_biaya_dan_retur".equals(kode)) {
            return new Jenis(kode, "Laporan Informasi Biaya dan Retur",
                    "sirs/informasi_biaya_dan_retur",
                    new String[] { S_PENDAFTARAN, S_PASIEN, S_TRANSAKSI });
        }
        if ("struk_pembayaran".equals(kode)) {
            return new Jenis(kode, "Laporan Bukti Pembayaran", "sirs/struk_pembayaran",
                    new String[] { S_PEMBAYARAN });
        }
        if ("tracer_pasien".equals(kode)) {
            return new Jenis(kode, "Laporan Tracer Pasien", "sirs/tracer_pasien",
                    new String[] { S_PENDAFTARAN });
        }
        throw new IllegalArgumentException("Jenis laporan SIRS tidak dikenal.");
    }

    public static boolean jenisDikenal(String kode) {
        try {
            jenis(kode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ------------------------------------------------------------ handle

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String kode, String pageKey) throws Exception {
        String action = text(request.getParameter("action"), "meta");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!jenisDikenal(kode)) {
                throw new IllegalArgumentException("Jenis laporan SIRS tidak dikenal.");
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Jenis j = jenis(kode);
            if ("meta".equals(action)) meta(json, request, j);
            else if ("lookup".equals(action)) lookup(json, request);
            else if ("export".equals(action)) cetak(json, request, j);
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
            fail(json, "INTERNAL_ERROR", "Laporan gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanSirsController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // -------------------------------------------------------------- meta

    /**
     * Umumkan filter dalam bentuk yang <b>sama persis</b> dengan kontrak laporan
     * generik ({@code NewUiLaporanUmumController}).
     *
     * <p>Bentuk yang sama bukan kebetulan: halaman laporan generik di klien
     * sudah membangun formulirnya dari deskripsi ini, dan memakainya kembali
     * berarti keenam laporan SIRS tidak menuntut halaman baru sama sekali.
     * Kalau bentuknya dibuat sendiri, akan ada dua deskripsi filter yang harus
     * dijaga sama tiap kali klien berubah.</p>
     */
    private static void meta(JSONObject j, HttpServletRequest request, Jenis jenis) throws Exception {
        j.put("judul", jenis.judul);
        j.put("kode", jenis.kode);
        j.put("template", jenis.template);
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));

        JSONArray arr = new JSONArray();
        for (String s : jenis.saringan) {
            arr.put(filter(s));
        }
        j.put("filter", arr);

        java.util.Calendar kal = java.util.Calendar.getInstance();
        kal.setTime(ais.ui.util.WaktuUtil.getDate());
        JSONArray tahun = new JSONArray();
        for (int t = kal.get(java.util.Calendar.YEAR) + 1;
                t >= kal.get(java.util.Calendar.YEAR) - 10; t--) {
            tahun.put(t);
        }
        j.put("pilihanTahun", tahun);
        JSONArray bulan = new JSONArray();
        for (int i = 1; i <= 12; i++) {
            bulan.put(new JSONObject().put("nilai", i).put("nama", namaBulan(i)));
        }
        j.put("pilihanBulan", bulan);
        j.put("bolehUbah", false);
        j.put("catatan", "Kontrak ini hanya membaca; keluarannya berkas PDF.");
    }

    /**
     * Deskripsi satu filter.
     *
     * <p>{@code cari} menandai filter yang <b>tidak boleh</b> disajikan sebagai
     * daftar tertutup. Tabel pasien, pendaftaran, transaksi, dan pembayaran
     * berisi puluhan ribu baris; menawarkan lima puluh yang terbaru saja berarti
     * operator tidak akan pernah menemukan pasien yang dicarinya. Filter tanpa
     * penanda ini tetap disajikan sebagai daftar biasa seperti sebelumnya.</p>
     */
    private static JSONObject filter(String nama) throws Exception {
        JSONObject d = new JSONObject().put("nama", nama);
        if (S_TAHUN.equals(nama)) {
            return d.put("label", "Tahun").put("tipe", "tahun").put("wajib", true);
        }
        if (S_BULAN.equals(nama)) {
            return d.put("label", "Bulan").put("tipe", "bulan").put("wajib", true);
        }
        if (S_STATUS.equals(nama)) {
            return d.put("label", "Status").put("tipe", "relasi").put("wajib", false);
        }
        if (S_PENDAFTARAN.equals(nama)) {
            return d.put("label", "Pendaftaran").put("tipe", "relasi")
                    .put("wajib", false).put("cari", true);
        }
        if (S_PASIEN.equals(nama)) {
            return d.put("label", "Pasien").put("tipe", "relasi")
                    .put("wajib", false).put("cari", true);
        }
        if (S_TRANSAKSI.equals(nama)) {
            return d.put("label", "Transaksi").put("tipe", "relasi")
                    .put("wajib", false).put("cari", true);
        }
        if (S_PEMBAYARAN.equals(nama)) {
            return d.put("label", "Pembayaran").put("tipe", "relasi")
                    .put("wajib", true).put("cari", true);
        }
        throw new IllegalArgumentException("Filter tidak dikenal: " + nama);
    }

    // ------------------------------------------------------------ lookup

    /**
     * Cari acuan yang harus dipilih sebelum laporan per objek dapat dicetak.
     *
     * <p>Layar ZK memakai banbox yang mencari sambil diketik. Di sini bentuknya
     * sama: kata kunci dicocokkan pada kode dan nama, hasilnya dibatasi supaya
     * satu kata kunci pendek tidak menarik seluruh tabel.</p>
     */
    @SuppressWarnings("unchecked")
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String jenis = text(r.getParameter("jenis"), "");
        String kata = text(r.getParameter("kata"), "").trim();
        Session s = HibernateUtil.openSession();
        try {
            JSONArray arr = new JSONArray();
            if (S_PENDAFTARAN.equals(jenis)) {
                Criteria c = s.createCriteria(Pendaftaran.class)
                        .createAlias("pasien", "pasien", Criteria.LEFT_JOIN);
                // Tracer hanya berlaku untuk pendaftaran rawat jalan, persis
                // seperti banbox yang dipakai layar lama.
                if ("rawat_jalan".equals(text(r.getParameter("lingkup"), ""))) {
                    c.add(Restrictions.eq("jenis", Pendaftaran.RAWAT_JALAN));
                }
                cocok(c, kata, "kode", "pasien.nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    Pendaftaran x = (Pendaftaran) o;
                    arr.put(new JSONObject().put("id", x.getId())
                            .put("kode", teks(x.getKode()))
                            .put("nama", x.getPasien() == null ? "" : teks(x.getPasien().getNama()))
                            .put("tanggal", tanggal(x.getTanggalPendaftaran())));
                }
            } else if (S_PASIEN.equals(jenis)) {
                Criteria c = s.createCriteria(Pasien.class);
                cocok(c, kata, "kode", "nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    Pasien x = (Pasien) o;
                    arr.put(new JSONObject().put("id", x.getId())
                            .put("kode", teks(x.getKode())).put("nama", teks(x.getNama())));
                }
            } else if (S_TRANSAKSI.equals(jenis)) {
                Criteria c = s.createCriteria(TransaksiMedis.class)
                        .createAlias("pasien", "pasien", Criteria.LEFT_JOIN);
                // Kolom `nama` dicari SEKALIGUS `pasien.nama`: getNama() pada
                // TransaksiMedis mengembalikan nama pasien bila transaksinya
                // tertaut pasien, sehingga mencari kolomnya saja akan gagal
                // menemukan transaksi menurut nama yang justru ditampilkan.
                cocok(c, kata, "kode", "nama", "pasien.nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    TransaksiMedis x = (TransaksiMedis) o;
                    arr.put(new JSONObject().put("id", x.getId())
                            .put("kode", teks(x.getKode())).put("nama", teks(x.getNama()))
                            .put("tanggal", tanggal(x.getTanggalTransaksi())));
                }
            } else if (S_PEMBAYARAN.equals(jenis)) {
                Criteria c = s.createCriteria(Pembayaran.class)
                        .createAlias("pasien", "pasien", Criteria.LEFT_JOIN);
                cocok(c, kata, "kode", "pasien.nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    Pembayaran x = (Pembayaran) o;
                    arr.put(new JSONObject().put("id", x.getId())
                            .put("kode", teks(x.getKode()))
                            .put("nama", x.getPasien() == null ? "" : teks(x.getPasien().getNama()))
                            .put("tanggal", tanggal(x.getTanggalPembayaran())));
                }
            } else if (S_STATUS.equals(jenis)) {
                // Ketiga pilihan ini datang dari konstanta Pendaftaran, bukan
                // ditulis ulang: teksnya masuk ke parameter laporan apa adanya,
                // sehingga salah ketik satu huruf menghasilkan laporan kosong.
                for (String v : new String[] { Pendaftaran.TERDAFTAR, Pendaftaran.KELUAR,
                        Pendaftaran.MENINGGAL }) {
                    arr.put(new JSONObject().put("id", v).put("nama", v));
                }
                j.put("bawaan", Pendaftaran.TERDAFTAR);
            } else if (S_BULAN.equals(jenis)) {
                for (int i = 1; i <= 12; i++) {
                    arr.put(new JSONObject().put("id", String.valueOf(i))
                            .put("nama", namaBulan(i)));
                }
            } else if (S_TAHUN.equals(jenis)) {
                java.util.Calendar kal = java.util.Calendar.getInstance();
                kal.setTime(ais.ui.util.WaktuUtil.getDate());
                int kini = kal.get(java.util.Calendar.YEAR);
                for (int t = kini + 1; t >= kini - 10; t--) {
                    arr.put(new JSONObject().put("id", String.valueOf(t)).put("nama", String.valueOf(t)));
                }
                j.put("bawaan", String.valueOf(kini));
            } else {
                throw new IllegalArgumentException("Jenis acuan tidak dikenal.");
            }
            j.put("jenis", jenis);
            j.put("pilihan", arr);
        } finally {
            s.close();
        }
    }

    /** Tambahkan pencocokan kata kunci pada beberapa kolom sekaligus. */
    private static void cocok(Criteria c, String kata, String... kolom) {
        if (kata.length() == 0) return;
        org.hibernate.criterion.Disjunction atau = Restrictions.disjunction();
        for (String k : kolom) {
            atau.add(Restrictions.ilike(k, kata, org.hibernate.criterion.MatchMode.ANYWHERE));
        }
        c.add(atau);
    }

    // ------------------------------------------------------------- cetak

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r, Jenis jenis) throws Exception {
        Map parameters = new HashMap();
        Session s = HibernateUtil.openSession();
        try {
            if (S_TAHUN.equals(jenis.saringan[0])) {
                periode(parameters, r, jenis);
            } else if (S_PEMBAYARAN.equals(jenis.saringan[0])) {
                buktiPembayaran(parameters, r, s);
            } else if ("tracer_pasien".equals(jenis.kode)) {
                tracer(parameters, r, s);
            } else {
                tagihan(parameters, r, s);
            }
        } finally {
            s.close();
        }
        JasperPdfUtil.tulis(j, jenis.template, parameters, jenis.kode, jenis.judul);
    }

    /** Laporan periodik: tahun, bulan, dan (bila ada) status pendaftaran. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void periode(Map parameters, HttpServletRequest r, Jenis jenis) {
        Integer tahun = angka(r.getParameter(S_TAHUN), "Tahun");
        Integer bulan = angka(r.getParameter(S_BULAN), "Bulan");
        if (tahun == null) throw new IllegalArgumentException("Tahun laporan belum dipilih.");
        if (bulan == null) throw new IllegalArgumentException("Bulan laporan belum dipilih.");
        if (bulan.intValue() < 1 || bulan.intValue() > 12) {
            throw new IllegalArgumentException("Bulan laporan di luar rentang 1..12.");
        }
        for (String f : jenis.saringan) {
            if (!S_STATUS.equals(f)) continue;
            // Layar lama memakai "Semua" ketika combobox status tidak dipilih.
            parameters.put("status", text(r.getParameter(S_STATUS), "Semua"));
        }
        parameters.put("tahun", tahun);
        parameters.put("bulan", bulan);
    }

    /**
     * Laporan tagihan dan biaya/retur.
     *
     * <p>Layar lama membaca {@code transaksi.getNama()} ketika pasien kosong dan
     * <b>tidak</b> memeriksa transaksi lebih dulu, sehingga memilih ketiganya
     * kosong berakhir dengan NullPointerException. Di sini keadaan itu dijawab
     * sebagai galat validasi yang terbaca. Perbedaan disengaja: memulangkan
     * pesan yang menjelaskan lebih berguna daripada meniru kegagalan.</p>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void tagihan(Map parameters, HttpServletRequest r, Session s) {
        Pendaftaran pendaftaran = (Pendaftaran) muat(s, Pendaftaran.class, r.getParameter("pendaftaranId"));
        Pasien pasien = (Pasien) muat(s, Pasien.class, r.getParameter("pasienId"));
        TransaksiMedis transaksi = (TransaksiMedis) muat(s, TransaksiMedis.class,
                r.getParameter("transaksiId"));
        if (pasien == null && transaksi == null) {
            throw new IllegalArgumentException(
                    "Pilih pasien atau transaksi lebih dulu; laporan ini menyebut nama pasien.");
        }
        parameters.put("nama_pasien", pasien == null ? teks(transaksi.getNama()) : teks(pasien.getNama()));
        parameters.put("mr", pasien == null ? "" : teks(pasien.getKode()));
        parameters.put("pendaftaran", id(pendaftaran == null ? null : pendaftaran.getId()));
        parameters.put("pasien", id(pasien == null ? null : pasien.getId()));
        parameters.put("transaksi", id(transaksi == null ? null : transaksi.getId()));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void buktiPembayaran(Map parameters, HttpServletRequest r, Session s) {
        Pembayaran pembayaran = (Pembayaran) muat(s, Pembayaran.class, r.getParameter("pembayaranId"));
        if (pembayaran == null) {
            throw new IllegalArgumentException("Pembayaran belum dipilih.");
        }
        Pasien pasien = pembayaran.getPasien();
        parameters.put("nama_pasien", pasien != null ? teks(pasien.getNama())
                : pembayaran.getTransaksi() == null ? "" : teks(pembayaran.getTransaksi().getNama()));
        parameters.put("mr", pasien == null ? "" : teks(pasien.getKode()));
        parameters.put("id", id(pembayaran.getId()));
    }

    /**
     * Laporan tracer, beserta barcode pendaftarannya.
     *
     * <p>Barcode ditulis ke {@code /report/temp} lalu dioper ke Jasper sebagai
     * <b>URL http</b> — bukan sebagai berkas. Bentuk itu dipertahankan apa
     * adanya dari layar lama: template mengharapkan parameter bertipe
     * {@link java.net.URL}, dan menggantinya dengan berkas atau aliran berarti
     * menyunting template yang sama-sama dipakai layar lama.</p>
     *
     * <p>Alamatnya disusun dari permintaan yang sedang berjalan, menggantikan
     * {@code Executions} milik ZK yang tidak ada di jalur native.</p>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void tracer(Map parameters, HttpServletRequest r, Session s) throws Exception {
        Pendaftaran pendaftaran = (Pendaftaran) muat(s, Pendaftaran.class, r.getParameter("pendaftaranId"));
        if (pendaftaran == null) {
            throw new IllegalArgumentException("Pendaftaran pasien belum dipilih.");
        }
        String kode = teks(pendaftaran.getKode());
        if (kode.length() == 0) {
            throw new IllegalArgumentException("Pendaftaran ini tidak punya kode untuk dijadikan barcode.");
        }
        File berkas = new File(r.getSession(true).getServletContext().getRealPath("/report/temp")
                + File.separator + "barcode_" + amanNamaBerkas(kode) + ".png");
        berkas.getParentFile().mkdirs();
        net.sourceforge.barbecue.BarcodeImageHandler.savePNG(
                net.sourceforge.barbecue.BarcodeFactory.createCode128B(kode), berkas);

        String pangkal = r.getScheme() + "://" + r.getServerName() + ":" + r.getServerPort()
                + r.getContextPath();
        parameters.put("pendaftaran", pendaftaran.getId());
        parameters.put("mybarcode", new java.net.URL(pangkal + "/report/temp/" + berkas.getName()));
    }

    /**
     * Buang segala sesuatu yang bukan huruf/angka dari kode sebelum dijadikan
     * nama berkas.
     *
     * <p>Kode pendaftaran datang dari basis data, bukan dari pengguna langsung,
     * tetapi ia tetap menyusun sebuah nama berkas — dan sebuah nama berkas yang
     * memuat pemisah jalur menulis ke tempat yang tidak dimaksudkan.</p>
     */
    private static String amanNamaBerkas(String kode) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < kode.length(); i++) {
            char c = kode.charAt(i);
            b.append(Character.isLetterOrDigit(c) ? c : '_');
        }
        return b.toString();
    }

    // -------------------------------------------------------------- util

    /** Id entitas untuk parameter Jasper; -1 berarti "tidak dipilih", seperti layar lama. */
    private static Long id(Long nilai) {
        return nilai == null ? Long.valueOf(-1L) : nilai;
    }

    private static Object muat(Session s, Class<?> kelas, String id) {
        String v = text(id, "").trim();
        if (v.length() == 0) return null;
        try {
            long l = Long.parseLong(v);
            if (l <= 0) return null;
            return s.get(kelas, Long.valueOf(l));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nilai acuan tidak sah.");
        }
    }

    private static Integer angka(String nilai, String label) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return null;
        try {
            return Integer.valueOf(Integer.parseInt(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
    }

    private static String namaBulan(int bulan) {
        List<String> nama = new ArrayList<String>();
        nama.add("Januari"); nama.add("Februari"); nama.add("Maret"); nama.add("April");
        nama.add("Mei"); nama.add("Juni"); nama.add("Juli"); nama.add("Agustus");
        nama.add("September"); nama.add("Oktober"); nama.add("November"); nama.add("Desember");
        return nama.get(bulan - 1);
    }

    private static String tanggal(Date d) {
        return d == null ? "" : Common.databaseDateFormat.get().format(d);
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
