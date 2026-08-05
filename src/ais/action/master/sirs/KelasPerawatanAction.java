package ais.action.master.sirs;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class KelasPerawatanAction extends GenericCrudAction<KelasPerawatan> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<KelasPerawatan> getEntityClass() { return KelasPerawatan.class; }

    @Override
    protected KelasPerawatan createNewEntity() { return new KelasPerawatan(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Kelas Perawatan"; }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KelasPerawatan.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KelasPerawatanRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final KelasPerawatan kelasPerawatan) throws Exception {
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

        nama = new Textbox(kelasPerawatan.getNama() == null ? "" : kelasPerawatan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Kelas Perawatan", nama);

        keterangan = new Textbox(kelasPerawatan.getKeterangan() == null ? "" : kelasPerawatan.getKeterangan());
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

    @SuppressWarnings("unchecked")
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Kelas Perawatan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Kelas Perawatan pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaKelasPerawatan()) {
            MyMessageboxConfig.show("Mohon maaf, data kelas perawatan dengan nama tersebut sudah terdaftar di dalam sistem. Langkah yang dapat dilakukan: (1) gunakan nama kelas perawatan yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        KelasPerawatan entity = currentEntity;
        boolean isNew = entity.getId() == null;
        if (!isNew) {
            entity = (KelasPerawatan) session.load(KelasPerawatan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (!isNew) {
            Common.refreshUpdate(session, entity);
        } else {
            session.save(entity);
            // Buat harga jual default untuk semua item medis
            List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(ItemMedis.class), ItemMedis.class);
            for (ItemMedis item : items) {
                HargaJualItem hargaJualItem = new HargaJualItem();
                hargaJualItem.setKelasPerawatan(entity);
                hargaJualItem.setHargaJual(item.getDefaultHargaJual() == null ? 0.0 : item.getDefaultHargaJual());
                hargaJualItem.setKeterangan(
                        "harga jual " + item.getNama() + " untuk kelas perawatan " + entity.getNama());
                session.save(hargaJualItem);
            }
        }
        return true;
    }

    public Boolean checkNamaKelasPerawatan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(KelasPerawatan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class KelasPerawatanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final KelasPerawatan kelasPerawatan = (KelasPerawatan) arg1;

            RevisiHelper.createNewRevisi(KelasPerawatan.class, kelasPerawatan, kelasPerawatan.getNama()).setParent(arg0);
            new Label(kelasPerawatan.getKeterangan()).setParent(arg0);

            // Custom delete: hapus harga_jual_item dulu sebelum delete kelas perawatan
            Hbox toolbar = new Hbox();
            MyToolbarbuttonConfig editBtn = new MyToolbarbuttonConfig("", "/img/edit.gif");
            editBtn.setTooltiptext("Rubah Data");
            editBtn.setVisible(edit);
            editBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    openForm(kelasPerawatan);
                }
            });
            editBtn.setParent(toolbar);

            MyToolbarbuttonConfig deleteBtn = new MyToolbarbuttonConfig("", "/img/delete.gif");
            deleteBtn.setTooltiptext("Hapus Data");
            deleteBtn.setVisible(delete);
            deleteBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data kelas perawatan ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            Session session = HibernateUtil.currentSession();
                                            session.createSQLQuery(
                                                    "delete from sirs.harga_jual_item where kelas_perawatan = "
                                                            + kelasPerawatan.getId())
                                                    .executeUpdate();
                                            Common.refreshDelete(session, kelasPerawatan);
                                            onSearchDefault(null);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Mohon maaf, data kelas perawatan ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
                                                    e.getMessage()));
                                        }
                                    }
                                }
                            });
                }
            });
            deleteBtn.setParent(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
