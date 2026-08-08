package ais.common.newui;

import javax.servlet.http.HttpServletRequest;

/** Fail-closed guard untuk route UI dan action service New UI. */
public final class NewUiRouteGuard {

    private NewUiRouteGuard() {
    }

    public static int evaluate(HttpServletRequest request, String module, String page) {
        if (module != null && module.startsWith("_shared")) {
            return NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
        }
        NewUiMenuNode node = NewUiMenuAccessService.findByRoute(request, module, page);
        if (node != null && node.isReadable()) return NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
        if (NewUiRouteRegistry.isKnownNewUiRoute(module, page)) {
            return NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN;
        }
        return NewUiRouteRegistry.UNMAPPED;
    }

    public static NewUiPermission permissionFor(HttpServletRequest request, String module, String page) {
        NewUiMenuNode node = NewUiMenuAccessService.findByRoute(request, module, page);
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
        if ("meta".equals(value) || "health".equals(value) || "read".equals(value)
                || "list".equals(value) || "detail".equals(value) || "options".equals(value)
                || "export".equals(value) || "search".equals(value)) return permission.isCanRead();
        if ("create".equals(value) || "new".equals(value) || "insert".equals(value)
                || "add".equals(value) || "save-new".equals(value)) return permission.isCanCreate();
        if ("save".equals(value)) {
            String id = request == null ? null : request.getParameter("id");
            return id == null || id.trim().length() == 0 ? permission.isCanCreate() : permission.isCanUpdate();
        }
        if ("update".equals(value) || "edit".equals(value) || "photo".equals(value)
                || "upload".equals(value) || "import".equals(value)
                || "save-existing".equals(value)) return permission.isCanUpdate();
        if ("delete".equals(value) || "remove".equals(value)
                || "permanent-delete".equals(value)) return permission.isCanDelete();
        if ("approve".equals(value)) return permission.isCanApprove();
        if ("reject".equals(value)) return permission.isCanReject();
        return false;
    }

    /** Penegakan selalu aktif; method dipertahankan untuk kompatibilitas pemanggil lama. */
    public static boolean isEnforced() { return true; }
}
