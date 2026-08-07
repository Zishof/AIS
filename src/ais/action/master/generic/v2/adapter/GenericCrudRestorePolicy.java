package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

@SuppressWarnings("rawtypes")
public interface GenericCrudRestorePolicy {
    boolean canViewGlobalAudit(GenericCrudRequestContext context) throws Exception;
    boolean canRestoreField(GenericCrudRequestContext context, Serializable id, String property, Number revision) throws Exception;
    boolean canRestoreRevision(GenericCrudRequestContext context, Serializable id, Number revision, boolean deep) throws Exception;
    boolean canRestoreDeletedRecord(GenericCrudRequestContext context, Serializable id, Number revision) throws Exception;
    boolean canMassRestore(GenericCrudRequestContext context, Map filters, boolean deep) throws Exception;
    int getMaximumDeepRestoreDepth();
    int getMaximumMassRestoreItems();
    String[] getIgnoredRestoreProperties();
}
