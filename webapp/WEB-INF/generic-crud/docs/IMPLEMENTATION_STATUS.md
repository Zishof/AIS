# Status Implementasi Paket

## Yang sudah tersedia dalam paket

- Spesifikasi arsitektur lengkap Generic CRUD metadata-driven.
- Prompt eksekusi Codex/Claude Code bertahap.
- Source scanner untuk seluruh subclass `GeneralValueObject`.
- Manifest snapshot 1.501 subclass dan 1.490 concrete entity.
- Generator alias JSP UI/service yang idempotent dan tidak menimpa file manual.
- 1.543 page binding alias yang seluruhnya **disabled**.
- SQL konfigurasi, page binding, preference, saved view, custom action, import/export job, idempotency, selection token, dan audit.
- Contoh definisi Agama dan override Mahasiswa.
- Prototype UI responsif.
- Matriks test/security/performance/migration.

## Yang sengaja belum diklaim selesai

Paket ini **bukan WAR/drop-in framework yang sudah terkompilasi**. Java Generic CRUD engine, dispatcher produksi, query/mutation services, import/export workers, report templates, photo adapters, dan parity integration harus diterapkan oleh Codex/Claude Code pada Git terbaru lalu dibuild dan diuji terhadap konfigurasi AIS aktual.

Alasannya:

- snapshot tidak menjamin mapping/session/helper sama dengan Git terbaru;
- menuId, privilege, dan tenant/scope tidak dapat ditebak aman;
- 1.490 entity mempunyai risiko dan business rule yang berbeda;
- banyak entity mempunyai Action/custom button existing;
- activation otomatis dapat membuka data internal/transaksi/integrasi.

## Prinsip aktivasi

Semua seed dan alias berada pada status disabled/review-required. Target awal yang harus benar-benar diimplementasikan end-to-end adalah `Agama`, kemudian reference master lain secara bertahap. Mahasiswa, Siswa, Dosen, Guru, Pegawai, dan Tbmuser memerlukan adapter khusus.



## Tambahan V2.1

Sudah tersedia dalam paket sebagai spesifikasi/template/config/prototype:

- Audit Trail global/per-row;
- restore field, manual correction, revision/deep/mass restore;
- Super Admin active-row delete policy;
- complex form override provider;
- Mahasiswa full-page tab definition;
- SQL migration 003;
- expanded test matrix.

Belum boleh diklaim production-ready sebelum Java runtime, Envers bridge, domain adapters, DB migration staging, Ant build, parity tests, dan security tests benar-benar dijalankan pada checkout Git terbaru.
