package ais.action.master.generic.v2.test;

import java.util.Iterator;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.KeluargaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudApprovalAdapter;
import ais.database.model.employ.Keluarga;

/** Structural test tanpa bootstrap database. */
public final class KeluargaGenericCrudDefinitionSelfTest {
    private KeluargaGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) {
        GenericCrudDefinition found = null;
        for (Iterator values = GenericCrudDefinitionRegistry.listDefinitions().iterator(); values.hasNext();) {
            GenericCrudDefinition value = (GenericCrudDefinition) values.next();
            if (Keluarga.class.equals(value.getEntityClass())) found = value;
        }
        check(found != null, "Keluarga belum terdaftar");
        check("employ".equals(found.getModuleKey()) && "keluarga".equals(found.getPageKey()), "Route keluarga salah");
        check(found.isFullCrud() && found.isCreateEnabled() && found.isUpdateEnabled() && found.isDeleteEnabled(), "Lifecycle keluarga belum lengkap");
        check(found.getAdapter() instanceof KeluargaGenericCrudAdapter, "Adapter keluarga salah");
        check(found.getScopeAdapter() == found.getAdapter(), "Scope keluarga tidak terpasang");
        check(found.getAdapter() instanceof GenericCrudApprovalAdapter, "Approval keluarga tidak terpasang");
        check(found.getField("pegawai") != null && found.getField("hubungan") != null
                && found.getField("status") != null, "Field inti keluarga belum lengkap");
        check(!found.getField("status").isCreateable() && !found.getField("status").isUpdateable(),
                "Status approval tidak boleh dibypass melalui mutation biasa");
        System.out.println("PASS KeluargaGenericCrudDefinitionSelfTest");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
}
