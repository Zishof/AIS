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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.NegaraDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Negara;
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

public class NegaraAction extends GenericCrudAction<Negara> {

    private static final long serialVersionUID = 3786091220301468178L;

    // ZK auto-wired — nama berbeda dari searchnama base
    private Textbox searchnamanegara;
    private Textbox searchkode;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox namanegara;
    private Textbox kode;

    // eventListener dipakai saat dipanggil via onAddExternal
    private EventListener eventListener = null;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Negara> getEntityClass() { return Negara.class; }

    @Override
    protected Negara createNewEntity() { return new Negara(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Negara"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "namaNegara", "kode", "aktif" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "namaNegara", "kode", "aktif");
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
                prosesUploadNegara((UploadEvent) event);
            }
        });
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Negara.class);
        if (order) criteria.addOrder(Order.asc("namaNegara"));
        criteria.add(searchnamanegara == null || searchnamanegara.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("namaNegara", searchnamanegara.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new NegaraRenderer();
    }

    // ======================== External open (dipanggil dari modul lain) ========================

    public static void onAddExternal(EventListener listener, Negara negara) throws Exception {
        NegaraAction action = new NegaraAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(action.addWindow);
        action.addWindow.setHeight("250px");
        action.addWindow.setWidth("500px");
        action.addWindow.setClosable(true);
        action.init(negara);
        action.addWindow.setVisible(true);
        action.addWindow.onModal();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Negara negara) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

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

        kode = new Textbox(negara.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Negara *", kode);

        namanegara = new Textbox(negara.getNamaNegara());
        namanegara.setWidth("100%");
        fb.addRow("Nama Negara *", namanegara);

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
        if (kode.getValue().trim().equals("")) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Negara",
            		"Kolom Kode Negara belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Kode Negara.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (namanegara.getValue().trim().equals("")) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Negara",
            		"Kolom Nama Negara belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Negara.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkKodeNegara()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Negara",
            		"Kode Negara sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan Kode Negara yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        if (checkNamaNegara()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Negara",
            		"Nama Negara sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama negara yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        NegaraDao negaraDao = DaoFactory.getInstance().getNegaraDao();
        Negara entity = currentEntity;
        if (entity.getId() != null) {
            entity = negaraDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setNamaNegara(namanegara.getValue());
        entity.setKode(kode.getValue().trim());
        if (entity.getId() != null) {
            negaraDao.update(entity);
        } else {
            negaraDao.save(entity);
        }
        return true;
    }

    public Boolean checkKodeNegara() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Negara.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    public Boolean checkNamaNegara() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Negara.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("namaNegara", namanegara.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Upload logic ========================

    private void prosesUploadNegara(UploadEvent uploadEvent) throws Exception {
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
                String nama = Common.getSheetContentAsString(sheet, 1, i);
                String kodeRow = Common.getSheetContentAsString(sheet, 2, i);
                if (nama != null && !nama.trim().isEmpty()) {
                    Long id = Common.getSheetContentAsLong(sheet, 0, i);
                    Negara negara = id == null || id.equals(-1L) ? null
                            : (Negara) sess.createCriteria(Negara.class).add(Restrictions.idEq(id)).uniqueResult();
                    if (negara == null) negara = new Negara();
                    negara.setNamaNegara(nama);
                    negara.setKode(kodeRow);
                    sess.getTransaction().begin();
                    sess.saveOrUpdate(negara);
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

    class NegaraRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Negara negara = (Negara) arg1;

            new Label(negara.getNamaNegara()).setParent(arg0);
            new Label(negara.getKode()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(negara.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    negara.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(negara);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, negara, NegaraAction.this).setParent(arg0);
        }
    }
}
