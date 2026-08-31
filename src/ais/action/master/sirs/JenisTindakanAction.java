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
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.JenisTindakan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Aksi CRUD (via kerangka {@link GenericCrudAction}) untuk kelola master data
 * {@link JenisTindakan} (jenis tindakan medis) pada modul SIRS: daftar dengan pencarian nama,
 * formulir tambah/ubah kustom (nama + keterangan), validasi nama tidak boleh duplikat, dan
 * pencatatan riwayat revisi lewat {@link RevisiHelper} pada setiap baris tabel.
 */
public class JenisTindakanAction extends GenericCrudAction<JenisTindakan> {

    private static final long serialVersionUID = -5779730267402400330L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    /** @return {@link JenisTindakan}, kelas entitas yang dikelola aksi ini. */
    @Override
    protected Class<JenisTindakan> getEntityClass() { return JenisTindakan.class; }

    /** @return instans {@link JenisTindakan} kosong untuk formulir tambah data baru. */
    @Override
    protected JenisTindakan createNewEntity() { return new JenisTindakan(); }

    /** @return judul jendela daftar/aksi ini. */
    @Override
    protected String getWindowTitle() { return "Pendataan Jenis Tindakan"; }

    /** @return kriteria pencarian {@link JenisTindakan} berdasarkan nama (ILIKE), diurutkan menurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisTindakan.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** @return renderer baris tabel {@link JenisTindakanRenderer} untuk daftar jenis tindakan. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new JenisTindakanRenderer();
    }

    // ======================== Form content ========================

    /** Menyusun formulir tambah/ubah (field nama dan keterangan) beserta tombol Batal/Simpan pada jendela modal. */
    @Override
    protected void buildFormContent(MyWindow window, final JenisTindakan jenisTindakan) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card + header
        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        // Plain Grid
        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        nama = new Textbox(jenisTindakan.getNama() == null ? "" : jenisTindakan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Jenis Tindakan", nama);

        keterangan = new Textbox(jenisTindakan.getKeterangan() == null ? "" : jenisTindakan.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

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
     * Memvalidasi nama wajib diisi dan belum terdaftar (lewat {@link #checkNamaJenisTindakan()}),
     * lalu menyimpan (buat baru atau perbarui) entitas {@link JenisTindakan}.
     *
     * @param event event pemicu (tidak dipakai)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (jendela tetap terbuka)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Tindakan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Jenis Tindakan pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaJenisTindakan()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Tindakan yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama jenis tindakan yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        JenisTindakan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (JenisTindakan) session.load(JenisTindakan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** @return {@code true} bila nama pada formulir sudah dipakai jenis tindakan lain (mengecualikan record yang sedang diedit). */
    public Boolean checkNamaJenisTindakan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(JenisTindakan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris tabel: kode+nama (via {@link RevisiHelper}), keterangan, dan tombol ubah/hapus. */
    class JenisTindakanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JenisTindakan jenisTindakan = (JenisTindakan) arg1;

            RevisiHelper.createNewRevisi(JenisTindakan.class, jenisTindakan, jenisTindakan.getNama()).setParent(arg0);
            new Label(jenisTindakan.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, jenisTindakan, JenisTindakanAction.this).setParent(arg0);
        }
    }
}
