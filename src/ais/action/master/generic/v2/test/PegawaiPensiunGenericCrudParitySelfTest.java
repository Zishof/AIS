package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.PegawaiPensiunGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.PensiunRecordGenericCrudAdapter;
import ais.database.model.Pegawai;
import ais.database.model.employ.Pensiun;

@SuppressWarnings("rawtypes")
public final class PegawaiPensiunGenericCrudParitySelfTest {
    private PegawaiPensiunGenericCrudParitySelfTest() { }
    public static void main(String[] args) throws Exception {
        PegawaiPensiunGenericCrudAdapter adapter = new PegawaiPensiunGenericCrudAdapter();
        GenericCrudDefinition d = new GenericCrudDefinition(); d.setEntityClass(Pegawai.class);
        d.setCreateEnabled(true); d.setUpdateEnabled(true); d.setDeleteEnabled(true); d.setImportEnabled(true);
        adapter.configure(d);
        check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled() && !d.isImportEnabled(), "mutasi tabel ditutup");
        GenericCrudRequestContext c = new GenericCrudRequestContext(); set(c, "canRead", Boolean.TRUE); set(c, "canUpdate", Boolean.TRUE);
        List actions = ((GenericCrudCustomActionProvider) adapter).getActions(d, c);
        check(actions.size() == 1 && "reactivate_employee".equals(((Map) actions.get(0)).get("actionKey")), "aksi aktif kembali");
        PensiunRecordGenericCrudAdapter record = new PensiunRecordGenericCrudAdapter();
        GenericCrudDefinition p = new GenericCrudDefinition(); p.setEntityClass(Pensiun.class); p.setUpdateEnabled(true); record.configure(p);
        check(!p.isUpdateEnabled(), "record pengajuan read-only");
        check(GenericCrudReviewedAdapterFactory.create(Pegawai.class, false,
                ais.action.master.employ.PensiunAction.class, false) instanceof PegawaiPensiunGenericCrudAdapter,
                "route pensiun memakai adapter khusus");
        check(!(GenericCrudReviewedAdapterFactory.create(Pegawai.class, false,
                ais.action.master.PegawaiAction.class, false) instanceof PegawaiPensiunGenericCrudAdapter),
                "route pegawai umum tidak ikut terfilter pensiun");
        System.out.println("PegawaiPensiunGenericCrudParitySelfTest OK"); System.exit(0);
    }
    private static void set(Object target, String field, Object value) throws Exception { java.lang.reflect.Field f=target.getClass().getDeclaredField(field);f.setAccessible(true);f.set(target,value); }
    private static void check(boolean value, String message) { if(!value) throw new IllegalStateException(message); }
}
