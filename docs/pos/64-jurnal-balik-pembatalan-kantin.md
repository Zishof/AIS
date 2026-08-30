# Jurnal Balik Pembatalan Kantin

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78603** (commit sesi ini, 8 berkas).
Mirror `java/` selaras. Menutup butir **C** gap analysis
[61-gap-analysis-posting.md](61-gap-analysis-posting.md).

## 1. Masalah dan bentuk solusinya

Pembatalan transaksi kantin MENGHAPUS KERAS header + baris penjualan (by design, lihat
javadoc `PembatalanTransaksiUtil`) dan hanya meninggalkan arsip
`koperasi.pembatalan_transaksi` — dengan bendera `sudah_diposting` yang selama ini cuma
peringatan bagi bagian keuangan bahwa "ada selisih yang harus dibereskan manual".
Kini bendera itu punya kelanjutan otomatis: baris dasbor baru **"Pembatalan Penjualan
Kantin"** (kunci `pembatalan_kantin`, kategori/izin `posting_penjualan`) menjurnal-balik
pendapatan transaksi terposting yang dibatalkan.

Jurnal per arsip: **Dr akun pendapatan** (konfigurasi
`akun_pendapatan_pembatalan_kantin_id` — WAJIB diisi admin, tanpa ini mesin menolak
bekerja dan mencatat ke Error Log tanpa meninggalkan riwayat kosong) / **Cr akun kas**
dari CARA PEMBAYARAN yang terekam arsip (dicari per nama ke
`CaraPembayaranKoperasi`), senilai `total_biaya`, bertanggal DIBATALKAN. Arsip yang
cara bayarnya tak dikenal/tanpa akun dilewati dan tetap draf.

Penopangnya: arsip mendapat field `posting_history`; `grup_transaksi` mendapat kolom
referensi `pembatalan_transaksi` (relasi diberi `@Audited(targetAuditMode=NOT_AUDITED)`
karena arsipnya memang tidak di-audit Envers); `CommonAkunting` mendapat cabang
referensinya; `PostingHistory` mendapat `JENIS_PEMBATALAN_KANTIN`. Kolom-kolom baru
dibuat `hbm2ddl update`.

## 2. Batasan yang disengaja (arsip hanya menyimpan potret teks)

- Rincian per JENIS PRODUK tidak direkonstruksi — pendapatan dibalik agregat ke satu
  akun konfigurasi (lazimnya akun pendapatan penjualan kantin atau akun retur
  penjualan, keputusan tim akuntansi).
- **PPN** tidak terbalik otomatis (arsip tidak menyimpan pajak) — bila tenant memungut
  PPN kantin, porsi PPN dibalik manual lewat Jurnal Penyesuaian.
- **Sisi HPP** tidak terbalik otomatis (arsip tidak menyimpan qty × harga pokok) —
  koreksi lewat batal-mundur + posting ulang periode HPP berjalan, atau Jurnal
  Penyesuaian. Catatan terkait: pembatalan menghapus baris `koperasi.pembelian`
  ber-cap `posting_hpp` sekalian, sehingga HPP batch lama tetap menyimpan porsi barang
  batal.
- Arsip yang transaksi aslinya BELUM terposting (`sudah_diposting=false`) memang tidak
  butuh jurnal balik dan tidak dihitung.

## 3. Pengujian

Harness `TesPostingPembatalanKantin` (scratchpad, DB UAT `ais`), fixture `UATPBK-`
rentang **10–20 Oktober 2091** (dipastikan kosong):

| Skenario | Hasil |
|---|---|
| R1: sudah_diposting, cara dikenal, 320k | Dr pendapatan 320k / Cr kas 320k, bertanggal dibatalkan; arsip tercap |
| R2: belum pernah terposting | tidak dihitung dan tidak disentuh |
| R3: cara bayar tak dikenal | dihitung draf, dilewati mesin |
| Dasbor | draf 2 → terposting 1 + draf 1, konsisten mesin |
| Idempoten + batal | posting/batal ulang 0; jurnal habis, arsip kembali draf |

**LULUS 9, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih (8 berkas).
Penjaga tanpa-konfigurasi diverifikasi sekali di JVM terpisah (n=0 tanpa riwayat
kosong); tidak bisa diuji-lalu-dipulihkan dalam satu JVM karena cache
`KonfigurasiManager` menahan hasil baca pertama — catatan harness baru.

## 4. Lanjutan

Sisa gap dok 61: **D** disposal aset, **B** keluarga dana anggota (menunggu keputusan
bagan akun), **E** lingkup Inventory Sales. Perbandingan hub ZK vs POS ada di
[63-gap-analysis-zk-vs-pos.md](63-gap-analysis-zk-vs-pos.md).
