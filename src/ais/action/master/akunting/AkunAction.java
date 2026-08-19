package ais.action.master.akunting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.akunting.util.AkunTreeModel;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.AkunDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupAkun;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>AkunAction — Pengelola Master Akun (Chart of Accounts)</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK Composer yang menangani manajemen master akun keuangan atau
 * Chart of Accounts (COA) dalam sistem akuntansi. Akun adalah unit dasar pencatatan
 * transaksi keuangan — setiap debet dan kredit harus dikaitkan dengan akun yang terdefinisi.
 * Kelas ini menyediakan antarmuka lengkap untuk membuat, mengubah, menghapus, mencari,
 * mengekspor, dan mengimpor akun, serta menampilkan hierarki akun dalam bentuk pohon.</p>
 *
 * <p><b>Struktur hierarki akun:</b><br>
 * Setiap {@code Akun} dapat memiliki parent (akun induk) membentuk struktur pohon yang
 * fleksibel (N level). Akun daun (leaf node) adalah akun yang tidak memiliki anak dan
 * dapat digunakan dalam transaksi. Akun induk berfungsi sebagai pengkelompok. Hierarki
 * ini ditampilkan dalam komponen {@code Tree} ZK di sisi kiri halaman, sementara
 * grid tabel menampilkan hasil pencarian di sisi kanan.</p>
 *
 * <p><b>Atribut kunci sebuah Akun:</b></p>
 * <ul>
 *   <li><b>Kode:</b> Kode unik akun (misalnya "1.1.01.001"); divalidasi unik sebelum simpan.</li>
 *   <li><b>Nama:</b> Nama deskriptif akun.</li>
 *   <li><b>Debet/Credit ({@code debetCredit}):</b> Normal saldo akun: 1 = Debet, -1 = Credit.
 *       Menentukan arah positif saldo akun ini.</li>
 *   <li><b>Grup Akun ({@code grupAkun}):</b> Kategori akun (Aktiva, Kewajiban, Modal, dll.).</li>
 *   <li><b>Aktifitas:</b> Klasifikasi arus kas: Operasi, Investasi, atau Pendanaan.</li>
 *   <li><b>Parent:</b> Akun induk dalam hierarki; null untuk akun root.</li>
 *   <li><b>Satuan Kerja:</b> Jika diisi, akun ini khusus untuk unit organisasi tertentu.</li>
 *   <li><b>Bank, Atas Nama, No. Rekening:</b> Untuk akun yang merepresentasikan rekening bank.</li>
 * </ul>
 *
 * <p><b>Fitur utama:</b></p>
 * <ul>
 *   <li>Tampilan pohon hierarki akun dengan tombol tambah anak, copy, ubah, dan hapus per node.</li>
 *   <li>Grid pencarian dengan filter kode, nama, aktifitas, dan satuan kerja.</li>
 *   <li>Download data semua akun ke file Excel (xlsx) secara asinkron.</li>
 *   <li>Upload massal akun dari file Excel dengan resolusi parent otomatis (mencoba
 *       memotong kode 2-10 karakter dari belakang untuk menemukan parent yang cocok).</li>
 *   <li>Validasi keunikan kode akun sebelum penyimpanan.</li>
 *   <li>Tombol addAkun (tambah akun ke pohon) dan add (tambah akun ke grid).</li>
 * </ul>
 *
 * <p><b>Cara kerja — dua tampilan berbeda:</b><br>
 * Halaman ini memiliki dua cara melihat data akun. Tampilan pohon ({@code Tree}) menampilkan
 * hierarki lengkap dengan tombol aksi per node. Tampilan grid ({@code MyGrid}) menampilkan
 * hasil pencarian flat dengan paging. Keduanya beroperasi independen: perubahan di pohon
 * (tambah/ubah/hapus) me-reload pohon via {@code onReloadTree}, sementara perubahan dari
 * grid me-refresh grid via {@code onSearchDefault}.</p>
 *
 * <p><b>Threading:</b><br>
 * Download data Excel dilakukan dalam Thread terpisah untuk mencegah pembekuan UI.
 * Thread membuat file Excel via Apache POI (XSSF), progress dilaporkan via Label dan
 * ZK Timer setiap 200ms. Upload Excel dilakukan di thread utama ZK (tidak dipisah)
 * menggunakan {@code HibernateUtil.currentNativeSession()} per baris dengan commit
 * dan disconnect per iterasi untuk menghindari masalah memori.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Field {@code akunLenght} (perhatikan typo: seharusnya "akunLength") dibaca dari
 * system property {@code akun_lenght} dan digunakan saat membuat kode default akun anak
 * (menambahkan digit nol sebanyak akunLenght karakter). Default adalah 2. Untuk
 * menambah field baru pada entitas Akun, perlu memperbarui: formulir di {@code init()},
 * logika simpan di {@code onSave()}, header kolom download di {@code doAfterCompose()},
 * dan logika upload Excel. Kelas ini tidak mengimplementasikan {@code DataCriteria} dan
 * {@code DataSearchDefault} secara formal via interface, namun memiliki implementasi
 * sendiri untuk {@code onSearchDefault}.</p>
 *
 * @see Akun
 * @see GrupAkun
 * @see AkunTreeModel
 * @see AkunDao
 */
public class AkunAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas oleh framework ZK.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal yang menampilkan formulir tambah/ubah Akun. */
	private MyWindow addWindow;

	/** Grid yang menampilkan hasil pencarian akun dalam tampilan flat/tabular. */
	private MyGrid grid;

	/** Komponen pohon yang menampilkan hierarki akun secara visual. */
	private Tree tree;

	/** Kotak teks filter pencarian berdasarkan nama akun. */
	private Textbox searchnama;

	/** Kotak teks filter pencarian berdasarkan kode akun. */
	private Textbox searchkode;

	/** Kotak teks filter pencarian berdasarkan aktifitas (Operasi/Investasi/Pendanaan). */
	private Textbox searchaktifitas;

	/** Komponen banbox untuk memfilter akun berdasarkan satuan kerja tertentu. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Kotak teks input nama akun di formulir tambah/ubah. */
	private Textbox nama;

	/** Kotak teks input keterangan/deskripsi akun di formulir. */
	private Textbox keterangan;

	/** Kotak teks input kode akun di formulir tambah/ubah. */
	private Textbox kode;

	/** Combobox pilihan normal saldo: Debet atau Credit. */
	private Combobox debetCredit;

	/** Komponen banbox untuk memilih akun induk (parent) dalam hierarki. */
	private AmbilDataAkunBanbox parent;

	/** Combobox pilihan Grup Akun (kategori akun dalam COA). */
	private Combobox grupAkun;

	/** Hak akses update; jika false, tombol edit disembunyikan. */
	private boolean edit = false;

	/** Hak akses delete; jika false, tombol hapus disembunyikan. */
	private boolean delete = false;

	/** Entitas Akun yang sedang aktif diedit di formulir. */
	private Akun akun;

	/** Tombol toolbar untuk menambah Akun baru ke tampilan grid. */
	private MyToolbarbuttonConfig add;

	/** Tombol toolbar untuk menambah Akun baru ke tampilan pohon. */
	private MyToolbarbuttonConfig addAkun;

	/** Model data pohon akun yang membangun struktur hierarki dari database. */
	private AkunTreeModel akunTreeModel;

	/**
	 * Panjang digit tambahan saat membuat kode akun anak dari pohon.
	 * Dibaca dari system property {@code akun_lenght}; default 2.
	 * Contoh: akun induk "1.1" dengan akunLenght=2 → kode anak default "1.100".
	 */
	private Integer akunLenght = 2;

	/** Panel tab untuk memuat halaman jenis_transaksi.zul secara lazy. */
	private Tabpanel jenisTransaksi;

	/** Komponen banbox untuk memilih satuan kerja khusus akun di formulir. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Menampilkan halaman Jenis Transaksi secara lazy saat tab dibuka.
	 *
	 * <p><b>Tujuan:</b> Event handler yang dipanggil saat pengguna membuka tab Jenis Transaksi.
	 * Mengimplementasikan pola lazy loading untuk menghindari pemuatan halaman yang tidak
	 * diperlukan saat inisialisasi awal.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah panel tab sudah memiliki children. Jika belum,
	 * membuat MyWindow tanpa dekorasi dan menyematkan MyInclude yang memuat halaman
	 * {@code /pages/master/akunting/jenis_transaksi.zul}. Pemuatan hanya dilakukan sekali —
	 * pada kunjungan tab berikutnya, children sudah ada sehingga tidak dimuat ulang.</p>
	 *
	 * @param event Event ZK dari klik/pilih tab; tidak digunakan langsung.
	 */
	public void onJenisTransaksi(Event event) {
		if (jenisTransaksi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisTransaksi);
			MyInclude iframe = new MyInclude("/pages/master/akunting/jenis_transaksi.zul");
			iframe.setParent(window);
		}
	}

	/** Panel tab untuk memuat halaman bank.zul secara lazy. */
	private Tabpanel banktab;

	/** Combobox pilihan Bank untuk akun yang merepresentasikan rekening bank. */
	private Combobox bank;

	/** Kotak teks nama pemilik rekening bank. */
	private Textbox atasNama;

	/** Kotak teks nomor rekening bank. */
	private Textbox noRek;

	/** Combobox pilihan aktifitas arus kas untuk akun. */
	private Combobox aktifitas;

	/** Model pohon satuan kerja untuk filter pencarian berdasarkan unit organisasi. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Menampilkan halaman Bank secara lazy saat tab dibuka.
	 *
	 * <p><b>Tujuan:</b> Event handler yang dipanggil saat pengguna membuka tab Bank.
	 * Menggunakan pola lazy loading yang sama dengan {@code onJenisTransaksi}.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah panel sudah memiliki children. Jika belum,
	 * membuat window tanpa dekorasi dan menyematkan MyInclude untuk {@code /pages/master/bank.zul}.
	 * Pengguna dapat melihat dan mengelola data bank dari tab ini tanpa meninggalkan halaman.</p>
	 *
	 * @param event Event ZK dari klik/pilih tab Bank; tidak digunakan langsung.
	 */
	public void onBank(Event event) {
		if (banktab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(banktab);
			MyInclude iframe = new MyInclude("/pages/master/bank.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * Memeriksa keamanan sebelum komposisi halaman dimulai oleh ZK framework.
	 *
	 * <p><b>Tujuan:</b> Memastikan pengguna telah melewati pemeriksaan keamanan sistem
	 * sebelum komponen ZK apa pun diinisialisasi. Ini adalah titik masuk pertama dalam
	 * siklus hidup ZK Composer untuk kelas ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk verifikasi
	 * sesi dan hak akses dasar, kemudian mendelegasikan ke super class ZK.</p>
	 *
	 * @param page     Halaman ZK yang sedang dikomposisikan.
	 * @param parent   Komponen induk dalam hierarki ZK.
	 * @param compInfo Informasi metadata komponen dari ZUL.
	 * @return Objek ComponentInfo dari super class.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman Chart of Accounts setelah seluruh komponen ZUL selesai dikomposisikan.
	 *
	 * <p><b>Tujuan:</b> Titik inisialisasi utama halaman master akun. Melakukan semua setup
	 * awal: konfigurasi filter dan toolbar, pemuatan pohon hierarki, pemuatan grid,
	 * dan penyiapan fitur download/upload massal.</p>
	 *
	 * <p><b>Cara kerja secara terurut:</b></p>
	 * <ol>
	 *   <li>Mengatur event listener pada {@code searchparent}: setiap perubahan satuan kerja
	 *       memperbarui grid pencarian secara otomatis.</li>
	 *   <li>Membuat {@code SatuanKerjaTreeModel} untuk filter hierarki satuan kerja.</li>
	 *   <li>Membaca system property {@code akun_lenght} untuk konfigurasi panjang kode anak.</li>
	 *   <li>Membuat combobox {@code debetCredit} secara programatik dengan dua pilihan:
	 *       "Debet" (nilai {@code Akun.DEBET}) dan "Credit" (nilai {@code Akun.CREDIT}).</li>
	 *   <li>Mengatur visibilitas tombol tambah berdasarkan hak akses CREATE.</li>
	 *   <li>Mengatur hak akses edit dan delete dari {@code CommonPrivilages}.</li>
	 *   <li>Memanggil {@code onReloadTree} untuk memuat pohon hierarki akun pertama kali.</li>
	 *   <li>Memanggil {@code onSearchDefault} untuk memuat grid pencarian pertama kali.</li>
	 *   <li>Menambahkan tombol "Download Data" ke toolbar. Tombol ini memulai proses download
	 *       asinkron: membuat file Excel di thread terpisah, menampilkan progress via Timer
	 *       dan Clients.showBusy, lalu membuka window pratinjau Excel (ZK Spreadsheet)
	 *       dengan tombol download file. Thread mengekspor kolom: ID, Kode, Nama, Parent,
	 *       Tipe (D/K), Jenis (GrupAkun), Aktifitas, Satker, Bank, Atas Nama, No. Rek.</li>
	 *   <li>Menambahkan tombol "Upload" ke toolbar untuk impor massal dari Excel.
	 *       Upload dilakukan di thread ZK utama dengan pemrosesan per-baris menggunakan
	 *       native session terpisah per baris. Resolusi parent dilakukan dengan mencoba
	 *       memotong kode dari belakang (2 sampai 10 karakter) hingga menemukan akun induk.
	 *       GrupAkun dibuat otomatis jika belum ada.</li>
	 * </ol>
	 *
	 * <p><b>Logika resolusi parent saat upload:</b><br>
	 * Untuk setiap baris Excel, sistem membaca kolom PARENT (kode akun induk). Jika tidak
	 * ditemukan, sistem mencoba menebak parent dengan memotong kode akun dari belakang
	 * sebanyak 2, 3, 4, ..., 10 karakter secara berurutan. Ini mendukung berbagai panjang
	 * digit level hierarki. Contoh: kode "1.1.01.001" → mencoba "1.1.01.0", "1.1.01.",
	 * "1.1.01", "1.1.0", "1.1.", "1.1", "1.", "1" dst.</p>
	 *
	 * <p><b>Penanganan error upload:</b> Exception per baris ditangkap dan ditampilkan
	 * via {@code Common.tampilErrorJikaAdmin} tanpa menghentikan proses baris berikutnya.
	 * Setelah semua baris diproses, dialog konfirmasi sukses ditampilkan.</p>
	 *
	 * @param comp Komponen root halaman ZK yang telah selesai dikomposisikan.
	 * @throws Exception Jika terjadi kesalahan inisialisasi atau akses database.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (System.getProperties().get("akun_lenght") != null) {
			akunLenght = Integer.parseInt(System.getProperties().get("akun_lenght").toString());
		}

		debetCredit = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Debet");
		if (comboitem != null) { comboitem.setValue(Akun.DEBET); }
		debetCredit.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Credit");
		if (comboitem != null) { comboitem.setValue(Akun.CREDIT); }
		debetCredit.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		if (addAkun != null) { addAkun.setVisible((add != null && add.isVisible())); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onReloadTree(null);
		onSearchDefault(null);

		MyToolbarbuttonConfig cetakToolbarbutton = new MyToolbarbuttonConfig("Download Data", "/img/print.png");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
		cetakToolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(7);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@SuppressWarnings("unchecked")
						@Override
						public void run() {

							try {

								List<Akun> data = HibernateUtil.currentSession().createCriteria(Akun.class)
										.add(searchnama.getValue().trim().isEmpty()
												? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
										.add(searchkode.getValue().trim().isEmpty()
												? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))
										.addOrder(Order.asc("kode")).setMaxResults(1048576).list();
								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("CETAK DATA");

								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);
								rowhead.createCell(0).setCellValue("ID");
								rowhead.createCell(1).setCellValue("KODE");
								rowhead.createCell(2).setCellValue("NAMA");
								rowhead.createCell(3).setCellValue("PARENT");
								rowhead.createCell(4).setCellValue("TIPE");
								rowhead.createCell(5).setCellValue("JENIS");
								rowhead.createCell(6).setCellValue("AKTIFITAS");
								rowhead.createCell(7).setCellValue("SATKER");

								rowhead.createCell(8).setCellValue("BANK");
								rowhead.createCell(9).setCellValue("ATAS NAMA");
								rowhead.createCell(10).setCellValue("NO. REK");

								if (!data.isEmpty()) {

									for (Akun o : data) {
										try {
											rowIndex++;
											if (o == null) {
												continue;
											}
											label.setValue("Sedang memproses data " + o.toString() + " ("
													+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
													+ " %)");

											XSSFRow row = sheet.createRow(rowIndex);
											row.createCell(0).setCellValue(o.getId());
											row.createCell(1).setCellValue(o.getKode());
											row.createCell(2).setCellValue(o.getNama());
											row.createCell(3)
													.setCellValue(o.getParent() == null ? "" : o.getParent().getKode());
											row.createCell(4).setCellValue(o.getDebetCredit() == null ? ""
													: (o.getDebetCredit().equals(1) ? "D" : "K"));
											row.createCell(5).setCellValue(
													o.getGrupAkun() == null ? "" : o.getGrupAkun().getNama());
											row.createCell(6).setCellValue(o.getAktifitas());
											row.createCell(7).setCellValue(
													o.getSatuanKerja() == null ? "" : o.getSatuanKerja().getKode());

											row.createCell(8)
													.setCellValue(o.getBank() == null ? "" : o.getBank().toString());
											row.createCell(9).setCellValue(o.getAtasNama());
											row.createCell(10).setCellValue(o.getNoRek());
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");
								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}

						}
					}).start();

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				final Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (media.getName().toLowerCase().endsWith("xlsx")) {

							InputStream inputStream = media.getStreamData();
							File file = new File(
									Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
							file.getParentFile().mkdirs();
							FileOutputStream fileOutputStream = new FileOutputStream(file);
							int c;
							while ((c = inputStream.read()) != -1) {
								fileOutputStream.write(c);
							}
							fileOutputStream.close();
							inputStream.close();

							XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
							XSSFSheet sheet = workbook.getSheetAt(0);

							String peringatan = "";
							final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Akun");
							for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
								try {

									Session session = HibernateUtil.currentNativeSession();

									String kode = Common.getSheetContentAsString(sheet, 1, i);
									String nama = Common.getSheetContentAsString(sheet, 2, i);
									String parent = Common.getSheetContentAsString(sheet, 3, i);
									String kredit = Common.getSheetContentAsString(sheet, 4, i);
									String grup = Common.getSheetContentAsString(sheet, 5, i);
									String aktifitas = Common.getSheetContentAsString(sheet, 6, i);
									String satker = Common.getSheetContentAsString(sheet, 7, i);

									if (nama != null && !nama.trim().isEmpty() && kode != null
											&& !kode.trim().isEmpty()) {
										Long id = Common.getSheetContentAsLong(sheet, 0, i);
										Akun akun = id == null || id.equals(-1L) ? null
												: (Akun) session.createCriteria(Akun.class).add(Restrictions.idEq(id))
														.uniqueResult();
										if (akun == null) {
											akun = (Akun) session.createCriteria(Akun.class)
													.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
										}

										SatuanKerja satuanKerja = satker == null || !Common.isNumber(satker) ? null
												: (SatuanKerja) session.createCriteria(SatuanKerja.class)
														.add(Restrictions.idEq(Long.parseLong(satker))).uniqueResult();
										if (satuanKerja == null && satker != null && !satker.trim().isEmpty()) {
											satuanKerja = (SatuanKerja) session.createCriteria(SatuanKerja.class)
													.add(Restrictions.ilike("kode", satker.trim(), MatchMode.EXACT))
													.setMaxResults(1).uniqueResult();
										}

										if (satuanKerja == null && satker != null && !satker.trim().isEmpty()) {
											satuanKerja = (SatuanKerja) session.createCriteria(SatuanKerja.class)
													.add(Restrictions.ilike("nama", satker.trim(), MatchMode.EXACT))
													.setMaxResults(1).uniqueResult();
										}

										Akun akunParent = (Akun) session.createCriteria(Akun.class)
												.add(Restrictions.eq("kode", parent)).setMaxResults(1).uniqueResult();

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 2);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 3);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 4);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 5);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 6);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 7);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 8);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 9);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										if (akunParent == null) {
											try {
												String sbs = kode.substring(0, kode.length() - 10);
												akunParent = (Akun) session.createCriteria(Akun.class)
														.add(Restrictions.eq("kode", sbs)).setMaxResults(1)
														.uniqueResult();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										GrupAkun grupAkun = (GrupAkun) session.createCriteria(GrupAkun.class)
												.add(Restrictions.ilike("nama", grup, MatchMode.EXACT)).setMaxResults(1)
												.uniqueResult();
										if (grupAkun == null) {
											grupAkun = new GrupAkun();
											grupAkun.setNama(grup);
											session.getTransaction().begin();
											session.saveOrUpdate(grupAkun);
											session.getTransaction().commit();
										}

										if ((grup == null || grup.isEmpty()) && akunParent != null) {
											grupAkun = akunParent.getGrupAkun();
										}

										Bank bank = (Bank) Common.getSheetContentAsObject(sheet, 7, i, Bank.class);

										String atasNama = Common.getSheetContentAsString(sheet, 8, i);
										String norek = Common.getSheetContentAsString(sheet, 9, i);

										if (akun == null) {
											akun = new Akun();
										}
										akun.setAtasNama(atasNama);
										akun.setNoRek(norek);
										akun.setBank(bank);
										akun.setNama(nama);
										akun.setAktifitas(aktifitas);
										akun.setKode(kode);
										akun.setParent(akunParent);
										akun.setGrupAkun(grupAkun);
										akun.setDebetCredit(
												kredit == null ? -1 : kredit.trim().equalsIgnoreCase("D") ? 1 : -1);
										akun.setSatuanKerja(satuanKerja);

										session.getTransaction().begin();
										session.saveOrUpdate(akun);
										session.getTransaction().commit();
										report.sukses(i, kode, nama);
									}
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();

								} catch (Exception e) {
									report.gagal(i, "baris-" + i, e, "Pastikan kode akun/asset valid dan tidak duplikat.");
									Common.tampilErrorJikaAdmin(e);
								}

							}

							try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) AkunAction laporan"); }
							MyMessageboxConfig.show(
									report.getRingkasan(),
									"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(null);
										}
									});

						} else {
							MyMessageboxConfig.show(
									"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
											+ media,
									"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						}
					}
				});
			}
		});
		Common.appendKeToolbar(upload, add, comp);

	}

	/**
	 * <h3>AkunRenderer — Renderer Baris Grid Akun dalam Tampilan Flat</h3>
	 *
	 * <p><b>Untuk apa:</b> Kelas inner yang merender setiap baris {@code Akun} dalam
	 * grid pencarian flat. Setiap baris menampilkan kode, nama, normal saldo, induk,
	 * keterangan, grup akun, aktifitas, informasi bank, dan tombol aksi.</p>
	 *
	 * <p><b>Cara kerja:</b> Untuk setiap objek Akun dalam ListModel, metode {@code render}
	 * dipanggil oleh ZK. Komponen dappend ke Row sesuai urutan kolom di ZUL:</p>
	 * <ul>
	 *   <li>Label kode akun.</li>
	 *   <li>Vbox dari RevisiHelper untuk tracking revisi, berisi nama akun.</li>
	 *   <li>Label "Debet" atau "Credit" dari nilai {@code debetCredit}.</li>
	 *   <li>Label nama akun induk (parent.getNama()).</li>
	 *   <li>Label keterangan akun.</li>
	 *   <li>Label nama grup akun.</li>
	 *   <li>Label aktifitas arus kas.</li>
	 *   <li>Vbox dengan tiga MyLabelKecil: nama bank, atas nama, nomor rekening.</li>
	 *   <li>Hbox toolbar dengan tombol Ubah dan Hapus.</li>
	 * </ul>
	 *
	 * <p><b>Tombol Hapus:</b> Hanya ditampilkan jika akun tidak memiliki anak
	 * ({@code akunTreeModel.getChildCount(akun) == 0}). Ini mencegah penghapusan
	 * akun induk yang masih memiliki anak, menjaga integritas hierarki.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan append komponen harus sesuai dengan kolom ZUL.
	 * Jika menambah kolom baru, tambahkan di akhir sebelum toolbar aksi.</p>
	 */
	class AkunRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data Akun ke dalam komponen Row ZK di grid pencarian.
		 *
		 * <p><b>Tujuan:</b> Mengisi satu baris grid dengan informasi lengkap akun dan
		 * kontrol interaktif untuk mengedit atau menghapus akun tersebut.</p>
		 *
		 * <p><b>Cara kerja:</b> Semua label informasi diappend secara berurutan ke Row.
		 * Tombol ubah memanggil {@code init(akun, true, eventListener)} dengan callback
		 * yang memanggil {@code onSearchDefault} setelah simpan. Tombol hapus menampilkan
		 * konfirmasi, kemudian memanggil {@code Common.refreshDelete} dan merefresh grid.
		 * Visibilitas tombol hapus dikontrol oleh kondisi akun tidak memiliki child.</p>
		 *
		 * <p><b>Penanganan error:</b> Exception saat hapus ditampilkan via dialog error.
		 * Biasanya disebabkan oleh relasi FK yang masih aktif (akun digunakan di transaksi).</p>
		 *
		 * @param arg0 Row ZK yang akan diisi komponen child.
		 * @param arg1 Objek {@code Akun} yang akan dirender.
		 * @throws Exception Jika terjadi kesalahan saat merender komponen UI.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Akun akun = (Akun) arg1;

			new Label(akun.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Akun.class, akun, akun.getNama()).setParent(arg0);

			new Label(
					akun.getDebetCredit() == null ? "" : akun.getDebetCredit().equals(Akun.DEBET) ? "Debet" : "Credit")
					.setParent(arg0);

			new Label(akun.getParent() == null ? "" : akun.getParent().getNama()).setParent(arg0);

			new Label(akun.getKeterangan()).setParent(arg0);

			new Label(akun.getGrupAkun() == null ? "" : akun.getGrupAkun().getNama()).setParent(arg0);

			new Label(akun.getAktifitas()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new MyLabelKecil(akun.getBank() == null ? "" : akun.getBank().getNama()).setParent(vbox);
			new MyLabelKecil(akun.getAtasNama()).setParent(vbox);
			new MyLabelKecil(akun.getNoRek()).setParent(vbox);

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
					init(akun, true, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && akunTreeModel.getChildCount(akun) == 0);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(akun);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	/**
	 * Menangani klik tombol tambah akun dari tampilan pohon.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk tombol {@code addAkun} di toolbar,
	 * yang membuka formulir akun baru dengan callback yang me-reload pohon setelah simpan.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat {@code Akun} baru, memanggil {@code init} dengan
	 * EventListener callback {@code onReloadTree}, lalu menampilkan window modal.
	 * Berbeda dari {@code onAdd} yang callback-nya memanggil {@code onSearchDefault}.</p>
	 *
	 * @param event Event ZK dari klik tombol addAkun.
	 * @throws Exception Jika terjadi kesalahan membangun formulir.
	 */
	public void onAddAkun(Event event) throws Exception {
		init(new Akun(), false, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menangani klik tombol tambah akun dari tampilan grid.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk tombol {@code add} di toolbar,
	 * yang membuka formulir akun baru dengan callback yang me-refresh grid setelah simpan.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat {@code Akun} baru, memanggil {@code init} dengan
	 * EventListener callback {@code onSearchDefault}, lalu menampilkan window modal.
	 * Setelah simpan, grid diperbarui tetapi pohon tidak di-reload (performa lebih baik).</p>
	 *
	 * @param event Event ZK dari klik tombol add.
	 * @throws Exception Jika terjadi kesalahan membangun formulir.
	 */
	public void onAdd(Event event) throws Exception {
		init(new Akun(), false, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun dan menampilkan formulir modal untuk menambah atau mengubah data Akun.
	 *
	 * <p><b>Tujuan:</b> Metode private sentral yang membangun semua komponen formulir Akun
	 * dalam window modal. Dapat dipanggil dari berbagai konteks: tambah baru dari grid,
	 * tambah baru dari pohon, tambah anak dari node pohon, copy node, atau edit existing.</p>
	 *
	 * <p><b>Cara kerja — field formulir yang dibangun:</b></p>
	 * <ul>
	 *   <li><b>Kode Akun *:</b> Textbox yang dapat diedit; diisi dengan kode yang sudah ada
	 *       atau kode default yang dibuat dari kode parent + digit nol.</li>
	 *   <li><b>Nama Akun *:</b> Textbox nama deskriptif akun.</li>
	 *   <li><b>Nilai Akun:</b> Combobox debetCredit (Debet/Credit), dipilih dari nilai
	 *       yang sudah ada pada akun.</li>
	 *   <li><b>Grup Akun:</b> Combobox dari tabel GrupAkun; termasuk pilihan "Semua" (null).</li>
	 *   <li><b>Aktifitas:</b> Combobox dengan pilihan null, Operasi, Investasi, Pendanaan.
	 *       Diset readonly karena nilainya terbatas.</li>
	 *   <li><b>Induk:</b> AmbilDataAkunBanbox untuk memilih akun induk via popup pencarian.
	 *       Diisi dengan akun parent yang sudah ada.</li>
	 *   <li><b>Khusus Untuk Satuan Kerja:</b> AmbilDataSatuanKerjaBanbox; opsional, untuk
	 *       membatasi penggunaan akun pada unit organisasi tertentu.</li>
	 *   <li><b>Bank:</b> Combobox dari tabel Bank (hanya yang aktif), termasuk pilihan
	 *       "=Bukan Bank=" (null). EventListener onChange menyembunyikan/menampilkan No. Rek.</li>
	 *   <li><b>Atas Nama:</b> Textbox nama pemilik rekening bank.</li>
	 *   <li><b>No. Rekening:</b> Textbox nomor rekening; hanya terlihat saat bank dipilih.</li>
	 *   <li><b>Keterangan:</b> Textbox multiline 4 baris untuk catatan tambahan.</li>
	 * </ul>
	 *
	 * <p><b>Tombol:</b> Batal menutup window ({@code setVisible(false)}); Simpan memanggil
	 * {@code onSave}, lalu jika berhasil memanggil {@code eventListener.onEvent} dengan
	 * data akun yang disimpan, kemudian menutup window.</p>
	 *
	 * <p><b>Parameter {@code isedit}:</b> Parameter ini diterima tetapi tidak digunakan
	 * dalam logika formulir saat ini. Mungkin dimaksudkan untuk membedakan mode tambah
	 * vs edit di masa depan.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dilempar ke pemanggil.</p>
	 *
	 * @param akun          Entitas Akun yang akan ditampilkan di formulir. ID null untuk baru,
	 *                      ID tidak null untuk edit.
	 * @param isedit        Flag yang menandai mode edit (true) atau tambah (false).
	 *                      Saat ini tidak mempengaruhi tampilan formulir.
	 * @param eventListener Callback yang dipanggil setelah simpan berhasil, dengan data akun
	 *                      sebagai event data. Biasanya memanggil onSearchDefault atau onReloadTree.
	 * @throws Exception Jika terjadi kesalahan membangun komponen UI atau mengakses database.
	 */
	private void init(Akun akun, Boolean isedit, final EventListener eventListener) throws Exception {
		this.akun = akun;
		addWindow.setTitle(akun.getId() == null ? "Tambah Akun" : "Ubah Akun");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Akun *"));
		String myCode = akun.getKode() == null ? "" : akun.getKode();
		row.appendChild(kode = new Textbox(myCode));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akun *"));
		row.appendChild(nama = new Textbox(akun.getNama() == null ? "" : akun.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Akun"));
		row.appendChild(debetCredit);
		Common.selectComboItem(debetCredit, akun.getDebetCredit());
		debetCredit.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Akun"));
		grupAkun = new Combobox();
		Common.insertComboDanSemua(grupAkun, "nama", GrupAkun.class);
		row.appendChild(grupAkun);
		Common.selectComboItem(true, grupAkun, akun.getGrupAkun());
		grupAkun.setWidth("90%");

		aktifitas = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktifitas"));
		row.appendChild(aktifitas);
		Comboitem comboitem = new Comboitem("Tanpa Aktifitas");
		comboitem.setValue(null);
		aktifitas.appendChild(comboitem);
		for (String s : new String[] { Akun.OPERASI, Akun.INVESTASI, Akun.PENDANAAN }) {
			comboitem = new Comboitem(s);
			comboitem.setValue(s);
			aktifitas.appendChild(comboitem);
		}
		Common.selectComboItem(true, aktifitas, akun.getAktifitas());
		aktifitas.setWidth("90%");
		aktifitas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk"));
		row.appendChild(parent = new AmbilDataAkunBanbox(true));
		parent.setValue(akun.getParent() == null ? "" : akun.getParent().toString());
		parent.setAttribute("akun", akun.getParent());
		parent.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus Untuk Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false));
		satuanKerja.setValue(akun.getSatuanKerja() == null ? "" : akun.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja", akun.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bank"));
		row.appendChild(bank = new Combobox());
		Common.insertComboDanSemua(bank, new String[] { "nama" }, "keterangan", Bank.class, "=Bukan Bank=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bank, akun.getBank());
		bank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atas Nama"));
		row.appendChild(atasNama = new Textbox(akun.getAtasNama()));
		atasNama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Rekening"));
		row.appendChild(noRek = new Textbox(akun.getNoRek()));
		noRek.setWidth("90%");

		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Bank bankData = (Bank) (bank.getSelectedItem() == null ? null : bank.getSelectedItem().getValue());

				noRek.getParent().setVisible(bankData != null);

			}
		};

		bank.addEventListener("onChange", eventListener2);
		eventListener2.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(akun.getKeterangan() == null ? "" : akun.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					eventListener.onEvent(new Event("", null, AkunAction.this.akun));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Memvalidasi input formulir dan menyimpan data Akun ke database.
	 *
	 * <p><b>Tujuan:</b> Metode inti penyimpanan Akun. Dipanggil saat pengguna mengklik
	 * tombol Simpan di formulir modal. Menangani validasi, pemeriksaan duplikasi kode,
	 * dan penyimpanan/pembaruan data akun.</p>
	 *
	 * <p><b>Cara kerja — validasi (berurutan, berhenti pada validasi pertama yang gagal):</b></p>
	 * <ol>
	 *   <li>Kode Akun tidak boleh kosong (setelah trim).</li>
	 *   <li>Nama Akun tidak boleh kosong (setelah trim).</li>
	 *   <li>Debet/Credit harus dipilih (tidak null).</li>
	 *   <li>Grup Akun harus dipilih (tidak null).</li>
	 *   <li>Kode tidak boleh duplikat via {@code checkNamaAkun()}: memanggil query count
	 *       yang memeriksa apakah kode sudah ada di database (mengecualikan ID akun saat ini
	 *       jika sedang edit agar tidak memblokir simpan tanpa perubahan kode).</li>
	 * </ol>
	 *
	 * <p><b>Cara kerja — penyimpanan:</b></p>
	 * <ol>
	 *   <li>Memuat ulang entitas dari AkunDao jika ID sudah ada.</li>
	 *   <li>Mengisi semua field: grupAkun, debetCredit, kode, nama, keterangan,
	 *       parent (dari attribute "akun" pada banbox), satuanKerja, aktifitas,
	 *       bank, atasNama, noRek.</li>
	 *   <li>Melakukan update atau insert via {@code AkunDao}.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Validasi menampilkan dialog EXCLAMATION dan mengembalikan
	 * false. Exception DAO dilempar ke pemanggil.</p>
	 *
	 * @param event Event ZK dari klik tombol; tidak digunakan langsung.
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan database atau pemrosesan DAO.
	 */
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Akun belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Kode Akun dengan kode akun yang valid; (2) Pastikan kode tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Akun belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama Akun dengan nama yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (debetCredit.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Debet / Credit belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis posisi akun (Debet atau Credit) dari dropdown; (2) Pastikan salah satu opsi telah terpilih; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (grupAkun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Grup Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Grup Akun dari dropdown yang tersedia; (2) Pastikan grup akun yang sesuai sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaAkun();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Akun sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan kode akun yang berbeda dan belum terdaftar; (2) Periksa daftar akun yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan kode baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		AkunDao akunDao = DaoFactory.getInstance().getAkunDao();
		if (akun.getId() != null) {
			akun = akunDao.load(akun.getId());
		}

		akun.setGrupAkun((GrupAkun) grupAkun.getSelectedItem().getValue());
		akun.setDebetCredit((Integer) debetCredit.getSelectedItem().getValue());
		akun.setKode(kode.getValue().trim());
		akun.setNama(nama.getValue());
		akun.setKeterangan(keterangan.getValue());
		akun.setParent((Akun) parent.getAttribute("akun"));
		akun.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		akun.setAktifitas(
				(String) (aktifitas.getSelectedItem() == null ? null : aktifitas.getSelectedItem().getValue()));
		akun.setBank((Bank) (bank.getSelectedItem() == null ? null : bank.getSelectedItem().getValue()));
		akun.setAtasNama(atasNama.getValue());
		akun.setNoRek(noRek.getValue());

		if (akun.getId() != null) {
			akunDao.update(akun);
		} else {
			akunDao.save(akun);
		}
		return true;
	}

	/**
	 * Memuat ulang seluruh pohon hierarki akun dari database dan menampilkannya di Tree.
	 *
	 * <p><b>Tujuan:</b> Me-rebuild tampilan pohon akun dari awal. Dipanggil saat inisialisasi
	 * halaman dan setelah operasi CRUD yang mengubah struktur hierarki (tambah node baru dari
	 * pohon, tambah node anak, hapus node dari pohon).</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Membuat instance {@code AkunTreeModel} baru yang memuat hierarki dari database.</li>
	 *   <li>Mengatur model ke komponen {@code Tree} via {@code setModel}.</li>
	 *   <li>Mengatur item renderer dengan anonymous class yang meng-extend
	 *       {@code ais.ui.util.MyTreeitemRenderer}.</li>
	 * </ol>
	 *
	 * <p><b>Renderer pohon — untuk setiap Treeitem:</b></p>
	 * <ul>
	 *   <li>Treerow dengan Treecell-Treecell berisi: kode-nama-satker, debet/credit,
	 *       keterangan, grup akun, aktifitas, dan toolbar aksi.</li>
	 *   <li>Toolbar aksi per node berisi 4 tombol:
	 *     <ul>
	 *       <li><b>Tambah anak:</b> Membuat Akun baru dengan parent = akun ini, kode default
	 *           = kode akun ini + string "0" sebanyak {@code akunLenght} karakter.
	 *           Callback: {@code reloadTreeitem(treeitem)} untuk me-reload sub-pohon.</li>
	 *       <li><b>Copy:</b> Membuat Akun baru dengan data yang sama (clone), kode sama
	 *           (perlu diubah pengguna). Callback: reloadTreeitem.</li>
	 *       <li><b>Ubah:</b> Edit akun yang ada. Callback: reloadTreeitem.</li>
	 *       <li><b>Hapus:</b> Konfirmasi dulu, lalu deleteRefresh dan detach Treeitem.
	 *           Hanya tampil jika akun tidak memiliki anak.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <p><b>Penanganan error:</b> Exception saat rendering ditangkap per-item dan ditampilkan
	 * via {@code Common.tampilErrorJikaAdmin} tanpa menghentikan rendering item lain.</p>
	 *
	 * @param event Event ZK pemicu; boleh null jika dipanggil programatik.
	 */
	public void onReloadTree(Event event) {
		akunTreeModel = new AkunTreeModel();
		tree.setModel(akunTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Akun akun = (Akun) arg1;

				try {
					Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(akun.getKode() + " - " + akun.getNama()
							+ (akun.getSatuanKerja() == null ? "" : " - " + akun.getSatuanKerja().getNama()))
							.setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(akun.getDebetCredit() == null ? ""
							: akun.getDebetCredit().equals(Akun.DEBET) ? "Debet" : "Credit").setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(akun.getKeterangan()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(akun.getGrupAkun() == null ? "" : akun.getGrupAkun().getNama()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(akun.getAktifitas()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
					// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
					final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
							new java.util.ArrayList<org.zkoss.zk.ui.Component>();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							akunLenght = Integer.parseInt(System.getProperties().get("akun_lenght").toString());

							String ss = "";
							for (int i = 0; i < akunLenght; i++) {
								ss += "0";
							}

							Akun myakun = (Akun) akun.clone();
							myakun.setId(null);
							myakun.setParent(akun);
							myakun.setKode(akun.getKode() + ss);
							init(myakun, false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem);
								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					aksiButtons.add(button);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
					button.setTooltiptext("Copy Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Akun myakun = (Akun) akun.clone();
							myakun.setId(null);
							myakun.setKode(akun.getKode());
							init(myakun, false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem);
								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					aksiButtons.add(button);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(akun, true, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem);

								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					aksiButtons.add(button);

					button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete && akunTreeModel.getChildCount(akun) == 0);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													Common.refreshDelete((akun));

													treeitem.detach();

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(
															"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																	+ e.getMessage());
												}

											}

										}
									});

						}
					});
					aksiButtons.add(button);

					ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	/**
	 * Me-reload sub-pohon mulai dari node induk setelah terjadi perubahan di node anak.
	 *
	 * <p><b>Tujuan:</b> Memperbarui tampilan pohon secara efisien setelah tambah, copy,
	 * ubah, atau hapus node akun. Alih-alih me-reload seluruh pohon (mahal untuk pohon besar),
	 * metode ini hanya me-reload sub-pohon yang relevan.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ul>
	 *   <li>Mencari Treeitem parent dari treeitem yang diberikan via {@code getParentItem()}.</li>
	 *   <li>Jika tidak ada parent (node root), memanggil {@code onReloadTree(null)}
	 *       untuk reload seluruh pohon.</li>
	 *   <li>Jika ada parent, memanggil {@code treeitemParent.unload()} untuk melepas
	 *       semua children dari DOM tanpa menghapus model.</li>
	 *   <li>Membuat Timer 200ms yang pada tick pertama membuka kembali treeitemParent
	 *       dan treeitem via {@code setOpen(true)}, kemudian menghentikan dirinya sendiri.</li>
	 *   <li>Memanggil {@code setOpen(true)} memicu ZK untuk memuat ulang children
	 *       dari model secara lazy, sehingga data terbaru dari database tampil.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan error:</b> Exception saat reload root ditangkap dan ditampilkan
	 * via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * @param treeitem Treeitem yang baru saja mengalami perubahan data. Digunakan untuk
	 *                 menentukan parent yang perlu di-reload.
	 */
	private void reloadTreeitem(final Treeitem treeitem) {
		final Treeitem treeitemParent = treeitem.getParentItem();
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(200);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					timer.detach();
				}
			});

			timer.start();
		}
	}

	/**
	 * Memperbarui tampilan grid pencarian akun berdasarkan filter yang aktif saat ini.
	 *
	 * <p><b>Tujuan:</b> Memuat daftar akun yang sesuai kriteria pencarian ke dalam grid flat.
	 * Dipanggil saat inisialisasi halaman, setelah simpan/hapus dari grid, dan saat filter
	 * pencarian berubah (termasuk perubahan satuan kerja via {@code searchparent}).</p>
	 *
	 * <p><b>Cara kerja — filter yang diterapkan:</b></p>
	 * <ul>
	 *   <li><b>Satuan kerja:</b> Hierarki satuan kerja dari {@code searchparent}. Akun
	 *       dengan satuanKerja null selalu tampil; akun dengan satuanKerja difilter ke
	 *       set yang mencakup parent dan semua turunannya.</li>
	 *   <li><b>Nama:</b> ILIKE anywhere jika {@code searchnama} tidak kosong.</li>
	 *   <li><b>Kode:</b> ILIKE anywhere jika {@code searchkode} tidak kosong.</li>
	 *   <li><b>Aktifitas:</b> ILIKE anywhere jika {@code searchaktifitas} tidak kosong.</li>
	 * </ul>
	 *
	 * <p><b>Urutan:</b> Ascending berdasarkan kode akun (sort abjad/numerik sesuai kode).</p>
	 *
	 * <p><b>Limit:</b> Menggunakan {@code Common.MAX_RESULT} melalui {@code ConstantValues.simpleList}
	 * untuk membatasi jumlah baris dan mencegah overload memori.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate tidak ditangkap dan akan menyebar ke ZK.</p>
	 *
	 * @param event Event ZK pemicu; boleh null jika dipanggil programatik.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<Akun> akun = ConstantValues.simpleList(session.createCriteria(Akun.class).addOrder(Order.asc("kode"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))
				.add(searchaktifitas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("aktifitas", searchaktifitas.getValue(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT), Akun.class);

		ListModel strset = new SimpleListModel(akun);
		grid.setRowRenderer(new AkunRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah kode akun yang diinput sudah digunakan oleh akun lain di database.
	 *
	 * <p><b>Tujuan:</b> Validasi keunikan kode akun sebelum penyimpanan. Mencegah
	 * duplikasi kode yang akan melanggar integritas data COA dan menyebabkan kebingungan
	 * dalam pelaporan keuangan.</p>
	 *
	 * <p><b>Cara kerja:</b> Menjalankan query count ke tabel Akun dengan dua kondisi:
	 * kode harus sama persis dengan kode yang diinput (case sensitive via {@code eq}),
	 * dan ID harus berbeda dari ID akun yang sedang diedit (menggunakan {@code sqlRestriction("1=1")}
	 * sebagai pass-through jika akun baru, atau {@code Restrictions.ne("id", ...)} untuk edit).</p>
	 *
	 * <p><b>Logika return:</b> Mengembalikan {@code true} jika kode SUDAH ADA (duplikat),
	 * {@code false} jika kode BELUM ADA atau hanya ada pada akun yang sama (saat edit tanpa
	 * perubahan kode). Perhatikan return value ini adalah "ada duplikat?" bukan "boleh simpan?".</p>
	 *
	 * <p><b>Penanganan null:</b> {@code kotaCount} tidak akan null karena query count
	 * selalu mengembalikan nilai; namun tetap ada inisialisasi null untuk keamanan kompilasi.</p>
	 *
	 * @return {@code true} jika kode sudah digunakan akun lain (ada duplikat);
	 *         {@code false} jika kode aman untuk digunakan.
	 */
	public Boolean checkNamaAkun() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Akun.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.akun.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.akun.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
