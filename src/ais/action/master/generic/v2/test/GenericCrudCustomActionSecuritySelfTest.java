package ais.action.master.generic.v2.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudCustomActionService;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.database.model.Tbmuser;

/** Membuktikan custom action tidak dapat melewati privilege server-side. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudCustomActionSecuritySelfTest {
    private GenericCrudCustomActionSecuritySelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition definition = new GenericCrudDefinition(); definition.setEntityClass(ais.database.model.MemoryInfo.class);
        GenericCrudRequestContext allowed = context(definition, true), denied = context(definition, false);
        RecordingProvider provider = new RecordingProvider(); GenericCrudCustomActionService service = new GenericCrudCustomActionService();
        GenericCrudResult result = service.execute(allowed, "clear_all", new ArrayList(), new LinkedHashMap(), provider);
        check(result.isSuccess() && provider.called, "provider dijalankan saat DELETE tersedia");
        try {
            service.execute(denied, "clear_all", new ArrayList(), new LinkedHashMap(), provider);
            throw new IllegalStateException("custom action lolos tanpa DELETE");
        } catch (GenericCrudException expected) { check(expected.getStatus() == 403, "ditolak dengan 403"); }
        System.out.println("PASS Generic CRUD custom action security self-test"); System.exit(0);
    }
    private static GenericCrudRequestContext context(GenericCrudDefinition d, boolean delete) throws Exception {
        GenericCrudRequestContext context = new GenericCrudRequestContext(); set(context, "definition", d);
        set(context, "user", new Tbmuser()); set(context, "canRead", Boolean.TRUE); set(context, "canDelete", Boolean.valueOf(delete)); return context;
    }
    private static void set(Object target, String name, Object value) throws Exception { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static final class RecordingProvider implements GenericCrudCustomActionProvider {
        private boolean called;
        public List getActions(GenericCrudDefinition d, GenericCrudRequestContext c) { List result = new ArrayList(); Map action = new LinkedHashMap(); action.put("actionKey", "clear_all"); action.put("requiredPrivilege", GenericCrudOperation.DELETE); action.put("selectionMode", "NONE"); action.put("enabled", Boolean.TRUE); result.add(action); return result; }
        public GenericCrudResult execute(String key, List ids, Map parameters, GenericCrudRequestContext context) { called = true; return GenericCrudResult.ok("ok", null); }
    }
}
