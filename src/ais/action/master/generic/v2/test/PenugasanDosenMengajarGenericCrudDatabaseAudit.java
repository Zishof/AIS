package ais.action.master.generic.v2.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.adapter.PenugasanDosenMengajarGenericCrudAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Tbmuser;

/** Audit database tanpa membuat dummy: custom generate memakai TA yang pasti kosong. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class PenugasanDosenMengajarGenericCrudDatabaseAudit {
    private PenugasanDosenMengajarGenericCrudDatabaseAudit() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition definition = GenericCrudDefinitionRegistry.tryAutoRegister("root",
                "penugasan_dosen_mengajar", new String[] { "PenugasanDosenMengajar", "Perkuliahan" },
                "ais.action.master", "PenugasanDosenMengajarAction",
                new String[] { "doAfterCompose", "onSearchDefault", "initCriteria" });
        check(definition != null && definition.getAdapter() instanceof PenugasanDosenMengajarGenericCrudAdapter,
                "binding adapter");
        check(!definition.isCreateEnabled() && definition.isUpdateEnabled() && !definition.isDeleteEnabled(),
                "capability Action");
        check(definition.getField("kode").isUpdateable() && definition.getField("keterangan").isUpdateable(),
                "field edit-inline");
        Session session = HibernateUtil.currentNativeSession(); Tbmuser demo; long before;
        try {
            demo = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.eq("userId", "demo"))
                    .setMaxResults(1).uniqueResult();
            before = ((Number) session.createCriteria(PenugasanDosenMengajar.class)
                    .setProjection(Projections.rowCount()).uniqueResult()).longValue();
        } finally { HibernateUtil.closeSession(); }
        check(demo != null, "user demo");
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        set(context, "definition", definition); set(context, "user", demo);
        set(context, "canRead", Boolean.TRUE); set(context, "canUpdate", Boolean.TRUE);
        Map parameters = new LinkedHashMap(); parameters.put("tahunAkademik", "0000/0000");
        parameters.put("semester", "Ganjil");
        GenericCrudResult result = ((PenugasanDosenMengajarGenericCrudAdapter) definition.getAdapter())
                .execute("generate_from_schedule", new ArrayList(), parameters, context);
        Session verify = HibernateUtil.currentNativeSession(); long after;
        try {
            after = ((Number) verify.createCriteria(PenugasanDosenMengajar.class)
                    .setProjection(Projections.rowCount()).uniqueResult()).longValue();
        } finally { HibernateUtil.closeSession(); }
        check(result.isSuccess() && before == after, "transaksi no-op tidak mengubah data");
        System.out.println("PenugasanDosenMengajarGenericCrudDatabaseAudit OK rows=" + after
                + " fields=" + definition.getFields().size());
        System.exit(0);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
