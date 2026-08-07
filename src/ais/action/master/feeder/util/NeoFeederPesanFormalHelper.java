package ais.action.master.feeder.util;

import ais.ui.util.MyMessageboxConfig;

/**
 * {@code NeoFeederPesanFormalHelper} — utilitas <b>bersama (reuse)</b> untuk menyusun &amp;
 * menampilkan seluruh pesan (sukses, peringatan, error) yang berkaitan dengan koneksi/otentikasi ke
 * Neo Feeder — memakai bahasa yang <b>formal, sopan, dan mudah dipahami</b> oleh semua pengguna
 * (bukan hanya administrator/teknisi).
 *
 * <h3>Latar belakang</h3>
 * Sebelumnya pesan koneksi Feeder sangat singkat dan teknis, mis. {@code "Username atau password
 * salah"} atau {@code "Tidak bisa connect atau username atau password salah"} — tidak menjelaskan
 * <i>mengapa</i> hal itu terjadi maupun <i>apa</i> yang sebaiknya dilakukan pengguna. Kelas ini
 * menutup celah tersebut: setiap pesan yang dihasilkan SELALU memuat empat bagian standar:
 * <ol>
 *   <li><b>Judul</b> — ringkasan status (berhasil/gagal) dalam bahasa formal.</li>
 *   <li><b>Penyebab</b> — penjelasan rinci &amp; konkret tentang apa yang terjadi, diterjemahkan
 *       dari detail teknis (pesan server, jenis kegagalan) menjadi kalimat yang mudah dipahami.</li>
 *   <li><b>Tindak Lanjut</b> — langkah-langkah konkret &amp; berurutan yang dapat dicoba pengguna
 *       sendiri untuk mengatasi masalah tersebut.</li>
 *   <li><b>Eskalasi</b> — anjuran menghubungi Administrator Sistem atau melaporkan ke Pengembang
 *       Sistem (WAJIB melampirkan tangkapan layar/screenshot saat kesalahan terjadi) apabila
 *       langkah-langkah di atas belum menyelesaikan masalah.</li>
 * </ol>
 *
 * <h3>Dipakai di titik pusat, bukan ditulis ulang di tiap layar</h3>
 * Method {@link #tampilkanSukses(String, String)}, {@link #tampilkanGagalKoneksi(String, String, boolean, String)},
 * {@link #tampilkanGagalLogin(String, String)}, dan {@link #tampilkanGagalUmum(String, String, String)}
 * langsung menampilkan {@link MyMessageboxConfig} dengan format baku di atas — sehingga layar
 * manapun yang berurusan dengan koneksi Neo Feeder (Pengaturan Koneksi, Test Koneksi, Coba Login
 * Feeder, dan pola serupa lainnya) cukup memanggil SATU method, tanpa menulis ulang teks pesan.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 (tanpa lambda/try-with-resources multi-catch).
 *
 * @author Tim AIS
 */
public final class NeoFeederPesanFormalHelper {

    private NeoFeederPesanFormalHelper() {
    }

    /** Baris eskalasi standar — dipakai di SEMUA pesan gagal agar konsisten. */
    private static final String ESKALASI =
        "Apabila Bapak/Ibu kurang memahami pesan kesalahan ini atau langkah-langkah di atas belum "
        + "berhasil mengatasi masalah, kami mohon agar segera menghubungi Administrator Sistem di "
        + "lingkungan Bapak/Ibu, atau melaporkan kejadian ini kepada Pengembang Sistem. Untuk "
        + "mempercepat proses penanganan, mohon WAJIB melampirkan tangkapan layar (screenshot) pada "
        + "saat kesalahan ini terjadi beserta waktu kejadiannya.";

    // =========================================================
    // SUKSES
    // =========================================================

    /**
     * Menyusun pesan SUKSES formal.
     *
     * @param aktivitas nama aktivitas yang berhasil, mis. {@code "Uji Koneksi dan Login ke Neo Feeder"}
     * @param detail    rincian tambahan yang relevan (boleh {@code null}/kosong), mis. nama server
     *                  yang berhasil dihubungi
     * @return pesan siap tampil
     */
    public static String pesanSukses(String aktivitas, String detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("Yang terhormat Bapak/Ibu Pengguna,\n\n");
        sb.append("Dengan ini kami sampaikan bahwa proses ");
        sb.append(kosongKe(aktivitas, "yang Bapak/Ibu jalankan"));
        sb.append(" telah BERHASIL diselesaikan dengan baik.");
        if (detail != null && detail.trim().length() > 0) {
            sb.append("\n\nRincian: ").append(detail.trim());
        }
        sb.append("\n\nTerima kasih atas kesabaran Bapak/Ibu. Sistem siap digunakan untuk proses "
                + "sinkronisasi data selanjutnya.");
        return sb.toString();
    }

    /** Menampilkan pesan sukses (lihat {@link #pesanSukses(String, String)}) via {@link MyMessageboxConfig}. */
    public static void tampilkanSukses(String aktivitas, String detail) {
        tampilkanAman(pesanSukses(aktivitas, detail), "Informasi",
                MyMessageboxConfig.INFORMATION);
    }

    // =========================================================
    // GAGAL — KONEKSI (IP/Port/jaringan tidak terjangkau)
    // =========================================================

    /**
     * Menyusun pesan GAGAL KONEKSI formal (mis. alamat IP/Port Feeder tidak dapat dihubungi).
     *
     * @param ip           alamat IP/host Feeder yang dicoba
     * @param port         port yang dicoba
     * @param https        apakah mode secure (https) sedang diaktifkan
     * @param detailTeknis pesan teknis mentah dari sistem (boleh {@code null}/kosong) — akan
     *                     diterjemahkan menjadi penjelasan yang mudah dipahami
     * @return pesan siap tampil
     */
    public static String pesanGagalKoneksi(String ip, String port, boolean https, String detailTeknis) {
        String dt = kosongKe(detailTeknis, "").toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("Yang terhormat Bapak/Ibu Pengguna,\n\n");
        sb.append("Mohon maaf, sistem TIDAK BERHASIL menghubungi server Neo Feeder pada alamat \"")
          .append(kosongKe(ip, "-")).append("\" dengan port \"").append(kosongKe(port, "-"))
          .append("\" (mode koneksi: ").append(https ? "HTTPS/secure" : "HTTP biasa").append(").\n\n");

        sb.append("Penyebab: ");
        if (dt.indexOf("curl") >= 0) {
            sb.append("Server aplikasi eCampus tidak dapat menjalankan perintah jaringan ('curl') yang "
                    + "diperlukan untuk menghubungi Neo Feeder. Kemungkinan besar terdapat kendala pada "
                    + "konfigurasi server tempat aplikasi eCampus berjalan.");
        } else if (dt.indexOf("bukan json") >= 0 || dt.indexOf("respons") >= 0) {
            sb.append("Server pada alamat dan port tersebut MEREKSPONS, namun jawaban yang diterima "
                    + "bukan format data yang dikenali oleh sistem. Hal ini biasanya terjadi karena: "
                    + "(1) alamat IP/Port yang dituju BUKAN alamat layanan Neo Feeder yang sebenarnya, "
                    + "(2) mode HTTPS/secure yang diaktifkan tidak sesuai dengan pengaturan server "
                    + "Feeder yang sesungguhnya, atau (3) terdapat halaman perantara (mis. proxy, "
                    + "firewall, atau halaman login jaringan) yang menghalangi permintaan sebelum "
                    + "mencapai layanan Neo Feeder.");
        } else if (dt.indexOf("timeout") >= 0 || dt.indexOf("waktu") >= 0) {
            sb.append("Permintaan ke server Neo Feeder MELEBIHI BATAS WAKTU tunggu (timeout). Hal ini "
                    + "umumnya disebabkan oleh koneksi jaringan yang lambat, server Feeder yang sedang "
                    + "sibuk/tidak responsif, atau adanya pemblokiran oleh firewall di salah satu sisi.");
        } else if (dt.indexOf("refused") >= 0 || dt.indexOf("ditolak") >= 0) {
            sb.append("Server pada alamat tersebut MENOLAK permintaan koneksi. Hal ini umumnya berarti "
                    + "tidak ada layanan yang aktif pada port yang dituju, atau port tersebut diblokir "
                    + "oleh firewall.");
        } else {
            sb.append("Alamat IP dan/atau Port yang dituju tidak dapat dijangkau oleh server aplikasi "
                    + "eCampus. Hal ini dapat disebabkan oleh: (1) alamat IP atau nomor Port yang "
                    + "dimasukkan kurang tepat, (2) server Neo Feeder sedang tidak aktif/dalam "
                    + "pemeliharaan, (3) tidak ada jalur jaringan (koneksi internet/intranet) antara "
                    + "server eCampus dan server Neo Feeder, atau (4) pengaturan mode HTTPS/secure "
                    + "tidak sesuai dengan yang seharusnya.");
        }
        if (detailTeknis != null && detailTeknis.trim().length() > 0) {
            sb.append("\n\nRincian teknis (untuk keperluan pelaporan): ").append(detailTeknis.trim());
        }

        sb.append("\n\nTindak Lanjut yang dapat Bapak/Ibu coba:\n");
        sb.append("  1. Periksa kembali kebenaran Alamat IP dan Port Aplikasi Feeder pada menu "
                + "Pengaturan Koneksi, pastikan tidak ada kesalahan pengetikan.\n");
        sb.append("  2. Pastikan pengaturan 'Menggunakan secure (https)' sudah sesuai dengan jenis "
                + "layanan Neo Feeder yang digunakan (aktifkan bila layanan memakai https, matikan "
                + "bila memakai http biasa).\n");
        sb.append("  3. Pastikan server tempat aplikasi eCampus berjalan memiliki akses jaringan "
                + "(internet/intranet) menuju alamat server Neo Feeder tersebut.\n");
        sb.append("  4. Pastikan layanan Neo Feeder pada sisi Perguruan Tinggi/PDDikti sedang aktif "
                + "dan tidak dalam masa pemeliharaan.\n");
        sb.append("  5. Bila menggunakan firewall/proxy, pastikan alamat dan port tersebut tidak "
                + "diblokir.\n");
        sb.append("  6. Setelah melakukan pemeriksaan di atas, silakan coba kembali menekan tombol "
                + "Test Koneksi/Coba Login Feeder.\n\n");
        sb.append(ESKALASI);
        return sb.toString();
    }

    /** Menampilkan pesan gagal koneksi (lihat {@link #pesanGagalKoneksi}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagalKoneksi(String ip, String port, boolean https, String detailTeknis) {
        tampilkanAman(pesanGagalKoneksi(ip, port, https, detailTeknis), "Gagal Menghubungi Server Neo Feeder",
                MyMessageboxConfig.EXCLAMATION);
    }

    // =========================================================
    // GAGAL — LOGIN/OTENTIKASI (username/password/token)
    // =========================================================

    /**
     * Menyusun pesan GAGAL LOGIN/OTENTIKASI formal.
     *
     * @param username     nama pengguna (username) Feeder yang dicoba (boleh {@code null})
     * @param detailTeknis pesan teknis mentah dari server Feeder (mis. {@code error_desc}); boleh
     *                     {@code null}/kosong
     * @return pesan siap tampil
     */
    public static String pesanGagalLogin(String username, String detailTeknis) {
        String dt = kosongKe(detailTeknis, "").toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("Yang terhormat Bapak/Ibu Pengguna,\n\n");
        sb.append("Mohon maaf, proses masuk (login) ke server Neo Feeder ")
          .append(username == null || username.trim().isEmpty() ? "" : "dengan nama pengguna \"" + username.trim() + "\" ")
          .append("TIDAK BERHASIL dilakukan.\n\n");

        boolean kredensial = dt.indexOf("password") >= 0 || dt.indexOf("username") >= 0 || dt.indexOf("salah") >= 0
                || dt.indexOf("invalid") >= 0;
        boolean terkunci = dt.indexOf("kunci") >= 0 || dt.indexOf("locked") >= 0 || dt.indexOf("blokir") >= 0;
        boolean kosong = dt.trim().length() == 0;
        // FIX "tampilkan saja errornya apa, lebih dipersingkat": bila error_desc mentah dari server
        // TIDAK cocok kategori kredensial/terkunci yang sudah dikenal (mis. "Periksa Koneksi Internet
        // Anda" -- bukan soal username/password), jangan paksakan Tindak Lanjut 5-langkah yang
        // berasumsi salah kredensial (menyesatkan). Tampilkan ringkas: pesan asli dari server + saran
        // singkat yang netral.
        if (!kredensial && !terkunci && !kosong) {
            sb.append("Server Neo Feeder mengembalikan keterangan berikut: \"").append(detailTeknis.trim())
              .append("\".\n\nSilakan periksa kembali Pengaturan Koneksi (IP/Port/Username/Password) dan "
                    + "koneksi jaringan server eCampus ke Neo Feeder, lalu coba tekan tombol Coba Login "
                    + "Feeder kembali.\n\n");
            sb.append(ESKALASI);
            return sb.toString();
        }

        sb.append("Penyebab: ");
        if (kredensial) {
            sb.append("Nama pengguna (username) dan/atau kata sandi (password) yang dimasukkan TIDAK "
                    + "SESUAI dengan data yang terdaftar pada server Neo Feeder (PDDikti).");
        } else if (terkunci) {
            sb.append("Akun Neo Feeder yang digunakan kemungkinan sedang DITERKUNCI/DIBLOKIR oleh "
                    + "sistem PDDikti, umumnya akibat beberapa kali percobaan login yang gagal "
                    + "sebelumnya.");
        } else {
            sb.append("Server Neo Feeder tidak mengembalikan kode akses (token) yang sah. Penyebab "
                    + "paling umum adalah nama pengguna dan/atau kata sandi yang tidak sesuai, namun "
                    + "dapat juga disebabkan oleh gangguan sementara pada layanan Neo Feeder.");
        }

        sb.append("\n\nTindak Lanjut yang dapat Bapak/Ibu coba:\n");
        sb.append("  1. Periksa kembali kebenaran Username dan Password Feeder pada menu Pengaturan "
                + "Koneksi, pastikan tidak ada kesalahan pengetikan (perhatikan huruf besar/kecil).\n");
        sb.append("  2. Pastikan akun (username) tersebut memang terdaftar dan berstatus aktif pada "
                + "sistem Neo Feeder/PDDikti Perguruan Tinggi Bapak/Ibu.\n");
        sb.append("  3. Bila baru saja mengganti kata sandi pada portal Feeder/PDDikti, mohon perbarui "
                + "juga kata sandi pada Pengaturan Koneksi eCampus agar tetap sesuai.\n");
        sb.append("  4. Bila sudah beberapa kali gagal, mohon tunggu beberapa saat sebelum mencoba "
                + "kembali (untuk menghindari pemblokiran sementara oleh sistem PDDikti).\n");
        sb.append("  5. Setelah melakukan pemeriksaan di atas, silakan coba kembali menekan tombol "
                + "Coba Login Feeder.\n\n");
        sb.append(ESKALASI);
        return sb.toString();
    }

    /** Menampilkan pesan gagal login (lihat {@link #pesanGagalLogin}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagalLogin(String username, String detailTeknis) {
        tampilkanAman(pesanGagalLogin(username, detailTeknis), "Gagal Masuk (Login) ke Neo Feeder",
                MyMessageboxConfig.EXCLAMATION);
    }

    // =========================================================
    // GAGAL — UMUM (fallback untuk kesalahan tak terduga lainnya)
    // =========================================================

    /**
     * Menyusun pesan GAGAL UMUM formal — dipakai sebagai fallback untuk kesalahan yang tidak
     * termasuk kategori koneksi maupun login (mis. kesalahan tak terduga/exception internal).
     *
     * @param aktivitas    nama aktivitas yang gagal, mis. {@code "Sinkronisasi data ke Neo Feeder"}
     * @param detailTeknis pesan teknis (mis. {@code exception.getMessage()}); boleh {@code null}/kosong
     * @param langkahTambahan langkah tindak lanjut tambahan yang spesifik untuk aktivitas ini (boleh
     *                        {@code null}) — akan ditambahkan SEBELUM baris eskalasi
     * @return pesan siap tampil
     */
    public static String pesanGagalUmum(String aktivitas, String detailTeknis, String langkahTambahan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Yang terhormat Bapak/Ibu Pengguna,\n\n");
        sb.append("Mohon maaf, telah terjadi kendala yang tidak terduga pada saat memproses ");
        sb.append(kosongKe(aktivitas, "permintaan Bapak/Ibu"));
        sb.append(".\n\n");
        sb.append("Penyebab: ");
        if (detailTeknis != null && detailTeknis.trim().length() > 0) {
            sb.append("Sistem mencatat keterangan teknis sebagai berikut: \"").append(detailTeknis.trim())
              .append("\". Keterangan ini dapat membantu Administrator Sistem maupun Pengembang Sistem "
                      + "dalam menelusuri akar permasalahan.");
        } else {
            sb.append("Sistem tidak dapat menentukan penyebab pasti secara otomatis. Kendala ini "
                    + "kemungkinan bersifat sementara.");
        }

        sb.append("\n\nTindak Lanjut yang dapat Bapak/Ibu coba:\n");
        sb.append("  1. Silakan ulangi proses ini beberapa saat lagi, kemungkinan kendala bersifat "
                + "sementara.\n");
        sb.append("  2. Periksa kembali data/pengaturan yang berkaitan dengan proses ini sebelum "
                + "mencoba kembali.\n");
        if (langkahTambahan != null && langkahTambahan.trim().length() > 0) {
            sb.append("  3. ").append(langkahTambahan.trim()).append("\n");
        }
        sb.append("\n").append(ESKALASI);
        return sb.toString();
    }

    /** Menampilkan pesan gagal umum (lihat {@link #pesanGagalUmum}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagalUmum(String aktivitas, String detailTeknis, String langkahTambahan) {
        tampilkanAman(pesanGagalUmum(aktivitas, detailTeknis, langkahTambahan), "Terjadi Kendala",
                MyMessageboxConfig.EXCLAMATION);
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
            ais.common.ErrorAuditUtil.record(t,
                    "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederPesanFormalHelper.java:tampilkanAman");
        }
    }
}
