# Serah Terima (Handover): UAT & Manual Bergambar Nyata — untuk Sesi Lain

Dokumen ini untuk sesi/agen LAIN yang akan melanjutkan pekerjaan ini dengan akses ke sesi
desktop yang benar-benar interaktif (mis. lewat vnc.ebisnis.id) — supaya tidak perlu menggali
ulang apa yang sudah ditemukan sesi ini. Tujuan akhirnya: manual pengguna yang sama isinya
dengan `manual-posting-keuangan/`, tetapi memakai TANGKAPAN LAYAR ASLI, bukan ilustrasi.

---

## 1. Ringkasan: apa yang sudah selesai

| Area | Status | Commit |
|---|---|---|
| Katalog laporan (11 laporan `lk_*`) lepas dari ZK, jadi natif | Selesai | r83859, r83874 |
| Layar Kelompok Aset natif (lepas dari `kelompok_asset.zul`) | Selesai | r83889-83900, git `6850455` |
| Urutan akun Master Aset vs Kelompok Aset (aset menang) | Selesai | r83962-83965 |
| Survei cakupan modul keuangan (169/181 aksi terpakai) | Selesai | r83946 |
| Hapus pembayaran hutang dari aplikasi + perbaikan idempotensi | Selesai | r83965, git `6b8b8cc` |
| UAT interaktif (klik + tangkap layar tiap langkah) | **GAGAL** — lihat Bagian 2 | doc 109, 110 |
| Manual PDF 10 halaman (ilustrasi tata letak) | Selesai, terkirim ke pengguna | r84008 |

Working copy SVN dan git dua-duanya **bersih** (sinkron dengan HEAD/origin) per commit
terakhir di atas — sesi ini tidak meninggalkan pekerjaan setengah jadi.

## 2. Kenapa sesi ini tidak bisa mengambil tangkapan layar interaktif

Baca lengkap: `docs/pos/110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md`. Ringkasnya:
sesi ini **tidak punya jendela foreground sama sekali** (`GetForegroundWindow()` selalu `0`) —
tiga teknik berbeda (PostMessage, SendInput+AttachThreadInput, UI Automation) semuanya
diverifikasi gagal dengan diagnostik langsung, bukan diduga. Build web juga gagal kompilasi
(`core_hw` memakai `dart:ffi` yang tidak ada di web).

**Kalau sesi Anda berjalan di sesi desktop yang benar-benar interaktif** (misalnya karena
terhubung lewat VNC/RDP ke konsol fisik, bukan lewat shell headless), masalah ini kemungkinan
besar TIDAK akan terjadi — `GetForegroundWindow()` akan mengembalikan nilai wajar, dan klik
biasa (lewat `mcp__Claude_Browser__computer` bila aplikasinya web, atau lewat kendali mouse/
keyboard nyata bila Anda benar-benar mengoperasikan mesin ini) akan bekerja. **Periksa dulu**
apakah Anda punya kendali interaktif sungguhan sebelum mengulang otomasi PostMessage/SendInput
yang sudah terbukti gagal di atas — kalau lingkungan Anda sama (headless), jangan mengulang
teknik yang sama; laporkan kembali ke pengguna.

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
