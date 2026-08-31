package ais.action.master.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dashboard widget untuk halaman Repository.
 *
 * Arsitektur:
 *  1. Async two-step render: tampilkan loading → query data → render UI.
 *  2. Semua query pakai aggregate (rowCount, groupBy) → ringan untuk data besar.
 *  3. Layout: hero → filter → metric cards → portal (funnel, quality, counters,
 *     recent activity, insight, radar chip).
 *  4. Semua HTML di-escape; tidak ada string raw yang tidak ter-escape.
 */
public class DasboardRepository extends MyPortallayout {

    private static final long serialVersionUID = 1L;
    private static final int RECENT_ITEM_LIMIT  = 10;
    private static final int TOP_LIST_LIMIT     = 8;

    private Date    filterMulai;
    private Date    filterSampai;
    private String  filterKeyword = "";

    private transient Html loadingHtml;

    // -------------------------------------------------------------------------
    // Constructor & Entry Point
    // -------------------------------------------------------------------------

    public DasboardRepository() throws Exception {
        super();
        setWidth("100%");
        setMaximizedMode("whole");
        startRender();
    }

    // -------------------------------------------------------------------------
    // Async Render Pipeline
    // -------------------------------------------------------------------------

    private void startRender() throws Exception {
        showLoading("Menyiapkan dasbor repository dan membaca filter awal…", 8);
        final DashboardData[] ref = new DashboardData[1];
        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                updateLoading("Menghitung koleksi, item, bitstream, DSpace, dan status Turnitin…", 35);
                ref[0] = loadData();
                updateLoading("Merender kartu overview dan analitik repository…", 78);
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event e2) throws Exception {
                        renderAll(ref[0]);
                    }
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Main Render
    // -------------------------------------------------------------------------

    private void renderAll(DashboardData d) throws Exception {
        Common.clear(this);

        MyPortalchildren wrapper = portalChildren(this, "100%",
                "box-sizing:border-box;padding:10px;");

        Panel rootPanel = styledPanel("Dasbor Repository Digital", wrapper,
                "width:100%;box-sizing:border-box;margin:0 0 14px 0;");
        Panelchildren body = styledPanelChildren(rootPanel,
                "width:100%;box-sizing:border-box;padding:10px;background:#f8fafc;");

        org.zkoss.zul.Div shell = div(body, "width:100%;box-sizing:border-box;overflow:hidden;");

        renderHero(shell, d);
        renderFilter(shell);
        renderMetricCards(shell, d);

        MyPortallayout portal = new MyPortallayout();
        portal.setWidth("100%");
        portal.setStyle("width:100%;box-sizing:border-box;border:0;background:transparent;");
        portal.setParent(shell);

        boolean mobile    = Common.isMobile();
        String  halfWidth = mobile ? "100%" : "50%";

        MyPortalchildren colTop    = portalChildren(portal, "100%",  "padding:6px;");
        MyPortalchildren colLeft   = portalChildren(portal, halfWidth, "padding:6px;");
        MyPortalchildren colRight  = portalChildren(portal, halfWidth, "padding:6px;");
        MyPortalchildren colBottom = portalChildren(portal, "100%",  "padding:6px;");

        renderFunnel(colTop, d);
        renderMetadataQuality(colLeft, d);
        renderAccessAndTurnitin(colRight, d);
        renderTopList(colLeft,  "Sebaran Collection",   d.perCollection,
                "Collection dengan jumlah item paling banyak.");
        renderTopList(colRight, "Sebaran Sumber Data",  d.perSource,
                "Asal data: eCampus, eSchool, perpustakaan, publikasi.");
        renderTopList(colLeft,  "Sebaran Jenis Dokumen", d.perDocumentType,
                "Dominasi tipe: thesis, book, article, dan lainnya.");
        renderRecentActivity(colRight, d);
        renderInsight(colBottom, d);
        renderRadarChips(colBottom, d);
    }

    // -------------------------------------------------------------------------
    // Section Renderers
    // -------------------------------------------------------------------------

    private void renderHero(Component parent, DashboardData d) {
        org.zkoss.zul.Div hero = div(parent,
                "width:100%;box-sizing:border-box;padding:20px 24px;margin-bottom:14px;"
                + "border-radius:14px;"
                + "background:linear-gradient(135deg,rgba(0,0,0,.32),rgba(0,0,0,0) 55%),"
                + "var(--theme-gradient,linear-gradient(135deg,var(--theme-primary,#1d4ed8) 0%,"
                + "var(--theme-primary,#1d4ed8) 45%,var(--theme-hover,#06b6d4) 100%));"
                + "color:var(--theme-text-on-primary,#ffffff);"
                + "text-shadow:var(--theme-text-shadow,none);");
        addHtml(hero,
                "<div style='font-size:11px;font-weight:600;letter-spacing:.12em;"
                + "text-transform:uppercase;opacity:.75;margin-bottom:4px;'>"
                + "Repository Control Center</div>"
                + "<div style='font-size:22px;font-weight:700;margin-bottom:6px;'>"
                + "Dasbor Repository Digital</div>"
                + "<div style='font-size:12px;opacity:.85;max-width:560px;line-height:1.6;"
                + "margin-bottom:16px;'>"
                + "Pantau kesiapan metadata, dokumen, sinkronisasi DSpace, akses publik, "
                + "dan indeks Turnitin dalam satu layar. Semua data menggunakan query agregat "
                + "agar tetap ringan untuk korpus besar.</div>"
                + "<div style='display:flex;flex-wrap:wrap;gap:16px;'>"
                + heroStat("Total Item",     d.totalItem)
                + heroStat("Tersinkron",     d.syncedItem)
                + heroStat("Indeks Turnitin", d.turnitinIndexed)
                + heroStat("Gagal Sync",     d.failedItem)
                + "</div>");
    }

    private String heroStat(String label, int value) {
        return "<div style='background:rgba(255,255,255,.15);border-radius:10px;"
                + "padding:10px 16px;min-width:90px;text-align:center;'>"
                + "<div style='font-size:24px;font-weight:700;line-height:1;'>" + value + "</div>"
                + "<div style='font-size:11px;opacity:.8;margin-top:4px;'>" + esc(label) + "</div>"
                + "</div>";
    }

    private void renderFilter(final Component parent) {
        org.zkoss.zul.Div box = div(parent,
                "width:100%;box-sizing:border-box;margin:0 0 14px 0;padding:10px 14px;"
                + "border-radius:10px;background:#ffffff;border:1px solid #dbe4ef;");
        Toolbar tb = new Toolbar();
        tb.setStyle("border:0;background:transparent;padding:4px;box-sizing:border-box;");
        tb.setParent(box);

        new MyLabelAgakKecil("Mulai:").setParent(tb);
        final MyDatebox mulai = new MyDatebox(filterMulai);
        mulai.setReadonly(true); mulai.setCols(5); mulai.setParent(tb);

        new MyLabelAgakKecil("Sampai:").setParent(tb);
        final MyDatebox sampai = new MyDatebox(filterSampai);
        sampai.setReadonly(true); sampai.setCols(5); sampai.setParent(tb);

        new MyLabelAgakKecil("Cari:").setParent(tb);
        final Textbox kw = new Textbox(filterKeyword == null ? "" : filterKeyword);
        kw.setCols(18);
        kw.setTooltiptext("Cari judul, author, subject, sumber, atau status sync");
        kw.setParent(tb);

        MyToolbarbuttonConfig applyBtn = new MyToolbarbuttonConfig("Terapkan Filter", "/img/svg/search.svg");
        applyBtn.setStyle("font-weight:bold;font-size:12px;margin-left:8px;");
        applyBtn.setParent(tb);
        applyBtn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                filterMulai   = mulai.getValue();
                filterSampai  = sampai.getValue();
                filterKeyword = kw.getValue();
                Common.clear(DasboardRepository.this);
                startRender();
            }
        });
    }

    private void renderMetricCards(Component parent, DashboardData d) {
        org.zkoss.zul.Div wrap = div(parent,
                "width:100%;box-sizing:border-box;margin:0 0 14px 0;"
                + "display:flex;flex-wrap:wrap;gap:10px;");
        metricCard(wrap, "Collection",   d.totalCollection, "Struktur koleksi aktif",  "#dbeafe", "#1e40af", "C");
        metricCard(wrap, "Total Item",   d.totalItem,       "Semua item repository",   "#ecfdf5", "#166534", "I");
        metricCard(wrap, "Bitstream",    d.totalBitstream,  "File & dokumen tersimpan","#fef3c7", "#92400e", "F");
        metricCard(wrap, "Tersinkron",   d.syncedItem,      "Status sync berhasil",    "#dcfce7", "#166534", "S");
        metricCard(wrap, "Gagal Sync",   d.failedItem,      "Perlu dicek ulang",       "#fee2e2", "#991b1b", "!");
        metricCard(wrap, "Turnitin",     d.turnitinIndexed, "Siap / sudah diindeks",   "#ede9fe", "#5b21b6", "T");
    }

    private void metricCard(Component parent, String title, int value,
            String desc, String bg, String color, String icon) {
        addHtml(parent,
                "<div style='flex:1 1 140px;min-width:140px;max-width:220px;"
                + "background:#ffffff;border:1px solid #e2e8f0;"
                + "border-radius:12px;padding:14px 16px;"
                + "box-shadow:0 4px 14px rgba(15,23,42,.07);box-sizing:border-box;'>"
                + "<div style='display:flex;align-items:center;gap:10px;margin-bottom:8px;'>"
                + "<div style='width:36px;height:36px;border-radius:9px;background:" + bg + ";"
                + "color:" + color + ";font-weight:700;font-size:16px;"
                + "display:flex;align-items:center;justify-content:center;'>" + esc(icon) + "</div>"
                + "<div style='font-size:26px;font-weight:700;color:#111827;line-height:1;'>"
                + value + "</div></div>"
                + "<div style='font-size:12px;font-weight:600;color:#374151;'>" + esc(title) + "</div>"
                + "<div style='font-size:11px;color:#9ca3af;margin-top:2px;'>" + esc(desc) + "</div>"
                + "</div>");
    }

    private void renderFunnel(Component parent, DashboardData d) {
        Panelchildren pch = createPanel("Overview Funnel Repository", parent);
        addExplanation(pch, "Posisi data dari item mentah → metadata lengkap → "
                + "file tersedia → sinkron DSpace → siap indeks Turnitin.");
        int max = maxOf(d.totalItem, d.metadataComplete, d.withBitstream, d.syncedItem, d.turnitinIndexed);
        funnelRow(pch, "Total Item Repository",  d.totalItem,       max, "#2563eb");
        funnelRow(pch, "Metadata Lengkap",       d.metadataComplete,max, "#16a34a");
        funnelRow(pch, "Memiliki File/Bitstream", d.withBitstream,   max, "#d97706");
        funnelRow(pch, "Sinkron DSpace",         d.syncedItem,      max, "#7c3aed");
        funnelRow(pch, "Indeks Turnitin",        d.turnitinIndexed, max, "#0891b2");
    }

    private void renderMetadataQuality(Component parent, DashboardData d) {
        Panelchildren pch = createPanel("Kualitas Metadata Dublin Core", parent);
        addExplanation(pch, "Kelengkapan metadata inti untuk discovery, OAI-PMH, DSpace, dan indeks Turnitin.");
        int max = Math.max(d.totalItem, 1);
        funnelRow(pch, "Judul (dc.title)",           d.withTitle,      max, "#2563eb");
        funnelRow(pch, "Author (dc.contributor)",     d.withAuthor,     max, "#16a34a");
        funnelRow(pch, "Abstract (dc.description)",   d.withAbstract,   max, "#d97706");
        funnelRow(pch, "Subject/Keyword (dc.subject)", d.withSubject,   max, "#7c3aed");
        funnelRow(pch, "Tanggal Terbit (dc.date)",    d.withIssuedDate, max, "#0891b2");
    }

    private void renderAccessAndTurnitin(Component parent, DashboardData d) {
        Panelchildren pch = createPanel("Akses, Preservasi, dan Turnitin", parent);
        addExplanation(pch, "Status akses publik, dokumen embargo/restricted, "
                + "integritas checksum, dan cakupan indeks Turnitin.");
        int maxItem  = Math.max(d.totalItem,     1);
        int maxBits  = Math.max(d.totalBitstream, 1);
        funnelRow(pch, "Open Access",          d.openAccess,       maxItem, "#16a34a");
        funnelRow(pch, "Restricted / Embargo", d.restrictedAccess, maxItem, "#d97706");
        funnelRow(pch, "Checksum File",        d.withChecksum,     maxBits, "#2563eb");
        funnelRow(pch, "Turnitin Indexed",     d.turnitinIndexed,  maxItem, "#7c3aed");
        funnelRow(pch, "Gagal Sync",           d.failedItem,       maxItem, "#b91c1c");
    }

    private void renderTopList(Component parent, String title,
            Map<String, Integer> map, String info) {
        Panelchildren pch = createPanel(title, parent);
        addExplanation(pch, info);
        if (map == null || map.isEmpty()) {
            addEmpty(pch, "Belum ada data yang dapat ditampilkan.");
            return;
        }
        List<LabelCount> rows = sortedDesc(map);
        int max = rows.isEmpty() ? 1 : rows.get(0).count;
        int limit = Math.min(rows.size(), TOP_LIST_LIMIT);
        for (int i = 0; i < limit; i++) {
            LabelCount item = rows.get(i);
            String color = (i % 2 == 0) ? "#2563eb" : "#16a34a";
            funnelRow(pch, item.label, item.count, max, color);
        }
    }

    private void renderRecentActivity(Component parent, DashboardData d) {
        Panelchildren pch = createPanel("Aktivitas Repository Terbaru", parent);
        addExplanation(pch, "Item terbaru berdasarkan tanggal sinkronisasi atau perubahan data.");
        if (d.recentItems.isEmpty()) {
            addEmpty(pch, "Belum ada aktivitas yang tercatat.");
            return;
        }
        for (int i = 0; i < d.recentItems.size(); i++) {
            RecentItem r = d.recentItems.get(i);
            String statusColor = "SYNCED".equals(r.status) ? "#16a34a"
                    : "FAILED".equals(r.status)            ? "#b91c1c"
                    : "#94a3b8";
            addHtml(pch,
                    "<div style='padding:8px 0;border-bottom:1px solid #f1f5f9;"
                    + "display:flex;align-items:flex-start;gap:8px;'>"
                    + "<div style='flex:1;min-width:0;'>"
                    + "<div style='font-size:12px;font-weight:500;color:#1e293b;"
                    + "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>"
                    + esc(r.title) + "</div>"
                    + "<div style='font-size:11px;color:#94a3b8;margin-top:2px;'>"
                    + esc(r.source) + " · " + esc(r.time) + "</div>"
                    + "</div>"
                    + "<span style='font-size:10px;font-weight:600;padding:2px 7px;"
                    + "border-radius:999px;background:" + statusBg(r.status) + ";"
                    + "color:" + statusColor + ";white-space:nowrap;'>"
                    + esc(r.status) + "</span>"
                    + "</div>");
        }
    }

    private void renderInsight(Component parent, DashboardData d) {
        Panelchildren pch = createPanel("Rekomendasi Tindak Lanjut", parent);
        addExplanation(pch, "Saran operasional berdasarkan kondisi data repository saat ini.");
        org.zkoss.zul.Div wrap = div(pch,
                "display:flex;flex-wrap:wrap;gap:10px;margin:8px 0 12px 0;");
        insightCard(wrap, "Prioritas Metadata",
                d.totalItem - d.metadataComplete,
                "item perlu title, author, abstract, atau subject.",
                "#eff6ff", "#1e40af");
        insightCard(wrap, "Prioritas File",
                d.totalItem - d.withBitstream,
                "item belum punya bitstream/file.",
                "#fff7ed", "#9a3412");
        insightCard(wrap, "Prioritas DSpace",
                d.failedItem,
                "item gagal sinkron — periksa pesan error.",
                "#fef2f2", "#991b1b");
        insightCard(wrap, "Coverage Turnitin",
                d.totalItem - d.turnitinIndexed,
                "item belum masuk indeks Turnitin.",
                "#f5f3ff", "#5b21b6");

        MyToolbarbuttonConfig refreshBtn =
                new MyToolbarbuttonConfig("Refresh Dasbor", "/img/svg/refresh.svg");
        refreshBtn.setStyle("font-weight:bold;font-size:12px;margin-top:8px;");
        refreshBtn.setParent(pch);
        refreshBtn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Common.clear(DasboardRepository.this);
                startRender();
            }
        });
    }

    private void renderRadarChips(Component parent, DashboardData d) {
        Panelchildren pch = createPanel("Radar Kesiapan Repository", parent);
        addExplanation(pch, "Persentase kesiapan tiap dimensi terhadap total item aktif.");
        int total = Math.max(d.totalItem, 1);
        org.zkoss.zul.Div wrap = div(pch,
                "display:flex;flex-wrap:wrap;gap:10px;margin:10px 0;");
        radarChip(wrap, "Metadata",   pct(d.metadataComplete, total), "#2563eb");
        radarChip(wrap, "File",       pct(d.withBitstream,    total), "#d97706");
        radarChip(wrap, "DSpace",     pct(d.syncedItem,       total), "#16a34a");
        radarChip(wrap, "Turnitin",   pct(d.turnitinIndexed,  total), "#7c3aed");
        radarChip(wrap, "Open Access",pct(d.openAccess,       total), "#0891b2");
    }

    // -------------------------------------------------------------------------
    // Loading Indicator
    // -------------------------------------------------------------------------

    private void showLoading(String message, int percent) throws Exception {
        Common.clear(this);
        MyPortalchildren wrapper = portalChildren(this, "100%",
                "box-sizing:border-box;padding:8px;");
        Panel panel = new Panel();
        panel.setBorder("none");
        panel.setWidth("100%");
        panel.setStyle("width:100%;box-sizing:border-box;border:0;background:transparent;");
        panel.setParent(wrapper);
        Panelchildren body = new Panelchildren();
        body.setStyle("width:100%;box-sizing:border-box;padding:0;background:transparent;");
        body.setParent(panel);
        loadingHtml = new Html(buildLoadingHtml(message, percent));
        loadingHtml.setParent(body);
    }

    private void updateLoading(String message, int percent) {
        if (loadingHtml != null) {
            loadingHtml.setContent(buildLoadingHtml(message, percent));
        }
    }

    private String buildLoadingHtml(String message, int percent) {
        int p = Math.min(Math.max(percent, 0), 100);
        return "<div style='padding:32px;text-align:center;font-family:\"Segoe UI\",Arial,sans-serif;'>"
                + "<div style='font-size:16px;font-weight:700;color:#1e293b;margin-bottom:4px;'>"
                + "Memproses Dasbor Repository</div>"
                + "<div style='font-size:13px;color:#64748b;margin-bottom:16px;'>"
                + esc(message) + "</div>"
                + "<div style='width:100%;max-width:360px;margin:0 auto;"
                + "height:8px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>"
                + "<div style='height:8px;width:" + p + "%;background:#2563eb;"
                + "border-radius:999px;transition:width .4s;'></div></div>"
                + "<div style='font-size:12px;color:#94a3b8;margin-top:8px;'>" + p + "%</div>"
                + "</div>";
    }

    // -------------------------------------------------------------------------
    // Data Loading
    // -------------------------------------------------------------------------

    private DashboardData loadData() {
        DashboardData d = new DashboardData();
        Session session = null;
        try {
            session          = HibernateUtil.openSession();
            d.totalCollection = countActive(session, RepoCollection.class, null, null);
            d.totalItem       = countActive(session, RepoItem.class, null, null);
            d.totalBitstream  = countActive(session, RepoBitstream.class, null, null);
            d.syncedItem      = countActive(session, RepoItem.class, "syncStatus", "SYNCED");
            d.failedItem      = countActive(session, RepoItem.class, "syncStatus", "FAILED");
            d.turnitinIndexed = countActive(session, RepoItem.class, "turnitinIndexed", Boolean.TRUE);
            d.openAccess      = countActive(session, RepoItem.class, "accessPolicy", "OPEN_ACCESS");
            d.restrictedAccess= d.totalItem - d.openAccess;
            d.withTitle       = countNotBlank(session, "title",        "title");
            d.withAuthor      = countNotBlank(session, "authors",      "authors");
            d.withAbstract    = countNotBlank(session, "abstractText", "abstract_text");
            d.withSubject     = countNotBlank(session, "subjects",     "subjects");
            d.withIssuedDate  = countNotNull(session,  "issuedAt");
            d.metadataComplete= countMetadataComplete(session);
            d.withBitstream   = countItemsWithBitstream(session);
            d.withChecksum    = countBitstreamWithChecksum(session);
            loadGroupCounters(session, d);
            loadRecent(session, d);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSession(session);
        }
        return d;
    }

    private int countActive(Session session, Class<?> clazz,
            String property, Object value) {
        if (RepoBitstream.class.equals(clazz)) {
            String hql = "select count(b.id) from RepoBitstream b where (b.aktif is null or b.aktif=true) "
                    + "and b.itemId in (select i.id from RepoItem i where i.tenantKey=:tenant "
                    + "and (i.aktif is null or i.aktif=true))";
            Object result = session.createQuery(hql).setString("tenant", RepositoryTenantScope.currentKey()).uniqueResult();
            return result == null ? 0 : ((Number) result).intValue();
        }
        Criteria c = session.createCriteria(clazz)
                .add(Restrictions.or(
                        Restrictions.isNull("aktif"),
                        Restrictions.eq("aktif", Boolean.TRUE)));
        if (RepoItem.class.equals(clazz) || RepoCollection.class.equals(clazz))
            c.add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()));
        if (property != null) {
            c.add(value == null
                    ? Restrictions.isNull(property)
                    : Restrictions.eq(property, value));
        }
        Object r = c.setProjection(Projections.rowCount()).uniqueResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private int countNotBlank(Session session, String property, String column) {
        Criteria c = baseItemCriteria(session);
        c.add(Restrictions.isNotNull(property));
        c.add(Restrictions.sqlRestriction(column + " <> ''"));
        Object r = c.setProjection(Projections.rowCount()).uniqueResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private int countNotNull(Session session, String property) {
        Object r = baseItemCriteria(session)
                .add(Restrictions.isNotNull(property))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private int countMetadataComplete(Session session) {
        Criteria c = baseItemCriteria(session)
                .add(Restrictions.isNotNull("title"))
                .add(Restrictions.isNotNull("authors"))
                .add(Restrictions.isNotNull("abstractText"))
                .add(Restrictions.isNotNull("subjects"))
                .add(Restrictions.sqlRestriction(
                        "title <> '' AND authors <> '' "
                        + "AND abstract_text <> '' AND subjects <> ''"));
        Object r = c.setProjection(Projections.rowCount()).uniqueResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private int countItemsWithBitstream(Session session) {
        Object r = session.createSQLQuery(
                "SELECT count(DISTINCT b.item_id) FROM public.repo_bitstream b "
                + "JOIN public.repo_item i ON i.id=b.item_id "
                + "WHERE (b.aktif IS NULL OR b.aktif=true) "
                + "AND (i.aktif IS NULL OR i.aktif=true) AND i.tenant_key=:tenant")
                .setString("tenant", RepositoryTenantScope.currentKey())
                .uniqueResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private int countBitstreamWithChecksum(Session session) {
        Object r = session.createSQLQuery(
                "SELECT count(*) FROM public.repo_bitstream b JOIN public.repo_item i ON i.id=b.item_id "
                + "WHERE (b.aktif IS NULL OR b.aktif=true) AND (i.aktif IS NULL OR i.aktif=true) "
                + "AND i.tenant_key=:tenant AND b.checksum IS NOT NULL AND b.checksum <> ''")
                .setString("tenant", RepositoryTenantScope.currentKey())
                .uniqueResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private Criteria baseItemCriteria(Session session) {
        Criteria c = session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(Restrictions.or(
                        Restrictions.isNull("aktif"),
                        Restrictions.eq("aktif", Boolean.TRUE)));
        if (filterMulai != null) {
            c.add(Restrictions.ge("lastSyncAt", filterMulai));
        }
        if (filterSampai != null) {
            c.add(Restrictions.le("lastSyncAt", filterSampai));
        }
        if (filterKeyword != null && filterKeyword.trim().length() > 0) {
            String q = filterKeyword.trim();
            c.add(Restrictions.or(
                    Restrictions.ilike("title",       q, MatchMode.ANYWHERE),
                    Restrictions.or(
                        Restrictions.ilike("authors",     q, MatchMode.ANYWHERE),
                        Restrictions.or(
                            Restrictions.ilike("subjects",    q, MatchMode.ANYWHERE),
                            Restrictions.ilike("sourceLabel", q, MatchMode.ANYWHERE)))));
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private void loadGroupCounters(Session session, DashboardData d) {
        // per-collection: SQL untuk join
        List<Object[]> colRows = session.createSQLQuery(
                "SELECT coalesce(c.nama,'Tanpa Collection'), count(i.id) "
                + "FROM public.repo_item i "
                + "LEFT JOIN public.repo_collection c ON i.collection_id = c.id "
                + "WHERE (i.aktif IS NULL OR i.aktif = true) AND i.tenant_key=:tenant "
                + "GROUP BY coalesce(c.nama,'Tanpa Collection') "
                + "ORDER BY count(i.id) DESC LIMIT 10")
                .setString("tenant", RepositoryTenantScope.currentKey())
                .list();
        fillMap(d.perCollection, colRows);

        // per-source
        List<Object[]> srcRows = session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(Restrictions.or(
                        Restrictions.isNull("aktif"),
                        Restrictions.eq("aktif", Boolean.TRUE)))
                .setProjection(Projections.projectionList()
                        .add(Projections.groupProperty("sourceLabel"))
                        .add(Projections.rowCount()))
                .list();
        fillMap(d.perSource, srcRows);

        // per-document-type
        List<Object[]> typeRows = session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("tenantKey", RepositoryTenantScope.currentKey()))
                .add(Restrictions.or(
                        Restrictions.isNull("aktif"),
                        Restrictions.eq("aktif", Boolean.TRUE)))
                .setProjection(Projections.projectionList()
                        .add(Projections.groupProperty("documentType"))
                        .add(Projections.rowCount()))
                .list();
        fillMap(d.perDocumentType, typeRows);
    }

    private void fillMap(Map<String, Integer> target, List<Object[]> rows) {
        if (rows == null) return;
        for (int i = 0; i < rows.size(); i++) {
            Object[] row   = rows.get(i);
            String   label = row[0] == null ? "Tidak Diisi" : row[0].toString();
            int      count = row[1] == null ? 0 : ((Number) row[1]).intValue();
            target.put(label, Integer.valueOf(count));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRecent(Session session, DashboardData d) {
        List<RepoItem> items = baseItemCriteria(session)
                .addOrder(Order.desc("lastSyncAt"))
                .addOrder(Order.desc("id"))
                .setMaxResults(RECENT_ITEM_LIMIT)
                .list();
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            RepoItem  item = items.get(i);
            RecentItem r   = new RecentItem();
            r.title  = item.getTitle();
            r.source = item.getSourceLabel();
            r.status = item.getSyncStatus();
            r.time   = item.getLastSyncAt() == null
                    ? "-"
                    : Common.dateFormat51.get().format(item.getLastSyncAt());
            d.recentItems.add(r);
        }
    }

    // -------------------------------------------------------------------------
    // HTML Component Helpers (reusable)
    // -------------------------------------------------------------------------

    private void funnelRow(Component parent, String label, int value, int max, String color) {
        int p = max <= 0 ? 0 : (int) Math.round(value * 100.0 / max);
        if (p < 4 && value > 0) p = 4;
        addHtml(parent,
                "<div style='display:flex;align-items:center;gap:8px;margin:6px 0;"
                + "font-size:12px;'>"
                + "<div style='width:130px;min-width:130px;color:#374151;'>" + esc(label) + "</div>"
                + "<div style='flex:1;height:8px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>"
                + "<div style='height:8px;width:" + p + "%;background:" + color + ";"
                + "border-radius:999px;transition:width .5s;'></div></div>"
                + "<div style='width:36px;text-align:right;font-weight:600;color:#1e293b;'>"
                + value + "</div>"
                + "</div>");
    }

    private void radarChip(Component parent, String label, int percent, String color) {
        addHtml(parent,
                "<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;"
                + "padding:10px 14px;min-width:100px;box-sizing:border-box;text-align:center;"
                + "box-shadow:0 2px 8px rgba(15,23,42,.06);'>"
                + "<div style='font-size:22px;font-weight:700;color:" + color + ";'>" + percent + "%</div>"
                + "<div style='font-size:11px;color:#64748b;margin-top:3px;'>" + esc(label) + "</div>"
                + "<div style='margin-top:6px;height:4px;background:#e5e7eb;border-radius:999px;"
                + "overflow:hidden;'>"
                + "<div style='height:4px;width:" + percent + "%;background:" + color + ";"
                + "border-radius:999px;'></div></div>"
                + "</div>");
    }

    private void insightCard(Component parent, String title, int value,
            String desc, String bg, String color) {
        int v = Math.max(value, 0);
        addHtml(parent,
                "<div style='flex:1 1 160px;min-width:160px;max-width:240px;"
                + "background:#ffffff;border:1px solid #e2e8f0;"
                + "border-left:4px solid " + color + ";"
                + "border-radius:10px;padding:12px 14px;"
                + "box-shadow:0 4px 12px rgba(15,23,42,.06);box-sizing:border-box;'>"
                + "<div style='font-size:11px;font-weight:600;color:" + color + ";"
                + "text-transform:uppercase;letter-spacing:.05em;margin-bottom:6px;'>"
                + esc(title) + "</div>"
                + "<div style='font-size:28px;font-weight:700;color:#111827;line-height:1;'>"
                + v + "</div>"
                + "<div style='font-size:11px;color:#94a3b8;margin-top:4px;line-height:1.5;'>"
                + esc(desc) + "</div>"
                + "</div>");
    }

    // -------------------------------------------------------------------------
    // ZK Component Factory Helpers
    // -------------------------------------------------------------------------

    private MyPortalchildren portalChildren(Component parent, String width, String extraStyle) {
        MyPortalchildren pc = new MyPortalchildren();
        pc.setWidth(width);
        pc.setStyle("width:100%;max-width:100%;box-sizing:border-box;" + extraStyle);
        pc.setParent(parent);
        return pc;
    }

    private Panel styledPanel(String title, Component parent, String style) {
        Panel panel = new MyPanelConfig();
        panel.setTitle(title);
        panel.setBorder("none");
        panel.setCollapsible(false);
        panel.setClosable(false);
        panel.setMaximizable(false);
        panel.setMinimizable(false);
        panel.setStyle(style + "background:#ffffff;");
        panel.setParent(parent);
        return panel;
    }

    private Panelchildren styledPanelChildren(Panel panel, String style) {
        Panelchildren pc = new Panelchildren();
        pc.setStyle(style);
        pc.setParent(panel);
        return pc;
    }

    /** Creates a modern card panel and returns its Panelchildren content area. */
    private Panelchildren createPanel(String title, Component parent) {
        Panel panel = styledPanel(title, parent,
                "width:100%;box-sizing:border-box;margin:0 0 12px 0;");
        return styledPanelChildren(panel,
                "width:100%;box-sizing:border-box;padding:12px;background:#ffffff;");
    }

    private org.zkoss.zul.Div div(Component parent, String style) {
        org.zkoss.zul.Div d = new org.zkoss.zul.Div();
        d.setStyle(style);
        d.setParent(parent);
        return d;
    }

    private void addHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private void addExplanation(Component parent, String text) {
        addHtml(parent,
                "<div style='font-size:11px;color:#94a3b8;margin-bottom:10px;line-height:1.5;'>"
                + esc(text) + "</div>");
    }

    private void addEmpty(Component parent, String text) {
        addHtml(parent,
                "<div style='font-size:12px;color:#94a3b8;padding:14px;text-align:center;'>"
                + esc(text) + "</div>");
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private int pct(int value, int total) {
        return total <= 0 ? 0 : (int) Math.round(value * 100.0 / total);
    }

    private int maxOf(int... values) {
        int m = 0;
        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i] > m) m = values[i];
        }
        return m;
    }

    private List<LabelCount> sortedDesc(Map<String, Integer> map) {
        List<LabelCount> list = new ArrayList<LabelCount>();
        for (String key : map.keySet()) {
            LabelCount lc = new LabelCount();
            lc.label = key;
            lc.count = map.get(key) == null ? 0 : map.get(key).intValue();
            list.add(lc);
        }
        Collections.sort(list, new Comparator<LabelCount>() {
            @Override
            public int compare(LabelCount a, LabelCount b) {
                return b.count - a.count;
            }
        });
        return list;
    }

    private String statusBg(String status) {
        if ("SYNCED".equals(status)) return "#dcfce7";
        if ("FAILED".equals(status)) return "#fee2e2";
        return "#f1f5f9";
    }

    private void closeSession(Session session) {
        if (session == null) return;
        try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/repository/DasboardRepository.java:772");}
        try { session.close();      } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/repository/DasboardRepository.java:773");}
    }

    // -------------------------------------------------------------------------
    // Inner Data Classes
    // -------------------------------------------------------------------------

    private static class DashboardData {
        int totalCollection;
        int totalItem;
        int totalBitstream;
        int syncedItem;
        int failedItem;
        int turnitinIndexed;
        int openAccess;
        int restrictedAccess;
        int withTitle;
        int withAuthor;
        int withAbstract;
        int withSubject;
        int withIssuedDate;
        int metadataComplete;
        int withBitstream;
        int withChecksum;
        Map<String, Integer> perCollection   = new HashMap<String, Integer>();
        Map<String, Integer> perSource        = new HashMap<String, Integer>();
        Map<String, Integer> perDocumentType  = new HashMap<String, Integer>();
        List<RecentItem>     recentItems      = new ArrayList<RecentItem>();
    }

    private static class RecentItem {
        String title;
        String source;
        String status;
        String time;
    }

    private static class LabelCount {
        String label;
        int    count;
    }
}
