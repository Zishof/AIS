package ais.action.master.sirs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.PemakaianReturItemDetailAction;
import ais.action.report.Report;
import ais.action.report.format1.sirs.inventory.LaporanPemakaianReturItemWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.PemakaianReturItem;
import ais.database.model.sirs.PemakaianReturItemDetail;
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

public class PemakaianReturItemAction extends GenericCrudAction<PemakaianReturItem> {

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
    private AmbilDataPegawaiBanbox pegawai;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PemakaianReturItem> getEntityClass() { return PemakaianReturItem.class; }

    @Override
    protected PemakaianReturItem createNewEntity() { return new PemakaianReturItem(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Pemakaian Barang"; }

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
        Criteria criteria = session.createCriteria(PemakaianReturItem.class)
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
        return new PemakaianReturItemRenderer();
    }

    // ======================== Cetak report ========================

    public void onCetak(Event event) throws Exception {
        LaporanPemakaianReturItemWindow laporanPemakaianReturItemWindow = new LaporanPemakaianReturItemWindow();
        laporanPemakaianReturItemWindow.setTitle("Laporan Retur Pemakaian Per Periode");
        laporanPemakaianReturItemWindow.setClosable(true);
        laporanPemakaianReturItemWindow.setWidth("750px");
        laporanPemakaianReturItemWindow.setHeight("95%");
        laporanPemakaianReturItemWindow.setParent(page.getFirstRoot());
        laporanPemakaianReturItemWindow.onModal();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final PemakaianReturItem pemakaianReturItem) throws Exception {
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

        kode = new MyTextbox(pemakaianReturItem.getKode() == null ? "" : pemakaianReturItem.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Pemakaian Barang", kode);

        tanggalPembuatan = new MyDatebox(
                pemakaianReturItem.getTanggalPembuatan() == null ? new Date() : pemakaianReturItem.getTanggalPembuatan());
        tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
        tanggalPembuatan.setCols(30);
        fb.addRow("Tanggal Pemakaian", tanggalPembuatan);

        lokasi = new Combobox();
        Common.insertCombo(lokasi, "nama", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(lokasi,
                pemakaianReturItem.getLokasi() == null ? myLokasi : pemakaianReturItem.getLokasi());
        lokasi.setDisabled(myLokasi != null);
        lokasi.setWidth("100%");

        EventListener lokasiListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
                        : lokasi.getSelectedItem().getValue());
                String generatedKode = Common.generateCode(PemakaianReturItem.class, 8, "PR", myLokasi);
                kode.setValue(generatedKode);
            }
        };
        lokasi.addEventListener("onChange", lokasiListener);
        lokasiListener.onEvent(null);
        fb.addRow("Lokasi", lokasi);

        pegawai = new AmbilDataPegawaiBanbox();
        pegawai.setAttribute("pegawai", pemakaianReturItem.getPegawai());
        pegawai.setValue(pemakaianReturItem.getPegawai() == null ? "" : pemakaianReturItem.getPegawai().getNama());
        pegawai.setWidth("100%");
        fb.addRow("Dipakai oleh", pegawai);

        keterangan = new MyTextbox(
                pemakaianReturItem.getKeterangan() == null ? "" : pemakaianReturItem.getKeterangan());
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
            MyMessageboxConfig.show(
                    "Mohon maaf, Kode Retur Pemakaian Barang belum terisi sehingga data belum dapat disimpan. Langkah yang dapat dilakukan: (1) pastikan Lokasi telah dipilih agar kode dapat dibuat secara otomatis; (2) periksa kembali kelengkapan isian formulir; (3) simpan ulang data setelah kode tampil.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (lokasi.getSelectedItem() == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, Lokasi belum dipilih sehingga data belum dapat disimpan. Langkah yang dapat dilakukan: (1) pilih Lokasi pada daftar yang tersedia; (2) pastikan Lokasi telah sesuai dengan retur pemakaian barang; (3) simpan kembali data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (pegawai.getAttribute("pegawai") == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, data Pegawai yang menggunakan barang belum diisi. Langkah yang dapat dilakukan: (1) tekan kolom \"Dipakai oleh\" untuk memilih Pegawai; (2) pastikan Pegawai yang dipilih sudah benar; (3) simpan kembali data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        PemakaianReturItem entity = currentEntity;
        if (entity.getId() != null) {
            entity = (PemakaianReturItem) session.load(PemakaianReturItem.class, entity.getId());
            currentEntity = entity;
        }
        entity.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
        entity.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
        entity.setKode(kode.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setTanggalPembuatan(tanggalPembuatan.getValue());
        if (entity.getId() != null) {
            Common.refreshUpdate(session, entity);
        } else {
            entity.setDibuatOleh(Common.getCurrentUser());
            myLokasi = (Lokasi) lokasi.getSelectedItem().getValue();
            entity.setIndex(Common.generateMaxByLokasi(PemakaianReturItem.class, myLokasi) + 1);
            String generatedKode = Common.generateCode(PemakaianReturItem.class, 8, "PR", myLokasi);
            kode.setValue(generatedKode);
            entity.setKode(generatedKode);
            session.save(entity);
        }
        return true;
    }

    // ======================== Renderer ========================

    class PemakaianReturItemRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PemakaianReturItem pemakaianReturItem = (PemakaianReturItem) arg1;

            final PemakaianReturItemDetailAction detail;
            (detail = new PemakaianReturItemDetailAction(pemakaianReturItem)).setParent(arg0);

            RevisiHelper.createNewRevisi(PemakaianReturItem.class, pemakaianReturItem,
                    pemakaianReturItem.getKode()).setParent(arg0);
            new Label(pemakaianReturItem.getLokasi() == null ? "" : pemakaianReturItem.getLokasi().getNama()).setParent(arg0);
            new Label(pemakaianReturItem.getPegawai() == null ? "" : pemakaianReturItem.getPegawai().getNama()).setParent(arg0);
            new Label(pemakaianReturItem.getDibuatOleh() == null ? ""
                    : pemakaianReturItem.getDibuatOleh().getUserNama()).setParent(arg0);
            new Label(pemakaianReturItem.getTanggalPembuatan() == null ? ""
                    : Common.dateFormat3.get().format(pemakaianReturItem.getTanggalPembuatan())).setParent(arg0);

            final Label disetujuiOleh = new Label(pemakaianReturItem.getDisetujuiOleh() == null ? ""
                    : pemakaianReturItem.getDisetujuiOleh().getUserNama());
            disetujuiOleh.setParent(arg0);

            final Label disetujuiTanggal = new Label(pemakaianReturItem.getTanggalPersetujuan() == null ? ""
                    : Common.dateFormat3.get().format(pemakaianReturItem.getTanggalPersetujuan()));
            disetujuiTanggal.setParent(arg0);
            new Label(pemakaianReturItem.getKeterangan()).setParent(arg0);

            org.zkoss.zul.Hbox toolbar = new org.zkoss.zul.Hbox();

            Toolbarbutton btnCetak = new MyToolbarbuttonConfig("", "/img/print.png");
            btnCetak.setTooltiptext("Cetak Retur Pemakaian Barang");
            btnCetak.addEventListener("onClick", new EventListener() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public void onEvent(Event event) throws Exception {
                    Map parameters = new HashMap();
                    parameters.put("id", pemakaianReturItem.getId());
                    Report.generateWindowReport(Report.PDF, parameters, "sirs/pemakaian_retur_item",
                            pemakaianReturItem.getTanggalPembuatan());
                }
            });
            btnCetak.setParent(toolbar);

            final Toolbarbutton btnApprove = new MyToolbarbuttonConfig("", "/img/check.png");
            final Toolbarbutton btnReject = new MyToolbarbuttonConfig("", "/img/cross.png");
            final Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            final Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");

            btnApprove.setVisible(approve && pemakaianReturItem.getDisetujuiOleh() == null);
            btnReject.setVisible(reject && pemakaianReturItem.getDisetujuiOleh() != null);
            btnEdit.setVisible(edit && pemakaianReturItem.getDisetujuiOleh() == null);
            btnDelete.setVisible(delete && pemakaianReturItem.getDisetujuiOleh() == null);

            btnApprove.setTooltiptext("Persetujuan");
            btnApprove.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menyetujui data Retur Pemakaian Barang ini? Setelah disetujui, transaksi stok akan tercatat sesuai retur dan data tidak dapat diubah kembali.",
                            "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        pemakaianReturItem.setDisetujuiOleh(Common.getCurrentUser());
                                        pemakaianReturItem.setTanggalPersetujuan(new Date());
                                        Common.refreshUpdate(session, pemakaianReturItem);
                                        List<PemakaianReturItemDetail> pemakaianReturItemDetails = session
                                                .createCriteria(PemakaianReturItemDetail.class)
                                                .add(Restrictions.eq("pemakaianReturItem", pemakaianReturItem)).list();
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where pemakaian_item_detail in (select id from sirs.pemakaian_item_detail where pemakaian_item = "
                                                        + pemakaianReturItem.getId() + ");").executeUpdate();
                                        for (PemakaianReturItemDetail pemakaianReturItemDetail : pemakaianReturItemDetails) {
                                            DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
                                            detailTransaksi.setPemakaianReturItemDetail(pemakaianReturItemDetail);
                                            detailTransaksi.setQtyBonus(0.0);
                                            detailTransaksi.setItem(pemakaianReturItemDetail.getItem());
                                            detailTransaksi.setAmount(Math.abs(
                                                    pemakaianReturItemDetail.getHarga() == null ? 0.0
                                                            : pemakaianReturItemDetail.getHarga()));
                                            detailTransaksi.setKeterangan("Transaksi Retur Pemakaian Barang");
                                            detailTransaksi.setKodeTransaksi(ConstantValues.returPemakaianBarang);
                                            detailTransaksi.setLokasi(pemakaianReturItem.getLokasi());
                                            detailTransaksi.setQty(Math.abs(
                                                    pemakaianReturItemDetail.getJumlah() == null ? 0.0
                                                            : pemakaianReturItemDetail.getJumlah()));
                                            detailTransaksi.setTanggal(new Date());
                                            session.save(detailTransaksi);
                                        }
                                        disetujuiTanggal.setValue(pemakaianReturItem.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(pemakaianReturItem.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(pemakaianReturItem.getDisetujuiOleh() == null ? ""
                                                : pemakaianReturItem.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && pemakaianReturItem.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && pemakaianReturItem.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && pemakaianReturItem.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && pemakaianReturItem.getDisetujuiOleh() == null);
                                        if (detail != null) {
                                            Common.clear(detail);
                                            detail.display();
                                        }
                                    }
                                }
                            });
                }
            });
            btnApprove.setParent(toolbar);

            btnReject.setTooltiptext("Dibatalkan");
            btnReject.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin membatalkan persetujuan Retur Pemakaian Barang ini? Setelah dibatalkan, transaksi stok yang terkait akan dihapus dan data dapat diubah kembali.",
                            "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        pemakaianReturItem.setDisetujuiOleh(null);
                                        pemakaianReturItem.setTanggalPersetujuan(null);
                                        Common.refreshUpdate(session, pemakaianReturItem);
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where pemakaian_item_detail in (select id from sirs.pemakaian_item_detail where pemakaian_item = "
                                                        + pemakaianReturItem.getId() + ");").executeUpdate();
                                        disetujuiTanggal.setValue(pemakaianReturItem.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(pemakaianReturItem.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(pemakaianReturItem.getDisetujuiOleh() == null ? ""
                                                : pemakaianReturItem.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && pemakaianReturItem.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && pemakaianReturItem.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && pemakaianReturItem.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && pemakaianReturItem.getDisetujuiOleh() == null);
                                        if (detail != null) {
                                            Common.clear(detail);
                                            detail.display();
                                        }
                                    }
                                }
                            });
                }
            });
            btnReject.setParent(toolbar);

            btnEdit.setTooltiptext("Rubah Data");
            btnEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    currentEntity = pemakaianReturItem;
                    buildFormContent(addWindow, pemakaianReturItem);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            btnEdit.setParent(toolbar);

            btnDelete.setTooltiptext("Hapus Data");
            btnDelete.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.",
                            "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            Session session = HibernateUtil.currentSession();
                                            List<PemakaianReturItemDetail> pemakaianReturItemDetails = session
                                                    .createCriteria(PemakaianReturItemDetail.class)
                                                    .add(Restrictions.eq("pemakaianReturItem", pemakaianReturItem)).list();
                                            for (PemakaianReturItemDetail pemakaianReturItemDetail : pemakaianReturItemDetails) {
                                                Common.refreshDelete(session, pemakaianReturItemDetail);
                                            }
                                            Common.refreshDelete(session, pemakaianReturItem);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) pastikan seluruh data detail yang terkait telah dihapus terlebih dahulu; (2) periksa apakah data ini masih digunakan pada transaksi lain; (3) apabila kendala berlanjut, mohon hubungi administrator sistem.",
                                                    e.getMessage()));
                                        }
                                    }
                                }
                            });
                }
            });
            btnDelete.setParent(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
