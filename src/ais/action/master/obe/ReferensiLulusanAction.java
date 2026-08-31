package ais.action.master.obe;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.LampiranLain;
import ais.database.model.obe.ReferensiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyWindow;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 * Controller/action ZK untuk referensi lulusan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * ObeBaseAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox kode}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code Hbox hboxLampiran}, {@code LampiranLain lainMahasiswa}, {@code
 * ReferensiLulusan referensiLulusan}, {@code EventListener eventListener}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initForm()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaReferensiLulusanDuplikat()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onAdd()}, {@code onAddExternal()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see ObeBaseAction
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class ReferensiLulusanAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;
    private Hbox    hboxLampiran;

    // File lampiran (diisi callback upload)
    protected LampiranLain lainMahasiswa;

    private ReferensiLulusan referensiLulusan;
    private EventListener    eventListener;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "kode", "nama", "keterangan", "aktif"};
        initCommon(comp, ReferensiLulusan.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    public void onAdd(Event event) throws Exception {
        initForm(new ReferensiLulusan());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((ReferensiLulusan) obj);
    }

    public static void onAddExternal(Event event, EventListener listener, ReferensiLulusan rl)
            throws Exception {
        ReferensiLulusanAction action = new ReferensiLulusanAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
                .appendChild(action.addWindow);
        action.addWindow.setHeight("98%");
        action.addWindow.setWidth("550px");
        action.initForm(rl);
        action.addWindow.setVisible(true);
        action.addWindow.onModal();
    }

    private void initForm(ReferensiLulusan rl) {
        // ReferensiLulusan tidak punya perguruanTinggi dari initCommon bila dipanggil static
        if (perguruanTinggi == null) {
            perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
        }
        this.referensiLulusan = rl;

        FormContext ctx = buildFormBorderlayout("Pendataan Referensi");
        Rows rows = ctx.rows;

        kode = new Textbox(rl.getKode());
        addFormRow(rows, "Kode Referensi", kode);
        kode.setWidth("90%");

        nama = new Textbox(rl.getNama());
        addFormRow(rows, "Nama Referensi", nama);
        nama.setWidth("90%");

        keterangan = new Textbox(rl.getKeterangan());
        keterangan.setWidth("90%");
        keterangan.setRows(3);
        addFormRow(rows, "Keterangan / Link", keterangan);

        // Lampiran
        lainMahasiswa  = null;
        hboxLampiran   = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hboxLampiran,
                rl.getId(), ReferensiLulusan.class.getName(),
                "Lampiran Referensi", false, new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        lainMahasiswa = (LampiranLain) arg0.getData();
                    }
                });
        addFormRow(rows, "Lampiran Referensi", hboxLampiran);

        Common.initKeterangan(rows,
                "Jika file lampiran referensi lebih dari satu file, zip dulu semua file tersebut");

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                    if (eventListener != null) {
                        eventListener.onEvent(new Event("", addWindow,
                                ReferensiLulusanAction.this.referensiLulusan));
                    }
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Referensi Lulusan")) return false;

        if (checkNamaReferensiLulusanDuplikat()) {
            MyMessageboxConfig.show("Nama Referensi Lulusan sudah ada di database", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        if (referensiLulusan.getId() != null) {
            referensiLulusan = (ReferensiLulusan) session.load(ReferensiLulusan.class, referensiLulusan.getId());
        }
        referensiLulusan.setKode(kode.getValue());
        referensiLulusan.setNama(nama.getValue());
        referensiLulusan.setKeterangan(keterangan.getValue());
        referensiLulusan.setPerguruanTinggi(perguruanTinggi);

        Common.refreshSaveOrUpdate(session, referensiLulusan);

        // Update ref file lampiran ke ID entity yang baru disimpan
        if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
            try {
                Session streamSession = StreamingHibernateUtil.getInstance().currentSession();
                streamSession.refresh(lainMahasiswa);
                lainMahasiswa.setRef(referensiLulusan.getId());
                streamSession.getTransaction().begin();
                streamSession.update(lainMahasiswa);
                streamSession.getTransaction().commit();
                StreamingHibernateUtil.getInstance().closeSession();
            } catch (Exception e) {
                StreamingHibernateUtil.getInstance().rollbackTransaction();
                Common.tampilErrorJikaAdmin(e);
            }
        }
        return true;
    }

    /** Cek duplikasi nama referensi (exclude ID sendiri saat edit). */
    public boolean checkNamaReferensiLulusanDuplikat() {
        Session session = HibernateUtil.currentSession();
        Integer count = ((Number) session.createCriteria(ReferensiLulusan.class)
                .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(referensiLulusan.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", referensiLulusan.getId()))
                .setProjection(Projections.rowCount())
                .uniqueResult()).intValue();
        return count > 0;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(ReferensiLulusan.class)
                .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            c.addOrder(Order.asc("kode"));
            c.addOrder(Order.asc("nama"));
        }
        if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
            c.add(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        }
        return c;
    }

    @Override
    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        executeSearch(initCriteria(false), initCriteria(true), new ReferensiLulusanRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    /**
     * Renderer lokal untuk layar/komponen {@link ReferensiLulusanAction}. Kelas ini menerjemahkan satu item data
     * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link ReferensiLulusanAction} dan dapat mengakses
     * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see ReferensiLulusanAction
     */
    class ReferensiLulusanRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ReferensiLulusan rl = (ReferensiLulusan) obj;

            new Label(rl.getKode()).setParent(row);

            Vbox vbox = (Vbox) namaCellRingkas(ReferensiLulusan.class, rl, rl.getNama());
            vbox.setParent(row);

            // Lampiran inline di baris grid
            Hbox lampBox = new Hbox();
            lampBox.setParent(vbox);
            LampiranLain.createDownloadUploadFileLain(lampBox,
                    rl.getId(), ReferensiLulusan.class.getName(),
                    "Lampiran", false, null, null, false, false, false, false);

            ringkasanKeterangan(rl.getKeterangan()).setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(rl.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    rl.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(rl);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, rl, ReferensiLulusanAction.this).setParent(row);
        }
    }
}
