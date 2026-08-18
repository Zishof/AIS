package ais.action.master.kalender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KalenderAkademik;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor Kalender Akademik — tampilan lengkap jadwal kegiatan kampus:
 * ringkasan status, linimasa kegiatan, distribusi per bulan, kategori,
 * dan tabel data dengan unduhan Excel.
 *
 * Pola: extends Div, lazy-load via timer, Hibernate langsung.
 * Mengikuti pola DasboardSPMI.
 */
public class DasbordKalenderAkademik extends Div {

    private static final long serialVersionUID = 1L;

    private static final String[] NAMA_BULAN = {
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
    };

    // ---- warna status ----
    private static final String CLR_BERLANGSUNG = "#f97316";
    private static final String CLR_BELUM_MULAI = "var(--ais-theme-primary,#3b82f6)";
    private static final String CLR_SELESAI     = "#64748b";
    private static final String CLR_HEADER      = "#1e3a5f";

    // ---- filter state ----
    private String filterTa           = Common.getCurrentTahunAkademik();
    private String filterGanjilGenap  = "";

    private Combobox cbTa;
    private Combobox cbGanjilGenap;

    // ---- paging tabel ----
    private Paging pagingTabel;
    private MyGrid  gridTabel;
    private List<KalenderAkademik> lastList = new ArrayList<KalenderAkademik>();

    // ================================================================
    // Data container
    // ================================================================

    private static class KaData {
        List<KalenderAkademik> list = new ArrayList<KalenderAkademik>();
        int total, berlangsung, belumMulai, selesai;
        // bulan (Jan..Des) -> jumlah kegiatan mulai di bulan itu
        Map<String, Integer> perBulan  = new LinkedHashMap<String, Integer>();
        // jenis kegiatan -> jumlah
        Map<String, Integer> perJenis  = new LinkedHashMap<String, Integer>();
        // status -> jumlah
        Map<String, Integer> perStatus = new LinkedHashMap<String, Integer>();
    }

    // ================================================================
    // Constructor
    // ================================================================

    public DasbordKalenderAkademik() {
        setWidth("100%");
        setStyle("min-height:300px; background:#f1f5f9; padding:14px 16px;"
               + " box-sizing:border-box; overflow:auto;");
        try {
            tampilLoading();
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event e) throws Exception {
                    renderAll();
                }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ================================================================
    // Loading skeleton
    // ================================================================

    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:80px 0; text-align:center;'>"
            + "<div style='font-size:40px; margin-bottom:16px; "
            +      "animation:ka-spin 1.5s linear infinite; display:inline-block;'>&#128197;</div>"
            + "<div style='font-size:15px; font-weight:800; color:#334155;'>"
            +      "Memuat Kalender Akademik&#8230;</div>"
            + "<div style='margin-top:8px; font-size:12px; color:#94a3b8;'>"
            +      "Menyiapkan jadwal kegiatan, status, dan grafik semester ini.</div>"
            + "</div>"
            + "<style>@keyframes ka-spin{to{transform:rotate(360deg)}}</style>");
    }

    // ================================================================
    // Main render  (dipanggil sekali dan setiap filter berubah)
    // ================================================================

    private void renderAll() {
        try {
            KaData d = loadDataWithCache();
            Common.clear(this);

            renderCss();
            renderFilter();
            renderRingkasan(d);
            renderTimeline(d);

            Div row3 = flexRow("margin-bottom:12px;");
            renderStatusDonut(colDiv(row3, "flex:1 1 220px; min-width:200px;"), d);
            renderBarBulan(colDiv(row3, "flex:2 1 300px; min-width:260px;"), d);
            renderRadarJenis(colDiv(row3, "flex:1 1 220px; min-width:200px;"), d);

            renderTabel(d);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ================================================================
    // Data queries
    // ================================================================

    private KaData loadDataWithCache() {
        String fp = (filterTa != null ? filterTa : "all")
                  + "_" + (filterGanjilGenap != null && !filterGanjilGenap.isEmpty() ? filterGanjilGenap : "all");
        String key = DashboardCacheUtil.keyWithFilter("DasbordKalenderAkademik", "ADMIN", null, fp);
        Object fromL2 = DashboardCacheUtil.getL2(key);
        if (fromL2 instanceof KaData) return (KaData) fromL2;
        Object fromL3 = DashboardCacheUtil.getL3(key);
        if (fromL3 instanceof KaData) {
            DashboardCacheUtil.putL2(key, fromL3);
            return (KaData) fromL3;
        }
        KaData d = loadData();
        DashboardCacheUtil.putL2(key, d);
        DashboardCacheUtil.putL3(key, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private KaData loadData() {
        KaData d = new KaData();
        Criteria base = HibernateUtil.currentSession()
            .createCriteria(KalenderAkademik.class)
            .add(Restrictions.or(
                Restrictions.isNull("aktif"),
                Restrictions.eq("aktif", Boolean.TRUE)));

        if (filterTa != null && !filterTa.isEmpty()) {
            base.add(Restrictions.eq("tahunAjaran", filterTa));
        }
        if (filterGanjilGenap != null && !filterGanjilGenap.isEmpty()) {
            base.add(Restrictions.eq("ganjilGenap", filterGanjilGenap));
        }
        base.addOrder(Order.asc("tanggalMulai"));

        d.list = ConstantValues.simpleList(base, KalenderAkademik.class);

        // inisialisasi perBulan dengan urutan yang benar
        for (String b : NAMA_BULAN) d.perBulan.put(b, 0);

        for (KalenderAkademik ka : d.list) {
            d.total++;
            String status = safeStatus(ka);
            if ("Berlangsung".equals(status))   d.berlangsung++;
            else if ("Belum Mulai".equals(status)) d.belumMulai++;
            else if ("Sudah Selesai".equals(status)) d.selesai++;

            Integer now = d.perStatus.get(status);
            d.perStatus.put(status, now == null ? 1 : now + 1);

            if (ka.getTanggalMulai() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(ka.getTanggalMulai());
                String bulan = NAMA_BULAN[cal.get(Calendar.MONTH)];
                Integer nb = d.perBulan.get(bulan);
                d.perBulan.put(bulan, nb == null ? 1 : nb + 1);
            }

            String jenis = "";
            try {
                jenis = ka.getJenisKegiatan() != null && ka.getJenisKegiatan().getNama() != null
                        ? ka.getJenisKegiatan().getNama() : "Umum";
            } catch (Exception ignored) { jenis = "Umum"; }
            if (jenis.isEmpty()) jenis = "Umum";
            Integer nj = d.perJenis.get(jenis);
            d.perJenis.put(jenis, nj == null ? 1 : nj + 1);
        }

        lastList = d.list;
        return d;
    }

    // ================================================================
    // CSS global dasbor
    // ================================================================

    private void renderCss() {
        appendHtml(this,
            "<style>"
            + ".ka-card{background:#fff;border-radius:14px;padding:14px 16px;"
            +   "box-shadow:0 2px 8px rgba(0,0,0,.06);box-sizing:border-box;}"
            + ".ka-card-title{font-size:11px;font-weight:700;color:#64748b;"
            +   "text-transform:uppercase;letter-spacing:.04em;margin-bottom:4px;}"
            + ".ka-card-val{font-size:30px;font-weight:900;line-height:1;}"
            + ".ka-card-sub{font-size:11px;color:#94a3b8;margin-top:4px;}"
            + ".ka-section-label{font-size:11px;font-weight:800;color:#475569;"
            +   "text-transform:uppercase;letter-spacing:.05em;margin-bottom:6px;}"
            + ".ka-desc{font-size:11px;color:#94a3b8;margin-bottom:10px;}"
            + ".ka-tl-bar{height:20px;border-radius:6px;display:flex;align-items:center;"
            +   "padding:0 6px;font-size:10px;font-weight:700;color:#fff;"
            +   "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;"
            +   "box-sizing:border-box;min-width:4px;cursor:default;"
            +   "transition:filter .15s;}"
            + ".ka-tl-bar:hover{filter:brightness(1.1);}"
            + ".ka-donut-wrap{position:relative;width:140px;height:140px;"
            +   "margin:0 auto 8px;}"
            + ".ka-donut-center{position:absolute;top:50%;left:50%;"
            +   "transform:translate(-50%,-50%);text-align:center;}"
            + ".ka-bar-row{display:flex;align-items:center;gap:6px;"
            +   "margin-bottom:5px;font-size:11px;}"
            + ".ka-bar-fill{height:14px;border-radius:4px;min-width:2px;"
            +   "transition:width .3s;}"
            + ".ka-spider-label{font-size:9px;fill:#475569;}"
            + ".ka-tbl-row-alt{background:#f8fafc;}"
            + ".ka-badge{display:inline-block;padding:2px 8px;border-radius:20px;"
            +   "font-size:10px;font-weight:700;}"
            + "@media(max-width:640px){"
            +   ".ka-card-val{font-size:22px;}"
            +   ".ka-tl-bar{height:16px;font-size:9px;}"
            + "}"
            + "</style>");
    }

    // ================================================================
    // Filter bar
    // ================================================================

    private void renderFilter() {
        Div bar = new Div();
        bar.setStyle("background:#fff;border-radius:14px;padding:12px 16px;"
                   + "box-shadow:0 2px 8px rgba(0,0,0,.06);margin-bottom:14px;"
                   + "display:flex;align-items:center;gap:10px;flex-wrap:wrap;");
        bar.setParent(this);

        appendHtml(bar, "<span style='font-size:12px;font-weight:700;color:#475569;'>Filter:</span>");

        appendHtml(bar, "<span style='font-size:11px;color:#64748b;'>Tahun Akademik</span>");
        cbTa = new Combobox();
        cbTa.setWidth("160px");
        cbTa.setReadonly(true);
        cbTa.setParent(bar);
        Common.generateTahunAjaranDanSemua(cbTa);
        if (filterTa != null && !filterTa.isEmpty()) {
            Common.selectComboItem(cbTa, filterTa);
        }
        if (cbTa.getSelectedItem() == null) cbTa.setSelectedIndex(0);

        appendHtml(bar, "<span style='font-size:11px;color:#64748b;'>Semester</span>");
        cbGanjilGenap = new Combobox();
        cbGanjilGenap.setWidth("110px");
        cbGanjilGenap.setReadonly(true);
        cbGanjilGenap.setParent(bar);
        for (String[] o : new String[][]{{"", "Semua"}, {"Ganjil", "Ganjil"}, {"Genap", "Genap"}}) {
            Comboitem ci = new MyComboitemConfig();
            ci.setLabel(o[1]);
            ci.setValue(o[0]);
            cbGanjilGenap.appendChild(ci);
        }
        cbGanjilGenap.setSelectedIndex(
            "Ganjil".equals(filterGanjilGenap) ? 1
            : "Genap".equals(filterGanjilGenap) ? 2 : 0);

        MyToolbarbuttonConfig btnTampilkan = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
        btnTampilkan.setParent(bar);
        btnTampilkan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                bacaFilter();
                tampilLoading();
                Common.createDefaultTimer(new EventListener() {
                    public void onEvent(Event e2) throws Exception { renderAll(); }
                });
            }
        });

        // Tombol download Excel
        MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig(
            "Unduh Excel", "/img/excel.gif");
        btnExcel.setParent(bar);
        btnExcel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                downloadExcel();
            }
        });
    }

    private void bacaFilter() {
        if (cbTa != null && cbTa.getSelectedItem() != null) {
            Object v = cbTa.getSelectedItem().getValue();
            filterTa = v == null ? "" : v.toString();
        }
        if (cbGanjilGenap != null && cbGanjilGenap.getSelectedItem() != null) {
            Object v = cbGanjilGenap.getSelectedItem().getValue();
            filterGanjilGenap = v == null ? "" : v.toString();
        }
    }

    // ================================================================
    // Kartu Ringkasan (4 KPI)
    // ================================================================

    private void renderRingkasan(KaData d) {
        Div wrap = new Div();
        wrap.setStyle("margin-bottom:12px;");
        wrap.setParent(this);

        appendHtml(wrap,
            "<div class='ka-section-label'>Ringkasan Kegiatan Akademik</div>"
            + "<div class='ka-desc'>Gambaran keseluruhan kegiatan yang dijadwalkan kampus pada semester ini.</div>");

        Div row = flexRowInto(wrap, "margin-bottom:0;");
        buatKartu(colDiv(row, "flex:1 1 140px; min-width:130px;"),
            "Total Kegiatan", d.total, CLR_HEADER,
            "Jumlah seluruh jadwal yang tercatat");
        buatKartu(colDiv(row, "flex:1 1 140px; min-width:130px;"),
            "Sedang Berlangsung", d.berlangsung, CLR_BERLANGSUNG,
            "Kegiatan yang berjalan hari ini");
        buatKartu(colDiv(row, "flex:1 1 140px; min-width:130px;"),
            "Belum Dimulai", d.belumMulai, CLR_BELUM_MULAI,
            "Kegiatan yang akan datang");
        buatKartu(colDiv(row, "flex:1 1 140px; min-width:130px;"),
            "Sudah Selesai", d.selesai, CLR_SELESAI,
            "Kegiatan yang telah berakhir");
    }

    private void buatKartu(Div parent, String judul, int nilai, String warna, String sub) {
        appendHtml(parent,
            "<div class='ka-card' style='border-top:4px solid " + warna + ";'>"
            + "<div class='ka-card-title'>" + esc(judul) + "</div>"
            + "<div class='ka-card-val' style='color:" + warna + ";'>" + nilai + "</div>"
            + "<div class='ka-card-sub'>" + esc(sub) + "</div>"
            + "</div>");
    }

    // ================================================================
    // Linimasa Kegiatan (Gantt horizontal)
    // ================================================================

    private void renderTimeline(KaData d) {
        Div card = buatCard(this, "margin-bottom:12px;");
        appendHtml(card,
            "<div class='ka-section-label'>Linimasa Kegiatan Semester Ini</div>"
            + "<div class='ka-desc'>Urutan dan durasi setiap kegiatan akademik dari awal hingga akhir semester, "
            +      "tersusun berdasarkan waktu. Warna menunjukkan status saat ini.</div>");

        if (d.list.isEmpty()) {
            appendHtml(card, "<div style='color:#94a3b8;font-size:12px;padding:20px 0;text-align:center;'>"
                + "Tidak ada data kegiatan untuk filter yang dipilih.</div>");
            return;
        }

        // Hitung rentang waktu keseluruhan
        Date tMin = null, tMax = null;
        for (KalenderAkademik ka : d.list) {
            if (tMin == null || ka.getTanggalMulai().before(tMin))   tMin = ka.getTanggalMulai();
            if (tMax == null || ka.getTanggalSelesai().after(tMax))  tMax = ka.getTanggalSelesai();
        }
        long span = tMax.getTime() - tMin.getTime();
        if (span <= 0) span = 1;

        // Header bulan
        StringBuilder tlHtml = new StringBuilder();
        tlHtml.append("<div style='overflow-x:auto;'>");
        tlHtml.append("<div style='min-width:500px;'>");
        tlHtml.append("<div style='display:flex;margin-bottom:4px;"
            + "font-size:10px;color:#94a3b8;font-weight:600;'>");
        tlHtml.append("<div style='width:160px;flex-shrink:0;'>Kegiatan</div>");
        tlHtml.append("<div style='flex:1;position:relative;height:16px;'>");
        // Render tick marks per bulan
        Calendar tickCal = Calendar.getInstance();
        tickCal.setTime(tMin);
        tickCal.set(Calendar.DAY_OF_MONTH, 1);
        while (!tickCal.getTime().after(tMax)) {
            long pos = tickCal.getTime().getTime() - tMin.getTime();
            double pct = (pos * 100.0) / span;
            if (pct >= 0 && pct <= 100) {
                int bulanIdx = tickCal.get(Calendar.MONTH);
                tlHtml.append("<span style='position:absolute;left:")
                      .append(String.format("%.1f", pct))
                      .append("%;transform:translateX(-50%);white-space:nowrap;'>")
                      .append(NAMA_BULAN[bulanIdx]).append("</span>");
            }
            tickCal.add(Calendar.MONTH, 1);
        }
        tlHtml.append("</div></div>");

        // Baris per kegiatan
        List<KalenderAkademik> sample = d.list.size() > 20
            ? d.list.subList(0, 20) : d.list;
        for (KalenderAkademik ka : sample) {
            String status = safeStatus(ka);
            String warna  = warnaStatus(status);
            double left   = ((ka.getTanggalMulai().getTime() - tMin.getTime()) * 100.0) / span;
            double width  = Math.max(0.5,
                ((ka.getTanggalSelesai().getTime() - ka.getTanggalMulai().getTime()) * 100.0) / span);
            String nama = ka.getNamaKegiatanAkademik() == null ? "" : ka.getNamaKegiatanAkademik();

            tlHtml.append("<div style='display:flex;align-items:center;margin-bottom:4px;'>");
            tlHtml.append("<div style='width:160px;flex-shrink:0;font-size:10px;color:#334155;"
                + "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;padding-right:8px;"
                + "' title='").append(esc(nama)).append("'>")
                .append(esc(truncate(nama, 24))).append("</div>");
            tlHtml.append("<div style='flex:1;position:relative;height:20px;background:#f1f5f9;"
                + "border-radius:4px;'>");
            tlHtml.append("<div class='ka-tl-bar' style='position:absolute;left:")
                  .append(String.format("%.2f", left)).append("%;width:")
                  .append(String.format("%.2f", width)).append("%;background:")
                  .append(warna).append(";' title='")
                  .append(esc(status)).append("'>")
                  .append(esc(truncate(nama, 18))).append("</div>");
            tlHtml.append("</div></div>");
        }

        if (d.list.size() > 20) {
            tlHtml.append("<div style='font-size:10px;color:#94a3b8;margin-top:4px;'>"
                + "... dan " + (d.list.size() - 20) + " kegiatan lainnya.</div>");
        }

        tlHtml.append("</div></div>"); // min-width, overflow-x
        appendHtml(card, tlHtml.toString());
    }

    // ================================================================
    // Donut chart — distribusi status
    // ================================================================

    private void renderStatusDonut(Div parent, KaData d) {
        Div card = buatCard(parent, "height:100%;");
        appendHtml(card,
            "<div class='ka-section-label'>Distribusi Status</div>"
            + "<div class='ka-desc'>Proporsi kegiatan berdasarkan statusnya saat ini.</div>");

        if (d.total == 0) {
            appendHtml(card, "<div style='color:#94a3b8;font-size:11px;'>Belum ada data.</div>");
            return;
        }

        // CSS conic-gradient donut
        double pctBerlangsung = (d.berlangsung * 100.0) / d.total;
        double pctBelumMulai  = (d.belumMulai  * 100.0) / d.total;
        double pctSelesai     = (d.selesai     * 100.0) / d.total;
        double end1 = pctBerlangsung;
        double end2 = end1 + pctBelumMulai;

        appendHtml(card,
            "<div class='ka-donut-wrap'>"
            + "<div style='width:140px;height:140px;border-radius:50%;"
            +   "background:conic-gradient("
            +   CLR_BERLANGSUNG + " 0% " + String.format("%.1f", end1) + "%,"
            +   CLR_BELUM_MULAI + " " + String.format("%.1f", end1) + "% " + String.format("%.1f", end2) + "%,"
            +   CLR_SELESAI     + " " + String.format("%.1f", end2) + "% 100%);"
            +   "-webkit-mask:radial-gradient(farthest-side,transparent 55%,#000 56%);"
            +   "mask:radial-gradient(farthest-side,transparent 55%,#000 56%);"
            + "'></div>"
            + "<div class='ka-donut-center'>"
            +   "<div style='font-size:18px;font-weight:900;color:#1e3a5f;'>" + d.total + "</div>"
            +   "<div style='font-size:9px;color:#64748b;'>Total</div>"
            + "</div>"
            + "</div>"
            // Legenda
            + "<div style='font-size:11px;'>"
            + legendItem(CLR_BERLANGSUNG, "Berlangsung", d.berlangsung, d.total)
            + legendItem(CLR_BELUM_MULAI, "Belum Mulai", d.belumMulai, d.total)
            + legendItem(CLR_SELESAI,     "Sudah Selesai", d.selesai, d.total)
            + "</div>");
    }

    private String legendItem(String warna, String label, int nilai, int total) {
        int pct = total == 0 ? 0 : (nilai * 100 / total);
        return "<div style='display:flex;align-items:center;gap:6px;margin-bottom:4px;'>"
            + "<div style='width:10px;height:10px;border-radius:3px;background:" + warna + ";flex-shrink:0;'></div>"
            + "<span style='color:#334155;'>" + esc(label) + "</span>"
            + "<span style='margin-left:auto;font-weight:700;color:#1e3a5f;'>" + nilai
            + " <span style='color:#94a3b8;font-weight:400;'>(" + pct + "%)</span></span>"
            + "</div>";
    }

    // ================================================================
    // Bar chart — kegiatan per bulan
    // ================================================================

    private void renderBarBulan(Div parent, KaData d) {
        Div card = buatCard(parent, "height:100%;");
        appendHtml(card,
            "<div class='ka-section-label'>Kegiatan per Bulan</div>"
            + "<div class='ka-desc'>Berapa banyak kegiatan yang dimulai setiap bulan — "
            +      "membantu Anda melihat bulan-bulan tersibuk dalam semester ini.</div>");

        int maxVal = 0;
        for (Map.Entry<String, Integer> e : d.perBulan.entrySet()) {
            if (e.getValue() > maxVal) maxVal = e.getValue();
        }
        if (maxVal == 0) maxVal = 1;

        // Bar chart horizontal
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-top:8px;'>");
        for (Map.Entry<String, Integer> e : d.perBulan.entrySet()) {
            int val = e.getValue();
            if (val == 0) continue; // sembunyikan bulan kosong
            double pct = (val * 80.0) / maxVal;
            sb.append("<div class='ka-bar-row'>");
            sb.append("<span style='width:28px;color:#64748b;flex-shrink:0;'>")
              .append(esc(e.getKey())).append("</span>");
            sb.append("<div class='ka-bar-fill' style='width:").append(String.format("%.1f", pct))
              .append("%;background:").append(CLR_HEADER).append(";'></div>");
            sb.append("<span style='color:#1e3a5f;font-weight:700;'>").append(val).append("</span>");
            sb.append("</div>");
        }
        sb.append("</div>");
        appendHtml(card, sb.toString());
    }

    // ================================================================
    // Radar / Spider chart — distribusi jenis kegiatan (SVG)
    // ================================================================

    private void renderRadarJenis(Div parent, KaData d) {
        Div card = buatCard(parent, "height:100%;");
        appendHtml(card,
            "<div class='ka-section-label'>Radar Jenis Kegiatan</div>"
            + "<div class='ka-desc'>Sebaran jenis atau kategori kegiatan akademik — "
            +      "semakin besar bidang, semakin banyak kegiatan di kategori tersebut.</div>");

        // Ambil max 8 jenis teratas
        List<Map.Entry<String, Integer>> jenisTop = new ArrayList<Map.Entry<String, Integer>>(
            d.perJenis.entrySet());
        Collections.sort(jenisTop, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        if (jenisTop.size() > 8) jenisTop = jenisTop.subList(0, 8);

        if (jenisTop.isEmpty()) {
            appendHtml(card, "<div style='color:#94a3b8;font-size:11px;'>Belum ada data jenis kegiatan.</div>");
            return;
        }

        int n = jenisTop.size();
        int maxV = 0;
        for (Map.Entry<String, Integer> e : jenisTop) {
            if (e.getValue() > maxV) maxV = e.getValue();
        }
        if (maxV == 0) maxV = 1;

        // SVG polygon radar
        int cx = 90, cy = 85, r = 60;
        StringBuilder svgSb = new StringBuilder();
        svgSb.append("<svg width='180' height='170' viewBox='0 0 180 170' "
            + "style='display:block;margin:0 auto;overflow:visible;'>");

        // Grid circles
        for (int gr = 1; gr <= 4; gr++) {
            double gr2 = r * gr / 4.0;
            svgSb.append("<circle cx='").append(cx).append("' cy='").append(cy)
                 .append("' r='").append(String.format("%.1f", gr2))
                 .append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
        }

        // Axes & labels
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i / n) - (Math.PI / 2);
            double lx = cx + r * Math.cos(angle);
            double ly = cy + r * Math.sin(angle);
            svgSb.append("<line x1='").append(cx).append("' y1='").append(cy)
                 .append("' x2='").append(String.format("%.1f", lx))
                 .append("' y2='").append(String.format("%.1f", ly))
                 .append("' stroke='#cbd5e1' stroke-width='1'/>");
            // Label jarak lebih jauh
            double lblX = cx + (r + 16) * Math.cos(angle);
            double lblY = cy + (r + 16) * Math.sin(angle);
            String anchor = Math.cos(angle) > 0.1 ? "start" : (Math.cos(angle) < -0.1 ? "end" : "middle");
            String jNama  = jenisTop.get(i).getKey();
            svgSb.append("<text x='").append(String.format("%.1f", lblX))
                 .append("' y='").append(String.format("%.1f", lblY + 3))
                 .append("' class='ka-spider-label' text-anchor='").append(anchor)
                 .append("'>").append(esc(truncate(jNama, 10))).append("</text>");
        }

        // Data polygon
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i / n) - (Math.PI / 2);
            double ratio = jenisTop.get(i).getValue() * 1.0 / maxV;
            double px = cx + r * ratio * Math.cos(angle);
            double py = cy + r * ratio * Math.sin(angle);
            if (i > 0) pts.append(" ");
            pts.append(String.format("%.1f", px)).append(",")
               .append(String.format("%.1f", py));
        }
        svgSb.append("<polygon points='").append(pts)
             .append("' fill='").append(CLR_HEADER)
             .append("33' stroke='").append(CLR_HEADER)
             .append("' stroke-width='1.5'/>");

        svgSb.append("</svg>");
        appendHtml(card, svgSb.toString());
    }

    // ================================================================
    // Tabel Data Lengkap (ZK Grid + paging)
    // ================================================================

    private void renderTabel(KaData d) {
        Div card = buatCard(this, "margin-bottom:12px;");
        appendHtml(card,
            "<div class='ka-section-label'>Daftar Lengkap Kegiatan Akademik</div>"
            + "<div class='ka-desc'>Seluruh kegiatan yang dijadwalkan berikut tanggal, "
            +      "durasi, dan statusnya — klik judul kolom untuk mengurutkan.</div>");

        if (d.list.isEmpty()) {
            appendHtml(card,
                "<div style='text-align:center;color:#94a3b8;font-size:12px;padding:24px 0;'>"
                + "&#128197; Tidak ada kegiatan ditemukan untuk filter yang dipilih.</div>");
            return;
        }

        // Paging
        pagingTabel = new Paging();
        pagingTabel.setDetailed(true);
        pagingTabel.setVisible(d.list.size() > 15);
        pagingTabel.setParent(card);
        Common.initPagingCustom(pagingTabel, new EventListener() {
            public void onEvent(Event e) throws Exception { refreshTabel(); }
        }, 15);

        // Grid
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
        buatKolom(cols, "Keterangan","20%");
        buatKolom(cols, "TA",        "8%");
        buatKolom(cols, "Semester",  "8%");
        buatKolom(cols, "Lama",      "7%");
        buatKolom(cols, "Status",    "11%");

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
        List<KalenderAkademik> sub = lastList.size() > ps
            ? lastList.subList(page * ps, Math.min((page + 1) * ps, lastList.size()))
            : lastList;
        ListModel model = new SimpleListModel(sub);
        gridTabel.setRowRenderer(new TabelRenderer());
        gridTabel.setModelCheckMobile(model);
    }

    private class TabelRenderer extends MyRowRenderer {
        private int no = (pagingTabel == null ? 0 : pagingTabel.getActivePage()) * 15;
        public void render(Row row, Object obj) throws Exception {
            row.setValign("middle");
            KalenderAkademik ka = (KalenderAkademik) obj;
            no++;
            String status = safeStatus(ka);
            row.setStyle(warnaRowStatus(status));

            // No
            new Label(String.valueOf(no)).setParent(row);
            // Tanggal
            String tgl = formatTanggal(ka);
            new Label(tgl).setParent(row);
            // Kegiatan
            new Label(safeStr(ka.getNamaKegiatanAkademik())).setParent(row);
            // Keterangan
            new Label(safeStr(ka.getDeskripsiKegiatanAkademik())).setParent(row);
            // TA
            new Label(safeStr(ka.getTahunAjaran())).setParent(row);
            // Semester
            new Label(safeStr(ka.getGanjilGenap())).setParent(row);
            // Lama
            new Label(ka.getJumlahHari() + " Hari").setParent(row);
            // Status badge
            Div badge = new Div();
            badge.setSclass("ka-badge");
            badge.setStyle("background:" + warnaStatus(status) + "22;"
                + "color:" + warnaStatus(status) + ";border:1px solid " + warnaStatus(status) + "44;");
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
                .getRealPath("/tmp/kalender_akademik_" + System.currentTimeMillis() + ".xlsx");
            File file = new File(path);
            file.getParentFile().mkdirs();
            file.createNewFile();

            XSSFWorkbook wb = new XSSFWorkbook();

            // ---- Style header ----
            XSSFCellStyle hStyle = wb.createCellStyle();
            XSSFFont hFont = wb.createFont();
            hFont.setBold(true);
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(30, 58, 95)));
            hStyle.setFillPattern(
                org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            XSSFFont hFontW = wb.createFont();
            hFontW.setBold(true);
            hFontW.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFontW);

            // ---- Sheet 1: Data ----
            XSSFSheet sheet = wb.createSheet("Kalender Akademik");
            sheet.setDefaultColumnWidth(20);

            String[] headers = {"No", "Tanggal", "Nama Kegiatan", "Keterangan",
                "Tahun Akademik", "Semester", "Lama (Hari)", "Status"};
            XSSFRow head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell c = head.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
            }
            sheet.setColumnWidth(0, 2000);
            sheet.setColumnWidth(1, 6000);
            sheet.setColumnWidth(2, 10000);
            sheet.setColumnWidth(3, 10000);

            int r = 1;
            for (KalenderAkademik ka : lastList) {
                XSSFRow row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 1);
                row.createCell(1).setCellValue(formatTanggal(ka));
                row.createCell(2).setCellValue(safeStr(ka.getNamaKegiatanAkademik()));
                row.createCell(3).setCellValue(safeStr(ka.getDeskripsiKegiatanAkademik()));
                row.createCell(4).setCellValue(safeStr(ka.getTahunAjaran()));
                row.createCell(5).setCellValue(safeStr(ka.getGanjilGenap()));
                row.createCell(6).setCellValue(ka.getJumlahHari() == null ? 0 : ka.getJumlahHari());
                row.createCell(7).setCellValue(safeStatus(ka));
            }

            // ---- Sheet 2: Ringkasan ----
            XSSFSheet sSummary = wb.createSheet("Ringkasan");
            sSummary.setDefaultColumnWidth(25);
            String[][] summary = {
                {"Total Kegiatan",      String.valueOf(lastList.size())},
                {"Sedang Berlangsung",  String.valueOf(countStatus("Berlangsung"))},
                {"Belum Dimulai",       String.valueOf(countStatus("Belum Mulai"))},
                {"Sudah Selesai",       String.valueOf(countStatus("Sudah Selesai"))},
            };
            XSSFRow sHead = sSummary.createRow(0);
            sHead.createCell(0).setCellValue("Keterangan");
            sHead.createCell(1).setCellValue("Jumlah");
            sHead.getCell(0).setCellStyle(hStyle);
            sHead.getCell(1).setCellStyle(hStyle);
            int sr = 1;
            for (String[] pair : summary) {
                XSSFRow sRow = sSummary.createRow(sr++);
                sRow.createCell(0).setCellValue(pair[0]);
                sRow.createCell(1).setCellValue(pair[1]);
            }

            FileOutputStream fos = new FileOutputStream(file);
            wb.write(fos);
            fos.close();
            wb.close();

            org.zkoss.zul.Filedownload.save(
                new FileInputStream(file),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "KalenderAkademik_" + filterTa.replace("/", "-") + ".xlsx");

        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    private int countStatus(String status) {
        int n = 0;
        for (KalenderAkademik ka : lastList) {
            if (status.equals(safeStatus(ka))) n++;
        }
        return n;
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
        card.setSclass("ka-card");
        card.setStyle(extraStyle);
        card.setParent(parent);
        return card;
    }

    // ================================================================
    // Helpers — data
    // ================================================================

    private static String safeStatus(KalenderAkademik ka) {
        try { return ka.getStatus(); } catch (Exception e) { return ""; }
    }

    private static String warnaStatus(String status) {
        if ("Berlangsung".equals(status))  return CLR_BERLANGSUNG;
        if ("Belum Mulai".equals(status))  return CLR_BELUM_MULAI;
        if ("Sudah Selesai".equals(status)) return CLR_SELESAI;
        return "#94a3b8";
    }

    private static String warnaRowStatus(String status) {
        if ("Berlangsung".equals(status))   return "background:#fff7ed;";
        if ("Belum Mulai".equals(status))   return "background:#eff6ff;";
        if ("Sudah Selesai".equals(status)) return "background:#f8fafc;";
        return "";
    }

    private static String formatTanggal(KalenderAkademik ka) {
        if (ka.getTanggalMulai() == null) return "";
        String s = Common.dateFormat4.get().format(ka.getTanggalMulai());
        if (ka.getTanggalSelesai() != null
            && !ka.getTanggalSelesai().equals(ka.getTanggalMulai())) {
            s += " s.d " + Common.dateFormat4.get().format(ka.getTanggalSelesai());
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
