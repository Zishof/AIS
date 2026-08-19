package ais.action.master.sirs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.KoreksiItemDetailAction;
import ais.action.report.Report;
import ais.action.report.format1.sirs.inventory.LaporanKoreksiItemWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.KoreksiItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.KoreksiItemMedis;
import ais.database.model.sirs.KoreksiItemMedisDetail;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

public class KoreksiItemAction extends GenericCrudAction<KoreksiItemMedis> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search fields (auto-wired from ZUL)
    private MyTextbox searchkode;
    private Combobox searchlokasi;

    // Additional privilege flags
    private boolean approve = false;
    private boolean reject = false;

    // State
    private Lokasi myLokasi;

    // Form fields
    private MyTextbox kode;
    private MyTextbox keterangan;
    private MyDatebox tanggalPembuatan;
    private Combobox lokasi;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<KoreksiItemMedis> getEntityClass() { return KoreksiItemMedis.class; }

    @Override
    protected KoreksiItemMedis createNewEntity() { return new KoreksiItemMedis(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Koreksi Persediaan Barang"; }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        myLokasi = Common.getCurrentLokasi();
        Common.insertCombo(searchlokasi, "nama", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(searchlokasi, myLokasi);
        if (searchlokasi != null) { searchlokasi.setDisabled(myLokasi != null); }
        approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
        reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KoreksiItemMedis.class)
                .add(searchlokasi == null || searchlokasi.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
                .add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new KoreksiItemRenderer();
    }

    // ======================== Cetak report ========================

    public void onCetak(Event event) throws Exception {
        LaporanKoreksiItemWindow laporanKoreksiItemWindow = new LaporanKoreksiItemWindow();
        laporanKoreksiItemWindow.setTitle("Laporan Koreksi Per Periode");
        laporanKoreksiItemWindow.setClosable(true);
        laporanKoreksiItemWindow.setWidth("750px");
        laporanKoreksiItemWindow.setHeight("95%");
        laporanKoreksiItemWindow.setParent(page.getFirstRoot());
        laporanKoreksiItemWindow.onModal();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final KoreksiItemMedis koreksiItem) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card
        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        // Header

        // Plain Grid
        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new MyTextbox(koreksiItem.getKode() == null ? "" : koreksiItem.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Koreksi Persediaan Barang", kode);

        tanggalPembuatan = new MyDatebox(
                koreksiItem.getTanggalPembuatan() == null ? new Date() : koreksiItem.getTanggalPembuatan());
        tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
        tanggalPembuatan.setCols(30);
        fb.addRow("Tanggal Pembuatan", tanggalPembuatan);

        lokasi = new Combobox();
        Common.insertCombo(lokasi, "nama", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(lokasi, koreksiItem.getLokasi() == null ? myLokasi : koreksiItem.getLokasi());
        lokasi.setDisabled(myLokasi != null);
        lokasi.setWidth("100%");

        EventListener lokasiListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
                        : lokasi.getSelectedItem().getValue());
                String generatedKode = Common.generateCode(KoreksiItemMedis.class, 8, "ADJ", myLokasi);
                kode.setValue(generatedKode);
            }
        };
        lokasi.addEventListener("onChange", lokasiListener);
        lokasiListener.onEvent(null);
        fb.addRow("Lokasi", lokasi);

        keterangan = new MyTextbox(
                koreksiItem.getKeterangan() == null ? "" : koreksiItem.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        // South
        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);

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
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Koreksi Persediaan Barang wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan Lokasi agar kode dapat dibuat otomatis; (2) pastikan kolom kode tidak kosong; (3) simpan kembali data setelah kode terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (lokasi.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Lokasi pada daftar yang tersedia; (2) pastikan pilihan Lokasi tidak dikosongkan; (3) simpan kembali data setelah Lokasi ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        KoreksiItemMedis entity = currentEntity;
        if (entity.getId() != null) {
            entity = (KoreksiItemMedis) session.load(KoreksiItemMedis.class, entity.getId());
            currentEntity = entity;
        }
        entity.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
        entity.setKode(kode.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setTanggalPembuatan(tanggalPembuatan.getValue());
        if (entity.getId() != null) {
            Common.refreshUpdate(session, entity);
        } else {
            entity.setDibuatOleh(Common.getCurrentUser());
            myLokasi = (Lokasi) lokasi.getSelectedItem().getValue();
            entity.setIndex(Common.generateMaxByLokasi(KoreksiItemMedis.class, myLokasi) + 1);
            String generatedKode = Common.generateCode(KoreksiItemMedis.class, 8, "ADJ", myLokasi);
            kode.setValue(generatedKode);
            entity.setKode(generatedKode);
            session.save(entity);
        }
        return true;
    }

    // ======================== Renderer ========================

    class KoreksiItemRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final KoreksiItemMedis koreksiItem = (KoreksiItemMedis) arg1;

            final KoreksiItemDetailAction detail;
            (detail = new KoreksiItemDetailAction(koreksiItem)).setParent(arg0);

            RevisiHelper.createNewRevisi(KoreksiItemMedis.class, koreksiItem, koreksiItem.getKode()).setParent(arg0);
            new Label(koreksiItem.getLokasi() == null ? "" : koreksiItem.getLokasi().getNama()).setParent(arg0);
            new Label(koreksiItem.getDibuatOleh() == null ? "" : koreksiItem.getDibuatOleh().getUserNama()).setParent(arg0);
            new Label(koreksiItem.getTanggalPembuatan() == null ? ""
                    : Common.dateFormat3.get().format(koreksiItem.getTanggalPembuatan())).setParent(arg0);

            final Label disetujuiOleh = new Label(
                    koreksiItem.getDisetujuiOleh() == null ? "" : koreksiItem.getDisetujuiOleh().getUserNama());
            disetujuiOleh.setParent(arg0);

            final Label disetujuiTanggal = new Label(koreksiItem.getTanggalPersetujuan() == null ? ""
                    : Common.dateFormat3.get().format(koreksiItem.getTanggalPersetujuan()));
            disetujuiTanggal.setParent(arg0);
            new Label(koreksiItem.getKeterangan()).setParent(arg0);

            // kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
            final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
                    new java.util.ArrayList<org.zkoss.zk.ui.Component>();

            Toolbarbutton btnCetak = new MyToolbarbuttonConfig("", "/img/print.png");
            btnCetak.setTooltiptext("Cetak Koreksi Persediaan Barang");
            btnCetak.addEventListener("onClick", new EventListener() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public void onEvent(Event event) throws Exception {
                    Map parameters = new HashMap();
                    parameters.put("id", koreksiItem.getId());
                    Report.generateWindowReport(Report.PDF, parameters, "koreksi_item",
                            koreksiItem.getTanggalPembuatan());
                }
            });
            aksiButtons.add(btnCetak);

            final Toolbarbutton btnApprove = new MyToolbarbuttonConfig("", "/img/check.png");
            final Toolbarbutton btnReject = new MyToolbarbuttonConfig("", "/img/cross.png");
            final Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            final Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");

            btnApprove.setVisible(approve && koreksiItem.getDisetujuiOleh() == null);
            btnReject.setVisible(reject && koreksiItem.getDisetujuiOleh() != null);
            btnEdit.setVisible(edit && koreksiItem.getDisetujuiOleh() == null);
            btnDelete.setVisible(delete && koreksiItem.getDisetujuiOleh() == null);

            btnApprove.setTooltiptext("Persetujuan");
            btnApprove.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui Koreksi Persediaan Barang ini? Setelah disetujui, koreksi persediaan akan diproses ke dalam stok.", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        Integer count = ((Number) session.createCriteria(KoreksiItemMedisDetail.class)
                                                .setProjection(Projections.count("id"))
                                                .add(Restrictions.isNull("kodeTransaksi")).uniqueResult()).intValue();
                                        if (!count.equals(0)) {
                                            MyMessageboxConfig.show("Mohon maaf, masih terdapat rincian koreksi yang belum ditentukan jenisnya. Langkah yang dapat dilakukan: (1) lengkapi jenis koreksi pada setiap rincian barang; (2) pastikan tidak ada rincian yang kosong; (3) lakukan kembali persetujuan setelah seluruh jenis koreksi terisi.", "Peringatan",
                                                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                                            return;
                                        }
                                        koreksiItem.setDisetujuiOleh(Common.getCurrentUser());
                                        koreksiItem.setTanggalPersetujuan(new Date());
                                        Common.refreshUpdate(session, koreksiItem);
                                        List<KoreksiItemMedisDetail> koreksiItemDetails = session
                                                .createCriteria(KoreksiItemMedisDetail.class)
                                                .add(Restrictions.eq("koreksiItem", koreksiItem)).list();
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where koreksi_item_detail in (select id from koreksi_item_detail where koreksi_item = "
                                                        + koreksiItem.getId() + ");").executeUpdate();
                                        for (KoreksiItemMedisDetail koreksiItemDetail : koreksiItemDetails) {
                                            DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
                                            detailTransaksi.setKoreksiItemDetail(koreksiItemDetail);
                                            detailTransaksi.setQtyBonus(0.0);
                                            detailTransaksi.setItem(koreksiItemDetail.getItem());
                                            detailTransaksi.setAmount(Math.abs(
                                                    koreksiItemDetail.getHarga() == null ? 0.0 : koreksiItemDetail.getHarga()));
                                            detailTransaksi.setKeterangan("Transaksi Koreksi Persediaan Barang");
                                            detailTransaksi.setKodeTransaksi(koreksiItemDetail.getKodeTransaksi());
                                            detailTransaksi.setLokasi(koreksiItem.getLokasi());
                                            detailTransaksi.setQty(Math.abs(
                                                    koreksiItemDetail.getJumlah() == null ? 0.0 : koreksiItemDetail.getJumlah()));
                                            detailTransaksi.setTanggal(new Date());
                                            session.save(detailTransaksi);
                                        }
                                        disetujuiTanggal.setValue(koreksiItem.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(koreksiItem.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(koreksiItem.getDisetujuiOleh() == null ? ""
                                                : koreksiItem.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && koreksiItem.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && koreksiItem.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && koreksiItem.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && koreksiItem.getDisetujuiOleh() == null);
                                        if (detail != null) {
                                            Common.clear(detail);
                                            detail.display();
                                        }
                                    }
                                }
                            });
                }
            });
            aksiButtons.add(btnApprove);

            btnReject.setTooltiptext("Dibatalkan");
            btnReject.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan persetujuan Koreksi Persediaan Barang ini? Pembatalan akan mengembalikan status koreksi dan menghapus dampaknya pada transaksi.", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        koreksiItem.setDisetujuiOleh(null);
                                        koreksiItem.setTanggalPersetujuan(null);
                                        Common.refreshUpdate(session, koreksiItem);
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where koreksi_item_detail in (select id from koreksi_item_detail where koreksi_item = "
                                                        + koreksiItem.getId() + ");").executeUpdate();
                                        disetujuiTanggal.setValue(koreksiItem.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(koreksiItem.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(koreksiItem.getDisetujuiOleh() == null ? ""
                                                : koreksiItem.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && koreksiItem.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && koreksiItem.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && koreksiItem.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && koreksiItem.getDisetujuiOleh() == null);
                                        if (detail != null) {
                                            Common.clear(detail);
                                            detail.display();
                                        }
                                    }
                                }
                            });
                }
            });
            aksiButtons.add(btnReject);

            btnEdit.setTooltiptext("Rubah Data");
            btnEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    currentEntity = koreksiItem;
                    buildFormContent(addWindow, koreksiItem);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            aksiButtons.add(btnEdit);

            btnDelete.setTooltiptext("Hapus Data");
            btnDelete.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data Koreksi Persediaan Barang ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            KoreksiItemDao koreksiItemDao = DaoFactory.getInstance().getKoreksiItemDao();
                                            Session session = koreksiItemDao.getCurrentSession();
                                            List<KoreksiItemMedisDetail> koreksiItemDetails = session
                                                    .createCriteria(KoreksiItemMedisDetail.class)
                                                    .add(Restrictions.eq("koreksiItem", koreksiItem)).list();
                                            for (KoreksiItemMedisDetail koreksiItemDetail : koreksiItemDetails) {
                                                Common.refreshDelete(session, koreksiItemDetail);
                                            }
                                            Common.refreshDelete(session, koreksiItem);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Mohon maaf, data Koreksi Persediaan Barang ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
                                                    e.getMessage()));
                                        }
                                    }
                                }
                            });
                }
            });
            aksiButtons.add(btnDelete);
            // Susun semua tombol: max 3 per baris, rata tengah
            ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
        }
    }
}
