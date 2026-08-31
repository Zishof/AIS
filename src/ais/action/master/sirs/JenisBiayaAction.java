package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.sirs.JenisBiaya;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD (berbasis {@link GenericCrudAction}) untuk data master <b>Jenis Biaya</b> modul SIRS
 * (mis. biaya item, tindakan, alat medis, paket — lihat konstanta {@code JenisBiaya.TIPE_*}).
 * Setiap jenis biaya wajib ditautkan ke satu {@link Akun} akunting tujuan (dipilih lewat
 * {@link AmbilDataAkunKreditBanbox}) tempat nilai biaya ini diposting, serta memiliki flag
 * {@code aktif} dan {@code defaultAktif} (dipakai default saat menu terkait dibuka pertama kali).
 */
public class JenisBiayaAction extends GenericCrudAction<JenisBiaya> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private MyTextbox nama;
    private MyTextbox variable;
    private Combobox tipe;
    private AmbilDataAkunKreditBanbox akun;
    private Checkbox aktif;
    private Checkbox defaultAktif;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    /** Mengembalikan kelas entitas yang dikelola layar ini: {@link JenisBiaya}. */
    @Override
    protected Class<JenisBiaya> getEntityClass() { return JenisBiaya.class; }

    /** Membuat instance {@link JenisBiaya} kosong untuk form tambah data baru. */
    @Override
    protected JenisBiaya createNewEntity() { return new JenisBiaya(); }

    /** Mengembalikan judul jendela form: {@code "Pendataan Jenis Biaya"}. */
    @Override
    protected String getWindowTitle() { return "Pendataan Jenis Biaya"; }

    /** Membangun kriteria pencarian jenis biaya, diurutkan berdasarkan id terbaru, disaring berdasarkan kecocokan sebagian nama pada kotak pencarian bila diisi. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisBiaya.class)
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    /** Membuat perender baris grid pencarian jenis biaya: {@link JenisBiayaRenderer}. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new JenisBiayaRenderer();
    }

    // ======================== Form content ========================

    /** Membangun tata letak form tambah/edit jenis biaya (nama, tipe, variabel, akun tujuan, flag aktif/default, keterangan) dengan toolbar simpan/batal di dalam {@code window}. */
    @Override
    protected void buildFormContent(MyWindow window, final JenisBiaya jenisBiaya) throws Exception {
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

        nama = new MyTextbox(jenisBiaya.getNama() == null ? "" : jenisBiaya.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Jenis Biaya", nama);

        tipe = new Combobox();
        Comboitem c = new Comboitem(JenisBiaya.TIPE_ITEM);
        c.setValue(JenisBiaya.TIPE_ITEM);
        tipe.appendChild(c);
        c = new Comboitem(JenisBiaya.TIPE_TINDAKAAN);
        c.setValue(JenisBiaya.TIPE_TINDAKAAN);
        tipe.appendChild(c);
        c = new Comboitem(JenisBiaya.TIPE_ALAT_MEDIS);
        c.setValue(JenisBiaya.TIPE_ALAT_MEDIS);
        tipe.appendChild(c);
        c = new Comboitem(JenisBiaya.TIPE_PAKET);
        c.setValue(JenisBiaya.TIPE_PAKET);
        tipe.appendChild(c);
        Common.selectComboItem(tipe, jenisBiaya.getTipe());
        tipe.setWidth("100%");
        fb.addRow("Tipe Jenis Biaya", tipe);

        variable = new MyTextbox(jenisBiaya.getVariable());
        variable.setWidth("100%");
        fb.addRow("Variable Jenis Biaya", variable);

        akun = new AmbilDataAkunKreditBanbox();
        akun.setValue(jenisBiaya.getAkun() == null ? "" : jenisBiaya.getAkun().toString());
        akun.setAttribute("akun", jenisBiaya.getAkun());
        akun.setWidth("100%");
        fb.addRow("Jenis Biaya ini akan masuk ke akun", akun);

        aktif = new Checkbox();
        aktif.setChecked(jenisBiaya.getAktif());
        fb.addRow("Aktif", aktif);

        defaultAktif = new Checkbox();
        defaultAktif.setChecked(jenisBiaya.getDefaultAktif());
        fb.addRow("Default digunakan", defaultAktif);

        keterangan = new MyTextbox(jenisBiaya.getKeterangan() == null ? "" : jenisBiaya.getKeterangan());
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
     * Memvalidasi (nama wajib, tipe wajib dipilih, akun tujuan wajib dipilih) dan
     * menyimpan/memperbarui data jenis biaya dari isian form saat ini.
     *
     * @param event event pemicu tombol simpan
     * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila validasi gagal
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Jenis Biaya wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Jenis Biaya pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (tipe.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Tipe Jenis Biaya wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Tipe Jenis Biaya pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah tipe ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (akun.getAttribute("akun") == null) {
            MyMessageboxConfig.show("Mohon maaf, Akun wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Akun melalui kolom pencarian akun yang tersedia; (2) pastikan Akun tidak dikosongkan; (3) simpan kembali data setelah Akun ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        JenisBiaya entity = currentEntity;
        if (entity.getId() != null) {
            entity = (JenisBiaya) session.load(JenisBiaya.class, entity.getId());
            currentEntity = entity;
        }
        entity.setVariable(variable.getValue());
        entity.setTipe((String) tipe.getSelectedItem().getValue());
        entity.setAkun((Akun) akun.getAttribute("akun"));
        entity.setAktif(aktif.isChecked());
        entity.setDefaultAktif(defaultAktif.isChecked());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    /** Perender baris grid pencarian jenis biaya: menampilkan nama (dengan tautan riwayat revisi), tipe, variabel, akun tujuan, status aktif/default, keterangan, dan tombol ubah/hapus. */
    class JenisBiayaRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JenisBiaya jenisBiaya = (JenisBiaya) arg1;

            RevisiHelper.createNewRevisi(JenisBiaya.class, jenisBiaya, jenisBiaya.getNama()).setParent(arg0);
            new Label(jenisBiaya.getTipe()).setParent(arg0);
            new Label(jenisBiaya.getVariable()).setParent(arg0);
            new Label(jenisBiaya.getAkun().toString()).setParent(arg0);
            new Label(jenisBiaya.getAktif() ? "Ya" : "Tidak").setParent(arg0);
            new Label(jenisBiaya.getDefaultAktif() ? "Ya" : "Tidak").setParent(arg0);
            new Label(jenisBiaya.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, jenisBiaya, JenisBiayaAction.this).setParent(arg0);
        }
    }
}
