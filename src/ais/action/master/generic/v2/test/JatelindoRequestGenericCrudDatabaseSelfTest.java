package ais.action.master.generic.v2.test;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;

/** Audit read-only request/detail Jatelindo aktual. */
public final class JatelindoRequestGenericCrudDatabaseSelfTest {
    private JatelindoRequestGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Number rows = (Number) session.createCriteria(JatelindoRequest.class)
                    .setProjection(Projections.rowCount()).uniqueResult();
            Number openDetails = (Number) session.createCriteria(JatelindoRequestDetail.class)
                    .add(Restrictions.isNull("idCicilan")).setProjection(Projections.rowCount()).uniqueResult();
            System.out.println("PASS jatelindo_request rows=" + rows + " openDetails=" + openDetails);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
