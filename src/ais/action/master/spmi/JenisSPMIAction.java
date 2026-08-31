package ais.action.master.spmi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spmi.JenisSPMI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk jenis spmi. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * BaseSPMIAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox kode}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code JenisSPMI jenisSPMI}, {@code Tabpanel standarTab}, {@code Tabpanel
 * butirMutuTab}, {@code Tabpanel indikatorTab}, {@code Tabpanel skenarioTab}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code loadTab()}, {@code
 * onDownloadSpmiGlobal()}, {@code onUploadSpmiGlobal()}, {@code resolveUploadEvent()}, {@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onStandar()}, {@code
 * onButirMutu()}, {@code onIndikator()}, {@code onSkenario()}, {@code onAdd()}, {@code buildForm()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see BaseSPMIAction
 */
public class JenisSPMIAction extends BaseSPMIAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // ---- Form fields ----
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;

    // ---- Current entity ----
    private JenisSPMI jenisSPMI;

    // ---- Lazy-loaded sub-tabs ----
    protected Tabpanel standarTab;
    protected Tabpanel butirMutuTab;
    protected Tabpanel indikatorTab;
    protected Tabpanel skenarioTab; 
    protected Tabbox spmiTabbox;
    protected MyToolbarbuttonConfig downloadSpmiGlobal;
    protected MyToolbarbuttonConfig uploadSpmiGlobal;

    // =====================================================================
    // Lazy tab loading
    // =====================================================================

    private void loadTab(Tabpanel panel, String src) {
        if (panel.getChildren().isEmpty()) {
            MyInclude include = new MyInclude();
            include.setHeight("100%");
            include.setWidth("100%");
            include.setParent(panel);
            include.setSrc(src);
        }
    }

    public void onStandar(Event event) {
        loadTab(standarTab, "/pages/master/spmi/standar_spmi.zul");
    }

    public void onButirMutu(Event event) {
        loadTab(butirMutuTab, "/pages/master/spmi/butir_mutu_spmi.zul");
    }

    public void onIndikator(Event event) {
        loadTab(indikatorTab, "/pages/master/spmi/indikator_spmi.zul");
    }

    public void onSkenario(Event event) {
        loadTab(skenarioTab, "/pages/master/spmi/skenario_spmi.zul");
    }

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();
        uploadSpmiGlobal.setVisible(edit);
        uploadSpmiGlobal.setUpload(Common.ukuranFileUpload());
        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(JenisSPMI.class,
                new String[]{"id", "kode", "nama", "keterangan", "aktif"});
    }

    public void onDownloadSpmiGlobal(Event event) throws Exception {
        try {
            Filedownload.save(SpmiGlobalExcelHelper.exportWorkbook(),
                    SpmiGlobalExcelHelper.MIME_XLSX, SpmiGlobalExcelHelper.fileName());
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Master SPMI Global tidak dapat diunduh. " + e.getMessage(),
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
        }
    }

    public void onUploadSpmiGlobal(Event event) throws Exception {
        if (!edit) throw new SecurityException("Anda tidak memiliki hak memperbarui master SPMI.");
        try {
            UploadEvent uploadEvent = resolveUploadEvent(event);
            Media media = uploadEvent.getMedia();
            if (media == null || media.getName() == null
                    || !media.getName().toLowerCase().endsWith(".xlsx")) {
                throw new IllegalArgumentException("File harus berformat Excel Open XML (.xlsx).");
            }
            if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
            SpmiGlobalExcelHelper.ImportResult result =
                    SpmiGlobalExcelHelper.importWorkbook(media.getByteData());
            standarTab.getChildren().clear();
            butirMutuTab.getChildren().clear();
            indikatorTab.getChildren().clear();
            skenarioTab.getChildren().clear();
            spmiTabbox.setSelectedIndex(0);
            onSearchDefault(null);
            MyMessageboxConfig.show(result.message(), "Pemberitahuan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Upload SPMI Global gagal. " + e.getMessage(),
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
        }
    }

    /**
     * Mengambil event upload asli. Komponen ZUL menggunakan forward sehingga ZK
     * membungkus UploadEvent di dalam ForwardEvent sebelum memanggil composer.
     * Tetap menerima UploadEvent langsung agar handler aman digunakan dari kedua
     * bentuk binding event.
     */
    private UploadEvent resolveUploadEvent(Event event) {
        Event origin = event;
        while (origin instanceof ForwardEvent) {
            origin = ((ForwardEvent) origin).getOrigin();
        }
        if (!(origin instanceof UploadEvent)) {
            throw new IllegalArgumentException("Event upload SPMI Global tidak valid.");
        }
        return (UploadEvent) origin;
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    /**
     * Renderer lokal untuk layar/komponen {@link JenisSPMIAction}. Kelas ini menerjemahkan satu item data menjadi
     * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link JenisSPMIAction} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see JenisSPMIAction
     */
    class JenisSPMIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final JenisSPMI item = (JenisSPMI) obj;

            new Label(item.getKode()).setParent(row);
            RevisiHelper.createNewRevisi(JenisSPMI.class, item, item.getNama()).setParent(row);
            new Label(item.getKeterangan()).setParent(row);

            final MyCheckboxConfig aktifCb = new MyCheckboxConfig("Aktif");
            aktifCb.setDisabled(!edit);
            aktifCb.setChecked(item.getAktif());
            aktifCb.setParent(row);
            row.setAttribute("checkbox", aktifCb);
            aktifCb.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    item.setAktif(aktifCb.isChecked());
                    Common.refreshSaveOrUpdate(item);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, item, JenisSPMIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new JenisSPMI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        jenisSPMI = (JenisSPMI) obj;
        buildForm(jenisSPMI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final JenisSPMI item) {
        FormHolder fh = prepareFormWindow("Pendataan Jenis SPMI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "Kode Jenis SPMI");
        row.appendChild(kode = new Textbox(item.getKode()));
        kode.setWidth("90%");

        row = addFormRow(rows, "Nama Jenis SPMI *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");

        row = addFormRow(rows, "Keterangan");
        row.appendChild(keterangan = new Textbox(item.getKeterangan()));
        keterangan.setWidth("90%");
        keterangan.setRows(3);

        finaliseFormWindow(fh, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
    }

    // =====================================================================
    // Save
    // =====================================================================

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Nama Jenis SPMI pada form; "
                    + "(2) pastikan teks tidak kosong atau hanya berisi spasi; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (jenisSPMI.getId() != null) {
            jenisSPMI = (JenisSPMI) session.load(JenisSPMI.class, jenisSPMI.getId());
        }
        jenisSPMI.setKode(kode.getValue());
        jenisSPMI.setNama(nama.getValue());
        jenisSPMI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, jenisSPMI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisSPMI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<JenisSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new JenisSPMIRenderer());
    }
}
