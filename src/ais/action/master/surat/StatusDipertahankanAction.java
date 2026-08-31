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
import ais.database.model.surat.StatusDipertahankan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Status Dipertahankan Surat (klasifikasi retensi/status arsip surat pada
 * modul persuratan, mis. status penyimpanan dokumen di kearsipan). Memperluas
 * {@link GenericCrudAction} untuk mewarisi kerangka baku cari/tambah/ubah/hapus, ditambah aksi
 * cetak dan unggah massal (lewat {@link #getDownloadUploadContents()}/{@link #onAfterInit}).
 * Kelas ini mengisi bagian spesifik entitas: kriteria pencarian (status aktif + nama), form input
 * (kode + nama + keterangan), validasi nama wajib dan tidak boleh duplikat, toggle aktif langsung
 * dari grid, serta renderer baris.
 */
public class StatusDipertahankanAction extends GenericCrudAction<StatusDipertahankan> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<StatusDipertahankan> getEntityClass() { return StatusDipertahankan.class; }

    @Override
    protected StatusDipertahankan createNewEntity() { return new StatusDipertahankan(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Status Dipertahankan Surat"; }

    /** Kolom yang disertakan pada template unduh/unggah massal data status dipertahankan. */
    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "keterangan", "aktif" };
    }

    /** Menambahkan tombol cetak dan unggah massal di sebelah tombol tambah, mengikuti hak akses tambah/ubah/hapus pengguna. */
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(StatusDipertahankan.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, StatusDipertahankan.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    /** Menyusun kriteria pencarian {@link StatusDipertahankan}, difilter status aktif (bila dicentang) dan nama, diurutkan berdasarkan nama bila diminta. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(StatusDipertahankan.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Menyediakan renderer baris grid {@link StatusDipertahankanRenderer} untuk daftar hasil pencarian. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new StatusDipertahankanRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah status dipertahankan (field kode + nama + keterangan) beserta tombol batal/simpan pada jendela dialog. */
    @Override
    protected void buildFormContent(MyWindow window, final StatusDipertahankan statusDipertahankan) throws Exception {
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

        kode = new Textbox(statusDipertahankan.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Status Dipertahankan", kode);

        nama = new Textbox(statusDipertahankan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Status Dipertahankan *", nama);

        keterangan = new Textbox(statusDipertahankan.getKeterangan());
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
     * Memvalidasi lalu menyimpan data status dipertahankan dari form: menolak bila nama kosong atau
     * sudah terdaftar pada baris lain; jika lolos menyimpan/memperbarui entitas dan mengembalikan
     * {@code true}.
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan Hibernate saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Status Dipertahankan Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Status; (2) isikan nama status secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaStatusDipertahankan()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Status Dipertahankan Surat sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar status yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        StatusDipertahankan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (StatusDipertahankan) session.load(StatusDipertahankan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /**
     * Memeriksa apakah nama status dipertahankan yang diisi di form sudah dipakai baris lain
     * (mengecualikan baris yang sedang diedit sendiri).
     *
     * @return {@code true} bila nama sudah terpakai baris lain, {@code false} bila belum
     */
    public Boolean checkNamaStatusDipertahankan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(StatusDipertahankan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid daftar status dipertahankan: kolom kode, nama (dengan link riwayat revisi), keterangan, checkbox aktif (toggle langsung tersimpan), dan tombol edit/hapus. */
    class StatusDipertahankanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final StatusDipertahankan statusDipertahankan = (StatusDipertahankan) arg1;

            new Label(statusDipertahankan.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(StatusDipertahankan.class, statusDipertahankan,
                    statusDipertahankan.getNama()).setParent(arg0);
            new Label(statusDipertahankan.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(statusDipertahankan.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    statusDipertahankan.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(statusDipertahankan);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, statusDipertahankan, StatusDipertahankanAction.this).setParent(arg0);
        }
    }
}
