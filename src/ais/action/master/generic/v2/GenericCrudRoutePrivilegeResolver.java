package ais.action.master.generic.v2;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.common.newui.NewUiRouteRegistry;

/** Mencocokkan route scaffold ke Menu legacy milik role aktif secara fail-closed. */
@SuppressWarnings("rawtypes")
final class GenericCrudRoutePrivilegeResolver {
    private GenericCrudRoutePrivilegeResolver() { }

    static boolean[] resolve(Tbmuser user, String module, String page, Long requestedMenuId) {
        if (user == null || page == null || page.length() < 4 || "index".equalsIgnoreCase(page)) return null;
        requestedMenuId = ais.common.newui.menu.NewUiMahasiswaMenu.authorizationMenuId(requestedMenuId);
        Tbmrole role;
        try { role = user.hakAkses(); } catch (Exception denied) { return null; }
        if (role == null || role.getRoleId() == null) return null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Tbmrole managed = (Tbmrole) session.createCriteria(Tbmrole.class)
                    .add(Restrictions.eq("roleId", role.getRoleId())).setMaxResults(1).uniqueResult();
            if (managed == null) return null;
            List rows = session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", managed)).list();
            String pageToken = normalize(page), moduleToken = normalize(module);
            RolePrivilage selected = null;
            for (int i = 0; rows != null && i < rows.size(); i++) {
                RolePrivilage privilege = (RolePrivilage) rows.get(i);
                Menu menu = privilege == null ? null : privilege.getMenu();
                if (requestedMenuId != null) {
                    if (menu == null || menu.getId() == null || !requestedMenuId.equals(menu.getId())) continue;
                    NewUiRouteRegistry.Route route = NewUiRouteRegistry.routeForMenuIdAndUrl(menu.getId(), menu.getUrl());
                    if (route != null && same(module, route.getModule()) && same(page, route.getPage())) {
                        selected = privilege;
                    }
                    break;
                }
                String url = menu == null ? "" : normalize(menu.getUrl());
                boolean pageMatch = url.indexOf(pageToken) >= 0;
                boolean moduleMatch = moduleToken.length() == 0 || "root".equals(moduleToken)
                        || url.indexOf(moduleToken) >= 0;
                if (pageMatch && moduleMatch) { selected = privilege; break; }
            }
            if (selected == null) return null;
            return new boolean[] { granted(selected.getRead()), granted(selected.getCreate()),
                    granted(selected.getUpdate()), granted(selected.getDelete()),
                    granted(selected.getApprove()), granted(selected.getReject()) };
        } catch (Exception denied) {
            return null;
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
        }
    }

    private static boolean granted(Integer value) { return value != null && value.intValue() == 1; }
    private static boolean same(String expected, String actual) {
        return expected == null ? actual == null : expected.equals(actual);
    }
    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }
}
