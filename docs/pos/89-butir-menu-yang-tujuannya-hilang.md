# Butir menu yang tujuannya hilang

Batch lanjutan sesudah doc 88.

---

## 1. Kanal keenam: tujuan butir menu

Menu tersimpan di `public.menu`, tetapi salinan luringnya ada di
`ais/common/MenuSnapshotData.java` — 1.216 butir dengan format
`id|parent|urut|label|url|icon|grup`.

Kolom `url` memuat dua jenis tujuan, dan **keduanya baru dicari ketika butirnya diklik**:

| Bentuk | Contoh | Tujuannya |
|---|---|---|
| jalur halaman | `/pages/master/pegawai.zul` | `webapp/WEB-INF/z/x/y/pages/master/pegawai.zul` |
| nama kelas laporan | `ais.action.report.format1.sekolah.LaporanRaporSiswa` | berkas `.java`-nya |

[alat/menu-tujuan-hilang.py](alat/menu-tujuan-hilang.py) memeriksa keduanya terhadap berkas
di pohon kerja — bukan pohon kelas, dengan alasan yang sama seperti doc 88.

```
butir menu    : 1216
punya tujuan  : 1049
tujuan hilang : 39
```

## 2. Snapshot ini BUKAN arsip pasif

Yang mengubah bobot temuannya: `MenuHelper.ensureMenusDariSnapshot()` menjalankan snapshot
ini **tiap kali aplikasi start**, dan meng-`INSERT` baris menu yang belum ada di
`public.menu`. Jadi butir yang tujuannya hilang bukan sekadar sisa lama di basis data — ia
**dipulihkan kembali setiap startup**.

Baris yang sudah ada tidak pernah ditimpa (supaya perubahan label/urutan oleh admin
terjaga). Karena itu memperbaiki snapshot berlaku untuk pemulihan dan instalasi berikutnya,
**bukan** untuk baris yang terlanjur ada — dan itu perlu disebut supaya tidak dikira
perbaikannya langsung terasa di sistem yang sudah berjalan.

## 3. Dua diperbaiki (r83269)

| Butir | Menunjuk | Sebenarnya |
|---|---|---|
| A-3.1.1 Profil Mahasiswa dan Lulusan | `…sapto.LaporanProfileMahasiswaDanLulusan` | `…LaporanProfileMahasiswaDanLulusan_A_3_1_1` |
| A-3.1.5 Profil Mahasiswa | `…sapto.LaporanProfileMahasiswa` | `…LaporanProfileMahasiswa_A_3_1_5` |

Kelas tanpa akhiran tidak pernah ada. Yang meyakinkan: **kode borangnya tertulis di label
butir menunya sendiri** — "A-3.1.1", "A-3.1.5" — dan cocok persis dengan akhiran nama
kelasnya. Tidak ada tebakan di sini; jawabannya ada di baris yang sama.

Keduanya menu SAPTO (akreditasi), di luar cabang SIRS, jadi terlihat pengguna.

## 4. Tiga puluh satu butir SIRS: rusak, tetapi sedang tersembunyi

Tiga puluh satu butir menunjuk `/pages/master/sirs/...` padahal halamannya ada di jalur
umum:

```
/pages/master/sirs/akunting/grup_transaksi.zul  ->  /pages/master/akunting/grup_transaksi.zul
/pages/master/sirs/akunting/posting_transaksi_pegawai.zul -> /pages/master/payroll/...
/pages/master/sirs/konfigurasi.zul              ->  /pages/master/konfigurasi.zul
```

Menu SIRS dibangun seolah modul itu punya salinan sendiri dari layar akuntansi; salinan itu
tidak pernah ada.

**Tetapi butir-butir itu tidak terlihat pengguna sekarang.** `sembunyikanMenuSirsSementara()`
berjalan tiap startup dan menyetel induk cabang SIRS `aktif=false`, sehingga seluruh
cabangnya tersembunyi. Status tiap anak sengaja tidak diubah supaya susunannya kembali utuh
bila modulnya diaktifkan lagi.

Jadi ini bukan kerusakan yang menimpa pengguna hari ini, melainkan **ranjau bagi siapa pun
yang menyalakan SIRS nanti**: begitu induknya `aktif=true`, tiga puluh satu butir langsung
menuju halaman galat. Dokumen `MenuHelper` menjelaskan mengaktifkan kembali hanya soal
mengubah satu nilai — jadi jaraknya memang sedekat itu.

Tidak diperbaiki sepihak: mengarahkan menu SIRS ke layar akuntansi umum adalah keputusan
tentang bagaimana modul itu seharusnya bekerja, bukan pembetulan jalur.

## 5. Enam tujuan yang benar-benar hilang

| Halaman | Cabang |
|---|---|
| `daftarulang_mahasiswa_lama_beasiswa.zul` | akademik — **terlihat** |
| `daftarulang_mahasiswa_lama_pengembalian_beasiswa.zul` | akademik — **terlihat** |
| `sirs/akunting/posting_transaksi_deposit.zul` | SIRS (tersembunyi) |
| `sirs/akunting/posting_transaksi_kasir.zul` | SIRS (tersembunyi) |
| `sirs/akunting/posting_transaksi_penerimaan_order.zul` | SIRS (tersembunyi) |
| `sirs/komunitas.zul` | SIRS (tersembunyi) |

Dua yang pertama paling mendesak: keduanya di cabang akademik yang aktif, dan halamannya
tidak ada di mana pun di `webapp`. Membuatnya kembali menuntut tahu apa yang seharusnya
dikerjakan layar itu — pekerjaan pemilik sistem, bukan penebak nama berkas.

## 6. Satu lolos-palsu yang tertangkap lebih dulu

Jalan pertama alat ini melaporkan **67** tujuan hilang. Dua puluh delapan di antaranya
palsu: URL menu boleh membawa query string —
`/pages/master/grup_pertemuan.zul?jenis=Konsultasi+Umum` — dan pemeriksanya memperlakukan
seluruh teks itu sebagai nama berkas.

Ditemukan bukan dengan analisis, melainkan dengan membaca daftarnya: tiga baris pertama
memuat `?jenis=` yang jelas bukan bagian nama berkas. Itu alasan mencetak temuan apa adanya
sebelum menghitungnya — angka 67 tanpa daftarnya akan langsung dipercaya.
