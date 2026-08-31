package ais.action.master;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.epsbed.KapasitasProdiHelper;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BerkasHasilAkreditasi;
import ais.database.model.BerkasHasilAkreditasiPunyaNama;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GrupJurusan;
import ais.database.model.Jabatan;
import ais.database.model.Jenjang;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;
import ais.database.model.epsbed.FasilitasAkademikJurusan;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;

import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD data master "Jurusan" (Program Studi) — salah satu entitas paling sentral di AIS
 * (dirujuk oleh mahasiswa, kurikulum, dosen, akreditasi, dsb). Form-nya multi-tab: data pokok
 * (kode, nama, fakultas, jenjang, gelar, pejabat prodi/kaprodi, akreditasi umum &amp; kesehatan,
 * satuan kerja), lampiran gambar (kop surat, kop+stempel, ikon, info prodi — kop wajib berformat
 * JPG), "Tentang Prodi" ({@link JenjangProgramStudi}, dibuat otomatis bila belum ada saat prodi
 * pertama dibuka), Kapasitas Prodi ({@link KapasitasProdiHelper}), Fasilitas Akademik Penunjang
 * ({@link FasilitasAkademikJurusan} — luas &amp; jumlah ruang kuliah/lab/dosen/administrasi,
 * koleksi buku), dan beberapa tab lazy-load berbasis iframe ZUL terpisah (Akreditasi, Konsentrasi,
 * Capaian Pembelajaran Lulusan, Jenis Capaian Pembelajaran Lulusan). Menyimpan satu Jurusan juga
 * menyinkronkan baris {@link Staff} kaprodi (jabatan {@code ConstantValues.KAPRODI}) secara
 * otomatis.
 *
 * <p>
 * Dua integrasi eksternal opsional (keduanya di balik konfigurasi {@code terhubung_ke_dspace}/
 * {@code aktifkan_terhubung_langsung_ke_feeder}): (1) publikasi ke repositori DSpace — setiap
 * Jurusan direpresentasikan sebagai "community" DSpace ({@link #getDspace(String, Jurusan, boolean)},
 * dengan opsi Jurusan sebagai root atau anak Fakultas), dan berkas hasil akreditasi diekspor
 * sebagai "collection"/"item" DSpace berikut metadata Dublin Core lengkap
 * ({@link #getDspace(String, BerkasHasilAkreditasiPunyaNama, boolean)}); (2) sinkronisasi data
 * prodi dari Neo Feeder PDDIKTI ({@link #exportKeFeeder}, mencocokkan prodi lokal ke prodi Feeder
 * lewat kode EPSBED+nama, lalu mengimpor lewat {@link FeederJSONImport#jurusan}). Kedua alur
 * berjalan di thread terpisah dengan progres ditampilkan via {@link Label}, dan kegagalan
 * (koneksi/login gagal) SELALU ditampilkan sebagai galat eksplisit — bukan ditutupi sebagai sukses
 * semu (perbaikan atas bug lama "gagal diam-diam" yang dicatat dalam komentar kode).
 * </p>
 */
public class JurusanAction extends GenericCrudAction<Jurusan> {

    private static final long serialVersionUID = 3786091220301468178L;

    // ZK auto-wired extra search fields dari ZUL
    private Textbox searchkode;
    private Combobox searchfakultas;
    private Combobox searchjenjang;

    // ZK auto-wired Tabpanel fields (lazy-loaded tabs dari ZUL)
    private Tabpanel konsentrasiTab;
    private Tabpanel akreditasiTab;
    private Tabpanel capaianPembelajaranLulusanTab;
    private Tabpanel jenisCapaianPembelajaranLulusanTab;

    // ZK auto-wired extra UI dari ZUL
    private MyColumnConfig label_Kaprodi;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox kode;
    private Textbox kodeEpsbed;
    private Textbox feeder;
    private Textbox kodeLain;
    private Textbox nama;
    private Textbox namaEn;
    private Combobox jenjang;
    private Combobox fakultas;
    private Combobox grupJurusan;
    private Combobox jenisJurusan;
    private AmbilDataDosenBanbox kaprodi;
    private Textbox labelPejabat1;
    private AmbilDataPegawaiBanbox pegawai1;
    private Textbox labelPejabat2;
    private AmbilDataPegawaiBanbox pegawai2;
    private Textbox labelPejabat3;
    private AmbilDataPegawaiBanbox pegawai3;
    private Textbox gelar;
    private Textbox singkatanGelar;
    private Textbox gelarEn;
    private Textbox singkatanGelarEn;
    private Textbox bahasaPengantar;
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private MyCheckboxConfig dosenHarusPakaiSatuanKerja;
    private MyDoublebox rasioDosenDanMahasiswa;
    private Combobox statusAkreditasi;
    private Textbox peringkatAkreditasi;
    private Textbox akreditasi;
    private Textbox noSkAkreditasi;
    private MyDatebox tanggalAkreditasi;
    private Textbox statusAkreditasiKes;
    private Textbox peringkatAkreditasiKes;
    private Textbox akreditasiKes;
    private Textbox noSkAkreditasiKes;
    private MyDatebox tanggalAkreditasiKes;
    private Textbox wa;
    private MyTextbox alamat;
    private MyTextbox alamat2;
    private MyCkEditor deskripsi;

    // Fasilitas form fields
    private Decimalbox luasRuangKuliah;
    private Decimalbox jumlahRuangKuliah;
    private Decimalbox luasRuangLab;
    private Decimalbox jumlahRuangLab;
    private Decimalbox luasTotalRuangDosenTetap;
    private Decimalbox luasTotalRuangAdministrasi;
    private Decimalbox jumlahJudulBuku;
    private Decimalbox jumlahEksemplarBuku;
    private Decimalbox luasKebunLahanPercobaanProdi;

    // Lampiran (file attachment fields)
    protected LampiranLain kop;
    protected LampiranLain kopStempel;
    protected LampiranLain icon;
    protected LampiranLain infoProdi;

    // Helper objects
    private final JenjangProgramStudiAction jenjangProgramStudiAction = new JenjangProgramStudiAction();
    private final KapasitasProdiHelper kapasitasProdiHelper = new KapasitasProdiHelper();

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Jurusan> getEntityClass() { return Jurusan.class; }

    @Override
    protected Jurusan createNewEntity() { return new Jurusan(); }

    @Override
    protected String getWindowTitle() { return "Pendataan " + Common.getBahasaConfig("Jurusan"); }

    /** Kolom yang disertakan pada operasi unduh/unggah massal data Jurusan. */
    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "fakultas", "jenjang", "kaprodi", "deskripsi",
                "labelPejabat1", "labelPejabat2", "labelPejabat3", "pegawai1", "pegawai2", "pegawai3",
                "satuanKerja", "dosenHarusPakaiSatuanKerja" };
    }

    /**
     * Menginisialisasi filter fakultas/jenjang (dikunci ke fakultas pengguna bila terikat),
     * tombol cetak data, dan tombol integrasi opsional: ekspor ke DSpace ({@code Jurusan} dan
     * berkas hasil akreditasi), pembatalan ekspor DSpace, dan sinkronisasi ke Neo Feeder
     * (khusus admin berhak akses Feeder). Seluruh tombol integrasi hanya tampil sesuai
     * konfigurasi terkait.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onAfterInit(Component comp) throws Exception {
        if (label_Kaprodi != null) {
            label_Kaprodi.setLabel(Common.getBahasa("label_Kaprodi"));
        }

        grupJurusan = new Combobox();
        Common.insertCombo(grupJurusan, "nama", GrupJurusan.class);

        fakultas = new Combobox();
        Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class,
                Restrictions.eq("aktif", true));
        Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
                Restrictions.eq("aktif", true));

        Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

        Tbmuser tbmuser = Common.getCurrentUser();
        if (tbmuser.ambilFakultas() != null) {
            Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
            Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
            fakultas.setDisabled(true);
            searchfakultas.setDisabled(true);
        } else {
            fakultas.setDisabled(false);
            searchfakultas.setDisabled(false);
        }

        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Jurusan.class, this, "id", "kode", "nama",
                "fakultas", "jenjang", "kaprodi", "deskripsi", "labelPejabat1", "labelPejabat2", "labelPejabat3",
                "pegawai1", "pegawai2", "pegawai3", "satuanKerja", "dosenHarusPakaiSatuanKerja");
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig(
                "Ekspor " + Common.getBahasaConfig("Jurusan"), "/img/corner.gif");
        if (add != null) {
        add.getParent().appendChild(exportKeOjs);
        }
        exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));
        exportKeOjs.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                final Label label = Common.displayLoadBar(new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        onSearchDefault(arg0);
                        LogLoginAction.tampilDpsaceLog();
                    }
                });
                new Thread(new Runnable() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void run() {
                        try {
                            String cookie = DspaceCommon.login();
                            List<Jurusan> jurusans = initCriteria(true).list();
                            int rowIndex = 1;
                            for (Jurusan jurusan : jurusans) {
                                label.setValue("Sedang memproses data " + jurusan.toString() + " ("
                                        + Common.numberFormat.get().format((rowIndex++) * 100.0 / jurusans.size())
                                        + " %)");
                                JurusanAction.getDspace(cookie, jurusan, true);
                            }
                            label.setValue("");
                        } catch (Exception e) {
                            // FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
                            // konek/login ke Dspace) hanya dicatat ke log admin lalu progres
                            // diset "" (=SUKSES palsu) di luar try, menutupi kegagalan.
                            Common.tampilErrorJikaAdmin(e);
                            label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
                                    "ekspor data Jurusan ke Dspace", null, e,
                                    new String[] {
                                            "Periksa kembali koneksi ke server Dspace dan coba ulangi.",
                                            "Pastikan Username/Password Dspace pada Pengaturan Koneksi masih benar.",
                                            "Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
                                    .replace("\n", " "));
                        }
                    }
                }).start();
            }
        });

        MyToolbarbuttonConfig exportBerkas = new MyToolbarbuttonConfig("Ekspor Berkas", "/img/corner.gif");
        if (add != null) {
        add.getParent().appendChild(exportBerkas);
        }
        exportBerkas.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));
        exportBerkas.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                final Label label = Common.displayLoadBar(new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        onSearchDefault(arg0);
                    }
                });
                new Thread(new Runnable() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void run() {
                        try {
                            String cookie = DspaceCommon.login();
                            Session session = HibernateUtil.currentSession();
                            Criteria criteria = session.createCriteria(BerkasHasilAkreditasiPunyaNama.class)
                                    .createCriteria("berkasHasilAkreditasi").createCriteria("jurusan")
                                    .addOrder(Order.asc("nama"))
                                    .add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                            : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                                    .add(searchfakultas.getSelectedItem() == null
                                            || searchfakultas.getSelectedItem().getValue() == null
                                                    ? Restrictions.sqlRestriction("1=1")
                                                    : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
                                    .add(searchjenjang.getSelectedItem() == null
                                            || searchjenjang.getSelectedItem().getValue() == null
                                                    ? Restrictions.sqlRestriction("1=1")
                                                    : Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))
                                    .add(Common.getCurrentUser() == null
                                            || Common.getCurrentUser().ambilJurusan() == null
                                                    ? Restrictions.sqlRestriction("1=1")
                                                    : Restrictions.eq("id", Common.getCurrentUser().ambilJurusan().getId()));
                            List<BerkasHasilAkreditasiPunyaNama> list = criteria.list();
                            int rowIndex = 1;
                            for (BerkasHasilAkreditasiPunyaNama berkas : list) {
                                label.setValue("Sedang memproses data " + berkas.toString() + " ("
                                        + Common.numberFormat.get().format((rowIndex++) * 100.0 / list.size()) + " %)");
                                JurusanAction.getDspace(cookie, berkas, true);
                            }
                            label.setValue("");
                        } catch (Exception e) {
                            // FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
                            // konek/login ke Dspace) hanya dicatat ke log admin lalu progres
                            // diset "" (=SUKSES palsu) di luar try, menutupi kegagalan.
                            Common.tampilErrorJikaAdmin(e);
                            label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
                                    "ekspor berkas hasil akreditasi ke Dspace", null, e,
                                    new String[] {
                                            "Periksa kembali koneksi ke server Dspace dan coba ulangi.",
                                            "Pastikan Username/Password Dspace pada Pengaturan Koneksi masih benar.",
                                            "Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
                                    .replace("\n", " "));
                        }
                    }
                }).start();
            }
        });

        MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor Berkas", "/img/svg/trash.svg");
        if (add != null) {
        add.getParent().appendChild(batalExport);
        }
        batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));
        batalExport.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
                        MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
                        new EventListener() {
                            @Override
                            public void onEvent(Event event) throws Exception {
                                int i = Integer.parseInt(event.getData().toString());
                                if (i == MyMessageboxConfig.OK) {
                                    final Label label = Common.displayLoadBar(new EventListener() {
                                        @Override
                                        public void onEvent(Event arg0) throws Exception {
                                            onSearchDefault(arg0);
                                            LogLoginAction.tampilDpsaceLog();
                                        }
                                    });
                                    new Thread(new Runnable() {
                                        @SuppressWarnings("unchecked")
                                        @Override
                                        public void run() {
                                        	try {
                                            try {
                                                String cookie = DspaceCommon.login();
                                                Session session = HibernateUtil.currentSession();
                                                Criteria criteria = session.createCriteria(BerkasHasilAkreditasiPunyaNama.class)
                                                        .createCriteria("berkasHasilAkreditasi").createCriteria("jurusan")
                                                        .addOrder(Order.asc("nama"))
                                                        .add(Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
                                                        .add(searchfakultas.getSelectedItem() == null
                                                                || searchfakultas.getSelectedItem().getValue() == null
                                                                        ? Restrictions.sqlRestriction("1=1")
                                                                        : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
                                                        .add(searchjenjang.getSelectedItem() == null
                                                                || searchjenjang.getSelectedItem().getValue() == null
                                                                        ? Restrictions.sqlRestriction("1=1")
                                                                        : Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))
                                                        .add(Common.getCurrentUser() == null
                                                                || Common.getCurrentUser().ambilJurusan() == null
                                                                        ? Restrictions.sqlRestriction("1=1")
                                                                        : Restrictions.eq("id", Common.getCurrentUser().ambilJurusan().getId()));
                                                List<BerkasHasilAkreditasiPunyaNama> list = criteria.list();
                                                int rowIndex = 1;
                                                for (BerkasHasilAkreditasiPunyaNama berkas : list) {
                                                    label.setValue("Sedang memproses data " + berkas.toString() + " ("
                                                            + Common.numberFormat.get().format((rowIndex++) * 100.0 / list.size()) + " %)");
                                                    DspaceInformation dspaceInformation = DspaceInformation.getDspaceInformation(
                                                            BerkasHasilAkreditasiPunyaNama.class.getName(), berkas.getId());
                                                    if (dspaceInformation != null) {
                                                        int result = DspaceInformation.delete(cookie,
                                                                "items/" + dspaceInformation.getUuid(),
                                                                dspaceInformation.getPostInfo());
                                                        if (result == 200) {
                                                            session = HibernateUtil.currentNativeSession();
                                                            session.getTransaction().begin();
                                                            session.delete(dspaceInformation);
                                                            session.getTransaction().commit();
                                                            HibernateUtil.closeSession();
                                                        }
                                                    }
                                                }
                                                label.setValue("");
                                            } catch (Exception e) {
                                                // FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
                                                // konek/login ke Dspace) hanya dicatat ke log admin lalu progres
                                                // diset "" (=SUKSES palsu) di luar try, menutupi kegagalan.
                                                Common.tampilErrorJikaAdmin(e);
                                                label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
                                                        "pembatalan ekspor berkas hasil akreditasi di Dspace", null, e,
                                                        new String[] {
                                                                "Periksa kembali koneksi ke server Dspace dan coba ulangi.",
                                                                "Pastikan Username/Password Dspace pada Pengaturan Koneksi masih benar.",
                                                                "Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
                                                        .replace("\n", " "));
                                            }
                                                                                	} finally {
                                        		ais.database.hibernate.HibernateUtil.closeSession();
                                        	}
                                        }
                                    }).start();
                                }
                            }
                        });
            }
        });

        if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
                && Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
            final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
            MyToolbarbuttonConfig buttonFeeder = new MyToolbarbuttonConfig("Syn. Feeder",
                    "/img/Button-Refresh-icon.png");
            buttonFeeder.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    MyMessageboxConfig.show("Apakah yakin ingin singkronkan data ke feeder ?", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
                            new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = Integer.parseInt(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        String[] kon = EksporFromFeederAction.koneksi();
                                        final String ip = kon[0];
                                        final String port = kon[1];
                                        final String username = kon[2];
                                        final String password = kon[3];
                                        final String url = kon[4];
                                        if (!EksporFromFeederAction.exists(url)) {
                                            MyMessageboxConfig.show(
                                                    ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
                                                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                                            return;
                                        }
                                        final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
                                            @Override
                                            public void onEvent(Event arg0) throws Exception {
                                                if (arg0 != null && !arg0.getName().isEmpty()) {
                                                    EksporFromFeederAction.display();
                                                    MyMessageboxConfig.show(arg0.getName(), "Info",
                                                            MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                                                }
                                                onSearchDefault(null);
                                            }
                                        });
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    FeederConnector feederConnector = new FeederConnector(ip,
                                                            Integer.parseInt(port), null);
                                                    String token = feederConnector.getToken(username, password);
                                                    if (token == null || token.trim().isEmpty()
                                                            || token.trim().toLowerCase().startsWith("error")) {
                                                        myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
                                                        return;
                                                    }
                                                    FeederExporter feederImporter = new FeederExporter(feederConnector,
                                                            token, null, null, myLabelProsesDetail);
                                                    exportKeFeeder(perguruanTinggi, feederImporter, token, feederConnector);
                                                    // FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
                                                    myLabelProsesDetail.setValue("");
                                                } catch (Exception e) {
                                                    Common.tampilErrorJikaAdmin(e);
                                                    myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
                                                            "sinkronisasi data Jurusan ke Neo Feeder",
                                                            null, e,
                                                            new String[] {
                                                                    "Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
                                                                    "Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
                                                                    "Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
                                                            .replace("\n", " "));
                                                }
                                            }
                                        }).start();
                                    }
                                }
                            });
                }
            });
            if (add != null) {
            add.getParent().appendChild(buttonFeeder);
            }
        }
    }

    /** Membangun kriteria pencarian {@link Jurusan} berdasarkan perguruan tinggi aktif, status aktif, filter nama/kode/fakultas/jenjang, dan dibatasi ke prodi pengguna sendiri bila pengguna terikat pada satu jurusan. Diurutkan menurut kode lalu nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Jurusan.class).createAlias("fakultas", "fakultas")
                .add(selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
                        ? Restrictions.or(Restrictions.isNull("fakultas.perguruanTinggi"),
                                Restrictions.eq("fakultas.perguruanTinggi", selectedPerguruanTinggi))
                        : Restrictions.sqlRestriction("true"))
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.asc("kode"));
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.or(
                                Restrictions.ilike("kodeEpsbed", searchkode.getValue().trim(), MatchMode.ANYWHERE),
                                Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE)))
                .add(searchfakultas == null || searchfakultas.getSelectedItem() == null
                        || searchfakultas.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
                .add(searchjenjang == null || searchjenjang.getSelectedItem() == null
                        || searchjenjang.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))
                .add(Common.getCurrentUser() == null || Common.getCurrentUser().ambilJurusan() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("id", Common.getCurrentUser().ambilJurusan().getId()));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new JurusanRenderer();
    }

    // ======================== ZUL lazy-tab event handlers ========================

    /** Memuat tab "Akreditasi" secara lazy (sekali saja) dengan menyisipkan iframe {@code /pages/master/akreditasi.zul}. */
    public void onAkreditasi(Event event) {
        if (akreditasiTab.getChildren().size() == 0) {
            MyWindow window = new MyWindow("", "none", false);
            window.setHeight("100%");
            window.setWidth("100%");
            window.setParent(akreditasiTab);
            MyInclude iframe = new MyInclude("/pages/master/akreditasi.zul");
            iframe.setParent(window);
        }
    }

    /** Memuat tab "Konsentrasi" secara lazy (sekali saja) dengan menyisipkan iframe {@code /pages/master/konsentrasi.zul}. */
    public void onKonsentrasi(Event event) {
        if (konsentrasiTab.getChildren().size() == 0) {
            MyWindow window = new MyWindow("", "none", false);
            window.setHeight("100%");
            window.setWidth("100%");
            window.setParent(konsentrasiTab);
            MyInclude iframe = new MyInclude("/pages/master/konsentrasi.zul");
            iframe.setParent(window);
        }
    }

    /** Memuat tab "Capaian Pembelajaran Lulusan" secara lazy (sekali saja) dengan menyisipkan iframe {@code /pages/master/capaian_jurusan.zul}. */
    public void onCapaianPembelajaranLulusan(Event event) {
        if (capaianPembelajaranLulusanTab.getChildren().size() == 0) {
            MyWindow window = new MyWindow("", "none", false);
            window.setHeight("100%");
            window.setWidth("100%");
            window.setParent(capaianPembelajaranLulusanTab);
            MyInclude iframe = new MyInclude("/pages/master/capaian_jurusan.zul");
            iframe.setParent(window);
        }
    }

    /** Memuat tab "Jenis Capaian Pembelajaran Lulusan" secara lazy (sekali saja) dengan menyisipkan iframe {@code /pages/master/jenis_capaian_jurusan.zul}. */
    public void onJenisCapaianPembelajaranLulusan(Event event) {
        if (jenisCapaianPembelajaranLulusanTab.getChildren().size() == 0) {
            MyWindow window = new MyWindow("", "none", false);
            window.setHeight("100%");
            window.setWidth("100%");
            window.setParent(jenisCapaianPembelajaranLulusanTab);
            MyInclude iframe = new MyInclude("/pages/master/jenis_capaian_jurusan.zul");
            iframe.setParent(window);
        }
    }

    // ======================== Static DSpace methods ========================

    /**
     * Membuat/memperbarui satu "community" DSpace yang merepresentasikan {@code jurusan}
     * (nama, deskripsi, teks hak cipta). Bila konfigurasi
     * {@code dpsace_jadikan_jurusan_sebagai_root} aktif, community dibuat sebagai root; selain
     * itu dibuat sebagai anak dari community fakultas terkait (memicu pembuatan community
     * fakultas terlebih dahulu bila belum ada lewat {@code FakultasAction.getDspace}).
     *
     * @param cookie sesi login DSpace yang sudah diautentikasi
     * @param jurusan jurusan yang direpresentasikan
     * @param update  perbarui community yang sudah ada bila {@code true}, buat baru bila belum ada
     * @return informasi community DSpace tersimpan ({@link DspaceInformation})
     * @throws Exception diteruskan dari kegagalan panggilan REST API DSpace
     */
    public static DspaceInformation getDspace(String cookie, Jurusan jurusan, boolean update) throws Exception {
        JSONObject jsonPost = new JSONObject();
        jsonPost.put("name", jurusan.getNama());
        jsonPost.put("copyrightText",
                "Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
        jsonPost.put("introductoryText", jurusan.getDeskripsi());
        jsonPost.put("shortDescription",
                "Repositori milik " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama());
        jsonPost.put("sidebarText", "Berisi semua repository Repositori milik "
                + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama());
        if (Common.bolehKonfigurasi("dpsace_jadikan_jurusan_sebagai_root", Konfigurasi.TIDAK_AKTIF)) {
            return DspaceInformation.dspaceProcess(cookie, jurusan, jsonPost.toString(), update,
                    "communities", "communities");
        } else {
            return DspaceInformation.dspaceProcess(cookie, jurusan, jsonPost.toString(), update,
                    "communities",
                    "communities/" + FakultasAction.getDspace(cookie, jurusan.getFakultas(), update) + "/communities");
        }
    }

    /**
     * Membuat/memperbarui satu "collection" DSpace yang merepresentasikan satu kategori berkas
     * hasil akreditasi ({@code berkasHasilAkreditasi}), ditempatkan di bawah community
     * jurusan/fakultas/perguruan tinggi terkait (dipilih berdasarkan level mana yang terisi pada
     * entitas). Collection selalu dibuat baru ({@code update=false}).
     *
     * @param cookie                 sesi login DSpace yang sudah diautentikasi
     * @param berkasHasilAkreditasi  kategori berkas akreditasi yang direpresentasikan
     * @return informasi collection DSpace tersimpan, atau {@code null} bila tidak ada level (jurusan/fakultas/PT) yang terisi
     * @throws Exception diteruskan dari kegagalan panggilan REST API DSpace
     */
    public static DspaceInformation getDspaceBerkasHasilAkreditasi(String cookie,
            BerkasHasilAkreditasi berkasHasilAkreditasi) throws Exception {
        Jurusan jurusan = berkasHasilAkreditasi.getJurusan();
        Fakultas fakultas = berkasHasilAkreditasi.getFakultas();
        PerguruanTinggi perguruanTinggi = berkasHasilAkreditasi.getPerguruanTinggi();
        if (jurusan != null) {
            String description = "Repositori " + berkasHasilAkreditasi.getNama() + " pada "
                    + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("name", berkasHasilAkreditasi.getNama());
            jsonPost.put("copyrightText",
                    "Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
            jsonPost.put("introductoryText", description);
            jsonPost.put("shortDescription", berkasHasilAkreditasi.getNama() + " Repository");
            jsonPost.put("sidebarText", description);
            return DspaceInformation.dspaceProcess(cookie, berkasHasilAkreditasi, jsonPost.toString(), false,
                    "collections",
                    "communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");
        } else if (fakultas != null) {
            String description = "Repositori " + berkasHasilAkreditasi.getNama() + " pada "
                    + Common.getBahasaConfig("Fakultas") + " " + fakultas.getNama();
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("name", berkasHasilAkreditasi.getNama());
            jsonPost.put("copyrightText",
                    "Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
            jsonPost.put("introductoryText", description);
            jsonPost.put("shortDescription", berkasHasilAkreditasi.getNama() + " Repository");
            jsonPost.put("sidebarText", description);
            return DspaceInformation.dspaceProcess(cookie, berkasHasilAkreditasi, jsonPost.toString(), false,
                    "collections",
                    "communities/" + FakultasAction.getDspace(cookie, fakultas, false) + "/collections");
        } else if (perguruanTinggi != null) {
            String description = "Repositori " + berkasHasilAkreditasi.getNama() + " pada " + perguruanTinggi.getNama();
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("name", berkasHasilAkreditasi.getNama());
            jsonPost.put("copyrightText",
                    "Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
            jsonPost.put("introductoryText", description);
            jsonPost.put("shortDescription", berkasHasilAkreditasi.getNama() + " Repository");
            jsonPost.put("sidebarText", description);
            return DspaceInformation.dspaceProcess(cookie, berkasHasilAkreditasi, jsonPost.toString(), false,
                    "collections",
                    "communities/" + PerguruanTinggiAction.getDspace(cookie, perguruanTinggi, false) + "/collections");
        } else {
            return null;
        }
    }

    /**
     * Membuat/memperbarui satu "item" DSpace (dokumen individual) untuk
     * {@code berkasHasilAkreditasiPunyaNama}, ditempatkan di dalam collection kategori terkait
     * ({@link #getDspaceBerkasHasilAkreditasi}). Menyusun metadata Dublin Core lengkap (penulis,
     * editor, hak cipta, deskripsi, abstrak, identifier, judul, kata kunci, penerbit, tanggal
     * terbit) dan, bila item memiliki lampiran berkas ({@link LampiranLain}), mengunggah berkas
     * fisiknya ke item DSpace setelah item dibuat.
     *
     * @param cookie                             sesi login DSpace yang sudah diautentikasi
     * @param berkasHasilAkreditasiPunyaNama      dokumen berkas akreditasi yang direpresentasikan
     * @param update                              perbarui item yang sudah ada bila {@code true}
     * @return informasi item DSpace tersimpan
     * @throws Exception diteruskan dari kegagalan panggilan REST API DSpace atau unggah berkas
     */
    public static DspaceInformation getDspace(String cookie,
            BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama, boolean update) throws Exception {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonMetadata;

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.contributor.author");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getPenulis());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.contributor.editor");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getEditor());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.date.copyright");
        jsonMetadata.put("value",
                "Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.description");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getKeterangan());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.description.abstract");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getAbstrak());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.identifier");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getKode());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.title");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getNama());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.subject");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getKeyword());
        jsonArray.put(jsonMetadata);

        jsonMetadata = new JSONObject();
        jsonMetadata.put("key", "dc.publisher");
        jsonMetadata.put("value", berkasHasilAkreditasiPunyaNama.getDiterbitkanoleh());
        jsonArray.put(jsonMetadata);

        if (berkasHasilAkreditasiPunyaNama.getTanggal() != null) {
            jsonMetadata = new JSONObject();
            jsonMetadata.put("key", "dc.date.issued");
            jsonMetadata.put("value",
                    Common.databaseDateFormat.get().format(berkasHasilAkreditasiPunyaNama.getTanggal()));
            jsonArray.put(jsonMetadata);
        }

        LampiranLain lampiranLain = LampiranLain.ambil(berkasHasilAkreditasiPunyaNama.getId(),
                BerkasHasilAkreditasiPunyaNama.class.getName());
        if (lampiranLain != null) {
            String uri = lampiranLain.createLinkUri(false);
            if (uri != null && !uri.trim().isEmpty()) {
                jsonMetadata = new JSONObject();
                jsonMetadata.put("key", "dc.identifier.uri");
                jsonMetadata.put("value", uri);
                jsonArray.put(jsonMetadata);
            }
        }

        JSONObject jsonPost = new JSONObject();
        jsonPost.put("metadata", jsonArray);

        DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie,
                berkasHasilAkreditasiPunyaNama, jsonPost.toString(), jsonArray.toString(), update, "items",
                "collections/" + getDspaceBerkasHasilAkreditasi(cookie,
                        berkasHasilAkreditasiPunyaNama.getBerkasHasilAkreditasi()) + "/items",
                "items/{uuid}/metadata");

        if (lampiranLain != null) {
            DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain, lampiranLain.getNama());
        }
        return dspaceInformation;
    }

    // ======================== Static info mode (read-only view) ========================

    /** Membuka jendela modal read-only berisi informasi lengkap {@code jurusan} (mode "info", bukan form edit), dipanggil dari layar lain yang perlu menampilkan detail prodi tanpa mengizinkan perubahan. */
    public static void onInfo(Jurusan jurusan) throws Exception {
        JurusanAction jurusanAction = new JurusanAction();
        jurusanAction.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(jurusanAction.addWindow);
        jurusanAction.addWindow.setHeight("95%");
        jurusanAction.addWindow.setWidth("750px");
        jurusanAction.buildInfoMode(jurusan);
        jurusanAction.addWindow.setVisible(true);
        jurusanAction.addWindow.setClosable(true);
        jurusanAction.addWindow.onModal();
    }

    /** Membangun tampilan mode-info (read-only) untuk {@code jurusan} ke dalam {@link #addWindow}, mendelegasikan ke {@link #buildFormInternal} dengan {@code info=true}. */
    public void buildInfoMode(Jurusan jurusan) throws Exception {
        currentEntity = jurusan;
        Common.clear(addWindow);
        buildFormInternal(addWindow, jurusan, true);
    }

    // ======================== Form content ========================

    /** Membangun form tambah/edit Jurusan (mode dapat diedit), mendelegasikan ke {@link #buildFormInternal} dengan {@code info=false}. */
    @Override
    protected void buildFormContent(MyWindow window, Jurusan jurusan) throws Exception {
        buildFormInternal(window, jurusan, false);
    }

    /**
     * Implementasi bersama form tambah/edit maupun tampilan read-only Jurusan: membangun seluruh
     * field data pokok (kode, nama, fakultas, jenjang, gelar, pejabat, akreditasi umum &amp;
     * kesehatan, satuan kerja) plus tab-tab tambahan (lampiran gambar, "Tentang Prodi" via
     * {@link #initTentangProdi}, Kapasitas Prodi via {@link #initKapasitasProdi}, Fasilitas
     * Akademik via {@link #initFasilitasAkademik}). Saat {@code info=true}, kontrol input
     * dinonaktifkan/disembunyikan sesuai kebutuhan tampilan read-only.
     *
     * @param window jendela tempat form dibangun
     * @param jurusan entitas yang diedit/ditampilkan
     * @param info    bila {@code true}, bangun sebagai tampilan read-only; bila {@code false}, form dapat diedit
     * @throws Exception diteruskan dari kegagalan pembangunan komponen
     */
    @SuppressWarnings("deprecation")
    private void buildFormInternal(MyWindow window, final Jurusan jurusan, final boolean info) throws Exception {
        currentEntity = jurusan;

        if (fakultas == null) {
            fakultas = new Combobox();
            Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class,
                    Restrictions.eq("aktif", true));
        }
        if (grupJurusan == null) {
            grupJurusan = new Combobox();
            Common.insertCombo(grupJurusan, "nama", GrupJurusan.class);
        } else {
            Common.insertCombo(grupJurusan, new String[] { "nama", "kode" }, GrupJurusan.class);
        }
        Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        Center center = new Center();
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        if (info) {
            center.appendChild(initMain(jurusan, true));
        } else {
            final ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });

            {
                org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Data Program Studi", "/img/svg/book.svg");
                panel.appendChild(initMain(jurusan, false));
            }

            final org.zkoss.zul.Div panelTentang = btnTab.tambahTab(1, "Tentang Program Studi", "/img/svg/info.svg");
            btnTab.onSetiapPilih(1, new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    if (panelTentang.getChildren().isEmpty()) {
                        if (!onSave(arg0)) {
                            btnTab.pilih(0);
                            Common.clear(panelTentang);
                            return;
                        }
                        panelTentang.appendChild(initTentangProdi(JurusanAction.this.currentEntity));
                    }
                }
            });

            {
                org.zkoss.zul.Div panel = btnTab.tambahTab(2, "Fasilitas Penunjang Akademik", "/img/svg/chalkboard-teacher-light.svg");
                panel.appendChild(initFasilitasAkademik(jurusan));
            }

            final org.zkoss.zul.Div panelKapasitas = btnTab.tambahTab(3, "Kapasitas Penerimaan Mahasiswa Baru", "/img/svg/users.svg");
            btnTab.onSetiapPilih(3, new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    if (panelKapasitas.getChildren().isEmpty()) {
                        if (!onSave(arg0)) {
                            btnTab.pilih(0);
                            Common.clear(panelKapasitas);
                            return;
                        }
                        panelKapasitas.appendChild(initKapasitasProdi());
                    }
                }
            });

            btnTab.tambahTabLazy(4, "Berkas Akreditasi", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
                @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                    MyWindow w = new MyWindow("", "none", false);
                    w.setHeight("100%"); w.setWidth("100%"); w.setParent(panel);
                    new MyInclude("/pages/master/berkas_hasil_akreditasi.zul?jurusan=" + jurusan.getId()).setParent(w);
                }
            });
            btnTab.setVisibleTombol(4, jurusan.getId() != null);

            btnTab.tambahTabLazy(5, "Capaian", "/img/svg/award.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
                @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                    MyWindow w = new MyWindow("", "none", false);
                    w.setHeight("100%"); w.setWidth("100%"); w.setParent(panel);
                    new MyInclude("/pages/master/capaian_jurusan.zul?jurusan=" + jurusan.getId()).setParent(w);
                }
            });
            btnTab.setVisibleTombol(5, jurusan.getId() != null);

            {
                org.zkoss.zul.Div panel = btnTab.tambahTab(6, "Deskripsi", "/img/svg/pencil-square.svg");
                deskripsi = new MyCkEditor();
                deskripsi.setParent(panel);
                deskripsi.setValue(jurusan.getDeskripsi());
                deskripsi.setHeight("100%");
                deskripsi.setWidth("100%");
            }
        }

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);
        toolbar.setHeight("35px");

        if (info) {
            MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
            cancel.setTooltiptext("Tutup");
            cancel.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    addWindow.detach();
                }
            });
            cancel.setParent(toolbar);
        } else {
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
        }
        borderlayout.setParent(window);
    }

    @SuppressWarnings("deprecation")
    private org.zkoss.zul.Div initMain(final Jurusan jurusan, boolean info) throws Exception {
        // Gunakan Div scrollable agar content ter-scroll meski parent Tabpanel
        // tidak meneruskan height ke inner Borderlayout di ZK5.
        org.zkoss.zul.Div scrollWrap = new org.zkoss.zul.Div();
        scrollWrap.setStyle("overflow-y:auto;max-height:calc(90vh - 160px);padding:8px;background:#f0f4f8;box-sizing:border-box;");

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(scrollWrap);

        org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
        grid.setStyle("border:none;width:100%;");
        grid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(grid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(jurusan.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode " + (info ? "" : "*"), kode);

        kodeEpsbed = new Textbox(jurusan.getKodeEpsbed());
        kodeEpsbed.setWidth("100%");
        Row rowKodeEpsbed = fb.addRow("Kode PDPT", kodeEpsbed);

        feeder = new Textbox(jurusan.getFeeder());
        feeder.setWidth("100%");
        Row rowFeeder = fb.addRow("Kode FEEDER (UUID)", feeder);
        rowFeeder.setVisible(Common.getApakahAdminBolehAksesFeeder());

        kodeLain = new Textbox(jurusan.getKodeLain());
        kodeLain.setWidth("100%");
        Row rowKodeLain = fb.addRow("Kode Lain (jika diperlukan)", kodeLain);

        nama = new Textbox(jurusan.getNama() == null ? "" : jurusan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama " + (info ? "" : "*"), nama);

        namaEn = new Textbox(jurusan.getNamaEn());
        namaEn.setWidth("100%");
        fb.addRow("Name English", namaEn);

        if (jenjang == null) {
            jenjang = new Combobox();
            Common.insertCombo(jenjang, "nama", Jenjang.class,
                    Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        }

        Common.selectComboItem(true, jenjang, jurusan.getJenjang() == null ? null : jurusan.getJenjang());
        jenjang.setWidth("100%");
        jenjang.setReadonly(true);
        fb.addRow("Jenjang " + (info ? "" : "*"), jenjang);

        Tbmuser tbmuser = Common.getCurrentUser();
        Common.selectComboItem(true, fakultas,
                jurusan.getFakultas() == null ? tbmuser.ambilFakultas() : jurusan.getFakultas());
        fakultas.setWidth("100%");
        fakultas.setReadonly(true);
        fb.addRow("Fakultas" + (info ? "" : " *"), fakultas);

        Common.selectComboItem(true, grupJurusan, jurusan.getGrupJurusan());
        grupJurusan.setWidth("100%");
        grupJurusan.setReadonly(true);
        Row rowGrupJurusan = fb.addRow(Common.getBahasa("label_grupJurusan"), grupJurusan);
        rowGrupJurusan.setVisible(!grupJurusan.getChildren().isEmpty());

        jenisJurusan = new Combobox();
        MyComboitemConfig comboitemConfig = new MyComboitemConfig(Jurusan.JENIS_EKSAKTA);
        comboitemConfig.setValue(Jurusan.JENIS_EKSAKTA);
        jenisJurusan.appendChild(comboitemConfig);
        comboitemConfig = new MyComboitemConfig(Jurusan.JENIS_SOSIAL);
        comboitemConfig.setValue(Jurusan.JENIS_SOSIAL);
        jenisJurusan.appendChild(comboitemConfig);

        Common.selectComboItem(jenisJurusan, jurusan.getJenisJurusan());
        jenisJurusan.setWidth("100%");
        jenisJurusan.setReadonly(true);
        fb.addRow("Jenis " + Common.getBahasaConfig("Jurusan") + (info ? "" : " *"), jenisJurusan);

        kaprodi = new AmbilDataDosenBanbox();
        kaprodi.setAttribute("dosen", jurusan.getKaprodi());
        kaprodi.setAttribute("myValue", jurusan.getKaprodi());
        kaprodi.setValue(jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNama());
        kaprodi.setWidth("100%");
        fb.addRow(Common.getBahasaConfig("Kaprodi"), kaprodi);

        labelPejabat1 = new Textbox(jurusan.getLabelPejabat1());
        pegawai1 = new AmbilDataPegawaiBanbox(false);
        pegawai1.setAttribute("pegawai", jurusan.getPegawai1());
        pegawai1.setValue(jurusan.getPegawai1() == null ? "" : jurusan.getPegawai1().getNama());
        pegawai1.setWidth("100%");
        pegawai1.setReadonly(true);

        labelPejabat2 = new Textbox(jurusan.getLabelPejabat2());
        pegawai2 = new AmbilDataPegawaiBanbox(false);
        pegawai2.setAttribute("pegawai", jurusan.getPegawai2());
        pegawai2.setValue(jurusan.getPegawai2() == null ? "" : jurusan.getPegawai2().getNama());
        pegawai2.setWidth("100%");
        pegawai2.setReadonly(true);

        labelPejabat3 = new Textbox(jurusan.getLabelPejabat3());
        pegawai3 = new AmbilDataPegawaiBanbox(false);
        pegawai3.setAttribute("pegawai", jurusan.getPegawai3());
        pegawai3.setValue(jurusan.getPegawai3() == null ? "" : jurusan.getPegawai3().getNama());
        pegawai3.setWidth("100%");
        pegawai3.setReadonly(true);

        if (info) {
            fb.addRow(jurusan.getLabelPejabat1(),
                    new Label(jurusan.getPegawai1() == null ? "" : jurusan.getPegawai1().getNama()))
                    .setVisible(!jurusan.getLabelPejabat1().equals("Pejabat I"));
            fb.addRow(jurusan.getLabelPejabat2(),
                    new Label(jurusan.getPegawai2() == null ? "" : jurusan.getPegawai2().getNama()))
                    .setVisible(!jurusan.getLabelPejabat2().equals("Pejabat II"));
            fb.addRow(jurusan.getLabelPejabat3(),
                    new Label(jurusan.getPegawai3() == null ? "" : jurusan.getPegawai3().getNama()))
                    .setVisible(!jurusan.getLabelPejabat3().equals("Pejabat III"));
        } else {
            Hbox pejabat1Hbox = new Hbox();
            labelPejabat1.setWidth("120px");
            pejabat1Hbox.appendChild(labelPejabat1);
            pejabat1Hbox.appendChild(pegawai1);
            fb.addFullRow(pejabat1Hbox);

            Hbox pejabat2Hbox = new Hbox();
            labelPejabat2.setWidth("120px");
            pejabat2Hbox.appendChild(labelPejabat2);
            pejabat2Hbox.appendChild(pegawai2);
            fb.addFullRow(pejabat2Hbox);

            Hbox pejabat3Hbox = new Hbox();
            labelPejabat3.setWidth("120px");
            pejabat3Hbox.appendChild(labelPejabat3);
            pejabat3Hbox.appendChild(pegawai3);
            fb.addFullRow(pejabat3Hbox);
        }

        gelar = new Textbox(jurusan.getGelar());
        gelar.setWidth("100%");
        fb.addRow("Gelar", gelar);

        singkatanGelar = new Textbox(jurusan.getSingkatanGelar());
        singkatanGelar.setWidth("100%");
        fb.addRow("Singkatan Gelar", singkatanGelar);

        gelarEn = new Textbox(jurusan.getGelarEn());
        gelarEn.setWidth("100%");
        fb.addRow("Gelar English", gelarEn);

        singkatanGelarEn = new Textbox(jurusan.getSingkatanGelarEn());
        singkatanGelarEn.setWidth("100%");
        fb.addRow("Singkatan Gelar English", singkatanGelarEn);

        bahasaPengantar = new Textbox(jurusan.getBahasaPengantar());
        bahasaPengantar.setWidth("100%");
        fb.addRow("Bahasa Pengantar", bahasaPengantar);

        if (!info) {
            satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false);
            satuanKerja.setAttribute("satuanKerja", jurusan.getSatuanKerja());
            satuanKerja.setValue(jurusan.getSatuanKerja() == null ? "" : jurusan.getSatuanKerja().getNama());
            satuanKerja.setWidth("100%");
            fb.addRow("Satuan Kerja", satuanKerja);

            dosenHarusPakaiSatuanKerja = new MyCheckboxConfig("Dosen Harus Pakai Satuan Kerja");
            dosenHarusPakaiSatuanKerja.setChecked(jurusan.getDosenHarusPakaiSatuanKerja());
            fb.addFullRow(dosenHarusPakaiSatuanKerja);

            rasioDosenDanMahasiswa = new MyDoublebox(jurusan.getRasioDosenDanMahasiswa());
            fb.addRow("Rasio Dosen Dan Mahasiswa", rasioDosenDanMahasiswa);

            statusAkreditasi = new Combobox();
            for (String s : Jurusan.SEMUA_STATUS) {
                comboitemConfig = new MyComboitemConfig(s);
                comboitemConfig.setValue(s);
                statusAkreditasi.appendChild(comboitemConfig);
            }
            Common.selectComboItem(statusAkreditasi, jurusan.getStatusAkreditasi());
            statusAkreditasi.setWidth("100%");
            statusAkreditasi.setReadonly(true);
            fb.addRow("Status Akreditasi Terakhir BAN-PT *", statusAkreditasi);

            peringkatAkreditasi = new Textbox(jurusan.getPeringkatAkreditasi());
            peringkatAkreditasi.setWidth("100%");
            fb.addRow("Peringkat Akreditasi Terakhir BAN-PT", peringkatAkreditasi);

            akreditasi = new Textbox(jurusan.getAkreditasi());
            akreditasi.setWidth("100%");
            fb.addRow("Nilai Akreditasi Terakhir BAN-PT", akreditasi);

            noSkAkreditasi = new Textbox(jurusan.getNoSkAkreditasi());
            noSkAkreditasi.setWidth("100%");
            fb.addRow("No. SK Akreditasi Terakhir BAN-PT", noSkAkreditasi);

            tanggalAkreditasi = new MyDatebox(jurusan.getTanggalAkreditasi());
            tanggalAkreditasi.setWidth("100%");
            fb.addRow("Tanggal Akreditasi Terakhir BAN-PT", tanggalAkreditasi);

            statusAkreditasiKes = new Textbox(jurusan.getStatusAkreditasiKes());
            statusAkreditasiKes.setWidth("100%");
            fb.addRow("Status Akreditasi Terakhir LAM-PT-Kes-PT *", statusAkreditasiKes);

            peringkatAkreditasiKes = new Textbox(jurusan.getPeringkatAkreditasiKes());
            peringkatAkreditasiKes.setWidth("100%");
            fb.addRow("Peringkat Akreditasi Terakhir LAM-PT-Kes-PT", peringkatAkreditasiKes);

            akreditasiKes = new Textbox(jurusan.getAkreditasiKes());
            akreditasiKes.setWidth("100%");
            fb.addRow("Nilai Akreditasi Terakhir LAM-PT-Kes-PT", akreditasiKes);

            noSkAkreditasiKes = new Textbox(jurusan.getNoSkAkreditasiKes());
            noSkAkreditasiKes.setWidth("100%");
            fb.addRow("No. SK Akreditasi Terakhir LAM-PT-Kes-PT", noSkAkreditasiKes);

            tanggalAkreditasiKes = new MyDatebox(jurusan.getTanggalAkreditasiKes());
            tanggalAkreditasiKes.setWidth("100%");
            fb.addRow("Tanggal Akreditasi Terakhir LAM-PT-Kes-PT", tanggalAkreditasiKes);
        }

        wa = new Textbox(jurusan.getWa());
        wa.setWidth("100%");
        fb.addRow("WA Operator", wa);

        alamat = new MyTextbox(jurusan.getAlamat());
        alamat.setWidth("100%");
        alamat.setRows(2);
        fb.addRow("Alamat I", alamat);

        alamat2 = new MyTextbox(jurusan.getAlamat2());
        alamat2.setWidth("100%");
        alamat2.setRows(2);
        fb.addRow("Alamat II", alamat2);

        if (info) {
            rowKodeEpsbed.setVisible(false);
            rowFeeder.setVisible(false);
            rowKodeLain.setVisible(false);
            rowGrupJurusan.setVisible(false);
            Common.freezeGanti(rows, true);
            fb.addFullRow(new Html(jurusan.getDeskripsi()));
        } else {
            kop = null;
            Hbox kopHbox = new Hbox();
            LampiranLain.createDownloadUploadFileLain(kopHbox, jurusan.getId(), LampiranLain.KOP_JURUSAN, "KOP",
                    false, new EventListener() {
                        @Override
                        public void onEvent(Event arg0) throws Exception {
                            kop = (LampiranLain) arg0.getData();
                        }
                    });
            fb.addRow("KOP (JPG)", kopHbox);

            kopStempel = null;
            Hbox kopStempelHbox = new Hbox();
            LampiranLain.createDownloadUploadFileLain(kopStempelHbox, jurusan.getId(), LampiranLain.STEMPEL_JURUSAN,
                    "Stempel", false, new EventListener() {
                        @Override
                        public void onEvent(Event arg0) throws Exception {
                            kopStempel = (LampiranLain) arg0.getData();
                        }
                    });
            fb.addRow("Stempel (JPG)", kopStempelHbox);

            icon = null;
            Hbox iconHbox = new Hbox();
            LampiranLain.createDownloadUploadFileLain(iconHbox, jurusan.getId(), LampiranLain.ICON_GELOMBANG_PMB,
                    "Icon", false, new EventListener() {
                        @Override
                        public void onEvent(Event arg0) throws Exception {
                            icon = (LampiranLain) arg0.getData();
                        }
                    });
            fb.addRow("Icon Gelombang (PNG)", iconHbox);

            infoProdi = null;
            Hbox infoProdiHbox = new Hbox();
            LampiranLain.createDownloadUploadFileLain(infoProdiHbox, jurusan.getId(), LampiranLain.INFO_JURUSAN,
                    "Informasi", false, new EventListener() {
                        @Override
                        public void onEvent(Event arg0) throws Exception {
                            infoProdi = (LampiranLain) arg0.getData();
                        }
                    });
            fb.addRow("Brosur / Info Prodi (JPG)", infoProdiHbox);
        }

        return scrollWrap;
    }

    /** Menampilkan/membangun tab "Tentang Prodi" ({@link JenjangProgramStudi}): mengambil baris terbaru milik {@code jurusan}, atau membuatnya otomatis (disalin dari jenjang jurusan) bila belum ada, lalu mendelegasikan render form ke {@link JenjangProgramStudiAction#init}. Mengembalikan layout kosong bila {@code jurusan} belum tersimpan. */
    private Borderlayout initTentangProdi(Jurusan jurusan) throws Exception {
        if (jurusan == null || jurusan.getId() == null) {
            return new ais.ui.util.MyBorderlayout();
        }
        JenjangProgramStudi jenjangProgramStudi = jurusan.getJenjangProgramStudi();
        if (jenjangProgramStudi == null) {
            Session session = HibernateUtil.currentSession();
            jenjangProgramStudi = (JenjangProgramStudi) session.createCriteria(JenjangProgramStudi.class)
                    .setMaxResults(1).addOrder(Order.desc("id")).add(Restrictions.eq("jurusan", jurusan))
                    .uniqueResult();
            if (jenjangProgramStudi == null) {
                jenjangProgramStudi = new JenjangProgramStudi();
                jenjangProgramStudi.setJurusan(jurusan);
                jenjangProgramStudi.setJenjang(jurusan.getJenjang());
                session.save(jenjangProgramStudi);
                session.flush();
                jurusan.setJenjangProgramStudi(jenjangProgramStudi);
                Common.refreshUpdate(session, jurusan);
                session.flush();
            }
        }
        return jenjangProgramStudiAction.init(jenjangProgramStudi, false);
    }

    /** Menampilkan tab "Kapasitas Prodi" lewat {@link KapasitasProdiHelper#display}. Mengembalikan layout kosong bila {@link #currentEntity} belum tersimpan. */
    private Borderlayout initKapasitasProdi() throws Exception {
        if (currentEntity == null || currentEntity.getId() == null) {
            return new ais.ui.util.MyBorderlayout();
        }
        return kapasitasProdiHelper.display(currentEntity);
    }

    /** Membangun form tab "Fasilitas Penunjang Akademik" ({@link FasilitasAkademikJurusan} — luas &amp; jumlah ruang kuliah/lab, luas ruang dosen tetap/administrasi, koleksi buku), memuat baris tersimpan milik {@code jurusan} atau membuat entitas baru (default nol) bila belum ada. */
    private Borderlayout initFasilitasAkademik(final Jurusan jurusan) {
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        org.zkoss.zul.Html headerHtml = new org.zkoss.zul.Html();
        headerHtml.setContent("<div style='" + FormBuilder.STYLE_HEADER + "'>&#9998;&nbsp; Fasilitas Penunjang Akademik</div>");
        headerHtml.setParent(cardWrap);

        org.zkoss.zul.Grid fasGrid = new org.zkoss.zul.Grid();
        fasGrid.setStyle("border:none;width:100%;");
        fasGrid.setParent(cardWrap);

        Session session = HibernateUtil.currentSession();
        FasilitasAkademikJurusan faj = jurusan == null || jurusan.getId() == null ? null
                : (FasilitasAkademikJurusan) session.createCriteria(FasilitasAkademikJurusan.class)
                        .add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1).uniqueResult();
        if (faj == null) {
            faj = new FasilitasAkademikJurusan(jurusan);
        }

        Rows rows = new Rows();
        rows.setParent(fasGrid);

        FormBuilder fb = new FormBuilder(rows);

        luasRuangKuliah = new Decimalbox(new BigDecimal(
                faj.getLuasRuangKuliah() == null ? 0.0 : faj.getLuasRuangKuliah()));
        fb.addRow("Luas ruang kuliah", luasRuangKuliah);

        luasKebunLahanPercobaanProdi = new Decimalbox(new BigDecimal(
                faj.getLuasKebunLahanPercobaanProdi() == null ? 0.0 : faj.getLuasKebunLahanPercobaanProdi()));
        fb.addRow("Luas kebun lahan percobaan prodi", luasKebunLahanPercobaanProdi);

        jumlahRuangKuliah = new Decimalbox(new BigDecimal(
                faj.getJumlahRuangKuliah() == null ? 0 : faj.getJumlahRuangKuliah().intValue()));
        fb.addRow("Jumlah ruang kuliah", jumlahRuangKuliah);

        luasRuangLab = new Decimalbox(new BigDecimal(
                faj.getLuasRuangLaboratorium() == null ? 0.0 : faj.getLuasRuangLaboratorium()));
        fb.addRow("Luas ruang laboratorium", luasRuangLab);

        jumlahRuangLab = new Decimalbox(new BigDecimal(
                faj.getJumlahRuangLaboratorium() == null ? 0 : faj.getJumlahRuangLaboratorium().intValue()));
        fb.addRow("Jumlah ruang laboratorium", jumlahRuangLab);

        luasTotalRuangDosenTetap = new Decimalbox(new BigDecimal(
                faj.getLuasTotalRuangDosenTetap() == null ? 0.0 : faj.getLuasTotalRuangDosenTetap()));
        fb.addRow("Luas total ruang dosen tetap", luasTotalRuangDosenTetap);

        luasTotalRuangAdministrasi = new Decimalbox(new BigDecimal(
                faj.getLuasTotalRuangAdministrasi() == null ? 0.0 : faj.getLuasTotalRuangAdministrasi()));
        fb.addRow("Luas total ruang administrasi", luasTotalRuangAdministrasi);

        jumlahJudulBuku = new Decimalbox(new BigDecimal(
                faj.getJumlahJudulBuku() == null ? 0 : faj.getJumlahJudulBuku().intValue()));
        fb.addRow("Jumlah judul buku", jumlahJudulBuku);

        jumlahEksemplarBuku = new Decimalbox(new BigDecimal(
                faj.getJumlahEksemplarBuku() == null ? 0 : faj.getJumlahEksemplarBuku().intValue()));
        fb.addRow("Jumlah eksemplar buku", jumlahEksemplarBuku);

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        return borderlayout;
    }

    // ======================== Feeder export ========================

    /**
     * Menyinkronkan seluruh program studi milik {@code perguruanTinggi} dari Neo Feeder PDDIKTI
     * ({@code GetAllProdi}): untuk setiap prodi Feeder yang cocok id perguruan tingginya,
     * mencocokkan ke {@link Jurusan} lokal lewat kode EPSBED+nama persis (fallback ke kode
     * EPSBED saja bila tidak ditemukan), menyimpan kode Feeder ({@code id_sms}) ke jurusan yang
     * cocok, lalu mengimpor/memperbarui data lewat {@link FeederJSONImport#jurusan}. Kegagalan
     * ditampilkan sebagai galat, tidak ditelan diam-diam.
     *
     * @param perguruanTinggi  perguruan tinggi konteks sinkronisasi
     * @param feederImporter   helper importer Feeder (diteruskan untuk konsistensi API, tidak dipakai langsung di method ini)
     * @param token            token autentikasi sesi Neo Feeder
     * @param feederConnector  koneksi ke server Neo Feeder
     */
    private void exportKeFeeder(PerguruanTinggi perguruanTinggi, FeederExporter feederImporter, String token,
            FeederConnector feederConnector) {
        try {
            String filter = "id_perguruan_tinggi='" + perguruanTinggi.getFeeder() + "'";
            JSONArray dataProdi = feederConnector.getData("GetAllProdi", token, filter, "", "5000", "0");
            for (int index = 0; index < dataProdi.length(); index++) {
                JSONObject jsonObject = dataProdi.getJSONObject(index);
                String id_perguruan_tinggi = jsonObject.getString("id_perguruan_tinggi");
                if (id_perguruan_tinggi.equalsIgnoreCase(perguruanTinggi.getFeeder())) {
                    String id_sms = jsonObject.getString("id_prodi");
                    Session session = HibernateUtil.currentNativeSession();
                    String kode_perguruan_tinggi = jsonObject.getString("kode_program_studi");
                    String nama_program_studi = jsonObject.getString("nama_program_studi");
                    Jurusan existing = (Jurusan) session.createCriteria(Jurusan.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("kodeEpsbed", kode_perguruan_tinggi))
                            .add(Restrictions.ilike("nama", nama_program_studi, MatchMode.EXACT))
                            .setMaxResults(1).uniqueResult();
                    if (existing == null) {
                        existing = (Jurusan) session.createCriteria(Jurusan.class)
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("kodeEpsbed", kode_perguruan_tinggi))
                                .setMaxResults(1).uniqueResult();
                    }
                    if (existing != null) {
                        existing.setFeeder(id_sms);
                        session.getTransaction().begin();
                        session.saveOrUpdate(existing);
                        session.getTransaction().commit();
                    }
                    HibernateUtil.closeSession();
                    FeederJSONImport.jurusan(jsonObject, true);
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ======================== Save logic ========================

    /**
     * Memvalidasi (kode, nama, fakultas, jenjang, jenis jurusan, dan status akreditasi wajib
     * diisi; berkas kop bila diunggah harus JPG) lalu menyimpan/memperbarui entitas
     * {@link Jurusan} dengan seluruh field form. Setelah entitas utama tersimpan, juga
     * menyinkronkan sub-entitas terkait: {@link JenjangProgramStudi} (lewat
     * {@code jenjangProgramStudiAction.onSave}), baris {@link Staff} kaprodi (jabatan
     * {@code ConstantValues.KAPRODI}, dibuat/diperbarui dari data kaprodi jurusan), dan
     * {@link FasilitasAkademikJurusan} (dari field-field fasilitas), serta mengaitkan lampiran
     * gambar (kop, kop+stempel, ikon, info prodi) ke id jurusan lewat {@link #saveLampiran}.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     */
    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().equals("")) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
            		"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Kode.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            kode.focus();
            return false;
        }
        if (nama.getValue().trim().equals("")) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
            		"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            nama.focus();
            return false;
        }
        if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Fakultas",
            		"Kolom Fakultas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Fakultas.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            fakultas.focus();
            return false;
        }
        if (jenjang.getSelectedItem() == null) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Jenjang",
            		"Kolom Jenjang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Jenjang.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            jenjang.focus();
            return false;
        }
        if (jenisJurusan.getSelectedItem() == null) {
            MyMessageboxConfig.show("Jenis " + Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            jenisJurusan.focus();
            return false;
        }
        if (statusAkreditasi.getSelectedItem() == null) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Status Akreditasi",
            		"Kolom Status Akreditasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Status Akreditasi.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            statusAkreditasi.focus();
            return false;
        }
        if (kop != null && !kop.getNama().toLowerCase().endsWith("jpg")) {
            MyMessageboxConfig.show("Kop harus berupa file JPG", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session mySession1 = HibernateUtil.currentNativeSession();
        Jurusan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Jurusan) mySession1.load(Jurusan.class, entity.getId());
            currentEntity = entity;
        }

        entity.setGrupJurusan((GrupJurusan) (grupJurusan.getSelectedItem() == null
                ? null : grupJurusan.getSelectedItem().getValue()));
        entity.setNamaEn(namaEn.getValue());
        entity.setStatusAkreditasi((String) statusAkreditasi.getSelectedItem().getValue());
        entity.setJenisJurusan((String) jenisJurusan.getSelectedItem().getValue());
        entity.setAlamat(alamat.getValue());
        entity.setAlamat2(alamat2.getValue());
        entity.setFeeder(feeder.getValue().trim());
        entity.setGelar(gelar.getValue());
        entity.setSingkatanGelar(singkatanGelar.getValue());
        entity.setAkreditasi(akreditasi.getValue());
        entity.setNoSkAkreditasi(noSkAkreditasi.getValue());
        entity.setTanggalAkreditasi(tanggalAkreditasi.getValue());
        entity.setPeringkatAkreditasi(peringkatAkreditasi.getValue());
        entity.setGelarEn(gelarEn.getValue());
        entity.setSingkatanGelarEn(singkatanGelarEn.getValue());
        entity.setKode(kode.getValue());
        entity.setKodeEpsbed(kodeEpsbed.getValue());
        entity.setNama(nama.getValue());
        entity.setFakultas((Fakultas) fakultas.getSelectedItem().getValue());
        entity.setRasioDosenDanMahasiswa(rasioDosenDanMahasiswa.getValue());
        entity.setJenjang((Jenjang) jenjang.getSelectedItem().getValue());
        entity.setDeskripsi(deskripsi.getValue());
        entity.setBahasaPengantar(bahasaPengantar.getValue().trim());
        entity.setKodeLain(kodeLain.getValue().trim());
        entity.setPegawai1((Pegawai) pegawai1.getAttribute("pegawai"));
        entity.setPegawai2((Pegawai) pegawai2.getAttribute("pegawai"));
        entity.setPegawai3((Pegawai) pegawai3.getAttribute("pegawai"));
        entity.setLabelPejabat1(labelPejabat1.getValue());
        entity.setLabelPejabat2(labelPejabat2.getValue());
        entity.setLabelPejabat3(labelPejabat3.getValue());
        entity.setWa(wa.getValue());
        entity.setStatusAkreditasiKes(statusAkreditasiKes.getValue());
        entity.setAkreditasiKes(akreditasiKes.getValue());
        entity.setPeringkatAkreditasiKes(peringkatAkreditasiKes.getValue());
        entity.setNoSkAkreditasiKes(noSkAkreditasiKes.getValue());
        entity.setTanggalAkreditasiKes(tanggalAkreditasiKes.getValue());
        entity.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
        entity.setDosenHarusPakaiSatuanKerja(dosenHarusPakaiSatuanKerja.isChecked());
        entity.setKaprodi((Dosen) kaprodi.getAttribute("dosen"));

        mySession1.getTransaction().begin();
        if (entity.getId() != null) {
            mySession1.update(entity);
        } else {
            mySession1.save(entity);
        }
        mySession1.getTransaction().commit();
        HibernateUtil.closeSession();

        jenjangProgramStudiAction.onSave(event, entity);

        Jabatan jabatan = ConstantValues.KAPRODI;
        Session session = HibernateUtil.currentSession();
        Staff staff = (Staff) session.createCriteria(Staff.class)
                .add(Restrictions.eq("jabatan", jabatan))
                .add(Restrictions.eq("jurusan", entity))
                .setMaxResults(1).uniqueResult();
        if (staff == null) staff = new Staff();
        staff.setJurusan(entity);
        staff.setJabatan(jabatan);
        staff.setNama(entity.getKaprodi() == null ? "" : entity.getKaprodi().getNama());
        staff.setNip(entity.getKaprodi() == null ? "" : entity.getKaprodi().getCode());
        staff.setStaff(jabatan == null ? "Kaprodi" : jabatan.getNama());
        Common.refreshSaveOrUpdate(session, staff);

        FasilitasAkademikJurusan faj = (FasilitasAkademikJurusan) session
                .createCriteria(FasilitasAkademikJurusan.class)
                .add(Restrictions.eq("jurusan", entity)).setMaxResults(1).uniqueResult();
        if (faj == null) faj = new FasilitasAkademikJurusan();
        faj.setNama(entity.getNama() + "__fasilitas_akademik");
        faj.setJurusan(entity);
        faj.setLuasRuangKuliah(luasRuangKuliah.getValue().doubleValue());
        faj.setLuasKebunLahanPercobaanProdi(luasKebunLahanPercobaanProdi.getValue().doubleValue());
        faj.setJumlahRuangKuliah(jumlahRuangKuliah.getValue().intValue());
        faj.setLuasRuangLaboratorium(luasRuangLab.getValue().doubleValue());
        faj.setJumlahRuangLaboratorium(jumlahRuangLab.getValue().intValue());
        faj.setLuasTotalRuangDosenTetap(luasTotalRuangDosenTetap.getValue().doubleValue());
        faj.setLuasTotalRuangAdministrasi(luasTotalRuangAdministrasi.getValue().doubleValue());
        faj.setJumlahJudulBuku(jumlahJudulBuku.getValue().intValue());
        faj.setJumlahEksemplarBuku(jumlahEksemplarBuku.getValue().intValue());
        Common.refreshSaveOrUpdate(session, faj);

        saveLampiran(kop, entity.getId());
        saveLampiran(kopStempel, entity.getId());
        saveLampiran(icon, entity.getId());
        saveLampiran(infoProdi, entity.getId());

        return true;
    }

    /** Mengaitkan satu lampiran ({@code lampiran}) yang sudah diunggah ke id entitas {@code ref}, dalam sesi Hibernate streaming terpisah; tidak melakukan apa pun bila {@code lampiran} {@code null}/belum tersimpan. */
    private void saveLampiran(LampiranLain lampiran, Long ref) {
        if (lampiran == null || lampiran.getId() == null) return;
        try {
            Session session = StreamingHibernateUtil.getInstance().currentSession();
            session.refresh(lampiran);
            lampiran.setRef(ref);
            session.getTransaction().begin();
            session.update(lampiran);
            session.getTransaction().commit();
            StreamingHibernateUtil.getInstance().closeSession();
        } catch (Exception e) {
            StreamingHibernateUtil.getInstance().rollbackTransaction();
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ======================== Renderer ========================

    /** Renderer baris daftar Jurusan: menampilkan kode, label revisi+nama, fakultas, jenjang, kaprodi, dan status akreditasi beserta tombol aksi baris (edit/hapus/info sesuai hak akses). */
    class JurusanRenderer extends MyRowRenderer {

        @Override
        @SuppressWarnings("deprecation")
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Jurusan jurusan = (Jurusan) arg1;

            MyDetail detail = new MyDetail();
            detail.setParent(arg0);

            Vbox aa;
            (aa = RevisiHelper.createNewRevisi(Jurusan.class, jurusan,
                    jurusan.getJenjang() == null ? "" : jurusan.getJenjang().getNama())).setParent(arg0);
            aa.appendChild(new Label(jurusan.getKodeLain()));

            Vbox myvbox = new Vbox();
            myvbox.setParent(detail);

            Hbox hbox = new Hbox();
            hbox.setParent(myvbox);
            LampiranLain.createDownloadUploadFileLain(hbox, jurusan.getId(), LampiranLain.KOP_JURUSAN, "KOP",
                    true, null, null, false, false, false, false);
            detail.setOpen(true);

            new Label(jurusan.getKode()).setParent(arg0);
            new MyHtml("<font>" + jurusan.getKodeEpsbed() + "<br>" + jurusan.getFeeder() + "</font>").setParent(arg0);
            new Label(jurusan.getNama()
                    + (jurusan.getNamaEn() == null || jurusan.getNamaEn().trim().isEmpty()
                            ? "" : " / " + jurusan.getNamaEn())).setParent(arg0);
            new Label(jurusan.getFakultas().getNama()).setParent(arg0);
            new Label(jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNama()).setParent(arg0);

            myvbox = new Vbox();
            myvbox.setParent(arg0);
            new Label(jurusan.getPegawai1() == null ? ""
                    : jurusan.getLabelPejabat1() + " : " + jurusan.getPegawai1().getNama()).setParent(myvbox);
            new Label(jurusan.getPegawai2() == null ? ""
                    : jurusan.getLabelPejabat2() + " : " + jurusan.getPegawai2().getNama()).setParent(myvbox);
            new Label(jurusan.getPegawai3() == null ? ""
                    : jurusan.getLabelPejabat3() + " : " + jurusan.getPegawai3().getNama()).setParent(myvbox);

            new Label(jurusan.getGelar()).setParent(arg0);
            new Label(jurusan.getSingkatanGelar()).setParent(arg0);
            new Label(jurusan.getAkreditasi()).setParent(arg0);
            new Label(jurusan.getNoSkAkreditasi()).setParent(arg0);
            new Label(jurusan.getTanggalAkreditasi() == null ? ""
                    : Common.dateFormat2.get().format(jurusan.getTanggalAkreditasi())).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(jurusan.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jurusan.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(jurusan);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, jurusan, JurusanAction.this).setParent(arg0);
        }
    }
}
