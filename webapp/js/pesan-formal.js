/**
 * Menampilkan pesan formal (sukses/gagal) yang konsisten di seluruh halaman JSP aplikasi ini.
 * Reusable -- pola yang sama dipakai di semua halaman JSP/JS, sejalan dengan ais.common.PesanFormalHelper
 * (versi Java untuk layar ZK).
 *
 * Pola pesan gagal WAJIB mengandung 4 bagian:
 *   1. Sapaan formal & permintaan maaf.
 *   2. Penjelasan penyebab (sedetail mungkin).
 *   3. Langkah tindak lanjut yang bisa dicoba pengguna.
 *   4. Eskalasi ke Administrator Sistem / Pengembang Sistem + WAJIB lampirkan screenshot.
 *
 * @param {string} aktivitas     deskripsi singkat aktivitas yang gagal, mis. "penyimpanan data transaksi POS"
 * @param {string} penyebab      penjelasan rinci penyebab kegagalan (boleh "" bila tidak diketahui)
 * @param {string[]} [langkahSolusi] daftar langkah tindak lanjut (array string, boleh kosong/undefined)
 */
function tampilkanPesanGagalFormal(aktivitas, penyebab, langkahSolusi) {
    var pesan = "Yang terhormat Bapak/Ibu Pengguna,\n\n";
    pesan += "Mohon maaf, telah terjadi kesalahan pada saat memproses " + (aktivitas || "permintaan Bapak/Ibu") + ".\n\n";
    pesan += "Penyebab: " + (penyebab && penyebab.length > 0 ? penyebab :
        "Sistem tidak dapat menentukan penyebab pasti secara otomatis. Kendala ini kemungkinan bersifat sementara.") + "\n\n";
    pesan += "Tindak Lanjut yang dapat Bapak/Ibu coba:\n";
    if (langkahSolusi && langkahSolusi.length > 0) {
        for (var i = 0; i < langkahSolusi.length; i++) {
            pesan += "  " + (i + 1) + ". " + langkahSolusi[i] + "\n";
        }
    } else {
        pesan += "  1. Silakan ulangi proses ini beberapa saat lagi.\n";
        pesan += "  2. Periksa kembali data/koneksi internet Bapak/Ibu sebelum mencoba kembali.\n";
    }
    pesan += "\nApabila Bapak/Ibu kurang memahami pesan kesalahan ini atau langkah-langkah di atas belum " +
        "berhasil mengatasi masalah, kami mohon agar segera menghubungi Administrator Sistem, atau " +
        "melaporkan kejadian ini kepada Pengembang Sistem. Mohon WAJIB melampirkan tangkapan layar " +
        "(screenshot) pada saat kesalahan ini terjadi.";
    alert(pesan);
}

/**
 * Menampilkan pesan sukses formal yang konsisten.
 * @param {string} aktivitas  aktivitas yang berhasil diselesaikan
 * @param {string} [detail]   rincian tambahan (opsional)
 */
function tampilkanPesanSuksesFormal(aktivitas, detail) {
    var pesan = "Yang terhormat Bapak/Ibu Pengguna,\n\n";
    pesan += "Dengan ini kami sampaikan bahwa proses " + (aktivitas || "yang Bapak/Ibu jalankan") +
        " telah BERHASIL diselesaikan dengan baik.";
    if (detail && detail.length > 0) {
        pesan += "\n\nRincian: " + detail;
    }
    pesan += "\n\nTerima kasih atas kesabaran Bapak/Ibu.";
    alert(pesan);
}
