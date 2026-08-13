package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudPage;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.action.master.generic.v2.adapter.GenericCrudDashboardProvider;
import ais.action.master.generic.v2.adapter.GenericCrudQueryProvider;
import ais.database.model.HasilUjianMahasiswa;

/** Kontrak parity antrean runtime HasilUjianMahasiswaAction. */
@SuppressWarnings("rawtypes")
public final class HasilUjianMahasiswaGenericCrudDefinitionSelfTest {
    private HasilUjianMahasiswaGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = null; List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) { GenericCrudDefinition c = (GenericCrudDefinition) definitions.get(i);
            if (c.getEntityClass() == HasilUjianMahasiswa.class) { d = c; break; } }
        check(d != null, "definition terdaftar"); check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(), "record DB read-only");
        check(d.getAdapter() instanceof GenericCrudQueryProvider, "antrean runtime menjadi source row");
        check(d.getAdapter() instanceof GenericCrudDashboardProvider, "KPI kuota tersedia");
        check(d.getAdapter() instanceof GenericCrudCustomActionProvider, "aksi kuota/reset/remove tersedia");
        check("id".equals(d.getIdentifierProperty()), "identifier Hibernate tetap tervalidasi");
        GenericCrudPage page = ((GenericCrudQueryProvider) d.getAdapter()).listRows(null, 1, 10, "", null, null);
        check(page != null && page.getTotal() == ais.action.master.helper.ProsesUjianHelper.kuotaUjian.size(),
                "snapshot antrean runtime konsisten");
        System.out.println("PASS Hasil Ujian Mahasiswa Generic CRUD definition self-test"); System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
