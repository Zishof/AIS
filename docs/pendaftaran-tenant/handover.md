# Handover — Pendaftaran Tenant Baru (`Common.ROOT + "/pendaftaran"`)

## Branch & commit
- Branch: `feat/new-ui-rbac-role-user` (keputusan tetap di branch delivery — 00-baseline.md).
- Commit program ini: `2340d2cc` (P0 audit) → `feecdedc` (P1 entity+seed) → `f4fcfc67`
  (P2 route+wizard+keamanan) → `340f0db0` (P3-P5 principal+worker+gating) → <commit P6> (backoffice+test+docs).
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
1. Mode HYBRID: schema per-tenant dibuat+diverifikasi TAPI `schema-only-v0` — migrasi tabel
   data-plane per-tenant belum diaktifkan (step RUN_MIGRATIONS/INSTALL_AUDIT = SKIPPED bercatat).
   Mode default LEGACY tidak terdampak.
2. Rate limiter in-JVM per node (bukan cluster-wide).
3. CAPTCHA belum (honeypot+elapsed+rate limit sudah); paket = Konfigurasi JSON (belum entity
   plan/billing penuh); flow wizard `tenant-baru` memakai wizard yang sama (langkah akun tetap
   tampil — email verifikasi otomatis ke email akun login).
4. Backfill Pendaftar existing + `migration-exceptions.csv` menunggu akses DB deployment.
5. Backoffice = halaman ringkas gated (bukan tab ZK PendaftarAction) — semua aksi §15 tersedia;
   integrasi menu ZK bisa menyusul tanpa perubahan server.

## UAT_REQUIRED
Deploy+restart Tomcat (operator) → jalankan 11-uat.md (UAT-1..7) + acceptance SQL ERD §8 +
harness konkurensi → simpan evidence → status DoD → UAT_PASSED/DONE.

## Rollback
`git revert` commit fase (additive); tabel telanjur dibuat dibiarkan (tidak dibaca setelah revert).
