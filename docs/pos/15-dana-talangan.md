# 15 — Dana Talangan

Dana talangan menjembatani satu **uang muka**: uang mukanya sudah disetujui dan
transfernya sudah benar-benar cair, tetapi dananya perlu ditalangi lebih dulu dari sumber
lain. Dipindahkan dari layar ZK `ais.action.master.akunting.DanaTalanganAction`.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/DanaTalanganApiHelper.java` |
| Kunci menu | `dana_talangan` (fail-closed, hak per-aksi) |
| Muara DPC | `TransferDpcUtil` — kolom `dana_talangan` |
| Dasbor & cetak | `KeuanganApiHelper`, modul `dana_talangan` |
| Posting jurnal | `PostingDanaTalanganAction.postingSemua/batalkanPostingSemua` |
| Layar | `apps/ebisnis/lib/screens/dana_talangan_screen.dart` |

---

## 1. Dua aturan yang dipertahankan apa adanya

**Uang muka yang boleh ditalangi hanya yang transfernya sudah TEREALISASI.** Mengikuti
`AmbilDataUangMukaBanbox`: aktif, berstatus Disetujui, punya penyetuju, dan pengajuan
transfernya sudah direalisasikan — lewat jalur transfer
(`prosesTransfer.realisasikanOleh` terisi) atau jalur transitori
(`transitoriData.transfer` terisi). Menalangi uang muka yang uangnya belum cair sama saja
mencatat utang yang belum ada.

**Sumber Dana Talangan wajib saat MENYETUJUI, bukan saat menyimpan.** Di layar ZK
pemeriksaannya memang hanya berjalan pada jalur persetujuan (`persetujuan && setujui`):
pengajuan boleh disimpan dulu sebagai draf tanpa menentukan sumbernya. Sumber itu
menentukan akun kredit jurnalnya, jadi baru mengikat ketika dokumen benar-benar disetujui.

Urutan validasinya: **Uang Muka → Judul → Nilai → (Sumber Dana, hanya saat menyetujui).**

---

## 2. Aksi

| Aksi | Guna |
|---|---|
| `dana_talangan_daftar` | daftar + status DPC + hak tombol |
| `dana_talangan_opsi` | sumber dana, satuan kerja, daftar status |
| `dana_talangan_cari_uang_muka` | pemilih uang muka yang transfernya terealisasi |
| `dana_talangan_simpan` | tambah/ubah |
| `dana_talangan_setujui` / `_tolak` | persetujuan |
| `dana_talangan_hapus` | hapus, dengan penjaga |
| `dana_talangan_ajukan_transfer` | muara DPC |

Pemilih uang muka juga mengembalikan **pilihan yang tersimpan** pada dokumen yang sedang
disunting meski keadaannya sudah berubah — supaya formulir lama tidak kehilangan isinya.

Penghapusan ditolak bila dokumen sudah dijurnal, sudah disetujui, atau sudah masuk DPC.

---

## 3. Jurnalnya

| Sisi | Akun |
|---|---|
| Debet | `uangMuka.jenisUangMuka.akun` — akun penerima uang muka yang ditalangi |
| Kredit | `jenisUangMuka.akunKelebihan` — akun kelebihan pada sumber dana talangannya |

Jenis riwayat postingnya `PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA`, sama seperti ZK.

Barisnya muncul di dasbor Draft Jurnal dengan nama **"Dana Talangan"** dan kunci hak
`dana_talangan` — lihat [06-posting-jurnal-keuangan.md](06-posting-jurnal-keuangan.md).

---

## 4. Layarnya

Berbeda dengan Kas Kecil dan Penggantian Kas Kecil, dokumen ini **bernilai tunggal**:
tidak ada mesin rincian, tidak ada anggaran per baris. Yang ada: pemilih uang muka,
kolom Nilai, dan dropdown Sumber Dana Talangan yang keterangannya menyebut bahwa ia baru
wajib sebelum disetujui.

Lokal-dulu, hapus lunak, dasbor, cetak, dan tombol Ajukan Transfer mengikuti pola yang
sama dengan modul Keuangan lainnya — lihat [02-grup-menu-keuangan.md](02-grup-menu-keuangan.md).

---

## 5. Cacat yang ikut ditutup

**Kunci antrean luring bertabrakan.** Layar Kas Kecil dan Penggantian Kas Kecil memakai
`'kas_besar:<id>'` sebagai kunci antrean saat MENYUNTING — warisan copy-paste dari waktu
layar Kas Kecil diturunkan dari Kas Besar. Akibatnya suntingan luring dokumen Kas Kecil
ber-id sama dengan dokumen Kas Besar menempati slot antrean yang sama dan saling
menimpa. Ketiga layar kini memakai kuncinya sendiri, dan ada uji yang menguncinya.

**Uji irisan grup menu melempar RangeError.** `sidebar_keuangan_test.dart` mengiris
`app_shell.dart` dengan pola ber-`\n`, padahal git di Windows meng-checkout berkas sebagai
CRLF (`core.autocrlf=true`). Polanya tidak pernah cocok, `indexOf` mengembalikan −1, dan
`substring(-1, …)` melempar RangeError — sehingga kegagalannya tampil sebagai galat
rentang, bukan sebagai pernyataan yang menjelaskan apa yang salah. Akhiran baris kini
dinormalkan dulu, dan kedua batas irisannya diperiksa dengan `expect`.

---

## 6. Hasil uji

`TesDanaTalangan` — **20 lulus, 0 gagal**, sisa data uji 0. Mencakup: uang muka yang
transfernya terealisasi muncul dan yang belum tidak muncul, keempat validasi, draf
tersimpan tanpa sumber dana lalu persetujuannya ditolak sampai sumbernya dipilih, muara
DPC, dasbor 4 KPI, jurnal seimbang 400.000 beserta pembatalannya, dan penjaga hapus.

Harness merangkai sendiri satu baris `akunting.proses_transfer` ber-`realisasikan_oleh`
untuk memenuhi syarat realisasi, lalu menghapusnya di akhir bersama data uji lainnya.

Sisi klien: `flutter analyze` 0 error, `flutter test` **227 lulus**.
