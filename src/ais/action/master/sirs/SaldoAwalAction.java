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
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.SaldoAwalDetailAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.SaldoAwalMedis;
import ais.database.model.sirs.SaldoAwalMedisDetail;
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
 * Layar CRUD modul SIRS (rumah sakit) untuk {@link SaldoAwalMedis} (saldo awal stok obat/alat medis
 * per {@link Lokasi}), dibangun di atas kerangka generik {@link GenericCrudAction}. Kode saldo awal
 * dibuat otomatis lewat {@link Common#generateCode} berbasis lokasi terpilih; rincian item saldo
 * awal dikelola terpisah lewat {@link SaldoAwalDetailAction} (ditampilkan lazy per baris grid).
 *
 * <p>
 * Alur persetujuan dua arah: {@code btnApprove} memvalidasi kelengkapan rincian (harga, tanggal
 * kadaluarsa, satuan, dan jumlah item wajib terisi wajar) sebelum mengizinkan persetujuan, lalu
 * "meledakkan" (materialize) setiap {@link SaldoAwalMedisDetail} menjadi satu baris {@link Kadaluarsa}
 * (stok kadaluarsa awal) dan satu baris {@link DetailTransaksiPasien} (transaksi saldo awal) —
 * baris lama pada kedua tabel tersebut dihapus lebih dulu via SQL native untuk mencegah duplikasi
 * bila disetujui ulang. {@code btnReject} membatalkan persetujuan dan menghapus balik seluruh baris
 * {@link Kadaluarsa}/{@link DetailTransaksiPasien} turunan tersebut. Data yang sudah disetujui tidak
 * bisa diubah/dihapus lagi lewat tombol ubah/hapus biasa.
 * </p>
 */
public class SaldoAwalAction extends GenericCrudAction<SaldoAwalMedis> {

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

    /** @return kelas entitas yang dikelola layar ini, {@link SaldoAwalMedis}. */
    @Override
    protected Class<SaldoAwalMedis> getEntityClass() { return SaldoAwalMedis.class; }

    /** @return instance {@link SaldoAwalMedis} kosong untuk form tambah baru. */
    @Override
    protected SaldoAwalMedis createNewEntity() { return new SaldoAwalMedis(); }

    /** @return judul jendela form tambah/ubah. */
    @Override
    protected String getWindowTitle() { return "Pendataan Saldo Awal"; }

    /** Inisialisasi layar: memuat combo lokasi pencarian (dikunci ke lokasi user saat ini bila ada), dan menentukan privilese APPROVE/REJECT tambahan di luar privilese dasar dari {@link GenericCrudAction}. */
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

    /** Membentuk criteria pencarian {@link SaldoAwalMedis} berdasarkan filter lokasi dan kode (ILIKE), diurut id menurun bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(SaldoAwalMedis.class)
                .add(searchlokasi == null || searchlokasi.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
                .add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    /** @return renderer baris grid untuk {@link SaldoAwalMedis} ({@link SaldoAwalRenderer}). */
    @Override
    protected MyRowRenderer createRenderer() {
        return new SaldoAwalRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link SaldoAwalMedis}: kode (readonly, auto-generate saat lokasi dipilih), tanggal pembuatan, lokasi, dan keterangan, plus toolbar Batal/Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final SaldoAwalMedis saldoAwal) throws Exception {
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

        kode = new MyTextbox(saldoAwal.getKode() == null ? "" : saldoAwal.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Saldo Awal", kode);

        tanggalPembuatan = new MyDatebox(
                saldoAwal.getTanggalPembuatan() == null ? new Date() : saldoAwal.getTanggalPembuatan());
        tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
        tanggalPembuatan.setCols(30);
        fb.addRow("Tanggal Pembuatan", tanggalPembuatan);

        lokasi = new Combobox();
        Common.insertCombo(lokasi, "nama", Lokasi.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.selectComboItem(lokasi, saldoAwal.getLokasi() == null ? myLokasi : saldoAwal.getLokasi());
        lokasi.setDisabled(myLokasi != null);
        lokasi.setWidth("100%");

        EventListener lokasiListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
                        : lokasi.getSelectedItem().getValue());
                String generatedKode = Common.generateCode(SaldoAwalMedis.class, 8, "AW", myLokasi);
                kode.setValue(generatedKode);
            }
        };
        lokasi.addEventListener("onChange", lokasiListener);
        lokasiListener.onEvent(null);
        fb.addRow("Lokasi", lokasi);

        keterangan = new MyTextbox(
                saldoAwal.getKeterangan() == null ? "" : saldoAwal.getKeterangan());
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
     * Memvalidasi (kode dan lokasi wajib terisi) dan menyimpan {@link SaldoAwalMedis}. Untuk entitas
     * baru, indeks urut per lokasi ({@link Common#generateMaxByLokasi}) dan kode final di-generate
     * ulang tepat sebelum simpan (memastikan keunikan meski kode sudah tampil di form sejak lokasi
     * dipilih).
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     */
    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show(
                    "Kode Saldo Awal harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi agar kode saldo awal terbentuk otomatis; (2) pastikan kolom kode saldo awal tidak kosong; (3) ulangi proses penyimpanan.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (lokasi.getSelectedItem() == null) {
            MyMessageboxConfig.show(
                    "Lokasi harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi pada kolom lokasi; (2) pastikan lokasi yang dipilih sudah benar; (3) ulangi proses penyimpanan.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        SaldoAwalMedis entity = currentEntity;
        if (entity.getId() != null) {
            entity = (SaldoAwalMedis) session.load(SaldoAwalMedis.class, entity.getId());
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
            entity.setIndex(Common.generateMaxByLokasi(SaldoAwalMedis.class, myLokasi) + 1);
            String generatedKode = Common.generateCode(SaldoAwalMedis.class, 8, "AW", myLokasi);
            kode.setValue(generatedKode);
            entity.setKode(generatedKode);
            session.save(entity);
        }
        return true;
    }

    // ======================== Renderer ========================

    /**
     * Renderer baris grid {@link SaldoAwalMedis}: kode (dengan detail rincian item ter-embed via
     * {@link SaldoAwalDetailAction}), lokasi, pembuat, tanggal pembuatan, status persetujuan
     * (penyetuju + tanggal), keterangan, dan tombol aksi (cetak PDF, setujui, batalkan persetujuan,
     * ubah, hapus) yang visibilitasnya bergantung pada privilese dan status persetujuan saat ini.
     */
    class SaldoAwalRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final SaldoAwalMedis saldoAwal = (SaldoAwalMedis) arg1;

            final SaldoAwalDetailAction detail;
            (detail = new SaldoAwalDetailAction(saldoAwal)).setParent(arg0);

            RevisiHelper.createNewRevisi(SaldoAwalMedis.class, saldoAwal, saldoAwal.getKode()).setParent(arg0);
            new Label(saldoAwal.getLokasi() == null ? "" : saldoAwal.getLokasi().getNama()).setParent(arg0);
            new Label(saldoAwal.getDibuatOleh() == null ? "" : saldoAwal.getDibuatOleh().getUserNama()).setParent(arg0);
            new Label(saldoAwal.getTanggalPembuatan() == null ? ""
                    : Common.dateFormat3.get().format(saldoAwal.getTanggalPembuatan())).setParent(arg0);

            final Label disetujuiOleh = new Label(
                    saldoAwal.getDisetujuiOleh() == null ? "" : saldoAwal.getDisetujuiOleh().getUserNama());
            disetujuiOleh.setParent(arg0);

            final Label disetujuiTanggal = new Label(saldoAwal.getTanggalPersetujuan() == null ? ""
                    : Common.dateFormat3.get().format(saldoAwal.getTanggalPersetujuan()));
            disetujuiTanggal.setParent(arg0);
            new Label(saldoAwal.getKeterangan()).setParent(arg0);

            // kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
            final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
                    new java.util.ArrayList<org.zkoss.zk.ui.Component>();

            Toolbarbutton btnCetak = new MyToolbarbuttonConfig("", "/img/print.png");
            btnCetak.setTooltiptext("Cetak Saldo Awal");
            btnCetak.addEventListener("onClick", new EventListener() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public void onEvent(Event event) throws Exception {
                    Map parameters = new HashMap();
                    parameters.put("id", saldoAwal.getId());
                    Report.generateWindowReport(Report.PDF, parameters, "sirs/saldo_awal",
                            saldoAwal.getTanggalPembuatan());
                }
            });
            aksiButtons.add(btnCetak);

            final Toolbarbutton btnApprove = new MyToolbarbuttonConfig("", "/img/check.png");
            final Toolbarbutton btnReject = new MyToolbarbuttonConfig("", "/img/cross.png");
            final Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            final Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");

            btnApprove.setVisible(approve && saldoAwal.getDisetujuiOleh() == null);
            btnReject.setVisible(reject && saldoAwal.getDisetujuiOleh() != null);
            btnEdit.setVisible(edit && saldoAwal.getDisetujuiOleh() == null);
            btnDelete.setVisible(delete && saldoAwal.getDisetujuiOleh() == null);

            btnApprove.setTooltiptext("Persetujuan");
            btnApprove.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menyetujui data Saldo Awal ini? Setelah disetujui, data stok, kadaluarsa, dan transaksi saldo awal akan tercatat ke dalam sistem.",
                            "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        Integer countItemBatchBelumbenar = ((Number) session
                                                .createCriteria(SaldoAwalMedisDetail.class)
                                                .setProjection(Projections.count("id"))
                                                .add(Restrictions.eq("saldoAwal", saldoAwal))
                                                .add(Restrictions.or(Restrictions.lt("harga", 1.0),
                                                        Restrictions.or(Restrictions.isNull("tanggalKadaluarsa"),
                                                                Restrictions.isNull("harga"))))
                                                .uniqueResult()).intValue();
                                        if (!countItemBatchBelumbenar.equals(0)) {
                                            MyMessageboxConfig.show(
                                                    "Tanggal kadaluarsa dan harga pada rincian saldo awal belum lengkap. Langkah yang dapat dilakukan: (1) buka rincian saldo awal; (2) lengkapi tanggal kadaluarsa dan harga pada setiap item; (3) ulangi proses persetujuan.",
                                                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                                            return;
                                        }
                                        Integer count = ((Number) session.createCriteria(SaldoAwalMedisDetail.class)
                                                .add(Restrictions.eq("saldoAwal", saldoAwal))
                                                .add(Restrictions.or(Restrictions.isNull("satuanItem"),
                                                        Restrictions.eq("satuanItem", ConstantValues.DEFAULT_SATUAN)))
                                                .setProjection(Projections.count("id")).uniqueResult()).intValue();
                                        if (!count.equals(0)) {
                                            MyMessageboxConfig.show(
                                                    "Data satuan pada rincian saldo awal belum lengkap. Langkah yang dapat dilakukan: (1) buka rincian saldo awal; (2) tentukan satuan yang benar pada setiap item; (3) ulangi proses persetujuan.",
                                                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                                            return;
                                        }
                                        count = ((Number) session.createCriteria(SaldoAwalMedisDetail.class)
                                                .add(Restrictions.eq("saldoAwal", saldoAwal))
                                                .add(Restrictions.lt("jumlah", 1.0))
                                                .setProjection(Projections.count("id")).uniqueResult()).intValue();
                                        if (!count.equals(0)) {
                                            MyMessageboxConfig.show(
                                                    "Data jumlah item pada rincian saldo awal belum lengkap. Langkah yang dapat dilakukan: (1) buka rincian saldo awal; (2) isi jumlah item minimal 1 pada setiap baris; (3) ulangi proses persetujuan.",
                                                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                                            return;
                                        }
                                        saldoAwal.setDisetujuiOleh(Common.getCurrentUser());
                                        saldoAwal.setTanggalPersetujuan(new Date());
                                        Common.refreshUpdate(session, saldoAwal);
                                        List<SaldoAwalMedisDetail> saldoAwalDetails = session
                                                .createCriteria(SaldoAwalMedisDetail.class)
                                                .add(Restrictions.eq("saldoAwal", saldoAwal)).list();
                                        session.createSQLQuery(
                                                "delete from sirs.kadaluarsa where saldo_awal_detail in (select id from sirs.saldo_awal_detail_medis where saldo_awal = "
                                                        + saldoAwal.getId() + ");").executeUpdate();
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where saldo_awal_detail in (select id from sirs.saldo_awal_detail_medis where saldo_awal = "
                                                        + saldoAwal.getId() + ");").executeUpdate();
                                        for (SaldoAwalMedisDetail saldoAwalDetail : saldoAwalDetails) {
                                            Kadaluarsa kadaluarsa = new Kadaluarsa();
                                            kadaluarsa.setItem(saldoAwalDetail.getItem());
                                            kadaluarsa.setKeterangan("Kadaluarsa " + saldoAwalDetail.getItem().getNama()
                                                    + " dari saldo awal");
                                            kadaluarsa.setLokasi(saldoAwal.getLokasi());
                                            kadaluarsa.setSaldoAwalDetail(saldoAwalDetail);
                                            kadaluarsa.setQty(saldoAwalDetail.getJumlah());
                                            kadaluarsa.setTanggalKadaluarsa(saldoAwalDetail.getTanggalKadaluarsa());
                                            session.save(kadaluarsa);
                                            DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
                                            detailTransaksi.setSaldoAwalDetail(saldoAwalDetail);
                                            detailTransaksi.setQtyBonus(0.0);
                                            detailTransaksi.setItem(saldoAwalDetail.getItem());
                                            detailTransaksi.setAmount(saldoAwalDetail.getHarga());
                                            detailTransaksi.setKeterangan("Transaksi Saldo Awal");
                                            detailTransaksi.setKodeTransaksi(ConstantValues.saldoAwal);
                                            detailTransaksi.setLokasi(saldoAwal.getLokasi());
                                            detailTransaksi.setQty(saldoAwalDetail.getJumlah());
                                            detailTransaksi.setTanggal(new Date());
                                            session.save(detailTransaksi);
                                        }
                                        disetujuiTanggal.setValue(saldoAwal.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(saldoAwal.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(saldoAwal.getDisetujuiOleh() == null ? ""
                                                : saldoAwal.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && saldoAwal.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && saldoAwal.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && saldoAwal.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && saldoAwal.getDisetujuiOleh() == null);
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
                            "Apakah Bapak/Ibu yakin ingin membatalkan persetujuan Saldo Awal ini? Data kadaluarsa dan transaksi yang telah terbentuk dari saldo awal ini akan dihapus.",
                            "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        Session session = HibernateUtil.currentSession();
                                        session.refresh(saldoAwal);
                                        saldoAwal.setDisetujuiOleh(null);
                                        saldoAwal.setTanggalPersetujuan(null);
                                        Common.refreshUpdate(session, saldoAwal);
                                        session.createSQLQuery(
                                                "delete from sirs.kadaluarsa where saldo_awal_detail in (select id from sirs.saldo_awal_detail_medis where saldo_awal = "
                                                        + saldoAwal.getId() + ");").executeUpdate();
                                        session.createSQLQuery(
                                                "delete from sirs.detail_transaksi_pasien where saldo_awal_detail in (select id from sirs.saldo_awal_detail_medis where saldo_awal = "
                                                        + saldoAwal.getId() + ");").executeUpdate();
                                        disetujuiTanggal.setValue(saldoAwal.getTanggalPersetujuan() == null ? ""
                                                : Common.dateFormat3.get().format(saldoAwal.getTanggalPersetujuan()));
                                        disetujuiOleh.setValue(saldoAwal.getDisetujuiOleh() == null ? ""
                                                : saldoAwal.getDisetujuiOleh().getUserNama());
                                        btnApprove.setVisible(approve && saldoAwal.getDisetujuiOleh() == null);
                                        btnReject.setVisible(reject && saldoAwal.getDisetujuiOleh() != null);
                                        btnEdit.setVisible(edit && saldoAwal.getDisetujuiOleh() == null);
                                        btnDelete.setVisible(delete && saldoAwal.getDisetujuiOleh() == null);
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
                    currentEntity = saldoAwal;
                    buildFormContent(addWindow, saldoAwal);
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
                            "Apakah Bapak/Ibu yakin ingin menghapus data ini? Data saldo awal beserta rinciannya akan dihapus secara permanen dan tidak dapat dikembalikan.",
                            "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            Session session = HibernateUtil.currentSession();
                                            List<SaldoAwalMedisDetail> saldoAwalDetails = session
                                                    .createCriteria(SaldoAwalMedisDetail.class)
                                                    .add(Restrictions.eq("saldoAwal", saldoAwal)).list();
                                            for (SaldoAwalMedisDetail saldoAwalDetail : saldoAwalDetails) {
                                                Common.refreshDelete(session, saldoAwalDetail);
                                            }
                                            Common.refreshDelete(session, saldoAwal);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait; (2) pastikan data ini tidak sedang digunakan pada transaksi lain; (3) apabila kendala berlanjut, hubungi administrator sistem.",
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
