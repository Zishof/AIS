package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.ParameterUmumGenericCrudAdapter;
import ais.database.model.ParameterUmum;

/** Kontrak editor ParameterUmum tanpa kebocoran credential. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ParameterUmumGenericCrudDefinitionSelfTest {
    private ParameterUmumGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) throws Exception {
        GenericCrudDefinition value = null;
        List definitions = GenericCrudDefinitionRegistry.listDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.get(i);
            if (candidate.getEntityClass() == ParameterUmum.class) { value = candidate; break; }
        }
        check(value != null, "definition ParameterUmum terdaftar");
        check(!value.isCreateEnabled() && value.isUpdateEnabled() && !value.isDeleteEnabled(),
                "hanya parameter existing yang boleh diedit");
        check(value.getAdapter() instanceof ParameterUmumGenericCrudAdapter, "adapter eksplisit");
        ParameterUmumGenericCrudAdapter adapter = (ParameterUmumGenericCrudAdapter) value.getAdapter();
        ParameterUmum secret = new ParameterUmum("integrasi_api_token", "jangan-bocor");
        Map row = new LinkedHashMap(); row.put("nilai", secret.getNilai()); row.put("info1", "rahasia");
        adapter.sanitizeRow(secret, row, null);
        check(ParameterUmumGenericCrudAdapter.MASK.equals(row.get("nilai")), "nilai secret disamarkan");
        check(ParameterUmumGenericCrudAdapter.MASK.equals(row.get("info1")), "info secret disamarkan");
        Map update = new LinkedHashMap(); update.put("nama", "diubah");
        update.put("nilai", ParameterUmumGenericCrudAdapter.MASK); update.put("keterangan", "aman");
        adapter.applyUpdateValues(secret, update, null);
        check("integrasi_api_token".equals(secret.getNama()), "nama parameter immutable");
        check("jangan-bocor".equals(secret.getNilai()), "mask tidak menimpa secret");
        check("aman".equals(secret.getKeterangan()), "field non-secret tetap diperbarui");
        ParameterUmum publicValue = new ParameterUmum("tahun_aktif", "2026");
        Map publicRow = new LinkedHashMap(); publicRow.put("nilai", publicValue.getNilai());
        adapter.sanitizeRow(publicValue, publicRow, null);
        check("2026".equals(publicRow.get("nilai")), "nilai publik tidak disamarkan");
        System.out.println("PASS Parameter Umum Generic CRUD definition self-test");
        System.exit(0);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
