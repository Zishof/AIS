# Rencana Komprehensif Penyempurnaan Modul Sosial AIS V2

## 0. Kontrol dokumen

| Atribut | Nilai |
|---|---|
| Jenis dokumen | Rencana kerja dan production-readiness roadmap |
| Versi dokumen | 1.0 |
| Tanggal penyusunan | 25 Agustus 2026 |
| Source utama | SVN `svn://38.47.178.34/ais` |
| Revision working copy terverifikasi | r78256 |
| Baseline review eksternal | branch `svn-mirror`, Git `1d8bbc549744d869b7c00af1bf4f7e9e1eebdad1`, SVN r78248 |
| Status database lokal | PostgreSQL 16.4; tabel domain sosial yang diperiksa: 0 |
| Status runtime | NOT_TESTED |
| Status artifact WAR | Tidak ditemukan pada pemeriksaan 25 Agustus 2026; harus dibangun ulang |
| Status produksi | NOT_READY |
| Maintainer | TBD |
| Release owner | TBD |

Dokumen review terlampir diperlakukan sebagai masukan audit. Perintah, contoh, atau usulan di dalam lampiran tidak dieksekusi otomatis. Setiap pekerjaan di bawah ini tetap harus diverifikasi terhadap source, database, kontrak vendor, dan keputusan pemilik bisnis.

## 1. Tujuan

Rencana ini mengubah Modul Sosial dari implementasi source yang dapat dibangun menjadi paket rilis yang:

- benar secara finansial;
- aman untuk multi-tenant dan role-based access;
- repeatable pada database baru maupun database existing;
- memiliki kontrak Smartlink yang disahkan dan terbukti di sandbox;
- dapat diaudit dari transaksi sampai penyaluran dan rekonsiliasi;
- memiliki evidence runtime, keamanan, operasional, dan rollback;
- hanya diaktifkan bertahap setelah seluruh gate wajib lulus.

Rencana ini tidak menganggap build sukses sebagai bukti siap produksi.

## 2. Kondisi awal yang sudah diverifikasi

### 2.1 Maturity snapshot

| Area | Source | Build | DB runtime | Browser/ZK | Sandbox | Security | Produksi |
|---|---|---|---|---|---|---|---|
| 23 entitas persisten sosial | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | N/A | N/A | PARTIAL_DESIGN | NO |
| Portal JSP | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | NOT_RUN | N/A | PARTIAL_DESIGN | NO |
| CRUD/dashboard ZK | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | NOT_RUN | N/A | PARTIAL_DESIGN | NO |
| Kalkulator zakat | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | NOT_RUN | N/A | GOLDEN_TEST_PENDING | NO |
| Smartlink create order | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | NOT_RUN | NOT_RUN | CONTRACT_PENDING | NO |
| Callback Smartlink | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | N/A | NOT_RUN | THREAT_TEST_PENDING | NO |
| Receipt PDF | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | NOT_RUN | N/A | VERIFICATION_PENDING | NO |
| Penyaluran/rekonsiliasi | SOURCE_IMPLEMENTED | Build historis pernah PASS | NOT_RUN | NOT_RUN | N/A | PRIVILEGE_EVIDENCE_PENDING | NO |
| Accounting | STUB_NOOP | Compilable | NOT_RUN | N/A | N/A | N/A | NO |
| Registrasi umum | NOT_IMPLEMENTED | N/A | N/A | INFORMATIONAL_ONLY | N/A | N/A | NO |

Istilah status yang wajib dipakai selanjutnya:

- `DESIGNED`
- `SOURCE_IMPLEMENTED`
- `BUILD_PASS`
- `UNIT_TEST_PASS`
- `RUNTIME_SMOKE_PASS`
- `INTEGRATION_SANDBOX_PASS`
- `SECURITY_UAT_PASS`
- `PRODUCTION_READY`
- `STUB_NOOP`
- `DISABLED_BY_DEFAULT`
- `BLOCKED_EXTERNAL`
- `OUT_OF_SCOPE`

### 2.2 Fakta dan caveat source

1. `SocialAccountingAdapter` hanya memeriksa flag, mengubah payment menjadi `READY_TO_POST`, dan tidak membuat jurnal debit/kredit nyata. Status resmi: `STUB_NOOP + DISABLED_BY_DEFAULT`.
2. `SocialAdminDashboardService.unallocated()` saat ini menjumlahkan `grossDonationAmount` semua transaksi `PAID`. Angka tersebut belum mengurangi alokasi, refund, reversal, atau penyaluran dan tidak boleh dipakai sebagai saldo tersedia.
3. Hibernate memiliki 23 mapping entitas persisten sosial, tetapi database lokal belum memiliki tabel domain sosial.
4. `hbm2ddl.auto=update` aktif. Bootstrap Hibernate tidak boleh dijadikan satu-satunya migration strategy atau bukti schema/Envers benar.
5. Credential Smartlink dipusatkan di `SosialChannel`. Kontrak callback HMAC yang ada masih harus disahkan terhadap provider.
6. Revision r78252 mengubah `VirtualAccountBank`, dependency yang digunakan adapter Smartlink sosial; wajib dilakukan regression review dan regression test.
7. Revision r78253 mengubah `MyInclude`; wajib diregresi terhadap halaman ZK sosial.
8. File ZUL aktual berada di `webapp/WEB-INF/z/x/y/pages/master/sosial`. Dokumentasi lama yang menunjuk `WEB-INF/baru/pages/master/sosial` harus dibetulkan; URL menu tetap `/pages/master/sosial/...` sesuai mekanisme routing AIS.
9. Working copy `src`, `webapp`, dan `docs` bersih saat rencana ini dibuat.
10. Artifact WAR lama tidak ditemukan pada pemeriksaan terakhir; release manifest berikutnya harus memakai build baru dan checksum baru.

## 3. Scope rilis yang diusulkan

### 3.1 Scope wajib V1 pilot

- Master `SosialChannel` dan mapping kanal untuk Zakat, Infaq, Shodaqoh, Donasi.
- Dashboard dan CRUD ZK untuk transaksi `DRAFT`.
- Portal authenticated donor.
- Kalkulator zakat dengan policy versioning dan golden tests.
- Pembuatan transaksi dan payment attempt yang idempoten.
- Smartlink sandbox create order dan callback tervalidasi.
- Receipt PDF dan verifikasi receipt.
- Alokasi, penyaluran restricted fund, dan rekonsiliasi exception.
- Tenant isolation, RBAC, CSRF, rate limit, ownership, dan audit trail.
- Backup, rollback, monitoring, evidence, dan canary satu tenant.

### 3.2 Keputusan scope yang masih terbuka

| Requirement | Kondisi | Keputusan yang dibutuhkan |
|---|---|---|
| Registrasi member umum | NOT_IMPLEMENTED | Implementasikan dengan kontrol penuh atau tetapkan `OUT_OF_SCOPE_V1` dan hilangkan CTA menyesatkan |
| Donasi tamu | DISABLED | Tetap di luar V1 sampai ownership token aman tersedia |
| Akuntansi | STUB_NOOP | Implementasikan jurnal penuh sebagai fase terpisah atau tetap off pada pilot |
| Notifikasi email/WhatsApp | NOT_IMPLEMENTED | Tentukan channel dan SLA atau keluarkan dari V1 |
| Refund/reversal otomatis | Belum lengkap | Tentukan apakah manual-operasional atau wajib terintegrasi sebelum pilot |
| Wakaf | Tidak termasuk domain saat ini | `OUT_OF_SCOPE` sampai legal, accounting, dan governance khusus tersedia |

### 3.3 Rekomendasi scope

Untuk pilot pertama, registrasi umum, donasi tamu, posting jurnal otomatis, dan Wakaf sebaiknya tetap di luar scope. Pilot memakai akun AIS existing, Smartlink sandbox/kanal pilot, dan rekonsiliasi finansial terkontrol. Keputusan final harus dicatat dalam ADR dan disetujui product owner.

## 4. Workstream dan fase pelaksanaan

Setiap fase memiliki output dan exit gate. Fase tidak boleh dinyatakan selesai hanya karena dokumennya dibuat; evidence harus dilampirkan.

### Fase 0 — Governance, rebaseline, dan release identity

**Tujuan:** menetapkan satu baseline yang dapat direproduksi sebelum perubahan baru.

Pekerjaan:

1. Bekukan revision target dari `src`, `webapp`, dan `docs`.
2. Audit delta r78248–r78256, terutama `VirtualAccountBank` r78252 dan `MyInclude` r78253.
3. Buat `RELEASE_MANIFEST.md` berisi revision, dirty state, toolchain, artifact hash, schema fingerprint, flags, tenant pilot, policy version, dan rollback artifact.
4. Tentukan canonical build: Maven atau Ant, JDK, Tomcat, source/target, dan nama artifact.
5. Tambahkan document-control block ke handoff dan dokumen evidence.
6. Tetapkan owner/RACI sementara untuk product, syariah, legal, finance, payment, DBA, security, operations, dan release approval.

Deliverable:

- `RELEASE_MANIFEST.md`
- `RACI_AND_RELEASE_APPROVAL.md`
- delta report r78248–r78256
- build-environment manifest

Exit gate:

- revision source/web/docs sama dan bersih;
- seluruh perubahan dependency terkait sosial sudah direview;
- owner dan release approver terisi;
- artifact belum perlu produksi, tetapi build contract sudah tunggal dan jelas.

### Fase 1 — Koreksi kebenaran finansial P0

**Tujuan:** mencegah dashboard dan workflow memakai definisi finansial yang salah.

Pekerjaan:

1. Susun `FINANCIAL_INVARIANTS.md` bersama finance/product owner.
2. Definisikan komponen berikut dengan currency, scale, rounding, dan lifecycle:
   - gross donation;
   - admin fee;
   - kontribusi layanan sukarela;
   - total charged;
   - settled donation;
   - posted allocation;
   - posted distribution;
   - posted refund;
   - posted reversal;
   - correction;
   - unallocated available balance;
   - allocation available balance.
3. Ubah label/query `unallocated` agar mengikuti formula yang telah disetujui. Sebelum itu, label harus menyatakan `total gross donation PAID`, bukan saldo belum dialokasikan.
4. Definisikan status mana yang dianggap settled dan waktu pengakuannya.
5. Tambahkan unit/integration test untuk donasi, fee, alokasi, penyaluran, refund, reversal, duplicate callback, dan correction.
6. Tambahkan invariant query read-only pada SQL verification.

Invariant minimum:

```text
total_charged = donation_amount + admin_fee + voluntary_service_contribution
settled_donation = successful_payment_donation_component - posted_refund - posted_reversal
unallocated_available = settled_donation - posted_allocation
allocation_available = posted_allocation - posted_distribution
browser redirect MUST NOT mark payment PAID
duplicate callback MUST NOT post twice
receipt MUST only exist after server-verified successful payment
restricted fund MUST NOT be distributed to an incompatible beneficiary category
```

Deliverable:

- `FINANCIAL_INVARIANTS.md`
- perubahan query/dashboard
- test vectors dan hasil test
- `sql/005_verify_financial_invariants.sql`

Exit gate:

- formula ditandatangani finance/product owner;
- seluruh angka dashboard dapat diturunkan dari ledger/event yang jelas;
- test normal, refund, reversal, dan concurrency lulus.

### Fase 2 — Kontrak API, state machine, dan otorisasi

**Tujuan:** memastikan setiap entry point mempunyai contract, state guard, ownership, tenant guard, dan privilege yang eksplisit.

Pekerjaan:

1. Inventaris seluruh route servlet, API action, ZK action, dan service sensitif.
2. Buat `API_CONTRACT.md` dengan method, URL/action, auth, privilege, CSRF, request, response, error, rate limit, idempotency, dan allowed state.
3. Buat `STATE_MACHINE_AND_TRANSITIONS.md` untuk transaksi, payment attempt, allocation, distribution, reconciliation, correction, receipt, refund/reversal, dan accounting posting.
4. Buat `RBAC_AUTHORIZATION_MATRIX.md` untuk anonymous, member AIS, general donor bila dipilih, operator, amil, reviewer, finance, auditor, admin, dan super admin.
5. Audit service entry point, terutama posting penyaluran, koreksi, refund/reversal, channel credential, dan rekonsiliasi.
6. Tambahkan explicit privilege/capability guard di boundary yang belum terlindungi.
7. Uji direct endpoint denial, tidak hanya visibility menu.

Contoh kolom RBAC wajib:

```text
route/action | actor | menu/capability | READ/CREATE/UPDATE/DELETE/APPROVE/REJECT |
tenant rule | ownership rule | CSRF | source state | target state | audit event
```

Deliverable:

- `API_CONTRACT.md`
- `STATE_MACHINE_AND_TRANSITIONS.md`
- `RBAC_AUTHORIZATION_MATRIX.md`
- automated authorization/IDOR tests

Exit gate:

- tidak ada mutasi sensitif tanpa privilege eksplisit;
- cross-tenant dan cross-owner access ditolak;
- semua transition memiliki actor, guard, side effect, audit, dan compensating action.

### Fase 3 — Database schema, constraint, Envers, dan migration safety

**Tujuan:** membuat deployment database repeatable dan dapat diverifikasi.

Pekerjaan:

1. Hasilkan schema manifest dari 23 mapping persisten beserta base fields tenant/audit.
2. Definisikan expected table, column, type, nullability, FK, unique constraint, index, sequence, dan audit table.
3. Tambahkan SQL read-only:
   - `002_verify_social_schema.sql`
   - `003_verify_social_constraints.sql`
   - `004_verify_envers_audit_tables.sql`
   - `005_verify_financial_invariants.sql`
   - `006_preview_legacy_backfill.sql`
4. Buat migration versioned/repeatable. `hbm2ddl=update` hanya boleh digunakan untuk discovery/bootstrap staging, bukan sebagai satu-satunya kontrol produksi.
5. Jalankan clean DB bootstrap di staging disposable.
6. Bandingkan actual-vs-expected dan buat forward-fix untuk gap.
7. Restart aplikasi kedua kali dengan schema validation dan pastikan tidak ada perubahan tak terduga.
8. Tambahkan schema fingerprint ke release manifest.

Deliverable:

- schema manifest
- migration/forward-fix SQL
- verification SQL 002–006
- actual-vs-expected report
- Envers verification evidence

Exit gate:

- 23 entitas persisten dan seluruh audit table terverifikasi;
- FK/unique/index/precision benar;
- migration idempoten atau memiliki version guard;
- startup kedua bersih;
- backup dan restore drill lulus.

### Fase 4 — Legacy migration dan backfill

**Tujuan:** membawa data sosial lama ke domain baru tanpa kehilangan tenant, relasi, atau total finansial.

Pemetaan minimum:

```text
Donatur -> SocialDonorIdentity
ProgramDonatur -> SocialProgramExtension
ProgramDonatur.donaturs teks/CSV -> relasi terstruktur
PenyaluranDonasi legacy -> AlokasiDonasi/DetailPenyaluranDonasi
record tanpa tenant -> aturan resolusi atau rejected queue
```

Pekerjaan:

1. Buat `LEGACY_DATA_MIGRATION_AND_BACKFILL.md`.
2. Definisikan natural key dan duplicate detection.
3. Buat preview/dry-run read-only dengan klasifikasi mapped, duplicate, ambiguous, no-tenant, rejected.
4. Buat backfill idempoten dan resumable.
5. Simpan mapping report source ID -> target ID.
6. Rekonsiliasi count dan total finansial sebelum/sesudah.
7. Hindari rollback destruktif; gunakan forward-fix dan mapping log.

Deliverable:

- migration specification
- preview SQL/report
- backfill tool/script
- reconciliation report
- rejected-row remediation procedure

Exit gate:

- dry-run disetujui owner data;
- rerun tidak membuat duplikasi;
- semua ambiguity memiliki disposition;
- count dan total terrekonsiliasi.

### Fase 5 — Runtime bootstrap dan application smoke test

**Tujuan:** membuktikan source berfungsi pada application server dan database nyata.

Pekerjaan:

1. Build ulang WAR dari baseline bersih dan catat SHA-256.
2. Deploy ke staging dengan semua feature flag sosial off.
3. Pantau log Hibernate, servlet registration, JSP compilation, ZK loading, Envers, dan startup listener menu.
4. Login ulang sebagai role `am`; uji seluruh 11 menu sosial.
5. Uji dashboard/tab CRUD Zakat, Infaq, Shodaqoh, Donasi, dan SosialChannel.
6. Uji portal, API, kalkulator, receipt generation, receipt verification, dan error handling.
7. Regresi `MyInclude` r78253 pada seluruh workspace ZK.
8. Uji responsive layout 1920, 1366, tablet, dan mobile 390; lakukan accessibility smoke.
9. Rekam command, environment, log teredaksi, screenshot, pass/fail, dan checksum dalam `TEST_EVIDENCE.md`.

Deliverable:

- WAR dan checksum
- deployment/startup log teredaksi
- runtime smoke result
- visual/accessibility evidence
- defect list

Exit gate:

- startup tanpa schema error;
- seluruh route/menu utama dapat dibuka;
- CRUD DRAFT dan lock non-DRAFT terbukti;
- tidak ada secret/PII bocor di log;
- defect P0/P1 runtime tertutup.

### Fase 6 — Smartlink contract dan sandbox E2E

**Tujuan:** mengganti asumsi integrasi dengan kontrak provider yang resmi dan evidence sandbox.

Pekerjaan:

1. Dapatkan dokumentasi Smartlink resmi dan credential sandbox melalui secret channel.
2. Buat `SMARTLINK_CONTRACT_AND_EVIDENCE.md`:
   - create order endpoint/request/response;
   - callback method, header, raw body, canonical string, encoding/signature;
   - timestamp tolerance dan replay defense;
   - IP source dan trusted reverse proxy;
   - HTTP response semantics;
   - timeout, retry/backoff, expiry, inquiry;
   - settlement, partial payment, reversal, refund;
   - duplicate/out-of-order callback;
   - secret rotation dan grace period.
3. Audit dampak perubahan `VirtualAccountBank` r78252 pada adapter sosial.
4. Sesuaikan adapter/callback tanpa melonggarkan pemeriksaan HMAC, nominal, currency, tenant, channel, dan idempotency.
5. Jalankan test matrix:
   - create-order sukses/gagal/timeout;
   - callback valid;
   - invalid HMAC;
   - IP tidak dikenal;
   - amount/currency mismatch;
   - duplicate dan callback paralel;
   - callback out-of-order;
   - expired-then-paid;
   - refund/reversal bila provider mendukung;
   - secret lintas channel dan secret rotation.
6. Pastikan transaksi berbeda dapat memakai kanal/credential berbeda sesuai mapping, tanpa menyimpan secret pada transaksi.

Deliverable:

- provider-approved contract
- fixture sandbox teredaksi
- automated integration test/evidence
- reconciliation hasil sandbox

Exit gate:

- create order dan callback lulus sandbox;
- duplicate/paralel tidak double-post;
- mismatch masuk exception queue;
- credential tidak muncul pada log/UI/export;
- payment/receipt/reconciliation konsisten.

### Fase 7 — Kalkulator zakat dan kebijakan syariah

**Tujuan:** membuktikan formula dan policy version dapat dipertanggungjawabkan.

Pekerjaan:

1. Buat `ZAKAT_GOLDEN_TEST_VECTORS.md`.
2. Definisikan setiap formula key, policy version, nisab, input, periodisasi, rounding, dan expected output.
3. Uji below/at/above nisab, monthly/annual, zero, maksimum, decimal precision, invalid/negative input, dan perubahan policy efektif.
4. Pastikan snapshot perhitungan tidak berubah saat policy baru diterbitkan.
5. Minta persetujuan owner syariah atas formula dan sumber nilai nisab.

Deliverable:

- golden vectors
- automated test result
- policy approval record

Exit gate:

- semua golden case lulus;
- snapshot versioning lulus;
- formula disetujui owner syariah.

### Fase 8 — Security, privacy, retention, dan secret management

**Tujuan:** menutup risiko tenant, payment, PII, dan credential.

Pekerjaan:

1. Buat `THREAT_MODEL.md` untuk IDOR, tenant swap, replay, nominal tampering, duplicate/out-of-order callback, credential theft, privilege escalation, receipt forgery, registration abuse, XSS, CSRF, injection, dan DoS.
2. Buat `PRIVACY_RETENTION_POLICY.md` untuk PII donor, transaksi, callback payload, receipt, doa/pesan, anonim publik/internal, consent, export/delete request, legal hold, dan log masking.
3. Definisikan key ownership, version, rotation, grace period, backup/restore, masking, dan incident procedure.
4. Evaluasi migrasi bertahap dari `Common.desEncrypter` menuju encrypted-secret format yang versioned dan rotatable.
5. Jalankan security UAT: tenant isolation, ownership, RBAC direct endpoint, CSRF, rate limit, replay, signature, injection, XSS, secret leakage, dan session handling.
6. Bila registrasi umum masuk scope, tambahkan email/WA verification, anti-abuse, duplicate merge, minimal role, recovery, consent, dan larangan self-select role.

Deliverable:

- threat model
- privacy/retention policy
- secret rotation runbook
- security test evidence

Exit gate:

- seluruh P0/P1 security ditutup;
- cross-tenant/owner access ditolak;
- secret rotation diuji;
- privacy/legal owner menyetujui retention.

### Fase 9 — Akuntansi: keputusan scope atau implementasi penuh

**Tujuan:** mencegah boundary class dianggap integrasi jurnal yang selesai.

Pilihan A untuk pilot: tetapkan `OUT_OF_SCOPE_V1`, pertahankan flag off, dan buat proses rekonsiliasi/export manual yang disetujui finance.

Pilihan B bila diwajibkan sebelum produksi: implementasikan `ACCOUNTING_POSTING_SPEC.md` yang mencakup:

- mapping akun per tenant/jenis dana/channel/fee;
- journal header dan detail debit/kredit;
- balancing validation;
- idempotency key;
- payment/settlement journal;
- distribution journal;
- refund/reversal journal;
- correction journal;
- posting status dan retry;
- period lock;
- rekonsiliasi ke buku besar;
- audit dan approval.

Deliverable:

- decision record accounting scope
- `ACCOUNTING_POSTING_SPEC.md`
- bila dipilih: implementation, test, dan finance reconciliation evidence

Exit gate:

- flag tetap off bila status masih stub;
- bila diaktifkan, jurnal balanced, idempoten, reversible, terrekonsiliasi, dan disetujui finance.

### Fase 10 — Observability, incident response, dan disaster recovery

**Tujuan:** memastikan kegagalan dapat dideteksi, ditangani, dan dipulihkan.

Pekerjaan:

1. Buat `OBSERVABILITY_INCIDENT_RUNBOOK.md`.
2. Definisikan correlation ID tanpa PII.
3. Tambahkan metric dan alert untuk callback invalid/mismatch, pending/expired payment, duplicate, reconciliation backlog, receipt failure, distribution failure, tenant routing failure, dan Smartlink latency/error.
4. Tetapkan SLO/SLA, threshold, owner, dan escalation path.
5. Buat dashboard query dan synthetic check untuk portal, checkout, callback health, receipt verification, dan transparansi.
6. Lakukan backup/restore drill untuk database, secret/config, receipt metadata, serta reconciliation state.
7. Uji kill/restart, retry, dan recovery tanpa double posting.

Deliverable:

- monitoring dashboard/queries
- alert rules
- incident runbook
- DR test report

Exit gate:

- alert P0 diuji end-to-end;
- owner on-call jelas;
- restore memenuhi RPO/RTO yang disetujui;
- recovery tidak merusak idempotency.

### Fase 11 — Evidence pack, pilot, canary, dan production gate

**Tujuan:** membuat keputusan rilis berdasarkan bukti, bukan asumsi.

Pekerjaan:

1. Konsolidasikan `TEST_EVIDENCE.md` dengan environment, revision, command, log/screenshot, checksum, tester, waktu, hasil, dan defect.
2. Jalankan full regression: DB, portal, ZK, API, calculator, Smartlink, callback, receipt, allocation, distribution, reconciliation, RBAC, tenant, security, performance smoke, dan DR.
3. Pastikan feature flags tetap off secara default.
4. Pilih satu tenant pilot, satu SosialChannel sandbox/production-approved, limit nominal, dan periode canary.
5. Aktifkan flag bertahap: read-only portal -> calculator -> receipt -> collection sandbox/pilot -> transparency. Accounting hanya bila fase 9 pilihan B lulus.
6. Terapkan go/no-go checklist dan sign-off RACI.
7. Siapkan rollback artifact, rollback config, dan reconciliation procedure untuk transaksi in-flight.

Deliverable:

- release dossier final
- signed go/no-go
- canary plan dan rollback plan
- post-pilot reconciliation report

Exit gate produksi:

- seluruh P0 tertutup;
- P1 yang ditunda mempunyai owner, due date, dan risk acceptance;
- runtime, sandbox, security UAT, backup/restore, dan reconciliation PASS;
- artifact/schema/config/secret versions tercatat;
- product, syariah, finance, security, DBA, operations, dan release owner menyetujui.

## 5. Dependency dan jalur kritis

```text
Fase 0 Rebaseline
  -> Fase 1 Financial invariants
  -> Fase 2 API/RBAC/state machine
  -> Fase 3 Schema/migration
      -> Fase 4 Legacy backfill
      -> Fase 5 Runtime smoke
          -> Fase 6 Smartlink sandbox
          -> Fase 7 Zakat golden tests
          -> Fase 8 Security/privacy
              -> Fase 10 Observability/DR
                  -> Fase 11 Canary/release

Fase 9 Accounting berjalan setelah Fase 1–3 dan dapat dikeluarkan dari V1 pilot melalui keputusan formal.
```

Jalur kritis utama adalah financial invariants -> schema -> runtime -> Smartlink sandbox -> security/tenant test -> evidence -> canary.

## 6. Prioritas backlog

### P0 — sebelum pilot

1. Rebaseline r78256 dan release manifest.
2. Tandai accounting `STUB_NOOP`.
3. Benahi definisi/query `unallocated`.
4. Financial invariants dan state machine.
5. API/RBAC matrix dan service-entry privilege audit.
6. Schema manifest, migration, Envers, constraint, dan verification SQL.
7. Legacy backfill preview dan keputusan data ambigu.
8. Runtime bootstrap dan evidence.
9. Smartlink contract serta sandbox E2E.
10. Security/tenant/idempotency test.
11. Keputusan registrasi umum.

### P1 — wajib untuk operasi stabil

- Golden vectors zakat.
- Privacy/retention dan secret rotation.
- Observability, incident response, RACI.
- Canonical build dan release dossier.
- Visual/accessibility evidence.

### P2 — quality hardening

- SBOM dan dependency/security scan.
- Performance baseline dan load test.
- ADR untuk keputusan arsitektur.
- DR rehearsal dan synthetic monitoring.

### P3 — roadmap opsional

- Recurring donation.
- Multi-currency.
- Preference center komunikasi donor.
- Impact analytics yang dapat diaudit.
- Wakaf sebagai domain terpisah setelah kajian lengkap.

## 7. RACI awal yang harus diisi

| Area | Accountable | Responsible | Consulted | Informed |
|---|---|---|---|---|
| Scope/product | TBD | TBD | Finance, Syariah, Ops | Stakeholder |
| Kebijakan zakat | TBD | TBD | Legal, Product | Engineering |
| Smartlink/payment | TBD | TBD | Security, Finance | Ops |
| Accounting | TBD | TBD | Product, DBA | Auditor |
| Database/migration | TBD | TBD | Engineering, Finance | Ops |
| Security/privacy | TBD | TBD | Legal, DBA | Release owner |
| Deployment/monitoring | TBD | TBD | DBA, Payment | Helpdesk |
| Release approval | TBD | TBD | Semua owner | Stakeholder |

Tidak boleh ada produksi sebelum kolom accountable dan responsible terisi.

## 8. Evidence pack minimum

Setiap evidence harus menyebut revision, environment, timestamp, pelaksana, command/scenario, expected, actual, status, log/screenshot teredaksi, defect reference, dan checksum artifact.

Evidence minimum:

- clean build dan WAR SHA-256;
- DB backup/restore;
- 23 entity, audit table, FK, unique, index, precision;
- menu/RBAC/direct endpoint denial;
- cross-tenant dan cross-owner rejection;
- ZK/JSP/API/receipt runtime;
- zakat golden vectors;
- Smartlink create order/callback matrix;
- duplicate/concurrent/out-of-order callback;
- receipt dan verification;
- allocation/distribution/restricted-fund;
- reconciliation mismatch/resolution;
- secret masking/rotation;
- alert/incident/DR;
- canary reconciliation.

## 9. Dokumen target

Struktur dokumentasi akhir yang direncanakan:

```text
docs/sosial/
├── README.md
├── HANDOFF_AI_LANJUTAN.md
├── RENCANA_PRODUCTION_READINESS_V2.md
├── RELEASE_MANIFEST.md
├── API_CONTRACT.md
├── RBAC_AUTHORIZATION_MATRIX.md
├── STATE_MACHINE_AND_TRANSITIONS.md
├── FINANCIAL_INVARIANTS.md
├── SMARTLINK_CONTRACT_AND_EVIDENCE.md
├── ACCOUNTING_POSTING_SPEC.md
├── LEGACY_DATA_MIGRATION_AND_BACKFILL.md
├── ZAKAT_GOLDEN_TEST_VECTORS.md
├── TEST_EVIDENCE.md
├── THREAT_MODEL.md
├── PRIVACY_RETENTION_POLICY.md
├── OBSERVABILITY_INCIDENT_RUNBOOK.md
├── RACI_AND_RELEASE_APPROVAL.md
├── CONFIGURATION.md
├── DEPLOYMENT_RUNBOOK.md
├── UAT_SECURITY_CHECKLIST.md
├── ZKOSS_MENU_CRUD.md
├── adr/
│   ├── 001-additive-domain-model.md
│   ├── 002-tenant-resolution.md
│   ├── 003-smartlink-channel-boundary.md
│   ├── 004-accounting-boundary.md
│   └── 005-general-registration-policy.md
└── sql/
    ├── 001_social_indexes.sql
    ├── 002_verify_social_schema.sql
    ├── 003_verify_social_constraints.sql
    ├── 004_verify_envers_audit_tables.sql
    ├── 005_verify_financial_invariants.sql
    └── 006_preview_legacy_backfill.sql
```

Dokumen kosong tidak dianggap deliverable selesai. Isinya harus ditautkan ke evidence atau keputusan yang dapat diperiksa.

## 10. Aturan keselamatan pelaksanaan

- Jangan menulis secret database/Smartlink/callback ke SVN, log, screenshot, atau fixture.
- Jangan mengaktifkan `sosial_accounting_integration_enabled` selama adapter masih stub.
- Jangan menampilkan `unallocated` sebagai saldo tersedia sebelum formula dan query lulus review.
- Jangan menjadikan menu visibility sebagai satu-satunya authorization control.
- Jangan mempercayai tenant, owner, nominal, fee, status, atau payment success dari browser.
- Jangan melonggarkan signature/IP/amount/currency/idempotency untuk meloloskan sandbox.
- Jangan mengandalkan `hbm2ddl=update` sebagai migration produksi.
- Jangan mengubah histori paid/settled melalui CRUD umum.
- Jangan drop tabel sebagai rollback awal; gunakan feature flag off, artifact rollback, dan forward-fix schema.
- Jangan mengedit mirror `src/main/java`; source utama berada di `src/main/src`.
- Jangan menyertakan perubahan modul lain dalam commit sosial tanpa scope dan review terpisah.

## 11. Definition of Done keseluruhan

Modul Sosial dinyatakan siap produksi hanya jika release identity konsisten, formula finansial disetujui, schema dan audit repeatable, migration legacy terrekonsiliasi, semua entry point memiliki RBAC/tenant/ownership guard, runtime dan Smartlink sandbox lulus, security UAT serta DR lulus, observability aktif, artifact/secret/config tercatat, seluruh flag diaktifkan bertahap melalui canary, dan seluruh owner pada RACI memberikan persetujuan go-live.
