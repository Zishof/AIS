# 117 — UAT cmnmedika: perbandingan data tenant vs DBF legacy

**Tanggal:** 2026-09-06
**Sifat:** hasil verifikasi data. Tidak ada kode produksi yang diubah.
**Pendahulu:** [116](116-uat-cmnmedika-impor-dbf-rekonsiliasi.md).
**Verdikt: data BELUM setara.** Tiga sebab, semuanya terukur dan terjelaskan.

---

## Ringkasan

| bagian | hasil |
|---|---|
| Master (supplier, customer, sales, produk) | **SETARA** — kunci dan seluruh medan |
| Mutasi vs sumbernya (BELI/JUAL) | 488/626 produk cocok; selisih **terjelaskan penuh** |
| Saldo stok vs STOK.DBF | **tidak dapat dibandingkan langsung** — lihat §3 |
| Lima kumpulan data | **belum diimpor sama sekali** — §4 |

---

## 1. Master — SETARA

Dibandingkan kunci **dan nilai tiap medan**, bukan sekadar jumlah baris (jumlah yang sama tetap
mungkin dengan kolom yang salah petakan):

| entitas | DBF | tenant | medan yang dibandingkan | hasil |
|---|---:|---:|---|---|
| supplier | 101 | 101 | nama, alamat, telp, termin | SETARA |
| customer | 333 | 333 | nama, alamat, telp, termin, diskon | SETARA |
| salesperson | 3 | 3 | nama | SETARA |
| produk | 626 | 626 | nama, harga_beli, harga_jual, stok_minimum | SETARA |

Perbandingan ini harus menjembatani perbedaan bentuk: schema tenant memisahkan identitas
(`supplier`) dari profil (`supplier_profile.alamat1/telp/syarat_bayar_hari`), sedangkan legacy
menyatukannya dalam satu baris.

---

## 2. Mutasi stok vs BELI/JUAL — selisihnya satu cacat, dan terukur tepat

488 dari 626 produk (77,96%) cocok tepat pada BELI **dan** JUAL. Sisanya 138 produk berselisih,
dan tenant **selalu kurang**:

```
PENGADAAN   DBF-diterima = 1.054.789,39   tenant = 1.041.061,99   selisih = 13.727,40
PENJUALAN   DBF-diterima =   238.976,51   tenant =   238.765,18   selisih =    211,33
```

### Sebabnya: kunci idempotensi terlalu kasar

```java
// SalesInventoryDbfImportHelper
String kunci = potong((pembelian ? "LEGACY-BELI-" : "LEGACY-JUAL-") + faktur + "-"
        + kodeProduk + "-" + s(r, "nomor_batch") + "-"
        + new SimpleDateFormat("yyyyMMdd").format(tanggal), 128);
```

Kunci itu **tidak memuat kuantitas, harga, maupun nomor baris**. Dua item baris yang sah pada
faktur yang sama, produk sama, batch sama, dan tanggal sama karena itu bertabrakan — yang kedua
dianggap kiriman ulang dan dilewati.

Dihitung langsung dari DBF, di antara baris yang **diterima** importir:

| berkas | diterima | kunci unik | baris hilang | qty hilang |
|---|---:|---:|---:|---:|
| BELI.DBF | 52.542 | 51.980 | **562** | **13.727,40** |
| JUAL.DBF | 93.989 | 93.938 | **51** | **211,33** |

Angka qty hilang **sama persis** dengan kekurangan tenant di atas. Selisihnya karena itu bukan
misteri: 613 baris legacy yang sah lenyap sebagai "duplikat".

Importir memang melaporkannya sebagai `dilewati` — jadi tidak disembunyikan — tetapi terhadap
aplikasi lama ini tetap **kehilangan data**. Kunci itu perlu memuat nomor urut baris DBF
(`legacy_source_record_no` sudah ada di katalog dan cocok untuk itu).

---

## 3. Saldo stok — STOK.DBF bukan pembanding yang sah

Perbandingan pertama membandingkan `AWAL + MASUK - KELUAR` (legacy) dengan
`SUM(arah * kuantitas)` sepanjang masa (tenant): 514 dari 626 produk berselisih, total 802.296
vs 7.497 unit.

**Asumsinya yang keliru, bukan datanya.** `STOK.DBF` memuat `TGLOPNAME`, dan ketiga kolom itu
adalah penghitung **sejak opname terakhir**, bukan sepanjang masa:

```
000102  AWAL=24  MASUK=1878  KELUAR=1897  ->  saldo 5     TGLOPNAME=2025-11-04
```

Setelah dijendela per `TGLOPNAME`, 419 dari 520 produk (80,6%) cocok pada MASUK **dan** KELUAR.

Tetapi pembandingnya tetap tidak sah, dan ini terbukti tanpa menyentuh basis data sama sekali:
**penghitung `STOK.DBF` tidak konsisten dengan `BELI/JUAL.DBF` milik legacy sendiri.**

| batas tanggal | produk yang MASUK dan KELUAR-nya cocok |
|---|---|
| `>= TGLOPNAME` | 435 / 520 (83,7%) |
| `> TGLOPNAME` | 449 / 520 (86,3%) |

71 produk berselisih pada varian terbaik, dan polanya khas — `MASUK` hampir selalu cocok,
`KELUAR` yang meleset:

```
kode        MASUK.dbf  dari BELI  KELUAR.dbf  dari JUAL
000403         626.00     626.00      640.00     636.00
000808         700.00     700.00      895.00     902.00
001122         255.00     255.00      207.00     196.00
```

Artinya ada sumber pergerakan stok **di luar** BELI/JUAL — terutama opname fisik. Berkasnya ada
(`dataopn.dbf`) dan belum diimpor.

Kesimpulan metodologisnya: menilai impor dengan `STOK.DBF` akan menyalahkan impor atas
ketidakkonsistenan yang sudah ada di data legacy. Pembanding yang sah adalah berkas yang benar-
benar menjadi sumber impor — dan itulah §2.

---

## 4. Lima kumpulan data yang belum diimpor sama sekali

Ekstraktor dan importir sama-sama hanya menangani **8 jenis**
(`supplier, customer, sales, produk, harga_beli, harga_jual, pembelian_legacy, penjualan_legacy`).
Direktori legacy memuat lebih dari itu:

| berkas | baris aktif | isinya | akibat bila tetap kosong |
|---|---:|---|---|
| `dataopn.dbf` | 1.843 | opname fisik (`STOKKOMP`, `STOKFISIK`) | saldo stok takkan pernah cocok; §3 |
| `batchno.dbf` | 1.257 | `NOBATCH`, `TGLEXP`, `JUMLAH` | `produk_batch` kosong — penelusuran batch mati |
| `Tran_Piut.DBF` | 832 | piutang beserta pembayarannya | layar piutang kosong |
| `Tran_Hut.DBF` | 70 | hutang beserta pembayarannya | layar hutang kosong |
| `account.dbf` | 12 | bagan akun | `{S}.akun` kosong |

### Koreksi atas laporan sebelumnya

Angka yang pernah saya sebut untuk berkas-berkas ini (`8.863 Tran_Hut`, `36.010 Tran_Piut`,
`2.875 batchno`) adalah **hitungan header DBF**, yang mencakup rekaman bertanda hapus. Baris
aktifnya jauh lebih sedikit: **70**, **832**, dan **1.257**. Perbedaannya besar dan mengubah
taksiran pekerjaan; angka di tabel atas adalah yang benar.

### `JUA-rusakL.DBF` BUKAN data yang hilang

Berkas 12 MB berisi 88.592 baris ini sempat tampak seperti kumpulan penjualan kedua. Ternyata
cuplikan lama `JUAL.DBF`:

```
irisan kunci (faktur, produk, tanggal) : 88.535
hanya di JUAL.DBF                      :  5.486
hanya di JUA-rusakL.DBF                :     12
```

Tanggalnya juga berhenti lebih awal (2026-06-30 vs 2026-08-04). Tidak perlu diimpor.

---

## 5. Yang harus dikerjakan agar data setara

1. **Perbaiki kunci idempotensi riwayat** — sertakan nomor urut baris DBF. Mengembalikan 613
   baris (13.938,73 unit). Ini satu-satunya cacat impor yang ditemukan.
2. **Impor `dataopn.dbf`** — tanpa opname, saldo stok tidak akan pernah cocok dengan layar lama.
3. **Impor `batchno.dbf`** ke `produk_batch` — inti bagi distributor farmasi.
4. **Impor `Tran_Piut` / `Tran_Hut`** — layar piutang dan hutang kini kosong.
5. **Impor `account.dbf`** ke `{S}.akun`, sesuai keputusan (a) akuntansi se-tenant.
6. Ditambah yang sudah tercatat di [116](116-uat-cmnmedika-impor-dbf-rekonsiliasi.md) §5:
   dokumen pembelian/penjualan tidak terbentuk (hanya mutasi), dan 48 layar belum dibandingkan.

Butir 1–5 menuntut perluasan `SalesInventoryDbfImportHelper` dan ekstraktornya — pekerjaan kode
di repo bersama, bukan sekadar penyetelan UAT.

---

## Alat verifikasi (di luar SVN, `C:\opt\uat-inventory`)

| berkas | menjawab |
|---|---|
| `banding-dbf.py` | master medan-demi-medan; saldo stok; pasangan harga; jejak faktur |
| `banding-stok.py` | MASUK/KELUAR per produk, dijendela `TGLOPNAME` |
| `banding-stok-sumber.py` | apakah STOK.DBF konsisten dengan BELI/JUAL legacy sendiri |
| `banding-mutasi.py` | kesetiaan impor: mutasi_stok vs baris yang diterima importir |
| `analisis-gagal.py` | menjelaskan 8.605 penolakan dari DBF-nya sendiri |

Semuanya gagal keras bila kueri salah, dan mencetak contoh nyata setiap kali berselisih — angka
"berbeda" tanpa contoh tidak dapat ditindaklanjuti.
