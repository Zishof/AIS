# Perbaikan Error POS/eBisnis 24 Agustus 2026 19:36

## Kelompok akar masalah

1. **Laporan stok per tanggal gagal mengubah satuan menjadi bigint**
   - Fingerprint: `invalid input syntax for type bigint: ""` pada `laporan_jalankan` dengan `r=stok_per_tanggal`.
   - Akar masalah bukan nilai stok, tetapi ekspresi lama `coalesce(pr.satuan,'')` pada kolom relasi `produk.satuan` bertipe bigint.
   - Source server saat ini sudah memakai `left join koperasi.satuan_produk sp` dan `coalesce(sp.nama,'')` (SVN r78219, dilengkapi r78222). Error pukul 19:36 menunjukkan server produksi masih memakai artefak sebelum revisi tersebut dan harus dibangun/deploy dari revisi terkini.

2. **Aksi transaksi dari layar agregat tidak membawa toko**
   - Fingerprint: `layani_transaksi`, ID transaksi tersedia tetapi pesan `Toko tidak diketahui utk akun ini`.
   - Layar agregat memang sah tidak mempunyai `toko_id` terpilih. Server sekarang mencari toko pemilik ID transaksi, memastikan ID hanya menunjuk satu toko, lalu memvalidasi toko tersebut kembali melalui aturan akses pengguna. Ini mempertahankan pengaman IDOR dan tidak membuka akses lintas toko.
   - Klien tetap menyisipkan `toko_id` untuk `layani_transaksi` dan `detail_transaksi` jika toko sedang dipilih.

3. **Akun legacy tanpa tenant dicatat sebagai error**
   - Fingerprint: `tenant_context` dengan kode `TENANT_ACCESS_DENIED`.
   - Kondisi ini sudah diperlakukan sebagai mode legacy/tanpa tenant oleh alur login. Pencatatan error klien sekarang juga mengecualikan respons yang diharapkan ini, tanpa menelan penolakan tenant lain.

4. **Top bar Flutter melampaui lebar layar**
   - Fingerprint: `RenderFlex overflowed ... on the right`.
   - Nama tenant, toko, pengguna, dan label status kini mempunyai batas lebar serta ellipsis. Informasi lengkap tetap tersedia melalui tooltip/menu terkait.

5. **Error koneksi/redirect lama**
   - Fingerprint lama: HTTP 301 menghasilkan HTML, HTTP 503, atau koneksi localhost ditolak.
   - Klien terkini sudah mengikuti redirect same-host secara terbatas dan tidak mengubah redirect lintas host. HTTP 503 serta connection refused tetap dilaporkan karena merupakan kondisi layanan, bukan data bisnis yang boleh dianggap berhasil.

6. **Respons API sukses legacy dicatat sebagai error**
   - Fingerprint: `error_log_health` mengembalikan `status: "00"` dan deskripsi sehat, tetapi tampil sebagai error.
   - Akar masalahnya adalah pemeriksaan global klien hanya menerima `status: "success"`. Klien sekarang menerima kedua kontrak sukses resmi, yaitu `success` dan `00`; status `99`/`error` tetap ditolak.

## Keamanan transaksi dan pengelolaan session

- Query resolusi toko memakai parameter JDBC, bukan konkatenasi input.
- Transaksi database di-rollback bila update gagal.
- `PreparedStatement` dan `ResultSet` ditutup di `finally`.
- Semua `openSession()` yang tersentuh dibersihkan dengan urutan `clear`, `disconnect`, dan `close` di `finally`; `currentSession()` tidak ditutup manual.
- Implementasi tetap kompatibel Java 1.7 dan gaya Java 1.6.

## Verifikasi

- Uji payload toko Flutter mencakup layar dengan toko terpilih dan layar agregat tanpa toko terpilih.
- `flutter analyze` dan uji terkait wajib lulus sebelum publikasi.
- `PosApi.java` serta `LaporanKantinUtil.java` dikompilasi dengan `-source 1.7 -target 1.7` sebelum commit.

## Catatan deployment

Perbaikan `stok_per_tanggal` baru efektif di server setelah aplikasi server dibangun dan artefak terbaru dideploy. Memperbarui Desktop/Android saja tidak mengganti SQL yang dijalankan server.
