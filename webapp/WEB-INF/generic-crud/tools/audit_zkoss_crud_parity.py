#!/usr/bin/env python3
"""Audit kontrak ZUL/Action agar migrasi New UI tidak menghilangkan fungsi."""

from __future__ import print_function

import argparse
import hashlib
import json
import re
from functools import lru_cache
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
SOURCE_ROOT = ROOT / "src" if (ROOT / "src").exists() else ROOT / "java"
ZUL_ROOT = ROOT / "webapp" / "WEB-INF" / "z" / "x" / "y"
MAHASISWA_ZUL = ZUL_ROOT / "pages" / "master" / "mahasiswa.zul"
MAHASISWA_ACTION = SOURCE_ROOT / "ais" / "action" / "master" / "MahasiswaAction.java"
MAHASISWA_PROVIDER = (SOURCE_ROOT / "ais" / "action" / "master" / "generic" / "v2" /
                       "adapter" / "MahasiswaGenericCrudFormProvider.java")
MAHASISWA_PARITY = (SOURCE_ROOT / "ais" / "action" / "master" / "generic" / "v2" /
                     "adapter" / "MahasiswaActionParityContract.java")
MAHASISWA_SUBROUTES = (SOURCE_ROOT / "ais" / "common" / "newui" / "menu" /
                        "NewUiNativeSubrouteRegistry.java")


def text(path):
    return path.read_text(encoding="utf-8", errors="replace")


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def mask_java(source):
    """Mask comments/string literals while retaining line and brace positions."""
    out = []
    state = "code"
    escaped = False
    i = 0
    while i < len(source):
        char = source[i]
        nxt = source[i + 1] if i + 1 < len(source) else ""
        if state == "line":
            if char == "\n": state = "code"; out.append(char)
            else: out.append(" ")
        elif state == "block":
            if char == "*" and nxt == "/": out.extend((" ", " ")); i += 1; state = "code"
            else: out.append("\n" if char == "\n" else " ")
        elif state in ("string", "char"):
            if char == "\n": out.append(char)
            else: out.append(" ")
            if escaped: escaped = False
            elif char == "\\": escaped = True
            elif (state == "string" and char == '"') or (state == "char" and char == "'"): state = "code"
        elif char == "/" and nxt == "/":
            out.extend((" ", " ")); i += 1; state = "line"
        elif char == "/" and nxt == "*":
            out.extend((" ", " ")); i += 1; state = "block"
        elif char == '"': out.append(" "); state = "string"
        elif char == "'": out.append(" "); state = "char"
        else: out.append(char)
        i += 1
    return "".join(out)


@lru_cache(maxsize=None)
def top_level_public_methods(path):
    source = text(path)
    masked = mask_java(source)
    class_match = re.search(r"\bpublic\s+(?:abstract\s+)?class\s+\w+[^\{]*\{", masked)
    if not class_match:
        return []
    depth = 1
    offset = class_match.end()
    result = []
    declaration = re.compile(
        r"^\s*public\s+(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?"
        r"(?:<[^>]+>\s+)?[\w.$<>\[\], ?]+\s+(\w+)\s*\(")
    for number, line in enumerate(masked[offset:].splitlines(), start=masked[:offset].count("\n") + 1):
        if depth == 1:
            match = declaration.match(line)
            if match:
                result.append({"name": match.group(1), "line": number})
        depth += line.count("{") - line.count("}")
    unique = []
    seen = set()
    for item in result:
        if item["name"] not in seen:
            seen.add(item["name"]); unique.append(item)
    return unique


@lru_cache(maxsize=None)
def parent_action_path(path):
    source = text(path)
    match = re.search(r"\bpublic\s+(?:abstract\s+)?class\s+\w+[^\{]*?\bextends\s+([\w.]+)", source)
    if not match:
        return None
    parent = match.group(1)
    if "." in parent:
        candidate = SOURCE_ROOT.joinpath(*parent.split(".")).with_suffix(".java")
        return candidate if candidate.exists() else None
    imported = re.search(r"\bimport\s+([\w.]+\." + re.escape(parent) + r")\s*;", source)
    if imported:
        candidate = SOURCE_ROOT.joinpath(*imported.group(1).split(".")).with_suffix(".java")
        if candidate.exists(): return candidate
    package = re.search(r"\bpackage\s+([\w.]+)\s*;", source)
    if package:
        candidate = SOURCE_ROOT.joinpath(*package.group(1).split("."), parent + ".java")
        if candidate.exists(): return candidate
    candidates = JAVA_SOURCE_INDEX.get(parent, [])
    return candidates[0] if len(candidates) == 1 else None


@lru_cache(maxsize=None)
def hierarchy_public_methods(path):
    result = []
    visited = set()
    current = path
    while current and current not in visited:
        visited.add(current)
        result.extend(top_level_public_methods(current))
        current = parent_action_path(current)
    unique = []
    seen = set()
    for item in result:
        if item["name"] not in seen:
            seen.add(item["name"]); unique.append(item)
    return unique


def zul_contract(path):
    source = text(path)
    apply_match = re.search(r'\bapply\s*=\s*"([^"]+)"', source)
    handlers = []
    for number, line in enumerate(source.splitlines(), start=1):
        for match in re.finditer(r'\bforward\s*=\s*"on\w+\s*=\s*([A-Za-z_]\w*)"', line):
            handlers.append({"name": match.group(1), "line": number, "binding": "forward"})
        for match in re.finditer(r'\b(on[A-Z]\w*)\s*=\s*"([^"]+)"', line):
            expression = match.group(2)
            simple = re.match(r"\s*([A-Za-z_]\w*)\s*\(", expression)
            handlers.append({"name": simple.group(1) if simple else expression,
                             "line": number, "binding": match.group(1)})
    dedup = []
    seen = set()
    for item in handlers:
        marker = (item["name"], item["line"], item["binding"])
        if marker not in seen: seen.add(marker); dedup.append(item)
    return {"apply": apply_match.group(1) if apply_match else None, "handlers": dedup}


def action_path(class_name):
    if not class_name or not class_name.startswith("ais."):
        return None
    path = SOURCE_ROOT.joinpath(*class_name.split(".")).with_suffix(".java")
    return path if path.exists() else None


def all_contracts():
    contracts = []
    for zul in sorted(ZUL_ROOT.rglob("*.zul")):
        contract = zul_contract(zul)
        action = action_path(contract["apply"])
        if not contract["apply"]:
            continue
        methods = hierarchy_public_methods(action) if action else []
        method_names = {item["name"] for item in methods}
        missing = []
        seen_missing = set()
        for item in contract["handlers"]:
            if (re.match(r"^[A-Za-z_]\w*$", item["name"])
                    and item["name"] not in method_names and item["name"] not in seen_missing):
                seen_missing.add(item["name"]); missing.append(item)
        contracts.append({
            "zul": zul.relative_to(ROOT).as_posix(),
            "actionClass": contract["apply"],
            "actionSource": action.relative_to(ROOT).as_posix() if action else None,
            "handlers": contract["handlers"],
            "publicMethods": methods,
            "missingHandlers": missing,
        })
    return contracts


def mahasiswa_gate():
    contract = zul_contract(MAHASISWA_ZUL)
    methods = top_level_public_methods(MAHASISWA_ACTION)
    provider = text(MAHASISWA_PROVIDER)
    parity = text(MAHASISWA_PARITY)
    subroutes = text(MAHASISWA_SUBROUTES)
    referenced = sorted({item["name"] for item in contract["handlers"]
                         if re.match(r"^[A-Za-z_]\w*$", item["name"])})
    uncovered = [name for name in referenced if name not in provider]
    uncovered_public = [item["name"] for item in methods if item["name"] not in parity]
    native_panel_keys = set(re.findall(
        r'nativePanel\(actions,\s*context,\s*"([^"]+)"', provider))
    native_subroute_keys = set(re.findall(
        r'add\(routes,\s*"([^"]+)"', subroutes))
    pending = sorted(native_panel_keys - native_subroute_keys)
    return {
        "sourceZul": MAHASISWA_ZUL.relative_to(ROOT).as_posix(),
        "sourceAction": MAHASISWA_ACTION.relative_to(ROOT).as_posix(),
        "sourceZulSha256": sha256(MAHASISWA_ZUL),
        "sourceActionSha256": sha256(MAHASISWA_ACTION),
        "zulHandlers": referenced,
        "publicMethods": methods,
        "uncoveredZulHandlers": uncovered,
        "uncoveredPublicMethods": uncovered_public,
        "providerHasNativeOnlyPolicy": (SOURCE_LITERAL in provider
                                         and "NEW_UI_NATIVE" in provider
                                         and "legacyRoute" not in provider
                                         and ".zul" not in provider),
        "nativeSubrouteCount": len(native_panel_keys & native_subroute_keys),
        "pendingNativeActions": pending,
    }


SOURCE_LITERAL = "ais.action.master.MahasiswaAction"


JAVA_SOURCE_INDEX = {}
for java_source in SOURCE_ROOT.rglob("*.java"):
    JAVA_SOURCE_INDEX.setdefault(java_source.stem, []).append(java_source)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", action="store_true", help="Cetak laporan JSON lengkap")
    parser.add_argument("--all", action="store_true", help="Audit seluruh pasangan ZUL/Action")
    args = parser.parse_args()
    gate = mahasiswa_gate()
    report = {"mahasiswa": gate}
    failed = (bool(gate["uncoveredZulHandlers"]) or bool(gate["uncoveredPublicMethods"])
              or not gate["providerHasNativeOnlyPolicy"] or bool(gate["pendingNativeActions"]))
    if args.all:
        contracts = all_contracts()
        report["summary"] = {
            "zulWithAction": len(contracts),
            "actionSourceMissing": sum(1 for item in contracts if not item["actionSource"]),
            "zulHandlerMissingInAction": sum(len(item["missingHandlers"]) for item in contracts),
        }
        report["contracts"] = contracts
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print("PASS" if not failed else "FAIL", "- Mahasiswa ZUL handlers covered:",
              len(gate["zulHandlers"]) - len(gate["uncoveredZulHandlers"]), "/", len(gate["zulHandlers"]))
        print("PASS" if gate["providerHasNativeOnlyPolicy"] else "FAIL", "- native-only parity policy")
        print("PASS" if not gate["pendingNativeActions"] else "FAIL",
              "- executable native subroutes:", gate["nativeSubrouteCount"],
              "pending:", len(gate["pendingNativeActions"]))
        print("PASS" if not gate["uncoveredPublicMethods"] else "FAIL",
              "- MahasiswaAction public methods classified:",
              len(gate["publicMethods"]) - len(gate["uncoveredPublicMethods"]), "/", len(gate["publicMethods"]))
        if gate["uncoveredZulHandlers"]:
            print("Uncovered:", ", ".join(gate["uncoveredZulHandlers"]))
        if gate["uncoveredPublicMethods"]:
            print("Unclassified public methods:", ", ".join(gate["uncoveredPublicMethods"]))
        if gate["pendingNativeActions"]:
            print("Pending native actions:", ", ".join(gate["pendingNativeActions"]))
        if args.all:
            print("INFO - ZUL/Action contracts:", report["summary"]["zulWithAction"])
            print("INFO - action source missing:", report["summary"]["actionSourceMissing"])
            print("INFO - referenced handlers missing in action source:", report["summary"]["zulHandlerMissingInAction"])
    raise SystemExit(1 if failed else 0)


if __name__ == "__main__":
    main()
