# 17 — Bagan akun: format Accurate & Bersihkan Akun

Permintaan pemilik produk pada layar **Kode Akun** POS Desktop:

> *"Ikuti format Accurate terlampir"* (unduh & unggah), dan
> *"Tambahkan menu bersihkan akun jika login sebagai `Common.apakahAdmin == true`"*.

Berkas rujukannya: `akun-perkiraan (1).xlsx` — 423 baris, satu lembar **Daftar Akun**.

---

## 1. Format Accurate, dibaca dari berkasnya sendiri

Kolomnya (baris 1 berkas asli):

| Kolom | Judul | Dipakai aplikasi |
|---|---|---|
| A | `No. ` | nomor urut, dibuat ulang saat unduh |
| B | `Tipe Akun` | **disimpan** pada kolom baru `akunting.akun.tipe_akun` |
| C | `Kode Perkiraan` | kode akun |
| D | `Nama` | nama akun |
| E | `Akun Induk` | kode induk — dicocokkan ke akun yang sudah ada |
| F | `Mata Uang` | selalu `IDR` pada berkas ini; diekspor `IDR` |
| G | `Saldo Awal` | **tidak diimpor** — lihat bagian 3 |
| H | `per Tgl` | **tidak diimpor** |
| I | `Kurs Saldo (Jika Asing)` | selalu `1`; diekspor `1` |
| J | `Cabang Saldo` | tidak dipakai |
| K | `Catatan` | keterangan akun |

**Tipe Akun** yang muncul di berkas itu, beserta jumlah barisnya: `EXPS` 177, `BANK` 51,
`REVE` 32, `AREC` 22, `OCAS` 22, `FASS` 20, `OEXP` 16, `DEPR` 15, `APAY` 15, `OCLY` 10,
`LTLY` 10, `OINC` 10, `EQTY` 8, `COGS` 8, `INTR` 7.

---

## 2. Kenapa `tipe_akun` DISIMPAN, bukan disimpulkan

Aplikasi ini menyimpan posisi (`debetCredit`) dan grup akun, bukan penggolongan Accurate.
Menyimpulkan kembali tipe dari keduanya mustahil tepat: `BANK` dan `OCAS` sama-sama debet,
`APAY` dan `EQTY` sama-sama kredit. Karena itu ditambahkan kolom `akunting.akun.tipe_akun`
(lewat Hibernate, tanpa SQL manual) sehingga berkas yang diunduh kembali **identik** dengan
yang diunggah.

Sebaliknya, posisi debet/kredit memang **disimpulkan** dari tipe — berkas Accurate tidak punya
kolom posisi:

| Saldo normal | Tipe |
|---|---|
| Debet | `BANK` `AREC` `OCAS` `INTR` `FASS` `EXPS` `COGS` `OEXP` |
| Kredit | `DEPR` `APAY` `OCLY` `LTLY` `EQTY` `REVE` `OINC` |

> `DEPR` (akumulasi penyusutan) melekat pada aset tetapi saldo normalnya **kredit**. Ia satu-
> satunya tipe "berbau aset" yang tidak boleh ikut daftar debet, jadi ia yang paling mungkin
> salah petakan — karena itu dijadikan kasus uji tersendiri.

Bila berkas kebetulan membawa kolom Posisi, isi kolom itu yang menang; penyimpulan dari tipe
hanya cadangan.

---

## 3. Saldo awal SENGAJA tidak diimpor

Berkas rujukan memuat 186 baris bersaldo awal. Kolomnya tetap disediakan agar berkasnya
berbentuk Accurate, tetapi **tidak dikirim ke server** saat unggah.

Alasannya: saldo awal dibukukan lewat layar **Saldo Awal (Neraca Awal)** yang menjurnalnya
dengan benar. Memasukkannya diam-diam dari berkas bagan akun akan membuat dua sumber angka
untuk hal yang sama, dan yang satu tidak berjurnal. Bila memang diinginkan, itu pekerjaan
tersendiri yang menyambung ke modul Saldo Awal — bukan efek samping unggah bagan akun.

---

## 4. Bersihkan Akun (khusus administrator)

Aksi server baru `kode_akun_bersihkan`.

**Gerbangnya** `Common.getApakahAdminLain(tbmuser)` — sumber kebenaran yang sama dengan
bendera `isAdmin` yang dikirim ke klien, jadi tombolnya memakai `Sesi.instance.isAdmin`.
Penegakan sesungguhnya tetap di server; klien hanya menyembunyikan tombol.

**Yang dihapus**: akun yang **belum dipakai satu pun baris jurnal** (`akunting.transaksi`)
**dan tidak punya turunan**.

**Yang tidak pernah tersentuh**: akun terpakai jurnal, akun yang masih punya anak, dan akun
yang masih dirujuk data lain (bank, jenis transaksi, master aset). Yang terakhir dijaga kunci
asing basis data — kegagalannya ditangkap **per akun**, akun itu dilewati, dan sisanya tetap
diproses.

**Selalu dua langkah.** `praTinjau: true` tidak menghapus apa pun; balasannya hanya daftar
akun yang akan dihapus. Layar menampilkan jumlah beserta 50 contoh pertama, dan baru memanggil
penghapusan sesungguhnya setelah pengguna menekan Hapus. Menghapus ratusan akun tidak boleh
terjadi karena salah klik.

---

## 5. Hasil uji

### Jalur TULIS dengan berkas aslinya (`TesAkunAccurate`)

12 baris diambil dari `akun-perkiraan (1).xlsx` — rantai induk-anak Kas & Bank lima tingkat,
tiga tingkat `DEPR`, plus `FASS`/`APAY`/`EQTY`/`REVE` — dipetakan dengan pemetaan kolom yang
sama dengan layar, diberi awalan kode `UATACC-`, dan disapu kembali di akhir.

| Tahap | Hasil |
|---|---|
| Impor 12 baris | **dibuat 12, ditolak 0** |
| Baca ulang: tipe akun | **12/12 cocok** dengan berkas |
| Baca ulang: rantai induk | **12/12 cocok** |
| Posisi debet/kredit dari tipe | **12/12 benar**, termasuk ketiga `DEPR` = kredit |
| Impor ULANG berkas yang sama | dibuat 0, diperbarui 12 — jumlah akun tetap, **tidak berganda** |
| Pembersihan | 0 baris uji tersisa |

### Aksi Bersihkan Akun (`TesBersihkanAkun`)

| Tahap | Hasil |
|---|---|
| Pratinjau | 234 kandidat, **sama persis** dengan hitungan SQL bebas (311 akun; 0 terpakai jurnal; 77 punya turunan) |
| Tanpa pengguna (bukan admin) | **ditolak**, status 91 |

> **Jalur HAPUS sungguhan sengaja TIDAK dijalankan.** Basis UAT ini dipakai bersama sesi lain
> dan 234 dari 311 akunnya masuk kandidat — menghapus bagan akun mereka bukan ongkos yang
> pantas untuk sebuah uji. Yang sudah dibuktikan: daftar kandidatnya tepat dan gerbangnya
> bekerja. Perilaku hapus per-akun (lewati yang ditahan kunci asing) **belum** dijalankan.
>
> Basis UAT ini juga kebetulan hanya berisi pengguna berstatus admin, sehingga penolakan
> diuji lewat pemanggilan tanpa pengguna sama sekali.

### Sisi klien

| Uji | Hasil |
|---|---|
| `test/kode_akun_format_accurate_test.dart` (baru, 5 uji) | **LULUS** — urutan 11 kolom, indeks pembacaan unggah, saldo awal tidak dikirim, tombol digerbangi `Sesi.isAdmin`, dan urutan pratinjau → dialog → hapus |
| Seluruh suite `apps/ebisnis` | **LULUS**, 294 uji |
| `flutter analyze` | 51 temuan, sama dengan garis dasar; tidak ada `error` |

---

## 6. Berkas yang disentuh

| Berkas | Perubahan |
|---|---|
| `ais/database/model/akunting/Akun.java` | kolom `tipe_akun` |
| `ais/action/servlet/api/KodeAkunApiHelper.java` | `tipeAkun` pada daftar & impor, `posisiDariTipeAccurate`, aksi `akunBersihkan`, pendaftaran dispatcher |
| `apps/ebisnis/lib/screens/kode_akun_screen.dart` | 11 kolom Accurate untuk unduh/unggah, tombol & alur Bersihkan Akun |
| `apps/ebisnis/test/kode_akun_format_accurate_test.dart` | **baru** |
