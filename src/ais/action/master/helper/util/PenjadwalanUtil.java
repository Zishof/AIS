package ais.action.master.helper.util;


import ais.common.CommonSearchFilterHelper;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataJamPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.MatakuliahKurikulumDetailHelper;
import ais.action.master.helper.PenjadwalanHelper;
import ais.action.master.helper.generic.AmbilDataKelasBanyak;
import ais.common.Common;
import ais.common.CommonPenjadwalan;
import ais.common.ConstantValues;
import ais.common.OnSearchDefaultListener;
import ais.database.dao.DaoFactory;
import ais.database.dao.PerkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JamPerkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Mesin utama pembangun formulir "Tambah/Ubah Jadwal Perkuliahan" berbasis ZK yang dipakai di
 * banyak layar penjadwalan pada paket {@code ais.action.master} (mis. RencanaTahunAkademikAction
 * dan sejenisnya). Kelas ini BUKAN composer ZUL biasa — ia dipakai sebagai objek pembantu yang
 * membangun sendiri {@link org.zkoss.zul.Window} popup lengkap dengan tab "Data Jadwal" dan tab
 * "Rencana Pembelajaran/Silabus", lalu mengelola seluruh siklus hidup form: pengisian kombo
 * (Fakultas, Jurusan, Program, Kurikulum, Mata Kuliah, Kelas, Dosen 1-10, Ruang, Jam/Masa
 * Perkuliahan, Hari, Waktu), validasi, dan penyimpanan entitas {@link Perkuliahan}.
 *
 * <h2>Alur pemakaian</h2>
 * <ol>
 * <li>Pemanggil membuat instance dengan {@link #PenjadwalanUtil(OnSearchDefaultListener)}.</li>
 * <li>Salah satu dari {@link #init(Perkuliahan, Integer, Integer, Boolean)},
 * {@link #init(Perkuliahan, Integer, Integer, Boolean, Boolean, Boolean)}, atau
 * {@link #initJadwalKurikulum(Perkuliahan, Integer, Integer, Boolean)} dipanggil untuk membangun
 * jendela form sesuai konteks (jadwal reguler, pra-perkuliahan, perkuliahan umum, atau jadwal yang
 * mengikuti kurikulum).</li>
 * <li>Pengguna mengisi form; tombol simpan pada layar pemanggil memicu {@link #onSave(Event)} yang
 * melakukan validasi lengkap (kelengkapan field wajib, status aktif penjadwalan pada periode
 * terkait via {@code CommonPenjadwalan#apakahPenjadwalanTidakAktif}, bentrok dosen via
 * {@code Perkuliahan#checkDosen}) sebelum entitas {@link Perkuliahan} disimpan.</li>
 * </ol>
 *
 * <p>
 * Kelas juga menyediakan utilitas statis independen dari siklus form di atas untuk mendeteksi
 * jadwal yang saling tumpang tindih (bentrok): {@link #lihatJadwalBentrok()} (menampilkan jendela
 * pemilihan Tahun Akademik/Semester lalu menyajikan hasil), serta tiga varian pemeriksa murni
 * {@link #checkBentrokBerdasarRuangan(List)}, {@link #checkBentrokBerdasarKelas(List)}, dan
 * {@link #checkBentrokBerdasarDosen(List)} yang membandingkan setiap pasangan {@link Perkuliahan}
 * dalam daftar berdasarkan kesamaan hari dan tumpang-tindih rentang waktu.
 * </p>
 *
 * <p>
 * Banyak bidang publik (mis. {@code dosen1}..{@code dosen10}, {@code minggu1}..{@code minggu5},
 * berbagai {@link ais.ui.util.MyCheckboxConfig}) merupakan komponen ZK yang dibangun di dalam
 * {@code init}/{@code initJadwalKurikulum} dan dibaca langsung oleh pemanggil atau oleh
 * {@link #onSave(Event)}; kelas ini tidak thread-safe dan dimaksudkan sebagai objek sekali pakai
 * per sesi ZK (satu instance per popup form yang sedang dibuka).
 * </p>
 */
public class PenjadwalanUtil {

	public MyTimebox waktuMulai;
	public MyTimebox waktuSelesai;

	public Combobox fakultas;
	public Combobox jurusan;
	public Combobox matakuliah;
	public AmbilDataDosenBanbox dosen1;
	public AmbilDataDosenBanbox dosen2;
	public AmbilDataDosenBanbox dosen3;
	public AmbilDataDosenBanbox dosen4;
	public AmbilDataDosenBanbox dosen5;
	public AmbilDataDosenBanbox dosen6;
	public AmbilDataDosenBanbox dosen7;
	public AmbilDataDosenBanbox dosen8;
	public AmbilDataDosenBanbox dosen9;
	public AmbilDataDosenBanbox dosen10;

	public AmbilDataJamPerkuliahanBanbox jamPerkuliahan;
	public AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	public Combobox semester;
	public Combobox jumlahDosen;
	public MyCheckboxConfig merupakan_paralel;
	private Boolean merupakanRemedial;
	public Combobox perkuliahan_paralel;
	public Decimalbox kapasitasKelas;

	public MyCheckboxConfig merupakan_tanpa_jadwal_perkuliahan;
	public MyCheckboxConfig merupakan_tanpa_dosen;
	public MyCheckboxConfig merupakan_tanpa_ruangan;

	public Combobox waktu;
	public AmbilDataKelasBanbox kelas;

	public Combobox hari;
	public Combobox tahunAjaran;
	public Combobox kurikulum;

	public Combobox program;
	public Combobox ganjilGenap;

	public MyCheckboxConfig minggu1;
	public MyCheckboxConfig minggu2;
	public MyCheckboxConfig minggu3;
	public MyCheckboxConfig minggu4;
	public MyCheckboxConfig minggu5;

	public Textbox keterangan;
	public Textbox keteranganJadwal;

	public AmbilDataRuangBanbox ruang;
	public MyCheckboxConfig abaikanWaktuBentrokDenganJadwalLain;
	public MyCheckboxConfig tampilkanSaatPengambilanKrs;
	public MyCheckboxConfig dosenBisaMerubahTanggalPerkuliahan;
	public MyCheckboxConfig kehadiranDosenHarusDiinputSesuaiJadwal;
	public MyCheckboxConfig kehadiranDosenHarusDiinputDiIpYangDitentukan;

	public MyCheckboxConfig kehadiranMahasiswaHarusDiinputSesuaiJadwal;
	public MyCheckboxConfig kehadiranMahasiswaHarusDiinputDiIpYangDitentukan;
	public MyCheckboxConfig adminBolehMenginputKehadiranDiluarJadwalDanIp;
	public MyCheckboxConfig jumlahRencanaPertemuanMengikutiKurikulum;
	public MyCheckboxConfig waktuPerkuliahanOnlineBebas;
	public MyIntbox jumlahMaksimalPertemuan;
	// private East east;
	public Perkuliahan perkuliahan;

	protected Konfigurasi tampilkanMingguPerkuliahan;

	public MatakuliahKurikulumDetailHelper matakuliahKurikulumDetailHelper = null;
	public PenjadwalanHelper penjadwalanHelper = new PenjadwalanHelper();
	private OnSearchDefaultListener onSearchDefaultListener;

	/**
	 * Membangun instance helper dan menyiapkan kombo-kombo yang tidak bergantung pada data
	 * {@link Perkuliahan} tertentu (Fakultas/Jurusan, daftar pilihan Semester sampai
	 * {@code max_semester_pilihan}, Jumlah Dosen 1-10, kombo Kelas, Hari, dan Waktu PAGI/SIANG/
	 * SORE/MALAM). Kombo yang bergantung pada data jadwal yang sedang diedit baru dibangun di
	 * {@link #init} / {@link #initJadwalKurikulum}.
	 *
	 * @param onSearchDefaultListener listener pencarian default yang diteruskan ke komponen banbox
	 *                                terkait (dosen, ruang, kelas, dsb.)
	 */
	public PenjadwalanUtil(OnSearchDefaultListener onSearchDefaultListener) {
		this.onSearchDefaultListener = onSearchDefaultListener;
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		semester = new Combobox();

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PenjadwalanUtil.java:195");

		}

		tampilkanMingguPerkuliahan = Common.getKonfigurasi("tampilkan_minggu_perkuliahan", Konfigurasi.AKTIF);

		MyComboitemConfig comboitem;
		for (int i = 1; i < maxSemesterPilihan; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		jumlahDosen = new Combobox();
		for (int i = 1; i <= 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			jumlahDosen.appendChild(comboitem);
		}

		kelas = new AmbilDataKelasBanbox();

		hari = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		waktu = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("PAGI");
		comboitem.setValue("PAGI");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SIANG");
		comboitem.setValue("SIANG");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SORE");
		comboitem.setValue("SORE");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("MALAM");
		comboitem.setValue("MALAM");
		waktu.appendChild(comboitem);

		program = Common.initPrograms(null);
	}

	private Window addWindow;
	public Combobox tahap;
	private Row rowdosen1;
	private Row rowdosen2;
	private Row rowdosen3;
	private Row rowdosen4;
	private Row rowdosen5;
	private Row rowdosen6;
	private Row rowdosen7;
	private Row rowdosen9;
	private Row rowdosen10;
	private Integer semesterPendek;
	private Row rowdosen8;
	private MyDatebox perkuliahanDimulai;
	private MyDatebox perkuliahanSampai;
	private MyTabConfig tabBiodata;
	private Tabpanel tabpanelPindahan;

	final EventListener pilihTabRencanaPembelajaran = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			matakuliahKurikulumDetailHelper = null;
			Common.clear(tabpanelPindahan);

			if (perkuliahan != null && perkuliahan.getId() != null
					&& ((Number) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue() > 0) {
				addWindow.setWidth("99%");

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Borderlayout borderlayoutRencanaPembelajaran = new ais.ui.util.MyBorderlayout();
						borderlayoutRencanaPembelajaran.setParent(tabpanelPindahan);

						final Center centerRencanaPembelajaran = new Center();
						centerRencanaPembelajaran.setParent(borderlayoutRencanaPembelajaran);
						ais.ui.util.ZkCompat.setFlex(centerRencanaPembelajaran, true);
						penjadwalanHelper.display(perkuliahan, centerRencanaPembelajaran);
					}
				});

			} else {

				final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (matakuliah == null
						|| matakuliah.getSelectedItem() == null ? null
								: matakuliah.getSelectedItem().getAttribute("kurikulumPunyaMatakuliah"));
				if (kurikulumPunyaMatakuliah != null) {

					addWindow.setWidth("99%");

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Borderlayout borderlayoutRencanaPembelajaran = new ais.ui.util.MyBorderlayout();
							borderlayoutRencanaPembelajaran.setParent(tabpanelPindahan);

							final Center centerRencanaPembelajaran = new Center();
							centerRencanaPembelajaran.setParent(borderlayoutRencanaPembelajaran);
							ais.ui.util.ZkCompat.setFlex(centerRencanaPembelajaran, true);

							matakuliahKurikulumDetailHelper = new MatakuliahKurikulumDetailHelper();
							matakuliahKurikulumDetailHelper.display(kurikulumPunyaMatakuliah, perkuliahan,
									centerRencanaPembelajaran);

							// hariEvent.onEvent(null);
						}
					});

				} else {
					if (arg0 != null && arg0.getTarget() != null && arg0.getTarget() == tabBiodata) {
						MyMessageboxConfig.show(
								"Untuk menampilkan Rencana Pembelajaran atau Silabus, mohon Bapak/Ibu terlebih dahulu memilih salah satu mata kuliah yang tersedia.",
								"informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						tabData.setSelected(true);
					}

				}
			}
		}
	};
	private MyTabConfig tabData;
	private Textbox feeder;
	private Boolean merupakanPraPerkuliahan;
	private Row rowKeteranganJadwal;
	private Row rowJamPerkuliahan;
	private Row rowWaktu;
	private Row rowHari;
	private MyCheckboxConfig merupakanTeamTeaching;
	private Row rowMerupakanTeamTeaching;
	private MyCheckboxConfig terdapatKegiatanPraktek;
	private Boolean merupakanPerkuliahanUmum;
	private MyCheckboxConfig ambilMkDiluarSemesterKurikulum;
	private MyCheckboxConfig kelasref;
	private Row rowJumlahDosen;
	private MyCheckboxConfig dosenBolehVerifikasiNilaiSendiri;
	private MyCheckboxConfig dosenBolehAbsenMenggunakanFoto;
	private MyCheckboxConfig mahasiswaBolehAbsenMenggunakanFoto;
	private MyCheckboxConfig sembunyikanNilaiJikaBelumDiverifikasi;
	private MyDoublebox persenKehadiranDinilai0;
	private MyCheckboxConfig sembunyikanFormatPenilaian;
	private Radiogroup mode;
	private Radiogroup lingkup;
	private MyIntbox batasWaktuBolehAbsenKehadiran;
	private MyCheckboxConfig aktif;
	private MyCheckboxConfig mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen;

	/**
	 * Varian ringkas {@link #init(Perkuliahan, Integer, Integer, Boolean, Boolean, Boolean)} untuk
	 * jadwal perkuliahan reguler (bukan pra-perkuliahan, bukan perkuliahan umum).
	 *
	 * @param perkuliahan      entitas jadwal yang akan dibuat/diubah (baru bila {@code getId()==null})
	 * @param semesterPendek   penanda konteks Semester Pendek, boleh {@code null}
	 * @param ekstrakurikuler  penanda konteks ekstrakurikuler ({@link Perkuliahan#EKSTRA}), boleh {@code null}
	 * @param merupakanRemedial penanda apakah jadwal ini remedial
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen ZK atau akses database
	 */
	public void init(final Perkuliahan perkuliahan, final Integer semesterPendek, final Integer ekstrakurikuler,
			final Boolean merupakanRemedial) throws Exception {
		init(perkuliahan, semesterPendek, ekstrakurikuler, false, false, merupakanRemedial);
	}

	/**
	 * Implementasi utama pembangun jendela "Tambah/Ubah Jadwal Perkuliahan". Melakukan validasi
	 * kewenangan (Fakultas/Program Studi user harus cocok dengan {@code perkuliahan.getJurusan()}
	 * bila jadwal sudah ada) dan status aktif periode penjadwalan sebelum membangun UI; bila salah
	 * satu gagal, method berhenti lebih awal setelah menampilkan {@link MyMessageboxConfig} tanpa
	 * membangun jendela. Selanjutnya membangun tab "Data Jadwal" (kombo Tahun Akademik, Semester
	 * Periode/Ganjil-Genap-SP, Program, Fakultas, Jurusan, Kelas, Mata Kuliah yang terikat pada
	 * kurikulum aktif, opsional Tahapan Kurikulum, Jumlah Dosen dan Dosen 1-10) dan tab "Rencana
	 * Pembelajaran/Silabus" yang baru dimuat lazily saat diklik (via
	 * {@link #pilihTabRencanaPembelajaran}) memakai {@link PenjadwalanHelper} atau
	 * {@link MatakuliahKurikulumDetailHelper} tergantung apakah jadwal sudah memiliki pertemuan.
	 * Field-field kombo dikunci ({@code setDisabled(true)}) apabila konfigurasi
	 * {@code jadwal_perkuliahan_tidak_bisa_diubah_ketika_diedit} aktif dan jadwal sudah diambil KRS
	 * oleh mahasiswa, demi menjaga integritas data.
	 *
	 * @param perkuliahan               entitas jadwal yang akan dibuat/diubah
	 * @param semesterPendek            penanda konteks Semester Pendek, boleh {@code null}
	 * @param ekstrakurikuler           penanda konteks ekstrakurikuler, boleh {@code null}
	 * @param merupakanPraPerkuliahan   {@code true} bila form untuk jadwal pra-perkuliahan (field
	 *                                  Fakultas/Jurusan/Semester disembunyikan/opsional)
	 * @param merupakanPerkuliahanUmum  {@code true} bila form untuk perkuliahan umum
	 * @param merupakanRemedial         penanda apakah jadwal ini remedial
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen ZK atau akses database
	 */
	public void init(final Perkuliahan perkuliahan, final Integer semesterPendek, final Integer ekstrakurikuler,
			final Boolean merupakanPraPerkuliahan, final Boolean merupakanPerkuliahanUmum,
			final Boolean merupakanRemedial) throws Exception {

		try {
			this.merupakanRemedial = merupakanRemedial;
			this.semesterPendek = semesterPendek;
			this.perkuliahan = perkuliahan;
			this.merupakanPraPerkuliahan = merupakanPraPerkuliahan;
			this.merupakanPerkuliahanUmum = merupakanPerkuliahanUmum;
			perkuliahan.setMerupakanPraPerkuliahan(merupakanPraPerkuliahan);
			perkuliahan.setMerupakanPerkuliahanUmum(merupakanPerkuliahanUmum);

			String ta = perkuliahan.getTahunAjaran();
			// SP-aware: perkuliahan Semester Pendek → cek konfigurasi PENJADWALAN_SP & tampilkan label "SP".
			boolean cekSp = perkuliahan.getStatusSemesterPendek() != null
					&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK);
			Integer semesterPendekCek = cekSp ? Perkuliahan.SEMESTER_PENDEK : semesterPendek;
			String sem = cekSp ? Perkuliahan.SP : perkuliahan.getGanjilGenap();
			if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendekCek, perkuliahan)) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, penjadwalan untuk Tahun Akademik \"{V1}\" semester \"{V2}\" saat ini belum diaktifkan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) hubungi bagian Akademik atau Administrator untuk mengaktifkan periode penjadwalan tersebut; (2) setelah diaktifkan, silakan Bapak/Ibu mengulangi proses ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
				return;
			}

			Tbmuser tbmuser = Common.getCurrentUser();
			if (perkuliahan != null && perkuliahan.getId() != null && perkuliahan.getJurusan() != null) {
				Fakultas userFakultas = tbmuser.ambilFakultas();
				Jurusan jurusan = tbmuser.ambilJurusan();
				if (userFakultas != null && perkuliahan.getJurusan() != null
						&& !userFakultas.getId().equals(perkuliahan.getJurusan().getFakultas().getId())) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu tidak memiliki wewenang untuk mengubah jadwal perkuliahan yang berasal dari Fakultas \"{V1}\". Langkah yang dapat dilakukan: (1) pastikan Bapak/Ibu mengelola jadwal pada Fakultas yang menjadi kewenangan Anda; (2) apabila memang diperlukan, hubungi Administrator untuk penyesuaian hak akses.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, perkuliahan.getJurusan().getFakultas().getNama());
					return;
				}
				if (jurusan != null && perkuliahan.getJurusan() != null
						&& !jurusan.getId().equals(perkuliahan.getJurusan().getId())) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu tidak memiliki wewenang untuk mengubah jadwal perkuliahan yang berasal dari Program Studi \"{V1}\". Langkah yang dapat dilakukan: (1) pastikan Bapak/Ibu mengelola jadwal pada Program Studi yang menjadi kewenangan Anda; (2) apabila memang diperlukan, hubungi Administrator untuk penyesuaian hak akses.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, perkuliahan.getJurusan().getNama());
					return;
				}
			}

			addWindow = new Window(
					perkuliahan.getId() == null ? "Tambah Jadwal Perkuliahan" : "Ubah Jadwal Perkuliahan", "none",
					true);
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
			kelas = new AmbilDataKelasBanbox();
			hari.setSelectedIndex(-1);

			addWindow.setWidth("600px");
			addWindow.setHeight("99%");
			addWindow.setPosition("center,center");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(addWindow);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			tabData = new MyTabConfig("Data Jadwal");
			tabData.setParent(tabs);

			tabData.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					addWindow.setWidth("600px");
				}
			});

			tabBiodata = new MyTabConfig("Rencana Pembelajaran / Silabus");
			tabBiodata.setVisible(!merupakanPraPerkuliahan && !merupakanPerkuliahanUmum);
			tabBiodata.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			tabpanelPindahan = new ais.ui.util.MyTabpanel();
			tabpanelPindahan.setParent(tabpanels);

			tabBiodata.addEventListener("onClick", pilihTabRencanaPembelajaran);

			final EventListener matakuliahEventListener = new EventListener() {
				@Override
				@SuppressWarnings({ })
				public void onEvent(Event event) throws Exception {
					if (matakuliah == null) {
						return;
					}
					Common.clear(matakuliah);
					matakuliah.setSelectedItem(null);

					if (merupakanPerkuliahanUmum) {

						Criteria criteria = HibernateUtil.currentSession().createCriteria(Matakuliah.class)
								.add(Restrictions.eq("merupakanPerkuliahanUmum", true))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {

							criteria.add(Restrictions.eq("extraKulikuler", true));
						} else {
							criteria.add(Restrictions.or(Restrictions.isNull("extraKulikuler"),
									Restrictions.eq("extraKulikuler", false)));
						}

						List<Matakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(criteria,
								Matakuliah.class);

						for (Matakuliah matakuliah : kurikulumPunyaMatakuliahs) {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setAttribute("kurikulumPunyaMatakuliah", null);
							comboitem.setLabel(matakuliah.getKode() + " - " + matakuliah.getNama());
							comboitem.setValue(matakuliah);
							String desc = "Kode: " + matakuliah.getKode() + ", Status: " + matakuliah.getStatus()
									+ ", SKS: " + matakuliah.getSks();
							comboitem.setDescription(desc);
							PenjadwalanUtil.this.matakuliah.appendChild(comboitem);
						}

						Common.selectComboItem(true, PenjadwalanUtil.this.matakuliah, perkuliahan.getMatakuliah());

					} else if (merupakanPraPerkuliahan) {

						Criteria criteria = HibernateUtil.currentSession().createCriteria(Matakuliah.class)
								.add(Restrictions.eq("merupakanPraPerkuliahan", true))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {

							criteria.add(Restrictions.eq("extraKulikuler", true));
						} else {
							criteria.add(Restrictions.or(Restrictions.isNull("extraKulikuler"),
									Restrictions.eq("extraKulikuler", false)));
						}

						List<Matakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(criteria,
								Matakuliah.class);

						for (Matakuliah matakuliah : kurikulumPunyaMatakuliahs) {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setAttribute("kurikulumPunyaMatakuliah", null);
							comboitem.setLabel(matakuliah.getKode() + " - " + matakuliah.getNama());
							comboitem.setValue(matakuliah);
							String desc = "Kode: " + matakuliah.getKode() + ", Status: " + matakuliah.getStatus()
									+ ", SKS: " + matakuliah.getSks();
							comboitem.setDescription(desc);
							PenjadwalanUtil.this.matakuliah.appendChild(comboitem);
						}

						Common.selectComboItem(true, PenjadwalanUtil.this.matakuliah, perkuliahan.getMatakuliah());

					} else {

						if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
							return;
						}
						if (kurikulum.getSelectedItem() == null) {
							return;
						}
						if (!ambilMkDiluarSemesterKurikulum.isChecked() && (semester.getSelectedItem() == null
								|| semester.getSelectedItem().getValue() == null)) {
							return;
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Criteria criteria = HibernateUtil.currentSession()
										.createCriteria(KurikulumPunyaMatakuliah.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(kurikulum.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("kurikulum", kurikulum.getSelectedItem().getValue()))

										.add(ambilMkDiluarSemesterKurikulum.isChecked()
												? Restrictions.sqlRestriction("true")
												: semester.getSelectedItem() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("semester",
																semester.getSelectedItem().getValue()))
										.createAlias("matakuliah", "matakuliah")
										.add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"),
										Restrictions.eq("matakuliah.aktif", true)));

								if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {

									criteria.add(Restrictions.eq("matakuliah.extraKulikuler", true));
								} else {
									criteria.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
											Restrictions.eq("matakuliah.extraKulikuler", false)));
								}

								List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(
										criteria.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama")),
										KurikulumPunyaMatakuliah.class);

								for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
									if (!kurikulumPunyaMatakuliah.getMatakuliah().getAktif()) {
										continue;
									}
									Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
									org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
									comboitem.setAttribute("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah);
									comboitem.setLabel(matakuliah.getKode() + " - " + matakuliah.getNama());
									comboitem.setValue(matakuliah);
									String desc = "Kode: " + matakuliah.getKode() + ", Status: "
											+ matakuliah.getStatus() + ", SKS: " + matakuliah.getSks() + ", SMT: "
											+ kurikulumPunyaMatakuliah.getSemester()
											+ (kurikulumPunyaMatakuliah.getTahap() == null ? ""
													: ", Tahap : " + kurikulumPunyaMatakuliah.getTahap());
									comboitem.setDescription(desc);
									PenjadwalanUtil.this.matakuliah.appendChild(comboitem);
								}

								Common.selectComboItem(true, PenjadwalanUtil.this.matakuliah,
										perkuliahan.getMatakuliah());

							}
						});
					}
				}

			};

			final EventListener kurikulumEventListener = new EventListener() {
				@Override
				@SuppressWarnings({ })
				public void onEvent(Event event) throws Exception {
					Common.clear(kurikulum);
					kurikulum.setSelectedItem(null);
					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						return;
					}

					Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
							|| jurusan.getSelectedItem().getValue() == null ? null
									: jurusan.getSelectedItem().getValue());

					List<Kurikulum> kurikulums = ConstantValues
							.simpleList(HibernateUtil.currentSession().createCriteria(Kurikulum.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.addOrder(Order.desc("tahun"))

									.createAlias("program", "program", Criteria.LEFT_JOIN)
									.add(Restrictions.or(Restrictions.isNull("program"),
											Restrictions.eq("program.nama",
													program.getSelectedItem() == null ? null
															: program.getSelectedItem().getValue())))

									.add(Restrictions.eq("jurusan", myJurusan)), Kurikulum.class);

					for (Kurikulum kurikulum : kurikulums) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
						comboitem.setValue(kurikulum);
						comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
								+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
						PenjadwalanUtil.this.kurikulum.appendChild(comboitem);
					}

					if (perkuliahan.getKurikulum() == null && myJurusan != null) {
						Kurikulum mykurikulum = (Kurikulum) ConstantValues
								.simpleObject(HibernateUtil.currentSession().createCriteria(Kurikulum.class)
										.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan))

										.createAlias("program", "program", Criteria.LEFT_JOIN)
										.add(Restrictions.or(Restrictions.isNull("program"),
												Restrictions.eq("program.nama",
														program.getSelectedItem() == null ? null
																: program.getSelectedItem().getValue())))

										.setMaxResults(1), Kurikulum.class);
						Common.selectComboItem(true, kurikulum, mykurikulum);
					}

					if (kurikulum.getSelectedItem() != null) {
						matakuliahEventListener.onEvent(event);
					}

				}

			};

			Borderlayout borderlayoutBaru = new ais.ui.util.MyBorderlayout();
			borderlayoutBaru.setParent(tabpanel);

			Center centerBaru = new Center();
			centerBaru.setParent(borderlayoutBaru);
			ais.ui.util.ZkCompat.setFlex(centerBaru, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(centerBaru);
			grid.setWidth("100%");
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
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
			Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
					perkuliahan.getTahunAjaran());
			row.appendChild(tahunAjaran);
			tahunAjaran.setWidth("90%");

			ganjilGenap = new Combobox();
			ganjilGenap.setReadonly(true);
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			ganjilGenap.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			ganjilGenap.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Ikut Smt Kurikulum");
			comboitem.setValue(null);
			ganjilGenap.appendChild(comboitem);

			// Pilihan "Semester Pendek (SP)" — pengganti tab "SP" yang dihilangkan. Bila dipilih, saat
			// simpan perkuliahan ditandai Semester Pendek (statusSemesterPendek = SEMESTER_PENDEK) dan
			// kolom ganjilGenap dikosongkan (lihat blok simpan di bawah). Nilai combo = Perkuliahan.SP.
			MyComboitemConfig comboitemSp = new MyComboitemConfig();
			comboitemSp.setLabel("Semester Pendek (SP)");
			comboitemSp.setValue(Perkuliahan.SP);
			ganjilGenap.appendChild(comboitemSp);

			row = new MyFormRow();
			row.setVisible(semesterPendek == null);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Semester Periode"));
			if (perkuliahan.getStatusSemesterPendek() != null
					&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) {
				// Semester Pendek (data baru dari filter SP maupun edit perkuliahan SP) → pra-pilih "Semester Pendek (SP)".
				ganjilGenap.setSelectedItem(comboitemSp);
			} else if (perkuliahan.getId() == null) {
				// Data BARU: ikuti nilai ganjilGenap dari filter (Ganjil/Genap) bila ada; selain itu default
				// "Ikut Smt Kurikulum". Ini membuat "Semester Periode" mengikuti filter "Jenis Smt".
				if (perkuliahan.getGanjilGenap() != null && perkuliahan.getGanjilGenap().trim().length() > 0) {
					Common.selectComboItem(true, ganjilGenap, perkuliahan.getGanjilGenap());
				} else {
					ganjilGenap.setSelectedItem(comboitem);
				}
			} else {
				Common.selectComboItem(true, ganjilGenap, perkuliahan.getGanjilGenap());
			}
			row.appendChild(ganjilGenap);
			ganjilGenap.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					"Program " + (merupakanPraPerkuliahan || merupakanPerkuliahanUmum ? "" : " *")));
			Common.selectComboItem(true, program,
					perkuliahan.getProgram() == null
							? (tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama())
							: perkuliahan.getProgram());
			row.appendChild(program);
			program.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					"Fakultas" + (merupakanPraPerkuliahan || merupakanPerkuliahanUmum ? "" : " *")));
			Common.selectComboItem(true, fakultas, perkuliahan.getJurusan() == null ? tbmuser.ambilFakultas()
					: perkuliahan.getJurusan().getFakultas());
			row.appendChild(fakultas);
			fakultas.setWidth("90%");

			if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan")
					+ (merupakanPraPerkuliahan || merupakanPerkuliahanUmum ? "" : " *")));
			Common.selectComboItem(true, jurusan,
					perkuliahan.getJurusan() == null ? tbmuser.ambilJurusan() : perkuliahan.getJurusan());
			row.appendChild(jurusan);
			jurusan.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(!perkuliahan.getMerupakanPraPerkuliahan());
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					"Semester " + (merupakanPraPerkuliahan || merupakanPerkuliahanUmum ? "" : " *")));
			row.appendChild(semester);
			Common.selectComboItem(true, semester, perkuliahan.getSemester());
			semester.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(!perkuliahan.getMerupakanPraPerkuliahan());
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					"Kurikulum " + (merupakanPraPerkuliahan || merupakanPerkuliahanUmum ? "" : " *")));
			row.appendChild(kurikulum = new Combobox());
			kurikulum.setWidth("90%");

			// KurikulumEventListener kurikulumEventListener = new
			// KurikulumEventListener();

			jurusan.addEventListener("onChange", kurikulumEventListener);
			program.addEventListener("onChange", kurikulumEventListener);

			kurikulumEventListener.onEvent(null);

			if (perkuliahan.getKurikulum() != null)
				Common.selectComboItem(true, kurikulum, perkuliahan.getKurikulum());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					"Kelas " + (merupakanPraPerkuliahan || merupakanPerkuliahanUmum ? "" : " *")));

			Hbox hboxKelas = new Hbox();
			row.appendChild(hboxKelas);
			hboxKelas.appendChild(kelas);
			kelas.setValue(perkuliahan.getKelas() == null ? "A" : perkuliahan.getKelas());
			kelas.setCols(4);
			kelas.setAttribute("kelas", perkuliahan.getKelasref());

			kelasref = new MyCheckboxConfig("Nama kelas selalu disamakan dgn master data kelas");
			hboxKelas.appendChild(kelasref);
			kelasref.setChecked(perkuliahan.getId() == null || perkuliahan.getKelasref() != null);

			row = new MyFormRow();
			row.setParent(rows);
			ambilMkDiluarSemesterKurikulum = new MyCheckboxConfig("Ambil matakuliah di luar semester kurikulum");
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(ambilMkDiluarSemesterKurikulum);
			ambilMkDiluarSemesterKurikulum.setChecked(perkuliahan.getAmbilMkDiluarSemesterKurikulum());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Matakuliah") + " *"));

			row.appendChild(matakuliah = new Combobox());
			matakuliah.setWidth("90%");

			semester.addEventListener("onChange", matakuliahEventListener);
			kurikulum.addEventListener("onChange", matakuliahEventListener);
			ambilMkDiluarSemesterKurikulum.addEventListener("onClick", matakuliahEventListener);

			matakuliahEventListener.onEvent(null);

			Common.selectComboItem(true, matakuliah, perkuliahan.getMatakuliah());

			matakuliah.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			terdapatKegiatanPraktek = new MyCheckboxConfig("Terdapat kegiatan praktek pada matakuliah ini");
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(terdapatKegiatanPraktek);
			terdapatKegiatanPraktek.setChecked(perkuliahan.getTerdapatKegiatanPraktek());

			if (perkuliahan.getId() != null) {

				if (Common.bolehKonfigurasi("jadwal_perkuliahan_tidak_bisa_diubah_ketika_diedit")) {

					Integer[] s = perkuliahan.ambilStatusKrs();

					Integer telahDisetujui = s[1];
					Integer belumDisetujui = s[0];

					if ((telahDisetujui + belumDisetujui) > 0) {
						tahunAjaran.setDisabled(true);
						program.setDisabled(true);
						fakultas.setDisabled(true);
						jurusan.setDisabled(true);
						semester.setDisabled(true);
						kurikulum.setDisabled(true);
						matakuliah.setDisabled(true);
						kelas.setDisabled(true);
						MyMessageboxConfig.show(
								"Demi menjaga integritas jadwal, jadwal perkuliahan ini telah diambil oleh beberapa mahasiswa sehingga data mata kuliah dan kelas tidak dapat diubah. Langkah yang dapat dilakukan: (1) batalkan dan bersihkan terlebih dahulu data mahasiswa yang telah mengambil jadwal ini; (2) setelah data mahasiswa bersih, silakan Bapak/Ibu melakukan perubahan data mata kuliah dan kelas.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					} else {
						tahunAjaran.setDisabled(false);
						program.setDisabled(false);
						semester.setDisabled(false);
						kurikulum.setDisabled(false);
						matakuliah.setDisabled(false);
						kelas.setDisabled(false);
						fakultas.setDisabled(tbmuser.ambilFakultas() != null);
						jurusan.setDisabled(tbmuser.ambilJurusan() != null);
					}

				}

			} else {
				tahunAjaran.setDisabled(false);
				program.setDisabled(false);
				semester.setDisabled(false);
				kurikulum.setDisabled(false);
				matakuliah.setDisabled(false);
				kelas.setDisabled(false);
				fakultas.setDisabled(tbmuser.ambilFakultas() != null);
				jurusan.setDisabled(tbmuser.ambilJurusan() != null);
			}

			if (ConstantValues.aktifkanTahapanKurikulum) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahapan"));
				row.appendChild(tahap = new Combobox());

				EventListener tahapanEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(tahap);

						Kurikulum selectedKurikulum = (Kurikulum) (kurikulum.getSelectedItem() == null ? null
								: kurikulum.getSelectedItem().getValue());
						Integer selectedSemester = (Integer) (semester.getSelectedItem() == null ? null
								: semester.getSelectedItem().getValue());
						Matakuliah selectedMatakuliah = (Matakuliah) (matakuliah.getSelectedItem() == null ? null
								: matakuliah.getSelectedItem().getValue());

						String pro = (String) (program.getSelectedItem() == null ? null
								: program.getSelectedItem().getValue());
						Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						if (selectedMatakuliah != null && selectedMatakuliah.getSksPraktek() > 0) {
							terdapatKegiatanPraktek.setDisabled(true);
							terdapatKegiatanPraktek.setChecked(true);
						} else {
							if (terdapatKegiatanPraktek.isDisabled()) {
								terdapatKegiatanPraktek.setChecked(false);
							}
							terdapatKegiatanPraktek.setDisabled(false);
						}

						if (pro != null && jur != null && selectedKurikulum != null && selectedSemester != null
								&& selectedMatakuliah != null) {

							perkuliahan.setKurikulum(selectedKurikulum);
							perkuliahan.setSemester(selectedSemester);
							perkuliahan.setMatakuliah(selectedMatakuliah);

							KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan
									.populateKurikulumPunyaMatakuliah();

							if (ConstantValues.jumlahTahapan.isEmpty()) {
								ConstantValues.initJumlahTahapan();
							}

							int jumlahTahapan = ConstantValues.getJumlahTahapan(pro, jur);

							for (int i = 1; i <= (jumlahTahapan * 5); i++) {
								MyComboitemConfig comboitem = new MyComboitemConfig("Tahap " + i);
								comboitem.setValue(i);
								tahap.appendChild(comboitem);
							}
							MyComboitemConfig comboitem = new MyComboitemConfig("Tanpa tahap");
							comboitem.setValue(null);
							tahap.appendChild(comboitem);

							if (kurikulumPunyaMatakuliah == null || kurikulumPunyaMatakuliah.getTahap() == null) {
								tahap.setSelectedItem(comboitem);
							} else {
								Common.selectComboItem(true, tahap, kurikulumPunyaMatakuliah.getTahap());
							}
							tahap.setReadonly(true);
							tahap.setWidth("90%");
						}
					}
				};

				tahap.setReadonly(true);

				matakuliah.addEventListener("onChange", tahapanEventListener);
				kurikulum.addEventListener("onChange", tahapanEventListener);
				semester.addEventListener("onChange", tahapanEventListener);

				Common.createDefaultTimer(tahapanEventListener);
			}

			rowJumlahDosen = new MyFormRow();
			rowJumlahDosen.setParent(rows);
			rowJumlahDosen.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Dosen *"));
			rowJumlahDosen.appendChild(jumlahDosen);
			Common.selectComboItem(jumlahDosen,
					perkuliahan.getId() == null && perkuliahan.getJumlahDosen() < 1 ? 1 : perkuliahan.getJumlahDosen());
			jumlahDosen.setWidth("90%");
			jumlahDosen.setReadonly(true);

			rowMerupakanTeamTeaching = new MyFormRow();
			rowMerupakanTeamTeaching.setStyle("border:0px;background: transparent;");
			rowMerupakanTeamTeaching.setParent(rows);
			merupakanTeamTeaching = new MyCheckboxConfig(
					"Merupakan Team Teaching, atau semua dosen bersama-sama masuk kelas");
			rowMerupakanTeamTeaching.appendChild(new ais.ui.util.MyLabelConfig(""));
			rowMerupakanTeamTeaching.appendChild(merupakanTeamTeaching);
			merupakanTeamTeaching.setChecked(perkuliahan.getMerupakanTeamTeaching());

			rowdosen1 = new MyFormRow();
			rowdosen1.setStyle("border:0px;background: transparent;");
			rowdosen1.setParent(rows);
			rowdosen1.appendChild(new MyLabelConfig("Dosen Utama *"));
			Hbox hbox = new Hbox();
			hbox.appendChild(dosen1 = new AmbilDataDosenBanbox());
			hbox.appendChild(merupakan_tanpa_dosen = new MyCheckboxConfig(Common.getBahasaConfig("Tanpa Dosen")));

			final EventListener jumlahDosenEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					rowJumlahDosen.setVisible(!merupakan_tanpa_dosen.isChecked());

					Integer jml = (Integer) (jumlahDosen.getSelectedItem() == null ? 1
							: jumlahDosen.getSelectedItem().getValue());

					rowMerupakanTeamTeaching.setVisible(jml >= 2);

					dosen1.setVisible(jml >= 1 && !merupakan_tanpa_dosen.isChecked());

					rowdosen2.setVisible(jml >= 2 && !merupakan_tanpa_dosen.isChecked());
					rowdosen3.setVisible(jml >= 3 && !merupakan_tanpa_dosen.isChecked());
					rowdosen4.setVisible(jml >= 4 && !merupakan_tanpa_dosen.isChecked());
					rowdosen5.setVisible(jml >= 5 && !merupakan_tanpa_dosen.isChecked());
					rowdosen6.setVisible(jml >= 6 && !merupakan_tanpa_dosen.isChecked());
					rowdosen7.setVisible(jml >= 7 && !merupakan_tanpa_dosen.isChecked());
					rowdosen8.setVisible(jml >= 8 && !merupakan_tanpa_dosen.isChecked());
					rowdosen9.setVisible(jml >= 9 && !merupakan_tanpa_dosen.isChecked());
					rowdosen10.setVisible(jml >= 10 && !merupakan_tanpa_dosen.isChecked());

					if (merupakan_tanpa_dosen.isChecked()) {
						dosen1.removeAttribute("myValue");
						dosen1.removeAttribute("dosen");

						dosen2.removeAttribute("myValue");
						dosen2.removeAttribute("dosen");

						dosen3.removeAttribute("myValue");
						dosen3.removeAttribute("dosen");

						dosen4.removeAttribute("myValue");
						dosen4.removeAttribute("dosen");

						dosen5.removeAttribute("myValue");
						dosen5.removeAttribute("dosen");

						dosen6.removeAttribute("myValue");
						dosen6.removeAttribute("dosen");

						dosen7.removeAttribute("myValue");
						dosen7.removeAttribute("dosen");

						dosen8.removeAttribute("myValue");
						dosen8.removeAttribute("dosen");

						dosen9.removeAttribute("myValue");
						dosen9.removeAttribute("dosen");

						dosen10.removeAttribute("myValue");
						dosen10.removeAttribute("dosen");
					}
				}
			};

			jumlahDosen.addEventListener("onChange", jumlahDosenEventListener);

			merupakan_tanpa_dosen.addEventListener(Events.ON_CHECK, jumlahDosenEventListener);
			merupakan_tanpa_dosen.setChecked(
					perkuliahan.getMerupakan_tanpa_dosen() != null && perkuliahan.getMerupakan_tanpa_dosen());

			rowdosen1.appendChild(hbox);
			dosen1.setValue(perkuliahan.getDosen1() == null ? "" : (perkuliahan.getDosen1().getNama()));
			dosen1.setAttribute("myValue", perkuliahan.getDosen1());
			dosen1.setWidth("90%");

			rowdosen2 = new MyFormRow();
			rowdosen2.setStyle("border:0px;background: transparent;");
			rowdosen2.setParent(rows);
			rowdosen2.appendChild(new MyLabelConfig(("Dosen 2")));
			rowdosen2.appendChild(dosen2 = new AmbilDataDosenBanbox());
			dosen2.setValue(perkuliahan.getDosen2() == null ? "" : (perkuliahan.getDosen2().getNama()));
			dosen2.setAttribute("myValue", perkuliahan.getDosen2());
			dosen2.setWidth("90%");

			rowdosen3 = new MyFormRow();
			rowdosen3.setStyle("border:0px;background: transparent;");
			rowdosen3.setParent(rows);
			rowdosen3.appendChild(new MyLabelConfig(("Dosen 3")));
			rowdosen3.appendChild(dosen3 = new AmbilDataDosenBanbox());
			dosen3.setValue(perkuliahan.getDosen3() == null ? "" : (perkuliahan.getDosen3().getNama()));

			dosen3.setAttribute("myValue", perkuliahan.getDosen3());
			dosen3.setWidth("90%");

			rowdosen4 = new MyFormRow();
			rowdosen4.setStyle("border:0px;background: transparent;");
			rowdosen4.setParent(rows);
			rowdosen4.appendChild(new MyLabelConfig(("Dosen 4")));
			rowdosen4.appendChild(dosen4 = new AmbilDataDosenBanbox());
			dosen4.setValue(perkuliahan.getDosen4() == null ? "" : (perkuliahan.getDosen4().getNama()));

			dosen4.setAttribute("myValue", perkuliahan.getDosen4());
			dosen4.setWidth("90%");

			rowdosen5 = new MyFormRow();
			rowdosen5.setStyle("border:0px;background: transparent;");
			rowdosen5.setParent(rows);
			rowdosen5.appendChild(new MyLabelConfig(("Dosen 5")));
			rowdosen5.appendChild(dosen5 = new AmbilDataDosenBanbox());
			dosen5.setValue(perkuliahan.getDosen5() == null ? "" : (perkuliahan.getDosen5().getNama()));

			dosen5.setAttribute("myValue", perkuliahan.getDosen5());
			dosen5.setWidth("90%");

			rowdosen6 = new MyFormRow();
			rowdosen6.setStyle("border:0px;background: transparent;");
			rowdosen6.setParent(rows);
			rowdosen6.appendChild(new MyLabelConfig(("Dosen 6")));
			rowdosen6.appendChild(dosen6 = new AmbilDataDosenBanbox());
			dosen6.setValue(perkuliahan.getDosen6() == null ? "" : (perkuliahan.getDosen6().getNama()));

			dosen6.setAttribute("myValue", perkuliahan.getDosen6());
			dosen6.setWidth("90%");

			rowdosen7 = new MyFormRow();
			rowdosen7.setStyle("border:0px;background: transparent;");
			rowdosen7.setParent(rows);
			rowdosen7.appendChild(new MyLabelConfig(("Dosen 7")));
			rowdosen7.appendChild(dosen7 = new AmbilDataDosenBanbox());
			dosen7.setValue(perkuliahan.getDosen7() == null ? "" : (perkuliahan.getDosen7().getNama()));

			dosen7.setAttribute("myValue", perkuliahan.getDosen7());
			dosen7.setWidth("90%");

			rowdosen8 = new MyFormRow();
			rowdosen8.setStyle("border:0px;background: transparent;");
			rowdosen8.setParent(rows);
			rowdosen8.appendChild(new MyLabelConfig(("Dosen 8")));
			rowdosen8.appendChild(dosen8 = new AmbilDataDosenBanbox());
			dosen8.setValue(perkuliahan.getDosen8() == null ? "" : (perkuliahan.getDosen8().getNama()));

			dosen8.setAttribute("myValue", perkuliahan.getDosen8());
			dosen8.setWidth("90%");

			rowdosen9 = new MyFormRow();
			rowdosen9.setStyle("border:0px;background: transparent;");
			rowdosen9.setParent(rows);
			rowdosen9.appendChild(new MyLabelConfig(("Dosen 9")));
			rowdosen9.appendChild(dosen9 = new AmbilDataDosenBanbox());
			dosen9.setValue(perkuliahan.getDosen9() == null ? "" : (perkuliahan.getDosen9().getNama()));

			dosen9.setAttribute("myValue", perkuliahan.getDosen9());
			dosen9.setWidth("90%");

			rowdosen10 = new MyFormRow();
			rowdosen10.setStyle("border:0px;background: transparent;");
			rowdosen10.setParent(rows);
			rowdosen10.appendChild(new MyLabelConfig(("Dosen 10")));
			rowdosen10.appendChild(dosen10 = new AmbilDataDosenBanbox());
			dosen10.setValue(perkuliahan.getDosen10() == null ? "" : (perkuliahan.getDosen10().getNama()));

			dosen10.setAttribute("myValue", perkuliahan.getDosen10());
			dosen10.setWidth("90%");

			jumlahDosenEventListener.onEvent(null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Ruang *"));

			hbox = new Hbox();
			hbox.appendChild(ruang = new AmbilDataRuangBanbox());
			hbox.appendChild(merupakan_tanpa_ruangan = new MyCheckboxConfig("Tanpa ruang"));
			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ruang.setVisible(!merupakan_tanpa_ruangan.isChecked());
					if (merupakan_tanpa_ruangan.isChecked()) {
						ruang.removeAttribute("ruang");
					}
				}
			};

			merupakan_tanpa_ruangan.addEventListener(Events.ON_CHECK, eventListener);
			merupakan_tanpa_ruangan.setChecked(
					perkuliahan.getMerupakan_tanpa_ruangan() != null && perkuliahan.getMerupakan_tanpa_ruangan());

			row.appendChild(hbox);
			ruang.setValue(perkuliahan.getRuang() == null ? "" : (perkuliahan.getRuang().getKodeRuangan()));
			ruang.setId("" + perkuliahan.getRuang() == null ? "ruang_-1" : "ruang_" + ruang.getId());
			ruang.setAttribute("ruang", perkuliahan.getRuang());
			ruang.setWidth("90%");
			eventListener.onEvent(null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas *"));
			row.appendChild(kapasitasKelas = new Decimalbox(
					perkuliahan.getKapasitasKelas() == null ? null : new BigDecimal(perkuliahan.getKapasitasKelas())));
			kapasitasKelas.setWidth("90%");

			ruang.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (kapasitasKelas.getValue() == null) {
						Ruang myRuang = (Ruang) ruang.getAttribute("ruang");
						if (myRuang != null) {
							kapasitasKelas.setValue(myRuang.getKapasitasRuangan() == null ? null
									: new BigDecimal(myRuang.getKapasitasRuangan()));
						}
					}
				}
			});

			fakultas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ruang.setFakultas((Fakultas) (fakultas.getSelectedItem() == null
							|| fakultas.getSelectedItem().getValue() == null ? null
									: fakultas.getSelectedItem().getValue()));
					jamPerkuliahan.setFakultasSelected((Fakultas) (fakultas.getSelectedItem() == null
							|| fakultas.getSelectedItem().getValue() == null ? null
									: fakultas.getSelectedItem().getValue()));
					masaPerkuliahan.setFakultasSelected((Fakultas) (fakultas.getSelectedItem() == null
							|| fakultas.getSelectedItem().getValue() == null ? null
									: fakultas.getSelectedItem().getValue()));

				}
			});

			jurusan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ruang.setJurusan(
							(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? null
									: jurusan.getSelectedItem().getValue()));
					jamPerkuliahan.setJurusanSelected(
							(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? null
									: jurusan.getSelectedItem().getValue()));
					masaPerkuliahan.setJurusanSelected(
							(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? null
									: jurusan.getSelectedItem().getValue()));

				}
			});

			fakultas.setReadonly(true);
			jurusan.setReadonly(true);
			kurikulum.setReadonly(true);
			program.setReadonly(true);
			semester.setReadonly(true);
			jumlahDosen.setReadonly(true);
			kelas.setReadonly(true);
			matakuliah.setReadonly(true);
			hari.setReadonly(true);
			tahunAjaran.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mode *"));

			mode = new Radiogroup();
			MyRadioConfig radio = new MyRadioConfig();
			radio.setLabel("Online");
			radio.setValue("O");
			mode.appendChild(radio);
			if (perkuliahan.getMode().equalsIgnoreCase("O")) {
				radio.setSelected(true);
			}

			radio = new MyRadioConfig();
			radio.setLabel("Offline");
			radio.setValue("F");
			mode.appendChild(radio);
			if (perkuliahan.getMode().equalsIgnoreCase("F")) {
				radio.setSelected(true);
			}

			radio = new MyRadioConfig();
			radio.setLabel("Campuran");
			radio.setValue("M");
			mode.appendChild(radio);
			if (perkuliahan.getMode().equalsIgnoreCase("M")) {
				radio.setSelected(true);
			}
			row.appendChild(mode);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Lingkup *"));

			lingkup = new Radiogroup();
			radio = new MyRadioConfig();
			radio.setLabel("Internal");
			radio.setValue("1");
			lingkup.appendChild(radio);
			if (perkuliahan.getLingkup().equalsIgnoreCase("1")) {
				radio.setSelected(true);
			}
			radio = new MyRadioConfig();
			radio.setLabel("External");
			radio.setValue("2");
			lingkup.appendChild(radio);
			if (perkuliahan.getLingkup().equalsIgnoreCase("2")) {
				radio.setSelected(true);
			}
			radio = new MyRadioConfig();
			radio.setLabel("Campuran");
			radio.setValue("3");
			lingkup.appendChild(radio);
			if (perkuliahan.getLingkup().equalsIgnoreCase("3")) {
				radio.setSelected(true);
			}
			row.appendChild(lingkup);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Perkuliahan *"));

			hbox = new Hbox();
			hbox.appendChild(waktu);
			hbox.appendChild(merupakan_tanpa_jadwal_perkuliahan = new MyCheckboxConfig("Tanpa jadwal mingguan"));
			eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					waktu.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());

					if (waktu.isVisible()) {
						waktu.setVisible(Common.bolehKonfigurasi("waktu_perkuliahan_pagi_siang_sore_malam_ditampilkan"));
					}

					waktuMulai.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
					waktuSelesai.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
					hari.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
					merupakan_paralel.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());

					jamPerkuliahan.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());

					if (merupakan_tanpa_jadwal_perkuliahan.isChecked()) {
						merupakan_paralel.setChecked(false);
						perkuliahan_paralel.setSelectedIndex(-1);
					}

					if (merupakan_tanpa_jadwal_perkuliahan.isChecked()) {
						waktu.setValue(null);
						waktuMulai.setValue(null);
						waktuSelesai.setValue(null);
						hari.setSelectedItem(null);
						jamPerkuliahan.setAttribute("jamPerkuliahan", null);
						jamPerkuliahan.setAttribute("myValue", null);
					}

					rowKeteranganJadwal.setVisible(merupakan_tanpa_jadwal_perkuliahan.isChecked());
					rowJamPerkuliahan.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
					rowWaktu.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
					rowHari.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
				}

			};
			merupakan_tanpa_jadwal_perkuliahan.addEventListener(Events.ON_CHECK, eventListener);
			merupakan_tanpa_jadwal_perkuliahan.setChecked(perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() != null
					&& perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan());
			row.appendChild(hbox);

			Common.selectComboItem(true, waktu, perkuliahan.getWaktu());
			waktu.setWidth("90%");

			Date dateMulai = null;
			Date dateSelesai = null;
			try {
				System.out.println("Perkuliahan getWaktuMulai = "
						+ (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()));
				if ((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) != null
						&& !(perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()).equals(""))
					dateMulai = Common.timeFormat2.get()
							.parse((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			try {
				if ((perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()) != null
						&& !(perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()).equals(""))
					dateSelesai = Common.timeFormat2.get()
							.parse((perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			rowKeteranganJadwal = new MyFormRow();
			rowKeteranganJadwal.setStyle("border:0px;background: transparent;");
			rowKeteranganJadwal.setParent(rows);
			rowKeteranganJadwal.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Jadwal"));
			rowKeteranganJadwal.appendChild(keteranganJadwal = new Textbox(perkuliahan.getKeteranganJadwal()));
			keteranganJadwal.setWidth("90%");
			keteranganJadwal.setRows(2);

			rowKeteranganJadwal.setVisible(merupakan_tanpa_jadwal_perkuliahan.isChecked());

			rowJamPerkuliahan = new MyFormRow();
			rowJamPerkuliahan.setStyle("border:0px;background: transparent;");
			rowJamPerkuliahan.setParent(rows);
			rowJamPerkuliahan.appendChild(new ais.ui.util.MyLabelConfig("Jam Perkuliahan"));
			rowJamPerkuliahan.appendChild(jamPerkuliahan = new AmbilDataJamPerkuliahanBanbox(
					(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
							: jurusan.getSelectedItem().getValue())));
			jamPerkuliahan
					.setValue(perkuliahan.getJamPerkuliahan() == null ? "" : perkuliahan.getJamPerkuliahan().getNama());
			jamPerkuliahan.setAttribute("jamPerkuliahan", perkuliahan.getJamPerkuliahan());
			jamPerkuliahan.setAttribute("myValue", perkuliahan.getJamPerkuliahan());
			jamPerkuliahan.setWidth("90%");

			jurusan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jamPerkuliahan.setJurusan(
							(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? null
									: jurusan.getSelectedItem().getValue()));
				}
			});

			rowWaktu = new MyFormRow();
			rowWaktu.setStyle("border:0px;background: transparent;");
			rowWaktu.setParent(rows);
			rowWaktu.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
			hbox = new Hbox();
			rowWaktu.appendChild(hbox);
			hbox.appendChild(
					waktuMulai = new MyTimebox(dateMulai == null ? ais.ui.util.WaktuUtil.getDate() : dateMulai));
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
			hbox.appendChild(
					waktuSelesai = new MyTimebox(dateSelesai == null ? ais.ui.util.WaktuUtil.getDate() : dateSelesai));

			EventListener jamPerkuliahanEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					JamPerkuliahan myJamPerkuliahan = (JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan");
					if (myJamPerkuliahan != null) {
						waktuMulai.setValue(myJamPerkuliahan.getMulai());
						waktuSelesai.setValue(myJamPerkuliahan.getSampai());
					}

					boolean jamPerkuliahanWajibDipilih = Common.bolehKonfigurasi("jam_perkuliahan_wajib_dipilih", Konfigurasi.TIDAK_AKTIF);

					waktuMulai.setDisabled(myJamPerkuliahan != null || jamPerkuliahanWajibDipilih);
					waktuSelesai.setDisabled(myJamPerkuliahan != null || jamPerkuliahanWajibDipilih);
				}
			};

			jamPerkuliahan.setEventListener(jamPerkuliahanEventListener);

			boolean jamPerkuliahanWajibDipilih = Common.bolehKonfigurasi("jam_perkuliahan_wajib_dipilih", Konfigurasi.TIDAK_AKTIF);

			waktuMulai.setDisabled(perkuliahan.getJamPerkuliahan() != null || jamPerkuliahanWajibDipilih);
			waktuSelesai.setDisabled(perkuliahan.getJamPerkuliahan() != null || jamPerkuliahanWajibDipilih);

			rowHari = new MyFormRow();
			rowHari.setStyle("border:0px;background: transparent;");
			rowHari.setParent(rows);
			rowHari.appendChild(new ais.ui.util.MyLabelConfig("Hari *"));
			Common.selectComboItem(true, hari, (perkuliahan.getHari() == null ? "" : perkuliahan.getHari()));
			rowHari.appendChild(hari);
			hari.setWidth("90%");

			rowKeteranganJadwal.setVisible(merupakan_tanpa_jadwal_perkuliahan.isChecked());
			rowJamPerkuliahan.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
			rowWaktu.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());
			rowHari.setVisible(!merupakan_tanpa_jadwal_perkuliahan.isChecked());

			// hari.addEventListener("onChange", hariEvent);
			// hariEvent.onEvent(null);

			boolean tampilkan_masa_perkuliahan = Common.bolehKonfigurasi("tampilkan_masa_perkuliahan");

			row = new MyFormRow();
			row.setVisible(tampilkan_masa_perkuliahan);
			row.setParent(rows);

			if (Common.bolehKonfigurasi("masa_perkuliahan_di_jadwal_perkuliahan_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan *"));
			} else {
				row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
			}

			row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox(
					(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
							: jurusan.getSelectedItem().getValue())));
			masaPerkuliahan.setValue(
					perkuliahan.getMasaPerkuliahan() == null ? "" : perkuliahan.getMasaPerkuliahan().getNama());
			masaPerkuliahan.setAttribute("masaPerkuliahan", perkuliahan.getMasaPerkuliahan());
			masaPerkuliahan.setAttribute("myValue", perkuliahan.getMasaPerkuliahan());
			masaPerkuliahan.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(tampilkan_masa_perkuliahan);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku mulai"));
			row.appendChild(new Hbox(new Component[] {
					perkuliahanDimulai = new MyDatebox(perkuliahan.getPerkuliahanDimulai()), new Label(ais.common.Common.getBahasaConfig(" s.d ")),
					perkuliahanSampai = new MyDatebox(perkuliahan.getPerkuliahanSampai()) }));

			EventListener masaPerkuliahanEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MasaPerkuliahan myMasaPerkuliahan = (MasaPerkuliahan) masaPerkuliahan
							.getAttribute("masaPerkuliahan");
					if (myMasaPerkuliahan != null) {
						perkuliahanDimulai.setValue(myMasaPerkuliahan.getMulai());
						perkuliahanSampai.setValue(myMasaPerkuliahan.getSampai());
					}

					perkuliahanDimulai.setDisabled(myMasaPerkuliahan != null);
					perkuliahanSampai.setDisabled(myMasaPerkuliahan != null);
				}
			};

			masaPerkuliahan.setEventListener(masaPerkuliahanEventListener);
			masaPerkuliahanEventListener.onEvent(null);

			final MyFormRow myrow = new MyFormRow();
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label());
			row.appendChild(merupakan_paralel = new MyCheckboxConfig("Merupakan jadwal perkuliahan paralel"));
			merupakan_paralel.setChecked(
					perkuliahan.getMerupakan_paralel() == null ? false : perkuliahan.getMerupakan_paralel());

			myrow.setVisible(false);
			myrow.setParent(rows);
			myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Paralel dari Perkuliahan (wajib diisi)")));
			myrow.appendChild(perkuliahan_paralel = new Combobox());
			perkuliahan_paralel.setWidth("90%");
			if (merupakan_paralel.isChecked()) {
				myrow.setVisible(true);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						generatePerkulihaanParalel(false);
						Common.selectComboItem(true, perkuliahan_paralel, perkuliahan.getPerkuliahan_paralel());
					}
				});
			}

			merupakan_paralel.addEventListener(Events.ON_CHECK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (perkuliahan.getId() != null && merupakan_paralel.isChecked()) {
						Session session = HibernateUtil.currentSession();
						Integer count = ((Number) session.createCriteria(Detailperkuliahan.class)
								.add(Restrictions.isNull("ikutiPerkuliahan"))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
								.uniqueResult()).intValue();
						if (!count.equals(0)) {
							merupakan_paralel.setChecked(false);
							MyMessageboxConfig.show(
									"Mohon maaf, jadwal perkuliahan yang sudah diambil oleh mahasiswa tidak dapat dijadikan jadwal paralel. Langkah yang dapat dilakukan: (1) transfer terlebih dahulu mahasiswa yang bersangkutan ke jadwal perkuliahan yang bukan paralel; (2) setelah itu, silakan Bapak/Ibu menjadikan jadwal ini sebagai jadwal paralel.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
					}

					if (merupakan_paralel.isChecked()) {
						myrow.setVisible(true);
						generatePerkulihaanParalel(false);
						Common.selectComboItem(true, perkuliahan_paralel, perkuliahan.getPerkuliahan_paralel());
					} else {
						myrow.setVisible(false);
						Common.clear(perkuliahan_paralel);
						perkuliahan_paralel.setSelectedItem(null);
					}

					if (perkuliahan_paralel.getSelectedItem() == null && !perkuliahan_paralel.getChildren().isEmpty()) {
						perkuliahan_paralel.setSelectedIndex(0);
					}

				}
			});

			eventListener.onEvent(null);
			perkuliahan_paralel.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(tampilkanMingguPerkuliahan.getNilai().equals(Konfigurasi.AKTIF));
			row.appendChild(new ais.ui.util.MyLabelConfig("Minggu 1"));
			row.appendChild(minggu1 = new MyCheckboxConfig());
			minggu1.setChecked(perkuliahan.getMinggu1());

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(tampilkanMingguPerkuliahan.getNilai().equals(Konfigurasi.AKTIF));
			row.appendChild(new ais.ui.util.MyLabelConfig("Minggu 2"));
			row.appendChild(minggu2 = new MyCheckboxConfig());
			minggu2.setChecked(perkuliahan.getMinggu2());

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(tampilkanMingguPerkuliahan.getNilai().equals(Konfigurasi.AKTIF));
			row.appendChild(new ais.ui.util.MyLabelConfig("Minggu 3"));
			row.appendChild(minggu3 = new MyCheckboxConfig());
			minggu3.setChecked(perkuliahan.getMinggu3());

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(tampilkanMingguPerkuliahan.getNilai().equals(Konfigurasi.AKTIF));
			row.appendChild(new ais.ui.util.MyLabelConfig("Minggu 4"));
			row.appendChild(minggu4 = new MyCheckboxConfig());
			minggu4.setChecked(perkuliahan.getMinggu4());

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(tampilkanMingguPerkuliahan.getNilai().equals(Konfigurasi.AKTIF));
			row.appendChild(new ais.ui.util.MyLabelConfig("Minggu 5"));
			row.appendChild(minggu5 = new MyCheckboxConfig());
			minggu5.setChecked(perkuliahan.getMinggu5());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Batas Minimal Persen Kehadiran"));
			row.appendChild(persenKehadiranDinilai0 = new MyDoublebox(perkuliahan.getPersenKehadiranDinilai0()));

			Common.initKeterangan(rows, "Isikan nilai 0 jika tidak ada batasan kehadiran mahasiswa");

			row = new MyFormRow();
			row.setVisible(Common.getKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF)
					.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Batas hari boleh melakukan presensi kehadiran"));
			row.appendChild(
					batasWaktuBolehAbsenKehadiran = new MyIntbox(perkuliahan.getBatasWaktuBolehAbsenKehadiran()));

			row = new MyFormRow();
			row.setParent(rows);
			row.setVisible(
					Common.bolehKonfigurasi("tampilkan_abaikan_waktu_bentrok_dengan_jadwal_lain"));
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(abaikanWaktuBentrokDenganJadwalLain = new MyCheckboxConfig(
					"Abaikan waktu jika bentrok dengan jadwal lain"));
			abaikanWaktuBentrokDenganJadwalLain.setChecked(perkuliahan.getAbaikanWaktuBentrokDenganJadwalLain());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(tampilkanSaatPengambilanKrs = new MyCheckboxConfig(
					"Merupakan penawaran rencana studi mahasiswa (KRS)"));
			tampilkanSaatPengambilanKrs.setChecked(perkuliahan.getTampilkanSaatPengambilanKrs());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(dosenBisaMerubahTanggalPerkuliahan = new MyCheckboxConfig(
					"Dosen boleh membuat agenda perkuliahan sendiri serta bisa mengubah tanggal dan waktu perkuliahan"));
			dosenBisaMerubahTanggalPerkuliahan.setChecked(perkuliahan.getDosenBisaMerubahTanggalPerkuliahan());

			boolean adaProsesVerifikasiNilai = Common.bolehKonfigurasi("ada_proses_verifikasi_penilaian_kepada_dosen", Konfigurasi.TIDAK_AKTIF);

			row = new MyFormRow();
			row.setVisible(adaProsesVerifikasiNilai);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(dosenBolehVerifikasiNilaiSendiri = new MyCheckboxConfig(
					"Dosen Boleh mem-verifikasi nilai sendiri"));
			dosenBolehVerifikasiNilaiSendiri.setChecked(perkuliahan.getDosenBolehVerifikasiNilaiSendiri());

			if (!adaProsesVerifikasiNilai) {
				perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(false);
			}

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(aktif = new MyCheckboxConfig(
					"Perkuliahan ini Aktif (Jika tidak aktif, semua mahasiwa yang mengambil KRS perkuliahan tidak bisa disetujui)"));
			aktif.setChecked(perkuliahan.getAktif());

			row = new MyFormRow();
			row.setVisible(adaProsesVerifikasiNilai);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(sembunyikanNilaiJikaBelumDiverifikasi = new MyCheckboxConfig(
					"Sembunyikan Nilai Jika Belum Diverifikasi"));
			sembunyikanNilaiJikaBelumDiverifikasi.setChecked(perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi());

			row = new MyFormRow();
			row.setVisible(adaProsesVerifikasiNilai);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(sembunyikanFormatPenilaian = new MyCheckboxConfig("Sembunyikan Ubah Format Penilaian"));
			sembunyikanFormatPenilaian.setChecked(perkuliahan.getSembunyikanFormatPenilaian());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(
					dosenBolehAbsenMenggunakanFoto = new MyCheckboxConfig("Dosen Diizinkan / Boleh Absen Online"));
			dosenBolehAbsenMenggunakanFoto.setChecked(perkuliahan.getDosenBolehAbsenMenggunakanFoto());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(mahasiswaBolehAbsenMenggunakanFoto = new MyCheckboxConfig(
					"Mahasiswa Diizinkan / Boleh Absen Online"));
			mahasiswaBolehAbsenMenggunakanFoto.setChecked(perkuliahan.getMahasiswaBolehAbsenMenggunakanFoto());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen = new MyCheckboxConfig(
					"Mahasiswa Diizinkan Boleh Absen Online ketika dosen telah absen"));
			mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen
					.setChecked(perkuliahan.getMahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(waktuPerkuliahanOnlineBebas = new MyCheckboxConfig(
					"Waktu perkuliahan online tidak ditentukan waktu atau bebas di klik kapan saja"));
			waktuPerkuliahanOnlineBebas.setChecked(perkuliahan.getWaktuPerkuliahanOnlineBebas());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(kehadiranDosenHarusDiinputSesuaiJadwal = new MyCheckboxConfig(
					"Kehadiran Dosen Harus Diinput Sesuai Jadwal"));
			kehadiranDosenHarusDiinputSesuaiJadwal.setChecked(perkuliahan.getKehadiranDosenHarusDiinputSesuaiJadwal());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(kehadiranMahasiswaHarusDiinputSesuaiJadwal = new MyCheckboxConfig(
					"Kehadiran Mahasiswa Harus Diinput Sesuai Jadwal"));
			kehadiranMahasiswaHarusDiinputSesuaiJadwal
					.setChecked(perkuliahan.getKehadiranMahasiswaHarusDiinputSesuaiJadwal());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(kehadiranDosenHarusDiinputDiIpYangDitentukan = new MyCheckboxConfig(
					"Kehadiran Dosen Harus Diinput Di Ip Ruangan atau Gedung"));
			kehadiranDosenHarusDiinputDiIpYangDitentukan
					.setChecked(perkuliahan.getKehadiranDosenHarusDiinputDiIpYangDitentukan());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(kehadiranMahasiswaHarusDiinputDiIpYangDitentukan = new MyCheckboxConfig(
					"Kehadiran Mahasiswa Harus Diinput Di Ip Ruangan atau Gedung"));
			kehadiranMahasiswaHarusDiinputDiIpYangDitentukan
					.setChecked(perkuliahan.getKehadiranMahasiswaHarusDiinputDiIpYangDitentukan());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(adminBolehMenginputKehadiranDiluarJadwalDanIp = new MyCheckboxConfig(
					"Admin Boleh Menginput Kehadiran Diluar Jadwal Dan Ip yang ditentukan"));
			adminBolehMenginputKehadiranDiluarJadwalDanIp
					.setChecked(perkuliahan.getAdminBolehMenginputKehadiranDiluarJadwalDanIp());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			row.appendChild(jumlahRencanaPertemuanMengikutiKurikulum = new MyCheckboxConfig(
					"Jumlah Rencana Perkuliahan Mengikuti Kurikulum"));
			jumlahRencanaPertemuanMengikutiKurikulum
					.setChecked(perkuliahan.getJumlahRencanaPertemuanMengikutiKurikulum());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Rencana Pertemuan Perkuliahan"));
			row.appendChild(jumlahMaksimalPertemuan = new MyIntbox(perkuliahan.getJumlahMaksimalPertemuan()));
			jumlahMaksimalPertemuan.setDisabled(jumlahRencanaPertemuanMengikutiKurikulum.isChecked());

			jumlahRencanaPertemuanMengikutiKurikulum.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jumlahMaksimalPertemuan.setDisabled(jumlahRencanaPertemuanMengikutiKurikulum.isChecked());
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
			row.appendChild(
					keterangan = new Textbox(perkuliahan.getKeterangan() == null ? "" : perkuliahan.getKeterangan()));
			keterangan.setWidth("90%");
			keterangan.setRows(3);

			row = new MyFormRow();
			row.setVisible(Common.getApakahAdminBolehAksesFeeder());
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
			row.appendChild(feeder = new Textbox(perkuliahan.getFeeder()));
			feeder.setWidth("90%");

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						try {
							onSearchDefaultListener.onSearchDefault(null);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/util/PenjadwalanUtil.java:1835");
						}
						addWindow.detach();
					}
				}
			});
			save.setParent(toolbar);

			MyToolbarbuttonConfig copy = new MyToolbarbuttonConfig("Copy ke prodi lain", "/img/svg/edit-copy.svg");
			copy.setVisible(perkuliahan.getId() != null);
			copy.setTooltiptext("Copy");
			copy.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();
					@SuppressWarnings("unchecked")
					List<Jurusan> jurusans = session.createCriteria(Jurusan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
					String warning = "";
					for (Jurusan jurusan : jurusans) {
						if (!jurusan.getId().equals(perkuliahan.getJurusan().getId())) {
							int count = ((Number) session.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(perkuliahan.getStatusSemesterPendek() == null
											? Restrictions.isNull("statusSemesterPendek")
											: Restrictions.eq("statusSemesterPendek",
													perkuliahan.getStatusSemesterPendek()))
									.add(Restrictions.sqlRestriction("true")).add(Restrictions.eq("jurusan", jurusan))
									.add(Restrictions.eq("program", perkuliahan.getProgram()))
									.add(Restrictions.eq("tahunAjaran", perkuliahan.getTahunAjaran()))
									.add(Restrictions.eq("semester", perkuliahan.getSemester()))
									.add(Restrictions.ilike("kelas", perkuliahan.getKelas(), MatchMode.EXACT))
									.createAlias("matakuliah", "matakuliah")
									.add(Restrictions.ilike("matakuliah.nama", perkuliahan.getMatakuliah().getNama(),
											MatchMode.EXACT))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							if (count == 0) {

								KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
										.createCriteria(KurikulumPunyaMatakuliah.class)
										.add(Restrictions
												.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.createAlias("matakuliah", "matakuliah")
										.add(Restrictions.ilike("matakuliah.nama",
												perkuliahan.getMatakuliah().getNama(), MatchMode.EXACT))
										.createAlias("kurikulum", "kurikulum")
										.add(Restrictions.eq("kurikulum.jurusan", jurusan))
										.addOrder(Order.desc("kurikulum.tahun")).setMaxResults(1).uniqueResult();
								if (kurikulumPunyaMatakuliah != null) {
									Perkuliahan p = (Perkuliahan) perkuliahan.clone();
									p.setId(null);
									p.setAbaikanWaktuBentrokDenganJadwalLain(true);
									p.setJurusan(jurusan);
									p.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
									p.setMatakuliah(kurikulumPunyaMatakuliah.getMatakuliah());
									p.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());
									session.save(p);
								} else {
									warning += "Matakuliah \"" + perkuliahan.getMatakuliah().getNama()
											+ "\" tidak ditemukan di kurikulum prodi " + jurusan.getNama()
											+ ".\nSehingga tidak bisa meng-copy data jadwal matakuliah tersebut ke prodi  "
											+ jurusan.getNama() + ".\n\n";
								}
							} else {
								warning += "Jadwal matakuliah \"" + perkuliahan.getMatakuliah().getNama() + "\" kelas "
										+ perkuliahan.getKelas() + " program " + perkuliahan.getProgram()
										+ " sudah ada di prodi  " + jurusan.getNama() + ".\n\n";
							}
						}
					}

					if (!warning.isEmpty()) {
						MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}

					try {
						onSearchDefaultListener.onSearchDefault(null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/util/PenjadwalanUtil.java:1914");
					}
					addWindow.detach();
				}
			});
			copy.setParent(toolbar);

			copy = new MyToolbarbuttonConfig("Copy ke kelas lain", "/img/svg/edit-copy.svg");
			copy.setVisible(perkuliahan.getId() != null);
			copy.setTooltiptext("Copy");
			copy.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataKelasBanyak ambilDataKelasBanyak = new AmbilDataKelasBanyak(new ArrayList<Kelas>());
					ambilDataKelasBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilDataKelasBanyak.setHeight("95%");
					ambilDataKelasBanyak.setWidth("700px");

					ambilDataKelasBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							@SuppressWarnings("unchecked")
							List<Kelas> kelases = (List<Kelas>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							String warning = "";
							for (Kelas kelas : kelases) {
								if (kelas.getNama() != null
										&& !kelas.getNama().equalsIgnoreCase(perkuliahan.getKelas())) {
									int count = ((Number) session.createCriteria(Perkuliahan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(perkuliahan.getStatusSemesterPendek() == null
													? Restrictions.isNull("statusSemesterPendek")
													: Restrictions.eq("statusSemesterPendek",
															perkuliahan.getStatusSemesterPendek()))
											.add(Restrictions.sqlRestriction("true"))
											.add(Restrictions.eq("jurusan", perkuliahan.getJurusan()))
											.add(Restrictions.eq("program", perkuliahan.getProgram()))
											.add(Restrictions.eq("tahunAjaran", perkuliahan.getTahunAjaran()))
											.add(Restrictions.eq("semester", perkuliahan.getSemester()))
											.add(Restrictions.ilike("kelas", kelas.getNama(), MatchMode.EXACT))
											.createAlias("matakuliah", "matakuliah")
											.add(Restrictions.ilike("matakuliah.nama",
													perkuliahan.getMatakuliah().getNama(), MatchMode.EXACT))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count == 0) {
										Perkuliahan p = (Perkuliahan) perkuliahan.clone();
										p.setId(null);
										p.setAbaikanWaktuBentrokDenganJadwalLain(true);
										p.setKelas(kelas.getNama());
										p.setKelasref(kelas);
										session.save(p);
									} else {
										warning += "Jadwal matakuliah \"" + perkuliahan.getMatakuliah().getNama()
												+ "\" program " + perkuliahan.getProgram() + " prodi "
												+ perkuliahan.getJurusan().getNama() + " sudah ada di kelas "
												+ kelas.getNama() + ".\n\n";
									}
								}
							}

							if (!warning.isEmpty()) {
								MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK,
										MyMessageboxConfig.EXCLAMATION);
							}

							onSearchDefaultListener.onSearchDefault(null);
							addWindow.detach();

						}
					});

					ambilDataKelasBanyak.onModal();
				}
			});
			copy.setParent(toolbar);

			save = new MyToolbarbuttonConfig("Kurikulum", "/img/svg/edit-box-line.svg");
			save.setVisible(perkuliahan.getId() != null);
			save.setTooltiptext("Kurikulum");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.detach();
					initJadwalKurikulum(perkuliahan, semesterPendek, ekstrakurikuler, merupakanRemedial);
				}
			});
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	/**
	 * Mengisi ulang kombo {@link #perkuliahan_paralel} dengan daftar jadwal {@link Perkuliahan}
	 * lain yang sekelas paralel (kandidat perkuliahan induk untuk fitur "merupakan paralel") —
	 * dicari berdasarkan kecocokan Tahun Akademik, Program, Jurusan/Fakultas, Semester, Mata
	 * Kuliah, dan nama Kelas, sambil mengecualikan jadwal itu sendiri (kecuali saat menyalin/
	 * {@code isCopy}) dan jadwal yang sudah ditandai paralel. Menampilkan peringatan dan berhenti
	 * lebih awal apabila field prasyarat (Tahun Akademik, Program, Jurusan, Semester, Mata Kuliah)
	 * belum terisi.
	 *
	 * @param isCopy {@code true} bila dipanggil dalam konteks menyalin jadwal (jadwal saat ini
	 *               tidak dikecualikan dari daftar kandidat)
	 * @throws Exception diteruskan dari kegagalan akses database
	 */
	protected void generatePerkulihaanParalel(Boolean isCopy) throws Exception {
		Common.clear(perkuliahan_paralel);

		if (tahunAjaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Akademik wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Tahun Akademik pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Program wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Program pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.showFormat("Mohon maaf, {V1} wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu data pada kolom tersebut; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, Common.getBahasaConfig("Jurusan"));
			return;
		}
		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Semester wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Semester pada kolom tersebut; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (matakuliah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Mata Kuliah wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Mata Kuliah pada kolom tersebut; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		List<Perkuliahan> perkuliahan = HibernateUtil.currentSession().createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("merupakanRemedial", merupakanRemedial)).addOrder(Order.desc("id"))
				.add(this.perkuliahan == null || this.perkuliahan.getId() == null || isCopy
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.perkuliahan.getId()))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.add(Restrictions.ilike("kelas", kelas.getValue().trim(), MatchMode.EXACT))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(Restrictions.eq("matakuliah", matakuliah.getSelectedItem().getValue()))

				.add(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
				.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)).list();

		for (Perkuliahan o : perkuliahan) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
					+ o.getMatakuliah().getNama() + " (" + o.getId() + ")");
			comboitem.setValue(o);

			String deskripsi = "Dosen: " + (o.getDosen1() == null ? "" : o.getDosen1().getNama()) + ",Smt: "
					+ (o.getSemester() + (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
					+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
					+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai();

			comboitem.setDescription(deskripsi);
			perkuliahan_paralel.appendChild(comboitem);
		}
		perkuliahan_paralel.setReadonly(true);
	}

	/**
	 * Memvalidasi seluruh isian form lalu menyimpan entitas {@link #perkuliahan}. Validasi mencakup
	 * kelengkapan field wajib (Tahun Akademik, Program, Jurusan, Semester, Kelas, Mata Kuliah,
	 * opsional Masa Perkuliahan bila konfigurasi mewajibkannya), status aktif penjadwalan untuk
	 * kombinasi Tahun Akademik/Semester/Semester-Pendek terkait (via
	 * {@code CommonPenjadwalan#apakahPenjadwalanTidakAktif}), kelengkapan Dosen Utama, bentrok
	 * jadwal dosen (via {@link Perkuliahan#checkDosen}), serta Waktu Mulai bila jadwal tidak
	 * ditandai "tanpa jadwal perkuliahan". Setiap kegagalan validasi menampilkan
	 * {@link MyMessageboxConfig} dan mengembalikan {@code false} tanpa menyimpan apa pun.
	 *
	 * @param event event ZK yang memicu penyimpanan (mis. klik tombol Simpan)
	 * @return {@code true} bila validasi lolos dan data berhasil disimpan; {@code false} bila
	 *         validasi gagal pada langkah mana pun
	 * @throws Exception diteruskan dari kegagalan akses database saat proses simpan
	 */
	public boolean onSave(Event event) throws Exception {

		if (tahunAjaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Akademik wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Tahun Akademik pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (!merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Program wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Program pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
			if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.showFormat("Mohon maaf, {V1} wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu data pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, Common.getBahasaConfig("Jurusan"));
				return false;
			}

			if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Semester wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Semester pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
			if (kelas.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Mohon maaf, Kelas wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) isi terlebih dahulu nama Kelas pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}
		if (matakuliah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Mata Kuliah wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Mata Kuliah pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (Common.bolehKonfigurasi("masa_perkuliahan_di_jadwal_perkuliahan_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
			if (masaPerkuliahan.getAttribute("masaPerkuliahan") == null) {
				MyMessageboxConfig.show("Mohon maaf, Masa Perkuliahan wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Masa Perkuliahan pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		String ta = (String) tahunAjaran.getSelectedItem().getValue();
		// SP-aware: bila "Semester Periode" = Semester Pendek (SP), cek konfigurasi PENJADWALAN_SP dan
		// tampilkan label "SP" pada peringatan (bukan Ganjil/Genap) — samakan dengan proses simpan versi SP.
		Object nilaiPeriodeCek = ganjilGenap == null || ganjilGenap.getSelectedItem() == null ? null
				: ganjilGenap.getSelectedItem().getValue();
		boolean cekSp = Perkuliahan.SP.equals(nilaiPeriodeCek);
		Integer semesterPendekCek = cekSp ? Perkuliahan.SEMESTER_PENDEK : semesterPendek;
		String sem = cekSp ? Perkuliahan.SP
				: (((Integer) semester.getSelectedItem().getValue()) % 2 == 0 ? Perkuliahan.GENAP
						: Perkuliahan.GANJIL);

		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendekCek, perkuliahan)) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, jadwal belum dapat disimpan karena penjadwalan untuk Tahun Akademik \"{V1}\" "
							+ "semester \"{V2}\" saat ini BELUM diaktifkan. Langkah yang dapat Bapak/Ibu lakukan: "
							+ "(1) pastikan pilihan Tahun Akademik dan Jenis Semester pada filter telah sesuai; "
							+ "(2) mohon meminta Administrator untuk mengaktifkan penjadwalan periode tersebut melalui "
							+ "tombol \"Aktif/Non-aktifkan Penjadwalan\" pada layar Jadwal Perkuliahan, atau melalui menu "
							+ "Konfigurasi; (3) setelah penjadwalan diaktifkan, silakan simpan kembali data ini. "
							+ "Terima kasih atas pengertian Bapak/Ibu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
			return false;
		}

		if (rowdosen1.isVisible() && dosen1.getAttribute("myValue") == null && !merupakan_tanpa_dosen.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Dosen Utama wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Dosen Utama pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (!Perkuliahan.checkDosen(tahunAjaran, semester, matakuliah, dosen1, dosen2, dosen3, dosen4, dosen5, dosen6,
				dosen7, dosen8, dosen9, dosen10, semesterPendek, perkuliahan.getId())) {
			return false;
		}
		if (waktuMulai.getValue() == null && !merupakan_tanpa_jadwal_perkuliahan.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Waktu Mulai wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) isi terlebih dahulu Waktu Mulai pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (waktuSelesai.getValue() == null && !merupakan_tanpa_jadwal_perkuliahan.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Waktu Selesai wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) isi terlebih dahulu Waktu Selesai pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (hari.getSelectedItem() == null && !merupakan_tanpa_jadwal_perkuliahan.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Hari wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Hari pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kapasitasKelas.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kapasitas Kelas wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) isi terlebih dahulu Kapasitas Kelas pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (ruang.getAttribute("ruang") == null && !merupakan_tanpa_ruangan.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Ruang wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Ruang pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Ruang rng = (Ruang) ruang.getAttribute("ruang");
		if (rng != null && rng.getKapasitasRuangan() != null && kapasitasKelas.getValue() != null
				&& kapasitasKelas.getValue().intValue() > rng.getKapasitasRuangan()) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, kapasitas ruangan yang dipilih hanya sebesar {V1}, sedangkan kapasitas kelas yang Bapak/Ibu masukkan adalah {V2}. Kapasitas kelas tidak boleh melebihi kapasitas ruangan. Langkah yang dapat dilakukan: (1) kurangi kapasitas kelas hingga sama dengan atau kurang dari kapasitas ruangan; (2) atau pilih ruangan lain yang memiliki kapasitas lebih besar.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, rng.getKapasitasRuangan(), kapasitasKelas.getValue().intValue());
			return false;
		}

		if (merupakan_paralel.isChecked() && perkuliahan_paralel.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, untuk membuat jadwal perkuliahan paralel Bapak/Ibu harus memilih jadwal perkuliahan utama terlebih dahulu. Langkah yang dapat dilakukan: (1) apabila jadwal perkuliahan utama belum tersedia, buat terlebih dahulu jadwal perkuliahan non-paralel yang baru; (2) pilih jadwal perkuliahan utama tersebut sebagai acuan; (3) kemudian hubungkan ke jadwal perkuliahan paralel yang akan Bapak/Ibu buat.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Double waktuMulaiD = !waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
				: Double.parseDouble(Common.timeFormat2.get().format(waktuMulai.getValue())) + 0.01;
		Double waktuSelesaiD = !waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
				: Double.parseDouble(Common.timeFormat2.get().format(waktuSelesai.getValue())) - 0.01;

		int s = (Integer) semester.getSelectedItem().getValue();

		if (!abaikanWaktuBentrokDenganJadwalLain.isChecked() && !merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			if (!merupakan_tanpa_jadwal_perkuliahan.isChecked()) {
				if (Common.checkKelasJadwalPerkuliahan(perkuliahan.getId(),
						(Jurusan) jurusan.getSelectedItem().getValue(), (String) program.getSelectedItem().getValue(),
						!hari.isVisible() || hari.getSelectedItem() == null ? null
								: hari.getSelectedItem().getValue().toString(),
						waktuMulaiD, waktuSelesaiD, tahunAjaran.getSelectedItem().getValue().toString(),
						s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, kelas.getValue().trim(),
						(Integer) semester.getSelectedItem().getValue(), null, semesterPendek, minggu1.isChecked(),
						minggu2.isChecked(), minggu3.isChecked(), minggu4.isChecked(), minggu5.isChecked(),
						perkuliahanDimulai.getValue(), perkuliahanSampai.getValue(),
						(Matakuliah) matakuliah.getSelectedItem().getValue(), rng) != null) {
					return false;
				}
			}
		}

		if (!abaikanWaktuBentrokDenganJadwalLain.isChecked() && !merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			if (!merupakan_tanpa_ruangan.isChecked() && !merupakan_tanpa_jadwal_perkuliahan.isChecked()) {

				if (Common.checkJadwalRuangPerkuliahan(perkuliahan.getId(),
						!ruang.isVisible() ? null : (Ruang) ruang.getAttribute("ruang"),
						!hari.isVisible() || hari.getSelectedItem() == null ? null
								: hari.getSelectedItem().getValue().toString(),
						waktuMulaiD, waktuSelesaiD, tahunAjaran.getSelectedItem().getValue().toString(),
						s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, null, semesterPendek, minggu1.isChecked(),
						minggu2.isChecked(), minggu3.isChecked(), minggu4.isChecked(), minggu5.isChecked(),
						perkuliahanDimulai.getValue(), perkuliahanSampai.getValue()) != null) {
					return false;
				}
			}
		}

		if (!abaikanWaktuBentrokDenganJadwalLain.isChecked() && !merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			if (!merupakan_tanpa_dosen.isChecked() && !merupakan_tanpa_jadwal_perkuliahan.isChecked()) {

				if (Common.checkJadwalDosen(perkuliahan.getId(),
						!hari.isVisible() || hari.getSelectedItem() == null ? null
								: hari.getSelectedItem().getValue().toString(),
						waktuMulaiD, waktuSelesaiD, !dosen1.isVisible() ? null : (Dosen) dosen1.getAttribute("myValue"),
						tahunAjaran.getSelectedItem().getValue().toString(),
						s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
						(Jurusan) jurusan.getSelectedItem().getValue(),
						(Matakuliah) matakuliah.getSelectedItem().getValue(), kelas.getValue().trim(), null,
						semesterPendek, minggu1.isChecked(), minggu2.isChecked(), minggu3.isChecked(),
						minggu4.isChecked(), minggu5.isChecked(), perkuliahanDimulai.getValue(),
						perkuliahanSampai.getValue()) != null) {
					return false;
				}
			}
		}

		if (perkuliahanDimulai.getValue() != null && perkuliahanSampai.getValue() != null
				&& perkuliahanDimulai.getValue().after(perkuliahanSampai.getValue())) {
			MyMessageboxConfig.show("Mohon maaf, tanggal mulai tidak boleh lebih besar daripada tanggal sampai. Langkah yang dapat dilakukan: (1) pastikan tanggal mulai lebih awal atau sama dengan tanggal sampai; (2) perbaiki isian tanggal, kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		PerkuliahanDao perkuliahanDao = DaoFactory.getInstance().getPerkuliahanDao();
		Perkuliahan perkuliahanParalel = (Perkuliahan) (perkuliahan_paralel.getSelectedItem() == null ? null
				: perkuliahan_paralel.getSelectedItem().getValue());

		if (perkuliahan.getId() != null) {
			if (perkuliahanParalel != null && perkuliahanParalel.getId() != null) {
				if (perkuliahan.getId().equals(perkuliahanParalel.getId())) {
					MyMessageboxConfig.show("Mohon maaf, sebuah jadwal perkuliahan tidak dapat dijadikan paralel terhadap dirinya sendiri. Langkah yang dapat dilakukan: (1) pilih jadwal perkuliahan utama yang berbeda sebagai acuan paralel; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
			perkuliahan = perkuliahanDao.load(perkuliahan.getId());
		}

		if (!abaikanWaktuBentrokDenganJadwalLain.isChecked() && !merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			if (!merupakan_paralel.isChecked()) {
				if (Common.checkMatakuliahKesamaanBukanParalel(perkuliahan,
						(Jurusan) jurusan.getSelectedItem().getValue(), kelas.getValue().trim(),
						(Matakuliah) matakuliah.getSelectedItem().getValue(), s,
						tahunAjaran.getSelectedItem().getValue().toString(),
						(String) program.getSelectedItem().getValue(), null, semesterPendek, minggu1.isChecked(),
						minggu2.isChecked(), minggu3.isChecked(), minggu4.isChecked(), minggu5.isChecked(),
						perkuliahanDimulai.getValue(), perkuliahanSampai.getValue(), merupakanRemedial) != null) {
					return false;
				}
			}
		}

		perkuliahan.setAmbilMkDiluarSemesterKurikulum(ambilMkDiluarSemesterKurikulum.isChecked());
		perkuliahan.setTerdapatKegiatanPraktek(terdapatKegiatanPraktek.isChecked());
		perkuliahan.setMerupakanTeamTeaching(merupakanTeamTeaching.isChecked());
		perkuliahan.setKeteranganJadwal(keteranganJadwal.getValue());
		perkuliahan.setJumlahRencanaPertemuanMengikutiKurikulum(jumlahRencanaPertemuanMengikutiKurikulum.isChecked());
		perkuliahan.setMerupakanRemedial(merupakanRemedial);
		perkuliahan.setJumlahMaksimalPertemuan(jumlahMaksimalPertemuan.getValue());
		perkuliahan.setDosenBisaMerubahTanggalPerkuliahan(dosenBisaMerubahTanggalPerkuliahan.isChecked());
		perkuliahan.setKeterangan(keterangan.getValue());
		perkuliahan.setAbaikanWaktuBentrokDenganJadwalLain(abaikanWaktuBentrokDenganJadwalLain.isChecked());
		perkuliahan.setTampilkanSaatPengambilanKrs(tampilkanSaatPengambilanKrs.isChecked());
		perkuliahan.setMasaPerkuliahan((MasaPerkuliahan) masaPerkuliahan.getAttribute("masaPerkuliahan"));
		perkuliahan.setPerkuliahanDimulai(perkuliahanDimulai.getValue());
		perkuliahan.setPerkuliahanSampai(perkuliahanSampai.getValue());

		perkuliahan.setMinggu1(minggu1.isChecked());
		perkuliahan.setMinggu2(minggu2.isChecked());
		perkuliahan.setMinggu3(minggu3.isChecked());
		perkuliahan.setMinggu4(minggu4.isChecked());
		perkuliahan.setMinggu5(minggu5.isChecked());

		perkuliahan.setJamPerkuliahan((JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan"));
		perkuliahan.setKapasitasKelas(kapasitasKelas.getValue().intValue());
		perkuliahan.setMerupakan_tanpa_dosen(merupakan_tanpa_dosen.isChecked());
		perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(merupakan_tanpa_jadwal_perkuliahan.isChecked());
		perkuliahan.setMerupakan_tanpa_ruangan(merupakan_tanpa_ruangan.isChecked());
		perkuliahan.setPerkuliahan_paralel(perkuliahanParalel);
		perkuliahan.setMerupakan_paralel(merupakan_paralel.isChecked());

		perkuliahan.setWaktu((String) (!waktu.isVisible() ? null
				: waktu.getSelectedItem() == null ? null : waktu.getSelectedItem().getValue()));
		perkuliahan.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
				: Common.timeFormat2.get().format(waktuMulai.getValue()));
		perkuliahan.setWaktuSelesai(!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
				: Common.timeFormat2.get().format(waktuSelesai.getValue()));
		perkuliahan.setHari(!hari.isVisible() || hari.getSelectedItem() == null ? null
				: hari.getSelectedItem().getValue().toString());

		perkuliahan.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));

		perkuliahan.setJumlahDosen(
				(Integer) (jumlahDosen.getSelectedItem() == null ? 1 : jumlahDosen.getSelectedItem().getValue()));
		perkuliahan.setDosen1((Dosen) (rowdosen1.isVisible() ? dosen1.getAttribute("myValue") : null));
		perkuliahan.setDosen2((Dosen) (rowdosen2.isVisible() ? dosen2.getAttribute("myValue") : null));
		perkuliahan.setDosen3((Dosen) (rowdosen3.isVisible() ? dosen3.getAttribute("myValue") : null));
		perkuliahan.setDosen4((Dosen) (rowdosen4.isVisible() ? dosen4.getAttribute("myValue") : null));
		perkuliahan.setDosen5((Dosen) (rowdosen5.isVisible() ? dosen5.getAttribute("myValue") : null));
		perkuliahan.setDosen6((Dosen) (rowdosen6.isVisible() ? dosen6.getAttribute("myValue") : null));
		perkuliahan.setDosen7((Dosen) (rowdosen7.isVisible() ? dosen7.getAttribute("myValue") : null));
		perkuliahan.setDosen8((Dosen) (rowdosen8.isVisible() ? dosen8.getAttribute("myValue") : null));
		perkuliahan.setDosen9((Dosen) (rowdosen9.isVisible() ? dosen9.getAttribute("myValue") : null));
		perkuliahan.setDosen10((Dosen) (rowdosen10.isVisible() ? dosen10.getAttribute("myValue") : null));

		perkuliahan.setWaktuPerkuliahanOnlineBebas(waktuPerkuliahanOnlineBebas.isChecked());

		perkuliahan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		perkuliahan.setMatakuliah(
				(Matakuliah) (matakuliah.getSelectedItem() == null ? null : matakuliah.getSelectedItem().getValue()));

		perkuliahan.setKelas(kelas.isVisible() ? kelas.getValue().trim() : "");
		perkuliahan.setKelasref((Kelas) kelas.getAttribute("kelas"));
		perkuliahan.setRuang((Ruang) (ruang.isVisible() ? ruang.getAttribute("ruang") : null));

		perkuliahan.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		perkuliahan.setProgram(
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "Reguler"
						: program.getSelectedItem().getValue().toString());
		perkuliahan.setKurikulum(
				(Kurikulum) (kurikulum.getSelectedItem() == null ? null : kurikulum.getSelectedItem().getValue()));

		// "Semester Periode" = "Semester Pendek (SP)" → tandai perkuliahan sebagai Semester Pendek,
		// SAMAKAN dengan cara simpan tab "SP" lama (statusSemesterPendek = SEMESTER_PENDEK). Selain itu
		// pakai nilai field semesterPendek (null pada form tambah/edit biasa = bukan SP).
		final Object nilaiSemesterPeriode = ganjilGenap.getSelectedItem() == null ? null
				: ganjilGenap.getSelectedItem().getValue();
		final boolean pilihSemesterPendek = Perkuliahan.SP.equals(nilaiSemesterPeriode);
		perkuliahan.setStatusSemesterPendek(pilihSemesterPendek ? Perkuliahan.SEMESTER_PENDEK : semesterPendek);

		perkuliahan.setKehadiranDosenHarusDiinputSesuaiJadwal(kehadiranDosenHarusDiinputSesuaiJadwal.isChecked());
		perkuliahan.setDosenBolehVerifikasiNilaiSendiri(dosenBolehVerifikasiNilaiSendiri.isChecked());
		perkuliahan.setKehadiranDosenHarusDiinputDiIpYangDitentukan(
				kehadiranDosenHarusDiinputDiIpYangDitentukan.isChecked());
		perkuliahan
				.setKehadiranMahasiswaHarusDiinputSesuaiJadwal(kehadiranMahasiswaHarusDiinputSesuaiJadwal.isChecked());
		perkuliahan.setKehadiranMahasiswaHarusDiinputDiIpYangDitentukan(
				kehadiranMahasiswaHarusDiinputDiIpYangDitentukan.isChecked());
		perkuliahan.setAdminBolehMenginputKehadiranDiluarJadwalDanIp(
				adminBolehMenginputKehadiranDiluarJadwalDanIp.isChecked());

		perkuliahan.setFeeder(feeder.getValue().trim());

		perkuliahan.setMerupakanPraPerkuliahan(merupakanPraPerkuliahan);
		perkuliahan.setMerupakanPerkuliahanUmum(merupakanPerkuliahanUmum);

		perkuliahan.setMahasiswaBolehAbsenMenggunakanFoto(mahasiswaBolehAbsenMenggunakanFoto.isChecked());
		perkuliahan.setDosenBolehAbsenMenggunakanFoto(dosenBolehAbsenMenggunakanFoto.isChecked());
		perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(sembunyikanNilaiJikaBelumDiverifikasi.isChecked());
		perkuliahan.setSembunyikanFormatPenilaian(sembunyikanFormatPenilaian.isChecked());
		perkuliahan.setBatasWaktuBolehAbsenKehadiran(batasWaktuBolehAbsenKehadiran.getValue());
		perkuliahan.setPersenKehadiranDinilai0(persenKehadiranDinilai0.getValue());

		perkuliahan.setLingkup(lingkup.getSelectedItem() == null || lingkup.getSelectedItem().getValue() == null ? null
				: lingkup.getSelectedItem().getValue().toString());

		perkuliahan.setMahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen(
				mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen.isChecked());

		perkuliahan.setMode(mode.getSelectedItem() == null || mode.getSelectedItem().getValue() == null ? null
				: mode.getSelectedItem().getValue().toString());
		perkuliahan.setAktif(aktif.isChecked());
		List<Perkuliahan> perkuliahansParalel = perkuliahan.getId() == null ? null
				: perkuliahan.ambilParalelPerkuliahan();
		Session session = perkuliahanDao.getCurrentSession();
		if (kelasref.isChecked()) {
			Kelas kls = (Kelas) kelas.getAttribute("kelas");
			if (kls == null) {
				kls = (Kelas) session.createCriteria(Kelas.class).add(Restrictions.ilike("nama", kelas.getValue()))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.setMaxResults(1).uniqueResult();
			}
			perkuliahan.setKelasref(kls);
		} else {
			perkuliahan.setKelasref(null);
		}

		if (!merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.populateKurikulumPunyaMatakuliah();
			if (kurikulumPunyaMatakuliah != null && tahap != null) {
				kurikulumPunyaMatakuliah.setTahap(
						(Integer) (tahap.getSelectedItem() == null ? null : tahap.getSelectedItem().getValue()));
				Common.refreshSaveOrUpdate(kurikulumPunyaMatakuliah);
			}
		}

		// Bila "Semester Pendek (SP)" dipilih, kolom ganjilGenap DIKOSONGKAN (SP lintas ganjil/genap —
		// nilai combo "Semester Pendek" bukan nilai ganjilGenap yang valid). Selain itu simpan apa adanya
		// (Ganjil/Genap, atau null untuk "Ikut Smt Kurikulum").
		perkuliahan.setGanjilGenap(pilihSemesterPendek ? null : (String) nilaiSemesterPeriode);

		Common.refreshSaveOrUpdate(session, perkuliahan);

		if (perkuliahansParalel != null && !perkuliahansParalel.isEmpty()) {
			for (Perkuliahan paralel : perkuliahansParalel) {
				if (paralel != null && paralel.getId() != null && !paralel.getId().equals(perkuliahan.getId())) {
					Perkuliahan paralelAktif = (Perkuliahan) session.get(Perkuliahan.class, paralel.getId());
					if (paralelAktif != null) {
						paralelAktif.setKelas(perkuliahan.getKelas());
						paralelAktif.setKelasref(perkuliahan.getKelasref());
						Common.refreshUpdate(session, paralelAktif);
					}
				}
			}
		}

		if (!merupakanPraPerkuliahan && !merupakanPerkuliahanUmum && matakuliahKurikulumDetailHelper != null) {
			matakuliahKurikulumDetailHelper.simpan(perkuliahan);
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	/**
	 * Varian pembangun jendela jadwal yang lebih ringkas, dipakai ketika jadwal dibuat langsung
	 * mengikuti struktur Kurikulum (mis. dari layar generate jadwal massal berbasis kurikulum).
	 * Berbeda dari {@link #init}, jendela ini tidak memiliki tab "Rencana Pembelajaran/Silabus" dan
	 * tata letak grid-nya disesuaikan untuk pengisian cepat kolom-kolom jadwal.
	 *
	 * @param perkuliahan     entitas jadwal yang akan dibuat/diubah
	 * @param semesterPendek  penanda konteks Semester Pendek, boleh {@code null}
	 * @param ekstrakurikuler penanda konteks ekstrakurikuler, boleh {@code null}
	 * @param merupakanRemedial penanda apakah jadwal ini remedial
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen ZK atau akses database
	 */
	public void initJadwalKurikulum(final Perkuliahan perkuliahan, final Integer semesterPendek,
			final Integer ekstrakurikuler, final Boolean merupakanRemedial) throws Exception {
		final MyWindow window = new MyWindow("Jadwal Perkuliahan", "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("98%");
		window.setHeight("95%");

		try {
			this.semesterPendek = semesterPendek;
			this.merupakanRemedial = merupakanRemedial;
			kelas = new AmbilDataKelasBanbox();
			Tbmuser tbmuser = Common.getCurrentUser();

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
			Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
					perkuliahan.getTahunAjaran());

			Hbox hb = new Hbox();
			row.appendChild(hb);

			hb.appendChild(tahunAjaran);
			tahunAjaran.setCols(10);

			ganjilGenap = new Combobox();
			ganjilGenap.setReadonly(true);
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			ganjilGenap.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			ganjilGenap.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Ikut Smt Kurikulum");
			comboitem.setValue(null);
			ganjilGenap.appendChild(comboitem);

			// Pilihan "Semester Pendek (SP)" — kini JUGA tersedia pada "Tambah Berdasar Kurikulum". Bila
			// dipilih, saat simpan perkuliahan ditandai Semester Pendek (statusSemesterPendek=SEMESTER_PENDEK)
			// & ganjilGenap dikosongkan (lihat blok simpan initJadwalKurikulum). Nilai combo = Perkuliahan.SP.
			MyComboitemConfig comboitemSpKurikulum = new MyComboitemConfig();
			comboitemSpKurikulum.setLabel("Semester Pendek (SP)");
			comboitemSpKurikulum.setValue(Perkuliahan.SP);
			ganjilGenap.appendChild(comboitemSpKurikulum);

			// Default mengikuti perkuliahan: SP bila entitas/field menandakan Semester Pendek; selain itu
			// ganjilGenap perkuliahan bila terisi; jika tidak, "Ikut Smt Kurikulum".
			if ((perkuliahan.getStatusSemesterPendek() != null
					&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK))
					|| (semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK))) {
				ganjilGenap.setSelectedItem(comboitemSpKurikulum);
			} else if (perkuliahan.getGanjilGenap() != null && perkuliahan.getGanjilGenap().trim().length() > 0) {
				Common.selectComboItem(true, ganjilGenap, perkuliahan.getGanjilGenap());
			} else {
				ganjilGenap.setSelectedItem(comboitem);
			}

			hb.appendChild(ganjilGenap);
			ganjilGenap.setCols(10);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
			Common.selectComboItem(true, program,
					perkuliahan.getProgram() == null
							? (tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama())
							: perkuliahan.getProgram());
			row.appendChild(program);
			program.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
			Hbox hboxKelas = new Hbox();
			row.appendChild(hboxKelas);
			hboxKelas.appendChild(kelas);
			kelas.setValue(perkuliahan.getKelas() == null ? "A" : perkuliahan.getKelas());
			kelas.setCols(4);
			kelas.setAttribute("kelas", perkuliahan.getKelasref());

			kelasref = new MyCheckboxConfig("Selalu sama dengan master data kelas");
			hboxKelas.appendChild(kelasref);
			kelasref.setChecked(perkuliahan.getId() == null || perkuliahan.getKelasref() != null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			Common.selectComboItem(true, fakultas, perkuliahan.getJurusan() == null ? tbmuser.ambilFakultas()
					: perkuliahan.getJurusan().getFakultas());
			row.appendChild(fakultas);
			fakultas.setWidth("90%");

			if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			Common.selectComboItem(true, jurusan,
					perkuliahan.getJurusan() == null ? tbmuser.ambilJurusan() : perkuliahan.getJurusan());
			row.appendChild(jurusan);
			jurusan.setWidth("90%");

			fakultas.setDisabled(tbmuser.ambilFakultas() != null);
			jurusan.setDisabled(tbmuser.ambilJurusan() != null);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
			row.appendChild(semester);
			Common.selectComboItem(true, semester, perkuliahan.getSemester());
			semester.setWidth("90%");
			semester.setReadonly(true);

			row = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(row, "1,1,4");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
			row.appendChild(kurikulum = new Combobox());
			kurikulum.setWidth("90%");
			kurikulum.setReadonly(true);

			Hbox hbox = new Hbox();
			row.appendChild(hbox);

			hbox.appendChild(abaikanWaktuBentrokDenganJadwalLain = new MyCheckboxConfig(
					"Abaikan waktu jika bentrok dengan jadwal lain"));
			abaikanWaktuBentrokDenganJadwalLain.setChecked(false);
			abaikanWaktuBentrokDenganJadwalLain.setVisible(
					Common.bolehKonfigurasi("tampilkan_abaikan_waktu_bentrok_dengan_jadwal_lain"));

			hbox.appendChild(tampilkanSaatPengambilanKrs = new MyCheckboxConfig(
					"Merupakan penawaran rencana studi mahasiswa (KRS)"));
			tampilkanSaatPengambilanKrs.setChecked(true);

			hbox.appendChild(dosenBisaMerubahTanggalPerkuliahan = new MyCheckboxConfig(
					"Dosen bisa mengubah tanggal dan waktu perkuliahan"));
			dosenBisaMerubahTanggalPerkuliahan.setChecked(
					Common.bolehKonfigurasi("secara_default_dosen_bisa_merubah_tanggal_perkuliahan"));

			row = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(row, "6");
			row.setParent(rows);

			MyGrid subGrid = new MyGrid();
			subGrid.setParent(row);

			columns = new Columns();
			columns.setParent(subGrid);

			column = new MyColumnConfig("Matakuliah");
			column.setParent(columns);
			column.setWidth("18%");

			column = new MyColumnConfig("Dosen");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("Ruang");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Jadwal");
			column.setParent(columns);
			column.setWidth("33%");

			column = new MyColumnConfig("Masa");
			column.setParent(columns);
			column.setWidth("19%");

			column = new MyColumnConfig("Kuota");
			column.setParent(columns);
			column.setWidth("5%");

			final Rows rowsSubGrid = new Rows();
			rowsSubGrid.setParent(subGrid);

			final EventListener matakuliahEventListenerLocal = new EventListener() {

				private RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
						.getCurrentRencanaTahunAkademik(ais.ui.util.WaktuUtil.getDate());

				@Override
				public void onEvent(Event event) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.clear(rowsSubGrid);
							if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
								return;
							}
							if (kurikulum.getSelectedItem() == null) {
								return;
							}
							if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
								return;
							}
							if (kelas.getValue().trim().isEmpty()) {
								return;
							}

							Session session = HibernateUtil.currentSession();
							Criteria criteria = session.createCriteria(KurikulumPunyaMatakuliah.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(kurikulum.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("kurikulum", kurikulum.getSelectedItem().getValue()))

									.add(semester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("semester", semester.getSelectedItem().getValue()))

									.createAlias("matakuliah", "matakuliah")
									.add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"),
										Restrictions.eq("matakuliah.aktif", true)));

							if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {

								criteria.add(Restrictions.eq("matakuliah.extraKulikuler", true));
							} else {
								criteria.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
										Restrictions.eq("matakuliah.extraKulikuler", false)));
							}

							List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
									.simpleList(criteria, KurikulumPunyaMatakuliah.class);

							System.out.println("kurikulumPunyaMatakuliahs -> " + kurikulumPunyaMatakuliahs.size());

							List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session
									.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(kurikulumPunyaMatakuliahs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliahs))
									.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))
									.add(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
									.add(Restrictions.eq("kelas", kelas.getValue()))
									.add(Restrictions.isNull("perkuliahan_paralel"))
									.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
											: Restrictions.eq("statusSemesterPendek", semesterPendek)),
									Perkuliahan.class);

							System.out.println("perkuliahans -> " + perkuliahans.size());
							Map<Long, Perkuliahan> mapPerkuliahan = new HashMap<Long, Perkuliahan>();
							for (Perkuliahan perkuliahan : perkuliahans) {
								mapPerkuliahan.put(perkuliahan.getKurikulumPunyaMatakuliah().getId(), perkuliahan);
							}

							System.out.println("mapPerkuliahan -> " + mapPerkuliahan.keySet());

							for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

								if (kurikulumPunyaMatakuliah.getMatakuliah().getAktif()) {

									final Perkuliahan perkuliahan = mapPerkuliahan
											.get(kurikulumPunyaMatakuliah.getId());

									System.out.println("perkuliahan -> " + perkuliahan);

									final MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setValign("top");
									row.setAttribute("perkuliahan", perkuliahan);
									row.setValign("top");
									row.setAttribute("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah);
									row.setParent(rowsSubGrid);

									Matakuliah matakuliahKurikulum = kurikulumPunyaMatakuliah.getMatakuliah();
									String keteranganSks = matakuliahKurikulum.getSks().intValue() == 0
											? " (0 SKS - non-kredit, tetap dapat dijadwalkan)"
											: " (" + matakuliahKurikulum.getSks() + " SKS)";
									final Checkbox checkboxConfig = new Checkbox(matakuliahKurikulum.getKode() + "-"
											+ matakuliahKurikulum.getNama() + keteranganSks);
									checkboxConfig.setChecked(perkuliahan != null);
									checkboxConfig.setDisabled(perkuliahan != null);
									row.appendChild(checkboxConfig);

									row.setValign("top");
									row.setAttribute("checkboxConfig", checkboxConfig);

									final Hbox hboxDosen = new Hbox();
									row.appendChild(hboxDosen);
									final Combobox jmlDosen = (Combobox) jumlahDosen.clone();
									jmlDosen.setReadonly(true);
									Common.selectComboItem(true, jmlDosen,
											perkuliahan == null ? 1 : perkuliahan.getJumlahDosen());
									hboxDosen.appendChild(jmlDosen);
									jmlDosen.setCols(1);

									row.setValign("top");
									row.setAttribute("jumlahDosen", jmlDosen);

									final Vbox vbox = new Vbox();
									hboxDosen.appendChild(vbox);

									EventListener jumlahDosenEvent = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.clear(vbox);

											List<Dosen> dosens = perkuliahan == null ? new ArrayList<Dosen>()
													: perkuliahan.populateDosenBuNama();

											for (int i = 1; i <= ((Integer) jmlDosen.getSelectedItem()
													.getValue()); i++) {
												Dosen dosen = dosens.size() > (i - 1) ? dosens.get(i - 1) : null;

												AmbilDataDosenBanbox dosen1 = new AmbilDataDosenBanbox();
												dosen1.setValue(dosen == null ? null : dosen.getNama());
												dosen1.setAttribute("dosen", dosen);
												dosen1.setAttribute("myValue", dosen);
												dosen1.setCols(4);
												vbox.appendChild(dosen1);

												row.setValign("top");
												row.setAttribute("dosen" + i, dosen1);
											}
										}
									};

									jumlahDosenEvent.onEvent(null);
									jmlDosen.addEventListener("onChange", jumlahDosenEvent);

									final AmbilDataRuangBanbox ruang = new AmbilDataRuangBanbox();
									ruang.setAttribute("ruang", perkuliahan == null ? null : perkuliahan.getRuang());
									ruang.setValue(perkuliahan == null || perkuliahan.getRuang() == null ? null
											: perkuliahan.getRuang().getNama());
									row.appendChild(ruang);
									ruang.setWidth("90%");

									row.setValign("top");
									row.setAttribute("ruang", ruang);

									final Combobox comboHari = (Combobox) hari.clone();
									comboHari.setCols(2);
									comboHari.setReadonly(true);
									Common.selectComboItem(true, comboHari,
											perkuliahan == null ? null : perkuliahan.getHari());

									Comboitem comboitem = new Comboitem("-");
									comboitem.setValue(null);
									comboHari.appendChild(comboitem);

									final Hbox hboxWaktu = new Hbox();
									row.appendChild(hboxWaktu);
									hboxWaktu.appendChild(new MyLabelConfig("Hari"));
									hboxWaktu.appendChild(comboHari);

									if (comboHari.getSelectedItem() == null) {
										comboHari.setSelectedItem(comboitem);
									}

									row.setValign("top");
									row.setAttribute("hari", comboHari);

									Date dateMulai = null;
									Date dateSelesai = null;
									if (perkuliahan != null) {
										try {
											if ((perkuliahan.getWaktuMulai() == null ? ""
													: perkuliahan.getWaktuMulai()) != null
													&& !(perkuliahan.getWaktuMulai() == null ? ""
															: perkuliahan.getWaktuMulai()).equals(""))
												dateMulai = Common.timeFormat2.get()
														.parse((perkuliahan.getWaktuMulai() == null ? ""
																: perkuliahan.getWaktuMulai()));
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										try {
											if ((perkuliahan.getWaktuSelesai() == null ? ""
													: perkuliahan.getWaktuSelesai()) != null
													&& !(perkuliahan.getWaktuSelesai() == null ? ""
															: perkuliahan.getWaktuSelesai()).equals(""))
												dateSelesai = Common.timeFormat2.get()
														.parse((perkuliahan.getWaktuSelesai() == null ? ""
																: perkuliahan.getWaktuSelesai()));
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									}

									final AmbilDataJamPerkuliahanBanbox jamPerkuliahan;
									hboxWaktu.appendChild(new ais.ui.util.MyLabelConfig("Jam"));
									hboxWaktu.appendChild(jamPerkuliahan = new AmbilDataJamPerkuliahanBanbox(
											(Jurusan) (jurusan.getSelectedItem() == null
													|| jurusan.getSelectedItem().getValue() == null ? null
															: jurusan.getSelectedItem().getValue())));
									jamPerkuliahan.setValue(
											perkuliahan == null || perkuliahan.getJamPerkuliahan() == null ? ""
													: perkuliahan.getJamPerkuliahan().getNama());
									jamPerkuliahan.setAttribute("jamPerkuliahan",
											perkuliahan == null ? null : perkuliahan.getJamPerkuliahan());
									jamPerkuliahan.setAttribute("myValue",
											perkuliahan == null ? null : perkuliahan.getJamPerkuliahan());
									jamPerkuliahan.setWidth("90%");

									jamPerkuliahan.setCols(4);

									row.setValign("top");
									row.setAttribute("jamPerkuliahan", jamPerkuliahan);

									final MyTimebox waktuMulai;
									final MyTimebox waktuSelesai;
									hboxWaktu.appendChild(new ais.ui.util.MyLabelConfig("Wkt"));
									hboxWaktu.appendChild(waktuMulai = new MyTimebox(
											dateMulai == null ? ais.ui.util.WaktuUtil.getDate() : dateMulai));
									waktuMulai.setWidth("90%");

									hboxWaktu.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
									hboxWaktu.appendChild(waktuSelesai = new MyTimebox(
											dateSelesai == null ? ais.ui.util.WaktuUtil.getDate() : dateSelesai));
									waktuSelesai.setWidth("90%");

									row.setValign("top");
									row.setAttribute("waktuMulai", waktuMulai);
									row.setValign("top");
									row.setAttribute("waktuSelesai", waktuSelesai);

									waktuMulai.setCols(2);
									waktuSelesai.setCols(2);

									EventListener jamPerkuliahanEventListener = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											JamPerkuliahan myJamPerkuliahan = (JamPerkuliahan) jamPerkuliahan
													.getAttribute("jamPerkuliahan");
											if (myJamPerkuliahan != null) {
												waktuMulai.setValue(myJamPerkuliahan.getMulai());
												waktuSelesai.setValue(myJamPerkuliahan.getSampai());
											}

											boolean jamPerkuliahanWajibDipilih = Common.bolehKonfigurasi("jam_perkuliahan_wajib_dipilih", Konfigurasi.TIDAK_AKTIF);

											waktuMulai.setDisabled(
													myJamPerkuliahan != null || jamPerkuliahanWajibDipilih);
											waktuSelesai.setDisabled(
													myJamPerkuliahan != null || jamPerkuliahanWajibDipilih);
										}
									};

									jamPerkuliahan.setEventListener(jamPerkuliahanEventListener);

									hboxWaktu.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
									final MyDatebox tanggalMulaiPerkuliahan = new MyDatebox(
											perkuliahan == null ? null : perkuliahan.getTanggalMulaiPerkuliahan());
									tanggalMulaiPerkuliahan.setCols(4);
									row.setValign("top");
									row.setAttribute("tanggalMulaiPerkuliahan", tanggalMulaiPerkuliahan);
									hboxWaktu.appendChild(tanggalMulaiPerkuliahan);
									tanggalMulaiPerkuliahan.setReadonly(true);

									if (perkuliahan == null || perkuliahan.getTanggalMulaiPerkuliahan() == null) {

										EventListener hariEventListener = new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												String har = (String) (comboHari.getSelectedItem() == null ? null
														: comboHari.getSelectedItem().getValue());

												if (har != null && rencanaTahunAkademik != null
														&& rencanaTahunAkademik.getTanggalMulai() != null) {
													Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
													calendar.setTime(rencanaTahunAkademik.getTanggalMulai());
													int i = 0;
													while (true) {
														i++;
														if (i > 356) {
															break;
														}
														try {
															int dayOfweek = calendar.get(Calendar.DAY_OF_WEEK);
															if (har.equals(Common.haris[dayOfweek - 1])) {
																tanggalMulaiPerkuliahan.setValue(calendar.getTime());
																break;
															}

															calendar.set(Calendar.DATE,
																	calendar.get(Calendar.DATE) + 1);
														} catch (Exception e) {
															break;
														}
													}
												}
											}
										};

										comboHari.addEventListener("onChange", hariEventListener);
										hariEventListener.onEvent(null);
									}

									final Hbox hboxMasa = new Hbox();
									row.appendChild(hboxMasa);

									final AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
									hboxMasa.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox(
											(Jurusan) (jurusan.getSelectedItem() == null
													|| jurusan.getSelectedItem().getValue() == null ? null
															: jurusan.getSelectedItem().getValue())));
									masaPerkuliahan.setValue(
											perkuliahan == null || perkuliahan.getMasaPerkuliahan() == null ? ""
													: perkuliahan.getMasaPerkuliahan().getNama());
									masaPerkuliahan.setAttribute("masaPerkuliahan",
											perkuliahan == null ? null : perkuliahan.getMasaPerkuliahan());
									masaPerkuliahan.setAttribute("myValue",
											perkuliahan == null ? null : perkuliahan.getMasaPerkuliahan());
									masaPerkuliahan.setWidth("90%");

									row.setValign("top");
									row.setAttribute("masaPerkuliahan", masaPerkuliahan);

									masaPerkuliahan.setCols(4);

									final MyDatebox perkuliahanDimulai;
									final MyDatebox perkuliahanSampai;

									hboxMasa.appendChild(perkuliahanDimulai = new MyDatebox(
											perkuliahan == null ? null : perkuliahan.getPerkuliahanDimulai()));
									hboxMasa.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
									hboxMasa.appendChild(perkuliahanSampai = new MyDatebox(
											perkuliahan == null ? null : perkuliahan.getPerkuliahanSampai()));

									row.setValign("top");
									row.setAttribute("perkuliahanDimulai", perkuliahanDimulai);
									row.setValign("top");
									row.setAttribute("perkuliahanSampai", perkuliahanSampai);

									perkuliahanDimulai.setCols(4);
									perkuliahanSampai.setCols(4);

									EventListener masaPerkuliahanEventListener = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MasaPerkuliahan myMasaPerkuliahan = (MasaPerkuliahan) masaPerkuliahan
													.getAttribute("masaPerkuliahan");
											if (myMasaPerkuliahan != null) {
												perkuliahanDimulai.setValue(myMasaPerkuliahan.getMulai());
												perkuliahanSampai.setValue(myMasaPerkuliahan.getSampai());
											}

											perkuliahanDimulai.setDisabled(myMasaPerkuliahan != null);
											perkuliahanSampai.setDisabled(myMasaPerkuliahan != null);
										}
									};

									masaPerkuliahan.setEventListener(masaPerkuliahanEventListener);
									masaPerkuliahanEventListener.onEvent(null);

									final MyIntbox kuota = new MyIntbox(
											perkuliahan == null ? Ruang.getDefaultKapasitas()
													: perkuliahan.getKapasitasKelas());

									EventListener eventListenerCheck = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											hboxDosen.setVisible(checkboxConfig.isChecked());
											ruang.setVisible(checkboxConfig.isChecked());
											hboxWaktu.setVisible(checkboxConfig.isChecked());
											hboxMasa.setVisible(checkboxConfig.isChecked());
											kuota.setVisible(checkboxConfig.isChecked());
										}
									};

									checkboxConfig.addEventListener("onClick", eventListenerCheck);
									eventListenerCheck.onEvent(null);

									kuota.setParent(row);
									kuota.setWidth("90%");
									row.setValign("top");
									row.setAttribute("kuota", kuota);
								}
							}
						}
					});
				}

			};

			final EventListener kurikulumEventListenerLocal = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(kurikulum);
					kurikulum.setSelectedItem(null);
					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						return;
					}

					Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
							|| jurusan.getSelectedItem().getValue() == null ? null
									: jurusan.getSelectedItem().getValue());

					List<Kurikulum> kurikulums = ConstantValues
							.simpleList(HibernateUtil.currentSession().createCriteria(Kurikulum.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.addOrder(Order.desc("tahun"))

									.createAlias("program", "program", Criteria.LEFT_JOIN)
									.add(Restrictions.or(Restrictions.isNull("program"),
											Restrictions.eq("program.nama",
													program.getSelectedItem() == null ? null
															: program.getSelectedItem().getValue())))

									.add(Restrictions.eq("jurusan", myJurusan)), Kurikulum.class);

					for (Kurikulum kurikulum : kurikulums) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
						comboitem.setValue(kurikulum);
						comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
								+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
						PenjadwalanUtil.this.kurikulum.appendChild(comboitem);
					}

					if (perkuliahan != null && perkuliahan.getKurikulum() != null) {
						Common.selectComboItem(true, kurikulum, perkuliahan.getKurikulum());
					}

					if (kurikulum.getSelectedItem() == null && myJurusan != null) {
						Kurikulum mykurikulum = (Kurikulum) ConstantValues
								.simpleObject(HibernateUtil.currentSession().createCriteria(Kurikulum.class)
										.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan))

										.createAlias("program", "program", Criteria.LEFT_JOIN)
										.add(Restrictions.or(Restrictions.isNull("program"),
												Restrictions.eq("program.nama",
														program.getSelectedItem() == null ? null
																: program.getSelectedItem().getValue())))

										.setMaxResults(1), Kurikulum.class);
						Common.selectComboItem(true, kurikulum, mykurikulum);
					}

					if (kurikulum.getSelectedItem() != null && kurikulum.getSelectedItem().getValue() != null) {
						matakuliahEventListenerLocal.onEvent(null);
					}
				}

			};

			Common.selectComboItem(true, tahunAjaran, perkuliahan.getTahunAjaran());
			Common.selectComboItem(true, semester, perkuliahan.getSemester());
			Common.selectComboItem(true, program, perkuliahan.getProgram());
			kelas.setValue(perkuliahan.getKelas());
			kelas.setAttribute("kelas", perkuliahan.getKelasref());
			kelasref.setChecked(perkuliahan.getId() == null || perkuliahan.getKelasref() != null);

			jurusan.addEventListener("onChange", kurikulumEventListenerLocal);
			program.addEventListener("onChange", kurikulumEventListenerLocal);

			semester.addEventListener("onChange", matakuliahEventListenerLocal);
			kurikulum.addEventListener("onChange", matakuliahEventListenerLocal);
			kelas.setEventListener(kurikulumEventListenerLocal);

			kurikulumEventListenerLocal.onEvent(null);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			final EventListener simpanEvent = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					int s = (Integer) semester.getSelectedItem().getValue();
					@SuppressWarnings("unchecked")
					List<Row> rows = rowsSubGrid.getChildren();
					for (Row row : rows) {
						Checkbox checkboxConfig = (Checkbox) row.getAttribute("checkboxConfig");

						if (checkboxConfig.isChecked()) {
							KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) row
									.getAttribute("kurikulumPunyaMatakuliah");
							Combobox hari = (Combobox) row.getAttribute("hari");

							MyTimebox waktuMulai = (MyTimebox) row.getAttribute("waktuMulai");
							MyTimebox waktuSelesai = (MyTimebox) row.getAttribute("waktuSelesai");

							MyIntbox kuota = (MyIntbox) row.getAttribute("kuota");

							MyDatebox tanggalMulaiPerkuliahan = (MyDatebox) row.getAttribute("tanggalMulaiPerkuliahan");

							Double waktuMulaiD = !waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
									: Double.parseDouble(Common.timeFormat2.get().format(waktuMulai.getValue())) + 0.01;
							Double waktuSelesaiD = !waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
									: Double.parseDouble(Common.timeFormat2.get().format(waktuSelesai.getValue()))
											- 0.01;

							if (hari.getSelectedItem() != null && hari.getSelectedItem().getValue() != null) {
								// if (waktuMulaiD != null && waktuSelesaiD !=
								// null
								// && waktuMulaiD >= waktuSelesaiD) {
								// MyMessageboxConfig.show(
								// "Waktu mulai \"" +
								// Common.timeFormat.get().format(waktuMulai.getValue())
								// + "\" tidak boleh lebih besar nilainya atau
								// sama
								// dengan waktu selesai \""
								// +
								// Common.timeFormat.get().format(waktuSelesai.getValue())
								// + "\" ",
								// "Peringatan", MyMessageboxConfig.OK,
								// MyMessageboxConfig.EXCLAMATION);
								// continue;
								// }
							}

							try {

								Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("perkuliahan");

								if (perkuliahan == null) {
									Session session = HibernateUtil.currentNativeSession();
									perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
											.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))
											.add(Restrictions.eq("tahunAjaran",
													tahunAjaran.getSelectedItem().getValue()))
											.add(Restrictions.eq("kelas", kelas.getValue()))
											.add(Restrictions.isNull("perkuliahan_paralel"))
											.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
													: Restrictions.eq("statusSemesterPendek", semesterPendek))
											.setMaxResults(1).uniqueResult();
									HibernateUtil.closeSession();
								}
								AmbilDataRuangBanbox ruang = (AmbilDataRuangBanbox) row.getAttribute("ruang");
								Ruang ruangan = (Ruang) ruang.getAttribute("ruang");
								if (perkuliahan == null) {
									perkuliahan = new Perkuliahan();
									perkuliahan.setKapasitasKelas(ruangan == null ? Ruang.getDefaultKapasitas()
											: ruangan.getKapasitasRuangan());
								}

								Combobox jumlahDosen = (Combobox) row.getAttribute("jumlahDosen");
								int jmlDosen = (Integer) jumlahDosen.getSelectedItem().getValue();
								for (int i = 1; i <= jmlDosen; i++) {
									AmbilDataDosenBanbox dosen1 = (AmbilDataDosenBanbox) row.getAttribute("dosen" + i);
									Dosen dsn = (Dosen) dosen1.getAttribute("dosen");
									if (dsn != null) {
										if (i == 1) {
											perkuliahan.setDosen1(dsn);
										} else if (i == 2) {
											perkuliahan.setDosen2(dsn);
										} else if (i == 3) {
											perkuliahan.setDosen3(dsn);
										} else if (i == 4) {
											perkuliahan.setDosen4(dsn);
										} else if (i == 5) {
											perkuliahan.setDosen5(dsn);
										} else if (i == 6) {
											perkuliahan.setDosen6(dsn);
										} else if (i == 7) {
											perkuliahan.setDosen7(dsn);
										} else if (i == 8) {
											perkuliahan.setDosen8(dsn);
										} else if (i == 9) {
											perkuliahan.setDosen9(dsn);
										} else if (i == 10) {
											perkuliahan.setDosen10(dsn);
										}
									}
								}

								MyDatebox perkuliahanDimulai = (MyDatebox) row.getAttribute("perkuliahanDimulai");
								MyDatebox perkuliahanSampai = (MyDatebox) row.getAttribute("perkuliahanSampai");
								Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

								AmbilDataMasaPerkuliahanBanbox masaPerkuliahan = (AmbilDataMasaPerkuliahanBanbox) row
										.getAttribute("masaPerkuliahan");

								if (Common.bolehKonfigurasi("masa_perkuliahan_di_jadwal_perkuliahan_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
									if (masaPerkuliahan == null) {
										MyMessageboxConfig.show("Mohon maaf, Masa Perkuliahan wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Masa Perkuliahan pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										return;
									}
								}

								AmbilDataJamPerkuliahanBanbox jamPerkuliahan = (AmbilDataJamPerkuliahanBanbox) row
										.getAttribute("jamPerkuliahan");

								if (!abaikanWaktuBentrokDenganJadwalLain.isChecked()) {
									if (hari.getSelectedItem() != null && hari.getSelectedItem().getValue() != null) {
										if (Common.checkKelasJadwalPerkuliahan(perkuliahan.getId(),
												(Jurusan) jurusan.getSelectedItem().getValue(),
												(String) program.getSelectedItem().getValue(),
												!hari.isVisible() || hari.getSelectedItem() == null ? null
														: hari.getSelectedItem().getValue().toString(),
												waktuMulaiD, waktuSelesaiD,
												tahunAjaran.getSelectedItem().getValue().toString(),
												s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
												kelas.getValue().trim(),
												(Integer) semester.getSelectedItem().getValue(), null, semesterPendek,
												true, true, true, true, true, perkuliahanDimulai.getValue(),
												perkuliahanSampai.getValue(), matakuliah,
												(Ruang) ruang.getAttribute("ruang")) != null) {
											continue;
										}
									}
								}

								if (!abaikanWaktuBentrokDenganJadwalLain.isChecked()) {
									if (ruang.getAttribute("ruang") != null) {

										if (Common.checkJadwalRuangPerkuliahan(perkuliahan.getId(),
												!ruang.isVisible() ? null : (Ruang) ruang.getAttribute("ruang"),
												!hari.isVisible() || hari.getSelectedItem() == null
														|| hari.getSelectedItem().getValue() == null ? null
																: hari.getSelectedItem().getValue().toString(),
												waktuMulaiD, waktuSelesaiD,
												tahunAjaran.getSelectedItem().getValue().toString(),
												s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, null,
												semesterPendek, true, true, true, true, true,
												perkuliahanDimulai.getValue(), perkuliahanSampai.getValue()) != null) {
											continue;
										}
									}
								}

								List<Dosen> dosens = perkuliahan.populateDosenBuNama();
								if (!abaikanWaktuBentrokDenganJadwalLain.isChecked()) {
									if (hari.getSelectedItem() != null && hari.getSelectedItem().getValue() != null) {

										for (Dosen dosen : dosens) {
											if (dosen != null) {
												if (Common.checkJadwalDosen(perkuliahan.getId(),
														!hari.isVisible() || hari.getSelectedItem() == null ? null
																: hari.getSelectedItem().getValue().toString(),
														waktuMulaiD, waktuSelesaiD, dosen,
														tahunAjaran.getSelectedItem().getValue().toString(),
														s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
														(Jurusan) jurusan.getSelectedItem().getValue(), matakuliah,
														kelas.getValue().trim(), null, semesterPendek, true, true, true,
														true, true, perkuliahanDimulai.getValue(),
														perkuliahanSampai.getValue()) != null) {
													continue;
												}
											}
										}
									}
								}

								if (!abaikanWaktuBentrokDenganJadwalLain.isChecked()) {
									if (Common.checkMatakuliahKesamaanBukanParalel(perkuliahan,
											(Jurusan) jurusan.getSelectedItem().getValue(), kelas.getValue().trim(),
											matakuliah, s, tahunAjaran.getSelectedItem().getValue().toString(),
											(String) program.getSelectedItem().getValue(), null, semesterPendek, true,
											true, true, true, true, perkuliahanDimulai.getValue(),
											perkuliahanSampai.getValue(), merupakanRemedial) != null) {
										continue;
									}
								}

								perkuliahan.setDosenBisaMerubahTanggalPerkuliahan(
										dosenBisaMerubahTanggalPerkuliahan.isChecked());
								perkuliahan.setAbaikanWaktuBentrokDenganJadwalLain(
										abaikanWaktuBentrokDenganJadwalLain.isChecked());
								perkuliahan.setTampilkanSaatPengambilanKrs(tampilkanSaatPengambilanKrs.isChecked());
								perkuliahan.setMasaPerkuliahan(
										(MasaPerkuliahan) masaPerkuliahan.getAttribute("masaPerkuliahan"));
								perkuliahan.setPerkuliahanDimulai(perkuliahanDimulai.getValue());
								perkuliahan.setPerkuliahanSampai(perkuliahanSampai.getValue());

								perkuliahan.setJamPerkuliahan(
										(JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan"));

								perkuliahan.setMerupakan_tanpa_dosen(dosens.isEmpty());
								perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(
										hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null);
								perkuliahan.setMerupakan_tanpa_ruangan(ruangan == null);

								perkuliahan
										.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
												: Common.timeFormat2.get().format(waktuMulai.getValue()));
								perkuliahan.setWaktuSelesai(
										!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
												: Common.timeFormat2.get().format(waktuSelesai.getValue()));
								perkuliahan.setHari(!hari.isVisible() || hari.getSelectedItem() == null
										|| hari.getSelectedItem().getValue() == null ? null
												: hari.getSelectedItem().getValue().toString());

								perkuliahan.setTahunAjaran((String) (tahunAjaran.getSelectedItem() == null ? null
										: tahunAjaran.getSelectedItem().getValue()));

								perkuliahan.setJumlahDosen((Integer) (jumlahDosen.getSelectedItem() == null ? 1
										: jumlahDosen.getSelectedItem().getValue()));

								perkuliahan.setJurusan((Jurusan) (jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue()));
								perkuliahan.setMatakuliah(matakuliah);

								perkuliahan.setKelas(kelas.isVisible() ? kelas.getValue().trim() : "");
								perkuliahan.setKelasref((Kelas) kelas.getAttribute("kelas"));
								perkuliahan.setRuang((Ruang) (ruang.isVisible() ? ruang.getAttribute("ruang") : null));

								perkuliahan.setSemester((Integer) semester.getSelectedItem().getValue());

								perkuliahan.setProgram(program.getSelectedItem() == null
										|| program.getSelectedItem().getValue() == null ? "Reguler"
												: program.getSelectedItem().getValue().toString());
								perkuliahan.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());
								perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
								// SP-aware: bila "Semester Periode" = Semester Pendek (SP), tandai perkuliahan sebagai
								// Semester Pendek & kosongkan ganjilGenap — samakan dengan simpan pada Tambah biasa.
								Object nilaiPeriodeKur = ganjilGenap.getSelectedItem() == null ? null
										: ganjilGenap.getSelectedItem().getValue();
								boolean pilihSpKur = Perkuliahan.SP.equals(nilaiPeriodeKur);
								perkuliahan.setStatusSemesterPendek(
										pilihSpKur ? Perkuliahan.SEMESTER_PENDEK : semesterPendek);
								perkuliahan.setTanggalMulaiPerkuliahan(tanggalMulaiPerkuliahan.getValue());
								perkuliahan.setMerupakanRemedial(merupakanRemedial);
								perkuliahan.setKapasitasKelas(kuota.getValue());

								perkuliahan.setGanjilGenap(pilihSpKur ? null : (String) nilaiPeriodeKur);

								List<Perkuliahan> perkuliahansParalel = perkuliahan.getId() == null ? null
										: perkuliahan.ambilParalelPerkuliahan();

								Session session = HibernateUtil.currentNativeSession();
								if (kelasref.isChecked()) {
									Kelas kls = (Kelas) kelas.getAttribute("kelas");
									if (kls == null) {
										kls = (Kelas) session.createCriteria(Kelas.class)
												.add(Restrictions.ilike("nama", kelas.getValue()))
												.add(Restrictions.or(Restrictions.eq("aktif", true),
														Restrictions.isNull("aktif")))
												.setMaxResults(1).uniqueResult();
									}
									perkuliahan.setKelasref(kls);
								} else {
									perkuliahan.setKelasref(null);
								}

								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, perkuliahan);

								if (perkuliahansParalel != null && !perkuliahansParalel.isEmpty()) {
									for (Perkuliahan paralel : perkuliahansParalel) {
										if (!paralel.getId().equals(perkuliahan.getId())) {
											session.refresh(paralel);
											paralel.setKelas(perkuliahan.getKelas());
											paralel.setKelasref(perkuliahan.getKelasref());
											Common.refreshUpdate(session, paralel);
										}
									}
								}

								session.getTransaction().commit();
								HibernateUtil.closeSession();

								row.setValign("top");
								row.setAttribute("perkuliahan", perkuliahan);

								session = HibernateUtil.currentSession();
								if (perkuliahan.getTanggalMulaiPerkuliahan() != null) {

									@SuppressWarnings("unchecked")
									List<KurikulumPunyaMatakuliahDetail> kurikulumPunyaMatakuliahDetails = session
											.createCriteria(KurikulumPunyaMatakuliahDetail.class)
											.addOrder(Order.asc("nomorUrut"))
											.add(Restrictions.eq("kurikulumPunyaMatakuliah",
													perkuliahan.getKurikulumPunyaMatakuliah()))
											.list();
									MatakuliahKurikulumDetailHelper.simpan(perkuliahan,
											perkuliahan.getKurikulumPunyaMatakuliah(), kurikulumPunyaMatakuliahDetails,
											perkuliahan.getTanggalMulaiPerkuliahan(),
											perkuliahan.getLewatiTanggalMerahNasional());
								}

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}

					/*
					 * Jangan menutup dialog seolah-olah seluruh data berhasil ketika sebagian jadwal
					 * dilewati oleh pemeriksaan bentrok. Hal ini terutama membingungkan untuk mata
					 * kuliah 0 SKS karena data kurikulumnya terlihat, tetapi jumlah jadwal tetap nol.
					 * SKS bukan syarat penjadwalan; yang diperiksa di sini adalah hasil penyimpanannya.
					 */
					int jumlahDipilih = 0;
					int jumlahTersedia = 0;
					String belumTersimpan = "";
					for (Row row : rows) {
						Checkbox checkboxConfig = (Checkbox) row.getAttribute("checkboxConfig");
						if (checkboxConfig != null && checkboxConfig.isChecked()) {
							jumlahDipilih++;
							if (row.getAttribute("perkuliahan") != null) {
								jumlahTersedia++;
							} else {
								belumTersimpan += (belumTersimpan.isEmpty() ? "" : "\n") + "- "
										+ checkboxConfig.getLabel();
							}
						}
					}
					if (jumlahDipilih > jumlahTersedia) {
						MyMessageboxConfig.show(
								"Sebagian jadwal belum tersimpan. Mata kuliah 0 SKS tetap diperbolehkan "
										+ "untuk dijadwalkan; data di bawah ini dilewati karena bentrok jadwal, "
										+ "data wajib belum lengkap, atau terjadi kegagalan penyimpanan:\n\n"
										+ belumTersimpan
										+ "\n\nPeriksa hari, waktu, ruang, dosen, kelas, dan Masa Perkuliahan, "
										+ "kemudian simpan kembali. Gunakan pilihan abaikan bentrok hanya jika "
										+ "jadwal tersebut memang telah dipastikan tidak bermasalah.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}

				}
			};

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (tahunAjaran.getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, Tahun Akademik wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Tahun Akademik pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Program wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Program pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.showFormat("Mohon maaf, {V1} wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu data pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, Common.getBahasaConfig("Jurusan"));
						return;
					}
					if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Semester wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Semester pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (kelas.getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, Kelas wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) isi terlebih dahulu nama Kelas pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					String ta = (String) tahunAjaran.getSelectedItem().getValue();
					// SP-aware: bila "Semester Periode" = Semester Pendek (SP) → cek konfigurasi PENJADWALAN_SP
					// dan tampilkan label "SP" pada peringatan.
					Object nilaiPeriodeCekKur = ganjilGenap == null || ganjilGenap.getSelectedItem() == null ? null
							: ganjilGenap.getSelectedItem().getValue();
					boolean cekSpKur = Perkuliahan.SP.equals(nilaiPeriodeCekKur);
					Integer semesterPendekCekKur = cekSpKur ? Perkuliahan.SEMESTER_PENDEK : semesterPendek;
					String sem = cekSpKur ? Perkuliahan.SP
							: (Perkuliahan.GANJIL.equals(nilaiPeriodeCekKur)
									|| Perkuliahan.GENAP.equals(nilaiPeriodeCekKur)
											? nilaiPeriodeCekKur.toString()
											: (((Integer) semester.getSelectedItem().getValue()) % 2 == 0
													? Perkuliahan.GENAP
													: Perkuliahan.GANJIL));

					if (apakahPenjadwalanKurikulumTidakAktif(ta, sem, semesterPendekCekKur)) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, jadwal belum dapat dibuat karena penjadwalan untuk Tahun Akademik \"{V1}\" "
										+ "semester \"{V2}\" saat ini BELUM diaktifkan. Langkah yang dapat Bapak/Ibu lakukan: "
										+ "(1) pastikan pilihan Tahun Akademik dan Jenis Semester telah sesuai; (2) mohon meminta "
										+ "Administrator mengaktifkan penjadwalan periode tersebut melalui tombol "
										+ "\"Aktif/Non-aktifkan Penjadwalan\" pada layar Jadwal Perkuliahan; (3) setelah aktif, "
										+ "silakan ulangi proses ini. Terima kasih atas pengertian Bapak/Ibu.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
						return;
					}

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							simpanEvent.onEvent(arg0);

							onSearchDefaultListener.onSearchDefault(null);
							window.detach();
						}
					});

				}
			});
			save.setParent(toolbar);

			MyToolbarbuttonConfig copy = new MyToolbarbuttonConfig("Simpan dan copy ke kelas lain",
					"/img/svg/edit-copy.svg");
			copy.setTooltiptext("Copy");
			copy.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (tahunAjaran.getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, Tahun Akademik wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Tahun Akademik pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Program wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Program pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.showFormat("Mohon maaf, {V1} wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu data pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, Common.getBahasaConfig("Jurusan"));
						return;
					}
					if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Semester wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Semester pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (kelas.getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, Kelas wajib diisi terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) isi terlebih dahulu nama Kelas pada kolom yang tersedia; (2) kemudian ulangi proses yang Bapak/Ibu lakukan.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					String ta = (String) tahunAjaran.getSelectedItem().getValue();
					// SP-aware: bila "Semester Periode" = Semester Pendek (SP) → cek konfigurasi PENJADWALAN_SP
					// dan tampilkan label "SP" pada peringatan.
					Object nilaiPeriodeCekKur = ganjilGenap == null || ganjilGenap.getSelectedItem() == null ? null
							: ganjilGenap.getSelectedItem().getValue();
					boolean cekSpKur = Perkuliahan.SP.equals(nilaiPeriodeCekKur);
					Integer semesterPendekCekKur = cekSpKur ? Perkuliahan.SEMESTER_PENDEK : semesterPendek;
					String sem = cekSpKur ? Perkuliahan.SP
							: (Perkuliahan.GANJIL.equals(nilaiPeriodeCekKur)
									|| Perkuliahan.GENAP.equals(nilaiPeriodeCekKur)
											? nilaiPeriodeCekKur.toString()
											: (((Integer) semester.getSelectedItem().getValue()) % 2 == 0
													? Perkuliahan.GENAP
													: Perkuliahan.GANJIL));

					if (apakahPenjadwalanKurikulumTidakAktif(ta, sem, semesterPendekCekKur)) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, jadwal belum dapat dibuat karena penjadwalan untuk Tahun Akademik \"{V1}\" "
										+ "semester \"{V2}\" saat ini BELUM diaktifkan. Langkah yang dapat Bapak/Ibu lakukan: "
										+ "(1) pastikan pilihan Tahun Akademik dan Jenis Semester telah sesuai; (2) mohon meminta "
										+ "Administrator mengaktifkan penjadwalan periode tersebut melalui tombol "
										+ "\"Aktif/Non-aktifkan Penjadwalan\" pada layar Jadwal Perkuliahan; (3) setelah aktif, "
										+ "silakan ulangi proses ini. Terima kasih atas pengertian Bapak/Ibu.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
						return;
					}

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							simpanEvent.onEvent(arg0);

							window.detach();

							AmbilDataKelasBanyak ambilDataKelasBanyak = new AmbilDataKelasBanyak(
									new ArrayList<Kelas>());
							ambilDataKelasBanyak
									.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							ambilDataKelasBanyak.setHeight("95%");
							ambilDataKelasBanyak.setWidth("700px");

							ambilDataKelasBanyak.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									@SuppressWarnings("unchecked")
									List<Kelas> kelases = (List<Kelas>) arg0.getData();
									Session session = HibernateUtil.currentSession();
									String warning = "";

									@SuppressWarnings("unchecked")
									List<Row> rows = rowsSubGrid.getChildren();
									for (Row row : rows) {
										Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("perkuliahan");
										if (perkuliahan != null && perkuliahan.getId() != null) {
											for (Kelas kelas : kelases) {
												if (kelas.getNama() != null
														&& !kelas.getNama().equalsIgnoreCase(perkuliahan.getKelas())) {
													int count = ((Number) session.createCriteria(Perkuliahan.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(perkuliahan.getStatusSemesterPendek() == null
																	? Restrictions.isNull("statusSemesterPendek")
																	: Restrictions.eq("statusSemesterPendek",
																			perkuliahan.getStatusSemesterPendek()))
															.add(Restrictions.sqlRestriction("true"))
															.add(Restrictions.eq("jurusan", perkuliahan.getJurusan()))
															.add(Restrictions.eq("program", perkuliahan.getProgram()))
															.add(Restrictions.eq("tahunAjaran",
																	perkuliahan.getTahunAjaran()))
															.add(Restrictions.eq("semester", perkuliahan.getSemester()))
															.add(Restrictions.ilike("kelas", kelas.getNama(),
																	MatchMode.EXACT))
															.createAlias("matakuliah", "matakuliah")
															.add(Restrictions.ilike("matakuliah.kode",
																	perkuliahan.getMatakuliah().getKode(),
																	MatchMode.EXACT))
															.setProjection(Projections.rowCount()).uniqueResult())
															.intValue();
													if (count == 0) {
														Perkuliahan p = (Perkuliahan) perkuliahan.clone();
														p.setId(null);
														p.setAbaikanWaktuBentrokDenganJadwalLain(true);
														p.setKelas(kelas.getNama());
														p.setKelasref(kelas);
														session.save(p);
													} else {
														warning += "Jadwal matakuliah \""
																+ perkuliahan.getMatakuliah().getNama() + "\" program "
																+ perkuliahan.getProgram() + " prodi "
																+ perkuliahan.getJurusan().getNama()
																+ " sudah ada di kelas " + kelas.getNama() + ".\n\n";
		}
	}

											}
										}
									}

									if (!warning.isEmpty()) {
										MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK,
												MyMessageboxConfig.EXCLAMATION);
									}

									onSearchDefaultListener.onSearchDefault(null);

								}
							});

							ambilDataKelasBanyak.onModal();

						}
					});

				}
			});
			copy.setParent(toolbar);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		window.onModal();
	}

	private boolean apakahPenjadwalanKurikulumTidakAktif(String tahunAkademik, String jenisSemester,
			Integer semesterPendek) {
		Jurusan jurusanTujuan = jurusan == null || jurusan.getSelectedItem() == null
				|| jurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) jurusan.getSelectedItem().getValue();
		Fakultas fakultasTujuan = jurusanTujuan == null ? null : jurusanTujuan.getFakultas();
		if (fakultasTujuan == null && fakultas != null && fakultas.getSelectedItem() != null
				&& fakultas.getSelectedItem().getValue() != null) {
			fakultasTujuan = (Fakultas) fakultas.getSelectedItem().getValue();
		}
		String programTujuan = program == null || program.getSelectedItem() == null
				|| program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue().toString();
		return CommonPenjadwalan.apakahPenjadwalanTidakAktif(tahunAkademik, jenisSemester, semesterPendek,
				fakultasTujuan, jurusanTujuan, programTujuan);
	}

	/**
	 * Menampilkan jendela pemilihan Tahun Akademik dan Semester (Ganjil/Genap), lalu setelah dipilih
	 * menyajikan hasil pengecekan jadwal yang saling bentrok pada periode tersebut. Titik masuk UI
	 * mandiri untuk fitur "Lihat Jadwal Bentrok"; menggunakan {@link #checkBentrokBerdasarRuangan},
	 * {@link #checkBentrokBerdasarKelas}, dan {@link #checkBentrokBerdasarDosen} sebagai mesin
	 * pemeriksa di baliknya.
	 *
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen ZK atau akses database
	 */
	public static void lihatJadwalBentrok() throws Exception {
		final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("300px");
		window.setWidth("600px");
		final Combobox tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		final Combobox genapGanjil = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjil);
		genapGanjil.setWidth("90%");
		genapGanjil.setReadonly(true);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		genapGanjil.appendChild(comboitem);

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		final AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig semesterPendek;
		row.appendChild(semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses Lihat Jam Bentrok", "/img/save.gif");
		save.setTooltiptext("Proses");
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();

				final String tahunAjaran = tahunAkademik.getSelectedItem() == null
						|| tahunAkademik.getSelectedItem().getValue() == null ? null
								: tahunAkademik.getSelectedItem().getValue().toString();
				final String smt = genapGanjil.getSelectedItem() == null
						|| genapGanjil.getSelectedItem().getValue() == null ? null
								: genapGanjil.getSelectedItem().getValue().toString();
				final Integer sp = semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null;
				final MasaPerkuliahan mp = (MasaPerkuliahan) masaPerkuliahan.getAttribute("masaPerkuliahan");

				final Label label = new Label(ais.common.Common.getBahasaConfig("Sedang mencari jadwal bentrok"));

				final Map<String, Map> bentroks = new HashMap<String, Map>();

				new Thread(new Runnable() {

					@SuppressWarnings({ "unchecked" })
					@Override
					public void run() {
						try {

						try {
							Session session = HibernateUtil.currentNativeSession();
							List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.isNotNull("jurusan")).add(Restrictions.isNotNull("hari"))
									.add(Restrictions.ne("hari", "")).add(Restrictions.gt("waktuMulaiD", 0.1))
									.add(Restrictions.gt("waktuSelesaiD", 0.1))
									.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("tahunAjaran", tahunAjaran))
									.add(sp == null ? Restrictions.isNull("statusSemesterPendek")
											: Restrictions.eq("statusSemesterPendek", sp))
									.add(smt == null ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("ganjilGenap", smt))
									.add(mp == null ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("masaPerkuliahan", mp))
									.list();
							HibernateUtil.closeSession();

							label.setValue("Sedang mencari jadwal bentrok berdasarkan ruangan");
							Map<Long, List<Perkuliahan[]>> bentrokRuangans = checkBentrokBerdasarRuangan(perkuliahans);
							label.setValue("Sedang mencari jadwal bentrok berdasarkan dosen");
							Map<Dosen, List<Perkuliahan[]>> bentrokDosens = checkBentrokBerdasarDosen(perkuliahans);
							label.setValue("Sedang mencari jadwal bentrok berdasarkan kelas");
							Map<String, List<Perkuliahan[]>> bentrokKelas = checkBentrokBerdasarKelas(perkuliahans);

							if (!bentrokRuangans.isEmpty()) {
								bentroks.put("Bentrok Ruangan", bentrokRuangans);
							}
							if (!bentrokDosens.isEmpty()) {
								bentroks.put("Bentrok Dosen", bentrokDosens);
							}
							if (!bentrokKelas.isEmpty()) {
								bentroks.put("Bentrok Kelas", bentrokKelas);
							}

							perkuliahans = null;

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/util/PenjadwalanUtil.java:3876");
						}

						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

				final Timer timer = new Timer(500);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						// System.out.println("process = " + label.getValue());
						Clients.showBusy(label.getValue());
						if (label.getValue().isEmpty()) {
							Clients.clearBusy();

							if (bentroks.isEmpty()) {
								MyMessageboxConfig.show("Dengan hormat, hasil pemeriksaan menunjukkan tidak ditemukan adanya jadwal yang bentrok. Seluruh jadwal yang diperiksa aman dan tidak saling berbenturan.", "Peringatan", MyMessageboxConfig.OK,
										MyMessageboxConfig.EXCLAMATION);
							} else {

								XSSFWorkbook workbook = new XSSFWorkbook();

								XSSFSheet sheet = workbook.createSheet("Jadwal bentrok");
								sheet.setDefaultColumnWidth(18);

								XSSFRow rowhead = sheet.createRow((short) 0);

								rowhead.createCell(0).setCellValue("Jenis Bentrok");
								rowhead.createCell(1).setCellValue("Bentrok di");
								int index = 2;
								for (String romawi : Common.ROMAWI_TANPA_NOL) {
									rowhead.createCell(index++).setCellValue("Jadwal " + romawi);
									rowhead.createCell(index++).setCellValue("Bentrok dengan Jadwal " + romawi);
								}

								@SuppressWarnings("unchecked")
								Map<Long, List<Perkuliahan[]>> bentrokRuangans = bentroks.get("Bentrok Ruangan");
								int rowIndex = 1;
								if (bentrokRuangans != null) {
									for (List<Perkuliahan[]> jadwals : bentrokRuangans.values()) {
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue("Bentrok Ruangan");

										cell = row.createCell(1);
										cell.setCellValue(jadwals.get(0)[0].getRuang().getNama());

										index = 2;
										for (Perkuliahan[] jadwal : jadwals) {
											row.createCell(index++).setCellValue(jadwal[0].infoSimple());

											row.createCell(index++).setCellValue(jadwal[1].infoSimple());
										}

										rowIndex++;
									}
								}

								@SuppressWarnings("unchecked")
								Map<Dosen, List<Perkuliahan[]>> bentrokDosens = bentroks.get("Bentrok Dosen");
								if (bentrokDosens != null) {
									for (Dosen dosen : bentrokDosens.keySet()) {
										List<Perkuliahan[]> jadwals = bentrokDosens.get(dosen);
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue("Bentrok Dosen");

										cell = row.createCell(1);
										cell.setCellValue(dosen.getNama());

										index = 2;
										for (Perkuliahan[] jadwal : jadwals) {
											row.createCell(index++).setCellValue(jadwal[0].infoSimple());
											row.createCell(index++).setCellValue(jadwal[1].infoSimple());
										}

										rowIndex++;
									}
								}

								@SuppressWarnings("unchecked")
								Map<Long, List<Perkuliahan[]>> bentrokKelas = bentroks.get("Bentrok Kelas");
								if (bentrokKelas != null) {
									for (List<Perkuliahan[]> jadwals : bentrokKelas.values()) {
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue("Bentrok Kelas");

										cell = row.createCell(1);
										cell.setCellValue(
												jadwals.get(0)[0].getSemester() + " " + jadwals.get(0)[0].getKelas());

										index = 2;
										for (Perkuliahan[] jadwal : jadwals) {
											row.createCell(index++).setCellValue(jadwal[0].infoSimple());

											row.createCell(index++).setCellValue(jadwal[1].infoSimple());
										}

										rowIndex++;
									}
								}

								try {
									String filename = Sessions.getCurrent().getWebApp()
											.getRealPath("/tmp/data_jam_bentrok_"
													+ URLEncoder.encode(Common.datetimeFormat2s.get()
															.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
													+ ".xlsx");
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();

									Filedownload.save(new FileInputStream(filename),
											"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
											"data_jam_bentrok.xlsx");
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}

								bentroks.clear();
							}

							timer.detach();
						}

					}
				});
				timer.start();
			}
		});
		save.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Memeriksa daftar {@link Perkuliahan} secara pasangan (O(n^2)) untuk menemukan jadwal yang
	 * berbagi ruangan yang sama pada hari yang sama dengan rentang waktu yang tumpang tindih.
	 * Jadwal bertanda "tanpa jadwal perkuliahan" atau tanpa ruang diabaikan. Setiap pasangan yang
	 * bentrok hanya dicatat sekali (dedup lewat kunci {@code id1_id2}/{@code id2_id1}).
	 *
	 * @param perkuliahans daftar jadwal yang akan diperiksa saling silang
	 * @return peta id ruang -> daftar pasangan {@link Perkuliahan} (array 2 elemen) yang bentrok
	 *         pada ruang tersebut
	 */
	public static Map<Long, List<Perkuliahan[]>> checkBentrokBerdasarRuangan(List<Perkuliahan> perkuliahans) {

		Map<Long, List<Perkuliahan[]>> bentrokRuangans = new HashMap<Long, List<Perkuliahan[]>>();
		Set<String> sudahAda = new HashSet<String>();
		for (Perkuliahan perkuliahan : perkuliahans) {
			if (perkuliahan == null || perkuliahan.getRuang() == null) {
				continue;
			}
			if (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) {
				continue;
			}

			for (Perkuliahan perkuliahanLain : perkuliahans) {

				if (perkuliahan == null || perkuliahan.getId().equals(perkuliahanLain.getId())
						|| perkuliahanLain.getRuang() == null) {
					continue;
				}

				if (perkuliahanLain.getMerupakan_tanpa_jadwal_perkuliahan()) {
					continue;
				}

				String kode = perkuliahan.getId() + "_" + perkuliahanLain.getId();
				String kode1 = perkuliahanLain.getId() + "_" + perkuliahan.getId();

				Long start1 = perkuliahan.getPerkuliahanDimulai() == null ? null
						: perkuliahan.getPerkuliahanDimulai().getTime() + 60L;
				Long end1 = perkuliahan.getPerkuliahanSampai() == null ? null
						: perkuliahan.getPerkuliahanSampai().getTime() - 60L;

				Long start2 = perkuliahanLain.getPerkuliahanDimulai() == null ? null
						: perkuliahanLain.getPerkuliahanDimulai().getTime() + 60;
				Long end2 = perkuliahanLain.getPerkuliahanSampai() == null ? null
						: perkuliahanLain.getPerkuliahanSampai().getTime() - 60L;

				boolean ada = true;
				if (start1 != null && end1 != null && start2 != null && end2 != null) {
					ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
				}

				if (perkuliahan.getRuang().getId().equals(perkuliahanLain.getRuang().getId()) && ada
						&& (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
						&& ((perkuliahan.getHari() == null ? "" : perkuliahan.getHari())
								.equals(perkuliahanLain.getHari()))
						&& (

						(perkuliahan.getWaktuMulaiD() >= perkuliahanLain.getWaktuMulaiD() + 0.01
								&& perkuliahan.getWaktuMulaiD() <= perkuliahanLain.getWaktuSelesaiD() - 0.01)

								||

								(perkuliahan.getWaktuSelesaiD() >= perkuliahanLain.getWaktuMulaiD() + 0.01
										&& perkuliahan.getWaktuSelesaiD() <= perkuliahanLain.getWaktuSelesaiD() - 0.01)

								||

								(perkuliahanLain.getWaktuMulaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
										&& perkuliahanLain.getWaktuMulaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

								||

								(perkuliahanLain.getWaktuSelesaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
										&& perkuliahanLain.getWaktuSelesaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

								||

								(Common.numberFormat.get().format(perkuliahanLain.getWaktuMulaiD())
										.equals(Common.numberFormat.get().format(perkuliahan.getWaktuMulaiD()))
										&& Common.numberFormat.get().format(perkuliahanLain.getWaktuSelesaiD()).equals(
												Common.numberFormat.get().format(perkuliahan.getWaktuSelesaiD())))

						)) {
					sudahAda.add(kode);
					sudahAda.add(kode1);

					Ruang ruang = perkuliahan.getRuang();
					if (bentrokRuangans.containsKey(ruang.getId())) {
						bentrokRuangans.get(ruang.getId()).add(new Perkuliahan[] { perkuliahan, perkuliahanLain });
					} else {
						List<Perkuliahan[]> pps = new ArrayList<Perkuliahan[]>();
						pps.add(new Perkuliahan[] { perkuliahan, perkuliahanLain });
						bentrokRuangans.put(ruang.getId(), pps);
					}
				}
			}
		}
		return bentrokRuangans;

	}

	/**
	 * Sama seperti {@link #checkBentrokBerdasarRuangan(List)}, tetapi mengelompokkan bentrok
	 * berdasarkan kesamaan nama Kelas (bukan Ruang) pada hari dan rentang waktu yang tumpang tindih.
	 *
	 * @param perkuliahans daftar jadwal yang akan diperiksa saling silang
	 * @return peta nama kelas -> daftar pasangan {@link Perkuliahan} yang bentrok pada kelas tersebut
	 */
	public static Map<String, List<Perkuliahan[]>> checkBentrokBerdasarKelas(List<Perkuliahan> perkuliahans) {

		Map<String, List<Perkuliahan[]>> bentrokRuangans = new HashMap<String, List<Perkuliahan[]>>();
		Set<String> sudahAda = new HashSet<String>();
		for (Perkuliahan perkuliahan : perkuliahans) {
			if (perkuliahan == null || perkuliahan.getKelas() == null) {
				continue;
			}
			if (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) {
				continue;
			}

			for (Perkuliahan perkuliahanLain : perkuliahans) {

				if (perkuliahan == null || perkuliahan.getId().equals(perkuliahanLain.getId())
						|| perkuliahanLain.getKelas() == null) {
					continue;
				}

				if (perkuliahanLain.getMerupakan_tanpa_jadwal_perkuliahan()) {
					continue;
				}

				String kode = perkuliahan.getId() + "_" + perkuliahanLain.getId();
				String kode1 = perkuliahanLain.getId() + "_" + perkuliahan.getId();

				Long start1 = perkuliahan.getPerkuliahanDimulai() == null ? null
						: perkuliahan.getPerkuliahanDimulai().getTime() + 60L;
				Long end1 = perkuliahan.getPerkuliahanSampai() == null ? null
						: perkuliahan.getPerkuliahanSampai().getTime() - 60L;

				Long start2 = perkuliahanLain.getPerkuliahanDimulai() == null ? null
						: perkuliahanLain.getPerkuliahanDimulai().getTime() + 60;
				Long end2 = perkuliahanLain.getPerkuliahanSampai() == null ? null
						: perkuliahanLain.getPerkuliahanSampai().getTime() - 60L;

				boolean ada = true;
				if (start1 != null && end1 != null && start2 != null && end2 != null) {
					ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
				}

				String kelasLengkap = perkuliahan.getKelas() + "-" + perkuliahan.getSemester() + "-"
						+ perkuliahan.getJurusan().getId();
				String kelasLengkapLain = perkuliahanLain.getKelas() + "-" + perkuliahanLain.getSemester() + "-"
						+ perkuliahanLain.getJurusan().getId();

				if (kelasLengkap.equalsIgnoreCase(kelasLengkapLain) && ada
						&& (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
						&& ((perkuliahan.getHari() == null ? "" : perkuliahan.getHari())
								.equals(perkuliahanLain.getHari()))
						&& (

						(perkuliahan.getWaktuMulaiD() >= perkuliahanLain.getWaktuMulaiD() + 0.01
								&& perkuliahan.getWaktuMulaiD() <= perkuliahanLain.getWaktuSelesaiD() - 0.01)

								||

								(perkuliahan.getWaktuSelesaiD() >= perkuliahanLain.getWaktuMulaiD() + 0.01
										&& perkuliahan.getWaktuSelesaiD() <= perkuliahanLain.getWaktuSelesaiD() - 0.01)

								||

								(perkuliahanLain.getWaktuMulaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
										&& perkuliahanLain.getWaktuMulaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

								||

								(perkuliahanLain.getWaktuSelesaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
										&& perkuliahanLain.getWaktuSelesaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

								||

								(Common.numberFormat.get().format(perkuliahanLain.getWaktuMulaiD())
										.equals(Common.numberFormat.get().format(perkuliahan.getWaktuMulaiD()))
										&& Common.numberFormat.get().format(perkuliahanLain.getWaktuSelesaiD()).equals(
												Common.numberFormat.get().format(perkuliahan.getWaktuSelesaiD())))

						)) {
					sudahAda.add(kode);
					sudahAda.add(kode1);

					if (bentrokRuangans.containsKey(kelasLengkap)) {
						bentrokRuangans.get(kelasLengkap).add(new Perkuliahan[] { perkuliahan, perkuliahanLain });
					} else {
						List<Perkuliahan[]> pps = new ArrayList<Perkuliahan[]>();
						pps.add(new Perkuliahan[] { perkuliahan, perkuliahanLain });
						bentrokRuangans.put(kelasLengkap, pps);
					}
				}
			}
		}
		return bentrokRuangans;

	}

	/**
	 * Sama seperti {@link #checkBentrokBerdasarRuangan(List)}, tetapi mengelompokkan bentrok
	 * berdasarkan kesamaan Dosen (dicek terhadap seluruh slot dosen1..dosen10 pada tiap jadwal) pada
	 * hari dan rentang waktu yang tumpang tindih — mendeteksi dosen yang terjadwal mengajar dua kelas
	 * berbeda pada waktu yang sama.
	 *
	 * @param perkuliahans daftar jadwal yang akan diperiksa saling silang
	 * @return peta {@link Dosen} -> daftar pasangan {@link Perkuliahan} yang bentrok untuk dosen tersebut
	 */
	public static Map<Dosen, List<Perkuliahan[]>> checkBentrokBerdasarDosen(List<Perkuliahan> perkuliahans) {

		Map<Dosen, List<Perkuliahan[]>> bentrokRuangans = new HashMap<Dosen, List<Perkuliahan[]>>();
		Set<String> sudahAda = new HashSet<String>();
		for (Perkuliahan perkuliahan : perkuliahans) {
			if (perkuliahan == null) {
				continue;
			}
			if (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) {
				continue;
			}

			List<Dosen> dosens = perkuliahan.populateDosenBuNama();

			for (Dosen dosen : dosens) {

				for (Perkuliahan perkuliahanLain : perkuliahans) {

					if (perkuliahan == null || perkuliahan.getId().equals(perkuliahanLain.getId())) {
						continue;
					}

					if (perkuliahanLain.getMerupakan_tanpa_jadwal_perkuliahan()) {
						continue;
					}

					List<Dosen> dosensLain = perkuliahanLain.populateDosenBuNama();

					for (Dosen dosenLain : dosensLain) {

						String kode = perkuliahan.getId() + "_" + perkuliahanLain.getId() + "_" + dosen.getId();
						String kode1 = perkuliahanLain.getId() + "_" + perkuliahan.getId() + "_" + dosenLain.getId();

						Long start1 = perkuliahan.getPerkuliahanDimulai() == null ? null
								: perkuliahan.getPerkuliahanDimulai().getTime() + 60L;
						Long end1 = perkuliahan.getPerkuliahanSampai() == null ? null
								: perkuliahan.getPerkuliahanSampai().getTime() - 60L;

						Long start2 = perkuliahanLain.getPerkuliahanDimulai() == null ? null
								: perkuliahanLain.getPerkuliahanDimulai().getTime() + 60;
						Long end2 = perkuliahanLain.getPerkuliahanSampai() == null ? null
								: perkuliahanLain.getPerkuliahanSampai().getTime() - 60L;

						boolean ada = true;
						if (start1 != null && end1 != null && start2 != null && end2 != null) {
							ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
						}

						if (dosen.getId().equals(dosenLain.getId()) && ada
								&& (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
								&& ((perkuliahan.getHari() == null ? "" : perkuliahan.getHari())
										.equals(perkuliahanLain.getHari()))
								&& (

								(perkuliahan.getWaktuMulaiD() >= perkuliahanLain.getWaktuMulaiD() + 0.01
										&& perkuliahan.getWaktuMulaiD() <= perkuliahanLain.getWaktuSelesaiD() - 0.01)

										||

										(perkuliahan.getWaktuSelesaiD() >= perkuliahanLain.getWaktuMulaiD() + 0.01
												&& perkuliahan.getWaktuSelesaiD() <= perkuliahanLain.getWaktuSelesaiD()
														- 0.01)

										||

										(perkuliahanLain.getWaktuMulaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
												&& perkuliahanLain.getWaktuMulaiD() <= perkuliahan.getWaktuSelesaiD()
														- 0.01)

										||

										(perkuliahanLain.getWaktuSelesaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
												&& perkuliahanLain.getWaktuSelesaiD() <= perkuliahan.getWaktuSelesaiD()
														- 0.01)

										||

										(Common.numberFormat.get().format(perkuliahanLain.getWaktuMulaiD())
												.equals(Common.numberFormat.get().format(perkuliahan.getWaktuMulaiD()))
												&& Common.numberFormat.get().format(perkuliahanLain.getWaktuSelesaiD())
														.equals(Common.numberFormat.get()
																.format(perkuliahan.getWaktuSelesaiD())))

								)) {
							sudahAda.add(kode);
							sudahAda.add(kode1);

							if (bentrokRuangans.containsKey(dosen)) {
								bentrokRuangans.get(dosen).add(new Perkuliahan[] { perkuliahan, perkuliahanLain });
							} else {
								List<Perkuliahan[]> pps = new ArrayList<Perkuliahan[]>();
								pps.add(new Perkuliahan[] { perkuliahan, perkuliahanLain });
								bentrokRuangans.put(dosen, pps);
							}
						}
					}
				}
			}
		}
		return bentrokRuangans;

	}
}
