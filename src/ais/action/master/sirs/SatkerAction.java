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

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Satker;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD modul SIRS untuk {@link Satker} (satuan kerja rumah sakit): nama (wajib unik, dicek
 * lewat {@link #checkNamaSatker()}) dan keterangan, dibangun di atas kerangka generik
 * {@link GenericCrudAction}.
 */
public class SatkerAction extends GenericCrudAction<Satker> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private MyTextbox nama;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    /** @return kelas entitas yang dikelola layar ini, {@link Satker}. */
    @Override
    protected Class<Satker> getEntityClass() { return Satker.class; }

    /** @return instance {@link Satker} kosong untuk form tambah baru. */
    @Override
    protected Satker createNewEntity() { return new Satker(); }

    /** @return judul jendela form tambah/ubah. */
    @Override
    protected String getWindowTitle() { return "Pendataan Satker"; }

    /** Membentuk criteria pencarian {@link Satker} berdasarkan filter nama (ILIKE), diurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Satker.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** @return renderer baris grid untuk {@link Satker} ({@link SatkerRenderer}). */
    @Override
    protected MyRowRenderer createRenderer() {
        return new SatkerRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link Satker}: nama dan keterangan, plus toolbar Batal/Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final Satker satker) throws Exception {
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

        nama = new MyTextbox(satker.getNama() == null ? "" : satker.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Satker", nama);

        keterangan = new MyTextbox(satker.getKeterangan() == null ? "" : satker.getKeterangan());
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

    /** Memvalidasi (nama wajib diisi dan harus unik) dan menyimpan {@link Satker} dari nilai form saat ini. @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Satuan Kerja (Satker) wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Satker pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaSatker()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Satker yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama satker yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Satker entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Satker) session.load(Satker.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** @return {@code true} bila sudah ada {@link Satker} lain dengan nama yang sama persis (mengecualikan entitas yang sedang diedit). */
    public Boolean checkNamaSatker() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Satker.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid {@link Satker}: nama (dengan revisi), keterangan, dan tombol ubah/hapus. */
    class SatkerRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Satker satker = (Satker) arg1;

            RevisiHelper.createNewRevisi(Satker.class, satker, satker.getNama()).setParent(arg0);
            new Label(satker.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, satker, SatkerAction.this).setParent(arg0);
        }
    }
}
