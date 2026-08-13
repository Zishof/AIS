package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.BerkasGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.database.model.Berkas;

/** Kontrak parity hierarchy BerkasAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class BerkasGenericCrudDefinitionSelfTest {
    private BerkasGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = null; List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) { GenericCrudDefinition c = (GenericCrudDefinition) definitions.get(i);
            if (c.getEntityClass() == Berkas.class) { d = c; break; } }
        check(d != null && d.isCreateEnabled() && d.isUpdateEnabled() && d.isDeleteEnabled(), "CRUD penuh terdaftar");
        check(d.getAdapter() instanceof BerkasGenericCrudAdapter, "adapter eksplisit terpasang");
        check(d.getAdapter() instanceof GenericCrudCustomActionProvider, "aksi hierarchy terpasang");
        check("nama".equals(d.getDefaultSortProperty()) && d.getField("kode") == null,
                "sort memakai property persisten, bukan kode transient Action lama");
        Map values = new LinkedHashMap(); List errors = new java.util.ArrayList();
        ((BerkasGenericCrudAdapter) d.getAdapter()).validateCreate(values, null, errors);
        check(!errors.isEmpty(), "nama wajib divalidasi");
        Berkas self = new Berkas("Siklus"); self.setId(Long.valueOf(99)); self.setParent(self);
        try { ((BerkasGenericCrudAdapter) d.getAdapter()).beforeSave(null, self, null);
            throw new IllegalStateException("siklus hierarchy lolos");
        } catch (ais.action.master.generic.v2.GenericCrudException expected) {
            check("BERKAS_HIERARCHY_CYCLE".equals(expected.getCode()), "siklus ditolak");
        }
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        set(context, "canCreate", Boolean.TRUE); set(context, "canUpdate", Boolean.TRUE);
        List actions = ((GenericCrudCustomActionProvider) d.getAdapter()).getActions(d, context);
        check(actions.size() == 2 && "add_child".equals(((Map) actions.get(0)).get("actionKey"))
                && "copy_node".equals(((Map) actions.get(1)).get("actionKey")),
                "tambah child dan copy node tersedia per baris");
        System.out.println("PASS Berkas Generic CRUD definition self-test"); System.exit(0);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
