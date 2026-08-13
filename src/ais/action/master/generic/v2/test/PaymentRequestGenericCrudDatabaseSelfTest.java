package ais.action.master.generic.v2.test;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.cimb.CimbRequest;
import ais.database.model.cimb.CimbRequestDetail;
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;

/** Audit tabel request/detail aktual dan metadata New UI. */
public final class PaymentRequestGenericCrudDatabaseSelfTest {
    private PaymentRequestGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition cimb = find(CimbRequest.class);
            GenericCrudDefinition ipaymu = find(IpaymuRequest.class);
            session = HibernateUtil.getSessionFactory().openSession();
            Number cimbRows = count(session, CimbRequest.class, false);
            Number cimbDetails = count(session, CimbRequestDetail.class, true);
            Number ipaymuRows = count(session, IpaymuRequest.class, false);
            Number ipaymuDetails = count(session, IpaymuRequestDetail.class, true);
            System.out.println("PASS payment_requests cimbFields=" + cimb.getFields().size()
                    + " cimbRows=" + cimbRows + " cimbOpenDetails=" + cimbDetails
                    + " ipaymuFields=" + ipaymu.getFields().size() + " ipaymuRows=" + ipaymuRows
                    + " ipaymuOpenDetails=" + ipaymuDetails);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
    private static Number count(Session session, Class type, boolean openDetail) {
        org.hibernate.Criteria criteria = session.createCriteria(type);
        if (openDetail) criteria.add(Restrictions.isNull("idCicilan"));
        return (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }
    private static GenericCrudDefinition find(Class type) {
        java.util.List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition value = (GenericCrudDefinition) definitions.get(i);
            if (value.getEntityClass() == type) return value;
        }
        throw new IllegalStateException("Definition tidak ditemukan: " + type.getName());
    }
}
