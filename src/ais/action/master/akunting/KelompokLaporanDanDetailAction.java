package ais.action.master.akunting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Groupbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.akunting.helper.AmbilDataBanyakAkun;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.KelompokLaporanDao;
import ais.database.dao.akunting.KelompokLaporanPunyaAkunDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>KelompokLaporanDanDetailAction — Pengelola Kelompok Laporan Keuangan dan Akun Anggotanya</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK Composer yang menangani manajemen hierarki kelompok laporan keuangan
 * ({@code KelompokLaporan}) beserta daftar akun yang menjadi anggota setiap kelompok
 * ({@code KelompokLaporanPunyaAkun}). Dalam sistem akuntansi, laporan keuangan seperti
 * Neraca, Laporan Laba Rugi, dan Laporan Arus Kas membutuhkan pengelompokan akun-akun
 * Chart of Accounts (COA) ke dalam seksi-seksi tertentu. Kelas ini memungkinkan administrator
 * sistem mendefinisikan pengelompokan tersebut secara fleksibel.</p>
 *
 * <p><b>Struktur data yang dikelola:</b></p>
 * <ul>
 *   <li>{@code KelompokLaporan}: Header grup laporan dengan atribut Jenis Laporan (misalnya
 *       Neraca, L/R), Master Grup Laporan (Aktiva/Kewajiban/Pendapatan/dll), nomor urut tampil,
 *       tiga level sub-grup (keterangan, keterangan1, keterangan2), dan flag tampilkanAkunRinci.</li>
 *   <li>{@code KelompokLaporanPunyaAkun}: Relasi many-to-many antara KelompokLaporan dan Akun,
 *       dengan field nomor urut tampil akun dalam kelompok tersebut.</li>
 * </ul>
 *
 * <p><b>Cara kerja — tampilan dua lapis:</b><br>
 * Grid utama menampilkan daftar {@code KelompokLaporan}. Setiap baris grid dapat di-expand
 * (karena renderer menggunakan {@code KelompokLaporanPunyaAkunAction} yang merupakan
 * subkelas {@code MyDetail}) untuk menampilkan daftar akun yang masuk dalam kelompok
 * tersebut. Detail akun dapat dikelola langsung dari expansion baris grid tanpa harus
 * membuka halaman terpisah.</p>
 *
 * <p><b>Fitur utama:</b></p>
 * <ul>
 *   <li>CRUD KelompokLaporan melalui formulir modal.</li>
 *   <li>Tambah akun ke kelompok via dialog multi-pilih {@code AmbilDataBanyakAkun}.</li>
 *   <li>Upload akun massal dari file Excel (xlsx) per kelompok, dengan logika
 *       auto-create KelompokLaporan baru jika belum ada.</li>
 *   <li>Upload massal dari toolbar utama untuk seluruh kelompok sekaligus.</li>
 *   <li>Download data akun ke Excel dari toolbar utama.</li>
 *   <li>Penomoran urut akun dalam kelompok secara inline via Intbox di grid detail.</li>
 *   <li>Hapus semua akun dari kelompok sekaligus (tombol Bersihkan).</li>
 *   <li>Salin akun dari kelompok lain saat membuat KelompokLaporan baru (fitur copyDari).</li>
 * </ul>
 *
 * <p><b>Threading:</b><br>
 * Upload Excel dilakukan dalam Thread terpisah ({@code new Thread(new Runnable())}) untuk
 * menghindari pembekuan UI. Progress dilaporkan melalui mekanisme label dan ZK Timer yang
 * dipantau setiap 200ms. Thread upload menggunakan {@code HibernateUtil.currentNativeSession()}
 * karena session Hibernate per-request ZK tidak tersedia di thread yang berbeda. Setelah
 * selesai, thread menutup session dengan {@code HibernateUtil.closeSession()}.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan tiga interface: {@code DataCriteria} (dua metode: satu untuk
 * KelompokLaporan, satu untuk detail akun), {@code DataSearchDefault}, dan {@code DataInitDefault}.
 * Jika menambah level sub-grup baru, perlu menambah field di KelompokLaporan, baris di
 * {@code initKelompokLaporan()}, dan kolom di {@code KelompokLaporanRenderer}.
 * Perhatikan bahwa {@code KelompokLaporanPunyaAkunAction} adalah inner class yang memiliki
 * state sendiri (field {@code kelompokLaporan}, {@code grid}, dll.) dan meng-extend
 * {@code MyDetail} sehingga berfungsi sebagai baris yang dapat di-expand di grid.</p>
 *
 * @see KelompokLaporan
 * @see KelompokLaporanPunyaAkun
 * @see MasterGrupLaporan
 * @see JenisLaporan
 */
public class KelompokLaporanDanDetailAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Serial version UID untuk serialisasi kelas oleh framework ZK.
	 */
	private static final long serialVersionUID = -4117053895788369352L;

	/** Jendela modal yang digunakan untuk formulir tambah/ubah KelompokLaporan. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar KelompokLaporan dengan baris yang dapat di-expand. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan keterangan (Sub Grup) kelompok laporan. */
	private Textbox searchketerangan;

	/** Combobox filter berdasarkan Jenis Laporan (Neraca, Laporan L/R, dll.). */
	private Combobox searchJenisLaporan;

	/** Combobox filter berdasarkan objek MasterGrupLaporan (ID + nama + keterangan). */
	private Combobox searchMasterGrupLaporan;

	/** Combobox filter berdasarkan nama MasterGrupLaporan (Aktiva/Kewajiban/Pendapatan/dll.). */
	private Combobox searchNamaMasterGrupLaporan;

	/** Kotak input nomor urut tampil KelompokLaporan dalam formulir. */
	private Doublebox urut;

	/** Kotak teks Sub Grup I (keterangan) KelompokLaporan dalam formulir. */
	private Textbox keterangan;

	/** Combobox pilihan Jenis Laporan dalam formulir tambah/ubah KelompokLaporan. */
	private Combobox jenisLaporan;

	/** Combobox pilihan Master Grup Laporan dalam formulir tambah/ubah KelompokLaporan. */
	private Combobox masterGrupLaporan;

	/** Hak akses update dari CommonPrivilages; menentukan tombol edit tampil. */
	private boolean edit = false;

	/** Hak akses delete dari CommonPrivilages; menentukan tombol hapus tampil. */
	private boolean delete = false;

	/** Entitas KelompokLaporan yang sedang aktif diedit atau dikelola. */
	private KelompokLaporan kelompokLaporan;

	/** Tombol toolbar untuk menambah KelompokLaporan baru. */
	private MyToolbarbuttonConfig add;

	/** Panel tab yang memuat halaman jenis_laporan.zul saat tab Jenis Laporan dibuka. */
	private Tabpanel jenisLaporanTab;

	/** Checkbox untuk mengaktifkan tampilan rinci akun di laporan. */
	private MyCheckboxConfig tampilkanAkunRinci;

	/** Checkbox status aktif/non-aktif setup laporan. */
	private MyCheckboxConfig aktif;

	/** Kotak teks Sub Grup II (keterangan1) KelompokLaporan dalam formulir. */
	private Textbox keterangan1;

	/** Kotak teks Sub Grup III (keterangan2) KelompokLaporan dalam formulir. */
	private Textbox keterangan2;

	/**
	 * Menampilkan halaman Jenis Laporan secara lazy (hanya saat tab dibuka pertama kali).
	 *
	 * <p><b>Tujuan:</b> Event handler yang dipanggil saat pengguna mengklik tab "Jenis Laporan"
	 * di antarmuka. Implementasi lazy loading mencegah halaman ZUL dimuat sebelum diperlukan,
	 * menghemat waktu muat awal.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah tab panel sudah memiliki children. Jika belum,
	 * membuat {@code MyWindow} tanpa title dan dekorasi, memasangnya ke panel, lalu
	 * menyematkan {@code MyInclude} yang memuat file ZUL jenis_laporan.zul. Karena tidak
	 * ada guard {@code if(isOpen())}, pemuatan terjadi saat event apapun (tidak hanya saat
	 * expand).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pola yang sama digunakan untuk tab-tab lazy loading lainnya
	 * di sistem. Jika path ZUL berubah, perbarui string path di sini.</p>
	 *
	 * @param event Event ZK dari klik tab, tidak digunakan secara langsung di metode ini.
	 */
	public void onJenisLaporan(Event event) {

		if (jenisLaporanTab.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisLaporanTab);
			MyInclude iframe = new MyInclude("/pages/master/akunting/jenis_laporan.zul");
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
	 * sesi dan hak akses, kemudian mendelegasikan ke implementasi super class ZK.</p>
	 *
	 * @param page     Halaman ZK yang sedang dikomposisikan.
	 * @param parent   Komponen induk dalam hierarki komponen ZK.
	 * @param compInfo Informasi metadata komponen dari file ZUL.
	 * @return Objek {@code ComponentInfo} dari super class untuk melanjutkan komposisi.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman Kelompok Laporan setelah seluruh komponen ZUL selesai dikomposisikan.
	 *
	 * <p><b>Tujuan:</b> Titik inisialisasi utama halaman ini. Melakukan semua setup awal:
	 * pemeriksaan sesi dan hak akses, pengisian semua combobox filter, konfigurasi hak akses,
	 * penambahan tombol download dan upload ke toolbar, dan pemuatan data awal ke grid.</p>
	 *
	 * <p><b>Cara kerja secara terurut:</b></p>
	 * <ol>
	 *   <li>Memeriksa sesi dan hak akses READ; jika tidak ada, diarahkan ke logoff.</li>
	 *   <li>Mengisi {@code searchJenisLaporan} dengan semua data JenisLaporan dari database.</li>
	 *   <li>Mengisi {@code searchMasterGrupLaporan} dengan semua MasterGrupLaporan (nama+ID+ket).</li>
	 *   <li>Mengisi {@code searchNamaMasterGrupLaporan} dinamis: jika ada data MasterGrupLaporan
	 *       di DB, ambil distinct nama-nya; jika tidak ada, isi dengan nilai konstanta
	 *       (Aktiva, Kewajiban, Pendapatan) sebagai fallback.</li>
	 *   <li>Menambahkan item "Semua" dengan nilai null sebagai pilihan default filter nama grup.</li>
	 *   <li>Mengatur hak akses tombol tambah, edit, dan delete dari {@code CommonPrivilages}.</li>
	 *   <li>Menambahkan tombol "Download Akun" ke toolbar — tombol ini men-download data
	 *       dari {@code initCriteriaDetail} (bukan {@code initCriteria}) dalam format Excel.</li>
	 *   <li>Menambahkan tombol "Upload Akun" yang mendukung upload Excel untuk batch insert
	 *       KelompokLaporanPunyaAkun. Logic upload dilakukan di EventListener terpisah yang
	 *       juga menangani pembuatan KelompokLaporan baru jika belum ada.</li>
	 *   <li>Memanggil {@code onSearchDefault} untuk memuat data pertama kali.</li>
	 * </ol>
	 *
	 * <p><b>Upload massal via toolbar:</b> EventListener upload toolbar mendukung format file
	 * Excel dengan kolom: kelompokLaporan.masterGrupLaporan, kelompokLaporan.jenisLaporan,
	 * kelompokLaporan.keterangan, kelompokLaporan.keterangan1, kelompokLaporan.aktif, akun,
	 * nomorUrut. Jika KelompokLaporan dengan kombinasi tersebut belum ada, dibuat baru otomatis.
	 * Setelah upload selesai, dilakukan cleanup untuk menghapus baris dengan akun null.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika sesi tidak valid atau hak READ tidak terpenuhi,
	 * pengguna diarahkan ke logoff. Exception dalam upload listener ditampilkan via
	 * {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * @param comp Komponen root halaman ZK yang telah selesai dikomposisikan.
	 * @throws Exception Jika terjadi kesalahan inisialisasi atau akses database.
	 */
	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertComboDanSemua(searchJenisLaporan, "nama", JenisLaporan.class);

		Common.insertComboDanSemua(searchMasterGrupLaporan, new String[] { "nama", "id", "keterangan" }, "keterangan",
				MasterGrupLaporan.class);

		List<String> namas = HibernateUtil.currentSession().createCriteria(MasterGrupLaporan.class)
				.setProjection(Projections.groupProperty("nama")).list();
		if (!namas.isEmpty()) {
			for (String n : namas) {
				MyComboitemConfig comboitem = new MyComboitemConfig(n);
				comboitem.setValue(n);
				searchNamaMasterGrupLaporan.appendChild(comboitem);
			}
		} else {
			MyComboitemConfig comboitem = new MyComboitemConfig(MasterGrupLaporan.AKTIVA);
			comboitem.setValue(MasterGrupLaporan.AKTIVA);
			searchNamaMasterGrupLaporan.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.KEWAJIBAN);
			comboitem.setValue(MasterGrupLaporan.KEWAJIBAN);
			searchNamaMasterGrupLaporan.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.PENDAPATAN);
			comboitem.setValue(MasterGrupLaporan.PENDAPATAN);
			searchNamaMasterGrupLaporan.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchNamaMasterGrupLaporan.appendChild(comboitem);
		if (searchNamaMasterGrupLaporan != null) { searchNamaMasterGrupLaporan.setSelectedItem(comboitem); }
		if (searchNamaMasterGrupLaporan != null) { searchNamaMasterGrupLaporan.setReadonly(true); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		String[] contents = new String[] { "id", "kelompokLaporan.urut", "kelompokLaporan.jenisLaporan",
				"kelompokLaporan.masterGrupLaporan", "kelompokLaporan.aktif", "kelompokLaporan.tampilkanAkunRinci",
				"kelompokLaporan.keterangan", "kelompokLaporan.keterangan1", "kelompokLaporan.keterangan2", "akun",
				"nomorUrut" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				return initCriteriaDetail(order);
			}
		}, contents);
		if (cetakToolbarbutton != null) { cetakToolbarbutton.setLabel("Download Akun"); }
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokLaporanPunyaAkun.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				KelompokLaporanPunyaAkun detail = (KelompokLaporanPunyaAkun) data[0];
				Session session = (Session) data[1];
				@SuppressWarnings("rawtypes")
				Map datum = (Map) data[2];
				try {
					String masterGrupLaporan = (String) datum.get("kelompokLaporan.masterGrupLaporan");
					String jen = (String) datum.get("kelompokLaporan.jenisLaporan");

					String ket = (String) datum.get("kelompokLaporan.keterangan");
					if (ket == null) {
						ket = "";
					}

					String ket1 = (String) datum.get("kelompokLaporan.keterangan1");
					if (ket1 == null) {
						ket1 = "";
					}

					String aktif = (String) datum.get("kelompokLaporan.aktif");

					MasterGrupLaporan grupLaporan = (MasterGrupLaporan) Common.getContentAsObject(masterGrupLaporan,
							MasterGrupLaporan.class, null);

					JenisLaporan jenisLaporan = (JenisLaporan) Common.getContentAsObject(jen, JenisLaporan.class, null);

					System.out.println("grupLaporan -> " + grupLaporan);
					System.out.println("jenisLaporan -> " + jenisLaporan);

					System.out.println("ket -> " + ket);
					System.out.println("ket1 -> " + ket1);

					if (grupLaporan != null && jenisLaporan != null) {

						KelompokLaporan kelompokLaporan = (KelompokLaporan) session
								.createCriteria(KelompokLaporan.class)
								.add(Restrictions.ilike("keterangan", ket.trim(), MatchMode.EXACT))
								.add(Restrictions.ilike("keterangan1", ket1.trim(), MatchMode.EXACT))
								.add(Restrictions.eq("masterGrupLaporan", grupLaporan))
								.add(Restrictions.eq("jenisLaporan", jenisLaporan)).setMaxResults(1)
								.addOrder(Order.asc("id")).uniqueResult();

						if (kelompokLaporan == null) {

							kelompokLaporan = new KelompokLaporan();
							kelompokLaporan.setMasterGrupLaporan(grupLaporan);
							kelompokLaporan.setJenisLaporan(jenisLaporan);
							kelompokLaporan.setKeterangan(ket);
							kelompokLaporan.setKeterangan1(ket1);

							kelompokLaporan.setAktif(aktif == null || aktif.equalsIgnoreCase("true"));
							session.save(kelompokLaporan);
							session.flush();
						} else {
							kelompokLaporan.setKeterangan(ket);
							kelompokLaporan.setKeterangan1(ket1);
							kelompokLaporan.setAktif(aktif == null || aktif.equalsIgnoreCase("true"));
							session.update(kelompokLaporan);
							session.flush();
						}

						detail.setKelompokLaporan(kelompokLaporan);

					}

				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				HibernateUtil.currentSession().createSQLQuery(
						"delete from akunting.kelompok_laporan_punya_akun where akun is null or kelompok_laporan is null;")
						.executeUpdate();
			}

		}, contents);
		if (upload != null) { upload.setLabel("Upload Akun"); }
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		onSearchDefault(null);
	}

	/**
	 * <h3>KelompokLaporanPunyaAkunAction — Pengelola Akun per Kelompok Laporan</h3>
	 *
	 * <p><b>Untuk apa:</b> Kelas inner yang merupakan baris yang dapat di-expand
	 * ({@code MyDetail}) dalam grid utama. Setiap instance mengelola daftar akun
	 * ({@code KelompokLaporanPunyaAkun}) yang tergabung dalam satu {@code KelompokLaporan}
	 * tertentu. Saat baris di-expand (event ON_OPEN), konten detail dimuat secara lazy.</p>
	 *
	 * <p><b>Cara kerja:</b> Saat konstruktor dipanggil, combobox kelompok laporan diinisialisasi
	 * dan event listener ON_OPEN dipasang. Saat baris di-expand, {@code display()} dipanggil
	 * yang membangun seluruh UI detail: Groupbox dengan toolbar (pencarian, tambah, upload, bersihkan)
	 * dan grid akun dengan kolom (Kode, Nama, Keterangan, Nomor Urut, Aksi). Data diperbarui
	 * via {@code loadData(null)} yang menjalankan query criteria Hibernate.</p>
	 *
	 * <p><b>Fitur tambah akun:</b> Membuka dialog {@code AmbilDataBanyakAkun} yang menampilkan
	 * semua Akun dengan checkbox untuk memilih banyak sekaligus. Akun yang sudah ada di
	 * kelompok ini di-exclude dari pilihan. Setelah konfirmasi, akun ditambahkan via
	 * saveOrUpdate (upsert) per akun.</p>
	 *
	 * <p><b>Upload Excel per kelompok:</b> Membaca file Excel dengan kolom kode akun (0)
	 * dan nomor urut (1). Akun dicari via {@code Common.getSheetContentAsObject}. Jika
	 * sudah ada di kelompok ini, nomor urut diperbarui; jika belum, baris baru dibuat.
	 * Proses dilakukan di thread terpisah dengan progress ZK Timer.</p>
	 *
	 * <p><b>Threading:</b> Upload Excel berjalan di thread terpisah menggunakan
	 * {@code HibernateUtil.currentNativeSession()}. Session ditutup di akhir thread
	 * dengan {@code HibernateUtil.closeSession()}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Kelas ini memiliki referensi ke {@code edit} dan {@code delete}
	 * dari outer class untuk mengontrol visibilitas tombol. Jika menambah kolom baru,
	 * perbarui definisi {@code Columns} di {@code display()} dan renderer di
	 * {@code KelompokLaporanPunyaAkunRenderer}.</p>
	 */
	class KelompokLaporanPunyaAkunAction extends MyDetail implements DataCriteria, DataSearchDefault {

		/**
		 * Serial version UID untuk serialisasi kelas inner ini.
		 */
		private static final long serialVersionUID = 1L;

		/** Combobox pilihan kelompok laporan di formulir tambah/ubah detail akun. */
		private Combobox cboKelompokLaporan;

		/** Jendela modal untuk formulir tambah/ubah detail akun satu per satu. */
		private MyWindow addWindow;

		/** Grid yang menampilkan daftar akun dalam kelompok laporan ini. */
		private MyGrid grid;

		/** KelompokLaporan yang dikelola oleh instance ini. */
		private KelompokLaporan kelompokLaporan;

		/** Komponen banbox untuk memilih satu Akun di formulir detail. */
		private AmbilDataAkunBanbox akun;

		/** Entitas detail akun yang sedang diedit di formulir. */
		private KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun;

		/** Kotak teks pencarian akun berdasarkan kode atau nama di panel detail. */
		private Textbox cari;

		/**
		 * Membuat instance KelompokLaporanPunyaAkunAction untuk satu kelompok laporan.
		 *
		 * <p><b>Tujuan:</b> Menginisialisasi komponen baris expand-able yang mengelola
		 * daftar akun untuk satu kelompok laporan tertentu.</p>
		 *
		 * <p><b>Cara kerja:</b></p>
		 * <ol>
		 *   <li>Memanggil super konstruktor {@code MyDetail}.</li>
		 *   <li>Menyimpan referensi kelompok laporan.</li>
		 *   <li>Menginisialisasi combobox kelompok laporan dengan semua data yang ada.</li>
		 *   <li>Memasang event listener untuk event {@code Events.ON_OPEN}: saat baris
		 *       di-expand, konten dibersihkan dan {@code display()} dipanggil untuk
		 *       membangun UI detail secara lazy.</li>
		 * </ol>
		 *
		 * <p><b>Pemeliharaan:</b> Lazy loading memastikan UI detail tidak dibangun hingga
		 * pengguna benar-benar mengklik expand. Ini penting untuk performa grid dengan
		 * banyak baris.</p>
		 *
		 * @param kelompokLaporan KelompokLaporan yang akan dikelola oleh instance ini.
		 *                        Tidak boleh null.
		 */
		public KelompokLaporanPunyaAkunAction(KelompokLaporan kelompokLaporan) {
			super();
			this.kelompokLaporan = kelompokLaporan;
			Common.insertCombo(cboKelompokLaporan = new Combobox(), "keterangan", KelompokLaporan.class);
			this.addEventListener(Events.ON_OPEN, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(KelompokLaporanPunyaAkunAction.this);
					if (isOpen()) {
						display();
					}
				}
			});
		}

		/**
		 * Membangun seluruh UI detail akun dalam kelompok laporan ini.
		 *
		 * <p><b>Tujuan:</b> Dipanggil secara lazy saat baris di-expand pertama kali.
		 * Membangun semua komponen UI yang diperlukan untuk mengelola daftar akun dalam
		 * kelompok laporan: toolbar dengan berbagai aksi dan grid dengan data akun.</p>
		 *
		 * <p><b>Cara kerja — toolbar yang dibangun:</b></p>
		 * <ul>
		 *   <li><b>Pencarian:</b> Label "Akun:", Textbox, dan tombol search. Event onOK
		 *       pada Textbox juga memicu pencarian.</li>
		 *   <li><b>Tambah Akun:</b> Membuka dialog {@code AmbilDataBanyakAkun} yang
		 *       menampilkan akun yang belum ada di kelompok ini. Setelah konfirmasi, akun
		 *       ditambahkan dengan saveOrUpdate per akun.</li>
		 *   <li><b>Download:</b> Export data akun di kelompok ini ke Excel.</li>
		 *   <li><b>Upload Akun:</b> Upload dari Excel (kolom 0=kode akun, 1=nomor urut).
		 *       Dilakukan di thread terpisah dengan progress timer.</li>
		 *   <li><b>Bersihkan:</b> Dialog konfirmasi sebelum menghapus semua akun dari
		 *       kelompok ini via SQL native DELETE.</li>
		 * </ul>
		 *
		 * <p><b>Cara kerja — grid yang dibangun:</b></p>
		 * <ul>
		 *   <li>Mold "paging" dengan pageSize 300 (untuk kelompok dengan banyak akun).</li>
		 *   <li>Kolom: Kode Akun (20%), Nama Akun (30%), Keterangan Akun (40%),
		 *       Nomor Urut (10%), Aksi (10%).</li>
		 *   <li>Data dimuat via {@code loadData(null)}.</li>
		 * </ul>
		 *
		 * <p><b>Pemeliharaan:</b> Metode ini dipanggil setiap kali baris di-re-expand
		 * (setelah {@code Common.clear} membersihkan konten). Perubahan UI di sini
		 * akan langsung terlihat saat expand berikutnya.</p>
		 */
		private void display() {

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(this);
			groupbox.appendChild(new Caption("Daftar Akun"));

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(groupbox);

			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun:")));
			toolbar.appendChild(cari = new Textbox());
			cari.setCols(15);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}

			});
			button.setParent(toolbar);

			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}

			});

			button = new MyToolbarbuttonConfig("Tambah Akun", "/img/add_item.png");

			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Akun> akuns = HibernateUtil.currentSession()
									.createCriteria(KelompokLaporanPunyaAkun.class)
									.add(Restrictions.eq("kelompokLaporan", kelompokLaporan))
									.setProjection(Projections.property("akun")).list();

							AmbilDataBanyakAkun addWindow = new AmbilDataBanyakAkun(akuns, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									List<Akun> akuns = (List<Akun>) arg0.getData();

									for (Akun akun : akuns) {

										KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun = (KelompokLaporanPunyaAkun) session
												.createCriteria(KelompokLaporanPunyaAkun.class)
												.add(Restrictions.eq("akun", akun))
												.add(Restrictions.eq("kelompokLaporan", kelompokLaporan))
												.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
										if (kelompokLaporanPunyaAkun == null) {
											kelompokLaporanPunyaAkun = new KelompokLaporanPunyaAkun();
										}
										kelompokLaporanPunyaAkun.setAkun(akun);
										kelompokLaporanPunyaAkun.setKelompokLaporan(kelompokLaporan);
										session.save(kelompokLaporanPunyaAkun);
										session.flush();
									}
									loadData(null);
								}
							});

							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
							addWindow.setVisible(true);
							addWindow.onModal();
						}
					});
				}

			});
			button.setParent(toolbar);

			final String[] contents = new String[] { "akun", "nomorUrut" };

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Akun" + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
					if (media.getName().toLowerCase().endsWith("xlsx")) {

						InputStream inputStream = media.getStreamData();
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						file.getParentFile().mkdirs();
						FileOutputStream fileOutputStream = new FileOutputStream(file);
						int c;
						while ((c = inputStream.read()) != -1) {
							fileOutputStream.write(c);
						}
						fileOutputStream.close();
						inputStream.close();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								uploadDataAkun(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(arg0);
										Clients.clearBusy();
									}
								}, contents);
							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.show(
								"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
										+ media,
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				}
			});
			toolbar.appendChild(upload);

			button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											session.createSQLQuery(
													"delete from akunting.kelompok_laporan_punya_akun where kelompok_laporan = "
															+ kelompokLaporan.getId())
													.executeUpdate();

											loadData(null);

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
			button.setParent(toolbar);

			grid = new MyGrid();
			grid.setMold("paging");
			grid.setPageSize(300);
			grid.setParent(groupbox);

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kode Akun");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Akun");
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan Akun");
			column.setWidth("40%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nomor Urut");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");

			loadData(null);
		}

		/**
		 * Melakukan upload data akun dari file Excel ke kelompok laporan ini secara asinkron.
		 *
		 * <p><b>Tujuan:</b> Memproses file Excel yang berisi daftar kode akun dan nomor urut,
		 * kemudian menyimpan semua akun tersebut ke kelompok laporan ini. Proses dilakukan
		 * di thread terpisah agar UI tidak membeku saat file besar diproses.</p>
		 *
		 * <p><b>Cara kerja:</b></p>
		 * <ol>
		 *   <li>Membuat Label progress dan Timer ZK yang memantau progress setiap 200ms.</li>
		 *   <li>Menampilkan busy indicator via {@code Clients.showBusy}.</li>
		 *   <li>Membuat thread baru yang membuka file Excel via XSSF POI.</li>
		 *   <li>Untuk setiap baris (mulai baris ke-2, baris ke-1 adalah header):
		 *     <ul>
		 *       <li>Membaca objek Akun dari kolom 0 via {@code Common.getSheetContentAsObject}.</li>
		 *       <li>Membaca nomor urut dari kolom 1 via {@code Common.getSheetContentAsInteger}.</li>
		 *       <li>Mencari apakah sudah ada KelompokLaporanPunyaAkun untuk akun ini di kelompok ini.</li>
		 *       <li>Jika sudah ada, perbarui nomor urut; jika belum, buat baru.</li>
		 *       <li>Simpan via saveOrUpdate dalam transaksi terpisah per baris.</li>
		 *     </ul>
		 *   </li>
		 *   <li>Memperbarui Label progress per baris untuk ditampilkan di busy indicator.</li>
		 *   <li>Setelah selesai, mengosongkan Label untuk menandai thread selesai.</li>
		 *   <li>Timer mendeteksi Label kosong, menampilkan dialog konfirmasi sukses,
		 *       memanggil eventListener callback, dan menghentikan diri sendiri.</li>
		 * </ol>
		 *
		 * <p><b>Threading:</b> Thread terpisah menggunakan {@code HibernateUtil.currentNativeSession()}
		 * yang membuat session Hibernate baru di luar konteks ZK. Session ditutup dengan
		 * {@code HibernateUtil.closeSession()} di akhir thread run().</p>
		 *
		 * <p><b>Penanganan error:</b> Exception per baris ditangkap dan ditampilkan via
		 * {@code Common.tampilErrorJikaAdmin} tanpa menghentikan proses baris berikutnya.
		 * Exception level atas (membuka file, dll.) dicetak ke System.err.</p>
		 *
		 * @param file          File Excel yang akan diproses; harus berformat xlsx.
		 * @param eventListener Callback yang dipanggil setelah upload selesai;
		 *                      biasanya memanggil {@code loadData} untuk refresh grid.
		 * @param contents      Array nama kolom untuk keperluan export (tidak digunakan di upload).
		 * @throws Exception Jika terjadi kesalahan saat setup timer atau proses awal.
		 */
		public void uploadDataAkun(final File file, final EventListener eventListener, final String[] contents)
				throws Exception {

			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Akun Kelompok Laporan");
			final Label downloadPath = new Label("");
			final Label peringatan = new Label("");

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
			Clients.showBusy(label.getValue());
			final Timer timer = new Timer(200);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.showBusy(label.getValue());
					if (label.getValue().isEmpty()) {
						System.out.println("loading file " + file.getAbsolutePath());
						if (!downloadPath.getValue().isEmpty()) { try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) {} }
						MyMessageboxConfig.show(
								report.getRingkasan(),
								"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
						Clients.clearBusy();
						timer.detach();
					}

				}
			});
			timer.start();

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					try {

						XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
						XSSFSheet sheet = workbook.getSheetAt(0);

						Session session = HibernateUtil.currentNativeSession();

						int rowCount = (sheet.getLastRowNum() + 1);
						for (int i = 1; i < rowCount; i++) {
							try {

								Akun akun = (Akun) Common.getSheetContentAsObject(sheet, 0, i, Akun.class);
								if (akun != null && akun.getId() != null) {

									Integer urut = Common.getSheetContentAsInteger(sheet, 1, i);

									KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun = (KelompokLaporanPunyaAkun) session
											.createCriteria(KelompokLaporanPunyaAkun.class)
											.add(Restrictions.eq("akun", akun))
											.add(Restrictions.eq("kelompokLaporan", kelompokLaporan))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

									if (kelompokLaporanPunyaAkun == null) {
										kelompokLaporanPunyaAkun = new KelompokLaporanPunyaAkun();

									}

									kelompokLaporanPunyaAkun.setAkun(akun);
									kelompokLaporanPunyaAkun.setKelompokLaporan(kelompokLaporan);
									if (urut != null && urut > 0) {
										kelompokLaporanPunyaAkun.setNomorUrut(urut);
									}

									session.getTransaction().begin();
									session.saveOrUpdate(kelompokLaporanPunyaAkun);
									session.getTransaction().commit();

									label.setValue("Upload data \"" + akun.getNim() + " - " + akun.getNama() + "\" ("
											+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
									report.sukses(i, akun.getNim(), akun.getNama());
								}

							} catch (Exception e) {
								report.gagal(i, "baris-" + i, e, "Pastikan kode akun/asset valid dan tidak duplikat.");
								Common.tampilErrorJikaAdmin(e);
							}

						}
					} catch (Exception e1) {
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/akunting/KelompokLaporanDanDetailAction.java:923");
					}

					HibernateUtil.closeSession();

					try {
						java.io.File rptFile = report.simpanLaporan();
						downloadPath.setValue(rptFile.getAbsolutePath());
					} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) KelompokLaporanDanDetailAction laporan"); }
					label.setValue("");
									} finally {
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				}
			}).start();
		}

		/**
		 * Memuat ulang daftar akun dalam kelompok laporan ini ke dalam grid detail.
		 *
		 * <p><b>Tujuan:</b> Memperbarui tampilan grid akun berdasarkan data terkini
		 * di database, dengan mempertimbangkan filter pencarian yang aktif ({@code cari}).</p>
		 *
		 * <p><b>Cara kerja:</b> Memanggil {@code initCriteria(true)} untuk mendapatkan
		 * daftar {@code KelompokLaporanPunyaAkun}, membuat SimpleListModel, mengatur
		 * renderer {@code KelompokLaporanPunyaAkunRenderer}, dan menampilkan ke grid
		 * via {@code setModelCheckMobile}.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Parameter {@code value} tidak digunakan dalam implementasi
		 * ini; diterima untuk fleksibilitas pemanggilan dari berbagai konteks (EventListener,
		 * langsung dari kode, dll.).</p>
		 *
		 * @param value Parameter tidak digunakan; dapat berupa apapun atau null.
		 */
		@SuppressWarnings("unchecked")
		public void loadData(Object value) {

			List<KelompokLaporanPunyaAkun> kelompokLaporanPunyaAkun = initCriteria(true).list();

			ListModel strset = new SimpleListModel(kelompokLaporanPunyaAkun);
			grid.setRowRenderer(new KelompokLaporanPunyaAkunRenderer());
			grid.setModelCheckMobile(strset);

		}

		/**
		 * <h3>KelompokLaporanPunyaAkunRenderer — Renderer Baris Grid Akun per Kelompok</h3>
		 *
		 * <p><b>Untuk apa:</b> Kelas inner-inner yang merender setiap baris
		 * {@code KelompokLaporanPunyaAkun} dalam grid detail. Setiap baris menampilkan
		 * kode akun, nama akun, keterangan akun, nomor urut yang dapat diedit secara
		 * inline, serta tombol ubah dan hapus.</p>
		 *
		 * <p><b>Cara kerja:</b> Untuk setiap baris, komponen berikut diappend ke Row:</p>
		 * <ul>
		 *   <li>Vbox dari {@code RevisiHelper.createNewRevisi} berisi kode akun (tracking revisi).</li>
		 *   <li>Label nama akun.</li>
		 *   <li>Label keterangan dari kelompok laporan (bukan keterangan akun).</li>
		 *   <li>Intbox nomor urut yang dapat diedit; onChange langsung menyimpan via
		 *       {@code Common.refreshUpdate}.</li>
		 *   <li>Hbox toolbar dengan tombol ubah (membuka modal) dan hapus (konfirmasi dulu).</li>
		 * </ul>
		 *
		 * <p><b>Pemeliharaan:</b> Urutan append harus sesuai urutan kolom di {@code display()}.</p>
		 */
		class KelompokLaporanPunyaAkunRenderer extends ais.ui.util.MyRowRenderer {

			/**
			 * Konstruktor default untuk renderer; tidak memerlukan parameter tambahan.
			 */
			public KelompokLaporanPunyaAkunRenderer() {

			}

			/**
			 * Merender satu baris data KelompokLaporanPunyaAkun ke dalam komponen Row ZK.
			 *
			 * <p><b>Tujuan:</b> Mengisi satu baris grid detail dengan informasi akun
			 * dan kontrol interaktif untuk mengedit nomor urut dan melakukan aksi CRUD.</p>
			 *
			 * <p><b>Cara kerja per kolom:</b></p>
			 * <ol>
			 *   <li><b>Kode Akun:</b> Wrapped dalam RevisiHelper untuk tracking perubahan.</li>
			 *   <li><b>Nama Akun:</b> Label sederhana dari {@code akun.getNama()}.</li>
			 *   <li><b>Keterangan:</b> Label dari keterangan kelompok laporan
			 *       (bukan keterangan akun itu sendiri).</li>
			 *   <li><b>Nomor Urut:</b> Intbox yang dapat diedit langsung; onChange
			 *       menyimpan perubahan ke database secara real-time.</li>
			 *   <li><b>Tombol:</b> Hbox dengan Ubah (membuka formulir detail modal)
			 *       dan Hapus (konfirmasi → deleteRefresh → refresh daftar).</li>
			 * </ol>
			 *
			 * <p><b>Penanganan error:</b> Exception dari operasi hapus ditampilkan ke pengguna
			 * via dialog. Relasi FK yang masih ada akan menyebabkan error yang informatif.</p>
			 *
			 * @param row  Row ZK yang akan diisi komponen child.
			 * @param data Objek {@code KelompokLaporanPunyaAkun} yang akan dirender.
			 * @throws Exception Jika terjadi kesalahan saat merender komponen.
			 */
			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun = (KelompokLaporanPunyaAkun) data;

				RevisiHelper.createNewRevisi(KelompokLaporanPunyaAkun.class, kelompokLaporanPunyaAkun,
						kelompokLaporanPunyaAkun.getAkun() == null ? "" : kelompokLaporanPunyaAkun.getAkun().getKode())
						.setParent(row);
				new Label(
						kelompokLaporanPunyaAkun.getAkun() == null ? "" : kelompokLaporanPunyaAkun.getAkun().getNama())
						.setParent(row);
				new Label(kelompokLaporanPunyaAkun.getKelompokLaporan() == null ? ""
						: kelompokLaporanPunyaAkun.getKelompokLaporan().getKeterangan()).setParent(row);

				final Intbox nomorUrut = new Intbox(kelompokLaporanPunyaAkun.getNomorUrut());
				nomorUrut.setWidth("90%");
				nomorUrut.setParent(row);
				nomorUrut.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kelompokLaporanPunyaAkun.setNomorUrut(nomorUrut.getValue());
						Common.refreshUpdate(kelompokLaporanPunyaAkun);
					}
				});

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						init(kelompokLaporanPunyaAkun);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
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

												Common.refreshDelete(kelompokLaporanPunyaAkun);

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
				button.setParent(toolbar);
				toolbar.setParent(row);

			}

		}

		/**
		 * Menginisialisasi formulir modal untuk menambah atau mengubah satu detail akun.
		 *
		 * <p><b>Tujuan:</b> Membangun window modal lengkap dengan formulir pilih akun dan
		 * pilih kelompok laporan. Digunakan untuk mengelola satu entri {@code KelompokLaporanPunyaAkun}
		 * secara individual (jika perlu memindah akun ke kelompok lain atau memperbaiki data).</p>
		 *
		 * <p><b>Cara kerja:</b></p>
		 * <ol>
		 *   <li>Menyimpan entitas yang diedit ke field instance.</li>
		 *   <li>Mengatur judul window berdasarkan apakah ini tambah baru atau ubah.</li>
		 *   <li>Membangun Borderlayout dengan Center (formulir) dan South (toolbar).</li>
		 *   <li>Center berisi grid dengan dua baris:
		 *     <ul>
		 *       <li>Baris "Akun": AmbilDataAkunBanbox dengan nilai dan atribut akun saat ini.</li>
		 *       <li>Baris "Kelompok Laporan": combobox (disabled) menunjukkan kelompok aktif.</li>
		 *     </ul>
		 *   </li>
		 *   <li>South berisi tombol Batal (detach window) dan Simpan (panggil onSave).</li>
		 * </ol>
		 *
		 * <p><b>Pemeliharaan:</b> Ukuran window (400px x 600px) hardcoded; sesuaikan jika
		 * menambah baris formulir baru. Combobox kelompok laporan sengaja disabled karena
		 * kelompok laporan konteks sudah ditentukan oleh baris parent.</p>
		 *
		 * @param kelompokLaporanPunyaAkun Entitas yang akan diedit. Jika ID null, ini adalah
		 *                                 entri baru; jika tidak null, ini adalah edit existing.
		 */
		public void init(KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun) {
			this.kelompokLaporanPunyaAkun = kelompokLaporanPunyaAkun;
			addWindow.setTitle(kelompokLaporanPunyaAkun.getId() == null ? "Tambah Item" : "Ubah Item");
			addWindow.setHeight("400px");
			addWindow.setWidth("600px");
			Common.clear(addWindow);
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(addWindow);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));

			row.appendChild(akun = new AmbilDataAkunBanbox(false));
			akun.setValue(
					kelompokLaporanPunyaAkun.getAkun() == null ? "" : kelompokLaporanPunyaAkun.getAkun().getNama());
			akun.setAttribute("akun", kelompokLaporanPunyaAkun.getAkun());
			akun.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Laporan"));
			row.appendChild(cboKelompokLaporan);
			Common.selectComboItem(cboKelompokLaporan,
					kelompokLaporanPunyaAkun.getKelompokLaporan() == null ? kelompokLaporan
							: kelompokLaporanPunyaAkun.getKelompokLaporan());
			cboKelompokLaporan.setWidth("90%");
			cboKelompokLaporan.setDisabled(true);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						loadData(null);
						addWindow.detach();
					}
				}
			});
			save.setParent(toolbar);

			borderlayout.setParent(addWindow);

		}

		/**
		 * Memvalidasi input dan menyimpan satu entri detail akun dalam kelompok laporan.
		 *
		 * <p><b>Tujuan:</b> Menyimpan atau memperbarui satu entri {@code KelompokLaporanPunyaAkun}
		 * setelah memvalidasi bahwa akun telah dipilih. Digunakan dari tombol simpan di
		 * formulir modal detail.</p>
		 *
		 * <p><b>Cara kerja:</b></p>
		 * <ol>
		 *   <li>Memvalidasi bahwa atribut "akun" pada komponen {@code AmbilDataAkunBanbox}
		 *       tidak null; jika null, tampilkan peringatan dan kembalikan false.</li>
		 *   <li>Memuat ulang entitas dari DAO jika ID sudah ada (menghindari detached entity).</li>
		 *   <li>Mengisi kelompokLaporan dan akun pada entitas.</li>
		 *   <li>Melakukan update atau insert via {@code KelompokLaporanPunyaAkunDao}.</li>
		 * </ol>
		 *
		 * <p><b>Penanganan error:</b> Hanya validasi null akun yang ditangani secara eksplisit.
		 * Exception DAO dilempar ke pemanggil.</p>
		 *
		 * @param event Event ZK dari klik tombol simpan; tidak digunakan secara langsung.
		 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.
		 * @throws Exception Jika terjadi kesalahan database saat menyimpan.
		 */
		public boolean onSave(Event event) throws Exception {
			if (akun.getAttribute("akun") == null) {
				MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang dibutuhkan sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			KelompokLaporanPunyaAkunDao kelompokLaporanPunyaAkunDao = DaoFactory.getInstance()
					.getKelompokLaporanPunyaAkunDao();
			if (kelompokLaporanPunyaAkun.getId() != null) {
				kelompokLaporanPunyaAkun = kelompokLaporanPunyaAkunDao.load(kelompokLaporanPunyaAkun.getId());

			}

			kelompokLaporanPunyaAkun.setKelompokLaporan(kelompokLaporan);
			kelompokLaporanPunyaAkun.setAkun((Akun) akun.getAttribute("akun"));

			if (kelompokLaporanPunyaAkun.getId() != null) {
				kelompokLaporanPunyaAkunDao.update(kelompokLaporanPunyaAkun);
			} else {
				kelompokLaporanPunyaAkunDao.save(kelompokLaporanPunyaAkun);
			}
			return true;
		}

		/**
		 * Membangun kriteria Hibernate untuk query daftar akun dalam kelompok laporan ini.
		 *
		 * <p><b>Tujuan:</b> Implementasi {@code initCriteria} untuk inner class ini.
		 * Menghasilkan kriteria query yang memfilter {@code KelompokLaporanPunyaAkun}
		 * berdasarkan kelompok laporan aktif dan teks pencarian dari Textbox {@code cari}.</p>
		 *
		 * <p><b>Cara kerja:</b></p>
		 * <ul>
		 *   <li>JOIN dengan tabel Akun (createAlias) untuk memungkinkan filter berdasarkan
		 *       kode atau nama akun.</li>
		 *   <li>Filter teks ({@code cari}): ILIKE on kode akun OR nama akun jika tidak kosong.</li>
		 *   <li>Filter kelompok laporan: hanya baris milik kelompok laporan instance ini.</li>
		 *   <li>Urutan: ascending by nomorUrut, lalu descending by ID (data terbaru duluan
		 *       jika nomor urut sama).</li>
		 * </ul>
		 *
		 * <p><b>Catatan parameter {@code order}:</b> Dalam implementasi ini, ordering
		 * selalu diterapkan terlepas dari nilai parameter {@code order}.</p>
		 *
		 * @param order Parameter dari interface; tidak digunakan dalam implementasi ini
		 *              karena ordering selalu diterapkan.
		 * @return Objek {@code Criteria} Hibernate yang siap dieksekusi.
		 */
		@Override
		public Criteria initCriteria(boolean order) {

			Session session = HibernateUtil.currentSession();
			return session.createCriteria(KelompokLaporanPunyaAkun.class).createAlias("akun", "akun")
					.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

							Restrictions.or(Restrictions.ilike("akun.kode", cari.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("akun.nama", cari.getValue().trim(), MatchMode.ANYWHERE))

					).addOrder(Order.asc("nomorUrut")).addOrder(Order.desc("id"))
					.add(Restrictions.eq("kelompokLaporan", kelompokLaporan));
		}

		/**
		 * Memperbarui tampilan daftar akun di panel detail.
		 *
		 * <p><b>Tujuan:</b> Implementasi {@code onSearchDefault} untuk inner class ini.
		 * Dipanggil setelah operasi CRUD (tambah/hapus akun) untuk merefresh tampilan grid.</p>
		 *
		 * <p><b>Cara kerja:</b> Mendelegasikan langsung ke {@code loadData(null)}.</p>
		 *
		 * @param event Event ZK pemicu; tidak digunakan.
		 */
		@Override
		public void onSearchDefault(Event event) {
			loadData(null);
		}

	}

	/**
	 * Menangani klik tombol tambah untuk membuat KelompokLaporan baru.
	 *
	 * <p><b>Tujuan:</b> Event handler dari tombol "Tambah" di toolbar utama.
	 * Membuka formulir modal kosong untuk membuat kelompok laporan baru.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@code KelompokLaporan} baru (tanpa ID),
	 * memanggil {@code initKelompokLaporan} untuk membangun UI formulir dalam window baru,
	 * lalu menampilkannya sebagai modal dialog.</p>
	 *
	 * @param event Event ZK dari klik tombol; tidak digunakan secara langsung.
	 * @throws Exception Jika terjadi kesalahan saat membangun formulir.
	 */
	public void onAddKelompokLaporan(Event event) throws Exception {
		initKelompokLaporan(new KelompokLaporan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir modal lengkap untuk menambah atau mengubah KelompokLaporan.
	 *
	 * <p><b>Tujuan:</b> Metode private yang membuat instance {@code MyWindow} baru dan
	 * mengisinya dengan semua komponen formulir KelompokLaporan. Berbeda dari kelas action
	 * lain yang menggunakan window yang sudah ada dari ZUL, metode ini membuat window baru
	 * secara programatik dan menambahkannya ke page.</p>
	 *
	 * <p><b>Cara kerja — field formulir yang dibangun:</b></p>
	 * <ul>
	 *   <li><b>No Urut *:</b> MyDoublebox untuk posisi kelompok dalam laporan.</li>
	 *   <li><b>Jenis Laporan *:</b> Combobox dari tabel JenisLaporan (readonly).</li>
	 *   <li><b>Grup Laporan *:</b> Combobox dari tabel MasterGrupLaporan (readonly).</li>
	 *   <li><b>Sub Grup I:</b> Textbox keterangan (level pertama pengelompokan).</li>
	 *   <li><b>Sub Grup II:</b> Textbox keterangan1 (level kedua pengelompokan).</li>
	 *   <li><b>Sub Grup III:</b> Textbox keterangan2 (level ketiga pengelompokan).</li>
	 *   <li><b>Tampilkan Akun Rinci:</b> Checkbox untuk mode tampil rinci di laporan.</li>
	 *   <li><b>Aktif:</b> Checkbox status aktif setup laporan ini.</li>
	 * </ul>
	 *
	 * <p><b>Perbedaan dengan kelas lain:</b> Window dibuat baru tiap kali (bukan reuse),
	 * ditambahkan ke {@code page.getFirstRoot()}, dan di-detach (bukan setVisible false)
	 * saat ditutup. Field {@code addWindow} dari outer class di-overwrite di sini.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari penyimpanan ditangani di {@code onSave}.</p>
	 *
	 * @param kelompokLaporan Entitas yang akan ditampilkan di formulir.
	 *                        ID null untuk data baru, ID tidak null untuk edit.
	 */
	private void initKelompokLaporan(KelompokLaporan kelompokLaporan) {
		this.kelompokLaporan = kelompokLaporan;

		addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setHeight("95%");
		addWindow.setWidth("600px");
		addWindow.setTitle(kelompokLaporan.getId() == null ? "Tambah Kelompok Laporan" : "Ubah Kelompok Laporan");
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

		Common.insertCombo(jenisLaporan = new Combobox(), "nama", "keterangan", JenisLaporan.class);

		Common.insertCombo(masterGrupLaporan = new Combobox(), new String[] { "nama", "id", "keterangan" },
				"keterangan", MasterGrupLaporan.class);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("No Urut *"));
		row.appendChild(urut = new MyDoublebox(kelompokLaporan.getUrut() == null ? 0.0 : kelompokLaporan.getUrut()));
		urut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Laporan *"));
		row.appendChild(jenisLaporan);
		Common.selectComboItem(jenisLaporan, kelompokLaporan.getJenisLaporan());
		jenisLaporan.setWidth("90%");
		jenisLaporan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Laporan *"));
		row.appendChild(masterGrupLaporan);
		Common.selectComboItem(masterGrupLaporan, kelompokLaporan.getMasterGrupLaporan());
		masterGrupLaporan.setWidth("90%");
		masterGrupLaporan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sub Grup I"));
		row.appendChild(keterangan = new Textbox(kelompokLaporan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sub Grup II"));
		row.appendChild(keterangan1 = new Textbox(kelompokLaporan.getKeterangan1()));
		keterangan1.setWidth("90%");
		keterangan1.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sub Grup III"));
		row.appendChild(keterangan2 = new Textbox(kelompokLaporan.getKeterangan2()));
		keterangan2.setWidth("90%");
		keterangan2.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tampilkanAkunRinci = new MyCheckboxConfig("Tampilkan akun secara rinci"));
		tampilkanAkunRinci.setChecked(kelompokLaporan.getTampilkanAkunRinci());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktif = new MyCheckboxConfig("Setup laporan ini aktif"));
		aktif.setChecked(kelompokLaporan.getAktif());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	/**
	 * <h3>KelompokLaporanRenderer — Renderer Baris Grid KelompokLaporan Utama</h3>
	 *
	 * <p><b>Untuk apa:</b> Merender setiap baris {@code KelompokLaporan} dalam grid utama.
	 * Setiap baris adalah komponen expand-able ({@code KelompokLaporanPunyaAkunAction})
	 * yang menampilkan panel detail akun saat diklik.</p>
	 *
	 * <p><b>Cara kerja:</b> Untuk setiap KelompokLaporan, komponen berikut diappend ke Row:</p>
	 * <ul>
	 *   <li>{@code KelompokLaporanPunyaAkunAction}: Panel expand-able berisi daftar akun.</li>
	 *   <li>Label nomor urut (diformat dengan numberFormat).</li>
	 *   <li>Vbox dari RevisiHelper berisi nama JenisLaporan (dengan tracking revisi).</li>
	 *   <li>Label nama MasterGrupLaporan.</li>
	 *   <li>Label keterangan MasterGrupLaporan.</li>
	 *   <li>Vbox berisi tiga Label sub-grup (keterangan, keterangan1, keterangan2).</li>
	 *   <li>Label jumlah akun dalam kelompok (query count langsung dari DB).</li>
	 *   <li>Label tampilkanAkunRinci (Ya/Tidak).</li>
	 *   <li>Label status aktif (Ya/Tidak).</li>
	 *   <li>Hbox tombol Ubah dan Hapus dari {@code Common.copyEditDeleteButtons}.</li>
	 * </ul>
	 *
	 * <p><b>Pemeliharaan:</b> Query count akun per baris adalah N+1 query masalah karena
	 * dilakukan untuk setiap baris grid. Pertimbangkan menggunakan Projections atau batch
	 * loading jika performa menjadi isu dengan data yang banyak.</p>
	 */
	class KelompokLaporanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data KelompokLaporan ke dalam komponen Row ZK.
		 *
		 * <p><b>Tujuan:</b> Mengisi satu baris grid utama dengan panel detail expand-able
		 * dan semua informasi kelompok laporan beserta tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b> Menambahkan instance {@code KelompokLaporanPunyaAkunAction}
		 * sebagai elemen pertama Row (akan di-expand saat klik). Kemudian menambahkan
		 * label-label informasi dan menghitung jumlah akun via Hibernate Projections.rowCount().
		 * Tombol ubah dan hapus menggunakan helper {@code Common.copyEditDeleteButtons}
		 * yang menangani event onClick secara otomatis mengacu ke interface outer class.</p>
		 *
		 * <p><b>Penanganan error:</b> Jika entitas terkait null, ditampilkan string kosong.</p>
		 *
		 * @param arg0 Row ZK yang akan diisi.
		 * @param arg1 Objek {@code KelompokLaporan} yang akan dirender.
		 * @throws Exception Jika terjadi kesalahan merender komponen atau akses database.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KelompokLaporan kelompokLaporan = (KelompokLaporan) arg1;

			(new KelompokLaporanPunyaAkunAction(kelompokLaporan)).setParent(arg0);
			new Label(kelompokLaporan.getUrut() == null ? "" : Common.numberFormat.get().format(kelompokLaporan.getUrut()))
					.setParent(arg0);

			RevisiHelper.createNewRevisi(KelompokLaporan.class, kelompokLaporan,
					kelompokLaporan.getJenisLaporan().getNama()).setParent(arg0);

			new Label(kelompokLaporan.getMasterGrupLaporan().getNama()).setParent(arg0);
			new Label(kelompokLaporan.getMasterGrupLaporan().getKeterangan()).setParent(arg0);

			Vbox ss = new Vbox();
			ss.setParent(arg0);
			new Label(kelompokLaporan.getKeterangan()).setParent(ss);
			new Label(kelompokLaporan.getKeterangan1()).setParent(ss);
			new Label(kelompokLaporan.getKeterangan2()).setParent(ss);

			int qty = ((Number) HibernateUtil.currentSession().createCriteria(KelompokLaporanPunyaAkun.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("kelompokLaporan", kelompokLaporan))
					.uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(qty)).setParent(arg0);
			new Label(kelompokLaporan.getTampilkanAkunRinci() ? "Ya" : "Tidak").setParent(arg0);
			new Label(kelompokLaporan.getAktif() ? "Ya" : "Tidak").setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokLaporan, KelompokLaporanDanDetailAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * Memvalidasi input dan menyimpan data KelompokLaporan ke database.
	 *
	 * <p><b>Tujuan:</b> Metode inti penyimpanan KelompokLaporan. Dipanggil dari tombol
	 * Simpan di formulir modal. Menangani validasi, penyimpanan, dan fitur salin akun
	 * dari kelompok lain jika {@code copyDari} diset.</p>
	 *
	 * <p><b>Cara kerja — validasi:</b></p>
	 * <ol>
	 *   <li>Memastikan No Urut tidak null.</li>
	 *   <li>Memastikan Jenis Laporan dipilih.</li>
	 *   <li>Memastikan Master Grup Laporan dipilih.</li>
	 * </ol>
	 *
	 * <p><b>Cara kerja — penyimpanan:</b></p>
	 * <ol>
	 *   <li>Memuat ulang dari DAO jika ID sudah ada.</li>
	 *   <li>Mengisi semua field: urut, jenisLaporan, masterGrupLaporan, aktif,
	 *       tampilkanAkunRinci, keterangan, keterangan1, keterangan2.</li>
	 *   <li>Melakukan update atau insert via {@code KelompokLaporanDao}.</li>
	 *   <li>Jika {@code kelompokLaporan.getCopyDari()} tidak null (fitur duplikasi),
	 *       menyalin semua akun dari kelompok sumber ke kelompok baru. Untuk setiap
	 *       KelompokLaporanPunyaAkun dari sumber, dicek apakah sudah ada di kelompok
	 *       baru; jika belum, dibuat baru dan disimpan.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Validasi menampilkan dialog dan mengembalikan false.
	 * Exception database dilempar ke pemanggil.</p>
	 *
	 * @param event Event ZK dari klik tombol simpan; tidak digunakan langsung.
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan database saat menyimpan.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (urut.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Urut belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom No Urut dengan angka yang valid; (2) Pastikan nomor urut tidak kosong dan bernilai positif; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisLaporan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Laporan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Laporan dari dropdown yang tersedia; (2) Pastikan jenis laporan yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (masterGrupLaporan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Master Grup Laporan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Master Grup Laporan dari dropdown yang tersedia; (2) Pastikan master grup laporan yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KelompokLaporanDao kelompokLaporanDao = DaoFactory.getInstance().getKelompokLaporanDao();

		if (kelompokLaporan.getId() != null) {
			kelompokLaporan = kelompokLaporanDao.load(kelompokLaporan.getId());
		}
		kelompokLaporan.setUrut(urut.getValue());
		kelompokLaporan.setJenisLaporan((JenisLaporan) jenisLaporan.getSelectedItem().getValue());
		kelompokLaporan.setMasterGrupLaporan((MasterGrupLaporan) masterGrupLaporan.getSelectedItem().getValue());
		kelompokLaporan.setAktif(aktif.isChecked());
		kelompokLaporan.setTampilkanAkunRinci(tampilkanAkunRinci.isChecked());
		kelompokLaporan.setKeterangan(keterangan.getValue());
		kelompokLaporan.setKeterangan1(keterangan1.getValue());
		kelompokLaporan.setKeterangan2(keterangan2.getValue());

		if (kelompokLaporan.getId() != null) {
			kelompokLaporanDao.update(kelompokLaporan);
		} else {
			kelompokLaporanDao.save(kelompokLaporan);
		}

		if (kelompokLaporan.getCopyDari() != null && kelompokLaporan.getCopyDari().getId() != null) {
			Session session = kelompokLaporanDao.getCurrentSession();

			List<KelompokLaporanPunyaAkun> laporanPunyaAkuns = session.createCriteria(KelompokLaporanPunyaAkun.class)
					.add(Restrictions.eq("kelompokLaporan.id", kelompokLaporan.getCopyDari().getId())).list();

			for (KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun : laporanPunyaAkuns) {
				KelompokLaporanPunyaAkun kelompokLaporanPunyaAkunBaru = (KelompokLaporanPunyaAkun) session
						.createCriteria(KelompokLaporanPunyaAkun.class)
						.add(Restrictions.eq("akun", kelompokLaporanPunyaAkun.getAkun()))
						.add(Restrictions.eq("kelompokLaporan", this.kelompokLaporan)).addOrder(Order.desc("id"))
						.setMaxResults(1).uniqueResult();
				if (kelompokLaporanPunyaAkunBaru == null) {
					kelompokLaporanPunyaAkunBaru = new KelompokLaporanPunyaAkun();
					kelompokLaporanPunyaAkunBaru.setAkun(kelompokLaporanPunyaAkun.getAkun());
					kelompokLaporanPunyaAkunBaru.setKelompokLaporan(this.kelompokLaporan);
					session.save(kelompokLaporanPunyaAkunBaru);
					session.flush();
				}

			}
		}

		return true;
	}

	/**
	 * Memperbarui tampilan daftar KelompokLaporan di grid utama.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@code onSearchDefault}. Memuat semua KelompokLaporan
	 * yang sesuai filter ke grid utama, tanpa paging (menggunakan MAX_RESULT global).</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code initCriteria(true)} dengan MAX_RESULT,
	 * membuat SimpleListModel, mengatur {@code KelompokLaporanRenderer}, dan memperbarui
	 * grid via {@code setModelCheckMobile}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika jumlah kelompok laporan sangat banyak (>MAX_RESULT),
	 * perlu menambahkan paging seperti halaman-halaman lain.</p>
	 *
	 * @param event Event ZK pemicu; boleh null jika dipanggil programatik.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		List<KelompokLaporan> kelompokLaporan = initCriteria(true).setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(kelompokLaporan);
		grid.setRowRenderer(new KelompokLaporanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun kriteria Hibernate untuk query detail (KelompokLaporanPunyaAkun) global.
	 *
	 * <p><b>Tujuan:</b> Menghasilkan kriteria untuk query seluruh {@code KelompokLaporanPunyaAkun}
	 * lintas semua kelompok laporan, dengan filter dari panel pencarian utama. Digunakan
	 * oleh tombol "Download Akun" di toolbar utama untuk export data lengkap.</p>
	 *
	 * <p><b>Cara kerja — filter yang diterapkan:</b></p>
	 * <ul>
	 *   <li>LEFT JOIN ke kelompokLaporan dan masterGrupLaporan untuk akses field.</li>
	 *   <li>Filter nama master grup laporan (searchNamaMasterGrupLaporan): ILIKE.</li>
	 *   <li>Filter jenis laporan (searchJenisLaporan): eq.</li>
	 *   <li>Filter master grup laporan spesifik (searchMasterGrupLaporan): eq.</li>
	 *   <li>Filter teks keterangan (searchketerangan): ILIKE pada keterangan, keterangan1,
	 *       dan keterangan2 (menggunakan OR).</li>
	 * </ul>
	 *
	 * <p><b>Urutan:</b> ascending urut kelompok → ascending ID kelompok → ascending nomorUrut → ascending ID detail.</p>
	 *
	 * @param order Parameter tidak digunakan karena ordering selalu diterapkan.
	 * @return Objek {@code Criteria} Hibernate untuk query detail global.
	 */
	public Criteria initCriteriaDetail(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(KelompokLaporanPunyaAkun.class)

				.createAlias("kelompokLaporan", "kelompokLaporan", Criteria.LEFT_JOIN)
				.createAlias("kelompokLaporan.masterGrupLaporan", "masterGrupLaporan", Criteria.LEFT_JOIN)

				.add(searchNamaMasterGrupLaporan.getSelectedItem() == null
						|| searchNamaMasterGrupLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("masterGrupLaporan.nama",
										searchNamaMasterGrupLaporan.getSelectedItem().getValue()))

				.add(searchJenisLaporan.getSelectedItem() == null
						|| searchJenisLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokLaporan.jenisLaporan",
										searchJenisLaporan.getSelectedItem().getValue()))

				.add(searchMasterGrupLaporan.getSelectedItem() == null
						|| searchMasterGrupLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokLaporan.masterGrupLaporan",
										searchMasterGrupLaporan.getSelectedItem().getValue()))

				.addOrder(Order.asc("kelompokLaporan.urut")).addOrder(Order.asc("kelompokLaporan.id"))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id"))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("kelompokLaporan.keterangan", searchketerangan.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("kelompokLaporan.keterangan1",
												searchketerangan.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("kelompokLaporan.keterangan2",
												searchketerangan.getValue().trim(), MatchMode.ANYWHERE)))

				);
	}

	/**
	 * Membangun kriteria Hibernate untuk query daftar KelompokLaporan utama.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code initCriteria} dari interface {@code DataCriteria}.
	 * Menghasilkan kriteria query yang memfilter {@code KelompokLaporan} berdasarkan
	 * filter yang aktif di panel pencarian utama.</p>
	 *
	 * <p><b>Cara kerja — filter yang diterapkan:</b></p>
	 * <ul>
	 *   <li>LEFT JOIN ke masterGrupLaporan untuk akses field nama.</li>
	 *   <li>Filter nama master grup (searchNamaMasterGrupLaporan): ILIKE.</li>
	 *   <li>Filter jenis laporan (searchJenisLaporan): eq pada relasi JenisLaporan.</li>
	 *   <li>Filter master grup spesifik (searchMasterGrupLaporan): eq pada relasi.</li>
	 *   <li>Filter teks sub-grup (searchketerangan): ILIKE pada keterangan, keterangan1,
	 *       dan keterangan2 menggunakan OR bertingkat.</li>
	 * </ul>
	 *
	 * <p><b>Urutan:</b> Ascending berdasarkan field urut.</p>
	 *
	 * @param order Parameter tidak digunakan; ordering selalu ascending by urut.
	 * @return Objek {@code Criteria} Hibernate untuk query KelompokLaporan.
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(KelompokLaporan.class)
				.createAlias("masterGrupLaporan", "masterGrupLaporan", Criteria.LEFT_JOIN)

				.add(searchNamaMasterGrupLaporan.getSelectedItem() == null
						|| searchNamaMasterGrupLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("masterGrupLaporan.nama",
										searchNamaMasterGrupLaporan.getSelectedItem().getValue()))

				.add(searchJenisLaporan.getSelectedItem() == null
						|| searchJenisLaporan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisLaporan", searchJenisLaporan.getSelectedItem().getValue()))

				.add(searchMasterGrupLaporan.getSelectedItem() == null
						|| searchMasterGrupLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("masterGrupLaporan",
										searchMasterGrupLaporan.getSelectedItem().getValue()))

				.addOrder(Order.asc("urut"))
				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("keterangan", searchketerangan.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("keterangan1", searchketerangan.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan2", searchketerangan.getValue().trim(),
												MatchMode.ANYWHERE))));
	}

	/**
	 * Menginisialisasi formulir untuk mengedit KelompokLaporan yang sudah ada.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code init} dari interface {@code DataInitDefault}.
	 * Dipanggil ketika pengguna mengklik tombol "Ubah" pada baris grid KelompokLaporan.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengkasting {@code GeneralValueObject} ke {@code KelompokLaporan},
	 * memanggil {@code initKelompokLaporan} untuk membangun formulir dengan data yang ada,
	 * kemudian menampilkan window sebagai modal dialog.</p>
	 *
	 * @param obj Entitas {@code KelompokLaporan} yang akan diedit, dibungkus dalam
	 *            {@code GeneralValueObject}. Harus bertipe {@code KelompokLaporan}.
	 * @throws Exception Jika terjadi kesalahan saat membangun formulir atau menampilkan modal.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		KelompokLaporan kelompokLaporan = (KelompokLaporan) obj;
		initKelompokLaporan(kelompokLaporan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
