package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataItemBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KonversiSatuanItem;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class KonversiSatuanItemAction extends GenericCrudAction<KonversiSatuanItem> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private Combobox searchsatuanDari;
    private Combobox searchsatuanMenjadi;

    // Form fields
    private AmbilDataItemBanbox item;
    private Combobox satuanDari;
    private Combobox satuanMenjadi;
    private MyDoublebox nilaiPersamaan;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<KonversiSatuanItem> getEntityClass() { return KonversiSatuanItem.class; }

    @Override
    protected KonversiSatuanItem createNewEntity() { return new KonversiSatuanItem(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Konversi Satuan Item"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.insertCombo(searchsatuanDari, "nama", SatuanItem.class);
        Common.insertCombo(searchsatuanMenjadi, "nama", SatuanItem.class);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        SatuanItem dari = searchsatuanDari == null || searchsatuanDari.getSelectedItem() == null
                ? null : (SatuanItem) searchsatuanDari.getSelectedItem().getValue();
        SatuanItem menjadi = searchsatuanMenjadi == null || searchsatuanMenjadi.getSelectedItem() == null
                ? null : (SatuanItem) searchsatuanMenjadi.getSelectedItem().getValue();

        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KonversiSatuanItem.class)
                .createAlias("item", "item")
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("item.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(dari == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("satuanDari", dari))
                .add(menjadi == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("satuanMenjadi", menjadi));
        if (order) criteria.addOrder(Order.asc("item.nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KonversiSatuanItemRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final KonversiSatuanItem konversiSatuanItem) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card
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

        item = new AmbilDataItemBanbox();
        item.setAttribute("item", konversiSatuanItem.getItem());
        item.setValue(konversiSatuanItem.getItem() == null ? ""
                : konversiSatuanItem.getItem().getKode() + " - " + konversiSatuanItem.getItem().getNama());
        item.setWidth("100%");
        fb.addRow("Item", item);

        satuanDari = new Combobox();
        Common.insertCombo(satuanDari, "nama", SatuanItem.class);
        Common.selectComboItem(satuanDari, konversiSatuanItem.getSatuanDari());
        satuanDari.setWidth("100%");
        fb.addRow("Satuan Dari", satuanDari);

        satuanMenjadi = new Combobox();
        Common.insertCombo(satuanMenjadi, "nama", SatuanItem.class);
        Common.selectComboItem(satuanMenjadi, konversiSatuanItem.getSatuanMenjadi());
        satuanMenjadi.setWidth("100%");
        fb.addRow("Satuan Menjadi", satuanMenjadi);

        nilaiPersamaan = new MyDoublebox(konversiSatuanItem.getNilaiPersamaan());
        nilaiPersamaan.setWidth("100%");
        fb.addRow("Nilai Persamaan", nilaiPersamaan);

        keterangan = new MyTextbox(
                konversiSatuanItem.getKeterangan() == null ? "" : konversiSatuanItem.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        // South
        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);

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
        if (item.getAttribute("item") == null) {
            MyMessageboxConfig.show("Mohon maaf, Item Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Item Medis; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Item Medis ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (satuanDari.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Satuan Asal (Satuan Dari) wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Satuan Dari pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah satuan ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (satuanMenjadi.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Satuan Tujuan (Satuan Menjadi) wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Satuan Menjadi pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah satuan tujuan ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nilaiPersamaan.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Nilai Persamaan konversi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan nilai persamaan antara dua satuan; (2) pastikan nilai berupa angka positif; (3) simpan kembali data setelah nilai terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        KonversiSatuanItem entity = currentEntity;
        if (entity.getId() != null) {
            entity = (KonversiSatuanItem) session.load(KonversiSatuanItem.class, entity.getId());
            currentEntity = entity;
        }
        entity.setItem((ItemMedis) item.getAttribute("item"));
        entity.setNilaiPersamaan(nilaiPersamaan.getValue());
        entity.setSatuanDari((SatuanItem) satuanDari.getSelectedItem().getValue());
        entity.setSatuanMenjadi((SatuanItem) satuanMenjadi.getSelectedItem().getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    class KonversiSatuanItemRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final KonversiSatuanItem konversiSatuanItem = (KonversiSatuanItem) arg1;

            RevisiHelper.createNewRevisi(KonversiSatuanItem.class, konversiSatuanItem,
                    konversiSatuanItem.getItem().getNama()).setParent(arg0);
            new Label(konversiSatuanItem.getSatuanDari().getNama()).setParent(arg0);
            new Label(konversiSatuanItem.getSatuanMenjadi().getNama()).setParent(arg0);
            new Label(konversiSatuanItem.getNilaiPersamaan() == null ? ""
                    : Common.numberFormat.get().format(konversiSatuanItem.getNilaiPersamaan())).setParent(arg0);
            new Label(konversiSatuanItem.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, konversiSatuanItem, KonversiSatuanItemAction.this).setParent(arg0);
        }
    }
}
