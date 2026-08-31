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
import ais.database.dao.PekerjaanOrangTuaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PekerjaanOrangTua;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Pekerjaan Orang Tua (referensi jenis pekerjaan orang tua/wali siswa),
 * dibangun di atas {@link GenericCrudAction}, dengan dukungan impor massal lewat unggah berkas
 * Excel (.xlsx) selain form tambah/ubah manual biasa.
 *
 * <p>
 * Pencarian daftar difilter berdasarkan kecocokan sebagian nama dan kode ({@code ilike
 * ANYWHERE}). Form simpan memvalidasi nama wajib isi dan tidak duplikat (dicek lewat
 * {@link #checkNamaPekerjaanOrangTua()}), menggunakan {@link PekerjaanOrangTuaDao} (bukan sesi
 * Hibernate langsung) untuk create/update. Unggahan Excel ({@link #prosesUpload(UploadEvent)})
 * dibaca baris demi baris (kolom id/nama/kode) dan disimpan sebagai create-or-update per baris,
 * berhenti membaca saat menemukan baris kosong pertama pada kolom nama. Baris tabel dirender
 * lewat {@link PekerjaanOrangTuaRenderer}, dengan checkbox status aktif yang langsung menyimpan
 * perubahan ke database saat diubah.
 * </p>
 */
public class PekerjaanOrangTuaAction extends GenericCrudAction<PekerjaanOrangTua> {

    private static final long serialVersionUID = 6641352157630711934L;

    // Form fields
    private Textbox nama;
    private Textbox kode;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PekerjaanOrangTua> getEntityClass() { return PekerjaanOrangTua.class; }

    @Override
    protected PekerjaanOrangTua createNewEntity() { return new PekerjaanOrangTua(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Pekerjaan Orang Tua"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "kode" };
    }

    /** Menambahkan tombol cetak data dan tombol unggah Excel ke toolbar setelah komponen ZUL selesai diinisialisasi, mengikuti hak akses pengguna saat ini. */
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

    /** Membangun kriteria pencarian daftar pekerjaan orang tua, difilter berdasarkan kecocokan sebagian nama dan/atau kode bila diisi. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PekerjaanOrangTua.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PekerjaanOrangTuaRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah (kode, nama, keterangan) beserta toolbar Batal/Simpan pada {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final PekerjaanOrangTua pekerjaanOrangTua) throws Exception {
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

        kode = new Textbox(pekerjaanOrangTua.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        nama = new Textbox(pekerjaanOrangTua.getNama() == null ? "" : pekerjaanOrangTua.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Pekerjaan Orang Tua *", nama);

        keterangan = new Textbox(pekerjaanOrangTua.getKeterangan() == null ? "" : pekerjaanOrangTua.getKeterangan());
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
     * Memvalidasi (nama wajib isi, nama tidak duplikat) dan menyimpan (create-or-update, lewat
     * {@link PekerjaanOrangTuaDao}) entitas pekerjaan orang tua dari isian form.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
     *         galat sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Pekerjaan Orang Tua",
            		"Kolom Pekerjaan Orang Tua belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Pekerjaan Orang Tua.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaPekerjaanOrangTua()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Pekerjaan Orang Tua",
            		"Pekerjaan Orang Tua sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan Pekerjaan Orang Tua yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        PekerjaanOrangTuaDao dao = DaoFactory.getInstance().getPekerjaanOrangTuaDao();
        PekerjaanOrangTua entity = currentEntity;
        if (entity.getId() != null) {
            entity = dao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (entity.getId() != null) {
            dao.update(entity);
        } else {
            dao.save(entity);
        }
        return true;
    }

    /** Mengecek apakah nama pada form sudah dipakai pekerjaan orang tua lain (di luar entitas yang sedang diedit). */
    public Boolean checkNamaPekerjaanOrangTua() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(PekerjaanOrangTua.class)
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
     * Mengimpor massal data Pekerjaan Orang Tua dari berkas Excel (.xlsx) yang diunggah:
     * menyimpannya sementara ke direktori {@code temp}, lalu membaca baris demi baris mulai
     * baris kedua (kolom 0=id, 1=nama, 2=kode); untuk setiap baris dengan nama terisi, entitas
     * di-create-or-update berdasarkan id (bila ada dan valid) dalam transaksi tersendiri per
     * baris. Berhenti membaca saat kolom nama pada suatu baris kosong. Menolak berkas yang bukan
     * berekstensi {@code .xlsx}.
     *
     * @param uploadEvent event unggah ZK berisi media berkas Excel
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
                    PekerjaanOrangTua obj = id == null || id.equals(-1L) ? null
                            : (PekerjaanOrangTua) sess.createCriteria(PekerjaanOrangTua.class)
                                    .add(Restrictions.idEq(id)).uniqueResult();
                    if (obj == null) obj = new PekerjaanOrangTua();
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

    /** Perenderan satu baris tabel pekerjaan orang tua: nama, kode, keterangan, checkbox status aktif (menyimpan langsung ke database saat diubah), dan tombol edit/hapus. */
    class PekerjaanOrangTuaRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PekerjaanOrangTua pekerjaanOrangTua = (PekerjaanOrangTua) arg1;

            new Label(pekerjaanOrangTua.getNama()).setParent(arg0);
            new Label(pekerjaanOrangTua.getKode()).setParent(arg0);
            new Label(pekerjaanOrangTua.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(pekerjaanOrangTua.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    pekerjaanOrangTua.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(pekerjaanOrangTua);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, pekerjaanOrangTua, PekerjaanOrangTuaAction.this).setParent(arg0);
        }
    }
}
