package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ais.action.master.generic.v2.adapter.GenericCrudFormDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudFormOverrideProvider;
import ais.action.master.generic.v2.adapter.GenericCrudApprovalAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;
import ais.action.master.generic.v2.adapter.GenericCrudDashboardProvider;

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
        result.put("sourceActionClassName", d.getSourceActionClassName());
        result.put("existingActionLifecycleBound", Boolean.valueOf(d.isExistingActionLifecycleBound()));
        result.put("metadataLifecycleBound", Boolean.valueOf(d.isMetadataLifecycleBound()));
        result.put("fields", d.getFields());
        result.put("pageSize", Integer.valueOf(d.getDefaultPageSize()));
        result.put("maxPageSize", Integer.valueOf(d.getMaxPageSize()));
        result.put("lookupThreshold", Integer.valueOf(d.getLookupThreshold()));
        result.put("formMode", d.getFormMode());
        result.put("versionProperty", d.getVersionProperty());
        result.put("identifierProperty", d.getIdentifierProperty());
        result.put("canCreate", Boolean.valueOf(context.isCanCreate() && d.isCreateEnabled()));
        result.put("canUpdate", Boolean.valueOf(context.isCanUpdate() && d.isUpdateEnabled()));
        result.put("canDelete", Boolean.valueOf(context.isCanDelete() && d.isDeleteEnabled()));
        result.put("rowAudit", Boolean.valueOf(d.isRowAuditEnabled()));
        result.put("restore", Boolean.valueOf(d.isRestoreEnabled()));
        result.put("adminDelete", Boolean.valueOf(d.isAdminDeleteEnabled()));
        result.put("importEnabled", Boolean.valueOf(d.isImportEnabled() && context.isCanCreate()
                && context.isCanUpdate() && (!d.isImportDeleteEnabled() || context.isCanDelete())
                && (!d.isImportRequiresApprove() || context.isCanApprove())));
        result.put("importDeleteEnabled", Boolean.valueOf(d.isImportDeleteEnabled() && context.isCanDelete()));
        result.put("exportXlsx", Boolean.valueOf(d.isExportXlsxEnabled()));
        result.put("exportPdf", Boolean.valueOf(d.isExportPdfEnabled()));
        result.put("exportDocx", Boolean.valueOf(d.isExportDocxEnabled()));
        result.put("exportPptx", Boolean.valueOf(d.isExportPptxEnabled()));
        result.put("savedViewEnabled", Boolean.valueOf(d.isSavedViewEnabled()));
        result.put("photoEnabled", Boolean.valueOf(d.isPhotoEnabled()));
        result.put("attachmentEnabled", Boolean.valueOf(d.isAttachmentEnabled()));
        boolean approval = d.getAdapter() instanceof GenericCrudApprovalAdapter;
        result.put("approvalEnabled", Boolean.valueOf(approval));
        result.put("canApprove", Boolean.valueOf(approval && context.isCanApprove()));
        result.put("canReject", Boolean.valueOf(approval && context.isCanReject()));
        if (d.getAdapter() instanceof GenericCrudCustomActionProvider) {
            result.put("customActions", ((GenericCrudCustomActionProvider) d.getAdapter()).getActions(d, context));
        }
        if (d.getAdapter() instanceof GenericCrudDashboardProvider) {
            result.put("dashboard", ((GenericCrudDashboardProvider) d.getAdapter()).getDashboard(context));
        }
        if (d.isPhotoEnabled()) {
            result.put("photoUrlTemplate", context.getRequest().getContextPath()
                    + "/AmbilFotoMahasiswa?nim={nim}&v={id}");
        }
        if ("ais.database.model.Mahasiswa".equals(d.getEntityKey())) {
            result.put("sourceMethodParity", ais.action.master.generic.v2.adapter.MahasiswaActionParityContract.metadata());
            Long selected = selectedMenuId(context.getRequest());
            String[] selection = ais.common.newui.menu.NewUiMahasiswaMenu.selection(selected);
            if (selection != null) {
                result.put("menuSelectionId", selected);
                result.put("menuSelectionLabel", selection[0]);
                result.put("menuSelectionGroup", selection[1]);
                result.put("menuSelectionAction", selection[2]);
            }
        }
        GenericCrudFormOverrideProvider formProvider = d.getFormOverrideProvider();
        if (formProvider != null) {
            GenericCrudFormDefinition form = formProvider.getDefinition(context, null, true);
            result.put("formDefinition", form);
            result.put("formActions", formProvider.getFormActions(context, null));
        } else {
            List parityActions = new GenericCrudZkossParityService().actions(context);
            if (!parityActions.isEmpty()) result.put("formActions", parityActions);
        }
        return result;
    }

    private Long selectedMenuId(HttpServletRequest request) {
        String requested = request == null ? null : request.getParameter("selectionMenuId");
        if (requested != null && requested.trim().length() > 0) {
            try {
                Long selection = Long.valueOf(requested);
                Long authorization = request.getParameter("menuId") == null ? null
                        : Long.valueOf(request.getParameter("menuId"));
                if (authorization != null && authorization.equals(
                        ais.common.newui.menu.NewUiMahasiswaMenu.authorizationMenuId(selection))) return selection;
            } catch (Exception ignored) { }
        }
        Object value = request == null ? null : request.getAttribute("nui_current_menu_id");
        if (value instanceof Long) return (Long) value;
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); }
        catch (Exception ignored) { return null; }
    }

    public GenericCrudPage list(GenericCrudRequestContext context, int page, int pageSize, String search,
            List filters, GenericCrudSort sort) throws Exception { return query.list(context, page, pageSize, search, filters, sort); }
    public Map get(GenericCrudRequestContext context, Serializable id) throws Exception { return query.get(context, id); }
    public GenericCrudResult create(GenericCrudRequestContext context, Map values) throws Exception { return mutation.create(context, values); }
    public GenericCrudResult update(GenericCrudRequestContext context, Serializable id, Map values, Object token) throws Exception { return mutation.update(context, id, values, token); }
    public GenericCrudResult delete(GenericCrudRequestContext context, Serializable id) throws Exception { return mutation.softDelete(context, id); }
    public GenericCrudPage revisions(GenericCrudRequestContext context, Serializable id, int page, int size) throws Exception { return audit.listRowRevisions(context, id, page, size); }
    public GenericCrudPage globalRevisions(GenericCrudRequestContext context, int page, int size) throws Exception { return audit.listGlobalRevisions(context, page, size); }
    public Map compare(GenericCrudRequestContext context, Serializable id, Number left, Number right) throws Exception { return audit.compare(context, id, left, right); }
}
