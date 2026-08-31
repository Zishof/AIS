package ais.common; // Sesuaikan dengan package Anda

/**
 * Kontrak callback generik untuk melaporkan kemajuan (<i>progress</i>) sebuah proses yang berjalan
 * lama (long-running task) — misalnya proses unggah/unduh berkas berukuran besar, proses ekspor
 * laporan massal, atau proses impor/pemrosesan data batch di modul-modul AIS — kembali ke pemanggil
 * tanpa proses tersebut perlu mengetahui bagaimana kemajuannya akan ditampilkan (mis. progress bar
 * ZK, log konsol, atau notifikasi lain).
 *
 * <p>
 * Pola pemakaiannya khas pola <i>observer</i>/<i>callback</i>: kode yang menjalankan tugas panjang
 * menerima sebuah implementasi {@link ProgressListener} (mis. lewat parameter konstruktor atau
 * method), lalu memanggil {@link #onProgress(int, String)} secara berkala dari dalam loop
 * pemrosesannya untuk melaporkan persentase penyelesaian beserta pesan status terkini. Pemanggil
 * (mis. komponen UI ZK) mengimplementasikan antarmuka ini untuk memperbarui elemen visual (progress
 * bar, label status) sesuai laporan yang diterima, sehingga logika pemrosesan tetap terpisah penuh
 * (<i>decoupled</i>) dari logika tampilan.
 * </p>
 *
 * <p>
 * Antarmuka ini sengaja dibuat sangat sederhana (satu method) agar mudah diimplementasikan sebagai
 * kelas anonim atau ekspresi lambda pada titik pemanggilan, tanpa perlu membuat kelas terpisah
 * hanya untuk menerima laporan kemajuan.
 * </p>
 */
public interface ProgressListener {
    /**
     * Dipanggil oleh proses yang sedang berjalan setiap kali kemajuannya perlu dilaporkan.
     *
     * @param percent persentase penyelesaian proses, umumnya bernilai 0 sampai 100 (implementasi
     *                pemanggil bertanggung jawab menjaga rentang nilai ini tetap valid)
     * @param message pesan status/keterangan singkat yang menyertai persentase tersebut, misalnya
     *                nama berkas yang sedang diproses atau tahap yang sedang berlangsung
     */
    void onProgress(int percent, String message);
}