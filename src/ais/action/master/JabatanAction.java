package ais.action.master;

import java.math.BigDecimal;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.employ.JabatanFungsionalTreeAction;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.DetailJabatanDosenHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.JabatanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.payroll.KodeTunjangan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Pendataan Jabatan. Digunakan pada data kepegawaian dan penghitungan
 * ekuivalensi SKS. Jabatan dapat dikaitkan dengan item tunjangan (payroll).
 */
public class JabatanAction extends GenericCrudAction<Jabatan> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Helper untuk detail dosen dalam renderer
    private final DetailJabatanDosenHelper detailJabatanDosenHelper = new DetailJabatanDosenHelper();

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox nama;
    private Combobox jenisJabatan;
    private Textbox keterangan;
    private Decimalbox eqSks;
    private JSONArray array;
    private Row rowTunjangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Jabatan> getEntityClass() {
        return Jabatan.class;
    }

    @Override
    protected Jabatan createNewEntity() {
        return new Jabatan();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Jabatan";
    }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "keterangan", "aktif", "ptSendiri", "eq_sks", "jenisJabatan" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        // jenisJabatan diinisialisasi di sini untuk potensi penggunaan di masa depan
        jenisJabatan = new Combobox();
        MyComboitemConfig comboitem = new MyComboitemConfig("Fungsional");
        comboitem.setValue("fungsional");
        jenisJabatan.appendChild(comboitem);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Jabatan.class);
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new JabatanRenderer();
    }

    // ======================== Form content ========================

    @SuppressWarnings("deprecation")
    @Override
    protected void buildFormContent(MyWindow window, final Jabatan jabatan) throws Exception {
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

        nama = new Textbox(jabatan.getNama() == null ? "" : jabatan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Jabatan *", nama);

        keterangan = new Textbox(jabatan.getKeterangan() == null ? "" : jabatan.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        eqSks = new Decimalbox(new BigDecimal(jabatan.getEq_sks() == null ? 0 : jabatan.getEq_sks()));
        eqSks.setWidth("100%");
        fb.addRow("Ekuivalensi SKS", eqSks);

        fb.addSectionHeader("TUNJANGAN");

        MyFormRow rowTunjanganLabel = new MyFormRow();
        ZkCompat.setSpans(rowTunjanganLabel, "2");
        rowTunjanganLabel.setParent(rows);

        MyFormRow rowTunjanganContent = new MyFormRow();
        ZkCompat.setSpans(rowTunjanganContent, "2");
        rowTunjanganContent.setParent(rows);
        array = new JSONArray(jabatan.getTunjangans());
        rowTunjangan = Common.tampilanScroll1(rowTunjanganContent);
        JabatanFungsionalTreeAction.reloadTunjangan(rowTunjangan, array, "TUNJ_JAB", KodeTunjangan.JABATAN_LAIN);

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
            PesanFormalHelper.tampilkanGagal("penyimpanan data Jabatan",
            		"Kolom Nama Jabatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Jabatan.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaJabatan()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Jabatan",
            		"Nama Jabatan sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama jabatan yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        JabatanDao jabatanDao = DaoFactory.getInstance().getJabatanDao();
        Jabatan entity = currentEntity;
        if (entity.getId() != null) {
            entity = jabatanDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setEq_sks(eqSks.getValue() == null ? 0 : Integer.parseInt(eqSks.getValue().toString()));
        entity.setTunjangans(array.toString());
        if (entity.getId() != null) {
            jabatanDao.update(entity);
        } else {
            jabatanDao.save(entity);
        }
        return true;
    }

    private boolean checkNamaJabatan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Jabatan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class JabatanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Jabatan jabatan = (Jabatan) arg1;

            final MyDetail mydetail = new MyDetail();
            mydetail.setParent(arg0);
            mydetail.addEventListener("onOpen", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    Common.clear(mydetail);
                    if (mydetail.isOpen()) {
                        detailJabatanDosenHelper.displayDetailDosen(jabatan, mydetail);
                    }
                }
            });

            new Label(jabatan.getNama()).setParent(arg0);
            new Label(jabatan.getKeterangan()).setParent(arg0);
            new Label(jabatan.getEq_sks() == null ? "" : jabatan.getEq_sks().toString()).setParent(arg0);

            final MyCheckboxConfig ptSendiri = new MyCheckboxConfig("Jbt di PT Sendiri");
            ptSendiri.setDisabled(!edit);
            ptSendiri.setChecked(jabatan.getPtSendiri());
            ptSendiri.setParent(arg0);
            ptSendiri.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jabatan.setPtSendiri(ptSendiri.isChecked());
                    Common.refreshSaveOrUpdate(jabatan);
                }
            });

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(jabatan.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jabatan.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(jabatan);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, jabatan, JabatanAction.this).setParent(arg0);
        }
    }
}
