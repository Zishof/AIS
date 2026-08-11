# 05 — Workflow Status Pendaftaran Tenant

## Status permohonan (`pendaftaran_tenant.status`)

```
DRAFT -> SUBMITTED -> EMAIL_VERIFICATION_PENDING -> VERIFIED
      -> [REVIEW_PENDING -> VERIFIED | REJECTED]        (bila jenis usaha requires_manual_review / risk flag)
VERIFIED -> PROVISIONING_QUEUED -> PROVISIONING -> READY -> ACTIVE
PROVISIONING -> PROVISIONING_FAILED -> PROVISIONING_QUEUED (retry berwenang)
DRAFT|SUBMITTED -> CANCELLED ; EMAIL_VERIFICATION_PENDING -> EXPIRED ; ACTIVE <-> SUSPENDED
```

## Aturan transisi (ditegakkan PendaftaranTenantService, bukan setter bebas)

1. Submit valid = SATU transaksi: find/create Pendaftar (existing self-service TIDAK diduplikasi)
   → insert profile (bila baru) → insert PendaftaranTenant → pilihan jenis usaha (≥1) → consent
   (versi = versi terpublikasi) → INSERT SchemaNameReservation (serialisasi username) → tantangan
   verifikasi email → audit event → commit. Gagal apa pun = rollback semua (tidak ada reservation/
   permohonan yatim; Pendaftar existing tidak tersentuh).
2. `Pendaftar.aktif=false` (eksplisit) + `merupakanSekolah` sesuai jenis (true hanya bila pilihan
   primary SEKOLAH/PERGURUAN_TINGGI/PESANTREN) saat dibuat lewat jalur baru; `aktif=true` di-set
   saat tenant PERTAMA READY.
3. Email verification wajib sesuai konfigurasi `pendaftaran_wajib_verifikasi_email` (default AKTIF).
   Bila pengiriman email nonaktif (`aktfikan_pengiriman_email` off), permohonan tetap tercatat dan
   admin dapat memverifikasi manual dari backoffice.
4. Manual review HANYA bila: ada jenis usaha `requires_manual_review`, duplicate/risk flag, atau
   konfigurasi platform memaksa review.
5. Provisioning tidak pernah mulai sebelum reservation berstatus RESERVED milik permohonan itu.
6. READY hanya bila seluruh step non-SKIPPED berstatus SUCCESS; `trial_start_at=ready_at`,
   `trial_end_at=ready_at + trial_days_snapshot` (default 30, configurable).
7. ACTIVE = owner login pertama setelah READY.
8. Retry provisioning: step SUCCESS dilewati (idempoten per step); FAILED tidak menghapus Pendaftar.
9. Rejection menyimpan reason internal (audit) + failure_message_safe publik.
10. Reservation dilepas (RELEASED) hanya oleh admin dan HANYA bila belum CONSUMED (belum ada
    schema/tenant) — dengan audit event.
11. Idempotency: submit ulang dgn idempotency_key sama → kembalikan registrationCode yang sama,
    tanpa baris kedua, tanpa OTP baru tanpa batas (resend rate-limited terpisah).

## Status entitas lain

- SchemaNameReservation: RESERVED → CONSUMED (provisioning sukses) / RELEASED (admin) / EXPIRED (draft basi).
- ProvisioningJob: QUEUED → RUNNING → SUCCESS|FAILED(→QUEUED saat retry)|CANCELLED; lease locked_by/locked_at.
- ProvisioningStep: PENDING → RUNNING → SUCCESS|FAILED|SKIPPED(mode LEGACY utk step schema)|COMPENSATED.
- TenantRegistry: PROVISIONING → READY → ACTIVE ↔ SUSPENDED (suspend TIDAK menghapus data posted — invariant #15).
- PendaftarTenantProfile.account_status: PENDING_VERIFICATION → ACTIVE ↔ SUSPENDED.
