#!/usr/bin/env python3
"""Audit route New UI yang benar-benar terikat ke lifecycle Action existing."""
from __future__ import print_function

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
SRC = ROOT / "src"
NEW = ROOT / "webapp" / "WEB-INF" / "new"
JAVA_BY_NAME = {}
for _java_path in SRC.rglob("*.java"):
    JAVA_BY_NAME.setdefault(_java_path.stem, []).append(_java_path)


def attr(text, name):
    match = re.search(r'setAttribute\("%s",\s*"([^"]+)' % re.escape(name), text)
    return match.group(1) if match else None


def source_contract(path, text, class_name):
    """Gabungkan deklarasi kontrak superclass lokal (field window/interface)."""
    parts = [text]
    seen = {path.resolve()}
    current_path, current_text, current_class = path, text, class_name
    for _ in range(12):
        declaration = re.search(
            r'class\s+%s\b[^\{]*?\bextends\s+([\w.]+)' % re.escape(current_class), current_text)
        if not declaration:
            break
        parent_name = declaration.group(1).split(".")[-1]
        candidates = []
        same_package = current_path.with_name(parent_name + ".java")
        if same_package.is_file():
            candidates.append(same_package)
        imported = re.search(r'import\s+([\w.]+\.%s)\s*;' % re.escape(parent_name), current_text)
        if imported:
            imported_path = SRC.joinpath(*imported.group(1).split(".")).with_suffix(".java")
            if imported_path.is_file():
                candidates.append(imported_path)
        candidates.extend(JAVA_BY_NAME.get(parent_name, []))
        parent_path = next((p for p in candidates if p.resolve() not in seen), None)
        if parent_path is None:
            break
        seen.add(parent_path.resolve())
        current_path = parent_path
        current_text = parent_path.read_text(encoding="utf-8", errors="ignore")
        current_class = parent_name
        parts.append(current_text)
    return "\n".join(parts)


def main():
    pages = []
    missing_action = []
    generic = []
    legacy = []
    entity_init = []
    custom_crud = []
    non_crud = []
    unresolved_class = []
    for path in NEW.rglob("*.jsp"):
        if "uiux" not in path.parts or "services" in path.parts:
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        package = attr(text, "nuiSourcePackage")
        action = attr(text, "nuiSourceClass")
        if not package or not action:
            continue
        pages.append(path)
        source_path = attr(text, "nuiSourcePath")
        source = ROOT / source_path if source_path else SRC.joinpath(*package.split("."), action + ".java")
        if not source.is_file():
            missing_action.append((path, source))
            continue
        java = source.read_text(encoding="utf-8", errors="ignore")
        top_level = re.search(
            r'public\s+(?:(?:abstract|final)\s+)?class\s+%s\b' % re.escape(action), java)
        if not top_level:
            unresolved_class.append((path, source))
            continue
        contract = source_contract(source, java, action)
        has_save = bool(re.search(r'public\s+(?:final\s+)?boolean\s+onSave\s*\(\s*(?:org\.zkoss\.zk\.ui\.event\.)?Event\b', java))
        window_field = bool(re.search(
            r'(?:private|protected|public)\s+(?:final\s+)?(?:MyWindow|Window)\s+\w+\s*(?:[;=,])', contract))
        component_host = bool(re.search(
            r'(?:private|protected|public)\s+(?:final\s+)?Component\s+\w*(?:window|dialog)\w*\s*(?:[;=,])',
            contract, re.IGNORECASE))
        lifecycle_host = window_field or component_host
        if re.search(r'extends\s+GenericCrudAction\s*<', java) and has_save:
            generic.append((path, source))
        elif "DataInitDefault" in contract and has_save and lifecycle_host:
            legacy.append((path, source))
        elif has_save and lifecycle_host and re.search(
                r'(?:public|protected|private)\s+void\s+init\s*\(\s*(?:final\s+)?[A-Z][\w.<>?]*\s+\w+', java):
            entity_init.append((path, source))
        elif has_save:
            custom_crud.append((path, source))
        else:
            non_crud.append((path, source))

    print("New UI source pages       : %d" % len(pages))
    print("GenericCrudAction bound  : %d" % len(generic))
    print("DataInitDefault bound    : %d" % len(legacy))
    print("Entity init(Action) bound: %d" % len(entity_init))
    print("Native lifecycle bound   : %d" % (len(generic) + len(legacy) + len(entity_init)))
    print("Custom CRUD review needed: %d" % len(custom_crud))
    print("Non-CRUD source pages    : %d" % len(non_crud))
    print("Unresolvable source class: %d" % len(unresolved_class))
    print("Action source missing    : %d" % len(missing_action))
    if missing_action:
        for page, source in missing_action[:20]:
            print("MISSING %s -> %s" % (page.relative_to(ROOT), source.relative_to(ROOT)))
    if "--details" in sys.argv:
        for page, source in custom_crud:
            print("CUSTOM %s -> %s" % (page.relative_to(ROOT), source.relative_to(ROOT)))
        for page, source in unresolved_class:
            print("UNRESOLVED %s -> %s" % (page.relative_to(ROOT), source.relative_to(ROOT)))
    # Missing source is a generator defect; custom Actions are reported but remain fail-closed.
    return 1 if missing_action else 0


if __name__ == "__main__":
    sys.exit(main())
