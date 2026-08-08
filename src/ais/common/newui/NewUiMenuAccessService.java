package ais.common.newui;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Konfigurasi;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;

/** Sumber tunggal sidebar, command palette, breadcrumb, dan route guard New UI. */
public final class NewUiMenuAccessService {

    private static final Logger LOG = Logger.getLogger(NewUiMenuAccessService.class.getName());

    private NewUiMenuAccessService() {
    }

    @SuppressWarnings("unchecked")
    public static List<NewUiMenuNode> getAccessibleTree(HttpServletRequest request) {
        List<NewUiMenuNode> empty = new ArrayList<NewUiMenuNode>();
        if (request == null) return empty;
        Tbmuser user = currentUser(request);
        if (user == null || user.getUserId() == null) return empty;
        Tbmrole activeRole = user.hakAkses();
        if (activeRole == null || activeRole.getRoleId() == null || Boolean.FALSE.equals(activeRole.getAktif())) {
            return empty;
        }

        String scope = institutionScope(request, user);
        String marker = NewUiCacheInvalidator.getGlobalVersion() + "|" + user.getUserId() + "|"
                + activeRole.getRoleId() + "|" + scope;
        HttpSession httpSession = request.getSession();
        Object cachedMarker = httpSession.getAttribute(NewUiCacheInvalidator.SESSION_TREE_VERSION);
        Object cachedTree = httpSession.getAttribute(NewUiCacheInvalidator.SESSION_TREE);
        if (marker.equals(cachedMarker) && cachedTree instanceof List) {
            return (List<NewUiMenuNode>) cachedTree;
        }

        boolean schoolContext = scope.startsWith("school:");
        boolean filterPerSchool = false;
        try {
            filterPerSchool = Common.bolehKonfigurasi("aktifkan_filter_per_sekolah", Konfigurasi.TIDAK_AKTIF);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.filterConfiguration");
        }
        List<NewUiMenuNode> flat = loadFlat(activeRole.getRoleId(), schoolContext, filterPerSchool);
        NewUiMenuTreeBuilder.Result result = NewUiMenuTreeBuilder.buildAndPrune(flat);
        if (result.getDuplicateCount() > 0 || result.getOrphanCount() > 0 || result.getCycleCount() > 0) {
            LOG.warning("New UI menu diagnostics: duplicate=" + result.getDuplicateCount() + ", orphan="
                    + result.getOrphanCount() + ", cycle=" + result.getCycleCount());
        }
        List<NewUiMenuNode> tree = result.getRoots();
        httpSession.setAttribute(NewUiCacheInvalidator.SESSION_TREE, tree);
        httpSession.setAttribute(NewUiCacheInvalidator.SESSION_TREE_VERSION, marker);
        return tree;
    }

    /** Tiga query tetap: assignment role, seluruh privilege role, metadata menu. */
    @SuppressWarnings("unchecked")
    private static List<NewUiMenuNode> loadFlat(String roleId, boolean schoolContext, boolean filterPerSchool) {
        List<NewUiMenuNode> flat = new ArrayList<NewUiMenuNode>();
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            List<Object> assignedRows = session.createQuery(
                    "select m.id from Tbmrole r join r.menus m where r.roleId = :roleId")
                    .setString("roleId", roleId).list();
            Set<Long> assignedIds = new HashSet<Long>();
            for (int i = 0; i < assignedRows.size(); i++) {
                Object id = assignedRows.get(i);
                if (id instanceof Number) assignedIds.add(Long.valueOf(((Number) id).longValue()));
            }

            List<RolePrivilage> privileges = session.createQuery(
                    "select rp from RolePrivilage rp join fetch rp.menu m where rp.role.roleId = :roleId")
                    .setString("roleId", roleId).list();
            Map<Long, NewUiPermission> permissionByMenu = new HashMap<Long, NewUiPermission>();
            for (int i = 0; i < privileges.size(); i++) {
                RolePrivilage rp = privileges.get(i);
                if (rp != null && rp.getMenu() != null && rp.getMenu().getId() != null) {
                    permissionByMenu.put(rp.getMenu().getId(), NewUiPermission.from(rp));
                }
            }

            List<Menu> menus = session.createQuery("from Menu m").list();
            for (int i = 0; i < menus.size(); i++) {
                Menu menu = menus.get(i);
                if (menu == null || menu.getId() == null || Boolean.FALSE.equals(menu.getAktif())
                        || !passesInstitutionScope(menu, schoolContext, filterPerSchool)) continue;
                // Parent tetap dimaterialisasi tanpa privilege; hanya assignment+RP memberi izin self.
                NewUiPermission permission = assignedIds.contains(menu.getId())
                        ? permissionByMenu.get(menu.getId()) : null;
                flat.add(toNode(menu, permission == null ? NewUiPermission.none() : permission));
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.loadFlat");
        } finally {
            closeQuietly(session);
        }
        LOG.fine("New UI menu snapshot role=" + roleId + ", queryCount=3, metadataNodes=" + flat.size());
        return flat;
    }

    private static NewUiMenuNode toNode(Menu menu, NewUiPermission permission) {
        NewUiMenuNode node = new NewUiMenuNode();
        node.setMenuId(menu.getId());
        node.setLabel(menu.getLabel() == null || menu.getLabel().trim().length() == 0 ? "Menu" : menu.getLabel());
        node.setIcon(menu.getIcon());
        node.setNomorUrut(menu.getNomorUrut());
        node.setRoot(menu.getRoot());
        node.setChild(menu.getChild());
        node.setPermission(permission);
        node.setLegacyUrl(buildLegacyUrl(menu));
        String[] route = NewUiRouteRegistry.routeForMenu(menu);
        if (route != null) {
            node.setNewUiModule(route[0]);
            node.setNewUiPage(route[1]);
        }
        return node;
    }

    private static String buildLegacyUrl(Menu menu) {
        String url = menu == null ? null : menu.getUrl();
        if (!NewUiRouteRegistry.isSafeLegacyUrl(url)) return null;
        try {
            String p = url.replaceAll("\\p{Punct}", "");
            return Common.ROOT + "/baru?p=" + URLEncoder.encode(p, "UTF-8") + "&menu=" + menu.getId();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.buildLegacyUrl");
            return null;
        }
    }

    public static NewUiMenuNode findById(HttpServletRequest request, Long menuId) {
        return findById(getAccessibleTree(request), menuId);
    }

    public static NewUiMenuNode findByRoute(HttpServletRequest request, String module, String page) {
        return findByRoute(getAccessibleTree(request), module, page);
    }

    public static List<NewUiMenuNode> breadcrumb(HttpServletRequest request, Long menuId) {
        List<NewUiMenuNode> path = new ArrayList<NewUiMenuNode>();
        findPath(getAccessibleTree(request), menuId, path);
        return path;
    }

    private static boolean findPath(List<NewUiMenuNode> nodes, Long id, List<NewUiMenuNode> path) {
        if (nodes == null || id == null) return false;
        for (int i = 0; i < nodes.size(); i++) {
            NewUiMenuNode node = nodes.get(i);
            path.add(node);
            if (id.equals(node.getMenuId()) || findPath(node.getChildren(), id, path)) return true;
            path.remove(path.size() - 1);
        }
        return false;
    }

    private static NewUiMenuNode findById(List<NewUiMenuNode> nodes, Long id) {
        if (nodes == null || id == null) return null;
        for (int i = 0; i < nodes.size(); i++) {
            NewUiMenuNode node = nodes.get(i);
            if (id.equals(node.getMenuId())) return node;
            NewUiMenuNode child = findById(node.getChildren(), id);
            if (child != null) return child;
        }
        return null;
    }

    private static NewUiMenuNode findByRoute(List<NewUiMenuNode> nodes, String module, String page) {
        if (nodes == null || module == null) return null;
        String wantedPage = page == null || page.length() == 0 ? "index" : page;
        for (int i = 0; i < nodes.size(); i++) {
            NewUiMenuNode node = nodes.get(i);
            String nodePage = node.getNewUiPage() == null ? "index" : node.getNewUiPage();
            if (module.equals(node.getNewUiModule()) && wantedPage.equals(nodePage)) return node;
            NewUiMenuNode child = findByRoute(node.getChildren(), module, page);
            if (child != null) return child;
        }
        return null;
    }

    private static boolean passesInstitutionScope(Menu menu, boolean schoolContext, boolean filterPerSchool) {
        boolean visible = schoolContext ? !Boolean.FALSE.equals(menu.getTampilDiSekolah())
                : !Boolean.FALSE.equals(menu.getTampilDiPt());
        if (!visible || !filterPerSchool || menu.getLabel() == null) return visible;
        if (schoolContext && "Sistem Informasi Akademik".equalsIgnoreCase(menu.getLabel())) return false;
        if (!schoolContext && "Sistem Sekolah".equalsIgnoreCase(menu.getLabel())) return false;
        return true;
    }

    private static String institutionScope(HttpServletRequest request, Tbmuser user) {
        try {
            Sekolah school = user.getSekolah();
            if (school == null || school.getId() == null) school = SekolahUtil.getSekolah(request);
            return school != null && school.getId() != null ? "school:" + school.getId() : "pt";
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.institutionScope");
            return "pt";
        }
    }

    private static Tbmuser currentUser(HttpServletRequest request) {
        try { return Common.getCurrentUser(request); }
        catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.currentUser");
            return null;
        }
    }

    private static void closeQuietly(Session session) {
        try { if (session != null) session.disconnect(); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.disconnect"); }
        try { HibernateUtil.closeSession(); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.close"); }
    }
}
