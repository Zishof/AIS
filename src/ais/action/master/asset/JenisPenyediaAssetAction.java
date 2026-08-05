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
import ais.database.model.asset.JenisPenyediaAsset;
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
 * <h3>JenisPenyediaAssetAction — Kontroler CRUD Master Jenis Penyedia Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss untuk halaman manajemen data master Jenis
 * Penyedia Aset ({@code JenisPenyediaAsset}). Jenis penyedia aset adalah klasifikasi
 * yang membedakan kategori vendor atau pemasok dalam sistem pengadaan aset, misalnya
 * "Distributor", "Manufaktur", "Rekanan Tetap", "UMKM", "Agen Tunggal", dan
 * sebagainya. Data ini digunakan sebagai referensi saat mendaftarkan penyedia baru
 * dan membantu proses seleksi serta pelaporan pengadaan aset berdasarkan jenis vendor.
 * <br><br>
 * Kontroler ini menyediakan operasi CRUD lengkap dengan kontrol hak akses berbasis
 * peran menggunakan {@code CommonPrivilages}. Implementasi mengikuti pola standar
 * kontroler master aset AIS dengan antarmuka {@code DataCriteria}, {@code DataSearchDefault},
 * dan {@code DataInitDefault}.
 *
 * <b>Cara kerja:</b><br>
 * Alur kerja identik dengan kontroler master serupa: pemeriksaan keamanan di
 * {@code doBeforeCompose}, inisialisasi komponen dan hak akses di {@code doAfterCompose},
 * rendering baris grid oleh kelas batin {@code JenisPenyediaAssetRenderer},
 * pembukaan formulir modal via {@code onAdd} dan {@code init}, validasi nama
 * (wajib diisi dan tidak duplikat) serta penyimpanan di {@code onSave}, dan
 * query data via {@code initCriteria} dan {@code onSearchDefault}. Formulir
 * memiliki tiga field: kode, nama, dan keterangan.
 *
 * <b>Threading:</b><br>
 * Model single-thread ZKoss per request. Tidak ada operasi multithreading eksplisit.
 * Sesi Hibernate diakses via {@code HibernateUtil.currentSession()} yang terikat
 * pada thread request. Semua event listener berjalan dalam thread ZK Desktop.
 *
 * <b>Pemeliharaan:</b><br>
 * Untuk menambah atribut baru pada jenis penyedia (misalnya kategori atau kode
 * SBU), tambahkan field input di formulir ({@code init}), logika simpan
 * ({@code onSave}), dan kolom render ({@code JenisPenyediaAssetRenderer}).
 * Array {@code contents} di {@code doAfterCompose} menentukan kolom ekspor/impor.
 * Validasi duplikasi dilakukan oleh {@code checkNamaJenisPenyediaAsset}.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class JenisPenyediaAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal ZKoss tempat formulir tambah/ubah jenis penyedia ditampilkan. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk navigasi halaman daftar data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis penyedia aset. */
	private MyGrid grid;

	/** Textbox filter pencarian berdasarkan nama jenis penyedia. */
	private Textbox searchnama;

	/** Checkbox filter pencarian berdasarkan status aktif. */
	private Checkbox searchaktif;

	/** Textbox input nama jenis penyedia pada formulir tambah/ubah. */
	private Textbox nama;

	/** Textbox input keterangan jenis penyedia pada formulir tambah/ubah. */
	private Textbox keterangan;

	/**
	 * Flag apakah pengguna saat ini memiliki hak ubah data.
	 * Diinisialisasi di {@code doAfterCompose} berdasarkan {@code CommonPrivilages.UPDATE}.
	 */
	private boolean edit = false;

	/**
	 * Flag apakah pengguna saat ini memiliki hak hapus data.
	 * Diinisialisasi di {@code doAfterCompose} berdasarkan {@code CommonPrivilages.DELETE}.
	 */
	private boolean delete = false;

	/**
	 * Entitas {@code JenisPenyediaAsset} yang sedang dalam proses tambah atau ubah.
	 */
	private JenisPenyediaAsset jenisPenyediaAsset;

	/** Tombol Tambah pada toolbar; visibilitas dikontrol oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Textbox input kode jenis penyedia pada formulir tambah/ubah. */
	private Textbox kode;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZKoss dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} lalu meneruskan
	 * ke implementasi induk. Jika pengguna tidak terautentikasi atau tidak berwenang,
	 * eksekusi dihentikan dan diarahkan ke halaman login.<br>
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
	 * <b>Pemeliharaan:</b> Jangan hapus baris keamanan ini.
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
	 * halaman manajemen jenis penyedia aset.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk proses autowiring ZKoss.</li>
	 *   <li>Menginisialisasi multi-bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Menampilkan/menyembunyikan tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete}.</li>
	 *   <li>Menjalankan pencarian awal dan mendaftarkan listener paginasi.</li>
	 *   <li>Menyiapkan tombol cetak/ekspor (kolom: id, kode, nama, keterangan, aktif)
	 *       dan tombol unggah/impor.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root hasil komposisi ZKoss</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Perbarui array {@code contents} jika entitas bertambah kolom.
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
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPenyediaAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPenyediaAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>JenisPenyediaAssetRenderer — Renderer Baris Grid Jenis Penyedia Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas batin ini merender setiap baris data {@code JenisPenyediaAsset} ke dalam
	 * komponen Row ZKoss pada grid utama. Menampilkan kode, nama (dengan riwayat revisi),
	 * keterangan, checkbox aktif interaktif, dan tombol aksi salin/ubah/hapus.<br>
	 * <br>
	 * <b>Cara kerja:</b> Engine renderer ZKoss memanggil metode {@code render} untuk
	 * setiap elemen model data. Checkbox aktif menggunakan listener onCheck yang langsung
	 * menyimpan perubahan ke database.<br>
	 * <br>
	 * <b>Threading:</b> Single-thread ZKoss Desktop per event.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan komponen baru jika kolom grid di ZUL bertambah.
	 */
	class JenisPenyediaAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi komponen Row ZKoss dengan data satu record
		 * {@code JenisPenyediaAsset} untuk ditampilkan dalam grid.<br>
		 * <br>
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan vertical alignment baris ke "top".</li>
		 *   <li>Menambahkan Label kode jenis penyedia.</li>
		 *   <li>Menambahkan komponen revisi nama via {@code RevisiHelper.createNewRevisi}.</li>
		 *   <li>Menambahkan Label keterangan.</li>
		 *   <li>Menambahkan checkbox "Aktif" yang dapat diubah langsung; perubahan
		 *       tersimpan otomatis via {@code Common.refreshSaveOrUpdate}.</li>
		 *   <li>Menambahkan tombol aksi salin/ubah/hapus via
		 *       {@code Common.copyEditDeleteButtons}.</li>
		 * </ol>
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Row ZKoss yang akan diisi</li>
		 *   <li>{@code arg1} — objek data; harus dapat di-cast ke
		 *       {@code JenisPenyediaAsset}</li>
		 * </ul>
		 * <b>Return:</b> void.<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dipropagasi ke engine renderer ZKoss.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Urutan komponen harus sesuai dengan kolom di ZUL.
		 *
		 * @param arg0 baris Row ZKoss target
		 * @param arg1 objek {@code JenisPenyediaAsset} yang akan dirender
		 * @throws Exception jika rendering gagal
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPenyediaAsset jenisPenyediaAsset = (JenisPenyediaAsset) arg1;
			new Label(jenisPenyediaAsset.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPenyediaAsset.class, jenisPenyediaAsset, jenisPenyediaAsset.getNama())
					.setParent(arg0);
			new Label(jenisPenyediaAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPenyediaAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPenyediaAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPenyediaAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPenyediaAsset, JenisPenyediaAssetAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar utama,
	 * membuka formulir tambah jenis penyedia aset baru dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans {@code JenisPenyediaAsset} kosong baru,
	 * memanggil {@code init(JenisPenyediaAsset)} untuk membangun formulir kosong,
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
		init(new JenisPenyediaAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code DataInitDefault} untuk membuka
	 * formulir ubah dari konteks eksternal (tombol ubah atau salin di grid).<br>
	 * <br>
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject}, melakukan cast ke
	 * {@code JenisPenyediaAsset}, menyimpan ke field instans, memanggil
	 * {@code init(JenisPenyediaAsset)} untuk membangun formulir dengan data terisi,
	 * lalu menampilkan {@code addWindow} sebagai modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code obj} — entitas yang akan diedit; harus bertipe
	 *       {@code JenisPenyediaAsset}</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code ClassCastException} jika tipe tidak sesuai.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Dipanggil oleh infrastruktur {@code Common};
	 * tanda tangan tidak boleh diubah.
	 *
	 * @param obj entitas yang akan diedit; harus bertipe {@code JenisPenyediaAsset}
	 * @throws Exception jika cast atau inisialisasi gagal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPenyediaAsset = (JenisPenyediaAsset) obj;
		init(jenisPenyediaAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengisi formulir tambah/ubah jenis penyedia aset
	 * di dalam window modal {@code addWindow}.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instans.</li>
	 *   <li>Mengatur judul window: "Tambah Jenis Penyedia" atau "Ubah Jenis Penyedia".</li>
	 *   <li>Membersihkan konten window dari formulir sebelumnya.</li>
	 *   <li>Membangun layout dengan area Center (formulir grid dua kolom: label 30%
	 *       dan input) serta area South (toolbar tombol Batal dan Simpan).</li>
	 *   <li>Formulir memiliki tiga baris input: Kode, Nama, dan Keterangan (3 baris teks).</li>
	 *   <li>Tombol Batal menutup modal; tombol Simpan memanggil {@code onSave}
	 *       dan menutup modal jika berhasil serta memperbarui grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code jenisPenyediaAsset} — entitas yang akan ditampilkan di formulir;
	 *       ID null = tambah baru, ID tidak null = ubah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar ke atas jika inisialisasi komponen
	 * ZKoss gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Referensi komponen input ({@code kode}, {@code nama},
	 * {@code keterangan}) disimpan sebagai field instans untuk digunakan di
	 * {@code onSave}.
	 *
	 * @param jenisPenyediaAsset entitas yang akan ditampilkan atau diedit di formulir
	 */
	private void init(JenisPenyediaAsset jenisPenyediaAsset) {
		this.jenisPenyediaAsset = jenisPenyediaAsset;
		addWindow.setTitle(jenisPenyediaAsset.getId() == null ? "Tambah Jenis Penyedia" : "Ubah Jenis Penyedia");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Penyedia"));
		row.appendChild(kode = new Textbox(jenisPenyediaAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Penyedia"));
		row.appendChild(nama = new Textbox(jenisPenyediaAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPenyediaAsset.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi dan menyimpan data jenis penyedia aset ke database,
	 * untuk operasi tambah baru maupun ubah data yang sudah ada.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah nama diisi; jika kosong, menampilkan peringatan
	 *       dan mengembalikan {@code false}.</li>
	 *   <li>Memeriksa duplikasi nama via {@code checkNamaJenisPenyediaAsset()};
	 *       jika duplikat ditemukan, menampilkan peringatan dan mengembalikan
	 *       {@code false}.</li>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Jika record sudah ada, memuat ulang entitas dari database via
	 *       {@code session.load} untuk mengikat ke sesi aktif.</li>
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
	 * Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan field baru yang perlu disimpan sebelum
	 * pemanggilan {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param event objek event dari aksi simpan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Penyedia belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis penyedia; (2) Jenis penyedia digunakan untuk mengkategorikan vendor/kontraktor; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaJenisPenyediaAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Jenis Penyedia sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPenyediaAsset.getId() != null) {
			jenisPenyediaAsset = (JenisPenyediaAsset) session.load(JenisPenyediaAsset.class,
					jenisPenyediaAsset.getId());

		}

		jenisPenyediaAsset.setKode(kode.getValue());
		jenisPenyediaAsset.setNama(nama.getValue());
		jenisPenyediaAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPenyediaAsset);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria query Hibernate untuk mengambil daftar
	 * {@code JenisPenyediaAsset} sesuai filter UI yang aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat criteria untuk {@code JenisPenyediaAsset} dengan filter aktif
	 *       (jika checkbox null atau dicentang: hanya aktif/null; jika tidak: semua).</li>
	 *   <li>Menambahkan ORDER BY nama ascending jika {@code order} true.</li>
	 *   <li>Menambahkan filter ilike nama jika {@code searchnama} tidak kosong.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — {@code true} untuk menyertakan ORDER BY</li>
	 * </ul>
	 * <b>Return:</b> {@code Criteria} Hibernate yang siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan filter kode jika dibutuhkan.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan; {@code false} untuk COUNT
	 * @return kriteria Hibernate yang dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPenyediaAsset.class)
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
	 * <b>Tujuan:</b> Menjalankan pencarian dan memperbarui grid tampilan jenis penyedia
	 * aset berdasarkan filter yang aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memperbarui total paginasi via {@code Common.initPaging}.</li>
	 *   <li>Mengambil daftar {@code JenisPenyediaAsset} dengan batas per halaman dan
	 *       offset sesuai halaman aktif.</li>
	 *   <li>Membungkus hasil dalam {@code SimpleListModel}.</li>
	 *   <li>Menetapkan renderer {@code JenisPenyediaAssetRenderer} dan memperbarui
	 *       model grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — event pemicu; boleh null</li>
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

		List<JenisPenyediaAsset> jenisPenyediaAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPenyediaAsset);
		grid.setRowRenderer(new JenisPenyediaAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama jenis penyedia aset yang dimasukkan di
	 * formulir sudah ada di database (deteksi duplikasi nama).<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menjalankan query COUNT pada {@code JenisPenyediaAsset} dengan kondisi
	 *       nama sama persis (setelah trim) dengan nilai input {@code nama}.</li>
	 *   <li>Mengecualikan record yang sedang diedit (jika ID tidak null) menggunakan
	 *       {@code Restrictions.ne("id", ...)} agar operasi ubah tidak mendeteksi
	 *       diri sendiri sebagai duplikat.</li>
	 * </ol>
	 * <b>Parameter:</b> tidak ada (membaca dari field instans {@code nama}
	 * dan {@code jenisPenyediaAsset}).<br>
	 * <b>Return:</b> {@code Boolean} — {@code true} jika duplikat ditemukan;
	 * {@code false} jika nama unik.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika perlu validasi kode unik juga, tambahkan metode
	 * {@code checkKodeJenisPenyediaAsset} dengan logika serupa.
	 *
	 * @return {@code true} jika nama sudah ada di database; {@code false} jika unik
	 */
	public Boolean checkNamaJenisPenyediaAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisPenyediaAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisPenyediaAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisPenyediaAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
