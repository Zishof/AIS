# 06 — Kontrak API `/pendaftaran`

Semua respons JSON: `status` "00"/"91" + `code` STABIL + `description` aman-publik
(+ `fieldErrors`, `retryable`, `redirect`, `registrationCode`, `nextStep` sesuai konteks).
Semua POST wajib token CSRF (`csrf` param / header `X-Pendaftaran-CSRF`) yang dibagikan GET form.

## GET

| URL | Perilaku |
|---|---|
| `GET /pendaftaran` | Wizard 8 langkah; attrs: katalogJson (jenis usaha+modul+paket+versi terms dari DB), csrfToken, formInstanceId (elapsed-time), idempotencyKey. TANPA record permanen/reservasi. |
| `GET /pendaftaran?mode=status&kode=REG-...` | Halaman status (data via POST get_status). |
| `GET /pendaftaran?mode=verifikasi&token=...` | Konsumsi tautan verifikasi email (idempoten; token sekali pakai; rate-limited per IP). |
| `GET /pendaftaran?mode=tenant-baru` | Wizard utk pendaftar login (flow penuh menyusul fase session-principal). |

## POST (action=...)

| action | Rate limit | Sukses code | Error code utama |
|---|---|---|---|
| `check_username` (username) | 60/jam/IP | `USERNAME_AVAILABLE` (`tersedia`:bool, `preview`) | `USERNAME_INVALID`, `USERNAME_RESERVED`, `USERNAME_TAKEN_*` |
| `check_email` (email) | 30/jam/IP | `EMAIL_AVAILABLE` / `EMAIL_ALREADY_REGISTERED` (`terdaftar`:bool) | `EMAIL_INVALID` |
| `submit_registration` (field wizard + idempotencyKey + formInstanceId + honeypot website_hp) | 10/jam/IP + 5/jam/email + 5/jam/username | `REGISTRATION_CREATED` / `REGISTRATION_ALREADY_CREATED` (idempotent replay) | `VALIDATION_FAILED`(+fieldErrors), `USERNAME_NOT_AVAILABLE`, `EMAIL_ALREADY_REGISTERED`, `BUSINESS_TYPE_INVALID`, `BUSINESS_TYPE_OTHER_REQUIRED`, `REQUEST_REJECTED`(bot), `RATE_LIMITED`, `CSRF_INVALID`, `INTERNAL_ERROR` |
| `verify_email` (token) | 20/jam/IP | `EMAIL_VERIFIED` | `VERIFICATION_TOKEN_INVALID` |
| `resend_verification` (kode) | 5/jam/IP + 3/jam/kode | `VERIFICATION_RESENT` | `REGISTRATION_NOT_FOUND`, `VERIFICATION_NOT_PENDING`, `RATE_LIMITED` |
| `get_status` (kode) | 60/jam/IP | data status ter-mask (emailMasked, tanpa detail internal) | `REGISTRATION_NOT_FOUND` |
| `cancel_draft` (kode) | kepemilikan sesi | `REGISTRATION_CANCELLED` | `NOT_OWNER`, `CANCEL_NOT_ALLOWED` |
| `retry_provisioning` | — | — | `ADMIN_ONLY` (retry = wewenang backoffice P6) |

Contoh sukses submit:
```json
{"status":"00","code":"REGISTRATION_CREATED","description":"Pendaftaran berhasil disimpan. Periksa email Anda untuk verifikasi.",
 "registrationCode":"REG-2026-000001","nextStep":"VERIFY_EMAIL","redirect":"/ecampus/pendaftaran?mode=status&kode=REG-2026-000001"}
```

## Bridge kompatibilitas (deprecation)

`POST /EbisnisPublic aksi=daftar` TIDAK lagi menyimpan (opsi transisi §4.5): AJAX menerima
`{status:"00", code:"REGISTRATION_MOVED", redirect:"<ctx>/pendaftaran"}` (JS lama otomatis pindah);
non-AJAX = 302 ke wizard. Alasan memilih transisi (bukan delegasi): form modal lama tidak memuat
consent versioned/username/multi-jenis — delegasi diam-diam berarti memalsukan consent (§14.5).
`aksi=login` lama tetap berfungsi tanpa perubahan. CTA "Daftar Sekarang" di ebisnis.jsp kini
tautan `Common.ROOT + "/pendaftaran"`.

## Konfigurasi (tabel Konfigurasi, semua opsional — default aman)

`pendaftaran_tenant_mode` (LEGACY), `pendaftaran_trial_hari` (30), `pendaftaran_password_min` (10),
`pendaftaran_terms_version`/`pendaftaran_privacy_version` (2026-01), `pendaftaran_reserved_usernames`
(CSV tambahan), `pendaftaran_paket_json` (katalog paket), `pendaftaran_subdomain_base` (ebisnis.id),
`pendaftaran_verifikasi_jam` (48), `pendaftaran_reservasi_jam` (72), `pendaftaran_min_detik_isi_form` (5),
`pendaftaran_base_url` (tautan email; fallback dari request), `pendaftaran_wajib_review_manual` (tidak aktif).
