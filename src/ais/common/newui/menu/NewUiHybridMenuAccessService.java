package ais.common.newui.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.newui.NewUiCacheInvalidator;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;

/** Sumber tunggal snapshot Menu/job_has_menu/RolePrivilage untuk Hybrid New UI. */
public final class NewUiHybridMenuAccessService {

    private static final Logger LOG = Logger.getLogger(NewUiHybridMenuAccessService.class.getName());

    private NewUiHybridMenuAccessService() { }

    public static NewUiHybridMenuSnapshot getSnapshot(HttpServletRequest request) {
        if (request == null) return NewUiHybridMenuSnapshot.empty();
        Tbmuser user = currentUser(request);
        if (user == null || user.getUserId() == null) return NewUiHybridMenuSnapshot.empty();
        Tbmrole role = null;
        try { role = user.hakAkses(); } catch (Exception e) { record(e, "activeRole"); }
        if (role == null || role.getRoleId() == null || Boolean.FALSE.equals(role.getAktif())) {
            return NewUiHybridMenuSnapshot.empty();
        }

        String scope = institutionScope(request, user);
        String marker = NewUiCacheInvalidator.getGlobalVersion() + "|" + user.getUserId() + "|"
                + role.getRoleId() + "|" + scope;
        HttpSession httpSession = request.getSession();
        NewUiHybridMenuSnapshot cached = NewUiHybridMenuCache.get(httpSession, marker);
        if (cached != null) return cached;

        boolean school = scope.startsWith("school:");
        boolean filterPerSchool = false;
        try { filterPerSchool = Common.bolehKonfigurasi("aktifkan_filter_per_sekolah", Konfigurasi.TIDAK_AKTIF); }
        catch (Exception e) { record(e, "filterConfiguration"); }

        List<NewUiHybridMenuNode> assigned = loadAssigned(role.getRoleId(), school, filterPerSchool,
                request.getContextPath());
        NewUiHybridMenuTreeBuilder.Result result = NewUiHybridMenuTreeBuilder.build(assigned);
        NewUiHybridMenuSnapshot snapshot = new NewUiHybridMenuSnapshot(user.getUserId(), role.getRoleId(), scope, result);
        NewUiHybridMenuCache.put(httpSession, marker, snapshot);
        if (snapshot.getDiagnostics().hasCriticalWarnings()) {
            LOG.warning("Hybrid menu role=" + role.getRoleId() + ": " + snapshot.getDiagnostics().summary());
        } else if (snapshot.getDiagnostics().hasWarnings()) {
            LOG.fine("Hybrid menu recovered role=" + role.getRoleId() + ": " + snapshot.getDiagnostics().summary());
        }
        return snapshot;
    }

    /** Dua query batch: assigned Menu role dan seluruh RolePrivilage role. */
    @SuppressWarnings("unchecked")
    private static List<NewUiHybridMenuNode> loadAssigned(String roleId, boolean school,
            boolean filterPerSchool, String contextPath) {
        List<NewUiHybridMenuNode> result = new ArrayList<NewUiHybridMenuNode>();
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            List<Menu> menus = session.createQuery(
                    "select distinct m from Tbmrole r join r.menus m where r.roleId = :roleId")
                    .setString("roleId", roleId).list();
            List<RolePrivilage> privilegeRows = session.createQuery(
                    "select rp from RolePrivilage rp join fetch rp.menu m where rp.role.roleId = :roleId")
                    .setString("roleId", roleId).list();
            Map<Long, NewUiPermission> permissions = new HashMap<Long, NewUiPermission>();
            Map<Long, Boolean> effectiveMenuIds = new HashMap<Long, Boolean>();
            for (int i = 0; i < menus.size(); i++) {
                Menu assigned = menus.get(i);
                if (assigned != null && assigned.getId() != null) {
                    effectiveMenuIds.put(assigned.getId(), Boolean.TRUE);
                }
            }
            int recoveredAdministratorMenus = 0;
            for (int i = 0; i < privilegeRows.size(); i++) {
                RolePrivilage row = privilegeRows.get(i);
                if (row != null && row.getMenu() != null && row.getMenu().getId() != null) {
                    permissions.put(row.getMenu().getId(), NewUiPermission.from(row));
                    /*
                     * TbmroleAction.Reset Menu memulihkan job_has_menu dari seluruh
                     * RolePrivilage role. Terapkan pemulihan efektif yang sama hanya
                     * untuk role administrator: tidak menulis database dan tidak
                     * memberikan privilege baru, tetapi mencegah menu READ admin
                     * hilang ketika relasi job_has_menu tertinggal/tidak lengkap.
                     */
                    if (Tbmrole.ADMINISTRATOR.equals(roleId)
                            && !effectiveMenuIds.containsKey(row.getMenu().getId())) {
                        menus.add(row.getMenu());
                        effectiveMenuIds.put(row.getMenu().getId(), Boolean.TRUE);
                        recoveredAdministratorMenus++;
                    }
                }
            }
            for (int i = 0; i < menus.size(); i++) {
                Menu menu = menus.get(i);
                if (menu == null || menu.getId() == null || Boolean.FALSE.equals(menu.getAktif())
                        || !passesInstitutionScope(menu, school, filterPerSchool)) continue;
                NewUiPermission permission = permissions.get(menu.getId());
                result.add(toNode(menu, permission == null ? NewUiPermission.none() : permission, contextPath));
            }
            if (recoveredAdministratorMenus > 0) {
                LOG.info("Hybrid menu administrator recoveredFromRolePrivilage="
                        + recoveredAdministratorMenus + ", effectiveAssigned=" + menus.size());
            }
        } catch (Exception e) {
            record(e, "loadAssigned");
        } finally {
            closeQuietly(session);
        }
        LOG.fine("Hybrid menu role=" + roleId + ", batchQueries=2, assignedInScope=" + result.size());
        return result;
    }

    private static NewUiHybridMenuNode toNode(Menu menu, NewUiPermission permission, String contextPath) {
        NewUiHybridMenuNode node = new NewUiHybridMenuNode();
        node.setMenuId(menu.getId()); node.setLabel(cleanLabel(menu.getLabel())); node.setIcon(menu.getIcon());
        node.setNomorUrut(menu.getNomorUrut()); node.setRoot(menu.getRoot()); node.setChild(menu.getChild());
        node.setPermission(permission); node.setAssigned(true); node.setActiveInScope(true);
        node.setExistingUrl(menu.getUrl());
        NewUiHybridMenuRouteRegistry.ResolvedRoute route = NewUiHybridMenuRouteRegistry.resolve(
                menu.getId(), menu.getUrl(), Boolean.TRUE.equals(menu.getBukaHalamanBaru()));
        node.setRouteStatus(route.getStatus());
        if (NewUiHybridMenuRouteRegistry.NEW_UI.equals(route.getStatus())) {
            node.setNewUiModule(route.getModule()); node.setNewUiPage(route.getPage());
            node.setResolvedUrl(safeContext(contextPath) + "/new?frame=1&menuId=" + menu.getId());
        }
        node.setAliases(aliasFor(node.getLabel()));
        node.setKeywords(keywordFor(menu.getUrl()));
        return node;
    }

    public static NewUiHybridMenuNode findByRoute(NewUiHybridMenuSnapshot snapshot, String module, String page) {
        if (snapshot == null || module == null) return null;
        String wantedPage = page == null || page.length() == 0 ? "index" : page;
        List<NewUiHybridMenuNode> all = snapshot.getSearchableNodes();
        for (int i = 0; i < all.size(); i++) {
            NewUiHybridMenuNode node = all.get(i);
            String nodePage = node.getNewUiPage() == null ? "index" : node.getNewUiPage();
            if (module.equals(node.getNewUiModule()) && wantedPage.equals(nodePage)) return node;
        }
        return null;
    }

    private static boolean passesInstitutionScope(Menu menu, boolean school, boolean filterPerSchool) {
        boolean visible = school ? !Boolean.FALSE.equals(menu.getTampilDiSekolah())
                : !Boolean.FALSE.equals(menu.getTampilDiPt());
        if (!visible || !filterPerSchool || menu.getLabel() == null) return visible;
        if (school && "Sistem Informasi Akademik".equalsIgnoreCase(menu.getLabel())) return false;
        if (!school && "Sistem Sekolah".equalsIgnoreCase(menu.getLabel())) return false;
        return true;
    }

    private static String institutionScope(HttpServletRequest request, Tbmuser user) {
        try {
            Sekolah school = user.getSekolah();
            if (school == null || school.getId() == null) school = SekolahUtil.getSekolah(request);
            return school != null && school.getId() != null ? "school:" + school.getId() : "pt";
        } catch (Exception e) { record(e, "institutionScope"); return "pt"; }
    }

    private static Tbmuser currentUser(HttpServletRequest request) {
        try { return Common.getCurrentUser(request); }
        catch (Exception e) { record(e, "currentUser"); return null; }
    }
    private static String cleanLabel(String value) { return value == null || value.trim().length() == 0 ? "Menu" : value.trim(); }
    private static String safeContext(String value) { return value == null ? "" : value; }
    private static String aliasFor(String label) {
        if (label == null) return "";
        return label.toLowerCase().replace("mata kuliah", "matakuliah").replace("kemahasiswaan", "mahasiswa");
    }
    private static String keywordFor(String url) {
        if (url == null) return "";
        return url.replaceAll("[^A-Za-z0-9]+", " ").replace("pages", "").replace("master", "").trim();
    }
    private static void closeQuietly(Session session) {
        try { if (session != null) session.disconnect(); } catch (Exception e) { record(e, "disconnect"); }
        try { HibernateUtil.closeSession(); } catch (Exception e) { record(e, "closeSession"); }
    }
    private static void record(Exception e, String point) { ais.common.ErrorAuditUtil.record(e, "NewUiHybridMenuAccessService." + point); }
}
