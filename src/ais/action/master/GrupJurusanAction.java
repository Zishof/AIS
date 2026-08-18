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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupJurusan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class GrupJurusanAction extends GenericCrudAction<GrupJurusan> {

    private static final long serialVersionUID = 3786091220301468178L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private AmbilDataDosenBanbox kajur;
    private Combobox fakultas;
    private MyCkEditor deskripsi;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<GrupJurusan> getEntityClass() { return GrupJurusan.class; }

    @Override
    protected GrupJurusan createNewEntity() { return new GrupJurusan(); }

    @Override
    protected String getWindowTitle() { return "Pendataan " + Common.getBahasa("label_grupJurusan"); }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "kode", "nama", "kajur", "fakultas", "deskripsi" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(GrupJurusan.class, this,
                "kode", "nama", "kajur", "fakultas", "deskripsi");
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(GrupJurusan.class);
        if (order) criteria.addOrder(Order.asc("kode"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new GrupJurusanRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final GrupJurusan grupJurusan) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        Tabbox tabbox = new Tabbox();
        tabbox.setParent(center);
        tabbox.setHeight("100%");
        tabbox.setWidth("100%");

        Tabs tabs = new Tabs();
        tabs.setParent(tabbox);

        new MyTabConfig("Data").setParent(tabs);
        new MyTabConfig("Deskripsi").setParent(tabs);

        Tabpanels tabpanels = new Tabpanels();
        tabpanels.setParent(tabbox);

        Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
        tabpanelUtama.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        tabpanelUtama.setParent(tabpanels);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(tabpanelUtama);

        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(grupJurusan.getKode() == null ? "" : grupJurusan.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode *", kode);

        nama = new Textbox(grupJurusan.getNama() == null ? "" : grupJurusan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama *", nama);

        fakultas = new Combobox();
        Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class,
                Restrictions.eq("aktif", true));
        Common.selectComboItem(fakultas, grupJurusan.getFakultas());
        fakultas.setWidth("100%");
        fb.addRow("Fakultas", fakultas);

        kajur = new AmbilDataDosenBanbox();
        kajur.setAttribute("dosen", grupJurusan.getKajur());
        kajur.setValue(grupJurusan.getKajur() == null ? "" : grupJurusan.getKajur().getNama());
        kajur.setWidth("100%");
        fb.addRow(Common.getBahasa("label_kajur"), kajur);

        Tabpanel tabpanelDeskripsi = new ais.ui.util.MyTabpanel();
        tabpanelDeskripsi.setParent(tabpanels);
        tabpanelDeskripsi.appendChild(deskripsi = new MyCkEditor());
        deskripsi.setValue(grupJurusan.getDeskripsi());
        deskripsi.setHeight("100%");
        deskripsi.setWidth("100%");

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
        if (kode.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
            		"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Kode.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
            		"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaGrupJurusan()) {
            MyMessageboxConfig.show("Nama " + Common.getBahasa("label_grupJurusan") + " sudah ada di database",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkKodeGrupJurusan()) {
            MyMessageboxConfig.show("Kode " + Common.getBahasa("label_grupJurusan") + " sudah ada di database",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session mySession1 = HibernateUtil.currentNativeSession();
        GrupJurusan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (GrupJurusan) mySession1.load(GrupJurusan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setKajur((Dosen) kajur.getAttribute("dosen"));
        entity.setFakultas((Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
                ? null : fakultas.getSelectedItem().getValue()));
        entity.setDeskripsi(deskripsi.getValue());
        mySession1.getTransaction().begin();
        if (entity.getId() != null) {
            mySession1.update(entity);
        } else {
            mySession1.save(entity);
        }
        mySession1.getTransaction().commit();
        HibernateUtil.closeSession();
        return true;
    }

    public Boolean checkNamaGrupJurusan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(GrupJurusan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    public Boolean checkKodeGrupJurusan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(GrupJurusan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class GrupJurusanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final GrupJurusan grupJurusan = (GrupJurusan) arg1;

            new Label(grupJurusan.getKode() == null ? "" : grupJurusan.getKode()).setParent(arg0);
            new Label(grupJurusan.getNama()).setParent(arg0);
            new Label(grupJurusan.getKajur() == null ? "" : grupJurusan.getKajur().getNama()).setParent(arg0);
            new Label(grupJurusan.getFakultas() == null ? "" : grupJurusan.getFakultas().getNama()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, grupJurusan, GrupJurusanAction.this).setParent(arg0);
        }
    }
}
