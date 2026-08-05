package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class SatuanItemAction extends GenericCrudAction<SatuanItem> {

    private static final long serialVersionUID = 3786091220301468178L;

    // Form fields
    private Textbox namaAwal;
    private Textbox nama;
    private Intbox jumlah;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<SatuanItem> getEntityClass() { return SatuanItem.class; }

    @Override
    protected SatuanItem createNewEntity() { return new SatuanItem(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Satuan Item"; }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(SatuanItem.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new SatuanItemRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final SatuanItem satuanItem) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card + header
        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        // Plain Grid
        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        namaAwal = new Textbox(satuanItem.getNamaAwal() == null ? "" : satuanItem.getNamaAwal());
        namaAwal.setWidth("100%");
        fb.addRow("Nama Satuan Mulai", namaAwal);

        nama = new Textbox(satuanItem.getNama() == null ? "" : satuanItem.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Satuan Sampai", nama);

        jumlah = new Intbox(satuanItem.getJumlah() == null ? 1 : satuanItem.getJumlah());
        jumlah.setWidth("100%");
        fb.addRow("Jumlah Item per Satuan", jumlah);

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
            MyMessageboxConfig.show("Mohon maaf, Nama Satuan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Satuan pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jumlah.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jumlah Item per Satuan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan angka jumlah item per satuan pada kolom yang tersedia; (2) pastikan nilai berupa angka positif; (3) simpan kembali data setelah nilai terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaJenisBarang()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Satuan yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama satuan yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        SatuanItem entity = currentEntity;
        if (entity.getId() != null) {
            entity = (SatuanItem) session.load(SatuanItem.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNamaAwal(namaAwal.getValue().trim());
        entity.setNama(nama.getValue().trim());
        entity.setJumlah(jumlah.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaJenisBarang() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(SatuanItem.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class SatuanItemRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final SatuanItem satuanItem = (SatuanItem) arg1;

            new Label(satuanItem.getNamaAwal()).setParent(arg0);
            RevisiHelper.createNewRevisi(SatuanItem.class, satuanItem, satuanItem.getNama()).setParent(arg0);
            new Label(satuanItem.getJumlah() == null ? "0"
                    : Common.numberFormat.get().format(satuanItem.getJumlah())).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, satuanItem, SatuanItemAction.this).setParent(arg0);
        }
    }
}
