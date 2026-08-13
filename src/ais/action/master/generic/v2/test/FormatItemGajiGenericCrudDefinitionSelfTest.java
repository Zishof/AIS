package ais.action.master.generic.v2.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.FormatItemGajiGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.database.model.payroll.FormatItemGaji;

/** Kontrak parity CRUD FormatItemGajiAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class FormatItemGajiGenericCrudDefinitionSelfTest {
    private FormatItemGajiGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(
                FormatItemGaji.class.getName(), "payroll", "format_item_gaji");
        check(d.isCreateEnabled() && d.isUpdateEnabled() && d.isDeleteEnabled(), "CRUD penuh aktif");
        check(d.getAdapter() instanceof FormatItemGajiGenericCrudAdapter, "adapter eksplisit terpasang");
        check(d.getAdapter() instanceof GenericCrudCustomActionProvider, "copy action terpasang");
        check("nama".equals(d.getDefaultSortProperty()), "sort nama dipertahankan");
        GenericCrudFieldDefinition active = d.getField("aktif");
        check(active != null && active.isUpdateable(), "toggle aktif dapat diubah");
        List errors = new ArrayList();
        ((FormatItemGajiGenericCrudAdapter) d.getAdapter()).validateCreate(new java.util.LinkedHashMap(), null, errors);
        check(!errors.isEmpty(), "nama wajib divalidasi");
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        java.lang.reflect.Field canCreate = context.getClass().getDeclaredField("canCreate"); canCreate.setAccessible(true); canCreate.set(context, Boolean.TRUE);
        List actions = ((GenericCrudCustomActionProvider) d.getAdapter()).getActions(d, context);
        Map copy = (Map) actions.get(0);
        check("copy".equals(copy.get("actionKey")) && "SINGLE".equals(copy.get("selectionMode")), "copy per baris");
        check(((List) copy.get("parameterNames")).contains("newName"), "nama copy di-allow-list server");
        System.out.println("PASS Format Item Gaji Generic CRUD definition self-test"); System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
