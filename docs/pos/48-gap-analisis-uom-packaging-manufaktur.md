# 48 — Analisis Kesenjangan: UoM, Packaging, dan Rantai Sales–Manufaktur

Analisis kesenjangan atas tiga dokumen referensi ERP yang diserahkan pemilik
sistem (29-08-2026):

| Dokumen | Isi pokok |
|---|---|
| `proses sales dan manufaktur.pdf` (8 hal) | Integrasi Sales–Purchase–Inventory–MRP: rute Buy/Manufacture/MTO, BOM, reordering min/max, scrap bahan baku, penyesuaian stok, QC produk jadi (rework/unbuild/scrap), jurnal otomatis |
| `UOM n Sales.pdf` (5 hal) | UoM sebagai jembatan: kategori, satuan dasar/beli/jual, konversi otomatis, backorder, dua cara jual grosir (Product Packaging + pricelist, atau Multi-UoM + pricelist) |
| `POS menggunakan fitur packaging.pdf` (1 hal) | Dua metode grosir di kasir: scan/pilih kemasan (qty dasar otomatis), atau ganti UoM per baris; keduanya bergantung pricelist |

**Metode.** Setiap klaim "ada/belum" di bawah diverifikasi terhadap kode
aktual — SVN r78498 dan pohon kerja git terkini — bukan dari ingatan atau
dokumen lama. Rujukan kelas/berkas disertakan supaya pembaca dapat memeriksa
ulang. Konsep di PDF memakai istilah ERP generik (bergaya Odoo); padanannya di
platform ini disebutkan eksplisit.

---

## 1. Ringkasan eksekutif

Fondasi platform **jauh lebih lengkap daripada yang tersirat dari permintaan**:
UoM ber-kategori/rasio, satuan pembelian per produk, kemasan ber-barcode yang
sudah dikenali kasir, produksi tujuh tipe dokumen (termasuk BOM ber-siklus
hidup dan waste), ambang stok per gudang dengan penjadwal otomatis, opname,
batch/kedaluwarsa, dan mutasi antar toko — semuanya sudah ada dan hidup.

Kesenjangan sebenarnya terkonsentrasi di TIGA tempat:

1. **Harga grosir tidak ada sama sekali** (kesenjangan komersial terbesar).
   Kemasan sudah memindahkan qty, tetapi harganya tetap harga ecer × qty.
   Kedua metode di PDF sama-sama bertumpu pada aturan harga ber-ambang
   kuantitas (pricelist) — dan mesin itu belum ada di sisi mana pun.
2. **UoM belum menyentuh sisi JUAL.** Pembelian sudah berkonversi
   (`satuanPembelian`), tetapi baris penjualan (POS maupun Sales Order
   lapangan) hanya mengenal satuan dasar.
3. **Otomasi rantai belum menyambung.** Ambang stok memicu *pengajuan*, bukan
   draf PO; tidak ada rute per produk (Buy/Manufacture), tidak ada MTO, dan
   kekurangan bahan baku di produksi tidak memicu apa pun. QC produk jadi
   (karantina → rework/unbuild/scrap) belum ada sebagai alur.

---

## 2. Peta kemampuan saat ini

Status: **ADA** (berfungsi), **SEBAGIAN** (fondasi ada, konsep PDF belum penuh),
**BELUM** (tidak ditemukan jejaknya di kode).

### 2.1 UoM (PDF "UOM n Sales")

| Konsep PDF | Status | Bukti di kode |
|---|---|---|
| Master UoM: kategori, satuan acuan, rasio, presisi | **ADA** | `SatuanProduk` (inventory); `uom_screen.dart`; `UomKonversi` (klien) — menolak konversi lintas kategori, faktor `SMALLER`/`REFERENCE` |
| Satuan dasar per produk (Inventory UoM) | **ADA** | `Produk.satuan` (`@JoinColumn satuan`) |
| Satuan pembelian per produk + konversi saat kulakan | **ADA** | `Produk.satuanPembelian`; `kulakan_screen.dart` menampilkan "1 {satuanBeli} = N {dasar}" dan membukukan stok dalam satuan dasar |
| Satuan JUAL per baris transaksi | **BELUM** | Baris kasir dan `SalesOrderLapanganItem` hanya `harga_satuan` + qty dasar; tidak ada kolom satuan di baris |
| Snapshot qty input + faktor + qty dasar pada mutasi | **ADA** (kontrak) | JavaDoc `UomKonversi`: "Mutasi stok server tetap wajib menyimpan snapshot qty input, faktor, dan qty dasar" |
| Pembulatan saran beli ke UoM pembelian (70 kg → 2 karung) | **BELUM** | Tidak ada mesin saran beli; lihat 2.4 |
| Backorder penjualan (kirim sebagian, sisa menyusul) | **BELUM** di jual | Back Order sudah ada di PENGADAAN (`poInduk`, dok. 40) — konsep dan polanya bisa dipakai ulang, tetapi sisi penjualan belum punya |

### 2.2 Packaging (PDF "POS menggunakan fitur packaging")

| Konsep PDF | Status | Bukti di kode |
|---|---|---|
| Preset kemasan per produk: nama, isi (qty dasar), barcode | **ADA** | `Produk.kemasan` JSON `[{"nama","barcode","qtyDasar","aktif"}]`; editor di `produk_screen.dart` (`_KemasanBaris`) |
| Scan barcode kemasan di kasir → qty dasar masuk keranjang | **ADA** | `kasir_screen.dart` ±1210: pencocokan barcode kemasan lintas produk, qty dasar ditambahkan |
| Stok tetap dibukukan dalam satuan dasar | **ADA** | Kontrak `Produk.getKemasan()`: "transaksi dan persediaan tetap dibukukan dalam satuan" |
| Tombol PILIH kemasan di layar kasir (tanpa scan) | **BELUM** | Jalur masuknya hanya barcode; kasir tanpa scanner tidak bisa memakai preset |
| Centang kanal per kemasan (Sales / POS / Purchase) | **BELUM** | JSON kemasan tidak punya bendera kanal; semua kemasan berlaku di mana kemasannya dikenali |
| Harga grosir otomatis saat kemasan/ambang terpenuhi | **BELUM** | Nol jejak `grosir`/`pricelist`/harga ber-min-qty di kedua repositori (satu-satunya cocokan: filter laporan) |
| Label kemasan tercetak di baris struk ("2 Dus 24") | **BELUM** | Baris keranjang tidak menyimpan kemasan yang dipakai — begitu qty dasar masuk, asal-usulnya hilang |

### 2.3 Produksi / MRP (PDF "proses sales dan manufaktur")

| Konsep PDF | Status | Bukti di kode |
|---|---|---|
| BOM dengan siklus hidup | **ADA** | `ProduksiDokumen` `documentType=BOM`; transisi DRAFT→ACTIVE→RETIRED dijaga (`ProduksiApiHelper`) |
| Order produksi + biaya (material/labor/overhead → unit cost) | **ADA** | `ProduksiDokumen` WO/OUTPUT/COST; kolom `materialCost..unitCost` |
| Pemakaian & retur bahan baku | **ADA** | tipe ISSUE / RETURN; `PemakaianBahanBaku` |
| Waste produksi (≈ scrap bahan baku saat produksi) | **ADA** | tipe WASTE; `ProductionWasteLine` |
| Genealogi lot barang jadi | **ADA** | `ProduksiGenealogiLot` |
| Resep sederhana kantin (HPP beku) | **ADA** | `Produk.bahanBaku` snapshot `{produk,nama,qty,harga}`; `ProduksiKantin` |
| RESERVASI komponen saat WO dibuat | **BELUM** | Tidak ada penanda reserved di stok |
| Kekurangan bahan → RfQ/pengajuan otomatis dari produksi | **BELUM** | `StokThresholdScheduler` berjalan dari AMBANG, bukan dari kebutuhan WO |
| Rute per produk (Buy / Manufacture / MTO) | **BELUM** | Tidak ada kolom rute di `Produk` |
| SO terkonfirmasi memicu MO/PO (MTO) | **BELUM** | — |
| Unbuild (bongkar produk jadi → komponen kembali) | **BELUM** | Tidak ada tipe dokumen UNBUILD |
| QC produk jadi: check → alert → karantina → rework/scrap | **BELUM** | Nol jejak quality/karantina di inventory |
| Jurnal otomatis per transaksi stok (valuation) | **SEBAGIAN** | `Produk.metodeHpp` + Posting HPP Penjualan; biaya produksi tercatat di dokumen; jurnal scrap/opname/produksi otomatis PERLU VERIFIKASI terhadap mesin draft-jurnal (dok. 06/09) |

### 2.4 Reordering & rantai pasok

| Konsep PDF | Status | Bukti di kode |
|---|---|---|
| Ambang minimum per gudang | **ADA** | `AmbangStokGudang` per produk×gudang (sengaja terpisah dari `Produk.stokMinimum` yang murni label peringatan) |
| Pemicu otomatis berkala + idempoten | **ADA** | `StokThresholdScheduler` (4 jam; tidak dobel selama pengajuan BARU/DIPROSES masih ada) |
| Arah 2 tingkat: cabang→induk, pusat→vendor | **ADA** | hierarki `Gudang.gudangInduk` |
| **Max quantity** (isi ulang sampai batas atas) | **BELUM** | `AmbangStokGudang` hanya ambang bawah; yang dibuat adalah pengajuan tanpa qty sasaran |
| Draf PO/MO otomatis (bukan sekadar pengajuan) | **BELUM** | Keluarannya `PengajuanPembelianGudang` — manusia yang meneruskan ke Kulakan/Pengadaan |
| Forecasted qty (on-hand − keluar + masuk) | **BELUM** | Stok yang dipakai stok fisik |

### 2.5 Yang sudah setara dengan PDF dan tidak perlu disentuh

Opname (`StokOpname`+`SesiStokOpname`), batch/kedaluwarsa
(`ProdukBatch`/`MutasiProdukBatch` + layar Kedaluwarsa), mutasi antar
toko/gudang (`MutasiStokToko`, `DistribusiDokumen`), retur tiga arah
(`ReturBarang`/`ReturPembelian`/`ReturPenjualan`), Back Order pengadaan
(dok. 40).

---

## 3. Prioritas implementasi

Urutan disusun dari **nilai bisnis ÷ ukuran pekerjaan**, dan dari
ketergantungan teknis (harga grosir dibutuhkan kedua metode PDF; UoM jual
menumpang pada mesin harga yang sama).

| P | Pekerjaan | Nilai | Ukuran |
|---|---|---|---|
| **P1** | Mesin harga ber-ambang (pricelist grosir) + lengkapi kemasan di kasir | Langsung dipakai kasir tiap hari; kedua PDF bertumpu di sini | Sedang |
| **P2** | Satuan JUAL per baris (Multi-UoM Metode 2) | Sales lapangan/grosir resmi bersatuan besar | Sedang |
| **P3** | Reordering lengkap: max qty + draf dokumen + rute Buy/Manufacture | Menutup separuh otomasi rantai | Sedang |
| **P4** | Produksi: reservasi komponen + kekurangan → pengajuan otomatis + Unbuild | Melengkapi MRP yang sudah 70% jadi | Sedang–besar |
| **P5** | MTO (SO → MO/PO) | Butuh P3; menyentuh alur SO | Besar |
| **P6** | QC produk jadi: karantina → rework/unbuild/scrap + jurnal | Alur baru utuh | Besar |

---

## 4. Langkah pengerjaan per fase

Seluruh fase tunduk pada aturan rumah di §5. Setiap fase berakhir dengan
harness regresi baru + dokumen di `docs/pos`.

### Fase 1 — Harga grosir + kemasan penuh di kasir (P1)

**Skema** (semua nullable, dibuat Hibernate saat boot — tanpa DDL tangan):

- Entitas baru `AturanHargaProduk` (skema `inventory`): `produk` (FK),
  `minQtyDasar` (ambang, dalam satuan dasar), `harga` (per satuan dasar),
  `berlakuMulai`/`berlakuSampai` (nullable), `toko` (nullable = semua toko),
  `aktif`. SATU bentuk aturan ini melayani KEDUA metode PDF: "harga kemasan"
  dinyatakan sebagai ambang `minQtyDasar = qtyDasar kemasan`.
- JSON `Produk.kemasan` ditambah kunci opsional `pos`/`beli` (bendera kanal)
  — kunci lama tetap dibaca apa adanya, klien lama tidak berubah perilaku.

**Server** (satu mesin, dipakai semua kanal):

- `HargaEngine.hitungHargaSatuan(produk, qtyDasar, toko, tanggal)` — ambang
  terbesar yang ≤ qty menang; tanpa aturan → `hargaJual` biasa. Dipanggil dari
  jalur checkout POS **di sisi server** (pola `selisihTotal`/`hasil_server_json`
  yang sudah ada di struk: server yang berwenang, klien menampilkan koreksi).
- Aksi API `harga_aturan_list/simpan/hapus` lewat amplop `success` (pelajaran
  dok. 41: JANGAN lewati penyelarasan amplop).

**Klien POS:**

- Tombol/lembar **pilih kemasan** pada produk ber-preset (satu ketukan, tanpa
  scanner) — melengkapi jalur scan yang sudah ada.
- Baris keranjang menyimpan `kemasanNama`+`kemasanQty` yang dipakai (snapshot,
  bukan rujukan) supaya struk bisa mencetak "2 Dus 24 (48 pcs)" dan supaya
  perubahan preset di kemudian hari tidak mengubah arsip struk.
- Harga baris dihitung ulang saat qty berubah (naik-turun melewati ambang harus
  memperbarui harga — dua arah, diuji dua arah).

**JSP/ZKoss:** `_pos.jsp` memanggil mesin harga yang sama (bukan salinan SQL) —
paritas kanal adalah aturan rumah (dok. 41 §cacat ZKoss).

**Uji:** ambang tepat di batas (qty = minQty), dua ambang bertumpuk, kemasan +
manual campur, toko spesifik vs semua toko, dan HARGA TURUN saat qty turun.

### Fase 2 — Satuan jual per baris (P2)

- Kolom baru di baris transaksi (POS + `SalesOrderLapanganItem`):
  `satuanJual` (FK, nullable = satuan dasar), `qtyInput`, `faktorKeDasar`
  (snapshot — kontrak `UomKonversi` sudah menuntut ini; yang kurang hanya
  tempat menyimpannya di baris jual).
- Stok, HPP, dan laporan TETAP membaca qty dasar — tidak ada satu pun rumus
  lama yang berubah; kolom baru murni tambahan tampilan + audit.
- Pemilih satuan di baris kasir hanya menampilkan satuan sekategori
  (`UomKonversi.konversi` sudah menolak lintas kategori — pakai itu, jangan
  saring ulang di UI).
- Harga per satuan besar = `HargaEngine` Fase 1 dengan `qtyDasar` hasil
  konversi — TIDAK ada mesin harga kedua.

### Fase 3 — Reordering lengkap (P3)

- `AmbangStokGudang` + `maxQty` (nullable; kosong = perilaku lama, hanya
  lapor). Qty saran = `maxQty − stokSaatIni`, dibulatkan NAIK ke
  `satuanPembelian` (contoh literal PDF: butuh 70 kg, karung 50 → 2 karung).
- `Produk` + kolom `rute` (nullable: `BELI` / `PRODUKSI`; kosong = BELI,
  perilaku hari ini). `StokThresholdScheduler` membaca rute: BELI →
  `DraftPembelian` otomatis (draf, bukan PO jadi — manusia tetap konfirmasi);
  PRODUKSI → `ProduksiDokumen` WO status DRAFT.
- Idempotensi yang sudah ada dipertahankan dan diperluas ke draf.

### Fase 4 — Produksi: reservasi, kekurangan, unbuild (P4)

- Reservasi: kolom `qtyReserved` pada stok per gudang; WO ACTIVE mengunci
  komponen; ISSUE mengurangi reserved+fisik; batal WO melepas reserved.
- Kekurangan komponen saat aktivasi WO → tulis `PengajuanPembelianGudang`
  (mesin yang sudah ada) dengan rujukan WO — bukan mesin baru.
- Tipe dokumen baru `UNBUILD` di `ProduksiDokumen` (kebalikan OUTPUT: barang
  jadi −N, komponen sesuai BOM +N, biaya kembali ke persediaan komponen) —
  menumpang penuh pada infrastruktur dokumen/event/hak akses produksi yang ada.

### Fase 5 — MTO (P5) dan Fase 6 — QC (P6)

Digarap setelah P1–P4 hidup; keduanya butuh keputusan bisnis (§6) sebelum
disain rinci. Garis besarnya: MTO = kolom rute `MTO_BELI`/`MTO_PRODUKSI` +
pemicu pada konfirmasi `SalesOrderLapangan`; QC = status karantina pada
`ProdukBatch`/lokasi + dokumen Quality Alert + disposisi
(rework=WO baru, unbuild=Fase 4, scrap=WASTE yang ada) + jurnal per disposisi
lewat mesin draft-jurnal (pola dok. 06/09; ingat pelajaran
`currentNativeSession` untuk posting dari API).

---

## 5. Aturan rumah yang mengikat semua fase

1. **Skema**: kolom/tabel baru nullable, dibuat Hibernate saat boot. Tanpa DDL
   tulisan tangan. (Aturan tetap pemilik sistem.)
2. **Satu mesin**: harga, konversi, dan posting masing-masing SATU salinan,
   dipanggil POS + JSP + ZKoss. Salinan kedua = penyimpangan diam-diam
   (pelajaran `FileFotoLain.ambilIsiBlob`, dok. 40).
3. **Server berwenang atas uang**: klien menampilkan, server menghitung ulang
   (pola `selisihTotal` di struk). Harga grosir TIDAK boleh dihitung final di
   klien.
4. **Amplop**: aksi baru melewati penyelarasan status (`00`→`success`; daftar
   kosong ≠ galat) — dok. 41.
5. **Lokal-dulu** (dok. 42): master (aturan harga, kemasan) boleh lokal-dulu;
   dokumen PRODUKSI dan posting nilai TETAP DARING (butuh id server + uang —
   dua golongan online-only spesifikasi 13.3).
6. **Java 1.7** di server; akhir baris PER BERKAS disamakan dengan HEAD-nya
   (dok. 44); working copy dipakai bersama — commit per berkas, diff dibaca
   baris demi baris sebelum stage.

---

## 6. Keputusan yang dibutuhkan dari pemilik sistem

1. **Bentuk harga grosir**: per-ambang qty (PDF Cara 1) sudah mencakup
   per-kemasan; apakah perlu JUGA harga tetap per satuan besar (PDF Cara 2,
   mis. "Rp 4.500.000/karung" yang tidak persis = 50 × harga/kg)? Fase 1
   dirancang untuk ambang; harga-tetap-per-UoM menambah satu kolom bila
   diminta.
2. **Kelipatan wajib**: apakah pembeli grosir WAJIB kelipatan kemasan (PDF
   menyebut mencegah "53 kg nanggung"), atau bebas? Menentukan validasi baris.
3. **Cakupan MTO**: dari `SalesOrderLapangan` saja, atau juga dari Pesanan
   POS? Pesanan POS hari ini adalah keranjang tertahan, bukan SO — memaksakan
   MTO di sana mengubah maknanya.
4. **Reservasi**: apakah stok reserved mengurangi stok yang boleh dijual kasir
   (bisa menolak penjualan), atau hanya informasi? Berdampak langsung ke UX
   kasir.
5. **Jurnal produksi/opname/waste otomatis**: diposting langsung atau lewat
   dasbor Draft Jurnal dulu (pola modul Keuangan, dok. 06/09)? Konsisten
   dengan yang sudah berjalan = lewat dasbor.

## 7. Verifikasi tertunda

- Kelengkapan jurnal otomatis stok saat ini (scrap/opname/produksi) terhadap
  mesin draft-jurnal — perlu dibaca dari `ProduksiApiHelper` COST + modul
  akunting sebelum Fase 4/6 didesain final.
- ~~Aturan Diskon~~ SUDAH diverifikasi: aturannya persen/nominal
  (`tab_aturan_diskon.dart`), TANPA ambang kuantitas — tidak bertabrakan dengan
  mesin harga grosir. Yang tetap harus diputuskan di Fase 1: URUTAN penerapan
  di checkout (harga grosir dulu baru diskon, atau sebaliknya) — dua-duanya
  menyentuh harga akhir dan urutannya mengubah hasil.
