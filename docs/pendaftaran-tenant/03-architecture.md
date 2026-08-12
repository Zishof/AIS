# 03 — Arsitektur Pendaftaran Tenant

## Peran konsep (§3.1 dokumen master)

```
Pendaftar                     = akun/pihak pendaftar (entity existing public.pendaftar) — 1..N tenant
PendaftarTenantProfile        = extension 1:1 akun self-service (email normalized unique, status akun, versi hash)
PendaftaranTenant             = SATU permohonan workspace (workflow DRAFT..ACTIVE)
JenisUsahaTenant (+Module)    = katalog jenis usaha + module bundle (database-driven, seed 14)
PendaftaranTenantJenisUsaha   = pilihan multi-select (SUMBER KEBENARAN; jenisBisnis = snapshot)
SchemaNameReservation         = reservasi atomik username/schema (unique normalized_name)
TenantRegistry                = tenant terprovision (slug immutable, schema hanya dari service)
TenantDomain                  = subdomain bawaan + custom domain (unique normalized_domain)
TenantModuleEntitlement       = union modul (BUSINESS_TYPE/PLAN/ADD_ON/OVERRIDE/TRIAL) ≠ permission
PendaftaranEmailVerification  = token/OTP hash + expiry + attempt
PendaftaranConsent            = versi terms/privacy + timestamp + IP + UA
RegistrationCredentialDelivery= bukti serah kredensial (tanpa plaintext)
ProvisioningJob/Step          = job-table + step idempoten (retry lanjut dari step aman)
TenantMembership              = keanggotaan Pendaftar↔tenant (owner; tbmuser nullable)
PendaftaranAuditEvent         = audit event bisnis (REGISTRATION_SUBMITTED..TENANT_READY)
```

## Lapisan kode

```
ais.action.servlet.PendaftaranTenantServlet     (P2) — controller tipis: baca request, CSRF envelope,
                                                       panggil service, tulis JSON/forward JSP
ais.action.servlet.api.PendaftaranTenantPublicHelper (P2) — jembatan kompatibilitas aksi=daftar lama
ais.service.registration.PendaftaranTenantService     — transaksi submit + transisi status
ais.service.registration.PendaftaranValidationService — normalisasi + validasi field
ais.service.registration.UsernameReservationService   — cek benturan + reservasi atomik
ais.service.registration.EmailVerificationService     — token hash, resend, verifikasi (P3)
ais.service.registration.JenisUsahaTenantSeedService  — seed katalog idempoten (P1, SUDAH ADA)
ais.service.tenant.TenantProvisioningService/Worker   — job/step + READY (P4)
ais.service.tenant.TenantEntitlementService           — union entitlement (P4)
ais.common.security.PublicRegistrationRateLimiter     — limiter IP+email+username in-JVM (P2)
```

## Mode kompatibilitas (§3.3)

Konfigurasi `pendaftaran_tenant_mode` (tabel Konfigurasi existing, `Common.getKonfigurasi`), nilai
`LEGACY` (default) / `HYBRID` / `TENANT_ONLY`:
- **LEGACY**: provisioning schema OFF — step CREATE_SCHEMA_*/RUN_MIGRATIONS/INSTALL_AUDIT = SKIPPED sah;
  tenant tetap dapat registry + entitlement + membership + trial; data operasional tetap scope
  per-Pendaftar di tabel existing (Brand/Toko/… dgn kolom `pendaftar`).
- **HYBRID**: tenant lama tetap; tenant baru diprovision ke schema `<slug>` + `<slug>__audit`
  DENGAN migrasi kanonik ber-riwayat+checksum (P7: `TenantSchemaMigrations` v1-core-pos —
  brand/toko/pedagang + audit; BUKAN hbm2ddl; idempoten; VERIFY_SCHEMA memeriksa schema+tabel+
  riwayat). Data-plane cutover (baca/tulis tabel tenant) = tahap TENANT_ONLY.
- **TENANT_ONLY**: hanya tenant ber-registry/schema valid yang menjalankan data-plane baru.

## Keputusan penamaan kolom

Tabel control-plane BARU memakai kolom snake_case gaya ERD (`pendaftar_id`, `tenant_id`, …, SEMUA
@Column/@JoinColumn eksplisit — landmine implicit-naming). Tabel existing tidak diubah; join ke
existing tetap lewat konvensi lama (`pendaftar`). TIDAK ADA kolom baru pada `Pendaftar` (hindari
jebakan kolom-audit Envers; lihat 01-source-audit §9).

## Keamanan (ringkas — detail 08-security.md)

- CSRF token session-bound (pola NewUiCsrfUtil) wajib di semua POST mutasi.
- Rate limit in-JVM per IP+email+username; honeypot + elapsed-time; audit SECURITY_BLOCKED.
- Password: PBKDF2WithHmacSHA256 versioned (algoritma+iterasi dicatat di profile), compare
  MessageDigest.isEqual, minimal panjang configurable (default 10) — jalur lama tidak diubah.
- Token verifikasi/reservasi: SecureRandom → simpan SHA-256 hex saja.
- IP disimpan sebagai hash di permohonan/audit; IP mentah hanya di consent (bukti hukum §14.5/§13.5).
