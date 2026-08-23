# 21 — Penomoran Dokumen Keuangan

Tempat tiap jenis dokumen Keuangan dipasangkan dengan templat nomornya. Dipindahkan dari
layar ZK `ais.action.master.akunting.NomorSuratAlurKeuanganAction`.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/NomorSuratKeuanganApiHelper.java` |
| Kunci menu | `nomor_surat_keuangan` (fail-closed, hak per-aksi) |
| Layar | `apps/ebisnis/lib/screens/nomor_surat_keuangan_screen.dart` |
| Perbaikan menyertai | `DraftJurnalApiHelper.modulPosting` (gerbang fail-open) |

---

## 1. Bukti yang memunculkannya

Kode dokumen yang terbit di harness modul-modul sebelumnya berbentuk `1041B55F9FAF` dan
`1041B080E08B` — **barcode, bukan nomor dokumen**. Penyebabnya satu pola yang sama di semua
modul Keuangan:

```java
if (NomorSuratAlurKeuangan.X_DATA == null
        || NomorSuratAlurKeuangan.X_DATA.getNomorSurat() == null) {
    return Common.getGeneratedBarCode();          // <-- jatuh ke BARCODE
}
```

Pemeriksaan basis data uji:

```
id | kode | nama                          | nomor_surat
 1 | 001  | Uang Muka                     | 1        <-- satu-satunya yang terpasang
 2 | 002  | Dana Talangan                 | null
 3 | 003  | Pertanggungjawaban            | null
 4 | 004  | Kas Kecil                     | null
 5 | 005  | Pembayaran Pembelian          | null
 6 | 006  | DPC                           | null     <-- dipakai Proses Transfer
 7 | 007  | Kas Besar                     | null
 8 | 008  | Pertanggungjawaban Kas Besar  | null
 9 | 009  | Transaksi Koperasi            | null
10 | 010  | Standing Instruction          | null
```

**Sembilan dari sepuluh alur menerbitkan barcode**, dan sampai sekarang memperbaikinya hanya
bisa dari layar ZK. Tidak ada satu pun pesan yang memberitahukannya — dokumennya tersimpan
dengan tenang, kodenya saja yang tidak dapat dibaca manusia.

---

## 2. Yang dikerjakan layar ini

**Tab "Alur Dokumen"** — sebelas alur, templat yang terpasang, contoh hasilnya, dan tanda
`pakaiBarcode` untuk yang belum dipasang berikut spanduk jumlahnya. Kalimatnya menyebut
akibatnya apa adanya ("dokumen jenis ini terbit dengan kode barcode, mis. 1041B55F9FAF"),
bukan label netral seperti "belum diatur" — yang netral tidak memberi tahu apa yang rusak.

**Tab "Templat Nomor"** — menyusun formatnya dari sepuluh segmen berurutan. Jenis segmen
yang dikenal mesin penomoran: `Kosong`, `Nomor Urut`, `Kata Statis`, `Tanggal`, `Bulan`,
`Bulan Romawi`, `Tahun`. Plus aturan reset (tiap tahun / tiap bulan), jumlah angka nol di
depan, dan index tersimpan.

---

## 3. Tiga hal yang mudah salah, dan penanganannya

**Templat tanpa segmen "Nomor Urut" ditolak.** Tanpa itu setiap dokumen menerima teks yang
sama persis, dan karena kode dokumen wajib unik, penyimpanannya akan gagal berulang kali di
`KodeUnikUtil.pastikanUnik` dengan pesan yang tidak dapat dimengerti pengguna. Ditolak di
layar maupun di server.

**"Tanda" pada Kata Statis adalah ISI-nya, bukan pemisah.** Pada segmen lain kolom itu
menempel *sesudah* segmennya sebagai pemisah; pada Kata Statis, kolom itulah teksnya.
Label isiannya berubah mengikuti jenis segmen supaya perbedaan itu tidak perlu ditebak.

**Templat yang masih dipasang tidak boleh dihapus.** Menghapusnya membuat alurnya langsung
jatuh ke barcode tanpa siapa pun menyadarinya. Pesannya menyebut berapa alur yang memakainya.

---

## 4. Pratinjau tidak menghabiskan nomor

`NomorSurat.format()` murni — yang menaikkan urutan adalah
`NomorSurat.tambahIndexNomorSurat()`, dan itu tidak pernah dipanggil di modul ini. Jadi
melihat contoh hasil aman diulang berkali-kali, termasuk saat menyusun templat yang belum
disimpan (`nomor_surat_keuangan_pratinjau` membangun objek lepas yang tidak pernah dilekatkan
ke sesi Hibernate).

**Catatan pemanggilan.** Dipakai bentuk `format(urutan, tanggal, satuanKerja)` yang
bersatuan kerja eksplisit. Bentuk dua argumennya memanggil `Common.getSatuanKerja()` **di
luar blok try**, sehingga meledak di luar konteks servlet — termasuk di harness.

---

## 5. Perbaikan menyertai: dua baris Draft Jurnal yang fail-open

`DraftJurnalApiHelper.modulPosting()` memetakan nama baris ke kunci menu yang menggerbangi
tombol Posting. Dua barisnya mengembalikan kunci yang **bukan kunci menu**:

```java
if ("Jurnal Pengajuan Transfer".equals(namaBaris)) return "pengajuan_transfer";
if ("Transitori".equals(namaBaris)) return "transitori";
```

`EbisnisMenuKatalog.bolehAksi` mengembalikan **true** untuk kunci di luar `KUNCI_CRUD`
(baris 576–578). Artinya kedua baris itu **fail-open**: siapa pun yang dapat membuka Draft
Jurnal boleh memposting maupun membatalkan jurnal pergerakan dana, tanpa gerbang per peran —
padahal setiap baris lain di sana digerbangi hak `create` modulnya sendiri.

Kini menunjuk `proses_transfer` dan `proses_transitori`, kunci menu yang benar-benar ada.

---

## 6. Pemilih akun: pohon, dan hanya daun yang dapat dipilih

Ditemukan saat modul ini dipakai: dialog pemilih akun (`widgets/pemilih_akun.dart`) berupa
**daftar datar** dan **semua** akun dapat dipilih — termasuk akun induk seperti
`100.000 ASET LANCAR` dan `111.000 KAS` yang tidak pernah menampung transaksi. Memilihnya
menghasilkan jurnal yang mendarat di akun salah, dan itu tidak ketahuan sampai laporannya
dibaca.

Angka pada basis data uji: **311 akun, 234 daun** — artinya **77 akun induk** ditawarkan
sebagai pilihan padahal tidak satu pun boleh dipakai.

Perbaikannya:

- `akun_list` kini mengirim `parentId` dan `leaf` (`NOT EXISTS` atas anak). Penambahan ini
  **aditif**, jadi pemanggil lama yang hanya membaca `id`/`kode`/`nama` tidak terpengaruh.
- Dialognya menyusun akun sebagai **pohon** dengan indentasi mengikuti kedalamannya.
- Akun induk **tidak dapat ditekan** (`enabled: false`, `onTap: null`) dan dibuat redup —
  jelas bukan pilihan, bukan sekadar pilihan yang kebetulan salah.
- Kotak pencarian **tetap ada** (kode maupun nama, kata boleh tersebar). Saat mencari,
  **jalur induk** hasilnya ikut ditampilkan sebagai konteks supaya susunannya tetap terbaca.
- Enter mengambil **daun teratas**, bukan baris teratas — akun induk tidak pernah menjadi
  jawaban.
- Jumlah yang disebut adalah jumlah **daun** ("3 akun dapat dipilih"), bukan seluruh baris.

---

## 7. Papan cepat: validasi Uang Muka dijalankan lebih dulu di layar

Dari log: `uang_muka_simpan` ditolak "Nilai belum diisi." dengan `nilai: 0.0`. Itu
**perilaku yang benar** — kolom Nilai memang wajib dan memang kosong. Yang kurang baik
adalah penggunanya harus menunggu perjalanan ke server untuk diberi tahu.

Judul dan Nilai kini diperiksa di layar sebelum dikirim. Server **tetap memeriksanya
ulang**; ini hanya mempercepat pesannya, bukan memindahkan aturannya ke layar. Pada
pengajuan berbasis PR, Nilai memang boleh kosong (nilainya datang dari baris PR) — sama
persis dengan aturan di server.

---

## 8. Hasil uji

| Uji | Hasil |
|---|---|
| `TesNomorSuratKeuangan` | **26 lulus, 0 gagal**, sisa data uji 0 |
| `flutter analyze` | 0 error |
| `flutter test` | **263 lulus** (sebelumnya 252) |

Harness memastikan langsung di basis data uji: **9 dari 10 alur belum dipasangi templat**.
Setelah templat contoh dipasang, kode yang dihasilkan `DPC/001/VIII/2026` — bukan barcode.
Pratinjau dijalankan dua kali dan `nomorindex` tidak bergerak (1 → 1), membuktikan ia tidak
menghabiskan nomor.

---

## 9. Catatan: pohon kelas UAT sempat kosong

`C:\opt\AIS\ais\build\uat-77608` — classpath yang dipakai `kompilasi_api.bat` dan
`jalankan_baru.bat` — ditemukan **kosong sama sekali** (nol berkas `.class`), tanpa proses
build yang sedang mengisinya. Tidak ada satu pun skrip di repositori yang menyebut nama
direktori itu, jadi ia memang pohon hasil `javac` manual atas seluruh `src/main/src`.

Dibangun ulang atas persetujuan pemilik pekerjaan: `javac -source 1.7 -target 1.7
-encoding UTF-8` atas **7.058 berkas** dengan classpath `webapp/WEB-INF/lib/*`, menghasilkan
**40.182 kelas**, nol galat. Dikompilasi ke direktori sementara lebih dulu lalu dipasang
hanya setelah dipastikan targetnya masih kosong — supaya sesi lain tidak menemukan pohon
setengah jadi.
