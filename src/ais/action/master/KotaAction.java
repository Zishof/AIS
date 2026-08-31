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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.KotaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
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
 * Pendataan Kota/Kabupaten. Digunakan sebagai referensi alamat pada biodata
 * mahasiswa, pegawai, dan dosen. Setiap kota berelasi ke satu Propinsi.
 *
 * Tersedia onAddExternal untuk membuka form kota dari modul lain (mis. PropinsiAction).
 */
public class KotaAction extends GenericCrudAction<Kota> {

    private static final long serialVersionUID = 2119481131653081288L;

    // ZK auto-wired — nama berbeda dari searchnama base; hanya ada field ini di ZUL
    private Textbox searchnamakota;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox namakota;
    private Combobox namapropinsi;

    // eventListener dipakai saat dipanggil via onAddExternal
    private EventListener eventListener = null;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Kota> getEntityClass() {
        return Kota.class;
    }

    @Override
    protected Kota createNewEntity() {
        return new Kota();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Kota";
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        // Download (cetakData) pakai standard button; upload menggunakan logik khusus
        // karena perlu memanggil simpanWilayah() setelah tiap baris diproses.
        final String[] cols = new String[] { "id", "kode", "nama", "propinsi" };
        MyToolbarbuttonConfig cetak = Common.cetakData(this, cols[0], cols[1], cols[2], cols[3]);
        if (add != null) {
        add.getParent().appendChild(cetak);
        }

        MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
                "Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        upload.setUpload(Common.ukuranFileUpload());
        upload.addEventListener("onUpload", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                prosesUploadKota((UploadEvent) event);
            }
        });
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Kota.class);
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnamakota == null || searchnamakota.getValue().trim().isEmpty()
                ? Restrictions.ilike("nama", "", MatchMode.ANYWHERE)
                : Restrictions.ilike("nama", searchnamakota.getValue(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KotaRenderer();
    }

    // ======================== External open (dipanggil dari modul lain) ========================

    public static void onAddExternal(EventListener listener, Kota kota) throws Exception {
        KotaAction action = new KotaAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(action.addWindow);
        action.addWindow.setHeight("250px");
        action.addWindow.setWidth("500px");
        action.addWindow.setClosable(true);
        action.init(kota);
        if (kota != null && kota.getPropinsi() != null) {
            Common.selectComboItem(true, action.namapropinsi, kota.getPropinsi());
            action.namapropinsi.setDisabled(true);
        }
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Kota kota) throws Exception {
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

        namakota = new Textbox(kota.getNama() == null ? "" : kota.getNama());
        namakota.setWidth("100%");
        fb.addRow("Nama Kota/Kabupaten *", namakota);

        Common.insertCombo(namapropinsi = new Combobox(), "nama", Propinsi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(namapropinsi, kota.getPropinsi());
        namapropinsi.setWidth("100%");
        namapropinsi.setReadonly(true);
        fb.addRow("Propinsi *", namapropinsi);

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
                if (eventListener != null) {
                    eventListener.onEvent(new Event("", addWindow, currentEntity));
                }
            }
        });
        save.setParent(toolbar);

        borderlayout.setParent(window);
    }

    // ======================== Save logic ========================

    public boolean onSave(Event event) throws Exception {
        if (namakota.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kota/Kabupaten",
            		"Kolom Nama Kota/Kabupaten belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Kota/Kabupaten.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (namapropinsi.getSelectedItem() == null || namapropinsi.getSelectedItem().getValue() == null) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Propinsi",
            		"Kolom Propinsi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Propinsi.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaKota()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kota/Kabupaten",
            		"Nama Kota/Kabupaten sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama kota/kabupaten yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        KotaDao kotaDao = DaoFactory.getInstance().getKotaDao();
        Kota entity = currentEntity;
        if (entity.getId() != null) {
            entity = kotaDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setNama(namakota.getValue());
        entity.setPropinsi((Propinsi) (namapropinsi.getSelectedItem() == null
                ? null : namapropinsi.getSelectedItem().getValue()));
        if (entity.getId() != null) {
            kotaDao.update(entity);
        } else {
            kotaDao.save(entity);
        }
        entity.simpanWilayah();
        return true;
    }

    private boolean checkNamaKota() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Kota.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", namakota.getValue().trim()).ignoreCase())
                .add(Restrictions.eq("propinsi", namapropinsi.getSelectedItem().getValue()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Upload logic (custom: simpanWilayah setelah tiap baris) ========================

    private void prosesUploadKota(UploadEvent uploadEvent) throws Exception {
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
        for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
            try {
                if (Common.getSheetContentAsString(sheet, 1, i) == null) break;
                Session sess = HibernateUtil.currentNativeSession();
                String nama = Common.getSheetContentAsString(sheet, 1, i);
                Propinsi propinsi = (Propinsi) Common.getSheetContentAsObject(sheet, 2, i, Propinsi.class);
                if (nama != null && !nama.trim().isEmpty() && propinsi != null) {
                    Long id = Common.getSheetContentAsLong(sheet, 0, i);
                    Kota kota = id == null || id.equals(-1L) ? null
                            : (Kota) sess.createCriteria(Kota.class).add(Restrictions.idEq(id)).uniqueResult();
                    if (kota == null) kota = new Kota();
                    kota.setNama(nama);
                    kota.setPropinsi(propinsi);
                    sess.getTransaction().begin();
                    sess.saveOrUpdate(kota);
                    sess.getTransaction().commit();
                    if (sess.isOpen()) { sess.disconnect(); sess.close(); }
                    HibernateUtil.closeSession();
                    try { kota.simpanWilayah(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/KotaAction.java:307");}
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        MyMessageboxConfig.show("Upload data berhasil dilakukan.", "Pemberitahuan",
                MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
                new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception { onSearchDefault(null); }
                });
    }

    // ======================== Renderer ========================

    /**
     * Renderer lokal untuk layar/komponen {@link KotaAction}. Kelas ini menerjemahkan satu item data menjadi baris
     * atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link KotaAction} dan dapat mengakses state kelas
     * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see KotaAction
     */
    class KotaRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Kota kota = (Kota) arg1;

            RevisiHelper.createNewRevisi(Kota.class, kota, kota.getNama()).setParent(arg0);
            new Label(kota.getKode()).setParent(arg0);
            new Label(kota.getPropinsi() == null ? "" : kota.getPropinsi().getNama()).setParent(arg0);

            kota.simpanWilayah();

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(kota.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    kota.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(kota);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, kota, KotaAction.this).setParent(arg0);
        }
    }
}
