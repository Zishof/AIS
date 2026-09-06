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
 * Helper reusable (kelas utilitas statis, {@code final}, tidak dapat diinstansiasi/diturunkan)
 * untuk membangun dasbor ringkasan HTML di halaman master — dipakai SERAGAM oleh banyak
 * {@code *Action} sebagai pengganti dasbor ad-hoc per halaman. Output berupa HTML murni (CSS
 * inline + SVG inline untuk grafik radar/donut/bar) sehingga tidak butuh library grafik
 * eksternal apa pun di sisi client.
 *
 * <p><b>Dua gaya dasbor utama</b> (dipilih pemanggil sesuai kebutuhan data yang tersedia):
 * <ul>
 *   <li>{@link #refresh(Html, Html, Class, String, String)} — dasbor "penuh" (total/hari ini/
 *       7 hari) yang menghitung sendiri lewat Hibernate {@code Criteria} pada satu entity
 *       ({@code clazz}), memakai kolom {@code tanggal_dirubah} untuk memfilter rentang waktu.</li>
 *   <li>{@link #refreshFromCriteria(Html, Html, Object, String, String)}/
 *       {@link #refreshFromCriteria(Html, Html, Object, String, String, long)} — dasbor
 *       "ringkas" (hanya total) yang mengikuti filter/kriteria aktif pada Action pemanggil,
 *       ditemukan lewat REFLEKSI (method {@code initCriteria}, atau fallback field
 *       {@code grid}) — lihat {@link #countFromCriteria(Object)},
 *       {@link #countFromCriteriaWithSession(Object)}, {@link #countFromGridModel(Object)}.</li>
 * </ul>
 * Selain itu {@link #buildH2HDashboard} menyediakan varian dasbor khusus untuk log pembayaran
 * H2H (breakdown per bank, tingkat keberhasilan, nominal).</p>
 *
 * <p><b>Progress bar.</b> Seluruh method publik di atas menampilkan progres pemuatan lewat
 * {@link #showProgress(Html, int, String, String)} (atau {@code Clients.showBusy} bila
 * {@code progressHtml} tidak disediakan) dan menutupnya di blok {@code finally} lewat
 * {@link #hideProgress(Html)}, sehingga indikator loading TIDAK PERNAH tertinggal menyala meski
 * terjadi exception saat membangun dasbor (kegagalan ditangkap dan dasbor diganti tampilan error
 * lewat {@link #buildErrorHtml(String, Exception)}).</p>
 *
 * <p>Kompatibel Java 1.6/1.7, ZKoss 5 (tanpa lambda/generics eksplisit pada beberapa API lama).</p>
 */
public final class GenericActionDashboardHelper {

    /** Kelas utilitas statis; tidak dimaksudkan untuk diinstansiasi. */
    private GenericActionDashboardHelper() {
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Membangun dan menampilkan dasbor "penuh" berbasis satu class entity Hibernate: menghitung
     * total keseluruhan, jumlah hari ini, dan jumlah 7 hari terakhir (query {@code COUNT} terpisah
     * lewat {@link #count(Class, Date)}, difilter kolom {@code tanggal_dirubah}), lalu merender
     * KPI card, bar chart perbandingan periode, donut komposisi, radar performa, dan bar chart
     * vertikal distribusi (lihat {@link #buildDashboardHtml(String, String, long, long, long)}).
     * Tidak melakukan apa pun bila {@code dashboardHtml} atau {@code clazz} bernilai {@code null}.
     * Kegagalan (mis. query gagal) ditangkap: pesan error ditampilkan ke admin dan dasbor diganti
     * tampilan error, TANPA melempar exception ke pemanggil.
     *
     * @param dashboardHtml komponen {@link Html} tujuan konten dasbor (wajib, tidak boleh {@code null})
     * @param progressHtml  komponen {@link Html} untuk progress bar; {@code null} memakai
     *                      {@code Clients.showBusy}/{@code clearBusy} sebagai gantinya
     * @param clazz         class entity Hibernate yang dihitung (mis. {@code Mahasiswa.class})
     * @param title         judul dasbor (ditampilkan pada hero section)
     * @param description   deskripsi singkat di bawah judul
     */
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

    /**
     * Dasbor "ringkas" berbasis filter/kriteria aktif pada Action pemanggil — total mengikuti
     * pencarian yang sedang dipakai (bukan seluruh data). Method ini SELALU menghitung ulang total
     * lewat refleksi (lihat {@link #refreshFromCriteria(Html, Html, Object, String, String, long)}
     * untuk varian yang menerima total siap pakai dan menghindari hitungan ganda).
     *
     * @param dashboardHtml komponen {@link Html} tujuan konten dasbor
     * @param progressHtml  komponen {@link Html} untuk progress bar; boleh {@code null}
     * @param action        instance Action pemanggil, dipakai lewat refleksi untuk menemukan
     *                      {@code initCriteria}/{@code grid} — lihat {@link #countFromCriteria}
     * @param title         judul dasbor
     * @param description   deskripsi singkat di bawah judul
     */
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

    /**
     * Menampilkan indikator progres pemuatan dasbor: bila {@code progressHtml} tidak {@code null},
     * merender bar progres HTML ({@link CommonDashboardHtmlHelper#progressBar}) dan membuat
     * komponen tersebut terlihat; bila {@code null}, memakai {@code Clients.showBusy(...)} sebagai
     * gantinya (mode tanpa komponen progres khusus). {@code percent} dijepit ke rentang 0-100.
     * Kegagalan (mis. komponen sudah detached) ditelan diam-diam agar tidak mengganggu alur
     * pembangunan dasbor.
     *
     * @param progressHtml komponen {@link Html} tujuan progress bar; boleh {@code null}
     * @param percent      persentase progres (dijepit ke 0-100)
     * @param title        judul singkat progres (mis. "Memuat dasbor")
     * @param detail       detail tambahan progres (mis. "Menghitung data...")
     */
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

    /**
     * Menyembunyikan indikator progres yang ditampilkan {@link #showProgress}: mengosongkan dan
     * menyembunyikan {@code progressHtml} bila tidak {@code null}, atau memanggil
     * {@code Clients.clearBusy()} bila {@code null}. Dipanggil dari blok {@code finally} pemanggil
     * agar SELALU dieksekusi meski pembangunan dasbor gagal. Kegagalan ditelan diam-diam.
     *
     * @param progressHtml komponen {@link Html} progress bar yang disembunyikan; boleh {@code null}
     */
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

    /**
     * Menyusun markup HTML lengkap dasbor "penuh" (dipakai {@link #refresh}): hero section, 4 KPI
     * card (Total/Hari Ini/7 Hari/Sebelumnya), bar chart horizontal perbandingan periode, donut
     * komposisi data 7-hari-terakhir-vs-total, radar performa 5 sumbu (Jumlah/Hari Ini/Minggu/
     * Histori/Pembaruan — nilainya dihitung heuristik dari {@code total}/{@code today}/
     * {@code sevenDays}/{@code older} lewat skala log/linear, BUKAN metrik terukur formal), bar
     * chart vertikal distribusi, dan footer petunjuk. {@code older} dihitung sebagai
     * {@code total - sevenDays} (dijepit ke 0 bila negatif).
     *
     * @param title       judul dasbor (hero section)
     * @param description deskripsi singkat di bawah judul
     * @param total       total keseluruhan data
     * @param today       jumlah data yang masuk sejak awal hari ini
     * @param sevenDays   jumlah data yang masuk dalam 7 hari terakhir
     * @return markup HTML lengkap siap ditempatkan pada komponen {@link Html}
     */
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

    /**
     * Menyusun markup HTML lengkap dasbor "ringkas" berbasis kriteria/filter (dipakai
     * {@link #refreshFromCriteria}), yang HANYA memiliki total hasil filter (tanpa breakdown hari
     * ini/7 hari): hero section, 4 KPI card generik (Total Hasil/Filter Aktif/Status Sistem/Aksi
     * Tersedia — tiga terakhir berupa label statis, bukan metrik terukur), panel "Cara Pakai"
     * (4 langkah kerja statis) + donut ukuran dataset (skala log dari {@code total}), panel
     * "Daftar Periksa" (checklist statis) + radar kesiapan data (nilai heuristik statis/berbasis
     * {@code total > 0}), dan footer petunjuk.
     *
     * @param title       judul dasbor
     * @param description deskripsi singkat di bawah judul
     * @param total       total hasil sesuai filter/kriteria aktif saat ini
     * @return markup HTML lengkap siap ditempatkan pada komponen {@link Html}
     */
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
     * Menyusun (dan mengembalikan langsung, TIDAK menampilkan sendiri ke komponen — beda dari
     * {@link #refresh}/{@link #refreshFromCriteria}, pemanggil yang menaruh hasilnya ke
     * {@link Html#setContent}) markup HTML dasbor khusus log pembayaran H2H (host-to-host):
     * hero section, 4 KPI card (Total Transaksi/Hari Ini/Berhasil/Nominal Hari Ini), bar chart
     * horizontal transaksi per bank (maks 6 bank pertama pada {@code bankNames}/{@code bankCounts})
     * berdampingan donut tingkat keberhasilan, serta radar kondisi sistem (5 sumbu: Volume,
     * Kelancaran dari {@code successRate}, Aktivitas, Monitoring, Validasi) berdampingan bar chart
     * tren transaksi per periode (hari ini/7 hari/sebelumnya).
     *
     * @param title          judul dasbor
     * @param description    deskripsi singkat di bawah judul
     * @param total          total keseluruhan transaksi H2H
     * @param today          jumlah transaksi yang masuk sejak awal hari ini
     * @param sevenDays      jumlah transaksi dalam 7 hari terakhir
     * @param bankNames      nama bank, dipasangkan indeks-demi-indeks dengan {@code bankCounts},
     *                       urut terbesar lebih dulu (hanya 6 elemen pertama yang dirender); boleh
     *                       {@code null}/kosong (menampilkan pesan "belum ada data")
     * @param bankCounts     jumlah transaksi per bank, dipasangkan indeks dengan {@code bankNames}
     * @param successCount   jumlah transaksi yang dianggap berhasil (kode respons diawali {@code "00"})
     * @param failCount      jumlah transaksi yang gagal/perlu ditinjau
     * @param nominalHariIni total nominal (rupiah) transaksi yang berhasil masuk hari ini, dirender
     *                       ringkas via {@link #formatNominalH2H(double)}
     * @return markup HTML lengkap dasbor H2H
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

    /**
     * Memformat nominal rupiah secara ringkas untuk KPI card H2H: {@code "Rp 0"} bila
     * {@code <= 0}, disingkat "M" (miliar, 1 desimal) bila &gt;= 1 miliar, disingkat "jt" (juta,
     * 1 desimal) bila &gt;= 1 juta, atau format ribuan biasa ({@link Common#numberFormat}) untuk
     * nilai lebih kecil. Kegagalan format (mis. locale bermasalah) fallback ke {@code "Rp "} +
     * nilai bulat tanpa pemisah ribuan.
     *
     * @param nominal nominal rupiah yang diformat
     * @return teks nominal ringkas siap tampil
     */
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

    /**
     * Membangun SVG grafik radar/spider-web (viewBox 200x200) dengan {@code n} sumbu — {@code n}
     * adalah {@code min(values.length, labels.length)}; mengembalikan string kosong bila
     * {@code n < 3} (radar butuh minimal 3 sumbu untuk bermakna secara visual). Setiap sumbu
     * digambar dari pusat ({@link #axisAngle}), dengan grid referensi 25%/50%/75%/100%
     * ({@link #radarPolygon}), lalu polygon data (nilai {@code values[i]} 0-100, dijepit ke
     * rentang itu) diisi warna {@code color} transparan ({@link #hexToRgba}) dengan garis tepi
     * solid, titik bundar di tiap sudut, dan label teks di ujung tiap sumbu (posisi anchor
     * disesuaikan sudut agar teks tidak terpotong tepi SVG).
     *
     * @param values nilai tiap sumbu (0-100; nilai di luar rentang dijepit)
     * @param labels label tiap sumbu, dipasangkan indeks dengan {@code values}
     * @param color  warna utama garis/isi polygon data (format heksadesimal {@code #rrggbb})
     * @return markup SVG radar, atau string kosong bila jumlah sumbu efektif kurang dari 3
     */
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

    /**
     * Membangun satu elemen {@code <polygon>} SVG beraturan berisi {@code n} sudut yang berjarak
     * {@code maxR * scale} dari pusat ({@code cx},{@code cy}) — dipakai {@link #buildRadarSvg}
     * untuk menggambar grid referensi (100%/75%/50%/25%) di belakang polygon data.
     *
     * @param n      jumlah sudut/sumbu
     * @param cx     koordinat X pusat
     * @param cy     koordinat Y pusat
     * @param maxR   radius maksimum (skala 100%)
     * @param scale  fraksi radius yang dipakai polygon ini (mis. 0.5 untuk grid 50%)
     * @param fill   warna isi (CSS color, boleh {@code "none"})
     * @param stroke warna garis tepi
     * @param sw     lebar garis tepi (CSS stroke-width, sebagai string)
     * @return markup elemen {@code <polygon>}
     */
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

    /**
     * Menghitung sudut (radian) sumbu ke-{@code i} dari {@code n} sumbu radar, terdistribusi rata
     * mengelilingi 360 derajat dimulai dari arah jam 12 (atas, {@code -PI/2}) searah jarum jam.
     *
     * @param i indeks sumbu (0-based)
     * @param n jumlah total sumbu
     * @return sudut dalam radian, siap dipakai {@code Math.cos}/{@code Math.sin}
     */
    private static double axisAngle(int i, int n) {
        return -Math.PI / 2.0 + i * 2.0 * Math.PI / n;
    }

    // =========================================================================
    // SVG: DONUT / RING CHART
    // =========================================================================

    /**
     * Membangun ring/donut chart SVG (radius 38, viewBox 100x100) berikut label persentase dan
     * teks di tengahnya (dilapis lewat {@code <div>} posisi absolut, bukan {@code <text>} SVG,
     * agar mudah diberi gaya tebal/besar). Lingkaran latar penuh digambar {@code bgColor}, lalu
     * lingkaran depan digambar {@code fillColor} sepanjang keliling proporsional {@code percent}
     * (memakai {@code stroke-dasharray}, diputar -90 derajat agar mulai dari jam 12).
     *
     * @param percent   persentase terisi (dijepit ke 0-100)
     * @param label     label singkat di bawah angka persentase (mis. "Terbaru")
     * @param fillColor warna segmen terisi
     * @param bgColor   warna latar/sisa lingkaran
     * @return markup HTML ({@code <div>} pembungkus + {@code <svg>}) donut chart
     */
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

    /**
     * Membangun bar chart vertikal murni CSS ({@code <div>} dengan tinggi proporsional, tanpa SVG)
     * setinggi 88px: tiap batang diberi label nilai di atas dan label kategori di bawah, tinggi
     * batang proporsional terhadap nilai maksimum pada {@code values} (batang dengan nilai &gt; 0
     * dijamin minimal setinggi 4px agar tetap terlihat).
     *
     * @param values nilai tiap batang
     * @param labels label kategori tiap batang, dipasangkan indeks dengan {@code values} (indeks
     *               di luar jangkauan memakai string kosong)
     * @param colors warna gradasi tiap batang, dipasangkan indeks dengan {@code values} (indeks
     *               di luar jangkauan memakai {@code #2563eb})
     * @return markup HTML bar chart vertikal
     */
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

    /** Tag pembuka {@code <div>} pembungkus terluar seluruh konten dasbor (font, warna latar, padding). */
    private static String outerOpen() {
        return "<div style='font-family:system-ui,-apple-system,Arial,Helvetica,sans-serif;"
                + "color:#0f172a;background:#f8fafc;padding:12px;box-sizing:border-box;"
                + "width:100%;line-height:1.5;'>";
    }

    /**
     * Tag pembuka {@code <div>} grid responsif ({@code display:grid}, kolom otomatis menyesuaikan
     * lebar kontainer) — dipakai membungkus baris KPI card maupun baris dua-panel (chart
     * berdampingan).
     *
     * @param minColWidth lebar minimum tiap kolom (CSS length, mis. {@code "260px"}) sebelum
     *                    kolom berikutnya turun ke baris baru
     * @return tag pembuka {@code <div>} grid
     */
    private static String gridOpen(String minColWidth) {
        return "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax("
                + minColWidth + ",1fr));gap:10px;margin-top:10px;'>";
    }

    /** Tag pembuka {@code <div>} "kartu" panel putih standar (border, radius, shadow halus) yang membungkus satu chart/panel dasbor. */
    private static String cardOpen() {
        return "<div style='background:#fff;border:1px solid #e5e7eb;border-radius:16px;"
                + "padding:14px;box-shadow:0 2px 12px rgba(15,23,42,.06);box-sizing:border-box;'>";
    }

    /** Merender judul panel (bold, gelap) di dalam sebuah {@link #cardOpen()}; {@code t} di-escape lewat {@link #escapeHtml(String)}. */
    private static String cardTitle(String t) {
        return "<div style='font-size:13px;font-weight:900;color:#0f172a;'>"
                + escapeHtml(t) + "</div>";
    }

    /** Merender subjudul/keterangan kecil (abu-abu) di bawah {@link #cardTitle(String)}; {@code s} di-escape lewat {@link #escapeHtml(String)}. */
    private static String cardSubtitle(String s) {
        return "<div style='font-size:10px;color:#64748b;margin-top:2px;margin-bottom:10px;'>"
                + escapeHtml(s) + "</div>";
    }

    /**
     * Merender kotak footer bergaris putus-putus di bagian bawah dasbor, berisi petunjuk singkat
     * untuk pengguna (mis. arahan membuka tab Data).
     *
     * @param innerHtml markup HTML mentah (TIDAK di-escape oleh method ini) yang ditampilkan di
     *                  dalam kotak footer — pemanggil bertanggung jawab meng-escape bagian yang
     *                  berasal dari data pengguna
     * @return markup HTML kotak footer
     */
    private static String footer(String innerHtml) {
        return "<div style='margin-top:10px;padding:11px 13px;border-radius:13px;"
                + "background:#fff;border:1px dashed #cbd5e1;color:#475569;font-size:10px;'>"
                + innerHtml + "</div>";
    }

    /**
     * Menambahkan (append ke {@code h}) hero section: badge kecil di atas, judul besar, dan
     * deskripsi, dengan latar gradien tiga warna ({@code cA} ke {@code cB} ke {@code cC}). Seluruh
     * teks di-escape lewat {@link #escapeHtml(String)}.
     *
     * @param h           {@link StringBuilder} akumulator markup dasbor
     * @param title       judul dasbor (font besar)
     * @param description deskripsi singkat di bawah judul
     * @param badge       label kecil huruf kapital di atas judul (mis. "Dasbor Ringkasan")
     * @param cA          warna gradien awal (kiri-atas)
     * @param cB          warna gradien tengah
     * @param cC          warna gradien akhir (kanan-bawah)
     */
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

    /**
     * Menambahkan (append ke {@code h}) satu KPI card: badge label berwarna, angka/nilai besar,
     * panah/indikator tren opsional, dan teks bantuan kecil di bawahnya.
     *
     * @param h          {@link StringBuilder} akumulator markup dasbor
     * @param label      label singkat kartu (ditampilkan sebagai badge kapital kecil)
     * @param value      nilai utama yang ditonjolkan (font besar)
     * @param trend      indikator tren opsional (mis. "▲"/"▼"/"—"); kosong/{@code null} berarti
     *                   tidak ditampilkan
     * @param trendColor warna teks {@code trend}
     * @param help       teks bantuan/keterangan singkat di bawah nilai
     * @param bg         warna latar badge label
     * @param color      warna teks badge label
     */
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

    /**
     * Menambahkan (append ke {@code h}) satu baris bar chart horizontal: label dan nilai numerik
     * di baris atas, batang gradien biru-cyan dengan lebar {@code width} di bawahnya.
     *
     * @param h     {@link StringBuilder} akumulator markup dasbor
     * @param label label kategori (mis. nama bank atau nama periode)
     * @param width lebar batang sebagai persentase CSS (mis. hasil {@link #pct(long, long)});
     *              kosong/{@code null} diperlakukan sebagai {@code "0%"}
     * @param value nilai numerik yang ditampilkan (diformat lewat {@link #format(long)})
     */
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

    /**
     * Menambahkan (append ke {@code h}) satu badge kecil berbentuk pil (dipakai mis. untuk label
     * "Nama Sumbu Radar Nilai%" di bawah grafik radar).
     *
     * @param h     {@link StringBuilder} akumulator markup dasbor
     * @param text  teks badge (di-escape lewat {@link #escapeHtml(String)})
     * @param bg    warna latar badge
     * @param color warna teks badge
     */
    private static void appendBadge(StringBuilder h, String text, String bg, String color) {
        h.append("<span style='font-size:9px;font-weight:800;color:").append(color)
         .append(";background:").append(bg)
         .append(";border-radius:999px;padding:3px 7px;'>")
         .append(escapeHtml(text)).append("</span>");
    }

    /**
     * Menambahkan (append ke {@code h}) satu baris legenda: titik bundar berwarna + label —
     * dipakai di bawah donut chart untuk menjelaskan arti tiap warna segmen.
     *
     * @param h     {@link StringBuilder} akumulator markup dasbor
     * @param color warna titik bundar
     * @param label teks label (di-escape lewat {@link #escapeHtml(String)})
     */
    private static void appendLegendDot(StringBuilder h, String color, String label) {
        h.append("<div style='display:flex;align-items:center;gap:5px;font-size:10px;color:#475569;'>");
        h.append("<div style='width:10px;height:10px;border-radius:999px;flex-shrink:0;background:")
         .append(color).append(";'></div>");
        h.append("<span>").append(escapeHtml(label)).append("</span>");
        h.append("</div>");
    }

    /**
     * Menambahkan (append ke {@code h}) satu langkah pada panel "Cara Pakai": lingkaran nomor
     * bergradien di kiri, judul dan deskripsi langkah di kanan.
     *
     * @param h     {@link StringBuilder} akumulator markup dasbor
     * @param num   nomor urut langkah (ditampilkan apa adanya di dalam lingkaran, TIDAK di-escape —
     *              pemanggil selalu mengisi literal angka statis)
     * @param title judul singkat langkah (di-escape lewat {@link #escapeHtml(String)})
     * @param desc  deskripsi langkah (di-escape lewat {@link #escapeHtml(String)})
     */
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

    /**
     * Menambahkan (append ke {@code h}) satu kartu kecil pada panel "Daftar Periksa": label tebal
     * di atas, nilai/keterangan berwarna di bawahnya.
     *
     * @param h     {@link StringBuilder} akumulator markup dasbor
     * @param label label item pemeriksaan (di-escape lewat {@link #escapeHtml(String)})
     * @param value keterangan/nilai item (di-escape lewat {@link #escapeHtml(String)})
     * @param bg    warna latar kartu
     * @param color warna teks {@code value}
     */
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

    /**
     * Mengonversi warna heksadesimal {@code #rrggbb} menjadi string CSS {@code rgba(r,g,b,alpha)}
     * — dipakai {@link #buildRadarSvg} untuk isi polygon data yang transparan. Kegagalan parse
     * (format hex tidak valid) fallback ke {@code rgba(37,99,235,0.15)} (biru transparan default).
     *
     * @param hex   warna sumber, format {@code #rrggbb} (tanda pagar opsional)
     * @param alpha opasitas 0.0-1.0
     * @return string CSS {@code rgba(...)}
     */
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

    /**
     * Menggelapkan warna heksadesimal {@code #rrggbb} dengan mengurangi tiap komponen R/G/B
     * sebesar 45 (dijepit ke minimal 0) — dipakai {@link #buildVBarChart} untuk warna gradien
     * bawah tiap batang. Kegagalan parse mengembalikan {@code hex} apa adanya.
     *
     * @param hex warna sumber, format {@code #rrggbb}
     * @return warna hasil penggelapan, format {@code #rrggbb}, atau {@code hex} asli bila gagal parse
     */
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

    /**
     * Menghitung {@code value} sebagai persentase dari {@code max}, dibulatkan dan dijepit ke
     * rentang 2-100 (nilai positif kecil dipaksa minimal 2% agar batang tetap terlihat pada bar
     * chart CSS). {@code max <= 0} selalu mengembalikan {@code "0%"} (menghindari pembagian nol).
     *
     * @param value nilai pembilang
     * @param max   nilai penyebut (basis 100%)
     * @return persentase sebagai string CSS (mis. {@code "42%"}), siap dipakai pada {@code width}
     */
    private static String pct(long value, long max) {
        if (max <= 0) { return "0%"; }
        int p = (int) Math.round(value * 100.0 / max);
        if (p < 2 && value > 0) { p = 2; }
        if (p > 100) { p = 100; }
        return p + "%";
    }

    /**
     * Memformat angka dengan pemisah ribuan sesuai locale aplikasi ({@link Common#numberFormat}).
     * Kegagalan format fallback ke {@code String.valueOf(value)} (tanpa pemisah ribuan).
     *
     * @param value nilai yang diformat
     * @return teks angka terformat
     */
    private static String format(long value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * Meng-escape karakter HTML dasar ({@code &}, {@code <}, {@code >}, {@code "}, {@code '})
     * pada {@code value} agar aman disisipkan sebagai teks di dalam markup dasbor (mencegah data
     * yang mengandung tag/atribut HTML tak sengaja dirender sebagai markup).
     *
     * @param value string sumber (boleh {@code null})
     * @return string ter-escape, atau string kosong bila {@code value} bernilai {@code null}
     */
    private static String escapeHtml(String value) {
        if (value == null) { return ""; }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * Menyusun markup HTML tampilan error pengganti dasbor, didelegasikan sepenuhnya ke
     * {@link CommonDashboardHtmlHelper#errorState(String, Exception)}. Dipakai
     * {@link #refresh}/{@link #refreshFromCriteria} pada blok {@code catch} agar pengguna tetap
     * melihat tampilan yang informatif (bukan area kosong) saat pembangunan dasbor gagal.
     *
     * @param title judul dasbor yang gagal dibangun
     * @param e     exception yang menyebabkan kegagalan
     * @return markup HTML tampilan error
     */
    private static String buildErrorHtml(String title, Exception e) {
        return CommonDashboardHtmlHelper.errorState(title, e);
    }

    // =========================================================================
    // STRATEGI HITUNG
    // =========================================================================

    /**
     * Menghitung jumlah baris entity {@code clazz} lewat query {@code SELECT count(*)} Hibernate,
     * dengan Session TERSENDIRI (dibuka lewat {@code getSessionFactory().openSession()}, bukan
     * {@code currentSession()}, dan ditutup dengan aman di {@code finally} via
     * {@link #closeSessionQuietly(Session)}) agar tidak mengganggu Session request yang sedang
     * berjalan. Bila {@code since} tidak {@code null}, ditambahkan filter
     * {@code Restrictions.ge("tanggal_dirubah", since)} — bila {@code clazz} tidak memiliki
     * properti tersebut, query dianggap gagal dan mengembalikan 0 (bukan melempar exception).
     * Kegagalan lain (mis. Session tidak dapat dibuka) juga ditangkap dan dilaporkan ke admin,
     * mengembalikan 0.
     *
     * @param clazz class entity Hibernate yang dihitung
     * @param since ambang bawah kolom {@code tanggal_dirubah} (inklusif); {@code null} berarti
     *              tanpa filter tanggal (hitung seluruh baris)
     * @return jumlah baris yang cocok, atau 0 bila query gagal/properti tanggal tidak ada
     */
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

    /**
     * Strategi hitung PERTAMA (dicoba lebih dulu oleh
     * {@link #refreshFromCriteria(Html, Html, Object, String, String, long)}): mencari method
     * {@code initCriteria(boolean)} pada {@code action} (atau superclass-nya) lewat refleksi,
     * memanggilnya dengan {@code false} (konvensi "tanpa order/paging" pada pola Action AIS), lalu
     * bila hasilnya sebuah {@link Criteria}, menambahkan proyeksi {@code rowCount()} dan
     * mengeksekusinya. Method target diakses via {@code setAccessible(true)} karena umumnya
     * {@code protected}/private pada Action.
     *
     * @param action instance Action pemanggil; {@code null} langsung mengembalikan -1
     * @return jumlah baris hasil kriteria, atau -1 bila {@code action} {@code null}, method
     *         {@code initCriteria(boolean)} tidak ditemukan, atau hasilnya bukan {@link Criteria}
     *         (menandakan pemanggil harus mencoba strategi berikutnya)
     * @throws Exception diteruskan apa adanya dari refleksi/eksekusi query (ditangkap oleh pemanggil)
     */
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

    /**
     * Strategi hitung KEDUA (dicoba bila {@link #countFromCriteria(Object)} gagal/tidak
     * ditemukan): mencari method {@code initCriteria(Session, boolean)} pada {@code action} lewat
     * refleksi — beberapa Action mengekspos varian yang menerima Session eksplisit alih-alih
     * memakai {@code currentSession()} internal. Session dibuka tersendiri dan ditutup aman via
     * {@link #closeSessionQuietly(Session)}.
     *
     * @param action instance Action pemanggil; {@code null} langsung mengembalikan -1
     * @return jumlah baris hasil kriteria, atau -1 bila method tidak ditemukan/hasil bukan
     *         {@link Criteria} (menandakan pemanggil harus mencoba strategi berikutnya)
     * @throws Exception diteruskan apa adanya dari refleksi/eksekusi query
     */
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

    /**
     * Strategi hitung TERAKHIR (fallback bila kedua strategi berbasis {@code initCriteria} gagal):
     * mencari field {@code grid} pada {@code action} (atau superclass-nya) lewat refleksi, membaca
     * nilainya, lalu memanggil method {@code getModel()} pada objek grid tersebut — bila hasilnya
     * sebuah {@link ListModel}, ukurannya ({@code getSize()}) dipakai sebagai total. Cocok untuk
     * Action yang datanya sudah dimuat ke grid tanpa method {@code initCriteria} yang dapat
     * dipanggil ulang secara aman.
     *
     * @param action instance Action pemanggil; {@code null} langsung mengembalikan -1
     * @return ukuran model grid, atau -1 bila field {@code grid} tidak ada/{@code null}, method
     *         {@code getModel()} tidak ada, atau hasilnya bukan {@link ListModel}
     * @throws Exception diteruskan apa adanya dari refleksi
     */
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

    /**
     * Mencari method {@code name} dengan tanda tangan {@code paramTypes} pada {@code clazz},
     * menelusuri rantai superclass ke atas ({@code getDeclaredMethod} hanya melihat class itu
     * sendiri, bukan warisan) hingga ditemukan atau rantai class habis ({@code null}).
     *
     * @param clazz      class awal pencarian
     * @param name       nama method yang dicari
     * @param paramTypes tanda tangan parameter method yang dicari
     * @return {@link Method} yang ditemukan (belum di-{@code setAccessible}), atau {@code null}
     *         bila tidak ditemukan pada {@code clazz} maupun seluruh superclass-nya
     */
    private static Method findMethod(Class clazz, String name, Class[] paramTypes) {
        Class current = clazz;
        while (current != null) {
            try { return current.getDeclaredMethod(name, paramTypes); }
            catch (Exception e) { current = current.getSuperclass(); }
        }
        return null;
    }

    /**
     * Mencari field {@code name} pada {@code clazz}, menelusuri rantai superclass ke atas (mirip
     * {@link #findMethod(Class, String, Class[])} tapi untuk field).
     *
     * @param clazz class awal pencarian
     * @param name  nama field yang dicari
     * @return {@link Field} yang ditemukan (belum di-{@code setAccessible}), atau {@code null}
     *         bila tidak ditemukan pada {@code clazz} maupun seluruh superclass-nya
     */
    private static Field findField(Class clazz, String name) {
        Class current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (Exception e) { current = current.getSuperclass(); }
        }
        return null;
    }

    /**
     * Mengonversi hasil query (biasanya dari {@code Projections.rowCount()}, bertipe {@link Long}
     * atau {@link Number} lain) menjadi {@code long} primitif.
     *
     * @param value hasil query (boleh bertipe {@link Number} apa pun, atau tipe lain/{@code null})
     * @return nilai sebagai {@code long}, atau 0 bila {@code value} bukan {@link Number}
     */
    private static long toLong(Object value) {
        if (value instanceof Long)   { return ((Long) value).longValue(); }
        if (value instanceof Number) { return ((Number) value).longValue(); }
        return 0L;
    }

    /**
     * Menghitung titik waktu 00:00:00.000 pada hari ini + {@code addDays} hari — dipakai sebagai
     * ambang bawah filter {@code tanggal_dirubah} pada {@link #count(Class, Date)} (mis.
     * {@code addDays=0} untuk "hari ini", {@code addDays=-6} untuk "7 hari terakhir termasuk
     * hari ini").
     *
     * @param addDays jumlah hari yang ditambahkan/dikurangkan dari hari ini (0 = hari ini)
     * @return awal hari (00:00:00.000) pada tanggal target, memakai timezone default JVM
     */
    private static Date startOfDay(int addDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, addDays);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Menutup {@code session} dengan aman (clear lalu disconnect lalu close, masing-masing
     * dibungkus try/catch tersendiri) untuk mencegah kebocoran koneksi/memory setelah
     * {@link #count(Class, Date)} atau {@link #countFromCriteriaWithSession(Object)} selesai.
     * Tidak melakukan apa pun bila {@code session} {@code null}; seluruh kegagalan (mis. session
     * sudah tertutup) ditelan diam-diam karena tujuannya murni pembersihan best-effort.
     *
     * @param session Session Hibernate yang akan ditutup; boleh {@code null}
     */
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
