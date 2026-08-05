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
import ais.database.dao.PropinsiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Negara;
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
 * Pendataan Propinsi. Digunakan sebagai referensi wilayah untuk Kota/Kabupaten
 * dan alamat pada biodata mahasiswa, pegawai, dan dosen.
 *
 * Tersedia onAddExternal untuk membuka form propinsi dari modul lain (mis. NegaraAction).
 */
public class PropinsiAction extends GenericCrudAction<Propinsi> {

    private static final long serialVersionUID = -6945668881632801032L;

    // ZK auto-wired — nama berbeda dari searchnama base; hanya ada field ini di ZUL
    private Textbox searchnamapropinsi;
    private Textbox searchkode;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox namapropinsi;
    private Textbox kode;
    private Combobox namanegara;

    // eventListener dipakai saat dipanggil via onAddExternal
    private EventListener eventListener = null;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Propinsi> getEntityClass() {
        return Propinsi.class;
    }

    @Override
    protected Propinsi createNewEntity() {
        return new Propinsi();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Propinsi";
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        // Cetakdata standard; upload menggunakan logik khusus (simpanWilayah tiap baris).
        MyToolbarbuttonConfig cetak = Common.cetakData(this, "id", "nama", "negara", "kode");
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
                prosesUploadPropinsi((UploadEvent) event);
            }
        });
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Propinsi.class);
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnamapropinsi == null || searchnamapropinsi.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnamapropinsi.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PropinsiRenderer();
    }

    // ======================== External open (dipanggil dari modul lain) ========================

    public static void onAddExternal(EventListener listener, Propinsi propinsi) throws Exception {
        PropinsiAction action = new PropinsiAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(action.addWindow);
        action.addWindow.setHeight("250px");
        action.addWindow.setWidth("500px");
        action.addWindow.setClosable(true);
        action.init(propinsi);
        if (propinsi != null && propinsi.getNegara() != null) {
            Common.selectComboItem(true, action.namanegara, propinsi.getNegara());
            action.namanegara.setDisabled(true);
        }
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Propinsi propinsi) throws Exception {
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

        namapropinsi = new Textbox(propinsi.getNama() == null ? "" : propinsi.getNama());
        namapropinsi.setWidth("100%");
        fb.addRow("Nama Propinsi *", namapropinsi);

        kode = new Textbox(propinsi.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        Common.insertCombo(namanegara = new Combobox(), "namaNegara", Negara.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(namanegara, propinsi.getNegara());
        namanegara.setWidth("100%");
        namanegara.setReadonly(true);
        fb.addRow("Negara *", namanegara);

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
        if (namapropinsi.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Propinsi",
            		"Kolom Nama Propinsi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Propinsi.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (namanegara.getSelectedItem() == null || namanegara.getSelectedItem().getValue() == null) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Negara",
            		"Kolom Nama Negara belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Negara.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaPropinsi()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Propinsi",
            		"Nama Propinsi sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama propinsi yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        PropinsiDao propinsiDao = DaoFactory.getInstance().getPropinsiDao();
        Propinsi entity = currentEntity;
        if (entity.getId() != null) {
            entity = propinsiDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setNama(namapropinsi.getValue());
        entity.setNegara((Negara) (namanegara.getSelectedItem() == null
                ? null : namanegara.getSelectedItem().getValue()));
        entity.setKode(kode.getValue());
        if (entity.getId() != null) {
            propinsiDao.update(entity);
        } else {
            propinsiDao.save(entity);
        }
        entity.simpanWilayah();
        return true;
    }

    private boolean checkNamaPropinsi() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Propinsi.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", namapropinsi.getValue().trim()))
                .add(Restrictions.eq("negara", namanegara.getSelectedItem().getValue()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Upload logic (custom: simpanWilayah setelah tiap baris) ========================

    private void prosesUploadPropinsi(UploadEvent uploadEvent) throws Exception {
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
                Negara negara = (Negara) Common.getSheetContentAsObject(sheet, 2, i, Negara.class,
                        Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                String kodePropinsi = Common.getSheetContentAsString(sheet, 3, i);
                if (nama != null && !nama.trim().isEmpty() && negara != null) {
                    Long id = Common.getSheetContentAsLong(sheet, 0, i);
                    Propinsi prop = id == null || id.equals(-1L) ? null
                            : (Propinsi) sess.createCriteria(Propinsi.class).add(Restrictions.idEq(id)).uniqueResult();
                    if (prop == null) prop = new Propinsi();
                    prop.setNama(nama);
                    prop.setNegara(negara);
                    prop.setKode(kodePropinsi);
                    sess.getTransaction().begin();
                    sess.saveOrUpdate(prop);
                    sess.getTransaction().commit();
                    HibernateUtil.closeSession();
                    try { prop.simpanWilayah(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PropinsiAction.java:317");}
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

    class PropinsiRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Propinsi propinsi = (Propinsi) arg1;

            RevisiHelper.createNewRevisi(Propinsi.class, propinsi, propinsi.getNama()).setParent(arg0);
            new Label(propinsi.getNegara() == null ? "" : propinsi.getNegara().getNamaNegara()).setParent(arg0);
            new Label(propinsi.getKode()).setParent(arg0);

            propinsi.simpanWilayah();

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(propinsi.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    propinsi.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(propinsi);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, propinsi, PropinsiAction.this).setParent(arg0);
        }
    }
}
