package ais.action.master.sirs;

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
import org.zkoss.zul.Timebox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.Shift;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class ShiftAction extends GenericCrudAction<Shift> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private Combobox searchLokasi;
    private Combobox searchJenis;

    // Form fields
    private MyTextbox nama;
    private Combobox lokasi;
    private Timebox mulai;
    private Timebox sampai;
    private Combobox jenisShift;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Shift> getEntityClass() { return Shift.class; }

    @Override
    protected Shift createNewEntity() { return new Shift(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Shift"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.insertCombo(searchLokasi, "nama", "keterangan", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.insertCombo(searchJenis, "nama", "akun", JenisBiayaLain.class,
                Restrictions.eq("jenis", JenisBiayaLain.SETOR_TRANSAKSI_PENJUALAN));
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Shift.class)
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchLokasi == null || searchLokasi.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("lokasi", searchLokasi.getSelectedItem().getValue()))
                .add(searchJenis == null || searchJenis.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenisShift", searchJenis.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new ShiftRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Shift shift) throws Exception {
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

        nama = new MyTextbox(shift.getNama() == null ? "" : shift.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Shift", nama);

        lokasi = new Combobox();
        Common.insertCombo(lokasi, "nama", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(lokasi, shift.getLokasi());
        lokasi.setWidth("100%");
        fb.addRow("Lokasi", lokasi);

        mulai = new MyTimebox(shift.getMulai());
        mulai.setFormat(Common.timeFormat.get().toPattern());
        fb.addRow("Waktu Mulai", mulai);

        sampai = new MyTimebox(shift.getSampai());
        sampai.setFormat(Common.timeFormat.get().toPattern());
        fb.addRow("Waktu Sampai", sampai);

        jenisShift = new Combobox();
        Common.insertCombo(jenisShift, "nama", "akun", JenisBiayaLain.class,
                Restrictions.eq("jenis", JenisBiayaLain.SETOR_TRANSAKSI_PENJUALAN));
        Common.selectComboItem(jenisShift, shift.getJenisShift());
        jenisShift.setWidth("100%");
        fb.addRow("Jenis Shift", jenisShift);

        keterangan = new MyTextbox(shift.getKeterangan() == null ? "" : shift.getKeterangan());
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

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Shift wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Shift pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (lokasi.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Lokasi pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Lokasi ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (mulai.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Waktu Mulai wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isi Waktu Mulai pada kolom yang tersedia; (2) pastikan format waktu sudah benar; (3) simpan kembali data setelah waktu mulai terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (sampai.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Waktu Sampai (Selesai) wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isi Waktu Sampai pada kolom yang tersedia; (2) pastikan waktu sampai lebih besar dari waktu mulai; (3) simpan kembali data setelah waktu sampai terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisShift.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Shift wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Shift pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah jenis shift ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaShift()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Shift yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama shift yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Shift entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Shift) session.load(Shift.class, entity.getId());
            currentEntity = entity;
        }
        entity.setJenisShift((JenisBiayaLain) jenisShift.getSelectedItem().getValue());
        entity.setMulai(mulai.getValue());
        entity.setSampai(sampai.getValue());
        entity.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaShift() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Shift.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class ShiftRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Shift shift = (Shift) arg1;

            RevisiHelper.createNewRevisi(Shift.class, shift, shift.getNama()).setParent(arg0);
            new Label(shift.getMulai() == null ? "" : Common.timeFormat.get().format(shift.getMulai())).setParent(arg0);
            new Label(shift.getSampai() == null ? "" : Common.timeFormat.get().format(shift.getSampai())).setParent(arg0);
            new Label(shift.getLokasi() == null ? "" : shift.getLokasi().getNama()).setParent(arg0);
            new Label(shift.getJenisShift() == null ? "" : shift.getJenisShift().toString()).setParent(arg0);
            new Label(shift.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, shift, ShiftAction.this).setParent(arg0);
        }
    }
}
