# Gerbang kompilasi yang hampir berbohong

Batch lanjutan sesudah doc 81. Satu alat baru, dan satu cacat pada alat itu yang nyaris
membuatnya lebih berbahaya daripada tidak ada.

---

## 1. Kenapa perlu alat kedua

Doc 81 membuat [alat/kompilasi-penuh.sh](alat/kompilasi-penuh.sh) dan alat itu langsung
menemukan HEAD patah. Tetapi ia memakan **belasan menit** untuk 7.400-an berkas, jadi
kenyataannya tidak akan pernah dijalankan sebelum commit. Gerbang yang terlalu mahal untuk
dipakai sama saja dengan tidak ada gerbang.

[alat/kompilasi-berubah.sh](alat/kompilasi-berubah.sh) hanya mengompilasi berkas yang
**berubah** sejak titik waktu tertentu (bawaan: awal hari ini), memakai `-sourcepath`
sehingga dependensinya tetap diambil dari sumber.

```
sejak         : {2026-09-02}
berkas diuji  : 316
kelas         : 21960
galat         : 0
BERSIH
```

Menit-menitan, bukan belasan menit. Dan itu sudah cukup: **kedua kerusakan HEAD hari ini**
— `Pegawai.java` (doc 76) dan `DraftJurnalApiHelper` + `PosApi` (doc 81) — adalah galat di
dalam berkas yang baru saja disunting, jadi keduanya tertangkap di sini.

Daftar berkasnya diambil dari `svn diff --summarize` **ditambah** `svn status`, karena
working copy ini dipakai beberapa sesi sekaligus: yang belum di-commit pun perlu diuji.

## 2. Buktinya benar-benar menangkap, bukan sekadar melaporkan bersih

Gerbang yang tidak pernah gagal tidak membuktikan apa pun. Diuji dengan mengambil versi
`DraftJurnalApiHelper.java` **sebelum** perbaikan r83115 lalu mengompilasinya dengan setelan
yang sama:

```
KODE KELUAR javac=1
galat terdeteksi: 1
...DraftJurnalApiHelper.java:722: error: cannot find symbol
        kunciModulBaris != null && bolehAksi(tbmuser, kunciModulBaris, "create"));
```

Galat historisnya muncul kembali persis. Mekanismenya terbukti.

## 3. Cacat pada alatnya sendiri: "BERSIH" yang palsu

Jalan pertama skrip ini melaporkan:

```
berkas diuji  : 315
galat         : 0
BERSIH
```

**Itu bohong.** `javac -d` menolak direktori keluaran yang belum ada, dan skripnya membuat
`$KERJA` tetapi bukan `$KERJA/kelas`. javac berhenti seketika:

```
javac: directory not found: .../kompilasi-berubah/kelas
Usage: javac <options> <source files>
```

Pesan itu **tidak memuat satu pun baris `error:`**. Penghitung galat membaca nol, syarat
`galat > 0` tidak terpenuhi, dan gerbangnya mengumumkan BERSIH — padahal tidak satu berkas
pun dikompilasi.

Doc 81 menutup dengan kalimat "gerbang yang melaporkan sukses saat gagal lebih buruk
daripada tidak ada gerbang, karena ia menumbuhkan rasa aman". Kalimat itu ditulis untuk
jebakan pipa. Beberapa menit kemudian saya membangun gerbang dengan cacat yang persis sama,
dan yang menemukannya bukan kewaspadaan melainkan kebetulan — percobaan tak terkait yang
memunculkan pesan "directory not found" yang sama.

### Perbaikannya: tiga syarat, bukan satu

Menghitung baris `error:` saja tidak cukup, karena javac bisa gagal **sebelum** sempat
mengompilasi apa pun.

| Syarat | Menangkap |
|---|---|
| `galat > 0` | kesalahan kompilasi biasa |
| `kode keluar javac ≠ 0` **dan** `galat = 0` | javac gagal tanpa galat kompilasi — opsi salah, classpath tak terbaca |
| jumlah kelas = 0 | kompilasinya tidak benar-benar berjalan |

Syarat ketiga yang paling jujur, dan sekarang jumlah kelas ikut dicetak setiap kali:
**21.960** pada jalan yang benar, **0** pada jalan yang berbohong. Angka itu yang membedakan.

Penjagaan yang sama dipasang di `kompilasi-penuh.sh`.

## 4. Yang bisa dipelajari

Ketiga cacat pertama skrip ini — hitungan galat yang menjadi `"0\n0"`, peringatan `tr`, dan
bawaan "kemarin" yang menjaring separuh pohon — semuanya kelihatan pada jalan pertama dan
langsung dibetulkan. Yang **tidak** kelihatan justru yang paling berbahaya: keluarannya
tampak persis seperti keberhasilan.

Sebuah alat pemeriksa perlu diuji pada kasus yang seharusnya GAGAL, bukan hanya pada kasus
yang seharusnya lolos. Bagian 2 dokumen ini ada karena itu.
