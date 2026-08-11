package ais.action.master.generic.v2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Memisahkan parameter kontrol HTTP (id target, version, CSRF, action) dari
 * nilai entity yang benar-benar boleh dimutasi. Filter yang sama dipakai
 * CREATE dan UPDATE sebelum payload masuk ke MutationService.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudSubmittedValues {
    private GenericCrudSubmittedValues() { }

    public static Map fromParameters(GenericCrudDefinition definition, Map parameters,
            boolean create) {
        Map result = new LinkedHashMap();
        if (definition == null || parameters == null) return result;
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            String property = field.getProperty();
            boolean mutable = create ? field.isCreateable() : field.isUpdateable();
            // ID pada UPDATE adalah locator record, bukan perubahan primary key.
            if (!create && definition.getIdentifierProperty().equals(property)) mutable = false;
            if (!mutable || !parameters.containsKey(property)) continue;
            result.put(property, first(parameters.get(property)));
        }
        return result;
    }

    private static Object first(Object value) {
        if (value instanceof String[]) {
            String[] values = (String[]) value;
            return values.length == 0 ? null : values[0];
        }
        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            return values.length == 0 ? null : values[0];
        }
        return value;
    }
}
