package ais.action.master.sirs;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.StatusTempatTidur;
import ais.database.model.sirs.TempatTidur;
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
import ais.action.master.helper.FilterLanjutHelper;

public class TempatTidurAction extends GenericCrudAction<TempatTidur> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private Combobox searchkelas;
    private Combobox searchruang;
    private Combobox searchkamar;
    private Combobox searchstatus;
    private Combobox searchstatusTempatTidur;

    // Form fields
    private MyTextbox nama;
    private Combobox kelasPerawatan;
    private Combobox ruang;
    private Combobox kamar;
    private Combobox statusTempatTidur;
    private Checkbox terisi;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<TempatTidur> getEntityClass() { return TempatTidur.class; }

    @Override
    protected TempatTidur createNewEntity() { return new TempatTidur(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Tempat Tidur"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Common.insertCombo(searchkelas, "nama", "keterangan", KelasPerawatan.class);
        Common.insertCombo(searchruang, "nama", "keterangan", Ruang.class);
        Common.insertCombo(searchstatusTempatTidur, "nama", "keterangan", StatusTempatTidur.class);

        Comboitem comboitem = new Comboitem("Terisi");
        if (comboitem != null) { comboitem.setValue(true); }
        searchstatus.appendChild(comboitem);
        comboitem = new Comboitem("Kosong");
        if (comboitem != null) { comboitem.setValue(false); }
        searchstatus.appendChild(comboitem);

        final EventListener searchKamarListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Common.clear(searchkamar);
                Common.insertCombo(searchkamar, "nama", "keterangan", Kamar.class,
                        Restrictions.and(
                                searchruang == null || searchruang.getSelectedItem() == null
                                        ? Restrictions.sqlRestriction("true")
                                        : Restrictions.eq("ruang", searchruang.getSelectedItem().getValue()),
                                searchkelas == null || searchkelas.getSelectedItem() == null
                                        ? Restrictions.sqlRestriction("true")
                                        : Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue())));
            }
        };
        searchkelas.addEventListener("onChange", searchKamarListener);
        searchruang.addEventListener("onChange", searchKamarListener);
        searchKamarListener.onEvent(null);
            FilterLanjutHelper.setup(comp);
}

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(TempatTidur.class)
                .add(searchstatusTempatTidur == null || searchstatusTempatTidur.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("statusTempatTidur", searchstatusTempatTidur.getSelectedItem().getValue()))
                .add(searchruang == null || searchruang.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("ruang", searchruang.getSelectedItem().getValue()))
                .add(searchkelas == null || searchkelas.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue()))
                .add(searchkamar == null || searchkamar.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("kamar", searchkamar.getSelectedItem().getValue()))
                .add(searchstatus == null || searchstatus.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("terisi", searchstatus.getSelectedItem().getValue()))
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new TempatTidurRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final TempatTidur tempatTidur) throws Exception {
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

        nama = new MyTextbox(tempatTidur.getNama() == null ? "" : tempatTidur.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Tempat Tidur", nama);

        kelasPerawatan = new Combobox();
        Common.insertCombo(kelasPerawatan, "nama", "keterangan", KelasPerawatan.class);
        Common.selectComboItem(kelasPerawatan, tempatTidur.getKelasPerawatan());
        kelasPerawatan.setWidth("100%");
        fb.addRow("Kelas", kelasPerawatan);

        ruang = new Combobox();
        Common.insertCombo(ruang, "nama", "keterangan", Ruang.class);
        Common.selectComboItem(ruang, tempatTidur.getRuang());
        ruang.setWidth("100%");
        fb.addRow("Ruang", ruang);

        kamar = new Combobox();
        kamar.setWidth("100%");
        fb.addRow("Kamar", kamar);

        final EventListener kamarListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Common.clear(kamar);
                Common.insertCombo(kamar, "nama", "keterangan", Kamar.class, Restrictions.and(
                        ruang.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
                                : Restrictions.eq("ruang", ruang.getSelectedItem().getValue()),
                        kelasPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
                                : Restrictions.eq("kelasPerawatan", kelasPerawatan.getSelectedItem().getValue())));
                Common.selectComboItem(kamar, tempatTidur.getKamar());
            }
        };
        kelasPerawatan.addEventListener("onChange", kamarListener);
        ruang.addEventListener("onChange", kamarListener);
        kamarListener.onEvent(null);

        statusTempatTidur = new Combobox();
        Common.insertCombo(statusTempatTidur, "nama", "keterangan", StatusTempatTidur.class);
        Common.selectComboItem(statusTempatTidur, tempatTidur.getStatusTempatTidur());
        statusTempatTidur.setWidth("100%");
        fb.addRow("Status", statusTempatTidur);

        terisi = new Checkbox();
        terisi.setChecked(tempatTidur.getTerisi() != null && tempatTidur.getTerisi());
        fb.addRow("Apakah terisi pasien", terisi);

        keterangan = new MyTextbox(tempatTidur.getKeterangan() == null ? "" : tempatTidur.getKeterangan());
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
            MyMessageboxConfig.show("Mohon maaf, Nama Tempat Tidur wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Tempat Tidur pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (statusTempatTidur.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Status Tempat Tidur wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Status Tempat Tidur pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah status ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        TempatTidur entity = currentEntity;
        if (entity.getId() != null) {
            entity = (TempatTidur) session.load(TempatTidur.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setTerisi(terisi.isChecked());
        entity.setStatusTempatTidur((StatusTempatTidur) statusTempatTidur.getSelectedItem().getValue());
        entity.setRuang(ruang.getSelectedItem() == null ? null : (Ruang) ruang.getSelectedItem().getValue());
        entity.setKamar(kamar.getSelectedItem() == null ? null : (Kamar) kamar.getSelectedItem().getValue());
        entity.setKelasPerawatan(kelasPerawatan.getSelectedItem() == null
                ? null : (KelasPerawatan) kelasPerawatan.getSelectedItem().getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    @SuppressWarnings("unchecked")
    class TempatTidurRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final TempatTidur tempatTidur = (TempatTidur) arg1;

            tempatTidur.updateTerisi();

            RevisiHelper.createNewRevisi(TempatTidur.class, tempatTidur, tempatTidur.getNama()).setParent(arg0);
            new Label(tempatTidur.getKelasPerawatan() == null ? "" : tempatTidur.getKelasPerawatan().getNama()).setParent(arg0);
            new Label(tempatTidur.getRuang() == null ? "" : tempatTidur.getRuang().getNama()).setParent(arg0);
            new Label(tempatTidur.getKamar() == null ? "" : tempatTidur.getKamar().getNama()).setParent(arg0);
            new Label(tempatTidur.getTerisi() == null || !tempatTidur.getTerisi() ? "Tidak" : "Ya").setParent(arg0);

            if (tempatTidur.getTerisi() != null && tempatTidur.getTerisi()) {
                ProjectionList projectionList = Projections.projectionList();
                projectionList.add(Projections.property("pasien"));
                projectionList.add(Projections.property("kode"));
                projectionList.add(Projections.property("tanggalPendaftaran"));

                Object[] pendaftaran = (Object[]) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
                        .setProjection(projectionList)
                        .add(Restrictions.eq("tempatTidur", tempatTidur))
                        .addOrder(Order.desc("id"))
                        .setMaxResults(1)
                        .uniqueResult();

                if (pendaftaran != null) {
                    Pasien pasien = (Pasien) (pendaftaran.length < 1 ? null : pendaftaran[0]);
                    String kode = (String) (pendaftaran.length < 2 ? null : pendaftaran[1]);
                    Date tanggalPendaftaran = (Date) (pendaftaran.length < 3 ? null : pendaftaran[2]);
                    new Html(pasien == null ? ""
                            : pasien.getKode() + " - " + pasien.getNama()
                            + "<br><b>No. Reg </b>: " + kode
                            + "<br><b>Wkt. Reg </b>: "
                            + (tanggalPendaftaran == null ? "" : Common.dateFormat3.get().format(tanggalPendaftaran)))
                            .setParent(arg0);
                } else {
                    new Label(ais.common.Common.getBahasaConfig("Tidak ada keterangan")).setParent(arg0);
                }
            } else {
                new Label("").setParent(arg0);
            }

            new Label(tempatTidur.getStatusTempatTidur() == null ? "" : tempatTidur.getStatusTempatTidur().getNama()).setParent(arg0);
            new Label(tempatTidur.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, tempatTidur, TempatTidurAction.this).setParent(arg0);
        }
    }
}
