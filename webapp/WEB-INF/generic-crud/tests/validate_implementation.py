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
controller = (java_root / "GenericCrudHttpController.java").read_text(encoding="utf-8")
query_service = (java_root / "GenericCrudQueryService.java").read_text(encoding="utf-8")
auto_adapter = (java_root / "adapter" / "GenericCrudAutoEntityAdapter.java").read_text(encoding="utf-8")
converter = (java_root / "GenericCrudValueConverter.java").read_text(encoding="utf-8")
shared_js = (shared / "assets" / "generic-crud.js").read_text(encoding="utf-8")
crud_page = (shared / "ui" / "crud_page.jsp").read_text(encoding="utf-8")
file_location_models = [
    model_root / "kkn" / "KelompokKkn.java",
    model_root / "pkl" / "KelompokPkl.java",
    model_root / "MatakuliahEkivalen.java",
    model_root / "PembagianKuotaPerkuliahanBerdasarkantahunAngkatan.java",
]
file_location_sources = [path.read_text(encoding="utf-8") for path in file_location_models]

checks = {
    "java files >= 35": len(list(java_root.rglob("*.java"))) >= 35,
    "runtime metadata verifier": (java_root / "GenericCrudRuntimeMetadataVerifier.java").exists(),
    "deny-by-default registry": "setEnabled(false)" in (java_root / "GenericCrudDefinitionRegistry.java").read_text(encoding="utf-8"),
    "pilot Agama": "Agama.class" in (java_root / "GenericCrudDefinitionRegistry.java").read_text(encoding="utf-8"),
    "CSRF mutation guard": "GenericCrudCsrf.requireMutation" in controller,
    "server paging": "setFirstResult" in query_service,
    "server count": "Projections.rowCount" in query_service,
    "admin delete disabled": "setAdminDeleteEnabled(false)" in (java_root / "GenericCrudDefinitionRegistry.java").read_text(encoding="utf-8"),
    "shared dispatcher": (shared / "services" / "dispatcher.jsp").exists(),
    "responsive UI": "@media(max-width:720px)" in (shared / "assets" / "generic-crud.css").read_text(encoding="utf-8"),
    "advanced filter UI": "data-gc-filter-panel" in crud_page,
    "static asset includes avoid JSP writer conflict": all(
        token in crud_page
        for token in (
            '<%@ include file="/WEB-INF/new/_shared/generic-crud/assets/generic-crud.css" %>',
            '<%@ include file="/WEB-INF/new/_shared/generic-crud/assets/generic-crud.js" %>',
        )
    ) and '<jsp:include page="/WEB-INF/new/_shared/generic-crud/assets/' not in crud_page,
    "column preference UI": "preference_save" in shared_js,
    "import dry-run and confirm": all(token in (java_root / "GenericCrudImportService.java").read_text(encoding="utf-8") for token in ("PREVIEW_READY", "confirm(", "IMPORT_DUPLICATE_FILE")),
    "import requires CUD privileges": "isCanCreate() && context.isCanUpdate() && context.isCanDelete()" in (java_root / "GenericCrudImportService.java").read_text(encoding="utf-8"),
    "import job owner role entity bound": all(token in (java_root / "GenericCrudImportService.java").read_text(encoding="utf-8") for token in ("ownerUserKey", "ownerRoleKey", "entityKey", "IMPORT_JOB_OWNER_DENIED", "IMPORT_JOB_EXPIRED")),
    "document exports": all(token in (java_root / "GenericCrudDocumentExportService.java").read_text(encoding="utf-8") for token in ("writePdf", "writeDocx", "writePptx")),
    "saved views owner scoped": all(token in (java_root / "GenericCrudSavedViewService.java").read_text(encoding="utf-8") for token in ("owner_user_key", "owner_role_key", "entity_key")),
    "audit scope validation": "scope.validateObject" in (java_root / "GenericCrudAuditService.java").read_text(encoding="utf-8"),
    "restore and admin delete routes": all(token in controller for token in ("restore_field", "restore_revision", "admin_delete_preflight", "admin_delete_confirm")),
    "no request-selected class": "request.getParameter(\"class" not in controller,
    "model source inventory >= 1500": len(list(model_root.rglob("*.java"))) >= 1500,
    "auto definition uses Hibernate allow-list": all(token in auto_factory for token in ("getAllClassMetadata", "GeneralValueObject.class.isAssignableFrom", "isBlockedClass", "buildAdministrative")),
    "auto definition never Class.forName": "Class.forName" not in auto_factory,
    "auto CRUD Super Admin scoped": "AUTO_CRUD_SUPER_ADMIN_REQUIRED" in auto_adapter,
    "dynamic Hibernate identifier UI": "meta.identifierProperty" in shared_js and "rowData.id" not in shared_js,
    "assigned identifier support": all(token in auto_factory + auto_adapter for token in ("isAssignedIdentifier", "setIdentifier")),
    "relation session adapter": (java_root / "adapter" / "GenericCrudSessionValueAdapter.java").exists() and "session.get(returned" in auto_adapter,
    "relation lookup allow-list": (java_root / "GenericCrudRelationLookupService.java").exists() and "relation_lookup" in controller,
    "relation UI lookup": all(token in shared_js for token in ("bindRelationLookup", "field.relationEntityKey", "field.property + '__label'")),
    "extended scalar conversion": all(token in converter for token in ("BigDecimal", "BigInteger", "UUID.fromString", "target.isEnum()", "java.sql.Timestamp")),
    "native temporal inputs": all(token in shared_js for token in ("'datetime-local'", "'date'", "'time'")),
    "sensitive models read only": "GenericCrudDefinition.READ_ONLY" in auto_factory and 'row.put("restricted"' in auto_factory,
    "file location excluded from generic forms": '"filelocation"' in auto_factory,
    "persistent file location getters are side-effect free": all(
        "public String getFileLocation() {\n\t\treturn fileLocation;\n\t}" in source
        and "public String getOrCreateFileLocation()" in source
        for source in file_location_sources
    ),
    "scaffold UI bridge": "tryAutoRegister" in (web_inf / "new" / "_shared" / "ui" / "page.jsp").read_text(encoding="utf-8"),
    "scaffold service bridge": "GenericCrudHttpController.handle" in (web_inf / "new" / "_shared" / "services" / "dispatcher.jsp").read_text(encoding="utf-8"),
    "admin model catalog": all((web_inf / "new" / "generic" / kind / name).exists() for kind, name in (("uiux", "model_catalog.jsp"), ("uiux", "model_crud.jsp"), ("services", "model_catalog_service.jsp"), ("services", "model_crud_service.jsp"))),
}

failed = [name for name, passed in checks.items() if not passed]
for name, passed in checks.items():
    print(("PASS" if passed else "FAIL") + " - " + name)
sys.exit(1 if failed else 0)
