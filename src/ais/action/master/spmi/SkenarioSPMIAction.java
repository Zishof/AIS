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
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * Action pengelola "Daftar Tilik/Skenario SPMI" ({@link SkenarioSPMI}) — item pertanyaan/bukti
 * audit terkecil pada hierarki Sistem Penjaminan Mutu Internal:
 * {@link JenisSPMI} → {@link StandarSPMI} → {@link ButirMutuSPMI} → {@link IndikatorSPMI} →
 * {@link SkenarioSPMI}. Setiap skenario merupakan satu poin yang diperiksa auditor saat audit mutu
 * internal (AMI), terkait ke satu indikator pada butir mutu tertentu.
 *
 * <p>
 * Filter pencarian ({@link #doAfterCompose(Component)}) berjenjang: memilih Jenis SPMI menyaring
 * ulang pilihan Standar, memilih Standar menyaring ulang pilihan Butir Mutu, dst., masing-masing
 * lewat listener {@code onChange} berantai. Form tambah/ubah ({@link #init(GeneralValueObject)} →
 * {@code buildForm}) memiliki struktur bertingkat serupa: memilih Jenis SPMI (dikunci
 * read-only bila konteks sudah menentukan salah satu tingkat lebih rendah, lihat
 * {@code resolveJenis}) menentukan pilihan Standar yang tersedia, dan seterusnya hingga Indikator.
 * {@link #onSave(Event)} memvalidasi seluruh field wajib (nomor urut, teks skenario, dan keempat
 * tingkat hierarki di atasnya) sebelum menyimpan. {@link #initCriteria(boolean)} membangun kueri
 * pencarian dengan filter pada keempat tingkat hierarki tersebut, digunakan oleh
 * {@link #onSearchDefault(Event)}.
 * </p>
 */
public class SkenarioSPMIAction extends BaseSPMIAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // ---- Search fields ----
    private Combobox searchjenisSPMI;
    private Combobox searchstandarSPMI;
    private Combobox searchbutirMutuSPMI;
    private Combobox searchindikatorSPMI;

    // ---- Form fields ----
    private MyIntbox nomorUrut;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox jenisSPMI;
    private Combobox standarSPMI;
    private Combobox butirMutuSPMI;
    private Combobox indikatorSPMI;

    // ---- Current entity ----
    private SkenarioSPMI skenarioSPMI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    /**
     * Inisialisasi composer setelah komponen ZK ter-wiring: menyiapkan filter pencarian berjenjang
     * (Jenis → Standar → Butir Mutu → Indikator SPMI), setiap perubahan pada satu tingkat
     * menyaring ulang pilihan tingkat di bawahnya dan memicu pencarian ulang.
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
        Common.insertComboDanSemua(searchindikatorSPMI, "nama", "butirMutuSPMI",
                IndikatorSPMI.class, Restrictions.eq("aktif", true));

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

        searchbutirMutuSPMI.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                ButirMutuSPMI sel = selectedValue(searchbutirMutuSPMI);
                Common.insertComboDanSemua(searchindikatorSPMI, "nama", "butirMutuSPMI", IndikatorSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("butirMutuSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                onSearchDefault(null);
            }
        });

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(SkenarioSPMI.class,
                new String[]{"id", "nomorUrut", "indikatorSPMI", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    /**
     * Renderer lokal untuk layar/komponen {@link SkenarioSPMIAction}. Kelas ini menerjemahkan satu item data
     * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link SkenarioSPMIAction} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see SkenarioSPMIAction
     */
    class SkenarioSPMIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final SkenarioSPMI item = (SkenarioSPMI) obj;

            new Label(item.getNomorUrut() + "").setParent(row);
            new Label(item.getIndikatorSPMI().getButirMutuSPMI().getStandarSPMI().getJenisSPMI().getNama()).setParent(row);
            new Label(item.getIndikatorSPMI().getButirMutuSPMI().getStandarSPMI().getNama()).setParent(row);
            new Label(item.getIndikatorSPMI().getButirMutuSPMI().getNama()).setParent(row);
            new Label(item.getIndikatorSPMI().getNama()).setParent(row);
            RevisiHelper.createNewRevisi(SkenarioSPMI.class, item, item.getNama()).setParent(row);
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

            Common.copyEditDeleteButtons(edit, delete, item, SkenarioSPMIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    /** Handler tombol tambah: membuka form dengan entitas {@link SkenarioSPMI} baru (kosong). */
    public void onAdd(Event event) throws Exception {
        init(new SkenarioSPMI());
    }

    /**
     * Membangun dan menampilkan form tambah/ubah skenario SPMI dengan pilihan hierarki
     * Jenis/Standar/Butir Mutu/Indikator berjenjang.
     *
     * @param obj entitas {@link SkenarioSPMI} yang akan diedit, atau baru (id {@code null}) untuk data baru
     */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        skenarioSPMI = (SkenarioSPMI) obj;
        buildForm(skenarioSPMI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final SkenarioSPMI item) throws Exception {
        FormHolder fh = prepareFormWindow("Pendataan Daftar Tilik/Skenario SPMI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "No Urut");
        row.appendChild(nomorUrut = new MyIntbox(item.getNomorUrut()));

        row = addFormRow(rows, "Daftar Tilik/Skenario Pertanyaan/Bukti yang akan diperiksa *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");
        nama.setRows(5);

        row = addFormRow(rows, "Jenis SPMI *");
        row.appendChild(jenisSPMI = new Combobox());
        jenisSPMI.setWidth("90%");
        Common.insertCombo(jenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, jenisSPMI, resolveJenis(item));
        jenisSPMI.setReadonly(true);

        row = addFormRow(rows, "Standar SPMI/Referensi Eksternal *");
        row.appendChild(standarSPMI = new Combobox());
        standarSPMI.setWidth("90%");
        Common.insertCombo(standarSPMI, "nama", "jenisSPMI",
                StandarSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, standarSPMI, resolveStandar(item));
        standarSPMI.setReadonly(true);

        row = addFormRow(rows, "Pernyataan Ayat Standar/Butir Mutu *");
        row.appendChild(butirMutuSPMI = new Combobox());
        butirMutuSPMI.setWidth("90%");
        Common.insertCombo(butirMutuSPMI, "nama", "standarSPMI",
                ButirMutuSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, butirMutuSPMI, resolveButir(item));
        butirMutuSPMI.setReadonly(true);

        row = addFormRow(rows, "Indikator *");
        row.appendChild(indikatorSPMI = new Combobox());
        indikatorSPMI.setWidth("90%");
        Common.insertCombo(indikatorSPMI, "nama", "butirMutuSPMI",
                IndikatorSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, indikatorSPMI,
                item.getIndikatorSPMI() == null
                        ? selectedValue(searchindikatorSPMI)
                        : item.getIndikatorSPMI());
        indikatorSPMI.setReadonly(true);

        // Cascade: Jenis → Standar
        EventListener jenisCascade = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                JenisSPMI sel = selectedValue(jenisSPMI);
                Common.insertCombo(standarSPMI, "nama", "jenisSPMI", StandarSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("jenisSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                Common.selectComboItem(true, standarSPMI, resolveStandar(item));
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
                Common.selectComboItem(true, butirMutuSPMI, resolveButir(item));
            }
        };
        standarCascade.onEvent(null);
        standarSPMI.addEventListener("onChange", standarCascade);

        // Cascade: ButirMutu → Indikator
        EventListener butirCascade = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                ButirMutuSPMI sel = selectedValue(butirMutuSPMI);
                Common.insertCombo(indikatorSPMI, "nama", "butirMutuSPMI", IndikatorSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("butirMutuSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                Common.selectComboItem(true, indikatorSPMI,
                        item.getIndikatorSPMI() == null
                                ? selectedValue(searchindikatorSPMI)
                                : item.getIndikatorSPMI());
            }
        };
        butirCascade.onEvent(null);
        butirMutuSPMI.addEventListener("onChange", butirCascade);

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
     * Memvalidasi dan menyimpan satu skenario SPMI. Field wajib: nomor urut, teks daftar
     * tilik/skenario, dan pilihan Jenis/Standar/Butir Mutu/Indikator SPMI. Setiap pelanggaran
     * validasi menampilkan pesan peringatan dan mengembalikan {@code false} tanpa menyimpan.
     *
     * @param event event ZK asal aksi simpan
     * @return {@code true} bila data berhasil disimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (nomorUrut.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, nomor urut skenario SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Nomor Urut pada form dengan nilai angka; "
                    + "(2) pastikan kolom tidak kosong sebelum menyimpan; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Daftar Tilik/Skenario belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Daftar Tilik/Skenario pada form; "
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
        if (indikatorSPMI.getSelectedItem() == null || indikatorSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Indikator SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Butir Mutu terlebih dahulu agar daftar Indikator termuat; "
                    + "(2) pilih Indikator dari daftar pilihan yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (skenarioSPMI.getId() != null) {
            skenarioSPMI = (SkenarioSPMI) session.load(SkenarioSPMI.class, skenarioSPMI.getId());
        }
        skenarioSPMI.setNomorUrut(nomorUrut.getValue());
        skenarioSPMI.setNama(nama.getValue());
        skenarioSPMI.setIndikatorSPMI((IndikatorSPMI) indikatorSPMI.getSelectedItem().getValue());
        skenarioSPMI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, skenarioSPMI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    /**
     * Membangun kueri pencarian skenario SPMI, difilter status aktif, kata kunci teks, dan
     * hierarki Jenis/Standar/Butir Mutu/Indikator (dijoin lewat alias
     * {@code indikatorSPMI}/{@code butirMutuSPMI} dst.).
     *
     * @param order {@code true} untuk menyertakan pengurutan hasil
     * @return kriteria Hibernate siap dieksekusi/dipaginasi
     */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(SkenarioSPMI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .createAlias("indikatorSPMI", "indikatorSPMI")
                .createAlias("indikatorSPMI.butirMutuSPMI", "butirMutuSPMI");

        IndikatorSPMI indikatorSel = selectedValue(searchindikatorSPMI);
        if (indikatorSel != null) {
            criteria.add(Restrictions.eq("indikatorSPMI", indikatorSel));
        }

        ButirMutuSPMI butirSel = selectedValue(searchbutirMutuSPMI);
        if (butirSel != null) {
            criteria.add(Restrictions.eq("indikatorSPMI.butirMutuSPMI", butirSel));
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

    /** Mengeksekusi ulang pencarian ({@link #initCriteria(boolean)}) untuk halaman aktif dan merender hasilnya ke grid daftar skenario SPMI. */
    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<SkenarioSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new SkenarioSPMIRenderer());
    }

    // =====================================================================
    // Internal helpers: resolve hierarchy values for form pre-population
    // =====================================================================

    private JenisSPMI resolveJenis(SkenarioSPMI item) {
        if (item.getIndikatorSPMI() != null
                && item.getIndikatorSPMI().getButirMutuSPMI() != null
                && item.getIndikatorSPMI().getButirMutuSPMI().getStandarSPMI() != null) {
            return item.getIndikatorSPMI().getButirMutuSPMI().getStandarSPMI().getJenisSPMI();
        }
        return selectedValue(searchjenisSPMI);
    }

    private StandarSPMI resolveStandar(SkenarioSPMI item) {
        if (item.getIndikatorSPMI() != null && item.getIndikatorSPMI().getButirMutuSPMI() != null) {
            return item.getIndikatorSPMI().getButirMutuSPMI().getStandarSPMI();
        }
        return selectedValue(searchstandarSPMI);
    }

    private ButirMutuSPMI resolveButir(SkenarioSPMI item) {
        if (item.getIndikatorSPMI() != null) {
            return item.getIndikatorSPMI().getButirMutuSPMI();
        }
        return selectedValue(searchbutirMutuSPMI);
    }

}
