# 49 — Gap Analisis Lanjutan: Eksekusi Stok Produksi & Rencana Pengerjaan Rinci

Pelengkap [48-gap-analisis-uom-packaging-manufaktur.md](48-gap-analisis-uom-packaging-manufaktur.md)
atas tiga PDF referensi yang sama (29-08-2026). Dokumen 48 tetap menjadi peta
utama; dokumen ini menambah tiga hal yang belum tercakup di sana:

1. **Dua koreksi** atas peta kemampuan §2.3 dokumen 48, dengan bukti kode.
2. Temuan eksekusi paling penting: **dokumen produksi tidak menggerakkan stok
   sama sekali** — dan itu mengubah urutan fase yang aman.
3. Langkah pengerjaan **di tingkat berkas** untuk tiap fase, termasuk fase
   prasyarat baru yang harus mendahului P4/P6.

**Metode.** Setiap klaim diverifikasi terhadap SVN r78501 dan git `3533591`
(keduanya baru di-update sebelum analisis, karena banyak sesi menulis ke pohon
yang sama). Klaim dokumen 48 TIDAK dipercaya begitu saja — semuanya diperiksa
ulang; yang benar disebut benar, dua yang meleset dikoreksi di §1.

---

## 1. Koreksi atas peta dokumen 48

### 1.1 "Pemakaian & retur bahan baku — ADA (tipe ISSUE/RETURN)" → menyesatkan

Dokumen 48 §2.3 menandai ISSUE/RETURN/WASTE sebagai **ADA** dengan bukti
`PemakaianBahanBaku`. Itu mencampur dua mekanisme yang TIDAK berhubungan:

- `koperasi.pemakaian_bahan_baku` ditulis oleh **`BahanBakuUtil.konsumsiBahanBaku`**
  pada saat PENJUALAN produk ber-resep di kasir (idempoten per bill, fail-safe).
  Satu-satunya penulisnya: `ais/action/master/inventory/BahanBakuUtil.java:129`.
- Dokumen produksi (`ProduksiDokumen` ISSUE/RETURN/OUTPUT/WASTE) **tidak pernah
  menulis ke tabel itu — atau ke ledger stok mana pun**. Diverifikasi dengan
  menyapu seluruh berkas yang menyentuh `ProduksiDokumen`: tidak satu pun
  memuat `pengadaan_produk`, `pemakaian_bahan_baku`, `stok_opname`, maupun
  pemanggilan hitung-ulang stok.

Konsekuensinya lihat §2 — ini temuan inti dokumen ini.

### 1.2 "QC/karantina — BELUM, nol jejak" → sebagian sudah ada

`ProdukBatch` sudah punya tiga status: `AKTIF`, **`KARANTINA`**, `DIMUSNAHKAN`
(`ais/database/model/inventory/ProdukBatch.java:36-38`), dan perubahan stok
fisik batch dicatat sebagai mutasi `OPNAME_BATCH`
(`KantinHelper.java:±14355`, `catatMutasiBatch`). Artinya fondasi karantina
QC di tingkat BATCH sudah berdiri; yang belum ada adalah ALURNYA (quality
check → alert → disposisi rework/unbuild/scrap) dan keterikatannya ke dokumen
produksi. Fase QC (P6 dok. 48) tidak membangun dari nol — ia menumpang status
batch yang sudah ada.

Klaim-klaim dokumen 48 yang lain **terverifikasi benar**, antara lain:
`StokThresholdScheduler` + `AmbangStokGudang` + `PengajuanPembelianGudangAction`
ada; `SalesOrderLapangan(+Item)` ada; `UomKonversi` klien + `uom_screen.dart`
ada; harga grosir/pricelist nol jejak; `AturanDiskon` tanpa ambang kuantitas
(seluruh medannya dibaca ulang — persen/nominal/prioritas/gabung/eksklusif,
tidak ada `minQty`).

---

## 2. Temuan inti: dokumen produksi tidak menggerakkan stok

### 2.1 Buktinya

Rumus stok yang berwenang (`ais/action/master/inventory/StokKantinUtil.java:18-20`):

```
stok = Σpengadaan.qty + Σstok_opname.selisih − Σpembelian.qty
       − Σpemakaian_bahan_baku.qty + Σretur_penjualan.qty(kembali_ke_stok)
       + Σmutasi_stok_toko.qty(tujuan) − Σmutasi_stok_toko.qty(asal)
```

**Tidak ada satu pun suku produksi.** `ProduksiDokumenBaris` punya bendera
`stockAffecting` (`stock_affecting`), tetapi TIDAK ADA eksekutornya — bendera
itu disimpan dan dikembalikan ke klien, lalu tidak dibaca siapa pun saat
dokumen di-POSTED (`ProduksiApiHelper.java`: transisi status hanya mengisi
`actualAt` dan `hitungBiaya`).

### 2.2 Artinya bagi pengguna

Modul produksi hari ini adalah **pencatat dokumen dan biaya**, bukan penggerak
barang: OUTPUT yang di-POSTED tidak menambah stok barang jadi, ISSUE tidak
mengurangi stok komponen, WASTE tidak mengurangi apa pun. Selama dipakai
sekadar arsip + kalkulasi HPP, itu tidak salah — tetapi SELURUH janji PDF
"proses sales dan manufaktur" (stok berkurang/bertambah otomatis, scrap
memotong stok, unbuild mengembalikan komponen) menuntut eksekusi stok.

### 2.3 Artinya bagi urutan fase dokumen 48

Fase 4 dok. 48 (reservasi komponen, kekurangan→pengajuan, UNBUILD) dan Fase 6
(QC dengan disposisi scrap) **dibangun di atas asumsi dokumen produksi
menggerakkan stok**. Reservasi tanpa ISSUE yang benar-benar memotong stok
adalah reservasi atas angka yang tidak pernah berubah; UNBUILD sebagai
"kebalikan OUTPUT" tidak bermakna bila OUTPUT sendiri tidak menambah stok.

Karena itu rencana di §3 menyisipkan **Fase 0 — Produksi Menggerakkan Stok**
sebagai prasyarat P4/P6. P1 (harga grosir) dan P2 (satuan jual) TIDAK
bergantung padanya dan boleh berjalan paralel.

---

## 3. Langkah pengerjaan rinci per fase

Semua fase tunduk pada aturan rumah dok. 48 §5 (skema nullable via Hibernate,
satu mesin lintas kanal, server berwenang atas uang, amplop `success`,
Java 1.7, akhir baris per berkas, commit per berkas dengan diff dibaca).

### Fase 0 — Produksi Menggerakkan Stok  *(baru; prasyarat P4 & P6)*

**Sasaran:** dokumen ISSUE/RETURN/OUTPUT/WASTE yang mencapai status final
benar-benar menggerakkan stok; REVERSED mengembalikannya; dua kali proses
tidak menggandakan.

1. **Ledger baru, satu tabel**: entitas `MutasiStokProduksi`
   (`koperasi.mutasi_stok_produksi`): `produk` FK, `toko`, `qtyMasuk`,
   `qtyKeluar`, `dokumen` FK ke `ProduksiDokumen`, `jenis`
   (ISSUE/RETURN/OUTPUT/WASTE), `waktu`, `oleh`. Nullable semua, didaftarkan di
   `hibernate.cfg.xml`, dibuat `hbm2ddl` saat boot — tanpa DDL tangan.
   *Mengapa tabel sendiri, bukan menumpang `pemakaian_bahan_baku`*: tabel itu
   punya makna tunggal (konsumsi resep saat jual) dan dipakai laporan HPP
   (`KantinHelper.java:11439,11513`); mencampurkan produksi akan mencemari
   laporan itu — persis alasan yang dulu memisahkan `pemakaian` dari
   `pembelian` (JavaDoc `BahanBakuUtil`).
2. **Rumus stok** ditambah dua suku:
   `+ Σmutasi_stok_produksi.qty_masuk − Σmutasi_stok_produksi.qty_keluar`.
   Sisir SEMUA titik yang menyalin rumus itu, bukan hanya
   `StokKantinUtil.hitungUlang`: cari `pemakaian_bahan_baku` di seluruh
   `--include=*.java` — setiap kueri yang menjumlahkannya adalah salinan rumus
   yang wajib ikut (ditemukan minimal di `KantinHelper.java:11439,11513,12474`
   dan `DashboardKantinAction`). Salinan yang terlewat = stok dua versi.
3. **Eksekusi pada transisi status** di `ProduksiApiHelper`:
   - POSTED/COMPLETED → hapus-lalu-tulis baris mutasi milik dokumen itu
     (pola idempoten `BahanBakuUtil`), HANYA untuk baris ber-`stockAffecting`.
     Arah per jenis: ISSUE = keluar (komponen), RETURN = masuk (komponen),
     OUTPUT = masuk (barang jadi), WASTE = keluar.
   - REVERSED → hapus baris mutasi milik dokumen (bukan menulis kontra-baris;
     ledger-nya per-dokumen sehingga hapus = pulih, dan jejak dokumennya
     sendiri tetap ada di `ProduksiDokumenEvent`).
   - **Transaksional dan TIDAK fail-safe**: berbeda dari konsumsi resep saat
     jual (yang sengaja tidak boleh menggagalkan checkout), kegagalan menulis
     mutasi produksi HARUS menggagalkan transisi status — dokumen yang
     mengaku POSTED tetapi stoknya tidak bergerak adalah kebohongan data.
     Pakai `currentNativeSession()` + transaksi eksplisit (pelajaran
     `batalkanPostingSemua` yang diam-diam tidak ter-commit, dok. riwayat
     Keuangan).
4. **HPP**: saat OUTPUT POSTED, `unitCost` dokumen tersedia. JANGAN langsung
   menimpa `Produk.hargaBeli` — itu keputusan pemilik (§4 no. 3).
5. **Uji** (harness `TesProduksiStok`, pola dok. 44: satu JVM, sapu awalan
   khas di `siapkan()`, `bersihkan()` tidak melempar):
   POSTED menambah/mengurangi sesuai jenis; REVERSED memulihkan; POSTED dua
   kali tidak menggandakan; baris tanpa `stockAffecting` tidak menggerakkan;
   kegagalan tulis membatalkan transisi.

### Fase A — Harga grosir + kemasan penuh di kasir  *(= P1 dok. 48)*

Langkah server/klien sudah dirinci dok. 48 §4 Fase 1. Tambahan jangkar dan
keputusan yang belum tertulis di sana:

1. Titik hitung server: `PosApi.bayar` — pola koreksi `selisihTotal` +
   `hasil_server_json` yang sudah dipoll `struk_screen.dart` tiap 500 ms;
   mesin harga dipanggil DI SITU, klien hanya pratinjau.
2. **Urutan harga-grosir vs AturanDiskon wajib diputuskan sebelum koding**
   (dok. 48 §7): rekomendasi — harga grosir menentukan HARGA SATUAN,
   AturanDiskon memotong SETELAHNYA; karena `dasarPerhitungan`
   (SETELAH_DISKON/HARGA_AWAL) sudah ada di `AturanDiskon`, definisi
   "HARGA_AWAL" harus dinyatakan = harga sesudah grosir, dan dituliskan di
   JavaDoc mesin harga.
3. Kemasan di keranjang disimpan sebagai SNAPSHOT `{nama, qtyDasar}` pada
   baris (alasan sama dengan snapshot `bahanBaku`: arsip struk tidak boleh
   berubah ketika preset diubah).
4. Pemilih kemasan tanpa scanner: lembar pilihan pada kartu produk kasir,
   hanya untuk produk ber-`kemasan` aktif; entri barcode tetap jalur utama.

### Fase B — Satuan jual per baris  *(= P2 dok. 48)*

Rincian dok. 48 §4 Fase 2 sudah memadai. Satu jangkar tambahan: konversi WAJIB
memakai `faktorUomInputKeDasar` (`KantinHelper.java:1002`) lewat aksi API —
fungsi itu satu-satunya yang menegakkan kesekategorian dengan pesan yang bisa
dibaca pengguna; `UomKonversi` klien hanya untuk pratinjau, bukan keputusan.

### Fase C — Reordering lengkap  *(= P3 dok. 48)*

Rincian dok. 48 memadai. Jangkar: pembulatan NAIK ke satuan pembelian memakai
`faktorUomInputKeDasar` yang sama (contoh PDF: butuh 70 kg, karung 50 →
2 karung); keluaran draf menumpang `DraftPembelian` yang sudah ada, dan
idempotensi `StokThresholdScheduler` (tidak dobel selama pengajuan
BARU/DIPROSES ada) diperluas ke draf.

### Fase D — Reservasi, kekurangan → pengajuan, UNBUILD  *(= P4 dok. 48; SETELAH Fase 0)*

1. UNBUILD menjadi sederhana setelah Fase 0: tipe dokumen baru yang mutasinya
   kebalikan OUTPUT+ISSUE (barang jadi keluar, komponen sesuai BOM masuk) —
   ledger yang sama, arah terbalik, idempoten yang sama.
2. Reservasi (`qtyReserved`) baru bermakna sekarang karena ISSUE sungguhan
   memotong stok. Keputusan §4 no. 4 dok. 48 (reserved menolak penjualan
   kasir atau sekadar informasi) tetap prasyarat.
3. Kekurangan komponen saat aktivasi WO → `PengajuanPembelianGudang` dengan
   rujukan WO (mesin yang ada, bukan mesin baru) — sudah benar di dok. 48.

### Fase E — MTO dan QC  *(= P5/P6 dok. 48; QC SETELAH Fase 0)*

Koreksi §1.2 mengubah titik berangkat QC: karantina TIDAK dibangun dari nol —
gunakan `ProdukBatch.STATUS_KARANTINA` + `catatMutasiBatch` yang ada. Alur
yang perlu dibangun: pemicu check pada OUTPUT (opsional per produk), dokumen
Quality Alert ringan, dan tiga disposisi yang SEMUANYA sudah punya mesin
setelah fase-fase di atas: rework = WO baru, unbuild = Fase D, scrap = WASTE
(yang setelah Fase 0 benar-benar memotong stok). Jurnal per disposisi lewat
dasbor Draft Jurnal (konsisten dok. 06/09).

---

## 4. Keputusan pemilik sistem — tambahan atas dok. 48 §6

1. *(mendesak, memblokir Fase A)* Urutan harga grosir vs AturanDiskon — §3
   Fase A butir 2.
2. *(memblokir Fase 0 butir eksekusi)* Dokumen produksi lama yang SUDAH
   berstatus POSTED/COMPLETED sebelum Fase 0: dibiarkan tanpa efek stok
   (disarankan — mengubah masa lalu menggeser stok tiba-tiba), atau dibuatkan
   migrasi opname sekali jalan?
3. `unitCost` OUTPUT → `Produk.hargaBeli`: otomatis, atau tombol "terapkan
   HPP" manual? (Menyentuh margin seluruh laporan.)
4. Batch `DIMUSNAHKAN`: hari ini perubahan stok fisiknya lewat `stok_fisik`
   (mutasi OPNAME_BATCH), status sendiri tidak memotong. Apakah pemusnahan
   wajib otomatis men-nol-kan stok batch + menulis opname, dengan jurnal beban
   kerugian? (Ini padanan "Scrap" PDF untuk barang kedaluwarsa.)

## 5. Verifikasi tertunda

- Apakah mutasi `OPNAME_BATCH` ikut mengalir ke `koperasi.stok_opname` (rumus
  stok utama) atau hanya sub-ledger batch — menentukan apakah karantina batch
  mengurangi stok yang bisa dijual. Perlu dibaca sebelum Fase E didesain.
- Kelengkapan jurnal draft-jurnal untuk opname/waste (warisan §7 dok. 48).
