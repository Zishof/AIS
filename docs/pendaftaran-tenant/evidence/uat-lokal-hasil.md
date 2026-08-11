# Evidence UAT Lokal — Pendaftaran Tenant (UAT-1..7)

Tanggal: 2026-08-12 · Lingkungan: Tomcat 9.0.89 lokal (JDK 1.8.0_202) + PostgreSQL 9.3 lokal,
context `/uat_tenant`, DB `uat_tenant` SEGAR (dibuat khusus UAT), mode `LEGACY` (default).
Deploy: salinan webapp + classes hasil `mvn -o compile` (commit `f8c17587`).
Startup Tomcat: **"Server startup in [694293] ms" tanpa SEVERE**; hbm2ddl membuat seluruh schema
(16 tabel control-plane + 1.511 tabel `new_audit`); seed 14 jenis usaha + 80 mapping modul; worker
`tenant-provisioning` hidup (job diproses tiap ±60 dtk). Smoke: `/`, `/login`, `/pendaftaran`,
`/EbisnisPublic` semua hidup.

## UAT-1 — E2E Apotek + Inventory/Sales ✅ LULUS
Wizard 8 langkah diisi PENUH lewat UI browser (bukan API): katalog 14 jenis dari DB dgn modul per
kartu; cek username real-time "Tersedia" + preview `apoteksehat_uat.ebisnis.id`; paket dari
Konfigurasi (250/400/600/750rb); review; submit → redirect status `REG-2026-000001`
EMAIL_VERIFICATION_PENDING, email ter-mask `bu***1@uji.example`, stepper progres.
Acceptance SQL pasca-submit: 1 pendaftar (`aktif=f` EKSPLISIT, `merupakansekolah=f`,
`domain=apoteksehat_uat`, snapshot `jenis_bisnis=APOTEK,INVENTORY_SALES`); 2 baris join jenis usaha
(id1 primary=t, id2 f); reservation RESERVED; consent TERMS+PRIVACY v2026-01; verifikasi PENDING
token HANYA hash 64-hex; profile PBKDF2WithHmacSHA256/120000; audit 5 event urut.
Verifikasi manual admin → PROVISIONING_QUEUED → worker → **READY dalam 1 tick**.
Acceptance READY: registry TEN-2026-000001 LEGACY; **trial 2026-08-12→2026-09-11 (tepat 30 hari
dari READY)**; pendaftar `aktif=t`; reservation **CONSUMED**; 16 step = 10 SUCCESS + 6 SKIPPED
(schema, sah LEGACY); membership OWNER ACTIVE; **entitlement UNION 23 modul ACTIVE tanpa duplikat**;
subdomain `apoteksehat_uat.ebisnis.id` primary ACTIVE.
Login pendaftar → dashboard: panel tenant badge **ACTIVE** (transisi saat login pertama), trial,
23 modul; Brand#1 → Toko#1 (ber-brand) → Mesin POS#1 (`apotek-sehat-cabang-dago-7253`, password &
QR tampil SEKALI). Logout→login ulang: data tetap (1/1/1). Envers: `pendaftaran_tenant__audit`=27,
`tenant_registry__audit`=14 baris.

## UAT-2 — E2E Bengkel Mobil ✅ LULUS
`REG-2026-000002` (`bengkelmaju_uat`) → READY. Entitlement: POS/INVENTORY/CUSTOMER/SUPPLIER/
PEMBELIAN/KAS_JURNAL/HUTANG_PIUTANG **ACTIVE**; KENDARAAN/WORK_ORDER/JASA_SERVIS/MEKANIK/
SPARE_PART/LAPORAN_BENGKEL **PLANNED** (dashboard menampilkan "belum tersedia", tanpa tombol semu).

## UAT-3 — Gating pra-READY ✅ LULUS
Login sebelum READY: `{"status":"91","description":"Akun ini sedang tidak aktif..."}` (password
benar terverifikasi PBKDF2, ditolak gerbang aktif=false) → mustahil membuat Brand/Toko/POS
pra-READY (perlu login). CATATAN: cabang fail-open akun LEGACY (pra-program) tidak dapat diuji di
DB segar (tidak ada akun legacy) — logika `alasanTidakBolehMutasi` mengembalikannya bila tak ada
baris `pendaftaran_tenant` (diverifikasi kode; uji runtime menyusul di DB dev berdata legacy).

## UAT-4 — Duplikat & race ✅ LULUS
check_email existing → EMAIL_ALREADY_REGISTERED ("silakan masuk"); check_username terpakai →
USERNAME_TAKEN_DOMAIN; `pendaftaran` → USERNAME_RESERVED; `Toko-Baru` → USERNAME_INVALID.
Harness `VerifikasiKonkurensiPendaftaran http://127.0.0.1:8080/uat_tenant 50`:
**"Skenario username-sama: sukses=1 ditolak=49 lain=0 → [LULUS] maksimal satu reservasi menang"**
(EXIT=0). Skenario-idempotency harness tertutup rate-limit submit-ip (lulus trivial) → dibuktikan
terpisah: replay `submit_registration` dgn idempotency_key ASLI REG-2 →
`REGISTRATION_ALREADY_CREATED` + registrationCode SAMA `REG-2026-000002`, TANPA baris baru
(count pendaftar tetap).

## UAT-5 — Backoffice admin ✅ LULUS
`mode=admin` (login `adminuat`, role `am`): daftar+filter+kolom lengkap; **Verifikasi Manual** ×3
(REG-1/2/5) → EMAIL_VERIFIED; **Tolak** REG-3 (reason wajib) → REJECTED + reservation otomatis
RELEASED (release manual kedua → RESERVATION_NOT_FOUND = guard anti-dobel bekerja); **jalur gagal**:
REG-5 reservasi dihapus paksa (SQL) → worker → **PROVISIONING_FAILED
`STEP_FAILED:RESERVE_USERNAME`** + auto-requeue backoff; penyebab dipulihkan → **admin Retry** →
READY (step SUCCESS lama TIDAK diulang); panel **Step** menampilkan 16 step + pesan error AMAN
("Detail teknis tercatat internal"). Gerbang: sesi anonim POST `admin_list` (CSRF valid) →
**ADMIN_ONLY**; GET mode=admin non-admin → redirect landing.

## UAT-6 — Bridge kompatibilitas ✅ LULUS
POST lama `aksi=daftar` → `{"status":"00","code":"REGISTRATION_MOVED","redirect":"/uat_tenant/pendaftaran"}`
(kontrak JS lama tetap: auto-redirect); TIDAK ada Pendaftar tersimpan (count tetap, tanpa "Toko
Lama"). `aksi=login`/`logout` lama berfungsi penuh (dipakai seluruh UAT ini).

## UAT-7 — Anti-automation ✅ LULUS
CSRF salah → CSRF_INVALID; honeypot terisi → REQUEST_REJECTED generik; formInstance tak dikenal/
terlalu cepat → REQUEST_REJECTED; rate limit check_email: **30 diizinkan lalu RATE_LIMITED**
(29+1 panggilan sebelumnya); submit-ip cap terbukti lewat harness (49 tertolak). Resend-limit
tidak tereksekusi terpisah (tidak ada permohonan menunggu-verifikasi tersisa) — jalur kode sama
dgn limiter lain yang terbukti.

## Audit event akhir (pendaftaran_audit_event)
PENDAFTAR_CREATED=6, REGISTRATION_SUBMITTED=6, BUSINESS_TYPES_SELECTED=6, CONSENT_ACCEPTED=6,
EMAIL_VERIFICATION_SENT=6, EMAIL_VERIFIED=4, MANUAL_VERIFICATION=4, TENANT_READY=4,
TENANT_ACTIVATED=2, PROVISIONING_FAILED=1, PROVISIONING_RETRIED=1, REGISTRATION_REJECTED=1.

## Temuan minor (backlog, bukan pemblokir)
1. Baris `provisioning_step` FAILED tidak tersisa pasca-gagal (ikut rollback transaksi step);
   info kegagalan tetap ada di `provisioning_job.error_code/error_message_safe`. Perbaikan:
   tandai step FAILED pada transaksi terpisah.
2. `clientIp()` mempercayai X-Forwarded-For tanpa daftar proxy tepercaya — di deployment TANPA
   reverse-proxy, header dapat dipalsukan utk menyebar limiter per-IP (limiter per-email/username
   tetap menahan). Perbaikan: kunci konfigurasi trust-proxy.
3. Artefak tester: baris reservation CONSUMED `retailgagal_uat` terhapus oleh sabotase yang kalah
   race (hanya data UAT lokal; keunikan tetap dijaga `tenant_registry.slug`).

## Kesimpulan
UAT-1..7 **LULUS** pada deployment lokal setara produksi (Tomcat+PG9.3). Status DoD → UAT_PASSED
utk seluruh alur; dua catatan cabang (fail-open legacy, resend-limit) + verifikasi ulang di server
dev berdata nyata dicatat utk operator.
