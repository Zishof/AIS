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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sosial.GelombangDonatur;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class GelombangDonaturAction extends GenericCrudAction<GelombangDonatur> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<GelombangDonatur> getEntityClass() { return GelombangDonatur.class; }

    @Override
    protected GelombangDonatur createNewEntity() { return new GelombangDonatur(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Masa Pendaftaran Donatur"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "mulai", "sampai", "keterangan", "aktif" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(GelombangDonatur.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, GelombangDonatur.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(GelombangDonatur.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.or(
                        Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
                        Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new GelombangDonaturRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final GelombangDonatur gelombangDonatur) throws Exception {
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

        kode = new Textbox(gelombangDonatur.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        nama = new Textbox(gelombangDonatur.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama *", nama);

        mulai = new MyDatebox(gelombangDonatur.getMulai());
        mulai.setReadonly(true);
        fb.addRow("Mulai", mulai);

        sampai = new MyDatebox(gelombangDonatur.getSampai());
        sampai.setReadonly(true);
        fb.addRow("Sampai", sampai);

        keterangan = new Textbox(gelombangDonatur.getKeterangan());
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
            MyMessageboxConfig.show("Nama Masa Pendaftaran Donatur harus diisi", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaGelombangDonatur()) {
            MyMessageboxConfig.show("Nama Masa Pendaftaran Donatur sudah ada di database", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        GelombangDonatur entity = currentEntity;
        if (entity.getId() != null) {
            entity = (GelombangDonatur) session.load(GelombangDonatur.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setMulai(mulai.getValue());
        entity.setSampai(sampai.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaGelombangDonatur() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(GelombangDonatur.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class GelombangDonaturRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final GelombangDonatur gelombangDonatur = (GelombangDonatur) arg1;

            new Label(gelombangDonatur.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(GelombangDonatur.class, gelombangDonatur, gelombangDonatur.getNama()).setParent(arg0);
            new Label(gelombangDonatur.getMulai() == null ? ""
                    : Common.dateFormat4.get().format(gelombangDonatur.getMulai())).setParent(arg0);
            new Label(gelombangDonatur.getSampai() == null ? ""
                    : Common.dateFormat4.get().format(gelombangDonatur.getSampai())).setParent(arg0);
            new Label(gelombangDonatur.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(gelombangDonatur.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    gelombangDonatur.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(gelombangDonatur);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, gelombangDonatur, GelombangDonaturAction.this).setParent(arg0);
        }
    }
}
