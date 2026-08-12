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

## Pembaruan status per fase

- Setelah P1 (commit feecdedc): G-03/G-05/G-07(model)/G-08(model)/G-11(model)/G-12(model)/G-13(model)/G-14(model)/G-15(model) → **MIGRATION_DONE** (entity+mapping+seed; tabel tercipta hbm2ddl saat deploy).
- Setelah P2: G-01 → **UI_DONE** (servlet+mapping+JSP wizard 8 langkah+status+verifikasi); G-02 → **BACKEND_DONE**
  (submit transaksi tunggal, aktif=false eksplisit); G-04 → **BACKEND_DONE** (reservation INSERT unique + cek
  benturan domain/registry/reservation/tbmuser/pedagang(COUNT>0)/pg_namespace/reserved-configurable);
  G-07 → **BACKEND_DONE** (challenge+verify+resend+MailSender best-effort); G-08 → **BACKEND_DONE**;
  G-09 → **BACKEND_DONE** (idempotency_key unique + replay balikan kode sama); G-10 → **SECURITY_DONE**
  (CSRF+rate limit+honeypot+elapsed; CAPTCHA configurable = catatan); G-16 → **BACKEND_DONE** (jalur baru);
  G-15 → **BACKEND_DONE** utk event submit/verify (event provisioning menyusul P4). Bridge aksi=daftar =
  deprecation redirect (§4.5 opsi transisi, alasan consent — lihat 06-api-contract.md).
- Setelah P3-P5 (commit 340f0db0): G-06 → **BACKEND_DONE+UI_DONE** (guard TENANT_NOT_READY semua aksi
  mutasi dashboard + panel tenant); G-11 → **BACKEND_DONE** (worker + step machine + retry backoff);
  G-12 → **BACKEND_DONE** (union entitlement ACTIVE/PLANNED); G-13 → **BACKEND_DONE** (trial mulai
  READY); G-14 → **BACKEND_DONE** (membership owner + tenant_list); G-18 → **BACKEND_DONE**
  (ubah/nonaktif IDOR-safe). Session principal + fixation + constant-time login → **SECURITY_DONE**.
- Setelah P6: G-17 → **UI_DONE** (backoffice mode=admin: list/filter/step + approve/reject/retry/
  release/verify-manual, reason wajib, gerbang root/role "am" per-request); unit test mandiri
  **TESTED** (35/35 LULUS, evidence/p6-verifikasi-mandiri.txt); harness konkurensi siap.
- Status akhir program (sebelum deploy): seluruh item implementasi **TESTED/SECURITY_DONE**;
  G-19 (backfill) + UAT runtime (Tomcat smoke, E2E, konkurensi ber-DB) = **UAT_REQUIRED**
  (11-uat.md; wewenang operator deploy).
- **2026-08-12 (lanjutan): UAT-1..7 DIEKSEKUSI dan LULUS** pada deployment lokal setara produksi
  (Tomcat 9 + JDK8 + PostgreSQL 9.3, DB segar, mode LEGACY) — bukti lengkap
  `evidence/uat-lokal-hasil.md`: E2E Apotek+IS & Bengkel (PLANNED jujur), gating pra-READY,
  konkurensi 1-menang-dari-50, idempotency replay kode sama, backoffice
  (verify-manual/reject/release-guard/retry-dari-step-aman/step-panel/gerbang admin), bridge
  REGISTRATION_MOVED, CSRF/honeypot/elapsed/rate-limit, Envers terisi, trial 30 hari dari READY.
  Status program → **UAT_PASSED** dgn 3 temuan minor backlog (step-FAILED rollback, trust-proxy
  XFF, artefak tester) + 2 cabang menunggu DB dev berdata legacy (fail-open akun lama,
  resend-limit) — lihat evidence.
- **2026-08-12 (penutupan): UAT-8a..8d LULUS** — cabang tersisa dieksekusi runtime: fail-open akun
  legacy sintetis (brand_tambah diizinkan tanpa tenant program), resend-limit 3/jam/kode +
  supersede token (3 SUPERSEDED+1 PENDING), verifikasi via TAUTAN token positif/negatif +
  single-use → READY, dan **G-19 backfill DITUTUP**: tool `PendaftarBackfillTool` (DRY-RUN/apply,
  idempoten) + report `migration-exceptions.csv` (klasifikasi LAINNYA_RAW/EMAIL_DUPLIKAT/
  EMAIL_KOSONG). G-19 → **TESTED** utk mekanisme; eksekusi di DB produksi tetap wewenang operator.
  Satu-satunya di luar jangkauan lokal: SMTP sungguhan. **UAT SELESAI SEMUA.**

## Catatan risiko lingkungan

- **PostgreSQL versi deployment bisa 9.3** (memori insiden `to_regclass`): `SELECT ... FOR UPDATE SKIP LOCKED` (9.5+) TIDAK boleh jadi satu-satunya mekanisme lock worker → pakai `locked_by/locked_at/retry_at` + `FOR UPDATE` biasa + lease timeout.
- **hbm2ddl=update** membuat tabel baru otomatis dari mapping (pola proyek: TANPA SQL DDL manual — lihat memori feedback). Migration "additive" = entity mapping baru + registrasi hibernate.cfg.xml; unique constraint dititip di anotasi `@Table(uniqueConstraints=...)`/`@Column(unique=true)` supaya ikut tercipta.
- **Sesi paralel (Codex) aktif di repo yang sama** → setiap commit pakai pathspec eksplisit, jangan `git add -A`.
- Build = **Maven** (`mvn -o compile` di `C:\opt\AIS\ais`) — TIDAK ada build.xml Ant di workspace (instruksi `ant compile` pada dokumen tidak berlaku; padanannya mvn).
