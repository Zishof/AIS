# 16 — Reimbursement Pegawai

Penggantian biaya yang lebih dulu ditalangi pegawai. Dipindahkan dari layar ZK
`ais.action.master.akunting.ReimbursementPegawaiAction`.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/ReimbursementApiHelper.java` |
| Kunci menu | `reimbursement` (fail-closed, hak per-aksi) |
| Muara DPC | `TransferDpcUtil` — kolom `reimbursement_pegawai` |
| Layar | `apps/ebisnis/lib/screens/reimbursement_screen.dart` |

---

## 1. Lima status, dan keputusan tiga arah

Modul ini satu-satunya di grup Keuangan yang tidak berhenti pada "setuju atau tolak":

| Status | Artinya |
|---|---|
| `Diajukan` | menunggu keputusan atasan |
| `Revisi` | **dikembalikan** kepada pengaju untuk diperbaiki |
| `Ditolak` | ditutup |
| `Disetujui` | siap diajukan ke DPC |
| `Lunas` | sudah dibayarkan |

Status **Revisi** itu yang membedakannya. Karena itu API menyediakan tiga aksi keputusan
— `reimbursement_setujui`, `reimbursement_tolak`, dan `reimbursement_revisi` — bukan dua.

**Catatan atasan wajib** pada penolakan maupun permintaan revisi. Tanpa alasan, pengaju
tidak tahu apa yang harus diperbaiki, sehingga status "Revisi" kehilangan gunanya.

**Menyimpan perbaikan mengembalikan dokumen ke antrean.** Begitu pengaju menyimpan ulang,
statusnya kembali `Diajukan` dan penyetuju sebelumnya dikosongkan — pengajuan yang sudah
direvisi tidak boleh diam-diam membawa persetujuan lama.

---

## 2. Anggaran wajib atau tidak ditentukan oleh JENISNYA

`jenisReimbursement.menggunakanAnggaran` yang memutuskan:

- **Menyala** → anggaran wajib dipilih. Pesannya menyebut nama jenisnya, supaya pengguna
  tahu aturan itu datang dari mana.
- **Padam** → yang wajib justru **akun pada jenisnya**. Bila admin belum melengkapinya,
  pengajuan ditolak dengan pesan yang menyebut siapa yang harus melengkapi dan di tab
  mana — bukan sekadar "data tidak lengkap".

Layar membaca bendera itu dari aksi `reimbursement_opsi` dan menyembunyikan pemilih
anggaran bila tidak dipakai, tetapi **server tetap memeriksanya ulang**: layar tidak
memegang aturan, ia hanya tidak menampilkan isian yang percuma.

---

## 3. Rincian: akun diturunkan, bukan dipilih

Tiap baris memilih **Jenis Pengeluaran**, dan akunnya diturunkan dari sana. Dua kegagalan
yang mirip sengaja dibedakan pesannya:

| Keadaan | Pesan |
|---|---|
| Jenis dipilih, akunnya belum dipetakan admin | "Akun untuk Jenis Pengeluaran … belum dipetakan oleh administrator …" |
| Jenis memang belum dipilih | "Setiap baris rincian wajib memilih Jenis Pengeluaran." |

Jenis yang akunnya kosong **tetap ditampilkan** di layar, dengan peringatan merah bahwa
barisnya akan ditolak — bukan disembunyikan. Menyembunyikannya membuat pengguna mencari
sesuatu yang tidak akan pernah muncul.

Nilai dokumen dihitung dari rincian (`nominal = Σ jumlah`), bukan diketik, dan minimal
satu baris harus valid.

---

## 4. Urutan validasi

Disamakan dengan `ReimbursementPegawaiAction.onSave`:

1. Jenis Reimbursement
2. Judul pengajuan
3. Anggaran (bila jenisnya memakai anggaran) **atau** akun pada jenisnya (bila tidak)
4. Pegawai penerima
5. Tanggal pengeluaran
6. Rincian: akun & jumlah per baris, lalu minimal satu baris valid

---

## 5. Penjaga ubah & hapus

Pengajuan berstatus **Disetujui** atau **Lunas** tidak boleh diubah maupun dihapus —
sama dengan layar ZK yang menyembunyikan tombol Ubah/Hapus pada kedua status itu.
Dokumen yang sudah dijurnal atau sudah masuk DPC juga terkunci.

Muara DPC-nya: `DaftarPengajuanTransfer.simpanReimbursement`, dengan gerbang status
**Disetujui**.

---

## 6. Dua catatan tentang lingkungan uji

**Seluruh data master modul ini kosong** di basis data UAT: `akunting.jenis_reimbursement`,
`akunting.jenis_pengeluaran`, dan `public.pegawai` semuanya nol baris. Harness membuat
sendiri satu pegawai, tiga jenis reimbursement (memakai anggaran / tanpa anggaran berakun /
tanpa anggaran tanpa akun), dan dua jenis pengeluaran (berakun / belum dipetakan), lalu
menghapus semuanya di akhir. Artinya modul ini terbukti secara mekanis, tetapi **belum
pernah bersentuhan dengan data nyata** seperti modul Keuangan lainnya.

**Kolom `nip` ada di entitas `Pegawai` tetapi tidak ada di tabelnya** pada basis data ini.
Query pemilih pegawai karena itu hanya memakai kolom yang pasti ada (`nama`, `aktif`,
`satuan_kerja`) — pelajaran umum: pada basis data yang skemanya bergeser, jangan
menyimpulkan kolomnya ada hanya karena entitasnya punya propertinya.

---

## 7. Hasil uji

`TesReimbursement` — **29 lulus, 0 gagal**, sisa data uji 0. Mencakup ketujuh pesan
validasi, perhitungan nilai dari rincian, alur Diajukan → Revisi → Diajukan → Disetujui,
penolakan tanpa catatan, muara DPC sebelum dan sesudah persetujuan, serta penjaga ubah
dan hapus.

Sisi klien: `flutter analyze` 0 error, `flutter test` **236 lulus**.
