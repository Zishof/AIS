#!/usr/bin/env python3
from __future__ import print_function
import csv
import json
import os
import py_compile
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
errors = []
checks = {}

required = [
    "README.md",
    "CHANGELOG_V2_AUDIT_RESTORE_COMPLEX_FORM.md",
    "AUDIT_RESTORE_SUPERADMIN_DELETE_SPEC.md",
    "COMPLEX_FORM_OVERRIDE_SPEC.md",
    "PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT.md",
    "PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT_V2.md",
    "ARSITEKTUR_TEKNIS_GENERIC_CRUD.md",
    "UIUX_SPEC_GENERIC_CRUD.md",
    "MATRIX_REQUIREMENT_DAN_ACCEPTANCE.md",
    "SECURITY_PERFORMANCE_BEST_PRACTICE.md",
    "MIGRATION_PLAN.md",
    "HOW_TO_RUN_CODEX.md",
    "SOURCE_SCAN_SUMMARY.md",
    "IMPLEMENTATION_STATUS.md",
    "sql/001_generic_crud_configuration.sql",
    "sql/002_generic_crud_diagnostics.sql",
    "sql/003_generic_crud_audit_restore_form_override.sql",
    "examples/agama.json",
    "examples/mahasiswa_override.json",
    "tests/TEST_MATRIX.md",
    "prototype/generic_crud_reference.html",
    "prototype/generic_crud_audit_restore_complex_form.html",
    "tools/scan_general_value_objects.py",
    "tools/generate_generic_crud_aliases.py",
    "templates/java/GenericCrudAuditRevisionAdapter.java.template",
    "templates/java/GenericCrudRestorePolicy.java.template",
    "templates/java/GenericCrudPermanentDeletePolicy.java.template",
    "templates/java/GenericCrudFormOverrideProvider.java.template",
    "templates/java/GenericCrudFormDefinition.java.template",
]
for rel in required:
    if not os.path.isfile(os.path.join(ROOT, rel)):
        errors.append("Missing: " + rel)
checks["required_files"] = len(required)

json_files = [
    "examples/agama.json", "examples/mahasiswa_override.json",
    "manifests/general_value_object_inventory.json",
    "manifests/scan_report.json",
    "generated_aliases_disabled/manifests/alias_generation_report.json"
]
parsed = {}
for rel in json_files:
    try:
        with open(os.path.join(ROOT, rel), "r", encoding="utf-8") as fh:
            parsed[rel] = json.load(fh)
    except Exception as exc:
        errors.append("Invalid JSON %s: %s" % (rel, exc))

for rel in ["tools/scan_general_value_objects.py", "tools/generate_generic_crud_aliases.py", "tools/validate_package.py"]:
    try:
        py_compile.compile(os.path.join(ROOT, rel), doraise=True)
    except Exception as exc:
        errors.append("Python compile failed %s: %s" % (rel, exc))

inventory_path = os.path.join(ROOT, "manifests/general_value_object_inventory.csv")
with open(inventory_path, "r", encoding="utf-8-sig", newline="") as fh:
    rows = list(csv.DictReader(fh))
checks["inventory_rows"] = len(rows)
checks["concrete_entities"] = sum(1 for r in rows if r.get("entity") == "True" and r.get("abstract") == "False")

alias_root = os.path.join(ROOT, "generated_aliases_disabled", "webapp")
ui = 0; service = 0; bad_include = 0; bad_service_include = 0
for base, _, files in os.walk(alias_root):
    for fn in files:
        if not fn.endswith(".jsp"):
            continue
        path = os.path.join(base, fn)
        with open(path, "r", encoding="utf-8") as fh:
            text = fh.read()
        if os.sep + "uiux" + os.sep in path:
            ui += 1
            if "pageContext.include" not in text:
                bad_include += 1
        elif os.sep + "services" + os.sep in path:
            service += 1
            if ".forward(request, response)" not in text:
                bad_service_include += 1
        if "#{" in text:
            errors.append("Legacy-invalid template expression in " + path)
checks["generated_ui_aliases"] = ui
checks["generated_service_aliases"] = service
if bad_include:
    errors.append("UI aliases without pageContext.include: %d" % bad_include)
if bad_service_include:
    errors.append("Service aliases without forward: %d" % bad_service_include)

sql1 = open(os.path.join(ROOT, "sql/001_generic_crud_configuration.sql"), "r", encoding="utf-8").read()
for table in [
    "generic_crud_entity_config", "generic_crud_page_binding",
    "generic_crud_field_config", "generic_crud_user_view",
    "generic_crud_saved_view", "generic_crud_custom_action_config",
    "generic_crud_import_job", "generic_crud_export_job",
    "generic_crud_idempotency", "generic_crud_audit_event",
    "generic_crud_selection_token"
]:
    if "CREATE TABLE IF NOT EXISTS " + table not in sql1:
        errors.append("SQL table missing in 001: " + table)

sql3 = open(os.path.join(ROOT, "sql/003_generic_crud_audit_restore_form_override.sql"), "r", encoding="utf-8").read()
for table in [
    "generic_crud_form_definition", "generic_crud_form_tab_config",
    "generic_crud_form_section_config", "generic_crud_restore_job",
    "generic_crud_restore_job_item"
]:
    if "CREATE TABLE IF NOT EXISTS " + table not in sql3:
        errors.append("SQL table missing in 003: " + table)
for token in ["admin_delete_enabled", "form_override_provider_class", "audit_revision_adapter_class",
              "super_admin_operation", "field_restore_enabled", "mass_restore_enabled"]:
    if token not in sql3:
        errors.append("SQL V2 token missing: " + token)

prompt = open(os.path.join(ROOT, "PERINTAH_MASTER_CODEX_GENERIC_CRUD_GENERAL_VALUE_OBJECT.md"), "r", encoding="utf-8").read()
for token in [
    "GenericCrudAuditRevisionAdapter", "GenericCrudRestorePolicy",
    "GenericCrudPermanentDeletePolicy", "GenericCrudFormOverrideProvider",
    "Common.getApakahAdmin()", "Hapus Data Ini", "Riwayat ID Ini",
    "Ubah & Restore", "FULL_PAGE_TABS", "saveBeforeEnter"
]:
    if token not in prompt:
        errors.append("Prompt V2 token missing: " + token)

agama = parsed.get("examples/agama.json", {})
if "auditPolicy" not in agama or "formOverride" not in agama:
    errors.append("Agama example missing auditPolicy/formOverride")
mhs = parsed.get("examples/mahasiswa_override.json", {})
if "auditPolicy" not in mhs or "formOverride" not in mhs:
    errors.append("Mahasiswa example missing auditPolicy/formOverride")
else:
    if mhs.get("formOverride", {}).get("mode") != "FULL_PAGE_TABS":
        errors.append("Mahasiswa form override is not FULL_PAGE_TABS")
    if len(mhs.get("formOverride", {}).get("tabs", [])) < 9:
        errors.append("Mahasiswa complex form tabs incomplete")

report = {"success": not errors, "checks": checks, "errors": errors}
print(json.dumps(report, ensure_ascii=False, indent=2))
if errors:
    sys.exit(1)
