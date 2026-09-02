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
