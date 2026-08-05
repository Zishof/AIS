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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.KategoriAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>KategoriAssetAction — Kontroler CRUD Master Kategori Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss untuk halaman manajemen data master Kategori
 * Aset ({@code KategoriAsset}). Kategori aset digunakan untuk mengklasifikasikan
 * aset tetap maupun aset habis pakai ke dalam kelompok-kelompok tertentu, misalnya
 * "Peralatan Kantor", "Kendaraan", "Infrastruktur IT", "Furniture", "Peralatan Lab",
 * dan sebagainya. Kategorisasi ini penting untuk keperluan laporan inventaris,
 * penyusutan aset, dan pengelompokan dalam sistem ERP.
 * <br><br>
 * Kelas ini juga mendukung atribut {@code feeder} yang kemungkinan digunakan untuk
 * integrasi dengan sistem pelaporan eksternal (feeder dikti atau sistem akuntansi).
 * Kontroler menyediakan operasi CRUD lengkap dengan kontrol hak akses berbasis peran
 * menggunakan {@code CommonPrivilages}, filter pencarian berdasarkan nama dan status
 * aktif, paginasi, serta ekspor dan impor data.
 *
 * <b>Cara kerja:</b><br>
 * Mengikuti pola standar kontroler master AIS: pemeriksaan keamanan di
 * {@code doBeforeCompose}, inisialisasi UI dan hak akses di {@code doAfterCompose},
 * rendering data oleh kelas batin {@code KategoriAssetRenderer}, pembukaan formulir
 * modal via {@code onAdd} dan {@code init}, validasi duplikasi nama serta penyimpanan
 * di {@code onSave}, dan query data via {@code initCriteria} dan
 * {@code onSearchDefault}. Array ekspor mencakup kolom tambahan {@code feeder}
 * yang membedakan kelas ini dari master aset lainnya.
 *
 * <b>Threading:</b><br>
 * Model single-thread ZKoss per request. Tidak ada operasi multithreading.
 * Sesi Hibernate dikelola via {@code HibernateUtil.currentSession()}.
 *
 * <b>Pemeliharaan:</b><br>
 * Jika perlu menambah field klasifikasi tambahan (misalnya kelompok SIMAK BMN atau
 * kode akun penyusutan), tambahkan pada entitas {@code KategoriAsset}, formulir
 * {@code init}, logika simpan {@code onSave}, renderer {@code KategoriAssetRenderer},
 * dan array {@code contents} di {@code doAfterCompose}.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class KategoriAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal ZKoss tempat formulir tambah/ubah kategori aset ditampilkan. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk navigasi halaman daftar data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar kategori aset. */
	private MyGrid grid;

	/** Textbox filter pencarian berdasarkan nama kategori. */
	private Textbox searchnama;

	/** Checkbox filter pencarian berdasarkan status aktif. */
	private Checkbox searchaktif;

	/** Textbox input nama kategori aset pada formulir tambah/ubah. */
	private Textbox nama;

	/** Textbox input keterangan kategori aset pada formulir tambah/ubah. */
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
	 * Entitas {@code KategoriAsset} yang sedang dalam proses tambah atau ubah.
	 */
	private KategoriAsset kategoriAsset;

	/** Tombol Tambah pada toolbar; visibilitas dikontrol oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Textbox input kode kategori aset pada formulir tambah/ubah. */
	private Textbox kode;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZKoss dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk memvalidasi
	 * sesi dan hak akses. Jika tidak lolos, pengguna diarahkan ke halaman login.
	 * Meneruskan ke {@code super.doBeforeCompose} untuk melanjutkan proses ZKoss.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK aktif</li>
	 *   <li>{@code parent} — komponen induk dalam hierarki ZK</li>
	 *   <li>{@code compInfo} — metadata komponen dari berkas ZUL</li>
	 * </ul>
	 * <b>Return:</b> {@code ComponentInfo} dari implementasi induk.<br>
	 * <br>
	 * <b>Penanganan error:</b> Akses tidak sah ditangani oleh {@code Common.doCheckSecurity()}.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Wajib ada; jangan dihapus.
	 *
	 * @param page     halaman ZK aktif
	 * @param parent   komponen induk
	 * @param compInfo metadata komponen ZUL
	 * @return {@code ComponentInfo} dari implementasi induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan pengaturan awal
	 * halaman manajemen kategori aset.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZKoss.</li>
	 *   <li>Menginisialisasi dukungan multi-bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Menampilkan/menyembunyikan tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete}.</li>
	 *   <li>Menjalankan pencarian awal dan mendaftarkan listener paginasi.</li>
	 *   <li>Menyiapkan tombol cetak/ekspor dengan kolom: id, kode, nama, keterangan,
	 *       aktif, feeder (kolom feeder khusus untuk integrasi eksternal).</li>
	 *   <li>Menyiapkan tombol unggah/impor yang hanya terlihat jika memiliki hak
	 *       CREATE, UPDATE, dan DELETE sekaligus.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root hasil komposisi ZKoss</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Array {@code contents} mencakup "feeder" yang khas untuk
	 * kategori aset. Perbarui jika entitas bertambah kolom.
	 *
	 * @param comp komponen root hasil komposisi ZKoss
	 * @throws Exception jika terjadi kesalahan inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif", "feeder" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KategoriAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KategoriAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>KategoriAssetRenderer — Renderer Baris Grid Kategori Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas batin ini merender setiap baris data {@code KategoriAsset} ke dalam
	 * komponen Row ZKoss pada grid utama halaman. Menampilkan kode, nama (dengan
	 * riwayat revisi), keterangan, checkbox aktif interaktif, dan tombol aksi
	 * salin/ubah/hapus.<br>
	 * <br>
	 * <b>Cara kerja:</b> Dipanggil oleh engine renderer ZKoss untuk setiap elemen
	 * model data. Checkbox aktif menggunakan listener onCheck yang menyimpan
	 * perubahan langsung ke database via {@code Common.refreshSaveOrUpdate}.<br>
	 * <br>
	 * <b>Threading:</b> Single-thread ZKoss Desktop per event.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan komponen harus sesuai dengan kolom di ZUL.
	 */
	class KategoriAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi komponen Row ZKoss dengan data satu record
		 * {@code KategoriAsset} untuk ditampilkan dalam grid.<br>
		 * <br>
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan vertical alignment baris ke "top".</li>
		 *   <li>Menambahkan Label kode kategori aset.</li>
		 *   <li>Menambahkan komponen riwayat revisi nama via
		 *       {@code RevisiHelper.createNewRevisi}.</li>
		 *   <li>Menambahkan Label keterangan.</li>
		 *   <li>Menambahkan checkbox "Aktif" yang dapat diubah langsung; perubahan
		 *       tersimpan otomatis ke database.</li>
		 *   <li>Menambahkan tombol aksi salin/ubah/hapus.</li>
		 * </ol>
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Row ZKoss yang akan diisi komponen</li>
		 *   <li>{@code arg1} — objek data; harus dapat di-cast ke {@code KategoriAsset}</li>
		 * </ul>
		 * <b>Return:</b> void.<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dipropagasi ke engine renderer ZKoss.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Urutan komponen harus sesuai urutan kolom di ZUL.
		 * Kolom feeder tidak ditampilkan di grid (hanya di ekspor).
		 *
		 * @param arg0 baris Row ZKoss target
		 * @param arg1 objek {@code KategoriAsset} yang akan dirender
		 * @throws Exception jika rendering gagal
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KategoriAsset kategoriAsset = (KategoriAsset) arg1;
			new Label(kategoriAsset.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KategoriAsset.class, kategoriAsset, kategoriAsset.getNama()).setParent(arg0);
			new Label(kategoriAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kategoriAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kategoriAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kategoriAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kategoriAsset, KategoriAssetAction.this).setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar utama,
	 * membuka formulir tambah kategori aset baru dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans {@code KategoriAsset} kosong baru,
	 * memanggil {@code init(KategoriAsset)} untuk membangun formulir kosong,
	 * kemudian menampilkan {@code addWindow} sebagai dialog modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol tambah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi formulir gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onAdd} di ZUL. Nama tidak boleh diubah.
	 *
	 * @param event objek event dari klik tombol tambah
	 * @throws Exception jika inisialisasi formulir gagal
	 */
	public void onAdd(Event event) throws Exception {
		init(new KategoriAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code DataInitDefault} untuk membuka
	 * formulir ubah/salin dari konteks eksternal (tombol ubah atau salin di grid).<br>
	 * <br>
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject}, melakukan cast ke
	 * {@code KategoriAsset}, menyimpan ke field instans, memanggil
	 * {@code init(KategoriAsset)} untuk membangun formulir dengan data terisi, lalu
	 * menampilkan {@code addWindow} sebagai modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code obj} — entitas yang akan diedit; harus bertipe {@code KategoriAsset}</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code ClassCastException} jika tipe tidak sesuai.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Dipanggil oleh infrastruktur {@code Common};
	 * tanda tangan tidak boleh diubah.
	 *
	 * @param obj entitas yang akan diedit; harus bertipe {@code KategoriAsset}
	 * @throws Exception jika cast atau inisialisasi gagal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kategoriAsset = (KategoriAsset) obj;
		init(kategoriAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengisi formulir tambah/ubah kategori aset
	 * di dalam window modal {@code addWindow}.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instans.</li>
	 *   <li>Mengatur judul window: "Tambah Kategori" atau "Ubah Kategori".</li>
	 *   <li>Membersihkan konten window dari formulir sebelumnya.</li>
	 *   <li>Membangun layout dengan area Center (formulir grid dua kolom) dan
	 *       South (toolbar tombol aksi).</li>
	 *   <li>Formulir memiliki tiga baris: Kode Kategori, Nama Kategori, dan
	 *       Keterangan (textarea 3 baris).</li>
	 *   <li>Tombol Batal menutup modal; tombol Simpan memanggil {@code onSave}
	 *       dan menutup modal jika berhasil serta memperbarui grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code kategoriAsset} — entitas yang akan ditampilkan di formulir;
	 *       ID null = tambah baru, ID tidak null = ubah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar ke atas jika inisialisasi
	 * komponen ZKoss gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Field formulir ({@code kode}, {@code nama},
	 * {@code keterangan}) disimpan sebagai field instans untuk diakses di
	 * {@code onSave}. Kolom feeder tidak ditampilkan di formulir ini.
	 *
	 * @param kategoriAsset entitas yang akan ditampilkan atau diedit di formulir
	 */
	private void init(KategoriAsset kategoriAsset) {
		this.kategoriAsset = kategoriAsset;
		addWindow.setTitle(kategoriAsset.getId() == null ? "Tambah Kategori" : "Ubah Kategori");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kategori"));
		row.appendChild(kode = new Textbox(kategoriAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kategori"));
		row.appendChild(nama = new Textbox(kategoriAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kategoriAsset.getKeterangan()));
		keterangan.setWidth("90%");
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
	 * <b>Tujuan:</b> Memvalidasi dan menyimpan data kategori aset ke database,
	 * untuk operasi tambah baru maupun ubah data yang sudah ada.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah nama diisi; jika kosong, menampilkan peringatan
	 *       "Nama Kategori harus diisi" dan mengembalikan {@code false}.</li>
	 *   <li>Memeriksa duplikasi nama via {@code checkNamaKategoriAsset()};
	 *       jika ditemukan, menampilkan peringatan dan mengembalikan {@code false}.</li>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Jika record sudah ada (ID tidak null), memuat ulang dari database
	 *       via {@code session.load} agar terikat pada sesi aktif.</li>
	 *   <li>Menetapkan nilai kode, nama, dan keterangan dari formulir ke entitas.</li>
	 *   <li>Menyimpan atau memperbarui via {@code Common.refreshSaveOrUpdate}.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol Simpan</li>
	 * </ul>
	 * <b>Return:</b> {@code boolean} — {@code true} jika penyimpanan berhasil;
	 * {@code false} jika validasi gagal.<br>
	 * <br>
	 * <b>Penanganan error:</b> Validasi menampilkan dialog peringatan; exception
	 * Hibernate tidak ditangkap dan dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Catatan: field {@code feeder} pada entitas tidak
	 * diisi melalui formulir ini; jika perlu dikelola, tambahkan input dan
	 * assignment di sini.
	 *
	 * @param event objek event dari aksi simpan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kategori belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama Kategori dengan nama yang jelas dan deskriptif; (2) Nama kategori digunakan untuk mengklasifikasikan aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKategoriAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Kategori sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kategoriAsset.getId() != null) {
			kategoriAsset = (KategoriAsset) session.load(KategoriAsset.class, kategoriAsset.getId());

		}

		kategoriAsset.setKode(kode.getValue());
		kategoriAsset.setNama(nama.getValue());
		kategoriAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kategoriAsset);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria query Hibernate untuk mengambil daftar
	 * {@code KategoriAsset} sesuai filter UI yang aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat criteria untuk {@code KategoriAsset} dengan filter aktif
	 *       (jika checkbox null atau dicentang: hanya aktif/null; jika tidak: semua).</li>
	 *   <li>Menambahkan ORDER BY nama ascending jika {@code order} true.</li>
	 *   <li>Menambahkan filter ilike nama jika {@code searchnama} tidak kosong.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — {@code true} untuk menyertakan ORDER BY nama</li>
	 * </ul>
	 * <b>Return:</b> {@code Criteria} Hibernate yang siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan filter berdasarkan kode atau feeder jika
	 * dibutuhkan di masa mendatang.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan; {@code false} untuk COUNT
	 * @return kriteria Hibernate yang dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KategoriAsset.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian dan memperbarui grid tampilan kategori aset
	 * berdasarkan filter yang aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memperbarui total paginasi via {@code Common.initPaging}.</li>
	 *   <li>Mengambil daftar {@code KategoriAsset} dengan batas per halaman dan
	 *       offset sesuai halaman aktif.</li>
	 *   <li>Membungkus hasil dalam {@code SimpleListModel}.</li>
	 *   <li>Menetapkan renderer {@code KategoriAssetRenderer} dan memperbarui
	 *       model grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — event pemicu; boleh null untuk pemanggilan programatik</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onSearchDefault} di ZUL.
	 *
	 * @param event event pemicu pencarian; boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KategoriAsset> kategoriAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kategoriAsset);
		grid.setRowRenderer(new KategoriAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama kategori aset yang dimasukkan di formulir
	 * sudah ada di database (deteksi duplikasi).<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menjalankan query COUNT pada {@code KategoriAsset} dengan kondisi nama
	 *       sama persis (setelah trim) dengan nilai input {@code nama}.</li>
	 *   <li>Mengecualikan record yang sedang diedit (jika ID tidak null) menggunakan
	 *       {@code Restrictions.ne("id", ...)} agar operasi ubah tidak mendeteksi
	 *       diri sendiri sebagai duplikat.</li>
	 * </ol>
	 * <b>Parameter:</b> tidak ada (membaca dari field instans {@code nama}
	 * dan {@code kategoriAsset}).<br>
	 * <b>Return:</b> {@code Boolean} — {@code true} jika duplikat ditemukan;
	 * {@code false} jika nama unik.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pemeriksaan berdasarkan nama exact match; pertimbangkan
	 * menambah pemeriksaan kode unik jika diperlukan.
	 *
	 * @return {@code true} jika nama sudah ada di database; {@code false} jika unik
	 */
	public Boolean checkNamaKategoriAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KategoriAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kategoriAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kategoriAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
