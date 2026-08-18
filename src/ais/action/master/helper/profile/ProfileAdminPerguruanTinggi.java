package ais.action.master.helper.profile;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Html;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.dashboard.admin.DashboardStatistikCalonMahasiswaMasuk;
import ais.action.master.dashboard.admin.DashboardStatistikDosenJurusan;
import ais.action.master.dashboard.admin.DashboardStatistikMahamahasiswaLulus;
import ais.action.master.dashboard.admin.DashboardStatistikMahamahasiswaMasuk;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileAdminPerguruanTinggi — Panel Profil Admin Perguruan Tinggi</h3>
 *
 * <p><b>Untuk apa:</b> Menyajikan dashboard profil satu halaman bagi pengguna yang
 * berperan sebagai admin atau pimpinan perguruan tinggi. Panel menampilkan kartu identitas
 * admin (termasuk fakultas/jurusan/program), angka penting kampus (mahasiswa, lulusan,
 * dosen, prodi, fakultas) yang dapat diklik untuk melihat rincian, dan berbagai grafik
 * statistik akademik.</p>
 *
 * <p><b>Cara kerja:</b> Tersedia dua titik masuk:
 * <ol>
 *   <li>{@link #init(Component)} — membangun Grid penuh termasuk kartu identitas,
 *       kemudian memanggil {@link #lanjut(Rows, Tbmuser, boolean)}. Jika {@code lain==true}
 *       dan sekolah terpilih di sesi, juga memanggil
 *       {@link ProfileAdminSekolah#lanjut(Rows, Tbmuser, boolean)}.</li>
 *   <li>{@link #lanjut(Rows, Tbmuser, boolean)} — statik, dapat dipanggil oleh profil
 *       lain (mis. {@link ProfileGabunganPengguna}) untuk menyisipkan panel PT ke dalam
 *       grid bersama.</li>
 * </ol>
 * </p>
 *
 * <p><b>Guard aktif di {@code lanjut}:</b> Seluruh panel hanya ditampilkan jika tidak ada
 * sekolah yang terpilih di sesi ({@code SekolahUtil.getSekolah() == null}), mencegah
 * duplikasi konten saat dipakai dari profil gabungan.</p>
 *
 * <p><b>Grafik yang ditampilkan (berurutan):</b>
 * <ul>
 *   <li>Kehadiran dosen (mode mobile) via {@code PengumumanAkademisAction}</li>
 *   <li>Dashboard Akademik per prodi via {@link ProfileAdminPerguruanTinggiAkademikDashboard}</li>
 *   <li>Kartu angka penting clickable via {@link #appendAngkaPenting(Rows)}</li>
 *   <li>Prestasi ringkas (mahasiswa/dosen/pegawai)</li>
 *   <li>Mahasiswa per angkatan, per lulus, cuti ({@link DashboardStatistikMahamahasiswaMasuk} /
 *       {@link DashboardStatistikMahamahasiswaLulus})</li>
 *   <li>Dosen per prodi ({@link DashboardStatistikDosenJurusan})</li>
 *   <li>Calon mahasiswa per prodi ({@link DashboardStatistikCalonMahasiswaMasuk})</li>
 * </ul>
 * </p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread. Dashboard singletons statik
 * hanya memanggil {@code initChart} yang tidak memodifikasi state.</p>
 *
 * <p><b>Pemeliharaan:</b> Java 1.7 dan ZKoss 5.5. {@code @SuppressWarnings("deprecation")}
 * ada pada {@code lanjut} karena API ZK lama di Dashboard*. Penambahan grafik baru cukup
 * menambahkan singleton statik dan row baru di {@code lanjut}.</p>
 */
public class ProfileAdminPerguruanTinggi {

	private static DashboardStatistikMahamahasiswaMasuk dashboardStatistikStatusMahamahasiswa = new DashboardStatistikMahamahasiswaMasuk();
	private static DashboardStatistikMahamahasiswaLulus dashboardStatistikMahamahasiswaLulus = new DashboardStatistikMahamahasiswaLulus();
	private static DashboardStatistikDosenJurusan dashboardStatistikDosenJurusan = new DashboardStatistikDosenJurusan();
	private static DashboardStatistikCalonMahasiswaMasuk dashboardStatistikCalonMahasiswaMasuk = new DashboardStatistikCalonMahasiswaMasuk();

	private Tbmuser tbmuser;
	private boolean lain = true;
	private boolean tampilkanChart = false;

	/**
	 * Membuat panel profil PT dengan mode {@code lain=true} dan {@code tampilkanChart=false}.
	 *
	 * <p>Konstruktor ringkas untuk penggunaan standar; panel PT ditampilkan penuh,
	 * dan jika sekolah juga aktif di sesi maka ringkasan sekolah ikut ditambahkan.</p>
	 *
	 * @param tbmuser pengguna yang sedang login; digunakan untuk kartu identitas dan
	 *                tampilan informasi fakultas/jurusan/program yang terasosiasi
	 */
	public ProfileAdminPerguruanTinggi(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Membuat panel profil PT dengan kontrol penuh atas mode "lain" dan tampilan grafik.
	 *
	 * @param tbmuser       pengguna yang sedang login
	 * @param lain          {@code true} untuk ikut menambahkan ringkasan sekolah jika ada
	 *                      sekolah yang terpilih di sesi
	 * @param tampilkanChart {@code true} untuk merender grafik via Chart.js/ZK;
	 *                       {@code false} untuk mode ringkasan teks saja
	 */
	public ProfileAdminPerguruanTinggi(Tbmuser tbmuser, boolean lain, boolean tampilkanChart) {
		this.tbmuser = tbmuser;
		this.lain = lain;
		this.tampilkanChart = tampilkanChart;
	}

	/**
	 * Membangun panel profil admin perguruan tinggi lengkap ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Titik masuk instance untuk merender halaman profil PT secara
	 * standalone, termasuk kartu identitas admin, panel angka penting, dan grafik statistik.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link ProfileUiHelper#prepareContentParent(Component)} untuk
	 *       membersihkan parent dan menyuntikkan CSS.</li>
	 *   <li>Membangun Grid 2-kolom (80px | auto) di dalam parent.</li>
	 *   <li>Jika parent adalah {@code LayoutRegion}, set judul dan collapse/split;
	 *       jika bukan, tambahkan baris info judul.</li>
	 *   <li>Memuat foto profil dan merender kartu identitas admin termasuk nama,
	 *       fakultas, jurusan, program studi, HP, dan email.</li>
	 *   <li>Memanggil {@link #lanjut(Rows, Tbmuser, boolean)} untuk grafik PT.</li>
	 *   <li>Jika {@code lain==true} dan ada sekolah terpilih di sesi, memanggil
	 *       {@link ProfileAdminSekolah#lanjut(Rows, Tbmuser, boolean)}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param parent komponen ZK tujuan; jika {@code null} metode langsung return
	 * @throws Exception bila terjadi error dalam penyusunan komponen ZK
	 */
	public void init(final Component parent) throws Exception {
		if (parent == null) {
			return;
		}
		String waktu = ProfileUiHelper.waktuSapaan();

		Component contentParent = ProfileUiHelper.prepareContentParent(parent);


		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
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

			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Profil Admin Perguruan Tinggi", "Ringkasan kondisi kampus: mahasiswa, dosen, dan pendaftar baru.");
		}

		if (tbmuser != null) {
			/* Kartu identitas satu baris: foto + sapaan + nama + kontak */
			Component foto = null;
			try {
				foto = CommonMedia.tampilkanGambarKecil(tbmuser);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggi.java:181");
			}
			org.zkoss.zul.Div vbox = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, foto, "Hai, Selamat " + waktu);

			Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
			Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
			Program program = tbmuser == null ? null : tbmuser.ambilProgram();

			vbox.appendChild(new MyLabelBoldAja(tbmuser.getUserNama()));
			vbox.appendChild(new MyLabelBoldAja(fakultas == null ? "" : fakultas.getNama()));
			vbox.appendChild(new MyLabelBoldAja(jurusan == null ? "" : jurusan.getNama()));
			vbox.appendChild(new MyLabelBoldAja(program == null ? "" : program.getNama()));
			tbmuser.tampilkanHp(vbox);
			tbmuser.tampilkanEmail(vbox);
		}

		lanjut(rows, tbmuser, tampilkanChart);
		if (lain) {

			Sekolah sekolah = SekolahUtil.getSekolah();
			if (sekolah != null && sekolah.getId() != null) {
				ProfileAdminSekolah.lanjut(rows, tbmuser, tampilkanChart);
			}

		}
	}

	/**
	 * Merender kartu "Angka Penting" perguruan tinggi ke dalam baris grid.
	 *
	 * <p><b>Tujuan:</b> Menyajikan ringkasan kuantitatif utama kampus dalam bentuk kartu
	 * statistik visual yang dapat diklik: jumlah mahasiswa (total + aktif/lulus via donut)
	 * dan jumlah dosen, prodi, serta fakultas. Setiap kartu membuka popup rincian saat diklik.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghitung total mahasiswa, mahasiswa lulus (tanggalLulus isNotNull), dosen,
	 *       prodi (Jurusan), dan fakultas via {@link ProfileUiHelper#hitung(Class, Criterion[])}.</li>
	 *   <li>Menghitung {@code aktif = mahasiswa - lulus}; jika negatif → 0.</li>
	 *   <li>Membuat ID modal unik via {@link ProfileUiHelper#nextModalId(String)} untuk setiap
	 *       kartu, agar popup tidak bertabrakan ID antar-render.</li>
	 *   <li>Menyusun HTML kartu via {@link ProfileUiHelper#statsWrap(String[])} dan
	 *       {@link ProfileUiHelper#statClickable(String, Object, String, String)};
	 *       donut lulus/aktif menggunakan {@link ProfileUiHelper#donutClickable}.</li>
	 *   <li>Membangun konten popup via {@link ProfileUiHelper#modal(String, String, String)} +
	 *       {@link ProfileUiHelper#modalAngka(String, Object, String, String)} dan
	 *       {@link ProfileUiHelper#tabelRincian(String[])} untuk kartu dengan data porsi.</li>
	 *   <li>Menambahkan baris kolspan-2 ke {@code rows} berisi Html ZK yang menggabungkan
	 *       kartu dan semua popup (hidden by default, ditampilkan via onclick JS).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Seluruh method dibungkus try-catch; error hanya
	 * ditampilkan ke admin via {@code Common.tampilErrorJikaAdmin(e)}.</p>
	 *
	 * @param rows baris ZK tujuan; tidak boleh {@code null}
	 */
	private static void appendAngkaPenting(Rows rows) {
		try {
			long mahasiswa = ProfileUiHelper.hitung(ais.database.model.Mahasiswa.class, null);
			long lulus = ProfileUiHelper.hitung(ais.database.model.Mahasiswa.class,
					new org.hibernate.criterion.Criterion[] {
							org.hibernate.criterion.Restrictions.isNotNull("tanggalLulus") });
			long dosen = ProfileUiHelper.hitung(ais.database.model.Dosen.class, null);
			long prodi = ProfileUiHelper.hitung(Jurusan.class, null);
			long fakultas = ProfileUiHelper.hitung(Fakultas.class, null);
			long aktif = mahasiswa - lulus;
			if (aktif < 0) {
				aktif = 0;
			}

			String idMhs = ProfileUiHelper.nextModalId("aisAngka");
			String idDosen = ProfileUiHelper.nextModalId("aisAngka");
			String idProdi = ProfileUiHelper.nextModalId("aisAngka");
			String idFak = ProfileUiHelper.nextModalId("aisAngka");
			String idLulus = ProfileUiHelper.nextModalId("aisAngka");
			String idAktif = ProfileUiHelper.nextModalId("aisAngka");

			String isi = ProfileUiHelper.statsWrap(new String[] {
					ProfileUiHelper.statClickable("Mahasiswa", ProfileUiHelper.fmt(Long.valueOf(mahasiswa)),
							"Semua mahasiswa yang tercatat", idMhs),
					ProfileUiHelper.statClickable("Dosen", ProfileUiHelper.fmt(Long.valueOf(dosen)),
							"Semua dosen yang tercatat", idDosen),
					ProfileUiHelper.statClickable("Prodi", ProfileUiHelper.fmt(Long.valueOf(prodi)),
							"Program studi yang dibuka", idProdi),
					ProfileUiHelper.statClickable("Fakultas", ProfileUiHelper.fmt(Long.valueOf(fakultas)),
							"Fakultas yang ada", idFak),
					mahasiswa <= 0 ? null
							: ProfileUiHelper.donutClickable("Sudah Lulus", lulus, mahasiswa, "#059669",
									"Dibanding seluruh mahasiswa", idLulus),
					mahasiswa <= 0 ? null
							: ProfileUiHelper.donutClickable("Masih Aktif", aktif, mahasiswa, "#2563eb",
									"Dibanding seluruh mahasiswa", idAktif) });

			StringBuffer popup = new StringBuffer();
			popup.append(ProfileUiHelper.modal(idMhs, "Rincian Mahasiswa",
					ProfileUiHelper.modalAngka("Mahasiswa", ProfileUiHelper.fmt(Long.valueOf(mahasiswa)),
							"Semua mahasiswa yang tercatat",
							"Rincian per program studi ada di kartu \"Mahasiswa Aktif\" pada panel Ringkasan Akademik di atas; daftar per individu di menu Mahasiswa.")));
			popup.append(ProfileUiHelper.modal(idDosen, "Rincian Dosen",
					ProfileUiHelper.modalAngka("Dosen", ProfileUiHelper.fmt(Long.valueOf(dosen)),
							"Semua dosen yang tercatat",
							"Rincian per prodi ada di kartu \"Dosen Aktif\" / panel \"Jml dosen per prodi\"; daftar per individu di menu Dosen.")));
			popup.append(ProfileUiHelper.modal(idProdi, "Rincian Prodi",
					ProfileUiHelper.modalAngka("Prodi", ProfileUiHelper.fmt(Long.valueOf(prodi)),
							"Program studi yang dibuka", "Daftar program studi ada di menu Prodi / Jurusan.")));
			popup.append(ProfileUiHelper.modal(idFak, "Rincian Fakultas",
					ProfileUiHelper.modalAngka("Fakultas", ProfileUiHelper.fmt(Long.valueOf(fakultas)),
							"Fakultas yang ada", "Daftar fakultas ada di menu Fakultas.")));
			popup.append(ProfileUiHelper.modal(idLulus, "Rincian Sudah Lulus",
					ProfileUiHelper.tabelRincian(new String[] {
							ProfileUiHelper.barisRincian("Sudah lulus", ProfileUiHelper.fmt(Long.valueOf(lulus))),
							ProfileUiHelper.barisRincian("Total mahasiswa", ProfileUiHelper.fmt(Long.valueOf(mahasiswa))),
							ProfileUiHelper.barisRincian("Porsi", persen(lulus, mahasiswa)) })));
			popup.append(ProfileUiHelper.modal(idAktif, "Rincian Masih Aktif",
					ProfileUiHelper.tabelRincian(new String[] {
							ProfileUiHelper.barisRincian("Masih aktif (belum lulus)",
									ProfileUiHelper.fmt(Long.valueOf(aktif))),
							ProfileUiHelper.barisRincian("Total mahasiswa", ProfileUiHelper.fmt(Long.valueOf(mahasiswa))),
							ProfileUiHelper.barisRincian("Porsi", persen(aktif, mahasiswa)) })));

			MyRowStyled row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new Html(ProfileUiHelper.panel("Angka Penting",
					"Jumlah mahasiswa, dosen, dan prodi saat ini. Klik tiap angka untuk rinciannya.", isi)
					+ popup.toString()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menghitung dan memformat persentase {@code bagian} dari {@code total} sebagai string.
	 *
	 * <p><b>Tujuan:</b> Helper format persentase aman untuk tampilan di popup rincian kartu
	 * angka penting. Misalnya, menampilkan "68%" untuk 68 mahasiswa lulus dari 100.</p>
	 *
	 * <p><b>Cara kerja:</b> Jika {@code total &lt;= 0}, mengembalikan {@code "0%"} untuk
	 * menghindari pembagian dengan nol. Sebaliknya menghitung {@code round(bagian*100/total)}
	 * dan mengembalikan string diakhiri "%".</p>
	 *
	 * @param bagian nilai bagian (pembilang); boleh negatif (meski secara semantik tidak lazim)
	 * @param total  nilai total (penyebut); jika &lt;= 0 mengembalikan {@code "0%"}
	 * @return string persentase dibulatkan, mis. {@code "68%"} atau {@code "0%"}
	 */
	private static String persen(long bagian, long total) {
		if (total <= 0) {
			return "0%";
		}
		return Math.round((bagian * 100.0) / total) + "%";
	}

	/**
	 * Menambahkan seluruh panel statistik perguruan tinggi ke dalam grid profil yang sudah ada.
	 *
	 * <p><b>Tujuan:</b> Method statik inti profil PT — dapat dipanggil dari
	 * {@link #init(Component)} maupun dari profil gabungan atau lainnya untuk menyisipkan
	 * panel PT ke dalam konteks grid yang lebih besar.</p>
	 *
	 * <p><b>Guard aktif:</b> Seluruh panel hanya dirender jika tidak ada sekolah yang
	 * terpilih di sesi ({@code SekolahUtil.getSekolah() == null || id == null}).
	 * Ini mencegah duplikasi saat profil sekolah sudah dirender terlebih dahulu.</p>
	 *
	 * <p><b>Panel yang ditambahkan (berurutan):</b>
	 * <ol>
	 *   <li>Kehadiran dosen mode mobile via
	 *       {@code PengumumanAkademisAction.tampilkanKehadiranDosen}.</li>
	 *   <li>Dashboard akademik per prodi via
	 *       {@link ProfileAdminPerguruanTinggiAkademikDashboard#append(Rows)}.</li>
	 *   <li>Kartu angka penting clickable via {@link #appendAngkaPenting(Rows)}.</li>
	 *   <li>Prestasi ringkas via
	 *       {@link ProfileUiHelper#appendPrestasiRingkas(Rows, boolean, boolean, boolean)}
	 *       dengan {@code sekolah=false, pt=true, pegawai=true}.</li>
	 *   <li>Grafik mahasiswa per angkatan ({@link DashboardStatistikMahamahasiswaMasuk}).</li>
	 *   <li>Grafik mahasiswa lulus per tahun ({@link DashboardStatistikMahamahasiswaLulus}).</li>
	 *   <li>Grafik mahasiswa cuti ({@code dashboardStatistikMahamahasiswaLulus.initChartCuti}).</li>
	 *   <li>Grafik dosen per prodi ({@link DashboardStatistikDosenJurusan}).</li>
	 *   <li>Grafik calon mahasiswa per prodi ({@link DashboardStatistikCalonMahasiswaMasuk}).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error grafik:</b> Setiap {@code initChart} dibungkus try-catch
	 * kosong agar kegagalan satu grafik tidak menghentikan render panel lainnya.
	 * Error kritis dari {@code ProfileAdminPerguruanTinggiAkademikDashboard.append}
	 * ditampilkan ke admin via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * @param rows          baris ZK tujuan penambahan panel; tidak boleh {@code null}
	 * @param tbmuser       pengguna aktif untuk pengecekan kehadiran mobile dosen
	 * @param tampilkanChart tidak digunakan secara langsung di body (grafik HTML selalu
	 *                       dirender via {@code tampilkanGrafikHtml=true}); parameter ini
	 *                       dipertahankan untuk konsistensi signature dengan profil lain
	 */
	@SuppressWarnings("deprecation")
	public static void lanjut(Rows rows, Tbmuser tbmuser, boolean tampilkanChart) {
		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah == null || sekolah.getId() == null) {

			boolean mobile = Common.isMobile();
			if (mobile && !PengumumanAkademisAction.isKehadiranHomeDitampilkan()) {

				String pengumuman = PengumumanAkademisAction.tampilkanKehadiranDosen(tbmuser, mobile);
				if (!pengumuman.isEmpty()) {
					MyRowStyled row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.appendChild(new Html(pengumuman));
				}
			}

			try {
				ProfileAdminPerguruanTinggiAkademikDashboard.append(rows);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			// Ringkasan akademik lanjutan (skripsi, pengajuan TA per status, jadwal
			// kuliah, KKN/PKL, wisuda) — dihitung di latar (loading dulu) + cache L2/L3,
			// semua kartu clickable. Gabungan otomatis ikut karena memanggil lanjut() ini.
			try {
				ProfileAkademikLanjutanDashboard.append(rows);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			boolean tampilkanGrafikHtml = true;

			appendAngkaPenting(rows);

			// Ringkasan prestasi sesuai jenis admin PERGURUAN TINGGI: mahasiswa, dosen, pegawai.
			ProfileUiHelper.appendPrestasiRingkas(rows, false, true, true);

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml mahasiswa per tahun angkatan"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Mahasiswa per Angkatan", "Banyaknya mahasiswa menurut tahun masuk, untuk melihat naik-turunnya mahasiswa baru.");

			MyRowStyled row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			try {
				dashboardStatistikStatusMahamahasiswa.initChart(row, tampilkanGrafikHtml);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggi.java:421");
			}

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml mahasiswa per tahun lulus"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Mahasiswa Lulus", "Banyaknya mahasiswa yang lulus tiap tahun.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			try {
				dashboardStatistikMahamahasiswaLulus.initChart(row, tampilkanGrafikHtml);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggi.java:432");
			}
			
			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml mahasiswa cuti"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Mahasiswa Cuti", "Mahasiswa yang sedang berhenti kuliah sementara.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			try {
				dashboardStatistikMahamahasiswaLulus.initChartCuti(row);  
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggi.java:443");
			}

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml dosen per prodi"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Dosen per Prodi", "Banyaknya dosen di tiap program studi.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			try {
				dashboardStatistikDosenJurusan.initChart(row, tampilkanGrafikHtml);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggi.java:454");
			}

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml calon mhs per prodi"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Calon Mahasiswa per Prodi", "Program studi yang paling banyak dipilih pendaftar baru.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			try {
				dashboardStatistikCalonMahasiswaMasuk.initChart(row, tampilkanGrafikHtml);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggi.java:465");
			}
		}

	}

}
