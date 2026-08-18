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
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Gudang;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class GudangAction extends GenericCrudAction<Gudang> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Combobox gudangInduk;
    private Textbox alamat;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Gudang> getEntityClass() { return Gudang.class; }

    @Override
    protected Gudang createNewEntity() { return new Gudang(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Gudang"; }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Gudang.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new GudangRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Gudang gudang) throws Exception {
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

        kode = new Textbox(gudang.getKode() == null ? "" : gudang.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Gudang", kode);

        nama = new Textbox(gudang.getNama() == null ? "" : gudang.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Gudang", nama);

        gudangInduk = new Combobox();
        Common.insertCombo(gudangInduk, "nama", "keterangan", Gudang.class);
        Common.selectComboItem(gudangInduk, gudang.getGudangInduk() == null ? null : gudang.getGudangInduk());
        gudangInduk.setWidth("100%");
        fb.addRow("Gudang Induk", gudangInduk);

        alamat = new Textbox(gudang.getAlamat() == null ? "" : gudang.getAlamat());
        alamat.setWidth("100%");
        alamat.setRows(4);
        fb.addRow("Alamat", alamat);

        keterangan = new Textbox(gudang.getKeterangan() == null ? "" : gudang.getKeterangan());
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
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Gudang wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode Gudang pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kode terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Gudang wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Gudang pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkKodeGudang()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Gudang yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan kode gudang yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Gudang entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Gudang) session.load(Gudang.class, entity.getId());
            currentEntity = entity;
        }
        entity.setAlamat(alamat.getValue());
        entity.setGudangInduk((Gudang) (gudangInduk.getSelectedItem() == null ? null : gudangInduk.getSelectedItem().getValue()));
        entity.setKode(kode.getValue().trim());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkKodeGudang() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Gudang.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class GudangRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Gudang gudang = (Gudang) arg1;

            new Label(gudang.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Gudang.class, gudang, gudang.getNama()).setParent(arg0);
            new Label(gudang.getGudangInduk() == null ? "" : gudang.getGudangInduk().getNama()).setParent(arg0);
            new Label(gudang.getAlamat()).setParent(arg0);
            new Label(gudang.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, gudang, GudangAction.this).setParent(arg0);
        }
    }
}
