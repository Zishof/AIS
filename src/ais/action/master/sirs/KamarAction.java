package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD data master "Kamar" SIRS: mendata kamar rawat inap beserta kelas perawatan dan ruang
 * (bangunan/gedung) tempatnya berada. Dibangun di atas kerangka kerja {@link GenericCrudAction}:
 * pencarian dapat difilter berdasarkan kelas perawatan, ruang, dan nama kamar; form tambah/edit
 * memuat nama, kelas perawatan, ruang, dan keterangan; renderer baris daftar menampilkan seluruh
 * atribut tersebut dengan tombol revisi dan aksi edit/hapus.
 */
public class KamarAction extends GenericCrudAction<Kamar> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private Combobox searchkelas;
    private Combobox searchruang;

    // Form fields
    private MyTextbox nama;
    private Combobox kelasPerawatan;
    private Combobox ruang;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Kamar> getEntityClass() { return Kamar.class; }

    @Override
    protected Kamar createNewEntity() { return new Kamar(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Kamar"; }

    /** Mengisi dropdown pencarian kelas perawatan dan ruang setelah komponen ZK selesai dirakit. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.insertCombo(searchkelas, "nama", "keterangan", KelasPerawatan.class);
        Common.insertCombo(searchruang, "nama", "keterangan", Ruang.class);
    }

    /** Membangun kriteria pencarian {@link Kamar} berdasarkan filter kelas perawatan, ruang, dan nama (ILIKE sebagian), diurutkan menurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Kamar.class)
                .add(searchkelas == null || searchkelas.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue()))
                .add(searchruang == null || searchruang.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("ruang", searchruang.getSelectedItem().getValue()))
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KamarRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/edit Kamar (field nama, kelas perawatan, ruang, keterangan) beserta toolbar Batal/Simpan di dalam jendela {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final Kamar kamar) throws Exception {
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

        nama = new MyTextbox(kamar.getNama() == null ? "" : kamar.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Kamar", nama);

        kelasPerawatan = new Combobox();
        Common.insertCombo(kelasPerawatan, "nama", "keterangan", KelasPerawatan.class);
        Common.selectComboItem(kelasPerawatan, kamar.getKelasPerawatan());
        kelasPerawatan.setWidth("100%");
        fb.addRow("Kelas", kelasPerawatan);

        ruang = new Combobox();
        Common.insertCombo(ruang, "nama", "keterangan", Ruang.class);
        Common.selectComboItem(ruang, kamar.getRuang());
        ruang.setWidth("100%");
        fb.addRow("Ruang", ruang);

        keterangan = new MyTextbox(kamar.getKeterangan() == null ? "" : kamar.getKeterangan());
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

    /** Memvalidasi (nama wajib diisi) lalu menyimpan/memperbarui entitas {@link Kamar} dari nilai form (nama, kelas perawatan, ruang, keterangan). @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Kamar wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Kamar pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Kamar entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Kamar) session.load(Kamar.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKelasPerawatan(kelasPerawatan.getSelectedItem() == null
                ? null : (KelasPerawatan) kelasPerawatan.getSelectedItem().getValue());
        entity.setRuang(ruang.getSelectedItem() == null
                ? null : (Ruang) ruang.getSelectedItem().getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    /** Merender satu baris daftar Kamar: label revisi+nama, kelas perawatan, ruang, keterangan, dan tombol edit/hapus. */
    class KamarRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Kamar kamar = (Kamar) arg1;

            RevisiHelper.createNewRevisi(Kamar.class, kamar, kamar.getNama()).setParent(arg0);
            new Label(kamar.getKelasPerawatan() == null ? "" : kamar.getKelasPerawatan().getNama()).setParent(arg0);
            new Label(kamar.getRuang() == null ? "" : kamar.getRuang().getNama()).setParent(arg0);
            new Label(kamar.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, kamar, KamarAction.this).setParent(arg0);
        }
    }
}
