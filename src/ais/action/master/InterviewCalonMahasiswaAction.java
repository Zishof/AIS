package ais.action.master;

import ais.action.master.pmb.VerifikasiPMBHelper;
import ais.common.CommonSearchFilterHelper;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PMBAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.pmb.InterviewPunyaCalonMahasiswaDetailAction;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.InterviewCalonMahasiswa;
import ais.database.model.InterviewPunyaCalonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * {@code InterviewCalonMahasiswaAction} adalah Action ZKoss yang mengelola seluruh
 * siklus hidup sesi wawancara (interview) bagi Calon Mahasiswa Baru (PMB) dalam sistem
 * AIS (Academic Information System). Kelas ini merupakan Composer berbasis
 * {@link GenericAutowireComposer} yang di-wire secara otomatis ke komponen ZUL
 * pada halaman administrasi PMB.
 *
 * <h2>Fungsi Utama</h2>
 * <p>Kelas ini bertanggung jawab atas dua skenario utama:</p>
 * <ol>
 *   <li><strong>Administrasi Interview (Admin Panel)</strong> — CRUD jadwal interview,
 *       pengaturan gelombang pendaftaran, kapasitas ruangan, media online (Jitsi,
 *       Google Meet, Zoom, BigBlueButton, Skype, WhatsApp, dan lain-lain), serta
 *       perbaikan urutan penempatan calon mahasiswa ke sesi interview yang tersedia.</li>
 *   <li><strong>Portal Calon Mahasiswa</strong> — Popup interaktif yang ditampilkan kepada
 *       calon mahasiswa saat mereka mengakses menu Interview. Popup ini mencakup informasi
 *       sesi interview (nama, jadwal, pewawancara), tombol notifikasi WA ke pewawancara,
 *       tautan konferensi video, serta form pernyataan kesiapan interview.</li>
 * </ol>
 *
 * <h2>Algoritma Penempatan Calon Mahasiswa ke Sesi Interview</h2>
 * <p>Tombol "Perbaiki Urutan Interview" menjalankan algoritma distribusi round-robin:</p>
 * <ol>
 *   <li>Ambil seluruh sesi interview ({@link InterviewCalonMahasiswa}) yang aktif untuk
 *       tahun akademik yang dipilih pada filter pencarian.</li>
 *   <li>Kumpulkan semua ID gelombang pendaftaran dari sesi-sesi tersebut.</li>
 *   <li>Untuk setiap gelombang pendaftaran, ambil daftar calon mahasiswa berurut
 *       berdasarkan nomor registrasi ({@code ORDER BY noRegistrasi ASC}).</li>
 *   <li>Distribusikan calon mahasiswa ke sesi interview menggunakan round-robin modular
 *       ({@code index % banyak}), melompati sesi yang kapasitasnya sudah penuh
 *       atau gelombangnya tidak sesuai.</li>
 *   <li>Jika calon mahasiswa sudah memiliki sesi ({@link InterviewPunyaCalonMahasiswa}),
 *       perbarui sesi-nya; jika belum, buat record baru.</li>
 * </ol>
 * <p>Tombol "Bersihkan Data dan Perbaiki Urutan" terlebih dahulu menghapus semua
 * {@link InterviewPunyaCalonMahasiswa} yang belum berstatus siap ({@code siap = false}
 * atau {@code null}), kemudian menjalankan algoritma distribusi yang sama.</p>
 *
 * <h2>Popup Interview untuk Calon Mahasiswa</h2>
 * <p>Method statis {@link #tampilkanInterview(BiodataCalonMahasiswa)} menangani seluruh
 * alur penampilan popup interview bagi calon mahasiswa, dengan urutan pemeriksaan:</p>
 * <ul>
 *   <li><strong>Gate Pembayaran</strong>: Jika gelombang mewajibkan pembayaran registrasi
 *       sebelum login, periksa apakah calon sudah lunas. Jika belum, tampilkan pesan.</li>
 *   <li><strong>Gate Verifikasi Berkas</strong>: Panggil
 *       {@link VerifikasiPMBHelper#checkVerifikasiSebelumInterview} untuk memastikan
 *       semua berkas wajib telah diupload sebelum proses interview dapat dilanjutkan.</li>
 *   <li><strong>Gate Ujian</strong>: Jika gelombang memiliki ujian online dan sesi
 *       interview mewajibkan ujian ({@code hrsUjian = true}), pastikan calon sudah
 *       mengikuti semua ujian yang dipersyaratkan.</li>
 *   <li><strong>Penentuan Sesi Interview</strong>: Cari sesi yang berlaku hari ini
 *       berdasarkan hierarki: prodi1 &rarr; prodi2-5 &rarr; fakultas utama &rarr;
 *       fakultas prodi2-5 &rarr; sesi tanpa prodi/fakultas (berlaku umum). Jika
 *       belum ada {@link InterviewPunyaCalonMahasiswa}, buat record baru ke sesi
 *       dengan penghuni paling sedikit.</li>
 *   <li><strong>Konferensi Video</strong>: Jika sesi menggunakan media online
 *       ({@code onlineMenggunakan != TIDAK_AKTIF}), tampilkan tombol akses
 *       konferensi video via {@link InterviewCalonMahasiswa#createVideoConrefrence}.</li>
 *   <li><strong>Pernyataan Siap</strong>: Calon mahasiswa dapat mencentang "Siap
 *       Interview" dan mengisi catatan. Saat tombol ditekan, sistem mengirimkan
 *       notifikasi WA ke pewawancara dan email ke semua pihak terkait.</li>
 * </ul>
 *
 * <h2>Pengelolaan Sesi Hibernate</h2>
 * <p>Seluruh akses database menggunakan {@link HibernateUtil#currentSession()} yang
 * dikelola oleh thread ZK. Sesi ini <strong>tidak boleh ditutup secara manual</strong>
 * karena ZK Framework menutupnya otomatis pada akhir siklus request. Penutupan manual
 * akan menyebabkan {@code HibernateException: Session is closed} pada akses berikutnya.</p>
 *
 * <h2>Kompatibilitas Java 1.7</h2>
 * <p>Kelas ini dipertahankan kompatibel dengan Java 1.7. Tidak ada lambda expression,
 * try-with-resources, method reference ({@code ::}), diamond operator, atau fitur
 * Java 8+ lainnya. Semua anonymous inner class listener dideklarasikan secara eksplisit.</p>
 *
 * <h2>Komponen ZUL yang Di-wire</h2>
 * <ul>
 *   <li>{@code add} — Tombol Tambah; tampil jika user memiliki hak {@code CREATE}</li>
 *   <li>{@code paging} — Navigasi halaman grid</li>
 *   <li>{@code grid} — Grid daftar sesi interview</li>
 *   <li>{@code searchnama}, {@code searchpegawai}, {@code searchcalon} — Filter teks</li>
 *   <li>{@code searchaktif} — Filter status aktif</li>
 *   <li>{@code searchTahunAjaran} — Filter tahun akademik</li>
 *   <li>{@code addWindow} — Popup form tambah/ubah sesi interview</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 2.1 — refactored 2026-07-16 (fix prodi-string bug, fix NPE email,
 *          add video-conf button in popup, compact header, helper extraction)
 * @see InterviewPunyaCalonMahasiswaDetailAction
 * @see VerifikasiPMBHelper
 * @see InterviewCalonMahasiswa
 * @see InterviewPunyaCalonMahasiswa
 */
public class InterviewCalonMahasiswaAction extends GenericAutowireComposer
        implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = -5779730267402400328L;

    // -----------------------------------------------------------------------
    // ZUL-wired components
    // -----------------------------------------------------------------------
    private MyWindow addWindow;
    private Paging paging;
    private MyGrid grid;

    private Textbox searchnama;
    private Textbox searchpegawai;
    private Textbox searchcalon;
    private Checkbox searchaktif;
    private Combobox searchTahunAjaran;

    // -----------------------------------------------------------------------
    // Add/Edit form fields
    // -----------------------------------------------------------------------
    private Textbox nama;
    private Textbox keterangan;
    private boolean edit = false;
    private boolean delete = false;
    private InterviewCalonMahasiswa interviewCalonMahasiswa;
    private MyToolbarbuttonConfig add;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private AmbilDataPegawaiBanbox pegawai;
    private Combobox onlineMenggunakan;

    // Rows for media online form sections
    private Row rowMeetKeterangan;
    private Row rowMeet;
    private Row rowLinkZoomKeterangan;
    private Row rowLinkZoomLink;
    private Row rowLinkZoom;
    private Textbox zoomLink;
    private Row rowLinkZoomButton;
    private Row rowLinkBbbKeterangan;
    private Row rowLinkBbbLink;
    private Row rowLinkBbb;
    private Textbox bbbLink;
    private Row rowLinkBbbButton;
    private Row rowLinkSkypeKeterangan;
    private Row rowLinkSkypeLink;
    private Row rowLinkSkype;
    private Textbox skypeLink;
    private Row rowLinkSkypeButton;
    private Row rowLinkWa;
    private Textbox waLink;
    private Row rowLinkWaButton;
    private Row rowLinkWaKeterangan;
    private Row rowLinkLain;
    private Textbox linkLain;
    private Row rowLinkLainKeterangan;

    private String tahunAkademikPenerimaanMahasiswaBaru;
    private Combobox tahunAkademik;
    private MyIntbox kapasitasRuangan;
    private HashSet<GelombangPendaftaran> selectedGelombangPendaftaran;
    private Combobox fakultas;
    private Combobox jurusan;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    /**
     * Inisialisasi komponen setelah ZUL selesai di-compose. Method ini dipanggil otomatis
     * oleh ZK Framework setelah seluruh komponen ZUL ter-wire ke field kelas ini.
     * <p>Tugas yang dilakukan:</p>
     * <ul>
     *   <li>Inisialisasi bahasa antarmuka</li>
     *   <li>Mengatur visibilitas tombol Tambah sesuai hak akses pengguna</li>
     *   <li>Mengisi dropdown tahun akademik dan memilih tahun PMB default</li>
     *   <li>Mendaftarkan listener paging dan memanggil pencarian awal</li>
     *   <li>Menambahkan tombol cetak, upload, Perbaiki Urutan, dan Bersihkan Data ke toolbar</li>
     * </ul>
     *
     * @param comp komponen root ZUL yang di-compose
     * @throws Exception jika terjadi kesalahan inisialisasi
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        if (add != null) {
            add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
            add.setTooltiptext("Tambah");
        }

        Common.generateTahunAjaranDanSemua(searchTahunAjaran);
        tahunAkademikPenerimaanMahasiswaBaru = Common
                .getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();
        Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
        onSearchDefault(null);
        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(null);
            }
        });

        String[] contents = new String[] { "id", "pegawai", "nama", "mulai", "sampai", "kapasitasRuangan",
                "keterangan", "aktif" };
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(InterviewCalonMahasiswa.class, this, contents);
        Common.appendKeToolbar(cetakToolbarbutton, add, comp);

        MyToolbarbuttonConfig upload = Common.uploadData(this, InterviewCalonMahasiswa.class, contents);
        if (upload != null) {
            upload.setVisible((add != null && add.isVisible()) && edit && delete);
        }
        Common.appendKeToolbar(upload, add, comp);

        // Tombol Perbaiki Urutan: mendistribusikan calon mahasiswa ke sesi interview
        // yang ada menggunakan round-robin berdasarkan nomor registrasi, tanpa
        // menghapus data yang sudah ada.
        MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Perbaiki Urutan Interview",
                "/img/svg/check2-circle.svg");
        if (button != null) {
            button.setParent(add.getParent());
        }
        button.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Common.createDefaultTimer(new EventListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        Session session = HibernateUtil.currentSession();
                        List<InterviewCalonMahasiswa> interviewCalonMahasiswas = ConstantValues.simpleList(
                                session.createCriteria(InterviewCalonMahasiswa.class)
                                        .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                Restrictions.eq("aktif", true)))
                                        .add(searchTahunAjaran.getSelectedItem() == null
                                                || searchTahunAjaran.getSelectedItem().getValue() == null
                                                        ? Restrictions.sqlRestriction("true")
                                                        : Restrictions.eq("tahunAkademik",
                                                                searchTahunAjaran.getSelectedItem().getValue())),
                                InterviewCalonMahasiswa.class);

                        TreeSet<Long> gelombangIds = new TreeSet<Long>();
                        for (InterviewCalonMahasiswa icm : interviewCalonMahasiswas) {
                            gelombangIds.addAll(icm.ambilGelombangPendaftaranId());
                        }
                        if (gelombangIds.isEmpty()) {
                            return;
                        }

                        for (Long gelombangPendaftaranId : gelombangIds) {
                            List<BiodataCalonMahasiswa> biodataList = ConstantValues.simpleList(
                                    session.createCriteria(BiodataCalonMahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                    Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("gelombangPendaftaran.id", gelombangPendaftaranId))
                                            .add(searchTahunAjaran.getSelectedItem() == null
                                                    || searchTahunAjaran.getSelectedItem().getValue() == null
                                                            ? Restrictions.sqlRestriction("true")
                                                            : Restrictions.eq("tahunAkademik",
                                                                    searchTahunAjaran.getSelectedItem().getValue()))
                                            .add(Restrictions.ne("noRegistrasi", ""))
                                            .add(Restrictions.isNotNull("noRegistrasi"))
                                            .addOrder(Order.asc("noRegistrasi")),
                                    BiodataCalonMahasiswa.class);

                            int index = 0;
                            int banyak = interviewCalonMahasiswas.size();
                            for (BiodataCalonMahasiswa cama : biodataList) {
                                try {
                                    InterviewCalonMahasiswa icm = interviewCalonMahasiswas.get(index % banyak);
                                    if (!icm.ambilGelombangPendaftaranId().contains(gelombangPendaftaranId)) {
                                        index++;
                                        continue;
                                    }
                                    int masuk = ((Number) session
                                            .createCriteria(InterviewPunyaCalonMahasiswa.class)
                                            .add(Restrictions.eq("interviewCalonMahasiswa", icm))
                                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                                    if (masuk >= icm.getKapasitasRuangan()) {
                                        index++;
                                        continue;
                                    }
                                    InterviewPunyaCalonMahasiswa ipmc =
                                            (InterviewPunyaCalonMahasiswa) session
                                                    .createCriteria(InterviewPunyaCalonMahasiswa.class)
                                                    .add(Restrictions.eq("biodataCalonMahasiswa", cama))
                                                    .setMaxResults(1).uniqueResult();
                                    if (ipmc != null) {
                                        ipmc.setInterviewCalonMahasiswa(icm);
                                        Common.refreshSaveOrUpdate(session, ipmc);
                                    } else {
                                        ipmc = new InterviewPunyaCalonMahasiswa();
                                        ipmc.setBiodataCalonMahasiswa(cama);
                                        ipmc.setInterviewCalonMahasiswa(icm);
                                        session.save(ipmc);
                                    }
                                } catch (Exception e) {
                                    ais.common.Common.tampilErrorJikaAdmin(e);
                                }
                                index++;
                            }
                        }
                        onSearchDefault(arg0);
                    }
                });
            }
        });

        // Tombol Bersihkan Data: hapus penempatan yang belum siap, lalu distribusi ulang.
        button = new MyToolbarbuttonConfig("Bersihkan data dan Perbaiki Urutan Interview",
                "/img/svg/check2-circle.svg");
        if (button != null) {
            button.setParent(add.getParent());
        }
        button.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                MyMessageboxConfig.show(
                        "Apakah Bapak/Ibu yakin ingin me-reset seluruh data interview ini? Mohon diperhatikan, seluruh penempatan calon mahasiswa pada sesi interview yang belum berstatus siap akan dihapus dan disusun ulang. Silakan tekan OK untuk melanjutkan atau Batal untuk membatalkan.",
                        "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                        MyMessageboxConfig.QUESTION, new EventListener() {
                            @Override
                            public void onEvent(Event event) throws Exception {
                                int i = Integer.parseInt(event.getData().toString());
                                if (i == MyMessageboxConfig.OK) {
                                    Common.createDefaultTimer(new EventListener() {
                                        @SuppressWarnings("unchecked")
                                        @Override
                                        public void onEvent(Event arg0) throws Exception {
                                            Session session = HibernateUtil.currentSession();
                                            List<InterviewCalonMahasiswa> icmList = ConstantValues.simpleList(
                                                    session.createCriteria(InterviewCalonMahasiswa.class)
                                                            .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                                    Restrictions.eq("aktif", true)))
                                                            .add(searchTahunAjaran.getSelectedItem() == null
                                                                    || searchTahunAjaran.getSelectedItem()
                                                                            .getValue() == null
                                                                                    ? Restrictions.sqlRestriction("true")
                                                                                    : Restrictions.eq("tahunAkademik",
                                                                                            searchTahunAjaran
                                                                                                    .getSelectedItem()
                                                                                                    .getValue())),
                                                    InterviewCalonMahasiswa.class);

                                            TreeSet<Long> gelombangIds = new TreeSet<Long>();
                                            String inSql = "";
                                            for (InterviewCalonMahasiswa icm : icmList) {
                                                inSql += inSql.isEmpty() ? icm.getId().toString()
                                                        : "," + icm.getId();
                                                gelombangIds.addAll(icm.ambilGelombangPendaftaranId());
                                            }
                                            if (gelombangIds.isEmpty() || inSql.isEmpty()) {
                                                return;
                                            }

                                            session.createSQLQuery(
                                                    "delete from interview_punya_calon_mahasiswa"
                                                            + " where interview_calon_mahasiswa in ("
                                                            + inSql + ") and (siap=false or siap is null)")
                                                    .executeUpdate();

                                            for (Long gelombangPendaftaranId : gelombangIds) {
                                                List<BiodataCalonMahasiswa> biodataList = ConstantValues.simpleList(
                                                        session.createCriteria(BiodataCalonMahasiswa.class)
                                                                .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                                        Restrictions.eq("aktif", true)))
                                                                .add(Restrictions.eq("gelombangPendaftaran.id",
                                                                        gelombangPendaftaranId))
                                                                .add(searchTahunAjaran.getSelectedItem() == null
                                                                        || searchTahunAjaran.getSelectedItem()
                                                                                .getValue() == null
                                                                                        ? Restrictions.sqlRestriction("true")
                                                                                        : Restrictions.eq("tahunAkademik",
                                                                                                searchTahunAjaran
                                                                                                        .getSelectedItem()
                                                                                                        .getValue()))
                                                                .add(Restrictions.ne("noRegistrasi", ""))
                                                                .add(Restrictions.isNotNull("noRegistrasi"))
                                                                .addOrder(Order.asc("noRegistrasi")),
                                                        BiodataCalonMahasiswa.class);

                                                int index = 0;
                                                int banyak = icmList.size();
                                                for (BiodataCalonMahasiswa cama : biodataList) {
                                                    try {
                                                        InterviewCalonMahasiswa icm = icmList.get(index % banyak);
                                                        if (!icm.ambilGelombangPendaftaranId()
                                                                .contains(gelombangPendaftaranId)) {
                                                            index++;
                                                            continue;
                                                        }
                                                        int masuk = ((Number) session
                                                                .createCriteria(InterviewPunyaCalonMahasiswa.class)
                                                                .add(Restrictions.eq("interviewCalonMahasiswa", icm))
                                                                .setProjection(Projections.rowCount())
                                                                .uniqueResult()).intValue();
                                                        if (masuk >= icm.getKapasitasRuangan()) {
                                                            index++;
                                                            continue;
                                                        }
                                                        InterviewPunyaCalonMahasiswa ipmc =
                                                                (InterviewPunyaCalonMahasiswa) session
                                                                        .createCriteria(
                                                                                InterviewPunyaCalonMahasiswa.class)
                                                                        .add(Restrictions.eq(
                                                                                "biodataCalonMahasiswa", cama))
                                                                        .setMaxResults(1).uniqueResult();
                                                        if (ipmc != null) {
                                                            ipmc.setInterviewCalonMahasiswa(icm);
                                                            Common.refreshSaveOrUpdate(session, ipmc);
                                                        } else {
                                                            ipmc = new InterviewPunyaCalonMahasiswa();
                                                            ipmc.setBiodataCalonMahasiswa(cama);
                                                            ipmc.setInterviewCalonMahasiswa(icm);
                                                            session.save(ipmc);
                                                        }
                                                    } catch (Exception e) {
                                                        ais.common.Common.tampilErrorJikaAdmin(e);
                                                    }
                                                    index++;
                                                }
                                            }
                                            onSearchDefault(arg0);
                                        }
                                    });
                                }
                            }
                        });
            }
        });
    }

    // -----------------------------------------------------------------------
    // Static helper methods
    // -----------------------------------------------------------------------

    /**
     * Membangun teks pilihan program studi calon mahasiswa dari prodi1 hingga prodi5.
     * Setiap prodi yang tidak null akan digabungkan menggunakan separator " dan ".
     * <p>Ini merupakan helper yang digunakan bersama oleh blok tampil popup dan blok
     * kirim notifikasi di dalam {@link #tampilkanInterview}, sehingga tidak ada duplikasi
     * logika pembangunan string prodi.</p>
     *
     * @param cama data calon mahasiswa; tidak boleh null
     * @return string gabungan nama prodi, misalnya {@code "Teknik Informatika dan Sistem Informasi"}
     */
    private static String buatPilihanProdiText(BiodataCalonMahasiswa cama) {
        String p = "";
        if (cama.getProdi1() != null) {
            p = cama.getProdi1().getNama();
        }
        if (cama.getProdi2() != null) {
            p += (p.isEmpty() ? "" : " dan ") + cama.getProdi2().getNama();
        }
        if (cama.getProdi3() != null) {
            p += (p.isEmpty() ? "" : " dan ") + cama.getProdi3().getNama();
        }
        if (cama.getProdi4() != null) {
            p += (p.isEmpty() ? "" : " dan ") + cama.getProdi4().getNama();
        }
        if (cama.getProdi5() != null) {
            p += (p.isEmpty() ? "" : " dan ") + cama.getProdi5().getNama();
        }
        return p;
    }

    /**
     * Membuat isi pesan notifikasi dalam format HTML yang dikirim melalui email
     * kepada pewawancara dan calon mahasiswa. Berisi identitas lengkap calon mahasiswa
     * beserta tautan unduh biodata PDF.
     *
     * @param cama    data calon mahasiswa
     * @param ipmc    record kesiapan interview milik calon mahasiswa
     * @param linkB   tautan biodata PDF yang telah di-generate sebelumnya
     * @return string pesan dalam format HTML menggunakan tag {@code <br>}
     */
    private static String buatBodyNotifikasiHtml(BiodataCalonMahasiswa cama,
            InterviewPunyaCalonMahasiswa ipmc, String linkB) {
        String p = buatPilihanProdiText(cama);
        String body = "Siap Interview:<br>";
        body += "<br>Nama : " + (cama.getNama() != null ? cama.getNama() : "");
        body += "<br>No.Reg : " + (cama.getNoRegistrasi() != null ? cama.getNoRegistrasi() : "");
        body += "<br>Gelombang : " + (cama.getGelombangPendaftaran() != null
                ? cama.getGelombangPendaftaran().getNama() : "-");
        body += "<br>Seleksi : " + (cama.getJenisSeleksi() != null ? cama.getJenisSeleksi().getNama() : "-");
        body += "<br>Pilihan : " + p;
        body += "<br>Program : " + (cama.getProgram() != null ? cama.getProgram() : "");
        body += "<br>Tahun : " + (cama.getTahunAkademik() != null ? cama.getTahunAkademik() : "");
        body += "<br>Semester : " + (cama.getSemesterMulai() != null ? cama.getSemesterMulai() : "");
        body += "<br>HP/Telp : " + (cama.getHp() != null ? cama.getHp() : "");
        body += "<br>Email : " + (cama.getEmail() != null ? cama.getEmail() : "");
        body += "<br>Link Biodata : " + linkB;
        body += "<br>Catatan : " + (ipmc.getKeterangan() != null ? ipmc.getKeterangan() : "");
        return body;
    }

    /**
     * Menghasilkan versi WA (*bold* markdown) dari body notifikasi yang sama.
     * Digunakan untuk dikirim via API Ultramsg WA kepada pewawancara dan calon mahasiswa.
     *
     * @param cama    data calon mahasiswa
     * @param ipmc    record kesiapan interview
     * @param linkB   tautan biodata PDF
     * @return string pesan dalam format teks WA dengan bold {@code *...*}
     */
    private static String buatBodyNotifikasiWa(BiodataCalonMahasiswa cama,
            InterviewPunyaCalonMahasiswa ipmc, String linkB) {
        String p = buatPilihanProdiText(cama);
        String body = "";
        body += "\n*Nama* : " + (cama.getNama() != null ? cama.getNama() : "");
        body += "\n*No.Reg* : " + (cama.getNoRegistrasi() != null ? cama.getNoRegistrasi() : "");
        body += "\n*Gelombang* : " + (cama.getGelombangPendaftaran() != null
                ? cama.getGelombangPendaftaran().getNama() : "-");
        body += "\n*Seleksi* : " + (cama.getJenisSeleksi() != null ? cama.getJenisSeleksi().getNama() : "-");
        body += "\n*Pilihan* : " + p;
        body += "\n*Program* : " + (cama.getProgram() != null ? cama.getProgram() : "");
        body += "\n*Tahun* : " + (cama.getTahunAkademik() != null ? cama.getTahunAkademik() : "");
        body += "\n*Semester* : " + (cama.getSemesterMulai() != null ? cama.getSemesterMulai() : "");
        body += "\n*HP/Telp* : " + (cama.getHp() != null ? cama.getHp() : "");
        body += "\n*Email* : " + (cama.getEmail() != null ? cama.getEmail() : "");
        body += "\n*Catatan* : " + (ipmc.getKeterangan() != null ? ipmc.getKeterangan() : "");
        return body;
    }

    /**
     * Menghasilkan teks yang aman untuk disematkan ke dalam konten HTML.
     * Melakukan escaping pada karakter {@code &}, {@code <}, dan {@code >}.
     *
     * @param s string input yang mungkin mengandung karakter HTML khusus
     * @return string yang sudah di-escape, atau string kosong jika input null
     */
    private static String safeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -----------------------------------------------------------------------
    // Interview popup for candidates
    // -----------------------------------------------------------------------

    /**
     * Menampilkan popup wawancara (interview) kepada calon mahasiswa yang telah login.
     * <p>Method ini bersifat statis karena dipanggil dari berbagai titik di aplikasi
     * (portal PMB, dashboard) tanpa memerlukan instance Action.</p>
     * <p>Alur lengkap:</p>
     * <ol>
     *   <li>Refresh data calon mahasiswa dari database untuk memastikan data terkini.</li>
     *   <li>Periksa gate pembayaran — jika gelombang mewajibkan bayar sebelum login dan
     *       calon belum lunas, hentikan proses dengan pesan informatif.</li>
     *   <li>Periksa gate berkas via {@link VerifikasiPMBHelper#checkVerifikasiSebelumInterview}.</li>
     *   <li>Cari atau buat {@link InterviewPunyaCalonMahasiswa} untuk calon ini.</li>
     *   <li>Periksa gate ujian jika wajib ujian sebelum interview.</li>
     *   <li>Bangun popup ZK ({@link MyWindow}) berisi header kompak, info sesi, foto
     *       pewawancara, tombol video conference, form siap, dan toolbar aksi.</li>
     * </ol>
     *
     * @param biodataCalonMahasiswa data calon mahasiswa yang akan melakukan interview;
     *                              tidak boleh null dan harus sudah ter-load di sesi Hibernate
     * @throws Exception jika terjadi kesalahan akses database atau komponen ZK
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    public static void tampilkanInterview(final BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
        Session session = HibernateUtil.currentSession();
        session.refresh(biodataCalonMahasiswa);
        boolean harusBayarSebelumLogin = biodataCalonMahasiswa.getGelombangPendaftaran()
                .getHarusBayarSebelumBisaLogin();
        GelombangPendaftaran myGelombangPendaftaran = biodataCalonMahasiswa.getGelombangPendaftaran();

        // Gate 1: Pembayaran registrasi
        if (harusBayarSebelumLogin) {
            if (biodataCalonMahasiswa.getPembayaranRegistrasi() == null
                    || biodataCalonMahasiswa.getPembayaranRegistrasi().getPersentaseLunas() < 0.01) {
                MyMessageboxConfig.show(
                        "Mohon maaf, calon mahasiswa terlebih dahulu wajib menyelesaikan pembayaran registrasi sebelum dapat mengikuti proses interview. Langkah yang dapat dilakukan: (1) selesaikan pembayaran biaya registrasi; (2) pastikan status pembayaran telah terkonfirmasi lunas; (3) ulangi kembali proses interview.",
                        "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                return;
            }
        }

        // Gate 2: Verifikasi kelengkapan berkas
        if (!VerifikasiPMBHelper.checkVerifikasiSebelumInterview(biodataCalonMahasiswa)) {
            return;
        }

        // Cari sesi interview aktif hari ini untuk calon mahasiswa ini
        InterviewPunyaCalonMahasiswa temp = (InterviewPunyaCalonMahasiswa) session
                .createCriteria(InterviewPunyaCalonMahasiswa.class)
                .add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
                .add(Restrictions.sqlRestriction("date('"
                        + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                        + "') between date(mulai) and date(sampai)"))
                .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

        // Jika belum ada, cari sesi yang cocok dan buat record baru
        if (temp == null) {
            Jurusan jurusan = biodataCalonMahasiswa.getProdi1();
            if (biodataCalonMahasiswa.getProdiLulus() != null) {
                jurusan = biodataCalonMahasiswa.getProdiLulus();
            }

            // Cari berdasarkan jurusan utama (prodi1)
            List<InterviewCalonMahasiswa> candidates = ConstantValues.simpleList(
                    session.createCriteria(InterviewCalonMahasiswa.class)
                            .add(Restrictions.eq("jurusan", jurusan))
                            .add(Restrictions.sqlRestriction("date('"
                                    + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                                    + "') between date(mulai) and date(sampai)"))
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("tahunAkademik", biodataCalonMahasiswa.getTahunAkademik())),
                    InterviewCalonMahasiswa.class);

            // Fallback: prodi2–5
            if (candidates.isEmpty()) {
                for (Jurusan j : new Jurusan[] {
                        biodataCalonMahasiswa.getProdi2(), biodataCalonMahasiswa.getProdi3(),
                        biodataCalonMahasiswa.getProdi4(), biodataCalonMahasiswa.getProdi5() }) {
                    if (j != null) {
                        candidates = ConstantValues.simpleList(
                                session.createCriteria(InterviewCalonMahasiswa.class)
                                        .add(Restrictions.eq("jurusan", j))
                                        .add(Restrictions.sqlRestriction("date('"
                                                + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                                                + "') between date(mulai) and date(sampai)"))
                                        .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                Restrictions.eq("aktif", true)))
                                        .add(Restrictions.eq("tahunAkademik",
                                                biodataCalonMahasiswa.getTahunAkademik())),
                                InterviewCalonMahasiswa.class);
                        if (!candidates.isEmpty()) {
                            break;
                        }
                    }
                }
            }

            // Fallback: fakultas utama
            if (candidates.isEmpty()) {
                Fakultas fakultas = biodataCalonMahasiswa.getProdi1() == null ? null
                        : biodataCalonMahasiswa.getProdi1().getFakultas();
                if (biodataCalonMahasiswa.getProdiLulus() != null) {
                    fakultas = biodataCalonMahasiswa.getProdiLulus().getFakultas();
                }
                candidates = ConstantValues.simpleList(
                        session.createCriteria(InterviewCalonMahasiswa.class)
                                .add(Restrictions.eq("fakultas", fakultas))
                                .add(Restrictions.sqlRestriction("date('"
                                        + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                                        + "') between date(mulai) and date(sampai)"))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("tahunAkademik", biodataCalonMahasiswa.getTahunAkademik())),
                        InterviewCalonMahasiswa.class);
            }

            // Fallback: fakultas prodi2–5
            if (candidates.isEmpty()) {
                for (Fakultas f : new Fakultas[] {
                        biodataCalonMahasiswa.getProdi2() == null ? null
                                : biodataCalonMahasiswa.getProdi2().getFakultas(),
                        biodataCalonMahasiswa.getProdi3() == null ? null
                                : biodataCalonMahasiswa.getProdi3().getFakultas(),
                        biodataCalonMahasiswa.getProdi4() == null ? null
                                : biodataCalonMahasiswa.getProdi4().getFakultas(),
                        biodataCalonMahasiswa.getProdi5() == null ? null
                                : biodataCalonMahasiswa.getProdi5().getFakultas() }) {
                    if (f != null) {
                        candidates = ConstantValues.simpleList(
                                session.createCriteria(InterviewCalonMahasiswa.class)
                                        .add(Restrictions.eq("fakultas", f))
                                        .add(Restrictions.sqlRestriction("date('"
                                                + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                                                + "') between date(mulai) and date(sampai)"))
                                        .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                Restrictions.eq("aktif", true)))
                                        .add(Restrictions.eq("tahunAkademik",
                                                biodataCalonMahasiswa.getTahunAkademik())),
                                InterviewCalonMahasiswa.class);
                        if (!candidates.isEmpty()) {
                            break;
                        }
                    }
                }
            }

            // Fallback terakhir: sesi tanpa prodi/fakultas (berlaku semua)
            if (candidates.isEmpty()) {
                candidates = ConstantValues.simpleList(
                        session.createCriteria(InterviewCalonMahasiswa.class)
                                .add(Restrictions.isNull("jurusan"))
                                .add(Restrictions.isNull("fakultas"))
                                .add(Restrictions.sqlRestriction("date('"
                                        + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                                        + "') between date(mulai) and date(sampai)"))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("tahunAkademik", biodataCalonMahasiswa.getTahunAkademik())),
                        InterviewCalonMahasiswa.class);
            }

            // Pilih sesi dengan penghuni paling sedikit (round-robin least-loaded)
            Long gelombangPendaftaranId = biodataCalonMahasiswa.getGelombangPendaftaran().getId();
            InterviewCalonMahasiswa pilih = null;
            int jml = 10000000;
            for (InterviewCalonMahasiswa icm : candidates) {
                if (!icm.ambilGelombangPendaftaranId().contains(gelombangPendaftaranId)) {
                    continue;
                }
                int masuk = ((Number) session.createCriteria(InterviewPunyaCalonMahasiswa.class)
                        .add(Restrictions.eq("interviewCalonMahasiswa", icm))
                        .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                if (jml > masuk) {
                    jml = masuk;
                    pilih = icm;
                }
            }

            if (pilih != null) {
                temp = new InterviewPunyaCalonMahasiswa();
                temp.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
                temp.setInterviewCalonMahasiswa(pilih);
                session.save(temp);
                session.flush();
            }
        }

        final InterviewPunyaCalonMahasiswa interviewPunyaCalonMahasiswa = temp;
        if (interviewPunyaCalonMahasiswa == null
                || interviewPunyaCalonMahasiswa.getInterviewCalonMahasiswa() == null) {
            MyMessageboxConfig.show(
                    "Mohon maaf, jadwal interview untuk Anda belum tersedia saat ini. Langkah yang dapat dilakukan: (1) pastikan Anda telah menyelesaikan proses pendaftaran dan verifikasi; (2) tunggu hingga panitia menetapkan jadwal interview; (3) coba kembali beberapa saat lagi atau hubungi panitia Penerimaan Mahasiswa Baru.",
                    "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        // Alias untuk kenyamanan pembacaan kode
        final InterviewCalonMahasiswa icm = interviewPunyaCalonMahasiswa.getInterviewCalonMahasiswa();

        // Gate 3: Wajib ujian terlebih dahulu
        if (myGelombangPendaftaran.getTerdapatUjianOnline()) {
            if (icm.getHrsUjian()) {
                try {
                    List<PertemuanPunyaUjian> pertemuanPunyaUjians = session
                            .createCriteria(PertemuanPunyaUjian.class)
                            .addOrder(Order.asc("nama"))
                            .createAlias("pertemuan", "pertemuan")
                            .createAlias("pertemuan.jadwalUjianPMB", "jadwalUjianPMB")
                            .add(Restrictions.or(Restrictions.isNull("jadwalUjianPMB.paket"),
                                    Restrictions.eq("jadwalUjianPMB.paket", biodataCalonMahasiswa.getPaket())))
                            .createAlias("jadwalUjianPMB.ujianPMB", "ujianPMB")
                            .add(Restrictions.eq("ujianPMB.gelombangPendaftaran",
                                    biodataCalonMahasiswa.getGelombangPendaftaran()))
                            .list();

                    System.out.println("pertemuanPunyaUjians -> " + pertemuanPunyaUjians.size());
                    for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
                        try {
                            int hasilCount = ((Number) session.createCriteria(HasilUjianMahasiswa.class)
                                    .add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
                                    .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
                                    .add(Restrictions.isNotNull("mulaiPada"))
                                    .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                            if (hasilCount == 0) {
                                MyMessageboxConfig.showFormat(
                                        "Mohon maaf, Anda belum mengikuti ujian \"{V1}\". Langkah yang dapat dilakukan: (1) buka menu Ikut Ujian terlebih dahulu; (2) pilih dan kerjakan ujian \"{V2}\" hingga selesai; (3) setelah ujian selesai, lanjutkan kembali proses interview.",
                                        "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
                                        ppu.getNama(), ppu.getNama());
                                return;
                            }
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/InterviewCalonMahasiswaAction.java:867");
                            // lewati jika terjadi error pada satu ujian
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/InterviewCalonMahasiswaAction.java:871");
                    // lewati gate ujian jika terjadi error tak terduga
                }
            }
        }

        // ---------------------------------------------------------------
        // Bangun popup interview
        // ---------------------------------------------------------------
        final MyWindow addWindow = new MyWindow("Proses Interview", "none", true);
        addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

        Borderlayout borderlayout = new Borderlayout();
        addWindow.appendChild(borderlayout);

        // Header kompak bergradient — menampilkan nama sesi, tanggal, dan pewawancara
        // tanpa membuang ruang layar dengan kop surat penuh (380px).
        North north = new North();
        north.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(north, false);
        north.setAutoscroll(true);
        north.setSclass("headerHbox");

        Div headerDiv = new Div();
        headerDiv.setStyle(
                "background:linear-gradient(135deg,#1e3a5f 0%,#2d6a9f 100%);"
                + "color:#fff;padding:14px 20px;min-height:64px;");
        north.appendChild(headerDiv);

        String namaIcm = icm.getNama() != null ? icm.getNama() : "Interview";
        String tglMulai = icm.getMulai() != null ? Common.dateFormat.get().format(icm.getMulai()) : "-";
        String tglSampai = icm.getSampai() != null ? Common.dateFormat.get().format(icm.getSampai()) : "-";
        String namaPewawancara = (icm.getPegawai() != null && icm.getPegawai().getNama() != null)
                ? icm.getPegawai().getNama() : "";

        String headerHtmlStr = "<div style='font-size:17px;font-weight:700;letter-spacing:0.2px;'>"
                + safeHtml(namaIcm) + "</div>"
                + "<div style='font-size:12px;margin-top:5px;opacity:0.88;'>"
                + "Jadwal: " + safeHtml(tglMulai) + " s/d " + safeHtml(tglSampai)
                + (namaPewawancara.isEmpty() ? ""
                        : " &nbsp;&bull;&nbsp; Pewawancara: " + safeHtml(namaPewawancara))
                + "</div>";
        headerDiv.appendChild(new Html(headerHtmlStr));

        Center center = new Center();
        center.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(center, true);

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setParent(center);
        grid.setHeight("100%");

        Columns columns = new Columns();
        columns.setParent(grid);

        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        column.setWidth("40%");
        column = new MyColumnConfig();
        column.setParent(columns);

        Rows rows = new Rows();
        rows.setParent(grid);

        // Info dari gelombang (jika ada)
        if (biodataCalonMahasiswa.getGelombangPendaftaran() != null
                && !biodataCalonMahasiswa.getGelombangPendaftaran().getInfoSaatInterview().isEmpty()) {
            MyFormRow row = new MyFormRow();
            row.setValign("top");
            rows.appendChild(row);
            ais.ui.util.ZkCompat.setSpans(row, "2");
            Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
            vbox.appendChild(new Caption("Informasi Interview"));
            vbox.setWidth("95%");
            vbox.setParent(row);
            vbox.appendChild(new Html(biodataCalonMahasiswa.getGelombangPendaftaran().getInfoSaatInterview()));
        }

        // Nama sesi interview
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Interview"));
        row.appendChild(new MyLabelBold(icm.getNama()));

        // Detail pewawancara (foto, nama, WA, email)
        if (icm.getPegawai() != null) {

            row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new MyLabelConfig(""));
            CommonMedia.tampilkanGambarKecil(icm.getPegawai()).setParent(row);

            row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new MyLabelConfig("Oleh"));
            row.appendChild(new MyLabelBold(
                    icm.getPegawai().getNama() != null ? icm.getPegawai().getNama() : ""));

            // Tombol video conference (jika sesi menggunakan media online)
            if (!icm.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.TIDAK_AKTIF)) {
                row = new MyFormRow();
                row.setParent(rows);
                row.appendChild(new MyLabelConfig("Konferensi Video"));
                Hbox videoBox = new Hbox();
                row.appendChild(videoBox);
                InterviewCalonMahasiswa.createVideoConrefrence(icm, videoBox, true, false, new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        // tidak ada aksi tambahan setelah buka video conference
                    }
                });
            }

            // Tombol WA ke pewawancara (buka WA dengan body notifikasi awal)
            final String linkBiodata = Common.getRequestHostWithProtocol() + "/Pdf?calmhs="
                    + Common.desEncrypter.get().encrypt(biodataCalonMahasiswa.getId().toString());

            row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new MyLabelConfig("WA/Telp"));
            String bodyWaAwal = buatBodyNotifikasiHtml(biodataCalonMahasiswa,
                    interviewPunyaCalonMahasiswa, linkBiodata);
            icm.getPegawai().tampilkanHp(row, bodyWaAwal);

            row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new MyLabelConfig("Email"));
            icm.getPegawai().tampilkanEmail(row);
        }

        // Form siap interview
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Siap Interview?"));
        final MyCheckboxConfig siap = new MyCheckboxConfig("Klik di sini untuk menyatakan siap interview sekarang.");
        siap.setChecked(interviewPunyaCalonMahasiswa.getSiap());
        row.appendChild(siap);

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Catatan"));
        final MyTextbox catatan;
        row.appendChild(catatan = new MyTextbox(interviewPunyaCalonMahasiswa.getKeterangan()));
        catatan.setWidth("95%");
        catatan.setRows(5);
        catatan.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                interviewPunyaCalonMahasiswa.setKeterangan(catatan.getValue());
                Common.refreshUpdate(interviewPunyaCalonMahasiswa);
            }
        });

        addWindow.setHeight("95%");
        addWindow.setWidth("600px");

        // Toolbar bawah
        South south = new South();
        ais.ui.util.ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);

        final MyToolbarbuttonConfig siapInterview = new MyToolbarbuttonConfig("Siap Interview Sekarang",
                "/img/svg/arrow-right-circle.svg");
        siapInterview.setTooltiptext("Siap Interview Sekarang");
        siapInterview.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.detach();

                if (siap.isChecked()) {
                    // Bangun tautan biodata PDF untuk disertakan di notifikasi
                    String linkB = Common.getRequestHostWithProtocol() + "/Pdf?calmhs="
                            + Common.desEncrypter.get().encrypt(biodataCalonMahasiswa.getId().toString());

                    // Reuse helper untuk body HTML (hapus duplikasi kode)
                    String body = buatBodyNotifikasiHtml(biodataCalonMahasiswa,
                            interviewPunyaCalonMahasiswa, linkB);

                    // Kirim email ke calon mahasiswa dan pewawancara
                    // Null-safe: email calon mahasiswa mungkin null atau kosong
                    String emailCama = (biodataCalonMahasiswa.getEmail() != null
                            && !biodataCalonMahasiswa.getEmail().isEmpty())
                                    ? biodataCalonMahasiswa.getEmail().split(",")[0] : "";
                    String emailUser = emailCama;
                    Pegawai pewawancara = icm.getPegawai();
                    if (!emailUser.isEmpty() && pewawancara != null
                            && pewawancara.getEmail() != null && !pewawancara.getEmail().isEmpty()) {
                        emailUser += "," + pewawancara.getEmail();
                    }

                    JSONArray userIds = new JSONArray();
                    List<String> usernames = HibernateUtil.currentSession()
                            .createCriteria(Tbmuser.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("pegawai", pewawancara))
                            .setProjection(Projections.groupProperty("userId")).list();
                    for (String u : usernames) {
                        userIds.put(u);
                    }

                    String subject = "Siap Interview " + biodataCalonMahasiswa.getNama() + " "
                            + biodataCalonMahasiswa.getNoRegistrasi();
                    String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
                    try {
                        MailSender.sendMail(userIds, subject, body, sender, emailUser,
                                interviewPunyaCalonMahasiswa);
                    } catch (Exception e) {
                        ais.common.Common.tampilErrorJikaAdmin(e);
                    }

                    // Buka WA pewawancara di browser
                    if (pewawancara != null) {
                        String hp = pewawancara.ambilNoHp();
                        String linkWa = "https://api.whatsapp.com/send?phone=" + hp + "&text="
                                + URLEncoder.encode(body.replaceAll("<br>", "\n"), "UTF-8");
                        Executions.getCurrent().sendRedirect(linkWa, "_blank");

                        // Kirim notifikasi WA otomatis jika konfigurasi diaktifkan
                        if (Common.bolehKonfigurasi("aktifkan_kirim_notif_interview_calon_mahasiswa_ke_wa")) {
                            String bodyWa = buatBodyNotifikasiWa(biodataCalonMahasiswa,
                                    interviewPunyaCalonMahasiswa, linkB);

                            String namaInstitusi = biodataCalonMahasiswa.getGelombangPendaftaran()
                                    .getPerguruanTinggi().getNama();

                            String sendPewawancara = "*Notifikasi Interview Calon Mahasiswa*\r\n\r\n"
                                    + "*Yth. Bapak/Ibu Panitia PMB,*\r\n\r\n"
                                    + "Dengan hormat,\r\n\r\n"
                                    + "Kami ingin memberitahukan bahwa calon mahasiswa atas nama:\r\n\r\n"
                                    + bodyWa + "\r\n\r\n"
                                    + "telah menyatakan kesiapannya untuk melaksanakan proses interview."
                                    + " Rincian data diri dan dokumen pendukung calon mahasiswa bisa"
                                    + " di download di " + linkB + ".\r\n\r\n"
                                    + "Mohon untuk mempersiapkan pelaksanaan interview dapat dilakukan"
                                    + " segera dengan menghubungi nomor "
                                    + (biodataCalonMahasiswa.getHp() != null ? biodataCalonMahasiswa.getHp() : "-")
                                    + ".\r\n\r\nTerima kasih atas perhatian dan kerjasamanya.\r\n";

                            Wa.kirimWaViaUltramsg(hp, sendPewawancara, null, null, Wa.buatProfile(null,
                                    biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi()));

                            String sendCama = "*Kepada Yth. " + biodataCalonMahasiswa.getNama() + "*\r\n\r\n"
                                    + "Terima kasih atas partisipasinya dalam proses Penerimaan Mahasiswa Baru di "
                                    + namaInstitusi + ".\r\n\r\n"
                                    + "Informasi selanjutnya, mohon menunggu antrian proses wawancara"
                                    + " (interview) yang akan disampaikan segera oleh tim interviewer ("
                                    + pewawancara.getNama() + " / No Telp/Wa " + hp
                                    + "). Jika ada informasi terbaru kami akan memberitahukan kembali"
                                    + " di nomor ini melalui sms/wa. Informasi biodata Anda sbb:\n"
                                    + bodyWa + "\n*Link Biodata: " + linkB + "\n\n"
                                    + "Terima kasih.\r\n\r\nHormat kami,\r\n\r\nPanitia PMB " + namaInstitusi;

                            Wa.kirimWaViaUltramsg(
                                    biodataCalonMahasiswa.getHp() != null ? biodataCalonMahasiswa.getHp() : "",
                                    sendCama, null, null, Wa.buatProfile(null,
                                            biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi()));
                        }
                    }
                }
            }
        });
        siapInterview.setParent(toolbar);

        // Listener checkbox: sinkronkan state ke DB dan tampilkan/sembunyikan tombol siap
        EventListener siapListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                siapInterview.setVisible(siap.isChecked());
                interviewPunyaCalonMahasiswa.setSiap(siap.isChecked());
                Common.refreshUpdate(interviewPunyaCalonMahasiswa);
            }
        };
        siapListener.onEvent(null);
        siap.addEventListener("onClick", siapListener);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.detach();
            }
        });
        cancel.setParent(toolbar);

        addWindow.onModal();
    }

    // -----------------------------------------------------------------------
    // Grid renderer
    // -----------------------------------------------------------------------

    /**
     * Renderer baris grid untuk menampilkan satu record {@link InterviewCalonMahasiswa}
     * pada tabel daftar sesi interview di halaman admin.
     * <p>Setiap baris menampilkan: detail peserta (via {@link InterviewPunyaCalonMahasiswaDetailAction}),
     * nama sesi, fakultas/prodi, nama pewawancara, kapasitas terisi, tanggal mulai dan sampai,
     * keterangan, checkbox aktif, checkbox wajib ujian, tombol video conference (jika aktif),
     * serta tombol salin/ubah/hapus.</p>
     */
    class InterviewCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

        /**
         * Merender satu baris tabel untuk satu sesi interview.
         *
         * @param arg0 komponen Row ZK yang akan diisi
         * @param arg1 object data; harus berupa {@link InterviewCalonMahasiswa}
         * @throws Exception jika terjadi kesalahan rendering
         */
        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final InterviewCalonMahasiswa interviewCalonMahasiswa = (InterviewCalonMahasiswa) arg1;

            // Detail peserta pada sesi ini
            (new InterviewPunyaCalonMahasiswaDetailAction(interviewCalonMahasiswa, edit)).setParent(arg0);

            // Nama sesi + riwayat revisi
            Vbox a;
            (a = RevisiHelper.createNewRevisi(InterviewCalonMahasiswa.class, interviewCalonMahasiswa,
                    interviewCalonMahasiswa.getNama())).setParent(arg0);

            if (interviewCalonMahasiswa.getFakultas() != null) {
                new Label(interviewCalonMahasiswa.getFakultas().getNama()).setParent(a);
            }
            if (interviewCalonMahasiswa.getJurusan() != null) {
                new Label(interviewCalonMahasiswa.getJurusan().getNama()).setParent(a);
            }

            new Label(interviewCalonMahasiswa.getPegawai().getNama()).setParent(arg0);

            int masuk = ((Number) HibernateUtil.currentSession()
                    .createCriteria(InterviewPunyaCalonMahasiswa.class)
                    .add(Restrictions.eq("interviewCalonMahasiswa", interviewCalonMahasiswa))
                    .setProjection(Projections.rowCount()).uniqueResult()).intValue();
            new Label(Common.numberFormat.get().format(masuk) + " / "
                    + Common.numberFormat.get().format(interviewCalonMahasiswa.getKapasitasRuangan()))
                    .setParent(arg0);

            new Label(Common.dateFormat.get().format(interviewCalonMahasiswa.getMulai())).setParent(arg0);
            new Label(Common.dateFormat.get().format(interviewCalonMahasiswa.getSampai())).setParent(arg0);
            new Label(interviewCalonMahasiswa.getKeterangan()).setParent(arg0);

            // Checkbox aktif
            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(interviewCalonMahasiswa.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    interviewCalonMahasiswa.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(interviewCalonMahasiswa);
                }
            });

            // Checkbox wajib ujian terlebih dahulu
            final MyCheckboxConfig hrsUjian = new MyCheckboxConfig("Hrs Ujian");
            hrsUjian.setDisabled(!edit);
            hrsUjian.setChecked(interviewCalonMahasiswa.getHrsUjian());
            hrsUjian.setParent(arg0);
            hrsUjian.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    interviewCalonMahasiswa.setHrsUjian(hrsUjian.isChecked());
                    Common.refreshSaveOrUpdate(interviewCalonMahasiswa);
                }
            });

            // Tombol salin/ubah/hapus + tombol video conference jika aktif
            Hbox s;
            (s = Common.copyEditDeleteButtons(edit, edit, delete, interviewCalonMahasiswa,
                    InterviewCalonMahasiswaAction.this, true)).setParent(arg0);

            if (!interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.TIDAK_AKTIF)) {
                InterviewCalonMahasiswa.createVideoConrefrence(interviewCalonMahasiswa, s, true, false,
                        new EventListener() {
                            @Override
                            public void onEvent(Event arg0) throws Exception {
                                // tidak ada aksi tambahan setelah membuka video conference
                            }
                        });
            }
        }
    }

    // -----------------------------------------------------------------------
    // CRUD: Add / Edit form
    // -----------------------------------------------------------------------

    /**
     * Dipanggil ketika pengguna menekan tombol Tambah di toolbar.
     * Membuat instance baru {@link InterviewCalonMahasiswa} dan membuka window form.
     *
     * @param event event klik dari toolbar; bisa null
     * @throws Exception jika terjadi kesalahan inisialisasi form
     */
    public void onAdd(Event event) throws Exception {
        init(new InterviewCalonMahasiswa());
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    /**
     * Implementasi {@link DataInitDefault#init(GeneralValueObject)}.
     * Dipanggil oleh mekanisme copy/edit untuk membuka form dengan data yang sudah ada.
     *
     * @param obj objek data; harus berupa {@link InterviewCalonMahasiswa}
     * @throws Exception jika terjadi kesalahan inisialisasi form
     */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        interviewCalonMahasiswa = (InterviewCalonMahasiswa) obj;
        init(interviewCalonMahasiswa);
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    /**
     * Menginisialisasi seluruh komponen form tambah/ubah sesi interview.
     * <p>Metode ini membangun ulang konten {@link #addWindow} setiap kali dipanggil
     * untuk memastikan tidak ada sisa data dari form sebelumnya.</p>
     * <p>Form mencakup:</p>
     * <ul>
     *   <li>Judul interview (wajib)</li>
     *   <li>Pegawai pewawancara (wajib, via Banbox)</li>
     *   <li>Fakultas dan Prodi target</li>
     *   <li>Tahun akademik dan gelombang pendaftaran (multi-select)</li>
     *   <li>Tanggal mulai dan sampai</li>
     *   <li>Kapasitas/kuota ruangan</li>
     *   <li>Media online (Jitsi, Google Meet, Zoom, BBB, Skype, WA, Lain-lain)</li>
     *   <li>Link untuk setiap platform yang dipilih (conditional visibility)</li>
     *   <li>Tombol tes koneksi online</li>
     *   <li>Keterangan</li>
     * </ul>
     *
     * @param interviewCalonMahasiswa data yang akan diisi ke dalam form;
     *                                jika {@code id == null} maka mode tambah, sebaliknya mode ubah
     * @throws Exception jika terjadi kesalahan pembuatan komponen ZK
     */
    @SuppressWarnings("deprecation")
    private void init(final InterviewCalonMahasiswa interviewCalonMahasiswa) throws Exception {
        this.interviewCalonMahasiswa = interviewCalonMahasiswa;
        addWindow.setTitle(interviewCalonMahasiswa.getId() == null
                ? "Tambah Interview Calon Mahasiswa" : "Ubah Interview Calon Mahasiswa");
        Common.clear(addWindow);

        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        Center center = new Center();
        center.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(center, true);

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setParent(center);
        grid.setHeight("100%");

        Columns columns = new Columns();
        columns.setParent(grid);

        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        column.setWidth("30%");
        column = new MyColumnConfig();
        column.setParent(columns);

        Rows rows = new Rows();
        rows.setParent(grid);

        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Judul Interview *"));
        row.appendChild(nama = new Textbox(interviewCalonMahasiswa.getNama()));
        nama.setWidth("90%");

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
        row.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
        pegawai.setValue(interviewCalonMahasiswa.getPegawai() == null
                ? "" : interviewCalonMahasiswa.getPegawai().getNama());
        pegawai.setAttribute("myValue", interviewCalonMahasiswa.getPegawai());
        pegawai.setAttribute("pegawai", interviewCalonMahasiswa.getPegawai());
        pegawai.setWidth("90%");
        pegawai.setReadonly(true);

        Tbmuser tbmuser = Common.getCurrentUser();
        fakultas = new Combobox();
        jurusan = new Combobox();
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
        Common.selectComboItem(fakultas, interviewCalonMahasiswa.getFakultas() == null
                ? tbmuser.ambilFakultas() : interviewCalonMahasiswa.getFakultas());
        row.appendChild(fakultas);
        fakultas.setWidth("90%");

        if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
            Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
                    Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                    CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
        }

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
        Common.pilihJurusan(jurusan, interviewCalonMahasiswa.getJurusan() == null
                ? tbmuser.ambilJurusan() : interviewCalonMahasiswa.getJurusan());
        row.appendChild(jurusan);
        jurusan.setWidth("90%");

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
        row.appendChild(tahunAkademik = new Combobox());
        Common.generateTahunAjaran(tahunAkademik);
        if (interviewCalonMahasiswa.getId() == null) {
            Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);
        } else {
            Common.selectComboItem(tahunAkademik, interviewCalonMahasiswa.getTahunAkademik());
        }
        tahunAkademik.setWidth("90%");

        row = new MyFormRow();
        row.appendChild(new ais.ui.util.MyLabelConfig());
        final MyCheckboxConfig gelombangPendaftaranForm;
        row.appendChild(gelombangPendaftaranForm = new MyCheckboxConfig("Untuk Gelombang Pendaftaran"));
        row.setParent(rows);

        row = new MyFormRow();
        row.setParent(rows);
        ais.ui.util.ZkCompat.setSpans(row, "2");
        final MyGrid subGridGelombangPendaftaran = new MyGrid();
        row.appendChild(subGridGelombangPendaftaran);

        Columns subColumns = new Columns();
        subColumns.setParent(subGridGelombangPendaftaran);
        Column c = new Column("Gelombang Pendaftaran");
        subColumns.appendChild(c);

        Rows subRows = new Rows();
        subRows.setParent(subGridGelombangPendaftaran);

        final MyFormRow subRow = new MyFormRow();
        subRow.setStyle("border:0px;background: transparent;");
        subRow.setParent(subRows);
        subRow.setValign("top");

        EventListener gelombangEventListener = new EventListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onEvent(Event arg0) throws Exception {
                List<GelombangPendaftaran> gelombangPendaftarans = ConstantValues.simpleList(
                        HibernateUtil.currentSession().createCriteria(GelombangPendaftaran.class)
                                .add(tahunAkademik.getSelectedItem() == null
                                        || tahunAkademik.getSelectedItem().getValue() == null
                                                ? Restrictions.sqlRestriction("true")
                                                : Restrictions.eq("tahunAkademik",
                                                        tahunAkademik.getSelectedItem().getValue()))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                        GelombangPendaftaran.class);

                if (interviewCalonMahasiswa.getId() != null) {
                    HibernateUtil.currentSession()
                            .refresh(InterviewCalonMahasiswaAction.this.interviewCalonMahasiswa);
                }
                selectedGelombangPendaftaran = new HashSet<GelombangPendaftaran>();
                for (GelombangPendaftaran gp : interviewCalonMahasiswa.ambilGelombangPendaftaran()) {
                    selectedGelombangPendaftaran.add(gp);
                }

                Set<Long> ids = new HashSet<Long>();
                for (GelombangPendaftaran v : selectedGelombangPendaftaran) {
                    ids.add(v.getId());
                }

                System.out.println("ids ->" + ids);

                subGridGelombangPendaftaran.setVisible(!selectedGelombangPendaftaran.isEmpty());
                gelombangPendaftaranForm.setChecked(!selectedGelombangPendaftaran.isEmpty());

                gelombangPendaftaranForm.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        subGridGelombangPendaftaran.setVisible(gelombangPendaftaranForm.isChecked());
                    }
                });

                Vbox vboxSkala = new Vbox();
                vboxSkala.setPack("top");
                vboxSkala.setParent(subRow);
                for (final GelombangPendaftaran gp : gelombangPendaftarans) {
                    final Checkbox checkboxGelombang = new Checkbox(gp.getNama());
                    checkboxGelombang.setParent(vboxSkala);
                    checkboxGelombang.setChecked(ids.contains(gp.getId()));
                    checkboxGelombang.addEventListener("onClick", new EventListener() {
                        @Override
                        public void onEvent(Event arg0) throws Exception {
                            if (checkboxGelombang.isChecked()) {
                                selectedGelombangPendaftaran.add(gp);
                            } else {
                                for (GelombangPendaftaran a : selectedGelombangPendaftaran) {
                                    if (a.getId().equals(gp.getId())) {
                                        selectedGelombangPendaftaran.remove(a);
                                        break;
                                    }
                                }
                            }
                            System.out.println("selectedGelombangPendaftaran => " + selectedGelombangPendaftaran);
                        }
                    });
                }
            }
        };

        gelombangEventListener.onEvent(null);
        tahunAkademik.addEventListener("onChange", gelombangEventListener);

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
        row.appendChild(mulai = new MyDatebox(interviewCalonMahasiswa.getMulai()));
        mulai.setReadonly(true);
        mulai.setFormat(Common.dateFormat.get().toPattern());
        mulai.setWidth("90%");

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
        row.appendChild(sampai = new MyDatebox(interviewCalonMahasiswa.getSampai()));
        sampai.setReadonly(true);
        sampai.setFormat(Common.dateFormat.get().toPattern());
        sampai.setWidth("90%");

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas/Kuota"));
        row.appendChild(kapasitasRuangan = new MyIntbox(interviewCalonMahasiswa.getKapasitasRuangan()));

        row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Media Online"));
        onlineMenggunakan = new Combobox();

        Comboitem mediaOnline = new Comboitem("Jitsi", "/img/jitsi.png");
        mediaOnline.setValue(InterviewCalonMahasiswa.JITSI);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Google Meet", "/img/meet-google.png");
        mediaOnline.setValue(InterviewCalonMahasiswa.GOOGLE_MEET);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Zoom", "/img/zoom.png");
        mediaOnline.setValue(InterviewCalonMahasiswa.ZOOM);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Big Blue Button", "/img/bbb.png");
        mediaOnline.setValue(InterviewCalonMahasiswa.BBB);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Skype", "/img/Skype-icon.png");
        mediaOnline.setValue(InterviewCalonMahasiswa.SKYPE);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Grup Whatsapp", "/img/svg/whats.svg");
        mediaOnline.setValue(Pertemuan.WA);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Lain-Lain", "/img/online-red-icon.png");
        mediaOnline.setValue(Pertemuan.LAIN);
        onlineMenggunakan.appendChild(mediaOnline);

        mediaOnline = new Comboitem("Tidak Ada Tatap Muka Online", "/img/svg/trash.svg");
        mediaOnline.setValue(InterviewCalonMahasiswa.TIDAK_AKTIF);
        onlineMenggunakan.appendChild(mediaOnline);

        Common.selectComboItem(onlineMenggunakan, interviewCalonMahasiswa.getOnlineMenggunakan());
        onlineMenggunakan.setCols(7);

        Hbox myonlineMenggunakan = new Hbox();
        row.appendChild(myonlineMenggunakan);
        myonlineMenggunakan.appendChild(onlineMenggunakan);

        final MyToolbarbuttonConfig testButton = new MyToolbarbuttonConfig("Tes Online Sekarang");
        myonlineMenggunakan.appendChild(testButton);
        testButton.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();
                String url = "";
                if (ol.equals(InterviewCalonMahasiswa.GOOGLE_MEET)) {
                    String l = interviewCalonMahasiswa.retreive("hangoutLink");
                    if (l == null || l.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Mohon maaf, untuk pelaksanaan tatap muka daring (online) menggunakan Google Meet, tautan pertemuan belum tersedia. Langkah yang dapat dilakukan: (1) lakukan sinkronisasi terlebih dahulu ke Google Calendar dengan menekan tombol Kalendar; (2) pastikan proses sinkronisasi berhasil; (3) ulangi kembali proses membuka tautan Google Meet.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    url = l + "?hs=122&ijlm=1588886137268";
                } else if (ol.equals(InterviewCalonMahasiswa.JITSI)) {
                    url = interviewCalonMahasiswa.generateJitsiLink();
                } else if (ol.equals(InterviewCalonMahasiswa.ZOOM)) {
                    url = interviewCalonMahasiswa.getZoomLink();
                } else if (ol.equals(InterviewCalonMahasiswa.BBB)) {
                    url = interviewCalonMahasiswa.getBbbLink();
                } else if (ol.equals(InterviewCalonMahasiswa.SKYPE)) {
                    url = interviewCalonMahasiswa.getSkypeLink();
                } else if (ol.equals(InterviewCalonMahasiswa.WA)) {
                    url = interviewCalonMahasiswa.getWaLink();
                } else if (ol.equals(InterviewCalonMahasiswa.LAIN)) {
                    url = interviewCalonMahasiswa.getLainLink();
                }
                if (url == null || url.trim().isEmpty()) {
                    MyMessageboxConfig.show(
                            "Mohon maaf, tautan (link) pertemuan daring belum terisi dengan benar. Langkah yang dapat dilakukan: (1) untuk tatap muka daring menggunakan Zoom, Big Blue Button, Skype, atau WhatsApp, masukkan tautan pertemuan secara lengkap dan benar; (2) pastikan tautan dapat diakses; (3) simpan dan ulangi kembali proses membuka tautan pertemuan.",
                            "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    return;
                }
                if (Common.isMobile()) {
                    ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                } else {
                    Clients.evalJavaScript(
                            "popupCenter({url: '" + url + "', title: 'Video Conference', w: 1200, h: 600});");
                }
            }
        });

        onlineMenggunakan.setReadonly(true);

        Common.initKeterangan(rows,
                "Jika terdapat wawancara atau kegiatan tatap muka secara online, pilihlah salah satu media online.");

        // ---------------------------------------------------------------
        // Baris Google Meet
        // ---------------------------------------------------------------
        rowMeetKeterangan = Common.initKeterangan(rows,
                "Untuk tatap muka online menggunakan Google Meet, harap singkronkan dulu ke Google Calendar di bawah ini.");

        rowMeet = new MyFormRow();
        rowMeet.setValign("top");
        rowMeet.setParent(rows);
        rowMeet.appendChild(new Label());
        rowMeet.appendChild(AktifitasPerkuliahanHelper.createCalendarButton(interviewCalonMahasiswa,
                Common.getCurrentUser(), true, new DataLoader() {
                    @Override
                    public void loadData(Object value) {
                        // tidak ada tindakan pasca-sync yang diperlukan di sini
                    }
                }));

        // ---------------------------------------------------------------
        // Baris Zoom
        // ---------------------------------------------------------------
        rowLinkZoomKeterangan = Common.initKeterangan(rows,
                "Untuk tatap muka online menggunakan Zoom, harap memasukkan link zoom di bawah ini. Contoh link zoom : https://us04web.zoom.us/j/4445712881?pwd=ZnNReHRJYXVRem8zRkc5OFpPd3I3QT09");

        rowLinkZoomLink = new MyFormRow();
        rowLinkZoomLink.setValign("top");
        rowLinkZoomLink.setParent(rows);
        rowLinkZoomLink.appendChild(new ais.ui.util.MyLabelConfig(""));
        A linkZoomSignup;
        rowLinkZoomLink.appendChild(linkZoomSignup = new A(
                "Klik disini dan login untuk mendapatkan link zoom yang baru, https://zoom.us/signin"));
        linkZoomSignup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                String server = "https://zoom.us/signin";
                if (Common.isMobile()) {
                    ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
                } else {
                    Clients.evalJavaScript(
                            "popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
                }
            }
        });

        rowLinkZoom = new MyFormRow();
        rowLinkZoom.setValign("top");
        rowLinkZoom.setParent(rows);
        rowLinkZoom.appendChild(new ais.ui.util.MyLabelConfig("Link Zoom *"));
        rowLinkZoom.appendChild(zoomLink = new Textbox(interviewCalonMahasiswa.getZoomLink()));
        zoomLink.setWidth("90%");
        zoomLink.setRows(2);

        rowLinkZoomButton = Common.initKeterangan(rows,
                "Secara default, link zoom akan menggunakan link zoom dari interviewCalonMahasiswa sebelumnya..");

        // ---------------------------------------------------------------
        // Baris BigBlueButton
        // ---------------------------------------------------------------
        rowLinkBbbKeterangan = Common.initKeterangan(rows,
                "Untuk tatap muka online menggunakan Big Blue Button, harap memasukkan link Big Blue Button di bawah ini. Contoh link bbb : https://demo.bigbluebutton.org/gl/muh-jjn-72p");

        rowLinkBbbLink = new MyFormRow();
        rowLinkBbbLink.setValign("top");
        rowLinkBbbLink.setParent(rows);
        rowLinkBbbLink.appendChild(new ais.ui.util.MyLabelConfig(""));
        A linkBbbSignup;
        rowLinkBbbLink.appendChild(linkBbbSignup = new A(
                "Klik disini dan login untuk mendapatkan link Big Blue Button yang baru, https://demo.bigbluebutton.org/gl/signin"));
        linkBbbSignup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                String server = "https://demo.bigbluebutton.org/gl/signin";
                if (Common.isMobile()) {
                    ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
                } else {
                    Clients.evalJavaScript(
                            "popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
                }
            }
        });

        rowLinkBbb = new MyFormRow();
        rowLinkBbb.setValign("top");
        rowLinkBbb.setParent(rows);
        rowLinkBbb.appendChild(new ais.ui.util.MyLabelConfig("Link Big Blue Button *"));
        rowLinkBbb.appendChild(bbbLink = new Textbox(interviewCalonMahasiswa.getBbbLink()));
        bbbLink.setWidth("90%");
        bbbLink.setRows(2);

        rowLinkBbbButton = Common.initKeterangan(rows,
                "Secara default, link Big Blue Button akan menggunakan link Big Blue Button dari interviewCalonMahasiswa sebelumnya..");

        // ---------------------------------------------------------------
        // Baris Skype
        // ---------------------------------------------------------------
        rowLinkSkypeKeterangan = Common.initKeterangan(rows,
                "Untuk tatap muka online menggunakan Skype, harap memasukkan link Skype di bawah ini. Contoh link skype : https://join.skype.com/Ut2b1onFnJnD");

        rowLinkSkypeLink = new MyFormRow();
        rowLinkSkypeLink.setValign("top");
        rowLinkSkypeLink.setParent(rows);
        rowLinkSkypeLink.appendChild(new ais.ui.util.MyLabelConfig(""));
        A linkSkypeSignup;
        rowLinkSkypeLink.appendChild(linkSkypeSignup = new A(
                "Klik disini dan login untuk mendapatkan link Skype yang baru, https://web.skype.com"));
        linkSkypeSignup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                String server = "https://web.skype.com";
                if (Common.isMobile()) {
                    ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
                } else {
                    Clients.evalJavaScript(
                            "popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
                }
            }
        });

        rowLinkSkype = new MyFormRow();
        rowLinkSkype.setValign("top");
        rowLinkSkype.setParent(rows);
        rowLinkSkype.appendChild(new ais.ui.util.MyLabelConfig("Link Skype *"));
        rowLinkSkype.appendChild(skypeLink = new Textbox(interviewCalonMahasiswa.getSkypeLink()));
        skypeLink.setWidth("90%");
        skypeLink.setRows(2);

        rowLinkSkypeButton = Common.initKeterangan(rows,
                "Secara default, link Skype akan menggunakan link Skype dari interviewCalonMahasiswa sebelumnya..");

        // ---------------------------------------------------------------
        // Baris WhatsApp Grup
        // ---------------------------------------------------------------
        rowLinkWa = new MyFormRow();
        rowLinkWa.setValign("top");
        rowLinkWa.setParent(rows);
        rowLinkWa.appendChild(new ais.ui.util.MyLabelConfig("Link Grup Whatsapp *"));
        rowLinkWa.appendChild(waLink = new Textbox(interviewCalonMahasiswa.getWaLink()));
        waLink.setWidth("90%");
        waLink.setRows(2);

        rowLinkWaButton = Common.initKeterangan(rows,
                "Secara default, link Grup Whatsapp akan menggunakan link Grup Whatsapp dari interviewCalonMahasiswa sebelumnya..");

        rowLinkWaKeterangan = Common.initKeterangan(rows,
                "Untuk tatap muka online menggunakan Grup WA, harap memasukkan link WA di atas. Untuk membuat link Grup WA, buka aplikasi WA Grup Anda (harus sebagai admin) atau buat grup WA baru, pilih Grup Info, dan pilih undang via link.. Contoh link : https://chat.whatsapp.com/Djx0r98Z30YTmFmEZGJ3");

        // ---------------------------------------------------------------
        // Baris Lain-lain
        // ---------------------------------------------------------------
        rowLinkLain = new MyFormRow();
        rowLinkLain.setValign("top");
        rowLinkLain.setParent(rows);
        rowLinkLain.appendChild(new ais.ui.util.MyLabelConfig("Link Media Online *"));
        // FIX: semula menggunakan getWaLink() — seharusnya getLainLink()
        rowLinkLain.appendChild(linkLain = new Textbox(interviewCalonMahasiswa.getLainLink()));
        linkLain.setWidth("90%");
        linkLain.setRows(2);

        rowLinkLainKeterangan = Common.initKeterangan(rows,
                "Untuk tatap muka online menggunakan media online lain, harap memasukkan link media tersebut di bawah ini.");

        // ---------------------------------------------------------------
        // Listener onChange media online: tampilkan/sembunyikan baris terkait
        // ---------------------------------------------------------------
        EventListener eventListenerOl = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();

                rowMeetKeterangan.setVisible(ol.equals(InterviewCalonMahasiswa.GOOGLE_MEET));
                rowMeet.setVisible(ol.equals(InterviewCalonMahasiswa.GOOGLE_MEET));

                rowLinkZoomKeterangan.setVisible(ol.equals(InterviewCalonMahasiswa.ZOOM));
                rowLinkZoom.setVisible(ol.equals(InterviewCalonMahasiswa.ZOOM));
                rowLinkZoomButton.setVisible(ol.equals(InterviewCalonMahasiswa.ZOOM));
                rowLinkZoomLink.setVisible(ol.equals(InterviewCalonMahasiswa.ZOOM));

                rowLinkBbbKeterangan.setVisible(ol.equals(InterviewCalonMahasiswa.BBB));
                rowLinkBbb.setVisible(ol.equals(InterviewCalonMahasiswa.BBB));
                rowLinkBbbButton.setVisible(ol.equals(InterviewCalonMahasiswa.BBB));
                rowLinkBbbLink.setVisible(ol.equals(InterviewCalonMahasiswa.BBB));

                rowLinkSkypeKeterangan.setVisible(ol.equals(InterviewCalonMahasiswa.SKYPE));
                rowLinkSkype.setVisible(ol.equals(InterviewCalonMahasiswa.SKYPE));
                rowLinkSkypeButton.setVisible(ol.equals(InterviewCalonMahasiswa.SKYPE));
                rowLinkSkypeLink.setVisible(ol.equals(InterviewCalonMahasiswa.SKYPE));

                rowLinkWa.setVisible(ol.equals(InterviewCalonMahasiswa.WA));
                rowLinkWaButton.setVisible(ol.equals(InterviewCalonMahasiswa.WA));
                waLink.setVisible(ol.equals(InterviewCalonMahasiswa.WA));
                rowLinkWaKeterangan.setVisible(ol.equals(InterviewCalonMahasiswa.WA));

                rowLinkLain.setVisible(ol.equals(InterviewCalonMahasiswa.LAIN));
                linkLain.setVisible(ol.equals(InterviewCalonMahasiswa.LAIN));
                rowLinkLainKeterangan.setVisible(ol.equals(InterviewCalonMahasiswa.LAIN));

                testButton.setVisible(true);
                if (ol.equals(InterviewCalonMahasiswa.GOOGLE_MEET)) {
                    testButton.setImage("/img/meet-google.png");
                } else if (ol.equals(InterviewCalonMahasiswa.JITSI)) {
                    testButton.setImage("/img/jitsi.png");
                } else if (ol.equals(InterviewCalonMahasiswa.ZOOM)) {
                    testButton.setImage("/img/zoom.png");
                } else if (ol.equals(InterviewCalonMahasiswa.BBB)) {
                    testButton.setImage("/img/bbb.png");
                } else if (ol.equals(InterviewCalonMahasiswa.SKYPE)) {
                    testButton.setImage("/img/Skype-icon.png");
                } else if (ol.equals(InterviewCalonMahasiswa.WA)) {
                    testButton.setImage("/img/svg/whats.svg");
                } else if (ol.equals(InterviewCalonMahasiswa.LAIN)) {
                    testButton.setImage("/img/online-red-icon.png");
                } else {
                    testButton.setVisible(false);
                }
            }
        };

        onlineMenggunakan.addEventListener("onChange", eventListenerOl);
        eventListenerOl.onEvent(null);

        // Keterangan / catatan bebas
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
        row.appendChild(keterangan = new Textbox(interviewCalonMahasiswa.getKeterangan()));
        keterangan.setWidth("90%");
        keterangan.setRows(3);

        // Toolbar bawah form
        South south = new South();
        ais.ui.util.ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
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

        borderlayout.setParent(addWindow);
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    /**
     * Memvalidasi dan menyimpan data sesi interview dari form tambah/ubah.
     * <p>Validasi yang dilakukan:</p>
     * <ul>
     *   <li>Judul interview tidak boleh kosong</li>
     *   <li>Pegawai pewawancara harus dipilih</li>
     * </ul>
     * <p>Jika validasi lolos, seluruh field form disinkronisasi ke entitas
     * {@link InterviewCalonMahasiswa} dan disimpan/diperbarui via
     * {@link Common#refreshSaveOrUpdate(Session, Object)}.</p>
     *
     * @param event event dari tombol Simpan; tidak digunakan langsung
     * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
     * @throws Exception jika terjadi kesalahan akses database
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().equals("")) {
            MyMessageboxConfig.show(
                    "Mohon Bapak/Ibu melengkapi Judul Interview terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Judul Interview; (2) pastikan judul tidak dikosongkan; (3) simpan kembali data interview.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (pegawai.getAttribute("pegawai") == null) {
            MyMessageboxConfig.show(
                    "Mohon Bapak/Ibu melengkapi isian Pegawai (pewawancara) terlebih dahulu. Langkah yang dapat dilakukan: (1) klik kolom pemilihan Pegawai; (2) pilih pegawai yang bertugas sebagai pewawancara; (3) simpan kembali data interview.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        if (interviewCalonMahasiswa.getId() != null) {
            interviewCalonMahasiswa = (InterviewCalonMahasiswa) session.load(InterviewCalonMahasiswa.class,
                    interviewCalonMahasiswa.getId());
        }

        interviewCalonMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
        interviewCalonMahasiswa.setMulai(mulai.getValue());
        interviewCalonMahasiswa.setSampai(sampai.getValue());
        interviewCalonMahasiswa.setOnlineMenggunakan(
                (Integer) (onlineMenggunakan == null || onlineMenggunakan.getSelectedItem() == null
                        ? null : onlineMenggunakan.getSelectedItem().getValue()));
        interviewCalonMahasiswa.setZoomLink(zoomLink.getValue().trim());
        interviewCalonMahasiswa.setBbbLink(bbbLink.getValue().trim());
        interviewCalonMahasiswa.setSkypeLink(skypeLink.getValue().trim());
        interviewCalonMahasiswa.setWaLink(waLink.getValue().trim());
        interviewCalonMahasiswa.setLainLink(linkLain.getValue().trim());
        interviewCalonMahasiswa.setNama(nama.getValue());
        interviewCalonMahasiswa.setKeterangan(keterangan.getValue());
        interviewCalonMahasiswa.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
        interviewCalonMahasiswa.setKapasitasRuangan(kapasitasRuangan.getValue());
        interviewCalonMahasiswa.setJurusan(
                (Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
        interviewCalonMahasiswa.setFakultas(
                (Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));

        String jenisS = "";
        for (GelombangPendaftaran gp : this.selectedGelombangPendaftaran) {
            jenisS += jenisS.isEmpty() ? gp.getId().toString() : "," + gp.getId();
        }
        interviewCalonMahasiswa.setGelombangPendaftaranLain(jenisS);

        Common.refreshSaveOrUpdate(session, interviewCalonMahasiswa);
        return true;
    }

    // -----------------------------------------------------------------------
    // Search / Criteria
    // -----------------------------------------------------------------------

    /**
     * Membangun {@link Criteria} Hibernate untuk pencarian daftar sesi interview.
     * <p>Filter yang didukung:</p>
     * <ul>
     *   <li>Nama sesi interview (ilike, ANYWHERE)</li>
     *   <li>Nama pegawai pewawancara (ilike, ANYWHERE)</li>
     *   <li>Nama atau nomor registrasi calon mahasiswa yang ada di sesi ini
     *       (sub-query via {@link InterviewPunyaCalonMahasiswa})</li>
     *   <li>Status aktif</li>
     *   <li>Tahun akademik</li>
     *   <li>Pegawai saat ini (jika bukan admin tanpa pegawai terkait)</li>
     * </ul>
     * <p><strong>Catatan privasi</strong>: Filter pegawai memastikan bahwa seorang
     * pewawancara hanya melihat sesi interview yang dia tangani sendiri. Hanya pengguna
     * yang tidak memiliki pegawai terkait (admin sistem) yang dapat melihat semua sesi.</p>
     *
     * @param order jika {@code true}, tambahkan ORDER BY id DESC
     * @return Criteria yang sudah dikonfigurasi
     */
    @SuppressWarnings("unchecked")
    public Criteria initCriteria(boolean order) {
        Tbmuser tbmuser = Common.getCurrentUser();
        Pegawai currentPegawai = tbmuser == null ? null : tbmuser.ambilPegawai();

        Session session = HibernateUtil.currentSession();

        List<Long> ids = new ArrayList<Long>();
        if (!searchcalon.getValue().trim().isEmpty()) {
            ids = session.createCriteria(InterviewPunyaCalonMahasiswa.class)
                    .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
                    .add(Restrictions.or(
                            Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi",
                                    searchcalon.getValue().trim(), MatchMode.ANYWHERE),
                            Restrictions.ilike("biodataCalonMahasiswa.nama",
                                    searchcalon.getValue().trim(), MatchMode.ANYWHERE)))
                    .add(Restrictions.isNotNull("interviewCalonMahasiswa"))
                    .setProjection(Projections.groupProperty("interviewCalonMahasiswa.id")).list();
        }

        Criteria criteria = session.createCriteria(InterviewCalonMahasiswa.class)
                .add(currentPegawai == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("pegawai", currentPegawai))
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchTahunAjaran.getSelectedItem() == null
                        || searchTahunAjaran.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("true")
                                : Restrictions.eq("tahunAkademik",
                                        searchTahunAjaran.getSelectedItem().getValue()));

        if (!ids.isEmpty()) {
            criteria.add(Restrictions.in("id", ids));
        }

        if (searchpegawai != null && !searchpegawai.getValue().trim().isEmpty()) {
            criteria.createAlias("pegawai", "pegawai")
                    .add(Restrictions.ilike("pegawai.nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE));
        }

        if (order) {
            criteria.addOrder(Order.desc("id"));
        }

        criteria.add(searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

        return criteria;
    }

    /**
     * Menjalankan pencarian dan memperbarui tampilan grid dengan hasil yang ditemukan.
     * Memanggil {@link #initCriteria(boolean)} dua kali: sekali untuk menghitung total
     * (keperluan paging) dan sekali untuk mengambil data halaman aktif.
     *
     * @param event event pemicu pencarian; bisa null (dipanggil dari inisialisasi)
     */
    @SuppressWarnings("unchecked")
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);

        List<InterviewCalonMahasiswa> list = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();

        ListModel strset = new SimpleListModel(list);
        grid.setRowRenderer(new InterviewCalonMahasiswaRenderer());
        grid.setModelCheckMobile(strset);
    }
}
