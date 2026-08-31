package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASBOARD_JADWAL_PELAJARAN_2026_05_30
 * Dibuat mengikuti pola DasborPerguruanTinggiTerpadu:
 * - Hero modern, filter global, loading indikator + progress bar bertahap.
 * - Overview khusus jadwal pelajaran, jam pelajaran, guru, kelas, kelas les, pertemuan, diskusi, grup pertemuan, dan ujian.
 * - Angka utama dapat diklik untuk membuka popup detail ringan dengan paging 10 baris.
 * - Kompatibel Java 1.6/1.7 dan ZKoss 5.5: tanpa lambda, tanpa diamond operator.
 *
 * Catatan integrasi:
 * - Class ini memakai model sekolah dan model pertemuan yang sudah ada pada package ais.database.model.sekolah dan ais.database.model.
 * - Filter tahun menggunakan rentang tahun dan dikonversi menjadi daftar tahun ajaran, misal 2024 => 2024/2025.
 * - Untuk JadwalPelajaran, filter memakai tahunAjaran dan semester.
 * - Untuk Pertemuan, filter memakai ta/smt serta relasi JadwalPelajaran bila kelas dipilih.
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
import org.hibernate.Session;
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
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.MasaJadwalPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Komponen dashboard khusus untuk dasboard jadwal pelajaran. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyPortallayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code java.util.concurrent.ConcurrentHashMap
 * _CACHE}, {@code java.util.concurrent.ConcurrentHashMap _EXPIRY}, {@code long _TTL_MS}, {@code int TOP_LIMIT},
 * {@code int DETAIL_LIMIT}, {@code boolean debug}, {@code Tbmuser tbmuser}, {@code Integer desktopHeight};
 * inisialisasi/lifecycle ({@code init()}, {@code initDefaultFilterValue()}, {@code appendMiniTable()});
 * pembacaan/pencarian ({@code renderDashboardContentDenganLoading()}, {@code
 * tampilkanLoadingDashboardSekolah()}, {@code hapusLoadingDashboardSekolah()}, {@code
 * buildLoadingDashboardHtml()}, {@code loadDashboardDataCached()}, {@code loadDashboardData()}); mutasi data
 * ({@code setDebug()}, {@code updateDashboardProgress()}); pelaporan/ekspor ({@code renderContent()}, {@code
 * renderHero()}, {@code renderFilter()}, {@code renderOverview()}, {@code renderAkademikPembelajaran()}, {@code
 * renderPenilaianCatatan()}); operasi domain lain ({@code isDebug()}, {@code buildCacheKey()}, {@code
 * countGeneric()}, {@code countPesertaKelas()}, {@code countPesertaLes()}, {@code countDistinctJadwal()});
 * konfigurasi constructor: {@code tbmuser}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyPortallayout
 */
public class DasboardJadwalPelajaran extends MyPortallayout {

    private static final long serialVersionUID = 20260530165001L;
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
    private Combobox searchSemester;

    private int currentMulai;
    private int currentSampai;
    private Sekolah currentSekolah;
    private KelasSiswa currentKelas;
    private Integer currentSemester;

    public DasboardJadwalPelajaran() throws Exception {
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
        DashboardGridExportHelper.pasang(this, "Jadwal Pelajaran");
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
        panel.setTitle("Dasbor Jadwal Pelajaran");
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
        currentKelas = getSelectedKelas();
        currentSemester = getSelectedSemester();

        renderHero();
        renderFilter();

        final int loadVersion = ++dashboardLoadVersion;
        tampilkanLoadingDashboardSekolah("Menyiapkan parameter filter dan tampilan awal...", 2);

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
            updateDashboardProgress("Mengambil ringkasan Dasbor Jadwal Pelajaran...", 5);

            DashboardJadwalData data = loadDashboardDataCached();

            if (loadVersion != dashboardLoadVersion) {
                return;
            }

            updateDashboardProgress("Menyusun kartu, panel analitik, tabel top data, dan rekomendasi...", 96);
            hapusLoadingDashboardSekolah();

            renderOverview(data);
            renderAkademikPembelajaran(data);
            renderPenilaianCatatan(data);
            renderTopTablesAndTrends(data);
            renderRecommendation(data);
        } catch (Exception e) {
            debugError("renderDashboardContentDenganLoading", e);
            hapusLoadingDashboardSekolah();
            appendHtml(body, "<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; "
                    + "color:#991b1b; border:1px solid #fecdd3; font-weight:700;'>Dasbor Jadwal Pelajaran belum dapat dimuat. "
                    + "Silakan tekan Refresh atau aktifkan debug untuk melihat detail error.</div>");
        }
    }

    private void tampilkanLoadingDashboardSekolah(String pesan, int persen) {
        if (body == null) {
            return;
        }

        hapusLoadingDashboardSekolah();

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

    private void hapusLoadingDashboardSekolah() {
        if (loadingDashboardContainer != null) {
            try {
                if (loadingDashboardContainer.getParent() != null) {
                    loadingDashboardContainer.detach();
                }
            } catch (Exception e) {
                debugError("hapusLoadingDashboardSekolah", e);
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
                + "<i class=\"fa fa-spinner fa-spin\"></i> Memproses Dasbor Jadwal Pelajaran</div>"
                + "<div style='font-size:12px; margin-bottom:10px; color:#64748b;'>" + safeHtml(pesan) + "</div>"
                + "<div style='width:100%; height:12px; border-radius:999px; overflow:hidden; background:#e2e8f0;'>"
                + "<div style='height:12px; width:" + progress + "%; border-radius:999px; "
                + "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div>"
                + "</div>"
                + "<div style='font-size:12px; font-weight:800; color:#0f766e; margin-top:7px;'>"
                + progress + "% selesai</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:4px;'>"
                + "Mohon tunggu. Sistem sedang mengambil data sekolah, kelas, siswa, guru, jadwal, dan penilaian secara bertahap.</div>"
                + "</div>";
    }

    private void renderHero() {
        String user = tbmuser == null ? "Pengguna" : safeText(tbmuser.getUserNama());
        String periode = currentMulai + " s.d. " + currentSampai;
        String sekolah = currentSekolah == null ? "Semua Sekolah" : safeText(labelSekolah(currentSekolah));
        String kelas = currentKelas == null ? "Semua Kelas" : safeText(labelKelas(currentKelas));
        String semester = currentSemester == null ? "Semua Semester" : String.valueOf(currentSemester);

        appendHtml(body, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
                + "color:#ffffff; box-shadow:0 18px 38px rgba(29,78,216,0.20);'>"
                + "<div style='position:absolute; width:260px; height:260px; right:-80px; top:-100px; border-radius:999px; background:rgba(255,255,255,0.13);'></div>"
                + "<div style='position:absolute; width:170px; height:170px; right:135px; bottom:-96px; border-radius:999px; background:rgba(255,255,255,0.10);'></div>"
                + "<div style='position:relative; z-index:2;'>"
                + "<div style='font-size:12px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:700;'>Monitoring Jadwal, Jam Pelajaran, Guru, Kelas, dan Pertemuan</div>"
                + "<div style='font-size:28px; line-height:1.18; font-weight:800; margin-top:7px;'>Dasbor Jadwal Pelajaran</div>"
                + "<div style='font-size:13px; max-width:960px; opacity:.93; margin-top:8px;'>Ringkasan ini dirancang khusus untuk memantau kesiapan JadwalPelajaran, master JamPelajaran, jenis jadwal, kelas reguler, kelas les, guru pengajar, mata pelajaran, pertemuan pembelajaran, diskusi, grup pertemuan, dan ujian yang terhubung dengan jadwal.</div>"
                + "<div style='margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;'>"
                + badgeHtml("Periode: " + periode, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml("Semester: " + semester, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(sekolah, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml(kelas, "rgba(255,255,255,.16)", "#ffffff")
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
        searchSekolah.setCols(24);
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

        new MyLabelAgakKecil("Semester:").setParent(toolbar);
        searchSemester = new Combobox();
        searchSemester.setCols(10);
        searchSemester.setReadonly(true);
        searchSemester.setParent(toolbar);
        populateSemesterCombo(searchSemester);
        selectComboByValue(searchSemester, currentSemester);

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
        searchSemester.addEventListener("onChange", reloadListener);

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
        refresh.setTooltiptext("Refresh dasbor");
        refresh.setParent(toolbar);
        refresh.addEventListener("onClick", reloadListener);
    }

    private String buildCacheKey() {
        return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
                + "|" + currentMulai + "|" + currentSampai
                + "|" + (currentSekolah == null ? "" : currentSekolah.getId())
                + "|" + (currentKelas == null ? "" : currentKelas.getId())
                + "|" + (currentSemester == null ? "" : currentSemester);
    }

    @SuppressWarnings("unchecked")
    private DashboardJadwalData loadDashboardDataCached() throws Exception {
        String _k = buildCacheKey();
        Long _e = _EXPIRY.get(_k);
        if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
            return (DashboardJadwalData) _CACHE.get(_k);
        }
        DashboardJadwalData data = loadDashboardData();
        _CACHE.put(_k, data);
        _EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
        return data;
    }

    private DashboardJadwalData loadDashboardData() throws Exception {
        DashboardJadwalData data = new DashboardJadwalData();
        data.tahunMulai = currentMulai;
        data.tahunSampai = currentSampai;
        data.tahunAjaranLabel = labelTahunAjaranFilter();
        data.semester = currentSemester;

        updateDashboardProgress("Mengambil master sekolah, guru, kelas, kelas les, dan peserta jadwal...", 10);

        data.totalSekolah = currentSekolah == null ? countGeneric(Sekolah.class, false, false, false) : 1;
        data.totalGuru = countGeneric(Guru.class, true, false, false);
        data.totalSiswa = countGeneric(Siswa.class, true, true, false);
        data.totalKelas = countGeneric(KelasSiswa.class, true, true, false);
        data.totalKelasLes = countGeneric(KelasLesSiswa.class, true, false, false);
        data.totalPesertaKelas = countPesertaKelas();
        data.totalPesertaLes = countPesertaLes();

        updateDashboardProgress("Mengambil data mata pelajaran, jenis jadwal, dan master pendukung jadwal...", 24);

        data.totalKurikulum = countGeneric(KurikulumSekolah.class, true, false, false);
        data.totalMapel = countGeneric(Matapelajaran.class, true, false, false);
        data.totalJenisPenilaian = countGeneric(JenisPenilaian.class, true, false, false);
        data.totalGrupPenilaian = countGeneric(GrupPenilaian.class, true, false, false);
        data.totalNilaiHuruf = countGeneric(NilaiHurufSekolah.class, false, true, true);

        updateDashboardProgress("Mengambil jadwal pelajaran, jam pelajaran, dan masa jadwal...", 38);

        data.totalJadwal = countGeneric(JadwalPelajaran.class, true, true, true);
        data.totalJadwalSemester = data.totalJadwal;
        data.totalJamPelajaran = countGeneric(JamPelajaran.class, true, false, false);
        data.totalMasaJadwal = countGeneric(MasaJadwalPelajaran.class, true, true, true);
        data.totalMapelTerjadwal = countDistinctJadwal("matapelajaran");
        data.totalGuruTerjadwal = countDistinctJadwal("guru");
        data.totalKelasTerjadwal = countDistinctJadwal("kelas");

        updateDashboardProgress("Mengambil pertemuan, diskusi, grup pertemuan, dan ujian yang terhubung jadwal...", 48);

        data.totalPertemuan = countPertemuan(null, null);
        data.totalPertemuanJadwal = countPertemuan("jadwalPelajaran", null);
        data.totalPertemuanKelasLes = countPertemuan("kelasLesSiswa", null);
        data.totalDiskusi = countPertemuanChild(PertemuanPunyaDiskusi.class);
        data.totalGrupPertemuan = countPertemuanChild(PertemuanPunyaGrupPertemuan.class);
        data.totalUjian = countPertemuanChild(PertemuanPunyaUjian.class);

        updateDashboardProgress("Mengambil indikator kualitas data jadwal pelajaran...", 56);

        data.totalCatatanSiswa = countGeneric(CatatanSiswa.class, false, true, true);
        data.totalCatatanDenganGuru = countCatatanDenganRelasi("guru");
        data.totalCatatanDenganKelas = countCatatanDenganRelasi("kelasSiswa");
        data.totalSiswaDenganKelas = countSiswaDenganKelas();

        data.rasioKelasTerisi = percent(data.totalSiswaDenganKelas, data.totalSiswa);
        data.rasioJadwalKelas = percent(data.totalKelasTerjadwal, data.totalKelas);
        data.rasioMapelTerjadwal = percent(data.totalMapelTerjadwal, data.totalMapel);
        data.rasioGuruMengajar = percent(data.totalGuruTerjadwal, data.totalGuru);

        updateDashboardProgress("Menghitung Top Mapel, Top Guru, Top Kelas, Top Hari, dan tren jadwal...", 66);

        data.topKelasPeserta = loadTopKelasPeserta();
        data.topKelasLesPeserta = loadTopKelasLesPeserta();
        data.topMapelJadwal = loadTopJadwalGroup("matapelajaran", "nama");
        data.topGuruJadwal = loadTopJadwalGroup("guru", "namaGuru");
        data.topKelasJadwal = loadTopJadwalGroup("kelas", "nama");
        data.topHariJadwal = loadTopJadwalByHari();
        data.topJenisJadwal = loadTopJenisJadwal();
        data.topCatatanKelas = loadTopCatatanKelas();
        data.topTingkatKelas = loadTopKelasByTingkat();

        updateDashboardProgress("Menghitung tren siswa, kelas, jadwal, catatan, dan peserta kelas...", 82);

        List<Integer> years = buildYearList();
        for (int i = 0; i < years.size(); i++) {
            Integer tahun = years.get(i);
            List<String> tahunAjaran = buildTahunAjaranList(tahun.intValue(), tahun.intValue());
            data.trendSiswa.add(new DashboardMiniRow(String.valueOf(tahun), countSiswaByTahun(tahun)));
            data.trendKelas.add(new DashboardMiniRow(labelTahunAjaranSingkat(tahun.intValue()), countKelasByTahunAjaran(tahunAjaran)));
            data.trendJadwal.add(new DashboardMiniRow(labelTahunAjaranSingkat(tahun.intValue()), countJadwalByTahunAjaran(tahunAjaran)));
            data.trendPertemuan.add(new DashboardMiniRow(labelTahunAjaranSingkat(tahun.intValue()), countPertemuanByTahunAjaran(tahunAjaran)));
            data.trendCatatan.add(new DashboardMiniRow(labelTahunAjaranSingkat(tahun.intValue()), countCatatanByTahunAjaran(tahunAjaran)));
            data.trendPeserta.add(new DashboardMiniRow(labelTahunAjaranSingkat(tahun.intValue()), countPesertaKelasByTahunAjaran(tahunAjaran)));
        }

        sortAndLimit(data.topKelasPeserta, TOP_LIMIT);
        sortAndLimit(data.topKelasLesPeserta, TOP_LIMIT);
        sortAndLimit(data.topMapelJadwal, TOP_LIMIT);
        sortAndLimit(data.topGuruJadwal, TOP_LIMIT);
        sortAndLimit(data.topKelasJadwal, TOP_LIMIT);
        sortAndLimit(data.topHariJadwal, TOP_LIMIT);
        sortAndLimit(data.topJenisJadwal, TOP_LIMIT);
        sortAndLimit(data.topCatatanKelas, TOP_LIMIT);
        sortAndLimit(data.topTingkatKelas, TOP_LIMIT);

        return data;
    }

    private void renderOverview(DashboardJadwalData data) {
        appendHtml(body, sectionIntroHtml("Overview Jadwal Pelajaran",
                "Ringkasan utama kesiapan jadwal pelajaran, jam pelajaran, masa jadwal, guru, kelas, mapel, dan pertemuan. Angka utama dapat diklik untuk membuka contoh data detail."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Jadwal Pelajaran", data.totalJadwal, "Jadwal sesuai filter", "#1d4ed8", "JP", "JADWAL");
        appendMetricCard(cards, "Jam Pelajaran", data.totalJamPelajaran, "Master jam aktif", "#0f766e", "JM", "JAM");
        appendMetricCard(cards, "Masa Jadwal", data.totalMasaJadwal, "Periode jadwal", "#7c3aed", "MJ", "MASA_JADWAL");
        appendMetricCard(cards, "Mapel Terjadwal", data.totalMapelTerjadwal, data.rasioMapelTerjadwal + "% dari mapel", "#0891b2", "MP", "JADWAL");
        appendMetricCard(cards, "Guru Terjadwal", data.totalGuruTerjadwal, data.rasioGuruMengajar + "% dari guru", "#16a34a", "GT", "JADWAL");
        appendMetricCard(cards, "Kelas Terjadwal", data.totalKelasTerjadwal, data.rasioJadwalKelas + "% dari kelas", "#f59e0b", "KT", "JADWAL");
        appendMetricCard(cards, "Kelas Reguler", data.totalKelas, "Kelas aktif", "#2563eb", "K", "KELAS");
        appendMetricCard(cards, "Kelas Les", data.totalKelasLes, "Kelas tambahan/les", "#ea580c", "KL", "KELAS_LES");
        appendMetricCard(cards, "Peserta Kelas", data.totalPesertaKelas, "Relasi kelas-siswa", "#0ea5e9", "PK", "PESERTA_KELAS");
        appendMetricCard(cards, "Pertemuan", data.totalPertemuan, "Pertemuan dari jadwal", "#dc2626", "PT", "PERTEMUAN");
        appendMetricCard(cards, "Diskusi", data.totalDiskusi, "Diskusi pertemuan", "#9333ea", "DS", "DISKUSI");
        appendMetricCard(cards, "Ujian", data.totalUjian, "Ujian pada pertemuan", "#64748b", "UJ", "UJIAN");
    }

    private void renderAkademikPembelajaran(DashboardJadwalData data) {
        appendHtml(body, sectionIntroHtml("Pembelajaran & Jadwal",
                "Membantu melihat kesiapan jadwal, keterlibatan guru, pemetaan mata pelajaran, dan keterisian kelas."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Jam Pelajaran", data.totalJamPelajaran, "Master jam pelajaran aktif", "#0f766e", "JM", "JAM");
        appendMetricCard(cards, "Masa Jadwal", data.totalMasaJadwal, "Periode masa jadwal", "#2563eb", "MJ", "MASA_JADWAL");
        appendMetricCard(cards, "Mapel Terjadwal", data.totalMapelTerjadwal, data.rasioMapelTerjadwal + "% dari mapel aktif", "#7c3aed", "MT", "JADWAL");
        appendMetricCard(cards, "Guru Mengajar", data.totalGuruTerjadwal, data.rasioGuruMengajar + "% dari guru aktif", "#16a34a", "GM", "JADWAL");
        appendMetricCard(cards, "Kelas Terjadwal", data.totalKelasTerjadwal, data.rasioJadwalKelas + "% dari kelas aktif", "#f59e0b", "KT", "JADWAL");
        appendMetricCard(cards, "Siswa Punya Kelas", data.totalSiswaDenganKelas, data.rasioKelasTerisi + "% dari siswa aktif", "#dc2626", "SK", "PESERTA_KELAS");

        String panelKesiapan = dashboardExplainHtml("Kesiapan pembelajaran dihitung dari jumlah master aktif dan keterhubungan data jadwal. Makin tinggi rasio mapel/guru/kelas terjadwal, makin lengkap data akademik berjalan.")
                + progressSectionHtml("Kesiapan Jadwal", data.totalJadwal <= 0 ? data.totalKelas : data.totalJadwal,
                        new DashboardMiniRow("Mata Pelajaran Aktif", data.totalMapel),
                        new DashboardMiniRow("Mapel Terjadwal", data.totalMapelTerjadwal),
                        new DashboardMiniRow("Guru Terjadwal", data.totalGuruTerjadwal),
                        new DashboardMiniRow("Kelas Terjadwal", data.totalKelasTerjadwal));

        String panelKeterisian = dashboardExplainHtml("Keterisian kelas dihitung dari relasi KelasSiswaPunyaSiswa dan siswa aktif. Angka ini berguna untuk mendeteksi siswa yang belum masuk kelas pada tahun ajaran terkait.")
                + miniStatHtml("Siswa Aktif", data.totalSiswa, "Sesuai sekolah dan tahun masuk")
                + miniStatHtml("Siswa Punya Kelas", data.totalSiswaDenganKelas, "Terdeteksi pada relasi kelas")
                + miniStatHtml("Peserta Kelas", data.totalPesertaKelas, "Total relasi kelas-siswa")
                + miniStatHtml("Rasio Kelas Terisi", data.rasioKelasTerisi, "% siswa sudah terhubung kelas");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(panelKesiapan) + dashboardPanelHtml(panelKeterisian) + "</div>");
    }

    private void renderPenilaianCatatan(DashboardJadwalData data) {
        appendHtml(body, sectionIntroHtml("Pertemuan, Diskusi, Grup & Ujian",
                "Bagian ini mengambil aktivitas pembelajaran yang terhubung ke JadwalPelajaran atau KelasLesSiswa, sehingga operator dapat melihat apakah jadwal sudah diturunkan menjadi pertemuan nyata."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Total Pertemuan", data.totalPertemuan, "Pertemuan sesuai periode", "#1d4ed8", "PT", "PERTEMUAN");
        appendMetricCard(cards, "Pertemuan Jadwal", data.totalPertemuanJadwal, "Memiliki JadwalPelajaran", "#0f766e", "PJ", "PERTEMUAN");
        appendMetricCard(cards, "Pertemuan Les", data.totalPertemuanKelasLes, "Memiliki KelasLesSiswa", "#f59e0b", "PL", "PERTEMUAN");
        appendMetricCard(cards, "Diskusi", data.totalDiskusi, "Komentar/diskusi pertemuan", "#7c3aed", "DS", "DISKUSI");
        appendMetricCard(cards, "Grup Pertemuan", data.totalGrupPertemuan, "Relasi grup pertemuan", "#16a34a", "GP", "GRUP_PERTEMUAN");
        appendMetricCard(cards, "Ujian", data.totalUjian, "Ujian pada pertemuan", "#dc2626", "UJ", "UJIAN");

        String panelPertemuan = dashboardExplainHtml("Pertemuan dihitung dari model Pertemuan yang mempunyai relasi JadwalPelajaran atau KelasLesSiswa. Rasio ini membantu melihat apakah jadwal yang sudah dibuat telah benar-benar memiliki pertemuan pembelajaran.")
                + progressSectionHtml("Konversi Jadwal ke Pertemuan", data.totalJadwal <= 0 ? 1 : data.totalJadwal,
                        new DashboardMiniRow("Jadwal Pelajaran", data.totalJadwal),
                        new DashboardMiniRow("Pertemuan Jadwal", data.totalPertemuanJadwal),
                        new DashboardMiniRow("Diskusi", data.totalDiskusi),
                        new DashboardMiniRow("Ujian", data.totalUjian));

        String panelAktivitas = dashboardExplainHtml("Diskusi, grup pertemuan, dan ujian adalah indikator aktivitas lanjutan dari jadwal. Bila jadwal banyak tetapi aktivitas sedikit, operator dapat memeriksa pembuatan pertemuan atau sinkronisasi kelas.")
                + miniStatHtml("Diskusi", data.totalDiskusi, "PertemuanPunyaDiskusi")
                + miniStatHtml("Grup Pertemuan", data.totalGrupPertemuan, "PertemuanPunyaGrupPertemuan")
                + miniStatHtml("Ujian", data.totalUjian, "PertemuanPunyaUjian");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(panelPertemuan) + dashboardPanelHtml(panelAktivitas) + "</div>");
    }

    private void renderTopTablesAndTrends(DashboardJadwalData data) {
        appendHtml(body, sectionIntroHtml("Top Data & Tren Jadwal Pelajaran",
                "Daftar ini membantu operator melihat mapel paling sering dijadwalkan, guru paling banyak mengajar, kelas paling padat jadwal, serta tren jadwal/pertemuan lintas tahun ajaran."));

        Div grid = new Div();
        grid.setParent(body);
        grid.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;");

        appendMiniTable(grid, "Top Mata Pelajaran Terjadwal", "Berdasarkan JadwalPelajaran", data.topMapelJadwal, "Mata Pelajaran", "Jadwal");
        appendMiniTable(grid, "Top Guru Terjadwal", "Berdasarkan guru utama di JadwalPelajaran", data.topGuruJadwal, "Guru", "Jadwal");
        appendMiniTable(grid, "Top Kelas Terjadwal", "Berdasarkan JadwalPelajaran", data.topKelasJadwal, "Kelas", "Jadwal");
        appendMiniTable(grid, "Top Hari Jadwal", "Berdasarkan hari utama pada jadwal", data.topHariJadwal, "Hari", "Jadwal");
        appendMiniTable(grid, "Top Jenis Jadwal/Jam", "Berdasarkan JenisJadwalPelajaran pada JamPelajaran", data.topJenisJadwal, "Jenis", "Jam");
        appendMiniTable(grid, "Top Kelas Berdasarkan Peserta", "Relasi kelas-siswa aktif", data.topKelasPeserta, "Kelas", "Peserta");
        appendMiniTable(grid, "Top Kelas Les Berdasarkan Peserta", "Relasi kelas les-siswa aktif", data.topKelasLesPeserta, "Kelas Les", "Peserta");
        appendMiniTable(grid, "Distribusi Tingkat Kelas", "Berdasarkan KelasSiswa.tingkat", data.topTingkatKelas, "Tingkat", "Kelas");
        appendMiniTable(grid, "Tren Jadwal", "Berdasarkan tahun ajaran", data.trendJadwal, "Tahun Ajaran", "Jadwal");
        appendMiniTable(grid, "Tren Pertemuan", "Berdasarkan tahun ajaran", data.trendPertemuan, "Tahun Ajaran", "Pertemuan");
        appendMiniTable(grid, "Tren Kelas", "Berdasarkan tahun ajaran", data.trendKelas, "Tahun Ajaran", "Kelas");
        appendMiniTable(grid, "Tren Peserta Kelas", "Berdasarkan tahun ajaran", data.trendPeserta, "Tahun Ajaran", "Peserta");
    }

    private void renderRecommendation(DashboardJadwalData data) {
        StringBuilder sb = new StringBuilder();
        sb.append(sectionIntroHtml("Rekomendasi Operasional Jadwal",
                "Rekomendasi ringan berdasarkan rasio kesiapan jadwal, guru terjadwal, mapel terjadwal, dan konversi jadwal menjadi pertemuan."));
        sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:12px; margin-top:12px;'>");
        sb.append(recommendationCardHtml("Lengkapi Jadwal Kelas", data.rasioJadwalKelas >= 80 ? "Baik" : "Perlu Dicek",
                data.rasioJadwalKelas >= 80 ? "Sebagian besar kelas sudah memiliki jadwal pelajaran." : "Masih ada kelas aktif yang belum terlihat memiliki JadwalPelajaran pada periode terpilih.",
                data.rasioJadwalKelas >= 80 ? "#16a34a" : "#f59e0b"));
        sb.append(recommendationCardHtml("Mapping Guru", data.rasioGuruMengajar >= 70 ? "Baik" : "Perlu Mapping",
                data.rasioGuruMengajar >= 70 ? "Guru aktif sudah cukup banyak terhubung ke jadwal." : "Periksa guru yang belum muncul pada jadwal, terutama guru mapel utama dan guru pengganti.",
                data.rasioGuruMengajar >= 70 ? "#16a34a" : "#dc2626"));
        sb.append(recommendationCardHtml("Mapping Mapel", data.rasioMapelTerjadwal >= 80 ? "Lengkap" : "Perlu Dilengkapi",
                data.rasioMapelTerjadwal >= 80 ? "Mata pelajaran aktif mayoritas sudah muncul pada jadwal." : "Periksa mata pelajaran aktif yang belum masuk ke jadwal pelajaran.",
                data.rasioMapelTerjadwal >= 80 ? "#16a34a" : "#f59e0b"));
        sb.append(recommendationCardHtml("Pertemuan", data.totalJadwal <= 0 || data.totalPertemuanJadwal >= data.totalJadwal ? "Aman" : "Pantau",
                data.totalJadwal <= 0 || data.totalPertemuanJadwal >= data.totalJadwal ? "Pertemuan jadwal sudah sebanding atau lebih banyak dari master jadwal." : "Jadwal sudah tersedia, namun pertemuan yang terhubung ke JadwalPelajaran masih perlu dipantau.",
                data.totalJadwal <= 0 || data.totalPertemuanJadwal >= data.totalJadwal ? "#16a34a" : "#f59e0b"));
        sb.append("</div>");
        appendHtml(body, sb.toString());
    }

    private long countGeneric(Class clazz, boolean filterAktif, boolean filterTahun, boolean filterSemester) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz);
            if (filterAktif && hasProperty(clazz, "aktif")) {
                c.add(activeCriterion());
            }
            applySekolahFilter(c, clazz, null);
            if (filterTahun) {
                applyTahunFilter(c, clazz, null);
            }
            if (filterSemester) {
                applySemesterFilter(c, clazz, null);
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countGeneric " + clazz, e);
            return 0;
        }
    }

    private long countPesertaKelas() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("kelasSiswa", "kelas");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("kelas.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countPesertaKelas", e);
            return 0;
        }
    }

    private long countPesertaLes() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasLesSiswaPunyaSiswa.class, "klps");
            c.createAlias("kelasLesSiswa", "kelasLes");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("kelasLes.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelasLes.sekolah", currentSekolah));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countPesertaLes", e);
            return 0;
        }
    }

    private long countDistinctJadwal(String property) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class);
            c.add(activeCriterion());
            applySekolahFilter(c, JadwalPelajaran.class, null);
            applyTahunFilter(c, JadwalPelajaran.class, null);
            applySemesterFilter(c, JadwalPelajaran.class, null);
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelas", currentKelas));
            }
            c.add(Restrictions.isNotNull(property));
            c.setProjection(Projections.countDistinct(property));
            Number n = (Number) c.uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countDistinctJadwal " + property, e);
            return 0;
        }
    }


    private long countPertemuan(String mustNotNullProperty, List<String> tahunAjaran) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Pertemuan.class, "pertemuan");
            if (hasProperty(Pertemuan.class, "aktif")) {
                c.add(activeCriterion("aktif"));
            }
            applyPertemuanFilter(c, "pertemuan", tahunAjaran);
            if (mustNotNullProperty != null && mustNotNullProperty.trim().length() > 0) {
                c.add(Restrictions.isNotNull(mustNotNullProperty));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countPertemuan " + mustNotNullProperty, e);
            return 0;
        }
    }

    private long countPertemuanByTahunAjaran(List<String> tahunAjaran) {
        return countPertemuan(null, tahunAjaran);
    }

    private long countPertemuanChild(Class clazz) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(clazz, "child");
            c.createAlias("pertemuan", "pertemuan");
            applyPertemuanFilter(c, "pertemuan", null);
            return getCount(c);
        } catch (Exception e) {
            debugError("countPertemuanChild " + clazz, e);
            return 0;
        }
    }

    private void applyPertemuanFilter(Criteria c, String alias, List<String> tahunAjaranOverride) {
        try {
            String prefix = alias == null || alias.trim().length() == 0 ? "" : alias + ".";
            List<String> tahunAjaran = tahunAjaranOverride == null ? buildTahunAjaranList(currentMulai, currentSampai) : tahunAjaranOverride;
            if (currentSekolah != null) {
                c.add(Restrictions.eq(prefix + "sekolah", currentSekolah));
            }
            if (tahunAjaran != null && !tahunAjaran.isEmpty() && hasProperty(Pertemuan.class, "ta")) {
                c.add(Restrictions.in(prefix + "ta", tahunAjaran));
            }
            if (currentSemester != null && hasProperty(Pertemuan.class, "smt")) {
                Disjunction d = Restrictions.disjunction();
                d.add(Restrictions.eq(prefix + "smt", String.valueOf(currentSemester)));
                d.add(Restrictions.eq(prefix + "smt", currentSemester.intValue() == 1 ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP));
                c.add(d);
            }
            if (currentKelas != null) {
                c.createAlias(prefix + "jadwalPelajaran", "jadwalFilter");
                c.add(Restrictions.eq("jadwalFilter.kelas", currentKelas));
            }
        } catch (Exception e) {
            debugError("applyPertemuanFilter", e);
        }
    }

    private long countCatatanDenganRelasi(String property) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(CatatanSiswa.class);
            applySekolahFilter(c, CatatanSiswa.class, null);
            applyTahunFilter(c, CatatanSiswa.class, null);
            applySemesterFilter(c, CatatanSiswa.class, null);
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            c.add(Restrictions.isNotNull(property));
            return getCount(c);
        } catch (Exception e) {
            debugError("countCatatanDenganRelasi " + property, e);
            return 0;
        }
    }

    private long countSiswaDenganKelas() {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("kelasSiswa", "kelas");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("kelas.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            c.add(Restrictions.isNotNull("siswa"));
            c.setProjection(Projections.countDistinct("siswa"));
            Number n = (Number) c.uniqueResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            debugError("countSiswaDenganKelas", e);
            return 0;
        }
    }

    private long countSiswaByTahun(Integer tahun) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
            c.add(activeCriterion());
            if (tahun != null) {
                c.add(Restrictions.eq("tahunMasuk", tahun));
            }
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countSiswaByTahun", e);
            return 0;
        }
    }

    private long countKelasByTahunAjaran(List<String> tahunAjaran) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswa.class);
            c.add(activeCriterion());
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("tahunAjaran", tahunAjaran));
            }
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countKelasByTahunAjaran", e);
            return 0;
        }
    }

    private long countJadwalByTahunAjaran(List<String> tahunAjaran) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class);
            c.add(activeCriterion());
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("tahunAjaran", tahunAjaran));
            }
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            if (currentSemester != null) {
                c.add(Restrictions.eq("semester", currentSemester));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countJadwalByTahunAjaran", e);
            return 0;
        }
    }

    private long countCatatanByTahunAjaran(List<String> tahunAjaran) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(CatatanSiswa.class);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("tahunAjaran", tahunAjaran));
            }
            if (currentSekolah != null) {
                c.add(Restrictions.eq("sekolah", currentSekolah));
            }
            if (currentSemester != null) {
                c.add(Restrictions.eq("semester", currentSemester));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countCatatanByTahunAjaran", e);
            return 0;
        }
    }

    private long countPesertaKelasByTahunAjaran(List<String> tahunAjaran) {
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("kelasSiswa", "kelas");
            c.add(activeCriterion("aktif"));
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            return getCount(c);
        } catch (Exception e) {
            debugError("countPesertaKelasByTahunAjaran", e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopKelasPeserta() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
            c.createAlias("kelasSiswa", "kelas");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("kelas.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
            }
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
            }
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("kelas.nama"))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopKelasPeserta", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopKelasLesPeserta() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasLesSiswaPunyaSiswa.class, "klps");
            c.createAlias("kelasLesSiswa", "kelasLes");
            c.add(activeCriterion("aktif"));
            c.add(activeCriterion("kelasLes.aktif"));
            if (currentSekolah != null) {
                c.add(Restrictions.eq("kelasLes.sekolah", currentSekolah));
            }
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("kelasLes.nama"))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopKelasLesPeserta", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopJadwalGroup(String association, String labelProperty) {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class, "jadwal");
            c.createAlias(association, "x");
            c.add(activeCriterion("aktif"));
            applySekolahFilter(c, JadwalPelajaran.class, null);
            applyTahunFilter(c, JadwalPelajaran.class, null);
            applySemesterFilter(c, JadwalPelajaran.class, null);
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelas", currentKelas));
            }
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("x." + labelProperty))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopJadwalGroup " + association, e);
        }
        return rows;
    }


    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopJadwalByHari() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class, "jadwal");
            c.add(activeCriterion("aktif"));
            applySekolahFilter(c, JadwalPelajaran.class, null);
            applyTahunFilter(c, JadwalPelajaran.class, null);
            applySemesterFilter(c, JadwalPelajaran.class, null);
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelas", currentKelas));
            }
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("hari"))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopJadwalByHari", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopJenisJadwal() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(JamPelajaran.class, "jam");
            c.createAlias("jenisJadwalPelajaran", "jenis");
            c.add(activeCriterion("aktif"));
            applySekolahFilter(c, JamPelajaran.class, null);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("jenis.nama"))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopJenisJadwal", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopCatatanKelas() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(CatatanSiswa.class, "catatan");
            c.createAlias("kelasSiswa", "kelas");
            applySekolahFilter(c, CatatanSiswa.class, null);
            applyTahunFilter(c, CatatanSiswa.class, null);
            applySemesterFilter(c, CatatanSiswa.class, null);
            if (currentKelas != null) {
                c.add(Restrictions.eq("kelasSiswa", currentKelas));
            }
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("kelas.nama"))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(safeText(o[0]), toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopCatatanKelas", e);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<DashboardMiniRow> loadTopKelasByTingkat() {
        List<DashboardMiniRow> rows = new ArrayList<DashboardMiniRow>();
        try {
            Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswa.class);
            c.add(activeCriterion());
            applySekolahFilter(c, KelasSiswa.class, null);
            applyTahunFilter(c, KelasSiswa.class, null);
            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("tingkat"))
                    .add(Projections.count("id")));
            List<Object[]> result = c.list();
            for (Object[] o : result) {
                rows.add(new DashboardMiniRow(o[0] == null ? "Tidak Diisi" : "Tingkat " + o[0], toLong(o[1])));
            }
        } catch (Exception e) {
            debugError("loadTopKelasByTingkat", e);
        }
        return rows;
    }

    private void applySekolahFilter(Criteria c, Class clazz, String aliasPrefix) {
        try {
            if (currentSekolah == null) {
                return;
            }
            if (hasProperty(clazz, "sekolah")) {
                c.add(Restrictions.eq(prefix(aliasPrefix, "sekolah"), currentSekolah));
            }
        } catch (Exception e) {
            debugError("applySekolahFilter " + clazz, e);
        }
    }

    private void applyTahunFilter(Criteria c, Class clazz, String aliasPrefix) {
        try {
            List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
            if (hasProperty(clazz, "tahunAjaran")) {
                c.add(Restrictions.in(prefix(aliasPrefix, "tahunAjaran"), tahunAjaran));
            } else if (hasProperty(clazz, "tahunAkademik")) {
                c.add(Restrictions.in(prefix(aliasPrefix, "tahunAkademik"), tahunAjaran));
            } else if (hasProperty(clazz, "tahunMasuk")) {
                c.add(Restrictions.ge(prefix(aliasPrefix, "tahunMasuk"), new Integer(currentMulai)));
                c.add(Restrictions.le(prefix(aliasPrefix, "tahunMasuk"), new Integer(currentSampai)));
            } else if (hasProperty(clazz, "ta")) {
                c.add(Restrictions.ge(prefix(aliasPrefix, "ta"), new Integer(currentMulai)));
                c.add(Restrictions.le(prefix(aliasPrefix, "ta"), new Integer(currentSampai)));
            }
        } catch (Exception e) {
            debugError("applyTahunFilter " + clazz, e);
        }
    }

    private void applySemesterFilter(Criteria c, Class clazz, String aliasPrefix) {
        try {
            if (currentSemester == null || !hasProperty(clazz, "semester")) {
                return;
            }
            Class type = getPropertyType(clazz, "semester");
            if (Integer.class.equals(type) || int.class.equals(type)) {
                c.add(Restrictions.eq(prefix(aliasPrefix, "semester"), currentSemester));
            } else {
                c.add(Restrictions.eq(prefix(aliasPrefix, "semester"), String.valueOf(currentSemester)));
            }
        } catch (Exception e) {
            debugError("applySemesterFilter " + clazz, e);
        }
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

    private List buildDetailList(String key) {
        try {
            if ("SEKOLAH".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(Sekolah.class);
                if (currentSekolah != null) {
                    c.add(Restrictions.eq("id", currentSekolah.getId()));
                }
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("SISWA".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(Siswa.class);
                c.add(activeCriterion());
                applySekolahFilter(c, Siswa.class, null);
                applyTahunFilter(c, Siswa.class, null);
                c.addOrder(Order.asc("namaSiswa"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("GURU".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(Guru.class);
                c.add(activeCriterion());
                applySekolahFilter(c, Guru.class, null);
                c.addOrder(Order.asc("namaGuru"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("KELAS".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswa.class);
                c.add(activeCriterion());
                applySekolahFilter(c, KelasSiswa.class, null);
                applyTahunFilter(c, KelasSiswa.class, null);
                if (currentKelas != null) {
                    c.add(Restrictions.eq("id", currentKelas.getId()));
                }
                c.addOrder(Order.asc("tingkat"));
                c.addOrder(Order.asc("nama"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("KELAS_LES".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(KelasLesSiswa.class);
                c.add(activeCriterion());
                applySekolahFilter(c, KelasLesSiswa.class, null);
                c.addOrder(Order.asc("tingkat"));
                c.addOrder(Order.asc("nama"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("PESERTA_KELAS".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class, "kps");
                c.createAlias("kelasSiswa", "kelas");
                c.add(activeCriterion("aktif"));
                if (currentSekolah != null) {
                    c.add(Restrictions.eq("kelas.sekolah", currentSekolah));
                }
                if (currentKelas != null) {
                    c.add(Restrictions.eq("kelasSiswa", currentKelas));
                }
                List<String> tahunAjaran = buildTahunAjaranList(currentMulai, currentSampai);
                if (tahunAjaran != null && !tahunAjaran.isEmpty()) {
                    c.add(Restrictions.in("kelas.tahunAjaran", tahunAjaran));
                }
                c.addOrder(Order.asc("nomorUrut"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("PESERTA_LES".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(KelasLesSiswaPunyaSiswa.class, "klps");
                c.createAlias("kelasLesSiswa", "kelasLes");
                c.add(activeCriterion("aktif"));
                if (currentSekolah != null) {
                    c.add(Restrictions.eq("kelasLes.sekolah", currentSekolah));
                }
                c.addOrder(Order.asc("nomorUrut"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("KURIKULUM".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(KurikulumSekolah.class);
                c.add(activeCriterion());
                applySekolahFilter(c, KurikulumSekolah.class, null);
                c.addOrder(Order.asc("nama"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("MAPEL".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(Matapelajaran.class);
                c.add(activeCriterion());
                applySekolahFilter(c, Matapelajaran.class, null);
                c.addOrder(Order.asc("nama"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("JADWAL".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class);
                c.add(activeCriterion());
                applySekolahFilter(c, JadwalPelajaran.class, null);
                applyTahunFilter(c, JadwalPelajaran.class, null);
                applySemesterFilter(c, JadwalPelajaran.class, null);
                if (currentKelas != null) {
                    c.add(Restrictions.eq("kelas", currentKelas));
                }
                c.addOrder(Order.asc("tahunAjaran"));
                c.addOrder(Order.asc("semester"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("JAM".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(JamPelajaran.class);
                c.add(activeCriterion());
                applySekolahFilter(c, JamPelajaran.class, null);
                c.addOrder(Order.asc("nama"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("MASA_JADWAL".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(MasaJadwalPelajaran.class);
                c.add(activeCriterion());
                applySekolahFilter(c, MasaJadwalPelajaran.class, null);
                applyTahunFilter(c, MasaJadwalPelajaran.class, null);
                applySemesterFilter(c, MasaJadwalPelajaran.class, null);
                c.addOrder(Order.desc("mulai"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("CATATAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(CatatanSiswa.class);
                applySekolahFilter(c, CatatanSiswa.class, null);
                applyTahunFilter(c, CatatanSiswa.class, null);
                applySemesterFilter(c, CatatanSiswa.class, null);
                if (currentKelas != null) {
                    c.add(Restrictions.eq("kelasSiswa", currentKelas));
                }
                c.addOrder(Order.desc("waktu"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("JENIS_PENILAIAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(JenisPenilaian.class);
                c.add(activeCriterion());
                applySekolahFilter(c, JenisPenilaian.class, null);
                c.addOrder(Order.asc("jenis"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("GRUP_PENILAIAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(GrupPenilaian.class);
                c.add(activeCriterion());
                applySekolahFilter(c, GrupPenilaian.class, null);
                c.addOrder(Order.asc("nama"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("NILAI_HURUF".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(NilaiHurufSekolah.class);
                applySekolahFilter(c, NilaiHurufSekolah.class, null);
                applyTahunFilter(c, NilaiHurufSekolah.class, null);
                applySemesterFilter(c, NilaiHurufSekolah.class, null);
                c.addOrder(Order.asc("mulai"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("PERTEMUAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(Pertemuan.class, "pertemuan");
                if (hasProperty(Pertemuan.class, "aktif")) {
                    c.add(activeCriterion("aktif"));
                }
                applyPertemuanFilter(c, "pertemuan", null);
                c.addOrder(Order.desc("tanggal"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("DISKUSI".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(PertemuanPunyaDiskusi.class, "child");
                c.createAlias("pertemuan", "pertemuan");
                applyPertemuanFilter(c, "pertemuan", null);
                c.addOrder(Order.desc("tanggal_dirubah"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("GRUP_PERTEMUAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(PertemuanPunyaGrupPertemuan.class, "child");
                c.createAlias("pertemuan", "pertemuan");
                applyPertemuanFilter(c, "pertemuan", null);
                c.addOrder(Order.desc("id"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
            if ("UJIAN".equals(key)) {
                Criteria c = HibernateUtil.currentSession().createCriteria(PertemuanPunyaUjian.class, "child");
                c.createAlias("pertemuan", "pertemuan");
                applyPertemuanFilter(c, "pertemuan", null);
                c.addOrder(Order.desc("mulaiUjian"));
                c.setMaxResults(DETAIL_LIMIT);
                return c.list();
            }
        } catch (Exception e) {
            debugError("buildDetailList " + key, e);
        }
        return new ArrayList();
    }

    private void openDetail(String key, String title) {
        try {
            Window window = new Window();
            window.setTitle(title == null || title.trim().length() == 0 ? "Detail Dasbor Jadwal Pelajaran" : title);
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
        if ("SISWA".equals(key)) {
            return new String[] { "NIS", "Nama", "Sekolah", "Tahun Masuk", "Status" };
        }
        if ("GURU".equals(key)) {
            return new String[] { "Kode", "Nama Guru", "Sekolah", "HP", "Status" };
        }
        if ("KELAS".equals(key)) {
            return new String[] { "Kelas", "Tingkat", "Tahun Ajaran", "Sekolah", "Guru Pembina" };
        }
        if ("KELAS_LES".equals(key)) {
            return new String[] { "Kelas Les", "Tingkat", "Mapel", "Sekolah", "Guru Pembina" };
        }
        if ("PESERTA_KELAS".equals(key)) {
            return new String[] { "Kelas", "No", "Siswa", "NIS", "Aktif" };
        }
        if ("PESERTA_LES".equals(key)) {
            return new String[] { "Kelas Les", "No", "Siswa", "NIS", "Aktif" };
        }
        if ("JADWAL".equals(key)) {
            return new String[] { "Tahun Ajaran", "Semester", "Kelas", "Mapel", "Guru", "Hari" };
        }
        if ("CATATAN".equals(key)) {
            return new String[] { "Waktu", "Siswa", "Kelas", "Guru", "Catatan" };
        }
        if ("NILAI_HURUF".equals(key)) {
            return new String[] { "Nilai", "Mulai", "Sampai", "Tahun", "Semester" };
        }
        if ("JAM".equals(key)) {
            return new String[] { "Nama", "Mulai", "Selesai", "JP", "Sekolah" };
        }
        if ("MASA_JADWAL".equals(key)) {
            return new String[] { "Nama", "Mulai", "Sampai", "Tahun Ajaran", "Semester" };
        }
        if ("PERTEMUAN".equals(key)) {
            return new String[] { "Tanggal", "Ke", "Topik", "Jadwal", "Kelas Les", "Status" };
        }
        if ("DISKUSI".equals(key)) {
            return new String[] { "Pertemuan", "Siswa", "Guru", "User", "Isi" };
        }
        if ("GRUP_PERTEMUAN".equals(key)) {
            return new String[] { "Nama", "Pertemuan", "Grup", "Mahasiswa", "Keterangan" };
        }
        if ("UJIAN".equals(key)) {
            return new String[] { "Nama", "Pertemuan", "Mulai", "Sampai", "Aktif" };
        }
        if ("SEKOLAH".equals(key)) {
            return new String[] { "Kode", "Nama", "Keterangan", "Aktif" };
        }
        return new String[] { "Nama", "Sekolah", "Keterangan", "Aktif" };
    }

    private String[] detailProperties(String key) {
        if ("SISWA".equals(key)) {
            return new String[] { "nomorInduk", "namaSiswa", "sekolah", "tahunMasuk", "aktif" };
        }
        if ("GURU".equals(key)) {
            return new String[] { "kode", "namaGuru", "sekolah", "hp", "aktif" };
        }
        if ("KELAS".equals(key)) {
            return new String[] { "nama", "tingkat", "tahunAjaran", "sekolah", "guruPembina" };
        }
        if ("KELAS_LES".equals(key)) {
            return new String[] { "nama", "tingkat", "matapelajaran", "sekolah", "guruPembina" };
        }
        if ("PESERTA_KELAS".equals(key)) {
            return new String[] { "kelasSiswa", "nomorUrut", "siswa.namaSiswa", "siswa.nomorInduk", "aktif" };
        }
        if ("PESERTA_LES".equals(key)) {
            return new String[] { "kelasLesSiswa", "nomorUrut", "siswa.namaSiswa", "siswa.nomorInduk", "aktif" };
        }
        if ("JADWAL".equals(key)) {
            return new String[] { "tahunAjaran", "semester", "kelas", "matapelajaran", "guru", "hari" };
        }
        if ("CATATAN".equals(key)) {
            return new String[] { "waktu", "siswa", "kelasSiswa", "guru", "keterangan" };
        }
        if ("NILAI_HURUF".equals(key)) {
            return new String[] { "nilaiHuruf", "mulai", "sampai", "tahunAkademik", "semester" };
        }
        if ("JAM".equals(key)) {
            return new String[] { "nama", "mulai", "selesai", "jp", "sekolah" };
        }
        if ("MASA_JADWAL".equals(key)) {
            return new String[] { "nama", "mulai", "sampai", "tahunAjaran", "semester" };
        }
        if ("PERTEMUAN".equals(key)) {
            return new String[] { "tanggal", "pertemuanKe", "topik", "jadwalPelajaran", "kelasLesSiswa", "statusPertemuan" };
        }
        if ("DISKUSI".equals(key)) {
            return new String[] { "pertemuan", "siswa", "guru", "tbmuser", "isi" };
        }
        if ("GRUP_PERTEMUAN".equals(key)) {
            return new String[] { "nama", "pertemuan", "grupPertemuan", "mahasiswa", "keterangan" };
        }
        if ("UJIAN".equals(key)) {
            return new String[] { "nama", "pertemuan", "mulaiUjian", "sampaiUjian", "aktif" };
        }
        if ("SEKOLAH".equals(key)) {
            return new String[] { "kode", "nama", "keterangan", "aktif" };
        }
        return new String[] { "nama", "sekolah", "keterangan", "aktif" };
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

    private void populateSemesterCombo(Combobox combo) {
        clearCombo(combo);
        Comboitem semua = new Comboitem("Semua");
        semua.setValue(null);
        semua.setParent(combo);
        Comboitem s1 = new Comboitem("Semester 1");
        s1.setValue(new Integer(1));
        s1.setParent(combo);
        Comboitem s2 = new Comboitem("Semester 2");
        s2.setValue(new Integer(2));
        s2.setParent(combo);
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

    private Integer getSelectedSemester() {
        try {
            if (searchSemester != null && searchSemester.getSelectedItem() != null) {
                Object val = searchSemester.getSelectedItem().getValue();
                return val instanceof Integer ? (Integer) val : null;
            }
        } catch (Exception e) {
            debugError("getSelectedSemester", e);
        }
        return currentSemester;
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
            Object ida = getValueByPath(a, "id");
            Object idb = getValueByPath(b, "id");
            return ida != null && idb != null && ida.equals(idb);
        } catch (Exception e) {
            return a.equals(b);
        }
    }

    private List<Integer> buildYearList() {
        List<Integer> list = new ArrayList<Integer>();
        for (int tahun = currentMulai; tahun <= currentSampai; tahun++) {
            list.add(new Integer(tahun));
        }
        if (list.isEmpty()) {
            list.add(new Integer(Calendar.getInstance().get(Calendar.YEAR)));
        }
        return list;
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

    private boolean hasProperty(Class clazz, String property) {
        return getReadMethod(clazz, property) != null;
    }

    private Class getPropertyType(Class clazz, String property) {
        Method method = getReadMethod(clazz, property);
        return method == null ? Object.class : method.getReturnType();
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

    private Object getValueByPath(Object object, String path) {
        if (object == null || path == null || path.trim().length() == 0) {
            return "";
        }
        try {
            Object current = object;
            String[] parts = path.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                if (current == null) {
                    return "";
                }
                Method method = getReadMethod(current.getClass(), parts[i]);
                if (method == null) {
                    return "";
                }
                current = method.invoke(current, new Object[0]);
            }
            return formatObject(current);
        } catch (Exception e) {
            return "";
        }
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
            return Boolean.TRUE.equals(object) ? "Aktif/Ya" : "Tidak";
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
        return (int) Math.round((value * 100.0d) / total);
    }

    private void appendMetricCard(Component parent, String title, long value, String subtitle, String color, String icon, final String detailKey) {
        appendValueMetricCard(parent, title, formatNumber(value), subtitle, color, icon, detailKey);
    }

    private void appendValueMetricCard(Component parent, String title, String value, String subtitle, String color, String icon, final String detailKey) {
        final String cardTitle = title;
        Div card = new Div();
        card.setParent(parent);
        card.setStyle("position:relative; min-height:116px; box-sizing:border-box; padding:15px; border-radius:16px; "
                + "background:#ffffff; border:1px solid #e6edf5; box-shadow:0 10px 24px rgba(15,23,42,0.05); "
                + (detailKey == null ? "" : "cursor:pointer;") + " overflow:hidden;");
        card.appendChild(new Html("<div style='position:absolute; right:-18px; top:-20px; width:86px; height:86px; border-radius:999px; background:"
                + color + "; opacity:.10;'></div>"
                + "<div style='display:flex; align-items:center; justify-content:space-between; gap:8px;'>"
                + "<div style='font-size:12px; color:#64748b; font-weight:700;'>" + safeHtml(title) + "</div>"
                + "<div style='min-width:34px; height:34px; display:flex; align-items:center; justify-content:center; border-radius:12px; color:#fff; font-size:12px; font-weight:900; background:"
                + color + ";'>" + safeHtml(icon) + "</div></div>"
                + "<div style='font-size:25px; line-height:1.1; margin-top:12px; color:#0f172a; font-weight:900;'>" + safeHtml(value) + "</div>"
                + "<div style='font-size:11px; color:#64748b; margin-top:7px; line-height:1.35;'>" + safeHtml(subtitle) + "</div>"
                + (detailKey == null ? "" : "<div style='font-size:10px; color:" + color + "; font-weight:800; margin-top:8px;'>Klik untuk detail</div>")
                ));
        if (detailKey != null) {
            card.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    openDetail(detailKey, "Detail " + cardTitle);
                }
            });
        }
    }

    private void appendMiniTable(Component parent, String title, String subtitle, List<DashboardMiniRow> rows, String col1, String col2) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#ffffff; border:1px solid #e6edf5; border-radius:16px; box-shadow:0 10px 24px rgba(15,23,42,0.05); overflow:hidden;'>");
        sb.append("<div style='padding:13px 14px; border-bottom:1px solid #eef2f7;'>");
        sb.append("<div style='font-size:14px; font-weight:900; color:#0f172a;'>").append(safeHtml(title)).append("</div>");
        sb.append("<div style='font-size:11px; color:#64748b; margin-top:3px;'>").append(safeHtml(subtitle)).append("</div>");
        sb.append("</div>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px;'>");
        sb.append("<thead><tr style='background:#f8fafc; color:#475569;'>");
        sb.append("<th style='text-align:left; padding:9px 12px;'>").append(safeHtml(col1)).append("</th>");
        sb.append("<th style='text-align:right; padding:9px 12px; width:95px;'>").append(safeHtml(col2)).append("</th>");
        sb.append("</tr></thead><tbody>");
        if (rows == null || rows.isEmpty()) {
            sb.append("<tr><td colspan='2' style='padding:12px; color:#94a3b8;'>Belum ada data.</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                DashboardMiniRow row = rows.get(i);
                sb.append("<tr style='background:").append(i % 2 == 0 ? "#ffffff" : "#f8fafc").append(";'>");
                sb.append("<td style='padding:9px 12px; color:#0f172a;'>").append(safeHtml(row.label)).append("</td>");
                sb.append("<td style='padding:9px 12px; color:#0f766e; text-align:right; font-weight:900;'>").append(formatNumber(row.value)).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div>");
        appendHtml(parent, sb.toString());
    }

    private String sectionIntroHtml(String title, String desc) {
        return "<div style='margin-top:18px; padding:14px 16px; background:#ffffff; border:1px solid #e6edf5; border-radius:16px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.04);'>"
                + "<div style='font-size:17px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:#64748b; margin-top:4px; line-height:1.45;'>" + safeHtml(desc) + "</div>"
                + "</div>";
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
                + "<div style='font-size:18px; font-weight:900; color:#0f766e;'>" + formatNumber(value) + "</div>"
                + "</div>";
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
            System.err.println("[DasboardJadwalPelajaran DEBUG] " + context);
            if (e != null) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasboardJadwalPelajaran.java:2060");
            }
        }
    }

    private static class DashboardJadwalData {
        int tahunMulai;
        int tahunSampai;
        String tahunAjaranLabel;
        Integer semester;

        long totalSekolah;
        long totalGuru;
        long totalSiswa;
        long totalKelas;
        long totalKelasLes;
        long totalPesertaKelas;
        long totalPesertaLes;
        long totalKurikulum;
        long totalMapel;
        long totalJenisPenilaian;
        long totalGrupPenilaian;
        long totalNilaiHuruf;
        long totalJadwal;
        long totalJadwalSemester;
        long totalJamPelajaran;
        long totalMasaJadwal;
        long totalMapelTerjadwal;
        long totalGuruTerjadwal;
        long totalKelasTerjadwal;
        long totalPertemuan;
        long totalPertemuanJadwal;
        long totalPertemuanKelasLes;
        long totalDiskusi;
        long totalGrupPertemuan;
        long totalUjian;
        long totalCatatanSiswa;
        long totalCatatanDenganGuru;
        long totalCatatanDenganKelas;
        long totalSiswaDenganKelas;

        int rasioKelasTerisi;
        int rasioJadwalKelas;
        int rasioMapelTerjadwal;
        int rasioGuruMengajar;

        List<DashboardMiniRow> topKelasPeserta = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topKelasLesPeserta = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topMapelJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topGuruJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topKelasJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topHariJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topJenisJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topCatatanKelas = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topTingkatKelas = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendSiswa = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendKelas = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendJadwal = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPertemuan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendCatatan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPeserta = new ArrayList<DashboardMiniRow>();
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
