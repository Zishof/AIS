package ais.action.master.helper;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * Generic helper untuk menampilkan, mencari, membandingkan, dan merestore data revisi Envers.
 *
 * Tujuan utama class ini:
 * 1. Menghilangkan duplikasi code pada banyak RevisiXXXHelper lama.
 * 2. Semua akses Hibernate memakai openSession() lokal dan ditutup di finally.
 * 3. Filter pencarian bisa disusun melalui parameter konstruktor.
 * 4. Mendukung restore satu revisi dan restore massal data terbaru dari revisi mulai tanggal tertentu.
 *
 * Kompatibel Java 1.7 dan gaya try-catch Java 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericRevisiHelper<T extends Serializable> extends MyWindow {

    private static final long serialVersionUID = 7629455006771214871L;

    public static final int MODE_SEMUA = 0;
    public static final int MODE_TAMBAH = 1;
    public static final int MODE_UBAH = 2;
    public static final int MODE_HAPUS = 3;

    private static final int PAGE_SIZE = 10;
    private static final int DEFAULT_ALL_DATA_MONTHS = 6;
    private static final int DEFAULT_CURRENT_LIMIT = 1000;
    private static final int DEFAULT_ALL_DATA_LIMIT = 1500;
    private static final int DEFAULT_DASHBOARD_ANALYSIS_LIMIT = 350;
    private static final long COUNT_CACHE_TTL_MS = 120000L;
    // LRU BER-BATAS (optimasi RAM Fase 2): TTL hanya dicek saat READ key yang sama —
    // entry kadaluarsa milik kombinasi filter yang tidak pernah diakses lagi sebelumnya
    // menumpuk selamanya. Batas 2000 kombinasi filter terakhir (value hanya 2 long).
    private static final Map<String, CountCacheEntry> COUNT_CACHE = java.util.Collections
            .synchronizedMap(new java.util.LinkedHashMap<String, CountCacheEntry>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, CountCacheEntry> eldest) {
                    return size() > 2000;
                }
            });

    private static class CountCacheEntry {
        private long value;
        private long createdAt;

        private CountCacheEntry(long value, long createdAt) {
            this.value = value;
            this.createdAt = createdAt;
        }
    }

    public static interface QueryCustomizer {
        void apply(Session session, AuditQuery query) throws Exception;
    }

    public static class FixedPropertyFilter implements QueryCustomizer {
        private String property;
        private Object value;

        public FixedPropertyFilter(String property, Object value) {
            this.property = property;
            this.value = value;
        }

        public void apply(Session session, AuditQuery query) throws Exception {
            if (property == null || property.trim().length() == 0 || value == null) {
                return;
            }
            query.add(AuditEntity.property(property).eq(value));
        }

        public String toString() {
            return "FixedPropertyFilter(" + property + "=" + (value == null ? "" : value.toString()) + ")";
        }
    }

    public static class EntityIdFilter implements QueryCustomizer {
        private Serializable id;

        public EntityIdFilter(Serializable id) {
            this.id = id;
        }

        public Serializable getId() {
            return id;
        }

        public void apply(Session session, AuditQuery query) throws Exception {
            if (id != null) {
                query.add(AuditEntity.id().eq(id));
            }
        }

        public String toString() {
            return "EntityIdFilter(" + (id == null ? "" : id.toString()) + ")";
        }
    }

    protected final Class entityClass;
    protected final String titleText;
    protected final EventListener callback;
    protected final String[] searchProperties;
    protected final QueryCustomizer[] customizers;

    protected MyGrid grid;
    protected Textbox keyword;
    protected MyDatebox mulai;
    protected MyDatebox sampai;
    protected Combobox tipeRevisi;
    protected Checkbox hanyaTampilYangBerubah;
    protected Checkbox hanyaTampilYangDihapus;
    protected Combobox filterKolomCari;
    protected Textbox nilaiKolomCari;
    /** Filter "kolom berubah" (tab Riwayat ID Ini): tampilkan hanya revisi yang mengubah kolom terpilih. */
    protected Combobox filterKolomBerubah;
    protected MyDatebox mulaiRestore;
    protected ClassMetadata classMetadata;
    protected List<String> propertyNames = new ArrayList<String>();
    protected Map<String, String> comparisonCache = new HashMap<String, String>();

    protected org.zkoss.zul.Div dashboardCurrentContainer;
    protected org.zkoss.zul.Div dashboardAllContainer;
    protected org.zkoss.zul.Div currentLoadingContainer;
    protected org.zkoss.zul.Div currentHistoryLoadingContainer;
    protected org.zkoss.zul.Div allLoadingContainer;
    protected volatile boolean currentSearchRunning = false;
    protected volatile boolean allSearchRunning = false;
    protected MyGrid allGrid;
    /* Paging langsung ke database (10 baris per halaman) untuk tab
     * "Riwayat ID Ini" dan "Seluruh Data Revisi". */
    protected org.zkoss.zul.Paging pagingDb;
    protected org.zkoss.zul.Paging pagingDbAll;
    private static final int UKURAN_HALAMAN_DB = 10;
    protected Textbox allKeyword;
    protected MyDatebox allMulai;
    protected MyDatebox allSampai;
    protected Combobox allTipeRevisi;
    protected Checkbox allHanyaTampilYangBerubah;
    protected Checkbox allHanyaTampilYangDihapus;
    protected Combobox allFilterKolomCari;
    protected Textbox allNilaiKolomCari;
    protected Combobox allFilterKolomBerubah;
    protected org.zkoss.zul.Tabbox mainTabbox;

    public GenericRevisiHelper(Class entityClass, String titleText, EventListener callback, String[] searchProperties,
            QueryCustomizer... customizers) throws Exception {
        super();
        this.entityClass = entityClass;
        this.titleText = titleText == null || titleText.trim().length() == 0 ? "Revisi Data" : titleText;
        this.callback = callback;
        this.searchProperties = searchProperties == null ? new String[0] : searchProperties;
        this.customizers = customizers == null ? new QueryCustomizer[0] : customizers;
        init();
    }

    protected void init() throws Exception {
        setTitle(titleText);
        setWidth("96%");
        setHeight("94%");
        setClosable(true);
        setSizable(true);
        setMaximizable(true);
        setBorder("normal");
        try {
            setContentStyle("overflow:hidden; padding:0;");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:198");
        }
        Common.clear(this);

        loadEntityMetadata();

        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        borderlayout.setWidth("100%");
        borderlayout.setHeight("100%");
        borderlayout.setParent(this);

        Center center = new Center();
        ais.ui.util.ZkCompat.setFlex(center, true);
        center.setBorder("none");
        try {
            center.setAutoscroll(true);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:214");
        }
        center.setParent(borderlayout);

        org.zkoss.zul.Tabbox tabbox = new org.zkoss.zul.Tabbox();
        mainTabbox = tabbox;
        tabbox.setWidth("100%");
        tabbox.setHeight("100%");
        tabbox.setStyle("overflow:hidden; background:#f6f8fb; max-width:100%;");
        tabbox.setParent(center);

        org.zkoss.zul.Tabs tabs = new org.zkoss.zul.Tabs();
        tabs.setParent(tabbox);
        org.zkoss.zul.Tab tabDashboard = new org.zkoss.zul.Tab("Dasbor Data Ini");
        tabDashboard.setTooltiptext("Ringkasan class dan ID yang sedang dilihat. Progress loading juga ditampilkan di tab ini agar mudah terlihat.");
        tabDashboard.setParent(tabs);
        org.zkoss.zul.Tab tabRiwayat = new org.zkoss.zul.Tab("Riwayat ID Ini");
        tabRiwayat.setParent(tabs);
        org.zkoss.zul.Tab tabSemua = new org.zkoss.zul.Tab("Seluruh Data Revisi");
        tabSemua.setTooltiptext("Menampilkan riwayat revisi seluruh ID pada class yang sama. Rentang tanggal wajib diisi agar data yang dimuat tetap ringan.");
        tabSemua.setParent(tabs);

        org.zkoss.zul.Tabpanels tabpanels = new org.zkoss.zul.Tabpanels();
        tabpanels.setHeight("100%");
        tabpanels.setStyle("overflow:hidden; background:#f6f8fb; max-width:100%;");
        tabpanels.setParent(tabbox);

        org.zkoss.zul.Tabpanel panelDashboard = new org.zkoss.zul.Tabpanel();
        panelDashboard.setHeight("100%");
        panelDashboard.setStyle("padding:0; background:#f6f8fb; overflow-x:hidden; overflow-y:auto; position:relative; max-width:100%;");
        panelDashboard.setParent(tabpanels);

        org.zkoss.zul.Div dashboardShell = new org.zkoss.zul.Div();
        dashboardShell.setWidth("100%");
        dashboardShell.setStyle("padding:14px 14px 78px 14px; box-sizing:border-box; background:#f6f8fb; min-height:100%; overflow-x:hidden; overflow-y:visible; max-width:100%;");
        dashboardShell.setParent(panelDashboard);

        currentLoadingContainer = createLoadingContainer(dashboardShell);

        dashboardCurrentContainer = new org.zkoss.zul.Div();
        dashboardCurrentContainer.setWidth("100%");
        dashboardCurrentContainer.setStyle("box-sizing:border-box; background:#f6f8fb; overflow-x:hidden; overflow-y:visible; max-width:100%;");
        dashboardCurrentContainer.setParent(dashboardShell);

        org.zkoss.zul.Tabpanel panelRiwayat = new org.zkoss.zul.Tabpanel();
        panelRiwayat.setHeight("100%");
        panelRiwayat.setStyle("padding:0; background:#f6f8fb; overflow-x:hidden; overflow-y:auto; position:relative; max-width:100%;");
        panelRiwayat.setParent(tabpanels);
        Vbox riwayatBox = new Vbox();
        riwayatBox.setWidth("100%");
        riwayatBox.setHeight("100%");
        riwayatBox.setStyle("padding:12px 12px 78px 12px; box-sizing:border-box; background:#f6f8fb; overflow-x:hidden; overflow-y:visible; min-height:100%; max-width:100%;");
        riwayatBox.setParent(panelRiwayat);
        renderCurrentHistoryFilter(riwayatBox);
        currentHistoryLoadingContainer = createLoadingContainer(riwayatBox);
        pagingDb = buatPagingDb(riwayatBox, false);
        grid = createRevisionGrid(riwayatBox);

        org.zkoss.zul.Tabpanel panelSemua = new org.zkoss.zul.Tabpanel();
        panelSemua.setHeight("100%");
        panelSemua.setStyle("padding:0; background:#f6f8fb; overflow-x:hidden; overflow-y:auto; position:relative; max-width:100%;");
        panelSemua.setParent(tabpanels);
        Vbox semuaBox = new Vbox();
        semuaBox.setWidth("100%");
        semuaBox.setHeight("100%");
        semuaBox.setStyle("padding:12px 12px 78px 12px; box-sizing:border-box; background:#f6f8fb; overflow-x:hidden; overflow-y:visible; min-height:100%; max-width:100%;");
        semuaBox.setParent(panelSemua);
        renderAllDataFilter(semuaBox);
        allLoadingContainer = createLoadingContainer(semuaBox);
        dashboardAllContainer = new org.zkoss.zul.Div();
        dashboardAllContainer.setWidth("100%");
        dashboardAllContainer.setStyle("margin-top:10px; width:100%; overflow-x:hidden; overflow-y:visible; max-width:100%;");
        dashboardAllContainer.setParent(semuaBox);
        pagingDbAll = buatPagingDb(semuaBox, true);
        allGrid = createRevisionGrid(semuaBox);

        South south = new South();
        south.setBorder("none");
        south.setSize("34px");
        south.setParent(borderlayout);
        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);
        MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
        tutup.setParent(toolbar);
        tutup.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                detach();
            }
        });

        onSearchDefault(null);
        renderDashboard(dashboardAllContainer, new RevisionDashboardData(), true);
        setGridData(allGrid, new ArrayList(), new DataRenderer());
    }

    private void loadEntityMetadata() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            classMetadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
            propertyNames.clear();
            if (classMetadata != null) {
                String idName = classMetadata.getIdentifierPropertyName();
                if (idName != null) {
                    propertyNames.add(idName);
                }
                String[] props = classMetadata.getPropertyNames();
                if (props != null) {
                    for (int i = 0; i < props.length; i++) {
                        propertyNames.add(props[i]);
                    }
                }
            }
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:330");
            }
        } finally {
            closeSession(session);
        }
    }

    private void renderCurrentHistoryFilter(Component parent) {
        org.zkoss.zul.Div filter = createFilterShell(parent,
                "Riwayat revisi ID terpilih",
                "Menampilkan perubahan untuk data yang sedang dibuka. Gunakan filter agar daftar lebih singkat dan mudah dibaca.");

        Hbox hbox1 = new Hbox();
        hbox1.setWidth("100%");
        hbox1.setStyle("gap:8px; flex-wrap:wrap; align-items:center;");
        hbox1.setParent(filter);
        hbox1.appendChild(new Label(ais.common.Common.getBahasaConfig("Kata kunci")));
        keyword = new Textbox();
        keyword.setWidth("240px");
        keyword.setParent(hbox1);

        hbox1.appendChild(new Label(ais.common.Common.getBahasaConfig("Mulai")));
        mulai = new MyDatebox(WaktuUtil.getDate());
        mulai.setWidth("120px");
        mulai.setTooltiptext("Boleh dikosongkan untuk melihat semua riwayat data ini.");
        try { mulai.setValue(null); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:355");}
        mulai.setParent(hbox1);

        hbox1.appendChild(new Label(ais.common.Common.getBahasaConfig("Sampai")));
        sampai = new MyDatebox(WaktuUtil.getDate());
        sampai.setWidth("120px");
        sampai.setTooltiptext("Boleh dikosongkan untuk melihat semua riwayat data ini.");
        try { sampai.setValue(null); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:362");}
        sampai.setParent(hbox1);

        hbox1.appendChild(new Label(ais.common.Common.getBahasaConfig("Tipe")));
        tipeRevisi = newRevisionTypeCombo();
        tipeRevisi.setParent(hbox1);

        hanyaTampilYangBerubah = new Checkbox("Hanya yang berubah");
        hanyaTampilYangBerubah.setTooltiptext("Menampilkan revisi yang benar-benar memiliki perbedaan terhadap revisi sebelumnya.");
        hanyaTampilYangBerubah.setStyle("font-size:11px;font-weight:bold;color:#334155;margin-left:4px;");
        hanyaTampilYangBerubah.setParent(hbox1);
        hanyaTampilYangBerubah.addEventListener(Events.ON_CHECK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });

        hanyaTampilYangDihapus = new Checkbox("Hanya yang dihapus");
        hanyaTampilYangDihapus.setTooltiptext("Menampilkan revisi dengan aksi hapus saja.");
        hanyaTampilYangDihapus.setStyle("font-size:11px;font-weight:bold;color:#b91c1c;margin-left:4px;");
        hanyaTampilYangDihapus.setParent(hbox1);
        hanyaTampilYangDihapus.addEventListener(Events.ON_CHECK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });

        hbox1.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari kolom")));
        filterKolomCari = newPropertyCombo("Semua kolom revisi");
        filterKolomCari.setParent(hbox1);
        nilaiKolomCari = new Textbox();
        nilaiKolomCari.setWidth("170px");
        nilaiKolomCari.setTooltiptext("Isi nilai kolom yang ingin dicari. Kosongkan untuk memakai kata kunci biasa.");
        nilaiKolomCari.setParent(hbox1);
        filterKolomCari.addEventListener(Events.ON_CHANGE, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });
        nilaiKolomCari.addEventListener("onOK", new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });

        // Filter "kolom berubah": daftar kolom diambil dari class Model (propertyNames = ID + seluruh
        // property Hibernate). Bila dipilih satu kolom, hanya revisi yang benar-benar mengubah kolom
        // tersebut yang ditampilkan (memakai collectChangedPropertyNames yang sama dgn dashboard).
        hbox1.appendChild(new Label(ais.common.Common.getBahasaConfig("Kolom berubah")));
        filterKolomBerubah = newChangedColumnCombo();
        filterKolomBerubah.setTooltiptext("Tampilkan hanya revisi yang mengubah kolom tertentu. Daftar kolom diambil dari class Model.");
        filterKolomBerubah.addEventListener(Events.ON_CHANGE, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });
        filterKolomBerubah.setParent(hbox1);

        MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/search.png");
        cari.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px;");
        cari.setParent(hbox1);
        cari.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });
        keyword.addEventListener("onOK", new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchDefault(event);
            }
        });

        MyToolbarbuttonConfig downloadSemua = new MyToolbarbuttonConfig("Download Semua", "/img/excel.png");
        downloadSemua.setTooltiptext("Unduh semua revisi data ini sesuai filter (CSV, bisa dibuka di Excel)");
        downloadSemua.setStyle("font-weight:bold; margin-left:4px;");
        downloadSemua.setParent(hbox1);
        downloadSemua.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                downloadSemuaRevisi(false);
            }
        });

        MyToolbarbuttonConfig analisisAi = new MyToolbarbuttonConfig("Analisis oleh AI", "/img/search.png");
        analisisAi.setTooltiptext("Salin PERINTAH + DATA revisi untuk dianalisis AI (tempel ke ChatGPT/Claude/Gemini)");
        analisisAi.setStyle("font-weight:bold; color:#ffffff; background:#7c3aed; border-radius:10px; padding:6px 14px; margin-left:4px;");
        analisisAi.setParent(hbox1);
        analisisAi.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                tampilkanAnalisisAi(false);
            }
        });

        MyToolbarbuttonConfig downloadAi = new MyToolbarbuttonConfig("Download Perintah AI", "/img/excel.png");
        downloadAi.setTooltiptext("Unduh berkas .md berisi perintah + data revisi untuk dianalisis AI");
        downloadAi.setStyle("font-weight:bold; margin-left:4px;");
        downloadAi.setParent(hbox1);
        downloadAi.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                downloadPerintahAi(false);
            }
        });

        Hbox hbox2 = new Hbox();
        hbox2.setWidth("100%");
        hbox2.setStyle("gap:8px; flex-wrap:wrap; align-items:center; margin-top:8px;");
        hbox2.setParent(filter);
        hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Restore data terbaru mulai tanggal")));
        mulaiRestore = new MyDatebox(addDays(WaktuUtil.getDate(), -7));
        mulaiRestore.setWidth("120px");
        mulaiRestore.setTooltiptext("Restore terbaru diproses per ID agar setiap data kembali ke revisi paling baru dalam rentang tanggal ini.");
        try { mulaiRestore.setReadonly(true); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:431");}
        try { mulaiRestore.setConstraint("no empty"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:432");}
        mulaiRestore.setParent(hbox2);

        MyToolbarbuttonConfig restoreMassal = new MyToolbarbuttonConfig("Restore Terbaru", "/img/refresh.gif");
        restoreMassal.setTooltiptext("Restore semua data terbaru dari revisi mulai tanggal yang dipilih");
        restoreMassal.setParent(hbox2);
        restoreMassal.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                restoreLatestFromDateWithConfirm();
            }
        });
    }

    private void renderAllDataFilter(Component parent) {
        org.zkoss.zul.Div filter = createFilterShell(parent,
                "Seluruh data revisi",
                "Melihat perubahan semua ID pada class yang sama. Rentang tanggal wajib diisi supaya data yang dibuka tidak terlalu banyak.");

        Hbox hbox = new Hbox();
        hbox.setWidth("100%");
        hbox.setStyle("gap:8px; flex-wrap:wrap; align-items:center;");
        hbox.setParent(filter);
        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Kata kunci")));
        allKeyword = new Textbox();
        allKeyword.setWidth("220px");
        allKeyword.setParent(hbox);

        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Mulai")));
        allMulai = new MyDatebox(addMonths(WaktuUtil.getDate(), -DEFAULT_ALL_DATA_MONTHS));
        allMulai.setWidth("120px");
        allMulai.setTooltiptext("Wajib diisi. Default menampilkan 6 bulan terakhir.");
        try { allMulai.setReadonly(true); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:463");}
        try { allMulai.setConstraint("no empty"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:464");}
        allMulai.setParent(hbox);

        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Sampai")));
        allSampai = new MyDatebox(WaktuUtil.getDate());
        allSampai.setWidth("120px");
        allSampai.setTooltiptext("Wajib diisi. Default sampai hari ini.");
        try { allSampai.setReadonly(true); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:471");}
        try { allSampai.setConstraint("no empty"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:472");}
        allSampai.setParent(hbox);

        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tipe")));
        allTipeRevisi = newRevisionTypeCombo();
        allTipeRevisi.setParent(hbox);

        allHanyaTampilYangBerubah = new Checkbox("Hanya yang berubah");
        allHanyaTampilYangBerubah.setStyle("font-size:11px;font-weight:bold;color:#334155;margin-left:4px;");
        allHanyaTampilYangBerubah.setParent(hbox);
        allHanyaTampilYangBerubah.addEventListener(Events.ON_CHECK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });

        allHanyaTampilYangDihapus = new Checkbox("Hanya yang dihapus");
        allHanyaTampilYangDihapus.setTooltiptext("Menampilkan revisi dengan aksi hapus saja.");
        allHanyaTampilYangDihapus.setStyle("font-size:11px;font-weight:bold;color:#b91c1c;margin-left:4px;");
        allHanyaTampilYangDihapus.setParent(hbox);
        allHanyaTampilYangDihapus.addEventListener(Events.ON_CHECK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });

        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari kolom")));
        allFilterKolomCari = newPropertyCombo("Semua kolom revisi");
        allFilterKolomCari.setParent(hbox);
        allNilaiKolomCari = new Textbox();
        allNilaiKolomCari.setWidth("170px");
        allNilaiKolomCari.setTooltiptext("Isi nilai kolom yang ingin dicari. Kosongkan untuk memakai kata kunci biasa.");
        allNilaiKolomCari.setParent(hbox);
        allFilterKolomCari.addEventListener(Events.ON_CHANGE, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });
        allNilaiKolomCari.addEventListener("onOK", new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });

        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Kolom berubah")));
        allFilterKolomBerubah = newChangedColumnCombo();
        allFilterKolomBerubah.setTooltiptext("Tampilkan hanya revisi yang mengubah kolom tertentu. Daftar kolom diambil dari class Model.");
        allFilterKolomBerubah.setParent(hbox);
        allFilterKolomBerubah.addEventListener(Events.ON_CHANGE, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });

        MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Tampilkan", "/img/search.png");
        cari.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px;");
        cari.setParent(hbox);
        cari.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });
        allKeyword.addEventListener("onOK", new EventListener() {
            public void onEvent(Event event) throws Exception {
                onSearchAllData(event);
            }
        });

        MyToolbarbuttonConfig downloadSemuaAll = new MyToolbarbuttonConfig("Download Semua", "/img/excel.png");
        downloadSemuaAll.setTooltiptext("Unduh semua revisi sesuai filter (CSV, bisa dibuka di Excel)");
        downloadSemuaAll.setStyle("font-weight:bold; margin-left:4px;");
        downloadSemuaAll.setParent(hbox);
        downloadSemuaAll.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                downloadSemuaRevisi(true);
            }
        });

        MyToolbarbuttonConfig analisisAi = new MyToolbarbuttonConfig("Analisis oleh AI", "/img/search.png");
        analisisAi.setTooltiptext("Salin PERINTAH + DATA revisi untuk dianalisis AI (tempel ke ChatGPT/Claude/Gemini)");
        analisisAi.setStyle("font-weight:bold; color:#ffffff; background:#7c3aed; border-radius:10px; padding:6px 14px; margin-left:4px;");
        analisisAi.setParent(hbox);
        analisisAi.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                tampilkanAnalisisAi(true);
            }
        });

        MyToolbarbuttonConfig downloadAi = new MyToolbarbuttonConfig("Download Perintah AI", "/img/excel.png");
        downloadAi.setTooltiptext("Unduh berkas .md berisi perintah + data revisi untuk dianalisis AI");
        downloadAi.setStyle("font-weight:bold; margin-left:4px;");
        downloadAi.setParent(hbox);
        downloadAi.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                downloadPerintahAi(true);
            }
        });
    }

    private org.zkoss.zul.Div createFilterShell(Component parent, String title, String desc) {
        org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
        shell.setWidth("100%");
        shell.setStyle("padding:14px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; "
                + "box-shadow:0 10px 24px rgba(15,23,42,.04); box-sizing:border-box; margin-bottom:10px;");
        shell.setParent(parent);
        appendHtml(shell, "<div style='font-size:14px; font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:#64748b; margin-top:4px; line-height:1.5;'>" + escapeHtml(desc) + "</div>");
        return shell;
    }

    private Combobox newRevisionTypeCombo() {
        Combobox combo = new Combobox();
        combo.setReadonly(true);
        appendCombo(combo, "Semua", Integer.valueOf(MODE_SEMUA));
        appendCombo(combo, "Tambah", Integer.valueOf(MODE_TAMBAH));
        appendCombo(combo, "Ubah", Integer.valueOf(MODE_UBAH));
        appendCombo(combo, "Hapus", Integer.valueOf(MODE_HAPUS));
        combo.setSelectedIndex(0);
        combo.setWidth("120px");
        return combo;
    }

    private MyGrid createRevisionGrid(Component parent) {
        MyGrid result = new MyGrid();
        result.setParent(parent);
        result.setWidth("100%");
        result.setHeight("100%");
        try { result.setVflex("1"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:561");}
        result.setStyle("min-height:500px; max-width:100%; overflow-x:hidden; overflow-y:auto; box-sizing:border-box;");
        result.setSclass("fgrid");
        result.setMold("paging");
        result.setPageSize(PAGE_SIZE);
        try { result.getPagingChild().setMold("os"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:566");}

        Columns columns = new Columns();
        columns.setParent(result);
        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("");
        column.setWidth("4%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Tanggal Revisi");
        column.setWidth("12%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Aksi");
        column.setWidth(ais.ui.util.GridKolomHelper.LEBAR_KOLOM_AKSI);
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Ringkasan Data");
        column.setWidth("28%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Perubahan Dari Data Sebelumnya");
        column.setWidth("18%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Oleh / Class Pengubah");
        column.setWidth("16%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Aksi Data");
        column.setWidth(ais.ui.util.GridKolomHelper.LEBAR_KOLOM_AKSI);
        return result;
    }

    private void appendCombo(Combobox combo, String label, Object value) {
        Comboitem item = new Comboitem(label);
        item.setValue(value);
        combo.appendChild(item);
    }

    private Combobox newPropertyCombo(String allLabel) {
        Combobox combo = new Combobox();
        combo.setReadonly(true);
        combo.setWidth("190px");
        combo.setTooltiptext("Daftar kolom diambil dari semua property object revisi.");
        appendCombo(combo, ais.common.Common.getBahasaConfig("(" + allLabel + ")"), null);
        List<String> kolomTerurut = getSortedPropertyNames(false);
        for (int i = 0; i < kolomTerurut.size(); i++) {
            appendCombo(combo, kolomTerurut.get(i), kolomTerurut.get(i));
        }
        combo.setSelectedIndex(0);
        return combo;
    }

    private Combobox newChangedColumnCombo() {
        Combobox combo = new Combobox();
        combo.setReadonly(true);
        combo.setWidth("200px");
        appendCombo(combo, ais.common.Common.getBahasaConfig("(Semua kolom)"), null);
        List<String> kolomTerurut = getSortedPropertyNames(true);
        for (int i = 0; i < kolomTerurut.size(); i++) {
            appendCombo(combo, kolomTerurut.get(i), kolomTerurut.get(i));
        }
        combo.setSelectedIndex(0);
        return combo;
    }

    private List<String> getSortedPropertyNames(boolean skipIgnoredComparison) {
        List<String> kolomTerurut = new ArrayList<String>();
        for (int i = 0; i < propertyNames.size(); i++) {
            String p = propertyNames.get(i);
            if (p == null || p.trim().length() == 0) {
                continue;
            }
            p = p.trim();
            if (skipIgnoredComparison && isIgnoredComparisonProperty(p)) {
                continue;
            }
            if (!kolomTerurut.contains(p)) {
                kolomTerurut.add(p);
            }
        }
        java.util.Collections.sort(kolomTerurut, String.CASE_INSENSITIVE_ORDER);
        return kolomTerurut;
    }

    public void onSearchDefault(Event event) throws Exception {
        resetPagingDb(false);
        startRevisionLoad(false);
    }

    public void onSearchAllData(Event event) throws Exception {
        resetPagingDb(true);
        startRevisionLoad(true);
    }

    private org.zkoss.zul.Paging buatPagingDb(Component parent, final boolean allDataMode) {
        org.zkoss.zul.Paging paging = new org.zkoss.zul.Paging();
        paging.setMold("os");
        paging.setDetailed(true);
        paging.setPageSize(UKURAN_HALAMAN_DB);
        paging.setVisible(false);
        paging.setParent(parent);
        paging.addEventListener("onPaging", new EventListener() {
            public void onEvent(Event event) throws Exception {
                org.zkoss.zul.Paging sumber = allDataMode ? pagingDbAll : pagingDb;
                int halaman = sumber == null ? 0 : sumber.getActivePage();
                startRevisionLoad(allDataMode, halaman * UKURAN_HALAMAN_DB);
            }
        });
        return paging;
    }

    private void resetPagingDb(boolean allDataMode) {
        try {
            org.zkoss.zul.Paging paging = allDataMode ? pagingDbAll : pagingDb;
            if (paging != null && paging.getActivePage() != 0) {
                paging.setActivePage(0);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:640");
        }
    }

    private void startRevisionLoad(final boolean allDataMode) throws Exception {
        startRevisionLoad(allDataMode, 0);
    }

    private void startRevisionLoad(final boolean allDataMode, final int barisAwal) throws Exception {
        if (allDataMode) {
            Date m = allMulai == null ? null : allMulai.getValue();
            Date s = allSampai == null ? null : allSampai.getValue();
            if (m == null || s == null) {
                MyMessageboxConfig.show("Rentang waktu wajib diisi agar data revisi seluruh ID tidak terlalu banyak.");
                renderDashboard(dashboardAllContainer, new RevisionDashboardData(), true);
                setGridData(allGrid, new ArrayList(), new DataRenderer());
                return;
            }
            if (allSearchRunning) {
                MyMessageboxConfig.show("Proses pencarian seluruh data revisi masih berjalan. Mohon tunggu sampai progress selesai.");
                return;
            }
            allSearchRunning = true;
        } else {
            if (currentSearchRunning) {
                MyMessageboxConfig.show("Proses pencarian riwayat ID ini masih berjalan. Mohon tunggu sampai progress selesai.");
                return;
            }
            currentSearchRunning = true;
        }

        final RevisionLoadContext ctx = new RevisionLoadContext();
        ctx.allDataMode = allDataMode;
        /* Paging DB: tiap halaman hanya menarik 10 baris dari database. */
        ctx.limit = UKURAN_HALAMAN_DB;
        ctx.first = barisAwal < 0 ? 0 : barisAwal;
        ctx.loadingContainer = resolveLoadingContainer(allDataMode);
        ctx.dashboardContainer = allDataMode ? dashboardAllContainer : dashboardCurrentContainer;
        ctx.targetGrid = allDataMode ? allGrid : grid;
        ctx.onlyChangedCheckbox = allDataMode ? allHanyaTampilYangBerubah : hanyaTampilYangBerubah;
        ctx.title = allDataMode ? "Memuat Seluruh Data Revisi" : "Memuat Riwayat Revisi ID Ini";
        ctx.selectedTabIndex = getSelectedTabIndex();
        showLoadProgress(ctx, 4, "Menyiapkan filter dan area tampilan. Tab yang sedang dibuka tetap dipertahankan.", 0, 0);
        runRevisionLoadStage(ctx, 1);
    }

    private org.zkoss.zul.Div resolveLoadingContainer(boolean allDataMode) {
        if (allDataMode) {
            return allLoadingContainer == null ? currentLoadingContainer : allLoadingContainer;
        }
        int selectedIndex = getSelectedTabIndex();
        if (selectedIndex == 1 && currentHistoryLoadingContainer != null) {
            return currentHistoryLoadingContainer;
        }
        return currentLoadingContainer == null ? currentHistoryLoadingContainer : currentLoadingContainer;
    }

    private int getSelectedTabIndex() {
        try {
            if (mainTabbox != null) {
                return mainTabbox.getSelectedIndex();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:702");
        }
        return 0;
    }

    private void keepSelectedTab(RevisionLoadContext ctx) {
        if (ctx == null || mainTabbox == null) {
            return;
        }
        try {
            int currentIndex = mainTabbox.getSelectedIndex();
            if (ctx.selectedTabIndex >= 0 && currentIndex != ctx.selectedTabIndex) {
                mainTabbox.setSelectedIndex(ctx.selectedTabIndex);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:716");
        }
    }

    private void runRevisionLoadStage(final RevisionLoadContext ctx, final int stage) {
        try {
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event event) throws Exception {
                    try {
                        keepSelectedTab(ctx);
                        if (stage == 1) {
                            showLoadProgress(ctx, 12, "Membuka koneksi aman dan menyiapkan query revisi.", 0, 0);
                            ctx.session = HibernateUtil.getSessionFactory().openSession();
                            ctx.countCacheKey = buildCountCacheKey(ctx.allDataMode);
                            ctx.total = hitungTotalRevisiDenganCache(ctx.session, ctx.allDataMode, ctx.countCacheKey);
                            ctx.totalEstimasi = ctx.total < 0L;
                            ctx.query = ctx.allDataMode
                                    ? buildAuditQuery(ctx.session, true, allKeyword, allMulai, allSampai, allTipeRevisi,
                                            allHanyaTampilYangDihapus, allFilterKolomCari, allNilaiKolomCari)
                                    : buildAuditQuery(ctx.session);
                            ctx.query.setFirstResult(ctx.first);
                            ctx.query.setMaxResults(ctx.limit);
                            runRevisionLoadStage(ctx, 2);
                        } else if (stage == 2) {
                            showLoadProgress(ctx, 28, "Mengambil data revisi dari database. Batas aman: " + ctx.limit + " baris.", 0, ctx.limit);
                            ctx.rawList = ctx.query.getResultList();
                            int loaded = ctx.rawList == null ? 0 : ctx.rawList.size();
                            if (ctx.total < 0L) {
                                ctx.total = ctx.first + loaded + (loaded >= ctx.limit ? 1 : 0);
                                ctx.totalEstimasi = true;
                            }
                            showLoadProgress(ctx, 48, "Data revisi terbaca " + loaded + " baris. Menyiapkan isi data.", loaded, ctx.limit);
                            runRevisionLoadStage(ctx, 3);
                        } else if (stage == 3) {
                            int loaded = ctx.rawList == null ? 0 : ctx.rawList.size();
                            showLoadProgress(ctx, 62, "Membaca nilai relasi yang diperlukan agar detail aman ditampilkan.", loaded, ctx.limit);
                            eagerInitialize(ctx.session, ctx.rawList);
                            runRevisionLoadStage(ctx, 4);
                        } else if (stage == 4) {
                            int loaded = ctx.rawList == null ? 0 : ctx.rawList.size();
                            showLoadProgress(ctx, 76, "Menyusun card, trend, komposisi, dan spider web revisi.", loaded, ctx.limit);
                            if (comparisonCache != null) {
                                comparisonCache.clear();
                            }
                            ctx.dashboardData = buildDashboardData(ctx.session, ctx.rawList, true);
                            /* Angka total mengikuti hitungan database, bukan
                             * jumlah baris halaman aktif. */
                            if (ctx.dashboardData != null && ctx.total > 0) {
                                ctx.dashboardData.totalRevisi = ctx.total > Integer.MAX_VALUE ? Integer.MAX_VALUE
                                        : (int) ctx.total;
                            }
                            runRevisionLoadStage(ctx, 5);
                        } else if (stage == 5) {
                            int loaded = ctx.rawList == null ? 0 : ctx.rawList.size();
                            showLoadProgress(ctx, 88, "Menyaring baris yang berubah dan menyiapkan paging tabel.", loaded, ctx.limit);
                            ctx.displayList = ctx.rawList == null ? new ArrayList() : ctx.rawList;
                            ctx.displayList = applyPostQueryFilters(ctx.session, ctx.displayList, ctx.allDataMode);
                            renderDashboard(ctx.dashboardContainer, ctx.dashboardData, ctx.allDataMode);
                            setGridData(ctx.targetGrid, ctx.displayList, new DataRenderer());
                            perbaruiPagingDb(ctx);
                            int shown = ctx.displayList == null ? 0 : ctx.displayList.size();
                            showLoadProgress(ctx, 100, "Selesai. Halaman " + (ctx.first / UKURAN_HALAMAN_DB + 1)
                                    + " menampilkan " + shown + " baris dari total "
                                    + (ctx.totalEstimasi ? "minimal " : "") + ctx.total
                                    + " revisi (10 data per halaman, langsung dari database).", loaded, shown);
                            finishRevisionLoad(ctx);
                            scheduleHideLoadProgress(ctx);
                        }
                    } catch (Exception e) {
                        handleRevisionLoadError(ctx, e);
                    }
                }
            });
        } catch (Exception e) {
            handleRevisionLoadError(ctx, e);
        }
    }

    private void setGridData(MyGrid targetGrid, List list, ais.ui.util.MyRowRenderer renderer) {
        if (targetGrid == null) {
            return;
        }
        try {
            targetGrid.setRowRenderer(renderer);
            targetGrid.setModelCheckMobile(new SimpleListModel(list == null ? new ArrayList() : list));
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:798");
            }
        }
    }

    private org.zkoss.zul.Div createLoadingContainer(Component parent) {
        org.zkoss.zul.Div div = new org.zkoss.zul.Div();
        div.setWidth("100%");
        div.setStyle("display:none; margin:0 0 10px 0;");
        div.setParent(parent);
        return div;
    }

    private void showLoadProgress(RevisionLoadContext ctx, int percent, String message, int loaded, int total) {
        try {
            if (ctx == null || ctx.loadingContainer == null) {
                return;
            }
            if (percent < 0) percent = 0;
            if (percent > 100) percent = 100;
            Common.clear(ctx.loadingContainer);
            ctx.loadingContainer.setStyle("display:block; margin:0 0 10px 0;");
            String safeMessage = escapeHtml(message == null ? "Memproses data revisi..." : message);
            String loadedText = total > 0 ? (loaded + " / " + total) : String.valueOf(loaded);
            String html = "<div style='padding:14px 16px; border-radius:16px; background:#ffffff; border:1px solid #dbeafe; "
                    + "box-shadow:0 10px 24px rgba(37,99,235,.08); box-sizing:border-box;'>"
                    + "<div style='display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap;'>"
                    + "<div><div style='font-size:11px; letter-spacing:.12em; text-transform:uppercase; color:#2563eb; font-weight:900;'>Revision Loading Center</div>"
                    + "<div style='font-size:15px; font-weight:900; color:#0f172a; margin-top:4px;'>" + escapeHtml(ctx.title) + "</div>"
                    + "<div style='font-size:12px; color:#64748b; margin-top:5px; line-height:1.45;'>" + safeMessage + "</div></div>"
                    + "<div style='text-align:right; min-width:96px;'><div style='font-size:28px; font-weight:900; color:#0f172a;'>" + percent + "%</div>"
                    + "<div style='font-size:11px; color:#64748b;'>" + escapeHtml(loadedText) + " data</div></div></div>"
                    + "<div style='height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:12px;'>"
                    + "<div style='height:12px; width:" + percent + "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>"
                    + "<div style='display:flex; gap:8px; flex-wrap:wrap; margin-top:10px; font-size:11px;'>"
                    + loadBadge("Filter") + loadBadge("Query") + loadBadge("Dashboard") + loadBadge("Paging 10") + loadBadge("Detail Object Link")
                    + "</div></div>";
            appendHtml(ctx.loadingContainer, html);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:836");
        }
    }

    private String loadBadge(String text) {
        return "<span style='padding:5px 9px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-weight:800;'>" + escapeHtml(text) + "</span>";
    }

    private void hideLoadProgress(RevisionLoadContext ctx) {
        try {
            if (ctx == null || ctx.loadingContainer == null) {
                return;
            }
            Common.clear(ctx.loadingContainer);
            ctx.loadingContainer.setStyle("display:none; margin:0; height:0; overflow:hidden;");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:851");
        }
    }

    private void scheduleHideLoadProgress(final RevisionLoadContext ctx) {
        try {
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event event) throws Exception {
                    hideLoadProgress(ctx);
                }
            });
        } catch (Exception e) {
            hideLoadProgress(ctx);
        }
    }

    private void finishRevisionLoad(RevisionLoadContext ctx) {
        try {
            closeSession(ctx == null ? null : ctx.session);
            if (ctx != null) {
                ctx.session = null;
                if (ctx.allDataMode) {
                    allSearchRunning = false;
                } else {
                    currentSearchRunning = false;
                }
            }
        } catch (Exception e) {
            if (ctx != null) {
                if (ctx.allDataMode) {
                    allSearchRunning = false;
                } else {
                    currentSearchRunning = false;
                }
            }
        }
    }

    private void handleRevisionLoadError(RevisionLoadContext ctx, Exception e) {
        try {
            if (isEnversUnavailableError(e)) {
                showLoadProgress(ctx, 100,
                        "Fitur riwayat revisi tidak tersedia saat ini. "
                        + "Hubungi administrator jika masalah berlanjut.", 0, 0);
            } else {
                showLoadProgress(ctx, 100, "Load data revisi gagal: " + safeExceptionMessage(e), 0, 0);
                Common.tampilErrorJikaAdmin(e);
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:899");
        } finally {
            finishRevisionLoad(ctx);
        }
    }

    private class RevisionLoadContext {
        boolean allDataMode;
        int limit;
        int first;
        long total;
        int selectedTabIndex;
        String title;
        Session session;
        AuditQuery query;
        List rawList;
        List displayList;
        RevisionDashboardData dashboardData;
        boolean totalEstimasi;
        String countCacheKey;
        org.zkoss.zul.Div loadingContainer;
        org.zkoss.zul.Div dashboardContainer;
        MyGrid targetGrid;
        Checkbox onlyChangedCheckbox;
    }

    private int getConfiguredLimit(String key, int defaultValue) {
        int result = defaultValue;
        try {
            String value = Common.getKonfigurasi(key, String.valueOf(defaultValue)).getNilai();
            if (value != null && value.trim().length() > 0) {
                result = Integer.parseInt(value.trim());
            }
        } catch (Exception e) {
            result = defaultValue;
        }
        if (result < PAGE_SIZE) {
            result = PAGE_SIZE;
        }
        if (result > 5000) {
            result = 5000;
        }
        return result;
    }

    private boolean isOnlyChangedRows() {
        return isOnlyChangedRows(hanyaTampilYangBerubah);
    }

    private boolean isOnlyChangedRows(Checkbox checkbox) {
        try {
            return checkbox != null && checkbox.isChecked();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isOnlyDeletedRows(Checkbox checkbox) {
        try {
            return checkbox != null && checkbox.isChecked();
        } catch (Exception e) {
            return false;
        }
    }

    private List applyPostQueryFilters(Session session, List list, boolean allDataMode) {
        List filtered = list == null ? new ArrayList() : list;
        if (isOnlyChangedRows(allDataMode ? allHanyaTampilYangBerubah : hanyaTampilYangBerubah)) {
            filtered = filterOnlyChangedRows(session, filtered);
        }
        String kolomPilih = getSelectedKolomBerubah(allDataMode);
        if (kolomPilih != null && kolomPilih.trim().length() > 0) {
            filtered = filterByChangedColumn(session, filtered, kolomPilih);
        }
        return filtered;
    }

    private List filterOnlyChangedRows(Session session, List list) {
        List filtered = new ArrayList();
        if (list == null || list.isEmpty()) {
            return filtered;
        }
        for (int i = 0; i < list.size(); i++) {
            Object row = list.get(i);
            Object entity = extractEntity(row);
            if (!(entity instanceof Serializable)) {
                continue;
            }
            String summary = buildComparisonWithPrevious(session, (Serializable) entity, extractRevisionEntity(row));
            if (hasMeaningfulChange(summary)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    /** Kolom yang dipilih pada filter "kolom berubah"; null = semua kolom (tanpa filter). */
    private String getSelectedKolomBerubah() {
        return getSelectedKolomBerubah(false);
    }

    private String getSelectedKolomBerubah(boolean allDataMode) {
        try {
            Combobox combo = allDataMode ? allFilterKolomBerubah : filterKolomBerubah;
            if (combo != null && combo.getSelectedItem() != null) {
                Object v = combo.getSelectedItem().getValue();
                return v == null ? null : v.toString();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.getSelectedKolomBerubah");
        }
        return null;
    }

    /**
     * Sisakan hanya baris revisi yang benar-benar MENGUBAH kolom {@code columnProperty}. Memakai
     * {@link #collectChangedPropertyNames} (logika deteksi perubahan yang sama dengan dashboard &amp;
     * tabel Field/Sebelum/Sesudah). Konsisten dengan filter "Hanya yang berubah" — bekerja pada
     * baris halaman aktif (paging DB 10/halaman).
     */
    private List filterByChangedColumn(Session session, List list, String columnProperty) {
        if (list == null || list.isEmpty() || columnProperty == null || columnProperty.trim().isEmpty()) {
            return list == null ? new ArrayList() : list;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Object row = list.get(i);
            Object entity = extractEntity(row);
            if (!(entity instanceof Serializable)) {
                continue;
            }
            try {
                List changed = collectChangedPropertyNames(session, (Serializable) entity,
                        extractRevisionEntity(row), extractRevisionType(row));
                if (changed != null && changed.contains(columnProperty)) {
                    filtered.add(row);
                }
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.filterByChangedColumn");
            }
        }
        return filtered;
    }

    private boolean hasMeaningfulChange(String comparisonText) {
        if (comparisonText == null) {
            return false;
        }
        String text = comparisonText.trim().toLowerCase();
        if (text.length() == 0) {
            return false;
        }
        return !(text.startsWith("tidak ada perubahan") || text.startsWith("belum ada revisi")
                || text.startsWith("gagal membandingkan"));
    }

    protected AuditQuery buildAuditQuery(Session session) throws Exception {
        return buildAuditQuery(session, false, keyword, mulai, sampai, tipeRevisi, hanyaTampilYangDihapus,
                filterKolomCari, nilaiKolomCari);
    }

    /**
     * Total revisi sesuai filter aktif, dihitung langsung di database
     * (projection count tanpa order) untuk kebutuhan paging 10/halaman.
     */
    private long hitungTotalRevisiDenganCache(Session session, boolean allDataMode, String cacheKey) {
        long now = System.currentTimeMillis();
        if (cacheKey != null && cacheKey.length() > 0) {
            CountCacheEntry cached = COUNT_CACHE.get(cacheKey);
            if (cached != null && now - cached.createdAt <= COUNT_CACHE_TTL_MS) {
                return cached.value;
            }
        }
        long total = hitungTotalRevisi(session, allDataMode);
        if (total >= 0L && cacheKey != null && cacheKey.length() > 0) {
            COUNT_CACHE.put(cacheKey, new CountCacheEntry(total, now));
        }
        return total;
    }

    private long hitungTotalRevisi(Session session, boolean allDataMode) {
        try {
            AuditReader reader = AuditReaderFactory.get(session);
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(entityClass, false, true);
            query.addProjection(AuditEntity.revisionNumber().count());

            Date m = normalizeStart((allDataMode ? allMulai : mulai) == null ? null
                    : (allDataMode ? allMulai : mulai).getValue());
            Date s = normalizeEnd((allDataMode ? allSampai : sampai) == null ? null
                    : (allDataMode ? allSampai : sampai).getValue());
            applyRevisionDateFilter(query, m, s);

            applyRevisionTypeFilter(query, allDataMode ? allTipeRevisi : tipeRevisi,
                    allDataMode ? allHanyaTampilYangDihapus : hanyaTampilYangDihapus);

            Textbox keywordBox = allDataMode ? allKeyword : keyword;
            String key = keywordBox == null || keywordBox.getValue() == null ? "" : keywordBox.getValue().trim();
            AuditCriterion keyCriterion = buildKeywordCriterion(key);
            if (keyCriterion != null) {
                query.add(keyCriterion);
            }
            AuditCriterion columnCriterion = buildColumnSearchCriterion(session,
                    allDataMode ? allFilterKolomCari : filterKolomCari,
                    allDataMode ? allNilaiKolomCari : nilaiKolomCari);
            if (columnCriterion != null) {
                query.add(columnCriterion);
            }

            for (int i = 0; i < customizers.length; i++) {
                if (customizers[i] != null) {
                    if (allDataMode && customizers[i] instanceof EntityIdFilter) {
                        continue;
                    }
                    customizers[i].apply(session, query);
                }
            }

            Object hasil = query.getSingleResult();
            return hasil instanceof Number ? ((Number) hasil).longValue() : 0L;
        } catch (Exception e) {
            if (isTimeoutQueryRevisi(e)) {
                return -1L;
            }
            if (!isEnversUnavailableError(e)) {
                try {
                    Common.tampilErrorJikaAdmin(e);
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1038");
                }
            }
            return 0L;
        }
    }

    private boolean isTimeoutQueryRevisi(Throwable e) {
        Throwable t = e;
        int guard = 0;
        while (t != null && guard < 20) {
            String pesan = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (pesan.indexOf("statement timeout") >= 0 || pesan.indexOf("canceling statement") >= 0
                    || pesan.indexOf("lock timeout") >= 0) {
                return true;
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    private String buildCountCacheKey(boolean allDataMode) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(entityClass == null ? "" : entityClass.getName()).append('|').append(allDataMode);
        appendDateKey(sb, allDataMode ? allMulai : mulai);
        appendDateKey(sb, allDataMode ? allSampai : sampai);
        appendTextboxKey(sb, allDataMode ? allKeyword : keyword);
        appendComboboxKey(sb, allDataMode ? allTipeRevisi : tipeRevisi);
        appendCheckboxKey(sb, allDataMode ? allHanyaTampilYangDihapus : hanyaTampilYangDihapus);
        appendComboboxKey(sb, allDataMode ? allFilterKolomCari : filterKolomCari);
        appendTextboxKey(sb, allDataMode ? allNilaiKolomCari : nilaiKolomCari);
        for (int i = 0; customizers != null && i < customizers.length; i++) {
            if (allDataMode && customizers[i] instanceof EntityIdFilter) {
                continue;
            }
            sb.append('|').append(customizers[i] == null ? "" : customizers[i].toString());
        }
        return sb.toString();
    }

    private void appendDateKey(StringBuilder sb, MyDatebox box) {
        Date value = null;
        try {
            value = box == null ? null : box.getValue();
        } catch (Exception e) {
            value = null;
        }
        sb.append('|').append(value == null ? "" : String.valueOf(value.getTime()));
    }

    private void appendTextboxKey(StringBuilder sb, Textbox box) {
        String value = "";
        try {
            value = box == null || box.getValue() == null ? "" : box.getValue().trim();
        } catch (Exception e) {
            value = "";
        }
        sb.append('|').append(value);
    }

    private void appendComboboxKey(StringBuilder sb, Combobox box) {
        try {
            sb.append('|').append(box == null ? -1 : box.getSelectedIndex()).append(':')
                    .append(box == null || box.getValue() == null ? "" : box.getValue());
        } catch (Exception e) {
            sb.append("|-1:");
        }
    }

    private void appendCheckboxKey(StringBuilder sb, Checkbox box) {
        try {
            sb.append('|').append(box != null && box.isChecked());
        } catch (Exception e) {
            sb.append("|false");
        }
    }

    private void perbaruiPagingDb(RevisionLoadContext ctx) {
        try {
            org.zkoss.zul.Paging paging = ctx.allDataMode ? pagingDbAll : pagingDb;
            if (paging == null) {
                return;
            }
            int total = ctx.total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ctx.total;
            paging.setPageSize(UKURAN_HALAMAN_DB);
            paging.setTotalSize(total < 0 ? 0 : total);
            int halaman = ctx.first / UKURAN_HALAMAN_DB;
            if (paging.getActivePage() != halaman && total > halaman * UKURAN_HALAMAN_DB) {
                paging.setActivePage(halaman);
            }
            paging.setVisible(total > UKURAN_HALAMAN_DB);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1059");
        }
    }

    protected AuditQuery buildAuditQuery(Session session, boolean skipEntityIdFilter, Textbox keywordBox,
            MyDatebox mulaiBox, MyDatebox sampaiBox, Combobox tipeBox) throws Exception {
        return buildAuditQuery(session, skipEntityIdFilter, keywordBox, mulaiBox, sampaiBox, tipeBox, null, null, null);
    }

    protected AuditQuery buildAuditQuery(Session session, boolean skipEntityIdFilter, Textbox keywordBox,
            MyDatebox mulaiBox, MyDatebox sampaiBox, Combobox tipeBox, Checkbox onlyDeletedBox,
            Combobox columnBox, Textbox columnValueBox) throws Exception {
        AuditReader reader;
        try {
            reader = AuditReaderFactory.get(session);
        } catch (UnsupportedOperationException uoe) {
            throw uoe;
        } catch (Exception getEx) {
            throw new RuntimeException("Riwayat revisi gagal diinisialisasi: " + getEx.getMessage(), getEx);
        }
        AuditQuery query = reader.createQuery().forRevisionsOfEntity(entityClass, false, true);
        query.addOrder(AuditEntity.revisionNumber().desc());

        Date m = normalizeStart(mulaiBox == null ? null : mulaiBox.getValue());
        Date s = normalizeEnd(sampaiBox == null ? null : sampaiBox.getValue());
        applyRevisionDateFilter(query, m, s);

        applyRevisionTypeFilter(query, tipeBox, onlyDeletedBox);

        String key = keywordBox == null || keywordBox.getValue() == null ? "" : keywordBox.getValue().trim();
        AuditCriterion keyCriterion = buildKeywordCriterion(key);
        if (keyCriterion != null) {
            query.add(keyCriterion);
        }
        AuditCriterion columnCriterion = buildColumnSearchCriterion(session, columnBox, columnValueBox);
        if (columnCriterion != null) {
            query.add(columnCriterion);
        }

        for (int i = 0; i < customizers.length; i++) {
            if (customizers[i] != null) {
                if (skipEntityIdFilter && customizers[i] instanceof EntityIdFilter) {
                    continue;
                }
                customizers[i].apply(session, query);
            }
        }
        return query;
    }

    private void applyRevisionTypeFilter(AuditQuery query, Combobox tipeBox, Checkbox onlyDeletedBox) {
        if (isOnlyDeletedRows(onlyDeletedBox)) {
            query.add(AuditEntity.revisionType().eq(RevisionType.DEL));
            return;
        }
        Integer tipe = getSelectedRevisionType(tipeBox);
        if (tipe != null) {
            if (tipe.intValue() == MODE_TAMBAH) {
                query.add(AuditEntity.revisionType().eq(RevisionType.ADD));
            } else if (tipe.intValue() == MODE_UBAH) {
                query.add(AuditEntity.revisionType().eq(RevisionType.MOD));
            } else if (tipe.intValue() == MODE_HAPUS) {
                query.add(AuditEntity.revisionType().eq(RevisionType.DEL));
            }
        }
    }

    private void applyRevisionDateFilter(AuditQuery query, Date mulaiDate, Date sampaiDate) {
        try {
            if (mulaiDate != null && sampaiDate != null) {
                query.add(AuditEntity.revisionProperty("timestamp").between(Long.valueOf(mulaiDate.getTime()), Long.valueOf(sampaiDate.getTime())));
            } else if (mulaiDate != null) {
                query.add(AuditEntity.revisionProperty("timestamp").ge(Long.valueOf(mulaiDate.getTime())));
            } else if (sampaiDate != null) {
                query.add(AuditEntity.revisionProperty("timestamp").le(Long.valueOf(sampaiDate.getTime())));
            }
        } catch (Exception e) {
            try {
                if (hasProperty("tanggal_dirubah")) {
                    if (mulaiDate != null && sampaiDate != null) {
                        query.add(AuditEntity.property("tanggal_dirubah").between(mulaiDate, sampaiDate));
                    } else if (mulaiDate != null) {
                        query.add(AuditEntity.property("tanggal_dirubah").ge(mulaiDate));
                    } else if (sampaiDate != null) {
                        query.add(AuditEntity.property("tanggal_dirubah").le(sampaiDate));
                    }
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1128");
            }
        }
    }

    protected AuditCriterion buildKeywordCriterion(String key) {
        if (key == null || key.trim().length() == 0) {
            return null;
        }
        String[] props = searchProperties == null || searchProperties.length == 0 ? guessSearchProperties() : searchProperties;
        AuditCriterion criterion = null;
        for (int i = 0; i < props.length; i++) {
            String property = props[i];
            // FIX akar masalah ClassCastException "String cannot be cast to Integer" (KE-1): LIKE
            // hanya valid utk kolom bertipe teks. Bila property ternyata Integer/Long dsb (mis.
            // "nis"/"nim" pada entity tertentu bertipe numerik, bukan String), Envers tetap
            // membangun SQL "... LIKE ?" tapi Hibernate membind parameter memakai tipe kolom asli
            // (Integer) -- meledak saat AuditQuery.getResultList() dieksekusi, bukan saat query
            // dibangun. Lewati property non-teks di sini supaya tidak pernah menghasilkan
            // kriteria yang pasti gagal.
            if (property != null && property.trim().length() > 0 && hasProperty(property.trim())
                    && isTextProperty(property.trim())) {
                AuditCriterion next = AuditEntity.property(property.trim()).like(key, org.hibernate.criterion.MatchMode.ANYWHERE);
                criterion = criterion == null ? next : AuditEntity.or(criterion, next);
            }
        }
        return criterion;
    }

    private AuditCriterion buildColumnSearchCriterion(Session session, Combobox columnBox, Textbox valueBox) {
        String property = getSelectedComboValue(columnBox);
        String value = valueBox == null || valueBox.getValue() == null ? "" : valueBox.getValue().trim();
        if (property == null || property.trim().length() == 0 || value.length() == 0) {
            return null;
        }
        property = property.trim();
        if (!hasProperty(property)) {
            return null;
        }
        try {
            Class propertyClass = getSearchPropertyClass(property);
            if (CharSequence.class.isAssignableFrom(propertyClass)) {
                if (isIdentifierProperty(property)) {
                    return AuditEntity.id().eq(value);
                }
                return AuditEntity.property(property).like(value, org.hibernate.criterion.MatchMode.ANYWHERE);
            }
            Object converted = convertManualInputValue(session, propertyClass, property, value);
            if (Date.class.isAssignableFrom(propertyClass) && converted instanceof Date) {
                Date awal = normalizeStart((Date) converted);
                Date akhir = normalizeEnd((Date) converted);
                if (isIdentifierProperty(property)) {
                    return AuditEntity.id().eq(converted);
                }
                return AuditEntity.property(property).between(awal, akhir);
            }
            if (isIdentifierProperty(property)) {
                return AuditEntity.id().eq(converted);
            }
            return AuditEntity.property(property).eq(converted);
        } catch (Exception e) {
            try {
                MyMessageboxConfig.show("Nilai filter kolom '" + property + "' belum sesuai tipe datanya: "
                        + (e.getMessage() == null ? e.toString() : e.getMessage()));
            } catch (Exception ex) {
                ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) GenericRevisiHelper.buildColumnSearchCriterion.show");
            }
            return null;
        }
    }

    private String getSelectedComboValue(Combobox combo) {
        try {
            if (combo != null && combo.getSelectedItem() != null) {
                Object value = combo.getSelectedItem().getValue();
                return value == null ? null : value.toString();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.getSelectedComboValue");
        }
        return null;
    }

    private Class getSearchPropertyClass(String property) {
        try {
            if (classMetadata != null && isIdentifierProperty(property) && classMetadata.getIdentifierType() != null
                    && classMetadata.getIdentifierType().getReturnedClass() != null) {
                return classMetadata.getIdentifierType().getReturnedClass();
            }
            if (classMetadata != null) {
                return getPropertyClass(classMetadata, property);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.getSearchPropertyClass");
        }
        return String.class;
    }

    private boolean isIdentifierProperty(String property) {
        try {
            return classMetadata != null && property != null
                    && property.equals(classMetadata.getIdentifierPropertyName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTextProperty(String property) {
        try {
            if (classMetadata == null) {
                return true;
            }
            org.hibernate.type.Type type = classMetadata.getPropertyType(property);
            if (type == null) {
                return true;
            }
            Class returnedClass = type.getReturnedClass();
            return returnedClass != null && CharSequence.class.isAssignableFrom(returnedClass);
        } catch (Exception e) {
            return true;
        }
    }

    protected String[] guessSearchProperties() {
        List<String> props = new ArrayList<String>();
        String[] candidates = new String[] { "nama", "kode", "keterangan", "judul", "topik", "nim", "nis", "email",
                "nomorInduk", "nomorIndukNasional", "noRegistrasi", "noUjian" };
        for (int i = 0; i < candidates.length; i++) {
            if (hasProperty(candidates[i])) {
                props.add(candidates[i]);
            }
        }
        return props.toArray(new String[props.size()]);
    }

    protected boolean hasProperty(String property) {
        if (property == null) {
            return false;
        }
        for (int i = 0; i < propertyNames.size(); i++) {
            if (property.equals(propertyNames.get(i))) {
                return true;
            }
        }
        return false;
    }

    private Integer getSelectedRevisionType() {
        return getSelectedRevisionType(tipeRevisi);
    }

    private Integer getSelectedRevisionType(Combobox combo) {
        try {
            if (combo != null && combo.getSelectedItem() != null) {
                Object value = combo.getSelectedItem().getValue();
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1185");
        }
        return Integer.valueOf(MODE_SEMUA);
    }

    protected void eagerInitialize(Session session, List list) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Object entity = extractEntity(list.get(i));
            if (entity == null) {
                continue;
            }
            try {
                // entity.toString() bisa memicu lazy-load proxy Hibernate. Kalau baris yang
                // direferensikan sudah tidak ada lagi di tabel (mis. data revisi/histori
                // lama yang FK-nya menunjuk ke baris yang sudah dihapus permanen), ini
                // melempar ObjectNotFoundException. Tangkap khusus supaya ketahuan
                // penyebabnya (bukan sekadar "empty-catch"), lalu lewati entity ini saja
                // -- entity lain di daftar tetap lanjut diproses.
                entity.toString();
                if (classMetadata != null) {
                    String[] props = classMetadata.getPropertyNames();
                    for (int p = 0; p < props.length; p++) {
                        try {
                            Object val = classMetadata.getPropertyValue(entity, props[p], EntityMode.POJO);
                            if (val != null) {
                                Hibernate.initialize(val);
                            }
                        } catch (org.hibernate.ObjectNotFoundException e) {
                            // Root cause ERROR 19 (ID 71): proxy lazy properti ini menunjuk ke baris
                            // yang sudah dihapus (mis. StatusTerbitItem#1). Biarkan properti ini
                            // tetap uninitialized/null, lanjutkan ke properti berikutnya.
                            ais.common.ErrorAuditUtil.record(e,
                                    "auto-audit(referensi lazy sudah terhapus, dilewati) src/ais/action/master/helper/GenericRevisiHelper.java:1291 properti="
                                            + props[p]);
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1209");
                        }
                    }
                }
            } catch (org.hibernate.ObjectNotFoundException e) {
                ais.common.ErrorAuditUtil.record(e,
                        "auto-audit(referensi lazy sudah terhapus, dilewati) src/ais/action/master/helper/GenericRevisiHelper.java:1284");
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1213");
            }
        }
    }

    protected Object extractEntity(Object value) {
        if (value == null) {
            return null;
        }
        if (entityClass.isInstance(value)) {
            return value;
        }
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length > 0 && entityClass.isInstance(arr[0])) {
                return arr[0];
            }
        }
        return null;
    }

    protected Object extractRevisionEntity(Object value) {
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length > 1) {
                return arr[1];
            }
        }
        return null;
    }

    protected RevisionType extractRevisionType(Object value) {
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length > 2 && arr[2] instanceof RevisionType) {
                return (RevisionType) arr[2];
            }
        }
        return null;
    }

    class DataRenderer extends ais.ui.util.MyRowRenderer {
        public void render(Row row, Object data) throws Exception {
            row.setValign("top");
            final Object entity = extractEntity(data);
            if (!(entity instanceof Serializable)) {
                row.setVisible(false);
                return;
            }
            final Serializable revisionObject = (Serializable) entity;
            final Object revEntity = extractRevisionEntity(data);
            final RevisionType revType = extractRevisionType(data);

            MyDetail detail = new MyDetail();
            detail.setParent(row);
            detail.addEventListener(Events.ON_OPEN, new EventListener() {
                public void onEvent(Event event) throws Exception {
                    MyDetail target = (MyDetail) event.getTarget();
                    if (target.isOpen() && target.getChildren().isEmpty()) {
                        renderDetail(target, revisionObject, revEntity);
                    }
                }
            });

            new MyLabelAgakKecil(formatRevisionDate(revEntity, revisionObject)).setParent(row);
            new MyLabelAgakKecil(labelRevisionType(revType)).setParent(row);
            new MyLabelAgakKecil(safeToString(revisionObject)).setParent(row);
            String ringkasanPerubahan = buildComparisonWithPrevious(revisionObject, revEntity);
            Component perubahan = createComparisonComponent(ringkasanPerubahan);
            perubahan.setParent(row);
            createOlehComponent(revisionObject).setParent(row);

            Hbox box = new Hbox();
            box.setParent(row);
            MyToolbarbuttonConfig restore = new MyToolbarbuttonConfig("Restore", "/img/refresh.gif");
            restore.setTooltiptext("Buka formulir berisi seluruh kolom revisi ini; ubah nilai (teks bebas) lalu Simpan untuk memperbarui data dengan ID tersebut.");
            restore.setParent(box);
            restore.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event event) throws Exception {
                    bukaFormEditRestore(revisionObject);
                }
            });

            if (isAdminUser()) {
                MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus Data Ini", "/img/delete.gif");
                hapus.setTooltiptext("Khusus admin: hapus data aktif berdasarkan ID revisi ini. Riwayat revisi tetap dapat dipakai untuk restore kembali.");
                hapus.setStyle("font-weight:bold; color:#ffffff; background:#dc2626; border-radius:10px; padding:4px 10px; margin-left:4px;");
                hapus.setParent(box);
                hapus.addEventListener(Events.ON_CLICK, new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        deleteDataIniWithConfirm(revisionObject);
                    }
                });
            }
        }
    }




    private void renderDashboard(Component parent, RevisionDashboardData d, boolean allDataMode) {
        if (parent == null) {
            return;
        }
        try {
            Common.clear(parent);
            if (d == null) {
                d = new RevisionDashboardData();
            }

            String scopeTitle = allDataMode ? "Dasbor Seluruh Data Revisi" : "Dasbor Revisi Data Ini";
            String scopeDesc = allDataMode
                    ? "Menunjukkan perubahan semua ID pada class yang sama dalam rentang waktu yang dipilih. Cocok untuk melihat aktivitas revisi secara umum."
                    : "Menunjukkan data apa yang sedang dilihat, jumlah riwayatnya, dan kapan perubahan paling sering terjadi.";
            String entityName = getEntityClassLabel();
            String idText = allDataMode ? "Semua ID" : getSelectedEntityIdText();

            appendHtml(parent, buildDashboardHeroHtml(scopeTitle, scopeDesc, entityName, idText, d, allDataMode));

            org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
            cardWrap.setWidth("100%");
            cardWrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
            cardWrap.setParent(parent);
            appendHtml(cardWrap, buildMetricCardHtml("Class Direvisi", escapeHtml(entityName), "Nama data yang sedang dilihat riwayat perubahannya.", "#dbeafe", "#1e40af", "C"));
            appendHtml(cardWrap, buildMetricCardHtml("Total Data Revisi", String.valueOf(d.totalRevisi), "Jumlah catatan revisi yang ditemukan oleh filter saat ini.", "#dcfce7", "#166534", "R"));
            appendHtml(cardWrap, buildMetricCardHtml("Total Perubahan", String.valueOf(d.totalPerubahan), "Perkiraan jumlah field yang berubah dibanding revisi sebelumnya.", "#fef3c7", "#92400e", "Δ"));
            appendHtml(cardWrap, buildMetricCardHtml("Tambah", String.valueOf(d.totalTambah), "Data baru yang tercatat dalam riwayat revisi.", "#ecfdf5", "#047857", "+"));
            appendHtml(cardWrap, buildMetricCardHtml("Edit", String.valueOf(d.totalUbah), "Data yang pernah diperbarui setelah dibuat.", "#eff6ff", "#1d4ed8", "✎"));
            appendHtml(cardWrap, buildMetricCardHtml("Delete", String.valueOf(d.totalHapus), "Data yang tercatat pernah dihapus.", "#fee2e2", "#991b1b", "×"));

            org.zkoss.zul.Div chartWrap = new org.zkoss.zul.Div();
            chartWrap.setWidth("100%");
            chartWrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
            chartWrap.setParent(parent);
            appendHtml(chartWrap, buildTrendPanelHtml(d));
            appendHtml(chartWrap, buildActionTrendPanelHtml(d));
            appendHtml(chartWrap, buildCompositionPanelHtml(d));
            appendHtml(chartWrap, buildFieldChangePanelHtml(d));
            appendHtml(chartWrap, buildTimeBucketPanelHtml(d));
            appendHtml(chartWrap, buildSpiderPanelHtml(d));
            appendHtml(chartWrap, buildInsightPanelHtml(d, allDataMode));
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1357");
            }
        }
    }

    private String buildDashboardHeroHtml(String title, String desc, String entityName, String idText,
            RevisionDashboardData d, boolean allDataMode) {
        String period = allDataMode ? formatPeriode(allMulai == null ? null : allMulai.getValue(), allSampai == null ? null : allSampai.getValue())
                : formatPeriode(mulai == null ? null : mulai.getValue(), sampai == null ? null : sampai.getValue());
        return "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;'>"
                + "<div style='position:absolute; right:-55px; top:-75px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
                + "<div style='position:absolute; right:110px; bottom:-80px; width:170px; height:170px; border-radius:999px; background:rgba(255,255,255,.10);'></div>"
                + "<div style='position:relative; z-index:1; display:flex; justify-content:space-between; gap:18px; flex-wrap:wrap; align-items:center;'>"
                + "<div style='max-width:760px;'>"
                + "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Revision Control Center</div>"
                + "<div style='font-size:28px; line-height:1.15; font-weight:900; margin-top:6px;'>" + escapeHtml(title) + "</div>"
                + "<div style='font-size:13px; opacity:.92; margin-top:8px; line-height:1.55;'>" + escapeHtml(desc) + "</div>"
                + "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
                + badge("Class: " + entityName) + badge("ID: " + idText) + badge("Periode: " + period) + badge("Data unik: " + d.totalDataUnik)
                + "</div></div>"
                + "<div style='display:flex; gap:10px; flex-wrap:wrap;'>"
                + heroNumber("Revisi", d.totalRevisi) + heroNumber("Perubahan", d.totalPerubahan)
                + "</div></div></div>";
    }

    private String badge(String text) {
        return "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:800;'>" + escapeHtml(text) + "</span>";
    }

    private String heroNumber(String label, int value) {
        return "<div style='min-width:112px; padding:13px 14px; border-radius:16px; background:rgba(255,255,255,.14); border:1px solid rgba(255,255,255,.18); text-align:right;'>"
                + "<div style='font-size:26px; font-weight:900;'>" + value + "</div>"
                + "<div style='font-size:11px; opacity:.88;'>" + escapeHtml(label) + "</div></div>";
    }

    private String buildMetricCardHtml(String title, String value, String desc, String bg, String color, String icon) {
        return "<div style='flex:1 1 150px; min-width:150px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px; "
                + "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;'>"
                + "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px;'>"
                + "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:900; background:" + bg + "; color:" + color + ";'>" + escapeHtml(icon) + "</div>"
                + "<div style='font-size:23px; font-weight:900; color:#0f172a; text-align:right; line-height:1.1;'>" + value + "</div>"
                + "</div><div style='font-size:12px; color:#64748b; margin-top:10px; font-weight:800;'>" + escapeHtml(title) + "</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:4px; line-height:1.45;'>" + escapeHtml(desc) + "</div></div>";
    }

    private String buildTrendPanelHtml(RevisionDashboardData d) {
        StringBuilder bars = new StringBuilder();
        int max = 1;
        for (int i = 0; i < d.trendValues.size(); i++) {
            Integer v = (Integer) d.trendValues.get(i);
            if (v != null && v.intValue() > max) {
                max = v.intValue();
            }
        }
        if (d.trendLabels.isEmpty()) {
            bars.append("<div style='font-size:12px; color:#94a3b8; padding:18px 0;'>Belum ada data perubahan pada filter ini.</div>");
        }
        for (int i = 0; i < d.trendLabels.size(); i++) {
            String label = (String) d.trendLabels.get(i);
            int value = ((Integer) d.trendValues.get(i)).intValue();
            int width = value <= 0 ? 1 : (int) Math.round((value * 100.0) / max);
            bars.append("<div style='display:flex; align-items:center; gap:8px; margin:8px 0;'>")
                    .append("<div style='width:74px; font-size:11px; color:#64748b;'>").append(escapeHtml(label)).append("</div>")
                    .append("<div style='flex:1; height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>")
                    .append("<div style='height:12px; width:").append(width).append("%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>")
                    .append("<div style='width:42px; text-align:right; font-size:11px; color:#0f172a; font-weight:800;'>").append(value).append("</div></div>");
        }
        return panelHtml("Trend Waktu Perubahan", "Memudahkan melihat bulan mana yang paling banyak terjadi perubahan.", bars.toString(), "flex:2 1 520px;");
    }

    private String buildCompositionPanelHtml(RevisionDashboardData d) {
        int total = d.totalTambah + d.totalUbah + d.totalHapus;
        int add = total == 0 ? 0 : (int) Math.round(d.totalTambah * 100.0 / total);
        int mod = total == 0 ? 0 : (int) Math.round(d.totalUbah * 100.0 / total);
        int del = total == 0 ? 0 : 100 - add - mod;
        String html = "<div style='display:flex; align-items:center; gap:16px; flex-wrap:wrap;'>"
                + "<div style='width:138px; height:138px; border-radius:999px; background:conic-gradient(#16a34a 0 " + add + "%, #2563eb " + add + "% " + (add + mod) + "%, #dc2626 " + (add + mod) + "% 100%); position:relative; box-shadow:inset 0 0 0 18px #ffffff, 0 12px 28px rgba(15,23,42,.10);'>"
                + "<div style='position:absolute; inset:42px; border-radius:999px; background:#ffffff; display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:900; color:#0f172a;'>" + total + "</div></div>"
                + "<div style='font-size:12px; color:#475569; line-height:1.8;'>"
                + legend("#16a34a", "Tambah", d.totalTambah, add) + legend("#2563eb", "Edit", d.totalUbah, mod) + legend("#dc2626", "Delete", d.totalHapus, del)
                + "</div></div>";
        return panelHtml("Komposisi Aksi", "Menunjukkan perbandingan tambah, edit, dan delete tanpa grafik tambahan dari library eksternal.", html, "flex:1 1 330px;");
    }

    private String buildActionTrendPanelHtml(RevisionDashboardData d) {
        StringBuilder sb = new StringBuilder();
        int max = 1;
        for (int i = 0; d != null && i < d.trendLabels.size(); i++) {
            int total = getListInt(d.trendTambahValues, i) + getListInt(d.trendUbahValues, i) + getListInt(d.trendHapusValues, i);
            if (total > max) max = total;
        }
        if (d == null || d.trendLabels.isEmpty()) {
            sb.append("<div style='font-size:12px; color:#94a3b8; padding:18px 0;'>Belum ada trend aksi pada filter ini.</div>");
        }
        for (int i = 0; d != null && i < d.trendLabels.size(); i++) {
            String label = String.valueOf(d.trendLabels.get(i));
            int add = getListInt(d.trendTambahValues, i);
            int mod = getListInt(d.trendUbahValues, i);
            int del = getListInt(d.trendHapusValues, i);
            int total = add + mod + del;
            int addW = total <= 0 ? 0 : (int) Math.round(add * 100.0 / total);
            int modW = total <= 0 ? 0 : (int) Math.round(mod * 100.0 / total);
            int delW = total <= 0 ? 0 : 100 - addW - modW;
            String rowOpacity = total <= 0 ? "0.45" : "1";
            sb.append("<div style='margin:9px 0;'>")
                    .append("<div style='display:flex; justify-content:space-between; font-size:11px; color:#64748b; margin-bottom:4px;'>")
                    .append("<span>").append(escapeHtml(label)).append("</span><b style='color:#0f172a;'>").append(total).append("</b></div>")
                    .append("<div style='height:13px; border-radius:999px; overflow:hidden; background:#e2e8f0; opacity:").append(rowOpacity).append("; display:flex;'>")
                    .append("<div title='Tambah' style='height:13px; width:").append(addW).append("%; background:#16a34a;'></div>")
                    .append("<div title='Edit' style='height:13px; width:").append(modW).append("%; background:#2563eb;'></div>")
                    .append("<div title='Delete' style='height:13px; width:").append(delW).append("%; background:#dc2626;'></div>")
                    .append("</div><div style='font-size:10px; color:#94a3b8; margin-top:3px;'>+").append(add).append(" · edit ").append(mod).append(" · hapus ").append(del).append("</div></div>");
        }
        return panelHtml("Trend Aksi per Bulan", "Membantu melihat apakah perubahan lebih banyak berupa tambah, edit, atau delete pada setiap bulan.", sb.toString(), "flex:2 1 520px;");
    }

    private String buildFieldChangePanelHtml(RevisionDashboardData d) {
        StringBuilder sb = new StringBuilder();
        int max = 1;
        for (int i = 0; d != null && i < d.fieldValues.size(); i++) {
            int v = getListInt(d.fieldValues, i);
            if (v > max) max = v;
        }
        if (d == null || d.fieldLabels.isEmpty()) {
            sb.append("<div style='font-size:12px; color:#94a3b8; padding:18px 0;'>Belum ada field dominan yang berubah pada sampel analisa.</div>");
        }
        for (int i = 0; d != null && i < d.fieldLabels.size(); i++) {
            String label = String.valueOf(d.fieldLabels.get(i));
            int value = getListInt(d.fieldValues, i);
            int width = value <= 0 ? 1 : (int) Math.round(value * 100.0 / max);
            sb.append("<div style='display:flex; align-items:center; gap:8px; margin:8px 0;'>")
                    .append("<div style='width:130px; font-size:11px; color:#475569; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>").append(escapeHtml(label)).append("</div>")
                    .append("<div style='flex:1; height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>")
                    .append("<div style='height:12px; width:").append(width).append("%; border-radius:999px; background:linear-gradient(90deg,#7c3aed,#2563eb);'></div></div>")
                    .append("<div style='width:34px; text-align:right; font-size:11px; color:#0f172a; font-weight:800;'>").append(value).append("</div></div>");
        }
        return panelHtml("Field Paling Sering Berubah", "Memudahkan mengetahui bagian data mana yang paling sering diperbarui.", sb.toString(), "flex:1 1 430px;");
    }

    private String buildTimeBucketPanelHtml(RevisionDashboardData d) {
        int malam = d == null ? 0 : d.malam;
        int pagi = d == null ? 0 : d.pagi;
        int siang = d == null ? 0 : d.siang;
        int sore = d == null ? 0 : d.sore;
        int max = Math.max(Math.max(malam, pagi), Math.max(siang, sore));
        if (max <= 0) max = 1;
        String html = timeBucketRow("Malam", "00.00-05.59", malam, max)
                + timeBucketRow("Pagi", "06.00-11.59", pagi, max)
                + timeBucketRow("Siang", "12.00-16.59", siang, max)
                + timeBucketRow("Sore", "17.00-23.59", sore, max);
        return panelHtml("Waktu Revisi Sering Terjadi", "Menunjukkan kebiasaan waktu pengguna melakukan perubahan data.", html, "flex:1 1 360px;");
    }

    private String timeBucketRow(String title, String sub, int value, int max) {
        int width = value <= 0 ? 1 : (int) Math.round(value * 100.0 / max);
        return "<div style='margin:8px 0;'>"
                + "<div style='display:flex; justify-content:space-between; font-size:11px; color:#64748b;'><span><b style='color:#334155;'>" + escapeHtml(title) + "</b> " + escapeHtml(sub) + "</span><b style='color:#0f172a;'>" + value + "</b></div>"
                + "<div style='height:11px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:4px;'>"
                + "<div style='height:11px; width:" + width + "%; border-radius:999px; background:linear-gradient(90deg,#0ea5e9,#22c55e);'></div></div></div>";
    }

    private String buildSpiderPanelHtml(RevisionDashboardData d) {
        int[] values = new int[] { d == null ? 0 : d.radarAktivitas, d == null ? 0 : d.radarKedalaman,
                d == null ? 0 : d.radarEdit, d == null ? 0 : d.radarTambah, d == null ? 0 : d.radarDelete };
        String[] labels = new String[] { "Aktivitas", "Kedalaman", "Edit", "Tambah", "Delete" };
        String points = radarPoints(values, 92, 92, 70);
        StringBuilder labelHtml = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            labelHtml.append("<div style='display:flex; justify-content:space-between; gap:8px; font-size:11px; margin:4px 0;'>")
                    .append("<span style='color:#64748b;'>").append(escapeHtml(labels[i])).append("</span><b style='color:#0f172a;'>").append(values[i]).append("%</b></div>");
        }
        String html = "<div style='display:flex; align-items:center; gap:14px; flex-wrap:wrap;'>"
                + "<svg width='184' height='184' viewBox='0 0 184 184' style='max-width:184px;'>"
                + radarGridPolygon(92, 92, 70, 1.0) + radarGridPolygon(92, 92, 70, 0.66) + radarGridPolygon(92, 92, 70, 0.33)
                + "<polygon points='" + points + "' style='fill:rgba(37,99,235,.30); stroke:#2563eb; stroke-width:3;'></polygon>"
                + radarAxisLines(92, 92, 70) + "</svg>"
                + "<div style='min-width:140px; flex:1;'>" + labelHtml.toString() + "</div></div>";
        return panelHtml("Spider Web Pola Revisi", "Ringkasan bentuk perubahan: seberapa aktif, seberapa dalam, dan aksi apa yang paling dominan.", html, "flex:1 1 390px;");
    }

    private int getListInt(List list, int index) {
        try {
            if (list != null && index >= 0 && index < list.size()) {
                Object value = list.get(index);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1546");
        }
        return 0;
    }

    private String radarPoints(int[] values, int cx, int cy, int radius) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; values != null && i < values.length; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * i / values.length);
            double pct = values[i] / 100.0;
            int x = (int) Math.round(cx + Math.cos(angle) * radius * pct);
            int y = (int) Math.round(cy + Math.sin(angle) * radius * pct);
            if (i > 0) sb.append(" ");
            sb.append(x).append(",").append(y);
        }
        return sb.toString();
    }

    private String radarGridPolygon(int cx, int cy, int radius, double scale) {
        int[] values = new int[] { (int) Math.round(100 * scale), (int) Math.round(100 * scale), (int) Math.round(100 * scale), (int) Math.round(100 * scale), (int) Math.round(100 * scale) };
        return "<polygon points='" + radarPoints(values, cx, cy, radius) + "' style='fill:none; stroke:#cbd5e1; stroke-width:1;'></polygon>";
    }

    private String radarAxisLines(int cx, int cy, int radius) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * i / 5);
            int x = (int) Math.round(cx + Math.cos(angle) * radius);
            int y = (int) Math.round(cy + Math.sin(angle) * radius);
            sb.append("<line x1='").append(cx).append("' y1='").append(cy).append("' x2='").append(x).append("' y2='").append(y).append("' style='stroke:#e2e8f0; stroke-width:1;'></line>");
        }
        return sb.toString();
    }

    private String buildInsightPanelHtml(RevisionDashboardData d, boolean allDataMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:10px;'>");
        sb.append(insightBox("Data unik", String.valueOf(d.totalDataUnik), "Jumlah ID berbeda yang memiliki revisi."));
        sb.append(insightBox("Revisi berubah", String.valueOf(d.totalBarisBerubah), "Baris revisi yang terdeteksi berisi perubahan."));
        sb.append(insightBox("Rata-rata", d.totalRevisi <= 0 ? "0" : String.valueOf(Math.round(d.totalPerubahan * 10.0 / d.totalRevisi) / 10.0), "Perkiraan perubahan field per revisi."));
        sb.append("</div>");
        String desc = allDataMode
                ? "Cocok untuk melihat class mana yang sering berubah dan kapan pengguna melakukan revisi terbanyak."
                : "Membantu memahami apakah data ini jarang berubah atau sering diperbarui.";
        return panelHtml("Ringkasan Mudah", desc, sb.toString(), "flex:1 1 360px;");
    }

    private String panelHtml(String title, String desc, String content, String flex) {
        return "<div style='" + flex + " background:#ffffff; border:1px solid #e5e7eb; border-radius:18px; padding:16px; "
                + "box-shadow:0 12px 26px rgba(15,23,42,.06); box-sizing:border-box;'>"
                + "<div style='font-size:15px; font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div>"
                + "<div style='font-size:12px; color:#64748b; margin-top:4px; line-height:1.5;'>" + escapeHtml(desc) + "</div>"
                + "<div style='margin-top:14px;'>" + content + "</div></div>";
    }

    private String legend(String color, String label, int value, int percent) {
        return "<div><span style='display:inline-block; width:10px; height:10px; border-radius:999px; background:" + color + "; margin-right:7px;'></span>"
                + escapeHtml(label) + ": <b>" + value + "</b> <span style='color:#94a3b8;'>(" + percent + "%)</span></div>";
    }

    private String insightBox(String title, String value, String desc) {
        return "<div style='border:1px solid #e2e8f0; border-radius:14px; padding:12px; background:#f8fafc;'>"
                + "<div style='font-size:20px; font-weight:900; color:#0f172a;'>" + escapeHtml(value) + "</div>"
                + "<div style='font-size:12px; font-weight:800; color:#475569; margin-top:4px;'>" + escapeHtml(title) + "</div>"
                + "<div style='font-size:11px; color:#94a3b8; margin-top:3px; line-height:1.4;'>" + escapeHtml(desc) + "</div></div>";
    }

    private RevisionDashboardData buildDashboardData(Session session, List list, boolean analyzeChange) {
        RevisionDashboardData data = new RevisionDashboardData();
        Map monthMap = new java.util.TreeMap();
        Map monthAddMap = new java.util.TreeMap();
        Map monthModMap = new java.util.TreeMap();
        Map monthDelMap = new java.util.TreeMap();
        Map fieldMap = new HashMap();
        Set uniqueIds = new HashSet();
        if (list == null) {
            return data;
        }
        data.totalRevisi = list.size();
        int limit = getConfiguredLimit("maksimal_analisa_dashboard_revisi", DEFAULT_DASHBOARD_ANALYSIS_LIMIT);
        if (limit > list.size()) {
            limit = list.size();
        }
        for (int i = 0; i < list.size(); i++) {
            Object row = list.get(i);
            Object entity = extractEntity(row);
            Object revEntity = extractRevisionEntity(row);
            RevisionType type = extractRevisionType(row);
            Date revisionDate = readRevisionDate(revEntity, entity);
            String month = formatMonthKey(revisionDate);
            incrementMap(monthMap, month);
            if (type == RevisionType.ADD) {
                data.totalTambah++;
                incrementMap(monthAddMap, month);
            } else if (type == RevisionType.MOD) {
                data.totalUbah++;
                incrementMap(monthModMap, month);
            } else if (type == RevisionType.DEL) {
                data.totalHapus++;
                incrementMap(monthDelMap, month);
            }
            accumulateTimeBucket(data, revisionDate);
            Object id = getEntityIdValue(entity);
            if (id != null) {
                uniqueIds.add(String.valueOf(id));
            }
            if (analyzeChange && i < limit && entity instanceof Serializable) {
                List changedProperties = collectChangedPropertyNames(session, (Serializable) entity, revEntity, type);
                if (changedProperties != null && !changedProperties.isEmpty()) {
                    data.totalBarisBerubah++;
                    data.totalPerubahan += changedProperties.size();
                    for (int p = 0; p < changedProperties.size(); p++) {
                        incrementMap(fieldMap, String.valueOf(changedProperties.get(p)));
                    }
                }
            }
        }
        data.totalDataUnik = uniqueIds.size();
        java.util.Iterator it = monthMap.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            data.trendLabels.add(String.valueOf(key));
            data.trendValues.add(monthMap.get(key));
            data.trendTambahValues.add(getMapInt(monthAddMap, key));
            data.trendUbahValues.add(getMapInt(monthModMap, key));
            data.trendHapusValues.add(getMapInt(monthDelMap, key));
        }
        fillTopFieldChanges(data, fieldMap, 8);
        fillRadarValues(data);
        return data;
    }

    private void incrementMap(Map map, Object key) {
        if (map == null) {
            return;
        }
        if (key == null) {
            key = "Tidak diketahui";
        }
        Integer current = (Integer) map.get(key);
        map.put(key, current == null ? Integer.valueOf(1) : Integer.valueOf(current.intValue() + 1));
    }

    private int getMapInt(Map map, Object key) {
        try {
            Object value = map == null ? null : map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1695");
        }
        return 0;
    }

    private void accumulateTimeBucket(RevisionDashboardData data, Date date) {
        try {
            if (data == null) {
                return;
            }
            if (date == null) {
                data.tanpaJam++;
                return;
            }
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (hour >= 0 && hour <= 5) {
                data.malam++;
            } else if (hour >= 6 && hour <= 11) {
                data.pagi++;
            } else if (hour >= 12 && hour <= 16) {
                data.siang++;
            } else {
                data.sore++;
            }
        } catch (Exception e) {
            try { data.tanpaJam++; } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1722");}
        }
    }

    private void fillTopFieldChanges(RevisionDashboardData data, Map fieldMap, int maxItems) {
        if (data == null || fieldMap == null || fieldMap.isEmpty()) {
            return;
        }
        List keys = new ArrayList(fieldMap.keySet());
        final Map sortFieldMap = fieldMap;
        java.util.Collections.sort(keys, new java.util.Comparator() {
            public int compare(Object o1, Object o2) {
                int v1 = getMapIntForSort(sortFieldMap, o1);
                int v2 = getMapIntForSort(sortFieldMap, o2);
                if (v1 == v2) {
                    return String.valueOf(o1).compareToIgnoreCase(String.valueOf(o2));
                }
                return v1 < v2 ? 1 : -1;
            }
        });
        for (int i = 0; i < keys.size() && i < maxItems; i++) {
            Object key = keys.get(i);
            data.fieldLabels.add(String.valueOf(key));
            data.fieldValues.add(Integer.valueOf(getMapInt(fieldMap, key)));
        }
    }

    private int getMapIntForSort(Map map, Object key) {
        return getMapInt(map, key);
    }

    private void fillRadarValues(RevisionDashboardData data) {
        if (data == null) {
            return;
        }
        int totalAction = data.totalTambah + data.totalUbah + data.totalHapus;
        double avgChange = data.totalRevisi <= 0 ? 0 : (data.totalPerubahan * 1.0 / data.totalRevisi);
        data.radarAktivitas = clampPercent(data.totalRevisi <= 0 ? 0 : Math.min(100, data.totalRevisi));
        data.radarKedalaman = clampPercent((int) Math.round(avgChange * 20.0));
        data.radarEdit = clampPercent(totalAction <= 0 ? 0 : (int) Math.round(data.totalUbah * 100.0 / totalAction));
        data.radarTambah = clampPercent(totalAction <= 0 ? 0 : (int) Math.round(data.totalTambah * 100.0 / totalAction));
        data.radarDelete = clampPercent(totalAction <= 0 ? 0 : (int) Math.round(data.totalHapus * 100.0 / totalAction));
    }

    private int clampPercent(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    private List collectChangedPropertyNames(Session session, Serializable revisionObject, Object revEntity, RevisionType type) {
        List result = new ArrayList();
        try {
            if (revisionObject == null || classMetadata == null) {
                return result;
            }
            if (type == RevisionType.ADD || type == RevisionType.DEL) {
                if (classMetadata.getIdentifier(revisionObject, EntityMode.POJO) != null) {
                    result.add(classMetadata.getIdentifierPropertyName() == null ? "ID" : classMetadata.getIdentifierPropertyName());
                }
                String[] propsAdd = classMetadata.getPropertyNames();
                for (int a = 0; propsAdd != null && a < propsAdd.length; a++) {
                    String property = propsAdd[a];
                    if (isIgnoredComparisonProperty(property)) {
                        continue;
                    }
                    Object value = null;
                    try { value = classMetadata.getPropertyValue(revisionObject, property, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1789");}
                    if (value != null && value.toString().trim().length() > 0) {
                        result.add(property);
                    }
                }
                return result;
            }
            Serializable id = null;
            try {
                id = classMetadata.getIdentifier(revisionObject, EntityMode.POJO);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1799");
            }
            Object previous = id == null ? null : findPreviousRevisionEntityForComparison(session, id, revEntity, revisionObject);
            if (previous == null) {
                result.add("Revisi awal");
                return result;
            }
            String[] props = classMetadata.getPropertyNames();
            for (int i = 0; props != null && i < props.length; i++) {
                String property = props[i];
                if (isIgnoredComparisonProperty(property)) {
                    continue;
                }
                Object before = null;
                Object after = null;
                try { before = classMetadata.getPropertyValue(previous, property, EntityMode.POJO); initializeQuietly(before); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1814");}
                try { after = classMetadata.getPropertyValue(revisionObject, property, EntityMode.POJO); initializeQuietly(after); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1815");}
                if (!sameValueForComparison(before, after)) {
                    result.add(property);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1820");
        }
        return result;
    }

    private int countChangedProperties(Session session, Serializable revisionObject, Object revEntity, RevisionType type) {
        try {
            if (revisionObject == null || classMetadata == null) {
                return 0;
            }
            if (type == RevisionType.ADD || type == RevisionType.DEL) {
                return countNonEmptyProperties(revisionObject);
            }
            Serializable id = null;
            try {
                id = classMetadata.getIdentifier(revisionObject, EntityMode.POJO);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1836");
            }
            Object previous = id == null ? null : findPreviousRevisionEntityForComparison(session, id, revEntity, revisionObject);
            if (previous == null) {
                return 1;
            }
            int count = 0;
            String[] props = classMetadata.getPropertyNames();
            for (int i = 0; props != null && i < props.length; i++) {
                String property = props[i];
                if (isIgnoredComparisonProperty(property)) {
                    continue;
                }
                Object before = null;
                Object after = null;
                try { before = classMetadata.getPropertyValue(previous, property, EntityMode.POJO); initializeQuietly(before); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1851");}
                try { after = classMetadata.getPropertyValue(revisionObject, property, EntityMode.POJO); initializeQuietly(after); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1852");}
                if (!sameValueForComparison(before, after)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private int countNonEmptyProperties(Object entity) {
        int count = 0;
        try {
            if (entity == null || classMetadata == null) {
                return 0;
            }
            if (classMetadata.getIdentifier(entity, EntityMode.POJO) != null) {
                count++;
            }
            String[] props = classMetadata.getPropertyNames();
            for (int i = 0; props != null && i < props.length; i++) {
                String property = props[i];
                if (isIgnoredComparisonProperty(property)) {
                    continue;
                }
                Object value = null;
                try { value = classMetadata.getPropertyValue(entity, property, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1879");}
                if (value != null && value.toString().trim().length() > 0) {
                    count++;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1884");
        }
        return count;
    }

    private Object getEntityIdValue(Object entity) {
        try {
            if (entity == null) {
                return null;
            }
            // FIX field relasi kosong di form "Ubah & Restore" utk snapshot
            // revisi Envers: nilai properti relasi (mis. VirtualAccountBank.
            // bankHost/calonSiswa) berupa proxy Hibernate yang terikat ke
            // sesi Envers internal yang SUDAH TERTUTUP saat form ini dibuka.
            // ClassMetadata.getIdentifier() reflektif kadang butuh sesi utk
            // resolve tipe/kelas, sedangkan getId() milik GeneralValueObject
            // adalah getter ID polos (non-lazy, non-check()) yang SELALU aman
            // dipanggil pada proxy Hibernate manapun (Hibernate menjamin
            // akses identifier tidak memicu inisialisasi) -- coba jalur ini
            // LEBIH DULU sblm reflection, supaya ID tetap tampil walau
            // proxy relasi berasal dari sesi yang sudah tertutup.
            if (entity instanceof ais.database.model.GeneralValueObject) {
                Long idLangsung = ((ais.database.model.GeneralValueObject) entity).getId();
                if (idLangsung != null) {
                    return idLangsung;
                }
            }
            // FIX ClassCastException/PropertyAccessException: field instance
            // "classMetadata" adalah metadata milik entity UTAMA yang sedang
            // direvisi -- tapi method ini juga dipanggil dari
            // formatValueForEdit() utk NILAI PROPERTI RELASI (entity lain,
            // mis. VirtualAccountBank sbg FK), yang class-nya BEDA dgn entity
            // utama. Memanggil classMetadata.getIdentifier(entity,...) dgn
            // metadata kelas yg salah membuat Hibernate memanggil getter
            // reflektif utk kelas yg salah -> ClassCastException. Resolve
            // metadata sesuai kelas RUNTIME entity ini sendiri (unwrap proxy
            // Hibernate dulu via Hibernate.getClass supaya tidak salah
            // ambil proxy class). Fallback ini dipakai utk entity dgn @Id
            // bukan Long/bukan dari field "id" warisan GeneralValueObject
            // (mis. Tbmuser ber-@Id String userId).
            Class<?> clazz = Hibernate.getClass(entity);
            ClassMetadata metaUntukEntity = HibernateUtil.getClassMetadata(clazz);
            if (metaUntukEntity != null) {
                return metaUntukEntity.getIdentifier(entity, EntityMode.POJO);
            }
            // Fallback: jika kelas runtime entity ini SAMA dgn entity utama
            // (kasus umum saat method dipanggil di luar formatValueForEdit),
            // classMetadata instance tetap valid dipakai.
            if (classMetadata != null) {
                return classMetadata.getIdentifier(entity, EntityMode.POJO);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1894");
        }
        return null;
    }

    private Date readRevisionDate(Object revEntity, Object entity) {
        try {
            if (revEntity instanceof DefaultRevisionEntity) {
                return ((DefaultRevisionEntity) revEntity).getRevisionDate();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1904");
        }
        try {
            if (classMetadata != null && hasProperty("tanggal_dirubah")) {
                Object value = classMetadata.getPropertyValue(entity, "tanggal_dirubah", EntityMode.POJO);
                if (value instanceof Date) {
                    return (Date) value;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1913");
        }
        return null;
    }

    private String formatMonthKey(Date date) {
        try {
            if (date == null) {
                return "Tanpa tanggal";
            }
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            int month = c.get(Calendar.MONTH) + 1;
            return String.valueOf(c.get(Calendar.YEAR)) + "-" + (month < 10 ? "0" + month : String.valueOf(month));
        } catch (Exception e) {
            return "Tanpa tanggal";
        }
    }

    private String getEntityClassLabel() {
        try {
            return entityClass == null ? "" : entityClass.getName();
        } catch (Exception e) {
            return "";
        }
    }

    private String getSelectedEntityIdText() {
        try {
            for (int i = 0; customizers != null && i < customizers.length; i++) {
                if (customizers[i] instanceof EntityIdFilter) {
                    Serializable id = ((EntityIdFilter) customizers[i]).getId();
                    return id == null ? "-" : String.valueOf(id);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:1948");
        }
        return "Semua ID";
    }

    private String formatPeriode(Date m, Date s) {
        if (m == null && s == null) {
            return "Semua waktu";
        }
        String mulaiText = m == null ? "Awal" : formatDateOnly(m);
        String sampaiText = s == null ? "Akhir" : formatDateOnly(s);
        return mulaiText + " s/d " + sampaiText;
    }

    private String formatDateOnly(Date date) {
        try {
            return date == null ? "" : Common.dateFormat5.get().format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private void appendHtml(Component parent, String html) {
        org.zkoss.zul.Html h = new org.zkoss.zul.Html(html == null ? "" : html);
        h.setParent(parent);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        String s = value;
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&#39;");
        return s;
    }

    private static class RevisionDashboardData {
        int totalRevisi;
        int totalPerubahan;
        int totalBarisBerubah;
        int totalDataUnik;
        int totalTambah;
        int totalUbah;
        int totalHapus;
        int malam;
        int pagi;
        int siang;
        int sore;
        int tanpaJam;
        int radarAktivitas;
        int radarKedalaman;
        int radarEdit;
        int radarTambah;
        int radarDelete;
        List trendLabels = new ArrayList();
        List trendValues = new ArrayList();
        List trendTambahValues = new ArrayList();
        List trendUbahValues = new ArrayList();
        List trendHapusValues = new ArrayList();
        List fieldLabels = new ArrayList();
        List fieldValues = new ArrayList();
    }

    /**
     * Hasil parse kolom olehId yang berisi:
     *   userId;ClassName1:line1;ClassName2:line2;...;ip
     * diisi oleh AuditTimestampInterceptor.olehId() pada setiap simpan.
     */
    private static class OlehIdInfo {
        String userId = "";
        List callEntries = new ArrayList(); // List<String> "ClassName:lineNumber"
        String ip = "";
    }

    /** Parse kolom olehId menjadi OlehIdInfo terstruktur. */
    private OlehIdInfo parseOlehId(String raw) {
        OlehIdInfo info = new OlehIdInfo();
        if (raw == null || raw.trim().length() == 0) {
            return info;
        }
        String[] parts = raw.split(";", -1);
        if (parts == null || parts.length == 0) {
            info.userId = raw.trim();
            return info;
        }
        // Bagian pertama = userId (dari generateOlehId)
        info.userId = parts[0] == null ? "" : parts[0].trim();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (part.length() == 0) {
                continue;
            }
            if (isCallStackEntry(part)) {
                info.callEntries.add(part);
            } else if (isIpAddress(part)) {
                info.ip = part;
            }
            // entry lain (misal catatan tambahan) diabaikan
        }
        return info;
    }

    /**
     * Mendeteksi entri call-stack yang dibangun AuditTimestampInterceptor.buildAuditCallFrom():
     * format "SimpleClassName:lineNumber", mis. "MahasiswaAction:123".
     */
    private boolean isCallStackEntry(String part) {
        if (part == null) {
            return false;
        }
        int colon = part.lastIndexOf(':');
        if (colon <= 0) {
            return false;
        }
        String afterColon = part.substring(colon + 1).trim();
        if (afterColon.length() == 0) {
            return false;
        }
        for (int i = 0; i < afterColon.length(); i++) {
            if (!Character.isDigit(afterColon.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Deteksi sederhana format IPv4. */
    private boolean isIpAddress(String part) {
        if (part == null) {
            return false;
        }
        String[] octets = part.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (int i = 0; i < octets.length; i++) {
            String oct = octets[i] == null ? "" : octets[i].trim();
            if (oct.length() == 0 || oct.length() > 3) {
                return false;
            }
            for (int j = 0; j < oct.length(); j++) {
                if (!Character.isDigit(oct.charAt(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Membuat komponen kolom "Oleh / Class Pengubah" yang menampilkan:
     * - Nama pengguna (oleh)
     * - User ID (jika berbeda dari nama)
     * - Call stack class:lineNumber yang tercatat saat simpan
     * - IP address
     */
    private Component createOlehComponent(Object entity) {
        try {
            String oleh = readString(entity, "oleh");
            String olehIdRaw = readString(entity, "olehId");
            OlehIdInfo info = parseOlehId(olehIdRaw);

            Vbox vbox = new Vbox();
            vbox.setWidth("100%");

            // Nama pengguna
            String displayName = oleh.length() > 0 ? oleh : info.userId;
            if (displayName.length() > 0) {
                Label nameLabel = new MyLabelAgakKecil(displayName);
                nameLabel.setStyle("font-weight:bold; color:#0f172a; white-space:normal; word-break:break-word;");
                nameLabel.setParent(vbox);
            }

            // User ID (jika berbeda dari nama)
            if (info.userId.length() > 0 && !info.userId.equals(oleh) && !info.userId.equals(displayName)) {
                Label idLabel = new MyLabelAgakKecil(info.userId);
                idLabel.setStyle("font-size:10px; color:#64748b; white-space:normal; word-break:break-word;");
                idLabel.setParent(vbox);
            }

            // Entri call-stack: class dan nomor baris
            if (!info.callEntries.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("<div style='margin-top:3px;'>");
                for (int i = 0; i < info.callEntries.size(); i++) {
                    String entry = (String) info.callEntries.get(i);
                    int colon = entry.lastIndexOf(':');
                    String cls  = colon > 0 ? entry.substring(0, colon) : entry;
                    String line = colon > 0 ? entry.substring(colon + 1) : "";
                    sb.append("<div style='font-size:10px; margin:1px 0; white-space:nowrap;'>")
                      .append("<span style='font-family:monospace; color:#1d4ed8; font-weight:bold;'>")
                      .append(escapeHtml(cls)).append("</span>");
                    if (line.length() > 0) {
                        sb.append("<span style='color:#64748b;'>:</span>")
                          .append("<span style='font-family:monospace; color:#dc2626; font-weight:bold;'>")
                          .append(escapeHtml(line)).append("</span>");
                    }
                    sb.append("</div>");
                }
                sb.append("</div>");
                org.zkoss.zul.Html callHtml = new org.zkoss.zul.Html(sb.toString());
                callHtml.setParent(vbox);
            }

            // IP address
            if (info.ip.length() > 0) {
                Label ipLabel = new MyLabelAgakKecil(info.ip);
                ipLabel.setStyle("font-size:10px; color:#94a3b8; white-space:normal; word-break:break-word;");
                try {
                    ipLabel.setTooltiptext("IP pengguna saat perubahan dicatat");
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2162");
                }
                ipLabel.setParent(vbox);
            }

            return vbox;
        } catch (Exception e) {
            return new MyLabelAgakKecil(readOleh(entity));
        }
    }

    private static class ComparisonChange implements Serializable {
        private static final long serialVersionUID = 6511472314475237202L;
        String field;
        String before;
        String after;
        boolean parsed;

        ComparisonChange(String field, String before, String after, boolean parsed) {
            this.field = field;
            this.before = before;
            this.after = after;
            this.parsed = parsed;
        }
    }

    private String buildComparisonLabelStyle(String ringkasanPerubahan) {
        if (!hasMeaningfulChange(ringkasanPerubahan)) {
            return "white-space:normal; word-break:break-word; overflow-wrap:break-word; line-height:1.45; color:#64748b;";
        }
        return "white-space:normal; word-break:break-word; overflow-wrap:break-word; line-height:1.45; "
                + "color:#b91c1c; font-weight:bold;";
    }

    private Component createComparisonComponent(String ringkasanPerubahan) {
        try {
            if (!hasMeaningfulChange(ringkasanPerubahan)) {
                Label kosong = new MyLabelAgakKecil(ringkasanPerubahan == null ? "Tidak ada perubahan." : ringkasanPerubahan);
                kosong.setStyle(buildComparisonLabelStyle(ringkasanPerubahan));
                kosong.setMultiline(true);
                return kosong;
            }

            List changes = parseComparisonChanges(ringkasanPerubahan);
            if (changes == null || changes.isEmpty()) {
                Label kosong = new MyLabelAgakKecil(ringkasanPerubahan == null ? "Tidak ada perubahan." : ringkasanPerubahan);
                kosong.setStyle(buildComparisonLabelStyle(ringkasanPerubahan));
                kosong.setMultiline(true);
                return kosong;
            }

            return createComparisonGrid(changes);
        } catch (Exception e) {
            Label fallback = new MyLabelAgakKecil(ringkasanPerubahan == null ? "" : ringkasanPerubahan);
            fallback.setMultiline(true);
            fallback.setStyle(buildComparisonLabelStyle(ringkasanPerubahan));
            return fallback;
        }
    }

    private Component createComparisonGrid(List changes) {
        Vbox wrapper = new Vbox();
        wrapper.setWidth("100%");
        wrapper.setStyle("max-width:100%; max-height:220px; overflow-y:auto; overflow-x:hidden; "
                + "box-sizing:border-box; padding:0; margin:0; background:#ffffff; border:1px solid #fee2e2; "
                + "border-radius:10px;");

        org.zkoss.zul.Grid comparisonGrid = new org.zkoss.zul.Grid();
        comparisonGrid.setWidth("100%");
        comparisonGrid.setStyle("border:none; margin:0; font-size:11px; background:#ffffff;");
        comparisonGrid.setParent(wrapper);

        Columns columns = new Columns();
        columns.setParent(comparisonGrid);
        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Field");
        column.setWidth("28%");
        column.setStyle("font-size:10px; font-weight:bold; color:#7f1d1d; background:#fef2f2;");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Sebelum");
        column.setWidth("36%");
        column.setStyle("font-size:10px; font-weight:bold; color:#7f1d1d; background:#fef2f2;");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Sesudah");
        column.setWidth("36%");
        column.setStyle("font-size:10px; font-weight:bold; color:#7f1d1d; background:#fef2f2;");

        Rows rows = new Rows();
        rows.setParent(comparisonGrid);

        for (int i = 0; changes != null && i < changes.size(); i++) {
            ComparisonChange change = (ComparisonChange) changes.get(i);
            Row row = new Row();
            row.setValign("top");
            row.setStyle(i % 2 == 0 ? "background:#ffffff;" : "background:#fff7f7;");
            row.setParent(rows);

            createComparisonCellLabel(change == null ? "" : change.field,
                    "font-size:11px; font-weight:900; color:#991b1b; line-height:1.35; "
                            + "white-space:normal; word-break:break-word; overflow-wrap:break-word;").setParent(row);
            createComparisonCellLabel(change == null ? "" : change.before,
                    "font-size:11px; color:#7f1d1d; line-height:1.35; "
                            + "white-space:normal; word-break:break-word; overflow-wrap:break-word;").setParent(row);
            createComparisonCellLabel(change == null ? "" : change.after,
                    "font-size:11px; color:#b91c1c; font-weight:700; line-height:1.35; "
                            + "white-space:normal; word-break:break-word; overflow-wrap:break-word;").setParent(row);
        }
        return wrapper;
    }

    private Label createComparisonCellLabel(String text, String style) {
        String raw = text == null ? "" : text;
        Label label = new MyLabelAgakKecil(shortenComparisonGridText(raw));
        label.setMultiline(true);
        label.setStyle(style);
        try {
            label.setTooltiptext(raw);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2282");
        }
        return label;
    }

    private List parseComparisonChanges(String ringkasanPerubahan) {
        List result = new ArrayList();
        if (ringkasanPerubahan == null) {
            return result;
        }
        String normalized = ringkasanPerubahan.replace('\r', '\n');
        String[] lines = normalized.split("\n");
        for (int i = 0; lines != null && i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            result.add(parseComparisonLine(line));
        }
        return result;
    }

    private ComparisonChange parseComparisonLine(String line) {
        if (line == null) {
            return new ComparisonChange("Info", "", "", false);
        }
        String cleaned = compactComparisonWhitespace(line);
        int colon = cleaned.indexOf(":");
        int arrow = cleaned.indexOf(" -> ");
        int arrowLength = 4;
        if (arrow < 0) {
            arrow = cleaned.indexOf("->");
            arrowLength = 2;
        }

        if (colon >= 0 && arrow > colon) {
            String field = cleaned.substring(0, colon).trim();
            String before = cleaned.substring(colon + 1, arrow).trim();
            String after = cleaned.substring(arrow + arrowLength).trim();
            return new ComparisonChange(emptyToDash(field), emptyToKosong(before), emptyToKosong(after), true);
        }

        if (arrow >= 0) {
            String before = cleaned.substring(0, arrow).trim();
            String after = cleaned.substring(arrow + arrowLength).trim();
            return new ComparisonChange("Data", emptyToKosong(before), emptyToKosong(after), true);
        }

        return new ComparisonChange("Info", cleaned, "-", false);
    }

    private String emptyToDash(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value.trim();
    }

    private String emptyToKosong(String value) {
        return value == null || value.trim().length() == 0 ? "(kosong)" : value.trim();
    }

    private String compactComparisonWhitespace(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        while (text.indexOf("  ") >= 0) {
            text = text.replace("  ", " ");
        }
        return text;
    }

    private String shortenComparisonGridText(String value) {
        String text = compactComparisonWhitespace(value);
        if (text.length() > 220) {
            return text.substring(0, 217) + "...";
        }
        return text;
    }

    private String buildComparisonHtml(String ringkasanPerubahan) {
        List changes = parseComparisonChanges(ringkasanPerubahan);
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:11px; color:#7f1d1d; border:1px solid #fee2e2;'>")
                .append("<thead><tr style='background:#fef2f2;'>")
                .append("<th style='text-align:left; padding:5px; border-bottom:1px solid #fee2e2;'>Field</th>")
                .append("<th style='text-align:left; padding:5px; border-bottom:1px solid #fee2e2;'>Sebelum</th>")
                .append("<th style='text-align:left; padding:5px; border-bottom:1px solid #fee2e2;'>Sesudah</th>")
                .append("</tr></thead><tbody>");
        for (int i = 0; changes != null && i < changes.size(); i++) {
            ComparisonChange change = (ComparisonChange) changes.get(i);
            String background = i % 2 == 0 ? "#ffffff" : "#fff7f7";
            sb.append("<tr style='background:").append(background).append(";'>")
                    .append("<td style='padding:5px; vertical-align:top; font-weight:900; color:#991b1b; word-break:break-word;'>")
                    .append(escapeHtml(change == null ? "" : change.field)).append("</td>")
                    .append("<td style='padding:5px; vertical-align:top; word-break:break-word;'>")
                    .append(escapeHtml(change == null ? "" : change.before)).append("</td>")
                    .append("<td style='padding:5px; vertical-align:top; font-weight:700; color:#b91c1c; word-break:break-word;'>")
                    .append(escapeHtml(change == null ? "" : change.after)).append("</td>")
                    .append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String buildComparisonLineHtml(String line) {
        ComparisonChange change = parseComparisonLine(line);
        return "<tr><td>" + escapeHtml(change.field) + "</td><td>" + escapeHtml(change.before)
                + "</td><td>" + escapeHtml(change.after) + "</td></tr>";
    }

    /**
     * Membandingkan data revisi saat ini dengan revisi sebelumnya untuk ID yang sama.
     * Kolom audit teknis oleh, olehId, dan tanggal_dirubah sengaja dilewati agar tampilan fokus pada data bisnis.
     */
    protected String buildComparisonWithPrevious(Serializable revisionObject, Object revEntity) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            return buildComparisonWithPrevious(session, revisionObject, revEntity);
        } catch (Exception e) {
            return "Gagal membandingkan perubahan: " + safeExceptionMessage(e);
        } finally {
            closeSession(session);
        }
    }

    protected String buildComparisonWithPrevious(Session session, Serializable revisionObject, Object revEntity) {
        String cacheKey = comparisonCacheKey(revisionObject, revEntity);
        try {
            if (comparisonCache != null && cacheKey != null && comparisonCache.containsKey(cacheKey)) {
                return comparisonCache.get(cacheKey);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2413");
        }

        String result = "Tidak ada perubahan.";
        try {
            if (revisionObject == null) {
                result = "Tidak ada perubahan.";
                return cacheComparison(cacheKey, result);
            }
            if (session == null || !session.isOpen()) {
                result = "Gagal membandingkan perubahan: session tidak tersedia.";
                return cacheComparison(cacheKey, result);
            }
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
            if (meta == null) {
                result = "Tidak ada perubahan.";
                return cacheComparison(cacheKey, result);
            }

            Serializable id = null;
            try {
                id = meta.getIdentifier(revisionObject, EntityMode.POJO);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2435");
            }
            if (id == null) {
                result = "Tidak ada perubahan.";
                return cacheComparison(cacheKey, result);
            }

            Object previous = findPreviousRevisionEntityForComparison(session, id, revEntity, revisionObject);
            if (previous == null) {
                result = "Belum ada revisi sebelumnya untuk ID yang sama.";
                return cacheComparison(cacheKey, result);
            }

            StringBuilder sb = new StringBuilder();
            String[] props = meta.getPropertyNames();
            for (int i = 0; props != null && i < props.length; i++) {
                String property = props[i];
                if (isIgnoredComparisonProperty(property)) {
                    continue;
                }

                Object before = null;
                Object after = null;
                try {
                    before = meta.getPropertyValue(previous, property, EntityMode.POJO);
                    initializeQuietly(before);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2461");
                }
                try {
                    after = meta.getPropertyValue(revisionObject, property, EntityMode.POJO);
                    initializeQuietly(after);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2466");
                }

                if (!sameValueForComparison(before, after)) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(property).append(": ");
                    sb.append(shortValueForComparison(before));
                    sb.append(" -> ");
                    sb.append(shortValueForComparison(after));
                }
            }

            result = sb.length() == 0 ? "Tidak ada perubahan." : sb.toString();
            return cacheComparison(cacheKey, result);
        } catch (Exception e) {
            result = "Gagal membandingkan perubahan: " + safeExceptionMessage(e);
            return cacheComparison(cacheKey, result);
        }
    }

    private String comparisonCacheKey(Serializable revisionObject, Object revEntity) {
        try {
            if (revisionObject == null) {
                return null;
            }
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
            Object id = meta == null ? null : meta.getIdentifier(revisionObject, EntityMode.POJO);
            Integer rev = extractRevisionNumberAsInteger(revEntity);
            return String.valueOf(entityClass == null ? "" : entityClass.getName()) + "#" + String.valueOf(id)
                    + "#" + String.valueOf(rev);
        } catch (Exception e) {
            return null;
        }
    }

    private String cacheComparison(String cacheKey, String result) {
        try {
            if (comparisonCache != null && cacheKey != null) {
                comparisonCache.put(cacheKey, result);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2508");
        }
        return result;
    }

    private Object findPreviousRevisionEntityForComparison(Session session, Serializable id, Object revEntity,
            Serializable revisionObject) {
        try {
            AuditReader reader = AuditReaderFactory.get(session);
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(entityClass, false, true);
            query.add(AuditEntity.id().eq(id));

            Integer revisionNumber = extractRevisionNumberAsInteger(revEntity);
            if (revisionNumber != null) {
                query.add(AuditEntity.revisionNumber().lt(revisionNumber));
            } else {
                Date tanggal = readDateValue(revisionObject, "tanggal_dirubah");
                if (tanggal != null && hasProperty("tanggal_dirubah")) {
                    query.add(AuditEntity.property("tanggal_dirubah").lt(tanggal));
                }
            }

            query.addOrder(AuditEntity.revisionNumber().desc());
            query.setMaxResults(5);
            List results = query.getResultList();
            for (int i = 0; results != null && i < results.size(); i++) {
                Object previous = extractEntity(results.get(i));
                if (previous != null) {
                    return previous;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2539");
        }
        return null;
    }

    private Integer extractRevisionNumberAsInteger(Object revEntity) {
        try {
            if (revEntity instanceof DefaultRevisionEntity) {
                return Integer.valueOf(((DefaultRevisionEntity) revEntity).getId());
            }
            if (revEntity != null) {
                String[] methods = new String[] { "getId", "getRevision", "getRevisionNumber", "getRev" };
                for (int i = 0; i < methods.length; i++) {
                    try {
                        Method method = revEntity.getClass().getMethod(methods[i], new Class[0]);
                        Object value = method.invoke(revEntity, new Object[0]);
                        if (value instanceof Number) {
                            return Integer.valueOf(((Number) value).intValue());
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2558");
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2562");
        }
        return null;
    }

    private Date readDateValue(Object entity, String property) {
        try {
            if (entity != null && property != null && classMetadata != null && hasProperty(property)) {
                Object value = classMetadata.getPropertyValue(entity, property, EntityMode.POJO);
                if (value instanceof Date) {
                    return (Date) value;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2575");
        }
        return null;
    }

    private boolean isIgnoredComparisonProperty(String property) {
        if (property == null) {
            return true;
        }
        String p = property.trim();
        return p.length() == 0 || "oleh".equalsIgnoreCase(p) || "olehId".equalsIgnoreCase(p)
                || "tanggal_dirubah".equalsIgnoreCase(p) || "tanggalDirubah".equalsIgnoreCase(p);
    }

    private boolean sameValueForComparison(Object before, Object after) {
        String a = valueKeyForComparison(before);
        String b = valueKeyForComparison(after);
        return a == null ? b == null : a.equals(b);
    }

    private String valueKeyForComparison(Object value) {
        try {
            if (value == null) {
                return "";
            }
            if (value instanceof GeneralValueObject) {
                GeneralValueObject gvo = (GeneralValueObject) value;
                return value.getClass().getName() + "#" + (gvo.getId() == null ? "" : gvo.getId().toString());
            }
            if (value instanceof Date) {
                return String.valueOf(((Date) value).getTime());
            }
            return value.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String shortValueForComparison(Object value) {
        String text = formatValue(value);
        if (text == null || text.trim().length() == 0) {
            return "(kosong)";
        }
        text = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        while (text.indexOf("  ") >= 0) {
            text = text.replace("  ", " ");
        }
        text = text.trim();
        int max = 180;
        if (text.length() > max) {
            return text.substring(0, max) + "...";
        }
        return text;
    }

    private void initializeQuietly(Object value) {
        try {
            if (value != null) {
                Hibernate.initialize(value);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2635");
        }
    }

    private static boolean isEnversUnavailableError(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof UnsupportedOperationException) {
            return true;
        }
        Throwable cause = e.getCause();
        return cause != null && cause != e && isEnversUnavailableError(cause);
    }

    private String safeExceptionMessage(Exception e) {
        try {
            if (e == null) {
                return "";
            }
            String msg = e.getMessage();
            if (msg == null || msg.trim().length() == 0) {
                return e.getClass().getName();
            }
            return msg.trim();
        } catch (Exception ex) {
            return "";
        }
    }

    protected void renderDetail(MyDetail detail, final Serializable revisionObject) {
        renderDetail(detail, revisionObject, null);
    }

    protected void renderDetail(MyDetail detail, final Serializable revisionObject, final Object revEntity) {
        Common.clear(detail);

        Hbox detailBar = new Hbox();
        detailBar.setStyle("margin:2px 0 8px 0; align-items:center;");
        detailBar.setParent(detail);
        MyToolbarbuttonConfig downloadSatu = new MyToolbarbuttonConfig("Download 1 revisi", "/img/excel.png");
        downloadSatu.setTooltiptext("Unduh detail revisi ini per field (CSV, bisa dibuka di Excel)");
        downloadSatu.setStyle("font-weight:bold;");
        downloadSatu.setParent(detailBar);
        downloadSatu.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                downloadSatuRevisi(revisionObject, revEntity);
            }
        });

        MyGrid detailGrid = new MyGrid();
        detailGrid.setWidth("100%");
        detailGrid.setStyle("max-width:100%; overflow-x:hidden; overflow-y:visible; table-layout:fixed; box-sizing:border-box;");
        detailGrid.setParent(detail);
        Columns columns = new Columns();
        columns.setParent(detailGrid);
        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Field");
        column.setWidth("22%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Nilai Revisi");
        column.setWidth("34%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Nilai Sebelumnya");
        column.setWidth("34%");
        column = new MyColumnConfig();
        column.setParent(columns);
        column.setLabel("Aksi");
        column.setWidth(ais.ui.util.GridKolomHelper.LEBAR_KOLOM_AKSI);
        Rows rows = new Rows();
        rows.setParent(detailGrid);

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
            Serializable id = meta == null ? null : meta.getIdentifier(revisionObject, EntityMode.POJO);
            Object current = id == null ? null : session.get(entityClass, id);
            Object previous = id == null ? null : findPreviousRevisionEntityForComparison(session, id, revEntity, revisionObject);

            if (meta != null) {
                Object previousId = previous == null ? null : meta.getIdentifier(previous, EntityMode.POJO);
                addDetailRow(rows, meta.getIdentifierPropertyName(), id, previousId, revisionObject, current, true);
                String[] props = meta.getPropertyNames();
                for (int i = 0; i < props.length; i++) {
                    Object revisionValue = null;
                    Object previousValue = null;
                    try { revisionValue = meta.getPropertyValue(revisionObject, props[i], EntityMode.POJO); initializeQuietly(revisionValue); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2725");}
                    try { previousValue = previous == null ? null : meta.getPropertyValue(previous, props[i], EntityMode.POJO); initializeQuietly(previousValue); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2726");}
                    addDetailRow(rows, props[i], revisionValue, previousValue, revisionObject, current, false);
                }
            }
        } catch (Exception e) {
            Row r = new Row();
            r.setParent(rows);
            ais.ui.util.ZkCompat.setSpans(r, "4");
            r.appendChild(new Label("Gagal menampilkan detail revisi: " + e.getMessage()));
        } finally {
            closeSession(session);
        }
    }

    private Component createRevisionValueComponent(final Object value, boolean highlighted) {
        try {
            if (value instanceof GeneralValueObject) {
                final GeneralValueObject gvo = (GeneralValueObject) value;
                if (gvo.getId() != null) {
                    A link = new A();
                    String label = shortText(formatValue(value), 120);
                    if (label == null || label.trim().length() == 0) {
                        label = getSafeHibernateClassName(value) + " #" + gvo.getId();
                    }
                    link.setLabel(label + "  | lihat revisi");
                    link.setTooltiptext("Buka riwayat revisi object relasi ini");
                    link.setStyle((highlighted ? "color:#b91c1c; font-weight:bold;" : "color:#2563eb; font-weight:bold;")
                            + " text-decoration:none; white-space:normal; word-break:break-word; overflow-wrap:break-word; cursor:pointer;");
                    link.addEventListener(Events.ON_CLICK, new EventListener() {
                        public void onEvent(Event event) throws Exception {
                            openRelatedRevisionPopup(gvo);
                        }
                    });
                    return link;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2762");
        }
        Label label = new Label(formatValue(value));
        label.setMultiline(true);
        label.setStyle((highlighted ? "color:#b91c1c; font-weight:bold;" : "color:#334155;")
                + " white-space:normal; word-break:break-word; overflow-wrap:break-word; line-height:15px;");
        return label;
    }

    private void applyHighlightStyle(Component component, String style) {
        try {
            if (component instanceof Label) {
                ((Label) component).setStyle(style + " white-space:normal; word-break:break-word; overflow-wrap:break-word; line-height:15px;");
            } else if (component instanceof A) {
                ((A) component).setStyle(style + " text-decoration:none; white-space:normal; word-break:break-word; overflow-wrap:break-word; cursor:pointer;");
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2778");
        }
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }
        String result = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        while (result.indexOf("  ") >= 0) {
            result = result.replace("  ", " ");
        }
        if (max > 0 && result.length() > max) {
            return result.substring(0, max) + "...";
        }
        return result;
    }

    private String getSafeHibernateClassName(Object value) {
        try {
            Class clazz = Hibernate.getClass(value);
            return clazz == null ? "Object" : clazz.getName();
        } catch (Exception e) {
            try {
                return value == null ? "Object" : value.getClass().getName();
            } catch (Exception ex) {
                return "Object";
            }
        }
    }

    private void openRelatedRevisionPopup(GeneralValueObject gvo) {
        try {
            if (gvo == null || gvo.getId() == null) {
                MyMessageboxConfig.show("Object relasi belum memiliki ID, sehingga riwayat revisinya belum bisa dibuka.");
                return;
            }
            Class clazz = null;
            try {
                clazz = Hibernate.getClass(gvo);
            } catch (Exception e) {
                clazz = gvo.getClass();
            }
            RevisiHelper helper = new RevisiHelper(clazz, gvo.getId());
            Component parent = getParent();
            if (parent != null) {
                parent.appendChild(helper);
            } else {
                appendChild(helper);
            }
            helper.setVisible(true);
            helper.onModal();
        } catch (Exception e) {
            try {
                MyMessageboxConfig.show("Riwayat revisi object relasi belum dapat dibuka. Pastikan object tersebut sudah diaudit oleh sistem.");
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2833");
            }
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2837");
            }
        }
    }

    private void addDetailRow(Rows rows, final String property, final Object oldValue, final Object currentValue,
            final Serializable revisionObject, final Object currentObject, boolean id) {
        Row r = new Row();
        r.setParent(rows);
        boolean berbeda = !id && !isIgnoredComparisonProperty(property) && !sameValueForComparison(oldValue, currentValue);
        if (berbeda) {
            r.setStyle("background:#fff1f2; color:#991b1b; font-weight:bold;");
        }

        Label fieldLabel = new Label(property == null ? "" : property);
        fieldLabel.setParent(r);
        Component revisiComponent = createRevisionValueComponent(oldValue, berbeda);
        revisiComponent.setParent(r);
        Component pembandingComponent = createRevisionValueComponent(currentValue, berbeda);
        pembandingComponent.setParent(r);
        if (berbeda) {
            String style = "color:#b91c1c; font-weight:bold;";
            fieldLabel.setStyle(style);
            applyHighlightStyle(revisiComponent, style);
            applyHighlightStyle(pembandingComponent, style);
        }

        Hbox aksi = new Hbox();
        aksi.setSpacing("4px");
        aksi.setParent(r);
        if (!id && Common.getApakahAdmin() && currentObject instanceof GeneralValueObject
                && !Common.bolehKonfigurasi("tidak_boleh_kembalikan_data_di_revisi_data", Konfigurasi.TIDAK_AKTIF)) {
            MyToolbarbuttonConfig ubah = new MyToolbarbuttonConfig("Pakai", "/img/check.gif");
            ubah.setTooltiptext("Ubah field ini ke nilai revisi");
            ubah.setParent(aksi);
            ubah.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event event) throws Exception {
                    restoreOneProperty((GeneralValueObject) currentObject, property, oldValue);
                }
            });
        }

        if (!id && Common.getApakahAdmin() && currentObject instanceof GeneralValueObject
                && !Common.bolehKonfigurasi("tidak_boleh_edit_manual_data_di_revisi_data", Konfigurasi.TIDAK_AKTIF)) {
            MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Edit", "/img/edit.gif");
            edit.setTooltiptext("Edit manual nilai field ini, lalu simpan ke data aktif");
            edit.setParent(aksi);
            edit.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event event) throws Exception {
                    showManualEditPopup((GeneralValueObject) currentObject, property, oldValue, currentValue);
                }
            });
        }
    }

    protected void showManualEditPopup(final GeneralValueObject currentObject, final String property,
            final Object revisionValue, final Object previousValue) {
        if (currentObject == null || currentObject.getId() == null || property == null || property.trim().length() == 0) {
            try {
                MyMessageboxConfig.show("Field belum dapat diedit karena data aktif atau nama field tidak lengkap.");
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2897");
            }
            return;
        }

        Session session = null;
        Object activeValue = null;
        Class propertyClass = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Class ownerClass = getHibernateClassSafely(currentObject);
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(ownerClass);
            Object activeObject = session.get(ownerClass, currentObject.getId());
            if (activeObject == null || meta == null) {
                MyMessageboxConfig.show("Data aktif belum ditemukan. Field tidak dapat diedit manual.");
                return;
            }
            activeValue = meta.getPropertyValue(activeObject, property, EntityMode.POJO);
            initializeQuietly(activeValue);
            propertyClass = getPropertyClass(meta, property);
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2920");
            }
        } finally {
            closeSession(session);
        }

        final MyWindow win = new MyWindow();
        win.setTitle("Edit Manual Nilai Revisi");
        win.setWidth("720px");
        win.setHeight("520px");
        win.setClosable(true);
        win.setSizable(true);
        win.setBorder("normal");
        try {
            win.setContentStyle("overflow:auto; padding:0; background:#f6f8fb;");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:2935");
        }

        Vbox root = new Vbox();
        root.setWidth("100%");
        root.setStyle("padding:14px; box-sizing:border-box; background:#f6f8fb;");
        root.setParent(win);

        appendHtml(root, "<div style='padding:16px; border-radius:18px; color:#fff; "
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 12px 28px rgba(15,23,42,.16);'>"
                + "<div style='font-size:11px; letter-spacing:.12em; text-transform:uppercase; opacity:.85; font-weight:900;'>Edit Manual Data Aktif</div>"
                + "<div style='font-size:20px; font-weight:900; margin-top:6px;'>" + escapeHtml(property) + "</div>"
                + "<div style='font-size:12px; line-height:1.55; opacity:.92; margin-top:8px;'>Isi nilai baru dengan hati-hati. Perubahan akan langsung disimpan ke data aktif, bukan hanya ke riwayat revisi.</div>"
                + "</div>");

        MyGrid infoGrid = new MyGrid();
        infoGrid.setWidth("100%");
        infoGrid.setStyle("margin-top:12px; border-radius:14px; overflow:hidden; background:#ffffff;");
        infoGrid.setParent(root);
        Columns columns = new Columns();
        columns.setParent(infoGrid);
        MyColumnConfig c = new MyColumnConfig();
        c.setParent(columns);
        c.setLabel("Keterangan");
        c.setWidth("28%");
        c = new MyColumnConfig();
        c.setParent(columns);
        c.setLabel("Nilai");
        c.setWidth("72%");
        Rows infoRows = new Rows();
        infoRows.setParent(infoGrid);
        addManualInfoRow(infoRows, "Class", getEntityClassLabel());
        addManualInfoRow(infoRows, "ID Data", String.valueOf(currentObject.getId()));
        addManualInfoRow(infoRows, "Tipe Field", propertyClass == null ? "Tidak diketahui" : propertyClass.getName());
        addManualInfoRow(infoRows, "Nilai Aktif", formatValueForManual(activeValue));
        addManualInfoRow(infoRows, "Nilai Revisi", formatValueForManual(revisionValue));
        addManualInfoRow(infoRows, "Nilai Sebelumnya", formatValueForManual(previousValue));

        appendHtml(root, "<div style='margin-top:12px; padding:12px 14px; border-radius:14px; background:#ffffff; border:1px solid #e5e7eb;'>"
                + "<div style='font-size:13px; font-weight:900; color:#0f172a;'>Nilai Baru</div>"
                + "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-top:4px;'>"
                + escapeHtml(buildManualEditHelpText(propertyClass)) + "</div></div>");

        final Textbox txtValue = new Textbox();
        txtValue.setWidth("100%");
        txtValue.setRows(5);
        txtValue.setMultiline(true);
        txtValue.setValue(formatManualInputValue(activeValue));
        txtValue.setStyle("margin-top:8px; font-family:Consolas,monospace; font-size:12px; border-radius:10px;");
        txtValue.setParent(root);

        Hbox quickButtons = new Hbox();
        quickButtons.setSpacing("8px");
        quickButtons.setStyle("margin-top:8px; flex-wrap:wrap;");
        quickButtons.setParent(root);
        MyToolbarbuttonConfig pakaiRevisi = new MyToolbarbuttonConfig("Isi dari Nilai Revisi", "/img/check.gif");
        pakaiRevisi.setTooltiptext("Masukkan nilai revisi ke form edit manual. Data belum disimpan sampai tombol Simpan diklik.");
        pakaiRevisi.setParent(quickButtons);
        pakaiRevisi.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                txtValue.setValue(formatManualInputValue(revisionValue));
            }
        });
        MyToolbarbuttonConfig pakaiSebelumnya = new MyToolbarbuttonConfig("Isi dari Nilai Sebelumnya", "/img/undo.gif");
        pakaiSebelumnya.setTooltiptext("Masukkan nilai sebelumnya ke form edit manual. Data belum disimpan sampai tombol Simpan diklik.");
        pakaiSebelumnya.setParent(quickButtons);
        pakaiSebelumnya.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                txtValue.setValue(formatManualInputValue(previousValue));
            }
        });

        Hbox buttons = new Hbox();
        buttons.setSpacing("8px");
        buttons.setStyle("margin-top:12px;");
        buttons.setParent(root);

        MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        simpan.setTooltiptext("Simpan nilai manual ke data aktif");
        simpan.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px;");
        simpan.setParent(buttons);
        simpan.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                confirmAndSaveManualValue(win, currentObject, property, txtValue.getValue());
            }
        });

        MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        batal.setTooltiptext("Tutup tanpa menyimpan perubahan");
        batal.setParent(buttons);
        batal.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event event) throws Exception {
                win.detach();
            }
        });

        try {
            Component parent = getParent();
            if (parent != null) {
                parent.appendChild(win);
            } else {
                appendChild(win);
            }
            win.setZindex(100000);
            win.doHighlighted();
        } catch (Exception e) {
            try {
                win.setVisible(true);
                win.onModal();
            } catch (Exception ex) {
                try {
                    Common.tampilErrorJikaAdmin(ex);
                } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3047");
                }
            }
        }
    }

    private void addManualInfoRow(Rows rows, String label, String value) {
        Row row = new Row();
        row.setParent(rows);
        Label l = new Label(label == null ? "" : label);
        l.setStyle("font-weight:bold; color:#334155;");
        l.setParent(row);
        Label v = new Label(value == null ? "" : value);
        v.setMultiline(true);
        v.setStyle("white-space:normal; word-break:break-word; color:#0f172a; line-height:15px;");
        v.setParent(row);
    }

    private String buildManualEditHelpText(Class propertyClass) {
        if (propertyClass == null) {
            return "Masukkan nilai baru sesuai tipe data field. Kosongkan jika nilai memang ingin dibuat kosong/null.";
        }
        if (Date.class.isAssignableFrom(propertyClass)) {
            return "Format tanggal yang disarankan: yyyy-MM-dd HH:mm:ss atau dd-MM-yyyy HH:mm:ss. Contoh: 2026-06-06 14:30:00.";
        }
        if (Boolean.class.equals(propertyClass) || Boolean.TYPE.equals(propertyClass)) {
            return "Masukkan true/false, ya/tidak, aktif/tidak, atau 1/0.";
        }
        if (Number.class.isAssignableFrom(propertyClass) || Integer.TYPE.equals(propertyClass) || Long.TYPE.equals(propertyClass)
                || Double.TYPE.equals(propertyClass) || Float.TYPE.equals(propertyClass) || Short.TYPE.equals(propertyClass)
                || Byte.TYPE.equals(propertyClass)) {
            return "Masukkan angka tanpa simbol mata uang. Untuk desimal bisa memakai titik atau koma.";
        }
        if (GeneralValueObject.class.isAssignableFrom(propertyClass)) {
            return "Field ini adalah relasi object. Masukkan ID data relasi yang akan dipakai. Kosongkan untuk menghapus relasi.";
        }
        if (propertyClass.isEnum()) {
            return "Field ini bertipe pilihan enum. Masukkan nama enum persis seperti yang dipakai di program.";
        }
        return "Masukkan nilai baru. Kosongkan jika nilai ingin dibuat kosong/null, selama tipe field mengizinkan.";
    }

    private String formatManualInputValue(Object value) {
        try {
            if (value == null) {
                return "";
            }
            if (value instanceof Date) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value);
            }
            if (value instanceof GeneralValueObject) {
                Object id = ((GeneralValueObject) value).getId();
                return id == null ? "" : id.toString();
            }
            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void confirmAndSaveManualValue(final MyWindow win, final GeneralValueObject currentObject,
            final String property, final String inputValue) throws Exception {
        MyMessageboxConfig.show("Simpan perubahan manual untuk field " + property + "?", "Konfirmasi Simpan",
                MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        int i = Integer.parseInt(event.getData().toString());
                        if (i == MyMessageboxConfig.OK) {
                            saveManualPropertyValue(currentObject, property, inputValue);
                            try {
                                win.detach();
                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3117");
                            }
                        }
                    }
                });
    }

    protected void saveManualPropertyValue(GeneralValueObject currentObject, String property, String inputValue) {
        Session session = null;
        Transaction tx = null;
        try {
            if (currentObject == null || currentObject.getId() == null || property == null || property.trim().length() == 0) {
                MyMessageboxConfig.show("Data aktif atau field belum lengkap. Perubahan tidak disimpan.");
                return;
            }
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            Class ownerClass = getHibernateClassSafely(currentObject);
            Object obj = session.get(ownerClass, currentObject.getId());
            if (obj == null) {
                MyMessageboxConfig.show("Data aktif tidak ditemukan. Perubahan tidak disimpan.");
                rollback(tx);
                return;
            }
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(ownerClass);
            if (meta == null || !hasProperty(property)) {
                MyMessageboxConfig.show("Metadata field tidak ditemukan. Perubahan tidak disimpan.");
                rollback(tx);
                return;
            }
            Class propertyClass = getPropertyClass(meta, property);
            Object converted = convertManualInputValue(session, propertyClass, property, inputValue);
            meta.setPropertyValue(obj, property, converted, EntityMode.POJO);
            // Tagihan.getAktif() adalah computed property; Hibernate memanggil getter
            // saat flush sehingga setAktif(true) di-override balik ke false oleh
            // logika bulanMulai. aktifkanmanual=true memaksa isRescued=true di getAktif()
            // → semua cek bisnis di-skip → validStatus=true.
            if (obj instanceof ais.database.model.sekolah.Tagihan
                    && "aktif".equals(property)
                    && Boolean.TRUE.equals(converted)) {
                meta.setPropertyValue(obj, "aktifkanmanual", Boolean.TRUE, EntityMode.POJO);
            }
            session.saveOrUpdate(obj);
            tx.commit();
            MyMessageboxConfig.show("Nilai field berhasil disimpan.");
            try {
                onSearchDefault(null);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3164");
            }
        } catch (Exception e) {
            rollback(tx);
            try {
                PesanFormalHelper.tampilkanGagalException("penyimpanan nilai field secara manual pada data revisi",
                        e,
                        new String[] {
                                "Periksa kembali apakah nilai yang diisikan sesuai dengan tipe data kolom aslinya (angka, tanggal, referensi ID, dsb.).",
                                "Pastikan baris data yang diedit belum dihapus atau diubah oleh pengguna lain.",
                                "Coba ulangi proses penyimpanan beberapa saat lagi.",
                                "Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
                        });
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3170");
            }
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3174");
            }
        } finally {
            closeSession(session);
        }
    }

    private Class getHibernateClassSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof org.hibernate.proxy.HibernateProxy) {
                org.hibernate.proxy.LazyInitializer initializer = ((org.hibernate.proxy.HibernateProxy) value)
                        .getHibernateLazyInitializer();
                if (initializer != null && initializer.getPersistentClass() != null) {
                    return initializer.getPersistentClass();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3193");
        }
        try {
            Class clazz = Hibernate.getClass(value);
            if (clazz != null) {
                return normalizeHibernateClass(clazz);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3200");
        }
        return normalizeHibernateClass(value.getClass());
    }

    private Class normalizeHibernateClass(Class clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
            if (meta != null) {
                return clazz;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3214");
        }
        String className = clazz.getName();
        String normalizedName = normalizeHibernateClassName(className);
        if (normalizedName != null && !normalizedName.equals(className)) {
            try {
                Class normalizedClass = Class.forName(normalizedName);
                ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(normalizedClass);
                if (meta != null) {
                    return normalizedClass;
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3225");
            }
        }
        return clazz;
    }

    private String normalizeHibernateClassName(String className) {
        if (className == null) {
            return null;
        }
        int proxyIndex = className.indexOf("_$$_");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }
        proxyIndex = className.indexOf("$$");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }
        return className;
    }

    private Serializable getEntityIdentifierSafely(Session session, Object entity, Class clazz) {
        if (entity == null) {
            return null;
        }
        try {
            if (entity instanceof org.hibernate.proxy.HibernateProxy) {
                org.hibernate.proxy.LazyInitializer initializer = ((org.hibernate.proxy.HibernateProxy) entity)
                        .getHibernateLazyInitializer();
                if (initializer != null && initializer.getIdentifier() instanceof Serializable) {
                    return (Serializable) initializer.getIdentifier();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3258");
        }
        try {
            if (entity instanceof GeneralValueObject && ((GeneralValueObject) entity).getId() != null) {
                return ((GeneralValueObject) entity).getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3264");
        }
        try {
            if (clazz == null) {
                clazz = getHibernateClassSafely(entity);
            }
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
            if (meta != null) {
                Object id = meta.getIdentifier(entity, EntityMode.POJO);
                if (id instanceof Serializable) {
                    return (Serializable) id;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3277");
        }
        return null;
    }

    private String buildEntityKey(Class clazz, Serializable id) {
        Class c = normalizeHibernateClass(clazz);
        return (c == null ? "" : c.getName()) + "-" + (id == null ? "" : String.valueOf(id));
    }

    private Object safeSessionGet(Session session, Class clazz, Serializable id) {
        if (session == null || clazz == null || id == null) {
            return null;
        }
        try {
            Class mappedClass = normalizeHibernateClass(clazz);
            return session.get(mappedClass == null ? clazz : mappedClass, id);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRestoreIgnoredRelationProperty(String property) {
        if (property == null) {
            return true;
        }
        return "json".equalsIgnoreCase(property) || "class".equalsIgnoreCase(property)
                || "copyDari".equalsIgnoreCase(property);
    }

    private boolean isNullableProperty(ClassMetadata meta, int index) {
        try {
            Method m = meta.getClass().getMethod("getPropertyNullability", new Class[0]);
            Object value = m.invoke(meta, new Object[0]);
            if (value instanceof boolean[]) {
                boolean[] nullability = (boolean[]) value;
                if (index >= 0 && index < nullability.length) {
                    return nullability[index];
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3317");
        }
        return true;
    }

    private void setPropertyValueSafely(ClassMetadata meta, Object entity, String property, Object value) {
        try {
            if (meta != null && entity != null && property != null) {
                meta.setPropertyValue(entity, property, value, EntityMode.POJO);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3327");
        }
    }

    private void addDeferredRelation(List<DeferredRelation> deferredRelations, Class ownerClass, Serializable ownerId,
            String property, Class relationClass, Serializable relationId) {
        if (deferredRelations == null || ownerClass == null || ownerId == null || property == null
                || relationClass == null || relationId == null) {
            return;
        }
        String key = buildEntityKey(ownerClass, ownerId) + "." + property + "->" + buildEntityKey(relationClass, relationId);
        for (int i = 0; i < deferredRelations.size(); i++) {
            DeferredRelation existing = deferredRelations.get(i);
            if (existing != null && key.equals(existing.key)) {
                return;
            }
        }
        deferredRelations.add(new DeferredRelation(key, normalizeHibernateClass(ownerClass), ownerId, property,
                normalizeHibernateClass(relationClass), relationId));
    }

    private static class DeferredRelation {
        private String key;
        private Class ownerClass;
        private Serializable ownerId;
        private String property;
        private Class relationClass;
        private Serializable relationId;

        private DeferredRelation(String key, Class ownerClass, Serializable ownerId, String property, Class relationClass,
                Serializable relationId) {
            this.key = key;
            this.ownerClass = ownerClass;
            this.ownerId = ownerId;
            this.property = property;
            this.relationClass = relationClass;
            this.relationId = relationId;
        }

        private String describe() {
            return key == null ? "" : key;
        }
    }

    private boolean isFatalRestoreException(Throwable throwable) {
        String msg = errorToString(throwable).toLowerCase();
        return msg.indexOf("transaction is aborted") >= 0 || msg.indexOf("assertionfailure") >= 0
                || msg.indexOf("genericjdbcexception") >= 0 || msg.indexOf("constraint") >= 0
                || msg.indexOf("foreign key") >= 0 || msg.indexOf("could not insert") >= 0
                || msg.indexOf("could not update") >= 0 || msg.indexOf("could not execute") >= 0;
    }

    private Class getPropertyClass(ClassMetadata meta, String property) {
        try {
            org.hibernate.type.Type type = meta.getPropertyType(property);
            if (type != null && type.getReturnedClass() != null) {
                return type.getReturnedClass();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3385");
        }
        return String.class;
    }

    private Object convertManualInputValue(Session session, Class propertyClass, String property, String inputValue) throws Exception {
        String text = inputValue == null ? "" : inputValue.trim();
        if (propertyClass == null) {
            propertyClass = String.class;
        }
        if (text.length() == 0 && !propertyClass.isPrimitive()) {
            return null;
        }
        if (String.class.equals(propertyClass)) {
            return inputValue == null ? "" : inputValue;
        }
        if (Integer.class.equals(propertyClass) || Integer.TYPE.equals(propertyClass)) {
            return Integer.valueOf(normalizeNumberText(text).intValue());
        }
        if (Long.class.equals(propertyClass) || Long.TYPE.equals(propertyClass)) {
            return Long.valueOf(normalizeNumberText(text).longValue());
        }
        if (Double.class.equals(propertyClass) || Double.TYPE.equals(propertyClass)) {
            return Double.valueOf(normalizeNumberText(text).doubleValue());
        }
        if (Float.class.equals(propertyClass) || Float.TYPE.equals(propertyClass)) {
            return Float.valueOf(normalizeNumberText(text).floatValue());
        }
        if (Short.class.equals(propertyClass) || Short.TYPE.equals(propertyClass)) {
            return Short.valueOf(normalizeNumberText(text).shortValue());
        }
        if (Byte.class.equals(propertyClass) || Byte.TYPE.equals(propertyClass)) {
            return Byte.valueOf(normalizeNumberText(text).byteValue());
        }
        if (BigDecimal.class.equals(propertyClass)) {
            return normalizeNumberText(text);
        }
        if (BigInteger.class.equals(propertyClass)) {
            return normalizeNumberText(text).toBigInteger();
        }
        if (Boolean.class.equals(propertyClass) || Boolean.TYPE.equals(propertyClass)) {
            return Boolean.valueOf(parseBooleanText(text));
        }
        if (Date.class.isAssignableFrom(propertyClass)) {
            return parseManualDate(text);
        }
        if (Character.class.equals(propertyClass) || Character.TYPE.equals(propertyClass)) {
            return text.length() == 0 ? Character.valueOf(' ') : Character.valueOf(text.charAt(0));
        }
        if (propertyClass.isEnum()) {
            return Enum.valueOf(propertyClass, text);
        }
        if (GeneralValueObject.class.isAssignableFrom(propertyClass)) {
            Serializable id = convertRelationId(session, propertyClass, text);
            if (id == null) {
                return null;
            }
            Object related = session.get(propertyClass, id);
            if (related == null) {
                try {
                    MyMessageboxConfig.show("Data relasi " + propertyClass.getSimpleName() + " dengan ID " + text
                            + " belum ada di database. Nilai relasi dikosongkan dulu agar tidak membuat transaksi gagal."
                            + " Gunakan Restore Revisi/Deep Restore untuk mengembalikan data relasi tersebut jika diperlukan.");
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3448");
                }
                return null;
            }
            return related;
        }
        throw new IllegalArgumentException("Tipe field " + propertyClass.getName() + " belum didukung untuk edit manual. Gunakan tombol Pakai untuk nilai revisi, atau tambahkan converter khusus bila diperlukan.");
    }

    private BigDecimal normalizeNumberText(String text) {
        String n = text == null ? "" : text.trim();
        n = n.replace(" ", "");
        if (n.indexOf('.') >= 0 && n.indexOf(',') >= 0) {
            n = n.replace(".", "").replace(',', '.');
        } else if (n.indexOf(',') >= 0) {
            n = n.replace(',', '.');
        }
        if (n.length() == 0 || "-".equals(n)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(n);
    }

    private boolean parseBooleanText(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim().toLowerCase();
        return "true".equals(t) || "1".equals(t) || "ya".equals(t) || "y".equals(t)
                || "aktif".equals(t) || "on".equals(t) || "yes".equals(t);
    }

    private Date parseManualDate(String text) throws Exception {
        String[] patterns = new String[] { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
                "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm", "dd-MM-yyyy",
                "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy" };
        for (int i = 0; i < patterns.length; i++) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(patterns[i]);
                sdf.setLenient(false);
                return sdf.parse(text);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3489");
            }
        }
        throw new IllegalArgumentException("Format tanggal belum dikenali. Gunakan yyyy-MM-dd HH:mm:ss atau dd-MM-yyyy HH:mm:ss.");
    }

    private Serializable convertRelationId(Session session, Class relationClass, String text) throws Exception {
        if (text == null || text.trim().length() == 0) {
            return null;
        }
        ClassMetadata relMeta = HibernateUtil.getSessionFactory().getClassMetadata(relationClass);
        Class idClass = Long.class;
        try {
            if (relMeta != null && relMeta.getIdentifierType() != null && relMeta.getIdentifierType().getReturnedClass() != null) {
                idClass = relMeta.getIdentifierType().getReturnedClass();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3505");
        }
        Object id;
        if (String.class.equals(idClass)) {
            id = text.trim();
        } else if (Integer.class.equals(idClass) || Integer.TYPE.equals(idClass)) {
            id = Integer.valueOf(normalizeNumberText(text).intValue());
        } else if (Long.class.equals(idClass) || Long.TYPE.equals(idClass)) {
            id = Long.valueOf(normalizeNumberText(text).longValue());
        } else if (BigInteger.class.equals(idClass)) {
            id = normalizeNumberText(text).toBigInteger();
        } else if (BigDecimal.class.equals(idClass)) {
            id = normalizeNumberText(text);
        } else {
            id = Long.valueOf(normalizeNumberText(text).longValue());
        }
        return (Serializable) id;
    }

    protected void restoreOneProperty(GeneralValueObject currentObject, String property, Object value) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            Class ownerClass = getHibernateClassSafely(currentObject);
            Object obj = session.get(ownerClass, currentObject.getId());
            if (obj instanceof GeneralValueObject) {
                ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(ownerClass);
                if (meta != null && property != null) {
                    meta.setPropertyValue(obj, property, value, EntityMode.POJO);
                    if (obj instanceof ais.database.model.sekolah.Tagihan
                            && "aktif".equals(property)
                            && Boolean.TRUE.equals(value)) {
                        meta.setPropertyValue(obj, "aktifkanmanual", Boolean.TRUE, EntityMode.POJO);
                    }
                    session.saveOrUpdate(obj);
                }
            }
            tx.commit();
            MyMessageboxConfig.show("Field berhasil dikembalikan.");
            onSearchDefault(null);
        } catch (Exception e) {
            rollback(tx);
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSession(session);
        }
    }

    protected void restoreWithConfirm(final Serializable revisionObject) throws Exception {
        MyMessageboxConfig.show("Apakah yakin ingin mengembalikan data sesuai revisi ini?", "Konfirmasi Restore",
                MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        int i = Integer.parseInt(event.getData().toString());
                        if (i == MyMessageboxConfig.OK) {
                            restoreRevisionObject(revisionObject, true);
                        }
                    }
                });
    }

    private boolean isAdminUser() {
        try {
            return Common.getApakahAdmin();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Buka formulir "Ubah &amp; Restore": menampilkan SELURUH kolom Class/Object revisi ini sebagai
     * input teks bebas (pra-isi nilai revisi). Setelah pengguna mengubah, tombol Simpan menerapkan
     * nilai ke objek dengan ID tersebut dan memperbaruinya ke database. Kolom relasi diisi dengan ID
     * data relasi (dikonversi ulang via {@code convertManualInputValue}); kolom koleksi hanya-lihat.
     */
    protected void bukaFormEditRestore(final Serializable revisionObject) {
        try {
            if (revisionObject == null || classMetadata == null) {
                restoreWithConfirm(revisionObject);
                return;
            }
            final ClassMetadata meta = classMetadata;
            final Object idVal = getEntityIdValue(revisionObject);

            final MyWindow win = new MyWindow();
            win.setTitle("Ubah & Restore Data - " + getEntityClassLabel() + (idVal == null ? "" : " (ID " + idVal + ")"));
            win.setWidth("74%");
            win.setHeight("88%");
            win.setClosable(true);
            win.setSizable(true);
            win.setBorder("normal");
            try { win.setContentStyle("overflow:auto;background:#f6f8fb;"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.bukaFormEditRestore.style"); }

            Vbox root = new Vbox();
            root.setWidth("100%");
            root.setStyle("padding:12px 14px;box-sizing:border-box;gap:8px;");
            root.setParent(win);

            appendHtml(root, "<div style='padding:14px 16px;border-radius:14px;color:#fff;"
                    + "background:linear-gradient(135deg,var(--ais-theme-primary,#1d4ed8),var(--ais-theme-accent,#06b6d4));'>"
                    + "<div style='font-size:11px;letter-spacing:.1em;text-transform:uppercase;font-weight:800;opacity:.85;'>Ubah & Restore</div>"
                    + "<div style='font-size:18px;font-weight:900;margin-top:4px;'>" + escapeHtml(getEntityClassLabel())
                    + (idVal == null ? "" : " &mdash; ID " + escapeHtml(String.valueOf(idVal))) + "</div>"
                    + "<div style='font-size:12px;opacity:.92;margin-top:6px;line-height:1.5;'>Semua kolom berisi nilai revisi ini (teks bebas). "
                    + "Ubah seperlunya lalu klik <b>Simpan</b> untuk memperbarui data dengan ID ini ke database. "
                    + "Kolom relasi diisi dengan <b>ID</b> data relasinya; kolom koleksi tidak dapat diubah dari sini.</div></div>");

            MyGrid grid = new MyGrid();
            grid.setWidth("100%");
            grid.setStyle("margin-top:6px;background:#fff;border-radius:12px;");
            Columns cols = new Columns();
            cols.setParent(grid);
            MyColumnConfig c1 = new MyColumnConfig();
            c1.setWidth("28%");
            c1.setParent(cols);
            MyColumnConfig c2 = new MyColumnConfig();
            c2.setParent(cols);
            Rows formRows = new Rows();
            formRows.setParent(grid);
            grid.setParent(root);

            final java.util.LinkedHashMap<String, Textbox> inputs = new java.util.LinkedHashMap<String, Textbox>();
            final java.util.LinkedHashSet<String> editable = new java.util.LinkedHashSet<String>();

            String idName = meta.getIdentifierPropertyName();
            barisFormEdit(formRows, (idName == null ? "id" : idName) + " (ID)", idVal == null ? "" : String.valueOf(idVal),
                    false, null, null);

            String[] props = meta.getPropertyNames();
            for (int i = 0; props != null && i < props.length; i++) {
                String prop = props[i];
                org.hibernate.type.Type t = null;
                try { t = meta.getPropertyType(prop); } catch (Exception e) { t = null; }
                boolean koleksi = t != null && t.isCollectionType();
                boolean relasi = t != null && t.isEntityType();
                Object val = null;
                try {
                    val = meta.getPropertyValue(revisionObject, prop, EntityMode.POJO);
                } catch (Exception e) {
                    // FIX: sebelumnya exception di sini ditelan TANPA log sama
                    // sekali -- properti relasi (mis. bankHost/calonSiswa) yang
                    // gagal di-resolve (proxy dari sesi Envers yang sudah
                    // tertutup) jadi kosong TANPA jejak diagnosa apapun. Catat
                    // supaya kasus field relasi kosong di masa depan bisa
                    // ditelusuri akar penyebabnya (bukan dugaan/asumsi).
                    ais.common.ErrorAuditUtil.record(e,
                            "GenericRevisiHelper.bukaFormEditRestore: gagal ambil nilai properti '" + prop
                                    + "' dari revisionObject utk " + getEntityClassLabel());
                    val = null;
                }
                String display = formatValueForEdit(meta, prop, val);
                boolean bolehUbah = !koleksi;
                String label = prop + (relasi ? " (relasi: ID)" : "") + (koleksi ? " (koleksi)" : "");
                barisFormEdit(formRows, label, display, bolehUbah, inputs, prop);
                if (bolehUbah) {
                    editable.add(prop);
                }
            }

            Hbox buttons = new Hbox();
            buttons.setStyle("margin-top:10px;");
            buttons.setParent(root);

            MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
            simpan.setStyle("font-weight:bold;color:#fff;background:#2563eb;border-radius:10px;padding:6px 16px;");
            simpan.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event e) throws Exception {
                    MyMessageboxConfig.show(
                            "Simpan perubahan & perbarui data ID " + (idVal == null ? "-" : idVal) + " ke database?",
                            "Konfirmasi Simpan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                            MyMessageboxConfig.QUESTION, new EventListener() {
                                public void onEvent(Event ev) throws Exception {
                                    if (Integer.parseInt(ev.getData().toString()) == MyMessageboxConfig.OK) {
                                        simpanFormEditRestore(win, revisionObject, inputs, editable);
                                    }
                                }
                            });
                }
            });
            simpan.setParent(buttons);

            MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
            batal.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event e) throws Exception {
                    win.detach();
                }
            });
            batal.setParent(buttons);

            Component parent = getParent();
            if (parent != null) {
                parent.appendChild(win);
            } else {
                appendChild(win);
            }
            win.setZindex(100000);
            win.doHighlighted();
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) GenericRevisiHelper.bukaFormEditRestore"); }
        }
    }

    private Textbox barisFormEdit(Rows rows, String label, String value, boolean bolehUbah,
            java.util.LinkedHashMap<String, Textbox> inputs, String prop) {
        Row r = new Row();
        r.setValign("top");
        r.setParent(rows);
        Label lbl = new Label(label);
        lbl.setStyle("font-size:12px;font-weight:700;color:#334155;");
        lbl.setParent(r);
        Textbox tb = new Textbox(value == null ? "" : value);
        tb.setWidth("98%");
        try {
            tb.setMultiline(true);
            int len = value == null ? 0 : value.length();
            tb.setRows(len > 140 ? 4 : (len > 55 ? 2 : 1));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.barisFormEdit"); }
        if (!bolehUbah) {
            tb.setReadonly(true);
            tb.setStyle("background:#f1f5f9;color:#64748b;");
        }
        tb.setParent(r);
        if (bolehUbah && inputs != null && prop != null) {
            inputs.put(prop, tb);
        }
        return tb;
    }

    private String formatValueForEdit(ClassMetadata meta, String prop, Object val) {
        if (val == null) {
            return "";
        }
        try {
            org.hibernate.type.Type t = meta.getPropertyType(prop);
            if (t != null && t.isCollectionType()) {
                return "[koleksi — tidak diubah dari sini]";
            }
            if (t != null && t.isEntityType()) {
                Object relId = getEntityIdValue(val);
                return relId == null ? "" : String.valueOf(relId);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.formatValueForEdit"); }
        if (val instanceof java.util.Date) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((java.util.Date) val);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.formatValueForEdit.date"); }
        }
        return String.valueOf(val);
    }

    /**
     * Terapkan nilai teks bebas dari formulir ke objek dengan ID revisi, lalu simpan ke database dalam
     * satu transaksi. Bila data aktif dengan ID tsb belum ada, snapshot revisi dipakai sebagai baris baru.
     */
    protected void simpanFormEditRestore(final MyWindow win, final Serializable revisionObject,
            final java.util.LinkedHashMap<String, Textbox> inputs, final java.util.LinkedHashSet<String> editable) {
        Session session = null;
        Transaction tx = null;
        try {
            Class ownerClass = getHibernateClassSafely(revisionObject);
            if (ownerClass == null) {
                ownerClass = entityClass;
            }
            if (ownerClass == null) {
                MyMessageboxConfig.show("Class data tidak dikenali. Perubahan tidak disimpan.");
                return;
            }
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(ownerClass);
            if (meta == null) {
                MyMessageboxConfig.show("Metadata class tidak ditemukan. Perubahan tidak disimpan.");
                return;
            }
            final Object idVal = getEntityIdValue(revisionObject);
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Object target = null;
            if (idVal instanceof Serializable) {
                target = session.get(ownerClass, (Serializable) idVal);
            }
            boolean insertBaru = false;
            if (target == null) {
                // Data aktif dengan ID ini belum ada (mis. revisi "Tambah"/data terhapus) → jadikan baris baru.
                target = revisionObject;
                insertBaru = true;
            }

            for (String prop : editable) {
                Textbox tb = inputs.get(prop);
                if (tb == null) {
                    continue;
                }
                try {
                    Class pc = getPropertyClass(meta, prop);
                    Object converted = convertManualInputValue(session, pc, prop, tb.getValue());
                    meta.setPropertyValue(target, prop, converted, EntityMode.POJO);
                } catch (Exception exField) {
                    rollback(tx);
                    MyMessageboxConfig.show("Nilai kolom '" + prop + "' tidak valid: " + safeExceptionMessage(exField)
                            + "\nPerbaiki lalu simpan lagi.");
                    return;
                }
            }

            // Simpan memakai mekanisme yang SAMA dengan Restore:
            // - Data belum ada (insertBaru) → session.replicate(OVERWRITE) => INSERT dengan MEMPERTAHANKAN
            //   ID asli dari objek revisi (bukan generate baru). saveOrUpdate langsung akan GAGAL di sini
            //   ("Batch update ... row count 0; expected 1") karena Hibernate mengira UPDATE untuk baris
            //   yang belum ada.
            // - Data sudah ada → merge => UPDATE. Nilai form yang sudah di-set di atas ikut tersimpan.
            // Relasi disiapkan/di-defer via prepareEntityRelationsBeforeSave (di dalam saveOrReplicate).
            java.util.List<DeferredRelation> deferred = new ArrayList<DeferredRelation>();
            saveOrReplicate(session, target, null, deferred);
            applyDeferredRelations(session, deferred, null);
            tx.commit();

            // Pastikan nilai form benar-benar tersimpan: baca ulang (session baru) & terapkan lagi bila
            // ada selisih (mis. saat baris baru di-insert lewat replicate, sebagian setter ber-side-effect
            // bisa menimpa nilai). Idempoten dan aman.
            verifikasiDanTerapkanUlang(ownerClass, meta, idVal, inputs, editable);

            try { win.detach(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) GenericRevisiHelper.simpanFormEditRestore.detach"); }
            MyMessageboxConfig.show("Data berhasil " + (insertBaru ? "di-INSERT (ID asli " + (idVal == null ? "-" : idVal)
                    + " dipertahankan) lalu diperbarui" : "diperbarui")
                    + " ke database (ID " + (idVal == null ? "-" : idVal) + ").");
            try { onSearchDefault(null); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) GenericRevisiHelper.simpanFormEditRestore.refresh"); }
        } catch (Exception e) {
            rollback(tx);
            try {
                PesanFormalHelper.tampilkanGagalException("penyimpanan perubahan manual data revisi (edit/restore)",
                        e,
                        new String[] {
                                "Periksa kembali nilai yang diisikan pada setiap field, pastikan tipe datanya sesuai dengan kolom aslinya (angka, tanggal, referensi ID, dsb).",
                                "Pastikan data induk (baris asli) belum dihapus atau diubah oleh pengguna lain saat form ini masih terbuka.",
                                "Coba ulangi proses penyimpanan beberapa saat lagi.",
                                "Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
                        });
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) GenericRevisiHelper.simpanFormEditRestore.msg"); }
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) GenericRevisiHelper.simpanFormEditRestore.err"); }
        } finally {
            closeSession(session);
        }
    }

    /**
     * Tahap KEDUA (transaksi terpisah, setelah insert/replicate berhasil): baca ulang baris dengan ID
     * yang SAMA lalu terapkan lagi seluruh nilai form sebagai UPDATE biasa. Menjamin nilai yang diketik
     * pengguna benar-benar tersimpan (mis. bila proses insert/replicate memicu setter ber-side-effect
     * yang menimpa sebagian nilai). Idempoten dan aman; kegagalan di sini tidak membatalkan insert utama.
     */
    private void verifikasiDanTerapkanUlang(Class ownerClass, ClassMetadata meta, Object idVal,
            java.util.LinkedHashMap<String, Textbox> inputs, java.util.LinkedHashSet<String> editable) {
        if (ownerClass == null || meta == null || !(idVal instanceof Serializable)) {
            return;
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            Object obj = session.get(ownerClass, (Serializable) idVal);
            if (obj == null) {
                rollback(tx);
                return;
            }
            for (String prop : editable) {
                Textbox tb = inputs.get(prop);
                if (tb == null) {
                    continue;
                }
                try {
                    Object converted = convertManualInputValue(session, getPropertyClass(meta, prop), prop, tb.getValue());
                    meta.setPropertyValue(obj, prop, converted, EntityMode.POJO);
                } catch (Exception ig) {
                    ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) GenericRevisiHelper.verifikasiDanTerapkanUlang.field");
                }
            }
            session.saveOrUpdate(obj);
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            ais.common.ErrorAuditUtil.record(e, "auto-audit GenericRevisiHelper.verifikasiDanTerapkanUlang");
        } finally {
            closeSession(session);
        }
    }

    protected void deleteDataIniWithConfirm(final Serializable revisionObject) throws Exception {
        if (!isAdminUser()) {
            MyMessageboxConfig.show("Fungsi Hapus Data Ini hanya tersedia untuk admin.");
            return;
        }
        final Object id = getEntityIdValue(revisionObject);
        String pesan = "Hapus data aktif ini dari database?\n\n"
                + "Class : " + getEntityClassLabel() + "\n"
                + "ID    : " + (id == null ? "tidak diketahui" : String.valueOf(id)) + "\n\n"
                + "Riwayat revisi tetap tersimpan sehingga data masih bisa direstore dari halaman ini. "
                + "Pastikan tidak ada data lain yang masih memakai data ini sebagai relasi.";
        MyMessageboxConfig.show(pesan, "Konfirmasi Hapus Data Ini",
                MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.EXCLAMATION, new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        int i = Integer.parseInt(event.getData().toString());
                        if (i == MyMessageboxConfig.OK) {
                            deleteDataIni(revisionObject);
                        }
                    }
                });
    }

    protected void deleteDataIni(Serializable revisionObject) {
        Session session = null;
        Transaction tx = null;
        try {
            if (!isAdminUser()) {
                MyMessageboxConfig.show("Fungsi Hapus Data Ini hanya tersedia untuk admin.");
                return;
            }
            if (revisionObject == null) {
                MyMessageboxConfig.show("Data revisi kosong. Data aktif tidak dapat dihapus.");
                return;
            }

            Class ownerClass = entityClass == null ? getHibernateClassSafely(revisionObject) : normalizeHibernateClass(entityClass);
            if (ownerClass == null) {
                MyMessageboxConfig.show("Class data belum dikenali. Data aktif tidak dapat dihapus.");
                return;
            }

            session = HibernateUtil.getSessionFactory().openSession();
            Serializable id = null;
            Object rawId = getEntityIdValue(revisionObject);
            if (rawId instanceof Serializable) {
                id = (Serializable) rawId;
            }
            if (id == null) {
                id = getEntityIdentifierSafely(session, revisionObject, ownerClass);
            }
            if (id == null) {
                MyMessageboxConfig.show("ID data belum ditemukan. Data aktif tidak dapat dihapus.");
                return;
            }

            Object activeObject = session.get(ownerClass, id);
            if (activeObject == null) {
                MyMessageboxConfig.show("Data aktif sudah tidak ada di database. Tidak ada data yang dihapus.");
                return;
            }

            tx = session.beginTransaction();
            session.delete(activeObject);
            session.flush();
            tx.commit();

            MyMessageboxConfig.show("Data berhasil dihapus. Riwayat revisi tetap tersimpan dan bisa direstore kembali jika diperlukan.");
            try {
                if (callback != null) {
                    callback.onEvent(new Event("onDeleteDataIni", GenericRevisiHelper.this, revisionObject));
                }
            } catch (Exception e) {
                try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3647");}
            }
            try {
                int selectedTab = getSelectedTabIndex();
                if (selectedTab == 2) {
                    onSearchAllData(null);
                } else {
                    onSearchDefault(null);
                }
            } catch (Exception e) {
                try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3657");}
            }
        } catch (Exception e) {
            rollback(tx);
            try {
                MyMessageboxConfig.show("Data belum dapat dihapus: " + safeExceptionMessage(e)
                        + "\n\nKemungkinan data masih dipakai oleh relasi/foreign key lain. Hapus atau lepaskan relasi tersebut terlebih dahulu, lalu ulangi.");
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3664");
            }
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3668");
            }
        } finally {
            closeSession(session);
        }
    }

    /**
     * Restore satu object revisi dengan progress bar, log otomatis, dan summary.
     * Proses dijalankan pada thread terpisah agar tampilan ZK tetap dapat memperbarui progress.
     */
    public void restoreRevisionObject(final Serializable revisionObject, final boolean deep) {
        final RestoreProgress progress = new RestoreProgress("Restore Revisi", entityClass);
        progress.setPrimaryTotal(1);
        final MyWindow progressWindow = showRestoreProgressWindow(progress, true);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                doRestoreRevisionObject(revisionObject, deep, progress);
            }
        }, "ais-restore-revisi-" + (entityClass == null ? "data" : entityClass.getSimpleName()));
        thread.setDaemon(true);
        thread.start();
    }

    private void doRestoreRevisionObject(Serializable revisionObject, boolean deep, RestoreProgress progress) {
        Session session = null;
        Transaction tx = null;
        boolean committed = false;
        List<DeferredRelation> deferredRelations = new ArrayList<DeferredRelation>();
        try {
            progress.start("Mulai restore satu data revisi.");
            if (revisionObject == null) {
                throw new IllegalArgumentException("Data revisi yang akan direstore kosong/null.");
            }

            session = HibernateUtil.getSessionFactory().openSession();
            AuditReader reader = AuditReaderFactory.get(session);
            tx = session.beginTransaction();

            if (deep) {
                progress.setStatus("Mengecek dan merestore data relasi pendukung...");
                restoreDependenciesRecursively(session, reader, revisionObject, new HashSet<String>(), progress,
                        deferredRelations);
            }

            progress.setStatus("Menyimpan data utama hasil restore...");
            progress.beginItem("DATA UTAMA", revisionObject);
            saveOrReplicate(session, revisionObject, progress, deferredRelations);
            applyDeferredRelations(session, deferredRelations, progress);
            progress.itemSuccess("Data utama berhasil diproses dan menunggu commit transaksi.");
            progress.markPrimaryProcessed();

            afterRestoreInTransaction(session, reader, revisionObject);

            tx.commit();
            committed = true;
            progress.finishSuccess("Restore selesai dan transaksi berhasil di-commit.");
        } catch (Exception e) {
            rollback(tx);
            progress.failGlobal(e, committed ? "Terjadi error setelah commit." : "Restore gagal. Transaksi sudah di-rollback agar data tidak setengah tersimpan.");
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3731");
            }
        } finally {
            closeSession(session);
        }
    }

    /**
     * Hook yang dipanggil setelah saveOrReplicate dan applyDeferredRelations, tapi SEBELUM tx.commit().
     * Subclass dapat override untuk memperbaiki field yang salah/null setelah restore generik.
     * Sesi masih terbuka dan transaksi masih aktif sehingga saveOrUpdate dapat dilakukan.
     */
    protected void afterRestoreInTransaction(Session session, AuditReader reader, Object entity) throws Exception {
    }

    private void restoreDependenciesRecursively(Session session, AuditReader reader, Object entityObj, Set<String> processedIds) {
        restoreDependenciesRecursively(session, reader, entityObj, processedIds, null, null);
    }

    private void restoreDependenciesRecursively(Session session, AuditReader reader, Object entityObj, Set<String> processedIds,
            RestoreProgress progress) {
        restoreDependenciesRecursively(session, reader, entityObj, processedIds, progress, null);
    }

    /**
     * Restore relasi pendukung dengan cara membaca metadata Hibernate, bukan memanggil semua getter.
     * Cara lama memanggil semua method get* sehingga method non-property seperti getJson() ikut diproses
     * dan relasi lazy proxy Javassist dipakai sebagai class Hibernate. Akibatnya restore dapat gagal dengan
     * "Unknown entity ..._$$_javassist" atau transaksi menjadi aborted sebelum data utama disimpan.
     */
    private void restoreDependenciesRecursively(Session session, AuditReader reader, Object entityObj, Set<String> processedIds,
            RestoreProgress progress, List<DeferredRelation> deferredRelations) {
        if (entityObj == null || processedIds == null) {
            return;
        }

        Class ownerClass = getHibernateClassSafely(entityObj);
        Serializable ownerId = getEntityIdentifierSafely(session, entityObj, ownerClass);
        if (ownerClass == null || ownerId == null) {
            return;
        }

        String ownerKey = buildEntityKey(ownerClass, ownerId);
        if (processedIds.contains(ownerKey)) {
            return;
        }
        processedIds.add(ownerKey);

        ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(ownerClass);
        if (meta == null) {
            return;
        }

        String[] names = meta.getPropertyNames();
        for (int i = 0; i < names.length; i++) {
            String property = names[i];
            if (property == null || isRestoreIgnoredRelationProperty(property)) {
                continue;
            }
            try {
                org.hibernate.type.Type type = meta.getPropertyType(property);
                if (type == null || !type.isEntityType()) {
                    continue;
                }

                Class propertyClass = null;
                try {
                    propertyClass = type.getReturnedClass();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3799");
                }
                if (propertyClass != null && !GeneralValueObject.class.isAssignableFrom(propertyClass)) {
                    continue;
                }

                Object relationObject = meta.getPropertyValue(entityObj, property, EntityMode.POJO);
                if (!(relationObject instanceof GeneralValueObject)) {
                    continue;
                }

                Class relationClass = getHibernateClassSafely(relationObject);
                if (relationClass == null || !GeneralValueObject.class.isAssignableFrom(relationClass)) {
                    relationClass = propertyClass;
                }
                if (relationClass == null || !GeneralValueObject.class.isAssignableFrom(relationClass)) {
                    continue;
                }

                Serializable relationId = getEntityIdentifierSafely(session, relationObject, relationClass);
                if (relationId == null) {
                    continue;
                }

                String relationKey = buildEntityKey(relationClass, relationId);
                Object existing = safeSessionGet(session, relationClass, relationId);
                if (existing != null) {
                    setPropertyValueSafely(meta, entityObj, property, existing);
                    continue;
                }

                if (processedIds.contains(relationKey)) {
                    if (deferredRelations != null) {
                        addDeferredRelation(deferredRelations, ownerClass, ownerId, property, relationClass, relationId);
                        setPropertyValueSafely(meta, entityObj, property, null);
                        if (progress != null) {
                            progress.appendLog("Relasi siklik ditunda sementara: " + ownerKey + "." + property
                                    + " -> " + relationKey + ". Relasi akan diisi ulang setelah object utama tersedia.");
                        }
                    }
                    continue;
                }

                if (progress != null) {
                    progress.addDynamicTotal(1);
                    progress.appendLog("Data relasi belum ada di database: " + relationKey
                            + ". Mencari revisi terakhir relasi tersebut.");
                }

                Object latest = findLatestAuditEntity(reader, relationClass, relationId, null);
                if (latest != null) {
                    restoreDependenciesRecursively(session, reader, latest, processedIds, progress, deferredRelations);
                    if (progress != null) {
                        progress.beginItem("DATA RELASI", latest);
                    }
                    saveOrReplicate(session, latest, progress, deferredRelations);
                    Object reloaded = safeSessionGet(session, relationClass, relationId);
                    if (reloaded != null) {
                        setPropertyValueSafely(meta, entityObj, property, reloaded);
                    } else if (deferredRelations != null) {
                        addDeferredRelation(deferredRelations, ownerClass, ownerId, property, relationClass, relationId);
                        setPropertyValueSafely(meta, entityObj, property, null);
                    }
                    if (progress != null) {
                        progress.itemSuccess("Relasi berhasil diproses: " + relationKey);
                    }
                } else {
                    if (deferredRelations != null) {
                        addDeferredRelation(deferredRelations, ownerClass, ownerId, property, relationClass, relationId);
                        setPropertyValueSafely(meta, entityObj, property, null);
                    }
                    if (progress != null) {
                        progress.itemFailed("Relasi tidak memiliki data audit yang bisa direstore: " + relationKey, null);
                    }
                }
            } catch (Exception e) {
                if (progress != null) {
                    progress.itemFailed("Gagal restore relasi property " + property + " pada " + ownerKey, e);
                }
                if (isFatalRestoreException(e)) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Object findLatestAuditEntity(AuditReader reader, Class clazz, Serializable id, Date fromDate) {
        try {
            Class mappedClass = normalizeHibernateClass(clazz);
            if (mappedClass == null) {
                mappedClass = clazz;
            }
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(mappedClass, false, true);
            query.add(AuditEntity.id().eq(id));
            if (fromDate != null) {
                try {
                    query.add(AuditEntity.revisionProperty("timestamp").ge(Long.valueOf(fromDate.getTime())));
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3896");
                }
            }
            query.addOrder(AuditEntity.revisionNumber().desc());
            query.setMaxResults(20);
            List results = query.getResultList();
            for (int i = 0; i < results.size(); i++) {
                Object entity = extractEntity(results.get(i));
                RevisionType type = extractRevisionType(results.get(i));
                if (entity != null && type != RevisionType.DEL) {
                    return entity;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3909");
        }
        return null;
    }

    private void saveOrReplicate(Session session, Object entity) throws Exception {
        saveOrReplicate(session, entity, null, null);
    }

    private void saveOrReplicate(Session session, Object entity, RestoreProgress progress,
            List<DeferredRelation> deferredRelations) throws Exception {
        if (entity == null) {
            return;
        }
        Class entityClassSafe = getHibernateClassSafely(entity);
        if (entityClassSafe == null) {
            entityClassSafe = entity.getClass();
        }
        ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClassSafe);
        Serializable id = getEntityIdentifierSafely(session, entity, entityClassSafe);
        bersihkanRelasiTagihanTransientSebelumSave(entity);
        if (meta != null) {
            prepareEntityRelationsBeforeSave(session, entity, entityClassSafe, id, meta, progress, deferredRelations);
        }

        Object existing = id == null ? null : safeSessionGet(session, entityClassSafe, id);
        if (existing == null) {
            // FIX duplicate key "virtual_account_bank_va_key" saat RESTORE dari Audit:
            // entity lama (ID sudah tidak ada di tabel live) bisa membawa relasi va (FK
            // unique ke Va) yang SAMA dengan Va yang SEKARANG sudah dipakai baris
            // VirtualAccountBank lain (VA number bisa dipakai ulang/di-replace seiring
            // waktu). session.replicate() di bawah lalu gagal INSERT krn constraint unik,
            // membatalkan transaksi. Berlaku HANYA di cabang restore-insert ini (existing
            // == null) -- alur update normal (cabang else di bawah) TIDAK disentuh sama
            // sekali.
            hindariKonflikVaSaatRestoreInsert(session, entity, entityClassSafe);
            try {
                session.replicate(entity, ReplicationMode.OVERWRITE);
            } catch (Exception ex) {
                throw ex;
            }
        } else {
            // KE-12: 'existing' sudah ter-load ke persistence context oleh safeSessionGet, LENGKAP dengan
            // proxy relasinya. Saat session.merge(entity) meng-cascade ke relasi, ia dapat berbenturan dengan
            // instance/proxy 'existing' yang sudah dikelola sesi sehingga Hibernate melempar
            // "AssertionFailure: entity was not detached". Evict 'existing' agar merge memulai dari konteks
            // bersih (merge akan me-reload seperlunya). Bila tetap gagal, exception naik -> transaksi
            // di-rollback oleh pemanggil (tidak ada data setengah tersimpan).
            try { session.evict(existing); } catch (Exception evictIg) { ais.common.ErrorAuditUtil.record(evictIg, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3947");}
            session.merge(entity);
        }
        session.flush();
    }

    private void bersihkanRelasiTagihanTransientSebelumSave(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            if (entity instanceof ais.database.model.sekolah.PembayaranSiswaDetail) {
                ais.database.model.sekolah.PembayaranSiswaDetail detail =
                        (ais.database.model.sekolah.PembayaranSiswaDetail) entity;
                ais.database.model.sekolah.Tagihan tagihan = detail.ambilTagihan();
                if (tagihan != null && tagihan.getId() == null) {
                    detail.setTagihan(null);
                }
            } else if (entity instanceof ais.database.model.sekolah.Tagihan) {
                ais.database.model.sekolah.Tagihan tagihan = (ais.database.model.sekolah.Tagihan) entity;
                ais.database.model.sekolah.PembayaranSiswaDetail detail = tagihan.ambilPembayaranSiswaDetail();
                if (detail != null && detail.getId() == null) {
                    tagihan.setPembayaranSiswaDetailTrue(null);
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) GenericRevisiHelper.bersihkanRelasiTagihanTransientSebelumSave");
        }
    }

    /**
     * Khusus jalur RESTORE-INSERT (dipanggil HANYA dari saveOrReplicate() saat existing == null):
     * cegah bentrok unique constraint "virtual_account_bank_va_key" dengan mem-postfix random
     * kode Va bila kode Va milik entity yang di-restore ternyata SUDAH dipakai baris
     * VirtualAccountBank lain yang masih live. Entity SELAIN VirtualAccountBank tidak disentuh
     * sama sekali (no-op langsung return). Alur simpan/update normal (di luar restore) TIDAK
     * pernah memanggil method ini.
     */
    private void hindariKonflikVaSaatRestoreInsert(Session session, Object entity, Class entityClassSafe) {
        if (session == null || entity == null || entityClassSafe == null) {
            return;
        }
        if (!ais.database.model.VirtualAccountBank.class.isAssignableFrom(entityClassSafe)) {
            return;
        }
        try {
            ais.database.model.VirtualAccountBank vaBank = (ais.database.model.VirtualAccountBank) entity;
            ais.database.model.Va vaLama = vaBank.getVa();
            if (vaLama == null || vaLama.getId() == null) {
                return;
            }
            Number jumlahDipakai = (Number) session
                    .createSQLQuery("SELECT COUNT(*) FROM public.virtual_account_bank WHERE va = :vaId")
                    .setParameter("vaId", vaLama.getId()).uniqueResult();
            if (jumlahDipakai == null || jumlahDipakai.longValue() <= 0) {
                return; // Va ini belum dipakai baris lain -- aman, tidak perlu diubah.
            }
            String kodeAsli = vaLama.getKode();
            String kodeBaru = (kodeAsli == null ? "" : kodeAsli) + "R"
                    + String.valueOf(1000000L + Math.round(Math.random() * 8999999L));

            ais.database.model.Va vaBaru = new ais.database.model.Va();
            vaBaru.setKode(kodeBaru);
            session.save(vaBaru);
            vaBank.setVa(vaBaru);

            System.out.println("GenericRevisiHelper: restore VirtualAccountBank id=" + vaBank.getId()
                    + " -- Va lama (id=" + vaLama.getId() + ", kode=" + kodeAsli
                    + ") sudah dipakai baris lain, dialihkan ke Va baru (kode=" + kodeBaru + ")");
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e,
                    "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:hindariKonflikVaSaatRestoreInsert");
            // Gagal-aman: kalau pengecekan/pembuatan Va baru ini sendiri error, JANGAN
            // gagalkan seluruh restore -- biarkan replicate() di caller yang menentukan
            // hasil akhirnya (masih bisa berhasil bila ternyata tidak ada konflik nyata).
        }
    }

    private void prepareEntityRelationsBeforeSave(Session session, Object entity, Class ownerClass, Serializable ownerId,
            ClassMetadata meta, RestoreProgress progress, List<DeferredRelation> deferredRelations) {
        if (session == null || entity == null || meta == null) {
            return;
        }
        String[] names = meta.getPropertyNames();
        for (int i = 0; i < names.length; i++) {
            String property = names[i];
            if (property == null || isRestoreIgnoredRelationProperty(property)) {
                continue;
            }
            try {
                org.hibernate.type.Type type = meta.getPropertyType(property);
                if (type == null || !type.isEntityType()) {
                    continue;
                }
                Class propertyClass = null;
                try {
                    propertyClass = type.getReturnedClass();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:3972");
                }
                if (propertyClass != null && !GeneralValueObject.class.isAssignableFrom(propertyClass)) {
                    continue;
                }

                Object relationObject = meta.getPropertyValue(entity, property, EntityMode.POJO);
                if (!(relationObject instanceof GeneralValueObject)) {
                    continue;
                }
                Class relationClass = getHibernateClassSafely(relationObject);
                if (relationClass == null || !GeneralValueObject.class.isAssignableFrom(relationClass)) {
                    relationClass = propertyClass;
                }
                Serializable relationId = getEntityIdentifierSafely(session, relationObject, relationClass);
                if (relationClass == null || relationId == null) {
                    continue;
                }

                Object existingRelation = safeSessionGet(session, relationClass, relationId);
                if (existingRelation != null) {
                    setPropertyValueSafely(meta, entity, property, existingRelation);
                    continue;
                }

                if (deferredRelations != null && ownerClass != null && ownerId != null) {
                    addDeferredRelation(deferredRelations, ownerClass, ownerId, property, relationClass, relationId);
                    setPropertyValueSafely(meta, entity, property, null);
                    if (progress != null) {
                        progress.appendLog("Relasi belum tersedia saat penyimpanan, ditunda sementara: "
                                + buildEntityKey(ownerClass, ownerId) + "." + property + " -> "
                                + buildEntityKey(relationClass, relationId));
                    }
                }
            } catch (Exception e) {
                if (progress != null) {
                    progress.appendLog("Peringatan: gagal menyiapkan relasi " + property + " sebelum save: " + errorToString(e));
                }
            }
        }
    }

    private void applyDeferredRelations(Session session, List<DeferredRelation> deferredRelations, RestoreProgress progress) {
        if (session == null || deferredRelations == null || deferredRelations.isEmpty()) {
            return;
        }
        for (int i = 0; i < deferredRelations.size(); i++) {
            DeferredRelation dr = deferredRelations.get(i);
            if (dr == null || dr.ownerClass == null || dr.ownerId == null || dr.property == null
                    || dr.relationClass == null || dr.relationId == null) {
                continue;
            }
            try {
                Object owner = safeSessionGet(session, dr.ownerClass, dr.ownerId);
                Object relation = safeSessionGet(session, dr.relationClass, dr.relationId);
                if (owner == null || relation == null) {
                    if (progress != null) {
                        progress.appendLog("Relasi tertunda belum bisa diisi ulang karena data belum lengkap: "
                                + dr.describe());
                    }
                    continue;
                }
                ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(dr.ownerClass);
                if (meta == null) {
                    continue;
                }
                meta.setPropertyValue(owner, dr.property, relation, EntityMode.POJO);
                session.saveOrUpdate(owner);
                if (progress != null) {
                    progress.appendLog("Relasi tertunda berhasil diisi ulang: " + dr.describe());
                }
            } catch (Exception e) {
                if (progress != null) {
                    progress.itemFailed("Gagal mengisi ulang relasi tertunda: " + dr.describe(), e);
                }
            }
        }
        try {
            session.flush();
        } catch (Exception e) {
            if (progress != null) {
                progress.itemFailed("Gagal flush setelah mengisi ulang relasi tertunda.", e);
            }
        }
    }

    protected void restoreLatestFromDateWithConfirm() throws Exception {
        MyMessageboxConfig.show("Restore semua data terbaru dari revisi mulai tanggal yang dipilih?", "Konfirmasi Restore Massal",
                MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        int i = Integer.parseInt(event.getData().toString());
                        if (i == MyMessageboxConfig.OK) {
                            Date tanggalRestore = mulaiRestore == null ? null : mulaiRestore.getValue();
                            if (tanggalRestore == null) {
                                tanggalRestore = addDays(WaktuUtil.getDate(), -7);
                                try {
                                    if (mulaiRestore != null) {
                                        mulaiRestore.setValue(tanggalRestore);
                                    }
                                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4071");
                                }
                            }
                            startRestoreLatestFromDate(tanggalRestore);
                        }
                    }
                });
    }

    private void startRestoreLatestFromDate(final Date fromDate) {
        final RestoreProgress progress = new RestoreProgress("Restore Massal Revisi", entityClass);
        final MyWindow progressWindow = showRestoreProgressWindow(progress, true);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                doRestoreLatestFromDate(fromDate, progress);
            }
        }, "ais-restore-revisi-massal-" + (entityClass == null ? "data" : entityClass.getSimpleName()));
        thread.setDaemon(true);
        thread.start();
    }

    public int restoreLatestFromDate(Date fromDate) {
        RestoreProgress progress = new RestoreProgress("Restore Massal Revisi", entityClass);
        doRestoreLatestFromDate(fromDate, progress);
        return progress.getSuccessCount();
    }

    private void doRestoreLatestFromDate(Date fromDate, RestoreProgress progress) {
        Session session = null;
        Set<String> processed = new HashSet<String>();
        int processedPrimary = 0;
        try {
            Date date = fromDate == null ? addDays(WaktuUtil.getDate(), -7) : fromDate;
            progress.start("Mengambil daftar revisi terbaru mulai " + Common.dateFormat5.get().format(date));

            session = HibernateUtil.getSessionFactory().openSession();
            AuditReader reader = AuditReaderFactory.get(session);
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(entityClass, false, true);
            try {
                query.add(AuditEntity.revisionProperty("timestamp").ge(Long.valueOf(date.getTime())));
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4111");
            }
            query.addOrder(AuditEntity.revisionNumber().desc());
            query.setMaxResults(5000);
            List rows = query.getResultList();

            List targets = new ArrayList();
            for (int i = 0; i < rows.size(); i++) {
                Object entity = extractEntity(rows.get(i));
                RevisionType type = extractRevisionType(rows.get(i));
                if (entity == null || type == RevisionType.DEL) {
                    continue;
                }
                Class entityClassSafe = getHibernateClassSafely(entity);
                ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClassSafe);
                Serializable id = getEntityIdentifierSafely(session, entity, entityClassSafe);
                String key = buildEntityKey(entityClassSafe, id);
                if (processed.contains(key)) {
                    continue;
                }
                processed.add(key);
                targets.add(entity);
            }

            progress.setPrimaryTotal(targets.size());
            progress.appendLog("Total target restore utama: " + targets.size());

            for (int i = 0; i < targets.size(); i++) {
                Object entity = targets.get(i);
                Transaction tx = null;
                boolean ok = false;
                try {
                    progress.setStatus("Restore data " + (i + 1) + " dari " + targets.size());
                    tx = session.beginTransaction();
                    List<DeferredRelation> deferredRelations = new ArrayList<DeferredRelation>();
                    restoreDependenciesRecursively(session, reader, entity, new HashSet<String>(), progress,
                            deferredRelations);
                    progress.beginItem("DATA UTAMA", entity);
                    saveOrReplicate(session, entity, progress, deferredRelations);
                    applyDeferredRelations(session, deferredRelations, progress);
                    tx.commit();
                    ok = true;
                    progress.itemSuccess("Commit berhasil untuk data utama: " + describeEntity(entity));
                } catch (Exception itemError) {
                    rollback(tx);
                    progress.itemFailed("Gagal restore data utama: " + describeEntity(entity) + ". Transaksi data ini di-rollback.", itemError);
                } finally {
                    processedPrimary++;
                    progress.setPrimaryProcessed(processedPrimary);
                    try {
                        session.clear();
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4162");
                    }
                }
            }

            progress.finishSuccess("Restore massal selesai diproses.");
        } catch (Exception e) {
            progress.failGlobal(e, "Restore massal gagal pada proses utama.");
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4172");
            }
        } finally {
            closeSession(session);
        }
    }

    private MyWindow showRestoreProgressWindow(final RestoreProgress progress, final boolean refreshAfterDone) {
        final MyWindow window = new MyWindow("Progress Restore Revisi", "normal", true);
        window.setWidth("620px");
        window.setHeight("330px");
        window.setClosable(false);
        window.setSizable(false);
        window.setParent(this);

        Vbox container = new Vbox();
        container.setWidth("100%");
        container.setHeight("100%");
        container.setStyle("padding:12px; box-sizing:border-box;");
        container.setParent(window);

        final Label status = new Label(ais.common.Common.getBahasaConfig("Menyiapkan proses restore..."));
        status.setStyle("font-weight:bold; color:#2c3e50;");
        status.setParent(container);

        final Progressmeter progressmeter = new Progressmeter();
        progressmeter.setWidth("100%");
        progressmeter.setValue(0);
        progressmeter.setParent(container);

        final Label percent = new Label("0%");
        percent.setParent(container);

        final Label detail = new Label(ais.common.Common.getBahasaConfig("Total: 0 | Diproses: 0 | Berhasil: 0 | Gagal: 0"));
        detail.setStyle("white-space:pre-wrap;");
        detail.setParent(container);

        final Label last = new Label("");
        last.setStyle("white-space:pre-wrap; color:#555;");
        last.setParent(container);

        final Timer timer = new Timer();
        timer.setDelay(700);
        timer.setRepeats(true);
        timer.setParent(window);
        timer.addEventListener(Events.ON_TIMER, new EventListener() {
            public void onEvent(Event event) throws Exception {
                int pct = progress.getPercent();
                progressmeter.setValue(pct);
                percent.setValue(pct + "%");
                status.setValue(progress.getStatus());
                detail.setValue(progress.getCounterText());
                last.setValue(progress.getLastMessage());

                if (progress.isDone()) {
                    timer.setRunning(false);
                    progressmeter.setValue(100);
                    percent.setValue("100%");
                    if (refreshAfterDone) {
                        try {
                            if (callback != null) {
                                callback.onEvent(new Event("onRestore", GenericRevisiHelper.this, null));
                            }
                        } catch (Exception e) {
                            progress.appendLog("Callback setelah restore gagal: " + errorToString(e));
                        }
                        try {
                            onSearchDefault(null);
                        } catch (Exception e) {
                            progress.appendLog("Refresh grid setelah restore gagal: " + errorToString(e));
                        }
                    }
                    downloadRestoreLog(progress);
                    MyMessageboxConfig.show(progress.buildSummaryMessage(), "Ringkasan Restore",
                            MyMessageboxConfig.OK, progress.getFailedCount() > 0 ? MyMessageboxConfig.EXCLAMATION
                                    : MyMessageboxConfig.INFORMATION);
                    try {
                        window.detach();
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4250");
                    }
                }
            }
        });
        timer.setRunning(true);
        try {
            window.onModal();
        } catch (Exception e) {
            try {
                window.doHighlighted();
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4261");
            }
        }
        return window;
    }

    private void downloadRestoreLog(RestoreProgress progress) {
        try {
            String name = "restore_revisi_" + (entityClass == null ? "data" : entityClass.getSimpleName()) + "_"
                    + String.valueOf(System.currentTimeMillis()) + ".txt";
            String text = progress.buildLogText();
            try {
                Filedownload.save(text.getBytes("UTF-8"), "text/plain", name);
            } catch (UnsupportedEncodingException e) {
                Filedownload.save(text.getBytes(), "text/plain", name);
            }
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4280");
            }
        }
    }

    // ============================================================
    // FITUR DOWNLOAD REVISI (CSV — bisa langsung dibuka di Excel)
    // ============================================================

    /** Batas baris satu kali unduh agar tidak membebani memori/jaringan. */
    private static final int MAX_EXPORT_REVISI = 5000;

    // ============================================================
    // ANALISIS OLEH AI: bangun "perintah + data revisi" agar bisa disalin/diunduh lalu
    // ditempel ke AI (ChatGPT/Claude/Gemini) untuk menelaah data apa yang berubah / janggal /
    // perlu dicek. READ-ONLY: tidak mengubah data apa pun.
    // ============================================================

    /** Susun teks PERINTAH AI + DATA revisi sesuai filter aktif. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String buildAiAnalysisPrompt(boolean allDataMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PERINTAH UNTUK AI (ANALISIS DATA REVISI) ===\n");
        sb.append("Anda adalah analis data sistem kampus (eCampus). Data di bawah adalah RIWAYAT REVISI (audit trail) sebuah entitas.\n");
        sb.append("Tolong analisis dan jawab dalam Bahasa Indonesia yang terstruktur:\n");
        sb.append("1. Perubahan apa saja yang terjadi: field apa, dari nilai apa -> ke nilai apa, kapan, dan oleh siapa.\n");
        sb.append("2. Perubahan mana yang JANGGAL / berpotensi SALAH (mis. nilai melonjak/anjlok tak wajar, status mundur,\n");
        sb.append("   penghapusan tak terduga, pengubah/kelas tak biasa, perubahan di luar jam wajar).\n");
        sb.append("3. Data / aspek apa yang PERLU DICEK lebih lanjut, beserta alasannya.\n");
        sb.append("4. Adakah POLA (perubahan berulang / bolak-balik, perubahan massal pada waktu berdekatan).\n");
        sb.append("5. KESIMPULAN ringkas + REKOMENDASI langkah tindak lanjut.\n");
        sb.append("Keterangan: Aksi ADD=tambah, MOD=ubah, DEL=hapus. Tanda '->' = perubahan dari nilai lama ke nilai baru.\n\n");

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            AuditQuery query = allDataMode
                    ? buildAuditQuery(session, true, allKeyword, allMulai, allSampai, allTipeRevisi,
                            allHanyaTampilYangDihapus, allFilterKolomCari, allNilaiKolomCari)
                    : buildAuditQuery(session);
            query.setMaxResults(MAX_EXPORT_REVISI);
            List results = query.getResultList();
            if (results == null) {
                results = new ArrayList();
            }
            results = applyPostQueryFilters(session, results, allDataMode);

            sb.append("=== METADATA ===\n");
            sb.append("Entitas / Class : ").append(entityClass == null ? "-" : entityClass.getName()).append("\n");
            sb.append("Cakupan         : ").append(allDataMode ? "Semua ID pada class ini"
                    : ("ID terpilih = " + aiSafeStr(getSelectedEntityIdText()))).append("\n");
            sb.append("Jumlah revisi   : ").append(results.size()).append("\n");
            sb.append("Disusun pada    : ").append(new java.util.Date().toString()).append("\n\n");

            sb.append("=== DATA REVISI (hanya field yang BERUBAH) ===\n");
            int no = 1;
            for (int i = 0; i < results.size(); i++) {
                Object item = results.get(i);
                Object entity = extractEntity(item);
                if (!(entity instanceof Serializable)) {
                    continue;
                }
                Serializable revisionObject = (Serializable) entity;
                Object revEntity = extractRevisionEntity(item);
                RevisionType revType = extractRevisionType(item);
                String oleh = readString(revisionObject, "oleh");
                OlehIdInfo info = parseOlehId(readString(revisionObject, "olehId"));
                sb.append("\n[Revisi #").append(no++).append("] ")
                        .append("Tgl=").append(aiSafeStr(formatRevisionDate(revEntity, revisionObject)))
                        .append(" | Aksi=").append(aiSafeStr(labelRevisionType(revType)))
                        .append(" | Oleh=").append(oleh != null && oleh.length() > 0 ? oleh : aiSafeStr(info.userId))
                        .append(" | UserID=").append(aiSafeStr(info.userId))
                        .append(" | IP=").append(aiSafeStr(info.ip)).append("\n");
                sb.append("  Ringkasan: ").append(aiPotong(safeToString(revisionObject), 300)).append("\n");
                List fieldRows = collectRevisionFieldRows(session, revisionObject, revEntity);
                boolean adaPerubahan = false;
                for (int r = 0; r < fieldRows.size(); r++) {
                    Object ro = fieldRows.get(r);
                    if (!(ro instanceof String[])) {
                        continue;
                    }
                    String[] c = (String[]) ro;
                    if (c.length < 3) {
                        continue;
                    }
                    // c[0]=field, c[1]=nilai revisi (baru), c[2]=nilai sebelumnya (lama)
                    if (!aiEqualsSafe(c[2], c[1])) {
                        sb.append("    - ").append(c[0]).append(": \"").append(aiPotong(c[2], 200))
                                .append("\" -> \"").append(aiPotong(c[1], 200)).append("\"\n");
                        adaPerubahan = true;
                    }
                }
                if (!adaPerubahan) {
                    sb.append("    (tidak terdeteksi perubahan field dibanding revisi sebelumnya)\n");
                }
            }
            sb.append("\n=== AKHIR DATA ===\n");
        } catch (Exception e) {
            sb.append("\n[Catatan: terjadi error saat menyusun data: ")
                    .append(e.getMessage() == null ? "-" : e.getMessage()).append("]\n");
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4383");
            }
        } finally {
            closeSession(session);
        }
        return sb.toString();
    }

    /** Popup berisi PERINTAH AI + DATA (textarea), lengkap tombol Salin (clipboard) dan Download. */
    private void tampilkanAnalisisAi(boolean allDataMode) {
        try {
            final String prompt = buildAiAnalysisPrompt(allDataMode);
            final MyWindow win = new MyWindow();
            win.setTitle("Analisis oleh AI - Perintah + Data Revisi");
            win.setWidth(Common.isMobile() ? "95%" : "780px");
            win.setHeight("80%");
            win.setClosable(true);
            win.setSizable(true);
            win.setMaximizable(true);
            win.setBorder("normal");

            Vbox box = new Vbox();
            box.setWidth("100%");
            box.setHeight("100%");
            box.setStyle("padding:12px; box-sizing:border-box;");
            box.setParent(win);

            Label info = new Label("Salin teks di bawah lalu tempel ke AI (ChatGPT/Claude/Gemini). Isinya: perintah analisis + data revisi sesuai filter.");
            info.setStyle("font-size:12px; color:#334155; font-weight:bold;");
            info.setParent(box);

            final Textbox ta = new Textbox();
            ta.setMultiline(true);
            ta.setRows(20);
            ta.setWidth("100%");
            ta.setValue(prompt);
            ta.setStyle("font-family:monospace; font-size:12px;");
            ta.setParent(box);

            Hbox bar = new Hbox();
            bar.setStyle("margin-top:8px; gap:8px; flex-wrap:wrap;");
            bar.setParent(box);

            MyToolbarbuttonConfig salin = new MyToolbarbuttonConfig("Salin ke Clipboard", "/img/check.gif");
            salin.setStyle("font-weight:bold; color:#ffffff; background:#7c3aed; border-radius:10px; padding:6px 14px;");
            salin.setParent(bar);
            // Disalin LANGSUNG di sisi browser (pakai gesture klik nyata) agar Clipboard API diizinkan.
            salin.setWidgetListener("onClick",
                    "var e=document.getElementById('" + ta.getUuid() + "');"
                  + "if(e){var v=(e.value!==undefined)?e.value:e.textContent;"
                  + "if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(v)"
                  + ".then(function(){alert('Perintah AI tersalin ke clipboard.');})"
                  + ".catch(function(){try{e.focus();e.select();document.execCommand('copy');alert('Perintah AI tersalin.');}catch(x){}});}"
                  + "else{try{e.focus();e.select();document.execCommand('copy');alert('Perintah AI tersalin.');}catch(x){}}}");

            MyToolbarbuttonConfig unduh = new MyToolbarbuttonConfig("Download .md", "/img/excel.png");
            unduh.setStyle("font-weight:bold;");
            unduh.setParent(bar);
            unduh.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event event) throws Exception {
                    downloadPerintahAiText(prompt);
                }
            });

            MyToolbarbuttonConfig tutupWin = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
            tutupWin.setParent(bar);
            tutupWin.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event event) throws Exception {
                    win.detach();
                }
            });

            Component parent = getParent();
            if (parent != null) {
                parent.appendChild(win);
            } else {
                appendChild(win);
            }
            win.setZindex(100000);
            win.doHighlighted();
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4466");
            }
        }
    }

    /** Unduh PERINTAH AI + DATA revisi sebagai berkas .md. */
    private void downloadPerintahAi(boolean allDataMode) {
        downloadPerintahAiText(buildAiAnalysisPrompt(allDataMode));
    }

    private void downloadPerintahAiText(String text) {
        try {
            String name = "analisis_ai_revisi_" + (entityClass == null ? "data" : entityClass.getSimpleName())
                    + "_" + System.currentTimeMillis() + ".md";
            try {
                Filedownload.save(text.getBytes("UTF-8"), "text/markdown;charset=UTF-8", name);
            } catch (UnsupportedEncodingException e) {
                Filedownload.save(text.getBytes(), "text/plain", name);
            }
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4488");
            }
        }
    }

    private static String aiSafeStr(String s) {
        return s == null ? "" : s;
    }

    private static String aiPotong(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\r", " ").replace("\n", " ");
        return t.length() > max ? (t.substring(0, max) + "...") : t;
    }

    private static boolean aiEqualsSafe(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    /**
     * Unduh SEMUA revisi sesuai filter aktif (satu baris per revisi) ke berkas CSV yang bisa
     * dibuka di Excel. Memakai query audit yang sama persis dengan tampilan tabel, hanya tanpa
     * paging (dibatasi {@link #MAX_EXPORT_REVISI}). currentNativeSession TIDAK dipakai; sesi
     * dedicated dibuka & ditutup di finally.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void downloadSemuaRevisi(boolean allDataMode) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            AuditQuery query = allDataMode
                    ? buildAuditQuery(session, true, allKeyword, allMulai, allSampai, allTipeRevisi,
                            allHanyaTampilYangDihapus, allFilterKolomCari, allNilaiKolomCari)
                    : buildAuditQuery(session);
            query.setMaxResults(MAX_EXPORT_REVISI);
            List results = query.getResultList();
            if (results == null) {
                results = new ArrayList();
            }
            results = applyPostQueryFilters(session, results, allDataMode);

            List rowsCsv = new ArrayList();
            int no = 1;
            for (int i = 0; i < results.size(); i++) {
                Object item = results.get(i);
                Object entity = extractEntity(item);
                if (!(entity instanceof Serializable)) {
                    continue;
                }
                Serializable revisionObject = (Serializable) entity;
                Object revEntity = extractRevisionEntity(item);
                RevisionType revType = extractRevisionType(item);
                String oleh = readString(revisionObject, "oleh");
                OlehIdInfo info = parseOlehId(readString(revisionObject, "olehId"));
                rowsCsv.add(new String[] { String.valueOf(no++), formatRevisionDate(revEntity, revisionObject),
                        labelRevisionType(revType), safeToString(revisionObject),
                        oleh != null && oleh.length() > 0 ? oleh : info.userId, info.userId, info.ip });
            }

            String csv = buildCsv(new String[] { "No", "Tanggal Revisi", "Aksi", "Ringkasan Data", "Oleh", "User ID",
                    "IP" }, rowsCsv);
            String name = "revisi_" + (entityClass == null ? "data" : entityClass.getSimpleName())
                    + (allDataMode ? "_semua_id_" : "_id_" + safeFileId()) + System.currentTimeMillis() + ".csv";
            saveCsvDownload(csv, name);
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4560");
            }
        } finally {
            closeSession(session);
        }
    }

    /** Unduh detail SATU revisi (satu baris per field: Nilai Revisi vs Nilai Sebelumnya) ke CSV. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void downloadSatuRevisi(Serializable revisionObject, Object revEntity) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List rowsCsv = collectRevisionFieldRows(session, revisionObject, revEntity);
            String csv = buildCsv(new String[] { "Field", "Nilai Revisi", "Nilai Sebelumnya" }, rowsCsv);
            String name = "revisi_" + (entityClass == null ? "data" : entityClass.getSimpleName()) + "_detail_"
                    + System.currentTimeMillis() + ".csv";
            saveCsvDownload(csv, name);
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4581");
            }
        } finally {
            closeSession(session);
        }
    }

    /** Kumpulkan baris {field, nilai revisi, nilai sebelumnya} untuk satu revisi (logika sama dgn renderDetail). */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List collectRevisionFieldRows(Session session, Serializable revisionObject, Object revEntity) {
        List out = new ArrayList();
        try {
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
            Serializable id = meta == null ? null : meta.getIdentifier(revisionObject, EntityMode.POJO);
            Object previous = id == null ? null
                    : findPreviousRevisionEntityForComparison(session, id, revEntity, revisionObject);
            if (meta != null) {
                Object previousId = previous == null ? null : meta.getIdentifier(previous, EntityMode.POJO);
                out.add(new String[] { meta.getIdentifierPropertyName(), formatValue(id), formatValue(previousId) });
                String[] props = meta.getPropertyNames();
                for (int i = 0; i < props.length; i++) {
                    Object revisionValue = null;
                    Object previousValue = null;
                    try {
                        revisionValue = meta.getPropertyValue(revisionObject, props[i], EntityMode.POJO);
                        initializeQuietly(revisionValue);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4607");
                    }
                    try {
                        previousValue = previous == null ? null
                                : meta.getPropertyValue(previous, props[i], EntityMode.POJO);
                        initializeQuietly(previousValue);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4613");
                    }
                    out.add(new String[] { props[i], formatValue(revisionValue), formatValue(previousValue) });
                }
            }
        } catch (Exception e) {
            out.add(new String[] { "(error)", e.getMessage() == null ? "" : e.getMessage(), "" });
        }
        return out;
    }

    /** Bentuk potongan nama file aman dari ID data yang sedang dibuka. */
    private String safeFileId() {
        try {
            String s = getSelectedEntityIdText();
            if (s == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                sb.append(Character.isLetterOrDigit(c) ? c : '_');
            }
            return sb.length() == 0 ? "" : (sb.toString() + "_");
        } catch (Exception e) {
            return "";
        }
    }

    /** Susun teks CSV (pemisah ';' + kutip ganda + BOM UTF-8 agar rapi di Excel). */
    @SuppressWarnings("rawtypes")
    private String buildCsv(String[] header, List rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿');
        if (header != null) {
            for (int i = 0; i < header.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                sb.append(csvCell(header[i]));
            }
            sb.append("\r\n");
        }
        if (rows != null) {
            for (int r = 0; r < rows.size(); r++) {
                String[] cells = (String[]) rows.get(r);
                if (cells == null) {
                    continue;
                }
                for (int i = 0; i < cells.length; i++) {
                    if (i > 0) {
                        sb.append(';');
                    }
                    sb.append(csvCell(cells[i]));
                }
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    private String csvCell(String v) {
        if (v == null) {
            return "";
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private void saveCsvDownload(String csv, String filename) {
        try {
            Filedownload.save(csv.getBytes("UTF-8"), "text/csv;charset=UTF-8", filename);
        } catch (UnsupportedEncodingException e) {
            Filedownload.save(csv.getBytes(), "text/csv", filename);
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4689");
            }
        }
    }

    private String describeEntity(Object entity) {
        if (entity == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(entity.getClass().getName());
        try {
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entity.getClass());
            if (meta != null) {
                Object id = meta.getIdentifier(entity, EntityMode.POJO);
                sb.append("#").append(id == null ? "" : id);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4706");
        }
        try {
            sb.append(" - ").append(entity.toString());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4710");
        }
        return sb.toString();
    }

    private String errorToString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable t = throwable;
        while (t != null) {
            if (sb.length() > 0) {
                sb.append(" | Disebabkan oleh: ");
            }
            sb.append(t.getClass().getName());
            if (t.getMessage() != null && t.getMessage().trim().length() > 0) {
                sb.append(": ").append(t.getMessage().trim());
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    private String suggestSolution(Throwable throwable) {
        String msg = errorToString(throwable).toLowerCase();
        if (msg.length() == 0) {
            return "Cek log aplikasi untuk detail penyebab error.";
        }
        if (msg.indexOf("constraint") >= 0 || msg.indexOf("foreign key") >= 0 || msg.indexOf("violates") >= 0) {
            return "Kemungkinan ada relasi/foreign key yang belum tersedia atau kolom wajib belum terisi. Coba aktifkan deep restore, restore data master/relasi terlebih dahulu, lalu ulangi proses restore.";
        }
        if (msg.indexOf("not-null") >= 0 || msg.indexOf("null value") >= 0) {
            return "Ada kolom NOT NULL yang belum memiliki nilai pada revisi lama. Lengkapi nilai field tersebut atau sesuaikan constraint database jika model terbaru memang memperbolehkan NULL.";
        }
        if (msg.indexOf("no row with the given identifier") >= 0 || msg.indexOf("objectnotfound") >= 0) {
            return "Ada object relasi yang sudah tidak ada. Restore data relasi tersebut terlebih dahulu atau jalankan restore dengan opsi deep agar relasi dicari dari audit.";
        }
        if (msg.indexOf("stale") >= 0 || msg.indexOf("unexpected row count") >= 0) {
            return "Data kemungkinan sudah berubah/dihapus oleh transaksi lain. Refresh halaman, pastikan data target masih valid, lalu ulangi restore.";
        }
        if (msg.indexOf("could not resolve property") >= 0 || msg.indexOf("column") >= 0 || msg.indexOf("sqlgrammar") >= 0) {
            return "Kemungkinan mapping model dan struktur database belum sinkron. Jalankan migrasi/InitIndex yang relevan dan pastikan nama kolom sesuai model.";
        }
        if (msg.indexOf("lazy") >= 0 || msg.indexOf("session") >= 0) {
            return "Kemungkinan object lazy sudah terlepas dari session. Ulangi proses dari halaman revisi terbaru; jika masih gagal, cek getter relasi model terkait.";
        }
        return "Cek detail error, pastikan data relasi masih tersedia, struktur tabel sesuai model, dan ulangi restore setelah refresh halaman.";
    }

    private class RestoreProgress {
        private String processName;
        private Class clazz;
        private Date startedAt;
        private Date finishedAt;
        private volatile int primaryTotal;
        private volatile int primaryProcessed;
        private volatile int total;
        private volatile int attempted;
        private volatile int success;
        private volatile int failed;
        private volatile boolean done;
        private volatile boolean successFinish;
        private volatile String status = "Menunggu proses restore...";
        private volatile String lastMessage = "";
        private StringBuilder log = new StringBuilder(4096);

        RestoreProgress(String processName, Class clazz) {
            this.processName = processName == null ? "Restore Revisi" : processName;
            this.clazz = clazz;
        }

        synchronized void start(String message) {
            startedAt = WaktuUtil.getDate();
            status = message == null ? "Mulai restore..." : message;
            appendLog("============================================================");
            appendLog(processName + " - " + (clazz == null ? "" : clazz.getName()));
            appendLog("Mulai : " + Common.datetimeFormat2s.get().format(startedAt));
            appendLog(status);
        }

        synchronized void setPrimaryTotal(int primaryTotal) {
            this.primaryTotal = primaryTotal < 0 ? 0 : primaryTotal;
            this.total = this.primaryTotal;
        }

        synchronized void addDynamicTotal(int add) {
            if (add > 0) {
                this.total += add;
            }
        }

        synchronized void setPrimaryProcessed(int value) {
            this.primaryProcessed = value < 0 ? 0 : value;
        }

        synchronized void markPrimaryProcessed() {
            this.primaryProcessed++;
        }

        synchronized void setStatus(String value) {
            status = value == null ? "" : value;
            appendLog(status);
        }

        synchronized void beginItem(String jenis, Object entity) {
            attempted++;
            if (total < attempted) {
                total = attempted;
            }
            lastMessage = "Memproses " + (jenis == null ? "DATA" : jenis) + ": " + describeEntity(entity);
            appendLog("[PROSES] " + lastMessage);
        }

        synchronized void itemSuccess(String message) {
            success++;
            lastMessage = message == null ? "Berhasil." : message;
            appendLog("[BERHASIL] " + lastMessage);
        }

        synchronized void itemFailed(String message, Throwable throwable) {
            failed++;
            lastMessage = message == null ? "Gagal." : message;
            appendLog("[GAGAL] " + lastMessage);
            if (throwable != null) {
                appendLog("Penyebab : " + errorToString(throwable));
                appendLog("Solusi   : " + suggestSolution(throwable));
            }
        }

        synchronized void finishSuccess(String message) {
            successFinish = failed == 0;
            done = true;
            finishedAt = WaktuUtil.getDate();
            status = message == null ? "Restore selesai." : message;
            lastMessage = status;
            appendLog(status);
            appendLog("Selesai: " + Common.datetimeFormat2s.get().format(finishedAt));
            appendLog(getCounterText());
        }

        synchronized void failGlobal(Throwable throwable, String message) {
            done = true;
            successFinish = false;
            finishedAt = WaktuUtil.getDate();
            if (attempted == 0) {
                attempted = 1;
            }
            if (failed == 0) {
                failed = attempted;
            }
            success = 0;
            status = message == null ? "Restore gagal." : message;
            lastMessage = status;
            appendLog("[GAGAL UTAMA] " + status);
            appendLog("Penyebab : " + errorToString(throwable));
            appendLog("Solusi   : " + suggestSolution(throwable));
            appendLog("Selesai: " + Common.datetimeFormat2s.get().format(finishedAt));
            appendLog(getCounterText());
        }

        synchronized void appendLog(String message) {
            if (message == null) {
                return;
            }
            try {
                log.append(Common.datetimeFormat2s.get().format(WaktuUtil.getDate())).append(" - ").append(message)
                        .append("\r\n");
            } catch (Exception e) {
                log.append(message).append("\r\n");
            }
        }

        synchronized int getPercent() {
            if (done) {
                return 100;
            }
            int baseTotal = primaryTotal <= 0 ? total : primaryTotal;
            int baseProcessed = primaryTotal <= 0 ? attempted : primaryProcessed;
            if (baseTotal <= 0) {
                return 0;
            }
            int pct = (int) Math.floor((baseProcessed * 100.0d) / baseTotal);
            if (pct < 0) {
                return 0;
            }
            if (pct > 99) {
                return done ? 100 : 99;
            }
            return pct;
        }

        synchronized String getCounterText() {
            return "Total target: " + primaryTotal + " | Total diproses: " + attempted + " | Berhasil: " + success
                    + " | Gagal: " + failed;
        }

        synchronized String getStatus() {
            return status == null ? "" : status;
        }

        synchronized String getLastMessage() {
            return lastMessage == null ? "" : lastMessage;
        }

        synchronized boolean isDone() {
            return done;
        }

        synchronized int getSuccessCount() {
            return success;
        }

        synchronized int getFailedCount() {
            return failed;
        }

        synchronized String buildSummaryMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(processName).append(" selesai.\n\n");
            sb.append(getCounterText()).append("\n");
            if (failed > 0) {
                sb.append("\nAda data yang gagal direstore. Detail penyebab dan solusi sudah ditulis di file log yang otomatis diunduh.");
            } else {
                sb.append("\nSemua data yang diproses berhasil direstore.");
            }
            return sb.toString();
        }

        synchronized String buildLogText() {
            StringBuilder sb = new StringBuilder(log.length() + 512);
            sb.append("LOG RESTORE REVISI\r\n");
            sb.append("Class       : ").append(clazz == null ? "" : clazz.getName()).append("\r\n");
            sb.append("Proses      : ").append(processName).append("\r\n");
            sb.append("Mulai       : ").append(startedAt == null ? "" : Common.datetimeFormat2s.get().format(startedAt)).append("\r\n");
            sb.append("Selesai     : ").append(finishedAt == null ? "" : Common.datetimeFormat2s.get().format(finishedAt)).append("\r\n");
            sb.append("Status      : ").append(successFinish ? "BERHASIL" : failed > 0 ? "SEBAGIAN/GAGAL" : "SELESAI").append("\r\n");
            sb.append(getCounterText()).append("\r\n");
            sb.append("============================================================\r\n");
            sb.append(log.toString());
            return sb.toString();
        }
    }

    protected Date addDays(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date == null ? new Date() : date);
        c.add(Calendar.DATE, days);
        return c.getTime();
    }

    protected Date addMonths(Date date, int months) {
        Calendar c = Calendar.getInstance();
        c.setTime(date == null ? new Date() : date);
        c.add(Calendar.MONTH, months);
        return c.getTime();
    }

    protected Date normalizeStart(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    protected Date normalizeEnd(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    protected String formatRevisionDate(Object revisionEntity, Object entity) {
        try {
            if (revisionEntity instanceof DefaultRevisionEntity) {
                return Common.dateFormat5.get().format(((DefaultRevisionEntity) revisionEntity).getRevisionDate());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:4999");
        }
        try {
            if (classMetadata != null && hasProperty("tanggal_dirubah")) {
                Object value = classMetadata.getPropertyValue(entity, "tanggal_dirubah", EntityMode.POJO);
                if (value instanceof Date) {
                    return Common.dateFormat5.get().format((Date) value);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:5008");
        }
        return "";
    }

    protected String labelRevisionType(RevisionType type) {
        if (type == RevisionType.ADD) return "Tambah";
        if (type == RevisionType.MOD) return "Ubah";
        if (type == RevisionType.DEL) return "Hapus";
        return "";
    }

    protected String readOleh(Object entity) {
        String oleh = readString(entity, "oleh");
        String olehId = readString(entity, "olehId");
        if (oleh.length() == 0) return olehId;
        if (olehId.length() == 0) return oleh;
        return oleh + " (" + olehId + ")";
    }

    protected String readString(Object entity, String property) {
        try {
            if (entity != null && classMetadata != null && hasProperty(property)) {
                Object value = classMetadata.getPropertyValue(entity, property, EntityMode.POJO);
                return value == null ? "" : value.toString();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:5034");
        }
        return "";
    }

    protected String safeToString(Object value) {
        try {
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    protected String formatValue(Object value) {
        try {
            if (value == null) return "";
            if (value instanceof Date) return Common.datetimeFormat2s.get().format((Date) value);
            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Format nilai KHUSUS untuk popup "Edit Manual Nilai Revisi".
     * <p>
     * Untuk field bertipe relasi {@link GeneralValueObject}, selain menampilkan label
     * (mis. "SURYADI (Pegawai)") juga menampilkan <b>ID</b>-nya — karena pada field relasi yang harus
     * dimasukkan ke "Nilai Baru" adalah <b>ID</b> data relasi, bukan labelnya. Tanpa ini admin tidak
     * tahu ID mana yang harus diketik. Untuk tipe non-relasi, perilakunya sama dengan {@link #formatValue(Object)}.
     */
    protected String formatValueForManual(Object value) {
        try {
            if (value == null) return "";
            if (value instanceof Date) return Common.datetimeFormat2s.get().format((Date) value);
            if (value instanceof GeneralValueObject) {
                Object id = ((GeneralValueObject) value).getId();
                String label = value.toString();
                return (label == null ? "" : label) + (id == null ? "   [ID: -]" : "   [ID: " + id + "]");
            }
            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    protected void rollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:5085");
        }
    }

    protected void closeSession(Session session) {
        if (session != null) {
            try {
                session.clear();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:5093");
            }
            try {
                session.disconnect();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:5097");
            }
            try {
                session.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/GenericRevisiHelper.java:5101");
            }
        }
    }
}
