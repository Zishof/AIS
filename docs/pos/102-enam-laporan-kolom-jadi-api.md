# Enam laporan sisa lepas dari ZUL — dan cara mengujinya tanpa menebak

Lanjutan doc 101. Enam entri katalog yang di sana sengaja dibiarkan menunjuk ZK kini punya
pelaksana natif. Titik keluar ZK dari katalog laporan: **11 → 1**.

---

## 1. Yang dibuat

| Kunci | Isi |
|---|---|
| `lk_neracalajur` | Kertas kerja: Neraca Saldo, Penyesuaian, NSD, lalu dipisah ke kolom Laba Rugi / Neraca |
| `lk_keu2` | Dua kolom periode |
| `lk_keu12` | Dua belas kolom bulan pada satu tahun |
| `lk_keu2th` | Perbandingan dua tahun |
| `lk_aruskas12` | Mutasi bersih Kas/Bank per bulan |
| `lk_aruskas31` | Mutasi bersih Kas/Bank per hari dalam satu bulan |

Setelahnya `launchZk(...)` tidak dipanggil di mana pun, dan methodnya ikut dibuang — kode mati
yang menganggur hanya mengundang orang memakainya lagi. Yang tersisa hanya `dashAkun()`
(dasbor akuntansi ZK) dan dua formulir aset di `posting_akun_perbaikan.dart`.

## 2. Dua arti dalam satu kolom, disebut terang-terangan

Kelas ZK aslinya mengirim **satu tanggal per kolom** — akhir bulan, lewat
`getActualMaximum(DAY_OF_MONTH)`. Untuk akun **Neraca** artinya tegas: saldo per tanggal itu,
kumulatif. Untuk akun **Laba Rugi** artinya tidak tegas — bisa mutasi bulan itu, bisa kumulatif
tahun berjalan — dan tata letak jrxml yang menentukannya **tersimpan di basis data**
(LampiranLain), bukan di repositori. Jadi tidak bisa dibaca.

Pilihan yang diambil: akun Neraca kumulatif, akun Laba Rugi mutasi periode, dan perbedaan itu
ditulis di catatan tiap laporan serta di keterangan katalog. Menebak diam-diam akan
menghasilkan laporan yang angkanya berbeda dari laporan resmi **tanpa ada yang tahu kenapa** —
kegagalan yang persis sama bentuknya dengan yang diperingatkan doc 100, hanya arahnya terbalik.

Bukti bahwa keduanya memang berbeda, dari pengujian: akun Kas membawa saldo 650.000 dari Juni
sampai Desember, sedangkan Penjualan hanya muncul −1.000.000 di Juni lalu nol.

## 3. `exists`, bukan `join` — dan ini terukur

Satu akun boleh terdaftar di **beberapa** Kelompok Laporan. Bila klasifikasinya dipasang
sebagai join (seperti `JOIN_KLAS` yang sudah ada), barisnya menggandakan dan saldo akun itu
terhitung berkali-kali.

Diuji dengan satu akun Kas yang dipetakan ke dua kelompok:

| Cara | Saldo Kas |
|---|---|
| `join` | **1.300.000** |
| `exists` | **650.000** ← benar |

Angka yang salah itu tidak terlihat salah. Ia tidak error, tidak kosong, dan besarnya masuk
akal. Karena itu keputusannya dicatat di komentar konstanta `KLAS_AWAL`, lengkap dengan
angkanya, supaya tidak ada yang "menyederhanakannya" jadi join di kemudian hari.

## 4. Cara mengujinya: javac tidak memeriksa SQL sama sekali

Enam laporan ini seluruhnya SQL. Gerbang kompilasi memeriksa Java-nya dan **buta** terhadap
isi string SQL — salah nama kolom, `GROUP BY` tidak sah, atau `exists` yang menggandakan
semuanya lolos dengan mulus.

Jadi diuji di PostgreSQL 16 sungguhan: klaster sekali-pakai, skema `akunting` minimal, dan data
yang sengaja memuat jebakannya —

- satu akun dipetakan ke **dua** kelompok laporan (menguji penggandaan),
- satu jurnal **belum terposting** senilai 999.999 (harus tidak muncul),
- satu jurnal **penyesuaian** (`jenis_transaksi = 7`),
- satu akun bank yang hanya dikenali lewat `bank_id` (menguji heuristik Kas/Bank).

Semuanya lulus: 999.999 tidak pernah muncul, Kas muncul sekali, penyesuaian masuk kolomnya
sendiri, dan NSD seimbang 1.000.000 = 1.000.000.

**Satu hal yang penting soal caranya.** SQL-nya **tidak** disalin ulang dengan tangan untuk
pengujian — ia dirakit dengan membaca dan mengevaluasi berkas Java-nya. Percobaan pertama
memakai salinan tangan, dan penjaga "potongan ini harus ada di sumber" langsung gagal: rakitan
runtime memang tidak pernah muncul utuh di sumber karena Java memecahnya menjadi banyak
literal. Menguji salinan tangan berarti menguji kueri yang **bukan** yang dikirim ke basis
data — pengujian yang selalu lulus dan tidak berarti apa-apa.

Manfaatnya langsung terbukti sekali lagi: setelah neraca lajur dirombak memakai konstanta
bersama, SQL dirakit ulang dari sumber terkini dan hasilnya identik angka-per-angka dengan
sebelum dirombak. Itu yang membuat perombakan boleh disebut aman.

## 5. `lk_dashakun`: satu jebakan nama, dan satu hal yang TIDAK disimpulkan

Entri terakhir yang masih menunjuk ZK adalah `lk_dashakun` — "Rasio, Grafik, Laba Ditahan &
Proyeksi Kas" → `common/display.zul?p=akuntansi`.

Penelusurannya memuat jebakan yang layak dicatat. Di paket `ais.action.master.akunting.helper`
ada **dua** kelas yang namanya beda satu huruf:

| Kelas | Ukuran | Dipakai `display.zul?p=akuntansi`? |
|---|---|---|
| `DasboardAkuntansi.java` | 92 KB | tidak |
| `DasboardAkunting.java` | 50 KB | **ya** |

Membuka yang salah menghasilkan kesimpulan yang terdengar meyakinkan tentang kelas yang tidak
ada hubungannya dengan entri ini.

Di kelas yang benar, kata "ditahan", "proyeksi", "forecast", "prediksi", dan "equity" **tidak
muncul sama sekali**, dan satu-satunya judul bagian yang tertulis adalah "Buku Besar" dan
"Neraca Lajur" — dua-duanya sudah punya versi natif sekarang.

Itu **belum cukup** untuk menyimpulkan entrinya berlebihan. Dasbor itu menyusun isinya secara
programatik, jadi daftar bagiannya tidak bisa dibaca utuh dari sumber; ketiadaan sebuah kata
bukan bukti ketiadaan fiturnya. Mencabut tautannya atas dasar itu berarti mengulang persis
kegagalan yang diperingatkan doc 100 — menghapus sesuatu yang bekerja karena pengukurannya
menjawab pertanyaan lain. Jadi entri ini sengaja dibiarkan, dan yang dicatat hanyalah apa yang
benar-benar terlihat.

## 6. Sisa

- `dashAkun()` → `common/display.zul?p=akuntansi` (dasbor, bukan laporan)
- `posting_akun_perbaikan.dart` → `master_asset.zul`, `kelompok_asset.zul` — butuh layar
  Flutter baru plus API-nya, bukan sekadar kunci laporan

Keduanya bukan pekerjaan katalog laporan, jadi tidak dipaksakan masuk batch ini.
