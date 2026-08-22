package ais.action.master.surat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.surat.util.SuratUtil;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.OpsiSuratMasukDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.surat.OpsiSuratMasuk;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class OpsiSuratMasukAction extends GenericCrudAction<OpsiSuratMasuk> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;
    private MyCheckboxConfig hanyaBoleh;
    private Textbox jenisPengguna;
    private Textbox usernamePengguna;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<OpsiSuratMasuk> getEntityClass() { return OpsiSuratMasuk.class; }

    @Override
    protected OpsiSuratMasuk createNewEntity() { return new OpsiSuratMasuk(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Opsi Surat Masuk"; }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        @SuppressWarnings("unused")
        OpsiSuratMasuk balas = SuratUtil.balas;
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(OpsiSuratMasuk.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new OpsiSuratMasukRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final OpsiSuratMasuk opsiSuratMasuk) throws Exception {
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

        nama = new Textbox(opsiSuratMasuk.getNama() == null ? "" : opsiSuratMasuk.getNama());
        nama.setWidth("100%");
        fb.addRow("Opsi Surat Masuk", nama);

        hanyaBoleh = new MyCheckboxConfig(
                "Dipilih oleh jenis pengguna (id role), jika tidak dipilih oleh id penguna (username)");
        hanyaBoleh.setChecked(opsiSuratMasuk.getJenisPengguna() != null);
        fb.addRow("", hanyaBoleh);

        jenisPengguna = new Textbox(opsiSuratMasuk.getJenisPengguna());
        jenisPengguna.setWidth("100%");
        jenisPengguna.setRows(2);
        fb.addRow("Hanya boleh dipilih oleh jenis pengguna", jenisPengguna);

        final Row keteranganJenis = Common.initKeterangan(rows,
                "Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua jenis pengguna");

        usernamePengguna = new Textbox(opsiSuratMasuk.getUsernamePengguna());
        usernamePengguna.setWidth("100%");
        usernamePengguna.setRows(2);
        fb.addRow("Diajukan oleh username pengguna", usernamePengguna);

        MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
                "/img/user_male_add.png");
        final MyFormRow rowAmbilPengguna = new MyFormRow();
        rowAmbilPengguna.setParent(rows);
        rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowAmbilPengguna.appendChild(toolbarbutton);
        toolbarbutton.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
                ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
                ambil.setEventListener(new EventListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
                        if (tbmusers != null && tbmusers.size() != 0) {
                            for (Tbmuser tbmuser : tbmusers) {
                                usernamePengguna.setValue(usernamePengguna.getValue()
                                        + (usernamePengguna.getValue().isEmpty()
                                                ? tbmuser.getUserId() : "," + tbmuser.getUserId()));
                            }
                        }
                    }
                });
                ambil.setWidth("850px");
                ambil.setHeight("97%");
                ambil.setVisible(true);
                ambil.onModal();
            }
        });

        final Row keteranganUsername = Common.initKeterangan(rows,
                "Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");

        EventListener toggleListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                jenisPengguna.getParent().setVisible(hanyaBoleh.isChecked());
                usernamePengguna.getParent().setVisible(!hanyaBoleh.isChecked());
                rowAmbilPengguna.setVisible(!hanyaBoleh.isChecked());
                keteranganUsername.setVisible(!hanyaBoleh.isChecked());
                keteranganJenis.setVisible(hanyaBoleh.isChecked());
            }
        };
        hanyaBoleh.addEventListener("onClick", toggleListener);
        try { toggleListener.onEvent(null); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }

        keterangan = new Textbox(opsiSuratMasuk.getKeterangan() == null ? "" : opsiSuratMasuk.getKeterangan());
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
            MyMessageboxConfig.show("Mohon maaf, Nama Opsi Surat Masuk belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Opsi; (2) isikan nama opsi surat masuk secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaOpsiSuratMasuk()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Opsi Surat Masuk sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar opsi surat masuk yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        OpsiSuratMasukDao dao = DaoFactory.getInstance().getOpsiSuratMasukDao();
        OpsiSuratMasuk entity = currentEntity;
        if (entity.getId() != null) {
            entity = dao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setJenisPengguna(hanyaBoleh.isChecked() ? jenisPengguna.getValue() : null);
        entity.setUsernamePengguna(hanyaBoleh.isChecked() ? null : usernamePengguna.getValue());
        if (entity.getId() != null) {
            dao.update(entity);
        } else {
            dao.save(entity);
        }
        return true;
    }

    public Boolean checkNamaOpsiSuratMasuk() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(OpsiSuratMasuk.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class OpsiSuratMasukRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final OpsiSuratMasuk opsiSuratMasuk = (OpsiSuratMasuk) arg1;

            RevisiHelper.createNewRevisi(OpsiSuratMasuk.class, opsiSuratMasuk, opsiSuratMasuk.getNama()).setParent(arg0);
            new Label(opsiSuratMasuk.getJenisPengguna() == null ? "Semua" : opsiSuratMasuk.getJenisPengguna()).setParent(arg0);
            new Label(opsiSuratMasuk.getUsernamePengguna() == null ? "Semua" : opsiSuratMasuk.getUsernamePengguna()).setParent(arg0);
            new Label(opsiSuratMasuk.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(opsiSuratMasuk.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    opsiSuratMasuk.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(opsiSuratMasuk);
                }
            });

            Hbox toolbar = new Hbox();
            MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
            button.setTooltiptext("Ubah Data");
            button.setVisible(edit);
            button.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    openForm(opsiSuratMasuk);
                }
            });
            button.setParent(toolbar);

            button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
            button.setTooltiptext("Hapus Data");
            button.setVisible(delete);
            button.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
                            new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = Integer.parseInt(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            DaoFactory.getInstance().getOpsiSuratMasukDao().delete(opsiSuratMasuk);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(
                                                    "Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
                                                            + e.getMessage());
                                        }
                                    }
                                }
                            });
                }
            });
            button.setParent(toolbar);
            ais.ui.util.MenuAksiBaris.pasang(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
