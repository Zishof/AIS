package ais.action.master.generic.v2.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.adapter.DashboardCatalogGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.database.model.Dashboard;

public final class DashboardCatalogGenericCrudParitySelfTest {
    private DashboardCatalogGenericCrudParitySelfTest() { }
    public static void main(String[] args) {
        check(GenericCrudReviewedAdapterFactory.isReviewed(Dashboard.class), "review registry");
        DashboardCatalogGenericCrudAdapter adapter = new DashboardCatalogGenericCrudAdapter();
        GenericCrudDefinition definition = new GenericCrudDefinition(); add(definition, "nama"); add(definition, "clazz");
        adapter.configure(definition);
        check(GenericCrudDefinition.READ_ONLY.equals(definition.getLifecycleStatus()), "read only");
        check(!definition.isCreateEnabled() && !definition.isUpdateEnabled() && !definition.isDeleteEnabled(), "tanpa CRUD tabel");
        List actions = adapter.getActions(definition, null); Map action = (Map) actions.get(0);
        check("open_sapto_catalog".equals(action.get("actionKey")) && "NONE".equals(action.get("selectionMode")), "catalog action");
        GenericCrudResult result = adapter.execute("open_sapto_catalog", new ArrayList(), null, null);
        check(result.isSuccess() && "/new?module=sapto&page=index".equals(((Map) result.getData()).get("redirectUrl")), "native route");
        System.out.println("DashboardCatalogGenericCrudParitySelfTest OK");
    }
    private static void add(GenericCrudDefinition definition, String name) {
        GenericCrudFieldDefinition field = new GenericCrudFieldDefinition(name, name, String.class.getName());
        field.setCreateable(true); field.setUpdateable(true); definition.addField(field);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
