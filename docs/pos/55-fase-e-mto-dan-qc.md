# 55. Fase E — MTO (Make-To-Order) dan QC Hasil Produksi

Tanggal: 29 Agustus 2026  
Status: server **lulus** (TesFaseEMtoQc 17/17 di DB UAT; TesFaseCReorder tetap
19/19 pasca-refactor), Flutter **lulus** (suite penuh 511/511, analyze bersih
di berkas tersentuh); belum di-commit  
Rujukan: dok. 48 §4 Fase 5/6 (P5/P6) + §6 no. 3, dok. 49 Fase E + koreksi
§1.2, dok. 53 (rute), dok. 54 (UNBUILD)

## Keputusan

### MTO

1. **Rute produk diperluas**: `MTO_BELI` / `MTO_PRODUKSI` (konstanta
   `Produk.RUTE_MTO_*`; kolom `rute` Fase C dipakai ulang, tanpa skema baru).
2. **Pemicu = konfirmasi `SalesOrderLapangan` (DRAFT→PESAN) SAJA** — sesuai
   analisis dok. 48 §6 no. 3 sendiri: Pesanan POS adalah keranjang tertahan,
   memaksakan MTO di sana mengubah maknanya. Berjalan DI DALAM transaksi
   konfirmasi (SO terkonfirmasi tanpa pemicunya = kebohongan data, pola
   Fase 0).
3. **MTO_PRODUKSI → draf WO** lewat mesin BERSAMA baru
   `ProduksiApiHelper.buatWoDrafOtomatis` (diekstrak dari penjadwal ambang
   Fase C — SATU mesin utk ambang stok, MTO, dan REWORK QC; idempoten lewat
   `referenceNo` kunci `MTO:SO:so:produk` selama DRAFT/RELEASED/IN_PROGRESS;
   BOM ACTIVE ikut dirujuk; tanpa BOM tetap terbit dengan catatan jujur).
4. **MTO_BELI → `PengajuanPembelianGudang`** ber-kolom baru `so_id`
   (nullable) lewat `Toko.gudangPemasok`; idempoten per SO+produk selama
   BARU/DIPROSES; toko tanpa gudangPemasok → kebutuhan tetap dilaporkan di
   respons (`mto`), tidak diam-diam hilang.

### QC

5. **`Produk.perluQc`** (nullable; null/false = perilaku lama).
6. **OUTPUT POSTED** ber-produk QC → SATU dokumen **Quality Alert** per
   OUTPUT (jenis `QC` di `ProduksiDokumen` — ringan, menumpang penuh infra
   dokumen/baris/event; idempoten per OUTPUT lewat `referenceNo`) + batch
   ber-lot sama **DIKARANTINA** (`ProdukBatch.STATUS_KARANTINA` +
   `KantinHelper.catatMutasiBatch` yang SUDAH ADA — koreksi dok. 49 §1.2:
   karantina tidak dibangun dari nol; `catatMutasiBatch` dibuka
   package-visible, satu pencatat). Baris tanpa lot/batch dicatat jujur di
   notes ("karantina fisik manual"), QC tetap terbit.
7. **Disposisi** (aksi `produksi_qc_disposisi`; inti `terapkanDisposisiQc`
   teruji langsung, endpoint = pembungkus aktor+transaksi):
   - `REWORK` → draf WO (mesin no. 3) + karantina diangkat;
   - `UNBUILD` → dokumen UNBUILD DRAFT Fase D, baris OUTPUT + komponen BOM
     ter-skala di-prefill — staf meninjau lalu POSTED;
   - `SCRAP` → dokumen WASTE DRAFT Fase 0 (memotong stok saat POSTED);
   - `RELEASE` → lolos tanpa turunan, karantina diangkat.
   UNBUILD/SCRAP membiarkan batch KARANTINA — barangnya keluar lewat dokumen
   turunan. QC lalu POSTED ber-catatan disposisi; sudah didisposisi = ditolak.
8. **Jurnal per disposisi**: dokumen turunan (WASTE/UNBUILD/WO) adalah sumber
   jurnalnya; pemetaan akun + penghitungan di dasbor Draft Jurnal menunggu
   rekonsiliasi akunting pemilik (dok. 48 §7) — dicatat sebagai batas fase,
   bukan dibangun setengah jadi.

## Berkas

- `Produk.java` — konstanta `RUTE_MTO_*`, kolom `perlu_qc`.
- `KantinHelper.java` — validasi rute 4 nilai + `perlu_qc` di `produk_simpan`;
  `catatMutasiBatch` package-visible.
- `PosApi.java` — katalog kirim `perluQc`; rute aksi `produksi_qc_disposisi`.
- `PengajuanPembelianGudang.java` — kolom `so_id`.
- `ProduksiApiHelper.java` — jenis `QC`; `buatWoDrafOtomatis` (mesin bersama);
  `buatQcAlertJikaPerlu` (kait OUTPUT POSTED); `qcDisposisi` +
  `terapkanDisposisiQc` + `isiKomponenUnbuildDariBom`.
- `StokThresholdScheduler.java` — refactor pakai mesin bersama (badan
  pembuatan WO lama dihapus, perilaku dibuktikan tetap 19/19).
- `SalesInventoryReceivableHelper.java` — kait konfirmasi + `terapkanMto`.
- Flutter: `models.dart` (`rute` MTO + `perluQc`), `produk_screen.dart`
  (dropdown rute 4 pilihan + saklar QC), `produksi_screen.dart` (bagian
  Quality Alert + tombol disposisi di rincian), `app_shell.dart` (menu
  `produksi_quality_alert`), `test/mto_qc_kontrak_test.dart`.

## Bukti

`TesFaseEMtoQc` (pola dok. 44; refleksi ke fungsi persis yang dipanggil alur;
`gudang_pemasok` toko dipulihkan): **17/17** —

- konfirmasi SO: MTO_PRODUKSI → WO draf planned 7 merujuk BOM; MTO_BELI →
  pengajuan 4 ber-`so_id` lewat gudang pemasok; rute kosong tak disentuh;
  konfirmasi ulang idempoten; respons membawa ringkasan `mto`;
- OUTPUT POSTED: Quality Alert terbit, batch lot sama → KARANTINA + mutasi
  `QC_KARANTINA`; OUTPUT sama dua kali → QC tetap satu; produk tanpa
  `perlu_qc` → tanpa QC;
- disposisi: SCRAP → WASTE draf qty benar + QC POSTED + batch tetap
  KARANTINA; UNBUILD → draf ber-baris OUTPUT + komponen BOM ter-skala
  (2×2=4); RELEASE → batch AKTIF kembali + mutasi `QC_LEPAS`.

`TesFaseCReorder` diulang pasca-refactor: **19/19** (mesin bersama tidak
mengubah perilaku penjadwal ambang). `javac -source 1.7` EXIT=0 (7 berkas);
0 `.class` di pohon sumber. Flutter **511/511**.

Catatan harness: baris SO uji wajib menyertakan `version=0` —
`SalesOrderLapangan` ber-`@Version`, INSERT mentah tanpa kolom itu membuat
flush Hibernate NPE (bukan bug produksi; alur normal selalu lewat entitas).

## Di luar fase ini (batas yang disengaja)

1. Pemetaan akun jurnal disposisi di dasbor Draft Jurnal — menunggu
   rekonsiliasi akunting pemilik (dok. 48 §7).
2. Keputusan pemilik yang masih terbuka: reserved menolak penjualan kasir
   (dok. 48 §6 no. 4, titik pasang dicatat di dok. 54); kelipatan wajib
   kemasan dan harga tetap per-kemasan (§6 no. 1–2).
3. MTO dari Pesanan POS — sengaja TIDAK (lihat no. 2 di atas).

Dengan ini seluruh fase peta jalan dok. 48/49 (0, A, B, C, D, E) sudah
terlaksana dan terbukti di DB UAT.
