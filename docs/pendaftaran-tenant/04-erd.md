# 04 — ERD Control-Plane (implementasi nyata)

```mermaid
erDiagram
    PENDAFTAR ||--o| PENDAFTAR_TENANT_PROFILE : "pendaftar_id UNIQUE"
    PENDAFTAR ||--o{ PENDAFTARAN_TENANT : "pendaftar_id"
    PENDAFTARAN_TENANT ||--o{ PENDAFTARAN_TENANT_JENIS_USAHA : "pendaftaran_tenant_id"
    JENIS_USAHA_TENANT ||--o{ PENDAFTARAN_TENANT_JENIS_USAHA : "jenis_usaha_tenant_id"
    JENIS_USAHA_TENANT ||--o{ JENIS_USAHA_TENANT_MODULE : "jenis_usaha_tenant_id"
    PENDAFTARAN_TENANT ||--o| SCHEMA_NAME_RESERVATION : "pendaftaran_tenant_id"
    PENDAFTARAN_TENANT ||--o{ PENDAFTARAN_CONSENT : "pendaftaran_tenant_id"
    PENDAFTARAN_TENANT ||--o{ PENDAFTARAN_EMAIL_VERIFICATION : "pendaftaran_tenant_id"
    PENDAFTARAN_TENANT ||--o| PROVISIONING_JOB : "pendaftaran_tenant_id"
    PROVISIONING_JOB ||--o{ PROVISIONING_STEP : "job_id"
    PENDAFTARAN_TENANT ||--o| TENANT_REGISTRY : "tenant_registry_id"
    TENANT_REGISTRY ||--o{ TENANT_DOMAIN : "tenant_id"
    TENANT_REGISTRY ||--o{ TENANT_MODULE_ENTITLEMENT : "tenant_id"
    TENANT_REGISTRY ||--o{ TENANT_MEMBERSHIP : "tenant_id"
    PENDAFTAR ||--o{ TENANT_MEMBERSHIP : "pendaftar_id"
    PENDAFTARAN_TENANT ||--o{ REGISTRATION_CREDENTIAL_DELIVERY : "pendaftaran_tenant_id"
```

- Java: paket `ais.database.model.tenant` (16 entity, +PENDAFTARAN_AUDIT_EVENT berdiri sendiri
  dgn id polos, tidak digambar).
- Entity existing TIDAK diubah: `Brand/Toko/Investor/AkunManajemen` tetap scope `pendaftar`;
  scope `tenant` utk data operasional dilakukan additive di fase gating/onboarding (P5) TANPA
  memutus data lama (tenant default per Pendaftar dibentuk saat backfill disetujui — §16.4).
- Detail kolom: lihat 07-data-dictionary.md; workflow: 05-workflow.md.
