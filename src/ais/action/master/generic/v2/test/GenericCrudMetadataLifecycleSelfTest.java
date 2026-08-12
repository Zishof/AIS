package ais.action.master.generic.v2.test;

import java.lang.reflect.Field;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.GenericCrudAutoEntityAdapter;
import ais.database.model.Agama;
import ais.database.model.Tbmuser;

/** Membuktikan fallback metadata eksplisit dan tetap fail-closed secara default. */
public final class GenericCrudMetadataLifecycleSelfTest {
    private GenericCrudMetadataLifecycleSelfTest() { }

    public static void main(String[] args) throws Exception {
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        set(context, "user", new Tbmuser()); set(context, "canRead", Boolean.TRUE);
        GenericCrudAutoEntityAdapter metadata = new GenericCrudAutoEntityAdapter(
                Agama.class, true, null, true);
        metadata.beforeSave(null, new Agama(), context);

        boolean rejected = false;
        try {
            new GenericCrudAutoEntityAdapter(Agama.class, true).beforeSave(null, new Agama(), context);
        } catch (GenericCrudException expected) {
            rejected = expected.getStatus() == 501;
        }
        check(rejected, "adapter tanpa Action/metadata lifecycle harus fail-closed");
        System.out.println("PASS Generic CRUD metadata lifecycle self-test");
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
