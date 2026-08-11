# 02 — Gap Ledger Pendaftaran Tenant (status per item DoD)

> Status: NOT_STARTED / AUDITED / DESIGNED / MIGRATION_DONE / BACKEND_DONE / UI_DONE / SECURITY_DONE / TESTED / UAT_PASSED / DONE / BLOCKED

| # | Item | Existing (fakta audit) | Gap | Status |
|---|------|------------------------|-----|--------|
| G-01 | Route publik `Common.ROOT + "/pendaftaran"` | Belum ada mapping `/pendaftaran` di web.xml | Servlet + mapping + whitelist filter | AUDITED |
| G-02 | Insert root `public.pendaftar` saat submit | `PendaftarPublicHelper.daftar` sudah insert, TAPI `aktif=true` langsung, tanpa workflow | Jalur baru: `aktif=false` sampai READY; kompatibilitas jalur lama dijaga | AUDITED |
| G-03 | Multi-jenis-usaha dari katalog DB | `Pendaftar.jenisBisnis` String tunggal, isian teks bebas dari form lama | Entity `JenisUsahaTenant` + join `PendaftaranTenantJenisUsaha` + seed 14 jenis; `jenisBisnis` jadi snapshot kompatibilitas | AUDITED |
| G-04 | Username/slug global-unique + immutable + reservasi atomik | `buatSlugUnik` query-then-insert (race dilindungi hanya oleh unique `pendaftar.domain`) | `schema_name_reservation` UNIQUE(normalized_name) + cek benturan domain/Tbmuser.userId/Pedagang.userid/pg_namespace/reserved list | AUDITED |
| G-05 | Pendaftar 1—N tenant | 1 Pendaftar = 1 domain/slug implisit | `PendaftaranTenant` + `TenantRegistry` + `TenantMembership` | AUDITED |
| G-06 | Gating pra-READY (Brand/Toko/POS/Investor/Manajemen/master/transaksi) | `PendaftarDashboardHelper` TANPA gerbang status apa pun | Guard READY + entitlement di seluruh aksi mutasi dashboard; mode LEGACY: pendaftar lama tetap jalan | AUDITED |
| G-07 | Verifikasi email | Tidak ada (daftar langsung aktif) | `PendaftaranEmailVerification` token hash+expiry+attempt+resend limit; MailClient reuse | AUDITED |
| G-08 | Consent (terms/privacy versioned) | Tidak ada | `PendaftaranConsent` + versi dokumen + IP/UA/locale | AUDITED |
| G-09 | Idempotency submit | Tidak ada | `idempotency_key` unique di `pendaftaran_tenant`; retry balikkan registrationCode sama | AUDITED |
| G-10 | CSRF + rate limit + honeypot + elapsed-time | Tidak ada di jalur publik ebisnis | Token server-generated session-bound; limiter IP+email+username in-JVM (MemoryCacheUtil pola) | AUDITED |
| G-11 | Provisioning job/step + retry | Tidak ada | `provisioning_job`+`provisioning_step`, worker + locking; mode LEGACY: langkah schema di-SKIP sah | AUDITED |
| G-12 | Module entitlement per jenis usaha | Tidak ada | `jenis_usaha_tenant_module` + `tenant_module_entitlement` (source: BUSINESS_TYPE/TRIAL/...) | AUDITED |
| G-13 | Trial 30 hari mulai READY | Tidak ada | `trial_start_at/trial_end_at` di registry + snapshot trialDays di permohonan | AUDITED |
| G-14 | Owner membership | Relasi longgar `Pendaftar.admin` (Tbmuser, nullable) | `tenant_membership` (pendaftar, is_owner, role_code, tbmuser nullable) | AUDITED |
| G-15 | Workflow status + audit event | Tidak ada | Status DRAFT..ACTIVE + exception; audit event via ErrorAuditUtil-pattern/tabel audit tersendiri + Envers | AUDITED |
| G-16 | Password policy jalur baru | PBKDF2WithHmacSHA256 120k OK; compare `equalsIgnoreCase` (bukan constant-time); minimal 6 | Compare `MessageDigest.isEqual`; minimal configurable (default 10); versioned params di profile | AUDITED |
| G-17 | Admin backoffice permohonan | `PendaftarAction` CRUD Pendaftar saja | Tab permohonan/jenis usaha/reservation/provisioning/entitlement/audit + aksi approve/reject/retry | AUDITED |
| G-18 | Update/nonaktif entitas dashboard | Helper hanya list+tambah | Tambah update/nonaktif + pagination saat onboarding (P5) | AUDITED |
| G-19 | Backfill Pendaftar existing | — | HANYA akun self-service terbukti (passwordHash NOT NULL) diberi profile; jenisBisnis dikenal→map, tak dikenal→LAINNYA+raw; exception report CSV | AUDITED |
| G-20 | Mode kompatibilitas LEGACY/HYBRID/TENANT_ONLY | Tidak ada | Konfigurasi default **LEGACY** (provisioning schema OFF — perilaku deployment existing tidak berubah diam-diam) | AUDITED |

## Catatan risiko lingkungan

- **PostgreSQL versi deployment bisa 9.3** (memori insiden `to_regclass`): `SELECT ... FOR UPDATE SKIP LOCKED` (9.5+) TIDAK boleh jadi satu-satunya mekanisme lock worker → pakai `locked_by/locked_at/retry_at` + `FOR UPDATE` biasa + lease timeout.
- **hbm2ddl=update** membuat tabel baru otomatis dari mapping (pola proyek: TANPA SQL DDL manual — lihat memori feedback). Migration "additive" = entity mapping baru + registrasi hibernate.cfg.xml; unique constraint dititip di anotasi `@Table(uniqueConstraints=...)`/`@Column(unique=true)` supaya ikut tercipta.
- **Sesi paralel (Codex) aktif di repo yang sama** → setiap commit pakai pathspec eksplisit, jangan `git add -A`.
- Build = **Maven** (`mvn -o compile` di `C:\opt\AIS\ais`) — TIDAK ada build.xml Ant di workspace (instruksi `ant compile` pada dokumen tidak berlaku; padanannya mvn).
