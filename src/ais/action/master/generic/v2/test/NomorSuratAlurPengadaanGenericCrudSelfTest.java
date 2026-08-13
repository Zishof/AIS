package ais.action.master.generic.v2.test;

import java.util.List;
import org.hibernate.Session;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.NomorSuratAlurPengadaanGenericCrudAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.NomorSuratAlurPengadaan;

/** Kontrak struktur dan verifikasi database aktual untuk alur pengadaan. */
@SuppressWarnings("rawtypes")
public final class NomorSuratAlurPengadaanGenericCrudSelfTest {
    private NomorSuratAlurPengadaanGenericCrudSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(
                NomorSuratAlurPengadaan.class.getName(), "asset", "nomor_surat_alur_pengadaan");
        check(!d.isCreateEnabled() && d.isUpdateEnabled() && !d.isDeleteEnabled(), "lifecycle update-only");
        check(d.getField("nomorSurat").isUpdateable() && !d.getField("nama").isUpdateable(), "hanya picker editable");
        if (!Boolean.getBoolean("database")) { System.out.println("PASS Nomor Surat Alur Pengadaan definition self-test"); System.exit(0); }
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            ((NomorSuratAlurPengadaanGenericCrudAdapter) d.getAdapter()).prepareRead(session, null);
            List rows = session.createCriteria(NomorSuratAlurPengadaan.class).list();
            check(rows.size() >= NomorSuratAlurPengadaan.S.length, "16 alur default tersedia");
            check(NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA != null
                    && NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA != null, "cache alur terisi");
            System.out.println("PASS nomor_surat_alur_pengadaan fields=" + d.getFields().size() + " rows=" + rows.size());
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
