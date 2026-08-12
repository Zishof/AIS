# Handover — Pendaftaran Tenant Baru (`Common.ROOT + "/pendaftaran"`)

## Branch & commit
- Branch: `feat/new-ui-rbac-role-user` (keputusan tetap di branch delivery — 00-baseline.md).
- Commit program ini: `2340d2cc` (P0 audit) → `feecdedc` (P1 entity+seed) → `f4fcfc67`
  (P2 route+wizard+keamanan) → `340f0db0` (P3-P5 principal+worker+gating) → `5c0d05ab` (P6 backoffice+test+docs).
- Semua ter-push ke `origin` (github.com/Zishof/AIS.git); mirror SVN otomatis.

## File berubah/baru (inti)
- Entity: `src/ais/database/model/tenant/*.java` (16) + mapping `src/hibernate.cfg.xml:372+`.
- Service: `src/ais/service/registration/*` (Seed, Validation, UsernameReservation,
  EmailVerification, PendaftaranTenantService, PendaftaranTenantAdminService, 2 kelas verifikasi),
  `src/ais/service/tenant/*` (Provisioning Service/Worker, Schema, Entitlement, Onboarding).
- Keamanan: `src/ais/common/security/*` (PasswordHashService, PendaftaranCsrfUtil,
  PublicRegistrationRateLimiter, PendaftarSessionPrincipal).
- Servlet/UI: `PendaftaranTenantServlet` + `web.xml` (mapping `/pendaftaran`, load-on-startup 20) +
  `webapp/WEB-INF/baru/public/pendaftaran_tenant.jsp` (wizard 8 langkah) +
  `pendaftaran_tenant_admin.jsp` (backoffice) + `EbisnisPublicServlet` (bridge deprecation +
  fixation + principal + gating dispatch) + `PendaftarPublicHelper` (constant-time + lastLoginAt) +
  `PendaftarDashboardHelper` (+ubah/nonaktif) + `dashboard_ebisnis.jsp` (panel tenant) +
  `ebisnis.jsp` (CTA → wizard).

## Migration & konfigurasi
Lihat 09-migration.md (additive via hbm2ddl; TANPA kolom baru di pendaftar) dan 06-api-contract.md
(12 kunci Konfigurasi `pendaftaran_*`, semua berdefault aman; mode default LEGACY).

## Build & test
- Build: `mvn -o compile` di `C:\opt\AIS\ais` → EXIT=0 (setiap fase; TIDAK ada build.xml Ant).
- Unit: `java -cp build\maven\classes ais.service.registration.VerifikasiPendaftaranTenantMandiri`
  → LULUS=35 GAGAL=0 (evidence/p6-verifikasi-mandiri.txt).
- Konkurensi (saat UAT): `java -cp ... VerifikasiKonkurensiPendaftaran <baseUrl> 50`.

## URL
- Publik: `<ctx>/pendaftaran` (+`?mode=status&kode=`, `?mode=verifikasi&token=`, `?mode=tenant-baru`).
- Backoffice: `<ctx>/pendaftaran?mode=admin` (root / role Administrator "am").

## Akun UAT
Belum ada (dibuat lewat wizard saat UAT). Admin backoffice: pakai akun admin platform existing.

## Known limitation (jujur, tercatat di kode+docs)
1. ~~Mode HYBRID schema-only~~ **DITUTUP di P7 (2026-08-12)**: RUN_MIGRATIONS/INSTALL_AUDIT/
   VERIFY_SCHEMA kini AKTIF pada mode non-LEGACY — mesin migrasi kanonik `TenantSchemaMigrations`
   (+`TenantSchemaService.terapkanMigrasi`) dgn riwayat `<schema>.tenant_schema_migration` +
   checksum SHA-256, idempoten, baseline `v1-core-pos` (brand/toko/pedagang + audit revinfo/
   mirror). UAT HYBRID lulus (evidence/uat-hybrid-hasil.md). Batas BARU yang tersisa: data-plane
   CUTOVER (aplikasi membaca/menulis tabel tenant schema = mode TENANT_ONLY, butuh routing sesi
   Hibernate per-tenant) — belum dikerjakan, by design bertahap.
2. Rate limiter in-JVM per node (bukan cluster-wide).
3. CAPTCHA belum (honeypot+elapsed+rate limit sudah); paket = Konfigurasi JSON (belum entity
   plan/billing penuh); flow wizard `tenant-baru` memakai wizard yang sama (langkah akun tetap
   tampil — email verifikasi otomatis ke email akun login).
4. Backfill Pendaftar existing + `migration-exceptions.csv` menunggu akses DB deployment.
5. Backoffice = halaman ringkas gated (bukan tab ZK PendaftarAction) — semua aksi §15 tersedia;
   integrasi menu ZK bisa menyusul tanpa perubahan server.

## UAT — SUDAH DIJALANKAN (2026-08-12)
UAT-1..7 dieksekusi LULUS pada deployment LOKAL setara produksi (Tomcat 9.0.89 + JDK 1.8.0_202 +
PostgreSQL 9.3, context /uat_tenant, DB segar, mode LEGACY) — bukti `evidence/uat-lokal-hasil.md`
(acceptance SQL ERD §8 terpenuhi; harness konkurensi 1/50; idempotency replay; backoffice penuh).
Sisa utk server DEV berdata nyata (operator): verifikasi cabang fail-open akun legacy +
resend-limit + backfill §16.4 (`migration-exceptions.csv`) + uji email SMTP sungguhan.

## Rollback
`git revert` commit fase (additive); tabel telanjur dibuat dibiarkan (tidak dibaca setelah revert).
