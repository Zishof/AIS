package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.GenericCrudDashboardProvider;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MemoryInfo;

/** Membuktikan dashboard dari tabel aktual; tidak menjalankan hapus semua. */
@SuppressWarnings("rawtypes")
public final class MemoryInfoGenericCrudDatabaseSelfTest {
    private MemoryInfoGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(MemoryInfo.class.getName(), "root", "memory_info");
            session = HibernateUtil.getSessionFactory().openSession();
            Number count = (Number) session.createCriteria(MemoryInfo.class).setProjection(Projections.rowCount()).uniqueResult();
            Map dashboard = ((GenericCrudDashboardProvider) d.getAdapter()).getDashboard(null);
            check(dashboard.get("kpis") instanceof List && dashboard.get("trend") instanceof List, "dashboard lengkap");
            System.out.println("PASS memory_info fields=" + d.getFields().size() + " rows=" + count.longValue()
                    + " kpis=" + ((List) dashboard.get("kpis")).size() + " trend=" + ((List) dashboard.get("trend")).size());
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
