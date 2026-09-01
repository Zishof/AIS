package ais.action.master.repository;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.A;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk repository. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid gridItem}, {@code MyGrid
 * gridCollection}, {@code Component dashboardContainer}, {@code Paging pagingItem}, {@code Textbox searchText},
 * {@code Checkbox searchAktif}, {@code MyToolbarbuttonConfig syncLocal}, {@code MyToolbarbuttonConfig
 * syncDspace}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * selectInitialTab()}, {@code initPagingListener()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * onOpenUpload()}, {@code refreshDashboard()}, {@code refreshCollections()}, {@code refreshItems()}); operasi
 * domain lain ({@code onSyncLocal()}, {@code onSyncDspace()}, {@code onOpenPublic()}, {@code onOpenWorkspace()},
 * {@code applyPrivileges()}, {@code buildItemCriteria()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class RepositoryAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    // -- Auto-wired ZUL components --
    private MyGrid           gridItem;
    private MyGrid           gridCollection;
    private Component        dashboardContainer;
    private Paging           pagingItem;
    private Textbox          searchText;
    private Checkbox         searchAktif;
    private MyToolbarbuttonConfig syncLocal;
    private MyToolbarbuttonConfig syncDspace;
    private org.zkoss.zul.Tabbox repoTabbox;

    private boolean canUpdate = false;
    private boolean canManageRepository = false;
    private Tbmuser currentUser;
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
            org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent,
            org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        currentUser = Common.getCurrentUser();
        canUpdate = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        canManageRepository = workflow.isRepositoryManager(currentUser);
        applyPrivileges();
        // Import SQL tidak diekspos dari browser. Sinkronisasi repository hanya
        // dijalankan melalui aksi/service typed dengan pemeriksaan privilege.
        initPagingListener();
        onSearchDefault(null);
        selectInitialTab();
    }

    /**
     * Memilih tab awal konsol repository berdasarkan parameter {@code tab} pada URL menu
     * (mis. {@code /pages/master/repository.zul?tab=item}). Ini membuat setiap sub-menu
     * Repository di MenuHelper mendarat langsung pada view-nya. Jika parameter tidak ada
     * atau tidak dikenal, tab Dashboard (indeks 0) tetap aktif — perilaku default lama.
     */
    private void selectInitialTab() {
        if (repoTabbox == null) {
            return;
        }
        String tab = null;
        try {
            if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
                tab = org.zkoss.zk.ui.Executions.getCurrent().getParameter("tab");
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/repository/RepositoryAction.java:99");
            // abaikan — fallback ke tab default
        }
        if (tab == null) {
            return;
        }
        int index;
        String key = tab.trim().toLowerCase();
        if ("item".equals(key)) {
            index = 1;
        } else if ("collection".equals(key) || "koleksi".equals(key)) {
            index = 2;
        } else if ("monitoring".equals(key)) {
            index = 3;
        } else {
            index = 0; // dashboard / nilai tak dikenal
        }
        try {
            if (index >= 0 && repoTabbox.getTabs() != null
                    && index < repoTabbox.getTabs().getChildren().size()) {
                repoTabbox.setSelectedIndex(index);
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/repository/RepositoryAction.java:121");
            // jangan gagalkan render halaman hanya karena pemilihan tab
        }
    }

    // -------------------------------------------------------------------------
    // Event Handlers (bound via ZUL forward="")
    // -------------------------------------------------------------------------

    public void onSearchDefault(Event event) {
        refreshDashboard();
        refreshCollections();
        refreshItems();
    }

    public void onSyncLocal(Event event) throws Exception {
        final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkron Repository");
        RepositorySyncService.synchronizeAll(false, true, laporan);
        onSearchDefault(null);
        laporan.selesaikan(null);
    }

    public void onSyncDspace(Event event) throws Exception {
        final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkron Repository + DSpace");
        RepositorySyncService.synchronizeAll(true, true, laporan);
        onSearchDefault(null);
        laporan.selesaikan(null);
    }

    /** Opens the guarded, server-rendered public repository in a new tab. */
    public void onOpenPublic(Event event) {
        String root = Common.ROOT == null ? "" : Common.ROOT;
        org.zkoss.zk.ui.Executions.getCurrent().sendRedirect(root + "/repository", "_blank");
    }

    /** Opens the same typed deposit/review/admin workflow used by the JSP version. */
    public void onOpenWorkspace(Event event) {
        String root = Common.ROOT == null ? "" : Common.ROOT;
        org.zkoss.zk.ui.Executions.getCurrent().sendRedirect(root + "/repository-workspace", "_blank");
    }

    /** Jalur eksplisit menuju deposit baru agar fitur unggah mudah ditemukan dari konsol. */
    public void onOpenUpload(Event event) {
        String root = Common.ROOT == null ? "" : Common.ROOT;
        org.zkoss.zk.ui.Executions.getCurrent().sendRedirect(
                root + "/repository-workspace?view=deposit", "_blank");
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private void applyPrivileges() {
        if (syncLocal  != null) syncLocal.setVisible(canUpdate);
        if (syncDspace != null) syncDspace.setVisible(canUpdate);
    }

    private void initPagingListener() {
        Common.initPaging(pagingItem, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onSearchDefault(null);
            }
        });
    }

    private void refreshDashboard() {
        if (dashboardContainer == null || dashboardContainer.getDesktop() == null || dashboardContainer.getPage() == null) return;
        try {
            Common.clear(dashboardContainer);
            if (dashboardContainer.getDesktop() == null || dashboardContainer.getPage() == null) return;
            new DasboardRepository().setParent(dashboardContainer);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshCollections() {
        if (gridCollection == null) return;
        Session session = HibernateUtil.currentSession();
        List<RepoCollection> data = session
                .createCriteria(RepoCollection.class)
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .addOrder(Order.asc("sortOrder"))
                .addOrder(Order.asc("nama"))
                .list();
        gridCollection.setRowRenderer(new CollectionRenderer());
        gridCollection.setModelCheckMobile(new SimpleListModel(data));
    }

    @SuppressWarnings("unchecked")
    private void refreshItems() {
        if (gridItem == null) return;
        Common.initPaging(buildItemCriteria(false), pagingItem);
        int page      = pagingItem == null ? 0 : pagingItem.getActivePage();
        int offset    = Common.ROWS_COUNT_ON_PAGE * page;
        List<RepoItem> data = buildItemCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(offset)
                .list();
        gridItem.setRowRenderer(new ItemRenderer());
        gridItem.setModelCheckMobile(new SimpleListModel(data));
    }

    private Criteria buildItemCriteria(boolean addOrder) {
        Session session = HibernateUtil.currentSession();
        boolean activeOnly = searchAktif == null || searchAktif.isChecked();
        Criteria c = session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(activeOnly
                        ? Restrictions.or(
                                Restrictions.isNull("aktif"),
                                Restrictions.eq("aktif", Boolean.TRUE))
                        : Restrictions.sqlRestriction("true"));
        String q = searchText == null ? "" : searchText.getValue().trim();
        if (q.length() > 0) {
            c.add(Restrictions.or(
                    Restrictions.ilike("title",   q, MatchMode.ANYWHERE),
                    Restrictions.or(
                        Restrictions.ilike("authors",  q, MatchMode.ANYWHERE),
                        Restrictions.ilike("subjects", q, MatchMode.ANYWHERE))));
        }
        if (addOrder) {
            c.addOrder(Order.desc("lastSyncAt"));
            c.addOrder(Order.desc("id"));
        }
        return c;
    }

    // -------------------------------------------------------------------------
    // Row Renderers
    // -------------------------------------------------------------------------

    /** Renders one row in the Collection tab. */
    class CollectionRenderer extends MyRowRenderer {
        @Override
        public void render(Row row, Object data) throws Exception {
            RepoCollection col = (RepoCollection) data;
            new Label(col.getKode()).setParent(row);
            new Label(col.getNama()).setParent(row);
            new Label(col.getTipe()).setParent(row);
            new Label(col.getDspaceHandle()).setParent(row);
            Html badge = new Html(activeBadge(col.getAktif()));
            badge.setParent(row);
        }
    }

    /** Renders one row in the Item Repository tab. */
    class ItemRenderer extends MyRowRenderer {
        @Override
        public void render(Row row, Object data) throws Exception {
            RepoItem item = (RepoItem) data;
            String root = Common.ROOT == null ? "" : Common.ROOT;
            boolean owner = currentUser != null && currentUser.getUserId().equals(item.getOwnerId());
            boolean manageable = owner || canManageRepository;
            boolean publicItem = "Publik".equals(publicationLabel(item));
            if (publicItem || manageable) {
                A title = new A(item.getTitle());
                title.setHref(publicItem ? root + "/repository/item/" + item.getId()
                        : root + "/repository-workspace?view=deposit&id=" + item.getId());
                title.setTarget("_blank");
                title.setTooltiptext(publicItem ? "Buka detail karya pada portal Repository"
                        : "Buka item pada workspace Repository");
                title.setSclass("repo-item-title-link");
                title.setParent(row);
            } else {
                new Label(item.getTitle()).setParent(row);
            }
            new Label(item.getSourceLabel()).setParent(row);
            new Label(item.getDocumentType()).setParent(row);
            Html accessBadge = new Html(accessBadge(item.getAccessPolicy()));
            accessBadge.setParent(row);
            new Label(publicationLabel(item)).setParent(row);
            Html statusBadge = new Html(syncStatusBadge(item.getSyncStatus()));
            statusBadge.setParent(row);
            Html turnitinBadge = new Html(boolBadge(item.getTurnitinIndexed(), "Ya", "Belum"));
            turnitinBadge.setParent(row);
            new Label(item.getDspaceHandle()).setParent(row);
            new Label(item.getSyncMessage()).setParent(row);
            Hbox actions = new Hbox();
            actions.setSpacing("6px");
            if (publicItem) {
                A detail = new A("Buka");
                detail.setHref(root + "/repository/item/" + item.getId());
                detail.setTarget("_blank");
                detail.setSclass("repo-row-action");
                detail.setParent(actions);
            }
            if (manageable) {
                A manage = new A("Kelola file");
                manage.setHref(root + "/repository-workspace?view=deposit&id=" + item.getId());
                manage.setTarget("_blank");
                manage.setTooltiptext("Buka metadata dan unggah berkas untuk item ini");
                manage.setSclass("repo-row-action repo-row-action-primary");
                manage.setParent(actions);
            }
            actions.setParent(row);
        }
    }

    // -------------------------------------------------------------------------
    // HTML Badge Builders (reusable)
    // -------------------------------------------------------------------------

    private String syncStatusBadge(String status) {
        if (status == null) status = "DRAFT";
        /* switch-on-String fitur Java 7; build server memakai -source 1.6. */
        String css;
        String statusUpper = status.toUpperCase();
        if ("SYNCED".equals(statusUpper)) {
            css = "repo-badge-synced";
        } else if ("FAILED".equals(statusUpper)) {
            css = "repo-badge-failed";
        } else {
            css = "repo-badge-draft";
        }
        return "<span class=\"repo-badge " + css + "\">"
                + escHtml(status) + "</span>";
    }

    private String boolBadge(Boolean value, String trueLabel, String falseLabel) {
        boolean b = value != null && value;
        String css   = b ? "repo-badge-true" : "repo-badge-false";
        String label = b ? trueLabel : falseLabel;
        return "<span class=\"repo-badge " + css + "\">" + escHtml(label) + "</span>";
    }

    private String activeBadge(Boolean aktif) {
        return boolBadge(aktif, "Aktif", "Nonaktif");
    }

    private String accessBadge(String accessPolicy) {
        String policy = accessPolicy == null ? "METADATA_ONLY" : accessPolicy.trim().toUpperCase();
        String css = "OPEN_ACCESS".equals(policy) ? "repo-badge-synced"
                : ("EMBARGOED".equals(policy) ? "repo-badge-draft" : "repo-badge-failed");
        String label = "OPEN_ACCESS".equals(policy) ? "Open Access"
                : ("METADATA_ONLY".equals(policy) ? "Metadata" : policy.replace('_', ' '));
        return "<span class=\"repo-badge " + css + "\">" + escHtml(label) + "</span>";
    }

    private String publicationLabel(RepoItem item) {
        if (item == null || Boolean.TRUE.equals(item.getIsWithdrawn())) return "Ditarik";
        String status = item.getSyncStatus() == null ? "DRAFT" : item.getSyncStatus().trim().toUpperCase();
        if ("SYNCED".equals(status) || "PUBLISHED".equals(status) || "APPROVED".equals(status)) return "Publik";
        if ("FAILED".equals(status)) return "Perlu tindakan";
        return "Draft/Internal";
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
