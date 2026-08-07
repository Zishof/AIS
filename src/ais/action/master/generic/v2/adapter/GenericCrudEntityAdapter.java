package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.database.model.GeneralValueObject;

@SuppressWarnings("rawtypes")
public interface GenericCrudEntityAdapter<T extends GeneralValueObject> {
    void configure(GenericCrudDefinition definition) throws Exception;
    void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) throws Exception;
    T createNew(GenericCrudRequestContext context) throws Exception;
    void validateCreate(Map values, GenericCrudRequestContext context, List fieldErrors) throws Exception;
    void validateUpdate(T current, Map values, GenericCrudRequestContext context, List fieldErrors) throws Exception;
    void applyCreateValues(T target, Map values, GenericCrudRequestContext context) throws Exception;
    void applyUpdateValues(T target, Map values, GenericCrudRequestContext context) throws Exception;
    void beforeSave(Session session, T target, GenericCrudRequestContext context) throws Exception;
    void afterSave(Session session, T target, GenericCrudRequestContext context) throws Exception;
    boolean canDelete(T target, GenericCrudRequestContext context, List blockingReasons) throws Exception;
    void delete(Session session, T target, GenericCrudRequestContext context) throws Exception;
    List getNaturalKeyProperties();
    GenericCrudResult executeCustomAction(String actionKey, List selectedIds, Map parameters, GenericCrudRequestContext context) throws Exception;
}
