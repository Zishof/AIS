package ais.action.master.obe;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.obe.ProfesiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyRowRenderer;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

/**
 * Layar CRUD (tambah/ubah/cari/aktif-nonaktifkan) untuk data <b>Profesi Lulusan</b> pada modul OBE
 * (Outcome-Based Education) — daftar profesi yang menjadi tujuan kompetensi lulusan sebuah program
 * studi (mis. "Analis Sistem", "Guru", dsb). Mengikuti pola aksi standar {@link ObeBaseAction}: data
 * wajib terikat ke satu program studi (fakultas + jurusan), form dibangun lewat helper
 * {@code buildFormBorderlayout}/{@code addKodeNamaFakultasJurusanRows}, dan hasil pencarian
 * ditampilkan lewat {@link ProfesiLulusanRenderer} dengan toggle aktif/nonaktif langsung dari grid.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class ProfesiLulusanAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox  kode;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox fakultas;
    private Combobox jurusan;

    private ProfesiLulusan profesiLulusan;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Inisialisasi standar layar ZK: mendaftarkan kolom data ({@code id, kode, nama, jurusan, keterangan, aktif}) yang dikenali komponen ini. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "kode", "nama", "jurusan", "keterangan", "aktif"};
        initCommon(comp, ProfesiLulusan.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    /** Membuka form tambah data profesi lulusan baru (kosong). */
    public void onAdd(Event event) throws Exception {
        initForm(new ProfesiLulusan());
    }

    /** Membuka form edit untuk data profesi lulusan yang sudah ada, dipanggil dari tombol edit pada grid. */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((ProfesiLulusan) obj);
    }

    /** Membangun form tambah/edit profesi lulusan: kode, nama, pilihan fakultas+jurusan, dan keterangan, lengkap dengan toolbar simpan. */
    private void initForm(ProfesiLulusan pl) {
        this.profesiLulusan = pl;

        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        FormContext ctx = buildFormBorderlayout("Pendataan Profesi Lulusan");
        Rows rows = ctx.rows;

        kode = new Textbox(pl.getKode());
        nama = new Textbox(pl.getNama());
        addKodeNamaFakultasJurusanRows(rows, kode, nama, fakultas, jurusan,
                pl.getJurusan(), "Kode Profesi Lulusan", "Nama Profesi Lulusan");

        keterangan = addKeteranganRow(rows, pl.getKeterangan());

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    /**
     * Memvalidasi (nama wajib, jurusan wajib) dan menyimpan/memperbarui data profesi lulusan dari
     * isian form saat ini.
     *
     * @param event event pemicu tombol simpan
     * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila validasi gagal (form tidak ditutup)
     */
    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Profesi Lulusan")) return false;
        if (!validateJurusanRequired(jurusan)) return false;

        Session session = HibernateUtil.currentSession();
        if (profesiLulusan.getId() != null) {
            profesiLulusan = (ProfesiLulusan) session.load(ProfesiLulusan.class, profesiLulusan.getId());
        }
        profesiLulusan.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
        profesiLulusan.setKode(kode.getValue());
        profesiLulusan.setNama(nama.getValue());
        profesiLulusan.setKeterangan(keterangan.getValue());
        profesiLulusan.setPerguruanTinggi(perguruanTinggi);

        Common.refreshSaveOrUpdate(session, profesiLulusan);
        return true;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    /** Membangun kriteria pencarian data profesi lulusan, dibatasi fakultas/jurusan sesuai filter aktif dan diurutkan berdasarkan kode+nama; lihat komentar kode untuk alasan kriteria OBE standar dipakai. */
    @Override
    public Criteria initCriteria(boolean order) {
        // Profesi Lulusan wajib terikat ke Prodi pada form. Gunakan kriteria OBE
        // standar supaya pilihan Fakultas/Prodi benar-benar membatasi query,
        // sama seperti Profil Lulusan, Bahan Kajian, dan CPL.
        return buildBaseCriteria(HibernateUtil.currentSession(), ProfesiLulusan.class,
                order, true, "kode", "nama");
    }

    /** Menjalankan pencarian default dan merender hasilnya ke grid lewat {@link ProfesiLulusanRenderer}. */
    @Override
    public void onSearchDefault(Event event) {
        executeSearch(initCriteria(false), initCriteria(true), new ProfesiLulusanRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    /** Perender baris grid pencarian profesi lulusan: menampilkan kode, nama, jurusan, ringkasan keterangan, toggle aktif langsung tersimpan saat diklik, dan tombol ubah/hapus. */
    class ProfesiLulusanRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ProfesiLulusan pl = (ProfesiLulusan) obj;

            new Label(pl.getKode()).setParent(row);
            namaCellRingkas(ProfesiLulusan.class, pl, pl.getNama()).setParent(row);
            new Label(pl.getJurusan() == null ? "" : pl.getJurusan().getNama()).setParent(row);
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

            Common.copyEditDeleteButtons(edit, delete, pl, ProfesiLulusanAction.this).setParent(row);
        }
    }
}
