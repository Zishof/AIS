package ais.action.master.sekolah;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.common.DashboardCacheUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.AktiftasHarianSiswa;
import ais.database.model.sekolah.Siswa;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.json.JSONObject;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Komponen dashboard khusus untuk dashboard aktifitas harian siswa. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String C_PRIMER}, {@code String
 * C_AKSEN}, {@code String C_HIJAU}, {@code String C_MERAH}, {@code String C_KUNING}, {@code String C_MUTED},
 * {@code String C_BG}, {@code String C_CARD}; inisialisasi/lifecycle ({@code doAfterCompose()}, {@code
 * initFilterDropdowns()}); pembacaan/pencarian ({@code onRefreshDashboard()}, {@code loadDataWithCache()},
 * {@code onDownloadExcel()}); pelaporan/ekspor ({@code buildFilterFingerprint()}, {@code renderCss()}, {@code
 * renderHeaderInfo()}, {@code renderStatCards()}, {@code renderDonutTrend()}, {@code renderSpiderChart()});
 * operasi domain lain ({@code fetchData()}, {@code aggregate()}, {@code statCard()}, {@code dowColor()}, {@code
 * h()}, {@code esc()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class DashboardAktifitasHarianSiswaAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    // ── Warna ─────────────────────────────────────────────────────────────────
    private static final String C_PRIMER = "#1e3a5f";
    private static final String C_AKSEN  = "#2563eb";
    private static final String C_HIJAU  = "#16a34a";
    private static final String C_MERAH  = "#dc2626";
    private static final String C_KUNING = "#d97706";
    private static final String C_MUTED  = "#64748b";
    private static final String C_BG     = "#f0f4f8";
    private static final String C_CARD   = "#ffffff";

    // ── ZUL bindings ──────────────────────────────────────────────────────────
    private Combobox searchyayasan;
    private Combobox searchsekolah;
    private Datebox  mulai;
    private Datebox  sampai;
    private Div      dashDiv;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (sampai != null) { sampai.setValue(cal.getTime()); }
        cal.add(Calendar.MONTH, -1);
        if (mulai != null) { mulai.setValue(cal.getTime()); }

        initFilterDropdowns();
        onRefreshDashboard(null);
    }

    // ── Init filter dropdowns ─────────────────────────────────────────────────
    private void initFilterDropdowns() {
        Common.initYayasanDanSekolahDanSemua(searchyayasan, searchsekolah, null, null);
    }

    // ── Refresh dashboard ─────────────────────────────────────────────────────
    public void onRefreshDashboard(Event event) {
        dashDiv.getChildren().clear();

        DashData d = loadDataWithCache();   // L1→L2→L3→DB

        renderCss();
        renderHeaderInfo(d);
        renderStatCards(d);
        renderDonutTrend(d);
        renderSpiderChart(d);
        renderTopSiswa(d);
        renderHeatmapDow(d);
        renderAktivitasTersulit(d);
        renderMateriDistribusi(d);
        renderTabelRekap(d);
    }

    /**
     * Muat DashData dengan caching berlapis:
     * L1 — DashData lokal dikirim ke semua panel (sudah terwujud dalam kode ini).
     * L2 — ZK Desktop attribute per sesi/tab (TTL 5 menit).
     * L3 — Static ConcurrentHashMap lintas sesi (TTL 3 menit).
     *
     * Cache key menyertakan filter (yayasan + sekolah + rentang tanggal)
     * sehingga perubahan filter selalu mengambil data segar dari DB.
     */
    private DashData loadDataWithCache() {
        String fingerprint = buildFilterFingerprint();
        String cacheKey = DashboardCacheUtil.keyWithFilter(
                "DashAktifitas", "ADMIN", null, fingerprint);

        // --- L2: ZK Desktop (per browser-tab) --------------------------------
        Object fromL2 = DashboardCacheUtil.getL2(cacheKey);
        if (fromL2 instanceof DashData) return (DashData) fromL2;

        // --- L3: shared lintas sesi (cocok untuk admin dengan filter sama) ---
        Object fromL3 = DashboardCacheUtil.getL3(cacheKey);
        if (fromL3 instanceof DashData) {
            DashboardCacheUtil.putL2(cacheKey, fromL3);   // pemanasan L2 dari L3
            return (DashData) fromL3;
        }

        // --- DB load ---------------------------------------------------------
        DashData d = new DashData();
        fetchData(d);
        aggregate(d);

        DashboardCacheUtil.putL2(cacheKey, d);
        DashboardCacheUtil.putL3(cacheKey, d);
        return d;
    }

    /**
     * Buat sidik jari filter sebagai bagian dari cache key.
     * Jika filter berubah, key berbeda → cache miss → data segar dari DB.
     */
    private String buildFilterFingerprint() {
        String y = (searchyayasan.getSelectedItem() != null
                    && searchyayasan.getSelectedItem().getValue() != null)
                ? searchyayasan.getSelectedItem().getValue().toString() : "all";
        String s = (searchsekolah.getSelectedItem() != null
                    && searchsekolah.getSelectedItem().getValue() != null)
                ? searchsekolah.getSelectedItem().getValue().toString() : "all";
        String m = (mulai.getValue()  != null) ? String.valueOf(mulai.getValue().getTime())  : "0";
        String p = (sampai.getValue() != null) ? String.valueOf(sampai.getValue().getTime()) : "0";
        return y + "_" + s + "_" + m + "_" + p;
    }

    // ── Fetch data ────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void fetchData(DashData d) {
        Session session = HibernateUtil.currentSession();
        Criteria cr = session.createCriteria(AktiftasHarianSiswa.class)
                .add(Restrictions.eq("aktif", true));

        if (mulai.getValue() != null && sampai.getValue() != null) {
            cr.add(Restrictions.between("tanggal", mulai.getValue(), sampai.getValue()));
        }

        boolean sekolahDipilih  = searchsekolah.getSelectedItem() != null
                && searchsekolah.getSelectedItem().getValue() != null;
        boolean yayasanDipilih  = searchyayasan.getSelectedItem() != null
                && searchyayasan.getSelectedItem().getValue() != null;

        if (sekolahDipilih) {
            cr.createAlias("siswa", "s");
            cr.add(CommonSearchFilterHelper.eqSelectedWithId("s.sekolah", searchsekolah, false));
        } else if (yayasanDipilih) {
            cr.createAlias("siswa", "s");
            cr.add(CommonSearchFilterHelper.eqSelectedWithId("s.yayasan", searchyayasan, false));
        }

        cr.addOrder(Order.asc("tanggal"));
        d.list = ConstantValues.simpleList(cr, AktiftasHarianSiswa.class);
    }

    // ── Aggregate ─────────────────────────────────────────────────────────────
    private void aggregate(DashData d) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", new Locale("id", "ID"));
        Set<String> hariSet  = new HashSet<String>();

        for (AktiftasHarianSiswa a : d.list) {
            Siswa siswa = a.getSiswa();
            if (siswa == null) continue;

            Long siswaId = siswa.getId();
            d.siswaUnikId.add(siswaId);

            if (!d.mapSiswa.containsKey(siswaId)) {
                d.mapSiswa.put(siswaId, siswa);
            }

            // per-siswa slot: [ya, tidak, totalHari]
            if (!d.perSiswa.containsKey(siswaId)) {
                d.perSiswa.put(siswaId, new int[]{0, 0, 0});
            }
            int[] slot = d.perSiswa.get(siswaId);
            slot[2]++;

            // trend harian
            String tglKey = (a.getTanggal() != null) ? sdf.format(a.getTanggal()) : "?";
            hariSet.add(tglKey);
            if (!d.trendHarian.containsKey(tglKey)) {
                d.trendHarian.put(tglKey, 0);
            }
            d.trendHarian.put(tglKey, d.trendHarian.get(tglKey) + 1);

            // komentar ortu
            String pesanOrtu = a.getPesanOrangTua();
            if (pesanOrtu == null || pesanOrtu.trim().isEmpty()) {
                d.belumKomentarOrtu++;
            }

            // parse aktifitas JSON — format: {"1":{"nama":"...","nilai":"YA"},...}
            String aktJson = a.getAktifitas();
            boolean allYa = true;
            int entryYa = 0, entryTotal = 0;
            if (aktJson != null && !aktJson.trim().isEmpty()) {
                try {
                    JSONObject jAkt = new JSONObject(aktJson);
                    java.util.Iterator<String> keys = jAkt.keys();
                    while (keys.hasNext()) {
                        JSONObject obj = jAkt.getJSONObject(keys.next());
                        String namaAkt = obj.optString("nama", "");
                        String nilai   = obj.optString("nilai", "").toUpperCase().trim();
                        if (namaAkt.isEmpty()) continue;
                        boolean ya = "YA".equals(nilai);

                        if (!d.perJenisAkt.containsKey(namaAkt)) {
                            d.perJenisAkt.put(namaAkt, new int[]{0, 0});
                        }
                        int[] ak = d.perJenisAkt.get(namaAkt);
                        ak[1]++;
                        entryTotal++;
                        if (ya) {
                            ak[0]++;
                            d.totalYa++;
                            slot[0]++;
                            entryYa++;
                        } else {
                            allYa = false;
                            d.totalTidak++;
                            slot[1]++;
                        }
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/DashboardAktifitasHarianSiswaAction.java:263");
                    // abaikan JSON tidak valid
                }
            }
            if (entryTotal > 0 && allYa) d.hariSempurna++;

            // per DOW — gunakan entryYa/entryTotal dari entry ini (bukan kumulatif slot)
            if (a.getTanggal() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(a.getTanggal());
                int dow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Min..6=Sab
                if (dow < 0) dow = 0;
                if (dow > 6) dow = 6;
                d.perDowAvg[dow][1]++;
                if (entryTotal > 0) {
                    d.perDowAvg[dow][0] += entryYa * 100 / entryTotal;
                }
            }

            // parse materi JSON — format: {"1":{"nama":"...","nilai":"Baik"},...}
            String matJson = a.getMateri();
            if (matJson != null && !matJson.trim().isEmpty()) {
                try {
                    JSONObject jMat = new JSONObject(matJson);
                    java.util.Iterator<String> mk = jMat.keys();
                    while (mk.hasNext()) {
                        JSONObject obj = jMat.getJSONObject(mk.next());
                        String namaMateri  = obj.optString("nama", "");
                        String nilaiMateri = obj.optString("nilai", "").trim();
                        if (namaMateri.isEmpty()) continue;
                        if (nilaiMateri.isEmpty()) nilaiMateri = "—";

                        if (!d.perMateri.containsKey(namaMateri)) {
                            d.perMateri.put(namaMateri, new LinkedHashMap<String, Integer>());
                        }
                        Map<String, Integer> dist = d.perMateri.get(namaMateri);
                        if (!dist.containsKey(nilaiMateri)) {
                            dist.put(nilaiMateri, 0);
                        }
                        dist.put(nilaiMateri, dist.get(nilaiMateri) + 1);
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/DashboardAktifitasHarianSiswaAction.java:304");
                    // abaikan
                }
            }
        }

        d.totalHari = hariSet.size();
    }

    // ════════════════════════════════════════════════════════════════════════
    // RENDER METHODS
    // ════════════════════════════════════════════════════════════════════════

    // ── 1. CSS ────────────────────────────────────────────────────────────────
    private void renderCss() {
        h(dashDiv,
            "<style>" +
            ".dsh-wrap{font-family:'Segoe UI',Arial,sans-serif;background:" + C_BG + ";padding:16px;}" +
            ".dsh-card{background:" + C_CARD + ";border-radius:14px;" +
              "box-shadow:0 2px 12px rgba(0,0,0,.08);padding:20px;margin-bottom:18px;}" +
            ".dsh-grid-3{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:18px;}" +
            ".dsh-grid-2{display:grid;grid-template-columns:repeat(2,1fr);gap:14px;margin-bottom:18px;}" +
            "@media(max-width:900px){.dsh-grid-3{grid-template-columns:repeat(2,1fr);}}" +
            "@media(max-width:600px){.dsh-grid-3,.dsh-grid-2{grid-template-columns:1fr;}}" +
            ".stat-card{background:" + C_CARD + ";border-radius:14px;" +
              "box-shadow:0 2px 12px rgba(0,0,0,.08);padding:18px 16px;}" +
            ".stat-num{font-size:2rem;font-weight:700;line-height:1.1;}" +
            ".stat-lbl{font-size:.82rem;color:" + C_MUTED + ";margin-top:4px;}" +
            ".sec-title{font-size:1.08rem;font-weight:700;color:" + C_PRIMER + ";margin:0 0 4px 0;}" +
            ".sec-desc{font-size:.82rem;color:" + C_MUTED + ";margin:0 0 14px 0;}" +
            ".hdr-bar{background:" + C_PRIMER + ";color:#fff;border-radius:10px;" +
              "padding:12px 18px;margin-bottom:16px;display:flex;align-items:center;gap:12px;flex-wrap:wrap;}" +
            ".hdr-lbl{font-size:.85rem;opacity:.75;}" +
            ".hdr-val{font-size:.95rem;font-weight:600;}" +
            ".dow-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:6px;}" +
            ".dow-cell{border-radius:8px;height:54px;display:flex;flex-direction:column;" +
              "align-items:center;justify-content:center;font-size:.75rem;font-weight:600;color:#fff;}" +
            ".donut-wrap{display:flex;align-items:flex-start;gap:18px;flex-wrap:wrap;}" +
            ".trend-wrap{flex:1;min-width:200px;}" +
            ".trend-bars{display:flex;align-items:flex-end;gap:4px;height:120px;overflow-x:auto;}" +
            ".trend-bar{flex:0 0 24px;background:" + C_AKSEN + ";border-radius:4px 4px 0 0;}" +
            ".trend-lbl{font-size:.65rem;color:" + C_MUTED + ";text-align:center;margin-top:3px;}" +
            ".spider-wrap{display:flex;justify-content:center;}" +
            ".legend-dot{width:12px;height:12px;border-radius:50%;display:inline-block;margin-right:5px;}" +
            ".mat-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:12px;}" +
            ".mat-card{background:#f8fafc;border-radius:10px;padding:12px;}" +
            "</style>");
    }

    // ── 2. Header info ────────────────────────────────────────────────────────
    private void renderHeaderInfo(DashData d) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
        String dari = (mulai.getValue()  != null) ? sdf.format(mulai.getValue())  : "-";
        String ke   = (sampai.getValue() != null) ? sdf.format(sampai.getValue()) : "-";

        String sekolahLabel;
        boolean sekolahDipilih = searchsekolah.getSelectedItem() != null
                && searchsekolah.getSelectedItem().getValue() != null;
        boolean yayasanDipilih = searchyayasan.getSelectedItem() != null
                && searchyayasan.getSelectedItem().getValue() != null;

        if (sekolahDipilih) {
            sekolahLabel = searchsekolah.getSelectedItem().getLabel();
        } else if (yayasanDipilih) {
            sekolahLabel = "Yayasan: " + searchyayasan.getSelectedItem().getLabel();
        } else {
            sekolahLabel = "Semua Sekolah";
        }

        h(dashDiv,
            "<div class='dsh-wrap'>" +
            "<div class='hdr-bar'>" +
            "<div><div class='hdr-lbl'>Periode</div>" +
              "<div class='hdr-val'>" + esc(dari) + " &ndash; " + esc(ke) + "</div></div>" +
            "<div style='width:1px;height:32px;background:rgba(255,255,255,.3);'></div>" +
            "<div><div class='hdr-lbl'>Lingkup</div>" +
              "<div class='hdr-val'>" + esc(sekolahLabel) + "</div></div>" +
            "<div style='width:1px;height:32px;background:rgba(255,255,255,.3);'></div>" +
            "<div><div class='hdr-lbl'>Total Catatan</div>" +
              "<div class='hdr-val'>" + d.list.size() + " entri</div></div>" +
            "</div>" +
            "</div>");
    }

    // ── 3. Stat cards ─────────────────────────────────────────────────────────
    private void renderStatCards(DashData d) {
        int total    = d.totalYa + d.totalTidak;
        int persenYa = (total > 0) ? (d.totalYa * 100 / total) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='padding:0 16px;'><div class='dsh-grid-3'>");
        sb.append(statCard(String.valueOf(d.siswaUnikId.size()), "Total Siswa Aktif",          C_AKSEN));
        sb.append(statCard(String.valueOf(d.totalHari),          "Hari dengan Data",           C_PRIMER));
        sb.append(statCard(persenYa + "%",                       "Rata-rata Kepatuhan YA",     C_HIJAU));
        sb.append(statCard(String.valueOf(d.hariSempurna),       "Hari Sempurna (semua YA)",   C_KUNING));
        sb.append(statCard(String.valueOf(d.belumKomentarOrtu),  "Belum Komentar Ortu",        C_MERAH));
        sb.append(statCard(String.valueOf(d.list.size()),        "Total Entri Aktivitas",      C_MUTED));
        sb.append("</div></div>");
        h(dashDiv, sb.toString());
    }

    private String statCard(String num, String lbl, String warna) {
        return "<div class='stat-card'>" +
               "<div class='stat-num' style='color:" + warna + ";'>" + esc(num) + "</div>" +
               "<div class='stat-lbl'>" + esc(lbl) + "</div>" +
               "</div>";
    }

    // ── 4. Donut + Trend harian ───────────────────────────────────────────────
    private void renderDonutTrend(DashData d) {
        int total    = d.totalYa + d.totalTidak;
        int persenYa = (total > 0) ? (d.totalYa * 100 / total) : 0;
        int persenTdk = 100 - persenYa;

        double circumference = 2.0 * Math.PI * 54.0;
        double dashYa  = circumference * persenYa  / 100.0;
        double dashTdk = circumference * persenTdk / 100.0;

        String donutSvg =
            "<svg width='140' height='140' viewBox='0 0 140 140'>" +
            "<circle cx='70' cy='70' r='54' fill='none' stroke='#e2e8f0' stroke-width='18'/>" +
            "<circle cx='70' cy='70' r='54' fill='none' stroke='" + C_HIJAU + "' stroke-width='18'" +
              " stroke-dasharray='" + fmtSVG(dashYa)  + " " + fmtSVG(circumference - dashYa)  + "'" +
              " transform='rotate(-90 70 70)'/>" +
            "<circle cx='70' cy='70' r='54' fill='none' stroke='" + C_MERAH + "' stroke-width='18'" +
              " stroke-dasharray='" + fmtSVG(dashTdk) + " " + fmtSVG(circumference - dashTdk) + "'" +
              " stroke-dashoffset='" + fmtSVG(-(circumference * persenYa / 100.0)) + "'" +
              " transform='rotate(-90 70 70)'/>" +
            "<text x='70' y='66' text-anchor='middle' font-size='22' font-weight='bold'" +
              " fill='" + C_PRIMER + "'>" + persenYa + "%</text>" +
            "<text x='70' y='84' text-anchor='middle' font-size='11'" +
              " fill='" + C_MUTED + "'>Kepatuhan</text>" +
            "</svg>";

        String legend =
            "<div style='margin-top:8px;'>" +
            "<span><span class='legend-dot' style='background:" + C_HIJAU + ";'></span>" +
              "YA: " + d.totalYa + "</span>&nbsp;&nbsp;" +
            "<span><span class='legend-dot' style='background:" + C_MERAH + ";'></span>" +
              "TIDAK: " + d.totalTidak + "</span>" +
            "</div>";

        // Trend bars – maks 20 hari terakhir
        List<String> tglKeys = new ArrayList<String>(d.trendHarian.keySet());
        int maxTrend = 1;
        for (String k : tglKeys) {
            int v = d.trendHarian.get(k);
            if (v > maxTrend) maxTrend = v;
        }
        int startIdx = Math.max(0, tglKeys.size() - 20);
        StringBuilder bars = new StringBuilder();
        for (int i = startIdx; i < tglKeys.size(); i++) {
            String k    = tglKeys.get(i);
            int    cnt  = d.trendHarian.get(k);
            int    pHgt = (maxTrend > 0) ? (cnt * 100 / maxTrend) : 0;
            bars.append("<div style='display:flex;flex-direction:column;align-items:center;'>");
            bars.append("<div class='trend-bar' style='height:" + pHgt + "%;'" +
                " title='" + esc(k) + ": " + cnt + "'></div>");
            bars.append("<div class='trend-lbl'>" + esc(k) + "</div>");
            bars.append("</div>");
        }

        h(dashDiv,
            "<div style='padding:0 16px;'><div class='dsh-card'>" +
            "<p class='sec-title'>Distribusi Kepatuhan &amp; Tren Harian</p>" +
            "<p class='sec-desc'>Proporsi aktivitas yang terpenuhi (YA) dibanding tidak terpenuhi," +
              " serta jumlah catatan per hari.</p>" +
            "<div class='donut-wrap'>" +
            "<div>" + donutSvg + legend + "</div>" +
            "<div class='trend-wrap'>" +
            "<div style='font-size:.82rem;color:" + C_MUTED + ";margin-bottom:6px;'>" +
              "Entri per Hari (maks 20 hari terakhir)</div>" +
            "<div class='trend-bars'>" + bars.toString() + "</div>" +
            "</div>" +
            "</div>" +
            "</div></div>");
    }

    // ── 5. Spider / Radar chart ───────────────────────────────────────────────
    private void renderSpiderChart(DashData d) {
        List<String> aktNames = new ArrayList<String>(d.perJenisAkt.keySet());
        if (aktNames.size() < 3) {
            h(dashDiv,
                "<div style='padding:0 16px;'><div class='dsh-card'>" +
                "<p class='sec-title'>Radar Jenis Aktivitas</p>" +
                "<p class='sec-desc'>Butuh minimal 3 jenis aktivitas untuk menampilkan radar.</p>" +
                "</div></div>");
            return;
        }

        int n   = Math.min(aktNames.size(), 8);
        int cx  = 160;
        int cy  = 160;
        int r   = 110;
        int rIn = 28;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg width='320' height='320' viewBox='0 0 320 320'>");

        // Background circles
        for (int lvl = 1; lvl <= 4; lvl++) {
            int rLvl = rIn + (r - rIn) * lvl / 4;
            svg.append("<circle cx='" + cx + "' cy='" + cy + "' r='" + rLvl +
                "' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
        }

        // Axis lines & labels
        for (int i = 0; i < n; i++) {
            double angle = 2.0 * Math.PI * i / n - Math.PI / 2.0;
            int    x2    = cx + (int) (r * Math.cos(angle));
            int    y2    = cy + (int) (r * Math.sin(angle));
            svg.append("<line x1='" + cx + "' y1='" + cy + "' x2='" + x2 + "' y2='" + y2 +
                "' stroke='#cbd5e1' stroke-width='1'/>");

            String lbl = aktNames.get(i);
            if (lbl.length() > 10) lbl = lbl.substring(0, 9) + "…";
            int lx = cx + (int) ((r + 16) * Math.cos(angle));
            int ly = cy + (int) ((r + 16) * Math.sin(angle));
            svg.append("<text x='" + lx + "' y='" + ly +
                "' text-anchor='middle' dominant-baseline='middle'" +
                " font-size='9' fill='" + C_MUTED + "'>" + esc(lbl) + "</text>");
        }

        // Data polygon
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double angle  = 2.0 * Math.PI * i / n - Math.PI / 2.0;
            int[]  ak     = d.perJenisAkt.get(aktNames.get(i));
            double persen = (ak[1] > 0) ? (double) ak[0] / ak[1] : 0.0;
            int    rv     = rIn + (int) ((r - rIn) * persen);
            int    px     = cx + (int) (rv * Math.cos(angle));
            int    py     = cy + (int) (rv * Math.sin(angle));
            if (pts.length() > 0) pts.append(" ");
            pts.append(px).append(",").append(py);
        }
        svg.append("<polygon points='" + pts.toString() +
            "' fill='" + C_AKSEN + "' fill-opacity='.25'" +
            " stroke='" + C_AKSEN + "' stroke-width='2'/>");
        svg.append("</svg>");

        h(dashDiv,
            "<div style='padding:0 16px;'><div class='dsh-card'>" +
            "<p class='sec-title'>Radar Jenis Aktivitas</p>" +
            "<p class='sec-desc'>Seberapa jauh setiap jenis aktivitas terpenuhi" +
              " dibandingkan total pencatatannya.</p>" +
            "<div class='spider-wrap'>" + svg.toString() + "</div>" +
            "</div></div>");
    }

    // ── 6. Top siswa horizontal bar ───────────────────────────────────────────
    private void renderTopSiswa(final DashData d) {
        List<Long> ids = new ArrayList<Long>(d.perSiswa.keySet());
        Collections.sort(ids, new Comparator<Long>() {
            @Override
            public int compare(Long a, Long b) {
                int[] sa = d.perSiswa.get(a);
                int[] sb = d.perSiswa.get(b);
                int pa = (sa[0] + sa[1]) > 0 ? sa[0] * 100 / (sa[0] + sa[1]) : 0;
                int pb = (sb[0] + sb[1]) > 0 ? sb[0] * 100 / (sb[0] + sb[1]) : 0;
                return pb - pa;
            }
        });

        int limit = Math.min(8, ids.size());
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='padding:0 16px;'><div class='dsh-card'>");
        sb.append("<p class='sec-title'>Top Siswa Berdasarkan Skor Kepatuhan</p>");
        sb.append("<p class='sec-desc'>Delapan siswa dengan persentase aktivitas YA tertinggi" +
            " dalam periode yang dipilih.</p>");

        for (int i = 0; i < limit; i++) {
            Long   id    = ids.get(i);
            int[]  slot  = d.perSiswa.get(id);
            int    total = slot[0] + slot[1];
            int    pct   = (total > 0) ? (slot[0] * 100 / total) : 0;
            Siswa  sw    = d.mapSiswa.get(id);
            String nama  = (sw != null) ? sw.getNamaSiswa() : "Siswa #" + id;

            sb.append("<div style='margin-bottom:8px;'>");
            sb.append("<div style='display:flex;justify-content:space-between;" +
                "font-size:.82rem;margin-bottom:2px;'>");
            sb.append("<span>" + esc(nama) + "</span>" +
                "<span style='color:" + C_MUTED + ";'>" + pct + "%</span>");
            sb.append("</div>");
            sb.append("<div style='background:#e2e8f0;border-radius:5px;height:16px;'>");
            sb.append("<div style='width:" + pct + "%;height:100%;background:" + C_AKSEN +
                ";border-radius:5px;'></div>");
            sb.append("</div></div>");
        }
        sb.append("</div></div>");
        h(dashDiv, sb.toString());
    }

    // ── 7. Heatmap hari dalam seminggu ────────────────────────────────────────
    private void renderHeatmapDow(DashData d) {
        String[] dowLabels = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='padding:0 16px;'><div class='dsh-card'>");
        sb.append("<p class='sec-title'>Kepatuhan per Hari dalam Seminggu</p>");
        sb.append("<p class='sec-desc'>Rata-rata tingkat kepatuhan aktivitas untuk setiap hari" +
            " dalam seminggu. Warna lebih gelap berarti lebih tinggi.</p>");
        sb.append("<div class='dow-grid'>");

        for (int dow = 0; dow < 7; dow++) {
            int sumPct = d.perDowAvg[dow][0];
            int cnt    = d.perDowAvg[dow][1];
            int avg    = (cnt > 0) ? (sumPct / cnt) : 0;
            String bg  = dowColor(avg);

            sb.append("<div class='dow-cell' style='background:" + bg + ";'>");
            sb.append("<div>" + dowLabels[dow] + "</div>");
            sb.append("<div style='font-size:.9rem;'>" + avg + "%</div>");
            sb.append("</div>");
        }
        sb.append("</div></div></div>");
        h(dashDiv, sb.toString());
    }

    private String dowColor(int pct) {
        if (pct <= 0)  return "#e2e8f0";
        if (pct <= 25) return "#bfdbfe";
        if (pct <= 50) return "#93c5fd";
        if (pct <= 75) return "#3b82f6";
        return C_AKSEN;
    }

    // ── 8. Aktivitas tersulit ─────────────────────────────────────────────────
    private void renderAktivitasTersulit(final DashData d) {
        List<String> aktNames = new ArrayList<String>(d.perJenisAkt.keySet());
        Collections.sort(aktNames, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int[] sa = d.perJenisAkt.get(a);
                int[] sb = d.perJenisAkt.get(b);
                int tidakA = sa[1] - sa[0];
                int tidakB = sb[1] - sb[0];
                return tidakB - tidakA;
            }
        });

        int limit  = Math.min(8, aktNames.size());
        int maxTdk = 1;
        for (int i = 0; i < limit; i++) {
            int[] ak  = d.perJenisAkt.get(aktNames.get(i));
            int   tdk = ak[1] - ak[0];
            if (tdk > maxTdk) maxTdk = tdk;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='padding:0 16px;'><div class='dsh-card'>");
        sb.append("<p class='sec-title'>Aktivitas Paling Sering Tidak Terpenuhi</p>");
        sb.append("<p class='sec-desc'>Jenis aktivitas yang paling banyak bernilai TIDAK —" +
            " berguna untuk menentukan prioritas pembinaan.</p>");

        for (int i = 0; i < limit; i++) {
            String nama = aktNames.get(i);
            int[]  ak   = d.perJenisAkt.get(nama);
            int    tdk  = ak[1] - ak[0];
            int    pct  = (maxTdk > 0) ? (tdk * 100 / maxTdk) : 0;

            sb.append("<div style='margin-bottom:8px;'>");
            sb.append("<div style='display:flex;justify-content:space-between;" +
                "font-size:.82rem;margin-bottom:2px;'>");
            sb.append("<span>" + esc(nama) + "</span>" +
                "<span style='color:" + C_MERAH + ";'>" + tdk + "x TIDAK</span>");
            sb.append("</div>");
            sb.append("<div style='background:#fee2e2;border-radius:5px;height:16px;'>");
            sb.append("<div style='width:" + pct + "%;height:100%;background:" + C_MERAH +
                ";border-radius:5px;'></div>");
            sb.append("</div></div>");
        }
        sb.append("</div></div>");
        h(dashDiv, sb.toString());
    }

    // ── 9. Distribusi materi ──────────────────────────────────────────────────
    private void renderMateriDistribusi(DashData d) {
        if (d.perMateri.isEmpty()) {
            h(dashDiv,
                "<div style='padding:0 16px;'><div class='dsh-card'>" +
                "<p class='sec-title'>Distribusi Materi</p>" +
                "<p class='sec-desc'>Belum ada data materi untuk ditampilkan.</p>" +
                "</div></div>");
            return;
        }

        String[] barColors = {C_AKSEN, C_HIJAU, C_KUNING, C_MERAH, C_PRIMER, C_MUTED};

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='padding:0 16px;'><div class='dsh-card'>");
        sb.append("<p class='sec-title'>Distribusi Nilai Materi</p>");
        sb.append("<p class='sec-desc'>Sebaran nilai yang diberikan pada setiap jenis materi" +
            " pembelajaran oleh siswa.</p>");
        sb.append("<div class='mat-grid'>");

        for (Map.Entry<String, Map<String, Integer>> entry : d.perMateri.entrySet()) {
            String               namaMateri = entry.getKey();
            Map<String, Integer> dist       = entry.getValue();

            int totalMateri = 0;
            for (int v : dist.values()) totalMateri += v;

            sb.append("<div class='mat-card'>");
            sb.append("<div style='font-size:.85rem;font-weight:600;color:" + C_PRIMER +
                ";margin-bottom:6px;'>" + esc(namaMateri) + "</div>");

            int colorIdx = 0;
            for (Map.Entry<String, Integer> nv : dist.entrySet()) {
                String nilaiKey = nv.getKey();
                int    cnt      = nv.getValue();
                int    pct      = (totalMateri > 0) ? (cnt * 100 / totalMateri) : 0;
                String barColor = barColors[colorIdx % barColors.length];
                colorIdx++;

                sb.append("<div style='margin-top:4px;'>");
                sb.append("<div style='display:flex;justify-content:space-between;" +
                    "font-size:.75rem;color:" + C_MUTED + ";'>");
                sb.append("<span>" + esc(nilaiKey) + "</span><span>" + pct + "%</span>");
                sb.append("</div>");
                sb.append("<div style='background:#e2e8f0;border-radius:4px;height:10px;'>");
                sb.append("<div style='width:" + pct + "%;height:100%;background:" + barColor +
                    ";border-radius:4px;'></div>");
                sb.append("</div></div>");
            }
            sb.append("</div>");
        }

        sb.append("</div></div></div>");
        h(dashDiv, sb.toString());
    }

    // ── 10. Tabel rekap per siswa (ZK Grid) ──────────────────────────────────
    private void renderTabelRekap(final DashData d) {
        renderSectionHeader("Rekap Kinerja per Siswa",
            "Ringkasan capaian setiap siswa: jumlah hari hadir, skor aktivitas YA dan TIDAK," +
            " persentase kepatuhan, serta status komentar orang tua.");

        Grid grid = new Grid();
        grid.setMold("paging");
        grid.setPageSize(10);
        grid.setStyle("margin:0 16px 18px 16px;");

        Columns cols = new Columns();
        cols.setParent(grid);

        String[] colNames = {"NIS", "Nama Siswa", "Total Hari", "Skor YA", "Skor TIDAK",
                             "% Selesai", "Komentar Ortu"};
        int[] colWidths    = {80, 180, 80, 80, 90, 90, 110};
        for (int i = 0; i < colNames.length; i++) {
            Column col = new Column(colNames[i]);
            col.setWidth(colWidths[i] + "px");
            col.setParent(cols);
        }

        Rows rows = new Rows();
        rows.setParent(grid);

        // Sort by % descending
        List<Long> ids = new ArrayList<Long>(d.perSiswa.keySet());
        Collections.sort(ids, new Comparator<Long>() {
            @Override
            public int compare(Long a, Long b) {
                int[] sa = d.perSiswa.get(a);
                int[] sb = d.perSiswa.get(b);
                int pa = (sa[0] + sa[1]) > 0 ? sa[0] * 100 / (sa[0] + sa[1]) : 0;
                int pb = (sb[0] + sb[1]) > 0 ? sb[0] * 100 / (sb[0] + sb[1]) : 0;
                return pb - pa;
            }
        });

        for (Long id : ids) {
            int[]  slot      = d.perSiswa.get(id);
            Siswa  sw        = d.mapSiswa.get(id);
            String nis       = (sw != null && sw.getNomorInduk() != null) ? sw.getNomorInduk() : "-";
            String nama      = (sw != null) ? sw.getNamaSiswa() : "Siswa #" + id;
            int    totalYaTdk = slot[0] + slot[1];
            int    pct        = (totalYaTdk > 0) ? (slot[0] * 100 / totalYaTdk) : 0;
            int    totalHari  = slot[2];

            // Hitung komentar ortu per siswa
            int komentarCount = 0;
            for (AktiftasHarianSiswa a : d.list) {
                if (a.getSiswa() != null && id.equals(a.getSiswa().getId())) {
                    String pesan = a.getPesanOrangTua();
                    if (pesan != null && !pesan.trim().isEmpty()) komentarCount++;
                }
            }

            Row row = new Row();
            new Label(nis).setParent(row);
            new Label(nama).setParent(row);
            new Label(String.valueOf(totalHari)).setParent(row);
            new Label(String.valueOf(slot[0])).setParent(row);
            new Label(String.valueOf(slot[1])).setParent(row);
            new Label(pct + "%").setParent(row);
            new Label(komentarCount + "/" + totalHari).setParent(row);
            row.setParent(rows);
        }

        grid.setParent(dashDiv);
    }

    // ── Excel export ──────────────────────────────────────────────────────────
    public void onDownloadExcel(Event event) {
        try {
            DashData d = new DashData();
            fetchData(d);
            aggregate(d);

            XSSFWorkbook wb    = new XSSFWorkbook();
            XSSFSheet    sheet = wb.createSheet("Rekap Aktivitas Harian");

            String[] hdrs = {"NIS", "Nama Siswa", "Kelas", "Sekolah", "Tanggal",
                             "Nama Aktivitas", "Keterangan", "Pesan Pembina",
                             "Pesan Ortu", "Skor YA", "Skor TIDAK"};

            XSSFRow hdrRow = sheet.createRow(0);
            for (int i = 0; i < hdrs.length; i++) {
                XSSFCell cell = hdrRow.createCell(i);
                cell.setCellValue(hdrs[i]);
            }

            SimpleDateFormat sdfOut = new SimpleDateFormat("dd/MM/yyyy");
            int rowNum = 1;
            for (AktiftasHarianSiswa a : d.list) {
                XSSFRow row = sheet.createRow(rowNum++);
                Siswa   sw  = a.getSiswa();
                row.createCell(0).setCellValue(
                    (sw != null && sw.getNomorInduk() != null) ? sw.getNomorInduk() : "");
                row.createCell(1).setCellValue(sw != null ? sw.getNamaSiswa() : "");
                row.createCell(2).setCellValue(
                    (sw != null && sw.getKelas()   != null) ? sw.getKelas().toString()   : "");
                row.createCell(3).setCellValue(
                    (sw != null && sw.getSekolah() != null) ? sw.getSekolah().toString() : "");
                row.createCell(4).setCellValue(
                    a.getTanggal() != null ? sdfOut.format(a.getTanggal()) : "");
                row.createCell(5).setCellValue(
                    a.getNama()          != null ? a.getNama()          : "");
                row.createCell(6).setCellValue(
                    a.getKeterangan()    != null ? a.getKeterangan()    : "");
                row.createCell(7).setCellValue(
                    a.getPesanPembina()  != null ? a.getPesanPembina()  : "");
                row.createCell(8).setCellValue(
                    a.getPesanOrangTua() != null ? a.getPesanOrangTua() : "");

                // Count YA/TIDAK dari JSON — format: {"1":{"nama":"...","nilai":"YA"},...}
                int ya = 0, tidak = 0;
                String aktJson = a.getAktifitas();
                if (aktJson != null && !aktJson.trim().isEmpty()) {
                    try {
                        JSONObject jAkt2 = new JSONObject(aktJson);
                        java.util.Iterator<String> ki = jAkt2.keys();
                        while (ki.hasNext()) {
                            JSONObject obj   = jAkt2.getJSONObject(ki.next());
                            String     nilai = obj.optString("nilai", "").toUpperCase().trim();
                            if ("YA".equals(nilai)) ya++; else if (!nilai.isEmpty()) tidak++;
                        }
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/DashboardAktifitasHarianSiswaAction.java:862");
                        // abaikan
                    }
                }
                row.createCell(9).setCellValue(ya);
                row.createCell(10).setCellValue(tidak);
            }

            for (int i = 0; i < hdrs.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            wb.close();

            SimpleDateFormat sdfFile = new SimpleDateFormat("yyyyMMdd_HHmm");
            String fileName = "Aktivitas_Harian_" + sdfFile.format(new Date()) + ".xlsx";
            org.zkoss.zul.Filedownload.save(
                baos.toByteArray(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileName);

        } catch (Exception ex) {
            ais.common.Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ── PDF stub ──────────────────────────────────────────────────────────────
    public void onPrintPDF(Event event) {
        try {
            MyMessageboxConfig.show("Fitur cetak PDF belum tersedia.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private static void h(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    private String fmtSVG(double val) {
        return String.format(Locale.US, "%.2f", val);
    }

    private void renderSectionHeader(String judul, String deskripsi) {
        h(dashDiv,
            "<div style='padding:0 16px 0 16px;margin-top:4px;'>" +
            "<p class='sec-title'>" + esc(judul) + "</p>" +
            "<p class='sec-desc'>"  + esc(deskripsi) + "</p>" +
            "</div>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // INNER DATA CLASS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Pembawa data/helper lokal milik {@link DashboardAktifitasHarianSiswaAction} untuk dash data. Tipe ini
     * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
     * jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DashboardAktifitasHarianSiswaAction}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List list}, {@code int totalHari},
     * {@code Set siswaUnikId}, {@code int totalYa}, {@code int totalTidak}, {@code int hariSempurna}, {@code int
     * belumKomentarOrtu}, {@code Map trendHarian}. Aturan bisnis bersama tetap berada pada kelas induk atau
     * service yang dipanggilnya.</p>
     *
     * @see DashboardAktifitasHarianSiswaAction
     */
    static final class DashData {
        List<AktiftasHarianSiswa>         list              = new ArrayList<AktiftasHarianSiswa>();
        int                               totalHari;
        Set<Long>                         siswaUnikId       = new HashSet<Long>();
        int                               totalYa;
        int                               totalTidak;
        int                               hariSempurna;
        int                               belumKomentarOrtu;
        // trend: formattedDate → count entries
        Map<String, Integer>              trendHarian       = new TreeMap<String, Integer>();
        // per siswa: siswaId → [ya, tidak, totalHari]
        Map<Long, int[]>                  perSiswa          = new LinkedHashMap<Long, int[]>();
        Map<Long, Siswa>                  mapSiswa          = new LinkedHashMap<Long, Siswa>();
        // per jenis aktivitas: namaAkt → [jumlahYa, jumlahTotal]
        Map<String, int[]>                perJenisAkt       = new LinkedHashMap<String, int[]>();
        // per hari minggu: 0=Min..6=Sab → [sumPersen, count]
        int[][]                           perDowAvg         = new int[7][2];
        // per jenis materi: namaMateri → (nilai → count)
        Map<String, Map<String, Integer>> perMateri         = new LinkedHashMap<String, Map<String, Integer>>();
    }
}
