package ais.action.master.obe;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.obe.ProfesiLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import java.util.HashMap;
import java.util.List;

/**
 * Controller/action ZK untuk profil lulusan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * ObeBaseAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox kode}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code Combobox
 * profesiLulusan}, {@code HashMap selectedReferensiLulusan}, {@code Row rowReferensi}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code init()}, {@code initForm()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code loadReferensiCheckboxes()}, {@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain
 * lain ({@code onAdd()}, {@code onAddExternal()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class ProfilLulusanAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox  kode;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox fakultas;
    private Combobox jurusan;
    private Combobox profesiLulusan;

    // Multi-select referensi
    private HashMap<Long, ReferensiLulusan> selectedReferensiLulusan;
    private Row rowReferensi;

    private ProfilLulusan profilLulusan;
    private EventListener eventListener;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "jurusan", "kode", "nama", "referensi", "keterangan", "profesiLulusan", "aktif"};
        initCommon(comp, ProfilLulusan.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    public void onAdd(Event event) throws Exception {
        initForm(new ProfilLulusan());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((ProfilLulusan) obj);
    }

    public static void onAddExternal(Event event, EventListener listener, ProfilLulusan pl)
            throws Exception {
        ProfilLulusanAction action = new ProfilLulusanAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
                .appendChild(action.addWindow);
        action.addWindow.setHeight("98%");
        action.addWindow.setWidth("550px");
        action.initForm(pl);
        action.lockFakultasJurusanIfSelected(action.fakultas, action.jurusan);
        action.addWindow.setVisible(true);
        action.addWindow.onModal();
    }

    private void initForm(final ProfilLulusan pl) throws Exception {
        this.profilLulusan = pl;

        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        FormContext ctx = buildFormBorderlayout("Pendataan Profil Lulusan");
        Rows rows = ctx.rows;

        kode = new Textbox(pl.getKode());
        nama = new Textbox(pl.getNama());
        addKodeNamaFakultasJurusanRows(rows, kode, nama, fakultas, jurusan,
                pl.getJurusan(), "Kode Profil Lulusan", "Nama Profil Lulusan");

        // Profesi — diisi ulang setiap jurusan berubah
        profesiLulusan = new Combobox();
        profesiLulusan.setWidth("90%");
        profesiLulusan.setReadonly(true);

        final EventListener jurusanListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Jurusan j = isUnselected(jurusan) ? null
                        : (Jurusan) jurusan.getSelectedItem().getValue();
                Common.insertComboDanSemua(profesiLulusan,
                        new String[]{"nama", "kode"}, "keterangan", ProfesiLulusan.class,
                        Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                        Restrictions.eq("perguruanTinggi", perguruanTinggi),
                        Restrictions.eq("jurusan", j));
                Common.selectComboItem(profesiLulusan, profilLulusan.getProfesiLulusan());
            }
        };
        jurusanListener.onEvent(null);
        jurusan.addEventListener("onChange", jurusanListener);
        addFormRow(rows, "Profesi", profesiLulusan);

        keterangan = addKeteranganRow(rows, pl.getKeterangan());

        // Baris multi-select referensi (span 2 kolom)
        rowReferensi = new MyFormRow();
        ZkCompat.setSpans(rowReferensi, "2");
        rowReferensi.setParent(rows);

        selectedReferensiLulusan = new HashMap<Long, ReferensiLulusan>();
        loadReferensiCheckboxes();

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                    if (eventListener != null) {
                        eventListener.onEvent(new Event("", addWindow,
                                ProfilLulusanAction.this.profilLulusan));
                    }
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    /** Memuat daftar ReferensiLulusan dan merender checkbox-nya. */
    private void loadReferensiCheckboxes() {
        List<ReferensiLulusan> refs = ConstantValues.simpleList(
                HibernateUtil.currentSession().createCriteria(ReferensiLulusan.class)
                        .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                        .addOrder(Order.asc("nama"))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                ReferensiLulusan.class);

        selectedReferensiLulusan.clear();
        for (String d : profilLulusan.getReferensi().split(",")) {
            try {
                if (!d.trim().isEmpty()) {
                    Long id = Long.parseLong(d.trim());
                    ReferensiLulusan ref = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(), id);
                    if (ref != null && !selectedReferensiLulusan.containsKey(id))
                        selectedReferensiLulusan.put(id, ref);
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/ProfilLulusanAction.java:171"); }
        }

        renderCheckboxGrid(rowReferensi, refs, selectedReferensiLulusan,
                "Pilih Referensi (SN-Dikti/KKNI/Peta Okupasi)", new CheckboxLabelFn<ReferensiLulusan>() {
                    @Override public String label(ReferensiLulusan r) { return r.getNama(); }
                });
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Profil Lulusan")) return false;
        if (!validateJurusanRequired(jurusan)) return false;

        Session session = HibernateUtil.currentSession();
        if (profilLulusan.getId() != null) {
            profilLulusan = (ProfilLulusan) session.load(ProfilLulusan.class, profilLulusan.getId());
        }
        profilLulusan.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
        profilLulusan.setProfesiLulusan(isUnselected(profesiLulusan) ? null
                : (ProfesiLulusan) profesiLulusan.getSelectedItem().getValue());
        profilLulusan.setKode(kode.getValue());
        profilLulusan.setNama(nama.getValue());
        profilLulusan.setKeterangan(keterangan.getValue());
        profilLulusan.setReferensi(mapKeysToString(selectedReferensiLulusan));
        profilLulusan.setPerguruanTinggi(perguruanTinggi);

        Common.refreshSaveOrUpdate(session, profilLulusan);
        return true;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    @Override
    public Criteria initCriteria(boolean order) {
        return buildBaseCriteria(HibernateUtil.currentSession(), ProfilLulusan.class,
                order, true, "kode", "nama");
    }

    @Override
    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        executeSearch(initCriteria(false), initCriteria(true), new ProfilLulusanRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    class ProfilLulusanRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ProfilLulusan pl = (ProfilLulusan) obj;

            new Label(pl.getKode()).setParent(row);
            namaCellRingkas(ProfilLulusan.class, pl, pl.getNama()).setParent(row);
            new Label(pl.getJurusan() == null ? "" : pl.getJurusan().getNama()).setParent(row);
            new Label(pl.getProfesiLulusan() == null ? "" : pl.getProfesiLulusan().getNama()).setParent(row);
            ringkasanKeterangan(BahanKajianAction.resolveNamaFromCsv(pl.getReferensi(), ReferensiLulusan.class)).setParent(row);
            ringkasanKeterangan(pl.getKeterangan()).setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(pl.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    pl.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(pl);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, pl, ProfilLulusanAction.this).setParent(row);
        }
    }
}
