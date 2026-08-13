package ais.action.master.generic.v2.test;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Workspace;

/** Audit hierarchy Workspace aktual tanpa melakukan mutasi data anggaran. */
@SuppressWarnings("rawtypes")
public final class WorkspaceGenericCrudDatabaseSelfTest {
    private WorkspaceGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null; int selfCycles = 0; int missingParents = 0;
        try {
            GenericCrudDefinition d = find(); session = HibernateUtil.getSessionFactory().openSession();
            Number total = (Number) session.createCriteria(Workspace.class).setProjection(Projections.rowCount()).uniqueResult();
            Number roots = (Number) session.createCriteria(Workspace.class).add(Restrictions.le("parentId", Long.valueOf(0)))
                    .setProjection(Projections.rowCount()).uniqueResult();
            List sample = session.createCriteria(Workspace.class).setMaxResults(1000).list();
            for (int i = 0; i < sample.size(); i++) {
                Workspace row = (Workspace) sample.get(i);
                if (row.getId() != null && row.getId().equals(row.getParentId())) selfCycles++;
                if (row.getParentId() != null && row.getParentId().longValue() > 0L
                        && session.get(Workspace.class, row.getParentId()) == null) missingParents++;
            }
            if (selfCycles > 0) throw new IllegalStateException("Workspace self-cycle ditemukan: " + selfCycles);
            System.out.println("PASS workspace fields=" + d.getFields().size() + " total=" + total
                    + " roots=" + roots + " sampleMissingParents=" + missingParents);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
    private static GenericCrudDefinition find() {
        List values = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < values.size(); i++) {
            GenericCrudDefinition d = (GenericCrudDefinition) values.get(i);
            if (d.getEntityClass() == Workspace.class) return d;
        }
        throw new IllegalStateException("Definition Workspace tidak ditemukan");
    }
}
