package ais.common.newui.laporan;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.TransaksiMedis;
import ais.action.report.helper.CommonReport;

/**
 * Kontrak native dua puluh lima laporan SIRS.
 *
 * <h3>Koreksi atas anggapan sebelumnya</h3>
 * <p>Enam laporan awal pernah dinyatakan tidak dapat dikonversi karena
 * "kelas-kelasnya tidak ada di repositori ini". <b>Anggapan itu keliru.</b>
 * Seluruhnya ada, di bawah {@code ais.action.report.format1.sirs.*}. Yang saya
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
 * <h3>Tiga keluarga laporan</h3>
 * <ul>
 *   <li><b>Periodik</b> — disaring tahun/bulan (dan status pada Ranap Per
 *       Ruangan). Tidak memerlukan pilihan entitas.</li>
 *   <li><b>Per objek</b> — menuntut satu pendaftaran, pasien, transaksi, atau
 *       pembayaran yang dipilih lebih dulu. Layar ZK memakai banbox pencari;
 *       jalur native memakai aksi {@code lookup} dengan kata kunci.</li>
 *   <li><b>Rawat Jalan</b> — memakai rentang tanggal atau tahun/bulan,
 *       jenis pasien, dokter, dan pilihan sampai 5/21 poli. Parameter tiap
 *       template dipertahankan persis walaupun penamaannya tidak seragam.</li>
 * </ul>
 *
 * <p>Kontrak tidak mengubah transaksi pasien; keluarannya berkas PDF. Dua
 * laporan bulanan memperbarui tabel minggu sementara yang dipisahkan menurut
 * pengguna, karena template legacy memang membaca rentang dari tabel itu.</p>
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
        final String format;

        Jenis(String kode, String judul, String template, String[] saringan) {
            this(kode, judul, template, saringan, ais.action.report.Report.PDF);
        }

        Jenis(String kode, String judul, String template, String[] saringan, String format) {
            this.kode = kode;
            this.judul = judul;
            this.template = template;
            this.saringan = saringan;
            this.format = format;
        }
    }

    static final String S_TAHUN = "tahun";
    static final String S_BULAN = "bulan";
    static final String S_STATUS = "status";
    static final String S_PENDAFTARAN = "pendaftaran";
    static final String S_PASIEN = "pasien";
    static final String S_TRANSAKSI = "transaksi";
    static final String S_PEMBAYARAN = "pembayaran";
    static final String S_MULAI = "mulai";
    static final String S_SAMPAI = "sampai";
    static final String S_JENIS_PASIEN = "jenis_pasien";
    static final String S_DOKTER = "dokter";
    static final String S_POLI = "poli";
    static final String S_LOKASI = "lokasi";
    static final String S_TANGGAL = "tanggal";
    static final String S_PENYEDIA = "penyedia";
    static final String S_JENIS_ITEM = "jenis_item";
    static final String S_JENIS_ITEM_MEDIS = "jenis_item_medis";

    /**
     * Dua puluh lima laporan yang dilayani kontrak ini.
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
        if ("rajal_tahunan_5".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan Pasien Rajal Tahunan (5 Poli)",
                    "sirs/rajal_laporan_kunjungan_pasien_tahunan",
                    new String[] { S_TAHUN, S_JENIS_PASIEN, S_POLI });
        }
        if ("rajal_per_dokter".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan Pasien Rawat Jalan per Dokter",
                    "sirs/laporan_kunjungan_pasien_rawat_jalan_per_dokter",
                    new String[] { S_MULAI, S_SAMPAI, S_DOKTER, S_JENIS_PASIEN });
        }
        if ("rajal_periode".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan Pasien Rawat Jalan",
                    "sirs/laporan_kunjungan_pasien_rawat_jalan",
                    new String[] { S_MULAI, S_SAMPAI, S_JENIS_PASIEN });
        }
        if ("rajal_tahunan_21".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan Pasien Tahunan (21 Poli)",
                    "sirs/rajal_laporan_kunjungan_pasien_tahunan_21",
                    new String[] { S_TAHUN, S_JENIS_PASIEN, S_POLI });
        }
        if ("rajal_umum_21".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan Pasien Umum (21 Poli)",
                    "sirs/rajal_laporan_kunjungan_pasien_umum_21",
                    new String[] { S_TAHUN, S_BULAN, S_JENIS_PASIEN, S_POLI });
        }
        if ("rajal_umum_5".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan Pasien Umum (5 Poli)",
                    "sirs/rajal_laporan_kunjungan_pasien_umum_5",
                    new String[] { S_TAHUN, S_BULAN, S_JENIS_PASIEN, S_POLI });
        }
        if ("rajal_poli_baru_lama".equals(kode)) {
            return new Jenis(kode, "Laporan Kunjungan per Poli Baru/Lama",
                    "sirs/laporan_kunjungan_pasien_baru_lama",
                    new String[] { S_MULAI, S_SAMPAI, S_JENIS_PASIEN });
        }
        if ("laporan_kasir_harian".equals(kode)) {
            return new Jenis(kode, "Laporan Pendapatan Kasir Harian",
                    "sirs/laporan_kasir_harian",
                    new String[] { S_LOKASI, S_MULAI, S_SAMPAI }, ais.action.report.Report.XLS);
        }
        if ("laporan_kasir_per_shift".equals(kode)) {
            return new Jenis(kode, "Laporan Pendapatan Kasir Per Shift",
                    "sirs/laporan_kasir_per_shift",
                    new String[] { S_LOKASI, S_MULAI, S_SAMPAI }, ais.action.report.Report.XLS);
        }
        if ("laporan_ranap_pasien_dinas".equals(kode)) {
            return new Jenis(kode, "Laporan Ranap Dinas Per Ruangan",
                    "sirs/laporan_ranap_pasien_dinas", new String[] { S_TAHUN, S_BULAN });
        }
        if ("ranap_laporan_perruangan_periode".equals(kode)) {
            return new Jenis(kode, "Laporan Ranap Per Ruangan Periode",
                    "sirs/ranap_laporan_perruangan_periode", new String[] { S_MULAI, S_SAMPAI });
        }
        if ("inventory_harga_beli".equals(kode)) {
            return new Jenis(kode, "Laporan Harga Beli Item", "sirs/daftar_harga_beli",
                    new String[] { S_PENYEDIA, S_JENIS_ITEM }, ais.action.report.Report.XLS);
        }
        if ("inventory_harga_jual".equals(kode)) {
            return new Jenis(kode, "Laporan Harga Jual Item", "sirs/daftar_harga_jual_item",
                    new String[] { S_JENIS_ITEM_MEDIS }, ais.action.report.Report.XLS);
        }
        if ("inventory_hpp".equals(kode)) {
            return new Jenis(kode, "Laporan HPP", "sirs/hpp",
                    new String[] { S_LOKASI, S_TANGGAL }, ais.action.report.Report.XLS);
        }
        if ("inventory_stok".equals(kode)) {
            return new Jenis(kode, "Laporan Stok", "sirs/laporan_stok",
                    new String[] { S_LOKASI, S_TANGGAL }, ais.action.report.Report.XLS);
        }
        if ("inventory_kadaluarsa".equals(kode)) {
            return new Jenis(kode, "Laporan Kadaluarsa", "sirs/laporan_kadaluarsa",
                    new String[] { S_LOKASI }, ais.action.report.Report.XLS);
        }
        if ("inventory_koreksi".equals(kode)) {
            return new Jenis(kode, "Laporan Koreksi Item", "sirs/koreksi_item_periode",
                    new String[] { S_LOKASI, S_MULAI, S_SAMPAI }, ais.action.report.Report.XLS);
        }
        if ("inventory_pemakaian".equals(kode)) {
            return new Jenis(kode, "Laporan Pemakaian Item", "sirs/pemakaian_item_periode",
                    new String[] { S_LOKASI, S_MULAI, S_SAMPAI }, ais.action.report.Report.XLS);
        }
        if ("inventory_penerimaan_order".equals(kode)) {
            return new Jenis(kode, "Laporan Penerimaan Order", "sirs/delivery_order_per_periode",
                    new String[] { S_LOKASI, S_MULAI, S_SAMPAI }, ais.action.report.Report.XLS);
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
            else if ("lookup".equals(action)) lookup(json, request, j);
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
     * berarti laporan SIRS tidak menuntut halaman baru sama sekali.
     * Kalau bentuknya dibuat sendiri, akan ada dua deskripsi filter yang harus
     * dijaga sama tiap kali klien berubah.</p>
     */
    private static void meta(JSONObject j, HttpServletRequest request, Jenis jenis) throws Exception {
        j.put("judul", jenis.judul);
        j.put("kode", jenis.kode);
        j.put("template", jenis.template);
        j.put("format", jenis.format);
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));

        JSONArray arr = new JSONArray();
        for (String s : jenis.saringan) {
            JSONObject definisi = filter(jenis, s);
            if (S_LOKASI.equals(s)) {
                Lokasi lokasi = Common.getCurrentLokasi();
                if (lokasi != null && lokasi.getId() != null) {
                    definisi.put("nilaiBawaan", lokasi.getId());
                    if (lokasiTerkunci(jenis)) {
                        definisi.put("terkunci", true);
                    }
                }
            }
            arr.put(definisi);
        }
        j.put("filter", arr);

        java.util.Calendar kal = java.util.Calendar.getInstance();
        kal.setTime(ais.ui.util.WaktuUtil.getDate());
        JSONArray tahun = new JSONArray();
        for (int t = kal.get(java.util.Calendar.YEAR) + 9;
                t >= kal.get(java.util.Calendar.YEAR) - 10; t--) {
            tahun.put(t);
        }
        j.put("pilihanTahun", tahun);
        JSONArray bulan = new JSONArray();
        for (int i = 1; i <= 12; i++) {
            bulan.put(new JSONObject().put("nilai", i).put("nama", namaBulan(i)));
        }
        java.util.Calendar mulai = (java.util.Calendar) kal.clone();
        if ("laporan_kasir_harian".equals(jenis.kode)
                || "laporan_kasir_per_shift".equals(jenis.kode)
                || "inventory_koreksi".equals(jenis.kode)
                || "inventory_pemakaian".equals(jenis.kode)
                || "inventory_penerimaan_order".equals(jenis.kode)
                || "ranap_laporan_perruangan_periode".equals(jenis.kode)) {
            mulai.add(java.util.Calendar.MONTH, -1);
        }
        j.put("pilihanBulan", bulan)
                .put("tahunBawaan", kal.get(java.util.Calendar.YEAR))
                .put("bulanBawaan", kal.get(java.util.Calendar.MONTH) + 1)
                .put("mulaiBawaan", Common.databaseDateFormat.get().format(mulai.getTime()))
                .put("sampaiBawaan", Common.databaseDateFormat.get().format(kal.getTime()));
        j.put("bolehUbah", false);
        j.put("catatan", "Kontrak ini hanya membaca; keluarannya mengikuti format PDF/XLS layar lama.");
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
    private static JSONObject filter(Jenis jenis, String nama) throws Exception {
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
        if (S_MULAI.equals(nama)) {
            return d.put("label", "Tanggal Mulai").put("tipe", "tanggal").put("wajib", true);
        }
        if (S_SAMPAI.equals(nama)) {
            return d.put("label", "Tanggal Sampai").put("tipe", "tanggal").put("wajib", true);
        }
        if (S_JENIS_PASIEN.equals(nama)) {
            boolean wajib = !"rajal_poli_baru_lama".equals(jenis.kode);
            return d.put("label", "Jenis Pasien").put("tipe", "relasi")
                    .put("wajib", wajib).put("pilihPertama", wajib);
        }
        if (S_DOKTER.equals(nama)) {
            return d.put("label", "Dokter").put("tipe", "relasi")
                    .put("wajib", true).put("cari", true);
        }
        if (S_POLI.equals(nama)) {
            int maksimal = batasPoli(jenis);
            JSONArray indeks = new JSONArray();
            if (maksimal == 5) {
                indeks.put(0).put(1).put(3).put(4).put(5);
            } else {
                for (int i = 0; i < maksimal; i++) indeks.put(i);
            }
            return d.put("label", "Poli").put("tipe", "relasi_banyak")
                    .put("wajib", false).put("cari", true)
                    .put("maksimal", maksimal).put("indeksBawaan", indeks);
        }
        if (S_LOKASI.equals(nama)) {
            return d.put("label", "Lokasi").put("tipe", "relasi").put("wajib", false);
        }
        if (S_TANGGAL.equals(nama)) {
            return d.put("label", "Per Tanggal").put("tipe", "tanggal").put("wajib", true);
        }
        if (S_PENYEDIA.equals(nama)) {
            JSONArray indeks = new JSONArray();
            for (int i = 0; i < 8; i++) indeks.put(i);
            return d.put("label", "Supplier").put("tipe", "relasi_banyak")
                    .put("wajib", false).put("cari", true)
                    .put("maksimal", 8).put("indeksBawaan", indeks);
        }
        if (S_JENIS_ITEM.equals(nama)) {
            return d.put("label", "Jenis Item").put("tipe", "relasi")
                    .put("wajib", false).put("pilihPertama", true);
        }
        if (S_JENIS_ITEM_MEDIS.equals(nama)) {
            return d.put("label", "Jenis Item Medis").put("tipe", "relasi")
                    .put("wajib", false).put("pilihPertama", true);
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
    private static void lookup(JSONObject j, HttpServletRequest r, Jenis jenis) throws Exception {
        String nama = text(r.getParameter("filter"), "");
        // Dibatasi pada filter yang laporan INI deklarasikan, mengikuti kontrak
        // laporan generik. Tanpa batas itu, satu endpoint laporan menjadi jalan
        // membaca tabel pasien dari menu mana pun yang punya kontrak ini.
        boolean diakui = false;
        for (String f : jenis.saringan) {
            if (f.equals(nama)) diakui = true;
        }
        if (!diakui) throw new IllegalArgumentException("Filter tidak dikenal pada laporan ini.");

        String q = text(r.getParameter("q"), "").trim();
        Session s = HibernateUtil.openSession();
        try {
            JSONArray arr = new JSONArray();
            if (S_LOKASI.equals(nama)) {
                Criteria c = s.createCriteria(Lokasi.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                Lokasi current = Common.getCurrentLokasi();
                if (lokasiTerkunci(jenis)
                        && current != null && current.getId() != null) {
                    c.add(Restrictions.eq("id", current.getId()));
                }
                cocok(c, q, "nama");
                for (Object o : c.addOrder(Order.asc("nama")).setMaxResults(BATAS_CARI).list()) {
                    Lokasi x = (Lokasi) o;
                    arr.put(pilihan(x.getId(), "", x.getNama(), null));
                }
            } else if (S_PENYEDIA.equals(nama)) {
                Criteria c = s.createCriteria(Penyedia.class);
                cocok(c, q, "nama", "alamat");
                for (Object o : c.addOrder(Order.asc("id")).setMaxResults(BATAS_CARI).list()) {
                    Penyedia x = (Penyedia) o;
                    arr.put(pilihan(x.getId(), "", x.getNama(), null));
                }
            } else if (S_JENIS_ITEM.equals(nama)) {
                Criteria c = s.createCriteria(JenisItem.class);
                cocok(c, q, "nama");
                for (Object o : c.addOrder(Order.asc("id")).setMaxResults(BATAS_CARI).list()) {
                    JenisItem x = (JenisItem) o;
                    arr.put(pilihan(x.getId(), "", x.getNama(), null));
                }
            } else if (S_JENIS_ITEM_MEDIS.equals(nama)) {
                Criteria c = s.createCriteria(JenisItemMedis.class);
                cocok(c, q, "nama");
                for (Object o : c.addOrder(Order.asc("id")).setMaxResults(BATAS_CARI).list()) {
                    JenisItemMedis x = (JenisItemMedis) o;
                    arr.put(pilihan(x.getId(), "", x.getNama(), null));
                }
            } else if (S_PENDAFTARAN.equals(nama)) {
                Criteria c = s.createCriteria(Pendaftaran.class)
                        .createAlias("pasien", "pasien", Criteria.LEFT_JOIN);
                // Tracer hanya berlaku untuk pendaftaran rawat jalan, sama
                // dengan banbox yang dipakai layar lama. Lingkupnya ditentukan
                // jenis laporannya, bukan diminta dari klien: batas yang dikirim
                // klien bukan batas.
                if ("tracer_pasien".equals(jenis.kode)) {
                    c.add(Restrictions.eq("jenis", Pendaftaran.RAWAT_JALAN));
                }
                cocok(c, q, "kode", "pasien.nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    Pendaftaran x = (Pendaftaran) o;
                    arr.put(pilihan(x.getId(), x.getKode(),
                            x.getPasien() == null ? "" : x.getPasien().getNama(),
                            x.getTanggalPendaftaran()));
                }
            } else if (S_PASIEN.equals(nama)) {
                Criteria c = s.createCriteria(Pasien.class);
                cocok(c, q, "kode", "nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    Pasien x = (Pasien) o;
                    arr.put(pilihan(x.getId(), x.getKode(), x.getNama(), null));
                }
            } else if (S_TRANSAKSI.equals(nama)) {
                Criteria c = s.createCriteria(TransaksiMedis.class)
                        .createAlias("pasien", "pasien", Criteria.LEFT_JOIN);
                // Kolom `nama` dicari SEKALIGUS `pasien.nama`: getNama() pada
                // TransaksiMedis mengembalikan nama pasien bila transaksinya
                // tertaut pasien, sehingga mencari kolomnya saja akan gagal
                // menemukan transaksi menurut nama yang justru ditampilkan.
                cocok(c, q, "kode", "nama", "pasien.nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    TransaksiMedis x = (TransaksiMedis) o;
                    arr.put(pilihan(x.getId(), x.getKode(), x.getNama(), x.getTanggalTransaksi()));
                }
            } else if (S_PEMBAYARAN.equals(nama)) {
                Criteria c = s.createCriteria(Pembayaran.class)
                        .createAlias("pasien", "pasien", Criteria.LEFT_JOIN);
                cocok(c, q, "kode", "pasien.nama");
                for (Object o : c.addOrder(Order.desc("id")).setMaxResults(BATAS_CARI).list()) {
                    Pembayaran x = (Pembayaran) o;
                    arr.put(pilihan(x.getId(), x.getKode(),
                            x.getPasien() == null ? "" : x.getPasien().getNama(),
                            x.getTanggalPembayaran()));
                }
            } else if (S_JENIS_PASIEN.equals(nama)) {
                Criteria c = s.createCriteria(JenisPasien.class);
                cocok(c, q, "nama");
                for (Object o : c.addOrder(Order.asc("nama")).setMaxResults(BATAS_CARI).list()) {
                    JenisPasien x = (JenisPasien) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if (S_DOKTER.equals(nama)) {
                Criteria c = s.createCriteria(Dokter.class);
                cocok(c, q, "kode", "nama");
                for (Object o : c.addOrder(Order.asc("nama")).setMaxResults(BATAS_CARI).list()) {
                    Dokter x = (Dokter) o;
                    arr.put(pilihan(x.getId(), x.getKode(), x.getNama(), null));
                }
            } else if (S_POLI.equals(nama)) {
                Criteria c = s.createCriteria(Poly.class).add(Restrictions.isNull("polyDari"));
                cocok(c, q, "kode", "nama");
                for (Object o : c.addOrder(Order.asc("id")).setMaxResults(BATAS_CARI).list()) {
                    Poly x = (Poly) o;
                    arr.put(pilihan(x.getId(), x.getKode(), x.getNama(), null));
                }
            } else if (S_STATUS.equals(nama)) {
                // Ketiga pilihan ini datang dari konstanta Pendaftaran, bukan
                // ditulis ulang: teksnya masuk ke parameter laporan apa adanya,
                // sehingga salah ketik satu huruf menghasilkan laporan kosong.
                for (String v : new String[] { Pendaftaran.TERDAFTAR, Pendaftaran.KELUAR,
                        Pendaftaran.MENINGGAL }) {
                    arr.put(new JSONObject().put("id", v).put("nama", v));
                }
            } else {
                throw new IllegalArgumentException("Filter ini bukan filter relasi.");
            }
            j.put("filter", nama);
            j.put("pilihan", arr);
            j.put("total", arr.length());
            j.put("batas", BATAS_CARI);
        } finally {
            s.close();
        }
    }

    /**
     * Satu baris pilihan.
     *
     * <p>Labelnya menggabungkan kode, nama, dan tanggal karena ketiganya
     * bersama-sama yang membedakan satu baris dari baris lain — nama saja
     * berulang, dan kode saja tidak dikenali operator.</p>
     */
    private static JSONObject pilihan(Long id, String kode, String nama, Date tanggal)
            throws Exception {
        StringBuilder label = new StringBuilder();
        if (teks(kode).length() > 0) label.append(kode);
        if (teks(nama).length() > 0) {
            if (label.length() > 0) label.append(" — ");
            label.append(nama);
        }
        String t = tanggal(tanggal);
        if (t.length() > 0) label.append(" (").append(t).append(')');
        if (label.length() == 0) label.append('#').append(id);
        return new JSONObject().put("id", id).put("kode", teks(kode))
                .put("nama", label.toString());
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
            if (jenis.kode.startsWith("rajal_")) {
                rajal(parameters, r, s, jenis);
            } else if (jenis.kode.startsWith("inventory_")) {
                inventory(parameters, r, s, jenis);
            } else if (jenis.kode.startsWith("laporan_kasir_")) {
                kasir(parameters, r, s);
            } else if ("ranap_laporan_perruangan_periode".equals(jenis.kode)) {
                rentang(parameters, r);
            } else if (S_TAHUN.equals(jenis.saringan[0])) {
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
        if (ais.action.report.Report.XLS.equals(jenis.format)) {
            JasperPdfUtil.tulisXls(j, jenis.template, parameters, jenis.kode, jenis.judul);
        } else {
            JasperPdfUtil.tulis(j, jenis.template, parameters, jenis.kode, jenis.judul);
        }
    }

    /** Parameter empat laporan inventori SIRS, termasuk delapan supplier harga beli. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void inventory(Map parameters, HttpServletRequest r, Session s, Jenis jenis) {
        if ("inventory_koreksi".equals(jenis.kode)
                || "inventory_pemakaian".equals(jenis.kode)
                || "inventory_penerimaan_order".equals(jenis.kode)) {
            kasir(parameters, r, s);
            return;
        }
        if ("inventory_kadaluarsa".equals(jenis.kode)) {
            Lokasi lokasi = lokasiAktif(s, r.getParameter(S_LOKASI));
            parameters.put("lokasi", lokasi == null ? Long.valueOf(-1L) : lokasi.getId());
            return;
        }
        if ("inventory_harga_beli".equals(jenis.kode)) {
            List<Long> supplier = idsEntitas(r.getParameter(S_PENYEDIA), s, Penyedia.class, 8, "supplier");
            for (int i = 1; i <= 8; i++) {
                parameters.put("p_" + i,
                        i <= supplier.size() ? supplier.get(i - 1) : Long.valueOf(-1L));
            }
            JenisItem item = (JenisItem) muat(s, JenisItem.class, r.getParameter(S_JENIS_ITEM));
            parameters.put("jenisItem", item == null ? Long.valueOf(-1L) : item.getId());
            return;
        }
        if ("inventory_harga_jual".equals(jenis.kode)) {
            JenisItemMedis item = (JenisItemMedis) muat(s, JenisItemMedis.class,
                    r.getParameter(S_JENIS_ITEM_MEDIS));
            parameters.put("jenisItem", item == null ? Long.valueOf(-1L) : item.getId());
            return;
        }

        Lokasi lokasi = lokasiAktif(s, r.getParameter(S_LOKASI));
        Date tanggal = tanggalWajib(r.getParameter(S_TANGGAL), "Per tanggal");
        parameters.put("lokasi", lokasi == null ? Long.valueOf(-1L) : lokasi.getId());
        if ("inventory_hpp".equals(jenis.kode)) {
            parameters.put("tgl", Common.databaseDateFormat.get().format(tanggal));
        } else {
            parameters.put("tanggal", tanggal);
        }
    }

    /** Muat ID CSV, hilangkan duplikasi, verifikasi kelas, dan pertahankan urutan pilihan. */
    private static List<Long> idsEntitas(String nilai, Session s, Class<?> kelas,
            int maksimal, String label) {
        Set<Long> ids = new LinkedHashSet<Long>();
        String mentah = text(nilai, "");
        if (mentah.length() > 0) {
            String[] bagian = mentah.split(",");
            for (int i = 0; i < bagian.length; i++) {
                Long id = id(bagian[i]);
                if (id == null || id.longValue() <= 0L || s.get(kelas, id) == null) {
                    throw new IllegalArgumentException("Pilihan " + label + " tidak sah.");
                }
                ids.add(id);
            }
        }
        if (ids.size() > maksimal) {
            throw new IllegalArgumentException("Maksimal " + maksimal + " " + label + " dapat dipilih.");
        }
        return new ArrayList<Long>(ids);
    }

    /** Parameter dua laporan kasir: lokasi aktif dan rentang tanggal. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void kasir(Map parameters, HttpServletRequest r, Session s) {
        // Lokasi milik sesi adalah batas otorisasi, bukan sekadar nilai awal UI.
        // Combobox ZK menguncinya; mengandalkan atribut `terkunci` di klien saja
        // memungkinkan request buatan tangan membaca lokasi lain.
        Lokasi lokasi = Common.getCurrentLokasi();
        if (lokasi == null) {
            lokasi = lokasiAktif(s, r.getParameter(S_LOKASI));
        }
        parameters.put("lokasi1", lokasi == null ? Long.valueOf(-1L) : lokasi.getId());
        rentang(parameters, r);
    }

    /** Lokasi dapat dipilih operator, tetapi lokasi nonaktif selalu ditolak. */
    private static Lokasi lokasiAktif(Session s, String nilai) {
        Lokasi lokasi = (Lokasi) muat(s, Lokasi.class, nilai);
        if (lokasi != null && Boolean.FALSE.equals(lokasi.getAktif())) {
            throw new IllegalArgumentException("Lokasi tidak aktif.");
        }
        return lokasi;
    }

    /** Layar legacy mengunci lokasi sesi pada laporan-laporan ini. */
    private static boolean lokasiTerkunci(Jenis jenis) {
        return jenis.kode.startsWith("laporan_kasir_")
                || "inventory_koreksi".equals(jenis.kode)
                || "inventory_pemakaian".equals(jenis.kode)
                || "inventory_penerimaan_order".equals(jenis.kode);
    }

    /** Parameter rentang tanggal bertipe teks yyyy-MM-dd seperti layar ZK. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void rentang(Map parameters, HttpServletRequest r) {
        Date mulai = tanggalWajib(r.getParameter(S_MULAI), "Tanggal mulai");
        Date sampai = tanggalWajib(r.getParameter(S_SAMPAI), "Tanggal sampai");
        if (mulai.after(sampai)) {
            throw new IllegalArgumentException("Tanggal mulai tidak boleh melewati tanggal sampai.");
        }
        parameters.put("tgl1", Common.databaseDateFormat.get().format(mulai));
        parameters.put("tgl2", Common.databaseDateFormat.get().format(sampai));
    }

    /**
     * Parameter tujuh laporan Rawat Jalan, disalin dari generateParameter()
     * masing-masing window ZK. Nama parameter sengaja tidak diseragamkan:
     * beberapa template memakai camelCase, satu template memakai snake_case.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void rajal(Map parameters, HttpServletRequest r, Session s, Jenis jenis) {
        JenisPasien jenisPasien = (JenisPasien) muat(s, JenisPasien.class,
                r.getParameter(S_JENIS_PASIEN));
        boolean opsional = "rajal_poli_baru_lama".equals(jenis.kode);
        if (jenisPasien == null && !opsional) {
            throw new IllegalArgumentException("Jenis pasien belum dipilih.");
        }

        if ("rajal_tahunan_5".equals(jenis.kode)
                || "rajal_tahunan_21".equals(jenis.kode)
                || "rajal_umum_5".equals(jenis.kode)
                || "rajal_umum_21".equals(jenis.kode)) {
            Integer tahun = angka(r.getParameter(S_TAHUN), "Tahun");
            if (tahun == null) throw new IllegalArgumentException("Tahun laporan belum dipilih.");
            parameters.put("tahun", tahun);
            parameters.put("nama_jenis_pasien", jenisPasien.getNama());
            parameters.put("jenis_pasien", jenisPasien.getId());
            isiPoli(parameters, r.getParameter(S_POLI), s, batasPoli(jenis));

            if ("rajal_umum_5".equals(jenis.kode) || "rajal_umum_21".equals(jenis.kode)) {
                Integer bulan = angka(r.getParameter(S_BULAN), "Bulan");
                if (bulan == null || bulan.intValue() < 1 || bulan.intValue() > 12) {
                    throw new IllegalArgumentException("Bulan laporan harus berada pada rentang 1..12.");
                }
                Tbmuser user = Common.getCurrentUser(r);
                if (user == null || user.getUserId() == null) {
                    throw new SecurityException("Sesi pengguna tidak dikenal.");
                }
                parameters.put("bulan", bulan);
                parameters.put("tbmuser", user.getUserId());
                // Kedua template membaca tabel minggu sementara milik user.
                CommonReport.inputMinggu(user, bulan, tahun);
            } else if ("rajal_tahunan_21".equals(jenis.kode)) {
                Tbmuser user = Common.getCurrentUser(r);
                if (user == null || user.getUserId() == null) {
                    throw new SecurityException("Sesi pengguna tidak dikenal.");
                }
                parameters.put("tbmuser", user.getUserId());
            }
            return;
        }

        Date mulai = tanggalWajib(r.getParameter(S_MULAI), "Tanggal mulai");
        Date sampai = tanggalWajib(r.getParameter(S_SAMPAI), "Tanggal sampai");
        if (mulai.after(sampai)) {
            throw new IllegalArgumentException("Tanggal mulai tidak boleh melewati tanggal sampai.");
        }
        parameters.put("nama_jenis_pasien", jenisPasien == null ? "" : jenisPasien.getNama());
        parameters.put("jenis_pasien", jenisPasien == null ? Long.valueOf(-1L) : jenisPasien.getId());

        if ("rajal_poli_baru_lama".equals(jenis.kode)) {
            parameters.put("tanggal_mulai", Common.databaseDateFormat.get().format(mulai));
            parameters.put("tanggal_sampai", Common.databaseDateFormat.get().format(sampai));
        } else {
            parameters.put("tanggalMulai", mulai);
            parameters.put("tanggalSelesai", sampai);
        }

        if ("rajal_per_dokter".equals(jenis.kode)) {
            Dokter dokter = (Dokter) muat(s, Dokter.class, r.getParameter(S_DOKTER));
            if (dokter == null) throw new IllegalArgumentException("Dokter belum dipilih.");
            parameters.put("dokter", dokter.getId());
            parameters.put("namaDokter", teks(dokter.getNama()));
        }
    }

    /** Isi poli1..poliN dan tolak id yang bukan poli tingkat atas. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void isiPoli(Map parameters, String nilai, Session s, int maksimal) {
        Set<Long> ids = new LinkedHashSet<Long>();
        String mentah = text(nilai, "");
        if (mentah.length() > 0) {
            String[] bagian = mentah.split(",");
            for (int i = 0; i < bagian.length; i++) {
                Long id = id(bagian[i]);
                if (id == null || id.longValue() <= 0L) {
                    throw new IllegalArgumentException("Pilihan poli tidak sah.");
                }
                Poly poli = (Poly) s.get(Poly.class, id);
                if (poli == null || poli.getPolyDari() != null) {
                    throw new IllegalArgumentException("Poli tidak ditemukan atau bukan poli utama.");
                }
                ids.add(id);
            }
        }
        if (ids.size() > maksimal) {
            throw new IllegalArgumentException("Maksimal " + maksimal + " poli dapat dipilih.");
        }
        List<Long> urut = new ArrayList<Long>(ids);
        for (int i = 1; i <= maksimal; i++) {
            parameters.put("poli" + i,
                    i <= urut.size() ? urut.get(i - 1) : Long.valueOf(-1L));
        }
    }

    private static int batasPoli(Jenis jenis) {
        return jenis.kode.endsWith("_5") ? 5 : 21;
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
        Pendaftaran pendaftaran = (Pendaftaran) muat(s, Pendaftaran.class, r.getParameter(S_PENDAFTARAN));
        Pasien pasien = (Pasien) muat(s, Pasien.class, r.getParameter(S_PASIEN));
        TransaksiMedis transaksi = (TransaksiMedis) muat(s, TransaksiMedis.class,
                r.getParameter(S_TRANSAKSI));
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
        Pembayaran pembayaran = (Pembayaran) muat(s, Pembayaran.class, r.getParameter(S_PEMBAYARAN));
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
        Pendaftaran pendaftaran = (Pendaftaran) muat(s, Pendaftaran.class, r.getParameter(S_PENDAFTARAN));
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

    private static Long id(String nilai) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return null;
        try {
            return Long.valueOf(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Date tanggalWajib(String nilai, String label) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) throw new IllegalArgumentException(label + " wajib diisi.");
        try {
            Date hasil = Common.databaseDateFormat.get().parse(v);
            if (!v.equals(Common.databaseDateFormat.get().format(hasil))) {
                throw new IllegalArgumentException(label + " bukan tanggal yang sah.");
            }
            return hasil;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            try {
                return new Date(Long.parseLong(v));
            } catch (Exception ignored) {
                throw new IllegalArgumentException(label + " bukan tanggal yang sah.");
            }
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
