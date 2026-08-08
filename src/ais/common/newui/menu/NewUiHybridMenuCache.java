package ais.common.newui.menu;

import javax.servlet.http.HttpSession;

/** Session cache untuk snapshot hybrid yang telah terlepas dari Hibernate. */
public final class NewUiHybridMenuCache {

    public static final String SNAPSHOT = "new_ui_hybrid_menu_snapshot";
    public static final String MARKER = "new_ui_hybrid_menu_marker";
    public static final String ROLE_ID = "new_ui_hybrid_menu_role_id";
    public static final String PERMISSIONS = "new_ui_hybrid_permission_map";
    public static final String ROUTES = "new_ui_hybrid_route_map";

    private NewUiHybridMenuCache() { }

    public static NewUiHybridMenuSnapshot get(HttpSession session, String marker) {
        if (session == null || marker == null || !marker.equals(session.getAttribute(MARKER))) return null;
        Object value = session.getAttribute(SNAPSHOT);
        return value instanceof NewUiHybridMenuSnapshot ? (NewUiHybridMenuSnapshot) value : null;
    }

    public static void put(HttpSession session, String marker, NewUiHybridMenuSnapshot snapshot) {
        if (session == null) return;
        session.setAttribute(MARKER, marker); session.setAttribute(SNAPSHOT, snapshot);
        session.setAttribute(ROLE_ID, snapshot == null ? null : snapshot.getRoleId());
    }

    public static void clear(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(SNAPSHOT); session.removeAttribute(MARKER); session.removeAttribute(ROLE_ID);
        session.removeAttribute(PERMISSIONS); session.removeAttribute(ROUTES);
    }
}
