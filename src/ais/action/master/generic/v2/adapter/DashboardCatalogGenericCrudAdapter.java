package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.database.model.Dashboard;

/** DashboardAction adalah launcher laporan SAPTO, bukan editor tabel dashboard. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class DashboardCatalogGenericCrudAdapter extends GenericCrudAutoEntityAdapter
        implements GenericCrudCustomActionProvider {
    public DashboardCatalogGenericCrudAdapter() { super(Dashboard.class, false, null, true); }
    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Katalog Dashboard dan Akreditasi");
        definition.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
        definition.setCreateEnabled(false); definition.setUpdateEnabled(false);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("nama"); definition.setDefaultSortAscending(true);
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false); field.setUpdateable(false);
        }
    }
    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", "open_sapto_catalog"); action.put("label", "Buka Katalog SAPTO");
        action.put("requiredPrivilege", GenericCrudOperation.READ); action.put("selectionMode", "NONE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanRead()));
        action.put("dangerous", Boolean.FALSE); action.put("parameterNames", new ArrayList());
        result.add(action); return result;
    }
    public GenericCrudResult execute(String key, List ids, Map parameters,
            GenericCrudRequestContext context) {
        if (!"open_sapto_catalog".equals(key))
            return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        String root = context == null || context.getRequest() == null ? "" : context.getRequest().getContextPath();
        Map data = new LinkedHashMap(); data.put("redirectUrl", root + "/new?module=sapto&page=index");
        return GenericCrudResult.ok("Katalog laporan SAPTO dibuka.", data);
    }
    public List getNaturalKeyProperties() { return new ArrayList(); }
}
