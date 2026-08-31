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
import ais.database.model.sirs.JenisRacikan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD pendataan {@link JenisRacikan} (jenis racikan obat) pada modul SIRS, dibangun di
 * atas kerangka {@link GenericCrudAction}. Menyediakan pencarian ilike berdasarkan nama, form
 * tambah/ubah sederhana (nama + keterangan) dengan validasi nama wajib diisi dan unik.
 */
public class JenisRacikanAction extends GenericCrudAction<JenisRacikan> {

    private static final long serialVersionUID = -5779730267402400329L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    /** Kelas entitas yang dikelola: {@link JenisRacikan}. */
    @Override
    protected Class<JenisRacikan> getEntityClass() { return JenisRacikan.class; }

    /** Membuat instance {@link JenisRacikan} kosong untuk form tambah data baru. */
    @Override
    protected JenisRacikan createNewEntity() { return new JenisRacikan(); }

    /** Judul jendela: {@code "Pendataan Jenis Racikan"}. */
    @Override
    protected String getWindowTitle() { return "Pendataan Jenis Racikan"; }

    /** Menyusun kriteria pencarian {@link JenisRacikan}, difilter ilike berdasarkan nama, terurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisRacikan.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Penyedia renderer baris grid hasil pencarian: {@link JenisRacikanRenderer}. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new JenisRacikanRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link JenisRacikan}: kolom nama dan keterangan, beserta tombol Batal dan Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final JenisRacikan jenisRacikan) throws Exception {
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

        nama = new Textbox(jenisRacikan.getNama() == null ? "" : jenisRacikan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Jenis Racikan", nama);

        keterangan = new Textbox(jenisRacikan.getKeterangan() == null ? "" : jenisRacikan.getKeterangan());
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
     * Memvalidasi dan menyimpan data {@link JenisRacikan}: menolak bila nama kosong atau nama
     * sudah dipakai jenis racikan lain (dicek via {@link #checkNamaJenisRacikan()}), lalu
     * menyimpan/memperbarui entitas.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Racikan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Jenis Racikan pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaJenisRacikan()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Racikan yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama jenis racikan yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        JenisRacikan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (JenisRacikan) session.load(JenisRacikan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** Memeriksa apakah nama pada form sudah dipakai {@link JenisRacikan} lain (mengecualikan entitas yang sedang diedit). */
    public Boolean checkNamaJenisRacikan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(JenisRacikan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid untuk {@link JenisRacikan}: nama (dengan tombol riwayat revisi), keterangan, dan tombol edit/hapus. */
    class JenisRacikanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JenisRacikan jenisRacikan = (JenisRacikan) arg1;

            RevisiHelper.createNewRevisi(JenisRacikan.class, jenisRacikan, jenisRacikan.getNama()).setParent(arg0);
            new Label(jenisRacikan.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, jenisRacikan, JenisRacikanAction.this).setParent(arg0);
        }
    }
}
