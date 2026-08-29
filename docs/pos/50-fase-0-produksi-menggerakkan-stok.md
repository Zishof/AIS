# 50 — Fase 0 Terlaksana: Produksi Menggerakkan Stok

Implementasi Fase 0 dari [49](49-produksi-eksekusi-stok-dan-rencana-rinci.md)
(desain + Adendum 29-08). Catatan UAT lengkap dan catatan lingkungan uji ada di
repo Flutter: `docs/pos/2026-08-29-fase-0-eksekusi-stok-produksi.md`.

## Ringkas

- Ledger baru `koperasi.mutasi_stok_produksi` (entitas `MutasiStokProduksi`,
  dibuat Hibernate saat boot). POSTED menulis FORWARD; REVERSED menulis
  KONTRA-BARIS — ledger tidak pernah dihapus. Idempoten dua lapis:
  unik `(dokumen, baris, arah)` + kunci `PRODUCTION:<dok>:<jenis>:<baris>:<arah>`.
- `formulaStokSql` menjadi 9 suku; seluruh salinan arus stok disisir:
  kartu stok POS + Inventory & Sales, CTE laporan harian (parameter 8→9),
  audit ledger-vs-stok, baseline opname, `fStok` per tanggal, dan daftar
  `TABEL_REFERENSI_PRODUK` untuk gabung produk.
- Transisi POSTED/REVERSED transaksional dan TIDAK fail-safe: validasi baris
  (tanpa itemId / beda toko / qty≤0) menolak transisinya dengan pesan terbaca.
  BOM/WO/COST tidak pernah menggerakkan stok.

## Bukti

Harness `TesProduksiStok`: **19 lulus / 0 gagal** pada DB UAT, lewat jalur API
yang sama dengan klien. Rumus 9 suku terbukti DIEKSEKUSI (pergerakan stok di
harness terjadi melalui `recomputeStokProdukNative`).
`javac -source 1.7 -target 1.7` EXIT=0; cermin `src`/`java` md5 identik;
0 `.class` di source tree.

## Peringatan untuk penerus

1. Kredensial `-D` di setenv harness lama DITOLAK server sejak 29-08 —
   harness lama akan gagal konek sampai diperbaiki pemilik sistem.
2. Baseline opname sudah lama tertinggal 3 suku (retur/mutasi) — drift
   pra-Fase-0, dilaporkan, belum diperbaiki.
3. Dokumen produksi lama yang terlanjur POSTED dibiarkan tanpa efek stok
   (keputusan pemilik sistem).
