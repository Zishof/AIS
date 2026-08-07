package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/** Satu pintu API; setiap service tetap mengulang guard server-side. */
@SuppressWarnings("rawtypes")
public class GenericCrudFacade {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudQueryService query = new GenericCrudQueryService();
    private final GenericCrudMutationService mutation = new GenericCrudMutationService();
    private final GenericCrudAuditService audit = new GenericCrudAuditService();

    public GenericCrudRequestContext context(HttpServletRequest request, String entity, String module, String page) throws Exception {
        return GenericCrudRequestContext.from(request, GenericCrudDefinitionRegistry.resolve(entity, module, page));
    }

    public Map metadata(GenericCrudRequestContext context) throws Exception {
        privilege.require(context, GenericCrudOperation.READ);
        GenericCrudDefinition d = context.getDefinition();
        Map result = new java.util.LinkedHashMap();
        result.put("entityKey", d.getEntityKey());
        result.put("moduleKey", d.getModuleKey());
        result.put("pageKey", d.getPageKey());
        result.put("displayName", d.getDisplayName());
        result.put("lifecycleStatus", d.getLifecycleStatus());
        result.put("fields", d.getFields());
        result.put("pageSize", Integer.valueOf(d.getDefaultPageSize()));
        result.put("maxPageSize", Integer.valueOf(d.getMaxPageSize()));
        result.put("lookupThreshold", Integer.valueOf(d.getLookupThreshold()));
        result.put("formMode", d.getFormMode());
        result.put("canCreate", Boolean.valueOf(context.isCanCreate() && d.isCreateEnabled()));
        result.put("canUpdate", Boolean.valueOf(context.isCanUpdate() && d.isUpdateEnabled()));
        result.put("canDelete", Boolean.valueOf(context.isCanDelete() && d.isDeleteEnabled()));
        result.put("rowAudit", Boolean.valueOf(d.isRowAuditEnabled()));
        result.put("restore", Boolean.valueOf(d.isRestoreEnabled()));
        result.put("adminDelete", Boolean.valueOf(d.isAdminDeleteEnabled()));
        return result;
    }

    public GenericCrudPage list(GenericCrudRequestContext context, int page, int pageSize, String search,
            List filters, GenericCrudSort sort) throws Exception { return query.list(context, page, pageSize, search, filters, sort); }
    public Map get(GenericCrudRequestContext context, Serializable id) throws Exception { return query.get(context, id); }
    public GenericCrudResult create(GenericCrudRequestContext context, Map values) throws Exception { return mutation.create(context, values); }
    public GenericCrudResult update(GenericCrudRequestContext context, Serializable id, Map values, Object token) throws Exception { return mutation.update(context, id, values, token); }
    public GenericCrudResult delete(GenericCrudRequestContext context, Serializable id) throws Exception { return mutation.softDelete(context, id); }
    public GenericCrudPage revisions(GenericCrudRequestContext context, Serializable id, int page, int size) throws Exception { return audit.listRowRevisions(context, id, page, size); }
}
