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
import org.zkoss.zul.Vbox;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.dashboard.sekolah.DashboardStatistikGuruMasuk;
import ais.action.master.dashboard.sekolah.DashboardStatistikJadwalMengajar;
import ais.action.master.dashboard.sekolah.DashboardStatistikKelasSiswa;
import ais.action.master.dashboard.sekolah.DashboardStatistikSiswaLulus;
import ais.action.master.dashboard.sekolah.DashboardStatistikSiswaMasuk;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileAdminSekolah — Panel Profil Admin/Kepala Sekolah</h3>
 *
 * <p><b>Untuk apa:</b> Menyajikan dashboard profil satu halaman bagi pengguna yang
 * berperan sebagai admin atau kepala sekolah. Panel menampilkan kartu identitas admin,
 * angka penting (jumlah siswa, guru, komposisi jenis kelamin), prestasi, dan serangkaian
 * grafik statistik sekolah berbasis konfigurasi hak akses.</p>
 *
 * <p><b>Cara kerja:</b> Tersedia dua titik masuk:
 * <ol>
 *   <li>{@link #init(Component)} — untuk instansi standalone; membangun Grid penuh
 *       termasuk kartu identitas, kemudian memanggil {@link #lanjut(Rows, Tbmuser, boolean)}.</li>
 *   <li>{@link #lanjut(Rows, Tbmuser, boolean)} — method statik yang dapat dipanggil
 *       oleh profil lain (mis. {@link ProfileGabunganPengguna}) untuk menyisipkan panel
 *       sekolah ke dalam grid yang sudah ada.</li>
 * </ol>
 * </p>
 *
 * <p><b>Grafik yang ditampilkan (jika hak akses e-learning aktif dan bukan PT):</b>
 * <ul>
 *   <li>Jumlah siswa per tahun angkatan ({@link DashboardStatistikSiswaMasuk})</li>
 *   <li>Jumlah siswa lulus per tahun ({@link DashboardStatistikSiswaLulus})</li>
 *   <li>Jumlah siswa per kelas, dengan filter tahun ajaran ({@link DashboardStatistikKelasSiswa})</li>
 *   <li>Jumlah guru ({@link DashboardStatistikGuruMasuk})</li>
 *   <li>Jumlah jadwal pelajaran, dengan filter tahun ajaran dan semester
 *       ({@link DashboardStatistikJadwalMengajar})</li>
 * </ul>
 * </p>
 *
 * <p><b>Field {@code lain}:</b> Bila {@code true} (default), setelah panel sekolah,
 * profil ini memeriksa apakah modul PT juga aktif (tanpa sekolah yang terpilih). Jika ya,
 * memanggil {@link ProfileAdminPerguruanTinggi#lanjut(Rows, Tbmuser, boolean)} untuk
 * menambahkan ringkasan PT ke halaman yang sama.</p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread. Instance {@code Dashboard*}
 * adalah singleton statik — hanya memanggil {@code initChart} yang tidak mengubah state.</p>
 *
 * <p><b>Pemeliharaan:</b> Kompatibel Java 1.7 dan ZKoss 5.5. Penambahan grafik baru
 * perlu menambahkan entry {@code Dashboard*} statik dan row baru di {@link #lanjut}.</p>
 */
public class ProfileAdminSekolah {
	private static DashboardStatistikGuruMasuk dashboardStatistikGuruMasuk = new DashboardStatistikGuruMasuk();
	private static DashboardStatistikSiswaMasuk dashboardStatistikStatusSiswa = new DashboardStatistikSiswaMasuk();
	private static DashboardStatistikSiswaLulus dashboardStatistikSiswaLulus = new DashboardStatistikSiswaLulus();
	private static DashboardStatistikKelasSiswa dashboardStatistikKelasSiswa = new DashboardStatistikKelasSiswa();
	private static DashboardStatistikJadwalMengajar dashboardStatistikJadwalMengajar = new DashboardStatistikJadwalMengajar();

	private Tbmuser tbmuser;
	private boolean lain = true;
	private boolean tampilkanChart = false;

	/**
	 * Membuat panel profil sekolah dengan mode {@code lain=true} dan {@code tampilkanChart=false}.
	 *
	 * <p>Konstruktor ringkas untuk penggunaan standar; panel sekolah ditampilkan penuh,
	 * dan jika PT juga aktif (tanpa sekolah) maka ringkasan PT ikut ditambahkan.
	 * Grafik hanya ditampilkan dalam bentuk placeholder (bukan render Chart.js).</p>
	 *
	 * @param tbmuser pengguna yang sedang login; digunakan untuk kartu identitas
	 *                dan pengecekan hak akses via {@code tbmuser.hakAkses().getElearning()}
	 */
	public ProfileAdminSekolah(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Membuat panel profil sekolah dengan kontrol penuh atas mode "lain" dan tampilan grafik.
	 *
	 * @param tbmuser       pengguna yang sedang login
	 * @param lain          {@code true} untuk ikut menampilkan ringkasan PT jika PT aktif
	 *                      dan tidak ada sekolah yang terpilih di sesi saat ini
	 * @param tampilkanChart {@code true} untuk merender grafik statistik via Chart.js/ZK chart;
	 *                       {@code false} untuk mode ringkasan teks saja
	 */
	public ProfileAdminSekolah(Tbmuser tbmuser, boolean lain, boolean tampilkanChart) {
		this.tbmuser = tbmuser;
		this.lain = lain;
		this.tampilkanChart = tampilkanChart;
	}

	/**
	 * Membangun panel profil admin sekolah lengkap ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Titik masuk instance untuk merender halaman profil sekolah
	 * secara standalone, termasuk kartu identitas admin, ringkasan sekolah, dan
	 * opsional ringkasan PT.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link ProfileUiHelper#prepareContentParent(Component)} untuk
	 *       membersihkan parent dan menyuntikkan CSS profil.</li>
	 *   <li>Membangun Grid 2-kolom (80px | auto) di dalam parent.</li>
	 *   <li>Jika parent adalah {@code LayoutRegion}, set judul dan collapse/split.
	 *       Jika bukan, tambahkan baris info via
	 *       {@link ProfileUiHelper#appendPanelInfoRow(Rows, int, String, String)}.</li>
	 *   <li>Memuat foto profil admin dan merender kartu identitas (nama, yayasan, sekolah,
	 *       HP, email) via {@link ProfileUiHelper#mulaiKartuIdentitas(Rows, int, Component, String)}.
	 *       Deteksi sekolah/yayasan didahulukan dari {@code SekolahUtil.getSekolah()} sebelum
	 *       fallback ke {@code tbmuser.ambilSekolah()}.</li>
	 *   <li>Memanggil {@link #lanjut(Rows, Tbmuser, boolean)} untuk menambahkan grafik.</li>
	 *   <li>Jika {@code lain==true} dan modul PT aktif serta tidak ada sekolah yang dipilih,
	 *       memanggil {@link ProfileAdminPerguruanTinggi#lanjut(Rows, Tbmuser, boolean)}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Kegagalan memuat foto tidak menghentikan render;
	 * exception diprint ke stderr dan {@code foto} dibiarkan {@code null}.</p>
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

			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Profil Admin Sekolah", "Ringkasan kondisi sekolah: siswa, guru, kelas, dan jadwal pelajaran.");
		}
		if (tbmuser != null) {
			/* Kartu identitas satu baris: foto + sapaan + nama + kontak */
			Component foto = null;
			try {
				foto = CommonMedia.tampilkanGambarKecil(tbmuser);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileAdminSekolah.java:189");
			}
			org.zkoss.zul.Div vbox = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, foto, "Hai, Selamat " + waktu);

			Sekolah sekolah = tbmuser == null ? null : tbmuser.ambilSekolah();
			Yayasan yayasan = tbmuser == null ? null : tbmuser.ambilYayasan();

			boolean[] a = Common.chekPtAtauSekolah(tbmuser);
			boolean ya = a[1];
			if (ya) {
				Sekolah sekolah2 = SekolahUtil.getSekolah();
				if (sekolah2 != null && sekolah2.getId() != null) {
					sekolah = sekolah2;
					yayasan = sekolah2.getYayasan();
				} else {
					Yayasan yayasan2 = SekolahUtil.getYayasan();
					if (yayasan2 != null && yayasan2.getId() != null) {
						yayasan = yayasan2;
					}
				}
			}

			vbox.appendChild(new MyLabelBoldAja(tbmuser.getUserNama()));
			vbox.appendChild(new MyLabelBoldAja(yayasan == null ? "" : yayasan.getNama()));
			vbox.appendChild(new MyLabelBoldAja(sekolah == null ? "" : sekolah.getNama()));

			tbmuser.tampilkanHp(vbox);
			tbmuser.tampilkanEmail(vbox);
		}

		ProfileAdminSekolah.lanjut(rows, tbmuser, tampilkanChart);

		if (lain) {

			boolean[] ptYa = Common.chekPtAtauSekolah();
			boolean pt = ptYa[0];

			Sekolah sekolah = SekolahUtil.getSekolah();
			if (sekolah == null || sekolah.getId() == null) {
				if (Common.getApakahAdmin() && pt) {
					ProfileAdminPerguruanTinggi.lanjut(rows, tbmuser, tampilkanChart);
				}
			}
		}
	}

	/**
	 * Merender kartu "Angka Penting" sekolah ke dalam baris grid.
	 *
	 * <p><b>Tujuan:</b> Menyajikan ringkasan kuantitatif utama sekolah dalam bentuk kartu
	 * statistik visual: jumlah siswa, jumlah guru, dan donut chart komposisi jenis kelamin
	 * siswa. Kartu ini muncul sebagai baris pertama panel profil sekolah.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mendapatkan sekolah aktif via {@code SekolahUtil.getSekolah()}; jika ada,
	 *       menyusun filter {@code Criterion} {@code eq("sekolah", sekolah)} untuk
	 *       membatasi query hanya pada sekolah tersebut.</li>
	 *   <li>Menghitung jumlah siswa via {@link ProfileUiHelper#hitung(Class, Criterion[])}.</li>
	 *   <li>Menghitung jumlah guru via {@link ProfileUiHelper#hitung(Class, Criterion[])}.</li>
	 *   <li>Mengelompokkan siswa berdasarkan {@code jenisKelamin} via
	 *       {@link ProfileUiHelper#kelompok(Class, String, Criterion[], int)} dan
	 *       mengklasifikasikan label ke laki-laki (diawali "l" atau mengandung "laki")
	 *       atau perempuan (diawali "p" atau mengandung "perempuan"/"wanita").</li>
	 *   <li>Menyusun HTML kartu via {@link ProfileUiHelper#statsWrap(String[])},
	 *       {@link ProfileUiHelper#stat(String, String, String)}, dan
	 *       {@link ProfileUiHelper#donut(String, double, double, String, String)};
	 *       donut hanya ditambahkan jika ada data jenis kelamin (totalLp &gt; 0).</li>
	 *   <li>Menambahkan baris baru ke {@code rows} dengan kolspan 2 berisi Html ZK.</li>
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
			Sekolah sekolah = SekolahUtil.getSekolah();
			org.hibernate.criterion.Criterion filterSekolah = sekolah != null && sekolah.getId() != null
					? org.hibernate.criterion.Restrictions.eq("sekolah", sekolah)
					: null;
			org.hibernate.criterion.Criterion[] filters = new org.hibernate.criterion.Criterion[] { filterSekolah };

			long jumlahSiswa = ProfileUiHelper.hitung(ais.database.model.sekolah.Siswa.class, filters);
			long jumlahGuru = ProfileUiHelper.hitung(ais.database.model.sekolah.Guru.class, filters);

			long lakiLaki = 0;
			long perempuan = 0;
			java.util.List<Object[]> jenisKelamin = ProfileUiHelper.kelompok(ais.database.model.sekolah.Siswa.class,
					"jenisKelamin", filters, 0);
			for (int i = 0; i < jenisKelamin.size(); i++) {
				String label = ProfileUiHelper.text(jenisKelamin.get(i)[0]).trim().toLowerCase();
				double nilai = ProfileUiHelper.asDouble(jenisKelamin.get(i)[1]);
				if (label.startsWith("l") || label.startsWith("p r") || label.indexOf("laki") >= 0) {
					lakiLaki += (long) nilai;
				} else if (label.startsWith("p") || label.indexOf("perempuan") >= 0 || label.indexOf("wanita") >= 0) {
					perempuan += (long) nilai;
				}
			}
			long totalLp = lakiLaki + perempuan;

			String isi = ProfileUiHelper.statsWrap(new String[] {
					ProfileUiHelper.stat("Siswa", ProfileUiHelper.fmt(Long.valueOf(jumlahSiswa)),
							"Semua siswa yang tercatat"),
					ProfileUiHelper.stat("Guru", ProfileUiHelper.fmt(Long.valueOf(jumlahGuru)),
							"Semua guru yang tercatat"),
					totalLp <= 0 ? null
							: ProfileUiHelper.donut("Siswa Laki-laki", lakiLaki, totalLp, "#2563eb",
									"Dibanding seluruh siswa"),
					totalLp <= 0 ? null
							: ProfileUiHelper.donut("Siswa Perempuan", perempuan, totalLp, "#db2777",
									"Dibanding seluruh siswa") });

			MyRowStyled row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new Html(
					ProfileUiHelper.panel("Angka Penting", "Jumlah siswa dan guru saat ini.", isi)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menambahkan seluruh panel statistik sekolah ke dalam grid profil yang sudah ada.
	 *
	 * <p><b>Tujuan:</b> Method statik yang merupakan "inti" profil sekolah — dapat
	 * dipanggil baik dari {@link #init(Component)} maupun dari profil lain seperti
	 * {@link ProfileGabunganPengguna#init(Component)} untuk menyisipkan panel sekolah
	 * ke dalam konteks grid yang lebih besar.</p>
	 *
	 * <p><b>Guard aktif:</b> Method hanya berjalan jika semua kondisi berikut terpenuhi:
	 * <ul>
	 *   <li>{@code tbmuser.hakAkses().getElearning() == true}</li>
	 *   <li>{@code tbmuser.getPerguruanTinggi() == null || id == null} (bukan admin PT langsung)</li>
	 * </ul>
	 * Jika guard gagal, tidak ada komponen yang ditambahkan ke {@code rows}.
	 * </p>
	 *
	 * <p><b>Panel yang ditambahkan (berurutan):</b>
	 * <ol>
	 *   <li>Kehadiran guru mobile (jika {@code Common.isMobile()}) via
	 *       {@code PengumumanAkademisAction.tampilkanKehadiranGuru}.</li>
	 *   <li>Kartu angka penting via {@link #appendAngkaPenting(Rows)}.</li>
	 *   <li>Kartu prestasi ringkas via
	 *       {@link ProfileUiHelper#appendPrestasiRingkas(Rows, boolean, boolean, boolean)}
	 *       dengan {@code sekolah=true, pt=false, pegawai=true}.</li>
	 *   <li>Grafik jumlah siswa per angkatan ({@link DashboardStatistikSiswaMasuk}).</li>
	 *   <li>Grafik jumlah siswa lulus ({@link DashboardStatistikSiswaLulus}).</li>
	 *   <li>Grafik jumlah siswa per kelas + Combobox filter tahun ajaran
	 *       ({@link DashboardStatistikKelasSiswa}); onChange memperbarui chart.</li>
	 *   <li>Grafik jumlah guru ({@link DashboardStatistikGuruMasuk}).</li>
	 *   <li>Grafik jumlah jadwal pelajaran + Combobox filter tahun ajaran dan semester
	 *       (Ganjil/Genap) ({@link DashboardStatistikJadwalMengajar}); onChange memperbarui
	 *       chart secara independen per filter.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> {@code @SuppressWarnings("deprecation")} karena
	 * {@code dashboardStatistik*.initChart} menggunakan API ZK lama. Jangan hapus
	 * anotasi ini tanpa terlebih dahulu memperbarui semua Dashboard* ke API baru.</p>
	 *
	 * @param rows          baris ZK tujuan penambahan panel; tidak boleh {@code null}
	 * @param tbmuser       pengguna aktif untuk pengecekan hak akses
	 * @param tampilkanChart {@code true} untuk merender grafik; {@code false} untuk
	 *                       placeholder teks
	 */
	@SuppressWarnings("deprecation")
	public static void lanjut(Rows rows, Tbmuser tbmuser, final boolean tampilkanChart) {

		if (tbmuser != null && tbmuser.hakAkses() != null && Boolean.TRUE.equals(tbmuser.hakAkses().getElearning())
				&& (tbmuser.getPerguruanTinggi() == null || tbmuser.getPerguruanTinggi().getId() == null)) {
			boolean mobile = Common.isMobile();
			MyRowStyled row;
			if (mobile && !PengumumanAkademisAction.isKehadiranHomeDitampilkan()) {

				String pengumuman = PengumumanAkademisAction.tampilkanKehadiranGuru(tbmuser, mobile);
				if (!pengumuman.isEmpty()) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.appendChild(new Html(pengumuman));
				}
			}

			appendAngkaPenting(rows);

			// Ringkasan data sekolah menyeluruh (siswa, guru, kelas, mapel, jadwal,
			// pembayaran, pelanggaran, catatan, kegiatan) — dihitung di latar (loading
			// dulu) + cache L2/L3, semua kartu clickable.
			try {
				ProfileSekolahLanjutanDashboard.append(rows);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			// Ringkasan prestasi sesuai jenis admin SEKOLAH: siswa, guru, pegawai.
			ProfileUiHelper.appendPrestasiRingkas(rows, true, false, true);

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml siswa per tahun angkatan"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Siswa per Angkatan", "Banyaknya siswa menurut tahun masuk, untuk melihat naik-turunnya jumlah siswa baru.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			dashboardStatistikStatusSiswa.initChart(row, tampilkanChart);

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml siswa per tahun lulus"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Siswa Lulus", "Banyaknya siswa yang lulus tiap tahun.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			dashboardStatistikSiswaLulus.initChart(row, tampilkanChart);

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml siswa per kelas"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Siswa per Kelas", "Banyaknya siswa di tiap kelas, agar pembagian kelas terlihat merata atau tidak.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			final Row rowKelas = new MyRowStyled();

			final Combobox tahunAjaran = Common.generateTahunAjaran(null);
			Hbox hbox = new Hbox();
			hbox.setParent(row);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Ajaran:")));
			hbox.appendChild(tahunAjaran);
			tahunAjaran.setCols(5);
			tahunAjaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dashboardStatistikKelasSiswa.initChart(rowKelas, tampilkanChart, tahunAjaran);
				}
			});

			rowKelas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowKelas, "2");
			dashboardStatistikKelasSiswa.initChart(rowKelas, tampilkanChart, tahunAjaran);

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml guru"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Guru", "Banyaknya guru yang tercatat di sekolah.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			dashboardStatistikGuruMasuk.initChart(row, tampilkanChart);

			rows.appendChild(new ais.ui.util.MyGroupConfig("Jml jadwal pelajaran"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jumlah Jadwal Pelajaran", "Banyaknya jadwal pelajaran pada tahun ajaran dan semester yang dipilih.");

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
			hbox = new Hbox();
			hbox.setParent(row);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Ajaran:")));
			hbox.appendChild(tahunAjaranJadwal);
			tahunAjaranJadwal.setCols(5);
			tahunAjaranJadwal.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dashboardStatistikJadwalMengajar.initChart(rowJadwal, tampilkanChart, tahunAjaranJadwal, null, null,
							ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
				}
			});

			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester:")));
			hbox.appendChild(searchJenisSemester);
			searchJenisSemester.setCols(2);
			searchJenisSemester.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dashboardStatistikJadwalMengajar.initChart(rowJadwal, tampilkanChart, tahunAjaranJadwal, null, null,
							ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
				}
			});

			rowJadwal.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowJadwal, "2");
			dashboardStatistikJadwalMengajar.initChart(rowJadwal, tampilkanChart, tahunAjaranJadwal, null, null,
					ProfileUiHelper.selectedInteger(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2));
		}
	}

}
