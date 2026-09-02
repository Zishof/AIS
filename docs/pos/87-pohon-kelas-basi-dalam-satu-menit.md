# Pohon kelas basi dalam satu menit

Batch lanjutan sesudah doc 86, mengerjakan sisa 10 galat di 8 berkas.

---

## 1. Hantunya bukan kebetulan, melainkan keadaan tetap

Doc 86 menemukan sebagian galat adalah hantu dari pohon kelas basi, lalu membangun ulang
pohonnya. Batch ini dimulai dengan daftar hasil pohon baru itu — dan **dua entri teratasnya
hantu lagi**:

| Galat | Kenyataannya |
|---|---|
| `package ais.common.newui.dashboard does not exist` | paketnya **ada**, berisi `NewUiAktifitasPustakawanController.java` |
| konstanta `MODE_DASBOR_KEMAHASISWAAN_SAYA` tidak ada | konstantanya **ada** di `NewUiLayarLainnyaController` |

Cap waktunya menjelaskan semuanya:

```
pohon kelas dibangun : 18:49:26
NewUiAktifitasPustakawanController.java : 18:50:26
NewUiLayarLainnyaController.java        : 18:50:28
```

**Satu menit.** Sesi lain menulis kedua berkas itu tepat sesudah pohonnya selesai.

Jadi ini bukan kelalaian sekali jalan yang bisa diperbaiki dengan "ingat bangun ulang dulu".
Di repositori yang disunting beberapa sesi sekaligus, pohon kelas **basi hampir seketika**.
Saat dokumen ini ditulis, 36 berkas `.java` sudah lebih baru daripada pohon yang baru
dibangun sejam sebelumnya.

## 2. Alatnya sekarang mengukur kesegarannya sendiri

[alat/jsp-scriptlet.ps1](alat/jsp-scriptlet.ps1) membandingkan cap waktu `.class` terbaru
dengan `.java` terbaru, lalu memperingatkan sebelum satu pun galat dilaporkan:

```
PERINGATAN: 36 berkas .java lebih baru daripada pohon kelas.
  Galat "cannot find symbol" atas kelas/metode BARU kemungkinan hantu, bukan cacat.
  Bangun ulang dgn alat/kompilasi-penuh.sh sebelum mempercayai daftarnya.
    PenggantianKasKecil.java  19.28.33
    KasKecil.java  19.28.09
```

Sengaja **memperingatkan, bukan menolak**. Menolak berjalan saat pohon basi akan membuat
alat ini praktis tidak pernah bisa dipakai di sini — dan alat yang tidak bisa dipakai tidak
menemukan apa pun.

Yang berbahaya bukan galat palsunya. Yang berbahaya adalah godaan memperbaikinya: mengubah
kode yang benar agar cocok dengan pohon yang usang. Dua kali dalam dua batch saya berhenti
tepat sebelum melakukannya, dan dua-duanya karena kebetulan memeriksa sumbernya lebih dulu.

## 3. Satu perbaikan nyata (r83239)

`o/ux/content/elearning/mahasiswa/info_elearning.jsp` memanggil

```java
tbmuser.getMahasiswa().rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek, false)
```

sebagai metode instance pada `Mahasiswa`. Metode itu sudah pindah: kini `static` di
`KrsDetailHelper`, dengan `krsMahasiswa` sebagai parameter tambahan. Yang tersisa di
`Mahasiswa.java` hanya rujukan JavaDoc yang menunjuk ke tempat barunya — jejak perpindahan
yang tidak ikut memperbaiki pemanggilnya.

Dialihkan ke overload **enam-argumen**, bukan yang dua-argumen. Yang dua-argumen mengambil
mahasiswanya dari `krsMahasiswa.getMahasiswa()`, sedangkan halaman ini memakai
`tbmuser.getMahasiswa()`; keduanya belum tentu orang yang sama bila halaman ini suatu saat
dibuka untuk KRS milik orang lain. Overload enam-argumen mempertahankan perilaku aslinya
persis.

## 4. Dua yang gugur, dua yang diserahkan

**Gugur** — `z/x/y/common/newtemplate_biodata_pegawai.jsp` (`Report.generateFileReport` tidak
cocok) **sudah tidak ada**: berkasnya dihapus sesi lain di tengah batch ini. Daftar pekerjaan
di repositori seramai ini punya masa berlaku.

**Diserahkan ke pemilik sistem** — `o/ux/dashboard/service/kunjungan_pengguna.jsp` memanggil
`DashboardStatistikKunjunganPengguna.generateDataset(...)` secara statis dengan tujuh
argumen. Kelas itu komponen ZK (konstruktor dan penangan event); tidak ada `generateDataset`
di sana maupun di mana pun. Memaparkannya kembali berarti mengeluarkan logika dari dalam
komponen — perubahan rancangan, bukan perbaikan mekanis.

Ditambah tiga halaman generator CRUD dari doc 86 yang kelas modelnya lenyap, ada **empat**
halaman yang menunggu keputusan, bukan menunggu ketikan.

## 5. Yang berubah dari cara kerja

Tiga batch berturut-turut memakai pola yang sama dan tiga-tiganya berbuah: jalankan
pemeriksa, lalu **periksa tiap temuannya terhadap sumber sebelum menyentuh apa pun**.
Langkah kedua itu yang menangkap tujuh hantu sejauh ini.

Pemeriksa memberi daftar, bukan vonis. Perbedaannya kelihatan sepele sampai daftarnya cukup
panjang untuk menggoda orang mengerjakannya tanpa membaca.
