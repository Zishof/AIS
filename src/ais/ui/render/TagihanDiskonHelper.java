package ais.ui.render;

import ais.common.Common;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailBiaya;
import ais.database.model.PengaturanPembayaranBulanan;

import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Row;

/**
 * Helper statis untuk menampilkan informasi tagihan, diskon, dan status pembayaran
 * mahasiswa di tabel pembayaran ({@link DetailPembayaranMahasiswaRenderer}).
 *
 * <h2>Latar Belakang &amp; Masalah yang Dipecahkan</h2>
 * <p>
 * Sebelum helper ini ada, logika menampilkan "harga dicoret sebelum diskon" tersebar
 * di tiga tempat berbeda di dalam {@code DetailPembayaranMahasiswaRenderer}: dua untuk
 * item Bulanan dan satu untuk item non-Bulanan. Ketiganya memiliki logika yang hampir
 * identik namun tidak ter-reuse, sehingga perbaikan bug harus dilakukan di tiga titik.
 *
 * <h2>Model Diskon yang Didukung</h2>
 * <p>
 * Sistem AIS mendukung tiga sumber diskon yang diperiksa secara berurutan:
 * <ol>
 *   <li><b>Kelompok Mahasiswa</b> — diskon yang dikonfigurasi pada
 *       {@code KelompokMahasiswa.jenisDiskonMahasiswa}. Berlaku untuk semua item
 *       biaya yang terdaftar pada JenisDiskonMahasiswa tersebut, dalam rentang
 *       semester yang dikonfigurasi ({@code smtMulai} hingga {@code smtSampai}).</li>
 *   <li><b>Jenis Seleksi Mahasiswa</b> — diskon yang melekat pada jalur masuk
 *       mahasiswa ({@code JenisSeleksi.jenisDiskonMahasiswa}). Logika rentang
 *       semester identik dengan sumber 1. Contoh: mahasiswa jalur beasiswa mendapat
 *       diskon 50% SPP sejak semester 1 hingga semester 8.</li>
 *   <li><b>DiskonMahasiswa Individual</b> — daftar diskon yang dilekatkan langsung
 *       pada {@code DetailKegiatan} (maksimal 3 item: diskon1, diskon2, diskon3).
 *       Digunakan untuk penyesuaian manual per-mahasiswa, misalnya beasiswa luar
 *       yang tidak terikat pada jalur seleksi atau kelompok tertentu.</li>
 * </ol>
 * <p>
 * Nilai diskon yang dipersistenkan di DB disimpan di {@code DetailKegiatan.diskon}
 * (tipe {@code Double}), dan dihitung oleh
 * {@link ais.database.model.Kegiatan#hitungDiskon(DetailKegiatan, ais.database.model.Kegiatan,
 * DetailBiaya, Double)}.
 *
 * <h2>Aturan Tampilan "Nominal Asli Sebelum Diskon"</h2>
 * <p>
 * Angka dicoret (line-through) di kolom Tagihan dihitung dengan dua sumber:
 * <pre>
 *   asli = tagihan_akhir + diskon_tersimpan          // Sumber 1: diskon eksplisit dari DB
 *   if (diskon == 0 &amp;&amp; nominalKonfig &gt; asli) {
 *       asli = nominalKonfig;                        // Sumber 2: fallback nominal konfigurasi
 *   }
 * </pre>
 * <b>Catatan penting:</b> Sumber 2 HANYA dipakai bila tidak ada diskon tersimpan
 * ({@code diskon &lt; 0.5}). Ini mencegah situasi di mana {@code nominalKonfig}
 * menyimpan nilai agregat (misal triwulan = 3 × bulanan) yang muncul sebagai
 * "nominal asli" — nilainya menyesatkan karena 3× lipat tagihan bulanan.
 *
 * <h2>Indikator Status Bayar</h2>
 * <p>
 * Method {@link #buatProgressBar(double, double)} menghasilkan HTML miniature
 * progress bar berwarna untuk menunjukkan persentase pembayaran secara visual:
 * <ul>
 *   <li>Hijau ({@code #22c55e}) — lunas 100%</li>
 *   <li>Kuning ({@code #f59e0b}) — cicilan (1%–99%)</li>
 *   <li>Merah ({@code #ef4444}) — belum ada pembayaran (0%)</li>
 * </ul>
 *
 * <h2>Badge Diskon</h2>
 * <p>
 * Method {@link #buatBadgeDiskon(String, double)} menghasilkan chip/badge HTML
 * berwarna hijau terang yang menampilkan nama jenis diskon dan persentasenya.
 * Badge ini lebih mudah dibaca daripada label teks biasa, khususnya di layar
 * HP yang memiliki ruang horizontal terbatas. Jika persentase = 0 (diskon nominal
 * flat), hanya nama yang ditampilkan.
 *
 * <h2>Penggunaan Minimal</h2>
 * <pre>
 * // Di dalam loop render baris DetailPembayaranMahasiswaRenderer:
 * TagihanDiskonHelper.tampilkanNominalAsliSebelumDiskon(
 *     vboxTagihan, rowPembayaran, jml, detailKegiatan,
 *     pengaturanPembayaranBulanan, detailBiaya, false);
 *
 * String badgeHtml = TagihanDiskonHelper.buatBadgeDiskon("Diskon SPP 50%", 50.0);
 * Html badge = new Html();
 * badge.setContent(badgeHtml);
 * badge.setParent(vbox);
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Semua method bersifat stateless dan aman dipanggil dari thread ZK mana pun.
 * Tidak ada field statis mutable. Format angka menggunakan
 * {@code Common.numberFormat.get()} (ThreadLocal) yang sudah thread-safe.
 *
 * @author AIS System
 * @version 2.0 — perbaikan bug diskon non-editable + nominalKonfig menyesatkan
 * @see DetailPembayaranMahasiswaRenderer
 * @see ais.database.model.Kegiatan#hitungDiskon
 * @see ais.database.model.JenisDiskonMahasiswa
 */
public final class TagihanDiskonHelper {

    /** Utilitas statis; tidak perlu diinstansiasi. */
    private TagihanDiskonHelper() {}

    // ─── Konstanta warna ──────────────────────────────────────────────────────

    /** Warna progress bar: lunas (hijau). */
    private static final String CLR_LUNAS   = "#22c55e";
    /** Warna progress bar: cicilan (kuning). */
    private static final String CLR_CICILAN = "#f59e0b";
    /** Warna progress bar: belum bayar (merah). */
    private static final String CLR_BELUM   = "#ef4444";

    // ─── Tampilkan nominal asli (dicoret) ────────────────────────────────────

    /**
     * Tampilkan nominal ASLI sebelum diskon sebagai teks dicoret (line-through)
     * di bawah nilai tagihan akhir dalam kolom Tagihan.
     *
     * <p>Teks dicoret hanya muncul bila ada selisih antara nominal asli dan tagihan
     * akhir. Bila tidak ada potongan (selisih &lt; 0.5), method ini tidak menambahkan
     * komponen apa pun ke {@code vbox}.
     *
     * <p><b>Sumber nilai "asli":</b>
     * <ol>
     *   <li>Bila {@code detailKegiatan.getDiskon() &gt; 0}: asli = tagihan + diskon
     *       (angka dari DB, akurat per item).</li>
     *   <li>Bila tidak ada diskon tersimpan: asli = max(tagihan, nominalKonfig) —
     *       fallback ke nominal terkonfigurasi di {@code PengaturanPembayaranBulanan}
     *       atau {@code DetailBiaya}. Sumber ini TIDAK dipakai bila sumber 1 ada,
     *       karena nominalKonfig bisa berupa nilai agregat (triwulan dll.) yang
     *       lebih besar dari tagihan bulanan sehingga menyesatkan.</li>
     * </ol>
     *
     * @param vbox     wadah ZK tempat label dicoret ditambahkan
     * @param row      baris ZK tempat atribut {@code tagDiskon} disimpan
     * @param jml      tagihan akhir setelah diskon (bisa negatif untuk item pengurang)
     * @param dk       DetailKegiatan yang menyimpan nilai diskon tersimpan; boleh null
     * @param ppb      PengaturanPembayaranBulanan yang menyimpan nominal konfigurasi;
     *                 boleh null (fallback ke detailBiaya)
     * @param db       DetailBiaya yang menyimpan nilaiBiaya; boleh null
     * @param negatif  true jika item ini adalah pengurang (DIKALI_NILAI_MINUS) —
     *                 angka dicoret juga akan ditampilkan negatif agar konsisten
     */
    public static void tampilkanNominalAsliSebelumDiskon(
            Vbox vbox, Row row, Double jml, DetailKegiatan dk,
            PengaturanPembayaranBulanan ppb, DetailBiaya db, boolean negatif) {
        try {
            double tagihan = jml == null ? 0.0 : Math.abs(jml);
            double diskon  = dk != null && dk.getDiskon() != null ? Math.abs(dk.getDiskon()) : 0.0;
            double asli    = tagihan + diskon; // sumber 1

            double nominalKonfig = ppb != null && ppb.getNominal() != null
                    ? Math.abs(ppb.getNominal())
                    : db != null && db.getNilaiBiaya() != null
                            ? Math.abs(db.getNilaiBiaya())
                            : 0.0;

            // Sumber 2 hanya saat TIDAK ada diskon eksplisit.
            // Mencegah nominalKonfig triwulan/agregat menimpa asli bulanan.
            if (diskon < 0.5 && nominalKonfig > asli) {
                asli = nominalKonfig;
            }

            if (asli <= tagihan + 0.5) {
                return; // tidak ada potongan, tidak perlu coretan
            }

            Label tagDiskon = new Label(Common.numberFormat.get().format(negatif ? -asli : asli));
            tagDiskon.setWidth("100%");
            tagDiskon.setStyle("text-decoration:line-through;color:#94a3b8;font-size:11px;");
            tagDiskon.setParent(vbox);
            row.setAttribute("tagDiskon", tagDiskon);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ─── Badge diskon ─────────────────────────────────────────────────────────

    /**
     * Bangun HTML chip/badge berwarna hijau untuk menampilkan nama dan persentase
     * diskon di samping keterangan item biaya.
     *
     * <p>Badge dirancang untuk:
     * <ul>
     *   <li>Menonjol secara visual tanpa mendominasi — ukuran font 10px, padding
     *       tipis, sudut membulat (pill shape).</li>
     *   <li>Tetap terbaca di layar HP karena tidak mengandung elemen block besar.</li>
     *   <li>Konsisten dengan palet warna sistem (hijau = aktif/diizinkan).</li>
     * </ul>
     *
     * @param namaDiskon nama jenis diskon, misalnya {@code "Diskon SPP 50%"};
     *                   di-escape HTML secara otomatis
     * @param persen     persentase diskon (0–100); jika &lt; 0.1, hanya nama yang tampil
     * @return string HTML siap-pakai untuk dimasukkan ke {@link Html#setContent(String)}
     */
    public static String buatBadgeDiskon(String namaDiskon, double persen) {
        if (namaDiskon == null || namaDiskon.trim().isEmpty()) {
            return "";
        }
        String escapedNama = escHtml(namaDiskon);
        String teks = persen >= 0.1
                ? escapedNama + " &minus;" + Math.round(persen) + "%"
                : escapedNama;
        return "<span style='display:inline-block;padding:1px 7px;border-radius:10px;"
                + "background:#dcfce7;color:#15803d;font-size:10px;font-weight:600;"
                + "line-height:1.6;white-space:nowrap;'>" + teks + "</span>";
    }

    // ─── Progress bar pembayaran ──────────────────────────────────────────────

    /**
     * Bangun HTML miniature progress bar horizontal yang merepresentasikan
     * persentase pembayaran ({@code dibayar/tagihan}).
     *
     * <p>Spesifikasi visual:
     * <ul>
     *   <li>Tinggi bar 4px, lebar 100% dari kolom.</li>
     *   <li>Warna batang berubah sesuai status: hijau (100%), kuning (1–99%),
     *       merah (0%).</li>
     *   <li>Tooltip persentase muncul saat hover (atribut {@code title}).</li>
     *   <li>Tidak memerlukan library eksternal — murni CSS inline.</li>
     * </ul>
     *
     * @param tagihan nilai total tagihan; jika &le; 0 mengembalikan string kosong
     * @param dibayar nilai yang sudah dibayarkan; boleh 0 atau &gt; tagihan
     * @return string HTML progress bar siap-pakai
     */
    public static String buatProgressBar(double tagihan, double dibayar) {
        if (tagihan < 0.5) {
            return "";
        }
        double pct = Math.min(100.0, Math.max(0.0, (dibayar / tagihan) * 100.0));
        int pctInt = (int) Math.round(pct);
        String warna = pctInt >= 100 ? CLR_LUNAS : (pctInt > 0 ? CLR_CICILAN : CLR_BELUM);
        return "<div title='" + pctInt + "%' style='height:4px;background:#e2e8f0;"
                + "border-radius:2px;overflow:hidden;margin-top:4px;'>"
                + "<div style='height:100%;width:" + pctInt + "%;background:" + warna
                + ";transition:width .3s;'></div></div>";
    }

    // ─── Chip status lunas ────────────────────────────────────────────────────

    /**
     * Bangun HTML chip status pembayaran: "Lunas", "Cicilan X%", atau "Belum Bayar".
     *
     * <p>Digunakan di kolom ringkas pada tampilan mobile di mana kolom Kekurangan
     * tidak cukup ruang untuk menampilkan nominal lengkap.
     *
     * @param tagihan nilai total tagihan; jika &le; 0 mengembalikan string kosong
     * @param dibayar nilai yang sudah dibayarkan
     * @return string HTML chip status
     */
    public static String buatChipStatus(double tagihan, double dibayar) {
        if (tagihan < 0.5) {
            return "";
        }
        double pct = Math.min(100.0, Math.max(0.0, (dibayar / tagihan) * 100.0));
        int pctInt = (int) Math.round(pct);
        String bg, fg, teks;
        if (pctInt >= 100) {
            bg = "#dcfce7"; fg = "#15803d"; teks = "Lunas";
        } else if (pctInt > 0) {
            bg = "#fef9c3"; fg = "#a16207"; teks = "Cicilan " + pctInt + "%";
        } else {
            bg = "#fee2e2"; fg = "#b91c1c"; teks = "Belum Bayar";
        }
        return "<span style='display:inline-block;padding:1px 7px;border-radius:10px;"
                + "background:" + bg + ";color:" + fg + ";font-size:10px;font-weight:600;"
                + "line-height:1.6;white-space:nowrap;'>" + teks + "</span>";
    }

    // ─── Ringkasan diskon untuk kolom Tagihan ────────────────────────────────

    /**
     * Hitung nilai nominal asli sebelum diskon untuk keperluan komputasi (bukan
     * tampilan). Metode ini menggunakan aturan sumber 1 vs sumber 2 yang sama
     * dengan {@link #tampilkanNominalAsliSebelumDiskon}.
     *
     * @param tagihan      nilai tagihan akhir setelah diskon
     * @param diskon       nilai diskon tersimpan (dari {@code DetailKegiatan.getDiskon()})
     * @param nominalKonfig nominal terkonfigurasi dari {@code PengaturanPembayaranBulanan}
     *                      atau {@code DetailBiaya}
     * @return nilai nominal asli sebelum diskon; sama dengan {@code tagihan} jika tidak ada diskon
     */
    public static double hitungNominalAsli(double tagihan, double diskon, double nominalKonfig) {
        double asli = tagihan + diskon;
        if (diskon < 0.5 && nominalKonfig > asli) {
            asli = nominalKonfig;
        }
        return asli;
    }

    // ─── Utilitas internal ────────────────────────────────────────────────────

    /** Escape karakter HTML khusus pada string yang akan dimasukkan ke atribut/konten HTML. */
    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
