# Cara Menjalankan Paket melalui Codex / Claude Code

## 1. Persiapan repository

```bash
git clone https://github.com/Zishof/AIS.git
cd AIS
git checkout master
git pull --ff-only origin master
git checkout -b feat/generic-crud-general-value-object
```

Pastikan Java, Ant, PostgreSQL development/staging, dan konfigurasi build existing tersedia.

## 2. Salin perangkat bantu

Dari paket ini, salin:

```text
tools/scan_general_value_objects.py
tools/generate_generic_crud_aliases.py
sql/
examples/
tests/
```

ke folder kerja yang sesuai. Jangan langsung menyalin `generated_aliases_disabled` ke production sebelum shared Generic CRUD engine dan dispatcher tersedia.

## 3. Inventaris ulang source terbaru

```bash
python tools/scan_general_value_objects.py \
  --project-root . \
  --output-dir build/generic-crud-manifest
```

Bandingkan `build/generic-crud-manifest/scan_report.json` dengan manifest snapshot paket.

## 4. Berikan prompt utama ke agent

Gunakan seluruh isi:

```text
PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT.md
```

Tambahkan instruksi sesi:

```text
Kerjakan bertahap dan jangan mengubah semua entity sekaligus. Mulai dari
foundation dan pilot Agama. Tunjukkan git diff, build result, test result,
dan daftar file yang berubah setiap selesai satu tahap. Jangan menganggap
pekerjaan selesai ketika endpoint masih placeholder atau 501.
```

## 5. Gate sebelum agent menulis code

Agent harus menunjukkan:

- source audit;
- lokasi class/helper terbaru;
- count entity terbaru;
- privilege implementation terbaru;
- session/transaction convention;
- Agama parity matrix;
- file plan;
- migration plan.

Jangan izinkan agent meneruskan apabila hanya menebak property atau tabel.

## 6. Implementasi per sesi

Rekomendasi sesi terpisah:

1. `01-foundation-runtime-metadata-rbac`
2. `02-query-filter-sort-paging`
3. `03-responsive-ui-list-detail`
4. `04-form-mutation-validation-audit`
5. `05-relation-combo-bandbox`
6. `06-column-preference-saved-view`
7. `07-xlsx-import-export`
8. `08-pdf-docx-pptx-job`
9. `09-photo-adapters`
10. `10-custom-action-approval`
11. `11-alias-generator-routing-menu`
12. `12-agama-parity-hardening`
13. `13-reference-master-wave`
14. `14-person-entity-adapters`

Setiap sesi harus build/test dan commit sendiri.

## 7. Menjalankan alias generator

Setelah shared engine tersedia:

```bash
python tools/generate_generic_crud_aliases.py \
  --inventory build/generic-crud-manifest/general_value_object_inventory.csv \
  --output-root build/generated-generic-crud \
  --statuses ELIGIBLE_METADATA_FIRST,ELIGIBLE_PARITY_FIRST,REVIEW_REQUIRED
```

Review:

```text
build/generated-generic-crud/manifests/generic_crud_page_bindings.csv
build/generated-generic-crud/manifests/alias_collisions.json
build/generated-generic-crud/sql/seed_generic_crud_entities_disabled.sql
```

Semua seed tetap disabled. Pilih hanya file alias yang diperlukan pada wave aktif atau gunakan generator idempotent yang terintegrasi build/development.

## 8. Database staging

Backup dahulu. Terapkan migration pada database development/staging setelah review:

```bash
psql -v ON_ERROR_STOP=1 -d NAMA_DATABASE \
  -f sql/001_generic_crud_configuration.sql
```

Jalankan diagnostik:

```bash
psql -v ON_ERROR_STOP=1 -d NAMA_DATABASE \
  -f sql/002_generic_crud_diagnostics.sql
```

Jangan menjalankan migration production tanpa review identifier/schema existing dan rollback plan.

## 9. Build dan test

```bash
ant clean
ant
git diff --check
```

Jalankan test sesuai `tests/TEST_MATRIX.md`. Uji role/scope dengan user nyata staging, bukan hanya super admin.

## 10. Review hasil agent

Periksa khusus:

```text
Tidak ada query/property mentah dari request
Tidak ada mass binding entity
Tidak ada business logic Hibernate di JSP
Tidak ada paging List/subList
Tidak ada relation preload >20
Tidak ada custom on* reflection exposure
Tidak ada password/token di JSON/export/audit
Tidak ada entity auto-enabled
Tidak ada scope adapter default allow
Tidak ada endpoint migrated yang masih 501
```

## 11. Commit dan push

```bash
git status --short
git diff --check
git add <file-yang-direview>
git commit -m "feat(generic-crud): add metadata-driven CRUD foundation"
git push -u origin feat/generic-crud-general-value-object
```

Gunakan pull request; jangan merge ke master sebelum build, security matrix, dan pilot Agama lulus.



## 12. Instruksi tambahan untuk V2.1

Sebelum agent mengimplementasikan audit/form kompleks, wajib baca:

```text
AUDIT_RESTORE_SUPERADMIN_DELETE_SPEC.md
COMPLEX_FORM_OVERRIDE_SPEC.md
CHANGELOG_V2_AUDIT_RESTORE_COMPLEX_FORM.md
sql/003_generic_crud_audit_restore_form_override.sql
```

Minta agent membuat dua parity matrix tambahan:

```text
docs/generic-crud/parity-generic-revisi-helper.md
docs/generic-crud/parity-mahasiswa-complex-form.md
```

Urutan aman:

1. audit read-only global/per-row;
2. compare;
3. field restore;
4. full/deep restore;
5. restore job preview/commit;
6. Super Admin delete pilot;
7. form framework;
8. Mahasiswa form parity.

Jangan menerima hasil yang hanya menampilkan tombol toast atau endpoint 501.
