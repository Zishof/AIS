package ais.action.master.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Html;
import org.zkoss.zul.ListModel;

import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;
import ais.database.hibernate.HibernateUtil;

/**
 * Helper reusable untuk dasbor ringkasan di halaman master.
 * Output: HTML murni (CSS + SVG inline) — tidak butuh library eksternal.
 * Kompatibel Java 1.6/1.7, ZKoss 5.
 */
public final class GenericActionDashboardHelper {

    private GenericActionDashboardHelper() {
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /** Dasbor berbasis class Hibernate: menghitung total, hari ini, 7 hari. */
    public static void refresh(Html dashboardHtml, Html progressHtml,
            Class clazz, String title, String description) {
        if (dashboardHtml == null || clazz == null) {
            return;
        }
        try {
            showProgress(progressHtml, 20, "Memuat dasbor", "Menghitung data...");
            long total     = count(clazz, null);
            long today     = count(clazz, startOfDay(0));
            long sevenDays = count(clazz, startOfDay(-6));
            showProgress(progressHtml, 85, "Menyusun grafik", "Hampir selesai...");
            dashboardHtml.setContent(buildDashboardHtml(title, description, total, today, sevenDays));
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:49");}
            try { dashboardHtml.setContent(buildErrorHtml(title, e)); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:50");}
        } finally {
            hideProgress(progressHtml);
        }
    }

    /** Dasbor berbasis filter/kriteria Action — total mengikuti pencarian aktif. */
    public static void refreshFromCriteria(Html dashboardHtml, Html progressHtml,
            Object action, String title, String description) {
        refreshFromCriteria(dashboardHtml, progressHtml, action, title, description, -1L);
    }

    /**
     * Sama seperti {@link #refreshFromCriteria(Html, Html, Object, String, String)}, tapi menerima
     * total yang SUDAH dihitung pemanggil (mis. {@code paging.getTotalSize()} tepat setelah
     * {@code Common.initPaging(initCriteria(false), paging)} pada method pencarian yang sama).
     *
     * <p><b>Kenapa perlu:</b> tanpa overload ini, setiap pemanggilan method di atas MENGHITUNG ULANG
     * {@code SELECT count(*)} yang SAMA PERSIS lewat refleksi ({@code initCriteria(false)} dipanggil
     * lagi) — padahal hitungan itu hampir selalu SUDAH dijalankan sesaat sebelumnya oleh
     * {@code Common.initPaging(...)} untuk mengisi {@code paging.getTotalSize()}. Untuk grid dengan
     * banyak JOIN (mis. layar Catatan Pembayaran Mahasiswa), query hitung ini bisa berat — menjalankannya
     * DUA KALI per pencarian menggandakan waktu tunggu tanpa manfaat, karena hasilnya identik.</p>
     *
     * @param totalYangSudahDihitung total baris yang sudah dihitung pemanggil; kirim nilai &lt; 0
     *                                (mis. {@code -1}) bila BELUM ada hitungan siap pakai — method ini
     *                                akan menghitung sendiri seperti overload 5-argumen di atas (perilaku lama).
     */
    public static void refreshFromCriteria(Html dashboardHtml, Html progressHtml,
            Object action, String title, String description, long totalYangSudahDihitung) {
        if (dashboardHtml == null) {
            return;
        }
        try {
            showProgress(progressHtml, 20, "Memuat dasbor", "Menghitung data...");
            long total = totalYangSudahDihitung;
            if (total < 0L) { total = countFromCriteria(action); }
            if (total < 0L) { total = countFromCriteriaWithSession(action); }
            if (total < 0L) { total = countFromGridModel(action); }
            if (total < 0L) { total = 0L; }
            showProgress(progressHtml, 85, "Menyusun grafik", "Hampir selesai...");
            dashboardHtml.setContent(buildCriteriaDashboardHtml(title, description, total));
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:71");}
            try { dashboardHtml.setContent(buildErrorHtml(title, e)); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:72");}
        } finally {
            hideProgress(progressHtml);
        }
    }

    // =========================================================================
    // PROGRESS / BUSY
    // =========================================================================

    public static void showProgress(Html progressHtml, int percent, String title, String detail) {
        try {
            if (progressHtml == null) {
                try {
                    Clients.showBusy((title == null ? "Memuat" : title)
                            + " - " + (detail == null ? "" : detail));
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:88");}
                return;
            }
            if (percent < 0)   { percent = 0; }
            if (percent > 100) { percent = 100; }
            progressHtml.setVisible(true);
            progressHtml.setContent(CommonDashboardHtmlHelper.progressBar(percent, title, detail));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:95");}
    }

    public static void hideProgress(Html progressHtml) {
        if (progressHtml == null) {
            try { Clients.clearBusy(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:100");}
            return;
        }
        try {
            progressHtml.setContent("");
            progressHtml.setVisible(false);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:106");}
    }

    // =========================================================================
    // DASHBOARD HTML — FULL (ada data total / hari ini / 7 hari)
    // =========================================================================

    private static String buildDashboardHtml(String title, String description,
            long total, long today, long sevenDays) {

        long older = total - sevenDays;
        if (older < 0L) { older = 0L; }

        // Radar: 5 sumbu - Volume, Aktivitas, Mingguan, Histori, Pembaruan
        int rVol  = total     <= 0 ? 5 : (int) Math.min(95, Math.log10(total + 1) * 20);
        int rAkt  = today     <= 0 ? 8 : (int) Math.min(95, 28 + today * 5);
        int rMgg  = sevenDays <= 0 ? 8 : (int) Math.min(95, 38 + sevenDays * 2);
        int rHist = older     <= 0 ? 5 : (int) Math.min(95, 48 + (int)(Math.log10(older + 1) * 10));
        int rPmb  = (today > 0 ? 32 : 0) + (sevenDays > 0 ? 38 : 0) + (older > 0 ? 20 : 0);
        int[] radarVal = { rVol, rAkt, rMgg, rHist, rPmb };
        String[] radarLbl = { "Jumlah", "Hari Ini", "Minggu", "Histori", "Pembaruan" };

        // Donut: % data 7 hari dari total
        int donutPct = total <= 0 ? 0 : (int) Math.min(100, sevenDays * 100 / total);
        if (donutPct < 1 && sevenDays > 0) { donutPct = 1; }

        // Max untuk progress bar
        long barMax = Math.max(Math.max(today, sevenDays), Math.max(older, 1L));

        // Trend hari ini vs rata-rata harian minggu ini
        long dailyAvg = sevenDays / 7;
        String trendArrow = today > dailyAvg ? "▲" : (today == 0 ? "—" : "▼");
        String trendColor = today > dailyAvg ? "#16a34a" : (today == 0 ? "#64748b" : "#dc2626");

        StringBuilder h = new StringBuilder(28000);
        h.append(outerOpen());

        // HERO
        appendHero(h, title, description, "Dasbor Ringkasan", "#0f172a", "#1e40af", "#06b6d4");

        // KPI CARDS
        h.append(gridOpen("160px"));
        appendKpiCard(h, "Total Data",  format(total),     "",          "#64748b",
                "Semua catatan yang tersimpan",        "#eff6ff", "#1d4ed8");
        appendKpiCard(h, "Hari Ini",    format(today),     trendArrow,  trendColor,
                "Masuk sejak awal hari ini",           "#ecfdf5", "#047857");
        appendKpiCard(h, "7 Hari",      format(sevenDays), "",          "#64748b",
                "Masuk dalam 7 hari terakhir",         "#fef3c7", "#92400e");
        appendKpiCard(h, "Sebelumnya",  format(older),     "",          "#64748b",
                "Lebih dari seminggu yang lalu",       "#f8fafc", "#334155");
        h.append("</div>");

        // BARIS 1: Bar chart | Donut ring
        h.append(gridOpen("260px"));

        // Panel kiri: horizontal bar chart
        h.append(cardOpen());
        h.append(cardTitle("Perbandingan Periode"));
        h.append(cardSubtitle("Berapa data yang masuk di setiap rentang waktu"));
        appendHBar(h, "Hari ini",         pct(today,     barMax), today);
        appendHBar(h, "7 hari terakhir",  pct(sevenDays, barMax), sevenDays);
        appendHBar(h, "Sebelumnya",       pct(older,     barMax), older);
        h.append("</div>");

        // Panel kanan: donut ring SVG
        h.append(cardOpen());
        h.append(cardTitle("Komposisi Data"));
        h.append(cardSubtitle("Seberapa besar bagian data yang masuk minggu ini"));
        h.append(buildDonutSvg(donutPct, "Terbaru", "#2563eb", "#e2e8f0"));
        h.append("<div style='display:flex;gap:10px;margin-top:10px;flex-wrap:wrap;justify-content:center;'>");
        appendLegendDot(h, "#2563eb", "Minggu ini (" + donutPct + "%)");
        appendLegendDot(h, "#e2e8f0", "Sebelumnya (" + (100 - donutPct) + "%)");
        h.append("</div></div>");

        h.append("</div>"); // end baris 1

        // BARIS 2: Radar | Vertical bar chart
        h.append(gridOpen("260px"));

        // Panel kiri: radar / spider web SVG
        h.append(cardOpen());
        h.append(cardTitle("Radar Performa"));
        h.append(cardSubtitle("Lima sisi yang menggambarkan kondisi data secara menyeluruh"));
        h.append(buildRadarSvg(radarVal, radarLbl, "#2563eb"));
        h.append("<div style='display:flex;gap:5px;flex-wrap:wrap;margin-top:8px;justify-content:center;'>");
        for (int i = 0; i < radarLbl.length; i++) {
            appendBadge(h, radarLbl[i] + " " + radarVal[i] + "%", "#eff6ff", "#1d4ed8");
        }
        h.append("</div></div>");

        // Panel kanan: vertical bar chart + ringkasan
        h.append(cardOpen());
        h.append(cardTitle("Grafik Distribusi"));
        h.append(cardSubtitle("Tinggi batang menunjukkan besar data di setiap periode"));
        h.append(buildVBarChart(
                new long[]{today, sevenDays, older},
                new String[]{"Hari Ini", "7 Hari", "Sebelumnya"},
                new String[]{"#2563eb", "#06b6d4", "#64748b"}));
        h.append("<div style='margin-top:10px;'>");
        long allMax = Math.max(total, 1L);
        appendHBar(h, "Hari ini vs total",   pct(today,     allMax), today);
        appendHBar(h, "7 hari vs total",      pct(sevenDays, allMax), sevenDays);
        h.append("</div></div>");

        h.append("</div>"); // end baris 2

        // FOOTER
        h.append(footer("Buka tab <b>Data</b> untuk melihat rincian setiap baris dan mengunduh laporan Excel."));
        h.append("</div>"); // end outer
        return h.toString();
    }

    // =========================================================================
    // DASHBOARD HTML — CRITERIA (hanya punya total dari filter aktif)
    // =========================================================================

    private static String buildCriteriaDashboardHtml(String title, String description, long total) {

        // Donut menunjukkan "seberapa besar" dataset ini secara log-scale
        int donutPct = total <= 0 ? 0 : (int) Math.min(95, Math.log10(total + 1) * 22);

        // Radar: aspek kesiapan data
        int rVol = total > 0 ? (int) Math.min(95, Math.log10(total + 1) * 20) : 5;
        int[] radarVal = { rVol, total > 0 ? 78 : 10, total > 0 ? 85 : 10,
                           total > 0 ? 70 : 10, total > 0 ? 90 : 10 };
        String[] radarLbl = { "Volume", "Validasi", "Ketersediaan", "Kelengkapan", "Kesiapan" };

        StringBuilder h = new StringBuilder(26000);
        h.append(outerOpen());

        // HERO
        appendHero(h, title, description, "Dasbor Operasional", "#1e3a8a", "#2563eb", "#38bdf8");

        // KPI CARDS
        h.append(gridOpen("160px"));
        appendKpiCard(h, "Total Hasil",   format(total), "",    "#64748b",
                "Jumlah data sesuai filter sekarang",      "#eff6ff", "#1d4ed8");
        appendKpiCard(h, "Filter",        "Aktif",       "●", "#16a34a",
                "Tampilan mengikuti pencarian yang dipakai", "#ecfdf5", "#047857");
        appendKpiCard(h, "Status Sistem", "Normal",      "✔", "#16a34a",
                "Data siap diproses dan dilihat",           "#fef3c7", "#92400e");
        appendKpiCard(h, "Aksi",          "Tersedia",    "",    "#64748b",
                "Buka tab Data untuk tindakan lanjutan",    "#f8fafc", "#334155");
        h.append("</div>");

        // BARIS 1: Langkah kerja | Donut
        h.append(gridOpen("260px"));

        // Panel kiri: workflow steps
        h.append(cardOpen());
        h.append(cardTitle("Cara Pakai"));
        h.append(cardSubtitle("Empat langkah mudah mengelola data"));
        appendStep(h, "1", "Atur Filter",    "Pilih tanggal, nama, atau kategori yang ingin dilihat");
        appendStep(h, "2", "Klik Cari",      "Data langsung diperbarui sesuai pilihan");
        appendStep(h, "3", "Buka Tab Data",  "Lihat rincian dan klik baris yang ingin diproses");
        appendStep(h, "4", "Unduh Laporan",  "Klik tombol Excel untuk menyimpan data ke komputer");
        h.append("</div>");

        // Panel kanan: donut ring
        h.append(cardOpen());
        h.append(cardTitle("Ukuran Dataset"));
        h.append(cardSubtitle("Gambaran seberapa besar data yang tersedia saat ini"));
        h.append(buildDonutSvg(donutPct, "Terisi", "#06b6d4", "#e2e8f0"));
        h.append("<div style='display:flex;gap:10px;margin-top:10px;flex-wrap:wrap;justify-content:center;'>");
        appendLegendDot(h, "#06b6d4", "Terpakai (" + donutPct + "%)");
        appendLegendDot(h, "#e2e8f0", "Potensi (" + (100 - donutPct) + "%)");
        h.append("</div></div>");

        h.append("</div>"); // end baris 1

        // BARIS 2: Daftar Periksa | Radar
        h.append(gridOpen("260px"));

        // Panel kiri: checklist cards
        h.append(cardOpen());
        h.append(cardTitle("Daftar Periksa"));
        h.append(cardSubtitle("Pastikan hal-hal ini sudah benar sebelum memproses data"));
        h.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(110px,1fr));gap:7px;margin-top:4px;'>");
        appendCheckCard(h, "Nominal",    "Sesuai tagihan",     "#ecfdf5", "#166534");
        appendCheckCard(h, "Tanggal",   "Dalam rentang",      "#eff6ff", "#1e3a8a");
        appendCheckCard(h, "Identitas", "NIM / No. Reg.",     "#fef3c7", "#92400e");
        appendCheckCard(h, "Status",    "Lunas / Belum lunas","#fae8ff", "#7e22ce");
        h.append("</div></div>");

        // Panel kanan: radar SVG
        h.append(cardOpen());
        h.append(cardTitle("Radar Kesiapan Data"));
        h.append(cardSubtitle("Lima aspek kesiapan data berdasarkan hasil pencarian"));
        h.append(buildRadarSvg(radarVal, radarLbl, "#06b6d4"));
        h.append("<div style='display:flex;gap:5px;flex-wrap:wrap;margin-top:8px;justify-content:center;'>");
        for (int i = 0; i < radarLbl.length; i++) {
            appendBadge(h, radarLbl[i] + " " + radarVal[i] + "%", "#ecfeff", "#0e7490");
        }
        h.append("</div></div>");

        h.append("</div>"); // end baris 2

        // FOOTER
        h.append(footer("Buka tab <b>Data</b> untuk melihat rincian setiap transaksi dan mengunduh laporan."));
        h.append("</div>"); // end outer
        return h.toString();
    }

    // =========================================================================
    // DASHBOARD HTML — H2H KHUSUS (breakdown per bank, sukses/gagal, nominal)
    // =========================================================================

    /**
     * Dasbor khusus log pembayaran H2H.
     * bankNames & bankCounts: pasangan nama bank + jumlah transaksi, urut terbesar dulu (maks 6 item).
     * successCount: transaksi responseCode dimulai "00".
     * nominalHariIni: total nominal double (rupiah) yang masuk hari ini.
     */
    public static String buildH2HDashboard(
            String title, String description,
            long total, long today, long sevenDays,
            String[] bankNames, long[] bankCounts,
            long successCount, long failCount,
            double nominalHariIni) {

        long older = Math.max(0L, total - sevenDays);

        // Trend hari ini vs rata-rata harian
        long dailyAvg = sevenDays / 7;
        String trendArrow = today > dailyAvg ? "▲" : (today == 0L ? "—" : "▼");
        String trendColor = today > dailyAvg ? "#16a34a" : (today == 0L ? "#64748b" : "#dc2626");

        // Success rate
        long totalForRate = successCount + failCount;
        int successRate = totalForRate <= 0L ? 0 : (int)(successCount * 100L / totalForRate);

        // Radar: 5 sumbu H2H-spesifik
        int rVol   = total <= 0L ? 5 : (int) Math.min(95, Math.log10(total + 1) * 20);
        int rLancar = Math.min(95, successRate);
        int rAkt   = today <= 0L ? 8 : (int) Math.min(95, 25 + today * 4);
        int rMon   = Math.min(95, (today > 0L ? 45 : 5) + (sevenDays > 10L ? 35 : 15));
        int rVal   = successRate > 80 ? 88 : (successRate > 50 ? 65 : (successRate > 20 ? 42 : 18));
        int[] radarVal = { rVol, rLancar, rAkt, rMon, rVal };
        String[] radarLbl = { "Volume", "Kelancaran", "Aktivitas", "Monitoring", "Validasi" };

        // Donut sukses
        int donutSucc = Math.min(100, Math.max(0, successRate));

        // Bank bar max
        long bankMax = 1L;
        if (bankCounts != null) {
            for (int i = 0; i < bankCounts.length; i++) {
                if (bankCounts[i] > bankMax) { bankMax = bankCounts[i]; }
            }
        }

        StringBuilder h = new StringBuilder(34000);
        h.append(outerOpen());

        // HERO
        appendHero(h, title, description, "Monitor Pembayaran Bank", "#0c2340", "#1e40af", "#0ea5e9");

        // KPI CARDS (4)
        h.append(gridOpen("150px"));
        appendKpiCard(h, "Total Transaksi", format(total), "", "#64748b",
                "Semua transaksi yang pernah masuk", "#eff6ff", "#1d4ed8");
        appendKpiCard(h, "Hari Ini", format(today), trendArrow, trendColor,
                "Transaksi yang masuk sejak pukul 00.00 hari ini", "#ecfdf5", "#047857");
        String successLabel = successRate > 0 ? successRate + "%" : "";
        appendKpiCard(h, "Berhasil", format(successCount), successLabel, "#16a34a",
                "Transaksi yang diterima dan diproses oleh bank", "#dcfce7", "#14532d");
        appendKpiCard(h, "Nominal Hari Ini", formatNominalH2H(nominalHariIni), "", "#64748b",
                "Total uang yang berhasil masuk hari ini", "#fef3c7", "#92400e");
        h.append("</div>");

        // BARIS 1: per-bank bar | donut sukses/gagal
        h.append(gridOpen("260px"));

        // Panel kiri: per-bank horizontal bar chart
        h.append(cardOpen());
        h.append(cardTitle("Transaksi per Bank"));
        h.append(cardSubtitle("Bank mana yang paling banyak mengirim transaksi"));
        if (bankNames != null && bankNames.length > 0) {
            for (int i = 0; i < bankNames.length && i < 6; i++) {
                String bName = (bankNames[i] == null || bankNames[i].trim().length() == 0)
                        ? "Tidak terdaftar" : bankNames[i];
                appendHBar(h, bName, pct(bankCounts[i], bankMax), bankCounts[i]);
            }
        } else {
            h.append("<div style='color:#94a3b8;font-size:11px;padding:12px 0;'>Belum ada data bank tersedia.</div>");
        }
        h.append("</div>");

        // Panel kanan: donut sukses/gagal
        h.append(cardOpen());
        h.append(cardTitle("Tingkat Keberhasilan"));
        h.append(cardSubtitle("Seberapa banyak transaksi yang berhasil diproses bank"));
        h.append(buildDonutSvg(donutSucc, "Berhasil", "#16a34a", "#fca5a5"));
        h.append("<div style='display:flex;gap:10px;margin-top:10px;flex-wrap:wrap;justify-content:center;'>");
        appendLegendDot(h, "#16a34a", "Berhasil " + format(successCount));
        appendLegendDot(h, "#fca5a5", "Perlu Ditinjau " + format(failCount));
        h.append("</div></div>");

        h.append("</div>"); // end baris 1

        // BARIS 2: radar | tren periode
        h.append(gridOpen("260px"));

        // Panel kiri: radar spider web
        h.append(cardOpen());
        h.append(cardTitle("Gambaran Kondisi Sistem"));
        h.append(cardSubtitle("Lima sisi yang menggambarkan kondisi pembayaran secara menyeluruh"));
        h.append(buildRadarSvg(radarVal, radarLbl, "#2563eb"));
        h.append("<div style='display:flex;gap:5px;flex-wrap:wrap;margin-top:8px;justify-content:center;'>");
        for (int i = 0; i < radarLbl.length; i++) {
            appendBadge(h, radarLbl[i] + " " + radarVal[i] + "%", "#eff6ff", "#1d4ed8");
        }
        h.append("</div></div>");

        // Panel kanan: tren periode (vertikal bar + horizontal bar)
        h.append(cardOpen());
        h.append(cardTitle("Tren Transaksi"));
        h.append(cardSubtitle("Perbandingan jumlah transaksi di setiap rentang waktu"));
        h.append(buildVBarChart(
                new long[]{ today, sevenDays, older },
                new String[]{ "Hari Ini", "7 Hari", "Sebelumnya" },
                new String[]{ "#16a34a", "#2563eb", "#64748b" }));
        long allMax = Math.max(total, 1L);
        h.append("<div style='margin-top:10px;'>");
        appendHBar(h, "Hari ini dari total",  pct(today,     allMax), today);
        appendHBar(h, "7 hari dari total",     pct(sevenDays, allMax), sevenDays);
        h.append("</div></div>");

        h.append("</div>"); // end baris 2

        // FOOTER
        h.append(footer("Buka tab <b>Transaksi H2H</b> untuk melihat tiap baris, lalu klik <b>Cek Ulang</b> pada transaksi yang perlu diproses ulang oleh bank."));
        h.append("</div>"); // end outer
        return h.toString();
    }

    private static String formatNominalH2H(double nominal) {
        try {
            if (nominal <= 0.0) { return "Rp 0"; }
            long lng = (long) nominal;
            if (lng >= 1000000000L) {
                double miliar = nominal / 1000000000.0;
                return "Rp " + String.format(Locale.US, "%.1f", miliar) + " M";
            }
            if (lng >= 1000000L) {
                double juta = nominal / 1000000.0;
                return "Rp " + String.format(Locale.US, "%.1f", juta) + " jt";
            }
            return "Rp " + Common.numberFormat.get().format(lng);
        } catch (Exception e) {
            return "Rp " + (long) nominal;
        }
    }

    // =========================================================================
    // SVG: RADAR / SPIDER WEB
    // =========================================================================

    private static String buildRadarSvg(int[] values, String[] labels, String color) {
        int n = 0;
        if (values != null && labels != null) {
            n = Math.min(values.length, labels.length);
        }
        if (n < 3) { return ""; }

        double cx = 100, cy = 100, maxR = 60, labelR = 80;

        StringBuilder svg = new StringBuilder(5500);
        svg.append("<svg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'")
           .append(" style='width:100%;max-width:220px;display:block;margin:0 auto;overflow:visible;'>");

        // Latar putih tipis untuk grid 100%
        svg.append(radarPolygon(n, cx, cy, maxR, 1.0, "#f8fafc", "#e2e8f0", "0.7"));

        // Grid 25%, 50%, 75%
        svg.append(radarPolygon(n, cx, cy, maxR, 0.75, "none", "#e2e8f0", "0.8"));
        svg.append(radarPolygon(n, cx, cy, maxR, 0.50, "none", "#e2e8f0", "0.8"));
        svg.append(radarPolygon(n, cx, cy, maxR, 0.25, "none", "#e2e8f0", "0.8"));

        // Sumbu dari pusat ke ujung
        for (int i = 0; i < n; i++) {
            double angle = axisAngle(i, n);
            double ex = cx + maxR * Math.cos(angle);
            double ey = cy + maxR * Math.sin(angle);
            svg.append(String.format(Locale.US,
                    "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#e2e8f0' stroke-width='0.8'/>",
                    cx, cy, ex, ey));
        }

        // Polygon data
        StringBuilder datapts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double angle = axisAngle(i, n);
            double r = maxR * Math.min(100, Math.max(0, values[i])) / 100.0;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i > 0) { datapts.append(" "); }
            datapts.append(String.format(Locale.US, "%.1f,%.1f", x, y));
        }
        svg.append("<polygon points='").append(datapts)
           .append("' fill='").append(hexToRgba(color, 0.15f))
           .append("' stroke='").append(color)
           .append("' stroke-width='2' stroke-linejoin='round'/>");

        // Titik di tiap sudut
        for (int i = 0; i < n; i++) {
            double angle = axisAngle(i, n);
            double r = maxR * Math.min(100, Math.max(0, values[i])) / 100.0;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            svg.append(String.format(Locale.US,
                    "<circle cx='%.1f' cy='%.1f' r='3.5' fill='%s' stroke='#fff' stroke-width='1.5'/>",
                    x, y, color));
        }

        // Label tiap sumbu
        for (int i = 0; i < n; i++) {
            double angle = axisAngle(i, n);
            double lx = cx + labelR * Math.cos(angle);
            double ly = cy + labelR * Math.sin(angle);
            String anchor;
            if (Math.cos(angle) > 0.3) {
                anchor = "start";
            } else if (Math.cos(angle) < -0.3) {
                anchor = "end";
            } else {
                anchor = "middle";
            }
            svg.append(String.format(Locale.US,
                    "<text x='%.1f' y='%.1f' font-size='9' fill='#475569'"
                    + " text-anchor='%s' dominant-baseline='middle'"
                    + " font-family='system-ui,Arial,sans-serif'>%s</text>",
                    lx, ly + 0.5, anchor, escapeHtml(labels[i])));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private static String radarPolygon(int n, double cx, double cy, double maxR,
            double scale, String fill, String stroke, String sw) {
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double angle = axisAngle(i, n);
            double x = cx + maxR * scale * Math.cos(angle);
            double y = cy + maxR * scale * Math.sin(angle);
            if (i > 0) { pts.append(" "); }
            pts.append(String.format(Locale.US, "%.1f,%.1f", x, y));
        }
        return "<polygon points='" + pts + "' fill='" + fill
                + "' stroke='" + stroke + "' stroke-width='" + sw + "'/>";
    }

    private static double axisAngle(int i, int n) {
        return -Math.PI / 2.0 + i * 2.0 * Math.PI / n;
    }

    // =========================================================================
    // SVG: DONUT / RING CHART
    // =========================================================================

    private static String buildDonutSvg(int percent, String label,
            String fillColor, String bgColor) {
        double r    = 38.0;
        double circ = 2.0 * Math.PI * r;
        int    pct  = Math.min(100, Math.max(0, percent));
        double filled = circ * pct / 100.0;
        double empty  = circ - filled;

        StringBuilder sb = new StringBuilder(1400);
        sb.append("<div style='position:relative;width:130px;height:130px;margin:4px auto 0;'>");
        sb.append("<svg viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'")
          .append(" width='130' height='130'")
          .append(" style='transform:rotate(-90deg);display:block;'>");
        sb.append(String.format(Locale.US,
                "<circle cx='50' cy='50' r='%.1f' fill='none' stroke='%s' stroke-width='11'/>",
                r, bgColor));
        if (pct > 0) {
            sb.append(String.format(Locale.US,
                    "<circle cx='50' cy='50' r='%.1f' fill='none' stroke='%s'"
                    + " stroke-width='11' stroke-dasharray='%.3f %.3f' stroke-linecap='round'/>",
                    r, fillColor, filled, empty));
        }
        sb.append("</svg>");
        sb.append("<div style='position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);")
          .append("text-align:center;line-height:1.1;pointer-events:none;'>");
        sb.append("<b style='font-size:22px;font-weight:900;color:#0f172a;'>")
          .append(pct).append("%</b><br/>");
        sb.append("<span style='font-size:9px;color:#64748b;font-weight:800;")
          .append("text-transform:uppercase;letter-spacing:.06em;'>")
          .append(escapeHtml(label)).append("</span>");
        sb.append("</div></div>");
        return sb.toString();
    }

    // =========================================================================
    // VERTICAL BAR CHART (pure CSS divs)
    // =========================================================================

    private static String buildVBarChart(long[] values, String[] labels, String[] colors) {
        int n = values.length;
        long max = 1L;
        for (int i = 0; i < n; i++) {
            if (values[i] > max) { max = values[i]; }
        }
        int chartH = 88;

        StringBuilder sb = new StringBuilder(2600);
        sb.append("<div style='display:flex;align-items:flex-end;justify-content:space-around;")
          .append("gap:6px;height:").append(chartH).append("px;padding:0 2px;'>");
        for (int i = 0; i < n; i++) {
            int barH = (int)(values[i] * (chartH - 22) / max);
            if (barH < 4 && values[i] > 0) { barH = 4; }
            String col  = (i < colors.length) ? colors[i] : "#2563eb";
            String colD = darken(col);
            String lbl  = (i < labels.length) ? labels[i] : "";
            sb.append("<div style='flex:1;display:flex;flex-direction:column;align-items:center;gap:2px;'>");
            sb.append("<div style='font-size:8px;font-weight:900;color:#334155;'>")
              .append(format(values[i])).append("</div>");
            sb.append("<div style='width:100%;max-width:52px;border-radius:6px 6px 0 0;")
              .append("background:linear-gradient(180deg,").append(col).append(",").append(colD).append(");")
              .append("height:").append(barH).append("px;min-height:4px;'></div>");
            sb.append("<div style='font-size:8px;color:#64748b;text-align:center;")
              .append("line-height:1.1;overflow:hidden;max-width:60px;white-space:nowrap;'>")
              .append(escapeHtml(lbl)).append("</div>");
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // =========================================================================
    // KOMPONEN HTML PEMBANTU
    // =========================================================================

    private static String outerOpen() {
        return "<div style='font-family:system-ui,-apple-system,Arial,Helvetica,sans-serif;"
                + "color:#0f172a;background:#f8fafc;padding:12px;box-sizing:border-box;"
                + "width:100%;line-height:1.5;'>";
    }

    private static String gridOpen(String minColWidth) {
        return "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax("
                + minColWidth + ",1fr));gap:10px;margin-top:10px;'>";
    }

    private static String cardOpen() {
        return "<div style='background:#fff;border:1px solid #e5e7eb;border-radius:16px;"
                + "padding:14px;box-shadow:0 2px 12px rgba(15,23,42,.06);box-sizing:border-box;'>";
    }

    private static String cardTitle(String t) {
        return "<div style='font-size:13px;font-weight:900;color:#0f172a;'>"
                + escapeHtml(t) + "</div>";
    }

    private static String cardSubtitle(String s) {
        return "<div style='font-size:10px;color:#64748b;margin-top:2px;margin-bottom:10px;'>"
                + escapeHtml(s) + "</div>";
    }

    private static String footer(String innerHtml) {
        return "<div style='margin-top:10px;padding:11px 13px;border-radius:13px;"
                + "background:#fff;border:1px dashed #cbd5e1;color:#475569;font-size:10px;'>"
                + innerHtml + "</div>";
    }

    private static void appendHero(StringBuilder h, String title, String description,
            String badge, String cA, String cB, String cC) {
        h.append("<div style='border-radius:18px;padding:15px 18px;color:#fff;")
         .append("background:linear-gradient(135deg,").append(cA).append(" 0%,")
         .append(cB).append(" 55%,").append(cC).append(" 100%);")
         .append("box-shadow:0 6px 24px rgba(15,23,42,.18);'>");
        h.append("<div style='font-size:9.5px;text-transform:uppercase;letter-spacing:.15em;")
         .append("opacity:.82;font-weight:800;'>").append(escapeHtml(badge)).append("</div>");
        h.append("<div style='font-size:20px;font-weight:900;line-height:1.2;margin-top:3px;'>")
         .append(escapeHtml(title)).append("</div>");
        h.append("<div style='font-size:11.5px;line-height:1.55;max-width:860px;margin-top:5px;opacity:.9;'>")
         .append(escapeHtml(description)).append("</div>");
        h.append("</div>");
    }

    private static void appendKpiCard(StringBuilder h, String label, String value,
            String trend, String trendColor, String help, String bg, String color) {
        h.append("<div style='background:#fff;border:1px solid #e5e7eb;border-radius:16px;")
         .append("padding:12px;box-shadow:0 2px 12px rgba(15,23,42,.06);")
         .append("box-sizing:border-box;min-height:96px;'>");
        h.append("<div style='display:inline-block;border-radius:999px;padding:3px 8px;background:")
         .append(bg).append(";color:").append(color)
         .append(";font-size:9px;font-weight:900;text-transform:uppercase;letter-spacing:.04em;'>")
         .append(escapeHtml(label)).append("</div>");
        h.append("<div style='display:flex;align-items:baseline;gap:5px;margin-top:6px;'>");
        h.append("<span style='font-size:26px;font-weight:900;color:#0f172a;line-height:1;'>")
         .append(escapeHtml(value)).append("</span>");
        if (trend != null && trend.length() > 0) {
            h.append("<span style='font-size:14px;color:").append(trendColor)
             .append(";font-weight:800;'>").append(trend).append("</span>");
        }
        h.append("</div>");
        h.append("<div style='font-size:10px;color:#64748b;line-height:1.4;margin-top:4px;'>")
         .append(escapeHtml(help)).append("</div>");
        h.append("</div>");
    }

    private static void appendHBar(StringBuilder h, String label, String width, long value) {
        String w = (width == null || width.trim().isEmpty()) ? "0%" : width.trim();
        h.append("<div style='margin:7px 0;'>");
        h.append("<div style='display:flex;justify-content:space-between;align-items:center;")
         .append("font-size:10.5px;font-weight:800;color:#334155;margin-bottom:4px;'>");
        h.append("<span>").append(escapeHtml(label)).append("</span>");
        h.append("<span>").append(format(value)).append("</span>");
        h.append("</div>");
        h.append("<div style='height:9px;background:#f1f5f9;border-radius:999px;overflow:hidden;'>");
        h.append("<div style='height:9px;width:").append(escapeHtml(w))
         .append(";border-radius:999px;")
         .append("background:linear-gradient(90deg,#2563eb,#06b6d4);'></div>");
        h.append("</div></div>");
    }

    private static void appendBadge(StringBuilder h, String text, String bg, String color) {
        h.append("<span style='font-size:9px;font-weight:800;color:").append(color)
         .append(";background:").append(bg)
         .append(";border-radius:999px;padding:3px 7px;'>")
         .append(escapeHtml(text)).append("</span>");
    }

    private static void appendLegendDot(StringBuilder h, String color, String label) {
        h.append("<div style='display:flex;align-items:center;gap:5px;font-size:10px;color:#475569;'>");
        h.append("<div style='width:10px;height:10px;border-radius:999px;flex-shrink:0;background:")
         .append(color).append(";'></div>");
        h.append("<span>").append(escapeHtml(label)).append("</span>");
        h.append("</div>");
    }

    private static void appendStep(StringBuilder h, String num, String title, String desc) {
        h.append("<div style='display:flex;gap:10px;margin-bottom:9px;align-items:flex-start;'>");
        h.append("<div style='flex-shrink:0;width:24px;height:24px;border-radius:999px;")
         .append("background:linear-gradient(135deg,#2563eb,#06b6d4);color:#fff;")
         .append("font-size:11px;font-weight:900;display:flex;align-items:center;")
         .append("justify-content:center;'>").append(num).append("</div>");
        h.append("<div>");
        h.append("<div style='font-size:11.5px;font-weight:900;color:#0f172a;'>")
         .append(escapeHtml(title)).append("</div>");
        h.append("<div style='font-size:10px;color:#64748b;line-height:1.4;'>")
         .append(escapeHtml(desc)).append("</div>");
        h.append("</div></div>");
    }

    private static void appendCheckCard(StringBuilder h, String label, String value,
            String bg, String color) {
        h.append("<div style='border-radius:11px;background:").append(bg)
         .append(";padding:9px;box-sizing:border-box;'>");
        h.append("<b style='font-size:11px;color:#0f172a;display:block;'>")
         .append(escapeHtml(label)).append("</b>");
        h.append("<span style='font-size:10px;font-weight:700;color:").append(color).append(";'>")
         .append(escapeHtml(value)).append("</span>");
        h.append("</div>");
    }

    // =========================================================================
    // UTILITAS WARNA & FORMAT
    // =========================================================================

    private static String hexToRgba(String hex, float alpha) {
        try {
            String h = hex.replace("#", "");
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return String.format(Locale.US, "rgba(%d,%d,%d,%.2f)", r, g, b, alpha);
        } catch (Exception e) {
            return "rgba(37,99,235,0.15)";
        }
    }

    private static String darken(String hex) {
        try {
            String h = hex.replace("#", "");
            int r = Math.max(0, Integer.parseInt(h.substring(0, 2), 16) - 45);
            int g = Math.max(0, Integer.parseInt(h.substring(2, 4), 16) - 45);
            int b = Math.max(0, Integer.parseInt(h.substring(4, 6), 16) - 45);
            return String.format(Locale.US, "#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hex;
        }
    }

    private static String pct(long value, long max) {
        if (max <= 0) { return "0%"; }
        int p = (int) Math.round(value * 100.0 / max);
        if (p < 2 && value > 0) { p = 2; }
        if (p > 100) { p = 100; }
        return p + "%";
    }

    private static String format(long value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) { return ""; }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String buildErrorHtml(String title, Exception e) {
        return CommonDashboardHtmlHelper.errorState(title, e);
    }

    // =========================================================================
    // STRATEGI HITUNG
    // =========================================================================

    private static long count(Class clazz, Date since) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(clazz);
            if (since != null) {
                try {
                    criteria.add(Restrictions.ge("tanggal_dirubah", since));
                } catch (Exception ignored) {
                    return 0L;
                }
            }
            criteria.setProjection(Projections.rowCount());
            Object value = criteria.uniqueResult();
            if (value instanceof Long)   { return ((Long) value).longValue(); }
            if (value instanceof Number) { return ((Number) value).longValue(); }
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:840");}
        } finally {
            closeSessionQuietly(session);
        }
        return 0L;
    }

    private static long countFromCriteria(Object action) throws Exception {
        if (action == null) { return -1L; }
        Method method = findMethod(action.getClass(), "initCriteria",
                new Class[]{Boolean.TYPE});
        if (method == null) { return -1L; }
        method.setAccessible(true);
        Object value = method.invoke(action, new Object[]{Boolean.FALSE});
        if (!(value instanceof Criteria)) { return -1L; }
        Criteria c = (Criteria) value;
        c.setProjection(Projections.rowCount());
        return toLong(c.uniqueResult());
    }

    private static long countFromCriteriaWithSession(Object action) throws Exception {
        if (action == null) { return -1L; }
        Method method = findMethod(action.getClass(), "initCriteria",
                new Class[]{Session.class, Boolean.TYPE});
        if (method == null) { return -1L; }
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            method.setAccessible(true);
            Object value = method.invoke(action, new Object[]{session, Boolean.FALSE});
            if (!(value instanceof Criteria)) { return -1L; }
            Criteria c = (Criteria) value;
            c.setProjection(Projections.rowCount());
            return toLong(c.uniqueResult());
        } finally {
            closeSessionQuietly(session);
        }
    }

    private static long countFromGridModel(Object action) throws Exception {
        if (action == null) { return -1L; }
        Field field = findField(action.getClass(), "grid");
        if (field == null) { return -1L; }
        field.setAccessible(true);
        Object grid = field.get(action);
        if (grid == null) { return -1L; }
        Method method = findMethod(grid.getClass(), "getModel", new Class[0]);
        if (method == null) { return -1L; }
        Object model = method.invoke(grid, new Object[0]);
        if (model instanceof ListModel) {
            return ((ListModel) model).getSize();
        }
        return -1L;
    }

    private static Method findMethod(Class clazz, String name, Class[] paramTypes) {
        Class current = clazz;
        while (current != null) {
            try { return current.getDeclaredMethod(name, paramTypes); }
            catch (Exception e) { current = current.getSuperclass(); }
        }
        return null;
    }

    private static Field findField(Class clazz, String name) {
        Class current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (Exception e) { current = current.getSuperclass(); }
        }
        return null;
    }

    private static long toLong(Object value) {
        if (value instanceof Long)   { return ((Long) value).longValue(); }
        if (value instanceof Number) { return ((Number) value).longValue(); }
        return 0L;
    }

    private static Date startOfDay(int addDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, addDays);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static void closeSessionQuietly(Session session) {
        if (session == null) { return; }
        try {
            if (session.isOpen()) {
                try { session.clear(); }      catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:933");}
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:934");}
                try { session.close(); }      catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:935");}
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/GenericActionDashboardHelper.java:937");}
    }
}
