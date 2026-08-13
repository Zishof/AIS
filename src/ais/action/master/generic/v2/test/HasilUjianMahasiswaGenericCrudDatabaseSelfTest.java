package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.HasilUjianMahasiswaGenericCrudAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.HasilUjianMahasiswa;

/** Verifikasi read-only konfigurasi kuota dan snapshot antrean aktual. */
@SuppressWarnings("rawtypes")
public final class HasilUjianMahasiswaGenericCrudDatabaseSelfTest {
    private HasilUjianMahasiswaGenericCrudDatabaseSelfTest() { }
    public static void main(String[] args) throws Exception {
        try {
            GenericCrudDefinition d = GenericCrudDefinitionRegistry.resolve(
                    HasilUjianMahasiswa.class.getName(), "root", "hasil_ujian_mahasiswa");
            HasilUjianMahasiswaGenericCrudAdapter adapter = (HasilUjianMahasiswaGenericCrudAdapter) d.getAdapter();
            Map dashboard = adapter.getDashboard(null); List kpis = (List) dashboard.get("kpis");
            if (kpis == null || kpis.size() != 3) throw new IllegalStateException("KPI kuota tidak lengkap");
            System.out.println("PASS hasil_ujian_mahasiswa fields=" + d.getFields().size()
                    + " queue=" + ais.action.master.helper.ProsesUjianHelper.kuotaUjian.size() + " kpis=" + kpis.size());
        } finally { try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { } }
        System.exit(0);
    }
}
