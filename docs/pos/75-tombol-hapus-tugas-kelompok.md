# 75. Tombol Hapus pada Detail Tugas Kelompok

Tanggal: 2 September 2026

## Laporan

Catur STTIF melaporkan bahwa detail Tugas Kelompok hanya menampilkan **Ubah
Judul & Instruksi**, **Tambah Kelompok**, dan **Kelola Kelompok & Anggota**.
Tidak ada tombol Hapus yang dapat ditemukan pada panel pengaturan utama.

## Akar masalah

Handler hapus sebenarnya masih tersedia pada toolbar lama yang kemudian diserap
ke menu aksi `...`. Ketika kartu ringkas dan panel **Pengaturan Tugas Kelompok**
ditambahkan, tiga aksi lain dipasang ke panel tersebut tetapi aksi hapus
tertinggal. Data dan hak akses bukan penyebabnya.

## Perbaikan

`TugasKelompokHelper.DetailPerkuliahanRenderer` sekarang menampilkan tombol
merah **Hapus Tugas Kelompok** di bagian paling bawah panel pengaturan.

- tombol hanya lahir di dalam panel yang dijaga `bolehKelola(...)`, sehingga
  mahasiswa, siswa, calon peserta, dan peserta kursus tidak mendapat akses;
- klik tombol selalu menampilkan konfirmasi yang menyebut judul tugas;
- setelah berhasil, daftar dimuat ulang dan listener induk diberi tahu;
- kegagalan karena nilai/relasi lain tetap memakai pesan formal lama;
- tombol toolbar lama dan tombol panel baru memakai satu handler
  `konfirmasiHapusTugasKelompok`, agar perilakunya tidak menyimpang.

## Berkas

Kedua mirror berikut diubah dan diverifikasi identik:

- `src/main/java/ais/action/master/helper/TugasKelompokHelper.java`;
- `src/main/src/ais/action/master/helper/TugasKelompokHelper.java`.

## Verifikasi

- kompilasi target Java 7 lulus dengan classpath hasil build + pustaka web;
- dua mirror memiliki SHA-256 yang sama;
- kontrak sumber: dua pembuat tombol Hapus (panel + toolbar), dua pemanggil
  handler bersama, dan hanya satu operasi `Common.refreshDelete`;
- panel pengaturan tetap dijaga oleh `bolehKelola(Common.getCurrentUser())`.

Percobaan kompilasi yang membiarkan `javac` menelusuri seluruh source tree
menemukan tiga galat lama/tidak terkait pada `OnlineBmtUtil.java` tentang
`JSONException`. Karena itu verifikasi perubahan ini dijalankan secara terarah
terhadap kelas target memakai kelas hasil build yang sudah ada.

## Operasional

Perubahan membutuhkan rebuild/deploy aplikasi web ECAMPUS. Sesudah deploy,
UAT dilakukan dengan login dosen/admin: buka pertemuan, tab Tugas, pilih tugas
kelompok, pastikan tombol terlihat, tekan Hapus, lalu uji **Batal** dan **OK**.
Jangan menguji OK pada data produksi yang masih diperlukan.
