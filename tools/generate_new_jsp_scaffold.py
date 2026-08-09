#!/usr/bin/env python3
"""Safe New UI scaffold maintenance.

The operational shell is hand-maintained and Menu/RBAC-driven. This tool only
validates that invariant and may regenerate a developer-catalog manifest; it
never rewrites index.jsp, sidebar.jsp, or the command palette.
"""

from __future__ import print_function

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NEW_UI = ROOT / "webapp" / "WEB-INF" / "new"
INDEX = NEW_UI / "index.jsp"
SIDEBAR = NEW_UI / "_shared" / "ui" / "sidebar.jsp"
HYBRID_MENU = NEW_UI / "_shared" / "menu"
HYBRID_SIDEBAR = HYBRID_MENU / "sidebar_hybrid.jsp"
COMMAND = NEW_UI / "_shared" / "ui" / "command_palette.jsp"
MANIFEST = NEW_UI / "_shared" / "config" / "developer-catalog-manifest.json"
FORBIDDEN_NATIVE_UI_PATTERNS = (
    (re.compile(r"\.zul", re.IGNORECASE), "ZUL reference"),
    (re.compile(r"/WEB-INF/baru", re.IGNORECASE), "non-New-UI JSP reference"),
    (re.compile(r"/baru(?:\?|/)", re.IGNORECASE), "non-New-UI route"),
    (re.compile(r"legacyRoute|SAFE_LEGACY_BRIDGE|LEGACY_EMBED|LEGACY_REDIRECT|openLegacy", re.IGNORECASE), "legacy execution hook"),
)


def validate_dynamic_navigation():
    errors = []
    index_text = INDEX.read_text(encoding="utf-8")
    sidebar_text = SIDEBAR.read_text(encoding="utf-8")
    hybrid_sidebar_text = HYBRID_SIDEBAR.read_text(encoding="utf-8")
    command_text = COMMAND.read_text(encoding="utf-8")
    if "String[][] modules" in index_text:
        errors.append("index.jsp still contains a static module array")
    if "sidebar.jsp" not in index_text or "command_palette.jsp" not in index_text:
        errors.append("index.jsp does not include both dynamic navigation partials")
    required_partials = (
        "sidebar_hybrid.jsp", "sidebar_branch.jsp", "leaf_catalog.jsp", "leaf_card.jsp",
        "breadcrumb.jsp", "empty_catalog.jsp", "menu_diagnostics.jsp",
    )
    for partial in required_partials:
        if not (HYBRID_MENU / partial).is_file():
            errors.append("missing hybrid partial: " + partial)
    if "sidebar_hybrid.jsp" not in sidebar_text:
        errors.append("sidebar facade does not delegate to the hybrid renderer")
    if "newUiHybridMenuSnapshot" not in hybrid_sidebar_text or "newUiHybridMenuSnapshot" not in command_text:
        errors.append("navigation partials do not consume the shared authorized hybrid snapshot")
    if "NewUiMenuAccessService.getAccessibleTree" in sidebar_text + hybrid_sidebar_text + command_text:
        errors.append("renderer JSP must not query/build menu snapshots")
    if "String[][] modules" in sidebar_text + hybrid_sidebar_text + command_text:
        errors.append("operational navigation still contains a static module array")
    if errors:
        raise SystemExit("\n".join("ERROR: " + error for error in errors))


def validate_native_only_ui():
    errors = []
    for path in sorted(NEW_UI.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in (".jsp", ".js", ".css", ".json"):
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for pattern, label in FORBIDDEN_NATIVE_UI_PATTERNS:
            match = pattern.search(text)
            if match:
                line = text.count("\n", 0, match.start()) + 1
                errors.append("%s:%d contains %s" % (path.relative_to(ROOT), line, label))
    if errors:
        raise SystemExit("\n".join("ERROR: " + error for error in errors[:100]))


def write_developer_manifest():
    entries = []
    for catalog in sorted(NEW_UI.glob("**/catalog.json")):
        if "_shared" in catalog.parts:
            continue
        data = json.loads(catalog.read_text(encoding="utf-8"))
        entries.append({
            "module": data.get("module"),
            "label": data.get("label"),
            "count": data.get("count", 0),
            "catalog": catalog.relative_to(NEW_UI).as_posix(),
        })
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps({"developerCatalogs": entries}, indent=2) + "\n", encoding="utf-8")
    print("Wrote developer-only manifest: %s (%d modules)" % (MANIFEST, len(entries)))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--write-developer-manifest", action="store_true")
    args = parser.parse_args()
    validate_dynamic_navigation()
    validate_native_only_ui()
    if args.write_developer_manifest:
        write_developer_manifest()
    print("PASS: operational navigation remains Menu/RBAC-driven and native-New-UI-only")


if __name__ == "__main__":
    main()
