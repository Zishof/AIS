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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.asset.StatusAssetDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.StatusAsset;

/**
 * <h3>StatusAssetAction — Kontroler CRUD Master Status Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss untuk halaman manajemen data master Status
 * Aset ({@code StatusAsset}). Status aset adalah data referensi yang
 * merepresentasikan kondisi fisik atau operasional suatu aset pada titik waktu
 * tertentu, misalnya "Baik", "Rusak Ringan", "Rusak Berat", "Tidak Aktif",
 * "Dihapuskan", "Dipinjam", dan sebagainya. Data ini digunakan oleh modul-modul
 * lain seperti inventarisasi aset, pemeliharaan, dan pelaporan kondisi aset.
 * <br><br>
 * Berbeda dengan beberapa kontroler master aset lainnya, {@code StatusAssetAction}
 * menggunakan pola DAO ({@code StatusAssetDao} dari {@code DaoFactory}) untuk
 * operasi simpan dan ubah, bukan langsung menggunakan {@code Common.refreshSaveOrUpdate}.
 * Selain itu, kelas ini melakukan pemeriksaan keamanan yang lebih ketat di
 * {@code doAfterCompose} dengan memeriksa atribut sesi {@code usersTemp} dan hak
 * akses READ sebelum melanjutkan inisialisasi. Tombol aksi ubah dan hapus per baris
 * diimplementasikan secara langsung di renderer (bukan menggunakan
 * {@code Common.copyEditDeleteButtons}).
 *
 * <b>Cara kerja:</b><br>
 * Pemeriksaan keamanan berlapis: {@code doBeforeCompose} memeriksa autentikasi umum,
 * sedangkan {@code doAfterCompose} memeriksa {@code usersTemp} dan hak READ secara
 * eksplisit sebelum melanjutkan ke inisialisasi UI. Jika tidak lolos, pengguna di-logoff.
 * {@code StatusAssetRenderer} membangun tombol ubah dan hapus secara manual di setiap
 * baris dengan logika konfirmasi hapus inline. Penyimpanan data menggunakan
 * {@code StatusAssetDao} dari {@code DaoFactory} dengan pola load-update atau save
 * berdasarkan keberadaan ID.
 *
 * <b>Threading:</b><br>
 * Model single-thread ZKoss per request. Sesi Hibernate diakses via
 * {@code HibernateUtil.currentSession()} untuk query, dan via {@code StatusAssetDao}
 * (yang menggunakan factory DAO) untuk operasi simpan/ubah. Semua event listener
 * berjalan dalam thread ZK Desktop yang sama.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini tidak mengimplementasikan {@code DataCriteria}, {@code DataSearchDefault},
 * atau {@code DataInitDefault}, sehingga tidak mendukung fitur ekspor dan impor data
 * generik dari {@code Common}. Jika fitur tersebut diperlukan, implementasikan
 * antarmuka-antarmuka tersebut dan tambahkan inisialisasinya di {@code doAfterCompose}.
 * Perhatikan penggunaan DAO (bukan {@code Common.refreshSaveOrUpdate}) yang dapat
 * dipertimbangkan untuk dikonsolidasikan di masa mendatang.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class StatusAssetAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal ZKoss tempat formulir tambah/ubah status aset ditampilkan. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk navigasi halaman daftar data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar status aset. */
	private MyGrid grid;

	/** Textbox filter pencarian berdasarkan nama status aset. */
	private Textbox searchnama;

	/** Textbox input nama status aset pada formulir tambah/ubah. */
	private Textbox nama;

	/** Textbox input keterangan status aset pada formulir tambah/ubah. */
	private Textbox keterangan;

	/**
	 * Flag apakah pengguna saat ini memiliki hak ubah data.
	 * Diinisialisasi berdasarkan {@code CommonPrivilages.UPDATE}.
	 */
	private boolean edit = false;

	/**
	 * Flag apakah pengguna saat ini memiliki hak hapus data.
	 * Diinisialisasi berdasarkan {@code CommonPrivilages.DELETE}.
	 */
	private boolean delete = false;

	/**
	 * Entitas {@code StatusAsset} yang sedang dalam proses tambah atau ubah.
	 */
	private StatusAsset statusAsset;

	/** Tombol Tambah pada toolbar; visibilitas dikontrol oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses awal sebelum komponen ZKoss dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk memvalidasi
	 * autentikasi pengguna. Kemudian meneruskan ke implementasi induk untuk melanjutkan
	 * proses komposisi ZKoss. Jika keamanan tidak terpenuhi, pengguna diarahkan
	 * keluar sebelum UI dibangun.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK aktif</li>
	 *   <li>{@code parent} — komponen induk</li>
	 *   <li>{@code compInfo} — metadata komponen ZUL</li>
	 * </ul>
	 * <b>Return:</b> {@code ComponentInfo} dari implementasi induk.<br>
	 * <br>
	 * <b>Penanganan error:</b> Akses tidak sah ditangani oleh {@code Common.doCheckSecurity()}.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pemeriksaan ini adalah lapis pertama keamanan; wajib ada.
	 *
	 * @param page     halaman ZK aktif
	 * @param parent   komponen induk
	 * @param compInfo metadata komponen ZUL
	 * @return {@code ComponentInfo} dari implementasi induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan pengaturan awal
	 * halaman manajemen status aset dengan pemeriksaan keamanan berlapis.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk proses autowiring ZKoss.</li>
	 *   <li>Menginisialisasi dukungan multi-bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Memeriksa atribut sesi {@code usersTemp} dan hak READ; jika tidak ada
	 *       atau tidak berwenang, menghapus atribut sesi, memanggil
	 *       {@code Common.goLogoff()}, dan mengembalikan lebih awal (menghentikan
	 *       inisialisasi lebih lanjut).</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete}.</li>
	 *   <li>Menjalankan pencarian awal dan mendaftarkan listener paginasi.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root hasil komposisi ZKoss</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika pemeriksaan keamanan gagal, metode kembali lebih
	 * awal setelah memanggil {@code Common.goLogoff()}. Exception lain dilempar.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Kelas ini tidak mendaftarkan tombol cetak/ekspor (berbeda
	 * dari master lain). Jika diperlukan di masa mendatang, tambahkan setelah blok
	 * paginasi dengan pola yang sama.
	 *
	 * @param comp komponen root hasil komposisi ZKoss
	 * @throws Exception jika terjadi kesalahan inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * <h3>StatusAssetRenderer — Renderer Baris Grid Status Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas batin ini merender setiap baris data {@code StatusAsset} ke dalam
	 * komponen Row ZKoss pada grid utama. Menampilkan nama (dengan riwayat revisi),
	 * keterangan, dan tombol aksi ubah/hapus yang diimplementasikan secara langsung
	 * (tidak menggunakan {@code Common.copyEditDeleteButtons}).<br>
	 * <br>
	 * <b>Cara kerja:</b> Engine renderer ZKoss memanggil {@code render} untuk setiap
	 * elemen. Tombol Ubah membuka formulir edit di window modal. Tombol Hapus
	 * menampilkan konfirmasi dialog; jika dikonfirmasi, menghapus data via
	 * {@code Common.refreshDelete} dan memanggil {@code onSearchDefault}. Jika
	 * hapus gagal karena relasi FK, menampilkan pesan error.<br>
	 * <br>
	 * <b>Threading:</b> Single-thread ZKoss Desktop per event.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Implementasi tombol hapus langsung di sini (bukan via helper)
	 * memberikan fleksibilitas pesan error kustom, namun membuat kode lebih panjang.
	 */
	class StatusAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi komponen Row ZKoss dengan data satu record
		 * {@code StatusAsset} untuk ditampilkan dalam grid.<br>
		 * <br>
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan vertical alignment baris ke "top".</li>
		 *   <li>Menambahkan komponen riwayat revisi nama via
		 *       {@code RevisiHelper.createNewRevisi}.</li>
		 *   <li>Menambahkan Label keterangan.</li>
		 *   <li>Membangun Hbox toolbar per baris berisi tombol Ubah (ikon edit) dan
		 *       tombol Hapus (ikon trash); visibilitas masing-masing dikontrol oleh
		 *       flag {@code edit} dan {@code delete}.</li>
		 *   <li>Tombol Ubah membuka formulir edit di window modal via {@code init}
		 *       dan {@code addWindow.onModal()}.</li>
		 *   <li>Tombol Hapus menampilkan dialog konfirmasi; jika OK dipilih,
		 *       menghapus data via {@code Common.refreshDelete} dan memperbarui grid;
		 *       jika gagal karena relasi, menampilkan pesan error detail.</li>
		 * </ol>
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Row ZKoss yang akan diisi</li>
		 *   <li>{@code arg1} — objek data; harus dapat di-cast ke {@code StatusAsset}</li>
		 * </ul>
		 * <b>Return:</b> void.<br>
		 * <br>
		 * <b>Penanganan error:</b> Kesalahan hapus ditangkap dan ditampilkan via
		 * {@code Common.tampilErrorJikaAdmin} dan pesan dialog; exception rendering
		 * dipropagasi ke engine ZKoss.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Perhatikan bahwa kelas ini menggunakan {@code new Integer}
		 * (deprecated di Java 9+) untuk parsing event data; pertimbangkan mengganti
		 * dengan {@code Integer.parseInt} jika diupgrade ke Java yang lebih baru.
		 *
		 * @param arg0 baris Row ZKoss target
		 * @param arg1 objek {@code StatusAsset} yang akan dirender
		 * @throws Exception jika rendering gagal
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final StatusAsset statusAsset = (StatusAsset) arg1;

			RevisiHelper.createNewRevisi(StatusAsset.class, statusAsset,
					statusAsset.getNama()).setParent(arg0);
			new Label(statusAsset.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(statusAsset);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(statusAsset);

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
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar utama,
	 * membuka formulir tambah status aset baru dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans {@code StatusAsset} kosong baru,
	 * memanggil {@code init(StatusAsset)} untuk membangun formulir, kemudian
	 * menampilkan {@code addWindow} sebagai dialog modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol tambah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi formulir gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onAdd} di ZUL; nama tidak boleh diubah.
	 *
	 * @param event objek event dari klik tombol tambah
	 * @throws Exception jika inisialisasi formulir gagal
	 */
	public void onAdd(Event event) throws Exception {
		init(new StatusAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengisi formulir tambah/ubah status aset
	 * di dalam window modal {@code addWindow}.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instans.</li>
	 *   <li>Mengatur judul window: "Tambah StatusAsset" atau "Ubah StatusAsset".</li>
	 *   <li>Membersihkan konten window dari formulir sebelumnya.</li>
	 *   <li>Membangun layout dengan area Center (formulir grid dua kolom: label 35%
	 *       dan input) serta area South (toolbar tombol Batal dan Simpan).</li>
	 *   <li>Formulir memiliki dua baris: Nama Status Asset (wajib diisi, dengan
	 *       penanganan null menggunakan string kosong sebagai fallback) dan
	 *       Keterangan (textarea 3 baris).</li>
	 *   <li>Tombol Batal menutup modal; tombol Simpan memanggil {@code onSave}
	 *       dan menutup modal jika berhasil serta memperbarui grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code statusAsset} — entitas yang akan ditampilkan di formulir;
	 *       ID null = tambah baru, ID tidak null = ubah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar ke atas jika inisialisasi
	 * komponen ZKoss gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Formulir ini tidak memiliki field kode (berbeda dari
	 * master aset lain). Jika field kode perlu ditambahkan, tambahkan baris form
	 * dan logika simpan yang sesuai.
	 *
	 * @param statusAsset entitas yang akan ditampilkan atau diedit di formulir
	 */
	private void init(StatusAsset statusAsset) {
		this.statusAsset = statusAsset;
		addWindow.setTitle(statusAsset.getId() == null ? "Tambah StatusAsset" : "Ubah StatusAsset");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Status Asset"));
		row.appendChild(nama = new Textbox(statusAsset.getNama() == null ? ""
				: statusAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				statusAsset.getKeterangan() == null ? "" : statusAsset
						.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

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
	 * <b>Tujuan:</b> Memvalidasi dan menyimpan data status aset ke database
	 * menggunakan pola DAO, untuk operasi tambah baru maupun ubah data yang ada.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah nama diisi; jika kosong, menampilkan peringatan
	 *       "Nama Status Asset harus diisi" dan mengembalikan {@code false}.</li>
	 *   <li>Memeriksa duplikasi nama via {@code checkNamaStatusAsset()}; jika
	 *       ditemukan, menampilkan peringatan dan mengembalikan {@code false}.</li>
	 *   <li>Mendapatkan DAO via {@code DaoFactory.getInstance().getStatusAssetDao()}.</li>
	 *   <li>Jika record sudah ada (ID tidak null), memuat ulang dari database
	 *       via DAO {@code load} method.</li>
	 *   <li>Menetapkan nilai nama dan keterangan dari formulir ke entitas.</li>
	 *   <li>Menggunakan {@code statusAssetDao.update} jika ID tidak null, atau
	 *       {@code statusAssetDao.save} jika ID null (data baru).</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol Simpan</li>
	 * </ul>
	 * <b>Return:</b> {@code boolean} — {@code true} jika penyimpanan berhasil;
	 * {@code false} jika validasi gagal.<br>
	 * <br>
	 * <b>Penanganan error:</b> Validasi menampilkan dialog peringatan; exception
	 * dari DAO tidak ditangkap dan dipropagasi ke atas. Berbeda dari master lain
	 * yang menggunakan {@code Common.refreshSaveOrUpdate}, kelas ini menggunakan
	 * DAO pattern secara eksplisit.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pertimbangkan konsolidasi ke {@code Common.refreshSaveOrUpdate}
	 * untuk konsistensi dengan master aset lain. Formulir ini tidak memiliki field kode.
	 *
	 * @param event objek event dari aksi simpan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Status Aset belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama status aset yang sesuai; (2) Status aset digunakan untuk menandai kondisi fisik dan kepemilikan aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaStatusAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Status Asset sudah ada di database",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		StatusAssetDao statusAssetDao = DaoFactory.getInstance()
				.getStatusAssetDao();
		if (statusAsset.getId() != null) {
			statusAsset = statusAssetDao.load(statusAsset.getId());

		}

		statusAsset.setNama(nama.getValue());
		statusAsset.setKeterangan(keterangan.getValue());

		if (statusAsset.getId() != null) {
			statusAssetDao.update(statusAsset);
		} else {
			statusAssetDao.save(statusAsset);
		}

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria query Hibernate untuk mengambil daftar
	 * {@code StatusAsset} sesuai filter pencarian nama yang aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil sesi Hibernate saat ini via {@code HibernateUtil.currentSession()}.</li>
	 *   <li>Membuat criteria untuk {@code StatusAsset} (tanpa filter aktif karena
	 *       kelas ini tidak memiliki checkbox filter aktif).</li>
	 *   <li>Menambahkan ORDER BY nama ascending jika {@code order} true.</li>
	 *   <li>Menambahkan filter {@code ilike} pada nama berdasarkan nilai
	 *       {@code searchnama} (selalu diterapkan, termasuk jika kosong yang
	 *       berarti menampilkan semua karena {@code ilike("nama", "", ANYWHERE)}
	 *       cocok dengan semua nilai).</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — {@code true} untuk menyertakan ORDER BY nama</li>
	 * </ul>
	 * <b>Return:</b> {@code Criteria} Hibernate yang siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke pemanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Berbeda dari master lain yang memiliki guard untuk string
	 * kosong ({@code isEmpty() ? sqlRestriction("true") : ilike(...)}), kelas ini
	 * langsung menggunakan ilike tanpa guard. Jika searchnama kosong, {@code ilike}
	 * dengan pola "%" + "" + "%" cocok dengan semua baris.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan; {@code false} untuk COUNT
	 * @return kriteria Hibernate yang dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(StatusAsset.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(Restrictions.ilike("nama", searchnama.getValue(),
				MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian dan memperbarui grid tampilan status aset
	 * berdasarkan filter pencarian nama yang aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memperbarui total paginasi via {@code Common.initPaging}.</li>
	 *   <li>Mengambil daftar {@code StatusAsset} dengan batas per halaman dan offset
	 *       sesuai halaman aktif paginasi.</li>
	 *   <li>Membungkus hasil dalam {@code SimpleListModel}.</li>
	 *   <li>Menetapkan renderer {@code StatusAssetRenderer} dan memperbarui model grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — event pemicu; boleh null untuk pemanggilan programatik</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini dipanggil dari beberapa tempat: inisialisasi awal,
	 * setelah simpan, setelah hapus, dan dari listener paginasi. Terikat ke event
	 * {@code onSearchDefault} di ZUL.
	 *
	 * @param event event pemicu pencarian; boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<StatusAsset> statusAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(statusAsset);
		grid.setRowRenderer(new StatusAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama status aset yang dimasukkan di formulir
	 * sudah ada di database (deteksi duplikasi nama).<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menjalankan query COUNT pada {@code StatusAsset} dengan kondisi nama
	 *       sama persis (setelah trim) dengan nilai input {@code nama}.</li>
	 *   <li>Mengecualikan record yang sedang diedit (jika ID tidak null) menggunakan
	 *       {@code Restrictions.ne("id", ...)} agar operasi ubah tidak mendeteksi
	 *       diri sendiri sebagai duplikat.</li>
	 * </ol>
	 * <b>Parameter:</b> tidak ada (membaca dari field instans {@code nama}
	 * dan {@code statusAsset}).<br>
	 * <b>Return:</b> {@code Boolean} — {@code true} jika nama sudah ada di database
	 * (duplikat); {@code false} jika nama unik dan dapat digunakan.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pemeriksaan exact match case-sensitive. Berbeda dari kelas
	 * saudara yang bernama {@code checkNamaStatusPenyediaAsset}, metode ini
	 * mengoperasikan entitas {@code StatusAsset} yang berbeda.
	 *
	 * @return {@code true} jika nama sudah digunakan; {@code false} jika nama unik
	 */
	public Boolean checkNamaStatusAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session
				.createCriteria(StatusAsset.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.statusAsset.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						this.statusAsset.getId())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
