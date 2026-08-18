package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASBOARD_KELULUSAN_SISWA_2026_05_30
 * Dashboard khusus kelulusan siswa berbasis template DasborAkademikSekolah.
 * Fokus data utama: Siswa dengan statusKeluar != null.
 * Indikator: jumlah lulusan, status keluar, tahun/tanggal lulus, kelengkapan ijazah,
 * no seri transkrip, no peserta ujian nasional, NISN, kontak, email, sekolah, jurusan,
 * gender, dan tren kelulusan.
 * Kompatibel Java 1.6/1.7 dan ZKoss 5.5: tanpa lambda, tanpa diamond operator.
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.StatusKeluarSiswa;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DasboardKelulusanSiswa extends MyPortallayout {

    private static final long serialVersionUID = 20260530231801L;
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> _CACHE
            = new java.util.concurrent.ConcurrentHashMap<String, Object>();
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> _EXPIRY
            = new java.util.concurrent.ConcurrentHashMap<String, Long>();
    private static final long _TTL_MS = 5L * 60 * 1000;
    private static final int TOP_LIMIT = 10;
    private static final int DETAIL_LIMIT = 100;

    private static boolean debug = true;

    private Tbmuser tbmuser;
    private Integer desktopHeight = 11000;

    private Div body;
    private Vbox loadingDashboardContainer;
    private int dashboardLoadVersion;

    private Intbox mulaiTahun;
    private Intbox sampaiTahun;
    private Combobox searchSekolah;
    private Combobox searchStatusKeluar;
    private Combobox searchJenisKelamin;

    private int currentMulai;
    private int currentSampai;
    private Sekolah currentSekolah;
    private StatusKeluarSiswa currentStatusKeluar;
    private String currentJenisKelamin;

    public DasboardKelulusanSiswa() throws Exception {
        super();
        setWidth("100%");
        setMaximizedMode("whole");
        tbmuser = Common.getCurrentUser();
        init();
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debugValue) {
        debug = debugValue;
    }

    private void init() throws Exception {
        DashboardGridExportHelper.pasang(this, "Kelulusan Siswa");
        if (tbmuser != null && tbmuser.getUserId() != null) {
            Integer h = MainAction.desktopHeights.get(tbmuser.getUserId());
            if (h != null) {
                desktopHeight = h;
            }
        }

        setStyle("background:#f8fafc; min-height:" + desktopHeight + "px; padding:0;");

        MyPortalchildren portalchildren = new MyPortalchildren();
        portalchildren.setParent(this);
        portalchildren.setWidth("100%");
        portalchildren.setStyle("padding:5px; margin-bottom:12px;");

        Panel panel = new MyPanelConfig();
        panel.setParent(portalchildren);
        panel.setTitle("Dasboard Kelulusan Siswa");
        panel.setBorder("none");
        panel.setCollapsible(false);
        panel.setClosable(false);
        panel.setMaximizable(false);
        panel.setMinimizable(false);
        panel.setStyle("margin-bottom:14px; border:1px solid #e6edf5; border-radius:16px; "
                + "background:#ffffff; box-shadow:0 12px 30px rgba(15,23,42,0.08); overflow:hidden;");

        Panelchildren panelchildren = new Panelchildren();
        panelchildren.setParent(panel);
        panelchildren.setStyle("padding:0; background:#f8fafc;");

        body = new Div();
        body.setParent(panelchildren);
        body.setWidth("100%");
        body.setStyle("box-sizing:border-box; padding:14px; background:#f8fafc;");

        initDefaultFilterValue();
        renderContent();
    }

    private void initDefaultFilterValue() {
        int tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);
        try {
            Number maxTahunLulus = (Number) HibernateUtil.currentSession().createCriteria(Siswa.class)
                    .add(Restrictions.isNotNull("statusKeluar"))
                    .setProjection(Projections.max("tahunLulus")).uniqueResult();
            if (maxTahunLulus != null && maxTahunLulus.intValue() > 1900 && maxTahunLulus.intValue() < 2900) {
                tahunSekarang = maxTahunLulus.intValue();
            }
        } catch (Exception e) {
            debugError("initDefaultFilterValue", e);
        }

        currentSampai = tahunSekarang;
        currentMulai = tahunSekarang - 2;
        currentJenisKelamin = null;
        currentStatusKeluar = null;
    }

    private void renderContent() throws Exception {
        Common.clear(body);

        currentMulai = mulaiTahun == null || mulaiTahun.getValue() == null ? currentMulai : mulaiTahun.getValue();
        currentSampai = sampaiTahun == null || sampaiTahun.getValue() == null ? currentSampai : sampaiTahun.getValue();
        if (currentMulai > currentSampai) {
            int temp = currentMulai;
            currentMulai = currentSampai;
            currentSampai = temp;
        }
        currentSekolah = getSelectedSekolah();
        currentStatusKeluar = getSelectedStatusKeluar();
        currentJenisKelamin = getSelectedJenisKelamin();

        renderHero();
        renderFilter();

        final int loadVersion = ++dashboardLoadVersion;
        tampilkanLoadingDashboardKelulusan("Menyiapkan parameter kelulusan dan status keluar siswa...", 2);

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (loadVersion != dashboardLoadVersion) {
                    return;
                }
                renderDashboardContentDenganLoading(loadVersion);
            }
        });
    }

    private void renderDashboardContentDenganLoading(int loadVersion) throws Exception {
        try {
            updateDashboardProgress("Mengambil data siswa dengan status keluar tidak kosong...", 5);
            DashboardKelulusanData data = loadDashboardDataCached();

            if (loadVersion != dashboardLoadVersion) {
                return;
            }

            updateDashboardProgress("Menyusun kartu, tabel top data, tren, dan rekomendasi kelulusan...", 96);
            hapusLoadingDashboardKelulusan();

            renderOverview(data);
            renderDokumenKelulusan(data);
            renderTopTablesAndTrends(data);
            renderRecommendation(data);
        } catch (Exception e) {
            debugError("renderDashboardContentDenganLoading", e);
            hapusLoadingDashboardKelulusan();
            appendHtml(body, "<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; "
                    + "color:#991b1b; border:1px solid #fecdd3; font-weight:700;'>Dasboard Kelulusan Siswa belum dapat dimuat. "
                    + "Silakan tekan Refresh atau aktifkan debug untuk melihat detail error.</div>");
        }
    }

    private String buildCacheKey() {
        return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
                + "|" + currentMulai + "|" + currentSampai
                + "|" + (currentSekolah == null ? "" : currentSekolah.getId())
                + "|" + (currentStatusKeluar == null ? "" : currentStatusKeluar.getId())
                + "|" + (currentJenisKelamin == null ? "" : currentJenisKelamin);
    }

    @SuppressWarnings("unchecked")
    private DashboardKelulusanData loadDashboardDataCached() throws Exception {
        String _k = buildCacheKey();
        Long _e = _EXPIRY.get(_k);
        if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
            return (DashboardKelulusanData) _CACHE.get(_k);
        }
        DashboardKelulusanData data = loadDashboardData();
        _CACHE.put(_k, data);
        _EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
        return data;
    }

    private DashboardKelulusanData loadDashboardData() throws Exception {
        DashboardKelulusanData data = new DashboardKelulusanData();
        data.tahunMulai = currentMulai;
        data.tahunSampai = currentSampai;

        updateDashboardProgress("Menghitung jumlah lulusan, gender, status keluar, dan sekolah...", 12);

        data.totalLulusan = countSiswa(null);
        data.totalLakiLaki = countSiswa("LAKI");
        data.totalPerempuan = countSiswa("PEREMPUAN");
        data.totalSekolah = countDistinctProperty("sekolah");
        data.totalStatusKeluar = countDistinctProperty("statusKeluar");
        data.totalPenjurusan = countDistinctProperty("penjurusanSekolah");

        updateDashboardProgress("Menghitung kelengkapan tahun lulus, tanggal lulus, dan dokumen ijazah...", 32);

        data.totalTahunLulusTerisi = countNotNull("tahunLulus");
        data.totalTanggalLulusTerisi = countNotNull("tanggalLulus");
        data.totalNoSeriIjazah = countStringTerisi("noSeriIjazah");
        data.totalNoSeriTranskrip = countStringTerisi("noSeriTranskrip");
        data.totalNoPesertaUjian = countStringTerisi("noPesertaUjianNasional");
        data.totalSkhun = countStringTerisi("skhun");
        data.totalNisn = countStringTerisi("nomorIndukNasional");

        updateDashboardProgress("Menghitung kelengkapan kontak alumni dan data orang tua...", 50);

        data.totalEmail = countStringTerisi("alamatEmail");
        data.totalHpSiswa = countStringTerisi("hp");
        data.totalTeleponSiswa = countStringTerisi("teleponSiswa");
        data.totalTeleponOrtu = countStringTerisi("teleponOrangTua");
        data.totalNamaAyah = countStringTerisi("namaAyah");
        data.totalNamaIbu = countStringTerisi("namaIbu");

        data.rasioTahunLulus = percent(data.totalTahunLulusTerisi, data.totalLulusan);
        data.rasioTanggalLulus = percent(data.totalTanggalLulusTerisi, data.totalLulusan);
        data.rasioIjazah = percent(data.totalNoSeriIjazah, data.totalLulusan);
        data.rasioTranskrip = percent(data.totalNoSeriTranskrip, data.totalLulusan);
        data.rasioNoUjian = percent(data.totalNoPesertaUjian, data.totalLulusan);
        data.rasioNisn = percent(data.totalNisn, data.totalLulusan);
        data.rasioEmail = percent(data.totalEmail, data.totalLulusan);
        data.rasioHp = percent(data.totalHpSiswa + data.totalTeleponSiswa, data.totalLulusan);

        updateDashboardProgress("Menyusun tabel status keluar, sekolah, tahun lulus, jurusan, dan gender...", 70);

        data.topStatusKeluar = loadTopSiswaGroup("statusKeluar");
        data.topSekolah = loadTopSiswaGroup("sekolah");
        data.topTahunLulus = loadTopSiswaGroup("tahunLulus");
        data.topPenjurusan = loadTopSiswaGroup("penjurusanSekolah");
        data.topJenisKelamin = loadTopSiswaGroup("jenisKelamin");
        data.topStatusSiswa = loadTopSiswaGroup("statusSiswa");

        updateDashboardProgress("Menyusun tren kelulusan dan validasi kelengkapan data setiap tahun...", 84);

        List<Integer> years = buildYearList();
        for (int i = 0; i < years.size(); i++) {
            Integer tahun = years.get(i);
            data.trendKelulusan.add(new DashboardMiniRow(String.valueOf(tahun), countBySingleYear(tahun.intValue(), null)));
            data.trendIjazah.add(new DashboardMiniRow(String.valueOf(tahun), countBySingleYear(tahun.intValue(), "noSeriIjazah")));
            data.trendTranskrip.add(new DashboardMiniRow(String.valueOf(tahun), countBySingleYear(tahun.intValue(), "noSeriTranskrip")));
            data.trendNoUjian.add(new DashboardMiniRow(String.valueOf(tahun), countBySingleYear(tahun.intValue(), "noPesertaUjianNasional")));
        }

        sortAndLimit(data.topStatusKeluar, TOP_LIMIT);
        sortAndLimit(data.topSekolah, TOP_LIMIT);
        sortAndLimit(data.topTahunLulus, TOP_LIMIT);
        sortAndLimit(data.topPenjurusan, TOP_LIMIT);
        sortAndLimit(data.topJenisKelamin, TOP_LIMIT);
        sortAndLimit(data.topStatusSiswa, TOP_LIMIT);

        return data;
    }

    private void renderHero() {
        String user = tbmuser == null ? "Pengguna" : safeText(tbmuser.getUserNama());
        String periode = currentMulai + " s.d. " + currentSampai;
        String sekolah = currentSekolah == null ? "Semua Sekolah" : labelSekolah(currentSekolah);
        String statusKeluar = currentStatusKeluar == null ? "Semua Status Keluar" : labelObject(currentStatusKeluar);
        String gender = currentJenisKelamin == null ? "Semua Gender" : currentJenisKelamin;

        appendHtml(body, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
                + "color:#ffffff; box-shadow:0 18px 38px rgba(29,78,216,0.22);'>"
                + "<div style='position:absolute; width:240px; height:240px; right:-70px; top:-90px; border-radius:999px; background:rgba(255,255,255,0.13);'></div>"
                + "<div style='position:absolute; width:160px; height:160px; right:120px; bottom:-92px; border-radius:999px; background:rgba(255,255,255,0.10);'></div>"
                + "<div style='position:relative; z-index:2;'>"
                + "<div style='font-size:12px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:700;'>Monitoring Alumni, Status Keluar, Dokumen Ijazah & Data Kontak</div>"
                + "<div style='font-size:28px; line-height:1.18; font-weight:800; margin-top:7px;'>Dasboard Kelulusan Siswa</div>"
                + "<div style='font-size:13px; max-width:930px; opacity:.93; margin-top:8px;'>Ringkasan khusus untuk memantau siswa yang sudah memiliki status keluar. Data utama diambil dari model Siswa dengan statusKeluar tidak kosong, lalu dikembangkan menjadi indikator tahun lulus, tanggal lulus, status kelulusan/keluar, kelengkapan nomor ijazah, transkrip, nomor peserta ujian nasional, NISN, kontak, dan sebaran sekolah/jurusan.</div>"
                + "<div style='margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;'>"
                + badgeHtml("Periode Lulus: " + periode, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(sekolah, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(statusKeluar, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(gender, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml("User: " + user, "rgba(255,255,255,.16)", "#ffffff")
                + "</div></div></div>");
    }

    private void renderFilter() throws Exception {
        Div filterContainer = new Div();
        filterContainer.setParent(body);
        filterContainer.setStyle("margin-top:14px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
                + "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(filterContainer);
        toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

        new MyLabelAgakKecil("Tahun Lulus:").setParent(toolbar);
        mulaiTahun = new Intbox(currentMulai);
        mulaiTahun.setCols(4);
        mulaiTahun.setParent(toolbar);

        new Label(ais.common.Common.getBahasaConfig("s.d.")).setParent(toolbar);
        sampaiTahun = new Intbox(currentSampai);
        sampaiTahun.setCols(4);
        sampaiTahun.setParent(toolbar);

        new MyLabelAgakKecil("Sekolah:").setParent(toolbar);
        searchSekolah = new Combobox();
        searchSekolah.setCols(24);
        searchSekolah.setReadonly(true);
        searchSekolah.setParent(toolbar);
        populateSekolahCombo(searchSekolah);
        selectComboByValue(searchSekolah, currentSekolah);

        new MyLabelAgakKecil("Status Keluar:").setParent(toolbar);
        searchStatusKeluar = new Combobox();
        searchStatusKeluar.setCols(22);
        searchStatusKeluar.setReadonly(true);
        searchStatusKeluar.setParent(toolbar);
        populateStatusKeluarCombo(searchStatusKeluar);
        selectComboByValue(searchStatusKeluar, currentStatusKeluar);

        new MyLabelAgakKecil("Gender:").setParent(toolbar);
        searchJenisKelamin = new Combobox();
        searchJenisKelamin.setCols(14);
        searchJenisKelamin.setReadonly(true);
        searchJenisKelamin.setParent(toolbar);
        populateJenisKelaminCombo(searchJenisKelamin);
        selectComboByValue(searchJenisKelamin, currentJenisKelamin);

        EventListener reloadListener = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                renderContent();
            }
        };
        mulaiTahun.addEventListener("onChange", reloadListener);
        sampaiTahun.addEventListener("onChange", reloadListener);
        searchSekolah.addEventListener("onChange", reloadListener);
        searchStatusKeluar.addEventListener("onChange", reloadListener);
        searchJenisKelamin.addEventListener("onChange", reloadListener);

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
        refresh.setTooltiptext("Refresh dasboard kelulusan siswa");
        refresh.setParent(toolbar);
        refresh.addEventListener("onClick", reloadListener);
    }

    private void renderOverview(DashboardKelulusanData data) {
        appendHtml(body, sectionIntroHtml("Overview Kelulusan Siswa",
                "Semua angka di bagian ini dihitung dari data Siswa yang sudah memiliki status keluar. Klik kartu untuk melihat contoh detail data."));

        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(grid, "Total Lulusan", data.totalLulusan, "Siswa dengan status keluar", "#1d4ed8", "fa-graduation-cap", "LULUSAN");
        appendMetricCard(grid, "Laki-laki", data.totalLakiLaki, "Lulusan laki-laki", "#0369a1", "fa-male", "LAKI");
        appendMetricCard(grid, "Perempuan", data.totalPerempuan, "Lulusan perempuan", "#be185d", "fa-female", "PEREMPUAN");
        appendMetricCard(grid, "Status Keluar", data.totalStatusKeluar, "Jenis status keluar terpakai", "#0f766e", "fa-tags", "LULUSAN");
        appendMetricCard(grid, "Sekolah", data.totalSekolah, "Sekolah asal lulusan", "#7c3aed", "fa-building", "LULUSAN");
        appendMetricCard(grid, "Jurusan/Peminatan", data.totalPenjurusan, "Penjurusan sekolah terisi", "#0891b2", "fa-sitemap", "LULUSAN");
        appendMetricCard(grid, "Tahun Lulus Terisi", data.totalTahunLulusTerisi, data.rasioTahunLulus + "% dari lulusan", "#16a34a", "fa-calendar", "TAHUN_LULUS");
        appendMetricCard(grid, "Tanggal Lulus Terisi", data.totalTanggalLulusTerisi, data.rasioTanggalLulus + "% dari lulusan", "#f59e0b", "fa-calendar-check-o", "TANGGAL_LULUS");
    }

    private void renderDokumenKelulusan(DashboardKelulusanData data) {
        appendHtml(body, sectionIntroHtml("Kelengkapan Dokumen & Kontak Alumni",
                "Membantu operator mengecek kesiapan data ijazah, transkrip, nomor ujian, NISN, email, dan nomor kontak alumni/orang tua."));

        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(grid, "No Seri Ijazah", data.totalNoSeriIjazah, data.rasioIjazah + "% dari lulusan", "#0f766e", "fa-certificate", "IJAZAH");
        appendMetricCard(grid, "No Seri Transkrip", data.totalNoSeriTranskrip, data.rasioTranskrip + "% dari lulusan", "#2563eb", "fa-file-text-o", "TRANSKRIP");
        appendMetricCard(grid, "No Peserta Ujian", data.totalNoPesertaUjian, data.rasioNoUjian + "% dari lulusan", "#7c3aed", "fa-id-card-o", "NO_UJIAN");
        appendMetricCard(grid, "SKHUN", data.totalSkhun, "Data SKHUN terisi", "#ea580c", "fa-file", "SKHUN");
        appendMetricCard(grid, "NISN/NIK Nasional", data.totalNisn, data.rasioNisn + "% dari lulusan", "#0891b2", "fa-barcode", "NISN");
        appendMetricCard(grid, "Email Alumni", data.totalEmail, data.rasioEmail + "% dari lulusan", "#0d9488", "fa-envelope", "EMAIL");
        appendMetricCard(grid, "Kontak Alumni", data.totalHpSiswa + data.totalTeleponSiswa, data.rasioHp + "% data HP/telepon", "#4f46e5", "fa-phone", "KONTAK");
        appendMetricCard(grid, "Belum Ada No Ijazah", data.totalLulusan - data.totalNoSeriIjazah, "Prioritas validasi dokumen", "#dc2626", "fa-warning", "BELUM_IJAZAH");

        Div panel = new Div();
        panel.setParent(body);
        panel.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:12px; margin-top:12px;");

        appendHtml(panel, progressCardHtml("Kesiapan Dokumen Kelulusan",
                "Mengukur berapa banyak data lulusan yang sudah memiliki dokumen utama.",
                data.totalLulusan,
                new DashboardMiniRow("Tahun Lulus", data.totalTahunLulusTerisi),
                new DashboardMiniRow("Tanggal Lulus", data.totalTanggalLulusTerisi),
                new DashboardMiniRow("No Seri Ijazah", data.totalNoSeriIjazah),
                new DashboardMiniRow("No Seri Transkrip", data.totalNoSeriTranskrip),
                new DashboardMiniRow("No Peserta Ujian", data.totalNoPesertaUjian)));

        appendHtml(panel, progressCardHtml("Kesiapan Kontak Alumni",
                "Membantu sekolah melakukan pelacakan alumni, legalisasi, dan komunikasi lanjutan.",
                data.totalLulusan,
                new DashboardMiniRow("Email", data.totalEmail),
                new DashboardMiniRow("HP Siswa", data.totalHpSiswa),
                new DashboardMiniRow("Telepon Siswa", data.totalTeleponSiswa),
                new DashboardMiniRow("Telepon Orang Tua", data.totalTeleponOrtu),
                new DashboardMiniRow("Nama Ayah/Ibu", Math.min(data.totalNamaAyah, data.totalNamaIbu))));
    }

    private void renderTopTablesAndTrends(DashboardKelulusanData data) {
        appendHtml(body, sectionIntroHtml("Sebaran & Tren Kelulusan",
                "Tabel berikut menampilkan konsentrasi lulusan berdasarkan status keluar, sekolah, tahun lulus, jurusan, gender, dan tren kelengkapan dokumen."));

        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:12px; margin-top:12px;");

        appendMiniTable(grid, "Top Status Keluar", "Status keluar yang paling banyak digunakan", data.topStatusKeluar, "Status", "Siswa");
        appendMiniTable(grid, "Top Sekolah", "Sebaran lulusan per sekolah", data.topSekolah, "Sekolah", "Siswa");
        appendMiniTable(grid, "Top Tahun Lulus", "Jumlah lulusan per tahun", data.topTahunLulus, "Tahun", "Siswa");
        appendMiniTable(grid, "Top Jurusan/Peminatan", "Sebaran lulusan berdasarkan penjurusan", data.topPenjurusan, "Jurusan", "Siswa");
        appendMiniTable(grid, "Gender Lulusan", "Sebaran gender lulusan", data.topJenisKelamin, "Gender", "Siswa");
        appendMiniTable(grid, "Status Siswa", "Status siswa pada data alumni", data.topStatusSiswa, "Status", "Siswa");

        Div trend = new Div();
        trend.setParent(body);
        trend.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:12px; margin-top:12px;");

        appendMiniTable(trend, "Tren Kelulusan", "Jumlah siswa keluar/lulus per tahun", data.trendKelulusan, "Tahun", "Siswa");
        appendMiniTable(trend, "Tren No Ijazah", "Kelengkapan nomor seri ijazah per tahun", data.trendIjazah, "Tahun", "Siswa");
        appendMiniTable(trend, "Tren No Transkrip", "Kelengkapan nomor seri transkrip per tahun", data.trendTranskrip, "Tahun", "Siswa");
        appendMiniTable(trend, "Tren No Ujian", "Kelengkapan nomor peserta ujian per tahun", data.trendNoUjian, "Tahun", "Siswa");
    }

    private void renderRecommendation(DashboardKelulusanData data) {
        appendHtml(body, sectionIntroHtml("Rekomendasi Operasional Kelulusan",
                "Saran otomatis untuk membantu validasi data kelulusan dan arsip alumni."));

        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:12px; margin-top:12px;");

        appendHtml(grid, recommendationCardHtml("Validasi Status Keluar", data.totalLulusan > 0 ? "BAIK" : "CEK",
                data.totalLulusan > 0 ? "Data lulusan sudah dapat dipantau dari field status keluar siswa." : "Belum ditemukan siswa dengan status keluar. Pastikan data status keluar sudah diisi saat proses kelulusan.", "#1d4ed8"));

        appendHtml(grid, recommendationCardHtml("Lengkapi Tahun/Tanggal Lulus", data.rasioTahunLulus >= 90 && data.rasioTanggalLulus >= 80 ? "BAIK" : "PRIORITAS",
                "Tahun lulus terisi " + data.rasioTahunLulus + "% dan tanggal lulus terisi " + data.rasioTanggalLulus + "%. Data ini penting untuk rekap lulusan, pelaporan, dan legalisasi ijazah.", "#0f766e"));

        appendHtml(grid, recommendationCardHtml("Nomor Ijazah & Transkrip", data.rasioIjazah >= 90 && data.rasioTranskrip >= 90 ? "BAIK" : "PERLU VALIDASI",
                "No seri ijazah terisi " + data.rasioIjazah + "% dan no seri transkrip terisi " + data.rasioTranskrip + "%. Prioritaskan siswa yang sudah lulus namun belum memiliki nomor dokumen.", "#dc2626"));

        appendHtml(grid, recommendationCardHtml("Kontak Alumni", data.rasioEmail >= 70 || data.rasioHp >= 70 ? "CUKUP" : "TINGKATKAN",
                "Email terisi " + data.rasioEmail + "% dan kontak siswa terisi sekitar " + data.rasioHp + "%. Kontak alumni membantu penelusuran alumni, tracer study sekolah, dan kebutuhan legalisasi.", "#7c3aed"));
    }

    private void tampilkanLoadingDashboardKelulusan(String pesan, int persen) {
        if (body == null) {
            return;
        }
        hapusLoadingDashboardKelulusan();

        loadingDashboardContainer = new Vbox();
        loadingDashboardContainer.setWidth("100%");
        loadingDashboardContainer.setParent(body);
        loadingDashboardContainer.setStyle("margin-top:14px;");

        Html htmlLoading = new Html(buildLoadingDashboardHtml(pesan, persen));
        loadingDashboardContainer.appendChild(htmlLoading);
    }

    private void updateDashboardProgress(String pesan, int persen) {
        if (loadingDashboardContainer == null) {
            return;
        }
        try {
            Common.clear(loadingDashboardContainer);
            Html htmlLoading = new Html(buildLoadingDashboardHtml(pesan, persen));
            loadingDashboardContainer.appendChild(htmlLoading);
            loadingDashboardContainer.invalidate();
        } catch (Exception e) {
            debugError("updateDashboardProgress", e);
        }
    }

    private void hapusLoadingDashboardKelulusan() {
        if (loadingDashboardContainer != null) {
            try {
                if (loadingDashboardContainer.getParent() != null) {
                    loadingDashboardContainer.detach();
                }
            } catch (Exception e) {
                debugError("hapusLoadingDashboardKelulusan", e);
            }
        }
        loadingDashboardContainer = null;
    }

    private String buildLoadingDashboardHtml(String pesan, int persen) {
        int progress = persen;
        if (progress < 0) {
            progress = 0;
        }
        if (progress > 100) {
            progress = 100;
        }

        return "<div style='padding:15px; text-align:center; color:#475569; background:#ffffff; "
                + "border:1px solid #e2e8f0; border-radius:16px; box-shadow:0 10px 24px rgba(15,23,42,0.05);'>"
                + "<div style='font-size:14px; font-weight:800; color:#0f172a; margin-bottom:6px;'>"
                + "<i class=\"fa fa-spinner fa-spin\"></i> Memproses Dasboard Kelulusan Siswa</div>"
                + "<div style='font-size:12px; margin-bottom:10px; color:#64748b;'>" + safeHtml(pesan) + "</div>"
                + "<div style='width:100%; height:12px; border-radius:999px; overflow:hidden; background:#e2e8f0;'>"
                + "<div style='height:12px; width:" + progress + "%; border-radius:999px; "
                + "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div>"
                + "</div>"
                + "<div style='font-size:12px; font-weight:800; color:#1d4ed8; margin-top:7px;'>"
                + progress + "% selesai</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:4px;'>"
                + "Mohon tunggu. Sistem sedang mengambil data alumni, status keluar, dokumen kelulusan, dan kontak secara bertahap.</div>"
                + "</div>";
    }

    private Criteria createBaseSiswaCriteria() {
        Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
        c.add(Restrictions.isNotNull("statusKeluar"));
        applyYearCriterion(c);
        if (currentSekolah != null) {
            c.add(Restrictions.eq("sekolah", currentSekolah));
        }
        if (currentStatusKeluar != null) {
            c.add(Restrictions.eq("statusKeluar", currentStatusKeluar));
        }
        if (currentJenisKelamin != null && currentJenisKelamin.trim().length() > 0) {
            if (currentJenisKelamin.toLowerCase().contains("laki")) {
                c.add(Restrictions.ilike("jenisKelamin", "%laki%"));
            } else if (currentJenisKelamin.toLowerCase().contains("puan")) {
                c.add(Restrictions.ilike("jenisKelamin", "%puan%"));
            } else {
                c.add(Restrictions.eq("jenisKelamin", currentJenisKelamin));
            }
        }
        return c;
    }

    private void applyYearCriterion(Criteria c) {
        try {
            Disjunction d = Restrictions.disjunction();
            d.add(Restrictions.between("tahunLulus", Integer.valueOf(currentMulai), Integer.valueOf(currentSampai)));
            d.add(Restrictions.between("tanggalLulus", startOfYear(currentMulai), endOfYear(currentSampai)));
            c.add(d);
        } catch (Exception e) {
            debugError("applyYearCriterion", e);
        }
    }

    private Date startOfYear(int tahun) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, tahun);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date endOfYear(int tahun) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, tahun);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private long countSiswa(String mode) {
        try {
            Criteria c = createBaseSiswaCriteria();
            if ("LAKI".equals(mode)) {
                c.add(Restrictions.ilike("jenisKelamin", "%laki%"));
            } else if ("PEREMPUAN".equals(mode)) {
                c.add(Restrictions.ilike("jenisKelamin", "%puan%"));
            }
            c.setProjection(Projections.rowCount());
            return toLong(c.uniqueResult());
        } catch (Exception e) {
            debugError("countSiswa " + mode, e);
            return 0;
        }
    }

    private long countNotNull(String property) {
        try {
            Criteria c = createBaseSiswaCriteria();
            c.add(Restrictions.isNotNull(property));
            c.setProjection(Projections.rowCount());
            return toLong(c.uniqueResult());
        } catch (Exception e) {
            debugError("countNotNull " + property, e);
            return 0;
        }
    }

    private long countStringTerisi(String property) {
        try {
            Criteria c = createBaseSiswaCriteria();
            addStringTerisi(c, property);
            c.setProjection(Projections.rowCount());
            return toLong(c.uniqueResult());
        } catch (Exception e) {
            debugError("countStringTerisi " + property, e);
            return 0;
        }
    }

    private long countDistinctProperty(String property) {
        try {
            Criteria c = createBaseSiswaCriteria();
            c.add(Restrictions.isNotNull(property));
            c.setProjection(Projections.countDistinct(property));
            return toLong(c.uniqueResult());
        } catch (Exception e) {
            debugError("countDistinctProperty " + property, e);
            return 0;
        }
    }

    private long countBySingleYear(int tahun, String stringProperty) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
            c.add(Restrictions.isNotNull("statusKeluar"));
            Disjunction d = Restrictions.disjunction();
            d.add(Restrictions.eq("tahunLulus", Integer.valueOf(tahun)));
            d.add(Restrictions.between("tanggalLulus", startOfYear(tahun), endOfYear(tahun)));
            c.add(d);
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            if (currentStatusKeluar != null) {
                c.add(Restrictions.eq("statusKeluar", currentStatusKeluar));
            }
            if (currentJenisKelamin != null && currentJenisKelamin.trim().length() > 0) {
                c.add(Restrictions.ilike("jenisKelamin", currentJenisKelamin.toLowerCase().contains("laki") ? "%laki%" : "%puan%"));
            }
            if (stringProperty != null) {
                addStringTerisi(c, stringProperty);
            }
            c.setProjection(Projections.rowCount());
            return toLong(c.uniqueResult());
        } catch (Exception e) {
            debugError("countBySingleYear " + tahun + " " + stringProperty, e);
            return 0;
        }
    }

    private void addStringTerisi(Criteria c, String property) {
        c.add(Restrictions.isNotNull(property));
        c.add(Restrictions.ne(property, ""));
    }

    private List<DashboardMiniRow> loadTopSiswaGroup(String property) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = createBaseSiswaCriteria();
            c.add(Restrictions.isNotNull(property));
            ProjectionList pl = Projections.projectionList();
            pl.add(Projections.groupProperty(property));
            pl.add(Projections.rowCount());
            c.setProjection(pl);
            List list = c.list();
            for (int i = 0; i < list.size(); i++) {
                Object[] row = (Object[]) list.get(i);
                rows.add(new DashboardMiniRow(labelObject(row[0]), toLong(row[1])));
            }
        } catch (Exception e) {
            debugError("loadTopSiswaGroup " + property, e);
        }
        return rows;
    }

    private List<Integer> buildYearList() {
        List<Integer> years = new ArrayList<Integer>();
        for (int year = currentMulai; year <= currentSampai; year++) {
            years.add(Integer.valueOf(year));
        }
        return years;
    }

    private void openDetail(final String detailKey, String title) throws Exception {
        Window window = new Window();
        window.setTitle(title);
        window.setWidth("96%");
        window.setHeight("82%");
        window.setClosable(true);
        window.setSizable(true);
        window.setMaximizable(true);
        window.setPosition("center");
        window.setStyle("border-radius:14px; overflow:hidden;");

        Vbox box = new Vbox();
        box.setParent(window);
        box.setWidth("100%");
        box.setHeight("100%");
        box.setStyle("padding:10px; box-sizing:border-box; background:#f8fafc; overflow:auto;");

        appendHtml(box, "<div style='padding:10px 12px; margin-bottom:10px; border-radius:12px; "
                + "background:#ffffff; border:1px solid #e2e8f0; color:#475569; font-size:12px;'>"
                + "Menampilkan maksimal " + DETAIL_LIMIT + " data pertama sesuai filter dasboard. Untuk export lengkap, gunakan modul laporan kelulusan/alumni.</div>");

        Grid grid = new Grid();
        grid.setParent(box);
        grid.setWidth("100%");
        grid.setStyle("background:#ffffff; border:1px solid #e6edf5; border-radius:12px; overflow:hidden;");
        grid.setMold("paging");
        grid.setPageSize(10);

        Columns cols = new Columns();
        cols.setParent(grid);
        addColumn(cols, "No", "45px");
        addColumn(cols, "NIS/NISN", "145px");
        addColumn(cols, "Nama Siswa", "220px");
        addColumn(cols, "Sekolah", "190px");
        addColumn(cols, "Status Keluar", "170px");
        addColumn(cols, "Tahun", "75px");
        addColumn(cols, "Tanggal Lulus", "120px");
        addColumn(cols, "Gender", "105px");
        addColumn(cols, "No Ijazah", "145px");
        addColumn(cols, "No Transkrip", "145px");
        addColumn(cols, "Kontak", "150px");

        Rows rows = new Rows();
        rows.setParent(grid);

        List list = loadDetailSiswa(detailKey);
        if (list == null || list.isEmpty()) {
            Row row = new Row();
            row.setParent(rows);
            Label label = new Label(ais.common.Common.getBahasaConfig("Tidak ada data detail yang cocok dengan filter saat ini."));
            label.setParent(row);
        } else {
            for (int i = 0; i < list.size(); i++) {
                Siswa siswa = (Siswa) list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.setStyle(i % 2 == 0 ? "background:#ffffff;" : "background:#f8fafc;");
                new Label(String.valueOf(i + 1)).setParent(row);
                new Label(safeText(siswa.getNomorInduk()) + " / " + safeText(siswa.getNomorIndukNasional())).setParent(row);
                new Label(safeText(siswa.getNamaSiswa())).setParent(row);
                new Label(labelObject(siswa.getSekolah())).setParent(row);
                new Label(labelObject(siswa.getStatusKeluar())).setParent(row);
                new Label(siswa.getTahunLulus() == null ? "" : String.valueOf(siswa.getTahunLulus())).setParent(row);
                new Label(formatDate(siswa.getTanggalLulus())).setParent(row);
                new Label(safeText(siswa.getJenisKelamin())).setParent(row);
                new Label(safeText(siswa.getNoSeriIjazah())).setParent(row);
                new Label(safeText(siswa.getNoSeriTranskrip())).setParent(row);
                new Label(safeText(siswa.getTeleponSiswa()) + " " + safeText(siswa.getHp())).setParent(row);
            }
        }

        pasangPopupDetail(window);
        window.doModal();
    }



    /**
     * ZK MyPortallayout hanya boleh memiliki anak MyPortalchildren.
     * Karena class ringkasan ini extends MyPortallayout, Window popup tidak boleh dipasang dengan window.setParent(this).
     * Popup ditempel ke Page aktif atau ke body Div agar kompatibel dengan ZKoss 5.5.
     */
    private void pasangPopupDetail(Window window) {
        if (window == null) {
            return;
        }
        try {
            if (body != null && body.getPage() != null) {
                window.setPage(body.getPage());
                return;
            }
        } catch (Exception e) {
            debugError("pasangPopupDetail.bodyPage", e);
        }
        try {
            if (getPage() != null) {
                window.setPage(getPage());
                return;
            }
        } catch (Exception e) {
            debugError("pasangPopupDetail.currentPage", e);
        }
        try {
            if (body != null) {
                window.setParent(body);
                return;
            }
        } catch (Exception e) {
            debugError("pasangPopupDetail.bodyParent", e);
        }
        try {
            Component parent = getParent();
            while (parent instanceof MyPortallayout) {
                parent = parent.getParent();
            }
            if (parent != null) {
                window.setParent(parent);
            }
        } catch (Exception e) {
            debugError("pasangPopupDetail.fallbackParent", e);
        }
    }

    private List loadDetailSiswa(String detailKey) {
        try {
            Criteria c = createBaseSiswaCriteria();
            if ("LAKI".equals(detailKey)) {
                c.add(Restrictions.ilike("jenisKelamin", "%laki%"));
            } else if ("PEREMPUAN".equals(detailKey)) {
                c.add(Restrictions.ilike("jenisKelamin", "%puan%"));
            } else if ("TAHUN_LULUS".equals(detailKey)) {
                c.add(Restrictions.isNotNull("tahunLulus"));
            } else if ("TANGGAL_LULUS".equals(detailKey)) {
                c.add(Restrictions.isNotNull("tanggalLulus"));
            } else if ("IJAZAH".equals(detailKey)) {
                addStringTerisi(c, "noSeriIjazah");
            } else if ("TRANSKRIP".equals(detailKey)) {
                addStringTerisi(c, "noSeriTranskrip");
            } else if ("NO_UJIAN".equals(detailKey)) {
                addStringTerisi(c, "noPesertaUjianNasional");
            } else if ("SKHUN".equals(detailKey)) {
                addStringTerisi(c, "skhun");
            } else if ("NISN".equals(detailKey)) {
                addStringTerisi(c, "nomorIndukNasional");
            } else if ("EMAIL".equals(detailKey)) {
                addStringTerisi(c, "alamatEmail");
            } else if ("KONTAK".equals(detailKey)) {
                Disjunction d = Restrictions.disjunction();
                d.add(Restrictions.isNotNull("hp"));
                d.add(Restrictions.isNotNull("teleponSiswa"));
                d.add(Restrictions.isNotNull("teleponOrangTua"));
                c.add(d);
            } else if ("BELUM_IJAZAH".equals(detailKey)) {
                Disjunction d = Restrictions.disjunction();
                d.add(Restrictions.isNull("noSeriIjazah"));
                d.add(Restrictions.eq("noSeriIjazah", ""));
                c.add(d);
            }
            c.addOrder(Order.desc("tahunLulus"));
            c.addOrder(Order.desc("tanggalLulus"));
            c.addOrder(Order.asc("namaSiswa"));
            c.setMaxResults(DETAIL_LIMIT);
            return c.list();
        } catch (Exception e) {
            debugError("loadDetailSiswa " + detailKey, e);
            return new ArrayList();
        }
    }

    private void clearCombo(Combobox combo) {
        if (combo == null) {
            return;
        }
        try {
            combo.getChildren().clear();
        } catch (Exception e) {
            debugError("clearCombo", e);
        }
    }

    private void populateSekolahCombo(Combobox combo) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua Sekolah");
        semua.setValue(null);
        semua.setParent(combo);
        try {
            List list = HibernateUtil.currentSession().createCriteria(Sekolah.class).list();
            Collections.sort(list, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    return labelSekolah((Sekolah) o1).compareToIgnoreCase(labelSekolah((Sekolah) o2));
                }
            });
            for (int i = 0; i < list.size(); i++) {
                Sekolah sekolah = (Sekolah) list.get(i);
                Comboitem item = new Comboitem(labelSekolah(sekolah));
                item.setValue(sekolah);
                item.setParent(combo);
            }
        } catch (Exception e) {
            debugError("populateSekolahCombo", e);
        }
        if (combo.getSelectedItem() == null && combo.getItems().size() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private void populateStatusKeluarCombo(Combobox combo) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua Status Keluar");
        semua.setValue(null);
        semua.setParent(combo);
        try {
            List list = HibernateUtil.currentSession().createCriteria(StatusKeluarSiswa.class).list();
            Collections.sort(list, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    return labelObject(o1).compareToIgnoreCase(labelObject(o2));
                }
            });
            for (int i = 0; i < list.size(); i++) {
                Object status = list.get(i);
                Comboitem item = new Comboitem(labelObject(status));
                item.setValue(status);
                item.setParent(combo);
            }
        } catch (Exception e) {
            debugError("populateStatusKeluarCombo", e);
        }
        if (combo.getSelectedItem() == null && combo.getItems().size() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private void populateJenisKelaminCombo(Combobox combo) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua Gender");
        semua.setValue(null);
        semua.setParent(combo);

        Comboitem laki = new Comboitem("Laki-laki");
        laki.setValue("Laki-laki");
        laki.setParent(combo);

        Comboitem perempuan = new Comboitem("Perempuan");
        perempuan.setValue("Perempuan");
        perempuan.setParent(combo);

        if (combo.getSelectedItem() == null && combo.getItems().size() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private Sekolah getSelectedSekolah() {
        try {
            if (searchSekolah != null && searchSekolah.getSelectedItem() != null) {
                return (Sekolah) searchSekolah.getSelectedItem().getValue();
            }
        } catch (Exception e) {
            debugError("getSelectedSekolah", e);
        }
        return currentSekolah;
    }

    private StatusKeluarSiswa getSelectedStatusKeluar() {
        try {
            if (searchStatusKeluar != null && searchStatusKeluar.getSelectedItem() != null) {
                return (StatusKeluarSiswa) searchStatusKeluar.getSelectedItem().getValue();
            }
        } catch (Exception e) {
            debugError("getSelectedStatusKeluar", e);
        }
        return currentStatusKeluar;
    }

    private String getSelectedJenisKelamin() {
        try {
            if (searchJenisKelamin != null && searchJenisKelamin.getSelectedItem() != null) {
                Object value = searchJenisKelamin.getSelectedItem().getValue();
                return value == null ? null : String.valueOf(value);
            }
        } catch (Exception e) {
            debugError("getSelectedJenisKelamin", e);
        }
        return currentJenisKelamin;
    }

    private void selectComboByValue(Combobox combo, Object value) {
        if (combo == null) {
            return;
        }
        try {
            for (int i = 0; i < combo.getItems().size(); i++) {
                Comboitem item = (Comboitem) combo.getItems().get(i);
                Object itemValue = item.getValue();
                if (value == null && itemValue == null) {
                    combo.setSelectedItem(item);
                    return;
                }
                if (value != null && itemValue != null && value.equals(itemValue)) {
                    combo.setSelectedItem(item);
                    return;
                }
            }
            if (combo.getItems().size() > 0) {
                combo.setSelectedIndex(0);
            }
        } catch (Exception e) {
            debugError("selectComboByValue", e);
        }
    }

    private String labelSekolah(Sekolah sekolah) {
        if (sekolah == null) {
            return "";
        }
        return labelObject(sekolah);
    }

    private String labelObject(Object object) {
        if (object == null) {
            return "Tidak Diisi";
        }
        String[] methods = new String[] { "getNama", "getNamaSiswa", "getNamaSekolah", "getKode", "toString" };
        for (int i = 0; i < methods.length; i++) {
            try {
                Method m = object.getClass().getMethod(methods[i], new Class[0]);
                Object result = m.invoke(object, new Object[0]);
                if (result != null && String.valueOf(result).trim().length() > 0) {
                    return String.valueOf(result).trim();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardKelulusanSiswa.java:1077");
            }
        }
        return object.toString();
    }

    private String sectionIntroHtml(String title, String subtitle) {
        return "<div style='margin-top:16px; padding:13px 15px; border-radius:16px; background:#ffffff; "
                + "border:1px solid #e6edf5; box-shadow:0 10px 24px rgba(15,23,42,0.04);'>"
                + "<div style='font-size:16px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:#64748b; margin-top:4px; line-height:1.45;'>" + safeHtml(subtitle) + "</div></div>";
    }

    private String badgeHtml(String text, String bg, String color) {
        return "<span style='display:inline-flex; align-items:center; padding:7px 10px; border-radius:999px; "
                + "background:" + bg + "; color:" + color + "; font-size:11px; font-weight:800;'>" + safeHtml(text) + "</span>";
    }

    private void appendMetricCard(Component parent, String title, long value, String subtitle, String color, String icon, final String detailKey) {
        Div card = new Div();
        card.setParent(parent);
        card.setStyle("position:relative; overflow:hidden; padding:14px; border-radius:16px; background:#ffffff; "
                + "border:1px solid #e6edf5; box-shadow:0 10px 24px rgba(15,23,42,0.05); cursor:" + (detailKey == null ? "default" : "pointer") + ";");
        appendHtml(card, "<div style='display:flex; align-items:flex-start; justify-content:space-between; gap:10px;'>"
                + "<div><div style='font-size:12px; color:#64748b; font-weight:800;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:28px; color:" + color + "; font-weight:900; line-height:1.1; margin-top:6px;'>" + safeHtml(formatNumber(value)) + "</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:6px; line-height:1.35;'>" + safeHtml(subtitle) + "</div></div>"
                + "<div style='width:38px; height:38px; border-radius:13px; display:flex; align-items:center; justify-content:center; color:#fff; background:" + color + ";'>"
                + "<i class='fa " + safeHtml(icon) + "'></i></div></div>");
        if (detailKey != null) {
            card.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    openDetail(detailKey, "Detail - " + detailKey.replace('_', ' '));
                }
            });
        }
    }

    private void appendMiniTable(Component parent, String title, String subtitle, List<DashboardMiniRow> rows, String col1, String col2) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#ffffff; border:1px solid #e6edf5; border-radius:16px; box-shadow:0 10px 24px rgba(15,23,42,0.05); overflow:hidden;'>");
        sb.append("<div style='padding:13px 14px; border-bottom:1px solid #eef2f7;'>");
        sb.append("<div style='font-size:14px; font-weight:900; color:#0f172a;'>").append(safeHtml(title)).append("</div>");
        sb.append("<div style='font-size:11px; color:#64748b; margin-top:3px;'>").append(safeHtml(subtitle)).append("</div></div>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px;'>");
        sb.append("<thead><tr style='background:#f8fafc; color:#475569;'>");
        sb.append("<th style='text-align:left; padding:9px 12px;'>").append(safeHtml(col1)).append("</th>");
        sb.append("<th style='text-align:right; padding:9px 12px; width:95px;'>").append(safeHtml(col2)).append("</th></tr></thead><tbody>");
        if (rows == null || rows.isEmpty()) {
            sb.append("<tr><td colspan='2' style='padding:12px; color:#94a3b8;'>Belum ada data.</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                DashboardMiniRow row = rows.get(i);
                sb.append("<tr style='background:").append(i % 2 == 0 ? "#ffffff" : "#f8fafc").append(";'>");
                sb.append("<td style='padding:9px 12px; color:#0f172a;'>").append(safeHtml(row.label)).append("</td>");
                sb.append("<td style='padding:9px 12px; color:#1d4ed8; text-align:right; font-weight:900;'>").append(formatNumber(row.value)).append("</td></tr>");
            }
        }
        sb.append("</tbody></table></div>");
        appendHtml(parent, sb.toString());
    }

    private String progressCardHtml(String title, String subtitle, long total, DashboardMiniRow r1, DashboardMiniRow r2, DashboardMiniRow r3, DashboardMiniRow r4, DashboardMiniRow r5) {
        DashboardMiniRow[] rows = new DashboardMiniRow[] { r1, r2, r3, r4, r5 };
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#ffffff; border:1px solid #e6edf5; border-radius:16px; padding:14px; box-shadow:0 10px 24px rgba(15,23,42,0.05);'>");
        sb.append("<div style='font-size:14px; font-weight:900; color:#0f172a;'>").append(safeHtml(title)).append("</div>");
        sb.append("<div style='font-size:11px; color:#64748b; margin-top:3px; line-height:1.4;'>").append(safeHtml(subtitle)).append("</div>");
        for (int i = 0; i < rows.length; i++) {
            DashboardMiniRow row = rows[i];
            int p = percent(row.value, total);
            sb.append("<div style='margin-top:11px;'>");
            sb.append("<div style='display:flex; justify-content:space-between; font-size:11px; color:#475569; font-weight:800;'>");
            sb.append("<span>").append(safeHtml(row.label)).append("</span><span>").append(formatNumber(row.value)).append(" / ").append(formatNumber(total)).append(" (" + p + "%)</span></div>");
            sb.append("<div style='height:9px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:5px;'>");
            sb.append("<div style='height:9px; width:").append(p).append("%; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;'></div></div></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String recommendationCardHtml(String title, String status, String desc, String color) {
        return "<div style='padding:14px; border-radius:16px; background:#ffffff; border:1px solid #e6edf5; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.05);'>"
                + "<div style='display:flex; justify-content:space-between; gap:8px; align-items:center;'>"
                + "<div style='font-size:14px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<span style='font-size:10px; font-weight:900; color:#fff; background:" + color + "; border-radius:999px; padding:5px 8px;'>" + safeHtml(status) + "</span>"
                + "</div><div style='font-size:12px; color:#64748b; line-height:1.45; margin-top:8px;'>" + safeHtml(desc) + "</div></div>";
    }

    private void appendHtml(Component parent, String html) {
        Html h = new Html(html);
        h.setParent(parent);
    }

    private void addColumn(Columns cols, String label, String width) {
        Column col = new Column(label);
        col.setParent(cols);
        if (width != null) {
            col.setWidth(width);
        }
    }

    private void sortAndLimit(List<DashboardMiniRow> rows, int limit) {
        if (rows == null) {
            return;
        }
        Collections.sort(rows, new Comparator<DashboardMiniRow>() {
            @Override
            public int compare(DashboardMiniRow o1, DashboardMiniRow o2) {
                if (o1.value == o2.value) {
                    return safeText(o1.label).compareToIgnoreCase(safeText(o2.label));
                }
                return o1.value < o2.value ? 1 : -1;
            }
        });
        while (rows.size() > limit) {
            rows.remove(rows.size() - 1);
        }
    }

    private long toLong(Object object) {
        if (object == null) {
            return 0;
        }
        if (object instanceof Number) {
            return ((Number) object).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(object));
        } catch (Exception e) {
            return 0;
        }
    }

    private int percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((value * 100.0) / total);
    }

    private String formatNumber(long number) {
        try {
            return Common.numberFormat.get().format(number);
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        try {
            return Common.dateFormat3.get().format(date);
        } catch (Exception e) {
            return String.valueOf(date);
        }
    }

    private String safeText(Object text) {
        return text == null ? "" : String.valueOf(text).trim();
    }

    private String safeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void debugError(String context, Exception e) {
        if (debug) {
            System.err.println("[DasboardKelulusanSiswa DEBUG] " + context);
            if (e != null) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasboardKelulusanSiswa.java:1254");
            }
        }
    }

    private static class DashboardKelulusanData {
        int tahunMulai;
        int tahunSampai;

        long totalLulusan;
        long totalLakiLaki;
        long totalPerempuan;
        long totalSekolah;
        long totalStatusKeluar;
        long totalPenjurusan;

        long totalTahunLulusTerisi;
        long totalTanggalLulusTerisi;
        long totalNoSeriIjazah;
        long totalNoSeriTranskrip;
        long totalNoPesertaUjian;
        long totalSkhun;
        long totalNisn;

        long totalEmail;
        long totalHpSiswa;
        long totalTeleponSiswa;
        long totalTeleponOrtu;
        long totalNamaAyah;
        long totalNamaIbu;

        int rasioTahunLulus;
        int rasioTanggalLulus;
        int rasioIjazah;
        int rasioTranskrip;
        int rasioNoUjian;
        int rasioNisn;
        int rasioEmail;
        int rasioHp;

        List<DashboardMiniRow> topStatusKeluar = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topSekolah = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topTahunLulus = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPenjurusan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topJenisKelamin = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topStatusSiswa = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendKelulusan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendIjazah = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendTranskrip = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendNoUjian = new ArrayList<DashboardMiniRow>();
    }

    private static class DashboardMiniRow {
        String label;
        long value;

        DashboardMiniRow(String label, long value) {
            this.label = label == null || label.trim().length() == 0 ? "Tidak Diisi" : label;
            this.value = value;
        }
    }
}
