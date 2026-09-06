package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import ais.action.master.helper.AngketPerkuliahanHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardPerkuliahan;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.NeoFeederProgressHelper;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.DetailperkuliahanForPenilaianHelper;
import ais.action.master.helper.DetailperkuliahanHelper;
import ais.action.master.helper.PenjadwalanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiPerkuliahanHelper;
import ais.action.master.helper.SyncHelper;
import ais.action.master.helper.TemplatePerkuliahanDetailHelper;
import ais.action.master.helper.util.PenjadwalanUtil;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.format1.akademik.LaporanAbsensi;
import ais.action.report.format1.akademik.LaporanCoverAbsensi;
import ais.action.report.format1.akademik.LaporanDaftarHadirDosenHarian;
import ais.action.report.format1.akademik.LaporanDosenPembinaMahasiswa;
import ais.action.report.format1.akademik.LaporanDosenPembinaMatakuliah;
import ais.action.report.format1.akademik.LaporanJadwalDosenMengajar;
import ais.action.report.format1.akademik.LaporanJadwalPerkuliahan;
import ais.action.report.format1.akademik.LaporanJadwalPerkuliahanPerAsisten;
import ais.action.report.format1.akademik.LaporanJadwalPerkuliahanPerDosen;
import ais.action.report.format1.akademik.LaporanJadwalPerkuliahanPerMatakuliah;
import ais.action.report.format1.akademik.LaporanJadwalPerkuliahanPerRuangan;
import ais.action.report.format1.akademik.LaporanJadwalPerkuliahanPerSemester;
import ais.action.report.format1.akademik.LaporanSebaranJadwalPerkuliahan;
import ais.action.report.format1.akademik.LaporanRekapDosen;
import ais.action.report.format1.akademik.LaporanRekapDosenMahasiswa;
import ais.action.report.format1.akademik.LaporanSKDosen;
import ais.action.report.format1.akademik.LaporanSKSDosen;
import ais.action.report.format1.akademik.LaporanSKSDosenPerMatakuliah;
import ais.action.report.format1.akademik.LaporanSksDosenWindow;
import ais.common.AsyncTaskManager;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPenjadwalan;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.OnSearchDefaultListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.PerkuliahanPunyaItem;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TugasPertemuan;
import ais.database.model.library.Item;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk perkuliahan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataMatakuliahBanbox searchmatakuliah}, {@code AmbilDataDosenBanbox
 * searchdosen}, {@code AmbilDataMahasiswaBanbox searchasisten}, {@code Checkbox searchaktif}, {@code Combobox
 * searchhari}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * onInitJadwalKurikulum()}, {@code init()}, {@code initCriteria()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onTampilRekapiLaporanDosenPembinaMahasiswa()}, {@code tampilanExportKeFeederSatuan()}, {@code
 * tampilanExportKeFeeder()}, {@code tampilanImportKelasDariFeeder()}, {@code getDspace()}, {@code
 * onTampilDaftarHadirDosen()}); penghapusan/pembatalan ({@code onDeletePerkuliahan()}); pelaporan/ekspor ({@code
 * onCetakLaporanDosenPembinaMatakuliah()}); operasi domain lain ({@code onStatistik()}, {@code onRekapDosen()},
 * {@code onJadwalRemedial()}, {@code generateiIntroductoryText()}, {@code onEkstrakurikuler()}, {@code
 * onJadwalSp()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PerkuliahanAction extends GenericAutowireComposer
		implements OnSearchDefaultListener, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220301468178L;

	protected MyWindow addWindow;

	protected Paging paging;
	protected MyGrid grid;

	protected AmbilDataMatakuliahBanbox searchmatakuliah;
	protected AmbilDataDosenBanbox searchdosen;
	protected AmbilDataMahasiswaBanbox searchasisten;
	private Checkbox searchaktif;
	protected Combobox searchhari;
	protected Combobox searchjmldosen;

	protected AmbilDataKelasBanbox searchkelas;
	protected MyCheckboxConfig searchparalel;
	protected MyCheckboxConfig searchpraktek;
	protected MyCheckboxConfig searchtanpakelas;

	protected MyCheckboxConfig searchmasukFeeder;
	protected MyCheckboxConfig searchbelummasukFeeder;

	protected AmbilDataRuangBanbox searchruang;

	protected Combobox searchTahap;

	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchsemester;

	protected Combobox searchprogram;
	protected Combobox searchObe;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;

	protected AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	protected AmbilDataKurikulumBanbox searchkurikulum;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;

	protected Textbox searchnamadsn;
	protected Textbox searchnamamk;
	// Pencarian cepat "Kode/Nama" di toolbar (server-side). Hanya ada di perkuliahan.zul;
	// null untuk zul lain yang juga apply PerkuliahanAction (jadwal/absensi/penilaian).
	protected Textbox cariCepatPerkuliahan;
	protected Textbox searchKeterangan;
	protected Textbox searchnamaasisten;

	protected boolean edit;
	protected boolean delete;

	protected boolean today = false;

	protected Perkuliahan perkuliahan;
	protected MyToolbarbuttonConfig add;
	protected MyToolbarbuttonConfig addJadwalKurikulum;
	/** Khusus admin: tombol aktif/non-aktifkan penjadwalan untuk TA &amp; Jenis Smt terpilih (mendukung SP). */
	protected MyToolbarbuttonConfig toggleAktifPenjadwalan;
	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected Integer semesterPendek = null;
	protected Integer ekstrakurikuler = null;
	protected Boolean merupakanPraPerkuliahan = false;
	protected Boolean merupakanPerkuliahanUmum = false;

	// protected Tbmuser users;

	protected Tabpanel laporanDosenPembinaMatakuliah;
	protected Tabpanel laporanDosenPembinaMahasiswa;
	protected Tabpanel laporanJadwalPerkuliahan;
	protected Tabpanel jadwalPerkuliahanPerDosen;
	protected Tabpanel jadwalPerkuliahanPerAsisten;
	protected Tabpanel laporanJadwalPerkuliahanPerRuangan;
	protected Tabpanel laporanJadwalPerkuliahanPerMatakuliah;
	protected Tabpanel laporanCoverAbsensi;
	protected Tabpanel laporanAbsensi;
	protected Tabpanel laporanJmlSksDosen;
	protected Tabpanel laporanJmlSksDosenPerMatakuliah;
	protected Tabpanel laporanJadwalPerkuliahanPerSemester;
	protected Tabpanel laporanJadwalDosenMengajar;
	protected Tabpanel laporanSebaranJadwal;
	protected Tabpanel laporanDaftarHadirDosen;
	protected Tabpanel masaPerkuliahanTab;

	protected MyToolbarbuttonConfig copy;
	protected MyToolbarbuttonConfig hapus;
	protected MyToolbarbuttonConfig bersihkan;

	protected MyColumnConfig colMinggu;

	protected MyColumnConfig colwaktu;

	protected AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper;

	private String[] contents = new String[] { "id", "matakuliah", "matakuliah.sks", "dosen1", "dosen2", "dosen3",
			"dosen4", "dosen5", "dosen6", "dosen7", "dosen8", "dosen9", "dosen10", "jurusan", "ruang", "semester",
			"kapasitasKelas", "program", "waktuMulai", "waktuSelesai", "hari", "tahunAjaran", "kelas", "kurikulum",
			"statusSemesterPendek", "merupakan_tanpa_jadwal_perkuliahan", "merupakan_tanpa_dosen",
			"merupakan_tanpa_ruangan", "jamPerkuliahan", "minggu1", "minggu2", "minggu3", "minggu4", "minggu5",
			"perkuliahanDimulai", "perkuliahanSampai", "masaPerkuliahan", "kurikulumPunyaMatakuliah",
			"tampilkanSaatPengambilanKrs", "abaikanWaktuBentrokDenganJadwalLain", "tanggalMulaiPerkuliahan",
			"dosenBisaMerubahTanggalPerkuliahan", "jumlahMaksimalPertemuan", "merupakanPraPerkuliahan",
			"merupakanPerkuliahanUmum", "keterangan", "jumlahDosen", "ambilMkDiluarSemesterKurikulum",
			"sembunyikanNilaiJikaBelumDiverifikasi", "waktuPerkuliahanOnlineBebas", "bolehAbsenWaktuIkutiPerkuliahan",
			"bolehAbsenSebelumWaktuMulaiDalamMenit", "bolehAbsenSetelahWaktuMulaiDalamMenit",
			"batasWaktuBolehAbsenKehadiran", "persenKehadiranDinilai0", "lingkup", "mode", "feeder" };

	private MyToolbarbuttonConfig upload;

	private Long selectedJurusan = null;
	private String tahunAjaran = null;

	private Integer jenisSemester = null;

	private String selectedProgram = null;
	protected Boolean merupakanRemedial = false;

	protected Tabpanel statistik;
	protected Tabpanel laporanRekapDosen;
	protected Tabpanel laporanRekapDosenPembinaMahasiswa;
	private boolean approve;

	private boolean reject;

	private boolean create;

	private MyToolbarbuttonConfig cetakPertemuan;

	private PerguruanTinggi perguruanTinggi;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardPerkuliahan include = new DashboardPerkuliahan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Perkuliahan", "Gambaran sebaran kelas, dosen pengampu, dan tingkat kehadiran perkuliahan.");
		}
	}

	public void onRekapDosen(Event event) {

		if (laporanRekapDosen.getChildren().size() == 0) {
			LaporanRekapDosen include = new LaporanRekapDosen();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(laporanRekapDosen);
		}
	}

	public void onTampilRekapiLaporanDosenPembinaMahasiswa(Event event) {

		if (laporanRekapDosenPembinaMahasiswa.getChildren().size() == 0) {
			LaporanRekapDosenMahasiswa include = new LaporanRekapDosenMahasiswa();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(laporanRekapDosenPembinaMahasiswa);
		}
	}

	public void onJadwalRemedial(Event event) {

		if (jadwalRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(jadwalRemedial);
			include.setSrc("/pages/master/perkuliahan_remedial.zul");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (execution.getParameter("jurusan") != null) {
			selectedJurusan = Long.parseLong(execution.getParameter("jurusan"));
		}
		if (execution.getParameter("program") != null) {
			selectedProgram = execution.getParameter("program");
		}
		if (execution.getParameter("statusSemesterPendek") != null) {
			semesterPendek = execution.getParameter("statusSemesterPendek").equals("null") ? null
					: Integer.parseInt(execution.getParameter("statusSemesterPendek"));
		}
		if (execution.getParameter("jenisSemester") != null) {
			jenisSemester = Integer.parseInt(execution.getParameter("jenisSemester"));
		}
		if (execution.getParameter("tahunAjaran") != null || today) {
			tahunAjaran = execution.getParameter("tahunAjaran");
		} else {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (execution.getParameter("masaPerkuliahan") != null
				&& Common.isNumber(execution.getParameter("masaPerkuliahan"))) {
			MasaPerkuliahan mp = (MasaPerkuliahan) HibernateUtil.currentSession().createCriteria(MasaPerkuliahan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("masaPerkuliahan").trim())))
					.uniqueResult();
			if (mp != null) {
				searchmasaperkulaiahan.setAttribute("masaPerkuliahan", mp);
				searchmasaperkulaiahan.setAttribute("myValue", mp);
				searchmasaperkulaiahan.setName(mp.getNama());
				searchmasaperkulaiahan.setDisabled(true);
			}
		}

		tbmuser = Common.getCurrentUser();
		// Tombol "Aktif/Non-aktifkan Penjadwalan" hanya tampil untuk admin.
		if (toggleAktifPenjadwalan != null) {
			toggleAktifPenjadwalan.setVisible(Common.getApakahAdmin());
		}
		aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
				tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), edit);
		if (searchasisten != null) {

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				searchasisten.setAttribute("mahasiswa", tbmuser.getMahasiswa());
				searchasisten.setValue(tbmuser.getMahasiswa().getNama());
				searchasisten.setDisabled(true);
			}

			searchasisten.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}
		for (String h : Common.haris) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setReadonly(true); }
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }

		if (today) {
			int dayOfweek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
			searchhari.setSelectedIndex(dayOfweek);
			searchhari.setDisabled(true);
		}

		if (searchjmldosen != null) {
			for (Integer h : new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(h.toString());
				comboitem.setValue(h);
				searchjmldosen.appendChild(comboitem);
			}
			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchjmldosen.appendChild(comboitem);
			searchjmldosen.setReadonly(true);
			searchjmldosen.setSelectedItem(comboitem);
		}

		if (searchtanpakelas != null) {
			searchtanpakelas
					.setVisible(Common.bolehKonfigurasi("tampilkan_search_kelas_di_penjadwalan", Konfigurasi.TIDAK_AKTIF));
		}

		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }

		searchkelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchkurikulum.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchmasaperkulaiahan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		colMinggu.setVisible(Common.bolehKonfigurasi("tampilkan_minggu_perkuliahan"));

		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		if (addJadwalKurikulum != null && !merupakanPraPerkuliahan && !merupakanPerkuliahanUmum) {
			addJadwalKurikulum.setVisible((add != null && add.isVisible()));
		}

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Jumlah Mahasiswa");
		columnHeadersAdding.add("Info KRS");
		columnHeadersAdding.add("Telah Disetujui");
		columnHeadersAdding.add("Belum Disetujui");
		columnHeadersAdding.add("Jumlah Realisasi Pertemuan");

		int jumlahPertemuanDefault = 16;
		try {
			jumlahPertemuanDefault = Integer
					.parseInt(Common.getKonfigurasi("jumlah_pertemuan_perkuliahan_default", "16").getNilai());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		for (int i = 1; i <= jumlahPertemuanDefault; i++) {
			columnHeadersAdding.add("Pertemuan ke-" + i);
		}

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Perkuliahan perkuliahan = (Perkuliahan) objects[0];

				XSSFRow row = (XSSFRow) objects[2];

				Integer[] s = perkuliahan.ambilStatusKrs();

				Integer telahDisetujui = s[1];
				Integer belumDisetujui = s[0];

				row.createCell(contents.length + 0).setCellValue(telahDisetujui + belumDisetujui);

				row.createCell(contents.length + 1).setCellValue(perkuliahan.populateInfoPersetujuanBiasa());

				List<String> telah = new ArrayList<String>();
				List<String> belum = new ArrayList<String>();

				Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
				for (Long detailperkuliahanid : detailperkuliahans) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {
						if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)) {
							belum.add(detailperkuliahan.getMahasiswa() == null ? ""
									: detailperkuliahan.getMahasiswa().getNim());
						} else {
							telah.add(detailperkuliahan.getMahasiswa() == null ? ""
									: detailperkuliahan.getMahasiswa().getNim());
						}
					}
				}
				detailperkuliahans = null;

				row.createCell(contents.length + 2).setCellValue(org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(telah.toString(), "[", ""), "]", ""));
				row.createCell(contents.length + 3).setCellValue(org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(belum.toString(), "[", ""), "]", ""));

				TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();
				row.createCell(contents.length + 4).setCellValue(pertemuans.size());
				int index = 5;
				for (Long pertemuanid : pertemuans.values()) {
					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
							pertemuanid.toString());
					if (pertemuan != null) {
						row.createCell(contents.length + index).setCellValue(pertemuan.getTopik());
						index++;
					}
				}
				pertemuans = null;

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Perkuliahan.class, this, "Download",
				"/img/print.png", columnHeadersAdding, dataAdding, false, null, "DATA TAMBAHAN", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		upload = Common.uploadData(this, Perkuliahan.class, contents);
		Common.appendKeToolbar(upload, add, comp);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchJenisSemester.appendChild(comboitem);

		// Filter "Semester Pendek (SP)" — pengganti tab "SP" yang dihilangkan. Bila dipilih, daftar
		// disaring HANYA perkuliahan Semester Pendek (statusSemesterPendek = SEMESTER_PENDEK), tanpa
		// batasan Ganjil/Genap — persis seperti perilaku tab "SP" dulu. Nilai combo = Perkuliahan.SP.
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semester Pendek (SP)"); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.SP); }
		searchJenisSemester.appendChild(comboitem);

		// Default Jenis Semester = SEMESTER SAAT INI (abaikan konfigurasi
			// 'pilihan_semester_di_perkuliahan_dibuat_default_semua_aja'; selalu pakai isNowSemensterGanjil()).
			Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				if (searchJenisSemester.getSelectedItem() == null) {
					return;
				}

				// "Semua" (null) DAN "Semester Pendek (SP)" sama-sama menampilkan seluruh nomor semester
				// (1..29), karena SP tidak dibatasi ganjil/genap.
				if (searchJenisSemester.getSelectedItem().getValue() == null
						|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					for (int i = 1; i < 30; i++) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {

					Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					if (genap) {
						for (int i : Common.genap) {
							if (i == 0)
								continue;
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {
						for (int i : Common.ganjil) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					}
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};

		searchJenisSemester.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.initPrograms(searchprogram);

		// Default filter "Tampilan Kurikulum" = Tampilkan Semua
		if (searchObe != null && searchObe.getItemCount() > 0) {
			searchObe.setSelectedIndex(0);
		}

		// System.out.println(users);
		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			// Common.selectComboItem(searchdosen, dosen);
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setDisabled(true);
		}

		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && jenisSemester == null); }

		if (colwaktu != null) {
			colwaktu.setVisible(
					Common.bolehKonfigurasi("waktu_perkuliahan_pagi_siang_sore_malam_ditampilkan"));
		}

		if (ConstantValues.aktifkanTahapanKurikulum) {
			searchTahap = new Combobox();
			if (ConstantValues.jumlahTahapan.isEmpty()) {
				ConstantValues.initJumlahTahapan();
			}

			MyComboitemConfig comboitemSemua = new MyComboitemConfig("Semua tahap");
			comboitemSemua.setValue(-1);
			searchTahap.appendChild(comboitemSemua);

			for (int i = 1; i <= 15; i++) {
				comboitem = new MyComboitemConfig("Tahap " + i);
				comboitem.setValue(i);
				searchTahap.appendChild(comboitem);
			}
			comboitem = new MyComboitemConfig("Tanpa tahap");
			comboitem.setValue(null);
			searchTahap.appendChild(comboitem);

			searchTahap.setSelectedItem(comboitemSemua);
			Common.appendKeToolbar(searchTahap, add, comp);
			searchTahap.setReadonly(true);
			searchTahap.setWidth("100px");
			searchTahap.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		if (jenisSemester == null) {
			MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Jml SKS", "/img/laptop.png");
			Common.appendKeToolbar(cetakSksDosen, add, comp);
			cetakSksDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanSksDosenWindow laporanSksDosenWindow = new LaporanSksDosenWindow("Monitor SKS", "none",
							true);
					laporanSksDosenWindow.setClosable(true);
					page.getFirstRoot().appendChild(laporanSksDosenWindow);
					laporanSksDosenWindow.setHeight("95%");
					laporanSksDosenWindow.setWidth("90%");
					laporanSksDosenWindow.onModal();
				}
			});
		}

		if (jenisSemester == null) {
			cetakPertemuan = new MyToolbarbuttonConfig("Gen.Pert", "/img/svg/check2.svg");
			Common.appendKeToolbar(cetakPertemuan, add, comp);
			cetakPertemuan.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin generate semua pertemuan pada perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												final List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();

												Radiogroup jenis = new Radiogroup();
												MyRadioConfig minggu;
												jenis.appendChild(minggu = new MyRadioConfig("Mingguan"));
												minggu.setChecked(true);

												jenis.setSelectedItem(minggu);

												MyCheckboxConfig uts = new MyCheckboxConfig("UTS");
												uts.setChecked(true);

												MyCheckboxConfig uas = new MyCheckboxConfig("UAS");
												uas.setChecked(true);

												List<Long> perkuliahanId = initCriteria(true)
														.add(Restrictions.or(
																Restrictions.isNotNull("tanggalMulaiPerkuliahan"),
																Restrictions.isNotNull("masaPerkuliahan")))
														.add(Restrictions.and(Restrictions.isNotNull("hari"),
																Restrictions.ne("hari", "")))
														.setProjection(Projections.property("id")).list();

												for (Long idP : perkuliahanId) {
													Perkuliahan perkuliahan = (Perkuliahan) ConstantValues
															.ambil(Perkuliahan.class.getName(), idP, true);

													Date tglAwal = perkuliahan == null ? null
															: perkuliahan.getTanggalMulaiPerkuliahan();

													if (perkuliahan != null && tglAwal != null) {
														Session sessionData = HibernateUtil.currentNativeSession();
														Date tanggalAwal = (Date) sessionData
																.createCriteria(Pertemuan.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.setProjection(Projections.min("tanggal"))
																.add(Restrictions.isNotNull("tanggal"))
																.add(Restrictions.eq("perkuliahan.id", idP))
																.uniqueResult();
														ais.common.Common.closeOpenedSession(sessionData);

														if (tanggalAwal == null) {

															Date curr = tglAwal;
															if (curr != null) {

																Calendar myCalendar = ais.ui.util.WaktuUtil
																		.getCalendar();
																myCalendar.setTime(curr);

																for (int i = 1; i <= perkuliahan
																		.getJumlahMaksimalPertemuan(); i++) {

																	if (perkuliahan.getLewatiTanggalMerahNasional()) {
																		myCalendar = Common.tanggalMerah(jenis,
																				myCalendar);
																	}

																	Date currDate = myCalendar.getTime();
																	System.out.println("currDate pertemuan -> "
																			+ Common.dateFormat6.get().format(currDate));
																	Pertemuan p = PenjadwalanHelper.buatPertemuan(
																			perkuliahan,
																			perkuliahan.getKurikulumPunyaMatakuliah(),
																			i, currDate, uts, uas, null, null);
																	if (p != null && p.getId() != null) {
																		pertemuans.add(p);
																	}
																	myCalendar = Common.curreDate(jenis, myCalendar);
																}
															}
														} else if (tanggalAwal != null && tanggalAwal.before(tglAwal)) {
															List<Pertemuan> pertemuansList = perkuliahan
																	.ambilPertemuanList();
															Date curr = tglAwal;

															Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
															myCalendar.setTime(curr);

															for (Pertemuan pertemuan : pertemuansList) {

																sessionData = HibernateUtil.currentNativeSession();
																sessionData.refresh(pertemuan);

																if (perkuliahan.getLewatiTanggalMerahNasional()) {
																	myCalendar = Common.tanggalMerahAja(jenis,
																			myCalendar);
																}

																Date currDate = myCalendar.getTime();
																System.out.println("currDate pertemuan -> "
																		+ Common.dateFormat6.get().format(currDate));
																pertemuan.setTanggal(currDate);

																pertemuan.setMulai(currDate);
																pertemuan.setSelesai(null);
																if (perkuliahan != null) {
																	pertemuan
																			.setWaktuMulai(perkuliahan.getWaktuMulai());
																	pertemuan.setWaktuSelesai(
																			perkuliahan.getWaktuSelesai());
																}

																sessionData.getTransaction().begin();
																Common.refreshSaveOrUpdate(sessionData, pertemuan);
																sessionData.getTransaction().commit();
																ais.common.Common.closeOpenedSession(sessionData);

																if (pertemuan != null && pertemuan.getId() != null) {
																	pertemuans.add(pertemuan);
																}

																myCalendar = Common.curreDate(jenis, myCalendar);

															}
														} else {
															List<Pertemuan> pertemuansList = perkuliahan
																	.ambilPertemuanList();
															for (Pertemuan pertemuan : pertemuansList) {
																if (pertemuan != null && pertemuan.getId() != null) {
																	pertemuans.add(pertemuan);
																}
															}

														}
													}
												}

												EventListener eventListener = (EventListener) Common
														.cetakDataCustomButton(Mahasiswa.class,
																new DataCriteriaWithColumn() {

																	@Override
																	public Object[] initCriteria(boolean order) {

																		try {
																			String[] contents = new String[] { "id",
																					"indikator", "topik",
																					"metodePembelajaran",
																					"pengalamanBelajar",
																					"waktupembelajaran",
																					"tugasDanPenilaian", "catatan",
																					"bukuRujukan1", "bukuRujukan2",
																					"dosenTamu", "dosenTamu", "tanggal",
																					"statusPertemuan", "ruang",
																					"waktuMulai", "waktuSelesai" };
																			return new Object[] { pertemuans,
																					contents };

																		} catch (Exception e) {
																			Common.tampilErrorJikaAdmin(e);
																		}
																		return null;
																	}

																}, null, "Download Data", "/img/print.png", null, null,
																false, null, "DATA TAMBAHAN",
																new String[] { "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "", "", "", "", "", "", "", "", "", "", "",
																		"", "" })
														.getAttribute("eventListener");

												eventListener.onEvent(null);

											}
										});
									}

								}
							});

				}
			});
		}
		if (jenisSemester == null) {
			MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("SK Dosen", "/img/Document-Write-icon.png");
			Common.appendKeToolbar(cetakSksDosen, add, comp);
			cetakSksDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					LaporanSKDosen laporanSKDosen = new LaporanSKDosen("SK Dosen", "none", true);
					laporanSKDosen.setClosable(true);
					page.getFirstRoot().appendChild(laporanSKDosen);
					laporanSKDosen.setHeight("95%");
					laporanSKDosen.setWidth("90%");
					laporanSKDosen.onModal();

				}
			});
		}

		if (jenisSemester == null) {
			MyToolbarbuttonConfig generatePasswordDosen = new MyToolbarbuttonConfig("Pwd Dosen", "/img/key_Layer.gif");
			generatePasswordDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Anda akan membuatkan dan mengambil username dan password dosen.",
							"Informasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final String filename = Sessions.getCurrent().getWebApp()
												.getRealPath("/tmp/user_password_dosen_"
														+ URLEncoder.encode(Common.datetimeFormat2s.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".xlsx");

										Session session = HibernateUtil.currentSession();
										List<Perkuliahan> perkuliahans = initCriteria(false).setMaxResults(1048576)
												.list();

										Map<Long, Dosen> dosensData = new HashMap<Long, Dosen>();

										TreeMap<Long, List<Perkuliahan>> dosensMap = new TreeMap<Long, List<Perkuliahan>>();
										for (Perkuliahan perkuliahan : perkuliahans) {

											Map<String, Dosen> map = perkuliahan.populateDosen();
											for (Dosen d : map.values()) {
												if (dosensMap.containsKey(d.getId())) {
													dosensMap.get(d.getId()).add(perkuliahan);
												} else {
													dosensData.put(d.getId(), d);
													List<Perkuliahan> itemDetails = new ArrayList<Perkuliahan>();
													itemDetails.add(perkuliahan);
													dosensMap.put(d.getId(), itemDetails);
												}
											}

										}

										List<Dosen> dosenList = new ArrayList<Dosen>(dosensData.values());
										Collections.sort(dosenList);

										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DOSEN");
										sheet.setDefaultColumnWidth(20);
										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("ID");
										rowhead.createCell(1).setCellValue("Username");
										rowhead.createCell(2).setCellValue("Password");
										rowhead.createCell(4).setCellValue("Prodi");
										rowhead.createCell(3).setCellValue("Fakultas");
										rowhead.createCell(5).setCellValue("Nama Lengkap");
										rowhead.createCell(6).setCellValue("Email");
										rowhead.createCell(7).setCellValue("HP");
										rowhead.createCell(8).setCellValue("SKS");
										rowhead.createCell(9).setCellValue("Matkul");

										for (Dosen dosen : dosenList) {
											if (dosen.getNama() != null && !dosen.getNama().trim().isEmpty()) {
												rowIndex++;
												Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("dosen", dosen)).setMaxResults(1)
														.uniqueResult();
												if (tbmuser == null || tbmuser.getUserId() == null) {
													tbmuser = new Tbmuser();

													String newUsername = StringUtils.split(dosen.getNama(), " ")[0] + ""
															+ RandomStringUtils.randomNumeric(3);

													newUsername = newUsername.toLowerCase().trim();

													tbmuser.setUserId(newUsername);
													tbmuser.setEmail(dosen.getEmail());
													tbmuser.setFakultas(dosen.getFakultas());
													tbmuser.setIs_encripted(true);
													tbmuser.setJurusan(dosen.getJurusan());
													tbmuser.setRoot(false);
													tbmuser.setUserNama(dosen.getNama());
													String passw = RandomStringUtils.randomNumeric(5);
													tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
													tbmuser.setUserRole(ConstantValues.roleDosen);
													tbmuser.setUserShow(1);
													tbmuser.setDosen(dosen);
													Common.refreshSaveOrUpdate(tbmuser);

													Common.saveOrUpdateUserAccess(tbmuser, null, tbmuser.getUserId(),
															passw.trim(), tbmuser.getEmail());

												}

												List<Perkuliahan> perkulishsnasDosen = dosensMap.get(dosen.getId());
												Integer sks = 0;
												String itemYangDipinjam = "";
												for (Perkuliahan perkul : perkulishsnasDosen) {
													sks += perkul.getMatakuliah().getSks();
													String s = perkul.getMatakuliah().getKode() + "-"
															+ perkul.getMatakuliah() + "=>"
															+ perkul.getMatakuliah().getSks() + "sks";
													itemYangDipinjam += itemYangDipinjam.isEmpty() ? s : " ," + s;
												}

												XSSFRow row = sheet.createRow(rowIndex);
												row.createCell(0).setCellValue(dosen.getId());
												row.createCell(1).setCellValue(tbmuser.getUserId());

												try {
													row.createCell(2).setCellValue(
															Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()));
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

												row.createCell(3).setCellValue(tbmuser.ambilFakultas() == null ? ""
														: tbmuser.ambilFakultas().getNama());
												row.createCell(4).setCellValue(tbmuser.ambilJurusan() == null ? ""
														: tbmuser.ambilJurusan().getNama());
												row.createCell(5).setCellValue(dosen.getNama());
												row.createCell(6).setCellValue(dosen.getEmail());
												row.createCell(7).setCellValue(dosen.getTelp());
												row.createCell(8).setCellValue(sks);
												row.createCell(9).setCellValue(itemYangDipinjam);
											}
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											// TODO Auto-generated catch
											// block
											Common.tampilErrorJikaAdmin(e);
										}

										try {
											File file = new File(filename);
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}

								}
							});

				}
			});
			Common.appendKeToolbar(generatePasswordDosen, add, comp);
			generatePasswordDosen.setVisible((add != null && add.isVisible()) && edit && tbmuser != null && tbmuser != null
					&& tbmuser.hakAkses() != null && tbmuser.hakAkses() != null && tbmuser != null
					&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
					&& (tbmuser != null && tbmuser.hakAkses() != null
							&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
			upload.setVisible(generatePasswordDosen.isVisible());
		}

		if (selectedJurusan != null) {
			Jurusan jurusan = (Jurusan) HibernateUtil.currentSession().createCriteria(Jurusan.class)
					.add(Restrictions.idEq(selectedJurusan)).uniqueResult();
			Common.selectComboItem(searchjurusan, jurusan, true);
			searchjurusan.setDisabled(true);
			Common.selectComboItem(searchfakultas, jurusan.getFakultas(), true);
			searchfakultas.setDisabled(true);
		}
		if (tahunAjaran != null) {
			Common.selectComboItem(searchTahunAjaran, tahunAjaran);
			searchTahunAjaran.setDisabled(true);
		}

		if (selectedProgram != null) {
			if (selectedProgram.equals("null")) {
				Common.selectComboItem(searchprogram, null);
			} else {
				Common.selectComboItem(searchprogram, selectedProgram);
			}

			searchprogram.setDisabled(true);
		}

		if (jenisSemester != null) {
			if (jenisSemester.equals(1)) {
				Common.selectComboItem(searchJenisSemester, Perkuliahan.GANJIL);
			} else if (jenisSemester.equals(2)) {
				Common.selectComboItem(searchJenisSemester, Perkuliahan.GENAP);
			} else {
				Common.selectComboItem(searchJenisSemester, null);
			}
			searchJenisSemester.setDisabled(true);
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		if (jenisSemester == null) {
			MyToolbarbuttonConfig checkBentrok = new MyToolbarbuttonConfig("Bentrok", "/img/Record-Normal-icon.png");
			Common.appendKeToolbar(checkBentrok, add, comp);
			checkBentrok.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					PenjadwalanUtil.lihatJadwalBentrok();
				}
			});

			// 1. Tombol: Singkronkan Semua
			boolean isSinkronSemuaAktif = Common.bolehKonfigurasi("aktifkan_tombol_sinkronkan_semua");
			SyncHelper.buatTombolSinkronisasi(add.getParent(), "Singkronkan", "/img/svg/check2.svg",
					isSinkronSemuaAktif, "Proses singkronisasi perkuliahan", new SyncHelper.SyncDataProvider() {
						@SuppressWarnings("unchecked")
						@Override
						public List<Perkuliahan> getData() throws Exception {
							return ConstantValues.simpleList(initCriteria(true), Perkuliahan.class);
						}
					}, new SyncHelper.SyncAction() {
						@Override
						public void execute(Perkuliahan p, Session session) throws Exception {
							p.singkronkan(session);
						}
					});

			// 2. Tombol: Singkronkan Mahasiswa (Syn.Mhs)
			SyncHelper.buatTombolSinkronisasi(add.getParent(), "Syn.Mhs", "/img/svg/check2.svg",
					Common.getApakahAdmin(), "Proses singkronisasi mahasiswa", new SyncHelper.SyncDataProvider() {
						@SuppressWarnings("unchecked")
						@Override
						public List<Perkuliahan> getData() throws Exception {
							return ConstantValues.simpleList(initCriteria(true), Perkuliahan.class);
						}
					}, new SyncHelper.SyncAction() {
						@Override
						public void execute(Perkuliahan p, Session session) throws Exception {
							p.singkronkanMhs(session);
						}
					});

			// 3. Tombol: Singkronkan Tugas (Syn.Tugas)
			SyncHelper.buatTombolSinkronisasi(add.getParent(), "Syn.Tugas", "/img/svg/check2.svg",
					Common.getApakahAdmin(), "Proses singkronisasi tugas", new SyncHelper.SyncDataProvider() {
						@SuppressWarnings("unchecked")
						@Override
						public List<Perkuliahan> getData() throws Exception {
							return ConstantValues.simpleList(initCriteria(true), Perkuliahan.class);
						}
					}, new SyncHelper.SyncAction() {
						@Override
						public void execute(Perkuliahan p, Session session) throws Exception {
							p.reInitTugas(session);
						}
					});

			// 4. Tombol: Singkronkan Ujian (Syn.Ujian) -> sinkron PESERTA ujian + SOAL-soal ujian
			SyncHelper.buatTombolSinkronisasi(add.getParent(), "Syn.Ujian", "/img/svg/check2.svg",
					Common.getApakahAdmin(), "Proses singkronisasi ujian", new SyncHelper.SyncDataProvider() {
						@SuppressWarnings("unchecked")
						@Override
						public List<Perkuliahan> getData() throws Exception {
							return ConstantValues.simpleList(initCriteria(true), Perkuliahan.class);
						}
					}, new SyncHelper.SyncAction() {
						@Override
						public void execute(Perkuliahan p, Session session) throws Exception {
							p.reInitUjian(session);
						}
					});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setDisabled(!edit);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiPerkuliahanHelper revisiHelper = new RevisiPerkuliahanHelper(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			button.setParent(add.getParent());

			PerkuliahanAction.tampilanExportKeFeeder(add.getParent(), tbmuser, this, this);
			tampilanImportKelasDariFeeder(add.getParent(), tbmuser, searchTahunAjaran, searchJenisSemester,
					searchjurusan, this);
		}

	        FilterLanjutHelper.setup(comp);
}

	public static void tampilanExportKeFeederSatuan(Component parent, Tbmuser tbmuser,
			final DataSearchDefault dataSearch, final Perkuliahan perkuliahan) {
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
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
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();

										final Label myLabelProsesDetail = NeoFeederProgressHelper.show("Sinkronisasi Neo Feeder", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(err, "Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													/* KE-FIX IOException "File '/opt/ecampus/error_xxx.txt' could not be
													 * created": berkas log ditulis ke direktori ABSOLUT /opt/ecampus yang
													 * di server produksi tidak dapat ditulisi oleh user Tomcat (mkdirs()
													 * gagal diam-diam, lalu writeStringToFile melempar IOException).
													 * Exception itu keluar dari event listener ZK sehingga dibungkus
													 * UiException: sinkronisasi NeoFeeder BERHENTI dan baris berikutnya
													 * (dataSearch.onSearchDefault) tidak pernah jalan -- padahal isi error
													 * sudah ditampilkan di Messagebox. Sekarang berkas dibuat di direktori
													 * temporer JVM yang dijamin dapat ditulisi, dan kegagalan menyimpan
													 * berkas diagnostik TIDAK BOLEH menggagalkan alur: cukup dicatat. */
													try {
														File dirTemp = new File(System.getProperty("java.io.tmpdir"));
														if (!dirTemp.exists()) {
															dirTemp.mkdirs();
														}
														File file = new File(dirTemp,
																"error_" + Common.randLong() + ".txt");
														FileUtils.writeStringToFile(file, err);
														Filedownload.save(file, "text/plain");
													} catch (Exception eSimpanLog) {
														ais.common.ErrorAuditUtil.record(eSimpanLog,
																"PerkuliahanAction: gagal menyimpan berkas log error sinkronisasi NeoFeeder");
													}
												}

												dataSearch.onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);
													// FIX IllegalStateException "Components can be accessed only in
													// event listeners": .setValue() langsung dari thread latar tidak
													// aman (lihat NeoFeederProgressHelper.updateProgres javadoc).
													// Pakai varian progres RINCI (per-mahasiswa) + log langkah agar
													// exception di tahap mana pun terlihat & bisa didiagnosis.
													try {
														kirimKeFeeder(feederImporter, perkuliahan, feederConnector,
																token, null, errorLog, myLabelProsesDetail, 1, 1);
													} catch (Exception e) {
														errorLog.add("[" + perkuliahan.info() + "] " + e.getMessage());
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

													NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port) hanya dicatat ke log admin lalu progres
													// diset "" (=SUKSES palsu) di luar try, menutupi kegagalan.
													ais.common.Common.tampilErrorJikaAdmin(e);
													NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
															"Error: " + PesanFormalHelper.pesanGagalException(
																	"pengiriman data perkuliahan \"" + perkuliahan.info()
																			+ "\" ke Neo Feeder",
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
			buttonTagihan.setParent(parent);
		}
	}

	public static void tampilanExportKeFeeder(Component parent, Tbmuser tbmuser, final DataSearchDefault dataSearch,
			final DataCriteria criteria) {
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
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
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = NeoFeederProgressHelper.show("Sinkronisasi Neo Feeder", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												dataSearch.onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, myLabelProsesDetail);

													List<Perkuliahan> tbmusers = ConstantValues.simpleList(
															(Criteria) criteria.initCriteria(true), Perkuliahan.class);
													int size = tbmusers.size();
													int sukses = 0;
													int gagal = 0;
													long tMulai = System.currentTimeMillis();
													logLangkahFeeder("MULAI kirim " + size + " perkuliahan ke Neo Feeder.");
													int index = 1;
													for (Perkuliahan perkuliahan : tbmusers) {
														// FIX IllegalStateException "Components can be accessed only
														// in event listeners": .setValue() langsung dari thread latar
														// tidak aman (lihat NeoFeederProgressHelper.updateProgres).
														// Pakai varian progres RINCI (per-kelas + per-mahasiswa) + log
														// langkah, dan BUNGKUS PER-PERKULIAHAN agar satu kegagalan
														// TIDAK menghentikan sisa batch (sebelumnya satu exception di
														// tengah proses membuat SISA data tidak pernah diproses tanpa
														// pesan yang terlihat pengguna).
														long tKelas0 = System.currentTimeMillis();
														try {
															kirimKeFeeder(feederImporter, perkuliahan, feederConnector,
																	token, null, errorLog, myLabelProsesDetail, index,
																	size);
															sukses++;
														} catch (Exception e) {
															gagal++;
															String pesan = "[Perkuliahan " + index + "/" + size + ": "
																	+ perkuliahan.info() + "] " + e.getMessage();
															errorLog.add(pesan);
															logLangkahFeeder(pesan);
															ais.common.Common.tampilErrorJikaAdmin(e);
														}
														logLangkahFeeder("Perkuliahan " + index + "/" + size + " ("
																+ perkuliahan.info() + ") SELESAI diproses ("
																+ (System.currentTimeMillis() - tKelas0) + " ms)");
														index++;
													}
													tbmusers.clear();
													tbmusers = null;
													logLangkahFeeder("SELESAI kirim ke Neo Feeder: " + sukses
															+ " berhasil, " + gagal + " gagal, dari total " + size
															+ " perkuliahan (" + (System.currentTimeMillis() - tMulai)
															+ " ms total).");
													NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port/query awal) hanya dicatat ke log admin lalu
													// progres diset "" (=SUKSES palsu) di luar try.
													ais.common.Common.tampilErrorJikaAdmin(e);
													NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
															"Error: " + PesanFormalHelper.pesanGagalException(
																	"pengiriman data perkuliahan (Nilai/Perkuliahan) ke Neo Feeder",
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
			parent.appendChild(buttonTagihan);

		}
	}

	/**
	 * Tombol "Ambil dari Feeder": mengambil (impor) data kelas kuliah / jadwal
	 * perkuliahan dari Neo Feeder berdasarkan Tahun Akademik dan Semester yang
	 * dipilih pada filter ({@code searchTahunAjaran} + {@code searchJenisSemester}).
	 *
	 * <p>Pola pengambilan mengikuti tombol "Ambil Nilai" (nilai dari feeder) di
	 * {@code MahasiswaAction}: buka koneksi + token, jalankan di thread terpisah
	 * dengan load bar, lalu impor tiap record. Daftar kelas diambil via
	 * {@code GetListKelasKuliah} (difilter {@code id_semester}), dan tiap kelas
	 * diimpor lewat {@link MahasiswaAction#ambilPerkuliahanDariFeeder(FeederConnector, String, String)}
	 * (yang memanggil {@code GetDetailKelasKuliah} + {@code FeederJSONImport.perkuliahan}).</p>
	 */
	public static void tampilanImportKelasDariFeeder(final Component parent, final Tbmuser tbmuser,
			final Combobox searchTahunAjaran, final Combobox searchJenisSemester, final Combobox searchjurusan,
			final DataSearchDefault dataSearch) {
		if (parent == null) {
			return;
		}

		boolean bolehAksesFeeder = tbmuser != null && Common.getApakahAdminBolehAksesFeeder();
		boolean feederAktif = Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder");
		MyToolbarbuttonConfig buttonAmbil = new MyToolbarbuttonConfig("Ambil Jadwal dari Feeder",
				"/img/Button-Refresh-icon.png");
		buttonAmbil.setTooltiptext("Ambil/penarikan jadwal kelas kuliah dari Neo Feeder sesuai filter Tahun Akademik, Semester, dan Prodi.");
		parent.appendChild(buttonAmbil);
		if (!bolehAksesFeeder || !feederAktif) {
			buttonAmbil.setDisabled(true);
			buttonAmbil.setTooltiptext(!feederAktif
					? "Tombol nonaktif karena konfigurasi aktifkan_terhubung_langsung_ke_feeder belum aktif."
					: "Tombol nonaktif karena user ini belum memiliki akses admin Feeder.");
			return;
		}
		buttonAmbil.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// Tahun Akademik wajib dipilih agar id_semester feeder bisa dibentuk.
				if (searchTahunAjaran == null || searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Silakan pilih Tahun Akademik terlebih dahulu", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				String tahunAjaran = (String) searchTahunAjaran.getSelectedItem().getValue();
				String tahun = tahunAjaran.split("/")[0];

				// Jenis semester (Ganjil/Genap) -> digit id_semester feeder (1/2). Null = Semua.
				String jenisSmt = (searchJenisSemester == null || searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null) ? null
								: (String) searchJenisSemester.getSelectedItem().getValue();

				String filterFeeder;
				String infoSmt;
				if (jenisSmt == null) {
					// Semua semester dalam tahun akademik ini (ganjil, genap, & pendek).
					filterFeeder = "id_semester like '" + tahun + "%'";
					infoSmt = tahunAjaran + " (Semua Semester)";
				} else {
					String id_smt = tahun + (jenisSmt.equalsIgnoreCase(Perkuliahan.GENAP) ? "2" : "1");
					filterFeeder = "id_semester = '" + id_smt + "'";
					infoSmt = tahunAjaran + " " + jenisSmt;
				}

				// Bila prodi dipilih & punya kode feeder, persempit ke prodi tersebut.
				Jurusan jurusan = (searchjurusan == null || searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null) ? null
								: (Jurusan) searchjurusan.getSelectedItem().getValue();
				if (jurusan != null && jurusan.getFeeder() != null && !jurusan.getFeeder().trim().isEmpty()) {
					filterFeeder += " AND id_prodi = '" + jurusan.getFeeder().trim() + "'";
					infoSmt += " - " + jurusan.getNama();
				}

				final String filter = filterFeeder;
				final String infoSemester = infoSmt;

				MyMessageboxConfig.show(
						"Data kelas kuliah (beserta dosen pengajar & peserta mahasiswa) untuk " + infoSemester
								+ " akan diambil dari Feeder.\nApakah Anda yakin ingin melanjutkan ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}

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

								final List<String> errorLog = new ArrayList<String>();
								final Label myLabelProsesDetail = NeoFeederProgressHelper.show("Sinkronisasi Neo Feeder", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										if (arg0 != null && !arg0.getName().isEmpty()) {
											EksporFromFeederAction.display();
											MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
										}

										if (!errorLog.isEmpty()) {
											String err = "";
											for (String s : errorLog) {
												err += err.isEmpty() ? s
														: "\n----------------------------------------------------------------------------------------------------------\n"
																+ s;
											}

											MyMessageboxConfig.show(
													"Error Terjadi, catatan error akan otomatis ter-download",
													"Error Terjadi", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);

											File file = new File("/opt/ecampus/error_" + Common.randLong() + ".txt");
											if (!file.getParentFile().exists()) {
												file.getParentFile().mkdirs();
											}
											FileUtils.writeStringToFile(file, err);
											Filedownload.save(file, "text/plain");
										}

										dataSearch.onSearchDefault(null);
									}
								});

								new Thread(new Runnable() {

									@Override
									public void run() {
										try {
											FeederConnector feederConnector = new FeederConnector(ip,
													Integer.parseInt(port), null);

											String token = feederConnector.getToken(username, password);
											System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));

											if (token == null || token.trim().isEmpty()
													|| token.trim().toLowerCase().startsWith("error")) {
												NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
														"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
												return;
											}

											// Total (best-effort) hanya untuk persentase progres.
											int total = 0;
											try {
												Integer totalObj = feederConnector.getCount(token,
														"GetCountKelasKuliah", filter);
												total = (totalObj == null || totalObj < 0) ? 0 : totalObj;
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}

											int limit = 100;
											int offset = 0;
											int index = 0;
											boolean lanjut = true;
											while (lanjut) {
												JSONArray dataKelas = feederConnector.getData("GetListKelasKuliah",
														token, filter, "", "" + limit, "" + offset);
												if (dataKelas == null || dataKelas.length() == 0) {
													break;
												}

												for (int k = 0; k < dataKelas.length(); k++) {
													JSONObject kelas = dataKelas.getJSONObject(k);
													index++;

													String id_kls = kelas.isNull("id_kelas_kuliah")
															? (kelas.isNull("id_kelas") ? null
																	: kelas.getString("id_kelas"))
															: kelas.getString("id_kelas_kuliah");
													String namaKelas = kelas.isNull("nama_kelas_kuliah") ? ""
															: kelas.getString("nama_kelas_kuliah");
													String namaMk = kelas.isNull("nama_mata_kuliah") ? ""
															: kelas.getString("nama_mata_kuliah");

													// FIX IllegalStateException "Components can be accessed only
													// in event listeners": .setValue() langsung dari thread latar
													// tidak aman (lihat NeoFeederProgressHelper.updateProgres).
													NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
															"Memproses " + namaMk + " " + namaKelas
																	+ (total > 0
																			? " (" + Common.numberFormat.get()
																					.format((index * 100.0) / total) + "%)"
																			: " (" + index + ")"));

													if (id_kls == null || id_kls.trim().isEmpty()) {
														continue;
													}

													try {
														// Impor kelas SEKALIGUS dosen pengajar & peserta (mahasiswa/KRS).
														MahasiswaAction.ambilKelasLengkapDariFeeder(feederConnector,
																token, id_kls, tbmuser);
													} catch (Exception e) {
														errorLog.add("Gagal impor kelas " + namaMk + " " + namaKelas
																+ " (" + id_kls + ") : " + e.getMessage());
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}

												if (dataKelas.length() < limit) {
													lanjut = false;
												} else {
													offset += limit;
												}
											}

											if (index == 0) {
												NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
														"Tidak ada data kelas kuliah di Feeder untuk " + infoSemester);
												Thread.sleep(2500);
											}
											NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
										} catch (Exception e) {
											// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
											// konek/query ke Feeder) hanya dicatat ke log admin lalu progres
											// diset "" (=SUKSES palsu) di luar try.
											ais.common.Common.tampilErrorJikaAdmin(e);
											NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
													"Error: " + PesanFormalHelper.pesanGagalException(
															"pengambilan data kelas kuliah dari Neo Feeder",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan Tahun Akademik dan Jenis Semester yang dipilih sudah benar.",
																	"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
															.replace("\n", " "));
										}
									}
								}).start();
							}
						});
			}
		});
	}

	protected Tabpanel jadwalSp;

	protected Tabpanel jadwalRemedial;

	@SuppressWarnings("unchecked")
	public static String generateiIntroductoryText(Perkuliahan perkuliahan) {
		if (perkuliahan.getMatakuliah() == null) {
			return "";
		}
		String introductoryText = "<h3>1. Identitas Matakuliah</h3><table style='border: 0px solid black;width: 100%;'><tr><td style='width: 20%;'>"
				+ (perkuliahan.getJurusan() == null ? ""
						: "Fakultas" + "</td><td>" + perkuliahan.getJurusan().getFakultas().getNama())
				+ "</td></tr>";

		introductoryText += (perkuliahan.getJurusan() == null ? ""
				: "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Jurusan") + "</td><td>"
						+ perkuliahan.getJurusan().getNama() + "</td></tr>");

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Kode Matakuliah") + "</td><td>"
				+ perkuliahan.getMatakuliah().getKode() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Nama Matakuliah") + "</td><td>"
				+ perkuliahan.getMatakuliah().getNama() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Semester") + "</td><td>"
				+ perkuliahan.getSemester() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("SKS") + "</td><td>"
				+ perkuliahan.getMatakuliah().getSks() + " sks</td></tr>";

		String pengampu = "";
		for (Dosen d : perkuliahan.populateDosen().values()) {
			String m = d.getNama();
			pengampu += pengampu.isEmpty() ? m : ",<br></br>" + m;
		}

		introductoryText += "<tr><td style='width: 20%;vertical-align: top;'>"
				+ Common.getBahasaConfig("Pengampu Perkuliahan") + "</td><td>" + pengampu + "</td></tr>";

		String peserta = "";

		Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					String m = detailperkuliahan.getMahasiswa().getNim() + " - "
							+ detailperkuliahan.getMahasiswa().getNama();
					peserta += peserta.isEmpty() ? m : ",<br></br>" + m;
				}
			}
		}
		detailperkuliahans = null;

		if (!perkuliahan.getDeskripsiPembelajaran().isEmpty()
				&& perkuliahan.getMatakuliah().getDeskripsiPembelajaran().isEmpty()) {

		}

		introductoryText += "<tr><td style='width: 20%;vertical-align: top;'>"
				+ Common.getBahasaConfig("Peserta Perkuliahan") + "</td><td>" + peserta + "</td></tr></table>";

		introductoryText += "<h3>2.	" + Common.getBahasaConfig("Deskripsi Pembelajaran") + "</h3><p>"
				+ perkuliahan.getDeskripsiPembelajaran().replaceAll("\n", "<br></br>") + "</p>";

		introductoryText += "<h3>3.	" + Common.getBahasaConfig("Capaian / Kompetensi") + "</h3><p>"
				+ perkuliahan.getCapaianPembelajaranProdi().replaceAll("\n", "<br></br>") + "</p>";

		introductoryText += "<h3>4.	" + Common.getBahasaConfig("Rencana Pembelajaran") + "</h3>";

		introductoryText += "<table style='border: 1px solid black;width: 100%;'>";
		introductoryText += "<tr style='border: 1px solid #ddd;'>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Minggu ke") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>"
				+ Common.getBahasaConfig("Kemampuan Akhir Pembelajaran") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>"
				+ Common.getBahasaConfig("Indikator Capaian Pembelajaran") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Bahan Kajian") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Metode Pembelajaran")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Pengalaman Belajar")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Waktu Pembelajaran")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Tugas dan Penilaian")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Sumber Belajar") + "</th>";
		introductoryText += "</tr>";
		int i = 1;
		TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				introductoryText += "<tr style='border: 1px solid #ddd;hover {background-color: rgba(169,169,169,0.4);}'>";
				introductoryText += "<td style='border: 1px solid #ddd;text-align:center;'>" + i + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getTopik().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getIndikator().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getBukuRujukan1().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getMetodePembelajaran().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getPengalamanBelajar().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getWaktupembelajaran().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getTugasDanPenilaian().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getBukuRujukan2().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "</tr>";

				String textContent = "";

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=1'>Catatan</a> ";

				Number ujian = pertemuan.ambilJumlahPertemuanPunyaUjian();
				Number diskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=6'>Ujian "
						+ (ujian.intValue() > 0 ? ":" + Common.numberFormat.get().format(ujian.intValue()) : "") + "</a> ";

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=7'>Diskusi "
						+ (diskusi.intValue() > 0 ? ":" + Common.numberFormat.get().format(diskusi.intValue()) : "")
						+ "</a> ";

				Number file = pertemuan.ambilJumlahPertemuanFileContent();

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=2'>Materi "
						+ (file.intValue() > 0 ? ":" + Common.numberFormat.get().format(file.intValue()) : "") + "</a> ";

				String namaTugas = (pertemuan.getJudultugas().trim().isEmpty() ? "Tidak ada tugas"
						: pertemuan.getJudultugas().length() > 30 ? pertemuan.getJudultugas().substring(0, 30) + "..."
								: pertemuan.getJudultugas());

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=3'> " + namaTugas + "</a> ";

				for (TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal().values()) {
					namaTugas = (tugasPertemuan.getJudultugas().trim().isEmpty() ? "Tidak ada tugas"
							: tugasPertemuan.getJudultugas().length() > 30
									? tugasPertemuan.getJudultugas().substring(0, 30) + "..."
									: tugasPertemuan.getJudultugas());
					textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol()
							+ "/pertemuan.zul?id=" + pertemuanid + "&info=3&tugas=" + tugasPertemuan.getId() + "'> "
							+ namaTugas + "</a> ";
				}

				Number audio = pertemuan.ambilJumlahAudioPertemuan();
				Number video = pertemuan.ambilJumlahVideoPertemuan();

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=4'>Audio "
						+ (audio.intValue() > 0 ? ":" + Common.numberFormat.get().format(audio.intValue()) : "") + "</a> ";

				textContent += "<a target='_blank' href='" + Common.getRequestHostWithProtocol() + "/pertemuan.zul?id="
						+ pertemuanid + "&info=5'>Video "
						+ (video.intValue() > 0 ? ":" + Common.numberFormat.get().format(video.intValue()) : "") + "</a> ";

				introductoryText += "<tr style='border: 1px solid #ddd;hover {background-color: rgba(169,169,169,0.4);}'>";
				introductoryText += "<td style='border: 1px solid #ddd;text-align:left;' colspan=\"9\">" +

						textContent

						+ "</td>";
				introductoryText += "</tr>";

				i++;
			}
		}
		pertemuans = null;

		introductoryText += "</table>";

		introductoryText += "<h3>5.	" + Common.getBahasaConfig("Daftar Rujukan") + "</h3><ol>";

		Session session = HibernateUtil.currentSession();
		List<Item> items = session.createCriteria(PerkuliahanPunyaItem.class)
				.setProjection(Projections.groupProperty("item")).add(Restrictions.eq("perkuliahan", perkuliahan))
				.list();
		for (Item itemData : items) {
			// CSLItemData item = ItemAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getNama() + " - " + itemData.getPengarangs() + " - "
						+ (itemData.getPenerbit() == null ? "" : itemData.getPenerbit().getNama()) + "-"
						+ (itemData.getTahun() == null || itemData.getTahun().equals(0) ? "" : itemData.getTahun())
						+ "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		List<BukuBahanAjar> bukuBahanAjars = session.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
				.setProjection(Projections.groupProperty("bukuBahanAjar"))
				.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah())).list();
		for (BukuBahanAjar itemData : bukuBahanAjars) {
			// CSLItemData item =
			// BukuBahanAjarAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getNama() + " - " + itemData.getPengarang1() + " - "
						+ (itemData.getPenerbit()) + "-"
						+ (itemData.getTahun() == null || itemData.getTahun().equals(0) ? "" : itemData.getTahun())
						+ "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		List<Artikel> artikels = session.createCriteria(DataPunyaArtikel.class)
				.setProjection(Projections.groupProperty("artikel")).add(Restrictions.eq("perkuliahan", perkuliahan))
				.list();
		for (Artikel itemData : artikels) {
			// CSLItemData item =
			// ArtikelAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getJudul() + "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		introductoryText += "</ol><br></br><br></br><br></br><br></br><br></br><br></br>";

		return introductoryText;
	}

	public static DspaceInformation getDspace(String cookie, Perkuliahan perkuliahan) throws Exception {

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", perkuliahan.infoSimple());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", PerkuliahanAction.generateiIntroductoryText(perkuliahan));
		jsonPost.put("shortDescription", "Artefak perkuliahan " + perkuliahan.info());
		jsonPost.put("sidebarText", "Artefak perkuliahan " + perkuliahan.info());
		return DspaceInformation.dspaceProcess(cookie, perkuliahan, jsonPost.toString(), true, "collections",
				"communities/" + MatakuliahAction.getDspace(cookie, perkuliahan.getMatakuliah()) + "/collections");
	}

	protected Tabpanel ekstrakurikulerTab;

	public void onEkstrakurikuler(Event event) {

		if (ekstrakurikulerTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(ekstrakurikulerTab);
			include.setSrc("/pages/master/perkuliahan_ekstrakurikuler.zul");
		}
	}

	public void onJadwalSp(Event event) {

		if (jadwalSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(jadwalSp);
			include.setSrc("/pages/master/perkuliahan_sp.zul");
		}
	}

	protected Tabpanel praPerkuliahanTab;

	public void onPraPerkuliahan(Event event) {

		if (praPerkuliahanTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(praPerkuliahanTab);
			include.setSrc("/pages/master/perkuliahan_pra_perkuliahan.zul");
		}
	}

	protected Tabpanel perkuliahanUmumTab;

	public void onPerkuliahanUmum(Event event) {

		if (perkuliahanUmumTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(perkuliahanUmumTab);
			include.setSrc("/pages/master/perkuliahan_perkuliahan_umum.zul");
		}
	}

	public void onTampilDaftarHadirDosen(Event event) {

		if (laporanDaftarHadirDosen.getChildren().size() == 0) {
			LaporanDaftarHadirDosenHarian laporanJadwalDosenMengajar = new LaporanDaftarHadirDosenHarian();
			laporanJadwalDosenMengajar.setHeight("100%");
			laporanJadwalDosenMengajar.setWidth("100%");
			laporanJadwalDosenMengajar.setParent(this.laporanDaftarHadirDosen);
		}
	}

	public void onTampilJadwalDosenMengajar(Event event) {

		if (laporanJadwalDosenMengajar.getChildren().size() == 0) {
			LaporanJadwalDosenMengajar laporanJadwalDosenMengajar = new LaporanJadwalDosenMengajar();
			laporanJadwalDosenMengajar.setHeight("100%");
			laporanJadwalDosenMengajar.setWidth("100%");
			laporanJadwalDosenMengajar.setParent(this.laporanJadwalDosenMengajar);
		}
	}

	public void onTampiliLaporanDosenPembinaMahasiswa(Event event) {

		if (laporanDosenPembinaMahasiswa.getChildren().size() == 0) {
			LaporanDosenPembinaMahasiswa dosenPembinaMahasiswa = new LaporanDosenPembinaMahasiswa();
			dosenPembinaMahasiswa.setHeight("100%");
			dosenPembinaMahasiswa.setWidth("100%");
			dosenPembinaMahasiswa.setParent(laporanDosenPembinaMahasiswa);
		}
	}

	public void onTampiliLaporanDosenPembinaMatakuliah(Event event) {

		if (laporanDosenPembinaMatakuliah.getChildren().size() == 0) {
			LaporanDosenPembinaMatakuliah dosenPembinaMatakuliah = new LaporanDosenPembinaMatakuliah();
			dosenPembinaMatakuliah.setHeight("100%");
			dosenPembinaMatakuliah.setWidth("100%");
			dosenPembinaMatakuliah.setParent(laporanDosenPembinaMatakuliah);
		}
	}

	public void onTampilLaporanJadwalPerkuliahanPerRuangan(Event event) {

		if (laporanJadwalPerkuliahanPerRuangan.getChildren().size() == 0) {
			LaporanJadwalPerkuliahanPerRuangan laporanJadwalPerkuliahanPerRuangan = new LaporanJadwalPerkuliahanPerRuangan();
			laporanJadwalPerkuliahanPerRuangan.setHeight("100%");
			laporanJadwalPerkuliahanPerRuangan.setWidth("100%");
			laporanJadwalPerkuliahanPerRuangan.setParent(this.laporanJadwalPerkuliahanPerRuangan);
		}
	}

	public void onTampilLaporanJadwalPerkuliahanPerMatakuliah(Event event) {

		if (laporanJadwalPerkuliahanPerMatakuliah.getChildren().size() == 0) {
			LaporanJadwalPerkuliahanPerMatakuliah laporan = new LaporanJadwalPerkuliahanPerMatakuliah();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanJadwalPerkuliahanPerMatakuliah);
		}
	}

	public void onTampilLaporanJadwalPerkuliahanPerSemester(Event event) {

		if (laporanJadwalPerkuliahanPerSemester.getChildren().size() == 0) {
			LaporanJadwalPerkuliahanPerSemester laporanJadwalPerkuliahanPerSemester = new LaporanJadwalPerkuliahanPerSemester();
			laporanJadwalPerkuliahanPerSemester.setHeight("100%");
			laporanJadwalPerkuliahanPerSemester.setWidth("100%");
			laporanJadwalPerkuliahanPerSemester.setParent(this.laporanJadwalPerkuliahanPerSemester);
		}
	}

	public void onSebaranJadwal(Event event) {

		if (laporanSebaranJadwal.getChildren().size() == 0) {
			LaporanSebaranJadwalPerkuliahan laporan = new LaporanSebaranJadwalPerkuliahan();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanSebaranJadwal);
		}
	}

	public void onJmlSKSDosen(Event event) {

		if (laporanJmlSksDosen.getChildren().size() == 0) {
			LaporanSKSDosen laporanSKSDosen = new LaporanSKSDosen();
			laporanSKSDosen.setHeight("100%");
			laporanSKSDosen.setWidth("100%");
			laporanSKSDosen.setParent(laporanJmlSksDosen);
		}
	}

	public void onJmlSKSDosenPerMatakuliah(Event event) {

		if (laporanJmlSksDosenPerMatakuliah.getChildren().size() == 0) {
			LaporanSKSDosenPerMatakuliah laporanSKSDosen = new LaporanSKSDosenPerMatakuliah();
			laporanSKSDosen.setHeight("100%");
			laporanSKSDosen.setWidth("100%");
			laporanSKSDosen.setParent(laporanJmlSksDosenPerMatakuliah);
		}
	}

	public void onTampilLaporanCoverAbsensi(Event event) {

		if (laporanCoverAbsensi.getChildren().size() == 0) {
			LaporanCoverAbsensi dosenPembinaMatakuliah = new LaporanCoverAbsensi();
			dosenPembinaMatakuliah.setHeight("100%");
			dosenPembinaMatakuliah.setWidth("100%");
			dosenPembinaMatakuliah.setParent(laporanCoverAbsensi);
		}
	}

	public void onTampilLaporanAbsensi(Event event) {

		if (laporanAbsensi.getChildren().size() == 0) {
			LaporanAbsensi dosenPembinaMatakuliah = new LaporanAbsensi();
			dosenPembinaMatakuliah.setHeight("100%");
			dosenPembinaMatakuliah.setWidth("100%");
			dosenPembinaMatakuliah.setParent(laporanAbsensi);
		}
	}

	public void onTampiliLaporanJadwalPerkuliahan(Event event) {

		if (laporanJadwalPerkuliahan.getChildren().size() == 0) {
			LaporanJadwalPerkuliahan laporanJadwalPerkuliahan = new LaporanJadwalPerkuliahan();
			laporanJadwalPerkuliahan.setHeight("100%");
			laporanJadwalPerkuliahan.setWidth("100%");
			laporanJadwalPerkuliahan.setParent(this.laporanJadwalPerkuliahan);
		}
	}

	public void onJadwalPerkuliahanPerDosen(Event event) {

		if (jadwalPerkuliahanPerDosen.getChildren().size() == 0) {
			LaporanJadwalPerkuliahanPerDosen laporanJadwalPerkuliahanPerDosen = new LaporanJadwalPerkuliahanPerDosen();
			laporanJadwalPerkuliahanPerDosen.setHeight("100%");
			laporanJadwalPerkuliahanPerDosen.setWidth("100%");
			laporanJadwalPerkuliahanPerDosen.setParent(this.jadwalPerkuliahanPerDosen);
		}
	}

	public void onMasaPerkuliahan(Event event) {

		if (masaPerkuliahanTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(masaPerkuliahanTab);
			include.setSrc("/pages/master/masa_perkuliahan.zul");
		}
	}

	public void onJadwalPerkuliahanPerAsisten(Event event) {

		if (jadwalPerkuliahanPerAsisten.getChildren().size() == 0) {
			LaporanJadwalPerkuliahanPerAsisten laporanJadwalPerkuliahanPerDosen = new LaporanJadwalPerkuliahanPerAsisten();
			laporanJadwalPerkuliahanPerDosen.setHeight("100%");
			laporanJadwalPerkuliahanPerDosen.setWidth("100%");
			laporanJadwalPerkuliahanPerDosen.setParent(this.jadwalPerkuliahanPerAsisten);
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PerkuliahanAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PerkuliahanAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DetailperkuliahanHelper
	 * detailperkuliahanHelper}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PerkuliahanAction
	 */
	class PerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		private DetailperkuliahanHelper detailperkuliahanHelper = new DetailperkuliahanHelper(semesterPendek, delete,
				edit, approve, reject, create);

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.isOpen()) {

						Common.clear(detail);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(detail, "100%", new int[] { 0 });

								detailperkuliahanHelper
										.setPerkuliahan(perkuliahan.getPerkuliahan_paralel() == null ? perkuliahan
												: perkuliahan.getPerkuliahan_paralel(), perkuliahan);

								int tinggi = perkuliahan.ambilJumlahDetailperkuliahanLangsung();

								{ org.zkoss.zul.Div panelMhs = btnTab.tambahTab(0, "Manajemen Mahasiswa", "/img/svg/users.svg");
								  panelMhs.setStyle("min-height: 200px;");
								  panelMhs.setHeight((100 + (77 * tinggi)) + "px");
								  detailperkuliahanHelper.display(
										perkuliahan.getPerkuliahan_paralel() == null ? perkuliahan
												: perkuliahan.getPerkuliahan_paralel(),
										perkuliahan, panelMhs, addWindow); }

								btnTab.tambahTabLazy(1, "Asisten Dosen", "/img/svg/chalkboard-teacher-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
									@Override
									public void muat(org.zkoss.zul.Div panel) throws Exception {
										panel.setHeight("500px");
										DetailperkuliahanForPenilaianHelper.displayAsistenMahasiswa(panel, perkuliahan);
									}
								});
								btnTab.setVisibleTombol(1, tbmuser == null || tbmuser.getMahasiswa() == null);

								btnTab.tambahTabLazy(2, "Manajemen Pertemuan", "/img/svg/calendar-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
									@Override
									public void muat(org.zkoss.zul.Div panel) throws Exception {
										panel.setStyle("min-height: 200px;");
										panel.setHeight("2000px");
										int banyak = 1;
										try {
											banyak = Integer.parseInt(Common.getKonfigurasi(
													"tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
										}
										aktifitasPerkuliahanHelper.initDetail(perkuliahan, panel, 0, banyak);
									}
								});

								btnTab.tambahTabLazy(3, "Pembatasan Kuota per Tahun Angkatan", "/img/svg/filter-square.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
									@Override
									public void muat(org.zkoss.zul.Div panel) throws Exception {
										panel.setHeight("500px");
										panel.setWidth("100%");
										Iframe iframe = new Iframe(
												"/pages/master/pembagian_kuota_perkuliahan_berdasarkan_tahun_angkatan.zul?perkuliahan="
														+ perkuliahan.getId());
										iframe.setHeight("490px");
										iframe.setWidth("100%");
										iframe.setScrolling("auto");
										iframe.setParent(panel);
									}
								});

								btnTab.tambahTabLazy(4, "Angket", "/img/svg/dashboard-chart.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
									@Override
									public void muat(org.zkoss.zul.Div panel) throws Exception {
										panel.setHeight("900px");
										panel.setWidth("100%");
										AngketPerkuliahanHelper.display(panel, perkuliahan);
									}
								});
								btnTab.setVisibleTombol(4, tbmuser == null || tbmuser.getMahasiswa() == null);

							}
						});

					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (perkuliahan.getFeeder() != null && !perkuliahan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}
			PerkuliahanAction.tampilanExportKeFeederSatuan(vbox, tbmuser, PerkuliahanAction.this, perkuliahan);

			new Label(perkuliahan.getWaktu()).setParent(arg0);
			Kurikulum kurikulum = perkuliahan.getKurikulum();
			Vbox a = RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
					perkuliahan.getMatakuliah().getKode() + "-" + perkuliahan.getMatakuliah().getNama() + " "
							+ (perkuliahan.getMatakuliah().getSks() > 0 ? perkuliahan.getMatakuliah().getSks() + " sks "
									: "")
							+ " " + (perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama()) + " "
							+ (kurikulum == null ? "" : " (Kurikulum : " + kurikulum.getNama() + ")"));
			a.setStyle("font-size:9px");
			a.setParent(arg0);

			MatakuliahPrasyaratAction.tampilPrasyarat(a, perkuliahan.getMatakuliah());

			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(arg0, perkuliahan, false);

			new Label((perkuliahan.getKapasitasKelas() == null ? ""
					: Common.numberFormat.get().format(perkuliahan.getKapasitasKelas()))).setParent(arg0);

			new Label((perkuliahan.getMinggu1() ? "1," : "") + (perkuliahan.getMinggu2() ? "2," : "")
					+ (perkuliahan.getMinggu3() ? "3," : "") + (perkuliahan.getMinggu4() ? "4," : "")
					+ (perkuliahan.getMinggu5() ? "5," : "")).setParent(arg0);

			new Label(perkuliahan.getSemester() + " "
					+ (perkuliahan.getKelas() == null || perkuliahan.getKelas().equals("") ? ""
							: " " + perkuliahan.getKelas())
					+ " (" + Common.labelJenisSemester(perkuliahan) + ")")
					.setParent(arg0);

			Html informasi = new ais.ui.util.MyHtml(perkuliahan.populateInfoPersetujuan());
			informasi.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					String ta = perkuliahan.getTahunAjaran();
					String sem = perkuliahan.getGanjilGenap();
					if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendek, perkuliahan)) {
						MyMessageboxConfig.show(
								"Penjadwalan tahun akademik \"" + ta + "\" semester \"" + sem + "\" tidak diaktifkan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					HibernateUtil.currentSession().refresh(perkuliahan);
					init(perkuliahan);

				}

			});
			aksiButtons.add(button);

			// copy
			button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
			button.setTooltiptext("Copy Jadwal");
			button.setVisible(edit);

			// button.setVisible(true);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					String ta = perkuliahan.getTahunAjaran();
					String sem = perkuliahan.getGanjilGenap();
					if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendek, perkuliahan)) {
						MyMessageboxConfig.show(
								"Penjadwalan tahun akademik \"" + ta + "\" semester \"" + sem + "\" tidak diaktifkan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					copy(perkuliahan);

				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					String ta = perkuliahan.getTahunAjaran();
					String sem = perkuliahan.getGanjilGenap();
					if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendek, perkuliahan)) {
						MyMessageboxConfig.show(
								"Penjadwalan tahun akademik \"" + ta + "\" semester \"" + sem + "\" tidak diaktifkan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.hapusPerkuliahan(perkuliahan, PerkuliahanAction.this, false, null);
									}

								}
							});

				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

			if (perkuliahan.getPerkuliahan_paralel() != null && perkuliahan.getPerkuliahan_paralel().getId() != null) {
				Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
				if (!detailperkuliahans.isEmpty()) {
					Session session = HibernateUtil.currentNativeSession();
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null || !detailperkuliahan.getPerkuliahan()
									.getId().equals(perkuliahan.getPerkuliahan_paralel().getId())) {
								detailperkuliahan.setPerkuliahan(perkuliahan.getPerkuliahan_paralel());
								session.getTransaction().begin();
								Common.refreshUpdate(session, detailperkuliahan);
								session.getTransaction().commit();
							}
						}
					}
					// session.disconnect();
					ais.common.Common.closeOpenedSession(session);
				}
				detailperkuliahans = null;

			}
		}
	}

	public static void kirimKeFeeder(FeederExporter feederImporter, Perkuliahan perkuliahan,
			FeederConnector feederConnector, String token, List<String> errorLog) {
		kirimKeFeeder(feederImporter, perkuliahan, feederConnector, token, null, errorLog);
	}

	/**
	 * Mencatat satu langkah proses kirim Nilai/Perkuliahan ke Neo Feeder ke log server
	 * ({@code System.out}, berstempel waktu) — dipakai untuk mendiagnosis tahap mana yang memakan
	 * waktu lama (mis. panggilan {@code GetPesertaKelasKuliah} yang dilakukan per-mahasiswa) serta
	 * menelusuri urutan kejadian tanpa membanjiri popup progres. Tidak pernah melempar exception
	 * agar logging tidak pernah mengganggu proses pengiriman.
	 */
	private static void logLangkahFeeder(String pesan) {
		try {
			System.out.println("[NeoFeeder-Nilai][" + new java.text.SimpleDateFormat("HH:mm:ss.SSS")
					.format(new java.util.Date()) + "] " + pesan);
		} catch (Throwable t) {
			// logging tidak boleh pernah mengganggu proses utama
		}
	}

	/**
	 * Kirim SATU baris nilai/peserta {@code detailperkuliahan} ke Neo Feeder, terbagi menjadi 4
	 * langkah independen — <b>kirim data kelas</b> (bila belum ada {@code feeder}), <b>cek peserta
	 * terdaftar</b> ({@code GetPesertaKelasKuliah}), <b>daftarkan peserta</b> bila belum terdaftar
	 * ({@code InsertPesertaKelasKuliah}), <b>kirim nilai</b> ({@code UpdateNilaiPerkuliahanKelas}),
	 * dan <b>simpan penanda lokal</b> (id_kls/id_reg_pd). Setiap langkah dibungkus try/catch
	 * TERPISAH: exception pada satu langkah TIDAK menghentikan langkah lain, DICATAT ke
	 * {@code errorLog} (agar terlihat &amp; bisa diunduh pengguna — sebelumnya beberapa langkah
	 * hanya mencatat ke log admin tersembunyi via {@code tampilErrorJikaAdmin}, atau bahkan
	 * dibuang ke {@code List} lokal yang tidak pernah dibaca) DAN ke log server (waktu tempuh tiap
	 * langkah, via {@link #logLangkahFeeder}) untuk mendiagnosis penyebab proses lambat — biasanya
	 * panggilan jaringan {@code GetPesertaKelasKuliah} yang dilakukan berulang untuk SETIAP
	 * mahasiswa di SETIAP kelas (tidak ada mekanisme batch pada API Neo Feeder).
	 */
	public static void kirimKeFeeder(FeederExporter feederImporter, Detailperkuliahan detailperkuliahan,
			FeederConnector feederConnector, String token, Mahasiswa mahasiswa, List<String> errorLog, boolean lagi) {
		kirimKeFeeder(feederImporter, detailperkuliahan, feederConnector, token, mahasiswa, errorLog, lagi, null);
	}

	public static void kirimKeFeeder(FeederExporter feederImporter, Detailperkuliahan detailperkuliahan,
			FeederConnector feederConnector, String token, Mahasiswa mahasiswa, List<String> errorLog, boolean lagi,
			java.util.Set<String> pesertaKelasFeeder) {

		if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null
				&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {

			String konteks = "Mahasiswa " + (detailperkuliahan.getMahasiswa() == null ? "?"
					: (detailperkuliahan.getMahasiswa().getNim() + " " + detailperkuliahan.getMahasiswa().getNama()))
					+ " - " + (detailperkuliahan.getPerkuliahan() == null ? "?"
							: detailperkuliahan.getPerkuliahan().info());

			// LANGKAH 1: kirim data kelas kuliah (bila belum tersinkron / belum punya kode feeder).
			if (detailperkuliahan.getPerkuliahan().getFeeder() == null
					|| detailperkuliahan.getPerkuliahan().getFeeder().isEmpty()) {
				try {
					long t0 = System.currentTimeMillis();
					feederImporter.kelas_kuliah(detailperkuliahan.getPerkuliahan(), errorLog, 0);
					logLangkahFeeder("[" + konteks + "] LANGKAH 1/4 kirim data kelas SELESAI ("
							+ (System.currentTimeMillis() - t0) + " ms)");
				} catch (Exception e) {
					String pesan = "[" + konteks + "] LANGKAH 1/4 GAGAL kirim data kelas: " + e.getMessage();
					errorLog.add(pesan);
					logLangkahFeeder(pesan);
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			if (detailperkuliahan.getMahasiswa() == null || detailperkuliahan.getMahasiswa().getIdRegPd() == null) {
				errorLog.add("[" + konteks + "] Mahasiswa belum terdaftar di feeder (idRegPd kosong).");
			} else {

				if (mahasiswa == null || (mahasiswa != null && mahasiswa.getIdRegPd() != null
						&& !mahasiswa.getIdRegPd().isEmpty() && detailperkuliahan.getMahasiswa() != null
						&& mahasiswa.getId().equals(detailperkuliahan.getMahasiswa().getId()))) {

					if (detailperkuliahan.getMahasiswa().getIdRegPd() != null
							&& detailperkuliahan.getPerkuliahan().getFeeder() != null) {

						String id_kelas_kuliah = detailperkuliahan.getPerkuliahan().getFeeder();
						String id_registrasi_mahasiswa = detailperkuliahan.getMahasiswa().getIdRegPd();

						// LANGKAH 2: cek apakah mahasiswa sudah terdaftar sbg peserta kelas ini.
						// CATATAN PERFORMA: panggilan jaringan ini dilakukan untuk SETIAP mahasiswa
						// di SETIAP kelas secara berurutan (tidak ada batch API Neo Feeder) —
						// inilah penyebab utama proses lambat untuk data dalam jumlah besar.
						boolean cekBerhasil = false;
						boolean sudahTerdaftar = false;
						try {
							long t0 = System.currentTimeMillis();
							if (pesertaKelasFeeder != null) {
								sudahTerdaftar = pesertaKelasFeeder.contains(id_registrasi_mahasiswa);
							} else {
								String filter = "id_kelas_kuliah='" + id_kelas_kuliah
										+ "' AND id_registrasi_mahasiswa='" + id_registrasi_mahasiswa + "'";
								JSONArray dataPesertaKelasKuliah = feederConnector.getData("GetPesertaKelasKuliah",
										token, filter, "", "1", "");
								sudahTerdaftar = dataPesertaKelasKuliah != null && dataPesertaKelasKuliah.length() > 0;
							}
							cekBerhasil = true;
							logLangkahFeeder("[" + konteks + "] LANGKAH 2/4 cek peserta SELESAI (status="
									+ (sudahTerdaftar ? "sudah terdaftar" : "belum terdaftar") + ", "
									+ (System.currentTimeMillis() - t0) + " ms)");
						} catch (Exception e) {
							String pesan = "[" + konteks
									+ "] LANGKAH 2/4 GAGAL cek peserta kelas (GetPesertaKelasKuliah): "
									+ e.getMessage();
							errorLog.add(pesan);
							logLangkahFeeder(pesan);
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

						// LANGKAH 3: daftarkan sbg peserta bila pengecekan berhasil & belum terdaftar.
						if (cekBerhasil && !sudahTerdaftar) {
							try {
								long t0 = System.currentTimeMillis();
								JSONObject record = new JSONObject();
								record.put("id_kelas_kuliah", id_kelas_kuliah);
								record.put("id_registrasi_mahasiswa", id_registrasi_mahasiswa);
								// FIX: sebelumnya error di sini dibuang ke List lokal yang tidak pernah
								// dibaca (silent). Sekarang ditulis ke errorLog yang sama, sehingga
								// terlihat oleh pengguna & ikut berkas unduhan error.
								JSONObject hasilInsertPeserta = feederConnector.insertOrUpdateRecordBaru(token, null,
										"InsertPesertaKelasKuliah", record, errorLog, detailperkuliahan);
								if (pesertaKelasFeeder != null && (hasilInsertPeserta == null
										|| hasilInsertPeserta.isNull("error_desc")
										|| hasilInsertPeserta.optString("error_desc").trim().isEmpty()
										|| FeederConnector.isInsertSudahAda(hasilInsertPeserta,
												"InsertPesertaKelasKuliah"))) {
									pesertaKelasFeeder.add(id_registrasi_mahasiswa);
								}
								logLangkahFeeder("[" + konteks + "] LANGKAH 3/4 daftarkan peserta SELESAI ("
										+ (System.currentTimeMillis() - t0) + " ms)");
							} catch (Exception e) {
								String pesan = "[" + konteks
										+ "] LANGKAH 3/4 GAGAL daftarkan peserta (InsertPesertaKelasKuliah): "
										+ e.getMessage();
								errorLog.add(pesan);
								logLangkahFeeder(pesan);
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						// LANGKAH 4: kirim nilai (bila sudah ada nilai > 0).
						if (detailperkuliahan.getTotalNilai() > 0.1) {
							try {
								long t0 = System.currentTimeMillis();
								JSONObject idMhs = new JSONObject();
								idMhs.put("id_kelas_kuliah", id_kelas_kuliah);
								idMhs.put("id_registrasi_mahasiswa", id_registrasi_mahasiswa);

								JSONObject record = new JSONObject();
								if (detailperkuliahan.getPerkuliahan() != null && detailperkuliahan.getPerkuliahan()
										.getSembunyikanNilaiJikaBelumDiverifikasi()
										&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
									record.put("nilai_angka", Common.numberFormat.get()
											.format(detailperkuliahan.getTotalNilaiSementara()));
									record.put("nilai_huruf", detailperkuliahan.getNilaiHurufSementara());
									record.put("nilai_indeks", Common.numberFormat.get()
											.format(detailperkuliahan.getTotalIPSementara()));
								} else {
									record.put("nilai_angka",
											Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()));
									record.put("nilai_huruf", detailperkuliahan.getNilaiHuruf());
									record.put("nilai_indeks",
											Common.numberFormat.get().format(detailperkuliahan.getTotalIP()));
								}

								feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateNilaiPerkuliahanKelas",
										record, errorLog, detailperkuliahan);
								logLangkahFeeder("[" + konteks + "] LANGKAH 4/4 kirim nilai SELESAI ("
										+ (System.currentTimeMillis() - t0) + " ms)");
							} catch (Exception e) {
								String pesan = "[" + konteks
										+ "] LANGKAH 4/4 GAGAL kirim nilai (UpdateNilaiPerkuliahanKelas): "
										+ e.getMessage();
								errorLog.add(pesan);
								logLangkahFeeder(pesan);
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						// LANGKAH 5: simpan penanda id_kls/id_reg_pd ke database lokal.
						Session session = HibernateUtil.currentNativeSession();
						try {
							long t0 = System.currentTimeMillis();
							detailperkuliahan.setId_kls(id_kelas_kuliah);
							detailperkuliahan.setId_reg_pd(id_registrasi_mahasiswa);
							session.getTransaction().begin();
							Common.refreshUpdate(session, detailperkuliahan);
							session.getTransaction().commit();
							logLangkahFeeder("[" + konteks + "] LANGKAH 5/5 simpan lokal SELESAI ("
									+ (System.currentTimeMillis() - t0) + " ms)");
						} catch (Exception e) {
							try {
								if (session.getTransaction() != null && session.getTransaction().isActive()) {
									session.getTransaction().rollback();
								}
							} catch (Exception er) {
								ais.common.ErrorAuditUtil.record(er,
										"auto-audit PerkuliahanAction.kirimKeFeeder:rollback");
							}
							String pesan = "[" + konteks + "] LANGKAH 5/5 GAGAL menyimpan penanda lokal: "
									+ e.getMessage();
							errorLog.add(pesan);
							logLangkahFeeder(pesan);
							ais.common.Common.tampilErrorJikaAdmin(e);
						} finally {
							ais.common.Common.closeOpenedSession(session);
						}
					}
				}
			}
		}
	}

	private static java.util.Set<String> ambilPesertaKelasFeeder(FeederConnector feederConnector, String token,
			String idKelasKuliah, List<String> errorLog, String labelKelas) {
		java.util.Set<String> peserta = new java.util.HashSet<String>();
		try {
			long t0 = System.currentTimeMillis();
			String filter = "id_kelas_kuliah='" + idKelasKuliah + "'";
			JSONArray dataPesertaKelasKuliah = feederConnector.getData("GetPesertaKelasKuliah", token, filter, "",
					"10000", "");
			if (dataPesertaKelasKuliah != null) {
				for (int i = 0; i < dataPesertaKelasKuliah.length(); i++) {
					JSONObject data = dataPesertaKelasKuliah.getJSONObject(i);
					String idReg = data.optString("id_registrasi_mahasiswa", "").trim();
					if (!idReg.isEmpty()) {
						peserta.add(idReg);
					}
				}
			}
			logLangkahFeeder(labelKelas + " - cache peserta Feeder: " + peserta.size() + " peserta ("
					+ (System.currentTimeMillis() - t0) + " ms)");
			return peserta;
		} catch (Exception e) {
			String pesan = "[" + labelKelas + "] Gagal mengambil daftar peserta kelas dari Feeder: " + e.getMessage();
			errorLog.add(pesan);
			logLangkahFeeder(pesan);
			ais.common.Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	public static void kirimKeFeeder(FeederExporter feederImporter, Perkuliahan perkuliahan,
			FeederConnector feederConnector, String token, Mahasiswa mahasiswa, List<String> errorLog) {
		kirimKeFeeder(feederImporter, perkuliahan, feederConnector, token, mahasiswa, errorLog, null, 1, 1);
	}

	/**
	 * Varian {@code kirimKeFeeder} per-KELAS dengan <b>progres rinci</b> (popup melayang) dan
	 * <b>log langkah-per-langkah</b>: menunjukkan kelas ke berapa dari berapa ({@code idxKelas}
	 * /{@code totalKelas}) DAN mahasiswa ke berapa dari berapa di dalam kelas tsb — sehingga
	 * pengguna tahu persis data apa yang sedang diproses, bukan hanya persentase diam yang lama
	 * tak berubah (lihat {@link #kirimKeFeeder(FeederExporter, Detailperkuliahan, FeederConnector,
	 * String, Mahasiswa, List, boolean)} untuk detail 5 langkah &amp; error-handling per mahasiswa).
	 *
	 * @param progres    handle popup progres ({@link NeoFeederProgressHelper}); boleh {@code null}
	 *                   (progres tidak ditampilkan, proses tetap berjalan)
	 * @param idxKelas   indeks kelas ini dalam batch (1-based)
	 * @param totalKelas jumlah total kelas dalam batch
	 */
	public static void kirimKeFeeder(FeederExporter feederImporter, Perkuliahan perkuliahan,
			FeederConnector feederConnector, String token, Mahasiswa mahasiswa, List<String> errorLog,
			Label progres, int idxKelas, int totalKelas) {

		String labelKelas = "Perkuliahan " + idxKelas + "/" + totalKelas + ": " + perkuliahan.info();
		double pctKelasAwal = totalKelas <= 0 ? 0 : ((idxKelas - 1) * 100.0 / totalKelas);

		NeoFeederProgressHelper.updateProgres(progres, labelKelas + " - mengirim data kelas kuliah... ("
				+ Common.numberFormat.get().format(pctKelasAwal) + "%)");
		long tKelas = System.currentTimeMillis();
		try {
			feederImporter.kelas_kuliah(perkuliahan, errorLog, 0);
			logLangkahFeeder(labelKelas + " - kirim data kelas SELESAI (" + (System.currentTimeMillis() - tKelas)
					+ " ms)");
		} catch (Exception e) {
			String pesan = "[" + labelKelas + "] Gagal mengirim data kelas kuliah: " + e.getMessage();
			errorLog.add(pesan);
			logLangkahFeeder(pesan);
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (perkuliahan.getFeeder() != null) {
			java.util.Set<String> pesertaKelasFeeder = ambilPesertaKelasFeeder(feederConnector, token,
					perkuliahan.getFeeder(), errorLog, labelKelas);
			java.util.Collection<Long> ids = perkuliahan.ambilDetailperkuliahan();
			int totalMhs = ids == null ? 0 : ids.size();
			int idxMhs = 1;
			if (ids != null) {
				for (Long id : ids) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, id.toString());
					String namaMhs = (detailperkuliahan == null || detailperkuliahan.getMahasiswa() == null) ? "?"
							: (detailperkuliahan.getMahasiswa().getNim() + " "
									+ detailperkuliahan.getMahasiswa().getNama());
					double pct = totalKelas <= 0 ? 0
							: (pctKelasAwal + (totalMhs <= 0 ? 0 : (idxMhs * 100.0 / totalMhs) / totalKelas));
					NeoFeederProgressHelper.updateProgres(progres,
							labelKelas + " | Mahasiswa " + idxMhs + "/" + totalMhs + ": " + namaMhs + " ("
									+ Common.numberFormat.get().format(pct) + "%)");
					long tMhs = System.currentTimeMillis();
					try {
						kirimKeFeeder(feederImporter, detailperkuliahan, feederConnector, token, mahasiswa,
								errorLog, true, pesertaKelasFeeder);
						logLangkahFeeder(labelKelas + " | Mahasiswa " + idxMhs + "/" + totalMhs + " (" + namaMhs
								+ ") SELESAI (" + (System.currentTimeMillis() - tMhs) + " ms)");
					} catch (Exception e) {
						String pesan = "[" + labelKelas + " | Mahasiswa " + namaMhs + "] " + e.getMessage();
						errorLog.add(pesan);
						logLangkahFeeder(pesan);
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					idxMhs++;
				}
			}
		}
	}

	@SuppressWarnings({})
	public void copy(final Perkuliahan perkuliahan) throws Exception {
		Perkuliahan newperkuliahan = (Perkuliahan) perkuliahan.clone();
		newperkuliahan.setId(null);
		newperkuliahan.setFeeder(null);
		new PenjadwalanUtil(this).init(newperkuliahan, semesterPendek, ekstrakurikuler, merupakanPraPerkuliahan,
				merupakanPerkuliahanUmum, merupakanRemedial);
	}

	public void onAdd(Event event) throws Exception {

		Perkuliahan perkuliahan = new Perkuliahan();
		try {
			perkuliahan.setTahunAjaran((String) searchTahunAjaran.getSelectedItem().getValue());
			perkuliahan.setSemester((Integer) (searchsemester.getSelectedItem() == null
					|| searchsemester.getSelectedItem().getValue() == null
							? (searchJenisSemester.getSelectedItem().getValue() == null ? 1
									: searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? 1
											: 2)
							: searchsemester.getSelectedItem().getValue()));
			perkuliahan.setProgram((String) (searchprogram.getSelectedItem() == null
					|| searchprogram.getSelectedItem().getValue() == null ? null
							: searchprogram.getSelectedItem().getValue()));
			perkuliahan.setKurikulum((Kurikulum) searchkurikulum.getAttribute("kurikulum"));
			perkuliahan.setMatakuliah((Matakuliah) searchmatakuliah.getAttribute("matakuliah"));
			perkuliahan.setHari(
					(String) (searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
							? ""
							: searchhari.getSelectedItem().getValue()));
			perkuliahan.setDosen1((Dosen) searchdosen.getAttribute("dosen"));
			perkuliahan.setJurusan((Jurusan) (searchjurusan.getSelectedItem() == null
					|| searchjurusan.getSelectedItem().getValue() == null ? null
							: searchjurusan.getSelectedItem().getValue()));
			perkuliahan.setMasaPerkuliahan((MasaPerkuliahan) searchmasaperkulaiahan.getAttribute("masaPerkuliahan"));
			perkuliahan.setKelas(searchkelas.getValue());
			perkuliahan.setKelasref((Kelas) searchkelas.getAttribute("kelas"));
			perkuliahan.setRuang((Ruang) searchruang.getAttribute("ruang"));
			perkuliahan.setMerupakanRemedial(merupakanRemedial);

			// Ikuti filter "Jenis Smt" untuk default "Semester Periode" di form Tambah:
			// SP → tandai Semester Pendek; Ganjil/Genap → set ganjilGenap; "Semua" → biarkan (Ikut Smt Kurikulum).
			Object filterJenisSmt = searchJenisSemester.getSelectedItem() == null ? null
					: searchJenisSemester.getSelectedItem().getValue();
			if (Perkuliahan.SP.equals(filterJenisSmt)) {
				perkuliahan.setStatusSemesterPendek(Perkuliahan.SEMESTER_PENDEK);
			} else if (Perkuliahan.GANJIL.equals(filterJenisSmt) || Perkuliahan.GENAP.equals(filterJenisSmt)) {
				perkuliahan.setGanjilGenap((String) filterJenisSmt);
			}

			if (perkuliahan.getKurikulum() != null) {
				perkuliahan.setProgram(perkuliahan.getKurikulum().getProgram() == null ? null
						: perkuliahan.getKurikulum().getProgram().getNama());
				perkuliahan.setJurusan(perkuliahan.getKurikulum().getJurusan());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		init(perkuliahan);
	}

	/**
	 * <b>Khusus admin</b>: aktif/non-aktifkan penjadwalan untuk <b>Tahun Akademik</b> &amp; <b>Jenis Smt</b>
	 * yang dipilih pada filter. Ini menoggle konfigurasi yang dipakai validasi simpan jadwal
	 * ({@link ais.common.CommonPenjadwalan#getKonfigurasi}). Mendukung <b>Semester Pendek (SP)</b>: bila
	 * filter "Jenis Smt" = "Semester Pendek (SP)" ({@code Perkuliahan.SP}) maka yang ditoggle adalah
	 * konfigurasi {@code PENJADWALAN_SP}; untuk Ganjil/Genap konfigurasi {@code PENJADWALAN}. Untuk
	 * "Semua" (tak menunjuk satu semester) diminta memilih Jenis Smt dahulu.
	 */
	public void onToggleAktifPenjadwalan(Event event) throws Exception {
		if (!Common.getApakahAdmin()) {
			return;
		}
		String ta = searchTahunAjaran.getSelectedItem() == null ? null
				: (String) searchTahunAjaran.getSelectedItem().getValue();
		if (ta == null || ta.trim().isEmpty()) {
			MyMessageboxConfig.show("Pilih Tahun Akademik pada filter terlebih dahulu.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Object js = searchJenisSemester.getSelectedItem() == null ? null
				: searchJenisSemester.getSelectedItem().getValue();
		String sem;
		Integer semPendek;
		if (Perkuliahan.SP.equals(js)) {
			sem = Perkuliahan.SP;
			semPendek = Perkuliahan.SEMESTER_PENDEK;
		} else if (Perkuliahan.GANJIL.equals(js) || Perkuliahan.GENAP.equals(js)) {
			sem = (String) js;
			semPendek = null;
		} else {
			MyMessageboxConfig.show(
					"Pilih Jenis Smt (Ganjil / Genap / Semester Pendek) pada filter terlebih dahulu — "
							+ "\"Semua\" tidak menunjuk satu semester tertentu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Konfigurasi konfigurasi = CommonPenjadwalan.getKonfigurasi(ta, sem, semPendek);
		boolean sekarangAktif = Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		konfigurasi.setNilai(sekarangAktif ? Konfigurasi.TIDAK_AKTIF : Konfigurasi.AKTIF);
		Common.refreshSaveOrUpdate(konfigurasi);
		if (sekarangAktif) {
			MyMessageboxConfig.showFormat(
					"Dengan hormat, penjadwalan perkuliahan untuk Tahun Akademik \"{V1}\" semester \"{V2}\" telah "
							+ "berhasil DINONAKTIFKAN. Mulai saat ini, penambahan maupun perubahan jadwal perkuliahan "
							+ "pada periode tersebut untuk sementara tidak dapat dilakukan. Apabila periode ini "
							+ "seharusnya masih terbuka, mohon aktifkan kembali melalui tombol \"Aktif/Non-aktifkan "
							+ "Penjadwalan\" pada layar ini. Terima kasih atas perhatian Bapak/Ibu.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
		} else {
			MyMessageboxConfig.showFormat(
					"Dengan hormat, penjadwalan perkuliahan untuk Tahun Akademik \"{V1}\" semester \"{V2}\" telah "
							+ "berhasil DIAKTIFKAN. Penambahan dan perubahan jadwal perkuliahan pada periode tersebut "
							+ "kininya dapat dilakukan. Terima kasih atas perhatian Bapak/Ibu.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
		}
	}

	public void onInitJadwalKurikulum(Event event) throws Exception {
		Perkuliahan perkuliahan = new Perkuliahan();
		try {
			perkuliahan.setTahunAjaran((String) searchTahunAjaran.getSelectedItem().getValue());
			perkuliahan.setSemester((Integer) (searchsemester.getSelectedItem() == null
					|| searchsemester.getSelectedItem().getValue() == null
							? (searchJenisSemester.getSelectedItem().getValue() == null ? 1
									: searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? 1
											: 2)
							: searchsemester.getSelectedItem().getValue()));
			perkuliahan.setProgram((String) (searchprogram.getSelectedItem() == null
					|| searchprogram.getSelectedItem().getValue() == null ? null
							: searchprogram.getSelectedItem().getValue()));
			perkuliahan.setKurikulum((Kurikulum) searchkurikulum.getAttribute("kurikulum"));
			perkuliahan.setMatakuliah((Matakuliah) searchmatakuliah.getAttribute("matakuliah"));
			perkuliahan.setHari(
					(String) (searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
							? ""
							: searchhari.getSelectedItem().getValue()));
			perkuliahan.setDosen1((Dosen) searchdosen.getAttribute("dosen"));
			perkuliahan.setJurusan((Jurusan) (searchjurusan.getSelectedItem() == null
					|| searchjurusan.getSelectedItem().getValue() == null ? null
							: searchjurusan.getSelectedItem().getValue()));
			perkuliahan.setMasaPerkuliahan((MasaPerkuliahan) searchmasaperkulaiahan.getAttribute("masaPerkuliahan"));
			perkuliahan.setKelas(searchkelas.getValue());
			perkuliahan.setKelasref((Kelas) searchkelas.getAttribute("kelas"));
			perkuliahan.setRuang((Ruang) searchruang.getAttribute("ruang"));
			perkuliahan.setMerupakanRemedial(merupakanRemedial);
			perkuliahan.setMerupakanPraPerkuliahan(merupakanPraPerkuliahan);
			perkuliahan.setMerupakanPerkuliahanUmum(merupakanPerkuliahanUmum);

			// Ikuti filter "Jenis Smt" untuk default "Semester Periode" pada Tambah Berdasar Kurikulum:
			// SP → tandai Semester Pendek; Ganjil/Genap → set ganjilGenap; "Semua" → biarkan (Ikut Smt Kurikulum).
			Object filterJenisSmtKur = searchJenisSemester.getSelectedItem() == null ? null
					: searchJenisSemester.getSelectedItem().getValue();
			if (Perkuliahan.SP.equals(filterJenisSmtKur)) {
				perkuliahan.setStatusSemesterPendek(Perkuliahan.SEMESTER_PENDEK);
			} else if (Perkuliahan.GANJIL.equals(filterJenisSmtKur) || Perkuliahan.GENAP.equals(filterJenisSmtKur)) {
				perkuliahan.setGanjilGenap((String) filterJenisSmtKur);
			}

			if (perkuliahan.getKurikulum() != null) {
				perkuliahan.setProgram(perkuliahan.getKurikulum().getProgram() == null ? null
						: perkuliahan.getKurikulum().getProgram().getNama());
				perkuliahan.setJurusan(perkuliahan.getKurikulum().getJurusan());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Object periodeTerpilih = searchJenisSemester.getSelectedItem() == null ? null
				: searchJenisSemester.getSelectedItem().getValue();
		boolean semesterPendekTerpilih = Perkuliahan.SP.equals(periodeTerpilih);
		String jenisSemester = semesterPendekTerpilih ? Perkuliahan.SP
				: (Perkuliahan.GANJIL.equals(periodeTerpilih) || Perkuliahan.GENAP.equals(periodeTerpilih)
						? (String) periodeTerpilih
						: (perkuliahan.getSemester() != null && perkuliahan.getSemester() % 2 == 0
								? Perkuliahan.GENAP
								: Perkuliahan.GANJIL));
		Integer statusSemesterPendek = semesterPendekTerpilih ? Perkuliahan.SEMESTER_PENDEK : semesterPendek;
		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(perkuliahan.getTahunAjaran(), jenisSemester,
				statusSemesterPendek, perkuliahan)) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, Tambah Berdasar Kurikulum tidak dapat dilakukan karena penjadwalan Tahun Akademik "
							+ "\"{V1}\" semester \"{V2}\" tidak aktif untuk fakultas/prodi yang dipilih.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					perkuliahan.getTahunAjaran(), jenisSemester);
			return;
		}

		new PenjadwalanUtil(this).initJadwalKurikulum(perkuliahan, semesterPendek, ekstrakurikuler, merupakanRemedial);
	}

	@SuppressWarnings({})
	protected void init(final Perkuliahan perkuliahan) throws Exception {
		new PenjadwalanUtil(this).init(perkuliahan, semesterPendek, ekstrakurikuler, merupakanPraPerkuliahan,
				merupakanPerkuliahanUmum, merupakanRemedial);
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(session, order);
	}

	public Criteria initCriteria(Session session, boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select perkuliahan from detailperkuliahan a inner join mahasiswa b on (a.mahasiswa = b.id) where perkuliahan is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by perkuliahan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		if ((searchasisten != null && searchasisten.getAttribute("mahasiswa") != null)
				|| !searchnamaasisten.getValue().trim().isEmpty()) {
			Mahasiswa mahasiswa = (Mahasiswa) searchasisten.getAttribute("mahasiswa");
			String sql = "this_.id in (select perkuliahan from mahasiswa_jadi_asisten a inner join mahasiswa b on (a.mahasiswa=b.id) where perkuliahan is not null and a.aktif=true "
					+ (mahasiswa == null ? "" : "and a.mahasiswa=" + mahasiswa.getId())
					+ (searchnamaasisten.getValue().trim().isEmpty() ? ""
							: "and (b.nama ilike '%" + searchnamaasisten.getValue().trim() + "%' or b.nim ilike '%"
									+ searchnamaasisten.getValue().trim() + "%')")
					+ " group by perkuliahan)";
			criterionMhs = Restrictions.and(criterionMhs, Restrictions.sqlRestriction(sql));
		}

		Criteria criteria = session.createCriteria(Perkuliahan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(searchKeterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchKeterangan.getValue().trim(), MatchMode.ANYWHERE));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		if (ConstantValues.aktifkanTahapanKurikulum) {
			criteria.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah", Criteria.LEFT_JOIN)
					.add((searchTahap != null && searchTahap.getSelectedItem() != null
							&& searchTahap.getSelectedItem().getValue() != null
							&& searchTahap.getSelectedItem().getValue().equals(-1))
									? Restrictions.sqlRestriction("true")
									: (searchTahap != null && searchTahap.getSelectedItem() != null
											&& searchTahap.getSelectedItem().getValue() != null
													? Restrictions.eq("kurikulumPunyaMatakuliah.tahap",
															searchTahap.getSelectedItem().getValue())
													: Restrictions.or(Restrictions.isNull("kurikulumPunyaMatakuliah"),
															Restrictions.isNull("kurikulumPunyaMatakuliah.tahap"))));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
						Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", searchdosen.getAttribute("myValue")));

		Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
		if (!searchnamadsn.getValue().trim().isEmpty()) {
			criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
					.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
					.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
					.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
					.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
					.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
					.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
					.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
					.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
					.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN);

			criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
		}

		criteria

				.add(searchnamamk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliah.kode", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliah.nama", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE)))

				// Pencarian cepat "Kode/Nama" (server-side, paging-aware) -> filter kode/nama matakuliah.
				.add(cariCepatPerkuliahan == null || cariCepatPerkuliahan.getValue() == null
						|| cariCepatPerkuliahan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.ilike("matakuliah.kode", cariCepatPerkuliahan.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("matakuliah.nama", cariCepatPerkuliahan.getValue().trim(),
												MatchMode.ANYWHERE)))

				.add(criterionNamaDosn).add(criterionMhs)

				.add((searchkurikulum == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkurikulum.getAttribute("kurikulum") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kurikulum", searchkurikulum.getAttribute("kurikulum"))))

				.add((searchkelas == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kelas", searchkelas.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchparalel.isChecked() ? Restrictions.or(Restrictions.sqlRestriction(
						"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
						Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))

				.add(searchpraktek != null && searchpraktek.isChecked()
						? Restrictions.eq("terdapatKegiatanPraktek", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(merupakanRemedial ? Restrictions.eq("merupakanRemedial", true)
						: Restrictions.or(Restrictions.isNull("merupakanRemedial"),
								Restrictions.eq("merupakanRemedial", false)))

				.add(searchtanpakelas.isChecked()
						? Restrictions.or(Restrictions.eq("kelas", ""), Restrictions.isNull("kelas"))
						: Restrictions.sqlRestriction("1=1"))

				.add(searchbelummasukFeeder.isChecked()
						? Restrictions.or(Restrictions.eq("feeder", ""), Restrictions.isNull("feeder"))
						: Restrictions.sqlRestriction("1=1"))

				.add(searchmasukFeeder.isChecked()
						? Restrictions.and(Restrictions.ne("feeder", ""), Restrictions.isNotNull("feeder"))
						: Restrictions.sqlRestriction("1=1"))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(criterion)

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add((searchmasaperkulaiahan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmasaperkulaiahan.getAttribute("masaPerkuliahan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaPerkuliahan", searchmasaperkulaiahan.getAttribute("masaPerkuliahan"))))

				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

				.add(searchjmldosen == null || searchjmldosen.getSelectedItem() == null
						|| searchjmldosen.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jumlahDosen", searchjmldosen.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				// Ganjil/Genap: TIDAK dibatasi bila "Semua" (null) ATAU "Semester Pendek (SP)" dipilih
				// (SP lintas ganjil/genap — filternya cukup lewat statusSemesterPendek di bawah).
				.add(merupakanPraPerkuliahan || merupakanPerkuliahanUmum
						|| searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
						|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", searchJenisSemester.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan || merupakanPerkuliahanUmum || searchsemester.getSelectedItem() == null
						|| searchsemester.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan ? Restrictions.eq("merupakanPraPerkuliahan", true)
						: Restrictions.or(Restrictions.eq("merupakanPraPerkuliahan", false),
								Restrictions.isNull("merupakanPraPerkuliahan")))

				.add(merupakanPerkuliahanUmum ? Restrictions.eq("merupakanPerkuliahanUmum", true)
						: Restrictions.or(Restrictions.eq("merupakanPerkuliahanUmum", false),
								Restrictions.isNull("merupakanPerkuliahanUmum")))

				// statusSemesterPendek: bila filter "Jenis Smt = Semester Pendek (SP)" dipilih, tampilkan
				// HANYA perkuliahan SP (statusSemesterPendek = SEMESTER_PENDEK) — persis tab "SP" lama.
				// Selain itu pakai perilaku lama (field semesterPendek: null → non-SP, else → SP).
				.add(merupakanRemedial ? Restrictions.sqlRestriction("true")
						: (searchJenisSemester.getSelectedItem() != null
								&& Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue()))
										? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
										: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(merupakanPraPerkuliahan || merupakanPerkuliahanUmum || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
		}

		// Filter "Tampilan Kurikulum": OBE / Bukan OBE / Semua. Kurikulum.obe (Boolean,
		// null dianggap bukan OBE). LEFT JOIN agar perkuliahan tanpa kurikulum tetap
		// terhitung sebagai "bukan OBE".
		String filterObe = (searchObe == null || searchObe.getSelectedItem() == null) ? null
				: (String) searchObe.getSelectedItem().getValue();
		if (filterObe != null && !filterObe.trim().isEmpty()) {
			criteria.createAlias("kurikulum", "kurikulumObe", Criteria.LEFT_JOIN);
			if ("OBE".equals(filterObe)) {
				criteria.add(Restrictions.eq("kurikulumObe.obe", true));
			} else if ("NONOBE".equals(filterObe)) {
				criteria.add(Restrictions.or(Restrictions.isNull("kurikulumObe.obe"),
						Restrictions.eq("kurikulumObe.obe", false)));
			}
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(final Event event) {
		try {
			int activePage = paging == null ? 0 : paging.getActivePage();
			Common.initPaging(initCriteria(false), paging);
			List<Perkuliahan> perkuliahan = ConstantValues.simpleList(
					initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * activePage),
					Perkuliahan.class);
			if (grid != null) {
				ListModel strset = new SimpleListModel(perkuliahan);
				grid.setRowRenderer(new PerkuliahanRenderer());
				grid.setModelCheckMobile(strset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onCetakLaporanDosenPembinaMatakuliah(Event event) throws Exception {
		LaporanDosenPembinaMatakuliah dosenPembinaMatakuliah = new LaporanDosenPembinaMatakuliah();
		dosenPembinaMatakuliah.setParent(page.getFirstRoot());
		dosenPembinaMatakuliah.setWidth("90%");
		dosenPembinaMatakuliah.setHeight("90%");
		dosenPembinaMatakuliah.setTitle("Laporan Dosen Pembina Matakuliah");
		dosenPembinaMatakuliah.setClosable(true);
		dosenPembinaMatakuliah.onModal();
	}

	public void onCopyPerkuliahan(Event event) throws Exception {
		final MyWindow window = new MyWindow("Copy Jadwal Perkuliaahan", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("95%");
		window.setWidth("90%");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);
		if (searchTahunAjaran.getSelectedItem() != null) {
			Common.selectComboItem(tahunAkademikDari, searchTahunAjaran.getSelectedItem().getValue());
		}
		final Combobox tahunAkademikKe = Common.generateTahunAjaran(null);

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);

		final Combobox fakultas = new Combobox();
		final Combobox jurusan = new Combobox();
		final Html peringatan = new ais.ui.util.MyHtml();

		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		final Combobox program = Common.initPrograms(null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.appendChild(peringatan);
		east.setWidth("40%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Dari"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Ke"));
		row.appendChild(tahunAkademikKe);
		tahunAkademikKe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy Jadwal Perkuliahan", "/img/svg/edit-copy.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());
				final String tahunAkademik2 = (String) (tahunAkademikKe.getSelectedItem() == null ? null
						: tahunAkademikKe.getSelectedItem().getValue());

				final String semester = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());
				final Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
						|| fakultas.getSelectedItem().getValue() == null ? null
								: fakultas.getSelectedItem().getValue());

				final String myProgram = (String) (program.getSelectedItem() == null
						|| program.getSelectedItem().getValue() == null ? null : program.getSelectedItem().getValue());

				if (myProgram == null) {
					MyMessageboxConfig.show("Program Dari harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (tahunAkademik1 == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik Dari",
							"Kolom Tahun Akademik Dari belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Tahun Akademik Dari.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return;
				}

				if (tahunAkademik2 == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik Ke",
							"Kolom Tahun Akademik Ke belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Tahun Akademik Ke.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return;
				}

				if (tahunAkademik1.equals(tahunAkademik2)) {
					MyMessageboxConfig.show("Tahun Akademik gak boleh sama", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester == null) {
					MyMessageboxConfig.show("Jenis Semester harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session
						.createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(merupakanRemedial ? Restrictions.sqlRestriction("true")
								: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
										: Restrictions.eq("statusSemesterPendek", semesterPendek))
						.createAlias("jurusan", "jurusan")
						.add(myFakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", myFakultas))
						.add(myJurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan", myJurusan))
						.add(Restrictions.eq("program", myProgram)).add(Restrictions.eq("tahunAjaran", tahunAkademik1))
						.add(Restrictions.eq("ganjilGenap", semester)),
						Perkuliahan.class);

				MyMessageboxConfig.show(
						"Apakah yakin ingin melanjutkan men-copy " + perkuliahans.size()
								+ " jadwal perkuliahan dari tahun akademik " + tahunAkademik1 + " ke tahun akademik "
								+ tahunAkademik2 + " di semester " + semester + " program " + myProgram + " ?",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();
									for (Perkuliahan perkuliahan : perkuliahans) {

										Integer ada = ((Number) session.createCriteria(Perkuliahan.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(merupakanRemedial ? Restrictions.sqlRestriction("true")
														: semesterPendek == null
																? Restrictions.isNull("statusSemesterPendek")
																: Restrictions.eq("statusSemesterPendek",
																		semesterPendek))
												.createAlias("jurusan", "jurusan")
												.add(myFakultas == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jurusan.fakultas", myFakultas))
												.add(myJurusan == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jurusan", myJurusan))
												.add(Restrictions.eq("tahunAjaran", tahunAkademik2))
												.add(Restrictions.eq("ganjilGenap", semester))
												.add(perkuliahan.getDosen1() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("dosen1", perkuliahan.getDosen1()))

												.add(perkuliahan.getDosen2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("dosen2", perkuliahan.getDosen2()))

												.add(perkuliahan.getProgram() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("program", perkuliahan.getProgram()))

												.add(perkuliahan.getMatakuliah() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))

												.add(perkuliahan.getKelas() == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.sqlRestriction(
																"upper(trim(this_.kelas)) = upper(trim('"
																		+ perkuliahan.getKelas() + "'))"))

												.setProjection(Projections.rowCount()).uniqueResult()).intValue();

										if (ada.equals(0)) {

											Perkuliahan copyPerkuliahan = (Perkuliahan) perkuliahan.clone();

											if (TemplatePerkuliahanDetailHelper
													.checkKeberadaanPerkuliahan(copyPerkuliahan, peringatan) == null) {
												copyPerkuliahan.setId(null);
												copyPerkuliahan.setFeeder(null);
												copyPerkuliahan.setTahunAjaran(tahunAkademik2);

												session.save(copyPerkuliahan);
											}
										}

									}

									MyMessageboxConfig.show("Copy jadwal perkuliahan berhasil dilakukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);
												}
											});

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}

	public void onDeletePerkuliahan(Event event) throws Exception {
		final MyWindow window = new MyWindow("Delete Jadwal Perkuliaahan", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("300px");
		window.setWidth("850px");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);
		if (searchTahunAjaran.getSelectedItem() != null) {
			Common.selectComboItem(tahunAkademikDari, searchTahunAjaran.getSelectedItem().getValue());
		}

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);

		final Combobox fakultas = new Combobox();
		final Combobox jurusan = new Combobox();

		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		final Combobox program = Common.initPrograms(null);

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
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Hapus Jadwal Perkuliahan", "/img/svg/trash.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());

				final String semester = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());
				final Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
						|| fakultas.getSelectedItem().getValue() == null ? null
								: fakultas.getSelectedItem().getValue());

				final String myProgram = (String) (program.getSelectedItem() == null
						|| program.getSelectedItem().getValue() == null ? null : program.getSelectedItem().getValue());

				if (myProgram == null) {
					MyMessageboxConfig.show("Program Dari harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (tahunAkademik1 == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik Dari",
							"Kolom Tahun Akademik Dari belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Tahun Akademik Dari.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return;
				}

				if (semester == null) {
					MyMessageboxConfig.show("Jenis Semester harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session
						.createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(merupakanRemedial ? Restrictions.sqlRestriction("true")
								: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
										: Restrictions.eq("statusSemesterPendek", semesterPendek))
						.createAlias("jurusan", "jurusan")
						.add(myFakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", myFakultas))
						.add(myJurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan", myJurusan))
						.add(Restrictions.eq("tahunAjaran", tahunAkademik1)).add(Restrictions.eq("program", myProgram))
						.add(Restrictions.eq("ganjilGenap", semester)),
						Perkuliahan.class);

				// Htm peringatan = "";

				MyMessageboxConfig.show(
						"Apakah yakin ingin menghapus jadwal perkuliahan ini ?\nCatatan : Perkuliahan yang sudah dibuatkan agenda pertemuan dan terdapat data mahasiswa-nya tidak dapat dihapus",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();
									for (Perkuliahan perkuliahan : perkuliahans) {

										Integer count = perkuliahan.ambilJumlahDetailperkuliahanLangsung();
										Integer count1 = perkuliahan.ambilJumlahPertemuan();

										if (count.equals(0) && count1.equals(0)) {
											session.delete(perkuliahan);
										}

									}

									MyMessageboxConfig.show("Hapus jadwal perkuliahan berhasil dilakukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);

												}
											});

									window.detach();

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings("unchecked")
	public void onBersihkanPerkuliahan(Event event) throws Exception {
		final MyWindow window = new MyWindow("Bersihkan Perkuliahan Kosong", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setWidth("560px");

		// Default rentang: kemarin s/d hari ini (1 hari)
		Calendar calMulai = Calendar.getInstance();
		calMulai.add(Calendar.DAY_OF_MONTH, -1);
		calMulai.set(Calendar.HOUR_OF_DAY, 0);
		calMulai.set(Calendar.MINUTE, 0);
		calMulai.set(Calendar.SECOND, 0);
		calMulai.set(Calendar.MILLISECOND, 0);

		Calendar calSelesai = Calendar.getInstance();
		calSelesai.set(Calendar.HOUR_OF_DAY, 23);
		calSelesai.set(Calendar.MINUTE, 59);
		calSelesai.set(Calendar.SECOND, 59);
		calSelesai.set(Calendar.MILLISECOND, 999);

		final Datebox dateboxMulai = new Datebox(calMulai.getTime());
		dateboxMulai.setFormat("dd-MM-yyyy");
		dateboxMulai.setConstraint("no future");

		final Datebox dateboxSelesai = new Datebox(calSelesai.getTime());
		dateboxSelesai.setFormat("dd-MM-yyyy");
		dateboxSelesai.setConstraint("no future");

		// Pakai Vbox (tinggi mengikuti isi) daripada Borderlayout yang butuh tinggi
		// eksplisit. Tanpa tinggi, Borderlayout kolaps 0px sehingga isi popup tidak
		// tampil (hanya judul yang terlihat).
		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setStyle("padding:8px;");
		vbox.setParent(window);

		MyGrid formGrid = new MyGrid();
		formGrid.setWidth("100%");
		formGrid.setParent(vbox);

		Rows rows = new Rows();
		rows.setParent(formGrid);

		// Baris info
		MyFormRow rowInfo = new MyFormRow();
		rowInfo.setSpans("2");
		rowInfo.setParent(rows);
		org.zkoss.zul.Html htmlInfo = new org.zkoss.zul.Html(
				"<div style='padding:10px 12px;background:#fff8e1;border:1px solid #ffe082;border-radius:6px;"
				+ "color:#6d4c00;font-size:12px;line-height:1.6;'>"
				+ "<b>Perhatian:</b> Fitur ini akan <b>menghapus secara permanen</b> data perkuliahan yang memenuhi "
				+ "<b>semua</b> kriteria berikut:<br/>"
				+ "&nbsp;&nbsp;1. Tidak ada mahasiswa yang mengambil KRS (tidak ada <i>detail perkuliahan</i>)<br/>"
				+ "&nbsp;&nbsp;2. Tidak ada agenda pertemuan yang dibuat<br/>"
				+ "&nbsp;&nbsp;3. Tanggal perubahan data berada dalam rentang tanggal yang dipilih"
				+ "</div>");
		rowInfo.appendChild(htmlInfo);

		MyFormRow rowMulai = new MyFormRow();
		rowMulai.setParent(rows);
		rowMulai.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		dateboxMulai.setWidth("90%");
		rowMulai.appendChild(dateboxMulai);

		MyFormRow rowSelesai = new MyFormRow();
		rowSelesai.setParent(rows);
		rowSelesai.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		dateboxSelesai.setWidth("90%");
		rowSelesai.appendChild(dateboxSelesai);

		// Baris info rentang = 1 hari
		MyFormRow rowRentang = new MyFormRow();
		rowRentang.setSpans("2");
		rowRentang.setParent(rows);
		final Label labelRentang = new Label(ais.common.Common.getBahasaConfig("Rentang: 1 hari"));
		labelRentang.setStyle("font-size:11px;color:#555;padding:4px 12px;");
		rowRentang.appendChild(labelRentang);

		// Update label rentang saat tanggal berubah
		org.zkoss.zk.ui.event.EventListener rangeUpdater = new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (dateboxMulai.getValue() != null && dateboxSelesai.getValue() != null) {
					long diffMs = dateboxSelesai.getValue().getTime() - dateboxMulai.getValue().getTime();
					long hari = diffMs / (1000L * 60 * 60 * 24) + 1;
					labelRentang.setValue("Rentang: " + hari + " hari");
				}
			}
		};
		dateboxMulai.addEventListener("onChange", rangeUpdater);
		dateboxSelesai.addEventListener("onChange", rangeUpdater);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:none; background:transparent;");
		toolbar.setParent(vbox);

		MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		btnBatal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.setParent(null);
			}
		});
		btnBatal.setParent(toolbar);

		MyToolbarbuttonConfig btnBersihkan = new MyToolbarbuttonConfig("Bersihkan Sekarang", "/img/svg/trash.svg");
		btnBersihkan.setStyle("color:#c0392b;");
		btnBersihkan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				final Date tglMulai = dateboxMulai.getValue();
				final Date tglSelesaiRaw = dateboxSelesai.getValue();

				if (tglMulai == null) {
					MyMessageboxConfig.show("Tanggal Mulai wajib diisi", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (tglSelesaiRaw == null) {
					MyMessageboxConfig.show("Tanggal Selesai wajib diisi", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (tglMulai.after(tglSelesaiRaw)) {
					MyMessageboxConfig.show("Tanggal Mulai tidak boleh setelah Tanggal Selesai", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				// Batas selesai sampai akhir hari
				Calendar calEnd = Calendar.getInstance();
				calEnd.setTime(tglSelesaiRaw);
				calEnd.set(Calendar.HOUR_OF_DAY, 23);
				calEnd.set(Calendar.MINUTE, 59);
				calEnd.set(Calendar.SECOND, 59);
				calEnd.set(Calendar.MILLISECOND, 999);
				final Date tglSelesai = calEnd.getTime();

				// Hitung dulu kandidat yang akan dihapus
				Session session = HibernateUtil.currentSession();
				final List<Perkuliahan> kandidat = ConstantValues.simpleList(
						session.createCriteria(Perkuliahan.class)
								.add(Restrictions.ge("tanggal_dirubah", tglMulai))
								.add(Restrictions.le("tanggal_dirubah", tglSelesai))
								.add(merupakanRemedial ? Restrictions.sqlRestriction("true")
										: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterPendek))
								.add(merupakanPraPerkuliahan != null && merupakanPraPerkuliahan
										? Restrictions.eq("merupakanPraPerkuliahan", true)
										: Restrictions.or(Restrictions.isNull("merupakanPraPerkuliahan"),
												Restrictions.eq("merupakanPraPerkuliahan", false)))
								.add(merupakanPerkuliahanUmum != null && merupakanPerkuliahanUmum
										? Restrictions.eq("merupakanPerkuliahanUmum", true)
										: Restrictions.or(Restrictions.isNull("merupakanPerkuliahanUmum"),
												Restrictions.eq("merupakanPerkuliahanUmum", false))),
						Perkuliahan.class);

				// Saring: hanya yang benar-benar kosong (tidak ada detailperkuliahan & pertemuan)
				final List<Perkuliahan> akanDihapus = new ArrayList<Perkuliahan>();
				for (Perkuliahan p : kandidat) {
					Integer jumlahDetail = p.ambilJumlahDetailperkuliahanLangsung();
					Integer jumlahPertemuan = p.ambilJumlahPertemuan();
					if (jumlahDetail != null && jumlahDetail.equals(0)
							&& jumlahPertemuan != null && jumlahPertemuan.equals(0)) {
						akanDihapus.add(p);
					}
				}

				if (akanDihapus.isEmpty()) {
					MyMessageboxConfig.show(
							"Tidak ditemukan data perkuliahan kosong pada rentang tanggal yang dipilih.\n"
							+ "Semua perkuliahan dalam rentang ini sudah memiliki mahasiswa atau pertemuan.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				String pesan = "Ditemukan " + akanDihapus.size() + " data perkuliahan yang akan dihapus.\n\n"
						+ "Kriteria: tidak ada mahasiswa KRS dan tidak ada pertemuan,\n"
						+ "dalam rentang " + Common.dateFormat3.get().format(tglMulai)
						+ " s/d " + Common.dateFormat3.get().format(tglSelesaiRaw) + ".\n\n"
						+ "Apakah Anda yakin ingin menghapus data tersebut?\n"
						+ "Tindakan ini tidak dapat dibatalkan.";

				MyMessageboxConfig.show(pesan, "Konfirmasi Hapus",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event evtKonfirmasi) throws Exception {
								int pilihan = Integer.parseInt(evtKonfirmasi.getData().toString());
								if (pilihan != MyMessageboxConfig.OK) {
									return;
								}
								Session sess = HibernateUtil.currentSession();
								int berhasilHapus = 0;
								int gagalHapus = 0;
								for (Perkuliahan p : akanDihapus) {
									try {
										sess.delete(p);
										berhasilHapus++;
									} catch (Exception ex) {
										gagalHapus++;
									}
								}
								String hasilPesan = "Pembersihan selesai.\n"
										+ "Berhasil dihapus : " + berhasilHapus + " data\n"
										+ (gagalHapus > 0 ? "Gagal dihapus    : " + gagalHapus + " data" : "");
								MyMessageboxConfig.show(hasilPesan, "Selesai",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										new EventListener() {
											@Override
											public void onEvent(Event evtSelesai) throws Exception {
												window.setParent(null);
												onSearchDefault(null);
											}
										});
							}
						});
			}
		});
		btnBersihkan.setParent(toolbar);

		window.onModal();
	}
}
