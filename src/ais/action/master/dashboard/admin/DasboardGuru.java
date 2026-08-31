package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASBOARD_GURU_2026_05_30
 * Dashboard khusus guru berbasis template DasborAkademikSekolah.
 * Fokus data: profil guru, jenis guru, kelengkapan data kontak/identitas,
 * jadwal pelajaran, penugasan mengajar, catatan guru, prestasi guru, dan absen guru piket.
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
import ais.database.model.sekolah.AbsenGuruPiket;
import ais.database.model.sekolah.CatatanGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisCatatanGuru;
import ais.database.model.sekolah.JenisGuru;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.database.model.sekolah.PrestasiGuru;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Komponen dashboard khusus untuk dasboard guru. Kelas ini memilih variasi data atau tampilan
 * dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyPortallayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code java.util.concurrent.ConcurrentHashMap
 * _CACHE}, {@code java.util.concurrent.ConcurrentHashMap _EXPIRY}, {@code long _TTL_MS}, {@code int TOP_LIMIT},
 * {@code int DETAIL_LIMIT}, {@code boolean debug}, {@code Tbmuser tbmuser}, {@code Integer desktopHeight};
 * inisialisasi/lifecycle ({@code init()}, {@code initDefaultFilterValue()}); pembacaan/pencarian ({@code
 * renderDashboardContentDenganLoading()}, {@code tampilkanLoadingDashboardGuru()}, {@code
 * hapusLoadingDashboardGuru()}, {@code buildLoadingDashboardHtml()}, {@code loadDashboardDataCached()}, {@code
 * loadDashboardData()}); mutasi data ({@code setDebug()}, {@code updateDashboardProgress()}); pelaporan/ekspor
 * ({@code renderContent()}, {@code renderHero()}, {@code renderFilter()}, {@code renderOverview()}, {@code
 * renderProfilDanKelengkapan()}, {@code renderAktivitasMengajar()}); operasi domain lain ({@code isDebug()},
 * {@code buildCacheKey()}, {@code countGuru()}, {@code countGeneric()}, {@code countEntityByGuru()}, {@code
 * countEntityByGuru()}); konfigurasi constructor: {@code tbmuser}. Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyPortallayout
 */
public class DasboardGuru extends MyPortallayout {

    private static final long serialVersionUID = 20260530180501L;
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
    private Combobox searchJenisGuru;
    private Combobox searchJenisKelamin;
    private Combobox searchSemester;

    private int currentMulai;
    private int currentSampai;
    private Sekolah currentSekolah;
    private JenisGuru currentJenisGuru;
    private String currentJenisKelamin;
    private Integer currentSemester;

    public DasboardGuru() throws Exception {
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
        DashboardGridExportHelper.pasang(this, "Guru");
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
        panel.setTitle("Dasboard Guru");
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
        currentSampai = tahunSekarang;
        currentMulai = tahunSekarang - 2;
        currentJenisKelamin = null;
        currentSemester = null;
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
        currentJenisGuru = getSelectedJenisGuru();
        currentJenisKelamin = getSelectedString(searchJenisKelamin, currentJenisKelamin);
        currentSemester = getSelectedSemester();

        renderHero();
        renderFilter();

        final int loadVersion = ++dashboardLoadVersion;
        tampilkanLoadingDashboardGuru("Menyiapkan parameter filter dan tampilan awal...", 2);

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
            updateDashboardProgress("Mengambil ringkasan profil guru dan kelengkapan data...", 8);
            DashboardGuruData data = loadDashboardDataCached();
            if (loadVersion != dashboardLoadVersion) {
                return;
            }
            updateDashboardProgress("Menyusun kartu, panel analitik, tabel top data, tren, dan rekomendasi...", 96);
            hapusLoadingDashboardGuru();

            renderOverview(data);
            renderProfilDanKelengkapan(data);
            renderAktivitasMengajar(data);
            renderTopTablesAndTrends(data);
            renderRecommendation(data);
        } catch (Exception e) {
            debugError("renderDashboardContentDenganLoading", e);
            hapusLoadingDashboardGuru();
            appendHtml(body, "<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; "
                    + "color:#991b1b; border:1px solid #fecdd3; font-weight:700;'>Dasboard Guru belum dapat dimuat. "
                    + "Silakan tekan Refresh atau aktifkan debug untuk melihat detail error.</div>");
        }
    }

    private void tampilkanLoadingDashboardGuru(String pesan, int persen) {
        if (body == null) {
            return;
        }
        hapusLoadingDashboardGuru();
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

    private void hapusLoadingDashboardGuru() {
        if (loadingDashboardContainer != null) {
            try {
                if (loadingDashboardContainer.getParent() != null) {
                    loadingDashboardContainer.detach();
                }
            } catch (Exception e) {
                debugError("hapusLoadingDashboardGuru", e);
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
        return "<div style='padding:18px; border-radius:16px; border:1px solid #dbeafe; background:#ffffff; "
                + "box-shadow:0 8px 22px rgba(15,23,42,0.06);'>"
                + "<div style='display:flex; align-items:center; justify-content:space-between; gap:12px;'>"
                + "<div style='font-weight:800; color:#1e3a8a; font-size:14px;'><i class=\"fa fa-spinner fa-spin\"></i> "
                + escapeHtml(pesan) + "</div>"
                + "<div style='font-weight:900; color:#2563eb; font-size:16px;'>" + progress + "%</div>"
                + "</div>"
                + "<div style='margin-top:12px; width:100%; height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
                + "<div style='width:" + progress + "%; height:12px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); "
                + "border-radius:999px; transition:width .25s ease;'></div>"
                + "</div>"
                + "<div style='margin-top:8px; color:#64748b; font-size:12px;'>Mohon menunggu, sistem sedang mengambil dan mengolah data guru sesuai filter.</div>"
                + "</div>";
    }

    private void renderHero() {
        String filterInfo = "Periode " + currentMulai + " - " + currentSampai;
        if (currentSekolah != null) {
            filterInfo += " • " + safeName(currentSekolah);
        }
        if (currentJenisGuru != null) {
            filterInfo += " • " + safeName(currentJenisGuru);
        }
        if (currentJenisKelamin != null && !currentJenisKelamin.trim().isEmpty()) {
            filterInfo += " • " + currentJenisKelamin;
        }
        if (currentSemester != null) {
            filterInfo += " • Semester " + currentSemester;
        }

        appendHtml(body, "<div style='border-radius:20px; padding:20px; color:#fff; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
                + "box-shadow:0 18px 40px rgba(30,64,175,0.25); overflow:hidden; position:relative;'>"
                + "<div style='position:absolute; right:-55px; top:-55px; width:190px; height:190px; border-radius:999px; background:rgba(255,255,255,0.12);'></div>"
                + "<div style='position:absolute; right:105px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(20,184,166,0.22);'></div>"
                + "<div style='position:relative; z-index:1;'>"
                + "<div style='font-size:13px; letter-spacing:.08em; text-transform:uppercase; opacity:.86; font-weight:800;'>Dashboard Operasional Guru</div>"
                + "<div style='font-size:29px; line-height:1.2; font-weight:900; margin-top:8px;'>Dasboard Guru</div>"
                + "<div style='max-width:880px; margin-top:8px; font-size:14px; line-height:1.7; opacity:.92;'>"
                + "Memantau profil guru, kelengkapan data identitas, penugasan mengajar, jadwal pelajaran, prestasi, catatan, dan piket guru dalam satu tampilan ringkas."
                + "</div>"
                + "<div style='margin-top:14px; display:inline-block; padding:8px 12px; border-radius:999px; background:rgba(255,255,255,0.16); "
                + "font-weight:800; font-size:12px;'>" + escapeHtml(filterInfo) + "</div>"
                + "</div></div>");
    }

    private void renderFilter() {
        Div filterBox = new Div();
        filterBox.setParent(body);
        filterBox.setStyle("margin-top:14px; padding:12px; border-radius:16px; border:1px solid #e2e8f0; "
                + "background:#ffffff; box-shadow:0 8px 22px rgba(15,23,42,0.05);");

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(filterBox);
        toolbar.setStyle("border:none; background:transparent; padding:0; display:flex; flex-wrap:wrap; gap:8px; align-items:center;");

        appendToolbarLabel(toolbar, "Mulai Tahun");
        mulaiTahun = new Intbox(currentMulai);
        mulaiTahun.setParent(toolbar);
        mulaiTahun.setWidth("85px");

        appendToolbarLabel(toolbar, "Sampai Tahun");
        sampaiTahun = new Intbox(currentSampai);
        sampaiTahun.setParent(toolbar);
        sampaiTahun.setWidth("85px");

        appendToolbarLabel(toolbar, "Sekolah");
        searchSekolah = new Combobox();
        searchSekolah.setParent(toolbar);
        searchSekolah.setWidth("230px");
        searchSekolah.setReadonly(true);
        populateSekolahCombobox();

        appendToolbarLabel(toolbar, "Jenis Guru");
        searchJenisGuru = new Combobox();
        searchJenisGuru.setParent(toolbar);
        searchJenisGuru.setWidth("210px");
        searchJenisGuru.setReadonly(true);
        populateJenisGuruCombobox();

        appendToolbarLabel(toolbar, "Gender");
        searchJenisKelamin = new Combobox();
        searchJenisKelamin.setParent(toolbar);
        searchJenisKelamin.setWidth("135px");
        searchJenisKelamin.setReadonly(true);
        populateStringCombobox(searchJenisKelamin, new String[] { "Semua", "Laki-laki", "Perempuan" }, currentJenisKelamin);

        appendToolbarLabel(toolbar, "Semester");
        searchSemester = new Combobox();
        searchSemester.setParent(toolbar);
        searchSemester.setWidth("125px");
        searchSemester.setReadonly(true);
        populateSemesterCombobox();

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig();
        refresh.setParent(toolbar);
        refresh.setLabel("Refresh");
        refresh.setStyle("margin-left:6px; font-weight:700; border-radius:10px; padding:6px 10px;");
        refresh.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                renderContent();
            }
        });
    }

    private String buildCacheKey() {
        return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
                + "|" + currentMulai + "|" + currentSampai
                + "|" + (currentSekolah == null ? "" : currentSekolah.getId())
                + "|" + (currentJenisGuru == null ? "" : currentJenisGuru.getId())
                + "|" + (currentJenisKelamin == null ? "" : currentJenisKelamin)
                + "|" + (currentSemester == null ? "" : currentSemester);
    }

    @SuppressWarnings("unchecked")
    private DashboardGuruData loadDashboardDataCached() throws Exception {
        String _k = buildCacheKey();
        Long _e = _EXPIRY.get(_k);
        if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
            return (DashboardGuruData) _CACHE.get(_k);
        }
        DashboardGuruData data = loadDashboardData();
        _CACHE.put(_k, data);
        _EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
        return data;
    }

    private DashboardGuruData loadDashboardData() throws Exception {
        DashboardGuruData data = new DashboardGuruData();

        updateDashboardProgress("Mengambil data master guru aktif, nonaktif, dan komposisi gender...", 14);
        data.totalGuru = countGuru(Boolean.TRUE, null, null, null, false);
        data.totalGuruSemua = countGuru(null, null, null, null, false);
        data.totalGuruNonAktif = countGuru(Boolean.FALSE, null, null, null, false);
        data.totalLakiLaki = countGuru(Boolean.TRUE, "jenisKelamin", "Laki-laki", null, false);
        data.totalPerempuan = countGuru(Boolean.TRUE, "jenisKelamin", "Perempuan", null, false);
        data.totalJenisGuru = countGeneric(JenisGuru.class, true);

        updateDashboardProgress("Mengambil kelengkapan kontak dan identitas guru...", 28);
        data.totalEmailGuru = countGuru(Boolean.TRUE, null, null, "alamatEmail", false);
        data.totalHpGuru = countGuru(Boolean.TRUE, null, null, "hp", false);
        data.totalTeleponGuru = countGuru(Boolean.TRUE, null, null, "teleponGuru", false);
        data.totalAlamatGuru = countGuru(Boolean.TRUE, null, null, "alamatGuru", false);
        data.totalNuptkGuru = countGuru(Boolean.TRUE, null, null, "nuptk", false);
        data.totalNipGuru = countGuru(Boolean.TRUE, null, null, "nip", false);
        data.totalNikGuru = countGuru(Boolean.TRUE, null, null, "nik", false);
        data.totalPegawaiLink = countGuru(Boolean.TRUE, null, null, "pegawaiId", true);

        updateDashboardProgress("Mengambil jadwal pelajaran dan slot guru mengajar...", 44);
        data.totalJadwalPelajaran = countJadwalPelajaran();
        data.totalSlotMengajar = countSlotGuruJadwal();
        data.totalGuruTerjadwal = countDistinctGuruJadwal();
        data.totalGuruTanpaJadwal = data.totalGuru - data.totalGuruTerjadwal;
        if (data.totalGuruTanpaJadwal < 0) {
            data.totalGuruTanpaJadwal = 0;
        }

        updateDashboardProgress("Mengambil penugasan, prestasi, catatan, dan absensi guru piket...", 62);
        data.totalPenugasan = countEntityByGuru(PenugasanGuruMengajar.class, "tahunAkademik", "semester", true);
        data.totalPrestasi = countEntityByGuru(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true);
        data.totalPrestasiDisetujui = countEntityByGuru(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true, "status", PrestasiGuru.DISETUJUI);
        data.totalPrestasiDitolak = countEntityByGuru(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true, "status", PrestasiGuru.DITOLAK);
        data.totalPrestasiBelum = countEntityByGuru(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true, "status", PrestasiGuru.BELUM_DIPROSES);
        data.totalPrestasiSedang = countEntityByGuru(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true, "status", PrestasiGuru.SEDANG_DIPROSES);
        data.totalPrestasiLuar = countEntityByGuru(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true, "prestasiLuarKampus", Boolean.TRUE);
        data.totalCatatan = countEntityByGuru(CatatanGuru.class, "tahunAjaran", "semester", false);
        data.totalJenisCatatan = countGeneric(JenisCatatanGuru.class, true);
        data.totalPiket = countAbsenPiket();
        data.totalSlotPiket = countSlotGuruPiket();
        data.totalGuruPiket = countDistinctGuruPiket();

        updateDashboardProgress("Menghitung rasio kesiapan data dan aktivitas guru...", 74);
        data.rasioGuruTerjadwal = percent(data.totalGuruTerjadwal, data.totalGuru);
        data.rasioEmail = percent(data.totalEmailGuru, data.totalGuru);
        data.rasioHp = percent(data.totalHpGuru + data.totalTeleponGuru, data.totalGuru);
        data.rasioNuptk = percent(data.totalNuptkGuru, data.totalGuru);
        data.rasioPrestasiDisetujui = percent(data.totalPrestasiDisetujui, data.totalPrestasi);
        data.rasioPiket = percent(data.totalGuruPiket, data.totalGuru);

        updateDashboardProgress("Menyusun tabel top data, distribusi, dan tren tahunan...", 86);
        data.topJenisGuru = loadTopGuruAssociation("jenisGuru", "nama", "Jenis Guru");
        data.topStatusKepegawaian = loadTopGuruAssociation("statusKepegawaian", "nama", "Status Kepegawaian");
        data.topGuruJadwal = loadTopGuruFromSlotTable("sekolah.jadwal_pelajaran", JADWAL_GURU_COLUMNS, "tahun_ajaran", "semester");
        data.topGuruPiket = loadTopGuruFromSlotTable("sekolah.absen_guru_piket", PIKET_GURU_COLUMNS, "tahun_ajaran", "semester");
        data.topPrestasiStatus = loadTopGroupByGuru(PrestasiGuru.class, "status", "Status Belum Diisi", "tahunAkademik", "jenisSemester", true);
        data.topPrestasiKategori = loadTopAssociationByGuru(PrestasiGuru.class, "kategoriPrestasiGuru", "nama", "Kategori Prestasi", "tahunAkademik", "jenisSemester", true);
        data.topCatatanJenis = loadTopAssociationByGuru(CatatanGuru.class, "jenisCatatanGuru", "nama", "Jenis Catatan", "tahunAjaran", "semester", false);
        data.topGuruPrestasi = loadTopGuruEntity(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true);
        data.topGuruCatatan = loadTopGuruEntity(CatatanGuru.class, "tahunAjaran", "semester", false);
        data.trendPenugasan = loadTrendEntity(PenugasanGuruMengajar.class, "tahunAkademik", "semester", true);
        data.trendPrestasi = loadTrendEntity(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true);
        data.trendCatatan = loadTrendEntity(CatatanGuru.class, "tahunAjaran", "semester", false);
        data.trendPiket = loadTrendAbsenPiket();

        sortAndLimit(data.topJenisGuru, TOP_LIMIT);
        sortAndLimit(data.topStatusKepegawaian, TOP_LIMIT);
        sortAndLimit(data.topGuruJadwal, TOP_LIMIT);
        sortAndLimit(data.topGuruPiket, TOP_LIMIT);
        sortAndLimit(data.topPrestasiStatus, TOP_LIMIT);
        sortAndLimit(data.topPrestasiKategori, TOP_LIMIT);
        sortAndLimit(data.topCatatanJenis, TOP_LIMIT);
        sortAndLimit(data.topGuruPrestasi, TOP_LIMIT);
        sortAndLimit(data.topGuruCatatan, TOP_LIMIT);

        return data;
    }

    private void renderOverview(DashboardGuruData data) {
        appendHtml(body, sectionIntroHtml("Ringkasan Utama Guru", "Klik angka pada kartu untuk membuka contoh detail data sesuai filter aktif. Data disusun untuk memantau profil, jadwal, penugasan, prestasi, catatan, dan piket guru."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");

        appendMetricCard(grid, "Guru Aktif", data.totalGuru, "Guru aktif sesuai filter", "#4338ca", "fa-users", "GURU_AKTIF");
        appendMetricCard(grid, "Belum Terjadwal", data.totalGuruTanpaJadwal, data.rasioGuruTerjadwal + "% guru sudah memiliki slot jadwal", "#ea580c", "fa-exclamation-triangle", "GURU_TANPA_JADWAL");
        appendMetricCard(grid, "Slot Mengajar", data.totalSlotMengajar, data.totalJadwalPelajaran + " jadwal pelajaran", "#0f766e", "fa-calendar", "JADWAL");
        appendMetricCard(grid, "Penugasan", data.totalPenugasan, "Surat/riwayat penugasan mengajar", "#0369a1", "fa-briefcase", "PENUGASAN");
        appendMetricCard(grid, "Prestasi Guru", data.totalPrestasi, data.rasioPrestasiDisetujui + "% disetujui", "#7c3aed", "fa-trophy", "PRESTASI");
        appendMetricCard(grid, "Catatan Guru", data.totalCatatan, data.totalJenisCatatan + " jenis catatan", "#be123c", "fa-sticky-note", "CATATAN");
        appendMetricCard(grid, "Guru Piket", data.totalGuruPiket, data.totalSlotPiket + " slot piket", "#0891b2", "fa-check-square", "PIKET");
        appendMetricCard(grid, "Email Terisi", data.totalEmailGuru, data.rasioEmail + "% dari guru aktif", "#16a34a", "fa-envelope", "GURU_EMAIL");
    }

    private void renderProfilDanKelengkapan(DashboardGuruData data) {
        appendHtml(body, sectionIntroHtml("Profil & Kelengkapan Data Guru", "Bagian ini membantu operator memeriksa master guru, identitas, dan kesiapan kontak untuk notifikasi akademik maupun administrasi."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(300px,1fr)); gap:12px;");

        String gender = dashboardPanelHtml(
                dashboardExplainHtml("Komposisi jenis kelamin dihitung dari guru aktif sesuai filter. Bila angka tidak wajar, cek kembali penulisan gender pada master guru.")
                + progressSectionHtml("Komposisi Gender", data.totalGuru,
                        new DashboardMiniRow("Laki-laki", data.totalLakiLaki),
                        new DashboardMiniRow("Perempuan", data.totalPerempuan),
                        new DashboardMiniRow("Tidak/Belum Sesuai", data.totalGuru - data.totalLakiLaki - data.totalPerempuan), null));
        appendHtml(grid, gender);

        String kontak = dashboardPanelHtml(
                dashboardExplainHtml("Kelengkapan kontak penting untuk notifikasi jadwal, catatan, penugasan, rapat, dan komunikasi akademik.")
                + progressSectionHtml("Kesiapan Kontak", data.totalGuru,
                        new DashboardMiniRow("Email Guru", data.totalEmailGuru),
                        new DashboardMiniRow("HP/Telepon Guru", data.totalHpGuru + data.totalTeleponGuru),
                        new DashboardMiniRow("Alamat Guru", data.totalAlamatGuru),
                        new DashboardMiniRow("Tautan Pegawai", data.totalPegawaiLink)));
        appendHtml(grid, kontak);

        String identitas = dashboardPanelHtml(
                dashboardExplainHtml("NUPTK, NIP, dan NIK membantu validasi administrasi, pelaporan, dan integrasi data kepegawaian.")
                + miniStatHtml("NUPTK Terisi", data.totalNuptkGuru, data.rasioNuptk + "% dari guru aktif")
                + miniStatHtml("NIP Terisi", data.totalNipGuru, "Untuk ASN/pegawai yang memiliki NIP")
                + miniStatHtml("NIK Terisi", data.totalNikGuru, "Kelengkapan identitas kependudukan")
                + miniStatHtml("Guru nonaktif", data.totalGuruNonAktif, "Di luar indikator guru aktif utama"));
        appendHtml(grid, identitas);
    }

    private void renderAktivitasMengajar(DashboardGuruData data) {
        appendHtml(body, sectionIntroHtml("Aktivitas Mengajar & Pembinaan", "Perbandingan jadwal, penugasan, prestasi, catatan, dan piket memberi gambaran pembinaan serta distribusi beban guru."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(300px,1fr)); gap:12px;");

        String jadwal = dashboardPanelHtml(
                dashboardExplainHtml("Guru terjadwal dihitung dari slot guru pada jadwal pelajaran. Slot mengajar dapat lebih besar dari jumlah jadwal karena satu jadwal bisa memiliki beberapa guru.")
                + miniStatHtml("Jadwal Pelajaran", data.totalJadwalPelajaran, "Jumlah baris jadwal sesuai periode")
                + miniStatHtml("Slot Mengajar", data.totalSlotMengajar, "Akumulasi guru pada kolom guru1 s.d. guru12")
                + miniStatHtml("Guru Terjadwal", data.totalGuruTerjadwal, data.rasioGuruTerjadwal + "% dari guru aktif")
                + miniStatHtml("Belum Terjadwal", data.totalGuruTanpaJadwal, "Perlu validasi jadwal atau status guru"));
        appendHtml(grid, jadwal);

        String prestasi = dashboardPanelHtml(
                dashboardExplainHtml("Pantau status prestasi guru agar bukti kinerja, penghargaan, dan dokumen pendukung tidak tertahan terlalu lama.")
                + progressSectionHtml("Status Prestasi Guru", data.totalPrestasi,
                        new DashboardMiniRow("Disetujui", data.totalPrestasiDisetujui),
                        new DashboardMiniRow("Sedang Diproses", data.totalPrestasiSedang),
                        new DashboardMiniRow("Belum Diproses", data.totalPrestasiBelum),
                        new DashboardMiniRow("Ditolak", data.totalPrestasiDitolak)));
        appendHtml(grid, prestasi);

        String piket = dashboardPanelHtml(
                dashboardExplainHtml("Absen guru piket membantu melihat keterlibatan guru dalam pengawasan harian dan layanan sekolah.")
                + miniStatHtml("Data Piket", data.totalPiket, "Jumlah catatan absen guru piket")
                + miniStatHtml("Slot Piket", data.totalSlotPiket, "Akumulasi guru pada kolom guru1 s.d. guru5")
                + miniStatHtml("Guru Terlibat Piket", data.totalGuruPiket, data.rasioPiket + "% dari guru aktif")
                + miniStatHtml("Penugasan Mengajar", data.totalPenugasan, "Riwayat surat/penugasan guru"));
        appendHtml(grid, piket);
    }

    private void renderTopTablesAndTrends(DashboardGuruData data) {
        appendHtml(body, sectionIntroHtml("Top Data & Tren", "Tabel berikut menampilkan konsentrasi data utama untuk membantu kepala sekolah/operator menentukan prioritas pembenahan."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(340px,1fr)); gap:12px;");

        appendTopTable(grid, "Top Jenis Guru", "Distribusi guru berdasarkan jenis guru.", data.topJenisGuru);
        appendTopTable(grid, "Top Status Kepegawaian", "Distribusi status kepegawaian guru.", data.topStatusKepegawaian);
        appendTopTable(grid, "Top Guru Berdasarkan Jadwal", "Guru dengan slot jadwal pelajaran terbanyak.", data.topGuruJadwal);
        appendTopTable(grid, "Top Guru Piket", "Guru dengan slot piket terbanyak.", data.topGuruPiket);
        appendTopTable(grid, "Top Status Prestasi", "Distribusi status proses prestasi guru.", data.topPrestasiStatus);
        appendTopTable(grid, "Top Kategori Prestasi", "Kategori prestasi guru paling sering tercatat.", data.topPrestasiKategori);
        appendTopTable(grid, "Top Jenis Catatan Guru", "Jenis catatan guru paling sering digunakan.", data.topCatatanJenis);
        appendTopTable(grid, "Top Guru Berprestasi", "Guru dengan catatan prestasi terbanyak.", data.topGuruPrestasi);
        appendTopTable(grid, "Top Guru Dengan Catatan", "Guru dengan catatan terbanyak.", data.topGuruCatatan);

        appendTrendPanel(grid, "Tren Penugasan Mengajar", "Jumlah penugasan guru per tahun ajaran.", data.trendPenugasan);
        appendTrendPanel(grid, "Tren Prestasi Guru", "Jumlah prestasi guru per tahun ajaran.", data.trendPrestasi);
        appendTrendPanel(grid, "Tren Catatan Guru", "Jumlah catatan guru per tahun ajaran.", data.trendCatatan);
        appendTrendPanel(grid, "Tren Absen Guru Piket", "Jumlah data piket guru per tahun ajaran.", data.trendPiket);
    }

    private void renderRecommendation(DashboardGuruData data) {
        appendHtml(body, sectionIntroHtml("Rekomendasi Tindak Lanjut", "Rekomendasi otomatis ini bersifat operasional untuk mempercepat validasi data guru."));
        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("margin-top:12px; display:grid; grid-template-columns:repeat(auto-fit,minmax(290px,1fr)); gap:12px;");

        String rekom1 = "Jika guru belum terjadwal masih tinggi, cek status aktif guru, relasi sekolah, tahun ajaran jadwal, dan kelengkapan mapping jadwal pelajaran.";
        if (data.totalGuru > 0 && data.rasioGuruTerjadwal >= 90) {
            rekom1 = "Distribusi guru terjadwal sudah baik. Tetap lakukan pengecekan guru pengganti, jadwal ganda, dan bentrok jam mengajar.";
        }
        appendRecommendationCard(grid, "Validasi Jadwal Mengajar", rekom1, "fa-calendar-check-o");

        String rekom2 = "Lengkapi email, nomor HP, NUPTK, NIP/NIK, dan tautan pegawai untuk memperkuat notifikasi serta integrasi administrasi.";
        if (data.rasioEmail >= 85 && data.rasioHp >= 85) {
            rekom2 = "Kelengkapan kontak sudah cukup kuat. Prioritaskan validasi identitas seperti NUPTK, NIP, NIK, dan data kepegawaian.";
        }
        appendRecommendationCard(grid, "Perapihan Master Guru", rekom2, "fa-address-card");

        String rekom3 = "Tindak lanjuti prestasi berstatus belum/sedang diproses agar rekam jejak kinerja guru dapat dipublikasikan dan dilaporkan tepat waktu.";
        if (data.totalPrestasi > 0 && data.rasioPrestasiDisetujui >= 80) {
            rekom3 = "Mayoritas prestasi guru sudah disetujui. Pertahankan alur verifikasi dan dorong guru mengunggah bukti prestasi secara rutin.";
        }
        appendRecommendationCard(grid, "Monitoring Prestasi", rekom3, "fa-trophy");

        String rekom4 = "Gunakan catatan guru dan piket sebagai bahan pembinaan, evaluasi kedisiplinan, dan pemerataan tugas non-mengajar.";
        appendRecommendationCard(grid, "Pembinaan & Piket", rekom4, "fa-clipboard");
    }

    private long countGuru(Boolean aktif, String eqProperty, Object eqValue, String filledProperty, boolean numericNotNull) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Guru.class);
            applyGuruFilter(c, aktif);
            if (eqProperty != null && eqValue != null) {
                c.add(Restrictions.eq(eqProperty, eqValue));
            }
            if (filledProperty != null && !filledProperty.trim().isEmpty()) {
                c.add(Restrictions.isNotNull(filledProperty));
                if (!numericNotNull) {
                    c.add(Restrictions.ne(filledProperty, ""));
                }
            }
            Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countGuru", e);
            return 0;
        }
    }

    private long countGeneric(Class clazz, boolean aktifAware) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            if (aktifAware && propertyExists(clazz, "aktif")) {
                c.add(activeCriterion());
            }
            if (currentSekolah != null && propertyExists(clazz, "sekolah")) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countGeneric " + clazz, e);
            return 0;
        }
    }

    private long countEntityByGuru(Class clazz, String tahunProperty, String semesterProperty, boolean semesterString) {
        return countEntityByGuru(clazz, tahunProperty, semesterProperty, semesterString, null, null);
    }

    private long countEntityByGuru(Class clazz, String tahunProperty, String semesterProperty, boolean semesterString, String eqProperty, Object eqValue) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            applyEntityGuruFilter(c, clazz, tahunProperty, semesterProperty, semesterString);
            if (eqProperty != null && eqValue != null) {
                c.add(Restrictions.eq(eqProperty, eqValue));
            }
            Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countEntityByGuru " + clazz, e);
            return 0;
        }
    }

    private long countJadwalPelajaran() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class);
            applyTahunAjaranSemesterSekolahFilter(c, JadwalPelajaran.class, "tahunAjaran", "semester", false);
            Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countJadwalPelajaran", e);
            return 0;
        }
    }

    private long countAbsenPiket() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(AbsenGuruPiket.class);
            applyTahunAjaranSemesterSekolahFilter(c, AbsenGuruPiket.class, "tahunAjaran", "semester", false);
            Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countAbsenPiket", e);
            return 0;
        }
    }

    private static final String[] JADWAL_GURU_COLUMNS = new String[] { "guru_id", "guru2_id", "guru3_id", "guru4_id", "guru5_id", "guru6_id", "guru7_id", "guru8_id", "guru9_id", "guru10_id", "guru11_id", "guru12_id" };
    private static final String[] PIKET_GURU_COLUMNS = new String[] { "guru_id", "guru2_id", "guru3_id", "guru4_id", "guru5_id" };

    private long countSlotGuruJadwal() {
        return countSlotGuruFromTable("sekolah.jadwal_pelajaran", JADWAL_GURU_COLUMNS, "tahun_ajaran", "semester");
    }

    private long countSlotGuruPiket() {
        return countSlotGuruFromTable("sekolah.absen_guru_piket", PIKET_GURU_COLUMNS, "tahun_ajaran", "semester");
    }

    private long countDistinctGuruJadwal() {
        return countDistinctGuruFromSlotTable("sekolah.jadwal_pelajaran", JADWAL_GURU_COLUMNS, "tahun_ajaran", "semester");
    }

    private long countDistinctGuruPiket() {
        return countDistinctGuruFromSlotTable("sekolah.absen_guru_piket", PIKET_GURU_COLUMNS, "tahun_ajaran", "semester");
    }

    private long countSlotGuruFromTable(String table, String[] columns, String tahunColumn, String semesterColumn) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("select sum(");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    sb.append("+");
                }
                sb.append("(case when t.").append(columns[i]).append(" is not null then 1 else 0 end)");
            }
            sb.append(") from ").append(table).append(" t where 1=1 ");
            sb.append(buildSqlPeriodeFilter("t", tahunColumn, semesterColumn));
            Number n = (Number) HibernateUtil.currentSession().createSQLQuery(sb.toString()).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countSlotGuruFromTable", e);
            return 0;
        }
    }

    private long countDistinctGuruFromSlotTable(String table, String[] columns, String tahunColumn, String semesterColumn) {
        try {
            String sql = buildSlotUnionSql(table, columns, tahunColumn, semesterColumn);
            sql = "select count(distinct x.guru_id) from (" + sql + ") x join sekolah.guru g on g.id = x.guru_id where x.guru_id is not null " + buildSqlGuruFilter("g");
            Number n = (Number) HibernateUtil.currentSession().createSQLQuery(sql).uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countDistinctGuruFromSlotTable", e);
            return 0;
        }
    }

    private List<DashboardMiniRow> loadTopGuruFromSlotTable(String table, String[] columns, String tahunColumn, String semesterColumn) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            String sql = buildSlotUnionSql(table, columns, tahunColumn, semesterColumn);
            sql = "select coalesce(g.nama_guru, '-') as nama, count(*) as total from (" + sql + ") x "
                    + "join sekolah.guru g on g.id = x.guru_id where x.guru_id is not null " + buildSqlGuruFilter("g")
                    + " group by coalesce(g.nama_guru, '-') order by count(*) desc limit " + TOP_LIMIT;
            List list = HibernateUtil.currentSession().createSQLQuery(sql).list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], "-"), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTopGuruFromSlotTable", e);
        }
        return rows;
    }

    private String buildSlotUnionSql(String table, String[] columns, String tahunColumn, String semesterColumn) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(" union all ");
            }
            sb.append("select t.").append(columns[i]).append(" as guru_id from ").append(table).append(" t where 1=1 ");
            sb.append(buildSqlPeriodeFilter("t", tahunColumn, semesterColumn));
        }
        return sb.toString();
    }

    private List<DashboardMiniRow> loadTopGuruAssociation(String association, String nameProperty, String emptyName) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Guru.class);
            applyGuruFilter(c, Boolean.TRUE);
            c.createAlias(association, "grp");
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("grp." + nameProperty))
                    .add(Projections.rowCount()));
            List list = c.list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], emptyName), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTopGuruAssociation " + association, e);
        }
        return rows;
    }

    private List<DashboardMiniRow> loadTopGroupByGuru(Class clazz, String groupProperty, String emptyName, String tahunProperty, String semesterProperty, boolean semesterString) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            applyEntityGuruFilter(c, clazz, tahunProperty, semesterProperty, semesterString);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty(groupProperty))
                    .add(Projections.rowCount()));
            List list = c.list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], emptyName), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTopGroupByGuru " + clazz, e);
        }
        return rows;
    }

    private List<DashboardMiniRow> loadTopAssociationByGuru(Class clazz, String association, String nameProperty, String emptyName, String tahunProperty, String semesterProperty, boolean semesterString) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            applyEntityGuruFilter(c, clazz, tahunProperty, semesterProperty, semesterString);
            c.createAlias(association, "grp");
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("grp." + nameProperty))
                    .add(Projections.rowCount()));
            List list = c.list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], emptyName), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTopAssociationByGuru " + clazz, e);
        }
        return rows;
    }

    private List<DashboardMiniRow> loadTopGuruEntity(Class clazz, String tahunProperty, String semesterProperty, boolean semesterString) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            applyEntityGuruFilter(c, clazz, tahunProperty, semesterProperty, semesterString);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("gfilter.nama"))
                    .add(Projections.rowCount()));
            List list = c.list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], "Guru"), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTopGuruEntity " + clazz, e);
        }
        return rows;
    }

    private List<DashboardMiniRow> loadTrendEntity(Class clazz, String tahunProperty, String semesterProperty, boolean semesterString) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            applyEntityGuruFilter(c, clazz, tahunProperty, semesterProperty, semesterString);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty(tahunProperty))
                    .add(Projections.rowCount()));
            List list = c.list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], "Tahun Ajaran"), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTrendEntity " + clazz, e);
        }
        sortTrend(rows);
        return rows;
    }

    private List<DashboardMiniRow> loadTrendAbsenPiket() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(AbsenGuruPiket.class);
            applyTahunAjaranSemesterSekolahFilter(c, AbsenGuruPiket.class, "tahunAjaran", "semester", false);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("tahunAjaran"))
                    .add(Projections.rowCount()));
            List list = c.list();
            for (Object o : list) {
                Object[] arr = (Object[]) o;
                rows.add(new DashboardMiniRow(toStringValue(arr[0], "Tahun Ajaran"), toLong(arr[1])));
            }
        } catch (Exception e) {
            debugError("loadTrendAbsenPiket", e);
        }
        sortTrend(rows);
        return rows;
    }

    private void applyGuruFilter(Criteria c, Boolean aktif) {
        if (aktif != null) {
            if (aktif.booleanValue()) {
                c.add(activeCriterion());
            } else {
                c.add(Restrictions.eq("aktif", Boolean.FALSE));
            }
        }
        if (currentSekolah != null) {
            Disjunction d = Restrictions.disjunction();
            d.add(Restrictions.eq("sekolah", currentSekolah));
            d.add(Restrictions.eq("sekolah1", currentSekolah));
            d.add(Restrictions.eq("sekolah2", currentSekolah));
            d.add(Restrictions.eq("sekolah3", currentSekolah));
            c.add(d);
        }
        if (currentJenisGuru != null) {
            c.add(Restrictions.eq("jenisGuru", currentJenisGuru));
        }
        if (currentJenisKelamin != null && !currentJenisKelamin.trim().isEmpty()) {
            c.add(Restrictions.eq("jenisKelamin", currentJenisKelamin));
        }
    }

    private void applyEntityGuruFilter(Criteria c, Class clazz, String tahunProperty, String semesterProperty, boolean semesterString) {
        boolean hasGuru = propertyExists(clazz, "guru");
        if (hasGuru) {
            c.createAlias("guru", "gfilter");
        }
        if (currentSekolah != null) {
            Disjunction d = Restrictions.disjunction();
            if (propertyExists(clazz, "sekolah")) {
                d.add(Restrictions.eq("sekolah", currentSekolah));
            }
            if (hasGuru) {
                d.add(Restrictions.eq("gfilter.sekolah", currentSekolah));
                d.add(Restrictions.eq("gfilter.sekolah1", currentSekolah));
                d.add(Restrictions.eq("gfilter.sekolah2", currentSekolah));
                d.add(Restrictions.eq("gfilter.sekolah3", currentSekolah));
            }
            c.add(d);
        }
        if (hasGuru && currentJenisGuru != null) {
            c.add(Restrictions.eq("gfilter.jenisGuru", currentJenisGuru));
        }
        if (hasGuru && currentJenisKelamin != null && !currentJenisKelamin.trim().isEmpty()) {
            c.add(Restrictions.eq("gfilter.jenisKelamin", currentJenisKelamin));
        }
        if (tahunProperty != null && propertyExists(clazz, tahunProperty)) {
            c.add(Restrictions.in(tahunProperty, tahunAjaranList()));
        }
        if (currentSemester != null && semesterProperty != null && propertyExists(clazz, semesterProperty)) {
            if (semesterString) {
                c.add(Restrictions.eq(semesterProperty, semesterLabel(currentSemester)));
            } else {
                c.add(Restrictions.eq(semesterProperty, currentSemester));
            }
        }
    }

    private void applyTahunAjaranSemesterSekolahFilter(Criteria c, Class clazz, String tahunProperty, String semesterProperty, boolean semesterString) {
        if (currentSekolah != null && propertyExists(clazz, "sekolah")) {
            c.add(Restrictions.eq("sekolah", currentSekolah));
        }
        if (tahunProperty != null && propertyExists(clazz, tahunProperty)) {
            c.add(Restrictions.in(tahunProperty, tahunAjaranList()));
        }
        if (currentSemester != null && semesterProperty != null && propertyExists(clazz, semesterProperty)) {
            if (semesterString) {
                c.add(Restrictions.eq(semesterProperty, semesterLabel(currentSemester)));
            } else {
                c.add(Restrictions.eq(semesterProperty, currentSemester));
            }
        }
    }

    private Criterion activeCriterion() {
        return Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif"));
    }

    private String buildSqlPeriodeFilter(String alias, String tahunColumn, String semesterColumn) {
        String sql = "";
        if (currentSekolah != null && currentSekolah.getId() != null) {
            sql += " and " + alias + ".sekolah_id = " + currentSekolah.getId().longValue() + " ";
        }
        List<String> tas = tahunAjaranList();
        if (tas != null && !tas.isEmpty()) {
            sql += " and " + alias + "." + tahunColumn + " in (" + sqlStringList(tas) + ") ";
        }
        if (currentSemester != null && semesterColumn != null && !semesterColumn.trim().isEmpty()) {
            sql += " and " + alias + "." + semesterColumn + " = " + currentSemester.intValue() + " ";
        }
        return sql;
    }

    private String buildSqlGuruFilter(String alias) {
        String sql = " and (" + alias + ".aktif = true or " + alias + ".aktif is null) ";
        if (currentSekolah != null && currentSekolah.getId() != null) {
            long sid = currentSekolah.getId().longValue();
            sql += " and (" + alias + ".sekolah_id = " + sid + " or " + alias + ".sekolah_1 = " + sid
                    + " or " + alias + ".sekolah_2 = " + sid + " or " + alias + ".sekolah_3 = " + sid + ") ";
        }
        if (currentJenisGuru != null && currentJenisGuru.getId() != null) {
            sql += " and " + alias + ".jenis_guru_id = " + currentJenisGuru.getId().longValue() + " ";
        }
        if (currentJenisKelamin != null && !currentJenisKelamin.trim().isEmpty()) {
            sql += " and " + alias + ".jenis_kelamin = '" + escapeSql(currentJenisKelamin) + "' ";
        }
        return sql;
    }

    private List<String> tahunAjaranList() {
        List<String> list = new ArrayList<String>();
        for (int i = currentMulai; i <= currentSampai; i++) {
            list.add(i + "/" + (i + 1));
        }
        return list;
    }

    private String semesterLabel(Integer semester) {
        if (semester == null) {
            return null;
        }
        return semester.intValue() == 1 ? "Ganjil" : "Genap";
    }

    private String sqlStringList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("'").append(escapeSql(list.get(i))).append("'");
        }
        return sb.toString();
    }

    private String escapeSql(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private void appendMetricCard(Div parent, String title, long value, String subtitle, String color, String icon, final String detailType) {
        Div card = new Div();
        card.setParent(parent);
        card.setStyle("cursor:pointer; border-radius:16px; padding:14px; background:#ffffff; border:1px solid #e2e8f0; "
                + "box-shadow:0 8px 20px rgba(15,23,42,0.06); position:relative; overflow:hidden;");
        card.appendChild(new Html("<div style='position:absolute; right:-24px; top:-24px; width:85px; height:85px; border-radius:999px; background:" + color + "; opacity:.10;'></div>"
                + "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px;'>"
                + "<div style='font-size:12px; color:#64748b; font-weight:800; text-transform:uppercase;'>" + escapeHtml(title) + "</div>"
                + "<div style='width:34px; height:34px; border-radius:12px; background:" + color + "; color:#fff; display:flex; align-items:center; justify-content:center;'>"
                + "<i class=\"fa " + icon + "\"></i></div></div>"
                + "<div style='margin-top:9px; font-size:27px; font-weight:900; color:#0f172a;'>" + formatNumber(value) + "</div>"
                + "<div style='margin-top:6px; color:#64748b; font-size:12px; line-height:1.45;'>" + escapeHtml(subtitle) + "</div>"
                + "<div style='margin-top:10px; font-size:11px; color:" + color + "; font-weight:800;'>Klik untuk melihat detail</div>"));
        card.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDetailWindow(detailType);
            }
        });
    }

    private void appendTopTable(Div parent, String title, String subtitle, List<DashboardMiniRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='border-radius:16px; background:#fff; border:1px solid #e2e8f0; padding:14px; box-shadow:0 8px 20px rgba(15,23,42,0.05);'>");
        sb.append("<div style='font-weight:900; color:#0f172a; font-size:15px;'>").append(escapeHtml(title)).append("</div>");
        sb.append("<div style='margin-top:4px; color:#64748b; font-size:12px; line-height:1.5;'>").append(escapeHtml(subtitle)).append("</div>");
        sb.append("<table style='width:100%; border-collapse:collapse; margin-top:12px; font-size:12px;'>");
        sb.append("<tr style='background:#f8fafc; color:#334155;'><th style='text-align:left; padding:8px; border-bottom:1px solid #e2e8f0;'>Nama</th><th style='text-align:right; padding:8px; border-bottom:1px solid #e2e8f0;'>Total</th></tr>");
        if (rows == null || rows.isEmpty()) {
            sb.append("<tr><td colspan='2' style='padding:10px; color:#94a3b8;'>Belum ada data.</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                DashboardMiniRow r = rows.get(i);
                String bg = i % 2 == 0 ? "#ffffff" : "#f8fafc";
                sb.append("<tr style='background:").append(bg).append(";'>");
                sb.append("<td style='padding:8px; border-bottom:1px solid #eef2f7; color:#0f172a; font-weight:700;'>").append(escapeHtml(r.label)).append("</td>");
                sb.append("<td style='padding:8px; border-bottom:1px solid #eef2f7; text-align:right; color:#1d4ed8; font-weight:900;'>").append(formatNumber(r.value)).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</table></div>");
        appendHtml(parent, sb.toString());
    }

    private void appendTrendPanel(Div parent, String title, String subtitle, List<DashboardMiniRow> rows) {
        long max = 1;
        if (rows != null) {
            for (DashboardMiniRow row : rows) {
                if (row.value > max) {
                    max = row.value;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='border-radius:16px; background:#fff; border:1px solid #e2e8f0; padding:14px; box-shadow:0 8px 20px rgba(15,23,42,0.05);'>");
        sb.append("<div style='font-weight:900; color:#0f172a; font-size:15px;'>").append(escapeHtml(title)).append("</div>");
        sb.append("<div style='margin-top:4px; color:#64748b; font-size:12px; line-height:1.5;'>").append(escapeHtml(subtitle)).append("</div>");
        if (rows == null || rows.isEmpty()) {
            sb.append("<div style='margin-top:12px; color:#94a3b8; font-size:12px;'>Belum ada data tren.</div>");
        } else {
            sb.append("<div style='margin-top:12px;'>");
            for (DashboardMiniRow row : rows) {
                int w = (int) Math.round((row.value * 100.0) / max);
                if (w < 6 && row.value > 0) {
                    w = 6;
                }
                sb.append("<div style='margin-bottom:10px;'>");
                sb.append("<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; color:#334155; font-weight:800;'>");
                sb.append("<span>").append(escapeHtml(row.label)).append("</span><span>").append(formatNumber(row.value)).append("</span></div>");
                sb.append("<div style='height:10px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:5px;'>");
                sb.append("<div style='height:10px; width:").append(w).append("%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
        sb.append("</div>");
        appendHtml(parent, sb.toString());
    }

    private String dashboardPanelHtml(String content) {
        return "<div style='border-radius:16px; background:#fff; border:1px solid #e2e8f0; padding:14px; "
                + "box-shadow:0 8px 20px rgba(15,23,42,0.05); min-height:150px;'>" + content + "</div>";
    }

    private String dashboardExplainHtml(String text) {
        return "<div style='font-size:12px; color:#64748b; line-height:1.6; margin-bottom:12px;'>" + escapeHtml(text) + "</div>";
    }

    private String progressSectionHtml(String title, long total, DashboardMiniRow a, DashboardMiniRow b, DashboardMiniRow c, DashboardMiniRow d) {
        DashboardMiniRow[] rows = new DashboardMiniRow[] { a, b, c, d };
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-weight:900; color:#0f172a; margin-bottom:10px;'>").append(escapeHtml(title)).append("</div>");
        for (int i = 0; i < rows.length; i++) {
            DashboardMiniRow r = rows[i];
            if (r == null) {
                continue;
            }
            int p = percent(r.value, total);
            sb.append("<div style='margin-bottom:10px;'>");
            sb.append("<div style='display:flex; justify-content:space-between; font-size:12px; color:#334155; font-weight:800;'><span>")
                    .append(escapeHtml(r.label)).append("</span><span>").append(formatNumber(r.value)).append(" / ").append(p).append("%</span></div>");
            sb.append("<div style='height:9px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:5px;'>")
                    .append("<div style='height:9px; width:").append(p).append("%; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;'></div></div>");
            sb.append("</div>");
        }
        return sb.toString();
    }

    private String miniStatHtml(String label, long value, String note) {
        return "<div style='display:flex; align-items:flex-start; justify-content:space-between; gap:12px; padding:10px 0; border-bottom:1px solid #eef2f7;'>"
                + "<div><div style='font-weight:900; color:#0f172a; font-size:13px;'>" + escapeHtml(label) + "</div>"
                + "<div style='margin-top:3px; color:#64748b; font-size:12px; line-height:1.4;'>" + escapeHtml(note) + "</div></div>"
                + "<div style='font-weight:900; color:#2563eb; font-size:18px;'>" + formatNumber(value) + "</div></div>";
    }

    private void appendRecommendationCard(Div parent, String title, String text, String icon) {
        appendHtml(parent, "<div style='border-radius:16px; background:#ffffff; border:1px solid #dbeafe; padding:15px; box-shadow:0 8px 20px rgba(15,23,42,0.05);'>"
                + "<div style='display:flex; gap:10px; align-items:center;'>"
                + "<div style='width:38px; height:38px; border-radius:14px; background:#eff6ff; color:#1d4ed8; display:flex; align-items:center; justify-content:center;'><i class=\"fa " + icon + "\"></i></div>"
                + "<div style='font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div></div>"
                + "<div style='margin-top:10px; color:#475569; font-size:12px; line-height:1.65;'>" + escapeHtml(text) + "</div></div>");
    }

    private String sectionIntroHtml(String title, String subtitle) {
        return "<div style='margin-top:16px; display:flex; align-items:flex-end; justify-content:space-between; gap:12px;'>"
                + "<div><div style='font-weight:900; color:#0f172a; font-size:18px;'>" + escapeHtml(title) + "</div>"
                + "<div style='margin-top:4px; color:#64748b; font-size:12px; line-height:1.5;'>" + escapeHtml(subtitle) + "</div></div></div>";
    }

    private void showDetailWindow(String detailType) {
        try {
            List<DashboardDetailRow> rows = loadDetailRows(detailType);
            Window window = new Window();
            window.setTitle("Detail " + detailTitle(detailType));
            window.setWidth("930px");
            window.setHeight("620px");
            window.setBorder("normal");
            window.setClosable(true);
            window.setSizable(true);
            window.setStyle("background:#ffffff;");

            Vbox container = new Vbox();
            container.setParent(window);
            container.setWidth("100%");
            container.setHeight("100%");
            container.setStyle("padding:12px; box-sizing:border-box;");

            appendHtml(container, "<div style='padding:12px; border-radius:14px; background:#eff6ff; border:1px solid #bfdbfe; color:#1e3a8a; font-weight:800;'>"
                    + escapeHtml(detailTitle(detailType)) + " • maksimal " + DETAIL_LIMIT + " data contoh • paging 10 baris</div>");

            Grid grid = new Grid();
            grid.setParent(container);
            grid.setWidth("100%");
            grid.setHeight("520px");
            grid.setMold("paging");
            grid.setPageSize(10);
            grid.setStyle("margin-top:10px; border:1px solid #e2e8f0;");

            Columns columns = new Columns();
            columns.setParent(grid);
            appendColumn(columns, "No", "55px");
            appendColumn(columns, "Nama/Data", "260px");
            appendColumn(columns, "Informasi", "300px");
            appendColumn(columns, "Keterangan", "280px");

            Rows rs = new Rows();
            rs.setParent(grid);
            if (rows == null || rows.isEmpty()) {
                Row row = new Row();
                row.setParent(rs);
                Cell cell = new Cell();
                cell.setParent(row);
                cell.setColspan(4);
                cell.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data untuk filter saat ini.")));
            } else {
                for (int i = 0; i < rows.size(); i++) {
                    DashboardDetailRow r = rows.get(i);
                    Row row = new Row();
                    row.setParent(rs);
                    row.setStyle(i % 2 == 0 ? "background:#ffffff;" : "background:#f8fafc;");
                    appendCell(row, String.valueOf(i + 1));
                    appendCell(row, r.title);
                    appendCell(row, r.info);
                    appendCell(row, r.note);
                }
            }
            pasangPopupDetail(window);
            window.doModal();
        } catch (Exception e) {
            debugError("showDetailWindow " + detailType, e);
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

    private List<DashboardDetailRow> loadDetailRows(String detailType) {
        if ("JADWAL".equals(detailType)) {
            return loadJadwalDetailRows();
        }
        if ("PENUGASAN".equals(detailType)) {
            return loadEntityDetailRows(PenugasanGuruMengajar.class, "tahunAkademik", "semester", true);
        }
        if ("PRESTASI".equals(detailType)) {
            return loadEntityDetailRows(PrestasiGuru.class, "tahunAkademik", "jenisSemester", true);
        }
        if ("CATATAN".equals(detailType)) {
            return loadEntityDetailRows(CatatanGuru.class, "tahunAjaran", "semester", false);
        }
        if ("PIKET".equals(detailType)) {
            return loadPiketDetailRows();
        }
        if ("GURU_TANPA_JADWAL".equals(detailType)) {
            return loadGuruTanpaJadwalRows();
        }
        return loadGuruDetailRows(detailType);
    }

    private List<DashboardDetailRow> loadGuruDetailRows(String detailType) {
        List<DashboardDetailRow> rows = new ArrayList<DashboardDetailRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Guru.class);
            if ("GURU_NONAKTIF".equals(detailType)) {
                applyGuruFilter(c, Boolean.FALSE);
            } else {
                applyGuruFilter(c, Boolean.TRUE);
            }
            if ("GURU_EMAIL".equals(detailType)) {
                c.add(Restrictions.isNotNull("alamatEmail"));
                c.add(Restrictions.ne("alamatEmail", ""));
            } else if ("GURU_HP".equals(detailType)) {
                Disjunction d = Restrictions.disjunction();
                d.add(Restrictions.and(Restrictions.isNotNull("hp"), Restrictions.ne("hp", "")));
                d.add(Restrictions.and(Restrictions.isNotNull("teleponGuru"), Restrictions.ne("teleponGuru", "")));
                c.add(d);
            } else if ("GURU_NUPTK".equals(detailType)) {
                c.add(Restrictions.isNotNull("nuptk"));
                c.add(Restrictions.ne("nuptk", ""));
            }
            c.addOrder(Order.asc("nama"));
            c.setMaxResults(DETAIL_LIMIT);
            List list = c.list();
            for (Object o : list) {
                Guru g = (Guru) o;
                rows.add(new DashboardDetailRow(safeName(g),
                        "Kode: " + nvl(invokeString(g, "getKode")) + " • Jenis: " + safeName(invokeObject(g, "getJenisGuru")),
                        "Email: " + nvl(invokeString(g, "getAlamatEmail")) + " • HP: " + nvl(invokeString(g, "getHp")) + " • NUPTK: " + nvl(invokeString(g, "getNuptk"))));
            }
        } catch (Exception e) {
            debugError("loadGuruDetailRows", e);
        }
        return rows;
    }

    private List<DashboardDetailRow> loadGuruTanpaJadwalRows() {
        List<DashboardDetailRow> rows = new ArrayList<DashboardDetailRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Guru.class);
            applyGuruFilter(c, Boolean.TRUE);
            c.addOrder(Order.asc("nama"));
            c.setMaxResults(DETAIL_LIMIT * 3);
            List list = c.list();
            Set<Long> guruTerjadwal = loadGuruIdsFromJadwal();
            for (Object o : list) {
                Guru g = (Guru) o;
                if (g != null && g.getId() != null && !guruTerjadwal.contains(g.getId())) {
                    rows.add(new DashboardDetailRow(safeName(g),
                            "Kode: " + nvl(invokeString(g, "getKode")) + " • Jenis: " + safeName(invokeObject(g, "getJenisGuru")),
                            "Belum ditemukan pada slot jadwal pelajaran periode filter."));
                    if (rows.size() >= DETAIL_LIMIT) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            debugError("loadGuruTanpaJadwalRows", e);
        }
        return rows;
    }

    private Set<Long> loadGuruIdsFromJadwal() {
        Set<Long> ids = new HashSet<Long>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class);
            applyTahunAjaranSemesterSekolahFilter(c, JadwalPelajaran.class, "tahunAjaran", "semester", false);
            c.setMaxResults(5000);
            List list = c.list();
            for (Object o : list) {
                JadwalPelajaran j = (JadwalPelajaran) o;
                addGuruId(ids, invokeObject(j, "getGuru"));
                addGuruId(ids, invokeObject(j, "getGuru2"));
                addGuruId(ids, invokeObject(j, "getGuru3"));
                addGuruId(ids, invokeObject(j, "getGuru4"));
                addGuruId(ids, invokeObject(j, "getGuru5"));
                addGuruId(ids, invokeObject(j, "getGuru6"));
                addGuruId(ids, invokeObject(j, "getGuru7"));
                addGuruId(ids, invokeObject(j, "getGuru8"));
                addGuruId(ids, invokeObject(j, "getGuru9"));
                addGuruId(ids, invokeObject(j, "getGuru10"));
                addGuruId(ids, invokeObject(j, "getGuru11"));
                addGuruId(ids, invokeObject(j, "getGuru12"));
            }
        } catch (Exception e) {
            debugError("loadGuruIdsFromJadwal", e);
        }
        return ids;
    }

    private void addGuruId(Set<Long> ids, Object guru) {
        if (guru instanceof Guru) {
            Guru g = (Guru) guru;
            if (g.getId() != null) {
                ids.add(g.getId());
            }
        }
    }

    private List<DashboardDetailRow> loadJadwalDetailRows() {
        List<DashboardDetailRow> rows = new ArrayList<DashboardDetailRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class);
            applyTahunAjaranSemesterSekolahFilter(c, JadwalPelajaran.class, "tahunAjaran", "semester", false);
            c.addOrder(Order.desc("id"));
            c.setMaxResults(DETAIL_LIMIT);
            List list = c.list();
            for (Object o : list) {
                JadwalPelajaran j = (JadwalPelajaran) o;
                rows.add(new DashboardDetailRow(
                        "Jadwal #" + nvl(invokeString(j, "getId")),
                        "Mapel: " + safeName(invokeObject(j, "getMatapelajaran")) + " • Kelas: " + safeName(invokeObject(j, "getKelas")),
                        "Guru: " + joinGuruNames(j) + " • TA: " + nvl(invokeString(j, "getTahunAjaran")) + " • Semester: " + nvl(invokeString(j, "getSemester"))));
            }
        } catch (Exception e) {
            debugError("loadJadwalDetailRows", e);
        }
        return rows;
    }

    private List<DashboardDetailRow> loadPiketDetailRows() {
        List<DashboardDetailRow> rows = new ArrayList<DashboardDetailRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(AbsenGuruPiket.class);
            applyTahunAjaranSemesterSekolahFilter(c, AbsenGuruPiket.class, "tahunAjaran", "semester", false);
            c.addOrder(Order.desc("tanggal"));
            c.setMaxResults(DETAIL_LIMIT);
            List list = c.list();
            for (Object o : list) {
                AbsenGuruPiket p = (AbsenGuruPiket) o;
                rows.add(new DashboardDetailRow(
                        "Piket #" + nvl(invokeString(p, "getId")),
                        "Tanggal: " + dateToString(invokeObject(p, "getTanggal")) + " • Jam ke: " + nvl(invokeString(p, "getJamke")),
                        "Guru: " + joinGuruNames(p) + " • TA: " + nvl(invokeString(p, "getTahunAjaran")) + " • Semester: " + nvl(invokeString(p, "getSemester"))));
            }
        } catch (Exception e) {
            debugError("loadPiketDetailRows", e);
        }
        return rows;
    }

    private List<DashboardDetailRow> loadEntityDetailRows(Class clazz, String tahunProperty, String semesterProperty, boolean semesterString) {
        List<DashboardDetailRow> rows = new ArrayList<DashboardDetailRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            applyEntityGuruFilter(c, clazz, tahunProperty, semesterProperty, semesterString);
            if (propertyExists(clazz, "id")) {
                c.addOrder(Order.desc("id"));
            }
            c.setMaxResults(DETAIL_LIMIT);
            List list = c.list();
            for (Object o : list) {
                Object guru = invokeObject(o, "getGuru");
                String nama = nvl(invokeString(o, "getNama"));
                if (nama.length() == 0) {
                    nama = safeName(guru);
                }
                String info = "Guru: " + safeName(guru);
                String status = invokeString(o, "getStatus");
                if (status != null && status.trim().length() > 0) {
                    info += " • Status: " + status;
                }
                String tahun = invokeString(o, "getTahunAkademik");
                if (tahun == null || tahun.trim().length() == 0) {
                    tahun = invokeString(o, "getTahunAjaran");
                }
                String sem = invokeString(o, "getSemester");
                if (sem == null || sem.trim().length() == 0) {
                    sem = invokeString(o, "getJenisSemester");
                }
                rows.add(new DashboardDetailRow(nama, info, "TA: " + nvl(tahun) + " • Semester: " + nvl(sem) + " • " + nvl(invokeString(o, "getKeterangan"))));
            }
        } catch (Exception e) {
            debugError("loadEntityDetailRows " + clazz, e);
        }
        return rows;
    }

    private String joinGuruNames(Object o) {
        List<String> names = new ArrayList<String>();
        for (int i = 1; i <= 12; i++) {
            String method = i == 1 ? "getGuru" : "getGuru" + i;
            Object guru = invokeObject(o, method);
            if (guru instanceof Guru) {
                String name = safeName(guru);
                if (name.length() > 0 && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        if (names.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private String detailTitle(String detailType) {
        if ("GURU_AKTIF".equals(detailType)) return "Guru Aktif";
        if ("GURU_TANPA_JADWAL".equals(detailType)) return "Guru Belum Terjadwal";
        if ("GURU_EMAIL".equals(detailType)) return "Guru Dengan Email";
        if ("GURU_HP".equals(detailType)) return "Guru Dengan HP/Telepon";
        if ("GURU_NUPTK".equals(detailType)) return "Guru Dengan NUPTK";
        if ("JADWAL".equals(detailType)) return "Jadwal Pelajaran";
        if ("PENUGASAN".equals(detailType)) return "Penugasan Guru Mengajar";
        if ("PRESTASI".equals(detailType)) return "Prestasi Guru";
        if ("CATATAN".equals(detailType)) return "Catatan Guru";
        if ("PIKET".equals(detailType)) return "Absen Guru Piket";
        return "Data Guru";
    }

    private void populateSekolahCombobox() {
        searchSekolah.getItems().clear();
        Comboitem all = new Comboitem("Semua Sekolah");
        all.setParent(searchSekolah);
        all.setValue(null);
        if (currentSekolah == null) {
            searchSekolah.setSelectedItem(all);
        }
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Sekolah.class);
            c.addOrder(Order.asc("nama"));
            c.setMaxResults(300);
            List list = c.list();
            for (Object o : list) {
                Sekolah s = (Sekolah) o;
                Comboitem item = new Comboitem(safeName(s));
                item.setParent(searchSekolah);
                item.setValue(s);
                if (currentSekolah != null && currentSekolah.getId() != null && s.getId() != null && currentSekolah.getId().equals(s.getId())) {
                    searchSekolah.setSelectedItem(item);
                }
            }
        } catch (Exception e) {
            debugError("populateSekolahCombobox", e);
        }
    }

    private void populateJenisGuruCombobox() {
        searchJenisGuru.getItems().clear();
        Comboitem all = new Comboitem("Semua Jenis Guru");
        all.setParent(searchJenisGuru);
        all.setValue(null);
        if (currentJenisGuru == null) {
            searchJenisGuru.setSelectedItem(all);
        }
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JenisGuru.class);
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            c.add(activeCriterion());
            c.addOrder(Order.asc("nama"));
            c.setMaxResults(200);
            List list = c.list();
            for (Object o : list) {
                JenisGuru j = (JenisGuru) o;
                Comboitem item = new Comboitem(safeName(j));
                item.setParent(searchJenisGuru);
                item.setValue(j);
                if (currentJenisGuru != null && currentJenisGuru.getId() != null && j.getId() != null && currentJenisGuru.getId().equals(j.getId())) {
                    searchJenisGuru.setSelectedItem(item);
                }
            }
        } catch (Exception e) {
            debugError("populateJenisGuruCombobox", e);
        }
    }

    private void populateStringCombobox(Combobox combo, String[] values, String selected) {
        combo.getItems().clear();
        for (int i = 0; i < values.length; i++) {
            Comboitem item = new Comboitem(values[i]);
            item.setParent(combo);
            item.setValue(i == 0 ? null : values[i]);
            if ((selected == null && i == 0) || (selected != null && selected.equals(values[i]))) {
                combo.setSelectedItem(item);
            }
        }
    }

    private void populateSemesterCombobox() {
        searchSemester.getItems().clear();
        Comboitem semua = new Comboitem("Semua");
        semua.setParent(searchSemester);
        semua.setValue(null);
        if (currentSemester == null) {
            searchSemester.setSelectedItem(semua);
        }
        Comboitem ganjil = new Comboitem("Semester 1 / Ganjil");
        ganjil.setParent(searchSemester);
        ganjil.setValue(Integer.valueOf(1));
        if (currentSemester != null && currentSemester.intValue() == 1) {
            searchSemester.setSelectedItem(ganjil);
        }
        Comboitem genap = new Comboitem("Semester 2 / Genap");
        genap.setParent(searchSemester);
        genap.setValue(Integer.valueOf(2));
        if (currentSemester != null && currentSemester.intValue() == 2) {
            searchSemester.setSelectedItem(genap);
        }
    }

    private Sekolah getSelectedSekolah() {
        try {
            if (searchSekolah != null && searchSekolah.getSelectedItem() != null) {
                Object v = searchSekolah.getSelectedItem().getValue();
                return v instanceof Sekolah ? (Sekolah) v : null;
            }
        } catch (Exception e) {
            debugError("getSelectedSekolah", e);
        }
        return currentSekolah;
    }

    private JenisGuru getSelectedJenisGuru() {
        try {
            if (searchJenisGuru != null && searchJenisGuru.getSelectedItem() != null) {
                Object v = searchJenisGuru.getSelectedItem().getValue();
                return v instanceof JenisGuru ? (JenisGuru) v : null;
            }
        } catch (Exception e) {
            debugError("getSelectedJenisGuru", e);
        }
        return currentJenisGuru;
    }

    private String getSelectedString(Combobox combo, String current) {
        try {
            if (combo != null && combo.getSelectedItem() != null) {
                Object v = combo.getSelectedItem().getValue();
                return v == null ? null : String.valueOf(v);
            }
        } catch (Exception e) {
            debugError("getSelectedString", e);
        }
        return current;
    }

    private Integer getSelectedSemester() {
        try {
            if (searchSemester != null && searchSemester.getSelectedItem() != null) {
                Object v = searchSemester.getSelectedItem().getValue();
                return v instanceof Integer ? (Integer) v : null;
            }
        } catch (Exception e) {
            debugError("getSelectedSemester", e);
        }
        return currentSemester;
    }

    private void appendToolbarLabel(Toolbar toolbar, String text) {
        Label label = new MyLabelAgakKecil(text);
        label.setParent(toolbar);
        label.setStyle("font-weight:800; color:#334155; margin-left:6px;");
    }

    private void appendColumn(Columns columns, String label, String width) {
        Column c = new Column(label);
        c.setParent(columns);
        c.setWidth(width);
    }

    private void appendCell(Row row, String value) {
        Cell cell = new Cell();
        cell.setParent(row);
        cell.setStyle("padding:8px;");
        cell.appendChild(new Label(value == null ? "" : value));
    }

    private void appendHtml(Component parent, String html) {
        Html h = new Html(html == null ? "" : html);
        h.setParent(parent);
    }

    private int percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((value * 100.0) / total);
    }

    private String formatNumber(long value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String toStringValue(Object value, String empty) {
        if (value == null) {
            return empty;
        }
        String s = String.valueOf(value);
        return s == null || s.trim().length() == 0 ? empty : s.trim();
    }

    private String safeName(Object obj) {
        if (obj == null) {
            return "";
        }
        String[] methods = new String[] { "getNama", "getNamaGuru", "getNamaSekolah", "getKode", "toString" };
        for (int i = 0; i < methods.length; i++) {
            String val = invokeString(obj, methods[i]);
            if (val != null && val.trim().length() > 0 && !val.startsWith(obj.getClass().getName())) {
                return val.trim();
            }
        }
        return String.valueOf(obj);
    }

    private Object invokeObject(Object obj, String methodName) {
        try {
            if (obj == null || methodName == null) {
                return null;
            }
            Method m = obj.getClass().getMethod(methodName, new Class[] {});
            return m.invoke(obj, new Object[] {});
        } catch (Exception e) {
            return null;
        }
    }

    private String invokeString(Object obj, String methodName) {
        Object v = invokeObject(obj, methodName);
        return v == null ? null : String.valueOf(v);
    }

    private String dateToString(Object value) {
        try {
            if (value instanceof Date) {
                return Common.dateFormat3.get().format((Date) value);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardGuru.java:1746");
        }
        return value == null ? "" : String.valueOf(value);
    }

    private String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean propertyExists(Class clazz, String property) {
        if (clazz == null || property == null || property.length() == 0) {
            return false;
        }
        String getter = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
        String booleanGetter = "is" + property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            clazz.getMethod(getter, new Class[] {});
            return true;
        } catch (Exception e) {
            try {
                clazz.getMethod(booleanGetter, new Class[] {});
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void sortAndLimit(List<DashboardMiniRow> rows, final int limit) {
        if (rows == null) {
            return;
        }
        Collections.sort(rows, new Comparator<DashboardMiniRow>() {
            @Override
            public int compare(DashboardMiniRow o1, DashboardMiniRow o2) {
                if (o1 == null && o2 == null) return 0;
                if (o1 == null) return 1;
                if (o2 == null) return -1;
                if (o1.value == o2.value) {
                    return o1.label.compareTo(o2.label);
                }
                return o1.value < o2.value ? 1 : -1;
            }
        });
        while (rows.size() > limit) {
            rows.remove(rows.size() - 1);
        }
    }

    private void sortTrend(List<DashboardMiniRow> rows) {
        if (rows == null) {
            return;
        }
        Collections.sort(rows, new Comparator<DashboardMiniRow>() {
            @Override
            public int compare(DashboardMiniRow o1, DashboardMiniRow o2) {
                if (o1 == null && o2 == null) return 0;
                if (o1 == null) return 1;
                if (o2 == null) return -1;
                return o1.label.compareTo(o2.label);
            }
        });
    }

    private void debugError(String source, Exception e) {
        if (debug && e != null) {
            System.err.println("[DasboardGuru] " + source + " : " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasboardGuru.java:1820");
        }
    }

    /**
     * Tipe implementasi bersarang {@link DashboardGuruData} milik {@link DasboardGuru}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardGuru}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long totalGuru}, {@code long
     * totalGuruSemua}, {@code long totalGuruNonAktif}, {@code long totalLakiLaki}, {@code long totalPerempuan},
     * {@code long totalJenisGuru}, {@code long totalEmailGuru}, {@code long totalHpGuru}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DasboardGuru
     */
    private static class DashboardGuruData {
        long totalGuru;
        long totalGuruSemua;
        long totalGuruNonAktif;
        long totalLakiLaki;
        long totalPerempuan;
        long totalJenisGuru;
        long totalEmailGuru;
        long totalHpGuru;
        long totalTeleponGuru;
        long totalAlamatGuru;
        long totalNuptkGuru;
        long totalNipGuru;
        long totalNikGuru;
        long totalPegawaiLink;
        long totalJadwalPelajaran;
        long totalSlotMengajar;
        long totalGuruTerjadwal;
        long totalGuruTanpaJadwal;
        long totalPenugasan;
        long totalPrestasi;
        long totalPrestasiDisetujui;
        long totalPrestasiDitolak;
        long totalPrestasiBelum;
        long totalPrestasiSedang;
        long totalPrestasiLuar;
        long totalCatatan;
        long totalJenisCatatan;
        long totalPiket;
        long totalSlotPiket;
        long totalGuruPiket;
        int rasioGuruTerjadwal;
        int rasioEmail;
        int rasioHp;
        int rasioNuptk;
        int rasioPrestasiDisetujui;
        int rasioPiket;
        List<DashboardMiniRow> topJenisGuru = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topStatusKepegawaian = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topGuruJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topGuruPiket = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPrestasiStatus = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPrestasiKategori = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topCatatanJenis = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topGuruPrestasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topGuruCatatan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPenugasan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPrestasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendCatatan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPiket = new ArrayList<DashboardMiniRow>();
    }

    /**
     * Pembawa data/helper lokal milik {@link DasboardGuru} untuk dashboard mini row. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardGuru}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code long value}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DasboardGuru
     */
    private static class DashboardMiniRow {
        String label;
        long value;

        DashboardMiniRow(String label, long value) {
            this.label = label == null || label.trim().length() == 0 ? "-" : label.trim();
            this.value = value;
        }
    }

    /**
     * Tipe implementasi bersarang {@link DashboardDetailRow} milik {@link DasboardGuru}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardGuru}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String title}, {@code String info},
     * {@code String note}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DasboardGuru
     */
    private static class DashboardDetailRow {
        String title;
        String info;
        String note;

        DashboardDetailRow(String title, String info, String note) {
            this.title = title == null ? "" : title;
            this.info = info == null ? "" : info;
            this.note = note == null ? "" : note;
        }
    }
}
