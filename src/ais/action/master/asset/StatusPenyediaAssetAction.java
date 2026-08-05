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
import ais.database.model.asset.StatusPenyediaAsset;
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
 * <h3>StatusPenyediaAssetAction — Kontroler CRUD Master Status Penyedia Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss untuk halaman manajemen data master
 * Status Penyedia Aset ({@code StatusPenyediaAsset}). Status penyedia aset adalah
 * referensi yang mengklasifikasikan kondisi atau status keterlibatan vendor/penyedia
 * dalam pengadaan aset, misalnya "Aktif", "Diblokir", "Dalam Evaluasi", dan sebagainya.
 * Kontroler ini menyediakan operasi lengkap CRUD (Create, Read, Update, Delete) dengan
 * kontrol hak akses berbasis peran (role-based access control) menggunakan
 * {@code CommonPrivilages}. Data ditampilkan dalam grid yang dapat difilter berdasarkan
 * nama dan status aktif, dengan dukungan paginasi dan ekspor/impor data.
 *
 * <b>Cara kerja:</b><br>
 * Saat halaman dimuat, {@code doBeforeCompose} memeriksa keamanan akses. Kemudian
 * {@code doAfterCompose} menginisialisasi komponen UI, mengatur visibilitas tombol tambah
 * berdasarkan hak CREATE, menentukan flag {@code edit} dan {@code delete} berdasarkan
 * hak UPDATE dan DELETE, menjalankan pencarian awal via {@code onSearchDefault}, dan
 * mendaftarkan listener paginasi. Fitur ekspor data (cetak) diinisialisasi via
 * {@code Common.cetakData}, dan fitur unggah (import) via {@code Common.uploadData}
 * yang hanya terlihat jika pengguna memiliki hak CREATE, UPDATE, dan DELETE sekaligus.
 * Kelas batin {@code StatusPenyediaAssetRenderer} merender setiap baris grid dengan
 * kolom kode, nama (dengan tautan revisi), keterangan, checkbox aktif yang dapat diubah
 * langsung, serta tombol salin/ubah/hapus. Formulir tambah/ubah dibuat secara programatik
 * di metode {@code init(StatusPenyediaAsset)} dalam window modal {@code addWindow}.
 * Validasi nama wajib diisi dan tidak boleh duplikat dilakukan di {@code onSave}.
 *
 * <b>Threading:</b><br>
 * Mengikuti model single-thread ZKoss per request UI. Tidak ada operasi latar belakang
 * atau multithreading eksplisit. Sesi Hibernate diakses via
 * {@code HibernateUtil.currentSession()} yang terikat pada thread request saat ini.
 * Listener event (onClick, onCheck) dieksekusi dalam thread ZK Desktop yang sama.
 *
 * <b>Pemeliharaan:</b><br>
 * Untuk menambah kolom baru pada entitas {@code StatusPenyediaAsset}, tambahkan baris
 * form di metode {@code init(StatusPenyediaAsset)}, logika simpan di {@code onSave},
 * dan render di {@code StatusPenyediaAssetRenderer.render}. Array {@code contents} di
 * {@code doAfterCompose} mengontrol kolom yang diekspor; perbarui jika ada kolom baru.
 * Validasi duplikasi nama dilakukan via query {@code checkNamaStatusPenyediaAsset};
 * jika perlu validasi kode juga, tambahkan metode serupa.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class StatusPenyediaAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini.
	 * Diperlukan oleh mekanisme serialisasi Java dan tidak boleh diubah
	 * secara sembarangan.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal ZKoss tempat formulir tambah/ubah ditampilkan. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk navigasi halaman daftar data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar status penyedia aset. */
	private MyGrid grid;

	/** Textbox untuk filter pencarian berdasarkan nama status penyedia. */
	private Textbox searchnama;

	/** Checkbox untuk filter pencarian berdasarkan status aktif. */
	private Checkbox searchaktif;

	/** Textbox input nama status penyedia pada formulir tambah/ubah. */
	private Textbox nama;

	/** Textbox input keterangan status penyedia pada formulir tambah/ubah. */
	private Textbox keterangan;

	/**
	 * Flag apakah pengguna saat ini memiliki hak akses ubah data.
	 * Diinisialisasi berdasarkan {@code CommonPrivilages.UPDATE} di {@code doAfterCompose}.
	 */
	private boolean edit = false;

	/**
	 * Flag apakah pengguna saat ini memiliki hak akses hapus data.
	 * Diinisialisasi berdasarkan {@code CommonPrivilages.DELETE} di {@code doAfterCompose}.
	 */
	private boolean delete = false;

	/**
	 * Entitas {@code StatusPenyediaAsset} yang sedang dalam proses tambah atau ubah.
	 * Bernilai null jika tidak ada operasi yang sedang berlangsung.
	 */
	private StatusPenyediaAsset statusPenyediaAsset;

	/** Tombol Tambah pada toolbar utama; visibilitasnya dikontrol oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Textbox input kode status penyedia pada formulir tambah/ubah. */
	private Textbox kode;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZKoss dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk memvalidasi
	 * sesi dan hak akses pengguna. Jika tidak lolos, eksekusi dihentikan atau
	 * pengguna diarahkan ke halaman login. Kemudian meneruskan ke implementasi
	 * induk {@code super.doBeforeCompose}.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK aktif</li>
	 *   <li>{@code parent} — komponen induk dalam hierarki ZK</li>
	 *   <li>{@code compInfo} — metadata komponen dari ZUL</li>
	 * </ul>
	 * <b>Return:</b> {@code ComponentInfo} dari implementasi induk.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code Common.doCheckSecurity()} menangani pengalihan
	 * dan exception akses tidak sah.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jangan hilangkan pemeriksaan keamanan ini.
	 *
	 * @param page     halaman ZK aktif
	 * @param parent   komponen induk
	 * @param compInfo metadata komponen ZUL
	 * @return {@code ComponentInfo} dari induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan pengaturan awal
	 * halaman manajemen status penyedia aset.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk proses autowiring ZKoss.</li>
	 *   <li>Menginisialisasi dukungan multi-bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Menampilkan atau menyembunyikan tombol "Tambah" berdasarkan hak CREATE.</li>
	 *   <li>Menyetel flag {@code edit} dan {@code delete} sesuai hak UPDATE/DELETE.</li>
	 *   <li>Menjalankan pencarian awal {@code onSearchDefault(null)} untuk mengisi grid.</li>
	 *   <li>Mendaftarkan listener paginasi yang memanggil {@code onSearchDefault}
	 *       setiap halaman berganti.</li>
	 *   <li>Membuat tombol cetak/ekspor via {@code Common.cetakData} dan menambahkannya
	 *       ke toolbar.</li>
	 *   <li>Membuat tombol unggah/impor via {@code Common.uploadData}; hanya terlihat
	 *       jika pengguna memiliki hak tambah, ubah, dan hapus sekaligus.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root hasil komposisi ZKoss</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi komponen gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Array {@code contents} menentukan kolom yang diekspor
	 * dan diimpor. Perbarui jika skema entitas berubah.
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(StatusPenyediaAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, StatusPenyediaAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>StatusPenyediaAssetRenderer — Renderer Baris Grid Status Penyedia Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas batin ini bertanggung jawab untuk merender setiap baris data
	 * {@code StatusPenyediaAsset} ke dalam komponen ZKoss Row yang ditampilkan
	 * pada grid utama halaman. Setiap baris menampilkan kode, nama dengan riwayat
	 * revisi, keterangan, checkbox status aktif yang dapat diubah langsung, serta
	 * tombol aksi salin/ubah/hapus.<br>
	 * <br>
	 * <b>Cara kerja:</b> Dipanggil oleh engine rendering ZKoss untuk setiap elemen
	 * model data. Menerima objek {@code StatusPenyediaAsset} dari parameter {@code arg1}
	 * dan mengisi komponen Row ({@code arg0}) dengan label dan kontrol interaktif.
	 * Checkbox aktif menggunakan listener {@code onCheck} yang langsung menyimpan
	 * perubahan ke database via {@code Common.refreshSaveOrUpdate}. Tombol aksi
	 * dibuat oleh {@code Common.copyEditDeleteButtons} yang sudah menangani logika
	 * salin, buka form ubah, dan konfirmasi hapus.<br>
	 * <br>
	 * <b>Threading:</b> Berjalan dalam thread ZK Desktop event tunggal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika kolom grid di ZUL bertambah, tambahkan komponen
	 * Label atau kontrol baru di metode {@code render} dengan urutan yang sesuai.
	 */
	class StatusPenyediaAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi komponen Row ZKoss dengan data satu record
		 * {@code StatusPenyediaAsset} untuk ditampilkan dalam grid.<br>
		 * <br>
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan vertical alignment baris ke "top".</li>
		 *   <li>Menambahkan Label kode status penyedia.</li>
		 *   <li>Menambahkan komponen revisi nama via {@code RevisiHelper.createNewRevisi}
		 *       yang memungkinkan melihat riwayat perubahan data.</li>
		 *   <li>Menambahkan Label keterangan.</li>
		 *   <li>Menambahkan checkbox "Aktif" yang dinonaktifkan jika pengguna tidak
		 *       punya hak ubah; perubahan checkbox langsung disimpan ke database.</li>
		 *   <li>Menambahkan tombol aksi (salin, ubah, hapus) via
		 *       {@code Common.copyEditDeleteButtons}.</li>
		 * </ol>
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — baris Row ZKoss yang akan diisi komponen</li>
		 *   <li>{@code arg1} — objek data; harus dapat di-cast ke
		 *       {@code StatusPenyediaAsset}</li>
		 * </ul>
		 * <b>Return:</b> void.<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dilempar ke engine renderer ZKoss
		 * jika terjadi masalah.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Urutan penambahan komponen ke {@code arg0} harus
		 * sesuai dengan urutan kolom yang didefinisikan di berkas ZUL.
		 *
		 * @param arg0 baris Row ZKoss target
		 * @param arg1 objek data {@code StatusPenyediaAsset} yang akan dirender
		 * @throws Exception jika terjadi kesalahan rendering
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final StatusPenyediaAsset statusPenyediaAsset = (StatusPenyediaAsset) arg1;
			new Label(statusPenyediaAsset.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(StatusPenyediaAsset.class, statusPenyediaAsset, statusPenyediaAsset.getNama())
					.setParent(arg0);
			new Label(statusPenyediaAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(statusPenyediaAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusPenyediaAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(statusPenyediaAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, statusPenyediaAsset, StatusPenyediaAssetAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar utama,
	 * membuka formulir tambah status penyedia aset baru dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans {@code StatusPenyediaAsset} kosong baru,
	 * memanggil {@code init(StatusPenyediaAsset)} untuk membangun formulir,
	 * kemudian menampilkan {@code addWindow} sebagai dialog modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol tambah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika terjadi masalah inisialisasi
	 * formulir atau komponen window modal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini terikat ke event {@code onAdd} di ZUL melalui
	 * konvensi penamaan ZKoss. Jangan ubah nama metode ini.
	 *
	 * @param event objek event dari klik tombol tambah
	 * @throws Exception jika terjadi kesalahan inisialisasi formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new StatusPenyediaAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code DataInitDefault} untuk membuka
	 * formulir ubah dari konteks luar (misalnya dari tombol salin atau ubah pada grid).<br>
	 * <br>
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject} (supertype umum entitas AIS),
	 * melakukan cast ke {@code StatusPenyediaAsset}, menyimpannya ke field instans,
	 * memanggil {@code init(StatusPenyediaAsset)} untuk membangun formulir dengan data
	 * yang sudah terisi, lalu menampilkan {@code addWindow} sebagai modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code obj} — entitas {@code StatusPenyediaAsset} yang akan diedit;
	 *       harus dapat di-cast tanpa aman</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code ClassCastException} jika {@code obj} bukan
	 * {@code StatusPenyediaAsset}; exception lain dilempar ke pemanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini dipanggil oleh infrastruktur {@code Common}
	 * (misalnya {@code copyEditDeleteButtons}). Tanda tangan tidak boleh diubah.
	 *
	 * @param obj entitas yang akan diedit; harus bertipe {@code StatusPenyediaAsset}
	 * @throws Exception jika cast gagal atau inisialisasi formulir bermasalah
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		statusPenyediaAsset = (StatusPenyediaAsset) obj;
		init(statusPenyediaAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengisi formulir tambah/ubah status penyedia aset
	 * di dalam window modal {@code addWindow}.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instans {@code statusPenyediaAsset}.</li>
	 *   <li>Mengatur judul window modal: "Tambah Status Penyedia" untuk data baru,
	 *       "Ubah Status Penyedia" untuk data yang sudah ada (ID tidak null).</li>
	 *   <li>Membersihkan konten window dari komposisi sebelumnya via {@code Common.clear}.</li>
	 *   <li>Membangun layout {@code Borderlayout} dengan area Center (formulir grid)
	 *       dan area South (toolbar tombol Batal dan Simpan).</li>
	 *   <li>Formulir memiliki dua kolom (label 30%, input sisa): baris Kode, Nama,
	 *       dan Keterangan (3 baris teks).</li>
	 *   <li>Tombol "Batal" menyembunyikan window; tombol "Simpan" memanggil
	 *       {@code onSave} dan jika berhasil memperbarui grid serta menutup modal.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code statusPenyediaAsset} — entitas yang datanya akan ditampilkan di
	 *       formulir; jika ID null, berarti tambah baru; jika tidak null, berarti ubah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Tidak ada penanganan khusus; exception dilempar ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika menambah field baru pada entitas, tambahkan baris form
	 * baru di sini dan simpan nilainya di {@code onSave}. Referensi komponen input
	 * ({@code kode}, {@code nama}, {@code keterangan}) disimpan sebagai field instans
	 * agar dapat diakses oleh {@code onSave}.
	 *
	 * @param statusPenyediaAsset entitas status penyedia yang akan ditampilkan/diedit
	 */
	private void init(StatusPenyediaAsset statusPenyediaAsset) {
		this.statusPenyediaAsset = statusPenyediaAsset;
		addWindow.setTitle(statusPenyediaAsset.getId() == null ? "Tambah Status Penyedia" : "Ubah Status Penyedia");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Status Penyedia"));
		row.appendChild(kode = new Textbox(statusPenyediaAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Status Penyedia"));
		row.appendChild(nama = new Textbox(statusPenyediaAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(statusPenyediaAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setSclass("ais-btn ais-btn-merah");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setSclass("ais-btn ais-btn-hijau");
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
	 * <b>Tujuan:</b> Memvalidasi dan menyimpan data status penyedia aset ke database,
	 * baik untuk operasi tambah baru maupun ubah data yang sudah ada.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah {@code nama} diisi; jika kosong, menampilkan pesan peringatan
	 *       dan mengembalikan {@code false}.</li>
	 *   <li>Memeriksa duplikasi nama via {@code checkNamaStatusPenyediaAsset()};
	 *       jika nama sudah ada (dengan pengecualian untuk record yang sedang diedit),
	 *       menampilkan peringatan dan mengembalikan {@code false}.</li>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Jika record sudah ada (ID tidak null), memuat ulang dari database via
	 *       {@code session.load} untuk mendapatkan instance yang terikat pada sesi
	 *       (menghindari stale object exception).</li>
	 *   <li>Menetapkan nilai kode, nama, dan keterangan dari input formulir ke entitas.</li>
	 *   <li>Menyimpan atau memperbarui via {@code Common.refreshSaveOrUpdate}.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol Simpan</li>
	 * </ul>
	 * <b>Return:</b> {@code boolean} — {@code true} jika penyimpanan berhasil,
	 * {@code false} jika validasi gagal.<br>
	 * <br>
	 * <b>Penanganan error:</b> Validasi menampilkan dialog informasi; exception Hibernate
	 * tidak ditangkap di sini dan dilempar ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan validasi tambahan di sini jika diperlukan.
	 * Jika menambah field baru, tetapkan nilainya pada entitas sebelum pemanggilan
	 * {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param event objek event ZKoss dari aksi simpan
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Status Penyedia belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama status penyedia yang sesuai; (2) Status penyedia digunakan untuk menandai kondisi aktif/tidak aktif penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaStatusPenyediaAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Status Penyedia sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (statusPenyediaAsset.getId() != null) {
			statusPenyediaAsset = (StatusPenyediaAsset) session.load(StatusPenyediaAsset.class,
					statusPenyediaAsset.getId());

		}

		statusPenyediaAsset.setKode(kode.getValue());
		statusPenyediaAsset.setNama(nama.getValue());
		statusPenyediaAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, statusPenyediaAsset);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria query Hibernate untuk mengambil daftar
	 * {@code StatusPenyediaAsset} sesuai filter yang aktif pada UI.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Membuat criteria untuk {@code StatusPenyediaAsset} dengan filter status aktif:
	 *       jika checkbox {@code searchaktif} null atau dicentang, hanya menampilkan
	 *       record dengan {@code aktif = true} atau {@code aktif IS NULL}; jika tidak
	 *       dicentang, menampilkan semua (SQL restriction "true").</li>
	 *   <li>Jika {@code order} true, menambahkan pengurutan berdasarkan nama ascending.</li>
	 *   <li>Menambahkan filter nama: jika {@code searchnama} tidak kosong, menggunakan
	 *       {@code ilike} untuk pencarian case-insensitive di mana saja dalam nama.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — {@code true} untuk menambahkan ORDER BY nama; {@code false}
	 *       untuk query tanpa urutan (digunakan untuk COUNT paginasi)</li>
	 * </ul>
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke pemanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika menambah filter baru (misalnya berdasarkan kode),
	 * tambahkan kondisi criteria di sini dan tambahkan komponen input filter di ZUL.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan; {@code false} untuk COUNT
	 * @return kriteria Hibernate yang dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(StatusPenyediaAsset.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data dan memperbarui tampilan grid
	 * berdasarkan filter yang aktif pada UI.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memperbarui paginasi (total data) via {@code Common.initPaging} menggunakan
	 *       criteria tanpa urutan (untuk efisiensi COUNT).</li>
	 *   <li>Mengambil daftar {@code StatusPenyediaAsset} dengan criteria berurutan,
	 *       dibatasi oleh {@code Common.ROWS_COUNT_ON_PAGE}, dimulai dari offset
	 *       halaman aktif pada komponen paginasi.</li>
	 *   <li>Membungkus hasilnya dalam {@code SimpleListModel}.</li>
	 *   <li>Menetapkan renderer {@code StatusPenyediaAssetRenderer} pada grid.</li>
	 *   <li>Memperbarui model grid via {@code setModelCheckMobile}.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss pemicu pencarian; boleh null jika
	 *       dipanggil secara programatik</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini terikat ke event {@code onSearchDefault} di ZUL.
	 * Jika komponen paginasi tidak ada di ZUL ({@code paging == null}), offset default
	 * ke halaman pertama (offset 0).
	 *
	 * @param event event pemicu pencarian; boleh null untuk pemanggilan programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<StatusPenyediaAsset> statusPenyediaAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(statusPenyediaAsset);
		grid.setRowRenderer(new StatusPenyediaAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama status penyedia aset yang dimasukkan di
	 * formulir sudah ada di database (deteksi duplikasi).<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menjalankan query COUNT pada tabel {@code StatusPenyediaAsset} dengan
	 *       kondisi nama sama persis (case-sensitive, setelah trim) dengan nilai
	 *       input {@code nama}.</li>
	 *   <li>Mengecualikan record yang sedang diedit (jika ID tidak null) menggunakan
	 *       {@code Restrictions.ne("id", ...)} agar tidak mendeteksi diri sendiri
	 *       sebagai duplikat saat operasi ubah.</li>
	 * </ol>
	 * <b>Parameter:</b> tidak ada (membaca nilai dari field instans {@code nama}
	 * dan {@code statusPenyediaAsset}).<br>
	 * <b>Return:</b> {@code Boolean} — {@code true} jika nama sudah ada di database
	 * (duplikat ditemukan); {@code false} jika nama unik.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pemeriksaan duplikasi hanya berdasarkan nama (exact match,
	 * case-sensitive). Jika perlu validasi kode juga unik, tambahkan metode serupa
	 * {@code checkKodeStatusPenyediaAsset}.
	 *
	 * @return {@code true} jika nama sudah digunakan oleh record lain; {@code false} jika unik
	 */
	public Boolean checkNamaStatusPenyediaAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(StatusPenyediaAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.statusPenyediaAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.statusPenyediaAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
