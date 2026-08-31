package ais.action.master.kalender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JadwalKegiatanKampus;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor Info Kegiatan — tampilan terpadu jadwal kegiatan kampus:
 * ringkasan status, linimasa waktu, tren bulanan, dan tabel lengkap
 * dengan unduhan Excel.
 *
 * Pola: extends Div, lazy-load via timer, Hibernate langsung.
 * Mengikuti pola DasboardSPMI & DasbordKalenderAkademik.
 */
public class DasbordInfoKegiatan extends Div {

    private static final long serialVersionUID = 1L;

    private static final String[] NAMA_BULAN = {
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
    };

    private static final String CLR_BERLANGSUNG = "#f97316";
    private static final String CLR_BELUM_MULAI = "var(--ais-theme-primary,#3b82f6)";
    private static final String CLR_SELESAI     = "#64748b";
    private static final String CLR_HEADER      = "#0f5132";

    // ---- paging tabel ----
    private Paging pagingTabel;
    private MyGrid  gridTabel;
    private List<JadwalKegiatanKampus> lastList = new ArrayList<JadwalKegiatanKampus>();

    // ================================================================
    // Data container
    // ================================================================

    /**
     * Tipe implementasi bersarang {@link IkData} milik {@link DasbordInfoKegiatan}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasbordInfoKegiatan}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List list}, {@code int total}, {@code
     * int berlangsung}, {@code int belumMulai}, {@code int selesai}, {@code Map perBulan}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DasbordInfoKegiatan
     */
    private static class IkData {
        List<JadwalKegiatanKampus> list = new ArrayList<JadwalKegiatanKampus>();
        int total, berlangsung, belumMulai, selesai;
        Map<String, Integer> perBulan = new LinkedHashMap<String, Integer>();
    }

    // ================================================================
    // Constructor
    // ================================================================

    public DasbordInfoKegiatan() {
        setWidth("100%");
        setStyle("min-height:300px;background:#f0fdf4;padding:14px 16px;"
               + "box-sizing:border-box;overflow:auto;");
        try {
            tampilLoading();
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event e) throws Exception { renderAll(); }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ================================================================
    // Loading
    // ================================================================

    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:80px 0;text-align:center;'>"
            + "<div style='font-size:40px;margin-bottom:16px;"
            +   "animation:ik-spin 1.5s linear infinite;display:inline-block;'>&#128203;</div>"
            + "<div style='font-size:15px;font-weight:800;color:#134e4a;'>Memuat Info Kegiatan&#8230;</div>"
            + "<div style='margin-top:8px;font-size:12px;color:#6b7280;'>"
            +   "Menyiapkan jadwal, status, dan ringkasan kegiatan kampus.</div>"
            + "</div>"
            + "<style>@keyframes ik-spin{to{transform:rotate(360deg)}}</style>");
    }

    // ================================================================
    // Main render
    // ================================================================

    private void renderAll() {
        try {
            IkData d = loadDataWithCache();
            Common.clear(this);

            renderCss();
            renderAksiBar();
            renderRingkasan(d);
            renderTimeline(d);

            Div row3 = flexRow("margin-bottom:12px;");
            renderStatusDonut(colDiv(row3, "flex:1 1 220px;min-width:200px;"), d);
            renderBarBulan(colDiv(row3, "flex:2 1 300px;min-width:260px;"), d);

            renderTabel(d);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ================================================================
    // Data queries
    // ================================================================

    private IkData loadDataWithCache() {
        String key = DashboardCacheUtil.key("DasbordInfoKegiatan", "ADMIN", null);
        Object fromL2 = DashboardCacheUtil.getL2(key);
        if (fromL2 instanceof IkData) return (IkData) fromL2;
        Object fromL3 = DashboardCacheUtil.getL3(key);
        if (fromL3 instanceof IkData) {
            DashboardCacheUtil.putL2(key, fromL3);
            return (IkData) fromL3;
        }
        IkData d = loadData();
        DashboardCacheUtil.putL2(key, d);
        DashboardCacheUtil.putL3(key, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private IkData loadData() {
        IkData d = new IkData();
        d.list = (List<JadwalKegiatanKampus>) HibernateUtil.currentSession()
            .createCriteria(JadwalKegiatanKampus.class)
            .add(Restrictions.or(
                Restrictions.isNull("aktif"),
                Restrictions.eq("aktif", Boolean.TRUE)))
            .addOrder(Order.asc("tanggalMulai"))
            .list();

        for (String b : NAMA_BULAN) d.perBulan.put(b, 0);

        for (JadwalKegiatanKampus kg : d.list) {
            d.total++;
            String status = safeStatus(kg);
            if ("Berlangsung".equals(status))    d.berlangsung++;
            else if ("Belum Mulai".equals(status)) d.belumMulai++;
            else if ("Sudah Selesai".equals(status)) d.selesai++;

            if (kg.getTanggalMulai() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(kg.getTanggalMulai());
                String b = NAMA_BULAN[cal.get(Calendar.MONTH)];
                Integer nb = d.perBulan.get(b);
                d.perBulan.put(b, nb == null ? 1 : nb + 1);
            }
        }
        lastList = d.list;
        return d;
    }

    // ================================================================
    // CSS
    // ================================================================

    private void renderCss() {
        appendHtml(this,
            "<style>"
            + ".ik-card{background:#fff;border-radius:14px;padding:14px 16px;"
            +   "box-shadow:0 2px 8px rgba(0,0,0,.06);box-sizing:border-box;}"
            + ".ik-card-title{font-size:11px;font-weight:700;color:#64748b;"
            +   "text-transform:uppercase;letter-spacing:.04em;margin-bottom:4px;}"
            + ".ik-card-val{font-size:28px;font-weight:900;line-height:1;}"
            + ".ik-card-sub{font-size:11px;color:#94a3b8;margin-top:4px;}"
            + ".ik-section-label{font-size:11px;font-weight:800;color:#475569;"
            +   "text-transform:uppercase;letter-spacing:.05em;margin-bottom:6px;}"
            + ".ik-desc{font-size:11px;color:#94a3b8;margin-bottom:10px;}"
            + ".ik-tl-bar{height:20px;border-radius:6px;display:flex;align-items:center;"
            +   "padding:0 6px;font-size:10px;font-weight:700;color:#fff;"
            +   "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;"
            +   "box-sizing:border-box;min-width:4px;}"
            + ".ik-bar-row{display:flex;align-items:center;gap:6px;margin-bottom:5px;font-size:11px;}"
            + ".ik-bar-fill{height:14px;border-radius:4px;min-width:2px;}"
            + ".ik-donut-wrap{position:relative;width:130px;height:130px;margin:0 auto 8px;}"
            + ".ik-donut-center{position:absolute;top:50%;left:50%;"
            +   "transform:translate(-50%,-50%);text-align:center;}"
            + ".ik-badge{display:inline-block;padding:2px 8px;border-radius:20px;"
            +   "font-size:10px;font-weight:700;}"
            + ".ik-event-card{background:#fff;border-radius:10px;border-left:4px solid;"
            +   "padding:10px 12px;margin-bottom:8px;box-shadow:0 1px 4px rgba(0,0,0,.05);}"
            + "@media(max-width:640px){.ik-card-val{font-size:20px;}}"
            + "</style>");
    }

    // ================================================================
    // Action bar (Download button)
    // ================================================================

    private void renderAksiBar() {
        Div bar = new Div();
        bar.setStyle("background:#fff;border-radius:14px;padding:10px 14px;"
                   + "box-shadow:0 2px 8px rgba(0,0,0,.06);margin-bottom:12px;"
                   + "display:flex;align-items:center;gap:10px;flex-wrap:wrap;");
        bar.setParent(this);

        appendHtml(bar,
            "<span style='font-size:12px;font-weight:700;color:#0f5132;'>"
            + "&#128203; Jadwal Kegiatan Kampus</span>"
            + "<span style='font-size:11px;color:#64748b;margin-left:auto;'>"
            + "Semua kegiatan yang direncanakan kampus.</span>");

        MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.gif");
        btnExcel.setParent(bar);
        btnExcel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { downloadExcel(); }
        });
    }

    // ================================================================
    // Ringkasan (4 KPI)
    // ================================================================

    private void renderRingkasan(IkData d) {
        Div wrap = new Div();
        wrap.setStyle("margin-bottom:12px;");
        wrap.setParent(this);

        appendHtml(wrap,
            "<div class='ik-section-label'>Ringkasan Kegiatan Kampus</div>"
            + "<div class='ik-desc'>"
            + "Gambaran cepat seluruh kegiatan yang pernah dicatat di sistem.</div>");

        Div row = flexRowInto(wrap, "");
        buatKartu(colDiv(row, "flex:1 1 130px;min-width:120px;"),
            "Total Kegiatan", d.total, CLR_HEADER, "Seluruh kegiatan tercatat");
        buatKartu(colDiv(row, "flex:1 1 130px;min-width:120px;"),
            "Sedang Berlangsung", d.berlangsung, CLR_BERLANGSUNG, "Aktif hari ini");
        buatKartu(colDiv(row, "flex:1 1 130px;min-width:120px;"),
            "Akan Datang", d.belumMulai, CLR_BELUM_MULAI, "Dijadwalkan ke depan");
        buatKartu(colDiv(row, "flex:1 1 130px;min-width:120px;"),
            "Sudah Selesai", d.selesai, CLR_SELESAI, "Kegiatan yang telah berakhir");
    }

    private void buatKartu(Div parent, String judul, int nilai, String warna, String sub) {
        appendHtml(parent,
            "<div class='ik-card' style='border-top:4px solid " + warna + ";'>"
            + "<div class='ik-card-title'>" + esc(judul) + "</div>"
            + "<div class='ik-card-val' style='color:" + warna + ";'>" + nilai + "</div>"
            + "<div class='ik-card-sub'>" + esc(sub) + "</div>"
            + "</div>");
    }

    // ================================================================
    // Linimasa (Gantt horizontal)
    // ================================================================

    private void renderTimeline(IkData d) {
        Div card = buatCard(this, "margin-bottom:12px;");
        appendHtml(card,
            "<div class='ik-section-label'>Linimasa Kegiatan Kampus</div>"
            + "<div class='ik-desc'>Urutan dan lama setiap kegiatan — "
            +   "memudahkan Anda melihat apakah ada kegiatan yang tumpang tindih.</div>");

        if (d.list.isEmpty()) {
            appendHtml(card, "<div style='color:#94a3b8;font-size:12px;padding:20px 0;text-align:center;'>"
                + "Tidak ada kegiatan yang terdaftar.</div>");
            return;
        }

        Date tMin = null, tMax = null;
        for (JadwalKegiatanKampus kg : d.list) {
            if (tMin == null || kg.getTanggalMulai().before(tMin))   tMin = kg.getTanggalMulai();
            if (tMax == null || kg.getTanggalSelesai().after(tMax))  tMax = kg.getTanggalSelesai();
        }
        long span = tMax.getTime() - tMin.getTime();
        if (span <= 0) span = 1;

        StringBuilder tlHtml = new StringBuilder();
        tlHtml.append("<div style='overflow-x:auto;'><div style='min-width:480px;'>");

        // Tick bulan
        tlHtml.append("<div style='display:flex;margin-bottom:4px;"
            + "font-size:10px;color:#94a3b8;font-weight:600;'>");
        tlHtml.append("<div style='width:150px;flex-shrink:0;'>Kegiatan</div>");
        tlHtml.append("<div style='flex:1;position:relative;height:16px;'>");
        Calendar tickCal = Calendar.getInstance();
        tickCal.setTime(tMin);
        tickCal.set(Calendar.DAY_OF_MONTH, 1);
        while (!tickCal.getTime().after(tMax)) {
            long pos = tickCal.getTime().getTime() - tMin.getTime();
            double pct = (pos * 100.0) / span;
            if (pct >= 0 && pct <= 100) {
                tlHtml.append("<span style='position:absolute;left:")
                      .append(String.format("%.1f", pct))
                      .append("%;transform:translateX(-50%);white-space:nowrap;'>")
                      .append(NAMA_BULAN[tickCal.get(Calendar.MONTH)]).append("</span>");
            }
            tickCal.add(Calendar.MONTH, 1);
        }
        tlHtml.append("</div></div>");

        List<JadwalKegiatanKampus> sample = d.list.size() > 18 ? d.list.subList(0, 18) : d.list;
        for (JadwalKegiatanKampus kg : sample) {
            String status = safeStatus(kg);
            String warna  = warnaStatus(status);
            double left   = ((kg.getTanggalMulai().getTime() - tMin.getTime()) * 100.0) / span;
            double width  = Math.max(0.5,
                ((kg.getTanggalSelesai().getTime() - kg.getTanggalMulai().getTime()) * 100.0) / span);
            String nama   = kg.getNama() == null ? "" : kg.getNama();

            tlHtml.append("<div style='display:flex;align-items:center;margin-bottom:4px;'>");
            tlHtml.append("<div style='width:150px;flex-shrink:0;font-size:10px;color:#334155;"
                + "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;padding-right:8px;' title='")
                .append(esc(nama)).append("'>").append(esc(truncate(nama, 22))).append("</div>");
            tlHtml.append("<div style='flex:1;position:relative;height:20px;"
                + "background:#f1f5f9;border-radius:4px;'>");
            tlHtml.append("<div class='ik-tl-bar' style='position:absolute;left:")
                .append(String.format("%.2f", left)).append("%;width:")
                .append(String.format("%.2f", width)).append("%;background:")
                .append(warna).append(";' title='").append(esc(status)).append("'>")
                .append(esc(truncate(nama, 16))).append("</div>");
            tlHtml.append("</div></div>");
        }
        if (d.list.size() > 18) {
            tlHtml.append("<div style='font-size:10px;color:#94a3b8;margin-top:4px;'>"
                + "... dan " + (d.list.size() - 18) + " kegiatan lainnya.</div>");
        }
        tlHtml.append("</div></div>");
        appendHtml(card, tlHtml.toString());
    }

    // ================================================================
    // Donut — distribusi status
    // ================================================================

    private void renderStatusDonut(Div parent, IkData d) {
        Div card = buatCard(parent, "height:100%;");
        appendHtml(card,
            "<div class='ik-section-label'>Distribusi Status</div>"
            + "<div class='ik-desc'>Perbandingan jumlah kegiatan berdasarkan statusnya.</div>");

        if (d.total == 0) {
            appendHtml(card, "<div style='color:#94a3b8;font-size:11px;'>Belum ada data.</div>");
            return;
        }
        double p1 = (d.berlangsung * 100.0) / d.total;
        double p2 = (d.belumMulai  * 100.0) / d.total;
        double e2 = p1 + p2;

        appendHtml(card,
            "<div class='ik-donut-wrap'>"
            + "<div style='width:130px;height:130px;border-radius:50%;"
            +   "background:conic-gradient("
            +   CLR_BERLANGSUNG + " 0% " + String.format("%.1f", p1) + "%,"
            +   CLR_BELUM_MULAI + " " + String.format("%.1f", p1) + "% " + String.format("%.1f", e2) + "%,"
            +   CLR_SELESAI     + " " + String.format("%.1f", e2) + "% 100%);"
            +   "-webkit-mask:radial-gradient(farthest-side,transparent 55%,#000 56%);"
            +   "mask:radial-gradient(farthest-side,transparent 55%,#000 56%);"
            + "'></div>"
            + "<div class='ik-donut-center'>"
            +   "<div style='font-size:16px;font-weight:900;color:#0f5132;'>" + d.total + "</div>"
            +   "<div style='font-size:9px;color:#64748b;'>Total</div>"
            + "</div></div>"
            + "<div style='font-size:11px;'>"
            + legendItem(CLR_BERLANGSUNG, "Berlangsung",   d.berlangsung, d.total)
            + legendItem(CLR_BELUM_MULAI, "Akan Datang",   d.belumMulai,  d.total)
            + legendItem(CLR_SELESAI,     "Sudah Selesai", d.selesai,     d.total)
            + "</div>");
    }

    private String legendItem(String warna, String label, int nilai, int total) {
        int pct = total == 0 ? 0 : (nilai * 100 / total);
        return "<div style='display:flex;align-items:center;gap:6px;margin-bottom:4px;'>"
            + "<div style='width:10px;height:10px;border-radius:3px;background:" + warna + ";flex-shrink:0;'></div>"
            + "<span style='color:#334155;'>" + esc(label) + "</span>"
            + "<span style='margin-left:auto;font-weight:700;color:#134e4a;'>" + nilai
            + " <span style='font-weight:400;color:#94a3b8;'>(" + pct + "%)</span></span>"
            + "</div>";
    }

    // ================================================================
    // Bar — kegiatan per bulan
    // ================================================================

    private void renderBarBulan(Div parent, IkData d) {
        Div card = buatCard(parent, "height:100%;");
        appendHtml(card,
            "<div class='ik-section-label'>Kegiatan per Bulan</div>"
            + "<div class='ik-desc'>Distribusi jumlah kegiatan berdasarkan bulan mulai — "
            +   "bulan dengan bar panjang adalah bulan paling padat kegiatan.</div>");

        int maxVal = 0;
        for (Map.Entry<String, Integer> e : d.perBulan.entrySet()) {
            if (e.getValue() > maxVal) maxVal = e.getValue();
        }
        if (maxVal == 0) maxVal = 1;

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-top:8px;'>");
        for (Map.Entry<String, Integer> e : d.perBulan.entrySet()) {
            int val = e.getValue();
            if (val == 0) continue;
            double pct = (val * 80.0) / maxVal;
            sb.append("<div class='ik-bar-row'>");
            sb.append("<span style='width:28px;color:#64748b;flex-shrink:0;'>")
              .append(esc(e.getKey())).append("</span>");
            sb.append("<div class='ik-bar-fill' style='width:").append(String.format("%.1f", pct))
              .append("%;background:").append(CLR_HEADER).append(";'></div>");
            sb.append("<span style='color:#0f5132;font-weight:700;'>").append(val).append("</span>");
            sb.append("</div>");
        }
        sb.append("</div>");
        appendHtml(card, sb.toString());
    }

    // ================================================================
    // Tabel ZK Grid
    // ================================================================

    private void renderTabel(IkData d) {
        Div card = buatCard(this, "margin-bottom:12px;");
        appendHtml(card,
            "<div class='ik-section-label'>Daftar Lengkap Kegiatan</div>"
            + "<div class='ik-desc'>Semua kegiatan kampus beserta detail waktu, "
            +   "tempat, dan statusnya — dapat diunduh sebagai file Excel.</div>");

        if (d.list.isEmpty()) {
            appendHtml(card,
                "<div style='text-align:center;color:#94a3b8;font-size:12px;padding:24px 0;'>"
                + "&#128203; Belum ada kegiatan yang tercatat.</div>");
            return;
        }

        pagingTabel = new Paging();
        pagingTabel.setDetailed(true);
        pagingTabel.setVisible(d.list.size() > 15);
        pagingTabel.setParent(card);
        Common.initPagingCustom(pagingTabel, new EventListener() {
            public void onEvent(Event e) throws Exception { refreshTabel(); }
        }, 15);

        gridTabel = new MyGrid();
        gridTabel.setSclass("dgrid");
        gridTabel.setFixedLayout(true);
        gridTabel.setWidth("100%");
        gridTabel.setParent(card);

        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(gridTabel);
        buatKolom(cols, "No",        "28px");
        buatKolom(cols, "Tanggal",   "18%");
        buatKolom(cols, "Kegiatan",  "25%");
        buatKolom(cols, "Waktu",     "10%");
        buatKolom(cols, "Tempat",    "15%");
        buatKolom(cols, "Pelaksana", "12%");
        buatKolom(cols, "Status",    "10%");

        pagingTabel.setPageSize(15);
        pagingTabel.setTotalSize(d.list.size());
        refreshTabel();
    }

    private void buatKolom(org.zkoss.zul.Columns parent, String label, String width) {
        MyColumnConfig col = new MyColumnConfig();
        col.setLabel(label);
        col.setWidth(width);
        col.setParent(parent);
    }

    private void refreshTabel() {
        if (gridTabel == null || lastList == null) return;
        int page = pagingTabel != null ? pagingTabel.getActivePage() : 0;
        int ps   = 15;
        List<JadwalKegiatanKampus> sub = lastList.size() > ps
            ? lastList.subList(page * ps, Math.min((page + 1) * ps, lastList.size()))
            : lastList;
        gridTabel.setRowRenderer(new TabelRenderer());
        gridTabel.setModelCheckMobile(new SimpleListModel(sub));
    }

    /**
     * Renderer lokal untuk layar/komponen {@link DasbordInfoKegiatan}. Kelas ini menerjemahkan satu item data
     * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasbordInfoKegiatan} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
     * implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int no}; operasi lokal: {@code
     * render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see DasbordInfoKegiatan
     */
    private class TabelRenderer extends MyRowRenderer {
        private int no = (pagingTabel == null ? 0 : pagingTabel.getActivePage()) * 15;
        public void render(Row row, Object obj) throws Exception {
            row.setValign("middle");
            JadwalKegiatanKampus kg = (JadwalKegiatanKampus) obj;
            no++;
            String status = safeStatus(kg);
            row.setStyle(warnaRow(status));

            new Label(String.valueOf(no)).setParent(row);
            new Label(formatTanggal(kg)).setParent(row);
            new Label(safeStr(kg.getNama())).setParent(row);
            new Label(formatWaktu(kg)).setParent(row);
            new Label(safeStr(kg.getTempat())).setParent(row);
            new Label(safeStr(kg.getPelaksana())).setParent(row);

            Div badge = new Div();
            badge.setSclass("ik-badge");
            badge.setStyle("background:" + warnaStatus(status) + "22;color:"
                + warnaStatus(status) + ";border:1px solid " + warnaStatus(status) + "44;");
            new Label(status).setParent(badge);
            badge.setParent(row);
        }
    }

    // ================================================================
    // Download Excel
    // ================================================================

    private void downloadExcel() {
        if (lastList == null || lastList.isEmpty()) {
            try { MyMessageboxConfig.show("Mohon maaf, tidak terdapat data yang dapat diunduh."); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return;
        }
        try {
            String path = Executions.getCurrent().getDesktop().getWebApp()
                .getRealPath("/tmp/info_kegiatan_" + System.currentTimeMillis() + ".xlsx");
            File file = new File(path);
            file.getParentFile().mkdirs();
            file.createNewFile();

            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFCellStyle hStyle = wb.createCellStyle();
            XSSFFont hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(15, 81, 50)));
            hStyle.setFillPattern(
                org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            XSSFSheet sheet = wb.createSheet("Info Kegiatan");
            sheet.setDefaultColumnWidth(22);

            String[] headers = {"No", "Tanggal", "Nama Kegiatan", "Waktu",
                "Tempat", "Pelaksana", "Peserta", "Narasumber", "Keterangan", "Status"};
            XSSFRow head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell c = head.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
            }
            sheet.setColumnWidth(2, 10000);
            sheet.setColumnWidth(8, 10000);

            int r = 1;
            for (JadwalKegiatanKampus kg : lastList) {
                XSSFRow row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 1);
                row.createCell(1).setCellValue(formatTanggal(kg));
                row.createCell(2).setCellValue(safeStr(kg.getNama()));
                row.createCell(3).setCellValue(formatWaktu(kg));
                row.createCell(4).setCellValue(safeStr(kg.getTempat()));
                row.createCell(5).setCellValue(safeStr(kg.getPelaksana()));
                row.createCell(6).setCellValue(safeStr(kg.getPeserta()));
                row.createCell(7).setCellValue(safeStr(kg.getNarasumber()));
                row.createCell(8).setCellValue(safeStr(kg.getKeterangan()));
                row.createCell(9).setCellValue(safeStatus(kg));
            }

            FileOutputStream fos = new FileOutputStream(file);
            wb.write(fos);
            fos.close();
            wb.close();

            org.zkoss.zul.Filedownload.save(
                new FileInputStream(file),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "InfoKegiatan.xlsx");

        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ================================================================
    // Helpers — layout
    // ================================================================

    private Div flexRow(String extraStyle) {
        Div r = new Div();
        r.setStyle("display:flex;gap:12px;flex-wrap:wrap;" + extraStyle);
        r.setParent(this);
        return r;
    }

    private Div flexRowInto(Div parent, String extraStyle) {
        Div r = new Div();
        r.setStyle("display:flex;gap:10px;flex-wrap:wrap;" + extraStyle);
        r.setParent(parent);
        return r;
    }

    private Div colDiv(Div parent, String flex) {
        Div c = new Div();
        c.setStyle(flex + " min-width:0;");
        c.setParent(parent);
        return c;
    }

    private Div buatCard(Component parent, String extraStyle) {
        Div card = new Div();
        card.setSclass("ik-card");
        card.setStyle(extraStyle);
        card.setParent(parent);
        return card;
    }

    // ================================================================
    // Helpers — data
    // ================================================================

    private static String safeStatus(JadwalKegiatanKampus kg) {
        try { return kg.ambilStatus(); } catch (Exception e) { return ""; }
    }

    private static String warnaStatus(String status) {
        if ("Berlangsung".equals(status))    return CLR_BERLANGSUNG;
        if ("Belum Mulai".equals(status))    return CLR_BELUM_MULAI;
        if ("Sudah Selesai".equals(status))  return CLR_SELESAI;
        return "#94a3b8";
    }

    private static String warnaRow(String status) {
        if ("Berlangsung".equals(status))   return "background:#fff7ed;";
        if ("Belum Mulai".equals(status))   return "background:#eff6ff;";
        if ("Sudah Selesai".equals(status)) return "background:#f8fafc;";
        return "";
    }

    private static String formatTanggal(JadwalKegiatanKampus kg) {
        if (kg.getTanggalMulai() == null) return "";
        String s = Common.dateFormat4.get().format(kg.getTanggalMulai());
        if (kg.getTanggalSelesai() != null
            && !kg.getTanggalSelesai().equals(kg.getTanggalMulai())) {
            s += " s.d " + Common.dateFormat4.get().format(kg.getTanggalSelesai());
        }
        return s;
    }

    private static String formatWaktu(JadwalKegiatanKampus kg) {
        if (kg.getWaktuMulai() == null) return "";
        String s = Common.timeFormat.get().format(kg.getWaktuMulai());
        if (kg.getWaktuSelesai() != null) {
            s += " - " + Common.timeFormat.get().format(kg.getWaktuSelesai());
        }
        return s;
    }

    // ================================================================
    // Helpers — HTML
    // ================================================================

    private static void appendHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
