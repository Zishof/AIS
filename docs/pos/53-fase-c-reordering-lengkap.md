# 53. Fase C — Reordering Lengkap (Min-Max, Rute BELI/PRODUKSI)

Tanggal: 29 Agustus 2026  
Status: server **lulus** (TesFaseCReorder 19/19 di DB UAT), Flutter **lulus**
(suite penuh 501/501, analyze bersih di berkas tersentuh); belum di-commit  
Rujukan: dok. 48 §4 Fase 3 (P3), dok. 49 Fase C, dok. 52 (satu penegak UOM)

## Koreksi peta dok. 48/49

Kedua dokumen menyebut keluaran rute BELI "menumpang `DraftPembelian`".
**`DraftPembelian` adalah entitas draf PENJUALAN kasir** (kios, member,
caraBayar, hargaJual) — menumpanginya akan mencemari makna tabel, alasan yang
sama dengan Fase 0 menolak menumpang `pemakaian_bahan_baku`. Mesin draf
pembelian yang sebenarnya **sudah ada**: `PengajuanPembelianGudang` status
BARU — draf yang manusia konfirmasi lewat layar Pengajuan/Pengadaan. Fase C
memperkaya mesin itu, tidak membuat (atau menumpangi) yang lain.

## Bug laten yang terungkap (dan diperbaiki)

`StokThresholdScheduler.kirimNotifikasi` memakai `select id from
public.tbmuser` — **PK tbmuser adalah `userid` (String), bukan `id`**.
Kueri itu SELALU melempar PSQLException; exception-nya memang ditangkap,
tetapi transaksi PostgreSQL sudah telanjur dibatalkan server-side, sehingga
`tx.commit()` siklus ikut gagal → **pengajuan otomatis tidak pernah
tersimpan sejak fitur ambang stok lahir**. Perbaikan dua lapis:

1. Kueri dibetulkan ke `userid` (String, konsisten semua pemakai lain).
2. Notifikasi kini berjalan di **sesi terpisah** — kegagalan apa pun di
   dalamnya tidak lagi bisa meracuni transaksi ledger pengajuan/WO.

## Keputusan

1. **`AmbangStokGudang.maxQty`** (nullable): saran qty = `maxQty − stok`
   (kebijakan min-max PDF); kosong = perilaku lama (buffer 2× ambang).
2. **Pembulatan NAIK ke satuan pembelian** memakai
   `KantinHelper.faktorUomInputKeDasar` yang SAMA (kini `public`; contoh
   literal PDF: butuh 70 kg, karung 50 → 2 karung = 100). Kegagalan
   konversi (kategori UOM salah) TIDAK menggagalkan siklus: saran tetap qty
   dasar mentah + keterangan "PERHATIAN: …" menjelaskan cara memperbaiki.
3. **`Produk.rute`** (nullable `BELI`/`PRODUKSI`; kosong = BELI = perilaku
   hari ini). Konstanta `Produk.RUTE_BELI/RUTE_PRODUKSI`.
4. **Rute PRODUKSI → draf Work Order**: `ProduksiDokumen` type `WO` status
   `DRAFT`, `plannedQty` = saran, `uom` = satuan dasar, `referenceNo` =
   kunci `AUTO-AMBANG:produk:gudang`. BOM ACTIVE yang baris OUTPUT-nya =
   produk ikut dirujuk (`bomId`); tanpa BOM, WO tetap terbit dengan catatan
   jujur "BELUM ADA BOM AKTIF…". Notifikasi menyebut draf WO, bukan
   pengajuan.
5. **Idempotensi diperluas ke draf** (dok. 49): pengajuan — mesin lama
   dipertahankan (tak dobel selama BARU/DIPROSES); WO — tak dobel selama
   ada WO otomatis DRAFT/RELEASED/IN_PROGRESS berkunci `referenceNo` sama.
6. Skema aditif via Hibernate (2 kolom nullable, tanpa entitas baru →
   tanpa perubahan mapping cfg; `hbm2ddl` menambah kolom saat boot).

## Berkas

- `ais/database/model/inventory/AmbangStokGudang.java` — `maxQty`.
- `ais/database/model/inventory/Produk.java` — `rute` + konstanta. (Berkas
  ini campuran 519 CRLF / 39 LF di HEAD; dinormalkan ke CRLF mayoritas,
  disengaja.)
- `ais/common/StokThresholdScheduler.java` — saran min-max, pembulatan
  satuan pembelian, cabang rute PRODUKSI (`terbitkanWoDraf`), perbaikan
  bug tbmuser + sesi notifikasi terpisah.
- `ais/action/servlet/api/KantinHelper.java` — `faktorUomInputKeDasar`
  jadi `public`; `produk_simpan` menerima `rute` (validasi BELI/PRODUKSI).
- `ais/action/servlet/PosApi.java` — katalog mengirim `rute`.
- `ais/action/master/inventory/PengajuanPembelianGudangAction.java` +
  `.../pengajuan_pembelian_gudang.zul` — kolom "Target Maks" di layar
  Ambang Stok (tooltip menjelaskan min-max & pembulatan).
- Flutter `models.dart` (`Produk.rute`), `produk_screen.dart`
  (SegmentedButton "Rute Pemenuhan Ulang Stok" di bawah Jenis Item,
  payload `rute`), `test/rute_produk_kontrak_test.dart`.

## Bukti

`TesFaseCReorder` (pola dok. 44; refleksi ke `prosesSatuAmbang` PERSIS yang
dipanggil siklus — sengaja TIDAK memanggil `jalankanSekali()` agar tidak
menyapu ambang asli DB UAT): **19/19** —

- maxQty: saran 90 dibulatkan naik ke 2×Karung50 = 100, keterangan menyebut
  kemasan; maxQty kosong: saran lama 30 (2×20−10);
- idempoten pengajuan (lama) dan idempoten WO (baru) — dua kali proses
  tidak menggandakan;
- rute PRODUKSI: WO DRAFT terbit (planned 55), `bomId` merujuk BOM ACTIVE
  baris OUTPUT, TIDAK ada pengajuan pembelian; tanpa BOM → `bom_id` kosong
  + catatan "BELUM ADA BOM";
- UOM pembelian lintas kategori: siklus tetap jalan, saran tak dibulatkan,
  keterangan "PERHATIAN"; stok di atas ambang tidak memicu.

`javac -source 1.7` EXIT=0 (semua berkas, termasuk layar ZK); 0 `.class`
di pohon sumber. Flutter 501/501; analyze bersih di berkas tersentuh.

## Di luar fase ini

- Layar khusus meninjau WO draf otomatis (sudah terlihat di daftar WO
  produksi biasa — cukup untuk Fase C).
- Reservasi/kekurangan komponen WO = Fase D; MTO/QC = Fase E.
