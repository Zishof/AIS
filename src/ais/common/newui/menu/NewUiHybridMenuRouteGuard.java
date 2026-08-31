package ais.common.newui.menu;

import javax.servlet.http.HttpServletRequest;

/** Guard server-side untuk groupMenuId, menuId, serta privilege action. */
public final class NewUiHybridMenuRouteGuard {

    private NewUiHybridMenuRouteGuard() { }

    public static String evaluateGroup(NewUiHybridMenuSnapshot snapshot, Long groupId) {
        if (snapshot == null || groupId == null) return NewUiHybridMenuRouteRegistry.NOT_FOUND;
        NewUiHybridMenuNode node = snapshot.findVisible(groupId);
        return node != null && node.isBranch() ? NewUiHybridMenuRouteRegistry.NEW_UI
                : NewUiHybridMenuRouteRegistry.FORBIDDEN;
    }

    public static String evaluateMenu(NewUiHybridMenuSnapshot snapshot, Long menuId) {
        if (snapshot == null || menuId == null) return NewUiHybridMenuRouteRegistry.NOT_FOUND;
        NewUiHybridMenuNode node = snapshot.findAssigned(menuId);
        if (node == null || !node.isAssigned() || node.getPermission() == null
                || !node.getPermission().isCanRead()) return NewUiHybridMenuRouteRegistry.FORBIDDEN;
        if (!node.hasValidRoute()) return NewUiHybridMenuRouteRegistry.NOT_MAPPED;
        return node.getRouteStatus();
    }

    public static boolean isActionAuthorized(HttpServletRequest request, NewUiHybridMenuNode node, String action) {
        if (node == null || node.getPermission() == null || !node.getPermission().isCanRead()) return false;
        ais.common.newui.NewUiPermission permission = node.getPermission();
        String value = action == null ? "meta" : action.trim().toLowerCase();
        if (isReadAction(value)) return permission.isCanRead();
        if ("operation_meta".equals(value)) return permission.isCanRead();
        if ("operation_download".equals(value)) {
            String subroute = request == null ? null : request.getParameter("nativeSubroute");
            if ("download_lampiran".equalsIgnoreCase(subroute)) return permission.isCanCreate() && permission.isCanUpdate();
            return permission.isCanRead();
        }
        if ("operation_upload".equals(value) || "operation_execute".equals(value)) {
            String subroute = request == null ? null : request.getParameter("nativeSubroute");
            if ("import_data".equalsIgnoreCase(subroute)) return permission.isCanCreate() && permission.isCanUpdate();
            return permission.isCanUpdate();
        }
        if ("create".equals(value) || "new".equals(value) || "insert".equals(value)
                || "add".equals(value) || "save-new".equals(value)) return permission.isCanCreate();
        if ("save".equals(value)) {
            String id = request == null ? null : request.getParameter("id");
            return id == null || id.trim().length() == 0 ? permission.isCanCreate() : permission.isCanUpdate();
        }
        if ("update".equals(value) || "edit".equals(value) || "photo".equals(value)
                || "photo_upload".equals(value) || "photo_delete".equals(value)
                || "upload".equals(value) || "save-existing".equals(value)
                || "restore_field".equals(value) || "restore_revision".equals(value)
                // Aksi workflow keuangan (mis. tagihan): controller memetakan
                // privilege yang sama dan tetap memvalidasi ulang di service.
                || "generate".equals(value) || "sync".equals(value)
                || "toggle".equals(value)) return permission.isCanUpdate();
        if ("import".equals(value) || value.startsWith("import_")) return permission.isCanCreate() && permission.isCanUpdate();
        if ("delete".equals(value) || "remove".equals(value) || "permanent-delete".equals(value)
                || "move".equals(value)
                || value.startsWith("admin_delete_")) return permission.isCanDelete();
        if ("approve".equals(value)) return permission.isCanApprove();
        if ("reject".equals(value)) return permission.isCanReject();
        return false;
    }

    private static boolean isReadAction(String value) {
        return "meta".equals(value) || "health".equals(value) || "read".equals(value)
                || "list".equals(value) || "detail".equals(value) || "get".equals(value)
                || "options".equals(value) || "lookup".equals(value)
                // ringkasan read-only (mis. Informasi Pembayaran Mahasiswa)
                || "informasi".equals(value)
                // penetapan harga keranjang kantin: hanya membaca lalu menghitung.
                // Harus ada di KEDUA penjaga; index.jsp memanggil penjaga ini dulu.
                || "harga".equals(value)
                || "relation_lookup".equals(value) || "search".equals(value)
                || "revisions".equals(value) || "global_revisions".equals(value)
                || "compare".equals(value) || "export".equals(value) || value.startsWith("export_")
                || "history".equals(value) || "move_targets".equals(value)
                || value.startsWith("preference_") || value.startsWith("saved_view_");
    }
}
