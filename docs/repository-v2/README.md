# Dokumentasi Repository AIS V2

- [Handoff implementasi 24 Agustus 2026](HANDOFF_IMPLEMENTASI_REPOSITORY_2026-08-24.md) — ringkasan
  lengkap fitur yang sudah diterapkan, keputusan teknis, status validasi, batasan, dan prompt untuk
  AI/pengembang berikutnya.
- [Konfigurasi integrasi](INTEGRATIONS.md) — JVM property, integrasi eksternal, dan validasi setelah
  deployment.
- [Rollout dan rollback](ROLLOUT_ROLLBACK.md) — feature flag V1/V2, multi-tenant, keunikan OAI, dan
  prosedur rollback.
- `validate-repository-server.sh` — smoke validation publik read-only setelah deployment.

Status source tidak sama dengan status runtime. Ikuti bagian validasi server pada dokumen handoff
sebelum menyatakan release selesai.
