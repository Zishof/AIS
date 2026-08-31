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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Devisi;
import ais.database.model.sirs.Bagian;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Bagian (unit kerja) pada modul SIRS (Sistem Informasi Rumah Sakit),
 * dibangun di atas {@link GenericCrudAction}. Setiap bagian memiliki kode, nama, divisi
 * ({@link Devisi}) induk, dan akun akunting ({@link Akun}) terkait — menjadikan modul ini titik
 * hubung antara struktur organisasi rumah sakit dan struktur akun keuangan.
 *
 * <p>
 * Pencarian daftar difilter berdasarkan kecocokan sebagian nama ({@code ilike ANYWHERE}). Form
 * simpan memvalidasi kode, nama, divisi, dan akun wajib diisi, serta kode tidak duplikat (dicek
 * lewat {@link #checkKodeBagian()}) sebelum menyimpan; baris tabel dirender lewat
 * {@link BagianRenderer} yang menampilkan kode, riwayat revisi, nama divisi, dan nama akun.
 * </p>
 */
public class BagianAction extends GenericCrudAction<Bagian> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private MyTextbox kode;
    private MyTextbox nama;
    private Combobox devisi;
    private AmbilDataAkunBanbox akun;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Bagian> getEntityClass() { return Bagian.class; }

    @Override
    protected Bagian createNewEntity() { return new Bagian(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Bagian"; }

    /** Membangun kriteria pencarian daftar bagian, difilter berdasarkan kecocokan sebagian nama bila {@code searchnama} diisi. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Bagian.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new BagianRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah (kode, nama, divisi, akun, keterangan) beserta toolbar Batal/Simpan pada {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final Bagian bagian) throws Exception {
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

        kode = new MyTextbox(bagian.getKode() == null ? "" : bagian.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Unit (Bagian)", kode);

        nama = new MyTextbox(bagian.getNama() == null ? "" : bagian.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Unit (Bagian)", nama);

        devisi = new Combobox();
        Common.insertCombo(devisi, "nama", "keterangan", Devisi.class);
        Common.selectComboItem(devisi, bagian.getDevisi() == null ? null : bagian.getDevisi());
        devisi.setWidth("100%");
        fb.addRow("Devisi", devisi);

        akun = new AmbilDataAkunBanbox();
        akun.setValue(bagian.getAkun() == null ? "" : bagian.getAkun().getNama());
        akun.setAttribute("akun", bagian.getAkun());
        akun.setWidth("100%");
        fb.addRow("Akun", akun);

        keterangan = new MyTextbox(bagian.getKeterangan() == null ? "" : bagian.getKeterangan());
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
     * Memvalidasi (kode, nama, divisi, dan akun wajib isi; kode tidak duplikat) dan menyimpan
     * (create-or-update) entitas bagian dari isian form.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
     *         peringatan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Bagian wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode Bagian pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Bagian wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Bagian pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (devisi.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Divisi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Divisi pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Divisi ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (akun.getAttribute("akun") == null) {
            MyMessageboxConfig.show("Mohon maaf, Akun wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Akun melalui kolom pencarian akun yang tersedia; (2) pastikan Akun tidak dikosongkan; (3) simpan kembali data setelah Akun ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkKodeBagian()) {
            MyMessageboxConfig.show("Mohon maaf, Kode Bagian yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan kode bagian yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Bagian entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Bagian) session.load(Bagian.class, entity.getId());
            currentEntity = entity;
        }
        entity.setAkun((Akun) akun.getAttribute("akun"));
        entity.setDevisi((Devisi) (devisi.getSelectedItem() == null ? null : devisi.getSelectedItem().getValue()));
        entity.setKode(kode.getValue().trim());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** Mengecek apakah kode pada form sudah dipakai bagian lain (di luar entitas yang sedang diedit). */
    public Boolean checkKodeBagian() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Bagian.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Perenderan satu baris tabel bagian: kode, nama (dengan tautan riwayat revisi), nama divisi, nama akun, keterangan, dan tombol edit/hapus. */
    class BagianRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Bagian bagian = (Bagian) arg1;

            new Label(bagian.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Bagian.class, bagian, bagian.getNama()).setParent(arg0);
            new Label(bagian.getDevisi() == null ? "" : bagian.getDevisi().getNama()).setParent(arg0);
            new Label(bagian.getAkun() == null ? "" : bagian.getAkun().getNama()).setParent(arg0);
            new Label(bagian.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, bagian, BagianAction.this).setParent(arg0);
        }
    }
}
