package ais.common;

import ais.ui.util.MyMessageboxConfig;

/**
 * {@code PesanFormalHelper} — utilitas <b>bersama (reuse)</b> di seluruh aplikasi untuk menyusun
 * &amp; menampilkan SEMUA jenis pesan (sukses, peringatan, error/kesalahan) memakai bahasa yang
 * <b>sangat formal, sopan, dan mudah dipahami</b> oleh semua pengguna — bukan hanya administrator
 * atau teknisi. Ini adalah versi UMUM (tidak terikat modul Neo Feeder) dari pola yang sebelumnya
 * dipakai khusus untuk pesan koneksi Feeder ({@code NeoFeederPesanFormalHelper}), agar SELURUH
 * alert di aplikasi — di modul manapun — bisa memakai standar yang sama.
 *
 * <h3>Standar isi pesan (WAJIB, sesuai kebijakan aplikasi)</h3>
 * Setiap pesan kesalahan yang dihasilkan kelas ini SELALU memuat 4 bagian:
 * <ol>
 *   <li><b>Sapaan formal</b> — pembuka sopan ("Yang terhormat Bapak/Ibu Pengguna,").</li>
 *   <li><b>Penyebab</b> — penjelasan serinci &amp; sedetail mungkin tentang apa yang terjadi
 *       (disuplai pemanggil, karena hanya pemanggil yang tahu konteks bisnisnya).</li>
 *   <li><b>Tindak Lanjut</b> — langkah-langkah konkret &amp; berurutan yang dapat dicoba pengguna
 *       sendiri untuk mengatasi masalah tersebut (disuplai pemanggil).</li>
 *   <li><b>Eskalasi</b> — anjuran BAKU (sama di semua pesan) menghubungi Administrator Sistem atau
 *       melaporkan ke Pengembang Sistem, WAJIB melampirkan tangkapan layar (screenshot) pada saat
 *       kesalahan terjadi, apabila langkah-langkah di atas belum menyelesaikan masalah.</li>
 * </ol>
 *
 * <h3>Multi-bahasa</h3>
 * Seluruh bagian TETAP/berulang (sapaan, label "Penyebab:"/"Tindak Lanjut:", teks eskalasi) SELALU
 * diterjemahkan satu-per-satu via {@link Common#getBahasaConfig(String)} — mekanisme kamus bahasa
 * baku aplikasi ini (dipakai juga oleh {@link MyMessageboxConfig}) — sehingga potongan teks yang
 * SAMA di setiap pemanggilan cukup diterjemahkan/di-cache SEKALI di kamus, bukan sebagai kalimat
 * gabungan yang unik tiap kali (yang akan membanjiri kamus &amp; menyulitkan cache). Isi dinamis
 * (penyebab spesifik, langkah spesifik, nilai data) disisipkan apa adanya oleh pemanggil dan tetap
 * ikut diterjemahkan best-effort oleh {@link MyMessageboxConfig#show} (yang menerjemahkan pesan
 * akhir secara keseluruhan sebagai lapisan kedua).
 *
 * <h3>Dipakai di titik pusat, bukan ditulis ulang di tiap layar</h3>
 * Cukup panggil {@link #tampilkanSukses(String, String)}, {@link #tampilkanGagal(String, String,
 * String[])}, atau {@link #tampilkanGagalException(String, Throwable, String[])} — SATU baris,
 * tanpa menulis ulang teks sapaan/label/eskalasi di tiap layar. Method {@code pesanX(...)} (tanpa
 * awalan {@code tampilkan}) hanya MENYUSUN string (tidak menampilkan apa pun) — berguna bila pesan
 * perlu disimpan dahulu ke {@code Label}/progress-bar sebelum ditampilkan belakangan.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 (tanpa lambda, tanpa try-with-resources multi-catch).
 *
 * @author Tim AIS
 */
public final class PesanFormalHelper {

    private PesanFormalHelper() {
    }

    // =========================================================
    // Label/teks TETAP (diterjemahkan satu-per-satu via kamus bahasa)
    // =========================================================

    private static String t(String s) {
        try {
            return Common.getBahasaConfig(s);
        } catch (Throwable e) {
            return s;
        }
    }

    private static String sapaan() {
        return t("Yang terhormat Bapak/Ibu Pengguna,");
    }

    private static String labelPenyebab() {
        return t("Penyebab:");
    }

    private static String labelTindakLanjut() {
        return t("Tindak Lanjut yang dapat Bapak/Ibu coba:");
    }

    /** Baris eskalasi BAKU — dipakai di SEMUA pesan gagal di seluruh aplikasi agar konsisten. */
    public static String eskalasi() {
        return t("Apabila Bapak/Ibu kurang memahami pesan kesalahan ini atau langkah-langkah di atas "
                + "belum berhasil mengatasi masalah, kami mohon agar segera menghubungi Administrator "
                + "Sistem di lingkungan Bapak/Ibu, atau melaporkan kejadian ini kepada Pengembang "
                + "Sistem. Untuk mempercepat proses penanganan, mohon WAJIB melampirkan tangkapan "
                + "layar (screenshot) pada saat kesalahan ini terjadi beserta waktu kejadiannya.");
    }

    // =========================================================
    // SUKSES
    // =========================================================

    /**
     * Menyusun pesan SUKSES formal.
     *
     * @param aktivitas nama aktivitas yang berhasil, mis. {@code "penyimpanan data Perkuliahan"};
     *                  boleh {@code null}/kosong
     * @param detail    rincian tambahan yang relevan (boleh {@code null}/kosong)
     * @return pesan siap tampil
     */
    public static String pesanSukses(String aktivitas, String detail) {
        StringBuilder sb = new StringBuilder();
        sb.append(sapaan()).append("\n\n");
        sb.append(t("Dengan ini kami sampaikan bahwa proses"));
        sb.append(" ").append(kosongKe(aktivitas, t("yang Bapak/Ibu jalankan")));
        sb.append(" ").append(t("telah BERHASIL diselesaikan dengan baik."));
        if (detail != null && detail.trim().length() > 0) {
            sb.append("\n\n").append(t("Rincian:")).append(" ").append(detail.trim());
        }
        sb.append("\n\n").append(t("Terima kasih atas kesabaran Bapak/Ibu."));
        return sb.toString();
    }

    /** Menampilkan pesan sukses (lihat {@link #pesanSukses(String, String)}) via {@link MyMessageboxConfig}. */
    public static void tampilkanSukses(String aktivitas, String detail) {
        tampilkanAman(pesanSukses(aktivitas, detail), "Informasi", MyMessageboxConfig.INFORMATION);
    }

    // =========================================================
    // GAGAL — UMUM (dipakai untuk SEMUA jenis error/kesalahan di aplikasi)
    // =========================================================

    /**
     * Menyusun pesan GAGAL/KESALAHAN formal — inti dari kelas ini, dipakai untuk SEMUA jenis
     * error di aplikasi (bukan hanya koneksi Feeder).
     *
     * @param aktivitas     nama aktivitas yang gagal, mis. {@code "penyimpanan data Perkuliahan"};
     *                      boleh {@code null}/kosong
     * @param penyebab      penjelasan SERINCI &amp; SEDETAIL mungkin tentang apa yang menyebabkan
     *                      kesalahan ini (WAJIB diisi pemanggil dengan penjelasan konkret, bukan
     *                      sekadar "terjadi kesalahan"); boleh {@code null}/kosong sebagai fallback
     *                      generik
     * @param langkahSolusi daftar langkah tindak lanjut berurutan yang dapat dicoba pengguna
     *                      (masing-masing SATU kalimat imperatif); boleh {@code null}/kosong
     *                      sebagai fallback generik
     * @return pesan siap tampil
     */
    public static String pesanGagal(String aktivitas, String penyebab, String[] langkahSolusi) {
        StringBuilder sb = new StringBuilder();
        sb.append(sapaan()).append("\n\n");
        sb.append(t("Mohon maaf, telah terjadi kesalahan pada saat memproses"));
        sb.append(" ").append(kosongKe(aktivitas, t("permintaan Bapak/Ibu"))).append(".\n\n");

        sb.append(labelPenyebab()).append(" ");
        if (penyebab != null && penyebab.trim().length() > 0) {
            sb.append(penyebab.trim());
        } else {
            sb.append(t("Sistem tidak dapat menentukan penyebab pasti secara otomatis. Kendala ini "
                    + "kemungkinan bersifat sementara."));
        }

        sb.append("\n\n").append(labelTindakLanjut()).append("\n");
        if (langkahSolusi != null && langkahSolusi.length > 0) {
            for (int i = 0; i < langkahSolusi.length; i++) {
                if (langkahSolusi[i] == null || langkahSolusi[i].trim().length() == 0) {
                    continue;
                }
                sb.append("  ").append(i + 1).append(". ").append(langkahSolusi[i].trim()).append("\n");
            }
        } else {
            sb.append("  1. ").append(t("Silakan ulangi proses ini beberapa saat lagi, kemungkinan "
                    + "kendala bersifat sementara.")).append("\n");
            sb.append("  2. ").append(t("Periksa kembali data/pengaturan yang berkaitan dengan proses "
                    + "ini sebelum mencoba kembali.")).append("\n");
        }

        sb.append("\n").append(eskalasi());
        return sb.toString();
    }

    /** Menampilkan pesan gagal (lihat {@link #pesanGagal}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagal(String aktivitas, String penyebab, String[] langkahSolusi) {
        tampilkanAman(pesanGagal(aktivitas, penyebab, langkahSolusi), "Terjadi Kesalahan",
                MyMessageboxConfig.EXCLAMATION);
    }

    /**
     * Varian {@link #pesanGagal} yang menerima {@link Throwable} langsung (mis. dalam blok
     * {@code catch}) — memakai {@code exception.getMessage()} sebagai rincian teknis tambahan pada
     * bagian Penyebab, digabung dengan penjelasan bisnis dari pemanggil.
     *
     * @param aktivitas       nama aktivitas yang gagal
     * @param penjelasanBisnis penjelasan penyebab dari sudut pandang bisnis/konteks pemanggil
     *                        (boleh {@code null}/kosong)
     * @param exception       exception yang tertangkap (boleh {@code null})
     * @param langkahSolusi   daftar langkah tindak lanjut (boleh {@code null})
     * @return pesan siap tampil
     */
    public static String pesanGagalException(String aktivitas, String penjelasanBisnis, Throwable exception,
            String[] langkahSolusi) {
        String penyebab;
        if (penjelasanBisnis != null && penjelasanBisnis.trim().length() > 0) {
            penyebab = penjelasanBisnis.trim();
        } else {
            penyebab = t("Proses belum dapat diselesaikan. Data tidak diubah agar tetap aman dan konsisten.");
        }
        return pesanGagal(aktivitas, penyebab, langkahSolusi);
    }

    /** Menampilkan pesan gagal-karena-exception (lihat {@link #pesanGagalException}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagalException(String aktivitas, Throwable exception, String[] langkahSolusi) {
        tampilkanGagalDenganDetail(aktivitas, null, exception, langkahSolusi);
    }

    /** Sama seperti {@link #tampilkanGagalException(String, Throwable, String[])} dengan tambahan penjelasan bisnis. */
    public static void tampilkanGagalException(String aktivitas, String penjelasanBisnis, Throwable exception,
            String[] langkahSolusi) {
        tampilkanGagalDenganDetail(aktivitas, penjelasanBisnis, exception, langkahSolusi);
    }

    /**
     * Pada layar ZK, gunakan dialog web bersama yang menyediakan Detail Error dan Copy Error.
     * Bila halaman lama belum memuat pesan-formal.js, tetap jatuh aman ke Messagebox biasa.
     */
    private static void tampilkanGagalDenganDetail(String aktivitas, String penjelasanBisnis,
            Throwable exception, String[] langkahSolusi) {
        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("judul", "Ada kendala");
            data.put("message", (penjelasanBisnis == null || penjelasanBisnis.trim().length() == 0)
                    ? "Proses belum dapat diselesaikan. Data tidak diubah agar tetap aman dan konsisten."
                    : penjelasanBisnis.trim());
            org.json.JSONArray solusi = new org.json.JSONArray();
            if (langkahSolusi != null) {
                for (int i = 0; i < langkahSolusi.length; i++) {
                    if (langkahSolusi[i] != null && langkahSolusi[i].trim().length() > 0) solusi.put(langkahSolusi[i].trim());
                }
            }
            data.put("solusi", solusi);
            data.put("teknis", detailTeknis(exception));
            data.put("referensi", "ZK-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase());
            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                    "if(typeof tampilkanPesanGagalFormal==='function'){tampilkanPesanGagalFormal("
                    + org.json.JSONObject.quote(kosongKe(aktivitas, "proses aplikasi")) + ","
                    + data.toString() + ");}else{alert("
                    + org.json.JSONObject.quote(pesanGagalException(aktivitas, penjelasanBisnis, exception, langkahSolusi))
                    + ");}");
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "PesanFormalHelper.tampilkanGagalDenganDetail");
            tampilkanAman(pesanGagalException(aktivitas, penjelasanBisnis, exception, langkahSolusi),
                    "Terjadi Kesalahan", MyMessageboxConfig.EXCLAMATION);
        }
    }

    private static String detailTeknis(Throwable error) {
        if (error == null) return "Tidak ada detail exception.";
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    // =========================================================
    // Util internal
    // =========================================================

    private static String kosongKe(String s, String pengganti) {
        return (s == null || s.trim().length() == 0) ? pengganti : s.trim();
    }

    /** Tampilkan via {@link MyMessageboxConfig}, gagal-aman (tidak pernah melempar ke pemanggil). */
    private static void tampilkanAman(String pesan, String judul, String ikon) {
        try {
            MyMessageboxConfig.show(pesan, judul, MyMessageboxConfig.OK, ikon);
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/PesanFormalHelper.java:tampilkanAman");
        }
    }
}
