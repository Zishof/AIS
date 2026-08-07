package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

@SuppressWarnings("rawtypes")
public interface GenericCrudPermanentDeletePolicy {
    boolean isEnabled();
    boolean canDeleteActiveRow(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    Map buildPreflight(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    String getTypedConfirmation(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    boolean isReasonRequired();
    void beforeDelete(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    void afterDelete(GenericCrudRequestContext context, Serializable id, Map beforeMaskedSnapshot) throws Exception;
}
