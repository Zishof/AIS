# Kompilasi penuh sebagai pemeriksaan, dan dua JavaDoc yang dikembalikan

Batch lanjutan sesudah doc 76.

---

## 1. Seluruh pohon dikompilasi — `Pegawai.java` memang satu-satunya yang patah

Doc 76 menemukan berkas yang tidak dapat dikompilasi **sudah ter-commit di HEAD**, lolos
karena tidak ada langkah kompilasi di antara suntingan dan commit. Pertanyaan yang wajar
menyusul: apakah itu satu-satunya?

Seluruh pohon dikompilasi untuk menjawabnya, bukan disimpulkan dari dugaan.

| | |
|---|---|
| berkas sumber | **7.430** |
| kelas dihasilkan | **40.949** |
| galat | **0** |
| peringatan | **0** |

```
javac -source 1.7 -target 1.7 -encoding UTF-8 -J-Xmx2g \
      -cp webapp/WEB-INF/lib/* -d <keluaran> @<daftar 7430 berkas>
```

Jadi ya — sesudah perbaikan r83037, `Pegawai.java` adalah satu-satunya kerusakan build.
Angkanya juga berguna sebagai garis dasar baru: doc 21 mencatat 7.058 berkas → 40.182
kelas, jadi pohon ini bertambah sekitar 370 berkas sejak saat itu.

**Ini yang layak dijadikan kebiasaan**, bukan temuan sekali pakai. Kompilasi penuh
memakan waktu belasan menit dan berjalan tanpa pengawasan; itu harga yang murah
dibandingkan menemukan HEAD tidak bisa dibangun pada saat rilis.

## 2. Dua blok JavaDoc dikembalikan ke metode yang dijelaskannya (r83057)

Dua kasus PINDAH dari doc 75 §3 dikerjakan:

| Blok | Sebelumnya menempel di atas | Tuan sebenarnya |
|---|---|---|
| "Fase 1: validasi stok server-side dengan row lock" (53 baris, ber-`@param`/`@return`) | JavaDoc `cekProdukKadaluarsa` | `validasiStokCukupDenganLock` |
| "session native berbasis ThreadLocal" berikut COOKBOOK penutupan sesi (48 baris) | JavaDoc `transaksiSedangAktif` | `currentNativeSession` |

Yang menguatkan diagnosis: **pada kedua kasus, metode tuannya berdiri tanpa dokumentasi
sama sekali.** `currentNativeSession()` hanya punya komentar di dalam badannya;
`validasiStokCukupDenganLock` sama sekali kosong. Artinya pass otomatis itu tidak
menambahkan dokumentasi baru di atas yang lama — ia **menggeser** dokumentasi yang sudah
ada, meninggalkan dua metode inti tanpa penjelasan sekaligus menyembunyikan penjelasannya
di tempat yang tidak dibaca siapa pun.

Bloknya dipindahkan apa adanya; tidak satu kata pun diubah.

## 3. Alat kedua: menebak tuan sebuah blok yatim

[alat/javadoc-cari-tuan.py](alat/javadoc-cari-tuan.py) melengkapi `javadoc-yatim.py`.
Alat pertama menjawab "blok ini tidak menempel ke apa pun"; alat kedua menjawab
"seharusnya menempel ke mana".

Cara menebaknya memakai bukti yang sama yang menguatkan kedua perbaikan di atas: blok
yatim biasanya menyebut nama tuannya di teksnya sendiri (`{@link #namaMethod}`), dan
tuannya kini berdiri **tanpa dokumentasi**. Kandidat yang tidak memenuhi kedua syarat itu
tidak ditebak, hanya dihitung.

```
berkas dianalisis : 91
blok yatim        : 129
punya calon tuan  : 13
```

Catatan penerapan: versi pertama alat ini memindai seluruh 7.430 berkas dan tidak selesai
dalam waktu wajar. Saringan murah dipasang lebih dulu — analisis penuh hanya untuk berkas
yang memang memuat blok beruntun, sembilan puluh satu berkas, bukan tujuh ribu.

## 4. Catatan operasional

Commit r83057 sempat gagal: `svn: E730060: Can't connect to host '38.47.178.34'`. Percobaan
kedua beberapa saat kemudian berhasil tanpa perubahan apa pun. Layak diingat saat sebuah
commit tampak gagal — periksa dulu apakah servernya yang tidak merespons, sebelum menduga
working copy-nya bermasalah.
