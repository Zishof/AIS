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
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KelasPerawatan;
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

public class HargaJualItemAction extends GenericCrudAction<HargaJualItem> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private AmbilDataItemBanbox item;
    private Combobox kelasPerawatan;
    private MyDoublebox hargaJual;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<HargaJualItem> getEntityClass() { return HargaJualItem.class; }

    @Override
    protected HargaJualItem createNewEntity() { return new HargaJualItem(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Harga Jual Barang Medis"; }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(HargaJualItem.class)
                .createAlias("item", "item")
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("item.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("item.nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new HargaJualItemRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final HargaJualItem hargaJualItem) throws Exception {
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
        item.setValue(hargaJualItem.getItem() == null ? "" : hargaJualItem.getItem().getNama());
        item.setAttribute("item", hargaJualItem.getItem());
        item.setWidth("100%");
        fb.addRow("Barang Medis", item);

        kelasPerawatan = new Combobox();
        Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class);
        Common.selectComboItem(kelasPerawatan, hargaJualItem.getKelasPerawatan());
        kelasPerawatan.setWidth("100%");
        fb.addRow("Kelas Perawatan", kelasPerawatan);

        hargaJual = new MyDoublebox(hargaJualItem.getHargaJual());
        hargaJual.setStyle("text-align:right;");
        hargaJual.setWidth("100%");
        fb.addRow("Harga Jual", hargaJual);

        keterangan = new MyTextbox(
                hargaJualItem.getKeterangan() == null ? "" : hargaJualItem.getKeterangan());
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
            MyMessageboxConfig.show("Mohon maaf, Nama Item wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Item; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Item ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (kelasPerawatan.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Kelas Perawatan wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Kelas Perawatan pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah kelas perawatan ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        HargaJualItem entity = currentEntity;
        if (entity.getId() != null) {
            entity = (HargaJualItem) session.load(HargaJualItem.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKelasPerawatan((KelasPerawatan) kelasPerawatan.getSelectedItem().getValue());
        entity.setItem((ItemMedis) item.getAttribute("item"));
        entity.setHargaJual(hargaJual.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    class HargaJualItemRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final HargaJualItem hargaJualItem = (HargaJualItem) arg1;

            RevisiHelper.createNewRevisi(HargaJualItem.class, hargaJualItem,
                    hargaJualItem.getItem() == null ? "" : hargaJualItem.getItem().getNama()).setParent(arg0);
            new Label(hargaJualItem.getKelasPerawatan() == null ? ""
                    : hargaJualItem.getKelasPerawatan().getNama()).setParent(arg0);
            new Label(hargaJualItem.getHargaJual() == null ? "0"
                    : Common.numberFormat.get().format(hargaJualItem.getHargaJual())).setParent(arg0);
            new Label(hargaJualItem.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, hargaJualItem, HargaJualItemAction.this).setParent(arg0);
        }
    }
}
