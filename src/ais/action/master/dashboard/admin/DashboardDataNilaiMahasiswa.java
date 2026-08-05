package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardDataNilaiMahasiswa &mdash; Rincian Nilai Mahasiswa per Matakuliah</h1>
 *
 * <p>
 * Berbeda dengan dasbor rekap IPK/IPS ({@code DashboardRekapNilaiMahasiswa}, yang meringkas nilai per
 * mahasiswa), dasbor ini menampilkan data <b>satu baris per catatan penilaian mata kuliah</b>
 * ({@code Detailperkuliahan}): NIM, nama mahasiswa, kelas, kode &amp; nama mata kuliah, dosen pengampu,
 * semester, tahun akademik, nilai angka, huruf mutu, nilai IP, jumlah SKS, rincian kehadiran, serta
 * rincian komponen nilai (tugas/UTS/UAS, dsb.) dan tanggal terakhir diubah. Dasbor ini dipakai staf
 * akademik dan dosen untuk menelusuri secara rinci nilai per mata kuliah &mdash; baik lewat panel
 * filter (Fakultas, Prodi, Tahun Akademik, Dosen, jenis &amp; nomor semester, angkatan, program,
 * matakuliah, nilai huruf, kelas, atau nama/NIM mahasiswa), maupun langsung dari konteks satu
 * {@link Perkuliahan} atau satu {@link Mahasiswa} tertentu (lihat konstruktor terkait) yang otomatis
 * memproses tanpa menampilkan form filter. Hasilnya dapat diunduh sebagai berkas Excel lewat tombol
 * <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak (form filter di region {@code North}, area hasil di region
 * {@code Center}) beserta event-listener kombo yang saling bergantung. {@link #initSpreadsheet()}
 * adalah method inti yang dipanggil saat tombol <b>Proses</b> ditekan (atau otomatis saat dasbor
 * dibuka dari konteks {@link Perkuliahan}/{@link Mahasiswa}): karena penyusunan data bisa memakan
 * waktu lama, proses berat ini dijalankan pada {@link Thread} latar belakang terpisah agar antarmuka
 * ZK tidak macet/hang menunggu. Selama proses berjalan, {@code Common.displayLoadBar(...)} menampilkan
 * indikator "sedang memproses" yang di-<i>polling</i> lewat komponen {@link Label} tersembunyi; begitu
 * label dikosongkan, berkas Excel yang sudah selesai ditulis ke folder sementara otomatis ditampilkan
 * sebagai tabel/grid ringan (lihat {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid},
 * dipanggil otomatis oleh {@code Common.displayLoadBar(...)}) alih-alih widget Excel penuh yang berat
 * di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel rinci, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (total data nilai,
 * rata-rata nilai angka, rata-rata nilai IP, rata-rata kehadiran), grafik donat komposisi huruf mutu,
 * grafik batang sebaran rentang nilai angka, dan grafik garis tren rata-rata nilai per semester (bila
 * data mencakup lebih dari satu semester). Seluruh angka pada panel ini dihitung <b>bersamaan</b>
 * dengan penulisan sel Excel di dalam {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis
 * data tambahan) lalu dirender lewat {@code ais.ui.util.HtmlChartHelper} setelah loop selesai,
 * dibungkus percobaan/tangkapan tersendiri supaya kegagalan menggambar grafik tidak pernah mengganggu
 * penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data penilaian pada praktiknya bisa tidak lengkap (mata kuliah/mahasiswa terkait sudah terhapus,
 * jadwal pertemuan belum lengkap, dsb.). Setiap catatan {@code Detailperkuliahan} diproses di dalam
 * blok percobaan tersendiri: bila satu data bermasalah, kesalahannya dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record} dan proses lanjut ke data berikutnya alih-alih menghentikan
 * seluruh rekap.
 * </p>
 *
 * <h2>Sesi Hibernate</h2>
 * <p>
 * Thread latar belakang mengambil sesi lewat {@code HibernateUtil.currentNativeSession()} (sesi native,
 * BUKAN sesi ber-scope permintaan/{@code currentSession()}), sehingga wajib ditutup secara eksplisit;
 * penutupan dilakukan tepat satu kali di blok {@code finally} agar sesi selalu tertutup baik proses
 * berhasil maupun gagal, dan tidak dipanggil berulang di banyak tempat.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class DashboardDataNilaiMahasiswa extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Intbox angkatan = new Intbox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private AmbilDataMatakuliahBanbox ambilDataMatakuliahBanbox = new AmbilDataMatakuliahBanbox();
	private Textbox nilaiHuruf = new Textbox();
	private Textbox kelas = new Textbox();
	private Textbox mhs = new Textbox();
	private Center center = new Center();

	private Map<Long, String> statuses = new HashMap<Long, String>();

	private File file;
	private Perkuliahan perkuliahan = null;
	private Mahasiswa mahasiswa = null;

	public DashboardDataNilaiMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiMahasiswa(Perkuliahan perkuliahan) {
		super();
		try {
			this.perkuliahan = perkuliahan;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiMahasiswa(Mahasiswa mahasiswa) {
		super();
		try {
			this.mahasiswa = mahasiswa;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/**
	 * Membangun seluruh tata letak dasbor: panel filter (region {@code North}) berisi kombo
	 * Fakultas/Prodi/Tahun Akademik/Dosen, kombo jenis semester (Ganjil/Genap/SP) yang mengisi ulang
	 * pilihan "Semester ke" secara dinamis, kotak teks Matakuliah/Nilai Huruf/Mahasiswa/Kelas untuk
	 * pencarian bebas, serta area hasil (region {@code Center}) dan tombol Proses/Download pada
	 * toolbar. Bila dasbor dibuka dari konteks {@link Perkuliahan} atau {@link Mahasiswa} tertentu
	 * (lihat konstruktor terkait), panel filter disembunyikan ({@code north.setVisible(false)}) karena
	 * konteksnya sudah tetap, dan proses langsung dijalankan otomatis (lihat pemanggilan
	 * {@link #initSpreadsheet()} di konstruktor). Method ini hanya menyiapkan komponen UI; proses
	 * pengambilan dan penulisan data baru dijalankan saat tombol Proses ditekan, lihat
	 * {@link #initSpreadsheet()}.
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init() throws Exception {

		Map<Long, GeneralValueObject> statusPertemuans = ConstantValues.ambilBerdasarClass(StatusPertemuan.class);

		for (GeneralValueObject s : statusPertemuans.values()) {
			StatusPertemuan statusPertemuan = (StatusPertemuan) s;
			if (statusPertemuan != null && statusPertemuan.getAktif()) {
				statuses.put(statusPertemuan.getId(), statusPertemuan.getNama());
			}
		}
		initFakultas();

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		final North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("240px");
		north.setAutoscroll(true);
		north.setVisible(perkuliahan == null && mahasiswa == null);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Html deskripsiDasbor = new Html();
		deskripsiDasbor.setContent("<div style='font-size:13px;color:#475569;padding:2px 4px 8px;'>"
				+ "Menampilkan rincian nilai tiap mahasiswa per matakuliah beserta huruf mutu, nilai IP, dan "
				+ "kehadiran, lengkap dengan ringkasan grafik performa nilai, sehingga hasil penilaian mudah "
				+ "ditelusuri tanpa membuka data satu per satu.</div>");
		deskripsiDasbor.setParent(row);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue(null);
		angkatan.setWidth("90%");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
				angkatan.setValue(null);
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}
			}
		};

		searchsemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final String tahunAkademik = (String) (DashboardDataNilaiMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardDataNilaiMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				final String semester = (String) (DashboardDataNilaiMahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardDataNilaiMahasiswa.this.semesterAbsensi.getSelectedItem().getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
						: searchsemester.getSelectedItem().getValue());

				if (tahunAkademik == null) {
					return;
				}
				final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
				final Boolean semuasemester = searchsemester.getSelectedItem() == null;
				if (!semuasemester) {
					angkatan.setValue(tahunAngkatan);
				}
			}
		});

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final String tahunAkademik = (String) (DashboardDataNilaiMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardDataNilaiMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				final String semester = (String) (DashboardDataNilaiMahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardDataNilaiMahasiswa.this.semesterAbsensi.getSelectedItem().getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
						: searchsemester.getSelectedItem().getValue());

				if (tahunAkademik == null) {
					return;
				}
				final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
				final Boolean semuasemester = searchsemester.getSelectedItem() == null;
				if (!semuasemester) {
					angkatan.setValue(tahunAngkatan);
				}
			}
		});

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(ambilDataMatakuliahBanbox);
		ambilDataMatakuliahBanbox.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Huruf"));
		row.appendChild(nilaiHuruf);
		nilaiHuruf.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mhs);
		mhs.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					if (file == null || !file.exists() || file.length() <= 0L) {
						MyMessageboxConfig.show("File rekap belum tersedia. Klik Proses terlebih dahulu.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"REKAP_PENILAIAN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java:448");

				}
			}
		});

		if (perkuliahan != null || mahasiswa != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					initSpreadsheet();

					Common.clear(north);
					north.setVisible(true);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(north);

					MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
					print.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								Filedownload.save(new FileInputStream(file),
										"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
										"REKAP_PENILAIAN.xlsx");
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java:476");

							}
						}
					});
					print.setParent(toolbar);
				}
			});
		} else {
			print.setParent(toolbar);
		}

	}

	private boolean prosentaseMengikutiDefaultPertemuan = Common.bolehKonfigurasi("prosentase_mengikuti_default_jumlah_pertemuan", Konfigurasi.TIDAK_AKTIF);

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna (atau konteks
	 * {@link Perkuliahan}/{@link Mahasiswa} tetap bila dasbor dibuka dari sana), lalu menjalankan
	 * pencarian daftar {@code Detailperkuliahan} yang cocok dan menuliskannya sebagai berkas Excel
	 * (satu baris per catatan penilaian mata kuliah) pada {@link Thread} latar belakang terpisah agar
	 * antarmuka tidak macet untuk data yang banyak. Bersamaan dengan penulisan sel Excel, method ini
	 * juga mengumpulkan ringkasan angka (rata-rata nilai/IP/kehadiran, komposisi huruf mutu, sebaran
	 * nilai, tren per semester) untuk panel grafik HTML/CSS yang dirender lewat
	 * {@link #renderChartRingkasan}. Region {@code Center} dibungkus satu {@link Vlayout} berisi dua
	 * anak: panel grafik ({@code chartPanel}) dan wadah grid/Excel ({@code gridHost}) yang terpisah,
	 * sehingga saat {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area hasil di
	 * akhir proses, panel grafik yang sudah tampil tidak ikut terhapus. Setiap catatan
	 * {@code Detailperkuliahan} diproses dalam blok percobaan tersendiri: data yang bermasalah dicatat
	 * lalu dilewati, tidak menggagalkan seluruh proses. Sesi Hibernate native ditutup satu kali di blok
	 * {@code finally}.
	 */
	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		final String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
				: searchsemester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final String mhs = this.mhs.getValue().trim();
		final String kelas = this.kelas.getValue().trim();

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");
		final Matakuliah matakuliah = (Matakuliah) ambilDataMatakuliahBanbox.getAttribute("matakuliah");

		if (tahunAkademik == null) {
			return;
		}
		final Integer tahunAngkatan = angkatan.getValue();

		final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

		System.out.println("init spreadsheet running => tahun = " + tahun);

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		// Center hanya boleh 1 anak -> bungkus dlm Vlayout: panel grafik ringkasan di atas, wadah
		// grid/Excel di bawah lewat Div terpisah (gridHost) supaya saat Common.displayLoadBar(...)
		// membersihkan & mengisi ulang area hasil, panel grafik yang sudah tampil tidak ikut terhapus.
		final Vlayout centerBox = new Vlayout();
		centerBox.setWidth("100%");
		centerBox.setHeight("100%");
		centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);

		final Html chartPanel = new Html();
		chartPanel.setParent(centerBox);

		final Div gridHost = new Div();
		gridHost.setStyle("width:100%;");
		gridHost.setParent(centerBox);

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, gridHost, sizedata);

		// Desktop HARUS ditangkap di sini (thread event ZK yang benar), bukan di dalam
		// background Thread di bawah -- di dalam Thread biasa tidak ada Execution/Desktop
		// context sama sekali. Dipakai belakangan lewat Executions.schedule(...) supaya
		// renderChartRingkasan (yang memanggil Html.setContent, mengubah komponen ZK)
		// dieksekusi kembali di event thread ZK yang sah, bukan langsung dari background
		// thread -> mencegah "IllegalStateException: Components can be accessed only in
		// event listeners".
		final Desktop desktop = Executions.getCurrent() == null ? null : Executions.getCurrent().getDesktop();

		// FIX Error A (IllegalStateException: server push must be enabled) & Error B
		// (Components can be accessed only in event listeners): kelas ini memakai Thread
		// latar mentah lalu memanggil Executions.schedule(desktop, ...) tanpa PERNAH
		// mengaktifkan server push desktop terlebih dahulu -- Executions.schedule()
		// MEWAJIBKAN desktop.isServerPushEnabled()==true (lihat DesktopImpl.checkSeverPush).
		// Pola sama dgn ais.common.AsyncTaskManager/CommonTimerHelper: reference-count per
		// Desktop (attribute key SAMA supaya kompatibel jika dipakai bersamaan dgn helper
		// lain pada desktop yg sama), enable sebelum Thread mulai, disable setelah proses
		// render chart di event listener terjadwal selesai (lihat decrefServerPush di bawah).
		increfServerPush(desktop);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				System.out.println("tahunAkademik = " + tahunAkademik);

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("PENILAIAN");
				sheet.setDefaultColumnWidth(25);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("NAMA");
				rowhead.createCell(2).setCellValue("KELAS");
				rowhead.createCell(3).setCellValue("KODE MATAKULIAH");
				rowhead.createCell(4).setCellValue("NAMA MATAKULIAH");
				rowhead.createCell(5).setCellValue("DOSEN");
				rowhead.createCell(6).setCellValue("SEMESTER");
				rowhead.createCell(7).setCellValue("TAHUN AKADEMIK");
				rowhead.createCell(8).setCellValue("NILAI");
				rowhead.createCell(9).setCellValue("HURUF");
				rowhead.createCell(10).setCellValue("NILAI IP");
				rowhead.createCell(11).setCellValue("JUMLAH SKS");
				rowhead.createCell(12).setCellValue("KEHADIRAN");
				rowhead.createCell(13).setCellValue("KEHADIRAN(%)");
				rowhead.createCell(14).setCellValue("NILAI RINCI");
				rowhead.createCell(15).setCellValue("TERAKHIR DIUBAH");

				Session session = HibernateUtil.currentNativeSession();

				Criterion criterion = Restrictions.sqlRestriction("false");
				if (dosen != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen1", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen2", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen3", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen4", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen5", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen6", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen7", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen8", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen9", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen10", dosen));
				}

				Criteria criteria = session.createCriteria(Detailperkuliahan.class)
						.setProjection(Projections.property("id"))
						.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
						.add(matakuliah == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perkuliahan.matakuliah", matakuliah),
										Restrictions.eq("matakuliahKonversi", matakuliah)));

				List<Long> perkuliahans = DashboardDataNilaiMahasiswa.this.mahasiswa != null
						? DashboardDataNilaiMahasiswa.this.mahasiswa.ambilDetailperkuliahan()
						: (DashboardDataNilaiMahasiswa.this.perkuliahan != null
								? new ArrayList<Long>(
										DashboardDataNilaiMahasiswa.this.perkuliahan.ambilDetailperkuliahan())
								: criteria

										.add(nilaiHuruf.getValue().trim().isEmpty()
												? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nilaiHuruf", nilaiHuruf.getValue().trim(),
														MatchMode.EXACT))

										.createAlias("mahasiswa", "mahasiswa")

										// .add(!semuasemester ?
										// Restrictions.eq(
										// "mahasiswa.tahunangkatan",
										// tahunAngkatan)
										// :
										// Restrictions.sqlRestriction("true"))

										.add(dosen == null ? Restrictions.sqlRestriction("true") : criterion)

										.createAlias("mahasiswa.jurusan", "jurusan")

										.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("semester"))

										.add(kelas.isEmpty() ? Restrictions.sqlRestriction("1=1")
												: Restrictions.ilike("mahasiswa.kelas", kelas, MatchMode.ANYWHERE))

										.add(mhs.isEmpty() ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(
														Restrictions.ilike("mahasiswa.nim", mhs, MatchMode.ANYWHERE),
														Restrictions.ilike("mahasiswa.nama", mhs, MatchMode.ANYWHERE)))

										.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("jurusan.fakultas", fakultas))

										.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("mahasiswa.jurusan", jurusan))

										.add(program == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("mahasiswa.program", program))

										.add(tahunAngkatan == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("mahasiswa.tahunangkatan", tahunAngkatan))

										.add(semesterKe == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("semester", semesterKe))

										.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(Restrictions.eq("tahunAkademik", tahunAkademik),
														Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik)))

										.add(semester == null ? Restrictions.sqlRestriction("1=1")
												: (semester.equals(Perkuliahan.SP)
														? Restrictions.eq("perkuliahan.statusSemesterPendek",
																Perkuliahan.SEMESTER_PENDEK)
														: semester.equals(Perkuliahan.GANJIL)
																? Restrictions.in("semester", Common.ganjil)
																: Restrictions.in("semester", Common.genap)))

										.list());

				int size = perkuliahans.size();

				System.out.println("size => " + size);

				int rowIndex = 1;
				int rowIndexAda = 1;

				// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel di
				// bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop selesai.
				final Map<String, int[]> jumlahPerHuruf = new LinkedHashMap<String, int[]>();
				final Map<Integer, double[]> perSemester = new TreeMap<Integer, double[]>();
				final int[] sebaranNilai = new int[5];
				double akumulasiNilai = 0;
				double akumulasiIp = 0;
				double akumulasiKehadiran = 0;
				int jumlahValid = 0;
				int jumlahKehadiranValid = 0;

				for (Long detailperkuliahanid : perkuliahans) {
					// KE-4: thread latar; panggilan bersarang (ambilData/populateDosen/hitungStatus) bisa menutup
					// native session -> "Session is closed!" saat createCriteria. Ambil ULANG (self-healing).
					session = HibernateUtil.currentNativeSession();
					try {
					if (detailperkuliahanid != null) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
								.createCriteria(Detailperkuliahan.class).add(Restrictions.idEq(detailperkuliahanid))
								.uniqueResult();

						if (detailperkuliahan != null) {

							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && detailperkuliahan.getMahasiswa() != null) {

								String detailNilai = detailperkuliahan.getDetailNilai();
								String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
								String nilaiRinci = "";
								for (String nn : nilais) {
									try {

										String[] s = nn.split(",");
										if (!s[0].trim().isEmpty()) {
											Long formatId = Long.parseLong(s[0]);
											Double nilai = Double.parseDouble(s[1]);
											String format = statuses.get(formatId);
											nilaiRinci += nilaiRinci.isEmpty()
													? format + ":" + Common.numberFormat.get().format(nilai)
													: "; " + format + ":" + Common.numberFormat.get().format(nilai);
										}
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								label.setValue("Sedang memproses data " + detailperkuliahan.toString() + " ("
										+ Common.numberFormat.get().format(rowIndexAda * 100.0 / size) + " %)");
								rowIndexAda++;

								String namaDosen = "";
								if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
									List<Dosen> dosens = detailperkuliahan.getPerkuliahan().populateDosenBuNama();
									for (Dosen d : dosens) {
										namaDosen += namaDosen.isEmpty() ? d.getNama() : ", " + d.getNama();
									}
								}

								XSSFRow row = sheet.createRow(rowIndex);
								XSSFCell cell = row.createCell(0);
								cell.setCellValue(detailperkuliahan.getMahasiswa().getNim());

								cell = row.createCell(1);
								cell.setCellValue(detailperkuliahan.getMahasiswa().getNama());

								cell = row.createCell(2);
								cell.setCellValue(detailperkuliahan.getPerkuliahan() == null ? ""
										: detailperkuliahan.getPerkuliahan().getKelas());

								cell = row.createCell(3);
								cell.setCellValue(matakuliah == null ? "" : matakuliah.getKode());

								cell = row.createCell(4);
								cell.setCellValue(matakuliah == null ? "" : matakuliah.getNama());

								cell = row.createCell(5);
								cell.setCellValue(namaDosen);

								cell = row.createCell(6);
								cell.setCellValue(detailperkuliahan.getSemester());

								cell = row.createCell(7);
								cell.setCellValue(detailperkuliahan.getTahunAkademik());

								double nilaiAngka = angkaAman(detailperkuliahan.getTotalNilai());
								double ipAngka = angkaAman(detailperkuliahan.getTotalIP());
								String hurufNilai = detailperkuliahan.getNilaiHuruf();

								cell = row.createCell(8);
								cell.setCellValue(nilaiAngka);

								cell = row.createCell(9);
								cell.setCellValue(hurufNilai);

								cell = row.createCell(10);
								cell.setCellValue(ipAngka);

								cell = row.createCell(11);
								cell.setCellValue(matakuliah == null ? 0 : matakuliah.getSks());

								if (detailperkuliahan.getPerkuliahan() != null) {
									ArrayList<String> statusPertemuan = new ArrayList<String>();
									for (Long pertemuanid : detailperkuliahan.getPerkuliahan().ambilPertemuan()
											.values()) {
										if (pertemuanid == null) {
											continue;
										}
										Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
												pertemuanid.toString());
										if (pertemuan != null && pertemuan.getAbsensi() != null) {
											if (!pertemuan.getAbsensi().trim().isEmpty()) {
												statusPertemuan.add(pertemuan.getAbsensi());
											}
										}
									}
									detailperkuliahan.getPerkuliahan();
									Map<String, Integer> statuses = Perkuliahan.hitungStatus(statusPertemuan,
											detailperkuliahan.getMahasiswa().getId());

									int semua = statuses.get("T") == null ? 0 : statuses.get("T");
									int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

									if (prosentaseMengikutiDefaultPertemuan) {
										semua = 16;
										try {
											semua = Integer.parseInt(
													Common.getKonfigurasi("jumlah_pertemuan_perkuliahan_default", "16")
															.getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java:782");

										}
									}

									String rinciAbsen = "";
									for (String key : statuses.keySet()) {
										if (!key.equals("T")) {
											int v = statuses.get(key);
											rinciAbsen += rinciAbsen.isEmpty() ? (key + "=" + v)
													: ", " + (key + "=" + v);
										}
									}

									cell = row.createCell(12);
									cell.setCellValue(rinciAbsen);

									double persen = semua == 0 ? 0.0 : (masuk * 100.0) / semua;

									cell = row.createCell(13);
									cell.setCellValue(Common.numberFormat.get().format(persen) + "%");

									akumulasiKehadiran += persen;
									jumlahKehadiranValid++;
								} else {
									cell = row.createCell(12);
									cell.setCellValue("Merupakan konversi");
									cell = row.createCell(13);
									cell.setCellValue("");
								}

								cell = row.createCell(14);
								cell.setCellValue(nilaiRinci);

								cell = row.createCell(15);
								cell.setCellValue(detailperkuliahan.getTanggal_dirubah() == null ? ""
										: Common.dateFormat5.get().format(detailperkuliahan.getTanggal_dirubah()));

								// -- Akumulasi ringkasan panel grafik: KPI, donat komposisi huruf, sebaran
								// nilai angka, tren rata-rata nilai per semester -- pakai nilai yang sama
								// dgn yang baru saja ditulis ke sel Excel di atas, tanpa kueri tambahan. --
								jumlahValid++;
								akumulasiNilai += nilaiAngka;
								akumulasiIp += ipAngka;

								if (hurufNilai != null && !hurufNilai.trim().isEmpty()) {
									int[] c = jumlahPerHuruf.get(hurufNilai);
									if (c == null) {
										c = new int[1];
										jumlahPerHuruf.put(hurufNilai, c);
									}
									c[0]++;
								}

								int idxSebaran = nilaiAngka < 50 ? 0 : nilaiAngka < 60 ? 1 : nilaiAngka < 70 ? 2
										: nilaiAngka < 80 ? 3 : 4;
								sebaranNilai[idxSebaran]++;

								Integer semesterDetail = detailperkuliahan.getSemester();
								if (semesterDetail != null && semesterDetail >= 1) {
									double[] agg = perSemester.get(semesterDetail);
									if (agg == null) {
										agg = new double[2];
										perSemester.put(semesterDetail, agg);
									}
									agg[0] += nilaiAngka;
									agg[1] += 1;
								}

								rowIndex++;
							}
						}
					}
					} catch (Exception exBaris) {
						// Satu data detail perkuliahan bermasalah (mis. mahasiswa/matakuliah terkait sudah
						// terhapus atau data tidak lengkap) tidak boleh menggagalkan seluruh proses rekap;
						// catat & lanjut ke data berikutnya.
						ais.common.ErrorAuditUtil.record(exBaris,
								"auto-guard(per-detailperkuliahan) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java");
					}
				}

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					Common.tampilErrorJikaAdmin(e);
				}

				// -- Render panel grafik (HTML/CSS, BUKAN JFreeChart) dari ringkasan yang sudah
				// dihitung di loop di atas. Kegagalan menggambar grafik tidak boleh mengganggu
				// Excel/grid yang sudah berhasil ditulis. --
				// PENTING: method ini sedang berjalan di background Thread (bukan event thread
				// ZK), sehingga TIDAK BOLEH memanggil renderChartRingkasan(...) secara langsung
				// di sini -- renderChartRingkasan mengubah komponen ZK (Html.setContent) yang
				// hanya boleh diakses dari event listener resmi ZK (butuh Desktop/Execution
				// context yang di-attach ke thread event, bukan thread biasa). Jadwalkan lewat
				// Executions.schedule(desktop, ...) mengikuti pola yang sudah dipakai di
				// DashboardTrenAktivitasPerkuliahan#renderDashboardDataOnUi supaya konsisten.
				try {
					final int jumlahValidFinal = jumlahValid;
					final double akumulasiNilaiFinal = akumulasiNilai;
					final double akumulasiIpFinal = akumulasiIp;
					final double akumulasiKehadiranFinal = akumulasiKehadiran;
					final int jumlahKehadiranValidFinal = jumlahKehadiranValid;

					if (desktop != null && desktop.isAlive()) {
						Executions.schedule(desktop, new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								try {
									// Halaman bisa saja sudah ditinggalkan pengguna persis di antara
									// Thread selesai menghitung dan event terjadwal ini akhirnya
									// dieksekusi -- skip diam-diam (bukan kondisi error) jika komponen
									// target sudah lepas dari halaman.
									if (chartPanel != null && chartPanel.getPage() != null) {
										renderChartRingkasan(chartPanel, jumlahValidFinal, akumulasiNilaiFinal,
												akumulasiIpFinal, akumulasiKehadiranFinal, jumlahKehadiranValidFinal,
												jumlahPerHuruf, sebaranNilai, perSemester);
									}
								} finally {
									// Push diaktifkan sebelum Thread latar dimulai (lihat increfServerPush
									// di atas) -- lepas di sini, setelah render chart benar-benar selesai.
									decrefServerPush(desktop);
								}
							}
						}, new Event("onRenderChartRingkasanNilaiMahasiswa"));
					} else {
						// Desktop sudah tidak aktif sebelum sempat dijadwalkan -> event listener di
						// atas tidak akan pernah jalan untuk melepas push, lepas di sini sbg fallback.
						decrefServerPush(desktop);
					}
				} catch (Exception exChart) {
					// Gagal sebelum/scaat menjadwalkan (mis. Executions.schedule melempar exception) ->
					// event listener tidak akan jalan, lepas push di sini sbg fallback.
					decrefServerPush(desktop);
					ais.common.ErrorAuditUtil.record(exChart,
						"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java");
				}

				perkuliahans.clear();
				label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/**
	 * Mengembalikan nilai {@code double} aman dari {@link Double} yang mungkin {@code null} (mis. Nilai
	 * atau Nilai IP yang belum sempat dihitung), agar tidak terjadi {@code NullPointerException} saat
	 * unboxing.
	 */
	private static double angkaAman(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	// Reference-counted enable/disable server push per Desktop -- attribute key SAMA dgn
	// ais.common.AsyncTaskManager & ais.common.CommonTimerHelper supaya konsisten (tidak
	// saling mematikan push milik tugas async lain) bila dipakai bersamaan pada desktop yg
	// sama. Lihat komentar di AsyncTaskManager.java utk penjelasan lengkap akar masalahnya.
	private static final String PUSH_REFCOUNT_ATTR = "aisAsyncTaskManager.pushRefCount";

	private static void increfServerPush(Desktop desktop) {
		if (desktop == null) {
			return;
		}
		try {
			synchronized (desktop) {
				java.util.concurrent.atomic.AtomicInteger ref = (java.util.concurrent.atomic.AtomicInteger) desktop
						.getAttribute(PUSH_REFCOUNT_ATTR);
				if (ref == null) {
					ref = new java.util.concurrent.atomic.AtomicInteger(0);
					desktop.setAttribute(PUSH_REFCOUNT_ATTR, ref);
				}
				if (ref.incrementAndGet() == 1 && !desktop.isServerPushEnabled()) {
					desktop.enableServerPush(true);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e,
				"auto-audit src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java:increfServerPush"); }
	}

	private static void decrefServerPush(Desktop desktop) {
		if (desktop == null) {
			return;
		}
		try {
			synchronized (desktop) {
				java.util.concurrent.atomic.AtomicInteger ref = (java.util.concurrent.atomic.AtomicInteger) desktop
						.getAttribute(PUSH_REFCOUNT_ATTR);
				if (ref == null) {
					return;
				}
				if (ref.decrementAndGet() <= 0 && desktop.isAlive() && desktop.isServerPushEnabled()) {
					desktop.enableServerPush(false);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e,
				"auto-audit src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswa.java:decrefServerPush"); }
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total data nilai, rata-rata Nilai, rata-rata Nilai IP,
	 * rata-rata kehadiran), grafik donat komposisi huruf mutu, grafik batang sebaran rentang nilai
	 * angka (0-100), serta grafik garis tren rata-rata nilai per semester (hanya bila data mencakup
	 * minimal 2 semester berbeda agar tren bermakna). Seluruh parameter berasal dari akumulasi yang
	 * sudah dihitung selagi menulis sel Excel di {@link #initSpreadsheet()}, sehingga tidak ada kueri
	 * basis data tambahan yang dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel            komponen tampilan tujuan.
	 * @param jumlahValid           jumlah baris data nilai yang berhasil diproses.
	 * @param akumulasiNilai        jumlah (bukan rata-rata) nilai angka (0-100) seluruh baris yang berhasil diproses.
	 * @param akumulasiIp           jumlah (bukan rata-rata) nilai IP (0-4) seluruh baris yang berhasil diproses.
	 * @param akumulasiKehadiran    jumlah (bukan rata-rata) persentase kehadiran, hanya utk baris yang memiliki jadwal pertemuan.
	 * @param jumlahKehadiranValid  jumlah baris yang memiliki data kehadiran (bukan hasil konversi).
	 * @param jumlahPerHuruf        peta huruf mutu (A, B, C, D, E, dst.) -&gt; jumlah (untuk grafik donat).
	 * @param sebaranNilai          jumlah data per rentang nilai angka: [&lt;50, 50-59, 60-69, 70-79, 80-100].
	 * @param perSemester           peta nomor semester -&gt; {jumlah nilai, jumlah data} terurut ascending (untuk tren).
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahValid, double akumulasiNilai,
			double akumulasiIp, double akumulasiKehadiran, int jumlahKehadiranValid,
			Map<String, int[]> jumlahPerHuruf, int[] sebaranNilai, Map<Integer, double[]> perSemester) {

		double rataNilai = jumlahValid == 0 ? 0 : akumulasiNilai / jumlahValid;
		double rataIp = jumlahValid == 0 ? 0 : akumulasiIp / jumlahValid;
		double rataKehadiran = jumlahKehadiranValid == 0 ? 0 : akumulasiKehadiran / jumlahKehadiranValid;

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Data Nilai", "Rata-rata Nilai", "Rata-rata Nilai IP", "Rata-rata Kehadiran" },
				new String[] { String.valueOf(jumlahValid), Common.numberFormat.get().format(rataNilai),
						Common.numberFormat.get().format(rataIp), Common.numberFormat.get().format(rataKehadiran) + "%" },
				new String[] { "baris data berhasil diproses", "skala 0 - 100", "skala 0.00 - 4.00",
						"dari data yang memiliki jadwal pertemuan" },
				null, null, new String[] { "#2563eb", "#16a34a", "#0891b2", "#7c3aed" }));

		if (!jumlahPerHuruf.isEmpty()) {
			String[] labelHuruf = jumlahPerHuruf.keySet().toArray(new String[jumlahPerHuruf.size()]);
			double[] nilaiHuruf = new double[labelHuruf.length];
			for (int i = 0; i < labelHuruf.length; i++) {
				nilaiHuruf[i] = jumlahPerHuruf.get(labelHuruf[i])[0];
			}
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Huruf Mutu",
					"Menunjukkan berapa banyak data nilai pada masing-masing huruf mutu (A, B, C, D, E, dan "
							+ "lainnya) dari seluruh data yang direkap, sehingga proporsi capaian nilai mudah "
							+ "dibandingkan.",
					labelHuruf, nilaiHuruf, null, jumlahValid + " data"));
		}

		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Nilai Angka (0-100)",
				"Menunjukkan berapa banyak data nilai berada pada tiap rentang angka, dari yang paling rendah "
						+ "sampai paling tinggi, sehingga sebaran capaian nilai terlihat sekilas.",
				new String[] { "< 50", "50 - 59", "60 - 69", "70 - 79", "80 - 100" },
				new double[] { sebaranNilai[0], sebaranNilai[1], sebaranNilai[2], sebaranNilai[3], sebaranNilai[4] },
				"#2563eb"));

		if (perSemester.size() >= 2) {
			int nSmt = perSemester.size();
			String[] kategoriSmt = new String[nSmt];
			double[] seriNilai = new double[nSmt];
			int idx = 0;
			for (Map.Entry<Integer, double[]> entri : perSemester.entrySet()) {
				kategoriSmt[idx] = "Smt " + entri.getKey();
				double[] agg = entri.getValue();
				seriNilai[idx] = agg[1] == 0 ? 0 : agg[0] / agg[1];
				idx++;
			}
			html.append(ais.ui.util.HtmlChartHelper.lineMulti("Tren Rata-rata Nilai per Semester",
					"Menunjukkan naik-turunnya rata-rata nilai mahasiswa dari semester ke semester, sehingga "
							+ "perkembangan hasil belajar terlihat jelas.",
					kategoriSmt, new String[] { "Rata-rata Nilai" }, new double[][] { seriNilai },
					new String[] { "#0891b2" }));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
