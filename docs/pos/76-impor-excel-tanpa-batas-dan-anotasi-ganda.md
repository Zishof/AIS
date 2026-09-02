# Impor Excel tanpa batas ukuran, dan anotasi ganda yang mematahkan kompilasi

Batch lanjutan sesudah doc 75.

---

## 1. Impor Excel: muatan didekode tanpa batas — ditutup

[35-lampiran-gambar.md](35-lampiran-gambar.md) menutup dengan bagian "Yang masih terbuka":
impor Excel tidak punya batas ukuran sama sekali di sisi server, "lubang memori yang
sejenis" dengan yang sudah ditutup untuk gambar.

Jalur gambar sudah benar sejak awal — memeriksa panjang **selagi masih berupa teks**:

```java
String tolakUkuran = PenjagaLampiranGambar.periksaPanjangBase64(base64, MAKS_GAMBAR_BYTES);
```

`periksaPanjangBase64` menghitung dari panjang string base64 (4 karakter per 3 byte, plus
kelonggaran 10%) sehingga muatan berlebih ditolak **sebelum** satu byte pun didekode.

Tiga situs impor tidak mengikutinya:

| Situs | Keadaan sebelumnya |
|---|---|
| `produkImporExcelPreview` | tanpa batas apa pun |
| importir katalog kedua (`file_base64`, bentuk sama) | tanpa batas apa pun |
| `bacaBerkasSoHarian` | ada batas 10 MB, tetapi **diperiksa sesudah `decode()`** |

Yang ketiga paling menyesatkan: batasnya *terlihat* ada, dan pesan penolakannya benar,
tetapi pemeriksaannya terjadi setelah salinan biner sebesar 3/4 muatan terlanjur
dialokasikan di heap. Muatan 400 MB tetap menjadi 300 MB byte array lebih dulu, baru
ditolak.

Ketiganya kini memanggil `periksaPanjangBase64` dengan `MAKS_LAMPIRAN_BYTES` (10 MB)
sebelum `decode()`. Pemeriksaan panjang-nol pada SO Harian dipertahankan apa adanya.

### Batas kejujuran perbaikan ini

Ini **mengurangi**, bukan menghapus, paparannya. String base64-nya sendiri sudah berada di
memori ketika sampai ke pemeriksa — ia ikut terbaca saat payload JSON diurai. Yang dicegah
adalah salinan biner berikutnya dan pembacaan workbook-nya, bukan muatan aslinya. Penutup
yang sesungguhnya adalah batas ukuran permintaan di tingkat servlet/kontainer; itu
pekerjaan tersendiri dan belum dikerjakan.

## 2. `Pegawai.java`: `@ManyToOne` ganda — kompilasi patah di HEAD

Ditemukan tidak sengaja: kompilasi verifikasi untuk butir 1 gagal, dan galatnya bukan pada
berkas yang disunting.

```
Pegawai.java:5915: error: repeated annotations are not supported in -source {0}
Pegawai.java:6208: error: ManyToOne is not a repeatable annotation type
```

Dua getter — `getAtasan()` dan `getOrangTua()` — membawa `@ManyToOne` **dua kali**: satu di
atas blok JavaDoc, satu lagi di bawahnya tepat sebelum deklarasi:

```java
@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
/**
 * Atasan berbasis jenis jabatan, bukan orang tertentu ...
 */
@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
@JoinColumn(name = "atasan", nullable = true)
public JenisJabatan getAtasan() {
```

`svn status` bersih, jadi keadaan ini **sudah ter-commit**: berkas itu tidak dapat
dikompilasi sama sekali di HEAD.

Asal-usulnya sama dengan JavaDoc yatim di doc 75 — blok JavaDoc disisipkan di antara
anotasi dan deklarasinya — hanya saja di sini anotasinya ikut tersalin, sehingga akibatnya
naik dari "dokumentasi tak terlihat" menjadi "berkas tidak bisa dikompilasi".

Salinan di atas JavaDoc dihapus, dan **hanya karena teksnya identik persis** dengan yang di
bawah, sehingga pemetaan Hibernate tidak berubah sedikit pun.

## 3. Koreksi: bukan 110 berkas, hanya satu

Sapuan pertama melaporkan **110 anotasi terduplikasi di 78 berkas** dan menyimpulkan
kerusakan build tersebar luas. **Angka itu salah**, dan koreksinya penting supaya tidak ada
yang memburu 78 berkas yang sebenarnya sehat.

Pendeteksinya menganggap tiap baris berawalan `@` sebagai anotasi telanjang. Padahal berkas
`sapto/*` menulis anotasi dan deklarasi **pada satu baris**:

```java
/** @return kode sheet template worksheet borang. */
@Override protected String getSheetCode() { return sheetCode; }
/** Tidak menambahkan filter apa pun ... */
@Override protected void buildFilters(Row row) { /* no filters */ }
```

Dibaca alat itu, baris pertama tampak "anotasi", baris JavaDoc berikutnya tampak
"disisipkan", dan `@Override` di bawahnya tampak "duplikat". Padahal itu **dua metode
berbeda**, masing-masing beranotasi sendiri, sepenuhnya sah.

Yang menyelamatkan dari suntingan massal yang keliru adalah aturan aman pada skrip
perbaikannya: hapus anotasi atas **hanya bila teksnya identik persis** dengan salah satu
anotasi di bawah JavaDoc. Aturan itu menolak 108 dari 110 usulan, menyisakan dua yang
memang duplikat — persis dua galat yang dilaporkan javac.

Pelajarannya bukan "regex payah", melainkan: **percayai kompilator, bukan pencocok pola.**
javac melaporkan tepat dua galat sejak awal. Angka 110 datang dari alat yang tidak mengerti
tata bahasa Java, dan seandainya perbaikannya dijalankan tanpa aturan aman, 108 anotasi sah
akan terhapus dan kerusakannya jauh lebih besar daripada yang diperbaiki.

## 4. Verifikasi

`javac -source 1.7 -target 1.7 -encoding UTF-8 -sourcepath . -cp webapp/WEB-INF/lib/*`
atas `KantinHelper.java` (menarik `Pegawai.java` lewat sourcepath): **EXIT=0**. Sebelum
perbaikan butir 2, kompilasi yang sama gagal dengan dua galat.

Isi kedua perbaikan dipastikan ada di HEAD lewat `svn cat -r HEAD`.

## 5. Commit

| Revisi | Isi | Pesan |
|---|---|---|
| (tersapu) | butir 1, tiga penjaga impor Excel | kosong |
| r83037 | butir 2, `@ManyToOne` ganda | kosong |

Penyapu yang dicatat di doc 74 §5 masih berjalan.
