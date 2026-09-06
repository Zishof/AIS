# 124 — Paritas tampilan 48 layar: hasil penilaian, dan satu celah yang terukur

**Tanggal:** 2026-09-07
**Sifat:** hasil penilaian paritas tampilan.
**Pendahulu:** [120](120-uat-paritas-data-tuntas.md), [122](122-layar-09-10-laporan-opname.md),
[123](123-cacat-jalur-tenant-terungkap-saat-menyandingkan.md).

---

## Verdikt

**Paritas DATA: tuntas** (doc 120, 123).
**Paritas TAMPILAN: 46 dari 48 layar sepadan**; **1 celah nyata** dan **2 menunggu keputusan
pemilik usaha**.

Dokumen bukti: `UAT-Sanding-48-Layar-Lama-vs-Baru.docx/pdf` (50 halaman, landscape) — tiap layar
lama disandingkan dengan tangkapan NYATA aplikasi baru pada tenant `cmnmedika`.

| status | jumlah | arti |
|---|---:|---|
| SEPADAN | 19 | fungsi dan informasinya sama; datanya sudah diadu dengan DBF |
| SEPADAN — bentuk berbeda | 10 | fungsi sama, idiom UI berbeda dan disengaja |
| CETAK / EKSPOR | 9 | "layar" lama adalah pratinjau cetak / dialog ekspor |
| MENU | 2 | layar lama adalah menu; kini item sidebar |
| SEPADAN — menunggu data uji | 4 | layarnya lengkap; berkas legacy tidak menyimpan dokumen jenis itu |
| **BELUM SEPADAN** | **2** | **layar 12 dan 13 — lihat §1** |
| Perlu keputusan pemilik usaha | 2 | layar 6 dan 20 — lihat §2 |

Versi pertama dokumen sanding meninggalkan **22 layar tanpa putusan** ("perlu dinilai penguji").
Itu memindahkan pekerjaan, bukan mengerjakannya. Tiap verdikt di atas kini ditulis sesudah kedua
gambarnya dibuka, dan untuk yang berselisih, sesudah angkanya diukur.

---

## 1. Celah nyata: harga jual KREDIT vs TUNAI (layar 12 dan 13)

Aplikasi lama memisahkan dua harga jual per produk, lengkap dengan margin masing-masing:

```
DAFTAR HARGA JUAL
  #KODE  Nama Barang  STOK  Satuan  Hrg Beli
         HARGA JUAL (KREDIT)   HARGA JUAL (TUNAI)   RL % (KREDIT)   RL % (TUNAI)
```

Sumbernya `STOK.DBF`: kolom **`HARGAJUAL`** (kredit) dan **`HARGAJUAL2`** (tunai).

Model tenant hanya punya **satu** harga jual standar:

| lapisan | kolom |
|---|---|
| produk | `harga_jual_standar` — satu nilai |
| per customer | `harga_jual_customer.harga` |
| daftar harga | `price_list_detail.harga` |

Tidak ada padanan untuk harga kedua per produk.

### Ini bukan perbedaan kosmetik — sudah diukur

```
produk STOK.DBF               626
  HARGAJUAL2 kosong           459
  keduanya nol                 34
  kredit == tunai               1
  kredit != tunai (NYATA)     132   <-- 21% dari seluruh produk
```

Contoh: `000301` kredit **Rp 217.000** vs tunai **Rp 206.000**; `000403` Rp 155.000 vs Rp 145.000;
`000412` kredit Rp 0 tetapi tunai Rp 54.000.

**Konsekuensinya:** kolom `HARGA JUAL (TUNAI)` dan `RL % (TUNAI)` pada layar 12, beserta
cetakannya (layar 13), tidak punya sumber pada model baru. Nilai tunai untuk 132 produk itu
**tidak terbawa** oleh impor.

### Dua jalan, dan keduanya keputusan pemilik usaha

1. **Tambahkan harga jual kedua pada model tenant** (`produk.harga_jual_tunai` atau daftar harga
   ber-jenis), lalu impor `HARGAJUAL2`. Menutup celahnya sepenuhnya.
2. **Nyatakan kebijakan dua harga dihentikan** — satu harga jual, diskon tunai ditangani lewat
   mekanisme diskon. Ini keputusan bisnis, bukan teknis, dan harus dinyatakan eksplisit supaya
   tidak terbaca sebagai data yang hilang diam-diam.

Selama salah satunya belum diambil, layar 12 dan 13 **tidak boleh dinyatakan sepadan**.

---

## 2. Dua hal yang menunggu keputusan, bukan pengamatan

**Layar 6 — jumlah customer 334 vs 333.** `CUSTOMER.DBF` berisi 334 baris tetapi hanya 333 kode
unik: kode `00375` ("ARI TK") tercatat dua kali dengan alamat berbeda (C3 dan C2). Legacy
menghitung baris; model tenant berkunci unik. Bukan data hilang — aturan yang berbeda. Menggabung
atau memisahkan keduanya adalah keputusan pemilik usaha.

**Layar 20 — diskon per baris pembelian.** Medan `HARGAASLI` / `DISCOUNT` / `DISCOUNT2` pada
`BELI.DBF` belum punya rumah pada model tenant (keputusan terbuka sejak doc 120), sehingga diskon
per baris pembelian tidak terekam.

---

## 3. Empat layar kosong yang BUKAN cacat

Layar 30 (Sales Order), 39 (SPJ), 40 (Nota Sales), dan 43 (Jurnal) kosong karena **berkas legacy
tidak menyimpan dokumen jenis itu** — bukan karena layarnya gagal:

- 9.850 penjualan legacy diimpor sebagai **faktur** dan mutasi stok; berkas legacy memang tidak
  menyimpan tahap ORDER. Faktur-fakturnya terlihat di layar Piutang dan di tab "Rincian per Baris
  Faktur" pada Laba Rugi.
- SPJ dan penugasan nota lahir dari proses di sistem baru; legacy hanya punya tombol
  "Sales Bawa Nota" pada formulir pembayaran.
- Bagan akun terimpor (11 akun, setara DBF) tetapi baris jurnalnya tidak ada di berkas legacy.

Membedakan "kosong karena rusak" dari "kosong karena memang tidak ada sumbernya" adalah inti
dokumen sanding ini; keduanya terlihat identik di layar.

### Satu jebakan baca yang perlu dicatat

Layar Laba Rugi berjendela bawaan **awal bulan berjalan sampai hari ini** — dan itu default yang
BENAR untuk laporan laba rugi; mengubahnya menjadi rentang lebar justru akan menyesatkan. Yang
membuatnya terbaca kosong hanyalah data UAT yang berakhir Agustus 2026: pada 7 September, jendela
1–7 September memang tidak berisi apa pun.

Bedakan ini dari jebakan Laporan Opname (doc 122), yang memang cacat: di sana jendela 30 hari
dipakai untuk peristiwa yang jarang terjadi, sehingga layar tampak kosong pada data sungguhan
kapan pun. Di sini jendelanya benar; datanya yang historis.

**Cara membacanya saat menguji:** lebarkan rentangnya ke periode yang berisi data sebelum
menyimpulkan layar ini kosong.

---

## 4. Yang tersisa untuk UAT 100%

1. **Celah harga kredit/tunai (§1)** — satu-satunya penghalang teknis yang tersisa.
2. Dua keputusan pemilik usaha (§2).
3. Data uji untuk sesi sales (SPJ, Nota, Biaya, Rekonsiliasi) bila alur itu ingin ikut diuji.
4. Sesudah semuanya: varian `sales-inventory` → `https://ebisnis.id/ebisnis`.
