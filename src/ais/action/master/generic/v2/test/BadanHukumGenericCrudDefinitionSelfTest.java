package ais.action.master.generic.v2.test;

import java.util.List;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.BadanHukumGenericCrudAdapter;
import ais.database.model.BadanHukum;

/** Kontrak parity form singleton BadanHukumAction pada New UI. */
public final class BadanHukumGenericCrudDefinitionSelfTest {
    private BadanHukumGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) throws Exception {
        GenericCrudDefinition value = null;
        List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.get(i);
            if (candidate.getEntityClass() == BadanHukum.class) { value = candidate; break; }
        }
        check(value != null, "definition BadanHukum terdaftar");
        check(value.getEntityClass() == BadanHukum.class, "entity BadanHukum");
        check(value.getAdapter() instanceof BadanHukumGenericCrudAdapter, "adapter eksplisit");
        check(value.isCreateEnabled() && value.isUpdateEnabled(), "create/update aktif");
        check(!value.isDeleteEnabled(), "singleton tidak dapat dihapus");
        check(value.getField("kode") != null && value.getField("kode").isRequired(), "kode wajib");
        check(value.getFields().size() >= 17, "seluruh field form existing tersedia");
        System.out.println("PASS Badan Hukum Generic CRUD definition self-test");
        System.exit(0);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
