package ais.action.master.helper.profile;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vbox;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.dashboard.sekolah.DashboardStatistikJadwalMengajar;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileGuru — Panel Profil Pengguna dengan Role Guru</h3>
 *
 * <p><b>Untuk apa:</b> Menampilkan halaman profil lengkap seorang guru sekolah, mencakup
 * kartu identitas (foto, nama, NIP, HP, email), ringkasan kepegawaian (jenis guru, status,
 * golongan), biodata diri, data kepegawaian formal, dan jadwal pelajaran interaktif yang
 * dapat difilter per tahun ajaran dan semester. Pada mode mobile, panel kehadiran guru
 * ({@link ais.action.master.PengumumanAkademisAction#tampilkanKehadiranGuru}) juga dimuat.</p>
 *
 * <p><b>Cara kerja:</b> Instansiasi dengan objek {@code Guru}, lalu panggil
 * {@link #init(Component)} dari ZK page controller. Metode ini membangun struktur Grid ZK
 * dua kolom (80 px + sisa) dan mengisi baris-baris dari atas ke bawah:
 * <ol>
 *   <li>Judul panel info (jika parent bukan {@code LayoutRegion}).</li>
 *   <li>Kartu identitas via {@link ProfileUiHelper#mulaiKartuIdentitas}.</li>
 *   <li>Ringkasan &amp; biodata via {@link #appendRingkasanDanBiodata(Rows)}.</li>
 *   <li>Jadwal pelajaran: baris filter (tahun ajaran + semester) +
 *       baris grafik yang di-refresh via {@code DashboardStatistikJadwalMengajar.initChart}
 *       setiap kali combobox berubah.</li>
 *   <li>Kehadiran mobile (kondisional).</li>
 *   <li>Tiga baris Space kosong sebagai padding bawah.</li>
 * </ol>
 * </p>
 *
 * <p><b>Sesi Hibernate:</b> Kelas ini tidak membuka sesi sendiri. Pengambilan data
 * mengandalkan cache koleksi {@code Guru} yang sudah dimuat oleh session manajemen utama
 * ({@code currentSession()}). {@code DashboardStatistikJadwalMengajar} menggunakan
 * sesi yang dibukanya sendiri secara internal.</p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread. Listener onChange di
 * combobox berjalan di event thread yang sama.</p>
 *
 * <p><b>Pemeliharaan:</b> Untuk menambah kartu baru (misal prestasi guru), cukup
 * tambahkan baris setelah {@code appendRingkasanDanBiodata}. Pola styling mengacu pada
 * {@link ProfileUiHelper} CSS class ({@code ais-profile-*}). Kompatibel Java 1.7 dan
 * ZKoss 5.5.</p>
 */
public class ProfileGuru {
	private DashboardStatistikJadwalMengajar dashboardStatistikJadwalMengajar = new DashboardStatistikJadwalMengajar();

	private Guru guru;

	/**
	 * Membuat instansi ProfileGuru untuk guru yang diberikan.
	 *
	 * <p><b>Cara kerja:</b> Menyimpan referensi ke objek {@code Guru}. Data guru harus
	 * sudah ter-load dalam sesi Hibernate aktif sebelum memanggil {@link #init(Component)}
	 * karena tidak ada pengambilan ulang dari database di dalam kelas ini.</p>
	 *
	 * @param guru entitas Guru yang profilnya akan ditampilkan; tidak boleh {@code null}
	 *             saat {@link #init(Component)} dipanggil
	 * @throws Exception propagasi dari konstruktor super (tidak digunakan secara praktis)
	 */
	public ProfileGuru(Guru guru) throws Exception {
		super();
		this.guru = guru;
	}

	/**
	 * Membangun seluruh panel profil guru ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Titik masuk utama untuk merender halaman profil guru. Dipanggil
	 * oleh controller ZK (biasanya {@code ProfileAction} atau sejenisnya) setelah instansi
	 * {@code ProfileGuru} dibuat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link ProfileUiHelper#prepareContentParent(Component)} untuk
	 *       membersihkan parent dan menyiapkan CSS profil (termasuk menyuntikkan blok
	 *       {@code &lt;style&gt;} ke dalam komponen).</li>
	 *   <li>Membuat Grid ZK dua kolom dan mengisi baris identitas, ringkasan, biodata,
	 *       serta jadwal pelajaran.</li>
	 *   <li>Jadwal pelajaran dirender via {@code DashboardStatistikJadwalMengajar.initChart}
	 *       dengan parameter guru saat ini. Listener {@code onChange} di combobox tahun
	 *       ajaran dan semester memanggil ulang {@code initChart} ke baris yang sama
	 *       (rowJadwal), sehingga grafik diperbarui tanpa reload halaman penuh.</li>
	 *   <li>Pada mode mobile ({@link ais.common.Common#isMobile()}), panel kehadiran
	 *       HTML dari {@code PengumumanAkademisAction.tampilkanKehadiranGuru} juga
	 *       ditambahkan jika hasilnya tidak kosong.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Pengambilan foto ({@code CommonMedia.tampilkanGambarKecil})
	 * di-wrap dengan try-catch agar foto yang tidak ditemukan tidak menghentikan
	 * rendering halaman.</p>
	 *
	 * @param parent komponen ZK tujuan (misalnya {@code Center} dari {@code Borderlayout}
	 *               atau {@code Div} portal); jika {@code null} metode langsung return
	 * @throws Exception bila terjadi error ZK yang tidak tertangkap dalam operasi
	 *                   penyusunan komponen
	 */
	@SuppressWarnings({ "deprecation" })
	public void init(final Component parent) throws Exception {

		if (parent == null) {
			return;
		}
		String waktu = ProfileUiHelper.waktuSapaan();

		Component contentParent = ProfileUiHelper.prepareContentParent(parent);

		String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(
				(javax.servlet.http.HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest(),
				"banner_perguruanTinggi_");
		if (background_PerguruanTinggi == null || background_PerguruanTinggi.trim().isEmpty()) {
			background_PerguruanTinggi = ais.common.Common.getRequestHostWithProtocol() + "/img/header.jpg";
		}

		Grid grid = new Grid();grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(contentParent);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("80px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (parent instanceof LayoutRegion) {
			((LayoutRegion) parent).setTitle("Hai, Selamat " + waktu);
			((LayoutRegion) parent).setCollapsible(true);
			((LayoutRegion) parent).setSplittable(true);
		} else {

			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Profil Guru", "Data diri, status kepegawaian, dan jadwal mengajar Anda ada di halaman ini.");
		}

		/* Kartu identitas satu baris: foto + sapaan + nama + kontak */
		Component foto = null;
		try {
			foto = CommonMedia.tampilkanGambarKecil(guru);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileGuru.java:173");
		}
		org.zkoss.zul.Div infoKartu = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, foto, "Hai, Selamat " + waktu);
		infoKartu.appendChild(new MyLabelBoldAja(guru.getNama()));
		infoKartu.appendChild(new MyLabelBoldAja(guru.getNim()));
		guru.tampilkanHp(infoKartu);
		guru.tampilkanEmail(infoKartu);

		Row row;

		

		appendRingkasanDanBiodata(rows);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		rows.appendChild(new ais.ui.util.MyGroupConfig("Jadwal pelajaran"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jadwal Pelajaran", "Jadwal mengajar Anda. Pilih tahun ajaran dan semester untuk melihat jadwal lainnya.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Row rowJadwal = new MyRowStyled();

		final Combobox searchJenisSemester = new Combobox();
		Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2);
		searchJenisSemester.setReadonly(true);

		final Combobox tahunAjaranJadwal = Common.generateTahunAjaran(null);
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Ajaran:")));
		hbox.appendChild(tahunAjaranJadwal);
		tahunAjaranJadwal.setCols(5);
		tahunAjaranJadwal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dashboardStatistikJadwalMengajar.initChart(rowJadwal, false, tahunAjaranJadwal, null, guru,
						ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
			}
		});

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester:")));
		hbox.appendChild(searchJenisSemester);
		searchJenisSemester.setCols(2);
		searchJenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dashboardStatistikJadwalMengajar.initChart(rowJadwal, false, tahunAjaranJadwal, null, guru,
						ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
			}
		});

		rowJadwal.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowJadwal, "2");
		dashboardStatistikJadwalMengajar.initChart(rowJadwal, false, tahunAjaranJadwal, null, guru,
				ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
		
		
		
		
		boolean mobile = Common.isMobile();
		if (mobile && !PengumumanAkademisAction.isKehadiranHomeDitampilkan()) {
			Tbmuser tbmuser = Common.getCurrentUser();
			String pengumuman = PengumumanAkademisAction.tampilkanKehadiranGuru(tbmuser, mobile);
			if (!pengumuman.isEmpty()) {
				row = new MyRowStyled();
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(new Html(pengumuman));
			}
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
	}

	/**
	 * Menambahkan kartu ringkasan kepegawaian, biodata diri, dan data kepegawaian formal
	 * ke dalam baris grid profil.
	 *
	 * <p><b>Tujuan:</b> Menyajikan data penting guru dalam tiga panel HTML yang saling
	 * melengkapi: (1) kartu ringkasan berisi empat angka utama (NIP, jenis guru, status
	 * kepegawaian, golongan); (2) tabel biodata pribadi; (3) tabel data kepegawaian
	 * formal (SK, TMT, lembaga pengangkat).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Merakit nama lengkap dengan gelar depan/belakang dari {@code Guru.getGelarDepan()},
	 *       {@code getNamaGuru()}, dan {@code getGelarBelakang()}.</li>
	 *   <li>Memanggil {@link ProfileUiHelper#statsWrap(String[])} dan
	 *       {@link ProfileUiHelper#stat(String, Object, String)} untuk membangun kartu
	 *       ringkasan berisi empat kartu angka dengan keterangan singkat.</li>
	 *   <li>Memanggil {@link ProfileUiHelper#infoTable(String, String, String[][])} dua
	 *       kali—satu untuk biodata dan satu untuk kepegawaian—lalu
	 *       {@link ProfileUiHelper#cols(String, String)} untuk menempatkan keduanya
	 *       secara berdampingan (dua kolom responsif).</li>
	 *   <li>Setiap hasil HTML dibungkus dalam {@code Html} ZK dan ditaruh di
	 *       {@code MyRowStyled} dengan span=2.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Seluruh logika dibungkus try-catch; error ditampilkan
	 * via {@link ais.common.Common#tampilErrorJikaAdmin(Exception)} sehingga tidak
	 * menghentikan rendering bagian lain dari profil.</p>
	 *
	 * @param rows kontainer baris grid ZK tujuan; tidak boleh {@code null}
	 */
	private void appendRingkasanDanBiodata(Rows rows) {
		try {
			final Long uid = (guru == null ? null : guru.getId());

			// Panel ringkasan + biodata/kepegawaian di-cache per-guru (TTL 20 mnt) untuk menghindari
			// query lazy-load relasi (jenis guru/status/golongan/agama/dll.) tiap profil dibuka.
			String ringkasanHtml = ProfileCacheUtil.htmlPerUser("GuruRingkasan", "GURU", uid,
					new ProfileCacheUtil.Pembuat() {
						@Override
						public String buat() throws Exception {
							return bangunRingkasanHtml();
						}
					});
			Row row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new Html(ringkasanHtml));

			String biodataHtml = ProfileCacheUtil.htmlPerUser("GuruBiodata", "GURU", uid,
					new ProfileCacheUtil.Pembuat() {
						@Override
						public String buat() throws Exception {
							return bangunBiodataKepegawaianHtml();
						}
					});
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new Html(biodataHtml));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Bangun HTML panel "Ringkasan" guru. Sumber data utk cache per-guru. */
	private String bangunRingkasanHtml() {
		String ringkasan = ProfileUiHelper.statsWrap(new String[] {
				ProfileUiHelper.stat("NIP", ProfileUiHelper.notBlank(guru.getNip()) ? guru.getNip() : "-",
						"Nomor induk pegawai"),
				ProfileUiHelper.stat("Jenis Guru",
						ProfileUiHelper.notBlank(ProfileUiHelper.nama(guru.getJenisGuru()))
								? ProfileUiHelper.nama(guru.getJenisGuru()) : "-",
						"Tugas utama mengajar"),
				ProfileUiHelper.stat("Status Kepegawaian",
						ProfileUiHelper.notBlank(ProfileUiHelper.nama(guru.getStatusKepegawaian()))
								? ProfileUiHelper.nama(guru.getStatusKepegawaian()) : "-",
						"Tetap / honor / kontrak"),
				ProfileUiHelper.stat("Golongan",
						ProfileUiHelper.notBlank(ProfileUiHelper.nama(guru.getGolonganPegawai()))
								? ProfileUiHelper.nama(guru.getGolonganPegawai()) : "-",
						"Golongan kepangkatan") });

		return ProfileUiHelper.panel("Ringkasan", "Status Anda sebagai pengajar secara sekilas.", ringkasan);
	}

	/** Bangun HTML gabungan "Data Diri" + "Kepegawaian" guru. Sumber data utk cache per-guru. */
	private String bangunBiodataKepegawaianHtml() {
		String namaLengkap = ((guru.getGelarDepan() == null ? "" : guru.getGelarDepan() + " ")
				+ ProfileUiHelper.text(guru.getNamaGuru())
				+ (guru.getGelarBelakang() == null ? "" : ", " + guru.getGelarBelakang())).trim();

		String biodata = ProfileUiHelper.infoTable("Data Diri", "Data pribadi Anda yang tercatat.",
				new String[][] {
						ProfileUiHelper.pasangan("Nama Lengkap", namaLengkap),
						ProfileUiHelper.pasangan("Nama Panggilan", guru.getPanggilan()),
						ProfileUiHelper.pasangan("Tempat, Tanggal Lahir",
								ProfileUiHelper.ttl(guru.getTempatLahir(), guru.getTanggalLahir())),
						ProfileUiHelper.pasangan("Jenis Kelamin", guru.getJenisKelamin()),
						ProfileUiHelper.pasangan("Agama", ProfileUiHelper.nama(guru.getAgama())),
						ProfileUiHelper.pasangan("Status Pernikahan", guru.getStatusNikah()),
						ProfileUiHelper.pasangan("Kewarganegaraan", guru.getKewarganegaraan()),
						ProfileUiHelper.pasangan("Alamat", guru.getAlamatGuru()),
						ProfileUiHelper.pasangan("Dusun", guru.getDusun()),
						ProfileUiHelper.pasangan("Kelurahan", guru.getKelurahan()),
						ProfileUiHelper.pasangan("Kecamatan", ProfileUiHelper.nama(guru.getKecamatan())),
						ProfileUiHelper.pasangan("Kode Pos", guru.getKodePos()),
						ProfileUiHelper.pasangan("HP", guru.getHp()),
						ProfileUiHelper.pasangan("Telepon", guru.getTeleponGuru()),
						ProfileUiHelper.pasangan("Email", guru.getAlamatEmail()) });

		String kepegawaian = ProfileUiHelper.infoTable("Kepegawaian", "Riwayat pengangkatan dan status kerja Anda.",
				new String[][] {
						ProfileUiHelper.pasangan("NIP", guru.getNip()),
						ProfileUiHelper.pasangan("Kode", guru.getKode()),
						ProfileUiHelper.pasangan("Status Pegawai", ProfileUiHelper.nama(guru.getStatusPegawai())),
						ProfileUiHelper.pasangan("Jenis PTK",
								ProfileUiHelper.nama(guru.getJenisPendidikDanTenagaKependidikan())),
						ProfileUiHelper.pasangan("Sumber Gaji", ProfileUiHelper.nama(guru.getSumberGaji())),
						ProfileUiHelper.pasangan("SK CPNS", guru.getSkCpns()),
						ProfileUiHelper.pasangan("Tanggal SK CPNS", ProfileUiHelper.tanggal(guru.getTglSkCpns())),
						ProfileUiHelper.pasangan("SK Pengangkatan", guru.getSkAngkat()),
						ProfileUiHelper.pasangan("TMT Pengangkatan", ProfileUiHelper.tanggal(guru.getTmtSkAngkat())),
						ProfileUiHelper.pasangan("TMT PNS", ProfileUiHelper.tanggal(guru.getTmtPns())),
						ProfileUiHelper.pasangan("Lembaga Pengangkat",
								ProfileUiHelper.nama(guru.getLembagaPengangkat())) });

		return ProfileUiHelper.cols(biodata, kepegawaian);
	}

}
