#!/usr/bin/env python3
"""
Pemindai source AIS untuk membangun inventaris semua class konkret yang merupakan
subclass langsung/tidak langsung ais.database.model.GeneralValueObject.

Tidak membutuhkan library eksternal. Parser ini bersifat source inventory, bukan
pengganti Hibernate runtime metadata. Implementasi produksi WAJIB memverifikasi
property melalui SessionFactory.getAllClassMetadata()/ClassMetadata sebelum
property dipakai untuk filter, sort, import, export, atau update.
"""
from __future__ import print_function

import argparse
import csv
import json
import os
import re
from collections import Counter, defaultdict

BASE_FQCN = "ais.database.model.GeneralValueObject"
PHOTO_CORE = set([
    "Mahasiswa", "Dosen", "Pegawai", "Tbmuser", "Siswa", "Guru",
    "BiodataCalonMahasiswa", "BiodataCalonSiswa"
])
RISK_WORDS = (
    "Log", "History", "Audit", "Temporary", "Temp", "Cache", "Request",
    "Response", "DetailBiaya", "Token", "Session", "Queue", "Job",
    "Report", "Rekap", "Statistik", "Dashboard", "View", "Snapshot",
    "Archive", "FileContent", "Blob", "Binary"
)
INTEGRATION_MODULES = set([
    "bni", "bri", "bsi", "cimb", "faspay", "finpay", "ipaymu",
    "jatelindo", "iso8583", "ojs", "epsbed", "feeder", "sister"
])


def read_text(path):
    with open(path, "rb") as fh:
        raw = fh.read()
    for enc in ("utf-8", "utf-8-sig", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            pass
    return raw.decode("utf-8", "ignore")


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    text = re.sub(r"//[^\n\r]*", " ", text)
    return text


def parse_source(path):
    text = read_text(path)
    clean = strip_comments(text)
    pkg_m = re.search(r"\bpackage\s+([\w.]+)\s*;", clean)
    package = pkg_m.group(1) if pkg_m else ""
    imports = {}
    for imp in re.findall(r"\bimport\s+([\w.$]+)\s*;", clean):
        if not imp.endswith(".*"):
            imports[imp.rsplit(".", 1)[-1]] = imp.replace("$", ".")

    class_m = re.search(
        r"\b(public\s+)?(abstract\s+)?class\s+(\w+)\s*"
        r"(?:extends\s+([\w.$<>?,\s]+?))?\s*"
        r"(?:implements\s+[^\{]+)?\{",
        clean,
    )
    if not class_m:
        return None
    name = class_m.group(3)
    extends_raw = (class_m.group(4) or "").strip()
    if extends_raw:
        extends_raw = re.sub(r"<.*", "", extends_raw).strip().split()[0]
    fqcn = (package + "." + name).strip(".")

    table_name = ""
    schema_name = ""
    table_m = re.search(r"@Table\s*\((.*?)\)", clean, flags=re.S)
    if table_m:
        args = table_m.group(1)
        nm = re.search(r"\bname\s*=\s*\"([^\"]+)\"", args)
        sm = re.search(r"\bschema\s*=\s*\"([^\"]+)\"", args)
        table_name = nm.group(1) if nm else ""
        schema_name = sm.group(1) if sm else ""

    public_methods = []
    for mm in re.finditer(
        r"\bpublic\s+(?!class\b|interface\b|enum\b)(?:static\s+)?"
        r"[\w.$<>\[\], ?]+\s+(\w+)\s*\(([^)]*)\)", clean
    ):
        method = mm.group(1)
        if method not in public_methods:
            public_methods.append(method)

    on_methods = [m for m in public_methods if m.startswith("on")]
    photo_hint = (
        name in PHOTO_CORE
        or "FileFoto" in clean
        or bool(re.search(r"\b(get|set)Foto\s*\(", clean))
        or bool(re.search(r"\bFoto[A-Z]\w*\b", clean))
    )

    return {
        "path": path,
        "package": package,
        "name": name,
        "fqcn": fqcn,
        "extends_raw": extends_raw,
        "imports": imports,
        "abstract": bool(class_m.group(2)),
        "entity": bool(re.search(r"@(?:javax\.persistence\.)?Entity\b", clean)),
        "mapped_superclass": bool(re.search(r"@(?:javax\.persistence\.)?MappedSuperclass\b", clean)),
        "table": table_name,
        "schema": schema_name,
        "photo_hint": photo_hint,
        "public_methods": public_methods,
        "on_methods": on_methods,
        "source": clean,
    }


def resolve_parent(info, classes, by_simple):
    ext = info.get("extends_raw") or ""
    if not ext:
        return ""
    ext = ext.replace("$", ".")
    if ext in classes:
        return ext
    if "." in ext:
        same = info["package"] + "." + ext
        if same in classes:
            return same
        return ext
    if ext in info["imports"]:
        return info["imports"][ext]
    same = info["package"] + "." + ext
    if same in classes:
        return same
    cands = by_simple.get(ext, [])
    if len(cands) == 1:
        return cands[0]
    # Pilih kandidat model AIS apabila nama sederhana ambigu.
    model = [x for x in cands if x.startswith("ais.database.model.")]
    return model[0] if model else (cands[0] if cands else ext)


def module_of(package):
    prefix = "ais.database.model"
    if package == prefix:
        return "root"
    if package.startswith(prefix + "."):
        return package[len(prefix) + 1:].split(".")[0]
    return "external"


def display_name(name):
    return re.sub(r"(?<!^)(?=[A-Z])", " ", name).strip()


def action_index(source_root):
    result = defaultdict(list)
    action_root = os.path.join(source_root, "ais", "action")
    if not os.path.isdir(action_root):
        return result
    for base, _, files in os.walk(action_root):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(base, fn)
            text = strip_comments(read_text(path))
            # Strongest mapping: GenericCrudAction<Entity>
            for entity in re.findall(r"extends\s+GenericCrudAction\s*<\s*(\w+)\s*>", text):
                result[entity].append(path)
            # Common action convention: FooAction -> Foo entity.
            if fn.endswith("Action.java"):
                result[fn[:-11]].append(path)
    return result


def action_custom_methods(path):
    text = strip_comments(read_text(path))
    methods = []
    standard = set([
        "onAdd", "onSave", "onDelete", "onEdit", "onSearchDefault",
        "onBantuan", "onPaging", "init", "doAfterCompose", "doBeforeCompose"
    ])
    for method in re.findall(r"\bpublic\s+[\w.$<>\[\], ?]+\s+(on\w+)\s*\(", text):
        if method not in standard and method not in methods:
            methods.append(method)
    return methods


def classify(info, has_action, custom_methods):
    if info["abstract"]:
        return "EXCLUDED_ABSTRACT", "Class abstract"
    if not info["entity"]:
        return "REVIEW_NOT_ENTITY", "Subclass tidak memiliki @Entity pada source"
    module = module_of(info["package"])
    name = info["name"]
    reasons = []
    if module in INTEGRATION_MODULES:
        reasons.append("modul integrasi eksternal")
    for word in RISK_WORDS:
        if word.lower() in name.lower():
            reasons.append("nama mengindikasikan data transaksi/log/internal: " + word)
            break
    if info["photo_hint"]:
        reasons.append("membutuhkan adapter foto")
    if custom_methods:
        reasons.append("mempunyai aksi khusus existing")
    if reasons:
        return "REVIEW_REQUIRED", "; ".join(reasons)
    if has_action:
        return "ELIGIBLE_PARITY_FIRST", "Sudah mempunyai Action existing; migrasi dengan parity test"
    return "ELIGIBLE_METADATA_FIRST", "Kandidat CRUD metadata; tetap default disabled sampai verifikasi Hibernate/menu/scope"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--project-root", required=True,
                    help="Root project yang berisi src/ais (atau langsung folder src)")
    ap.add_argument("--output-dir", required=True)
    args = ap.parse_args()

    project_root = os.path.abspath(args.project_root)
    source_root = project_root
    if os.path.isdir(os.path.join(project_root, "src", "ais")):
        source_root = os.path.join(project_root, "src")
    if not os.path.isdir(os.path.join(source_root, "ais")):
        raise SystemExit("Tidak menemukan src/ais di: " + project_root)

    classes = {}
    for base, _, files in os.walk(source_root):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            parsed = parse_source(os.path.join(base, fn))
            if parsed:
                classes[parsed["fqcn"]] = parsed

    by_simple = defaultdict(list)
    for fqcn, info in classes.items():
        by_simple[info["name"]].append(fqcn)
    for info in classes.values():
        info["extends"] = resolve_parent(info, classes, by_simple)

    memo = {}
    def is_subclass(fqcn, stack=None):
        if fqcn == BASE_FQCN:
            return True
        if fqcn in memo:
            return memo[fqcn]
        if stack is None:
            stack = set()
        if fqcn in stack:
            memo[fqcn] = False
            return False
        stack.add(fqcn)
        info = classes.get(fqcn)
        if not info or not info.get("extends"):
            memo[fqcn] = False
            return False
        value = is_subclass(info["extends"], stack)
        memo[fqcn] = value
        return value

    action_map = action_index(source_root)
    rows = []
    for fqcn, info in sorted(classes.items()):
        if fqcn == BASE_FQCN or not is_subclass(fqcn):
            continue
        action_paths = sorted(set(action_map.get(info["name"], [])))
        custom = []
        for path in action_paths:
            for method in action_custom_methods(path):
                if method not in custom:
                    custom.append(method)
        status, reason = classify(info, bool(action_paths), custom)
        rows.append({
            "entity_key": fqcn,
            "class_name": info["name"],
            "display_name": display_name(info["name"]),
            "package": info["package"],
            "module": module_of(info["package"]),
            "extends": info["extends"],
            "abstract": info["abstract"],
            "entity": info["entity"],
            "schema": info["schema"],
            "table": info["table"],
            "photo_adapter_required": info["photo_hint"],
            "existing_actions": "|".join(os.path.relpath(p, source_root) for p in action_paths),
            "custom_action_candidates": "|".join(custom),
            "eligibility": status,
            "eligibility_reason": reason,
            "source_path": os.path.relpath(info["path"], source_root),
        })

    out = os.path.abspath(args.output_dir)
    if not os.path.isdir(out):
        os.makedirs(out)

    fields = [
        "entity_key", "class_name", "display_name", "package", "module",
        "extends", "abstract", "entity", "schema", "table",
        "photo_adapter_required", "existing_actions", "custom_action_candidates",
        "eligibility", "eligibility_reason", "source_path"
    ]
    with open(os.path.join(out, "general_value_object_inventory.csv"), "w", newline="", encoding="utf-8") as fh:
        wr = csv.DictWriter(fh, fieldnames=fields)
        wr.writeheader(); wr.writerows(rows)

    with open(os.path.join(out, "general_value_object_inventory.json"), "w", encoding="utf-8") as fh:
        json.dump(rows, fh, ensure_ascii=False, indent=2)

    summary = Counter((r["module"], r["eligibility"]) for r in rows)
    with open(os.path.join(out, "module_summary.csv"), "w", newline="", encoding="utf-8") as fh:
        wr = csv.writer(fh)
        wr.writerow(["module", "eligibility", "count"])
        for (module, status), count in sorted(summary.items()):
            wr.writerow([module, status, count])

    photo = [r for r in rows if r["photo_adapter_required"]]
    with open(os.path.join(out, "photo_candidate_entities.csv"), "w", newline="", encoding="utf-8") as fh:
        wr = csv.DictWriter(fh, fieldnames=fields)
        wr.writeheader(); wr.writerows(photo)

    custom = [r for r in rows if r["custom_action_candidates"]]
    with open(os.path.join(out, "custom_action_candidates.csv"), "w", newline="", encoding="utf-8") as fh:
        wr = csv.DictWriter(fh, fieldnames=fields)
        wr.writeheader(); wr.writerows(custom)

    totals = Counter(r["eligibility"] for r in rows)
    report = {
        "base_class": BASE_FQCN,
        "total_subclasses": len(rows),
        "concrete_entities": sum(1 for r in rows if not r["abstract"] and r["entity"]),
        "abstract": sum(1 for r in rows if r["abstract"]),
        "photo_candidates": len(photo),
        "custom_action_candidates": len(custom),
        "eligibility": dict(totals),
        "warning": "Hasil source scanner harus diverifikasi ulang dengan Hibernate runtime metadata sebelum CRUD diaktifkan."
    }
    with open(os.path.join(out, "scan_report.json"), "w", encoding="utf-8") as fh:
        json.dump(report, fh, ensure_ascii=False, indent=2)
    print(json.dumps(report, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
