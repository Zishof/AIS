package ais.common.newui;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Penjaga otorisasi route New UI, memakai pohon menu yang dapat diakses
 * ({@link NewUiMenuAccessService}) dan {@link NewUiRouteRegistry}.
 *
 * <p>Prinsip fail-closed: UI hanya boleh dibuka bila route termapping ke sebuah
 * Menu yang dapat dibaca role aktif. Route yang tidak termapping ditolak untuk
 * pengguna biasa (hanya Developer Catalog admin yang boleh melihatnya).</p>
 *
 * <p><b>Rollout aman:</b> penegakan dikendalikan flag konfigurasi
 * <code>nui_rbac_enforce</code> (default <b>nonaktif</b> = mode monitor), sehingga
 * dapat diaktifkan setelah tabel alias {@link NewUiRouteRegistry} dilengkapi dari
 * hasil diagnostik DB dan seluruh uji lulus. Dalam mode monitor, {@link #shouldBlock}
 * selalu false tetapi keputusan tetap dapat dievaluasi/di-log via {@link #evaluate}.</p>
 *
 * <p>Kompatibel Java 1.6.</p>
 */
public final class NewUiRouteGuard {

    private NewUiRouteGuard() {
    }

    /** Status pemetaan/otorisasi route. Lihat konstanta {@link NewUiRouteRegistry}. */
    public static int evaluate(HttpServletRequest request, String module, String page) {
        List<NewUiMenuNode> tree = NewUiMenuAccessService.getAccessibleTree(request);
        NewUiMenuNode node = findNode(tree, module, page);
        if (node != null && node.isReadable()) {
            return NewUiRouteRegistry.MAPPED_AND_AUTHORIZED;
        }
        boolean known = NewUiRouteRegistry.isKnownRouteToken(module)
                || NewUiRouteRegistry.isKnownRouteToken(module + "/" + page);
        if (known) {
            return NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN;
        }
        return NewUiRouteRegistry.UNMAPPED;
    }

    /** Hak akses untuk route (dipakai menyembunyikan/menampilkan tombol aksi). */
    public static NewUiPermission permissionFor(HttpServletRequest request, String module, String page) {
        List<NewUiMenuNode> tree = NewUiMenuAccessService.getAccessibleTree(request);
        NewUiMenuNode node = findNode(tree, module, page);
        return node != null ? node.getPermission() : NewUiPermission.none();
    }

    /** true bila UI READ diizinkan untuk route ini. */
    public static boolean isUiReadAuthorized(HttpServletRequest request, String module, String page) {
        return permissionFor(request, module, page).isCanRead();
    }

    /**
     * true bila request HARUS diblokir. Menghormati flag rollout: dalam mode monitor
     * (default) selalu false. Setelah <code>nui_rbac_enforce</code> aktif, memblokir
     * route yang MAPPED_BUT_FORBIDDEN atau UNMAPPED.
     */
    public static boolean shouldBlock(HttpServletRequest request, String module, String page) {
        if (!isEnforced()) {
            return false;
        }
        int status = evaluate(request, module, page);
        return status == NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN || status == NewUiRouteRegistry.UNMAPPED;
    }

    /** Flag penegakan RBAC New UI. Default nonaktif (monitor). */
    public static boolean isEnforced() {
        try {
            return Common.bolehKonfigurasi("nui_rbac_enforce", Konfigurasi.TIDAK_AKTIF);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRouteGuard.isEnforced");
            return false;
        }
    }

    private static NewUiMenuNode findNode(List<NewUiMenuNode> nodes, String module, String page) {
        if (nodes == null || module == null) {
            return null;
        }
        for (int i = 0; i < nodes.size(); i++) {
            NewUiMenuNode node = nodes.get(i);
            if (module.equals(node.getNewUiModule())
                    && (page == null || page.equals(node.getNewUiPage()))) {
                return node;
            }
            NewUiMenuNode inChild = findNode(node.getChildren(), module, page);
            if (inChild != null) {
                return inChild;
            }
        }
        return null;
    }
}
