package ais.action.master.generic.v2.test;

import java.util.List;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.CicilanPembayaranGagalGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.common.Common;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/** Kontrak parity CicilanPembayaranGagalAction. */
public final class CicilanPembayaranGagalGenericCrudDefinitionSelfTest {
    private CicilanPembayaranGagalGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = null; List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) { GenericCrudDefinition c = (GenericCrudDefinition) definitions.get(i);
            if (c.getEntityClass() == CicilanPembayaranGagal.class) { d = c; break; } }
        check(d != null, "definition terdaftar"); check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(), "laporan read-only");
        check(d.getAdapter() instanceof CicilanPembayaranGagalGenericCrudAdapter, "adapter eksplisit terpasang");
        check(d.getAdapter() instanceof GenericCrudCustomActionProvider, "aksi Tidak Gagal terpasang");
        CicilanPembayaranGagal failed = new CicilanPembayaranGagal(); failed.setNilai(Double.valueOf(125000d));
        failed.setKe(Integer.valueOf(2)); failed.setKeterangan("uji mapping");
        CicilanPembayaran success = Common.copyCicilanPembayaranKeSukses(failed);
        check(success != null && success.getNilai().equals(failed.getNilai())
                && success.getKe().equals(failed.getKe()), "mapping existing gagal-ke-sukses dipertahankan");
        Tbmrole role = new Tbmrole(); role.setRoleId(Tbmrole.ADMINISTRATOR);
        Tbmuser user = new Tbmuser(); user.setUserId("generic-crud-admin-test"); user.setUserRole(role);
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        set(context, "definition", d); set(context, "user", user); set(context, "canRead", Boolean.TRUE);
        java.util.Map action = (java.util.Map) ((GenericCrudCustomActionProvider) d.getAdapter())
                .getActions(d, context).get(0);
        check(Boolean.TRUE.equals(action.get("enabled")) && "SINGLE".equals(action.get("selectionMode")),
                "aksi hanya aktif per baris untuk Administrator");
        System.out.println("PASS Cicilan Pembayaran Gagal Generic CRUD definition self-test"); System.exit(0);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
