package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BerkasHasilAkreditasiPunyaNama;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.Jabatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Staff;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class FakultasAction extends GenericCrudAction<Fakultas> {

    private static final long serialVersionUID = 3786091220301468178L;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox nama;
    private Textbox kode;
    private AmbilDataDosenBanbox dekan;
    private AmbilDataDosenBanbox pudek1;
    private AmbilDataDosenBanbox pudek2;
    private AmbilDataDosenBanbox pudek3;
    private Combobox perguruanTinggi;
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private MyCkEditor deskripsi;
    private Textbox warna;
    protected LampiranLain kop;
    protected LampiranLain foot;
    private Textbox namaEn;
    private Textbox labelPejabat1;
    private AmbilDataPegawaiBanbox pegawai1;
    private Textbox labelPejabat2;
    private AmbilDataPegawaiBanbox pegawai2;
    private Textbox labelPejabat3;
    private AmbilDataPegawaiBanbox pegawai3;
    private Textbox wa;
    private MyCheckboxConfig dosenHarusPakaiSatuanKerja;
    protected LampiranLain kopStempel;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Fakultas> getEntityClass() { return Fakultas.class; }

    @Override
    protected Fakultas createNewEntity() { return new Fakultas(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Fakultas"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "kode", "nama", "namaEn", "dekan", "perguruanTinggi", "deskripsi",
                "labelPejabat1", "labelPejabat2", "labelPejabat3", "pegawai1", "pegawai2", "pegawai3" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Fakultas.class, this,
                "kode", "nama", "namaEn", "dekan", "perguruanTinggi", "deskripsi",
                "labelPejabat1", "labelPejabat2", "labelPejabat3", "pegawai1", "pegawai2", "pegawai3");
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        boolean dspaceAktif = Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF);

        MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor Berkas", "/img/corner.gif");
        exportKeOjs.setVisible(dspaceAktif);
        exportKeOjs.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                final Label label = Common.displayLoadBar(new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        onSearchDefault(arg0);
                    }
                });
                new Thread(new Runnable() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void run() {
                        try {
                            String cookie = DspaceCommon.login();
                            Session sess = HibernateUtil.currentSession();
                            Criteria criteria = sess.createCriteria(BerkasHasilAkreditasiPunyaNama.class)
                                    .createCriteria("berkasHasilAkreditasi").createCriteria("fakultas")
                                    .addOrder(Order.asc("nama"))
                                    .add(searchnama.getValue().trim().isEmpty()
                                            ? Restrictions.sqlRestriction("true")
                                            : Restrictions.ilike("nama", searchnama.getValue().trim(),
                                                    MatchMode.ANYWHERE));
                            List<BerkasHasilAkreditasiPunyaNama> list = criteria.list();
                            int rowIndex = 1;
                            for (BerkasHasilAkreditasiPunyaNama item : list) {
                                label.setValue("Sedang memproses data " + item.toString() + " ("
                                        + Common.numberFormat.get().format(
                                                (rowIndex++) * 100.0 / list.size()) + " %)");
                                JurusanAction.getDspace(cookie, item, true);
                            }
                        } catch (Exception e) {
                            Common.tampilErrorJikaAdmin(e);
                        }
                        label.setValue("");
                    }
                }).start();
            }
        });
        if (add != null) {
        add.getParent().appendChild(exportKeOjs);
        }

        MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor Berkas",
                "/img/svg/trash.svg");
        batalExport.setVisible(dspaceAktif);
        batalExport.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
                        MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
                        new EventListener() {
                            @Override
                            public void onEvent(Event event) throws Exception {
                                int i = Integer.parseInt(event.getData().toString());
                                if (i == MyMessageboxConfig.OK) {
                                    final Label label = Common.displayLoadBar(new EventListener() {
                                        @Override
                                        public void onEvent(Event arg0) throws Exception {
                                            onSearchDefault(arg0);
                                            LogLoginAction.tampilDpsaceLog();
                                        }
                                    });
                                    new Thread(new Runnable() {
                                        @SuppressWarnings("unchecked")
                                        @Override
                                        public void run() {
                                        	try {
                                            try {
                                                String cookie = DspaceCommon.login();
                                                Session sess = HibernateUtil.currentSession();
                                                Criteria criteria = sess
                                                        .createCriteria(BerkasHasilAkreditasiPunyaNama.class)
                                                        .createCriteria("berkasHasilAkreditasi")
                                                        .createCriteria("fakultas")
                                                        .addOrder(Order.asc("nama"))
                                                        .add(Restrictions.ilike("nama",
                                                                searchnama.getValue(), MatchMode.ANYWHERE));
                                                List<BerkasHasilAkreditasiPunyaNama> list = criteria.list();
                                                int rowIndex = 1;
                                                for (BerkasHasilAkreditasiPunyaNama item : list) {
                                                    label.setValue("Sedang memproses data "
                                                            + item.toString() + " ("
                                                            + Common.numberFormat.get().format(
                                                                    (rowIndex++) * 100.0 / list.size())
                                                            + " %)");
                                                    DspaceInformation info = DspaceInformation
                                                            .getDspaceInformation(
                                                                    BerkasHasilAkreditasiPunyaNama.class.getName(),
                                                                    item.getId());
                                                    if (info != null) {
                                                        int result = DspaceInformation.delete(cookie,
                                                                "items/" + info.getUuid(),
                                                                info.getPostInfo());
                                                        if (result == 200) {
                                                            sess = HibernateUtil.currentNativeSession();
                                                            sess.getTransaction().begin();
                                                            sess.delete(info);
                                                            sess.getTransaction().commit();
                                                            HibernateUtil.closeSession();
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Common.tampilErrorJikaAdmin(e);
                                            }
                                            label.setValue("");
                                                                                	} finally {
                                        		ais.database.hibernate.HibernateUtil.closeSession();
                                        	}
                                        }
                                    }).start();
                                }
                            }
                        });
            }
        });
        if (add != null) {
        add.getParent().appendChild(batalExport);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        PerguruanTinggi selectedPT = PerguruanTinggiUtil.getPerguruanTinggi();
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Fakultas.class)
                .add(selectedPT != null && selectedPT.getId() != null
                        ? Restrictions.or(Restrictions.isNull("perguruanTinggi"),
                                Restrictions.eq("perguruanTinggi", selectedPT))
                        : Restrictions.sqlRestriction("true"))
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("kode"));
        criteria.add(searchnama == null || searchnama.getValue().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new FakultasRenderer();
    }

    // ======================== Static DSpace helper (dipanggil oleh JurusanAction) ========================

    public static DspaceInformation getDspace(String cookie, Fakultas fakultas, boolean update) throws Exception {
        JSONObject jsonPost = new JSONObject();
        jsonPost.put("name", fakultas.getNama());
        jsonPost.put("copyrightText",
                "Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
        jsonPost.put("introductoryText", fakultas.getDeskripsi());
        jsonPost.put("shortDescription",
                "Repositori milik " + Common.getBahasaConfig("Fakultas") + " " + fakultas.getNama());
        jsonPost.put("sidebarText",
                "Repositori milik " + Common.getBahasaConfig("Fakultas") + " " + fakultas.getNama());
        if (Common.bolehKonfigurasi("dpsace_jadikan_fakultas_sebagai_root")) {
            return DspaceInformation.dspaceProcess(cookie, fakultas, jsonPost.toString(), update,
                    "communities", "communities");
        } else {
            return DspaceInformation.dspaceProcess(cookie, fakultas, jsonPost.toString(), update,
                    "communities", "communities/"
                            + PerguruanTinggiAction.getDspace(cookie, fakultas.getPerguruanTinggi(), update)
                            + "/communities");
        }
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Fakultas fakultas) throws Exception {
        Borderlayout borderlayout = new MyBorderlayout();

        Center center = new Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        Tabbox tabbox = new Tabbox();
        tabbox.setParent(cardWrap);
        tabbox.setWidth("100%");

        Tabs tabs = new Tabs();
        tabs.setParent(tabbox);

        new MyTabConfig("Data").setParent(tabs);
        final MyTabConfig tabAngket = new MyTabConfig("Berkas Akreditasi");
        tabAngket.setParent(tabs);
        tabAngket.setVisible(fakultas.getId() != null);
        new MyTabConfig("Deskripsi").setParent(tabs);

        Tabpanels tabpanels = new Tabpanels();
        tabpanels.setParent(tabbox);

        // ---- Tab Data ----
        Tabpanel tabpanelData = new ais.ui.util.MyTabpanel();
        tabpanelData.setParent(tabpanels);

        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(tabpanelData);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        fb.addSectionHeader("IDENTITAS");

        kode = new Textbox(fakultas.getKode() == null ? "" : fakultas.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        nama = new Textbox(fakultas.getNama() == null ? "" : fakultas.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama", nama);

        namaEn = new Textbox(fakultas.getNamaEn());
        namaEn.setWidth("100%");
        fb.addRow("Nama dalam Bahasa Inggris", namaEn);

        perguruanTinggi = new Combobox();
        Common.insertCombo(perguruanTinggi, new String[] { "nama", "kodePerguruanTinggi" }, "alamat1",
                PerguruanTinggi.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(perguruanTinggi, fakultas.getPerguruanTinggi());
        fb.addRow("Perguruan Tinggi", perguruanTinggi);

        fb.addSectionHeader("PIMPINAN");

        dekan = new AmbilDataDosenBanbox();
        dekan.setAttribute("dosen", fakultas.getDekan());
        dekan.setValue(fakultas.getDekan() == null ? "" : fakultas.getDekan().getNama());
        dekan.setWidth("100%");
        fb.addRow(Common.getBahasa("label_dekan"), dekan);

        pudek1 = new AmbilDataDosenBanbox();
        pudek1.setAttribute("dosen", fakultas.getPudek1());
        pudek1.setValue(fakultas.getPudek1() == null ? "" : fakultas.getPudek1().getNama());
        pudek1.setWidth("100%");
        fb.addRow(Common.getBahasa("label_pudek1"), pudek1);

        pudek2 = new AmbilDataDosenBanbox();
        pudek2.setAttribute("dosen", fakultas.getPudek2());
        pudek2.setValue(fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getNama());
        pudek2.setWidth("100%");
        fb.addRow(Common.getBahasa("label_pudek2"), pudek2);

        pudek3 = new AmbilDataDosenBanbox();
        pudek3.setAttribute("dosen", fakultas.getPudek3());
        pudek3.setValue(fakultas.getPudek3() == null ? "" : fakultas.getPudek3().getNama());
        pudek3.setWidth("100%");
        fb.addRow(Common.getBahasa("label_pudek3"), pudek3);

        wa = new Textbox(fakultas.getWa());
        wa.setWidth("100%");
        fb.addRow("WA Operator", wa);

        fb.addSectionHeader("SATUAN KERJA");

        satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false);
        satuanKerja.setAttribute("satuanKerja", fakultas.getSatuanKerja());
        satuanKerja.setValue(fakultas.getSatuanKerja() == null ? "" : fakultas.getSatuanKerja().getNama());
        satuanKerja.setWidth("100%");
        fb.addRow("Satuan Kerja", satuanKerja);

        dosenHarusPakaiSatuanKerja = new MyCheckboxConfig("Dosen Harus Pakai Satuan Kerja");
        dosenHarusPakaiSatuanKerja.setChecked(fakultas.getDosenHarusPakaiSatuanKerja());
        fb.addFullRow(dosenHarusPakaiSatuanKerja);

        fb.addSectionHeader("PEJABAT TAMBAHAN");

        labelPejabat1 = new Textbox(fakultas.getLabelPejabat1());
        labelPejabat1.setWidth("100%");
        pegawai1 = new AmbilDataPegawaiBanbox(false);
        pegawai1.setAttribute("pegawai", fakultas.getPegawai1());
        pegawai1.setValue(fakultas.getPegawai1() == null ? "" : fakultas.getPegawai1().getNama());
        pegawai1.setWidth("100%");
        pegawai1.setReadonly(true);
        MyFormRow rowPej1 = new MyFormRow();
        rowPej1.setParent(rows);
        rowPej1.appendChild(labelPejabat1);
        rowPej1.appendChild(pegawai1);

        labelPejabat2 = new Textbox(fakultas.getLabelPejabat2());
        labelPejabat2.setWidth("100%");
        pegawai2 = new AmbilDataPegawaiBanbox(false);
        pegawai2.setAttribute("pegawai", fakultas.getPegawai2());
        pegawai2.setValue(fakultas.getPegawai2() == null ? "" : fakultas.getPegawai2().getNama());
        pegawai2.setWidth("100%");
        pegawai2.setReadonly(true);
        MyFormRow rowPej2 = new MyFormRow();
        rowPej2.setParent(rows);
        rowPej2.appendChild(labelPejabat2);
        rowPej2.appendChild(pegawai2);

        labelPejabat3 = new Textbox(fakultas.getLabelPejabat3());
        labelPejabat3.setWidth("100%");
        pegawai3 = new AmbilDataPegawaiBanbox(false);
        pegawai3.setAttribute("pegawai", fakultas.getPegawai3());
        pegawai3.setValue(fakultas.getPegawai3() == null ? "" : fakultas.getPegawai3().getNama());
        pegawai3.setWidth("100%");
        pegawai3.setReadonly(true);
        MyFormRow rowPej3 = new MyFormRow();
        rowPej3.setParent(rows);
        rowPej3.appendChild(labelPejabat3);
        rowPej3.appendChild(pegawai3);

        fb.addSectionHeader("BERKAS & TAMPILAN");

        kop = null;
        Hbox hboxKop = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hboxKop, fakultas.getId(), LampiranLain.KOP_FAKULTAS,
                "KOP", false, new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        kop = (LampiranLain) arg0.getData();
                    }
                });
        fb.addRow("KOP (JPG)", hboxKop);

        foot = null;
        Hbox hboxFoot = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hboxFoot, fakultas.getId(), LampiranLain.FOOT_FAKULTAS,
                "FOOT", false, new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        foot = (LampiranLain) arg0.getData();
                    }
                });
        fb.addRow("FOOT (JPG)", hboxFoot);

        kopStempel = null;
        Hbox hboxStempel = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hboxStempel, fakultas.getId(), LampiranLain.STEMPEL_FAKULTAS,
                "Stempel", false, new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        kopStempel = (LampiranLain) arg0.getData();
                    }
                });
        fb.addRow("Stempel (JPG)", hboxStempel);

        warna = new Textbox();
        warna.setValue(fakultas.getWarna());
        warna.setWidth("100%");
        fb.addRow("Warna", warna);

        // ---- Tab Berkas Akreditasi (lazy load on click) ----
        final Tabpanel tabpanelAngket = new ais.ui.util.MyTabpanel();
        tabpanelAngket.setParent(tabpanels);
        tabAngket.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (tabpanelAngket.getChildren().isEmpty()) {
                    MyWindow w = new MyWindow("", "none", false);
                    w.setHeight("100%");
                    w.setWidth("100%");
                    w.setParent(tabpanelAngket);
                    new MyInclude("/pages/master/berkas_hasil_akreditasi.zul?fakultas=" + fakultas.getId())
                            .setParent(w);
                }
            }
        });

        // ---- Tab Deskripsi ----
        Tabpanel tabpanelDeskripsi = new ais.ui.util.MyTabpanel();
        tabpanelDeskripsi.setParent(tabpanels);
        tabpanelDeskripsi.appendChild(deskripsi = new MyCkEditor());
        deskripsi.setValue(fakultas.getDeskripsi());
        deskripsi.setHeight("100%");
        deskripsi.setWidth("100%");

        South south = new South();
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
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
        try {
            if (kode.getValue().trim().equals("")) {
                PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
                		"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
                		new String[] {
                				"Isi/pilih terlebih dahulu Kode.",
                				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
                		});
                return false;
            }
            if (nama.getValue().trim().equals("")) {
                PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
                		"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
                		new String[] {
                				"Isi/pilih terlebih dahulu Nama.",
                				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
                		});
                return false;
            }
            if (checkNamaFakultas()) {
                MyMessageboxConfig.show("Nama " + Common.getBahasaConfig("Fakultas") + " sudah ada di database",
                        "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                return false;
            }
            if (checkKodeFakultas()) {
                MyMessageboxConfig.show("Kode " + Common.getBahasaConfig("Fakultas") + " sudah ada di database",
                        "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                return false;
            }
            if (kop != null && !kop.getNama().toLowerCase().endsWith("jpg")) {
                MyMessageboxConfig.show("Kop harus berupa file JPG", "Peringatan",
                        MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                return false;
            }

            Session mySession1 = HibernateUtil.currentNativeSession();
            Fakultas entity = currentEntity;
            if (entity.getId() != null) {
                entity = (Fakultas) mySession1.load(Fakultas.class, entity.getId());
                currentEntity = entity;
            }
            entity.setKode(kode.getValue());
            entity.setNama(nama.getValue());
            entity.setNamaEn(namaEn.getValue());
            entity.setDekan((Dosen) dekan.getAttribute("dosen"));
            entity.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null
                    ? null : perguruanTinggi.getSelectedItem().getValue()));
            entity.setDeskripsi(deskripsi.getValue());
            entity.setWarna(warna.getValue());
            entity.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
            entity.setPudek1((Dosen) pudek1.getAttribute("dosen"));
            entity.setPudek2((Dosen) pudek2.getAttribute("dosen"));
            entity.setPudek3((Dosen) pudek3.getAttribute("dosen"));
            entity.setPegawai1((Pegawai) pegawai1.getAttribute("pegawai"));
            entity.setPegawai2((Pegawai) pegawai2.getAttribute("pegawai"));
            entity.setPegawai3((Pegawai) pegawai3.getAttribute("pegawai"));
            entity.setLabelPejabat1(labelPejabat1.getValue());
            entity.setLabelPejabat2(labelPejabat2.getValue());
            entity.setLabelPejabat3(labelPejabat3.getValue());
            entity.setWa(wa.getValue());
            entity.setDosenHarusPakaiSatuanKerja(dosenHarusPakaiSatuanKerja.isChecked());

            mySession1.getTransaction().begin();
            if (entity.getId() != null) {
                mySession1.update(entity);
            } else {
                mySession1.save(entity);
            }
            mySession1.getTransaction().commit();
            HibernateUtil.closeSession();

            // Auto-create/update Jabatan Dekan dan Staff
            Session session = HibernateUtil.currentSession();
            Jabatan jabatan = (Jabatan) session.createCriteria(Jabatan.class)
                    .add(Restrictions.eq("nama", Common.getBahasa("label_dekan")))
                    .setMaxResults(1).uniqueResult();
            if (jabatan == null) {
                jabatan = new Jabatan();
                jabatan.setEq_sks(0);
                jabatan.setNama(Common.getBahasa("label_dekan"));
                jabatan.setKeterangan(Common.getBahasa("label_dekan"));
                session.save(jabatan);
            }
            Staff staff = (Staff) session.createCriteria(Staff.class)
                    .add(Restrictions.eq("jabatan", jabatan))
                    .add(Restrictions.eq("fakultas", entity))
                    .setMaxResults(1).uniqueResult();
            if (staff == null) staff = new Staff();
            staff.setFakultas(entity);
            staff.setJabatan(jabatan);
            staff.setNama(entity.getDekan() == null ? "" : entity.getDekan().getNama());
            staff.setNip(entity.getDekan() == null ? "" : entity.getDekan().getCode());
            staff.setStaff(jabatan.getNama());
            Common.refreshSaveOrUpdate(session, staff);

            // Simpan lampiran KOP / FOOT / Stempel
            if (kop != null && kop.getId() != null) {
                try {
                    Session s = StreamingHibernateUtil.getInstance().currentSession();
                    s.refresh(kop);
                    kop.setRef(entity.getId());
                    s.getTransaction().begin();
                    s.update(kop);
                    s.getTransaction().commit();
                    StreamingHibernateUtil.getInstance().closeSession();
                } catch (Exception e) {
                    StreamingHibernateUtil.getInstance().rollbackTransaction();
                    Common.tampilErrorJikaAdmin(e);
                }
            }
            if (foot != null && foot.getId() != null) {
                try {
                    Session s = StreamingHibernateUtil.getInstance().currentSession();
                    s.refresh(foot);
                    foot.setRef(entity.getId());
                    s.getTransaction().begin();
                    s.update(foot);
                    s.getTransaction().commit();
                    StreamingHibernateUtil.getInstance().closeSession();
                } catch (Exception e) {
                    StreamingHibernateUtil.getInstance().rollbackTransaction();
                    Common.tampilErrorJikaAdmin(e);
                }
            }
            if (kopStempel != null && kopStempel.getId() != null) {
                try {
                    Session s = StreamingHibernateUtil.getInstance().currentSession();
                    s.refresh(kopStempel);
                    kopStempel.setRef(entity.getId());
                    s.getTransaction().begin();
                    s.update(kopStempel);
                    s.getTransaction().commit();
                    StreamingHibernateUtil.getInstance().closeSession();
                } catch (Exception e) {
                    StreamingHibernateUtil.getInstance().rollbackTransaction();
                    Common.tampilErrorJikaAdmin(e);
                }
            }
            return true;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return true;
    }

    public Boolean checkNamaFakultas() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Fakultas.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    public Boolean checkKodeFakultas() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Fakultas.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class FakultasRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Fakultas f = (Fakultas) arg1;

            new Label(f.getKode() == null ? "" : f.getKode().toString()).setParent(arg0);

            Vbox s;
            (s = RevisiHelper.createNewRevisi(Fakultas.class, f, f.getNama())).setParent(arg0);
            new Label(f.getNamaEn()).setParent(s);

            Vbox myvbox = new Vbox();
            myvbox.setParent(s);
            Hbox hbox = new Hbox();
            hbox.setParent(myvbox);
            LampiranLain.createDownloadUploadFileLain(hbox, f.getId(), LampiranLain.KOP_FAKULTAS,
                    "KOP", true, null, null, false, false, false, false);
            hbox = new Hbox();
            hbox.setParent(myvbox);
            LampiranLain.createDownloadUploadFileLain(hbox, f.getId(), LampiranLain.FOOT_FAKULTAS,
                    "FOOT", true, null, null, false, false, false, false);

            myvbox = new Vbox();
            myvbox.setParent(arg0);
            new Label(f.getDekan() == null ? "" : f.getDekan().getNama()).setParent(myvbox);
            new Label(f.getPudek1() == null ? "" : f.getPudek1().getNama()).setParent(myvbox);
            new Label(f.getPudek2() == null ? "" : f.getPudek2().getNama()).setParent(myvbox);
            new Label(f.getPudek3() == null ? "" : f.getPudek3().getNama()).setParent(myvbox);

            myvbox = new Vbox();
            myvbox.setParent(arg0);
            new Label(f.getPegawai1() == null ? ""
                    : f.getLabelPejabat1() + " : " + f.getPegawai1().getNama()).setParent(myvbox);
            new Label(f.getPegawai2() == null ? ""
                    : f.getLabelPejabat2() + " : " + f.getPegawai2().getNama()).setParent(myvbox);
            new Label(f.getPegawai3() == null ? ""
                    : f.getLabelPejabat3() + " : " + f.getPegawai3().getNama()).setParent(myvbox);

            new Label(f.getSatuanKerja() == null ? "" : f.getSatuanKerja().getNama()).setParent(arg0);
            new Label(f.getPerguruanTinggi() == null ? "" : f.getPerguruanTinggi().getNama()).setParent(arg0);

            Label l;
            (l = new Label(f.getWarna() + " " + f.getRgb())).setParent(arg0);
            l.setStyle("background-color:" + f.getWarna() + ";");

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(f.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    f.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(f);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, f, FakultasAction.this).setParent(arg0);
        }
    }
}
