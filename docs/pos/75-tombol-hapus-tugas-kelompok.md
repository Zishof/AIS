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

## Analisis revisi dan timeline

- Revisi SVN `r78057` (22 Agustus 2026) menerapkan pola menu aksi baris
  `...`. `MenuAksiBaris` memindahkan tombol toolbar ke `Menupopup`, tetapi
  tetap meneruskan klik ke handler tombol aslinya.
- Revisi `r78678` (31 Agustus 2026 pukul 21:49 WIB) memperkenalkan kartu detail
  dan panel hijau **Pengaturan Tugas Kelompok**. Panel baru hanya memasang
  **Ubah Judul & Instruksi**, **Tambah Kelompok**, serta **Kelola Kelompok &
  Anggota**. Aksi hapus tidak ikut dipindahkan dan tetap berada di menu `...`
  lama pada bagian bawah detail.
- Akun pada tangkapan layar sudah menampilkan panel pengaturan. Panel tersebut
  hanya dibuat bila `bolehKelola(Common.getCurrentUser())` bernilai benar;
  karena itu masalah bukan berasal dari peran atau hak akses akun.
- Laporan/tangkapan layar diterima 2 September 2026 pukul 21:13 WIB. Perbaikan
  baru masuk pada `r83340` pukul 22:09 WIB, sehingga tangkapan layar memang
  berasal dari build sebelum perbaikan.

Secara teknis aksi hapus sebelumnya masih dapat dicapai melalui menu `...`,
tetapi penempatannya tidak mudah ditemukan dan tidak konsisten dengan tiga
aksi pengelolaan lain pada panel utama. Karena itu keluhan pengguna bahwa tombol
hapus tidak tersedia adalah masalah discoverability antarmuka yang valid.

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

## Draf balasan WhatsApp

> Waalaikumsalam Pak Catur. Sudah kami cek sampai ke source code. Penyebabnya
> bukan hak akses Bapak dan bukan data tugasnya. Saat tampilan Tugas Kelompok
> diperbarui, tombol Ubah, Tambah Kelompok, dan Kelola Anggota dipindahkan ke
> panel Pengaturan yang baru, sedangkan tombol Hapus masih tertinggal di menu
> lama bertanda `...` pada bagian bawah. Karena itu tombolnya terlihat seperti
> tidak tersedia. Perbaikannya sudah kami masukkan: tombol **Hapus Tugas
> Kelompok** sekarang ditampilkan langsung pada panel Pengaturan, berwarna merah
> dan dilengkapi konfirmasi agar tidak terhapus tanpa sengaja. Perubahan akan
> terlihat setelah versi ECAMPUS terbaru selesai di-build dan di-deploy. Untuk
> sementara, opsi hapus masih dapat dibuka melalui menu `...` di bagian bawah
> detail tugas. Setelah deploy, mohon muat ulang halaman atau login kembali lalu
> dicek. Terima kasih atas laporannya, Pak.
