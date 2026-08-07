package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

@SuppressWarnings("rawtypes")
public interface GenericCrudAuditRevisionAdapter {
    boolean supportsAudit();
    boolean supportsFieldRestore();
    boolean supportsRevisionRestore();
    boolean supportsDeepRestore();
    boolean supportsMassRestore();
    List listGlobalRevisions(GenericCrudRequestContext context, Map filters, int first, int max, String sort, boolean ascending) throws Exception;
    List listRowRevisions(GenericCrudRequestContext context, Serializable id, Map filters, int first, int max) throws Exception;
    Map compareRevisions(GenericCrudRequestContext context, Serializable id, Number left, Number right) throws Exception;
    GenericCrudResult restoreField(GenericCrudRequestContext context, Serializable id, Number revision, String property, Object token, String reason) throws Exception;
    GenericCrudResult correctField(GenericCrudRequestContext context, Serializable id, String property, Object value, Object token, String reason) throws Exception;
    GenericCrudResult restoreRevision(GenericCrudRequestContext context, Serializable id, Number revision, boolean deep, Object token, String reason) throws Exception;
    Map previewRestoreLatestFromDate(GenericCrudRequestContext context, Date from, Map activeFilter, boolean deep) throws Exception;
    String enqueueRestoreLatestFromDate(GenericCrudRequestContext context, Date from, Map activeFilter, boolean deep, String reason, String idempotencyKey) throws Exception;
}
