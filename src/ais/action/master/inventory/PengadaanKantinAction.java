package ais.action.master.inventory;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.PengadaanProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Pengadaan / Kulakan (Barang Masuk) — konversi dari {@code modul/kantin/pengadaan + kulakan}.
 *
 * <p>Mencatat barang yang masuk dari pemasok. Saat disimpan, <b>stok produk dihitung ulang</b>
 * dengan rumus yang sama seperti versi JSP: {@code stok = Σmasuk(pengadaan) + Σopname − Σkeluar(penjualan)},
 * harga beli mengikuti pengadaan terakhir, dan harga jual otomatis dinaikkan bila lebih kecil dari
 * harga beli (anti margin negatif). Tombol "Hitung Ulang Stok" menyinkronkan ulang semua produk.</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat &amp; mencatat
 * pengadaan untuk produk tokonya sendiri.</p>
 */
public class PengadaanKantinAction extends GenericCrudAction<PengadaanProduk> {

    private static final long serialVersionUID = 1L;

    private Combobox produk;
    private org.zkoss.zul.Textbox nomorFaktur;
    private org.zkoss.zul.Textbox namaSupplier;
    private MyDoublebox qty;
    private MyDoublebox hargaBeliSatuan;
    private Datebox waktuPengadaan;
    private org.zkoss.zul.Textbox keterangan;

    private Toko scopeTokoCache;
    private boolean scopeResolved = false;

    private Toko scopeToko() {
        if (!scopeResolved) {
            scopeResolved = true;
            try {
                Toko ct = Common.getCurrentToko();
                if (ct != null && ct.getId() != null) {
                    Boolean b = ct.getBolehMelihatTokolain();
                    if (b == null || !b.booleanValue()) {
                        scopeTokoCache = ct;
                    }
                }
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/PengadaanKantinAction.java:76");
            }
        }
        return scopeTokoCache;
    }

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PengadaanProduk> getEntityClass() {
        return PengadaanProduk.class;
    }

    @Override
    protected PengadaanProduk createNewEntity() {
        return new PengadaanProduk();
    }

    @Override
    protected String getWindowTitle() {
        return "Pengadaan Barang";
    }

    @Override
    protected String getIntroTitle() {
        return "Pengadaan / Kulakan (Barang Masuk)";
    }

    @Override
    protected String getIntroDescription() {
        return "Catat barang yang masuk dari pemasok. Setelah disimpan, stok produk otomatis bertambah dan harga "
                + "beli ikut diperbarui. Tekan \"Hitung Ulang Stok\" bila ingin menyinkronkan ulang seluruh stok.";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PengadaanProduk.class);
        if (order) {
            criteria.addOrder(Order.desc("waktuPengadaan"));
        }
        Toko st = scopeToko();
        String kw = searchnama == null ? "" : searchnama.getValue().trim();
        if (!kw.isEmpty()) {
            criteria.createAlias("produk", "pr");
            criteria.add(Restrictions.or(
                    Restrictions.ilike("pr.nama", kw, MatchMode.ANYWHERE),
                    Restrictions.ilike("nomorFaktur", kw, MatchMode.ANYWHERE)));
        }
        if (st != null) {
            criteria.add(Restrictions.eq("toko", st));
        }
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PengadaanRenderer();
    }

    // Tambah tombol "Hitung Ulang Stok" di area aksi filter.
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        super.onAfterInit(comp);
        if (add != null && add.getParent() != null) {
            MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Hitung Ulang Stok", "/img/refresh.gif");
            btn.setTooltiptext("Sinkronkan stok produk dari barang masuk, keluar, dan opname");
            btn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    recomputeStokAll();
                    MyMessageboxConfig.show("Stok produk berhasil dihitung ulang.", "Berhasil",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                }
            });
            btn.setParent(add.getParent());
        }
    }

    // ======================== Form UI ========================

    @Override
    protected void buildFormContent(MyWindow window, final PengadaanProduk e) throws Exception {
        Toko st = scopeToko();

        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        ZkCompat.setFlex(center, true);
        center.setParent(borderlayout);

        Div cardWrap = new Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);
        Grid formGrid = new Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);
        Rows rows = new Rows();
        rows.setParent(formGrid);
        FormBuilder fb = new FormBuilder(rows);

        produk = new Combobox();
        produk.setWidth("100%");
        produk.setReadonly(true);
        if (st != null) {
            Common.insertCombo(produk, "nama", Produk.class, Restrictions.eq("aktif", true),
                    Restrictions.eq("toko", st));
        } else {
            Common.insertCombo(produk, "nama", Produk.class, Restrictions.eq("aktif", true));
        }
        Common.selectComboItem(produk, e.getProduk());
        fb.addRow("Produk *", produk, "Barang yang masuk/dibeli");

        nomorFaktur = new org.zkoss.zul.Textbox(e.getNomorFaktur());
        nomorFaktur.setWidth("100%");
        fb.addRow("Nomor Faktur", nomorFaktur, "Nomor nota dari pemasok (opsional)");

        namaSupplier = new org.zkoss.zul.Textbox(e.getNamaSupplier());
        namaSupplier.setWidth("100%");
        fb.addRow("Pemasok / Supplier", namaSupplier, "Nama pemasok barang");

        qty = new MyDoublebox(e.getId() == null ? null : e.getQty());
        fb.addRow("Jumlah Masuk *", qty, "Banyaknya unit yang masuk");

        hargaBeliSatuan = new MyDoublebox(e.getId() == null ? null : e.getHargaBeliSatuan());
        fb.addRow("Harga Beli Satuan (Rp) *", hargaBeliSatuan, "Harga modal per unit");

        waktuPengadaan = new Datebox(e.getWaktuPengadaan());
        waktuPengadaan.setFormat("dd-MM-yyyy");
        waktuPengadaan.setWidth("100%");
        fb.addRow("Tanggal Masuk", waktuPengadaan, "Tanggal barang diterima");

        keterangan = new org.zkoss.zul.Textbox(e.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(2);
        fb.addRow("Keterangan", keterangan, "Catatan tambahan bila perlu");

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);
        Toolbar toolbar = new Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup tanpa menyimpan");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan & perbarui stok");
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

    // ======================== Save + recompute stok ========================

    public boolean onSave(Event event) throws Exception {
        if (produk.getSelectedItem() == null || produk.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Produk belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Produk pada kolom yang tersedia; (2) pilih salah satu Produk yang sesuai; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (qty.getValue() == null || qty.getValue() <= 0) {
            MyMessageboxConfig.show("Mohon maaf, Jumlah Masuk belum diisi atau bernilai 0. Langkah yang dapat dilakukan: (1) isi kolom Jumlah Masuk dengan angka yang lebih dari 0; (2) pastikan jumlah sesuai dengan barang yang diterima; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (hargaBeliSatuan.getValue() == null || hargaBeliSatuan.getValue() <= 0) {
            MyMessageboxConfig.show("Mohon maaf, Harga Beli Satuan belum diisi atau bernilai 0. Langkah yang dapat dilakukan: (1) isi kolom Harga Beli Satuan dengan nilai yang lebih dari 0; (2) pastikan nilai sesuai dengan harga pembelian aktual; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        PengadaanProduk e = currentEntity;
        if (e.getId() != null) {
            e = (PengadaanProduk) session.load(PengadaanProduk.class, e.getId());
            currentEntity = e;
        }
        Produk pr = (Produk) produk.getSelectedItem().getValue();
        e.setProduk(pr);
        e.setToko(pr.getToko());
        e.setNomorFaktur(nomorFaktur.getValue());
        e.setNamaSupplier(namaSupplier.getValue());
        e.setQty(qty.getValue());
        e.setHargaBeliSatuan(hargaBeliSatuan.getValue());
        e.setTotalHarga(qty.getValue() * hargaBeliSatuan.getValue());
        e.setWaktuPengadaan(waktuPengadaan.getValue() == null ? new Date() : waktuPengadaan.getValue());
        e.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, e);

        // Hitung ulang stok produk yang bersangkutan.
        if (pr != null && pr.getId() != null) {
            recomputeStokProduk(pr.getId());
        }
        return true;
    }

    /**
     * Hitung ulang stok satu produk = Σ masuk(pengadaan) + Σ opname − Σ keluar(penjualan),
     * harga beli = pengadaan terakhir, harga jual dinaikkan bila lebih kecil dari harga beli.
     */
    private void recomputeStokProduk(Long produkId) throws Exception {
        // Logika dipindah ke StokKantinUtil agar dipakai ulang oleh Stok Opname & sinkronisasi.
        StokKantinUtil.recomputeStokProduk(produkId);
    }

    private void recomputeStokAll() throws Exception {
        Toko st = scopeToko();
        StokKantinUtil.recomputeStokToko(st == null ? null : st.getId());
    }

    private double scalar(String sql) {
        try {
            Object o = HibernateUtil.currentSession().createSQLQuery(sql).uniqueResult();
            return o == null ? 0.0 : ((Number) o).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ======================== Renderer ========================

    class PengadaanRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PengadaanProduk e = (PengadaanProduk) arg1;

            new Label(e.getWaktuPengadaan() == null ? ""
                    : new java.text.SimpleDateFormat("dd MMM yyyy").format(e.getWaktuPengadaan())).setParent(arg0);
            new Label(e.getNomorFaktur()).setParent(arg0);
            String namaProduk = e.getProduk() == null ? "(produk dihapus)" : e.getProduk().getNama();
            RevisiHelper.createNewRevisi(PengadaanProduk.class, e, namaProduk).setParent(arg0);
            new Label(e.getNamaSupplier()).setParent(arg0);
            new Label(Common.numberFormat.get().format(e.getQty())).setParent(arg0);
            new Label("Rp " + Common.numberFormat.get().format(e.getHargaBeliSatuan())).setParent(arg0);
            Label tot = new Label("Rp " + Common.numberFormat.get().format(e.getTotalHarga()));
            tot.setStyle("font-weight:700;color:#16a34a;");
            tot.setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, e, PengadaanKantinAction.this).setParent(arg0);
        }
    }
}
