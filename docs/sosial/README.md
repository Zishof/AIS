# Modul Sosial AIS V1

Baseline awal implementasi: SVN revision 78235. Inventaris handoff source disusun pada r78245; audit source lanjutan terakhir menggunakan baseline r78523 pada 29 Agustus 2026.

## Mulai dari sini

Untuk pengembang atau AI yang akan melanjutkan pekerjaan, baca [HANDOFF_AI_LANJUTAN.md](HANDOFF_AI_LANJUTAN.md). Dokumen tersebut memuat inventaris implementasi, status database dan build, desain Smartlink/SosialChannel, risiko, batasan, serta urutan deployment dan pengujian yang disarankan.

Rencana penyempurnaan berdasarkan audit dokumentasi terbaru tersedia di [RENCANA_PRODUCTION_READINESS_V2.md](RENCANA_PRODUCTION_READINESS_V2.md). Rencana tersebut direbaseline ke working copy r78256 dan memisahkan pekerjaan source, database, runtime, sandbox, keamanan, serta production gate.

## Dossier production-readiness

- [RELEASE_MANIFEST.md](RELEASE_MANIFEST.md)
- [CODE_REVIEW_2026-08-29.md](CODE_REVIEW_2026-08-29.md)
- [TEST_EVIDENCE.md](TEST_EVIDENCE.md)
- [FINANCIAL_INVARIANTS.md](FINANCIAL_INVARIANTS.md)
- [STATE_MACHINE_AND_TRANSITIONS.md](STATE_MACHINE_AND_TRANSITIONS.md)
- [API_CONTRACT.md](API_CONTRACT.md)
- [RBAC_AUTHORIZATION_MATRIX.md](RBAC_AUTHORIZATION_MATRIX.md)
- [SMARTLINK_CONTRACT_AND_EVIDENCE.md](SMARTLINK_CONTRACT_AND_EVIDENCE.md)
- [ACCOUNTING_POSTING_SPEC.md](ACCOUNTING_POSTING_SPEC.md)
- [LEGACY_DATA_MIGRATION_AND_BACKFILL.md](LEGACY_DATA_MIGRATION_AND_BACKFILL.md)
- [ZAKAT_GOLDEN_TEST_VECTORS.md](ZAKAT_GOLDEN_TEST_VECTORS.md)
- [GENERAL_REGISTRATION_DECISION.md](GENERAL_REGISTRATION_DECISION.md)
- [THREAT_MODEL.md](THREAT_MODEL.md)
- [PRIVACY_RETENTION_POLICY.md](PRIVACY_RETENTION_POLICY.md)
- [OBSERVABILITY_INCIDENT_RUNBOOK.md](OBSERVABILITY_INCIDENT_RUNBOOK.md)
- [RACI_AND_RELEASE_APPROVAL.md](RACI_AND_RELEASE_APPROVAL.md)

SQL verifikasi read-only tersedia sebagai `002`–`006` di folder [sql](sql). Script `005` dan `006` dijalankan setelah tabel terbentuk pada deployment target.

## Lokasi

- Java/model/service: `src/main/src/ais/...`
- Hibernate mapping: `src/main/src/hibernate.cfg.xml`
- JSP: `src/main/webapp/WEB-INF/baru/modul/sosial`
- CSS/JS: `src/main/webapp/css/baru/sosial.css`, `src/main/webapp/js/baru/sosial.js`
- Dokumentasi: `src/main/docs/sosial`

`src/main/java` adalah mirror dan bukan target edit utama.

## Komponen yang diimplementasikan

- Domain additive untuk tenant, donor identity, jenis dana/zakat, policy dan snapshot kalkulasi, program extension, transaksi, alokasi, payment attempt, receipt, penyaluran typed, rekonsiliasi, koreksi, dan doa/pesan.
- `BigDecimal` untuk seluruh nominal baru.
- Tenant context yang ditentukan dari user/institusi atau host, bukan parameter browser.
- Identity resolver idempotent untuk pengguna AIS.
- Compliance gate dan feature flags default-off.
- Portal publik `/sosial`, program, kalkulator, checkout, status, riwayat, transparansi, bantuan, kebijakan, workspace, serta verifikasi bukti.
- API typed `/sosial-api` dengan CSRF, rate limit, validation, ownership, dan server-calculated total.
- Smartlink adapter khusus sosial tanpa membuat tagihan akademik palsu. Credential diisi sekali pada master `SosialChannel`; jenis dana dan transaksi hanya menyimpan referensinya.
- Callback `/SosialSmartlinkCallback` dengan IP allow-list, HMAC-SHA256, fingerprint, lock, amount check, duplicate handling, mismatch queue, allocation posting, dan receipt.
- PDF bukti setor on-demand di `/sosial-receipt-pdf`.
- Service posting penyaluran dengan restricted balance check.
- Service rekonsiliasi settlement dan accounting boundary yang default-off.
- CRUD ZKoss Zakat, Infaq, Shodaqoh, Donasi, dan SosialChannel dengan dashboard pada tab pertama serta CRUD pada tab kedua.
- Seeder menu startup idempoten dan hak penuh role Administrator `am`; lihat `ZKOSS_MENU_CRUD.md`.

## Status aktivasi

Seluruh fitur mutasi uang harus tetap nonaktif sampai konfigurasi tenant, policy, legal, gateway, receipt, RBAC, dan callback tersedia. Tidak ada deployment atau callback end-to-end test pada implementasi ini; keduanya dilakukan setelah artifact dipasang pada environment target.

## Batasan sebelum pilot

- General registration belum membuat akun baru dari portal sosial. Halaman pendaftaran hanya menjelaskan kesiapan dan harus dihubungkan ke lifecycle registrasi AIS yang disetujui; flag tetap off.
- Donasi tamu belum diaktifkan. V1 mewajibkan login AIS sampai token kepemilikan transaksi tamu yang dapat kedaluwarsa tersedia.
- Notification sender/worker belum dihubungkan ke email/WhatsApp existing.
- Accounting adapter tidak memposting jurnal sebelum mapping akun disetujui dan flag diaktifkan.
- Kontrak header callback implementasi saat ini adalah `X-Smartlink-Signature = hex(HMAC-SHA256(secret SosialChannel transaksi, rawBody))`. Kontrak ini wajib dicocokkan dengan Smartlink saat deployment; jangan melonggarkan verifikasi untuk membuat callback lolos.
- CRUD dan dashboard ZKoss sudah tersedia di source serta menu/hak admin lokal sudah terdaftar. Namun seluruhnya masih memerlukan verifikasi runtime setelah deployment dan login ulang.

## Build lokal

Kelas baru telah dikompilasi terarah menggunakan `-source 1.6 -target 1.6`. Build penuh `mvn -DskipTests package` juga berhasil dan menghasilkan `build/maven/ais.war`. Artifact tetap harus dibangun ulang oleh pipeline AIS yang berwenang sebelum deployment produksi.
