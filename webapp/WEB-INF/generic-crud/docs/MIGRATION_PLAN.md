# Rencana Migrasi Master CRUD Generik AIS

## 1. Sasaran migrasi

Migrasi dilakukan tanpa big-bang. Action/ZUL/JSP lama tetap menjadi fallback sampai tiap entity lulus parity, privilege, scope, import/export, dan regression test. Generic CRUD harus dapat diaktifkan per entity menggunakan configuration/feature flag.

## 2. Status lifecycle entity

```text
DISCOVERED
  ↓
REVIEW_REQUIRED
  ↓
CONFIGURED_DISABLED
  ↓
READ_ONLY_PILOT
  ↓
FULL_CRUD_PILOT
  ↓
PARITY_VERIFIED
  ↓
DEFAULT_NEW_UI
  ↓
LEGACY_DEPRECATION_CANDIDATE
```

Rollback dapat mengembalikan entity dari `DEFAULT_NEW_UI` ke legacy route tanpa kehilangan metadata/configuration.

## 3. Inventaris dan klasifikasi

Jalankan scanner pada source terbaru. Hasil source scan hanya kandidat; lakukan verifikasi runtime Hibernate. Kelompokkan entity:

### Kelompok A — Reference sederhana

Ciri:

- scalar fields sedikit;
- relation minimal;
- tidak ada custom action kompleks;
- tidak ada foto;
- tidak ada transaksi/approval kritis.

Contoh kandidat: Agama dan master jenis/kategori kecil.

Strategi: metadata-first, full CRUD pilot.

### Kelompok B — Reference berelasi

Ciri:

- beberapa many-to-one;
- scope organisasi;
- lookup >20 mungkin terjadi;
- validasi domain ringan.

Strategi: generic engine + relation/scope adapter.

### Kelompok C — Existing Action kompleks

Ciri:

- mempunyai Action/ZUL lama;
- custom buttons/report/import;
- validasi dan helper domain.

Strategi: parity-first. Generic list/form hanya menggantikan UI, business helper existing direuse melalui adapter.

### Kelompok D — Photo/person entities

Mahasiswa, Siswa, Dosen, Guru, Pegawai, Tbmuser dan turunan terkait.

Strategi: setelah engine stabil; photo adapter, field masking, scope, custom action, dan concurrency wajib.

### Kelompok E — Approval/workflow

Strategi: read-only dahulu, kemudian approval adapter yang mengikuti state machine existing.

### Kelompok F — Transaction/integration/internal

Contoh umum: payment request/response, bank integration, audit/log, queue/job, file content, token/session, temporary tables.

Strategi: default disabled atau specialized read-only operational screen. Jangan otomatis full CRUD.

## 4. Wave implementasi

### Wave 0 — Baseline dan safety net

Deliverables:

- branch fitur;
- source/runtime inventory;
- config schema;
- feature flag;
- routing fallback;
- parity test template;
- performance baseline;
- backup/rollback runbook.

Exit criteria:

- build existing lulus sebelum perubahan;
- snapshot SQL/config tersedia;
- entity dapat dimatikan tanpa redeploy bila desain memungkinkan.

### Wave 1 — Agama sebagai golden pilot

Gunakan `AgamaAction.java` sebagai referensi fungsi, bukan sekadar tampilan. Catat:

- field/filter;
- validation;
- pagination;
- download/upload;
- privilege;
- renderer/form behavior;
- delete rules.

Implementasikan:

- list/detail;
- filter/sort/paging DB;
- add/edit/delete;
- XLSX export/import dry-run;
- column chooser;
- audit;
- responsive UI.

Exit criteria:

- hasil dan rule sama dengan legacy;
- no placeholder service;
- privilege matrix lulus;
- legacy route tetap tersedia.

### Wave 2 — 10–20 reference masters

Pilih beberapa entity dari modul berbeda untuk memvalidasi generalitas:

- root/reference;
- sekolah;
- akunting reference;
- inventory/asset reference;
- employ reference.

Hindari entity risiko tinggi pada wave ini.

Exit criteria:

- engine tidak membutuhkan copy-paste Java/JSP per entity;
- alias page cukup metadata/entityKey;
- relation combo/bandbox terbukti;
- user column preferences stabil.

### Wave 3 — Export/report/job

Implementasi lengkap XLSX/PDF/DOCX/PPTX dan async jobs. Uji dataset besar dan file cleanup.

### Wave 4 — Advanced relation dan scope

Aktifkan entity multi-tenant/multi-organisasi dengan scope adapter. Lakukan cross-tenant security tests.

### Wave 5 — Approval dan custom actions

Migrasikan entity dengan workflow/custom buttons secara selektif. Buat parity matrix tiap action.

### Wave 6 — Person/photo entities

Urutan:

1. entity person yang paling sederhana;
2. Guru/Dosen/Pegawai;
3. Siswa;
4. Mahasiswa;
5. Tbmuser.

Mahasiswa dan Tbmuser terakhir karena custom action dan dampak keamanan besar.

### Wave 7 — Operational/transaksi

Hanya setelah review domain. Banyak entity sebaiknya specialized screen, bukan generic CRUD.

## 5. Parity matrix template

Untuk setiap entity existing Action:

| Fungsi legacy | Lokasi source | Privilege | Scope | New UI action | Adapter/helper | Test | Status |
|---|---|---|---|---|---|---|---|
| List | `...Action.java` | READ | PT/School | list | QueryService | IT-001 | Pending |
| Add | ... | CREATE | ... | create | Adapter | IT-002 | Pending |
| Custom action | ... | ... | ... | custom key | Existing helper | IT-003 | Pending |

Tidak boleh menandai parity hanya berdasarkan kemiripan nama tombol.

## 6. Generator strategy

Generator boleh membuat:

- alias JSP UI;
- alias service;
- default disabled entity config seed;
- field candidate report;
- adapter skeleton;
- parity worksheet.

Generator tidak boleh:

- mengaktifkan entity otomatis;
- menebak tenant scope;
- menebak natural key;
- mengekspor method `on*` otomatis;
- menimpa adapter/custom JSP manual;
- menghapus file tanpa manifest/`--prune` eksplisit;
- menggunakan getter transient sebagai mapped column.

Gunakan marker generated dan output report. File override manual ditempatkan di lokasi terpisah atau dilindungi.

## 7. Routing dan coexistence

Rekomendasi route:

```text
/new?module={module}&page={entitySlug}
/new?module={module}&page={entitySlug}&legacy=1
```

Atau configuration menentukan target default. Deep-link generic tetap melewati READ/scope guard.

Menu:

- satu menu bisnis existing, bukan menu tambahan per class internal;
- target New UI hanya jika entity enabled dan parity verified;
- fallback legacy jika feature flag off;
- command palette memakai menu authorized yang sama.

## 8. Database migration strategy

- migration additive;
- tidak mengubah table bisnis pada fase foundation;
- config tables nullable/independent dari FK existing sampai ID/schema dipastikan;
- seed idempotent;
- index dibuat berdasarkan query config/job, bukan seluruh bisnis;
- rollback menonaktifkan feature flag dan mempertahankan audit/job data;
- cleanup job/file mempunyai retention policy.

## 9. Data and cache invalidation

Invalidasi metadata/menu/preference ketika:

- role/menu privilege berubah;
- active role berubah;
- entity/field config berubah;
- Hibernate deployment/mapping version berubah;
- saved view/column preference disimpan;
- tenant/scope context berubah.

Cache key wajib memasukkan entity, user/role bila relevan, tenant/scope version, dan config version. Jangan menggunakan global cache untuk relation scoped tanpa key scope.

## 10. Deployment sequence

1. Merge migration/config + disabled engine.
2. Deploy staging, run runtime metadata verifier.
3. Review diagnostics; no entity active.
4. Enable Agama for pilot roles.
5. Run privilege/security/performance tests.
6. Enable selected production cohort.
7. Monitor latency/error/audit/job/temp storage.
8. Expand cohort/entity.
9. Keep legacy link during observation period.
10. Deprecate legacy only after acceptance and rollback window.

## 11. Rollback

### Fast rollback

- set global/entity feature flag off;
- route back to legacy;
- stop accepting new import/export jobs;
- allow safe running jobs to complete or cancel;
- preserve config/audit/job data.

### Application rollback

- redeploy previous WAR;
- clear Tomcat JSP work/cache as required;
- migration tables remain additive;
- verify legacy menu/action.

### Import incident rollback

- use audit/batch ID;
- adapter-specific compensating action or restore revision;
- do not run blind SQL reversal;
- preserve evidence and error workbook.

## 12. Monitoring during rollout

Per entity and wave monitor:

- page/list latency p50/p95;
- count query duration;
- error/403/409 rate;
- session/connection count;
- export/import queue and failures;
- memory/temp storage;
- duplicate/validation errors;
- user adoption and return-to-legacy rate;
- privilege discrepancy reports.

## 13. Completion criteria per wave

- build and regression pass;
- source/runtime metadata validation pass;
- privilege/scope tests pass;
- performance target pass on representative data;
- UI desktop/mobile pass;
- documentation and rollback updated;
- no critical open issue;
- acceptance owner signs parity matrix;
- legacy remains available until defined sunset.



# Amandemen Migration Plan V2.1

## Wave 1A — Audit parity untuk Agama

- hubungkan audit global/per-row ke Envers/GenericRevisiHelper;
- compare, field restore, Ubah & Restore, full/deep restore;
- restore latest from date pada staging;
- admin delete tetap disabled sampai FK/preflight test selesai.

## Wave 1B — Super Admin delete pilot

Aktifkan hanya pada satu reference master yang aman setelah:

- `Common.getApakahAdmin()` dan DELETE test;
- typed confirmation/reason;
- FK preflight;
- audit retained;
- restore setelah delete berhasil;
- rollback test.

Jangan mengaktifkan massal dengan SQL update seluruh entity.

## Wave 5A — Complex form framework

- provider registry;
- tab/section metadata;
- generic fallback;
- conditional/lazy/save-before-enter;
- cross-tab validation;
- responsive/accessibility.

## Wave 6A — Mahasiswa complex-form parity

Migrasikan satu tab per tahap dengan feature flag. Pertahankan Action/ZUL lama sampai seluruh tab, field, custom action, validator, photo, relation, audit, dan effect bisnis lulus parity. Admin delete Mahasiswa tetap disabled kecuali ada domain policy yang secara eksplisit disetujui.
