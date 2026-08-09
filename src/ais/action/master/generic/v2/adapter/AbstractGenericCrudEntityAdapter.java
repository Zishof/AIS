package ais.action.master.generic.v2.adapter;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudValueConverter;
import ais.database.model.GeneralValueObject;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractGenericCrudEntityAdapter<T extends GeneralValueObject>
        implements GenericCrudEntityAdapter<T> {

    public void configure(GenericCrudDefinition definition) throws Exception { }
    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) throws Exception { }
    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) throws Exception { }
    public void validateUpdate(T current, Map values, GenericCrudRequestContext context, List errors) throws Exception { }
    public void applyCreateValues(T target, Map values, GenericCrudRequestContext context) throws Exception { apply(target, values); }
    public void applyUpdateValues(T target, Map values, GenericCrudRequestContext context) throws Exception { apply(target, values); }
    public void beforeSave(Session session, T target, GenericCrudRequestContext context) throws Exception { }
    public void afterSave(Session session, T target, GenericCrudRequestContext context) throws Exception { }
    public boolean canDelete(T target, GenericCrudRequestContext context, List reasons) throws Exception { return false; }
    public void delete(Session session, T target, GenericCrudRequestContext context) throws Exception {
        throw new GenericCrudException(403, "DELETE_DISABLED", "Penghapusan tidak diizinkan untuk entity ini.");
    }
    public List getNaturalKeyProperties() { return new ArrayList(); }
    public GenericCrudResult executeCustomAction(String key, List ids, Map parameters, GenericCrudRequestContext context) throws Exception {
        return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Custom action tidak terdaftar.");
    }

    private void apply(T target, Map values) throws Exception {
        PropertyDescriptor[] descriptors = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            if (!values.containsKey(descriptor.getName())) { continue; }
            Method writer = descriptor.getWriteMethod();
            if (writer == null) { throw new GenericCrudException(400, "READ_ONLY_FIELD", "Field tidak dapat diubah."); }
            Object converted = GenericCrudValueConverter.convert(values.get(descriptor.getName()), descriptor.getPropertyType());
            writer.invoke(target, new Object[] { converted });
        }
    }
}
