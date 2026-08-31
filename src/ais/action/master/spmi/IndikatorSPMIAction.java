package ais.action.master.spmi;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * Layar CRUD master data Indikator SPMI (Sistem Penjaminan Mutu Internal) pada modul SPMI,
 * dibangun di atas {@code BaseSPMIAction} (kelas dasar bersama untuk hierarki entitas SPMI).
 * Indikator berada pada level terbawah hierarki Jenis SPMI &gt; Standar SPMI &gt; Butir Mutu SPMI
 * &gt; Indikator SPMI, sehingga baik filter pencarian maupun form tambah/ubah menerapkan pola
 * combobox berjenjang (cascading): memilih Jenis mempersempit pilihan Standar, memilih Standar
 * mempersempit pilihan Butir Mutu.
 *
 * <p>
 * Pencarian mendukung filter status aktif, kecocokan sebagian nama, serta filter berjenjang
 * jenis/standar/butir mutu SPMI ({@link #initCriteria(boolean)}). Form simpan memvalidasi nomor
 * urut, nama, dan seluruh tingkat hierarki (jenis/standar/butir mutu) wajib dipilih sebelum
 * menyimpan; combobox pada form dikunci read-only setelah nilai default/hasil cascade diisi,
 * memaksa pengguna memilih ulang lewat perubahan combobox tingkat atasnya untuk mengganti
 * pilihan.
 * </p>
 */
public class IndikatorSPMIAction extends BaseSPMIAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // ---- Search fields ----
    private Combobox searchjenisSPMI;
    private Combobox searchstandarSPMI;
    private Combobox searchbutirMutuSPMI;

    // ---- Form fields ----
    private MyIntbox nomorUrut;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox jenisSPMI;
    private Combobox standarSPMI;
    private Combobox butirMutuSPMI;

    // ---- Current entity ----
    private IndikatorSPMI indikatorSPMI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    /**
     * Menyiapkan combobox filter berjenjang jenis/standar/butir mutu SPMI (memilih jenis
     * mempersempit pilihan standar, memilih standar mempersempit pilihan butir mutu, keduanya
     * memicu pencarian ulang), memuat data awal, memasang paging, dan menambahkan tombol
     * cetak/unggah ke toolbar.
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        Common.insertComboDanSemua(searchjenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        Common.insertComboDanSemua(searchstandarSPMI, "nama", "jenisSPMI",
                StandarSPMI.class, Restrictions.eq("aktif", true));
        Common.insertComboDanSemua(searchbutirMutuSPMI, "nama", "standarSPMI",
                ButirMutuSPMI.class, Restrictions.eq("aktif", true));

        searchjenisSPMI.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                JenisSPMI sel = selectedValue(searchjenisSPMI);
                Common.insertComboDanSemua(searchstandarSPMI, "nama", "jenisSPMI", StandarSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("jenisSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                onSearchDefault(null);
            }
        });

        searchstandarSPMI.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                StandarSPMI sel = selectedValue(searchstandarSPMI);
                Common.insertComboDanSemua(searchbutirMutuSPMI, "nama", "standarSPMI", ButirMutuSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("standarSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                onSearchDefault(null);
            }
        });

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(IndikatorSPMI.class,
                new String[]{"id", "nomorUrut", "butirMutuSPMI", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    /** Perenderan satu baris tabel indikator SPMI: nomor urut, nama jenis/standar/butir mutu SPMI, nama indikator (dengan tautan riwayat revisi), keterangan, checkbox status aktif, dan tombol edit/hapus. */
    class IndikatorSPMIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final IndikatorSPMI item = (IndikatorSPMI) obj;

            new Label(item.getNomorUrut() + "").setParent(row);
            new Label(item.getButirMutuSPMI().getStandarSPMI().getJenisSPMI().getNama()).setParent(row);
            new Label(item.getButirMutuSPMI().getStandarSPMI().getNama()).setParent(row);
            new Label(item.getButirMutuSPMI().getNama()).setParent(row);
            RevisiHelper.createNewRevisi(IndikatorSPMI.class, item, item.getNama()).setParent(row);
            new Label(item.getKeterangan()).setParent(row);

            final MyCheckboxConfig aktifCb = new MyCheckboxConfig("Aktif");
            aktifCb.setDisabled(!edit);
            aktifCb.setChecked(item.getAktif());
            aktifCb.setParent(row);
            row.setAttribute("checkbox", aktifCb);
            aktifCb.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    item.setAktif(aktifCb.isChecked());
                    Common.refreshSaveOrUpdate(item);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, item, IndikatorSPMIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    /** Membuka dialog tambah dengan entitas {@link IndikatorSPMI} baru (kosong). */
    public void onAdd(Event event) throws Exception {
        init(new IndikatorSPMI());
    }

    /** Membuka dialog ubah untuk entitas {@code obj} yang diberikan (dipanggil dari tombol edit baris tabel). */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        indikatorSPMI = (IndikatorSPMI) obj;
        buildForm(indikatorSPMI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    /**
     * Membangun form tambah/ubah indikator SPMI: nomor urut, nama indikator, dan tiga combobox
     * berjenjang (jenis/standar/butir mutu SPMI) dengan cascade otomatis — memilih jenis
     * memuat ulang pilihan standar, memilih standar memuat ulang pilihan butir mutu, masing-masing
     * combobox dikunci read-only setelah nilai awal/cascade ditentukan.
     */
    private void buildForm(final IndikatorSPMI item) throws Exception {
        FormHolder fh = prepareFormWindow("Pendataan Indikator SPMI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "No Urut");
        row.appendChild(nomorUrut = new MyIntbox(item.getNomorUrut()));

        row = addFormRow(rows, "Indikator *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");
        nama.setRows(5);

        row = addFormRow(rows, "Jenis SPMI *");
        row.appendChild(jenisSPMI = new Combobox());
        jenisSPMI.setWidth("90%");
        Common.insertCombo(jenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, jenisSPMI,
                item.getButirMutuSPMI() == null || item.getButirMutuSPMI().getStandarSPMI() == null
                        ? selectedValue(searchjenisSPMI)
                        : item.getButirMutuSPMI().getStandarSPMI().getJenisSPMI());
        jenisSPMI.setReadonly(true);

        row = addFormRow(rows, "Standar SPMI/Referensi Eksternal *");
        row.appendChild(standarSPMI = new Combobox());
        standarSPMI.setWidth("90%");
        Common.insertCombo(standarSPMI, "nama", "jenisSPMI",
                StandarSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, standarSPMI,
                item.getButirMutuSPMI() == null
                        ? selectedValue(searchstandarSPMI)
                        : item.getButirMutuSPMI().getStandarSPMI());
        standarSPMI.setReadonly(true);

        row = addFormRow(rows, "Pernyataan Ayat Standar/Butir Mutu *");
        row.appendChild(butirMutuSPMI = new Combobox());
        butirMutuSPMI.setWidth("90%");
        Common.insertCombo(butirMutuSPMI, "nama", "standarSPMI",
                ButirMutuSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, butirMutuSPMI,
                item.getButirMutuSPMI() == null
                        ? selectedValue(searchbutirMutuSPMI)
                        : item.getButirMutuSPMI());
        butirMutuSPMI.setReadonly(true);

        // Cascade: Jenis → Standar
        EventListener jenisCascade = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                JenisSPMI sel = selectedValue(jenisSPMI);
                Common.insertCombo(standarSPMI, "nama", "jenisSPMI", StandarSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("jenisSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                Common.selectComboItem(true, standarSPMI,
                        item.getButirMutuSPMI() == null
                                ? selectedValue(searchstandarSPMI)
                                : item.getButirMutuSPMI().getStandarSPMI());
            }
        };
        jenisCascade.onEvent(null);
        jenisSPMI.addEventListener("onChange", jenisCascade);

        // Cascade: Standar → ButirMutu
        EventListener standarCascade = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                StandarSPMI sel = selectedValue(standarSPMI);
                Common.insertCombo(butirMutuSPMI, "nama", "standarSPMI", ButirMutuSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("standarSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                Common.selectComboItem(true, butirMutuSPMI,
                        item.getButirMutuSPMI() == null
                                ? selectedValue(searchbutirMutuSPMI)
                                : item.getButirMutuSPMI());
                butirMutuSPMI.setReadonly(true);
            }
        };
        standarCascade.onEvent(null);
        standarSPMI.addEventListener("onChange", standarCascade);

        row = addFormRow(rows, "Keterangan");
        row.appendChild(keterangan = new Textbox(item.getKeterangan()));
        keterangan.setWidth("90%");
        keterangan.setRows(5);

        finaliseFormWindow(fh, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
    }

    // =====================================================================
    // Save
    // =====================================================================

    /**
     * Memvalidasi (nomor urut, nama, jenis, standar, dan butir mutu SPMI wajib diisi/dipilih)
     * dan menyimpan (create-or-update) entitas indikator SPMI dari isian form.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
     *         peringatan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nomorUrut.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, nomor urut indikator SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Nomor Urut pada form dengan nilai angka; "
                    + "(2) pastikan kolom tidak kosong sebelum menyimpan; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, nama indikator SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Indikator pada form dengan deskripsi yang jelas; "
                    + "(2) pastikan teks tidak kosong atau hanya berisi spasi; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisSPMI.getSelectedItem() == null || jenisSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Jenis SPMI dari daftar pilihan; "
                    + "(2) pastikan daftar sudah memuat data Jenis SPMI yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (standarSPMI.getSelectedItem() == null || standarSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Standar SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Jenis SPMI terlebih dahulu agar daftar Standar termuat; "
                    + "(2) pilih Standar SPMI dari daftar pilihan yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (butirMutuSPMI.getSelectedItem() == null || butirMutuSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Pernyataan Ayat Standar/Butir Mutu belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Standar SPMI terlebih dahulu agar daftar Butir Mutu termuat; "
                    + "(2) pilih Butir Mutu dari daftar pilihan yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (indikatorSPMI.getId() != null) {
            indikatorSPMI = (IndikatorSPMI) session.load(IndikatorSPMI.class, indikatorSPMI.getId());
        }
        indikatorSPMI.setNomorUrut(nomorUrut.getValue());
        indikatorSPMI.setNama(nama.getValue());
        indikatorSPMI.setButirMutuSPMI((ButirMutuSPMI) butirMutuSPMI.getSelectedItem().getValue());
        indikatorSPMI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, indikatorSPMI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    /**
     * Membangun kriteria pencarian daftar indikator SPMI, difilter berdasarkan status aktif,
     * kecocokan sebagian nama, serta filter berjenjang butir mutu/standar/jenis SPMI (dari alias
     * relasi) bila dipilih pada panel pencarian.
     */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(IndikatorSPMI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .createAlias("butirMutuSPMI", "butirMutuSPMI");

        ButirMutuSPMI butirSel = selectedValue(searchbutirMutuSPMI);
        if (butirSel != null) {
            criteria.add(Restrictions.eq("butirMutuSPMI", butirSel));
        }

        StandarSPMI standarSel = selectedValue(searchstandarSPMI);
        if (standarSel != null) {
            criteria.add(Restrictions.eq("butirMutuSPMI.standarSPMI", standarSel));
        }

        JenisSPMI jenisSel = selectedValue(searchjenisSPMI);
        if (jenisSel != null) {
            criteria.createAlias("butirMutuSPMI.standarSPMI", "standarSPMI")
                    .add(Restrictions.eq("standarSPMI.jenisSPMI", jenisSel));
        }

        if (order) criteria.addOrder(Order.asc("nomorUrut"));
        return criteria;
    }

    /** Memuat ulang halaman pertama/berjalan daftar indikator SPMI sesuai kriteria pencarian saat ini, memperbarui paging dan grid. */
    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<IndikatorSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new IndikatorSPMIRenderer());
    }

}
