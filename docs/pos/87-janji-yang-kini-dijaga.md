# 87 — Janji dalam pesan galat, kini dijaga

Tanggal: 2026-09-02

Dok. 86 ditutup dengan satu kalimat:

> **Pesan galat adalah janji.** Kalimat "simpan ulang dengan persetujuan harga
> modal tinggi" adalah kontrak antara server dan pengguna, sama mengikatnya
> seperti kontrak antarkode — dan kontrak itu tidak dikompilasi, tidak diuji,
> dan tidak dijaga siapa pun.

`izin_harga_modal_tinggi` ditemukan secara kebetulan, sebagai satu baris di
dalam daftar 26 sisa penyaringan. Pertanyaan yang benar sesudahnya bukan "sudah
diperbaiki?" melainkan **"ada berapa janji lain yang tidak bisa ditepati?"**

Jawabannya: nol — dan sekarang ada yang menjaganya tetap nol.

## 1. Himpunannya ternyata kecil

Dok. 84 dan 86 sama-sama berakhir **tanpa** penjaga, karena pengukurannya
menyisakan ratusan dan puluhan kandidat yang bercampur panggilan pihak ketiga
yang sah. Gerbang di atas dasar seperti itu menuduh yang tidak bersalah.

Kali ini bentuknya dipersempit ke satu hal yang sangat spesifik: **penanda
boolean dari request yang, bila tidak dikirim, menyebabkan PENOLAKAN.** Itulah
bentuk pintu darurat.

Cakupannya diukur, bukan ditebak:

| | |
|---|---|
| 222 | pemanggilan `optBoolean("k", false)` di pohon servlet |
| 11 | dinegasikan langsung di dalam syarat |
| 41 | melewati variabel lokal lebih dulu |
| **3** | benar-benar menjaga sebuah penolakan |

Tiga. Setiap anggotanya dapat — dan sudah — diperiksa tangan satu per satu:

| Pintu | Berkas | Keadaan |
|---|---|---|
| `izin_harga_modal_tinggi` | `KantinHelper` | buntu sampai dok. 86; kini dapat dibuka layar Produk |
| `pengiriman_pending` | `KantinHelper` | dapat dibuka |
| `termasuk_nonaktif` | `HargaGrosirApiHelper` | dapat dibuka |

Bentuk "lewat variabel lokal" sengaja ikut dijaring meski akhirnya tidak
menambah satu pun. Itu bukan pekerjaan sia-sia: tanpa mengukurnya, angka 3
hanyalah dugaan. Sekarang ia hasil pemeriksaan.

## 2. Dibuktikan bisa gagal sebelum dipercaya

Penjaga yang belum pernah terbukti menyala tidak layak dipakai sebagai bukti.
Kontrol negatifnya memakai cacat aslinya, bukan cacat buatan: baris pengirim
`izin_harga_modal_tinggi` dicabut dari `produk_screen.dart`, lalu:

```
== Pintu yang TIDAK DAPAT dibuka klien mana pun ==
   - izin_harga_modal_tinggi   (KantinHelper.java)

1 PINTU BUNTU -- pesannya menjanjikan yang mustahil          rc=1
```

Sisipan dikembalikan, repositori diperiksa bersih, alatnya kembali `rc=0`.

Alat ini juga **menyebut pesan yang dilihat pengguna** saat melapor, bukan
sekadar nama penandanya. Yang perlu dibaca orang yang memperbaikinya bukan
"kunci X yatim", melainkan kalimat yang sedang dijanjikan sistem kepada
penggunanya.

## 3. Batas yang dinyatakan

Tidak dijaring: penanda yang dirakit dinamis, penanda yang dibaca di berkas
berbeda dari tempat penolakannya, dan penolakan yang berjarak lebih dari ~1.200
karakter dari negasinya. Batas-batas itu ditulis di kepala berkas alatnya,
bukan hanya di sini.

Alat ini juga tidak menyentuh bentuk pintu yang lain: penolakan yang dibuka
oleh nilai non-boolean (mis. sebuah alasan teks yang wajib diisi), atau oleh
peran pengguna. Keduanya kelas tersendiri.

## 4. Yang dipelajari

**Ukuran himpunan menentukan apakah temuan boleh menjadi gerbang.** Dua dokumen
sebelumnya menemukan kelas cacat yang nyata dan tetap tidak membuat penjaga,
karena pengukurannya tidak dapat dipercaya. Perbedaannya bukan pada seberapa
serius cacatnya, melainkan pada apakah setiap anggota himpunannya masih dapat
diperiksa manusia. Tiga bisa; dua ratus tidak.

**Mempersempit bentuk lebih baik daripada memperbanyak saringan.** Dok. 86
memerlukan lima saringan berlapis dan tetap menyisakan 26 kandidat bercampur.
Di sini satu definisi yang tajam — "penanda yang bila absen menyebabkan
penolakan" — langsung menghasilkan himpunan yang bersih.
