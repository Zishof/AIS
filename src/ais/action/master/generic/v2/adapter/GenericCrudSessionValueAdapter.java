package ais.action.master.generic.v2.adapter;

import java.util.Map;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;

/**
 * Extension point untuk field yang harus di-resolve memakai Session aktif,
 * misalnya many-to-one. Interface terpisah menjaga kompatibilitas adapter lama.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudSessionValueAdapter {
    void applyCreateValues(Session session, GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception;
    void applyUpdateValues(Session session, GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception;
}
