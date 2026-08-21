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
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardDataNilaiDanIPKMahasiswa &mdash; Rincian Nilai per Mata Kuliah &amp; IPK Mahasiswa</h1>
 *
 * <p>
 * Dasbor ini dipakai oleh staf akademik, dosen, dan pimpinan program studi untuk menelusuri
 * <b>nilai per mata kuliah</b> (bukan rekap per mahasiswa seperti {@link DashboardRekapNilaiMahasiswa}) —
 * satu baris hasil mewakili satu nilai mata kuliah yang diambil seorang mahasiswa pada semester
 * tertentu, lengkap dengan Nilai angka, Huruf mutu, Nilai IP mata kuliah tersebut, jumlah SKS, serta
 * IPS/IPK/SKS/SKSK mahasiswa bersangkutan. Kelas ini memiliki tiga cara pemakaian: (1) dibuka langsung
 * dari menu dengan filter bebas (Fakultas, Prodi, Tahun Akademik, Dosen, jenis &amp; nomor semester,
 * Angkatan, Program, Matakuliah, Nilai Huruf, Kelas, atau nama/NIM mahasiswa) lalu menekan tombol
 * <b>Proses</b>; (2) dibuka dari halaman lain dengan sebuah objek {@link Perkuliahan} spesifik lewat
 * konstruktor {@link #DashboardDataNilaiDanIPKMahasiswa(Perkuliahan)}, menampilkan nilai seluruh peserta
 * kelas tersebut; atau (3) dibuka dengan sebuah objek {@link Mahasiswa} spesifik lewat konstruktor
 * {@link #DashboardDataNilaiDanIPKMahasiswa(Mahasiswa)}, menampilkan seluruh riwayat nilai mahasiswa
 * itu. Pada mode (2) dan (3) panel filter (region {@code North}) disembunyikan dan proses berjalan
 * otomatis begitu jendela dibuka.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak (form filter di region {@code North}, area hasil di region
 * {@code Center}) beserta listener kombo yang saling bergantung. {@link #initSpreadsheet()} adalah
 * method inti yang dipanggil saat tombol <b>Proses</b> ditekan (atau otomatis pada mode Perkuliahan
 * /Mahasiswa): karena daftar detail nilai bisa sangat banyak, penulisan berkas Excel dijalankan pada
 * {@link Thread} latar belakang terpisah agar antarmuka ZK tidak macet, dengan indikator "sedang
 * memproses" lewat {@code Common.displayLoadBar(...)}. Begitu proses selesai, berkas Excel yang sudah
 * ditulis otomatis ditampilkan sebagai tabel/grid ringan (lihat
 * {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid}, dipanggil otomatis oleh
 * {@code LoadBarUtils.loadSpreadsheet}) alih-alih widget Excel penuh yang berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini menampilkan panel ringkasan berupa kartu KPI (total data nilai,
 * rata-rata Nilai, rata-rata Nilai IP, total SKS tercatat), grafik donat komposisi huruf mutu, grafik
 * batang sebaran rentang nilai angka, grafik garis tren rata-rata nilai per semester (bila data
 * mencakup lebih dari satu semester), dan grafik radar perbandingan rata-rata nilai antar mata kuliah
 * (bila datanya mencakup 3 sampai 10 mata kuliah berbeda, agar bentuk jaring tetap terbaca). Seluruh
 * angka pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel Excel di dalam
 * {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data tambahan) lalu dirender lewat
 * {@code ais.ui.util.HtmlChartHelper} setelah loop selesai, dibungkus percobaan/tangkapan tersendiri
 * supaya kegagalan menggambar grafik tidak pernah mengganggu penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Setiap baris detail nilai diproses di dalam blok percobaan tersendiri: bila satu data bermasalah
 * (mis. mata kuliah sudah terhapus, relasi perkuliahan kosong), kesalahannya dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record} dan proses lanjut ke data berikutnya alih-alih menghentikan
 * seluruh rekap.
 * </p>
 *
 * <h2>Sesi Hibernate</h2>
 * <p>
 * Thread latar belakang mengambil sesi lewat {@code HibernateUtil.currentNativeSession()} (sesi
 * native, BUKAN sesi ber-scope permintaan/{@code currentSession()}), sehingga wajib ditutup secara
 * eksplisit; penutupan dilakukan tepat satu kali di blok {@code finally} agar sesi selalu tertutup
 * baik proses berhasil maupun gagal, dan tidak dipanggil berulang di banyak tempat.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class DashboardDataNilaiDanIPKMahasiswa extends MyWindow {

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

	public DashboardDataNilaiDanIPKMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiDanIPKMahasiswa(Perkuliahan perkuliahan) {
		super();
		try {
			this.perkuliahan = perkuliahan;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiDanIPKMahasiswa(Mahasiswa mahasiswa) {
		super();
		try {
			this.mahasiswa = mahasiswa;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiDanIPKMahasiswa(String title, String border, boolean closable) {
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
	 * Fakultas/Prodi/Tahun Akademik/Dosen, kombo jenis semester (Ganjil/Genap/SP), pemilih Matakuliah,
	 * kotak teks Nilai Huruf/Kelas/Mahasiswa untuk pencarian bebas, serta area hasil (region
	 * {@code Center}) dan tombol Proses/Download pada toolbar. Bila kelas ini dibuka lewat konstruktor
	 * {@link #DashboardDataNilaiDanIPKMahasiswa(Perkuliahan)} atau
	 * {@link #DashboardDataNilaiDanIPKMahasiswa(Mahasiswa)}, panel filter disembunyikan
	 * ({@code north.setVisible(false)}) dan {@link #initSpreadsheet()} dipanggil otomatis lewat timer
	 * karena target data (satu kelas atau satu mahasiswa) sudah pasti, tanpa perlu filter manual.
	 * Method ini hanya menyiapkan komponen UI; proses pengambilan dan penulisan data baru dijalankan
	 * saat tombol Proses ditekan (atau otomatis pada mode Perkuliahan/Mahasiswa), lihat
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
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("292px");
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
				+ "Menampilkan rincian nilai setiap mata kuliah yang diambil mahasiswa per semester, lengkap "
				+ "dengan huruf mutu, nilai IP mata kuliah, serta IPS/IPK, sehingga capaian nilai per mata "
				+ "kuliah mudah ditelusuri dan dibandingkan tanpa membuka data satu per satu.</div>");
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
				final String tahunAkademik = (String) (DashboardDataNilaiDanIPKMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardDataNilaiDanIPKMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				final String semester = (String) (DashboardDataNilaiDanIPKMahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardDataNilaiDanIPKMahasiswa.this.semesterAbsensi.getSelectedItem().getValue());

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
				final String tahunAkademik = (String) (DashboardDataNilaiDanIPKMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardDataNilaiDanIPKMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				final String semester = (String) (DashboardDataNilaiDanIPKMahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardDataNilaiDanIPKMahasiswa.this.semesterAbsensi.getSelectedItem().getValue());

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
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"REKAP_PENILAIAN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiDanIPKMahasiswa.java:447");

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
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiDanIPKMahasiswa.java:475");

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

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna (atau memakai
	 * {@link #perkuliahan}/{@link #mahasiswa} bila dasbor dibuka dalam mode spesifik), lalu menjalankan
	 * penyusunan daftar detail nilai pada {@link Thread} latar belakang terpisah (agar antarmuka tidak
	 * macet untuk data yang banyak), menuliskannya sebagai berkas Excel sekaligus mengumpulkan
	 * ringkasan angka (komposisi huruf mutu, sebaran nilai, tren per semester, rata-rata per mata
	 * kuliah) untuk panel grafik HTML/CSS yang dirender lewat {@link #renderChartRingkasan}. Region
	 * {@code Center} dibungkus satu {@link Vlayout} berisi dua anak: panel grafik ({@code chartPanel})
	 * dan wadah grid/Excel ({@code gridHost}) yang terpisah, sehingga saat
	 * {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area hasil di akhir proses,
	 * panel grafik yang sudah tampil tidak ikut terhapus. Setiap baris detail nilai diproses dalam
	 * blok percobaan tersendiri: data yang bermasalah dicatat lalu dilewati, tidak menggagalkan seluruh
	 * proses. Sesi Hibernate native ditutup satu kali di blok {@code finally}.
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

		/* UBAH 21-08-2026: tabel rincian sebelumnya hanya sebuah Div polos tanpa penanda apa pun,
		 * sehingga setelah panel grafik yang tinggi (kartu KPI + donut) ia terdorong ke bawah lipatan
		 * dan terlihat seolah hilang. Kini diberi judul bagian dan tinggi minimum supaya jelas bahwa
		 * rincian datanya ada di bawah grafik. Isinya tetap Grid ZK biasa -- widget Excel dirender
		 * lalu diganti Grid oleh PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(). */
		final org.zkoss.zul.Html judulRincian = new org.zkoss.zul.Html(
				"<div style='margin:14px 2px 6px;font-size:14px;font-weight:700;color:#0f172a;'>"
				+ "Rincian Data</div>"
				+ "<div style='margin:0 2px 8px;font-size:12px;color:#64748b;'>"
				+ "Tabel lengkap di bawah ini memuat data yang meringkas grafik di atas.</div>");
		judulRincian.setParent(centerBox);

		final Div gridHost = new Div();
		gridHost.setStyle("width:100%;min-height:220px;");
		gridHost.setParent(centerBox);

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, gridHost, sizedata);

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
				rowhead.createCell(12).setCellValue("IPS");
				rowhead.createCell(13).setCellValue("IPK");
				rowhead.createCell(14).setCellValue("SKS");
				rowhead.createCell(15).setCellValue("SKSK");

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

				List<Long> perkuliahans = DashboardDataNilaiDanIPKMahasiswa.this.mahasiswa != null
						? DashboardDataNilaiDanIPKMahasiswa.this.mahasiswa.ambilDetailperkuliahan()
						: (DashboardDataNilaiDanIPKMahasiswa.this.perkuliahan != null
								? new ArrayList<Long>(
										DashboardDataNilaiDanIPKMahasiswa.this.perkuliahan.ambilDetailperkuliahan())
								: criteria

										.add(nilaiHuruf.getValue().trim().isEmpty()
												? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nilaiHuruf", nilaiHuruf.getValue().trim(),
														MatchMode.EXACT))

										.createAlias("mahasiswa", "mahasiswa")

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
				final Map<String, int[]> jumlahPerHuruf = new TreeMap<String, int[]>();
				final Map<Integer, double[]> nilaiPerSemester = new TreeMap<Integer, double[]>();
				final Map<String, double[]> nilaiPerMatakuliah = new LinkedHashMap<String, double[]>();
				final int[] sebaranNilai = new int[5];
				double akumulasiNilai = 0;
				double akumulasiIP = 0;
				int totalSks = 0;
				int jumlahValid = 0;

				for (Long detailperkuliahanid : perkuliahans) {

					// Baris dianggap "dibuat" hanya bila sampai berhasil menulis seluruh sel -> rowIndex
					// hanya maju utk data yg benar2 tertulis (persis perilaku semula), TAPI kalau exception
					// terjadi SETELAH createRow (di tengah pengisian sel), rowIndex tetap maju di finally
					// supaya baris berikutnya tidak menimpa baris yg sudah separuh terisi.
					boolean barisDibuat = false;
					try {

						if (detailperkuliahanid == null) {
							continue;
						}
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
								.createCriteria(Detailperkuliahan.class).add(Restrictions.idEq(detailperkuliahanid))
								.uniqueResult();

						if (detailperkuliahan == null) {
							continue;
						}

						Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
								? detailperkuliahan.getMatakuliahKonversi()
								: detailperkuliahan.getPerkuliahan().getMatakuliah();
						if (matakuliah == null) {
							continue;
						}

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
						if (detailperkuliahan.getPerkuliahan() != null) {
							List<Dosen> dosens = detailperkuliahan.getPerkuliahan().populateDosenBuNama();
							for (Dosen d : dosens) {
								namaDosen += namaDosen.isEmpty() ? d.getNama() : ", " + d.getNama();
							}
						}

						XSSFRow row = sheet.createRow(rowIndex);
						XSSFCell cell = row.createCell(0);
						cell.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
								: detailperkuliahan.getMahasiswa().getNim());

						cell = row.createCell(1);
						cell.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
								: detailperkuliahan.getMahasiswa().getNama());

						cell = row.createCell(2);
						cell.setCellValue(detailperkuliahan.getPerkuliahan() == null ? ""
								: detailperkuliahan.getPerkuliahan().getKelas());

						cell = row.createCell(3);
						cell.setCellValue(matakuliah.getKode());

						cell = row.createCell(4);
						cell.setCellValue(matakuliah.getNama());

						cell = row.createCell(5);
						cell.setCellValue(namaDosen);

						// getSemester()/getTotalNilai()/getTotalIP()/getSks() adalah Integer/Double yg
						// BOLEH null (mis. nilai belum dihitung) -> dibungkus angkaAman/angkaAmanInt agar
						// tak NullPointerException saat auto-unboxing ke parameter primitif setCellValue.
						Integer semesterData = detailperkuliahan.getSemester();
						cell = row.createCell(6);
						cell.setCellValue(angkaAmanInt(semesterData));

						cell = row.createCell(7);
						cell.setCellValue(detailperkuliahan.getTahunAkademik());

						double nilaiAngka = angkaAman(detailperkuliahan.getTotalNilai());
						cell = row.createCell(8);
						cell.setCellValue(nilaiAngka);

						// Sel Excel tetap memakai nilai APA ADANYA (persis perilaku semula, termasuk
						// spasi bila ada, dan tetap sel kosong/blank bila null -> setCellValue(String)
						// sudah aman menerima null) -> "hurufChart" (versi trim) dipakai TERPISAH, hanya
						// utk mengelompokkan kategori pada grafik donat agar " A" dan "A" tak dobel.
						String hurufNilaiAsli = detailperkuliahan.getNilaiHuruf();
						cell = row.createCell(9);
						cell.setCellValue(hurufNilaiAsli);
						String hurufChart = hurufNilaiAsli == null ? "" : hurufNilaiAsli.trim();

						double nilaiIP = angkaAman(detailperkuliahan.getTotalIP());
						cell = row.createCell(10);
						cell.setCellValue(nilaiIP);

						int sksMatkul = angkaAmanInt(matakuliah.getSks());
						cell = row.createCell(11);
						cell.setCellValue(sksMatkul);

						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
								detailperkuliahan.getSemester(), null,
								detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().getStatusSemesterPendek());

						cell = row.createCell(12);
						cell.setCellValue(krsMahasiswa == null ? 0 : angkaAman(krsMahasiswa.getIps()));

						cell = row.createCell(13);
						cell.setCellValue(krsMahasiswa == null ? 0 : angkaAman(krsMahasiswa.getIpk()));

						cell = row.createCell(14);
						cell.setCellValue(krsMahasiswa == null ? 0 : angkaAmanInt(krsMahasiswa.getSksYangDiambil()));

						cell = row.createCell(15);
						cell.setCellValue(krsMahasiswa == null ? 0 : angkaAmanInt(krsMahasiswa.getSksk()));

						// -- Akumulasi ringkasan panel grafik: KPI, donat komposisi huruf mutu, sebaran
						// nilai angka, tren rata-rata nilai per semester, radar rata-rata nilai per mata
						// kuliah -- memakai nilai yg SAMA PERSIS dgn yg baru saja ditulis ke sel Excel di
						// atas, tanpa kueri basis data tambahan. --
						akumulasiNilai += nilaiAngka;
						akumulasiIP += nilaiIP;
						totalSks += sksMatkul;
						jumlahValid++;

						int idxSebaran = nilaiAngka < 60 ? 0 : nilaiAngka < 70 ? 1 : nilaiAngka < 80 ? 2
								: nilaiAngka < 90 ? 3 : 4;
						sebaranNilai[idxSebaran]++;

						if (!hurufChart.isEmpty()) {
							int[] c = jumlahPerHuruf.get(hurufChart);
							if (c == null) {
								c = new int[1];
								jumlahPerHuruf.put(hurufChart, c);
							}
							c[0]++;
						}

						if (semesterData != null) {
							double[] agg = nilaiPerSemester.get(semesterData);
							if (agg == null) {
								agg = new double[2];
								nilaiPerSemester.put(semesterData, agg);
							}
							agg[0] += nilaiAngka;
							agg[1] += 1;
						}

						String namaMatkulChart = matakuliah.getNama() == null || matakuliah.getNama().trim().isEmpty()
								? matakuliah.getKode() : matakuliah.getNama();
						if (namaMatkulChart != null && !namaMatkulChart.trim().isEmpty()) {
							double[] agg2 = nilaiPerMatakuliah.get(namaMatkulChart);
							if (agg2 == null) {
								agg2 = new double[2];
								nilaiPerMatakuliah.put(namaMatkulChart, agg2);
							}
							agg2[0] += nilaiAngka;
							agg2[1] += 1;
						}

						barisDibuat = true;

					} catch (Exception exBaris) {
						// Satu data detail nilai bermasalah (mis. mata kuliah sudah terhapus, relasi
						// perkuliahan kosong) tidak boleh menggagalkan seluruh proses rekap; catat & lanjut.
						ais.common.ErrorAuditUtil.record(exBaris,
								"auto-guard(per-baris) src/ais/action/master/dashboard/admin/DashboardDataNilaiDanIPKMahasiswa.java");
					} finally {
						if (barisDibuat) {
							rowIndex++;
						}
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
				try {
					renderChartRingkasan(chartPanel, jumlahValid, akumulasiNilai, akumulasiIP, totalSks,
							jumlahPerHuruf, sebaranNilai, nilaiPerSemester, nilaiPerMatakuliah);
				} catch (Exception exChart) { ais.common.ErrorAuditUtil.record(exChart,
						"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardDataNilaiDanIPKMahasiswa.java"); }

				perkuliahans.clear();
				label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardDataNilaiDanIPKMahasiswa.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/** Mengembalikan nilai {@code double} aman dari {@link Double} yang mungkin {@code null} (mis. Nilai/Nilai
	 * IP mata kuliah yang belum pernah dihitung), agar tidak terjadi {@code NullPointerException} saat unboxing. */
	private static double angkaAman(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	/** Mengembalikan nilai {@code int} aman dari {@link Integer} yang mungkin {@code null} (mis. semester atau
	 * jumlah SKS yang belum terisi), agar tidak terjadi {@code NullPointerException} saat unboxing. */
	private static int angkaAmanInt(Integer nilai) {
		return nilai == null ? 0 : nilai.intValue();
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total data nilai, rata-rata Nilai, rata-rata Nilai IP,
	 * total SKS tercatat), grafik donat komposisi huruf mutu (A, B, C, D, E, dst.), grafik batang
	 * sebaran rentang nilai angka, grafik garis tren rata-rata nilai per semester (hanya bila data
	 * mencakup lebih dari satu semester), serta grafik radar (jaring laba-laba) perbandingan rata-rata
	 * nilai antar mata kuliah (hanya bila datanya mencakup 3 sampai 10 mata kuliah berbeda, agar bentuk
	 * jaring tetap terbaca — bila lebih dari 10, radar dilewati karena terlalu padat untuk dibaca).
	 * Seluruh parameter berasal dari akumulasi yang sudah dihitung selagi menulis sel Excel di
	 * {@link #initSpreadsheet()}, sehingga tidak ada kueri basis data tambahan yang dijalankan hanya
	 * untuk kebutuhan grafik.
	 *
	 * @param chartPanel        komponen tampilan tujuan.
	 * @param jumlahValid       jumlah baris detail nilai yang berhasil diproses.
	 * @param akumulasiNilai    jumlah (bukan rata-rata) nilai angka seluruh baris yang berhasil diproses.
	 * @param akumulasiIP       jumlah (bukan rata-rata) nilai IP seluruh baris yang berhasil diproses.
	 * @param totalSks          jumlah SKS tercatat pada seluruh baris yang berhasil diproses.
	 * @param jumlahPerHuruf    peta huruf mutu -&gt; jumlah (untuk grafik donat), terurut alfabet.
	 * @param sebaranNilai      jumlah baris per rentang nilai angka: [&lt;60, 60-69, 70-79, 80-89, 90-100].
	 * @param nilaiPerSemester  peta nomor semester -&gt; {jumlah nilai, jumlah baris} (untuk tren), terurut naik.
	 * @param nilaiPerMatakuliah peta nama mata kuliah -&gt; {jumlah nilai, jumlah baris} (untuk radar).
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahValid, double akumulasiNilai,
			double akumulasiIP, int totalSks, Map<String, int[]> jumlahPerHuruf, int[] sebaranNilai,
			Map<Integer, double[]> nilaiPerSemester, Map<String, double[]> nilaiPerMatakuliah) {

		double rataNilai = jumlahValid == 0 ? 0 : akumulasiNilai / jumlahValid;
		double rataIP = jumlahValid == 0 ? 0 : akumulasiIP / jumlahValid;

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Data Nilai", "Rata-rata Nilai", "Rata-rata Nilai IP", "Total SKS Tercatat" },
				new String[] { String.valueOf(jumlahValid), Common.numberFormat.get().format(rataNilai),
						Common.numberFormat.get().format(rataIP), String.valueOf(totalSks) },
				new String[] { "baris nilai mata kuliah berhasil diproses", "skala 0 - 100", "skala 0.00 - 4.00",
						"dari seluruh data pada rekap ini" },
				null, null, new String[] { "#2563eb", "#16a34a", "#0891b2", "#7c3aed" }));

		if (!jumlahPerHuruf.isEmpty()) {
			String[] labelHuruf = jumlahPerHuruf.keySet().toArray(new String[jumlahPerHuruf.size()]);
			double[] nilaiHurufChart = new double[labelHuruf.length];
			for (int i = 0; i < labelHuruf.length; i++) {
				nilaiHurufChart[i] = jumlahPerHuruf.get(labelHuruf[i])[0];
			}
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Huruf Mutu",
					"Menunjukkan berapa banyak nilai mata kuliah yang mendapat masing-masing huruf mutu "
							+ "(A, B, C, D, E, dan sejenisnya) dari seluruh data yang direkap, sehingga "
							+ "proporsinya mudah dibandingkan.",
					labelHuruf, nilaiHurufChart, null, jumlahValid + " nilai"));
		}

		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Nilai Angka Mata Kuliah",
				"Menunjukkan berapa banyak data nilai berada pada tiap rentang angka, dari yang paling "
						+ "rendah sampai paling tinggi, sehingga sebaran capaian nilai mata kuliah terlihat "
						+ "sekilas.",
				new String[] { "< 60", "60 - 69", "70 - 79", "80 - 89", "90 - 100" },
				new double[] { sebaranNilai[0], sebaranNilai[1], sebaranNilai[2], sebaranNilai[3], sebaranNilai[4] },
				"#2563eb"));

		if (nilaiPerSemester.size() > 1) {
			String[] kategoriSmt = new String[nilaiPerSemester.size()];
			double[] seriNilai = new double[nilaiPerSemester.size()];
			int i = 0;
			for (Map.Entry<Integer, double[]> entri : nilaiPerSemester.entrySet()) {
				kategoriSmt[i] = "Smt " + entri.getKey();
				double[] agg = entri.getValue();
				seriNilai[i] = agg[1] == 0 ? 0 : agg[0] / agg[1];
				i++;
			}
			html.append(ais.ui.util.HtmlChartHelper.lineMulti("Tren Rata-rata Nilai per Semester",
					"Menunjukkan naik-turunnya rata-rata nilai mata kuliah dari semester ke semester, "
							+ "sehingga perkembangan prestasi akademik terlihat jelas.",
					kategoriSmt, new String[] { "Rata-rata Nilai" }, new double[][] { seriNilai },
					new String[] { "#16a34a" }));
		}

		if (nilaiPerMatakuliah.size() >= 3 && nilaiPerMatakuliah.size() <= 10) {
			String[] axesMatkul = nilaiPerMatakuliah.keySet().toArray(new String[nilaiPerMatakuliah.size()]);
			double[] nilaiMatkulChart = new double[axesMatkul.length];
			for (int i = 0; i < axesMatkul.length; i++) {
				double[] agg = nilaiPerMatakuliah.get(axesMatkul[i]);
				nilaiMatkulChart[i] = agg[1] == 0 ? 0 : agg[0] / agg[1];
			}
			html.append(ais.ui.util.HtmlChartHelper.radar("Perbandingan Rata-rata Nilai Antar Mata Kuliah",
					"Membandingkan rata-rata nilai antar mata kuliah sekaligus dalam satu tampilan, sehingga "
							+ "mata kuliah dengan capaian tertinggi atau terendah mudah terlihat.",
					axesMatkul, new String[] { "Rata-rata Nilai" }, new double[][] { nilaiMatkulChart },
					new String[] { "#7c3aed" }, 100.0));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
