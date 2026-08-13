package ais.action.master.generic.v2.test;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGaji;

/** Audit read-only tabel format dan tree item gaji aktual. */
@SuppressWarnings("rawtypes")
public final class FormatItemGajiGenericCrudDatabaseSelfTest {
    private FormatItemGajiGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(
                    FormatItemGaji.class.getName(), "payroll", "format_item_gaji");
            session = HibernateUtil.getSessionFactory().openSession();
            List formats = session.createCriteria(FormatItemGaji.class).list();
            Number items = (Number) session.createCriteria(ItemGaji.class)
                    .setProjection(Projections.rowCount()).uniqueResult();
            for (int i = 0; i < formats.size(); i++) {
                FormatItemGaji row = (FormatItemGaji) formats.get(i);
                if (row.getNama() == null || row.getNama().trim().length() == 0)
                    throw new IllegalStateException("Nama format kosong id=" + row.getId());
                if (row.getAktif()) {
                    Number duplicate = (Number) session.createCriteria(FormatItemGaji.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                            .add(Restrictions.eq("nama", row.getNama())).setProjection(Projections.rowCount()).uniqueResult();
                    if (duplicate.longValue() > 1L)
                        throw new IllegalStateException("Nama format aktif duplikat: " + row.getNama());
                }
            }
            System.out.println("PASS format_item_gaji fields=" + d.getFields().size()
                    + " rows=" + formats.size() + " treeItems=" + items);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
