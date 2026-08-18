package ais.action.master.sirs;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisBiaya;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class DokterAction extends GenericCrudAction<Dokter> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private MyTextbox searchkode;
    private Combobox searchkategori;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private Combobox kategori;
    private MyTextbox alamat;
    private Checkbox aktif;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Dokter> getEntityClass() { return Dokter.class; }

    @Override
    protected Dokter createNewEntity() { return new Dokter(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Tenaga Medis"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        for (String s : Dokter.KATEGOSRIES) {
            Comboitem comboitem = new Comboitem(s);
            comboitem.setValue(s);
            searchkategori.appendChild(comboitem);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Dokter.class)
                .add(searchkategori == null || searchkategori.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("kategori", searchkategori.getSelectedItem().getValue()))
                .add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new DokterRenderer();
    }

    // ======================== Form content ========================

    @SuppressWarnings("unchecked")
    @Override
    protected void buildFormContent(MyWindow window, final Dokter dokter) throws Exception {
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

        String mykode = dokter.getKode();
        if (mykode == null || mykode.trim().isEmpty()) {
            Long max = (Long) HibernateUtil.currentSession().createCriteria(Dokter.class)
                    .setProjection(Projections.max("id")).uniqueResult();
            if (max == null) max = 1L;
            mykode = "DR" + (++max);
        }
        kode = new MyTextbox(
                dokter.getKode() == null || mykode.trim().isEmpty() ? mykode : dokter.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Tenaga Medis", kode);

        nama = new MyTextbox(dokter.getNama() == null ? "" : dokter.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Tenaga Medis", nama);

        kategori = new Combobox();
        for (String s : Dokter.KATEGOSRIES) {
            Comboitem comboitem = new Comboitem(s);
            comboitem.setValue(s);
            kategori.appendChild(comboitem);
        }
        Common.selectComboItem(kategori, dokter.getKategori());
        kategori.setWidth("100%");
        fb.addRow("Kategori Tenaga Medis", kategori);

        alamat = new MyTextbox(dokter.getAlamat() == null ? "" : dokter.getAlamat());
        alamat.setWidth("100%");
        alamat.setRows(4);
        fb.addRow("Alamat", alamat);

        aktif = new Checkbox();
        aktif.setChecked(dokter.getAktif());
        fb.addRow("Aktif", aktif);

        keterangan = new MyTextbox(dokter.getKeterangan() == null ? "" : dokter.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        // Section: JenisBiaya checkboxes
        fb.addSectionHeader("JENIS BIAYA");

        // Jenis biaya yang SUDAH terpilih untuk dokter ini diambil lewat query LANGSUNG ke tabel
        // relasi (sirs.dokter_has_jenis_biaya) memakai session aktif — BUKAN via dokter.getJenisBiayas()
        // yang bersifat LAZY. Objek "dokter" berasal dari daftar/cache (detached) sehingga koleksi
        // lazy-nya tidak dapat di-inisialisasi lagi ("failed to lazily initialize a collection of role:
        // ...Dokter.jenisBiayas, no session or session was closed"). Query ke tabel relasi ini bebas
        // dari masalah tersebut (tanpa lazy-load, tanpa bergantung pada attachment entity).
        java.util.Set<Long> jenisBiayaTerpilih = new java.util.HashSet<Long>();
        if (dokter.getId() != null) {
            try {
                List<?> idTerpilih = HibernateUtil.currentSession()
                        .createSQLQuery("select jenis_biaya from sirs.dokter_has_jenis_biaya where dokter = :dokterId")
                        .setParameter("dokterId", dokter.getId())
                        .list();
                for (Object o : idTerpilih) {
                    if (o instanceof Number) {
                        jenisBiayaTerpilih.add(Long.valueOf(((Number) o).longValue()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<JenisBiaya> jenisBiayas = HibernateUtil.currentSession().createCriteria(JenisBiaya.class)
                .addOrder(Order.asc("nama")).list();
        for (JenisBiaya jenisBiaya : jenisBiayas) {
            Checkbox checkbox = new Checkbox(jenisBiaya.getNama() + " " + jenisBiaya.getVariable());
            if (jenisBiaya.getId() != null && jenisBiayaTerpilih.contains(jenisBiaya.getId())) {
                checkbox.setChecked(true);
            }
            fb.addFullRow(checkbox);
        }

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
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Dokter wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Dokter pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (kategori.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Kategori Dokter wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Kategori Dokter pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah kategori ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Dokter entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Dokter) session.load(Dokter.class, entity.getId());
            currentEntity = entity;
        }
        entity.setAktif(aktif.isChecked());
        entity.setAlamat(alamat.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setKategori((String) kategori.getSelectedItem().getValue());
        entity.setKode(kode.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    class DokterRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Dokter dokter = (Dokter) arg1;

            new Label(dokter.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Dokter.class, dokter, dokter.getNama()).setParent(arg0);
            new Label(dokter.getKategori()).setParent(arg0);
            new Label(dokter.getAlamat()).setParent(arg0);
            new Label(dokter.getAktif() ? "Ya" : "Tidak").setParent(arg0);
            new Label(dokter.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, dokter, DokterAction.this).setParent(arg0);
        }
    }
}
