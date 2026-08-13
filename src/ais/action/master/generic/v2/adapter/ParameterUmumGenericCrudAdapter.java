package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Criteria;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterUmum;

/** Native New UI untuk editor konfigurasi ParameterUmumAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ParameterUmumGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<ParameterUmum>
        implements GenericCrudScopeAdapter, GenericCrudRowSanitizer {
    public static final String MASK = "********";
    private static final String[] VALUE_PROPERTIES = {
        "nilai", "info1", "info2", "info3", "info4", "info5"
    };
    private static final String[] SECRET_MARKERS = {
        "password", "passwd", "secret", "token", "api_key", "apikey",
        "private_key", "client_secret", "credential", "key_password",
        "kata_sandi", "sandi", "access_key", "auth_key"
    };

    public ParameterUmum createNew(GenericCrudRequestContext context) {
        return new ParameterUmum();
    }

    public void applyUpdateValues(ParameterUmum target, Map values,
            GenericCrudRequestContext context) throws Exception {
        Map safe = new java.util.LinkedHashMap(values);
        safe.remove("nama");
        if (isSecret(target)) {
            for (int i = 0; i < VALUE_PROPERTIES.length; i++) {
                String property = VALUE_PROPERTIES[i];
                if (MASK.equals(String.valueOf(safe.get(property)))) safe.remove(property);
            }
        }
        super.applyUpdateValues(target, safe, context);
    }

    public boolean canDelete(ParameterUmum target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Nama parameter adalah kontrak konfigurasi aplikasi dan tidak boleh dihapus.");
        return false;
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("nama");
        return result;
    }

    public void sanitizeRow(GeneralValueObject object, Map row,
            GenericCrudRequestContext context) {
        if (!isSecret(object)) return;
        for (int i = 0; i < VALUE_PROPERTIES.length; i++) {
            if (row.containsKey(VALUE_PROPERTIES[i])) row.put(VALUE_PROPERTIES[i], MASK);
        }
    }

    public boolean isSensitiveProperty(GeneralValueObject object, String property,
            GenericCrudRequestContext context) {
        if (!isSecret(object)) return false;
        for (int i = 0; i < VALUE_PROPERTIES.length; i++) {
            if (VALUE_PROPERTIES[i].equals(property)) return true;
        }
        return false;
    }

    private boolean isSecret(GeneralValueObject object) {
        if (!(object instanceof ParameterUmum)) return false;
        String name = ((ParameterUmum) object).getNama();
        if (name == null) return false;
        String normalized = name.toLowerCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
        for (int i = 0; i < SECRET_MARKERS.length; i++) {
            if (normalized.indexOf(SECRET_MARKERS[i]) >= 0) return true;
        }
        return false;
    }

    /** ParameterUmum adalah konfigurasi global; akses tetap wajib melalui RBAC route/menu. */
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }
}
