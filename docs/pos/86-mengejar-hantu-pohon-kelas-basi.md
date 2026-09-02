# Empat halaman lagi, dan hantu dari pohon kelas yang basi

Batch lanjutan sesudah doc 85, mengerjakan daftar 18 galat di 11 berkas.

---

## 1. Peringatan sendiri yang langsung menagih

Doc 84 §6 menutup dengan catatan: pemeriksaan scriptlet memakai pohon kelas hasil kompilasi
terakhir, dan **pohon yang basi memberi hasil yang basi**. Catatan itu menagih pada batch
berikutnya juga.

Dua berkas dalam daftar melaporkan:

```
import ais.common.newui.sekolah.NewUiSiswaAsuhanController;   error: cannot find symbol
```

Kelas itu **ada**, persis di paket yang diimpor. Yang tidak ada adalah kelasnya di pohon
kelas yang dipakai memeriksa — pohon itu dibangun beberapa jam sebelumnya, sebelum kelas
tersebut masuk. Hal yang sama berlaku untuk `DashboardStatistikKunjunganPengguna` (ada) dan
`ConstantValues.simpleList` (ada, generik).

Seandainya daftar itu dikerjakan apa adanya, yang terjadi bukan perbaikan melainkan
kerusakan: kode yang benar "diperbaiki" agar cocok dengan pohon kelas yang usang.

Karena itu pohon kelasnya dibangun ulang lebih dulu, dan gerbangnya dijalankan ulang.
Urutan ini bukan saran, melainkan syarat: **kompilasi `.java` dulu, baru scriptlet JSP.**

## 2. Empat halaman diperbaiki (r83213)

Keempatnya cacat yang tidak bergantung pada kesegaran pohon kelas — bisa dipastikan dari
sumber saja.

### Tiga rujukan paket yang basi

Halaman generator CRUD menunjuk kelas model di paket yang sudah ditinggalkannya:

| Halaman | Ditunjuk | Sebenarnya |
|---|---|---|
| `pagesmasterkonfigurasilkpzul` | `ais.database.model.KonfigurasiSK` | `…model.employ.KonfigurasiSK` |
| `pagesmasterkonfigurasisekolahzul` | `ais.database.model.Sekolah` | `…model.sekolah.Sekolah` |
| `pagesmasterslipgajipegawaibulananzul` | `ais.database.model.PembayaranGaji` | `…model.payroll.PembayaranGaji` |

Menariknya, galatnya menunjuk `DynamicJspCrudGenerator.generate` — seolah metodenya yang
hilang. Tanda tangan `generate(Class)` ternyata masih ada; yang tidak ditemukan kompilator
adalah **argumennya**. Membaca pesan galat apa adanya akan menuntun ke berkas yang salah.

### Satu `ThreadLocal` dipakai langsung

`o/kursus/content/semua_berita.jsp` memakai `Common.monthFormat21.format(...)`, padahal
field itu `ThreadLocal<SimpleDateFormat>`. Sama persis dengan `berita.jsp` di r83174 — cacat
yang sama, di halaman bersebelahan, luput bersamaan.

## 3. Tiga halaman yang TIDAK diperbaiki

Tiga halaman generator CRUD lain menunjuk kelas yang **tidak ada di mana pun** lagi:

| Halaman | Kelas yang hilang |
|---|---|
| `pagesmasterkknkknutkmhszul` | `ais.database.model.kkn.KknMahasiswa` |
| `pagesmasterkonfigurasidetailzul` | `ais.database.model.KonfigurasiDetail` |
| `pagesmasterpklpklutkmhszul` | `ais.database.model.pkl.PklMahasiswa` |

Pencarian pengganti tidak memberi padanan satu-lawan-satu: yang ada `KelompokKkn`,
`KknPunyaPersyaratan`, `MahasiswaDapatPkl`, `PengecualianPklMahasiswa` — kelas serumpun,
bukan pengganti. Untuk `KonfigurasiDetail` tidak ada apa pun yang mendekati.

Menebak salah satunya berarti menyambungkan halaman ke entitas yang salah, dan hasilnya
akan tampak bekerja. Ketiga halaman ini kemungkinan besar sisa modul yang sudah dibongkar —
tetapi memutuskannya milik pemilik sistem, bukan pembaca pesan galat.

## 4. Dua cacat lagi pada alatnya sendiri

Keduanya membuat log tidak dapat diurai, dan keduanya khas PowerShell.

**Koma mengikat lebih kuat daripada plus.** Dalam daftar berkoma, `'-cp', '"' + $x + '"'`
tidak menghasilkan dua elemen melainkan **empat** — tanda kutip menjadi elemennya sendiri.
Berkas argumen javac lalu memuat `-cp` diikuti baris berisi hanya `"`, dan javac menolak
dengan `invalid flag`.

**stderr javac dibungkus dan dipotong.** Baik `*>` maupun `2>&1 | Out-File` melewatkan
keluaran javac melalui pemformat record galat PowerShell, yang memotong baris di lebar
konsol. Jalur berkas terbelah dua baris, sehingga nama berkas dan nomor barisnya tidak
dapat dipasangkan lagi. Diganti redirection `cmd`, yang menulis keluaran javac apa adanya.

Yang kedua sempat tertutupi karena `Select-String` tetap menghitung dengan benar — laporan
di layar akurat sementara lognya tidak berguna bagi alat mana pun. Cacat yang hanya terasa
oleh pemakai berikutnya, dan ini kedua kalinya pola itu muncul (doc 85 §4).
