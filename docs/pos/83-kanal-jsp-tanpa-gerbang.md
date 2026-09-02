# Kanal JSP tanpa gerbang

Batch lanjutan sesudah doc 82.

---

## 1. Lubang yang tersisa

Dua gerbang dari doc 81 dan 82 hanya menutup berkas `.java`. Di repositori ini ada
**10.374 berkas `.jsp`**, dan JSP tidak ikut dikompilasi kapan pun sebelum dipakai: Tomcat
menerjemahkannya **saat halamannya dibuka**.

Artinya tag yang tidak ditutup, direktif yang salah tulis, atau `include` yang menunjuk
berkas tak ada lolos sepenuhnya — melewati commit, melewati kedua gerbang `.java`, melewati
rilis — dan baru muncul sebagai halaman galat di depan pengguna. Kanal yang paling tidak
terlindungi justru yang paling langsung dilihat orang.

## 2. Alatnya: [alat/jsp-terjemah.ps1](alat/jsp-terjemah.ps1)

Memakai **Jasper**, prakompiler JSP bawaan Tomcat, yang bisa dijalankan luring tanpa
menyalakan server. Bawaannya menguji JSP yang berubah sejak awal hari ini; `-Semua` menyapu
seluruh webapp.

```
cakupan      : berubah sejak {2026-09-02}
berkas diuji : 68
galat        : 0
java dihasilkan: 68
BERSIH
```

Yang dinilai adalah **terjemahan JSP-nya**: tag, direktif, penutupan scriptlet, resolusi
include dan taglib. Java di dalam scriptlet tidak ikut dinilai — itu menuntut seluruh pohon
kelas proyek ikut dikompilasi, dan sudah ditangani gerbang `.java`.

### Classpath-nya menyesatkan kalau salah

Tiga bagian, dan ketiganya wajib:

| Bagian | Kenapa |
|---|---|
| `tomcat/lib/*.jar` | `jasper.jar`, servlet-api, EL |
| `tomcat/bin/*.jar` | `tomcat-juli.jar` — `org.apache.juli.logging.LogFactory` |
| `ant/lib/*.jar` | `JspC extends org.apache.tools.ant.Task` |

Kurang salah satunya, pesannya `Error: Could not find or load main class
org.apache.jasper.JspC` — seolah Jasper-nya tidak terpasang, padahal kelasnya ada dan yang
hilang adalah kelas induknya. `javap` atas jar-nya yang membongkar itu: `JspC` ternyata
turunan `Task` milik Ant, dan Ant memang tidak ikut di lib Tomcat.

## 3. Dibuktikan MENOLAK lebih dulu

Sesuai pelajaran doc 82, alat ini diuji pada kasus yang seharusnya **gagal** sebelum
hasil "BERSIH"-nya dipercaya. Dua JSP kecil, satu sah dan satu dengan scriptlet tidak
ditutup:

```
=== sah.jsp
INFO: Generation completed with [0] errors

=== rusak.jsp
SEVERE: JasperException: rusak.jsp (line: [3], column: [2]) Unterminated [<%] tag
Generation completed with [1] errors
```

Alatnya juga memakai tiga syarat yang sama dengan gerbang `.java`: hitungan galat, dan
**jumlah berkas hasil terjemahan** — nol berkas berarti penerjemahannya tidak benar-benar
berjalan, betapapun bersih laporannya.

## 4. Hasil: seluruh kanal JSP sehat

Sapuan penuh dijalankan atas keadaan HEAD:

| | |
|---|---|
| berkas JSP di webapp | **10.374** |
| berhasil diterjemahkan | **10.374** |
| galat | **0** |
| lama | **82 detik** |

Jumlah keluaran cocok persis dengan jumlah masukan — itu yang membedakan "bersih" dari
"tidak mengerjakan apa pun".

Delapan puluh dua detik untuk seluruh kanal. Berbeda dari kompilasi `.java` yang butuh
belasan menit dan karenanya dibuatkan versi cepat (doc 82), pemeriksaan JSP **tidak perlu
dibagi dua**: menyapu semuanya sudah cukup murah untuk dijalankan sesering apa pun.

### Yang berarti dan yang tidak

Hasil ini berarti: tidak ada satu pun JSP yang akan gagal diterjemahkan saat dibuka. Tag
tertutup, direktif sah, include dan taglib teresolusi.

Hasil ini **tidak** berarti halamannya benar. Java di dalam scriptlet tidak dinilai di
sini, dan tidak ada JSP yang benar-benar dijalankan. Sebuah halaman masih bisa melempar
`NullPointerException` pada baris pertama scriptlet-nya dan tetap lolos pemeriksaan ini.

Batas itu perlu disebut justru karena angkanya terlihat meyakinkan. Nol galat atas 10.374
berkas mudah dibaca sebagai "kanal JSP sudah aman", padahal yang dibuktikan hanya bahwa
halamannya dapat diterjemahkan.

## 5. Tiga gerbang sekarang

| Alat | Cakupan | Lama | Kapan |
|---|---|---|---|
| [alat/kompilasi-berubah.sh](alat/kompilasi-berubah.sh) | `.java` yang berubah | menit | sebelum commit |
| [alat/jsp-terjemah.ps1](alat/jsp-terjemah.ps1) | JSP (berubah atau semua) | detik | sebelum commit |
| [alat/kompilasi-penuh.sh](alat/kompilasi-penuh.sh) | seluruh `.java` | belasan menit | berkala, tanpa ditunggui |

Ketiganya keluar dengan kode 1 bila gagal, dan ketiganya memeriksa **jumlah keluaran**, bukan
hanya hitungan galat — pelajaran dari doc 82, tempat sebuah gerbang melaporkan BERSIH tanpa
mengompilasi apa pun.

Tidak satu pun dari ketiganya berjalan sendiri. Penyapu commit di mesin ini tetap
meng-commit tanpa memeriksa apa pun; alat-alat ini hanya membuat kerusakan terlihat dalam
hitungan menit, bukan saat rilis.
