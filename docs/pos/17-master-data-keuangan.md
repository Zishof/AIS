# 17 — Master Data Keuangan

Enam data master yang menjadi tulang punggung seluruh grup Keuangan, dan yang sampai
sekarang **hanya dapat dipelihara di layar ZK**.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/MasterKeuanganApiHelper.java` |
| Kunci menu | `master_keuangan` (fail-closed, hak per-aksi create/update/delete) |
| Layar | `apps/ebisnis/lib/screens/master_keuangan_screen.dart` |
| Harness | `TesMasterKeuangan` — **59 lulus, 0 gagal**, sisa data uji 0 |

---

## 1. Kenapa modul ini dikerjakan

Ini **celah yang dibuat oleh pekerjaan sebelumnya sendiri.** Delapan modul Keuangan sudah
dipindahkan ke Desktop/Android, tetapi tidak satu pun kunci menu untuk jenis uang muka,
jenis kas kecil/besar, jenis reimbursement, jenis pengeluaran, maupun cara pembayaran
transfer. Padahal:

- **Pemetaan akun di sinilah yang menentukan jurnal setiap dokumen Keuangan.** Bila
  akunnya belum lengkap, dokumen tetap bisa diajukan dan disetujui, lalu **dilewati begitu
  saja** oleh mesin posting — tanpa satu pun pesan galat di dokumennya. Gejala seperti ini
  sangat sulit dilacak dari sisi pengguna.
- **Dua tabelnya kosong** di basis data UAT (`akunting.jenis_reimbursement` dan
  `akunting.jenis_pengeluaran`), sehingga modul Reimbursement Pegawai belum dapat dipakai
  sama sekali sebelum ada yang mengisinya.

| Tipe | Dipakai oleh | Akun yang dipetakan |
|---|---|---|
| `jenis_uang_muka` | Uang Muka, LPJ, Dana Talangan | penerima, kelebihan, sponsor |
| `jenis_kas_kecil` | Kas Kecil, Penggantian Kas Kecil | kas kecil, penutup kas kecil |
| `jenis_kas_besar` | Kas Besar, LPJ Kas Besar | kas besar, penerima |
| `jenis_reimbursement` | Reimbursement Pegawai | akun biaya (bila tanpa anggaran) |
| `jenis_pengeluaran` | rincian Reimbursement | akun biaya per baris |
| `cara_pembayaran_transfer` | Proses Transfer, posting jurnal | kas/bank, transitori |

---

## 2. Satu layar, satu formulir, enam tipe

Yang berbeda antar tipe **hanya label medan akunnya**, bukan kuncinya. Karena itu server
mengirim kunci **posisional** — `akunId`, `akunKeduaId`, `akunKetigaId` — yang sama persis
dipakai daftar maupun simpan, dan hanya `label`-nya yang berganti per tipe:

```
"medanAkun": [ {"kunci":"akunId","label":"Akun Kas Besar","wajibUntukJurnal":true},
               {"kunci":"akunKeduaId","label":"Akun Penerima","wajibUntukJurnal":true} ]
```

Akibatnya **tidak ada satu pun cabang per tipe di sisi layar** — sesuatu yang diuji secara
eksplisit (`expect(src, isNot(contains("== 'jenis_kas_besar'")))`). Menambah tipe ketujuh
kelak cukup menyentuh server.

Bendera lain yang dikirim bersamanya: `punyaKode`, `punyaAnggaran` (hanya
`jenis_reimbursement`), dan `punyaSatuanKerja` (semua kecuali `jenis_pengeluaran`, yang
memang tidak punya kolomnya).

---

## 3. Akun belum lengkap: ditandai, bukan dilarang

Menolak penyimpanan jenis yang akunnya belum diketahui akan menghalangi admin menyiapkan
data bertahap. Yang dilakukan justru sebaliknya:

- jenis tanpa akun **tetap boleh disimpan**;
- barisnya ditandai `akunLengkap: false` beserta ikon peringatan pada namanya;
- jumlahnya dilaporkan sebagai `belumLengkap` dan ditampilkan sebagai spanduk di atas
  tabel: *"N … belum lengkap akunnya. Dokumen yang memakainya tetap bisa diajukan, tetapi
  TIDAK akan terjurnal."*

Yang dihitung hanya medan bertanda `wajibUntukJurnal` — akun sponsor dan akun transitori,
misalnya, memang boleh kosong.

---

## 4. Penjaga hapus

Jenis yang **sudah dipakai dokumen tidak dapat dihapus** — bukan sekadar karena FK menolak,
tetapi karena menghapusnya memutus riwayat dokumen lama dari akunnya. Jumlah pemakaiannya
disebutkan dalam pesannya, berikut jalan keluarnya:

> *Jenis Reimbursement ini sudah dipakai 1 dokumen sehingga tidak boleh dihapus.
> Nonaktifkan saja bila tidak dipakai lagi.*

Jumlah itu juga tampil sebagai kolom **Dipakai** pada tabel, dan layar menolak lebih dulu
tanpa perjalanan ke server bila angkanya di atas nol — server tetap memeriksanya ulang.

`jenis_pengeluaran` dihitung berbeda: ia dirujuk dari dalam kolom formula (JSON) rincian
reimbursement, bukan lewat relasi, sehingga pemakaiannya dicari sebagai teks.

---

## 5. Tipe divalidasi dengan daftar putih, tidak pernah masuk SQL

Parameter `tipe` datang dari klien, dan seluruh nama tabel/kolom di helper ini **tertulis
di kode**. `tipeSah()` hanya menerima enam nilai; apa pun di luar itu ditolak dengan
`"Tipe master keuangan tidak dikenali."` sebelum satu query pun disusun. Harness mengujinya
dengan tipe `"akunting.akun; DROP TABLE x"`.

Penyimpanan lewat **entitas Hibernate** (bukan SQL langsung) supaya audit Envers ikut
berjalan sebagaimana pada layar ZK.

---

## 6. Lokal-dulu, dan jebakan kunci antrean

Layar ini memakai jalur lokal-dulu yang sama dengan modul Keuangan lain
(`MasterOffline.daftarCacheDulu` + `prosesSimpanMaster`). Satu hal yang **wajib** diperhatikan
di sini dan tidak muncul di layar lain: enam tipe berbagi satu layar, tetapi **id-nya
berasal dari enam tabel berbeda**. Karena itu nama tipe ikut masuk ke kunci antrean maupun
kunci cache:

```
cacheKey : master_keuangan:<tipe>
kunci    : master_keuangan:<tipe>:<id>
```

Tanpa itu, `jenis_kas_kecil` id 3 dan `jenis_kas_besar` id 3 akan menempati slot antrean
yang sama dan saling menimpa — persis cacat yang pernah terjadi antara Kas Kecil dan Kas
Besar (lihat [07-temuan-dan-jebakan.md](07-temuan-dan-jebakan.md)).

---

## 7. Cacat lain yang ikut ditambal

`_menuDariLabel` pada `app_shell.dart` **tidak pernah memuat** `'Dana Talangan'` dan
`'Reimbursement Pegawai'` sejak kedua modul itu dipasang. Akibatnya, membuka menunya dari
drawer (mode layar sempit) tidak menyorot menu yang sama di sidebar (mode lebar). Ketiganya
— berikut `'Master Data Keuangan'` — kini terdaftar, dan ada uji yang menjaganya.

Dua impor mati pada `dana_talangan_screen.dart` (`pemilih_akun.dart`,
`pemilih_anggaran.dart`) juga dibuang: dokumen dana talangan mewarisi anggaran dari uang
muka yang ditalanginya, jadi ia memang tidak pernah memilih anggaran atau akun sendiri.

---

## 8. Hasil uji

| Uji | Hasil |
|---|---|
| `TesMasterKeuangan` (server) | 59 lulus, 0 gagal, sisa data uji 0 |
| `flutter analyze` | 0 error |
| `flutter test` | **241 lulus** (sebelumnya 236) |

Cakupan harness: opsi keenam tipe, penolakan tipe asing pada ketiga aksi, nama wajib,
siklus tambah→ubah→hapus untuk keenam tipe, penanda `akunLengkap` sebelum dan sesudah
dilengkapi, penjaga hapus sebelum dan sesudah dokumennya hilang, bendera
`menggunakanAnggaran`, `satuanKerjaId` pulang-pergi, laporan hak akses, dan pencarian.

---

## 9. Yang masih harus dikerjakan orang

Administrator tetap perlu **mengisi `jenis_reimbursement` dan `jenis_pengeluaran` berikut
akunnya** sebelum modul Reimbursement Pegawai dapat dipakai. Layar ini menyediakan
tempatnya; isinya bukan sesuatu yang boleh ditebak dari sini.
