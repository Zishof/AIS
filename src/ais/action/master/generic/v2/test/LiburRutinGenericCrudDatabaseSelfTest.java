package ais.action.master.generic.v2.test;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.LiburRutinGenericCrudAdapter;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.LiburRutin;

/** Verifikasi seed dan urutan tujuh hari pada database aktual. */
@SuppressWarnings("rawtypes")
public final class LiburRutinGenericCrudDatabaseSelfTest {
    private LiburRutinGenericCrudDatabaseSelfTest() { }

    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition definition = GenericCrudDefinitionRegistry.resolve(
                    LiburRutin.class.getName(), "payroll", "libur_rutin");
            session = HibernateUtil.getSessionFactory().openSession();
            ((LiburRutinGenericCrudAdapter) definition.getAdapter()).prepareRead(session, null);
            List rows = session.createCriteria(LiburRutin.class).addOrder(Order.asc("hari")).list();
            if (rows.size() != Common.haris.length) {
                throw new IllegalStateException("Libur rutin harus tepat tujuh hari, aktual=" + rows.size());
            }
            for (int i = 0; i < rows.size(); i++) {
                LiburRutin row = (LiburRutin) rows.get(i);
                if (!Integer.valueOf(i + 1).equals(row.getHari())
                        || !Common.haris[i].equals(row.getNama())) {
                    throw new IllegalStateException("Urutan/nama hari tidak sesuai pada posisi " + (i + 1));
                }
            }
            System.out.println("PASS libur_rutin fields=" + definition.getFields().size()
                    + " rows=" + rows.size());
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
