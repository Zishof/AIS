# 08 — Harness uji

## 1. Kenapa harness, bukan unit test

Kode server AIS terikat erat ke Hibernate, ZK, dan basis data nyata. Menguji aturan
bisnisnya secara terisolasi berarti menulis tiruan yang justru menyembunyikan perbedaan
perilaku. Karena itu setiap modul diuji dengan **program Java kecil** yang memanggil
API helper yang sebenarnya, terhadap basis data UAT, lalu memeriksa hasilnya lewat SQL.

Aturan yang dipegang seluruh harness:

- **Data uji diberi awalan khas** (`UATPR`, `UATDPC`, `UATPOS`, `UATTRF`, `UATBKK`, …)
  supaya bisa dihapus tuntas.
- **Dibersihkan di blok `finally`**, dan barisnya dihitung ulang di akhir — laporan
  ditutup dengan `sisa data uji = 0`.
- **Data master yang dipinjam dikembalikan.** `TesPostingPjKasBesar` mengisi
  `jenis_kas_besar.akun_penerima` sementara, lalu mengembalikannya ke nilai semula.
- **Baris bawaan tidak disentuh.** `TesDpcKeuangan` mencatat jumlah baris DPC sebelum dan
  sesudah, dan memastikan angkanya kembali sama.

## 2. Cara menjalankan

Harness dan skrip pembantunya ada di direktori scratchpad sesi kerja, bukan di
repositori. Dua skrip yang dipakai:

| Skrip | Guna |
|---|---|
| `kompilasi_api.bat` | kompilasi ke `outjava` dengan `-source 1.7 -target 1.7`; `outjava` didahulukan pada classpath supaya kelas hasil patch terpakai |
| `jalankan_baru.bat` | menjalankan kelas uji dengan `outjava` di depan classpath |

Kredensial basis data **tidak pernah ditulis di skrip**. Keduanya memanggil `setenv.bat`
milik Tomcat UAT, yang menyuntikkan sambungan lewat `CATALINA_OPTS`.

Satu kali jalan memakan beberapa menit karena Hibernate membangun ulang session factory
dan memeriksa seluruh skema.

## 3. Daftar harness dan hasil terakhir

### Modul Keuangan

| Harness | Hasil | Cakupan |
|---|---|---|
| `TesUangMuka` | 22/22 | validasi, saldo anggaran, simpan, persetujuan |
| `TesPjUangMuka` | 20/20 | perhitungan pajak/PPN, penolakan bila melebihi uang muka |
| `TesKasBesar` | 20/20 | rincian nol, ambil dari kas kecil, penomoran |
| `TesPjKasBesar` | 18/18 | perbandingan pecahan (bukan dibulatkan) |
| `TesKasKecil` | 19/19 | akun wajib, saldo, satu dokumen menggantung per jenis |
| `TesPenggantianKasKecil` | 20/20 | pemilihan dokumen kas kecil, pembaruan induk, tautan balik |

### Lintas modul

| Harness | Hasil | Cakupan |
|---|---|---|
| `TesAnggaranKeuangan` | 19/19 | anggaran per baris rincian, tebakan dari akun, hapus melepas anggaran |
| `TesDpcKeuangan` | 21/21 | gerbang persetujuan, idempoten, status pada daftar, LPJ tanpa kembalian |
| `TesUangMukaDariPr` | 23/23 | pemilih PR, nilai dari PR, tautan balik, hapus |
| `TesHakMenuAkuntansi` | 57/57 | hak menu & per aksi |
| `TesCrudKodeAkun` | 20/20 | CRUD lima tab Kode Akun |

### Posting jurnal

| Harness | Hasil | Cakupan |
|---|---|---|
| `TesPostingLpj` | 13/13 | jurnal seimbang 1.000.000, selisih 200.000 ke akun kelebihan, posting ulang ditolak |
| `TesPostingPjKasBesar` | 10/10 | jurnal seimbang 600.000 |
| `TesPostingTransfer` | 17/17 | Uang Muka 750.000 & Penggantian Kas Kecil 320.000, plus keenam baris dasbor muncul dan menyatakan punya mesin posting |
| `TesBatalKasKecil` | 6/6 | uji regresi: pembatalan benar-benar terjadi, bukan sekadar dilaporkan |

`TesPostingTransfer` merangkai sendiri satu baris `akunting.proses_transfer` dan
menautkannya ke baris DPC, karena layar Proses Transfer belum diport. Baris rangkaian itu
ikut dihapus di akhir.

Harness itu juga memeriksa **keenam baris dasbor benar-benar dirender** dan menyatakan
`bisaPosting`. Pemeriksaan ini ditambahkan setelah ketahuan bahwa `pj_kas_besar` dan
`penggantian_kas_kecil` sempat punya mesin dan kriteria tetapi kuncinya belum masuk
`DraftJurnalRingkasanUtil.KUNCI` — mesinnya jalan, tombolnya tidak pernah terlihat. Uji
per-modul saja tidak menangkap kelalaian seperti itu karena ia memanggil lewat nama
modul, bukan lewat daftar yang dirender.

## 4. Uji sisi klien

Sisi Flutter diuji dengan `flutter test` biasa. Yang terkait pekerjaan ini:

| Berkas uji | Isi |
|---|---|
| `sidebar_akuntansi_test.dart` | grup Akuntansi & submenunya di kedua platform |
| `sidebar_keuangan_test.dart` | grup Keuangan, fail-closed, perpindahan Pajak & Pembayaran Vendor |
| `keuangan_lokal_dulu_test.dart` | lokal-dulu, hapus lunak, dasbor & cetak, anggaran per baris, muara DPC |
| `uang_muka_kontrak_test.dart` | kontrak aksi/muatan, hak per aksi, penyaring, "diambil dari PR" |
| `kode_akun_crud_kontrak_test.dart` | tombol CRUD Kode Akun |
| `drawer_akuntansi_halaman_test.dart` | layar bertab dibungkus Scaffold agar aman dibuka dari drawer |
| `draft_jurnal_kontrak_test.dart` | kontrak dasbor Draft Jurnal & tombol posting |

Seluruh suite terakhir dijalankan: **221 lulus**, `flutter analyze` 0 error.
