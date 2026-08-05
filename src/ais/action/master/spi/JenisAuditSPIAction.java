package ais.action.master.spi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spi.JenisAuditSPI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>JenisAuditSPIAction &mdash; Pengendali Layar Setup Data Master SPI (Level 1)</h2>
 *
 * <p>
 * Kelas ini adalah pengendali (controller) ZK bagi layar "Setup SPI" &mdash; layar tab utama tempat
 * staf Satuan Pengawasan Internal mendata seluruh hierarki checklist audit dari nol: mulai dari
 * kategori besar (Jenis Audit, dikelola langsung di kelas ini), lalu turun ke kriteria acuan
 * ({@link KriteriaAuditSPIAction}, tab kedua), dan langkah uji paling rinci
 * ({@link ChecklistAuditSPIAction}, tab ketiga). Ketiga tab tersebut ditampilkan dalam satu jendela
 * ZUL ({@code jenis_audit_spi.zul}) memakai komponen ZK {@code Tabbox}, sehingga staf SPI bisa
 * berpindah antar level tanpa membuka layar terpisah &mdash; pola yang sama persis dipakai modul
 * Audit Mutu Internal akademik (SPMI) yang sudah terbukti nyaman dipakai bertahun-tahun.
 * </p>
 *
 * <h3>Pemuatan tab secara malas (lazy loading)</h3>
 * <p>
 * Tab kedua (Kriteria) dan ketiga (Checklist) TIDAK langsung dimuat saat halaman pertama kali
 * dibuka &mdash; isinya baru disisipkan ({@link #loadTab(Tabpanel, String)}) pada saat pengguna
 * benar-benar mengklik tab tersebut untuk pertama kalinya ({@link #onKriteria(Event)},
 * {@link #onChecklist(Event)}). Pendekatan ini penting untuk kinerja: seorang staf yang hanya perlu
 * menambah satu Jenis Audit baru tidak perlu menunggu ketiga grid data (yang masing-masing
 * melakukan pencarian ke database) selesai dimuat sekaligus di awal &mdash; cukup grid yang sedang
 * ia lihat yang aktif melakukan query. Begitu satu tab pernah dibuka, isinya TIDAK dimuat ulang pada
 * kunjungan berikutnya dalam sesi yang sama ({@code panel.getChildren().isEmpty()} sebagai penanda),
 * sehingga berpindah-pindah tab terasa instan setelah pemuatan pertama.
 * </p>
 *
 * <h3>Mengapa hanya 3 tab, bukan 5 seperti SPMI</h3>
 * <p>
 * Lihat javadoc {@link JenisAuditSPI} untuk penjelasan lengkap kenapa hierarki checklist audit SPI
 * cukup 3 tingkat (Jenis &rarr; Kriteria &rarr; Checklist), berbeda dengan modul SPMI akademik yang
 * meniru struktur 5 tingkat instrumen akreditasi BAN-PT. Struktur yang lebih ramping ini membuat
 * staf SPI tidak perlu menavigasi lapisan data yang tidak relevan bagi kebutuhan audit internal.
 * </p>
 *
 * <h3>Pola CRUD baku yang dipakai (diwariskan dari {@link BaseSPIAction})</h3>
 * <p>
 * Seluruh mekanisme umum (pemeriksaan hak akses, paging, tombol cetak/unggah, kerangka formulir
 * popup) disediakan oleh kelas dasar {@link BaseSPIAction}; kelas ini HANYA berisi hal-hal yang
 * spesifik untuk entitas {@link JenisAuditSPI}: kolom apa saja yang tampil di tabel
 * ({@link JenisAuditSPIRenderer}), field apa saja yang ada di formulir tambah/ubah
 * ({@link #buildForm(JenisAuditSPI)}), aturan validasi sebelum data disimpan ({@link #onSave(Event)}),
 * dan kriteria pencarian/pengurutan data ({@link #initCriteria(boolean)}). Pemisahan tanggung jawab
 * ini membuat penambahan entitas data master baru di masa depan (jika suatu hari SPI butuh level
 * keempat) cukup meniru pola kelas ini tanpa perlu menyentuh kembali {@link BaseSPIAction}.
 * </p>
 *
 * <h3>Centang Aktif langsung tersimpan tanpa membuka formulir</h3>
 * <p>
 * Kolom "Aktif" pada tabel dirender sebagai checkbox yang bisa langsung diklik dari daftar
 * ({@link JenisAuditSPIRenderer#render(Row, Object)}) &mdash; setiap kali diklik, perubahan
 * langsung disimpan ke database seketika ({@code Common.refreshSaveOrUpdate(item)}) tanpa perlu
 * membuka jendela formulir. Ini mempercepat pekerjaan staf yang hanya ingin menonaktifkan satu
 * kategori audit yang sudah tidak relevan tanpa harus membuka-tutup popup.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class JenisAuditSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    // ---- Form fields ----
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;

    // ---- Current entity ----
    private JenisAuditSPI jenisAuditSPI;

    // ---- Lazy-loaded sub-tabs ----
    protected Tabpanel kriteriaTab;
    protected Tabpanel checklistTab;

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

    public void onKriteria(Event event) {
        loadTab(kriteriaTab, "/pages/master/spi/kriteria_audit_spi.zul");
    }

    public void onChecklist(Event event) {
        loadTab(checklistTab, "/pages/master/spi/checklist_audit_spi.zul");
    }

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();
        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(JenisAuditSPI.class,
                new String[]{"id", "kode", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class JenisAuditSPIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final JenisAuditSPI item = (JenisAuditSPI) obj;

            new Label(item.getKode()).setParent(row);
            RevisiHelper.createNewRevisi(JenisAuditSPI.class, item, item.getNama()).setParent(row);
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

            Common.copyEditDeleteButtons(edit, delete, item, JenisAuditSPIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new JenisAuditSPI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        jenisAuditSPI = (JenisAuditSPI) obj;
        buildForm(jenisAuditSPI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final JenisAuditSPI item) {
        FormHolder fh = prepareFormWindow("Pendataan Jenis Audit SPI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "Kode Jenis Audit");
        row.appendChild(kode = new Textbox(item.getKode()));
        kode.setWidth("90%");

        row = addFormRow(rows, "Nama Jenis Audit *");
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
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Audit belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Nama Jenis Audit dengan nama yang deskriptif (contoh: Audit Keuangan, Audit Operasional);"
                    + " (2) pastikan nama tidak kosong dan belum digunakan oleh jenis audit lain;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (jenisAuditSPI.getId() != null) {
            jenisAuditSPI = (JenisAuditSPI) session.load(JenisAuditSPI.class, jenisAuditSPI.getId());
        }
        jenisAuditSPI.setKode(kode.getValue());
        jenisAuditSPI.setNama(nama.getValue());
        jenisAuditSPI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, jenisAuditSPI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisAuditSPI.class)
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
        List<JenisAuditSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new JenisAuditSPIRenderer());
    }
}
