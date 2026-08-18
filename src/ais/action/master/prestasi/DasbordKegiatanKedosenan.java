package ais.action.master.prestasi;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor Kegiatan Dosen (Kegiatan Kedosenan).
 *
 * Tampilan &amp; struktur panel dibuat sama dengan Dasbor Prestasi (lihat
 * {@link DasbordPrestasi}) — gaya kartu, batang tren, donat, jaring laba-laba
 * (spider), dan heatmap memakai HTML/CSS/SVG murni (tanpa JFreeChart), memakai
 * kelas CSS {@code dps-*} yang sama agar konsisten dan responsif di HP/desktop.
 * Yang berbeda hanyalah datanya: aktivitas/kegiatan kedosenan, bukan prestasi.
 *
 * Cache: L2 (per tab browser) + L3 (lintas sesi, admin) via {@link DashboardCacheUtil}.
 */
public class DasbordKegiatanKedosenan extends Div {

    private static final long serialVersionUID = 1L;

    // ══ Palet & konstanta (samakan dengan DasbordPrestasi) ════════════════════
    private static final String CLR_PRIMER = "#d97706";
    private static final String CLR_SUKSES = "#059669";
    private static final String CLR_UNGU   = "#7c3aed";
    private static final String CLR_HEADER = "#78350f";
    private static final String CLR_BG     = "#fffbeb";

    private static final String[] PALET = {
        "#d97706","var(--ais-theme-primary,#2563eb)","#16a34a","#9333ea","#e11d48","#0891b2","#ea580c","#65a30d"
    };
    private static final String[] BULAN = {
        "Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agt","Sep","Okt","Nov","Des"
    };
    private static final String[] HARI = { "Min","Sen","Sel","Rab","Kam","Jum","Sab" };

    private static final int MAX_ROWS  = 1500;
    private static final int PAGE_SIZE = 15;

    private Paging pgTabel;
    private MyGrid gridTabel;
    private Data   lastData;

    // ── Satu kegiatan yang sudah dinormalisasi ────────────────────────────────
    static final class Item {
        final Date   mulai;
        final String kode, nama, aspek, aspekRinci, jabatan, skala, status, fakultas, prodi, dosen, tempat;
        final Integer tahun;
        Item(Date mulai, String kode, String nama, String aspek, String aspekRinci, String jabatan,
             String skala, String status, String fakultas, String prodi, String dosen, String tempat, Integer tahun) {
            this.mulai = mulai; this.kode = ss(kode); this.nama = ss(nama); this.aspek = ss(aspek);
            this.aspekRinci = ss(aspekRinci); this.jabatan = ss(jabatan); this.skala = ss(skala);
            this.status = ss(status); this.fakultas = ss(fakultas); this.prodi = ss(prodi);
            this.dosen = ss(dosen); this.tempat = ss(tempat); this.tahun = tahun;
        }
    }

    // ── Data agregat (dimuat 1×, dipakai semua panel) ─────────────────────────
    static final class Data {
        List<Item> semua = new ArrayList<Item>();
        Map<String,Integer> perBulan = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perHari  = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perTahun = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perAspek = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perAspekRinci = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perJabatan = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perSkala = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perStatus = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perFakultas = new LinkedHashMap<String,Integer>();
        Map<String,Integer> perProdi = new LinkedHashMap<String,Integer>();
        Map<String,Integer> topDosen = new LinkedHashMap<String,Integer>();
        int total, bulanIni, disetujui, ditolak, diproses, belum;
        double rataPerBulan;
        String namaPengguna = "", namaRole = "";
    }

    // ── Konstruktor ───────────────────────────────────────────────────────────
    public DasbordKegiatanKedosenan() {
        setWidth("100%");
        setStyle("min-height:300px;background:" + CLR_BG + ";padding:12px 14px;box-sizing:border-box;overflow:auto;");
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
        h(this, "<div style='padding:80px 0;text-align:center;'>"
            + "<div style='font-size:48px;margin-bottom:16px;animation:dps-spin 1.2s linear infinite;display:inline-block;'>&#128218;</div>"
            + "<div style='font-size:15px;font-weight:800;color:" + CLR_HEADER + ";'>Memuat Kegiatan Dosen&#8230;</div>"
            + "<div style='margin-top:8px;font-size:12px;color:#6b7280;'>Menyiapkan ringkasan, grafik, dan daftar lengkap kegiatan.</div>"
            + "</div><style>@keyframes dps-spin{to{transform:rotate(360deg)}}</style>");
    }

    // ══ Render utama ══════════════════════════════════════════════════════════
    private void renderAll() throws Exception {
        Data d = loadDataWithCache();
        lastData = d;
        Common.clear(this);
        renderCss();
        renderHeader(d);
        renderKartuRingkasan(d);
        renderStatus(d);
        renderTrenBulanan(d);
        renderTrenTahunan(d);
        renderDistribusiAspek(d);
        renderSpiderJabatan(d);
        renderPolaHari(d);
        renderDistribusiSkala(d);
        renderDistribusiAspekRinci(d);
        renderTopDosen(d);
        renderTopUnit(d);
        renderKegiatanBaru(d);
        renderTabel(d);
    }

    // ══ Cache L2 + L3 ═════════════════════════════════════════════════════════
    private Data loadDataWithCache() {
        Tbmuser user = Common.getCurrentUser();
        Dosen   dos  = user != null ? user.ambilDosen() : null;
        boolean personal = dos != null;
        Long    uid  = (user != null) ? user.getId() : null;

        String key = DashboardCacheUtil.key("DasbordKegiatanKedosenan", personal ? "dosen" : "all", personal ? uid : null);
        Object l2  = DashboardCacheUtil.getL2(key);
        if (l2 instanceof Data) return (Data) l2;
        if (!personal) {
            Object l3 = DashboardCacheUtil.getL3(key);
            if (l3 instanceof Data) { DashboardCacheUtil.putL2(key, l3); return (Data) l3; }
        }
        Data d = loadData(user, dos);
        DashboardCacheUtil.putL2(key, d);
        if (!personal) DashboardCacheUtil.putL3(key, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private Data loadData(Tbmuser user, Dosen dos) {
        Data d = new Data();
        if (dos != null) { d.namaRole = "Dosen"; d.namaPengguna = ss(dos.getNama()); }
        else { d.namaRole = "Administrator"; d.namaPengguna = user != null ? ss(user.getUserNama()) : ""; }

        Calendar cal = Calendar.getInstance();
        for (int i = 11; i >= 0; i--) {
            Calendar t = (Calendar) cal.clone(); t.add(Calendar.MONTH, -i);
            d.perBulan.put(BULAN[t.get(Calendar.MONTH)] + " " + t.get(Calendar.YEAR), 0);
        }
        for (String hh : HARI) d.perHari.put(hh, 0);

        Criteria c = HibernateUtil.currentSession().createCriteria(KegiatanKedosenan.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (dos != null) {
            c.add(Restrictions.sqlRestriction(
                    "this_.id in (select kegiatan_kedosenan from kegiatan_kedosenan_punya_dosen where dosen = "
                            + dos.getId() + ")"));
        }

        for (KegiatanKedosenan k : (List<KegiatanKedosenan>) c.list()) {
            String aspek      = k.getKelompokKegiatanKedosenan()       != null ? k.getKelompokKegiatanKedosenan().getNama()       : "";
            String aspekRinci = k.getDetailKelompokKegiatanKedosenan() != null ? k.getDetailKelompokKegiatanKedosenan().getNama() : "";
            String jabatan    = k.getJabatanKegiatanKedosenan()        != null ? k.getJabatanKegiatanKedosenan().getNama()        : "";
            String skala      = k.getSkalaKegiatanKedosenan()          != null ? k.getSkalaKegiatanKedosenan().getNama()          : "";
            String fak        = k.getFakultas() != null ? k.getFakultas().getNama() : "";
            String prodi      = k.getJurusan()  != null ? k.getJurusan().getNama()  : "";
            String dosenNama  = k.getDiajukanOleh() != null ? k.getDiajukanOleh().getNama() : "";
            d.semua.add(new Item(k.getMulai(), k.getKode(), k.getNama(), aspek, aspekRinci, jabatan, skala,
                k.getStatus(), fak, prodi, dosenNama, k.getTempat(), k.getTahun()));
        }

        Collections.sort(d.semua, new Comparator<Item>() {
            public int compare(Item a, Item b) {
                if (a.mulai == null && b.mulai == null) return 0;
                if (a.mulai == null) return 1;
                if (b.mulai == null) return -1;
                return b.mulai.compareTo(a.mulai);
            }
        });

        int nowY = cal.get(Calendar.YEAR), nowM = cal.get(Calendar.MONTH);
        for (Item e : d.semua) {
            d.total++;
            incr(d.perAspek,      e.aspek.isEmpty()      ? "(tanpa aspek)"  : e.aspek);
            if (!e.aspekRinci.isEmpty()) incr(d.perAspekRinci, e.aspekRinci);
            if (!e.jabatan.isEmpty())    incr(d.perJabatan, e.jabatan);
            if (!e.skala.isEmpty())      incr(d.perSkala, e.skala);
            if (!e.fakultas.isEmpty())   incr(d.perFakultas, e.fakultas);
            if (!e.prodi.isEmpty())      incr(d.perProdi, e.prodi);
            if (!e.dosen.isEmpty())      incr(d.topDosen, e.dosen);

            String st = e.status.isEmpty() ? "(tanpa status)" : e.status;
            incr(d.perStatus, st);
            if      (KegiatanKedosenan.DISETUJUI.equalsIgnoreCase(e.status))       d.disetujui++;
            else if (KegiatanKedosenan.DITOLAK.equalsIgnoreCase(e.status))         d.ditolak++;
            else if (KegiatanKedosenan.SEDANG_DIPROSES.equalsIgnoreCase(e.status)) d.diproses++;
            else                                                                   d.belum++;

            if (e.mulai != null) {
                Calendar cc = Calendar.getInstance(); cc.setTime(e.mulai);
                String bk = BULAN[cc.get(Calendar.MONTH)] + " " + cc.get(Calendar.YEAR);
                if (d.perBulan.containsKey(bk)) d.perBulan.put(bk, d.perBulan.get(bk) + 1);
                if (cc.get(Calendar.YEAR) == nowY && cc.get(Calendar.MONTH) == nowM) d.bulanIni++;
                d.perHari.put(HARI[cc.get(Calendar.DAY_OF_WEEK) - 1], d.perHari.get(HARI[cc.get(Calendar.DAY_OF_WEEK) - 1]) + 1);
            }
            if (e.tahun != null && e.tahun > 2000) incr(d.perTahun, String.valueOf(e.tahun));
        }

        int sumB = 0, cntB = 0;
        for (int v : d.perBulan.values()) { if (v > 0) { sumB += v; cntB++; } }
        d.rataPerBulan = cntB > 0 ? (double) sumB / cntB : 0;

        d.topDosen   = sortDesc(d.topDosen);
        d.perAspek   = sortDesc(d.perAspek);
        d.perJabatan = sortDesc(d.perJabatan);
        return d;
    }

    // ══ CSS (sama dengan DasbordPrestasi) ═════════════════════════════════════
    private void renderCss() {
        h(this,
          "<style>"
          + ".dps-card{background:#fff;border-radius:14px;padding:16px 18px;box-shadow:0 2px 10px rgba(0,0,0,.07);margin-bottom:14px;}"
          + ".dps-card-title{font-size:13px;font-weight:800;color:#78350f;letter-spacing:.4px;margin-bottom:3px;text-transform:uppercase;}"
          + ".dps-card-desc{font-size:11.5px;color:#64748b;margin-bottom:12px;line-height:1.5;}"
          + ".dps-stat-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:14px;}"
          + ".dps-sc{background:#fff;border-radius:12px;padding:14px 16px;box-shadow:0 2px 8px rgba(0,0,0,.06);border-top:4px solid;text-align:center;}"
          + ".dps-sc-val{font-size:26px;font-weight:800;line-height:1.1;}"
          + ".dps-sc-lbl{font-size:11px;font-weight:600;color:#64748b;margin-top:3px;}"
          + ".dps-sc-sub{font-size:10px;color:#94a3b8;margin-top:2px;}"
          + ".dps-juara-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:14px;}"
          + ".dps-jcard{border-radius:12px;padding:12px 10px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,.09);}"
          + ".dps-jcard-num{font-size:28px;font-weight:900;}"
          + ".dps-jcard-lbl{font-size:11px;font-weight:700;margin-top:4px;}"
          + ".dps-jcard-sub{font-size:10px;opacity:.75;margin-top:2px;}"
          + ".dps-bar-row{display:flex;align-items:center;gap:6px;margin-bottom:6px;font-size:11px;}"
          + ".dps-bar-label{width:90px;flex-shrink:0;color:#64748b;text-align:right;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;}"
          + ".dps-bar-track{flex:1;background:#f1f5f9;border-radius:4px;height:18px;overflow:hidden;position:relative;}"
          + ".dps-bar-fill{height:100%;border-radius:4px;transition:width .5s;min-width:2px;}"
          + ".dps-bar-val{width:32px;flex-shrink:0;font-weight:700;color:#78350f;text-align:left;}"
          + ".dps-donut-wrap{display:flex;gap:14px;align-items:flex-start;flex-wrap:wrap;}"
          + ".dps-legend{display:flex;flex-direction:column;gap:5px;flex:1;min-width:120px;}"
          + ".dps-leg-item{display:flex;align-items:center;gap:6px;font-size:11px;color:#374151;}"
          + ".dps-dot{width:11px;height:11px;border-radius:2px;flex-shrink:0;}"
          + ".dps-heatmap{display:grid;grid-template-columns:repeat(7,1fr);gap:5px;}"
          + ".dps-hm-cell{border-radius:6px;padding:10px 2px;text-align:center;font-size:10px;font-weight:700;}"
          + ".dps-note{background:#fffbeb;border-radius:10px;border-left:4px solid;padding:10px 12px;margin-bottom:7px;}"
          + ".dps-note-badge{font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.3px;margin-bottom:3px;}"
          + ".dps-note-name{font-size:12px;font-weight:700;color:#111827;margin-bottom:2px;}"
          + ".dps-note-tgl{font-size:10px;color:#64748b;}"
          + ".dps-note-judul{font-size:11px;color:#374151;margin-top:4px;line-height:1.4;}"
          + ".dps-capaian-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(120px,1fr));gap:8px;}"
          + ".dps-capaian-card{border-radius:10px;padding:12px 8px;text-align:center;font-size:11px;font-weight:700;}"
          + ".dps-tp-row{display:flex;align-items:center;gap:8px;margin-bottom:6px;font-size:11px;}"
          + ".dps-tp-rank{width:22px;height:22px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:800;flex-shrink:0;}"
          + ".dps-tp-name{flex:1;color:#1e293b;font-weight:600;}"
          + ".dps-tp-bar{flex:2;height:12px;background:#f1f5f9;border-radius:4px;overflow:hidden;}"
          + ".dps-tp-fill{height:100%;border-radius:4px;background:#d97706;}"
          + ".dps-tp-cnt{width:28px;text-align:right;font-weight:700;color:#78350f;}"
          + ".dps-header{background:linear-gradient(135deg,#78350f 0%,#d97706 100%);border-radius:16px;padding:18px 20px;color:#fff;margin-bottom:14px;box-shadow:0 4px 16px rgba(120,53,15,.28);}"
          + ".dps-header-title{font-size:20px;font-weight:900;letter-spacing:.3px;}"
          + ".dps-header-sub{font-size:12px;opacity:.85;margin-top:4px;}"
          + ".dps-header-badge{display:inline-block;background:rgba(255,255,255,.2);border-radius:20px;padding:2px 10px;font-size:11px;font-weight:700;}"
          + ".dps-header-num{font-size:28px;font-weight:900;opacity:.95;text-align:right;}"
          + ".dps-header-num-lbl{font-size:10px;font-weight:400;opacity:.75;}"
          + "@media(max-width:900px){.dps-stat-grid{grid-template-columns:repeat(2,1fr);}.dps-juara-grid{grid-template-columns:repeat(2,1fr);}}"
          + "@media(max-width:480px){.dps-stat-grid{grid-template-columns:repeat(2,1fr);}.dps-juara-grid{grid-template-columns:repeat(2,1fr);}.dps-sc-val{font-size:20px;}.dps-heatmap{gap:3px;}}"
          + "</style>");
    }

    // ── 1. Header ──────────────────────────────────────────────────────────────
    private void renderHeader(Data d) {
        String tgl = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id","ID")).format(new Date());
        h(this,
          "<div class='dps-header'>"
          + "<div style='display:flex;align-items:center;gap:14px;flex-wrap:wrap;'>"
          + "<div style='font-size:36px;'>&#128218;</div>"
          + "<div style='flex:1;min-width:0;'>"
          + "<div class='dps-header-title'>Kegiatan Dosen</div>"
          + "<div class='dps-header-sub'>Selamat datang, <b>" + esc(d.namaPengguna) + "</b>&nbsp;"
          +   "<span class='dps-header-badge'>" + esc(d.namaRole) + "</span>&nbsp;&nbsp;" + esc(tgl) + "</div></div>"
          + "<div class='dps-header-num'>" + d.total + "<div class='dps-header-num-lbl'>total kegiatan</div></div>"
          + "</div></div>");
    }

    // ── 2. Kartu ringkasan ─────────────────────────────────────────────────────
    private void renderKartuRingkasan(Data d) {
        String rata = d.rataPerBulan < 1 ? "< 1" : String.format("%.1f", d.rataPerBulan);
        int pctSetuju = d.total > 0 ? (int) Math.round((double) d.disetujui * 100 / d.total) : 0;
        h(this,
          "<div class='dps-stat-grid'>"
          + statCard(d.total,    "Total Kegiatan",   "Semua kegiatan yang tercatat",       CLR_PRIMER)
          + statCard(d.bulanIni, "Bulan Ini",        "Kegiatan di bulan berjalan",          CLR_SUKSES)
          + statCardTeks(d.disetujui + " (" + pctSetuju + "%)", "Sudah Disetujui", "Dari seluruh kegiatan", CLR_UNGU)
          + statCardTeks(rata + " / bln", "Rata-rata", "Rata-rata per bulan (12 bln terakhir)", CLR_PRIMER)
          + "</div>");
    }

    // ── 3. Highlight status persetujuan ───────────────────────────────────────
    private void renderStatus(Data d) {
        if (d.disetujui + d.diproses + d.belum + d.ditolak == 0) return;
        h(this,
          "<div style='margin-bottom:14px;'>"
          + "<div class='dps-card-title' style='margin-bottom:8px;'>&#9989; Status Persetujuan</div>"
          + "<div class='dps-card-desc' style='margin-bottom:10px;'>Pembagian kegiatan menurut status: sudah disetujui, masih diproses, belum diproses, atau ditolak.</div>"
          + "<div class='dps-juara-grid'>"
          + juaraCard("&#9989;", "Disetujui",        d.disetujui, "#f0fdf4", "#14532d", "#16a34a")
          + juaraCard("&#128338;", "Sedang Diproses", d.diproses,  "#eff6ff", "var(--ais-theme-primary,#1e3a8a)", "var(--ais-theme-primary,#2563eb)")
          + juaraCard("&#128196;", "Belum Diproses",  d.belum,     "#f8fafc", "#334155", "#64748b")
          + juaraCard("&#10060;", "Ditolak",          d.ditolak,   "#fef2f2", "#7f1d1d", "#dc2626")
          + "</div></div>");
    }

    // ── 4. Tren 12 bulan ───────────────────────────────────────────────────────
    private void renderTrenBulanan(Data d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128200; Tren Kegiatan 12 Bulan Terakhir</div>"
            + "<div class='dps-card-desc'>Seberapa aktif kegiatan setiap bulan — batang lebih panjang berarti lebih banyak kegiatan pada bulan itu.</div>");
        int maxV = 1;
        for (int v : d.perBulan.values()) if (v > maxV) maxV = v;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Integer> en : d.perBulan.entrySet())
            sb.append(barRow(en.getKey(), en.getValue(), maxV, CLR_PRIMER));
        h(card, sb.length() == 0 ? kosong() : sb.toString());
    }

    // ── 5. Tren per tahun ──────────────────────────────────────────────────────
    private void renderTrenTahunan(Data d) {
        if (d.perTahun.size() < 2) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128197; Perbandingan Antar Tahun</div>"
            + "<div class='dps-card-desc'>Jumlah kegiatan tiap tahun, untuk melihat apakah dari tahun ke tahun makin meningkat.</div>");
        List<Map.Entry<String,Integer>> list = new ArrayList<Map.Entry<String,Integer>>(d.perTahun.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) { return a.getKey().compareTo(b.getKey()); }
        });
        int maxV = 1;
        for (Map.Entry<String,Integer> en : list) if (en.getValue() > maxV) maxV = en.getValue();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Integer> en : list) sb.append(barRow(en.getKey(), en.getValue(), maxV, "var(--ais-theme-primary,#2563eb)"));
        h(card, sb.toString());
    }

    // ── 6. Distribusi aspek (donut) ────────────────────────────────────────────
    private void renderDistribusiAspek(Data d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127775; Sebaran Aspek Kegiatan</div>"
            + "<div class='dps-card-desc'>Bidang kegiatan mana yang paling banyak dilakukan — bagian lingkaran yang lebih besar berarti lebih sering.</div>");
        if (d.perAspek.isEmpty()) { h(card, kosong()); return; }
        List<Map.Entry<String,Integer>> top = topN(d.perAspek, 8);
        StringBuilder conic = new StringBuilder();
        double start = 0;
        for (int i = 0; i < top.size(); i++) {
            double pct = (double) top.get(i).getValue() / Math.max(d.total, 1) * 100;
            if (i > 0) conic.append(",");
            conic.append(PALET[i % PALET.length]).append(" ").append(String.format("%.1f", start)).append("% ")
                 .append(String.format("%.1f", start + pct)).append("%");
            start += pct;
        }
        StringBuilder legend = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            legend.append("<div class='dps-leg-item'><div class='dps-dot' style='background:").append(PALET[i % PALET.length]).append(";'></div>")
                  .append("<div>").append(esc(cut(top.get(i).getKey(), 26))).append(" <b>(").append(top.get(i).getValue()).append(")</b></div></div>");
        }
        h(card, "<div class='dps-donut-wrap'>"
            + "<div style='width:130px;height:130px;border-radius:50%;background:conic-gradient(" + conic
            + ");flex-shrink:0;box-shadow:0 2px 8px rgba(0,0,0,.1);'></div>"
            + "<div class='dps-legend'>" + legend + "</div></div>");
    }

    // ── 7. Spider chart jabatan ────────────────────────────────────────────────
    private void renderSpiderJabatan(Data d) {
        List<Map.Entry<String,Integer>> top = topN(d.perJabatan, 7);
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#129309; Radar Jabatan dalam Kegiatan</div>"
            + "<div class='dps-card-desc'>Gambaran peran/jabatan yang paling sering diemban dalam bentuk jaring — ujung yang makin jauh dari pusat berarti makin sering.</div>");
        if (top.size() < 3) { h(card, kosong()); return; }
        int n = top.size();
        int maxV = 1;
        for (Map.Entry<String,Integer> e : top) if (e.getValue() > maxV) maxV = e.getValue();
        double cx = 110, cy = 110, r = 80;
        StringBuilder pts = new StringBuilder(), gridLines = new StringBuilder(), lbls = new StringBuilder();
        for (int lvl = 1; lvl <= 4; lvl++) {
            StringBuilder ring = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / n;
                double rr = r * lvl / 4;
                if (i > 0) ring.append(" ");
                ring.append(String.format(Locale.US, "%.1f,%.1f", cx + rr * Math.cos(a), cy + rr * Math.sin(a)));
            }
            gridLines.append("<polygon points='").append(ring).append("' fill='none' stroke='#fef3c7' stroke-width='1'/>");
        }
        for (int i = 0; i < n; i++) {
            double a   = -Math.PI / 2 + 2 * Math.PI * i / n;
            double rat = (double) top.get(i).getValue() / maxV;
            double px  = cx + r * rat * Math.cos(a);
            double py  = cy + r * rat * Math.sin(a);
            if (i > 0) pts.append(" ");
            pts.append(String.format(Locale.US, "%.1f,%.1f", px, py));
            double lx = cx + (r + 18) * Math.cos(a);
            double ly = cy + (r + 18) * Math.sin(a);
            lbls.append(String.format(Locale.US,
                "<text x='%.1f' y='%.1f' text-anchor='middle' dominant-baseline='middle' font-size='8' fill='#78350f'>%s</text>",
                lx, ly, esc(cut(top.get(i).getKey(), 10))));
            gridLines.append(String.format(Locale.US,
                "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#fde68a' stroke-width='1'/>",
                cx, cy, cx + r * Math.cos(a), cy + r * Math.sin(a)));
        }
        h(card,
          "<div style='display:flex;justify-content:center;'>"
          + "<svg viewBox='0 0 220 220' style='width:100%;max-width:220px;display:block;' xmlns='http://www.w3.org/2000/svg'>"
          + gridLines
          + "<polygon points='" + pts + "' fill='" + CLR_PRIMER + "' fill-opacity='.3' stroke='" + CLR_PRIMER + "' stroke-width='2'/>"
          + lbls
          + "</svg></div>");
    }

    // ── 8. Heatmap hari ────────────────────────────────────────────────────────
    private void renderPolaHari(Data d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128197; Pola Kegiatan per Hari</div>"
            + "<div class='dps-card-desc'>Hari mana yang paling sering menjadi tanggal kegiatan — membantu melihat ritme aktivitas dalam seminggu.</div>");
        int maxH = 1;
        for (int v : d.perHari.values()) if (v > maxH) maxH = v;
        StringBuilder sb = new StringBuilder("<div class='dps-heatmap'>");
        for (Map.Entry<String,Integer> en : d.perHari.entrySet()) {
            int v = en.getValue();
            double intensity = maxH > 0 ? (double) v / maxH : 0;
            int r2 = (int)(240 + (217 - 240) * intensity);
            int g2 = (int)(240 + (119 - 240) * intensity);
            int b2 = (int)(240 + (6   - 240) * intensity);
            String bg = "rgb(" + r2 + "," + g2 + "," + b2 + ")";
            String fg = intensity > 0.55 ? "#fff" : "#374151";
            sb.append("<div class='dps-hm-cell' style='background:" + bg + ";color:" + fg + ";' title='" + esc(en.getKey()) + ": " + v + " kegiatan'>")
              .append(esc(en.getKey())).append("<br><b>").append(v).append("</b></div>");
        }
        sb.append("</div>");
        h(card, sb.toString());
    }

    // ── 9. Distribusi skala ────────────────────────────────────────────────────
    private void renderDistribusiSkala(Data d) {
        if (d.perSkala.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127757; Skala Kegiatan</div>"
            + "<div class='dps-card-desc'>Ukuran/skala kegiatan, dari tingkat lokal sampai internasional.</div>");
        String[] capWarna = {"#e0f2fe","#bbf7d0","#fef9c3","#fde8d8","#ede9fe","#fce7f3"};
        String[] capTeks  = {"#0c4a6e","#14532d","#78350f","#7c2d12","#4c1d95","#831843"};
        List<Map.Entry<String,Integer>> list = topN(d.perSkala, 6);
        StringBuilder sb = new StringBuilder("<div class='dps-capaian-grid'>");
        for (int i = 0; i < list.size(); i++) {
            sb.append("<div class='dps-capaian-card' style='background:" + capWarna[i % capWarna.length]
                + ";color:" + capTeks[i % capTeks.length] + ";'>"
                + "<div style='font-size:22px;font-weight:900;'>" + list.get(i).getValue() + "</div>"
                + "<div>" + esc(list.get(i).getKey()) + "</div></div>");
        }
        sb.append("</div>");
        h(card, sb.toString());
    }

    // ── 10. Distribusi aspek rinci ─────────────────────────────────────────────
    private void renderDistribusiAspekRinci(Data d) {
        if (d.perAspekRinci.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128278; Sebaran Aspek Rinci</div>"
            + "<div class='dps-card-desc'>Rincian bidang kegiatan yang lebih detail.</div>");
        List<Map.Entry<String,Integer>> top = topN(d.perAspekRinci, 8);
        int maxV = 1;
        for (Map.Entry<String,Integer> en : top) if (en.getValue() > maxV) maxV = en.getValue();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top.size(); i++)
            sb.append(barRow(top.get(i).getKey(), top.get(i).getValue(), maxV, PALET[i % PALET.length]));
        h(card, sb.length() == 0 ? kosong() : sb.toString());
    }

    // ── 11. Top dosen ──────────────────────────────────────────────────────────
    private void renderTopDosen(Data d) {
        if (d.topDosen.isEmpty() || !d.namaRole.equals("Administrator")) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127947; Dosen Paling Aktif</div>"
            + "<div class='dps-card-desc'>Delapan dosen dengan jumlah kegiatan terbanyak — menampilkan siapa yang paling aktif.</div>");
        List<Map.Entry<String,Integer>> top8 = topN(d.topDosen, 8);
        int maxV = top8.isEmpty() ? 1 : top8.get(0).getValue();
        String[] rankClr = {"#fbbf24","#94a3b8","#f97316","#86efac","#93c5fd","#c4b5fd","#fca5a5","#6ee7b7"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top8.size(); i++) {
            int pct = maxV > 0 ? top8.get(i).getValue() * 100 / maxV : 0;
            sb.append("<div class='dps-tp-row'>"
                + "<div class='dps-tp-rank' style='background:" + rankClr[i % rankClr.length] + ";'>" + (i + 1) + "</div>"
                + "<div class='dps-tp-name'>" + esc(cut(top8.get(i).getKey(), 24)) + "</div>"
                + "<div class='dps-tp-bar'><div class='dps-tp-fill' style='width:" + pct + "%;background:" + PALET[i % PALET.length] + ";'></div></div>"
                + "<div class='dps-tp-cnt'>" + top8.get(i).getValue() + "</div></div>");
        }
        h(card, sb.length() == 0 ? kosong() : sb.toString());
    }

    // ── 12. Top fakultas & prodi (admin) ──────────────────────────────────────
    private void renderTopUnit(Data d) {
        if (!d.namaRole.equals("Administrator")) return;
        if (!d.perFakultas.isEmpty()) {
            Div card = buatCard();
            h(card, "<div class='dps-card-title'>&#127979; Fakultas Paling Aktif</div>"
                + "<div class='dps-card-desc'>Fakultas dengan kegiatan dosen terbanyak.</div>");
            List<Map.Entry<String,Integer>> top = topN(d.perFakultas, 6);
            int maxV = 1;
            for (Map.Entry<String,Integer> en : top) if (en.getValue() > maxV) maxV = en.getValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < top.size(); i++) sb.append(barRow(top.get(i).getKey(), top.get(i).getValue(), maxV, PALET[(i + 1) % PALET.length]));
            h(card, sb.length() == 0 ? kosong() : sb.toString());
        }
        if (!d.perProdi.isEmpty()) {
            Div card = buatCard();
            h(card, "<div class='dps-card-title'>&#127891; Program Studi Paling Aktif</div>"
                + "<div class='dps-card-desc'>Program studi dengan kegiatan dosen terbanyak.</div>");
            List<Map.Entry<String,Integer>> top = topN(d.perProdi, 6);
            int maxV = 1;
            for (Map.Entry<String,Integer> en : top) if (en.getValue() > maxV) maxV = en.getValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < top.size(); i++) sb.append(barRow(top.get(i).getKey(), top.get(i).getValue(), maxV, PALET[(i + 3) % PALET.length]));
            h(card, sb.length() == 0 ? kosong() : sb.toString());
        }
    }

    // ── 13. Kegiatan terbaru ───────────────────────────────────────────────────
    private void renderKegiatanBaru(Data d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128218; Kegiatan Paling Baru</div>"
            + "<div class='dps-card-desc'>Lima kegiatan yang paling baru tercatat — untuk pantauan cepat tanpa membuka seluruh daftar.</div>");
        if (d.semua.isEmpty()) { h(card, kosong()); return; }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id","ID"));
        int n = Math.min(5, d.semua.size());
        for (int i = 0; i < n; i++) {
            Item e = d.semua.get(i);
            String clr = PALET[i % PALET.length];
            h(card,
              "<div class='dps-note' style='border-left-color:" + clr + ";'>"
              + "<div class='dps-note-badge' style='color:" + clr + ";'>"
              +   esc(e.aspek) + (e.jabatan.isEmpty() ? "" : " &middot; " + esc(e.jabatan)) + "</div>"
              + "<div class='dps-note-name'>" + esc(e.nama) + "</div>"
              + "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
              +   "<div class='dps-note-tgl'>" + (e.mulai != null ? sdf.format(e.mulai) : "-") + "</div>"
              +   (e.dosen.isEmpty() ? "" : "<div class='dps-note-tgl'>&#128100; " + esc(cut(e.dosen, 28)) + "</div>")
              +   "<div style='font-size:10px;border-radius:4px;padding:1px 6px;font-weight:700;background:" + statusBg(e.status)
              +     ";color:" + statusFg(e.status) + ";'>" + esc(e.status.isEmpty() ? "-" : e.status) + "</div>"
              + "</div></div>");
        }
    }

    // ── 14. Tabel lengkap + download ───────────────────────────────────────────
    private void renderTabel(final Data d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128196; Daftar Lengkap Kegiatan</div>"
            + "<div class='dps-card-desc'>Semua data kegiatan dalam satu tabel. Tekan tombol unduh untuk menyimpannya ke Excel.</div>");
        if (d.semua.isEmpty()) { h(card, kosong()); return; }

        MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig();
        btnExcel.setLabel("Unduh Excel"); btnExcel.setImage("/img/excel.gif");
        btnExcel.setTooltiptext("Unduh seluruh data kegiatan ke file Excel");
        btnExcel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { unduhExcel(); }
        });
        btnExcel.setParent(card);

        pgTabel = new Paging();
        pgTabel.setPageSize(PAGE_SIZE);
        pgTabel.setTotalSize(d.semua.size());
        pgTabel.setParent(card);

        gridTabel = new MyGrid();
        gridTabel.setWidth("100%");
        gridTabel.setSclass("dgrid");
        gridTabel.setFixedLayout(true);
        gridTabel.setParent(card);

        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(gridTabel);
        buatKol(cols, "Mulai", "90px");
        buatKol(cols, "Kode", "80px");
        buatKol(cols, "Nama Kegiatan", null);
        buatKol(cols, "Aspek", "13%");
        buatKol(cols, "Jabatan", "10%");
        buatKol(cols, "Skala", "10%");
        buatKol(cols, "Diajukan oleh", "13%");
        buatKol(cols, "Status", "90px");

        final List<Item> daftar = d.semua;
        Common.initPagingCustom(pgTabel, new EventListener() {
            public void onEvent(Event e) throws Exception {
                int from = pgTabel.getActivePage() * PAGE_SIZE;
                int to   = Math.min(from + PAGE_SIZE, daftar.size());
                gridTabel.setModel(new SimpleListModel(daftar.subList(from, to)));
            }
        }, PAGE_SIZE);

        gridTabel.setRowRenderer(new MyRowRenderer() {
            public void render(Row row, Object data) throws Exception {
                Item pe = (Item) data;
                SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yy", new Locale("id","ID"));
                sel(row, pe.mulai != null ? sf.format(pe.mulai) : "-");
                sel(row, pe.kode);
                sel(row, pe.nama);
                sel(row, pe.aspek);
                sel(row, pe.jabatan);
                sel(row, pe.skala);
                sel(row, pe.dosen);
                sel(row, pe.status);
            }
        });
        int to = Math.min(PAGE_SIZE, d.semua.size());
        gridTabel.setModel(new SimpleListModel(d.semua.subList(0, to)));
    }

    private void unduhExcel() {
        try {
            if (lastData == null || lastData.semua.isEmpty()) {
                MyMessageboxConfig.show("Tidak ada data untuk diunduh.");
                return;
            }
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFCellStyle hStyle = wb.createCellStyle();
            hStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(120, 53, 15)));
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);

            XSSFSheet sheet = wb.createSheet("Kegiatan Dosen");
            String[] hdrs = { "No","Mulai","Kode","Nama Kegiatan","Aspek","Aspek Rinci","Jabatan","Skala",
                "Diajukan oleh","Fakultas","Prodi","Status","Tempat","Tahun" };
            XSSFRow hRow = sheet.createRow(0);
            for (int i = 0; i < hdrs.length; i++) {
                XSSFCell cell = hRow.createCell(i);
                cell.setCellValue(hdrs[i]); cell.setCellStyle(hStyle);
            }
            SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy");
            List<Item> list = lastData.semua;
            for (int i = 0; i < list.size(); i++) {
                Item pe = list.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(pe.mulai != null ? sf.format(pe.mulai) : "");
                row.createCell(2).setCellValue(pe.kode);
                row.createCell(3).setCellValue(pe.nama);
                row.createCell(4).setCellValue(pe.aspek);
                row.createCell(5).setCellValue(pe.aspekRinci);
                row.createCell(6).setCellValue(pe.jabatan);
                row.createCell(7).setCellValue(pe.skala);
                row.createCell(8).setCellValue(pe.dosen);
                row.createCell(9).setCellValue(pe.fakultas);
                row.createCell(10).setCellValue(pe.prodi);
                row.createCell(11).setCellValue(pe.status);
                row.createCell(12).setCellValue(pe.tempat);
                row.createCell(13).setCellValue(pe.tahun != null ? pe.tahun : 0);
            }
            for (int i = 0; i < hdrs.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            wb.close();
            String namaFile = "Dasbor_Kegiatan_Dosen_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
            org.zkoss.zul.Filedownload.save(baos.toByteArray(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", namaFile);
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ══ Private helpers (sama gaya DasbordPrestasi) ═══════════════════════════
    private Div buatCard() {
        Div dv = new Div(); dv.setSclass("dps-card"); dv.setParent(this); return dv;
    }

    private void h(Component parent, String html) {
        Html node = new Html(); node.setContent(html); node.setParent(parent);
    }

    private void sel(Row row, String val) {
        new org.zkoss.zul.Label(val != null ? val : "").setParent(row);
    }

    private void buatKol(org.zkoss.zul.Columns cols, String label, String width) {
        MyColumnConfig col = new MyColumnConfig();
        col.setLabel(label);
        if (width != null) col.setWidth(width);
        col.setParent(cols);
    }

    private String statCard(int val, String label, String sub, String clr) {
        return "<div class='dps-sc' style='border-top-color:" + clr + ";'>"
            + "<div class='dps-sc-val' style='color:" + clr + ";'>" + val + "</div>"
            + "<div class='dps-sc-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-sc-sub'>" + esc(sub) + "</div></div>";
    }

    private String statCardTeks(String val, String label, String sub, String clr) {
        return "<div class='dps-sc' style='border-top-color:" + clr + ";'>"
            + "<div class='dps-sc-val' style='color:" + clr + ";font-size:18px;'>" + esc(val) + "</div>"
            + "<div class='dps-sc-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-sc-sub'>" + esc(sub) + "</div></div>";
    }

    private String juaraCard(String emoji, String label, int val, String bg, String clr, String dot) {
        return "<div class='dps-jcard' style='background:" + bg + ";color:" + clr + ";border-top:4px solid " + dot + ";'>"
            + "<div style='font-size:24px;'>" + emoji + "</div>"
            + "<div class='dps-jcard-num' style='color:" + dot + ";'>" + val + "</div>"
            + "<div class='dps-jcard-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-jcard-sub'>kegiatan</div></div>";
    }

    private String barRow(String label, int val, int maxVal, String clr) {
        double pct = maxVal > 0 ? (double) val / maxVal * 100 : 0;
        return "<div class='dps-bar-row'>"
            + "<div class='dps-bar-label' title='" + esc(label) + "'>" + esc(cut(label, 14)) + "</div>"
            + "<div class='dps-bar-track'><div class='dps-bar-fill' style='width:" + String.format(Locale.US, "%.1f", pct)
            + "%;background:" + clr + ";'></div></div>"
            + "<div class='dps-bar-val'>" + val + "</div></div>";
    }

    private String kosong() {
        return "<div style='text-align:center;padding:24px;color:#94a3b8;font-size:12px;'>&#128270; Belum ada data untuk ditampilkan.</div>";
    }

    private static String statusBg(String status) {
        if (KegiatanKedosenan.DISETUJUI.equalsIgnoreCase(status))       return "#dcfce7";
        if (KegiatanKedosenan.DITOLAK.equalsIgnoreCase(status))         return "#fee2e2";
        if (KegiatanKedosenan.SEDANG_DIPROSES.equalsIgnoreCase(status)) return "#dbeafe";
        return "#f1f5f9";
    }

    private static String statusFg(String status) {
        if (KegiatanKedosenan.DISETUJUI.equalsIgnoreCase(status))       return "#166534";
        if (KegiatanKedosenan.DITOLAK.equalsIgnoreCase(status))         return "#991b1b";
        if (KegiatanKedosenan.SEDANG_DIPROSES.equalsIgnoreCase(status)) return "var(--ais-theme-primary,#1e40af)";
        return "#475569";
    }

    private static void incr(Map<String,Integer> map, String key) {
        Integer v = map.get(key);
        map.put(key, v == null ? 1 : v + 1);
    }

    private static LinkedHashMap<String,Integer> sortDesc(Map<String,Integer> map) {
        List<Map.Entry<String,Integer>> list = new ArrayList<Map.Entry<String,Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) { return b.getValue().compareTo(a.getValue()); }
        });
        LinkedHashMap<String,Integer> out = new LinkedHashMap<String,Integer>();
        for (Map.Entry<String,Integer> e : list) out.put(e.getKey(), e.getValue());
        return out;
    }

    private static List<Map.Entry<String,Integer>> topN(Map<String,Integer> map, int n) {
        List<Map.Entry<String,Integer>> list = new ArrayList<Map.Entry<String,Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) { return b.getValue().compareTo(a.getValue()); }
        });
        return list.size() > n ? list.subList(0, n) : list;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String ss(Object o) { return o == null ? "" : o.toString().trim(); }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
