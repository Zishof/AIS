package ais.action.master.spi;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * <h2>BaseSPIAction &mdash; Kerangka Bersama Layar CRUD Modul Satuan Pengawasan Internal</h2>
 *
 * <p>
 * Kelas dasar abstrak ini menyediakan seluruh perlengkapan yang SELALU dibutuhkan oleh layar
 * "Pendataan"/CRUD (Create-Read-Update-Delete) sederhana di modul SPI &mdash; mulai dari layar
 * data master (Jenis Audit, Kriteria Audit, Checklist Audit di Bagian A) hingga layar-layar
 * pelaksanaan audit yang akan dibangun pada fase berikutnya. Tujuannya satu: agar setiap layar
 * CRUD baru di modul ini TIDAK perlu menulis ulang kode boilerplate yang sama persis (pemeriksaan
 * hak akses, pemasangan paging, pembangunan formulir popup, penyegaran tabel data), sehingga kode
 * tetap ringkas, konsisten antar-layar, dan gampang dirawat &mdash; cukup satu tempat yang perlu
 * diperbaiki bila suatu hari perilaku bersama ini perlu diubah.
 * </p>
 *
 * <h3>Apa saja yang disediakan kelas ini</h3>
 * <ul>
 *   <li><b>Pemeriksaan keamanan otomatis</b> &mdash; {@link #doBeforeCompose} memanggil
 *       {@code Common.doCheckSecurity()} sebelum komponen ZK lain dibangun, sehingga pengguna
 *       yang tidak berhak tidak akan pernah sampai melihat isi layar.</li>
 *   <li><b>Bendera hak akses</b> ({@link #edit}, {@link #delete}) yang diisi otomatis oleh
 *       {@link #initPrivileges()} berdasarkan peran pengguna yang sedang login, dipakai subclass
 *       untuk menyembunyikan/menonaktifkan tombol ubah &amp; hapus bila memang tak berhak.</li>
 *   <li><b>Pemasangan paging</b> ({@link #initPagingListener()}) &mdash; setiap kali pengguna
 *       pindah halaman, otomatis memanggil ulang pencarian data terbaru.</li>
 *   <li><b>Tombol cetak/unggah data massal</b> ({@link #appendCetakUpload(Class, String[])}) untuk
 *       mengekspor/mengimpor data lewat berkas Excel &mdash; tombol unggah otomatis hanya tampil
 *       bagi pengguna yang punya hak ubah DAN hapus sekaligus, karena unggah massal berpotensi
 *       menimpa/menghapus data lama.</li>
 *   <li><b>Pembangun formulir tambah/ubah</b> ({@link #prepareFormWindow(String)} dan
 *       {@link #finaliseFormWindow(FormHolder, EventListener)}) &mdash; kerangka jendela popup
 *       standar (grid dua kolom label-input, tombol Batal &amp; Simpan) yang tinggal diisi
 *       baris-baris formulir spesifik oleh subclass lewat {@link #addFormRow(Rows, String)}.</li>
 *   <li><b>Penyegar tabel data</b> ({@link #refreshGridData(List, MyRowRenderer)}) yang memasang
 *       ulang model data grid tanpa perlu membangun ulang seluruh komponen tabel dari nol.</li>
 * </ul>
 *
 * <h3>Kontrak yang harus dipenuhi subclass</h3>
 * <p>
 * Kelas ini mengimplementasikan tiga antarmuka standar aplikasi ({@link DataCriteria},
 * {@link DataSearchDefault}, {@link DataInitDefault}) namun sengaja TIDAK mengimplementasikan
 * method-method di dalamnya secara konkret (kelas ini {@code abstract}) &mdash; setiap subclass
 * WAJIB menyediakan sendiri: {@code initCriteria(boolean)} (kriteria pencarian Hibernate),
 * {@code onSearchDefault(Event)} (pemicu pencarian ulang &amp; penyegaran tabel), dan
 * {@code init(GeneralValueObject)} (menyiapkan &amp; membuka formulir tambah/ubah untuk satu
 * entitas). Field-field yang otomatis terhubung dari ZUL (mis. {@link #grid}, {@link #paging},
 * {@link #searchnama}, {@link #searchaktif}, {@link #addWindow}, {@link #add}) HARUS memakai id
 * yang PERSIS SAMA di setiap berkas ZUL yang memakai composer turunan kelas ini, karena ZK
 * menyambungkannya berdasarkan kecocokan id komponen.
 * </p>
 *
 * <h3>Kenapa dipisah dari {@code ais.action.master.spmi.BaseSPMIAction}</h3>
 * <p>
 * Struktur kelas ini SENGAJA meniru persis {@code BaseSPMIAction} milik modul Audit Mutu Internal
 * akademik (SPMI) yang sudah lama dipakai di produksi, karena kebutuhan "layar CRUD dengan kartu
 * filter + tabel + formulir popup" pada dasarnya sama di kedua modul. Namun keduanya tetap dijaga
 * sebagai kelas terpisah (bukan satu kelas dasar bersama di paket umum) supaya perubahan pada satu
 * modul TIDAK pernah tanpa sengaja memengaruhi perilaku modul lain yang sudah stabil di produksi
 * &mdash; prinsip kehati-hatian yang lebih penting daripada penghematan baris kode semata.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public abstract class BaseSPIAction extends GenericAutowireComposer
        implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = 1L;

    // ---- Field yang tersambung otomatis dari ZUL (id HARUS sama persis di setiap ZUL) ----
    protected MyWindow         addWindow;
    protected Paging           paging;
    protected MyGrid           grid;
    protected Textbox          searchnama;
    protected Checkbox         searchaktif;
    protected MyToolbarbuttonConfig add;

    // ---- Bendera hak akses, diisi initPrivileges() ----
    protected boolean edit;
    protected boolean delete;

    /** Wadah sederhana hasil {@link #prepareFormWindow(String)}: rangka border-layout + baris formulir. */
    protected static final class FormHolder {
        public final Borderlayout borderlayout;
        public final Rows         rows;

        FormHolder(Borderlayout bl, Rows r) {
            this.borderlayout = bl;
            this.rows         = r;
        }
    }

    // =====================================================================
    // Siklus hidup ZK
    // =====================================================================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
            org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent,
            org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    // =====================================================================
    // Pembantu inisialisasi bersama
    // =====================================================================

    /** Membaca bendera hak akses dari sesi &amp; mengatur tampil/tidaknya tombol Tambah. */
    protected void initPrivileges() {
        if (add != null) {
            add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
            add.setTooltiptext("Tambah");
        }
        edit   = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
    }

    /** Memasang komponen paging supaya perpindahan halaman memicu {@link #onSearchDefault(Event)}. */
    protected void initPagingListener() {
        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSearchDefault(null);
            }
        });
    }

    /**
     * Menambahkan tombol Cetak &amp; Unggah setelah tombol Tambah. Tombol Unggah hanya tampil
     * bila pengguna punya hak ubah DAN hapus sekaligus (unggah massal bisa menimpa/menghapus data).
     */
    protected void appendCetakUpload(Class<?> clazz, String[] contents) {
        MyToolbarbuttonConfig cetak = Common.cetakData(clazz, this, contents);
        if (add != null) {
            add.getParent().appendChild(cetak);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, clazz, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
            add.getParent().appendChild(upload);
        }
    }

    // =====================================================================
    // Pembantu popup tambah/ubah
    // =====================================================================

    /** Menampilkan {@link #addWindow} sebagai dialog modal. */
    protected void openAddWindow() throws Exception {
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    /**
     * Mengosongkan &amp; mengatur judul jendela popup, lalu membangun rangka standar dua-kolom
     * (label 30% + input) tempat baris-baris formulir akan disisipkan.
     *
     * @return {@link FormHolder} berisi border-layout &amp; wadah baris (Rows) yang siap diisi
     *         lewat {@link #addFormRow(Rows, String)}.
     */
    protected FormHolder prepareFormWindow(String title) {
        addWindow.setTitle(title);
        Common.clear(addWindow);

        Borderlayout bl = new MyBorderlayout();

        Center center = new Center();
        center.setParent(bl);
        ZkCompat.setFlex(center, true);

        MyGrid formGrid = new MyGrid();
        formGrid.setWidth("100%");
        formGrid.setHeight("100%");
        formGrid.setParent(center);

        Columns columns = new Columns();
        columns.setParent(formGrid);
        MyColumnConfig labelCol = new MyColumnConfig();
        labelCol.setWidth("30%");
        labelCol.setParent(columns);
        new MyColumnConfig().setParent(columns);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        return new FormHolder(bl, rows);
    }

    /**
     * Menambahkan toolbar South (Batal + Simpan) lalu menautkan border-layout ke {@link #addWindow}.
     * WAJIB dipanggil setelah seluruh baris formulir selesai ditambahkan.
     */
    protected void finaliseFormWindow(final FormHolder fh, final EventListener onSaveAction) {
        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(fh.borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", onSaveAction);
        save.setParent(toolbar);

        fh.borderlayout.setParent(addWindow);
    }

    // =====================================================================
    // Pembangun baris formulir
    // =====================================================================

    /** Menambahkan satu baris berlabel ke {@code rows}, mengembalikannya agar pemanggil menambah input-nya. */
    protected Row addFormRow(Rows rows, String labelText) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new MyLabelConfig(labelText));
        return row;
    }

    // =====================================================================
    // Pembantu penyegaran tabel
    // =====================================================================

    @SuppressWarnings("unchecked")
    protected <T> void refreshGridData(List<T> data, MyRowRenderer renderer) {
        grid.setRowRenderer(renderer);
        grid.setModelCheckMobile(new SimpleListModel(data));
    }

    /** Pembantu null-safe membaca nilai terpilih dari sebuah Combobox. */
    @SuppressWarnings("unchecked")
    protected static <T> T selectedValue(Combobox cb) {
        return (cb == null || cb.getSelectedItem() == null) ? null : (T) cb.getSelectedItem().getValue();
    }
}
