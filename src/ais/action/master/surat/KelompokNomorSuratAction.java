package ais.action.master.surat;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.surat.KelompokNomorSurat;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class KelompokNomorSuratAction extends GenericCrudAction<KelompokNomorSurat> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox nama;
    private Textbox userid;
    private Textbox grupUserid;
    private Longbox mulaiUrutanKe;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<KelompokNomorSurat> getEntityClass() { return KelompokNomorSurat.class; }

    @Override
    protected KelompokNomorSurat createNewEntity() { return new KelompokNomorSurat(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Kelompok Nomor Surat"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "userid", "grupUserid", "mulaiUrutanKe", "keterangan" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokNomorSurat.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KelompokNomorSurat.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KelompokNomorSuratRenderer();
    }

    // ======================== Static utility ========================

    public static void checkKelompok(Combobox kelompokNomorSurat) {
        Tbmuser tbmuser = Common.getCurrentUser();
        if (tbmuser != null && tbmuser.getUserId() != null) {
            for (Object o : kelompokNomorSurat.getChildren()) {
                Comboitem comboitem = (Comboitem) o;
                KelompokNomorSurat kelompok = (KelompokNomorSurat) comboitem.getValue();
                if (kelompok != null) {
                    if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
                        for (String s : kelompok.getGrupUserid().split(";")) {
                            if (s.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId().trim())) {
                                Common.selectComboItem(kelompokNomorSurat, kelompok);
                                kelompokNomorSurat.setDisabled(true);
                                break;
                            }
                        }
                    }
                    for (String s : kelompok.getUserid().split(";")) {
                        if (s.trim().equalsIgnoreCase(tbmuser.getUserId().trim())) {
                            Common.selectComboItem(kelompokNomorSurat, kelompok);
                            kelompokNomorSurat.setDisabled(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final KelompokNomorSurat kelompokNomorSurat) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center with card
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

        nama = new Textbox(kelompokNomorSurat.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Kelompok Nomor Surat *", nama);

        mulaiUrutanKe = new Longbox(kelompokNomorSurat.getMulaiUrutanKe());
        mulaiUrutanKe.setWidth("100%");
        fb.addRow("Mulai Urutan Ke", mulaiUrutanKe);

        userid = new Textbox(kelompokNomorSurat.getUserid());
        userid.setWidth("100%");
        userid.setRows(2);
        fb.addRow("Pengguna", userid,
                "Jika pengguna lebih dari satu, pisah dengan tanda semikolon (;)");

        grupUserid = new Textbox(kelompokNomorSurat.getGrupUserid());
        grupUserid.setWidth("100%");
        grupUserid.setRows(2);
        fb.addRow("Grup Pengguna", grupUserid,
                "Jika grup pengguna lebih dari satu, pisah dengan tanda semikolon (;)");

        keterangan = new Textbox(kelompokNomorSurat.getKeterangan());
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

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Kelompok Nomor Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Kelompok; (2) isikan nama kelompok nomor surat secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaKelompokNomorSurat()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Kelompok Nomor Surat sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar kelompok nomor surat yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        KelompokNomorSurat entity = currentEntity;
        if (entity.getId() != null) {
            entity = (KelompokNomorSurat) session.load(KelompokNomorSurat.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setGrupUserid(grupUserid.getValue().trim());
        entity.setUserid(userid.getValue().trim());
        entity.setKeterangan(keterangan.getValue());
        entity.setMulaiUrutanKe(mulaiUrutanKe.getValue());
        Common.refreshUpdate(session, entity);
        return true;
    }

    public Boolean checkNamaKelompokNomorSurat() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(KelompokNomorSurat.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class KelompokNomorSuratRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final KelompokNomorSurat kelompokNomorSurat = (KelompokNomorSurat) arg1;

            RevisiHelper.createNewRevisi(KelompokNomorSurat.class, kelompokNomorSurat,
                    kelompokNomorSurat.getNama()).setParent(arg0);
            new Label(kelompokNomorSurat.getUserid()).setParent(arg0);
            new Label(kelompokNomorSurat.getGrupUserid()).setParent(arg0);
            new Label(kelompokNomorSurat.getMulaiUrutanKe() + "").setParent(arg0);
            new Label(kelompokNomorSurat.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, kelompokNomorSurat, KelompokNomorSuratAction.this).setParent(arg0);
        }
    }
}
