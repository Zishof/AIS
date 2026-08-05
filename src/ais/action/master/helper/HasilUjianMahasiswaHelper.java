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
			@Override
			public void onEvent(Event arg0) throws Exception {
				new PertemuanPunyaUjianHelper(null, null).bukaPengaturanUjian(pertemuanPunyaUjian,
						new EventListener() {
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

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin mengulang seluruh ujian ini? Seluruh hasil dan jawaban peserta pada ujian ini akan dikosongkan dan tidak dapat dikembalikan. Silakan pilih OK untuk melanjutkan atau Batal untuk membatalkan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

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
			@Override
			public void onEvent(Event event) throws Exception {
				HasilUjianMahasiswaHelper.ujianDianggapHadir(pertemuanPunyaUjian, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

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

				@Override
				public void onEvent(Event arg0) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);
						}
					});

					new Thread(new Runnable() {

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

				@Override
				public Object ambil() {
					return hasilUjianMahasiswas;
				}
			}, new Ambildata() {

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
				@Override
				public void onEvent(Event event) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);

						}
					});

					new Thread(new Runnable() {

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

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

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

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});

					final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
					File file = new File("/opt/ecampus/test.txt");
					ais.common.BacaTulisUtil.tulis(file, "test send..");
					driveUtilPerPengguna.prosesBackup(file, "test_files",

							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
											.getData();

									if (fileUpload != null && fileUpload.getId() != null) {

										new Thread(new Runnable() {

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
	 * Menampilkan popup "Rincian Skor" untuk satu Sub-CPMK milik seorang peserta: daftar soal
	 * pada Sub-CPMK tersebut beserta skor yang didapat dan skor maksimal per soal, plus totalnya.
	 * Dipicu saat nilai per Sub-CPMK pada tabel peserta diklik.
	 *
	 * @param himParam    hasil ujian peserta (boleh detached; di-refetch di sini)
	 * @param formatNilai Sub-CPMK yang diklik
	 */
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
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String[] htmlRef  = new String[1];
				final byte[][] xlsRef   = new byte[1][];
				final Throwable[] errorRef = new Throwable[1];

				final Label label = Common.displayLoadBar(new EventListener() {
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
							@Override public void onEvent(Event e) throws Exception { window.detach(); }
						});
						btnClose.setParent(toolbar);

						MyToolbarbuttonConfig btnXls = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
						btnXls.addEventListener("onClick", new EventListener() {
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

	/**
	 * Membuat tombol "Analisis Butir Soal" yang menghasilkan laporan psikometrik
	 * (Tingkat Kesukaran dan Daya Pembeda) untuk setiap soal dalam ujian.
	 *
	 * <p><b>Tujuan:</b> Membantu dosen/tim ujian mengevaluasi kualitas soal. Soal yang terlalu
	 * mudah (TK mendekati 1.0) atau terlalu sulit (TK mendekati 0.0), atau soal dengan daya
	 * pembeda negatif (peserta pandai malah salah), perlu direvisi atau diganti untuk meningkatkan
	 * validitas dan reliabilitas ujian.</p>
	 *
	 * <p><b>Algoritma analisis butir soal (Item Analysis):</b></p>
	 * <ul>
	 *   <li><b>Tingkat Kesukaran (TK):</b> proporsi peserta yang menjawab benar = benar / total.
	 *       Kategori: Mudah (TK>0.7), Sedang (0.3-0.7), Sulit (&lt;0.3).</li>
	 *   <li><b>Daya Pembeda (DP):</b> beda proporsi benar antara kelompok atas (27% tertinggi)
	 *       dan kelompok bawah (27% terendah). Kategori: Sangat Baik (&gt;0.4), Baik (0.3-0.4),
	 *       Cukup (0.2-0.3), Jelek (&lt;0.2), Negatif (&lt;0).</li>
	 *   <li><b>Rekomendasi:</b> Gunakan/Revisi/Ganti berdasarkan kombinasi TK dan DP.</li>
	 * </ul>
	 *
	 * <p><b>Output:</b> Dua output dihasilkan di thread latar:</p>
	 * <ol>
	 *   <li>File Excel (.xlsx) berisi data analisis tabular.</li>
	 *   <li>HTML visual ({@link #buildAnalisisVisualHtml}) berisi chart distribusi dan
	 *       ringkasan statistik.</li>
	 * </ol>
	 *
	 * <p><b>Pemeliharaan:</b> Algoritma item analysis di sini menggunakan formula klasik
	 * (atas-bawah 27%). Bila ingin mengubah persentase grup atau formula DP, ubah konstanta
	 * dan hitungan di dalam closure thread latar. Nama method mengandung typo "analsis"
	 * (bukan "analisis") — jangan perbaiki untuk menjaga kompatibilitas backward.</p>
	 *
	 * @param pertemuanPunyaUjian ujian yang soal-soalnya akan dianalisis
	 * @param ambil               callback opsional (tidak digunakan dalam output, hanya untuk
	 *                            konsistensi signature dengan {@link #hasilObe})
	 * @return {@code Toolbarbutton} siap pasang di toolbar, selalu visible
	 */
	/**
	 * Overload kompatibilitas (2 argumen): jumlah "Peserta Ujian" pada dashboard memakai
	 * ukuran map hasil ujian ({@code hasilUjianMahasiswas.size()}). Dipakai antara lain oleh
	 * {@code HasilUjianSiswaHelper}. Untuk menampilkan jumlah peserta TERDAFTAR (sama seperti
	 * "Jumlah Peserta" di tab Statistik), gunakan overload 3 argumen di bawah.
	 */
	public static Toolbarbutton analsisButirSoal(final PertemuanPunyaUjian pertemuanPunyaUjian, final Ambildata ambil) {
		return analsisButirSoal(pertemuanPunyaUjian, ambil, null);
	}

	/**
	 * Versi lengkap: {@code ambilJumlahPeserta} (boleh {@code null}) menyediakan jumlah peserta
	 * TERDAFTAR pada saat tombol diklik — dipakai agar kartu "Peserta Ujian" pada dashboard
	 * KONSISTEN dengan angka "Jumlah Peserta" di tab Statistik. Bila {@code null} atau tidak
	 * mengembalikan angka &gt; 0, jumlah peserta jatuh-balik ke ukuran map hasil ujian.
	 */
	public static Toolbarbutton analsisButirSoal(final PertemuanPunyaUjian pertemuanPunyaUjian, final Ambildata ambil,
			final Ambildata ambilJumlahPeserta) {
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Analisis Butir Soal", "/img/svg/check2-circle.svg");
		cari.addEventListener("onClick", new EventListener() {

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
									double dpAnal = jumlahAtas > 0
											? (jmlAtasAnal.doubleValue() - jmlBawahAnal.doubleValue()) / jumlahAtas
											: 0.0;
									String katDPAnal;
									if (totalJawabAnal == 0) {
										katDPAnal = "Blm dikerjakan";
									} else if (dpAnal >= 0.40) {
										katDPAnal = "Sangat Baik"; statsGlobal[2]++;
									} else if (dpAnal >= 0.30) {
										katDPAnal = "Baik"; statsGlobal[2]++;
									} else if (dpAnal >= 0.20) {
										katDPAnal = "Perlu Revisi"; statsGlobal[3]++;
									} else {
										katDPAnal = "Ganti"; statsGlobal[4]++;
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

									double jml = (jmlAtas.doubleValue() - jmlBawah.doubleValue()) / jumlahAtas;

									XSSFCell cell = rowhead.createCell(col);
									cell.setCellValue("**" + Common.numberFormat.get().format(jml));

									String ni;
									if (benar.equals(0) && salah.equals(0)) {
										ni = "Blm dikerjakan";
									} else if (jml < 0.20) {
										ni = "REDGanti";
									} else if (jml < 0.40) {
										ni = "YELLOWRevisi";
									} else {
										ni = "GREENGunakan";
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

	

	public class DetailPertemuanPunyaUjianRenderer extends ais.ui.util.MyRowRenderer {

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

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					detail.setAttribute("eventListener", this);

					if (event != null && event.getData() != null && event.getData() instanceof HasilUjianMahasiswa) {
						Common.clear(arg0);
						Common.createDefaultTimer(new EventListener() {

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

							new MyLabelKecil(nilai.getNama() + " : " + Common.numberFormat.get().format(nilaiDapat)
									+ (nilaiMax.equals(0.0) ? "" : " / " + Common.numberFormat.get().format(nilaiMax)))
									.setParent(vboxDa);

						}
					}
				}

			} else {

				new Label(Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())
						+ (hasilUjianMahasiswa.getJawabanBenarMax() == null ? ""
								: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenarMax())))
						.setParent(arg0);
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

				@Override
				public void onEvent(Event arg0) throws Exception {
					hasilUjianMahasiswa.put(
							(startIndexInput.getValue() == null ? 0 : (startIndexInput.getValue() - 1)) + "", "index");
				}
			});
			startIndexInput.setParent(hb);

			int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan();

			Set<Long> idsa = (Set<Long>) s[1];
			int terjawab = idsa.size();

			int belum = totalSoal - terjawab;

			Double persen = (100.0 * terjawab) / totalSoal;
			Double persenBelum = (100.0 * belum) / totalSoal;

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

								MyLabelKecil lblNilai = new MyLabelKecil(nilai.getNama() + (nilaiMax.equals(0.0) ? ""
										: " : " + Common.numberFormat.get().format((nilaiDapat * 100.0) / nilaiMax)));
								if (!nilaiMax.equals(0.0)) {
									// Nilai per Sub-CPMK dapat diklik → popup daftar soal + skor yang didapat.
									lblNilai.setStyle("cursor:pointer; text-decoration:underline; color:#2563eb;");
									lblNilai.setTooltiptext("Klik untuk melihat rincian soal & skor " + nilai.getNama());
									final FormatNilai fnKlik = nilai;
									final HasilUjianMahasiswa himKlik = hasilUjianMahasiswa;
									lblNilai.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											bukaPopupRincianSubCpmk(himKlik, fnKlik);
										}
									});
								}
								lblNilai.setParent(vboxDa);

							}
						}
					}

				} else {
					final HasilUjianMahasiswa himNilaiKlik = hasilUjianMahasiswa;
					MyLabelKecil lblNilaiPg = new MyLabelKecil(
						Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()) + "");
					// Nilai (pilihan ganda) dapat DIKLIK -> popup perbandingan Skor Jawaban vs Skor Diperoleh
					// per soal (mirip versi OBE). Memudahkan menemukan soal berdata tak wajar (skor > maks).
					lblNilaiPg.setStyle("cursor:pointer; text-decoration:underline; color:#2563eb;");
					lblNilaiPg.setTooltiptext("Klik untuk melihat perbandingan Skor Jawaban vs Skor Diperoleh per soal");
					lblNilaiPg.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							bukaPopupPerbandinganSkor(himNilaiKlik);
						}
					});
					lblNilaiPg.setParent(arg0);
				}
			} else {

				hbox = new Hbox();
				hbox.setParent(arg0);

				final MyDoublebox doublebox = new MyDoublebox();
				doublebox.setCols(3);
				doublebox.setValue(tempHasilUjianMahasiswa.getNilai());
				doublebox.setParent(hbox);
				final org.zkoss.zul.Label lblAutoNilai = new org.zkoss.zul.Label("");
				lblAutoNilai.setStyle("color:green;font-size:13px;font-weight:bold;");
				lblAutoNilai.setParent(hbox);
				doublebox.addEventListener("onChange", new EventListener() {

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

			keterangan.addEventListener("onChange", new EventListener() {

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

	private int jumlahPeserta = 0;
	private Map<Long, Object[]> hasilUjianMahasiswas = null;
	
	// PERBAIKAN 1: Gunakan tipe pasti <VOMahasiswa>, hindari wildcard '? extends'
	private List<VOMahasiswa> mahasiswasTemorary = null;

	@SuppressWarnings("unchecked")
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
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Menggunakan {@code ConcurrentHashMap} sebagai storage hasil ({@code hasilUjianMahasiswas})
	 *       agar aman bila dipanggil dari konteks multi-thread.</li>
	 *   <li>Membaca parameter filter: nilai {@code pertemuanPunyaUjian} (spesifik atau semua
	 *       dalam pertemuan), {@code mahasiswa}, {@code biodataCalonMahasiswa}, dan nilai
	 *       pencarian dari {@link #nama}.</li>
	 *   <li>Membangun Criteria berbeda untuk masing-masing tipe peserta (mahasiswa reguler,
	 *       calon mahasiswa PMB dengan berbagai filter gelombang/ruangan, siswa, calon siswa,
	 *       peserta kursus). Tiap Criteria digabung via OR bila mode semua-peserta.</li>
	 *   <li>Memanggil {@link #displayStatistik} dengan hasil agregat setelah data dimuat.</li>
	 *   <li>Setiap hasil dimasukkan ke grid sebagai {@code Object[]} yang dirender oleh
	 *       inner class {@code RowRenderer}.</li>
	 * </ol>
	 *
	 * <p><b>Session Hibernate:</b> Menggunakan session terdedikasi ({@code openSession()} dari
	 * SessionFactory), ditutup di {@code finally} via {@link #closeSessionSafe(Session)}.
	 * Ini penting karena method ini dipanggil dari thread latar DataCriteria.</p>
	 *
	 * <p><b>Parameter {@code value}:</b> Bila {@code Boolean.TRUE}, grid di-refresh (data lama
	 * dihapus terlebih dahulu). Bila null/false, ini adalah load pertama kali.</p>
	 *
	 * @param value Boolean; true = refresh mode (invalidasi cache); null/false = load pertama
	 */
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
	 * Method helper untuk menutup session Hibernate dengan aman
	 * sesuai dengan urutan spesifik yang diminta.
	 */
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

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

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
	 * Membangun HTML untuk widget "multi-donut" berbasis CSS yang menampilkan distribusi
	 * beberapa kategori dalam satu panel ringkas. Digunakan untuk visualisasi kualitas soal
	 * (Gunakan/Revisi/Ganti) dan tingkat kesukaran (Mudah/Sedang/Sulit).
	 *
	 * <p><b>Cara kerja:</b> Untuk setiap kategori, menghitung persentase ({@code count/total*100}),
	 * menampilkan progress bar horizontal bertingkat, dan legenda berwarna. Semua styling
	 * dilakukan inline tanpa CSS eksternal agar widget portabel dan tidak bergantung pada
	 * class CSS aplikasi.</p>
	 *
	 * @param title   judul widget yang ditampilkan di atas
	 * @param total   total soal (denominasi untuk persentase)
	 * @param labels  array label kategori (panjang harus sama dengan counts dan colors)
	 * @param counts  array jumlah soal per kategori
	 * @param colors  array kode warna CSS hex per kategori (misal "#22c55e")
	 * @return HTML string siap dimasukkan ke {@code org.zkoss.zul.Html}
	 */
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
