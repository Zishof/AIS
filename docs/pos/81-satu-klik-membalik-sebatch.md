# Satu klik membalik status se-batch, dan kompilasi penuh sebagai alat

Batch lanjutan sesudah doc 80.

---

## 1. Temuan: `setPostingActive` bekerja pada riwayat, bukan pada jurnal

Layar Jurnal punya aksi "posting aktif/nonaktif" per baris. Yang dilakukannya:

```java
GrupTransaksi g = require(s, id);
PostingHistory p = g.getPostingHistory();
p.setPosting(value);
```

Ia mengubah **riwayat posting**, bukan jurnalnya. Itu penting karena riwayat dipakai
bersama: `PostingTransaksiHarianAction.postingSemua` membuat **satu** `PostingHistory`
(baris 959) sebelum gelung atas seluruh id yang terkumpul (baris 948). Seluruh dokumen
dalam satu posting massal memakai riwayat yang sama.

Akibatnya: menonaktifkan posting pada **satu baris** membalik status **seluruh dokumen
dalam batch itu**. Layarnya bekerja per baris, datanya bekerja per batch.

Bendera `posting` ini bukan hiasan — doc 53 mencatat bahwa `posting_history.posting`
menentukan sebuah dokumen terhitung terposting atau kembali menjadi draf di dasbor.

Aksinya juga tanpa pengaman: di controller hanya `csrf(q)` lalu langsung dijalankan —
tidak ada konfirmasi, tidak ada penyebutan berapa dokumen yang ikut terpengaruh. Bandingkan
dengan `deleteAll` dan `cleanDuplicates` di layar yang sama, yang menuntut konfirmasi ketik.

### Tidak diperbaiki sepihak

Sebagian dari perilaku ini mungkin memang dikehendaki: menonaktifkan satu batch posting
sekaligus adalah operasi yang masuk akal. Yang jelas keliru bukan efeknya, melainkan bahwa
efek itu **tidak terlihat** dari layar maupun dari balasan API.

Bahan untuk memperbaikinya sudah tersedia: penyaring `postingId` sudah terekspos ke klien,
sehingga daftar dokumen yang seposting dapat ditampilkan sebelum aksi dijalankan. Tiga
pilihan, urut dari yang paling ringan:

1. Balasan API menyertakan jumlah dokumen yang berbagi riwayat itu — aditif, klien lama
   tidak terpengaruh.
2. Layar menampilkan "N dokumen memakai posting ini" sebelum tombolnya ditekan.
3. Konfirmasi ketik bila N > 1, memakai mekanisme `confirm` yang sudah ada.

Pilihan mana yang tepat bergantung pada apakah pembalikan se-batch memang alur kerja yang
dipakai. Itu keputusan pemilik sistem.

`activateBatch` memanggil `setPostingActive` dalam gelung atas banyak id, sehingga riwayat
yang sama disetel berulang kali. Mubazir, tetapi tidak merusak: hasilnya sama.

## 2. Sapuan SQL mentah di paket `newui`: bersih

Seluruh 107 berkas paket `ais.common.newui` disapu untuk `createSQLQuery` yang mengubah
data. Yang ditemukan hanya dua, keduanya di `NewUiJournalService` dan keduanya sudah
dibahas di doc 80 (`deleteAll` dan `cleanDuplicates`). Tidak ada jalur tulis mentah lain
yang memintas lapisan entitas.

Hasil negatif, dan layak dicatat justru karena itu: kecurigaan bahwa pola "menulis lintas
modul lewat SQL mentah" tersebar luas di New UI ternyata tidak berdasar.

## 3. Daftar isi dicocokkan ulang: 50 dari 125 dokumen tidak terdaftar (r83111)

Folder ini berisi 125 dokumen. **Lima puluh di antaranya tidak pernah masuk tabel Daftar
isi** — 40 persen isinya praktis tidak dapat ditemukan dari pintu depannya sendiri.

Sebabnya wajar dan justru itu yang membuatnya berulang: dokumen ditulis banyak sesi yang
berjalan bersamaan, penomorannya berbenturan (ada dua berkas bernomor 53, 54, 55, 57, 60,
70 ...), dan tiap sesi menambahkan barisnya sendiri secara manual. Sekali terlewat, tidak
ada yang menyadarinya.

Ringkasan tiap baris baru diambil **apa adanya dari judul H1 dokumennya sendiri**, bukan
ditulis ulang — supaya tidak ada dokumen sesi lain yang salah diwakili.

Pemeriksaan ulangnya satu baris, dan layak dijalankan sesekali:

```sh
for f in *.md; do [ "$f" = README.md ] && continue
  grep -qF "($f)" README.md || echo "belum terdaftar: $f"; done
```

## 4. Kompilasi penuh dijadikan alat: [alat/kompilasi-penuh.sh](alat/kompilasi-penuh.sh)

Doc 77 mengompilasi seluruh pohon satu kali untuk memastikan `Pegawai.java` satu-satunya
yang patah. Pemeriksaan itu kini menjadi skrip, karena sekali jalan tidak menyelesaikan apa
pun: penyapu commit di mesin ini meng-commit perubahan siapa pun dalam hitungan detik dan
**tidak pernah mengompilasi**. Itu sudah terbukti meloloskan berkas rusak ke HEAD (doc 76).
Tidak ada gerbang lain.

Skripnya keluar dengan kode 1 bila ada galat, sehingga bisa dipasang di penjadwal tanpa
diawasi. Ia memakai `-source 1.7 -target 1.7` dengan sengaja: server produksi masih Java 7,
sedangkan javac 8 menerima konstruksi yang ditolak Java 7 (doc 36).

Lama jalannya belasan menit untuk 7.400-an berkas. Itu murah dibandingkan menemukan HEAD
tidak dapat dibangun pada saat rilis.

## 5. Alatnya langsung menemukan: HEAD tidak dapat dikompilasi lagi

Skrip itu dijalankan atas keadaan HEAD saat itu juga. Hasilnya **4 galat** dari dua sebab
yang tidak berhubungan — padahal kompilasi penuh di doc 77, beberapa jam sebelumnya, bersih.
`svn status` atas ketiga berkasnya kosong, jadi keadaan rusak itu memang sudah ter-commit.

**Satu — `DraftJurnalApiHelper:722`**

```
error: cannot find symbol
        kunciModulBaris != null && bolehAksi(tbmuser, kunciModulBaris, "create"));
  symbol: variable tbmuser
```

Pemeriksaan hak akses ditambahkan di dalam `ringkasan()`, yang tidak punya parameter
`Tbmuser`. Diperbaiki mengikuti pola berkas itu sendiri: dua pembantu lain di sana
(`jalankanPosting`, dan pembantu di baris 623) sudah menerima `Tbmuser` sebagai parameter
pertama, dan `proses()` — satu-satunya pemanggil `ringkasan` — sudah memegangnya. Parameter
ditambahkan dan diteruskan. Tidak ada keputusan rancangan baru yang diambil sepihak.

**Dua — `PosApi:1524, 1526, 1528`**

```
error: bolehAksiCrud(...) is not public in KantinHelper;
       cannot be accessed from outside package
```

Dideklarasikan tanpa modifier (package-private) lalu dipanggil dari paket lain. Dijadikan
`public`; `PosApi` sudah memanggil `KantinHelper` 183 kali dan kelas itu punya 166 metode
publik, jadi tidak ada batas rancangan yang ditembus. Commit r83115.

### Yang perlu digarisbawahi

Ini **kejadian kedua hari ini**, sesudah `Pegawai.java` di doc 76. Dua kali dalam satu hari,
dari dua sesi berbeda, dengan pola yang sama persis: perubahan yang belum selesai tersapu
commit sebelum sempat dikompilasi. Bukan kelalaian orangnya — tidak ada satu pun langkah di
alur kerja ini yang mengompilasi apa pun sebelum commit.

Alat ini tidak memperbaiki sebabnya. Ia hanya membuat akibatnya terlihat dalam hitungan
menit alih-alih ditemukan saat rilis. Sebabnya ada pada penyapu itu sendiri.

### Jebakan pemakaian yang langsung termakan sendiri

Skrip ini keluar dengan kode 1 bila ada galat. Panggilan pertama saya menyalurkannya ke
`| tail -12`, sehingga kode keluar yang terbaca adalah milik `tail` — **0**. Laporan tugas
latar pun menyebut "exit code 0" padahal keluarannya jelas menulis "galat: 4".

Catatan itu kini ada di kepala skripnya. Sebuah gerbang yang melaporkan sukses saat gagal
lebih buruk daripada tidak ada gerbang, karena ia menumbuhkan rasa aman.
