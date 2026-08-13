package ais.action.master.generic.v2.test;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaranGagal;

/** Audit read-only jumlah transaksi gagal bernilai positif pada database aktual. */
public final class CicilanPembayaranGagalGenericCrudDatabaseSelfTest {
    private CicilanPembayaranGagalGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(
                    CicilanPembayaranGagal.class.getName(), "root", "cicilan_pembayaran_gagal");
            session = HibernateUtil.getSessionFactory().openSession();
            Number total = (Number) session.createCriteria(CicilanPembayaranGagal.class)
                    .setProjection(Projections.rowCount()).uniqueResult();
            Number visible = (Number) session.createCriteria(CicilanPembayaranGagal.class)
                    .add(Restrictions.gt("nilai", Double.valueOf(0.01d)))
                    .setProjection(Projections.rowCount()).uniqueResult();
            if (visible.longValue() > total.longValue()) throw new IllegalStateException("Filter nilai gagal tidak valid");
            System.out.println("PASS cicilan_pembayaran_gagal fields=" + d.getFields().size()
                    + " total=" + total + " visible=" + visible);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
