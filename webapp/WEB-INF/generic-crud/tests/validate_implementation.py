from pathlib import Path
import sys

web_inf = Path(__file__).resolve().parents[2]
svn_java = web_inf.parents[1] / "java"
git_java = web_inf.parent.parent / "src"
source_root = svn_java if svn_java.exists() else git_java
java_root = source_root / "ais" / "action" / "master" / "generic" / "v2"
shared = web_inf / "new" / "_shared" / "generic-crud"

checks = {
    "java files >= 35": len(list(java_root.rglob("*.java"))) >= 35,
    "runtime metadata verifier": (java_root / "GenericCrudRuntimeMetadataVerifier.java").exists(),
    "deny-by-default registry": "setEnabled(false)" in (java_root / "GenericCrudDefinitionRegistry.java").read_text(encoding="utf-8"),
    "pilot Agama": "Agama.class" in (java_root / "GenericCrudDefinitionRegistry.java").read_text(encoding="utf-8"),
    "CSRF mutation guard": "GenericCrudCsrf.requireMutation" in (java_root / "GenericCrudHttpController.java").read_text(encoding="utf-8"),
    "server paging": "setFirstResult" in (java_root / "GenericCrudQueryService.java").read_text(encoding="utf-8"),
    "server count": "Projections.rowCount" in (java_root / "GenericCrudQueryService.java").read_text(encoding="utf-8"),
    "admin delete disabled": "setAdminDeleteEnabled(false)" in (java_root / "GenericCrudDefinitionRegistry.java").read_text(encoding="utf-8"),
    "shared dispatcher": (shared / "services" / "dispatcher.jsp").exists(),
    "responsive UI": "@media(max-width:720px)" in (shared / "assets" / "generic-crud.css").read_text(encoding="utf-8"),
}

failed = [name for name, passed in checks.items() if not passed]
for name, passed in checks.items():
    print(("PASS" if passed else "FAIL") + " - " + name)
sys.exit(1 if failed else 0)
