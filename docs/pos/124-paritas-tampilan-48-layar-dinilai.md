# 124 — Paritas tampilan 48 layar: hasil penilaian, dan satu celah yang terukur

**Tanggal:** 2026-09-07
**Sifat:** hasil penilaian paritas tampilan.
**Pendahulu:** [120](120-uat-paritas-data-tuntas.md), [122](122-layar-09-10-laporan-opname.md),
[123](123-cacat-jalur-tenant-terungkap-saat-menyandingkan.md).

---

## Verdikt

**Paritas DATA: tuntas** (doc 120, 123).
**Paritas TAMPILAN: 48 dari 48 layar sepadan**; sisa **2 menunggu keputusan pemilik usaha**.

> **DIPERBARUI 2026-09-07 sore.** Celah harga jual kredit/tunai (§1) **sudah ditutup**: pemilik
> usaha memilih menambahkan harga jual kedua ke model. Migrasi tenant **v20** (r86563) menambahkan
> `produk.harga_jual_tunai`, importir membawa `HARGAJUAL2`, dan layar menampilkan keempat kolom
> legacy. Verifikasi: **626/626 cocok dengan STOK.DBF, 0 berselisih**. §1 di bawah dipertahankan
> apa adanya sebagai catatan bagaimana celahnya ditemukan dan diukur.

Dokumen bukti: `UAT-Sanding-48-Layar-Lama-vs-Baru.docx/pdf` (50 halaman, landscape) — tiap layar
lama disandingkan dengan tangkapan NYATA aplikasi baru pada tenant `cmnmedika`.

| status | jumlah | arti |
|---|---:|---|
| SEPADAN | 19 | fungsi dan informasinya sama; datanya sudah diadu dengan DBF |
| SEPADAN — bentuk berbeda | 10 | fungsi sama, idiom UI berbeda dan disengaja |
| CETAK / EKSPOR | 9 | "layar" lama adalah pratinjau cetak / dialog ekspor |
| MENU | 2 | layar lama adalah menu; kini item sidebar |
| SEPADAN — menunggu data uji | 4 | layarnya lengkap; berkas legacy tidak menyimpan dokumen jenis itu |
| ~~BELUM SEPADAN~~ | ~~2~~ → **0** | layar 12 dan 13 — **ditutup v20**, lihat §1 |
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

### Yang dipilih, dan hasilnya

Pemilik usaha memilih **jalan pertama**. Migrasi tenant **v20** (r86563):

| bagian | perubahan |
|---|---|
| skema | `produk.harga_jual_tunai numeric(18,2)`, boleh NULL, tanpa pengisian surut |
| importir | `HARGAJUAL2` dibawa; NULL diteruskan sebagai NULL, tidak dijadikan nol |
| baca | `si_price_analysis` mengirim `hargaJualTunai` + `marginTunaiPersen` |
| layar | kolom Hrg Jual (Kredit), Hrg Jual (Tunai), RL % Kredit, RL % Tunai |

**NULL berarti "tidak ada harga tunai terpisah", bukan nol.** 459 dari 626 produk memang begitu;
menuliskan 0 membuat margin tunainya −100% dan setiap laporan harga salah. Layarnya menampilkan
tanda hubung, dan **tidak** menyalin harga kredit ke kolom tunai — itu pernyataan yang tidak
pernah dibuat siapa pun, dan pembacanya tidak punya cara membedakannya dari harga sungguhan.

**Verifikasi:** 626 produk diperbarui 0 gagal; **626/626 cocok dengan `STOK.DBF`, 0 berselisih**;
167 produk berharga tunai, 132 di antaranya berbeda dari kredit. Contoh `000301`: kredit
Rp 217.000 (margin 7,69%) vs tunai Rp 206.000 (margin 2,23%). Uji kesetaraan tetap
**LULUS=207 GAGAL=0**.

---

## 2. Dua hal yang menunggu keputusan, bukan pengamatan

**Layar 6 — jumlah customer 334 vs 333. ✅ DIPUTUSKAN: digabung (2026-09-07).**
`CUSTOMER.DBF` berisi 334 baris tetapi hanya 333 kode unik: kode `00375` ("ARI TK", BOBOS)
tercatat dua kali, **berbeda hanya pada kode wilayahnya** (`ALAMAT` = C3 vs C2); seluruh medan
lain — termasuk piutang awal/masuk/keluar — identik dan nol.

Penggabungannya kini **eksplisit**, dan itu perubahan yang sesungguhnya. Sebelum ini hasilnya
sudah "tergabung" tetapi karena **kebetulan**: penjaga `COALESCE`/`WHERE NOT EXISTS` di importir
membuat baris kedua menjadi no-op yang tidak dilaporkan siapa pun. Datanya sama; yang tidak ada
adalah siapa pun yang tahu bahwa 334 menjadi 333.

| lapisan | perubahan |
|---|---|
| ekstraktor | `gabung_kode_ganda()` menggabungkan baris berkode sama pada jenis MASTER, melaporkan tiap penggabungan |
| aturan | nilai kosong diisi dari baris berikutnya; bila keduanya terisi dan berbeda, baris pertama menang dan yang dibuang **disebut** |
| pembanding | `banding-dbf` mencetak "334 baris berkode → 333 kode unik. Kode ganda: 00375 x2" |

**Jenis TRANSAKSI sengaja tidak ikut digabung.** Dua baris berfaktur sama adalah dua ITEM yang
sah, bukan duplikat — menggabungkannya akan menghapus barang yang benar-benar dibeli. Itu persis
cacat kunci idempotensi yang pernah melenyapkan 562 baris BELI dan 51 baris JUAL (doc 116).

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

1. ~~Celah harga kredit/tunai~~ — **ditutup** oleh migrasi v20 (§1).
2. ~~Layar 6 (kode ganda)~~ — **diputuskan: digabung** (§2). Sisa satu: layar 20 (§2).
3. ~~Kode wilayah mitra~~ — **diputuskan: diimpor** (§5), migrasi v21.
4. Data uji untuk sesi sales (SPJ, Nota, Biaya, Rekonsiliasi) bila alur itu ingin ikut diuji.
5. Sesudah semuanya: varian `sales-inventory` → `https://ebisnis.id/ebisnis`.

---

## 5. Kode wilayah mitra — ditemukan, lalu ditutup (v21)

Ditemukan justru saat mengerjakan penggabungan `00375` — medan yang membuat kedua baris itu
berbeda ternyata **tidak pernah dibaca importir**, untuk pelanggan mana pun.

```
CUSTOMER.DBF, 334 baris:
  ALAMAT   terisi 334/334, hanya 7 nilai unik: C7(65) C5(57) C2(57) C1(53) C3(52) C4(34) C6(16)
  WILAYAH  terisi   0/334   <-- medan yang BERNAMA wilayah justru kosong seluruhnya
  ALAMAT1  terisi 334/334   ("BOBOS", "BODE", "PS SUMBER", ...)
```

Jadi pada berkas ini **`ALAMAT` adalah kode wilayah/rute**, dan `ALAMAT1` adalah alamat jalannya.
Itu cocok dengan layar legacy 06, yang berkolom "Alamat" (BODE, PS SUMBER) dan "Wilayah" (C1–C5),
serta bertombol **"Urut Wilayah"**.

`map_customer` membaca `ALAMAT1` → `alamat`, dan **`ALAMAT` tidak dibaca sama sekali** — bahkan
tidak masuk daftar "medan dilewati", sehingga tidak pernah dilaporkan. Medan `WILAYAH` yang
dilaporkan sebagai dilewati tidak pernah terisi, jadi laporan itu tidak pernah muncul: dua
kekeliruan yang saling menutupi.

**Akibatnya:** kolom Wilayah pada layar Master Customer kosong, pengurutan "Urut Wilayah" tidak
punya padanan, dan `SalesInventoryMasterTenant.dukungWilayahMitra()` mengembalikan `false` —
benar untuk model tenant hari ini, tetapi datanya **ada** dan sedang dibuang.

**✅ DIPUTUSKAN (2026-09-07): tambahkan kolom wilayah, lalu impor.** Terlaksana sebagai migrasi
**v21** (r86603) — dan lingkupnya melebar dari yang diminta, sebab supplier ternyata punya celah
yang sama dengan sumber yang berbeda:

| berkas | alamat jalan | wilayah |
|---|---|---|
| `SUPPLIER.DBF` | `ALAMAT` (97) | **`WILAYAH`** (72) — "CIREBON", "CRB" |
| `CUSTOMER.DBF` | `ALAMAT1` (334) | **`ALAMAT`** (334) — C1..C7; kolom bernama `WILAYAH` **kosong 0/334** |

Menyeragamkan keduanya "supaya rapi" akan menaruh kode wilayah pelanggan di medan alamat.

**Tidak dijadikan tabel referensi ber-FK, dan datanya membuktikan itu benar:** supplier memakai
`CRB` (35), `CIREBON` (22), `CREBON` (1), dan `CBR` untuk tempat yang tampaknya sama. FK menuntut
penyeragaman lebih dulu — keputusan bisnis tentang wilayah mana sama dengan mana, bukan keputusan
migrasi. Teks disimpan apa adanya; penyeragaman tetap dapat diambil kapan saja.

**Satu cacat saya sendiri, diperbaiki di tengah jalan:** impor pertama menuliskan `''` untuk 29
supplier yang memang tidak berwilayah. Bukan sekadar kerapian — penjaga `NULLIF(TRIM(...),'')`
pada UPDATE memperlakukan `''` sebagai kosong, jadi baris ber-`''` akan **ditimpa ulang setiap
impor**, termasuk wilayah yang sudah diketik pengguna. Kosong kini diteruskan sebagai NULL.

**Verifikasi:** supplier 72 berwilayah / 29 NULL / 0 string kosong (cocok DBF); customer 333
berwilayah, sebaran C1=53 C2=56 C3=52 C4=34 C5=57 C6=16 C7=65 — C2 turun 57→56 karena baris ganda
`00375` digabung, sisanya identik. API menampilkan wilayah, mencarinya (cari "C6" → 16, cocok
DBF), dan mengurutkannya. Keempat master **SETARA termasuk wilayah**.

### Audit seluruh medan `CUSTOMER.DBF` — supaya tidak ada temuan ketiga

Setelah dua kali menemukan medan terbuang secara kebetulan, seluruh 15 medan diperiksa sekaligus:

| medan | terisi | status |
|---|---:|---|
| KODECUST, NAMACUST, ATASNAMA, ALAMAT1 | 334 | terimpor |
| SYARAT_BYR | 164 | terimpor (termin) |
| NOTELPON | 1 | terimpor |
| **ALAMAT** (kode wilayah) | **334** | **TIDAK terimpor, tidak dilaporkan** |
| **PIUTAWAL** | **84** | **TIDAK terimpor — dan itu BENAR, lihat di bawah** |
| PIUTMASUK | 1 | tidak terimpor (turunan, sama alasannya) |
| ALMBANK | 11 | dilaporkan sebagai medan dilewati |
| ALAMAT2, REKRUPIAH, DISCOUNT, PIUTKELUAR, NAMABANK | 0 | kosong seluruhnya |

**`PIUTAWAL` sengaja tidak diimpor, dan datanya membuktikan itu benar.** Ia saldo piutang per
pelanggan — ringkasan dari `Tran_Piut.DBF`. Diadu per pelanggan:

```
cocok persis           68 dari 85
berselisih             17
  PIUTAWAL LEBIH KECIL dari rinciannya : 17
  PIUTAWAL lebih besar                 :  0
```

Arahnya searah tanpa kecuali: **`PIUTAWAL` adalah ringkasan tersimpan yang tertinggal dari
detailnya sendiri.** Mengimpornya berarti memasang sumber kebenaran kedua yang sudah terbukti
salah pada 17 pelanggan (total Rp 9.376.500 lebih rendah). Prinsip "derive, don't store" di sini
bukan preferensi gaya — ia menolak angka yang memang keliru.

**Kolom SALES yang kosong pada layar Master Customer bukan cacat:** `CUSTOMER.DBF` tidak punya
medan kode sales sama sekali. Keterkaitan pelanggan–sales pada legacy lahir dari fakturnya
(`Tran_Piut.KODESALES`), dan di layar Piutang aplikasi baru kolom SALES memang terisi.
