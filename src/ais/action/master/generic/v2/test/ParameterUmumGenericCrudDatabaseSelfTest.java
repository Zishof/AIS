package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.ParameterUmumGenericCrudAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterUmum;

/** Verifikasi mapping/tabel aktual tanpa pernah mencetak nilai konfigurasi. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ParameterUmumGenericCrudDatabaseSelfTest {
    private ParameterUmumGenericCrudDatabaseSelfTest() { }

    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            GenericCrudDefinition definition = GenericCrudDefinitionRegistry.resolve(
                    ParameterUmum.class.getName(), "root", "parameter_umum");
            ParameterUmumGenericCrudAdapter adapter =
                    (ParameterUmumGenericCrudAdapter) definition.getAdapter();
            session = HibernateUtil.getSessionFactory().openSession();
            List rows = session.createCriteria(ParameterUmum.class).list();
            int protectedRows = 0;
            for (int i = 0; i < rows.size(); i++) {
                ParameterUmum parameter = (ParameterUmum) rows.get(i);
                Map output = new LinkedHashMap();
                output.put("nilai", parameter.getNilai());
                output.put("info1", parameter.getInfo1());
                adapter.sanitizeRow(parameter, output, null);
                if (adapter.isSensitiveProperty(parameter, "nilai", null)) {
                    protectedRows++;
                    if (!ParameterUmumGenericCrudAdapter.MASK.equals(output.get("nilai"))) {
                        throw new IllegalStateException("Nilai rahasia tidak disamarkan");
                    }
                }
            }
            System.out.println("PASS parameter_umum fields=" + definition.getFields().size()
                    + " rows=" + rows.size() + " protected=" + protectedRows);
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
