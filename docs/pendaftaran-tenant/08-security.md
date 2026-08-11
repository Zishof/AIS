# 08 — Keamanan Route Publik `/pendaftaran`

## Permukaan & whitelist
- Spring Security: lolos via catch-all `/** = IS_AUTHENTICATED_ANONYMOUSLY` (applicationContext-security.xml:36)
  — konsisten dgn endpoint publik existing (/EbisnisPublic, /PosApi); TIDAK ada perubahan file security.
- FilterJSP meneruskan `/pendaftaran` apa adanya; akses langsung `pendaftaran_tenant.jsp` dipantulkan ke `/`.
- Endpoint admin (retry/approve/reject/release) TIDAK ada di servlet publik — `retry_provisioning` publik dijawab `ADMIN_ONLY`.

## CSRF
- `PendaftaranCsrfUtil`: token SecureRandom 32-byte per session (key `pendaftaran_csrf`), wajib di SEMUA POST,
  perbandingan constant-time, `rotate()` tersedia utk pasca-login/verifikasi.

## Anti-automation (§13.3)
- `PublicRegistrationRateLimiter` in-JVM sliding window: submit 10/jam/IP + 5/jam/email + 5/jam/username;
  check_username 60, check_email 30, verify 20, resend 5/IP + 3/kode, status 60. Multi-node = limit per-node
  (diterima utk anti-abuse dasar; limiter terdistribusi = pekerjaan lanjutan).
- Honeypot `website_hp` (CSS off-screen) + elapsed-time (formInstanceId dibagikan GET; submit < N detik ditolak,
  default 5, config `pendaftaran_min_detik_isi_form`) — keduanya dijawab generik `REQUEST_REJECTED` tanpa
  membocorkan mekanisme.
- CAPTCHA configurable = TIDAK diimplementasikan fase ini (tercatat; honeypot+elapsed+rate limit dulu).

## Password & token
- PBKDF2WithHmacSHA256 120k/256bit/salt 32B (`PasswordHashService`), compare `MessageDigest.isEqual`;
  parameter dicatat versioned di `pendaftar_tenant_profile` (upgrade-on-login dimungkinkan).
- Minimal panjang configurable default 10 (jalur lama 6 TIDAK diubah — kompatibilitas).
- Token verifikasi email & reservation: SecureRandom 32B; DB hanya menyimpan SHA-256 hex; token baru
  men-supersede lama; expiry configurable. Password/OTP/token TIDAK pernah masuk audit/log/error_log.

## XSS / injection / privacy
- JSP: nilai server di-escape (`StringEscapeUtils.escapeHtml` utk kode status); data dinamis dirender via
  `textContent` (bukan innerHTML). Katalog berasal dari DB internal (admin-controlled) dan di-serialize JSON.
- Seluruh akses data via Hibernate Criteria/parameter binding; raw SQL hanya COUNT pedagang/pg_namespace
  dgn named parameter. Tidak ada schema DDL di fase ini (schema identifier baru menyusul di provisioning P4
  dgn whitelist regex + quoting — TIDAK dari request, invariant #3).
- Tidak ada open redirect: `redirect` selalu path relatif context sendiri.
- Status publik menyembunyikan email (mask), tanpa stack trace/schema/detail DB; jawaban check_email tidak
  membocorkan tenant apa yang dimiliki sebuah akun.
- IP: disimpan HASH di permohonan+audit; IP mentah hanya di `pendaftaran_consent` (bukti persetujuan §14.5).

## Sesi
- Wizard publik tidak membuat sesi login. Bukti kepemilikan cancel_draft = daftar registrationCode pada
  HttpSession browser pengirim. Session principal ringan + rotasi sesi login pendaftar = fase berikutnya (P3).

## Catatan risiko diterima (didokumentasikan)
1. Rate limiter in-JVM per node (bukan cluster-wide).
2. GET mode=verifikasi memutasi status (konsumsi token) — standar tautan email; token sekali-pakai + rate limit.
3. `Pendaftar.getDomain()` legacy mengembalikan null utk string kosong — jalur baru selalu mengisi slug valid.
