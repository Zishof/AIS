package ais.action.master;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.GedungDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Gedung;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Pendataan Gedung. Digunakan sebagai referensi lokasi ruang perkuliahan.
 * Gedung dapat dikaitkan ke Fakultas/Prodi (PT) atau Yayasan/Sekolah.
 */
public class GedungAction extends GenericCrudAction<Gedung> {

    private static final long serialVersionUID = 6593841083724341265L;

    // ZK auto-wired extra search fields dari ZUL
    private Combobox searchjurusan;
    private Combobox searchfakultas;
    private Combobox searchyayasan;
    private Combobox searchsekolah;
    private Label labelFakProd;
    private Label labelYaySek;
    private Hbox fakProd;
    private Hbox yaySek;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox nama;
    private Textbox alamat;
    private Textbox telp;
    private Textbox cp;
    private Textbox lokasi;
    private Textbox ip;
    private Combobox fakultas;
    private Combobox jurusan;
    private Combobox yayasan;
    private Combobox sekolah;

    private boolean pt;
    private boolean ya;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Gedung> getEntityClass() {
        return Gedung.class;
    }

    @Override
    protected Gedung createNewEntity() {
        return new Gedung();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Gedung";
    }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "deskripsiLokasi", "alamat", "contactPerson", "telpon", "ip",
                "yayasan", "sekolah", "fakultas", "jurusan", "aktif" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        boolean[] ptYa = Common.chekPtAtauSekolah();
        pt = ptYa[0];
        ya = ptYa[1];

        if (labelFakProd != null) labelFakProd.setVisible(pt);
        if (fakProd != null) fakProd.setVisible(pt);
        if (labelYaySek != null) labelYaySek.setVisible(ya);
        if (yaySek != null) yaySek.setVisible(ya);

        Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
        Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Gedung.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
                .add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
                .add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
                .add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new GedungRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Gedung gedung) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        nama = new Textbox(gedung.getNama() == null ? "" : gedung.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama *", nama);

        telp = new Textbox(gedung.getTelpon() == null ? "" : gedung.getTelpon());
        telp.setWidth("100%");
        fb.addRow("Telp.", telp);

        lokasi = new Textbox(gedung.getDeskripsiLokasi() == null ? "" : gedung.getDeskripsiLokasi());
        lokasi.setWidth("100%");
        lokasi.setRows(5);
        fb.addRow("Deskripsi Lokasi", lokasi);

        alamat = new Textbox(gedung.getAlamat() == null ? "" : gedung.getAlamat());
        alamat.setWidth("100%");
        alamat.setRows(5);
        fb.addRow("Alamat", alamat);

        cp = new Textbox(gedung.getContactPerson() == null ? "" : gedung.getContactPerson());
        cp.setWidth("100%");
        fb.addRow("Nomor Kontak", cp);

        ip = new Textbox(gedung.getIp());
        ip.setWidth("100%");
        ip.setRows(2);
        fb.addRow("Alamat IP Gedung", ip,
                "Pisahkan dengan tanda semikolon (;) jika terdapat banyak alamat IP. Kosongkan jika berlaku untuk semua alamat IP");

        Tbmuser tbmuser = Common.getCurrentUser();

        fakultas = new Combobox();
        jurusan = new Combobox();
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

        yayasan = new Combobox();
        sekolah = new Combobox();
        Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

        if (ya) {
            fb.addSectionHeader("YAYASAN / SEKOLAH");

            Common.selectComboItem(yayasan,
                    gedung.getYayasan() == null ? (tbmuser == null ? null : tbmuser.ambilYayasan()) : gedung.getYayasan());
            yayasan.setWidth("100%");
            MyFormRow rowYayasan = new MyFormRow();
            rowYayasan.setVisible(true);
            rowYayasan.setParent(rows);
            rowYayasan.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
            rowYayasan.appendChild(yayasan);

            Common.pilihSekolah(sekolah,
                    gedung.getSekolah() == null ? (tbmuser == null ? null : tbmuser.ambilSekolah()) : gedung.getSekolah());
            sekolah.setWidth("100%");
            MyFormRow rowSekolah = new MyFormRow();
            rowSekolah.setVisible(true);
            rowSekolah.setParent(rows);
            rowSekolah.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
            rowSekolah.appendChild(sekolah);
        } else {
            Common.selectComboItem(yayasan,
                    gedung.getYayasan() == null ? (tbmuser == null ? null : tbmuser.ambilYayasan()) : gedung.getYayasan());
            Common.pilihSekolah(sekolah,
                    gedung.getSekolah() == null ? (tbmuser == null ? null : tbmuser.ambilSekolah()) : gedung.getSekolah());
        }

        if (pt) {
            fb.addSectionHeader("FAKULTAS / PRODI");

            Common.selectComboItem(fakultas,
                    gedung.getFakultas() == null ? (tbmuser == null ? null : tbmuser.ambilFakultas()) : gedung.getFakultas());
            if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
                Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
                        Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                        CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
            }
            fakultas.setWidth("100%");
            MyFormRow rowFakultas = new MyFormRow();
            rowFakultas.setVisible(true);
            rowFakultas.setParent(rows);
            rowFakultas.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
            rowFakultas.appendChild(fakultas);

            Common.pilihJurusan(jurusan,
                    gedung.getJurusan() == null ? (tbmuser == null ? null : tbmuser.ambilJurusan()) : gedung.getJurusan());
            jurusan.setWidth("100%");
            MyFormRow rowJurusan = new MyFormRow();
            rowJurusan.setVisible(true);
            rowJurusan.setParent(rows);
            rowJurusan.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
            rowJurusan.appendChild(jurusan);
        } else {
            Common.selectComboItem(fakultas,
                    gedung.getFakultas() == null ? (tbmuser == null ? null : tbmuser.ambilFakultas()) : gedung.getFakultas());
            if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
                Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
                        Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                        CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
            }
            Common.pilihJurusan(jurusan,
                    gedung.getJurusan() == null ? (tbmuser == null ? null : tbmuser.ambilJurusan()) : gedung.getJurusan());
        }

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
            PesanFormalHelper.tampilkanGagal("penyimpanan data Gedung",
                    "Kolom Nama Gedung belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data Gedung "
                            + "dapat disimpan.",
                    new String[] {
                            "Isi terlebih dahulu kolom Nama Gedung.",
                            "Ulangi proses penyimpanan setelah kolom tersebut terisi."
                    });
            return false;
        }
        GedungDao gedungDao = DaoFactory.getInstance().getGedungDao();
        Gedung entity = currentEntity;
        if (entity.getId() != null) {
            entity = gedungDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setDeskripsiLokasi(lokasi.getValue());
        entity.setTelpon(telp.getValue());
        entity.setAlamat(alamat.getValue());
        entity.setContactPerson(cp.getValue());
        entity.setIp(ip.getValue());
        entity.setFakultas((Fakultas) (fakultas == null || fakultas.getSelectedItem() == null
                || fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue()));
        entity.setJurusan((Jurusan) (jurusan == null || jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue()));
        entity.setYayasan((Yayasan) (yayasan == null || yayasan.getSelectedItem() == null
                ? null : yayasan.getSelectedItem().getValue()));
        entity.setSekolah((Sekolah) (sekolah == null || sekolah.getSelectedItem() == null
                ? null : sekolah.getSelectedItem().getValue()));
        Common.refreshSaveOrUpdate(entity);
        return true;
    }

    // ======================== Renderer ========================

    class GedungRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Gedung gedung = (Gedung) arg1;

            new Label(gedung.getNama()).setParent(arg0);
            new Label(gedung.getDeskripsiLokasi()).setParent(arg0);
            new Label(gedung.getTelpon()).setParent(arg0);
            new Label(gedung.getAlamat()).setParent(arg0);
            new Label(gedung.getContactPerson()).setParent(arg0);
            new Label(gedung.getIp()).setParent(arg0);

            new Label((gedung.getFakultas() == null ? "" : gedung.getFakultas().getNama())
                    + (gedung.getJurusan() == null ? "" : " / " + gedung.getJurusan().getNama())
                    + (gedung.getYayasan() == null ? "" : gedung.getYayasan().getNama())
                    + (gedung.getSekolah() == null ? "" : " / " + gedung.getSekolah().getNama()))
                    .setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(gedung.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    gedung.setAktif(checkbox.isChecked());
                    Common.refreshUpdate(gedung);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, gedung, GedungAction.this).setParent(arg0);
        }
    }
}
