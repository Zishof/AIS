# 118 — Solusi paritas data: perbaikan kunci riwayat + empat jenis impor baru

**Tanggal:** 2026-09-06
**Sifat:** perubahan kode + hasil verifikasinya.
**Pendahulu:** [116](116-uat-cmnmedika-impor-dbf-rekonsiliasi.md), [117](117-uat-banding-tenant-vs-dbf.md).
**Hasil: kesetiaan impor 100% (626/626 produk, selisih 0,00).** Sisa selisih terhadap
`STOK.DBF` bukan milik kode ini — §4.

---

## 1. Cacat: kunci idempotensi menabrakkan item baris yang sah

```java
// SEBELUM
"LEGACY-BELI-" + faktur + "-" + kodeProduk + "-" + nomor_batch + "-" + yyyyMMdd
```

Keempat unsur itu tidak cukup. Berkas legacy memuat beberapa item baris pada satu faktur yang
keempat nilainya sama — barang yang sama dibeli dua kali dengan harga berbeda, atau satu jumlah
dipecah menjadi beberapa baris. Baris kedua dan seterusnya dianggap kiriman ulang lalu dilewati.

Dihitung dari DBF, di antara baris yang **diterima** importir:

| berkas | diterima | kunci unik | baris hilang | qty hilang |
|---|---:|---:|---:|---:|
| BELI.DBF | 52.542 | 51.980 | **562** | **13.727,40** |
| JUAL.DBF | 93.989 | 93.938 | **51** | **211,33** |

Angka qty hilang **sama persis** dengan kekurangan `mutasi_stok` terhadap sumbernya.

### Perbaikan

```java
// SESUDAH -- nomor urut baris DBF ikut menyusun kunci
String barisKe = s(r, "baris_ke");
... + (barisKe.isEmpty() ? "" : "-" + barisKe)
```

Idempotensinya **tidak melemah**: nomor urut baris tetap bagi satu berkas, jadi kiriman ulang
menghasilkan kunci yang sama. Muatan lama tanpa `baris_ke` jatuh ke bentuk kunci lama dan tetap
sah.

**Migrasi:** bentuk kunci berbeda dari sebelumnya. Penyewa yang sudah mengimpor dengan bentuk lama
harus mengosongkan `mutasi_stok` legacy-nya sebelum mengimpor ulang; bila tidak, baris lama tidak
dikenali dan akan tergandakan. Dicatat di komentar kodenya.

**Penjaga:** `uji-kesetaraan-kunci-riwayat.sql` — lima blok, termasuk dua blok penjaga yang
membuktikan contohnya membedakan benar dari salah, dan bahwa idempotensinya tidak rusak.

### Hasil

| | sebelum | sesudah | diterima importir |
|---|---:|---:|---:|
| pembelian dibuat | 51.980 | **52.542** | 52.542 ✓ |
| penjualan dibuat | 93.938 | **93.989** | 93.989 ✓ |
| dilewati | 562 + 51 | **0 + 0** | ✓ |

---

## 2. Empat jenis impor baru

`JENIS_DIDUKUNG` bertambah: `opname`, `piutang_legacy`, `hutang_legacy`, `akun_legacy`.
**Tidak perlu bundel migrasi** — seluruh tabel tujuannya sudah ada di katalog v1–v19.

| jenis | sumber | tujuan | hasil |
|---|---|---|---|
| `opname` | `dataopn.dbf` | `stok_opname` + `_detail` + `mutasi_stok` | 1.771 dibuat, 13 gagal |
| `piutang_legacy` | `Tran_Piut.DBF` | `piutang_customer` | **108** dibuat |
| `hutang_legacy` | `Tran_Hut.DBF` | `hutang_supplier` | **21** dibuat |
| `akun_legacy` | `account.dbf` | `{S}.akun` | 11 dibuat, 1 dilewati |

108 dan 21 **sama persis** dengan jumlah baris ber-nilai > 0 di berkasnya. 13 kegagalan opname
seluruhnya kode barang yatim. 1 akun dilewati karena `account.dbf` memuat kode 102 dua kali.

### Tiga keputusan yang perlu disebut

**Opname menulis tiga hal, dan yang ketiga menentukan.** Kepala per tanggal, rinciannya per
produk, dan satu baris `mutasi_stok` sebesar selisihnya. Tanpa baris mutasi itu, opname hanya
menjadi catatan yang tidak menggerakkan kartu stok. Selisih nol **tidak** melahirkan mutasi —
pergerakan bernilai nol hanya mengotori kartu.

**Opname memakai `jenis = OPNAME`, bukan menumpang PENGADAAN/PENJUALAN.** Versi pertama menumpang,
dan akibatnya terukur: total PENGADAAN membengkak **211.554** unit dan PENJUALAN **194.728** unit
di atas berkas sumbernya. Saldonya benar, tetapi laporan pembelian dan penjualan ikut membengkak
dan kartu stok menyebut koreksi fisik sebagai "pengadaan" — angkanya benar, ceritanya salah.

**Status piutang/hutang adalah TAFSIR, dan dinyatakan begitu.** Sumbernya hanya menyatakan lunas
lewat ada-tidaknya `TGLBAYAR`; tidak ada rincian pembayaran yang bisa dijumlahkan. Maka
`terbayar`/`sisa` ditulis sebagai potret, dan `legacy_tafsir` merekam bahwa statusnya hasil
tafsiran — supaya pembacanya kelak tidak mengira itu hasil penjumlahan alokasi.

**`SALDO`/`MUTASI` pada `account.dbf` sengaja tidak dikirim.** Saldo akun adalah turunan dari
jurnal; menuliskannya sebagai angka tetap akan berselisih dengan jurnalnya begitu ada posting.

---

## 3. Yang justru TIDAK diimpor — dan koreksi atas doc 117

Doc 117 menyebut `batchno.dbf` (1.257 baris) sebagai celah yang membuat penelusuran batch mati.
**Itu keliru.** Diperiksa isinya:

```
batchno.dbf  aktif=1257   ber-NOBATCH=1   ber-TGLEXP=0
```

Satu baris punya nomor batch, tidak ada satu pun punya tanggal kedaluwarsa. Data batch praktis
tidak ada di aplikasi lama, sehingga **`produk_batch` kosong justru paritas yang benar.**
Mengimpornya akan membuat tenant menampilkan sesuatu yang tidak dimiliki aplikasi lama.

Koreksi skala lainnya: doc 117 mencantumkan `Tran_Piut` 832 dan `Tran_Hut` 70 baris. Yang
benar-benar berisi hanya **132** dan **68**; sisanya rekaman kosong seluruhnya.

---

## 4. Verifikasi: pemecahan selisih per komponen

Setelah perbaikan, `mutasi_stok` mereproduksi **100%** baris yang diterima importir:

```
produk dibandingkan                    626
COCOK (BELI dan JUAL keduanya)         626  (100.00%)
PENGADAAN   DBF-diterima=1.054.789,39   tenant=1.054.789,39   selisih=0,00
PENJUALAN   DBF-diterima=  238.976,51   tenant=  238.976,51   selisih=0,00
```

Penghitung `STOK.DBF` masih berselisih pada sebagian produk. Selisih itu **bukan satu hal
melainkan tiga**, dan `banding-pecah.py` memisahkannya per produk, dijendela pada `TGLOPNAME`:

| komponen | MASUK | KELUAR | produk |
|---|---:|---:|---:|
| **D−C cacat impor** (harus nol) | **0,00** | **0,00** | **0** |
| C−B penolakan aturan importir | 0,00 | 0,00 | 0 |
| B−A ketidakkonsistenan legacy sendiri | −102,00 | −113,80 | 71 |
| **tanpa selisih apa pun** | | | **449** |

Hanya D−C yang dapat diperbaiki dengan mengubah kode kita, dan nilainya nol. B−A adalah
`STOK.DBF` yang tidak sama dengan berkas transaksinya sendiri — tidak dapat diperbaiki tanpa
sumber data lain, dan itulah **batas paritas yang sesungguhnya** dari berkas-berkas ini.

### Keadaan akhir tenant

```
supplier 101   customer 333   salesperson 3   produk 626   akun 11
harga_beli_supplier 2.512      harga_jual_customer 11.685
mutasi_stok 148.217  = PENGADAAN 52.542 + PENJUALAN 93.989 + OPNAME 1.686
stok_opname 461      stok_opname_detail 1.771
piutang_customer 108 hutang_supplier 21
```

Master tetap **SETARA** pada kunci dan seluruh medan (nama, alamat, telp, termin, diskon, harga,
stok minimum).

---

## 5. Dua cacat pada alat sendiri, ikut diperbaiki

**Pelari impor menandai bongkah gagal sebagai sukses.** Importir menjawab `status: success` di
tingkat bongkah walau seluruh barisnya ditolak; pelari mencatatnya OK, sehingga percobaan ulang
melewatinya — kegagalan terkubur oleh catatan kemajuannya sendiri. Sekarang bongkah hanya ditandai
OK bila `gagal == 0`.

**`legacy_source_record_no` bertipe `integer`, dikirim sebagai teks.** PostgreSQL menolaknya, dan
karena impornya satu transaksi, **seluruh bongkah** batal, bukan satu baris — terlihat sebagai
"1.843 gagal" yang sebenarnya satu kesalahan tipe. Ditutup dengan pembantu `noBaris()`.

---

## 6. Yang masih menghalangi UAT 100%

1. **Dokumen pembelian/penjualan tidak terbentuk** — riwayat legacy sengaja hanya menjadi
   `mutasi_stok` (doc 116 §5.1). Layar yang mendaftar faktur akan kosong sementara `inventory.exe`
   menampilkannya. Ini keputusan yang belum diambil, bukan cacat.
2. **48 layar belum dibandingkan** dengan aplikasi lama.
3. Medan tanpa rumah: `produk.stok_legacy`, dan `HARGAASLI`/`DISCOUNT`/`DISCOUNT2` pada BELI.DBF.

## Berkas yang berubah

| berkas | perubahan |
|---|---|
| `SalesInventoryDbfImportHelper.java` | kunci riwayat + `noBaris()` + 3 metode impor baru |
| `SalesInventoryDbfImportTenant.java` | 4 jenis + 6 perakit SQL, termasuk `sisipMutasiOpname` |
| `uji-kesetaraan-kunci-riwayat.sql` | penjaga baru, 5 blok, semuanya LULUS |
