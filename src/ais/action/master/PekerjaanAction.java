package ais.action.master;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pekerjaan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class PekerjaanAction extends GenericCrudAction<Pekerjaan> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;
    private Longbox feeder;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Pekerjaan> getEntityClass() { return Pekerjaan.class; }

    @Override
    protected Pekerjaan createNewEntity() { return new Pekerjaan(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Pekerjaan"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "keterangan", "aktif", "feeder" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Pekerjaan.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, Pekerjaan.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Pekerjaan.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PekerjaanRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Pekerjaan pekerjaan) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // ---- Center: scrollable card ----
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

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        nama = new Textbox(pekerjaan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama pekerjaan *", nama);

        keterangan = new Textbox(pekerjaan.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        feeder = new Longbox(pekerjaan.getFeeder());
        feeder.setWidth("100%");
        fb.addRow("Feeder", feeder);

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
            PesanFormalHelper.tampilkanGagal("penyimpanan data pekerjaan",
            		"Kolom Nama pekerjaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama pekerjaan.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaPekerjaan()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data pekerjaan",
            		"Nama pekerjaan sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama pekerjaan yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Pekerjaan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Pekerjaan) session.load(Pekerjaan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setFeeder(feeder.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaPekerjaan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Pekerjaan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class PekerjaanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Pekerjaan pekerjaan = (Pekerjaan) arg1;

            RevisiHelper.createNewRevisi(Pekerjaan.class, pekerjaan, pekerjaan.getNama()).setParent(arg0);
            new Label(pekerjaan.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(pekerjaan.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    pekerjaan.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(pekerjaan);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, pekerjaan, PekerjaanAction.this).setParent(arg0);
        }
    }
}
