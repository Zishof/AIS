# 54. Fase D — Reservasi Komponen, Kekurangan → Pengajuan, UNBUILD

Tanggal: 29 Agustus 2026  
Status: server **lulus** (TesFaseDProduksi 17/17 di DB UAT), Flutter **lulus**
(suite penuh 501/501, analyze bersih di berkas tersentuh); belum di-commit  
Rujukan: dok. 48 §4 Fase 4 (P4), dok. 49 Fase D, dok. 50 (ledger Fase 0),
dok. 53 (rute PRODUKSI menghasilkan WO draf)

## Keputusan

1. **UNBUILD = tipe dokumen produksi baru** (`production_unbuild` → `UNBUILD`)
   yang menumpang PENUH infrastruktur Fase 0 — dokumen/baris/event/hak
   akses/idempoten/REVERSED semuanya sama. Satu-satunya yang baru: arah stok
   menjadi **per-BARIS** (`arahMasukBaris`): baris bertipe `OUTPUT` (barang
   jadi) KELUAR, baris lain (komponen BOM) MASUK — kebalikan OUTPUT+ISSUE
   dalam satu dokumen. Transisi memakai jalur baku DRAFT→POSTED→REVERSED.
   Jenis dokumen lain tetap arah per-dokumen — tidak ada perubahan perilaku.
2. **Reservasi komponen**: entitas baru `ReservasiStokProduksi`
   (`inventory_production.production_reservation`; satu baris = satu komponen
   BOM yang dikunci satu WO; `qty` kebutuhan awal, `qtySisa` berjalan):
   - WO **RELEASED** → tulis baris reservasi (kebutuhan = qty baris BOM ×
     rasio `plannedQty` WO / qty baris OUTPUT BOM); idempoten per WO.
   - **ISSUE POSTED** ber-`referenceNo` = documentNo WO satu toko →
     `qtySisa` berkurang (0 = SELESAI); ISSUE **REVERSED** memulihkan
     (dibatasi qty awal). ISSUE tanpa referensi WO tidak menyentuh apa pun.
   - WO **CANCELLED/COMPLETED** → semua baris AKTIF ditutup (BATAL/SELESAI).
     Baris tidak pernah dihapus — jejak komersial.
   - **INFORMASI SAJA bagi kasir**: keputusan dok. 48 §6 no. 4 (reserved
     menolak penjualan kasir?) MASIH TERBUKA — sampai pemilik mengunci,
     ledger ini tidak mengurangi stok yang boleh dijual dan tidak disentuh
     alur kasir mana pun.
3. **Kekurangan komponen saat rilis** → `PengajuanPembelianGudang` (mesin
   lama) dengan kolom baru `wo_id` (nullable, aditif): tersedia = stok toko −
   reservasi AKTIF WO LAIN; kurang = butuh − tersedia. Gudang asal =
   `Toko.gudangPemasok` (relasi yang sudah ada); tujuan = induknya. Idempoten
   per (WO, produk) selama BARU/DIPROSES. **Toko tanpa gudangPemasok:
   kekurangan tetap dilaporkan** di respons rilis (array `kekurangan`) dan
   dicatat di notes dokumen — tidak diam-diam hilang.
4. Skema via Hibernate: 1 entitas baru (didaftarkan di `hibernate.cfg.xml`),
   1 kolom nullable (`wo_id`); `hbm2ddl` membuat saat boot, tanpa DDL tangan.

## Berkas

- `ais/database/model/inventory/ReservasiStokProduksi.java` (baru).
- `ais/database/model/inventory/PengajuanPembelianGudang.java` — `woId`.
- `ais/action/servlet/api/ProduksiApiHelper.java` — jenis UNBUILD,
  `arahMasukBaris`, kait `ubahStatus` (RELEASED/CANCELLED/COMPLETED WO;
  ISSUE POSTED/REVERSED), `reservasiSaatRilis`/`tutupReservasi`/
  `sesuaikanReservasiIssue`.
- `hibernate.cfg.xml` — 1 mapping.
- Flutter `screens/produksi_screen.dart` + `widgets/app_shell.dart` — menu
  "Unbuild / Bongkar Barang Jadi" (kunci izin `produksi_production_unbuild`,
  gerbang profil inventory-sales yang sama dengan menu produksi lain).

## Bukti

`TesFaseDProduksi` (pola dok. 44; refleksi ke method private PERSIS yang
dipanggil `ubahStatus`; `gudang_pemasok` toko dipulihkan ke nilai semula):
**17/17** —

- rilis WO (BOM: OUTPUT 1, kompA 2, kompB 3; planned 10): reservasi 20/30;
  kekurangan hanya kompB (stok 10) → pengajuan 20 ber-`wo_id` dan gudang
  pemasok; kompA cukup → tanpa pengajuan; rilis ulang tidak menggandakan;
- ISSUE 5 kompA ber-referensi WO: sisa 15 + mutasi keluar 5; REVERSED:
  sisa pulih 20 + kontra-baris masuk 5;
- UNBUILD (jadi 2 keluar; kompA 4 & kompB 6 masuk): arah per-baris benar;
  posting dua kali tidak menggandakan; REVERSED menukar arah;
- batal WO menutup reservasi → BATAL.

`javac -source 1.7` EXIT=0; 0 `.class` di pohon sumber; Flutter 501/501.

## Di luar fase ini

- Keputusan pemilik §6 no. 4: reserved menolak penjualan kasir — begitu
  dikunci "menolak", titik pasangnya adalah pemeriksaan stok `bayar`
  (`KantinHelper`) membaca `sum(qtySisa)` AKTIF per produk+toko.
- Tampilan daftar reservasi per WO di layar produksi (data sudah ada di
  tabel; layar menyusul bila diminta).
- MTO dan QC = Fase E (dok. 48 §6 no. 3 juga masih terbuka).
