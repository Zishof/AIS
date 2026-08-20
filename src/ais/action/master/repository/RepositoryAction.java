package ais.action.master.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zk.ui.util.Clients;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

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
        canUpdate = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        applyPrivileges();
        initUploadSqlButton(comp);
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

    private void initUploadSqlButton(final Component parentComp) {
        Component toolbar = null;
        if (syncDspace != null) {
            toolbar = syncDspace.getParent();
        }
        if (toolbar == null && syncLocal != null) {
            toolbar = syncLocal.getParent();
        }
        if (toolbar == null) {
            return;
        }

        MyToolbarbuttonConfig uploadRepositorySql = new MyToolbarbuttonConfig("Upload SQL Repository", "/img/upload.gif");
        uploadRepositorySql.setUpload("true,maxsize=-1");
        uploadRepositorySql.setVisible(canUpdate);
        uploadRepositorySql.addEventListener("onUpload", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                UploadEvent uploadEvent = (UploadEvent) event;
                Media media = uploadEvent == null ? null : uploadEvent.getMedia();
                if (media == null || media.getName() == null || !media.getName().toLowerCase().endsWith(".sql")) {
                    MyMessageboxConfig.show("File yang di-upload harus berupa file SQL repository (.sql).",
                            "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
                    return;
                }

                final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/repository-import/"
                        + System.currentTimeMillis() + "-" + sanitizeFileName(media.getName())));
                file.getParentFile().mkdirs();
                InputStream inputStream = media.getStreamData();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    int c;
                    while ((c = inputStream.read()) != -1) {
                        fileOutputStream.write(c);
                    }
                } finally {
                    fileOutputStream.close();
                    inputStream.close();
                }

                final MyWindow progressWindow = new MyWindow();
                progressWindow.setTitle("Import Repository");
                progressWindow.setWidth("560px");
                progressWindow.setHeight("220px");
                progressWindow.setClosable(false);
                progressWindow.setSizable(false);
                final Html progressHtml = new Html(buildRepositoryProgressHtml(0, "Menyiapkan import SQL repository."));
                Vbox progressBox = new Vbox();
                progressBox.setWidth("100%");
                progressBox.setStyle("padding:16px;");
                progressBox.appendChild(progressHtml);
                progressWindow.appendChild(progressBox);
                progressWindow.setParent(parentComp);
                progressWindow.doModal();

                final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
                /* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
                 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
                 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
                 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
                 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
                ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RepositorySqlImportHelper helper = new RepositorySqlImportHelper();
                            final String[] lastMessage = new String[] { "" };
                            RepositorySqlImportHelper.Result result = helper.importSql(file,
                                    new RepositorySqlImportHelper.ProgressListener() {
                                        @Override
                                        public void onProgress(int percent, String message) {
                                            lastMessage[0] = percent + "% - " + message;
                                            if (desktop == null || !desktop.isAlive()) {
                                                return;
                                            }
                                            try {
                                                org.zkoss.zk.ui.Executions.activate(desktop);
                                                try {
                                                    progressHtml.setContent(buildRepositoryProgressHtml(percent, message));
                                                    Clients.showBusy(lastMessage[0]);
                                                } finally {
                                                    org.zkoss.zk.ui.Executions.deactivate(desktop);
                                                }
                                            } catch (Exception e) {
                                                System.out.println(lastMessage[0]);
                                            }
                                        }
                                    });
                            if (desktop == null || !desktop.isAlive()) {
                                return;
                            }
                            org.zkoss.zk.ui.Executions.activate(desktop);
                            try {
                                Clients.clearBusy();
                                if (progressWindow.getParent() != null) {
                                    progressWindow.detach();
                                }
                                onSearchDefault(null);
                                String pesan = "Import repository selesai.\nSchema staging: " + result.schema
                                        + "\nItem: " + result.importedItems
                                        + "\nMetadata: " + result.importedMetadata
                                        + "\nBitstream: " + result.importedBitstreams
                                        + "\nStatement SQL gagal: " + result.failedStatements;
                                MyMessageboxConfig.show(pesan, "Pemberitahuan", MyMessageboxConfig.OK,
                                        MyMessageboxConfig.INFORMATION);
                            } finally {
                                org.zkoss.zk.ui.Executions.deactivate(desktop);
                            }
                        } catch (Exception e) {
                            if (desktop == null || !desktop.isAlive()) {
                                return;
                            }
                            try {
                                org.zkoss.zk.ui.Executions.activate(desktop);
                                try {
                                    Clients.clearBusy();
                                    if (progressWindow.getParent() != null) {
                                        progressWindow.detach();
                                    }
                                    MyMessageboxConfig.show("Import repository gagal: " + e.getMessage(), "Error",
                                            MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
                                } finally {
                                    org.zkoss.zk.ui.Executions.deactivate(desktop);
                                }
                            } catch (Exception uiError) {
                                Common.tampilErrorJikaAdmin(uiError);
                            }
                        }
                    }
                });
            }
        });
        uploadRepositorySql.setParent(toolbar);
    }

    private String buildSyncMessage(RepositorySyncService.SyncSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dipindai : ").append(s.getScanned()).append("\n");
        sb.append("Berhasil : ").append(s.getSynced()).append("\n");
        sb.append("Gagal    : ").append(s.getFailed());
        String msg = s.getMessage();
        if (msg != null && msg.trim().length() > 0) {
            sb.append("\n\n").append(msg);
        }
        return sb.toString();
    }

    private static String buildRepositoryProgressHtml(int percent, String message) {
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        String safeMessage = message == null ? "" : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<div style='font-family:Arial,sans-serif;color:#1f2937;'>"
                + "<div style='font-size:15px;font-weight:bold;margin-bottom:8px;'>Import Repository</div>"
                + "<div style='font-size:12px;line-height:18px;margin-bottom:10px;'>" + safeMessage + "</div>"
                + "<div style='height:14px;background:#e5e7eb;border-radius:7px;overflow:hidden;'>"
                + "<div style='height:14px;width:" + percent + "%;background:#2563eb;'></div></div>"
                + "<div style='font-size:12px;color:#475569;margin-top:8px;'>Progress " + percent + "%</div>"
                + "</div>";
    }

    private Thread createDaemonThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable);
        thread.setName(name == null || name.trim().length() == 0 ? "ais-repository-worker" : name);
        thread.setDaemon(true);
        return thread;
    }

    private String sanitizeFileName(String name) {
        if (name == null) {
            return "repository.sql";
        }
        return name.replace('\\', '_').replace('/', '_').replace(':', '_');
    }

    private void refreshDashboard() {
        if (dashboardContainer == null) return;
        try {
            Common.clear(dashboardContainer);
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
            new Label(item.getTitle()).setParent(row);
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
