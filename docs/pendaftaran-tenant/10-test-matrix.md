# 10 — Matriks Uji

## Unit (§21.1) — SUDAH DIJALANKAN ✅

`ais.service.registration.VerifikasiPendaftaranTenantMandiri` (tanpa DB):
`java -cp C:\opt\AIS\ais\build\maven\classes ais.service.registration.VerifikasiPendaftaranTenantMandiri`
→ **LULUS=35 GAGAL=0 EXIT=0** (evidence/p6-verifikasi-mandiri.txt). Cakupan: normalisasi email,
username (NFKC/regex/panjang/dash), telepon, dedup jenis usaha, rapikan, PBKDF2 hash/verify/
constant-time-null-safety/salt-unik, SHA-256 vektor dikenal, token acak, parameter versi hash.
Yang butuh DB (reserved-configurable, terms version, state transition penuh, trial date,
entitlement union) → diuji lewat jalur integrasi di bawah.

## Konkurensi (§21.3) — HARNESS SIAP, EKSEKUSI SAAT UAT ⏳

`ais.service.registration.VerifikasiKonkurensiPendaftaran <baseUrl> <N>`:
- N submit username sama (idempotency beda) → LULUS bila ≤1 `REGISTRATION_CREATED`
  (serialisasi = unique constraint `schema_name_reservation.normalized_name`).
- 10 submit idempotency key sama → LULUS bila seluruh registrationCode identik.
- Worker ganda: klaim job dilindungi `@Version` (StaleObjectStateException → kalah race);
  uji dgn 2 node menunjuk DB sama saat UAT.
- Resend OTP bersamaan: token baru men-supersede lama; rate limit 3/jam/kode.

## Security (§21.4) — implementasi + titik uji UAT

| Uji | Mekanisme | Cara uji |
|---|---|---|
| GET publik | catch-all security + FilterJSP pass | curl GET /pendaftaran → 200 wizard |
| Admin tidak publik | `admin_*` + mode=admin gerbang root/role "am" per-request | curl tanpa login → ADMIN_ONLY / redirect |
| CSRF invalid | token session-bound constant-time | POST tanpa/salah csrf → CSRF_INVALID |
| Rate limit | limiter IP/email/username | loop submit ke-11 dalam sejam → RATE_LIMITED |
| XSS payload | escape + textContent; nama `<script>` tersimpan sbg data | render status: tidak dieksekusi |
| SQL payload | Criteria/parameter binding penuh | `' OR 1=1--` di username → USERNAME_INVALID (regex) |
| Schema injection | identifier dari registry + regex + reserved + quoting | request tidak pernah membawa schema name |
| Open redirect | redirect selalu path context sendiri | inspeksi respons |
| Session fixation | invalidate+sesi baru saat login | bandingkan JSESSIONID pra/pasca login |
| IDOR tenant/status | status by kode acak REG- (bukan enumerable id); cancel wajib sesi pemilik; dashboard filter pendaftar.id | coba kode/id milik akun lain |
| Credential di log | password/OTP/token tidak pernah di-log/audit/DB (hanya hash) | grep log UAT |

## Integrasi & kompatibilitas (§21.2/§21.5) — SAAT UAT (butuh Tomcat+DB deploy)

Submit→row Pendaftar aktif=false; pilihan jenis usaha tersimpan semua + jenisBisnis snapshot;
duplicate email→arahkan login; verifikasi→VERIFIED→job; worker→READY (trial mulai READY;
Pendaftar aktif=true); membership owner; Envers `new_audit.*__audit` terisi; login lama tetap;
Brand/Toko/Mesin existing tetap; PosApi tidak berubah; startup Hibernate sukses (16 mapping);
acceptance SQL ERD §8. Skenario E2E §21.6 (Apotek+Inventory/Sales, Bengkel Mobil) → 11-uat.md.
