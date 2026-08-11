# 07 — Kamus Data Control-Plane (schema public, semua @Audited → new_audit.<tabel>__audit)

Semua entity: paket `ais.database.model.tenant`, extends GeneralValueObject, dynamicInsert/Update,
`id` IDENTITY, kolom `oleh/olehid/tanggal_dirubah` standar repo, @Column eksplisit SEMUA field.
`version` = javax.persistence.@Version (optimistic lock) pada entity workflow-mutable.

| Tabel | Entity | Kolom kunci/unik | Catatan |
|---|---|---|---|
| jenis_usaha_tenant | JenisUsahaTenant | code UNIQUE | katalog 14 seed; code immutable; requires_manual_review; display_order; aktif |
| jenis_usaha_tenant_module | JenisUsahaTenantModule | UNIQUE(jenis_usaha_tenant_id, module_code) | bundle modul per jenis; default_enabled/required |
| pendaftar_tenant_profile | PendaftarTenantProfile | pendaftar_id UNIQUE; normalized_email UNIQUE | extension 1:1 akun self-service; account_status; password_algorithm/version/iterations; must_change_password; email_verified_at |
| pendaftaran_tenant | PendaftaranTenant | registration_code UNIQUE; idempotency_key UNIQUE | status workflow; current_stage; desired/normalized_username; trial_days_snapshot; failure_code/message_safe; source_ip_hash |
| pendaftaran_tenant_jenis_usaha | PendaftaranTenantJenisUsaha | UNIQUE(pendaftaran_tenant_id, jenis_usaha_tenant_id) | primary_choice; other_description (wajib utk LAINNYA) |
| schema_name_reservation | SchemaNameReservation | normalized_name UNIQUE | titik serialisasi username; status RESERVED/CONSUMED/RELEASED/EXPIRED; reservation_token_hash |
| tenant_registry | TenantRegistry | code UNIQUE; slug UNIQUE; schema_name UNIQUE; audit_schema_name UNIQUE | status; tenant_mode LEGACY/HYBRID/TENANT_ONLY; trial_start/end_at; owner_pendaftar_id |
| tenant_domain | TenantDomain | normalized_domain UNIQUE | type SUBDOMAIN/CUSTOM; verification_token_hash; primary_domain |
| tenant_module_entitlement | TenantModuleEntitlement | UNIQUE(tenant_id, module_code, source) | source BUSINESS_TYPE/SUBSCRIPTION_PLAN/ADD_ON/ADMIN_OVERRIDE/TRIAL; status ACTIVE/DISABLED/PLANNED; limit_value; plan_version |
| pendaftaran_email_verification | PendaftaranEmailVerification | — (index via FK) | token_hash/otp_hash SHA-256; attempt_count; expires/consumed_at; status PENDING/CONSUMED/EXPIRED/SUPERSEDED |
| pendaftaran_consent | PendaftaranConsent | — | consent_type TERMS/PRIVACY/MARKETING; document_version; source_ip (mentah, bukti); user_agent; locale; channel |
| registration_credential_delivery | RegistrationCredentialDelivery | — | delivery_channel SCREEN_ONCE/EMAIL; delivered/acknowledged_at; TANPA plaintext |
| provisioning_job | ProvisioningJob | — | status QUEUED/RUNNING/SUCCESS/FAILED/CANCELLED; locked_by/locked_at lease; retry_at; attempt |
| provisioning_step | ProvisioningStep | UNIQUE(job_id, step_code) | 16 step kanonik; status PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/COMPENSATED; checksum |
| tenant_membership | TenantMembership | UNIQUE(tenant_id, pendaftar_id) | is_owner; role_code (OWNER); tbmuser_id nullable; valid_from/until |
| pendaftaran_audit_event | PendaftaranAuditEvent | — | event_code (19 kode §18); actor_type; pendaftar/registration/tenant_id POLOS (bukan FK relasi); detail_json non-sensitif |

Konstrain unik dititipkan pada anotasi (`@Column(unique)` / `@Table(uniqueConstraints)`) sehingga
tercipta oleh `hbm2ddl.auto=update` saat CREATE TABLE (pola migrasi additive repo ini — tanpa DDL
manual). Index tambahan non-unik (status+updated_at, retry_at) menyusul via anotasi @Index bila
terbukti perlu di UAT — bukan pemblokir fungsional.
