# Dua pohon cermin yang terus bergeser

Batch lanjutan sesudah doc 95.

---

## 1. Invarian yang belum pernah diuji

`ais/src/main/java` dan `ais/src/main/src` **memetakan URL repositori yang sama** (`^/src`).
Isinya karena itu wajib identik. Fakta itu sudah tercatat sejak lama, tetapi tidak pernah
diperiksa.

Diperiksa sekarang:

| | |
|---|---|
| berkas `.java` di `java/` | 7.494 |
| berkas `.java` di `src/` | 7.494 |
| hanya ada di salah satu | **0** |
| ada di keduanya tetapi isinya berbeda | **5** |

Strukturnya utuh — tidak ada berkas yang hanya ada di satu sisi. Yang bergeser hanya isinya.

## 2. Kenapa penyimpangan ini berbahaya

Dua hal, dan yang kedua sudah pernah terjadi:

**Menyunting salinan yang basi.** Pekerjaannya benar, lalu hilang begitu salinan satunya
di-`update`. Tidak ada peringatan, tidak ada konflik — berkasnya memang bersih di mata SVN.

**Membaca salinan yang basi.** Doc 87 mencatat dua kelas yang dilaporkan "tidak ada" padahal
ada — waktu itu karena pohon *kelas* yang basi. Dua pohon *sumber* yang berbeda isi
menghasilkan kesalahan yang sama persis, dari arah yang berbeda.

## 3. Empat disamakan, satu sengaja tidak

Lima penyimpangan awal terbagi dua jenis, dan pembedanya menentukan boleh-tidaknya disentuh:

| Berkas | Keadaan | Tindakan |
|---|---|---|
| `OnlineBmt.java`, `KantinHelper.java`, `TopupHelper.java`, `OnlineBmtUtil.java` | dua sisi **bersih**, satu ketinggalan revisi | `svn update` sisi yang lama |
| `CicilanPembayaranGagal.java` | `src/` bertanda **M** — suntingan lokal | **tidak disentuh** |

Berkas bertanda `M` adalah pekerjaan sesi lain yang belum di-commit. Meng-`update`-nya
berisiko menimpa pekerjaan itu, dan tidak ada alasan mendesak untuk menyamakannya sekarang —
ia akan sama dengan sendirinya begitu pemiliknya commit.

Sesudah disamakan, keempatnya identik.

## 4. Daftarnya berubah dua kali dalam satu batch

Yang paling layak dicatat bukan kelima berkas itu, melainkan bahwa **kumpulannya bergerak**:

- Saat `java/OnlineBmt.java` di-`update`, repositori sudah maju lagi — sekarang `java/` yang
  lebih baru dan `src/` yang ketinggalan. Perannya terbalik di tengah perbaikan.
- Saat alatnya dijalankan untuk memverifikasi hasil, daftarnya sudah berbeda: `JadwalPembayaran.java`
  dan `JamPerkuliahan.java` muncul dengan suntingan lokal, dan `CicilanPembayaranGagal.java`
  berubah menjadi bersih di kedua sisi — sesi lain rupanya baru meng-commit-nya.

Jadi "menyamakan kedua pohon" bukan pekerjaan yang bisa diselesaikan sekali. Pada repositori
yang disunting beberapa sesi sekaligus, beberapa berkas akan selalu berbeda pada saat mana
pun. Yang berguna bukan menyamakannya, melainkan **mengetahui berkas mana yang sedang
berbeda sebelum menyuntingnya.**

## 5. Alatnya

[alat/cermin-java-src.py](alat/cermin-java-src.py) membandingkan kedua pohon dan — yang
penting — memisahkan kedua jenis penyimpangan:

```
BEDA  ais/database/model/JadwalPembayaran.java
        suntingan lokal di src/ -- JANGAN di-update, itu kerja sesi lain
BEDA  ais/database/model/CicilanPembayaranGagal.java
        dua-duanya bersih -- salah satu ketinggalan revisi; svn update sisi yang lama
```

Tanpa pembedaan itu alatnya berbahaya: saran "samakan saja" pada berkas yang sedang disunting
sesi lain akan menghapus pekerjaan orang.

Layak dijalankan sebelum menyunting berkas Java mana pun di sini — bukan berkala, melainkan
saat hendak menyentuh sesuatu.
