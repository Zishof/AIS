# Serah Terima (Handover): UAT & Manual Bergambar Nyata — untuk Sesi Lain

Dokumen ini untuk sesi/agen LAIN yang akan melanjutkan pekerjaan ini dengan akses ke sesi
desktop yang benar-benar interaktif (mis. lewat vnc.ebisnis.id) — supaya tidak perlu menggali
ulang apa yang sudah ditemukan sesi ini. Tujuan akhirnya: manual pengguna yang sama isinya
dengan `manual-posting-keuangan/`, tetapi memakai TANGKAPAN LAYAR ASLI, bukan ilustrasi.

## Pembaruan hasil kelanjutan UAT (4 September 2026)

Pekerjaan pada handover ini telah dilanjutkan tanpa VNC memakai Flutter integration test yang
merender aplikasi Windows eBisnis secara langsung. Tangkapan layar asli tersimpan di
`docs/pos/manual-posting-keuangan/screenshots-uat/`, sedangkan manual hasil akhirnya bernama
`Manual-UAT-Akuntansi-eBisnis.docx` dan `Manual-UAT-Akuntansi-eBisnis.pdf`.

Data sampel kini mencakup delapan jurnal manual terposting, lima saldo awal terposting,
tiga jurnal penyesuaian, dua faktur kulakan, dua pembayaran hutang, dua penerimaan piutang,
tiga item anggaran, serta pemetaan 256 akun yang sebelumnya belum terhubung ke Kelompok
Laporan. Enam laporan inti telah dirender ulang untuk periode 1–30 September 2026.

Temuan yang masih terbuka: akun Persediaan pada Master Aset menahan Posting HPP/Kulakan;
Posting Penjualan dapat mempertahankan state layar HPP; Tutup Buku tidak membaca pemetaan Laba
Ditahan toko; RAB Bulanan overflow pada lebar 1264 px; dan setelah restart server endpoint
`jurnal_umum_daftar` mengalami `SERVER_ERROR` (referensi `API-MTLWU4YI`). Closing final dan
Tutup Buku final sengaja tidak dieksekusi agar periode basis data demo bersama tidak terkunci.

## Pembaruan UAT Keuangan dan integrasi Akuntansi (4 September 2026)

UAT berikutnya juga telah dijalankan langsung pada build Windows varian eBisnis tanpa VNC.
Seluruh 12 submenu grup **KEUANGAN** dibuka, formulir utama diverifikasi, data sampel realistis
ditambahkan, proses transfer direalisasikan, dan hasil jurnalnya ditelusuri sampai **AKUNTANSI →
Draft Jurnal** dan laporan. Bukti asli berjumlah 37 PNG dan tersimpan di
`docs/pos/manual-keuangan-akuntansi/screenshots-uat/`.

Manual final telah dibuat dan diperiksa visual per halaman:

- `docs/pos/manual-keuangan-akuntansi/Manual-UAT-Keuangan-dan-Integrasi-Akuntansi-eBisnis.docx`
- `docs/pos/manual-keuangan-akuntansi/Manual-UAT-Keuangan-dan-Integrasi-Akuntansi-eBisnis.pdf`

Keduanya berisi 37 halaman A4, 35 gambar bernomor, matriks UAT, pola jurnal, checklist, serta
daftar defect/retest. Generatornya ada di
`docs/pos/manual-keuangan-akuntansi/build_manual_keuangan.py`.

Kondisi data setelah penyiapan:

- Uang Muka 3 dokumen (Rp4.650.000) dan 2 pertanggungjawaban (Rp2.650.000; pengembalian
  Rp200.000).
- Kas Besar 3 dokumen (Rp2.700.089) dan 2 pertanggungjawaban (Rp2.600.000; pengembalian
  Rp100.000).
- Kas Kecil 2 dokumen (Rp450.000), 2 penggantian (Rp450.000), dan 1 Dana Talangan
  (Rp750.000).
- Proses Transfer: 7 terealisasi, 1 menunggu realisasi, dan 1 DPC belum diproses. Empat
  proses baru menghasilkan satu jurnal otomatis masing-masing.
- Master Data Keuangan: 60 Jenis Pengeluaran, 9 Kategori Biaya Sales, dan 2 penerima Kas
  Besar yang sebelumnya belum lengkap kini sudah dipetakan. Sebelas alur dokumen memakai
  templat nomor `FIN/0000/IX/2026`.
- Draft Jurnal periode uji menampilkan 81 aktivitas: 51 draft, 30 terposting, 0 closing.
  Kategori Uang Muka dan Kas berisi 15 aktivitas (13 terposting), sedangkan Pengajuan
  Transfer berisi 7 aktivitas dan seluruhnya terposting.
- Laporan Laba Rugi, Arus Kas, dan Keseluruhan Jurnal berhasil menampilkan nilai jurnal
  terposting periode 1–30 September 2026.

Temuan terbuka yang tidak boleh disamarkan sebagai data kosong biasa:

1. **Reimbursement Pegawai — BLOCKED.** Penyimpanan ditolak `ConstraintViolationException`
   karena kolom wajib `atasan` tidak diisi oleh API saat ini. Form dapat dibuka, tetapi data
   contoh tidak dapat dibuat melalui kontrak API yang tersedia.
2. **Bayar Pajak — PARTIAL.** Dua BAST PPN senilai total Rp220.000 tampil pada Pajak
   Terutang, tetapi penyetoran gagal karena kolom wajib `jenis_pajak_barang` tidak diisi
   untuk baris PPN. Riwayat Setoran tetap kosong.
3. Satu pertanggungjawaban Kas Besar lama masih gagal diposting karena referensi akun data
   warisan; pertanggungjawaban sampel baru berhasil diposting.
4. Endpoint `jurnal_umum_daftar` tetap mengembalikan `SERVER_ERROR`; integrasi jurnal dan
   laporan tetap dapat diverifikasi melalui endpoint/layer laporan yang berbeda.

Perbaikan klien yang dilakukan selama UAT: `PengadaanPajakScreen` memakai
`TickerProviderStateMixin` karena layar tersebut memiliki dua `TabController`. Tanpa ini,
menu Bayar Pajak berhenti saat dibuka; sesudah perbaikan, tes layar pajak lulus.

---

## 1. Ringkasan: apa yang sudah selesai

| Area | Status | Commit |
|---|---|---|
| Katalog laporan (11 laporan `lk_*`) lepas dari ZK, jadi natif | Selesai | r83859, r83874 |
| Layar Kelompok Aset natif (lepas dari `kelompok_asset.zul`) | Selesai | r83889-83900, git `6850455` |
| Urutan akun Master Aset vs Kelompok Aset (aset menang) | Selesai | r83962-83965 |
| Survei cakupan modul keuangan (169/181 aksi terpakai) | Selesai | r83946 |
| Hapus pembayaran hutang dari aplikasi + perbaikan idempotensi | Selesai | r83965, git `6b8b8cc` |
| UAT Akuntansi (render Windows + tangkap layar asli) | Selesai, dengan temuan terdokumentasi | pembaruan 4 September 2026 |
| UAT Keuangan → Akuntansi (37 tangkapan layar asli) | Selesai, dengan 2 blocker backend | `manual-keuangan-akuntansi/` |
| Manual PDF 10 halaman (ilustrasi tata letak) | Selesai, terkirim ke pengguna | r84008 |

Catatan historis tentang working copy bersih di bawah berasal dari sesi lama. Kelanjutan UAT
ini menambahkan skrip integration test, skrip seed, dokumen, dan satu perbaikan mixin klien;
periksa `git status` sebelum commit agar perubahan milik pengguna lain tidak ikut terbawa.

## 2. Catatan historis: keterbatasan sesi lama dan cara yang kini berhasil

Baca lengkap: `docs/pos/110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md`. Ringkasnya:
sesi lama **tidak punya jendela foreground sama sekali** (`GetForegroundWindow()` selalu `0`) —
tiga teknik berbeda (PostMessage, SendInput+AttachThreadInput, UI Automation) semuanya
diverifikasi gagal dengan diagnostik langsung, bukan diduga. Build web juga gagal kompilasi
(`core_hw` memakai `dart:ffi` yang tidak ada di web).

**Kalau sesi Anda berjalan di sesi desktop yang benar-benar interaktif** (misalnya karena
terhubung lewat VNC/RDP ke konsol fisik, bukan lewat shell headless), masalah ini kemungkinan
besar TIDAK akan terjadi — `GetForegroundWindow()` akan mengembalikan nilai wajar, dan klik
biasa (lewat `mcp__Claude_Browser__computer` bila aplikasinya web, atau lewat kendali mouse/
keyboard nyata bila Anda benar-benar mengoperasikan mesin ini) akan bekerja. **Periksa dulu**
apakah Anda punya kendali interaktif sungguhan sebelum mengulang otomasi PostMessage/SendInput
yang sudah terbukti gagal di atas. Cara yang berhasil pada kelanjutan ini adalah Flutter
`integration_test` dengan target Windows: widget aplikasi dirender dan dikendalikan langsung,
tanpa mengandalkan VNC, mouse OS, atau foreground window.

## 3. Cara membangun & masuk ke aplikasi

```
cd C:\opt\CodeBaseDesktopDanMobile\apps\ebisnis
flutter build windows --release -t lib/main.dart
```

**WAJIB baca dulu `docs/pos/109-uat-jebakan-build-dart-define-stale.md`** sebelum menjalankan
perintah di atas: mesin ini dipakai bersama sesi lain, dan `flutter build windows` bisa
mewarisi `--dart-define` BASI dari build varian lain (mis. Al-Bahjah) tanpa peringatan apa
pun — build "berhasil" tapi executable-nya salah varian. Periksa lebih dulu:

```
grep DART_DEFINES apps\ebisnis\windows\flutter\ephemeral\generated_config.cmake
```

Kosong = aman (varian eBisnis polos). Kalau berisi `EBISNIS_VARIANT=...`, jalankan
`flutter clean` dulu sebelum build ulang.

Setelah build sukses, jalankan `build\windows\x64\runner\Release\ebisnis.exe`. Aplikasi masuk
OTOMATIS memakai kredensial tersimpan ke server produksi `ebisnis.id` (konteks "ebisnis"),
akun "admin", toko "Kantin Demo" — tidak perlu login manual. Judul jendela harus "eBisnis
POS"; kalau menampilkan merek lain, itu tanda jebakan dart-define di atas belum diperbaiki.

## 4. Urutan layar yang perlu ditangkap

Sama dengan daftar isi `manual-posting-keuangan/Manual-Posting-Laporan-Keuangan-eBisnis.pdf`
(baca PDF itu dulu — isinya SUDAH lengkap dan benar, tinggal ilustrasinya yang perlu diganti
tangkapan layar asli). Urutan menu (sidebar kiri, grup AKUNTANSI kecuali disebut lain):

1. Kode Akun / Grup Akun / Jenis Transaksi / Bank
2. Jurnal Umum — daftar, tombol "Jurnal Baru" (formulir kepala + baris debet/kredit), aksi
   Posting/Batalkan posting/Hapus pada satu baris
3. Posting Kulakan — muat draf, baris siap vs belum siap, tombol Sesuaikan Akun, dialog
   konfirmasi, hasil posting
4. Posting Bayar Hutang, Posting Terima Piutang — tata letak sama dengan Posting Kulakan
5. Posting HPP, Posting Penjualan — pratinjau otomatis, tombol Posting, dialog konfirmasi
6. Saldo Awal (Neraca Awal), Jurnal Penyesuaian Berkala, Tutup Buku (Laba Ditahan)
7. Katalog Laporan > kategori "Laporan Keuangan Resmi — Komparatif", "Buku Besar Resmi",
   "Arus Kas & Analisa Keuangan" — 11 laporan, semuanya sudah natif (lihat doc 101, 102)

## 5. Cara mengganti ilustrasi jadi tangkapan layar asli TANPA menulis ulang teks

Generator manualnya ada di `docs/pos/manual-posting-keuangan/` (`susun.py` + `isi.py` +
`mockup.py`). Seluruh teks penjelasan, sitasi kode, dan urutan bagian **sudah benar** — yang
perlu diganti hanya pemanggilan `b.tabel(...)`/`b.formulir(...)` (ilustrasi) menjadi
`b.gambar_asli(path_png, judul, keterangan)` (tangkapan nyata). Method `gambar_asli` sudah ada
di `mockup.py` dan dipakai contohnya di Lampiran D (dua halaman yang SUDAH memakai tangkapan
layar nyata) — tiru pola itu untuk seluruh bagian lain.

Jangan buang `mockup.py`/`susun.py`/`isi.py` — edit `isi.py` di tempat, jalankan ulang
`python susun.py`, lalu commit PDF barunya menimpa yang lama.

## 6. Bahan sitasi kode yang sudah dikumpulkan (tidak perlu digali ulang)

Semua sitasi berkas:baris untuk tiap layar (label medan, nama kolom, pesan validasi, nama
aksi API) sudah ada di `docs/pos/manual-posting-keuangan/isi.py` — baca berkas itu sebagai
referensi cepat sebelum menggali ulang kode sumber Dart/Java.

## 7. Konteks server yang relevan (AIS/Java)

- Katalog laporan: `ais/action/master/koperasi/helper/LaporanKatalogData.java` dan
  `LaporanKantinUtil.java` — 11 laporan `lk_*` (doc 101, 102).
- Urutan akun posting: `ais/database/model/asset/MasterAsset.java` —
  `akunTransaksiEfektif()`/`akunPenyusutanEfektif()`/`akunBiayaPenyusutanEfektif()` (doc 108).
  **Perhatian**: doc 108 mencatat kemungkinan data lama sudah membawa salinan nilai kelompok
  akibat perilaku lama — kueri deteksinya ada di dokumen itu, belum dijalankan ke basis data
  produksi.
- Aksi API keuangan yang TIDAK terpakai klien (4 lubang nyata): `apotik_sesi_kas_*`,
  `hutang_bayar_hapus` (sudah diperbaiki sesi ini), `*_saldo_anggaran` (4 aksi),
  `si_expense_category_save` — rincian di doc 107.

## 8. Satu hal yang BELUM diverifikasi sama sekali

Perubahan urutan akun (Bagian 7, doc 108) hanya diverifikasi lewat KOMPILASI, belum pernah
dijalankan terhadap aplikasi atau basis data nyata. Bila sesi Anda punya akses interaktif
sungguhan, ini kandidat kuat untuk diuji PERTAMA KALI sebelum hal lain — buat satu Kelompok
Aset dan satu Master Aset dengan akun Pembelian yang BERBEDA, posting satu dokumen lewat
Posting Kulakan, dan periksa akun mana yang benar-benar dipakai jurnalnya (harus akun Master
Aset, bukan Kelompok Aset).
