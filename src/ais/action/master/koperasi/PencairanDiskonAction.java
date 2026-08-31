package ais.action.master.koperasi;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.PencairanDiskon;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Pencairan Saldo Diskon (Cashback) Member — konversi dari {@code modul/kantin/diskon/pencairan_diskon}.
 *
 * <p>Mencatat dan mengelola penarikan/pencairan saldo cashback member beserta statusnya
 * (PENDING / BERHASIL / DITOLAK). Memakai {@link GenericCrudAction}.</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat
 * pencairan yang terjadi di tokonya.</p>
 */
public class PencairanDiskonAction extends GenericCrudAction<PencairanDiskon> {

    private static final long serialVersionUID = 1L;

    private static final String[] STATUS = { "PENDING", "BERHASIL", "DITOLAK" };

    // Filter tambahan (di-wire dari ZUL); searchaktif sengaja tidak dipakai (tak ada kolom aktif).
    private Combobox searchstatus;

    // Form fields
    private org.zkoss.zul.Textbox kodePencairan;
    private Combobox anggota;
    private Combobox toko;
    private Combobox caraPembayaran;
    private Combobox status;
    private MyDoublebox nominalCair;
    private Datebox waktuPencairan;
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
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PencairanDiskonAction.java:81");
            }
        }
        return scopeTokoCache;
    }

    // ======================== Abstract implementations ========================

    @Override
    protected Class<PencairanDiskon> getEntityClass() {
        return PencairanDiskon.class;
    }

    @Override
    protected PencairanDiskon createNewEntity() {
        return new PencairanDiskon();
    }

    @Override
    protected String getWindowTitle() {
        return "Pencairan Diskon";
    }

    @Override
    protected String getIntroTitle() {
        return "Pencairan Saldo Cashback Member";
    }

    @Override
    protected String getIntroDescription() {
        return "Catatan penarikan saldo cashback member: siapa yang mencairkan, berapa, lewat apa, dan statusnya. "
                + "Gunakan untuk memantau dan menyetujui/menolak permintaan pencairan.";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PencairanDiskon.class);
        if (order) {
            criteria.addOrder(Order.desc("waktuPencairan"));
        }
        String kw = searchnama == null ? "" : searchnama.getValue().trim();
        if (!kw.isEmpty()) {
            criteria.createAlias("anggotaKoperasi", "ak");
            criteria.add(Restrictions.or(
                    Restrictions.ilike("kodePencairan", kw, MatchMode.ANYWHERE),
                    Restrictions.ilike("ak.nama", kw, MatchMode.ANYWHERE)));
        }
        if (searchstatus != null && searchstatus.getSelectedItem() != null
                && searchstatus.getSelectedItem().getValue() != null) {
            criteria.add(Restrictions.eq("status", (String) searchstatus.getSelectedItem().getValue()));
        }
        Toko st = scopeToko();
        if (st != null) {
            criteria.add(Restrictions.eq("toko", st));
        }
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PencairanRenderer();
    }

    // Isi pilihan status di filter (dipanggil dari hook base setelah inisialisasi standar).
    @Override
    protected void onAfterInit(org.zkoss.zk.ui.Component comp) throws Exception {
        super.onAfterInit(comp);
        if (searchstatus != null && searchstatus.getItems().isEmpty()) {
            Comboitem semua = new Comboitem("Semua Status");
            semua.setValue(null);
            semua.setParent(searchstatus);
            for (String s : STATUS) {
                Comboitem it = new Comboitem(s);
                it.setValue(s);
                it.setParent(searchstatus);
            }
            searchstatus.setSelectedItem(semua);
            searchstatus.setReadonly(true);
        }
    }

    // ======================== Form UI ========================

    @Override
    protected void buildFormContent(MyWindow window, final PencairanDiskon pd) throws Exception {
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

        kodePencairan = new org.zkoss.zul.Textbox(pd.getKodePencairan());
        kodePencairan.setWidth("100%");
        fb.addRow("No. Pencairan", kodePencairan, "Kosongkan untuk dibuat otomatis");

        anggota = new Combobox();
        anggota.setWidth("100%");
        anggota.setReadonly(true);
        Common.insertCombo(anggota, "nama", AnggotaKoperasi.class);
        Common.selectComboItem(anggota, pd.getAnggotaKoperasi());
        fb.addRow("Member *", anggota, "Anggota yang mencairkan saldo cashback");

        toko = new Combobox();
        toko.setWidth("100%");
        toko.setReadonly(true);
        Common.insertComboDanSemua(toko, "nama", Toko.class, Restrictions.eq("aktif", true));
        if (st != null) {
            Common.selectComboItem(true, toko, st);
            toko.setDisabled(true);
        } else {
            Common.selectComboItem(toko, pd.getToko());
        }
        fb.addRow("Toko/Kios", toko, "Tempat pencairan dilakukan (boleh kosong)");

        caraPembayaran = new Combobox();
        caraPembayaran.setWidth("100%");
        caraPembayaran.setReadonly(true);
        Common.insertCombo(caraPembayaran, "nama", CaraPembayaranKoperasi.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(caraPembayaran, pd.getCaraPembayaran());
        fb.addRow("Dicairkan Via *", caraPembayaran, "Cara pencairan: tunai, transfer, potong belanja, dll");

        nominalCair = new MyDoublebox(pd.getNominalCair());
        fb.addRow("Nominal Dicairkan (Rp) *", nominalCair, "Jumlah saldo yang ditarik");

        waktuPencairan = new Datebox(pd.getWaktuPencairan());
        waktuPencairan.setFormat("dd-MM-yyyy");
        waktuPencairan.setWidth("100%");
        fb.addRow("Waktu Pencairan", waktuPencairan, "Tanggal eksekusi pencairan");

        status = new Combobox();
        status.setWidth("100%");
        status.setReadonly(true);
        for (String s : STATUS) {
            Comboitem it = new Comboitem(s);
            it.setValue(s);
            it.setParent(status);
            if (s.equalsIgnoreCase(pd.getStatus())) {
                status.setSelectedItem(it);
            }
        }
        fb.addRow("Status", status, "PENDING = menunggu, BERHASIL = sudah cair, DITOLAK = ditolak");

        keterangan = new org.zkoss.zul.Textbox(pd.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan, "Catatan, mis. ditransfer ke rekening BCA");

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
        save.setTooltiptext("Simpan perubahan");
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
        if (anggota.getSelectedItem() == null || anggota.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, member penerima diskon belum dipilih. Langkah yang dapat dilakukan: (1) pilih member dari daftar yang tersedia; (2) pastikan member sudah terdaftar di sistem; (3) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (caraPembayaran.getSelectedItem() == null || caraPembayaran.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, cara pencairan diskon belum dipilih. Langkah yang dapat dilakukan: (1) pilih cara pencairan (tunai/transfer/saldo) dari daftar; (2) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nominalCair.getValue() == null || nominalCair.getValue() <= 0) {
            MyMessageboxConfig.show("Mohon maaf, nominal pencairan harus lebih dari 0. Langkah yang dapat dilakukan: (1) isi kolom Nominal Pencairan dengan angka lebih dari nol; (2) pastikan saldo diskon member mencukupi; (3) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        PencairanDiskon pd = currentEntity;
        if (pd.getId() != null) {
            pd = (PencairanDiskon) session.load(PencairanDiskon.class, pd.getId());
            currentEntity = pd;
        }

        String kode = kodePencairan.getValue() == null ? "" : kodePencairan.getValue().trim();
        if (kode.isEmpty()) {
            kode = "WD-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        }
        pd.setKodePencairan(kode);
        pd.setAnggotaKoperasi((AnggotaKoperasi) anggota.getSelectedItem().getValue());
        pd.setCaraPembayaran((CaraPembayaranKoperasi) caraPembayaran.getSelectedItem().getValue());
        pd.setNominalCair(nominalCair.getValue());
        pd.setWaktuPencairan(waktuPencairan.getValue() == null ? new Date() : waktuPencairan.getValue());
        pd.setStatus(status.getSelectedItem() == null ? "PENDING" : (String) status.getSelectedItem().getValue());
        pd.setKeterangan(keterangan.getValue());

        Toko st = scopeToko();
        if (st != null) {
            pd.setToko(st);
        } else {
            pd.setToko((Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue()));
        }
        Common.refreshSaveOrUpdate(session, pd);
        return true;
    }

    // ======================== Renderer ========================

    /**
     * Renderer lokal untuk layar/komponen {@link PencairanDiskonAction}. Kelas ini menerjemahkan satu item data
     * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link PencairanDiskonAction} dan dapat mengakses
     * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see PencairanDiskonAction
     */
    class PencairanRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final PencairanDiskon pd = (PencairanDiskon) arg1;

            RevisiHelper.createNewRevisi(PencairanDiskon.class, pd, pd.getKodePencairan()).setParent(arg0);
            new Label(pd.getAnggotaKoperasi() == null ? "" : pd.getAnggotaKoperasi().getNama()).setParent(arg0);
            new Label(pd.getToko() == null ? "-" : pd.getToko().getNama()).setParent(arg0);

            Label nom = new Label("Rp "
                    + Common.numberFormat.get().format(pd.getNominalCair() == null ? 0.0 : pd.getNominalCair()));
            nom.setStyle("font-weight:700;color:#16a34a;");
            nom.setParent(arg0);

            new Label(pd.getCaraPembayaran() == null ? "" : pd.getCaraPembayaran().getNama()).setParent(arg0);

            String s = pd.getStatus();
            Label lblStatus = new Label(s);
            String warna = "BERHASIL".equalsIgnoreCase(s) ? "#16a34a"
                    : ("DITOLAK".equalsIgnoreCase(s) ? "#dc2626" : "#f97316");
            lblStatus.setStyle("font-weight:800;color:" + warna + ";");
            lblStatus.setParent(arg0);

            new Label(pd.getWaktuPencairan() == null ? ""
                    : new java.text.SimpleDateFormat("dd MMM yyyy").format(pd.getWaktuPencairan())).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, pd, PencairanDiskonAction.this).setParent(arg0);
        }
    }
}
