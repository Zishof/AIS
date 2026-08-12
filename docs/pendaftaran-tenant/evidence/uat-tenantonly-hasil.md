# Evidence UAT TENANT_ONLY (P8) — Data-Plane Cutover

Tanggal: 2026-08-12 · Lingkungan lokal yang sama (Tomcat + PG 9.3, /uat_tenant). Redeploy class
P8, restart ("Server startup in [685851] ms", tanpa SEVERE).

## 1. Dual-write + audit nyata ✅ LULUS
Login `hana` (tenant `hybriduat`, ber-schema) → buat Brand#3 "Brand Hybrid Cutover" → Toko#2
"Gerai Hybrid Satu" (ber-brand) → Mesin POS#2. Bukti SQL:
- **Shared tetap ditulis** (POS-compat): `public.brand#3`, `koperasi.toko#2`, `koperasi.pedagang#2`.
- **Mirror ber-ID-SAMA di schema tenant**: `hybriduat.brand#3/toko#2/pedagang#2`.
- **Audit Envers-style di `hybriduat__audit`**: `revinfo`=3 baris; `brand id=3 rev=1 revtype=0`,
  `toko id=2 rev=2 revtype=0`, `pedagang id=2 rev=3 revtype=0` (audit pedagang TANPA kolom pass).
- Semua dalam transaksi yang sama dgn tulis shared (atomik).

## 2. Read dari schema tenant + penanda sumber ✅ LULUS
`brand_list`/`toko_list`/`mesin_pos_list` mengembalikan `sumberData="tenant-schema"`.

## 3. Sinkron backfill baris pra-cutover ✅ LULUS
Baris disuntik LANGSUNG ke `public.brand` (id 4 "Brand Pra-Cutover", simulasi data sebelum
cutover) → `brand_list` → baris muncul DAN tersalin ke `hybriduat.brand#4` + audit revtype=0.

## 4. Bukti DEFINITIF read-from-tenant-schema ✅ LULUS
Nama brand#4 diubah HANYA di `hybriduat.brand` → `brand_list` menampilkan
"NAMA-DARI-TENANT-SCHEMA" (bukan nama di tabel shared) → daftar benar-benar dibaca dari schema
tenant. Nama di-revert setelah uji.

## 5. Gerbang §3.3 mode TENANT_ONLY (switch konfigurasi → efek SEKETIKA tanpa restart) ✅ LULUS
`pendaftaran_tenant_mode` → TENANT_ONLY:
- `hana` (tenant ber-schema) → mutasi TETAP BOLEH (brand#5).
- `budi` (tenant program LEGACY tanpa schema) → DIBLOKIR: "Platform berjalan pada mode
  TENANT_ONLY: tenant Anda belum memiliki schema terprovision...".
- `warunglegacy` → DIBLOKIR — dan itu BENAR: investigasi menunjukkan akun ini TANPA SENGAJA
  menjadi pemilik tenant program `resenduji_uat` (REG-2026-000007 dibuat pada UAT-8b saat sesi
  masih login sbg akun tsb → **flow "tenant-baru utk pendaftar login" TERBUKTI end-to-end**:
  satu Pendaftar → tenant kedua, TANPA Pendaftar duplikat, verifikasi override ke email akun —
  invariant §3.1/ERD #14 teruji nyata secara tidak direncanakan).
- Akun legacy MURNI pra-program (`kiosmurni`, tanpa permohonan) → mutasi TETAP BOLEH (brand#6)
  — fail-open jalur lama dipertahankan.
Restore mode → HYBRID: `budi` langsung boleh lagi (brand#7) — mode-switch dua arah efektif
seketika (mode dibaca SQL langsung, bukan cache Konfigurasi; keputusan didokumentasikan).

## 6. Bug NYATA tertangkap & diperbaiki dalam UAT ini ✅
`listToko` semula `SELECT t.id, t.nama, b.nama, ...` — dua kolom berlabel `nama` membuat
Hibernate native query mengembalikan nilai yang sama utk keduanya (brandNama = nama toko).
Fix: alias unik (`t_nama`/`b_nama`); verifikasi pasca-redeploy di bawah.

## 7. Verifikasi fix brandNama pasca-redeploy ✅ LULUS
Redeploy + restart ("Server startup in [315248] ms") → `toko_list`:
`"Gerai Hybrid Satu | brand=Brand Hybrid Cutover | mesin=1"` (`sumberData=tenant-schema`) —
alias unik memperbaiki kolusi label; join brand + agregat mesin dari schema tenant benar.

## Kesimpulan
Cutover data-plane utk permukaan yang dimiliki program ini (Brand/Toko/Mesin POS dashboard)
TERBUKTI: dual-write atomik + audit per-tenant nyata + read dari schema tenant + backfill
inkremental + gerbang TENANT_ONLY dua arah. Batas jujur yang TERSISA: runtime POS/eBisnis
(PosApi dst.) masih membaca tabel shared — cutover penuh runtime = program lanjutan tersendiri
(multi-fase), bukan bagian paket pendaftaran ini.
