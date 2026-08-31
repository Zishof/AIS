# Laporan Aktivitas (Surplus/Defisit) & Pemilih Unit pada Laporan Keuangan

Tanggal: 31 Agustus 2026. Kode server masuk SVN **r78643**; perubahan klien POS Flutter ada di
repo terpisah `CodeBaseDesktopDanMobile` (belum di-commit — lihat §5). Menutup butir 2 dan 4
pada dok [66](66-laporan-keuangan-standar-yayasan.md) §5.

## 1. `akn_laporan_aktivitas` — Laporan Aktivitas (Perhitungan Surplus/Defisit)

Susunan nirlaba/yayasan, dihitung dari jurnal TERPOSTING + klasifikasi Kelompok Laporan jenis
"Laba Rugi" (sumber angka persis sama dengan `akn_laba_rugi`):

```
A. PENDAPATAN                    per Kelompok Laporan + subtotal
   JUMLAH PENDAPATAN
B. BIAYA
   1. HARGA POKOK PENJUALAN (HPP)   akun ber-kelompok bertanda HPP / harga pokok
      Jumlah Harga Pokok Penjualan
   2. LABA (RUGI) KOTOR             = pendapatan - HPP
      Contribution Margin (%)
   3. BIAYA TETAP                   beban selain HPP, per Kelompok Laporan + subtotal
      Jumlah Biaya Tetap
   4. SURPLUS (DEFISIT) - LABA (RUGI) USAHA
      Profit Margin (%)
```

Pemisah HPP vs biaya tetap memakai tag klasifikasi yang sudah dipakai `akn_laba_rugi`
(`TAG_KLAS` memuat "hpp" atau "harga pokok"), jadi tidak ada aturan baru yang harus dipelajari
admin. Margin dikirim sebagai persen; bila pendapatan nol, sel margin dikosongkan (bukan nol,
bukan bagi-nol).

**Yang sengaja TIDAK ditiru**: rincian HPP bergaya persediaan (saldo awal barang + pembelian −
persediaan akhir) pada lembar Excel yayasan. Di buku besar HPP adalah satu akun beban; angka
persediaannya ada di modul stok, dan versi berbasis stok tersedia terpisah pada kategori
"Margin, Laba & Analisa". Mencampur keduanya dalam satu laporan justru membuat angka tidak bisa
direkonsiliasi ke jurnal.

## 2. Pemilih Unit / Satuan Kerja

Sebelumnya SELURUH laporan berbasis jurnal terkunci pada konfigurasi `satuan_kerja_kantin`:
satu instalasi hanya bisa menampilkan satu unit, padahal satu yayasan menjalankan sekolah,
mart, katering, dan laundry yang masing-masing menuntut paket laporannya sendiri plus
konsolidasi. Parameter `lintasSatker` sudah ada di mesin tetapi tidak ada satu klien pun yang
mengirimnya — praktis mati.

Sekarang:

| Lapisan | Perubahan |
|---|---|
| Mesin | `LaporanKantinUtil.kantinSatkerId()` menghormati `ThreadLocal SATKER_PILIHAN` yang diisi dari parameter permintaan `satkerId`. Karena SELURUH cabang laporan jurnal melewati metode itu (langsung atau lewat `klausaLedger*`), satu perubahan menutup semuanya. |
| Arti nilai | `satkerId > 0` → unit itu saja; `satkerId <= 0` → **semua unit (konsolidasi)**; parameter tidak dikirim → ikut konfigurasi seperti dahulu, jadi pemanggil lama tidak berubah perilaku. |
| Katalog | Kategori berbasis jurnal (Keuangan, Buku Besar, Kas & Bank, Pajak PPN, Anggaran RAB, Rekonsiliasi Bank) memberi flag `satker:true` pada itemnya. Item ber-`url` (JRXML/ZK) tidak diberi flag karena punya formnya sendiri. |
| Respons katalog | `PosApi` menyertakan `satuanKerja` (daftar unit) dan `satuanKerjaDefault` (unit bawaan dari konfigurasi) — sekali jalan bersama katalog, tanpa panggilan tambahan. |
| Daftar unit | Hanya `rab.satuan_kerja` dengan `default_item = true` (± 23 baris terpakai). Tabel itu juga memuat ± 37.000 baris referensi K/L pemerintah hasil impor RKAKL yang tidak boleh muncul di pemilih. Baris pertama selalu "Semua Unit (Konsolidasi)" berid 0. |
| Klien POS | `laporan_screen.dart` menangkap kedua field itu dan meneruskannya; `laporan_detail_screen.dart` menampilkan dropdown "Unit / Satuan Kerja" bila item ber-flag `satker`, mengirim `satkerId`, dan **memasukkannya ke kunci cache** agar hasil unit A tidak menimpa unit B. |

Penanda per-thread dibersihkan di `finally` bersama `LINTAS_SATKER` — wadah menggunakan ulang
thread, penanda yang tertinggal akan membuat permintaan berikutnya ikut tersaring diam-diam.
Ini diuji tersendiri.

## 3. Pengujian

`TesLaporanYayasan` diperluas dari 20 menjadi **32 pemeriksaan, GAGAL 0**. Tambahannya:

| Skenario | Hasil |
|---|---|
| Laporan Aktivitas: jumlah pendapatan | 3jt — hanya periode berjalan, saldo awal November tidak ikut |
| Laporan Aktivitas: pemisahan biaya | HPP 2jt terpisah dari biaya tetap 1,7jt (beban kas + memorial) |
| Laporan Aktivitas: laba kotor & margin | 1jt dan Contribution Margin 33,33% |
| Laporan Aktivitas: kasus rugi | Defisit −700rb, Profit Margin −23,33% |
| `satkerId` = Unit Mart | hanya jurnal unit itu (kas masuk 1jt) |
| `satkerId` = Unit Laundry | hanya jurnal unit itu (700rb) |
| `satkerId` = 0 | konsolidasi 3jt + 1jt + 700rb = 4,7jt |
| tanpa `satkerId` | perilaku lama tidak berubah |
| kebersihan ThreadLocal | permintaan berikutnya tanpa `satkerId` tidak tertular pilihan sebelumnya |
| daftar unit | memuat "Semua Unit" dan unit terpakai, TIDAK memuat baris impor RKAKL |
| flag katalog | item berbasis jurnal ber-`satker`, item ber-`url` tidak |

Catatan harness: classpath Tomcat yang benar adalah
`C:\opt\tomcat7\apache-tomcat-7.0.109\lib\*` (bukan `C:\opt\tomcat7\lib`). Salah menaruhnya
membuat Hibernate Validator gagal `HV000183 javax.el.ExpressionFactory` berulang-ulang.

## 4. Bagi admin

- Laporan Aktivitas memakai pemetaan Kelompok Laporan jenis "Laba Rugi" yang sudah ada. Agar
  bagian HPP terisi, kelompok akun HPP harus bernama mengandung "HPP" atau "Harga Pokok".
- Pemilih unit hanya menampilkan Satuan Kerja bertanda dipakai (`default_item`). Unit baru yang
  belum ditandai tidak akan muncul.
- Jurnal LAMA yang `satuan_kerja`-nya kosong tidak akan muncul saat sebuah unit dipilih; hanya
  terlihat pada "Semua Unit (Konsolidasi)". Ini menampakkan data yang memang belum berunit —
  perbaikannya di sisi data, bukan laporan.

## 5. Sisi operasional

Server: bangun ulang WAR + mulai ulang Tomcat (tidak ada kolom/tabel baru).
Klien: perubahan Flutter ada di working copy `CodeBaseDesktopDanMobile` dan **belum di-commit**
(repo git, di luar konvensi commit SVN sesi ini) — dua berkas, 68 baris tambah, 4 ubah, lolos
`dart analyze`. Selama klien belum diperbarui, server tetap berjalan seperti biasa: tanpa
`satkerId` perilakunya sama dengan sebelumnya.
