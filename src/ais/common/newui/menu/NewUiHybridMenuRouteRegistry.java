package ais.common.newui.menu;

import java.io.Serializable;

import ais.common.newui.NewUiRouteRegistry;

/** Resolver route New UI native dengan status eksplisit dan fail-closed. */
public final class NewUiHybridMenuRouteRegistry {

    public static final String NEW_UI = "NEW_UI";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_MAPPED = "NOT_MAPPED";
    public static final String NOT_FOUND = "NOT_FOUND";

    public static final class ResolvedRoute implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String status;
        private final String module;
        private final String page;
        private final String legacyUrl;

        ResolvedRoute(String status, String module, String page, String legacyUrl) {
            this.status = status; this.module = module; this.page = page; this.legacyUrl = legacyUrl;
        }
        public String getStatus() { return status; }
        public String getModule() { return module; }
        public String getPage() { return page; }
        public String getLegacyUrl() { return legacyUrl; }
        public boolean isValid() { return NEW_UI.equals(status); }
    }

    private NewUiHybridMenuRouteRegistry() { }

    public static ResolvedRoute resolve(Long menuId, String existingUrl, boolean openNewWindow) {
        NewUiRouteRegistry.Route route = NewUiRouteRegistry.routeForMenuIdAndUrl(menuId, existingUrl);
        if (route != null) return new ResolvedRoute(NEW_UI, route.getModule(), route.getPage(), null);
        // Native-only: URL lama hanya boleh dipakai sebagai kunci pencarian registry.
        // Route yang belum memiliki JSP New UI tidak boleh di-embed atau di-redirect.
        return new ResolvedRoute(NOT_MAPPED, null, null, null);
    }
}
