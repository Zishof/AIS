package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.action.master.generic.v2.adapter.JatelindoRequestGenericCrudAdapter;
import ais.database.model.jatelindo.JatelindoRequest;

/** Kontrak parity laporan dan tombol cek pembayaran Jatelindo. */
@SuppressWarnings("rawtypes")
public final class JatelindoRequestGenericCrudSelfTest {
    private JatelindoRequestGenericCrudSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = find();
        check(d != null && d.isEnabled() && !d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(),
                "laporan Jatelindo read-only");
        check(d.getAdapter() instanceof JatelindoRequestGenericCrudAdapter, "adapter eksplisit terpasang");
        check(d.getField("trxId") != null && d.getField("kodeStatus") != null
                && d.getField("jatelindoResponse") != null, "field pencarian/status/respons tersedia");
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        set(context, "canRead", Boolean.TRUE); set(context, "canUpdate", Boolean.TRUE);
        List actions = ((GenericCrudCustomActionProvider) d.getAdapter()).getActions(d, context);
        check(actions.size() == 2 && "view_details".equals(((Map) actions.get(0)).get("actionKey"))
                && "check_payment".equals(((Map) actions.get(1)).get("actionKey")),
                "rincian dan cek pembayaran tersedia");
        System.out.println("PASS Jatelindo request Generic CRUD self-test"); System.exit(0);
    }
    private static GenericCrudDefinition find() {
        List values = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < values.size(); i++) {
            GenericCrudDefinition d = (GenericCrudDefinition) values.get(i);
            if (d.getEntityClass() == JatelindoRequest.class) return d;
        }
        return null;
    }
    private static void set(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
