#!/usr/bin/env python3
"""Audit route New UI yang benar-benar terikat ke lifecycle Action existing."""
from __future__ import print_function

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
SRC = ROOT / "src"
NEW = ROOT / "webapp" / "WEB-INF" / "new"


def attr(text, name):
    match = re.search(r'setAttribute\("%s",\s*"([^"]+)' % re.escape(name), text)
    return match.group(1) if match else None


def main():
    pages = []
    missing_action = []
    generic = []
    legacy = []
    entity_init = []
    custom_crud = []
    non_crud = []
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
        has_save = bool(re.search(r'public\s+(?:final\s+)?boolean\s+onSave\s*\(\s*(?:org\.zkoss\.zk\.ui\.event\.)?Event\b', java))
        if re.search(r'extends\s+GenericCrudAction\s*<', java) and has_save:
            generic.append((path, source))
        elif "DataInitDefault" in java and has_save and re.search(r'\bMyWindow\s+addWindow\b', java):
            legacy.append((path, source))
        elif has_save and re.search(r'\bMyWindow\s+addWindow\b', java) and re.search(
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
    print("Action source missing    : %d" % len(missing_action))
    if missing_action:
        for page, source in missing_action[:20]:
            print("MISSING %s -> %s" % (page.relative_to(ROOT), source.relative_to(ROOT)))
    # Missing source is a generator defect; custom Actions are reported but remain fail-closed.
    return 1 if missing_action else 0


if __name__ == "__main__":
    sys.exit(main())
