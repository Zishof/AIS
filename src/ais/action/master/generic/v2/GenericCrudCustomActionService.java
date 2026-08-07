package ais.action.master.generic.v2;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;

@SuppressWarnings("rawtypes")
public class GenericCrudCustomActionService {
    public GenericCrudResult execute(GenericCrudRequestContext context, String key, List ids, Map parameters,
            GenericCrudCustomActionProvider provider) throws Exception {
        if (provider == null || key == null || key.length() == 0) throw new GenericCrudException(403, "CUSTOM_ACTION_DISABLED", "Custom action tidak terdaftar.");
        return provider.execute(key, ids, parameters, context);
    }
}
