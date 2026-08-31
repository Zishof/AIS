package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.AsramaPunyaMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanRekapitulasiAsrama;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.UploadReportHelper;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Asrama;
import ais.database.model.AsramaPunyaMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Asrama pada modul akademik, dibangun di atas {@link GenericCrudAction}.
 * Setiap asrama dapat dibatasi cakupannya ke fakultas/jurusan/tahun angkatan tertentu (kosong
 * berarti berlaku untuk semua), dan menaungi daftar mahasiswa penghuninya lewat
 * {@link AsramaPunyaMahasiswaHelper} pada baris yang dapat diperluas.
 *
 * <p>
 * {@link #onAfterInit(Component)} melakukan migrasi data satu kali: bila belum ada baris
 * {@link Asrama} sama sekali, kelas ini membangkitkan baris awal dari nilai unik kolom
 * {@code asrama} pada tabel {@link Perkuliahan} lama, atau membuat satu asrama default "A" bila
 * data lama itu pun kosong. Mendukung impor massal lewat unggah Excel
 * ({@link #onUploadData(Event)} — satu sheet per asrama, kolom NIM dicoba dari 4 posisi kolom
 * berbeda, dijalankan di thread terpisah dengan laporan hasil via {@link UploadReportHelper} dan
 * status via polling timer), dikendalikan oleh flag konfigurasi
 * {@code boleh_upload_data_asrama}. Tombol unduh kustom ({@link #cetakDataCustomButton})
 * menghasilkan file Excel (satu sheet per asrama, daftar mahasiswa penghuni) dan menampilkan
 * pratinjau spreadsheet sebelum diunduh — juga dijalankan di thread terpisah dengan polling timer.
 * Terdapat pula tab laporan rekapitulasi asrama yang dimuat lazy saat pertama dibuka
 * ({@link #onTampilAsrama(Event)}).
 * </p>
 */
public class AsramaAction extends GenericCrudAction<Asrama> {

    private static final long serialVersionUID = -5779730267402400328L;

    // ZK auto-wired extra search fields
    private Combobox searchfakultas;
    private Combobox searchjurusan;
    private Decimalbox searchtahun;

    // ZK auto-wired tab panel
    private Tabpanel laporanAsrama;

    // ZK auto-wired upload button
    private MyToolbarbuttonConfig uploadData;

    // Form fields
    private Textbox nama;
    private Combobox fakultas;
    private Combobox jurusan;
    private Textbox keterangan;
    private Decimalbox tahunAngkatan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Asrama> getEntityClass() { return Asrama.class; }

    @Override
    protected Asrama createNewEntity() { return new Asrama(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Asrama"; }

    /**
     * Membangkitkan data asrama awal bila tabel masih kosong (dari nilai unik kolom lama
     * {@code Perkuliahan.asrama}, atau satu asrama default "A" bila keduanya kosong),
     * menambahkan tombol cetak data ke toolbar, menyiapkan combobox fakultas/jurusan (termasuk
     * untuk filter pencarian), dan mengatur visibilitas tombol unggah data sesuai konfigurasi
     * {@code boleh_upload_data_asrama}.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        Session session = HibernateUtil.currentSession();

        int count = ((Number) session.createCriteria(Asrama.class)
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
        if (count == 0) {
            List asramaes = session.createCriteria(Perkuliahan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.isNotNull("asrama"))
                    .add(Restrictions.ne("asrama", ""))
                    .setProjection(Projections.groupProperty("asrama")).list();
            for (Object k : asramaes) {
                if (k != null) {
                    Asrama asrama = new Asrama();
                    asrama.setNama(k.toString());
                    asrama.setKeterangan("Asrama " + k.toString());
                    session.save(asrama);
                }
            }
        }

        count = ((Number) session.createCriteria(Asrama.class)
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
        if (count == 0) {
            Asrama asrama = new Asrama();
            asrama.setNama("A");
            asrama.setKeterangan("Asrama A");
            session.save(asrama);
        }

        MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Asrama", "/img/print.png");
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        fakultas = new Combobox();
        jurusan = new Combobox();
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);

        if (uploadData != null) {
            uploadData.setVisible(Common.bolehKonfigurasi("boleh_upload_data_asrama"));
        }
    }

    /** Membangun kriteria pencarian daftar asrama, difilter berdasarkan kecocokan sebagian nama, tahun angkatan, jurusan, dan fakultas bila diisi (baris tanpa cakupan tetap ikut tampil sebagai "berlaku untuk semua"). */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Asrama.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
                .add(searchtahun == null || searchtahun.getValue() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("tahunAngkatan", searchtahun.getValue().intValue()))
                .add(searchjurusan == null || searchjurusan.getSelectedItem() == null
                        || searchjurusan.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.or(Restrictions.isNull("jurusan"),
                                        CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))
                .add(searchfakultas == null || searchfakultas.getSelectedItem() == null
                        || searchfakultas.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.or(Restrictions.isNull("fakultas"),
                                        CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new AsramaRenderer();
    }

    // ======================== ZUL lazy-tab event handler ========================

    /** Memuat panel {@link LaporanRekapitulasiAsrama} ke tab laporan secara lazy, hanya sekali saat tab masih kosong. */
    public void onTampilAsrama(Event event) {
        if (laporanAsrama.getChildren().size() == 0) {
            LaporanRekapitulasiAsrama laporanRekapitulasiAsrama = new LaporanRekapitulasiAsrama();
            laporanRekapitulasiAsrama.setHeight("100%");
            laporanRekapitulasiAsrama.setWidth("100%");
            laporanRekapitulasiAsrama.setParent(laporanAsrama);
        }
    }

    // ======================== Upload event handler ========================

    /**
     * Mengimpor massal data penghuni asrama dari berkas Excel (.xlsx): setiap sheet mewakili
     * satu asrama (dicari berdasarkan nama sheet, dibuat bila belum ada), dan setiap baris
     * dicocokkan ke {@link Mahasiswa} lewat NIM yang dicoba dari 4 posisi kolom berbeda (kolom
     * 0-3) hingga ditemukan. Relasi lama mahasiswa tersebut ke asrama manapun dihapus lebih
     * dulu ({@code delete from asrama_punya_mahasiswa}) sebelum baris {@link
     * AsramaPunyaMahasiswa} baru disimpan, sehingga satu mahasiswa hanya terdaftar di satu
     * asrama. Diproses di thread terpisah dengan laporan hasil sukses/gagal per baris via
     * {@link UploadReportHelper}, status dipantau lewat timer polling yang mengunduh laporan
     * dan menampilkan ringkasan setelah selesai. Menolak berkas yang bukan {@code .xlsx}.
     */
    public void onUploadData(Event event) throws Exception {
        final Tbmuser tbmuser = Common.getCurrentUser();
        ForwardEvent forwardEvent = (ForwardEvent) event;
        Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
        if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
        if (!media.getName().toLowerCase().endsWith("xlsx")) {
            MyMessageboxConfig.show(
                    "File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
                            + media,
                    "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
            return;
        }

        InputStream inputStream = media.getStreamData();
        final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
        file.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        int c;
        while ((c = inputStream.read()) != -1) fileOutputStream.write(c);
        fileOutputStream.close();
        inputStream.close();

        final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data asrama sedang berlangsung, harap menunggu.."));
        final UploadReportHelper report = new UploadReportHelper("Upload Asrama");
        final Label downloadPath = new Label();
        new Thread(new Runnable() {
            @Override
            public void run() {
            	try {
                XSSFWorkbook workbook;
                try {
                    workbook = new XSSFWorkbook(file.getAbsolutePath());
                    for (XSSFSheet sheet : Common.getAllXSSFSheet(workbook)) {
                        Session session = HibernateUtil.currentNativeSession();
                        Asrama asrama = (Asrama) session.createCriteria(Asrama.class)
                                .add(Restrictions.ilike("nama", sheet.getSheetName().trim(), MatchMode.EXACT))
                                .setMaxResults(1).uniqueResult();
                        if (asrama == null) {
                            asrama = new Asrama();
                            asrama.setNama(sheet.getSheetName().trim());
                            asrama.setKeterangan(sheet.getSheetName().trim());
                            session.getTransaction().begin();
                            session.save(asrama);
                            session.getTransaction().commit();
                        }
                        HibernateUtil.closeSession();

                        int size = (sheet.getLastRowNum() + 1);
                        for (int i = 0; i < size; i++) {
                            session = HibernateUtil.currentNativeSession();
                            try {
                                Mahasiswa mahasiswa = null;
                                try {
                                    String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
                                    mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
                                    if (mahasiswa == null) {
                                        nim = Common.getCellContent(Common.getCell(sheet, 1, i));
                                        mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
                                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                                .add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
                                    }
                                    if (mahasiswa == null) {
                                        nim = Common.getCellContent(Common.getCell(sheet, 2, i));
                                        mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
                                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                                .add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
                                    }
                                    if (mahasiswa == null) {
                                        nim = Common.getCellContent(Common.getCell(sheet, 3, i));
                                        mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
                                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                                .add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
                                    }
                                } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/AsramaAction.java:268");
                                    // nim column lookup failures are non-fatal
                                }
                                if (mahasiswa == null) continue;

                                session.createSQLQuery(
                                        "delete from asrama_punya_mahasiswa where mahasiswa=" + mahasiswa.getId())
                                        .executeUpdate();
                                AsramaPunyaMahasiswa asramaPunyaMahasiswa = new AsramaPunyaMahasiswa();
                                asramaPunyaMahasiswa.setMahasiswa(mahasiswa);
                                asramaPunyaMahasiswa.setAsrama(asrama);
                                asramaPunyaMahasiswa.setOleh(tbmuser.getUserId());
                                asramaPunyaMahasiswa.setTbmuser(tbmuser);
                                asramaPunyaMahasiswa.setDiubahDari(AsramaAction.class.getSimpleName());
                                session.getTransaction().begin();
                                session.save(asramaPunyaMahasiswa);
                                session.getTransaction().commit();
                                HibernateUtil.closeSession();
                                label.setValue("Upload mahasiswa " + mahasiswa + " di asrama " + asrama.getNama()
                                        + ".. " + Common.numberFormat.get().format(i * 100.0 / size) + " %");
                                report.sukses(i, mahasiswa.getNim(), "Asrama " + asrama.getNama());
                            } catch (Exception e1) {
                                HibernateUtil.closeSession();
                                e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/AsramaAction.java:290");
                                report.gagal(i, "baris-" + i, e1, "Periksa data NIM pada baris ini");
                            }
                        }
                        session = HibernateUtil.currentNativeSession();
                        AsramaPunyaMahasiswaHelper.syncAsrama(asrama, session, true, tbmuser);
                        HibernateUtil.closeSession();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/AsramaAction.java:298");
                }
                // FIX compile "unreported exception IOException": simpanLaporan() checked exception.
                try {
                    downloadPath.setValue(report.simpanLaporan().getAbsolutePath());
                } catch (java.io.IOException eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) src/ais/action/master/AsramaAction.java:306"); }
                label.setValue("");
                        	} finally {
            		ais.database.hibernate.HibernateUtil.closeSession();
            	}
            }
        }).start();

        final Timer timer = new Timer(500);
        timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        timer.setRepeats(true);
        timer.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Clients.showBusy(label.getValue());
                if (label.getValue().isEmpty()) {
                    Clients.clearBusy();
                    try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception ignored) {}
                    MyMessageboxConfig.show("Update data asrama berhasil dilakukan. " + report.getRingkasan(), "Pemberitahuan",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    timer.detach();
                }
            }
        });
        timer.start();
    }

    // ======================== Form content ========================

    /**
     * Membangun form tambah/ubah (nama, fakultas, program studi cascading terhadap fakultas,
     * tahun angkatan, keterangan) beserta toolbar Batal/Simpan; fakultas/jurusan default
     * mengikuti fakultas/jurusan pengguna yang login bila belum diisi pada entitas.
     */
    @Override
    protected void buildFormContent(MyWindow window, final Asrama asrama) throws Exception {
        if (fakultas == null) {
            fakultas = new Combobox();
            jurusan = new Combobox();
            Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
        }

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

        nama = new Textbox(asrama.getNama() == null ? "" : asrama.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Asrama *", nama);

        Tbmuser tbmuser = Common.getCurrentUser();

        Common.selectComboItem(fakultas,
                asrama.getFakultas() == null ? tbmuser.ambilFakultas() : asrama.getFakultas());
        fakultas.setWidth("100%");
        fb.addRow("Fakultas", fakultas,
                "Kosongkan " + Common.getBahasaConfig("Fakultas")
                        + " jika asrama ini berlaku untuk semua " + Common.getBahasaConfig("Fakultas"));

        if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
            Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
                    Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                    CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
        }

        Common.pilihJurusan(jurusan,
                asrama.getJurusan() == null ? tbmuser.ambilJurusan() : asrama.getJurusan());
        jurusan.setWidth("100%");
        fb.addRow("Program Studi", jurusan,
                "Kosongkan " + Common.getBahasaConfig("Jurusan")
                        + " jika asrama ini berlaku untuk semua " + Common.getBahasaConfig("Jurusan"));

        tahunAngkatan = new Decimalbox(
                asrama.getTahunAngkatan() == null ? null : new BigDecimal(asrama.getTahunAngkatan()));
        tahunAngkatan.setWidth("100%");
        fb.addRow("Tahun Angkatan", tahunAngkatan,
                "Kosongkan tahun angkatan jika asrama ini berlaku untuk semua tahun angkatan");

        keterangan = new Textbox(asrama.getKeterangan() == null ? "" : asrama.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
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
     * Memvalidasi (nama wajib isi, nama tidak duplikat) dan menyimpan (create-or-update) entitas
     * asrama dari isian form, termasuk cakupan fakultas/jurusan/tahun angkatan opsional.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
     *         galat sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Asrama",
            		"Kolom Nama Asrama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Asrama.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaAsrama()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Asrama",
            		"Nama Asrama sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama asrama yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Asrama entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Asrama) session.load(Asrama.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
                ? null : jurusan.getSelectedItem().getValue()));
        entity.setTahunAngkatan(tahunAngkatan.getValue() == null ? null : tahunAngkatan.getValue().intValue());
        entity.setFakultas((Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
                ? null : fakultas.getSelectedItem().getValue()));
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** Mengecek apakah nama pada form sudah dipakai asrama lain (di luar entitas yang sedang diedit). */
    public Boolean checkNamaAsrama() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Asrama.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Custom download button ========================

    /**
     * Membangun tombol toolbar yang, saat diklik, menghasilkan berkas Excel berisi daftar
     * mahasiswa penghuni tiap asrama aktif (satu sheet per asrama, kolom No./NIM/Nama) —
     * dijalankan di thread terpisah dengan indikator sibuk (busy) dipantau lewat timer polling —
     * lalu menampilkan pratinjau spreadsheet dalam dialog modal beserta tombol unduh.
     *
     * @param buttonLabel label tombol toolbar
     * @param buttonImage path ikon tombol toolbar
     * @return tombol toolbar siap disisipkan
     */
    @SuppressWarnings("unchecked")
    public MyToolbarbuttonConfig cetakDataCustomButton(String buttonLabel, String buttonImage) {
        MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);
        toolbarbutton.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
                final Intbox intbox = new Intbox(10);
                Clients.showBusy(label.getValue());

                final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
                        + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
                        + ".xlsx");
                final File file;
                (file = new File(filename)).createNewFile();

                final Timer timer = new Timer(200);
                timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
                timer.setRepeats(true);
                timer.addEventListener("onTimer", new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        try {
                            Clients.showBusy(label.getValue());
                            if (label.getValue().trim().equalsIgnoreCase("-")) {
                                Clients.clearBusy();
                                timer.detach();
                            } else if (label.getValue().isEmpty()) {
                                Center center = new Center();
                                final MyWindow window = new MyWindow("Cetak Data", "none", true);
                                window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
                                window.setHeight("97%");
                                window.setWidth("90%");

                                Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
                                borderlayout.setParent(window);
                                ZkCompat.setFlex(center, true);
                                center.setParent(borderlayout);

                                Common.clear(center);
                                ais.ui.util.MySpreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
                                Common.clear(center);
                                spreadsheet.setParent(center);
                                spreadsheet.setWidth("100%");
                                spreadsheet.setHeight("100%");
                                spreadsheet.setSrc("../../tmp/" + file.getName());
                                spreadsheet.setMaxrows(intbox.getValue() + 1);
                                spreadsheet.setMaxcolumns(3);
                                ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

                                South south = new South();
                                south.setParent(borderlayout);

                                Toolbar toolbar = new Toolbar();
                                toolbar.setParent(south);
                                MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
                                cancel.setTooltiptext("Tutup");
                                cancel.addEventListener("onClick", new EventListener() {
                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        window.detach();
                                    }
                                });
                                cancel.setParent(toolbar);

                                MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
                                print.addEventListener("onClick", new EventListener() {
                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        try {
                                            Filedownload.save(new FileInputStream(file),
                                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                    file.getName());
                                        } catch (Exception e) {
                                            Common.tampilErrorJikaAdmin(e);
                                        }
                                    }
                                });
                                print.setParent(toolbar);

                                window.setVisible(true);
                                window.onModal();
                                Clients.clearBusy();
                                timer.detach();
                            }
                        } catch (Exception e) {
                            Clients.clearBusy();
                        }
                    }
                });
                timer.start();

                try {
                    Clients.showBusy(label.getValue());
                    final List<Asrama> asramaes = initCriteria(true)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Session session = HibernateUtil.currentSession();
                                XSSFWorkbook workbook = new XSSFWorkbook();
                                for (Asrama asrama : asramaes) {
                                    List<AsramaPunyaMahasiswa> data = session
                                            .createCriteria(AsramaPunyaMahasiswa.class)
                                            .add(Restrictions.eq("asrama", asrama))
                                            .createAlias("mahasiswa", "mahasiswa")
                                            .addOrder(Order.asc("mahasiswa.nim")).setMaxResults(1048576).list();
                                    intbox.setValue(data.size());

                                    XSSFSheet sheet = workbook.createSheet(asrama.getNama());
                                    sheet.setDefaultColumnWidth(20);
                                    int rowIndex = 0;

                                    XSSFRow rowhead = sheet.createRow((short) 0);
                                    rowhead.createCell(0).setCellValue("No.");
                                    rowhead.createCell(1).setCellValue("NIM");
                                    rowhead.createCell(2).setCellValue("Nama");

                                    for (AsramaPunyaMahasiswa o : data) {
                                        try {
                                            rowIndex++;
                                            if (o == null) continue;
                                            label.setValue("Sedang memproses data " + o.toString() + " ("
                                                    + Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
                                                    + " %)");
                                            XSSFRow row = sheet.createRow(rowIndex);
                                            row.createCell(0).setCellValue(rowIndex);
                                            row.createCell(1).setCellValue(o.getMahasiswa().getNim());
                                            row.createCell(2).setCellValue(o.getMahasiswa().getNama());
                                        } catch (Exception e) {
                                            Common.tampilErrorJikaAdmin(e);
                                        }
                                    }
                                    data.clear();
                                    data = null;
                                }
                                try {
                                    FileOutputStream fileOut = new FileOutputStream(filename);
                                    workbook.write(fileOut);
                                    fileOut.close();
                                } catch (IOException e) {
                                    Common.tampilErrorJikaAdmin(e);
                                }
                                label.setValue("");
                            } catch (Exception e) {
                                Common.tampilErrorJikaAdmin(e);
                                label.setValue("-");
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
            }
        });
        return toolbarbutton;
    }

    // ======================== Renderer ========================

    /** Perenderan satu baris tabel asrama: detail penghuni yang dapat diperluas (lewat {@link AsramaPunyaMahasiswaHelper}), nama (tautan riwayat revisi), cakupan fakultas/jurusan/tahun angkatan ("Semua" bila tidak dibatasi), keterangan, checkbox status aktif, jumlah penghuni, dan tombol edit/hapus. */
    class AsramaRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Asrama asrama = (Asrama) arg1;

            final MyDetail detail = new MyDetail();
            detail.setParent(arg0);
            detail.addEventListener("onOpen", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    Common.clear(detail);
                    if (detail.isOpen()) {
                        AsramaPunyaMahasiswaHelper detailperkuliahanHelper = new AsramaPunyaMahasiswaHelper();
                        detailperkuliahanHelper.display(asrama, detail, addWindow);
                    }
                }
            });

            RevisiHelper.createNewRevisi(Asrama.class, asrama, asrama.getNama()).setParent(arg0);
            new Label(asrama.getFakultas() == null ? "Semua" : asrama.getFakultas().getNama()).setParent(arg0);
            new Label(asrama.getJurusan() == null ? "Semua" : asrama.getJurusan().getNama()).setParent(arg0);
            new Label(asrama.getTahunAngkatan() == null ? "Semua" : asrama.getTahunAngkatan() + "").setParent(arg0);
            new Label(asrama.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(asrama.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    asrama.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(asrama);
                }
            });

            int count = ((Number) HibernateUtil.currentSession().createCriteria(AsramaPunyaMahasiswa.class)
                    .add(Restrictions.eq("asrama", asrama)).setProjection(Projections.rowCount()).uniqueResult())
                    .intValue();
            new Label(Common.numberFormat.get().format(count)).setParent(arg0);

            Hbox toolbar = new Hbox();
            Common.copyEditDeleteButtons(edit, delete, asrama, AsramaAction.this).setParent(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
