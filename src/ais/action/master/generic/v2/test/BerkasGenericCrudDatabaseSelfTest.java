package ais.action.master.generic.v2.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Berkas;

/** Audit read-only hierarchy Berkas aktual. */
@SuppressWarnings("rawtypes")
public final class BerkasGenericCrudDatabaseSelfTest {
    private BerkasGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null; int roots = 0, cycles = 0;
        try {
            GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(Berkas.class.getName(), "root", "berkas");
            session = HibernateUtil.getSessionFactory().openSession(); List rows = session.createCriteria(Berkas.class).list();
            for (int i = 0; i < rows.size(); i++) { Berkas row = (Berkas) rows.get(i); if (row.getParent() == null) roots++;
                Set ids = new HashSet(); Berkas cursor = row; int depth = 0;
                while (cursor != null && cursor.getId() != null) { if (!ids.add(cursor.getId()) || ++depth > 1000) { cycles++; break; } cursor = cursor.getParent(); }
            }
            if (cycles > 0) throw new IllegalStateException("Hierarchy Berkas existing bersiklus=" + cycles);
            System.out.println("PASS berkas fields=" + d.getFields().size() + " rows=" + rows.size() + " roots=" + roots + " cycles=" + cycles);
        } finally { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { } }
        System.exit(0);
    }
}
