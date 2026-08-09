package ais.common.newui.menu;

import javax.servlet.http.HttpSession;

/** Facade invalidasi agar perubahan Menu/role/privilege membersihkan V1 dan V2. */
public final class NewUiHybridMenuCacheInvalidator {

    private NewUiHybridMenuCacheInvalidator() { }

    public static long invalidateAll() {
        return ais.common.newui.NewUiCacheInvalidator.invalidateAllMenuVersions();
    }

    public static void invalidateRole(String roleId) {
        ais.common.newui.NewUiCacheInvalidator.invalidateRole(roleId);
    }

    public static void invalidateSession(HttpSession session) {
        NewUiHybridMenuCache.clear(session);
        ais.common.newui.NewUiCacheInvalidator.invalidateSession(session);
    }
}
