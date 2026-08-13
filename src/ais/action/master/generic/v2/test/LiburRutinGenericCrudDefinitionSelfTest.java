package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.GenericCrudQueryInitializer;
import ais.action.master.generic.v2.adapter.LiburRutinGenericCrudAdapter;
import ais.database.model.payroll.LiburRutin;

/** Kontrak parity LiburRutinAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class LiburRutinGenericCrudDefinitionSelfTest {
    private LiburRutinGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) throws Exception {
        GenericCrudDefinition value = null;
        List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.get(i);
            if (candidate.getEntityClass() == LiburRutin.class) { value = candidate; break; }
        }
        check(value != null, "definition LiburRutin terdaftar");
        check(!value.isCreateEnabled() && value.isUpdateEnabled() && !value.isDeleteEnabled(),
                "hanya toggle hari libur yang aktif");
        check(value.getAdapter() instanceof GenericCrudQueryInitializer, "seed lifecycle terpasang");
        check("hari".equals(value.getDefaultSortProperty()), "urutan hari dipertahankan");
        LiburRutin row = new LiburRutin(); row.setHari(Integer.valueOf(1)); row.setNama("Minggu");
        row.setKeterangan("Hari Minggu"); row.setLibur(Boolean.TRUE);
        Map update = new LinkedHashMap(); update.put("hari", "7"); update.put("nama", "Diubah");
        update.put("keterangan", "Diubah"); update.put("libur", "false");
        ((LiburRutinGenericCrudAdapter) value.getAdapter()).applyUpdateValues(row, update, null);
        check(Integer.valueOf(1).equals(row.getHari()) && "Minggu".equals(row.getNama()),
                "identitas hari immutable");
        check(!row.getLibur().booleanValue(), "status libur dapat diubah");
        System.out.println("PASS Libur Rutin Generic CRUD definition self-test");
        System.exit(0);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
