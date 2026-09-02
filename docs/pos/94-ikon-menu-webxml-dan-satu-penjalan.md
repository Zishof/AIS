# Ikon menu, `web.xml`, dan satu penjalan untuk semua

Batch lanjutan sesudah doc 93.

---

## 1. Ikon menu: dua berkas hilang, bukan tiga puluh enam

Kolom ikon pada `MenuSnapshotData` diperiksa untuk pertama kalinya — sebelumnya diabaikan.
Jalan pertama melaporkan **36 ikon hilang**, dan daftarnya langsung membantahnya sendiri:

```
far fa-calendar-alt      1 butir  mis. Jadwal
fas fa-bell              1 butir  mis. Notifikasi Saya
fas fa-cash-register     1 butir  mis. Kasir (POS)
```

Itu **kelas CSS Font Awesome**, bukan jalur berkas. Kolom ikon menerima dua bentuk sekaligus,
dan pemeriksanya memperlakukan semuanya sebagai jalur.

Sesudah dipisahkan:

| | |
|---|---|
| ikon berupa kelas CSS | 44 |
| ikon berupa jalur berkas | 916 |
| **jalur yang berkasnya hilang** | **2** |

| Berkas | Butir menu |
|---|---|
| `/img/Service Creation Device.png` | Satuan Item, Konversi Satuan Item |
| `/img/recycle-icon.png` | Pasien Pindah Kelas / Tempat Tidur (cabang SIRS, tersembunyi) |

Tidak ada berkas bernama mirip di `/img` — `Creation Tools.png` ada, tetapi itu ikon lain.
Menggantinya adalah pilihan tampilan, bukan pembetulan rujukan, jadi tidak dilakukan sepihak.
Akibatnya kecil: gambar rusak pada dua butir menu yang aktif.

## 2. `web.xml`: bersih

Seluruh `servlet-class`, `filter-class`, dan `listener-class` diperiksa terhadap pohon
sumber:

```
kelas unik di web.xml : 173  (ais.*: 162, pihak ketiga: 11)
tidak ada sumbernya   : 0
```

Seratus enam puluh dua dari seratus enam puluh dua ada. Hasil negatif, dan dicatat supaya
tidak diperiksa ulang.

## 3. Sembilan alat, tiga cara pemanggilan, nol yang diingat

Masalah yang lebih besar daripada kedua temuan di atas: direktori `alat/` kini berisi
sembilan pemeriksa dengan tiga cara pemanggilan berbeda (`sh`, `python`, `powershell`), dan
sebagian menuntut argumen. Tidak ada seorang pun akan mengingat semuanya — dan **pemeriksa
yang tidak dijalankan tidak menemukan apa pun**.

[alat/periksa-semua.sh](alat/periksa-semua.sh) menjalankan seluruh pemeriksaan **murah**
sekaligus lalu meringkasnya:

```
===== RINGKASAN
  TEMUAN  rujukan kelas ZUL
  TEMUAN  tujuan butir menu
  TEMUAN  include runtime
  BERSIH  subreport jrxml
  BERSIH  terjemahan JSP
```

Tiga "TEMUAN" itu adalah pekerjaan terbuka yang sudah didokumentasikan (13 rujukan kelas
menggantung di doc 88, 37 tujuan menu di doc 89, satu include di doc 90) — jadi keluaran ini
memang sesuai keadaan, bukan penemuan baru.

Yang **tidak** ikut dijalankan disebut apa adanya di kepala skripnya, berikut alasannya:
kompilasi penuh terlalu mahal untuk dijalankan sambil menunggu, kompilasi berkas-berubah
dipakai sebelum commit, dan gerbang scriptlet JSP menuntut pohon kelas yang segar (doc 87).

## 4. Dua cacat kecil pada penjalan itu sendiri

`echo "\nRingkasan"` mencetak `\n` **harfiah** di `sh` POSIX — `echo` tidak menafsirkan
urutan escape, `printf` yang menafsirkannya. Terlihat di jalan pertama.

Dan sekali lagi: menyunting skrip lewat heredoc menelan backslash, sehingga pola pencarian
`'\n'` tidak pernah cocok dengan isi berkas. Ini kali keempat jebakan yang sama muncul
dalam rangkaian batch ini. Jalan keluarnya menyunting berdasarkan **indeks baris**, bukan
pencocokan teks berescape.

## 5. Yang tersisa dari seam ini

Enam kanal rujukan-runtime sudah diperiksa seluruhnya: kelas ZUL, tujuan menu, include
runtime, subreport, ikon menu, dan `web.xml`. Yang masih terbuka semuanya menunggu
**keputusan**, bukan pemeriksaan lebih lanjut:

- 13 halaman ZUL yang kelasnya lenyap (8 di antaranya masih dirujuk)
- 31 butir menu SIRS yang menunjuk salinan layar yang tak pernah ada
- 6 tujuan menu yang halamannya benar-benar hilang
- 6 templat laporan tanpa sumber
- 2 ikon hilang
- 1 include yang dua kandidat tujuannya berbeda isi

Tidak satu pun dapat diselesaikan dengan menebak, dan itu sebabnya semuanya masih di sini.
