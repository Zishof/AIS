package ais.action.master.generic.v2.test;

import java.util.List;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.ProdukPesertaGenericCrudAdapter;
import ais.database.model.kursus.ProdukPeserta;

/** Kontrak parity laporan read-only ProdukPesertaAction. */
public final class ProdukPesertaGenericCrudDefinitionSelfTest {
    private ProdukPesertaGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition d = null; List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.get(i);
            if (candidate.getEntityClass() == ProdukPeserta.class) { d = candidate; break; }
        }
        check(d != null, "definition terdaftar");
        check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(), "laporan wajib read-only");
        check(d.getAdapter() instanceof ProdukPesertaGenericCrudAdapter, "adapter eksplisit terpasang");
        check(!d.isDefaultSortAscending() && "id".equals(d.getDefaultSortProperty()), "record terbaru tampil dahulu");
        check(d.getField("pesertaKursus") != null && d.getField("produkKursus") != null
                && d.getField("pesertaPunyaProdukKursus") != null, "relasi laporan lengkap");
        check(d.isExportXlsxEnabled() && d.isExportPdfEnabled(), "ekspor laporan tersedia");
        System.out.println("PASS Produk Peserta Generic CRUD definition self-test"); System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
