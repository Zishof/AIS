# Evidence UAT HYBRID (P7) — Migrasi Data-Plane Per-Tenant

Tanggal: 2026-08-12 · Lingkungan: Tomcat lokal yang sama (uat_tenant, PG 9.3), konfigurasi
`pendaftaran_tenant_mode` di-set **HYBRID**, redeploy class hasil compile P7 (mvn -o compile
EXIT=0), restart Tomcat ("Server startup in [3076523] ms", tanpa SEVERE — durasi didominasi
validasi ulang metadata hbm2ddl atas ±2.000 tabel existing pada PG 9.3; dicatat sbg karakteristik
lingkungan, bukan regresi kode P7).

## Skenario 1 — Registrasi HYBRID end-to-end ✅ LULUS
`REG-2026-000008` (Toko Hybrid UAT, retail, username `hybriduat`) → verifikasi manual admin →
worker memproses → **READY**. Bukti step (16, SEMUA jalur schema AKTIF):

```
VALIDATE_REGISTRATION  SUCCESS  validasi lulus; mode=HYBRID
RESERVE_USERNAME       SUCCESS  reservasi #9 RESERVED
CREATE_TENANT_REGISTRY SUCCESS  registry #6 slug hybriduat
CREATE_SCHEMA_ERP      SUCCESS  schema hybriduat dipastikan ada
CREATE_SCHEMA_AUDIT    SUCCESS  schema hybriduat dipastikan ada
RUN_MIGRATIONS         SUCCESS  ERP applied=1 skipped=0 checksum=a6db06cb130d
INSTALL_AUDIT          SUCCESS  AUDIT applied=1 skipped=0 checksum=d58fd5bae2c6
SEED_CONFIGURATION/MODULES/ROLES SUCCESS (6 entitlement)
CREATE_OWNER_USER      SKIPPED  (by design: login = akun Pendaftar)
CREATE_MEMBERSHIP      SUCCESS
CREATE_SUBSCRIPTION_TRIAL SUCCESS
VERIFY_SCHEMA          SUCCESS  schema+tabel+riwayat migrasi terverifikasi (v1-core-pos)
VERIFY_LOGIN           SUCCESS
MARK_READY             SUCCESS  READY; trial s.d. 2026-09-11
```

## Skenario 2 — Struktur nyata di database ✅ LULUS
- `pg_namespace`: **hybriduat** + **hybriduat__audit** ada.
- Tabel ERP `hybriduat`: `brand`, `toko`, `pedagang`, `tenant_schema_migration`.
- Tabel AUDIT `hybriduat__audit`: `revinfo`, `brand`, `toko`, `pedagang` (pola Envers rev/revtype).
- Riwayat migrasi (`hybriduat.tenant_schema_migration`):
  `v1-core-pos-erp|ERP|4b8bff529787…` + `v1-core-pos-audit|AUDIT|c8809f088a65…` (checksum SHA-256).
- Registry: `hybriduat | HYBRID | schema_name=hybriduat | audit=hybriduat__audit |
  schema_version=v1-core-pos | READY` — schemaName HANYA ditulis provisioning (invariant #3).

## Skenario 3 — Idempotensi migrasi (§10.2 "dapat diulang idempotent") ✅ LULUS
Step RUN_MIGRATIONS/INSTALL_AUDIT/VERIFY_SCHEMA di-reset PENDING + job di-requeue (simulasi
retry) → worker mengulang → **`ERP applied=0 skipped=1` + `AUDIT applied=0 skipped=1`**
(checksum riwayat cocok → dilewati, TANPA error, TANPA objek ganda), VERIFY_SCHEMA lulus lagi,
job SUCCESS. Proteksi korupsi: checksum riwayat ≠ kanonik → IllegalStateException (fail keras,
codepath `terapkanMigrasi`).

## Skenario 4 — Tenant LEGACY existing tidak terganggu ✅ LULUS
Pasca mode HYBRID + restart: login pendaftar Apotek (LEGACY) tetap sukses; `tenant_list` =
`apoteksehat_uat:ACTIVE:23modul`; data onboarding utuh (brand=1, toko=1, mesinPos=1).
Registry lama tetap `tenant_mode=LEGACY` (snapshot per-tenant — perubahan konfigurasi platform
tidak menulis ulang tenant lama).

## Kesimpulan
P7 HYBRID **LULUS UAT penuh**: mesin migrasi kanonik ber-riwayat+checksum menggantikan
`schema-only-v0`; step schema/migrasi/audit/verify aktif dan idempoten; LEGACY tidak terdampak.
Batas berikutnya (di luar P7, tercatat): data-plane CUTOVER — aplikasi MEMBACA/MENULIS tabel
tenant schema (mode TENANT_ONLY) — membutuhkan strategi routing sesi Hibernate per-tenant;
tabel v1 adalah baseline struktur utk migrasi tsb.
