package ais.action.master.generic.v2.test;

import java.util.Iterator;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.JenjangProgramStudiGenericCrudAdapter;
import ais.database.model.JenjangProgramStudi;

/** Verifikasi allow-list override native Jenjang Program Studi. */
@SuppressWarnings("rawtypes")
public final class JenjangProgramStudiGenericCrudDefinitionSelfTest {
    private JenjangProgramStudiGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) {
        GenericCrudDefinition found = null;
        Iterator definitions = GenericCrudDefinitionRegistry.listDefinitions().iterator();
        while (definitions.hasNext()) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.next();
            if (candidate.getEntityClass() == JenjangProgramStudi.class) found = candidate;
        }
        check(found != null, "Definisi Jenjang Program Studi belum terdaftar.");
        check("root".equals(found.getModuleKey()) && "jenjang_program_studi".equals(found.getPageKey()),
                "Binding route Jenjang Program Studi salah.");
        check(found.isCreateEnabled() && found.isUpdateEnabled() && found.isDeleteEnabled(),
                "CRUD Jenjang Program Studi belum lengkap.");
        check(found.getAdapter() instanceof JenjangProgramStudiGenericCrudAdapter,
                "Override adapter Jenjang Program Studi belum aktif.");
        check(found.getField("jurusan") != null && found.getField("jurusan").isRequired(),
                "Jurusan wajib belum dipertahankan dari Action existing.");
        check(found.getField("jenjang") != null, "Field Jenjang belum tersedia di New UI.");
        System.out.println("PASS Jenjang Program Studi native CRUD definition self-test");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
