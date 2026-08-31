package ais.action.master.antarjemput;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.antarjemput.DetailPenjemputanAntarJemput;
import ais.database.model.antarjemput.JadwalAntarJemput;
import ais.database.model.antarjemput.KartuPenjemputAntarJemput;
import ais.database.model.antarjemput.KendaraanAntarJemput;
import ais.database.model.antarjemput.LogNotifikasiAntarJemput;
import ais.database.model.antarjemput.PesertaJadwalAntarJemput;
import ais.database.model.antarjemput.RuteAntarJemput;
import ais.database.model.antarjemput.TransaksiPenjemputanAntarJemput;
import ais.database.model.asset.Asset;
import ais.database.model.asset.AssetDetail;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk antar jemput. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String KIND_KENDARAAN}, {@code String
 * KIND_RUTE}, {@code String KIND_JADWAL}, {@code String KIND_PESERTA}, {@code String KIND_KARTU}, {@code String
 * KIND_TRANSAKSI}, {@code String KIND_DETAIL}, {@code String KIND_LOG}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initPagings()}); pembacaan/pencarian ({@code findById()},
 * {@code onRefresh()}, {@code onSearchKendaraan()}, {@code onSearchRute()}, {@code onSearchJadwal()}, {@code
 * onSearchPeserta()}); mutasi data ({@code saveForm()}, {@code updateTransaksiStatus()}, {@code
 * updateDetailStatus()}, {@code setScanStatus()}); penghapusan/pembatalan ({@code deleteData()});
 * pelaporan/ekspor ({@code renderKendaraan()}, {@code renderRute()}, {@code renderJadwal()}, {@code
 * renderPeserta()}, {@code renderKartu()}, {@code renderTransaksi()}); operasi domain lain ({@code
 * wireIncludedComponents()}, {@code or()}, {@code bindPaging()}, {@code onAddKendaraan()}, {@code onAddRute()},
 * {@code onAddJadwal()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
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
public class AntarJemputAction extends GenericAutowireComposer {

    private static final long serialVersionUID = -2837473261882019021L;

    // ── Kind constants ─────────────────────────────────────────────────────────
    private static final String KIND_KENDARAAN = "kendaraan";
    private static final String KIND_RUTE      = "rute";
    private static final String KIND_JADWAL    = "jadwal";
    private static final String KIND_PESERTA   = "peserta";
    private static final String KIND_KARTU     = "kartu";
    private static final String KIND_TRANSAKSI = "transaksi";
    private static final String KIND_DETAIL    = "detail";
    private static final String KIND_LOG       = "log";

    // ── Status color palette ──────────────────────────────────────────────────
    private static final String COLOR_BLUE   = "#2563eb";
    private static final String COLOR_PURPLE = "#7c3aed";
    private static final String COLOR_GREEN  = "#15803d";
    private static final String COLOR_RED    = "#b91c1c";
    private static final String COLOR_TEAL   = "#0f766e";
    private static final String COLOR_ORANGE = "#c2410c";
    private static final String COLOR_AMBER  = "#b45309";
    private static final String COLOR_GRAY   = "#64748b";

    // ── Day name abbreviations (Mon=0) ────────────────────────────────────────
    private static final String[] HARI_SINGKAT = {"Min","Sen","Sel","Rab","Kam","Jum","Sab"};

    // ── Component references ───────────────────────────────────────────────────
    private Div     dashboardPanel;
    private Div     monitorPanel;
    private MyGrid  gridKendaraan;
    private MyGrid  gridRute;
    private MyGrid  gridJadwal;
    private MyGrid  gridPeserta;
    private MyGrid  gridKartu;
    private MyGrid  gridTransaksi;
    private MyGrid  gridDetail;
    private MyGrid  gridLog;
    private MyGrid  gridAntrianGerbang;
    private Paging  pagingKendaraan;
    private Paging  pagingRute;
    private Paging  pagingJadwal;
    private Paging  pagingPeserta;
    private Paging  pagingKartu;
    private Paging  pagingTransaksi;
    private Paging  pagingDetail;
    private Paging  pagingLog;
    private Textbox searchKendaraan;
    private Textbox searchRute;
    private Textbox searchJadwal;
    private Textbox searchPeserta;
    private Textbox searchKartu;
    private Textbox searchTransaksi;
    private Textbox searchDetail;
    private Textbox searchLog;
    private Checkbox aktifKendaraan;
    private Checkbox aktifRute;
    private Checkbox aktifJadwal;
    private Checkbox aktifPeserta;
    private Checkbox aktifKartu;

    // ── Gate verification ──────────────────────────────────────────────────────
    private Textbox  scanNomor;
    private Textbox  scanPintu;
    private Longbox  scanJadwalId;
    private Label    scanStatus;

    // ── Form dialog ───────────────────────────────────────────────────────────
    private MyWindow formWindow;
    private String   currentKind;
    private GeneralValueObject        currentObject;
    private Map<String, Component>    inputs = new HashMap<String, Component>();

    // ── Privileges ────────────────────────────────────────────────────────────
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;

    // ===========================================================================
    // LIFECYCLE
    // ===========================================================================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
            org.zkoss.zk.ui.Page page, Component parent,
            org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        wireIncludedComponents(comp);
        canCreate = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
        canUpdate = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        canDelete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
        initPagings();
        refreshAll();
    }

    // ===========================================================================
    // WIRING
    // ===========================================================================

    private void wireIncludedComponents(Component root) {
        dashboardPanel     = (Div)     or(dashboardPanel,     findById(root, "dashboardPanel"));
        monitorPanel       = (Div)     or(monitorPanel,       findById(root, "monitorPanel"));
        gridKendaraan      = (MyGrid)  or(gridKendaraan,      findById(root, "gridKendaraan"));
        gridRute           = (MyGrid)  or(gridRute,           findById(root, "gridRute"));
        gridJadwal         = (MyGrid)  or(gridJadwal,         findById(root, "gridJadwal"));
        gridPeserta        = (MyGrid)  or(gridPeserta,        findById(root, "gridPeserta"));
        gridKartu          = (MyGrid)  or(gridKartu,          findById(root, "gridKartu"));
        gridTransaksi      = (MyGrid)  or(gridTransaksi,      findById(root, "gridTransaksi"));
        gridDetail         = (MyGrid)  or(gridDetail,         findById(root, "gridDetail"));
        gridLog            = (MyGrid)  or(gridLog,            findById(root, "gridLog"));
        gridAntrianGerbang = (MyGrid)  or(gridAntrianGerbang, findById(root, "gridAntrianGerbang"));
        pagingKendaraan    = (Paging)  or(pagingKendaraan,    findById(root, "pagingKendaraan"));
        pagingRute         = (Paging)  or(pagingRute,         findById(root, "pagingRute"));
        pagingJadwal       = (Paging)  or(pagingJadwal,       findById(root, "pagingJadwal"));
        pagingPeserta      = (Paging)  or(pagingPeserta,      findById(root, "pagingPeserta"));
        pagingKartu        = (Paging)  or(pagingKartu,        findById(root, "pagingKartu"));
        pagingTransaksi    = (Paging)  or(pagingTransaksi,    findById(root, "pagingTransaksi"));
        pagingDetail       = (Paging)  or(pagingDetail,       findById(root, "pagingDetail"));
        pagingLog          = (Paging)  or(pagingLog,          findById(root, "pagingLog"));
        searchKendaraan    = (Textbox) or(searchKendaraan,    findById(root, "searchKendaraan"));
        searchRute         = (Textbox) or(searchRute,         findById(root, "searchRute"));
        searchJadwal       = (Textbox) or(searchJadwal,       findById(root, "searchJadwal"));
        searchPeserta      = (Textbox) or(searchPeserta,      findById(root, "searchPeserta"));
        searchKartu        = (Textbox) or(searchKartu,        findById(root, "searchKartu"));
        searchTransaksi    = (Textbox) or(searchTransaksi,    findById(root, "searchTransaksi"));
        searchDetail       = (Textbox) or(searchDetail,       findById(root, "searchDetail"));
        searchLog          = (Textbox) or(searchLog,          findById(root, "searchLog"));
        aktifKendaraan     = (Checkbox) or(aktifKendaraan,    findById(root, "aktifKendaraan"));
        aktifRute          = (Checkbox) or(aktifRute,         findById(root, "aktifRute"));
        aktifJadwal        = (Checkbox) or(aktifJadwal,       findById(root, "aktifJadwal"));
        aktifPeserta       = (Checkbox) or(aktifPeserta,      findById(root, "aktifPeserta"));
        aktifKartu         = (Checkbox) or(aktifKartu,        findById(root, "aktifKartu"));
    }

    private Component or(Component current, Component fallback) {
        return current != null ? current : fallback;
    }

    private Component findById(Component parent, String id) {
        if (parent == null || id == null) return null;
        if (id.equals(parent.getId())) return parent;
        for (Object child : parent.getChildren()) {
            Component found = findById((Component) child, id);
            if (found != null) return found;
        }
        return null;
    }

    // ===========================================================================
    // PAGING
    // ===========================================================================

    private void initPagings() {
        bindPaging(pagingKendaraan, KIND_KENDARAAN);
        bindPaging(pagingRute,      KIND_RUTE);
        bindPaging(pagingJadwal,    KIND_JADWAL);
        bindPaging(pagingPeserta,   KIND_PESERTA);
        bindPaging(pagingKartu,     KIND_KARTU);
        bindPaging(pagingTransaksi, KIND_TRANSAKSI);
        bindPaging(pagingDetail,    KIND_DETAIL);
        bindPaging(pagingLog,       KIND_LOG);
    }

    private void bindPaging(final Paging paging, final String kind) {
        if (paging == null) return;
        Common.initPaging(paging, new EventListener() {
            public void onEvent(Event event) throws Exception { search(kind); }
        });
    }

    // ===========================================================================
    // EVENT HANDLERS
    // ===========================================================================

    public void onRefresh(Event event)           { refreshAll(); }
    public void onSearchKendaraan(Event event)   { search(KIND_KENDARAAN); }
    public void onSearchRute(Event event)        { search(KIND_RUTE); }
    public void onSearchJadwal(Event event)      { search(KIND_JADWAL); }
    public void onSearchPeserta(Event event)     { search(KIND_PESERTA); }
    public void onSearchKartu(Event event)       { search(KIND_KARTU); }
    public void onSearchTransaksi(Event event)   { search(KIND_TRANSAKSI); }
    public void onSearchDetail(Event event)      { search(KIND_DETAIL); }
    public void onSearchLog(Event event)         { search(KIND_LOG); }

    public void onAddKendaraan(Event event) { openForm(KIND_KENDARAAN, new KendaraanAntarJemput()); }
    public void onAddRute(Event event)      { openForm(KIND_RUTE,      new RuteAntarJemput()); }
    public void onAddJadwal(Event event)    { openForm(KIND_JADWAL,    new JadwalAntarJemput()); }
    public void onAddPeserta(Event event)   { openForm(KIND_PESERTA,   new PesertaJadwalAntarJemput()); }
    public void onAddKartu(Event event)     { openForm(KIND_KARTU,     new KartuPenjemputAntarJemput()); }

    public void onVerifikasiGerbang(Event event) { verifikasiGerbang(); }

    public void onReloadDashboard(Event event) {
        renderDashboard();
        renderMonitor();
        search(KIND_TRANSAKSI);
        search(KIND_DETAIL);
    }

    // ===========================================================================
    // REFRESH
    // ===========================================================================

    private void refreshAll() {
        renderDashboard();
        renderMonitor();
        renderAntrianGerbang();
        search(KIND_KENDARAAN);
        search(KIND_RUTE);
        search(KIND_JADWAL);
        search(KIND_PESERTA);
        search(KIND_KARTU);
        search(KIND_TRANSAKSI);
        search(KIND_DETAIL);
        search(KIND_LOG);
    }

    // ===========================================================================
    // SEARCH
    // ===========================================================================

    private void search(String kind) {
        try {
            MyGrid grid = gridFor(kind);
            if (grid == null) return;
            Paging paging = pagingFor(kind);
            if (paging != null) Common.initPaging(criteriaFor(kind, false), paging);
            Criteria data = criteriaFor(kind, true);
            if (paging != null) {
                data.setFirstResult(Common.ROWS_COUNT_ON_PAGE * paging.getActivePage());
                data.setMaxResults(Common.ROWS_COUNT_ON_PAGE);
            }
            grid.setRowRenderer(new AntarJemputRenderer(kind));
            grid.setModelCheckMobile(new SimpleListModel(data.list()));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private Criteria criteriaFor(String kind, boolean withOrder) {
        Criteria c = HibernateUtil.currentSession().createCriteria(entityClassFor(kind));
        String keyword = keyword(kind);
        Checkbox aktif = aktifBoxFor(kind);
        if (aktif != null && aktif.isChecked()) {
            c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        }
        if (keyword.length() > 0) {
            if (KIND_TRANSAKSI.equals(kind)) {
                c.add(Restrictions.or(
                    Restrictions.ilike("nomorAntrian", keyword, MatchMode.ANYWHERE),
                    Restrictions.ilike("nomorScan",    keyword, MatchMode.ANYWHERE)));
            } else if (KIND_DETAIL.equals(kind)) {
                c.add(Restrictions.or(
                    Restrictions.ilike("nama",            keyword, MatchMode.ANYWHERE),
                    Restrictions.ilike("statusPanggilan", keyword, MatchMode.ANYWHERE)));
            } else if (KIND_LOG.equals(kind)) {
                c.add(Restrictions.or(
                    Restrictions.ilike("perangkatTujuan", keyword, MatchMode.ANYWHERE),
                    Restrictions.ilike("pesan",           keyword, MatchMode.ANYWHERE)));
            } else {
                c.add(Restrictions.or(
                    Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE),
                    Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE)));
            }
        }
        if (withOrder) {
            if (KIND_TRANSAKSI.equals(kind)) c.addOrder(Order.desc("waktuScan"));
            else if (KIND_DETAIL.equals(kind) || KIND_LOG.equals(kind)) c.addOrder(Order.desc("id"));
            else c.addOrder(Order.asc("nama"));
        }
        return c;
    }

    private String keyword(String kind) {
        Textbox tb = searchBoxFor(kind);
        if (tb == null || tb.getValue() == null) return "";
        return tb.getValue().trim();
    }

    // ── Kind lookup helpers ────────────────────────────────────────────────────

    private Class entityClassFor(String kind) {
        if (KIND_KENDARAAN.equals(kind)) return KendaraanAntarJemput.class;
        if (KIND_RUTE.equals(kind))      return RuteAntarJemput.class;
        if (KIND_JADWAL.equals(kind))    return JadwalAntarJemput.class;
        if (KIND_PESERTA.equals(kind))   return PesertaJadwalAntarJemput.class;
        if (KIND_KARTU.equals(kind))     return KartuPenjemputAntarJemput.class;
        if (KIND_TRANSAKSI.equals(kind)) return TransaksiPenjemputanAntarJemput.class;
        if (KIND_DETAIL.equals(kind))    return DetailPenjemputanAntarJemput.class;
        return LogNotifikasiAntarJemput.class;
    }

    private MyGrid gridFor(String kind) {
        if (KIND_KENDARAAN.equals(kind)) return gridKendaraan;
        if (KIND_RUTE.equals(kind))      return gridRute;
        if (KIND_JADWAL.equals(kind))    return gridJadwal;
        if (KIND_PESERTA.equals(kind))   return gridPeserta;
        if (KIND_KARTU.equals(kind))     return gridKartu;
        if (KIND_TRANSAKSI.equals(kind)) return gridTransaksi;
        if (KIND_DETAIL.equals(kind))    return gridDetail;
        return gridLog;
    }

    private Paging pagingFor(String kind) {
        if (KIND_KENDARAAN.equals(kind)) return pagingKendaraan;
        if (KIND_RUTE.equals(kind))      return pagingRute;
        if (KIND_JADWAL.equals(kind))    return pagingJadwal;
        if (KIND_PESERTA.equals(kind))   return pagingPeserta;
        if (KIND_KARTU.equals(kind))     return pagingKartu;
        if (KIND_TRANSAKSI.equals(kind)) return pagingTransaksi;
        if (KIND_DETAIL.equals(kind))    return pagingDetail;
        return pagingLog;
    }

    private Textbox searchBoxFor(String kind) {
        if (KIND_KENDARAAN.equals(kind)) return searchKendaraan;
        if (KIND_RUTE.equals(kind))      return searchRute;
        if (KIND_JADWAL.equals(kind))    return searchJadwal;
        if (KIND_PESERTA.equals(kind))   return searchPeserta;
        if (KIND_KARTU.equals(kind))     return searchKartu;
        if (KIND_TRANSAKSI.equals(kind)) return searchTransaksi;
        if (KIND_DETAIL.equals(kind))    return searchDetail;
        return searchLog;
    }

    private Checkbox aktifBoxFor(String kind) {
        if (KIND_KENDARAAN.equals(kind)) return aktifKendaraan;
        if (KIND_RUTE.equals(kind))      return aktifRute;
        if (KIND_JADWAL.equals(kind))    return aktifJadwal;
        if (KIND_PESERTA.equals(kind))   return aktifPeserta;
        if (KIND_KARTU.equals(kind))     return aktifKartu;
        return null;
    }

    private String titleFor(String kind) {
        if (KIND_KENDARAAN.equals(kind)) return "Kendaraan Antar Jemput";
        if (KIND_RUTE.equals(kind))      return "Rute Antar Jemput";
        if (KIND_JADWAL.equals(kind))    return "Jadwal Antar Jemput";
        if (KIND_PESERTA.equals(kind))   return "Peserta Jadwal";
        if (KIND_KARTU.equals(kind))     return "Kartu Penjemput";
        return "Antar Jemput";
    }

    // ===========================================================================
    // ROW RENDERER
    // ===========================================================================

    /**
     * Renderer lokal untuk layar/komponen {@link AntarJemputAction}. Kelas ini menerjemahkan satu item data
     * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link AntarJemputAction} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String kind}; operasi lokal: {@code
     * render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see AntarJemputAction
     */
    class AntarJemputRenderer extends MyRowRenderer {
        private final String kind;
        AntarJemputRenderer(String kind) { this.kind = kind; }

        public void render(Row row, Object data) throws Exception {
            row.setValign("top");
            if      (KIND_KENDARAAN.equals(kind)) renderKendaraan(row, (KendaraanAntarJemput) data);
            else if (KIND_RUTE.equals(kind))      renderRute(row,      (RuteAntarJemput) data);
            else if (KIND_JADWAL.equals(kind))    renderJadwal(row,    (JadwalAntarJemput) data);
            else if (KIND_PESERTA.equals(kind))   renderPeserta(row,   (PesertaJadwalAntarJemput) data);
            else if (KIND_KARTU.equals(kind))     renderKartu(row,     (KartuPenjemputAntarJemput) data);
            else if (KIND_TRANSAKSI.equals(kind)) renderTransaksi(row, (TransaksiPenjemputanAntarJemput) data);
            else if (KIND_DETAIL.equals(kind))    renderDetail(row,    (DetailPenjemputanAntarJemput) data);
            else                                  renderLog(row,       (LogNotifikasiAntarJemput) data);
        }
    }

    private void renderKendaraan(Row row, KendaraanAntarJemput data) {
        label(row, data.getKode());
        label(row, data.getNama());
        label(row, data.getNomorPolisi());
        label(row, data.getKapasitasDuduk() + "");
        label(row, nama(data.getSopir()));
        label(row, buildKruNames(data));
        activeCell(row, data);
        actionCell(row, KIND_KENDARAAN, data);
    }

    private void renderRute(Row row, RuteAntarJemput data) {
        label(row, data.getKode());
        label(row, data.getNama());
        badge(row, data.getJenisLayanan(), "#e0f2fe", "#075985");
        label(row, data.getTitikAwal());
        label(row, data.getTitikAkhir());
        label(row, data.getEstimasiMenit() + " menit");
        activeCell(row, data);
        actionCell(row, KIND_RUTE, data);
    }

    private void renderJadwal(Row row, JadwalAntarJemput data) {
        label(row, data.getKode());
        label(row, data.getNama());
        label(row, formatDate(data.getTanggal()));
        label(row, jam(data.getJamMulai()) + " - " + jam(data.getJamSelesai()));
        label(row, nama(data.getKendaraanAntarJemput()));
        label(row, nama(data.getSopir()));
        badge(row, data.getStatus(), statusBg(data.getStatus()), statusFg(data.getStatus()));
        activeCell(row, data);
        actionCell(row, KIND_JADWAL, data);
    }

    private void renderPeserta(Row row, PesertaJadwalAntarJemput data) {
        label(row, data.getNomorUrut() + "");
        label(row, data.getNama());
        label(row, orangLabel(data));
        label(row, nama(data.getJadwalAntarJemput()));
        label(row, data.getTitikJemput());
        label(row, data.getTitikTurun());
        activeCell(row, data);
        actionCell(row, KIND_PESERTA, data);
    }

    private void renderKartu(Row row, KartuPenjemputAntarJemput data) {
        label(row, data.getNomorKartu());
        label(row, data.getNamaPenjemput());
        label(row, data.getHubungan());
        label(row, data.getNomorHp());
        label(row, orangLabel(data));
        label(row, formatDate(data.getBerlakuSampai()));
        activeCell(row, data);
        actionCell(row, KIND_KARTU, data);
    }

    private void renderTransaksi(Row row, TransaksiPenjemputanAntarJemput data) {
        label(row, data.getNomorAntrian());
        label(row, formatDateTime(data.getWaktuScan()));
        label(row, data.getTipeScan());
        label(row, data.getNomorScan());
        label(row, data.getPintuGerbang());
        label(row, data.getKartuPenjemputAntarJemput() != null
                ? data.getKartuPenjemputAntarJemput().getNamaPenjemput() : "");
        badge(row, data.getStatus(), statusBg(data.getStatus()), statusFg(data.getStatus()));
        transaksiActionCell(row, data);
    }

    private void renderDetail(Row row, DetailPenjemputanAntarJemput data) {
        label(row, data.getNama());
        label(row, data.getPerangkatTujuan());
        label(row, data.getTeksPanggilan());
        badge(row, data.getStatusPanggilan(), statusBg(data.getStatusPanggilan()), statusFg(data.getStatusPanggilan()));
        label(row, formatDateTime(data.getWaktuDipanggil()));
        label(row, formatDateTime(data.getWaktuSerahTerima()));
        detailActionCell(row, data);
    }

    private void renderLog(Row row, LogNotifikasiAntarJemput data) {
        label(row, data.getKanal());
        label(row, data.getPerangkatTujuan());
        label(row, data.getPesan());
        badge(row, data.getStatus(), statusBg(data.getStatus()), statusFg(data.getStatus()));
        label(row, data.getPercobaan() != null ? data.getPercobaan() + "" : "");
        label(row, formatDateTime(data.getWaktuKirim()));
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private void label(Row row, String value) {
        new Label(value != null ? value : "").setParent(row);
    }

    private void badge(Row row, String value, String bg, String fg) {
        new Html("<span class='aj-badge' style='background:" + bg + ";color:" + fg + "'>"
                + esc(value) + "</span>").setParent(row);
    }

    private void activeCell(Row row, final GeneralValueObject obj) {
        final MyCheckboxConfig cb = new MyCheckboxConfig("Aktif");
        cb.setChecked(readAktif(obj));
        cb.setDisabled(!canUpdate);
        cb.setParent(row);
        cb.addEventListener("onCheck", new EventListener() {
            public void onEvent(Event event) throws Exception {
                writeAktif(obj, cb.isChecked());
                Common.refreshSaveOrUpdate(obj);
                renderDashboard();
            }
        });
    }

    private void actionCell(Row row, final String kind, final GeneralValueObject obj) {
        Toolbar tb = rowToolbar(row);
        MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
        edit.setDisabled(!canUpdate);
        edit.setTooltiptext("Ubah data");
        edit.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception { openForm(kind, reload(obj)); }
        });
        edit.setParent(tb);
        MyToolbarbuttonConfig del = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
        del.setDisabled(!canDelete);
        del.setTooltiptext("Hapus data ini");
        del.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception { deleteData(kind, obj); }
        });
        del.setParent(tb);
    }

    private void transaksiActionCell(Row row, final TransaksiPenjemputanAntarJemput data) {
        Toolbar tb = rowToolbar(row);
        addTxBtn(tb, "Panggil", "/img/Sound-on-icon.png", data, TransaksiPenjemputanAntarJemput.DIPANGGIL, "Ubah status menjadi Dipanggil");
        addTxBtn(tb, "Selesai", "/img/ok.gif",            data, TransaksiPenjemputanAntarJemput.SELESAI,   "Tandai selesai");
        addTxBtn(tb, "Tolak",   "/img/cancel.gif",        data, TransaksiPenjemputanAntarJemput.DITOLAK,   "Tolak transaksi");
    }

    private void detailActionCell(Row row, final DetailPenjemputanAntarJemput data) {
        Toolbar tb = rowToolbar(row);
        addDetailBtn(tb, "Dipanggil", data, DetailPenjemputanAntarJemput.SUDAH_DIPANGGIL, "Tandai sudah dipanggil");
        addDetailBtn(tb, "Keluar",    data, DetailPenjemputanAntarJemput.KELUAR_KELAS,     "Tandai keluar kelas");
        addDetailBtn(tb, "Serah",     data, DetailPenjemputanAntarJemput.DISERAHKAN,        "Konfirmasi serah terima");
    }

    private Toolbar rowToolbar(Row row) {
        Toolbar tb = new Toolbar();
        tb.setSclass("aj-row-actions");
        tb.setParent(row);
        return tb;
    }

    private void addTxBtn(Toolbar tb, String label, String img,
            final TransaksiPenjemputanAntarJemput data, final String status, String tip) {
        MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(label, img);
        btn.setDisabled(!canUpdate);
        btn.setTooltiptext(tip);
        btn.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception { updateTransaksiStatus(data, status); }
        });
        btn.setParent(tb);
    }

    private void addDetailBtn(Toolbar tb, String label,
            final DetailPenjemputanAntarJemput data, final String status, String tip) {
        MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(label, "/img/ok.gif");
        btn.setDisabled(!canUpdate);
        btn.setTooltiptext(tip);
        btn.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception { updateDetailStatus(data, status); }
        });
        btn.setParent(tb);
    }

    // ===========================================================================
    // FORM DIALOG
    // ===========================================================================

    private void openForm(String kind, GeneralValueObject obj) {
        if (!canCreate && (obj == null || obj.getId() == null)) return;
        currentKind   = kind;
        currentObject = obj;
        inputs.clear();
        Common.clear(formWindow);
        formWindow.setTitle(titleFor(kind));

        Borderlayout layout = new MyBorderlayout();
        layout.setParent(formWindow);
        Center center = new Center();
        ais.ui.util.ZkCompat.setFlex(center, true);
        center.setParent(layout);
        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setParent(center);
        Columns cols = new Columns();
        cols.setParent(grid);
        MyColumnConfig col1 = new MyColumnConfig();
        col1.setWidth("32%");
        col1.setParent(cols);
        new MyColumnConfig().setParent(cols);
        Rows rows = new Rows();
        rows.setParent(grid);
        buildFormRows(kind, obj, rows);

        South south = new South();
        ais.ui.util.ZkCompat.setFlex(south, true);
        south.setParent(layout);
        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);
        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup tanpa menyimpan");
        cancel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception { formWindow.setVisible(false); }
        });
        cancel.setParent(toolbar);
        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setDisabled(!canUpdate && currentObject != null && currentObject.getId() != null);
        save.setTooltiptext("Simpan perubahan");
        save.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception {
                if (saveForm()) formWindow.setVisible(false);
            }
        });
        save.setParent(toolbar);

        formWindow.setWidth(Common.isMobile() ? "100%" : "760px");
        formWindow.setHeight(Common.isMobile() ? "100%" : "560px");
        formWindow.setVisible(true);
        try {
            formWindow.onModal();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void buildFormRows(String kind, GeneralValueObject obj, Rows rows) {
        addText(rows, "kode",       "Kode",       obj.getKode(),       false);
        addText(rows, "nama",       "Nama *",     obj.getNama(),       false);
        addText(rows, "keterangan", "Keterangan", obj.getKeterangan(), true);

        if (KIND_KENDARAAN.equals(kind)) {
            KendaraanAntarJemput d = (KendaraanAntarJemput) obj;
            addText(rows, "nomorPolisi",   "Nomor Polisi",     d.getNomorPolisi(), false);
            addInt (rows, "kapasitasDuduk","Kapasitas Duduk",  d.getKapasitasDuduk());
            addLong(rows, "asset",         "ID Asset",         id(d.getAsset()));
            addLong(rows, "assetDetail",   "ID Asset Detail",  id(d.getAssetDetail()));
            addLong(rows, "sopir",         "ID Pegawai Sopir", id(d.getSopir()));
            addLong(rows, "kenek1",        "ID Pegawai Kenek 1", id(d.getKenek1()));
            addLong(rows, "kenek2",        "ID Pegawai Kenek 2", id(d.getKenek2()));
            addLong(rows, "kenek3",        "ID Pegawai Kenek 3", id(d.getKenek3()));
            addBool(rows, "aktif",         "Aktif",            d.getAktif());
        } else if (KIND_RUTE.equals(kind)) {
            RuteAntarJemput d = (RuteAntarJemput) obj;
            addText(rows, "jenisLayanan",  "Jenis Layanan",  d.getJenisLayanan(), false);
            addText(rows, "titikAwal",     "Titik Awal",     d.getTitikAwal(),    true);
            addText(rows, "titikAkhir",    "Titik Akhir",    d.getTitikAkhir(),   true);
            addTime(rows, "jamBerangkat",  "Jam Berangkat",  d.getJamBerangkat());
            addInt (rows, "estimasiMenit", "Estimasi Menit", d.getEstimasiMenit());
            addBool(rows, "aktif",         "Aktif",          d.getAktif());
        } else if (KIND_JADWAL.equals(kind)) {
            JadwalAntarJemput d = (JadwalAntarJemput) obj;
            addDate(rows, "tanggal",              "Tanggal",       d.getTanggal());
            addTime(rows, "jamMulai",             "Jam Mulai",     d.getJamMulai());
            addTime(rows, "jamSelesai",           "Jam Selesai",   d.getJamSelesai());
            addText(rows, "hari",                 "Hari",          d.getHari(),        false);
            addText(rows, "tahunAjaran",          "Tahun Ajaran",  d.getTahunAjaran(), false);
            addInt (rows, "semester",             "Semester",      d.getSemester());
            addText(rows, "status",               "Status",        d.getStatus(),      false);
            addLong(rows, "ruteAntarJemput",      "ID Rute",       id(d.getRuteAntarJemput()));
            addLong(rows, "kendaraanAntarJemput", "ID Kendaraan",  id(d.getKendaraanAntarJemput()));
            addLong(rows, "sopir",                "ID Sopir",      id(d.getSopir()));
            addLong(rows, "kenek1",               "ID Kenek 1",    id(d.getKenek1()));
            addLong(rows, "kenek2",               "ID Kenek 2",    id(d.getKenek2()));
            addLong(rows, "kenek3",               "ID Kenek 3",    id(d.getKenek3()));
            addBool(rows, "aktif",                "Aktif",         d.getAktif());
        } else if (KIND_PESERTA.equals(kind)) {
            PesertaJadwalAntarJemput d = (PesertaJadwalAntarJemput) obj;
            addInt (rows, "nomorUrut",        "Nomor Urut",       d.getNomorUrut());
            addLong(rows, "jadwalAntarJemput","ID Jadwal",         id(d.getJadwalAntarJemput()));
            addLong(rows, "siswa",            "ID Siswa",          id(d.getSiswa()));
            addLong(rows, "mahasiswa",        "ID Mahasiswa",      id(d.getMahasiswa()));
            addLong(rows, "guru",             "ID Guru",           id(d.getGuru()));
            addLong(rows, "dosen",            "ID Dosen",          id(d.getDosen()));
            addLong(rows, "pegawai",          "ID Pegawai",        id(d.getPegawai()));
            addLong(rows, "kelasSiswa",       "ID Kelas Siswa",    id(d.getKelasSiswa()));
            addText(rows, "titikJemput",      "Titik Jemput",      d.getTitikJemput(),     true);
            addText(rows, "titikTurun",       "Titik Turun",       d.getTitikTurun(),       true);
            addText(rows, "catatanKesehatan", "Catatan Khusus",    d.getCatatanKesehatan(), true);
            addText(rows, "statusLangganan",  "Status Langganan",  d.getStatusLangganan(),  false);
            addBool(rows, "aktif",            "Aktif",             d.getAktif());
        } else if (KIND_KARTU.equals(kind)) {
            KartuPenjemputAntarJemput d = (KartuPenjemputAntarJemput) obj;
            addText(rows, "namaPenjemput", "Nama Penjemput *", d.getNamaPenjemput(),  false);
            addText(rows, "hubungan",      "Hubungan",         d.getHubungan(),        false);
            addText(rows, "nomorIdentitas","Nomor Identitas",  d.getNomorIdentitas(),  false);
            addText(rows, "nomorHp",       "Nomor HP",         d.getNomorHp(),         false);
            addText(rows, "nomorKartu",    "Nomor Kartu *",    d.getNomorKartu(),      false);
            addText(rows, "barcode",       "Barcode / QR",     d.getBarcode(),         false);
            addDate(rows, "berlakuMulai",  "Berlaku Mulai",    d.getBerlakuMulai());
            addDate(rows, "berlakuSampai", "Berlaku Sampai",   d.getBerlakuSampai());
            addLong(rows, "siswa",         "ID Siswa",         id(d.getSiswa()));
            addLong(rows, "mahasiswa",     "ID Mahasiswa",     id(d.getMahasiswa()));
            addLong(rows, "guru",          "ID Guru",          id(d.getGuru()));
            addLong(rows, "dosen",         "ID Dosen",         id(d.getDosen()));
            addLong(rows, "pegawai",       "ID Pegawai",       id(d.getPegawai()));
            addBool(rows, "aktif",         "Aktif",            d.getAktif());
        }
    }

    private boolean saveForm() {
        try {
            if (!KIND_KARTU.equals(currentKind) && text("nama").trim().length() == 0) {
                warn("Nama wajib diisi"); return false;
            }
            if (KIND_KARTU.equals(currentKind)) {
                if (text("namaPenjemput").trim().length() == 0) { warn("Nama penjemput wajib diisi"); return false; }
                if (text("nomorKartu").trim().length() == 0)    { warn("Nomor kartu wajib diisi");    return false; }
            }
            Session session = HibernateUtil.currentSession();
            if (currentObject.getId() != null) {
                currentObject = (GeneralValueObject) session.load(entityClassFor(currentKind), currentObject.getId());
            }
            writeCommon(currentObject);
            writeSpecific(session, currentKind, currentObject);
            Common.refreshSaveOrUpdate(session, currentObject);
            search(currentKind);
            renderDashboard();
            return true;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return false;
        }
    }

    private void writeCommon(GeneralValueObject obj) {
        obj.setKode(text("kode"));
        obj.setNama(text("nama"));
        obj.setKeterangan(text("keterangan"));
    }

    private void writeSpecific(Session session, String kind, GeneralValueObject obj) {
        if (KIND_KENDARAAN.equals(kind)) {
            KendaraanAntarJemput d = (KendaraanAntarJemput) obj;
            d.setNomorPolisi(text("nomorPolisi"));
            d.setKapasitasDuduk(intValue("kapasitasDuduk"));
            d.setAsset((Asset)       loadById(session, Asset.class,       longValue("asset")));
            d.setAssetDetail((AssetDetail) loadById(session, AssetDetail.class, longValue("assetDetail")));
            d.setSopir((Pegawai)     loadById(session, Pegawai.class,     longValue("sopir")));
            d.setKenek1((Pegawai)    loadById(session, Pegawai.class,     longValue("kenek1")));
            d.setKenek2((Pegawai)    loadById(session, Pegawai.class,     longValue("kenek2")));
            d.setKenek3((Pegawai)    loadById(session, Pegawai.class,     longValue("kenek3")));
            d.setAktif(bool("aktif"));
        } else if (KIND_RUTE.equals(kind)) {
            RuteAntarJemput d = (RuteAntarJemput) obj;
            d.setJenisLayanan(text("jenisLayanan"));
            d.setTitikAwal(text("titikAwal"));
            d.setTitikAkhir(text("titikAkhir"));
            d.setJamBerangkat(date("jamBerangkat"));
            d.setEstimasiMenit(intValue("estimasiMenit"));
            d.setAktif(bool("aktif"));
        } else if (KIND_JADWAL.equals(kind)) {
            JadwalAntarJemput d = (JadwalAntarJemput) obj;
            d.setTanggal(date("tanggal"));
            d.setJamMulai(date("jamMulai"));
            d.setJamSelesai(date("jamSelesai"));
            d.setHari(text("hari"));
            d.setTahunAjaran(text("tahunAjaran"));
            d.setSemester(intValue("semester"));
            d.setStatus(text("status"));
            d.setRuteAntarJemput((RuteAntarJemput)       loadById(session, RuteAntarJemput.class,       longValue("ruteAntarJemput")));
            d.setKendaraanAntarJemput((KendaraanAntarJemput) loadById(session, KendaraanAntarJemput.class, longValue("kendaraanAntarJemput")));
            d.setSopir((Pegawai)  loadById(session, Pegawai.class, longValue("sopir")));
            d.setKenek1((Pegawai) loadById(session, Pegawai.class, longValue("kenek1")));
            d.setKenek2((Pegawai) loadById(session, Pegawai.class, longValue("kenek2")));
            d.setKenek3((Pegawai) loadById(session, Pegawai.class, longValue("kenek3")));
            d.setAktif(bool("aktif"));
        } else if (KIND_PESERTA.equals(kind)) {
            PesertaJadwalAntarJemput d = (PesertaJadwalAntarJemput) obj;
            d.setNomorUrut(intValue("nomorUrut"));
            d.setJadwalAntarJemput((JadwalAntarJemput) loadById(session, JadwalAntarJemput.class, longValue("jadwalAntarJemput")));
            d.setSiswa((Siswa)       loadById(session, Siswa.class,       longValue("siswa")));
            d.setMahasiswa((Mahasiswa) loadById(session, Mahasiswa.class, longValue("mahasiswa")));
            d.setGuru((Guru)         loadById(session, Guru.class,        longValue("guru")));
            d.setDosen((Dosen)       loadById(session, Dosen.class,       longValue("dosen")));
            d.setPegawai((Pegawai)   loadById(session, Pegawai.class,     longValue("pegawai")));
            d.setKelasSiswa((KelasSiswa) loadById(session, KelasSiswa.class, longValue("kelasSiswa")));
            d.setTitikJemput(text("titikJemput"));
            d.setTitikTurun(text("titikTurun"));
            d.setCatatanKesehatan(text("catatanKesehatan"));
            d.setStatusLangganan(text("statusLangganan"));
            d.setAktif(bool("aktif"));
        } else if (KIND_KARTU.equals(kind)) {
            KartuPenjemputAntarJemput d = (KartuPenjemputAntarJemput) obj;
            d.setNamaPenjemput(text("namaPenjemput"));
            d.setHubungan(text("hubungan"));
            d.setNomorIdentitas(text("nomorIdentitas"));
            d.setNomorHp(text("nomorHp"));
            d.setNomorKartu(text("nomorKartu"));
            d.setBarcode(text("barcode"));
            d.setBerlakuMulai(date("berlakuMulai"));
            d.setBerlakuSampai(date("berlakuSampai"));
            d.setSiswa((Siswa)       loadById(session, Siswa.class,       longValue("siswa")));
            d.setMahasiswa((Mahasiswa) loadById(session, Mahasiswa.class, longValue("mahasiswa")));
            d.setGuru((Guru)         loadById(session, Guru.class,        longValue("guru")));
            d.setDosen((Dosen)       loadById(session, Dosen.class,       longValue("dosen")));
            d.setPegawai((Pegawai)   loadById(session, Pegawai.class,     longValue("pegawai")));
            d.setAktif(bool("aktif"));
        }
    }

    private void deleteData(String kind, GeneralValueObject obj) {
        try {
            Session session = HibernateUtil.currentSession();
            GeneralValueObject reloaded = (GeneralValueObject) session.load(entityClassFor(kind), obj.getId());
            Common.refreshDelete(reloaded);
            search(kind);
            renderDashboard();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private GeneralValueObject reload(GeneralValueObject obj) {
        if (obj == null || obj.getId() == null) return obj;
        return (GeneralValueObject) HibernateUtil.currentSession().get(obj.getClass(), obj.getId());
    }

    // ===========================================================================
    // GATE VERIFICATION
    // ===========================================================================

    private void verifikasiGerbang() {
        try {
            String nomor = scanNomor != null && scanNomor.getValue() != null
                    ? scanNomor.getValue().trim() : "";
            if (nomor.length() == 0) { warn("Nomor kartu atau barcode wajib diisi"); return; }

            Session session = HibernateUtil.currentSession();
            KartuPenjemputAntarJemput kartu = (KartuPenjemputAntarJemput) session
                    .createCriteria(KartuPenjemputAntarJemput.class)
                    .add(Restrictions.or(Restrictions.eq("nomorKartu", nomor), Restrictions.eq("barcode", nomor)))
                    .setMaxResults(1).uniqueResult();

            if (kartu == null || !Boolean.TRUE.equals(kartu.getAktif())) {
                createRejectedTransaction(session, nomor, "Kartu tidak ditemukan atau tidak aktif"); return;
            }
            if (kartu.getBerlakuSampai() != null && kartu.getBerlakuSampai().before(WaktuUtil.getDate())) {
                createRejectedTransaction(session, nomor, "Masa berlaku kartu sudah habis"); return;
            }

            JadwalAntarJemput jadwal = null;
            if (scanJadwalId != null && scanJadwalId.getValue() != null) {
                jadwal = (JadwalAntarJemput) session.get(JadwalAntarJemput.class, scanJadwalId.getValue());
            }

            TransaksiPenjemputanAntarJemput transaksi = createTransaction(session, nomor, kartu, jadwal, TransaksiPenjemputanAntarJemput.MENUNGGU);
            List peserta = findPeserta(session, kartu, jadwal);

            if (peserta.isEmpty()) {
                transaksi.setStatus(TransaksiPenjemputanAntarJemput.DITOLAK);
                transaksi.setKeterangan("Tidak ada peserta aktif yang cocok");
                Common.refreshSaveOrUpdate(session, transaksi);
                setScanStatus("Ditolak: peserta tidak ditemukan pada jadwal aktif", false);
                search(KIND_TRANSAKSI);
                renderAntrianGerbang();
                return;
            }

            for (int i = 0; i < peserta.size(); i++) {
                PesertaJadwalAntarJemput p = (PesertaJadwalAntarJemput) peserta.get(i);
                DetailPenjemputanAntarJemput detail = new DetailPenjemputanAntarJemput();
                detail.setTransaksiPenjemputanAntarJemput(transaksi);
                detail.setPesertaJadwalAntarJemput(p);
                detail.setNama(p.getNama());
                detail.setSiswa(p.getSiswa());
                detail.setMahasiswa(p.getMahasiswa());
                detail.setGuru(p.getGuru());
                detail.setDosen(p.getDosen());
                detail.setPegawai(p.getPegawai());
                detail.setKelasSiswa(p.getKelasSiswa());
                detail.setPerangkatTujuan(resolvePerangkatTujuan(p));
                detail.setTeksPanggilan(resolveTeksPanggilan(p));
                detail.setStatusPanggilan(DetailPenjemputanAntarJemput.MENUNGGU_PANGGILAN);
                Common.refreshSaveOrUpdate(session, detail);
                createNotifLog(session, detail, "ANTRI");
            }
            transaksi.setStatus(TransaksiPenjemputanAntarJemput.DIPANGGIL);
            Common.refreshSaveOrUpdate(session, transaksi);
            setScanStatus("Berhasil: " + peserta.size() + " peserta masuk antrian panggilan", true);
            search(KIND_TRANSAKSI);
            search(KIND_DETAIL);
            search(KIND_LOG);
            renderDashboard();
            renderMonitor();
            renderAntrianGerbang();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private TransaksiPenjemputanAntarJemput createTransaction(Session session, String nomor,
            KartuPenjemputAntarJemput kartu, JadwalAntarJemput jadwal, String status) {
        TransaksiPenjemputanAntarJemput t = new TransaksiPenjemputanAntarJemput();
        t.setKode("AJ-" + WaktuUtil.getDate().getTime());
        t.setNama(kartu != null ? kartu.getNamaPenjemput() : nomor);
        t.setWaktuScan(WaktuUtil.getDate());
        t.setTipeScan("KARTU_BARCODE");
        t.setNomorScan(nomor);
        t.setPintuGerbang(scanPintu != null ? scanPintu.getValue() : null);
        t.setNomorAntrian(generateNomorAntrian(session));
        t.setKartuPenjemputAntarJemput(kartu);
        t.setJadwalAntarJemput(jadwal);
        t.setStatus(status);
        Common.refreshSaveOrUpdate(session, t);
        return t;
    }

    private void createRejectedTransaction(Session session, String nomor, String message) {
        TransaksiPenjemputanAntarJemput t = createTransaction(session, nomor, null, null, TransaksiPenjemputanAntarJemput.DITOLAK);
        t.setKeterangan(message);
        Common.refreshSaveOrUpdate(session, t);
        setScanStatus("Ditolak: " + message, false);
        search(KIND_TRANSAKSI);
        renderDashboard();
        renderAntrianGerbang();
    }

    private List findPeserta(Session session, KartuPenjemputAntarJemput kartu, JadwalAntarJemput jadwal) {
        Criteria c = session.createCriteria(PesertaJadwalAntarJemput.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        if (jadwal != null) c.add(Restrictions.eq("jadwalAntarJemput", jadwal));
        List<org.hibernate.criterion.Criterion> ors = new ArrayList<org.hibernate.criterion.Criterion>();
        if (kartu.getSiswa()     != null) ors.add(Restrictions.eq("siswa",     kartu.getSiswa()));
        if (kartu.getMahasiswa() != null) ors.add(Restrictions.eq("mahasiswa", kartu.getMahasiswa()));
        if (kartu.getGuru()      != null) ors.add(Restrictions.eq("guru",      kartu.getGuru()));
        if (kartu.getDosen()     != null) ors.add(Restrictions.eq("dosen",     kartu.getDosen()));
        if (kartu.getPegawai()   != null) ors.add(Restrictions.eq("pegawai",   kartu.getPegawai()));
        if (ors.isEmpty()) return new ArrayList();
        org.hibernate.criterion.Criterion combined = ors.get(0);
        for (int i = 1; i < ors.size(); i++) combined = Restrictions.or(combined, ors.get(i));
        c.add(combined);
        c.addOrder(Order.asc("nomorUrut"));
        return c.list();
    }

    private void updateTransaksiStatus(TransaksiPenjemputanAntarJemput data, String status) {
        try {
            Session session = HibernateUtil.currentSession();
            TransaksiPenjemputanAntarJemput t = (TransaksiPenjemputanAntarJemput)
                    session.load(TransaksiPenjemputanAntarJemput.class, data.getId());
            t.setStatus(status);
            Common.refreshSaveOrUpdate(session, t);
            search(KIND_TRANSAKSI);
            renderDashboard();
            renderMonitor();
            renderAntrianGerbang();
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    private void updateDetailStatus(DetailPenjemputanAntarJemput data, String status) {
        try {
            Session session = HibernateUtil.currentSession();
            DetailPenjemputanAntarJemput detail = (DetailPenjemputanAntarJemput)
                    session.load(DetailPenjemputanAntarJemput.class, data.getId());
            detail.setStatusPanggilan(status);
            if (DetailPenjemputanAntarJemput.SUDAH_DIPANGGIL.equals(status)) {
                detail.setWaktuDipanggil(WaktuUtil.getDate());
                createNotifLog(session, detail, "TERKIRIM");
            } else if (DetailPenjemputanAntarJemput.KELUAR_KELAS.equals(status)) {
                detail.setWaktuKeluarKelas(WaktuUtil.getDate());
            } else if (DetailPenjemputanAntarJemput.DISERAHKAN.equals(status)) {
                detail.setWaktuSerahTerima(WaktuUtil.getDate());
            }
            Common.refreshSaveOrUpdate(session, detail);
            search(KIND_DETAIL);
            search(KIND_LOG);
            renderDashboard();
            renderMonitor();
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    private void createNotifLog(Session session, DetailPenjemputanAntarJemput detail, String status) {
        LogNotifikasiAntarJemput log = new LogNotifikasiAntarJemput();
        log.setDetailPenjemputanAntarJemput(detail);
        log.setKanal("SOUNDBOX");
        log.setPerangkatTujuan(detail.getPerangkatTujuan());
        log.setPesan(detail.getTeksPanggilan());
        log.setStatus(status);
        log.setPercobaan(Integer.valueOf(1));
        log.setWaktuKirim(WaktuUtil.getDate());
        Common.refreshSaveOrUpdate(session, log);
    }

    private String generateNomorAntrian(Session session) {
        Number count = (Number) session.createCriteria(TransaksiPenjemputanAntarJemput.class)
                .setProjection(Projections.rowCount()).uniqueResult();
        return "AJ-" + (count != null ? count.intValue() + 1 : 1);
    }

    // ===========================================================================
    // DASHBOARD — orchestrator
    // ===========================================================================

    private void renderDashboard() {
        if (dashboardPanel == null) return;
        Common.clear(dashboardPanel);
        Div wrap = div(dashboardPanel, "aj-dashboard-wrap");

        // Collect all stats once to reuse across sections
        int menunggu      = countStatus(TransaksiPenjemputanAntarJemput.class, "status", TransaksiPenjemputanAntarJemput.MENUNGGU);
        int dipanggil     = countStatus(TransaksiPenjemputanAntarJemput.class, "status", TransaksiPenjemputanAntarJemput.DIPANGGIL);
        int selesai       = countStatus(TransaksiPenjemputanAntarJemput.class, "status", TransaksiPenjemputanAntarJemput.SELESAI);
        int ditolak       = countStatus(TransaksiPenjemputanAntarJemput.class, "status", TransaksiPenjemputanAntarJemput.DITOLAK);
        int soundboxOk    = countStatus(LogNotifikasiAntarJemput.class, "status", "TERKIRIM");
        int soundboxGagal = countStatus(LogNotifikasiAntarJemput.class, "status", "GAGAL");

        int cntKendaraan  = countActive(KendaraanAntarJemput.class);
        int cntRute       = countActive(RuteAntarJemput.class);
        int cntJadwal     = countActive(JadwalAntarJemput.class);
        int cntKartu      = countActive(KartuPenjemputAntarJemput.class);
        int cntPeserta    = countActive(PesertaJadwalAntarJemput.class);

        buildKpiSection(wrap, menunggu, dipanggil, selesai, ditolak, soundboxOk, soundboxGagal);
        buildChartsRow(wrap, menunggu, dipanggil, selesai, ditolak,
                cntKendaraan, cntRute, cntJadwal, cntKartu, cntPeserta);
        buildFunnelDonutRow(wrap, menunggu, dipanggil, selesai, ditolak);
        buildReadinessSection(wrap, cntKendaraan, cntRute, cntJadwal, cntKartu, cntPeserta);
        buildActivitySection(wrap);
    }

    // ── Section: KPI Cards ─────────────────────────────────────────────────────

    private void buildKpiSection(Div parent,
            int menunggu, int dipanggil, int selesai, int ditolak,
            int soundboxOk, int soundboxGagal) {
        sectionHeader(parent,
            "Ringkasan Hari Ini",
            "Angka di bawah ini menunjukkan kondisi penjemputan hari ini secara langsung (real-time). " +
            "Klik Refresh Dasbor untuk memperbarui.");
        Div grid = div(parent, "aj-kpi-grid");
        kpiCard(grid, "Menunggu",       menunggu,      "Penjemput sudah scan, menunggu siswa dipanggil",         COLOR_BLUE,   "?");
        kpiCard(grid, "Dipanggil",      dipanggil,     "Siswa sedang dalam proses pemanggilan ke kelas",         COLOR_PURPLE, "!");
        kpiCard(grid, "Selesai",        selesai,       "Siswa sudah diserahkan ke penjemput dengan selamat",     COLOR_GREEN,  "V");
        kpiCard(grid, "Ditolak",        ditolak,       "Kartu tidak valid atau data tidak cocok, perlu dicek",   COLOR_RED,    "X");
        kpiCard(grid, "Notif Terkirim", soundboxOk,    "Suara panggilan berhasil diputar melalui soundbox kelas", COLOR_TEAL,  "OK");
        kpiCard(grid, "Notif Gagal",    soundboxGagal, "Suara panggilan gagal, perlu dicek perangkat soundbox",  COLOR_ORANGE, "!");
    }

    private void kpiCard(Div parent, String title, int value, String desc, String color, String icon) {
        String html = "<div class='aj-kpi-card' style='border-left-color:" + color + "'>"
            + "<div class='aj-kpi-top'>"
            +   "<div class='aj-kpi-icon' style='background:" + color + "'>" + esc(icon) + "</div>"
            +   "<div class='aj-kpi-val' style='color:" + color + "'>" + value + "</div>"
            + "</div>"
            + "<div class='aj-kpi-title'>" + esc(title) + "</div>"
            + "<div class='aj-kpi-desc'>" + esc(desc) + "</div>"
            + "</div>";
        html(parent, html);
    }

    // ── Section: Charts Row (Trend Bar + Radar Spider) ─────────────────────────

    private void buildChartsRow(Div parent,
            int menunggu, int dipanggil, int selesai, int ditolak,
            int cntKendaraan, int cntRute, int cntJadwal, int cntKartu, int cntPeserta) {
        sectionHeader(parent,
            "Grafik & Analisis",
            "Grafik batang menunjukkan tren jumlah penjemputan setiap hari selama 7 hari terakhir. " +
            "Grafik jaring laba-laba menunjukkan seberapa lengkap data pendukung layanan sudah diisi.");
        Div row = div(parent, "aj-charts-row");

        // --- Bar Chart: 7-day trend ---
        int[] trendCounts = last7DayCounts();
        String[] trendLabels = last7DayLabels();
        String barSvg = buildTrendBarSvg(trendLabels, trendCounts);
        String barCard = "<div class='aj-chart-card'>"
            + "<div class='aj-chart-title'>Tren Penjemputan 7 Hari Terakhir</div>"
            + "<div class='aj-chart-desc'>Jumlah scan kartu penjemput yang masuk per hari. Grafik ini membantu Anda melihat hari-hari tersibuk.</div>"
            + barSvg + "</div>";
        html(row, barCard);

        // --- Radar/Spider Chart: readiness ---
        int maxRef = Math.max(10, Math.max(cntKendaraan, Math.max(cntRute,
                Math.max(cntJadwal, Math.max(cntKartu, cntPeserta)))));
        String[] radarLabels = {"Kendaraan", "Rute", "Jadwal", "Kartu", "Peserta"};
        int[]    radarValues = {cntKendaraan, cntRute, cntJadwal, cntKartu, cntPeserta};
        String radarSvg = buildRadarSvg(radarLabels, radarValues, maxRef);
        String radarCard = "<div class='aj-chart-card'>"
            + "<div class='aj-chart-title'>Radar Kesiapan Layanan</div>"
            + "<div class='aj-chart-desc'>Grafik jaring ini menunjukkan seberapa lengkap data kendaraan, rute, jadwal, kartu, dan peserta yang sudah aktif. Semakin lebar jarring, semakin siap layanan.</div>"
            + radarSvg + "</div>";
        html(row, radarCard);
    }

    // ── Section: Funnel + Donut ────────────────────────────────────────────────

    private void buildFunnelDonutRow(Div parent,
            int menunggu, int dipanggil, int selesai, int ditolak) {
        sectionHeader(parent,
            "Alur Penjemputan & Distribusi Status",
            "Diagram corong menggambarkan perjalanan penjemput dari scan kartu hingga serah terima siswa. " +
            "Diagram lingkaran menunjukkan proporsi masing-masing status transaksi hari ini.");
        Div row = div(parent, "aj-charts-row");

        // Funnel
        int totalScan   = menunggu + dipanggil + selesai + ditolak;
        int totalDipanggil = dipanggil + selesai;
        int totalSelesai   = selesai;
        String funnelHtml = buildFunnelHtml(
            new String[]{"Scan Gerbang", "Dipanggil", "Serah Terima"},
            new int[]{totalScan, totalDipanggil, totalSelesai},
            new String[]{COLOR_BLUE, COLOR_PURPLE, COLOR_GREEN});
        html(row, "<div class='aj-chart-card'>"
            + "<div class='aj-chart-title'>Alur Penjemputan (Corong)</div>"
            + "<div class='aj-chart-desc'>Setiap tahap menunjukkan berapa siswa yang berhasil melewati proses tersebut. Idealnya angka Serah Terima mendekati angka Scan Gerbang.</div>"
            + funnelHtml + "</div>");

        // Donut
        String donutSvg = buildDonutSvg(
            new String[]{"Menunggu", "Dipanggil", "Selesai", "Ditolak"},
            new int[]{menunggu, dipanggil, selesai, ditolak},
            new String[]{COLOR_BLUE, COLOR_PURPLE, COLOR_GREEN, COLOR_RED});
        String donutLegend = buildDonutLegend(
            new String[]{"Menunggu", "Dipanggil", "Selesai", "Ditolak"},
            new int[]{menunggu, dipanggil, selesai, ditolak},
            new String[]{COLOR_BLUE, COLOR_PURPLE, COLOR_GREEN, COLOR_RED});
        html(row, "<div class='aj-chart-card'>"
            + "<div class='aj-chart-title'>Distribusi Status Transaksi</div>"
            + "<div class='aj-chart-desc'>Proporsi status dari semua transaksi penjemputan. Warna hijau (Selesai) yang dominan menandakan operasional berjalan lancar.</div>"
            + "<div class='aj-donut-wrap'>" + donutSvg + donutLegend + "</div>"
            + "</div>");
    }

    // ── Section: Readiness Detail ──────────────────────────────────────────────

    private void buildReadinessSection(Div parent,
            int cntKendaraan, int cntRute, int cntJadwal, int cntKartu, int cntPeserta) {
        sectionHeader(parent,
            "Status Kesiapan Layanan",
            "Menunjukkan berapa banyak data yang sudah aktif dan siap digunakan untuk layanan antar jemput. " +
            "Data ini harus diisi lengkap agar proses penjemputan berjalan lancar.");
        Div grid = div(parent, "aj-readiness-grid");
        readinessBar(grid, "Kendaraan Aktif",  cntKendaraan, "Jumlah armada kendaraan yang siap beroperasi",                    COLOR_BLUE);
        readinessBar(grid, "Rute Aktif",       cntRute,      "Jumlah jalur perjalanan yang sudah didefinisikan",                COLOR_PURPLE);
        readinessBar(grid, "Jadwal Aktif",     cntJadwal,    "Jumlah jadwal antar jemput yang sedang berjalan",                 COLOR_TEAL);
        readinessBar(grid, "Kartu Penjemput",  cntKartu,     "Jumlah kartu yang sudah diterbitkan untuk orang tua/penjemput",  COLOR_AMBER);
        readinessBar(grid, "Peserta Terdaftar",cntPeserta,   "Jumlah siswa yang aktif menggunakan layanan antar jemput",        COLOR_GREEN);
    }

    private void readinessBar(Div parent, String label, int value, String desc, String color) {
        String html = "<div class='aj-readiness-item'>"
            + "<div class='aj-readiness-top'>"
            +   "<span class='aj-readiness-label'>" + esc(label) + "</span>"
            +   "<span class='aj-readiness-value' style='color:" + color + "'>" + value + "</span>"
            + "</div>"
            + "<div class='aj-readiness-bar-track'>"
            +   "<div class='aj-readiness-bar' style='width:min(100%," + value + "px);background:" + color + "'></div>"
            + "</div>"
            + "<div class='aj-readiness-desc'>" + esc(desc) + "</div>"
            + "</div>";
        html(parent, html);
    }

    // ── Section: Recent Activity Feed ──────────────────────────────────────────

    private void buildActivitySection(Div parent) {
        sectionHeader(parent,
            "Aktivitas Terkini",
            "10 transaksi scan gerbang yang paling baru ditampilkan di sini. " +
            "Gunakan tab Transaksi untuk melihat riwayat lengkap dan melakukan tindakan lebih lanjut.");
        Div feed = div(parent, "aj-feed");
        List list = HibernateUtil.currentSession()
                .createCriteria(TransaksiPenjemputanAntarJemput.class)
                .addOrder(Order.desc("waktuScan"))
                .setMaxResults(10).list();
        if (list.isEmpty()) {
            html(feed, "<div class='aj-feed-empty'>Belum ada aktivitas penjemputan hari ini.</div>");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            TransaksiPenjemputanAntarJemput t = (TransaksiPenjemputanAntarJemput) list.get(i);
            String statusBg = statusBg(t.getStatus());
            String statusFg = statusFg(t.getStatus());
            String antrian  = t.getNomorAntrian() != null ? t.getNomorAntrian() : "-";
            String waktu    = formatDateTime(t.getWaktuScan());
            String penj     = t.getKartuPenjemputAntarJemput() != null
                    ? t.getKartuPenjemputAntarJemput().getNamaPenjemput() : t.getNomorScan();
            String gerbang  = t.getPintuGerbang() != null ? t.getPintuGerbang() : "";
            html(feed, "<div class='aj-feed-item'>"
                + "<div class='aj-feed-num'>" + antrian + "</div>"
                + "<div class='aj-feed-body'>"
                +   "<div class='aj-feed-name'>" + esc(penj) + "</div>"
                +   "<div class='aj-feed-meta'>" + esc(waktu) + " &bull; " + esc(gerbang) + "</div>"
                + "</div>"
                + "<span class='aj-badge' style='background:" + statusBg + ";color:" + statusFg + "'>"
                +   esc(t.getStatus()) + "</span>"
                + "</div>");
        }
    }

    // ===========================================================================
    // SVG CHART BUILDERS
    // ===========================================================================

    /**
     * SVG bar chart - 7-day trend.
     * viewBox 500x200; bars with day labels and value labels.
     */
    private String buildTrendBarSvg(String[] labels, int[] values) {
        int n     = labels.length;
        int W     = 500, H = 200, padLeft = 30, padBottom = 30, padTop = 20;
        int barW  = (W - padLeft - 10) / n - 6;
        int plotH = H - padBottom - padTop;
        int max   = 1;
        for (int v : values) if (v > max) max = v;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg viewBox='0 0 ").append(W).append(" ").append(H)
          .append("' xmlns='http://www.w3.org/2000/svg' style='width:100%;height:auto;display:block'>");

        // Horizontal grid lines (4 lines)
        for (int g = 0; g <= 4; g++) {
            int y = padTop + plotH * g / 4;
            int gv = max * (4 - g) / 4;
            sb.append("<line x1='").append(padLeft).append("' y1='").append(y)
              .append("' x2='").append(W - 5).append("' y2='").append(y)
              .append("' stroke='#e2e8f0' stroke-width='1'/>");
            sb.append("<text x='").append(padLeft - 4).append("' y='").append(y + 4)
              .append("' text-anchor='end' font-size='9' fill='#94a3b8'>").append(gv).append("</text>");
        }

        // Bars
        for (int i = 0; i < n; i++) {
            int barH = max > 0 ? (int)((values[i] * 1.0 / max) * plotH) : 0;
            int x    = padLeft + i * ((W - padLeft - 10) / n) + 3;
            int y    = padTop + plotH - barH;
            // Bar (gradient effect via two rects)
            sb.append("<rect x='").append(x).append("' y='").append(y)
              .append("' width='").append(barW).append("' height='").append(barH)
              .append("' rx='3' fill='").append(COLOR_BLUE).append("' opacity='0.85'/>");
            // Value label on top of bar
            if (values[i] > 0) {
                sb.append("<text x='").append(x + barW / 2).append("' y='").append(y - 3)
                  .append("' text-anchor='middle' font-size='10' font-weight='bold' fill='#1e40af'>")
                  .append(values[i]).append("</text>");
            }
            // Day label below
            sb.append("<text x='").append(x + barW / 2).append("' y='").append(H - 8)
              .append("' text-anchor='middle' font-size='10' fill='#64748b'>")
              .append(esc(labels[i])).append("</text>");
        }
        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * SVG radar/spider web chart.
     * n axes around a center; data polygon filled with semi-transparent blue.
     */
    private String buildRadarSvg(String[] labels, int[] values, int maxRef) {
        int n = labels.length;
        if (n < 3) return "";
        int cx = 200, cy = 200, r = 140, size = 400;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg viewBox='0 0 ").append(size).append(" ").append(size)
          .append("' xmlns='http://www.w3.org/2000/svg' style='width:100%;height:auto;display:block'>");

        // --- Grid rings ---
        for (int ring = 1; ring <= 5; ring++) {
            int ri = r * ring / 5;
            sb.append(radarPolygonStr(cx, cy, ri, n, "#e2e8f0", "none", "1"));
        }

        // --- Axis lines ---
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int x2 = cx + (int)(r * Math.cos(angle));
            int y2 = cy + (int)(r * Math.sin(angle));
            sb.append("<line x1='").append(cx).append("' y1='").append(cy)
              .append("' x2='").append(x2).append("' y2='").append(y2)
              .append("' stroke='#cbd5e1' stroke-width='1'/>");
        }

        // --- Data polygon ---
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            double val   = maxRef > 0 ? Math.min(1.0, values[i] * 1.0 / maxRef) : 0;
            int px = cx + (int)(r * val * Math.cos(angle));
            int py = cy + (int)(r * val * Math.sin(angle));
            if (i > 0) pts.append(" ");
            pts.append(px).append(",").append(py);
        }
        sb.append("<polygon points='").append(pts)
          .append("' fill='rgba(37,99,235,0.18)' stroke='").append(COLOR_BLUE).append("' stroke-width='2'/>");

        // --- Data dots + labels ---
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            double val   = maxRef > 0 ? Math.min(1.0, values[i] * 1.0 / maxRef) : 0;
            int px = cx + (int)(r * val * Math.cos(angle));
            int py = cy + (int)(r * val * Math.sin(angle));
            sb.append("<circle cx='").append(px).append("' cy='").append(py)
              .append("' r='4' fill='").append(COLOR_BLUE).append("'/>");

            // Label at axis tip (slightly beyond max ring)
            int lx = cx + (int)((r + 22) * Math.cos(angle));
            int ly = cy + (int)((r + 22) * Math.sin(angle));
            String anchor = lx < cx - 5 ? "end" : (lx > cx + 5 ? "start" : "middle");
            sb.append("<text x='").append(lx).append("' y='").append(ly + 4)
              .append("' text-anchor='").append(anchor).append("' font-size='11' font-weight='600' fill='#334155'>")
              .append(esc(labels[i])).append("</text>");
            // Value hint
            sb.append("<text x='").append(lx).append("' y='").append(ly + 15)
              .append("' text-anchor='").append(anchor).append("' font-size='10' fill='#64748b'>")
              .append(values[i]).append("</text>");
        }
        sb.append("</svg>");
        return sb.toString();
    }

    private String radarPolygonStr(int cx, int cy, int r, int n, String stroke, String fill, String sw) {
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int px = cx + (int)(r * Math.cos(angle));
            int py = cy + (int)(r * Math.sin(angle));
            if (i > 0) pts.append(" ");
            pts.append(px).append(",").append(py);
        }
        return "<polygon points='" + pts + "' stroke='" + stroke + "' fill='" + fill
                + "' stroke-width='" + sw + "'/>";
    }

    /**
     * SVG donut chart using stroke-dasharray technique.
     * Each segment is a circle with partial stroke drawn via cumulative rotation.
     */
    private String buildDonutSvg(String[] labels, int[] values, String[] colors) {
        int total = 0;
        for (int v : values) total += v;

        if (total == 0) {
            return "<svg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg' style='width:160px;height:160px'>"
                + "<circle cx='100' cy='100' r='70' fill='none' stroke='#f1f5f9' stroke-width='28'/>"
                + "<text x='100' y='104' text-anchor='middle' font-size='11' fill='#94a3b8'>Belum ada data</text>"
                + "</svg>";
        }

        int cx = 100, cy = 100, r = 70;
        double circ = 2 * Math.PI * r;
        StringBuilder sb = new StringBuilder();
        sb.append("<svg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg' style='width:160px;height:160px'>");
        sb.append("<circle cx='100' cy='100' r='70' fill='none' stroke='#f1f5f9' stroke-width='28'/>");

        double cumAngle = -90.0; // start at top
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) continue;
            double fraction = values[i] * 1.0 / total;
            double dash     = fraction * circ;
            double gap      = circ - dash;
            sb.append("<circle cx='100' cy='100' r='70'")
              .append(" fill='none'")
              .append(" stroke='").append(colors[i % colors.length]).append("'")
              .append(" stroke-width='28'")
              .append(" stroke-dasharray='").append(String.format("%.3f", dash))
              .append(" ").append(String.format("%.3f", gap)).append("'")
              .append(" transform='rotate(").append(String.format("%.2f", cumAngle)).append(" 100 100)'/>")
              .append("\n");
            cumAngle += fraction * 360.0;
        }
        // White center hole + total
        sb.append("<circle cx='100' cy='100' r='45' fill='white'/>");
        sb.append("<text x='100' y='96' text-anchor='middle' font-size='24' font-weight='bold' fill='#0f172a'>")
          .append(total).append("</text>");
        sb.append("<text x='100' y='113' text-anchor='middle' font-size='10' fill='#94a3b8'>Total Scan</text>");
        sb.append("</svg>");
        return sb.toString();
    }

    private String buildDonutLegend(String[] labels, int[] values, String[] colors) {
        int total = 0;
        for (int v : values) total += v;
        StringBuilder sb = new StringBuilder("<div class='aj-legend'>");
        for (int i = 0; i < labels.length; i++) {
            int pct = total > 0 ? (int) Math.round(values[i] * 100.0 / total) : 0;
            sb.append("<div class='aj-legend-item'>")
              .append("<span class='aj-legend-dot' style='background:").append(colors[i % colors.length]).append("'></span>")
              .append("<span class='aj-legend-lbl'>").append(esc(labels[i])).append("</span>")
              .append("<span class='aj-legend-pct'>").append(values[i]).append(" (").append(pct).append("%)</span>")
              .append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * HTML/CSS funnel chart — centered trapezoid stages.
     * Each stage shrinks proportionally to the previous stage.
     */
    private String buildFunnelHtml(String[] labels, int[] values, String[] colors) {
        int n   = labels.length;
        int max = 1;
        for (int v : values) if (v > max) max = v;

        StringBuilder sb = new StringBuilder("<div class='aj-funnel'>");
        for (int i = 0; i < n; i++) {
            int pct = max > 0 ? Math.max(30, (int)(values[i] * 100 / max)) : 30;
            String color = colors[i % colors.length];
            int convPct  = i == 0 ? 100 : (values[0] > 0 ? (int) Math.round(values[i] * 100.0 / values[0]) : 0);
            sb.append("<div class='aj-funnel-stage' style='width:").append(pct)
              .append("%;background:").append(color).append(";opacity:").append(0.75 + 0.08 * (n - i)).append("'>")
              .append("<span class='aj-funnel-label'>").append(esc(labels[i])).append("</span>")
              .append("<span class='aj-funnel-count'>").append(values[i]).append("</span>")
              .append("<span class='aj-funnel-pct'>").append(convPct).append("%</span>")
              .append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // ===========================================================================
    // DATA HELPERS — trend counts & dates
    // ===========================================================================

    private int[] last7DayCounts() {
        int[] counts = new int[7];
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, -i);
            Date from = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date to = cal.getTime();
            counts[6 - i] = countBetweenDates(TransaksiPenjemputanAntarJemput.class, "waktuScan", from, to);
        }
        return counts;
    }

    private String[] last7DayLabels() {
        String[] labels = new String[7];
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            labels[6 - i] = HARI_SINGKAT[cal.get(Calendar.DAY_OF_WEEK) - 1];
        }
        return labels;
    }

    private int countBetweenDates(Class clazz, String field, Date from, Date to) {
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createCriteria(clazz)
                    .setProjection(Projections.rowCount())
                    .add(Restrictions.ge(field, from))
                    .add(Restrictions.lt(field, to))
                    .uniqueResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    private int countStatus(Class clazz, String field, String status) {
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createCriteria(clazz)
                    .setProjection(Projections.rowCount())
                    .add(Restrictions.eq(field, status))
                    .uniqueResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    private int countActive(Class clazz) {
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createCriteria(clazz)
                    .setProjection(Projections.rowCount())
                    .add(Restrictions.or(
                        Restrictions.isNull("aktif"),
                        Restrictions.eq("aktif", Boolean.TRUE)))
                    .uniqueResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    // ===========================================================================
    // MONITOR RENDERING
    // ===========================================================================

    private void renderMonitor() {
        if (monitorPanel == null) return;
        Common.clear(monitorPanel);
        Div outer = div(monitorPanel, "aj-monitor-outer");
        List list = HibernateUtil.currentSession()
                .createCriteria(DetailPenjemputanAntarJemput.class)
                .add(Restrictions.ne("statusPanggilan", DetailPenjemputanAntarJemput.DISERAHKAN))
                .addOrder(Order.desc("id")).setMaxResults(12).list();
        if (list.isEmpty()) {
            html(outer, "<div class='aj-monitor-empty'>Belum ada antrian penjemputan aktif saat ini.</div>");
            return;
        }
        Div grid = div(outer, "aj-monitor-grid");
        for (int i = 0; i < list.size(); i++) {
            DetailPenjemputanAntarJemput d = (DetailPenjemputanAntarJemput) list.get(i);
            html(grid, "<div class='aj-monitor-card'>"
                + "<div class='aj-monitor-number'>" + (i + 1) + "</div>"
                + "<div>"
                + "<div class='aj-monitor-name'>" + esc(d.getNama()) + "</div>"
                + "<div class='aj-monitor-sub'>"  + esc(d.getPerangkatTujuan()) + "</div>"
                + "<span class='aj-chip'>"         + esc(d.getStatusPanggilan()) + "</span>"
                + "</div>"
                + "</div>");
        }
    }

    private void renderAntrianGerbang() {
        if (gridAntrianGerbang == null) return;
        List list = HibernateUtil.currentSession()
                .createCriteria(TransaksiPenjemputanAntarJemput.class)
                .add(Restrictions.ne("status", TransaksiPenjemputanAntarJemput.SELESAI))
                .addOrder(Order.desc("waktuScan")).setMaxResults(10).list();
        gridAntrianGerbang.setRowRenderer(new AntarJemputRenderer(KIND_TRANSAKSI));
        gridAntrianGerbang.setModelCheckMobile(new SimpleListModel(list));
    }

    // ===========================================================================
    // DASHBOARD UI LAYOUT HELPERS
    // ===========================================================================

    /** Section heading + user-friendly description. */
    private void sectionHeader(Div parent, String title, String desc) {
        html(parent, "<div class='aj-section-header'>"
            + "<div class='aj-section-title'>" + esc(title) + "</div>"
            + "<div class='aj-section-desc'>" + esc(desc) + "</div>"
            + "</div>");
    }

    private Div div(Component parent, String sclass) {
        Div d = new Div();
        d.setSclass(sclass);
        d.setParent(parent);
        return d;
    }

    private void html(Component parent, String markup) {
        new Html(markup).setParent(parent);
    }

    // ===========================================================================
    // STATUS COLOR HELPERS
    // ===========================================================================

    private String statusBg(String s) {
        if (s == null) return "#e5e7eb";
        if (s.contains("SELESAI") || s.contains("SERAH") || s.contains("TERKIRIM")) return "#dcfce7";
        if (s.contains("TOLAK")   || s.contains("GAGAL"))                            return "#fee2e2";
        if (s.contains("PANGGIL"))                                                    return "#ede9fe";
        return "#e0f2fe";
    }

    private String statusFg(String s) {
        if (s == null) return "#374151";
        if (s.contains("SELESAI") || s.contains("SERAH") || s.contains("TERKIRIM")) return "#166534";
        if (s.contains("TOLAK")   || s.contains("GAGAL"))                            return "#991b1b";
        if (s.contains("PANGGIL"))                                                    return "#5b21b6";
        return "#075985";
    }

    // ===========================================================================
    // ENTITY / FORM HELPERS
    // ===========================================================================

    private Boolean readAktif(GeneralValueObject obj) {
        if (obj instanceof KendaraanAntarJemput)      return ((KendaraanAntarJemput) obj).getAktif();
        if (obj instanceof RuteAntarJemput)           return ((RuteAntarJemput) obj).getAktif();
        if (obj instanceof JadwalAntarJemput)         return ((JadwalAntarJemput) obj).getAktif();
        if (obj instanceof PesertaJadwalAntarJemput)  return ((PesertaJadwalAntarJemput) obj).getAktif();
        if (obj instanceof KartuPenjemputAntarJemput) return ((KartuPenjemputAntarJemput) obj).getAktif();
        return Boolean.TRUE;
    }

    private void writeAktif(GeneralValueObject obj, boolean aktif) {
        Boolean val = Boolean.valueOf(aktif);
        if      (obj instanceof KendaraanAntarJemput)      ((KendaraanAntarJemput) obj).setAktif(val);
        else if (obj instanceof RuteAntarJemput)           ((RuteAntarJemput) obj).setAktif(val);
        else if (obj instanceof JadwalAntarJemput)         ((JadwalAntarJemput) obj).setAktif(val);
        else if (obj instanceof PesertaJadwalAntarJemput)  ((PesertaJadwalAntarJemput) obj).setAktif(val);
        else if (obj instanceof KartuPenjemputAntarJemput) ((KartuPenjemputAntarJemput) obj).setAktif(val);
    }

    private String resolvePerangkatTujuan(PesertaJadwalAntarJemput p) {
        if (p.getKelasSiswa() != null) return "Kelas " + p.getKelasSiswa().getNama();
        if (p.getSiswa() != null && p.getSiswa().getKelas() != null) return "Kelas " + p.getSiswa().getKelas().getNama();
        return "Monitor Gerbang";
    }

    private String resolveTeksPanggilan(PesertaJadwalAntarJemput p) {
        String nama = p.getNama() != null ? p.getNama() : "";
        return p.getSiswa() != null
                ? "Penjemputan ananda " + nama + " sudah datang."
                : nama + " sudah ditunggu di gerbang.";
    }

    private Object loadById(Session session, Class clazz, Long id) {
        return id != null ? session.get(clazz, id) : null;
    }

    private Long id(GeneralValueObject obj)    { return obj != null ? obj.getId()   : null; }
    private String nama(GeneralValueObject obj) { return obj != null && obj.getNama() != null ? obj.getNama() : ""; }

    private String buildKruNames(KendaraanAntarJemput data) {
        StringBuilder sb = new StringBuilder();
        appendNama(sb, data.getKenek1());
        appendNama(sb, data.getKenek2());
        appendNama(sb, data.getKenek3());
        return sb.toString();
    }

    private void appendNama(StringBuilder sb, GeneralValueObject obj) {
        if (obj == null || obj.getNama() == null) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(obj.getNama());
    }

    private String orangLabel(PesertaJadwalAntarJemput p) {
        if (p.getSiswa()     != null) return "Siswa #"     + p.getSiswa().getId();
        if (p.getMahasiswa() != null) return "Mahasiswa #" + p.getMahasiswa().getId();
        if (p.getGuru()      != null) return "Guru #"      + p.getGuru().getId();
        if (p.getDosen()     != null) return "Dosen #"     + p.getDosen().getId();
        if (p.getPegawai()   != null) return "Pegawai #"   + p.getPegawai().getId();
        return "";
    }

    private String orangLabel(KartuPenjemputAntarJemput k) {
        if (k.getSiswa()     != null) return "Siswa #"     + k.getSiswa().getId();
        if (k.getMahasiswa() != null) return "Mahasiswa #" + k.getMahasiswa().getId();
        if (k.getGuru()      != null) return "Guru #"      + k.getGuru().getId();
        if (k.getDosen()     != null) return "Dosen #"     + k.getDosen().getId();
        if (k.getPegawai()   != null) return "Pegawai #"   + k.getPegawai().getId();
        return "";
    }

    // ===========================================================================
    // FORM INPUT BUILDERS / READERS
    // ===========================================================================

    private void addText(Rows rows, String key, String label, String value, boolean multi) {
        Row row = formRow(rows, label);
        Textbox tb = new Textbox(value != null ? value : "");
        tb.setWidth("96%");
        if (multi) tb.setRows(3);
        tb.setParent(row);
        inputs.put(key, tb);
    }

    private void addLong(Rows rows, String key, String label, Long value) {
        Row row = formRow(rows, label);
        Longbox lb = new Longbox(value);
        lb.setWidth("96%");
        lb.setParent(row);
        inputs.put(key, lb);
    }

    private void addInt(Rows rows, String key, String label, Integer value) {
        Row row = formRow(rows, label);
        Intbox ib = new Intbox(value);
        ib.setWidth("96%");
        ib.setParent(row);
        inputs.put(key, ib);
    }

    private void addDate(Rows rows, String key, String label, Date value) {
        Row row = formRow(rows, label);
        MyDatebox db = new MyDatebox();
        db.setValue(value);
        db.setWidth("96%");
        db.setParent(row);
        inputs.put(key, db);
    }

    private void addTime(Rows rows, String key, String label, Date value) {
        Row row = formRow(rows, label);
        Timebox tb = new Timebox();
        tb.setValue(value);
        tb.setWidth("96%");
        tb.setParent(row);
        inputs.put(key, tb);
    }

    private void addBool(Rows rows, String key, String label, Boolean value) {
        Row row = formRow(rows, label);
        Checkbox cb = new Checkbox(label);
        cb.setChecked(value == null || value.booleanValue());
        cb.setParent(row);
        inputs.put(key, cb);
    }

    private Row formRow(Rows rows, String label) {
        Row row = new Row();
        row.setValign("top");
        row.setSclass("aj-form-row ais-row-styled");
        row.setParent(rows);
        row.appendChild(new MyLabelConfig(label));
        return row;
    }

    private String text(String key)    { Component c = inputs.get(key); return c instanceof Textbox  ? ((Textbox) c).getValue()  : ""; }
    private Long   longValue(String key){ Component c = inputs.get(key); return c instanceof Longbox  ? ((Longbox) c).getValue()  : null; }
    private Integer intValue(String key){ Component c = inputs.get(key); return c instanceof Intbox   ? ((Intbox) c).getValue()   : null; }
    private Boolean bool(String key)    { Component c = inputs.get(key); return c instanceof Checkbox ? Boolean.valueOf(((Checkbox) c).isChecked()) : Boolean.FALSE; }

    private Date date(String key) {
        Component c = inputs.get(key);
        if (c instanceof Datebox) return ((Datebox) c).getValue();
        if (c instanceof Timebox) return ((Timebox) c).getValue();
        return null;
    }

    // ===========================================================================
    // STRING UTILS
    // ===========================================================================

    private String formatDate(Date date) {
        try { return date != null ? Common.dateFormat.get().format(date) : ""; } catch (Exception e) { return ""; }
    }

    private String formatDateTime(Date date) {
        try { return date != null ? Common.datetimeFormat2s.get().format(date) : ""; } catch (Exception e) { return ""; }
    }

    private String jam(Date date) {
        try { return date != null ? Common.timeFormat.get().format(date) : ""; } catch (Exception e) { return ""; }
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void setScanStatus(String value, boolean ok) {
        if (scanStatus == null) return;
        scanStatus.setValue(value);
        scanStatus.setStyle(ok ? "font-weight:600;color:#166534;" : "font-weight:600;color:#991b1b;");
    }

    private void warn(String message) {
        try {
            MyMessageboxConfig.show(message, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
