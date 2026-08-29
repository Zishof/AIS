# Posting Simpan-Pinjam Koperasi: Kerangka Yatim Akhirnya Dilengkapi

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78584** (commit sesi ini, 8 berkas).
Mirror `java/` selaras. Menutup butir **A** gap analysis
[61-gap-analysis-posting.md](61-gap-analysis-posting.md).

## 1. Kerangka yang dilengkapi

Sejak lama `TransaksiKoperasi` punya field `postingHistory`, `grup_transaksi` punya
kolom `transaksi_koperasi`, dan layarnya mengunci tanggal persetujuan saat "terposting"
— tanpa satu pun pengisi. Yang ditambahkan:

- **Master `ProdukKoperasi` mendapat dua akun** (separuh desain yang hilang):
  `akun` = posisi dana produk (kewajiban simpanan untuk tipe PENAMBAHAN, piutang
  pembiayaan untuk PENGURANGAN) dan `akun_margin` = pendapatan margin — yang kedua
  BELUM dipakai mesin ini (margin diakui saat angsuran; keluarga
  `PembayaranAnggotaKoperasi`, dok 61 butir B) tetapi disiapkan sekarang berikut
  picker-nya di layar master (pola `AmbilDataAkunBanbox`). Kolom dibuat otomatis
  `hbm2ddl update` saat start.
- **`CommonAkunting.saveTransaksi` mendapat cabang referensi `TransaksiKoperasi`**
  (rantainya belum pernah menyetel `grup_transaksi.transaksi_koperasi`).
- **`PostingHistory.JENIS_SIMPAN_PINJAM`** ("Simpan Pinjam Koperasi").
- **Baris dasbor baru "Simpan Pinjam Koperasi"** (kunci `simpan_pinjam_koperasi`,
  kategori sendiri; izin memakai kunci deskriptif fail-closed pola siswa/mahasiswa)
  + mesin `postingSemua`/`batalkanPostingSemua` di `TransaksiKoperasiAction` + dispatch.

## 2. Desain jurnal

Kriteria (mesin = dasbor): status **Disetujui**, aktif, `nilai` ≠ 0, rentang
`tanggal_pembuatan` (kolom filter layar), dan **TANPA pengajuan transfer** — dokumen
yang cair lewat pengajuan transfer sudah dijurnal baris "Jurnal Pengajuan Transfer"
(debet akun tujuan per pengajuan), menjurnalnya lagi berarti dobel.

Jurnal per dokumen, senilai **pokok** (`nilai`), bertanggal persetujuan (fallback
tanggal pembuatan), arah mengikuti `TipeProdukKoperasi.jenis`:

| Tipe produk | Debet | Kredit |
|---|---|---|
| PENAMBAHAN (simpanan disetor) | akun kas cara pembayaran | akun posisi produk |
| PENGURANGAN (pembiayaan cair tunai) | akun posisi produk | akun kas cara pembayaran |

Margin sengaja tidak dijurnal pada dokumen pengajuan — pengakuan margin menunggu
jurnal angsuran (butir B). Produk tanpa akun/tipe atau dokumen tanpa cara pembayaran
dilewati dan tetap terhitung draf. Konvensi keluarga mesin berlaku penuh (riwayat
`posting=true`, cap-hanya-bila-tersimpan, hapus riwayat kosong, batal menghapus baris
anak dulu, hanya non-closing).

## 3. Pengujian

Harness `TesPostingSimpanPinjam` (scratchpad, DB UAT `ais`), fixture `UATSPK-` rentang
**10–20 September 2091** (dipastikan kosong):

| Skenario | Hasil |
|---|---|
| S1 simpanan 500k disetujui | Dr kas 500k / Cr akun simpanan 500k; jurnal bertanggal persetujuan |
| P1 pembiayaan 2jt disetujui | Dr akun piutang 2jt / Cr kas 2jt |
| T1 lewat pengajuan transfer | dikecualikan (anti-dobel jalur transfer) |
| B1 belum disetujui | tidak pernah terpilih |
| N1 produk tanpa akun posisi | dihitung draf, dilewati mesin, tanpa jurnal |
| Dasbor | draf 3 → terposting 2 + draf 1, konsisten mesin |
| Idempoten + batal | posting/batal ulang 0; jurnal habis, dokumen kembali draf |

**LULUS 14, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih (8 berkas).

Catatan harness baru: `produk_koperasi.koperasi` NOT NULL (buat koperasi fixture);
`getFormula()` produk menagih konstanta seed `ConstantValues.BIAYA_ADMIN` pada formula
kosong/`[]` — isi kolom `formula` dengan JSON non-default agar flush Hibernate tidak
NPE di UAT tanpa data seed; kolom `tanggalpersetujuanmanual` memakai penamaan default
(bukan snake_case).

## 4. Lanjutan

Butir gap analysis berikutnya (dok 61): **C** jurnal balik pembatalan kantin,
**D** disposal aset, **B** keluarga dana anggota (menunggu keputusan bagan akun,
`akun_margin` produk sudah disiapkan dari sini), **E** keputusan lingkup Inventory
Sales.
