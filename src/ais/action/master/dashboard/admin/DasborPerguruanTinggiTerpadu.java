package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/* DASBOR_PERGURUAN_TINGGI_TERPADU_PLUS_HEALTH_2026_05_30
 * Update 6: default filter menjadi 3 tahun terakhir dan tambah dashboard kesehatan akademik,
 * mutu data akademik, siklus hidup mahasiswa, dan kapasitas prodi-dosen.
 *
 * DASBOR_PERGURUAN_TINGGI_TERPADU_LOADING_PROGRESS_V2_2026_05_30
 * Update 5: menampilkan loading indikator dan progress pengambilan data bertahap.
 *
 * DASBOR_PERGURUAN_TINGGI_TERPADU_PLUS_BIMBINGAN_LULUSAN_INDEX_READY_2026_05_30
 * Update 4: default filter tahun hanya tahun berjalan, tambah bimbingan, sidang, wisuda,
 * lulusan, status keluar, predikat kelulusan, status/pekerjaan/domisili setelah lulus,
 * masa studi, dan semester lulus.
 *
 * DASBOR_PERGURUAN_TINGGI_TERPADU_PLUS_PERKULIAHAN_KEHADIRAN_2026_05_29
 * Update: memakai baseline terbaru dari pengguna, termasuk Common.initFakultasDanJurusanData(...).
 * Tambahan 3: ringkasan perkuliahan, kehadiran dosen, kehadiran mahasiswa, dan pencapaian perkuliahan.
 * Tambahan 2: ringkasan dosen, beban SKS dosen, kegiatan/organisasi/prestasi/karya dosen,
 * publikasi ilmiah, penelitian dan pengabdian, serta buku ajar.
 * Tambahan 1: ringkasan IPK, KRS, Nilai, SKS kumulatif, kegiatan, organisasi, dan prestasi mahasiswa.
 *
 * DASBOR_PERGURUAN_TINGGI_TERPADU_2026_05_29
 * Dibuat sebagai pengganti panel ProfileAdminPerguruanTinggi pada DashboardData.
 * Struktur UI mengikuti pola DasboardSurat: hero, global filter, kartu ringkasan,
 * panel analitik, funnel, top prodi, tren, dan popup detail sederhana.
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BukuBahanAjar;
import ais.database.model.Dosen;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.PenghargaanDosen;
import ais.database.model.PrestasiDosen;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor tunggal terpadu untuk pimpinan perguruan tinggi: menggabungkan ringkasan dari
 * puluhan dashboard terpisah yang sebelumnya berdiri sendiri (kurikulum, PMB/SPMB, mahasiswa,
 * KRS/IPK/nilai, kegiatan kemahasiswaan, dosen, penelitian/publikasi/buku ajar, perkuliahan &
 * kehadiran, bimbingan/sidang/wisuda/lulusan, kesehatan akademik & mutu data) menjadi satu halaman
 * hero + filter global + rangkaian panel kartu/analitik/tren, menggantikan panel lama
 * {@code ProfileAdminPerguruanTinggi} pada {@code DashboardData} (mengikuti pola tata letak
 * {@code DasboardSurat}: hero, filter global, kartu ringkasan, panel analitik, funnel, top prodi,
 * tren, dan popup detail).
 *
 * <h2>Alur render (lihat {@link #renderContent()})</h2>
 * Setiap perubahan filter (rentang tahun, fakultas/jurusan, program) membangun ulang seluruh
 * halaman: {@link #renderHero()} dan panel filter dirender segera, lalu (via timer, agar indikator
 * memuat sempat tampil ke browser sebelum kueri berat dijalankan) {@link #renderDashboardContentDenganLoading(int)}
 * memanggil {@link #loadDashboardDataCached()} — yang membungkus {@link #loadDashboardData()} dengan
 * cache proses-hidup statis ({@code _CACHE}/{@code _EXPIRY}, TTL 5 menit, kunci dari
 * {@link #buildCacheKey()} = user+filter — DIBAGIKAN LINTAS SESI/USER karena statis, sehingga hasil
 * query satu user dapat terlihat oleh user lain dengan kombinasi filter identik dalam jendela TTL
 * yang sama; risiko rendah karena isinya murni data agregat/angka, bukan data personal per baris)
 * — lalu merender 12 panel berurutan ({@link #renderOverview}, {@link #renderPmbFunnel},
 * {@link #renderKurikulumMahasiswaAnalytic}, {@link #renderAkademikMahasiswaTambahan},
 * {@link #renderKesehatanAkademikTambahan}, {@link #renderPerkuliahanKehadiranTambahan},
 * {@link #renderBimbinganLulusanTambahan}, {@link #renderKemahasiswaanTambahan},
 * {@link #renderDosenPenelitianTambahan}, {@link #renderTopTablesAndTrends},
 * {@link #renderExistingDashboardShortcut}, {@link #renderRecommendation}). Sebuah nomor versi
 * ({@code dashboardLoadVersion}) mencegah hasil render dari permintaan filter yang sudah using
 * (superseded oleh permintaan filter berikutnya) menimpa tampilan.
 *
 * <h2>Model data ({@link DashboardPtData})</h2>
 * {@link #loadDashboardData()} mengisi satu objek agregat besar berisi puluhan hitungan/rata-rata
 * (total fakultas/prodi/kurikulum, funnel PMB, mahasiswa & KRS berjalan, IPK/SKS min-max-rata,
 * kegiatan/organisasi/prestasi mahasiswa, dosen & beban SKS, publikasi/penelitian/buku ajar,
 * perkuliahan & kehadiran, bimbingan/sidang/wisuda/lulusan) dengan memanggil banyak method
 * {@code count*}/{@code aggregateKrsNumber} kecil; masing-masing method tersebut mendelegasikan
 * pembangunan kriteria pencarian ke pasangan {@code create*Criteria}-nya (nama method mengikuti
 * pola {@code count<Entitas>}/{@code create<Entitas>Criteria}, umumnya menerima parameter opsional
 * {@code Jurusan forcedJurusan}/{@code Integer tahun} untuk mempersempit cakupan ke jurusan/tahun
 * tertentu) — pasangan kriteria yang SAMA dipakai ulang oleh {@link #showDetailWindow(String)}
 * untuk menyusun tabel rincian (dibatasi 150 baris) saat pengguna mengklik angka pada kartu metrik
 * ({@link #appendMetricCard}/{@link #appendValueMetricCard}, terhubung lewat parameter
 * {@code detailKey} yang dipetakan {@link #resolveDetailTitle(String)}) — memastikan angka
 * ringkasan dan rincian drill-down-nya selalu konsisten karena berasal dari kriteria yang identik.
 * Popup detail ditempelkan lewat {@link #attachPopupWindow(Window)} ke {@code body} (bukan
 * langsung ke {@code this}) karena {@link MyPortallayout} hanya menerima {@code MyPortalchildren}
 * sebagai anak langsung.
 *
 * <p>
 * Seluruh panel dirender sebagai blok HTML statis (bukan komponen ZK individual) lewat kumpulan
 * helper pembangun markup ({@code sectionIntroHtml}, {@code progressSectionHtml}/{@code progressRowHtml},
 * {@code tableMiniRowsHtml}, {@code trendHtml}, {@code miniStatHtml}, {@code badgeHtml},
 * {@code riskBadgeHtml}, dll.) demi performa render pada halaman dengan sangat banyak angka.
 * {@link #calculateAcademicHealthScore(DashboardPtData)} menghasilkan skor komposit kesehatan
 * akademik (0-100) dari rasio-rasio kunci (mis. tingkat aktivasi KRS, penyelesaian nilai) yang
 * ditampilkan pada panel kesehatan akademik dan rekomendasi aksi.
 * </p>
 */
public class DasborPerguruanTinggiTerpadu extends MyPortallayout {

    private static final long serialVersionUID = 20260529111001L;
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> _CACHE
            = new java.util.concurrent.ConcurrentHashMap<String, Object>();
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> _EXPIRY
            = new java.util.concurrent.ConcurrentHashMap<String, Long>();
    private static final long _TTL_MS = 5L * 60 * 1000;
    private static final int TOP_LIMIT = 10;
    private static final String[] PRODI_PILIHAN_FIELDS = new String[] { "prodi1", "prodi2", "prodi3", "prodi4", "prodi5", "prodiLulus" };

    private static boolean debug = false;

    private Tbmuser tbmuser;
    private Integer desktopHeight = 11000;

    private Div body;
    private Vbox loadingDashboardContainer;
    private int dashboardLoadVersion;
    private Intbox mulaiTahun;
    private Intbox sampaiTahun;
    private Combobox searchfakultas;
    private Combobox searchjurusan;
    private Combobox searchprogram;

    private int currentMulai;
    private int currentSampai;
    private Fakultas currentFakultas;
    private Jurusan currentJurusan;
    private String currentProgram;

    /** Membuat dasbor untuk user yang sedang login dan langsung menyusun seluruh tampilan lewat {@link #init()}. */
    public DasborPerguruanTinggiTerpadu() throws Exception {
        super();
        setWidth("100%");
        setMaximizedMode("whole");
        tbmuser = Common.getCurrentUser();
        init();
    }

    /** @return {@code true} bila mode debug aktif (kesalahan render ditampilkan detail lewat {@link #debugError}, bukan hanya dicatat diam-diam). */
    public static boolean isDebug() {
        return debug;
    }

    /** @param debugValue aktifkan/nonaktifkan mode debug untuk seluruh instans dasbor pada JVM ini (bidang statis). */
    public static void setDebug(boolean debugValue) {
        debug = debugValue;
    }

    /** Memasang tombol ekspor grid, menyesuaikan tinggi desktop tersimpan milik user, menyusun kerangka panel (hero+filter+body), lalu menentukan nilai filter default dan merender konten awal. */
    private void init() throws Exception {
        DashboardGridExportHelper.pasang(this, "Perguruan Tinggi Terpadu");
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
        panel.setTitle("Dasbor Perguruan Tinggi");
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

    /** Menentukan rentang tahun filter default (3 tahun terakhir s.d. tahun berjalan) sebelum panel filter pertama kali dirender. */
    private void initDefaultFilterValue() {
        int tahunSekarang = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
        try {
            Number maxMhs = (Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                    .add(activeCriterion()).setProjection(Projections.max("tahunangkatan")).uniqueResult();
            Number maxCalon = (Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
                    .add(activeCriterion()).setProjection(Projections.max("tahun")).uniqueResult();
            if (maxMhs != null && maxMhs.intValue() > tahunSekarang) {
                tahunSekarang = maxMhs.intValue();
            }
            if (maxCalon != null && maxCalon.intValue() > tahunSekarang) {
                tahunSekarang = maxCalon.intValue();
            }
        } catch (Exception e) {
            debugError("initDefaultFilterValue", e);
        }
        // Default filter terbaru: 3 tahun terakhir (tahun berjalan dan 2 tahun sebelumnya).
        currentMulai = tahunSekarang - 2;
        currentSampai = tahunSekarang;
    }

    /**
     * Menyusun ulang seluruh halaman dasbor sesuai filter formulir saat ini: membaca dan
     * menormalkan (tukar bila terbalik) rentang tahun serta fakultas/jurusan/program terpilih,
     * merender hero dan panel filter segera, lalu menjadwalkan pengambilan data berat dan
     * render panel konten lewat timer (agar indikator memuat sempat tampil ke browser lebih
     * dulu) via {@link #renderDashboardContentDenganLoading(int)}, ditandai dengan nomor versi
     * agar hasil filter yang sudah usang tidak menimpa tampilan filter terbaru.
     */
    private void renderContent() throws Exception {
        Common.clear(body);
        currentMulai = mulaiTahun == null || mulaiTahun.getValue() == null ? currentMulai : mulaiTahun.getValue();
        currentSampai = sampaiTahun == null || sampaiTahun.getValue() == null ? currentSampai : sampaiTahun.getValue();
        if (currentMulai > currentSampai) {
            int temp = currentMulai;
            currentMulai = currentSampai;
            currentSampai = temp;
        }
        currentFakultas = getSelectedFakultas();
        currentJurusan = getSelectedJurusan();
        currentProgram = getSelectedProgram();

        renderHero();
        renderFilter();

        final int loadVersion = ++dashboardLoadVersion;
        tampilkanLoadingDashboardTerpadu("Menyiapkan parameter filter dan tampilan awal...", 2);

        /*
         * Query dasbor terpadu ini banyak dan berat. Timer dipakai agar UI loading/progress
         * tampil dulu ke browser sebelum loadDashboardData() mulai mengambil data.
         */
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

    /**
     * Mengambil data agregat (lewat {@link #loadDashboardDataCached()}, dengan pelaporan
     * progres bertahap) lalu merender kedua belas panel konten secara berurutan (lihat javadoc
     * kelas). Dibatalkan diam-diam bila {@code loadVersion} sudah usang (filter berubah lagi
     * sebelum pengambilan data selesai). Kegagalan apa pun ditangkap dan ditampilkan sebagai
     * pesan galat ringkas ke pengguna, dicatat lewat {@link #debugError}.
     *
     * @param loadVersion nomor versi permintaan render ini, dibandingkan terhadap {@code dashboardLoadVersion} saat ini
     */
    private void renderDashboardContentDenganLoading(int loadVersion) throws Exception {
        try {
            updateDashboardProgress("Mengambil ringkasan Dasbor Perguruan Tinggi Terpadu...", 5);

            DashboardPtData data = loadDashboardDataCached();

            if (loadVersion != dashboardLoadVersion) {
                return;
            }

            updateDashboardProgress("Menyusun kartu, panel analitik, dan shortcut dashboard rinci...", 96);
            hapusLoadingDashboardTerpadu();

            renderOverview(data);
            renderPmbFunnel(data);
            renderKurikulumMahasiswaAnalytic(data);
            renderAkademikMahasiswaTambahan(data);
            renderKesehatanAkademikTambahan(data);
            renderPerkuliahanKehadiranTambahan(data);
            renderBimbinganLulusanTambahan(data);
            renderKemahasiswaanTambahan(data);
            renderDosenPenelitianTambahan(data);
            renderTopTablesAndTrends(data);
            renderExistingDashboardShortcut();
            renderRecommendation(data);
        } catch (Exception e) {
            debugError("renderDashboardContentDenganLoading", e);
            hapusLoadingDashboardTerpadu();
            appendHtml(body, "<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; "
                    + "color:#991b1b; border:1px solid #fecdd3; font-weight:700;'>Dasbor Perguruan Tinggi belum dapat dimuat. "
                    + "Silakan tekan Refresh atau aktifkan debug untuk melihat detail error.</div>");
        }
    }

    /** Menampilkan indikator memuat (bar progres HTML) di awal {@code body}, menggantikan indikator sebelumnya bila ada. */
    private void tampilkanLoadingDashboardTerpadu(String pesan, int persen) {
        if (body == null) {
            return;
        }

        hapusLoadingDashboardTerpadu();

        loadingDashboardContainer = new Vbox();
        loadingDashboardContainer.setWidth("100%");
        loadingDashboardContainer.setParent(body);
        loadingDashboardContainer.setStyle("margin-top:14px;");

        Html htmlLoading = new Html(buildLoadingDashboardHtml(pesan, persen));
        loadingDashboardContainer.appendChild(htmlLoading);
    }

    /** Memperbarui pesan dan persentase pada indikator memuat yang sedang tampil; tidak melakukan apa pun bila belum ada indikator aktif. */
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

    /** Melepas indikator memuat dari tampilan bila sedang terpasang. */
    private void hapusLoadingDashboardTerpadu() {
        if (loadingDashboardContainer != null) {
            try {
                if (loadingDashboardContainer.getParent() != null) {
                    loadingDashboardContainer.detach();
                }
            } catch (Exception e) {
                debugError("hapusLoadingDashboardTerpadu", e);
            }
        }
        loadingDashboardContainer = null;
    }

    /** @return markup HTML bar progres (dibatasi 0-100%) dengan {@code pesan} status di bawahnya. */
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
                + "<i class=\"fa fa-spinner fa-spin\"></i> Memproses Dasbor Perguruan Tinggi Terpadu</div>"
                + "<div style='font-size:12px; margin-bottom:10px; color:#64748b;'>" + safeHtml(pesan) + "</div>"
                + "<div style='width:100%; height:12px; border-radius:999px; overflow:hidden; background:#e2e8f0;'>"
                + "<div style='height:12px; width:" + progress + "%; border-radius:999px; "
                + "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div>"
                + "</div>"
                + "<div style='font-size:12px; font-weight:800; color:var(--ais-theme-primary,#1d4ed8); margin-top:7px;'>"
                + progress + "% selesai</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:4px;'>"
                + "Mohon tunggu. Sistem sedang mengambil data bertahap agar halaman utama tetap informatif.</div>"
                + "</div>";
    }

    /** Menampilkan banner hero (judul, deskripsi ringkas cakupan dasbor, dan badge periode/user/sumber data) di awal {@code body}. */
    private void renderHero() {
        String user = tbmuser == null ? "Pengguna" : safeText(tbmuser.getUserNama());
        String periode = currentMulai + " s.d. " + currentSampai;
        appendHtml(body, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
                + "color:#ffffff; box-shadow:0 18px 38px rgba(29,78,216,0.22);'>"
                + "<div style='position:absolute; width:240px; height:240px; right:-70px; top:-90px; border-radius:999px; background:rgba(255,255,255,0.13);'></div>"
                + "<div style='position:absolute; width:160px; height:160px; right:120px; bottom:-92px; border-radius:999px; background:rgba(255,255,255,0.10);'></div>"
                + "<div style='position:relative; z-index:2;'>"
                + "<div style='font-size:12px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:700;'>Monitoring Akademik, PMB, Kurikulum & Mahasiswa</div>"
                + "<div style='font-size:28px; line-height:1.18; font-weight:800; margin-top:7px;'>Dasbor Perguruan Tinggi Terpadu</div>"
                + "<div style='font-size:13px; max-width:880px; opacity:.93; margin-top:8px;'>Ringkasan gabungan dari dashboard Kurikulum, Calon Mahasiswa, Rekap Pendaftar SPMB, Mahasiswa, KRS, IPK, Nilai, SKS kumulatif, kegiatan mahasiswa, perkuliahan, dosen, penelitian, publikasi, bimbingan, sidang, wisuda, dan lulusan dalam satu halaman utama.</div>"
                + "<div style='margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;'>"
                + badgeHtml("Periode: " + periode, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml("User: " + user, "rgba(255,255,255,.16)", "#ffffff")
                + badgeHtml("Sumber: Dashboard PT Existing", "rgba(255,255,255,.16)", "#ffffff")
                + "</div></div></div>");
    }

    /** Menyusun toolbar filter global (rentang tahun, fakultas/prodi, program, tombol refresh); setiap perubahan langsung memicu {@link #renderContent()}. */
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

        new MyLabelAgakKecil("Fakultas / Prodi:").setParent(toolbar);
        searchfakultas = new Combobox();
        searchjurusan = new Combobox();
        Common.initFakultasDanJurusanData(currentFakultas, currentJurusan, searchfakultas, searchjurusan);
        searchfakultas.setCols(12);
        searchjurusan.setCols(15);
        searchfakultas.setReadonly(true);
        searchjurusan.setReadonly(true);
        searchfakultas.setParent(toolbar);
        searchjurusan.setParent(toolbar);
        selectComboByValue(searchfakultas, currentFakultas);
        selectComboByValue(searchjurusan, currentJurusan);

        new MyLabelAgakKecil("Program:").setParent(toolbar);
        searchprogram = new Combobox();
        Common.initPrograms(searchprogram);
        searchprogram.setCols(10);
        searchprogram.setReadonly(true);
        searchprogram.setParent(toolbar);
        selectComboByValue(searchprogram, currentProgram);

        EventListener reloadListener = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                renderContent();
            }
        };
        mulaiTahun.addEventListener("onChange", reloadListener);
        sampaiTahun.addEventListener("onChange", reloadListener);
        searchfakultas.addEventListener("onChange", reloadListener);
        searchjurusan.addEventListener("onChange", reloadListener);
        searchprogram.addEventListener("onChange", reloadListener);

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
        refresh.setTooltiptext("Refresh dasbor");
        refresh.setParent(toolbar);
        refresh.addEventListener("onClick", reloadListener);
    }

    /** @return kunci cache gabungan id user + rentang tahun + fakultas/jurusan/program terpilih, dipakai oleh {@link #loadDashboardDataCached()}. */
    private String buildCacheKey() {
        return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
                + "|" + currentMulai + "|" + currentSampai
                + "|" + (currentFakultas == null ? "" : currentFakultas.getId())
                + "|" + (currentJurusan == null ? "" : currentJurusan.getId())
                + "|" + (currentProgram == null ? "" : currentProgram);
    }

    @SuppressWarnings("unchecked")
    /**
     * @return data agregat dasbor untuk kombinasi filter saat ini, dari cache statis
     *         proses-hidup ({@code _CACHE}, TTL {@link #_TTL_MS}) bila masih berlaku, atau
     *         dihitung ulang lewat {@link #loadDashboardData()} dan disimpan ke cache
     */
    private DashboardPtData loadDashboardDataCached() throws Exception {
        String _k = buildCacheKey();
        Long _e = _EXPIRY.get(_k);
        if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
            return (DashboardPtData) _CACHE.get(_k);
        }
        DashboardPtData data = loadDashboardData();
        _CACHE.put(_k, data);
        _EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
        return data;
    }

    /**
     * Menghitung seluruh metrik dasbor untuk filter saat ini ke dalam satu {@link DashboardPtData}:
     * master fakultas/prodi/kurikulum, funnel PMB (pendaftar-lulus seleksi-daftar ulang-dapat
     * NIM-isi form tambahan), mahasiswa aktif & KRS berjalan, statistik IPK/SKS kumulatif,
     * kegiatan/organisasi/prestasi mahasiswa, dosen & beban SKS, publikasi/penelitian/buku ajar,
     * perkuliahan & kehadiran, hingga bimbingan/sidang/wisuda/lulusan — masing-masing lewat
     * pasangan method {@code count*}/{@code create*Criteria} terkait (lihat javadoc kelas).
     * Melaporkan progres pengambilan data bertahap lewat {@link #updateDashboardProgress}.
     *
     * @return data agregat lengkap siap dirender oleh seluruh panel {@code render*}
     */
    private DashboardPtData loadDashboardData() throws Exception {
        DashboardPtData data = new DashboardPtData();
        data.tahunMulai = currentMulai;
        data.tahunSampai = currentSampai;
        data.tahunAkademikBerjalan = safeText(Common.getCurrentTahunAkademik());
        data.semesterBerjalan = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

        updateDashboardProgress("Mengambil master fakultas, program studi, dan kurikulum...", 10);

        data.totalFakultas = countFakultasAktif();
        data.totalProdi = countJurusanAktif();
        data.totalKurikulum = countKurikulumAktif();

        updateDashboardProgress("Mengambil data PMB/SPMB: pendaftar, lulus seleksi, daftar ulang, dan NIM...", 16);

        data.totalPendaftar = countCalon(false, false, false, false, null);
        data.totalLulusSeleksi = countCalon(true, false, false, false, null);
        data.totalDaftarUlang = countCalon(true, true, false, false, null);
        data.totalDapatNim = countCalon(true, false, true, false, null);
        data.totalIsiFormTambahan = countCalon(true, false, false, true, null);

        updateDashboardProgress("Mengambil data mahasiswa aktif dan KRS berjalan...", 24);

        data.totalMahasiswa = countMahasiswa(null);
        data.totalMahasiswaKrs = countMahasiswaKrsBerjalan(data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.totalMahasiswaBelumKrsEstimasi = data.totalMahasiswa - data.totalMahasiswaKrs;
        if (data.totalMahasiswaBelumKrsEstimasi < 0) {
            data.totalMahasiswaBelumKrsEstimasi = 0;
        }

        updateDashboardProgress("Mengambil data akademik mahasiswa: KRS, nilai, IPK, dan SKS kumulatif...", 32);

        data.totalKrsRecord = countDetailKrsBerjalan(data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.totalNilaiDisetujui = countDetailNilai(data.tahunAkademikBerjalan, false);
        data.totalNilaiBelum = countDetailNilai(data.tahunAkademikBerjalan, true);
        data.totalNilaiSudah = data.totalNilaiDisetujui - data.totalNilaiBelum;
        if (data.totalNilaiSudah < 0) {
            data.totalNilaiSudah = 0;
        }

        data.avgIpk = aggregateKrsNumber("ipk", "avg", data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.minIpk = aggregateKrsNumber("ipk", "min", data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.maxIpk = aggregateKrsNumber("ipk", "max", data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.totalIpk = countKrsNumber("ipk", data.tahunAkademikBerjalan, data.semesterBerjalan);

        data.avgSksKumulatif = aggregateKrsNumber("sksk", "avg", data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.minSksKumulatif = aggregateKrsNumber("sksk", "min", data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.maxSksKumulatif = aggregateKrsNumber("sksk", "max", data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.totalSksKumulatif = countKrsNumber("sksk", data.tahunAkademikBerjalan, data.semesterBerjalan);

        updateDashboardProgress("Mengambil data kegiatan, organisasi, dan prestasi mahasiswa...", 40);

        data.totalKegiatanKemahasiswaan = countKegiatanKemahasiswaan(null, null);
        data.totalOrganisasiIntraKampus = countOrganisasiIntraKampus(null, null);
        data.totalPrestasiMahasiswa = countPrestasiMahasiswa(null, null);

        updateDashboardProgress("Mengambil data dosen, beban SKS, publikasi, penelitian, dan buku ajar...", 52);

        data.totalDosenAktif = countDosenAktif(null);
        data.totalBebanSksDosen = countBebanSksDosen(null, null);
        data.totalKegiatanDosen = countKegiatanDosen(null, null);
        data.totalOrganisasiDosen = countOrganisasiDosen(null, null);
        data.totalPrestasiDosen = countPrestasiDosen(null, null);
        data.totalKaryaDosen = countKaryaDosen(null, null);
        data.totalPublikasiIlmiah = countPublikasiIlmiah(null, null);
        data.totalPenelitianPengabdian = countPenelitianPengabdian(null, null);
        data.totalBukuAjar = countBukuAjar(null, null);

        updateDashboardProgress("Mengambil data perkuliahan, pertemuan, absensi, dan pencapaian...", 64);

        data.totalPenawaranPerkuliahan = countPerkuliahan(null, null, null);
        data.totalPerkuliahanGanjil = countPerkuliahan(null, null, Perkuliahan.GANJIL);
        data.totalPerkuliahanGenap = countPerkuliahan(null, null, Perkuliahan.GENAP);
        data.totalPerkuliahanSp = countPerkuliahan(null, null, Perkuliahan.SP);
        data.totalPerkuliahanBerjalan = countPerkuliahanByTahunAkademik(data.tahunAkademikBerjalan, data.semesterBerjalan, null);
        data.totalDetailDisetujuiBerjalan = countDetailPerkuliahanDisetujui(data.tahunAkademikBerjalan, data.semesterBerjalan);
        data.totalPertemuanAktif = countPertemuan(null, null, false, false);
        data.totalPertemuanBerlalu = countPertemuan(null, null, true, false);
        data.totalAbsensiTerisi = countPertemuan(null, null, false, true);
        data.rasioAbsensiTerisi = percent(data.totalAbsensiTerisi,
                data.totalPertemuanBerlalu <= 0 ? data.totalPertemuanAktif : data.totalPertemuanBerlalu);

        updateDashboardProgress("Mengambil data bimbingan, sidang, wisuda, lulusan, dan tracer alumni...", 72);

        data.totalBimbinganMahasiswa = countBimbinganMahasiswa(null, null);
        data.totalSidangMahasiswa = countSidangMahasiswa(null, null);
        data.totalWisudaMahasiswa = countWisudaMahasiswa(null, null);
        data.totalLulusanMahasiswa = countLulusanMahasiswa(null, null, null);
        data.totalStatusKeluarMahasiswa = countLulusanMahasiswa(null, null, "statusKeluar");
        data.totalPredikatKelulusanMahasiswa = countLulusanMahasiswa(null, null, "predikatKelulusan");
        data.totalStatusSetelahLulusMahasiswa = countLulusanMahasiswa(null, null, "statusSetelahLulus");
        data.totalPekerjaanSetelahLulusMahasiswa = countLulusanMahasiswa(null, null, "statusPekerjaanSetelahLulus");
        data.totalDomisiliSetelahLulusMahasiswa = countLulusanMahasiswa(null, null, "statusDomisiliSetelahLulus");
        data.totalMasaStudiTerdeteksi = countMasaStudiMahasiswa(null, null);
        data.totalSemesterLulusTerdeteksi = countLulusanMahasiswa(null, null, "semesterLulus");

        List<Jurusan> jurusans = loadJurusanList(TOP_LIMIT <= 0 ? 10 : 1000);
        updateDashboardProgress("Menghitung sebaran top program studi dari " + jurusans.size() + " prodi...", 78);
        for (int i = 0; i < jurusans.size(); i++) {
            if (i == 0 || i == jurusans.size() - 1 || i % 10 == 0) {
                int progressProdi = 78 + ((i + 1) * 10 / (jurusans.size() <= 0 ? 1 : jurusans.size()));
                updateDashboardProgress("Menghitung data program studi " + (i + 1) + " dari " + jurusans.size() + "...", progressProdi);
            }
            Jurusan jurusan = jurusans.get(i);
            long jumlahMhs = countMahasiswa(jurusan);
            long jumlahCalon = countCalonByJurusan(jurusan, false, false, false);
            long jumlahLulus = countCalonByJurusan(jurusan, true, false, false);
            long jumlahKurikulum = countKurikulumByJurusan(jurusan);
            long jumlahKegiatan = countKegiatanKemahasiswaan(jurusan, null);
            long jumlahOrganisasi = countOrganisasiIntraKampus(jurusan, null);
            long jumlahPrestasi = countPrestasiMahasiswa(jurusan, null);
            long jumlahDosen = countDosenAktif(jurusan);
            long jumlahBebanSks = countBebanSksDosen(jurusan, null);
            long jumlahKegiatanDosen = countKegiatanDosen(jurusan, null);
            long jumlahOrganisasiDosen = countOrganisasiDosen(jurusan, null);
            long jumlahPrestasiDosen = countPrestasiDosen(jurusan, null);
            long jumlahKaryaDosen = countKaryaDosen(jurusan, null);
            long jumlahPublikasi = countPublikasiIlmiah(jurusan, null);
            long jumlahPenelitianPengabdian = countPenelitianPengabdian(jurusan, null);
            long jumlahBukuAjar = countBukuAjar(jurusan, null);
            long jumlahPerkuliahan = countPerkuliahan(jurusan, null, null);
            long jumlahPertemuan = countPertemuan(jurusan, null, false, false);
            long jumlahAbsensi = countPertemuan(jurusan, null, false, true);
            long jumlahPencapaian = countPertemuan(jurusan, null, true, false);
            long jumlahBimbingan = countBimbinganMahasiswa(jurusan, null);
            long jumlahSidang = countSidangMahasiswa(jurusan, null);
            long jumlahWisuda = countWisudaMahasiswa(jurusan, null);
            long jumlahLulusan = countLulusanMahasiswa(jurusan, null, null);
            long jumlahStatusKeluar = countLulusanMahasiswa(jurusan, null, "statusKeluar");
            long jumlahPredikatKelulusan = countLulusanMahasiswa(jurusan, null, "predikatKelulusan");
            long jumlahMasaStudi = countMasaStudiMahasiswa(jurusan, null);
            if (jumlahMhs > 0) {
                data.topMahasiswaProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahMhs));
            }
            if (jumlahCalon > 0) {
                data.topPendaftarProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahCalon));
            }
            if (jumlahLulus > 0) {
                data.topLulusProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahLulus));
            }
            if (jumlahKurikulum > 0) {
                data.topKurikulumProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahKurikulum));
            }
            if (jumlahKegiatan > 0) {
                data.topKegiatanProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahKegiatan));
            }
            if (jumlahOrganisasi > 0) {
                data.topOrganisasiProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahOrganisasi));
            }
            if (jumlahPrestasi > 0) {
                data.topPrestasiProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPrestasi));
            }
            if (jumlahDosen > 0) {
                data.topDosenProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahDosen));
            }
            if (jumlahBebanSks > 0) {
                data.topBebanSksDosenProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahBebanSks));
            }
            if (jumlahKegiatanDosen > 0) {
                data.topKegiatanDosenProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahKegiatanDosen));
            }
            if (jumlahOrganisasiDosen > 0) {
                data.topOrganisasiDosenProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahOrganisasiDosen));
            }
            if (jumlahPrestasiDosen > 0) {
                data.topPrestasiDosenProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPrestasiDosen));
            }
            if (jumlahKaryaDosen > 0) {
                data.topKaryaDosenProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahKaryaDosen));
            }
            if (jumlahPublikasi > 0) {
                data.topPublikasiIlmiahProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPublikasi));
            }
            if (jumlahPenelitianPengabdian > 0) {
                data.topPenelitianPengabdianProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPenelitianPengabdian));
            }
            if (jumlahBukuAjar > 0) {
                data.topBukuAjarProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahBukuAjar));
            }
            if (jumlahPerkuliahan > 0) {
                data.topPerkuliahanProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPerkuliahan));
            }
            if (jumlahPertemuan > 0) {
                data.topPertemuanProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPertemuan));
            }
            if (jumlahAbsensi > 0) {
                data.topAbsensiProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahAbsensi));
            }
            if (jumlahPencapaian > 0) {
                data.topPencapaianProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPencapaian));
            }
            if (jumlahBimbingan > 0) {
                data.topBimbinganProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahBimbingan));
            }
            if (jumlahSidang > 0) {
                data.topSidangProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahSidang));
            }
            if (jumlahWisuda > 0) {
                data.topWisudaProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahWisuda));
            }
            if (jumlahLulusan > 0) {
                data.topLulusanProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahLulusan));
            }
            if (jumlahStatusKeluar > 0) {
                data.topStatusKeluarProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahStatusKeluar));
            }
            if (jumlahPredikatKelulusan > 0) {
                data.topPredikatKelulusanProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahPredikatKelulusan));
            }
            if (jumlahMasaStudi > 0) {
                data.topMasaStudiProdi.add(new DashboardMiniRow(safeJurusanLabel(jurusan), jumlahMasaStudi));
            }
        }
        sortAndLimit(data.topMahasiswaProdi, TOP_LIMIT);
        sortAndLimit(data.topPendaftarProdi, TOP_LIMIT);
        sortAndLimit(data.topLulusProdi, TOP_LIMIT);
        sortAndLimit(data.topKurikulumProdi, TOP_LIMIT);
        sortAndLimit(data.topKegiatanProdi, TOP_LIMIT);
        sortAndLimit(data.topOrganisasiProdi, TOP_LIMIT);
        sortAndLimit(data.topPrestasiProdi, TOP_LIMIT);
        sortAndLimit(data.topDosenProdi, TOP_LIMIT);
        sortAndLimit(data.topBebanSksDosenProdi, TOP_LIMIT);
        sortAndLimit(data.topKegiatanDosenProdi, TOP_LIMIT);
        sortAndLimit(data.topOrganisasiDosenProdi, TOP_LIMIT);
        sortAndLimit(data.topPrestasiDosenProdi, TOP_LIMIT);
        sortAndLimit(data.topKaryaDosenProdi, TOP_LIMIT);
        sortAndLimit(data.topPublikasiIlmiahProdi, TOP_LIMIT);
        sortAndLimit(data.topPenelitianPengabdianProdi, TOP_LIMIT);
        sortAndLimit(data.topBukuAjarProdi, TOP_LIMIT);
        sortAndLimit(data.topPerkuliahanProdi, TOP_LIMIT);
        sortAndLimit(data.topPertemuanProdi, TOP_LIMIT);
        sortAndLimit(data.topAbsensiProdi, TOP_LIMIT);
        sortAndLimit(data.topPencapaianProdi, TOP_LIMIT);
        sortAndLimit(data.topBimbinganProdi, TOP_LIMIT);
        sortAndLimit(data.topSidangProdi, TOP_LIMIT);
        sortAndLimit(data.topWisudaProdi, TOP_LIMIT);
        sortAndLimit(data.topLulusanProdi, TOP_LIMIT);
        sortAndLimit(data.topStatusKeluarProdi, TOP_LIMIT);
        sortAndLimit(data.topPredikatKelulusanProdi, TOP_LIMIT);
        sortAndLimit(data.topMasaStudiProdi, TOP_LIMIT);

        updateDashboardProgress("Menghitung tren tahunan sesuai rentang filter...", 90);

        for (int tahun = currentMulai; tahun <= currentSampai; tahun++) {
            data.trendPendaftar.add(new DashboardMiniRow(String.valueOf(tahun), countCalon(false, false, false, false, new Integer(tahun))));
            data.trendMahasiswa.add(new DashboardMiniRow(String.valueOf(tahun), countMahasiswaByTahun(tahun)));
            data.trendKurikulum.add(new DashboardMiniRow(String.valueOf(tahun), countKurikulumByTahun(tahun)));
            data.trendKegiatan.add(new DashboardMiniRow(String.valueOf(tahun), countKegiatanKemahasiswaan(null, new Integer(tahun))));
            data.trendOrganisasi.add(new DashboardMiniRow(String.valueOf(tahun), countOrganisasiIntraKampus(null, new Integer(tahun))));
            data.trendPrestasi.add(new DashboardMiniRow(String.valueOf(tahun), countPrestasiMahasiswa(null, new Integer(tahun))));
            data.trendBebanSksDosen.add(new DashboardMiniRow(String.valueOf(tahun), countBebanSksDosen(null, new Integer(tahun))));
            data.trendKegiatanDosen.add(new DashboardMiniRow(String.valueOf(tahun), countKegiatanDosen(null, new Integer(tahun))));
            data.trendOrganisasiDosen.add(new DashboardMiniRow(String.valueOf(tahun), countOrganisasiDosen(null, new Integer(tahun))));
            data.trendPrestasiDosen.add(new DashboardMiniRow(String.valueOf(tahun), countPrestasiDosen(null, new Integer(tahun))));
            data.trendKaryaDosen.add(new DashboardMiniRow(String.valueOf(tahun), countKaryaDosen(null, new Integer(tahun))));
            data.trendPublikasiIlmiah.add(new DashboardMiniRow(String.valueOf(tahun), countPublikasiIlmiah(null, new Integer(tahun))));
            data.trendPenelitianPengabdian.add(new DashboardMiniRow(String.valueOf(tahun), countPenelitianPengabdian(null, new Integer(tahun))));
            data.trendBukuAjar.add(new DashboardMiniRow(String.valueOf(tahun), countBukuAjar(null, new Integer(tahun))));
            data.trendPerkuliahan.add(new DashboardMiniRow(String.valueOf(tahun), countPerkuliahan(null, new Integer(tahun), null)));
            data.trendPertemuan.add(new DashboardMiniRow(String.valueOf(tahun), countPertemuan(null, new Integer(tahun), false, false)));
            data.trendAbsensi.add(new DashboardMiniRow(String.valueOf(tahun), countPertemuan(null, new Integer(tahun), false, true)));
            data.trendPencapaian.add(new DashboardMiniRow(String.valueOf(tahun), countPertemuan(null, new Integer(tahun), true, false)));
            data.trendBimbingan.add(new DashboardMiniRow(String.valueOf(tahun), countBimbinganMahasiswa(null, new Integer(tahun))));
            data.trendSidang.add(new DashboardMiniRow(String.valueOf(tahun), countSidangMahasiswa(null, new Integer(tahun))));
            data.trendWisuda.add(new DashboardMiniRow(String.valueOf(tahun), countWisudaMahasiswa(null, new Integer(tahun))));
            data.trendLulusan.add(new DashboardMiniRow(String.valueOf(tahun), countLulusanMahasiswa(null, new Integer(tahun), null)));
            data.trendMasaStudi.add(new DashboardMiniRow(String.valueOf(tahun), countMasaStudiMahasiswa(null, new Integer(tahun))));
        }

        updateDashboardProgress("Menghitung rasio akhir dan menyiapkan data tampilan...", 94);

        data.rasioLulus = percent(data.totalLulusSeleksi, data.totalPendaftar);
        data.rasioDaftarUlang = percent(data.totalDaftarUlang, data.totalLulusSeleksi);
        data.rasioKonversiNim = percent(data.totalDapatNim, data.totalPendaftar);
        data.rasioKrs = percent(data.totalMahasiswaKrs, data.totalMahasiswa);

        data.rasioNilaiBelum = percent(data.totalNilaiBelum, data.totalNilaiDisetujui);
        data.rasioKurikulumPerProdi = percent(data.totalKurikulum, data.totalProdi);
        data.rasioWisudaLulusan = percent(data.totalWisudaMahasiswa, data.totalLulusanMahasiswa);
        data.rasioLulusanMahasiswa = percent(data.totalLulusanMahasiswa, data.totalMahasiswa);
        data.rasioMahasiswaDariPendaftar = percent(data.totalDapatNim, data.totalPendaftar);
        data.rasioMahasiswaPerDosen = data.totalDosenAktif <= 0L ? 0L : Math.round((data.totalMahasiswa * 1.0d) / data.totalDosenAktif);
        data.skorKesehatanAkademik = calculateAcademicHealthScore(data);

        return data;
    }

    /** Panel "Overview Perguruan Tinggi": kartu ringkasan total fakultas/prodi/kurikulum/mahasiswa dan metrik inti lainnya, masing-masing dapat diklik untuk drill-down lewat {@link #showDetailWindow(String)}. */
    private void renderOverview(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Overview Perguruan Tinggi",
                "Ringkasan ini menggabungkan data prodi, kurikulum, PMB/SPMB, mahasiswa aktif, dan KRS berjalan. Nilai pada kartu tertentu dapat diklik untuk membuka contoh data."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Fakultas", data.totalFakultas, "Fakultas aktif", "#0f766e", "F", "FAKULTAS");
        appendMetricCard(cards, "Prodi", data.totalProdi, "Program studi aktif", "var(--ais-theme-primary,#2563eb)", "P", "PRODI");
        appendMetricCard(cards, "Kurikulum", data.totalKurikulum, "Kurikulum aktif sesuai filter", "#7c3aed", "K", "KURIKULUM");
        appendMetricCard(cards, "Pendaftar PMB", data.totalPendaftar, "Semua peminat/pilihan prodi", "#0891b2", "PMB", "PENDAFTAR");
        appendMetricCard(cards, "Lulus Seleksi", data.totalLulusSeleksi, "Calon mahasiswa prodi lulus", "#16a34a", "✓", "LULUS");
        appendMetricCard(cards, "Daftar Ulang", data.totalDaftarUlang, "Sudah bayar daftar ulang", "#f59e0b", "DU", "DAFTAR_ULANG");
        appendMetricCard(cards, "Dapat NIM", data.totalDapatNim, "Sudah menjadi mahasiswa", "#0ea5e9", "N", "DAPAT_NIM");
        appendMetricCard(cards, "Mahasiswa", data.totalMahasiswa, "Mahasiswa aktif sesuai filter", "var(--ais-theme-primary,#1d4ed8)", "M", "MAHASISWA");
        appendMetricCard(cards, "KRS Berjalan", data.totalMahasiswaKrs, "Estimasi dari detail perkuliahan " + data.tahunAkademikBerjalan, "#dc2626", "KRS", null);
    }

    /** Panel "PMB / SPMB": funnel bertahap pendaftar &rarr; lulus seleksi &rarr; daftar ulang &rarr; dapat NIM &rarr; isi form tambahan, memakai {@link #progressSectionHtml}/{@link #progressRowHtml}. */
    private void renderPmbFunnel(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml(" PMB / SPMB",
                "Diadaptasi dari DashboardCalonMahasiswa dan RekapPendaftarSpmbSemua: peminat, lulus seleksi, daftar ulang, sampai memperoleh NIM."));

        String funnel = dashboardExplainHtml("Gunakan bagian ini untuk melihat titik bocor proses PMB. Rasio daftar ulang dihitung dari yang lulus seleksi, sedangkan rasio NIM dihitung dari total pendaftar.")
                + progressSectionHtml("Pipeline PMB", data.totalPendaftar,
                        new DashboardMiniRow("Pendaftar", data.totalPendaftar),
                        new DashboardMiniRow("Lulus Seleksi", data.totalLulusSeleksi),
                        new DashboardMiniRow("Dapat NIM", data.totalDapatNim))
                + "<div style='display:flex; gap:8px; flex-wrap:wrap; margin-top:10px;'>"
                + badgeHtml("Rasio lulus: " + data.rasioLulus + "%", "#ecfeff", "#155e75")
                + badgeHtml("Rasio daftar ulang: " + data.rasioDaftarUlang + "%", "#fffbeb", "#92400e")
                + badgeHtml("Konversi NIM: " + data.rasioKonversiNim + "%", "#eff6ff", "var(--ais-theme-primary,#1d4ed8)")
                + "</div>";

        String daftarUlang = dashboardExplainHtml("Daftar ulang dan NIM memakai pola yang sama dengan rekap SPMB: pembayaran daftar ulang, calon mahasiswa yang sudah punya mahasiswa/NIM, serta selisih yang perlu dicek.")
                + miniStatHtml("Daftar Ulang", data.totalDaftarUlang, "Dari lulus seleksi")
                + miniStatHtml("Dapat NIM", data.totalDapatNim, "Sudah masuk data mahasiswa")
                + miniStatHtml("Isi Form Tambahan", data.totalIsiFormTambahan, "Calon lulus dengan data tambahan")
                + miniStatHtml("Selisih DU - NIM", Math.abs(data.totalDaftarUlang - data.totalDapatNim), "Perlu validasi PMB/registrasi");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(funnel) + dashboardPanelHtml(daftarUlang) + "</div>");
    }

    /** Panel "Analitik Kurikulum, Mahasiswa, dan KRS": statistik KRS berjalan, IPK/SKS kumulatif (rata-rata/min/maks), dan status kelengkapan nilai. */
    private void renderKurikulumMahasiswaAnalytic(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Analitik Kurikulum, Mahasiswa, dan KRS",
                "Bagian ini memadukan dashboard kurikulum, dashboard mahasiswa, dan dashboard KRS untuk melihat kesiapan akademik terhadap jumlah mahasiswa."));

        long kurikulumPerProdi = data.totalProdi <= 0 ? 0 : Math.round((data.totalKurikulum * 100.0d) / data.totalProdi);
        String akademik = dashboardExplainHtml("Rasio kurikulum per prodi membantu memeriksa apakah program studi sudah memiliki kurikulum aktif pada rentang tahun yang dipilih.")
                + progressSectionHtml("Kesiapan Akademik", data.totalProdi <= 0 ? data.totalKurikulum : data.totalProdi,
                        new DashboardMiniRow("Prodi Aktif", data.totalProdi),
                        new DashboardMiniRow("Kurikulum Aktif", data.totalKurikulum),
                        new DashboardMiniRow("Kurikulum/Prodi (%)", kurikulumPerProdi));

        String krs = dashboardExplainHtml("KRS berjalan dihitung dari jumlah mahasiswa unik pada tabel detail perkuliahan tahun akademik berjalan. Jika filter fakultas/prodi/program aktif, angka KRS tetap bersifat estimasi global karena sumber dashboard KRS existing memakai SQL detailperkuliahan.")
                + miniStatHtml("Mahasiswa Aktif", data.totalMahasiswa, "Sesuai filter tahun/prodi/program")
                + miniStatHtml("KRS Berjalan", data.totalMahasiswaKrs, data.tahunAkademikBerjalan + " - " + data.semesterBerjalan)
                + miniStatHtml("Belum KRS Estimasi", data.totalMahasiswaBelumKrsEstimasi, "Mahasiswa - KRS berjalan")
                + miniStatHtml("Rasio KRS", data.rasioKrs, "% mahasiswa sudah terdeteksi KRS");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(akademik) + dashboardPanelHtml(krs) + "</div>");
    }

    /** Panel "Dashboard Akademik Mahasiswa": rincian tambahan seputar KRS/nilai/IPK di luar ringkasan pada {@link #renderKurikulumMahasiswaAnalytic}. */
    private void renderAkademikMahasiswaTambahan(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Dashboard Akademik Mahasiswa",
                "Tambahan dari DashboardKrsMahasiswa, DashboardIpkMahasiswa, DashboardNilaiMahasiswa, dan DashboardSksKumulatifMahasiswa. Angka memakai tahun akademik berjalan dan tetap mengikuti filter fakultas/prodi/program bila memungkinkan."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Mhs KRS Unik", data.totalMahasiswaKrs, data.tahunAkademikBerjalan + " - " + data.semesterBerjalan, "#dc2626", "KRS", "KRS_MHS");
        appendMetricCard(cards, "Detail KRS", data.totalKrsRecord, "Jumlah detail mata kuliah/KRS berjalan", "#ef4444", "DK", "DETAIL_KRS");
        appendValueMetricCard(cards, "Rata-rata IPK", formatDecimal(data.avgIpk), "Min " + formatDecimal(data.minIpk) + " / Max " + formatDecimal(data.maxIpk), "#7c3aed", "IPK", "IPK");
        appendValueMetricCard(cards, "Rata-rata SKS", formatDecimal(data.avgSksKumulatif), "Min " + formatDecimal(data.minSksKumulatif) + " / Max " + formatDecimal(data.maxSksKumulatif), "#0f766e", "SKS", "SKS_KUM");
        appendMetricCard(cards, "Nilai Disetujui", data.totalNilaiDisetujui, "Detail nilai pada TA berjalan", "var(--ais-theme-primary,#2563eb)", "N", "NILAI");
        appendMetricCard(cards, "Nilai Belum", data.totalNilaiBelum, "Nilai huruf kosong/belum terisi", "#f59e0b", "!", "NILAI_BELUM");

        String ipkPanel = dashboardExplainHtml("IPK dan SKS kumulatif mengikuti pola DashboardIpkMahasiswa/DashboardSksKumulatifMahasiswa: sumber data KrsMahasiswa, tahun akademik berjalan, semester ganjil/genap berjalan, dan semester pendek diabaikan.")
                + miniStatHtml("Data IPK Terbaca", data.totalIpk, "KrsMahasiswa.ipk > 0")
                + miniStatHtml("IPK Rata-rata", formatDecimal(data.avgIpk), "Rentang " + formatDecimal(data.minIpk) + " - " + formatDecimal(data.maxIpk))
                + miniStatHtml("Data SKS Terbaca", data.totalSksKumulatif, "KrsMahasiswa.sksk > 0")
                + miniStatHtml("SKS Rata-rata", formatDecimal(data.avgSksKumulatif), "Rentang " + formatDecimal(data.minSksKumulatif) + " - " + formatDecimal(data.maxSksKumulatif));

        String nilaiPanel = dashboardExplainHtml("Nilai mengambil Detailperkuliahan yang sudah disetujui pada tahun akademik berjalan. Nilai belum terisi menjadi indikator proses input/validasi nilai yang perlu dimonitor.")
                + progressSectionHtml("Validasi Nilai", data.totalNilaiDisetujui,
                        new DashboardMiniRow("Total Detail Nilai", data.totalNilaiDisetujui),
                        new DashboardMiniRow("Sudah Bernilai", data.totalNilaiSudah),
                        new DashboardMiniRow("Belum Bernilai", data.totalNilaiBelum));

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(ipkPanel) + dashboardPanelHtml(nilaiPanel) + "</div>");
    }


    /** Panel "Dashboard Kesehatan Akademik & Mutu Data": skor komposit dari {@link #calculateAcademicHealthScore(DashboardPtData)} beserta indikator mutu data (mis. tingkat kelengkapan nilai/KRS) yang menyusunnya. */
    private void renderKesehatanAkademikTambahan(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Dashboard Kesehatan Akademik & Mutu Data",
                "Dashboard tambahan ini merangkum kesehatan operasional akademik: kesiapan kurikulum, KRS, validasi nilai, absensi, siklus hidup mahasiswa, rasio dosen-mahasiswa, dan data lulusan. Angka dibuat dari data yang sudah diambil sehingga tidak menambah dependency model baru."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendValueMetricCard(cards, "Skor Kesehatan", data.skorKesehatanAkademik + "%", "Gabungan KRS, nilai, absensi, PMB, kurikulum", "#0f766e", "HK", null);
        appendValueMetricCard(cards, "Kurikulum/Prodi", data.rasioKurikulumPerProdi + "%", "Kurikulum aktif dibanding prodi aktif", "var(--ais-theme-primary,#2563eb)", "KP", null);
        appendValueMetricCard(cards, "Rasio KRS", data.rasioKrs + "%", "Mahasiswa terdeteksi KRS berjalan", "#dc2626", "KRS", null);
        appendValueMetricCard(cards, "Nilai Belum", data.rasioNilaiBelum + "%", "Nilai kosong dari detail nilai disetujui", "#f59e0b", "NB", null);
        appendValueMetricCard(cards, "Absensi Terisi", data.rasioAbsensiTerisi + "%", "Absensi dari pertemuan berlalu/aktif", "#0891b2", "AB", null);
        appendMetricCard(cards, "Mhs/Dosen", data.rasioMahasiswaPerDosen, "Rasio mahasiswa aktif per dosen aktif", "#7c3aed", "MD", null);
        appendValueMetricCard(cards, "Wisuda/Lulusan", data.rasioWisudaLulusan + "%", "Pendaftaran wisuda dibanding lulusan", "#16a34a", "WL", null);
        appendValueMetricCard(cards, "Lulusan/Mhs", data.rasioLulusanMahasiswa + "%", "Lulusan dibanding mahasiswa aktif filter", "#9333ea", "LM", null);

        long siklusBase = data.totalPendaftar;
        if (siklusBase < data.totalMahasiswa) {
            siklusBase = data.totalMahasiswa;
        }
        if (siklusBase < data.totalLulusanMahasiswa) {
            siklusBase = data.totalLulusanMahasiswa;
        }
        if (siklusBase <= 0L) {
            siklusBase = 1L;
        }

        String siklusMahasiswa = dashboardExplainHtml("Siklus hidup mahasiswa memperlihatkan alur dari PMB, menjadi mahasiswa/NIM, aktif ber-KRS, sampai lulusan dan wisuda. Membantu pimpinan melihat titik yang perlu dikuatkan di setiap fase akademik.")
                + progressSectionHtml("Siklus Hidup Mahasiswa", siklusBase,
                        new DashboardMiniRow("Pendaftar PMB", data.totalPendaftar),
                        new DashboardMiniRow("Dapat NIM", data.totalDapatNim),
                        new DashboardMiniRow("Lulusan", data.totalLulusanMahasiswa))
                + miniStatHtml("Mahasiswa Aktif", data.totalMahasiswa, "Basis monitoring akademik")
                + miniStatHtml("KRS Berjalan", data.totalMahasiswaKrs, data.tahunAkademikBerjalan + " - " + data.semesterBerjalan)
                + miniStatHtml("Wisuda", data.totalWisudaMahasiswa, "Pendaftaran wisuda pada filter tahun");

        String mutuData = dashboardExplainHtml("Mutu data akademik mengukur kelengkapan data yang sering menjadi sumber masalah saat pelaporan: nilai kosong, status lulusan, predikat, tracer, dan semester lulus.")
                + miniStatHtml("Nilai Belum", data.rasioNilaiBelum + "%", data.totalNilaiBelum + " dari " + data.totalNilaiDisetujui + " detail nilai")
                + miniStatHtml("Status Keluar Terisi", data.totalStatusKeluarMahasiswa, "Data mahasiswa keluar/lulus")
                + miniStatHtml("Predikat Terisi", data.totalPredikatKelulusanMahasiswa, "Predikat/yudisium lulusan")
                + miniStatHtml("Tracer Terisi", data.totalStatusSetelahLulusMahasiswa, "Status setelah lulus/keluar")
                + miniStatHtml("Semester Lulus Terisi", data.totalSemesterLulusTerdeteksi, "Kesiapan dashboard semester lulus");

        String kapasitasAkademik = dashboardExplainHtml("Kapasitas akademik membantu membaca beban layanan prodi: mahasiswa aktif, dosen aktif, jumlah kelas, dan pertemuan. Rasio mahasiswa per dosen bersifat agregat sesuai filter fakultas/prodi/program.")
                + miniStatHtml("Dosen Aktif", data.totalDosenAktif, "Dosen aktif sesuai filter")
                + miniStatHtml("Mahasiswa per Dosen", data.rasioMahasiswaPerDosen, "Rasio agregat")
                + miniStatHtml("Penawaran Kuliah", data.totalPenawaranPerkuliahan, "Kelas/perkuliahan aktif")
                + miniStatHtml("Pertemuan Aktif", data.totalPertemuanAktif, "Rencana/pertemuan aktif")
                + miniStatHtml("Absensi Terisi", data.totalAbsensiTerisi, "Kontrol pelaksanaan perkuliahan");

        String risikoOperasional = dashboardExplainHtml("Panel risiko menandai area yang perlu diprioritaskan. Nilai ideal bukan untuk menghukum prodi, tetapi sebagai alarm awal agar validasi data lebih cepat dilakukan.")
                + riskBadgeHtml("KRS", data.rasioKrs, 70, "Rasio mahasiswa terdeteksi KRS")
                + riskBadgeHtml("Nilai", 100 - data.rasioNilaiBelum, 85, "Kelengkapan input nilai")
                + riskBadgeHtml("Absensi", data.rasioAbsensiTerisi, 70, "Absensi terisi dari pertemuan")
                + riskBadgeHtml("Kurikulum", data.rasioKurikulumPerProdi, 100, "Kurikulum aktif terhadap prodi")
                + riskBadgeHtml("Daftar Ulang", data.rasioDaftarUlang, 50, "Konversi lulus seleksi ke daftar ulang");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(siklusMahasiswa) + dashboardPanelHtml(mutuData) + "</div>");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(kapasitasAkademik) + dashboardPanelHtml(risikoOperasional) + "</div>");
    }

    /** Panel "Dashboard Bimbingan, Sidang, Wisuda, dan Lulusan": jumlah mahasiswa dalam bimbingan tugas akhir, sidang, wisuda, dan yang sudah lulus (termasuk masa studi rata-rata). */
    private void renderBimbinganLulusanTambahan(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Dashboard Bimbingan, Sidang, Wisuda, dan Lulusan",
                "Tambahan dari DashboardBimbinganMahasiswa, DashboardSidangMahasiswa, DashboardWisudaMahasiswa, DashboardLulusan, dan dashboard rincian lulusan. Data mengikuti filter tahun, fakultas/prodi, dan program."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Bimbingan TA", data.totalBimbinganMahasiswa, "Mahasiswa Request Tugas Akhir", "#7c3aed", "TA", "BIMBINGAN_MHS");
        appendMetricCard(cards, "Sidang", data.totalSidangMahasiswa, "Data skripsi/tugas akhir", "var(--ais-theme-primary,#2563eb)", "SD", "SIDANG_MHS");
        appendMetricCard(cards, "Wisuda", data.totalWisudaMahasiswa, "Pendaftaran wisuda", "#16a34a", "WS", "WISUDA_MHS");
        appendMetricCard(cards, "Lulusan", data.totalLulusanMahasiswa, "Mahasiswa tahun lulus", "#0f766e", "L", "LULUSAN_MHS");
        appendMetricCard(cards, "Status Keluar", data.totalStatusKeluarMahasiswa, "Status keluar terisi", "#dc2626", "SK", "STATUS_KELUAR_MHS");
        appendMetricCard(cards, "Predikat", data.totalPredikatKelulusanMahasiswa, "Predikat kelulusan terisi", "#f59e0b", "PK", "PREDIKAT_KELULUSAN_MHS");
        appendMetricCard(cards, "Status Setelah Lulus", data.totalStatusSetelahLulusMahasiswa, "Tracer/status lanjutan", "#0891b2", "SL", "STATUS_SETELAH_LULUS_MHS");
        appendMetricCard(cards, "Pekerjaan", data.totalPekerjaanSetelahLulusMahasiswa, "Pekerjaan setelah lulus", "#9333ea", "PJ", "PEKERJAAN_SETELAH_LULUS_MHS");
        appendMetricCard(cards, "Domisili", data.totalDomisiliSetelahLulusMahasiswa, "Domisili setelah lulus", "#0ea5e9", "DM", "DOMISILI_SETELAH_LULUS_MHS");
        appendMetricCard(cards, "Masa Studi", data.totalMasaStudiTerdeteksi, "Tahun lulus & angkatan terisi", "#0f766e", "MS", "MASA_STUDI_MHS");
        appendMetricCard(cards, "Semester Lulus", data.totalSemesterLulusTerdeteksi, "Semester lulus terisi", "var(--ais-theme-primary,#1d4ed8)", "SM", "SEMESTER_LULUS_MHS");

        String akademikAkhir = dashboardExplainHtml("Bagian ini memadukan proses akhir akademik: bimbingan, sidang, wisuda, dan lulusan. Angka bimbingan memakai MahasiswaRequestTugasAkhir, sidang memakai Skripsi, wisuda memakai PendaftaranWisuda, dan lulusan memakai Mahasiswa.tahunLulus.")
                + tableMiniRowsHtml("Top Prodi Bimbingan", data.topBimbinganProdi, "Prodi", "Bimbingan")
                + tableMiniRowsHtml("Top Prodi Sidang", data.topSidangProdi, "Prodi", "Sidang")
                + tableMiniRowsHtml("Top Prodi Wisuda", data.topWisudaProdi, "Prodi", "Wisuda");

        String lulusan = dashboardExplainHtml("Rincian lulusan mengikuti dashboard existing: status keluar, predikat kelulusan, status/pekerjaan/domisili setelah lulus, masa studi, dan semester lulus.")
                + tableMiniRowsHtml("Top Prodi Lulusan", data.topLulusanProdi, "Prodi", "Lulusan")
                + tableMiniRowsHtml("Top Status Keluar Terisi", data.topStatusKeluarProdi, "Prodi", "Data")
                + tableMiniRowsHtml("Top Predikat Terisi", data.topPredikatKelulusanProdi, "Prodi", "Data");

        String masa = dashboardExplainHtml("Masa studi dihitung dari data mahasiswa yang memiliki tahun lulus dan tahun angkatan. Semester lulus dihitung dari field semesterLulus pada mahasiswa.")
                + miniStatHtml("Masa Studi Terdeteksi", data.totalMasaStudiTerdeteksi, "tahunLulus & tahunangkatan")
                + miniStatHtml("Semester Lulus Terisi", data.totalSemesterLulusTerdeteksi, "semesterLulus tidak kosong")
                + tableMiniRowsHtml("Top Prodi Masa Studi", data.topMasaStudiProdi, "Prodi", "Data");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(akademikAkhir) + dashboardPanelHtml(lulusan) + dashboardPanelHtml(masa) + "</div>");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(trendHtml("Tren Bimbingan", data.trendBimbingan))
                + dashboardPanelHtml(trendHtml("Tren Sidang", data.trendSidang))
                + dashboardPanelHtml(trendHtml("Tren Wisuda", data.trendWisuda))
                + dashboardPanelHtml(trendHtml("Tren Lulusan", data.trendLulusan))
                + dashboardPanelHtml(trendHtml("Tren Masa Studi Terdeteksi", data.trendMasaStudi))
                + "</div>");
    }

    /** Panel "Dashboard Kemahasiswaan": kegiatan kemahasiswaan, keanggotaan organisasi intra kampus, dan prestasi mahasiswa. */
    private void renderKemahasiswaanTambahan(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Dashboard Kemahasiswaan",
                "Tambahan dari DashboardKegiatanKemahasiswaanUmum, DashboardOrganisasiIntraKampusUmum, dan DashboardPrestasiMahasiswaUmum. Bagian ini memantau aktivitas non-akademik mahasiswa dalam rentang tahun filter."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Kegiatan", data.totalKegiatanKemahasiswaan, "Kegiatan kemahasiswaan", "#0891b2", "KG", "KEGIATAN");
        appendMetricCard(cards, "Organisasi", data.totalOrganisasiIntraKampus, "Organisasi intra kampus", "#16a34a", "OR", "ORGANISASI");
        appendMetricCard(cards, "Prestasi", data.totalPrestasiMahasiswa, "Prestasi mahasiswa", "#f59e0b", "PR", "PRESTASI");

        String topKegiatan = dashboardExplainHtml("Top prodi berdasarkan jumlah data kegiatan kemahasiswaan mahasiswa.")
                + tableMiniRowsHtml("Top Prodi Kegiatan", data.topKegiatanProdi, "Prodi", "Kegiatan");
        String topOrganisasi = dashboardExplainHtml("Top prodi berdasarkan partisipasi organisasi intra kampus.")
                + tableMiniRowsHtml("Top Prodi Organisasi", data.topOrganisasiProdi, "Prodi", "Organisasi");
        String topPrestasi = dashboardExplainHtml("Top prodi berdasarkan jumlah prestasi mahasiswa.")
                + tableMiniRowsHtml("Top Prodi Prestasi", data.topPrestasiProdi, "Prodi", "Prestasi");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(topKegiatan) + dashboardPanelHtml(topOrganisasi) + dashboardPanelHtml(topPrestasi) + "</div>");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(trendHtml("Tren Kegiatan Kemahasiswaan", data.trendKegiatan))
                + dashboardPanelHtml(trendHtml("Tren Organisasi Intra Kampus", data.trendOrganisasi))
                + dashboardPanelHtml(trendHtml("Tren Prestasi Mahasiswa", data.trendPrestasi))
                + "</div>");
    }


    /** Panel "Dashboard Perkuliahan & Kehadiran": jumlah kelas perkuliahan, pertemuan (termasuk yang sudah lewat/berabsensi), dan beban SKS dosen pengajar. */
    private void renderPerkuliahanKehadiranTambahan(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Dashboard Perkuliahan & Kehadiran",
                "Tambahan dari DashboardPerkuliahan, DashboardKehadiranDosen, DashboardKehadiranMahasiswa, dan DashboardPencapaianPerkuliahan. Ringkasan ini memantau penawaran kelas, detail peserta, pertemuan aktif, absensi, serta pencapaian perkuliahan."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Penawaran Kuliah", data.totalPenawaranPerkuliahan, "Semua kelas pada rentang filter", "var(--ais-theme-primary,#2563eb)", "PK", "PERKULIAHAN");
        appendMetricCard(cards, "Ganjil", data.totalPerkuliahanGanjil, "Semester ganjil reguler", "#0f766e", "G", "PERKULIAHAN_GANJIL");
        appendMetricCard(cards, "Genap", data.totalPerkuliahanGenap, "Semester genap reguler", "#0891b2", "GE", "PERKULIAHAN_GENAP");
        appendMetricCard(cards, "SP", data.totalPerkuliahanSp, "Semester pendek", "#7c3aed", "SP", "PERKULIAHAN_SP");
        appendMetricCard(cards, "Kelas Berjalan", data.totalPerkuliahanBerjalan, data.tahunAkademikBerjalan + " - " + data.semesterBerjalan, "var(--ais-theme-primary,#1d4ed8)", "KB", "PERKULIAHAN_BERJALAN");
        appendMetricCard(cards, "Peserta Disetujui", data.totalDetailDisetujuiBerjalan, "Detailperkuliahan disetujui", "#16a34a", "PD", "DETAIL_PERKULIAHAN_DISETUJUI");
        appendMetricCard(cards, "Pertemuan Aktif", data.totalPertemuanAktif, "Rencana/pertemuan aktif", "#f59e0b", "PT", "PERTEMUAN_AKTIF");
        appendMetricCard(cards, "Absensi Terisi", data.totalAbsensiTerisi, "Pertemuan dengan absensi", "#dc2626", "AB", "ABSENSI_TERISI");
        appendMetricCard(cards, "Pencapaian", data.totalPertemuanBerlalu, "Pertemuan yang sudah berlalu", "#9333ea", "PC", "PENCAPAIAN_PERKULIAHAN");

        String penawaranPanel = dashboardExplainHtml("Penawaran perkuliahan mengikuti pola DashboardPerkuliahan: data Perkuliahan aktif, bukan paralel, dikelompokkan ke semester ganjil, genap, dan semester pendek.")
                + progressSectionHtml("Komposisi Penawaran", data.totalPenawaranPerkuliahan,
                        new DashboardMiniRow("Ganjil", data.totalPerkuliahanGanjil),
                        new DashboardMiniRow("Genap", data.totalPerkuliahanGenap),
                        new DashboardMiniRow("Semester Pendek", data.totalPerkuliahanSp));

        String kehadiranPanel = dashboardExplainHtml("Kehadiran dosen/mahasiswa dan pencapaian perkuliahan pada dashboard rinci memakai data Pertemuan. Di halaman utama ini ditampilkan indikator ringan: pertemuan aktif, pertemuan yang tanggalnya sudah berlalu, dan absensi yang sudah terisi.")
                + miniStatHtml("Pertemuan Aktif", data.totalPertemuanAktif, "Semua pertemuan aktif sesuai filter")
                + miniStatHtml("Pertemuan Berlalu", data.totalPertemuanBerlalu, "Tanggal pertemuan < hari ini")
                + miniStatHtml("Absensi Terisi", data.totalAbsensiTerisi, "Absensi tidak null")
                + miniStatHtml("Rasio Absensi", data.rasioAbsensiTerisi, "% dari pertemuan berlalu/aktif");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(penawaranPanel) + dashboardPanelHtml(kehadiranPanel) + "</div>");

        String topPerkuliahan = dashboardExplainHtml("Top prodi berdasarkan jumlah penawaran kelas perkuliahan pada rentang tahun filter.")
                + tableMiniRowsHtml("Top Prodi Penawaran Kuliah", data.topPerkuliahanProdi, "Prodi", "Kelas");
        String topPertemuan = dashboardExplainHtml("Top prodi berdasarkan jumlah pertemuan aktif. Angka ini membantu melihat beban monitoring kehadiran dan pencapaian pembelajaran.")
                + tableMiniRowsHtml("Top Prodi Pertemuan", data.topPertemuanProdi, "Prodi", "Pertemuan");
        String topAbsensi = dashboardExplainHtml("Top prodi berdasarkan jumlah pertemuan yang sudah memiliki data absensi.")
                + tableMiniRowsHtml("Top Prodi Absensi", data.topAbsensiProdi, "Prodi", "Absensi");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(topPerkuliahan) + dashboardPanelHtml(topPertemuan) + dashboardPanelHtml(topAbsensi) + "</div>");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(trendHtml("Tren Penawaran Kuliah", data.trendPerkuliahan))
                + dashboardPanelHtml(trendHtml("Tren Pertemuan Aktif", data.trendPertemuan))
                + dashboardPanelHtml(trendHtml("Tren Absensi Terisi", data.trendAbsensi))
                + dashboardPanelHtml(trendHtml("Tren Pencapaian/Berlalu", data.trendPencapaian))
                + "</div>");
    }

    /** Panel "Dashboard Dosen, Penelitian, Publikasi, dan Buku Ajar": dosen aktif, kegiatan/organisasi/prestasi/karya dosen, publikasi ilmiah, penelitian & pengabdian, dan buku ajar. */
    private void renderDosenPenelitianTambahan(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Dashboard Dosen, Penelitian, Publikasi, dan Buku Ajar",
                "Tambahan dari DashboardBebanSksDosen, DashboardKegiatanKedosenanUmum, DashboardOrganisasiDosenUmum, DashboardPrestasiDosenUmum, DashboardKaryaDosenUmum, DashboardPublikasiIlmiah, DashboardPenelitianDanPengabdian, dan DashboardBukuAjar."));

        Div cards = new Div();
        cards.setParent(body);
        cards.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;");

        appendMetricCard(cards, "Dosen Aktif", data.totalDosenAktif, "Dosen aktif sesuai filter fakultas/prodi", "#0f766e", "DS", "DOSEN");
        appendMetricCard(cards, "Beban SKS", data.totalBebanSksDosen, "Perkuliahan aktif dengan dosen", "#dc2626", "SKS", "BEBAN_SKS_DOSEN");
        appendMetricCard(cards, "Kegiatan Dosen", data.totalKegiatanDosen, "Kegiatan kedosenan", "#0891b2", "KD", "KEGIATAN_DOSEN");
        appendMetricCard(cards, "Organisasi Dosen", data.totalOrganisasiDosen, "Organisasi dosen", "#16a34a", "OD", "ORGANISASI_DOSEN");
        appendMetricCard(cards, "Prestasi Dosen", data.totalPrestasiDosen, "Prestasi dosen", "#f59e0b", "PD", "PRESTASI_DOSEN");
        appendMetricCard(cards, "Karya Dosen", data.totalKaryaDosen, "Penghargaan/karya dosen", "#7c3aed", "KR", "KARYA_DOSEN");
        appendMetricCard(cards, "Publikasi", data.totalPublikasiIlmiah, "Artikel/publikasi ilmiah", "var(--ais-theme-primary,#2563eb)", "PI", "PUBLIKASI_ILMIAH");
        appendMetricCard(cards, "Penelitian/PkM", data.totalPenelitianPengabdian, "Penelitian dan pengabdian", "#9333ea", "PP", "PENELITIAN_PENGABDIAN");
        appendMetricCard(cards, "Buku Ajar", data.totalBukuAjar, "Buku/bahan ajar", "#0f766e", "BA", "BUKU_AJAR");

        String topDosen = dashboardExplainHtml("Top prodi berdasarkan jumlah dosen aktif dan jumlah kelas/perkuliahan yang memiliki dosen pada rentang filter.")
                + tableMiniRowsHtml("Top Prodi Dosen Aktif", data.topDosenProdi, "Prodi", "Dosen")
                + tableMiniRowsHtml("Top Beban SKS Dosen", data.topBebanSksDosenProdi, "Prodi", "Kelas");
        String topKinerja = dashboardExplainHtml("Aktivitas tridharma dosen: kegiatan kedosenan, organisasi, prestasi, dan karya/penghargaan.")
                + tableMiniRowsHtml("Top Kegiatan Dosen", data.topKegiatanDosenProdi, "Prodi", "Kegiatan")
                + tableMiniRowsHtml("Top Prestasi Dosen", data.topPrestasiDosenProdi, "Prodi", "Prestasi")
                + tableMiniRowsHtml("Top Karya Dosen", data.topKaryaDosenProdi, "Prodi", "Karya");
        String topRiset = dashboardExplainHtml("Produktivitas ilmiah dosen dari publikasi, penelitian/pengabdian, dan buku ajar.")
                + tableMiniRowsHtml("Top Publikasi Ilmiah", data.topPublikasiIlmiahProdi, "Prodi", "Publikasi")
                + tableMiniRowsHtml("Top Penelitian/PkM", data.topPenelitianPengabdianProdi, "Prodi", "Data")
                + tableMiniRowsHtml("Top Buku Ajar", data.topBukuAjarProdi, "Prodi", "Buku");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(topDosen) + dashboardPanelHtml(topKinerja) + dashboardPanelHtml(topRiset) + "</div>");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(trendHtml("Tren Beban SKS Dosen", data.trendBebanSksDosen))
                + dashboardPanelHtml(trendHtml("Tren Kegiatan Dosen", data.trendKegiatanDosen))
                + dashboardPanelHtml(trendHtml("Tren Publikasi Ilmiah", data.trendPublikasiIlmiah))
                + dashboardPanelHtml(trendHtml("Tren Penelitian/PkM", data.trendPenelitianPengabdian))
                + dashboardPanelHtml(trendHtml("Tren Buku Ajar", data.trendBukuAjar))
                + "</div>");
    }

    /** Panel "Sebaran Prodi dan Tren Tahunan": tabel top prodi (lewat {@link #tableMiniRowsHtml}) dan grafik tren tahunan sederhana (lewat {@link #trendHtml}) berbasis rentang tahun filter. */
    private void renderTopTablesAndTrends(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Sebaran Prodi dan Tren Tahunan",
                "Membantu pimpinan melihat prodi dengan volume mahasiswa/pendaftar terbesar, kurikulum terbanyak, serta tren tahunan dari data existing."));

        String topMahasiswa = dashboardExplainHtml("Top prodi berdasarkan jumlah mahasiswa aktif pada rentang angkatan yang dipilih.")
                + tableMiniRowsHtml("Top Prodi Berdasarkan Mahasiswa", data.topMahasiswaProdi, "Prodi", "Mahasiswa");
        String topPendaftar = dashboardExplainHtml("Top prodi berdasarkan peminat PMB/SPMB dari pilihan prodi dan prodi lulus.")
                + tableMiniRowsHtml("Top Prodi Berdasarkan Pendaftar", data.topPendaftarProdi, "Prodi", "Pendaftar");
        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(topMahasiswa) + dashboardPanelHtml(topPendaftar) + "</div>");

        String topLulus = dashboardExplainHtml("Top prodi berdasarkan calon mahasiswa yang sudah memiliki prodi lulus.")
                + tableMiniRowsHtml("Top Prodi Lulus Seleksi", data.topLulusProdi, "Prodi", "Lulus");
        String topKurikulum = dashboardExplainHtml("Top prodi berdasarkan jumlah kurikulum aktif pada rentang tahun kurikulum.")
                + tableMiniRowsHtml("Top Prodi Kurikulum", data.topKurikulumProdi, "Prodi", "Kurikulum");
        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(topLulus) + dashboardPanelHtml(topKurikulum) + "</div>");

        appendHtml(body, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
                + dashboardPanelHtml(trendHtml("Tren Pendaftar PMB", data.trendPendaftar))
                + dashboardPanelHtml(trendHtml("Tren Mahasiswa per Angkatan", data.trendMahasiswa))
                + dashboardPanelHtml(trendHtml("Tren Kurikulum per Tahun", data.trendKurikulum))
                + "</div>");
    }

    /** Panel "Dashboard Rinci Existing": tombol pintasan yang membuka masing-masing dashboard rinci lama (Kurikulum, Calon Mahasiswa, Mahasiswa, dst.) sebagai jendela modal terpisah, menjaga halaman terpadu ini tetap ringan sambil tetap menyediakan akses ke detail operasional. */
    private void renderExistingDashboardShortcut() {
        appendHtml(body, sectionIntroHtml("Dashboard Rinci Existing",
                "Tombol berikut membuka dashboard rinci yang menjadi sumber data utama. Ini menjaga halaman utama tetap ringan, tetapi pengguna tetap bisa turun ke detail operasional."));

        Div panel = new Div();
        panel.setParent(body);
        panel.setStyle("margin-top:12px; padding:15px; background:#ffffff; border:1px solid #e8eef6; border-radius:17px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.05); box-sizing:border-box;");

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(panel);
        toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

        appendShortcutButton(toolbar, "Dashboard Kurikulum", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Kurikulum", new DashboardKurikulum());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Calon Mahasiswa", "/img/offline-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Calon Mahasiswa", new DashboardCalonMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Mahasiswa", "/img/online-red-icon_not_yet_access.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Mahasiswa", new DashboardMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard KRS", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard KRS Mahasiswa", new DashboardKrsMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard IPK", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard IPK Mahasiswa", new DashboardIpkMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Nilai", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Nilai Mahasiswa", new DashboardNilaiMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard SKS", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard SKS Kumulatif", new DashboardSksKumulatifMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Kegiatan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Kegiatan Kemahasiswaan", new DashboardKegiatanKemahasiswaanUmum());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Organisasi", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Organisasi Intra Kampus", new DashboardOrganisasiIntraKampusUmum());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Prestasi", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Prestasi Mahasiswa", new DashboardPrestasiMahasiswaUmum());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Perkuliahan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Perkuliahan", new DashboardPerkuliahan());
            }
        });
        appendShortcutButton(toolbar, "Kehadiran Dosen", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Kehadiran Dosen", new DashboardKehadiranDosen());
            }
        });
        appendShortcutButton(toolbar, "Kehadiran Mahasiswa", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Kehadiran Mahasiswa", new DashboardKehadiranMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Pencapaian Perkuliahan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Pencapaian Perkuliahan", new DashboardPencapaianPerkuliahan());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Bimbingan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Bimbingan Mahasiswa", new DashboardBimbinganMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Sidang", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Sidang Mahasiswa", new DashboardSidangMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Wisuda", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Wisuda Mahasiswa", new DashboardWisudaMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Dashboard Lulusan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Lulusan", new DashboardLulusan());
            }
        });
        appendShortcutButton(toolbar, "Status Keluar", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Status Keluar Mahasiswa", new DashboardStatusKeluarMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Predikat Kelulusan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Predikat Kelulusan", new DashboardPredikatKelulusanMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Status Setelah Lulus", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Status Setelah Lulus", new DashboardStatusSetelahLulusMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Pekerjaan Lulusan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Pekerjaan Setelah Lulus", new DashboardStatusPekerjaanSetelahLulusMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Domisili Lulusan", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Domisili Setelah Lulus", new DashboardStatusDomisiliSetelahLulusMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Masa Studi", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Masa Studi Mahasiswa", new DashboardMasaStudiMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Semester Lulus", "/img/Document-Text-icon.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showDashboardWindow("Dashboard Lulus di Semester", new DashboardLulusDiSemesterMahasiswa());
            }
        });
        appendShortcutButton(toolbar, "Rekap Pendaftar SPMB", "/img/print.png", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Common.displayWindow("/pages/pmb/statistik/rekap_pendaftar_spmb_semua.zul", true, "95%", "95%");
            }
        });
    }

    /** Panel "Rekomendasi Aksi": saran tindak lanjut berbasis skor kesehatan akademik dan rasio-rasio kunci lain (mis. mahasiswa belum KRS, nilai belum disetujui), dirender sebagai daftar item rekomendasi ({@link #recommendationItemHtml}). */
    private void renderRecommendation(DashboardPtData data) {
        appendHtml(body, sectionIntroHtml("Rekomendasi Aksi",
                "Rekomendasi otomatis berdasarkan rasio PMB, kesiapan kurikulum, dan KRS berjalan."));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:12px; margin-top:12px;'>");
        if (data.rasioDaftarUlang < 50 && data.totalLulusSeleksi > 0) {
            sb.append(recommendationItemHtml("Prioritas PMB", "Rasio daftar ulang masih di bawah 50%. Lakukan follow-up calon mahasiswa yang sudah lulus tetapi belum daftar ulang.", "#fef3c7", "#92400e"));
        } else {
            sb.append(recommendationItemHtml("PMB Stabil", "Rasio daftar ulang cukup baik atau belum ada data lulus seleksi pada filter ini.", "#dcfce7", "#166534"));
        }
        if (data.totalProdi > 0 && data.totalKurikulum < data.totalProdi) {
            sb.append(recommendationItemHtml("Review Kurikulum", "Jumlah kurikulum aktif lebih kecil dari prodi aktif. Periksa prodi yang belum memiliki kurikulum pada rentang tahun ini.", "#dbeafe", "var(--ais-theme-primary,#1d4ed8)"));
        } else {
            sb.append(recommendationItemHtml("Kurikulum Siap", "Jumlah kurikulum aktif sudah mencukupi secara agregat terhadap prodi aktif.", "#ecfeff", "#155e75"));
        }
        if (data.rasioKrs < 70 && data.totalMahasiswa > 0) {
            sb.append(recommendationItemHtml("Monitoring KRS", "Rasio mahasiswa terdeteksi KRS berjalan masih rendah. Cek dashboard KRS belum mengambil/sama sekali/sebagian.", "#fee2e2", "#991b1b"));
        } else {
            sb.append(recommendationItemHtml("KRS Terkendali", "Rasio KRS berjalan relatif baik atau data mahasiswa pada filter ini belum tersedia.", "#f0fdf4", "#15803d"));
        }
        if (data.avgIpk > 0.0d && data.avgIpk < 3.0d) {
            sb.append(recommendationItemHtml("Pantau IPK", "Rata-rata IPK berjalan berada di bawah 3.00. Perlu pendampingan akademik dan monitoring prodi.", "#ede9fe", "#5b21b6"));
        }
        if (data.totalNilaiBelum > 0) {
            sb.append(recommendationItemHtml("Validasi Nilai", "Masih ada detail nilai yang belum memiliki nilai huruf. Koordinasikan input nilai dengan prodi/dosen pengampu.", "#fff7ed", "#9a3412"));
        }
        if (data.totalPrestasiMahasiswa <= 0 && data.totalMahasiswa > 0) {
            sb.append(recommendationItemHtml("Dorong Prestasi", "Belum ada prestasi mahasiswa pada filter ini. Perlu program pembinaan dan pencatatan prestasi.", "#fef2f2", "#991b1b"));
        }
        if (data.skorKesehatanAkademik > 0 && data.skorKesehatanAkademik < 70) {
            sb.append(recommendationItemHtml("Kesehatan Akademik", "Skor kesehatan akademik masih di bawah 70%. Prioritaskan KRS, nilai, absensi, dan kelengkapan data lulusan.", "#fff1f2", "#9f1239"));
        }
        sb.append("</div>");
        appendHtml(body, sb.toString());
    }

    /** Menambahkan satu kartu metrik angka (judul, nilai bulat, subjudul, warna aksen, ikon) ke {@code parent}; dapat diklik untuk membuka rincian lewat {@link #showDetailWindow(String)} bila {@code detailKey} diisi. */
    private void appendMetricCard(Component parent, String title, long value, String subtitle, String color, String icon, final String detailKey) {
        Div card = new Div();
        card.setParent(parent);
        card.setStyle("background:#ffffff; border:1px solid #e8eef6; border-radius:17px; padding:15px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.05); min-height:106px; box-sizing:border-box;");

        appendHtml(card, "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px;'>"
                + "<div style='font-size:12px; color:#64748b; font-weight:800; text-transform:uppercase; letter-spacing:.04em;'>"
                + safeHtml(title) + "</div>"
                + "<div style='width:34px; height:34px; border-radius:12px; display:flex; align-items:center; justify-content:center; "
                + "color:#ffffff; font-weight:900; background:" + color + ";'>" + safeHtml(icon) + "</div></div>");

        if (detailKey == null || value <= 0) {
            appendHtml(card, "<div style='font-size:30px; line-height:1; font-weight:900; color:#0f172a; margin-top:12px;'>" + formatNumber(value) + "</div>");
        } else {
            A a = new A(formatNumber(value));
            a.setParent(card);
            a.setStyle("display:block; font-size:30px; line-height:1; font-weight:900; color:var(--ais-theme-primary,#1d4ed8); margin-top:12px; text-decoration:none;");
            a.setTooltiptext("Klik untuk melihat contoh data " + title);
            a.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    showDetailWindow(detailKey);
                }
            });
        }
        appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:6px;'>" + safeHtml(subtitle) + "</div>");
    }

    /** Seperti {@link #appendMetricCard}, untuk nilai yang sudah berupa teks terformat (mis. desimal/persentase) alih-alih bilangan bulat mentah. */
    private void appendValueMetricCard(Component parent, String title, String value, String subtitle, String color, String icon, final String detailKey) {
        Div card = new Div();
        card.setParent(parent);
        card.setStyle("background:#ffffff; border:1px solid #e8eef6; border-radius:17px; padding:15px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.05); min-height:106px; box-sizing:border-box;");

        appendHtml(card, "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px;'>"
                + "<div style='font-size:12px; color:#64748b; font-weight:800; text-transform:uppercase; letter-spacing:.04em;'>"
                + safeHtml(title) + "</div>"
                + "<div style='width:34px; height:34px; border-radius:12px; display:flex; align-items:center; justify-content:center; "
                + "color:#ffffff; font-weight:900; background:" + color + ";'>" + safeHtml(icon) + "</div></div>");

        if (detailKey == null) {
            appendHtml(card, "<div style='font-size:30px; line-height:1; font-weight:900; color:#0f172a; margin-top:12px;'>" + safeHtml(value) + "</div>");
        } else {
            A a = new A(value);
            a.setParent(card);
            a.setStyle("display:block; font-size:30px; line-height:1; font-weight:900; color:var(--ais-theme-primary,#1d4ed8); margin-top:12px; text-decoration:none;");
            a.setTooltiptext("Klik untuk melihat contoh data " + title);
            a.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    showDetailWindow(detailKey);
                }
            });
        }
        appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:6px;'>" + safeHtml(subtitle) + "</div>");
    }

    /** Menambahkan satu tombol pintasan (label+ikon) yang menjalankan {@code listener} saat diklik, dipakai untuk membuka dashboard rinci lama pada {@link #renderExistingDashboardShortcut()}. */
    private void appendShortcutButton(Component parent, String label, String image, EventListener listener) {
        MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(label, image);
        button.setParent(parent);
        button.setTooltiptext(label);
        button.addEventListener("onClick", listener);
    }


    private void appendDosenActivityRows(Rows rows, List list, String[] periodeGetters, String[] namaGetters) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            Row row = new Row();
            row.setParent(rows);
            row.appendChild(new Label(readNestedValue(obj, periodeGetters)));
            row.appendChild(new Label(resolveDosenNama(obj)));
            row.appendChild(new Label(resolveDosenProdi(obj)));
            row.appendChild(new Label(readNestedValue(obj, namaGetters)));
        }
    }

    private void appendPerkuliahanRows(Rows rows, List list) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            Row row = new Row();
            row.setParent(rows);
            row.appendChild(new Label(readValue(obj, "getTahunAjaran")));
            row.appendChild(new Label(readValue(obj, "getSemester")));
            row.appendChild(new Label(readNestedValue(obj, new String[] { "getJurusan", "getNama" })));
            row.appendChild(new Label(readNestedValue(obj, new String[] { "getDosen1", "getNama" })));
            row.appendChild(new Label(readNestedValue(obj, new String[] { "getMatakuliah", "getNama" }) + " / " + readValue(obj, "getKelas")));
        }
    }

    /** Menampilkan {@code dashboard} (komponen jendela dashboard rinci lama) sebagai jendela modal besar (95%x95%), dipakai oleh tombol pintasan pada {@link #renderExistingDashboardShortcut()}. */
    private void showDashboardWindow(String title, Component dashboard) throws Exception {
        Window window = new Window();
        window.setTitle(title);
        window.setBorder("normal");
        window.setClosable(true);
        window.setMaximizable(true);
        window.setWidth("95%");
        window.setHeight("95%");
        attachPopupWindow(window);

        dashboard.setParent(window);
        applyComponentSizeIfSupported(dashboard, "100%", "100%");

        window.doModal();
    }


    private void showSimpleInfoWindow(String title, String message) throws Exception {
        Window window = new Window();
        window.setTitle(title);
        window.setBorder("normal");
        window.setClosable(true);
        window.setWidth("520px");
        window.setHeight("220px");
        attachPopupWindow(window);
        appendHtml(window, "<div style='padding:18px; font-size:13px; color:#475569; line-height:1.55;'>" + safeHtml(message) + "</div>");
        window.doModal();
    }

    private void applyComponentSizeIfSupported(Component component, String width, String height) {
        if (component == null) {
            return;
        }

        /*
         * org.zkoss.zk.ui.Component pada ZK 5.5 tidak memiliki method setWidth/setHeight.
         * Beberapa dashboard turunan MyWindow memilikinya, tetapi pemanggilan harus lewat
         * concrete class/reflection agar tidak error compile.
         */
        invokeStringSetter(component, "setWidth", width);
        invokeStringSetter(component, "setHeight", height);
    }

    private void invokeStringSetter(Component component, String methodName, String value) {
        try {
            Method method = component.getClass().getMethod(methodName, new Class[] { String.class });
            method.invoke(component, new Object[] { value });
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborPerguruanTinggiTerpadu.java:1441");
            // Abaikan jika concrete component tidak mendukung method tersebut.
        }
    }

    /** Menempelkan {@code window} popup ke {@code body} (atau induk lain sebagai fallback) alih-alih langsung ke {@code this}, karena {@link MyPortallayout} hanya menerima {@code MyPortalchildren} sebagai anak langsung. */
    private void attachPopupWindow(Window window) {
        /*
         * ZK MyPortallayout hanya menerima MyPortalchildren sebagai direct child.
         * Karena itu Window popup tidak boleh setParent(this).
         * Popup ditempelkan ke body Div / parent luar agar tetap bisa doModal().
         */
        if (body != null) {
            window.setParent(body);
        } else if (getParent() != null) {
            window.setParent(getParent());
        } else if (getPage() != null) {
            window.setPage(getPage());
        }
    }

    /**
     * Menampilkan jendela modal berisi tabel rincian (dibatasi 150 baris) untuk satu metrik kartu
     * yang diklik: {@code detailKey} menentukan cabang kriteria yang dipakai (menggunakan
     * method {@code create*Criteria} yang SAMA dengan yang dipakai untuk menghitung ringkasan
     * pada kartu, menjamin konsistensi angka vs rincian) dan susunan kolom tabel yang ditampilkan.
     *
     * @param detailKey kunci metrik (mis. {@code "FAKULTAS"}, {@code "PRODI"}, {@code "KURIKULUM"}, dst.) yang menentukan data dan kolom yang ditampilkan
     */
    private void showDetailWindow(String detailKey) throws Exception {
        Window window = new Window();
        window.setTitle(resolveDetailTitle(detailKey));
        window.setBorder("normal");
        window.setClosable(true);
        window.setMaximizable(true);
        window.setWidth("92%");
        window.setHeight("85%");
        attachPopupWindow(window);
        window.setStyle("background:#ffffff;");

        Div wrap = new Div();
        wrap.setParent(window);
        wrap.setStyle("padding:12px; overflow:auto; height:100%; box-sizing:border-box;");

        appendHtml(wrap, dashboardExplainHtml("Data di bawah ini dibatasi maksimal 150 baris agar popup tetap ringan. Gunakan dashboard rinci existing untuk download/analisis penuh."));

        MyGrid grid = new MyGrid();
        grid.setParent(wrap);
        grid.setWidth("100%");
        grid.setSclass("fgrid");
        grid.setStyle("border:0; background:transparent; border-radius:14px; overflow:hidden;");

        Columns columns = new Columns();
        columns.setParent(grid);
        Rows rows = new Rows();
        rows.setParent(grid);

        if ("FAKULTAS".equals(detailKey)) {
            addColumn(columns, "Nama", "40%");
            addColumn(columns, "Keterangan", "60%");
            List list = createFakultasCriteria(HibernateUtil.currentSession()).addOrder(Order.asc("nama")).setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getNama")));
                row.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas aktif")));
            }
        } else if ("PRODI".equals(detailKey)) {
            addColumn(columns, "Fakultas", "30%");
            addColumn(columns, "Prodi", "45%");
            addColumn(columns, "Keterangan", "25%");
            List list = createJurusanCriteria(HibernateUtil.currentSession()).addOrder(Order.asc("fakultas")).addOrder(Order.asc("nama")).setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Jurusan j = (Jurusan) list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(j.getFakultas() == null ? "-" : safeText(j.getFakultas().getNama())));
                row.appendChild(new Label(safeText(j.getNama())));
                row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prodi aktif")));
            }
        } else if ("KURIKULUM".equals(detailKey)) {
            addColumn(columns, "Tahun", "12%");
            addColumn(columns, "Prodi", "38%");
            addColumn(columns, "Nama", "50%");
            List list = createKurikulumCriteria(HibernateUtil.currentSession(), null).addOrder(Order.desc("tahun")).setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahun")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getJurusan", "getNama" })));
                row.appendChild(new Label(readValue(obj, "getNama")));
            }
        } else if ("PENDAFTAR".equals(detailKey) || "LULUS".equals(detailKey) || "DAFTAR_ULANG".equals(detailKey) || "DAPAT_NIM".equals(detailKey)) {
            addColumn(columns, "Tahun", "10%");
            addColumn(columns, "Nama", "28%");
            addColumn(columns, "Pilihan/Lulus", "34%");
            addColumn(columns, "Status", "28%");
            Criteria criteria = createCalonCriteria(HibernateUtil.currentSession(), "LULUS".equals(detailKey) || "DAFTAR_ULANG".equals(detailKey) || "DAPAT_NIM".equals(detailKey),
                    "DAFTAR_ULANG".equals(detailKey), "DAPAT_NIM".equals(detailKey), false, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahun")));
                row.appendChild(new Label(resolveCalonNama(obj)));
                row.appendChild(new Label(resolveCalonProdiInfo(obj)));
                row.appendChild(new Label(resolveCalonStatus(obj)));
            }
        } else if ("MAHASISWA".equals(detailKey)) {
            addColumn(columns, "Angkatan", "10%");
            addColumn(columns, "NIM", "18%");
            addColumn(columns, "Nama", "30%");
            addColumn(columns, "Prodi", "30%");
            addColumn(columns, "Program", "12%");
            Criteria criteria = createMahasiswaCriteria(HibernateUtil.currentSession(), null);
            criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunangkatan")));
                row.appendChild(new Label(readValue(obj, "getNim")));
                row.appendChild(new Label(readValue(obj, "getNama")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getJurusan", "getNama" })));
                row.appendChild(new Label(readValue(obj, "getProgram")));
            }
        } else if ("KRS_MHS".equals(detailKey) || "DETAIL_KRS".equals(detailKey) || "NILAI".equals(detailKey) || "NILAI_BELUM".equals(detailKey)) {
            addColumn(columns, "TA", "10%");
            addColumn(columns, "Semester", "10%");
            addColumn(columns, "NIM", "16%");
            addColumn(columns, "Nama", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Nilai", "14%");
            Criteria criteria;
            if ("NILAI".equals(detailKey) || "NILAI_BELUM".equals(detailKey)) {
                criteria = createDetailNilaiCriteria(HibernateUtil.currentSession(), Common.getCurrentTahunAkademik(), "NILAI_BELUM".equals(detailKey));
            } else {
                criteria = createDetailKrsCriteria(HibernateUtil.currentSession(), Common.getCurrentTahunAkademik(), Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
            }
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunAkademik")));
                row.appendChild(new Label(readValue(obj, "getSemester")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                row.appendChild(new Label(readValue(obj, "getNilaiHuruf")));
            }
        } else if ("IPK".equals(detailKey) || "SKS_KUM".equals(detailKey)) {
            addColumn(columns, "TA", "10%");
            addColumn(columns, "NIM", "16%");
            addColumn(columns, "Nama", "28%");
            addColumn(columns, "Prodi", "28%");
            addColumn(columns, "IPK", "9%");
            addColumn(columns, "SKS", "9%");
            Criteria criteria = createKrsMahasiswaCriteria(HibernateUtil.currentSession(), Common.getCurrentTahunAkademik(), Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP, "IPK".equals(detailKey) ? "ipk" : "sksk");
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunAkademik")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                row.appendChild(new Label(readValue(obj, "getIpk")));
                row.appendChild(new Label(readValue(obj, "getSksk")));
            }
        } else if ("KEGIATAN".equals(detailKey)) {
            addColumn(columns, "TA", "10%");
            addColumn(columns, "NIM", "14%");
            addColumn(columns, "Nama", "24%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Kegiatan", "28%");
            Criteria criteria = createKegiatanKemahasiswaanCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getKegiatanKemahasiswaan", "getTahunAkademik" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getKegiatanKemahasiswaan", "getNama" })));
            }
        } else if ("ORGANISASI".equals(detailKey)) {
            addColumn(columns, "Tahun", "10%");
            addColumn(columns, "NIM", "14%");
            addColumn(columns, "Nama", "24%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Organisasi", "28%");
            Criteria criteria = createOrganisasiIntraKampusCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahun")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getOrganisasiIntraKampus", "getNama" })));
            }
        } else if ("PRESTASI".equals(detailKey)) {
            addColumn(columns, "TA", "10%");
            addColumn(columns, "NIM", "14%");
            addColumn(columns, "Nama", "24%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Prestasi", "28%");
            Criteria criteria = createPrestasiMahasiswaCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunAkademik")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getCabangPrestasiMahasiswa", "getNama" }) + " / " + readNestedValue(obj, new String[] { "getKategoriPrestasiMahasiswa", "getNama" })));
            }
        } else if ("BIMBINGAN_MHS".equals(detailKey) || "SIDANG_MHS".equals(detailKey) || "WISUDA_MHS".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "NIM", "14%");
            addColumn(columns, "Nama", "24%");
            addColumn(columns, "Prodi", "22%");
            addColumn(columns, "Keterangan", "28%");
            Criteria criteria;
            if ("BIMBINGAN_MHS".equals(detailKey)) {
                criteria = createBimbinganMahasiswaCriteria(HibernateUtil.currentSession(), null, null);
            } else if ("SIDANG_MHS".equals(detailKey)) {
                criteria = createSidangMahasiswaCriteria(HibernateUtil.currentSession(), null, null);
            } else {
                criteria = createWisudaMahasiswaCriteria(HibernateUtil.currentSession(), null, null);
            }
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                if ("WISUDA_MHS".equals(detailKey)) {
                    row.appendChild(new Label(readNestedValue(obj, new String[] { "getSkripsi", "getTahunAkademik" })));
                } else {
                    row.appendChild(new Label(readValue(obj, "getTahunAkademik")));
                }
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                if ("WISUDA_MHS".equals(detailKey)) {
                    row.appendChild(new Label(readNestedValue(obj, new String[] { "getWisuda", "getNama" }) + " / " + readValue(obj, "getPersetujuanWisuda")));
                } else if ("SIDANG_MHS".equals(detailKey)) {
                    row.appendChild(new Label(readValue(obj, "getJudul") + " / Sidang: " + readValue(obj, "getTelahSidang")));
                } else {
                    row.appendChild(new Label(readValue(obj, "getJudul") + " / Status: " + readValue(obj, "getStatus")));
                }
            }
        } else if ("LULUSAN_MHS".equals(detailKey) || "STATUS_KELUAR_MHS".equals(detailKey)
                || "PREDIKAT_KELULUSAN_MHS".equals(detailKey) || "STATUS_SETELAH_LULUS_MHS".equals(detailKey)
                || "PEKERJAAN_SETELAH_LULUS_MHS".equals(detailKey) || "DOMISILI_SETELAH_LULUS_MHS".equals(detailKey)
                || "MASA_STUDI_MHS".equals(detailKey) || "SEMESTER_LULUS_MHS".equals(detailKey)) {
            addColumn(columns, "Tahun Lulus", "10%");
            addColumn(columns, "NIM", "14%");
            addColumn(columns, "Nama", "24%");
            addColumn(columns, "Prodi", "22%");
            addColumn(columns, "Keterangan", "30%");
            String field = null;
            if ("STATUS_KELUAR_MHS".equals(detailKey)) field = "statusKeluar";
            else if ("PREDIKAT_KELULUSAN_MHS".equals(detailKey)) field = "predikatKelulusan";
            else if ("STATUS_SETELAH_LULUS_MHS".equals(detailKey)) field = "statusSetelahLulus";
            else if ("PEKERJAAN_SETELAH_LULUS_MHS".equals(detailKey)) field = "statusPekerjaanSetelahLulus";
            else if ("DOMISILI_SETELAH_LULUS_MHS".equals(detailKey)) field = "statusDomisiliSetelahLulus";
            else if ("SEMESTER_LULUS_MHS".equals(detailKey)) field = "semesterLulus";
            Criteria criteria = "MASA_STUDI_MHS".equals(detailKey)
                    ? createMasaStudiMahasiswaCriteria(HibernateUtil.currentSession(), null, null)
                    : createLulusanMahasiswaCriteria(HibernateUtil.currentSession(), null, null, field);
            criteria.addOrder(Order.desc("tahunLulus")).addOrder(Order.asc("nim"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunLulus")));
                row.appendChild(new Label(readValue(obj, "getNim")));
                row.appendChild(new Label(readValue(obj, "getNama")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getJurusan", "getNama" })));
                row.appendChild(new Label(resolveLulusanInfo(obj, detailKey)));
            }
        } else if ("DOSEN".equals(detailKey)) {
            addColumn(columns, "NIDN", "18%");
            addColumn(columns, "Nama", "32%");
            addColumn(columns, "Fakultas", "25%");
            addColumn(columns, "Prodi", "25%");
            Criteria criteria = createDosenCriteria(HibernateUtil.currentSession(), null);
            criteria.addOrder(Order.asc("nama"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getNidn")));
                row.appendChild(new Label(readValue(obj, "getNama")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getFakultas", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getJurusan", "getNama" })));
            }
        } else if ("BEBAN_SKS_DOSEN".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Dosen", "24%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Mata Kuliah", "40%");
            Criteria criteria = createPerkuliahanDosenCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("tahunAjaran"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunAjaran")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getDosen1", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getJurusan", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMatakuliah", "getNama" })));
            }
        } else if ("KEGIATAN_DOSEN".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Kegiatan", "38%");
            Criteria criteria = createKegiatanDosenCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getKegiatanKedosenan", "getTahunAkademik" }, new String[] { "getKegiatanKedosenan" });
        } else if ("ORGANISASI_DOSEN".equals(detailKey)) {
            addColumn(columns, "Tahun", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Organisasi", "38%");
            Criteria criteria = createOrganisasiDosenCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getTahun" }, new String[] { "getOrganisasiDosen" });
        } else if ("PRESTASI_DOSEN".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Prestasi", "38%");
            Criteria criteria = createPrestasiDosenCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getTahunAkademik" }, new String[] { "getCabangPrestasiDosen", "getNama" });
        } else if ("KARYA_DOSEN".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Karya", "38%");
            Criteria criteria = createKaryaDosenCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getTahunAkademik" }, new String[] { "getKategoriPenghargaan", "getNama" });
        } else if ("PUBLIKASI_ILMIAH".equals(detailKey)) {
            addColumn(columns, "Tahun", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Judul", "38%");
            Criteria criteria = createPublikasiIlmiahCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getTahun" }, new String[] { "getJudul" });
        } else if ("PENELITIAN_PENGABDIAN".equals(detailKey)) {
            addColumn(columns, "Tahun", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Judul", "38%");
            Criteria criteria = createPenelitianPengabdianCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getPenelitianDanPengabdian", "getTahun" }, new String[] { "getJudul" });
        } else if ("BUKU_AJAR".equals(detailKey)) {
            addColumn(columns, "Tahun", "12%");
            addColumn(columns, "Dosen", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Buku", "38%");
            Criteria criteria = createBukuAjarCriteria(HibernateUtil.currentSession(), null, null);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendDosenActivityRows(rows, list, new String[] { "getTahun" }, new String[] { "getNama" });
        } else if ("PERKULIAHAN".equals(detailKey) || "PERKULIAHAN_GANJIL".equals(detailKey)
                || "PERKULIAHAN_GENAP".equals(detailKey) || "PERKULIAHAN_SP".equals(detailKey)
                || "PERKULIAHAN_BERJALAN".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Semester", "10%");
            addColumn(columns, "Prodi", "23%");
            addColumn(columns, "Dosen", "25%");
            addColumn(columns, "Mata Kuliah/Kelas", "30%");
            String semesterFilter = null;
            Integer tahunFilter = null;
            if ("PERKULIAHAN_GANJIL".equals(detailKey)) {
                semesterFilter = Perkuliahan.GANJIL;
            } else if ("PERKULIAHAN_GENAP".equals(detailKey)) {
                semesterFilter = Perkuliahan.GENAP;
            } else if ("PERKULIAHAN_SP".equals(detailKey)) {
                semesterFilter = Perkuliahan.SP;
            } else if ("PERKULIAHAN_BERJALAN".equals(detailKey)) {
                semesterFilter = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
                tahunFilter = new Integer(parseTahunAkademikAwal(Common.getCurrentTahunAkademik()));
            }
            Criteria criteria = createPerkuliahanCriteria(HibernateUtil.currentSession(), null, tahunFilter, semesterFilter);
            criteria.addOrder(Order.desc("tahunAjaran")).addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            appendPerkuliahanRows(rows, list);
        } else if ("DETAIL_PERKULIAHAN_DISETUJUI".equals(detailKey)) {
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Semester", "10%");
            addColumn(columns, "NIM", "16%");
            addColumn(columns, "Nama", "26%");
            addColumn(columns, "Prodi", "24%");
            addColumn(columns, "Keterangan", "12%");
            Criteria criteria = createDetailPerkuliahanDisetujuiCriteria(HibernateUtil.currentSession(), Common.getCurrentTahunAkademik(), Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
            criteria.addOrder(Order.desc("id"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTahunAkademik")));
                row.appendChild(new Label(readValue(obj, "getSemester")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNim" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getMahasiswa", "getJurusan", "getNama" })));
                row.appendChild(new Label(ais.common.Common.getBahasaConfig("Disetujui")));
            }
        } else if ("PERTEMUAN_AKTIF".equals(detailKey) || "ABSENSI_TERISI".equals(detailKey) || "PENCAPAIAN_PERKULIAHAN".equals(detailKey)) {
            addColumn(columns, "Tanggal", "14%");
            addColumn(columns, "TA", "12%");
            addColumn(columns, "Prodi", "22%");
            addColumn(columns, "Mata Kuliah", "28%");
            addColumn(columns, "Topik/Status", "24%");
            boolean onlyPast = "PENCAPAIAN_PERKULIAHAN".equals(detailKey);
            boolean onlyAbsensi = "ABSENSI_TERISI".equals(detailKey);
            Criteria criteria = createPertemuanCriteria(HibernateUtil.currentSession(), null, null, onlyPast, onlyAbsensi);
            criteria.addOrder(Order.desc("tanggal"));
            List list = criteria.setMaxResults(150).list();
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Row row = new Row();
                row.setParent(rows);
                row.appendChild(new Label(readValue(obj, "getTanggal")));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getPerkuliahan", "getTahunAjaran" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getPerkuliahan", "getJurusan", "getNama" })));
                row.appendChild(new Label(readNestedValue(obj, new String[] { "getPerkuliahan", "getMatakuliah", "getNama" })));
                row.appendChild(new Label(readValue(obj, "getTopik")));
            }
        }

        if (rows.getChildren().isEmpty()) {
            Row row = new Row();
            row.setParent(rows);
            row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data pada filter ini.")));
        }
        window.doModal();
    }

    private void addColumn(Columns columns, String label, String width) {
        Column column = new MyColumnConfig(label);
        column.setParent(columns);
        if (width != null) {
            column.setWidth(width);
        }
    }

    private long countFakultasAktif() {
        return rowCount(createFakultasCriteria(HibernateUtil.currentSession()));
    }

    private long countJurusanAktif() {
        return rowCount(createJurusanCriteria(HibernateUtil.currentSession()));
    }

    private long countKurikulumAktif() {
        return rowCount(createKurikulumCriteria(HibernateUtil.currentSession(), null));
    }

    private long countKurikulumByJurusan(Jurusan jurusan) {
        return rowCount(createKurikulumCriteria(HibernateUtil.currentSession(), jurusan));
    }

    private long countKurikulumByTahun(int tahun) {
        Criteria criteria = createKurikulumCriteria(HibernateUtil.currentSession(), null);
        criteria.add(Restrictions.eq("tahun", new Integer(tahun)));
        return rowCount(criteria);
    }

    private long countMahasiswa(Jurusan forcedJurusan) {
        return rowCount(createMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan));
    }

    private long countMahasiswaByTahun(int tahun) {
        Criteria criteria = createMahasiswaCriteria(HibernateUtil.currentSession(), null);
        criteria.add(Restrictions.eq("tahunangkatan", new Integer(tahun)));
        return rowCount(criteria);
    }

    private long countCalon(boolean onlyLulus, boolean daftarUlang, boolean dapatNim, boolean formTambahan, Integer tahun) {
        return rowCount(createCalonCriteria(HibernateUtil.currentSession(), onlyLulus, daftarUlang, dapatNim, formTambahan, tahun));
    }

    private long countCalonByJurusan(Jurusan jurusan, boolean onlyLulus, boolean daftarUlang, boolean dapatNim) {
        Criteria criteria = createCalonCriteria(HibernateUtil.currentSession(), onlyLulus, daftarUlang, dapatNim, false, null);
        applyCalonJurusanFilter(criteria, jurusan, onlyLulus || daftarUlang || dapatNim);
        return rowCount(criteria);
    }

    private long countMahasiswaKrsBerjalan(String tahunAkademik, String semester) {
        try {
            if (tahunAkademik == null || tahunAkademik.trim().length() == 0) {
                return 0L;
            }
            Criteria criteria = createDetailKrsCriteria(HibernateUtil.currentSession(), tahunAkademik, semester);
            Object obj = criteria.setProjection(Projections.countDistinct("mahasiswa.id")).uniqueResult();
            return toLong(obj);
        } catch (Exception e) {
            debugError("countMahasiswaKrsBerjalan", e);
            return 0L;
        }
    }

    private long countDetailKrsBerjalan(String tahunAkademik, String semester) {
        return rowCount(createDetailKrsCriteria(HibernateUtil.currentSession(), tahunAkademik, semester));
    }

    private long countDetailNilai(String tahunAkademik, boolean hanyaBelum) {
        return rowCount(createDetailNilaiCriteria(HibernateUtil.currentSession(), tahunAkademik, hanyaBelum));
    }

    private long countKrsNumber(String field, String tahunAkademik, String semester) {
        return rowCount(createKrsMahasiswaCriteria(HibernateUtil.currentSession(), tahunAkademik, semester, field));
    }

    private double aggregateKrsNumber(String field, String aggregate, String tahunAkademik, String semester) {
        try {
            Criteria criteria = createKrsMahasiswaCriteria(HibernateUtil.currentSession(), tahunAkademik, semester, field);
            if ("min".equals(aggregate)) {
                criteria.setProjection(Projections.min(field));
            } else if ("max".equals(aggregate)) {
                criteria.setProjection(Projections.max(field));
            } else {
                criteria.setProjection(Projections.avg(field));
            }
            Object obj = criteria.uniqueResult();
            return obj instanceof Number ? ((Number) obj).doubleValue() : 0.0d;
        } catch (Exception e) {
            debugError("aggregateKrsNumber-" + field + "-" + aggregate, e);
            return 0.0d;
        }
    }

    private long countKegiatanKemahasiswaan(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createKegiatanKemahasiswaanCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countOrganisasiIntraKampus(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createOrganisasiIntraKampusCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countPrestasiMahasiswa(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createPrestasiMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }


    private long countDosenAktif(Jurusan forcedJurusan) {
        return rowCount(createDosenCriteria(HibernateUtil.currentSession(), forcedJurusan));
    }

    private long countBebanSksDosen(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createPerkuliahanDosenCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countKegiatanDosen(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createKegiatanDosenCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countOrganisasiDosen(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createOrganisasiDosenCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countPrestasiDosen(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createPrestasiDosenCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countKaryaDosen(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createKaryaDosenCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countPublikasiIlmiah(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createPublikasiIlmiahCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countPenelitianPengabdian(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createPenelitianPengabdianCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countBukuAjar(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createBukuAjarCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countPerkuliahan(Jurusan forcedJurusan, Integer tahun, String semester) {
        return rowCount(createPerkuliahanCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun, semester));
    }

    private long countPerkuliahanByTahunAkademik(String tahunAkademik, String semester, Jurusan forcedJurusan) {
        return rowCount(createPerkuliahanCriteria(HibernateUtil.currentSession(), forcedJurusan,
                new Integer(parseTahunAkademikAwal(tahunAkademik)), semester));
    }

    private long countDetailPerkuliahanDisetujui(String tahunAkademik, String semester) {
        return rowCount(createDetailPerkuliahanDisetujuiCriteria(HibernateUtil.currentSession(), tahunAkademik, semester));
    }

    private long countPertemuan(Jurusan forcedJurusan, Integer tahun, boolean onlyPast, boolean onlyWithAbsensi) {
        return rowCount(createPertemuanCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun, onlyPast, onlyWithAbsensi));
    }


    private long countBimbinganMahasiswa(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createBimbinganMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countSidangMahasiswa(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createSidangMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countWisudaMahasiswa(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createWisudaMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private long countLulusanMahasiswa(Jurusan forcedJurusan, Integer tahun, String requiredField) {
        return rowCount(createLulusanMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun, requiredField));
    }

    private long countMasaStudiMahasiswa(Jurusan forcedJurusan, Integer tahun) {
        return rowCount(createMasaStudiMahasiswaCriteria(HibernateUtil.currentSession(), forcedJurusan, tahun));
    }

    private Criteria createBimbinganMahasiswaCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(MahasiswaRequestTugasAkhir.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyMahasiswaAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createSidangMahasiswaCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(Skripsi.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyMahasiswaAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createWisudaMahasiswaCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(PendaftaranWisuda.class)
                .createAlias("mahasiswa", "mahasiswa")
                .createAlias("skripsi", "skripsi")
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("skripsi.tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("skripsi.tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyMahasiswaAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createLulusanMahasiswaCriteria(Session session, Jurusan forcedJurusan, Integer tahun, String requiredField) {
        Criteria criteria = session.createCriteria(Mahasiswa.class)
                .add(activeCriterion())
                .add(Restrictions.isNotNull("tahunLulus"));
        if (tahun == null) {
            criteria.add(Restrictions.between("tahunLulus", new Integer(currentMulai), new Integer(currentSampai)));
        } else {
            criteria.add(Restrictions.eq("tahunLulus", tahun));
        }
        if (requiredField != null && requiredField.trim().length() > 0) {
            criteria.add(Restrictions.isNotNull(requiredField));
        }
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("program", currentProgram));
        }
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.createAlias("jurusan", "jurusanLulusanAlias");
            criteria.add(Restrictions.eq("jurusanLulusanAlias.fakultas", currentFakultas));
        }
        return criteria;
    }

    private Criteria createMasaStudiMahasiswaCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = createLulusanMahasiswaCriteria(session, forcedJurusan, tahun, null);
        criteria.add(Restrictions.isNotNull("tahunangkatan"));
        return criteria;
    }
    private Criteria createKrsMahasiswaCriteria(Session session, String tahunAkademik, String semester, String positiveField) {
        Criteria criteria = session.createCriteria(KrsMahasiswa.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(tahunAkademik == null || tahunAkademik.trim().length() == 0 ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahunAkademik", tahunAkademik))
                .add(semester == null || semester.trim().length() == 0 ? Restrictions.sqlRestriction("true") : Restrictions.sqlRestriction("semester%2=" + (Perkuliahan.GENAP.equals(semester) ? "0" : "1")))
                .add(Restrictions.isNull("semesterPendek"))
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (positiveField != null && positiveField.trim().length() > 0) {
            criteria.add(createPositiveKrsFieldCriterion(positiveField));
        }
        applyMahasiswaAliasFilter(criteria, null);
        return criteria;
    }


    private Criterion createPositiveKrsFieldCriterion(String positiveField) {
        if ("ipk".equalsIgnoreCase(positiveField)) {
            return Restrictions.sqlRestriction("{alias}.ipk > 0");
        }
        if ("sksk".equalsIgnoreCase(positiveField)) {
            return Restrictions.sqlRestriction("{alias}.sksk > 0");
        }
        return Restrictions.sqlRestriction("true");
    }

    private Criteria createDetailKrsCriteria(Session session, String tahunAkademik, String semester) {
        Criteria criteria = session.createCriteria(Detailperkuliahan.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(tahunAkademik == null || tahunAkademik.trim().length() == 0 ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahunAkademik", tahunAkademik))
                .add(semester == null || semester.trim().length() == 0 ? Restrictions.sqlRestriction("true") : Restrictions.sqlRestriction("semester%2=" + (Perkuliahan.GENAP.equals(semester) ? "0" : "1")))
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        applyMahasiswaAliasFilter(criteria, null);
        return criteria;
    }

    private Criteria createDetailNilaiCriteria(Session session, String tahunAkademik, boolean hanyaBelum) {
        Criteria criteria = session.createCriteria(Detailperkuliahan.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(tahunAkademik == null || tahunAkademik.trim().length() == 0 ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahunAkademik", tahunAkademik))
                .add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (hanyaBelum) {
            criteria.add(Restrictions.or(Restrictions.isNull("nilaiHuruf"), Restrictions.eq("nilaiHuruf", "")));
        }
        applyMahasiswaAliasFilter(criteria, null);
        return criteria;
    }

    private Criteria createKegiatanKemahasiswaanCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
                .createAlias("kegiatanKemahasiswaan", "kegiatanKemahasiswaan")
                .createAlias("mahasiswa", "mahasiswa")
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("kegiatanKemahasiswaan.tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("kegiatanKemahasiswaan.tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyMahasiswaAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createOrganisasiIntraKampusCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (tahun == null) {
            criteria.add(Restrictions.between("tahun", new Integer(currentMulai), new Integer(currentSampai)));
        } else {
            criteria.add(Restrictions.eq("tahun", tahun));
        }
        applyMahasiswaAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createPrestasiMahasiswaCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(PrestasiMahasiswa.class)
                .createAlias("mahasiswa", "mahasiswa")
                .add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyMahasiswaAliasFilter(criteria, forcedJurusan);
        return criteria;
    }


    private Criteria createDosenCriteria(Session session, Jurusan forcedJurusan) {
        Criteria criteria = session.createCriteria(Dosen.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.add(Restrictions.eq("fakultas", currentFakultas));
        }
        return criteria;
    }

    private Criteria createPerkuliahanDosenCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("dosen1"))
                .add(Restrictions.isNull("perkuliahan_paralel"));
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("program", currentProgram));
        }
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAjaran", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAjaran", toTahunAkademik(tahun.intValue())));
        }
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.createAlias("jurusan", "jurusanBebanSks");
            criteria.add(Restrictions.eq("jurusanBebanSks.fakultas", currentFakultas));
        }
        return criteria;
    }


    private Criteria createPerkuliahanCriteria(Session session, Jurusan forcedJurusan, Integer tahun, String semester) {
        Criteria criteria = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNull("perkuliahan_paralel"));
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("program", currentProgram));
        }
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAjaran", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAjaran", toTahunAkademik(tahun.intValue())));
        }
        applyPerkuliahanSemesterRestriction(criteria, semester);
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.createAlias("jurusan", "jurusanPerkuliahan");
            criteria.add(Restrictions.eq("jurusanPerkuliahan.fakultas", currentFakultas));
        }
        return criteria;
    }

    private void applyPerkuliahanSemesterRestriction(Criteria criteria, String semester) {
        if (criteria == null || semester == null || semester.trim().length() == 0) {
            return;
        }
        if (Perkuliahan.SP.equals(semester)) {
            criteria.add(Restrictions.sqlRestriction("{alias}.status_semesterpendek=" + Perkuliahan.SEMESTER_PENDEK));
        } else {
			criteria.add(Restrictions.and(Restrictions.isNull("statusSemesterPendek"),
					Restrictions.eq("ganjilGenap", semester)));
        }
    }

    private Criteria createDetailPerkuliahanDisetujuiCriteria(Session session, String tahunAkademik, String semester) {
        Criteria criteria = createDetailKrsCriteria(session, tahunAkademik, semester);
        criteria.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI));
        return criteria;
    }

    private Criteria createPertemuanCriteria(Session session, Jurusan forcedJurusan, Integer tahun, boolean onlyPast, boolean onlyWithAbsensi) {
        Criteria criteria = session.createCriteria(Pertemuan.class)
                .createAlias("perkuliahan", "perkuliahan")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.or(Restrictions.isNull("perkuliahan.aktif"), Restrictions.eq("perkuliahan.aktif", true)))
                .add(Restrictions.isNull("perkuliahan.perkuliahan_paralel"));
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("perkuliahan.program", currentProgram));
        }
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("perkuliahan.tahunAjaran", tas));
        } else {
            criteria.add(Restrictions.eq("perkuliahan.tahunAjaran", toTahunAkademik(tahun.intValue())));
        }
        if (onlyPast) {
            Date now = ais.ui.util.WaktuUtil.getDate();
            criteria.add(Restrictions.isNotNull("tanggal"));
            criteria.add(Restrictions.lt("tanggal", now));
        }
        if (onlyWithAbsensi) {
            criteria.add(Restrictions.isNotNull("absensi"));
        }
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("perkuliahan.jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.createAlias("perkuliahan.jurusan", "jurusanPertemuan");
            criteria.add(Restrictions.eq("jurusanPertemuan.fakultas", currentFakultas));
        }
        return criteria;
    }

    private Criteria createKegiatanDosenCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(KegiatanKedosenanPunyaDosen.class)
                .createAlias("kegiatanKedosenan", "kegiatanKedosenan")
                .createAlias("dosen", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("kegiatanKedosenan.tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("kegiatanKedosenan.tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createOrganisasiDosenCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(OrganisasiDosenPunyaDosen.class)
                .createAlias("dosen", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            criteria.add(Restrictions.between("tahun", new Integer(currentMulai), new Integer(currentSampai)));
        } else {
            criteria.add(Restrictions.eq("tahun", tahun));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createPrestasiDosenCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(PrestasiDosen.class)
                .createAlias("dosen", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createKaryaDosenCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(PenghargaanDosen.class)
                .createAlias("dosen", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            List<String> tas = academicYearList(currentMulai, currentSampai);
            criteria.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("tahunAkademik", tas));
        } else {
            criteria.add(Restrictions.eq("tahunAkademik", toTahunAkademik(tahun.intValue())));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createPublikasiIlmiahCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(Artikel.class)
                .createAlias("tbmuser", "tbmuser")
                .createAlias("tbmuser.dosen", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            criteria.add(Restrictions.between("tahun", new Integer(currentMulai), new Integer(currentSampai)));
        } else {
            criteria.add(Restrictions.eq("tahun", tahun));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createPenelitianPengabdianCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(PengajuanPenelitianDanPengabdian.class)
                .createAlias("penelitianDanPengabdian", "penelitianDanPengabdian")
                .createAlias("tbmuser", "tbmuser")
                .createAlias("tbmuser.dosen", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            criteria.add(Restrictions.between("penelitianDanPengabdian.tahun", new Integer(currentMulai), new Integer(currentSampai)));
        } else {
            criteria.add(Restrictions.eq("penelitianDanPengabdian.tahun", tahun));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private Criteria createBukuAjarCriteria(Session session, Jurusan forcedJurusan, Integer tahun) {
        Criteria criteria = session.createCriteria(BukuBahanAjar.class)
                .createAlias("dosenPengarang1", "dosen")
                .add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)));
        if (tahun == null) {
            criteria.add(Restrictions.between("tahun", new Integer(currentMulai), new Integer(currentSampai)));
        } else {
            criteria.add(Restrictions.eq("tahun", tahun));
        }
        applyDosenAliasFilter(criteria, forcedJurusan);
        return criteria;
    }

    private void applyMahasiswaAliasFilter(Criteria criteria, Jurusan forcedJurusan) {
        if (criteria == null) {
            return;
        }
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("mahasiswa.program", currentProgram));
        }
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("mahasiswa.jurusan", jurusan));
        } else if (currentFakultas != null) {
            try {
                criteria.createAlias("mahasiswa.jurusan", "jurusanMahasiswaAlias");
                criteria.add(Restrictions.eq("jurusanMahasiswaAlias.fakultas", currentFakultas));
            } catch (Exception e) {
                debugError("applyMahasiswaAliasFilter", e);
            }
        }
    }


    private void applyDosenAliasFilter(Criteria criteria, Jurusan forcedJurusan) {
        if (criteria == null) {
            return;
        }
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("dosen.jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.add(Restrictions.eq("dosen.fakultas", currentFakultas));
        }
    }

    private List<String> academicYearList(int mulai, int sampai) {
        List<String> tas = new ArrayList<String>();
        for (int tahun = mulai; tahun <= sampai; tahun++) {
            tas.add(toTahunAkademik(tahun));
        }
        return tas;
    }

    private String toTahunAkademik(int tahun) {
        return tahun + "/" + (tahun + 1);
    }

    private Criteria createFakultasCriteria(Session session) {
        Criteria criteria = session.createCriteria(Fakultas.class).add(activeCriterion());
        if (currentFakultas != null) {
            criteria.add(Restrictions.eq("id", currentFakultas.getId()));
        }
        return criteria;
    }

    private Criteria createJurusanCriteria(Session session) {
        Criteria criteria = session.createCriteria(Jurusan.class).add(activeCriterion());
        if (currentJurusan != null) {
            criteria.add(Restrictions.eq("id", currentJurusan.getId()));
        }
        if (currentFakultas != null) {
            criteria.add(Restrictions.eq("fakultas", currentFakultas));
        }
        return criteria;
    }

    private Criteria createKurikulumCriteria(Session session, Jurusan forcedJurusan) {
        Criteria criteria = session.createCriteria(Kurikulum.class)
                .add(activeCriterion())
                .add(Restrictions.between("tahun", new Integer(currentMulai), new Integer(currentSampai)));
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.createAlias("jurusan", "jurusanAlias");
            criteria.add(Restrictions.eq("jurusanAlias.fakultas", currentFakultas));
        }
        return criteria;
    }

    private Criteria createMahasiswaCriteria(Session session, Jurusan forcedJurusan) {
        Criteria criteria = session.createCriteria(Mahasiswa.class)
                .add(activeCriterion())
                .add(Restrictions.between("tahunangkatan", new Integer(currentMulai), new Integer(currentSampai)));
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("program", currentProgram));
        }
        Jurusan jurusan = forcedJurusan == null ? currentJurusan : forcedJurusan;
        if (jurusan != null) {
            criteria.add(Restrictions.eq("jurusan", jurusan));
        } else if (currentFakultas != null) {
            criteria.createAlias("jurusan", "jurusanAlias");
            criteria.add(Restrictions.eq("jurusanAlias.fakultas", currentFakultas));
        }
        return criteria;
    }

    private Criteria createCalonCriteria(Session session, boolean onlyLulus, boolean daftarUlang, boolean dapatNim, boolean formTambahan, Integer tahun) {
        Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
                .add(activeCriterion())
                .add(tahun == null ? Restrictions.between("tahun", new Integer(currentMulai), new Integer(currentSampai)) : Restrictions.eq("tahun", tahun));
        if (currentProgram != null) {
            criteria.add(Restrictions.eq("program", currentProgram));
        }
        if (onlyLulus || daftarUlang || dapatNim || formTambahan) {
            criteria.add(Restrictions.isNotNull("prodiLulus"));
        }
        if (daftarUlang) {
            criteria.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN);
            criteria.add(Restrictions.gt("pembayaranDaftarUlang.dibayar", new Double(0.1d)));
        }
        if (dapatNim) {
            criteria.add(Restrictions.isNotNull("mahasiswa"));
        }
        if (formTambahan) {
            criteria.add(Restrictions.isNotNull("parameterTambahanInds"));
            criteria.add(Restrictions.ne("parameterTambahanInds", ""));
        }
        applyCalonSelectedProdiFilter(criteria, onlyLulus || daftarUlang || dapatNim || formTambahan);
        return criteria;
    }

    private void applyCalonSelectedProdiFilter(Criteria criteria, boolean onlyLulusField) {
        if (currentJurusan != null) {
            applyCalonJurusanFilter(criteria, currentJurusan, onlyLulusField);
        } else if (currentFakultas != null) {
            List<Jurusan> jurusans = loadJurusanByFakultas(currentFakultas);
            if (jurusans == null || jurusans.isEmpty()) {
                criteria.add(Restrictions.sqlRestriction("1=0"));
                return;
            }
            Disjunction disjunction = Restrictions.disjunction();
            for (int i = 0; i < jurusans.size(); i++) {
                Jurusan j = jurusans.get(i);
                if (onlyLulusField) {
                    disjunction.add(Restrictions.eq("prodiLulus", j));
                } else {
                    for (int x = 0; x < PRODI_PILIHAN_FIELDS.length; x++) {
                        disjunction.add(Restrictions.eq(PRODI_PILIHAN_FIELDS[x], j));
                    }
                }
            }
            criteria.add(disjunction);
        }
    }

    private void applyCalonJurusanFilter(Criteria criteria, Jurusan jurusan, boolean onlyLulusField) {
        if (jurusan == null) {
            return;
        }
        if (onlyLulusField) {
            criteria.add(Restrictions.eq("prodiLulus", jurusan));
        } else {
            Disjunction disjunction = Restrictions.disjunction();
            for (int i = 0; i < PRODI_PILIHAN_FIELDS.length; i++) {
                disjunction.add(Restrictions.eq(PRODI_PILIHAN_FIELDS[i], jurusan));
            }
            criteria.add(disjunction);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Jurusan> loadJurusanList(int limit) {
        Criteria criteria = createJurusanCriteria(HibernateUtil.currentSession());
        criteria.addOrder(Order.asc("fakultas")).addOrder(Order.asc("nama"));
        if (limit > 0) {
            criteria.setMaxResults(limit);
        }
        return ConstantValues.simpleList(criteria, Jurusan.class);
    }

    @SuppressWarnings("unchecked")
    private List<Jurusan> loadJurusanByFakultas(Fakultas fakultas) {
        if (fakultas == null) {
            return new ArrayList<Jurusan>();
        }
        return ConstantValues.simpleList(HibernateUtil.currentSession().createCriteria(Jurusan.class)
                .add(activeCriterion()).add(Restrictions.eq("fakultas", fakultas)).addOrder(Order.asc("nama")), Jurusan.class);
    }

    private Criterion activeCriterion() {
        return Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));
    }

    private long rowCount(Criteria criteria) {
        try {
            Object obj = criteria.setProjection(Projections.rowCount()).uniqueResult();
            return toLong(obj);
        } catch (Exception e) {
            debugError("rowCount", e);
            return 0L;
        }
    }


    @SuppressWarnings("unchecked")
    private TipePenelitianDanPengabdian resolveTipePenelitianDanPengabdian() {
        try {
            List<TipePenelitianDanPengabdian> list = HibernateUtil.currentSession()
                    .createCriteria(TipePenelitianDanPengabdian.class)
                    .setMaxResults(1).list();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (Exception e) {
            debugError("resolveTipePenelitianDanPengabdian", e);
        }
        return null;
    }

    private int parseTahunAkademikAwal(String tahunAkademik) {
        try {
            if (tahunAkademik == null || tahunAkademik.trim().length() == 0) {
                return ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
            }
            String[] parts = tahunAkademik.split("/");
            return Integer.parseInt(parts[0].trim());
        } catch (Exception e) {
            return ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
        }
    }

    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private int percent(long value, long total) {
        if (total <= 0L || value <= 0L) {
            return 0;
        }
        return (int) Math.round((value * 100.0d) / total);
    }

    private int clampScore(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    /**
     * @return skor komposit kesehatan akademik (0-100), rata-rata dari beberapa rasio kunci yang
     *         masing-masing dibatasi ke 0-100 lewat {@link #clampScore(int)}: tingkat aktivasi
     *         KRS, tingkat penyelesaian nilai (100 dikurangi rasio belum), rasio absensi terisi,
     *         rasio kurikulum per prodi, rasio daftar ulang, dan (bila ada lulusan) rasio
     *         wisuda terhadap lulusan
     */
    private int calculateAcademicHealthScore(DashboardPtData data) {
        int total = 0;
        int count = 0;

        total += clampScore(data.rasioKrs);
        count++;
        total += clampScore(100 - data.rasioNilaiBelum);
        count++;
        total += clampScore(data.rasioAbsensiTerisi);
        count++;
        total += clampScore(data.rasioKurikulumPerProdi);
        count++;
        total += clampScore(data.rasioDaftarUlang);
        count++;
        if (data.totalLulusanMahasiswa > 0L) {
            total += clampScore(data.rasioWisudaLulusan);
            count++;
        }

        return count <= 0 ? 0 : Math.round((total * 1.0f) / count);
    }


    private Fakultas getSelectedFakultas() {
        try {
            if (searchfakultas == null || searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
                return currentFakultas;
            }
            return (Fakultas) searchfakultas.getSelectedItem().getValue();
        } catch (Exception e) {
            return currentFakultas;
        }
    }

    private Jurusan getSelectedJurusan() {
        try {
            if (searchjurusan == null || searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
                return currentJurusan;
            }
            return (Jurusan) searchjurusan.getSelectedItem().getValue();
        } catch (Exception e) {
            return currentJurusan;
        }
    }

    private String getSelectedProgram() {
        try {
            if (searchprogram == null || searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null) {
                return currentProgram;
            }
            return (String) searchprogram.getSelectedItem().getValue();
        } catch (Exception e) {
            return currentProgram;
        }
    }

    private void selectComboByValue(Combobox combo, Object value) {
        if (combo == null || value == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            Comboitem item = combo.getItemAtIndex(i);
            if (item != null && item.getValue() != null && item.getValue().equals(value)) {
                combo.setSelectedItem(item);
                return;
            }
        }
        try {
            Common.selectComboItem(combo, value);
        } catch (Exception e) {
            debugError("selectComboByValue", e);
        }
    }

    private void sortAndLimit(List<DashboardMiniRow> rows, final int max) {
        Collections.sort(rows, new Comparator<DashboardMiniRow>() {
            public int compare(DashboardMiniRow a, DashboardMiniRow b) {
                if (a.value == b.value) {
                    return safeText(a.label).compareToIgnoreCase(safeText(b.label));
                }
                return a.value < b.value ? 1 : -1;
            }
        });
        while (rows.size() > max) {
            rows.remove(rows.size() - 1);
        }
    }

    private String sectionIntroHtml(String title, String description) {
        return "<div style='margin-top:16px; padding:14px 16px; border-radius:16px; background:#ffffff; "
                + "border:1px solid #e8eef6; box-shadow:0 8px 22px rgba(15,23,42,0.04);'>"
                + "<div style='font-size:16px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:#64748b; margin-top:5px; line-height:1.55;'>" + safeHtml(description) + "</div></div>";
    }

    private String dashboardExplainHtml(String description) {
        return "<div style='font-size:12px; color:#64748b; line-height:1.55; margin:-2px 0 12px 0; "
                + "padding:10px 12px; border-radius:12px; background:#f8fafc; border:1px dashed #cbd5e1;'>" + safeHtml(description) + "</div>";
    }

    private String dashboardPanelHtml(String content) {
        return "<div style='margin-top:12px; padding:15px; background:#ffffff; border:1px solid #e8eef6; border-radius:17px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,0.05); box-sizing:border-box;'>" + content + "</div>";
    }

    private String progressSectionHtml(String title, long total, DashboardMiniRow a, DashboardMiniRow b, DashboardMiniRow c) {
        return "<div><div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>" + safeHtml(title) + "</div>"
                + progressRowHtml(a.label, a.value, total, "#0891b2")
                + progressRowHtml(b.label, b.value, total, "#16a34a")
                + progressRowHtml(c.label, c.value, total, "var(--ais-theme-primary,#1d4ed8)") + "</div>";
    }

    private String progressRowHtml(String label, long value, long total, String color) {
        long pct = total <= 0L ? 0L : Math.round((value * 100.0d) / total);
        if (pct > 100L) {
            pct = 100L;
        }
        return "<div style='margin-bottom:12px;'>"
                + "<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; color:#475569; margin-bottom:5px;'>"
                + "<span style='font-weight:700;'>" + safeHtml(label) + "</span><span>" + formatNumber(value) + " (" + pct + "%)</span></div>"
                + "<div style='height:10px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
                + "<div style='height:10px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div></div>";
    }

    private String tableMiniRowsHtml(String title, List<DashboardMiniRow> rows, String col1, String col2) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>").append(safeHtml(title)).append("</div>");
        if (rows == null || rows.isEmpty()) {
            sb.append(emptyHtml("Belum ada data."));
            return sb.toString();
        }
        sb.append("<table style='width:100%; border-collapse:separate; border-spacing:0 7px;'>")
                .append("<thead><tr style='font-size:11px; color:#64748b; text-transform:uppercase; letter-spacing:.05em;'>")
                .append("<th style='text-align:left; padding:0 8px;'>").append(safeHtml(col1)).append("</th>")
                .append("<th style='text-align:right; padding:0 8px;'>").append(safeHtml(col2)).append("</th></tr></thead><tbody>");
        for (int i = 0; i < rows.size(); i++) {
            DashboardMiniRow row = rows.get(i);
            sb.append("<tr style='background:#f8fafc; border-radius:12px;'>")
                    .append("<td style='padding:10px 8px; color:#0f172a; font-weight:650; border-radius:10px 0 0 10px;'>")
                    .append(safeHtml(row.label)).append("</td>")
                    .append("<td style='padding:10px 8px; text-align:right; color:var(--ais-theme-primary,#1d4ed8); font-weight:800; border-radius:0 10px 10px 0;'>")
                    .append(formatNumber(row.value)).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String trendHtml(String title, List<DashboardMiniRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>").append(safeHtml(title)).append("</div>");
        if (rows == null || rows.isEmpty()) {
            sb.append(emptyHtml("Belum ada tren pada filter ini."));
            return sb.toString();
        }
        long max = 1L;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).value > max) {
                max = rows.get(i).value;
            }
        }
        sb.append("<div style='display:flex; align-items:flex-end; gap:8px; height:170px; padding:12px; background:#f8fafc; border-radius:14px; overflow:auto;'>");
        for (int i = 0; i < rows.size(); i++) {
            DashboardMiniRow row = rows.get(i);
            long h = 10L + Math.round((row.value * 130.0d) / max);
            sb.append("<div style='min-width:58px; text-align:center; display:flex; flex-direction:column; justify-content:flex-end; height:145px;'>")
                    .append("<div style='font-size:11px; font-weight:800; color:var(--ais-theme-primary,#1d4ed8); margin-bottom:4px;'>").append(formatNumber(row.value)).append("</div>")
                    .append("<div style='height:").append(h).append("px; border-radius:12px 12px 4px 4px; background:linear-gradient(180deg, var(--ais-theme-accent,#38bdf8), var(--ais-theme-primary,#2563eb));'></div>")
                    .append("<div style='font-size:10px; color:#64748b; margin-top:6px; white-space:nowrap;'>").append(safeHtml(row.label)).append("</div></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String miniStatHtml(String title, long value, String subtitle) {
        return "<div style='display:flex; justify-content:space-between; align-items:center; gap:12px; padding:10px 12px; "
                + "border-radius:13px; border:1px solid #e2e8f0; background:#ffffff; margin-bottom:8px;'>"
                + "<div><div style='font-size:12px; font-weight:800; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:11px; color:#64748b; margin-top:3px;'>" + safeHtml(subtitle) + "</div></div>"
                + "<div style='font-size:18px; font-weight:900; color:var(--ais-theme-primary,#1d4ed8);'>" + formatNumber(value) + "</div></div>";
    }

    private String miniStatHtml(String title, String value, String subtitle) {
        return "<div style='display:flex; justify-content:space-between; align-items:center; gap:12px; padding:10px 12px; "
                + "border-radius:13px; border:1px solid #e2e8f0; background:#ffffff; margin-bottom:8px;'>"
                + "<div><div style='font-size:12px; font-weight:800; color:#0f172a;'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:11px; color:#64748b; margin-top:3px;'>" + safeHtml(subtitle) + "</div></div>"
                + "<div style='font-size:18px; font-weight:900; color:var(--ais-theme-primary,#1d4ed8);'>" + safeHtml(value) + "</div></div>";
    }

    private String recommendationItemHtml(String title, String desc, String bg, String color) {
        return "<div style='border-radius:16px; padding:14px; background:" + bg + "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
                + "<div style='font-size:13px; font-weight:900; color:" + color + ";'>" + safeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:" + color + "; line-height:1.45; margin-top:10px;'>" + safeHtml(desc) + "</div></div>";
    }

    private String badgeHtml(String text, String bg, String color) {
        return "<span style='display:inline-block; padding:5px 9px; border-radius:999px; background:" + bg
                + "; color:" + color + "; font-size:11px; font-weight:800;'>" + safeHtml(text) + "</span>";
    }


    private String riskBadgeHtml(String title, int value, int target, String subtitle) {
        int score = clampScore(value);
        String bg = score >= target ? "#dcfce7" : (score >= Math.max(50, target - 20) ? "#fffbeb" : "#fee2e2");
        String color = score >= target ? "#166534" : (score >= Math.max(50, target - 20) ? "#92400e" : "#991b1b");
        String status = score >= target ? "Aman" : (score >= Math.max(50, target - 20) ? "Perlu Pantau" : "Prioritas");
        return "<div style='display:flex; justify-content:space-between; align-items:center; gap:12px; padding:10px 12px; "
                + "border-radius:13px; border:1px solid rgba(15,23,42,.08); background:" + bg + "; margin-bottom:8px;'>"
                + "<div><div style='font-size:12px; font-weight:900; color:" + color + ";'>" + safeHtml(title) + " - " + status + "</div>"
                + "<div style='font-size:11px; color:" + color + "; margin-top:3px;'>" + safeHtml(subtitle) + " | Target " + target + "%</div></div>"
                + "<div style='font-size:18px; font-weight:900; color:" + color + ";'>" + score + "%</div></div>";
    }

    private String emptyHtml(String text) {
        return "<div style='padding:14px; border-radius:13px; background:#f8fafc; color:#64748b; border:1px dashed #cbd5e1; font-size:12px;'>" + safeHtml(text) + "</div>";
    }

    private void appendHtml(Component parent, String html) {
        Html h = new Html(html);
        h.setParent(parent);
    }

    private String resolveDetailTitle(String detailKey) {
        if ("FAKULTAS".equals(detailKey)) return "Detail Fakultas Aktif";
        if ("PRODI".equals(detailKey)) return "Detail Program Studi Aktif";
        if ("KURIKULUM".equals(detailKey)) return "Detail Kurikulum Aktif";
        if ("PENDAFTAR".equals(detailKey)) return "Detail Pendaftar PMB/SPMB";
        if ("LULUS".equals(detailKey)) return "Detail Calon Mahasiswa Lulus Seleksi";
        if ("DAFTAR_ULANG".equals(detailKey)) return "Detail Calon Mahasiswa Daftar Ulang";
        if ("DAPAT_NIM".equals(detailKey)) return "Detail Calon Mahasiswa Sudah Dapat NIM";
        if ("MAHASISWA".equals(detailKey)) return "Detail Mahasiswa Aktif";
        if ("KRS_MHS".equals(detailKey)) return "Detail Mahasiswa KRS Berjalan";
        if ("DETAIL_KRS".equals(detailKey)) return "Detail KRS / Perkuliahan Berjalan";
        if ("IPK".equals(detailKey)) return "Detail IPK Mahasiswa";
        if ("SKS_KUM".equals(detailKey)) return "Detail SKS Kumulatif Mahasiswa";
        if ("NILAI".equals(detailKey)) return "Detail Nilai Mahasiswa";
        if ("NILAI_BELUM".equals(detailKey)) return "Detail Nilai Belum Terisi";
        if ("KEGIATAN".equals(detailKey)) return "Detail Kegiatan Kemahasiswaan";
        if ("ORGANISASI".equals(detailKey)) return "Detail Organisasi Intra Kampus";
        if ("PRESTASI".equals(detailKey)) return "Detail Prestasi Mahasiswa";
        if ("DOSEN".equals(detailKey)) return "Detail Dosen Aktif";
        if ("BEBAN_SKS_DOSEN".equals(detailKey)) return "Detail Beban SKS Dosen";
        if ("KEGIATAN_DOSEN".equals(detailKey)) return "Detail Kegiatan Dosen";
        if ("ORGANISASI_DOSEN".equals(detailKey)) return "Detail Organisasi Dosen";
        if ("PRESTASI_DOSEN".equals(detailKey)) return "Detail Prestasi Dosen";
        if ("KARYA_DOSEN".equals(detailKey)) return "Detail Karya Dosen";
        if ("PUBLIKASI_ILMIAH".equals(detailKey)) return "Detail Publikasi Ilmiah";
        if ("PENELITIAN_PENGABDIAN".equals(detailKey)) return "Detail Penelitian/Pengabdian";
        if ("BUKU_AJAR".equals(detailKey)) return "Detail Buku Ajar";
        if ("PERKULIAHAN".equals(detailKey)) return "Detail Penawaran Perkuliahan";
        if ("PERKULIAHAN_GANJIL".equals(detailKey)) return "Detail Perkuliahan Semester Ganjil";
        if ("PERKULIAHAN_GENAP".equals(detailKey)) return "Detail Perkuliahan Semester Genap";
        if ("PERKULIAHAN_SP".equals(detailKey)) return "Detail Perkuliahan Semester Pendek";
        if ("PERKULIAHAN_BERJALAN".equals(detailKey)) return "Detail Perkuliahan Berjalan";
        if ("DETAIL_PERKULIAHAN_DISETUJUI".equals(detailKey)) return "Detail Peserta Perkuliahan Disetujui";
        if ("PERTEMUAN_AKTIF".equals(detailKey)) return "Detail Pertemuan Aktif";
        if ("ABSENSI_TERISI".equals(detailKey)) return "Detail Absensi Terisi";
        if ("PENCAPAIAN_PERKULIAHAN".equals(detailKey)) return "Detail Pencapaian Perkuliahan";
        if ("BIMBINGAN_MHS".equals(detailKey)) return "Detail Bimbingan Mahasiswa";
        if ("SIDANG_MHS".equals(detailKey)) return "Detail Sidang Mahasiswa";
        if ("WISUDA_MHS".equals(detailKey)) return "Detail Wisuda Mahasiswa";
        if ("LULUSAN_MHS".equals(detailKey)) return "Detail Lulusan Mahasiswa";
        if ("STATUS_KELUAR_MHS".equals(detailKey)) return "Detail Status Keluar Mahasiswa";
        if ("PREDIKAT_KELULUSAN_MHS".equals(detailKey)) return "Detail Predikat Kelulusan Mahasiswa";
        if ("STATUS_SETELAH_LULUS_MHS".equals(detailKey)) return "Detail Status Setelah Lulus";
        if ("PEKERJAAN_SETELAH_LULUS_MHS".equals(detailKey)) return "Detail Pekerjaan Setelah Lulus";
        if ("DOMISILI_SETELAH_LULUS_MHS".equals(detailKey)) return "Detail Domisili Setelah Lulus";
        if ("MASA_STUDI_MHS".equals(detailKey)) return "Detail Masa Studi Mahasiswa";
        if ("SEMESTER_LULUS_MHS".equals(detailKey)) return "Detail Semester Lulus Mahasiswa";
        return "Detail Data";
    }

    private String resolveCalonNama(Object obj) {
        String val = readValue(obj, "getNama");
        if (val.length() > 0 && !"-".equals(val)) return val;
        val = readValue(obj, "getNamaLengkap");
        if (val.length() > 0 && !"-".equals(val)) return val;
        val = readValue(obj, "getNamaCalonMahasiswa");
        if (val.length() > 0 && !"-".equals(val)) return val;
        return readValue(obj, "getNomorPendaftaran");
    }

    private String resolveCalonProdiInfo(Object obj) {
        String lulus = readNestedValue(obj, new String[] { "getProdiLulus", "getNama" });
        if (lulus != null && lulus.trim().length() > 0 && !"-".equals(lulus)) {
            return "Lulus: " + lulus;
        }
        String p1 = readNestedValue(obj, new String[] { "getProdi1", "getNama" });
        String p2 = readNestedValue(obj, new String[] { "getProdi2", "getNama" });
        return "Pil.1: " + p1 + " / Pil.2: " + p2;
    }

    private String resolveCalonStatus(Object obj) {
        String nim = readNestedValue(obj, new String[] { "getMahasiswa", "getNim" });
        if (nim != null && nim.trim().length() > 0 && !"-".equals(nim)) {
            return "Sudah NIM: " + nim;
        }
        String bayar = readNestedValue(obj, new String[] { "getPembayaranDaftarUlang", "getDibayar" });
        if (bayar != null && bayar.trim().length() > 0 && !"-".equals(bayar)) {
            return "Daftar ulang: " + bayar;
        }
        return readNestedValue(obj, new String[] { "getStatusAwalMahasiswa", "getNama" });
    }


    private String resolveLulusanInfo(Object obj, String detailKey) {
        if ("STATUS_KELUAR_MHS".equals(detailKey)) {
            return readNestedValue(obj, new String[] { "getStatusKeluar", "getNama" });
        }
        if ("PREDIKAT_KELULUSAN_MHS".equals(detailKey)) {
            return readNestedValue(obj, new String[] { "getPredikatKelulusan", "getNama" });
        }
        if ("STATUS_SETELAH_LULUS_MHS".equals(detailKey)) {
            return readNestedValue(obj, new String[] { "getStatusSetelahLulus", "getNama" });
        }
        if ("PEKERJAAN_SETELAH_LULUS_MHS".equals(detailKey)) {
            return readNestedValue(obj, new String[] { "getStatusPekerjaanSetelahLulus", "getNama" });
        }
        if ("DOMISILI_SETELAH_LULUS_MHS".equals(detailKey)) {
            return readNestedValue(obj, new String[] { "getStatusDomisiliSetelahLulus", "getNama" });
        }
        if ("SEMESTER_LULUS_MHS".equals(detailKey)) {
            return "Semester: " + readValue(obj, "getSemesterLulus");
        }
        if ("MASA_STUDI_MHS".equals(detailKey)) {
            try {
                String thnLulus = readValue(obj, "getTahunLulus");
                String thnAngkatan = readValue(obj, "getTahunangkatan");
                int lulus = Integer.parseInt(thnLulus);
                int angkatan = Integer.parseInt(thnAngkatan);
                return "Masa studi: " + (lulus - angkatan) + " tahun (angkatan " + thnAngkatan + ")";
            } catch (Exception e) {
                return "Angkatan: " + readValue(obj, "getTahunangkatan");
            }
        }
        String status = readNestedValue(obj, new String[] { "getStatusKeluar", "getNama" });
        String predikat = readNestedValue(obj, new String[] { "getPredikatKelulusan", "getNama" });
        return "Status: " + status + " / Predikat: " + predikat;
    }

    private String resolveDosenNama(Object obj) {
        String nama = readNestedValue(obj, new String[] { "getDosen", "getNama" });
        if (nama != null && nama.trim().length() > 0 && !"-".equals(nama)) {
            return nama;
        }
        nama = readNestedValue(obj, new String[] { "getTbmuser", "getDosen", "getNama" });
        if (nama != null && nama.trim().length() > 0 && !"-".equals(nama)) {
            return nama;
        }
        nama = readNestedValue(obj, new String[] { "getDosenPengarang1", "getNama" });
        return nama;
    }

    private String resolveDosenProdi(Object obj) {
        String prodi = readNestedValue(obj, new String[] { "getDosen", "getJurusan", "getNama" });
        if (prodi != null && prodi.trim().length() > 0 && !"-".equals(prodi)) {
            return prodi;
        }
        prodi = readNestedValue(obj, new String[] { "getTbmuser", "getDosen", "getJurusan", "getNama" });
        if (prodi != null && prodi.trim().length() > 0 && !"-".equals(prodi)) {
            return prodi;
        }
        prodi = readNestedValue(obj, new String[] { "getDosenPengarang1", "getJurusan", "getNama" });
        return prodi;
    }

    private String safeJurusanLabel(Jurusan jurusan) {
        if (jurusan == null) {
            return "-";
        }
        String fakultas = jurusan.getFakultas() == null ? "" : safeText(jurusan.getFakultas().getNama()) + " - ";
        return fakultas + safeText(jurusan.getNama());
    }

    private String readValue(Object obj, String getter) {
        if (obj == null || getter == null) {
            return "-";
        }
        try {
            Method method = obj.getClass().getMethod(getter, new Class[0]);
            Object val = method.invoke(obj, new Object[0]);
            if (val == null) {
                return "-";
            }
            return safeText(val.toString());
        } catch (Exception e) {
            return "-";
        }
    }

    private String readNestedValue(Object obj, String[] getters) {
        if (obj == null || getters == null) {
            return "-";
        }
        Object cur = obj;
        for (int i = 0; i < getters.length; i++) {
            if (cur == null) {
                return "-";
            }
            try {
                Method method = cur.getClass().getMethod(getters[i], new Class[0]);
                cur = method.invoke(cur, new Object[0]);
            } catch (Exception e) {
                return "-";
            }
        }
        return cur == null ? "-" : safeText(cur.toString());
    }

    private String formatNumber(long number) {
        try {
            return Common.numberFormat.get().format(number);
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }

    private String formatDecimal(double number) {
        try {
            return Common.numberFormat.get().format(number);
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String safeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void debugError(String context, Exception e) {
        if (debug) {
            System.err.println("[DasborPerguruanTinggiTerpadu DEBUG] " + context);
            if (e != null) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasborPerguruanTinggiTerpadu.java:3154");
            }
        }
    }

    /**
     * Struct data mentah (bukan JavaBean, bidang publik-paket diakses langsung) berisi seluruh
     * hasil hitungan/agregasi satu kali muat dasbor: filter yang dipakai (rentang tahun, tahun
     * akademik/semester berjalan), serta puluhan bidang {@code total*}/{@code avg*}/{@code min*}/
     * {@code max*}/{@code rasio*}/{@code top*}/{@code trend*} yang dipetakan langsung ke kartu dan
     * panel pada method {@code render*} (lihat javadoc kelas induk untuk bagaimana nilai-nilai ini
     * dihitung lewat {@link #loadDashboardData()}). Instans ini disimpan di cache statis
     * {@link #_CACHE} selama {@link #_TTL_MS}.
     */
    private static class DashboardPtData {
        int tahunMulai;
        int tahunSampai;
        String tahunAkademikBerjalan;
        String semesterBerjalan;
        long totalFakultas;
        long totalProdi;
        long totalKurikulum;
        long totalPendaftar;
        long totalLulusSeleksi;
        long totalDaftarUlang;
        long totalDapatNim;
        long totalIsiFormTambahan;
        long totalMahasiswa;
        long totalMahasiswaKrs;
        long totalMahasiswaBelumKrsEstimasi;
        long totalKrsRecord;
        long totalNilaiDisetujui;
        long totalNilaiSudah;
        long totalNilaiBelum;
        long totalIpk;
        long totalSksKumulatif;
        long totalKegiatanKemahasiswaan;
        long totalOrganisasiIntraKampus;
        long totalPrestasiMahasiswa;
        long totalDosenAktif;
        long totalBebanSksDosen;
        long totalKegiatanDosen;
        long totalOrganisasiDosen;
        long totalPrestasiDosen;
        long totalKaryaDosen;
        long totalPublikasiIlmiah;
        long totalPenelitianPengabdian;
        long totalBukuAjar;
        long totalPenawaranPerkuliahan;
        long totalPerkuliahanGanjil;
        long totalPerkuliahanGenap;
        long totalPerkuliahanSp;
        long totalPerkuliahanBerjalan;
        long totalDetailDisetujuiBerjalan;
        long totalPertemuanAktif;
        long totalPertemuanBerlalu;
        long totalAbsensiTerisi;
        long totalBimbinganMahasiswa;
        long totalSidangMahasiswa;
        long totalWisudaMahasiswa;
        long totalLulusanMahasiswa;
        long totalStatusKeluarMahasiswa;
        long totalPredikatKelulusanMahasiswa;
        long totalStatusSetelahLulusMahasiswa;
        long totalPekerjaanSetelahLulusMahasiswa;
        long totalDomisiliSetelahLulusMahasiswa;
        long totalMasaStudiTerdeteksi;
        long totalSemesterLulusTerdeteksi;
        long rasioMahasiswaPerDosen;
        int rasioAbsensiTerisi;
        int rasioNilaiBelum;
        int rasioKurikulumPerProdi;
        int rasioWisudaLulusan;
        int rasioLulusanMahasiswa;
        int rasioMahasiswaDariPendaftar;
        int skorKesehatanAkademik;
        double avgIpk;
        double minIpk;
        double maxIpk;
        double avgSksKumulatif;
        double minSksKumulatif;
        double maxSksKumulatif;
        int rasioLulus;
        int rasioDaftarUlang;
        int rasioKonversiNim;
        int rasioKrs;
        List<DashboardMiniRow> topMahasiswaProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPendaftarProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topLulusProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topKurikulumProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topKegiatanProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topOrganisasiProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPrestasiProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topDosenProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topBebanSksDosenProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topKegiatanDosenProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topOrganisasiDosenProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPrestasiDosenProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topKaryaDosenProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPublikasiIlmiahProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPenelitianPengabdianProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topBukuAjarProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPerkuliahanProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPertemuanProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topAbsensiProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPencapaianProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topBimbinganProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topSidangProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topWisudaProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topLulusanProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topStatusKeluarProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topPredikatKelulusanProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> topMasaStudiProdi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPendaftar = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendMahasiswa = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendKurikulum = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendKegiatan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendOrganisasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPrestasi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendBebanSksDosen = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendKegiatanDosen = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendOrganisasiDosen = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPrestasiDosen = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendKaryaDosen = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPublikasiIlmiah = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPenelitianPengabdian = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendBukuAjar = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPerkuliahan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPertemuan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendAbsensi = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendPencapaian = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendBimbingan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendSidang = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendWisuda = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendLulusan = new ArrayList<DashboardMiniRow>();
        List<DashboardMiniRow> trendMasaStudi = new ArrayList<DashboardMiniRow>();
    }

    /** Pasangan label-nilai sederhana untuk satu baris pada tabel top-N atau tren, dipakai oleh {@link #tableMiniRowsHtml}, {@link #trendHtml}, dan {@link #sortAndLimit}. */
    private static class DashboardMiniRow {
        String label;
        long value;

        /** @param label   nama baris (mis. nama prodi/tahun) @param value nilai numerik baris */
        DashboardMiniRow(String label, long value) {
            this.label = label;
            this.value = value;
        }
    }
}
