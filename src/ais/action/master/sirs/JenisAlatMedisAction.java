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
import ais.database.model.sirs.JenisAlatMedis;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Jenis Alat Medis pada modul SIRS (Sistem Informasi Rumah Sakit),
 * dibangun di atas {@link GenericCrudAction}. Mengelola daftar sederhana nama + keterangan jenis
 * alat medis (mis. Diagnostik, Bedah, Laboratorium), tanpa relasi ke entitas lain.
 *
 * <p>
 * Pencarian daftar difilter berdasarkan kecocokan sebagian nama ({@code ilike ANYWHERE}). Form
 * simpan memvalidasi nama tidak kosong dan tidak duplikat (dicek lewat
 * {@link #checkNamaJenisAlatMedis()}) sebelum menyimpan; baris tabel dirender lewat
 * {@link JenisAlatMedisRenderer} yang menampilkan riwayat revisi ({@link RevisiHelper}) di
 * samping nama.
 * </p>
 */
public class JenisAlatMedisAction extends GenericCrudAction<JenisAlatMedis> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<JenisAlatMedis> getEntityClass() { return JenisAlatMedis.class; }

    @Override
    protected JenisAlatMedis createNewEntity() { return new JenisAlatMedis(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Jenis Alat Medis"; }

    /** Membangun kriteria pencarian daftar jenis alat medis, difilter berdasarkan kecocokan sebagian nama bila {@code searchnama} diisi. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisAlatMedis.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new JenisAlatMedisRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah (nama + keterangan) beserta toolbar Batal/Simpan pada {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final JenisAlatMedis jenisAlatMedis) throws Exception {
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

        nama = new Textbox(jenisAlatMedis.getNama() == null ? "" : jenisAlatMedis.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Jenis Alat Medis", nama);

        keterangan = new Textbox(jenisAlatMedis.getKeterangan() == null ? "" : jenisAlatMedis.getKeterangan());
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
     * Memvalidasi (nama wajib isi, nama tidak duplikat) dan menyimpan (create-or-update) entitas
     * jenis alat medis dari isian form.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
     *         peringatan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Alat Medis wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Jenis Alat Medis pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaJenisAlatMedis()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Alat Medis yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama jenis alat medis yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        JenisAlatMedis entity = currentEntity;
        if (entity.getId() != null) {
            entity = (JenisAlatMedis) session.load(JenisAlatMedis.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** Mengecek apakah nama pada form sudah dipakai jenis alat medis lain (di luar entitas yang sedang diedit). */
    public Boolean checkNamaJenisAlatMedis() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(JenisAlatMedis.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Perenderan satu baris tabel jenis alat medis: nama (dengan tautan riwayat revisi), keterangan, dan tombol edit/hapus. */
    class JenisAlatMedisRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) arg1;

            RevisiHelper.createNewRevisi(JenisAlatMedis.class, jenisAlatMedis, jenisAlatMedis.getNama()).setParent(arg0);
            new Label(jenisAlatMedis.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, jenisAlatMedis, JenisAlatMedisAction.this).setParent(arg0);
        }
    }
}
