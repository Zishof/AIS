package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.KomunitasPunyaPasienAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Komunitas;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class KomunitasAction extends GenericCrudAction<Komunitas> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private MyTextbox nama;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private Checkbox aktif;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Komunitas> getEntityClass() { return Komunitas.class; }

    @Override
    protected Komunitas createNewEntity() { return new Komunitas(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Komunitas"; }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Komunitas.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KomunitasRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Komunitas komunitas) throws Exception {
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

        nama = new MyTextbox(komunitas.getNama() == null ? "" : komunitas.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Komunitas", nama);

        mulai = new MyDatebox(komunitas.getMulai());
        mulai.setWidth("100%");
        fb.addRow("Komunitas berlaku mulai", mulai);

        sampai = new MyDatebox(komunitas.getSampai());
        sampai.setWidth("100%");
        fb.addRow("Komunitas berlaku sampai", sampai);

        aktif = new Checkbox();
        aktif.setChecked(komunitas.getAktif());
        fb.addRow("Aktif (berlaku)", aktif);

        keterangan = new MyTextbox(komunitas.getKeterangan() == null ? "" : komunitas.getKeterangan());
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

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Komunitas wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Komunitas pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (mulai.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, tanggal Mulai Berlaku Komunitas wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal mulai berlaku pada kolom yang tersedia; (2) pastikan kolom tanggal tidak dikosongkan; (3) simpan kembali data setelah tanggal terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaKomunitas()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Komunitas yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama komunitas yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Komunitas entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Komunitas) session.load(Komunitas.class, entity.getId());
            currentEntity = entity;
        }
        entity.setMulai(mulai.getValue());
        entity.setSampai(sampai.getValue());
        entity.setAktif(aktif.isChecked());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaKomunitas() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Komunitas.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class KomunitasRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Komunitas komunitas = (Komunitas) arg1;

            new KomunitasPunyaPasienAction(komunitas).setParent(arg0);
            RevisiHelper.createNewRevisi(Komunitas.class, komunitas, komunitas.getNama()).setParent(arg0);
            new Label(komunitas.getMulai() == null ? "" : Common.dateFormat3.get().format(komunitas.getMulai())).setParent(arg0);
            new Label(komunitas.getSampai() == null ? "" : Common.dateFormat3.get().format(komunitas.getSampai())).setParent(arg0);
            new Label(komunitas.getAktif() ? "Ya" : "Tidak").setParent(arg0);
            new Label(komunitas.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, komunitas, KomunitasAction.this).setParent(arg0);
        }
    }
}
