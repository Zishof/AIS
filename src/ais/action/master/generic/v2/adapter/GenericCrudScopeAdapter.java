package ais.action.master.generic.v2.adapter;

import org.hibernate.Criteria;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;

public interface GenericCrudScopeAdapter {
    void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception;
    void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception;
    void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception;
}
