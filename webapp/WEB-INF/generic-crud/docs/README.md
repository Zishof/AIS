# Paket Implementasi Master CRUD Generik AIS — V2.1

Paket ini adalah spesifikasi eksekusi dan perangkat bantu untuk membangun **Master CRUD generik berbasis metadata** bagi seluruh entitas AIS yang merupakan turunan langsung maupun tidak langsung dari:

```java
ais.database.model.GeneralValueObject
```

> Catatan terminologi: di permintaan awal tertulis `GenericValueObject`. Pada source AIS yang dianalisis, class dasarnya bernama **`GeneralValueObject`**. Implementasi harus mengikuti nama aktual source agar tidak membuat class paralel yang membingungkan.

## Mengapa harus berupa engine metadata, bukan 1.500 halaman yang disalin

Pemindaian snapshot source menghasilkan lebih dari seribu entitas turunan `GeneralValueObject`. Membuat action/form/query satu per satu akan mengulangi bug, tidak konsisten, sulit diamankan, dan sulit dipelihara. Paket ini memakai pola:

1. **Satu engine CRUD generik** untuk query, filter, sort, paging, form, lookup, impor, ekspor, audit, dan privilege.
2. **Metadata per entitas/field/page binding** yang hanya boleh memakai property yang benar-benar terdaftar pada Hibernate runtime metadata.
3. **Adapter/override** untuk business rule khusus, foto, approval, scope data, dan tombol custom.
4. **Alias JSP per modul/entitas** agar mengikuti struktur `WEB-INF/new/{MODUL}/uiux` dan `services` yang sudah disepakati.
5. **Default deny / default disabled** sampai entitas lulus verifikasi mapping, menu, privilege, tenant scope, dan parity test.

## Isi paket

- `PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT.md` — instruksi utama untuk Codex/Claude Code.
- `ARSITEKTUR_TEKNIS_GENERIC_CRUD.md` — desain class, service, registry, query, import/export, approval, foto, custom action, dan routing.
- `UIUX_SPEC_GENERIC_CRUD.md` — spesifikasi desktop/tablet/mobile dan perilaku komponen.
- `MATRIX_REQUIREMENT_DAN_ACCEPTANCE.md` — pemetaan seluruh permintaan ke acceptance criteria.
- `SECURITY_PERFORMANCE_BEST_PRACTICE.md` — ketentuan keamanan, performa, audit, dan data scope.
- `MIGRATION_PLAN.md` — strategi bertahap tanpa merusak Action/ZUL existing.
- `sql/001_generic_crud_configuration.sql` — tabel konfigurasi dan job.
- `sql/002_generic_crud_diagnostics.sql` — query diagnostik, tanpa membuat indeks secara membabi buta.
- `tools/scan_general_value_objects.py` — scanner source yang dapat dijalankan ulang terhadap Git terbaru.
- `tools/generate_generic_crud_aliases.py` — generator alias JSP/service dan SQL seed yang seluruhnya tetap disabled.
- `generated_aliases_disabled/` — 3.086 alias JSP untuk 1.543 page binding dari 1.490 entity konkret pada snapshot; belum boleh diaktifkan tanpa engine/runtime review.
- `prototype/generic_crud_reference.html` — prototype UI responsif yang dapat dibuka langsung di browser.
- `HOW_TO_RUN_CODEX.md` — cara membagi pekerjaan ke sesi Codex/Claude Code.
- `manifests/*.csv|json` — hasil scan snapshot source yang tersedia saat paket dibuat.
- `examples/*.json` — contoh metadata untuk Agama dan override Mahasiswa.
- `tests/TEST_MATRIX.md` — skenario uji fungsional, privilege, performa, import/export, mobile, dan keamanan.
- `REFERENSI_BEST_PRACTICE.md` — sumber resmi dan keputusan desain.

## Cara memakai

```bash
git checkout master
git pull --ff-only origin master
git checkout -b feat/generic-crud-general-value-object

python tools/scan_general_value_objects.py \
  --project-root . \
  --output-dir build/generic-crud-manifest
```

Lalu berikan `PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT.md` sebagai instruksi utama kepada Codex/Claude Code. Agent wajib membaca source terbaru terlebih dahulu dan tidak boleh menyalin asumsi dari snapshot ini apabila source terbaru berbeda.

## Prinsip keselamatan utama

- Jangan mengekspos seluruh entitas secara otomatis hanya karena class extends `GeneralValueObject`.
- Jangan menerima nama property/kolom/sort dari request tanpa allow-list metadata.
- Jangan melakukan binding request langsung ke entity Hibernate.
- Jangan membuat lookup berdasarkan getter yang belum tentu mapped.
- Jangan menghapus massal langsung; wajib dry-run, preview, privilege per operasi, dan audit.
- Jangan menggabungkan privilege beberapa role; ikuti role aktif existing.
- Jangan meletakkan business logic Hibernate di JSP; JSP service hanya adapter tipis menuju Java service.
- Jangan memuat semua data ke `List` lalu paging di memori.



## Tambahan utama V2.1

Paket ini sekarang secara eksplisit mencakup:

- Audit Trail global dan per row/per ID;
- compare before/after;
- restore satu field, koreksi manual, restore revision/deep/deleted record;
- restore terbaru mulai tanggal dengan dry-run dan job progress;
- `Hapus Data Ini` khusus `Common.getApakahAdmin()` + DELETE + policy entity, dengan audit tetap dipertahankan;
- custom/override Add/Edit form untuk drawer, modal, tabbed, full-page, wizard, custom component, dan legacy bridge;
- contoh full-page tab form Mahasiswa sesuai pola Action existing.

Mulai tambahan V2 dari `CHANGELOG_V2_AUDIT_RESTORE_COMPLEX_FORM.md`.
