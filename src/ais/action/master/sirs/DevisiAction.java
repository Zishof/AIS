package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.DevisiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Devisi;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Devisi (divisi akunting, mis. untuk pembagian tanggung jawab keuangan
 * pada modul SIRS/akunting). Memperluas {@link GenericCrudAction} untuk mewarisi kerangka baku
 * cari/tambah/ubah/hapus; kelas ini mengisi bagian spesifik entitas: kriteria pencarian berdasarkan
 * nama, form input (kode + nama + keterangan), validasi kode/nama wajib dan kode tidak boleh
 * duplikat ({@link #checkKodeDevisi()}), serta renderer baris grid. Berbeda dari sebagian besar
 * layar CRUD sejenis di paket ini, penyimpanan didelegasikan ke {@link DevisiDao} (via
 * {@link DaoFactory}) alih-alih memanggil Hibernate langsung.
 */
public class DevisiAction extends GenericCrudAction<Devisi> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Devisi> getEntityClass() { return Devisi.class; }

    @Override
    protected Devisi createNewEntity() { return new Devisi(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Devisi"; }

    /** Menyusun kriteria pencarian {@link Devisi} berdasarkan nama (filter {@code searchnama}), diurutkan berdasarkan nama bila diminta. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Devisi.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Menyediakan renderer baris grid {@link DevisiRenderer} untuk daftar hasil pencarian. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new DevisiRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah devisi (field kode + nama + keterangan) beserta tombol batal/simpan pada jendela dialog. */
    @Override
    protected void buildFormContent(MyWindow window, final Devisi devisi) throws Exception {
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

        kode = new Textbox(devisi.getKode() == null ? "" : devisi.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Devisi", kode);

        nama = new Textbox(devisi.getNama() == null ? "" : devisi.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Devisi", nama);

        keterangan = new Textbox(devisi.getKeterangan() == null ? "" : devisi.getKeterangan());
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
     * Memvalidasi lalu menyimpan data devisi dari form: menolak bila kode atau nama kosong, atau
     * kode sudah terdaftar pada baris lain; jika lolos menyimpan/memperbarui entitas lewat
     * {@link DevisiDao} dan mengembalikan {@code true}.
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan DAO saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Divisi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode Divisi pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kode terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Divisi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Divisi pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkKodeDevisi()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Divisi yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan kode divisi yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        DevisiDao devisiDao = DaoFactory.getInstance().getDevisiDao();
        Devisi entity = currentEntity;
        if (entity.getId() != null) {
            entity = devisiDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue().trim());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (entity.getId() != null) {
            devisiDao.update(entity);
        } else {
            devisiDao.save(entity);
        }
        return true;
    }

    /**
     * Memeriksa apakah kode devisi yang diisi di form sudah dipakai baris lain (mengecualikan
     * baris yang sedang diedit sendiri).
     *
     * @return {@code true} bila kode sudah terpakai baris lain, {@code false} bila belum
     */
    public Boolean checkKodeDevisi() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Devisi.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid daftar devisi: kolom kode, nama (dengan link riwayat revisi), keterangan, dan tombol edit/hapus. */
    class DevisiRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Devisi devisi = (Devisi) arg1;

            new Label(devisi.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Devisi.class, devisi, devisi.getNama()).setParent(arg0);
            new Label(devisi.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, devisi, DevisiAction.this).setParent(arg0);
        }
    }
}
