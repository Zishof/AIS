from pathlib import Path
import sys

web_inf = Path(__file__).resolve().parents[2]
svn_java = web_inf.parents[1] / "java"
git_java = web_inf.parent.parent / "src"
source_root = svn_java if svn_java.exists() else git_java
java_root = source_root / "ais" / "action" / "master" / "generic" / "v2"
shared = web_inf / "new" / "_shared" / "generic-crud"
model_root = source_root / "ais" / "database" / "model"
auto_factory = (java_root / "GenericCrudAutoDefinitionFactory.java").read_text(encoding="utf-8")

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
    "advanced filter UI": "data-gc-filter-panel" in (shared / "ui" / "crud_page.jsp").read_text(encoding="utf-8"),
    "column preference UI": "preference_save" in (shared / "assets" / "generic-crud.js").read_text(encoding="utf-8"),
    "import dry-run and confirm": all(token in (java_root / "GenericCrudImportService.java").read_text(encoding="utf-8") for token in ("PREVIEW_READY", "confirm(", "IMPORT_DUPLICATE_FILE")),
    "import requires CUD privileges": "isCanCreate() && context.isCanUpdate() && context.isCanDelete()" in (java_root / "GenericCrudImportService.java").read_text(encoding="utf-8"),
    "import job owner role entity bound": all(token in (java_root / "GenericCrudImportService.java").read_text(encoding="utf-8") for token in ("ownerUserKey", "ownerRoleKey", "entityKey", "IMPORT_JOB_OWNER_DENIED", "IMPORT_JOB_EXPIRED")),
    "document exports": all(token in (java_root / "GenericCrudDocumentExportService.java").read_text(encoding="utf-8") for token in ("writePdf", "writeDocx", "writePptx")),
    "saved views owner scoped": all(token in (java_root / "GenericCrudSavedViewService.java").read_text(encoding="utf-8") for token in ("owner_user_key", "owner_role_key", "entity_key")),
    "audit scope validation": "scope.validateObject" in (java_root / "GenericCrudAuditService.java").read_text(encoding="utf-8"),
    "restore and admin delete routes": all(token in (java_root / "GenericCrudHttpController.java").read_text(encoding="utf-8") for token in ("restore_field", "restore_revision", "admin_delete_preflight", "admin_delete_confirm")),
    "no request-selected class": "request.getParameter(\"class" not in (java_root / "GenericCrudHttpController.java").read_text(encoding="utf-8"),
    "model source inventory >= 1500": len(list(model_root.rglob("*.java"))) >= 1500,
    "auto definition uses Hibernate allow-list": all(token in auto_factory for token in ("getAllClassMetadata", "GeneralValueObject.class.isAssignableFrom", "isBlockedClass", "buildAdministrative")),
    "auto definition never Class.forName": "Class.forName" not in auto_factory,
    "auto CRUD Super Admin scoped": "AUTO_CRUD_SUPER_ADMIN_REQUIRED" in (java_root / "adapter" / "GenericCrudAutoEntityAdapter.java").read_text(encoding="utf-8"),
    "scaffold UI bridge": "tryAutoRegister" in (web_inf / "new" / "_shared" / "ui" / "page.jsp").read_text(encoding="utf-8"),
    "scaffold service bridge": "GenericCrudHttpController.handle" in (web_inf / "new" / "_shared" / "services" / "dispatcher.jsp").read_text(encoding="utf-8"),
    "admin model catalog": all((web_inf / "new" / "generic" / kind / name).exists() for kind, name in (("uiux", "model_catalog.jsp"), ("uiux", "model_crud.jsp"), ("services", "model_catalog_service.jsp"), ("services", "model_crud_service.jsp"))),
}

failed = [name for name, passed in checks.items() if not passed]
for name, passed in checks.items():
    print(("PASS" if passed else "FAIL") + " - " + name)
sys.exit(1 if failed else 0)
