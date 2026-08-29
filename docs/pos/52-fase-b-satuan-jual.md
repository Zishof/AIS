# 52. Fase B — Satuan Jual per Baris Transaksi

Tanggal: 29 Agustus 2026  
Status: server **lulus** (TesSatuanJual 9/9 di DB UAT), kasir Flutter **lulus**
(suite penuh 493/493, analyze bersih di berkas tersentuh); belum di-commit  
Rujukan: dok. 48 §4 Fase 2, dok. 49 Fase B, dok. 51 (urutan grosir→diskon)

## Masalah

Kasir menjual "2 Karung" tetapi seluruh alur (stok, ambang grosir, diskon,
laporan) berpikir dalam satuan dasar (kg). Sebelum fase ini klien harus
mengonversi sendiri — dan angka hasil konversi klien dipercaya server begitu
saja. Itu melanggar prinsip arsitektur: **server berwenang atas uang dan stok**.

## Keputusan

1. **Tiga kolom snapshot nullable** pada baris penjualan — `satuan_jual`,
   `qty_input`, `faktor_ke_dasar` — di dua entitas:
   `inventory.Pembelian` (baris POS kantin) dan
   `koperasi.SalesOrderLapanganItem` (baris SO lapangan).
   Kolom `qty`/`jumlah` TETAP satuan dasar; snapshot hanya untuk tampilan,
   struk, dan audit ("dulu dijual sebagai 2 Karung50"). Skema lewat entitas
   Hibernate (aditif, tanpa DDL tangan); tidak perlu mapping cfg baru.
2. **Server menurunkan jumlah dasar sendiri.**
   `KantinHelper.terapkanSatuanJual(session, transaksi, hasil)` berjalan di
   `bayar` SEBELUM harga grosir dan diskon: untuk baris dengan
   `satuan_jual_id`, server memuat SatuanProduk, menghitung
   `faktor = faktorUomInputKeDasar(produk, satuan)` (satu titik penegakan
   kategori UOM, kini package-visible dan dipakai juga oleh
   `SalesInventoryReceivableHelper`), lalu **menimpa** `jumlah` kiriman klien
   dengan `qty_input × faktor`. Lintas kategori / qty nol / satuan tak dikenal
   → status 91 dengan pesan terbaca.
3. **Urutan dikunci** (melanjutkan dok. 51): satuan jual menurunkan qty dasar
   DULU → ambang grosir menilai qty dasar yang benar → diskon memotong
   terakhir. `pengiriman_pending` tetap tidak ditimpa harga.
4. **Klien hanya pratinjau.** Kasir Flutter memakai `UomKonversi` untuk
   menghitung pratinjau dan MENGIRIM `satuan_jual_id` + `qty_input`; angka
   `jumlah` kirimannya boleh salah — server menimpanya (dibuktikan harness:
   klien mengirim 999, tersimpan 100).
5. **Snapshot swa-batal di klien.** Bila kasir mengubah qty dasar lewat
   stepper hingga `qty_input × faktor ≠ jumlah`, label dan payload satuan
   gugur sendiri — tidak ada klaim "2 Karung" yang berbohong (pola sama
   dengan label kemasan Fase A).

## Berkas

- `ais/database/model/inventory/Pembelian.java` — 3 kolom snapshot.
- `ais/database/model/koperasi/SalesOrderLapanganItem.java` — 3 kolom
  snapshot (BigDecimal, presisi 19).
- `ais/action/servlet/api/KantinHelper.java` — `terapkanSatuanJual` baru +
  kait di `bayar` sebelum grosir/diskon; `faktorUomInputKeDasar` jadi
  package-visible.
- `ais/database/model/koperasi/PembelianAnggotaKoperasi.java` — `simpanRinci`
  menyalin snapshot dari payload ke baris tersimpan. (Berkas ini semula
  campuran 960 CRLF / 1 LF; dinormalkan ke CRLF mayoritas, disengaja.)
- `ais/action/servlet/api/SalesInventoryReceivableHelper.java` — baris SO:
  turunkan jumlah lewat titik penegakan yang sama, simpan snapshot.
- Flutter `lib/models.dart` — `ItemKeranjang.satuanJualId/satuanJualNama/
  qtyInput/faktorKeDasar`, `satuanJualKonsisten`, `labelSatuanJual`.
- Flutter `lib/screens/keranjang_screen.dart` — tombol satuan per baris
  (produk ber-satuan dasar saja) → dialog pilih satuan sekategori dari
  `uom_list` + qty (hasil dasar wajib bulat, `jumlah` klien bertipe int);
  label baris & struk; payload `bayar` membawa `satuan_jual_id`/`qty_input`
  hanya bila snapshot masih sejalan.
- Flutter `test/satuan_jual_kontrak_test.dart` — 6 uji kontrak.

## Bukti

`TesSatuanJual` (harness dok. 44, DB UAT, refleksi ke fungsi persis yang
dipanggil `bayar`): 9/9 —

- 2 × Karung50 → `jumlah` ditimpa jadi 100 (kiriman klien 999 dibuang),
  `faktor_ke_dasar` = 50 ikut di baris;
- baris tanpa satuan tidak disentuh;
- lintas kategori (Liter → produk kg) ditolak, pesan menyebut kategori;
- `qty_input` ≤ 0 ditolak; satuan tak dikenal ditolak.

`javac -source 1.7 -target 1.7` EXIT=0 (cek terisolasi + build `uat-77608`);
mirror `src`/`java` md5 identik; 0 `.class` di pohon sumber.
Flutter: suite penuh 493/493; analyze tanpa temuan di berkas tersentuh.

## Di luar fase ini

- Kelipatan wajib kemasan dan harga tetap per-kemasan (Metode 2) — keputusan
  terbuka dok. 48 §6, menunggu pemilik.
- Pratinjau satuan jual di kanal `_pos.jsp` (otoritas final sudah ikut lewat
  `bayar`).
