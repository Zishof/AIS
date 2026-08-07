package ais.action.master.generic.v2;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;

@SuppressWarnings("rawtypes")
public class GenericCrudCustomActionService {
    public GenericCrudResult execute(GenericCrudRequestContext context, String key, List ids, Map parameters,
            GenericCrudCustomActionProvider provider) throws Exception {
        if (provider == null || key == null || key.length() == 0) throw new GenericCrudException(403, "CUSTOM_ACTION_DISABLED", "Custom action tidak terdaftar.");
        List actions = provider.getActions(context.getDefinition(), context); Map selected = null;
        for (int i = 0; actions != null && i < actions.size(); i++) { if (actions.get(i) instanceof Map && key.equals(String.valueOf(((Map) actions.get(i)).get("actionKey")))) { selected = (Map) actions.get(i); break; } }
        if (selected == null || Boolean.FALSE.equals(selected.get("enabled"))) throw new GenericCrudException(403, "CUSTOM_ACTION_DISABLED", "Custom action tidak aktif pada allow-list.");
        String privilege = String.valueOf(selected.get("requiredPrivilege")); new GenericCrudPrivilegeGuard().require(context, privilege);
        String mode = String.valueOf(selected.get("selectionMode")); int count = ids == null ? 0 : ids.size();
        if ("NONE".equals(mode) && count != 0) throw new GenericCrudException(400, "SELECTION_NOT_ALLOWED", "Action tidak menerima selection.");
        if ("SINGLE".equals(mode) && count != 1) throw new GenericCrudException(400, "SINGLE_SELECTION_REQUIRED", "Pilih tepat satu data.");
        if ("MULTIPLE".equals(mode) && (count < 1 || count > 1000)) throw new GenericCrudException(400, "MULTIPLE_SELECTION_INVALID", "Pilih 1 sampai 1000 data.");
        Object allowedObject = selected.get("parameterNames");
        if (parameters != null && !parameters.isEmpty()) {
            if (!(allowedObject instanceof List)) throw new GenericCrudException(400, "ACTION_PARAMETERS_NOT_ALLOWED", "Action tidak menerima parameter.");
            java.util.Iterator keys = parameters.keySet().iterator(); while (keys.hasNext()) if (!((List) allowedObject).contains(String.valueOf(keys.next()))) throw new GenericCrudException(400, "ACTION_PARAMETER_INVALID", "Parameter action tidak terdaftar.");
        }
        return provider.execute(key, ids, parameters, context);
    }
}
