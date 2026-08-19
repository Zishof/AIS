package ais.action.master.sekolah;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Textbox;

import ais.action.servlet.CatatanOrangTuaServlet;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.AktiftasHarianSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;

/**
 * Composer untuk halaman publik (tanpa login) catatan harian siswa.
 *
 * Orang tua/wali dapat:
 *  - Melihat ringkasan aktivitas & materi anak per bulan
 *  - Membaca pesan dari pembina/guru
 *  - Memberikan komentar/tanggapan harian
 *  - Menavigasi antar bulan
 *
 * Akses via servlet: /AktiftasHarianSiswa?siswa={ID_SISWA}
 */
public class CatatanOrangTuaAktiftasHarianAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    // ── Format tanggal ──────────────────────────────────────────────────────
    private static final Locale LOC_ID = new Locale("id", "ID");
    private static final ThreadLocal<SimpleDateFormat> FMT_BULAN = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("MMMM yyyy", LOC_ID);
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FMT_HARI_PENDEK = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("EEE, dd MMM", LOC_ID);
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FMT_HARI_LENGKAP = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("EEEE, dd MMMM yyyy", LOC_ID);
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FMT_TANGGAL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("dd", LOC_ID);
        }
    };

    // ── Warna tema ──────────────────────────────────────────────────────────
    private static final String C_PRIMER   = "#0f172a";
    private static final String C_AKSEN    = "#3b82f6";
    private static final String C_AKSEN2   = "#2563eb";
    private static final String C_HIJAU    = "#16a34a";
    private static final String C_MERAH    = "#dc2626";
    private static final String C_KUNING   = "#d97706";
    private static final String C_MUTED    = "#64748b";
    private static final String C_BG       = "#f0f4f8";
    private static final String C_CARD     = "#ffffff";
    private static final String C_BORDER   = "#e2e8f0";
    private static final String C_ORTU     = "#7c3aed";

    // ── ZUL-wired component ─────────────────────────────────────────────────
    private Div mainDiv;

    // ── State ────────────────────────────────────────────────────────────────
    private Siswa           siswa;
    private int             tahun;
    private int             bulanIdx; // Calendar.MONTH (0-based)
    private List<AktiftasHarianSiswa> dataList;

    // ── Data aggregation ─────────────────────────────────────────────────────
    private static final class DashData {
        int totalHari;
        int hariLengkap;      // semua aktivitas = YA
        int totalYaGlobal;
        int totalAktGlobal;
        int belumKomentar;
        // trend: tanggal → %selesai
        final List<Date>    trendTanggal  = new ArrayList<Date>();
        final List<Integer> trendPersen   = new ArrayList<Integer>();
        // spider: namaAktivitas → [jumlahYa, jumlahTotal]
        final Map<String, int[]> perJenis  = new LinkedHashMap<String, int[]>();
        // heatmap: hariKe → %selesai (-1 = tidak ada data)
        final Map<Integer, Integer> heatmap = new LinkedHashMap<Integer, Integer>();
        // materi: namaMateri → daftar nilai (string)
        final Map<String, List<String>> materiNilai = new LinkedHashMap<String, List<String>>();
    }

    // ════════════════════════════════════════════════════════════════════════
    // doAfterCompose
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initState();
        renderAll();
    }

    // ────────────────────────────────────────────────────────────────────────
    // initState: baca siswa dari HTTP session, set bulan default
    // ────────────────────────────────────────────────────────────────────────
    private void initState() {
        HttpSession httpSess = (HttpSession) Executions.getCurrent()
                .getDesktop().getSession().getNativeSession();

        Object siswaIdObj = httpSess != null
                ? httpSess.getAttribute(CatatanOrangTuaServlet.SK_SISWA_ID) : null;

        if (siswaIdObj != null) {
            try {
                Long siswaId = Long.parseLong(siswaIdObj.toString());
                Session sess = HibernateUtil.currentSession();
                siswa = (Siswa) sess.get(Siswa.class, siswaId);
            } catch (Exception e) {
                siswa = null;
            }
        }

        // Cek apakah ada bulan yg tersimpan di ZK session
        Object savedBulan = Executions.getCurrent().getDesktop().getSession()
                .getAttribute("co_bulan_idx");
        Object savedTahun = Executions.getCurrent().getDesktop().getSession()
                .getAttribute("co_tahun");

        if (savedBulan != null && savedTahun != null) {
            bulanIdx = (Integer) savedBulan;
            tahun    = (Integer) savedTahun;
        } else {
            Calendar cal = Calendar.getInstance();
            bulanIdx = cal.get(Calendar.MONTH);
            tahun    = cal.get(Calendar.YEAR);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigasi bulan
    // ────────────────────────────────────────────────────────────────────────
    private void navBulan(int delta) throws Exception {
        bulanIdx += delta;
        if (bulanIdx < 0)  { bulanIdx = 11; tahun--; }
        if (bulanIdx > 11) { bulanIdx = 0;  tahun++; }
        Executions.getCurrent().getDesktop().getSession()
                .setAttribute("co_bulan_idx", bulanIdx);
        Executions.getCurrent().getDesktop().getSession()
                .setAttribute("co_tahun", tahun);
        Common.clear(mainDiv);
        renderAll();
    }

    // ════════════════════════════════════════════════════════════════════════
    // renderAll: orkestrasi semua bagian halaman
    // ════════════════════════════════════════════════════════════════════════
    private void renderAll() throws Exception {
        if (siswa == null) {
            renderHalamanError();
            return;
        }
        dataList = loadData();
        DashData d = aggregate(dataList);

        renderCss();
        renderHeader();
        renderMonthNav();
        renderStatCards(d);
        renderRingkasanJudul();
        renderDonutDanTrend(d);
        renderSpiderChart(d);
        renderHeatmap(d);
        renderMateriRingkasan(d);
        renderDivider("Catatan Harian Anak");
        renderCatatanHarian();
        renderFooter();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Data loading & aggregation
    // ════════════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private List<AktiftasHarianSiswa> loadData() {
        Calendar cal = Calendar.getInstance();
        cal.set(tahun, bulanIdx, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date awal = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date akhir = cal.getTime();

        Session sess = HibernateUtil.currentSession();
        Criteria cr = sess.createCriteria(AktiftasHarianSiswa.class)
                .add(Restrictions.eq("siswa", siswa))
                .add(Restrictions.ge("tanggal", awal))
                .add(Restrictions.le("tanggal", akhir))
                .addOrder(Order.asc("tanggal"));
        return ConstantValues.simpleList(cr, AktiftasHarianSiswa.class);
    }

    private DashData aggregate(List<AktiftasHarianSiswa> list) {
        DashData d = new DashData();
        d.totalHari = list.size();

        // Inisialisasi heatmap untuk semua hari di bulan
        Calendar cal = Calendar.getInstance();
        cal.set(tahun, bulanIdx, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= maxDay; i++) d.heatmap.put(i, -1);

        for (AktiftasHarianSiswa akt : list) {
            // Heatmap
            if (akt.getTanggal() != null) {
                cal.setTime(akt.getTanggal());
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

                // Aktivitas
                int ya = 0, total = 0;
                if (akt.getAktifitas() != null && !akt.getAktifitas().trim().isEmpty()) {
                    try {
                        JSONObject j = new JSONObject(akt.getAktifitas());
                        Iterator<String> keys = j.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONObject item = j.getJSONObject(key);
                            String nama  = item.optString("nama", "");
                            String nilai = item.optString("nilai", "");
                            if (nama.isEmpty()) continue;
                            total++;
                            boolean isYa = nilai.equalsIgnoreCase("YA");
                            if (isYa) ya++;
                            d.totalAktGlobal++;
                            if (isYa) d.totalYaGlobal++;

                            // Spider
                            if (!d.perJenis.containsKey(nama)) {
                                d.perJenis.put(nama, new int[]{0, 0});
                            }
                            d.perJenis.get(nama)[1]++;
                            if (isYa) d.perJenis.get(nama)[0]++;
                        }
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:258"); /* ignore */ }
                }

                int persen = total > 0 ? (ya * 100 / total) : 0;
                if (ya == total && total > 0) d.hariLengkap++;
                d.heatmap.put(dayOfMonth, persen);
                d.trendTanggal.add(akt.getTanggal());
                d.trendPersen.add(persen);
            }

            if (akt.getPesanOrangTua() == null || akt.getPesanOrangTua().trim().isEmpty()) {
                d.belumKomentar++;
            }

            // Materi
            if (akt.getMateri() != null && !akt.getMateri().trim().isEmpty()) {
                try {
                    JSONObject jm = new JSONObject(akt.getMateri());
                    Iterator<String> keys = jm.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject item = jm.getJSONObject(key);
                        String nama  = item.optString("nama", "");
                        String nilai = item.optString("nilai", "");
                        if (nama.isEmpty()) continue;
                        if (!d.materiNilai.containsKey(nama)) {
                            d.materiNilai.put(nama, new ArrayList<String>());
                        }
                        if (!nilai.isEmpty()) d.materiNilai.get(nama).add(nilai);
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:288"); /* ignore */ }
            }
        }
        return d;
    }

    // ════════════════════════════════════════════════════════════════════════
    // CSS
    // ════════════════════════════════════════════════════════════════════════
    private void renderCss() {
        h(mainDiv,
            "<style>"
            + "*{box-sizing:border-box;margin:0;padding:0;}"
            + "body,html{background:" + C_BG + ";font-family:'Segoe UI',system-ui,-apple-system,sans-serif;}"

            // Header
            + ".co-header{background:linear-gradient(135deg," + C_PRIMER + " 0%," + C_AKSEN2 + " 100%);"
            +   "padding:20px 16px 28px;color:#fff;position:relative;overflow:hidden;}"
            + ".co-header::before{content:'';position:absolute;top:-30px;right:-30px;"
            +   "width:120px;height:120px;border-radius:50%;background:rgba(255,255,255,.08);}"
            + ".co-header::after{content:'';position:absolute;bottom:-20px;left:30px;"
            +   "width:80px;height:80px;border-radius:50%;background:rgba(255,255,255,.05);}"
            + ".co-avatar{width:52px;height:52px;border-radius:50%;background:rgba(255,255,255,.2);"
            +   "display:flex;align-items:center;justify-content:center;font-size:24px;"
            +   "margin-bottom:12px;border:2px solid rgba(255,255,255,.4);}"
            + ".co-nama{font-size:20px;font-weight:800;letter-spacing:.3px;}"
            + ".co-info{font-size:12px;opacity:.8;margin-top:4px;line-height:1.6;}"
            + ".co-badge{display:inline-block;background:rgba(255,255,255,.2);border-radius:20px;"
            +   "padding:2px 10px;font-size:11px;margin-top:6px;backdrop-filter:blur(4px);}"

            // Month nav
            + ".co-month-nav{background:" + C_CARD + ";padding:10px 16px;"
            +   "display:flex;align-items:center;justify-content:space-between;"
            +   "box-shadow:0 1px 3px rgba(0,0,0,.08);position:sticky;top:0;z-index:100;}"
            + ".co-month-btn{background:none;border:1px solid " + C_BORDER + ";border-radius:8px;"
            +   "width:36px;height:36px;cursor:pointer;font-size:16px;color:" + C_PRIMER + ";"
            +   "display:flex;align-items:center;justify-content:center;transition:all .15s;}"
            + ".co-month-btn:active{background:" + C_BG + ";}"
            + ".co-month-label{font-size:14px;font-weight:700;color:" + C_PRIMER + ";text-align:center;}"
            + ".co-month-sub{font-size:11px;color:" + C_MUTED + ";text-align:center;margin-top:2px;}"

            // Stats
            + ".co-stats{display:grid;grid-template-columns:repeat(2,1fr);gap:10px;"
            +   "padding:14px 14px 6px;}"
            + ".co-stat-card{background:" + C_CARD + ";border-radius:14px;"
            +   "padding:14px;box-shadow:0 1px 6px rgba(0,0,0,.07);}"
            + ".co-stat-icon{font-size:22px;margin-bottom:6px;}"
            + ".co-stat-val{font-size:24px;font-weight:900;color:" + C_PRIMER + ";line-height:1;}"
            + ".co-stat-lbl{font-size:11px;color:" + C_MUTED + ";margin-top:4px;line-height:1.4;}"

            // Section
            + ".co-section{padding:12px 14px 4px;}"
            + ".co-section-title{font-size:13px;font-weight:800;color:" + C_PRIMER + ";"
            +   "text-transform:uppercase;letter-spacing:.5px;margin-bottom:3px;}"
            + ".co-section-desc{font-size:11px;color:" + C_MUTED + ";line-height:1.5;margin-bottom:10px;}"
            + ".co-card{background:" + C_CARD + ";border-radius:16px;"
            +   "box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden;margin-bottom:12px;}"

            // Donut
            + ".co-donut-wrap{padding:20px;display:flex;align-items:center;gap:20px;}"
            + ".co-donut-svg{flex-shrink:0;}"
            + ".co-donut-legend{flex:1;}"
            + ".co-donut-pct{font-size:36px;font-weight:900;color:" + C_PRIMER + ";line-height:1;}"
            + ".co-donut-sub{font-size:12px;color:" + C_MUTED + ";margin-top:4px;line-height:1.5;}"
            + ".co-legend-row{display:flex;align-items:center;gap:8px;margin-top:10px;font-size:12px;}"
            + ".co-legend-dot{width:12px;height:12px;border-radius:3px;flex-shrink:0;}"

            // Trend bar
            + ".co-trend-body{padding:14px 14px 18px;}"
            + ".co-trend-bars{display:flex;align-items:flex-end;gap:4px;height:80px;}"
            + ".co-trend-col{display:flex;flex-direction:column;align-items:center;flex:1;}"
            + ".co-trend-bar{width:100%;border-radius:4px 4px 0 0;min-height:3px;transition:height .3s;}"
            + ".co-trend-lbl{font-size:9px;color:" + C_MUTED + ";margin-top:4px;text-align:center;"
            +   "writing-mode:vertical-rl;transform:rotate(180deg);height:28px;overflow:hidden;}"
            + ".co-trend-pct{font-size:9px;font-weight:700;margin-bottom:2px;}"

            // Spider
            + ".co-spider-wrap{padding:14px;display:flex;justify-content:center;}"

            // Heatmap
            + ".co-heatmap-body{padding:12px 14px;}"
            + ".co-heatmap-dow{display:grid;grid-template-columns:repeat(7,1fr);gap:4px;"
            +   "margin-bottom:4px;}"
            + ".co-heatmap-dow-lbl{font-size:9px;font-weight:700;color:" + C_MUTED + ";"
            +   "text-align:center;}"
            + ".co-heatmap-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:4px;}"
            + ".co-hm-cell{aspect-ratio:1;border-radius:5px;display:flex;align-items:center;"
            +   "justify-content:center;font-size:9px;font-weight:700;color:#fff;}"
            + ".co-hm-empty{background:transparent;}"
            + ".co-hm-nodata{background:#e2e8f0;color:" + C_MUTED + ";}"
            + ".co-hm-legend{display:flex;align-items:center;gap:6px;margin-top:10px;"
            +   "font-size:10px;color:" + C_MUTED + ";}"
            + ".co-hm-lg-cell{width:14px;height:14px;border-radius:3px;}"

            // Materi
            + ".co-materi-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));"
            +   "gap:8px;padding:14px;}"
            + ".co-materi-item{border-radius:10px;padding:10px 12px;border:1px solid " + C_BORDER + ";}"
            + ".co-materi-nama{font-size:11px;font-weight:700;color:" + C_PRIMER + ";margin-bottom:4px;}"
            + ".co-materi-nilai{font-size:10px;color:" + C_MUTED + ";}"
            + ".co-materi-val{display:inline-block;padding:2px 7px;border-radius:12px;"
            +   "font-size:10px;font-weight:600;margin:2px 2px 0 0;}"

            // Divider
            + ".co-divider{display:flex;align-items:center;gap:10px;padding:16px 14px 10px;}"
            + ".co-divider-line{flex:1;height:1px;background:" + C_BORDER + ";}"
            + ".co-divider-text{font-size:12px;font-weight:800;color:" + C_AKSEN + ";"
            +   "text-transform:uppercase;letter-spacing:.6px;white-space:nowrap;}"

            // Catatan harian
            + ".co-hari-card{background:" + C_CARD + ";margin:0 14px 10px;border-radius:16px;"
            +   "box-shadow:0 2px 8px rgba(0,0,0,.07);overflow:hidden;}"
            + ".co-hari-header{padding:12px 14px;display:flex;align-items:center;"
            +   "justify-content:space-between;cursor:pointer;border-bottom:1px solid " + C_BORDER + ";}"
            + ".co-hari-tanggal{font-size:13px;font-weight:800;color:" + C_PRIMER + ";}"
            + ".co-hari-badge{font-size:10px;font-weight:700;padding:3px 10px;"
            +   "border-radius:20px;}"
            + ".co-hari-body{padding:12px 14px;}"

            // Aktivitas row
            + ".co-akt-list{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:10px;}"
            + ".co-akt-chip{display:inline-flex;align-items:center;gap:5px;font-size:11px;"
            +   "padding:5px 10px;border-radius:20px;font-weight:600;}"
            + ".co-akt-ya{background:#dcfce7;color:#15803d;}"
            + ".co-akt-tidak{background:#fee2e2;color:#b91c1c;}"
            + ".co-akt-na{background:#f1f5f9;color:" + C_MUTED + ";}"

            // Materi per hari
            + ".co-mat-row{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:10px;}"
            + ".co-mat-chip{display:inline-block;font-size:11px;padding:4px 10px;"
            +   "border-radius:20px;background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe;}"

            // Pesan pembina
            + ".co-pesan-pbna{background:#fffbeb;border-left:3px solid #f59e0b;"
            +   "border-radius:0 10px 10px 0;padding:10px 12px;font-size:12px;"
            +   "color:#78350f;margin-bottom:10px;line-height:1.6;}"
            + ".co-pesan-pbna-lbl{font-size:10px;font-weight:800;text-transform:uppercase;"
            +   "letter-spacing:.4px;color:#b45309;margin-bottom:4px;}"

            // Komentar ortu
            + ".co-ortu-area{background:#faf5ff;border:1px solid #e9d5ff;border-radius:12px;"
            +   "padding:12px;margin-top:4px;}"
            + ".co-ortu-lbl{font-size:11px;font-weight:800;color:" + C_ORTU + ";"
            +   "margin-bottom:8px;display:flex;align-items:center;gap:6px;}"
            + ".co-ortu-existing{font-size:12px;color:#4c1d95;line-height:1.6;"
            +   "margin-bottom:8px;font-style:italic;padding:8px 10px;"
            +   "background:#ede9fe;border-radius:8px;}"
            + ".co-ortu-edit-link{font-size:11px;color:" + C_ORTU + ";cursor:pointer;"
            +   "text-decoration:underline;margin-bottom:8px;display:inline-block;}"
            + ".co-ortu-save-btn{background:" + C_ORTU + ";color:#fff;border:none;"
            +   "border-radius:10px;padding:9px 18px;font-size:12px;font-weight:700;"
            +   "cursor:pointer;width:100%;margin-top:8px;transition:background .15s;}"
            + ".co-ortu-save-btn:active{background:#6d28d9;}"
            + ".co-ortu-cancel-btn{background:transparent;color:" + C_MUTED + ";border:1px solid " + C_BORDER + ";"
            +   "border-radius:10px;padding:8px 18px;font-size:12px;cursor:pointer;width:100%;margin-top:6px;}"
            + ".co-tx{width:100%;border:1px solid #d8b4fe;border-radius:10px;"
            +   "padding:10px 12px;font-size:12px;font-family:inherit;resize:vertical;"
            +   "min-height:80px;background:#fff;outline:none;line-height:1.6;}"
            + ".co-tx:focus{border-color:" + C_ORTU + ";box-shadow:0 0 0 3px rgba(124,58,237,.15);}"

            // Empty
            + ".co-empty{text-align:center;padding:48px 20px;color:" + C_MUTED + ";font-size:13px;"
            +   "line-height:1.8;}"
            + ".co-empty-icon{font-size:48px;display:block;margin-bottom:10px;}"

            // Footer
            + ".co-footer{text-align:center;padding:24px;font-size:11px;color:#94a3b8;}"

            // Responsive
            + "@media(min-width:480px){"
            +   ".co-stats{grid-template-columns:repeat(4,1fr);}"
            +   ".co-donut-pct{font-size:42px;}"
            + "}"
            + "@media(min-width:720px){"
            +   ".co-header{padding:28px 24px 36px;}"
            +   ".co-hari-card{margin:0 20px 12px;}"
            +   ".co-section{padding:14px 20px 4px;}"
            +   ".co-trend-bars{height:100px;}"
            + "}"
            + "</style>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Header: info siswa
    // ════════════════════════════════════════════════════════════════════════
    private void renderHeader() {
        String namaSiswa = safeStr(siswa.getNamaSiswa());
        String nis       = safeStr(siswa.getNomorInduk());
        String kelas     = "";
        String sekolah   = "";
        try { if (siswa.getKelas()  != null) kelas   = safeStr(siswa.getKelas().getNama()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:478");}
        try { if (siswa.getSekolah()!= null) sekolah = safeStr(siswa.getSekolah().getNama()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:479");}

        h(mainDiv,
            "<div class='co-header'>"
            + "<div class='co-avatar'>&#128100;</div>"
            + "<div class='co-nama'>" + esc(namaSiswa) + "</div>"
            + "<div class='co-info'>"
            + (nis.isEmpty()    ? "" : "NIS: " + esc(nis) + "<br>")
            + (kelas.isEmpty()  ? "" : "Kelas: " + esc(kelas) + "<br>")
            + (sekolah.isEmpty()? "" : esc(sekolah))
            + "</div>"
            + "<div class='co-badge'>&#128216; Catatan Harian Siswa</div>"
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Navigasi bulan
    // ════════════════════════════════════════════════════════════════════════
    private void renderMonthNav() {
        Calendar cal = Calendar.getInstance();
        cal.set(tahun, bulanIdx, 1);
        String namaBulan = FMT_BULAN.get().format(cal.getTime());
        int totalHari    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int hariBerdata  = dataList != null ? dataList.size() : 0;

        Div navDiv = new Div();
        navDiv.setSclass("co-month-nav");
        navDiv.setParent(mainDiv);

        // Tombol prev
        org.zkoss.zul.Button btnPrev = new org.zkoss.zul.Button("◀");
        btnPrev.setStyle("background:none;border:1px solid #e2e8f0;border-radius:8px;"
                + "width:36px;height:36px;cursor:pointer;font-size:16px;color:#0f172a;");
        btnPrev.setParent(navDiv);
        btnPrev.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { navBulan(-1); }
        });

        // Label tengah
        Div labelDiv = new Div();
        labelDiv.setStyle("text-align:center;");
        labelDiv.setParent(navDiv);
        h(labelDiv,
            "<div class='co-month-label'>&#128197; " + esc(namaBulan) + "</div>"
            + "<div class='co-month-sub'>" + hariBerdata + " dari " + totalHari + " hari tercatat</div>");

        // Tombol next (nonaktif jika sudah bulan sekarang)
        Calendar sekarang = Calendar.getInstance();
        boolean isFuture  = (tahun > sekarang.get(Calendar.YEAR))
                || (tahun == sekarang.get(Calendar.YEAR) && bulanIdx >= sekarang.get(Calendar.MONTH));

        org.zkoss.zul.Button btnNext = new org.zkoss.zul.Button("▶");
        btnNext.setStyle("background:none;border:1px solid #e2e8f0;border-radius:8px;"
                + "width:36px;height:36px;cursor:pointer;font-size:16px;"
                + "color:" + (isFuture ? "#cbd5e1" : "#0f172a") + ";");
        btnNext.setDisabled(isFuture);
        btnNext.setParent(navDiv);
        btnNext.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { navBulan(1); }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // Stat cards
    // ════════════════════════════════════════════════════════════════════════
    private void renderStatCards(DashData d) {
        int persen = d.totalAktGlobal > 0
                ? (d.totalYaGlobal * 100 / d.totalAktGlobal) : 0;

        String[][] cards = {
            {"&#128197;", String.valueOf(d.totalHari),
                "Hari Tercatat", C_AKSEN},
            {"&#10003;", String.valueOf(d.hariLengkap),
                "Hari Semua\nAktivitas Terpenuhi", C_HIJAU},
            {"&#128200;", persen + "%",
                "Rata-rata Aktivitas\nBerhasil Dilakukan", C_KUNING},
            {"&#128172;", String.valueOf(d.belumKomentar),
                "Hari Belum\nAda Komentar Anda", d.belumKomentar > 0 ? C_MERAH : C_MUTED}
        };

        h(mainDiv, "<div class='co-stats'>");
        for (String[] c : cards) {
            h(mainDiv,
                "<div class='co-stat-card'>"
                + "<div class='co-stat-icon'>" + c[0] + "</div>"
                + "<div class='co-stat-val' style='color:" + c[3] + ";'>" + esc(c[1]) + "</div>"
                + "<div class='co-stat-lbl'>" + esc(c[2]).replace("\n", "<br>") + "</div>"
                + "</div>");
        }
        h(mainDiv, "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Label ringkasan
    // ════════════════════════════════════════════════════════════════════════
    private void renderRingkasanJudul() {
        h(mainDiv,
            "<div class='co-divider'>"
            + "<div class='co-divider-line'></div>"
            + "<div class='co-divider-text'>&#128202; Ringkasan Bulan Ini</div>"
            + "<div class='co-divider-line'></div>"
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Donut + Trend (side-by-side on tablet)
    // ════════════════════════════════════════════════════════════════════════
    private void renderDonutDanTrend(DashData d) {
        // ── Donut ────────────────────────────────────────────────────────
        renderSectionHeader("Seberapa Banyak Aktivitas Terpenuhi",
            "Dari semua aktivitas yang dijadwalkan bulan ini, berapa persen yang berhasil dilakukan anak.");

        int persenYa = d.totalAktGlobal > 0
                ? (d.totalYaGlobal * 100 / d.totalAktGlobal) : 0;
        int persenTdk = 100 - persenYa;

        // SVG Donut (r=40, cx=cy=50, circumference≈251.2)
        double circ = 2 * Math.PI * 40; // ~251.33
        double dashYa  = circ * persenYa  / 100.0;
        double dashTdk = circ - dashYa;

        String warna = persenYa >= 80 ? C_HIJAU : persenYa >= 50 ? C_KUNING : C_MERAH;

        h(mainDiv,
            "<div class='co-card'><div class='co-donut-wrap'>"
            + "<svg class='co-donut-svg' width='120' height='120' viewBox='0 0 100 100'>"
            + "<circle cx='50' cy='50' r='40' fill='none' stroke='#e2e8f0' stroke-width='12'/>"
            + "<circle cx='50' cy='50' r='40' fill='none' stroke='" + warna + "' stroke-width='12'"
            +   " stroke-dasharray='" + fmt(dashYa) + " " + fmt(dashTdk) + "'"
            +   " stroke-dashoffset='" + fmt(circ * 0.25) + "' stroke-linecap='round'/>"
            + "<text x='50' y='46' text-anchor='middle' font-size='16' font-weight='900' fill='" + C_PRIMER + "'>"
            +   persenYa + "%</text>"
            + "<text x='50' y='60' text-anchor='middle' font-size='8' fill='" + C_MUTED + "'>selesai</text>"
            + "</svg>"
            + "<div class='co-donut-legend'>"
            + "<div class='co-donut-sub'>Dari <b>" + d.totalAktGlobal + "</b> tugas aktivitas "
            + "bulan ini, <b style='color:" + warna + ";'>" + d.totalYaGlobal + "</b> berhasil dilakukan.</div>"
            + "<div class='co-legend-row'><div class='co-legend-dot' style='background:" + C_HIJAU + ";'></div>"
            +   "<span>Terpenuhi: <b>" + d.totalYaGlobal + "</b></span></div>"
            + "<div class='co-legend-row'><div class='co-legend-dot' style='background:#fee2e2;border:1px solid " + C_MERAH + ";'></div>"
            +   "<span>Belum: <b>" + (d.totalAktGlobal - d.totalYaGlobal) + "</b></span></div>"
            + "</div></div></div>");

        // ── Trend harian ─────────────────────────────────────────────────
        if (!d.trendTanggal.isEmpty()) {
            renderSectionHeader("Tren Harian Aktivitas",
                "Grafik di bawah menunjukkan naik-turun pencapaian aktivitas anak setiap harinya dalam bulan ini.");

            StringBuilder bars = new StringBuilder();
            bars.append("<div class='co-card'><div class='co-trend-body'>"
                    + "<div class='co-trend-bars'>");
            int max = d.trendPersen.isEmpty() ? 100 : 100;
            for (int i = 0; i < d.trendTanggal.size(); i++) {
                int p  = d.trendPersen.get(i);
                String warnaTrend = p >= 80 ? C_HIJAU : p >= 50 ? C_KUNING : C_MERAH;
                String dd = FMT_TANGGAL.get().format(d.trendTanggal.get(i));
                bars.append("<div class='co-trend-col'>"
                    + "<div class='co-trend-pct' style='font-size:8px;color:" + warnaTrend + ";'>" + p + "</div>"
                    + "<div class='co-trend-bar' style='height:" + p + "%;background:" + warnaTrend + ";opacity:.85;'></div>"
                    + "<div class='co-trend-lbl'>" + dd + "</div>"
                    + "</div>");
            }
            bars.append("</div></div></div>");
            h(mainDiv, bars.toString());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Spider / Radar chart per jenis aktivitas
    // ════════════════════════════════════════════════════════════════════════
    private void renderSpiderChart(DashData d) {
        if (d.perJenis.isEmpty()) return;

        renderSectionHeader("Profil Aktivitas Anak",
            "Gambaran seberapa konsisten anak melakukan tiap jenis aktivitas. Semakin besar area, semakin bagus.");

        // Ambil maks 8 jenis
        List<String> namaList = new ArrayList<String>(d.perJenis.keySet());
        if (namaList.size() > 8) namaList = namaList.subList(0, 8);
        int n = namaList.size();
        if (n < 3) { // spider perlu min 3 titik
            // Fallback ke bar horizontal
            renderSpiderFallbackBars(d, namaList);
            return;
        }

        int cx = 120, cy = 120, R = 90, r_inner = 18;
        StringBuilder svg = new StringBuilder();
        svg.append("<div class='co-card'><div class='co-spider-wrap'>"
                + "<svg width='240' height='240' viewBox='0 0 240 240'>");

        // Grid rings
        for (int ring = 1; ring <= 4; ring++) {
            double rr = R * ring / 4.0;
            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double angle = Math.toRadians(-90 + 360.0 * i / n);
                pts.append(fmt(cx + rr * Math.cos(angle))).append(",")
                   .append(fmt(cy + rr * Math.sin(angle))).append(" ");
            }
            svg.append("<polygon points='").append(pts)
               .append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
        }

        // Spoke lines
        for (int i = 0; i < n; i++) {
            double angle = Math.toRadians(-90 + 360.0 * i / n);
            svg.append("<line x1='").append(cx).append("' y1='").append(cy)
               .append("' x2='").append(fmt(cx + R * Math.cos(angle)))
               .append("' y2='").append(fmt(cy + R * Math.sin(angle)))
               .append("' stroke='#e2e8f0' stroke-width='1'/>");
        }

        // Data polygon
        StringBuilder dataPts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String nama = namaList.get(i);
            int[] v = d.perJenis.get(nama);
            double pct = v[1] > 0 ? (double) v[0] / v[1] : 0;
            double angle = Math.toRadians(-90 + 360.0 * i / n);
            double rr = R * pct;
            dataPts.append(fmt(cx + rr * Math.cos(angle))).append(",")
                   .append(fmt(cy + rr * Math.sin(angle))).append(" ");
        }
        svg.append("<polygon points='").append(dataPts)
           .append("' fill='rgba(59,130,246,.25)' stroke='").append(C_AKSEN)
           .append("' stroke-width='2'/>");

        // Labels + dots
        for (int i = 0; i < n; i++) {
            String nama = namaList.get(i);
            int[] v = d.perJenis.get(nama);
            double pct = v[1] > 0 ? (double) v[0] / v[1] : 0;
            double angle = Math.toRadians(-90 + 360.0 * i / n);
            double rr = R * pct;

            // Dot pada data
            svg.append("<circle cx='").append(fmt(cx + rr * Math.cos(angle)))
               .append("' cy='").append(fmt(cy + rr * Math.sin(angle)))
               .append("' r='3' fill='").append(C_AKSEN).append("'/>");

            // Label
            double lx = cx + (R + 14) * Math.cos(angle);
            double ly = cy + (R + 14) * Math.sin(angle);
            String anchor = lx < cx - 5 ? "end" : lx > cx + 5 ? "start" : "middle";
            // Truncate nama jika panjang
            String shortNama = nama.length() > 9 ? nama.substring(0, 8) + "…" : nama;
            int persenLbl = (int) (pct * 100);
            svg.append("<text x='").append(fmt(lx)).append("' y='").append(fmt(ly))
               .append("' text-anchor='").append(anchor)
               .append("' font-size='9' fill='").append(C_PRIMER).append("' font-weight='700'>")
               .append(esc(shortNama)).append("</text>");
            svg.append("<text x='").append(fmt(lx)).append("' y='").append(fmt(ly + 10))
               .append("' text-anchor='").append(anchor)
               .append("' font-size='8' fill='").append(C_AKSEN2).append("'>")
               .append(persenLbl).append("%</text>");
        }

        svg.append("</svg></div></div>");
        h(mainDiv, svg.toString());
    }

    private void renderSpiderFallbackBars(DashData d, List<String> namaList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='co-card' style='padding:14px;'>");
        for (String nama : namaList) {
            int[] v = d.perJenis.get(nama);
            int p = v[1] > 0 ? (v[0] * 100 / v[1]) : 0;
            String warna = p >= 80 ? C_HIJAU : p >= 50 ? C_KUNING : C_MERAH;
            sb.append("<div style='margin-bottom:10px;'>"
                + "<div style='display:flex;justify-content:space-between;font-size:11px;"
                +   "color:" + C_PRIMER + ";margin-bottom:4px;'>"
                + "<span>" + esc(nama) + "</span><b>" + p + "%</b></div>"
                + "<div style='background:#e2e8f0;border-radius:4px;height:8px;'>"
                + "<div style='width:" + p + "%;background:" + warna + ";height:8px;border-radius:4px;'></div>"
                + "</div></div>");
        }
        sb.append("</div>");
        h(mainDiv, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Heatmap kalender bulan
    // ════════════════════════════════════════════════════════════════════════
    private void renderHeatmap(DashData d) {
        renderSectionHeader("Kalender Aktivitas Bulan Ini",
            "Setiap kotak mewakili satu hari. Warna hijau = aktivitas bagus, merah = perlu perhatian, abu-abu = belum ada data.");

        Calendar cal = Calendar.getInstance();
        cal.set(tahun, bulanIdx, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        int maxDay   = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='co-card'><div class='co-heatmap-body'>"
            + "<div class='co-heatmap-dow'>"
            + "<div class='co-heatmap-dow-lbl'>Min</div>"
            + "<div class='co-heatmap-dow-lbl'>Sen</div>"
            + "<div class='co-heatmap-dow-lbl'>Sel</div>"
            + "<div class='co-heatmap-dow-lbl'>Rab</div>"
            + "<div class='co-heatmap-dow-lbl'>Kam</div>"
            + "<div class='co-heatmap-dow-lbl'>Jum</div>"
            + "<div class='co-heatmap-dow-lbl'>Sab</div>"
            + "</div><div class='co-heatmap-grid'>");

        // Sel kosong sebelum hari pertama
        for (int i = 0; i < firstDow; i++) {
            sb.append("<div class='co-hm-cell co-hm-empty'></div>");
        }

        for (int day = 1; day <= maxDay; day++) {
            Integer persen = d.heatmap.get(day);
            String bg, color, txt;
            if (persen == null || persen < 0) {
                bg    = "#f1f5f9"; color = "#94a3b8"; txt = String.valueOf(day);
            } else if (persen == 0) {
                bg = "#fee2e2"; color = "#b91c1c"; txt = String.valueOf(day);
            } else if (persen < 50) {
                bg = "#fef08a"; color = "#78350f"; txt = String.valueOf(day);
            } else if (persen < 80) {
                bg = "#bbf7d0"; color = "#14532d"; txt = String.valueOf(day);
            } else {
                bg = "#16a34a"; color = "#fff"; txt = String.valueOf(day);
            }
            sb.append("<div class='co-hm-cell' style='background:").append(bg)
              .append(";color:").append(color).append(";' title='Hari ").append(day)
              .append(persen != null && persen >= 0 ? ": " + persen + "% selesai" : ": belum ada data")
              .append("'>").append(txt).append("</div>");
        }

        sb.append("</div>"
            + "<div class='co-hm-legend'>"
            + "<div class='co-hm-lg-cell' style='background:#f1f5f9;border:1px solid #e2e8f0;'></div><span>Belum ada data</span>"
            + "<div class='co-hm-lg-cell' style='background:#fee2e2;'></div><span>Perlu perhatian</span>"
            + "<div class='co-hm-lg-cell' style='background:#bbf7d0;'></div><span>Cukup baik</span>"
            + "<div class='co-hm-lg-cell' style='background:#16a34a;'></div><span>Sangat baik</span>"
            + "</div></div></div>");

        h(mainDiv, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Ringkasan materi per bulan
    // ════════════════════════════════════════════════════════════════════════
    private void renderMateriRingkasan(DashData d) {
        if (d.materiNilai.isEmpty()) return;

        renderSectionHeader("Pencapaian Materi Belajar",
            "Kumpulan nilai/catatan materi anak selama bulan ini dari semua sesi belajar.");

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='co-card'><div class='co-materi-grid'>");

        for (Map.Entry<String, List<String>> entry : d.materiNilai.entrySet()) {
            String nama   = entry.getKey();
            List<String> nilaiList = entry.getValue();

            sb.append("<div class='co-materi-item'>"
                + "<div class='co-materi-nama'>" + esc(nama) + "</div>"
                + "<div class='co-materi-nilai'>");
            for (String n : nilaiList) {
                String warna = nilaiWarna(n);
                sb.append("<span class='co-materi-val' style='background:").append(warna)
                  .append(";'>").append(esc(n)).append("</span>");
            }
            if (nilaiList.isEmpty()) {
                sb.append("<span style='color:#94a3b8;font-size:10px;'>—</span>");
            }
            sb.append("</div></div>");
        }
        sb.append("</div></div>");
        h(mainDiv, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Daftar catatan harian + form komentar orang tua
    // ════════════════════════════════════════════════════════════════════════
    private void renderCatatanHarian() throws Exception {
        if (dataList == null || dataList.isEmpty()) {
            h(mainDiv,
                "<div class='co-empty'>"
                + "<span class='co-empty-icon'>&#128214;</span>"
                + "Belum ada catatan harian untuk bulan ini.<br>"
                + "Silakan pilih bulan lain menggunakan tombol navigasi di atas."
                + "</div>");
            return;
        }

        // Terbalik: terbaru di atas
        List<AktiftasHarianSiswa> reversed =
                new ArrayList<AktiftasHarianSiswa>(dataList);
        Collections.reverse(reversed);

        for (AktiftasHarianSiswa akt : reversed) {
            renderSatuHari(akt);
        }
    }

    private void renderSatuHari(final AktiftasHarianSiswa akt) {
        Div card = new Div();
        card.setSclass("co-hari-card");
        card.setParent(mainDiv);

        // Header hari
        String tglStr  = akt.getTanggal() != null ? FMT_HARI_LENGKAP.get().format(akt.getTanggal()) : "—";
        String tglPendek = akt.getTanggal() != null ? FMT_HARI_PENDEK.get().format(akt.getTanggal()) : "—";

        // Hitung badge
        int ya = 0, total = 0;
        if (akt.getAktifitas() != null && !akt.getAktifitas().trim().isEmpty()) {
            try {
                JSONObject j = new JSONObject(akt.getAktifitas());
                Iterator<String> keys = j.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject item = j.getJSONObject(key);
                    String nilai = item.optString("nilai", "");
                    total++;
                    if (nilai.equalsIgnoreCase("YA")) ya++;
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:899");}
        }
        String badgeColor = total == 0 ? C_MUTED
                : ya == total ? C_HIJAU
                : ya * 2 >= total ? C_KUNING : C_MERAH;
        String badgeTxt   = total == 0 ? "—" : ya + "/" + total + " &#10003;";

        h(card,
            "<div class='co-hari-header'>"
            + "<div class='co-hari-tanggal'>&#128197; " + esc(tglStr) + "</div>"
            + "<div class='co-hari-badge' style='background:" + badgeColor + ";color:#fff;'>"
            +   badgeTxt + "</div>"
            + "</div>");

        Div body = new Div();
        body.setSclass("co-hari-body");
        body.setParent(card);

        // Keterangan umum
        if (akt.getKeterangan() != null && !akt.getKeterangan().trim().isEmpty()) {
            h(body,
                "<div style='font-size:12px;color:" + C_MUTED + ";margin-bottom:10px;"
                + "padding:8px 10px;background:#f8fafc;border-radius:8px;line-height:1.5;'>"
                + esc(akt.getKeterangan()) + "</div>");
        }

        // Aktivitas
        renderAktivitasHari(body, akt.getAktifitas());

        // Materi
        renderMateriHari(body, akt.getMateri());

        // Pesan pembina
        if (akt.getPesanPembina() != null && !akt.getPesanPembina().trim().isEmpty()) {
            h(body,
                "<div class='co-pesan-pbna'>"
                + "<div class='co-pesan-pbna-lbl'>&#128218; Pesan Guru / Pembina</div>"
                + esc(akt.getPesanPembina())
                + "</div>");
        }

        // Komentar orang tua
        renderKomentarOrtu(body, akt);
    }

    private void renderAktivitasHari(Div parent, String aktifitasJson) {
        if (aktifitasJson == null || aktifitasJson.trim().isEmpty()) return;
        try {
            JSONObject j = new JSONObject(aktifitasJson);
            if (j.length() == 0) return;

            h(parent,
                "<div style='font-size:11px;font-weight:800;text-transform:uppercase;"
                + "letter-spacing:.4px;color:" + C_PRIMER + ";margin-bottom:6px;'>"
                + "Aktivitas Hari Ini</div>"
                + "<div class='co-akt-list'>");

            List<String> keys = new ArrayList<String>();
            Iterator<String> iter = j.keys();
            while (iter.hasNext()) keys.add(iter.next());
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    try { return Integer.parseInt(a) - Integer.parseInt(b); }
                    catch (NumberFormatException e) { return a.compareTo(b); }
                }
            });

            StringBuilder chips = new StringBuilder();
            for (String key : keys) {
                JSONObject item = j.getJSONObject(key);
                String nama  = item.optString("nama", "");
                String nilai = item.optString("nilai", "");
                if (nama.isEmpty()) continue;
                boolean isYa  = nilai.equalsIgnoreCase("YA");
                boolean isTdk = nilai.equalsIgnoreCase("TIDAK");
                String cls   = isYa ? "co-akt-ya" : isTdk ? "co-akt-tidak" : "co-akt-na";
                String sym   = isYa ? "&#10003;" : isTdk ? "&#10007;" : "&#8212;";
                chips.append("<div class='co-akt-chip ").append(cls).append("'>")
                     .append(sym).append(" ").append(esc(nama)).append("</div>");
            }
            h(parent, chips.toString() + "</div>");
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:980");}
    }

    private void renderMateriHari(Div parent, String materiJson) {
        if (materiJson == null || materiJson.trim().isEmpty()) return;
        try {
            JSONObject j = new JSONObject(materiJson);
            if (j.length() == 0) return;

            h(parent,
                "<div style='font-size:11px;font-weight:800;text-transform:uppercase;"
                + "letter-spacing:.4px;color:" + C_PRIMER + ";margin-bottom:6px;'>"
                + "Materi Belajar</div>"
                + "<div class='co-mat-row'>");

            List<String> keys = new ArrayList<String>();
            Iterator<String> iter = j.keys();
            while (iter.hasNext()) keys.add(iter.next());
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    try { return Integer.parseInt(a) - Integer.parseInt(b); }
                    catch (NumberFormatException e) { return a.compareTo(b); }
                }
            });

            StringBuilder chips = new StringBuilder();
            for (String key : keys) {
                JSONObject item = j.getJSONObject(key);
                String nama  = item.optString("nama", "");
                String nilai = item.optString("nilai", "");
                if (nama.isEmpty()) continue;
                chips.append("<div class='co-mat-chip'>")
                     .append(esc(nama))
                     .append(nilai.isEmpty() ? "" : ": <b>" + esc(nilai) + "</b>")
                     .append("</div>");
            }
            h(parent, chips.toString() + "</div>");
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:1017");}
    }

    private void renderKomentarOrtu(Div parent, final AktiftasHarianSiswa akt) {
        Div komentarDiv = new Div();
        komentarDiv.setSclass("co-ortu-area");
        komentarDiv.setParent(parent);

        h(komentarDiv,
            "<div class='co-ortu-lbl'>&#128172; Tanggapan Orang Tua / Wali</div>");

        String existing = akt.getPesanOrangTua();
        boolean adaKomentar = existing != null && !existing.trim().isEmpty();

        if (adaKomentar) {
            h(komentarDiv,
                "<div class='co-ortu-existing'>&#8220;" + esc(existing.trim()) + "&#8221;</div>");
        }

        // Textarea + tombol simpan
        h(komentarDiv, adaKomentar
                ? "<div style='font-size:11px;color:" + C_MUTED + ";margin-bottom:8px;'>Ubah komentar:</div>"
                : "<div style='font-size:11px;color:" + C_MUTED + ";margin-bottom:8px;'>Tuliskan komentar atau catatan untuk guru:</div>");

        final Textbox txKomentar = new Textbox();
        txKomentar.setMultiline(true);
        txKomentar.setRows(3);
        txKomentar.setSclass("co-tx");
        txKomentar.setWidth("100%");
        if (adaKomentar) txKomentar.setValue(existing.trim());
        txKomentar.setParent(komentarDiv);

        org.zkoss.zul.Button btnSimpan = new org.zkoss.zul.Button("&#128190; Simpan Komentar");
        btnSimpan.setStyle("background:" + C_ORTU + ";color:#fff;border:none;"
                + "border-radius:10px;padding:9px 18px;font-size:12px;font-weight:700;"
                + "cursor:pointer;width:100%;margin-top:8px;");
        btnSimpan.setParent(komentarDiv);
        final Long aktId = akt.getId();
        btnSimpan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                String pesan = txKomentar.getValue();
                if (pesan == null || pesan.trim().isEmpty()) {
                    MyMessageboxConfig.show("Komentar tidak boleh kosong.");
                    return;
                }
                savePesanOrangTua(aktId, pesan.trim());
                // Reload
                Common.clear(mainDiv);
                dataList = loadData();
                DashData d = aggregate(dataList);
                renderCss();
                renderHeader();
                renderMonthNav();
                renderStatCards(d);
                renderRingkasanJudul();
                renderDonutDanTrend(d);
                renderSpiderChart(d);
                renderHeatmap(d);
                renderMateriRingkasan(d);
                renderDivider("Catatan Harian Anak");
                renderCatatanHarian();
                renderFooter();
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // Save komentar orang tua ke DB
    // ════════════════════════════════════════════════════════════════════════
    private void savePesanOrangTua(Long aktId, String pesan) {
        Session sess = HibernateUtil.currentSession();
        Transaction tx = sess.beginTransaction();
        try {
            AktiftasHarianSiswa akt = (AktiftasHarianSiswa) sess.get(AktiftasHarianSiswa.class, aktId);
            if (akt != null) {
                akt.setPesanOrangTua(pesan);
                sess.merge(akt);
                tx.commit();
            } else {
                tx.rollback();
            }
        } catch (Exception ex) {
            try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:1099");}
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sekolah/CatatanOrangTuaAktiftasHarianAction.java:1100");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: section header
    // ════════════════════════════════════════════════════════════════════════
    private void renderSectionHeader(String judul, String deskripsi) {
        h(mainDiv,
            "<div class='co-section'>"
            + "<div class='co-section-title'>" + esc(judul) + "</div>"
            + "<div class='co-section-desc'>" + esc(deskripsi) + "</div>"
            + "</div>");
    }

    private void renderDivider(String teks) {
        h(mainDiv,
            "<div class='co-divider'>"
            + "<div class='co-divider-line'></div>"
            + "<div class='co-divider-text'>" + esc(teks) + "</div>"
            + "<div class='co-divider-line'></div>"
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Error page
    // ════════════════════════════════════════════════════════════════════════
    private void renderHalamanError() {
        h(mainDiv,
            "<div class='co-empty' style='padding-top:80px;'>"
            + "<span class='co-empty-icon'>&#9888;&#65039;</span>"
            + "<b>Data siswa tidak ditemukan.</b><br>"
            + "Pastikan tautan yang digunakan sudah benar,<br>"
            + "atau hubungi pihak sekolah untuk mendapatkan tautan yang baru."
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Footer
    // ════════════════════════════════════════════════════════════════════════
    private void renderFooter() {
        h(mainDiv,
            "<div class='co-footer'>"
            + "&#127891; Buku Penghubung Digital &mdash; Sistem Informasi Sekolah"
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private static void h(org.zkoss.zk.ui.Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String fmt(double d) {
        // Gunakan Locale.US agar desimal pakai titik (bukan koma)
        return String.format(java.util.Locale.US, "%.2f", d);
    }

    /**
     * Pilih warna latar berdasarkan teks nilai materi.
     */
    private static String nilaiWarna(String nilai) {
        if (nilai == null) return "#f1f5f9";
        String v = nilai.toUpperCase().trim();
        if (v.equals("A") || v.equals("SANGAT BAIK") || v.equals("BAIK SEKALI") || v.equals("LANCAR")) {
            return "#dcfce7"; // hijau terang
        } else if (v.equals("B") || v.equals("BAIK")) {
            return "#dbeafe"; // biru terang
        } else if (v.equals("C") || v.equals("CUKUP")) {
            return "#fef9c3"; // kuning terang
        } else if (v.equals("D") || v.equals("KURANG")) {
            return "#fee2e2"; // merah terang
        }
        return "#f1f5f9"; // abu-abu default
    }
}
