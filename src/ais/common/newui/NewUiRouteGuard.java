package ais.common.newui;

import javax.servlet.http.HttpServletRequest;

import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuRouteGuard;
import ais.common.newui.menu.NewUiHybridMenuRouteRegistry;
import ais.common.newui.menu.NewUiHybridMenuSnapshot;

/** Fail-closed guard untuk route UI dan action service New UI. */
public final class NewUiRouteGuard {

    private NewUiRouteGuard() {
    }

    public static int evaluate(HttpServletRequest request, String module, String page) {
        if (module != null && module.startsWith("_shared")) {
            return NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
        }
        Long menuId = menuId(request);
        NewUiHybridMenuSnapshot snapshot = NewUiMenuAccessService.getSnapshot(request);
        String status = NewUiHybridMenuRouteGuard.evaluateMenu(snapshot, menuId);
        NewUiHybridMenuNode node = menuId == null ? null : snapshot.findAssigned(menuId);
        if (NewUiHybridMenuRouteRegistry.NEW_UI.equals(status)
                && node != null && module != null && module.equals(node.getNewUiModule())) {
            String wanted = page == null || page.length() == 0 ? "index" : page;
            String actual = node.getNewUiPage() == null ? "index" : node.getNewUiPage();
            if (wanted.equals(actual)) return NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
        }
        if (NewUiHybridMenuRouteRegistry.FORBIDDEN.equals(status)) return NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN;
        return NewUiRouteRegistry.UNMAPPED;
    }

    public static NewUiPermission permissionFor(HttpServletRequest request, String module, String page) {
        NewUiMenuNode node = NewUiMenuAccessService.findAssigned(request, menuId(request));
        return node == null ? NewUiPermission.none() : node.getPermission();
    }

    public static boolean isUiReadAuthorized(HttpServletRequest request, String module, String page) {
        return evaluate(request, module, page) == NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
    }

    public static boolean shouldBlock(HttpServletRequest request, String module, String page) {
        int status = evaluate(request, module, page);
        return status != NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
    }

    public static boolean isActionAuthorized(HttpServletRequest request, String module, String page, String action) {
        if (evaluate(request, module, page) != NewUiRouteRegistry.MAPPED_AND_AUTHORIZED) return false;
        if (module != null && module.startsWith("_shared")) return true;
        NewUiPermission permission = permissionFor(request, module, page);
        String value = action == null ? "meta" : action.trim().toLowerCase();
        if (isReadAction(value)) return permission.isCanRead();
        if ("create".equals(value) || "new".equals(value) || "insert".equals(value)
                || "add".equals(value) || "save-new".equals(value)) return permission.isCanCreate();
        if ("save".equals(value)) {
            String id = request == null ? null : request.getParameter("id");
            return id == null || id.trim().length() == 0 ? permission.isCanCreate() : permission.isCanUpdate();
        }
        if ("update".equals(value) || "edit".equals(value) || "photo".equals(value)
                || "upload".equals(value) || "save-existing".equals(value)
                || "restore_field".equals(value) || "restore_revision".equals(value)) return permission.isCanUpdate();
        if ("import".equals(value) || value.startsWith("import_")) return permission.isCanCreate() && permission.isCanUpdate();
        if ("delete".equals(value) || "remove".equals(value)
                || "permanent-delete".equals(value) || value.startsWith("admin_delete_")) return permission.isCanDelete();
        if ("approve".equals(value)) return permission.isCanApprove();
        if ("reject".equals(value)) return permission.isCanReject();
        return false;
    }

    private static boolean isReadAction(String value) {
        return "meta".equals(value) || "health".equals(value) || "read".equals(value)
                || "list".equals(value) || "detail".equals(value) || "get".equals(value)
                || "options".equals(value) || "lookup".equals(value)
                || "relation_lookup".equals(value) || "search".equals(value)
                || "revisions".equals(value) || "global_revisions".equals(value)
                || "compare".equals(value) || "export".equals(value) || value.startsWith("export_")
                || value.startsWith("preference_") || value.startsWith("saved_view_");
    }

    /** Penegakan selalu aktif; method dipertahankan untuk kompatibilitas pemanggil lama. */
    public static boolean isEnforced() { return true; }

    private static Long menuId(HttpServletRequest request) {
        if (request == null) return null;
        String value = request.getParameter("menuId");
        if (value == null || value.trim().length() == 0) value = request.getParameter("menu");
        try { return value == null ? null : Long.valueOf(value); }
        catch (Exception e) { return null; }
    }
}
