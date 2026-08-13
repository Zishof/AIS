package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.action.master.generic.v2.adapter.GenericCrudDashboardProvider;
import ais.database.model.MemoryInfo;

/** Kontrak parity MemoryInfoAction tanpa menjalankan aksi destruktif. */
@SuppressWarnings("rawtypes")
public final class MemoryInfoGenericCrudDefinitionSelfTest {
    private MemoryInfoGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = null; List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.get(i);
            if (candidate.getEntityClass() == MemoryInfo.class) { d = candidate; break; }
        }
        check(d != null, "definition MemoryInfo terdaftar");
        check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(), "record monitoring read-only");
        check(d.getAdapter() instanceof GenericCrudCustomActionProvider, "toolbar action provider tersedia");
        check(d.getAdapter() instanceof GenericCrudDashboardProvider, "dashboard provider tersedia");
        List actions = ((GenericCrudCustomActionProvider) d.getAdapter()).getActions(d, null);
        check(actions.size() == 1 && "clear_all".equals(((Map) actions.get(0)).get("actionKey")), "hapus semua terdaftar");
        check("DELETE".equals(((Map) actions.get(0)).get("requiredPrivilege")), "hapus semua wajib privilege DELETE");
        check(d.isExportXlsxEnabled() && d.isExportPdfEnabled(), "laporan dan ekspor tersedia");
        System.out.println("PASS Memory Info Generic CRUD definition self-test"); System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
