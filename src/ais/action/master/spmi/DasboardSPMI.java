package ais.action.master.spmi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.spmi.HasilSPMI;
import ais.database.model.spmi.HasilTemuanSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.database.model.spmi.TindakLanjutTemuanSPMI;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor AMI SPMI — tampilan ringkasan lengkap siklus PPEPP:
 * status pengajuan, zona kepatuhan (Hijau/Kuning/Merah), distribusi
 * temuan, tindak lanjut, radar standar, tren KTS, dan rekomendasi.
 *
 * UI/UX: responsive flexbox, mobile-friendly, color-zone system,
 * PPEPP cycle indicator, tindak lanjut progress tracking.
 */
public class DasboardSPMI extends Div {

    private static final long serialVersionUID = 2L;

    // Zone thresholds (sesuai best practice LLDIKTI / BAN-PT)
    private static final int ZONA_HIJAU_MIN  = 70;  // >= 70% = Hijau
    private static final int ZONA_KUNING_MIN = 50;  // 50–70% = Kuning

    // ---- filter state ----
    private String    filterTa       = Common.getCurrentTahunAkademik();
    private String    filterSemester = "";
    private JenisSPMI filterJenis    = null;

    private Combobox cbTa;
    private Combobox cbSemester;
    private Combobox cbJenis;

    // ----------------------------------------------------------------
    // Data container
    // ----------------------------------------------------------------

    private static class SpmiData {
        // AMI status
        int totalAmi, disetujui, menunggu, ditolak;

        // Temuan
        int totalTemuan, jmlO, jmlKtsMyr, jmlKtsMnr, jmlS, jmlLs, jmlBelum;

        // Coverage
        int totalSkenario, skenarioTeraudit;

        // Tindak lanjut (fase Pengendalian PPEPP)
        int tlTotal, tlSelesai, tlBerjalan, tlTerlambat;

        // Standar aktif (untuk PPEPP fase Penetapan)
        int standarAktif;

        // Charts
        List<Object[]> perJenis      = new ArrayList<Object[]>();
        List<Object[]> perProdi      = new ArrayList<Object[]>();
        List<Object[]> standarMatrix = new ArrayList<Object[]>();
        List<Object[]> trendKts      = new ArrayList<Object[]>();
        List<Object[]> topKts        = new ArrayList<Object[]>();
        List<HasilSPMI> recent       = new ArrayList<HasilSPMI>();

        // Derived
        int healthPct() {
            return totalTemuan == 0 ? 0 : (jmlS + jmlLs) * 100 / totalTemuan;
        }
        int covPct() {
            return totalSkenario == 0 ? 0 : skenarioTeraudit * 100 / totalSkenario;
        }
        int ktsPct() {
            return totalTemuan == 0 ? 0 : (jmlKtsMyr + jmlKtsMnr) * 100 / totalTemuan;
        }
        String zona() {
            int h = healthPct();
            return h >= ZONA_HIJAU_MIN ? "HIJAU" : (h >= ZONA_KUNING_MIN ? "KUNING" : "MERAH");
        }
    }

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public DasboardSPMI() {
        setWidth("100%");
        setStyle("min-height:300px; background:#f1f5f9; padding:14px 16px;"
               + " box-sizing:border-box; overflow:auto;");
        try {
            tampilLoading();
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    renderAll();
                }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ----------------------------------------------------------------
    // Loading skeleton
    // ----------------------------------------------------------------

    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:80px 0; text-align:center;'>"
            + "<div style='font-size:40px; margin-bottom:16px; animation:spin 1.5s linear infinite;'>&#9203;</div>"
            + "<div style='font-size:15px; font-weight:800; color:#334155;'>Memuat Dasbor AMI SPMI&#8230;</div>"
            + "<div style='margin-top:8px; font-size:12px; color:#94a3b8;'>"
            + "Menghitung data audit, temuan, tindak lanjut, dan zona kepatuhan.</div>"
            + "</div>"
            + "<style>@keyframes spin{to{transform:rotate(360deg)}}</style>");
    }

    // ----------------------------------------------------------------
    // Main render
    // ----------------------------------------------------------------

    private void renderAll() {
        try {
            SpmiData d = loadDataWithCache();
            Common.clear(this);

            // 1. Hero + PPEPP cycle
            renderHero(d);
            renderPpepp(d);

            // 2. Filter bar
            renderFilter();

            // 3. KPI cards (2 rows)
            renderPengajuanCards(d);
            renderTemuanCards(d);

            // 4. Zona + Health gauge + TL cards  (tiga panel sejajar)
            Div row4 = flexRow();
            renderZonaPanel(colDiv(row4, "flex:0 1 200px; min-width:160px;"), d);
            renderDonutHealth(colDiv(row4, "flex:1 1 220px; min-width:180px;"), d);
            renderTlSummary(colDiv(row4, "flex:1 1 240px; min-width:180px;"), d);

            // 5. Per Standar dengan zona warna
            renderPerStandar(this, d);

            // 6. Trend KTS + Top KTS
            Div row6 = flexRow();
            renderTrendKts(colDiv(row6, "flex:1 1 300px;"), d);
            renderTopKts(colDiv(row6, "flex:1 1 300px;"), d);

            // 7. Distribusi + Per Jenis + Per Prodi
            Div row7 = flexRow();
            renderDistribusi(colDiv(row7, "flex:2 1 280px;"), d);
            renderPerJenis(colDiv(row7, "flex:1 1 220px;"), d);
            renderPerProdi(colDiv(row7, "flex:1 1 220px;"), d);

            // 8. Recent AMI table
            renderRecent(this, d);

            // 9. Action plan / rekomendasi
            renderActionPlan(this, d);

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ----------------------------------------------------------------
    // Data queries
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private SpmiData loadDataWithCache() {
        String fp = (filterTa != null ? filterTa : "all")
                  + "_" + (filterSemester != null && !filterSemester.isEmpty() ? filterSemester : "all");
        String key = DashboardCacheUtil.keyWithFilter("DasboardSPMI", "ADMIN", null, fp);
        Object fromL2 = DashboardCacheUtil.getL2(key);
        if (fromL2 instanceof SpmiData) return (SpmiData) fromL2;
        Object fromL3 = DashboardCacheUtil.getL3(key);
        if (fromL3 instanceof SpmiData) {
            DashboardCacheUtil.putL2(key, fromL3);
            return (SpmiData) fromL3;
        }
        SpmiData d = loadData();
        DashboardCacheUtil.putL2(key, d);
        DashboardCacheUtil.putL3(key, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private SpmiData loadData() {
        SpmiData d   = new SpmiData();
        Session  sess = HibernateUtil.currentSession();

        // AMI status
        d.totalAmi  = count(buildHC());
        d.disetujui = count(buildHC().add(Restrictions.eq("status", HasilSPMI.DISETUJU)));
        d.menunggu  = count(buildHC().add(Restrictions.eq("status", HasilSPMI.PENGAJUAN)));
        d.ditolak   = count(buildHC().add(Restrictions.eq("status", HasilSPMI.DITOLAK)));

        // Temuan
        d.totalTemuan = count(buildTC());
        d.jmlKtsMyr   = count(buildTC().add(Restrictions.eq("status", HasilTemuanSPMI.KTS_MYR1)));
        d.jmlKtsMnr   = count(buildTC().add(Restrictions.eq("status", HasilTemuanSPMI.KTS_MNR1)));
        d.jmlO        = count(buildTC().add(Restrictions.eq("status", HasilTemuanSPMI.O1)));
        d.jmlS        = count(buildTC().add(Restrictions.eq("status", HasilTemuanSPMI.S1)));
        d.jmlLs       = count(buildTC().add(Restrictions.eq("status", HasilTemuanSPMI.LS1)));
        d.jmlBelum    = count(buildTC().add(Restrictions.isNull("status")));

        // Coverage
        d.totalSkenario = count(sess.createCriteria(SkenarioSPMI.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))));
        Number nc = (Number) buildTC()
            .setProjection(Projections.countDistinct("skenarioSPMI")).uniqueResult();
        d.skenarioTeraudit = nc == null ? 0 : nc.intValue();

        // Standar aktif (fase Penetapan)
        d.standarAktif = count(sess.createCriteria(StandarSPMI.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))));

        // Tindak lanjut (fase Pengendalian)
        try {
            Criteria ctlBase = sess.createCriteria(TindakLanjutTemuanSPMI.class)
                .createAlias("hasilTemuanSPMI", "ht")
                .createAlias("ht.hasilSPMI", "hs")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
            addHsAliasFilterDirect(ctlBase, "hs");

            d.tlTotal      = count(ctlBase);
            d.tlSelesai    = count(sess.createCriteria(TindakLanjutTemuanSPMI.class)
                .createAlias("hasilTemuanSPMI", "ht").createAlias("ht.hasilSPMI", "hs")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .add(Restrictions.eq("status", TindakLanjutTemuanSPMI.SELESAI)));
            d.tlBerjalan   = count(sess.createCriteria(TindakLanjutTemuanSPMI.class)
                .createAlias("hasilTemuanSPMI", "ht").createAlias("ht.hasilSPMI", "hs")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .add(Restrictions.eq("status", TindakLanjutTemuanSPMI.SEDANG_BERJALAN)));
            // TERLAMBAT = belum selesai dan melewati target_date (dihitung dari DB, bukan status kolom)
            d.tlTerlambat  = count(sess.createCriteria(TindakLanjutTemuanSPMI.class)
                .createAlias("hasilTemuanSPMI", "ht").createAlias("ht.hasilSPMI", "hs")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .add(Restrictions.ne("status", TindakLanjutTemuanSPMI.SELESAI))
                .add(Restrictions.isNotNull("targetDate"))
                .add(Restrictions.lt("targetDate", new Date())));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/spmi/DasboardSPMI.java:267");}

        // Per Jenis
        try {
            List<Object[]> pj = (List<Object[]>) buildHC()
                .createAlias("jenisSPMI", "jns", Criteria.LEFT_JOIN)
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("jns.nama"))
                    .add(Projections.rowCount()))
                .setMaxResults(15).list();
            sortByCountDesc(pj);
            d.perJenis = pj;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/spmi/DasboardSPMI.java:279");}

        // Per Prodi
        try {
            List<Object[]> pp = (List<Object[]>) buildHC()
                .createAlias("jurusan", "jrs", Criteria.LEFT_JOIN)
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("jrs.nama"))
                    .add(Projections.rowCount()))
                .setMaxResults(15).list();
            sortByCountDesc(pp);
            d.perProdi = pp;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/spmi/DasboardSPMI.java:291");}

        // Standar matrix
        try {
            Criteria cStd = sess.createCriteria(HasilTemuanSPMI.class)
                .createAlias("hasilSPMI", "hs")
                .createAlias("skenarioSPMI", "sk")
                .createAlias("sk.indikatorSPMI", "ind")
                .createAlias("ind.butirMutuSPMI", "bm")
                .createAlias("bm.standarSPMI", "std", Criteria.LEFT_JOIN);
            addHsAliasFilter(cStd);
            d.standarMatrix = (List<Object[]>) cStd
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("std.nama"))
                    .add(Projections.groupProperty("status"))
                    .add(Projections.rowCount()))
                .setMaxResults(500).list();
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/spmi/DasboardSPMI.java:308");}

        // Trend KTS (semua TA agar tren terlihat)
        try {
            Criteria cTrend = sess.createCriteria(HasilTemuanSPMI.class)
                .createAlias("hasilSPMI", "hs")
                .add(Restrictions.or(
                    Restrictions.eq("status", HasilTemuanSPMI.KTS_MYR1),
                    Restrictions.eq("status", HasilTemuanSPMI.KTS_MNR1)));
            if (filterSemester != null && !filterSemester.isEmpty())
                cTrend.add(Restrictions.eq("hs.semester", filterSemester));
            if (filterJenis != null)
                cTrend.add(Restrictions.eq("hs.jenisSPMI", filterJenis));
            List<Object[]> trend = (List<Object[]>) cTrend
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("hs.ta"))
                    .add(Projections.rowCount()))
                .setMaxResults(20).list();
            Collections.sort(trend, new Comparator<Object[]>() {
                public int compare(Object[] a, Object[] b) {
                    return safeStr(a[0]).compareTo(safeStr(b[0]));
                }
            });
            d.trendKts = trend;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/spmi/DasboardSPMI.java:332");}

        // Top worst skenario
        try {
            Criteria cTop = sess.createCriteria(HasilTemuanSPMI.class)
                .createAlias("hasilSPMI", "hs")
                .createAlias("skenarioSPMI", "sk", Criteria.LEFT_JOIN)
                .add(Restrictions.or(
                    Restrictions.eq("status", HasilTemuanSPMI.KTS_MYR1),
                    Restrictions.eq("status", HasilTemuanSPMI.KTS_MNR1)));
            addHsAliasFilter(cTop);
            List<Object[]> topList = (List<Object[]>) cTop
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("sk.nama"))
                    .add(Projections.rowCount()))
                .setMaxResults(30).list();
            sortByCountDesc(topList);
            d.topKts = topList.size() > 8 ? topList.subList(0, 8) : topList;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/spmi/DasboardSPMI.java:350");}

        // Recent AMI
        d.recent = (List<HasilSPMI>) buildHC()
            .addOrder(Order.desc("id")).setMaxResults(10).list();

        return d;
    }

    // ----------------------------------------------------------------
    // Helpers: criteria builders
    // ----------------------------------------------------------------

    private int count(Criteria c) {
        Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
        return n == null ? 0 : n.intValue();
    }

    private void sortByCountDesc(List<Object[]> list) {
        Collections.sort(list, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return ((Number) b[1]).intValue() - ((Number) a[1]).intValue();
            }
        });
    }

    private Criteria buildHC() {
        Session sess = HibernateUtil.currentSession();
        Criteria c = sess.createCriteria(HasilSPMI.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        applyHasilFilter(c);
        return c;
    }

    private Criteria buildTC() {
        Session sess = HibernateUtil.currentSession();
        Criteria c = sess.createCriteria(HasilTemuanSPMI.class)
            .createAlias("hasilSPMI", "hs");
        addHsAliasFilter(c);
        return c;
    }

    private void applyHasilFilter(Criteria c) {
        if (filterTa       != null && !filterTa.isEmpty())       c.add(Restrictions.eq("ta",       filterTa));
        if (filterSemester != null && !filterSemester.isEmpty()) c.add(Restrictions.eq("semester", filterSemester));
        if (filterJenis    != null)                              c.add(Restrictions.eq("jenisSPMI", filterJenis));
    }

    private void addHsAliasFilter(Criteria c) {
        if (filterTa       != null && !filterTa.isEmpty())       c.add(Restrictions.eq("hs.ta",       filterTa));
        if (filterSemester != null && !filterSemester.isEmpty()) c.add(Restrictions.eq("hs.semester", filterSemester));
        if (filterJenis    != null)                              c.add(Restrictions.eq("hs.jenisSPMI", filterJenis));
    }

    private void addHsAliasFilterDirect(Criteria c, String alias) {
        if (filterTa       != null && !filterTa.isEmpty())       c.add(Restrictions.eq(alias + ".ta",       filterTa));
        if (filterSemester != null && !filterSemester.isEmpty()) c.add(Restrictions.eq(alias + ".semester", filterSemester));
        if (filterJenis    != null)                              c.add(Restrictions.eq(alias + ".jenisSPMI", filterJenis));
    }

    private Map<String, int[]> buildStandarAgg(List<Object[]> matrix) {
        Map<String, int[]> agg = new LinkedHashMap<String, int[]>();
        for (Object[] row : matrix) {
            String std = row[0] == null ? "(Tanpa Standar)" : row[0].toString();
            String st  = row[1] == null ? "" : row[1].toString();
            int    cnt = ((Number) row[2]).intValue();
            if (!agg.containsKey(std)) agg.put(std, new int[6]);
            int[] arr = agg.get(std);
            arr[0] += cnt;
            if (HasilTemuanSPMI.S1.equals(st))            arr[1] += cnt;
            else if (HasilTemuanSPMI.LS1.equals(st))      arr[2] += cnt;
            else if (HasilTemuanSPMI.O1.equals(st))       arr[3] += cnt;
            else if (HasilTemuanSPMI.KTS_MNR1.equals(st)) arr[4] += cnt;
            else if (HasilTemuanSPMI.KTS_MYR1.equals(st)) arr[5] += cnt;
        }
        return agg;
    }

    // ================================================================
    // RENDER SECTIONS
    // ================================================================

    // ----------------------------------------------------------------
    // Hero banner
    // ----------------------------------------------------------------

    private void renderHero(SpmiData d) {
        int pct    = d.totalAmi == 0 ? 0 : d.disetujui * 100 / d.totalAmi;
        String zona = d.zona();
        String zClr = "HIJAU".equals(zona) ? "#22c55e" : ("MERAH".equals(zona) ? "#ef4444" : "#f59e0b");

        appendHtml(this,
            "<div style='border-radius:20px; padding:22px 26px; margin-bottom:14px;"
            + " background:linear-gradient(135deg,rgba(0,0,0,.22) 0%,rgba(0,0,0,0) 55%),"
            + "linear-gradient(135deg,#1e3a8a 0%,#1d4ed8 60%,#0ea5e9 100%);"
            + " color:#fff; box-shadow:0 10px 30px rgba(30,58,138,.28);'>"
            + "<div style='display:flex; flex-wrap:wrap; gap:14px; align-items:center;'>"
            + "<div style='flex:1; min-width:180px;'>"
            + "<div style='font-size:11px; font-weight:700; opacity:.75; letter-spacing:.1em;"
            + " text-transform:uppercase; margin-bottom:5px;'>Sistem Penjaminan Mutu Internal</div>"
            + "<div style='font-size:21px; font-weight:900; letter-spacing:-.03em; line-height:1.2;'>"
            + "Dasbor Audit Mutu Internal (AMI)</div>"
            + "<div style='font-size:12px; opacity:.8; margin-top:6px; line-height:1.5;'>"
            + "Siklus PPEPP — Pantau kepatuhan standar mutu dalam satu layar.</div>"
            + "<div style='margin-top:12px; display:flex; gap:7px; flex-wrap:wrap;'>"
            + heroBadge(d.totalAmi + " Pengajuan AMI")
            + heroBadge(d.disetujui + " Disetujui (" + pct + "%)")
            + heroBadge(filterTa != null && !filterTa.isEmpty() ? "TA " + esc(filterTa) : "Semua TA")
            + (d.jmlKtsMyr > 0
                ? "<span style='display:inline-block; border-radius:999px; padding:4px 12px;"
                  + " font-size:11px; font-weight:700; background:rgba(239,68,68,.35);"
                  + " border:1px solid rgba(239,68,68,.6);'>"
                  + d.jmlKtsMyr + " KTS Mayor</span>"
                : heroBadge("Tidak Ada KTS Mayor"))
            + "</div></div>"
            // Zona indicator di kanan
            + "<div style='flex:0 0 auto; text-align:center; background:rgba(255,255,255,.12);"
            + " border-radius:16px; padding:16px 20px; min-width:120px;'>"
            + "<div style='font-size:10px; font-weight:700; opacity:.7; letter-spacing:.1em;'>ZONA MUTU</div>"
            + "<div style='font-size:28px; font-weight:900; color:" + zClr + "; margin:6px 0; line-height:1;'>"
            + zona + "</div>"
            + "<div style='font-size:11px; opacity:.75;'>" + d.healthPct() + "% Patuh</div>"
            + "</div>"
            + "</div></div>");
    }

    private String heroBadge(String text) {
        return "<span style='display:inline-block; border-radius:999px; padding:4px 12px;"
               + " font-size:11px; font-weight:700; background:rgba(255,255,255,.18);'>"
               + esc(text) + "</span>";
    }

    // ----------------------------------------------------------------
    // PPEPP Cycle Indicator (BARU)
    // ----------------------------------------------------------------

    private void renderPpepp(SpmiData d) {
        // Tentukan status tiap fase
        boolean p1 = d.standarAktif > 0;               // Penetapan: ada standar aktif
        boolean p2 = d.disetujui > 0;                  // Pelaksanaan: ada AMI disetujui
        boolean e1 = d.totalTemuan > 0;                // Evaluasi: ada temuan tercatat
        boolean p4 = d.tlTotal > 0;                    // Pengendalian: ada tindak lanjut
        boolean p5 = d.trendKts.size() >= 2;           // Peningkatan: ada tren (min 2 TA)

        int tlPct = d.tlTotal == 0 ? 0 : d.tlSelesai * 100 / d.tlTotal;

        appendHtml(this,
            "<div style='background:#fff; border-radius:18px; padding:16px 20px;"
            + " box-shadow:0 2px 12px rgba(0,0,0,.06); margin-bottom:14px;'>"
            + "<div style='font-size:12px; font-weight:800; color:#0f172a; margin-bottom:12px;"
            + " padding-bottom:8px; border-bottom:2px solid #f1f5f9;'>"
            + "&#9654; Status Siklus PPEPP — Periode Saat Ini</div>"
            + "<div style='display:flex; gap:8px; flex-wrap:wrap;'>"
            + ppeppStep("P", "Penetapan",    "Standar mutu ditetapkan",
                p1 ? "Standar aktif: " + d.standarAktif : "Belum ada standar aktif",
                p1 ? "done" : "miss")
            + ppeppStep("P", "Pelaksanaan",  "AMI dilaksanakan",
                p2 ? d.disetujui + " AMI disetujui" : "Belum ada AMI disetujui",
                p2 ? "done" : (d.menunggu > 0 ? "partial" : "miss"))
            + ppeppStep("E", "Evaluasi",     "Temuan tercatat",
                e1 ? d.totalTemuan + " temuan (" + (d.jmlKtsMyr + d.jmlKtsMnr) + " KTS)"
                   : "Belum ada temuan",
                e1 ? "done" : "miss")
            + ppeppStep("P", "Pengendalian", "Tindak lanjut temuan",
                d.tlTotal > 0
                    ? d.tlSelesai + "/" + d.tlTotal + " selesai (" + tlPct + "%)"
                    : "Belum ada tindak lanjut",
                d.tlTotal == 0 ? "miss" : (tlPct >= 100 ? "done" : (d.tlTerlambat > 0 ? "warn" : "partial")))
            + ppeppStep("P", "Peningkatan",  "Tren KTS membaik",
                p5 ? "Data tren tersedia (" + d.trendKts.size() + " TA)" : "Data historis belum cukup",
                p5 ? (isTrendDecreasing(d.trendKts) ? "done" : "warn") : "miss")
            + "</div></div>");
    }

    private String ppeppStep(String code, String title, String sub, String detail, String state) {
        String bg   = "done".equals(state)    ? "#dcfce7"
                    : "partial".equals(state) ? "#dbeafe"
                    : "warn".equals(state)    ? "#fef9c3"
                    : "#fee2e2";
        String clr  = "done".equals(state)    ? "#166534"
                    : "partial".equals(state) ? "#1e40af"
                    : "warn".equals(state)    ? "#854d0e"
                    : "#991b1b";
        String icon = "done".equals(state)    ? "&#10003;"
                    : "partial".equals(state) ? "&#9654;"
                    : "warn".equals(state)    ? "&#9888;"
                    : "&#9711;";
        return "<div style='flex:1; min-width:110px; border-radius:14px; padding:12px 14px;"
             + " background:" + bg + "; border:1.5px solid " + clr + "44;'>"
             + "<div style='display:flex; gap:8px; align-items:center; margin-bottom:6px;'>"
             + "<span style='width:28px; height:28px; border-radius:50%; background:" + clr
             + "; color:#fff; display:flex; align-items:center; justify-content:center;"
             + " font-size:11px; font-weight:900; flex-shrink:0;'>" + icon + "</span>"
             + "<div><div style='font-size:9px; font-weight:800; color:" + clr + "; text-transform:uppercase;"
             + " letter-spacing:.06em;'>" + code + " — " + esc(title) + "</div>"
             + "<div style='font-size:9px; color:" + clr + "; opacity:.8;'>" + esc(sub) + "</div></div>"
             + "</div>"
             + "<div style='font-size:10px; color:" + clr + "; opacity:.9; line-height:1.4;'>"
             + esc(detail) + "</div>"
             + "</div>";
    }

    private boolean isTrendDecreasing(List<Object[]> trend) {
        if (trend.size() < 2) return false;
        int first = ((Number) trend.get(0)[1]).intValue();
        int last  = ((Number) trend.get(trend.size() - 1)[1]).intValue();
        return last < first;
    }

    // ----------------------------------------------------------------
    // Filter bar
    // ----------------------------------------------------------------

    private void renderFilter() {
        Div bar = new Div();
        bar.setStyle("background:#fff; border-radius:14px; padding:12px 16px;"
                   + " box-shadow:0 2px 8px rgba(0,0,0,.06); margin-bottom:14px;"
                   + " display:flex; align-items:center; gap:10px; flex-wrap:wrap;");
        bar.setParent(this);

        appendHtml(bar, "<span style='font-size:12px; font-weight:700; color:#475569;'>Filter:</span>");

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Tahun Akademik</span>");
        cbTa = new Combobox();
        cbTa.setWidth("150px");
        cbTa.setReadonly(true);
        cbTa.setParent(bar);
        Common.generateTahunAjaranDanSemua(cbTa);
        if (filterTa != null && !filterTa.isEmpty()) {
            Common.selectComboItem(cbTa, filterTa);
        }
        if (cbTa.getSelectedItem() == null) cbTa.setSelectedIndex(0);

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Semester</span>");
        cbSemester = new Combobox();
        cbSemester.setWidth("100px");
        cbSemester.setReadonly(true);
        cbSemester.setParent(bar);
        for (String[] opt : new String[][]{{"", "Semua"}, {"Ganjil", "Ganjil"}, {"Genap", "Genap"}}) {
            Comboitem ci = new Comboitem(opt[1]);
            ci.setValue(opt[0]);
            cbSemester.appendChild(ci);
        }
        cbSemester.setSelectedIndex("Ganjil".equals(filterSemester) ? 1 : ("Genap".equals(filterSemester) ? 2 : 0));

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Jenis SPMI</span>");
        cbJenis = new Combobox();
        cbJenis.setWidth("200px");
        cbJenis.setReadonly(true);
        cbJenis.setParent(bar);
        Common.insertComboDanSemua(cbJenis, "nama", "keterangan",
            JenisSPMI.class, Restrictions.eq("aktif", true));
        if (filterJenis != null) Common.selectComboItem(true, cbJenis, filterJenis);
        else cbJenis.setSelectedIndex(0);

        MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
        btn.setParent(bar);
        btn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                readFilter();
                tampilLoading();
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event e2) throws Exception { renderAll(); }
                });
            }
        });
    }

    private void readFilter() {
        if (cbTa != null && cbTa.getSelectedItem() != null) {
            Object v = cbTa.getSelectedItem().getValue();
            filterTa = v == null ? "" : v.toString();
        }
        if (cbSemester != null && cbSemester.getSelectedItem() != null) {
            Object v = cbSemester.getSelectedItem().getValue();
            filterSemester = v == null ? "" : v.toString();
        }
        if (cbJenis != null && cbJenis.getSelectedItem() != null) {
            Object v = cbJenis.getSelectedItem().getValue();
            filterJenis = (v instanceof JenisSPMI) ? (JenisSPMI) v : null;
        }
    }

    // ----------------------------------------------------------------
    // KPI cards: Pengajuan AMI
    // ----------------------------------------------------------------

    private void renderPengajuanCards(SpmiData d) {
        Div row = new Div();
        row.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;");
        row.setParent(this);
        metricCard(row, "Total Pengajuan AMI", d.totalAmi,  "Semua AMI terdaftar",          "#dbeafe", "#1e40af", "#1d4ed8", "&#128196;");
        metricCard(row, "Disetujui",           d.disetujui, "Audit selesai disetujui",       "#dcfce7", "#166534", "#16a34a", "&#10003;");
        metricCard(row, "Menunggu Persetujuan",d.menunggu,  "Masih dalam proses pengajuan",  "#fef9c3", "#854d0e", "#ca8a04", "&#9203;");
        metricCard(row, "Ditolak",             d.ditolak,   "Tidak dilanjutkan / ditolak",   "#fee2e2", "#991b1b", "#dc2626", "&#10007;");
    }

    // ----------------------------------------------------------------
    // KPI cards: Temuan
    // ----------------------------------------------------------------

    private void renderTemuanCards(SpmiData d) {
        Div row = new Div();
        row.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:14px;");
        row.setParent(this);
        metricCard(row, "Total Temuan",      d.totalTemuan,      "Semua item temuan",        "#f1f5f9", "#334155", "#475569", "&#128202;");
        metricCard(row, "KTS Mayor",         d.jmlKtsMyr,        "Harus segera diperbaiki",  "#fee2e2", "#991b1b", "#dc2626", "&#9888;");
        metricCard(row, "KTS Minor",         d.jmlKtsMnr,        "Perlu perbaikan terjadwal","#ffedd5", "#9a3412", "#ea580c", "&#9651;");
        metricCard(row, "Observasi",         d.jmlO,             "Perlu pemantauan",         "#dbeafe", "#1e40af", "#3b82f6", "&#128270;");
        metricCard(row, "Sesuai + Melebihi", d.jmlS + d.jmlLs,  "Standar terpenuhi",        "#dcfce7", "#166534", "#22c55e", "&#127942;");
    }

    // ----------------------------------------------------------------
    // Zona kepatuhan panel (NEW)
    // ----------------------------------------------------------------

    private void renderZonaPanel(Div parent, SpmiData d) {
        Div panel = createPanel("Zona Kepatuhan", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:12px; line-height:1.5;'>"
            + "Klasifikasi zona berdasarkan tingkat kepatuhan standar mutu.</div>");

        for (String[] z : new String[][]{
            {"HIJAU",  "&#127381;", "#dcfce7", "#166534", "#22c55e", ">= 70%", "Mutu Baik"},
            {"KUNING", "&#127380;", "#fef9c3", "#854d0e", "#eab308", "50–70%",  "Perlu Perhatian"},
            {"MERAH",  "&#127379;", "#fee2e2", "#991b1b", "#ef4444", "< 50%",   "Perlu Perbaikan"}
        }) {
            boolean aktif = d.zona().equals(z[0]);
            appendHtml(panel,
                "<div style='border-radius:12px; padding:10px 12px; margin-bottom:7px;"
                + " background:" + z[2] + ";"
                + (aktif ? " border:2px solid " + z[4] + "; box-shadow:0 0 0 3px " + z[4] + "22;" : "")
                + "'>"
                + "<div style='display:flex; justify-content:space-between; align-items:center;'>"
                + "<div>"
                + "<div style='font-size:12px; font-weight:900; color:" + z[3] + ";'>"
                + z[1] + " ZONA " + z[0] + (aktif ? " &#9664;" : "") + "</div>"
                + "<div style='font-size:10px; color:" + z[3] + "; opacity:.8; margin-top:2px;'>"
                + z[5] + " Kepatuhan — " + z[6] + "</div>"
                + "</div>"
                + (aktif ? "<div style='font-size:20px; font-weight:900; color:" + z[4] + ";'>"
                    + d.healthPct() + "%</div>" : "")
                + "</div></div>");
        }
    }

    // ----------------------------------------------------------------
    // Donut health gauge
    // ----------------------------------------------------------------

    private void renderDonutHealth(Div parent, SpmiData d) {
        Div panel = createPanel("Tingkat Kepatuhan", parent);

        int hp  = d.healthPct();
        int cov = d.covPct();
        int kts = d.ktsPct();
        String hColor = hp >= ZONA_HIJAU_MIN ? "#22c55e" : (hp >= ZONA_KUNING_MIN ? "#f59e0b" : "#ef4444");

        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Persentase butir standar yang <b>terpenuhi</b> dari seluruh temuan.</div>");

        appendHtml(panel, svgDonut(hp, hColor, "Patuh"));

        appendHtml(panel,
            "<div style='display:flex; gap:7px; flex-wrap:wrap; margin-top:10px;'>"
            + miniScoreBox("Cakupan Audit", cov + "%", cov >= 80 ? "#22c55e" : "#f59e0b")
            + miniScoreBox("Risiko KTS",    kts + "%", kts <= 15 ? "#22c55e" : "#ef4444")
            + miniScoreBox("Skenario",      d.skenarioTeraudit + "/" + d.totalSkenario, "#3b82f6")
            + "</div>");

        appendHtml(panel,
            "<div style='margin-top:10px;'>"
            + miniBar("Sesuai (S)",            d.totalTemuan == 0 ? 0 : d.jmlS      * 100 / d.totalTemuan, "#22c55e")
            + miniBar("Melebihi Standar (LS)", d.totalTemuan == 0 ? 0 : d.jmlLs     * 100 / d.totalTemuan, "#10b981")
            + miniBar("Observasi (O)",         d.totalTemuan == 0 ? 0 : d.jmlO      * 100 / d.totalTemuan, "#3b82f6")
            + miniBar("KTS Minor",             d.totalTemuan == 0 ? 0 : d.jmlKtsMnr * 100 / d.totalTemuan, "#f97316")
            + miniBar("KTS Mayor",             d.totalTemuan == 0 ? 0 : d.jmlKtsMyr * 100 / d.totalTemuan, "#ef4444")
            + "</div>");
    }

    // ----------------------------------------------------------------
    // Tindak Lanjut summary (NEW)
    // ----------------------------------------------------------------

    private void renderTlSummary(Div parent, SpmiData d) {
        Div panel = createPanel("Tindak Lanjut KTS (Pengendalian)", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:12px; line-height:1.4;'>"
            + "Status penyelesaian tindak lanjut atas temuan KTS — fase <b>Pengendalian</b> dalam siklus PPEPP.</div>");

        if (d.tlTotal == 0) {
            appendHtml(panel,
                "<div style='text-align:center; padding:20px 10px;'>"
                + "<div style='font-size:28px; margin-bottom:8px;'>&#128203;</div>"
                + "<div style='font-size:12px; color:#64748b; font-weight:600;'>Belum ada tindak lanjut</div>"
                + "<div style='font-size:10px; color:#94a3b8; margin-top:4px; line-height:1.4;'>"
                + "Tambahkan tindak lanjut di setiap baris temuan KTS pada form Pengajuan AMI.</div>"
                + "</div>");
            return;
        }

        int tlPct = d.tlTotal == 0 ? 0 : d.tlSelesai * 100 / d.tlTotal;
        appendHtml(panel, svgDonut(tlPct, tlPct >= 80 ? "#22c55e" : (tlPct >= 50 ? "#3b82f6" : "#f97316"), "Selesai"));

        appendHtml(panel,
            "<div style='display:grid; grid-template-columns:1fr 1fr; gap:7px; margin-top:10px;'>"
            + tlBox(d.tlSelesai,   "Selesai",       "#dcfce7", "#166534")
            + tlBox(d.tlBerjalan,  "Berjalan",      "#dbeafe", "#1e40af")
            + tlBox(d.tlTerlambat, "Terlambat",     "#fee2e2", "#991b1b")
            + tlBox(d.tlTotal - d.tlSelesai - d.tlBerjalan - d.tlTerlambat,
                                   "Belum Dimulai", "#f1f5f9", "#475569")
            + "</div>");

        if (d.tlTerlambat > 0) {
            appendHtml(panel,
                "<div style='margin-top:10px; padding:8px 10px; background:#fee2e2; border-radius:10px;"
                + " font-size:11px; color:#991b1b; font-weight:600;'>"
                + "&#9888; " + d.tlTerlambat + " tindak lanjut melewati target waktu!</div>");
        }
    }

    private String tlBox(int val, String label, String bg, String clr) {
        return "<div style='border-radius:10px; padding:8px 10px; background:" + bg + "; text-align:center;'>"
             + "<div style='font-size:20px; font-weight:900; color:" + clr + ";'>" + val + "</div>"
             + "<div style='font-size:9px; color:" + clr + "; opacity:.8;'>" + esc(label) + "</div>"
             + "</div>";
    }

    // ----------------------------------------------------------------
    // Per Standar — stacked bar dengan zona warna (IMPROVED)
    // ----------------------------------------------------------------

    private void renderPerStandar(Div parent, SpmiData d) {
        Div panel = createPanel("Kepatuhan per Standar SPMI (dengan Zona)", parent);

        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.5;'>"
            + "Warna zona tiap standar: "
            + "<span style='color:#22c55e; font-weight:700;'>&#9632; Hijau &ge;70%</span> &nbsp;"
            + "<span style='color:#eab308; font-weight:700;'>&#9632; Kuning 50–70%</span> &nbsp;"
            + "<span style='color:#ef4444; font-weight:700;'>&#9632; Merah &lt;50%</span></div>");

        if (d.standarMatrix.isEmpty()) { appendHtml(panel, emptyMsg()); return; }

        appendHtml(panel,
            "<div style='display:flex; gap:8px; flex-wrap:wrap; margin-bottom:10px;'>"
            + colorChip("Sesuai (S)",      "#22c55e")
            + colorChip("Melebihi (LS)",   "#10b981")
            + colorChip("Observasi (O)",   "#3b82f6")
            + colorChip("KTS Minor",       "#f97316")
            + colorChip("KTS Mayor",       "#ef4444")
            + "</div>");

        Map<String, int[]> agg = buildStandarAgg(d.standarMatrix);
        for (Map.Entry<String, int[]> entry : agg.entrySet()) {
            int[] st    = entry.getValue();
            int   total = Math.max(1, st[0]);
            int   comp  = (st[1] + st[2]) * 100 / total;
            String zona = comp >= ZONA_HIJAU_MIN ? "#22c55e" : (comp >= ZONA_KUNING_MIN ? "#eab308" : "#ef4444");
            String zIcon= comp >= ZONA_HIJAU_MIN ? "&#127381;" : (comp >= ZONA_KUNING_MIN ? "&#127380;" : "&#127379;");
            String lbl  = entry.getKey().length() > 35 ? entry.getKey().substring(0, 34) + "…" : entry.getKey();

            appendHtml(panel,
                "<div style='margin:9px 0; padding:10px 12px; border-radius:12px;"
                + " background:#f8fafc; border-left:4px solid " + zona + ";'>"
                + "<div style='display:flex; justify-content:space-between; margin-bottom:5px; flex-wrap:wrap; gap:4px;'>"
                + "<span style='font-size:11px; color:#334155; font-weight:600;'>" + esc(lbl) + "</span>"
                + "<span style='font-size:11px; font-weight:800; color:" + zona + ";'>"
                + zIcon + " " + comp + "% patuh</span>"
                + "</div>"
                + "<div style='height:12px; border-radius:6px; display:flex; overflow:hidden; background:#e2e8f0;'>"
                + (st[1] > 0 ? seg(st[1] * 100 / total, "#22c55e") : "")
                + (st[2] > 0 ? seg(st[2] * 100 / total, "#10b981") : "")
                + (st[3] > 0 ? seg(st[3] * 100 / total, "#3b82f6") : "")
                + (st[4] > 0 ? seg(st[4] * 100 / total, "#f97316") : "")
                + (st[5] > 0 ? seg(st[5] * 100 / total, "#ef4444") : "")
                + "</div>"
                + "<div style='display:flex; gap:10px; margin-top:5px; font-size:9px; color:#64748b; flex-wrap:wrap;'>"
                + (st[1] > 0 ? "<span style='color:#22c55e;'>" + st[1] + " S</span>" : "")
                + (st[2] > 0 ? "<span style='color:#10b981;'>" + st[2] + " LS</span>" : "")
                + (st[3] > 0 ? "<span style='color:#3b82f6;'>" + st[3] + " O</span>" : "")
                + (st[4] > 0 ? "<span style='color:#f97316;'>" + st[4] + " KTS-MNR</span>" : "")
                + (st[5] > 0 ? "<span style='color:#ef4444;'>" + st[5] + " KTS-MYR</span>" : "")
                + "</div></div>");
        }
    }

    // ----------------------------------------------------------------
    // Trend KTS
    // ----------------------------------------------------------------

    private void renderTrendKts(Div parent, SpmiData d) {
        Div panel = createPanel("Tren Ketidaksesuaian (KTS) Tiap Tahun", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "<b>Tren menurun = mutu membaik.</b> Grafik menampilkan semua TA untuk analisis jangka panjang.</div>");

        appendHtml(panel, svgTrend(d.trendKts));

        if (d.trendKts.size() >= 2) {
            Object[] first  = d.trendKts.get(0);
            Object[] last   = d.trendKts.get(d.trendKts.size() - 1);
            int fv = ((Number) first[1]).intValue();
            int lv = ((Number) last[1]).intValue();
            boolean dec = lv < fv;
            appendHtml(panel,
                "<div style='font-size:11px; color:#475569; margin-top:8px; padding:8px 10px;"
                + " background:" + (dec ? "#dcfce7" : "#fee2e2") + "; border-radius:8px;'>"
                + (dec ? "&#9660; Tren <b>menurun</b> — tanda positif mutu membaik."
                       : "&#9650; Tren <b>meningkat</b> — perlu perhatian lebih.")
                + " Dari <b>" + fv + " KTS</b> (" + esc(safeStr(first[0])) + ")"
                + " &rarr; <b>" + lv + " KTS</b> (" + esc(safeStr(last[0])) + ")."
                + "</div>");
        } else if (d.trendKts.isEmpty()) {
            appendHtml(panel, "<div style='color:#94a3b8; font-size:11px; text-align:center; padding:20px;'>"
                + "Belum ada data KTS untuk ditampilkan trendnya.</div>");
        }
    }

    // ----------------------------------------------------------------
    // Top KTS skenario
    // ----------------------------------------------------------------

    private void renderTopKts(Div parent, SpmiData d) {
        Div panel = createPanel("Butir Standar Paling Sering Tidak Sesuai", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Fokuskan perbaikan di sini untuk dampak terbesar terhadap mutu.</div>");

        if (d.topKts.isEmpty()) {
            appendHtml(panel,
                "<div style='text-align:center; padding:20px; color:#22c55e; font-size:13px; font-weight:700;'>"
                + "&#10003; Tidak ada KTS pada periode ini!</div>");
            return;
        }

        int maxKts = ((Number) d.topKts.get(0)[1]).intValue();
        for (int i = 0; i < d.topKts.size(); i++) {
            Object[] row = d.topKts.get(i);
            String nama  = row[0] == null ? "(Tanpa Nama)" : row[0].toString();
            int    cnt   = ((Number) row[1]).intValue();
            int    w     = maxKts == 0 ? 0 : cnt * 100 / maxKts;
            String bg    = i == 0 ? "#fee2e2" : (i == 1 ? "#ffedd5" : "#fff7ed");
            String nbg   = i == 0 ? "#ef4444" : (i == 1 ? "#f97316" : "#fb923c");
            appendHtml(panel,
                "<div style='margin:7px 0; padding:8px 10px; border-radius:10px; background:" + bg + ";'>"
                + "<div style='display:flex; justify-content:space-between; gap:6px; margin-bottom:4px;'>"
                + "<span style='font-size:10px; color:#475569; font-weight:600;'>" + (i + 1) + ". "
                + esc(nama.length() > 60 ? nama.substring(0, 59) + "…" : nama) + "</span>"
                + "<span style='font-size:11px; font-weight:800; color:#ef4444; white-space:nowrap;'>"
                + cnt + " KTS</span>"
                + "</div>"
                + "<div style='height:7px; border-radius:4px; background:#fecaca;'>"
                + "<div style='height:7px; border-radius:4px; background:" + nbg + "; width:" + w + "%;'></div>"
                + "</div></div>");
        }
    }

    // ----------------------------------------------------------------
    // Distribusi temuan
    // ----------------------------------------------------------------

    private void renderDistribusi(Div parent, SpmiData d) {
        Div panel = createPanel("Distribusi Status Temuan AMI", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "<b>Merah</b> = ketidaksesuaian yang harus segera ditangani.</div>");

        int total = Math.max(1, d.totalTemuan);
        appendHtml(panel, pctBar("KTS Mayor (KTS MYR)",    d.jmlKtsMyr, total, "#dc2626"));
        appendHtml(panel, pctBar("KTS Minor (KTS MNR)",    d.jmlKtsMnr, total, "#ea580c"));
        appendHtml(panel, pctBar("Observasi (O)",          d.jmlO,      total, "#3b82f6"));
        appendHtml(panel, pctBar("Sesuai (S)",             d.jmlS,      total, "#22c55e"));
        appendHtml(panel, pctBar("Melebihi Standar (LS)",  d.jmlLs,     total, "#10b981"));
        if (d.jmlBelum > 0)
            appendHtml(panel, pctBar("Belum Ditentukan",   d.jmlBelum,  total, "#94a3b8"));

        if (d.totalTemuan == 0)
            appendHtml(panel, emptyMsg());
        else {
            appendHtml(panel,
                "<div style='margin-top:10px; padding:9px 10px; background:#f8fafc; border-radius:10px;"
                + " display:flex; gap:12px; flex-wrap:wrap; font-size:11px;'>"
                + "<span><b style='color:#ef4444;'>" + (d.jmlKtsMyr + d.jmlKtsMnr) + "</b> Total KTS</span>"
                + "<span><b style='color:#22c55e;'>" + (d.jmlS + d.jmlLs) + "</b> Sesuai+LS</span>"
                + "<span><b style='color:#3b82f6;'>" + d.jmlO + "</b> Observasi</span>"
                + "</div>");
        }
    }

    // ----------------------------------------------------------------
    // Per Jenis & Per Prodi
    // ----------------------------------------------------------------

    private void renderPerJenis(Div parent, SpmiData d) {
        Div panel = createPanel("AMI per Jenis SPMI", parent);
        if (d.perJenis.isEmpty()) { appendHtml(panel, emptyMsg()); return; }
        int max = 1;
        for (Object[] r : d.perJenis) max = Math.max(max, ((Number) r[1]).intValue());
        for (Object[] r : d.perJenis)
            appendHtml(panel, countBar(r[0] == null ? "(Tanpa Jenis)" : r[0].toString(),
                    ((Number) r[1]).intValue(), max, "#1d4ed8"));
    }

    private void renderPerProdi(Div parent, SpmiData d) {
        Div panel = createPanel("AMI per Program Studi", parent);
        if (d.perProdi.isEmpty()) { appendHtml(panel, emptyMsg()); return; }
        int max = 1;
        for (Object[] r : d.perProdi) max = Math.max(max, ((Number) r[1]).intValue());
        for (Object[] r : d.perProdi)
            appendHtml(panel, countBar(r[0] == null ? "(Tanpa Prodi)" : r[0].toString(),
                    ((Number) r[1]).intValue(), max, "#7c3aed"));
    }

    // ----------------------------------------------------------------
    // Recent AMI table (IMPROVED — lebih rapi, status badge)
    // ----------------------------------------------------------------

    private void renderRecent(Div parent, SpmiData d) {
        Div panel = createPanel("10 Pengajuan AMI Terbaru", parent);
        if (d.recent.isEmpty()) { appendHtml(panel, emptyMsg()); return; }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto; -webkit-overflow-scrolling:touch;'>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; min-width:520px;'>");
        sb.append("<tr style='background:#f8fafc;'>");
        for (String h : new String[]{"Judul Pengajuan", "Auditor / Auditee", "Jenis / Prodi", "TA / Smt", "Status"})
            sb.append("<th style='padding:8px 10px; text-align:left; color:#475569; font-weight:700;"
                    + " border-bottom:2px solid #e2e8f0; white-space:nowrap; font-size:11px;'>"
                    + esc(h) + "</th>");
        sb.append("</tr>");

        for (HasilSPMI h : d.recent) {
            String st  = h.getStatus();
            String sBg = HasilSPMI.DISETUJU.equals(st) ? "#dcfce7" : (HasilSPMI.DITOLAK.equals(st) ? "#fee2e2" : "#fef9c3");
            String sCl = HasilSPMI.DISETUJU.equals(st) ? "#166534" : (HasilSPMI.DITOLAK.equals(st) ? "#991b1b" : "#854d0e");
            sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>")
              .append("<td style='padding:8px 10px; color:#1e293b;'>")
                .append(esc(h.getNama().length() > 40 ? h.getNama().substring(0, 39) + "…" : h.getNama()))
                .append("</td>")
              .append("<td style='padding:8px 10px; color:#475569; font-size:11px;'>")
                .append(esc(h.getAuditorNama() != null && !h.getAuditorNama().isEmpty() ? h.getAuditorNama() : "—"))
                .append("<br><span style='color:#94a3b8;'>")
                .append(esc(h.getAuditeeNama() != null ? h.getAuditeeNama() : ""))
                .append("</span></td>")
              .append("<td style='padding:8px 10px; color:#475569; font-size:11px;'>")
                .append(esc(h.getJenisSPMI() != null ? h.getJenisSPMI().getNama() : "—"))
                .append(h.getJurusan() != null
                    ? "<br><span style='font-size:10px; color:#94a3b8;'>" + esc(h.getJurusan().getNama()) + "</span>"
                    : "")
                .append("</td>")
              .append("<td style='padding:8px 10px; color:#475569; white-space:nowrap; font-size:11px;'>")
                .append(esc(h.getTa() != null ? h.getTa() : ""))
                .append("<br><span style='color:#94a3b8;'>")
                .append(esc(h.getSemester() != null ? h.getSemester() : ""))
                .append("</span></td>")
              .append("<td style='padding:8px 10px;'><span style='border-radius:999px; padding:3px 10px;"
                    + " font-size:10px; font-weight:700; background:" + sBg + "; color:" + sCl + ";'>"
                    + esc(st) + "</span></td>")
              .append("</tr>");
        }
        sb.append("</table></div>");
        appendHtml(panel, sb.toString());
    }

    // ----------------------------------------------------------------
    // Action plan (rekomendasi otomatis)
    // ----------------------------------------------------------------

    private void renderActionPlan(Div parent, SpmiData d) {
        Div panel = createPanel("Saran Tindakan Prioritas (Otomatis)", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Rekomendasi berdasarkan kondisi audit saat ini — merah = prioritas utama.</div>");

        int tlPct = d.tlTotal == 0 ? 0 : d.tlSelesai * 100 / d.tlTotal;

        // Saran 1: KTS Mayor
        String sKts = d.jmlKtsMyr > 0
            ? d.jmlKtsMyr + " KTS Mayor harus segera ditindaklanjuti. Buat rencana perbaikan mendasar secepatnya."
            : (d.jmlKtsMnr > 0
                ? d.jmlKtsMnr + " KTS Minor tercatat. Tetapkan rencana koreksi dan pantau setiap bulan."
                : "Tidak ada KTS — pertahankan kepatuhan standar yang sudah tercapai.");

        // Saran 2: Tindak Lanjut
        String sTl = d.tlTotal == 0
            ? "Belum ada tindak lanjut yang dicatat. Masukkan rencana tindak lanjut untuk setiap temuan KTS."
            : (d.tlTerlambat > 0
                ? d.tlTerlambat + " tindak lanjut melewati target waktu! Segera eskalasi ke pimpinan."
                : tlPct < 50
                    ? "Penyelesaian tindak lanjut baru " + tlPct + "%. Percepat pelaksanaan koreksi."
                    : "Tindak lanjut " + tlPct + "% selesai. Pertahankan momentum perbaikan.");

        // Saran 3: Cakupan audit
        int covPct = d.covPct();
        String sCov = covPct < 50
            ? "Cakupan audit baru " + covPct + "%. Pastikan semua skenario aktif diaudit agar evaluasi menyeluruh."
            : (covPct < 80
                ? "Cakupan " + covPct + "% — masih ada skenario belum diaudit. Lengkapi sebelum akhir semester."
                : "Cakupan audit sudah baik (" + covPct + "%). Pertahankan konsistensi.");

        // Saran 4: Zona
        int hp = d.healthPct();
        String sZona = hp >= ZONA_HIJAU_MIN
            ? "Zona HIJAU (" + hp + "%). Dokumentasikan praktik terbaik sebagai acuan audit berikutnya."
            : (hp >= ZONA_KUNING_MIN
                ? "Zona KUNING (" + hp + "%). Fokus perbaikan item Observasi agar naik ke status Sesuai."
                : "Zona MERAH (" + hp + "%). Lakukan review menyeluruh dan susun rencana peningkatan mutu segera!");

        // Saran 5: Trend
        String sTrend = d.trendKts.size() >= 2
            ? (isTrendDecreasing(d.trendKts)
                ? "Tren KTS menurun — tanda positif mutu sedang membaik. Pertahankan program AMI rutin."
                : "Tren KTS meningkat — lakukan kajian mendalam pada proses pelaksanaan standar.")
            : "Belum cukup data historis. Laksanakan AMI secara konsisten setiap semester.";

        // Saran 6: Persetujuan
        String sPrs = d.menunggu > 0
            ? d.menunggu + " pengajuan AMI masih menunggu persetujuan. Segera tinjau agar tidak menunda proses audit."
            : "Tidak ada pengajuan menunggu — seluruh proses sudah diselesaikan.";

        appendHtml(panel,
            "<div style='display:flex; gap:10px; flex-wrap:wrap;'>"
            + planCard("1", "Penanganan KTS",      sKts,  d.jmlKtsMyr > 0 ? "high" : (d.jmlKtsMnr > 0 ? "mid" : "low"))
            + planCard("2", "Tindak Lanjut",        sTl,   d.tlTerlambat > 0 ? "high" : (d.tlTotal == 0 ? "mid" : "low"))
            + planCard("3", "Cakupan Audit",        sCov,  covPct < 50 ? "high" : (covPct < 80 ? "mid" : "low"))
            + planCard("4", "Zona Kepatuhan",       sZona, hp < ZONA_KUNING_MIN ? "high" : (hp < ZONA_HIJAU_MIN ? "mid" : "low"))
            + planCard("5", "Pantau Tren KTS",      sTrend,"low")
            + planCard("6", "Proses Persetujuan",   sPrs,  d.menunggu > 0 ? "mid" : "low")
            + "</div>");
    }

    // ================================================================
    // SVG Charts
    // ================================================================

    private String svgDonut(int pct, String color, String label) {
        double r   = 50.0, circ = 2.0 * Math.PI * r;
        double dash = circ * pct / 100.0, gap = circ - dash;
        String da  = String.format("%.2f %.2f", dash, gap);
        return "<svg viewBox='0 0 130 130' style='width:120px;height:120px;display:block;margin:auto;'"
             + " xmlns='http://www.w3.org/2000/svg'>"
             + "<circle cx='65' cy='65' r='50' fill='none' stroke='#e2e8f0' stroke-width='14'/>"
             + "<circle cx='65' cy='65' r='50' fill='none' stroke='" + color + "' stroke-width='14'"
             + " stroke-dasharray='" + da + "' stroke-linecap='round' transform='rotate(-90 65 65)'/>"
             + "<text x='65' y='60' text-anchor='middle' dominant-baseline='middle'"
             + " font-size='22' font-weight='900' fill='" + color + "'>" + pct + "%</text>"
             + "<text x='65' y='79' text-anchor='middle' font-size='9' fill='#94a3b8'>" + esc(label) + "</text>"
             + "</svg>";
    }

    private String svgTrend(List<Object[]> trend) {
        if (trend.isEmpty())
            return "<div style='text-align:center; color:#94a3b8; font-size:11px; padding:30px;'>"
                 + "Belum ada data tren KTS.</div>";

        int w = 360, h = 140, padL = 32, padR = 10, padT = 14, padB = 34;
        int plotW = w - padL - padR, plotH = h - padT - padB, n = trend.size();

        int maxVal = 1;
        for (Object[] row : trend) maxVal = Math.max(maxVal, ((Number) row[1]).intValue());

        StringBuilder svg = new StringBuilder(
            "<svg viewBox='0 0 " + w + " " + h + "' style='width:100%;' xmlns='http://www.w3.org/2000/svg'>");
        svg.append("<rect x='").append(padL).append("' y='").append(padT)
           .append("' width='").append(plotW).append("' height='").append(plotH)
           .append("' fill='#f8fafc' rx='4'/>");

        for (int lv = 1; lv <= 4; lv++) {
            int gy = padT + plotH - plotH * lv / 4;
            svg.append("<line x1='").append(padL).append("' y1='").append(gy)
               .append("' x2='").append(padL + plotW).append("' y2='").append(gy)
               .append("' stroke='#e2e8f0' stroke-width='1' stroke-dasharray='4 2'/>");
            svg.append("<text x='").append(padL - 3).append("' y='").append(gy)
               .append("' text-anchor='end' dominant-baseline='middle' font-size='8' fill='#94a3b8'>")
               .append(maxVal * lv / 4).append("</text>");
        }

        int[] px = new int[n], py = new int[n];
        for (int i = 0; i < n; i++) {
            int val = ((Number) trend.get(i)[1]).intValue();
            px[i] = padL + (n == 1 ? plotW / 2 : i * plotW / (n - 1));
            py[i] = padT + plotH - (plotH * val / maxVal);
        }

        // Area fill
        StringBuilder area = new StringBuilder("M ").append(padL).append(" ").append(padT + plotH);
        for (int i = 0; i < n; i++) area.append(" L ").append(px[i]).append(" ").append(py[i]);
        area.append(" L ").append(padL + plotW).append(" ").append(padT + plotH).append(" Z");
        svg.append("<path d='").append(area).append("' fill='#ef444418'/>");

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++)
            line.append(i == 0 ? "M " : " L ").append(px[i]).append(" ").append(py[i]);
        svg.append("<path d='").append(line)
           .append("' fill='none' stroke='#ef4444' stroke-width='2.5'"
                 + " stroke-linecap='round' stroke-linejoin='round'/>");

        for (int i = 0; i < n; i++) {
            svg.append("<circle cx='").append(px[i]).append("' cy='").append(py[i])
               .append("' r='4' fill='#ef4444' stroke='#fff' stroke-width='1.5'/>");
            int val = ((Number) trend.get(i)[1]).intValue();
            svg.append("<text x='").append(px[i]).append("' y='").append(py[i] - 7)
               .append("' text-anchor='middle' font-size='8' font-weight='700' fill='#ef4444'>")
               .append(val).append("</text>");
            String ta = safeStr(trend.get(i)[0]);
            svg.append("<text x='").append(px[i]).append("' y='").append(h - 5)
               .append("' text-anchor='middle' font-size='8' fill='#64748b'>")
               .append(esc(shortTa(ta))).append("</text>");
        }
        svg.append("<text x='10' y='").append(padT + plotH / 2)
           .append("' text-anchor='middle' font-size='8' fill='#94a3b8'"
                 + " transform='rotate(-90 10 ").append(padT + plotH / 2).append(")'>KTS</text>");
        svg.append("</svg>");
        return svg.toString();
    }

    // ================================================================
    // HTML component helpers
    // ================================================================

    private void metricCard(Div parent, String label, int val, String sub,
                            String bg, String dark, String accent, String icon) {
        appendHtml(parent,
            "<div style='flex:1 1 130px; min-width:120px; border-radius:18px; padding:16px 14px;"
            + " background:" + bg + "; transition:transform .15s;'>"
            + "<div style='font-size:16px; margin-bottom:4px;'>" + icon + "</div>"
            + "<div style='font-size:9px; font-weight:700; text-transform:uppercase; letter-spacing:.09em;"
            + " color:" + dark + "; opacity:.82;'>" + esc(label) + "</div>"
            + "<div style='font-size:32px; font-weight:900; color:" + accent + "; line-height:1.05; margin-top:5px;'>"
            + val + "</div>"
            + "<div style='font-size:10px; color:" + dark + "; opacity:.68; margin-top:5px; line-height:1.4;'>"
            + esc(sub) + "</div>"
            + "</div>");
    }

    private String miniBar(String label, int pct, String color) {
        int w = Math.min(100, Math.max(0, pct));
        return "<div style='margin:5px 0;'>"
             + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
             + "<span style='font-size:10px; color:#475569;'>" + esc(label) + "</span>"
             + "<span style='font-size:10px; font-weight:700; color:#1e293b;'>" + pct + "%</span></div>"
             + "<div style='height:6px; border-radius:3px; background:#e2e8f0;'>"
             + "<div style='height:6px; border-radius:3px; background:" + color + "; width:" + w + "%;'></div>"
             + "</div></div>";
    }

    private String miniScoreBox(String label, String val, String color) {
        return "<div style='flex:1 1 70px; border-radius:10px; padding:7px 8px; background:#f8fafc;"
             + " border:1.5px solid #e2e8f0; text-align:center;'>"
             + "<div style='font-size:14px; font-weight:900; color:" + color + ";'>" + val + "</div>"
             + "<div style='font-size:8px; color:#64748b; margin-top:2px; line-height:1.3;'>" + esc(label) + "</div>"
             + "</div>";
    }

    private String pctBar(String label, int val, int total, String color) {
        int pct = total == 0 ? 0 : val * 100 / total;
        return "<div style='margin:6px 0;'>"
             + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
             + "<span style='font-size:11px; color:#475569;'>" + esc(label) + "</span>"
             + "<span style='font-size:11px; font-weight:700; color:#1e293b;'>"
             + val + " (" + pct + "%)</span></div>"
             + "<div style='height:9px; border-radius:5px; background:#f1f5f9;'>"
             + "<div style='height:9px; border-radius:5px; background:" + color
             + "; width:" + Math.min(100, pct) + "%;'></div>"
             + "</div></div>";
    }

    private String countBar(String label, int val, int max, String color) {
        int w = max == 0 ? 0 : Math.min(100, val * 100 / max);
        return "<div style='margin:5px 0;'>"
             + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
             + "<span style='font-size:11px; color:#475569;'>" + esc(label) + "</span>"
             + "<span style='font-size:11px; font-weight:700; color:#1e293b;'>" + val + " AMI</span></div>"
             + "<div style='height:8px; border-radius:4px; background:#f1f5f9;'>"
             + "<div style='height:8px; border-radius:4px; background:" + color + "; width:" + w + "%;'></div>"
             + "</div></div>";
    }

    private String seg(int pct, String color) {
        return "<div style='width:" + pct + "%; background:" + color + "; height:100%;'></div>";
    }

    private String planCard(String no, String title, String body, String urgency) {
        String bg   = "high".equals(urgency) ? "#fee2e2" : ("mid".equals(urgency) ? "#fef9c3" : "#f0fdf4");
        String clr  = "high".equals(urgency) ? "#991b1b" : ("mid".equals(urgency) ? "#854d0e" : "#166534");
        String tag  = "high".equals(urgency)
            ? "<span style='font-size:9px; background:#dc2626; color:#fff; border-radius:4px; padding:1px 6px; margin-left:5px;'>PRIORITAS</span>"
            : ("mid".equals(urgency)
                ? "<span style='font-size:9px; background:#f59e0b; color:#fff; border-radius:4px; padding:1px 6px; margin-left:5px;'>SEGERA</span>"
                : "");
        return "<div style='flex:1 1 180px; border-radius:14px; padding:13px 14px; background:" + bg + ";'>"
             + "<div style='font-size:9px; font-weight:900; color:" + clr + "; opacity:.65;'>SARAN " + no + tag + "</div>"
             + "<div style='font-size:12px; font-weight:800; color:" + clr + "; margin-top:4px;'>"
             + esc(title) + "</div>"
             + "<div style='font-size:10px; color:" + clr + "; opacity:.85; margin-top:5px; line-height:1.5;'>"
             + esc(body) + "</div></div>";
    }

    private Div createPanel(String title, Div parent) {
        Div panel = new Div();
        panel.setStyle("background:#fff; border-radius:18px; padding:18px 20px;"
                     + " box-shadow:0 2px 12px rgba(0,0,0,.06); margin-bottom:12px;");
        panel.setParent(parent);
        appendHtml(panel,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin-bottom:10px;"
            + " padding-bottom:8px; border-bottom:2px solid #f1f5f9;'>" + esc(title) + "</div>");
        return panel;
    }

    private String colorChip(String label, String color) {
        return "<span style='font-size:10px; color:#475569; white-space:nowrap;'>"
             + "<span style='display:inline-block; width:10px; height:10px; border-radius:2px;"
             + " background:" + color + "; margin-right:4px; vertical-align:middle;'></span>"
             + esc(label) + "</span>";
    }

    private String emptyMsg() {
        return "<div style='color:#94a3b8; font-size:11px; text-align:center; padding:16px;'>"
             + "Belum ada data pada filter saat ini.</div>";
    }

    private Div flexRow() {
        Div r = new Div();
        r.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-bottom:0;");
        r.setParent(this);
        return r;
    }

    private Div colDiv(Div parent, String flex) {
        Div c = new Div();
        c.setStyle(flex + " min-width:0;");
        c.setParent(parent);
        return c;
    }

    // ================================================================
    // Utility
    // ================================================================

    private static String shortTa(String ta) {
        if (ta == null) return "?";
        if (ta.contains("/") && ta.length() >= 7) {
            String[] p = ta.split("/");
            return (p[0].length() >= 4 ? p[0].substring(2) : p[0])
                 + "/" + (p[1].length() >= 4 ? p[1].substring(2) : p[1]);
        }
        return ta.length() > 7 ? ta.substring(ta.length() - 7) : ta;
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private static void appendHtml(org.zkoss.zk.ui.Component parent, String html) {
        new org.zkoss.zul.Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
