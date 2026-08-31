package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.sirs.JenisBiayaLain;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD pendataan {@link JenisBiayaLain} ("Variable Transaksi") pada modul SIRS: variabel
 * biaya lain-lain yang dipetakan ke satu {@link Akun} akunting, dikelompokkan berdasarkan jenis
 * transaksi tempat variabel tersebut dipakai (penjualan, pembelian, penerimaan, pembayaran kasir
 * tunai/non-tunai/asuransi, deposit, setor transaksi penjualan, atau lain-lain — lihat konstanta
 * {@link JenisBiayaLain}). Dibangun di atas kerangka {@link GenericCrudAction}.
 */
public class JenisBiayaLainAction extends GenericCrudAction<JenisBiayaLain> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search field (auto-wired from ZUL)
    private Combobox searchjenis;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private Combobox jenis;
    private AmbilDataAkunBanbox akun;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    /** Kelas entitas yang dikelola: {@link JenisBiayaLain}. */
    @Override
    protected Class<JenisBiayaLain> getEntityClass() { return JenisBiayaLain.class; }

    /** Membuat instance {@link JenisBiayaLain} kosong untuk form tambah data baru. */
    @Override
    protected JenisBiayaLain createNewEntity() { return new JenisBiayaLain(); }

    /** Judul jendela: {@code "Pendataan Variable Transaksi"}. */
    @Override
    protected String getWindowTitle() { return "Pendataan Variable Transaksi"; }

    /** Melengkapi inisialisasi bawaan dengan mengisi pilihan jenis pada combobox filter pencarian. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initJenis(searchjenis);
    }

    /** Menyusun kriteria pencarian {@link JenisBiayaLain}, difilter ilike nama/kode dan exact match jenis, terurut id menurun bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisBiayaLain.class)
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchjenis == null || searchjenis.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.desc("id"));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Penyedia renderer baris grid hasil pencarian: {@link JenisBiayaLainRenderer}. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new JenisBiayaLainRenderer();
    }

    /** Mengisi {@code cb} dengan seluruh nilai konstan jenis transaksi yang dikenal {@link JenisBiayaLain} (penjualan, pembelian, dst.). */
    private void initJenis(Combobox cb) {
        if (cb == null) return;
        addComboItem(cb, JenisBiayaLain.PENJUALAN);
        addComboItem(cb, JenisBiayaLain.PEMBELIAN);
        addComboItem(cb, JenisBiayaLain.PENERIMAAN);
        addComboItem(cb, JenisBiayaLain.PEMBAYARAN_KASIR_TUNAI);
        addComboItem(cb, JenisBiayaLain.PEMBAYARAN_KASIR_BUKAN_TUNAI);
        addComboItem(cb, JenisBiayaLain.PEMBAYARAN_KASIR_ASURANSI);
        addComboItem(cb, JenisBiayaLain.PEMBAYARAN_DEPOSIT);
        addComboItem(cb, JenisBiayaLain.SIMPAN_DEPOSIT);
        addComboItem(cb, JenisBiayaLain.CARA_BAYAR_DEPOSIT);
        addComboItem(cb, JenisBiayaLain.SETOR_TRANSAKSI_PENJUALAN);
        addComboItem(cb, JenisBiayaLain.LAIN);
        cb.setWidth("90%");
    }

    /** Menambahkan satu {@link Comboitem} berlabel/bernilai {@code value} ke {@code cb}. */
    private void addComboItem(Combobox cb, String value) {
        Comboitem item = new Comboitem(value);
        item.setValue(value);
        cb.appendChild(item);
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link JenisBiayaLain}: kode (dibangkitkan otomatis, tidak dapat diedit), nama, jenis, akun tujuan, dan keterangan, beserta tombol Batal dan Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final JenisBiayaLain jenisBiayaLain) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card wrapper
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

        kode = new MyTextbox(jenisBiayaLain.getKode() == null
                ? Common.generateCode(JenisBiayaLain.class, 8) : jenisBiayaLain.getKode());
        kode.setWidth("100%");
        kode.setDisabled(true);
        fb.addRow("Kode Variable Transaksi", kode);

        nama = new MyTextbox(jenisBiayaLain.getNama() == null ? "" : jenisBiayaLain.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Variable Transaksi", nama);

        jenis = new Combobox();
        initJenis(jenis);
        Common.selectComboItem(jenis, jenisBiayaLain.getJenis());
        fb.addRow("Jenis Variable Transaksi", jenis);

        akun = new AmbilDataAkunBanbox();
        akun.setValue(jenisBiayaLain.getAkun() == null ? "" : jenisBiayaLain.getAkun().toString());
        akun.setAttribute("akun", jenisBiayaLain.getAkun());
        akun.setWidth("100%");
        fb.addRow("Variable transaksi ini akan masuk ke akun", akun);

        keterangan = new MyTextbox(jenisBiayaLain.getKeterangan() == null ? "" : jenisBiayaLain.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        // South + Toolbar
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

    /**
     * Memvalidasi dan menyimpan data {@link JenisBiayaLain}: menolak bila nama kosong, jenis
     * belum dipilih, atau akun tujuan belum dipilih, lalu menyimpan/memperbarui entitas (kode
     * dibangkitkan otomatis via {@link Common#generateCode} untuk data baru).
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Variabel Transaksi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Variabel Transaksi pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenis.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Variabel Transaksi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Variabel Transaksi pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah jenis ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (akun.getAttribute("akun") == null) {
            MyMessageboxConfig.show("Mohon maaf, Akun wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Akun melalui kolom pencarian akun yang tersedia; (2) pastikan Akun tidak dikosongkan; (3) simpan kembali data setelah Akun ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        JenisBiayaLain entity = currentEntity;
        if (entity.getId() != null) {
            entity = (JenisBiayaLain) session.load(JenisBiayaLain.class, entity.getId());
            currentEntity = entity;
        }
        entity.setAkun((Akun) akun.getAttribute("akun"));
        entity.setJenis((String) jenis.getSelectedItem().getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        if (entity.getId() != null) {
            Common.refreshUpdate(session, entity);
        } else {
            entity.setKode(Common.generateCode(JenisBiayaLain.class, 8));
            session.save(entity);
        }
        return true;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid untuk {@link JenisBiayaLain}: kode, nama (dengan tombol riwayat revisi), jenis, akun tujuan, keterangan, dan tombol edit/hapus. */
    class JenisBiayaLainRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JenisBiayaLain jenisBiayaLain = (JenisBiayaLain) arg1;

            new Label(jenisBiayaLain.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(JenisBiayaLain.class, jenisBiayaLain, jenisBiayaLain.getNama()).setParent(arg0);
            new Label(jenisBiayaLain.getJenis()).setParent(arg0);
            new Label(jenisBiayaLain.getAkun().toString()).setParent(arg0);
            new Label(jenisBiayaLain.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, jenisBiayaLain, JenisBiayaLainAction.this).setParent(arg0);
        }
    }
}
