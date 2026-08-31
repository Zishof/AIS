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
import ais.action.master.sirs.detail.PemakaianItemDetailAction;
import ais.action.report.Report;
import ais.action.report.format1.sirs.inventory.LaporanPemakaianItemWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.PemakaianItem;
import ais.database.model.sirs.PemakaianItemDetail;
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

/**
 * Layar CRUD dan alur persetujuan Pemakaian Barang ({@link PemakaianItem}) pada modul SIRS: pencatatan
 * pengambilan barang/obat dari stok oleh pegawai (mis. untuk keperluan pasien), diikuti detail item
 * ({@link PemakaianItemDetail}, dikelola lewat {@link PemakaianItemDetailAction} tertanam pada
 * setiap baris grid). Kode transaksi dibangkitkan otomatis per lokasi ({@code Common.generateCode}),
 * dan lokasi terkunci mengikuti lokasi kerja pengguna bila sudah ditentukan ({@link
 * Common#getCurrentLokasi()}). Alur persetujuan: sebelum disetujui, data masih dapat diubah/dihapus
 * (tombol edit/hapus hanya tampil selama {@code disetujuiOleh} kosong); menyetujui
 * ({@code btnApprove}, hak {@link CommonPrivilages#APPROVE}) MENCATAT stok keluar dengan membuat
 * baris {@link DetailTransaksiPasien} untuk setiap detail item; membatalkan persetujuan
 * ({@code btnReject}, hak {@link CommonPrivilages#REJECT}) menghapus baris transaksi stok yang
 * bersangkutan via SQL native langsung dan mengosongkan status persetujuan, mengembalikan data ke
 * status dapat diedit.
 */
public class PemakaianiItemAction extends GenericCrudAction<PemakaianItem> {

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
    private MyTextbox keperluan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PemakaianItem> getEntityClass() { return PemakaianItem.class; }

    @Override
    protected PemakaianItem createNewEntity() { return new PemakaianItem(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Pemakaian Barang"; }

    /** Menginisialisasi komponen dasar layar, mengunci filter lokasi ke lokasi kerja pengguna bila ada, dan mengevaluasi hak akses approve/reject. */
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

    /** Menyusun kriteria pencarian {@link PemakaianItem}, difilter lokasi dan kode, diurutkan id terbaru lebih dulu bila diminta. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PemakaianItem.class)
                .add(searchlokasi == null || searchlokasi.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
                .add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    /** Menyediakan renderer baris grid {@link PemakaianItemRenderer} untuk daftar hasil pencarian. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new PemakaianItemRenderer();
    }

    // ======================== Cetak report ========================

    /** Membuka dialog modal laporan pemakaian barang per periode ({@link LaporanPemakaianItemWindow}). */
    public void onCetak(Event event) throws Exception {
        LaporanPemakaianItemWindow laporanPemakaianItemWindow = new LaporanPemakaianItemWindow();
        laporanPemakaianItemWindow.setTitle("Laporan Pemakaian Per Periode");
        laporanPemakaianItemWindow.setClosable(true);
        laporanPemakaianItemWindow.setWidth("750px");
        laporanPemakaianItemWindow.setHeight("95%");
        laporanPemakaianItemWindow.setParent(page.getFirstRoot());
        laporanPemakaianItemWindow.onModal();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah pemakaian barang (kode auto-generate per lokasi, tanggal, lokasi, pegawai pemakai, keperluan, keterangan) beserta tombol batal/simpan pada jendela dialog. */
    @Override
    protected void buildFormContent(MyWindow window, final PemakaianItem pemakaianItem) throws Exception {
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

        kode = new MyTextbox(pemakaianItem.getKode() == null ? "" : pemakaianItem.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Pemakaian Barang", kode);

        tanggalPembuatan = new MyDatebox(
                pemakaianItem.getTanggalPembuatan() == null ? new Date() : pemakaianItem.getTanggalPembuatan());
        tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
        tanggalPembuatan.setCols(30);
        fb.addRow("Tanggal Pemakaian", tanggalPembuatan);

        lokasi = new Combobox();
        Common.insertCombo(lokasi, "nama", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(lokasi, pemakaianItem.getLokasi() == null ? myLokasi : pemakaianItem.getLokasi());
        lokasi.setDisabled(myLokasi != null);
        lokasi.setWidth("100%");

        EventListener lokasiListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
                        : lokasi.getSelectedItem().getValue());
                String generatedKode = Common.generateCode(PemakaianItem.class, 8, "PB", myLokasi);
                kode.setValue(generatedKode);
            }
        };
        lokasi.addEventListener("onChange", lokasiListener);
        lokasiListener.onEvent(null);
        fb.addRow("Lokasi", lokasi);

        pegawai = new AmbilDataPegawaiBanbox();
        pegawai.setAttribute("pegawai", pemakaianItem.getPegawai());
        pegawai.setValue(pemakaianItem.getPegawai() == null ? "" : pemakaianItem.getPegawai().getNama());
        pegawai.setWidth("100%");
        fb.addRow("Dipakai oleh", pegawai);

        keperluan = new MyTextbox(
                pemakaianItem.getKeperluan() == null ? "" : pemakaianItem.getKeperluan());
        keperluan.setWidth("100%");
        keperluan.setRows(4);
        fb.addRow("Keperluan", keperluan);

        keterangan = new MyTextbox(
                pemakaianItem.getKeterangan() == null ? "" : pemakaianItem.getKeterangan());
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

    /**
     * Memvalidasi lalu menyimpan data pemakaian barang dari form: menolak bila kode/lokasi/pegawai
     * pemakai/keperluan belum lengkap; jika lolos, data baru diberi nomor urut per lokasi dan kode
     * baru dibangkitkan ulang ({@code Common.generateCode}), lalu entitas disimpan/diperbarui.
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan Hibernate saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show(
                    "Mohon maaf, Kode Pemakaian Barang belum terisi sehingga data belum dapat disimpan. Langkah yang dapat dilakukan: (1) pastikan Lokasi telah dipilih agar kode dapat dibuat secara otomatis; (2) periksa kembali kelengkapan isian formulir; (3) simpan ulang data setelah kode tampil.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (lokasi.getSelectedItem() == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, Lokasi belum dipilih sehingga data belum dapat disimpan. Langkah yang dapat dilakukan: (1) pilih Lokasi pada daftar yang tersedia; (2) pastikan Lokasi telah sesuai dengan pemakaian barang; (3) simpan kembali data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (pegawai.getAttribute("pegawai") == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, data Pegawai yang menggunakan barang belum diisi. Langkah yang dapat dilakukan: (1) tekan kolom \"Dipakai oleh\" untuk memilih Pegawai; (2) pastikan Pegawai yang dipilih sudah benar; (3) simpan kembali data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (keperluan.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show(
                    "Mohon maaf, Keperluan Pemakaian belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Keperluan dengan uraian yang jelas; (2) periksa kembali isian formulir; (3) simpan kembali data.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        PemakaianItem entity = currentEntity;
        if (entity.getId() != null) {
            entity = (PemakaianItem) session.load(PemakaianItem.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKeperluan(keperluan.getValue().trim());
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
            entity.setIndex(Common.generateMaxByLokasi(PemakaianItem.class, myLokasi) + 1);
            String generatedKode = Common.generateCode(PemakaianItem.class, 8, "PB", myLokasi);
            kode.setValue(generatedKode);
            entity.setKode(generatedKode);
            session.save(entity);
        }
        return true;
    }

    // ======================== Renderer ========================

    /**
     * Renderer baris grid daftar pemakaian barang: detail item tertanam ({@link PemakaianItemDetailAction}),
     * kode (dengan link riwayat revisi), lokasi, pegawai pemakai, keperluan, pembuat + tanggal, status
     * persetujuan (pemberi + tanggal), keterangan, dan tombol aksi cetak/setujui/batalkan/ubah/hapus
     * yang visibilitasnya bergantung pada hak akses dan status persetujuan baris.
     */
    class PemakaianItemRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PemakaianItem pemakaianItem = (PemakaianItem) arg1;

            final PemakaianItemDetailAction detail;
            (detail = new PemakaianItemDetailAction(pemakaianItem)).setParent(arg0);

            RevisiHelper.createNewRevisi(PemakaianItem.class, pemakaianItem,
                    pemakaianItem.getKode()).setParent(arg0);
            new Label(pemakaianItem.getLokasi() == null ? "" : pemakaianItem.getLokasi().getNama()).setParent(arg0);
            new Label(pemakaianItem.getPegawai() == null ? "" : pemakaianItem.getPegawai().getNama()).setParent(arg0);
            new Label(pemakaianItem.getKeperluan()).setParent(arg0);
            new Label(pemakaianItem.getDibuatOleh() == null ? ""
                    : pemakaianItem.getDibuatOleh().getUserNama()).setParent(arg0);
            new Label(pemakaianItem.getTanggalPembuatan() == null ? ""
                    : Common.dateFormat3.get().format(pemakaianItem.getTanggalPembuatan())).setParent(arg0);

            final Label disetujuiOleh = new Label(pemakaianItem.getDisetujuiOleh() == null ? ""
                    : pemakaianItem.getDisetujuiOleh().getUserNama());
            disetujuiOleh.setParent(arg0);

            final Label disetujuiTanggal = new Label(pemakaianItem.getTanggalPersetujuan() == null ? ""
                    : Common.dateFormat3.get().format(pemakaianItem.getTanggalPersetujuan()));
            disetujuiTanggal.setParent(arg0);
            new Label(pemakaianItem.getKeterangan()).setParent(arg0);

            // kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
            final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
                    new java.util.ArrayList<org.zkoss.zk.ui.Component>();

            Toolbarbutton btnCetak = new MyToolbarbuttonConfig("", "/img/print.png");
            btnCetak.setTooltiptext("Cetak Pemakaian Barang");
            btnCetak.addEventListener("onClick", new EventListener() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public void onEvent(Event event) throws Exception {
                    Map parameters = new HashMap();
                    parameters.put("id", pemakaianItem.getId());
                    Report.generateWindowReport(Report.PDF, parameters, "pemakaian_item",
                            pemakaianItem.getTanggalPembuatan());
                }
            });
            aksiButtons.add(btnCetak);

            final Toolbarbutton btnApprove = new MyToolbarbuttonConfig("", "/img/check.png");
            final Toolbarbutton btnReject = new MyToolbarbuttonConfig("", "/img/cross.png");
            final Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            final Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");

            btnApprove.setVisible(approve && pemakaianItem.getDisetujuiOleh() == null);
            btnReject.setVisible(reject && pemakaianItem.getDisetujuiOleh() != null);
            btnEdit.setVisible(edit && pemakaianItem.getDisetujuiOleh() == null);
            btnDelete.setVisible(delete && pemakaianItem.getDisetujuiOleh() == null);

            btnApprove.setTooltiptext("Persetujuan");
            btnApprove.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menyetujui data Pemakaian Barang ini? Setelah disetujui, stok barang akan berkurang sesuai jumlah pemakaian dan data tidak dapat diubah kembali.",
                            "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        pemakaianItem.setDisetujuiOleh(Common.getCurrentUser());
                                        pemakaianItem.setTanggalPersetujuan(new Date());
                                        Common.refreshUpdate(session, pemakaianItem);
                                        List<PemakaianItemDetail> pemakaianItemDetails = session
                                                .createCriteria(PemakaianItemDetail.class)
                                                .add(Restrictions.eq("pemakaianItem", pemakaianItem)).list();
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where pemakaian_item_detail in (select id from sirs.pemakaian_item_detail where pemakaian_item = "
                                                        + pemakaianItem.getId() + ");").executeUpdate();
                                        for (PemakaianItemDetail pemakaianItemDetail : pemakaianItemDetails) {
                                            DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
                                            detailTransaksi.setPemakaianItemDetail(pemakaianItemDetail);
                                            detailTransaksi.setQtyBonus(0.0);
                                            detailTransaksi.setItem(pemakaianItemDetail.getItem());
                                            detailTransaksi.setAmount(Math.abs(
                                                    pemakaianItemDetail.getHarga() == null ? 0.0
                                                            : pemakaianItemDetail.getHarga()));
                                            detailTransaksi.setKeterangan("Transaksi Pemakaian Barang");
                                            detailTransaksi.setKodeTransaksi(ConstantValues.pemakaianBarang);
                                            detailTransaksi.setLokasi(pemakaianItem.getLokasi());
                                            detailTransaksi.setQty(Math.abs(
                                                    pemakaianItemDetail.getJumlah() == null ? 0.0
                                                            : pemakaianItemDetail.getJumlah()));
                                            detailTransaksi.setTanggal(new Date());
                                            session.save(detailTransaksi);
                                        }
                                        disetujuiTanggal.setValue(pemakaianItem.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(pemakaianItem.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(pemakaianItem.getDisetujuiOleh() == null ? ""
                                                : pemakaianItem.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && pemakaianItem.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && pemakaianItem.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && pemakaianItem.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && pemakaianItem.getDisetujuiOleh() == null);
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
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin membatalkan persetujuan Pemakaian Barang ini? Setelah dibatalkan, transaksi pengurangan stok yang terkait akan dihapus dan data dapat diubah kembali.",
                            "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        pemakaianItem.setDisetujuiOleh(null);
                                        pemakaianItem.setTanggalPersetujuan(null);
                                        Common.refreshUpdate(session, pemakaianItem);
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where pemakaian_item_detail in (select id from sirs.pemakaian_item_detail where pemakaian_item = "
                                                        + pemakaianItem.getId() + ");").executeUpdate();
                                        disetujuiTanggal.setValue(pemakaianItem.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(pemakaianItem.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(pemakaianItem.getDisetujuiOleh() == null ? ""
                                                : pemakaianItem.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && pemakaianItem.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && pemakaianItem.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && pemakaianItem.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && pemakaianItem.getDisetujuiOleh() == null);
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
                    currentEntity = pemakaianItem;
                    buildFormContent(addWindow, pemakaianItem);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            aksiButtons.add(btnEdit);

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
                                            List<PemakaianItemDetail> pemakaianItemDetails = session
                                                    .createCriteria(PemakaianItemDetail.class)
                                                    .add(Restrictions.eq("pemakaianItem", pemakaianItem)).list();
                                            for (PemakaianItemDetail pemakaianItemDetail : pemakaianItemDetails) {
                                                session.delete(pemakaianItemDetail);
                                            }
                                            Common.refreshDelete(session, pemakaianItem);
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
            aksiButtons.add(btnDelete);
            // Susun semua tombol: max 3 per baris, rata tengah
            ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
        }
    }
}
