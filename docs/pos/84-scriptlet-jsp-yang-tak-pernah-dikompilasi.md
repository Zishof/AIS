# Scriptlet JSP yang tak pernah dikompilasi

Batch lanjutan sesudah doc 83.

---

## 1. Gerbang JSP kemarin berhenti separuh jalan

Doc 83 membuktikan seluruh 10.374 JSP **dapat diterjemahkan**. Yang belum dinilai:
**Java di dalam scriptlet-nya**. Padahal di situlah letak pembusukan yang paling wajar —
kode Java berubah setiap hari, dan scriptlet yang memanggil metode lama tidak ikut
diperbarui karena tidak ada kompilator yang menegurnya.

Langkah yang kurang itu sederhana: hasil terjemahan Jasper adalah berkas `.java` biasa.
Kompilasi saja terhadap pohon kelas proyek.

## 2. Hasilnya: satu halaman yang pasti gagal dibuka

`WEB-INF/baru/modul/elearning/load_ringkasan.jsp` memanggil `StreamingHibernateUtil`
seolah kelas utilitas statis:

```java
sessStream = StreamingHibernateUtil.openSession();          // baris 28
StreamingHibernateUtil.closeSessionQuietly(sessStream);     // baris 297
```

Keduanya salah:

| Panggilan | Kenyataannya |
|---|---|
| `openSession()` | metode **instance**; kelasnya singleton lewat `getInstance()` |
| `closeSessionQuietly(Session)` | **tidak ada** pada kelas itu |

`StreamingHibernateUtil` hanya *memanggil* `HibernateUtil.closeSessionQuietly(s)` di dalam
`closeSession()`-nya sendiri (baris 106) — ia tidak pernah mendefinisikannya.

Jadi halaman ini **tidak dapat dikompilasi sama sekali**. Dan karena JSP baru diterjemahkan
Tomcat saat halamannya dibuka, kerusakan itu tidak muncul di mana pun: tidak di commit,
tidak di kedua gerbang `.java`, tidak di gerbang terjemahan JSP doc 83 — hanya sebagai
halaman galat di depan pengguna yang membukanya.

Perbaikannya (r83171):

```java
sessStream = StreamingHibernateUtil.getInstance().openSession();
HibernateUtil.closeSessionQuietly(sessStream);
```

Penutupannya mengikuti aturan yang sudah tertulis di JavaDoc `HibernateUtil`: sesi yang
dibuka lewat `openSession()` — session lepas, bukan ThreadLocal — ditutup dengan
`closeSessionQuietly(s)`. `HibernateUtil` sudah diimpor di JSP itu sejak awal, jadi tidak
ada impor baru yang perlu ditambahkan.

Diverifikasi: terjemahan Jasper 0 galat, kompilasi scriptlet 0 galat, **1 kelas dihasilkan**.
Angka terakhir yang penting — nol galat dengan nol keluaran adalah lolos palsu (doc 82).

## 3. Kenapa ini belum bisa jadi gerbang

Uji coba atas 300 berkas pertama memberi 140 galat, dan ragamnya membongkar batas
pendekatan ini:

| Galat | Jumlah | Arti |
|---|---|---|
| `variable vm` | 59 | **derau** — potongan JSP yang memang tidak berdiri sendiri |
| `method closeSessionQuietly(Session)` | 1 | **temuan sungguhan** |

Banyak berkas `.jsp` di sini adalah **potongan** yang di-`include` ke halaman lain dan
memakai variabel yang didefinisikan halaman pemanggilnya. Jasper tetap membuatkan servlet
tersendiri untuk tiap berkas, sehingga potongan itu dikompilasi terpisah — dan variabel
pemanggilnya tentu saja tidak ada. Galatnya nyata bagi kompilator, tetapi salah bagi kita.

Karena itu langkah ini belum dipasang sebagai gerbang seperti tiga alat sebelumnya. Yang
berguna adalah **menyaring ragam galatnya**: "cannot find symbol: method/class" menandakan
rujukan yang basi, sedangkan "cannot find symbol: variable" hampir selalu berarti berkasnya
sebuah potongan.

## 4. Sapuan penuh: 10.374 berkas, dua halaman rusak

Seluruh hasil terjemahan dikompilasi sekaligus terhadap pohon kelas proyek. > **KOREKSI (doc 85): alinea berikut SALAH.** Sapuan itu berhenti pada galat *sintaks*
> sebelum javac sampai ke analisis semantik, jadi derau `vm` tidak pernah diperiksa --
> bukan hilang. Sesudah galat sintaksnya diperbaiki, deraunya muncul utuh: 124 galat dari
> 25 berkas. Lihat [85-gerbang-keempat-dan-koreksi-doc-84.md](85-gerbang-keempat-dan-koreksi-doc-84.md).

Menariknya,
menyusun **semuanya dalam satu jalan** menghilangkan hampir seluruh derau potongan JSP —
javac melihat berkas-berkas itu bersamaan, jadi 59 galat `variable vm` pada uji 300 berkas
tinggal nol. Yang tersisa hanya lima galat, dari dua berkas.

### `karir/setelah_login.jsp:8` — escape petik ganda salah tulis

```java
.replace("\\"", "&quot;")
```

Itu string berisi backslash, lalu petik ganda menggantung: empat galat sintaks beruntun
dari satu salah ketik. Maksudnya jelas `.replace("\"", "&quot;")`.

### `presensi/_service_presensi_harian.jsp:161` — lambda pada kanal yang mengompilasi Java 1.6

```java
java.util.function.Function<Double, String> formatDurasi = (Double hours) -> { ... };
```

Tomcat mengompilasi JSP pada `compilerSourceVM` **bawaan 1.6**, dan setelan itu tidak
di-override di mana pun — tidak di `WEB-INF/web.xml`, tidak di `conf/web.xml` Tomcat
(hanya disebut di blok komentar). Lambda karenanya ditolak.

Diubah menjadi kelas anonim yang setara. Semua pemanggil `.apply(...)` tidak tersentuh,
dan hasilnya selaras dengan basis kode yang memang dikompilasi pada `-source 1.7`.
Alternatifnya menyetel `compilerSourceVM=1.8`, tetapi itu mengubah konfigurasi penyebaran
seluruh aplikasi demi satu halaman.

Keduanya di r83174, diverifikasi: terjemahan 0 galat, kompilasi scriptlet 0 galat,
**3 kelas dihasilkan**.

## 5. Yang membuat temuan ini mudah terlewat

Ketiga halaman yang diperbaiki hari ini — `load_ringkasan`, `setelah_login`,
`_service_presensi_harian` — tidak akan pernah muncul di pemeriksaan mana pun yang ada
sebelumnya:

| Pemeriksaan | Menangkapnya? |
|---|---|
| kompilasi `.java` (doc 81, 82) | tidak — JSP bukan `.java` |
| terjemahan JSP (doc 83) | tidak — sintaks JSP-nya sah, yang salah Java di dalamnya |
| penyapu commit | tidak — tidak memeriksa apa pun |
| pemakaian sehari-hari | hanya bila ada yang kebetulan membuka halaman itu |

Tiga halaman rusak, dan satu-satunya cara menemukannya adalah membuka halamannya satu per
satu — atau melakukan apa yang dilakukan Tomcat, lebih awal.

## 6. Batas yang jujur

Pemeriksaan ini memakai pohon kelas hasil kompilasi penuh terakhir. Bila pohon itu basi,
hasilnya ikut basi: metode yang baru dihapus pagi ini tidak akan terdeteksi sampai pohon
kelasnya dibangun ulang. Urutannya karena itu penting — kompilasi `.java` dulu, baru
scriptlet JSP.

Dan seperti doc 83: lolosnya kompilasi tidak berarti halamannya benar. Tidak ada satu pun
halaman yang benar-benar dijalankan di sini.
