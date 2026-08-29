# 51 — Fase A Terlaksana (inti): Mesin Harga Grosir

Implementasi inti Fase A dari [48](48-gap-analisis-uom-packaging-manufaktur.md)
§4 Fase 1 dan [49](49-produksi-eksekusi-stok-dan-rencana-rinci.md) §3 Fase A.
Catatan UAT lengkap: repo Flutter
`docs/pos/2026-08-29-fase-a-harga-grosir.md`.

## Keputusan yang dikunci (29-08-2026)

**Harga grosir menentukan HARGA SATUAN lebih dulu; AturanDiskon memotong
SESUDAHNYA.** "HARGA_AWAL" pada `AturanDiskon.dasarPerhitungan` kini berarti
harga sesudah grosir. Tercatat di JavaDoc `AturanHargaProduk` dan
`HargaGrosirApiHelper`.

## Bentuk

- Entitas `AturanHargaProduk` (`koperasi.aturan_harga_produk`): per produk,
  ambang `min_qty_dasar` dalam SATUAN DASAR, harga per satuan dasar, toko
  null = semua toko, jendela waktu, aktif. SATU bentuk melayani kedua metode
  PDF — harga kemasan = ambang sebesar isi kemasan. Hapus = nonaktif.
- Mesin `HargaGrosirApiHelper`: ambang TERBESAR yang ≤ qty menang; aturan
  ber-toko menang atas global; qty penentu = TOTAL produk itu se-keranjang
  (2×25 memenuhi ambang 50); baris `ekstra` bersarang ikut.
- Kait dua tempat, mesin SATU: `bayar` (payload dimutasi SEBELUM evaluasi
  diskon — seluruh hilir mewarisi) dan `diskon_evaluasi` (pratinjau; respons
  membawa peta `hargaGrosir`). Transaksi lokal-dulu yang baru tersinkron
  (`pengiriman_pending`) TIDAK ditimpa — harga saat kejadian berlaku.
- Rute API `harga_grosir_list/simpan/hapus`; gerbang = gerbang aturan diskon.
- Klien kasir: `ItemKeranjang.hargaGrosir` (hanya diisi server), subtotal dan
  seluruh payload memakai harga efektif; dua arah (qty turun → katalog).

## Bukti

Harness `TesHargaGrosir`: **13 lulus / 0 gagal** (batas inklusif, prioritas
toko>global, ambang bertingkat, kedaluwarsa/nonaktif/toko-lain tidak bocor,
gabungan qty lintas baris, ekstra bersarang, dua arah). Flutter:
`harga_grosir_kontrak_test` 4 lulus; suite penuh **459 lulus / 0 gagal**;
analyze bersih. `javac -source 1.7` EXIT=0; cermin `src`/`java` identik.

## Belum termasuk (kelanjutan Fase A)

Pemilih kemasan sekali-ketuk di kartu kasir + snapshot `kemasanNama/qty` di
baris struk (dok. 48 §4 Fase 1 butir klien); layar kelola aturan harga; paritas
tampilan _pos.jsp (otoritas harga finalnya SUDAH ikut karena bayar dipakai
semua kanal).
