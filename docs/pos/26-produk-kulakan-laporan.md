# 26 — Produk: stok per tanggal & ekspor · Kulakan: cetak faktur

Commit `7722f41` (fitur) dan `f4651c9` (salinan tersimpan).

## Stok pada tanggal tertentu

Layar **Produk** mendapat penyaring tanggal. Memilih tanggal menampilkan stok **pada
tanggal itu**, bukan stok sekarang.

Angkanya tidak dihitung ulang di klien. Klien memanggil `laporan_jalankan` dengan
`r: 'stok_per_tanggal'`, sehingga rumusnya tetap satu-satunya rumus stok yang dipakai
seluruh sistem — `StokKantinUtil.formulaStokSql`, tujuh suku. Menyalin rumus stok ke klien
berarti dua sumber kebenaran yang cepat atau lambat berbeda, dan selisih stok adalah jenis
selisih yang paling lama tidak ketahuan.

Pemilih tanggalnya dibatasi sampai **hari ini**: stok masa depan tidak bermakna karena
tidak ada mutasi setelah hari ini.

Berkas: `lib/screens/produk_stok_tanggal.dart` — `StokPerTanggal.ambil({tanggal, kataKunci})`
mengembalikan `HasilStokTanggal` (`stokPerKode`, `dariCache`, `disimpanPada`, `keLaporan()`).

## Ekspor laporan

Bilah yang sama menyediakan **Preview · Atur Model · PDF · Excel · Word**, memakai mesin
laporan yang sudah ada (`DynamicReportDesigner`, `DynamicReportData`,
`DynamicReportModel.fromData`) — sama dengan yang dipakai layar Pelanggan.

Yang diekspor adalah **baris yang sudah tersaring server** (kata kunci + tanggal), jadi isi
berkas persis sama dengan yang tampil di layar. Ini pokok permintaannya: ekspor yang
diam-diam mengambil data lebih luas daripada yang terlihat adalah laporan yang menyesatkan.

Nama berkas ikut menyebut tanggalnya (`stok-produk-20260821.pdf`), supaya dua ekspor pada
tanggal acuan berbeda tidak saling menimpa di folder unduhan.

## Salinan tersimpan (cache)

Hasil laporan disimpan lewat `CoreDb.instance.simpanCacheReferensi` /
`ambilCacheReferensi`, sehingga membuka ulang tanggal yang sama tidak menembak server lagi.

Saat data berasal dari salinan, layar menandainya dan menyebut **kapan salinan itu
diambil**. Angka rupiah dan stok yang tidak menyebutkan umurnya akan terbaca sebagai angka
terkini — itu lebih berbahaya daripada memuat lebih lambat.

## Kulakan → Detail: tombol Print

Sebelumnya faktur hanya bisa diunduh sebagai PDF/Word, lalu dicetak dari aplikasi lain.

Dokumen PDF-nya dipisah ke `_dokumenFakturPdf()` dan dipakai oleh **dua** tombol: Unduh dan
Print (`Printing.layoutPdf` dari paket `printing`). Satu sumber dokumen, bukan dua rutin
penyusun tata letak — kalau dipisah, cetakan dan unduhan akan berbeda begitu salah satunya
diubah, dan perbedaan itu baru ketahuan di tangan pengguna.

## Jebakan yang ditemui

- **`DropdownButtonFormField.initialValue`** tidak ada di versi Flutter ini; yang benar
  `value`.
- **Kelas kembar.** `konfigurasi_screen.dart` dan beberapa layar lain memuat lebih dari
  satu `State` dengan prolog metode yang mirip. Menyisipkan metode berdasarkan jangkar teks
  pernah mendaratkannya di kelas yang salah lebih dari sekali. Sisipkan dengan jangkar yang
  memuat nama unik kelasnya, dan periksa `svn diff`/`git diff` sebelum commit.
- **`SelectableText` memakai `EditableText` yang read-only.** Uji yang memastikan "harga
  bukan input" tidak bisa sekadar mencari ketiadaan `EditableText`; yang diperiksa adalah
  `readOnly == true`.
