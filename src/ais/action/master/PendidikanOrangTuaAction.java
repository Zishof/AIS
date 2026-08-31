package ais.action.master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendidikanOrangTuaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PendidikanOrangTua;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Pendidikan Orang Tua (jenjang pendidikan terakhir ayah/ibu siswa/mahasiswa,
 * dipakai pada data PMB/kesiswaan). Memperluas {@link GenericCrudAction} untuk mewarisi kerangka
 * baku cari/tambah/ubah/hapus, ditambah aksi cetak dan unggah massal via file Excel
 * (.xlsx — lihat {@link #prosesUpload}). Kelas ini mengisi bagian spesifik entitas: kriteria
 * pencarian (nama + kode), form input (nama + kode + keterangan), validasi nama wajib dan tidak
 * boleh duplikat, serta renderer baris. Penyimpanan didelegasikan ke {@link PendidikanOrangTuaDao}.
 *
 * <p>
 * <b>Catatan keamanan:</b> {@link #prosesUpload(UploadEvent)} menulis file Excel yang diunggah ke
 * disk memakai nama file asli dari klien tanpa sanitasi ({@code "/temp/" + media.getName()}) —
 * berpotensi path traversal bila nama file memuat komponen path (mis. {@code "../"}).
 * </p>
 */
public class PendidikanOrangTuaAction extends GenericCrudAction<PendidikanOrangTua> {

    private static final long serialVersionUID = -1036939833372046390L;

    // Form fields
    private Textbox nama;
    private Textbox kode;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PendidikanOrangTua> getEntityClass() { return PendidikanOrangTua.class; }

    @Override
    protected PendidikanOrangTua createNewEntity() { return new PendidikanOrangTua(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Pendidikan Orang Tua"; }

    /** Kolom yang disertakan pada template unduh/unggah massal data pendidikan orang tua. */
    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "kode" };
    }

    /** Menambahkan tombol cetak dan unggah massal (Excel) di sebelah tombol tambah, mengikuti hak akses tambah/ubah/hapus pengguna. */
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "kode");
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
                prosesUpload((UploadEvent) event);
            }
        });
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    /** Menyusun kriteria pencarian {@link PendidikanOrangTua} berdasarkan nama dan kode, diurutkan berdasarkan nama bila diminta. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PendidikanOrangTua.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Menyediakan renderer baris grid {@link PendidikanOrangTuaRenderer} untuk daftar hasil pencarian. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new PendidikanOrangTuaRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah pendidikan orang tua (field nama + kode + keterangan) beserta tombol batal/simpan pada jendela dialog. */
    @Override
    protected void buildFormContent(MyWindow window, final PendidikanOrangTua pendidikanOrangTua) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

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

        nama = new Textbox(pendidikanOrangTua.getNama() == null ? "" : pendidikanOrangTua.getNama());
        nama.setWidth("100%");
        fb.addRow("Pendidikan Orang Tua *", nama);

        kode = new Textbox(pendidikanOrangTua.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        keterangan = new Textbox(pendidikanOrangTua.getKeterangan() == null ? "" : pendidikanOrangTua.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
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

    /**
     * Memvalidasi lalu menyimpan data pendidikan orang tua dari form: menolak bila nama kosong atau
     * sudah terdaftar pada baris lain; jika lolos menyimpan/memperbarui entitas lewat
     * {@link PendidikanOrangTuaDao} dan mengembalikan {@code true}.
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan DAO saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Pendidikan Orang Tua",
            		"Kolom Pendidikan Orang Tua belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Pendidikan Orang Tua.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaPendidikanOrangTua()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Pendidikan Orang Tua",
            		"Pendidikan Orang Tua sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan Pendidikan Orang Tua yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        PendidikanOrangTuaDao dao = DaoFactory.getInstance().getPendidikanOrangTuaDao();
        PendidikanOrangTua entity = currentEntity;
        if (entity.getId() != null) {
            entity = dao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue().trim());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (entity.getId() != null) {
            dao.update(entity);
        } else {
            dao.save(entity);
        }
        return true;
    }

    /**
     * Memeriksa apakah nama pendidikan orang tua yang diisi di form sudah dipakai baris lain
     * (mengecualikan baris yang sedang diedit sendiri).
     *
     * @return {@code true} bila nama sudah terpakai baris lain, {@code false} bila belum
     */
    public Boolean checkNamaPendidikanOrangTua() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(PendidikanOrangTua.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Upload logic ========================

    /**
     * Memproses unggahan massal data pendidikan orang tua dari file Excel (.xlsx): membaca setiap
     * baris mulai baris kedua (baris pertama header), memetakan kolom id/nama/kode, dan
     * menyimpan/memperbarui entitas per baris (id {@code -1} atau kosong berarti baris baru).
     * Berhenti membaca lebih awal saat kolom nama pada suatu baris kosong. Setiap baris disimpan
     * dalam transaksi Hibernate tersendiri sehingga kegagalan satu baris tidak membatalkan baris lain.
     *
     * @param uploadEvent event unggah ZK berisi file Excel yang diunggah pengguna
     * @throws Exception diteruskan apa adanya dari kegagalan I/O atau parsing workbook
     */
    private void prosesUpload(UploadEvent uploadEvent) throws Exception {
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
        FileOutputStream fos = new FileOutputStream(file);
        int c;
        while ((c = inputStream.read()) != -1) fos.write(c);
        fos.close();
        inputStream.close();

        XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
        XSSFSheet sheet = workbook.getSheetAt(0);
        String peringatan = "";
        for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
            try {
                if (Common.getSheetContentAsString(sheet, 1, i) == null) break;
                Session sess = HibernateUtil.currentNativeSession();
                String namaVal = Common.getSheetContentAsString(sheet, 1, i);
                String kodeVal = Common.getSheetContentAsString(sheet, 2, i);
                if (namaVal != null && !namaVal.trim().isEmpty()) {
                    Long id = Common.getSheetContentAsLong(sheet, 0, i);
                    PendidikanOrangTua obj = id == null || id.equals(-1L) ? null
                            : (PendidikanOrangTua) sess.createCriteria(PendidikanOrangTua.class)
                                    .add(Restrictions.idEq(id)).uniqueResult();
                    if (obj == null) obj = new PendidikanOrangTua();
                    obj.setNama(namaVal);
                    obj.setKode(kodeVal);
                    sess.getTransaction().begin();
                    sess.saveOrUpdate(obj);
                    sess.getTransaction().commit();
                }
                HibernateUtil.closeSession();
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        MyMessageboxConfig.show(
                "Upload data berhasil dilakukan." + (peringatan.isEmpty() ? "" : "\n" + peringatan),
                "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
                new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception { onSearchDefault(null); }
                });
    }

    // ======================== Renderer ========================

    /** Renderer baris grid daftar pendidikan orang tua: kolom nama, kode, keterangan, dan tombol edit/hapus. */
    class PendidikanOrangTuaRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PendidikanOrangTua pendidikanOrangTua = (PendidikanOrangTua) arg1;

            new Label(pendidikanOrangTua.getNama()).setParent(arg0);
            new Label(pendidikanOrangTua.getKode()).setParent(arg0);
            new Label(pendidikanOrangTua.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, pendidikanOrangTua, PendidikanOrangTuaAction.this).setParent(arg0);
        }
    }
}
