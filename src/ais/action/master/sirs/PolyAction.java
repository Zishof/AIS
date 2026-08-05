package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class PolyAction extends GenericCrudAction<Poly> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search field (auto-wired from ZUL)
    private Combobox searchjenis;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private Combobox jenis;
    private Combobox polyDari;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Poly> getEntityClass() { return Poly.class; }

    @Override
    protected Poly createNewEntity() { return new Poly(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Poli"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initJenis(searchjenis);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Poly.class)
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchjenis == null || searchjenis.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PolyRenderer();
    }

    private void initJenis(Combobox combobox) {
        if (combobox == null) return;
        Comboitem comboitem = new Comboitem(Pendaftaran.RAWAT_JALAN);
        comboitem.setValue(Pendaftaran.RAWAT_JALAN);
        combobox.appendChild(comboitem);
        comboitem = new Comboitem(Pendaftaran.RAWAT_INAP);
        comboitem.setValue(Pendaftaran.RAWAT_INAP);
        combobox.appendChild(comboitem);
        comboitem = new Comboitem(Pendaftaran.RAWAT_UGD);
        comboitem.setValue(Pendaftaran.RAWAT_UGD);
        combobox.appendChild(comboitem);
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Poly poly) throws Exception {
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

        kode = new MyTextbox(poly.getKode() == null ? "" : poly.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Poli", kode);

        nama = new MyTextbox(poly.getNama() == null ? "" : poly.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Poli", nama);

        jenis = new Combobox();
        initJenis(jenis);
        Common.selectComboItem(jenis, poly.getJenis());
        jenis.setWidth("100%");
        fb.addRow("Jenis Poli", jenis);

        polyDari = new Combobox();
        Common.insertCombo(polyDari, "nama", Poly.class);
        Common.selectComboItem(polyDari, poly.getPolyDari());
        polyDari.setWidth("100%");
        fb.addRow("Bagian dari Poli", polyDari);

        keterangan = new MyTextbox(poly.getKeterangan() == null ? "" : poly.getKeterangan());
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
            MyMessageboxConfig.show("Mohon maaf, Nama Poli wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Poli pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenis.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Poli wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Poli pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah jenis poli ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaPoly()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Poli yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama poli yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Poly entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Poly) session.load(Poly.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setPolyDari((Poly) (polyDari.getSelectedItem() == null ? null : polyDari.getSelectedItem().getValue()));
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setJenis((String) jenis.getSelectedItem().getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaPoly() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Poly.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class PolyRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Poly poly = (Poly) arg1;

            new Label(poly.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Poly.class, poly, poly.getNama()).setParent(arg0);
            new Label(poly.getJenis()).setParent(arg0);
            new Label(poly.getPolyDari() == null ? "" : poly.getPolyDari().getNama()).setParent(arg0);
            new Label(poly.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, poly, PolyAction.this).setParent(arg0);
        }
    }
}
