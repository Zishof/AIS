package ais.action.master.helper.profile;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileSiswa — Panel Profil Pengguna dengan Role Siswa</h3>
 *
 * <p><b>Untuk apa:</b> Menampilkan halaman profil lengkap seorang siswa sekolah, mencakup
 * kartu identitas (foto, nama, NISN/NIM, HP, email), ringkasan utama (kelas, tahun masuk,
 * umur, urutan anak), biodata diri, data orang tua/wali, dan jadwal pelajaran interaktif
 * yang dapat difilter per tahun ajaran dan semester. Pada mode mobile, panel kehadiran
 * juga ditampilkan jika tersedia.</p>
 *
 * <p><b>Cara kerja:</b> Instansiasi dengan objek {@code Siswa}, lalu panggil
 * {@link #init(Component)} dari ZK page controller. Metode ini membangun Grid ZK dua
 * kolom (80 px + sisa) dan mengisi baris secara berurutan:
 * <ol>
 *   <li>Panel info judul (jika parent bukan {@code LayoutRegion}).</li>
 *   <li>Kartu identitas via {@link ProfileUiHelper#mulaiKartuIdentitas}.</li>
 *   <li>Panel kehadiran mobile jika {@link ais.common.Common#isMobile()} true.</li>
 *   <li>Ringkasan &amp; biodata &amp; data keluarga via
 *       {@link #appendRingkasanDanBiodata(Rows)}.</li>
 *   <li>Jadwal pelajaran: filter combobox tahun ajaran + semester +
 *       baris grafik direfresh via {@code DashboardStatistikJadwalMengajar.initChart}.</li>
 *   <li>Tiga baris Space sebagai padding bawah.</li>
 * </ol>
 * </p>
 *
 * <p><b>Sesi Hibernate:</b> Tidak membuka sesi sendiri. Bergantung pada koleksi
 * {@code Siswa} yang sudah ter-load. {@code DashboardStatistikJadwalMengajar} mengelola
 * sesinya sendiri.</p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread. Listener combobox berjalan
 * di event thread yang sama.</p>
 *
 * <p><b>Pemeliharaan:</b> Kompatibel Java 1.7 dan ZKoss 5.5. Untuk menambah panel baru
 * (misal prestasi siswa), tambahkan baris setelah {@code appendRingkasanDanBiodata}.
 * Styling mengacu pada {@link ProfileUiHelper} CSS class ({@code ais-profile-*}).</p>
 */
public class ProfileSiswa {
	private DashboardStatistikJadwalMengajar dashboardStatistikJadwalMengajar = new DashboardStatistikJadwalMengajar();

	private Siswa siswa;

	/**
	 * Membuat instansi ProfileSiswa untuk siswa yang diberikan.
	 *
	 * <p><b>Cara kerja:</b> Menyimpan referensi ke objek {@code Siswa}. Data siswa harus
	 * sudah ter-load dalam sesi Hibernate aktif sebelum memanggil {@link #init(Component)}
	 * karena tidak ada pengambilan ulang dari database di dalam kelas ini.</p>
	 *
	 * @param siswa entitas Siswa yang profilnya akan ditampilkan; tidak boleh {@code null}
	 *              saat {@link #init(Component)} dipanggil
	 * @throws Exception propagasi dari konstruktor super (tidak digunakan secara praktis)
	 */
	public ProfileSiswa(Siswa siswa) throws Exception {
		super();
		this.siswa = siswa;
	}

	/**
	 * Membangun seluruh panel profil siswa ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Titik masuk utama untuk merender halaman profil siswa. Dipanggil
	 * oleh controller ZK setelah instansi {@code ProfileSiswa} dibuat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link ProfileUiHelper#prepareContentParent(Component)} untuk
	 *       membersihkan isi parent dan menyuntikkan CSS profil.</li>
	 *   <li>Membuat Grid dua kolom dan mengisi identitas siswa (foto, nama, NISN/NIM,
	 *       HP, email).</li>
	 *   <li>Pada mode mobile, menambahkan panel kehadiran HTML jika tidak kosong.</li>
	 *   <li>Memanggil {@link #appendRingkasanDanBiodata(Rows)} untuk panel ringkasan,
	 *       biodata, dan keluarga.</li>
	 *   <li>Jadwal pelajaran: baris filter combobox tahun ajaran (5 char) dan semester
	 *       (2 char, readonly) serta baris grafik {@code rowJadwal} yang direfresh oleh
	 *       listener {@code onChange} dari kedua combobox.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Pengambilan foto dibungkus try-catch sehingga foto
	 * tidak ditemukan tidak menghentikan rendering halaman.</p>
	 *
	 * @param parent komponen ZK tujuan; jika {@code null} metode langsung return
	 * @throws Exception bila terjadi error ZK dalam operasi penyusunan komponen
	 */
	@SuppressWarnings({ "deprecation" })
	public void init(final Component parent) throws Exception {

		if (parent == null) {
			return;
		}
		String waktu = ProfileUiHelper.waktuSapaan();

		Component contentParent = ProfileUiHelper.prepareContentParent(parent);

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

			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Profil Siswa", "Data diri dan jadwal pelajaranmu ada di halaman ini.");
		}

		/* Kartu identitas satu baris: foto + sapaan + nama + kontak */
		Component foto = null;
		try {
			foto = CommonMedia.tampilkanGambarKecil(siswa);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileSiswa.java:157");
		}
		org.zkoss.zul.Div infoKartu = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, foto, "Hai, Selamat " + waktu);
		infoKartu.appendChild(new MyLabelBoldAja(siswa.getNama()));
		infoKartu.appendChild(new MyLabelBoldAja(siswa.getNim()));
		siswa.tampilkanHp(infoKartu);
		siswa.tampilkanEmail(infoKartu);

		// Tombol BAYAR (Wizard Pembayaran Siswa) — 1 klik untuk membayar tagihan sekolah
		// langkah demi langkah via WizardPembayaranSiswaHelper (wizard mandiri 5 langkah,
		// ramah mobile — tidak lagi menyematkan engine PembayaranOnline).
		// Gerbang konfig sama dengan profil mahasiswa.
		if (siswa != null && siswa.getId() != null
				&& Common.getKonfigurasi("tampilkan_tombol_bayar_wizard_di_profile",
						ais.database.model.Konfigurasi.AKTIF).getNilai()
						.equals(ais.database.model.Konfigurasi.AKTIF)) {
			Row rowBayar = new MyRowStyled();
			rowBayar.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowBayar, "2");
			Hbox hboxBayar = new Hbox();
			hboxBayar.setSpacing("10px");
			hboxBayar.setStyle("padding:10px 5px;");
			hboxBayar.setSclass("ais-btn-group");
			hboxBayar.setParent(rowBayar);
			ais.ui.util.MyToolbarbuttonConfig btnBayar = new ais.ui.util.MyToolbarbuttonConfig("Bayar",
					"/img/Finance-Invoice-icon.png");
			btnBayar.setStyle("font-weight:800;border:0;border-radius:8px;padding:6px 16px;cursor:pointer;color:#ffffff;"
					+ "background:linear-gradient(135deg,#0ea5e9,#0369a1);box-shadow:0 4px 12px rgba(14,165,233,.3);");
			btnBayar.setTooltiptext("Buka Wizard Pembayaran — bayar tagihan sekolah langkah demi langkah");
			btnBayar.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ais.action.master.sekolah.helper.WizardPembayaranSiswaHelper.buka(siswa, null);
				}
			});
			btnBayar.setParent(hboxBayar);
		}

		Row row;

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

		appendRingkasanDanBiodata(rows);

		rows.appendChild(new ais.ui.util.MyGroupConfig("Jadwal pelajaran"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jadwal Pelajaran", "Daftar pelajaran yang kamu ikuti. Pilih tahun ajaran dan semester untuk melihat jadwal lainnya.");

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
				dashboardStatistikJadwalMengajar.initChart(rowJadwal, false, tahunAjaranJadwal, siswa, null,
						ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
			}
		});

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester:")));
		hbox.appendChild(searchJenisSemester);
		searchJenisSemester.setCols(2);
		searchJenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dashboardStatistikJadwalMengajar.initChart(rowJadwal, false, tahunAjaranJadwal, siswa, null,
						ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
			}
		});

		rowJadwal.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowJadwal, "2");
		dashboardStatistikJadwalMengajar.initChart(rowJadwal, false, tahunAjaranJadwal, siswa, null,
				ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));

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
	 * Menambahkan kartu ringkasan, biodata diri, dan data orang tua/wali ke dalam
	 * baris grid profil siswa.
	 *
	 * <p><b>Tujuan:</b> Menyajikan data siswa dalam tiga panel HTML yang saling
	 * melengkapi: (1) kartu ringkasan empat angka (Kelas, Tahun Masuk, Umur, Anak ke);
	 * (2) tabel biodata diri lengkap (nomor induk, NISN, tempat-tanggal lahir,
	 * jenis kelamin, agama, golongan darah, hobi, alamat, sekolah asal, dll.);
	 * (3) tabel data orang tua &amp; wali (ayah, ibu, wali).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghitung string "anak ke": {@code siswa.getAnakKe()} + " dari " +
	 *       {@code siswa.getDariAnakKe()} bila keduanya tersedia.</li>
	 *   <li>Membangun kartu ringkasan via {@link ProfileUiHelper#statsWrap(String[])} dan
	 *       {@link ProfileUiHelper#stat(String, Object, String)}.</li>
	 *   <li>Membangun tabel biodata dan keluarga via
	 *       {@link ProfileUiHelper#infoTable(String, String, String[][])} dua kali, lalu
	 *       digabung berdampingan via {@link ProfileUiHelper#cols(String, String)}.</li>
	 *   <li>Setiap hasil HTML ditaruh di {@code MyRowStyled} dengan span=2.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Dibungkus try-catch; error ditampilkan via
	 * {@link ais.common.Common#tampilErrorJikaAdmin(Exception)} sehingga panel lain
	 * tidak terpengaruh.</p>
	 *
	 * @param rows kontainer baris grid ZK tujuan; tidak boleh {@code null}
	 */
	private void appendRingkasanDanBiodata(Rows rows) {
		try {
			final Long uid = (siswa == null ? null : siswa.getId());

			// Panel ringkasan + biodata/keluarga di-cache per-siswa (TTL 20 mnt). Pembentukan HTML
			// memicu banyak getter relasi (kelas/agama/pekerjaan ortu) yg masing-masing query lazy;
			// caching string-nya menghilangkan query berulang setiap profil dibuka. Lihat ProfileCacheUtil.
			String ringkasanHtml = ProfileCacheUtil.htmlPerUser("SiswaRingkasan", "SISWA", uid,
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

			String biodataHtml = ProfileCacheUtil.htmlPerUser("SiswaBiodata", "SISWA", uid,
					new ProfileCacheUtil.Pembuat() {
						@Override
						public String buat() throws Exception {
							return bangunBiodataKeluargaHtml();
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

	/** Bangun HTML panel "Ringkasan" (Kelas/Tahun Masuk/Umur/Anak ke). Sumber data utk cache per-siswa. */
	private String bangunRingkasanHtml() {
		String kelas = ProfileUiHelper.nama(siswa.getKelas());
		String anakKe = siswa.getAnakKe() == null ? ""
				: siswa.getAnakKe() + (siswa.getDariAnakKe() == null ? "" : " dari " + siswa.getDariAnakKe());

		String ringkasan = ProfileUiHelper.statsWrap(new String[] {
				ProfileUiHelper.stat("Kelas", ProfileUiHelper.notBlank(kelas) ? kelas : "-", "Kelas yang sedang diikuti"),
				ProfileUiHelper.stat("Tahun Masuk", siswa.getTahunMasuk() == null ? "-" : siswa.getTahunMasuk(),
						"Mulai bersekolah di sini"),
				ProfileUiHelper.stat("Umur",
						ProfileUiHelper.notBlank(ProfileUiHelper.umur(siswa.getTanggalLahir()))
								? ProfileUiHelper.umur(siswa.getTanggalLahir()) : "-",
						"Dihitung dari tanggal lahir"),
				ProfileUiHelper.stat("Anak ke", ProfileUiHelper.notBlank(anakKe) ? anakKe : "-",
						"Urutan dalam keluarga") });

		return ProfileUiHelper.panel("Ringkasan", "Info penting tentang kamu secara sekilas.", ringkasan);
	}

	/** Bangun HTML gabungan "Data Diri" + "Orang Tua & Wali". Sumber data utk cache per-siswa. */
	private String bangunBiodataKeluargaHtml() {
		String biodata = ProfileUiHelper.infoTable("Data Diri", "Data pribadimu yang tercatat di sekolah.",
				new String[][] {
						ProfileUiHelper.pasangan("Nomor Induk", siswa.getNomorInduk()),
						ProfileUiHelper.pasangan("NISN / NIM", siswa.getNim()),
						ProfileUiHelper.pasangan("Nama Panggilan", siswa.getPanggilan()),
						ProfileUiHelper.pasangan("Tempat, Tanggal Lahir",
								ProfileUiHelper.ttl(siswa.getTempatLahir(), siswa.getTanggalLahir())),
						ProfileUiHelper.pasangan("Jenis Kelamin", siswa.getJenisKelamin()),
						ProfileUiHelper.pasangan("Agama", ProfileUiHelper.nama(siswa.getAgama())),
						ProfileUiHelper.pasangan("Golongan Darah", siswa.getGolonganDarah()),
						ProfileUiHelper.pasangan("Kewarganegaraan", siswa.getKewarganegaraan()),
						ProfileUiHelper.pasangan("Bahasa di Rumah", siswa.getBahasa()),
						ProfileUiHelper.pasangan("Hobi", siswa.getHobby()),
						ProfileUiHelper.pasangan("Alamat", siswa.getAlamatSiswa()),
						ProfileUiHelper.pasangan("Telepon", siswa.getTeleponSiswa()),
						ProfileUiHelper.pasangan("Email", siswa.getAlamatEmail()),
						ProfileUiHelper.pasangan("Sekolah Asal", siswa.getSekolahAsal()),
						ProfileUiHelper.pasangan("Diterima di Kelas", siswa.getDiterimaDiSekolahIniDiKelas()),
						ProfileUiHelper.pasangan("Status dalam Keluarga", siswa.getStatusDalamKeluarga()) });

		String keluarga = ProfileUiHelper.infoTable("Orang Tua & Wali",
				"Kontak keluarga yang bisa dihubungi sekolah.",
				new String[][] {
						ProfileUiHelper.pasangan("Nama Ayah", siswa.getNamaAyah()),
						ProfileUiHelper.pasangan("Pekerjaan Ayah", ProfileUiHelper.nama(siswa.getPekerjaanAyah())),
						ProfileUiHelper.pasangan("HP Ayah", siswa.getHp1ayah()),
						ProfileUiHelper.pasangan("Nama Ibu", siswa.getNamaIbu()),
						ProfileUiHelper.pasangan("Pekerjaan Ibu", ProfileUiHelper.nama(siswa.getPekerjaanIbu())),
						ProfileUiHelper.pasangan("HP Ibu", siswa.getHp1ibu()),
						ProfileUiHelper.pasangan("Telepon Orang Tua", siswa.getTeleponOrangTua()),
						ProfileUiHelper.pasangan("Alamat Orang Tua", siswa.getAlamatOrangTua()),
						ProfileUiHelper.pasangan("Nama Wali", siswa.getNamaWali()),
						ProfileUiHelper.pasangan("Pekerjaan Wali", ProfileUiHelper.nama(siswa.getPekerjaanWali())),
						ProfileUiHelper.pasangan("Telepon Wali", siswa.getTeleponWali()),
						ProfileUiHelper.pasangan("Alamat Wali", siswa.getAlamatWali()) });

		return ProfileUiHelper.cols(biodata, keluarga);
	}

}
