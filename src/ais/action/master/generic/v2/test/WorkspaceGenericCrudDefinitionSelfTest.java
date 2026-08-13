package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.action.master.generic.v2.adapter.WorkspaceGenericCrudAdapter;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.SatuanKerja;

/** Kontrak parity lifecycle hierarchy dan revisi Workspace/RAB. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class WorkspaceGenericCrudDefinitionSelfTest {
    private WorkspaceGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = find();
        check(d != null && d.isCreateEnabled() && d.isUpdateEnabled() && !d.isDeleteEnabled(),
                "Workspace create/update aktif dan delete generik ditolak");
        check(d.getAdapter() instanceof WorkspaceGenericCrudAdapter
                && d.getAdapter() instanceof GenericCrudCustomActionProvider, "adapter eksplisit dan aksi revisi terpasang");
        check(d.getField("tahunWorkspace") != null && d.getField("revisi") != null
                && d.getField("satuanKerja") != null && d.getField("hargaTotal") != null,
                "field inti Workspace tersedia");
        Map values = new LinkedHashMap(); List errors = new java.util.ArrayList();
        ((WorkspaceGenericCrudAdapter) d.getAdapter()).validateCreate(values, null, errors);
        check(errors.size() == 4, "empat field wajib tervalidasi");
        Workspace cyclic = new Workspace(); cyclic.setId(Long.valueOf(5)); cyclic.setParentId(Long.valueOf(5));
        cyclic.setNama("Siklus"); cyclic.setTahunWorkspace(Integer.valueOf(2026)); cyclic.setRevisi(Integer.valueOf(1));
        cyclic.setSatuanKerja(new SatuanKerja());
        try { ((WorkspaceGenericCrudAdapter) d.getAdapter()).beforeSave(null, cyclic, null);
            throw new IllegalStateException("siklus lolos");
        } catch (GenericCrudException expected) {
            check("WORKSPACE_HIERARCHY_CYCLE".equals(expected.getCode()), "siklus ditolak sebelum simpan");
        }
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        set(context, "canCreate", Boolean.TRUE); set(context, "canUpdate", Boolean.TRUE);
        List actions = ((GenericCrudCustomActionProvider) d.getAdapter()).getActions(d, context);
        check(actions.size() == 2 && "copy_tree".equals(((Map) actions.get(0)).get("actionKey"))
                && "next_revision".equals(((Map) actions.get(1)).get("actionKey")),
                "salin pohon dan revisi berikutnya tersedia");
        System.out.println("PASS Workspace Generic CRUD definition self-test"); System.exit(0);
    }
    private static GenericCrudDefinition find() {
        List values = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < values.size(); i++) {
            GenericCrudDefinition d = (GenericCrudDefinition) values.get(i);
            if (d.getEntityClass() == Workspace.class) return d;
        }
        return null;
    }
    private static void set(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
