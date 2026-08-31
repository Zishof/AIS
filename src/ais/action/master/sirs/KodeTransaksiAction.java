package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Aksi CRUD (via kerangka {@link GenericCrudAction}) untuk kelola master data
 * {@link KodeTransaksiMedis} (kode transaksi medis) pada modul SIRS: daftar dengan pencarian
 * kode dan nama, formulir tambah/ubah (kode, nama, jenis Penambahan/Pengurangan, keterangan),
 * dengan validasi kode dan nama masing-masing tidak boleh duplikat.
 */
public class KodeTransaksiAction extends GenericCrudAction<KodeTransaksiMedis> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search field (auto-wired from ZUL)
    private MyTextbox searchkode;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private Combobox jenis;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    /** @return {@link KodeTransaksiMedis}, kelas entitas yang dikelola aksi ini. */
    @Override
    protected Class<KodeTransaksiMedis> getEntityClass() { return KodeTransaksiMedis.class; }

    /** @return instans {@link KodeTransaksiMedis} kosong untuk formulir tambah data baru. */
    @Override
    protected KodeTransaksiMedis createNewEntity() { return new KodeTransaksiMedis(); }

    /** @return judul jendela daftar/aksi ini. */
    @Override
    protected String getWindowTitle() { return "Pendataan Kode Transaksi"; }

    /** @return kriteria pencarian {@link KodeTransaksiMedis} berdasarkan kode dan nama (ILIKE), diurutkan menurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KodeTransaksiMedis.class)
                .add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    /** @return renderer baris tabel {@link KodeTransaksiRenderer} untuk daftar kode transaksi. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new KodeTransaksiRenderer();
    }

    // ======================== Form content ========================

    /** Menyusun formulir tambah/ubah (kode, nama, jenis Penambahan/Pengurangan, keterangan) beserta tombol Batal/Simpan pada jendela modal. */
    @Override
    protected void buildFormContent(MyWindow window, final KodeTransaksiMedis kodeTransaksi) throws Exception {
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

        kode = new MyTextbox(kodeTransaksi.getKode() == null ? "" : kodeTransaksi.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        nama = new MyTextbox(kodeTransaksi.getNama() == null ? "" : kodeTransaksi.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama", nama);

        jenis = new Combobox();
        Comboitem comboitem = new Comboitem("Penambahan (+)");
        comboitem.setValue(KodeTransaksiMedis.PENAMBAHAN);
        jenis.appendChild(comboitem);
        comboitem = new Comboitem("Pengurangan (-)");
        comboitem.setValue(KodeTransaksiMedis.PENGURANGAN);
        jenis.appendChild(comboitem);
        Common.selectComboItem(jenis, kodeTransaksi.getJenis());
        jenis.setWidth("100%");
        fb.addRow("Jenis", jenis);

        keterangan = new MyTextbox(kodeTransaksi.getKeterangan() == null ? "" : kodeTransaksi.getKeterangan());
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
     * Memvalidasi kode, nama, dan jenis wajib diisi/dipilih, serta kode dan nama masing-masing
     * belum terdaftar (lewat {@link #checkKodeTransaksi()}/{@link #checkNamaKodeTransaksi()}),
     * lalu menyimpan (buat baru atau perbarui) entitas {@link KodeTransaksiMedis}.
     *
     * @param event event pemicu (tidak dipakai)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (jendela tetap terbuka)
     */
    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Transaksi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode Transaksi pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kode terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Kode Transaksi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Kode Transaksi pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenis.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Kode Transaksi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Kode Transaksi pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah jenis ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkKodeTransaksi()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Transaksi yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan kode transaksi yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaKodeTransaksi()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Kode Transaksi yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        KodeTransaksiMedis entity = currentEntity;
        if (entity.getId() != null) {
            entity = (KodeTransaksiMedis) session.load(KodeTransaksiMedis.class, entity.getId());
            currentEntity = entity;
        }
        entity.setJenis((Integer) jenis.getSelectedItem().getValue());
        entity.setKode(kode.getValue().trim());
        entity.setNama(nama.getValue().trim());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** @return {@code true} bila kode pada formulir sudah dipakai kode transaksi lain (mengecualikan record yang sedang diedit). */
    public Boolean checkKodeTransaksi() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(KodeTransaksiMedis.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    /** @return {@code true} bila nama pada formulir sudah dipakai kode transaksi lain (mengecualikan record yang sedang diedit). */
    public Boolean checkNamaKodeTransaksi() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(KodeTransaksiMedis.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris tabel: kode, nama (via {@link RevisiHelper}), label jenis (Penambahan/Pengurangan), keterangan, dan tombol ubah/hapus. */
    class KodeTransaksiRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final KodeTransaksiMedis kodeTransaksi = (KodeTransaksiMedis) arg1;

            new Label(kodeTransaksi.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(KodeTransaksiMedis.class, kodeTransaksi,
                    kodeTransaksi.getNama()).setParent(arg0);
            new Label(kodeTransaksi.getJenis() == null ? "" : kodeTransaksi.getJenis()
                    .equals(KodeTransaksiMedis.PENAMBAHAN) ? "Penambahan (+)" : kodeTransaksi.getJenis()
                    .equals(KodeTransaksiMedis.PENGURANGAN) ? "Pengurangan (-)" : "").setParent(arg0);
            new Label(kodeTransaksi.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, kodeTransaksi, KodeTransaksiAction.this).setParent(arg0);
        }
    }
}
