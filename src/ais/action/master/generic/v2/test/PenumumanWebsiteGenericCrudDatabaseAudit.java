package ais.action.master.generic.v2.test;

import java.util.Map;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.GenericCrudInstitutionScope;
import ais.action.master.generic.v2.adapter.PenumumanWebsiteGenericCrudAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PenumumanWebsite;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;

/** Audit read-only terhadap metadata, route adapter, user demo, dan scope tenant. */
@SuppressWarnings("rawtypes")
public final class PenumumanWebsiteGenericCrudDatabaseAudit {
    private PenumumanWebsiteGenericCrudDatabaseAudit() { }

    public static void main(String[] args) {
        GenericCrudDefinition definition = GenericCrudDefinitionRegistry.tryAutoRegister("master",
                "penumuman_website", new String[] { "PenumumanWebsite" },
                "ais.action.master", "PenumumanWebsiteAction",
                new String[] { "onAdd", "onSave", "onSearchDefault" });
        check(definition != null && definition.getAdapter() instanceof PenumumanWebsiteGenericCrudAdapter,
                "binding adapter");
        check(definition.isCreateEnabled() && definition.isUpdateEnabled() && !definition.isDeleteEnabled(),
                "capability Action");
        check("tanggal".equals(definition.getDefaultSortProperty())
                && !definition.isDefaultSortAscending() && definition.getMaxPageSize() == 200,
                "konfigurasi daftar Action");
        Session session = HibernateUtil.currentNativeSession();
        try {
            Tbmuser demo = (Tbmuser) session.createCriteria(Tbmuser.class)
                    .add(org.hibernate.criterion.Restrictions.eq("userId", "demo"))
                    .setMaxResults(1).uniqueResult();
            check(demo != null, "user demo");
            Map bindings = GenericCrudInstitutionScope.bindings(PenumumanWebsite.class, demo);
            check(demo.getPerguruanTinggi() == null || bindings.containsKey("perguruanTinggi"),
                    "scope perguruan tinggi");
            PerguruanTinggi pt = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
                    .setMaxResults(1).uniqueResult();
            check(pt != null, "data perguruan tinggi");
            Tbmuser scopedUser = new Tbmuser();
            scopedUser.setPerguruanTinggi(pt);
            Map explicitScope = GenericCrudInstitutionScope.bindings(PenumumanWebsite.class, scopedUser);
            check(explicitScope.get("perguruanTinggi") == pt, "binding tenant eksplisit");
            Number rows = (Number) session.createCriteria(PenumumanWebsite.class)
                    .setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
            System.out.println("PenumumanWebsiteGenericCrudDatabaseAudit OK rows=" + rows
                    + " scope=" + explicitScope.keySet() + " fields=" + definition.getFields().size());
        } finally {
            HibernateUtil.closeSession();
        }
        System.exit(0);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
