package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.database.model.asset.ParameterTambahanPerbaikanAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>ParameterTambahanPerbaikanAssetAction — Kontroler CRUD Parameter Tambahan Perbaikan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZK (ZKoss Composer) untuk halaman manajemen parameter tambahan
 * yang digunakan dalam proses perbaikan aset (alat/fasilitas) di modul manajemen aset eCampus.
 * Parameter tambahan adalah metadata fleksibel yang melengkapi formulir perbaikan aset,
 * misalnya: "Nama Teknisi", "Biaya Suku Cadang", "Nomor Work Order", dan sebagainya. Parameter
 * ini dikelompokkan ke dalam kelompok parameter ({@code KelompokParameterTambahanPerbaikanAsset})
 * sehingga dapat ditampilkan secara terstruktur pada formulir perbaikan. Halaman ini menyediakan
 * fitur: melihat daftar parameter per kelompok, menambahkan parameter baru (dari daftar
 * {@code ParameterTambahan} yang tersedia) ke kelompok yang dipilih, mengubah kelompok
 * atau parameter dari entri yang sudah ada, serta menghapus entri parameter.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Halaman ini menggunakan pola tab untuk navigasi: tab utama menampilkan daftar parameter
 * dengan filter per kelompok, tab "Manajemen Kelompok" menampilkan halaman kelola kelompok
 * parameter (lazy-loaded via {@code MyInclude}), dan tab "Manajemen Parameter" menampilkan
 * halaman kelola master parameter tambahan (juga lazy-loaded). Saat tombol "Tambah" ditekan,
 * dialog multi-pilih {@code AmbilDataParameterTambahanBanyak} ditampilkan sehingga pengguna
 * dapat memilih beberapa parameter sekaligus untuk ditambahkan ke kelompok yang aktif.
 * Setiap parameter yang dipilih disimpan sebagai entitas {@code ParameterTambahanPerbaikanAsset}
 * yang menghubungkan {@code ParameterTambahan} dengan {@code KelompokParameterTambahanPerbaikanAsset}.<br><br>
 *
 * <b>Threading:</b><br>
 * Semua operasi database dilakukan di thread utama ZK. Tidak ada pemrosesan latar belakang
 * di kelas ini. Operasi penyimpanan multi-parameter di {@code onAdd} menggunakan sesi
 * Hibernate yang sama untuk semua iterasi dalam satu transaksi implisit.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan {@code DataCriteria} dan {@code DataSearchDefault}.
 * Jika struktur parameter tambahan berubah (misalnya penambahan field "urutan tampil"),
 * modifikasi perlu dilakukan di entitas {@code ParameterTambahanPerbaikanAsset}, di
 * form {@code init()}, di {@code onSave()}, dan di renderer. Pengurutan default adalah
 * descending berdasarkan ID; ubah di {@code initCriteria} jika diperlukan urutan lain.
 *
 * @author eCampus Dev Team
 * @version 1.0
 */
public class ParameterTambahanPerbaikanAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai komponen ZK.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal untuk form tambah/ubah parameter tambahan perbaikan aset. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk grid daftar parameter. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar parameter tambahan per kelompok. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama parameter tambahan. */
	private Textbox searchnama;

	/** Combobox filter untuk memilih kelompok parameter yang ingin ditampilkan. */
	private Combobox searchkelompokParameterTambahanPerbaikanAsset;

	/** Combobox input kelompok parameter pada form tambah/ubah. */
	private Combobox kelompokParameterTambahanPerbaikanAsset;

	/** Combobox input parameter tambahan yang tersedia pada form tambah/ubah. */
	private Combobox parameterTambahan;

	/** Penanda apakah pengguna memiliki hak ubah data (default: true karena halaman internal). */
	private boolean edit = true;

	/** Penanda apakah pengguna memiliki hak hapus data (default: true karena halaman internal). */
	private boolean delete = true;

	/** Tombol toolbar untuk aksi temukan/cari, digunakan sebagai titik acuan toolbar. */
	private MyToolbarbuttonConfig find;

	/** Entitas parameter tambahan perbaikan aset yang sedang dikelola. */
	private ParameterTambahanPerbaikanAsset parameterTambahanPerbaikanAsset;

	/**
	 * Mereset filter kelompok ke pilihan pertama dan memuat ulang daftar parameter.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengisi ulang combobox filter kelompok parameter dengan data terbaru dari database,
	 * memilih entri pertama secara otomatis, lalu me-refresh grid daftar parameter.
	 * Digunakan sebagai "tombol reset filter" agar pengguna dapat kembali ke tampilan awal
	 * dengan cepat tanpa harus mengatur ulang filter secara manual.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Mengisi ulang combobox filter kelompok dengan semua {@code KelompokParameterTambahanPerbaikanAsset}
	 *     dari database via {@code Common.insertCombo}.
	 * (2) Jika ada item di combobox, memilih item pertama (index 0).
	 * (3) Memanggil {@code onSearchDefault(null)} untuk me-refresh grid.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari event ZUL (misalnya tombol reset). Jika logika reset
	 * perlu mencakup field filter lain, tambahkan pembersihan field di sini.
	 *
	 * @param event Event ZK yang memicu reset (biasanya klik tombol reset filter).
	 */
	public void onResetParameter(Event event) {
		Common.insertCombo(searchkelompokParameterTambahanPerbaikanAsset, "nama",
				KelompokParameterTambahanPerbaikanAsset.class);
		if (!searchkelompokParameterTambahanPerbaikanAsset.getChildren().isEmpty()) {
			searchkelompokParameterTambahanPerbaikanAsset.setSelectedIndex(0);
		}
		onSearchDefault(null);
	}

	/** Panel tab untuk menampilkan halaman manajemen kelompok parameter (lazy-loaded). */
	private Tabpanel manajemenKelompok;

	/**
	 * Memuat halaman manajemen kelompok parameter ke dalam tab yang sesuai.
	 *
	 * <b>Tujuan:</b><br>
	 * Menampilkan halaman manajemen kelompok parameter tambahan perbaikan aset
	 * ({@code kelompok_parameter_tambahan_perbaikan_asset.zul}) di dalam tab
	 * "Manajemen Kelompok" secara lazy. Halaman ini hanya dimuat pertama kali
	 * tab diklik untuk menghemat sumber daya.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika panel tab belum memiliki konten (children kosong), membuat {@code MyWindow}
	 * transparan berukuran penuh dan menambahkan {@code MyInclude} yang menunjuk ke
	 * file ZUL manajemen kelompok. Jika sudah ada konten, tidak ada yang dilakukan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Path ZUL dapat berubah jika file dipindahkan. Pastikan path di {@code MyInclude}
	 * selalu konsisten dengan lokasi file ZUL sebenarnya.
	 *
	 * @param event Event ZK dari pemilihan tab manajemen kelompok.
	 */
	public void onManajemenKelompok(Event event) {
		if (manajemenKelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompok);
			MyInclude iframe = new MyInclude("/pages/master/asset/kelompok_parameter_tambahan_perbaikan_asset.zul");
			iframe.setParent(window);
		}
	}

	/** Panel tab untuk menampilkan halaman manajemen master parameter tambahan (lazy-loaded). */
	private Tabpanel manajemenParameter;

	/**
	 * Memuat halaman manajemen parameter tambahan ke dalam tab yang sesuai.
	 *
	 * <b>Tujuan:</b><br>
	 * Menampilkan halaman master parameter tambahan ({@code parameter_tambahan.zul})
	 * di dalam tab "Manajemen Parameter" secara lazy. Pengguna dapat mengelola
	 * definisi parameter tambahan (tipe data, label inputan, nilai default) dari tab ini.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Sama dengan {@code onManajemenKelompok}: membuat MyWindow transparan dan
	 * MyInclude yang menunjuk ke ZUL parameter tambahan, hanya jika tab belum berisi konten.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perubahan path ZUL master parameter tambahan harus diperbarui di sini.
	 *
	 * @param event Event ZK dari pemilihan tab manajemen parameter.
	 */
	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * Pemeriksaan keamanan sebelum halaman dikompilasi oleh ZK.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan pengguna memiliki sesi dan hak akses yang valid sebelum halaman
	 * parameter tambahan perbaikan aset dimuat. Ini adalah gate keamanan pertama
	 * yang dipanggil dalam siklus hidup halaman ZK.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk validasi sesi dan hak akses,
	 * lalu memanggil {@code super.doBeforeCompose} untuk melanjutkan proses ZK normal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pola standar keamanan eCampus. Jangan modifikasi tanpa koordinasi tim keamanan.
	 *
	 * @param page     Halaman ZK yang sedang dimuat.
	 * @param parent   Komponen induk.
	 * @param compInfo Informasi metadata komponen ZUL.
	 * @return {@code ComponentInfo} untuk dilanjutkan proses komposisi oleh ZK.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi komponen dan logika bisnis setelah semua elemen ZUL terhubung.
	 *
	 * <b>Tujuan:</b><br>
	 * Titik masuk utama inisialisasi halaman parameter tambahan perbaikan aset. Dipanggil
	 * otomatis oleh ZK setelah semua komponen di-autowire. Menyiapkan filter kelompok,
	 * data awal grid, paginasi, serta tombol cetak dan upload data.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code super.doAfterCompose} dan menginisialisasi bahasa.
	 * (2) Memeriksa dan membuat kelompok parameter default via {@code checkCreateDefault()}.
	 * (3) Mengisi combobox filter kelompok dengan semua kelompok yang tersedia dan
	 *     memilih item pertama secara otomatis.
	 * (4) Memanggil {@code onSearchDefault} untuk memuat data grid awal.
	 * (5) Mengatur event listener paginasi.
	 * (6) Menambahkan tombol cetak dan upload data ke toolbar.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception diteruskan ke ZK framework. Kegagalan {@code checkCreateDefault}
	 * (misalnya database tidak responsif) akan mencegah halaman dimuat.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom yang diekspor berubah, perbarui array {@code contents}.
	 *
	 * @param comp Komponen akar halaman ZUL yang telah di-autowire.
	 * @throws Exception Jika terjadi kesalahan inisialisasi ZK atau Hibernate.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		KelompokParameterTambahanPerbaikanAsset.checkCreateDefault();

		Common.insertCombo(searchkelompokParameterTambahanPerbaikanAsset, "nama",
				KelompokParameterTambahanPerbaikanAsset.class);
		if (!searchkelompokParameterTambahanPerbaikanAsset.getChildren().isEmpty()) {
			searchkelompokParameterTambahanPerbaikanAsset.setSelectedIndex(0);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "parameterTambahan", "kelompokParameterTambahanPerbaikanAsset",
				"nomorUrut" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanPerbaikanAsset.class, contents);
		Common.appendKeToolbar(upload, find, comp);
	}

	/**
	 * Renderer baris grid untuk daftar parameter tambahan perbaikan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Merender setiap entri {@code ParameterTambahanPerbaikanAsset} ke dalam baris
	 * grid ZK. Setiap baris menampilkan nama kelompok, nama/label parameter, flag
	 * lampiran, tipe data, nilai data, serta tombol ubah dan hapus.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode {@code render} dipanggil per baris oleh ZK. Tombol ubah membuka form
	 * edit via {@code init(parameterTambahanPerbaikanAsset)}. Tombol hapus memunculkan
	 * dialog konfirmasi sebelum memanggil {@code Common.refreshDelete}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penambahan kolom baru memerlukan sinkronisasi dengan header kolom di file ZUL.
	 */
	class ParameterTambahanPerbaikanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data parameter tambahan perbaikan aset.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi sel-sel baris grid dengan data kelompok parameter, label inputan,
		 * flag lampiran, tipe data, nilai data, serta tombol aksi ubah dan hapus.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * Membuat Label untuk setiap kolom data. Tombol ubah memanggil {@code init()}
		 * untuk membuka form edit dalam modal. Tombol hapus menampilkan dialog konfirmasi;
		 * jika pengguna mengkonfirmasi, memanggil {@code Common.refreshDelete} dan
		 * jika gagal (karena relasi FK), menampilkan pesan error yang informatif.<br><br>
		 *
		 * <b>Penanganan error:</b><br>
		 * Kegagalan hapus karena relasi FK ditangkap dan ditampilkan ke pengguna melalui
		 * {@code Common.tampilErrorJikaAdmin} dan {@code MyMessageboxConfig.show}.
		 *
		 * @param arg0 Baris grid ZK yang akan diisi.
		 * @param arg1 Objek {@code ParameterTambahanPerbaikanAsset} yang dirender.
		 * @throws Exception Jika terjadi kesalahan pembuatan komponen ZK.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			final ParameterTambahanPerbaikanAsset parameterTambahanPerbaikanAsset = (ParameterTambahanPerbaikanAsset) arg1;
			new Label(parameterTambahanPerbaikanAsset.getKelompokParameterTambahanPerbaikanAsset().getNama())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanPerbaikanAsset.class, parameterTambahanPerbaikanAsset,
					parameterTambahanPerbaikanAsset.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(
					parameterTambahanPerbaikanAsset.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(parameterTambahanPerbaikanAsset.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanPerbaikanAsset.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanPerbaikanAsset);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(parameterTambahanPerbaikanAsset);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Menangani klik tombol "Tambah" untuk menambahkan parameter baru ke kelompok aktif.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka dialog multi-pilih {@code AmbilDataParameterTambahanBanyak} agar pengguna
	 * dapat memilih satu atau lebih parameter tambahan yang belum ada di kelompok aktif,
	 * lalu menyimpan semua pilihan tersebut sebagai entitas baru di database.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memvalidasi bahwa kelompok sudah dipilih di filter; jika belum, menampilkan
	 *     peringatan dan fokus ke combobox kelompok.
	 * (2) Mengambil daftar parameter yang sudah ada di kelompok aktif (untuk dikecualikan
	 *     dari pilihan) via criteria dengan projection groupProperty.
	 * (3) Membuka dialog {@code AmbilDataParameterTambahanBanyak} dengan daftar eksklusi.
	 * (4) Ketika pengguna mengkonfirmasi pilihan, event listener menyimpan setiap parameter
	 *     yang dipilih sebagai {@code ParameterTambahanPerbaikanAsset} baru ke database.
	 * (5) Me-refresh grid setelah penyimpanan.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Validasi dilakukan sebelum membuka dialog. Kegagalan penyimpanan diteruskan ke ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Sesi Hibernate di dalam event listener menggunakan {@code HibernateUtil.currentSession()}
	 * yang terikat ke sesi ZK aktif. Pastikan tidak ada konflik sesi saat menyimpan massal.
	 *
	 * @param event Event ZK dari klik tombol tambah.
	 * @throws Exception Jika terjadi kesalahan saat membuka dialog atau menyimpan data.
	 */
	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		if (searchkelompokParameterTambahanPerbaikanAsset.getSelectedItem() == null
				|| searchkelompokParameterTambahanPerbaikanAsset.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kelompok Parameter belum dipilih. Langkah yang dapat dilakukan: (1) Pilih kelompok parameter dari dropdown di bagian filter; (2) Setelah kelompok dipilih, tombol Tambah akan bisa digunakan; (3) ulangi proses penambahan data. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanPerbaikanAsset.focus();
						}
					});
			return;
		}

		List<ParameterTambahan> parameterTambahans = initCriteria(false)
				.setProjection(Projections.groupProperty("parameterTambahan")).list();

		AmbilDataParameterTambahanBanyak window = new AmbilDataParameterTambahanBanyak(parameterTambahans);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("90%");
		window.setHeight("90%");

		window.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<ParameterTambahan> parameterTambahans = (List<ParameterTambahan>) arg0.getData();

				if (parameterTambahans != null) {
					Session session = HibernateUtil.currentSession();
					for (ParameterTambahan parameterTambahan : parameterTambahans) {

						ParameterTambahanPerbaikanAsset parameterTambahanPerbaikanAsset = new ParameterTambahanPerbaikanAsset();
						parameterTambahanPerbaikanAsset.setParameterTambahan(parameterTambahan);
						parameterTambahanPerbaikanAsset.setKelompokParameterTambahanPerbaikanAsset(
								(KelompokParameterTambahanPerbaikanAsset) (searchkelompokParameterTambahanPerbaikanAsset
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanPerbaikanAsset.getSelectedItem()
														.getValue()));

						session.save(parameterTambahanPerbaikanAsset);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	/**
	 * Menginisialisasi dan membangun form ubah parameter tambahan perbaikan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten {@code addWindow} dan membangun ulang form edit yang memungkinkan
	 * pengguna mengubah kelompok dan parameter dari entri yang sudah ada. Digunakan
	 * khusus untuk mode ubah (entitas sudah memiliki ID).<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi entitas dan mengatur judul jendela ("Tambah Parameter" atau "Ubah Parameter").
	 * (2) Membangun layout form dua kolom dengan Borderlayout, Center, dan South.
	 * (3) Baris pertama: combobox kelompok parameter (readonly, diisi dari database).
	 * (4) Baris kedua: combobox parameter tambahan dengan label tipe dan nilai data.
	 * (5) Toolbar dengan tombol Batal dan Simpan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Form ini bersifat minimal karena penambahan banyak parameter sekaligus dilakukan
	 * via dialog {@code AmbilDataParameterTambahanBanyak}. Jika field baru ditambahkan
	 * ke entitas, perbarui form ini dan metode {@code onSave}.
	 *
	 * @param parameterTambahanPerbaikanAsset Entitas yang akan diedit. Jika ID null,
	 *                                        form dalam mode tambah individual.
	 */
	private void init(ParameterTambahanPerbaikanAsset parameterTambahanPerbaikanAsset) {
		this.parameterTambahanPerbaikanAsset = parameterTambahanPerbaikanAsset;
		addWindow.setTitle(parameterTambahanPerbaikanAsset.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok"));
		Common.insertCombo(kelompokParameterTambahanPerbaikanAsset = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanPerbaikanAsset.class);
		Common.selectComboItem(kelompokParameterTambahanPerbaikanAsset,
				parameterTambahanPerbaikanAsset.getKelompokParameterTambahanPerbaikanAsset());
		row.appendChild(kelompokParameterTambahanPerbaikanAsset);
		kelompokParameterTambahanPerbaikanAsset.setWidth("90%");
		kelompokParameterTambahanPerbaikanAsset.setReadonly(true);

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanPerbaikanAsset.getParameterTambahan());

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
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Menyimpan perubahan parameter tambahan perbaikan aset ke database.
	 *
	 * <b>Tujuan:</b><br>
	 * Memvalidasi input form (kelompok dan parameter harus dipilih), lalu menyimpan
	 * atau memperbarui entitas {@code ParameterTambahanPerbaikanAsset} di database.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memvalidasi bahwa combobox kelompok parameter sudah dipilih; jika belum,
	 *     menampilkan pesan peringatan dan mengembalikan false.
	 * (2) Memvalidasi bahwa combobox parameter tambahan sudah dipilih; jika belum,
	 *     menampilkan pesan peringatan dan mengembalikan false.
	 * (3) Mengambil sesi Hibernate aktif.
	 * (4) Jika entitas sudah ada di database (mode ubah), me-load ulang untuk menghindari
	 *     stale object.
	 * (5) Mengisi field kelompok dan parameter dari pilihan combobox.
	 * (6) Memanggil {@code Common.refreshSaveOrUpdate} untuk commit ke database.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika penyimpanan berhasil. {@code false} jika validasi gagal.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Validasi manual untuk field wajib ditampilkan via {@code MyMessageboxConfig.show}.
	 * Exception Hibernate diteruskan ke ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika field baru ditambahkan (misal: urutan tampil), tambahkan setter di sini.
	 *
	 * @param event Event ZK dari klik tombol Simpan.
	 * @return {@code true} jika berhasil disimpan, {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan Hibernate saat menyimpan data.
	 */
	public boolean onSave(Event event) throws Exception {

		if (kelompokParameterTambahanPerbaikanAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kelompok Parameter belum dipilih. Langkah yang dapat dilakukan: (1) Pilih kelompok parameter dari dropdown yang tersedia; (2) Jika kelompok belum ada, tambahkan melalui menu Kelompok Parameter Perbaikan; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (parameterTambahan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Nama Parameter belum dipilih. Langkah yang dapat dilakukan: (1) Pilih nama parameter dari dropdown yang tersedia; (2) Jika parameter belum ada, tambahkan melalui menu konfigurasi parameter; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahanPerbaikanAsset.getId() != null) {
			parameterTambahanPerbaikanAsset = (ParameterTambahanPerbaikanAsset) session
					.load(ParameterTambahanPerbaikanAsset.class, parameterTambahanPerbaikanAsset.getId());

		}
		parameterTambahanPerbaikanAsset.setKelompokParameterTambahanPerbaikanAsset(
				(KelompokParameterTambahanPerbaikanAsset) (kelompokParameterTambahanPerbaikanAsset.getSelectedItem() == null
						? null
						: kelompokParameterTambahanPerbaikanAsset.getSelectedItem().getValue()));

		parameterTambahanPerbaikanAsset
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanPerbaikanAsset);

		return true;
	}

	/**
	 * Membangun Criteria Hibernate untuk kueri daftar parameter tambahan perbaikan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyusun filter Hibernate yang menggabungkan filter kelompok aktif dan filter
	 * nama parameter. Digunakan oleh {@code onSearchDefault} dan {@code onAdd}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria dari {@code ParameterTambahanPerbaikanAsset} dengan join alias
	 * ke {@code parameterTambahan}. Filter yang diterapkan:
	 * (1) Filter kelompok: jika combobox kelompok di filter sudah dipilih, membatasi
	 *     ke kelompok tersebut; jika belum, menampilkan semua (sqlRestriction "true").
	 * (2) Filter nama: ILIKE case-insensitive partial match pada field nama parameterTambahan.
	 * Jika {@code order} true, ditambahkan ORDER BY id descending.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini juga digunakan di {@code onAdd} dengan projection untuk mendapatkan
	 * daftar parameter yang sudah ada di kelompok aktif (untuk eksklusi di dialog tambah).
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC.
	 * @return Objek {@code Criteria} Hibernate siap dieksekusi.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanPerbaikanAsset.class).createAlias("parameterTambahan",
				"parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchkelompokParameterTambahanPerbaikanAsset.getSelectedItem() == null
				|| searchkelompokParameterTambahanPerbaikanAsset.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelompokParameterTambahanPerbaikanAsset",
								searchkelompokParameterTambahanPerbaikanAsset.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(),
								MatchMode.ANYWHERE));

		return criteria;
	}

	/**
	 * Memuat ulang data grid parameter tambahan berdasarkan kriteria filter saat ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi {@code DataSearchDefault}. Me-refresh tampilan grid daftar parameter
	 * tambahan perbaikan aset setelah perubahan filter, penambahan, perubahan, atau
	 * penghapusan data.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data dan memperbarui paginasi via {@code Common.initPaging}.
	 * (2) Mengeksekusi kueri dengan criteria terurut, dibatasi per halaman.
	 * (3) Menetapkan renderer dan model ke grid ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil dari berbagai titik: paginasi, tambah data, ubah data, hapus data,
	 * reset filter. Pastikan selalu terpanggil setelah operasi yang mengubah data.
	 *
	 * @param event Event ZK yang memicu pencarian ulang, bisa {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanPerbaikanAsset> parameterTambahanPerbaikanAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanPerbaikanAsset);
		grid.setRowRenderer(new ParameterTambahanPerbaikanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
