package ais.common.newui;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;

/**
 * Sumber tunggal navigasi & hak akses New UI berbasis RBAC existing.
 *
 * <p>Menghasilkan pohon {@link NewUiMenuNode} yang HANYA memuat menu yang:
 * <ol>
 *   <li>di-assign ke role aktif ({@link Tbmrole#getMenus()} / <code>job_has_menu</code>);</li>
 *   <li>aktif (<code>Menu.aktif</code> tidak bernilai false);</li>
 *   <li>lolos lingkup lembaga (<code>tampilDiPt</code>/<code>tampilDiSekolah</code>);</li>
 *   <li>dapat dibaca (ada <code>RolePrivilage._read=1</code>) — <b>fail-closed</b>, atau
 *       merupakan parent yang menampung minimal satu descendant yang dapat dibaca.</li>
 * </ol>
 * Dipakai bersama sidebar, command palette, breadcrumb, dan route guard.</p>
 *
 * <p>Efisiensi: privilege role diambil sekali secara batch (menghindari N+1). Hasil
 * di-cache per-session dengan penanda <code>version|roleId</code>; bila
 * {@link NewUiCacheInvalidator#getGlobalVersion()} atau role aktif berubah, tree
 * dibangun ulang pada request berikutnya.</p>
 *
 * <p>Kompatibel Java 1.6. Semua entity di-materialisasi menjadi DTO sebelum session
 * ditutup untuk menghindari LazyInitializationException.</p>
 */
public final class NewUiMenuAccessService {

    private static final int READ = 0;

    private NewUiMenuAccessService() {
    }

    /**
     * Pohon menu yang dapat diakses user pada request ini (root-level nodes).
     * Mengembalikan list kosong bila belum login / role null / tidak ada menu.
     */
    @SuppressWarnings("unchecked")
    public static List<NewUiMenuNode> getAccessibleTree(HttpServletRequest request) {
        List<NewUiMenuNode> empty = new ArrayList<NewUiMenuNode>();
        if (request == null) {
            return empty;
        }

        Tbmuser tbmuser = null;
        try {
            tbmuser = Common.getCurrentUser(request);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.getCurrentUser");
        }
        if (tbmuser == null) {
            return empty;
        }

        Tbmrole role = tbmuser.hakAkses();
        if (role == null || role.getRoleId() == null || Boolean.FALSE.equals(role.getAktif())) {
            return empty;
        }

        // --- cache per-session ber-versi ---
        HttpSession httpSession = request.getSession();
        String marker = NewUiCacheInvalidator.getGlobalVersion() + "|" + role.getRoleId();
        try {
            Object cachedMarker = httpSession.getAttribute(NewUiCacheInvalidator.SESSION_TREE_VERSION);
            Object cachedTree = httpSession.getAttribute(NewUiCacheInvalidator.SESSION_TREE);
            if (marker.equals(cachedMarker) && cachedTree instanceof List) {
                return (List<NewUiMenuNode>) cachedTree;
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.cacheRead");
        }

        boolean sekolahContext = resolveSekolahContext(request, tbmuser);

        // --- build flat DTO list dalam satu session (hindari N+1) ---
        List<NewUiMenuNode> flat = new ArrayList<NewUiMenuNode>();
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();

            Tbmrole managed = (Tbmrole) session.createCriteria(Tbmrole.class)
                    .add(Restrictions.eq("roleId", role.getRoleId())).setMaxResults(1).uniqueResult();
            if (managed == null) {
                return empty;
            }

            // privilege role sekali (batch) → map menuId -> RolePrivilage
            Map<Long, RolePrivilage> privByMenu = new HashMap<Long, RolePrivilage>();
            List<RolePrivilage> privs = session.createCriteria(RolePrivilage.class)
                    .add(Restrictions.eq("role", managed)).list();
            if (privs != null) {
                for (int i = 0; i < privs.size(); i++) {
                    RolePrivilage rp = privs.get(i);
                    if (rp != null && rp.getMenu() != null && rp.getMenu().getId() != null) {
                        privByMenu.put(rp.getMenu().getId(), rp);
                    }
                }
            }

            Set<Menu> menuSet = managed.getMenus();
            if (menuSet != null) {
                Iterator<Menu> it = menuSet.iterator();
                while (it.hasNext()) {
                    Menu menu = it.next();
                    if (menu == null || menu.getId() == null) {
                        continue;
                    }
                    // aktif: sembunyikan hanya bila eksplisit false (konsisten dgn CommonMenu)
                    if (Boolean.FALSE.equals(menu.getAktif())) {
                        continue;
                    }
                    // lingkup lembaga
                    if (!passesInstitutionScope(menu, sekolahContext)) {
                        continue;
                    }
                    flat.add(toNode(menu, NewUiPermission.from(privByMenu.get(menu.getId()))));
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.build");
        } finally {
            closeQuietly(session);
        }

        // --- urutkan lalu bangun tree + prune (di luar session) ---
        Collections.sort(flat, NODE_ORDER);
        List<NewUiMenuNode> roots = buildTree(flat);
        List<NewUiMenuNode> pruned = pruneUnreadable(roots);

        try {
            httpSession.setAttribute(NewUiCacheInvalidator.SESSION_TREE, pruned);
            httpSession.setAttribute(NewUiCacheInvalidator.SESSION_TREE_VERSION, marker);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.cacheWrite");
        }
        return pruned;
    }

    /** Cek satu hak (READ/CREATE/…): pakai ulang CommonPrivilages agar konsisten. */
    public static boolean hasPrivilege(HttpServletRequest request, Menu menu, Integer privilegeCode) {
        if (menu == null) {
            return false;
        }
        Tbmuser tbmuser = null;
        try {
            tbmuser = Common.getCurrentUser(request);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.hasPrivilege.user");
        }
        if (tbmuser == null) {
            return false;
        }
        return ais.common.CommonPrivilages.checkPrevilages(menu, privilegeCode, tbmuser);
    }

    // ------------------------------------------------------------------
    // Helper internal
    // ------------------------------------------------------------------

    private static NewUiMenuNode toNode(Menu menu, NewUiPermission permission) {
        NewUiMenuNode node = new NewUiMenuNode();
        node.setMenuId(menu.getId());
        node.setLabel(menu.getLabel() != null ? menu.getLabel() : "Menu");
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

    /** URL legacy /baru — sama dengan pola CommonMenu.buildMenuItem. */
    private static String buildLegacyUrl(Menu menu) {
        if (menu.getUrl() == null || menu.getUrl().length() == 0) {
            return null;
        }
        try {
            String p = menu.getUrl().replaceAll("\\p{Punct}", "");
            return Common.ROOT + "/baru?p=" + URLEncoder.encode(p, "UTF-8") + "&menu=" + menu.getId();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.buildLegacyUrl");
            return null;
        }
    }

    private static boolean passesInstitutionScope(Menu menu, boolean sekolahContext) {
        // Lenient: sembunyikan hanya bila flag konteks aktif bernilai false eksplisit.
        if (sekolahContext) {
            return !Boolean.FALSE.equals(menu.getTampilDiSekolah());
        }
        return !Boolean.FALSE.equals(menu.getTampilDiPt());
    }

    private static boolean resolveSekolahContext(HttpServletRequest request, Tbmuser tbmuser) {
        try {
            Sekolah sekolah = SekolahUtil.getSekolah(request);
            if (tbmuser != null && tbmuser.getSekolah() != null && tbmuser.getSekolah().getId() != null) {
                sekolah = tbmuser.getSekolah();
            }
            return sekolah != null && sekolah.getId() != null;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.resolveSekolahContext");
            return false;
        }
    }

    /** Bangun tree dari list datar: root==0 = top-level; anak = node dgn root==induk.child. */
    private static List<NewUiMenuNode> buildTree(List<NewUiMenuNode> flat) {
        // index child-key -> list node yang root-nya == child-key
        Map<Long, List<NewUiMenuNode>> byRoot = new HashMap<Long, List<NewUiMenuNode>>();
        for (int i = 0; i < flat.size(); i++) {
            NewUiMenuNode n = flat.get(i);
            Long r = n.getRoot() == null ? Long.valueOf(0L) : n.getRoot();
            List<NewUiMenuNode> bucket = byRoot.get(r);
            if (bucket == null) {
                bucket = new ArrayList<NewUiMenuNode>();
                byRoot.put(r, bucket);
            }
            bucket.add(n);
        }
        List<NewUiMenuNode> roots = byRoot.get(Long.valueOf(0L));
        if (roots == null) {
            roots = new ArrayList<NewUiMenuNode>();
        }
        for (int i = 0; i < roots.size(); i++) {
            attachChildren(roots.get(i), byRoot, 0);
        }
        return roots;
    }

    private static void attachChildren(NewUiMenuNode node, Map<Long, List<NewUiMenuNode>> byRoot, int depth) {
        if (depth > 12) {
            return; // jaga-jaga terhadap data melingkar
        }
        Long key = node.getChild();
        if (key == null) {
            return;
        }
        List<NewUiMenuNode> kids = byRoot.get(key);
        if (kids == null || kids.isEmpty()) {
            return;
        }
        List<NewUiMenuNode> childList = new ArrayList<NewUiMenuNode>();
        for (int i = 0; i < kids.size(); i++) {
            NewUiMenuNode child = kids.get(i);
            if (child == node) {
                continue;
            }
            attachChildren(child, byRoot, depth + 1);
            childList.add(child);
        }
        node.setChildren(childList);
    }

    /**
     * Buang node yang tidak dapat dibaca dan tidak punya descendant yang dapat dibaca.
     * Node yang dipertahankan hanya karena anaknya ditandai sebagai group (heading).
     */
    private static List<NewUiMenuNode> pruneUnreadable(List<NewUiMenuNode> nodes) {
        List<NewUiMenuNode> kept = new ArrayList<NewUiMenuNode>();
        if (nodes == null) {
            return kept;
        }
        for (int i = 0; i < nodes.size(); i++) {
            NewUiMenuNode node = nodes.get(i);
            List<NewUiMenuNode> keptChildren = pruneUnreadable(node.getChildren());
            node.setChildren(keptChildren);
            boolean selfReadable = node.isReadable();
            boolean hasTarget = (node.getLegacyUrl() != null && node.getLegacyUrl().length() > 0)
                    || node.isMappedToNewUi();
            if (selfReadable || !keptChildren.isEmpty()) {
                // group/heading jika tidak dapat diklik sendiri tetapi menampung anak
                node.setGroup(!keptChildren.isEmpty() && (!selfReadable || !hasTarget));
                kept.add(node);
            }
        }
        return kept;
    }

    private static void closeQuietly(Session session) {
        try {
            if (session != null) {
                session.disconnect();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.close.disconnect");
        }
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.close.close");
        }
        try {
            HibernateUtil.closeSession();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiMenuAccessService.close.hib");
        }
    }

    /** Urutan: nomorUrut ASC (null terakhir), root ASC, child ASC, menuId ASC. */
    private static final Comparator<NewUiMenuNode> NODE_ORDER = new Comparator<NewUiMenuNode>() {
        public int compare(NewUiMenuNode a, NewUiMenuNode b) {
            int c = compareIntNullsLast(a.getNomorUrut(), b.getNomorUrut());
            if (c != 0) {
                return c;
            }
            c = compareLong(a.getRoot(), b.getRoot());
            if (c != 0) {
                return c;
            }
            c = compareLong(a.getChild(), b.getChild());
            if (c != 0) {
                return c;
            }
            return compareLong(a.getMenuId(), b.getMenuId());
        }
    };

    private static int compareIntNullsLast(Integer a, Integer b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.intValue() < b.intValue() ? -1 : (a.intValue() > b.intValue() ? 1 : 0);
    }

    private static int compareLong(Long a, Long b) {
        long x = a == null ? 0L : a.longValue();
        long y = b == null ? 0L : b.longValue();
        return x < y ? -1 : (x > y ? 1 : 0);
    }
}
