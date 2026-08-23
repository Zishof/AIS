# Multi-tenant Inventory & Sales

Pelaksanaan `PERINTAH_MASTER_CODEX_CLAUDE_IMPLEMENTASI_MULTI_TENANT_INVENTORY_SALES.md`.

| Dokumen | Fase | Isi |
|---|---|---|
| [00-baseline.md](00-baseline.md) | P0 §8.1 | Baseline kompilasi dan uji, topologi working copy |
| [TABLE_CLASSIFICATION.csv](TABLE_CLASSIFICATION.csv) | P0 §8.2 | 23 tabel jalur `si_*`, 4 skema, klasifikasi |
| [01-tenant-context.md](01-tenant-context.md) | P1 | Tujuh kelas konteks tenant per request |
| [02-api-tenant.md](02-api-tenant.md) | P2 | Aksi `tenant_*` dan medan tenant pada `ActorContext` |
| [03-migrasi-schema.md](03-migrasi-schema.md) | P3 + v9 | Delapan bundel migrasi, 74 tabel |
| [04-refactor-si.md](04-refactor-si.md) | P4 (sebagian) | Fondasi tenant untuk `si_*` |
| [05-pemetaan-tabel.md](05-pemetaan-tabel.md) | P4 prasyarat | Pemetaan `koperasi.*` → `<slug>.*`, **tiga jebakan** |
| [06-rbac-tenant.md](06-rbac-tenant.md) | P8 | Delapan peran tenant dan gerbangnya |

Sisi Flutter ada di repo `zishof-platform`:
`docs/pos-inventory-sales/MULTI_TENANT_FLOW.md`.

## Keadaan sekarang, ringkas

**Sudah berdiri.** Konteks tenant per request dengan kontrak kode galat baku §7.2; empat aksi
`tenant_*`; 74 tabel schema tenant dalam delapan bundel append-only ber-checksum; penulis audit
per-tenant; gerbang tenant pada dispatcher `si_*` yang dirutekan lewat `tbmuser.pendaftar` — `null`
berarti schema existing, tanpa perubahan apa pun bagi pengguna hari ini.

**Belum.** Sebelas helper `si_*` (7.512 baris) masih membaca schema bersama — inti P4 yang
tersisa. Satu Session per request (§9.4) menunggu bersamanya.

## Dua hal yang menahan kemajuan

**Belum ada basis data uji.** Seluruh verifikasi P3 dan P4 bersifat statis: kompilasi,
checksum, dan pemeriksaan katalog. Migrasi 63 tabel itu **belum pernah dijalankan pada
PostgreSQL mana pun**. Memindahkan 7.512 baris SQL ke tabel yang belum pernah ada, tanpa cara
menjalankannya, menghasilkan perubahan yang tidak dapat diverifikasi siapa pun.

**Versi PostgreSQL produksi setidaknya 9.5** — bukan 9.3 seperti diasumsikan §5. Kode aktif
memakai `ON CONFLICT` (8 berkas), `SKIP LOCKED` (2), `CREATE INDEX IF NOT EXISTS` (6), dan
`jsonb` (2); pada 9.3 semuanya galat sintaks. Bundel v2–v8 tetap bergaya 9.3 (aman, dan
checksum-nya tidak boleh berubah), tetapi v9 ke atas boleh memakai sintaks yang lebih baru —
dan penjaga `TenantSchemaMigrasiSelfTest` perlu dilonggarkan bersamaan. Versi persisnya masih
perlu dipastikan: `SELECT version();`

## Tiga hal yang paling mudah merusak data

1. **`koperasi.pembelian` adalah PENJUALAN**, bukan pembelian pemasok. Memetakannya ke
   `<slug>.pembelian` karena namanya sama akan membalik laba rugi tanpa satu galat pun.
2. **`jenis_produk` → `kategori_produk` menghilangkan `maksimalHarian`** — batas pembelian
   harian yang nyata dipakai.
3. **`anggota_koperasi` → `customer` menghilangkan identitas** dan tautan sivitas.

Rinciannya di [05-pemetaan-tabel.md](05-pemetaan-tabel.md). Baca sebelum memindahkan kueri.

## Aturan yang tidak boleh dilanggar

1. **DDL bundel yang sudah dirilis tidak boleh disunting.** Checksum-nya tercatat pada tiap
   tenant; yang berubah membuat migrasi gagal keras. Tambahkan versi baru di akhir katalog.
   `TenantSchemaMigrasiSelfTest` mematoknya.
2. **Nama schema tidak boleh sampai ke klien.** `TenantKonteksSelfTest` memeriksa teks JSON,
   bukan daftar medan.
3. **Tidak ada `SET search_path`.** Kualifikasi eksplisit lewat penanda `{t}`/`{a}`.
4. **Tidak ada fallback senyap ke schema bersama** bagi tenant program.
5. **Aksi `si_*` baru wajib dipetakan** ke area RBAC di `TenantRbac.area()`. Yang tidak
   dipetakan ditolak untuk **semua** peran, termasuk OWNER — `TenantRbacSelfTest` memindai
   sumber dan menangkapnya.

## Menjalankan penjaganya

```
java ais.service.tenant.test.TenantSchemaMigrasiSelfTest
java ais.service.tenant.test.TenantKonteksSelfTest
java ais.service.tenant.test.TenantRbacSelfTest
```

Keduanya berjalan tanpa basis data. `TenantKonteksSelfTest` berisik tanpa
`hibernate.cfg.xml` pada classpath — baca baris `GAGAL:` dan baris terakhir.
