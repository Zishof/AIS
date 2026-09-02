# Gerbang keempat, dan koreksi atas doc 84

Batch lanjutan sesudah doc 84.

---

## 1. Koreksi: klaim "derau hilang bila dikompilasi sekaligus" itu SALAH

Doc 84 §4 menulis bahwa menyusun seluruh hasil terjemahan dalam satu jalan javac
menghilangkan derau potongan JSP — 59 galat `variable vm` pada uji 300 berkas menjadi nol
pada sapuan 10.374 berkas.

**Bukan itu yang terjadi.** Sapuan penuh saat itu berhenti pada lima galat **sintaks** dari
dua berkas. javac melaporkan galat sintaks pada tahap parse dan tidak melanjutkan ke analisis
semantik; ia tidak pernah sampai memeriksa `vm`. Nol galat `variable vm` bukan tanda derau
hilang, melainkan tanda pemeriksaannya berhenti lebih awal.

Begitu kedua berkas itu diperbaiki (r83174), jalan berikutnya melaju sampai analisis
semantik dan derau itu muncul utuh: **124 galat dari 25 berkas**, sebagian besar `vm`.

Yang membuat kekeliruan ini mudah dipercaya: angkanya bergerak ke arah yang diharapkan.
Lima galat terdengar seperti kemajuan dibanding 140, padahal keduanya mengukur hal yang
berbeda — yang satu berhenti di parse, yang lain sampai ke semantik. Pelajaran yang sama
berulang untuk ketiga kalinya: **hitungan galat yang menurun tidak selalu berarti keadaan
membaik.**

## 2. Derau potongan JSP ditangani dengan cara yang benar

Tujuh dari 25 berkas bergalat adalah potongan di `WEB-INF/baru/home/` yang namanya berawalan
garis bawah — `_header.jsp`, `_footer.jsp`, `_hero.jsp`, dan seterusnya. Semuanya memakai
`vm`, variabel milik halaman yang meng-include-nya.

[alat/jsp-scriptlet.ps1](alat/jsp-scriptlet.ps1) kini melewati berkas berawalan garis bawah
dan menyebut jumlahnya di laporan. Jasper menyandikan `_` menjadi `_005f` pada nama berkas
hasil terjemahan, jadi penyaringnya bekerja pada nama itu.

Ini penyaring berbasis konvensi penamaan, bukan analisis include yang sesungguhnya. Potongan
yang tidak berawalan garis bawah tetap lolos ke daftar — dan tiga di antaranya justru ternyata
bukan potongan sama sekali, melainkan cacat sungguhan (bagian 3).

## 3. Tiga halaman rusak lagi (r83189)

Ketiganya tersembunyi di balik galat sintaks tadi:

| Halaman | Cacat |
|---|---|
| `baru/modul/home/dosen/info.jsp` | `dosen.getNip()` — kelas `Dosen` tidak punya metode itu; yang ada `getNidn()`. Label di halaman itu sendiri berbunyi **"NIP / NIDN"**, jadi `getNidn()` memang yang dimaksud |
| `o/kursus/content/berita.jsp` | `Common.monthFormat21.format(...)` — bertipe `ThreadLocal<SimpleDateFormat>`, jadi harus `.get().format(...)` seperti pemakaian `dateFormat` di seluruh basis kode |
| `o/ux/content/common/menu.jsp` | `MainHelper.hasChild(Long, List<Menu>)` dipanggil dengan `Set<Menu>`. Salinan `List` dibuat **sekali di luar gelung**, bukan per baris menu |

Dengan tiga sebelumnya (doc 84), **enam halaman rusak** ditemukan dan diperbaiki hari ini —
semuanya halaman yang pasti gagal saat dibuka, dan tidak satu pun tertangkap pemeriksaan
yang ada sebelumnya.

## 4. Dua cacat pemakaian pada alatnya sendiri

**Log tidak terbaca.** Redirection `*>` PowerShell menulis **UTF-16** dan membungkus stderr
javac dalam format record galatnya sendiri (`javac.exe : ...` diikuti `At ...ps1:77 char:1`).
Akibatnya lognya tidak terbaca `grep`/`sed`, dan nomor barisnya pecah di tengah pesan.
Diganti `2>&1 | Out-File -Encoding utf8`.

Yang menutupinya: `Select-String` milik PowerShell membaca UTF-16 tanpa mengeluh, jadi
**hitungan galat di dalam skrip tetap benar** sementara lognya tidak dapat dianalisis alat
lain. Cacat yang hanya terasa oleh pemakai berikutnya.

## 5. Keadaan sekarang: 18 galat di 11 berkas

Sesudah potongan dikecualikan dan enam halaman diperbaiki:

| | |
|---|---|
| galat | **18** |
| berkas | **11** |
| `cannot find symbol` | 14 |
| `incompatible types` | 2 |
| lain-lain | 2 |

Sebagian mungkin masih potongan yang tidak berawalan garis bawah; sebagian lagi kemungkinan
besar cacat sungguhan — pola yang muncul mirip dengan enam yang sudah diperbaiki: metode yang
sudah tidak ada, `ThreadLocal` yang dipakai langsung, tipe koleksi yang tidak cocok.

Sisanya sengaja tidak dikerjakan dalam batch ini. Masing-masing menuntut pemeriksaan
tersendiri terhadap sumbernya — persis seperti enam yang pertama — dan menebak perbaikannya
tanpa itu justru akan menambah kerusakan.
