package ais.action.master.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFComment;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.VOMahasiswa;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.Ambildata;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>HasilUjianMahasiswaHelper — Panel Rekap dan Koreksi Hasil Ujian</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini bertanggung jawab menampilkan rekap hasil ujian dari
 * semua peserta untuk satu {@code PertemuanPunyaUjian}. Ia mengelola grid data hasil ujian,
 * fitur koreksi nilai esai, ekspor ke Excel/Google Drive, analisis butir soal (item analysis),
 * laporan hasil OBE, dan fungsi administratif seperti "Ujian Dianggap Hadir".</p>
 *
 * <p><b>Cara kerja:</b> Implementasi pola "Helper-as-DataLoader" — kelas ini mengimplementasikan
 * {@link DataLoader} sehingga dapat dipakai sebagai callback sumber data untuk {@code MyGrid}.
 * Alur utama:</p>
 * <ol>
 *   <li>Diinstansiasi oleh Action (dosen/admin) via salah satu konstruktor.</li>
 *   <li>{@link #display(PertemuanPunyaUjian,Component)} membangun layout grid + toolbar
 *       dalam {@code Component} yang diberikan (biasanya panel detail atau tab).</li>
 *   <li>{@link #loadData(Object)} dipanggil oleh framework saat grid perlu dimuat atau
 *       di-refresh. Method ini mengambil data {@code HasilUjianMahasiswa} dari database
 *       menggunakan berbagai Criteria Hibernate.</li>
 *   <li>{@link #tampilRow(MyDetail,HasilUjianMahasiswa)} dipanggil per baris untuk
 *       merender detail satu hasil ujian (soal, jawaban, nilai).</li>
 * </ol>
 *
 * <p><b>Fitur-fitur utama:</b></p>
 * <ul>
 *   <li><b>Grid hasil ujian:</b> Menampilkan nama peserta, nilai, waktu pengerjaan, status lulus,
 *       tombol detail (lihat jawaban), tombol koreksi nilai esai.</li>
 *   <li><b>Statistik:</b> {@link #displayStatistik} menampilkan ringkasan statistik ujian
 *       (jumlah peserta, terjawab, ikut ujian) di panel East.</li>
 *   <li><b>Ekspor Excel:</b> Tombol ekspor membuat file .xlsx berisi rekap hasil ujian
 *       termasuk detail jawaban per soal.</li>
 *   <li><b>Hasil OBE:</b> {@link #hasilObe(PertemuanPunyaUjian,Ambildata)} menampilkan
 *       rekap capaian OBE per Sub-CPMK berdasarkan data hasilJsonObe.</li>
 *   <li><b>Analisis Butir Soal:</b> {@link #analsisButirSoal(PertemuanPunyaUjian,Ambildata)}
 *       menghitung tingkat kesukaran (TK) dan daya pembeda (DP) untuk setiap soal,
 *       serta menghasilkan visualisasi HTML dan file Excel analisis.</li>
 *   <li><b>Ujian Dianggap Hadir:</b> {@link #ujianDianggapHadir(PertemuanPunyaUjian,EventListener)}
 *       secara massal menandai semua peserta yang mengikuti ujian sebagai "hadir" di presensi.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Method-method yang menghasilkan laporan berat (Excel, analisis butir soal,
 * hasil OBE) menggunakan thread latar dengan pola loading-bar ZK. Thread ini menggunakan
 * session Hibernate terdedikasi (bukan ZK currentSession) dan menutup session di finally.
 * Hasil disimpan ke array final[] sebagai "shared container" antara thread latar dan callback
 * ZK event. Ini aman karena {@code label.setValue("")} di akhir thread menciptakan
 * happens-before dengan callback ZK.</p>
 *
 * <p><b>Pemeliharaan:</b> Bila menambah kolom baru ke ekspor Excel, tambahkan header di
 * {@link #loadData} (bagian header row) dan nilai di {@link #tampilRow}. Untuk menambah
 * fitur analisis baru, pertimbangkan method statik baru di kelas ini dengan pola yang sama
 * seperti {@link #hasilObe} atau {@link #analsisButirSoal}.</p>
 *
 * @see HasilUjianMahasiswa
 * @see ProsesUjianHelper
 * @see DataLoader
 */
public class HasilUjianMahasiswaHelper implements DataLoader {

	/** Konfigurasi ujian dalam pertemuan yang sedang ditampilkan hasilnya. */
	private PertemuanPunyaUjian pertemuanPunyaUjian;

	/** Grid ZK yang menampilkan daftar hasil ujian seluruh peserta. */
	private MyGrid grid;

	/**
	 * Bila tidak null, grid hanya menampilkan hasil ujian untuk mahasiswa ini.
	 * Digunakan pada tampilan per-peserta (bukan tampilan rekap semua peserta).
	 */
	private Mahasiswa mahasiswa;

	/**
	 * Bila tidak null, grid hanya menampilkan hasil untuk calon mahasiswa ini (PMB).
	 * Saling eksklusif dengan {@link #mahasiswa}.
	 */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	/**
	 * Panel East/Center yang dipakai untuk menampilkan statistik ujian via
	 * {@link #displayStatistik(int,int,int)}.
	 */
	private Center east;

	/** Pertemuan yang menjadi konteks rekap ini (sumber semua PertemuanPunyaUjian). */
	private Pertemuan pertemuan;

	/** Textbox pencarian nama peserta (filter grid, hanya tersedia di mode rekap semua peserta). */
	private Textbox nama;

	/**
	 * Konstruktor untuk tampilan rekap semua peserta dalam satu pertemuan.
	 * {@link #mahasiswa} dan {@link #biodataCalonMahasiswa} diset null.
	 *
	 * @param pertemuan pertemuan yang hasilnya akan ditampilkan
	 */
	public HasilUjianMahasiswaHelper(Pertemuan pertemuan) {
		this.mahasiswa = null;
		this.biodataCalonMahasiswa = null;
		this.pertemuan = pertemuan;
	}

	/**
	 * Konstruktor untuk tampilan rekap hasil ujian satu peserta spesifik.
	 * Digunakan pada halaman portal peserta untuk melihat riwayat ujian sendiri.
	 *
	 * @param mahasiswa              mahasiswa yang hasilnya ditampilkan; null untuk mode peserta calon
	 * @param biodataCalonMahasiswa  calon mahasiswa PMB; null untuk mahasiswa reguler
	 * @param pertemuan              pertemuan yang hasilnya ditampilkan
	 */
	public HasilUjianMahasiswaHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			Pertemuan pertemuan) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.pertemuan = pertemuan;
	}

	/**
	 * Menampilkan panel ringkasan statistik ujian di area East/Center ({@link #east}).
	 *
	 * <p><b>Tujuan:</b> Memberikan gambaran cepat kepada dosen/admin tentang kondisi ujian:
	 * berapa peserta yang terdaftar, berapa yang sudah menjawab, dan berapa yang benar-benar
	 * mengikuti ujian (bukan sekadar terdaftar). Berguna untuk monitoring ujian real-time.</p>
	 *
	 * <p><b>Cara kerja:</b> Membersihkan panel {@link #east} lalu membuat {@code MyGrid}
	 * dengan dua kolom (label: nilai) yang menampilkan tiga metrik:
	 * {@code jumlahPeserta}, {@code terjawab}, dan {@code pesertaYgIkutUjian}.
	 * Grid bersifat read-only (tidak ada tombol aksi).</p>
	 *
	 * @param jumlahPeserta       total peserta yang terdaftar untuk ujian ini
	 * @param terjawab            peserta yang sudah menjawab minimal satu soal
	 * @param pesertaYgIkutUjian  peserta yang menekan "Ikuti Ujian" (telahIkutUjian=true)
	 */
	@SuppressWarnings({ })
	private void displayStatistik(int jumlahPeserta, int terjawab, int pesertaYgIkutUjian) {
		Common.clear(east);

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(east);
		grid.setWidth("100%");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Soal")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan())));

		int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan() * jumlahPeserta;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Soal")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(totalSoal)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Terjawab")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(terjawab)));

		int belum = totalSoal - terjawab;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Belum Terjawab")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

		Double persen = (100.0 * terjawab) / totalSoal;
		Double persenBelum = (100.0 * belum) / totalSoal;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new org.zkoss.zul.Html(buildStatistikPieHtml("Progres Jawaban", terjawab, totalSoal, "Terjawab", "Belum Terjawab")));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Peserta")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(jumlahPeserta)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta yg melaksanakan ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(pesertaYgIkutUjian)));

		belum = jumlahPeserta - pesertaYgIkutUjian;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta yg belum ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

		persen = (100.0 * pesertaYgIkutUjian) / jumlahPeserta;
		persenBelum = (100.0 * belum) / jumlahPeserta;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new org.zkoss.zul.Html(buildStatistikPieHtml("Keikutsertaan Peserta", pesertaYgIkutUjian, jumlahPeserta, "Ikut Ujian", "Belum Ujian")));

		TreeMap<String, String> d = pertemuanPunyaUjian.getPertemuan().ambilData("ujian_" + pertemuanPunyaUjian.getId(), null);
		List<Dosen> dsn = pertemuanPunyaUjian.getPertemuan().ambilDosen();
		int jumlahTotal = jumlahPeserta + dsn.size();
		int telahAkses1 = d.size();
		int belumAkses1 = jumlahTotal - telahAkses1;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total perserta yg bisa akses")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(jumlahTotal)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perserta yg akses ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(telahAkses1)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perserta yg belum akses")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belumAkses1)));

		persen = (100.0 * telahAkses1) / jumlahTotal;
		persenBelum = (100.0 * belumAkses1) / jumlahTotal;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new org.zkoss.zul.Html(buildStatistikPieHtml("Akses Ujian", telahAkses1, jumlahTotal, "Sudah Akses", "Belum Akses")));

		d.clear();
		d = null;
		dsn.clear();
		dsn = null;
	}

	/**
	 * Membangun seluruh antarmuka rekap hasil ujian (grid + toolbar + panel statistik)
	 * di dalam komponen {@code detail} yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Ini adalah titik masuk utama kelas ini. Dipanggil oleh Action/ZUL
	 * ketika dosen/admin membuka rekap hasil ujian untuk satu {@code PertemuanPunyaUjian}.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Menyimpan {@code pertemuanPunyaUjian} ke field instance dan membuat {@link #grid}.</li>
	 *   <li>Membuat {@code Borderlayout} dengan panel North (toolbar) dan Center (grid).
	 *       Bila pengguna saat ini adalah dosen/admin (bukan peserta), panel East berisi
	 *       statistik ujian.</li>
	 *   <li>Toolbar berisi tombol: Ulang Semua (hapus semua hasil & mulai dari awal),
	 *       Hitung Ulang (recalculate skor), Ekspor Excel, Upload ke GDrive,
	 *       Ujian Dianggap Hadir, dan (bila OBE) Hasil OBE.</li>
	 *   <li>Grid diinisialisasi dengan kolom-kolom: Nama, No. Ujian/NIM/NISN, Nilai, Status Lulus,
	 *       Waktu Pengerjaan, Sisa Waktu, dan kolom aksi (Detail/Koreksi).</li>
	 *   <li>Setelah layout selesai, memanggil {@code DataCriteria.init(grid, this)} untuk
	 *       memuat data pertama kali via {@link #loadData(Object)}.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception dari toolbar event listener ditangkap per-listener.
	 * Kesalahan memuat data pertama kali akan terlihat sebagai grid kosong.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika menambah tombol baru di toolbar, tambahkan di sini.
	 * Jika menambah kolom baru di grid, tambahkan di sini (header) dan di
	 * {@link HasilUjianMahasiswaHelper.RowRenderer#render} (nilai).</p>
	 *
	 * @param pertemuanPunyaUjian konfigurasi ujian yang hasilnya akan ditampilkan
	 * @param detail              komponen ZK induk tempat layout akan dipasang
	 */
	public void display(final PertemuanPunyaUjian pertemuanPunyaUjian, final Component detail) {
		this.pertemuanPunyaUjian = pertemuanPunyaUjian;
		grid = new MyGrid();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(detail);

		North north = new North();
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		Tbmuser tbmuser = Common.getCurrentUser();

		// Edit Ujian: buka "Pengaturan Data Ujian" (Ujian.java) langsung dari layar Hasil Ujian.
		MyToolbarbuttonConfig btnEditUjian = new MyToolbarbuttonConfig("Edit Ujian", "/img/svg/edit-box-line.svg");
		btnEditUjian.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getSiswa() == null);
		btnEditUjian.addEventListener("onClick", new EventListener() {
			/**
			 * Membuka modal "Pengaturan Data Ujian" milik {@code PertemuanPunyaUjianHelper}
			 * langsung dari layar Hasil Ujian, sehingga dosen tidak perlu menutup rekap dan
			 * menelusuri ulang pertemuan hanya untuk mengubah durasi, jumlah soal ditampilkan,
			 * atau jendela waktu ujian.
			 *
			 * <p>Helper diinstansiasi dengan dua argumen {@code null} karena
			 * {@code bukaPengaturanUjian} tidak memerlukan konteks mahasiswa/calon mahasiswa —
			 * ia hanya menyunting konfigurasi ujian. {@code EventListener} yang diserahkan
			 * sebagai argumen kedua adalah callback "sesudah simpan".</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembangunan modal pengaturan
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				new PertemuanPunyaUjianHelper(null, null).bukaPengaturanUjian(pertemuanPunyaUjian,
						new EventListener() {
							/**
							 * Callback yang dijalankan setelah modal pengaturan ujian menyimpan
							 * perubahan. Memanggil {@code loadData(true)} dengan penanda refresh
							 * {@code true} agar himpunan soal terjawab dibaca ULANG dari sumbernya
							 * (menembus cache) — perlu karena mengubah jumlah soal ditampilkan
							 * mengubah pula paket soal tiap peserta, sehingga statistik dan kolom
							 * Skor/Max pada grid akan salah bila memakai data cache.
							 *
							 * @param e event penanda selesai simpan; tidak dipakai
							 * @throws Exception diteruskan dari pemuatan ulang grid
							 */
							@Override
							public void onEvent(Event e) throws Exception {
								loadData(true);
							}
						});
			}
		});
		btnEditUjian.setParent(toolbar);

		boolean rakhasil = Common.bolehKonfigurasi("tampilkan_rekap_hasil_ujian");

		boolean masihAdaWaktu = (pertemuanPunyaUjian.getMulaiUjian() == null
				|| pertemuanPunyaUjian.getMulaiUjian().before(ais.ui.util.WaktuUtil.getDate()))
				&& (pertemuanPunyaUjian.getSampaiUjian() == null
						|| pertemuanPunyaUjian.getSampaiUjian().after(ais.ui.util.WaktuUtil.getDate()));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ulang Semua", "/img/svg/trash.svg");
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		button.setDisabled(!masihAdaWaktu);
		button.addEventListener("onClick", new EventListener() {

			/**
			 * Meminta konfirmasi sebelum menjalankan aksi <b>destruktif</b> "Ulang Semua".
			 *
			 * <p>Listener ini sendiri tidak mengubah apa pun; ia hanya memunculkan
			 * {@code MyMessageboxConfig} bertombol OK/Batal berikon pertanyaan, dengan teks
			 * peringatan yang menyatakan secara tegas bahwa seluruh hasil dan jawaban peserta
			 * akan dikosongkan dan TIDAK DAPAT DIKEMBALIKAN. Pekerjaan sesungguhnya dilakukan
			 * listener bersarang di bawah, hanya bila pengguna memilih OK.</p>
			 *
			 * <p>Tombol pemicunya di-{@code setDisabled(!masihAdaWaktu)}, yaitu hanya aktif
			 * selama jendela waktu ujian masih berjalan — penjagaan agar hasil ujian yang sudah
			 * berakhir tidak dikosongkan secara tak sengaja. Perlu dicatat bahwa penjagaan itu
			 * bersifat UI semata; listener ini tidak memeriksa ulang jendela waktu.</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembangunan dialog konfirmasi
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin mengulang seluruh ujian ini? Seluruh hasil dan jawaban peserta pada ujian ini akan dikosongkan dan tidak dapat dikembalikan. Silakan pilih OK untuk melanjutkan atau Batal untuk membatalkan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

							/**
							 * Mengosongkan hasil ujian SELURUH peserta setelah pengguna menekan OK.
							 *
							 * <p><b>Cara kerja.</b> Jawaban pengguna dibaca dari
							 * {@code event.getData()} dan hanya diproses bila sama dengan
							 * {@code MyMessageboxConfig.OK}. Selanjutnya, dalam SATU transaksi
							 * pada session terdedikasi:</p>
							 * <ol>
							 *   <li>Mengambil seluruh {@link HasilUjianMahasiswa} milik
							 *       {@code pertemuanPunyaUjian} ini.</li>
							 *   <li>Untuk tiap peserta, seluruh {@link HasilUjianMahasiswaDetail}
							 *       di-<i>null</i>-kan pada tiga kolom: {@code bankSoalDetail}
							 *       (pilihan yang dipilih), {@code jawaban} (teks jawaban), dan
							 *       {@code waktuJawab}. Baris detail sengaja TIDAK dihapus,
							 *       melainkan dikosongkan — struktur soal per peserta tetap utuh
							 *       sehingga peserta dapat langsung mengulang tanpa pembentukan
							 *       ulang paket soal, dan relasi ke lampiran jawaban tidak putus.</li>
							 *   <li>{@code hasilUjianMahasiswa.reset()} mengembalikan entity utama
							 *       ke keadaan awal (nilai, waktu mulai/selesai, penanda ikut ujian).</li>
							 *   <li>{@code session.flush()} + {@code session.clear()} setiap 50
							 *       peserta untuk menahan pertumbuhan first-level cache pada kelas
							 *       besar.</li>
							 * </ol>
							 *
							 * <p><b>Transaksi tunggal.</b> Seluruh peserta berada dalam satu
							 * transaksi, sehingga kegagalan di tengah proses me-rollback SEMUANYA —
							 * tidak ada keadaan setengah-terhapus. Konsekuensinya, pada ujian
							 * berpeserta sangat banyak transaksi ini berumur panjang dan menahan
							 * kunci baris cukup lama.</p>
							 *
							 * <p><b>Penanganan error.</b> Kegagalan me-rollback transaksi,
							 * menampilkan detail teknis kepada administrator
							 * ({@code Common.tampilErrorJikaAdmin}), dan memunculkan pesan
							 * berbahasa manusia berisi tiga langkah yang dapat dicoba pengguna.
							 * Session ditutup di {@code finally}.</p>
							 *
							 * <p><b>Otorisasi.</b> Tidak ada pemeriksaan peran di dalam listener.
							 * Perlindungan sepenuhnya berasal dari {@code setVisible(...)} pada
							 * tombol pemicu (bukan mahasiswa, bukan siswa, bukan peserta kursus,
							 * dan bukan mode satu peserta) — pola penjagaan hanya di lapisan UI.
							 * Mengingat aksi ini menghapus jawaban seluruh peserta secara permanen,
							 * pemeriksaan peran di sisi server layak dipertimbangkan.</p>
							 *
							 * @param event event dialog; {@code getData()} berisi kode tombol yang
							 *              ditekan pengguna
							 * @throws Exception diteruskan dari penguraian kode tombol
							 */
							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Session session = null;
									Transaction tx = null;
									try {
										session = HibernateUtil.getSessionFactory().openSession();
										tx = session.beginTransaction();

										List<HasilUjianMahasiswa> ujianMahasiswas = session
												.createCriteria(HasilUjianMahasiswa.class)
												.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
												.list();

										int count = 0;
										for (HasilUjianMahasiswa hasilUjianMahasiswa : ujianMahasiswas) {

											List<HasilUjianMahasiswaDetail> hasilUjianMahasiswaDetails = session
													.createCriteria(HasilUjianMahasiswaDetail.class)
													.add(Restrictions.eq("hasilUjianMahasiswa", hasilUjianMahasiswa))
													.list();
											for (HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail : hasilUjianMahasiswaDetails) {
												hasilUjianMahasiswaDetail.setBankSoalDetail(null);
												hasilUjianMahasiswaDetail.setJawaban(null);
												hasilUjianMahasiswaDetail.setWaktuJawab(null);
												session.update(hasilUjianMahasiswaDetail);
											}
											hasilUjianMahasiswa.reset();
											session.update(hasilUjianMahasiswa);

											count++;
											if (count % 50 == 0) {
												session.flush();
												session.clear();
											}
										}

										tx.commit();
										loadData(null);
									} catch (Exception e) {
										if (tx != null) tx.rollback();
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(Common.pesan(
				"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain di dalam sistem. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala masih berlanjut, hubungi Admin untuk bantuan lebih lanjut.",
				e.getMessage()));
									} finally {
										if (session != null && session.isOpen()) {
											try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:500");}
										}
									}
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		final String[] contents = new String[] {
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.nim"
						: "biodataCalonMahasiswa.noRegistrasi",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.nama"
						: "biodataCalonMahasiswa.nama",

				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.telp"
						: "biodataCalonMahasiswa.asalSma",

				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.email"
						: "biodataCalonMahasiswa.noUjian",

				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.tahunangkatan"
						: "biodataCalonMahasiswa.prodi1.nama",

				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.jurusan.nama"
						: "biodataCalonMahasiswa.prodi2.nama",

				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "mahasiswa.jurusan.fakultas.nama"
						: "biodataCalonMahasiswa.prodi3.nama",

				"pertemuanPunyaUjian", "lamaPengerjaan", "sisaWaktuPengerjaan", "totalNilai-number",
				"jumlahSoal-number", "jawabanBenar-number", "jawabanBenarMax-number", "telahIkutUjian", "nilai-number",
				"lulus", "mulaiPada", "selesaiPada", "jumlahIkut-number", "keyhasil" };

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Jumlah dikerjakan");
		columnHeadersAdding.add("Jumlah Belum dikerjakan");
		columnHeadersAdding.add("Telah dikerjakan");
		columnHeadersAdding.add("Belum dikerjakan");

		EventListener dataAdding = new EventListener() {

			/**
			 * <b>Kait kolom tambahan</b> untuk ekspor Excel "Rekap Hasil Ujian". Dipanggil
			 * {@code Common.cetakDataCustomButton} SATU KALI PER BARIS peserta, setelah kolom
			 * standar (yang berasal dari array {@code contents}) selesai ditulis, untuk mengisi
			 * empat kolom terakhir yang tidak dapat dinyatakan sebagai jalur properti sederhana.
			 *
			 * <p><b>Kontrak {@code event.getData()}.</b> Berupa {@code Object[]} dengan
			 * {@code [0]} = entity {@link HasilUjianMahasiswa} baris ini dan {@code [2]} =
			 * {@link XSSFRow} baris Excel yang sedang ditulis. Indeks {@code [1]} tidak dipakai
			 * di sini. Kolom tambahan ditulis pada posisi {@code contents.length + 0..3}, sejajar
			 * dengan empat judul yang didaftarkan pada {@code columnHeadersAdding}: "Jumlah
			 * dikerjakan", "Jumlah Belum dikerjakan", "Telah dikerjakan", "Belum dikerjakan".
			 * Menambah judul tanpa menambah sel di sini (atau sebaliknya) akan menggeser kolom.</p>
			 *
			 * <p><b>Cara kerja.</b> Mengambil paket soal peserta ({@code ambilUjianPunyaSoals})
			 * dan peta jawabannya, lalu menelusuri setiap {@link HasilUjianMahasiswaDetail}.
			 * Untuk setiap jawaban tak kosong, teks "soal;JAWABAN:jawaban" dirangkai dan id soal
			 * yang bersangkutan DIBUANG dari {@code ujianPunyaSoals}. Dengan begitu, setelah
			 * penelusuran selesai, isi {@code ujianPunyaSoals} yang tersisa persis merupakan
			 * daftar soal yang BELUM dikerjakan — itulah sumber dua kolom terakhir.</p>
			 *
			 * <p><b>Penjagaan null berlapis.</b> Penentuan apakah huruf pilihan ikut ditampilkan
			 * memeriksa {@code getUjianPunyaSoal()}, {@code getUjian()}, dan
			 * {@code getTampilanHurufDiPilihanJawaban()} (yang bertipe {@code Boolean} sehingga
			 * boleh null) memakai {@code Boolean.TRUE.equals(...)}, ditambah pemeriksaan
			 * {@code getBankSoalDetail() != null}. Tanpa rantai penjagaan itu satu baris rusak
			 * akan melempar {@link NullPointerException} dan menggagalkan ekspor.</p>
			 *
			 * <p><b>Pembatasan panjang.</b> Kedua kolom teks dipotong
			 * {@code Common.maxPanjang(..., 20000)}. Batas ini wajib: sel Excel memiliki batas
			 * keras 32.767 karakter, dan soal esai panjang dari banyak butir dapat melampauinya
			 * sehingga menghasilkan berkas rusak.</p>
			 *
			 * <p><b>Ketahanan.</b> Kegagalan per detail jawaban maupun kegagalan seluruh baris
			 * ditangkap dan direkam ke {@code ErrorAuditUtil}; ekspor tetap berlanjut ke peserta
			 * berikutnya dengan sel yang bersangkutan dibiarkan kosong.</p>
			 *
			 * @param arg0 event pembawa {@code Object[]{hasilUjian, ?, XSSFRow}} dari mesin ekspor
			 * @throws Exception tidak dilempar dalam praktik — seluruh badan dibungkus try/catch
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) objects[0];
				XSSFRow row = (XSSFRow) objects[2];

				try {

					MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
							hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);

					int total = ujianPunyaSoals.size();
					Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
							.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

					StringBuilder sbJawaban = new StringBuilder();
					for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
						for (Long hasilUjianMahasiswaDetailid : aa) {
							try {
								HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
										.ambilData(HasilUjianMahasiswaDetail.class,
												hasilUjianMahasiswaDetailid.toString());
								if (hasilUjianMahasiswaDetail != null) {

									// Null-safe: getUjianPunyaSoal()/getUjian() bisa null, getTampilanHuruf... bisa Boolean
									// null, getBankSoalDetail() bisa null (lihat cek di bawah); tanpa guard = NPE per-baris.
									boolean tampilHuruf = hasilUjianMahasiswaDetail.getUjianPunyaSoal() != null
											&& hasilUjianMahasiswaDetail.getUjianPunyaSoal().getUjian() != null
											&& Boolean.TRUE.equals(hasilUjianMahasiswaDetail.getUjianPunyaSoal()
													.getUjian().getTampilanHurufDiPilihanJawaban());
									String h = (tampilHuruf && hasilUjianMahasiswaDetail.getBankSoalDetail() != null)
											? hasilUjianMahasiswaDetail.getBankSoalDetail().getHuruf() + ". "
											: "";

									String j = hasilUjianMahasiswaDetail.getBankSoalDetail() != null
											? (h + hasilUjianMahasiswaDetail.getBankSoalDetail().getJawaban())
											: hasilUjianMahasiswaDetail.getJawaban();

									if (!j.trim().isEmpty()) {
										j = hasilUjianMahasiswaDetail.getUjianPunyaSoal().getBankSoal().getSoal()
												+ ";JAWABAN:" + j + "\n\n";

										if (sbJawaban.length() > 0) sbJawaban.append("; ");
										sbJawaban.append(j);
										ujianPunyaSoals.remove(hasilUjianMahasiswaDetail.getUjianPunyaSoal().getId());
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:591");
							}
						}
					}

					int size = ujianPunyaSoals.size();
					row.createCell(contents.length).setCellValue(total - size);
					row.createCell(contents.length + 1).setCellValue(size);

					String jawabanFinal = Common.maxPanjang(sbJawaban.toString(), 20000);

					row.createCell(contents.length + 2).setCellValue(jawabanFinal);

					StringBuilder sbBelum = new StringBuilder();
					for (Long id : ujianPunyaSoals) {
						UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
								.ambilData(UjianPunyaSoal.class, id.toString());
						if (ujianPunyaSoal != null) {
							if (sbBelum.length() > 0) sbBelum.append("; ");
							sbBelum.append(ujianPunyaSoal.getBankSoal().getSoal());
						}
					}

					String belumFinal = Common.maxPanjang(sbBelum.toString(), 20000);

					row.createCell(contents.length + 3).setCellValue(belumFinal);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:618");
				}
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(HasilUjianMahasiswa.class,
				new DataCriteria() {

					/**
					 * Menyusun {@link Criteria} sumber data untuk ekspor Excel "Rekap Hasil
					 * Ujian": seluruh {@link HasilUjianMahasiswa} milik {@code pertemuanPunyaUjian}
					 * yang sudah memiliki {@code keyhasil}.
					 *
					 * <p><b>Arti {@code keyhasil} tidak null.</b> Kolom itu baru terisi setelah
					 * baris hasil ujian benar-benar terbentuk untuk peserta. Menyaringnya di sini
					 * mencegah baris kosong ikut terekspor.</p>
					 *
					 * <p><b>Pola {@code sqlRestriction("true")}.</b> Kedua penyaring peserta
					 * ({@code mahasiswa} dan {@code biodataCalonMahasiswa}) selalu di-{@code add},
					 * namun berubah menjadi kondisi {@code true} yang tidak menyaring apa pun
					 * ketika field-nya null. Cara ini menjaga bentuk query tetap tunggal tanpa
					 * percabangan, sekaligus membuat mode "satu peserta" dan mode "semua peserta"
					 * memakai jalur kode yang sama.</p>
					 *
					 * <p><b>Session.</b> Memakai {@code HibernateUtil.currentSession()} — session
					 * {@code ThreadLocal} milik request ZK, BUKAN session terdedikasi. Ini benar
					 * di sini karena mesin ekspor menjalankan Criteria pada thread request yang
					 * sama dan mengelola sendiri siklus hidup session tersebut.</p>
					 *
					 * <p><b>Parameter {@code order} diabaikan.</b> Berbeda dari
					 * {@link DataCriteria} lain di kelas ini, method ini tidak menambahkan
					 * {@code addOrder} sehingga urutan baris ekspor ditentukan basis data. Bila
					 * urutan yang stabil diperlukan (mis. untuk pembandingan antar-ekspor),
					 * tambahkan pengurutan di dalam cabang {@code order}.</p>
					 *
					 * @param order penanda apakah pengurutan diminta; tidak dipakai
					 * @return Criteria siap dieksekusi mesin ekspor
					 */
					@Override
					public Criteria initCriteria(boolean order) {
						Session session = HibernateUtil.currentSession();
						return session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
								.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("mahasiswa", mahasiswa))
								.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa));
					}
				}, "Rekap Hasil Ujian", "/img/print.png", columnHeadersAdding, dataAdding, contents);
		cetakToolbarbutton.setVisible(tbmuser != null && rakhasil);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Peserta dianggap hadir", "/img/svg/check2.svg");
		masuk.setVisible(pertemuan != null && tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		masuk.setTooltiptext("Tutup");
		masuk.setParent(toolbar);
		masuk.addEventListener("onClick", new EventListener() {
			/**
			 * Menjalankan aksi massal <b>"Peserta dianggap hadir"</b>: menandai presensi hadir
			 * bagi seluruh peserta yang mengikuti ujian ini, tanpa dosen perlu mengisi presensi
			 * satu per satu.
			 *
			 * <p>Seluruh pekerjaan (dialog konfirmasi, transaksi, penulisan presensi) didelegasikan
			 * ke {@link HasilUjianMahasiswaHelper#ujianDianggapHadir(PertemuanPunyaUjian, EventListener)}.
			 * Listener ini hanya menyediakan callback "sesudah selesai" yang membuka kembali layar
			 * pertemuan agar dosen dapat langsung memeriksa hasil presensinya.</p>
			 *
			 * <p><b>Visibilitas tombol</b> mensyaratkan {@code pertemuan} tidak null (presensi
			 * hanya bermakna pada pertemuan perkuliahan, bukan ujian PMB lepas) serta pengguna
			 * bukan mahasiswa/siswa/peserta kursus dan bukan mode satu peserta. Seperti tombol
			 * lain di toolbar ini, penjagaannya hanya di lapisan UI.</p>
			 *
			 * @param event event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembangunan dialog konfirmasi
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				HasilUjianMahasiswaHelper.ujianDianggapHadir(pertemuanPunyaUjian, new EventListener() {

					/**
					 * Callback sesudah seluruh presensi tertulis: membuka kembali layar pertemuan
					 * lewat {@code PertemuanHelper.display(...)} sehingga daftar presensi yang baru
					 * saja diperbarui langsung terlihat.
					 *
					 * <p>Argumen {@code mahasiswa}/{@code biodataCalonMahasiswa} diteruskan agar
					 * layar pertemuan tetap berada pada mode tampilan yang sama dengan layar rekap
					 * ini. Argumen terakhir {@code 0} adalah indeks tab awal.</p>
					 *
					 * @param arg0 event penanda selesai; tidak dipakai
					 * @throws Exception diteruskan dari pembangunan layar pertemuan
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

							/**
							 * Implementasi {@link DataLoader} <b>kosong yang disengaja</b>.
							 *
							 * <p>{@code PertemuanHelper.display(...)} mewajibkan sebuah
							 * {@code DataLoader} sebagai kait pemuatan ulang, tetapi pada konteks
							 * ini tidak ada yang perlu dimuat ulang: layar pertemuan baru saja
							 * dibangun dari awal sehingga sudah menampilkan data terkini. Badan
							 * kosong lebih tepat daripada meneruskan {@code null}, yang akan
							 * memicu {@link NullPointerException} di dalam helper tersebut.</p>
							 *
							 * @param value penanda refresh dari pemanggil; sengaja diabaikan
							 */
							@Override
							public void loadData(Object value) {

							}
						}, 0);
					}
				});

			}
		});

		if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {
			MyToolbarbuttonConfig koreksiAiPg = new MyToolbarbuttonConfig("Koreksi Otomatis via AI",
					"/img/svg/sparkles.svg");
			koreksiAiPg.setTooltiptext(
					"Isi kolom Koreksi/penjelasan SEMUA peserta via AI (pilihan ganda; skor tetap otomatis)");
			koreksiAiPg.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;");
			koreksiAiPg.addEventListener("onClick", new EventListener() {
				/**
				 * Menjalankan <b>"Koreksi Otomatis via AI"</b> untuk ujian PILIHAN GANDA:
				 * mengisi kolom Koreksi/penjelasan seluruh peserta memakai model bahasa,
				 * TANPA mengubah skor.
				 *
				 * <p><b>Perbedaan penting dari varian esai.</b> Pada pilihan ganda skor tetap
				 * dihitung otomatis oleh mesin penilaian; AI di sini hanya menuliskan penjelasan
				 * mengapa jawaban peserta benar/salah — nilai peserta tidak disentuh. Karena itu
				 * pesan penutupnya berbunyi "selesai dikoreksi (penjelasan)", berbeda dari varian
				 * esai yang menyebut "Nilai dihitung ulang". Konsekuensinya, fitur ini tidak dapat
				 * mengubah integritas nilai pilihan ganda.</p>
				 *
				 * <p><b>Tiga tahap.</b></p>
				 * <ol>
				 *   <li><b>Pengumpulan tugas di thread ZK.</b> Untuk setiap peserta pada
				 *       {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas},
				 *       {@code KoreksiHasilUjian.kumpulkanPg(hum)} mengumpulkan butir yang layak
				 *       dikoreksi; peserta tanpa butir dilewati. Prompt dibangun sekali per
				 *       peserta lewat {@code promptKoreksiPg(items, bangunKonteksUjian(hum))} dan
				 *       disimpan sebagai {@code Object[]{id, nama, prompt, items}}. Tahap ini
				 *       WAJIB berjalan di thread ZK karena pengumpulan butir menyentuh komponen
				 *       dan konteks session. Bila tidak ada tugas sama sekali, pengguna diberi
				 *       tahu dan proses berhenti.</li>
				 *   <li><b>Popup progres.</b> Sebuah {@code Window} 560px berisi label status,
				 *       {@code Progressmeter}, dan {@code Textbox} monospace yang menampilkan
				 *       aliran keluaran AI secara langsung.</li>
				 *   <li><b>Thread pemanggil AI + timer pemantau.</b> Lihat Javadoc
				 *       {@code run()} dan {@code onEvent(evtTimer)} di bawah.</li>
				 * </ol>
				 *
				 * <p><b>Wadah bersama.</b> {@code done[]}, {@code selesai[]}, {@code statusNow[]},
				 * dan {@code sink} adalah wadah {@code final} yang ditulis thread AI dan dibaca
				 * timer ZK. {@code sink} sengaja bertipe {@link StringBuffer} (bukan
				 * {@code StringBuilder}) karena method-methodnya tersinkronisasi — dua thread
				 * benar-benar mengaksesnya bersamaan.</p>
				 *
				 * @param event event {@code onClick}; tidak dipakai
				 * @throws Exception diteruskan dari pembangunan komponen popup
				 */
				@Override
				public void onEvent(Event event) throws Exception {
					final java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>();
					for (Object[] a : hasilUjianMahasiswas.values()) {
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) a[0];
						java.util.List<Object[]> items = KoreksiHasilUjian.kumpulkanPg(hum);
						if (items.isEmpty()) {
							continue;
						}
						String nama = "";
						try {
							if (hum.getMahasiswa() != null && hum.getMahasiswa().getNama() != null) {
								nama = hum.getMahasiswa().getNama();
							}
						} catch (Exception e) {
						}
						tugas.add(new Object[]{ hum.getId(), nama,
								KoreksiHasilUjian.promptKoreksiPg(items, KoreksiHasilUjian.bangunKonteksUjian(hum)),
								items });
					}
					if (tugas.isEmpty()) {
						MyMessageboxConfig.show("Tidak ada jawaban untuk dikoreksi.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					final org.zkoss.zul.Window win = new org.zkoss.zul.Window("Koreksi Otomatis via AI", "normal",
							false);
					win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					win.setWidth("560px");
					org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
					vb.setStyle("padding:16px;");
					vb.setHflex("1");
					vb.setParent(win);
					final org.zkoss.zul.Label statusLbl = new org.zkoss.zul.Label("Menyiapkan...");
					statusLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
					vb.appendChild(statusLbl);
					final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter(0);
					meter.setWidth("100%");
					vb.appendChild(meter);
					final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
					streamBox.setMultiline(true);
					streamBox.setReadonly(true);
					streamBox.setRows(8);
					streamBox.setHflex("1");
					streamBox.setStyle("width:100%;margin-top:10px;font-family:monospace;font-size:11px;");
					vb.appendChild(streamBox);
					win.doHighlighted();

					final int total = tugas.size();
					final int[] done = { 0 };
					final boolean[] selesai = { false };
					final StringBuffer sink = new StringBuffer();
					final String[] statusNow = { "" };

					new Thread(new Runnable() {
						/**
						 * Memanggil model AI <b>satu peserta pada satu waktu</b> (berurutan, bukan
						 * paralel) lalu menerapkan hasilnya sebagai teks koreksi/penjelasan.
						 *
						 * <p><b>Mengapa berurutan.</b> Berbeda dari "Hitung Ulang Semua" yang
						 * memakai kolam 50 thread, pemanggilan AI sengaja diserialkan: layanan AI
						 * berbatas laju (rate limit) dan berbiaya per panggilan, dan aliran
						 * keluaran ditampilkan langsung ke satu kotak teks yang akan menjadi
						 * campur aduk bila beberapa panggilan menulis bersamaan.</p>
						 *
						 * <p><b>Alur per iterasi.</b> Memperbarui {@code statusNow[0]} dengan
						 * nomor urut dan nama peserta, MENGOSONGKAN {@code sink} agar kotak
						 * aliran hanya memperlihatkan keluaran peserta yang sedang diproses,
						 * memanggil {@code GenerateAiHelper.panggilAi(prompt, sink, 2048)}
						 * (batas 2048 token), lalu {@code KoreksiHasilUjian.terapkanKoreksiPg}
						 * menuliskan hasilnya ke kolom koreksi. Pencacah {@code done[0]}
						 * dinaikkan SETELAH pemrosesan sehingga meter progres tidak pernah
						 * mendahului kenyataan.</p>
						 *
						 * <p><b>Ketahanan.</b> Kegagalan pada satu peserta ditangkap dan direkam
						 * ke {@code ErrorAuditUtil}; perulangan tetap berlanjut ke peserta
						 * berikutnya sehingga satu peserta bermasalah tidak membatalkan seluruh
						 * proses. Peserta yang gagal tidak ditandai di UI — periksa jejak audit
						 * bila ada penjelasan yang tidak terisi.</p>
						 *
						 * <p><b>Penanda selesai.</b> {@code selesai[0] = true} adalah SATU-SATUNYA
						 * sinyal yang membuat timer menutup popup. Ia diset di luar {@code try}
						 * per-iterasi sehingga selalu tercapai selama perulangan tidak dihentikan
						 * {@link Error} — bila terjadi, popup akan menggantung.</p>
						 */
						@Override
						@SuppressWarnings("unchecked")
						public void run() {
							for (int i = 0; i < tugas.size(); i++) {
								Object[] t = tugas.get(i);
								statusNow[0] = "Mengoreksi " + (i + 1) + "/" + total
										+ (((String) t[1]).length() > 0 ? " — " + t[1] : "");
								sink.setLength(0);
								try {
									String resp = GenerateAiHelper.panggilAi((String) t[2], sink, 2048);
									KoreksiHasilUjian.terapkanKoreksiPg((java.util.List<Object[]>) t[3], resp);
								} catch (Exception e) {
									ais.common.ErrorAuditUtil.record(e, "HasilUjianMahasiswaHelper.koreksiAiPg");
								}
								done[0] = i + 1;
							}
							selesai[0] = true;
						}
					}).start();

					final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(800);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.setRepeats(true);
					timer.addEventListener("onTimer", new EventListener() {
						/**
						 * Denyut pemantau progres koreksi AI pilihan ganda, dijalankan ZK setiap
						 * 800 ms. Merupakan <b>jembatan</b> antara thread AI (yang tidak boleh
						 * menyentuh komponen ZK) dan antarmuka pengguna.
						 *
						 * <p><b>Yang dilakukan setiap denyut.</b> Menghitung persen dari
						 * {@code done[0]} memakai aritmetika {@code long} ({@code done[0] * 100L})
						 * agar tidak meluap pada jumlah peserta besar; menyalin
						 * {@code statusNow[0]} ke label; dan menyalin {@code sink} ke kotak
						 * aliran hanya bila isinya BERBEDA — perbandingan ini penting agar ZK
						 * tidak mengirim pembaruan komponen 800 ms sekali tanpa perlu.</p>
						 *
						 * <p><b>Penanganan error sengaja senyap.</b> Seluruh pembaruan komponen
						 * dibungkus {@code try/catch} kosong: bila desktop ZK sudah dilepas
						 * (pengguna menutup tab) pembaruan akan melempar, dan menampilkan galat
						 * untuk itu tidak berguna. Pemeriksaan {@code selesai[0]} sengaja berada
						 * DI LUAR {@code try} agar penutupan popup tetap berjalan meski satu
						 * pembaruan tampilan gagal.</p>
						 *
						 * <p><b>Saat selesai.</b> Timer dihentikan dan dilepas, popup ditutup,
						 * {@code loadData(true)} memuat ulang grid dengan penanda refresh, lalu
						 * pesan ringkasan ditampilkan. Perhatikan bahwa jumlah yang dilaporkan
						 * adalah jumlah tugas, bukan jumlah yang benar-benar berhasil — peserta
						 * yang gagal dikoreksi tetap ikut terhitung.</p>
						 *
						 * @param evtTimer event {@code onTimer}; tidak dipakai
						 * @throws Exception diteruskan dari pemuatan ulang grid atau messagebox
						 */
						@Override
						public void onEvent(Event evtTimer) throws Exception {
							try {
								meter.setValue(total > 0 ? (int) (done[0] * 100L / total) : 100);
								statusLbl.setValue(statusNow[0]);
								String cur = sink.toString();
								if (!cur.equals(streamBox.getValue())) {
									streamBox.setValue(cur);
								}
							} catch (Exception ig) {
							}
							if (selesai[0]) {
								timer.stop();
								timer.detach();
								win.detach();
								loadData(true);
								MyMessageboxConfig.show(total + " peserta selesai dikoreksi (penjelasan) via AI.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
					timer.start();
				}
			});
			koreksiAiPg.setParent(toolbar);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hitung Ulang Semua", "/img/svg/check2-circle.svg");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				/**
				 * Menjalankan <b>"Hitung Ulang Semua"</b> untuk ujian PILIHAN GANDA: menghitung
				 * ulang nilai OBE dan nilai pilihan ganda SELURUH peserta yang sedang termuat di
				 * grid, secara paralel di latar.
				 *
				 * <p>Listener ini hanya menyiapkan bilah pemuatan lalu melepas satu thread
				 * koordinator. Callback bilah pemuatan (yang dijalankan ZK setelah nilai label
				 * dikosongkan thread koordinator) memuat ulang grid dengan
				 * {@code loadData(true)}.</p>
				 *
				 * <p><b>Cakupan.</b> Yang diproses adalah isi
				 * {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas} — yaitu peserta yang
				 * sedang termuat di grid, bukan seluruh isi database. Bila kotak pencarian sedang
				 * menyaring nama, hanya peserta hasil saringan itulah yang dihitung ulang.</p>
				 *
				 * <p><b>Otorisasi.</b> Tombol pemicu tidak diberi {@code setVisible(...)}
				 * bersyarat seperti tombol destruktif lain di toolbar ini, dan listener tidak
				 * memeriksa peran. Karena aksi ini MENULIS kolom nilai seluruh peserta,
				 * perlindungannya sepenuhnya bergantung pada kelayakan pemanggil
				 * {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)}.</p>
				 *
				 * @param arg0 event {@code onClick}; tidak dipakai
				 * @throws Exception diteruskan dari pembuatan bilah pemuatan
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						/**
						 * Callback bilah pemuatan: dijalankan pada thread ZK setelah thread
						 * koordinator mengosongkan nilai label. Memuat ulang grid dengan penanda
						 * refresh {@code true} agar himpunan soal terjawab dan angka nilai dibaca
						 * ulang, bukan diambil dari cache yang baru saja menjadi usang.
						 *
						 * @param arg0 event penanda selesai; tidak dipakai
						 * @throws Exception diteruskan dari pemuatan ulang grid
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);
						}
					});

					new Thread(new Runnable() {

						/**
						 * Thread <b>koordinator</b> hitung ulang paralel. Tugasnya menyiapkan
						 * prasyarat bersama, menyebar satu tugas per peserta ke kolam thread,
						 * menunggu seluruhnya selesai, lalu mengosongkan label bilah pemuatan.
						 *
						 * <p><b>Mengapa paralel.</b> Sebelumnya seluruh peserta diproses berurutan
						 * dalam satu thread dan satu session, sangat lambat untuk ujian berpeserta
						 * banyak. Kini tiap peserta diproses pada thread DAN session Hibernate
						 * sendiri lewat {@code Executors.newFixedThreadPool(DbThreadPool.safe(50))}.
						 * {@code DbThreadPool.safe} membatasi ukuran kolam terhadap kapasitas
						 * kolam koneksi database sehingga tidak terjadi kelaparan koneksi.</p>
						 *
						 * <p><b>Prasyarat yang WAJIB dihitung sekali di muka:</b>
						 * {@code formatNilaisPreComputed}. Bila setiap thread memanggil
						 * {@code Common.getFormatNilais()} sendiri-sendiri,
						 * {@code setDefaultPembobotan()} dari 50 thread akan saling me-reset
						 * persentase menjadi 0 sehingga sebagian thread memperoleh
						 * {@link FormatNilai} kosong dan {@code nilaiObe} hanya terisi satu
						 * sub-CPMK. Pemuatan di muka memakai {@code ambilFormatNilai(sesPre, true)}
						 * dengan argumen refresh {@code true} yang WAJIB: penanda
						 * {@code udah("format_nilai_baru")} sudah diset saat {@code loadData()},
						 * sehingga tanpa refresh {@code setDefaultPembobotan} dilewati dan
						 * {@code statusPertemuan} yang null tidak diperbaiki — akibatnya
						 * {@code ambilMapNomor} melewatkan sub-CPMK tersebut. Session sementara
						 * {@code sesPre} ditutup segera setelah dipakai.</p>
						 *
						 * <p><b>Penyelesaian.</b> {@code executor.shutdown()} lalu
						 * {@code awaitTermination(Long.MAX_VALUE, NANOSECONDS)} — menunggu tanpa
						 * batas waktu; interupsi hanya mengembalikan penanda interrupt pada thread
						 * ini. Setelah itu label dikosongkan sehingga callback ZK berjalan.
						 * Berbeda dari thread analisis butir soal, pengosongan label di sini
						 * berada DI LUAR {@code try} sehingga bilah pemuatan tetap hilang meski
						 * terjadi kegagalan.</p>
						 */
						@Override
						public void run() {
							try {
								final int total = hasilUjianMahasiswas.size();
								final java.util.concurrent.atomic.AtomicInteger diproses =
										new java.util.concurrent.atomic.AtomicInteger(0);

								// ====== HITUNG ULANG PARALEL (50 proses) ======
								// Sebelumnya semua peserta diproses BERURUTAN dalam SATU thread/session →
								// lambat untuk ujian dg banyak peserta. Kini tiap peserta diproses di thread
								// & session Hibernate SENDIRI (aman untuk paralel) memakai kolam terkendali
								// DbThreadPool.safe(50). Instance TERKELOLA diambil ulang via session.get(id)
								// pada session milik masing-masing thread (tidak memakai objek grid bersama).

								// Pre-compute formatNilais SEKALI sebelum loop thread.
								// Race condition: bila setiap thread memanggil Common.getFormatNilais()
								// bersamaan, setDefaultPembobotan() dari 50 thread saling reset persen=0 →
								// sebagian thread mendapat FormatNilai kosong → nilaiObe hanya terisi 1 sub-CPMK.
								//
								// refresh=true WAJIB: flag udah("format_nilai_baru") di-set saat loadData()
								// sehingga getFormatNilais(sesPre, perkuliahan) (refresh=false) melewati
								// setDefaultPembobotan → FormatNilai dengan statusPertemuan=null (akibat bug
								// lama ambilByNama session-close) tidak diperbaiki → ambilMapNomor melewati
								// sub-CPMK tersebut → nilaiObe hanya berisi sub-CPMK yang sudah punya
								// statusPertemuan (biasanya hanya 1.01). Dengan refresh=true,
								// setDefaultPembobotan selalu dipanggil dan memperbaiki statusPertemuan
								// in-memory → semua sub-CPMK masuk treeMap → nilaiObe lengkap.
								final List<FormatNilai> formatNilaisPreComputed;
								{
									Session sesPre = null;
									List<FormatNilai> tmp = new ArrayList<FormatNilai>();
									try {
										sesPre = HibernateUtil.getSessionFactory().openSession();
										tmp = pertemuanPunyaUjian.getPertemuan().getPerkuliahan().ambilFormatNilai(sesPre, true);
									} catch (Exception eFormatNilai) {
										System.out.println("[HITUNG-ULANG-PRE-FORMAT] gagal pre-compute formatNilais: " + eFormatNilai);
										eFormatNilai.printStackTrace(); ais.common.ErrorAuditUtil.record(eFormatNilai, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:719");
									} finally {
										if (sesPre != null && sesPre.isOpen()) try { sesPre.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:721");}
									}
									formatNilaisPreComputed = tmp;
								}

								java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
										.newFixedThreadPool(ais.common.DbThreadPool.safe(50));

								for (final Object[] a : hasilUjianMahasiswas.values()) {
									executor.submit(new Runnable() {
										/**
										 * Tugas hitung ulang untuk <b>satu peserta</b>, dijalankan
										 * pada thread dan session Hibernate miliknya sendiri.
										 *
										 * <p><b>Isolasi wajib.</b> Objek {@link HasilUjianMahasiswa}
										 * yang tersimpan pada map grid TIDAK dipakai untuk menulis.
										 * Hanya id-nya yang diambil, lalu instance TERKELOLA
										 * diperoleh ulang dengan {@code session.get(...)} pada
										 * session milik thread ini. Tanpa isolasi ini, 50 thread
										 * akan berbagi objek entity yang sama dan saling merusak
										 * keadaan dirty-check Hibernate.</p>
										 *
										 * <p><b>Urutan perhitungan.</b> Menyegarkan
										 * {@code jumlahSoal} dari konfigurasi ujian, memanggil
										 * {@code ProsesUjianHelper.hitungObe(...)} dengan
										 * {@code formatNilaisPreComputed} bersama (aman karena
										 * hanya dibaca), lalu {@code hitungPilihanGanda(...)}.
										 * Kegagalan {@code hitungPilihanGanda} SENGAJA ditelan
										 * ke jejak audit dan tidak melempar keluar, karena
										 * membiarkannya membatalkan transaksi akan ikut membuang
										 * {@code nilaiObe} yang sudah benar dihitung sebelumnya.</p>
										 *
										 * <p><b>Setelah commit.</b> {@code tx} di-null-kan agar
										 * blok {@code catch} tidak mencoba me-rollback transaksi
										 * yang telah selesai, lalu cache MapDB disegarkan lewat
										 * {@code GeneralValueObject.masukkanDataLangsung(...)}
										 * berkunci {@code keyhasil} supaya tampilan tidak basi.
										 * Kegagalan penyegaran cache tidak membatalkan
										 * keberhasilan simpan.</p>
										 *
										 * <p><b>Blok {@code finally}.</b> Menaikkan pencacah atomik
										 * {@code diproses} dan memperbarui label progres
										 * ({@link java.util.concurrent.atomic.AtomicInteger} wajib
										 * di sini karena dinaikkan 50 thread). Pembaruan label
										 * dibungkus {@code try/catch} sendiri agar galat UI di
										 * luar Desktop tidak mematikan thread. Session ditutup
										 * bertahap {@code clear} &rarr; {@code disconnect} &rarr;
										 * {@code close}, masing-masing dalam try/catch terpisah.</p>
										 *
										 * <p><b>Ketahanan.</b> Peserta yang gagal di-rollback dan
										 * dicatat; peserta lain tidak terpengaruh karena tiap tugas
										 * punya transaksi sendiri. Tugas yang datanya cacat
										 * ({@code a} null/pendek, id null, entity tidak ditemukan)
										 * langsung {@code return} — namun {@code finally} tetap
										 * berjalan sehingga pencacah progres tetap akurat.</p>
										 */
										@Override
										public void run() {
											Session session = null;
											Transaction tx = null;
											try {
												HasilUjianMahasiswa asli = (a == null || a.length < 1) ? null
														: (HasilUjianMahasiswa) a[0];
												if (asli == null || asli.getId() == null) {
													return;
												}
												session = HibernateUtil.getSessionFactory().openSession();
												HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
														.get(HasilUjianMahasiswa.class, asli.getId());
												if (hasilUjianMahasiswa == null) {
													return;
												}

												tx = session.beginTransaction();

												MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
														hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
														new Label(), true);
												Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
														.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
																ujianPunyaSoals, false);

												hasilUjianMahasiswa.setJumlahSoal(pertemuanPunyaUjian.getJmlDitampilkan() == null
														? 0.0 : pertemuanPunyaUjian.getJmlDitampilkan().doubleValue());

												ProsesUjianHelper.hitungObe(hasilUjianMahasiswa, hasilUjianMahasiswaDetails, formatNilaisPreComputed);
												System.out.println("[HITUNG-ULANG] peserta=" + hasilUjianMahasiswa.getId()
														+ " jmlJawaban=" + (hasilUjianMahasiswaDetails == null ? 0
																: hasilUjianMahasiswaDetails.size())
														+ " nilaiObe=" + hasilUjianMahasiswa.getNilaiObe());

												// Kegagalan nilai pilihan ganda TIDAK BOLEH me-rollback nilaiObe.
												try {
													ProsesUjianHelper.hitungPilihanGanda(hasilUjianMahasiswa,
															hasilUjianMahasiswaDetails);
												} catch (Exception ePg) {
													System.out.println("[HITUNG-ULANG-PG-ERROR] peserta="
															+ hasilUjianMahasiswa.getId() + " -> " + ePg);
													ePg.printStackTrace(); ais.common.ErrorAuditUtil.record(ePg, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:773");
												}
												hasilUjianMahasiswaDetails = null;

												session.update(hasilUjianMahasiswa);
												tx.commit();
												tx = null;

												// Segarkan cache MapDB (ambilByKey) agar tampilan tak basi.
												try {
													GeneralValueObject.masukkanDataLangsung(HasilUjianMahasiswa.class,
															hasilUjianMahasiswa, hasilUjianMahasiswa.getKeyhasil());
												} catch (Exception eCache) {
													eCache.printStackTrace(); ais.common.ErrorAuditUtil.record(eCache, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:786");
												}
											} catch (Exception e) {
												if (tx != null) {
													try { tx.rollback(); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:790");}
												}
												System.out.println("[HITUNG-ULANG-ROLLBACK] peserta gagal diproses -> " + e);
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:793");
											} finally {
												int cur = diproses.incrementAndGet();
												try {
													if (label != null) {
														label.setValue("Menghitung ulang " + cur + " dari " + total + " ("
																+ Common.numberFormat.get().format(cur * 100.0 / total) + " %)");
													}
												} catch (Exception exUi) { ais.common.ErrorAuditUtil.record(exUi, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:801");
												}
												if (session != null) {
													try { if (session.isOpen()) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:804");}
													try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:805");}
													try { if (session.isOpen()) session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:806");}
												}
											}
										}
									});
								}

								executor.shutdown();
								try {
									executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
								} catch (InterruptedException ie) {
									Thread.currentThread().interrupt();
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:820");
							}

							label.setValue("");
						}
					}).start();

				}
			});

			toolbar.appendChild(HasilUjianMahasiswaHelper.analsisButirSoal(pertemuanPunyaUjian, new Ambildata() {

				/**
				 * Penyedia data peserta untuk Analisis Butir Soal.
				 *
				 * <p>Mengembalikan {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas} sebagai
				 * referensi HIDUP, bukan salinan. Dievaluasi saat tombol diklik — bukan saat
				 * tombol dibuat — sehingga selalu memperoleh isi map terbaru hasil
				 * {@link HasilUjianMahasiswaHelper#loadData(Object)}. Pola pemanggilan tertunda
				 * inilah alasan {@code analsisButirSoal} menerima {@link Ambildata} alih-alih
				 * langsung menerima {@code Map}.</p>
				 *
				 * <p><b>Perhatian:</b> {@code analsisButirSoal} tidak menjaga hasil {@code null}.
				 * Selama method ini dipanggil setelah {@code loadData}, map tidak pernah null.</p>
				 *
				 * @return {@code Map<Long, Object[]>} hasil ujian seluruh peserta yang termuat
				 */
				@Override
				public Object ambil() {
					return hasilUjianMahasiswas;
				}
			}, new Ambildata() {

				/**
				 * Penyedia jumlah peserta TERDAFTAR untuk kartu "Peserta Ujian" pada dashboard
				 * Analisis Butir Soal.
				 *
				 * <p>Mengembalikan {@link HasilUjianMahasiswaHelper#jumlahPeserta} agar angka
				 * pada dashboard SAMA dengan "Jumlah Peserta" di tab Statistik. Tanpa penyedia
				 * ini, {@code analsisButirSoal} akan memakai ukuran map hasil ujian yang hanya
				 * mencakup peserta yang punya baris hasil — angka yang lebih kecil dan
				 * membingungkan bila dibandingkan antar-tab.</p>
				 *
				 * <p>Seperti penyedia di atasnya, nilainya dibaca saat tombol diklik sehingga
				 * sudah terisi hasil {@code loadData}. Dibungkus {@link Integer} karena
				 * {@code analsisButirSoal} memeriksanya dengan {@code instanceof Number} dan
				 * hanya memakainya bila {@code > 0}.</p>
				 *
				 * @return jumlah peserta terdaftar sebagai {@link Integer}
				 */
				@Override
				public Object ambil() {
					// Samakan "Peserta Ujian" di dashboard dengan "Jumlah Peserta" di tab Statistik
					// (jumlah peserta terdaftar). Dibaca saat tombol diklik → nilainya sudah terisi.
					return Integer.valueOf(HasilUjianMahasiswaHelper.this.jumlahPeserta);
				}
			}));

		} else {
			MyToolbarbuttonConfig koreksiAiSemua = new MyToolbarbuttonConfig("Koreksi Otomatis via AI",
					"/img/svg/sparkles.svg");
			koreksiAiSemua.setTooltiptext(
					"Koreksi otomatis SEMUA peserta essay via AI (isi Skor & Koreksi) lalu hitung ulang");
			koreksiAiSemua.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;");
			koreksiAiSemua.addEventListener("onClick", new EventListener() {
				/**
				 * Menjalankan <b>"Koreksi Otomatis via AI"</b> untuk ujian ESAI: mengisi Skor DAN
				 * teks Koreksi seluruh peserta memakai model bahasa, lalu menghitung ulang nilai.
				 *
				 * <p><b>Perbedaan mendasar dari varian pilihan ganda.</b> Pada esai tidak ada
				 * kunci jawaban yang dapat dicocokkan otomatis, sehingga AI di sini menetapkan
				 * SKOR — bukan sekadar penjelasan. Ini menjadikan fitur ini satu-satunya jalur di
				 * kelas ini yang menyerahkan penentuan nilai kepada model bahasa. Konsekuensinya
				 * untuk integritas nilai perlu disadari: nilai yang dihasilkan tidak deterministik
				 * antar-pemanggilan dan tidak dapat direproduksi persis. Dosen tetap dapat
				 * menimpanya lewat editor Nilai per baris pada grid.</p>
				 *
				 * <p><b>Tiga tahap</b> sama seperti varian pilihan ganda: pengumpulan tugas di
				 * thread ZK memakai {@code KoreksiHasilUjian.kumpulkanEssay(hum)} dan
				 * {@code promptKoreksiEssay(items, bangunKonteksUjian(hum))}; popup progres
				 * streaming; lalu thread AI berurutan dengan timer pemantau 800 ms. Peserta tanpa
				 * jawaban esai dilewati, dan bila tidak ada tugas sama sekali pengguna diberi tahu
				 * lalu proses berhenti.</p>
				 *
				 * <p><b>Otorisasi.</b> Tombol pemicu tidak diberi {@code setVisible(...)}
				 * bersyarat dan listener tidak memeriksa peran, padahal aksinya menulis kolom
				 * nilai. Perlindungannya bersandar pada kelayakan pemanggil
				 * {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)}.</p>
				 *
				 * @param event event {@code onClick}; tidak dipakai
				 * @throws Exception diteruskan dari pembangunan komponen popup
				 */
				@Override
				public void onEvent(Event event) throws Exception {
					// 1) Kumpulkan tugas (di thread ZK, butuh komponen Label): {humId, nama, prompt, items}
					final java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>();
					for (Object[] a : hasilUjianMahasiswas.values()) {
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) a[0];
						java.util.List<Object[]> items = KoreksiHasilUjian.kumpulkanEssay(hum);
						if (items.isEmpty()) {
							continue;
						}
						String nama = "";
						try {
							if (hum.getMahasiswa() != null && hum.getMahasiswa().getNama() != null) {
								nama = hum.getMahasiswa().getNama();
							}
						} catch (Exception e) {
						}
						tugas.add(new Object[]{ hum.getId(), nama, KoreksiHasilUjian.promptKoreksiEssay(items, KoreksiHasilUjian.bangunKonteksUjian(hum)), items });
					}
					if (tugas.isEmpty()) {
						MyMessageboxConfig.show("Tidak ada jawaban essay untuk dikoreksi.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					// 2) Popup progres streaming
					final org.zkoss.zul.Window win = new org.zkoss.zul.Window("Koreksi Otomatis via AI", "normal",
							false);
					win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					win.setWidth("560px");
					org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
					vb.setStyle("padding:16px;");
					vb.setHflex("1");
					vb.setParent(win);
					final org.zkoss.zul.Label statusLbl = new org.zkoss.zul.Label("Menyiapkan...");
					statusLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
					vb.appendChild(statusLbl);
					final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter(0);
					meter.setWidth("100%");
					vb.appendChild(meter);
					final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
					streamBox.setMultiline(true);
					streamBox.setReadonly(true);
					streamBox.setRows(8);
					streamBox.setHflex("1");
					streamBox.setStyle("width:100%;margin-top:10px;font-family:monospace;font-size:11px;");
					vb.appendChild(streamBox);
					win.doHighlighted();

					final int total = tugas.size();
					final int[] done = { 0 };
					final boolean[] selesai = { false };
					final StringBuffer sink = new StringBuffer();
					final String[] statusNow = { "" };

					new Thread(new Runnable() {
						/**
						 * Memanggil model AI satu peserta pada satu waktu (berurutan) untuk
						 * menetapkan skor dan teks koreksi jawaban ESAI.
						 *
						 * <p>Bentuknya identik dengan varian pilihan ganda — pembaruan
						 * {@code statusNow[0]}, pengosongan {@code sink} sebelum tiap panggilan,
						 * batas 2048 token, kegagalan per peserta ditelan ke {@code ErrorAuditUtil}
						 * agar perulangan berlanjut, dan {@code done[0]} dinaikkan setelah
						 * pemrosesan — dengan satu perbedaan penting: penerapan hasil memakai
						 * {@code terapkanKoreksiEssay(items, resp, humId)} yang menerima
						 * <b>argumen ketiga berupa id {@link HasilUjianMahasiswa}</b>.</p>
						 *
						 * <p>Argumen id itulah yang membuat varian esai dapat MENGHITUNG ULANG
						 * NILAI peserta setelah skor per butir ditulis; varian pilihan ganda tidak
						 * memerlukannya karena hanya menyentuh teks penjelasan. Itu pula sebabnya
						 * pesan penutup varian ini menyebut "Nilai dihitung ulang".</p>
						 *
						 * <p><b>Konkurensi.</b> Thread ini menulis {@code statusNow}, {@code sink},
						 * {@code done}, dan {@code selesai} yang dibaca timer ZK. Tidak ada kunci
						 * eksplisit — konsistensi bersandar pada sifat atomik penulisan referensi
						 * dan {@code int}, serta pada {@link StringBuffer} yang tersinkronisasi.
						 * Timer paling buruk hanya menampilkan status yang tertinggal satu denyut.</p>
						 */
						@Override
						@SuppressWarnings("unchecked")
						public void run() {
							for (int i = 0; i < tugas.size(); i++) {
								Object[] t = tugas.get(i);
								statusNow[0] = "Mengoreksi " + (i + 1) + "/" + total
										+ (((String) t[1]).length() > 0 ? " — " + t[1] : "");
								sink.setLength(0);
								try {
									String resp = GenerateAiHelper.panggilAi((String) t[2], sink, 2048);
									KoreksiHasilUjian.terapkanKoreksiEssay((java.util.List<Object[]>) t[3], resp,
											(Long) t[0]);
								} catch (Exception e) {
									ais.common.ErrorAuditUtil.record(e,
											"HasilUjianMahasiswaHelper.koreksiAiSemua");
								}
								done[0] = i + 1;
							}
							selesai[0] = true;
						}
					}).start();

					// 3) Timer pantau progres → update popup
					final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(800);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.setRepeats(true);
					timer.addEventListener("onTimer", new EventListener() {
						/**
						 * Denyut pemantau progres koreksi AI ESAI, dijalankan ZK setiap 800 ms.
						 *
						 * <p>Perilakunya identik dengan pemantau varian pilihan ganda: menghitung
						 * persen memakai aritmetika {@code long} agar tidak meluap, menyalin
						 * status, memperbarui kotak aliran HANYA bila isinya berubah, dan
						 * membungkus seluruh pembaruan komponen dalam {@code try/catch} kosong
						 * karena desktop ZK bisa sudah dilepas. Pemeriksaan {@code selesai[0]}
						 * berada di luar {@code try} agar penutupan popup tetap terjadi.</p>
						 *
						 * <p><b>Yang berbeda:</b> pesan penutupnya menyebut "Nilai dihitung ulang"
						 * — pada esai, {@code terapkanKoreksiEssay} memang menuliskan skor dan
						 * memicu perhitungan ulang nilai, sedangkan varian pilihan ganda hanya
						 * menulis penjelasan. Setelah popup ditutup, {@code loadData(true)} memuat
						 * ulang grid sehingga nilai baru langsung terlihat.</p>
						 *
						 * @param evtTimer event {@code onTimer}; tidak dipakai
						 * @throws Exception diteruskan dari pemuatan ulang grid atau messagebox
						 */
						@Override
						public void onEvent(Event evtTimer) throws Exception {
							try {
								meter.setValue(total > 0 ? (int) (done[0] * 100L / total) : 100);
								statusLbl.setValue(statusNow[0]);
								String cur = sink.toString();
								if (!cur.equals(streamBox.getValue())) {
									streamBox.setValue(cur);
								}
							} catch (Exception ig) {
							}
							if (selesai[0]) {
								timer.stop();
								timer.detach();
								win.detach();
								loadData(true);
								MyMessageboxConfig.show(
										total + " peserta selesai dikoreksi via AI. Nilai dihitung ulang.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
					timer.start();
				}
			});
			koreksiAiSemua.setParent(toolbar);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hitung Ulang Semua",
					"/img/Button-Refresh-icon.png");
			cari.setTooltiptext("Hitung Ulang Semua");
			cari.addEventListener("onClick", new EventListener() {
				/**
				 * Menjalankan <b>"Hitung Ulang Semua"</b> untuk ujian NON-pilihan-ganda (esai dan
				 * sejenisnya): menghitung ulang nilai seluruh peserta yang termuat di grid dari
				 * skor per butir yang telah dikoreksi.
				 *
				 * <p><b>Berbeda jauh dari varian pilihan ganda.</b> Varian ini TIDAK memakai kolam
				 * thread; seluruh peserta diproses berurutan dalam SATU thread dan SATU session,
				 * dengan transaksi terpisah per peserta. Pilihan itu masuk akal untuk esai:
				 * jumlah peserta yang perlu dihitung ulang biasanya jauh lebih kecil karena hanya
				 * yang sudah dikoreksi yang bernilai, dan perhitungannya jauh lebih ringan
				 * (satu query agregat, bukan pemuatan seluruh detail jawaban).</p>
				 *
				 * <p>Listener ini menyiapkan bilah pemuatan berikut callback yang memanggil
				 * {@code loadData(true)}, lalu melepas thread pekerja.</p>
				 *
				 * @param event event {@code onClick}; tidak dipakai
				 * @throws Exception diteruskan dari pembuatan bilah pemuatan
				 */
				@Override
				public void onEvent(Event event) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						/**
						 * Callback bilah pemuatan: dijalankan pada thread ZK setelah thread
						 * pekerja mengosongkan label, memuat ulang grid dengan penanda refresh
						 * {@code true} agar nilai baru terlihat.
						 *
						 * @param arg0 event penanda selesai; tidak dipakai
						 * @throws Exception diteruskan dari pemuatan ulang grid
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);

						}
					});

					new Thread(new Runnable() {

						/**
						 * Thread pekerja hitung ulang nilai untuk ujian non-pilihan-ganda.
						 *
						 * <p><b>Alur per peserta</b> (berurutan, satu transaksi masing-masing):</p>
						 * <ol>
						 *   <li>{@code session.refresh(hasilUjianMahasiswa)} menyegarkan objek yang
						 *       diambil dari map grid agar mencerminkan keadaan database terkini.</li>
						 *   <li>Bila kurikulum perkuliahan ber-OBE (rantai penjagaan null berlapis
						 *       sampai {@code apakahObe(tahunAjaran, ganjilGenap)}), detail jawaban
						 *       dimuat lalu {@code ProsesUjianHelper.hitungObe(...)} memperbarui
						 *       {@code nilaiObe}. Berbeda dari varian pilihan ganda, di sini
						 *       {@code formatNilais} TIDAK diserahkan sehingga
						 *       {@code hitungObe} mengambilnya sendiri — aman karena thread hanya
						 *       satu, sehingga masalah saling-reset {@code setDefaultPembobotan}
						 *       yang mendera varian paralel tidak muncul.</li>
						 *   <li>Nilai pokok dihitung lewat SATU query proyeksi yang mengambil
						 *       pasangan {@code (nilai, bankSoal.skor)} seluruh detail jawaban,
						 *       lalu {@code sumNilai = Σ (nilai * 100 / skor)} dan nilai akhir
						 *       {@code sumNilai / jumlahDetail}. Perhatikan bahwa penyebutnya
						 *       adalah jumlah baris DETAIL yang terbaca, bukan jumlah soal ujian:
						 *       soal yang sama sekali tidak dijawab tidak menghasilkan baris detail
						 *       sehingga tidak menurunkan rata-rata. Rumus ini juga BERBEDA dari
						 *       {@code ProsesUjianHelper.hitungPilihanGanda}.</li>
						 *   <li>Nilai hanya ditulis bila {@code sumNilai > 0.1} — ambang penjaga
						 *       agar peserta yang belum dikoreksi tidak tertimpa nilai 0.</li>
						 * </ol>
						 *
						 * <p><b>Pembagian nol.</b> {@code skor} soal yang bernilai 0 menghasilkan
						 * {@code Infinity} pada {@code (nilai * 100.0) / skor} — bukan exception.
						 * Nilai yang tercemar {@code Infinity} akan lolos ambang {@code > 0.1} dan
						 * tersimpan. Periksa skor soal di bank soal bila menemukan nilai janggal.</p>
						 *
						 * <p><b>Sumber daya.</b> Satu session dipakai bersama seluruh peserta,
						 * dengan {@code flush()} + {@code clear()} setiap 50 peserta dan ditutup
						 * di {@code finally}. Kegagalan per peserta di-rollback dan dicatat;
						 * perulangan berlanjut. Label dikosongkan di akhir, di luar {@code try},
						 * sehingga bilah pemuatan selalu hilang.</p>
						 */
						@Override
						public void run() {

							Session session = null;
							Transaction tx = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								
								int rowIndex = 1;
								int count = 0;
								for (Object[] a : hasilUjianMahasiswas.values()) {
									
									try {
										tx = session.beginTransaction();
										
										HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
										session.refresh(hasilUjianMahasiswa);

										if (hasilUjianMahasiswa.getPertemuanPunyaUjian() != null
												&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan() != null
												&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan() != null
												&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum() != null
												&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
																hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getTahunAjaran(),
																hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getGanjilGenap())) {

											MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
													hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
													new Label(), true);
											Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
													.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
															ujianPunyaSoals, false);

											ProsesUjianHelper.hitungObe(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);
										}

										@SuppressWarnings("unchecked")
										List<Object[]> sumNilais = session.createCriteria(HasilUjianMahasiswaDetail.class)
												.createAlias("bankSoal", "bankSoal")
												.add(Restrictions.eq("hasilUjianMahasiswa", hasilUjianMahasiswa))
												.setProjection(
														Projections.projectionList().add(Projections.property("nilai"))
																.add(Projections.property("bankSoal.skor")))
												.list();

										Double sumNilai = 0.0;
										for (Object[] o : sumNilais) {
											Double nilai = ((Number) o[0]).doubleValue();
											Double skor = ((Number) o[1]).doubleValue();
											sumNilai += (nilai * 100.0) / skor;
										}

										System.out.println("sumNilais = " + sumNilais + ", sumNilai = " + sumNilai);

										if (sumNilai != null && sumNilai.doubleValue() > 0.1) {

											Double n = sumNilai.doubleValue() / sumNilais.size();
											hasilUjianMahasiswa.setNilai(n);

											session.update(hasilUjianMahasiswa);
										}
										tx.commit();
										
										label.setValue("Sedang memproses data " + hasilUjianMahasiswa.toString() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / hasilUjianMahasiswas.size())
												+ " %)");

										count++;
										if (count % 50 == 0) {
											session.flush();
											session.clear();
										}
									} catch (Exception e) {
										if (tx != null) tx.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:939");
									}
									rowIndex++;
								}
							} finally {
								if (session != null && session.isOpen()) {
									try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:945");}
								}
							}

							label.setValue("");
						}
					}).start();

				}

			});
			cari.setParent(toolbar);
		}

		if (pertemuanPunyaUjian.getPertemuan() != null && pertemuanPunyaUjian.getPertemuan().getPerkuliahan() != null
				&& pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getKurikulum() != null
				&& pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
						pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getTahunAjaran(),
						pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getGanjilGenap())) {
			toolbar.appendChild(HasilUjianMahasiswaHelper.hasilObe(pertemuanPunyaUjian, new Ambildata() {

				/**
				 * Penyedia data peserta untuk laporan <b>Hasil OBE</b>.
				 *
				 * <p>Sama seperti penyedia untuk Analisis Butir Soal, mengembalikan
				 * {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas} sebagai referensi hidup
				 * yang dievaluasi saat tombol diklik. Thread latar {@code hasilObe} membacanya
				 * untuk menyusun tabel capaian per CPMK/Sub-CPMK dan berkas Excel-nya.</p>
				 *
				 * <p>Tombol pemanggilnya hanya ditambahkan ke toolbar bila kurikulum perkuliahan
				 * ber-OBE, sehingga penyedia ini tidak pernah dibuat pada perkuliahan non-OBE.</p>
				 *
				 * @return {@code Map<Long, Object[]>} hasil ujian seluruh peserta yang termuat
				 */
				@Override
				public Object ambil() {
					return hasilUjianMahasiswas;
				}
			}));
		}

		button = new MyToolbarbuttonConfig("Download Lampiran", FileFoto.icon(null));
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			/**
			 * Memicu aksi <b>"Download Lampiran"</b>: mengumpulkan seluruh berkas lampiran
			 * jawaban peserta menjadi satu arsip ZIP lalu mengunduhkannya.
			 *
			 * <p>Listener ini tidak melakukan pekerjaan apa pun sendiri. Ia hanya membungkus
			 * proses berat ke dalam {@code Common.createDefaultTimer(...)} disertai pesan
			 * "Harap tunggu.. sedang melakukan proses download lampiran..". Pembungkusan timer
			 * ini penting: mengunduh dan menyalin ratusan berkas dapat memakan waktu lama, dan
			 * menjalankannya langsung pada thread event akan membekukan antarmuka tanpa indikator
			 * apa pun.</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembuatan timer
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					/**
					 * Mengumpulkan berkas lampiran jawaban SELURUH peserta ke satu folder kerja,
					 * mengarsipkannya menjadi ZIP, lalu mengirimkannya sebagai unduhan.
					 *
					 * <p><b>Struktur folder hasil.</b> Folder kerja diberi nama
					 * {@code /opt/ecampus/lampiran_hasil_ujian_<epochMillis>} sehingga dua proses
					 * yang berjalan bersamaan tidak bertabrakan. Di dalamnya dibuat satu subfolder
					 * PER SOAL, dinamai dari 55 karakter pertama teks soal yang telah
					 * di-{@code URLEncoder}-kan (pengkodean wajib karena teks soal boleh
					 * mengandung karakter yang tidak sah sebagai nama berkas). Setiap berkas
					 * dinamai {@code <nim>_<nama>_<idLampiran>_<namaAsli>}.</p>
					 *
					 * <p><b>Tiga jenis lampiran ditangani berbeda:</b> lampiran yang sudah
					 * dipindahkan ke Google Drive ditulis sebagai berkas {@code .txt} berisi URL
					 * penerusannya; lampiran berupa pranala eksternal ditulis sebagai {@code .txt}
					 * berisi pranala tersebut; lampiran berkas nyata disalin dengan
					 * {@code IOUtils.copyLarge} (aliran, bukan muat-ke-memori — penting karena
					 * lampiran jawaban bisa berupa video atau gambar besar).</p>
					 *
					 * <p><b>Efek samping penulisan data.</b> Bila sebuah lampiran ditemukan
					 * sementara kolom {@code jawaban} peserta masih kosong, teks jawaban DIISI
					 * "Jawaban terdapat di file terlampir" dan disimpan dalam transaksi kecil
					 * tersendiri. Ini membuat aksi yang tampak read-only ternyata dapat MENGUBAH
					 * data jawaban. Tujuannya agar rekap dan koreksi tidak menampilkan jawaban
					 * kosong padahal peserta mengunggah berkas.</p>
					 *
					 * <p><b>Pengarsipan.</b> {@code Common.zipDir(...)} dibungkus {@code try/catch}
					 * karena folder kerja {@code /opt/ecampus} dapat hilang, tidak dapat ditulis,
					 * atau diskusnya penuh. Kegagalan ditampilkan sebagai pesan yang menyebut
					 * ketiga kemungkinan itu, bukan {@code FileNotFoundException} mentah.</p>
					 *
					 * <p><b>Kebersihan.</b> Folder kerja maupun berkas ZIP TIDAK dihapus setelah
					 * unduhan; pembersihannya diserahkan ke perawatan sistem. Session Hibernate
					 * ditutup di {@code finally}.</p>
					 *
					 * @param arg0 event timer; tidak dipakai
					 * @throws Exception diteruskan dari operasi berkas dan pengkodean nama
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

						File fileFolderLampiran = new File("/opt/ecampus/lampiran_hasil_ujian_"
								+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
						fileFolderLampiran.mkdirs();
						System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());
						
						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							
							for (Object[] a : hasilUjianMahasiswas.values()) {
								HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
								Mahasiswa mahasiswa = hasilUjianMahasiswa.getMahasiswa();

								MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
										hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
										true);

								Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
										.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
												ujianPunyaSoals);

								for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
									for (Long hasilUjianMahasiswaDetailid : aa) {
										HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
												.ambilData(HasilUjianMahasiswaDetail.class,
														hasilUjianMahasiswaDetailid.toString());
														
										if (hasilUjianMahasiswaDetail != null) {
											BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
											for (int i = 0; i < bankSoal.getJumlahLampiran(); i++) {
												LampiranLain lampiranLain = LampiranLain
														.ambil(hasilUjianMahasiswaDetail.getId(), "Jawaban ke-" + (i + 1));

												if (lampiranLain != null) {

													if (hasilUjianMahasiswaDetail.getJawaban().trim().isEmpty()) {
														hasilUjianMahasiswaDetail
																.setJawaban("Jawaban terdapat di file terlampir");
														Transaction tx = null;
														try {
															tx = session.beginTransaction();
															session.update(hasilUjianMahasiswaDetail);
															tx.commit();
														} catch(Exception e) {
															if(tx != null) tx.rollback();
														}
													}

													File fileFoto = lampiranLain.ambilFile();

													File folder = new File(fileFolderLampiran.getAbsolutePath() + "/"
															+ URLEncoder.encode((bankSoal.getSoal().length() > 55
																	? bankSoal.getSoal().substring(0, 55)
																	: bankSoal.getSoal()), "UTF-8"));
													folder.mkdirs();

													if (lampiranLain.getGdrive() != null
															&& !lampiranLain.getGdrive().trim().isEmpty()) {
														fileFoto = new File(
																folder.getAbsolutePath() + "/"
																		+ URLEncoder.encode(mahasiswa.getNim() + "_"
																				+ mahasiswa.getNama(), "UTF-8")
																		+ "_" + lampiranLain.getId() + "_"
																		+ fileFoto.getName() + ".txt");
														ais.common.BacaTulisUtil.tulis(fileFoto,
																lampiranLain.forwardGDriveUrl());
													} else if (lampiranLain.getLink() != null
															&& !lampiranLain.getLink().trim().isEmpty()) {
														fileFoto = new File(
																folder.getAbsolutePath() + "/"
																		+ URLEncoder.encode(mahasiswa.getNim() + "_"
																				+ mahasiswa.getNama(), "UTF-8")
																		+ "_" + lampiranLain.getId() + "_"
																		+ fileFoto.getName() + ".txt");
														ais.common.BacaTulisUtil.tulis(fileFoto,
																lampiranLain.getLink().trim());
													} else {
														File fileCopy = new File(
																folder.getAbsolutePath() + "/"
																		+ URLEncoder.encode(mahasiswa.getNim() + "_"
																				+ mahasiswa.getNama(), "UTF-8")
																		+ "_" + lampiranLain.getId() + "_"
																		+ fileFoto.getName());
														System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
														FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
														FileInputStream fileInputStream = new FileInputStream(fileFoto);
														IOUtils.copyLarge(fileInputStream, fileOutputStream);
														fileInputStream.close();
														fileOutputStream.close();
													}
												}
											}
										}
									}
								}

							}
						} finally {
							if (session != null && session.isOpen()) {
								try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1084");}
							}
						}

						// Arsipkan & unduh lampiran. Bila folder kerja "/opt/ecampus" tidak dapat
						// dibuat/ditulis (folder hilang, tanpa izin, atau disk penuh) zipDir melempar
						// IOException. Tangani agar proses download tidak menjatuhkan request dengan
						// FileNotFoundException mentah — tampilkan pesan yang jelas ke pengguna.
						File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
						try {
							Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
							Filedownload.save(fileFolderLampiranZip, "application/zip");
						} catch (Exception exZip) {
							Common.showInfo("Gagal membuat arsip lampiran hasil ujian. Pastikan folder penyimpanan "
									+ "server (/opt/ecampus) tersedia, memiliki izin tulis, dan kapasitas disk mencukupi, "
									+ "lalu coba lagi.");
							Common.tampilErrorJikaAdmin(exZip);
						}

					}
				}, "Harap tunggu.. sedang melakukan proses download lampiran..");
			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig drive = new MyToolbarbuttonConfig("Lampiran ke Drive", FileFoto.icon("drive.google"));
		drive.setParent(toolbar);
		drive.addEventListener("onClick", new EventListener() {

			/**
			 * Memicu aksi <b>"Lampiran ke Drive"</b>: memindahkan seluruh berkas lampiran jawaban
			 * peserta dari penyimpanan basis data ke Google Drive milik pengguna yang sedang
			 * masuk, lalu MENGHAPUS salinan aslinya dari basis data.
			 *
			 * <p><b>Sifatnya destruktif dan tidak dapat dibatalkan.</b> Setelah proses selesai,
			 * kolom {@code foto} pada {@link LampiranLain} di-null-kan dan
			 * {@code FileFoto.hapusTotal(...)} menghapus data biner aslinya; yang tersisa hanya
			 * id berkas Drive. Bila berkas di Drive kemudian dihapus atau akses akunnya dicabut,
			 * lampiran jawaban peserta hilang permanen. Tidak ada dialog konfirmasi.</p>
			 *
			 * <p><b>Kepemilikan berkas.</b> {@code GDriveUtilPerPengguna(tbmuser)} memakai
			 * kredensial Drive milik pengguna aktif, dan {@code gdriveUsername} pada tiap lampiran
			 * diisi {@code tbmuser.getUserId()}. Artinya berkas jawaban seluruh peserta berpindah
			 * ke akun pribadi dosen/admin yang menekan tombol ini — pertimbangkan kebijakan
			 * institusi mengenai penyimpanan data akademik di akun perorangan sebelum memakainya.</p>
			 *
			 * <p><b>Alur.</b> (1) Menelusuri seluruh peserta di grid, mengumpulkan id
			 * {@link LampiranLain} untuk setiap "Jawaban ke-N" sebanyak
			 * {@code bankSoal.getJumlahLampiran()}. (2) Bila kosong, pengguna diberi tahu dan
			 * proses berhenti. (3) Bila ada, sebuah bilah pemuatan "jangan berhenti" ditampilkan
			 * dan satu berkas uji ({@code /opt/ecampus/test.txt}) dikirim lebih dulu untuk
			 * memastikan kredensial Drive sah — pengiriman sesungguhnya baru berjalan di dalam
			 * callback keberhasilan berkas uji tersebut.</p>
			 *
			 * <p><b>Otorisasi.</b> Tombol ini TIDAK diberi {@code setVisible(...)} bersyarat sama
			 * sekali, berbeda dari tombol "Download Lampiran" di sebelahnya yang menyaring peran
			 * peserta. Setiap pengguna yang dapat membuka layar rekap dapat memicunya.</p>
			 *
			 * @param event event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pengumpulan lampiran dan inisialisasi Drive
			 */
			@Override
			public void onEvent(Event event) throws Exception {

				final Tbmuser tbmuser = Common.getCurrentUser();

				final List<Long> tugasFileContents = new ArrayList<Long>();

				for (Object[] a : hasilUjianMahasiswas.values()) {
					HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];

					MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
							hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);

					Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
							.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

					for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
						for (Long hasilUjianMahasiswaDetailid : aa) {
							HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());
							if (hasilUjianMahasiswaDetail != null) {
								BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
								for (int i = 0; i < bankSoal.getJumlahLampiran(); i++) {
									LampiranLain lampiranLain = LampiranLain.ambil(hasilUjianMahasiswaDetail.getId(),
											"Jawaban ke-" + (i + 1));

									if (lampiranLain != null) {
										tugasFileContents.add(lampiranLain.getId());
									}
								}
							}
						}
					}
				}

				if (tugasFileContents.isEmpty()) {
					MyMessageboxConfig.show(
				"Mohon maaf, tidak terdapat berkas ujian yang dapat dikirim ke Google Drive. Langkah yang dapat dilakukan: (1) pastikan peserta telah mengunggah berkas jawaban pada ujian ini; (2) apabila seharusnya terdapat berkas, muat ulang halaman lalu coba kembali.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} else {

					final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
					final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

						/**
						 * Callback bilah pemuatan yang <b>sengaja kosong</b>.
						 *
						 * <p>{@code displayLoadBarjanganBerhenti} mewajibkan sebuah listener
						 * penyelesaian, tetapi pada alur unggah Drive tidak ada yang perlu
						 * dilakukan saat selesai: grid tidak berubah tampilannya (yang berpindah
						 * hanya lokasi penyimpanan berkas), dan status akhir sudah disampaikan
						 * lewat label yang diisi "Selesai" oleh thread pengunggah.</p>
						 *
						 * <p>Varian "jangan berhenti" dipilih agar bilah pemuatan tidak menghilang
						 * sendiri di tengah proses unggah yang bisa berlangsung sangat lama.</p>
						 *
						 * @param arg0 event penanda selesai; tidak dipakai
						 * @throws Exception tidak pernah dilempar
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});

					final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
					File file = new File("/opt/ecampus/test.txt");
					ais.common.BacaTulisUtil.tulis(file, "test send..");
					driveUtilPerPengguna.prosesBackup(file, "test_files",

							new EventListener() {

								/**
								 * Callback keberhasilan <b>berkas uji</b>. Dipanggil
								 * {@code prosesBackup} setelah {@code /opt/ecampus/test.txt}
								 * berhasil diunggah ke folder {@code test_files} di Drive.
								 *
								 * <p>Berkas uji berfungsi sebagai <b>pemeriksaan kredensial</b>:
								 * bila token Drive pengguna kedaluwarsa atau izinnya dicabut,
								 * kegagalan terjadi pada satu berkas kecil dan callback ini tidak
								 * pernah dipanggil — jauh lebih baik daripada baru ketahuan
								 * setelah separuh lampiran terunggah dan aslinya terlanjur
								 * dihapus. Karena itu pengiriman sesungguhnya hanya dimulai bila
								 * {@code fileUpload} dan id-nya tidak null.</p>
								 *
								 * @param arg0 event pembawa {@code com.google.api.services.drive.model.File}
								 *             hasil unggah berkas uji
								 * @throws Exception diteruskan dari pelepasan thread pengunggah
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
											.getData();

									if (fileUpload != null && fileUpload.getId() != null) {

										new Thread(new Runnable() {

											/**
											 * Thread pengunggah lampiran ke Google Drive.
											 *
											 * <p><b>Mengapa memakai SQL mentah.</b> Daftar berkas
											 * yang akan diunggah diambil lewat
											 * {@code createSQLQuery("select id, foto from lampiran_lain where foto is not null and id in (...)")}
											 * pada {@code StreamingHibernateUtil} — bukan Criteria
											 * biasa. Alasannya, memuat entity {@link LampiranLain}
											 * lengkap berarti ikut memuat kolom biner {@code foto}
											 * untuk seluruh lampiran sekaligus ke memori. Query ini
											 * hanya mengambil id dan REFERENSI biner (large object
											 * id), sehingga isi berkas dibaca satu per satu.</p>
											 *
											 * <p><b>Alur per berkas.</b> Memuat {@link FileFoto}
											 * berdasarkan id, mengambil berkasnya, memperbarui
											 * label progres, lalu {@code kirimBackupLangsung(...)}
											 * mengunggahnya. Bila pengiriman mengembalikan
											 * {@code null} perulangan DIHENTIKAN
											 * ({@code break}) — pilihan yang disengaja: kegagalan
											 * unggah biasanya berarti kuota habis atau koneksi
											 * putus, sehingga meneruskan hanya akan menghasilkan
											 * rentetan kegagalan. Exception per berkas juga
											 * memicu {@code break} setelah ditampilkan ke
											 * administrator.</p>
											 *
											 * <p><b>Konsekuensi penghentian di tengah jalan.</b>
											 * Lampiran yang sudah terunggah SUDAH dihapus data
											 * binernya, sedangkan sisanya belum tersentuh. Keadaan
											 * campuran ini sah dan tidak merusak — menekan tombol
											 * kembali akan melanjutkan sisanya karena query hanya
											 * mengambil baris yang {@code foto}-nya masih ada.</p>
											 *
											 * <p><b>Sumber daya.</b> Session ditutup di
											 * {@code finally}; label diisi "Selesai" setelahnya.</p>
											 */
											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												Session session = null;
												try {
													session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

													String tableName = "lampiran_lain";
													String colFotoName = "foto";

													StringBuilder idsBuilder = new StringBuilder();
													for (Long id : tugasFileContents) {
														if(idsBuilder.length() > 0) idsBuilder.append(",");
														idsBuilder.append(id.toString());
													}
													String ids = idsBuilder.toString();

													List<Object[]> inds = session.createSQLQuery("select id," + colFotoName
															+ " from " + tableName + " where " + colFotoName
															+ " is not null and id in (" + ids + ")  order by id desc;")
															.list();

													int size = inds.size();
													int index = 0;
													for (Object[] o : inds) {
														index++;

														try {
															Object id = o[0];
															final Object fotoId = o[1];

															final FileFoto fileFoto = (FileFoto) session
																	.createCriteria(LampiranLain.class)
																	.add(Restrictions.idEq(Long.parseLong(id.toString())))
																	.uniqueResult();
															
															if (fileFoto != null) {
																File file = fileFoto.ambilFile();
																if (file != null && file.exists()) {
																	String s = "Mengirim file " + file.getName() + " ("
																			+ Common.numberFormat.get()
																					.format((index * 100.0) / size)
																			+ "%)";
																	System.out.println(s);
																	label.setValue(s);

																	com.google.api.services.drive.model.File fileKirim = driveUtilPerPengguna
																			.kirimBackupLangsung(null, file,
																					perguruanTinggi,
																					fileFoto.getClass().getSimpleName(),
																					new EventListener() {

																						/**
																						 * Callback keberhasilan unggah SATU
																						 * lampiran: menukar penyimpanan berkas
																						 * dari basis data ke Google Drive.
																						 *
																						 * <p>Dalam satu transaksi pada session
																						 * terpisah: memuat ulang
																						 * {@link LampiranLain} terkelola,
																						 * meng-{@code null}-kan kolom
																						 * {@code foto}, mengisi {@code gdrive}
																						 * dengan id berkas Drive dan
																						 * {@code gdriveUsername} dengan
																						 * pemilik kredensial, lalu commit.</p>
																						 *
																						 * <p><b>Urutan yang menentukan.</b>
																						 * {@code FileFoto.hapusTotal(...)} yang
																						 * benar-benar membuang data biner
																						 * dipanggil SETELAH commit. Bila
																						 * dipanggil sebelum commit dan
																						 * transaksi gagal, berkas sudah hilang
																						 * sementara kolom {@code gdrive} belum
																						 * tersimpan — lampiran menjadi tidak
																						 * dapat dijangkau dari kedua sisi.</p>
																						 *
																						 * <p>Kegagalan di-rollback dan dicatat
																						 * ke {@code ErrorAuditUtil}; session
																						 * ditutup di {@code finally}.</p>
																						 *
																						 * @param arg0 event pembawa berkas Drive
																						 *             hasil unggah
																						 * @throws Exception diteruskan dari
																						 *         operasi basis data
																						 */
																						@Override
																						public void onEvent(Event arg0)
																								throws Exception {
																							com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
																									.getData();

																							if (fileUpload != null
																									&& fileUpload
																											.getId() != null) {

																								Session iSession = null;
																								Transaction iTx = null;
																								try {
																									iSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
																									iTx = iSession.beginTransaction();

																									FileFoto iFileFoto = (FileFoto) iSession.get(LampiranLain.class, fileFoto.getId());
																									if (iFileFoto != null) {
																										iFileFoto.setFoto(null);
																										iFileFoto.setGdrive(fileUpload.getId());
																										iFileFoto.setGdriveUsername(tbmuser.getUserId());
																										iSession.update(iFileFoto);
																									}
																									iTx.commit();

																									FileFoto.hapusTotal(fotoId.toString(), iSession);

																								} catch (Exception e) {
																									if (iTx != null) iTx.rollback();
																									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1260");
																								} finally {
																									if (iSession != null && iSession.isOpen()) {
																										try { iSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1263");}
																									}
																								}
																							}

																						}
																					});

																	if (fileKirim == null) {
																		System.out.println(
																				"Gagal Terkirim " + file.getAbsolutePath());
																		break;
																	} else {
																		System.out.println(
																				"Terkirim " + fileKirim.toPrettyString());

																	}
																}
															}
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
															break;
														}
													}
												} finally {
													if(session != null && session.isOpen()) {
														try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1289");}
													}
												}
												label.setValue("Selesai");
											}
										}).start();
									}

								}
							});

				}

			}
		});

		String[] contents1 = new String[] {
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "id"
						: "hasilUjianMahasiswa.biodataCalonMahasiswa.noRegistrasi",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "hasilUjianMahasiswa.mahasiswa.nim"
						: "hasilUjianMahasiswa.biodataCalonMahasiswa.noUjian",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "hasilUjianMahasiswa.mahasiswa.nama"
						: "hasilUjianMahasiswa.biodataCalonMahasiswa.nama",
				"bankSoalDetail.huruf", "bankSoal.soal-text", "bankSoalDetail.jawaban", "bankSoalDetail.betul", "nilai",
				"jawaban", "koreksi", "waktuJawab", "hasilUjianMahasiswa", "hasilUjianMahasiswa.keyhasil" };
		cetakToolbarbutton = Common.cetakDataCustomButton(HasilUjianMahasiswaDetail.class, new DataCriteria() {

			/**
			 * Menyusun {@link Criteria} sumber data untuk ekspor Excel <b>"Soal dan Jawaban"</b>:
			 * seluruh {@link HasilUjianMahasiswaDetail} milik ujian ini.
			 *
			 * <p><b>Berbeda dari ekspor "Rekap Hasil Ujian".</b> Ekspor rekap menghasilkan SATU
			 * baris per peserta; ekspor ini menghasilkan satu baris per JAWABAN, sehingga ukuran
			 * berkasnya kira-kira sebesar jumlah peserta dikali jumlah soal. Dipakai ketika
			 * diperlukan telaah menyeluruh isi jawaban (mis. pemeriksaan indikasi kecurangan atau
			 * telaah kualitas jawaban esai).</p>
			 *
			 * <p><b>Penyaringan.</b> Alias {@code hasilUjianMahasiswa} dibuat agar penyaringan
			 * dapat menembus relasi ke {@code pertemuanPunyaUjian}. Berbeda dari
			 * {@link DataCriteria} ekspor rekap, di sini TIDAK ada penyaring
			 * {@code mahasiswa}/{@code biodataCalonMahasiswa}: ekspor selalu mencakup seluruh
			 * peserta ujian, bahkan ketika layar sedang berada pada mode satu peserta.</p>
			 *
			 * <p><b>Pengurutan.</b> Parameter {@code order} DIHORMATI di sini: bila diminta,
			 * baris diurutkan menurut peserta lalu id detail, sehingga jawaban satu peserta
			 * berkelompok berurutan dan berkasnya enak dibaca.</p>
			 *
			 * @param order penanda apakah pengurutan diminta mesin ekspor
			 * @return Criteria siap dieksekusi, dengan atau tanpa pengurutan
			 */
			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(HasilUjianMahasiswaDetail.class)
						.createAlias("hasilUjianMahasiswa", "hasilUjianMahasiswa")
						.add(Restrictions.eq("hasilUjianMahasiswa.pertemuanPunyaUjian", pertemuanPunyaUjian));

				if (order) {
					criteria.addOrder(Order.asc("hasilUjianMahasiswa")).addOrder(Order.asc("id"));
				}
				return criteria;
			}
		}, "Soal dan Jawaban", "/img/print.png", contents1);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			/**
			 * Tombol <b>Refresh</b>: memuat ulang grid dengan penanda refresh {@code true}.
			 *
			 * <p>Penanda {@code true} berarti himpunan soal terjawab dibaca ULANG dari sumbernya,
			 * menembus cache. Inilah tombol yang dirujuk berbagai pesan di kelas ini ("Jika angka
			 * pada grid belum berubah, klik Refresh") setelah aksi yang mengubah nilai di luar
			 * jalur grid — misalnya "Hitung Ulang Peserta Ini" pada panel diagnostik nilai 0.</p>
			 *
			 * <p>Perhatikan bahwa memuat ulang tanpa kata kunci pencarian juga MERESET berkas
			 * lokasi hasil ujian pada {@code PertemuanPunyaUjian} — lihat penjelasan
			 * {@code reloadNama} pada {@link HasilUjianMahasiswaHelper#loadData(Object)}.</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pemuatan ulang grid
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(true);
			}
		});

		nama = new Textbox();
		nama.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null);
		nama.setCols(7);
		nama.setParent(toolbar);
		nama.addEventListener("onOK", new EventListener() {

			/**
			 * Menjalankan pencarian ketika pengguna menekan Enter di kotak nama.
			 *
			 * <p>Memuat ulang grid dengan {@code loadData(null)} — penanda refresh
			 * {@code null}/{@code false}, BUKAN {@code true}. Pilihan ini disengaja: pencarian
			 * hanya menyaring peserta mana yang ditampilkan, sedangkan isi jawaban tiap peserta
			 * tidak berubah, sehingga cache boleh dipakai dan pencarian terasa jauh lebih
			 * cepat.</p>
			 *
			 * <p>Efek sampingnya, karena kata kunci tidak kosong, {@code loadData} juga MELEWATI
			 * reset berkas lokasi hasil ujian — penjagaan agar hasil pencarian parsial tidak
			 * menghapus peta lokasi peserta yang sedang tersaring keluar.</p>
			 *
			 * @param arg0 event {@code onOK} (tombol Enter); tidak dipakai
			 * @throws Exception diteruskan dari pemuatan ulang grid
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			/**
			 * Tombol kaca pembesar di samping kotak nama — jalur alternatif pemicu pencarian bagi
			 * pengguna yang mengeklik alih-alih menekan Enter.
			 *
			 * <p>Perilakunya identik dengan listener {@code onOK} pada kotak nama:
			 * {@code loadData(null)}, yaitu tanpa memaksa pembacaan ulang cache karena pencarian
			 * hanya mengubah penyaringan peserta. Visibilitasnya pun disamakan dengan kotak nama
			 * (disembunyikan pada mode satu peserta serta bagi peserta kursus dan siswa).</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pemuatan ulang grid
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Tombol <b>Tutup</b> di panel South: melepas komponen induk tempat seluruh layar
			 * rekap dibangun.
			 *
			 * <p>Yang dilepas adalah {@code detail} — parameter yang diserahkan pemanggil ke
			 * {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)}. Pada
			 * pemanggil yang menyerahkan sebuah {@code Window}, ini menutup jendela rekap;
			 * pada pemanggil yang menyerahkan panel tab, ini mengosongkan tab tersebut.</p>
			 *
			 * <p>Melepas induk otomatis melepas seluruh keturunannya, termasuk grid, toolbar,
			 * dan panel statistik. Thread latar yang mungkin masih berjalan tidak dihentikan —
			 * ia akan gagal senyap saat mencoba memperbarui label yang sudah terlepas, dan
			 * kegagalan itu memang sudah diantisipasi blok {@code try/catch} di dalamnya.</p>
			 *
			 * @param event event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari operasi pelepasan komponen
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				detail.detach();
			}
		});
		cancel.setParent(toolbar);

		east = new Center();
		east.setBorder("none");

		Center center = new Center();
		center.setParent(borderlayout);
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("15000px");
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSoal = new MyTabConfig("Peserta", "/img/svg/user-group.svg");
		tabs.appendChild(tabSoal);
		MyTabConfig tabPeserta = new MyTabConfig("Statistik", "/img/svg/chart-line-light.svg");
		tabs.appendChild(tabPeserta);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel parent = new ais.ui.util.MyTabpanel();
		parent.setHeight("15000px");
		parent.setParent(tabpanels);

		Borderlayout borderlayoutLagi = new Borderlayout();
		borderlayoutLagi.setParent(parent);

		Center centerLagi = new Center();
		centerLagi.setParent(borderlayoutLagi);
		centerLagi.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(centerLagi, true);

		parent = new ais.ui.util.MyTabpanel();
		parent.setHeight("15000px");
		parent.setParent(tabpanels);

		borderlayoutLagi = new Borderlayout();
		borderlayoutLagi.setParent(parent);

		east.setParent(borderlayoutLagi);
		ais.ui.util.ZkCompat.setFlex(east, true);

		grid.setParent(centerLagi);

		grid.setSclass("fgrid ais-data-grid");
		grid.setHeight("auto");
		grid.setWidth("100%");
		grid.setMold("paging");
		// Tampilkan SEMUA peserta dalam satu halaman (page size besar) -> tidak perlu navigasi
		// paging untuk ukuran kelas normal; seluruh data langsung terlihat (cukup di-scroll).
		grid.setPageSize(1000);
		grid.setPagingPosition("both");
		try {
			org.zkoss.zul.Paging pg = grid.getPagingChild();
			if (pg != null) {
				pg.setMold("os");
				pg.setDetailed(true);
			}
		} catch (Exception ignorePg) { ais.common.ErrorAuditUtil.record(ignorePg, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1441");
		}

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig("Peserta Ujian");
		column.setParent(columns);
		column.setWidth("18%");

		column = new MyColumnConfig("Waktu Pengerjaan");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Lama Pengerjaan");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Skor/Max");
		column.setParent(columns);
		column.setWidth("8%");
		column.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA));

		column = new MyColumnConfig("Statistik");
		column.setParent(columns);
		column.setWidth("13%");

		column = new MyColumnConfig("Nilai");
		column.setParent(columns);
		column.setWidth("11%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);
		column.setWidth("13%");

		column = new MyColumnConfig("Pelanggaran");
		column.setParent(columns);
		column.setWidth("13%");

		loadData(null);

	}

	/**
	 * Popup "Perbandingan Skor" utk peserta ujian pilihan ganda (non-OBE): daftar semua soal +
	 * Skor Diperoleh (nilai jawaban peserta) vs Skor Jawaban/Maksimal (skor soal). Baris yang skor
	 * diperolehnya MELEBIHI skor maksimal soal (data tak wajar, mis. 1 record rusak bernilai 100 pada
	 * soal berskor 0) disorot MERAH -- itulah penyebab total skor & Nilai janggal (mis. Nilai > 100).
	 * Dipicu saat sel "Nilai" pada tabel peserta diklik (mirip popup rincian versi OBE).
	 *
	 * @param himParam hasil ujian peserta (boleh detached; di-refetch di sini)
	 */
	public static void bukaPopupPerbandinganSkor(final HasilUjianMahasiswa himParam) {
		Session session = null;
		List<Object[]> baris = new ArrayList<Object[]>(); // {no, soal, diperoleh, maks, anomali}
		double totalDiperoleh = 0.0;
		double totalMaks = 0.0;
		int jmlAnomali = 0;
		int jmlPersen = 0;
		String namaMhs = "";
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			HasilUjianMahasiswa him = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, himParam.getId());
			if (him == null) {
				him = himParam;
			}
			try {
				namaMhs = him.getMahasiswa() == null ? "" : him.getMahasiswa().getNama();
			} catch (Exception eN) { ais.common.ErrorAuditUtil.record(eN, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1521");
			}
			List<?> details = session.createCriteria(HasilUjianMahasiswaDetail.class)
					.add(Restrictions.eq("hasilUjianMahasiswa", him)).createAlias("bankSoal", "bankSoal")
					.addOrder(Order.asc("id")).list();
			// Gabung per soal (bankSoal): skor diperoleh dijumlah, skor maksimal = skor soal.
			java.util.LinkedHashMap<Long, Double> diperolehPerSoal = new java.util.LinkedHashMap<Long, Double>();
			java.util.Map<Long, Double> maksPerSoal = new java.util.HashMap<Long, Double>();
			java.util.Map<Long, String> soalPerSoal = new java.util.HashMap<Long, String>();
			java.util.Map<Long, String> tipePerSoal = new java.util.HashMap<Long, String>();
			for (Object o : details) {
				HasilUjianMahasiswaDetail d = (HasilUjianMahasiswaDetail) o;
				BankSoal bs = d.getBankSoal();
				if (bs == null) {
					continue;
				}
				Long key = bs.getId();
				double earned = d.getNilai() == null ? 0.0 : d.getNilai();
				diperolehPerSoal.put(key, (diperolehPerSoal.containsKey(key) ? diperolehPerSoal.get(key) : 0.0) + earned);
				if (!maksPerSoal.containsKey(key)) {
					maksPerSoal.put(key, bs.getSkor() == null ? 0.0 : bs.getSkor());
					soalPerSoal.put(key, bs.getSoal() == null ? "" : bs.getSoal());
				tipePerSoal.put(key, bs.getJenisPilihanGanda());
				}
			}
			int no = 0;
			for (java.util.Map.Entry<Long, Double> e : diperolehPerSoal.entrySet()) {
				no++;
				double diperoleh = e.getValue() == null ? 0.0 : e.getValue();
				double maks = maksPerSoal.get(e.getKey()) == null ? 0.0 : maksPerSoal.get(e.getKey());
				String tipe = tipePerSoal.get(e.getKey());
				boolean skorPersen = BankSoal.MENGURUTKAN.equals(tipe) || BankSoal.MENJODOHKAN.equals(tipe);
				// Tipe persen (Menjodohkan/Mengurutkan): 'diperoleh' = 0-100 (%), WAJAR melebihi maks poin ->
				// BUKAN error (sudah dikonversi proporsional ke poin oleh hitungPilihanGanda). Error SEBENARNYA
				// hanya bila tipe POIN tetapi diperoleh > maks.
				boolean errorPoin = !skorPersen && diperoleh > maks;
				if (errorPoin) {
					jmlAnomali++;
				}
				if (skorPersen) {
					jmlPersen++;
				}
				totalDiperoleh += diperoleh;
				totalMaks += maks;
				baris.add(new Object[] { no, soalPerSoal.get(e.getKey()), diperoleh, maks, tipe, skorPersen, errorPoin });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1568");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1574");
				}
			}
		}

		MyWindow window = new MyWindow(
				"Perbandingan Skor Jawaban vs Skor Diperoleh" + (namaMhs.isEmpty() ? "" : " - " + namaMhs), "normal",
				true);
		window.setWidth("760px");
		window.setHeight("80%");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Vbox vb = new Vbox();
		vb.setWidth("100%");
		vb.setParent(window);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(vb);
		Columns cols = new Columns();
		cols.setParent(grid);
		org.zkoss.zul.Column col;
		col = new org.zkoss.zul.Column("No");
		col.setWidth("42px");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Soal");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Tipe");
		col.setWidth("104px");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Skor Diperoleh");
		col.setWidth("110px");
		col.setAlign("right");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Skor Jawaban (Maks)");
		col.setWidth("140px");
		col.setAlign("right");
		col.setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < baris.size(); i++) {
			Object[] r = baris.get(i);
			String tipe = r[4] == null ? "" : r[4].toString();
			boolean skorPersen = Boolean.TRUE.equals(r[5]);
			boolean errorPoin = Boolean.TRUE.equals(r[6]);
			Row row = new Row();
			row.setValign("top");
			if (errorPoin) {
				// Tipe POIN tetapi skor diperoleh > maksimal -> data tak wajar -> sorot MERAH.
				row.setStyle("background:#fde8e8;");
			} else if (skorPersen) {
				// Tipe persen (Menjodohkan/Mengurutkan) -> sorot AMBER sbg penanda beda skala.
				row.setStyle("background:#fff7e6;");
			}
			row.setParent(rows);
			new Label(r[0] + "").setParent(row);
			new org.zkoss.zul.Html(r[1] == null ? "" : r[1].toString()).setParent(row);
			Label lblTipe = new Label(skorPersen ? (tipe + " (%)") : "Pilihan Ganda");
			if (skorPersen) {
				lblTipe.setStyle("color:#b45309;font-weight:bold;");
			}
			lblTipe.setParent(row);
			Label lblDiperoleh = new Label(Common.numberFormat.get().format((Double) r[2])
				+ (skorPersen ? " %" : "") + (errorPoin ? "  (!)" : ""));
			if (errorPoin) {
				lblDiperoleh.setStyle("color:#c62828;font-weight:bold;");
			} else if (skorPersen) {
				lblDiperoleh.setStyle("color:#b45309;font-weight:bold;");
			}
			lblDiperoleh.setParent(row);
			new Label(Common.numberFormat.get().format((Double) r[3])).setParent(row);
		}

		if (baris.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			new Label(ais.common.Common.getBahasaConfig("Tidak ada rincian jawaban.")).setParent(row);
			new Label("").setParent(row);
			new Label("").setParent(row);
			new Label("").setParent(row);
			new Label("").setParent(row);
		}

		Row rowTotal = new Row();
		rowTotal.setStyle("background:#f1f5f9;");
		rowTotal.setParent(rows);
		new MyLabelBoldAja("").setParent(rowTotal);
		new MyLabelBoldAja("TOTAL").setParent(rowTotal);
		new MyLabelBoldAja("").setParent(rowTotal);
		new MyLabelBoldAja(Common.numberFormat.get().format(totalDiperoleh)).setParent(rowTotal);
		new MyLabelBoldAja(Common.numberFormat.get().format(totalMaks)).setParent(rowTotal);

		double nilaiHitung = totalMaks == 0.0 ? 0.0 : (totalDiperoleh * 100.0) / totalMaks;
		new MyLabelKecil("Nilai = skor diperoleh / skor maksimal x 100 = "
				+ Common.numberFormat.get().format(totalDiperoleh) + " / " + Common.numberFormat.get().format(totalMaks)
				+ " x 100 = " + Common.numberFormat.get().format(nilaiHitung)).setParent(vb);

		if (jmlPersen > 0) {
			org.zkoss.zul.Label infoPersen = new org.zkoss.zul.Label("Catatan: " + jmlPersen
				+ " soal bertipe Menjodohkan/Mengurutkan (baris amber) menyimpan skor sebagai PERSENTASE "
				+ "(0-100), beda skala dengan Pilihan Ganda. Sistem kini otomatis mengonversinya ke poin "
				+ "= (persen/100) x skor soal saat menghitung Nilai, sehingga Nilai tidak lagi > 100.");
			infoPersen.setMultiline(true);
			infoPersen.setStyle("color:#b45309;font-weight:bold;font-size:11px;display:block;margin-top:6px;");
			infoPersen.setParent(vb);
		}
		if (jmlAnomali > 0) {
			org.zkoss.zul.Label peringatan = new org.zkoss.zul.Label("Ada " + jmlAnomali
				+ " soal bertipe POIN dengan Skor Diperoleh MELEBIHI Skor Maksimal (data tak wajar, baris "
				+ "merah). Perbaiki skor jawaban soal itu lalu klik \"Hitung Ulang Semua\".");
			peringatan.setMultiline(true);
			peringatan.setStyle("color:#c62828;font-weight:bold;font-size:11px;display:block;margin-top:6px;");
			peringatan.setParent(vb);
		}

		try {
			window.onModal();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1693");
			// InterruptedException wajar saat modal ditutup; abaikan.
		}
	}

	/**
	 * <b>Tujuan:</b> Membuat tombol ikon tanda-tanya kecil ("Bantuan") yang dipasang berdampingan
	 * dengan angka nilai <b>0</b> pada baris peserta di grid rekap hasil ujian. Ketika diklik,
	 * tombol membuka {@link #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)} yang
	 * mendiagnosis MENGAPA nilai peserta itu 0 dan menawarkan tindakan perbaikan.
	 *
	 * <p><b>Latar belakang.</b> Nilai 0 adalah keluhan operasional yang paling sering muncul di
	 * modul ujian, dan penyebabnya beragam sekali: peserta memang belum mengerjakan; peserta
	 * sudah mengerjakan tapi semua jawabannya salah; kunci jawaban di bank soal belum ditandai
	 * {@code betul}; kolom {@code skor} soal masih 0 sehingga penyebut rumus nol; jawaban esai
	 * belum dikoreksi dosen; atau nilai belum dihitung ulang setelah soal/kunci diubah. Tanpa
	 * bantuan, dosen tidak punya cara membedakan keenam kondisi tersebut dari layar rekap dan
	 * akan mengeskalasi ke administrator. Tombol ini memindahkan diagnosis itu ke tangan dosen.</p>
	 *
	 * <p><b>Titik pemasangan.</b> Tombol dibuat di {@code DetailPertemuanPunyaUjianRenderer.render(...)}
	 * pada EMPAT konteks berbeda, seluruhnya hanya bila skor yang diperoleh 0 sementara skor
	 * maksimalnya &gt; 0 (kombinasi yang menandakan ada sesuatu yang layak diselidiki, bukan
	 * sekadar soal tanpa bobot):</p>
	 * <ul>
	 *   <li>kolom <b>Skor/Max</b> per Sub-CPMK pada mode OBE;</li>
	 *   <li>kolom <b>Skor/Max</b> pilihan ganda pada mode non-OBE;</li>
	 *   <li>kolom <b>Nilai</b> per Sub-CPMK pada mode OBE + pilihan ganda;</li>
	 *   <li>kolom <b>Nilai</b> pilihan ganda maupun esai pada mode non-OBE (di sini syaratnya
	 *       cukup nilai == 0, tanpa memeriksa skor maksimal).</li>
	 * </ul>
	 *
	 * <p><b>Cara kerja.</b> Membuat {@link MyToolbarbuttonConfig} dengan ikon
	 * {@code /img/Help-icon.png}, lalu <b>mengosongkan label</b> ({@code setLabel("")}) dan
	 * memampatkan gayanya ({@code padding:0;margin-left:4px;min-width:18px;min-height:18px})
	 * sehingga tombol hanya seukuran ikon. Ini penting: kolom Nilai pada grid sangat sempit, dan
	 * tombol berlabel teks akan melebarkan sel sehingga kolom paling kanan ("Pelanggaran")
	 * terpotong. Penjelasan tetap bisa dibaca lewat {@code tooltiptext} "Mengapa nilai masih 0?".
	 * Satu listener {@code onClick} dipasang yang meneruskan ketiga parameter apa adanya ke
	 * {@link #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)}.</p>
	 *
	 * <p><b>Sifat parameter {@code final}.</b> Ketiga parameter dideklarasikan {@code final}
	 * karena ditangkap oleh anonymous inner class {@code EventListener}; ini syarat bahasa pada
	 * Java 7 (target kompilasi proyek ini). Nilai {@code totalSoal} dan {@code terjawab} adalah
	 * <b>snapshot</b> pada saat baris dirender, BUKAN nilai hidup — bila di sela render dan klik
	 * peserta menambah jawaban, popup akan menampilkan angka lama. Data yang benar-benar kritis
	 * (jumlah detail jawaban, skor per soal) justru dibaca ulang dari database di dalam
	 * {@link #buatPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)}, sehingga ketidaksinkronan
	 * ini hanya memengaruhi dua baris ringkasan teratas pada popup.</p>
	 *
	 * <p><b>Sifat/otorisasi.</b> Method {@code static} tanpa state. Tidak melakukan pengecekan
	 * hak akses sendiri — kelayakan pemanggil ditentukan oleh konteks render grid rekap. Tombol
	 * ini sendiri bersifat read-only; tindakan yang berpotensi mengubah data ("Hitung Ulang
	 * Peserta Ini") baru muncul di dalam popup dan didokumentasikan pada
	 * {@link #hitungUlangNilaiPeserta(HasilUjianMahasiswa)}.</p>
	 *
	 * @param hasilUjianMahasiswa entity hasil ujian peserta yang nilainya 0; boleh detached —
	 *                            akan di-refetch di dalam popup
	 * @param totalSoal           jumlah soal yang ditampilkan untuk ujian ini
	 *                            ({@code pertemuanPunyaUjian.getJmlDitampilkan()}), snapshot saat render
	 * @param terjawab            jumlah soal yang sudah dijawab peserta ini, snapshot saat render
	 * @return tombol ikon siap dipasang sebagai anak {@code Hbox} pada sel grid
	 * @see #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)
	 */
	private static MyToolbarbuttonConfig tombolBantuanNilaiNol(final HasilUjianMahasiswa hasilUjianMahasiswa,
			final int totalSoal, final int terjawab) {
		MyToolbarbuttonConfig bantuan = new MyToolbarbuttonConfig("Bantuan", "/img/Help-icon.png");
		bantuan.setLabel("");
		bantuan.setTooltiptext("Mengapa nilai masih 0?");
		bantuan.setStyle("padding:0;margin-left:4px;min-width:18px;min-height:18px;");
		bantuan.addEventListener("onClick", new EventListener() {
			/**
			 * Membuka panel diagnostik "Penjelasan Nilai 0" untuk peserta yang bersangkutan.
			 *
			 * <p>Listener ini hanya meneruskan ketiga nilai yang ditangkap dari
			 * {@link #tombolBantuanNilaiNol(HasilUjianMahasiswa, int, int)} apa adanya. Perlu
			 * diingat bahwa {@code totalSoal} dan {@code terjawab} adalah SNAPSHOT saat baris
			 * grid dirender, sedangkan angka-angka lain pada popup dibaca ulang dari database.</p>
			 *
			 * @param event event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembangunan jendela diagnostik
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				bukaPopupPenjelasanNilaiNol(hasilUjianMahasiswa, totalSoal, terjawab);
			}
		});
		return bantuan;
	}

	/**
	 * <b>Tujuan:</b> Menampilkan jendela modal <b>"Penjelasan Nilai 0"</b> — sebuah panel
	 * diagnostik yang menerangkan kepada dosen mengapa nilai seorang peserta masih 0 dan
	 * menyediakan satu tombol tindakan langsung untuk memperbaikinya.
	 *
	 * <p><b>Peran dalam trio.</b> Method ini adalah lapisan <i>presentasi</i> dari tiga method
	 * yang bekerja bersama:</p>
	 * <ul>
	 *   <li>{@link #tombolBantuanNilaiNol(HasilUjianMahasiswa, int, int)} — pemicu (tombol ikon
	 *       di grid);</li>
	 *   <li><b>method ini</b> — kerangka jendela, label teks, dan tombol aksi;</li>
	 *   <li>{@link #buatPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)} — mesin diagnosis yang
	 *       membaca database dan menyusun teks penjelasannya;</li>
	 *   <li>{@link #hitungUlangNilaiPeserta(HasilUjianMahasiswa)} — aksi perbaikan satu peserta.</li>
	 * </ul>
	 *
	 * <p><b>Cara kerja.</b></p>
	 * <ol>
	 *   <li>Membuat {@link MyWindow} berukuran tetap 520&times;430px, {@code closable}, dengan
	 *       {@code contentStyle} {@code overflow:auto} agar teks panjang tetap dapat digulir.
	 *       Jendela dipasang ke root halaman aktif melalui
	 *       {@code ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()} — pola baku
	 *       di kelas ini untuk popup yang harus bertahan meski komponen pemicunya di-detach.</li>
	 *   <li>Mengisi sebuah {@code Vbox} dengan satu {@link Label} berisi hasil
	 *       {@link #buatPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)}. Label dikonfigurasi
	 *       {@code setMultiline(true)} + {@code setPre(true)} + CSS {@code white-space:pre-wrap}
	 *       karena teks diagnosis disusun sebagai plain text ber-{@code \n} dan berpoin, bukan
	 *       HTML. Kombinasi ini menjaga baris baru tetap terlihat sekaligus mengizinkan
	 *       pembungkusan kata pada baris yang terlalu panjang.</li>
	 *   <li>Menambahkan {@code Hbox} aksi berisi satu tombol <b>"Hitung Ulang Peserta Ini"</b>.
	 *       Berbeda dari "Hitung Ulang Semua" di toolbar yang memproses seluruh kelas dengan
	 *       kolam 50 thread, tombol ini hanya memproses SATU peserta secara sinkron sehingga
	 *       aman diklik berulang saat mendiagnosis. Hasil kembalian
	 *       {@link #hitungUlangNilaiPeserta(HasilUjianMahasiswa)} berupa kalimat status langsung
	 *       ditampilkan lewat {@code MyMessageboxConfig.show(...)}.</li>
	 *   <li>Memanggil {@code window.onModal()} di dalam {@code try/catch}. ZK melempar
	 *       {@code InterruptedException} sebagai mekanisme NORMAL ketika modal ditutup; exception
	 *       itu direkam ke {@code ErrorAuditUtil} sebagai jejak audit dan tidak dilempar ulang.</li>
	 * </ol>
	 *
	 * <p><b>Batas ketersegaran data.</b> Diagnosis dibaca ulang dari database setiap kali popup
	 * dibuka (lihat {@link #buatPenjelasanNilaiNol}), tetapi jendela ini <b>tidak</b> menyegarkan
	 * dirinya sendiri setelah "Hitung Ulang Peserta Ini" dijalankan — teks penjelasan tetap
	 * menampilkan kondisi sebelum perbaikan. Ini disengaja agar dosen dapat membandingkan
	 * kondisi lama dengan angka baru yang muncul pada messagebox hasil. Untuk melihat diagnosis
	 * terbaru, tutup popup lalu klik kembali tombol bantuan.</p>
	 *
	 * <p><b>Efek samping pada grid.</b> Popup ini tidak me-refresh grid pemanggil. Setelah
	 * "Hitung Ulang Peserta Ini" berhasil, angka pada baris grid masih angka lama sampai dosen
	 * menekan tombol <b>Refresh</b> di toolbar — hal ini dinyatakan eksplisit pada kalimat
	 * penutup yang dikembalikan {@link #hitungUlangNilaiPeserta(HasilUjianMahasiswa)}.</p>
	 *
	 * <p><b>Sifat/otorisasi.</b> Method {@code static}, tidak menyentuh field instance dan tidak
	 * membuka session Hibernate sendiri. Seperti tombol pemicunya, tidak melakukan pengecekan hak
	 * akses; gerbang berada pada konteks render grid rekap dosen/admin.</p>
	 *
	 * @param hasilUjianMahasiswa entity hasil ujian peserta yang sedang didiagnosis; boleh
	 *                            detached — di-refetch di dalam {@link #buatPenjelasanNilaiNol}
	 *                            dan {@link #hitungUlangNilaiPeserta}
	 * @param totalSoal           jumlah soal yang ditampilkan untuk ujian ini (snapshot saat render grid)
	 * @param terjawab            jumlah soal yang telah dijawab peserta (snapshot saat render grid)
	 * @see #buatPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)
	 * @see #hitungUlangNilaiPeserta(HasilUjianMahasiswa)
	 */
	private static void bukaPopupPenjelasanNilaiNol(final HasilUjianMahasiswa hasilUjianMahasiswa, final int totalSoal,
			final int terjawab) {
		MyWindow window = new MyWindow("Penjelasan Nilai 0", "normal", true);
		window.setWidth("520px");
		window.setHeight("430px");
		window.setContentStyle("overflow:auto;padding:10px;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Vbox vb = new Vbox();
		vb.setWidth("100%");
		vb.setParent(window);

		Label info = new Label(buatPenjelasanNilaiNol(hasilUjianMahasiswa, totalSoal, terjawab));
		info.setMultiline(true);
		info.setPre(true);
		info.setStyle("font-size:12px;line-height:1.45;color:#334155;white-space:pre-wrap;");
		info.setParent(vb);

		Hbox aksi = new Hbox();
		aksi.setStyle("margin-top:10px;");
		aksi.setParent(vb);

		MyToolbarbuttonConfig hitungUlang = new MyToolbarbuttonConfig("Hitung Ulang Peserta Ini",
				"/img/Button-Refresh-icon.png");
		hitungUlang.setTooltiptext("Muat ulang detail jawaban dari database lalu hitung ulang nilai peserta ini saja");
		hitungUlang.addEventListener("onClick", new EventListener() {
			/**
			 * Menjalankan perbaikan <b>"Hitung Ulang Peserta Ini"</b> lalu menampilkan kalimat
			 * hasilnya.
			 *
			 * <p>{@link #hitungUlangNilaiPeserta(HasilUjianMahasiswa)} dirancang tidak pernah
			 * melempar exception dan SELALU mengembalikan kalimat berbahasa Indonesia yang layak
			 * ditampilkan — baik untuk jalur sukses, data tidak ditemukan, maupun error. Karena
			 * itu listener ini dapat menyalurkan nilai baliknya langsung ke messagebox tanpa
			 * penanganan error tambahan.</p>
			 *
			 * <p>Perhitungan berjalan SINKRON pada thread event ZK (bukan di latar). Ini dapat
			 * diterima karena hanya satu peserta yang diproses; berbeda dari "Hitung Ulang Semua"
			 * yang wajib berjalan di latar.</p>
			 *
			 * <p><b>Yang tidak dilakukan:</b> baris grid dan teks diagnosis pada popup TIDAK
			 * disegarkan. Kalimat hasil sudah memuat pengingat agar pengguna menekan Refresh pada
			 * daftar hasil ujian bila angka di grid belum berubah.</p>
			 *
			 * @param event event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembangunan messagebox
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(hitungUlangNilaiPeserta(hasilUjianMahasiswa), "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		});
		hitungUlang.setParent(aksi);

		try {
			window.onModal();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:bukaPopupPenjelasanNilaiNol");
		}
	}

	/**
	 * <b>Tujuan:</b> Menghitung ulang nilai <b>satu</b> peserta ujian dari sumber datanya
	 * (rincian jawaban di database), lalu mengembalikan kalimat status siap tampil untuk
	 * ditampilkan pada messagebox. Merupakan aksi perbaikan yang ditawarkan panel diagnostik
	 * {@link #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)}.
	 *
	 * <p><b>Mengapa versi satu-peserta perlu ada.</b> Toolbar rekap sudah memiliki "Hitung Ulang
	 * Semua" yang memproses seluruh kelas memakai kolam 50 thread dan session Hibernate per
	 * thread. Untuk keperluan diagnosis, cara itu terlalu berat dan terlalu berisiko: dosen yang
	 * hanya ingin memastikan satu peserta bermasalah tidak seharusnya menyentuh nilai peserta
	 * lain. Method ini menjalankan pipeline penilaian yang SAMA persis, namun untuk satu id saja,
	 * secara sinkron di thread pemanggil, sehingga hasilnya dapat langsung dilaporkan.</p>
	 *
	 * <p><b>Alur lengkap.</b></p>
	 * <ol>
	 *   <li><b>Penjagaan awal.</b> Bila parameter {@code null} atau {@code getId()} {@code null},
	 *       langsung mengembalikan kalimat "data peserta ujian tidak ditemukan" tanpa membuka
	 *       session sama sekali.</li>
	 *   <li><b>Session terdedikasi.</b> Membuka session baru dari {@code SessionFactory}
	 *       (bukan {@code HibernateUtil.currentSession()} milik ZK) sehingga transaksi ini
	 *       terpisah dari siklus request dan tidak terpengaruh session ZK yang mungkin sudah
	 *       ditutup thread lain.</li>
	 *   <li><b>Refetch terkelola.</b> {@code session.get(HasilUjianMahasiswa.class, id)}
	 *       mengambil instance TERKELOLA. Ini wajib: objek yang datang dari grid bersifat
	 *       detached, dan {@code session.update()} atas objek detached hasil cache MapDB bisa
	 *       menimpa perubahan yang dilakukan proses lain. Bila hasil {@code null} atau
	 *       {@code getPertemuanPunyaUjian()} {@code null}, dikembalikan kalimat gagal yang
	 *       menyebut penyebabnya.</li>
	 *   <li><b>Memuat ulang jawaban dari DATABASE.</b> {@code ambilUjianPunyaSoals(jml, new Label(), true)}
	 *       mengambil paket soal peserta ini, lalu dipakai overload EMPAT argumen
	 *       {@code ambilHasilUjianMahasiswaDetail(true, jml, new Label(), ujianPunyaSoals)}.
	 *       Argumen pertama {@code true} berarti <b>paksa muat ulang</b> — inilah yang membedakan
	 *       method ini dari pembacaan biasa di layar yang memakai cache MapDB. Cache basi adalah
	 *       salah satu penyebab paling sering "nilai 0 padahal jawaban ada", sehingga menembus
	 *       cache adalah inti dari tindakan perbaikan ini.</li>
	 *   <li><b>Menyegarkan jumlah soal.</b> {@code setJumlahSoal(jmlDitampilkan)} dengan
	 *       penjagaan null &rarr; {@code 0.0}. Kolom ini ikut basi bila konfigurasi ujian diubah
	 *       setelah peserta mengerjakan.</li>
	 *   <li><b>Cabang PILIHAN GANDA.</b> Bila jenis ujian {@code BankSoal.PILIHAN_GANDA}:
	 *     <ul>
	 *       <li>Bila kurikulum perkuliahan ber-OBE (rantai penjagaan null berlapis sampai
	 *           {@code kurikulum.apakahObe(tahunAjaran, ganjilGenap)}), dipanggil
	 *           {@code ambilFormatNilai(session, true)} lalu
	 *           {@code ProsesUjianHelper.hitungObe(...)}. Argumen {@code true} pada
	 *           {@code ambilFormatNilai} memaksa {@code setDefaultPembobotan()} dijalankan;
	 *           tanpa itu sebagian {@code FormatNilai} dapat memiliki {@code statusPertemuan}
	 *           null sehingga sub-CPMK-nya terlewat dan {@code nilaiObe} hanya terisi sebagian
	 *           (kasus yang sama dijelaskan panjang lebar pada blok "Hitung Ulang Semua").</li>
	 *       <li>Kegagalan {@code hitungObe} <b>sengaja ditelan</b> ke {@code ErrorAuditUtil} dan
	 *           TIDAK me-rollback transaksi, agar perhitungan pilihan ganda di bawahnya tetap
	 *           berjalan. Prioritasnya: lebih baik nilai pokok terhitung meski rekap OBE gagal,
	 *           daripada keduanya gagal.</li>
	 *       <li>{@code ProsesUjianHelper.hitungPilihanGanda(...)} mengisi
	 *           {@code jawabanBenar}, {@code jawabanBenarMax}, dan {@code nilai}.</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>Cabang NON-PILIHAN GANDA (esai/manual).</b> Hanya {@code hitungWaktu(...)} yang
	 *       dipanggil. <b>Perhatikan:</b> untuk ujian esai method ini TIDAK mengubah kolom
	 *       {@code nilai} — nilai esai adalah hasil koreksi manual dosen (atau AI) dan memang
	 *       tidak boleh ditimpa oleh perhitungan otomatis. Konsekuensinya, pada ujian esai tombol
	 *       "Hitung Ulang Peserta Ini" akan melaporkan nilai yang sama seperti sebelumnya; itu
	 *       perilaku benar, bukan kegagalan. Perbaikan nilai esai dilakukan lewat koreksi jawaban
	 *       atau tombol "Hitung Ulang" per baris pada kolom Nilai.</li>
	 *   <li><b>Simpan dan segarkan cache.</b> {@code session.update} + {@code tx.commit()}, lalu
	 *       {@code tx} di-null-kan agar blok {@code catch} tidak mencoba me-rollback transaksi
	 *       yang sudah selesai. Setelah commit, {@code GeneralValueObject.masukkanDataLangsung(...)}
	 *       menimpa entri cache MapDB yang berkunci {@code keyhasil} supaya tampilan tidak
	 *       menyajikan angka basi. Kegagalan penyegaran cache dicatat namun tidak membatalkan
	 *       keberhasilan penyimpanan.</li>
	 *   <li><b>Kalimat status.</b> Mengembalikan ringkasan berisi Skor/Max dan Nilai terbaru,
	 *       ditutup pengingat bahwa grid perlu ditekan Refresh bila angkanya belum berubah.</li>
	 * </ol>
	 *
	 * <p><b>Kontrak nilai balik.</b> Method ini <b>tidak pernah melempar exception</b>. Setiap
	 * jalur — sukses, data tidak ditemukan, maupun error — mengembalikan {@code String} berbahasa
	 * Indonesia yang layak ditampilkan langsung ke pengguna. Pada jalur error, transaksi
	 * di-rollback (bila masih aktif), exception direkam ke {@code ErrorAuditUtil}, dan pesannya
	 * disertakan pada kalimat balikan agar administrator dapat menelusuri.</p>
	 *
	 * <p><b>Integritas nilai.</b> Method ini adalah salah satu dari beberapa jalur yang MENULIS
	 * kolom {@code nilai}/{@code jawabanBenar} secara langsung. Ia tidak memverifikasi identitas
	 * pemanggil; gerbangnya sepenuhnya berada pada konteks UI (hanya dirender di grid rekap
	 * dosen/admin). Karena perhitungan selalu diturunkan ulang dari rincian jawaban di database,
	 * pemanggilan berulang bersifat <b>idempoten</b> — tidak ada akumulasi nilai.</p>
	 *
	 * <p><b>Session dan transaksi.</b> Session ditutup di {@code finally} dengan penjagaan
	 * {@code isOpen()}; kegagalan {@code close()} direkam sebagai jejak audit. Transaksi dimulai
	 * SETELAH pembacaan detail jawaban selesai sehingga jendela kunci database sesingkat mungkin.</p>
	 *
	 * @param hasilUjianMahasiswaParam entity peserta yang akan dihitung ulang; boleh
	 *                                 {@code null}/detached/tanpa id — semua ditangani sebagai
	 *                                 kalimat gagal yang informatif
	 * @return kalimat status berbahasa Indonesia siap tampil; tidak pernah {@code null}
	 * @see ProsesUjianHelper#hitungPilihanGanda
	 * @see ProsesUjianHelper#hitungObe
	 * @see #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)
	 */
	private static String hitungUlangNilaiPeserta(HasilUjianMahasiswa hasilUjianMahasiswaParam) {
		if (hasilUjianMahasiswaParam == null || hasilUjianMahasiswaParam.getId() == null) {
			return "Nilai belum dapat dihitung ulang karena data peserta ujian tidak ditemukan.";
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class,
					hasilUjianMahasiswaParam.getId());
			if (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getPertemuanPunyaUjian() == null) {
				return "Nilai belum dapat dihitung ulang karena data hasil ujian/pertemuan tidak ditemukan.";
			}
			PertemuanPunyaUjian ppu = hasilUjianMahasiswa.getPertemuanPunyaUjian();
			MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(),
					new Label(), true);
			Map<Long, Set<Long>> details = hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(true,
					ppu.getJmlDitampilkan(), new Label(), ujianPunyaSoals);

			tx = session.beginTransaction();
			hasilUjianMahasiswa.setJumlahSoal(
					ppu.getJmlDitampilkan() == null ? 0.0 : ppu.getJmlDitampilkan().doubleValue());
			if (ppu.getUjian() != null && BankSoal.PILIHAN_GANDA.equals(ppu.getUjian().getJenis())) {
				try {
					if (ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null
							&& ppu.getPertemuan().getPerkuliahan().getKurikulum() != null
							&& ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
									ppu.getPertemuan().getPerkuliahan().getTahunAjaran(),
									ppu.getPertemuan().getPerkuliahan().getGanjilGenap())) {
						List<FormatNilai> formatNilais = ppu.getPertemuan().getPerkuliahan()
								.ambilFormatNilai(session, true);
						ProsesUjianHelper.hitungObe(hasilUjianMahasiswa, details, formatNilais);
					}
				} catch (Exception eObe) {
					ais.common.ErrorAuditUtil.record(eObe,
							"HasilUjianMahasiswaHelper.hitungUlangNilaiPeserta-hitungObe");
				}
				ProsesUjianHelper.hitungPilihanGanda(hasilUjianMahasiswa, details);
			} else {
				ProsesUjianHelper.hitungWaktu(hasilUjianMahasiswa, details);
			}
			session.update(hasilUjianMahasiswa);
			tx.commit();
			tx = null;

			try {
				GeneralValueObject.masukkanDataLangsung(HasilUjianMahasiswa.class, hasilUjianMahasiswa,
						hasilUjianMahasiswa.getKeyhasil());
			} catch (Exception eCache) {
				ais.common.ErrorAuditUtil.record(eCache,
						"HasilUjianMahasiswaHelper.hitungUlangNilaiPeserta-refresh-cache");
			}

			return "Nilai peserta ini sudah dihitung ulang.\n\nSkor/Max sekarang: "
					+ Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar()) + " / "
					+ Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenarMax())
					+ "\nNilai sekarang: " + Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai())
					+ "\n\nJika angka pada grid belum berubah, klik Refresh pada daftar hasil ujian.";
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex,
							"auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:hitungUlangNilaiPeserta-rollback");
				}
			}
			ais.common.ErrorAuditUtil.record(e, "HasilUjianMahasiswaHelper.hitungUlangNilaiPeserta");
			return "Nilai belum berhasil dihitung ulang: " + e.getMessage();
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex,
							"auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:hitungUlangNilaiPeserta-close");
				}
			}
		}
	}

	/**
	 * <b>Tujuan:</b> Mesin diagnosis di balik panel "Penjelasan Nilai 0". Membaca kondisi nyata
	 * rincian jawaban seorang peserta dari database, lalu menyusun teks berbahasa Indonesia yang
	 * berisi ringkasan angka + daftar kemungkinan penyebab + langkah perbaikan yang disarankan.
	 *
	 * <p><b>Prinsip perancangan.</b> Teks yang dihasilkan sengaja dibuka dengan kalimat
	 * "Nilai 0 tidak selalu berarti peserta belum mengerjakan". Ini menjawab kekeliruan pembacaan
	 * yang paling sering terjadi: kolom "Soal Terjawab" pada grid hanya menghitung jawaban yang
	 * DIPILIH/DIISI, sedangkan nilai dihitung dari SKOR jawaban yang tersimpan. Kedua angka itu
	 * bisa berbeda jauh, dan perbedaannya itulah petunjuk diagnosis.</p>
	 *
	 * <p><b>Data yang dikumpulkan (fase baca).</b> Method membuka session Hibernate terdedikasi,
	 * me-refetch entity ({@code session.get}) dengan fallback ke objek parameter bila hasil
	 * refetch {@code null}, menentukan apakah ujian bertipe {@code PILIHAN_GANDA}, lalu memindai
	 * SELURUH {@code HasilUjianMahasiswaDetail} milik peserta ini dan mengakumulasi enam besaran:</p>
	 * <ul>
	 *   <li>{@code jumlahDetail} — banyak baris rincian jawaban yang tersimpan;</li>
	 *   <li>{@code detailSkorPositif} / {@code detailSkorNol} — pemilahan rincian berdasarkan
	 *       {@code nilai > 0} atau tidak. Rasio keduanya membedakan "salah semua" dari
	 *       "belum dikoreksi";</li>
	 *   <li>{@code soalSkorNol} — banyak rincian yang {@code bankSoal}-nya {@code null} atau
	 *       {@code skor}-nya {@code <= 0}. Ini penanda konfigurasi bank soal yang keliru: soal
	 *       tanpa bobot tidak akan pernah menyumbang nilai berapa pun jawabannya;</li>
	 *   <li>{@code totalDiperoleh} — jumlah seluruh {@code nilai} rincian;</li>
	 *   <li>{@code totalMaks} — jumlah {@code skor} soal, <b>hanya untuk soal yang skornya &gt; 0</b>.
	 *       Soal berskor 0 sengaja tidak ikut dijumlah dan dialihkan ke pencacah
	 *       {@code soalSkorNol} supaya tidak menyamarkan masalah konfigurasi tersebut.</li>
	 * </ul>
	 *
	 * <p><b>Ketahanan.</b> Seluruh fase baca dibungkus {@code try/catch} lebar; kegagalan apa pun
	 * direkam ke {@code ErrorAuditUtil} dan alur TETAP lanjut menyusun teks dengan pencacah
	 * bernilai nol. Artinya popup selalu terbuka membawa penjelasan generik, tidak pernah
	 * menampilkan halaman error. Session ditutup di {@code finally} dengan penjagaan
	 * {@code isOpen()}.</p>
	 *
	 * <p><b>Pohon keputusan (fase susun teks).</b> Setelah blok ringkasan angka, penyebab nomor 1
	 * dipilih dari empat kemungkinan yang saling eksklusif, berurutan dari yang paling spesifik:</p>
	 * <ol>
	 *   <li>{@code jumlahDetail == 0} &rarr; rincian jawaban belum tersimpan/terbaca sama sekali,
	 *       sistem tidak punya dasar menghitung;</li>
	 *   <li>{@code terjawab >= totalSoal && totalSoal > 0 && detailSkorPositif == 0} &rarr; kasus
	 *       paling mencurigakan: peserta menjawab SEMUA soal tetapi tidak satu pun berskor.
	 *       Teks menyebut tiga kemungkinan turunannya (jawaban memang salah semua, kunci jawaban
	 *       belum sesuai, atau nilai belum dihitung ulang setelah soal/kunci diubah);</li>
	 *   <li>{@code detailSkorPositif == 0} &rarr; belum ada rincian berskor, namun peserta belum
	 *       menjawab semua soal;</li>
	 *   <li>selain itu &rarr; sebagian skor sudah ada tetapi total akhir masih 0, mengarah ke
	 *       masalah agregasi/cache sehingga disarankan "Hitung Ulang Semua" atau "Sinkronkan Nilai".</li>
	 * </ol>
	 * Penyebab nomor 2 hanya muncul bila {@code soalSkorNol &gt; 0}. Penyebab nomor 3 selalu
	 * muncul namun isinya bercabang: untuk pilihan ganda mengarahkan ke popup perbandingan skor
	 * per soal, untuk esai mengarahkan ke koreksi jawaban lebih dulu.
	 *
	 * <p><b>Catatan tampilan yang diketahui.</b> Karena butir "2." bersyarat, penomoran dalam teks
	 * dapat melompat dari "1." langsung ke "3." pada ujian yang seluruh soalnya sudah berbobot.
	 * Ini murni kosmetik dan tidak memengaruhi diagnosis; bila ingin dirapikan, gantilah penomoran
	 * statis dengan pencacah berjalan.</p>
	 *
	 * <p><b>Format keluaran.</b> Plain text ber-{@code \n}, BUKAN HTML — itulah sebabnya
	 * {@link #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)} menampilkannya dengan
	 * {@code Label} ber-{@code setPre(true)} dan {@code white-space:pre-wrap}. Angka desimal
	 * diformat memakai {@code Common.numberFormat} (ThreadLocal) agar konsisten dengan sisa
	 * aplikasi.</p>
	 *
	 * <p><b>Sifat.</b> {@code static}, read-only terhadap database (tidak ada transaksi maupun
	 * {@code update}), dan tidak menyentuh field instance. Tidak pernah melempar exception.</p>
	 *
	 * @param hasilUjianMahasiswa entity peserta yang didiagnosis; boleh {@code null} atau
	 *                            detached — bila {@code null} seluruh pencacah tetap 0 dan teks
	 *                            generik tetap dihasilkan
	 * @param totalSoal           jumlah soal yang ditampilkan untuk ujian ini, diteruskan apa
	 *                            adanya dari renderer grid (snapshot)
	 * @param terjawab            jumlah soal yang dijawab peserta, diteruskan apa adanya dari
	 *                            renderer grid (snapshot); dipakai pada cabang keputusan nomor 2
	 * @return teks diagnosis plain text berbahasa Indonesia; tidak pernah {@code null}
	 * @see #bukaPopupPenjelasanNilaiNol(HasilUjianMahasiswa, int, int)
	 * @see #bukaPopupPerbandinganSkor(HasilUjianMahasiswa)
	 */
	private static String buatPenjelasanNilaiNol(HasilUjianMahasiswa hasilUjianMahasiswa, int totalSoal, int terjawab) {
		Session session = null;
		int jumlahDetail = 0;
		int detailSkorPositif = 0;
		int detailSkorNol = 0;
		int soalSkorNol = 0;
		double totalDiperoleh = 0.0;
		double totalMaks = 0.0;
		boolean pilihanGanda = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			HasilUjianMahasiswa him = hasilUjianMahasiswa;
			if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getId() != null) {
				HasilUjianMahasiswa db = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class,
						hasilUjianMahasiswa.getId());
				if (db != null) {
					him = db;
				}
			}
			if (him != null && him.getPertemuanPunyaUjian() != null && him.getPertemuanPunyaUjian().getUjian() != null) {
				pilihanGanda = BankSoal.PILIHAN_GANDA.equals(him.getPertemuanPunyaUjian().getUjian().getJenis());
			}
			if (him != null) {
				List<?> details = session.createCriteria(HasilUjianMahasiswaDetail.class)
						.add(Restrictions.eq("hasilUjianMahasiswa", him)).list();
				jumlahDetail = details == null ? 0 : details.size();
				if (details != null) {
					for (Object o : details) {
						HasilUjianMahasiswaDetail d = (HasilUjianMahasiswaDetail) o;
						double nilai = d.getNilai() == null ? 0.0 : d.getNilai().doubleValue();
						BankSoal bankSoal = d.getBankSoal();
						double skor = bankSoal == null || bankSoal.getSkor() == null ? 0.0
								: bankSoal.getSkor().doubleValue();
						totalDiperoleh += nilai;
						if (skor > 0.0) {
							totalMaks += skor;
						} else {
							soalSkorNol++;
						}
						if (nilai > 0.0) {
							detailSkorPositif++;
						} else {
							detailSkorNol++;
						}
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "HasilUjianMahasiswaHelper.buatPenjelasanNilaiNol");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex,
							"auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:buatPenjelasanNilaiNol-close");
				}
			}
		}

		StringBuffer sb = new StringBuffer();
		sb.append("Nilai 0 tidak selalu berarti peserta belum mengerjakan. Kolom \"Soal Terjawab\" hanya menghitung jawaban yang dipilih/diisi, sedangkan nilai dihitung dari skor jawaban yang tersimpan.\n\n");
		sb.append("Ringkasan data saat ini:\n");
		sb.append("- Total soal ditampilkan: ").append(totalSoal).append("\n");
		sb.append("- Soal terjawab: ").append(terjawab).append("\n");
		sb.append("- Rincian jawaban tersimpan: ").append(jumlahDetail).append("\n");
		sb.append("- Rincian dengan skor > 0: ").append(detailSkorPositif).append("\n");
		sb.append("- Rincian dengan skor 0/kosong: ").append(detailSkorNol).append("\n");
		sb.append("- Total skor diperoleh: ").append(Common.numberFormat.get().format(totalDiperoleh)).append("\n");
		sb.append("- Total skor maksimal terdeteksi: ").append(Common.numberFormat.get().format(totalMaks)).append("\n\n");

		sb.append("Kemungkinan penyebab:\n");
		if (jumlahDetail == 0) {
			sb.append("1. Rincian jawaban peserta belum tersimpan/terbaca, sehingga sistem belum punya dasar menghitung nilai.\n");
		} else if (terjawab >= totalSoal && totalSoal > 0 && detailSkorPositif == 0) {
			sb.append("1. Peserta sudah menjawab semua soal, tetapi semua rincian jawaban masih bernilai 0. Untuk pilihan ganda, kemungkinan jawaban peserta memang salah semua, kunci jawaban belum sesuai, atau nilai belum dihitung ulang setelah perubahan soal/kunci.\n");
		} else if (detailSkorPositif == 0) {
			sb.append("1. Belum ada rincian jawaban yang memiliki skor lebih dari 0.\n");
		} else {
			sb.append("1. Skor sebagian jawaban sudah ada, tetapi total nilai akhir masih 0. Coba klik Hitung Ulang Semua atau Sinkronkan Nilai.\n");
		}
		if (soalSkorNol > 0) {
			sb.append("2. Ada ").append(soalSkorNol)
					.append(" rincian yang soal/skor maksimalnya 0 atau kosong. Periksa skor soal di bank soal.\n");
		}
		if (pilihanGanda) {
			sb.append("3. Klik angka nilai 0 untuk melihat perbandingan skor per soal, lalu periksa kunci jawaban dan klik Hitung Ulang Semua/Sinkronkan Nilai.\n");
		} else {
			sb.append("3. Untuk essay/manual, lakukan koreksi jawaban terlebih dahulu lalu klik Hitung Ulang.\n");
		}
		return sb.toString();
	}

	/**
	 * Menampilkan jendela modal <b>"Rincian Skor &lt;nama Sub-CPMK&gt;"</b> yang membedah nilai
	 * satu Sub-CPMK milik SATU peserta menjadi daftar soal pembentuknya, lengkap dengan skor yang
	 * diperoleh dan skor maksimal per soal, baris TOTAL, serta rumus nilai akhirnya.
	 *
	 * <p><b>Tujuan.</b> Pada mode OBE, kolom nilai peserta tidak lagi berupa satu angka tunggal
	 * melainkan sederet angka per Sub-CPMK (mis. "1.01 : 72"). Angka agregat itu sering
	 * dipertanyakan dosen ("kenapa Sub-CPMK ini nilainya 0 padahal mahasiswanya menjawab?").
	 * Popup ini menjawab pertanyaan tersebut dengan menampilkan <i>audit trail</i> perhitungan:
	 * soal mana saja yang dipetakan ke Sub-CPMK tersebut (lewat {@code PertemuanPunyaUjian.formatNilais}),
	 * berapa skor yang didapat peserta pada tiap soal, dan berapa skor maksimalnya. Dengan begitu
	 * dosen dapat membedakan tiga penyebab berbeda yang sama-sama memunculkan nilai rendah:
	 * (a) peserta memang menjawab salah, (b) soal belum dipetakan ke Sub-CPMK mana pun sehingga
	 * daftar kosong, atau (c) skor maksimal soal di bank soal masih 0 sehingga penyebut rumus nol.</p>
	 *
	 * <p><b>Pemicu.</b> Dipanggil dari listener {@code onClick} pada label nilai per Sub-CPMK di
	 * {@code DetailPertemuanPunyaUjianRenderer.render(...)}. Label hanya dibuat dapat diklik
	 * (bergaya garis-bawah biru) bila {@code nilaiMax != 0}; bila skor maksimal 0 label tetap
	 * teks biasa karena rincian tidak akan bermakna.</p>
	 *
	 * <p><b>Cara kerja.</b></p>
	 * <ol>
	 *   <li><b>Refetch entity.</b> {@code himParam} yang datang dari grid biasanya berstatus
	 *       <i>detached</i> (session-nya sudah ditutup oleh {@link #loadData(Object)}). Method
	 *       membuka session Hibernate terdedikasi lewat {@code HibernateUtil.getSessionFactory()
	 *       .openSession()} lalu memuat ulang entity dengan {@code session.get(...)}. Bila hasil
	 *       refetch {@code null} (record sudah dihapus di sesi lain), objek parameter dipakai apa
	 *       adanya sebagai fallback agar popup tetap terbuka dan menampilkan daftar kosong,
	 *       bukan melempar exception ke pengguna.</li>
	 *   <li><b>Menentukan jumlah soal.</b> Diambil dari {@code pertemuanPunyaUjian.getJmlDitampilkan()}
	 *       dengan penjagaan null berlapis (baik {@code pertemuanPunyaUjian} maupun
	 *       {@code jmlDitampilkan} boleh null) sehingga jatuh ke {@code 0}.</li>
	 *   <li><b>Memuat jawaban peserta.</b> {@code ambilUjianPunyaSoals(jml, new Label(), true)}
	 *       mengembalikan daftar id {@code UjianPunyaSoal} sesuai paket soal yang benar-benar
	 *       diterima peserta ini (penting untuk ujian acak/random, karena tiap peserta bisa
	 *       memperoleh kombinasi soal berbeda). Hasilnya dipakai
	 *       {@code ambilHasilUjianMahasiswaDetail(jml, ujianPunyaSoals, false)} untuk memetakan
	 *       soal &rarr; himpunan id {@code HasilUjianMahasiswaDetail}. Argumen terakhir
	 *       {@code false} berarti TIDAK memaksa muat ulang dari database — dipakai cache MapDB
	 *       agar popup terbuka cepat.</li>
	 *   <li><b>Menghitung rincian.</b> Pekerjaan berat didelegasikan ke
	 *       {@code ProsesUjianHelper.rincianSkorSubCpmk(him, details, formatNilai.getId())}
	 *       yang mengembalikan {@code List<Object[]>} berisi {@code [0]=nomor, [1]=teks soal (HTML),
	 *       [2]=skor diperoleh (Double), [3]=skor maksimal (Double)}. Menempatkan logika pemetaan
	 *       Sub-CPMK di {@code ProsesUjianHelper} membuat popup ini memakai SUMBER KEBENARAN yang
	 *       sama dengan mesin penilaian ({@code hitungObe}) — bila keduanya berbeda, itu bug
	 *       nyata, bukan sekadar perbedaan tampilan.</li>
	 *   <li><b>Merender jendela.</b> {@link MyWindow} 720px &times; 80% berisi {@link MyGrid}
	 *       empat kolom (No / Soal / Skor / Maks). Kolom "Soal" memakai {@code org.zkoss.zul.Html}
	 *       karena teks soal disimpan sebagai HTML kaya (gambar, rumus, penomoran). Baris TOTAL
	 *       berlatar abu-abu menjumlahkan kolom Skor dan Maks.</li>
	 *   <li><b>Rumus penutup.</b> Baris {@link MyLabelKecil} di bawah grid menampilkan
	 *       {@code Nilai = totalSkor / totalMax * 100}, dengan penjagaan {@code totalMax == 0.0}
	 *       menghasilkan {@code 0.0} (bukan {@code NaN}/{@code Infinity}). Angka ini harus SAMA
	 *       dengan angka pada label yang diklik; bila berbeda, penyebabnya hampir selalu cache
	 *       {@code nilaiObe} yang basi — jalankan "Hitung Ulang Semua" lebih dulu.</li>
	 *   <li><b>Daftar kosong.</b> Bila {@code rincian} kosong, satu baris pesan
	 *       "Tidak ada soal pada Sub-CPMK ini." ditampilkan; baris TOTAL tetap dirender dengan
	 *       nilai 0 agar struktur grid konsisten.</li>
	 * </ol>
	 *
	 * <p><b>Manajemen session.</b> Session dibuka dan ditutup SELURUHNYA di blok
	 * {@code try/finally} SEBELUM komponen ZK dibangun. Ini disengaja: seluruh data sudah
	 * dimaterialisasi ke {@code List<Object[]>} berisi tipe primitif/String, sehingga tidak ada
	 * risiko {@code LazyInitializationException} saat komponen dirender maupun saat jendela
	 * modal masih terbuka menunggu pengguna. Kegagalan {@code session.close()} sendiri direkam
	 * ke {@code ErrorAuditUtil} dan tidak dilempar ulang.</p>
	 *
	 * <p><b>Penanganan error.</b> Seluruh tahap pengambilan data dibungkus satu {@code try/catch}
	 * lebar. Exception apa pun dicatat ({@code printStackTrace} + {@code ErrorAuditUtil}) dan
	 * ditampilkan ke administrator via {@code Common.tampilErrorJikaAdmin(e)}, namun alur TETAP
	 * berlanjut membangun jendela — pengguna melihat popup kosong, bukan halaman error.
	 * Pemanggilan {@code window.onModal()} di akhir dibungkus {@code try/catch} karena ZK
	 * melempar {@code InterruptedException} sebagai mekanisme normal saat modal ditutup.</p>
	 *
	 * <p><b>Otorisasi.</b> Method ini TIDAK melakukan pengecekan hak akses sendiri. Ia mengandalkan
	 * gerbang di lapisan pemanggil: label pemicunya hanya dirender di dalam grid rekap yang
	 * dibangun {@link #display(PertemuanPunyaUjian, Component)}. Karena bersifat
	 * <b>read-only</b> (tidak ada {@code update}/{@code commit} sama sekali) dampak terburuknya
	 * terbatas pada keterbacaan rincian jawaban, bukan perubahan nilai. Bila kelak popup ini
	 * dipanggil dari konteks portal peserta, tambahkan penjagaan kepemilikan
	 * ({@code him.getMahasiswa()} vs pengguna aktif) di sini.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila struktur {@code Object[]} yang dikembalikan
	 * {@code ProsesUjianHelper.rincianSkorSubCpmk} berubah (mis. menambah kolom "bobot"),
	 * perbarui juga jumlah kolom grid, jumlah {@code Label} pada cabang daftar-kosong, dan
	 * jumlah sel baris TOTAL — ZK akan merender baris rusak (sel bergeser) bila jumlahnya
	 * tidak seragam antar-baris.</p>
	 *
	 * @param himParam    hasil ujian peserta yang diklik; boleh detached — di-refetch di dalam
	 *                    method. Bila {@code getId()} null, {@code session.get} akan gagal dan
	 *                    ditangani sebagai daftar kosong.
	 * @param formatNilai Sub-CPMK ({@link FormatNilai}) yang labelnya diklik; {@code getId()}-nya
	 *                    dipakai sebagai kunci filter soal dan {@code getNama()}-nya dipakai pada
	 *                    judul jendela serta baris TOTAL
	 * @see ProsesUjianHelper#rincianSkorSubCpmk
	 * @see #bukaPopupPerbandinganSkor(HasilUjianMahasiswa)
	 */
	public static void bukaPopupRincianSubCpmk(final HasilUjianMahasiswa himParam, final FormatNilai formatNilai) {
		Session session = null;
		List<Object[]> rincian = new ArrayList<Object[]>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			HasilUjianMahasiswa him = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, himParam.getId());
			if (him == null) {
				him = himParam;
			}
			Integer jml = (him.getPertemuanPunyaUjian() == null
					|| him.getPertemuanPunyaUjian().getJmlDitampilkan() == null) ? Integer.valueOf(0)
							: him.getPertemuanPunyaUjian().getJmlDitampilkan();

			MyArrayList<Long> ujianPunyaSoals = him.ambilUjianPunyaSoals(jml, new Label(), true);
			Map<Long, Set<Long>> details = him.ambilHasilUjianMahasiswaDetail(jml, ujianPunyaSoals, false);
			rincian = ProsesUjianHelper.rincianSkorSubCpmk(him, details, formatNilai.getId());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1715");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1721");
				}
			}
		}

		MyWindow window = new MyWindow("Rincian Skor " + formatNilai.getNama(), "normal", true);
		window.setWidth("720px");
		window.setHeight("80%");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Vbox vb = new Vbox();
		vb.setWidth("100%");
		vb.setParent(window);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(vb);
		Columns cols = new Columns();
		cols.setParent(grid);
		org.zkoss.zul.Column col;
		col = new org.zkoss.zul.Column("No");
		col.setWidth("42px");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Soal");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Skor");
		col.setWidth("72px");
		col.setAlign("right");
		col.setParent(cols);
		col = new org.zkoss.zul.Column("Maks");
		col.setWidth("72px");
		col.setAlign("right");
		col.setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		double totalSkor = 0.0;
		double totalMax = 0.0;
		for (int i = 0; i < rincian.size(); i++) {
			Object[] r = rincian.get(i);
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			new Label(r[0] + "").setParent(row);
			new org.zkoss.zul.Html(r[1] == null ? "" : r[1].toString()).setParent(row);
			Double sk = (Double) r[2];
			Double mx = (Double) r[3];
			totalSkor += (sk == null ? 0.0 : sk.doubleValue());
			totalMax += (mx == null ? 0.0 : mx.doubleValue());
			new Label(Common.numberFormat.get().format(sk == null ? 0.0 : sk)).setParent(row);
			new Label(Common.numberFormat.get().format(mx == null ? 0.0 : mx)).setParent(row);
		}

		if (rincian.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			new Label(ais.common.Common.getBahasaConfig("Tidak ada soal pada Sub-CPMK ini.")).setParent(row);
			new Label("").setParent(row);
			new Label("").setParent(row);
			new Label("").setParent(row);
		}

		Row rowTotal = new Row();
		rowTotal.setStyle("background:#f1f5f9;");
		rowTotal.setParent(rows);
		new MyLabelBoldAja("").setParent(rowTotal);
		new MyLabelBoldAja("TOTAL " + formatNilai.getNama()).setParent(rowTotal);
		new MyLabelBoldAja(Common.numberFormat.get().format(totalSkor)).setParent(rowTotal);
		new MyLabelBoldAja(Common.numberFormat.get().format(totalMax)).setParent(rowTotal);

		new MyLabelKecil("Nilai " + formatNilai.getNama() + " = "
				+ Common.numberFormat.get().format(totalMax == 0.0 ? 0.0 : (totalSkor * 100.0) / totalMax) + "  (skor "
				+ Common.numberFormat.get().format(totalSkor) + " / " + Common.numberFormat.get().format(totalMax) + ")")
				.setParent(vb);

		try {
			window.onModal();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1799");
			// InterruptedException wajar saat modal ditutup; abaikan.
		}
	}

	/**
	 * Membuat tombol "Hasil OBE" yang ketika diklik menampilkan rekap capaian OBE
	 * per Sub-CPMK berdasarkan data ujian. Tombol hanya terlihat bila kurikulum perkuliahan
	 * menggunakan OBE.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan dosen melihat distribusi skor peserta per CPMK/Sub-CPMK
	 * dalam format tabel HTML dan mengunduhnya sebagai Excel. Data bersumber dari field
	 * {@code hasilJsonObe} yang diisi oleh {@link ProsesUjianHelper#hitungObe}.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Tombol hanya visible bila {@code kurikulum.apakahObe(...)} = true.</li>
	 *   <li>Klik memunculkan loading-bar dan memulai thread latar yang:</li>
	 *   <ul>
	 *     <li>Mengambil semua {@code HasilUjianMahasiswa} untuk ujian ini via Hibernate.</li>
	 *     <li>Mengurai {@code hasilJsonObe} dari setiap peserta.</li>
	 *     <li>Membangun tabel HTML agregasi per Sub-CPMK (skor rata-rata, distribusi capaian).</li>
	 *     <li>Membangun file Excel dengan data yang sama.</li>
	 *   </ul>
	 *   <li>Setelah thread selesai, callback menampilkan jendela modal 95%×98% dengan konten
	 *       HTML scroll dan tombol Download Excel.</li>
	 * </ol>
	 *
	 * <p><b>Thread-safety:</b> Gunakan array final ({@code htmlRef[]}, {@code xlsRef[]}) sebagai
	 * shared container antara thread latar dan callback ZK — aman karena label.setValue("")
	 * menciptakan happens-before.</p>
	 *
	 * @param pertemuanPunyaUjian konfigurasi ujian OBE yang hasilnya akan direkap
	 * @param ambil               callback untuk menyegarkan grid setelah perubahan data (jika diperlukan)
	 * @return {@code Toolbarbutton} siap pasang di toolbar, visible hanya bila kurikulum OBE
	 */
	public static Toolbarbutton hasilObe(final PertemuanPunyaUjian pertemuanPunyaUjian, final Ambildata ambil) {
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hasil OBE", "/img/svg/check2-circle.svg");
		cari.setVisible(pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null
				&& pertemuanPunyaUjian.getPertemuan().getPerkuliahan() != null
				&& pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getKurikulum() != null
				&& pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
						pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getTahunAjaran(),
						pertemuanPunyaUjian.getPertemuan().getPerkuliahan().getGanjilGenap()));
		cari.addEventListener("onClick", new EventListener() {
			/**
			 * Menjalankan laporan <b>Hasil OBE</b>: merekap capaian tiap peserta per CPMK dan
			 * Sub-CPMK, lengkap dengan kolom CPL turunan, baris rata-rata kelas, baris ambang
			 * (threshold), dan baris persentase mahasiswa lulus per CPMK.
			 *
			 * <p><b>Tiga wadah bersama {@code final}</b> menjembatani thread latar dengan callback
			 * ZK: {@code htmlRef[0]} menampung tabel HTML, {@code xlsRef[0]} menampung berkas
			 * Excel sebagai {@code byte[]}, dan {@code errorRef[0]} menampung penyebab kegagalan.
			 * {@code errorRef} berperan penting bagi pengalaman pengguna — bila pembuatan Excel
			 * gagal, tombol Download dapat menjelaskan APA yang salah (mis. Sub-CPMK tanpa kode
			 * atau mahasiswa tanpa program studi) alih-alih sekadar diam.</p>
			 *
			 * <p><b>Berbeda dari Analisis Butir Soal</b>, berkas Excel di sini disimpan di MEMORI
			 * sebagai {@code byte[]}, bukan ditulis ke berkas {@code /tmp}. Konsekuensinya tidak
			 * ada sampah berkas yang tertinggal, tetapi laporan berukuran sangat besar akan
			 * membebani heap.</p>
			 *
			 * <p>Tombol pemicunya sudah di-{@code setVisible(...)} hanya untuk perkuliahan
			 * ber-OBE, sehingga listener ini tidak perlu memeriksanya lagi.</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pembuatan bilah pemuatan
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String[] htmlRef  = new String[1];
				final byte[][] xlsRef   = new byte[1][];
				final Throwable[] errorRef = new Throwable[1];

				final Label label = Common.displayLoadBar(new EventListener() {
					/**
					 * Callback bilah pemuatan: membangun jendela hasil setelah thread latar
					 * selesai menghitung.
					 *
					 * <p>Menyusun {@link MyWindow} 98%&times;95% dengan {@code Borderlayout}:
					 * Center berisi {@code Div} bergulir yang menampung tabel HTML dari
					 * {@code htmlRef[0]} (atau pesan "Tidak ada data OBE." bila null), dan South
					 * berisi toolbar Tutup + Download Excel.</p>
					 *
					 * <p>Karena callback ini baru berjalan SETELAH label dikosongkan thread latar,
					 * seluruh wadah bersama dijamin sudah terisi — hubungan
					 * <i>happens-before</i> terbentuk oleh penyerahan event ke antrean ZK.</p>
					 *
					 * @param arg0 event penanda selesai; tidak dipakai
					 * @throws Exception diteruskan dari pembangunan komponen jendela
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow window = new MyWindow("Hasil OBE", "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("98%");

						Borderlayout bl = new ais.ui.util.MyBorderlayout();
						bl.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(bl);

						org.zkoss.zul.Div scrollDiv = new org.zkoss.zul.Div();
						scrollDiv.setStyle("overflow:auto;width:100%;height:100%;");
						scrollDiv.setParent(center);

						org.zkoss.zul.Html htmlPanel = new org.zkoss.zul.Html();
						htmlPanel.setContent(htmlRef[0] != null ? htmlRef[0]
								: "<div style='padding:16px;color:#888'>Tidak ada data OBE.</div>");
						htmlPanel.setParent(scrollDiv);

						South south = new South();
						south.setParent(bl);
						Toolbar toolbar = new Toolbar();
						toolbar.setParent(south);

						MyToolbarbuttonConfig btnClose = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						btnClose.addEventListener("onClick", new EventListener() {
							/**
							 * Tombol Tutup jendela Hasil OBE: melepas jendela beserta seluruh
							 * isinya. Wadah bersama {@code htmlRef}/{@code xlsRef} ikut menjadi
							 * sampah memori setelah jendela dilepas, sehingga berkas Excel yang
							 * disimpan di memori terbebas.
							 *
							 * @param e event {@code onClick}; tidak dipakai
							 * @throws Exception diteruskan dari pelepasan komponen
							 */
							@Override public void onEvent(Event e) throws Exception { window.detach(); }
						});
						btnClose.setParent(toolbar);

						MyToolbarbuttonConfig btnXls = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
						btnXls.addEventListener("onClick", new EventListener() {
							/**
							 * Tombol <b>Download Excel</b> untuk laporan Hasil OBE, dengan tiga
							 * jalur balasan yang berbeda menurut keadaan wadah bersama.
							 *
							 * <ol>
							 *   <li><b>{@code xlsRef[0]} terisi</b> — berkas dikirim sebagai
							 *       unduhan {@code hasil_obe.xlsx} lewat {@code Filedownload.save}
							 *       dari {@link java.io.ByteArrayInputStream} (berkas ada di
							 *       memori, bukan di disk).</li>
							 *   <li><b>{@code xlsRef[0]} null tetapi {@code errorRef[0]} terisi</b>
							 *       — pembuatan Excel GAGAL. Ditampilkan lewat
							 *       {@code PesanFormalHelper.tampilkanGagalException} beserta
							 *       empat langkah bantuan yang menyebut penyebab paling lazim:
							 *       data CPMK/Sub-CPMK tidak lengkap (kode atau nama kosong) dan
							 *       mahasiswa tanpa program studi.</li>
							 *   <li><b>Keduanya null</b> — proses belum selesai; pengguna diminta
							 *       menunggu sampai indikator pemuatan menghilang lalu mengeklik
							 *       kembali.</li>
							 * </ol>
							 *
							 * <p>Pembedaan jalur kedua dan ketiga inilah alasan {@code errorRef}
							 * ada: tanpanya, kegagalan permanen dan proses yang masih berjalan
							 * akan terlihat sama persis bagi pengguna, yaitu tombol yang seolah
							 * tidak merespons.</p>
							 *
							 * @param e event {@code onClick}; tidak dipakai
							 * @throws Exception diteruskan dari penyaluran unduhan
							 */
							@Override public void onEvent(Event e) throws Exception {
								if (xlsRef[0] != null) {
									Filedownload.save(new ByteArrayInputStream(xlsRef[0]),
										"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
										"hasil_obe.xlsx");
								} else {
									Throwable err = errorRef[0];
									if (err != null) {
										PesanFormalHelper.tampilkanGagalException(
											"membuat berkas Excel Hasil OBE",
											err instanceof Exception ? (Exception) err
												: new RuntimeException(err),
											new String[] {
												"Kemungkinan penyebab: data CPMK/Sub-CPMK tidak lengkap (kode atau nama kosong), atau ada mahasiswa tanpa program studi.",
												"Cek konfigurasi RPS OBE — pastikan setiap Sub-CPMK dan CPMK sudah memiliki kode dan nama.",
												"Tutup jendela ini, muat ulang halaman, lalu buka kembali Hasil OBE.",
												"Jika kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
											});
									} else {
										MyMessageboxConfig.show(
											"Berkas Excel belum selesai dibuat. Harap tunggu proses selesai (indikator loading menghilang) lalu klik Download Excel kembali.",
											"Harap Tunggu", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, null);
									}
								}
							}
						});
						btnXls.setParent(toolbar);

						window.setVisible(true);
						window.onModal();
					}
				});

				new Thread(new Runnable() {
					/**
					 * Thread latar penyusun laporan <b>Hasil OBE</b> — bagian terpanjang dari
					 * fitur ini. Menyiapkan metadata soal, memetakan CPMK dan CPL, memproses tiap
					 * peserta, lalu merakit tabel HTML dan workbook Excel.
					 *
					 * <h4>Tahap 1 — metadata soal</h4>
					 * <p>Mengambil daftar {@code UjianPunyaSoal} (batas 1000 soal), lalu untuk
					 * tiap nomor soal mencari {@link FormatNilai} pemetaannya lewat
					 * {@code pertemuanPunyaUjian.ambilMapNomor(formatNilais)} — sumber kebenaran
					 * yang SAMA dengan yang dipakai mesin penilaian. Dari situ dikumpulkan kode
					 * Sub-CPMK ({@code statusPertemuan.nama}), kode CPMK
					 * ({@code capaianPembelajaranLulusan.kode}), skor maksimal, dan kunci jawaban
					 * per soal. Label soal memakai nomor urut, kecuali pada ujian acak yang
					 * memakai cuplikan teks soal 8 karakter.</p>
					 *
					 * <h4>Tahap 2 — pemetaan CPL</h4>
					 * <p>Mengambil {@code CapaianLulusan} aktif milik program studi perkuliahan,
					 * lalu menautkannya ke CPMK dengan mencocokkan pola {@code ",<idCpmk>,"} di
					 * dalam kolom CSV {@code capaianPembelajaranLulusan}. Pagar koma di kedua sisi
					 * WAJIB agar id 1 tidak keliru cocok dengan id 11 atau 21. CPL yang tidak
					 * tertaut CPMK mana pun tidak dimasukkan sebagai kolom.</p>
					 *
					 * <h4>Tahap 3 — pemrosesan peserta</h4>
					 * <p>Peringkat dihitung dari himpunan nilai DISTINCT (pola yang sama dengan
					 * analisis butir soal), dan kelompok Atas/Tengah/Bawah ditentukan dengan
					 * belah-dua atas jumlah tingkat skor. Identitas peserta diambil dari salah
					 * satu dari empat jenis ({@code Mahasiswa}, {@code BiodataCalonMahasiswa} —
					 * memakai {@code prodiLulus} dengan cadangan {@code prodi1},
					 * {@code Siswa}, {@code CalonSiswa}). Skor per soal diakumulasi per
					 * {@code BankSoal} lalu dijumlahkan ke ember CPMK, dan persentase per CPMK
					 * ({@code gained/max*100}) dibandingkan dengan {@code cpmk.getMinimal()} untuk
					 * mencacah kelulusan. {@code session.clear()} setiap 50 peserta menahan
					 * pertumbuhan cache.</p>
					 *
					 * <h4>Tahap 4 — perakitan keluaran</h4>
					 * <p>Tabel HTML memakai header dua tingkat: baris pertama satu sel per CPMK
					 * yang di-{@code colspan} sebanyak Sub-CPMK-nya plus satu kolom sigma, baris
					 * kedua nama tiap Sub-CPMK. Sel CPMK diwarnai hijau/merah menurut apakah
					 * mencapai ambang. Bagian {@code tfoot} memuat baris Rata-rata, Threshold, dan
					 * % Mahasiswa Lulus. Seluruh teks dari basis data diloloskan lewat
					 * {@link #obeEsc(String)}. Hasilnya disimpan ke {@code htmlRef[0]}; workbook
					 * Excel disimpan ke {@code xlsRef[0]} sebagai {@code byte[]}.</p>
					 *
					 * <h4>Ketahanan</h4>
					 * <p>Kegagalan per peserta ditangkap sehingga satu baris rusak tidak
					 * membatalkan laporan. Kegagalan menyeluruh disimpan ke {@code errorRef[0]}
					 * agar tombol Download Excel dapat menjelaskan sebabnya. Konversi angka dari
					 * teks memakai {@link #parseIntObe(String)} yang mengembalikan {@code 0}
					 * alih-alih melempar, supaya satu sel non-numerik tidak menggagalkan seluruh
					 * workbook. Session ditutup di {@code finally}.</p>
					 *
					 * <p><b>{@code @SuppressWarnings("deprecation")}</b> diperlukan karena API
					 * POI/ZK tertentu yang dipakai perakitan workbook sudah ditandai usang.</p>
					 */
					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void run() {
						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							Object[] objects = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(
									true, pertemuanPunyaUjian, "", 0, 1000);
							List<Long> soalIds = (List<Long>) objects[0];

							List<FormatNilai> formatNilais = Common.getFormatNilais(
									pertemuanPunyaUjian.getPertemuan().getPerkuliahan(), true);
							TreeMap<Integer, FormatNilai> treeMap =
									pertemuanPunyaUjian.ambilMapNomor(formatNilais);

							// ── Soal metadata (indexed parallel to soalIds) ────────────
							int soalCount = 0;
							List<Long>   soalBankIds   = new ArrayList<Long>();
							List<String> soalLabels    = new ArrayList<String>();
							List<String> soalSubCpmk   = new ArrayList<String>();
							List<String> soalCpmkKode  = new ArrayList<String>();
							List<Double> soalMaxSkor   = new ArrayList<Double>();
							List<String> soalKunjawabn = new ArrayList<String>(); // kunci huruf

							// cpmkKode → CPMK obj (ordered)
							LinkedHashMap<String, CapaianPembelajaranLulusan> cpmkMap =
									new LinkedHashMap<String, CapaianPembelajaranLulusan>();
							// cpmkKode → ordered sub-cpmk kodes
							LinkedHashMap<String, LinkedHashSet<String>> cpmkToSubs =
									new LinkedHashMap<String, LinkedHashSet<String>>();

							// Letter distribution (for Data Soal sheet)
							TreeSet<String> hurufs = new TreeSet<String>();
							TreeMap<String, Integer> hurufsJawab   = new TreeMap<String, Integer>();
							TreeMap<Long, Integer>   jumlahBenar   = new TreeMap<Long, Integer>();
							TreeMap<Long, Integer>   jumlahSalah   = new TreeMap<Long, Integer>();

							int soalNo = 1;
							for (Long did : soalIds) {
								UjianPunyaSoal ups = (UjianPunyaSoal)
										GeneralValueObject.ambilData(UjianPunyaSoal.class, did.toString());
								if (ups == null) { soalNo++; continue; }

								FormatNilai fn = treeMap.get(soalNo);
								String subK = "", cpmkK = "";
								CapaianPembelajaranLulusan cpmkObj = null;
								if (fn != null) {
									if (fn.getStatusPertemuan() != null) {
										String snm = fn.getStatusPertemuan().getNama();
										subK = snm == null ? "" : snm.trim();
									}
									if (fn.getCapaianPembelajaranLulusan() != null) {
										cpmkObj = fn.getCapaianPembelajaranLulusan();
										String rawKode = cpmkObj.getKode();
										cpmkK = rawKode == null ? "" : rawKode.trim();
										if (!cpmkK.isEmpty()) {
											cpmkMap.put(cpmkK, cpmkObj);
											if (!cpmkToSubs.containsKey(cpmkK))
												cpmkToSubs.put(cpmkK, new LinkedHashSet<String>());
											if (!subK.isEmpty()) cpmkToSubs.get(cpmkK).add(subK);
										}
									}
								}

								double maxS = ups.getBankSoal().getSkor();

								String lbl = soalNo + "";
								if (!pertemuanPunyaUjian.getRandom()) { lbl = soalNo + ""; }
								else {
									try {
										lbl = Jsoup.parse(ups.getBankSoal().getSoal()).text();
										if (lbl.length() > 9) lbl = lbl.substring(0, 8) + "..";
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:1978");}
								}

								// Kunci jawaban
								StringBuilder kj = new StringBuilder();
								BankSoal bs = ups.getBankSoal();
								List<Long> bsds = bs.ambilBankSoalDetail(false);
								for (Long bsdId : bsds) {
									BankSoalDetail bsd = (BankSoalDetail)
											GeneralValueObject.ambilData(BankSoalDetail.class, bsdId.toString());
									if (bsd != null) {
										if (!bsd.getHuruf().isEmpty() && !bsd.getJawaban().trim().isEmpty())
											hurufs.add(bsd.getHuruf());
										if (bsd.getBetul()) {
											if (kj.length() > 0) kj.append(",");
											kj.append(bsd.getHuruf());
										}
									}
								}

								soalBankIds.add(bs.getId());
								soalLabels.add(lbl);
								soalSubCpmk.add(subK);
								soalCpmkKode.add(cpmkK);
								soalMaxSkor.add(maxS);
								soalKunjawabn.add(kj.toString());
								soalCount++;
								soalNo++;
							}

							// ── Load CPL (for CPL score columns) ─────────────────────
							LinkedHashMap<String, CapaianLulusan> cplMap =
									new LinkedHashMap<String, CapaianLulusan>();
							LinkedHashMap<String, List<String>> cplToCpmks =
									new LinkedHashMap<String, List<String>>();
							try {
								ais.database.model.Jurusan jur = pertemuanPunyaUjian.getPertemuan()
										.getPerkuliahan().getJurusan();
								if (jur != null) {
									List<CapaianLulusan> cpls = session
										.createCriteria(CapaianLulusan.class)
										.add(Restrictions.eq("jurusan", jur))
										.add(Restrictions.eq("aktif", true))
										.addOrder(Order.asc("kode")).list();
									for (CapaianLulusan cpl : cpls) {
										String ck = cpl.getKode().trim();
										if (ck.isEmpty()) continue;
										String csv = cpl.getCapaianPembelajaranLulusan();
										List<String> linked = new ArrayList<String>();
										for (Map.Entry<String, CapaianPembelajaranLulusan> e2
												: cpmkMap.entrySet()) {
											if (csv.contains("," + e2.getValue().getId() + ","))
												linked.add(e2.getKey());
										}
										if (!linked.isEmpty()) {
											cplMap.put(ck, cpl);
											cplToCpmks.put(ck, linked);
										}
									}
								}
							} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2038");}

							// ── Process students ──────────────────────────────────────
							Map<Long, Object[]> hasilMap = (Map<Long, Object[]>) ambil.ambil();

							TreeSet<Double> nilaiSet = new TreeSet<Double>(Collections.reverseOrder());
							for (Object[] a : hasilMap.values())
								nilaiSet.add(((HasilUjianMahasiswa) a[0]).getNilai());
							int jumlahPeserta = nilaiSet.size();

							// studentId → [nim, nama, prodi, nilaiStr, rankStr, kelompok]
							LinkedHashMap<Long, String[]> studentMeta =
									new LinkedHashMap<Long, String[]>();
							// studentId → rawScores[soalCount]
							LinkedHashMap<Long, double[]> studentSoalSkor =
									new LinkedHashMap<Long, double[]>();
							// studentId → rawHuruf[soalCount]
							LinkedHashMap<Long, String[]> studentSoalHuruf =
									new LinkedHashMap<Long, String[]>();
							// studentId → cpmkKode → [gained, max]
							LinkedHashMap<Long, LinkedHashMap<String, double[]>> studentCpmkSkor =
									new LinkedHashMap<Long, LinkedHashMap<String, double[]>>();
							// class CPMK sum: cpmkKode → [sumPct, count]
							LinkedHashMap<String, double[]> cpmkClassSum =
									new LinkedHashMap<String, double[]>();
							LinkedHashMap<String, int[]> cpmkPassCnt =
									new LinkedHashMap<String, int[]>();
							for (String k : cpmkMap.keySet()) {
								cpmkClassSum.put(k, new double[]{0, 0});
								cpmkPassCnt.put(k, new int[]{0});
							}

							int rowIdx = 1;
							for (Object[] a : hasilMap.values()) {
								try {
									HasilUjianMahasiswa him = (HasilUjianMahasiswa) a[0];
									label.setValue("Memproses " + him.toString()
											+ " (" + rowIdx + "/" + hasilMap.size() + ")");

									int rangking = 0;
									for (Double n : nilaiSet) {
										rangking++;
										if (Common.numberFormat.get().format(n).equals(
												Common.numberFormat.get().format(him.getNilai()))) break;
									}
									String kelompok = rangking <= jumlahPeserta / 2 ? "Atas"
											: rangking > (jumlahPeserta + 1) / 2 ? "Bawah" : "Tengah";

									String nim = "", nama = "", prodi = "";
									if (him.getMahasiswa() != null) {
										nim   = him.getMahasiswa().getNim();
										nama  = him.getMahasiswa().getNama();
										Jurusan jMhs = him.getMahasiswa().getJurusan();
										prodi = jMhs == null ? "" : jMhs.getNama();
									} else if (him.getBiodataCalonMahasiswa() != null) {
										nim   = him.getBiodataCalonMahasiswa().getNoRegistrasi();
										nama  = him.getBiodataCalonMahasiswa().getNama();
										Jurusan j2 = him.getBiodataCalonMahasiswa().getProdiLulus();
										if (j2 == null) j2 = him.getBiodataCalonMahasiswa().getProdi1();
										prodi = j2 == null ? "" : j2.getNama();
									} else if (him.getSiswa() != null) {
										nim   = him.getSiswa().getNomorInduk();
										nama  = him.getSiswa().getNama();
										prodi = him.getSiswa().getKelas() == null ? ""
												: him.getSiswa().getKelas().getNama();
									} else if (him.getCalonSiswa() != null) {
										nim   = him.getCalonSiswa().getNomorInduk();
										nama  = him.getCalonSiswa().getNama();
										GelombangPendaftaranPsb gel = him.getCalonSiswa().getGelombangPendaftaranPsb();
										prodi = gel == null ? "" : gel.getNama();
									}
									studentMeta.put(him.getId(), new String[]{
											nim, nama, prodi,
											Common.numberFormat.get().format(him.getNilai()),
											rangking + "", kelompok,
											him.getJawabanBenar() + "",
											(him.getJawabanBenarMax() - him.getJawabanBenar()) + "" });

									// Load detail for this student
									MyArrayList<Long> upsList = him.ambilUjianPunyaSoals(
											him.getPertemuanPunyaUjian().getJmlDitampilkan(),
											new Label(), true);
									Map<Long, Set<Long>> detailMap = him.ambilHasilUjianMahasiswaDetail(
											pertemuanPunyaUjian.getJmlDitampilkan(), upsList, false);

									// bankSoalId → [gained, huruf]
									TreeMap<Long, Double>  bsGained = new TreeMap<Long, Double>();
									TreeMap<Long, String>  bsHuruf  = new TreeMap<Long, String>();
									TreeMap<Long, String>  bsJawTxt = new TreeMap<Long, String>();

									for (Set<Long> detSet : detailMap.values()) {
										for (Long detId : detSet) {
											HasilUjianMahasiswaDetail det = (HasilUjianMahasiswaDetail)
													GeneralValueObject.ambilData(
													HasilUjianMahasiswaDetail.class, detId.toString());
											if (det == null || det.getBankSoal() == null) continue;
											Long bsId = det.getBankSoal().getId();

											double prev = bsGained.containsKey(bsId) ? bsGained.get(bsId) : 0.0;
											bsGained.put(bsId, prev + det.getNilai());

											String h = det.getBankSoalDetail() == null ? "-"
													: det.getBankSoalDetail().getHuruf();
											String cur = bsHuruf.containsKey(bsId) ? bsHuruf.get(bsId) : "";
											bsHuruf.put(bsId, cur.isEmpty() ? h : cur + "," + h);

											String s = det.getBankSoalDetail() == null ? ""
													: det.getBankSoalDetail().getJawaban();
											String curt = bsJawTxt.containsKey(bsId) ? bsJawTxt.get(bsId) : "";
											bsJawTxt.put(bsId, curt.isEmpty() ? s : curt + ";" + s);

											// Aggregate class letter distribution
											String key = bsId + "_" + h;
											Integer cnt2 = hurufsJawab.get(key);
											hurufsJawab.put(key, cnt2 == null ? 1 : cnt2 + 1);

											if (det.getBankSoalDetail() != null) {
												if (det.getBankSoalDetail().getBetul()) {
													Integer bc = jumlahBenar.get(bsId);
													jumlahBenar.put(bsId, bc == null ? 1 : bc + 1);
												} else {
													Integer sc = jumlahSalah.get(bsId);
													jumlahSalah.put(bsId, sc == null ? 1 : sc + 1);
												}
											}
										}
									}
									// Mark unanswered soal
									for (int si = 0; si < soalCount; si++) {
										Long bsId = soalBankIds.get(si);
										if (!bsGained.containsKey(bsId)) {
											String key = bsId + "_-";
											Integer cnt2 = hurufsJawab.get(key);
											hurufsJawab.put(key, cnt2 == null ? 1 : cnt2 + 1);
										}
									}

									// Build per-soal arrays and CPMK aggregates
									double[] rawSkor = new double[soalCount];
									String[] rawHuruf2 = new String[soalCount];
									Arrays.fill(rawHuruf2, "");

									LinkedHashMap<String, double[]> cpmkSkor =
											new LinkedHashMap<String, double[]>();
									for (String k : cpmkMap.keySet()) cpmkSkor.put(k, new double[]{0, 0});

									for (int si = 0; si < soalCount; si++) {
										Long bsId = soalBankIds.get(si);
										rawSkor[si]  = bsGained.containsKey(bsId) ? bsGained.get(bsId) : 0.0;
										rawHuruf2[si] = bsHuruf.containsKey(bsId)  ? bsHuruf.get(bsId)  : "";
										String ck = soalCpmkKode.get(si);
										if (!ck.isEmpty() && cpmkSkor.containsKey(ck)) {
											cpmkSkor.get(ck)[0] += rawSkor[si];
											cpmkSkor.get(ck)[1] += soalMaxSkor.get(si);
										}
									}

									studentSoalSkor.put(him.getId(), rawSkor);
									studentSoalHuruf.put(him.getId(), rawHuruf2);
									studentCpmkSkor.put(him.getId(), cpmkSkor);

									// Accumulate class CPMK stats
									for (Map.Entry<String, double[]> e2 : cpmkSkor.entrySet()) {
										String ck = e2.getKey();
										double[] gs = e2.getValue();
										if (gs[1] > 0) {
											double pct = gs[0] / gs[1] * 100.0;
											cpmkClassSum.get(ck)[0] += pct;
											cpmkClassSum.get(ck)[1] += 1;
											CapaianPembelajaranLulusan co = cpmkMap.get(ck);
											double minV = co != null && co.getMinimal() != null
													? co.getMinimal() : 0;
											if (pct >= minV) cpmkPassCnt.get(ck)[0]++;
										}
									}

									rowIdx++;
									if (rowIdx % 50 == 0) session.clear();
								} catch (Exception ex) { ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2215"); rowIdx++; }
							}
							hurufs.add("-");

							// ── Build HTML ─────────────────────────────────────────────
							String mkNama = "", ujianNama = "";
							try { mkNama = pertemuanPunyaUjian.getPertemuan().getPerkuliahan()
									.getKurikulumPunyaMatakuliah().getMatakuliah().getNama();
							} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2223");}
							try { ujianNama = pertemuanPunyaUjian.getUjian().getNama();
							} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2225");}

							StringBuilder html = new StringBuilder();
							html.append("<style>");
							html.append(".ot{border-collapse:collapse;font-size:12px;font-family:Arial,sans-serif;width:100%}");
							html.append(".ot th,.ot td{border:1px solid #cbd5e1;padding:3px 6px;white-space:nowrap}");
							html.append(".ot th{background:#1e3a8a;color:#fff;text-align:center}");
							html.append(".ot .hc{background:#1d4ed8}");
							html.append(".ot .hs{background:#3b82f6;font-size:11px}");
							html.append(".ot .hp{background:#6d28d9}");
							html.append(".ot .nr{text-align:right;font-family:monospace}");
							html.append(".ot .ct{text-align:center}");
							html.append(".ot .pa{background:#dcfce7;color:#166534;font-weight:600}");
							html.append(".ot .fa{background:#fee2e2;color:#991b1b}");
							html.append(".ot .ok{color:#16a34a;font-weight:700}");
							html.append(".ot .ng{color:#dc2626;font-weight:700}");
							html.append(".ot .ar td{background:#eff6ff}");
							html.append(".ot .tr td{background:#fefce8;font-size:11px;color:#854d0e}");
							html.append(".ot .pr td{background:#f0fdf4}");
							html.append(".ot .sr td{background:#e0f2fe}");
							html.append("</style>");
							html.append("<div style='padding:6px 10px 2px'>");
							html.append("<b style='font-size:13px;color:#1e3a8a'>")
								.append(obeEsc(mkNama)).append(" &mdash; ").append(obeEsc(ujianNama)).append("</b>");
							html.append("&nbsp; <span style='font-size:11px;color:#64748b'>")
								.append("Peserta: <b>").append(jumlahPeserta).append("</b>")
								.append(" | CPMK terukur: <b>").append(cpmkMap.size()).append("</b>")
								.append(" | CPL terkait: <b>").append(cplMap.size()).append("</b>")
								.append("</span></div>");

							html.append("<div style='padding:0 6px 12px;overflow-x:auto'>");
							html.append("<table class='ot'><thead>");

							// Header row 1
							html.append("<tr>");
							html.append("<th rowspan='2'>No.</th><th rowspan='2'>NIM</th>");
							html.append("<th rowspan='2' style='min-width:130px'>Nama</th>");
							html.append("<th rowspan='2' style='min-width:90px'>Prodi</th>");
							for (Map.Entry<String, CapaianPembelajaranLulusan> eC : cpmkMap.entrySet()) {
								String ck = eC.getKey();
								LinkedHashSet<String> subs = cpmkToSubs.get(ck);
								int span = (subs == null ? 0 : subs.size()) + 1;
								html.append("<th class='hc' colspan='").append(span)
									.append("' title='").append(obeEsc(eC.getValue().getNama())).append("'>")
									.append(obeEsc(ck));
								Double minV = eC.getValue().getMinimal();
								if (minV != null)
									html.append(" <small>(min:").append(minV.intValue()).append("%)</small>");
								html.append("</th>");
							}
							for (Map.Entry<String, CapaianLulusan> eP : cplMap.entrySet()) {
								html.append("<th class='hp' rowspan='2' title='")
									.append(obeEsc(eP.getValue().getNama())).append("'>")
									.append(obeEsc(eP.getKey())).append("</th>");
							}
							html.append("<th rowspan='2'>Total<br>Skor</th>");
							html.append("<th rowspan='2'>Rank</th><th rowspan='2'>Kel.</th></tr>");

							// Header row 2: sub-cpmk cols + Σ
							html.append("<tr>");
							for (String ck : cpmkMap.keySet()) {
								LinkedHashSet<String> subs = cpmkToSubs.get(ck);
								if (subs != null) for (String sub : subs) {
									html.append("<th class='hs' style='min-width:52px'>")
										.append(obeEsc(sub)).append("</th>");
								}
								html.append("<th class='hc' style='min-width:50px'><b>&Sigma;</b></th>");
							}
							html.append("</tr></thead><tbody>");

							int idx = 1;
							for (Map.Entry<Long, String[]> me : studentMeta.entrySet()) {
								Long sid = me.getKey();
								String[] meta = me.getValue();
								LinkedHashMap<String, double[]> cpmkSkor2 = studentCpmkSkor.get(sid);
								double[] rawSkor2 = studentSoalSkor.get(sid);
								html.append("<tr>");
								html.append("<td class='ct'>").append(idx).append("</td>");
								html.append("<td>").append(obeEsc(meta[0])).append("</td>");
								html.append("<td>").append(obeEsc(meta[1])).append("</td>");
								html.append("<td style='font-size:11px'>").append(obeEsc(meta[2])).append("</td>");
								for (String ck : cpmkMap.keySet()) {
									LinkedHashSet<String> subs = cpmkToSubs.get(ck);
									if (subs != null) for (String sub : subs) {
										double sg = 0, sm = 0;
										for (int si = 0; si < soalCount; si++) {
											if (soalCpmkKode.get(si).equals(ck)
													&& soalSubCpmk.get(si).equals(sub)) {
												sg += rawSkor2 != null ? rawSkor2[si] : 0;
												sm += soalMaxSkor.get(si);
											}
										}
										if (sm > 0) html.append("<td class='nr'>")
												.append(String.format("%.0f", sg/sm*100)).append("</td>");
										else html.append("<td class='ct'>-</td>");
									}
									double[] gs = cpmkSkor2 != null ? cpmkSkor2.get(ck) : null;
									if (gs != null && gs[1] > 0) {
										double pct = gs[0] / gs[1] * 100.0;
										Double minV = cpmkMap.get(ck).getMinimal();
										boolean pass = minV == null || pct >= minV;
										html.append("<td class='nr ").append(pass ? "pa" : "fa").append("'>")
											.append(String.format("%.1f", pct)).append("</td>");
									} else html.append("<td class='ct'>-</td>");
								}
								for (List<String> linked : cplToCpmks.values()) {
									double s2 = 0; int c2 = 0;
									for (String ck : linked) {
										double[] gs = cpmkSkor2 != null ? cpmkSkor2.get(ck) : null;
										if (gs != null && gs[1] > 0) { s2 += gs[0]/gs[1]*100; c2++; }
									}
									html.append(c2 > 0
										? "<td class='nr'>" + String.format("%.1f", s2/c2) + "</td>"
										: "<td class='ct'>-</td>");
								}
								html.append("<td class='nr'>").append(meta[3]).append("</td>");
								html.append("<td class='ct'>").append(meta[4]).append("</td>");
								html.append("<td class='ct'>").append(meta[5]).append("</td>");
								html.append("</tr>");
								idx++;
							}
							html.append("</tbody><tfoot>");

							// Rata-rata row
							html.append("<tr class='ar'><td class='ct' colspan='4'><b>Rata-rata</b></td>");
							for (String ck : cpmkMap.keySet()) {
								LinkedHashSet<String> subs = cpmkToSubs.get(ck);
								if (subs != null) for (String sub : subs) {
									double ss = 0; int sc = 0;
									for (double[] rs : studentSoalSkor.values()) {
										double sg2 = 0, sm2 = 0;
										for (int si = 0; si < soalCount; si++) {
											if (soalCpmkKode.get(si).equals(ck)
													&& soalSubCpmk.get(si).equals(sub)) {
												sg2 += rs[si]; sm2 += soalMaxSkor.get(si);
											}
										}
										if (sm2 > 0) { ss += sg2/sm2*100; sc++; }
									}
									html.append(sc > 0
										? "<td class='nr'>" + String.format("%.0f", ss/sc) + "</td>"
										: "<td class='ct'>-</td>");
								}
								double[] cs = cpmkClassSum.get(ck);
								html.append(cs != null && cs[1] > 0
									? "<td class='nr'><b>" + String.format("%.1f", cs[0]/cs[1]) + "</b></td>"
									: "<td class='ct'>-</td>");
							}
							for (Map.Entry<String, List<String>> ePL : cplToCpmks.entrySet()) {
								double s2 = 0; int c2 = 0;
								for (String ck : ePL.getValue()) {
									double[] cs = cpmkClassSum.get(ck);
									if (cs != null && cs[1] > 0) { s2 += cs[0]/cs[1]; c2++; }
								}
								html.append(c2 > 0
									? "<td class='nr'>" + String.format("%.1f", s2/c2) + "</td>"
									: "<td class='ct'>-</td>");
							}
							html.append("<td colspan='3'></td></tr>");

							// Threshold row
							html.append("<tr class='tr'><td colspan='4'><b>Threshold</b></td>");
							for (String ck : cpmkMap.keySet()) {
								LinkedHashSet<String> subs = cpmkToSubs.get(ck);
								int subN = subs == null ? 0 : subs.size();
								for (int i = 0; i < subN; i++) html.append("<td class='ct'>-</td>");
								Double minV = cpmkMap.get(ck).getMinimal();
								html.append("<td class='ct'>")
									.append(minV != null ? minV.intValue() + "%" : "-")
									.append("</td>");
							}
							for (int i = 0; i < cplMap.size() + 3; i++) html.append("<td></td>");
							html.append("</tr>");

							// % Lulus row
							html.append("<tr class='pr'><td colspan='4'><b>% Mhs Lulus</b></td>");
							for (String ck : cpmkMap.keySet()) {
								LinkedHashSet<String> subs = cpmkToSubs.get(ck);
								int subN = subs == null ? 0 : subs.size();
								for (int i = 0; i < subN; i++) html.append("<td class='ct'>-</td>");
								int[] pc = cpmkPassCnt.get(ck);
								double[] cs = cpmkClassSum.get(ck);
								int tot = cs != null ? (int) cs[1] : 0;
								html.append("<td class='ct'>")
									.append(tot > 0 ? String.format("%.0f%%", pc[0]*100.0/tot) : "-")
									.append("</td>");
							}
							for (int i = 0; i < cplMap.size() + 3; i++) html.append("<td></td>");
							html.append("</tr>");

							// Status row
							html.append("<tr class='sr'><td colspan='4'><b>Status CPMK</b></td>");
							for (String ck : cpmkMap.keySet()) {
								LinkedHashSet<String> subs = cpmkToSubs.get(ck);
								int subN = subs == null ? 0 : subs.size();
								for (int i = 0; i < subN; i++) html.append("<td></td>");
								double[] cs = cpmkClassSum.get(ck);
								Double minV = cpmkMap.get(ck).getMinimal();
								String sts;
								if (minV != null && cs != null && cs[1] > 0) {
									boolean ach = cs[0]/cs[1] >= minV;
									sts = ach ? "<span class='ok'>&#10003; Tercapai</span>"
											  : "<span class='ng'>&#10007; Belum</span>";
								} else { sts = "<span style='color:#94a3b8'>-</span>"; }
								html.append("<td class='ct'>").append(sts).append("</td>");
							}
							for (int i = 0; i < cplMap.size() + 3; i++) html.append("<td></td>");
							html.append("</tr></tfoot></table></div>");

							// Legend
							if (!cpmkMap.isEmpty()) {
								html.append("<div style='padding:2px 10px 10px;font-size:11px;color:#64748b'>");
								html.append("<b>Keterangan CPMK:</b> ");
								boolean first = true;
								for (Map.Entry<String, CapaianPembelajaranLulusan> eC : cpmkMap.entrySet()) {
									if (!first) html.append(" | ");
									html.append("<b>").append(obeEsc(eC.getKey())).append("</b>: ")
										.append(obeEsc(eC.getValue().getNama() == null
												? "" : eC.getValue().getNama()));
									first = false;
								}
								html.append("</div>");
							}
							htmlRef[0] = html.toString();

							// ── Build Excel ────────────────────────────────────────────
							XSSFWorkbook wb = new XSSFWorkbook();

							// Sheet 1: Rekap OBE
							XSSFSheet shObe = wb.createSheet("Rekap OBE");
							shObe.setDefaultColumnWidth(14);
							{
								// build header row 1
								int ri = 0;
								XSSFRow rh1 = shObe.createRow(ri++);
								int c = 0;
								rh1.createCell(c++).setCellValue("No.");
								rh1.createCell(c++).setCellValue("NIM");
								rh1.createCell(c++).setCellValue("Nama");
								rh1.createCell(c++).setCellValue("Prodi");
								for (String ck : cpmkMap.keySet()) {
									LinkedHashSet<String> subs = cpmkToSubs.get(ck);
									if (subs != null) for (String sub : subs)
										rh1.createCell(c++).setCellValue(sub);
									CapaianPembelajaranLulusan co = cpmkMap.get(ck);
									Double minV = co.getMinimal();
									rh1.createCell(c++).setCellValue(ck
											+ (minV != null ? " (min:" + minV.intValue() + "%)" : ""));
								}
								for (String ck : cplMap.keySet()) rh1.createCell(c++).setCellValue(ck);
								rh1.createCell(c++).setCellValue("Total Skor");
								rh1.createCell(c++).setCellValue("Rank");
								rh1.createCell(c++).setCellValue("Kelompok");

								// Data rows
								int di = 1;
								for (Map.Entry<Long, String[]> me : studentMeta.entrySet()) {
									Long sid = me.getKey();
									String[] meta = me.getValue();
									LinkedHashMap<String, double[]> cpmkSkor3 = studentCpmkSkor.get(sid);
									double[] rs3 = studentSoalSkor.get(sid);
									XSSFRow row = shObe.createRow(ri++);
									c = 0;
									row.createCell(c++).setCellValue(di++);
									row.createCell(c++).setCellValue(meta[0]);
									row.createCell(c++).setCellValue(meta[1]);
									row.createCell(c++).setCellValue(meta[2]);
									for (String ck : cpmkMap.keySet()) {
										LinkedHashSet<String> subs = cpmkToSubs.get(ck);
										if (subs != null) for (String sub : subs) {
											double sg = 0, sm = 0;
											for (int si = 0; si < soalCount; si++) {
												if (soalCpmkKode.get(si).equals(ck)
														&& soalSubCpmk.get(si).equals(sub)) {
													sg += rs3 != null ? rs3[si] : 0;
													sm += soalMaxSkor.get(si);
												}
											}
											row.createCell(c++).setCellValue(sm > 0
													? Double.parseDouble(String.format(java.util.Locale.US, "%.1f", sg/sm*100)) : 0);
										}
										double[] gs = cpmkSkor3 != null ? cpmkSkor3.get(ck) : null;
										row.createCell(c++).setCellValue(gs != null && gs[1] > 0
												? Double.parseDouble(String.format(java.util.Locale.US, "%.1f", gs[0]/gs[1]*100)) : 0);
									}
									for (List<String> linked : cplToCpmks.values()) {
										double s2 = 0; int c2 = 0;
										for (String ck : linked) {
											double[] gs = cpmkSkor3 != null ? cpmkSkor3.get(ck) : null;
											if (gs != null && gs[1] > 0) { s2 += gs[0]/gs[1]*100; c2++; }
										}
										row.createCell(c++).setCellValue(c2 > 0
												? Double.parseDouble(String.format(java.util.Locale.US, "%.1f", s2/c2)) : 0);
									}
									row.createCell(c++).setCellValue(meta[3]);
									row.createCell(c++).setCellValue(parseIntObe(meta[4]));
									row.createCell(c++).setCellValue(meta[5]);
								}

								// Summary rows (** styled)
								XSSFRow rAvg = shObe.createRow(ri++);
								rAvg.createCell(0).setCellValue("**Rata-rata");
								c = 4;
								for (String ck : cpmkMap.keySet()) {
									LinkedHashSet<String> subs = cpmkToSubs.get(ck);
									if (subs != null) for (String sub : subs) {
										double ss = 0; int sc = 0;
										for (double[] rrs : studentSoalSkor.values()) {
											double sg2 = 0, sm2 = 0;
											for (int si = 0; si < soalCount; si++) {
												if (soalCpmkKode.get(si).equals(ck)
														&& soalSubCpmk.get(si).equals(sub)) {
													sg2 += rrs[si]; sm2 += soalMaxSkor.get(si);
												}
											}
											if (sm2 > 0) { ss += sg2/sm2*100; sc++; }
										}
										rAvg.createCell(c++).setCellValue(sc > 0
												? Double.parseDouble(String.format(java.util.Locale.US, "%.1f", ss/sc)) : 0);
									}
									double[] cs = cpmkClassSum.get(ck);
									rAvg.createCell(c++).setCellValue(cs != null && cs[1] > 0
											? Double.parseDouble(String.format(java.util.Locale.US, "%.1f", cs[0]/cs[1])) : 0);
								}

								XSSFRow rThr = shObe.createRow(ri++);
								rThr.createCell(0).setCellValue("**Threshold");
								c = 4;
								for (String ck : cpmkMap.keySet()) {
									LinkedHashSet<String> subs = cpmkToSubs.get(ck);
									int subN = subs == null ? 0 : subs.size();
									for (int i = 0; i < subN; i++) rThr.createCell(c++).setCellValue("-");
									Double minV = cpmkMap.get(ck).getMinimal();
									rThr.createCell(c++).setCellValue(minV != null
											? minV.intValue() + "%" : "-");
								}

								XSSFRow rLul = shObe.createRow(ri++);
								rLul.createCell(0).setCellValue("**% Lulus");
								c = 4;
								for (String ck : cpmkMap.keySet()) {
									LinkedHashSet<String> subs = cpmkToSubs.get(ck);
									int subN = subs == null ? 0 : subs.size();
									for (int i = 0; i < subN; i++) rLul.createCell(c++).setCellValue("-");
									int[] pc = cpmkPassCnt.get(ck);
									double[] cs = cpmkClassSum.get(ck);
									int tot = cs != null ? (int) cs[1] : 0;
									rLul.createCell(c++).setCellValue(tot > 0
											? String.format("%.0f%%", pc[0]*100.0/tot) : "-");
								}

								XSSFRow rSts = shObe.createRow(ri++);
								rSts.createCell(0).setCellValue("**Status");
								c = 4;
								for (String ck : cpmkMap.keySet()) {
									LinkedHashSet<String> subs = cpmkToSubs.get(ck);
									int subN = subs == null ? 0 : subs.size();
									for (int i = 0; i < subN; i++) rSts.createCell(c++).setCellValue("-");
									double[] cs = cpmkClassSum.get(ck);
									Double minV = cpmkMap.get(ck).getMinimal();
									String sts = "-";
									if (minV != null && cs != null && cs[1] > 0)
										sts = cs[0]/cs[1] >= minV ? "Tercapai" : "Belum Tercapai";
									rSts.createCell(c++).setCellValue("**" + sts);
								}
								Common.setStyled(shObe);
							}

							// Sheet 2: Data Soal (existing format)
							XSSFSheet shSoal = wb.createSheet("Data Soal");
							shSoal.setDefaultColumnWidth(20);
							{
								int ri = 0;
								XSSFRow rh = shSoal.createRow(ri++);
								rh.createCell(0).setCellValue("No.");
								rh.createCell(1).setCellValue("NIM/Kode");
								rh.createCell(2).setCellValue("Nama");
								rh.createCell(3).setCellValue("Kelas/Prodi");
								int c = 4;
								for (int si = 0; si < soalCount; si++) {
									rh.createCell(c++).setCellValue(soalLabels.get(si));
								}
								rh.createCell(c).setCellValue("Benar");
								rh.createCell(c+1).setCellValue("Salah");
								rh.createCell(c+2).setCellValue("Skor");
								rh.createCell(c+3).setCellValue("Rank");
								rh.createCell(c+4).setCellValue("Kelompok");

								int di = 1;
								for (Map.Entry<Long, String[]> me : studentMeta.entrySet()) {
									Long sid = me.getKey();
									String[] meta = me.getValue();
									double[] rs3 = studentSoalSkor.get(sid);
									String[] rh2 = studentSoalHuruf.get(sid);
									XSSFRow row = shSoal.createRow(ri++);
									row.createCell(0).setCellValue(di++);
									row.createCell(1).setCellValue(meta[0]);
									row.createCell(2).setCellValue(meta[1]);
									row.createCell(3).setCellValue(meta[2]);
									c = 4;
									for (int si = 0; si < soalCount; si++) {
										XSSFCell cell = row.createCell(c++);
										String h = rh2 != null ? rh2[si] : "";
										cell.setCellValue(h.isEmpty() ? "" : h);
									}
									row.createCell(c).setCellValue(parseIntObe(meta[6]));
									row.createCell(c+1).setCellValue(parseIntObe(meta[7]));
									row.createCell(c+2).setCellValue(meta[3]);
									row.createCell(c+3).setCellValue(parseIntObe(meta[4]));
									row.createCell(c+4).setCellValue(meta[5]);
								}

								// Analysis rows
								int base = ri;
								shSoal.createRow(base).createCell(1).setCellValue("**Hasil Analisis");
								XSSFRow rSoalLbl = shSoal.createRow(base+1);
								rSoalLbl.createCell(1).setCellValue("**Soal");
								XSSFRow rKunci = shSoal.createRow(base+2);
								rKunci.createCell(1).setCellValue("**Kunci Jawaban");
								c = 4;
								for (int si = 0; si < soalCount; si++) {
									rSoalLbl.createCell(c).setCellValue("**" + soalLabels.get(si));
									rKunci.createCell(c).setCellValue("**" + soalKunjawabn.get(si));
									c++;
								}
								int pen = 3;
								for (String h : hurufs) {
									XSSFRow rH = shSoal.createRow(base + pen++);
									rH.createCell(1).setCellValue("**" + (h.equals("-") ? "Tidak Dijawab"
											: "Jawaban " + h));
									c = 4;
									for (int si = 0; si < soalCount; si++) {
										Integer cnt2 = hurufsJawab.get(soalBankIds.get(si) + "_" + h);
										rH.createCell(c++).setCellValue("**" + (cnt2 == null ? 0 : cnt2));
									}
								}
								XSSFRow rBe = shSoal.createRow(base + pen++);
								rBe.createCell(1).setCellValue("**Jawaban Benar");
								XSSFRow rSa = shSoal.createRow(base + pen++);
								rSa.createCell(1).setCellValue("**Jawaban Salah");
								XSSFRow rSub = shSoal.createRow(base + pen++);
								rSub.createCell(1).setCellValue("**Sub-CPMK");
								XSSFRow rCpmkR = shSoal.createRow(base + pen++);
								rCpmkR.createCell(1).setCellValue("**CPMK");
								c = 4;
								for (int si = 0; si < soalCount; si++) {
									Integer be = jumlahBenar.get(soalBankIds.get(si));
									Integer sa = jumlahSalah.get(soalBankIds.get(si));
									rBe.createCell(c).setCellValue("**" + (be == null ? 0 : be));
									rSa.createCell(c).setCellValue("**" + (sa == null ? 0 : sa));
									rSub.createCell(c).setCellValue("**" + soalSubCpmk.get(si));
									rCpmkR.createCell(c).setCellValue("**" + soalCpmkKode.get(si));
									c++;
								}
								Common.setStyled(shSoal);
							}

							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							wb.write(baos);
							xlsRef[0] = baos.toByteArray();

						} catch (Exception e) {
							errorRef[0] = e;
							ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2687");
						} finally {
							if (session != null && session.isOpen()) {
								try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2690");}
							}
						}
						label.setValue("");
					}
				}).start();
			}
		});
		return cari;
	}

	/**
	 * Meng-escape karakter HTML sensitif khusus untuk konten tabel OBE.
	 * Versi sederhana tanpa escape tanda kutip tunggal, cukup untuk konten sel tabel.
	 *
	 * @param s string yang akan di-escape; null menghasilkan ""
	 * @return string dengan &amp;, &lt;, &gt;, &quot; sudah di-escape
	 */
	private static String obeEsc(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;");
	}

	/**
	 * Mengonversi {@code String} menjadi {@code int} dengan AMAN khusus untuk pembuatan
	 * file Excel "Hasil OBE".
	 *
	 * <p><b>Latar belakang masalah:</b> Pembuatan workbook Excel pada {@code hasilObe(...)}
	 * mengisi kolom <i>Rank</i>, <i>Benar</i>, dan <i>Salah</i> dari array {@code meta[]}
	 * yang nilainya berupa teks. Sebelumnya nilai-nilai itu langsung dikonversi dengan
	 * {@link Integer#parseInt(String)}. Bila satu saja baris peserta memiliki nilai non-numerik
	 * (mis. kosong, tanda "-", atau spasi karena peserta belum dinilai), {@code parseInt} akan
	 * melempar {@link NumberFormatException}. Karena seluruh proses pembuatan Excel dibungkus
	 * satu blok {@code try-catch} di thread latar, exception tersebut menggagalkan SELURUH
	 * workbook sehingga {@code xlsRef[0]} tetap {@code null}; akibatnya tombol "Download Excel"
	 * tampak tidak merespon (handler hanya menyimpan file bila {@code xlsRef[0] != null}).</p>
	 *
	 * <p><b>Solusi:</b> Metode ini menggantikan {@code Integer.parseInt(meta[...])} sehingga
	 * nilai yang tidak dapat di-parse dikembalikan sebagai {@code 0} (nilai aman dan netral
	 * untuk kolom angka), bukan melempar exception. Dengan demikian pembuatan workbook tidak
	 * pernah batal hanya karena satu sel non-numerik, dan tombol Download Excel selalu berfungsi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Gunakan metode ini untuk SEMUA konversi kolom angka opsional pada
	 * ekspor OBE. Bila di masa depan ada kolom angka baru yang berasal dari teks {@code meta[]},
	 * konversikan melalui metode ini agar ketangguhan tetap terjaga. {@code null} dan string
	 * kosong sama-sama menghasilkan {@code 0}.</p>
	 *
	 * @param s teks yang akan dikonversi (boleh {@code null} atau kosong)
	 * @return nilai integer hasil parse, atau {@code 0} bila {@code s} null/kosong/non-numerik
	 */
	private static int parseIntObe(String s) {
		if (s == null) return 0;
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/** Kategori DP: soal belum dijawab siapa pun ({@code totalJawab == 0}). */
	private static final int DP_KAT_BLM_DIKERJAKAN = 0;
	/** Kategori DP: sudah dijawab tapi tidak ada kelompok "Atas" ({@code jumlahAtas == 0}), DP tak terdefinisi. */
	private static final int DP_KAT_TIDAK_DAPAT_DIHITUNG = 1;
	/** Kategori DP: {@code DP < 0.20}. */
	private static final int DP_KAT_GANTI = 2;
	/** Kategori DP: {@code 0.20 <= DP < 0.30}. */
	private static final int DP_KAT_PERLU_REVISI = 3;
	/** Kategori DP: {@code 0.30 <= DP < 0.40}. */
	private static final int DP_KAT_BAIK = 4;
	/** Kategori DP: {@code DP >= 0.40}. */
	private static final int DP_KAT_SANGAT_BAIK = 5;

	/**
	 * <h3>Sumber ambang kategori Daya Pembeda TUNGGAL — dipakai dashboard dan Excel</h3>
	 *
	 * <p><b>Latar belakang.</b> {@link #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)}
	 * menghasilkan dua keluaran (dashboard HTML dan berkas Excel) dari satu perhitungan DP. Sebelum
	 * method ini ada, tiap keluaran menulis ulang sendiri ambang 0.40/0.30/0.20 secara terpisah dan
	 * keduanya diam-diam menyimpang: Excel memakai batas "Gunakan" di DP &ge; 0.40, sedangkan
	 * dashboard (dan panduan di bawah tabelnya) memakai DP &ge; 0.30. Akibatnya soal dengan DP pada
	 * pita 0.30&ndash;0.399 tampil "Baik/Layak Pakai" di layar tetapi "Revisi" di berkas unduhan.
	 * Jalur Excel juga membagi {@code jumlahAtas} tanpa penjagaan sehingga saat seluruh peserta
	 * bernilai sama ({@code jumlahAtas == 0}) hasil baginya {@code NaN} dan jatuh ke cabang
	 * "Gunakan" — soal yang sama sekali tidak bisa membedakan siapa pun malah tercatat layak pakai.</p>
	 *
	 * <p><b>Kontrak.</b> KEDUA jalur WAJIB memanggil method ini untuk menentukan kategori DP alih-alih
	 * menulis ulang perbandingan ambangnya sendiri, dan KEDUA jalur wajib menjaga pembagian dengan
	 * {@code adaKelompokAtas} sebelum menghitung {@code dp} itu sendiri (lihat pemanggil). Bila ambang
	 * ini perlu diubah di masa depan, ubah SATU kali di sini — jangan pernah menduplikasinya lagi.</p>
	 *
	 * @param dp             nilai Daya Pembeda yang sudah dihitung (harus {@code 0.0}, BUKAN
	 *                       {@code NaN}, bila {@code adaKelompokAtas} false — lihat pemanggil)
	 * @param sudahDijawab   {@code false} bila soal ini sama sekali belum dijawab siapa pun
	 *                       ({@code totalJawab == 0} di dashboard, atau {@code benar==0 && salah==0}
	 *                       di Excel — keduanya ekuivalen)
	 * @param adaKelompokAtas {@code false} bila {@code jumlahAtas == 0} (seluruh peserta bernilai
	 *                        sama sehingga tidak ada yang masuk kelompok "Atas"), membuat DP tak
	 *                        terdefinisi terlepas dari nilai {@code dp} yang diteruskan
	 * @return salah satu konstanta {@code DP_KAT_*} di atas
	 */
	private static int kategoriDayaPembeda(double dp, boolean sudahDijawab, boolean adaKelompokAtas) {
		if (!sudahDijawab) return DP_KAT_BLM_DIKERJAKAN;
		if (!adaKelompokAtas) return DP_KAT_TIDAK_DAPAT_DIHITUNG;
		if (dp >= 0.40) return DP_KAT_SANGAT_BAIK;
		if (dp >= 0.30) return DP_KAT_BAIK;
		if (dp >= 0.20) return DP_KAT_PERLU_REVISI;
		return DP_KAT_GANTI;
	}

	/**
	 * <b>Overload kompatibilitas (2 argumen)</b> dari
	 * {@link #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)}: meneruskan
	 * {@code null} sebagai {@code ambilJumlahPeserta} sehingga kartu "Peserta Ujian" pada
	 * dashboard memakai ukuran map hasil ujian ({@code hasilUjianMahasiswas.size()}) — yaitu
	 * jumlah peserta yang PUNYA baris hasil ujian, bukan jumlah peserta terdaftar.
	 *
	 * <p><b>Pemakai lintas modul.</b> Overload inilah yang dipanggil oleh
	 * {@code HasilUjianSiswaHelper} (domain sekolah) dan
	 * {@code PenjaminanMutuAnalisisHelper}. Keduanya <b>meminjam langsung</b> mesin analisis
	 * milik kelas ini alih-alih menduplikasinya, sehingga perubahan apa pun pada rumus TK/DP di
	 * overload 3 argumen akan ikut mengubah laporan analisis butir soal di modul sekolah dan
	 * modul penjaminan mutu. Perlakukan kedua method itu sebagai API publik lintas modul:
	 * jangan mengubah tanda tangan, kontrak {@code Ambildata}, maupun ambang kategori tanpa
	 * memeriksa ketiga pemanggil.</p>
	 *
	 * <p><b>Konsekuensi angka.</b> Karena {@code ambilJumlahPeserta} {@code null}, maka
	 * {@code jumlahPeserta} = ukuran map hasil ujian. Ini memengaruhi tiga besaran turunan:
	 * kolom "Kosong" per soal, rata-rata nilai pada kartu ringkasan, dan penyebut batang
	 * distribusi pilihan. Untuk menyelaraskan angka "Peserta Ujian" dengan "Jumlah Peserta"
	 * di tab Statistik (peserta TERDAFTAR, termasuk yang tidak hadir), gunakan overload
	 * 3 argumen.</p>
	 *
	 * <p><b>Catatan pemanggil {@code PenjaminanMutuAnalisisHelper}.</b> Modul itu meneruskan
	 * {@code Ambildata} yang mengembalikan {@code null}. Thread latar melakukan
	 * {@code ((Map) ambil.ambil()).values()} tanpa penjagaan null, sehingga jalur tersebut
	 * berakhir pada {@code NullPointerException} yang tertangkap {@code catch} terluar. Karena
	 * {@code label.setValue("")} berada DI DALAM blok {@code try} (bukan di {@code finally}),
	 * bilah pemuatan tidak pernah dibersihkan dan jendela hasil tidak pernah terbuka. Pemanggil
	 * WAJIB menyediakan {@code Ambildata} yang mengembalikan {@code Map<Long, Object[]>} yang
	 * sudah terisi — lihat pola pemakaian di {@link #display(PertemuanPunyaUjian, Component)}.</p>
	 *
	 * @param pertemuanPunyaUjian ujian yang butir soalnya akan dianalisis
	 * @param ambil               penyedia {@code Map<Long, Object[]>} hasil ujian seluruh
	 *                            peserta; TIDAK boleh mengembalikan {@code null}
	 * @return {@code Toolbarbutton} "Analisis Butir Soal" siap dipasang ke toolbar
	 * @see #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)
	 */
	public static Toolbarbutton analsisButirSoal(final PertemuanPunyaUjian pertemuanPunyaUjian, final Ambildata ambil) {
		return analsisButirSoal(pertemuanPunyaUjian, ambil, null);
	}

	/**
	 * <b>Tujuan:</b> Membuat tombol toolbar <b>"Analisis Butir Soal"</b> yang, ketika diklik,
	 * menjalankan analisis psikometrik klasik (<i>classical item analysis</i>) atas seluruh butir
	 * soal ujian dan menyajikannya dalam jendela modal dua tab: dashboard visual HTML dan
	 * spreadsheet lengkap yang juga dapat diunduh sebagai berkas {@code .xlsx}.
	 *
	 * <p>Method ini adalah <b>mesin analisis butir soal tunggal</b> untuk seluruh aplikasi.
	 * Modul sekolah ({@code HasilUjianSiswaHelper}) dan modul penjaminan mutu
	 * ({@code PenjaminanMutuAnalisisHelper}) tidak memiliki implementasi sendiri melainkan
	 * memanggil overload 2 argumen di atas. Perlakukan seluruh ambang kategori dan rumus di
	 * bawah ini sebagai kontrak lintas modul.</p>
	 *
	 * <h3>1. Struktur eksekusi</h3>
	 * <ol>
	 *   <li>Tombol dibuat dan SELALU visible (tidak ada penjagaan hak akses pada tombol itu
	 *       sendiri; kelayakan ditentukan oleh konteks toolbar tempat ia dipasang).</li>
	 *   <li>Klik menyiapkan wadah bersama {@code final}: {@code soalAnalisisList}
	 *       ({@code List<String[12]>} per soal), {@code statsGlobal} ({@code int[9]}),
	 *       {@code nilaiGlobal} ({@code double[1]}), plus {@code Intbox} penampung dimensi sheet.
	 *       Wadah ini ditulis thread latar dan dibaca callback ZK — aman karena
	 *       {@code label.setValue("")} di akhir thread membentuk <i>happens-before</i> terhadap
	 *       antrean event ZK.</li>
	 *   <li>{@code Common.displayLoadBar(callback)} menampilkan bilah pemuatan; {@code callback}
	 *       baru dijalankan setelah nilai label dikosongkan.</li>
	 *   <li>Thread latar membuka session Hibernate terdedikasi, membangun workbook XSSF,
	 *       menghitung analisis, menulis berkas ke {@code /tmp/data_<timestamp>.xlsx} di
	 *       {@code realPath} aplikasi, mengisi {@code intbox}/{@code colsbox}, lalu mengosongkan
	 *       label.</li>
	 *   <li>{@code callback} membangun {@link MyWindow} 95%&times;94% berisi {@link Tabbox}
	 *       dua tab. Tab 1 "Dashboard Analisis Butir Soal" merender
	 *       {@link #buildAnalisisVisualHtml(java.util.List, int[], double[])}; tab 2
	 *       "Data Lengkap (Spreadsheet)" menampilkan berkas xlsx. Toolbar berisi Tutup dan
	 *       "Download Excel Lengkap".</li>
	 * </ol>
	 *
	 * <h3>2. Pengelompokan peserta Atas/Tengah/Bawah — INI BUKAN 27%</h3>
	 * <p>Dokumentasi lama menyatakan pengelompokan memakai kaidah klasik "27% teratas vs 27%
	 * terbawah". <b>Itu tidak benar untuk implementasi ini.</b> Yang sebenarnya terjadi:</p>
	 * <ol>
	 *   <li>{@code treeMapRangking} adalah {@code TreeSet<Double>} berurutan MENURUN yang berisi
	 *       nilai-nilai <b>DISTINCT</b> peserta — bukan daftar peserta. Ukurannya disimpan sebagai
	 *       {@code jumlahTingkatSkor}, yaitu <b>banyaknya tingkat skor berbeda</b>, bukan
	 *       banyaknya peserta.</li>
	 *   <li>{@code rangking} seorang peserta = posisi 1-basis nilainya di dalam himpunan distinct
	 *       tersebut. Pembandingannya memakai {@code Common.numberFormat.format(...)} (perbandingan
	 *       STRING hasil format, bukan {@code double}), sehingga dua nilai yang berbeda di digit
	 *       yang tidak ditampilkan akan dianggap satu peringkat.</li>
	 *   <li>Pembagian kelompok adalah <b>belah-dua (median split)</b> atas tingkat skor:
	 *       {@code "Atas"} bila {@code rangking <= jumlahTingkatSkor / 2};
	 *       {@code "Bawah"} bila {@code rangking > (jumlahTingkatSkor + 1) / 2};
	 *       selain itu {@code "Tengah"}. Untuk jumlah tingkat genap tidak ada kelompok Tengah;
	 *       untuk ganjil tepat satu tingkat menjadi Tengah.</li>
	 * </ol>
	 * <p><b>Implikasi metodologis yang harus disadari.</b> Karena pembelahan dilakukan atas
	 * TINGKAT SKOR dan bukan atas PESERTA, jumlah peserta di kelompok Atas dan Bawah hampir
	 * selalu tidak sama — terlebih bila banyak peserta bernilai kembar. Padahal penyebut rumus
	 * DP di bawah hanya memakai jumlah peserta kelompok Atas. Akibatnya nilai DP dapat keluar
	 * dari rentang teoretis [-1, +1] ketika kelompok Bawah lebih gemuk daripada kelompok Atas.
	 * Perlakukan DP di sini sebagai indikator relatif untuk mengurutkan kualitas soal, bukan
	 * sebagai koefisien yang dapat dibandingkan dengan tabel baku psikometri.</p>
	 *
	 * <h3>3. Tingkat Kesukaran (TK / p)</h3>
	 * <p>Untuk tiap soal dihitung dari dua pencacah yang diakumulasi saat pemindaian jawaban:</p>
	 * <pre>
	 *   totalJawab = jumlahBenar[soal] + jumlahSalah[soal]
	 *   TK = totalJawab &gt; 0 ? jumlahBenar[soal] / totalJawab : 0.0
	 *   kosong = max(0, jumlahPeserta - totalJawab)
	 * </pre>
	 * <p>Dua hal penting yang berbeda dari rumus baku:</p>
	 * <ul>
	 *   <li><b>Penyebutnya adalah yang MENJAWAB, bukan seluruh peserta.</b> Soal yang dilewati
	 *       hampir semua orang namun dijawab benar oleh satu peserta akan memperoleh TK = 1.00
	 *       dan dikategorikan "Mudah". Kolom "Kosong" pada tabel harus selalu dibaca berdampingan
	 *       dengan TK agar tidak salah simpul.</li>
	 *   <li><b>Pencacahnya adalah BARIS RINCIAN JAWABAN, bukan peserta.</b>
	 *       {@code jumlahBenar}/{@code jumlahSalah} dinaikkan sekali untuk setiap
	 *       {@code HasilUjianMahasiswaDetail}. Pada soal berjawaban ganda (satu peserta memilih
	 *       beberapa opsi) satu peserta menyumbang beberapa cacahan, sehingga {@code totalJawab}
	 *       dapat melebihi jumlah peserta dan {@code kosong} terpangkas menjadi 0 oleh
	 *       {@code Math.max}.</li>
	 * </ul>
	 * <p>Kategori TK (identik antara dashboard dan Excel):
	 * {@code totalJawab == 0} &rarr; <b>"Blm dikerjakan"</b>;
	 * {@code TK &gt; 0.70} &rarr; <b>Mudah</b>;
	 * {@code 0.30 &le; TK &le; 0.70} &rarr; <b>Sedang</b>;
	 * {@code TK &lt; 0.30} &rarr; <b>Sulit</b>.</p>
	 *
	 * <h3>4. Daya Pembeda (DP / D)</h3>
	 * <pre>
	 *   jumlahAtas          = banyaknya PESERTA berkelompok "Atas"
	 *   jumlahPosisi[s+"_Atas"]  = banyaknya BARIS jawaban BENAR pada soal s dari peserta Atas
	 *   jumlahPosisi[s+"_Bawah"] = banyaknya BARIS jawaban BENAR pada soal s dari peserta Bawah
	 *   DP = jumlahAtas &gt; 0 ? (atasBenar - bawahBenar) / jumlahAtas : 0.0
	 * </pre>
	 * <p>Peserta berkelompok "Tengah" tidak ikut menyumbang pembilang mana pun — sesuai kaidah
	 * item analysis yang membuang kelompok tengah.</p>
	 * <p><b>Kategori DP (identik antara dashboard dan Excel, lihat
	 * {@link #kategoriDayaPembeda(double, boolean, boolean)}):</b>
	 * {@code totalJawab == 0} &rarr; <b>"Blm dikerjakan"</b>;
	 * {@code jumlahAtas == 0} (seluruh peserta bernilai sama, DP tak terdefinisi) &rarr;
	 * <b>"Tidak dapat dihitung"</b>;
	 * {@code DP &ge; 0.40} &rarr; <b>"Sangat Baik"</b>;
	 * {@code DP &ge; 0.30} &rarr; <b>"Baik"</b>;
	 * {@code DP &ge; 0.20} &rarr; <b>"Perlu Revisi"</b>;
	 * selain itu &rarr; <b>"Ganti"</b>. Tidak ada kategori "Cukup", "Jelek", maupun "Negatif" —
	 * DP negatif jatuh ke "Ganti". Pada Excel, "Sangat Baik" dan "Baik" sama-sama dicetak
	 * "GREENGunakan" (satu-satunya bucket "layak pakai" di sana), "Perlu Revisi" dicetak
	 * "YELLOWRevisi", dan "Ganti" dicetak "REDGanti"; "Blm dikerjakan" dan "Tidak dapat
	 * dihitung" dicetak apa adanya tanpa awalan warna.</p>
	 * <p><b>Riwayat perbaikan (sebelumnya menyimpang, sekarang satu sumber ambang).</b>
	 * Dahulu baris "Kriteria" pada sheet Excel menulis ulang ambangnya sendiri dengan batas
	 * "Gunakan" di {@code DP &ge; 0.40} (bukan {@code 0.30} seperti dashboard), sehingga soal
	 * dengan DP pada pita 0.30&ndash;0.399 tampil "Baik/Layak Pakai" di layar tetapi "Revisi"
	 * di berkas unduhan. Jalur Excel juga membagi {@code jumlahAtas} tanpa penjagaan
	 * {@code jumlahAtas > 0}, sehingga saat SELURUH peserta bernilai sama
	 * ({@code jumlahTingkatSkor == 1}) hasil baginya {@code NaN} dan (karena semua perbandingan
	 * {@code NaN < ambang} bernilai {@code false}) SEMUA soal tercetak "GREENGunakan" walau
	 * sama sekali tidak membedakan siapa pun. Kedua cacat ini ditutup dengan memindahkan
	 * ambang dan penjagaan pembagian ke {@link #kategoriDayaPembeda(double, boolean, boolean)}
	 * yang dipakai KEDUA jalur — jangan pernah menulis ulang ambang 0.40/0.30/0.20 secara
	 * terpisah lagi di salah satu jalur.</p>
	 *
	 * <h3>5. Rekomendasi soal — hanya dari DP</h3>
	 * <p>Pencacah {@code statsGlobal[2..4]} (Gunakan / Perlu Revisi / Ganti) yang menjadi dasar
	 * donat "Kualitas Soal" dan kartu "Soal Layak Pakai" dinaikkan <b>semata-mata dari kategori
	 * DP</b>. Tingkat Kesukaran hanya mengisi pencacah terpisah {@code statsGlobal[5..7]}
	 * (Mudah/Sedang/Sulit) dan tidak ikut menentukan rekomendasi. Dokumentasi lama yang menyebut
	 * "rekomendasi berdasarkan kombinasi TK dan DP" keliru.</p>
	 *
	 * <h3>6. Arti {@code statsGlobal} dan {@code soalAnalisisList}</h3>
	 * <p>{@code statsGlobal} ({@code int[9]}): {@code [0]} peserta, {@code [1]} total soal,
	 * {@code [2]} gunakan, {@code [3]} revisi, {@code [4]} ganti, {@code [5]} mudah,
	 * {@code [6]} sedang, {@code [7]} sulit, {@code [8]} peserta yang sudah ikut ujian
	 * (elemen {@code [1]} pada nilai map berupa koleksi tak kosong). {@code nilaiGlobal[0]}
	 * berisi rata-rata nilai = {@code totalNilaiSum / jumlahPeserta}.</p>
	 * <p>{@code soalAnalisisList} berisi {@code String[12]} per soal:
	 * {@code [0]} nomor soal, {@code [1]} teks soal (sudah di-{@code Jsoup}-kan dan dipangkas
	 * 62 karakter), {@code [2]} kunci, {@code [3]} benar, {@code [4]} salah, {@code [5]} kosong,
	 * {@code [6]} nilai TK, {@code [7]} kategori TK, {@code [8]} nilai DP, {@code [9]} kategori
	 * DP, {@code [10]} HTML batang distribusi pilihan, {@code [11]} nama Sub-CPMK.</p>
	 *
	 * <h3>7. Pemetaan Sub-CPMK</h3>
	 * <p>Kolom "Kesesuaian Sub-CPMK" dibangun dari JSON {@code pertemuanPunyaUjian.getFormatNilais()}
	 * yang memetakan {@code idFormatNilai -> "1,2,5-8"}. Notasi <b>RENTANG</b> {@code a-b}
	 * didukung agar selaras dengan {@code PertemuanPunyaUjian.ambilMapNomor} yang dipakai saat
	 * penilaian; bila hanya angka satuan yang diurai, sebagian soal akan tampak tidak terpetakan
	 * padahal tetap dinilai. Satu nomor soal boleh dipetakan ke beberapa Sub-CPMK — namanya
	 * digabung dengan koma. Nomor soal untuk pencocokan diambil dari {@code getNomorUrut()},
	 * kecuali pada ujian acak ({@code getRandom()}) yang memakai nomor urut tampilan.</p>
	 *
	 * <h3>8. Batang distribusi pilihan jawaban</h3>
	 * <p>Himpunan huruf opsi ({@code hurufs}) dikumpulkan secara <b>global untuk seluruh soal</b>,
	 * lalu ditambah entri {@code "-"} untuk "tidak dijawab". Konsekuensinya, soal yang hanya
	 * memiliki opsi A&ndash;D tetap menampilkan batang E bernilai 0% bila ada soal lain yang
	 * memiliki opsi E. Persentase batang dinormalisasi memakai
	 * {@link #persenDistribusiSeratus(int[], int)} (metode sisa terbesar) agar totalnya tepat
	 * 100% dan tidak tampak "kurang 1&ndash;2%" akibat pembulatan ke bawah per batang.</p>
	 *
	 * <h3>9. Susunan berkas Excel</h3>
	 * <p>Baris 0 adalah header (No./Kode/Nama/Kelas-Prodi, satu kolom per soal, lalu Benar/Salah/
	 * Skor/Rangking/Kelompok Rangking). Setiap peserta menempati satu baris; sel jawaban berisi
	 * huruf pilihan dengan komentar sel berisi teks jawaban dan skornya. Setelah baris peserta
	 * menyusul blok analisis yang seluruh selnya diawali penanda {@code "**"} (dipakai
	 * {@code Common.setStyled} untuk membedakan baris analisis dari baris data): Hasil Analisis,
	 * Soal, Kunci Jawaban, satu baris per huruf pilihan, Jawaban Benar, Jawaban Salah,
	 * Daya Pembeda, Kriteria, Tingkat Kesukaran (p), dan Kategori Kesukaran. Awalan warna
	 * {@code GREEN}/{@code YELLOW}/{@code RED} pada teks kategori adalah instruksi pewarnaan
	 * untuk {@code Common.setStyled}, bukan bagian dari teks yang dimaksudkan terbaca.</p>
	 *
	 * <h3>10. Ketahanan, sumber daya, dan hal yang perlu diwaspadai</h3>
	 * <ul>
	 *   <li>Kegagalan per peserta dan per soal dibungkus {@code try/catch} sendiri sehingga satu
	 *       baris rusak tidak membatalkan seluruh laporan.</li>
	 *   <li>{@code session.clear()} dipanggil setiap 50 peserta untuk menahan pertumbuhan
	 *       first-level cache pada kelas besar.</li>
	 *   <li>{@code fileOut} dan {@code session} ditutup di {@code finally}. Namun
	 *       {@code label.setValue("")} berada di dalam {@code try}, sehingga <b>exception apa pun
	 *       membuat bilah pemuatan menggantung selamanya</b> dan jendela hasil tidak pernah
	 *       terbuka. Bila menambah kode di thread ini, pertimbangkan memindahkan pengosongan
	 *       label ke {@code finally}.</li>
	 *   <li>Berkas xlsx ditulis ke {@code /tmp} milik aplikasi web dan TIDAK pernah dihapus oleh
	 *       kode ini; pembersihannya diserahkan ke perawatan sistem.</li>
	 *   <li>Nama method memuat salah ketik <b>"analsis"</b> (seharusnya "analisis"). JANGAN
	 *       diperbaiki tanpa menyunting ketiga pemanggil lintas modul secara bersamaan.</li>
	 * </ul>
	 *
	 * <h3>11. Otorisasi</h3>
	 * <p>Method ini tidak memeriksa hak akses sama sekali dan tidak menyaring data per satuan
	 * kerja: seluruh data peserta bersumber dari {@code Map} yang disodorkan pemanggil lewat
	 * {@code ambil}. Dengan demikian cakupan data sepenuhnya menjadi tanggung jawab pemanggil.
	 * Analisis ini bersifat <b>read-only</b> — tidak ada {@code update} maupun {@code commit}
	 * terhadap entity ujian mana pun, sehingga tidak dapat dipakai mengubah nilai.</p>
	 *
	 * @param pertemuanPunyaUjian ujian yang butir soalnya dianalisis; dipakai untuk mengambil
	 *                            daftar soal ({@code ambilUjianPunyaSoal}), menentukan mode acak,
	 *                            membaca pemetaan Sub-CPMK, dan menyusun judul jendela
	 * @param ambil               penyedia {@code Map<Long, Object[]>} berisi hasil ujian seluruh
	 *                            peserta ({@code [0]} = {@link HasilUjianMahasiswa},
	 *                            {@code [1]} = himpunan id soal terjawab). TIDAK boleh
	 *                            mengembalikan {@code null}
	 * @param ambilJumlahPeserta  penyedia opsional jumlah peserta TERDAFTAR, dibaca saat tombol
	 *                            diklik. Bila {@code null}, bukan {@link Number}, atau
	 *                            {@code <= 0}, jumlah peserta jatuh-balik ke ukuran map hasil
	 *                            ujian. Nilai ini menjadi penyebut kolom "Kosong", rata-rata
	 *                            nilai, dan batang distribusi — sehingga kedua overload dapat
	 *                            menghasilkan rata-rata yang berbeda untuk ujian yang sama
	 * @return {@code Toolbarbutton} "Analisis Butir Soal" siap dipasang ke toolbar; selalu visible
	 * @see #buildAnalisisVisualHtml(java.util.List, int[], double[])
	 * @see #persenDistribusiSeratus(int[], int)
	 * @see #hasilObe(PertemuanPunyaUjian, Ambildata)
	 */
	public static Toolbarbutton analsisButirSoal(final PertemuanPunyaUjian pertemuanPunyaUjian, final Ambildata ambil,
			final Ambildata ambilJumlahPeserta) {
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Analisis Butir Soal", "/img/svg/check2-circle.svg");
		cari.addEventListener("onClick", new EventListener() {

			/**
			 * Menjalankan <b>Analisis Butir Soal</b>: menyiapkan wadah bersama, menampilkan bilah
			 * pemuatan, dan melepas thread latar penghitung.
			 *
			 * <p><b>Nama berkas keluaran</b> disusun dari {@code realPath("/tmp/data_<timestamp>.xlsx")}
			 * pada aplikasi web. Cap waktu di-{@code URLEncoder}-kan karena formatnya dapat
			 * memuat karakter yang tidak sah sebagai nama berkas, dan keunikannya mencegah dua
			 * pengguna saling menimpa berkas.</p>
			 *
			 * <p><b>Wadah bersama {@code final}</b> yang disiapkan di sini: {@code soalAnalisisList}
			 * (satu {@code String[12]} per soal), {@code statsGlobal} ({@code int[9]} statistik
			 * agregat), {@code nilaiGlobal} ({@code double[1]} rata-rata nilai), serta
			 * {@code intbox}/{@code colsbox} yang menampung dimensi sheet agar komponen
			 * {@code Spreadsheet} dapat diatur ukurannya. Semuanya ditulis thread latar dan dibaca
			 * callback ZK; keamanannya bersandar pada hubungan <i>happens-before</i> yang
			 * terbentuk saat thread latar mengosongkan label.</p>
			 *
			 * <p>Arti tiap indeks {@code statsGlobal} dan {@code soalAnalisisList} beserta rumus
			 * TK/DP yang mengisinya didokumentasikan lengkap pada Javadoc
			 * {@link #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)}.</p>
			 *
			 * @param arg0 event {@code onClick}; tidak dipakai
			 * @throws Exception diteruskan dari pengkodean nama berkas dan pembuatan bilah pemuatan
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				final Intbox intbox = new Intbox(1);

				final Intbox colsbox = new Intbox(1);

				// Shared containers: background thread writes, display callback reads (safe because
				// label.setValue("") establishes happens-before between thread and ZK event queue)
				final List<String[]> soalAnalisisList = new ArrayList<String[]>();
				// Each String[12]: [0]nomorSoal [1]soalText [2]kunci [3]benar [4]salah [5]kosong
				//                  [6]tkVal [7]katTK [8]dpVal [9]katDP [10]distribHtml [11]subCpmkNama
				final int[] statsGlobal = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
				// [0]=peserta [1]=totalSoal [2]=gunakan [3]=revisi [4]=ganti [5]=mudah [6]=sedang [7]=sulit
				//                  [8]=pesertaYgIkutUjian (sudah mengerjakan/menjawab minimal 1 soal)
				final double[] nilaiGlobal = new double[]{0.0};

				final Label label = Common.displayLoadBar(new EventListener() {

					/**
					 * Callback bilah pemuatan Analisis Butir Soal: membangun jendela hasil dua tab
					 * setelah thread latar selesai.
					 *
					 * <p><b>Susunan jendela.</b> {@link MyWindow} 94%&times;95% dengan
					 * {@code Borderlayout} yang diberi tinggi dan lebar PASTI ("100%"). Ukuran
					 * pasti ini disengaja: tanpanya, panel tab pertama belum memiliki tinggi
					 * terbatas saat render awal sehingga scrollbar-nya baru muncul setelah tab
					 * kedua dibuka. North berisi toolbar (Tutup, Download Excel Lengkap), Center
					 * berisi {@link Tabbox}.</p>
					 *
					 * <p><b>Dua tab.</b> Tab 1 "Dashboard Analisis Butir Soal" merender HTML dari
					 * {@link #buildAnalisisVisualHtml(java.util.List, int[], double[])}. Tab 2
					 * "Data Lengkap (Spreadsheet)" menampilkan berkas xlsx lewat komponen
					 * {@code Spreadsheet} yang menunjuk ke {@code ../../tmp/<namaBerkas>}, dengan
					 * jumlah baris dan kolom diambil dari {@code intbox}/{@code colsbox} yang diisi
					 * thread latar. {@code PratinjauXlsxHelper.gantiSpreadsheetDenganGrid}
					 * menggantinya dengan grid biasa bila komponen spreadsheet tidak tersedia.</p>
					 *
					 * <p><b>Dua penanggulangan tata letak ZK 5</b> untuk gejala "tab pertama belum
					 * bisa digulir sampai tab kedua dibuka": (1) listener {@code onSelect} pada
					 * tabbox yang meng-{@code invalidate} panel terpilih; (2) timer yang, setelah
					 * render awal, berpindah sekejap ke tab kedua lalu kembali ke tab pertama —
					 * meniru langkah manual yang selama ini menjadi solusi pengguna.</p>
					 *
					 * @param arg0 event penanda selesai; tidak dipakai
					 * @throws Exception diteruskan dari pembuatan berkas dan komponen jendela
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

						final File file = new File(filename);

						final MyWindow window = new MyWindow(
								"Analisis Butir Soal — " + pertemuanPunyaUjian.getNama(), "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("94%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						// Beri tinggi & lebar PASTI agar Center (tabbox) langsung terikat tingginya
						// sejak awal render. Tanpa ini, panel tab pertama ("Dashboard Analisis Butir
						// Soal") belum punya tinggi terbatas sehingga scrollbar-nya baru muncul setelah
						// tab kedua dibuka (memicu re-layout). Dengan tinggi pasti, scroll langsung ada.
						borderlayout.setHeight("100%");
						borderlayout.setWidth("100%");
						borderlayout.setParent(window);

						// ---- NORTH: toolbar ----
						North north = new North();
						north.setParent(borderlayout);
						Toolbar toolbar = new Toolbar();
						toolbar.setParent(north);

						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							/**
							 * Tombol Tutup jendela Analisis Butir Soal: melepas jendela beserta
							 * kedua tabnya.
							 *
							 * <p>Berkas xlsx di {@code /tmp} TIDAK dihapus saat jendela ditutup —
							 * ia tetap ada sampai dibersihkan perawatan sistem. Ini disengaja agar
							 * unduhan yang sedang berjalan tidak terputus ketika pengguna menutup
							 * jendela lebih dulu.</p>
							 *
							 * @param event event {@code onClick}; tidak dipakai
							 * @throws Exception diteruskan dari pelepasan komponen
							 */
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);

						MyToolbarbuttonConfig excelBtn = new MyToolbarbuttonConfig(
								"Download Excel Lengkap", "/img/excel.png");
						excelBtn.setTooltiptext(
								"Unduh data jawaban per peserta + analisis butir soal dalam format Excel");
						excelBtn.addEventListener("onClick", new EventListener() {
							/**
							 * Tombol <b>Download Excel Lengkap</b>: mengirimkan berkas hasil
							 * analisis dari {@code /tmp} sebagai unduhan.
							 *
							 * <p>Berbeda dari tombol Download pada laporan Hasil OBE yang membaca
							 * {@code byte[]} di memori, di sini berkas dibaca sebagai
							 * {@link FileInputStream} dari disk. Karena itu berkas harus masih ada
							 * — bila perawatan sistem sudah membersihkan {@code /tmp}, unduhan
							 * gagal.</p>
							 *
							 * <p><b>Penanganan error sengaja senyap.</b> Kegagalan hanya direkam
							 * ke {@code ErrorAuditUtil} tanpa pesan ke pengguna, sehingga tombol
							 * tampak tidak merespons ketika berkas belum selesai ditulis atau
							 * sudah terhapus. Ini kelemahan pengalaman pengguna yang layak
							 * diperbaiki dengan meniru pola tiga jalur pada tombol Download Excel
							 * di laporan Hasil OBE, yang membedakan "belum selesai" dari "gagal"
							 * lewat wadah {@code errorRef}.</p>
							 *
							 * @param event event {@code onClick}; tidak dipakai
							 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
							 */
							@Override
							public void onEvent(Event event) throws Exception {
								try {
									Filedownload.save(
											new FileInputStream(file),
											"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
											file.getName());
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2878"); /* ignore */ }
							}
						});
						excelBtn.setParent(toolbar);

						// ---- CENTER: tabbox ----
						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						final Tabbox tabbox = new Tabbox();
						tabbox.setWidth("100%");
						tabbox.setHeight("100%");
						tabbox.setParent(center);
						/*
						 * Gejala: tab pertama ("Dashboard Analisis Butir Soal") belum bisa di-scroll
						 * sampai tab kedua ("Data Lengkap") dibuka dulu. Penyebab: di ZK 5, tabpanel
						 * yang sedang aktif baru mendapat tinggi/overflow yang benar setelah ada
						 * perpindahan tab (re-layout). Solusi tambahan: tiap kali tab dipilih,
						 * invalidate panel terpilih agar tata letak (dan scroll) dihitung ulang.
						 */
						tabbox.addEventListener("onSelect", new EventListener() {
							/**
							 * Penanggulangan tata letak ZK 5: meng-{@code invalidate} panel tab
							 * yang baru dipilih agar tinggi dan {@code overflow}-nya dihitung
							 * ulang, sehingga scrollbar-nya muncul.
							 *
							 * <p><b>Gejala yang diatasi:</b> di ZK 5, tabpanel yang sedang aktif
							 * baru memperoleh tinggi/overflow yang benar setelah terjadi
							 * perpindahan tab (re-layout). Tanpa listener ini, konten tab yang
							 * lebih tinggi dari jendela terpotong tanpa dapat digulir.</p>
							 *
							 * <p>Kegagalan {@code invalidate} diabaikan dan direkam ke jejak audit
							 * — pada kasus terburuk hanya scrollbar yang tidak muncul, bukan
							 * kehilangan data.</p>
							 *
							 * @param ev event {@code onSelect}; tidak dipakai (panel terpilih
							 *           diambil dari tabbox)
							 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
							 */
							@Override
							public void onEvent(Event ev) throws Exception {
								try {
									Tabpanel sel = tabbox.getSelectedPanel();
									if (sel != null) {
										sel.invalidate();
									}
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2907");
									// abaikan
								}
							}
						});

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);
						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						// Tab 1: Visual analysis dashboard
						MyTabConfig tabVisual = new MyTabConfig("Dashboard Analisis Butir Soal");
						tabVisual.setParent(tabs);
						Tabpanel panelVisual = new ais.ui.util.MyTabpanel();
						panelVisual.setStyle("overflow:auto;padding:0;");
						panelVisual.setParent(tabpanels);
						org.zkoss.zul.Html htmlVisual = new org.zkoss.zul.Html();
						htmlVisual.setContent(
								buildAnalisisVisualHtml(soalAnalisisList, statsGlobal, nilaiGlobal));
						htmlVisual.setParent(panelVisual);

						// Tab 2: Full spreadsheet
						MyTabConfig tabSheet = new MyTabConfig("Data Lengkap (Spreadsheet)");
						tabSheet.setParent(tabs);
						Tabpanel panelSheet = new ais.ui.util.MyTabpanel();
						panelSheet.setStyle("padding:0;");
						panelSheet.setParent(tabpanels);
						file.getParentFile().mkdirs();
						file.createNewFile();
						Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
						spreadsheet.setSrc("../../tmp/" + file.getName());
						spreadsheet.setMaxrows(intbox.getValue() + 1);
						spreadsheet.setMaxcolumns(colsbox.getValue() + 1);
						spreadsheet.setWidth("100%");
						spreadsheet.setHeight("100%");
						spreadsheet.setParent(panelSheet);
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

						window.setVisible(true);
						window.onModal();

						/*
						 * Picu re-layout awal agar tab pertama langsung bisa di-scroll TANPA perlu
						 * membuka tab "Data Lengkap" dulu: secara terprogram pindah sekejap ke tab
						 * kedua lalu kembali ke tab pertama (meniru langkah manual yang selama ini
						 * jadi solusi pengguna). Dijalankan via timer agar terjadi setelah render awal.
						 */
						Common.createDefaultTimer(new EventListener() {
							/**
							 * Penanggulangan tata letak kedua: memicu re-layout AWAL agar tab
							 * pertama langsung dapat digulir tanpa pengguna perlu membuka tab
							 * "Data Lengkap" lebih dulu.
							 *
							 * <p>Caranya meniru langkah manual yang selama ini menjadi solusi
							 * pengguna: berpindah sekejap ke tab kedua lalu kembali ke tab
							 * pertama, ditutup {@code invalidate()} pada panel terpilih.
							 * Dijalankan lewat {@code Common.createDefaultTimer} agar terjadi
							 * SETELAH render awal — bila dijalankan langsung, komponen belum
							 * terpasang di klien sehingga perpindahan tab tidak memicu re-layout
							 * apa pun.</p>
							 *
							 * <p>Dijaga terhadap tabbox yang hanya memiliki satu tab, dan
							 * kegagalan diabaikan (direkam ke jejak audit) karena dampak
							 * terburuknya hanya scrollbar yang belum muncul.</p>
							 *
							 * @param ev event timer; tidak dipakai
							 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
							 */
							@Override
							public void onEvent(Event ev) throws Exception {
								try {
									if (tabbox.getTabs() != null && tabbox.getTabs().getChildren().size() > 1) {
										tabbox.setSelectedIndex(1);
										tabbox.setSelectedIndex(0);
									}
									Tabpanel sel = tabbox.getSelectedPanel();
									if (sel != null) {
										sel.invalidate();
									}
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:2966");
									// abaikan
								}
							}
						});
					}
				});

				new Thread(new Runnable() {

					/**
					 * Thread latar penghitung <b>Analisis Butir Soal</b>. Menyusun sheet Excel
					 * berisi matriks jawaban seluruh peserta, menghitung statistik butir
					 * (Tingkat Kesukaran dan Daya Pembeda), lalu mengisi wadah bersama untuk
					 * dashboard visual.
					 *
					 * <h4>Bagian 1 — matriks jawaban per peserta</h4>
					 * <p>Baris header memuat No./Kode/Nama/Kelas-Prodi, satu kolom per soal
					 * (dengan komentar sel berisi teks soal lengkap), lalu Benar/Salah/Skor/
					 * Rangking/Kelompok Rangking. Tiap peserta mengisi satu baris; sel jawaban
					 * memuat huruf pilihan dan komentar selnya memuat teks jawaban beserta
					 * skornya. Identitas peserta diambil dari salah satu dari empat jenis, dengan
					 * catatan khusus untuk {@code Siswa}: kelas diambil dari jadwal pelajaran
					 * pertemuan bila tersedia, mengalahkan kelas yang melekat pada siswa —
					 * agar rekap mencerminkan kelas tempat ujian berlangsung.</p>
					 *
					 * <h4>Bagian 2 — pengelompokan dan pencacahan</h4>
					 * <p>Peringkat dan kelompok Atas/Tengah/Bawah dihitung dengan belah-dua atas
					 * jumlah TINGKAT SKOR distinct — bukan kaidah 27%. Tiga peta pencacah diisi
					 * sepanjang penelusuran: {@code hurufsJawab} (berkunci
					 * {@code soalId + "_" + huruf}, termasuk {@code "-"} untuk tidak dijawab),
					 * {@code jumlahBenar}/{@code jumlahSalah} per soal, dan {@code jumlahPosisi}
					 * (berkunci {@code soalId + "_" + kelompok}) yang hanya mencacah jawaban
					 * BENAR. Ketiganya mencacah BARIS RINCIAN, bukan peserta — lihat catatan pada
					 * Javadoc {@link #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)}
					 * mengenai dampaknya pada soal berjawaban ganda.</p>
					 *
					 * <h4>Bagian 3 — data dashboard</h4>
					 * <p>Peta {@code nomorToSubCpmk} dibangun dari JSON
					 * {@code pertemuanPunyaUjian.getFormatNilais()}, mendukung notasi RENTANG
					 * {@code a-b} selain angka satuan agar selaras dengan {@code ambilMapNomor}
					 * saat penilaian. Untuk tiap soal dihitung TK, kategori TK, DP, kategori DP,
					 * dan HTML batang distribusi pilihan yang dinormalisasi tepat 100% memakai
					 * {@link #persenDistribusiSeratus(int[], int)}. Hasilnya di-{@code add} ke
					 * {@code soalAnalisisList} sebagai {@code String[12]}, sementara
					 * {@code statsGlobal} dan {@code nilaiGlobal} diisi agregatnya.</p>
					 *
					 * <h4>Bagian 4 — blok analisis pada sheet</h4>
					 * <p>Di bawah baris peserta ditulis blok analisis yang seluruh selnya diawali
					 * penanda {@code "**"} — penanda inilah yang dibaca {@code Common.setStyled}
					 * untuk membedakan baris analisis dari baris data. Awalan
					 * {@code GREEN}/{@code YELLOW}/{@code RED} pada teks kategori adalah instruksi
					 * pewarnaan, bukan bagian teks yang dimaksudkan terbaca.</p>
					 *
					 * <h4>Penyelesaian dan hal yang perlu diwaspadai</h4>
					 * <p>Workbook ditulis ke {@code filename}, dimensi sheet disimpan ke
					 * {@code intbox}/{@code colsbox}, lalu label dikosongkan sehingga callback ZK
					 * membangun jendela hasil. <b>Pengosongan label berada DI DALAM blok
					 * {@code try}</b>: exception apa pun — termasuk
					 * {@link NullPointerException} ketika {@code ambil.ambil()} mengembalikan
					 * {@code null} — membuat bilah pemuatan menggantung selamanya dan jendela
					 * hasil tidak pernah terbuka. Blok {@code finally} hanya menutup
					 * {@code fileOut} dan session, tidak menyentuh label. Bila menambah kode di
					 * sini, pertimbangkan memindahkan pengosongan label ke {@code finally}.</p>
					 *
					 * <p>Kegagalan per peserta dan per soal ditangkap terpisah sehingga satu baris
					 * rusak tidak membatalkan laporan; {@code session.clear()} setiap 50 peserta
					 * menahan pertumbuhan cache.</p>
					 */
					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void run() {

						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							
							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet("Analisis Butir Soal");
							sheet.setDefaultColumnWidth(20);

							Object[] objects = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(true, pertemuanPunyaUjian,
									"", 0, 1000);
							List<Long> ujianPunyaSoalsData = (List<Long>) objects[0];

							int rowIndex = 0;
							XSSFRow rowhead = sheet.createRow(rowIndex);

							rowhead.createCell(0).setCellValue("No.");
							rowhead.createCell(1).setCellValue("Kode");
							rowhead.createCell(2).setCellValue("Nama");
							rowhead.createCell(3).setCellValue("Kelas/Prodi");
							int col = 4;
							TreeSet<String> hurufs = new TreeSet<String>();
							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {
									String content = ujianPunyaSoal.getBankSoal().getSoal();
									try {
										content = Jsoup.parse(content.toString()).text();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3011");
									}

									XSSFComment coment = sheet.createComment();
									coment.setString(content);
									XSSFCell cell = rowhead.createCell(col);
									cell.setCellComment(coment);
									cell.setCellValue(pertemuanPunyaUjian.getRandom()
											? (content.length() > 10 ? content.substring(0, 9) + ".." : content)
											: ujianPunyaSoal.getNomorUrut() + "");
									col++;

									BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
									List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(false);
									for (Long bankSoalDetailid : bankSoalDetails) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());

										if (bankSoalDetail != null && !bankSoalDetail.getHuruf().isEmpty()
												&& !bankSoalDetail.getJawaban().trim().isEmpty()) {
											hurufs.add(bankSoalDetail.getHuruf());
										}

									}
								}
							}
							rowhead.createCell(col).setCellValue("Benar");
							rowhead.createCell(col + 1).setCellValue("Salah");
							rowhead.createCell(col + 2).setCellValue("Skor");
							rowhead.createCell(col + 3).setCellValue("Rangking");
							rowhead.createCell(col + 4).setCellValue("Kelompok Rangking");

							Map<Long, Object[]> hasilUjianMahasiswas = (Map<Long, Object[]>) ambil.ambil();

							TreeSet<Double> treeMapRangking = new TreeSet<Double>(Collections.reverseOrder());
							for (Object[] a : hasilUjianMahasiswas.values()) {
								HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
								treeMapRangking.add(hasilUjianMahasiswa.getNilai());
							}

							// JUMLAH PESERTA UJIAN = banyaknya peserta yang benar-benar ikut ujian.
							// Map hasilUjianMahasiswas di-key per peserta (voMahasiswa.getId()) sehingga
							// size()-nya adalah jumlah peserta yang TEPAT.
							//
							// PERBAIKAN (bug "jumlah peserta belum sesuai"): sebelumnya jumlah peserta
							// keliru diambil dari treeMapRangking.size(), yaitu banyaknya NILAI yang
							// BERBEDA. Akibatnya peserta yang nilainya sama hanya terhitung satu, membuat
							// "Jumlah Peserta" tampil LEBIH KECIL dari kenyataan, sekaligus membuat
							// rata-rata nilai, persentase distribusi pilihan (bisa >100%), dan kolom
							// kosong ikut salah karena penyebutnya keliru.
							// PESERTA UJIAN mengikuti angka "Jumlah Peserta" di tab Statistik (jumlah
							// peserta TERDAFTAR) bila pemanggil menyediakannya; jika tidak, pakai ukuran
							// map hasil ujian sebagai cadangan.
							int jumlahPeserta = hasilUjianMahasiswas.size();
							if (ambilJumlahPeserta != null) {
								try {
									Object nilaiPeserta = ambilJumlahPeserta.ambil();
									if (nilaiPeserta instanceof Number && ((Number) nilaiPeserta).intValue() > 0) {
										jumlahPeserta = ((Number) nilaiPeserta).intValue();
									}
								} catch (Exception abaikanPeserta) { ais.common.ErrorAuditUtil.record(abaikanPeserta, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3071");
								}
							}

							// Banyaknya TINGKAT NILAI yang berbeda (peringkat unik). Dipakai HANYA untuk
							// membagi kelompok Atas/Tengah/Bawah pada perhitungan Daya Pembeda, karena
							// "rangking" di bawah dihitung dari posisi nilai peserta pada daftar nilai
							// unik ini — BUKAN untuk menghitung jumlah peserta.
							int jumlahTingkatSkor = treeMapRangking.size();

							TreeMap<String, Integer> hurufsJawab = new TreeMap<String, Integer>();
							TreeMap<Long, Integer> jumlahBenar = new TreeMap<Long, Integer>();
							TreeMap<Long, Integer> jumlahSalah = new TreeMap<Long, Integer>();

							TreeMap<String, Integer> jumlahPosisi = new TreeMap<String, Integer>();
							double jumlahAtas = 0.0;
							rowIndex = 1;
							int countFlush = 0;
							
							for (Object[] a : hasilUjianMahasiswas.values()) {

								try {
									HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];

									int rangking = 0;
									for (Double nilai : treeMapRangking) {
										rangking++;
										if (Common.numberFormat.get().format(nilai)
												.equals(Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()))) {
											break;
										}
									}
									String posisi = rangking <= (jumlahTingkatSkor / 2) ? "Atas"
											: rangking > ((jumlahTingkatSkor + 1) / 2) ? "Bawah" : "Tengah";

									if (posisi.equalsIgnoreCase("Atas")) {
										jumlahAtas += 1.0;
									}

									MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
											hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
											true);
									Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
											.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
													ujianPunyaSoals, false);

									label.setValue("Sedang memproses data " + hasilUjianMahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / hasilUjianMahasiswas.size())
											+ " %)");

									XSSFRow row = sheet.createRow(rowIndex);
									row.createCell(0).setCellValue(rowIndex);
									if (hasilUjianMahasiswa.getMahasiswa() != null) {
										row.createCell(1).setCellValue(hasilUjianMahasiswa.getMahasiswa().getNim());
										row.createCell(2).setCellValue(hasilUjianMahasiswa.getMahasiswa().getNama());
										row.createCell(3)
												.setCellValue(hasilUjianMahasiswa.getMahasiswa().getJurusan().getNama());
									} else if (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null) {
										row.createCell(1).setCellValue(
												hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNoRegistrasi());
										row.createCell(2)
												.setCellValue(hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNama());

										Jurusan jurusan = hasilUjianMahasiswa.getBiodataCalonMahasiswa().getProdiLulus();
										if (jurusan == null) {
											jurusan = hasilUjianMahasiswa.getBiodataCalonMahasiswa().getProdi1();
										}
										row.createCell(3).setCellValue(jurusan == null ? "" : jurusan.getNama());
									} else if (hasilUjianMahasiswa.getSiswa() != null) {
										row.createCell(1).setCellValue(hasilUjianMahasiswa.getSiswa().getNomorInduk());
										row.createCell(2).setCellValue(hasilUjianMahasiswa.getSiswa().getNama());
										KelasSiswa kelas = hasilUjianMahasiswa.getSiswa().getKelas();

										if (pertemuanPunyaUjian.getPertemuan() != null
												&& pertemuanPunyaUjian.getPertemuan().getJadwalPelajaran() != null
												&& pertemuanPunyaUjian.getPertemuan().getJadwalPelajaran()
														.getKelas() != null) {
											kelas = pertemuanPunyaUjian.getPertemuan().getJadwalPelajaran().getKelas();
										}

										row.createCell(3).setCellValue(kelas == null ? "" : kelas.getNama());
									} else if (hasilUjianMahasiswa.getCalonSiswa() != null) {
										row.createCell(1).setCellValue(hasilUjianMahasiswa.getCalonSiswa().getNomorInduk());
										row.createCell(2).setCellValue(hasilUjianMahasiswa.getCalonSiswa().getNama());
										GelombangPendaftaranPsb gel = hasilUjianMahasiswa.getCalonSiswa()
												.getGelombangPendaftaranPsb();

										row.createCell(3).setCellValue(gel == null ? "" : gel.getNama());
									}
									col = 4;
									for (Long d : ujianPunyaSoalsData) {

										UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
												.ambilData(UjianPunyaSoal.class, d.toString());
										if (ujianPunyaSoal != null) {
											List<Long> jawaban = new ArrayList<Long>();
											for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {

												for (Long hasilUjianMahasiswaDetailid : aa) {
													HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
															.ambilData(HasilUjianMahasiswaDetail.class,
																	hasilUjianMahasiswaDetailid.toString());
													if (hasilUjianMahasiswaDetail != null) {
														BankSoal bankSoal = hasilUjianMahasiswa == null ? null
																: hasilUjianMahasiswaDetail.getBankSoal();

														if (bankSoal != null && bankSoal.getId()
																.equals(ujianPunyaSoal.getBankSoal().getId())) {
															jawaban.add(hasilUjianMahasiswaDetailid);
														}

													}
												}
											}

											if (jawaban.isEmpty()) {
												XSSFCell cell = row.createCell(col);
												cell.setCellValue("");

												String h = "-";
												String key = ujianPunyaSoal.getBankSoal().getId() + "_" + h;
												Integer jml = hurufsJawab.get(key);
												if (jml == null) {
													jml = 0;
												}
												jml += 1;
												hurufsJawab.put(key, jml);

											} else {
												String huruf = "";
												String jawabandata = "";
												Double skorLocal = 0.0;
												for (Long idData : jawaban) {
													HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
															.ambilData(HasilUjianMahasiswaDetail.class, idData.toString());
													if (hasilUjianMahasiswaDetail != null) {
														String h = hasilUjianMahasiswaDetail.getBankSoalDetail() == null
																? "-"
																: hasilUjianMahasiswaDetail.getBankSoalDetail().getHuruf();
														huruf += huruf.isEmpty() ? h : "," + h;

														String s = hasilUjianMahasiswaDetail.getBankSoalDetail() == null
																? ""
																: hasilUjianMahasiswaDetail.getBankSoalDetail()
																		.getJawaban();

														jawabandata += jawabandata.isEmpty() ? s : "," + s;

														skorLocal += hasilUjianMahasiswaDetail.getNilai();

														String key = ujianPunyaSoal.getBankSoal().getId() + "_" + h;
														Integer jml = hurufsJawab.get(key);
														if (jml == null) {
															jml = 0;
														}
														jml += 1;
														hurufsJawab.put(key, jml);

														Integer benar = jumlahBenar
																.get(ujianPunyaSoal.getBankSoal().getId());
														if (benar == null) {
															benar = 0;
														}

														if (hasilUjianMahasiswaDetail.getBankSoalDetail() != null
																&& hasilUjianMahasiswaDetail.getBankSoalDetail()
																		.getBetul()) {
															benar++;
														}

														jumlahBenar.put(ujianPunyaSoal.getBankSoal().getId(), benar);

														Integer salah = jumlahSalah
																.get(ujianPunyaSoal.getBankSoal().getId());
														if (salah == null) {
															salah = 0;
														}

														if (hasilUjianMahasiswaDetail.getBankSoalDetail() != null
																&& !hasilUjianMahasiswaDetail.getBankSoalDetail()
																		.getBetul()) {
															salah++;
														}

														jumlahSalah.put(ujianPunyaSoal.getBankSoal().getId(), salah);

														if (hasilUjianMahasiswaDetail.getBankSoalDetail() != null
																&& hasilUjianMahasiswaDetail.getBankSoalDetail()
																		.getBetul()) {

															Integer posisiBenar = jumlahPosisi.get(
																	ujianPunyaSoal.getBankSoal().getId() + "_" + posisi);
															if (posisiBenar == null) {
																posisiBenar = 0;
															}
															posisiBenar++;
															jumlahPosisi.put(
																	ujianPunyaSoal.getBankSoal().getId() + "_" + posisi,
																	posisiBenar);
														}

													}
												}

												XSSFComment coment = sheet.createComment();
												coment.setString(
														jawabandata + ", skor : " + Common.numberFormat.get().format(skorLocal));
												XSSFCell cell = row.createCell(col);
												cell.setCellComment(coment);
												cell.setCellValue(huruf);
											}
											col++;
										}
									}

									row.createCell(col).setCellValue(hasilUjianMahasiswa.getJawabanBenar());
									row.createCell(col + 1).setCellValue(hasilUjianMahasiswa.getJawabanBenarMax()
											- hasilUjianMahasiswa.getJawabanBenar());
									row.createCell(col + 2).setCellValue(hasilUjianMahasiswa.getNilai());

									row.createCell(col + 3).setCellValue(rangking);

									row.createCell(col + 4).setCellValue(posisi);

									hasilUjianMahasiswaDetails = null;

									countFlush++;
									if (countFlush % 50 == 0) {
										session.clear();
									}

								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3303");
								}

								rowIndex++;
							}

							// ============================================================
							// Kumpulkan data analisis untuk dashboard visual
							// ============================================================

							// Build reverse map: nomorSoal (int) → Sub-CPMK name(s) from formatNilais JSON
							final java.util.Map<Integer, String> nomorToSubCpmk = new java.util.HashMap<Integer, String>();
							try {
								String fnjson = pertemuanPunyaUjian.getFormatNilais();
								if (fnjson != null && !fnjson.trim().isEmpty()) {
									JSONObject jfn = new JSONObject(fnjson);
									java.util.Iterator<?> fnKeys = jfn.keys();
									while (fnKeys.hasNext()) {
										String fnId = (String) fnKeys.next();
										String nomors = jfn.optString(fnId, "");
										for (String nm : nomors.split(",")) {
											nm = nm.trim();
											if (nm.isEmpty()) {
												continue;
											}
											// Dukung RENTANG "a-b" (mis. 1-10) selain angka satuan — selaras
											// dengan PertemuanPunyaUjian.ambilMapNomor saat scoring.
											java.util.List<Integer> nums = new java.util.ArrayList<Integer>();
											try {
												if (nm.contains("-")) {
													String[] r = nm.split("-");
													if (r.length == 2) {
														int s = Integer.parseInt(r[0].trim());
														int e = Integer.parseInt(r[1].trim());
														for (int i = Math.min(s, e); i <= Math.max(s, e); i++) {
															nums.add(i);
														}
													}
												} else {
													nums.add(Integer.parseInt(nm));
												}
											} catch (Exception ignoredFn) {
												continue;
											}
											FormatNilai fn = (FormatNilai) session.get(FormatNilai.class, Long.parseLong(fnId));
											String fnNama = fn != null ? fn.getNama() : fnId;
											for (Integer n : nums) {
												String existing = nomorToSubCpmk.get(n);
												nomorToSubCpmk.put(n, existing == null ? fnNama : existing + ", " + fnNama);
											}
										}
									}
								}
							} catch (Exception ignoredFnOuter) {}

							double totalNilaiSum = 0.0;
							for (Object[] ax : hasilUjianMahasiswas.values()) {
								totalNilaiSum += ((HasilUjianMahasiswa) ax[0]).getNilai();
							}
							statsGlobal[0] = jumlahPeserta;
							statsGlobal[1] = ujianPunyaSoalsData.size();
							// PESERTA YANG IKUT UJIAN = yang sudah menjawab minimal satu soal (elemen
							// [1] pada nilai map = daftar id soal terjawab, tak kosong). Definisinya sama
							// dengan "Peserta yg melaksanakan ujian" di tab Statistik. Peserta belum ikut
							// dihitung belakangan di HTML dashboard (total - ikut).
							int pesertaIkutUjianStat = 0;
							for (Object[] aIkut : hasilUjianMahasiswas.values()) {
								if (aIkut != null && aIkut.length > 1 && aIkut[1] instanceof java.util.Collection
										&& !((java.util.Collection<?>) aIkut[1]).isEmpty()) {
									pesertaIkutUjianStat++;
								}
							}
							statsGlobal[8] = pesertaIkutUjianStat;
							nilaiGlobal[0] = jumlahPeserta > 0 ? totalNilaiSum / jumlahPeserta : 0.0;

							for (Long dAnal : ujianPunyaSoalsData) {
								try {
									UjianPunyaSoal upsAnal = (UjianPunyaSoal) GeneralValueObject
											.ambilData(UjianPunyaSoal.class, dAnal.toString());
									if (upsAnal == null) continue;

									Long soalIdAnal = upsAnal.getBankSoal().getId();

									StringBuilder kunciSb = new StringBuilder();
									List<Long> bsdIds = upsAnal.getBankSoal().ambilBankSoalDetail(false);
									for (Long bsdId : bsdIds) {
										BankSoalDetail bsd = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, bsdId.toString());
										if (bsd != null && bsd.getBetul()) {
											if (kunciSb.length() > 0) kunciSb.append(", ");
											kunciSb.append(bsd.getHuruf());
										}
									}

									Integer benarAnal = jumlahBenar.get(soalIdAnal);
									if (benarAnal == null) benarAnal = 0;
									Integer salahAnal = jumlahSalah.get(soalIdAnal);
									if (salahAnal == null) salahAnal = 0;
									int totalJawabAnal = benarAnal + salahAnal;
									int kosongAnal = Math.max(0, jumlahPeserta - totalJawabAnal);

									double pTKAnal = totalJawabAnal > 0
											? (benarAnal.doubleValue() / totalJawabAnal) : 0.0;
									String katTKAnal;
									if (totalJawabAnal == 0) { katTKAnal = "Blm dikerjakan"; }
									else if (pTKAnal > 0.70) { katTKAnal = "Mudah"; statsGlobal[5]++; }
									else if (pTKAnal >= 0.30) { katTKAnal = "Sedang"; statsGlobal[6]++; }
									else { katTKAnal = "Sulit"; statsGlobal[7]++; }

									Integer jmlAtasAnal = jumlahPosisi.get(soalIdAnal + "_Atas");
									if (jmlAtasAnal == null) jmlAtasAnal = 0;
									Integer jmlBawahAnal = jumlahPosisi.get(soalIdAnal + "_Bawah");
									if (jmlBawahAnal == null) jmlBawahAnal = 0;
									boolean adaKelompokAtasAnal = jumlahAtas > 0;
									double dpAnal = adaKelompokAtasAnal
											? (jmlAtasAnal.doubleValue() - jmlBawahAnal.doubleValue()) / jumlahAtas
											: 0.0;
									String katDPAnal;
									switch (kategoriDayaPembeda(dpAnal, totalJawabAnal != 0, adaKelompokAtasAnal)) {
									case DP_KAT_BLM_DIKERJAKAN:
										katDPAnal = "Blm dikerjakan";
										break;
									case DP_KAT_TIDAK_DAPAT_DIHITUNG:
										katDPAnal = "Tidak dapat dihitung";
										break;
									case DP_KAT_SANGAT_BAIK:
										katDPAnal = "Sangat Baik"; statsGlobal[2]++;
										break;
									case DP_KAT_BAIK:
										katDPAnal = "Baik"; statsGlobal[2]++;
										break;
									case DP_KAT_PERLU_REVISI:
										katDPAnal = "Perlu Revisi"; statsGlobal[3]++;
										break;
									default:
										katDPAnal = "Ganti"; statsGlobal[4]++;
										break;
									}

									StringBuilder distribSb = new StringBuilder();
									distribSb.append("<div style='display:flex;flex-direction:column;gap:2px;min-width:110px;'>");
									// Persentase distribusi dibulatkan memakai metode "sisa terbesar" (largest
									// remainder) agar TOTAL semua batang (A..E + kosong) tepat 100% — bukan 98%/99%
									// seperti bila tiap batang dibulatkan ke bawah sendiri-sendiri.
									int[] distCounts = new int[hurufs.size() + 1];
									int idxDistFill = 0;
									for (String hHitung : hurufs) {
										Integer cH = hurufsJawab.get(soalIdAnal + "_" + hHitung);
										distCounts[idxDistFill++] = cH == null ? 0 : cH.intValue();
									}
									distCounts[idxDistFill] = kosongAnal;
									int[] distPct = persenDistribusiSeratus(distCounts, jumlahPeserta);
									int idxDistRender = 0;
									for (String hAnal : hurufs) {
										Integer cntH = hurufsJawab.get(soalIdAnal + "_" + hAnal);
										int cntHv = cntH == null ? 0 : cntH.intValue();
										int pctH = distPct[idxDistRender++];
										boolean betulH = kunciSb.indexOf(hAnal) >= 0;
										String barC = betulH ? "#22c55e" : "#94a3b8";
										String lblC = betulH ? "#166534" : "#64748b";
										distribSb.append("<div style='display:flex;align-items:center;gap:4px;'>");
										distribSb.append("<span style='font-size:11px;font-weight:700;width:14px;color:")
											.append(lblC).append(";'>").append(escapeStatHtml(hAnal)).append("</span>");
										distribSb.append("<div style='flex:1;height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden;'>");
										distribSb.append("<div style='height:100%;width:").append(pctH)
											.append("%;background:").append(barC).append(";border-radius:4px;'></div></div>");
										distribSb.append("<span style='font-size:10px;color:#94a3b8;width:24px;text-align:right;'>")
											.append(pctH).append("%</span></div>");
									}
									if (kosongAnal > 0) {
										int pctK = distPct[hurufs.size()];
										distribSb.append("<div style='display:flex;align-items:center;gap:4px;'>");
										distribSb.append("<span style='font-size:11px;color:#cbd5e1;width:14px;'>-</span>");
										distribSb.append("<div style='flex:1;height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden;'>");
										distribSb.append("<div style='height:100%;width:").append(pctK)
											.append("%;background:#cbd5e1;border-radius:4px;'></div></div>");
										distribSb.append("<span style='font-size:10px;color:#cbd5e1;width:24px;text-align:right;'>")
											.append(pctK).append("%</span></div>");
									}
									distribSb.append("</div>");

									String soalTextAnal = upsAnal.getBankSoal().getSoal();
									try { soalTextAnal = Jsoup.parse(soalTextAnal).text(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3429");}
									if (soalTextAnal != null && soalTextAnal.length() > 65) {
										soalTextAnal = soalTextAnal.substring(0, 62) + "...";
									}
									String nomorSoalAnal = pertemuanPunyaUjian.getRandom()
											? String.valueOf(soalAnalisisList.size() + 1)
											: (upsAnal.getNomorUrut() == null
													? String.valueOf(soalAnalisisList.size() + 1)
													: String.valueOf(upsAnal.getNomorUrut()));

									String subCpmkNamaAnal = "";
									try {
										int nomorSoalInt = Integer.parseInt(nomorSoalAnal);
										String mapped = nomorToSubCpmk.get(nomorSoalInt);
										if (mapped != null) subCpmkNamaAnal = mapped;
									} catch (Exception ignoredSubCpmk) {}
									soalAnalisisList.add(new String[]{
										nomorSoalAnal,
										soalTextAnal == null ? "" : soalTextAnal,
										kunciSb.toString(),
										String.valueOf(benarAnal),
										String.valueOf(salahAnal),
										String.valueOf(kosongAnal),
										Common.numberFormat.get().format(pTKAnal),
										katTKAnal,
										Common.numberFormat.get().format(dpAnal),
										katDPAnal,
										distribSb.toString(),
										subCpmkNamaAnal
									});
								} catch (Exception exAnal) {
									exAnal.printStackTrace(); ais.common.ErrorAuditUtil.record(exAnal, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3453");
								}
							}
							// ============================================================

							rowhead = sheet.createRow(rowIndex);

							rowhead.createCell(0).setCellValue("**");
							rowhead.createCell(1).setCellValue("**Hasil Analisis");
							rowhead.createCell(2).setCellValue("**");
							rowhead.createCell(3).setCellValue("**");

							col = 4;
							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {
									XSSFCell cell = rowhead.createCell(col);
									cell.setCellValue("**");
									col++;
								}
							}
							rowhead.createCell(col).setCellValue("**");
							rowhead.createCell(col + 1).setCellValue("**");
							rowhead.createCell(col + 2).setCellValue("**");
							rowhead.createCell(col + 3).setCellValue("**");
							rowhead.createCell(col + 4).setCellValue("**");

							rowhead = sheet.createRow(rowIndex + 1);

							rowhead.createCell(0).setCellValue("**");
							rowhead.createCell(1).setCellValue("**Soal");
							rowhead.createCell(2).setCellValue("**");
							rowhead.createCell(3).setCellValue("**");

							col = 4;

							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {
									String content = ujianPunyaSoal.getBankSoal().getSoal();
									try {
										content = Jsoup.parse(content.toString()).text();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3500");
									}

									XSSFComment coment = sheet.createComment();
									coment.setString(content);
									XSSFCell cell = rowhead.createCell(col);
									cell.setCellComment(coment);
									cell.setCellValue("**" + (pertemuanPunyaUjian.getRandom()
											? (content.length() > 10 ? content.substring(0, 9) + ".." : content)
											: ujianPunyaSoal.getNomorUrut() + ""));
									col++;

								}
							}
							rowhead.createCell(col).setCellValue("**");
							rowhead.createCell(col + 1).setCellValue("**");
							rowhead.createCell(col + 2).setCellValue("**");
							rowhead.createCell(col + 3).setCellValue("**");
							rowhead.createCell(col + 4).setCellValue("**");

							rowhead = sheet.createRow(rowIndex + 2);

							rowhead.createCell(0).setCellValue("**");
							rowhead.createCell(1).setCellValue("**Kunci Jawaban");
							rowhead.createCell(2).setCellValue("**");
							rowhead.createCell(3).setCellValue("**");

							col = 4;

							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {
									
									StringBuilder hurufBetul = new StringBuilder();
									StringBuilder hurufJawaban = new StringBuilder();
									
									BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
									List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(false);
									for (Long bankSoalDetailid : bankSoalDetails) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());

										if (bankSoalDetail != null && bankSoalDetail.getBetul()) {
											if(hurufBetul.length() > 0) {
												hurufBetul.append(",");
												hurufJawaban.append(";");
											}
											hurufBetul.append(bankSoalDetail.getHuruf());
											hurufJawaban.append(bankSoalDetail.getJawaban());
										}

									}

									XSSFComment coment = sheet.createComment();
									coment.setString(hurufJawaban.toString());
									XSSFCell cell = rowhead.createCell(col);
									cell.setCellComment(coment);
									cell.setCellValue("**" + hurufBetul.toString());
									col++;

								}
							}
							rowhead.createCell(col).setCellValue("**");
							rowhead.createCell(col + 1).setCellValue("**");
							rowhead.createCell(col + 2).setCellValue("**");
							rowhead.createCell(col + 3).setCellValue("**");
							rowhead.createCell(col + 4).setCellValue("**");

							hurufs.add("-");

							int penambaan = 3;
							for (String huruf : hurufs) {
								rowhead = sheet.createRow(rowIndex + penambaan);

								rowhead.createCell(0).setCellValue("**");
								rowhead.createCell(1).setCellValue(
										huruf.equalsIgnoreCase("-") ? "**Tidak Dijawab" : "**Jawaban " + huruf);
								rowhead.createCell(2).setCellValue("**");
								rowhead.createCell(3).setCellValue("**");
								col = 4;
								for (Long d : ujianPunyaSoalsData) {

									UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
											.ambilData(UjianPunyaSoal.class, d.toString());
									if (ujianPunyaSoal != null) {
										String key = ujianPunyaSoal.getBankSoal().getId() + "_" + huruf;
										Integer jml = hurufsJawab.get(key);
										if (jml == null) {
											jml = 0;
										}

										XSSFCell cell = rowhead.createCell(col);
										cell.setCellValue("**" + Common.numberFormat.get().format(jml));
										col++;
									}
								}

								rowhead.createCell(col).setCellValue("**");
								rowhead.createCell(col + 1).setCellValue("**");
								rowhead.createCell(col + 2).setCellValue("**");
								rowhead.createCell(col + 3).setCellValue("**");
								rowhead.createCell(col + 4).setCellValue("**");

								penambaan++;
							}

							col = 4;
							rowhead = sheet.createRow(rowIndex + penambaan);

							rowhead.createCell(0).setCellValue("**");
							rowhead.createCell(1).setCellValue("**Jawaban Benar");
							rowhead.createCell(2).setCellValue("**");
							rowhead.createCell(3).setCellValue("**");

							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {

									Integer jml = jumlahBenar.get(ujianPunyaSoal.getBankSoal().getId());
									if (jml == null) {
										jml = 0;
									}

									XSSFCell cell = rowhead.createCell(col);
									cell.setCellValue("**" + Common.numberFormat.get().format(jml));
									col++;
								}
							}

							rowhead.createCell(col).setCellValue("**");
							rowhead.createCell(col + 1).setCellValue("**");
							rowhead.createCell(col + 2).setCellValue("**");
							rowhead.createCell(col + 3).setCellValue("**");
							rowhead.createCell(col + 4).setCellValue("**");

							rowhead = sheet.createRow(rowIndex + penambaan + 1);

							rowhead.createCell(0).setCellValue("**");
							rowhead.createCell(1).setCellValue("**Jawaban Salah");
							rowhead.createCell(2).setCellValue("**");
							rowhead.createCell(3).setCellValue("**");
							col = 4;
							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {

									Integer jml = jumlahSalah.get(ujianPunyaSoal.getBankSoal().getId());
									if (jml == null) {
										jml = 0;
									}

									XSSFCell cell = rowhead.createCell(col);
									cell.setCellValue("**" + Common.numberFormat.get().format(jml));
									col++;
								}
							}

							rowhead.createCell(col).setCellValue("**");
							rowhead.createCell(col + 1).setCellValue("**");
							rowhead.createCell(col + 2).setCellValue("**");
							rowhead.createCell(col + 3).setCellValue("**");
							rowhead.createCell(col + 4).setCellValue("**");

							rowhead = sheet.createRow(rowIndex + penambaan + 2);

							rowhead.createCell(0).setCellValue("**");
							rowhead.createCell(1).setCellValue("**Daya Pembeda");
							rowhead.createCell(2).setCellValue("**");
							rowhead.createCell(3).setCellValue("**");

							XSSFRow rowhead1 = sheet.createRow(rowIndex + penambaan + 3);

							rowhead1.createCell(0).setCellValue("**");
							rowhead1.createCell(1).setCellValue("**Kriteria");
							rowhead1.createCell(2).setCellValue("**");
							rowhead1.createCell(3).setCellValue("**");

							col = 4;
							for (Long d : ujianPunyaSoalsData) {

								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, d.toString());
								if (ujianPunyaSoal != null) {

									Integer jmlAtas = jumlahPosisi.get(ujianPunyaSoal.getBankSoal().getId() + "_Atas");
									if (jmlAtas == null) {
										jmlAtas = 0;
									}

									Integer jmlBawah = jumlahPosisi.get(ujianPunyaSoal.getBankSoal().getId() + "_Bawah");
									if (jmlBawah == null) {
										jmlBawah = 0;
									}

									Integer benar = jumlahBenar.get(ujianPunyaSoal.getBankSoal().getId());
									if (benar == null) {
										benar = 0;
									}

									Integer salah = jumlahSalah.get(ujianPunyaSoal.getBankSoal().getId());
									if (salah == null) {
										salah = 0;
									}

									boolean adaKelompokAtas = jumlahAtas > 0;
									double jml = adaKelompokAtas
											? (jmlAtas.doubleValue() - jmlBawah.doubleValue()) / jumlahAtas
											: 0.0;

									XSSFCell cell = rowhead.createCell(col);
									cell.setCellValue("**" + Common.numberFormat.get().format(jml));

									boolean sudahDijawabExcel = !(benar.equals(0) && salah.equals(0));
									String ni;
									switch (kategoriDayaPembeda(jml, sudahDijawabExcel, adaKelompokAtas)) {
									case DP_KAT_BLM_DIKERJAKAN:
										ni = "Blm dikerjakan";
										break;
									case DP_KAT_TIDAK_DAPAT_DIHITUNG:
										ni = "Tidak dapat dihitung";
										break;
									case DP_KAT_GANTI:
										ni = "REDGanti";
										break;
									case DP_KAT_PERLU_REVISI:
										ni = "YELLOWRevisi";
										break;
									default:
										ni = "GREENGunakan";
										break;
									}
									cell = rowhead1.createCell(col);
									cell.setCellValue("**" + ni);

									col++;
								}
							}

							rowhead.createCell(col).setCellValue("**");
							rowhead.createCell(col + 1).setCellValue("**");
							rowhead.createCell(col + 2).setCellValue("**");
							rowhead.createCell(col + 3).setCellValue("**");
							rowhead.createCell(col + 4).setCellValue("**");

							rowhead1.createCell(col).setCellValue("**");
							rowhead1.createCell(col + 1).setCellValue("**");
							rowhead1.createCell(col + 2).setCellValue("**");
							rowhead1.createCell(col + 3).setCellValue("**");
							rowhead1.createCell(col + 4).setCellValue("**");

							// ---- Tingkat Kesukaran (p = benar / total) ----
							XSSFRow rowTK = sheet.createRow(rowIndex + penambaan + 4);
							rowTK.createCell(0).setCellValue("**");
							rowTK.createCell(1).setCellValue("**Tingkat Kesukaran (p)");
							rowTK.createCell(2).setCellValue("**");
							rowTK.createCell(3).setCellValue("**");

							XSSFRow rowKatTK = sheet.createRow(rowIndex + penambaan + 5);
							rowKatTK.createCell(0).setCellValue("**");
							rowKatTK.createCell(1).setCellValue("**Kategori Kesukaran");
							rowKatTK.createCell(2).setCellValue("**");
							rowKatTK.createCell(3).setCellValue("**");

							col = 4;
							for (Long dtk : ujianPunyaSoalsData) {
								UjianPunyaSoal soalTK = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, dtk.toString());
								if (soalTK != null) {
									Integer benarTK = jumlahBenar.get(soalTK.getBankSoal().getId());
									if (benarTK == null) benarTK = 0;
									Integer salahTK = jumlahSalah.get(soalTK.getBankSoal().getId());
									if (salahTK == null) salahTK = 0;
									int totalTK = benarTK + salahTK;
									double pTK = totalTK > 0 ? (benarTK.doubleValue() / totalTK) : 0.0;

									XSSFCell cellTK = rowTK.createCell(col);
									cellTK.setCellValue("**" + Common.numberFormat.get().format(pTK));

									String katTK;
									if (totalTK == 0) {
										katTK = "Blm dikerjakan";
									} else if (pTK > 0.70) {
										katTK = "GREENMudah";
									} else if (pTK >= 0.30) {
										katTK = "YELLOWSedang";
									} else {
										katTK = "REDSulit";
									}
									XSSFCell cellKatTK = rowKatTK.createCell(col);
									cellKatTK.setCellValue("**" + katTK);
									col++;
								}
							}

							rowTK.createCell(col).setCellValue("**");
							rowTK.createCell(col + 1).setCellValue("**");
							rowTK.createCell(col + 2).setCellValue("**");
							rowTK.createCell(col + 3).setCellValue("**");
							rowTK.createCell(col + 4).setCellValue("**");

							rowKatTK.createCell(col).setCellValue("**");
							rowKatTK.createCell(col + 1).setCellValue("**");
							rowKatTK.createCell(col + 2).setCellValue("**");
							rowKatTK.createCell(col + 3).setCellValue("**");
							rowKatTK.createCell(col + 4).setCellValue("**");

							Common.setStyled(sheet);

							try {
								fileOut = new FileOutputStream(filename);
								workbook.write(fileOut);
							} catch (IOException e) {
								Common.tampilErrorJikaAdmin(e);
							}

							intbox.setValue(sheet.getLastRowNum());
							colsbox.setValue((int) sheet.getRow(0).getLastCellNum());

							label.setValue("");
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3814");
						} finally {
							if (fileOut != null) {
								try { fileOut.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3817");}
							}
							if (session != null && session.isOpen()) {
								try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3820");}
							}
						}
					}
				}).start();

			}
		});
		return cari;
	}

	/**
	 * Menampilkan detail hasil ujian satu peserta dalam panel collapsible (MyDetail).
	 * Memperlihatkan setiap soal beserta jawaban peserta, kunci jawaban, skor, dan
	 * (untuk esai) area koreksi nilai manual.
	 *
	 * <p><b>Tujuan:</b> Ketika dosen mengklik tombol "Detail" pada satu baris hasil ujian,
	 * method ini mengisi panel detail dengan breakdown per-soal. Dosen bisa melihat
	 * apakah peserta menjawab benar, dan untuk soal esai bisa memberikan/mengubah nilai secara
	 * manual.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Guard: bila {@code tempHasilUjianMahasiswa} null atau tanpa {@code pertemuanPunyaUjian},
	 *       no-op.</li>
	 *   <li>Mengambil daftar soal dari {@code hasilUjianMahasiswa.ambilUjianPunyaSoals(...)},
	 *       yaitu soal yang dikerjakan peserta ini (urutan asli, bukan re-randomized).</li>
	 *   <li>Untuk setiap soal, menampilkan:
	 *     <ul>
	 *       <li>Nomor soal dan teks soal (dari {@code BankSoal.soal}, di-strip HTML menggunakan
	 *           Jsoup).</li>
	 *       <li>Opsi jawaban peserta (dari {@code HasilUjianMahasiswaDetail}).</li>
	 *       <li>Kunci jawaban yang benar.</li>
	 *       <li>Skor yang diperoleh peserta untuk soal ini.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Untuk soal ESAI: menampilkan {@code Textbox} nilai yang dapat diubah dosen,
	 *       dengan tombol "Simpan" yang menyimpan nilai baru via native session.</li>
	 *   <li>Setelah semua soal di-render, memanggil {@code Common.refreshUpdate} pada
	 *       {@code hasilUjianMahasiswa} untuk menyinkronkan skor total.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception per-soal ditangkap dan dicetak ke stderr.
	 * Koreksi nilai yang gagal disimpan ditampilkan via pesan error.</p>
	 *
	 * @param detail                   komponen MyDetail tempat konten akan diisi
	 * @param tempHasilUjianMahasiswa  objek hasil ujian yang akan ditampilkan detailnya
	 */
	public void tampilRow(final MyDetail detail, final HasilUjianMahasiswa tempHasilUjianMahasiswa) {
		if (tempHasilUjianMahasiswa != null && tempHasilUjianMahasiswa.getPertemuanPunyaUjian() != null) {
			pertemuanPunyaUjian = tempHasilUjianMahasiswa.getPertemuanPunyaUjian();
		}
		new KoreksiHasilUjian().display(detail, tempHasilUjianMahasiswa, pertemuanPunyaUjian);
	}

	

	/**
	 * Renderer lokal untuk layar/komponen {@link HasilUjianMahasiswaHelper}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link HasilUjianMahasiswaHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see HasilUjianMahasiswaHelper
	 */
	public class DetailPertemuanPunyaUjianRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender SATU baris grid rekap hasil ujian — sembilan sel yang memuat
		 * kartu peserta, waktu pengerjaan, skor, nilai, keterangan, dan rekap pelanggaran —
		 * sekaligus memasang seluruh <i>editor sebaris</i> (inline editor) yang memungkinkan
		 * dosen mengubah data hasil ujian langsung dari grid tanpa membuka jendela lain.
		 *
		 * <p>Method ini adalah <b>bagian terpanjang dan paling padat efek samping</b> pada kelas
		 * ini: ia bukan sekadar penggambar tampilan, melainkan tempat sepuluh listener penulis
		 * database dipasang. Perubahan di sini berdampak langsung pada integritas nilai.</p>
		 *
		 * <h3>1. Resolusi objek baris</h3>
		 * <p>{@code arg1} adalah salah satu dari empat implementasi {@code VOMahasiswa}
		 * ({@link Mahasiswa}, {@link BiodataCalonMahasiswa}, {@link Siswa}, {@link CalonSiswa}).
		 * Keempatnya diuji dengan {@code instanceof}, lalu id-nya dipakai mencari
		 * {@code Object[]} pada {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas}. Bila
		 * entri belum ada — lazim terjadi karena grid dipasang SEBELUM kolam thread latar selesai
		 * mengisi map — method langsung {@code return} sehingga baris tampil kosong, bukan
		 * melempar exception. Menekan Refresh setelah pemuatan selesai akan memunculkan isinya.</p>
		 * <p>{@code s[0]} adalah entity hasil ujian dan {@code s[1]} himpunan id soal terjawab;
		 * dari situ dihitung {@code terjawab}, {@code belum} (dijaga tidak negatif), serta
		 * persentase terjawab/belum yang dijaga terhadap {@code totalSoal <= 0}.</p>
		 *
		 * <h3>2. Panel detail yang dimuat malas</h3>
		 * <p>{@link MyDetail} dipasang sebagai sel pertama dengan listener {@code onOpen}. Isi
		 * detail (rincian soal &amp; koreksi jawaban) baru dibangun saat panel DIBUKA, lewat
		 * {@link HasilUjianMahasiswaHelper#tampilRow(MyDetail, HasilUjianMahasiswa)} yang
		 * mendelegasikan ke {@code KoreksiHasilUjian}. Pemuatan malas ini penting karena satu
		 * halaman grid memuat sampai 1000 peserta. Listener yang sama juga menangani sinyal
		 * "data berubah": bila {@code event.getData()} berupa {@link HasilUjianMahasiswa},
		 * baris dibersihkan lalu {@code render(...)} dipanggil ulang lewat timer ZK — pola
		 * render-ulang mandiri yang membuat perubahan dari panel koreksi langsung terlihat.</p>
		 *
		 * <h3>3. Sel-sel tampilan</h3>
		 * <ul>
		 *   <li><b>Kartu peserta:</b> foto kecil ({@code CommonMedia.tampilkanGambarKecil}) plus
		 *       nama, dibungkus {@code RevisiHelper.createNewRevisi} sehingga riwayat revisi
		 *       entity dapat ditelusuri dari baris. Baris diberi {@code sclass}
		 *       {@code "ais-peserta-row"} agar CSS meratakan latar kotak bersarang.</li>
		 *   <li><b>Waktu:</b> tanggal ujian, waktu mulai, dan waktu selesai — masing-masing hanya
		 *       dirender bila datanya ada.</li>
		 *   <li><b>Lama Waktu:</b> diformat memakai {@code Common.timeFormat1} (HH:mm:ss) dan
		 *       BUKAN format tanggal, karena {@code getLamaPengerjaan()} adalah DURASI yang dibuat
		 *       dari {@code GregorianCalendar(0,0,0,jam,menit,detik)} sehingga bagian tanggalnya
		 *       tidak bermakna ("31-12-0002").</li>
		 *   <li><b>Statistik ringkas:</b> jumlah soal, soal terjawab, dan soal belum terjawab
		 *       beserta persentasenya. Latar baris diwarnai merah muda bila pengerjaan sebagian
		 *       (0&lt;persen&lt;100) dan hijau muda bila 100%.</li>
		 *   <li><b>Pelanggaran:</b> jumlah pelanggaran (merah bila &gt;0, hijau "0 (bersih)" bila
		 *       tidak ada) dan cuplikan {@code logPelanggaran} dipangkas 400 karakter dengan
		 *       teks penuh pada tooltip.</li>
		 * </ul>
		 *
		 * <h3>4. Sel Skor/Max dan Nilai — dua cabang besar</h3>
		 * <p>Tampilan skor bercabang menurut apakah kurikulum perkuliahan ber-OBE
		 * ({@code kurikulum.apakahObe(tahunAjaran, ganjilGenap)}):</p>
		 * <ul>
		 *   <li><b>Mode OBE:</b> satu baris per Sub-CPMK, dibaca dari JSON {@code nilaiObe}.
		 *       Nilai tersebut sengaja diambil <b>LANGSUNG dari database</b> lewat
		 *       {@code Projections.property("nilaiObe")} pada session terdedikasi, MENEMBUS cache
		 *       MapDB {@code ambilByKey} yang bisa basi. Tanpa itu kolom Skor/Max tidak akan
		 *       mencerminkan hasil "Hitung Ulang" terbaru. Pada varian pilihan ganda + OBE,
		 *       label nilai per Sub-CPMK dibuat dapat diklik menuju
		 *       {@link HasilUjianMahasiswaHelper#bukaPopupRincianSubCpmk(HasilUjianMahasiswa, FormatNilai)},
		 *       tetapi hanya bila {@code nilaiMax != 0}.</li>
		 *   <li><b>Mode non-OBE:</b> label "benar / maks", dan pada pilihan ganda label Nilai
		 *       dapat diklik menuju
		 *       {@link HasilUjianMahasiswaHelper#bukaPopupPerbandinganSkor(HasilUjianMahasiswa)}
		 *       untuk menemukan soal berdata tak wajar (skor diperoleh melebihi skor maksimal).</li>
		 * </ul>
		 * <p>Pada keempat titik tersebut, bila skor 0 sementara maksimalnya &gt; 0, tombol bantuan
		 * {@code tombolBantuanNilaiNol(...)} disisipkan.</p>
		 *
		 * <h3>5. Editor sebaris dan pola penyimpanannya</h3>
		 * <p>Sepuluh listener penulis dipasang. Dua pola penyimpanan berbeda dipakai secara sadar:</p>
		 * <ul>
		 *   <li><b>Pola HQL bulk update</b> — dipakai {@code Sisa Waktu} dan checkbox
		 *       {@code Lengkapi ulang jawaban}. Alasannya didokumentasikan pada komentar di kode:
		 *       pola {@code session.get} + setter + {@code update} memicu Hibernate MEMANGGIL
		 *       getter saat dirty-check (mapping berbasis PROPERTY access), dan getter
		 *       {@code getSisaWaktuPengerjaan()} SENGAJA menimpa nilai in-memory dengan cache
		 *       berkas "live" yang masih berisi nilai lama — sehingga masukan admin batal
		 *       tersimpan. {@code update ... set ... where id = :id} tidak memuat entity dan tidak
		 *       memanggil getter sama sekali, sehingga nilai yang tersimpan persis seperti yang
		 *       diketik. Ini contoh konkret dampak pola arsitektur <i>getter yang memutasi
		 *       field</i> yang tersebar di paket model.</li>
		 *   <li><b>Pola muat-ubah-simpan</b> — dipakai {@code Ikut ujian} ({@code jumlahIkut}),
		 *       {@code Nilai} (esai), tombol {@code Hitung Ulang} per baris, dan
		 *       {@code Keterangan}. Aman karena kolom-kolom itu tidak punya getter bermutasi.</li>
		 * </ul>
		 * <p>Editor {@code Nilai} dan {@code Keterangan} memberi umpan balik simpan-otomatis
		 * berupa tanda &#10003;/&#10007; yang memudar sendiri lewat {@code Clients.evalJavaScript}.
		 * Field {@code Nomor Terakhir} tidak menulis ke database melainkan ke penyimpanan berkas
		 * "live" entity ({@code put(nilai, "index")}) karena dipakai layar ujian yang sedang
		 * berjalan.</p>
		 * <p>Tombol <b>Hitung Ulang</b> per baris memakai rumus yang BERBEDA dari mesin penilaian
		 * pusat: ia menjumlahkan {@code (nilaiDetail * 100 / skorSoal)} untuk setiap soal lalu
		 * membaginya dengan jumlah soal, dan hanya mengambil SATU detail per soal
		 * ({@code iterator().next()}). Untuk soal berjawaban ganda hasilnya karena itu dapat
		 * berbeda dari {@code ProsesUjianHelper.hitungPilihanGanda}. Bila total &le; 0.1 nilai
		 * tidak ditulis dan pengguna diberi pesan "belum dikoreksi".</p>
		 *
		 * <h3>6. Tombol Reset Ujian</h3>
		 * <p>Menghapus jawaban peserta ({@code bankSoalDetail}, {@code jawaban},
		 * {@code waktuJawab} di-null-kan — baris detail TIDAK dihapus), memanggil
		 * {@code reset()} pada entity utama, lalu mengosongkan {@code sisaWaktuPengerjaan},
		 * {@code jumlahPelanggaran}, dan {@code logPelanggaran}. Dilindungi dialog konfirmasi
		 * dan dijalankan lewat timer ZK agar tidak memblokir antrean event. Setelah commit,
		 * baris dirender ulang.</p>
		 *
		 * <h3>7. Otorisasi — perlu diperhatikan</h3>
		 * <p>Satu-satunya pemeriksaan hak akses di dalam method ini adalah gerbang tombol
		 * <b>Reset Ujian</b>: {@code Common.getCurrentUser()} harus bukan mahasiswa dan bukan
		 * siswa. Perhatikan dua hal:</p>
		 * <ul>
		 *   <li>Gerbang itu <b>tidak mengecualikan</b> pengguna ber-{@code biodataCalonMahasiswa}
		 *       atau ber-{@code calonSiswa}, berbeda dari gerbang tombol toolbar di
		 *       {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)} yang
		 *       memeriksa keempat peran peserta.</li>
		 *   <li>Editor yang MENGUBAH NILAI ({@code Doublebox} nilai esai, tombol Hitung Ulang),
		 *       waktu ({@code Sisa Waktu}), dan status pengerjaan ({@code Ikut ujian},
		 *       {@code Lengkapi ulang jawaban}) sama sekali TIDAK bergerbang di dalam
		 *       listener-nya. Perlindungannya sepenuhnya bersandar pada asumsi bahwa grid ini
		 *       hanya dibuka dari tombol "Hasil Ujian" milik layar dosen/admin. Ini adalah pola
		 *       <i>penjagaan hanya di lapisan UI</i>: begitu ada jalur baru yang membuka grid
		 *       untuk peran peserta, seluruh editor tersebut ikut terbuka tanpa perlawanan.
		 *       Bila menambahkan pemanggil baru {@code display(...)}, WAJIB memverifikasi
		 *       perannya, atau lebih baik menambahkan pemeriksaan peran di dalam listener
		 *       penulis di sini.</li>
		 * </ul>
		 * <p>Tidak ada pula penyaringan kepemilikan per baris: renderer mempercayai sepenuhnya
		 * isi {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas} yang disusun
		 * {@link HasilUjianMahasiswaHelper#loadData(Object)}.</p>
		 *
		 * <h3>8. Session dan ketahanan</h3>
		 * <p>Setiap listener membuka session Hibernate SENDIRI dari {@code SessionFactory} dan
		 * menutupnya di {@code finally}; tidak ada session yang dibagi antar listener. Beberapa
		 * blok {@code catch} pada editor {@code Ikut ujian} hanya me-rollback tanpa memberi tahu
		 * pengguna — kegagalan simpan di sana berlangsung senyap. Sebaliknya editor Nilai dan
		 * Keterangan menampilkan tanda &#10007; beserta pesan kegagalan.</p>
		 *
		 * <h3>9. Pemeliharaan</h3>
		 * <p>Jumlah sel yang ditambahkan ke {@code arg0} HARUS sama dengan jumlah
		 * {@code MyColumnConfig} yang dideklarasikan di
		 * {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)}
		 * (sembilan kolom). Menambah satu {@code Vbox}/{@code Hbox} tanpa menambah kolom akan
		 * menggeser seluruh sel ke kanan. Perhatikan bahwa beberapa cabang OBE/non-OBE
		 * menambahkan sel pada posisi berbeda — telusuri kedua cabang saat mengubah tata letak.</p>
		 *
		 * @param arg0 baris {@link Row} yang akan diisi; sudah terpasang pada {@code Rows} grid
		 * @param arg1 objek peserta dari model grid — {@link Mahasiswa},
		 *             {@link BiodataCalonMahasiswa}, {@link Siswa}, atau {@link CalonSiswa}.
		 *             Tipe lain menghasilkan baris kosong
		 * @throws Exception diteruskan dari pembuatan komponen ZK maupun akses data
		 * @see HasilUjianMahasiswaHelper#loadData(Object)
		 * @see HasilUjianMahasiswaHelper#tampilRow(MyDetail, HasilUjianMahasiswa)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, final Object arg1) throws Exception {
			// Kartu peserta hasil ujian (foto+nama dari Vbox/Hbox ZK). Tandai dengan
			// ais-peserta-row agar CSS meratakan latar kotak bersarang -> hover tanpa garis putih.
			arg0.setSclass("ais-peserta-row");
			Mahasiswa mahasiswa = (arg1 instanceof Mahasiswa) ? (Mahasiswa) arg1 : null;
			BiodataCalonMahasiswa biodataCalonMahasiswa = (arg1 instanceof BiodataCalonMahasiswa)
					? (BiodataCalonMahasiswa) arg1
					: null;
			Siswa siswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
			CalonSiswa calonSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

			Object[] s = mahasiswa != null ? hasilUjianMahasiswas.get(mahasiswa.getId())
					: biodataCalonMahasiswa != null ? hasilUjianMahasiswas.get(biodataCalonMahasiswa.getId())
							: siswa != null ? hasilUjianMahasiswas.get(siswa.getId())
									: calonSiswa != null ? hasilUjianMahasiswas.get(calonSiswa.getId()) : null;

			if (s == null) {
				return;
			}

			final HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) s[0];

			final HasilUjianMahasiswa tempHasilUjianMahasiswa = hasilUjianMahasiswa;
			int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan();
			Set<Long> idsa = (Set<Long>) s[1];
			int terjawab = idsa == null ? 0 : idsa.size();
			int belum = totalSoal - terjawab;
			if (belum < 0) {
				belum = 0;
			}
			Double persen = totalSoal <= 0 ? 0.0 : (100.0 * terjawab) / totalSoal;
			Double persenBelum = totalSoal <= 0 ? 0.0 : (100.0 * belum) / totalSoal;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				/**
				 * Listener {@code onOpen} panel detail baris, merangkap penerima sinyal
				 * "data berubah". Menangani DUA peristiwa yang berbeda:
				 *
				 * <ol>
				 *   <li><b>Event membawa {@link HasilUjianMahasiswa}</b> — sinyal dari panel
				 *       koreksi bahwa data peserta baru saja berubah. Baris dibersihkan lalu
				 *       {@code render(arg0, arg1)} dipanggil ULANG lewat timer ZK, sehingga
				 *       seluruh sel (skor, nilai, statistik, pewarnaan latar) dibangun kembali
				 *       dari data terbaru. Pemakaian timer penting: merender ulang komponen dari
				 *       dalam listener komponen itu sendiri dapat merusak pohon komponen yang
				 *       sedang diproses ZK.</li>
				 *   <li><b>Event biasa (panel dibuka)</b> — isi detail dibangun secara MALAS lewat
				 *       {@link HasilUjianMahasiswaHelper#tampilRow(MyDetail, HasilUjianMahasiswa)}.
				 *       Tanpa pemuatan malas, satu halaman grid berisi sampai 1000 peserta akan
				 *       memuat rincian soal dan jawaban semuanya sekaligus.</li>
				 * </ol>
				 *
				 * <p><b>{@code detail.setAttribute("eventListener", this)}</b> menyimpan referensi
				 * listener ini pada komponen agar kode lain (mis. panel koreksi) dapat
				 * mengambilnya kembali dan mengirimkan sinyal "data berubah" tanpa perlu
				 * menyimpan referensi tersendiri.</p>
				 *
				 * @param event event {@code onOpen}, atau sinyal perubahan data yang membawa
				 *              {@link HasilUjianMahasiswa} pada {@code getData()}
				 * @throws Exception diteruskan dari pembangunan panel detail
				 */
				@Override
				public void onEvent(Event event) throws Exception {
					detail.setAttribute("eventListener", this);

					if (event != null && event.getData() != null && event.getData() instanceof HasilUjianMahasiswa) {
						Common.clear(arg0);
						Common.createDefaultTimer(new EventListener() {

							/**
							 * Membangun ulang seluruh isi baris dengan memanggil kembali
							 * {@code render(arg0, arg1)} setelah data peserta berubah.
							 *
							 * <p>Dijalankan lewat timer, BUKAN langsung, karena pemanggil berada
							 * di dalam listener milik komponen yang barusan dibersihkan
							 * {@code Common.clear(arg0)}. Merender ulang di tengah pemrosesan
							 * event atas pohon komponen yang sama berisiko merusak keadaan ZK;
							 * timer menunda pekerjaan itu ke siklus event berikutnya ketika
							 * pembersihan sudah tuntas.</p>
							 *
							 * <p>{@code arg0} (baris) dan {@code arg1} (objek peserta) ditangkap
							 * dari parameter {@code render} terluar, sehingga baris dibangun ulang
							 * untuk peserta yang sama — data terbarunya diambil ulang dari
							 * {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas}.</p>
							 *
							 * @param event event timer; tidak dipakai
							 * @throws Exception diteruskan dari perenderan ulang baris
							 */
							@Override
							public void onEvent(Event event) throws Exception {
								render(arg0, arg1);
							}
						});
					} else {
						tampilRow(detail, tempHasilUjianMahasiswa);
					}

				}
			};

			detail.addEventListener("onOpen", eventListener);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			if (mahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
			} else if (biodataCalonMahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(hbox);
			}

			Vbox vb = RevisiHelper.createNewRevisi(HasilUjianMahasiswa.class, hasilUjianMahasiswa,
					mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim());
			vb.setParent(hbox);

			vb.appendChild(new Label(mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			if (hasilUjianMahasiswa.getMulaiPada() != null) {
				new MyLabelKecil("Ujian tgl : " + (hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.dateFormat6.get().format(hasilUjianMahasiswa.getMulaiPada()))).setParent(vbox);
			}

			if (hasilUjianMahasiswa.getMulaiPada() != null) {
				new MyLabelKecil("waktu mulai : " + (hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.timeFormat.get().format(hasilUjianMahasiswa.getMulaiPada()))).setParent(vbox);
			}
			if (hasilUjianMahasiswa.getSelesaiPada() != null) {
				new MyLabelKecil("waktu selesai : " + (hasilUjianMahasiswa.getSelesaiPada() == null ? ""
						: Common.timeFormat.get().format(hasilUjianMahasiswa.getSelesaiPada()))).setParent(vbox);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox hb = new Hbox();
			hb.setParent(vbox);

			new MyLabelKecil("Ikut ujian").setParent(hb);
			final Intbox ikut = new Intbox(hasilUjianMahasiswa.getJumlahIkut());
			ikut.setCols(1);
			ikut.setStyle("font-size:9px;");
			ikut.setParent(hb);
			new MyLabelKecil("kali").setParent(hb);
			ikut.addEventListener("onChange", new EventListener() {

				/**
				 * Menyimpan perubahan kolom <b>"Ikut ujian ... kali"</b> ({@code jumlahIkut}).
				 *
				 * <p><b>Untuk apa kolom ini.</b> {@code jumlahIkut} membatasi berapa kali peserta
				 * boleh mengulang ujian. Menaikkannya memberi kesempatan ujian ulang — misalnya
				 * bagi peserta yang koneksinya terputus di tengah ujian.</p>
				 *
				 * <p><b>Pola muat-ubah-simpan</b> dipakai di sini (berbeda dari {@code Sisa Waktu}
				 * dan {@code Lengkapi ulang jawaban} yang wajib memakai HQL bulk update), karena
				 * {@code jumlahIkut} tidak memiliki getter yang memutasi field dari cache berkas,
				 * sehingga dirty-check Hibernate tidak akan menimpa nilai yang baru diketik.</p>
				 *
				 * <p><b>Kelemahan yang perlu diketahui: kegagalan berlangsung SENYAP.</b> Blok
				 * {@code catch} hanya me-rollback transaksi tanpa memberi tahu pengguna dan tanpa
				 * merekam ke {@code ErrorAuditUtil} — berbeda dari editor {@code Nilai} dan
				 * {@code Keterangan} di baris yang sama, yang menampilkan tanda &#10007; beserta
				 * pesan kegagalannya. Akibatnya admin dapat mengira perubahan tersimpan padahal
				 * tidak. Kotak isian juga tidak dikembalikan ke nilai lama saat gagal.</p>
				 *
				 * <p><b>Otorisasi.</b> Tidak ada pemeriksaan peran di dalam listener; lihat
				 * bagian Otorisasi pada Javadoc {@code render(...)}.</p>
				 *
				 * @param arg0 event {@code onChange}; nilai baru dibaca dari komponen {@code ikut}
				 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, hasilUjianMahasiswa.getId());
						if (hum != null) {
							hum.setJumlahIkut(ikut.getValue());
							session.update(hum);
						}
						tx.commit();
					} catch(Exception e) {
						if(tx != null) tx.rollback();
					} finally {
						if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:3993");}
					}
				}
			});

			// Lama Waktu = DURASI pengerjaan (getLamaPengerjaan dibuat via GregorianCalendar(0,0,0,jam,menit,detik)
			// -> bagian tanggalnya ngawur "31-12-0002"). Format bagian WAKTU-nya saja: HH:mm:ss (timeFormat1).
			new MyLabelKecil("Lama Waktu : " + (hasilUjianMahasiswa.getLamaPengerjaan() == null ? ""
					: Common.timeFormat1.get().format(hasilUjianMahasiswa.getLamaPengerjaan()))).setParent(vbox);

			final Timebox sisaWaktu = new ais.ui.util.MyTimebox(hasilUjianMahasiswa.getSisaWaktuPengerjaan());
			sisaWaktu.setStyle("font-size:9px;");
			sisaWaktu.setDisabled(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null);
			sisaWaktu.addEventListener("onChange", new EventListener() {

				/**
				 * Menyimpan penambahan/pengurangan <b>Sisa Waktu</b> pengerjaan seorang peserta —
				 * fitur yang dipakai admin untuk memberi tambahan waktu saat ujian masih
				 * BERLANGSUNG.
				 *
				 * <p><b>Mengapa memakai HQL bulk update, bukan muat-ubah-simpan.</b> Ini
				 * perbaikan atas laporan "admin tidak bisa menambah waktu ujian, nilainya kembali
				 * ke 0 setelah refresh". Pola lama ({@code session.get} + setter +
				 * {@code session.update} + commit) memicu Hibernate MEMANGGIL
				 * {@code getSisaWaktuPengerjaan()} saat dirty-check/flush karena pemetaan
				 * entity ini berbasis PROPERTY access. Getter tersebut SENGAJA menimpa nilai
				 * in-memory dengan isi cache berkas "live" ({@code retreive()}) yang pada saat
				 * commit masih memuat nilai LAMA — sinkronisasi ke berkas baru baru dilakukan
				 * SETELAH commit. Akibatnya nilai yang baru diketik admin tertimpa nilai lama.
				 * {@code update HasilUjianMahasiswa set sisaWaktuPengerjaan = :v where id = :id}
				 * tidak memuat entity dan tidak memanggil getter sama sekali, sehingga yang
				 * tersimpan persis sesuai masukan admin. Ini contoh konkret dampak pola
				 * arsitektur <i>getter yang memutasi field</i> yang tersebar di paket model —
				 * fakta arsitektur yang harus dipertahankan, bukan bug untuk "dirapikan".</p>
				 *
				 * <p><b>Dua langkah setelah commit.</b> {@code hasilUjianMahasiswa.put(...)}
				 * menyinkronkan cache berkas "live" agar layar ujian peserta yang MASIH berjalan
				 * tidak membaca sisa waktu basi, lalu {@code setSisaWaktuPengerjaan(...)}
				 * menyegarkan objek in-memory milik grid. Urutan ini penting: {@code put}
				 * lebih dulu, karena getter membaca dari berkas.</p>
				 *
				 * <p><b>Penjagaan.</b> Tidak melakukan apa pun bila nilai kosong. Komponen
				 * sendiri di-{@code setDisabled(...)} ketika {@code sisaWaktuPengerjaan} null
				 * (peserta belum mulai ujian). Kegagalan di-rollback dan direkam ke
				 * {@code ErrorAuditUtil}, meski tidak ditampilkan ke pengguna.</p>
				 *
				 * @param arg0 event {@code onChange}; nilai baru dibaca dari komponen {@code sisaWaktu}
				 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (sisaWaktu.getValue() != null) {
						// FIX (laporan: admin tidak bisa menambah waktu ujian, kembali ke 0 setelah
						// refresh): pola lama (session.get + setSisaWaktuPengerjaan + session.update +
						// commit) memicu Hibernate MEMANGGIL getter getSisaWaktuPengerjaan() saat
						// dirty-check/flush (mapping berbasis PROPERTY access) -- getter itu SENGAJA
						// menimpa nilai in-memory dengan cache file "live" (retreive()) yang masih
						// menyimpan nilai LAMA (blm disinkronkan) pada saat commit ini terjadi (sinkron
						// ke file baru dilakukan SETELAH commit, lihat put() di bawah) -> nilai admin
						// yang baru batal tersimpan / tertimpa nilai lama. FIX: UPDATE langsung via HQL
						// bulk update (tidak memuat entity, tidak memanggil getter sama sekali) supaya
						// nilai yang benar-benar tersimpan PERSIS sesuai input admin.
						Session session = null;
						Transaction tx = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							tx = session.beginTransaction();
							session.createQuery(
									"update HasilUjianMahasiswa set sisaWaktuPengerjaan = :v where id = :id")
									.setParameter("v", sisaWaktu.getValue())
									.setParameter("id", hasilUjianMahasiswa.getId()).executeUpdate();
							tx.commit();
						} catch(Exception e) {
							if(tx != null) tx.rollback();
							ais.common.ErrorAuditUtil.record(e, "HasilUjianMahasiswaHelper sisaWaktu onChange");
						} finally {
							if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4025");}
						}
						// Sinkronkan cache file "live" agar getSisaWaktuPengerjaan() (dipakai layar ujian
						// peserta yang MASIH berlangsung) tidak membaca nilai basi.
						hasilUjianMahasiswa.put(Common.databaseDateFormat1.get().format(sisaWaktu.getValue()));
						hasilUjianMahasiswa.setSisaWaktuPengerjaan(sisaWaktu.getValue());
					}
				}
			});

			hb = new Hbox();
			hb.setParent(vbox);
			new MyLabelKecil("Sisa Waktu").setParent(hb);
			sisaWaktu.setParent(hb);
			sisaWaktu.setCols(4);

			if (pertemuan != null && pertemuan.getPerkuliahan() != null
					&& pertemuan.getPerkuliahan().getKurikulum() != null
					&& pertemuan.getPerkuliahan().getKurikulum().apakahObe(pertemuan.getPerkuliahan().getTahunAjaran(),
							pertemuan.getPerkuliahan().getGanjilGenap())) {

				Vbox vboxDa = new Vbox();
				vboxDa.setParent(arg0);

				Session session = null;
				List<FormatNilai> formatNilais = new ArrayList<FormatNilai>();
				// BULLETPROOF: baca nilaiObe LANGSUNG dari DB (bypass cache MapDB ambilByKey yang bisa
				// basi) agar kolom Skor/Max selalu mencerminkan hasil "Hitung Ulang" terbaru.
				String nilaiObeStr = hasilUjianMahasiswa.getNilaiObe();
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					formatNilais = Common.getFormatNilais(session, pertemuan.getPerkuliahan());
					if (hasilUjianMahasiswa.getId() != null) {
						Object v = session.createCriteria(HasilUjianMahasiswa.class)
								.add(org.hibernate.criterion.Restrictions.idEq(hasilUjianMahasiswa.getId()))
								.setProjection(org.hibernate.criterion.Projections.property("nilaiObe"))
								.uniqueResult();
						if (v != null) {
							nilaiObeStr = (String) v;
						}
					}
				} finally {
					if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4064");}
				}
				JSONObject jsonObjectHasil = new JSONObject(
						nilaiObeStr == null || nilaiObeStr.trim().isEmpty() ? "{}" : nilaiObeStr);

				for (FormatNilai nilai : formatNilais) {
					if (nilai.getStatusPertemuan() != null) {
						if (!jsonObjectHasil.isNull(nilai.getId().toString())) {

							Double nilaiDapat = jsonObjectHasil.getDouble(nilai.getId().toString());
							Double nilaiMax = jsonObjectHasil.isNull(nilai.getId().toString() + "_max") ? 0.0
									: jsonObjectHasil.getDouble(nilai.getId().toString() + "_max");

							Hbox hbSkorObe = new Hbox();
							hbSkorObe.setParent(vboxDa);
							new MyLabelKecil(nilai.getNama() + " : " + Common.numberFormat.get().format(nilaiDapat)
									+ (nilaiMax.equals(0.0) ? "" : " / " + Common.numberFormat.get().format(nilaiMax)))
									.setParent(hbSkorObe);
							if (nilaiDapat.doubleValue() == 0.0 && nilaiMax.doubleValue() > 0.0) {
								tombolBantuanNilaiNol(hasilUjianMahasiswa, totalSoal, terjawab).setParent(hbSkorObe);
							}

						}
					}
				}

			} else {

				Hbox hbSkorPg = new Hbox();
				hbSkorPg.setParent(arg0);
				new Label(Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())
						+ (hasilUjianMahasiswa.getJawabanBenarMax() == null ? ""
								: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenarMax())))
						.setParent(hbSkorPg);
				double skorDapatPg = hasilUjianMahasiswa.getJawabanBenar() == null ? 0.0
						: hasilUjianMahasiswa.getJawabanBenar().doubleValue();
				double skorMaxPg = hasilUjianMahasiswa.getJawabanBenarMax() == null ? 0.0
						: hasilUjianMahasiswa.getJawabanBenarMax().doubleValue();
				if (skorDapatPg == 0.0 && skorMaxPg > 0.0) {
					tombolBantuanNilaiNol(hasilUjianMahasiswa, totalSoal, terjawab).setParent(hbSkorPg);
				}
			}

			hb = new Hbox();
			hb.setParent(vbox);
			new MyLabelKecil("Nomor Terakhir").setParent(hb);
			int startIndex = 0;
			try {
				String ss = hasilUjianMahasiswa.retreive("index");
				if (ss != null && !ss.trim().isEmpty()) {
					startIndex = Integer.parseInt(ss.trim());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4103");
			}

			final MyIntbox startIndexInput = new MyIntbox(startIndex + 1);
			startIndexInput.setStyle("font-size:8px;");
			startIndexInput.setCols(2);
			startIndexInput.addEventListener("onChange", new EventListener() {

				/**
				 * Menyimpan <b>"Nomor Terakhir"</b>, yaitu nomor soal tempat peserta akan
				 * melanjutkan ujian.
				 *
				 * <p><b>Bukan ke basis data.</b> Berbeda dari editor lain di baris ini, nilai
				 * ditulis ke penyimpanan berkas "live" milik entity lewat
				 * {@code hasilUjianMahasiswa.put(nilai, "index")} — bukan lewat transaksi
				 * Hibernate. Penyimpanan berkas itulah yang dibaca layar ujian peserta yang
				 * sedang berjalan, sehingga perubahan langsung berlaku tanpa menunggu commit
				 * maupun invalidasi cache.</p>
				 *
				 * <p><b>Pergeseran satu.</b> Yang ditampilkan kepada admin adalah nomor
				 * BERBASIS-SATU (soal ke-1, ke-2, ...), sedangkan yang disimpan adalah indeks
				 * BERBASIS-NOL. Karena itu nilai dikurangi 1 saat disimpan, sebagaimana ia
				 * ditambah 1 saat dibaca ke dalam {@code MyIntbox}. Masukan kosong
				 * ({@code getValue()} null) disimpan sebagai 0 — mengembalikan peserta ke soal
				 * pertama.</p>
				 *
				 * <p><b>Tanpa penanganan error.</b> Tidak ada {@code try/catch}: kegagalan
				 * penulisan berkas akan merambat sebagai exception ZK dan tampil sebagai galat
				 * pada antarmuka.</p>
				 *
				 * @param arg0 event {@code onChange}; nilai baru dibaca dari {@code startIndexInput}
				 * @throws Exception diteruskan dari penulisan berkas penyimpanan "live"
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					hasilUjianMahasiswa.put(
							(startIndexInput.getValue() == null ? 0 : (startIndexInput.getValue() - 1)) + "", "index");
				}
			});
			startIndexInput.setParent(hb);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil("Jml Soal : " + Common.numberFormat.get().format(totalSoal)).setParent(vbox);
			new MyLabelAgakKecil("Soal Terjawab : " + Common.numberFormat.get().format(terjawab) + " / "
					+ Common.numberFormat.get().format(persen) + "%").setParent(vbox);
			new MyLabelAgakKecil("Soal Belum Terjawab : " + Common.numberFormat.get().format(belum) + " / "
					+ Common.numberFormat.get().format(persenBelum) + "%").setParent(vbox);

			if (persen.intValue() > 0 && persen.intValue() < 100) {
				arg0.setStyle("background-color: rgba(205,92,92,0.4);");
			} else if (persen.intValue() == 100) {
				arg0.setStyle("background:#eeffeb;");
			}

			final MyCheckboxConfig lengkapiJawaban = new MyCheckboxConfig(
					"Lengkapi ulang jawaban (pilihan ini tidak aktif kembali ketika peserta telah ujian ulang)");
			lengkapiJawaban.setStyle("font-size:8px;");
			lengkapiJawaban.setChecked(hasilUjianMahasiswa.getLengkapiJawaban());
			lengkapiJawaban.addEventListener("onClick", new EventListener() {

				/**
				 * Menyimpan centang <b>"Lengkapi ulang jawaban"</b> — izin bagi peserta untuk
				 * kembali masuk dan melengkapi jawaban yang belum terisi.
				 *
				 * <p>Label komponennya menegaskan bahwa pilihan ini TIDAK aktif lagi begitu
				 * peserta menjalani ujian ulang, sehingga izin bersifat sekali pakai.</p>
				 *
				 * <p><b>Memakai HQL bulk update</b> dengan alasan yang sama seperti editor
				 * {@code Sisa Waktu} di atas — laporan aslinya berbunyi "checklist ini kembali
				 * hilang setelah refresh". Memuat entity lalu menyimpannya berisiko memicu getter
				 * lain milik entity ini yang memiliki efek samping (menimpa field dari cache
				 * berkas) saat dirty-check, sehingga nilai yang tersimpan tidak sesuai pilihan
				 * admin. Bulk update tidak memuat entity dan tidak memanggil getter apa pun.</p>
				 *
				 * <p>Setelah commit, objek in-memory milik grid ikut disegarkan dengan
				 * {@code setLengkapiJawaban(...)} supaya keadaan centang konsisten tanpa perlu
				 * memuat ulang baris. Kegagalan di-rollback dan direkam ke {@code ErrorAuditUtil},
				 * namun tidak ditampilkan ke pengguna dan centang tidak dikembalikan ke keadaan
				 * semula.</p>
				 *
				 * @param arg0 event {@code onClick}; keadaan baru dibaca dari
				 *             {@code lengkapiJawaban.isChecked()}
				 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					// FIX (laporan: checklist ini kembali hilang setelah refresh) — pola sama dengan
					// perbaikan "Sisa Waktu" di atas: pakai HQL bulk update (tanpa memuat entity /
					// memanggil getter lain milik entity ini yang bisa punya efek samping) agar nilai
					// yang tersimpan pasti sesuai pilihan admin.
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						session.createQuery(
								"update HasilUjianMahasiswa set lengkapiJawaban = :v where id = :id")
								.setParameter("v", lengkapiJawaban.isChecked())
								.setParameter("id", hasilUjianMahasiswa.getId()).executeUpdate();
						tx.commit();
					} catch(Exception e) {
						if(tx != null) tx.rollback();
						ais.common.ErrorAuditUtil.record(e, "HasilUjianMahasiswaHelper lengkapiJawaban onClick");
					} finally {
						if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4165");}
					}
					hasilUjianMahasiswa.setLengkapiJawaban(lengkapiJawaban.isChecked());
				}
			});
			lengkapiJawaban.setParent(vbox);

			if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {

				if (pertemuan != null && pertemuan.getPerkuliahan() != null
						&& pertemuan.getPerkuliahan().getKurikulum() != null
						&& pertemuan.getPerkuliahan().getKurikulum().apakahObe(
								pertemuan.getPerkuliahan().getTahunAjaran(),
								pertemuan.getPerkuliahan().getGanjilGenap())) {

					Vbox vboxDa = new Vbox();
					vboxDa.setParent(arg0);

					Session session = null;
					List<FormatNilai> formatNilais = new ArrayList<FormatNilai>();
					// BULLETPROOF: baca nilaiObe LANGSUNG dari DB (bypass cache MapDB) agar kolom Nilai
					// per Sub-CPMK selalu mencerminkan hasil "Hitung Ulang" terbaru.
					String nilaiObeStr = hasilUjianMahasiswa.getNilaiObe();
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						formatNilais = Common.getFormatNilais(session, pertemuan.getPerkuliahan());
						if (hasilUjianMahasiswa.getId() != null) {
							Object v = session.createCriteria(HasilUjianMahasiswa.class)
									.add(org.hibernate.criterion.Restrictions.idEq(hasilUjianMahasiswa.getId()))
									.setProjection(org.hibernate.criterion.Projections.property("nilaiObe"))
									.uniqueResult();
							if (v != null) {
								nilaiObeStr = (String) v;
							}
						}
					} finally {
						if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4200");}
					}
					JSONObject jsonObjectHasil = new JSONObject(
							nilaiObeStr == null || nilaiObeStr.trim().isEmpty() ? "{}" : nilaiObeStr);

					for (FormatNilai nilai : formatNilais) {
						if (nilai.getStatusPertemuan() != null) {
							if (!jsonObjectHasil.isNull(nilai.getId().toString())) {

								Double nilaiDapat = jsonObjectHasil.getDouble(nilai.getId().toString());
								Double nilaiMax = jsonObjectHasil.isNull(nilai.getId().toString() + "_max") ? 0.0
										: jsonObjectHasil.getDouble(nilai.getId().toString() + "_max");

								Hbox hbNilaiObe = new Hbox();
								hbNilaiObe.setParent(vboxDa);
								MyLabelKecil lblNilai = new MyLabelKecil(nilai.getNama() + (nilaiMax.equals(0.0) ? ""
										: " : " + Common.numberFormat.get().format((nilaiDapat * 100.0) / nilaiMax)));
								if (!nilaiMax.equals(0.0)) {
									// Nilai per Sub-CPMK dapat diklik → popup daftar soal + skor yang didapat.
									lblNilai.setStyle("cursor:pointer; text-decoration:underline; color:#2563eb;");
									lblNilai.setTooltiptext("Klik untuk melihat rincian soal & skor " + nilai.getNama());
									final FormatNilai fnKlik = nilai;
									final HasilUjianMahasiswa himKlik = hasilUjianMahasiswa;
									lblNilai.addEventListener("onClick", new EventListener() {
										/**
										 * Membuka popup rincian skor untuk Sub-CPMK yang labelnya
										 * diklik.
										 *
										 * <p>Variabel {@code himKlik} dan {@code fnKlik} adalah
										 * salinan {@code final} yang dibuat tepat sebelum listener
										 * ini — perlu karena {@code nilai} adalah variabel
										 * perulangan yang berubah pada iterasi berikutnya.
										 * Tanpa salinan, seluruh label akan merujuk Sub-CPMK yang
										 * terakhir diproses (dan pada Java 7 kode tidak akan
										 * dikompilasi sama sekali).</p>
										 *
										 * <p>Listener ini hanya dipasang bila {@code nilaiMax != 0};
										 * pada skor maksimal nol rincian tidak bermakna sehingga
										 * label dibiarkan sebagai teks biasa tanpa gaya
										 * dapat-diklik.</p>
										 *
										 * @param ev event {@code onClick}; tidak dipakai
										 * @throws Exception diteruskan dari pembangunan popup
										 */
										@Override
										public void onEvent(Event ev) throws Exception {
											bukaPopupRincianSubCpmk(himKlik, fnKlik);
										}
									});
								}
								lblNilai.setParent(hbNilaiObe);
								if (nilaiDapat.doubleValue() == 0.0 && nilaiMax.doubleValue() > 0.0) {
									tombolBantuanNilaiNol(hasilUjianMahasiswa, totalSoal, terjawab).setParent(hbNilaiObe);
								}

							}
						}
					}

				} else {
					final HasilUjianMahasiswa himNilaiKlik = hasilUjianMahasiswa;
					Hbox hbNilaiPg = new Hbox();
					hbNilaiPg.setParent(arg0);
					MyLabelKecil lblNilaiPg = new MyLabelKecil(
						Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()) + "");
					// Nilai (pilihan ganda) dapat DIKLIK -> popup perbandingan Skor Jawaban vs Skor Diperoleh
					// per soal (mirip versi OBE). Memudahkan menemukan soal berdata tak wajar (skor > maks).
					lblNilaiPg.setStyle("cursor:pointer; text-decoration:underline; color:#2563eb;");
					lblNilaiPg.setTooltiptext("Klik untuk melihat perbandingan Skor Jawaban vs Skor Diperoleh per soal");
					lblNilaiPg.addEventListener("onClick", new EventListener() {
						/**
						 * Membuka popup <b>"Perbandingan Skor Jawaban vs Skor Diperoleh"</b> untuk
						 * peserta ujian pilihan ganda non-OBE.
						 *
						 * <p>Popup ini adalah alat diagnosis nilai janggal: ia menyorot MERAH
						 * setiap soal bertipe poin yang skor diperolehnya MELEBIHI skor
						 * maksimalnya — data tak wajar yang menjadi penyebab lazim nilai peserta
						 * melampaui 100.</p>
						 *
						 * <p>{@code himNilaiKlik} adalah salinan {@code final} dari entity baris,
						 * disiapkan tepat sebelum listener ini karena ditangkap anonymous inner
						 * class pada Java 7.</p>
						 *
						 * <p>Berbeda dari label Sub-CPMK versi OBE, label ini SELALU dibuat dapat
						 * diklik tanpa syarat skor maksimal.</p>
						 *
						 * @param ev event {@code onClick}; tidak dipakai
						 * @throws Exception diteruskan dari pembangunan popup
						 */
						@Override
						public void onEvent(Event ev) throws Exception {
							bukaPopupPerbandinganSkor(himNilaiKlik);
						}
					});
					lblNilaiPg.setParent(hbNilaiPg);
					double nilaiPg = hasilUjianMahasiswa.getNilai() == null ? 0.0
							: hasilUjianMahasiswa.getNilai().doubleValue();
					if (nilaiPg == 0.0) {
						tombolBantuanNilaiNol(hasilUjianMahasiswa, totalSoal, terjawab).setParent(hbNilaiPg);
					}
				}
			} else {

				hbox = new Hbox();
				hbox.setParent(arg0);

				final MyDoublebox doublebox = new MyDoublebox();
				doublebox.setCols(3);
				doublebox.setValue(tempHasilUjianMahasiswa.getNilai());
				doublebox.setParent(hbox);
				double nilaiEssay = tempHasilUjianMahasiswa.getNilai() == null ? 0.0
						: tempHasilUjianMahasiswa.getNilai().doubleValue();
				if (nilaiEssay == 0.0) {
					tombolBantuanNilaiNol(tempHasilUjianMahasiswa, totalSoal, terjawab).setParent(hbox);
				}
				final org.zkoss.zul.Label lblAutoNilai = new org.zkoss.zul.Label("");
				lblAutoNilai.setStyle("color:green;font-size:13px;font-weight:bold;");
				lblAutoNilai.setParent(hbox);
				doublebox.addEventListener("onChange", new EventListener() {

					/**
					 * <b>Editor NILAI langsung</b> untuk ujian esai: menyimpan angka yang diketik
					 * dosen ke kolom {@code nilai} peserta.
					 *
					 * <p>Inilah jalur paling langsung untuk mengubah nilai peserta di seluruh
					 * kelas ini — satu kali ketik lalu pindah fokus, dan nilai tersimpan. Tidak
					 * ada dialog konfirmasi, tidak ada validasi rentang (nilai negatif atau lebih
					 * dari 100 diterima apa adanya), dan tidak ada jejak audit nilai lama.</p>
					 *
					 * <p><b>Otorisasi.</b> Listener TIDAK memeriksa peran pengguna sama sekali.
					 * Bandingkan dengan tombol "Reset Ujian" beberapa sel di kanannya, yang
					 * dijaga {@code Common.getCurrentUser()} bukan mahasiswa dan bukan siswa.
					 * Ketimpangan ini disengaja atau tidak, dampaknya sama: perlindungan kolom
					 * nilai bersandar sepenuhnya pada asumsi bahwa grid rekap hanya dapat dibuka
					 * dari layar dosen/admin. Setiap penambahan jalur baru yang memanggil
					 * {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)}
					 * WAJIB memverifikasi peran pemanggilnya.</p>
					 *
					 * <p><b>Umpan balik simpan-otomatis.</b> Berhasil menampilkan &#10003; hijau
					 * bertooltip "Tersimpan" yang memudar sendiri setelah 2,5 detik lewat
					 * {@code Clients.evalJavaScript}; gagal menampilkan &#10007; merah bertooltip
					 * berisi pesan kesalahan. Pola ini jauh lebih baik daripada editor
					 * {@code Ikut ujian} yang gagal secara senyap, dan layak ditiru bila menambah
					 * editor sebaris baru.</p>
					 *
					 * <p><b>Catatan konsistensi.</b> Menulis {@code nilai} secara langsung TIDAK
					 * memperbarui {@code nilaiObe}, {@code jawabanBenar}, maupun cache MapDB.
					 * Menjalankan "Hitung Ulang Semua" setelahnya akan MENIMPA angka yang diketik
					 * di sini dengan hasil perhitungan otomatis — meski pada ujian esai
					 * {@code hitungWaktu} tidak menyentuh kolom nilai, sehingga dalam praktik
					 * nilai esai yang diketik manual bertahan.</p>
					 *
					 * @param arg0 event {@code onChange}; nilai baru dibaca dari {@code doublebox}
					 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = null;
						Transaction tx = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							tx = session.beginTransaction();
							HasilUjianMahasiswa hum = (HasilUjianMahasiswa) session.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.idEq(tempHasilUjianMahasiswa.getId())).uniqueResult();
							
							if(hum != null) {
								hum.setNilai(doublebox.getValue());
								session.update(hum);
							}
							tx.commit();
							lblAutoNilai.setStyle("color:green;font-size:13px;font-weight:bold;");
							lblAutoNilai.setValue("✓");
							lblAutoNilai.setTooltiptext("Tersimpan");
							org.zkoss.zk.ui.util.Clients.evalJavaScript(
								"(function(){var e=document.getElementById('" + lblAutoNilai.getUuid() + "');"
								+ "if(!e)return;e.style.transition='none';e.style.opacity='1';"
								+ "setTimeout(function(){e.style.transition='opacity 0.8s';e.style.opacity='0';},2500);"
								+ "})();"
							);
						} catch(Exception e) {
							if(tx != null) tx.rollback();
							lblAutoNilai.setValue("✗");
							lblAutoNilai.setStyle("color:red;font-size:13px;font-weight:bold;");
							lblAutoNilai.setTooltiptext("Gagal simpan: " + e.getMessage());
						} finally {
							if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4279");}
						}
					}
				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang",
						"/img/Button-Refresh-icon.png");
				// Ikon saja (label disembunyikan) agar tidak melebarkan sel Nilai -> sebelumnya label
				// panjang "Hitung Ulang" membuat tabel meluap & kolom kanan (Pelanggaran) terpotong.
				button.setLabel("");
				button.setOrient("horizontal");
				button.setTooltiptext("Hitung Ulang");
				button.addEventListener("onClick", new EventListener() {
					/**
					 * Tombol <b>Hitung Ulang</b> per baris (ikon saja, tanpa label): menghitung
					 * ulang nilai SATU peserta esai dari skor koreksi per soal.
					 *
					 * <p><b>Label sengaja dikosongkan</b> ({@code setLabel("")}) agar tombol tidak
					 * melebarkan sel Nilai yang sempit — tanpa itu tabel meluap dan kolom paling
					 * kanan ("Pelanggaran") terpotong. Maksudnya tetap terbaca lewat
					 * {@code tooltiptext}.</p>
					 *
					 * <p><b>Rumus yang dipakai BERBEDA dari mesin penilaian pusat.</b> Untuk tiap
					 * soal dalam paket peserta, diambil <b>SATU</b> {@link HasilUjianMahasiswaDetail}
					 * saja ({@code hasilUjianMahasiswaDetails.iterator().next()}), lalu
					 * dijumlahkan {@code (nilaiDetail * 100 / skorSoal)} dan hasilnya dibagi
					 * jumlah soal. Untuk soal berjawaban ganda — yang memiliki lebih dari satu
					 * baris detail — jawaban selain yang pertama diabaikan, sehingga hasilnya
					 * dapat menyimpang dari {@code ProsesUjianHelper.hitungPilihanGanda}.
					 * Perbedaan ini perlu diketahui sebelum membandingkan angka tombol ini dengan
					 * angka hasil "Hitung Ulang Semua".</p>
					 *
					 * <p><b>Pemuatan paksa.</b> Detail jawaban diambil dengan overload empat
					 * argumen ber-{@code refresh=true}, sehingga skor koreksi yang baru saja
					 * disimpan dosen langsung terbaca tanpa terhalang cache.</p>
					 *
					 * <p><b>Ambang 0.1.</b> Nilai hanya ditulis bila total {@code > 0.1}. Bila
					 * tidak, muncul pesan bahwa hasil ujian peserta ini belum dikoreksi, disertai
					 * dua langkah yang dapat dilakukan. Penjagaan ini mencegah peserta yang belum
					 * dikoreksi tertimpa nilai 0.</p>
					 *
					 * <p><b>Pembagian nol.</b> {@code skor} soal bernilai 0 menghasilkan
					 * {@code Infinity}, bukan exception; nilai yang tercemar akan lolos ambang dan
					 * tersimpan. Periksa skor soal di bank soal bila menemukan nilai janggal.</p>
					 *
					 * <p>Setelah commit, {@code doublebox} disegarkan agar angka baru langsung
					 * terlihat. Kegagalan di-rollback dan direkam, tanpa pesan ke pengguna.</p>
					 *
					 * @param event event {@code onClick}; tidak dipakai
					 * @throws Exception diteruskan dari pemuatan detail jawaban
					 */
					@Override
					public void onEvent(Event event) throws Exception {

						MyArrayList<Long> ujianPunyaSoals = tempHasilUjianMahasiswa.ambilUjianPunyaSoals(
								tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
								true);
						MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = tempHasilUjianMahasiswa
								.ambilHasilUjianMahasiswaDetail(true,
										tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
										new Label(), ujianPunyaSoals);

						Double sumNilai = 0.0;
						for (Long ujianPunyaSoalid : ujianPunyaSoals) {
							UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
									.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
							if (ujianPunyaSoal != null) {
								BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
								Set<Long> hasilUjianMahasiswaDetails = hasilUjianMahasiswaDetailsa
										.get(bankSoal.getId());
								if (hasilUjianMahasiswaDetails != null && !hasilUjianMahasiswaDetails.isEmpty()) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													hasilUjianMahasiswaDetails.iterator().next().toString());
									if (hasilUjianMahasiswaDetail != null) {
										Double nilai = hasilUjianMahasiswaDetail.getNilai();
										Double skor = bankSoal.getSkor();
										sumNilai += (nilai * 100.0) / skor;
									}
								}
							}
						}

						if (sumNilai != null && sumNilai.doubleValue() > 0.1) {

							Double n = sumNilai.doubleValue() / ujianPunyaSoals.size();

							Session session = null;
							Transaction tx = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								tx = session.beginTransaction();
								
								HasilUjianMahasiswa hum = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, tempHasilUjianMahasiswa.getId());
								if (hum != null) {
									hum.setNilai(n);
									session.update(hum);
								}
								tx.commit();
							} catch (Exception e) {
								if (tx != null) tx.rollback();
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4342");
							} finally {
								if (session != null && session.isOpen()) {
									try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4345");}
								}
							}
							doublebox.setValue(n);

						} else {
							MyMessageboxConfig.show(
				"Mohon maaf, hasil ujian mahasiswa ini belum Anda koreksi. Langkah yang dapat dilakukan: (1) lakukan koreksi terhadap jawaban peserta terlebih dahulu; (2) setelah nilai terisi, ulangi kembali tindakan ini.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
					}

				});
				button.setParent(hbox);

			}

			final MyTextbox keterangan = new MyTextbox(tempHasilUjianMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);
			org.zkoss.zul.Hbox hboxKet = new org.zkoss.zul.Hbox();
			hboxKet.setParent(arg0);
			keterangan.setParent(hboxKet);
			final org.zkoss.zul.Label lblAutoKet = new org.zkoss.zul.Label("");
			lblAutoKet.setStyle("color:green;font-size:13px;font-weight:bold;");
			lblAutoKet.setParent(hboxKet);

			// === Kolom Pelanggaran (rekap pengawasan ujian / anti-curang) ===
			int jmlLgr = hasilUjianMahasiswa.getJumlahPelanggaran() == null ? 0
					: hasilUjianMahasiswa.getJumlahPelanggaran().intValue();
			Vbox vboxLgr = new Vbox();
			vboxLgr.setParent(arg0);
			Label lblJmlLgr = new Label(jmlLgr > 0 ? (jmlLgr + " pelanggaran") : "0 (bersih)");
			lblJmlLgr.setStyle(jmlLgr > 0 ? "color:#b91c1c;font-weight:bold;" : "color:#16a34a;");
			lblJmlLgr.setParent(vboxLgr);
			String logLgr = hasilUjianMahasiswa.getLogPelanggaran();
			if (logLgr != null && !logLgr.trim().isEmpty()) {
				Label lblLogLgr = new Label(logLgr.length() > 400 ? logLgr.substring(0, 400) + " ..." : logLgr);
				lblLogLgr.setMultiline(true);
				lblLogLgr.setPre(true);
				lblLogLgr.setStyle("font-size:10px;color:#64748b;white-space:pre-wrap;");
				lblLogLgr.setTooltiptext(logLgr);
				lblLogLgr.setParent(vboxLgr);
			}

			// === Tombol Reset Ujian per peserta (admin/dosen saja) ===
			final String namaPesertaReset = mahasiswa != null ? mahasiswa.getNama()
					: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama()
					: siswa != null ? siswa.getNama()
					: calonSiswa != null ? calonSiswa.getNama() : "peserta";
			Tbmuser tbmuserCurrent = Common.getCurrentUser();
			if (tbmuserCurrent != null && tbmuserCurrent.getMahasiswa() == null
					&& tbmuserCurrent.getSiswa() == null) {
				Vbox vboxReset = new Vbox();
				vboxReset.setParent(arg0);
				MyToolbarbuttonConfig btnReset = new MyToolbarbuttonConfig("Reset Ujian", "/img/svg/trash.svg");
				btnReset.setTooltiptext("Reset ujian " + namaPesertaReset + " — seolah belum pernah mengikuti ujian sama sekali");
				btnReset.setStyle("color:#b91c1c;");
				btnReset.addEventListener("onClick", new EventListener() {
					/**
					 * Tombol <b>Reset Ujian</b> per peserta: meminta konfirmasi sebelum menghapus
					 * seluruh jawaban dan riwayat pengerjaan seorang peserta, seolah ia belum
					 * pernah mengikuti ujian sama sekali.
					 *
					 * <p>Berbeda dari "Ulang Semua" di toolbar yang mengosongkan SELURUH peserta,
					 * tombol ini bekerja pada satu peserta — dipakai ketika hanya satu orang yang
					 * perlu mengulang, misalnya karena kendala teknis saat ujian berlangsung.
					 * Nama peserta disebut eksplisit pada dialog konfirmasi dan pada
					 * {@code tooltiptext} agar admin tidak salah sasaran.</p>
					 *
					 * <p><b>Gerbang otorisasi.</b> Tombol hanya dibuat bila
					 * {@code Common.getCurrentUser()} bukan mahasiswa dan bukan siswa. Perhatikan
					 * bahwa gerbang ini TIDAK mengecualikan pengguna ber-{@code biodataCalonMahasiswa}
					 * maupun ber-{@code calonSiswa} — berbeda dari gerbang tombol toolbar di
					 * {@link HasilUjianMahasiswaHelper#display(PertemuanPunyaUjian, Component)}
					 * yang memeriksa keempat peran peserta. Ini satu-satunya pemeriksaan peran di
					 * dalam renderer baris.</p>
					 *
					 * @param onClickEvent event {@code onClick}; tidak dipakai
					 * @throws Exception diteruskan dari pembangunan dialog konfirmasi
					 */
					@Override
					public void onEvent(Event onClickEvent) throws Exception {
						MyMessageboxConfig.show(
							"Yakin mereset ujian " + namaPesertaReset + "?\n\nSemua jawaban dan riwayat pengerjaan akan dihapus.",
							"Konfirmasi Reset Ujian",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION,
							new EventListener() {
								/**
								 * Menerima jawaban dialog konfirmasi Reset Ujian dan meneruskannya
								 * bila pengguna memilih OK.
								 *
								 * <p>Pekerjaan sesungguhnya tidak dijalankan langsung melainkan
								 * dibungkus {@code Common.createDefaultTimer(...)}, agar transaksi
								 * penghapusan berjalan di siklus event berikutnya dan tidak
								 * memblokir antrean event ZK selagi dialog ditutup.</p>
								 *
								 * @param okEvent event dialog; {@code getData()} berisi kode tombol
								 * @throws Exception diteruskan dari penguraian kode tombol
								 */
								@Override
								public void onEvent(Event okEvent) throws Exception {
									int pilihan = Integer.parseInt(okEvent.getData().toString());
									if (pilihan == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {
											/**
											 * Melaksanakan reset ujian satu peserta dalam SATU
											 * transaksi.
											 *
											 * <p><b>Tiga langkah.</b> (1) Seluruh
											 * {@link HasilUjianMahasiswaDetail} milik peserta
											 * di-{@code null}-kan pada {@code bankSoalDetail},
											 * {@code jawaban}, dan {@code waktuJawab} — baris
											 * detail sengaja TIDAK dihapus agar struktur paket
											 * soal peserta tetap utuh dan relasi ke lampiran
											 * jawaban tidak putus, sama seperti aksi "Ulang
											 * Semua". (2) Entity utama diambil ulang sebagai
											 * instance terkelola lalu {@code reset()} dipanggil,
											 * disusul pengosongan {@code sisaWaktuPengerjaan},
											 * {@code jumlahPelanggaran}, dan
											 * {@code logPelanggaran} — sehingga rekam jejak
											 * pelanggaran pengawasan ujian IKUT TERHAPUS, hal
											 * yang perlu disadari bila catatan itu masih
											 * diperlukan sebagai bukti. (3) Baris dirender ulang
											 * lewat timer bersarang.</p>
											 *
											 * <p>Kegagalan me-rollback transaksi, merekam ke
											 * {@code ErrorAuditUtil} dengan menyertakan id
											 * peserta, dan menampilkan pesan galat kepada
											 * pengguna. Session ditutup di {@code finally}.</p>
											 *
											 * @param timerEvent event timer; tidak dipakai
											 * @throws Exception diteruskan dari operasi basis data
											 */
											@SuppressWarnings("unchecked")
											@Override
											public void onEvent(Event timerEvent) throws Exception {
												Session sess = null;
												Transaction tx = null;
												try {
													sess = HibernateUtil.getSessionFactory().openSession();
													tx = sess.beginTransaction();
													// 1. Hapus semua jawaban detail
													java.util.List<HasilUjianMahasiswaDetail> details = sess
															.createCriteria(HasilUjianMahasiswaDetail.class)
															.add(Restrictions.eq("hasilUjianMahasiswa", tempHasilUjianMahasiswa))
															.list();
													for (HasilUjianMahasiswaDetail hmd : details) {
														hmd.setBankSoalDetail(null);
														hmd.setJawaban(null);
														hmd.setWaktuJawab(null);
														sess.update(hmd);
													}
													// 2. Reset entitas utama
													HasilUjianMahasiswa humRefresh = (HasilUjianMahasiswa) sess.get(
															HasilUjianMahasiswa.class, tempHasilUjianMahasiswa.getId());
													if (humRefresh != null) {
														humRefresh.reset();
														humRefresh.setSisaWaktuPengerjaan(null);
														humRefresh.setJumlahPelanggaran(null);
														humRefresh.setLogPelanggaran(null);
														sess.update(humRefresh);
													}
													tx.commit();
													// 3. Reload baris
													Common.createDefaultTimer(new EventListener() {
														/**
														 * Merender ulang baris setelah reset
														 * berhasil, sehingga sel skor, nilai,
														 * statistik jawaban, pewarnaan latar, dan
														 * kolom pelanggaran kembali menampilkan
														 * keadaan "belum mengerjakan".
														 *
														 * <p>Timer bersarang di dalam timer:
														 * pembersihan dan perenderan ditunda satu
														 * siklus lagi setelah transaksi commit,
														 * agar pembacaan ulang data tidak
														 * mendahului penyelesaian transaksi.</p>
														 *
														 * @param reloadEvent event timer; tidak dipakai
														 * @throws Exception diteruskan dari perenderan ulang
														 */
														@Override
														public void onEvent(Event reloadEvent) throws Exception {
															Common.clear(arg0);
															render(arg0, arg1);
														}
													});
												} catch (Exception ex) {
													if (tx != null) tx.rollback();
													ais.common.ErrorAuditUtil.record(ex,
															"auto-audit resetUjian HasilUjianMahasiswaHelper id=" + tempHasilUjianMahasiswa.getId());
													MyMessageboxConfig.show("Gagal reset: " + ex.getMessage(),
															"Error", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												} finally {
													if (sess != null && sess.isOpen()) {
														try { sess.close(); } catch (Exception ex) {
															ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:resetUjian");
														}
													}
												}
											}
										});
									}
								}
							});
					}
				});
				btnReset.setParent(vboxReset);
			}

			keterangan.addEventListener("onChange", new EventListener() {

				/**
				 * Menyimpan catatan bebas dosen pada kolom <b>Keterangan</b> peserta.
				 *
				 * <p>Dipakai untuk mencatat hal-hal yang tidak tertampung kolom terstruktur —
				 * misalnya alasan pemberian tambahan waktu, catatan kendala teknis, atau
				 * keterangan hasil verifikasi dugaan kecurangan.</p>
				 *
				 * <p><b>Pola muat-ubah-simpan</b> dipakai di sini; {@code keterangan} tidak
				 * memiliki getter yang memutasi field sehingga tidak memerlukan HQL bulk update
				 * seperti {@code Sisa Waktu} dan {@code Lengkapi ulang jawaban}.</p>
				 *
				 * <p><b>Umpan balik simpan-otomatis</b> sama seperti editor Nilai: &#10003; hijau
				 * bertooltip "Tersimpan" yang memudar sendiri setelah 2,5 detik lewat
				 * {@code Clients.evalJavaScript}, atau &#10007; merah bertooltip berisi pesan
				 * kesalahan bila gagal.</p>
				 *
				 * <p><b>Tanpa pembatasan panjang dan tanpa pelolosan HTML</b> di sisi ini; isi
				 * kolom diperlakukan sebagai teks biasa oleh komponen {@code Textbox} yang
				 * menampilkannya kembali, sehingga tidak menjadi jalur injeksi pada layar ini.
				 * Berhati-hatilah bila kelak kolom ini dirender melalui komponen HTML.</p>
				 *
				 * @param arg0 event {@code onChange}; nilai baru dibaca dari {@code keterangan}
				 * @throws Exception tidak dilempar dalam praktik — badan dibungkus try/catch
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, tempHasilUjianMahasiswa.getId());
						if(hum != null) {
							hum.setKeterangan(keterangan.getValue());
							session.update(hum);
						}
						tx.commit();
						lblAutoKet.setStyle("color:green;font-size:13px;font-weight:bold;");
						lblAutoKet.setValue("✓");
						lblAutoKet.setTooltiptext("Tersimpan");
						org.zkoss.zk.ui.util.Clients.evalJavaScript(
							"(function(){var e=document.getElementById('" + lblAutoKet.getUuid() + "');"
							+ "if(!e)return;e.style.transition='none';e.style.opacity='1';"
							+ "setTimeout(function(){e.style.transition='opacity 0.8s';e.style.opacity='0';},2500);"
							+ "})();"
						);
					} catch(Exception e) {
						if(tx != null) tx.rollback();
						lblAutoKet.setValue("✗");
						lblAutoKet.setStyle("color:red;font-size:13px;font-weight:bold;");
						lblAutoKet.setTooltiptext("Gagal simpan: " + e.getMessage());
					} finally {
						if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4403");}
					}
				}
			});

		}
	}

	/**
	 * Jumlah peserta yang menjadi <b>penyebut</b> seluruh statistik ujian di layar ini.
	 *
	 * <p>Diisi oleh {@link #loadData(Object)} dan maknanya BERBEDA menurut mode tampilan:</p>
	 * <ul>
	 *   <li><b>Mode satu mahasiswa</b> ({@link #mahasiswa} tidak null): diisi
	 *       {@code perkuliahan.ambilJumlahDetailperkuliahan()} — jumlah peserta kelas, bukan 1.</li>
	 *   <li><b>Mode satu calon mahasiswa</b> ({@link #biodataCalonMahasiswa} tidak null): jumlah
	 *       baris {@code HasilUjianMahasiswa} ber-{@code keyhasil} untuk ujian ini.</li>
	 *   <li><b>Mode PMB per gelombang</b>: jumlah calon mahasiswa terdaftar pada gelombang.</li>
	 *   <li><b>Mode pertemuan perkuliahan</b>: jumlah mahasiswa peserta pertemuan setelah
	 *       menyaring daftar {@code mhsYgTidakIkut}.</li>
	 *   <li><b>Mode lain</b>: ukuran daftar peserta hasil query gabungan.</li>
	 * </ul>
	 *
	 * <p>Field ini juga dibaca dari luar melalui {@code Ambildata} yang diserahkan ke
	 * {@link #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)}, agar kartu
	 * "Peserta Ujian" pada dashboard analisis butir soal memakai angka yang SAMA dengan
	 * "Jumlah Peserta" di tab Statistik. Karena dibaca saat tombol diklik (bukan saat tombol
	 * dibuat), nilainya sudah terisi hasil {@code loadData}.</p>
	 *
	 * <p><b>Perhatian pembagian nol:</b> {@link #displayStatistik(int, int, int)} membagi dengan
	 * field ini tanpa penjagaan. Bila bernilai 0 (mis. ujian tanpa peserta terdaftar), hasil bagi
	 * {@code double} menjadi {@code NaN}/{@code Infinity} dan tampil apa adanya pada label
	 * persentase — bukan exception, tetapi angka yang tidak bermakna.</p>
	 */
	private int jumlahPeserta = 0;

	/**
	 * Cache hasil ujian seluruh peserta untuk satu kali tampilan grid, berkunci
	 * <b>id peserta</b> ({@code VOMahasiswa.getId()} — bisa id Mahasiswa, BiodataCalonMahasiswa,
	 * Siswa, atau CalonSiswa tergantung mode).
	 *
	 * <p><b>Bentuk nilai:</b> {@code Object[2]} dengan
	 * {@code [0]} = entity {@link HasilUjianMahasiswa} peserta tersebut, dan
	 * {@code [1]} = {@code Set<Long>} berisi id {@code BankSoal} yang sudah terjawab
	 * (hasil {@code ambilBankSoalIdTerjawab}). Elemen {@code [1]} yang tidak kosong dipakai
	 * sebagai definisi "peserta sudah ikut ujian" baik di {@link #loadData(Object)} maupun di
	 * dashboard {@link #analsisButirSoal(PertemuanPunyaUjian, Ambildata, Ambildata)}.</p>
	 *
	 * <p><b>Konkurensi:</b> diisi ulang sebagai {@code java.util.concurrent.ConcurrentHashMap}
	 * pada setiap {@link #loadData(Object)} karena pengisiannya dilakukan oleh kolam sampai
	 * 100 thread. {@code ConcurrentHashMap} WAJIB di sini — {@code HashMap} biasa dapat rusak
	 * struktur internalnya bila di-{@code put} bersamaan. Konsekuensi lain yang perlu diingat:
	 * map ini TIDAK menerima kunci maupun nilai {@code null}.</p>
	 *
	 * <p><b>Pemakai:</b> renderer baris ({@code DetailPertemuanPunyaUjianRenderer.render}) membaca
	 * map ini untuk mengubah objek peserta menjadi hasil ujiannya; hampir seluruh tombol toolbar
	 * (Hitung Ulang Semua, Koreksi via AI, Download Lampiran, Lampiran ke Drive, Hasil OBE,
	 * Analisis Butir Soal) mengiterasi {@code values()}-nya. Karena itu semua fitur tersebut
	 * hanya bekerja atas peserta yang sedang termuat di grid — bukan atas seluruh isi database.</p>
	 *
	 * <p><b>Siklus hidup:</b> bernilai {@code null} sebelum {@link #loadData(Object)} pertama.
	 * Menekan tombol toolbar sebelum pemuatan selesai dapat menimbulkan
	 * {@link NullPointerException} yang tertangkap penanganan error masing-masing listener.</p>
	 */
	private Map<Long, Object[]> hasilUjianMahasiswas = null;

	/**
	 * Daftar objek peserta yang menjadi <b>model baris grid</b> pada satu kali pemuatan.
	 *
	 * <p>Bertipe {@code List<VOMahasiswa>} — {@code VOMahasiswa} adalah antarmuka bersama yang
	 * diimplementasikan {@code Mahasiswa}, {@code BiodataCalonMahasiswa}, {@code Siswa}, dan
	 * {@code CalonSiswa}, sehingga satu daftar dapat menampung keempat jenis peserta dan
	 * renderer cukup melakukan {@code instanceof} untuk membedakannya. Komentar "PERBAIKAN 1"
	 * pada kode menandai penggantian wildcard {@code List<? extends VOMahasiswa>} dengan tipe
	 * pasti ini agar {@code addAll(...)} dapat dipakai — dengan wildcard, penambahan elemen
	 * ditolak kompiler.</p>
	 *
	 * <p><b>Alur:</b> diisi {@link #loadData(Object)} dari berbagai Criteria sesuai mode, lalu
	 * dibungkus {@code SimpleListModel} dan dipasang ke {@link #grid}. Setelah model terpasang,
	 * sebuah timer ZK memanggil {@code clear()} atas daftar ini untuk melepas referensi entity
	 * agar tidak menahan memori sepanjang umur desktop — karena itu jangan mengandalkan isinya
	 * setelah pemuatan selesai; sumber kebenaran per peserta adalah
	 * {@link #hasilUjianMahasiswas}.</p>
	 *
	 * <p><b>Perhatian:</b> thread latar pengisi {@link #hasilUjianMahasiswas} mengiterasi daftar
	 * yang sama, sementara timer ZK dapat mengosongkannya. Iterasi memakai for-each atas
	 * {@code ArrayList} biasa, sehingga urutan kedua peristiwa itu menentukan apakah muncul
	 * {@code ConcurrentModificationException} — tertangkap {@code catch} terluar thread dan hanya
	 * berakibat sebagian baris tidak terisi (grid tampak kosong sebagian) tanpa merusak data.</p>
	 */
	private List<VOMahasiswa> mahasiswasTemorary = null;

	/**
	 * Implementasi {@link DataLoader#loadData(Object)} — memuat data hasil ujian dari
	 * database dan mengisi {@link #grid}. Dipanggil oleh framework {@code DataCriteria}
	 * saat inisialisasi awal dan setiap kali grid perlu di-refresh.
	 *
	 * <p><b>Tujuan:</b> Mengambil semua {@code HasilUjianMahasiswa} yang relevan dari database
	 * menggunakan Hibernate Criteria, lalu mengisi grid beserta statistik. Mendukung berbagai
	 * mode: rekap per-pertemuan (semua ujian dalam pertemuan), per-ujian (satu
	 * {@code PertemuanPunyaUjian}), per-mahasiswa, atau per-calon-mahasiswa. Juga mendukung
	 * filter pencarian nama peserta.</p>
	 *
	 * <p><b>Dua fase yang harus dibedakan.</b> Method ini TIDAK memuat hasil ujian secara
	 * sinkron. Ia hanya menyusun DAFTAR PESERTA lebih dulu, memasangnya sebagai model grid,
	 * lalu menyerahkan pengambilan hasil ujian per peserta kepada kolam thread di latar:</p>
	 * <ol>
	 *   <li><b>Fase 1 &mdash; daftar peserta (sinkron).</b> {@link #hasilUjianMahasiswas}
	 *       diinisialisasi ulang sebagai {@code ConcurrentHashMap} dan
	 *       {@link #mahasiswasTemorary} sebagai {@code ArrayList} kosong. Parameter filter
	 *       diekstrak sekali di awal ({@code jadwalUjianPMB}, {@code ujianPMB}, gelombang,
	 *       {@code pesertaUjianHarusTelahUjian}, {@code pesertaUjianHarusPunyaNomorUjian},
	 *       {@code ruanganYgIkut}, paket) agar rantai getter panjang tidak diulang di setiap
	 *       cabang. Tiga {@code Criterion} pencarian disiapkan di muka — versi beralias untuk
	 *       query yang menyertakan {@code createAlias}, versi langsung untuk query atas
	 *       {@code BiodataCalonMahasiswa} sendiri, dan versi mahasiswa — masing-masing menjadi
	 *       {@code sqlRestriction("true")} bila kotak pencarian kosong sehingga struktur query
	 *       tetap seragam tanpa percabangan tambahan.</li>
	 *   <li><b>Fase 2 &mdash; hasil ujian per peserta (asinkron).</b> Sebuah {@code Thread}
	 *       menyerahkan satu tugas per peserta ke {@code ExecutorService} berukuran
	 *       {@code DbThreadPool.safe(100)}. Tiap tugas memanggil
	 *       {@code HasilUjianMahasiswa.ambilByKey(...)} lalu menyimpan
	 *       {@code Object[]{hasilUjian, idSoalTerjawab}} ke {@link #hasilUjianMahasiswas}.
	 *       Bilah pemuatan diperbarui dari dalam tugas; setelah seluruh tugas selesai label
	 *       dikosongkan sehingga callback ZK berjalan dan grid dirender.</li>
	 * </ol>
	 *
	 * <p><b>Enam mode pemilihan peserta.</b> Percabangan di fase 1 menentukan siapa yang masuk
	 * {@link #mahasiswasTemorary}: (a) satu mahasiswa tertentu; (b) satu calon mahasiswa
	 * tertentu; (c) PMB dengan syarat "peserta harus telah ujian" &mdash; diambil dari
	 * {@code HasilUjianMahasiswa} yang {@code mulaiPada}-nya tidak null; (d) PMB dengan pembatasan
	 * ruangan &mdash; lewat {@code RuangPaketPMB} dan {@code sqlRestriction} atas kolom
	 * {@code ruang_pmb}; (e) PMB per gelombang &mdash; seluruh calon aktif pada gelombang, inilah
	 * satu-satunya cabang yang mengikutsertakan calon yang BELUM ujian; (f) pertemuan perkuliahan
	 * &mdash; {@code pertemuan.ambilMahasiswa()} disaring di memori terhadap kata kunci pencarian
	 * dan daftar {@code mhsYgTidakIkut}. Cabang terakhir sebagai cadangan menggabungkan peserta
	 * calon dan mahasiswa yang punya {@code keyhasil}.</p>
	 *
	 * <p><b>Efek samping {@code reloadNama}.</b> Bila kotak pencarian KOSONG, method menganggap
	 * ini pemuatan penuh dan MERESET berkas lokasi hasil ujian pada {@code PertemuanPunyaUjian}
	 * ({@code bersihkanLokasiHasilUjianMahasiswa()} lalu menulis JSON kosong), untuk kemudian
	 * diisi ulang oleh tiap tugas lewat {@code populateHasilUjianMahasiswa(...)}. Saat pencarian
	 * berisi teks, reset ini DILEWATI supaya hasil pencarian parsial tidak menghapus peta lokasi
	 * peserta lain. Konsekuensi yang perlu diingat: memuat ulang tanpa kata kunci akan menulis
	 * ulang berkas tersebut.</p>
	 *
	 * <p><b>Session Hibernate.</b> Setiap cabang query membuka session terdedikasi dari
	 * {@code SessionFactory} dan menutupnya di {@code finally} lewat
	 * {@link #closeSessionSafe(org.hibernate.Session)} (clear &rarr; disconnect &rarr; close).
	 * Di dalam tugas thread pool, {@code HibernateUtil.closeSession()} dipanggil DUA KALI —
	 * sekali di awal untuk membuang session {@code ThreadLocal} yang basi (thread pool dipakai
	 * ulang lintas request sehingga koneksi JDBC-nya bisa sudah ditutup c3p0 walau
	 * {@code isOpen()} masih true) dan sekali di {@code finally} agar tidak menggantung.</p>
	 *
	 * <p><b>Statistik.</b> Callback ZK menjumlahkan {@code terjawab} dari seluruh nilai map dan
	 * mencacah peserta yang himpunan jawabannya tidak kosong sebagai
	 * {@code pesertaYgIkutUjian}, lalu memanggil
	 * {@link #displayStatistik(int, int, int)} dengan {@link #jumlahPeserta} sebagai penyebut.</p>
	 *
	 * <p><b>Renderer.</b> Baris grid dirender oleh {@link DetailPertemuanPunyaUjianRenderer},
	 * yang memetakan objek peserta kembali ke {@link #hasilUjianMahasiswas} berdasarkan id.
	 * Peserta yang belum sempat terisi thread latar akan menghasilkan baris kosong (renderer
	 * langsung {@code return}), bukan error.</p>
	 *
	 * <p><b>Otorisasi dan cakupan data.</b> Method ini tidak menerapkan penyaringan satuan kerja
	 * maupun pemeriksaan peran. Pembatasan hanya berasal dari objek yang disuntikkan melalui
	 * konstruktor: bila {@link #mahasiswa} atau {@link #biodataCalonMahasiswa} diisi, hanya
	 * peserta itu yang dimuat; bila keduanya null, SELURUH peserta ujian dimuat. Dengan demikian
	 * pemilihan konstruktor yang tepat oleh Action pemanggil adalah satu-satunya gerbang cakupan
	 * data pada layar ini.</p>
	 *
	 * <p><b>Parameter {@code value}.</b> Diperlakukan sebagai {@code Boolean} {@code refresh} dan
	 * diteruskan ke {@code ambilBankSoalIdTerjawab(..., refresh)}: {@code true} memaksa pembacaan
	 * ulang himpunan jawaban dari sumbernya (menembus cache), {@code null}/{@code false}
	 * mengizinkan pemakaian cache. Nilai selain {@code Boolean} akan menimbulkan
	 * {@link ClassCastException} pada baris konversi di awal method.</p>
	 *
	 * @param value {@code Boolean} penanda refresh; {@code true} = paksa baca ulang jawaban,
	 *              {@code null}/{@code false} = boleh memakai cache
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {
	    // Menggunakan ConcurrentHashMap agar aman saat diproses oleh banyak thread
	    hasilUjianMahasiswas = new java.util.concurrent.ConcurrentHashMap<Long, Object[]>();
	    mahasiswasTemorary = new ArrayList<VOMahasiswa>();

	    final Boolean refresh = (Boolean) (value != null ? value : false);
	    final String searchValue = (nama != null && nama.getValue() != null) ? nama.getValue().trim() : "";
	    final boolean isSearchEmpty = searchValue.isEmpty();

	    // Ekstraksi objek untuk efisiensi memori
	    Object pertemuanObj = pertemuanPunyaUjian != null ? pertemuanPunyaUjian.getPertemuan() : null;
	    Object jadwalPMBObj = null;
	    Object ujianPMBObj = null;
	    Object gelombangObj = null;
	    Boolean isHarusTelahUjian = false;
	    Boolean isHarusPunyaNoUjian = false;
	    String ruanganYgIkut = "";
	    Object paketPMBObj = null;

	    if (pertemuanObj != null) {
	        jadwalPMBObj = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB();
	        if (jadwalPMBObj != null) {
	            ujianPMBObj = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB().getUjianPMB();
	            isHarusTelahUjian = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB().getPesertaUjianHarusTelahUjian();
	            isHarusPunyaNoUjian = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB().getPesertaUjianHarusPunyaNomorUjian();
	            ruanganYgIkut = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB().getRuanganYgIkut();
	            paketPMBObj = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB().getPaket();
	            
	            if (ujianPMBObj != null) {
	                gelombangObj = pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran();
	            }
	        }
	    }

	    org.hibernate.criterion.Criterion searchCriteriaCalonMhsAlias = isSearchEmpty ? 
	        org.hibernate.criterion.Restrictions.sqlRestriction("true") :
	        org.hibernate.criterion.Restrictions.or(
	            org.hibernate.criterion.Restrictions.ilike("biodataCalonMahasiswa.nama", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE),
	            org.hibernate.criterion.Restrictions.or(
	                org.hibernate.criterion.Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE),
	                org.hibernate.criterion.Restrictions.ilike("biodataCalonMahasiswa.noUjian", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE)
	            )
	        );

	    org.hibernate.criterion.Criterion searchCriteriaCalonMhsDirect = isSearchEmpty ? 
	        org.hibernate.criterion.Restrictions.sqlRestriction("true") :
	        org.hibernate.criterion.Restrictions.or(
	            org.hibernate.criterion.Restrictions.ilike("nama", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE),
	            org.hibernate.criterion.Restrictions.or(
	                org.hibernate.criterion.Restrictions.ilike("noRegistrasi", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE),
	                org.hibernate.criterion.Restrictions.ilike("noUjian", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE)
	            )
	        );

	    org.hibernate.criterion.Criterion searchCriteriaMhsAlias = isSearchEmpty ? 
	        org.hibernate.criterion.Restrictions.sqlRestriction("true") :
	        org.hibernate.criterion.Restrictions.or(
	            org.hibernate.criterion.Restrictions.ilike("mahasiswa.nama", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE),
	            org.hibernate.criterion.Restrictions.ilike("mahasiswa.nim", searchValue, org.hibernate.criterion.MatchMode.ANYWHERE)
	        );

	    if (mahasiswa != null) {
	        mahasiswasTemorary.add(mahasiswa);
	        jumlahPeserta = pertemuanPunyaUjian.getPertemuan().getPerkuliahan().ambilJumlahDetailperkuliahan();
	    } else if (biodataCalonMahasiswa != null) {
	        mahasiswasTemorary.add(biodataCalonMahasiswa);
	        org.hibernate.Session session = null;
	        try {
	            session = HibernateUtil.getSessionFactory().openSession();
	            jumlahPeserta = ((Number) session.createCriteria(HasilUjianMahasiswa.class)
	                    .add(org.hibernate.criterion.Restrictions.isNotNull("keyhasil"))
	                    .setProjection(org.hibernate.criterion.Projections.rowCount())
	                    .add(org.hibernate.criterion.Restrictions.isNotNull("biodataCalonMahasiswa"))
	                    .add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
	                    .uniqueResult()).intValue();
	        } finally {
	            closeSessionSafe(session);
	        }
	    } else {
	        if (pertemuanObj != null && jadwalPMBObj != null) {
	            org.hibernate.Session session = null;
	            try {
	                session = HibernateUtil.getSessionFactory().openSession();

	                if (ujianPMBObj != null && gelombangObj != null && isHarusTelahUjian != null && isHarusTelahUjian) {
	                    // PERBAIKAN 2: Gunakan .addAll() untuk menyisipkan seluruh isi List dari hasil query
	                    mahasiswasTemorary.addAll(ConstantValues.simpleList(
	                            session.createCriteria(HasilUjianMahasiswa.class)
	                                    .add(org.hibernate.criterion.Restrictions.isNotNull("mulaiPada"))
	                                    .add(org.hibernate.criterion.Restrictions.isNotNull("keyhasil"))
	                                    .setProjection(org.hibernate.criterion.Projections.groupProperty("biodataCalonMahasiswa.id"))
	                                    .add(org.hibernate.criterion.Restrictions.isNotNull("biodataCalonMahasiswa"))
	                                    .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
	                                    .add(searchCriteriaCalonMhsAlias)
	                                    .add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
	                            BiodataCalonMahasiswa.class, false));

	                } else if (ujianPMBObj != null && gelombangObj != null && ruanganYgIkut != null && !ruanganYgIkut.isEmpty()) {
	                    mahasiswasTemorary.addAll(ConstantValues.simpleList(session.createCriteria(RuangPaketPMB.class)
	                            .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
	                            .add(isHarusPunyaNoUjian ? 
	                                    org.hibernate.criterion.Restrictions.and(org.hibernate.criterion.Restrictions.ne("biodataCalonMahasiswa.noUjian", ""), org.hibernate.criterion.Restrictions.isNotNull("biodataCalonMahasiswa.noUjian")) 
	                                    : org.hibernate.criterion.Restrictions.sqlRestriction("true"))
	                            .add(searchCriteriaCalonMhsAlias)
	                            .setProjection(org.hibernate.criterion.Projections.property("biodataCalonMahasiswa.id"))
	                            .add(org.hibernate.criterion.Restrictions.sqlRestriction("ruang_pmb in (-1" + ruanganYgIkut + "-1)")),
	                            BiodataCalonMahasiswa.class, false));

	                } else if (ujianPMBObj != null && gelombangObj != null) {
	                    mahasiswasTemorary.addAll(ConstantValues.simpleList(
	                            session.createCriteria(BiodataCalonMahasiswa.class)
	                                    .add(org.hibernate.criterion.Restrictions.or(org.hibernate.criterion.Restrictions.isNull("aktif"), org.hibernate.criterion.Restrictions.eq("aktif", true)))
	                                    .add(isHarusPunyaNoUjian ? 
	                                            org.hibernate.criterion.Restrictions.and(org.hibernate.criterion.Restrictions.ne("noUjian", ""), org.hibernate.criterion.Restrictions.isNotNull("noUjian")) 
	                                            : org.hibernate.criterion.Restrictions.sqlRestriction("true"))
	                                    .add(searchCriteriaCalonMhsDirect)
	                                    .add(paketPMBObj == null ? org.hibernate.criterion.Restrictions.sqlRestriction("true") : org.hibernate.criterion.Restrictions.eq("paket", paketPMBObj))
	                                    .add(org.hibernate.criterion.Restrictions.eq("gelombangPendaftaran", gelombangObj))
	                                    .addOrder(org.hibernate.criterion.Order.asc("noRegistrasi")),
	                            BiodataCalonMahasiswa.class));

	                    jumlahPeserta = mahasiswasTemorary.size();

	                } else {
	                    mahasiswasTemorary.addAll(ConstantValues.simpleList(
	                            session.createCriteria(HasilUjianMahasiswa.class)
	                                    .add(org.hibernate.criterion.Restrictions.isNotNull("keyhasil"))
	                                    .setProjection(org.hibernate.criterion.Projections.groupProperty("biodataCalonMahasiswa.id"))
	                                    .add(org.hibernate.criterion.Restrictions.isNotNull("biodataCalonMahasiswa"))
	                                    .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
	                                    .add(searchCriteriaCalonMhsAlias)
	                                    .add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
	                            BiodataCalonMahasiswa.class, false));
	                }
	            } finally {
	                closeSessionSafe(session);
	            }

	        } else if (pertemuanObj != null) {
	            List<Mahasiswa> temp = pertemuanPunyaUjian.getPertemuan().ambilMahasiswa();
	            List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
	            String searchLower = searchValue.toLowerCase();
	            
	            for (Mahasiswa maha : temp) {
	                if (isSearchEmpty || 
	                   (maha.getNama() != null && maha.getNama().toLowerCase().contains(searchLower)) || 
	                   (maha.getNim() != null && maha.getNim().toLowerCase().contains(searchLower))) {
	                    
	                    Long id = maha.getId();
	                    if (pertemuanPunyaUjian.getMhsYgTidakIkut() == null || !pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ",")) {
	                        mahasiswas.add(maha);
	                    }
	                }
	            }
	            temp = null;
	            // PERBAIKAN 3: Memasukkan objek Mahasiswa ke dalam List Induk <VOMahasiswa>
	            mahasiswasTemorary.addAll(mahasiswas);
	            jumlahPeserta = mahasiswasTemorary.size();

	        } else {
	            org.hibernate.Session session = null;
	            try {
	                session = HibernateUtil.getSessionFactory().openSession();
	                
	                mahasiswasTemorary.addAll(ConstantValues.simpleList(
	                        session.createCriteria(HasilUjianMahasiswa.class)
	                                .add(org.hibernate.criterion.Restrictions.isNotNull("keyhasil"))
	                                .setProjection(org.hibernate.criterion.Projections.groupProperty("biodataCalonMahasiswa.id"))
	                                .add(org.hibernate.criterion.Restrictions.isNotNull("biodataCalonMahasiswa"))
	                                .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
	                                .add(searchCriteriaCalonMhsAlias)
	                                .add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
	                        BiodataCalonMahasiswa.class, false));

	                List<Mahasiswa> mhs = ConstantValues.simpleList(
	                        session.createCriteria(HasilUjianMahasiswa.class)
	                                .add(org.hibernate.criterion.Restrictions.isNotNull("keyhasil"))
	                                .setProjection(org.hibernate.criterion.Projections.groupProperty("mahasiswa.id"))
	                                .add(org.hibernate.criterion.Restrictions.isNotNull("mahasiswa"))
	                                .createAlias("mahasiswa", "mahasiswa")
	                                .add(searchCriteriaMhsAlias)
	                                .add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
	                        Mahasiswa.class, false);

	                if (mhs != null) {
	                    mahasiswasTemorary.addAll(mhs);
	                }
	                jumlahPeserta = mahasiswasTemorary.size();
	                
	            } finally {
	                closeSessionSafe(session);
	            }
	        }
	    }

	    final org.zkoss.zul.Label label = Common.displayLoadBar(new org.zkoss.zk.ui.event.EventListener() {
	        /**
	         * Callback bilah pemuatan {@code loadData}: memasang model grid, menghitung
	         * statistik, dan membersihkan daftar sementara — dijalankan pada thread ZK setelah
	         * seluruh tugas thread pool selesai mengisi
	         * {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas}.
	         *
	         * <p><b>Urutan yang penting.</b> {@code SimpleListModel} dibungkus dari
	         * {@link HasilUjianMahasiswaHelper#mahasiswasTemorary}, renderer dipasang, lalu
	         * {@code setModelCheckMobile(strset)} memicu perenderan baris. Renderer memerlukan
	         * map hasil ujian sudah terisi — itulah sebabnya callback ini baru berjalan setelah
	         * label dikosongkan thread koordinator.</p>
	         *
	         * <p><b>Statistik.</b> Menjumlahkan soal terjawab seluruh peserta dan mencacah
	         * peserta yang himpunan jawabannya TIDAK KOSONG sebagai {@code pesertaYgIkutUjian}.
	         * Definisi "sudah ikut ujian" ini — ada minimal satu soal terjawab — sama persis
	         * dengan yang dipakai dashboard Analisis Butir Soal, sehingga kedua layar konsisten.
	         * Hasilnya diserahkan ke {@link HasilUjianMahasiswaHelper#displayStatistik(int, int, int)}
	         * dengan {@link HasilUjianMahasiswaHelper#jumlahPeserta} sebagai penyebut.</p>
	         *
	         * <p><b>Pembersihan tertunda.</b> Timer ZK mengosongkan {@code mahasiswasTemorary}
	         * setelah model terpasang, untuk melepas referensi entity agar tidak menahan memori
	         * sepanjang umur desktop.</p>
	         *
	         * @param arg0 event penanda selesai; tidak dipakai
	         * @throws Exception diteruskan dari pemasangan model dan pembangunan panel statistik
	         */
	        @Override
	        public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
	            org.zkoss.zul.ListModel strset = new org.zkoss.zul.SimpleListModel(mahasiswasTemorary);
	            grid.setRowRenderer(new DetailPertemuanPunyaUjianRenderer());
	            grid.setModelCheckMobile(strset);

	            int terjawab = 0;
	            int pesertaYgIkutUjian = 0;
	            
	            for (Object[] obj : hasilUjianMahasiswas.values()) {
	                if (obj != null && obj.length > 1) {
	                    Set<Long> terjwb = (Set<Long>) obj[1];
	                    int jumlhaTerjawab = terjwb == null ? 0 : terjwb.size();
	                    terjawab += jumlhaTerjawab;
	                    if (jumlhaTerjawab > 0) {
	                        pesertaYgIkutUjian++;
	                    }
	                }
	            }

	            displayStatistik(jumlahPeserta, terjawab, pesertaYgIkutUjian);

	            Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
	                /**
	                 * Melepas isi {@link HasilUjianMahasiswaHelper#mahasiswasTemorary} setelah
	                 * model grid terpasang, agar referensi entity peserta tidak menahan memori
	                 * sepanjang umur desktop ZK.
	                 *
	                 * <p>Dijalankan lewat timer — bukan langsung — karena {@code SimpleListModel}
	                 * yang baru saja dipasang masih membaca daftar ini selama render awal.
	                 * Mengosongkannya seketika akan menghasilkan grid kosong.</p>
	                 *
	                 * <p><b>Perhatian konkurensi:</b> thread latar pengisi
	                 * {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas} mengiterasi daftar
	                 * yang sama. Bila pembersihan ini terjadi selagi iterasi berlangsung,
	                 * {@link java.util.ConcurrentModificationException} dapat terjadi — tertangkap
	                 * {@code catch} terluar thread tersebut, dengan akibat sebagian baris tidak
	                 * terisi (grid tampak kosong sebagian) tanpa merusak data. Menekan Refresh
	                 * memulihkannya.</p>
	                 *
	                 * @param arg0 event timer; tidak dipakai
	                 * @throws Exception tidak dilempar dalam praktik
	                 */
	                @Override
	                public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
	                    if (mahasiswasTemorary != null) {
	                        mahasiswasTemorary.clear();
	                    }
	                }
	            });
	        }
	    });

	    final boolean reloadNama = isSearchEmpty;
	    if (reloadNama) {
	        pertemuanPunyaUjian.bersihkanLokasiHasilUjianMahasiswa();
	        pertemuanPunyaUjian.tulisLokasiHasilUjianMahasiswa(new org.json.JSONObject().toString());
	    }

	    // Threading dan Background Processing
	    new Thread(new Runnable() {
	        /**
	         * Thread <b>koordinator</b> fase kedua {@code loadData}: menyebar satu tugas per
	         * peserta ke kolam thread, menunggu seluruhnya selesai, lalu mengosongkan label bilah
	         * pemuatan sehingga callback ZK memasang model grid.
	         *
	         * <p><b>Ukuran kolam.</b> {@code Executors.newFixedThreadPool(DbThreadPool.safe(100))}
	         * — dua kali lipat kolam "Hitung Ulang Semua" (50), karena tugas di sini hanya
	         * MEMBACA sehingga lebih ringan dan tidak menahan kunci tulis.
	         * {@code DbThreadPool.safe} membatasi angka itu terhadap kapasitas kolam koneksi
	         * database agar tidak terjadi kelaparan koneksi.</p>
	         *
	         * <p><b>Jalan pintas.</b> Bila tidak ada peserta sama sekali, label langsung
	         * dikosongkan dan thread berakhir — tanpa membuat kolam thread.</p>
	         *
	         * <p><b>Lewati yang sudah ada.</b> Peserta yang null atau yang id-nya sudah terdapat
	         * pada {@link HasilUjianMahasiswaHelper#hasilUjianMahasiswas} dilewati, namun pencacah
	         * progres tetap dinaikkan agar persentase tidak macet.</p>
	         *
	         * <p><b>Penyelesaian.</b> {@code shutdown()} lalu
	         * {@code awaitTermination(Long.MAX_VALUE, NANOSECONDS)} — menunggu tanpa batas waktu.
	         * Pengosongan label berada di blok {@code finally}, sehingga bilah pemuatan SELALU
	         * hilang meski terjadi kegagalan; ini berbeda dari thread Analisis Butir Soal yang
	         * mengosongkan label di dalam {@code try} dan karenanya dapat menggantung.</p>
	         */
	        @Override
	        public void run() {
	            try {
	                final int size = mahasiswasTemorary.size();
	                if (size == 0) {
	                    if (label != null) label.setValue("");
	                    return;
	                }

	                final java.util.concurrent.atomic.AtomicInteger processedCounter = new java.util.concurrent.atomic.AtomicInteger(0);
	                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(100));

	                for (final VOMahasiswa voMahasiswa : mahasiswasTemorary) {
	                    if (voMahasiswa == null || hasilUjianMahasiswas.containsKey(voMahasiswa.getId())) {
	                        processedCounter.incrementAndGet();
	                        continue;
	                    }

	                    executor.submit(new Runnable() {
	                        /**
	                         * Tugas pemuatan hasil ujian untuk <b>satu peserta</b>, dijalankan di
	                         * kolam thread.
	                         *
	                         * <p><b>Membuang session ThreadLocal basi lebih dulu.</b> Baris
	                         * pertama memanggil {@code HibernateUtil.closeSession()}. Ini
	                         * perbaikan atas galat "You can't operate on a closed Connection":
	                         * kolam thread ({@code DbThreadPool}) DIPAKAI ULANG lintas
	                         * request/operasi, sehingga session native {@code ThreadLocal} milik
	                         * thread ini bisa basi — {@code isOpen()} masih {@code true} padahal
	                         * koneksi JDBC-nya sudah ditutup c3p0 atau basis data. Membuangnya di
	                         * muka memaksa method entity ({@code ambilByKey},
	                         * {@code ambilBankSoalIdTerjawab}) membuka session native yang SEGAR.
	                         * Panggilan kedua di {@code finally} menutup session yang dibuka tugas
	                         * ini agar tidak menggantung untuk pemakai kolam berikutnya.</p>
	                         *
	                         * <p><b>Alur.</b> Peserta dipilah menjadi {@code Mahasiswa} atau
	                         * {@code BiodataCalonMahasiswa} lewat {@code instanceof} (jenis lain
	                         * diteruskan sebagai dua-duanya null), lalu
	                         * {@code HasilUjianMahasiswa.ambilByKey(...)} mengambil baris hasilnya.
	                         * Bila {@code reloadNama} aktif — yaitu pemuatan penuh tanpa kata kunci
	                         * pencarian — {@code populateHasilUjianMahasiswa(...)} menulis ulang
	                         * peta lokasi peserta pada {@code PertemuanPunyaUjian}. Terakhir,
	                         * pasangan {@code Object[]{hasilUjian, idSoalTerjawab}} disimpan ke
	                         * map berkunci id peserta; argumen {@code refresh} menentukan apakah
	                         * himpunan jawaban dibaca ulang atau boleh dari cache.</p>
	                         *
	                         * <p><b>Ketahanan.</b> Kegagalan per peserta dicatat ke
	                         * {@code ErrorAuditUtil} dan tidak menghentikan peserta lain — baris
	                         * yang bersangkutan akan tampil kosong di grid. Pembaruan label progres
	                         * dibungkus {@code try/catch} tersendiri agar galat UI di luar Desktop
	                         * tidak mematikan tugas. Pencacah {@code processedCounter} bertipe
	                         * {@link java.util.concurrent.atomic.AtomicInteger} karena dinaikkan
	                         * sampai 100 thread.</p>
	                         */
	                        @Override
	                        public void run() {
	                            try {
	                                // FIX "You can't operate on a closed Connection": thread pool (DbThreadPool) DIPAKAI ULANG
	                                // lintas request/operasi -> sesi native ThreadLocal thread ini bisa BASI (isOpen() true tapi
	                                // koneksi JDBC sudah ditutup c3p0/DB). Buang dulu -> entity method (ambilByKey/
	                                // ambilBankSoalIdTerjawab) membuka sesi native SEGAR.
	                                try { HibernateUtil.closeSession(); } catch (Exception ignoreClose) { ais.common.ErrorAuditUtil.record(ignoreClose, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4717");}
	                                Mahasiswa voMhs = (voMahasiswa instanceof Mahasiswa) ? (Mahasiswa) voMahasiswa : null;
	                                BiodataCalonMahasiswa voCal = (voMahasiswa instanceof BiodataCalonMahasiswa) ? (BiodataCalonMahasiswa) voMahasiswa : null;

	                                HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(
	                                        pertemuanPunyaUjian, voMhs, voCal, null, null);

	                                if (reloadNama && hasilUjianMahasiswa != null) {
	                                    pertemuanPunyaUjian.populateHasilUjianMahasiswa(hasilUjianMahasiswa, true);
	                                }

	                                if (hasilUjianMahasiswa != null) {
	                                    MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
	                                            hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
	                                            new org.zkoss.zul.Label(), true);

	                                    hasilUjianMahasiswas.put(voMahasiswa.getId(), new Object[]{
	                                            hasilUjianMahasiswa,
	                                            hasilUjianMahasiswa.ambilBankSoalIdTerjawab(
	                                                    pertemuanPunyaUjian.getJmlDitampilkan(),
	                                                    ujianPunyaSoals, refresh)
	                                    });
	                                }
	                            } catch (Exception e) {
	                                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4741");
	                            } finally {
	                                // Tutup sesi native yang dibuka task ini agar tidak menggantung/basi di thread pool.
	                                try { HibernateUtil.closeSession(); } catch (Exception ignoreClose) { ais.common.ErrorAuditUtil.record(ignoreClose, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4744");}
	                                int currentIdx = processedCounter.incrementAndGet();
	                                double percentage = (currentIdx * 100.0) / size;
	                                if (label != null && voMahasiswa.getNama() != null) {
	                                    try {
	                                        label.setValue("Sedang memproses data " + voMahasiswa.getNama() + " ("
	                                                + Common.numberFormat.get().format(percentage) + " %)");
	                                    } catch (Exception uiEx) { ais.common.ErrorAuditUtil.record(uiEx, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4751");
	                                        // Mencegah background thread mati secara tidak sengaja karena exception UI di luar Desktop
	                                    }
	                                }
	                            }
	                        }
	                    });
	                }

	                executor.shutdown();
	                try {
	                    executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
	                } catch (InterruptedException ie) {
	                    ie.printStackTrace(); ais.common.ErrorAuditUtil.record(ie, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4764");
	                }

	            } catch (Exception e) {
	                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4768");
	            } finally {
	                if (label != null) {
	                    try {
	                        label.setValue("");
	                    } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4773");}
	                }
	            }
	        }
	    }).start();
	}

	/**
	 * Menutup session Hibernate dengan aman: clear → disconnect → close, masing-masing dalam
	 * blok try-catch terpisah sehingga kegagalan satu langkah tidak mencegah langkah berikutnya.
	 *
	 * <p><b>Tujuan:</b> Menghindari kebocoran koneksi database akibat session yang tidak
	 * tertutup dengan benar. Pola tiga-langkah (clear/disconnect/close) mengikuti best practice
	 * Hibernate: clear menghapus first-level cache, disconnect melepas koneksi JDBC (mengembalikan
	 * ke pool), close menutup session object sepenuhnya.</p>
	 *
	 * <p><b>Null-safe:</b> Tidak ada aksi bila {@code session} null.</p>
	 *
	 * @param session session Hibernate yang akan ditutup; null-safe
	 */
	private void closeSessionSafe(org.hibernate.Session session) {
	    if (session != null) {
	        try {
	            session.clear();
	        } catch (Exception ex) {
	            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4802");
	        }
	        try {
	            session.disconnect();
	        } catch (Exception ex) {
	            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4807");
	        }
	        try {
	            if (session.isOpen()) {
	                session.close();
	            }
	        } catch (Exception ex) {
	            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4814");
	        }
	    }
	}

	/**
	 * Menampilkan konfirmasi dan, bila dikonfirmasi, secara massal menandai semua peserta
	 * yang mengikuti ujian ini sebagai "hadir" di data presensi pertemuan.
	 *
	 * <p><b>Tujuan:</b> Fitur ini diperuntukkan bagi ujian yang menggantikan pertemuan biasa —
	 * peserta yang mengikuti ujian otomatis dianggap hadir tanpa perlu presensi manual.
	 * Menghemat waktu dosen untuk ujian dengan banyak peserta.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi "Apakah yakin..." menggunakan
	 *       {@code MyMessageboxConfig.show()}.</li>
	 *   <li>Bila dikonfirmasi (OK), membuat timer ZK untuk menjalankan proses di thread
	 *       selanjutnya (menghindari blokir ZK event queue).</li>
	 *   <li>Di dalam timer: membuka native session Hibernate, mengambil semua
	 *       {@code HasilUjianMahasiswa} yang memiliki {@code telahIkutUjian=true} untuk
	 *       ujian ini.</li>
	 *   <li>Untuk setiap peserta, mengambil atau membuat {@code Statusabsensi} "hadir"
	 *       di pertemuan terkait dan menyimpannya dalam transaksi yang sama.</li>
	 *   <li>Setelah selesai, memanggil {@code eventListener.onEvent()} untuk memperbarui
	 *       tampilan presensi di UI pemanggil.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Transaction di-rollback bila exception terjadi. Session
	 * selalu ditutup di finally.</p>
	 *
	 * @param pertemuanPunyaUjian ujian yang pesertanya akan ditandai hadir
	 * @param eventListener       callback yang dipanggil setelah proses selesai untuk refresh UI
	 * @throws Exception bila ZK event atau konstruksi dialog gagal
	 */
	public static void ujianDianggapHadir(final PertemuanPunyaUjian pertemuanPunyaUjian,
			final EventListener eventListener) throws Exception {

		MyMessageboxConfig.show(Common.pesan(
				"Apakah Bapak/Ibu yakin seluruh mahasiswa yang mengikuti \"{V1}\" akan dianggap hadir pada kelas ini? Tindakan ini akan menandai kehadiran seluruh peserta ujian. Silakan pilih OK untuk melanjutkan atau Batal untuk membatalkan.",
				pertemuanPunyaUjian.getUjian().getNama()),
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					/**
					 * Menerima jawaban dialog konfirmasi "Peserta dianggap hadir" dan meneruskan
					 * bila pengguna memilih OK.
					 *
					 * <p>Pelaksanaannya dibungkus {@code Common.createDefaultTimer(...)} agar
					 * penulisan presensi seluruh peserta — yang dapat memakan waktu pada kelas
					 * besar — berjalan di siklus event berikutnya dan tidak memblokir antrean
					 * event ZK selagi dialog ditutup.</p>
					 *
					 * @param event event dialog; {@code getData()} berisi kode tombol yang ditekan
					 * @throws Exception diteruskan dari penguraian kode tombol
					 */
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								/**
								 * Menuliskan presensi HADIR bagi seluruh peserta yang benar-benar
								 * mengerjakan ujian, dalam SATU transaksi.
								 *
								 * <p><b>Syarat seorang peserta ditandai hadir</b> — ketiganya
								 * harus terpenuhi: memiliki {@code mahasiswa} ATAU
								 * {@code biodataCalonMahasiswa}; {@code mulaiPada} tidak null; dan
								 * {@code selesaiPada} tidak null. Artinya peserta yang membuka
								 * ujian tetapi tidak pernah menyelesaikannya TIDAK dianggap hadir.
								 * Perhatikan bahwa peserta berjenis {@code Siswa}/{@code CalonSiswa}
								 * tidak pernah lolos syarat pertama sehingga fitur ini efektif
								 * hanya untuk domain perguruan tinggi.</p>
								 *
								 * <p><b>Isi catatan presensi.</b> {@code pertemuan.populate(...)}
								 * dipanggil dengan status {@code ConstantValues.MASUK} dan
								 * keterangan yang menyebut nama ujian, waktu mulai dan selesai
								 * pengerjaan, jumlah soal, serta jumlah soal terjawab — jejak yang
								 * memadai untuk audit kehadiran di kemudian hari. Jam mulai dan
								 * selesai presensi diambil dari catatan absensi peserta bila ada,
								 * dengan cadangan jam pertemuan.</p>
								 *
								 * <p><b>Entity pertemuan diambil ulang</b> lewat
								 * {@code session.get(Pertemuan.class, id)} agar yang dimodifikasi
								 * adalah instance TERKELOLA milik session ini, bukan objek
								 * detached dari layar pemanggil. Satu {@code session.update} di
								 * akhir menyimpan seluruh perubahan presensi sekaligus.</p>
								 *
								 * <p><b>Transaksi tunggal</b> berarti kegagalan di tengah proses
								 * me-rollback SEMUA presensi — tidak ada keadaan setengah
								 * tertulis. Session ditutup di {@code finally}, lalu
								 * {@code eventListener} pemanggil dijalankan lewat timer untuk
								 * menyegarkan tampilan.</p>
								 *
								 * <p><b>Otorisasi.</b> Tidak ada pemeriksaan peran di dalam
								 * listener; gerbangnya hanya {@code setVisible(...)} pada tombol
								 * pemicu di toolbar.</p>
								 *
								 * @param arg0 event timer; tidak dipakai
								 * @throws Exception diteruskan dari operasi basis data dan presensi
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {

									Session session = null;
									Transaction tx = null;
									try {
										session = HibernateUtil.getSessionFactory().openSession();
										tx = session.beginTransaction();
										
										Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
										if (pertemuan != null && pertemuan.getId() != null) {
											pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuan.getId());
										}

										List<Long> hasilUjianMahasiswas = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(true);
										System.out.println("hasilUjianMahasiswas -> " + hasilUjianMahasiswas);
										
										for (Long hasilUjianMahasiswaid : hasilUjianMahasiswas) {

											HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
													.ambilData(HasilUjianMahasiswa.class, hasilUjianMahasiswaid.toString());
											System.out.println("hasilUjianMahasiswa -> " + hasilUjianMahasiswa);
											
											if (hasilUjianMahasiswa != null) {
												if ((hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
														|| hasilUjianMahasiswa.getMahasiswa() != null)
														&& hasilUjianMahasiswa.getMulaiPada() != null
														&& hasilUjianMahasiswa.getSelesaiPada() != null) {
													
													Long mhs = hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
															? hasilUjianMahasiswa.getBiodataCalonMahasiswa().getId()
															: hasilUjianMahasiswa.getMahasiswa().getId();
													Statusabsensi statusabsensi = ConstantValues.MASUK;

													MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
															.ambilUjianPunyaSoals(hasilUjianMahasiswa
																	.getPertemuanPunyaUjian().getJmlDitampilkan(),
																	new Label(), true);

													Set<Long> idsa = hasilUjianMahasiswa.ambilBankSoalIdTerjawab(
															pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

													int terjawab = idsa.size();

													String mulai = pertemuan.retreiveAbsensiMulai(mhs);
													String sampai = pertemuan.retreiveAbsensiSampai(mhs);
													if (mulai == null || mulai.trim().isEmpty()) {
														mulai = pertemuan.getWaktuMulai();
													}
													if (sampai == null || sampai.trim().isEmpty()) {
														sampai = pertemuan.getWaktuSelesai();
													}

													System.out.println("terjawab -> " + terjawab + ", mulai " + mulai
															+ ", sampai " + sampai);

													pertemuan.populate(mhs, statusabsensi, "Mengikuti ujian \""
															+ pertemuanPunyaUjian.getUjian().getNama() + "\" pada "
															+ Common.dateFormat5.get().format(hasilUjianMahasiswa.getMulaiPada())
															+ " sampai dengan "
															+ Common.dateFormat5.get()
																	.format(hasilUjianMahasiswa.getSelesaiPada())
															+ " dengan jumlah soal "
															+ Common.numberFormat.get()
																	.format(hasilUjianMahasiswa.getJumlahSoal())
															+ " dan telah terjawab " + Common.numberFormat.get().format(terjawab),
															null, mulai, sampai, "Mahasiswa");
												}
											}
										}

										session.update(pertemuan);
										tx.commit();
									} catch(Exception e) {
										if(tx != null) tx.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4939");
									} finally {
										if(session != null && session.isOpen()) try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianMahasiswaHelper.java:4941");}
									}

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				});

	}

	/**
	 * Membangun HTML visual lengkap untuk laporan Analisis Butir Soal, mencakup kartu ringkasan
	 * statistik, diagram donut kualitas soal dan tingkat kesukaran, serta tabel detail per soal.
	 *
	 * <p><b>Tujuan:</b> Mengubah data mentah hasil analisis butir soal (array String[] per soal
	 * dan statistik agregat) menjadi tampilan HTML yang informatif dan visual. Output ditampilkan
	 * di panel HTML dalam jendela modal setelah thread latar selesai menghitung.</p>
	 *
	 * <p><b>Struktur output HTML:</b></p>
	 * <ul>
	 *   <li><b>Kartu ringkasan:</b> Peserta, Jumlah Soal, Rata-rata Nilai, Soal Layak Pakai
	 *       (via {@link #appendStatCard}).</li>
	 *   <li><b>Diagram donut Kualitas Soal (DP):</b> distribusi Gunakan/Revisi/Ganti
	 *       (via {@link #buildMultiDonutHtml}).</li>
	 *   <li><b>Diagram donut Tingkat Kesukaran (TK):</b> distribusi Mudah/Sedang/Sulit.</li>
	 *   <li><b>Tabel detail:</b> Satu baris per soal dengan kolom No, Soal, Kunci, Benar,
	 *       Salah, Kosong, TK (nilai+kategori), DP (nilai+kategori), dan distribusi pilihan
	 *       (chart micro inline).</li>
	 * </ul>
	 *
	 * @param rows   list hasil analisis per soal; setiap String[] = [0]nomorSoal [1]teks [2]kunci
	 *               [3]benar [4]salah [5]kosong [6]tkVal [7]katTK [8]dpVal [9]katDP [10]distribHtml
	 * @param stats  array statistik agregat: [0]peserta [1]totalSoal [2]gunakan [3]revisi
	 *               [4]ganti [5]mudah [6]sedang [7]sulit
	 * @param nilaiG array satu elemen: [0] = rata-rata nilai peserta
	 * @return HTML string siap dimasukkan ke {@code org.zkoss.zul.Html}
	 */
	private static String buildAnalisisVisualHtml(java.util.List<String[]> rows, int[] stats, double[] nilaiG) {
		// stats: [0]=peserta [1]=totalSoal [2]=gunakan [3]=revisi [4]=ganti [5]=mudah [6]=sedang [7]=sulit [8]=ikutUjian
		int peserta = stats[0], totalSoal = stats[1];
		int gunakan = stats[2], revisi = stats[3], ganti = stats[4];
		int mudah = stats[5], sedang = stats[6], sulit = stats[7];
		int ikutUjian = stats.length > 8 ? stats[8] : 0;
		int belumIkut = Math.max(0, peserta - ikutUjian);
		double rataRata = nilaiG[0];
		int layak = gunakan;

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:sans-serif;padding:12px;background:#f8fafc;min-height:100%;'>");

		// --- Stat cards ---
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-bottom:16px;'>");
		appendStatCard(sb, "Peserta Ujian", String.valueOf(peserta), "orang (total)", "#3b82f6");
		appendStatCard(sb, "Sudah Ikut Ujian", String.valueOf(ikutUjian), "orang", "#0ea5e9");
		appendStatCard(sb, "Belum Ikut Ujian", String.valueOf(belumIkut), "orang", "#f59e0b");
		appendStatCard(sb, "Jumlah Soal", String.valueOf(totalSoal), "butir", "#8b5cf6");
		appendStatCard(sb, "Rata-rata Nilai", Common.numberFormat.get().format(rataRata), "poin", "#06b6d4");
		appendStatCard(sb, "Soal Layak Pakai", layak + " / " + totalSoal, "butir", "#22c55e");
		sb.append("</div>");

		// --- Donut charts ---
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:20px;margin-bottom:20px;'>");

		// Kualitas soal (DP)
		sb.append(buildMultiDonutHtml("Kualitas Soal (Daya Pembeda)", totalSoal,
			new String[]{"Gunakan", "Perlu Revisi", "Ganti"},
			new int[]{gunakan, revisi, ganti},
			new String[]{"#22c55e", "#f59e0b", "#ef4444"}));

		// Tingkat kesukaran (TK)
		sb.append(buildMultiDonutHtml("Tingkat Kesukaran", totalSoal,
			new String[]{"Mudah", "Sedang", "Sulit"},
			new int[]{mudah, sedang, sulit},
			new String[]{"#f97316", "#3b82f6", "#7c3aed"}));

		// Keikutsertaan peserta (sudah ikut vs belum ikut) — senada dengan tab Statistik
		sb.append(buildMultiDonutHtml("Keikutsertaan Peserta", peserta,
			new String[]{"Sudah Ikut", "Belum Ikut"},
			new int[]{ikutUjian, belumIkut},
			new String[]{"#0ea5e9", "#f59e0b"}));

		sb.append("</div>");

		// --- Table ---
		sb.append("<div style='overflow-x:auto;border-radius:8px;box-shadow:0 1px 4px rgba(0,0,0,0.08);'>");
		sb.append("<table style='width:100%;border-collapse:collapse;font-size:12px;background:#fff;'>");
		sb.append("<thead>");
		sb.append("<tr style='background:#1e40af;color:#fff;'>");
		sb.append("<th style='padding:8px 6px;text-align:center;white-space:nowrap;'>No.</th>");
		sb.append("<th style='padding:8px 6px;text-align:left;min-width:160px;'>Soal</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>Kunci</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>Benar</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>Salah</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>Kosong</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>TK (p)</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>Kategori Kesukaran</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>DP (D)</th>");
		sb.append("<th style='padding:8px 6px;text-align:center;'>Kategori Daya Beda</th>");
		sb.append("<th style='padding:8px 6px;text-align:left;min-width:120px;'>Kesesuaian Sub-CPMK</th>");
		sb.append("<th style='padding:8px 6px;text-align:left;min-width:140px;'>Distribusi Pilihan</th>");
		sb.append("</tr></thead><tbody>");

		for (int i = 0; i < rows.size(); i++) {
			String[] r = rows.get(i);
			String rowBg = i % 2 == 0 ? "#fff" : "#f1f5f9";
			sb.append("<tr style='background:").append(rowBg).append(";border-bottom:1px solid #e2e8f0;'>");
			sb.append("<td style='padding:7px 6px;text-align:center;font-weight:700;color:#374151;'>").append(escapeStatHtml(r[0])).append("</td>");
			sb.append("<td style='padding:7px 6px;color:#374151;'>").append(escapeStatHtml(r[1])).append("</td>");
			sb.append("<td style='padding:7px 6px;text-align:center;font-weight:700;color:#1d4ed8;'>").append(escapeStatHtml(r[2])).append("</td>");
			sb.append("<td style='padding:7px 6px;text-align:center;color:#166534;font-weight:600;'>").append(escapeStatHtml(r[3])).append("</td>");
			sb.append("<td style='padding:7px 6px;text-align:center;color:#991b1b;'>").append(escapeStatHtml(r[4])).append("</td>");
			sb.append("<td style='padding:7px 6px;text-align:center;color:#78716c;'>").append(escapeStatHtml(r[5])).append("</td>");
			sb.append("<td style='padding:7px 6px;text-align:center;font-weight:600;'>").append(escapeStatHtml(r[6])).append("</td>");

			// Kategori TK badge
			String katTK = r[7];
			String tkBadgeBg = "#3b82f6"; String tkBadgeColor = "#fff";
			if ("Mudah".equals(katTK)) { tkBadgeBg = "#fed7aa"; tkBadgeColor = "#c2410c"; }
			else if ("Sedang".equals(katTK)) { tkBadgeBg = "#bfdbfe"; tkBadgeColor = "#1d4ed8"; }
			else if ("Sulit".equals(katTK)) { tkBadgeBg = "#ede9fe"; tkBadgeColor = "#6d28d9"; }
			else { tkBadgeBg = "#f1f5f9"; tkBadgeColor = "#64748b"; }
			sb.append("<td style='padding:7px 6px;text-align:center;'><span style='background:")
				.append(tkBadgeBg).append(";color:").append(tkBadgeColor)
				.append(";padding:2px 8px;border-radius:12px;font-size:11px;font-weight:600;white-space:nowrap;'>")
				.append(escapeStatHtml(katTK)).append("</span></td>");

			sb.append("<td style='padding:7px 6px;text-align:center;font-weight:600;'>").append(escapeStatHtml(r[8])).append("</td>");

			// Kategori DP badge
			String katDP = r[9];
			String dpBadgeBg = "#f1f5f9"; String dpBadgeColor = "#64748b";
			if ("Sangat Baik".equals(katDP) || "Baik".equals(katDP)) { dpBadgeBg = "#bbf7d0"; dpBadgeColor = "#166534"; }
			else if ("Perlu Revisi".equals(katDP)) { dpBadgeBg = "#fef9c3"; dpBadgeColor = "#854d0e"; }
			else if ("Ganti".equals(katDP)) { dpBadgeBg = "#fee2e2"; dpBadgeColor = "#991b1b"; }
			sb.append("<td style='padding:7px 6px;text-align:center;'><span style='background:")
				.append(dpBadgeBg).append(";color:").append(dpBadgeColor)
				.append(";padding:2px 8px;border-radius:12px;font-size:11px;font-weight:600;white-space:nowrap;'>")
				.append(escapeStatHtml(katDP)).append("</span></td>");

			// Kesesuaian Sub-CPMK
			String subCpmkVal = (r.length > 11 && r[11] != null && !r[11].isEmpty()) ? r[11] : null;
			sb.append("<td style='padding:7px 6px;font-size:11px;color:#1e40af;font-weight:600;'>")
				.append(subCpmkVal != null ? escapeStatHtml(subCpmkVal) : "<span style='color:#94a3b8;font-weight:400;'>&#8212;</span>")
				.append("</td>");
			// Distribusi (raw HTML from builder)
			sb.append("<td style='padding:7px 6px;'>").append(r[10]).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</tbody></table></div>");

		// --- Panduan ---
		sb.append("<div style='margin-top:14px;padding:10px 14px;background:#fff;border-radius:8px;border-left:4px solid #3b82f6;font-size:11px;color:#475569;'>");
		sb.append("<b style='color:#1e40af;'>Panduan Membaca Hasil Analisis:</b><br/>");
		sb.append("&#9632; <b>Tingkat Kesukaran (p)</b>: p &gt; 0.70 = Mudah &nbsp;|&nbsp; 0.30 &le; p &le; 0.70 = Sedang &nbsp;|&nbsp; p &lt; 0.30 = Sulit<br/>");
		sb.append("&#9632; <b>Daya Pembeda (D)</b>: D &ge; 0.40 = Sangat Baik &nbsp;|&nbsp; 0.30&ndash;0.39 = Baik &nbsp;|&nbsp; 0.20&ndash;0.29 = Perlu Revisi &nbsp;|&nbsp; D &lt; 0.20 = Ganti<br/>");
		sb.append("&#9632; Soal ideal: Sedang &amp; Baik/Sangat Baik. Distribusi pilihan yang merata menandakan pengecoh efektif.");
		sb.append("</div>");

		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Menghitung persentase bulat (integer) dari sekumpulan jumlah sehingga TOTAL-nya
	 * TEPAT 100 memakai metode <b>sisa terbesar</b> (largest remainder / Hamilton).
	 * Dipakai untuk distribusi pilihan jawaban agar jumlah semua batang persen tidak
	 * "kurang 1-2%" akibat pembulatan ke bawah per batang.
	 *
	 * @param counts jumlah per kategori (mis. banyak peserta memilih A, B, C, D, E, kosong).
	 * @param total  total pembagi (mis. jumlah peserta). Bila &le; 0, semua hasil 0.
	 * @return array persentase integer seukuran {@code counts}, menjumlah tepat 100 bila
	 *         {@code total > 0} dan jumlah {@code counts} sama dengan {@code total}.
	 */
	private static int[] persenDistribusiSeratus(int[] counts, int total) {
		int n = counts == null ? 0 : counts.length;
		int[] hasil = new int[n];
		if (n == 0 || total <= 0) {
			return hasil;
		}
		double[] sisa = new double[n];
		int jumlahFloor = 0;
		for (int i = 0; i < n; i++) {
			double tepat = counts[i] * 100.0 / total;
			hasil[i] = (int) Math.floor(tepat);
			sisa[i] = tepat - hasil[i];
			jumlahFloor += hasil[i];
		}
		int kurang = 100 - jumlahFloor;
		for (int s = 0; s < kurang; s++) {
			int idxMax = -1;
			double sisaMax = -1.0;
			for (int i = 0; i < n; i++) {
				if (counts[i] > 0 && sisa[i] > sisaMax) {
					sisaMax = sisa[i];
					idxMax = i;
				}
			}
			if (idxMax < 0) {
				break;
			}
			hasil[idxMax]++;
			sisa[idxMax] = -1.0;
		}
		return hasil;
	}

	/**
	 * <b>Tujuan:</b> Membangun satu panel HTML berisi diagram <i>donut</i> berbasis CSS murni
	 * beserta legendanya, untuk memvisualisasikan distribusi BEBERAPA kategori sekaligus dalam
	 * satu lingkaran. Dipakai pada dashboard {@link #analsisButirSoal} untuk tiga panel:
	 * "Kualitas Soal (Daya Pembeda)" (Gunakan/Perlu Revisi/Ganti), "Tingkat Kesukaran"
	 * (Mudah/Sedang/Sulit), dan "Keikutsertaan Peserta" (Sudah Ikut/Belum Ikut).
	 *
	 * <p><b>Perbedaan dengan {@link #buildStatistikPieHtml}.</b> {@code buildStatistikPieHtml}
	 * hanya mampu menampilkan DUA busur (nilai vs sisa) dengan warna tetap hijau/abu-abu dan
	 * menampilkan angka persen besar di tengah lingkaran. Method ini menerima array
	 * {@code labels}/{@code counts}/{@code colors} sepanjang N sehingga dapat menggambar N busur
	 * berwarna bebas, tetapi lubang tengahnya dibiarkan polos (tanpa angka) karena tidak ada satu
	 * angka tunggal yang mewakili distribusi multi-kategori.</p>
	 *
	 * <p><b>Cara kerja.</b></p>
	 * <ol>
	 *   <li><b>Kerangka kartu.</b> Sebuah {@code <div>} berlatar putih, sudut membulat 8px,
	 *       bayangan lembut, dan {@code min-width:240px} agar kartu tidak menyempit berlebihan
	 *       ketika induknya adalah container {@code display:flex;flex-wrap:wrap}.</li>
	 *   <li><b>Judul.</b> Ditampilkan 13px bold warna biru tua {@code #1e3a5f}, diloloskan lewat
	 *       {@link #escapeStatHtml(String)}.</li>
	 *   <li><b>Perakitan {@code conic-gradient}.</b> Untuk setiap kategori dihitung
	 *       {@code pct = (int)(counts[i] * 100.0 / total)} — pembulatan ke bawah (truncation),
	 *       BUKAN {@code Math.round}. Variabel {@code soFar} mengakumulasi posisi awal busur
	 *       berikutnya, sehingga tiap kategori menghasilkan potongan gradien
	 *       {@code "<warna> <soFar>% <soFar+pct>%"}. Bila total akumulasi kurang dari 100%
	 *       (akibat pembulatan ke bawah, atau karena jumlah {@code counts} memang lebih kecil
	 *       dari {@code total}), sisa lingkaran diisi warna abu-abu {@code #e2e8f0}. Konsekuensi
	 *       yang perlu diketahui: busur pada donut ini bisa berjumlah 98&ndash;99%, berbeda dengan
	 *       batang distribusi pilihan jawaban yang sengaja dinormalisasi tepat 100% memakai
	 *       {@link #persenDistribusiSeratus(int[], int)}. Perbedaan ini disengaja — sisa abu-abu
	 *       pada donut justru bermakna "soal yang belum terkategori" (mis. soal berstatus
	 *       "Blm dikerjakan" yang tidak masuk hitungan Gunakan/Revisi/Ganti mana pun).</li>
	 *   <li><b>Lingkaran.</b> Div 72&times;72px {@code border-radius:50%} dengan latar
	 *       {@code conic-gradient(...)}, di dalamnya div putih 48&times;48px bermargin
	 *       {@code 12px auto} yang membentuk "lubang" donut.</li>
	 *   <li><b>Legenda.</b> Satu baris per kategori: titik bulat 10px berwarna sesuai
	 *       {@code colors[i]}, nama kategori, lalu jumlah dan persentase yang didorong ke kanan
	 *       memakai {@code margin-left:auto}.</li>
	 * </ol>
	 *
	 * <p><b>Kontrak parameter.</b> Ketiga array {@code labels}, {@code counts}, dan {@code colors}
	 * HARUS sepanjang N yang sama. Perakitan gradien mengiterasi {@code counts.length} sedangkan
	 * legenda mengiterasi {@code labels.length}; bila panjangnya berbeda akan terjadi
	 * {@link ArrayIndexOutOfBoundsException} pada {@code colors[i]}. Tidak ada validasi defensif
	 * di sini karena seluruh pemanggil adalah kode internal {@link #buildAnalisisVisualHtml}
	 * yang menuliskan ketiga array sebagai literal berdampingan.</p>
	 *
	 * <p><b>Penjagaan pembagian nol.</b> Bila {@code total <= 0}, seluruh {@code pct} bernilai 0
	 * sehingga lingkaran tampil sepenuhnya abu-abu dan legenda menampilkan "0 (0%)" —
	 * tidak melempar {@link ArithmeticException} dan tidak menghasilkan {@code NaN}.</p>
	 *
	 * <p><b>Keamanan.</b> {@code title} dan setiap {@code labels[i]} diloloskan lewat
	 * {@link #escapeStatHtml(String)}. {@code counts[i]} adalah {@code int} sehingga aman.
	 * {@code colors[i]} TIDAK di-escape karena disisipkan ke dalam nilai properti CSS; nilainya
	 * berasal dari literal hex di kode pemanggil, bukan dari database maupun input pengguna.
	 * Jangan pernah meneruskan warna yang berasal dari data pengguna ke parameter ini tanpa
	 * validasi format terlebih dahulu — CSS injection lewat {@code conic-gradient} memungkinkan
	 * pemuatan resource eksternal.</p>
	 *
	 * <p><b>Batasan browser.</b> {@code conic-gradient} membutuhkan Chrome 69+/Firefox 83+/
	 * Safari 12.1+. Pada browser lama lingkaran akan tampil polos tanpa busur; tidak ada
	 * fallback yang disediakan.</p>
	 *
	 * <p><b>Sifat.</b> Sepenuhnya {@code static} dan stateless — tidak menyentuh field instance,
	 * session Hibernate, maupun komponen ZK. Aman dipanggil dari thread latar (memang dipanggil
	 * dari thread latar {@link #analsisButirSoal} melalui {@link #buildAnalisisVisualHtml}).</p>
	 *
	 * @param title  judul kartu yang ditampilkan di atas lingkaran; di-escape HTML
	 * @param total  penyebut persentase, umumnya jumlah soal atau jumlah peserta; bila
	 *               {@code <= 0} seluruh persentase menjadi 0
	 * @param labels array nama kategori sepanjang N; di-escape HTML
	 * @param counts array jumlah per kategori sepanjang N, berpasangan indeks dengan {@code labels}
	 * @param colors array warna CSS (mis. {@code "#22c55e"}) sepanjang N; TIDAK di-escape
	 * @return potongan HTML lengkap satu kartu donut, siap di-{@code append} ke HTML dashboard
	 * @see #buildAnalisisVisualHtml(java.util.List, int[], double[])
	 * @see #buildStatistikPieHtml(String, int, int, String, String)
	 */
	private static String buildMultiDonutHtml(String title, int total, String[] labels, int[] counts, String[] colors) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#fff;border-radius:8px;padding:14px 18px;box-shadow:0 1px 4px rgba(0,0,0,0.08);min-width:240px;'>");
		sb.append("<div style='font-size:13px;font-weight:700;color:#1e3a5f;margin-bottom:10px;'>").append(escapeStatHtml(title)).append("</div>");

		// Build conic-gradient stops
		StringBuilder gradient = new StringBuilder();
		int soFar = 0;
		for (int i = 0; i < counts.length; i++) {
			int pct = total > 0 ? (int)(counts[i] * 100.0 / total) : 0;
			if (i > 0) gradient.append(", ");
			gradient.append(colors[i]).append(" ").append(soFar).append("% ").append(soFar + pct).append("%");
			soFar += pct;
		}
		if (soFar < 100) {
			gradient.append(", #e2e8f0 ").append(soFar).append("% 100%");
		}

		sb.append("<div style='display:flex;align-items:center;gap:14px;'>");
		sb.append("<div style='width:72px;height:72px;border-radius:50%;background:conic-gradient(")
			.append(gradient).append(");flex-shrink:0;'>");
		sb.append("<div style='width:48px;height:48px;border-radius:50%;background:#fff;margin:12px auto;'></div>");
		sb.append("</div>");

		sb.append("<div style='display:flex;flex-direction:column;gap:4px;'>");
		for (int i = 0; i < labels.length; i++) {
			int pct = total > 0 ? (int)(counts[i] * 100.0 / total) : 0;
			sb.append("<div style='display:flex;align-items:center;gap:6px;font-size:11px;'>");
			sb.append("<span style='width:10px;height:10px;border-radius:50%;background:").append(colors[i]).append(";flex-shrink:0;'></span>");
			sb.append("<span style='color:#374151;'>").append(escapeStatHtml(labels[i])).append("</span>");
			sb.append("<span style='color:#6b7280;font-weight:600;margin-left:auto;'>").append(counts[i]).append(" (").append(pct).append("%)</span>");
			sb.append("</div>");
		}
		sb.append("</div></div></div>");
		return sb.toString();
	}

	/**
	 * <b>Tujuan:</b> Menambahkan satu kartu statistik (stat card) ke dalam {@link StringBuilder} HTML yang sedang
	 * dibangun untuk panel statistik ujian. Setiap kartu menampilkan satu metrik ujian (misalnya jumlah peserta,
	 * jumlah soal terjawab, rata-rata nilai) dalam format yang menarik secara visual dengan latar putih, sudut
	 * membulat, bayangan lembut, dan garis aksen berwarna di bagian atas kartu.
	 *
	 * <p><b>Cara kerja:</b> Method ini bersifat murni stateless: ia hanya memanggil {@code sb.append()} berulang
	 * kali untuk menghasilkan elemen {@code <div>} HTML dengan inline style. Struktur kartu yang dihasilkan terdiri
	 * dari tiga lapisan vertikal:
	 * <ol>
	 *   <li><b>Label atas</b> — nama metrik dalam ukuran font kecil (11px) berwarna abu-abu ({@code #6b7280}),
	 *       diloloskan melalui {@link #escapeStatHtml(String)} untuk mencegah XSS.</li>
	 *   <li><b>Nilai utama</b> — angka/nilai metrik dalam ukuran besar (22px, bold 800) dengan warna sesuai
	 *       parameter {@code color}, misalnya hijau untuk "Lulus" atau merah untuk "Gagal".</li>
	 *   <li><b>Satuan/keterangan tambahan</b> — teks penjelasan kecil (11px) berwarna abu-abu terang
	 *       ({@code #94a3b8}), misalnya "dari total peserta" atau "menit".</li>
	 * </ol>
	 * Kartu menggunakan {@code flex:1} agar bila ada beberapa kartu dalam satu baris flex-container, lebar dibagi
	 * merata. Lebar minimal 140px mencegah kartu terlalu sempit saat banyak metrik ditampilkan berdampingan.
	 *
	 * <p><b>Keamanan:</b> Tiga teks yang dapat mengandung karakter HTML berbahaya ({@code label}, {@code value},
	 * {@code unit}) semuanya diloloskan melalui {@link #escapeStatHtml(String)} sebelum dimasukkan ke dalam HTML.
	 * Parameter {@code color} dipercaya merupakan nilai CSS yang sudah aman (misalnya {@code "#22c55e"}) dan tidak
	 * di-escape karena dimasukkan ke dalam atribut style bukan konten teks — validasi format diserahkan ke caller.
	 *
	 * <p><b>Integrasi:</b> Method ini dipanggil dari {@link #buildMultiDonutHtml} dan konteks lain yang membangun
	 * panel ringkasan berisi beberapa kartu statistik berdampingan dalam satu container {@code display:flex}.
	 * Caller bertanggung jawab membuka dan menutup elemen container flex tersebut; method ini hanya menambahkan
	 * satu kartu di dalamnya.
	 *
	 * <p><b>Sifat/thread-safety:</b> Sepenuhnya stateless. Tidak mengakses field instance maupun resource bersama.
	 * Aman dipanggil dari thread mana pun asalkan parameter {@code sb} tidak diakses bersamaan dari thread lain.
	 *
	 * <p><b>Pemeliharaan:</b> Bila desain kartu statistik perlu diubah (misalnya menambahkan ikon, mengubah font
	 * size, atau mengganti skema warna), cukup ubah template HTML di sini — seluruh kartu yang memanggil method
	 * ini akan ikut berubah secara otomatis.
	 *
	 * @param sb    {@link StringBuilder} HTML tempat kartu ditambahkan; tidak boleh {@code null}
	 * @param label teks judul/nama metrik yang ditampilkan di atas kartu, misalnya "Jumlah Peserta"; di-escape HTML
	 * @param value nilai metrik sebagai string besar di tengah kartu, misalnya "120"; di-escape HTML
	 * @param unit  satuan atau keterangan tambahan di bawah nilai, misalnya "orang"; di-escape HTML
	 * @param color nilai warna CSS untuk aksen garis atas dan teks nilai utama, misalnya {@code "#22c55e"}
	 */
	private static void appendStatCard(StringBuilder sb, String label, String value, String unit, String color) {
		sb.append("<div style='background:#fff;border-radius:8px;padding:12px 18px;box-shadow:0 1px 4px rgba(0,0,0,0.08);min-width:140px;border-top:3px solid ").append(color).append(";flex:1;'>");
		sb.append("<div style='font-size:11px;color:#6b7280;margin-bottom:4px;'>").append(escapeStatHtml(label)).append("</div>");
		sb.append("<div style='font-size:22px;font-weight:800;color:").append(color).append(";'>").append(escapeStatHtml(value)).append("</div>");
		sb.append("<div style='font-size:11px;color:#94a3b8;'>").append(escapeStatHtml(unit)).append("</div>");
		sb.append("</div>");
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan string HTML lengkap untuk satu diagram lingkaran (donut chart) CSS yang menampilkan
	 * persentase dari suatu nilai terhadap total, digunakan pada panel statistik ujian di bagian timur layar untuk
	 * memvisualisasikan metrik seperti "Peserta yang ikut ujian" atau "Soal yang terjawab".
	 *
	 * <p><b>Cara kerja:</b> Method ini membangun HTML murni berbasis CSS tanpa JavaScript maupun library eksternal,
	 * sehingga kompatibel dengan semua browser modern dan tidak membutuhkan koneksi internet. Langkah-langkah
	 * pembangunan HTML:
	 * <ol>
	 *   <li><b>Kalkulasi persentase:</b> {@code percent = round((value / total) * 100)}, dengan penjagaan
	 *       {@code safeTotal = max(total, 1)} untuk mencegah pembagian nol. Hasil dijepit ke rentang [0, 100].</li>
	 *   <li><b>Judul diagram:</b> ditampilkan di atas lingkaran, berwarna biru tua ({@code #1e3a8a}), bold.</li>
	 *   <li><b>Lingkaran luar (donut ring):</b> Sebuah {@code <div>} berukuran 150×150px dengan {@code border-radius:50%}
	 *       dan properti CSS {@code conic-gradient} yang membagi lingkaran menjadi dua busur:
	 *       <ul>
	 *         <li>Busur hijau ({@code #22c55e}) dari 0% hingga {@code gradPct} mewakili bagian {@code value}.</li>
	 *         <li>Busur abu-abu ({@code #e2e8f0}) dari {@code gradPct} hingga 100% mewakili bagian sisa.</li>
	 *       </ul></li>
	 *   <li><b>Lingkaran tengah (hole):</b> Div putih 94×94px terpusat di dalam donut ring (teknik "donut")
	 *       menampilkan persentase dalam angka besar ({@code 22px, font-weight:900}) berwarna biru ({@code #1d4ed8})
	 *       dan label "dari total" di bawahnya.</li>
	 *   <li><b>Legenda bawah:</b> Dua item inline — satu dengan lingkaran kecil hijau untuk {@code labelValue} dan
	 *       satu dengan lingkaran kecil abu-abu untuk {@code labelRemain} — masing-masing menampilkan jumlah aktual
	 *       dalam {@code <b>}.</li>
	 * </ol>
	 *
	 * <p><b>Batasan teknis:</b> Properti CSS {@code conic-gradient} didukung oleh browser modern (Chrome 69+,
	 * Firefox 83+, Safari 12.1+) tetapi tidak tersedia di Internet Explorer. Karena aplikasi ini berbasis ZK dan
	 * umumnya diakses melalui browser modern, ini bukan masalah praktis.
	 *
	 * <p><b>Keamanan:</b> Parameter {@code title}, {@code labelValue}, dan {@code labelRemain} diloloskan melalui
	 * {@link #escapeStatHtml(String)} sebelum dimasukkan ke HTML. Parameter {@code value}, {@code total}, dan
	 * {@code remain} adalah tipe primitif {@code int} yang dihasilkan dari kalkulasi — tidak berasal dari input
	 * pengguna sehingga aman langsung dimasukkan.
	 *
	 * <p><b>Sifat/thread-safety:</b> Sepenuhnya stateless dan tidak mengakses resource bersama. Aman dipanggil
	 * dari thread mana pun.
	 *
	 * <p><b>Pemeliharaan:</b> Ukuran donut (150px outer, 94px inner), warna, dan font-size dikodekan langsung
	 * sebagai inline style di sini. Untuk mengubah tampilan global semua donut statistik, cukup ubah nilai-nilai
	 * tersebut di method ini. Bila perlu membuat donut dengan ukuran berbeda, pertimbangkan menambahkan parameter
	 * ukuran atau membuat overload terpisah.
	 *
	 * @param title        judul yang ditampilkan di atas lingkaran, misalnya "Peserta Ujian"; di-escape HTML
	 * @param value        nilai numerik yang diwakili busur hijau (bagian yang "ada/terpenuhi")
	 * @param total        total keseluruhan sebagai penyebut persentase; jika &le;0 diperlakukan sebagai 1
	 * @param labelValue   label untuk legenda nilai (busur hijau), misalnya "Ikut Ujian"; di-escape HTML
	 * @param labelRemain  label untuk legenda sisa (busur abu-abu), misalnya "Tidak Ikut"; di-escape HTML
	 * @return string HTML lengkap berisi donut chart CSS, siap dimasukkan ke dalam panel ZK Html component
	 */
	private static String buildStatistikPieHtml(String title, int value, int total, String labelValue, String labelRemain) {
		int safeTotal = total <= 0 ? 1 : total;
		int percent = (int) Math.round((value * 100.0) / safeTotal);
		if (percent < 0) percent = 0;
		if (percent > 100) percent = 100;
		int remain = Math.max(0, total - value);
		String gradPct = percent + "%";
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:16px 8px;text-align:center;font-family:sans-serif;'>");
		sb.append("<div style='font-size:14px;font-weight:700;color:#1e3a8a;margin-bottom:10px;'>")
			.append(escapeStatHtml(title)).append("</div>");
		sb.append("<div style='width:150px;height:150px;border-radius:50%;");
		sb.append("background:conic-gradient(#22c55e 0% ").append(gradPct)
			.append(",#e2e8f0 ").append(gradPct).append(" 100%);");
		sb.append("margin:0 auto;display:flex;align-items:center;justify-content:center;'>");
		sb.append("<div style='width:94px;height:94px;border-radius:50%;background:#fff;");
		sb.append("display:flex;flex-direction:column;align-items:center;justify-content:center;");
		sb.append("box-shadow:inset 0 2px 8px rgba(0,0,0,.12);'>");
		sb.append("<span style='font-size:22px;font-weight:900;color:#1d4ed8;'>").append(percent).append("%</span>");
		sb.append("<span style='font-size:10px;color:#64748b;margin-top:2px;'>dari total</span>");
		sb.append("</div></div>");
		sb.append("<div style='margin-top:12px;display:flex;justify-content:center;gap:18px;font-size:12px;color:#475569;flex-wrap:wrap;'>");
		sb.append("<span><span style='display:inline-block;width:10px;height:10px;border-radius:50%;");
		sb.append("background:#22c55e;margin-right:5px;vertical-align:middle;'></span>");
		sb.append(escapeStatHtml(labelValue)).append(": <b>").append(value).append("</b></span>");
		sb.append("<span><span style='display:inline-block;width:10px;height:10px;border-radius:50%;");
		sb.append("background:#e2e8f0;border:1px solid #94a3b8;margin-right:5px;vertical-align:middle;'></span>");
		sb.append(escapeStatHtml(labelRemain)).append(": <b>").append(remain).append("</b></span>");
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * <b>Tujuan:</b> Mengloloskan karakter-karakter berbahaya dari string yang akan dimasukkan ke dalam konten HTML
	 * di panel statistik ujian, mencegah serangan Cross-Site Scripting (XSS) pada data yang berasal dari database
	 * atau input pengguna yang ditampilkan melalui HTML string builder.
	 *
	 * <p><b>Cara kerja:</b> Method ini melakukan penggantian tiga karakter yang paling berbahaya dalam konteks konten
	 * HTML (di antara tag, bukan di dalam atribut):
	 * <ul>
	 *   <li>{@code &} → {@code &amp;} — harus diganti pertama agar entity hasil escape lainnya tidak ikut di-escape
	 *       kembali (misalnya {@code &lt;} tidak menjadi {@code &amp;lt;}).</li>
	 *   <li>{@code <} → {@code &lt;} — mencegah pembukaan tag HTML baru yang tidak diinginkan.</li>
	 *   <li>{@code >} → {@code &gt;} — mencegah penutupan tag HTML dan potensi injeksi konten.</li>
	 * </ul>
	 * Jika parameter {@code v} bernilai {@code null}, method mengembalikan string kosong ({@code ""}) sehingga
	 * caller tidak perlu memeriksa {@code null} secara terpisah.
	 *
	 * <p><b>Cakupan escape:</b> Method ini TIDAK mengloloskan tanda petik ({@code "} atau {@code '}) karena
	 * dirancang untuk digunakan dalam konteks <em>konten teks</em> HTML (di antara tag), bukan di dalam nilai
	 * atribut HTML. Bila method ini digunakan di dalam nilai atribut (misalnya {@code value="..."}), perlu
	 * ditambahkan escape untuk {@code "} dan/atau {@code '} tergantung pada tanda kutip yang digunakan.
	 * Dalam implementasi saat ini, seluruh pemanggil method ini menggunakannya untuk konten teks, bukan atribut.
	 *
	 * <p><b>Perbandingan dengan metode lain:</b> Kelas lain seperti {@link ProsesUjianHelper} mengimplementasikan
	 * {@code escapeHtmlSimple} yang turut mengloloskan tanda petik. Method ini lebih minimal karena konteks
	 * penggunaannya (konten teks di antara tag HTML) tidak memerlukannya.
	 *
	 * <p><b>Sifat/thread-safety:</b> Sepenuhnya stateless. String Java bersifat immutable sehingga operasi
	 * {@code replace} membuat string baru tanpa mengubah parameter asli. Aman dipanggil dari thread mana pun.
	 *
	 * <p><b>Pemeliharaan:</b> Bila di masa mendatang HTML builder pada panel statistik juga menerima teks
	 * yang akan dimasukkan ke dalam nilai atribut HTML, tambahkan escape untuk {@code '"'} dan {@code '\''}.
	 * Urutan penggantian (& terlebih dahulu) HARUS dipertahankan untuk menghindari double-escaping.
	 *
	 * @param v string yang akan di-escape; boleh {@code null} (dikembalikan sebagai {@code ""})
	 * @return string dengan karakter {@code &}, {@code <}, dan {@code >} diganti entity HTML-nya, atau string
	 *         kosong jika {@code v} bernilai {@code null}
	 */
	private static String escapeStatHtml(String v) {
		if (v == null) return "";
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

}
