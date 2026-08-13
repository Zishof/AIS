package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.adapter.CimbRequestGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.IpaymuRequestGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.database.model.Mahasiswa;
import ais.database.model.OrangTua;
import ais.database.model.Tbmuser;
import ais.database.model.cimb.CimbRequest;
import ais.database.model.ipaymu.IpaymuRequest;

/** Kontrak parity read-only dan isolasi mahasiswa untuk request payment gateway. */
public final class PaymentRequestGenericCrudDefinitionSelfTest {
    private PaymentRequestGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition cimb = find(CimbRequest.class);
        GenericCrudDefinition ipaymu = find(IpaymuRequest.class);
        check(readOnly(cimb) && cimb.getAdapter() instanceof CimbRequestGenericCrudAdapter,
                "CIMB harus read-only eksplisit");
        check(readOnly(ipaymu) && ipaymu.getAdapter() instanceof IpaymuRequestGenericCrudAdapter,
                "iPaymu harus read-only eksplisit");
        check(cimb.getField("trxId") != null && cimb.getField("cimbResponse") != null,
                "kolom transaksi/respons CIMB tersedia");
        check(ipaymu.getField("buyer") != null && ipaymu.getField("ipaymuResponse") != null,
                "kolom pembeli/respons iPaymu tersedia");
        check(cimb.getAdapter() instanceof GenericCrudCustomActionProvider
                && ipaymu.getAdapter() instanceof GenericCrudCustomActionProvider,
                "aksi rincian nominal tersedia");

        Mahasiswa current = new Mahasiswa(); current.setId(Long.valueOf(1));
        Mahasiswa other = new Mahasiswa(); other.setId(Long.valueOf(2));
        final Mahasiswa activeStudent = current;
        Tbmuser user = new Tbmuser() {
            private static final long serialVersionUID = 1L;
            public Mahasiswa getMahasiswa() { return activeStudent; }
        };
        GenericCrudRequestContext context = new GenericCrudRequestContext(); set(context, "user", user);
        CimbRequest own = new CimbRequest(); own.setMahasiswa(current);
        ((CimbRequestGenericCrudAdapter) cimb.getAdapter()).validateObjectScope(own, context);
        IpaymuRequest denied = new IpaymuRequest(); denied.setMahasiswa(other);
        try {
            ((IpaymuRequestGenericCrudAdapter) ipaymu.getAdapter()).validateObjectScope(denied, context);
            throw new IllegalStateException("scope mahasiswa lain lolos");
        } catch (GenericCrudException expected) {
            check(expected.getStatus() == 403, "scope mahasiswa lain harus 403");
        }
        final java.util.List childIds = new java.util.ArrayList();
        childIds.add(Long.valueOf(1));
        Tbmuser parentUser = new Tbmuser() {
            private static final long serialVersionUID = 1L;
            public OrangTua getOrangTua() {
                return new OrangTua() {
                    private static final long serialVersionUID = 1L;
                    public java.util.List<Long> ambilAnakMahasiswa() { return childIds; }
                    public java.util.List<Long> ambilAnakSiswa() { return new java.util.ArrayList<Long>(); }
                };
            }
        };
        GenericCrudRequestContext parentContext = new GenericCrudRequestContext();
        set(parentContext, "user", parentUser);
        ((CimbRequestGenericCrudAdapter) cimb.getAdapter()).validateObjectScope(own, parentContext);
        try {
            ((IpaymuRequestGenericCrudAdapter) ipaymu.getAdapter()).validateObjectScope(denied, parentContext);
            throw new IllegalStateException("scope anak mahasiswa lain lolos untuk orang tua");
        } catch (GenericCrudException expected) {
            check(expected.getStatus() == 403, "scope anak mahasiswa lain untuk orang tua harus 403");
        }
        System.out.println("PASS payment request Generic CRUD definition self-test"); System.exit(0);
    }
    private static boolean readOnly(GenericCrudDefinition d) {
        return d != null && d.isEnabled() && !d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled();
    }
    private static GenericCrudDefinition find(Class type) {
        java.util.List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition value = (GenericCrudDefinition) definitions.get(i);
            if (value.getEntityClass() == type) return value;
        }
        return null;
    }
    private static void set(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
