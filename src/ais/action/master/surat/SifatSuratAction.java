package ais.action.master.surat;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.surat.SifatSurat;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD (berbasis {@link GenericCrudAction}) untuk data master <b>Sifat Surat</b> (mis.
 * Biasa, Penting, Rahasia) modul persuratan. Mendukung cetak dan unggah data massal lewat
 * {@link Common#cetakData}/{@link Common#uploadData} pada kolom {@link #getDownloadUploadContents()}.
 * Validasi menolak nama kosong atau nama yang sudah dipakai sifat surat lain
 * ({@link #checkNamaSifatSurat()}). Grid pencarian mendukung filter status aktif dan toggle
 * aktif/nonaktif langsung dari baris.
 */
public class SifatSuratAction extends GenericCrudAction<SifatSurat> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    /** Mengembalikan kelas entitas yang dikelola layar ini: {@link SifatSurat}. */
    @Override
    protected Class<SifatSurat> getEntityClass() { return SifatSurat.class; }

    /** Membuat instance {@link SifatSurat} kosong untuk form tambah data baru. */
    @Override
    protected SifatSurat createNewEntity() { return new SifatSurat(); }

    /** Mengembalikan judul jendela form: {@code "Pendataan Sifat Surat"}. */
    @Override
    protected String getWindowTitle() { return "Pendataan Sifat Surat"; }

    /** Mengembalikan kolom yang disertakan pada unduh/unggah data massal: {@code id, kode, nama, keterangan, aktif}. */
    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "keterangan", "aktif" };
    }

    /** Menambahkan tombol cetak dan unggah data massal ke toolbar (di sebelah tombol tambah), sesuai hak akses pengguna. */
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(SifatSurat.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, SifatSurat.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    /** Membangun kriteria pencarian sifat surat, diurutkan berdasarkan nama, disaring status aktif dan/atau kecocokan sebagian nama sesuai filter pencarian aktif. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(SifatSurat.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Membuat perender baris grid pencarian sifat surat: {@link SifatSuratRenderer}. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new SifatSuratRenderer();
    }

    // ======================== Form content ========================

    /** Membangun tata letak form tambah/edit sifat surat (kode, nama, keterangan) dengan toolbar simpan/batal di dalam {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final SifatSurat sifatSurat) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center with card
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(sifatSurat.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Sifat Surat", kode);

        nama = new Textbox(sifatSurat.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Sifat Surat *", nama);

        keterangan = new Textbox(sifatSurat.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
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
     * Memvalidasi (nama wajib, nama belum dipakai sifat surat lain) dan menyimpan/memperbarui data
     * sifat surat dari isian form saat ini.
     *
     * @param event event pemicu tombol simpan
     * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila validasi gagal
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Sifat Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Sifat Surat; (2) isikan nama sifat surat secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaSifatSurat()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Sifat Surat sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar sifat surat yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        SifatSurat entity = currentEntity;
        if (entity.getId() != null) {
            entity = (SifatSurat) session.load(SifatSurat.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** Memeriksa apakah nama pada form sudah dipakai sifat surat lain (mengecualikan record yang sedang diedit sendiri). */
    public Boolean checkNamaSifatSurat() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(SifatSurat.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Perender baris grid pencarian sifat surat: menampilkan kode, nama (dengan tautan riwayat revisi), keterangan, toggle aktif langsung tersimpan saat diklik, dan tombol ubah/hapus. */
    class SifatSuratRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final SifatSurat sifatSurat = (SifatSurat) arg1;

            new Label(sifatSurat.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(SifatSurat.class, sifatSurat, sifatSurat.getNama()).setParent(arg0);
            new Label(sifatSurat.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(sifatSurat.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    sifatSurat.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(sifatSurat);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, sifatSurat, SifatSuratAction.this).setParent(arg0);
        }
    }
}
