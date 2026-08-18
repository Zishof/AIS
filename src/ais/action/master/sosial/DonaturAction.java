package ais.action.master.sosial;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sosial.Donatur;
import ais.database.model.sosial.GelombangDonatur;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class DonaturAction extends GenericCrudAction<Donatur> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search field (auto-wired from ZUL)
    private Textbox searchgelombang;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Textbox telp;
    private Textbox email;
    private Textbox alamat;
    private Combobox gelombangDonatur;
    private AmbilDataNegaraBanbox negara;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Donatur> getEntityClass() { return Donatur.class; }

    @Override
    protected Donatur createNewEntity() { return new Donatur(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Donatur"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "email", "telp", "alamat", "gelombangDonatur", "keterangan", "aktif" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Donatur.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, Donatur.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Donatur.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));

        if (searchgelombang != null && !searchgelombang.getValue().trim().isEmpty()) {
            criteria.createAlias("gelombangDonatur", "gelombangDonatur").add(Restrictions.or(
                    Restrictions.ilike("gelombangDonatur.kode", searchnama.getValue().trim()),
                    Restrictions.ilike("gelombangDonatur.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
        }

        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.or(
                        Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
                        Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new DonaturRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Donatur donatur) throws Exception {
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

        kode = new Textbox(donatur.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Donatur", kode);

        nama = new Textbox(donatur.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Donatur", nama);

        telp = new Textbox(donatur.getTelp());
        telp.setWidth("100%");
        fb.addRow("Telp./WA", telp);

        email = new Textbox(donatur.getEmail());
        email.setWidth("100%");
        fb.addRow("Email", email);

        alamat = new Textbox(donatur.getAlamat());
        alamat.setWidth("100%");
        alamat.setRows(3);
        fb.addRow("Alamat", alamat);

        gelombangDonatur = new Combobox();
        Common.insertComboDanSemua(gelombangDonatur, "nama", GelombangDonatur.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(gelombangDonatur, donatur.getGelombangDonatur());
        gelombangDonatur.setWidth("100%");
        gelombangDonatur.setReadonly(true);
        fb.addRow("Masa Pendaftaran", gelombangDonatur);

        negara = new AmbilDataNegaraBanbox();
        try {
            negara.setAttribute("negara", donatur.getNegara());
            negara.setValue(donatur.getNegara().getNamaNegara());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sosial/DonaturAction.java:168");
            // negara belum diisi
        }
        negara.setReadonly(true);
        negara.setWidth("100%");
        fb.addRow("Asal Negara", negara);

        keterangan = new Textbox(donatur.getKeterangan());
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

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Nama Donatur harus diisi", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (gelombangDonatur.getSelectedItem() == null || gelombangDonatur.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Masa pendaftaran donatur harus diisi", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Donatur entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Donatur) session.load(Donatur.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setAlamat(alamat.getValue().trim());
        entity.setEmail(email.getValue().trim());
        entity.setTelp(telp.getValue().trim());
        entity.setGelombangDonatur((GelombangDonatur) gelombangDonatur.getSelectedItem().getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaDonatur() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Donatur.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class DonaturRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Donatur donatur = (Donatur) arg1;

            new Label(donatur.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Donatur.class, donatur, donatur.getNama()).setParent(arg0);
            new Label(donatur.getEmail()).setParent(arg0);
            new Label(donatur.getTelp()).setParent(arg0);
            new Label(donatur.getAlamat()).setParent(arg0);
            new Label(donatur.getGelombangDonatur() == null ? "" : donatur.getGelombangDonatur().getNama()).setParent(arg0);
            new Label(donatur.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(donatur.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    donatur.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(donatur);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, donatur, DonaturAction.this).setParent(arg0);
        }
    }
}
