package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.DiskonDetailAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.Komunitas;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class DiskonAction extends GenericCrudAction<Diskon> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private Combobox searchAsuransi;
    private Combobox searchKomunitas;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private MyDoublebox jumlah;
    private Intbox jumlahMinimal;
    private Intbox jumlahMaksimal;
    private AmbilDataAkunKreditBanbox akun;
    private Combobox asuransi;
    private Combobox komunitas;
    private Checkbox aktif;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Diskon> getEntityClass() { return Diskon.class; }

    @Override
    protected Diskon createNewEntity() { return new Diskon(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Diskon"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.insertCombo(searchAsuransi, "nama", Asuransi.class);
        Common.insertCombo(searchKomunitas, "nama", Komunitas.class);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Diskon.class)
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchAsuransi == null || searchAsuransi.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("asuransi", searchAsuransi.getSelectedItem().getValue()))
                .add(searchKomunitas == null || searchKomunitas.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("komunitas", searchKomunitas.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new DiskonRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Diskon diskon) throws Exception {
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

        kode = new MyTextbox(
                diskon.getKode() == null ? Common.generateCode(Diskon.class, 8) : diskon.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Diskon", kode);

        nama = new MyTextbox(diskon.getNama() == null ? "" : diskon.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Diskon", nama);

        mulai = new MyDatebox(diskon.getMulai());
        mulai.setWidth("100%");
        fb.addRow("Diskon berlaku mulai", mulai);

        sampai = new MyDatebox(diskon.getSampai());
        sampai.setWidth("100%");
        fb.addRow("Diskon berlaku sampai", sampai);

        jumlah = new MyDoublebox(diskon.getJumlah());
        jumlah.setWidth("100%");
        fb.addRow("Nilai Diskon (%)", jumlah);

        jumlahMinimal = new Intbox(diskon.getJumlahMinimal());
        jumlahMinimal.setWidth("100%");
        fb.addRow("Jumlah minimal mendapatkan diskon", jumlahMinimal);

        jumlahMaksimal = new Intbox(diskon.getJumlahMaksimal());
        jumlahMaksimal.setWidth("100%");
        fb.addRow("Jumlah maksimal mendapatkan diskon", jumlahMaksimal);

        akun = new AmbilDataAkunKreditBanbox();
        akun.setValue(diskon.getAkun() == null ? "" : diskon.getAkun().toString());
        akun.setAttribute("akun", diskon.getAkun());
        akun.setWidth("100%");
        fb.addRow("Diskon ini akan masuk ke akun", akun);

        asuransi = new Combobox();
        Common.insertCombo(asuransi, "nama", Asuransi.class);
        Common.selectComboItem(asuransi, diskon.getAsuransi());
        asuransi.setWidth("100%");
        fb.addRow("Berlaku untuk asuransi / kelompok pasien", asuransi);

        komunitas = new Combobox();
        Common.insertCombo(komunitas, "nama", Komunitas.class);
        Common.selectComboItem(komunitas, diskon.getKomunitas());
        komunitas.setWidth("100%");
        fb.addRow("Berlaku untuk komunitas pasien", komunitas);

        aktif = new Checkbox();
        aktif.setChecked(diskon.getAktif());
        fb.addRow("Aktif (berlaku)", aktif);

        keterangan = new MyTextbox(diskon.getKeterangan() == null ? "" : diskon.getKeterangan());
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
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show(
                    "Mohon maaf, Nama Diskon belum diisi. Mohon Bapak/Ibu mengisi terlebih dahulu nama diskon sebelum menyimpan data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (mulai.getValue() == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, tanggal mulai berlakunya Diskon belum diisi. Mohon Bapak/Ibu menentukan terlebih dahulu tanggal mulai berlaku sebelum menyimpan data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (akun.getAttribute("akun") == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, kolom Akun belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu akun tujuan diskon sebelum menyimpan data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Diskon entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Diskon) session.load(Diskon.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKomunitas(komunitas.getSelectedItem() == null ? null : (Komunitas) komunitas.getSelectedItem().getValue());
        entity.setAktif(aktif.isChecked());
        entity.setAsuransi(asuransi.getSelectedItem() == null ? null : (Asuransi) asuransi.getSelectedItem().getValue());
        entity.setJumlahMaksimal(jumlahMaksimal.getValue());
        entity.setAkun((Akun) akun.getAttribute("akun"));
        entity.setJumlahMinimal(jumlahMinimal.getValue());
        entity.setJumlah(jumlah.getValue());
        entity.setMulai(mulai.getValue());
        entity.setSampai(sampai.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (entity.getId() != null) {
            Common.refreshUpdate(session, entity);
        } else {
            entity.setKode(Common.generateCode(Diskon.class, 8));
            session.save(entity);
        }
        return true;
    }

    // ======================== Renderer ========================

    class DiskonRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Diskon diskon = (Diskon) arg1;

            new DiskonDetailAction(diskon).setParent(arg0);
            RevisiHelper.createNewRevisi(Diskon.class, diskon, diskon.getNama()).setParent(arg0);
            new Label(diskon.getMulai() == null ? "" : Common.dateFormat3.get().format(diskon.getMulai())).setParent(arg0);
            new Label(diskon.getSampai() == null ? "" : Common.dateFormat3.get().format(diskon.getSampai())).setParent(arg0);
            new Label(Common.numberFormat.get().format(diskon.getJumlah())).setParent(arg0);
            new Label(diskon.getJumlahMinimal().toString()).setParent(arg0);
            new Label(diskon.getJumlahMaksimal().toString()).setParent(arg0);
            new Label(diskon.getAkun().toString()).setParent(arg0);
            new Label(diskon.getAsuransi() == null ? "" : diskon.getAsuransi().toString()).setParent(arg0);
            new Label(diskon.getKomunitas() == null ? "" : diskon.getKomunitas().toString()).setParent(arg0);
            new Label(diskon.getAktif() ? "Ya" : "Tidak").setParent(arg0);
            new Label(diskon.getKeterangan()).setParent(arg0);

            org.zkoss.zul.Hbox toolbar = new org.zkoss.zul.Hbox();

            Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            btnEdit.setTooltiptext("Rubah Data");
            btnEdit.setVisible(edit);
            btnEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    currentEntity = diskon;
                    buildFormContent(addWindow, diskon);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            btnEdit.setParent(toolbar);

            Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");
            btnDelete.setTooltiptext("Hapus Data");
            btnDelete.setVisible(delete);
            btnDelete.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.",
                            "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                            MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            Session session = HibernateUtil.currentSession();
                                            session.createSQLQuery(
                                                    "delete from sirs.diskon_detail where diskon = " + diskon.getId())
                                                    .executeUpdate();
                                            Common.refreshDelete(session, diskon);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
                                                    e.getMessage()));
                                        }
                                    }
                                }
                            });
                }
            });
            btnDelete.setParent(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
