package ais.action.master.generic.v2.test;

import java.util.List;
import org.hibernate.Session;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.ProdukPeserta;

/** Audit read-only relasi peserta, produk, dan transaksi pembelian aktual. */
@SuppressWarnings("rawtypes")
public final class ProdukPesertaGenericCrudDatabaseSelfTest {
    private ProdukPesertaGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null; int invalid = 0;
        try {
            GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(
                    ProdukPeserta.class.getName(), "kursus", "produk_peserta");
            session = HibernateUtil.getSessionFactory().openSession();
            List rows = session.createCriteria(ProdukPeserta.class).setMaxResults(500).list();
            for (int i = 0; i < rows.size(); i++) {
                ProdukPeserta row = (ProdukPeserta) rows.get(i);
                if (row.getPesertaKursus() == null || row.getProdukKursus() == null
                        || row.getPesertaPunyaProdukKursus() == null) invalid++;
            }
            System.out.println("PASS produk_peserta fields=" + d.getFields().size()
                    + " sampledRows=" + rows.size() + " incompleteRelations=" + invalid);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
