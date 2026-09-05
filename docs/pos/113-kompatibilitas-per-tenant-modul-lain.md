# 113 — Kompatibilitas per-tenant modul lain (Apotik dkk.)

**Tanggal:** 2026-09-06
**Sifat:** pemeriksaan kompatibilitas saja. Tidak ada kode modul lain yang diubah.
**Pemicu:** permintaan "chek modul lain, misalnya modul apotik, juga kalau bisa per tenan
sebelum modul sales-inventory ini, hanya sekedar chek kompatibilias".

---

## Ringkasan

Dari 70 helper API di `ais/action/servlet/api`, **hanya `SalesInventory*` yang sadar-tenant.**
Sebutan tenant (`TenantSchema`, `ctx.tenant`, `TenantContext`, `jalurTenant`) berjumlah **251 pada
SalesInventory dan 0 pada semua modul lain** — Apotik termasuk.

Apotik **belum siap** per-tenant, dan penghalangnya bukan besarnya kode melainkan **mekanisme akses
basis datanya**. Perkiraan biaya port: kira-kira **setara dengan seluruh pemindahan Inventory &
Sales** (§16–§30), walau barisnya hanya 40%-nya.

---

## Mengapa Inventory & Sales bisa dipindah

Pola cabang-tenant yang dipakai sepanjang §16–§30 bekerja begini:

```java
// SalesInventoryStokHelper.java:234
if (SalesInventoryStokTenant.aktif(ctx)) {
    String sk = SalesInventoryStokTenant.skema(ctx.tenant);
    sql = SalesInventoryStokTenant.sqlKartu(sk, pid, dari, sampai);   // string tenant
} else {
    sql = ...;                                                        // string legacy
}
PreparedStatement ps = session.connection().prepareStatement(sql);
```

Kelas `*Tenant.java` **hanya merakit string SQL** — tidak satu pun mengeksekusi. Yang dieksekusi
tetap helper lamanya. Pola ini bisa dipakai **karena helper Inventory sudah lebih dulu berjalan di
atas JDBC mentah**: nama schema hanyalah teks di dalam string, jadi ia bisa diganti.

## Mengapa Apotik tidak

Apotik menulis lewat entitas Hibernate, dan entitasnya **memaku schema pada anotasi**:

```java
@Table(schema = "sirs", name = "item_medis")        // ItemMedis
@Table(schema = "sirs", name = "transaksi_medis")   // TransaksiMedis
@Table(schema = "sirs", name = "apotik_sesi_kas")   // ApotikSesiKas
@Table(schema = "sirs", name = "kadaluarsa")        // Kadaluarsa
@Table(schema = "sirs", name = "resep")             // Resep
```

`session.save(itemMedis)` **selalu** menulis ke `sirs`. Tidak ada parameter yang bisa mengubahnya:
patokan itu dibaca sekali saat SessionFactory dibangun, dan SessionFactory-nya dipakai bersama
seluruh penyewa. Inilah kendala yang sama yang memaksa seluruh jalur tulis tenant Inventory memakai
SQL native, dan yang memaksa audit ditulis tangan (`TenantAuditWriter`) karena `default_schema`
Envers pun statis.

### Angka penentunya

Rasio **eksekutor SQL-string** (`prepareStatement` / `createSQLQuery` / `executeQuery`) terhadap
**panggilan ORM** (`session.get/load/save/update/delete/merge/persist/createCriteria/createQuery`):

| modul | baris | SQL-string | ORM | rasio | kesiapan |
|---|---:|---:|---:|---:|---|
| KantinHelper | 19.487 | 251 | 252 | 1,00 | sebagian |
| **SalesInventory** | 18.607 | 380 | 164 | **2,32** | **sudah dipindah** |
| PengadaanPos | 8.323 | 3 | 72 | 0,04 | berat |
| **Apotik (15 berkas)** | **7.330** | **26** | **176** | **0,15** | **berat** |
| ElearningApi | 4.147 | 0 | 48 | 0,00 | berat |
| LinimasaApi | 3.821 | 12 | 12 | 1,00 | siap |
| SopService | 3.250 | 3 | 47 | 0,06 | berat |
| TagihanSiswa | 2.253 | 1 | 25 | 0,04 | berat |
| HotelApi | 1.913 | 0 | 54 | 0,00 | berat |
| SuratApi | 1.861 | 0 | 38 | 0,00 | berat |
| ProsesTransfer | 1.479 | 22 | 31 | 0,71 | sebagian |
| TopupHelper | 1.446 | 2 | 13 | 0,15 | berat |

Apotik dan SalesInventory berselisih **15 kali lipat** pada rasio ini. Membalik 176 panggilan ORM
menjadi SQL native adalah pekerjaannya — bukan menambah cabang.

**Batas ukuran ini:** rasio tersebut mengukur *mekanisme*, dan mekanisme itulah yang menghambat
atau memuluskan pemindahan Inventory. Ia **bukan** bukti biaya: modul ber-ORM sedikit tetapi
berlogika terjalin bisa saja lebih mahal. Nilai 99,00 pada modul tanpa ORM sama sekali adalah
penanda, bukan rasio sungguhan.

---

## Schema yang dipatok Apotik

| schema | sebutan | catatan |
|---|---:|---|
| `sirs` | 78 | rumah utamanya — rumah sakit, bukan `koperasi` |
| `akunting` | 8 | posting jurnal (`ApotikPostingHelper`, `ApotikPbfPostingHelper`) |
| `koperasi` | 5 | titik singgung dengan Inventory |
| `public` | 1 | penyemaian demo |

Berbeda dengan Inventory yang berumah di `koperasi`, Apotik berumah di **`sirs`**. Katalog migrasi
tenant v1–v19 dibangun dari kebutuhan `koperasi`; tak satu pun tabel `sirs` ada di dalamnya.

> Sebagian sebutan `sirs.` itu ada di **Javadoc**, bukan kode. Hitungan di atas belum memisahkan
> keduanya; yang sudah dipastikan lewat kode adalah 20 tabel di bawah.

---

## Tabel Apotik vs katalog tenant v1–v19

20 tabel yang disentuh Apotik lewat SQL native:

```
apotik_batch_konsumsi   apotik_item_profile      apotik_narkotika_log
apotik_pbf_dokumen      apotik_pbf_pembayaran    apotik_pembayaran_transaksi
apotik_posting_link     cara_pembayaran_koperasi detail_transaksi_pasien
grup_transaksi          item_medis               kadaluarsa
kode_transaksi_medis    pegawai                  pembelian_anggota_koperasi
posting_history         resep                    transaksi_medis
transaksi_medis_detail  util
```

Katalog tenant berisi 78 tabel. **Irisannya nol.**

### Konsepnya beririsan, bentuknya tidak

Beberapa konsep Apotik memang sudah punya sepupu di katalog tenant — tetapi sepupu, bukan kembar:

| konsep Apotik (`sirs`) | sepupu tenant | status |
|---|---|---|
| `item_medis` | `produk` | bentuk beda (lihat bawah) |
| `kadaluarsa` + `apotik_batch_konsumsi` | `produk_batch` | belum dibandingkan |
| `transaksi_medis` (+`_detail`) | `faktur_penjualan` (+`_detail`) | belum dibandingkan |
| `apotik_sesi_kas` | `sesi_kas_kasir` | belum dibandingkan |
| `cara_pembayaran_koperasi` | `cara_pembayaran` | belum dibandingkan |
| `apotik_posting_link`, `posting_history` | `posting_log` | belum dibandingkan |

Perbandingan yang **sudah** dilakukan, `item_medis` vs `produk`:

| `sirs.item_medis` | `{S}.produk` |
|---|---|
| `kode` | `kode` |
| `batas_minimal_stok` | `stok_minimum` |
| `default_harga_beli` | `harga_beli_terakhir` |
| `default_harga_jual` | `harga_jual_standar` |
| `default_permintaan` | **tidak ada rumahnya** |
| — | `satuan_id` (wajib untuk mutasi stok) |
| — | `barcode`, `pakai_batch`, `pakai_expiry`, `kategori_produk_id` |

Ini menegaskan kembali pelajaran yang sudah tercatat: **schema tenant bukan cermin legacy.** Kueri
Apotik tidak bisa dipindah dengan mengganti prefiks `sirs.` menjadi `{S}.`; tabel dan kolomnya
berbeda. Setiap konsep butuh keputusan pemetaannya sendiri, persis seperti 19 bundel v1–v19 lahir
satu per satu.

> Daftar kolom `item_medis` diambil dari anotasi `@Column(name=...)` yang eksplisit saja. Medan yang
> mengandalkan penamaan bawaan Hibernate belum terhitung, jadi daftar itu **batas bawah**.

---

## Apa yang dibutuhkan bila Apotik hendak dipindah

Bukan rekomendasi untuk mengerjakannya — hanya biayanya, agar keputusannya bisa diambil.

1. **Bundel katalog baru (v20+).** ~20 tabel, tidak ada yang bisa dipakai ulang apa adanya. Aturan
   append-only tetap berlaku: DDL yang sudah dirilis tidak boleh disunting.
2. **Membalik 176 panggilan ORM menjadi SQL native**, karena `@Table(schema="sirs")` tidak bisa
   dinegosiasikan saat runtime. Ini bagian terbesarnya.
3. **Kelas `Apotik*Tenant.java`** perakit SQL, sepadan dengan 11 kelas tenant Inventory.
4. **Audit tulis-tangan** untuk entitas Apotik, karena `default_schema` Envers statis.
5. **Penjaga dispatcher** setara `audit-penjaga-tenant.py` — saat ini belum ada padanannya untuk
   aksi Apotik, jadi tidak ada yang mencegah aksi baru lolos ke schema bersama.
6. **Uji kesetaraan** per konsep, berikut blok penjaga yang membuktikan contohnya membedakan benar
   dari salah.
7. **Keputusan posting jurnal.** Apotik menulis ke `akunting` (8 titik). Inventory menyelesaikan ini
   dengan `jurnal`/`jurnal_detail` se-tenant; Apotik butuh keputusan yang sama, dan itu menyentuh
   `ApotikPostingHelper` serta `ApotikPbfPostingHelper`.

## Modul yang lebih murah dari Apotik

Bila tujuannya menambah **satu** modul per-tenant lagi setelah Inventory, urutan menurut biaya
mekanisme — bukan menurut nilai bisnis:

- **`LinimasaApi`** (3.821 baris, rasio 1,00) — paling murah dari yang besar.
- **`KantinHelper`** (19.487 baris, rasio 1,00) — separuh jalannya sudah JDBC, tetapi besar; dan
  `sesi_kas_kasir`, `draft_penjualan`, `cara_pembayaran` **sudah ada** di katalog tenant.
- **`ProsesTransfer`** (1.479 baris, rasio 0,71).

Apotik ada di urutan bawah bersama HotelApi, ElearningApi, dan SuratApi.

---

## Kesimpulan

Apotik **tidak** bisa dijadikan per-tenant tanpa pekerjaan sebesar pemindahan Inventory & Sales
sendiri. Tidak ada penghalang arsitektural yang permanen — katalog migrasi, `TenantSchemaService`,
`TenantAuditWriter`, dan pola cabang-tenant semuanya generik dan siap dipakai ulang. Yang mahal
adalah membalik jalur ORM Apotik menjadi SQL native, dan merancang ~20 tabel yang belum punya
padanan.

**Tidak ada perubahan kode modul lain dalam pemeriksaan ini.** Rencana `sales-inventory` dan
pendaftar `cmnmedika` tidak terpengaruh: keduanya hanya menyentuh jalur `SalesInventory*` yang sudah
sadar-tenant dan sudah berpenjaga.
