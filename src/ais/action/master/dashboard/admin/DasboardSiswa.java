package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASBOARD_SISWA_2026_05_30
 * Dashboard khusus kesiswaan berbasis template DasborAkademikSekolah.
 * Fokus data: profil siswa, kelas, asrama, organisasi, prestasi, apresiasi, pelanggaran,
 * dan kelengkapan kontak siswa/orang tua.
 * Kompatibel Java 1.6/1.7 dan ZKoss 5.5: tanpa lambda, tanpa diamond operator.
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Cell;
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
import ais.database.model.sekolah.ApresiasiSiswa;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.OrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswaPunyaSiswa;
import ais.database.model.sekolah.PelanggaranSiswa;
import ais.database.model.sekolah.PrestasiSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Komponen dashboard khusus untuk dasboard siswa. Kelas ini memilih variasi data atau tampilan
 * dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyPortallayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code java.util.concurrent.ConcurrentHashMap
 * _CACHE}, {@code java.util.concurrent.ConcurrentHashMap _EXPIRY}, {@code long _TTL_MS}, {@code int TOP_LIMIT},
 * {@code int DETAIL_LIMIT}, {@code boolean debug}, {@code Tbmuser tbmuser}, {@code Integer desktopHeight};
 * inisialisasi/lifecycle ({@code init()}, {@code initDefaultFilterValue()}, {@code appendMiniTable()});
 * pembacaan/pencarian ({@code renderDashboardContentDenganLoading()}, {@code tampilkanLoadingDashboardSiswa()},
 * {@code hapusLoadingDashboardSiswa()}, {@code buildLoadingDashboardHtml()}, {@code loadDashboardDataCached()},
 * {@code loadDashboardData()}); mutasi data ({@code setDebug()}, {@code updateDashboardProgress()});
 * pelaporan/ekspor ({@code renderContent()}, {@code renderHero()}, {@code renderFilter()}, {@code
 * renderOverview()}, {@code renderProfilDanKelengkapan()}, {@code renderAktivitasKesiswaan()}); operasi domain
 * lain ({@code isDebug()}, {@code buildCacheKey()}, {@code countGeneric()}, {@code countSiswa()}, {@code
 * countSiswaDenganKelas()}, {@code countEntitySiswa()}); konfigurasi constructor: {@code tbmuser}. Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyPortallayout
 */
public class DasboardSiswa extends MyPortallayout {

    private static final long serialVersionUID = 20260530172001L;
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> _CACHE
            = new java.util.concurrent.ConcurrentHashMap<String, Object>();
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> _EXPIRY
            = new java.util.concurrent.ConcurrentHashMap<String, Long>();
    private static final long _TTL_MS = 5L * 60 * 1000;
    private static final int TOP_LIMIT = 10;
    private static final int DETAIL_LIMIT = 75;

    private static boolean debug = true;

    private Tbmuser tbmuser;
    private Integer desktopHeight = 11000;

    private Div body;
    private Vbox loadingDashboardContainer;
    private int dashboardLoadVersion;

    private Intbox mulaiTahun;
    private Intbox sampaiTahun;
    private Combobox searchSekolah;
    private Combobox searchKelas;
    private Combobox searchJenisKelamin;
    private Combobox searchStatusSiswa;

    private int currentMulai;
    private int currentSampai;
    private Sekolah currentSekolah;
    private KelasSiswa currentKelas;
    private String currentJenisKelamin;
    private String currentStatusSiswa;

    public DasboardSiswa() throws Exception {
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
        DashboardGridExportHelper.pasang(this, "Siswa");
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
        panel.setTitle("Dasboard Siswa");
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
            Number maxSiswa = (Number) HibernateUtil.currentSession().createCriteria(Siswa.class)
                    .add(activeCriterion()).setProjection(Projections.max("tahunMasuk")).uniqueResult();
            if (maxSiswa != null && maxSiswa.intValue() > tahunSekarang) {
                tahunSekarang = maxSiswa.intValue();
            }
        } catch (Exception e) {
            debugError("initDefaultFilterValue", e);
        }

        currentSampai = tahunSekarang;
        currentMulai = tahunSekarang - 2;
        currentJenisKelamin = null;
        currentStatusSiswa = null;
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
        currentKelas = getSelectedKelas();
        currentJenisKelamin = getSelectedString(searchJenisKelamin, currentJenisKelamin);
        currentStatusSiswa = getSelectedString(searchStatusSiswa, currentStatusSiswa);

        renderHero();
        renderFilter();

        final int loadVersion = ++dashboardLoadVersion;
        tampilkanLoadingDashboardSiswa("Menyiapkan parameter filter dan tampilan awal...", 2);

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
            updateDashboardProgress("Mengambil ringkasan profil siswa dan relasi kelas...", 8);
            DashboardSiswaData data = loadDashboardDataCached();
            if (loadVersion != dashboardLoadVersion) {
                return;
            }
            updateDashboardProgress("Menyusun kartu, panel analitik, tabel top data, tren, dan rekomendasi...", 96);
            hapusLoadingDashboardSiswa();

            renderOverview(data);
            renderProfilDanKelengkapan(data);
            renderAktivitasKesiswaan(data);
            renderTopTablesAndTrends(data);
            renderRecommendation(data);
        } catch (Exception e) {
            debugError("renderDashboardContentDenganLoading", e);
            hapusLoadingDashboardSiswa();
            appendHtml(body, "<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; "
                    + "color:#991b1b; border:1px solid #fecdd3; font-weight:700;'>Dasboard Siswa belum dapat dimuat. "
                    + "Silakan tekan Refresh atau aktifkan debug untuk melihat detail error.</div>");
        }
    }

    private void tampilkanLoadingDashboardSiswa(String pesan, int persen) {
        if (body == null) {
            return;
        }
        hapusLoadingDashboardSiswa();
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

    private void hapusLoadingDashboardSiswa() {
        if (loadingDashboardContainer != null) {
            try {
                if (loadingDashboardContainer.getParent() != null) {
                    loadingDashboardContainer.detach();
                }
            } catch (Exception e) {
                debugError("hapusLoadingDashboardSiswa", e);
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
                + "<i class=\"fa fa-spinner fa-spin\"></i> Memproses Dasboard Siswa</div>"
                + "<div style='font-size:12px; margin-bottom:10px; color:#64748b;'>" + safeHtml(pesan) + "</div>"
                + "<div style='width:100%; height:12px; border-radius:999px; overflow:hidden; background:#e2e8f0;'>"
                + "<div style='height:12px; width:" + progress + "%; border-radius:999px; "
                + "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>"
                + "<div style='font-size:12px; font-weight:800; color:#4338ca; margin-top:7px;'>" + progress + "% selesai</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:4px;'>Mohon tunggu. Sistem sedang mengambil data siswa, kelas, prestasi, pelanggaran, apresiasi, asrama, dan organisasi.</div>"
                + "</div>";
    }

    private void renderHero() {
        String user = tbmuser == null ? "Pengguna" : safeText(tbmuser.getUserNama());
        String periode = currentMulai + " s.d. " + currentSampai;
        String sekolah = currentSekolah == null ? "Semua Sekolah" : safeText(labelSekolah(currentSekolah));
        String kelas = currentKelas == null ? "Semua Kelas" : safeText(labelKelas(currentKelas));
        String jk = currentJenisKelamin == null ? "Semua Jenis Kelamin" : currentJenisKelamin;
        String status = currentStatusSiswa == null ? "Semua Status Siswa" : currentStatusSiswa;

        appendHtml(body, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
                + "color:#ffffff; box-shadow:0 18px 38px rgba(67,56,202,0.22);'>"
                + "<div style='position:absolute; width:240px; height:240px; right:-70px; top:-90px; border-radius:999px; background:rgba(255,255,255,0.13);'></div>"
                + "<div style='position:absolute; width:160px; height:160px; right:120px; bottom:-92px; border-radius:999px; background:rgba(255,255,255,0.10);'></div>"
                + "<div style='position:relative; z-index:2;'>"
                + "<div style='font-size:12px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:700;'>Monitoring Kesiswaan, Prestasi, Apresiasi, Disiplin & Keaktifan</div>"
                + "<div style='font-size:28px; line-height:1.18; font-weight:800; margin-top:7px;'>Dasboard Siswa</div>"
                + "<div style='font-size:13px; max-width:930px; opacity:.93; margin-top:8px;'>Ringkasan terpadu untuk memantau data siswa, distribusi kelas, kelengkapan kontak, status siswa, prestasi, apresiasi, pelanggaran, keasramaan, dan keaktifan organisasi siswa dalam satu halaman operasional.</div>"
                + "<div style='margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;'>"
                + badgeHtml("Periode: " + periode, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(sekolah, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(kelas, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(jk, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(status, "rgba(255,255,255,.16)", "#ffffff")
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

        new MyLabelAgakKecil("Tahun:").setParent(toolbar);
        mulaiTahun = new Intbox(currentMulai);
        mulaiTahun.setCols(4);
        mulaiTahun.setParent(toolbar);

        new Label(ais.common.Common.getBahasaConfig("s.d.")).setParent(toolbar);
        sampaiTahun = new Intbox(currentSampai);
        sampaiTahun.setCols(4);
        sampaiTahun.setParent(toolbar);

        new MyLabelAgakKecil("Sekolah:").setParent(toolbar);
        searchSekolah = new Combobox();
        searchSekolah.setCols(22);
        searchSekolah.setReadonly(true);
        searchSekolah.setParent(toolbar);
        populateSekolahCombo(searchSekolah);
        selectComboByValue(searchSekolah, currentSekolah);

        new MyLabelAgakKecil("Kelas:").setParent(toolbar);
        searchKelas = new Combobox();
        searchKelas.setCols(22);
        searchKelas.setReadonly(true);
        searchKelas.setParent(toolbar);
        populateKelasCombo(searchKelas, currentSekolah);
        selectComboByValue(searchKelas, currentKelas);

        new MyLabelAgakKecil("Jenis Kelamin:").setParent(toolbar);
        searchJenisKelamin = new Combobox();
        searchJenisKelamin.setCols(14);
        searchJenisKelamin.setReadonly(true);
        searchJenisKelamin.setParent(toolbar);
        populateJenisKelaminCombo(searchJenisKelamin);
        selectComboByValue(searchJenisKelamin, currentJenisKelamin);

        new MyLabelAgakKecil("Status:").setParent(toolbar);
        searchStatusSiswa = new Combobox();
        searchStatusSiswa.setCols(18);
        searchStatusSiswa.setReadonly(true);
        searchStatusSiswa.setParent(toolbar);
        populateStatusSiswaCombo(searchStatusSiswa);
        selectComboByValue(searchStatusSiswa, currentStatusSiswa);

        EventListener reloadListener = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                renderContent();
            }
        };
        mulaiTahun.addEventListener("onChange", reloadListener);
        sampaiTahun.addEventListener("onChange", reloadListener);
        searchSekolah.addEventListener("onChange", reloadListener);
        searchKelas.addEventListener("onChange", reloadListener);
        searchJenisKelamin.addEventListener("onChange", reloadListener);
        searchStatusSiswa.addEventListener("onChange", reloadListener);

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
        refresh.setTooltiptext("Refresh dasboard");
        refresh.setParent(toolbar);
        refresh.addEventListener("onClick", reloadListener);
    }

    private String buildCacheKey() {
        return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
                + "|" + currentMulai + "|" + currentSampai
                + "|" + (currentSekolah == null ? "" : currentSekolah.getId())
                + "|" + (currentKelas == null ? "" : currentKelas.getId())
                + "|" + (currentJenisKelamin == null ? "" : currentJenisKelamin)
                + "|" + (currentStatusSiswa == null ? "" : currentStatusSiswa);
    }

    @SuppressWarnings("unchecked")
    private DashboardSiswaData loadDashboardDataCached() throws Exception {
        String _k = buildCacheKey();
        Long _e = _EXPIRY.get(_k);
        if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
            return (DashboardSiswaData) _CACHE.get(_k);
        }
        DashboardSiswaData data = loadDashboardData();
        _CACHE.put(_k, data);
        _EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
        return data;
    }

    private DashboardSiswaData loadDashboardData() throws Exception {
        DashboardSiswaData data = new DashboardSiswaData();
        data.tahunMulai = currentMulai;
        data.tahunSampai = currentSampai;
        data.tahunAjaranLabel = labelTahunAjaranFilter();

        updateDashboardProgress("Mengambil jumlah siswa, gender, status, dan keterkaitan kelas...", 14);
        data.totalSiswa = countSiswa(Boolean.TRUE, null, null, null, false);
        data.totalSiswaSemua = countSiswa(null, null, null, null, false);
        data.totalSiswaNonAktif = countSiswa(Boolean.FALSE, null, null, null, false);
        data.totalLakiLaki = countSiswa(Boolean.TRUE, "jenisKelamin", "Laki-laki", null, false);
        data.totalPerempuan = countSiswa(Boolean.TRUE, "jenisKelamin", "Perempuan", null, false);
        data.totalSiswaDenganKelas = countSiswaDenganKelas();
        data.totalSiswaTanpaKelas = data.totalSiswa - data.totalSiswaDenganKelas;
        if (data.totalSiswaTanpaKelas < 0) {
            data.totalSiswaTanpaKelas = 0;
        }

        updateDashboardProgress("Mengambil kelengkapan kontak siswa dan orang tua...", 27);
        data.totalEmailSiswa = countSiswa(Boolean.TRUE, null, null, "alamatEmail", false);
        data.totalHpSiswa = countSiswa(Boolean.TRUE, null, null, "hp", false);
        data.totalTeleponSiswa = countSiswa(Boolean.TRUE, null, null, "teleponSiswa", false);
        data.totalTeleponOrtu = countSiswa(Boolean.TRUE, null, null, "teleponOrangTua", false);
        data.totalAlamatSiswa = countSiswa(Boolean.TRUE, null, null, "alamatSiswa", false);

        updateDashboardProgress("Mengambil prestasi, apresiasi, dan pelanggaran siswa...", 45);
        data.totalPrestasi = countEntitySiswa(PrestasiSiswa.class, false, null, null, null, null);
        data.totalPrestasiDisetujui = countEntitySiswa(PrestasiSiswa.class, false, "status", PrestasiSiswa.DISETUJUI, null, null);
        data.totalPrestasiDitolak = countEntitySiswa(PrestasiSiswa.class, false, "status", PrestasiSiswa.DITOLAK, null, null);
        data.totalPrestasiBelum = countEntitySiswa(PrestasiSiswa.class, false, "status", PrestasiSiswa.BELUM_DIPROSES, null, null);
        data.totalPrestasiSedang = countEntitySiswa(PrestasiSiswa.class, false, "status", PrestasiSiswa.SEDANG_DIPROSES, null, null);
        data.totalPrestasiLuar = countEntitySiswa(PrestasiSiswa.class, false, "prestasiLuarKampus", Boolean.TRUE, null, null);
        data.totalPelanggaran = countEntitySiswa(PelanggaranSiswa.class, true, null, null, null, null);
        data.totalApresiasi = countEntitySiswa(ApresiasiSiswa.class, true, null, null, null, null);

        updateDashboardProgress("Mengambil data asrama dan organisasi siswa...", 62);
        data.totalAsrama = countGeneric(AsramaSiswa.class, true);
        data.totalPenghuniAsrama = countEntitySiswa(AsramaSiswaPunyaSiswa.class, true, null, null, null, null);
        data.totalOrganisasi = countGeneric(OrganisasiSiswa.class, false);
        data.totalAnggotaOrganisasi = countEntitySiswa(OrganisasiSiswaPunyaSiswa.class, false, null, null, null, null);
        data.totalAnggotaOrganisasiDisetujui = countEntitySiswa(OrganisasiSiswaPunyaSiswa.class, false, "persetujuan", Boolean.TRUE, null, null);

        updateDashboardProgress("Menghitung rasio kesiapan data siswa...", 74);
        data.rasioKelas = percent(data.totalSiswaDenganKelas, data.totalSiswa);
        data.rasioEmail = percent(data.totalEmailSiswa, data.totalSiswa);
        data.rasioHpOrtu = percent(data.totalTeleponOrtu, data.totalSiswa);
        data.rasioApresiasiPelanggaran = percent(data.totalApresiasi, data.totalApresiasi + data.totalPelanggaran);
        data.rasioPrestasiDisetujui = percent(data.totalPrestasiDisetujui, data.totalPrestasi);

        updateDashboardProgress("Menyusun tabel top data dan tren per tahun...", 86);
        data.topKelasPeserta = loadTopKelasPeserta();
        data.topAsramaPenghuni = loadTopAsramaPenghuni();
        data.topOrganisasiAnggota = loadTopOrganisasiAnggota();
        data.topPrestasiStatus = loadTopGroup(PrestasiSiswa.class, "status", null, "Status Belum Diisi");
        data.topPrestasiKategori = loadTopAssociationGroup(PrestasiSiswa.class, "kategoriPrestasiSiswa", "nama", "Kategori Prestasi");
        data.topApresiasi = loadTopAssociationGroup(ApresiasiSiswa.class, "apresiasiDanPenghargaan", "nama", "Jenis Apresiasi");
        data.topPelanggaran = loadTopAssociationGroup(PelanggaranSiswa.class, "pelanggaranDanHukuman", "nama", "Jenis Pelanggaran");
        data.trendSiswa = loadTrendSiswa();
        data.trendPrestasi = loadTrendEntity(PrestasiSiswa.class);
        data.trendApresiasi = loadTrendEntity(ApresiasiSiswa.class);
        data.trendPelanggaran = loadTrendEntity(PelanggaranSiswa.class);

        sortAndLimit(data.topKelasPeserta, TOP_LIMIT);
        sortAndLimit(data.topAsramaPenghuni, TOP_LIMIT);
        sortAndLimit(data.topOrganisasiAnggota, TOP_LIMIT);
        sortAndLimit(data.topPrestasiStatus, TOP_LIMIT);
        sortAndLimit(data.topPrestasiKategori, TOP_LIMIT);
        sortAndLimit(data.topApresiasi, TOP_LIMIT);
        sortAndLimit(data.topPelanggaran, TOP_LIMIT);

        return data;
    }

    private void renderOverview(DashboardSiswaData data) {
        appendHtml(body, sectionIntroHtml("Ringkasan Utama Siswa", "Klik angka pada kartu untuk membuka contoh detail data sesuai filter aktif. Data disusun untuk memantau profil, kelas, prestasi, apresiasi, pelanggaran, asrama, dan organisasi siswa."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");

        appendMetricCard(grid, "Siswa Aktif", data.totalSiswa, "Siswa aktif sesuai tahun masuk/filter", "#4338ca", "fa-users", "SISWA");
        appendMetricCard(grid, "Belum Terkait Kelas", data.totalSiswaTanpaKelas, data.rasioKelas + "% siswa sudah terkait kelas", "#ea580c", "fa-exclamation-triangle", "SISWA_TANPA_KELAS");
        appendMetricCard(grid, "Prestasi", data.totalPrestasi, data.rasioPrestasiDisetujui + "% disetujui", "#0f766e", "fa-trophy", "PRESTASI");
        appendMetricCard(grid, "Pelanggaran", data.totalPelanggaran, "Pelanggaran aktif pada periode", "#dc2626", "fa-warning", "PELANGGARAN");
        appendMetricCard(grid, "Apresiasi", data.totalApresiasi, "Apresiasi/penghargaan siswa", "#7c3aed", "fa-star", "APRESIASI");
        appendMetricCard(grid, "Penghuni Asrama", data.totalPenghuniAsrama, "Relasi aktif siswa-asrama", "#0369a1", "fa-home", "ASRAMA");
        appendMetricCard(grid, "Anggota Organisasi", data.totalAnggotaOrganisasi, "Riwayat organisasi siswa", "#be123c", "fa-sitemap", "ORGANISASI");
        appendMetricCard(grid, "Email Terisi", data.totalEmailSiswa, data.rasioEmail + "% dari siswa aktif", "#0891b2", "fa-envelope", "SISWA_EMAIL");
    }

    private void renderProfilDanKelengkapan(DashboardSiswaData data) {
        appendHtml(body, sectionIntroHtml("Profil & Kelengkapan Data", "Bagian ini membantu operator memeriksa keseimbangan profil siswa dan kesiapan data komunikasi keluarga."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(300px,1fr)); gap:12px;");

        String gender = dashboardPanelHtml(
                dashboardExplainHtml("Komposisi jenis kelamin dihitung dari siswa aktif sesuai filter. Bila angka kecil, cek kembali penulisan jenis kelamin pada master siswa.")
                + progressSectionHtml("Komposisi Gender", data.totalSiswa,
                        new DashboardMiniRow("Laki-laki", data.totalLakiLaki),
                        new DashboardMiniRow("Perempuan", data.totalPerempuan),
                        new DashboardMiniRow("Tidak/Belum Sesuai", data.totalSiswa - data.totalLakiLaki - data.totalPerempuan), null));
        appendHtml(grid, gender);

        String kontak = dashboardPanelHtml(
                dashboardExplainHtml("Kelengkapan kontak diperlukan untuk notifikasi akademik, pembayaran, pelanggaran, prestasi, dan komunikasi wali kelas/orang tua.")
                + progressSectionHtml("Kesiapan Data Kontak", data.totalSiswa,
                        new DashboardMiniRow("Email Siswa", data.totalEmailSiswa),
                        new DashboardMiniRow("HP Siswa", data.totalHpSiswa + data.totalTeleponSiswa),
                        new DashboardMiniRow("Telepon Orang Tua", data.totalTeleponOrtu),
                        new DashboardMiniRow("Alamat Siswa", data.totalAlamatSiswa)));
        appendHtml(grid, kontak);

        String kelas = dashboardPanelHtml(
                dashboardExplainHtml("Siswa yang belum terkait kelas perlu ditindaklanjuti agar proses akademik, presensi, nilai, pembayaran, dan laporan dapat berjalan konsisten.")
                + miniStatHtml("Sudah terkait kelas", data.totalSiswaDenganKelas, data.rasioKelas + "% dari siswa aktif")
                + miniStatHtml("Belum terkait kelas", data.totalSiswaTanpaKelas, "Perlu pengecekan relasi kelas_punya_siswa")
                + miniStatHtml("Siswa nonaktif", data.totalSiswaNonAktif, "Di luar indikator siswa aktif utama"));
        appendHtml(grid, kelas);
    }

    private void renderAktivitasKesiswaan(DashboardSiswaData data) {
        appendHtml(body, sectionIntroHtml("Aktivitas Kesiswaan", "Perbandingan prestasi, apresiasi, pelanggaran, asrama, dan organisasi memberi gambaran pembinaan siswa secara menyeluruh."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(300px,1fr)); gap:12px;");

        String prestasi = dashboardPanelHtml(
                dashboardExplainHtml("Pantau status proses prestasi agar sertifikat/pengakuan siswa tidak tertahan terlalu lama.")
                + progressSectionHtml("Status Prestasi", data.totalPrestasi,
                        new DashboardMiniRow("Disetujui", data.totalPrestasiDisetujui),
                        new DashboardMiniRow("Sedang Diproses", data.totalPrestasiSedang),
                        new DashboardMiniRow("Belum Diproses", data.totalPrestasiBelum),
                        new DashboardMiniRow("Ditolak", data.totalPrestasiDitolak)));
        appendHtml(grid, prestasi);

        String pembinaan = dashboardPanelHtml(
                dashboardExplainHtml("Apresiasi perlu lebih menonjol dari pelanggaran agar budaya pembinaan lebih positif dan terukur.")
                + progressSectionHtml("Apresiasi vs Pelanggaran", data.totalApresiasi + data.totalPelanggaran,
                        new DashboardMiniRow("Apresiasi", data.totalApresiasi),
                        new DashboardMiniRow("Pelanggaran", data.totalPelanggaran),
                        new DashboardMiniRow("Rasio Apresiasi", data.rasioApresiasiPelanggaran),
                        null));
        appendHtml(grid, pembinaan);

        String organisasi = dashboardPanelHtml(
                dashboardExplainHtml("Keterlibatan siswa pada asrama dan organisasi dapat dipakai sebagai indikator pembinaan karakter, kepemimpinan, dan minat bakat.")
                + miniStatHtml("Master Asrama", data.totalAsrama, "Asrama aktif")
                + miniStatHtml("Penghuni Asrama", data.totalPenghuniAsrama, "Relasi asrama-punya-siswa aktif")
                + miniStatHtml("Master Organisasi", data.totalOrganisasi, "Organisasi siswa")
                + miniStatHtml("Anggota Disetujui", data.totalAnggotaOrganisasiDisetujui, "Keanggotaan organisasi tervalidasi"));
        appendHtml(grid, organisasi);
    }

    private void renderTopTablesAndTrends(DashboardSiswaData data) {
        appendHtml(body, sectionIntroHtml("Top Data & Tren", "Tabel ini menampilkan ranking tertinggi dan tren tahunan agar area yang perlu perhatian cepat terlihat."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px;");

        appendMiniTable(grid, "Top Kelas Berdasarkan Jumlah Siswa", "Relasi kelas_punya_siswa aktif", data.topKelasPeserta, "Kelas", "Siswa");
        appendMiniTable(grid, "Top Asrama Berdasarkan Penghuni", "Relasi asrama_punya_siswa aktif", data.topAsramaPenghuni, "Asrama", "Siswa");
        appendMiniTable(grid, "Top Organisasi Berdasarkan Anggota", "Riwayat organisasi siswa", data.topOrganisasiAnggota, "Organisasi", "Anggota");
        appendMiniTable(grid, "Status Prestasi", "Distribusi status prestasi", data.topPrestasiStatus, "Status", "Jumlah");
        appendMiniTable(grid, "Kategori Prestasi", "Kategori/cabang prestasi siswa", data.topPrestasiKategori, "Kategori", "Jumlah");
        appendMiniTable(grid, "Jenis Apresiasi", "Apresiasi dan penghargaan", data.topApresiasi, "Jenis", "Jumlah");
        appendMiniTable(grid, "Jenis Pelanggaran", "Pelanggaran dan hukuman", data.topPelanggaran, "Jenis", "Jumlah");
        appendMiniTable(grid, "Tren Siswa Baru", "Berdasarkan tahun masuk", data.trendSiswa, "Tahun", "Siswa");
        appendMiniTable(grid, "Tren Prestasi", "Berdasarkan tahun/tanggal prestasi", data.trendPrestasi, "Tahun", "Prestasi");
        appendMiniTable(grid, "Tren Apresiasi", "Berdasarkan waktu apresiasi", data.trendApresiasi, "Tahun", "Apresiasi");
        appendMiniTable(grid, "Tren Pelanggaran", "Berdasarkan waktu pelanggaran", data.trendPelanggaran, "Tahun", "Pelanggaran");
    }

    private void renderRecommendation(DashboardSiswaData data) {
        appendHtml(body, sectionIntroHtml("Rekomendasi Operasional", "Rekomendasi otomatis berdasarkan data yang sedang tampil pada filter aktif."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:12px;");

        String statusKelas = data.totalSiswaTanpaKelas > 0 ? "PERLU TINDAK LANJUT" : "BAIK";
        String warnaKelas = data.totalSiswaTanpaKelas > 0 ? "#ea580c" : "#16a34a";
        appendHtml(grid, recommendationCardHtml("Validasi Relasi Kelas", statusKelas,
                data.totalSiswaTanpaKelas > 0 ? "Masih ada " + formatNumber(data.totalSiswaTanpaKelas) + " siswa aktif yang belum terhubung dengan kelas. Prioritaskan perbaikan relasi kelas_punya_siswa agar laporan akademik lebih akurat." : "Seluruh siswa aktif pada filter ini sudah memiliki keterkaitan kelas.", warnaKelas));

        String statusKontak = data.rasioHpOrtu < 80 ? "TINGKATKAN DATA" : "CUKUP BAIK";
        String warnaKontak = data.rasioHpOrtu < 80 ? "#dc2626" : "#16a34a";
        appendHtml(grid, recommendationCardHtml("Lengkapi Kontak Orang Tua", statusKontak,
                "Kelengkapan telepon orang tua saat ini sekitar " + data.rasioHpOrtu + "%. Data ini penting untuk notifikasi wali kelas, pelanggaran, prestasi, pembayaran, dan keadaan darurat.", warnaKontak));

        String statusPrestasi = data.totalPrestasiBelum + data.totalPrestasiSedang > 0 ? "MONITOR PROSES" : "TERKENDALI";
        appendHtml(grid, recommendationCardHtml("Monitoring Prestasi", statusPrestasi,
                "Ada " + formatNumber(data.totalPrestasiBelum + data.totalPrestasiSedang) + " prestasi yang belum selesai diproses. Pastikan validasi, arsip sertifikat, dan status pengakuan siswa diperbarui.", statusPrestasi.equals("MONITOR PROSES") ? "#7c3aed" : "#16a34a"));

        String statusDisiplin = data.totalPelanggaran > data.totalApresiasi ? "PERKUAT PEMBINAAN" : "POSITIF";
        appendHtml(grid, recommendationCardHtml("Keseimbangan Pembinaan", statusDisiplin,
                "Jumlah apresiasi " + formatNumber(data.totalApresiasi) + " dan pelanggaran " + formatNumber(data.totalPelanggaran) + ". Dorong pencatatan apresiasi agar pembinaan siswa tidak hanya berfokus pada pelanggaran.", statusDisiplin.equals("PERKUAT PEMBINAAN") ? "#dc2626" : "#16a34a"));
    }

    private long countGeneric(Class clazz, boolean filterAktif) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            if (filterAktif && hasProperty(clazz, "aktif")) {
                c.add(activeCriterion());
            }
            if (currentSekolah != null && hasProperty(clazz, "sekolah")) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countGeneric " + clazz, e);
            return 0;
        }
    }

    private long countSiswa(Boolean aktif, String eqProperty, Object eqValue, String notEmptyProperty, boolean tanpaKelas) {
        try {
            if (currentKelas != null) {
                Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
                c.createAlias("siswa", "siswa");
                c.createAlias("kelasSiswa", "kelas");
                c.add(activeCriterion("aktif"));
                c.add(activeCriterion("kelas.aktif"));
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
                applySiswaAliasFilter(c, "siswa");
                if (aktif != null) {
                    if (Boolean.TRUE.equals(aktif)) {
                        c.add(activeCriterion("siswa.aktif"));
                    } else {
                        c.add(Restrictions.eq("siswa.aktif", Boolean.FALSE));
                    }
                }
                if (eqProperty != null && eqValue != null) {
                    c.add(Restrictions.eq("siswa." + eqProperty, eqValue));
                }
                if (notEmptyProperty != null) {
                    addNotEmpty(c, "siswa." + notEmptyProperty);
                }
                c.setProjection(Projections.countDistinct("siswa"));
                Number n = (Number) c.uniqueResult();
                return n == null ? 0 : n.longValue();
            }

            Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
            applySiswaAliasFilter(c, null);
            if (aktif != null) {
                if (Boolean.TRUE.equals(aktif)) {
                    c.add(activeCriterion());
                } else {
                    c.add(Restrictions.eq("aktif", Boolean.FALSE));
                }
            }
            if (eqProperty != null && eqValue != null) {
                c.add(Restrictions.eq(eqProperty, eqValue));
            }
            if (notEmptyProperty != null) {
                addNotEmpty(c, notEmptyProperty);
            }
            if (tanpaKelas) {
                List ids = getSiswaIdsDenganKelas();
                if (ids != null && !ids.isEmpty()) {
                    c.add(Restrictions.not(Restrictions.in("id", ids)));
                }
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countSiswa", e);
            return 0;
        }
    }

    private long countSiswaDenganKelas() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("siswa", "siswa");
            c.createAlias("kelasSiswa", "kelas");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("siswa.aktif"));
            c.add(activeCriterion("kelas.aktif"));
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            applySiswaAliasFilter(c, "siswa");
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            c.setProjection(Projections.countDistinct("siswa"));
            Number n = (Number) c.uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countSiswaDenganKelas", e);
            return 0;
        }
    }

    private long countEntitySiswa(Class clazz, boolean filterAktif, String eqProperty, Object eqValue, String notEmptyProperty, String forcedDetailKey) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz, "x");
            if (hasProperty(clazz, "siswa")) {
                c.createAlias("siswa", "siswa");
                applySiswaAliasFilter(c, "siswa");
            }
            if (filterAktif && hasProperty(clazz, "aktif")) {
                c.add(activeCriterion("aktif"));
            }
            if (currentSekolah != null) {
                if (hasProperty(clazz, "sekolah")) {
                    c.add(Restrictions.eq("sekolah", currentSekolah));
                } else if (hasProperty(clazz, "siswa")) {
                    c.add(Restrictions.eq("siswa.sekolah", currentSekolah));
                }
            }
            applyPeriodFilter(c, clazz, null);
            applyKelasFilterIfPossible(c, clazz, null);
            if (eqProperty != null && eqValue != null) {
                c.add(Restrictions.eq(eqProperty, eqValue));
            }
            if (notEmptyProperty != null) {
                addNotEmpty(c, notEmptyProperty);
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countEntitySiswa " + clazz, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopKelasPeserta() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("kelasSiswa", "kelas");
            c.createAlias("siswa", "siswa");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("kelas.aktif"));
            c.add(activeCriterion("siswa.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            applySiswaAliasFilter(c, "siswa");
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("kelas.nama"))
                    .add(Projections.countDistinct("siswa")));
            List<Object[]> result = c.list();
            for (int i = 0; i < result.size(); i++) {
                Object[] o = result.get(i);
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopKelasPeserta", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopAsramaPenghuni() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(AsramaSiswaPunyaSiswa.class, "asp");
            c.createAlias("asramaSiswa", "asrama");
            c.createAlias("siswa", "siswa");
            c.add(activeCriterion("aktif"));
            applySiswaAliasFilter(c, "siswa");
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("asrama.nama"))
                    .add(Projections.countDistinct("siswa")));
            List<Object[]> result = c.list();
            for (int i = 0; i < result.size(); i++) {
                Object[] o = result.get(i);
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopAsramaPenghuni", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopOrganisasiAnggota() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(OrganisasiSiswaPunyaSiswa.class, "osp");
            c.createAlias("organisasiSiswa", "organisasi");
            c.createAlias("siswa", "siswa");
            applySiswaAliasFilter(c, "siswa");
            applyPeriodFilter(c, OrganisasiSiswaPunyaSiswa.class, null);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("organisasi.nama"))
                    .add(Projections.countDistinct("siswa")));
            List<Object[]> result = c.list();
            for (int i = 0; i < result.size(); i++) {
                Object[] o = result.get(i);
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopOrganisasiAnggota", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopGroup(Class clazz, String property, String aliasPrefix, String emptyLabel) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz, "x");
            if (hasProperty(clazz, "siswa")) {
                c.createAlias("siswa", "siswa");
                applySiswaAliasFilter(c, "siswa");
            }
            if (hasProperty(clazz, "aktif")) {
                c.add(activeCriterion("aktif"));
            }
            if (currentSekolah != null && hasProperty(clazz, "sekolah")) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            applyPeriodFilter(c, clazz, null);
            applyKelasFilterIfPossible(c, clazz, null);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty(prefix(aliasPrefix, property)))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (int i = 0; i < result.size(); i++) {
                Object[] o = result.get(i);
                rows.add(new DashboardMiniRow(safeText(o[0]).length() == 0 ? emptyLabel : safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopGroup " + clazz + " " + property, e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopAssociationGroup(Class clazz, String association, String labelProperty, String fallback) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz, "x");
            c.createAlias(association, "grp");
            if (hasProperty(clazz, "siswa")) {
                c.createAlias("siswa", "siswa");
                applySiswaAliasFilter(c, "siswa");
            }
            if (hasProperty(clazz, "aktif")) {
                c.add(activeCriterion("aktif"));
            }
            if (currentSekolah != null && hasProperty(clazz, "sekolah")) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            applyPeriodFilter(c, clazz, null);
            applyKelasFilterIfPossible(c, clazz, null);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("grp." + labelProperty))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (int i = 0; i < result.size(); i++) {
                Object[] o = result.get(i);
                rows.add(new DashboardMiniRow(safeText(o[0]).length() == 0 ? fallback : safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopAssociationGroup " + clazz + " " + association, e);
        }
        return rows;
    }

    private List<DashboardMiniRow> loadTrendSiswa() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        for (int tahun = currentMulai; tahun <= currentSampai; tahun++) {
            rows.add(new DashboardMiniRow(String.valueOf(tahun), countSiswaByTahun(tahun)));
        }
        return rows;
    }

    private long countSiswaByTahun(int tahun) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
            c.add(activeCriterion());
            c.add(Restrictions.eq("tahunMasuk", new Integer(tahun)));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            if (currentJenisKelamin != null) {
                c.add(Restrictions.eq("jenisKelamin", currentJenisKelamin));
            }
            if (currentStatusSiswa != null) {
                c.add(Restrictions.eq("statusSiswa", currentStatusSiswa));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countSiswaByTahun", e);
            return 0;
        }
    }

    private List<DashboardMiniRow> loadTrendEntity(Class clazz) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        for (int tahun = currentMulai; tahun <= currentSampai; tahun++) {
            rows.add(new DashboardMiniRow(String.valueOf(tahun), countEntityBySingleYear(clazz, tahun)));
        }
        return rows;
    }

    private long countEntityBySingleYear(Class clazz, int tahun) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz, "x");
            if (hasProperty(clazz, "siswa")) {
                c.createAlias("siswa", "siswa");
                applySiswaAliasFilter(c, "siswa");
            }
            if (hasProperty(clazz, "aktif")) {
                c.add(activeCriterion("aktif"));
            }
            if (currentSekolah != null && hasProperty(clazz, "sekolah")) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            applySingleYearFilter(c, clazz, tahun);
            applyKelasFilterIfPossible(c, clazz, null);
            return getCount(c);
        } catch (Exception e) {
            debugError("countEntityBySingleYear " + clazz, e);
            return 0;
        }
    }

    private void applySiswaAliasFilter(Criteria c, String aliasPrefix) {
        try {
            String p = aliasPrefix == null || aliasPrefix.trim().length() == 0 ? "" : aliasPrefix + ".";
            if (currentSekolah != null) {
                c.add(Restrictions.eq(p + "sekolah", currentSekolah));
            }
            c.add(Restrictions.ge(p + "tahunMasuk", new Integer(currentMulai)));
            c.add(Restrictions.le(p + "tahunMasuk", new Integer(currentSampai)));
            if (currentJenisKelamin != null) {
                c.add(Restrictions.eq(p + "jenisKelamin", currentJenisKelamin));
            }
            if (currentStatusSiswa != null) {
                c.add(Restrictions.eq(p + "statusSiswa", currentStatusSiswa));
            }
        } catch (Exception e) {
            debugError("applySiswaAliasFilter", e);
        }
    }

    private void applyPeriodFilter(Criteria c, Class clazz, String aliasPrefix) {
        try {
            String p = aliasPrefix == null || aliasPrefix.trim().length() == 0 ? "" : aliasPrefix + ".";
            if (hasProperty(clazz, "tahunMasuk")) {
                c.add(Restrictions.ge(p + "tahunMasuk", new Integer(currentMulai)));
                c.add(Restrictions.le(p + "tahunMasuk", new Integer(currentSampai)));
            } else if (hasProperty(clazz, "tahun")) {
                c.add(Restrictions.ge(p + "tahun", new Integer(currentMulai)));
                c.add(Restrictions.le(p + "tahun", new Integer(currentSampai)));
            } else if (hasProperty(clazz, "tahunAjaran")) {
                c.add(Restrictions.in(p + "tahunAjaran", buildTahunAjaranList(currentMulai, currentSampai)));
            } else if (hasProperty(clazz, "tahunAkademik")) {
                c.add(Restrictions.in(p + "tahunAkademik", buildTahunAjaranList(currentMulai, currentSampai)));
            } else if (hasProperty(clazz, "ta")) {
                c.add(Restrictions.in(p + "ta", buildTahunAjaranList(currentMulai, currentSampai)));
            } else if (hasProperty(clazz, "waktu")) {
                c.add(Restrictions.ge(p + "waktu", startDate(currentMulai)));
                c.add(Restrictions.le(p + "waktu", endDate(currentSampai)));
            } else if (hasProperty(clazz, "tanggal")) {
                c.add(Restrictions.ge(p + "tanggal", startDate(currentMulai)));
                c.add(Restrictions.le(p + "tanggal", endDate(currentSampai)));
            } else if (hasProperty(clazz, "mulai")) {
                c.add(Restrictions.ge(p + "mulai", startDate(currentMulai)));
                c.add(Restrictions.le(p + "mulai", endDate(currentSampai)));
            }
        } catch (Exception e) {
            debugError("applyPeriodFilter " + clazz, e);
        }
    }

    private void applySingleYearFilter(Criteria c, Class clazz, int tahun) {
        try {
            if (hasProperty(clazz, "tahunMasuk")) {
                c.add(Restrictions.eq("tahunMasuk", new Integer(tahun)));
            } else if (hasProperty(clazz, "tahun")) {
                c.add(Restrictions.eq("tahun", new Integer(tahun)));
            } else if (hasProperty(clazz, "tahunAjaran")) {
                c.add(Restrictions.in("tahunAjaran", buildTahunAjaranList(tahun, tahun)));
            } else if (hasProperty(clazz, "tahunAkademik")) {
                c.add(Restrictions.in("tahunAkademik", buildTahunAjaranList(tahun, tahun)));
            } else if (hasProperty(clazz, "ta")) {
                c.add(Restrictions.in("ta", buildTahunAjaranList(tahun, tahun)));
            } else if (hasProperty(clazz, "waktu")) {
                c.add(Restrictions.ge("waktu", startDate(tahun)));
                c.add(Restrictions.le("waktu", endDate(tahun)));
            } else if (hasProperty(clazz, "tanggal")) {
                c.add(Restrictions.ge("tanggal", startDate(tahun)));
                c.add(Restrictions.le("tanggal", endDate(tahun)));
            } else if (hasProperty(clazz, "mulai")) {
                c.add(Restrictions.ge("mulai", startDate(tahun)));
                c.add(Restrictions.le("mulai", endDate(tahun)));
            }
        } catch (Exception e) {
            debugError("applySingleYearFilter " + clazz, e);
        }
    }

    private void applyKelasFilterIfPossible(Criteria c, Class clazz, String aliasPrefix) {
        try {
            if (currentKelas == null) {
                return;
            }
            if (hasProperty(clazz, "kelasSiswa")) {
                c.add(Restrictions.eq(prefix(aliasPrefix, "kelasSiswa"), currentKelas));
            } else if (hasProperty(clazz, "kelas")) {
                c.add(Restrictions.eq(prefix(aliasPrefix, "kelas"), currentKelas));
            }
        } catch (Exception e) {
            debugError("applyKelasFilterIfPossible " + clazz, e);
        }
    }

    private void addNotEmpty(Criteria c, String property) {
        c.add(Restrictions.isNotNull(property));
        c.add(Restrictions.ne(property, ""));
    }

    private String prefix(String aliasPrefix, String property) {
        return aliasPrefix == null || aliasPrefix.trim().length() == 0 ? property : aliasPrefix + "." + property;
    }

    private Criterion activeCriterion() {
        return activeCriterion("aktif");
    }

    private Criterion activeCriterion(String property) {
        Disjunction or = Restrictions.disjunction();
        or.add(Restrictions.eq(property, Boolean.TRUE));
        or.add(Restrictions.isNull(property));
        return or;
    }

    private long getCount(Criteria c) {
        c.setProjection(Projections.rowCount());
        Number n = (Number) c.uniqueResult();
        return n == null ? 0 : n.longValue();
    }

    @SuppressWarnings("unchecked")
    private List getSiswaIdsDenganKelas() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("siswa", "siswa");
            c.createAlias("kelasSiswa", "kelas");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("siswa.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            c.setProjection(Projections.distinct(Projections.property("siswa.id")));
            return c.list();
        } catch (Exception e) {
            debugError("getSiswaIdsDenganKelas", e);
            return new ArrayList();
        }
    }

    private List buildDetailList(String key) {
        try {
            if ("SISWA".equals(key) || "SISWA_EMAIL".equals(key) || "SISWA_TANPA_KELAS".equals(key)) {
                List list = buildSiswaDetailList(key);
                return list;
            }
            if ("PRESTASI".equals(key) || "PRESTASI_DISETUJUI".equals(key) || "PRESTASI_PENDING".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(PrestasiSiswa.class, "x");
                c.createAlias("siswa", "siswa");
                applySiswaAliasFilter(c, "siswa");
                applyPeriodFilter(c, PrestasiSiswa.class, null);
                applyKelasFilterIfPossible(c, PrestasiSiswa.class, null);
                if (currentSekolah != null) {
                    c.add(Restrictions.eq("sekolah", currentSekolah));
                }
                if ("PRESTASI_DISETUJUI".equals(key)) {
                    c.add(Restrictions.eq("status", PrestasiSiswa.DISETUJUI));
                } else if ("PRESTASI_PENDING".equals(key)) {
                    c.add(Restrictions.or(Restrictions.eq("status", PrestasiSiswa.BELUM_DIPROSES), Restrictions.eq("status", PrestasiSiswa.SEDANG_DIPROSES)));
                }
                c.addOrder(Order.desc("tanggal"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("PELANGGARAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(PelanggaranSiswa.class, "x");
                c.createAlias("siswa", "siswa");
                c.add(activeCriterion("aktif"));
                applySiswaAliasFilter(c, "siswa");
                applyPeriodFilter(c, PelanggaranSiswa.class, null);
                if (currentSekolah != null) {
                    c.add(Restrictions.eq("sekolah", currentSekolah));
                }
                c.addOrder(Order.desc("waktu"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("APRESIASI".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(ApresiasiSiswa.class, "x");
                c.createAlias("siswa", "siswa");
                c.add(activeCriterion("aktif"));
                applySiswaAliasFilter(c, "siswa");
                applyPeriodFilter(c, ApresiasiSiswa.class, null);
                if (currentSekolah != null) {
                    c.add(Restrictions.eq("sekolah", currentSekolah));
                }
                c.addOrder(Order.desc("waktu"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("ASRAMA".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(AsramaSiswaPunyaSiswa.class, "x");
                c.createAlias("siswa", "siswa");
                c.createAlias("asramaSiswa", "asrama");
                c.add(activeCriterion("aktif"));
                applySiswaAliasFilter(c, "siswa");
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("ORGANISASI".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(OrganisasiSiswaPunyaSiswa.class, "x");
                c.createAlias("siswa", "siswa");
                c.createAlias("organisasiSiswa", "organisasi");
                applySiswaAliasFilter(c, "siswa");
                applyPeriodFilter(c, OrganisasiSiswaPunyaSiswa.class, null);
                c.addOrder(Order.desc("mulai"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
        } catch (Exception e) {
            debugError("buildDetailList " + key, e);
        }
        return new ArrayList();
    }

    private List buildSiswaDetailList(String key) {
        List result = new ArrayList();
        try {
            if (currentKelas != null) {
                if ("SISWA_TANPA_KELAS".equals(key)) {
                    return result;
                }
                Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
                c.createAlias("siswa", "siswa");
                c.createAlias("kelasSiswa", "kelas");
                c.add(activeCriterion("aktif"));
                c.add(activeCriterion("kelas.aktif"));
                c.add(activeCriterion("siswa.aktif"));
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
                applySiswaAliasFilter(c, "siswa");
                if ("SISWA_EMAIL".equals(key)) {
                    addNotEmpty(c, "siswa.alamatEmail");
                }
                c.addOrder(Order.asc("nomorUrut"));
                c.setMaxResults(DETAIL_LIMIT);
                List kpsList = c.list();
                for (int i = 0; i < kpsList.size(); i++) {
                    Object siswa = getRawValueByPath(kpsList.get(i), "siswa");
                    if (siswa != null) {
                        result.add(siswa);
                    }
                }
                return result;
            }

            Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
            applySiswaAliasFilter(c, null);
            c.add(activeCriterion());
            if ("SISWA_EMAIL".equals(key)) {
                addNotEmpty(c, "alamatEmail");
            }
            if ("SISWA_TANPA_KELAS".equals(key)) {
                List ids = getSiswaIdsDenganKelas();
                if (ids != null && !ids.isEmpty()) {
                    c.add(Restrictions.not(Restrictions.in("id", ids)));
                }
            }
            c.addOrder(Order.asc("namaSiswa"));
            c.setMaxResults(DETAIL_LIMIT);
            result = c.list();
        } catch (Exception e) {
            debugError("buildSiswaDetailList", e);
        }
        return result;
    }

    private void openDetail(String key, String title) {
        try {
            Window window = new Window();
            window.setTitle(title == null || title.trim().length() == 0 ? "Detail Dasboard Siswa" : title);
            window.setWidth("88%");
            window.setHeight((desktopHeight == null ? 650 : Math.max(520, desktopHeight - 120)) + "px");
            window.setClosable(true);
            window.setSizable(true);
            window.setMaximizable(true);
            window.setBorder("normal");
            pasangPopupDetail(window);

            Div wrapper = new Div();
            wrapper.setParent(window);
            wrapper.setStyle("padding:12px; background:#f8fafc; height:100%; overflow:auto; box-sizing:border-box;");

            appendHtml(wrapper, "<div style='padding:12px; border-radius:14px; background:#ffffff; border:1px solid #e2e8f0; "
                    + "margin-bottom:10px; color:#475569;'>Menampilkan maksimal " + DETAIL_LIMIT
                    + " data pertama sesuai filter aktif. Gunakan menu asal untuk pencarian/ekspor penuh bila diperlukan.</div>");

            Grid grid = new Grid();
            grid.setParent(wrapper);
            grid.setMold("paging");
            grid.setPageSize(10);
            grid.setPagingPosition("bottom");
            grid.setWidth("100%");
            grid.setStyle("background:#ffffff; border:1px solid #e2e8f0;");

            Columns cols = new Columns();
            cols.setParent(grid);
            addColumn(cols, "No", "55px");
            String[] properties = detailProperties(key);
            String[] labels = detailLabels(key);
            for (int i = 0; i < labels.length; i++) {
                addColumn(cols, labels[i], null);
            }

            Rows rows = new Rows();
            rows.setParent(grid);
            List list = buildDetailList(key);
            if (list == null || list.isEmpty()) {
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label("-"));
                Cell cell = new Cell();
                cell.setColspan(labels.length);
                cell.setParent(row);
                cell.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data sesuai filter aktif.")));
            } else {
                for (int i = 0; i < list.size(); i++) {
                    Object object = list.get(i);
                    Row row = new Row();
                    row.setParent(rows);
                    row.setStyle(i % 2 == 0 ? "background:#ffffff;" : "background:#f8fafc;");
                    row.appendChild(new Label(String.valueOf(i + 1)));
                    for (int j = 0; j < properties.length; j++) {
                        row.appendChild(new Label(safeText(getValueByPath(object, properties[j]))));
                    }
                }
            }
            window.doModal();
        } catch (Exception e) {
            debugError("openDetail " + key, e);
        }
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

    private String[] detailLabels(String key) {
        if ("SISWA".equals(key) || "SISWA_EMAIL".equals(key) || "SISWA_TANPA_KELAS".equals(key)) {
            return new String[] { "NIS", "Nama", "Sekolah", "Gender", "Tahun", "HP", "Email" };
        }
        if ("PRESTASI".equals(key) || "PRESTASI_DISETUJUI".equals(key) || "PRESTASI_PENDING".equals(key)) {
            return new String[] { "Tanggal", "Siswa", "Prestasi", "Status", "Juara", "Penyelenggara", "Kelas" };
        }
        if ("PELANGGARAN".equals(key)) {
            return new String[] { "Waktu", "Siswa", "Jenis", "Keterangan", "Tampilkan Login" };
        }
        if ("APRESIASI".equals(key)) {
            return new String[] { "Waktu", "Siswa", "Jenis", "Keterangan", "Aktif" };
        }
        if ("ASRAMA".equals(key)) {
            return new String[] { "Asrama", "Siswa", "NIS", "Keterangan", "Aktif" };
        }
        if ("ORGANISASI".equals(key)) {
            return new String[] { "Organisasi", "Jabatan", "Siswa", "Mulai", "Sampai", "Disetujui" };
        }
        return new String[] { "Nama", "Siswa", "Keterangan", "Aktif" };
    }

    private String[] detailProperties(String key) {
        if ("SISWA".equals(key) || "SISWA_EMAIL".equals(key) || "SISWA_TANPA_KELAS".equals(key)) {
            return new String[] { "nomorInduk", "namaSiswa", "sekolah", "jenisKelamin", "tahunMasuk", "hp", "alamatEmail" };
        }
        if ("PRESTASI".equals(key) || "PRESTASI_DISETUJUI".equals(key) || "PRESTASI_PENDING".equals(key)) {
            return new String[] { "tanggal", "siswa.namaSiswa", "nama", "status", "juara", "penyelenggara", "kelasSiswa" };
        }
        if ("PELANGGARAN".equals(key)) {
            return new String[] { "waktu", "siswa.namaSiswa", "pelanggaranDanHukuman", "keterangan", "tampilkanInfoIniSaatSiswaLogin" };
        }
        if ("APRESIASI".equals(key)) {
            return new String[] { "waktu", "siswa.namaSiswa", "apresiasiDanPenghargaan", "keterangan", "aktif" };
        }
        if ("ASRAMA".equals(key)) {
            return new String[] { "asramaSiswa", "siswa.namaSiswa", "siswa.nomorInduk", "keterangan", "aktif" };
        }
        if ("ORGANISASI".equals(key)) {
            return new String[] { "organisasiSiswa", "jabatanOrganisasiSiswa", "siswa.namaSiswa", "mulai", "sampai", "persetujuan" };
        }
        return new String[] { "nama", "siswa", "keterangan", "aktif" };
    }

    private void addColumn(Columns cols, String label, String width) {
        Column col = new Column(label);
        col.setParent(cols);
        if (width != null) {
            col.setWidth(width);
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

    private void populateKelasCombo(Combobox combo, Sekolah sekolah) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua Kelas");
        semua.setValue(null);
        semua.setParent(combo);
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswa.class);
            c.add(activeCriterion());
            if (sekolah != null) {
                c.add(Restrictions.eq("sekolah", sekolah));
            }
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("tahunAjaran", tahunAjaran));
            }
            c.addOrder(Order.asc("tingkat"));
            c.addOrder(Order.asc("nama"));
            List list = c.list();
            for (int i = 0; i < list.size(); i++) {
                KelasSiswa kelas = (KelasSiswa) list.get(i);
                Comboitem item = new Comboitem(labelKelas(kelas));
                item.setValue(kelas);
                item.setParent(combo);
            }
        } catch (Exception e) {
            debugError("populateKelasCombo", e);
        }
        if (combo.getSelectedItem() == null && combo.getItems().size() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private void populateJenisKelaminCombo(Combobox combo) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua");
        semua.setValue(null);
        semua.setParent(combo);
        Comboitem l = new Comboitem("Laki-laki");
        l.setValue("Laki-laki");
        l.setParent(combo);
        Comboitem p = new Comboitem("Perempuan");
        p.setValue("Perempuan");
        p.setParent(combo);
        if (combo.getSelectedItem() == null && combo.getItems().size() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private void populateStatusSiswaCombo(Combobox combo) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua Status");
        semua.setValue(null);
        semua.setParent(combo);
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
            c.setProjection(Projections.distinct(Projections.property("statusSiswa")));
            List list = c.list();
            List<String> statuses = new ArrayList<String>();
            for (int i = 0; i < list.size(); i++) {
                String s = safeText(list.get(i));
                if (s.length() > 0 && !statuses.contains(s)) {
                    statuses.add(s);
                }
            }
            Collections.sort(statuses);
            for (int i = 0; i < statuses.size(); i++) {
                Comboitem item = new Comboitem(statuses.get(i));
                item.setValue(statuses.get(i));
                item.setParent(combo);
            }
        } catch (Exception e) {
            debugError("populateStatusSiswaCombo", e);
        }
        if (combo.getSelectedItem() == null && combo.getItems().size() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private Sekolah getSelectedSekolah() {
        try {
            if (searchSekolah != null && searchSekolah.getSelectedItem() != null) {
                Object val = searchSekolah.getSelectedItem().getValue();
                return val instanceof Sekolah ? (Sekolah) val : null;
            }
        } catch (Exception e) {
            debugError("getSelectedSekolah", e);
        }
        return currentSekolah;
    }

    private KelasSiswa getSelectedKelas() {
        try {
            if (searchKelas != null && searchKelas.getSelectedItem() != null) {
                Object val = searchKelas.getSelectedItem().getValue();
                return val instanceof KelasSiswa ? (KelasSiswa) val : null;
            }
        } catch (Exception e) {
            debugError("getSelectedKelas", e);
        }
        return currentKelas;
    }

    private String getSelectedString(Combobox combo, String currentValue) {
        try {
            if (combo != null && combo.getSelectedItem() != null) {
                Object val = combo.getSelectedItem().getValue();
                return val == null ? null : String.valueOf(val);
            }
        } catch (Exception e) {
            debugError("getSelectedString", e);
        }
        return currentValue;
    }

    private void selectComboByValue(Combobox combo, Object value) {
        if (combo == null) {
            return;
        }
        try {
            List items = combo.getItems();
            for (int i = 0; i < items.size(); i++) {
                Comboitem item = (Comboitem) items.get(i);
                Object itemValue = item.getValue();
                if (value == null && itemValue == null) {
                    combo.setSelectedItem(item);
                    return;
                }
                if (value != null && itemValue != null && sameEntity(value, itemValue)) {
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

    private boolean sameEntity(Object a, Object b) {
        if (a == b) {
            return true;
        }
        try {
            Object ida = getRawValueByPath(a, "id");
            Object idb = getRawValueByPath(b, "id");
            return ida != null && idb != null && ida.equals(idb);
        } catch (Exception e) {
            return a.equals(b);
        }
    }

    private List<String> buildTahunAjaranList(int mulai, int sampai) {
        Set<String> set = new HashSet<String>();
        for (int tahun = mulai; tahun <= sampai; tahun++) {
            set.add(tahun + "/" + (tahun + 1));
            set.add(tahun + "-" + (tahun + 1));
            set.add(String.valueOf(tahun));
        }
        List<String> list = new ArrayList<String>(set);
        Collections.sort(list);
        return list;
    }

    private String labelTahunAjaranFilter() {
        if (currentMulai == currentSampai) {
            return labelTahunAjaranSingkat(currentMulai);
        }
        return labelTahunAjaranSingkat(currentMulai) + " s.d. " + labelTahunAjaranSingkat(currentSampai);
    }

    private String labelTahunAjaranSingkat(int tahun) {
        return tahun + "/" + (tahun + 1);
    }

    private String labelSekolah(Sekolah sekolah) {
        String nama = safeText(getValueByPath(sekolah, "nama"));
        if (nama.length() == 0) {
            nama = safeText(sekolah);
        }
        return nama;
    }

    private String labelKelas(KelasSiswa kelas) {
        String nama = safeText(getValueByPath(kelas, "nama"));
        String tahun = safeText(getValueByPath(kelas, "tahunAjaran"));
        if (tahun.length() > 0) {
            return nama + " - " + tahun;
        }
        return nama;
    }

    private Date startDate(int tahun) {
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

    private Date endDate(int tahun) {
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

    private boolean hasProperty(Class clazz, String property) {
        return getReadMethod(clazz, property) != null;
    }

    private Method getReadMethod(Class clazz, String property) {
        if (clazz == null || property == null || property.trim().length() == 0) {
            return null;
        }
        String suffix = property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            return clazz.getMethod("get" + suffix, new Class[0]);
        } catch (Exception e) {
            try {
                return clazz.getMethod("is" + suffix, new Class[0]);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private Object getRawValueByPath(Object object, String path) {
        if (object == null || path == null || path.trim().length() == 0) {
            return null;
        }
        try {
            Object current = object;
            String[] parts = path.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                if (current == null) {
                    return null;
                }
                Method method = getReadMethod(current.getClass(), parts[i]);
                if (method == null) {
                    return null;
                }
                current = method.invoke(current, new Object[0]);
            }
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getValueByPath(Object object, String path) {
        return formatObject(getRawValueByPath(object, path));
    }

    private String formatObject(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof Date) {
            try {
                return Common.dateFormat3.get().format((Date) object);
            } catch (Exception e) {
                return object.toString();
            }
        }
        if (object instanceof Boolean) {
            return Boolean.TRUE.equals(object) ? "Ya/Aktif" : "Tidak";
        }
        return String.valueOf(object);
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

    private void appendMetricCard(Component parent, String title, long value, String subtitle, String color, String icon, final String detailKey) {
        appendValueMetricCard(parent, title, formatNumber(value), subtitle, color, icon, detailKey);
    }

    private void appendValueMetricCard(Component parent, String title, String value, String subtitle, String color, String icon, final String detailKey) {
        Div card = new Div();
        card.setParent(parent);
        card.setStyle("position:relative; overflow:hidden; padding:14px; border-radius:16px; background:#ffffff; "
                + "border:1px solid #e6edf5; box-shadow:0 10px 24px rgba(15,23,42,0.05); cursor:" + (detailKey == null ? "default" : "pointer") + ";");
        appendHtml(card, "<div style='display:flex; align-items:flex-start; justify-content:space-between; gap:10px;'>"
                + "<div><div style='font-size:12px; color:#64748b; font-weight:800;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:28px; color:" + color + "; font-weight:900; line-height:1.1; margin-top:6px;'>" + safeHtml(value) + "</div>"
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
                sb.append("<td style='padding:9px 12px; color:#4338ca; text-align:right; font-weight:900;'>").append(formatNumber(row.value)).append("</td></tr>");
            }
        }
        sb.append("</tbody></table></div>");
        appendHtml(parent, sb.toString());
    }

    private String sectionIntroHtml(String title, String desc) {
        return "<div style='margin-top:18px; padding:14px 16px; background:#ffffff; border:1px solid #e6edf5; border-radius:16px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.04);'>"
                + "<div style='font-size:17px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:#64748b; margin-top:4px; line-height:1.45;'>" + safeHtml(desc) + "</div></div>";
    }

    private String dashboardPanelHtml(String content) {
        return "<div style='background:#ffffff; border:1px solid #e6edf5; border-radius:16px; padding:14px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.05);'>" + content + "</div>";
    }

    private String dashboardExplainHtml(String text) {
        return "<div style='font-size:12px; line-height:1.55; color:#475569; margin-bottom:10px;'>" + safeHtml(text) + "</div>";
    }

    private String miniStatHtml(String label, long value, String desc) {
        return "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px; padding:9px 0; border-top:1px solid #eef2f7;'>"
                + "<div><div style='font-size:12px; font-weight:800; color:#0f172a;'>" + safeHtml(label) + "</div>"
                + "<div style='font-size:11px; color:#94a3b8;'>" + safeHtml(desc) + "</div></div>"
                + "<div style='font-size:18px; font-weight:900; color:#4338ca;'>" + formatNumber(value) + "</div></div>";
    }

    private String progressSectionHtml(String title, long total, DashboardMiniRow r1, DashboardMiniRow r2, DashboardMiniRow r3, DashboardMiniRow r4) {
        DashboardMiniRow[] rows = new DashboardMiniRow[] { r1, r2, r3, r4 };
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:13px; font-weight:900; color:#0f172a; margin-bottom:8px;'>").append(safeHtml(title)).append("</div>");
        long denominator = total <= 0 ? 1 : total;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == null) {
                continue;
            }
            int pct = percent(rows[i].value, denominator);
            if (pct > 100) {
                pct = 100;
            }
            sb.append("<div style='margin-top:8px;'>");
            sb.append("<div style='display:flex; justify-content:space-between; font-size:11px; color:#475569;'>")
                    .append("<span>").append(safeHtml(rows[i].label)).append("</span><b>").append(formatNumber(rows[i].value)).append("</b></div>");
            sb.append("<div style='height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden; margin-top:4px;'>")
                    .append("<div style='height:9px; width:").append(pct).append("%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>");
            sb.append("</div>");
        }
        return sb.toString();
    }

    private String badgeHtml(String text, String background, String color) {
        return "<span style='display:inline-block; padding:7px 10px; border-radius:999px; background:" + background
                + "; color:" + color + "; font-size:11px; font-weight:800;'>" + safeHtml(text) + "</span>";
    }

    private String recommendationCardHtml(String title, String status, String desc, String color) {
        return "<div style='background:#ffffff; border:1px solid #e6edf5; border-radius:16px; padding:14px; "
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

    private String formatNumber(long number) {
        try {
            return Common.numberFormat.get().format(number);
        } catch (Exception e) {
            return String.valueOf(number);
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
            System.err.println("[DasboardSiswa DEBUG] " + context);
            if (e != null) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasboardSiswa.java:1872");
            }
        }
    }

    /**
     * Tipe implementasi bersarang {@link DashboardSiswaData} milik {@link DasboardSiswa}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSiswa}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int tahunMulai}, {@code int
     * tahunSampai}, {@code String tahunAjaranLabel}, {@code long totalSiswa}, {@code long totalSiswaSemua}, {@code
     * long totalSiswaNonAktif}, {@code long totalLakiLaki}, {@code long totalPerempuan}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DasboardSiswa
     */
    private static class DashboardSiswaData {
        int tahunMulai;
        int tahunSampai;
        String tahunAjaranLabel;

        long totalSiswa;
        long totalSiswaSemua;
        long totalSiswaNonAktif;
        long totalLakiLaki;
        long totalPerempuan;
        long totalSiswaDenganKelas;
        long totalSiswaTanpaKelas;

        long totalEmailSiswa;
        long totalHpSiswa;
        long totalTeleponSiswa;
        long totalTeleponOrtu;
        long totalAlamatSiswa;

        long totalPrestasi;
        long totalPrestasiDisetujui;
        long totalPrestasiDitolak;
        long totalPrestasiBelum;
        long totalPrestasiSedang;
        long totalPrestasiLuar;
        long totalPelanggaran;
        long totalApresiasi;

        long totalAsrama;
        long totalPenghuniAsrama;
        long totalOrganisasi;
        long totalAnggotaOrganisasi;
        long totalAnggotaOrganisasiDisetujui;

        int rasioKelas;
        int rasioEmail;
        int rasioHpOrtu;
        int rasioApresiasiPelanggaran;
        int rasioPrestasiDisetujui;

        List<DashboardMiniRow> topKelasPeserta = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topAsramaPenghuni = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topOrganisasiAnggota = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPrestasiStatus = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPrestasiKategori = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topApresiasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPelanggaran = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendSiswa = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPrestasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendApresiasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPelanggaran = new ArrayList<DashboardMiniRow>();
    }

    /**
     * Pembawa data/helper lokal milik {@link DasboardSiswa} untuk dashboard mini row. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSiswa}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code long value}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DasboardSiswa
     */
    private static class DashboardMiniRow {
        String label;
        long value;

        DashboardMiniRow(String label, long value) {
            this.label = label == null || label.trim().length() == 0 ? "Tidak Diisi" : label;
            this.value = value;
        }
    }
}
