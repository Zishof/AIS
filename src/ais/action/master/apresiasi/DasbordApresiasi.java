package ais.action.master.apresiasi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.ApresiasiSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor terpadu untuk apresiasi siswa — menampilkan tren, distribusi jenis,
 * pola harian, dan daftar apresiasi berdasarkan peran pengguna.
 */
public class DasbordApresiasi extends Div {

    private static final long serialVersionUID = 1L;

    public enum Lingkup {
        SEMUA, SISWA;

        public String getNamaModul() {
            switch (this) {
                case SISWA: return "Apresiasi Siswa";
                default:    return "Semua Apresiasi";
            }
        }
    }

    private static final String CLR_PRIMER     = "var(--ais-theme-primary,#7c3aed)";
    private static final String CLR_SUKSES     = "#059669";
    private static final String CLR_PERINGATAN = "#d97706";
    private static final String CLR_BAHAYA     = "#dc2626";
    private static final String CLR_MUTED      = "#64748b";
    private static final String CLR_HEADER     = "#4c1d95";
    private static final String CLR_BG         = "#faf5ff";

    private static final String[] PALET = {
        "var(--ais-theme-primary,#7c3aed)","#a855f7","#ec4899","#f43f5e",
        "#f97316","#eab308","#22c55e","#14b8a6"
    };
    private static final String[] BULAN = {
        "Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agt","Sep","Okt","Nov","Des"
    };
    private static final String[] HARI_MINGGU = {
        "Min","Sen","Sel","Rab","Kam","Jum","Sab"
    };

    private static final int PAGE_SIZE = 15;
    private static final int MAX_ROWS  = 600;

    private final Lingkup    lingkup;
    private Paging           pgTabel;
    private MyGrid           gridTabel;
    private List<AprEntry>   lastList = new ArrayList<AprEntry>();
    private DashData         lastData;

    static final class AprEntry {
        final Date   tanggal;
        final String jenisNama;
        final String subjekNama;
        final String keterangan;
        final String sumber;

        AprEntry(Date tanggal, String jenisNama,
                 String subjekNama, String keterangan, String sumber) {
            this.tanggal    = tanggal;
            this.jenisNama  = safeStr(jenisNama);
            this.subjekNama = safeStr(subjekNama);
            this.keterangan = safeStr(keterangan);
            this.sumber     = safeStr(sumber);
        }
    }

    static final class DashData {
        List<AprEntry>       semua    = new ArrayList<AprEntry>();
        Map<String, Integer> perJenis = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perBulan = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perHari  = new LinkedHashMap<String, Integer>();
        int    total, bulanIni;
        double rataPerBulan;
        String namaRole    = "";
        String namaPengguna = "";
    }

    public DasbordApresiasi() { this(Lingkup.SEMUA); }

    public DasbordApresiasi(Lingkup lingkup) {
        this.lingkup = lingkup != null ? lingkup : Lingkup.SEMUA;
        setWidth("100%");
        setStyle("min-height:300px;background:" + CLR_BG
               + ";padding:12px 14px;box-sizing:border-box;overflow:auto;");
        tampilLoading();
        try {
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event e) throws Exception { renderAll(); }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:80px 0;text-align:center;'>"
            + "<div style='font-size:42px;margin-bottom:16px;"
            +   "animation:da-spin 1.2s linear infinite;display:inline-block;'>&#127942;</div>"
            + "<div style='font-size:15px;font-weight:700;color:" + CLR_HEADER + ";'>"
            +   "Memuat Dasbor Apresiasi&#8230;</div>"
            + "<div style='margin-top:8px;font-size:12px;color:#6b7280;'>"
            +   "Menyiapkan ringkasan, grafik, dan daftar apresiasi.</div>"
            + "</div>"
            + "<style>@keyframes da-spin{to{transform:rotate(360deg)}}</style>");
    }

    private void renderAll() throws Exception {
        DashData d = loadDataWithCache();   // L1→L2→L3→DB
        lastData = d;
        Common.clear(this);
        renderCss();
        renderHeader(d);
        renderKartuRingkasan(d);
        renderTrenBulanan(d);
        renderDistribusiDanRadar(d);
        renderPolaMinggu(d);
        renderCatatanTerbaru(d);
        renderTabelLengkap(d);
    }

    /** Cache L2 (session) + L3 (app-wide) sebelum query ke DB. */
    private DashData loadDataWithCache() {
        Tbmuser user      = Common.getCurrentUser();
        Long    userId    = (user != null) ? user.getId() : null;
        boolean isPersonal = (user != null && (
                user.getSiswa()     != null ||
                user.getMahasiswa() != null ||
                user.ambilGuru()    != null ||
                user.ambilDosen()   != null ||
                user.ambilPegawai() != null));

        String cacheKey = DashboardCacheUtil.key(
                "DasbordApresiasi", lingkup.name(), isPersonal ? userId : null);

        Object fromL2 = DashboardCacheUtil.getL2(cacheKey);
        if (fromL2 instanceof DashData) return (DashData) fromL2;

        if (!isPersonal) {
            Object fromL3 = DashboardCacheUtil.getL3(cacheKey);
            if (fromL3 instanceof DashData) {
                DashboardCacheUtil.putL2(cacheKey, fromL3);
                return (DashData) fromL3;
            }
        }

        DashData d = loadData();
        DashboardCacheUtil.putL2(cacheKey, d);
        if (!isPersonal) DashboardCacheUtil.putL3(cacheKey, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private DashData loadData() {
        DashData d = new DashData();
        Tbmuser user = Common.getCurrentUser();

        Mahasiswa mhs = user != null ? user.getMahasiswa() : null;
        Dosen     dos = user != null ? user.ambilDosen()   : null;
        Siswa     sis = user != null ? user.getSiswa()     : null;
        Guru      gur = user != null ? user.ambilGuru()    : null;
        Pegawai   peg = user != null ? user.ambilPegawai() : null;

        if (sis != null) {
            d.namaRole = "Siswa";     d.namaPengguna = safeStr(sis.getNama());
        } else if (gur != null) {
            d.namaRole = "Guru";      d.namaPengguna = safeStr(gur.getNama());
        } else if (dos != null) {
            d.namaRole = "Dosen";     d.namaPengguna = safeStr(dos.getNama());
        } else if (mhs != null) {
            d.namaRole = "Mahasiswa"; d.namaPengguna = safeStr(mhs.getNama());
        } else if (peg != null) {
            d.namaRole = "Pegawai";   d.namaPengguna = safeStr(peg.getNama());
        } else {
            d.namaRole     = "Administrator";
            d.namaPengguna = user != null ? safeStr(user.getUserNama()) : "";
        }

        Calendar cal = Calendar.getInstance();
        for (int i = 11; i >= 0; i--) {
            Calendar tmp = (Calendar) cal.clone();
            tmp.add(Calendar.MONTH, -i);
            d.perBulan.put(BULAN[tmp.get(Calendar.MONTH)] + " " + tmp.get(Calendar.YEAR), 0);
        }
        for (String h : HARI_MINGGU) d.perHari.put(h, 0);

        try { muatApresiasiSiswa(d, sis); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/apresiasi/DasbordApresiasi.java:237"); /*skip*/ }

        Collections.sort(d.semua, new Comparator<AprEntry>() {
            public int compare(AprEntry a, AprEntry b) {
                if (a.tanggal == null && b.tanggal == null) return 0;
                if (a.tanggal == null) return 1;
                if (b.tanggal == null) return -1;
                return b.tanggal.compareTo(a.tanggal);
            }
        });

        int nowYear = cal.get(Calendar.YEAR), nowMonth = cal.get(Calendar.MONTH);
        for (AprEntry e : d.semua) {
            d.total++;
            String jk = e.jenisNama.isEmpty() ? "(tanpa jenis)" : e.jenisNama;
            Integer prev = d.perJenis.get(jk);
            d.perJenis.put(jk, prev == null ? 1 : prev + 1);
            if (e.tanggal != null) {
                Calendar c = Calendar.getInstance(); c.setTime(e.tanggal);
                String bk = BULAN[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
                if (d.perBulan.containsKey(bk)) d.perBulan.put(bk, d.perBulan.get(bk) + 1);
                if (c.get(Calendar.YEAR) == nowYear && c.get(Calendar.MONTH) == nowMonth) d.bulanIni++;
                String hk = HARI_MINGGU[c.get(Calendar.DAY_OF_WEEK) - 1];
                d.perHari.put(hk, d.perHari.get(hk) + 1);
            }
        }
        int totalNonZero = 0, sumBulan = 0;
        for (int v : d.perBulan.values()) { if (v > 0) { totalNonZero++; sumBulan += v; } }
        d.rataPerBulan = totalNonZero > 0 ? (double) sumBulan / totalNonZero : 0;
        lastList = d.semua;
        return d;
    }

    @SuppressWarnings("unchecked")
    private void muatApresiasiSiswa(DashData d, Siswa sis) {
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(ApresiasiSiswa.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (sis != null) c.add(Restrictions.eq("siswa", sis));
        for (ApresiasiSiswa a : (List<ApresiasiSiswa>) c.list()) {
            String jenis  = a.getApresiasiDanPenghargaan() != null ? a.getApresiasiDanPenghargaan().getNama() : "";
            String subjek = a.getSiswa() != null ? a.getSiswa().getNama() : safeStr(a.getNama());
            d.semua.add(new AprEntry(a.getWaktu(), jenis, subjek, a.getKeterangan(), "Apresiasi Siswa"));
        }
    }

    private void renderCss() {
        appendHtml(this,
            "<style>"
            + ".da-card{background:#fff;border-radius:14px;padding:14px 16px;"
            +   "box-shadow:0 2px 10px rgba(0,0,0,.07);margin-bottom:14px;}"
            + ".da-section-title{font-size:13px;font-weight:800;color:" + CLR_HEADER + ";"
            +   "letter-spacing:.4px;margin-bottom:4px;text-transform:uppercase;}"
            + ".da-section-desc{font-size:12px;color:" + CLR_MUTED + ";margin-bottom:12px;line-height:1.5;}"
            + ".da-stat-val{font-size:28px;font-weight:800;line-height:1.1;}"
            + ".da-stat-label{font-size:11px;font-weight:600;color:" + CLR_MUTED + ";margin-top:2px;}"
            + ".da-stat-sub{font-size:10px;color:#94a3b8;margin-top:3px;}"
            + ".da-flex{display:flex;gap:10px;flex-wrap:wrap;}"
            + ".da-col{min-width:0;flex:1 1 120px;}"
            + ".da-stat-card{background:#fff;border-radius:12px;padding:14px 16px;"
            +   "box-shadow:0 2px 8px rgba(0,0,0,.06);border-top:4px solid;text-align:center;}"
            + ".da-bar-row{display:flex;align-items:center;gap:8px;margin-bottom:6px;font-size:11px;}"
            + ".da-bar-fill{height:18px;border-radius:4px;min-width:2px;}"
            + ".da-bar-label{width:48px;flex-shrink:0;color:" + CLR_MUTED + ";text-align:right;}"
            + ".da-bar-val{min-width:28px;font-weight:700;color:" + CLR_HEADER + ";}"
            + ".da-legend-item{display:flex;align-items:center;gap:6px;font-size:11px;color:#374151;margin-bottom:4px;}"
            + ".da-dot{width:11px;height:11px;border-radius:2px;flex-shrink:0;}"
            + ".da-note-card{background:#faf5ff;border-radius:10px;border-left:4px solid;"
            +   "padding:10px 12px;margin-bottom:7px;}"
            + ".da-note-jenis{font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.3px;margin-bottom:4px;}"
            + ".da-note-subjek{font-size:12px;font-weight:600;color:#111827;margin-bottom:2px;}"
            + ".da-note-tgl{font-size:10px;color:" + CLR_MUTED + ";}"
            + ".da-note-ket{font-size:11px;color:#374151;margin-top:4px;line-height:1.4;}"
            + ".da-heatmap{display:grid;grid-template-columns:repeat(7,1fr);gap:5px;}"
            + ".da-heatmap-cell{border-radius:4px;text-align:center;padding:8px 2px;"
            +   "font-size:10px;font-weight:700;transition:transform .2s;cursor:default;}"
            + ".da-heatmap-cell:hover{transform:scale(1.1);}"
            + ".da-header-bar{background:linear-gradient(135deg," + CLR_HEADER + ",var(--ais-theme-primary,#7c3aed));"
            +   "border-radius:14px;padding:16px 20px;color:#fff;margin-bottom:14px;"
            +   "box-shadow:0 4px 14px rgba(76,29,149,.3);}"
            + "@media(max-width:640px){"
            +   ".da-stat-val{font-size:22px;}.da-stat-card{padding:10px 12px;}"
            + "}"
            + "</style>");
    }

    private void renderHeader(DashData d) {
        String tgl = new SimpleDateFormat("EEEE, dd MMMM yyyy",
                new java.util.Locale("id", "ID")).format(new Date());
        String badgeColor = CLR_PRIMER;
        if ("Siswa".equals(d.namaRole))     badgeColor = "#8b5cf6";
        else if ("Guru".equals(d.namaRole)) badgeColor = "#22c55e";

        String judul = lingkup == Lingkup.SEMUA ? "Dasbor Apresiasi" : lingkup.getNamaModul();
        appendHtml(this,
            "<div class='da-header-bar'>"
            + "<div style='display:flex;align-items:center;gap:12px;flex-wrap:wrap;'>"
            + "<div style='font-size:32px;'>&#127942;</div>"
            + "<div style='flex:1;min-width:0;'>"
            + "<div style='font-size:18px;font-weight:800;'>" + esc(judul) + "</div>"
            + "<div style='font-size:12px;opacity:.85;margin-top:2px;'>"
            +   "Selamat datang, <b>" + esc(d.namaPengguna) + "</b>"
            +   " &nbsp;&#8226;&nbsp; "
            +   "<span style='background:" + badgeColor + ";padding:2px 8px;border-radius:20px;font-size:10px;font-weight:700;'>"
            +   esc(d.namaRole) + "</span>"
            +   "&nbsp;&nbsp;" + esc(tgl)
            + "</div></div>"
            + "<div style='text-align:right;font-size:22px;font-weight:800;opacity:.9;'>"
            +   d.total
            + "<div style='font-size:10px;font-weight:400;opacity:.75;'>total apresiasi</div>"
            + "</div></div></div>");
    }

    private void renderKartuRingkasan(DashData d) {
        Div wrap = kartuSection(this, "Ringkasan Apresiasi",
            "Gambaran cepat jumlah apresiasi yang diberikan — total, bulan ini, dan rata-ratanya.");
        Div row = buatFlex(wrap);
        String rataTeks = d.rataPerBulan < 1 ? "< 1" : String.format("%.1f", d.rataPerBulan);
        buatKartu(kartuCol(row), "Total Apresiasi",  d.total, CLR_PRIMER, "Semua apresiasi tercatat");
        buatKartu(kartuCol(row), "Bulan Ini",        d.bulanIni, CLR_SUKSES, "Apresiasi di bulan berjalan");
        buatKartuTeks(kartuCol(row), "Jenis Tercatat", d.perJenis.size() + " jenis", CLR_PERINGATAN, "Keberagaman jenis apresiasi");
        buatKartuTeks(kartuCol(row), "Rata-rata/Bln",  rataTeks + " apresiasi", CLR_HEADER, "Rata-rata per bulan (12 bln terakhir)");
    }

    private void renderTrenBulanan(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='da-section-title'>&#128200; Tren Apresiasi 12 Bulan Terakhir</div>"
            + "<div class='da-section-desc'>Lihat seberapa aktif pemberian apresiasi setiap bulannya — semakin panjang bar, semakin banyak apresiasi diberikan.</div>");
        int maxVal = 1;
        for (int v : d.perBulan.values()) if (v > maxVal) maxVal = v;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> en : d.perBulan.entrySet()) {
            int v = en.getValue(); double pct = (double) v / maxVal * 100;
            sb.append("<div class='da-bar-row'>"
                + "<div class='da-bar-label'>" + esc(en.getKey()) + "</div>"
                + "<div class='da-bar-fill' style='width:" + String.format("%.1f", pct) + "%;background:" + CLR_PRIMER + ";'></div>"
                + "<div class='da-bar-val'>" + v + "</div></div>");
        }
        if (d.perBulan.isEmpty()) sb.append("<div style='color:#9ca3af;font-size:12px;padding:20px 0;'>Belum ada data.</div>");
        appendHtml(card, sb.toString());
    }

    private void renderDistribusiDanRadar(DashData d) {
        Div row = buatFlex(this);
        Div colDonut = kartuCol(row);
        Div colSpider = kartuCol(row);

        Div cardDonut = buatCard(colDonut);
        appendHtml(cardDonut,
            "<div class='da-section-title'>&#127775; Distribusi Jenis Apresiasi</div>"
            + "<div class='da-section-desc'>Proporsi setiap jenis apresiasi yang paling sering diberikan.</div>");
        if (d.perJenis.isEmpty()) {
            tampilKosong(cardDonut, "Belum ada data.");
        } else {
            List<Map.Entry<String, Integer>> topJ = ambilTopN(d.perJenis, 8);
            StringBuilder conic = new StringBuilder(); double start = 0;
            for (int i = 0; i < topJ.size(); i++) {
                double pct = (double) topJ.get(i).getValue() / Math.max(d.total, 1) * 100;
                if (i > 0) conic.append(",");
                conic.append(PALET[i % PALET.length]).append(" ").append(String.format("%.1f", start)).append("% ").append(String.format("%.1f", start + pct)).append("%");
                start += pct;
            }
            appendHtml(cardDonut,
                "<div style='display:flex;gap:12px;align-items:center;flex-wrap:wrap;'>"
                + "<div style='width:120px;height:120px;border-radius:50%;background:conic-gradient(" + conic + ");flex-shrink:0;box-shadow:0 2px 8px rgba(0,0,0,.1);'></div>"
                + "<div style='flex:1;min-width:0;'>");
            for (int i = 0; i < topJ.size(); i++) {
                appendHtml(cardDonut,
                    "<div class='da-legend-item'>"
                    + "<div class='da-dot' style='background:" + PALET[i % PALET.length] + ";'></div>"
                    + "<div>" + esc(potong(topJ.get(i).getKey(), 28)) + " <b>(" + topJ.get(i).getValue() + ")</b></div></div>");
            }
            appendHtml(cardDonut, "</div></div>");
        }

        Div cardSpider = buatCard(colSpider);
        appendHtml(cardSpider,
            "<div class='da-section-title'>&#129351; Spider — Top Jenis Apresiasi</div>"
            + "<div class='da-section-desc'>Visualisasi jaring laba-laba — sudut lebih lebar berarti jenis apresiasi lebih sering diberikan.</div>");
        List<Map.Entry<String, Integer>> topS = ambilTopN(d.perJenis, 6);
        if (topS.isEmpty()) {
            tampilKosong(cardSpider, "Belum ada data.");
        } else {
            int n = topS.size(); int maxV = 1;
            for (Map.Entry<String, Integer> e : topS) if (e.getValue() > maxV) maxV = e.getValue();
            double cx = 90, cy = 90, r = 70;
            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double angle = Math.PI / 2 - 2 * Math.PI * i / n;
                double ratio = (double) topS.get(i).getValue() / maxV;
                pts.append(String.format("%.1f,%.1f", cx + r * ratio * Math.cos(angle), cy - r * ratio * Math.sin(angle)));
                if (i < n - 1) pts.append(" ");
            }
            StringBuilder labels = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double angle = Math.PI / 2 - 2 * Math.PI * i / n;
                labels.append(String.format("<text x='%.1f' y='%.1f' text-anchor='middle' font-size='7' fill='#374151'>%s</text>",
                        cx + (r + 16) * Math.cos(angle), cy - (r + 16) * Math.sin(angle),
                        esc(potong(topS.get(i).getKey(), 10))));
            }
            appendHtml(cardSpider,
                "<svg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg' style='width:100%;max-width:180px;display:block;margin:0 auto;'>"
                + "<circle cx='" + cx + "' cy='" + cy + "' r='" + r + "' fill='none' stroke='#f3e8ff' stroke-width='1'/>"
                + "<circle cx='" + cx + "' cy='" + cy + "' r='" + (r * 0.5) + "' fill='none' stroke='#f3e8ff' stroke-width='1'/>"
                + "<polygon points='" + pts + "' fill='" + CLR_PRIMER + "' fill-opacity='.3' stroke='" + CLR_PRIMER + "' stroke-width='1.5'/>"
                + labels + "</svg>");
        }
    }

    private void renderPolaMinggu(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='da-section-title'>&#128197; Pola Apresiasi per Hari</div>"
            + "<div class='da-section-desc'>Hari mana yang paling banyak apresiasi diberikan — untuk melihat ritme penghargaan.</div>");
        int maxH = 1;
        for (int v : d.perHari.values()) if (v > maxH) maxH = v;
        StringBuilder sb = new StringBuilder("<div class='da-heatmap'>");
        for (Map.Entry<String, Integer> en : d.perHari.entrySet()) {
            int v = en.getValue(); double intensity = maxH > 0 ? (double) v / maxH : 0;
            int r2 = (int)(220 + (124 - 220) * intensity);
            int g2 = (int)(220 + (58 - 220) * intensity);
            int b2 = (int)(220 + (237 - 220) * intensity);
            String bg = "rgb(" + r2 + "," + g2 + "," + b2 + ")";
            String fg = intensity > 0.5 ? "#fff" : "#374151";
            sb.append("<div class='da-heatmap-cell' style='background:" + bg + ";color:" + fg + ";' title='" + en.getKey() + ": " + v + "'>"
                + esc(en.getKey()) + "<br/><span style='font-size:12px;'>" + v + "</span></div>");
        }
        sb.append("</div>");
        appendHtml(card, sb.toString());
    }

    private void renderCatatanTerbaru(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='da-section-title'>&#127942; 5 Apresiasi Terbaru</div>"
            + "<div class='da-section-desc'>Apresiasi yang paling baru diberikan — untuk memantau penghargaan terkini.</div>");
        List<AprEntry> lima = d.semua.size() > 5 ? d.semua.subList(0, 5) : d.semua;
        if (lima.isEmpty()) { tampilKosong(card, "Belum ada data apresiasi."); return; }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new java.util.Locale("id","ID"));
        for (int i = 0; i < lima.size(); i++) {
            AprEntry e = lima.get(i); String clr = PALET[i % PALET.length];
            appendHtml(card,
                "<div class='da-note-card' style='border-left-color:" + clr + ";'>"
                + "<div class='da-note-jenis' style='color:" + clr + ";'>" + esc(e.jenisNama) + " · " + esc(e.sumber) + "</div>"
                + "<div class='da-note-subjek'>" + esc(e.subjekNama) + "</div>"
                + "<div class='da-note-tgl'>" + (e.tanggal != null ? sdf.format(e.tanggal) : "-") + "</div>"
                + (e.keterangan.isEmpty() ? "" : "<div class='da-note-ket'>" + esc(potong(e.keterangan, 120)) + "</div>")
                + "</div>");
        }
    }

    private void renderTabelLengkap(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='da-section-title'>&#128196; Daftar Lengkap Apresiasi</div>"
            + "<div class='da-section-desc'>Semua data apresiasi dalam tabel — bisa diunduh ke Excel untuk arsip atau laporan.</div>");
        if (d.semua.isEmpty()) { tampilKosong(card, "Belum ada data."); return; }

        MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig();
        btnExcel.setLabel("Unduh Excel"); btnExcel.setImage("/img/excel.gif");
        btnExcel.setTooltiptext("Unduh data apresiasi ke Excel");
        btnExcel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { unduhExcel(); }
        });
        btnExcel.setParent(card);

        pgTabel = new Paging();
        pgTabel.setPageSize(PAGE_SIZE);
        pgTabel.setTotalSize(d.semua.size());
        pgTabel.setParent(card);

        gridTabel = new MyGrid();
        gridTabel.setWidth("100%"); gridTabel.setSclass("dgrid"); gridTabel.setFixedLayout(true);
        gridTabel.setParent(card);

        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns(); cols.setParent(gridTabel);
        buatKolom(cols, "Tanggal", "100px");
        buatKolom(cols, "Jenis", "20%");
        buatKolom(cols, "Nama Siswa", "25%");
        buatKolom(cols, "Keterangan", null);
        buatKolom(cols, "Sumber", "15%");

        final List<AprEntry> daftar = d.semua;
        Common.initPagingCustom(pgTabel, new EventListener() {
            public void onEvent(Event e) throws Exception {
                int from = pgTabel.getActivePage() * PAGE_SIZE;
                int to   = Math.min(from + PAGE_SIZE, daftar.size());
                gridTabel.setModel(new SimpleListModel(daftar.subList(from, to)));
            }
        }, PAGE_SIZE);

        gridTabel.setRowRenderer(new MyRowRenderer() {
            @Override
            public void render(Row row, Object data) throws Exception {
                AprEntry ae = (AprEntry) data;
                SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy", new java.util.Locale("id","ID"));
                buatSel(row, ae.tanggal != null ? sf.format(ae.tanggal) : "-");
                buatSel(row, ae.jenisNama);
                buatSel(row, ae.subjekNama);
                buatSel(row, ae.keterangan);
                buatSel(row, ae.sumber);
            }
        });
        int to = Math.min(PAGE_SIZE, d.semua.size());
        gridTabel.setModel(new SimpleListModel(d.semua.subList(0, to)));
    }

    private void unduhExcel() {
        try {
            if (lastData == null || lastData.semua.isEmpty()) {
                MyMessageboxConfig.show(
                        "Mohon maaf, saat ini tidak ada data yang dapat diunduh. Langkah yang dapat dilakukan: (1) pastikan filter atau kriteria pencarian sudah sesuai; (2) lakukan proses pemuatan data terlebih dahulu hingga data tampil; (3) apabila data memang belum tersedia, silakan coba kembali di lain waktu.",
                        "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                return;
            }
            String namaFile = "Dasbor_Apresiasi_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
            String path = Executions.getCurrent().getDesktop().getWebApp().getRealPath("/tmp/" + namaFile);
            new File(path).getParentFile().mkdirs();

            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("Apresiasi");
            XSSFCellStyle hStyle = wb.createCellStyle();
            hStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(76, 29, 149)));
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont hFont = wb.createFont(); hFont.setBold(true);
            hFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);

            XSSFRow header = sheet.createRow(0);
            String[] hdrs = {"No","Tanggal","Jenis","Nama Siswa","Keterangan","Sumber"};
            for (int i = 0; i < hdrs.length; i++) {
                XSSFCell cell = header.createCell(i); cell.setCellValue(hdrs[i]); cell.setCellStyle(hStyle);
            }
            SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy");
            List<AprEntry> list = lastData.semua;
            for (int i = 0; i < list.size(); i++) {
                AprEntry ae = list.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(ae.tanggal != null ? sf.format(ae.tanggal) : "");
                row.createCell(2).setCellValue(ae.jenisNama);
                row.createCell(3).setCellValue(ae.subjekNama);
                row.createCell(4).setCellValue(ae.keterangan);
                row.createCell(5).setCellValue(ae.sumber);
            }
            for (int i = 0; i < hdrs.length; i++) sheet.autoSizeColumn(i);

            FileOutputStream fos = new FileOutputStream(path);
            wb.write(fos); fos.close(); wb.close();
            org.zkoss.zul.Filedownload.save(
                new FileInputStream(new File(path)),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", namaFile);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private Div buatCard(Component parent) { Div d = new Div(); d.setSclass("da-card"); d.setParent(parent); return d; }
    private Div kartuSection(Component parent, String judul, String deskripsi) {
        Div card = buatCard(parent);
        appendHtml(card, "<div class='da-section-title'>" + esc(judul) + "</div>"
            + "<div class='da-section-desc'>" + esc(deskripsi) + "</div>");
        return card;
    }
    private Div buatFlex(Component parent) { Div d = new Div(); d.setSclass("da-flex"); d.setParent(parent); return d; }
    private Div kartuCol(Div parent) { Div d = new Div(); d.setSclass("da-col"); d.setParent(parent); return d; }
    private void buatKartu(Div parent, String label, int val, String clr, String sub) {
        appendHtml(parent,
            "<div class='da-stat-card' style='border-top-color:" + clr + ";'>"
            + "<div class='da-stat-val' style='color:" + clr + ";'>" + val + "</div>"
            + "<div class='da-stat-label'>" + esc(label) + "</div>"
            + "<div class='da-stat-sub'>" + esc(sub) + "</div></div>");
    }
    private void buatKartuTeks(Div parent, String label, String val, String clr, String sub) {
        appendHtml(parent,
            "<div class='da-stat-card' style='border-top-color:" + clr + ";'>"
            + "<div class='da-stat-val' style='color:" + clr + ";font-size:20px;'>" + esc(val) + "</div>"
            + "<div class='da-stat-label'>" + esc(label) + "</div>"
            + "<div class='da-stat-sub'>" + esc(sub) + "</div></div>");
    }
    private void tampilKosong(Component parent, String pesan) {
        appendHtml(parent, "<div style='text-align:center;padding:24px;color:#9ca3af;font-size:13px;'>&#128270; " + esc(pesan) + "</div>");
    }
    private void buatKolom(org.zkoss.zul.Columns cols, String label, String width) {
        MyColumnConfig col = new MyColumnConfig(); col.setLabel(label);
        if (width != null) col.setWidth(width); col.setParent(cols);
    }
    private void buatSel(Row row, String val) {
        org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(val != null ? val : ""); lbl.setParent(row);
    }
    private List<Map.Entry<String, Integer>> ambilTopN(Map<String, Integer> map, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) { return b.getValue().compareTo(a.getValue()); }
        });
        return list.size() > n ? list.subList(0, n) : list;
    }
    private void appendHtml(Component parent, String html) { Html h = new Html(); h.setContent(html); h.setParent(parent); }
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    private static String safeStr(Object o) { return o == null ? "" : o.toString().trim(); }
    private static String potong(String s, int max) { if (s == null) return ""; return s.length() > max ? s.substring(0, max) + "…" : s; }
}
