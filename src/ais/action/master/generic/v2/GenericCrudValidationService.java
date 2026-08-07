package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class GenericCrudValidationService {
    public List validateRequired(GenericCrudDefinition definition, Map values, boolean create) {
        List errors = new ArrayList();
        Iterator iterator = definition.getFields().iterator();
        while (iterator.hasNext()) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) iterator.next();
            if (!field.isRequired() || (!create && !values.containsKey(field.getProperty()))) { continue; }
            Object value = values.get(field.getProperty());
            if (value == null || String.valueOf(value).trim().length() == 0) {
                errors.add(field.getProperty() + ":" + field.getLabel() + " wajib diisi");
            }
        }
        return errors;
    }
}
