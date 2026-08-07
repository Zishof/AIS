# Indeks Paket Generic CRUD `GeneralValueObject`

## Mulai dari sini

1. `IMPLEMENTATION_STATUS.md` — batas antara yang sudah dibuat dan yang masih harus diimplementasikan pada Git terbaru.
2. `PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT.md` — prompt utama untuk Codex/Claude Code.
3. `HOW_TO_RUN_CODEX.md` — urutan menjalankan agent, generator, migration, build, test, dan Git.
4. `ARSITEKTUR_TEKNIS_GENERIC_CRUD.md` — rancangan teknis engine.
5. `UIUX_SPEC_GENERIC_CRUD.md` — spesifikasi UI desktop/tablet/mobile.

## Perencanaan dan pengendalian

- `MATRIX_REQUIREMENT_DAN_ACCEPTANCE.md`
- `SECURITY_PERFORMANCE_BEST_PRACTICE.md`
- `MIGRATION_PLAN.md`
- `tests/TEST_MATRIX.md`
- `REFERENSI_BEST_PRACTICE.md`

## Inventaris source snapshot

- `SOURCE_SCAN_SUMMARY.md`
- `manifests/general_value_object_inventory.csv`
- `manifests/general_value_object_inventory.json`
- `manifests/module_summary.csv`
- `manifests/photo_candidate_entities.csv`
- `manifests/custom_action_candidates.csv`
- `manifests/scan_report.json`

## Tools

- `tools/scan_general_value_objects.py`
- `tools/generate_generic_crud_aliases.py`
- `tools/validate_package.py`

## Generated aliases — seluruhnya disabled

`generated_aliases_disabled/` berisi:

- 1.543 JSP UI alias;
- 1.543 JSP service alias;
- 1.543 page binding manifest;
- SQL seed 1.490 entity concrete yang tetap `REVIEW_REQUIRED`/disabled.

Alias hanya mendelegasikan ke shared Generic CRUD renderer/dispatcher yang harus diimplementasikan pada project terbaru.

## SQL

- `sql/001_generic_crud_configuration.sql`
- `sql/002_generic_crud_diagnostics.sql`

## Contoh

- `examples/agama.json` — golden pilot.
- `examples/mahasiswa_override.json` — contoh adapter/custom action kompleks, masih disabled.
- `templates/java/*.template`
- `templates/jsp/*.template`

## Prototype

- `prototype/generic_crud_reference.html` — buka langsung di browser untuk melihat referensi responsive CRUD.

## Integritas

- `VALIDATION_REPORT.md`
- `validation_result.json`
- `SHA256SUMS.txt`



## Tambahan V2.1

- `CHANGELOG_V2_AUDIT_RESTORE_COMPLEX_FORM.md`
- `AUDIT_RESTORE_SUPERADMIN_DELETE_SPEC.md`
- `COMPLEX_FORM_OVERRIDE_SPEC.md`
- `PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT_V2.md`
- `sql/003_generic_crud_audit_restore_form_override.sql`
- `prototype/generic_crud_audit_restore_complex_form.html`
- template Java audit/restore/delete/form provider.
