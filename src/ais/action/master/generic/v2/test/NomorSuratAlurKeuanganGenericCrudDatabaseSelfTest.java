package ais.action.master.generic.v2.test;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.NomorSuratAlurKeuanganGenericCrudAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.NomorSuratAlurKeuangan;

/** Verifikasi seed dan cache alur keuangan pada database aktual. */
@SuppressWarnings("rawtypes")
public final class NomorSuratAlurKeuanganGenericCrudDatabaseSelfTest {
    private NomorSuratAlurKeuanganGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition definition = GenericCrudDefinitionRegistry.resolve(
                    NomorSuratAlurKeuangan.class.getName(), "akunting", "nomor_surat_alur_keuangan");
            session = HibernateUtil.getSessionFactory().openSession();
            ((NomorSuratAlurKeuanganGenericCrudAdapter) definition.getAdapter()).prepareRead(session, null);
            List rows = session.createCriteria(NomorSuratAlurKeuangan.class).addOrder(Order.asc("kode")).list();
            if (rows.size() < NomorSuratAlurKeuangan.S.length) throw new IllegalStateException("Alur default belum lengkap");
            if (NomorSuratAlurKeuangan.UANG_MUKA_DATA == null || NomorSuratAlurKeuangan.SI == null
                    || NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA == null) {
                throw new IllegalStateException("Cache alur keuangan belum dimuat");
            }
            System.out.println("PASS nomor_surat_alur_keuangan fields=" + definition.getFields().size()
                    + " rows=" + rows.size());
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
