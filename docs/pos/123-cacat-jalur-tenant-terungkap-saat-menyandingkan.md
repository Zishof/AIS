# 123 — Enam cacat jalur tenant yang baru terungkap saat layar disandingkan

**Tanggal:** 2026-09-07
**Sifat:** catatan cacat + perbaikan + koreksi alat ukur.
**Pendahulu:** [120](120-uat-paritas-data-tuntas.md), [122](122-layar-09-10-laporan-opname.md).

---

## Ringkasan

Menyandingkan tangkapan layar baru dengan tangkapan layar aplikasi lama membongkar **enam cacat
jalur tenant** dan **empat cacat alat ukur**. Tidak satu pun terlihat sebelumnya, meskipun
paritas data sudah dinyatakan tuntas di doc 120 — dan itulah pelajaran utamanya:

> Seluruh perbandingan sebelumnya memanggil aksi dengan parameter yang **dipilih alat ukur**,
> bukan parameter yang **dikirim layar**. Aksi yang sama lulus dari alat ukur dan menampilkan
> pesan kesalahan di layar.

---

## 1. Cacat jalur tenant

### 1.1 Saringan master menyebut kolom yang hanya ada di jalur legacy

`supplierList`, `customerList`, dan `salesList` menyusun klausa `where` **sebelum** jalurnya
ditentukan, lalu menyebut kolom milik jalur legacy:

| jalur | tabel | punya `aktif`? | punya `wilayah`? |
|---|---|---|---|
| legacy | `koperasi.supplier_inventory_profile sp` | ya | ya |
| tenant | `{S}.supplier_profile sp` | **tidak** (ada di `{S}.supplier p`) | **tidak** |

Akibatnya `si_supplier_list` dengan `aktif=aktif` dan `si_customer_list`/`si_sales_list` dengan
`keyword` melempar `PSQLException: kolom sp.aktif belum ada`, yang dibungkus menjadi pesan generik
*"Data belum berubah. Silakan muat ulang halaman…"*. **Layar 01-07 seluruhnya menampilkan pesan
kesalahan**, dan layarnya memang mengirim `aktif` sebagai bawaan.

Padanan tenantnya **sudah ditulis** — `kunciSupplier()`, `aktifSupplier()`, `kunciCustomer()`,
`aktifCustomer()`, `kunciSales()`, `aktifSales()`, `dukungWilayahMitra()` — tetapi **tidak satu pun
dipanggil**. Perbaikannya hanya menyambungkan, bukan menulis SQL baru.

Ikut diperbaiki: `sales_owner_id` (tenant memakai `customer.salesperson_id`, bukan
`cp.sales_owner`) dan `sort=wilayah` (tidak ada padanannya; jatuh ke urutan nama).

### 1.2 `saldo_stok` tidak pernah diisi impor — layar Persediaan kosong

Lingkup toko pada model tenant ditegakkan lewat gudang, dan daftar barisnya dibatasi produk yang
punya baris `saldo_stok`. Tabel itu **kosong**: impor legacy menulis `mutasi_stok` (buku besarnya)
tetapi tidak pernah membentuk `saldo_stok`.

`si_inventory_balance` karena itu mengembalikan **0 untuk setiap pengguna bertoko**, sementara
admin melihat 626. Gejalanya bukan galat melainkan **daftar kosong** — jenis kegagalan yang paling
mudah disangka "memang belum ada datanya".

Ditambahkan jenis impor `saldo_stok`: murni turunan, dapat diulang, membangun ulang saldo dari
`mutasi_stok`. Plus baris **bernilai nol** untuk 70 produk yang belum pernah bergerak — aplikasi
lama menampilkan seluruh 626 jenis barang, dan produk bersaldo nol justru yang ingin dilihat.
Gudangnya **tidak ditebak**: hanya diisi bila tokonya punya tepat satu gudang.

Hasil: 556 (bermutasi) + 70 (nol) = **626 = angka layar lama**.

### 1.3 Piutang tanpa faktur pasangan lenyap dari layar pemilik

`si_receivable_list` mengembalikan 108 untuk admin tetapi **107** untuk pemilik. Piutang
`2402-000091` (Rp 273.000) hilang karena lingkup toko ditegakkan lewat `f.toko_id`, sedangkan
faktur itu **tidak ada di JUAL.DBF sama sekali** — ketidakkonsistenan legacy, dan `f.toko_id`
bernilai NULL.

Yang hilang adalah **uang yang ditagihkan**, dan satu-satunya pengguna yang dapat melihatnya
adalah admin — yang justru dirancang tanpa toko. Penyaringnya kini menerima baris bertoko tidak
diketahui. Tampil dua kali pada tenant bertoko banyak lebih baik daripada hilang diam-diam:
hanya yang pertama yang kelihatan lalu dapat diperbaiki.

### 1.4 Nama toko tidak pernah diisi pada cabang toko-aktif

`isiTokoDariPedagangAtauMultiToko` mengisi `tokoNama` hanya pada cabang Pedagang. Akibatnya kepala
cetakan menulis **"Toko: (global)"** untuk pemilik yang jelas terikat toko.

Diisi di dispatcher, **sesudah** konteks tenant terpasang, lewat SQL asli — bukan dengan memuat
entitas `Toko` yang ber-schema tersemat dan akan membaca `koperasi.toko`. Nama yang keliru lebih
buruk daripada nama yang kosong: yang kosong kelihatan.

### 1.5 dan 1.6

Dua cacat sebelumnya sudah tercatat di [122](122-layar-09-10-laporan-opname.md): gerbang
`si_stock_count_*` tanpa handler, dan `TenantRbac.area()` yang hanya mengenal ejaan `stok`
sedangkan gerbangnya memakai `stock`.

---

## 2. Cacat alat ukur — dan mengapa ini bagian terpenting dokumen ini

| alat | cacatnya | akibatnya |
|---|---|---|
| `uat_48_layar_test.dart` | penjaganya hanya "PNG > 5 KB" | layar bergalat tercatat **OK** tiga penangkapan berturut-turut, dan sempat masuk dokumen bukti |
| `banding-api.py` | memanggil aksi **tanpa** parameter saringan | tiga aksi master dinyatakan COCOK padahal melempar pengecualian di layar |
| `banding-api.py` | mencari id produk lewat `si_product_list` — aksi yang tidak pernah ada | `si_inventory_ledger` **tidak pernah diuji sekali pun**; barisnya tercetak "(dilewati)", terlihat seperti keputusan padahal kegagalan |
| `banding-dbf.py` | membandingkan saldo dengan **ringkasan** `STOK.DBF` | verdikt merah permanen yang tak dapat diperbaiki dari sisi kode, sehingga baris verdiktnya jadi kebiasaan diabaikan |

Ketiga yang pertama punya bentuk yang sama: **alat ukur menguji sesuatu yang lebih mudah daripada
yang ingin diketahui**, lalu melaporkan hasilnya seolah menjawab pertanyaan aslinya.

### Perbaikan verdikt saldo stok

`STOK.DBF` adalah ringkasan hitungan aplikasi lama, dan ia tidak sepakat dengan berkas transaksinya
sendiri. Membandingkan dengannya berarti merah selamanya. Yang **dapat** diuji: apakah saldo tenant
setia pada berkas transaksi.

```
acuan  = fisik opname di TGLOPNAME + (BELI - JUAL) sesudah tanggal itu
tenant = fisik opname di TGLOPNAME + SUM(mutasi)   sesudah tanggal itu

produk setia pada berkas transaksi : 520
TIDAK setia (cacat impor)          : 0
```

Selisih −15,13 unit terhadap `STOK.DBF` tetap **dicetak**, bukan disembunyikan, dan disebut apa
adanya: ringkasan legacy tidak sepakat dengan transaksinya sendiri, pada 79 dari 520 produk.

Perbandingan lama (`AWAL+MASUK−KELUAR` vs jumlah SELURUH mutasi) melaporkan **−825.141,83 unit**
pada 375 produk. Angka itu sepenuhnya cacat pengukuran: `MASUK`/`KELUAR` di `STOK.DBF` menghitung
**sejak `TGLOPNAME`**, bukan sepanjang masa. Bahwa `AWAL` memang hitungan fisik opname dibuktikan
lebih dulu, bukan diasumsikan: **499 dari 520** cocok persis dengan `stok_opname_detail`. Batas
harinya `> TGLOPNAME` juga diukur, bukan ditebak:

| batas | cocok | sisa |
|---|---:|---:|
| `> TGLOPNAME` | **441** | **−15,13** |
| `>= TGLOPNAME` (opname ikut) | 40 | +46.084,87 |
| `>= TGLOPNAME` (opname dibuang) | 426 | −599,13 |

---

## 3. Hasil sesudah perbaikan

```
banding-dbf         bersih      banding-saldo-api   bersih
banding-sisa        bersih      banding-opname-api  bersih
banding-mutasi      bersih      banding-pecah       bersih
banding-dokumen     bersih

banding-api (muklis, Pemilik)  : SELURUH AKSI COCOK
banding-api (demo,   Admin)    : SELURUH AKSI COCOK
18 varian saringan layar       : seluruhnya OK
Penangkapan layar              : 17 berhasil, 0 gagal, 0 GALAT-LAYAR
si_inventory_balance           : 626 = 626 (angka layar lama)
si_receivable_list             : 108 = 108 untuk kedua aktor
```

---

## 4. Satu perbedaan yang BUKAN cacat, dan perlu diputuskan pemilik usaha

Layar lama menulis **"Jml Cust.: 334"**, aplikasi baru **333**. `CUSTOMER.DBF` berisi 334 baris
tetapi hanya **333 kode unik**: kode `00375` ("ARI TK") tercatat dua kali dengan alamat berbeda
(C3 dan C2). Legacy menghitung baris; model tenant berkunci unik. Bukan data hilang, melainkan
aturan yang berbeda — dan keputusan menggabung atau memisahkan keduanya ada pada pemilik usaha,
bukan pada migrasi.

---

## 5. Yang tersisa

1. Perbandingan berdampingan 48 layar sudah tersedia sebagai dokumen; **putusan kesepadanan per
   layar ada pada penguji**, dan 20 layar ditandai `PERLU DINILAI` dengan catatan apa yang harus
   diperiksa.
2. Layar sesi sales (SPJ, Nota, Biaya, Rekonsiliasi) belum berdata uji.
3. Medan legacy tanpa rumah: `HARGAASLI`/`DISCOUNT`/`DISCOUNT2` pada BELI.DBF, `produk.stok_legacy`.
