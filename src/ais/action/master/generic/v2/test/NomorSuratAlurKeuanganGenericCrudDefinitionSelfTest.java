package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.NomorSuratAlurKeuanganGenericCrudAdapter;
import ais.database.model.akunting.NomorSuratAlurKeuangan;

/** Kontrak parity editor inline NomorSuratAlurKeuanganAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NomorSuratAlurKeuanganGenericCrudDefinitionSelfTest {
    private NomorSuratAlurKeuanganGenericCrudDefinitionSelfTest() { }
    public static void main(String[] args) throws Exception {
        GenericCrudDefinition value = null; List all = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < all.size(); i++) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) all.get(i);
            if (candidate.getEntityClass() == NomorSuratAlurKeuangan.class) { value = candidate; break; }
        }
        check(value != null, "definition terdaftar");
        check(!value.isCreateEnabled() && value.isUpdateEnabled() && !value.isDeleteEnabled(),
                "hanya template nomor surat yang dapat diubah");
        check("kode".equals(value.getDefaultSortProperty()), "urut kode");
        check(value.getField("nomorSurat").isUpdateable(), "picker nomor surat aktif");
        check(!value.getField("kode").isUpdateable() && !value.getField("nama").isUpdateable(),
                "identitas alur immutable");
        NomorSuratAlurKeuangan row = new NomorSuratAlurKeuangan();
        row.setKode("001"); row.setNama(NomorSuratAlurKeuangan.UANG_MUKA); row.setKeterangan("Kas Advance");
        Map update = new LinkedHashMap(); update.put("kode", "999"); update.put("nama", "Diubah");
        update.put("keterangan", "Diubah");
        ((NomorSuratAlurKeuanganGenericCrudAdapter) value.getAdapter()).applyUpdateValues(row, update, null);
        check("001".equals(row.getKode()) && NomorSuratAlurKeuangan.UANG_MUKA.equals(row.getNama()),
                "identitas tidak berubah");
        System.out.println("PASS Nomor Surat Alur Keuangan Generic CRUD definition self-test");
        System.exit(0);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
