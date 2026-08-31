package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.RacikanDetailAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.JenisRacikan;
import ais.database.model.sirs.Racikan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD (berbasis {@link GenericCrudAction}) untuk data master <b>Racikan</b> obat modul
 * SIRS. Kode racikan dibangkitkan otomatis ({@link Common#generateCode(Class, int)}, 8 karakter,
 * field readonly di form) dan tidak dapat diedit manual. Pencarian hanya menampilkan racikan
 * "induk" ({@code variasiDari} kosong) — racikan yang merupakan variasi dari racikan lain
 * disembunyikan dari daftar utama. Setiap baris grid menampilkan komponen
 * {@link RacikanDetailAction} untuk mengelola rincian bahan racikan. Penghapusan menghapus dulu
 * baris {@code sirs.racikan_detail} terkait sebelum menghapus racikan itu sendiri.
 */
public class RacikanAction extends GenericCrudAction<Racikan> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private Combobox jenisRacikan;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    /** Mengembalikan kelas entitas yang dikelola layar ini: {@link Racikan}. */
    @Override
    protected Class<Racikan> getEntityClass() { return Racikan.class; }

    /** Membuat instance {@link Racikan} kosong untuk form tambah data baru. */
    @Override
    protected Racikan createNewEntity() { return new Racikan(); }

    /** Mengembalikan judul jendela form: {@code "Pendataan Racikan"}. */
    @Override
    protected String getWindowTitle() { return "Pendataan Racikan"; }

    /** Membangun kriteria pencarian racikan induk (bukan variasi), diurutkan berdasarkan id terbaru, disaring berdasarkan kecocokan sebagian nama pada kotak pencarian bila diisi. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Racikan.class)
                .add(Restrictions.isNull("variasiDari"))
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    /** Membuat perender baris grid pencarian racikan: {@link RacikanRenderer}. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new RacikanRenderer();
    }

    // ======================== Form content ========================

    /** Membangun tata letak form tambah/edit racikan (kode otomatis readonly, nama, jenis racikan, keterangan) dengan toolbar simpan/batal di dalam {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final Racikan racikan) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card wrapper
        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        // Header

        // Plain Grid
        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new MyTextbox(racikan.getKode() == null ? Common.generateCode(Racikan.class, 8) : racikan.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Racikan", kode);

        nama = new MyTextbox(racikan.getNama() == null ? "" : racikan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Racikan", nama);

        jenisRacikan = new Combobox();
        Common.insertCombo(jenisRacikan, "nama", JenisRacikan.class);
        Common.selectComboItem(jenisRacikan, racikan.getJenisRacikan());
        jenisRacikan.setWidth("100%");
        fb.addRow("Jenis Racikan", jenisRacikan);

        keterangan = new MyTextbox(racikan.getKeterangan() == null ? "" : racikan.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        // South + Toolbar
        org.zkoss.zul.South south = new org.zkoss.zul.South();
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        save.setParent(toolbar);
        borderlayout.setParent(window);
    }

    // ======================== Save logic ========================

    /**
     * Memvalidasi (nama wajib, nama belum dipakai racikan lain) dan menyimpan/memperbarui data
     * racikan dari isian form saat ini; data baru diberi kode otomatis lewat
     * {@link Common#generateCode(Class, int)}.
     *
     * @param event event pemicu tombol simpan
     * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila validasi gagal
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show(
                    "Nama Racikan harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Nama Racikan; (2) pastikan isian tidak kosong; (3) ulangi proses penyimpanan.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaRacikan()) {
            MyMessageboxConfig.show(
                    "Nama Racikan yang dimasukkan sudah terdaftar di dalam sistem. Langkah yang dapat dilakukan: (1) gunakan nama racikan yang berbeda; (2) periksa kembali data racikan yang telah ada; (3) ulangi proses penyimpanan.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Racikan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Racikan) session.load(Racikan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setJenisRacikan(jenisRacikan.getSelectedItem() == null
                ? null : (JenisRacikan) jenisRacikan.getSelectedItem().getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (entity.getId() != null) {
            Common.refreshUpdate(session, entity);
        } else {
            entity.setKode(Common.generateCode(Racikan.class, 8));
            session.save(entity);
        }
        return true;
    }

    /** Memeriksa apakah nama pada form sudah dipakai racikan lain (mengecualikan record yang sedang diedit sendiri). */
    public Boolean checkNamaRacikan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Racikan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /**
     * Perender baris grid pencarian racikan: menampilkan komponen rincian bahan
     * ({@link RacikanDetailAction}), kode, nama (dengan tautan riwayat revisi), jenis racikan,
     * keterangan, dan tombol ubah/hapus kustom — sebelum menghapus baris racikan, terlebih dahulu
     * menghapus seluruh baris {@code sirs.racikan_detail} yang tertaut lewat SQL native. Kegagalan
     * hapus (mis. masih dipakai transaksi lain) ditangkap dan ditampilkan sebagai pesan error.
     */
    class RacikanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Racikan racikan = (Racikan) arg1;

            new RacikanDetailAction(racikan, true).setParent(arg0);
            new Label(racikan.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Racikan.class, racikan, racikan.getNama()).setParent(arg0);
            new Label(racikan.getJenisRacikan() == null ? "" : racikan.getJenisRacikan().getNama()).setParent(arg0);
            new Label(racikan.getKeterangan()).setParent(arg0);

            org.zkoss.zul.Hbox toolbar = new org.zkoss.zul.Hbox();

            Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            btnEdit.setTooltiptext("Rubah Data");
            btnEdit.setVisible(edit);
            btnEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    currentEntity = racikan;
                    buildFormContent(addWindow, racikan);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            btnEdit.setParent(toolbar);

            Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");
            btnDelete.setTooltiptext("Hapus Data");
            btnDelete.setVisible(delete);
            btnDelete.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menghapus data ini? Data racikan beserta rinciannya akan dihapus secara permanen dan tidak dapat dikembalikan.",
                            "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            Session session = HibernateUtil.currentSession();
                                            session.createSQLQuery(
                                                    "delete from sirs.racikan_detail where racikan = " + racikan.getId())
                                                    .executeUpdate();
                                            Common.refreshDelete(session, racikan);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait; (2) pastikan data ini tidak sedang digunakan pada transaksi lain; (3) apabila kendala berlanjut, hubungi administrator sistem.",
                                                    e.getMessage()));
                                        }
                                    }
                                }
                            });
                }
            });
            btnDelete.setParent(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
