#!/usr/bin/env python3
"""Safe New UI scaffold maintenance.

The operational shell is hand-maintained and Menu/RBAC-driven. This tool only
validates that invariant and may regenerate a developer-catalog manifest; it
never rewrites index.jsp, sidebar.jsp, or the command palette.
"""

from __future__ import print_function

import argparse
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NEW_UI = ROOT / "webapp" / "WEB-INF" / "new"
INDEX = NEW_UI / "index.jsp"
SIDEBAR = NEW_UI / "_shared" / "ui" / "sidebar.jsp"
HYBRID_MENU = NEW_UI / "_shared" / "menu"
HYBRID_SIDEBAR = HYBRID_MENU / "sidebar_hybrid.jsp"
COMMAND = NEW_UI / "_shared" / "ui" / "command_palette.jsp"
MANIFEST = NEW_UI / "_shared" / "config" / "developer-catalog-manifest.json"


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
    if args.write_developer_manifest:
        write_developer_manifest()
    print("PASS: operational navigation remains Menu/RBAC-driven")


if __name__ == "__main__":
    main()
