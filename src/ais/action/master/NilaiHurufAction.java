package ais.action.master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisNilaiHurufMatakuliah;
import ais.database.model.Jurusan;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Action CRUD (berbasis kerangka {@link GenericCrudAction}) untuk mengelola tabel konversi Nilai
 * Huruf ({@link NilaiHuruf}) — pemetaan rentang nilai angka ({@code mulai}-{@code sampai}) ke
 * huruf mutu (mis. A, B+, C) beserta bobot IPK-nya, dapat spesifik per fakultas/jurusan, tahun
 * angkatan, tahun akademik/semester, kode mata kuliah, dan jenis nilai huruf.
 *
 * <h2>Fitur tambahan di luar CRUD baku</h2>
 * <ul>
 * <li>Unggah massal lewat berkas Excel (.xlsx) ({@link #prosesUploadNilaiHuruf(UploadEvent)}),
 * ditambahkan sebagai tombol toolbar hanya bagi pengguna dengan hak edit dan hapus sekaligus.</li>
 * <li>Sinkronisasi ulang nilai huruf mahasiswa berdasarkan tabel konversi terbaru:
 * {@link #onSyncronisasiNilai(Event)} (seluruh nilai) dan
 * {@link #onSyncronisasiHanyaYangBelumDapatNilai(Event)} (hanya yang belum mendapat nilai huruf),
 * keduanya dijalankan pada thread terpisah dengan indikator progres.</li>
 * </ul>
 * <p>
 * {@link #onSave(Event)} menyimpan seluruh field form ke entitas, lalu memuat ulang cache statis
 * tabel konversi ({@code ConstantValues.realoadNilaiHuruf}) baik segera maupun via timer susulan,
 * agar perhitungan IPK di seluruh aplikasi memakai data terbaru tanpa perlu restart.
 * {@link #initCriteria(boolean)} membangun kueri pencarian dengan filter jurusan, fakultas, tahun
 * angkatan, dan nilai huruf (pencocokan persis).
 * </p>
 */
public class NilaiHurufAction extends GenericCrudAction<NilaiHuruf> {

    private static final long serialVersionUID = 261036075526361529L;

    // ZK auto-wired extra search fields
    private Combobox searchfakultas;
    private Combobox searchjurusan;
    private Textbox searchnilaiHuruf;
    private Decimalbox searchtahunAngkatan;

    // Form fields
    private Decimalbox mulai;
    private Decimalbox sampai;
    private Textbox nilaiHuruf;
    private Decimalbox nilaiDiIPK;
    private Decimalbox tahunAngkatan;
    private Combobox fakultas;
    private Combobox jurusan;
    private Combobox tahunAkademik;
    private Combobox semester;
    private MyTextbox kodeMk;
    private Combobox jenisNilaiHuruf;
    private Textbox keterangan;

    private static final String[] CONTENTS = new String[] { "id", "mulai", "sampai", "tahunAngkatan",
            "nilaiHuruf", "nilaiDiIPK", "jurusan", "fakultas", "tahunAkademik", "semester",
            "lulus", "kodeMk", "jenisNilaiHuruf", "keterangan" };

    // ======================== Abstract implementations ========================

    @Override
    protected Class<NilaiHuruf> getEntityClass() { return NilaiHuruf.class; }

    @Override
    protected NilaiHuruf createNewEntity() { return new NilaiHuruf(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Nilai Huruf"; }

    @Override
    protected String[] getDownloadUploadContents() { return CONTENTS; }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);

        if (delete && edit) {
            MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, CONTENTS);
            if (add != null) {
            add.getParent().appendChild(cetakToolbarbutton);
            }

            MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
                    "Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
            upload.setVisible((add != null && add.isVisible()) && edit && delete);
            upload.setUpload(Common.ukuranFileUpload());
            upload.addEventListener("onUpload", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    prosesUploadNilaiHuruf((UploadEvent) event);
                }
            });
            if (add != null) {
            add.getParent().appendChild(upload);
            }
        }
    }

    /**
     * Membangun kueri pencarian tabel nilai huruf, difilter jurusan, fakultas, tahun angkatan, dan
     * nilai huruf (pencocokan persis).
     *
     * @param order {@code true} untuk mengurutkan hasil berdasarkan nilai huruf menaik
     * @return kriteria Hibernate siap dieksekusi/dipaginasi
     */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(NilaiHuruf.class);
        if (order) criteria.addOrder(Order.asc("nilaiHuruf"));
        criteria.add(searchjurusan == null || searchjurusan.getSelectedItem() == null
                        || searchjurusan.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
                .add(searchfakultas == null || searchfakultas.getSelectedItem() == null
                        || searchfakultas.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
                .add(searchtahunAngkatan == null || searchtahunAngkatan.getValue() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("tahunAngkatan", searchtahunAngkatan.getValue().intValue()))
                .add(searchnilaiHuruf == null || searchnilaiHuruf.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ilike("nilaiHuruf", searchnilaiHuruf.getValue().trim(), MatchMode.EXACT));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new NilaiHurufRenderer();
    }

    // ======================== Sync event handlers ========================

    /** Menjalankan sinkronisasi ulang SELURUH nilai huruf mahasiswa berdasarkan tabel konversi saat ini, pada thread terpisah dengan label progres yang diperbarui berkala. */
    public void onSyncronisasiNilai(Event event) {
        final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi nilai huruf sedang berlangsung, harap menunggu.."));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Common.synNilaiHuruf(label, false);
            }
        }).start();
        startSyncTimer(label);
    }

    /** Seperti {@link #onSyncronisasiNilai(Event)}, tetapi hanya memproses mahasiswa yang belum memiliki nilai huruf. */
    public void onSyncronisasiHanyaYangBelumDapatNilai(Event event) {
        final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi nilai huruf sedang berlangsung, harap menunggu.."));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Common.synNilaiHuruf(label, true);
            }
        }).start();
        startSyncTimer(label);
    }

    private void startSyncTimer(final Label label) {
        final Timer timer = new Timer(500);
        timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        timer.setRepeats(true);
        timer.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Clients.showBusy(label.getValue());
                if (label.getValue().isEmpty()) {
                    Clients.clearBusy();
                    MyMessageboxConfig.show("Synchronize nilai huruf berhasil dilakukan", "Pemberitahuan",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    timer.detach();
                }
            }
        });
        timer.start();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final NilaiHuruf nilai) throws Exception {
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // ---- Center: scrollable card ----
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

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        mulai = new Decimalbox(new BigDecimal(nilai.getMulai() == null ? 0 : nilai.getMulai()));
        mulai.setWidth("100%");
        fb.addRow("Mulai", mulai);

        sampai = new Decimalbox(new BigDecimal(nilai.getSampai() == null ? 0 : nilai.getSampai()));
        sampai.setWidth("100%");
        fb.addRow("Sampai", sampai);

        nilaiHuruf = new Textbox(nilai.getNilaiHuruf());
        nilaiHuruf.setWidth("100%");
        fb.addRow("Huruf", nilaiHuruf);

        nilaiDiIPK = new Decimalbox(new BigDecimal(nilai.getNilaiDiIPK() == null ? 0 : nilai.getNilaiDiIPK()));
        nilaiDiIPK.setWidth("100%");
        fb.addRow("Nilai di IPK", nilaiDiIPK);

        tahunAngkatan = new Decimalbox(new BigDecimal(nilai.getTahunAngkatan() == null ? 0 : nilai.getTahunAngkatan()));
        tahunAngkatan.setWidth("100%");
        fb.addRow("Tahun Angkatan", tahunAngkatan);

        Tbmuser tbmuser = Common.getCurrentUser();
        fakultas = new Combobox();
        jurusan = new Combobox();
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

        Common.selectComboItem(fakultas,
                nilai.getFakultas() == null ? tbmuser.ambilFakultas() : nilai.getFakultas());
        fakultas.setWidth("100%");
        fb.addRow("Fakultas", fakultas);

        if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
            Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
                    Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                    CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
        }

        Common.selectComboItem(true, jurusan,
                nilai.getJurusan() == null ? tbmuser.ambilJurusan() : nilai.getJurusan());
        jurusan.setWidth("100%");
        fb.addRow("Program Studi", jurusan);

        jenisNilaiHuruf = new Combobox();
        Common.insertComboDanSemua(jenisNilaiHuruf, new String[] { "nama" }, "keterangan",
                JenisNilaiHurufMatakuliah.class, "Nilai Huruf Default", Restrictions.eq("aktif", true));
        Common.selectComboItem(true, jenisNilaiHuruf, nilai.getJenisNilaiHuruf());
        jenisNilaiHuruf.setWidth("100%");
        fb.addRow("Jenis Nilai Huruf", jenisNilaiHuruf);

        tahunAkademik = new Combobox();
        org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
        comboitem.setLabel("Semua");
        comboitem.setValue(null);
        tahunAkademik.appendChild(comboitem);
        tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);

        semester = new Combobox();
        comboitem = new MyComboitemConfig();
        comboitem.setLabel("Semua");
        comboitem.setValue(null);
        semester.appendChild(comboitem);
        comboitem = new MyComboitemConfig();
        comboitem.setLabel(Perkuliahan.GENAP);
        comboitem.setValue(Perkuliahan.GENAP);
        semester.appendChild(comboitem);
        comboitem = new MyComboitemConfig();
        comboitem.setLabel(Perkuliahan.GANJIL);
        comboitem.setValue(Perkuliahan.GANJIL);
        semester.appendChild(comboitem);

        Common.selectComboItem(tahunAkademik, nilai.getTahunAkademik());
        tahunAkademik.setWidth("100%");
        fb.addRow("Berlaku Mulai Tahun Akademik", tahunAkademik);

        Common.selectComboItem(semester, nilai.getSemester());
        semester.setReadonly(true);
        semester.setWidth("100%");
        fb.addRow("Berlaku Mulai Semester", semester);

        kodeMk = new MyTextbox(nilai.getKodeMk());
        kodeMk.setWidth("100%");
        kodeMk.setRows(2);
        fb.addRow("Khusus untuk kode matakuliah", kodeMk,
                "Jika terdapat banyak kode matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56");

        keterangan = new Textbox(nilai.getKeterangan());
        keterangan.setWidth("100%");
        fb.addRow("Keterangan", keterangan);

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
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

    /**
     * Menyimpan satu baris konversi nilai huruf dengan seluruh field form, lalu memuat ulang cache
     * statis tabel konversi ({@code ConstantValues.realoadNilaiHuruf}) segera dan sekali lagi lewat
     * timer susulan, agar perubahan langsung berpengaruh pada perhitungan IPK di seluruh aplikasi.
     *
     * @param event event ZK asal aksi simpan
     * @return selalu {@code true} (tidak ada validasi tambahan di luar tipe data form)
     */
    public boolean onSave(Event event) throws Exception {
        Session session = HibernateUtil.currentSession();
        NilaiHuruf entity = currentEntity;
        if (entity.getId() != null) {
            entity = (NilaiHuruf) session.load(NilaiHuruf.class, entity.getId());
            currentEntity = entity;
        }
        entity.setMulai(mulai.getValue().doubleValue());
        entity.setSampai(sampai.getValue().doubleValue());
        entity.setTahunAngkatan(tahunAngkatan.getValue().intValue());
        entity.setNilaiHuruf(nilaiHuruf.getValue());
        entity.setNilaiDiIPK(nilaiDiIPK.getValue().doubleValue());
        entity.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
                ? null : jurusan.getSelectedItem().getValue()));
        entity.setFakultas((Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
                ? null : fakultas.getSelectedItem().getValue()));
        entity.setTahunAkademik((String) (tahunAkademik.getSelectedItem() == null
                || tahunAkademik.getSelectedItem().getValue() == null
                        ? null : tahunAkademik.getSelectedItem().getValue()));
        entity.setSemester((String) (semester.getSelectedItem() == null
                ? null : semester.getSelectedItem().getValue()));
        entity.setJenisNilaiHuruf((JenisNilaiHurufMatakuliah) (jenisNilaiHuruf.getSelectedItem() == null
                ? null : jenisNilaiHuruf.getSelectedItem().getValue()));
        entity.setKodeMk(kodeMk.getValue().trim());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        ConstantValues.realoadNilaiHuruf(HibernateUtil.currentSession());
        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                ConstantValues.realoadNilaiHuruf(HibernateUtil.currentSession());
            }
        });
        return true;
    }

    // ======================== Upload logic ========================

    private void prosesUploadNilaiHuruf(UploadEvent uploadEvent) throws Exception {
        Media media = uploadEvent.getMedia();
        if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
        if (!media.getName().toLowerCase().endsWith("xlsx")) {
            MyMessageboxConfig.show(
                    "File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). " + media,
                    "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
            return;
        }
        InputStream inputStream = media.getStreamData();
        File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
        file.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        int c;
        while ((c = inputStream.read()) != -1) fileOutputStream.write(c);
        fileOutputStream.close();
        inputStream.close();

        XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
        XSSFSheet sheet = workbook.getSheetAt(0);
        String peringatan = "";
        int rowCount = (sheet.getLastRowNum() + 1);
        for (int i = 1; i < rowCount; i++) {
            try {
                Session sess = HibernateUtil.currentNativeSession();
                Double mulaiVal = Common.getSheetContentAsDouble(sheet, 1, i);
                Double sampaiVal = Common.getSheetContentAsDouble(sheet, 2, i);
                Integer tahunAngkatanVal = Common.getSheetContentAsInteger(sheet, 3, i);
                String nilaiHurufText = Common.getSheetContentAsString(sheet, 4, i);
                Double nilaiDiIPKVal = Common.getSheetContentAsDouble(sheet, 5, i);
                Jurusan jurusanVal = (Jurusan) Common.getSheetContentAsObject(sheet, 6, i, Jurusan.class,
                        Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                Fakultas fakultasVal = (Fakultas) Common.getSheetContentAsObject(sheet, 7, i, Fakultas.class);
                String tahunAkademikVal = Common.getSheetContentAsString(sheet, 8, i);
                String semesterVal = Common.getSheetContentAsString(sheet, 9, i);
                String kodeMkVal = Common.getSheetContentAsString(sheet, 11, i);
                JenisNilaiHurufMatakuliah jenisVal = (JenisNilaiHurufMatakuliah) Common.getSheetContentAsObject(
                        sheet, 12, i, JenisNilaiHurufMatakuliah.class);

                if (nilaiHurufText != null && !nilaiHurufText.trim().isEmpty() && mulaiVal != null
                        && sampaiVal != null && tahunAngkatanVal != null && nilaiDiIPKVal != null) {
                    Long id = Common.getSheetContentAsLong(sheet, 0, i);
                    NilaiHuruf nilaiHurufObj = id == null || id.equals(-1L) ? null
                            : (NilaiHuruf) sess.createCriteria(NilaiHuruf.class)
                                    .add(Restrictions.idEq(id)).uniqueResult();
                    if (nilaiHurufObj == null) nilaiHurufObj = new NilaiHuruf();
                    nilaiHurufObj.setTahunAkademik(tahunAkademikVal == null || tahunAkademikVal.trim().isEmpty()
                            ? null : tahunAkademikVal.trim());
                    nilaiHurufObj.setSemester(semesterVal == null || semesterVal.trim().isEmpty()
                            ? null : semesterVal.trim());
                    nilaiHurufObj.setNama(nilaiHurufText);
                    nilaiHurufObj.setNilaiHuruf(nilaiHurufText);
                    nilaiHurufObj.setFakultas(fakultasVal);
                    nilaiHurufObj.setJurusan(jurusanVal);
                    nilaiHurufObj.setMulai(mulaiVal);
                    nilaiHurufObj.setNilaiDiIPK(nilaiDiIPKVal);
                    nilaiHurufObj.setSampai(sampaiVal);
                    nilaiHurufObj.setTahunAngkatan(tahunAngkatanVal);
                    nilaiHurufObj.setKodeMk(kodeMkVal);
                    nilaiHurufObj.setJenisNilaiHuruf(jenisVal);
                    sess.getTransaction().begin();
                    sess.saveOrUpdate(nilaiHurufObj);
                    sess.getTransaction().commit();
                }
                HibernateUtil.closeSession();
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        ConstantValues.realoadNilaiHuruf(HibernateUtil.currentSession());
        MyMessageboxConfig.show(
                "Upload data berhasil dilakukan." + (peringatan.isEmpty() ? "" : "\n" + peringatan),
                "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
                new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        onSearchDefault(null);
                    }
                });
    }

    // ======================== Renderer ========================

    class NilaiHurufRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final NilaiHuruf nilai = (NilaiHuruf) arg1;

            RevisiHelper.createNewRevisi(NilaiHuruf.class, nilai, nilai.getMulai().toString()).setParent(arg0);
            new Label(nilai.getSampai().toString()).setParent(arg0);
            new Label(nilai.getNilaiHuruf()).setParent(arg0);
            new Label(nilai.getNilaiDiIPK().toString()).setParent(arg0);
            new Label(nilai.getTahunAngkatan() == null ? "" : nilai.getTahunAngkatan().toString()).setParent(arg0);
            new Label(nilai.getJurusan() == null ? "" : nilai.getJurusan().getNama()).setParent(arg0);
            new Label(nilai.getFakultas() == null ? "" : nilai.getFakultas().getNama()).setParent(arg0);
            new Label((nilai.getTahunAkademik() == null || nilai.getTahunAkademik().trim().isEmpty()
                    ? " Semua " : nilai.getTahunAkademik()) + " / "
                    + (nilai.getSemester() == null || nilai.getSemester().trim().isEmpty()
                            ? " Semua " : nilai.getSemester())
                    + " / " + nilai.getTa()).setParent(arg0);
            new Label(nilai.getKodeMk()).setParent(arg0);
            new Label(nilai.getJenisNilaiHuruf() == null ? "" : nilai.getJenisNilaiHuruf().getNama()).setParent(arg0);
            new Label(nilai.getKeterangan()).setParent(arg0);

            // Satu sel berisi DUA checkbox bertumpuk: "Lulus" (status kelulusan) dan
            // "Tampilkan status lulus/tidak lulus" (kontrol tampilan status di halaman-halaman).
            org.zkoss.zul.Vbox vboxCek = new org.zkoss.zul.Vbox();
            vboxCek.setSpacing("2px");
            vboxCek.setStyle("white-space:nowrap;");
            vboxCek.setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Lulus");
            checkbox.setChecked(nilai.getLulus());
            checkbox.setTooltiptext("Tandai nilai huruf ini sebagai LULUS.");
            checkbox.setStyle("white-space:nowrap;");
            checkbox.setParent(vboxCek);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    nilai.setLulus(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(nilai);
                }
            });

            // Checkbox BARU: bila dicentang, status Lulus/Tidak Lulus untuk nilai huruf ini
            // ditampilkan (mis. hanya untuk jenjang S2). Bila tidak, status tsb disembunyikan di
            // halaman-halaman (Dasbor Studi, Daftar Historis MK, dll.). Default: tidak dicentang.
            final MyCheckboxConfig checkboxTampilStatus = new MyCheckboxConfig("Tampilkan status");
            checkboxTampilStatus.setChecked(Boolean.TRUE.equals(nilai.getTampilkanStatusLulus()));
            checkboxTampilStatus.setTooltiptext("Tampilkan status Lulus/Tidak Lulus untuk nilai huruf ini di halaman-halaman (mis. jenjang S2). Bila tidak dicentang, status disembunyikan.");
            checkboxTampilStatus.setStyle("white-space:nowrap;");
            checkboxTampilStatus.setParent(vboxCek);
            checkboxTampilStatus.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    nilai.setTampilkanStatusLulus(checkboxTampilStatus.isChecked());
                    Common.refreshSaveOrUpdate(nilai);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, nilai, NilaiHurufAction.this).setParent(arg0);
        }
    }
}
