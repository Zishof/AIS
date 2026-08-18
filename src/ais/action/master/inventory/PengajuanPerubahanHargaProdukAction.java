package ais.action.master.inventory;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
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
import ais.database.model.inventory.PengajuanPerubahanHargaProduk;
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
 * Pengajuan Perubahan Harga Produk — konversi dari {@code modul/kantin/barang/pengajuan_perubahan_harga_produk}.
 *
 * <p>Mencatat usulan harga beli/jual baru untuk sebuah produk. Saat <b>disetujui</b>, harga pada
 * produk ikut diperbarui otomatis. Memakai {@link GenericCrudAction}.</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat &amp; mengajukan
 * perubahan harga untuk produk tokonya sendiri.</p>
 */
public class PengajuanPerubahanHargaProdukAction extends GenericCrudAction<PengajuanPerubahanHargaProduk> {

    private static final long serialVersionUID = 1L;

    private Combobox produk;
    private MyDoublebox hargaBeli;
    private MyDoublebox hargaJual;
    private Datebox tanggal;

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
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/PengajuanPerubahanHargaProdukAction.java:70");
            }
        }
        return scopeTokoCache;
    }

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PengajuanPerubahanHargaProduk> getEntityClass() {
        return PengajuanPerubahanHargaProduk.class;
    }

    @Override
    protected PengajuanPerubahanHargaProduk createNewEntity() {
        return new PengajuanPerubahanHargaProduk();
    }

    @Override
    protected String getWindowTitle() {
        return "Pengajuan Perubahan Harga";
    }

    @Override
    protected String getIntroTitle() {
        return "Pengajuan Perubahan Harga Produk";
    }

    @Override
    protected String getIntroDescription() {
        return "Ajukan harga beli/jual baru untuk sebuah produk. Setelah disetujui, harga produk langsung diperbarui "
                + "— sehingga perubahan harga tercatat rapi dan bisa ditelusuri.";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PengajuanPerubahanHargaProduk.class);
        if (order) {
            criteria.addOrder(Order.desc("id"));
        }
        Toko st = scopeToko();
        String kw = searchnama == null ? "" : searchnama.getValue().trim();
        boolean needAlias = st != null || !kw.isEmpty();
        if (needAlias) {
            criteria.createAlias("produk", "pr");
        }
        if (!kw.isEmpty()) {
            criteria.add(Restrictions.ilike("pr.nama", kw, MatchMode.ANYWHERE));
        }
        if (st != null) {
            criteria.add(Restrictions.eq("pr.toko", st));
        }
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PengajuanRenderer();
    }

    // ======================== Form UI ========================

    @Override
    protected void buildFormContent(MyWindow window, final PengajuanPerubahanHargaProduk e) throws Exception {
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
        fb.addRow("Produk *", produk, "Produk yang harganya akan diubah");

        // Tampilkan harga produk saat ini sebagai acuan.
        if (e.getProduk() != null) {
            fb.addRow("Harga Saat Ini",
                    new Label("Beli Rp " + Common.numberFormat.get().format(e.getProduk().getHargaBeli())
                            + "  ·  Jual Rp " + Common.numberFormat.get().format(e.getProduk().getHargaJual())),
                    "Harga produk yang berlaku sekarang");
        }

        hargaBeli = new MyDoublebox(e.getId() == null ? null : e.getHargaBeli());
        fb.addRow("Harga Beli Baru", hargaBeli, "Usulan harga beli (modal) yang baru");

        hargaJual = new MyDoublebox(e.getId() == null ? null : e.getHargaJual());
        fb.addRow("Harga Jual Baru *", hargaJual, "Usulan harga jual yang baru");

        tanggal = new Datebox(e.getTanggal());
        tanggal.setFormat("dd-MM-yyyy");
        tanggal.setWidth("100%");
        fb.addRow("Tanggal Pengajuan", tanggal, "Tanggal usulan diajukan");

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
        save.setTooltiptext("Simpan pengajuan");
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
        if (produk.getSelectedItem() == null || produk.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Produk belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Produk pada kolom yang tersedia; (2) pilih salah satu Produk yang akan diajukan perubahan harganya; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (hargaJual.getValue() == null || hargaJual.getValue() <= 0) {
            MyMessageboxConfig.show("Mohon maaf, Harga Jual Baru belum diisi atau bernilai 0. Langkah yang dapat dilakukan: (1) isi kolom Harga Jual Baru dengan nilai yang lebih dari 0; (2) pastikan harga yang diajukan sesuai kebutuhan; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        PengajuanPerubahanHargaProduk e = currentEntity;
        if (e.getId() != null) {
            e = (PengajuanPerubahanHargaProduk) session.load(PengajuanPerubahanHargaProduk.class, e.getId());
            currentEntity = e;
        }
        e.setProduk((Produk) produk.getSelectedItem().getValue());
        e.setHargaBeli(hargaBeli.getValue());
        e.setHargaJual(hargaJual.getValue());
        e.setTanggal(tanggal.getValue() == null ? new Date() : tanggal.getValue());
        Common.refreshSaveOrUpdate(session, e);
        return true;
    }

    // ======================== Approval ========================

    private void onSetujui(final PengajuanPerubahanHargaProduk pe) throws Exception {
        Session session = HibernateUtil.currentSession();
        PengajuanPerubahanHargaProduk e = (PengajuanPerubahanHargaProduk) session
                .load(PengajuanPerubahanHargaProduk.class, pe.getId());
        e.setTanggalDisetujui(new Date());
        e.setDisetujuiOleh(Common.getCurrentUser());

        // Terapkan harga baru ke produk.
        Produk pr = e.getProduk();
        if (pr != null && pr.getId() != null) {
            Produk prod = (Produk) session.load(Produk.class, pr.getId());
            if (e.getHargaBeli() != null && e.getHargaBeli() > 0) {
                prod.setHargaBeli(e.getHargaBeli());
            }
            prod.setHargaJual(e.getHargaJual());
            Common.refreshSaveOrUpdate(session, prod);
        }
        Common.refreshSaveOrUpdate(session, e);
        onSearchDefault(null);
        MyMessageboxConfig.show("Pengajuan disetujui dan harga produk telah diperbarui.", "Berhasil",
                MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
    }

    // ======================== Renderer ========================

    class PengajuanRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PengajuanPerubahanHargaProduk e = (PengajuanPerubahanHargaProduk) arg1;

            String namaProduk = e.getProduk() == null ? "(produk dihapus)" : e.getProduk().getNama();
            RevisiHelper.createNewRevisi(PengajuanPerubahanHargaProduk.class, e, namaProduk).setParent(arg0);

            new Label("Rp " + Common.numberFormat.get().format(e.getHargaBeli())).setParent(arg0);
            Label lj = new Label("Rp " + Common.numberFormat.get().format(e.getHargaJual()));
            lj.setStyle("font-weight:700;color:#16a34a;");
            lj.setParent(arg0);

            new Label(e.getTanggal() == null ? ""
                    : new java.text.SimpleDateFormat("dd MMM yyyy").format(e.getTanggal())).setParent(arg0);

            boolean disetujui = e.getTanggalDisetujui() != null;
            Label status = new Label(disetujui ? "Disetujui" : "Menunggu");
            status.setStyle("font-weight:800;color:" + (disetujui ? "#16a34a" : "#f97316") + ";");
            status.setParent(arg0);

            // Tombol Setujui (hanya bila belum disetujui & punya hak ubah).
            if (!disetujui && edit) {
                Button setujui = new Button("Setujui");
                setujui.setTooltiptext("Setujui pengajuan dan perbarui harga produk");
                setujui.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event ev) throws Exception {
                        onSetujui(e);
                    }
                });
                setujui.setParent(arg0);
            } else {
                new Label(disetujui ? "✓" : "-").setParent(arg0);
            }

            Common.copyEditDeleteButtons(edit, delete, e, PengajuanPerubahanHargaProdukAction.this).setParent(arg0);
        }
    }
}
