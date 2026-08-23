# 04 — Muara dokumen Keuangan: DPC (Daftar Pengajuan Transfer)

Dokumen keuangan yang sudah disetujui tidak langsung dibayar. Ia masuk lebih dulu ke
**Daftar Pengajuan Transfer** — kolam pengajuan milik bagian keuangan pusat — lalu dari
sana diproses menjadi Proses Transfer.

## 1. Bagaimana ZK melakukannya

Penautan dikerjakan `DaftarPengajuanTransfer.simpanXxx(...)`, yang **idempoten**: bila
dokumen sudah punya baris DPC, ia tidak membuat yang kedua.

Di ZK, tombol yang memicunya adalah *Singkronkan* pada layar Proses Transfer
(`SinkronDaftarPengajuanTransferHelper`), yang menyapu 15 tipe entitas dan menautkan yang
sudah siap tetapi belum punya baris DPC.

## 2. Yang dikerjakan

`ais/action/servlet/api/TransferDpcUtil.java` memindahkan tombol sapuan itu menjadi
**aksi per dokumen**, dengan gerbang persetujuan yang sama persis:

| Dokumen | Aksi | Syarat |
|---|---|---|
| Uang Muka | `uang_muka_ajukan_transfer` | status Disetujui |
| Kas Besar | `kas_besar_ajukan_transfer` | status Disetujui |
| Penggantian Kas Kecil | `penggantian_kas_kecil_ajukan_transfer` | status Disetujui **dan** penyetuju terisi |
| Pertanggungjawaban | `pj_uang_muka_ajukan_transfer` | penyetuju terisi **dan** ada dana dikembalikan (> 0,1) |
| Pertanggungjawaban Kas Besar | `pj_kas_besar_ajukan_transfer` | sama seperti di atas |

**Kas Kecil sengaja tidak punya aksi ini.** Pengeluaran kas kecil tidak ditransfer satu
per satu; uangnya kembali lewat dokumen Penggantian Kas Kecil.

Hak yang diperiksa adalah hak **approve** pada modul dokumennya — bukan sekadar hak
melihat — karena mengajukan ke DPC memindahkan dokumen ke tangan bagian keuangan.

## 3. Status pada daftar

`TransferDpcUtil.lampirkanStatus(session, modul, data)` menempelkan tiga field ke setiap
baris daftar, dengan **satu query untuk seluruh halaman** (bukan per baris):

| Field | Nilai |
|---|---|
| `dpcAda` | `true` bila dokumen sudah punya baris DPC |
| `dpcKode` | kode baris DPC-nya |
| `dpcStatus` | `Belum diajukan` / `Menunggu transfer` / `Sudah ditransfer` |

Nama kolom relasi per modul pada `akunting.daftar_pengajuan_transfer`: `uang_muka`,
`pertangungjawaban`, `kas_besar`, `pertangungjawaban_kas_besar`, `penggantian_kas_kecil`.
Nama kolom tidak pernah datang dari luar apa adanya — hanya lima nama itu yang diterima.

## 4. Sisi klien

Kelima layar mendapat tombol **Ajukan ke proses transfer**, muncul hanya bila dokumennya
sudah Disetujui dan pengguna punya hak approve. Dokumen yang sudah masuk kolam transfer
menampilkan ikon terkunci berisi kode DPC-nya, bukan tombol lagi. Kolom Status ikut
menampilkan penanda "di daftar transfer".

Pengajuan memakai jalur lokal-dulu yang sama dengan persetujuan, sehingga tetap bisa
dilakukan saat luring dan menyusul terkirim.

## 5. Batas yang perlu diketahui

Layar **Proses Transfer** sendiri belum diport. Dokumen yang sudah masuk DPC dari
Desktop/Android masih harus diproses menjadi transfer di aplikasi web. Ini berdampak ke
posting jurnal — lihat [06-posting-jurnal-keuangan.md](06-posting-jurnal-keuangan.md).
