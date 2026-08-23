# 05 — Pemetaan tabel existing ke schema tenant

Dokumen ini **wajib dibaca sebelum satu kueri `si_*` pun dipindah** (P4). Tanpa itu, tiga
pemetaan yang tampak jelas akan menghasilkan data yang salah tanpa satu galat pun muncul.

Semua pemetaan diturunkan dari **medan entitasnya**, bukan dari kemiripan nama.

## Ringkasan

| | |
|---|---|
| Tabel `si_*` berklasifikasi TENANT_OWNED | 16 |
| Sudah punya padanan | 6 |
| Bundel v9 (sudah dibuat) | 10 |
| **Jebakan pemetaan** | **3** |

---

## Bagian 1 — Tiga jebakan

### ⚠ J-1: `koperasi.pembelian` adalah PENJUALAN, bukan pembelian

Ini yang paling berbahaya, dan namanya justru mengundang kesalahan.

| | `koperasi.pembelian` (existing) | `<slug>.pembelian` (v4) |
|---|---|---|
| Arti | **penjualan POS** — baris belanja anggota | **pembelian dari pemasok** |
| Sumber | transaksi kasir | `BELI.DBF` |
| Medan penciri | `produk`, `member`, `anggotaKoperasi`, `hargaSatuan`, `kios` | `supplier_id`, `nomor_faktur`, `hutang` |
| Arah uang | **masuk** | **keluar** |

**Pemetaan yang benar:**

```
koperasi.pembelian                  →  <slug>.faktur_penjualan_detail
koperasi.pembelian_anggota_koperasi →  <slug>.faktur_penjualan
```

`pembelian_anggota_koperasi` adalah **kepala** transaksi (`anggotaKoperasi`, `toko`,
`caraPembayaranKoperasi`, `lunas`, `sesiKasKasir`, `kasirLoginNama`, `idPerangkat`);
`pembelian` adalah **barisnya**.

> Memetakan `koperasi.pembelian` → `<slug>.pembelian` karena namanya sama akan mencatat
> penjualan sebagai pembelian. Laba rugi terbalik, stok bertambah saat seharusnya berkurang,
> dan tidak ada galat yang muncul. **Jangan pernah memetakan berdasarkan nama.**

### ⚠ J-2: `jenis_produk` → `kategori_produk` menghilangkan aturan bisnis

`koperasi.jenis_produk` memuat `maksimalHarian` — **batas pembelian harian per jenis
produk**, aturan nyata yang dipakai kantin sekolah. `<slug>.kategori_produk` (v3) tidak
punya kolom itu.

Memetakan begitu saja akan menghilangkan batas hariannya diam-diam: pengguna dapat membeli
melampaui plafon dan tidak ada yang menolak.

**Tindakan:** v9 menambahkan kolom `maksimal_harian numeric(18,2)` ke `kategori_produk`
lewat `ALTER TABLE`, bukan memaksakan pemetaan yang merugi.

### ⚠ J-3: `anggota_koperasi` → `customer` menghilangkan identitas

`koperasi.anggota_koperasi` bukan sekadar master pelanggan. Ia juga memuat:

- **identitas login** — `userid`, `pass`, `tanggalKadaluarsa`;
- **jenis dan tipe anggota** — `jenisAnggotaKoperasi`, `tipeAnggotaKoperasi`;
- **tautan sivitas** — `mahasiswa`, `dosen`, `guru`, `siswa`, `pegawai`, `satuanKerja`;
- **riwayat keanggotaan** — `tanggalBerhenti`, `alasanBerhenti`, `jumlahPeringatan`.

`<slug>.customer` (v2) hanya punya `kode`, `nama`, `salesperson_id`, `status`, `aktif`.

**Tindakan:** v9 menambahkan `customer_anggota_profile` yang menampung medan keanggotaan.
**Kolom `pass` TIDAK dipindahkan** — kredensial tidak ikut migrasi (§24 butir 13); anggota
yang memang punya login diselaraskan lewat `pengguna_tenant`.

---

## Bagian 2 — Pemetaan lengkap

### Sudah punya padanan

| Existing | Padanan | Catatan |
|---|---|---|
| `koperasi.produk` | `<slug>.produk` | nama dan arti sama |
| `koperasi.pembelian` | `<slug>.faktur_penjualan_detail` | **lihat J-1** |
| `koperasi.pembelian_anggota_koperasi` | `<slug>.faktur_penjualan` | kepala transaksi |
| `koperasi.anggota_koperasi` | `<slug>.customer` + `customer_anggota_profile` | **lihat J-3** |
| `koperasi.jenis_produk` | `<slug>.kategori_produk` + kolom baru | **lihat J-2** |
| `mutasi_idempoten` | `<slug>.idempotency_record` | yang lama tetap di `public` untuk jalur legacy |

### Bundel v9 — sepuluh tabel, **sudah dibuat**

| Existing | Tabel v9 | Mengapa tidak ada di v2–v8 |
|---|---|---|
| `koperasi.cara_pembayaran_koperasi` | `cara_pembayaran` | v2–v8 diturunkan dari arsip FoxPro, yang tidak mengenal metode bayar ber-akun GL |
| `koperasi.sesi_kas_kasir` | `sesi_kas_kasir` | tidak ada konsep sesi kasir di FoxPro |
| `koperasi.retur_penjualan` | `retur_penjualan` | idem |
| `koperasi.draft_pembelian` | `draft_penjualan_detail` | keranjang kasir |
| `koperasi.draft_pembelian_anggota_koperasi` | `draft_penjualan` | kepala keranjang |
| `koperasi.pemakaian_bahan_baku` | `pemakaian_bahan_baku` | resep/bahan baku kantin |
| `koperasi.survey_kepuasan_pos` | `survey_kepuasan` | |
| `koperasi.transaksi_backup_ack` | `transaksi_backup_ack` | **bukan entitas Hibernate** — hanya SQL mentah di `PosApi:3871,3929` |
| `koperasi.jenis_anggota_koperasi` | `jenis_customer` | |
| `public.foto_gambar_produk` | `foto_produk` | lihat catatan Blob di bawah |

### Tetap di schema bersama

| Tabel | Alasan |
|---|---|
| `public.tbmuser` | identitas login — harus diketahui **sebelum** tenant dipilih |
| `public.tbmrole` | peran global jalur POS lama; kewenangan **dalam tenant** memakai `<slug>.role_tenant` |
| `public.tenant_registry`, `tenant_membership` | control plane |
| `library.kode_transaksi` | referensi lintas modul |
| `public.mutasi_idempoten` | jalur legacy lintas-tenant |
| `new_audit.*` | 1.561 tabel Envers — pemblokir yang belum tuntas |
| `asset.master_asset`, `asset.detail_transaksi_asset` | **belum diputuskan** — milik modul Pengadaan |

---

## Bagian 3 — Catatan penerapan

### `foto_gambar_produk` memuat `Blob`

Entitasnya punya `Blob foto` **dan** `path`, `gdrive`, `url`. Sebelum v9 ditulis, perlu
dipastikan mana yang benar-benar dipakai produksi. Memindahkan Blob per tenant menggandakan
ukuran basis data; bila yang dipakai `path`/`url`, cukup metadatanya yang pindah dan berkasnya
tetap di penyimpanan berkas.

**Belum diverifikasi** — perlu melihat data produksi.

### `transaksi_backup_ack` tidak punya entitas

Hanya SQL mentah di `PosApi:3871`. Kolomnya harus dibaca dari kuerinya, bukan dari anotasi:

```
toko, kode_transaksi, id_perangkat, nama_mesin, kasir_user_id, kasir_nama, waktu
```

dengan kunci unik `(toko, lower(kode_transaksi), id_perangkat)`. Karena tidak ada entitas,
tidak ada risiko Hibernate menuliskannya ke schema yang salah — tetapi juga tidak ada yang
menjaga bentuknya.

### Temuan: PostgreSQL produksi setidaknya 9.5, bukan 9.3

Kueri `transaksi_backup_ack` itu memakai **`ON CONFLICT`** — sintaks **PostgreSQL 9.5+**.
Penelusuran lebih luas menemukan kode aktif memakai:

| Sintaks | Versi minimum | Berkas |
|---|---|---|
| `ON CONFLICT` | 9.5 | 8 (termasuk `PosApi`, `TenantDataPlaneService`) |
| `SKIP LOCKED` | 9.5 | 2 |
| `CREATE INDEX IF NOT EXISTS` | 9.5 | 6 |
| `jsonb` | 9.4 | 2 |

Pada PostgreSQL 9.3 semuanya adalah **galat sintaks** — kode ini tidak akan berjalan sama
sekali. Jadi produksi **pasti** 9.5 ke atas.

Artinya batasan §5 dokumen master ("PostgreSQL minimum yang masih didukung aplikasi,
termasuk 9.3 bila masih wajib") adalah kemungkinan yang sudah lama ditinggalkan kodenya
sendiri.

**Konsekuensinya untuk migrasi:**

- Bundel **v2–v8 tetap apa adanya**. DDL 9.3 berjalan sempurna di 9.5+, dan menyuntingnya
  akan mengubah checksum lalu menggagalkan setiap tenant yang sudah memasangnya.
- Bundel **v9 dan seterusnya boleh** memakai `CREATE INDEX IF NOT EXISTS`, `jsonb`, dan
  `ON CONFLICT` — bila memang menguntungkan.
- `TenantSchemaMigrasiSelfTest` saat ini **menolak** ketiganya. Penjaga itu perlu
  dilonggarkan bersamaan dengan v9, atau ia akan menghalangi pemakaian yang sah.

> Yang perlu dikonfirmasi tinggal versi persisnya, dan itu satu kueri di server produksi:
> `SELECT version();`. Selama belum, saya tetap menulis DDL bergaya 9.5 — bukan 9.3, bukan
> pula fitur 10+.

### `koperasi.toko` mengalir dua arah

`toko` sudah ada di **kedua** tempat: `koperasi.toko` (kanonik, control plane) dan
`<slug>.toko` (cermin v1, ditulis `TenantDataPlaneService`). Kueri tenant membaca cermin;
pendaftaran outlet tetap menulis yang kanonik. Jangan menjadikan cermin sebagai sumber
kebenaran.

### Urutan pemindahan yang disarankan

Dari yang paling sedikit ketergantungannya:

1. master baca-saja — `produk`, `jenis_produk`, `cara_pembayaran`
2. master tulis — `customer` beserta profilnya
3. transaksi baca — laporan penjualan
4. transaksi tulis — `faktur_penjualan` + detail, **di sinilah J-1 menentukan**
5. sesi kas, retur, draft
6. akuntansi

Setiap langkah dibandingkan hasilnya terhadap jalur lama pada basis data uji sebelum
dilanjutkan.
