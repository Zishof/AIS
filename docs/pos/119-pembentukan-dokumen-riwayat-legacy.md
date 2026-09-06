# 119 — Pembentukan dokumen dari riwayat legacy: data tenant setara dengan DBF

**Tanggal:** 2026-09-06
**Sifat:** perubahan kode + hasil verifikasinya.
**Pendahulu:** [116](116-uat-cmnmedika-impor-dbf-rekonsiliasi.md),
[117](117-uat-banding-tenant-vs-dbf.md), [118](118-solusi-paritas-data-impor-legacy.md).

**Hasil: dokumen pembelian dan penjualan setara — 5.578 dan 9.850 dokumen, seluruhnya cocok pada
jumlah baris DAN nilai total.**

---

## 1. Koreksi atas keberatan di doc 116 §5.1

Doc 116 menyatakan riwayat legacy sengaja hanya menjadi `mutasi_stok` karena membentuk dokumen
berarti "mengarang struktur yang tidak ada di sumbernya".

**Keberatan itu salah sasaran.** `BELI.DBF`/`JUAL.DBF` memang tidak memuat *rekaman* header —
tetapi memuat **informasinya**: nomor faktur, kode mitra, dan tanggal ada pada **setiap baris**.
Mengelompokkan baris yang bersepakat pada ketiganya adalah normalisasi, bukan penciptaan; di
aplikasi lama baris-baris itu memang satu faktur yang sama, dan layarnya menampilkannya begitu.

Yang benar dari doc 116: tanpa langkah ini, layar daftar faktur kosong sementara aplikasi lama
menampilkan puluhan ribu baris.

---

## 2. Tiga keputusan rancangan

**Total diakumulasi, bukan dihitung ulang.** Baris satu faktur tersebar di beberapa bongkah
permintaan; saat baris pertama masuk, totalnya belum dapat diketahui. Akumulasi hanya dijalankan
bila rinciannya benar-benar tersisip, sehingga kiriman ulang tidak menggelembungkan total.

**Penjaga rincian memakai `baris_ke`** — pembeda yang sama dengan kunci mutasi. Satu baris DBF
karena itu menghasilkan tepat satu mutasi **dan** tepat satu rincian dokumen; kedua sisi konsisten
menurut definisi, bukan kebetulan.

**`batch_no` dan `expiry_date` menempati kolomnya sendiri.** Pada `mutasi_stok` keduanya hanya
dapat dititipkan sebagai teks di `keterangan` karena tabel itu tak punya tempatnya; pada rincian
dokumen kolomnya ada.

---

## 3. Mitra yang tidak dikenal: PERINGATAN, bukan kegagalan

Kepala dokumen menuntut mitra (kolomnya NOT NULL). Versi pertama melempar galat dengan alasan
"dokumen separuh jadi lebih menyesatkan daripada tidak ada dokumen".

**Alasan itu keliru, dan akibatnya terukur.** Mutasi stok disisipkan LEBIH DULU, jadi melempar
tidak membatalkannya — barisnya tetap tersimpan, hanya dilaporkan sebagai `gagal`. Hasilnya
230 baris `JUAL.DBF` tercatat gagal padahal mutasinya utuh:

```
mutasi_stok PENJUALAN      93.989
faktur_penjualan_detail    93.759     <- selisih 230, semuanya customer yatim
```

Sekarang keadaan itu dicatat sebagai peringatan: mutasinya sah dan tetap tersimpan, dokumennya
tidak terbentuk, dan pelaporannya menyebut apa adanya.

---

## 4. Bug: `legacy_tafsir` adalah `varchar(64)`

Teks tafsir yang ditulis semula ~98 aksara. PostgreSQL menolaknya
("nilai terlalu panjang untuk tipe character varying(64)") dan — karena impornya satu transaksi —
**seluruh bongkah** batal, bukan satu baris. Selama beberapa menit tidak ada satu baris pun
mendarat tanpa penyebab yang terlihat dari luar.

Teksnya dipendekkan, dan dibungkus `potong(..., 64)` sebagai penjaga supaya perubahan kata di
kemudian hari tidak menggagalkan impor lagi. `nomor_dokumen` dan `batch_no` (juga `varchar(64)`)
ikut dijaga meskipun nilai legacy-nya jauh lebih pendek.

---

## 5. Hasil verifikasi

### Dokumen — `banding-dokumen.py`

| | dokumen | cocok (baris **dan** total) | hanya-DBF | hanya-tenant |
|---|---:|---:|---:|---:|
| PEMBELIAN (`BELI.DBF`) | 5.578 | **5.578** | 0 | 0 |
| PENJUALAN (`JUAL.DBF`) | 9.850 | **9.850** | 0 | 0 |

Nol dokumen berbeda jumlah baris; nol berbeda nilai total. Pada penjualan, 230 baris (menyentuh 51
dokumen) tidak menghasilkan rincian karena customernya yatim — dihitung terpisah, bukan
disembunyikan sebagai selisih.

### Master, mutasi, dan saldo — tidak berubah

```
MASTER            supplier 101/101, customer 333/333, sales 3/3, produk 626/626 -- SETARA
KESETIAAN MUTASI  626/626 produk (100%), PENGADAAN & PENJUALAN selisih 0,00
SALDO (pecah)     cacat impor 0,00 pada 0 produk; legacy tak konsisten -102,00/-113,80 pada 71;
                  449 dari 520 produk tanpa selisih apa pun
```

### Keadaan tenant

```
supplier 101   customer 333   salesperson 3   produk 626   akun 11
harga_beli_supplier 2.512      harga_jual_customer 11.685
mutasi_stok 148.217  = PENGADAAN 52.542 + PENJUALAN 93.989 + OPNAME 1.686
pembelian 5.578      pembelian_detail 52.542
faktur_penjualan 9.850  faktur_penjualan_detail 93.759
stok_opname 461      stok_opname_detail 1.771
piutang_customer 108 hutang_supplier 21
```

---

## 6. Catatan operasional: `TRUNCATE ... CASCADE` lebih luas dari dugaan

Saat menyiapkan impor ulang, `TRUNCATE cmnmedika.faktur_penjualan ... CASCADE` ikut menyapu
`piutang_customer` dan `hutang_supplier` lewat kunci asing `faktur_penjualan_id`, serta
`alokasi_penerimaan_piutang`, `draft_penjualan_detail`, dan `surat_perintah_sales_nota`.

Pada klaster UAT sekali-pakai ini tidak berbahaya — semuanya dapat dibentuk ulang dari DBF. Pada
instalasi berisi data, perintah semacam itu **tidak boleh dijalankan tanpa memeriksa lebih dulu
apa yang ikut terhapus**; `NOTICE` dari PostgreSQL menyebutkannya, tetapi sesudah kejadian.

---

## 7. Yang tersisa untuk UAT 100%

Data sudah setara sejauh yang dapat dibuktikan dari berkas DBF. Yang belum:

1. **48 layar belum dibandingkan** dengan `inventory.exe` — paritas fungsional, bukan data.
2. Batas yang tak dapat ditembus: 71 produk yang penghitung `STOK.DBF`-nya tidak sama dengan
   `BELI/JUAL.DBF` miliknya sendiri (doc 117 §3).
3. Medan tanpa rumah: `produk.stok_legacy`, serta `HARGAASLI`/`DISCOUNT`/`DISCOUNT2` pada BELI.DBF.

## Alat verifikasi baru

| berkas | menjawab |
|---|---|
| `banding-dokumen.py` | jumlah dokumen, jumlah baris, dan nilai total per faktur |
