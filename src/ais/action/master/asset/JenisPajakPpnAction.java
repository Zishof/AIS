package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.JenisPajakPpn;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisPajakPpnAction — Kontroler CRUD Master Jenis Pajak PPN</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss untuk halaman manajemen data master Jenis
 * Pajak PPN ({@code JenisPajakPpn}). Pajak PPN (Pajak Pertambahan Nilai) dalam
 * konteks pengadaan aset dapat bervariasi jenisnya, misalnya PPN 11%, PPN tidak
 * dipungut, PPN dibebaskan, atau jenis pajak jasa tertentu. Setiap jenis pajak
 * dikaitkan dengan akun akuntansi khusus ({@code Akun}) untuk keperluan pencatatan
 * jurnal otomatis saat transaksi pengadaan terjadi, serta memiliki persentase pajak
 * yang menentukan besaran pemotongan.
 * <br><br>
 * Kontroler ini menyediakan operasi CRUD lengkap dengan kontrol hak akses berbasis
 * peran menggunakan {@code CommonPrivilages}. Fitur khusus dibandingkan kontroler
 * master aset lain adalah adanya field akun akuntansi ({@code AmbilDataAkunBanbox})
 * dan persentase pajak ({@code MyDoublebox}) pada formulir tambah/ubah, serta
 * tampilan kolom akun dan persen di grid.
 *
 * <b>Cara kerja:</b><br>
 * Alur kerja mengikuti pola standar kontroler master AIS: pemeriksaan keamanan di
 * {@code doBeforeCompose}, inisialisasi komponen dan hak akses di {@code doAfterCompose},
 * rendering data di {@code JenisPajakPpnRenderer}, pembukaan formulir modal via
 * {@code onAdd}/{@code init}, validasi dan penyimpanan di {@code onSave}, dan
 * query data via {@code initCriteria}/{@code onSearchDefault}. Field akun dipilih
 * menggunakan komponen bantu {@code AmbilDataAkunBanbox} (combobox dengan fitur
 * pencarian akun), dan nilainya disimpan sebagai atribut komponen ZKoss untuk
 * diteruskan ke entitas saat menyimpan.
 *
 * <b>Threading:</b><br>
 * Model threading ZKoss single-thread per request. Tidak ada operasi multithreading.
 * Sesi Hibernate diakses via {@code HibernateUtil.currentSession()}. Semua listener
 * event berjalan dalam thread ZK Desktop yang sama.
 *
 * <b>Pemeliharaan:</b><br>
 * Jika tarif PPN berubah secara regulasi, data dapat langsung diperbarui melalui
 * UI ini. Pastikan field persentase (persen) menggunakan format angka desimal
 * yang benar (misalnya 11.0 untuk 11%). Jika akun aset PPN diubah, hubungkan
 * dengan benar melalui picker akun. Array {@code contents} menentukan kolom
 * ekspor/impor data.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class JenisPajakPpnAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal ZKoss tempat formulir tambah/ubah jenis pajak PPN ditampilkan. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk navigasi halaman daftar data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis pajak PPN. */
	private MyGrid grid;

	/** Textbox untuk filter pencarian berdasarkan nama jenis pajak PPN. */
	private Textbox searchnama;

	/** Checkbox untuk filter pencarian berdasarkan status aktif. */
	private Checkbox searchaktif;

	/** Textbox input nama jenis pajak PPN pada formulir tambah/ubah. */
	private Textbox nama;

	/** Textbox input keterangan jenis pajak PPN pada formulir tambah/ubah. */
	private Textbox keterangan;

	/**
	 * Flag apakah pengguna saat ini memiliki hak akses ubah data.
	 * Diinisialisasi berdasarkan {@code CommonPrivilages.UPDATE}.
	 */
	private boolean edit = false;

	/**
	 * Flag apakah pengguna saat ini memiliki hak akses hapus data.
	 * Diinisialisasi berdasarkan {@code CommonPrivilages.DELETE}.
	 */
	private boolean delete = false;

	/**
	 * Entitas {@code JenisPajakPpn} yang sedang dalam proses tambah atau ubah.
	 */
	private JenisPajakPpn jenisPajakPpn;

	/** Tombol Tambah pada toolbar; visibilitasnya dikontrol oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Textbox input kode jenis pajak PPN pada formulir tambah/ubah. */
	private Textbox kode;

	/**
	 * Komponen bantu pemilihan akun akuntansi PPN menggunakan combobox
	 * dengan fitur pencarian; nilainya tersimpan sebagai atribut komponen ZKoss.
	 */
	private AmbilDataAkunBanbox akun;

	/**
	 * Komponen input angka desimal untuk persentase pajak PPN.
	 * Nilai disimpan dalam entitas sebagai {@code double}.
	 */
	private MyDoublebox persen;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZKoss dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk memvalidasi
	 * sesi dan hak akses, kemudian meneruskan ke implementasi induk.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK aktif</li>
	 *   <li>{@code parent} — komponen induk</li>
	 *   <li>{@code compInfo} — metadata komponen ZUL</li>
	 * </ul>
	 * <b>Return:</b> {@code ComponentInfo} dari implementasi induk.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code Common.doCheckSecurity()} menangani akses tidak sah.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pemeriksaan keamanan ini wajib ada dan tidak boleh dihapus.
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
	 * halaman manajemen jenis pajak PPN.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZKoss.</li>
	 *   <li>Menginisialisasi dukungan multi-bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Menampilkan/menyembunyikan tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} berdasarkan hak UPDATE/DELETE.</li>
	 *   <li>Menjalankan pencarian awal {@code onSearchDefault(null)}.</li>
	 *   <li>Mendaftarkan listener paginasi.</li>
	 *   <li>Membuat tombol cetak/ekspor dengan kolom: id, kode, nama, akun, persen,
	 *       keterangan, aktif.</li>
	 *   <li>Membuat tombol unggah/impor yang hanya terlihat jika memiliki hak
	 *       CREATE, UPDATE, dan DELETE.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root hasil komposisi ZKoss</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi komponen gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Array {@code contents} menentukan kolom ekspor/impor.
	 * Kolom "akun" dan "persen" khas untuk entitas ini dibandingkan master lain.
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

		String[] contents = new String[] { "id", "kode", "nama", "akun", "persen", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPajakPpn.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPajakPpn.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>JenisPajakPpnRenderer — Renderer Baris Grid Jenis Pajak PPN</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas batin ini merender setiap baris {@code JenisPajakPpn} ke dalam Row ZKoss
	 * pada grid utama. Selain kolom standar (kode, nama, keterangan, aktif), renderer
	 * ini juga menampilkan informasi akun akuntansi yang terkait (kode + nama akun)
	 * dan persentase pajak dalam format angka.<br>
	 * <br>
	 * <b>Cara kerja:</b> Dipanggil oleh engine renderer ZKoss. Menerima objek
	 * {@code JenisPajakPpn} dan mengisi Row dengan Label kode, Label nama (dengan
	 * riwayat revisi), Label akun (format: "kode nama" atau kosong jika null),
	 * Label persentase (diformat dengan {@code Common.numberFormat}), Label keterangan,
	 * checkbox aktif interaktif, dan tombol aksi salin/ubah/hapus.<br>
	 * <br>
	 * <b>Threading:</b> Single-thread ZKoss.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan komponen harus sesuai urutan kolom di ZUL.
	 */
	class JenisPajakPpnRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu record {@code JenisPajakPpn} ke dalam baris Row
		 * ZKoss pada grid utama halaman.<br>
		 * <br>
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan vertical alignment ke "top".</li>
		 *   <li>Menambahkan Label kode PPN.</li>
		 *   <li>Menambahkan komponen revisi nama via {@code RevisiHelper}.</li>
		 *   <li>Menambahkan Label akun: jika akun null menampilkan string kosong,
		 *       jika tidak menampilkan format "kode_akun nama_akun".</li>
		 *   <li>Menambahkan Label persentase pajak diformat dengan {@code Common.numberFormat}.</li>
		 *   <li>Menambahkan Label keterangan.</li>
		 *   <li>Menambahkan checkbox "Aktif" interaktif; perubahan langsung disimpan
		 *       ke database via {@code Common.refreshSaveOrUpdate}.</li>
		 *   <li>Menambahkan tombol aksi salin/ubah/hapus.</li>
		 * </ol>
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Row ZKoss target</li>
		 *   <li>{@code arg1} — objek data; harus dapat di-cast ke {@code JenisPajakPpn}</li>
		 * </ul>
		 * <b>Return:</b> void.<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dipropagasi ke engine renderer ZKoss.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Jika format tampilan akun atau persentase perlu diubah,
		 * sesuaikan di sini.
		 *
		 * @param arg0 baris Row ZKoss target
		 * @param arg1 objek {@code JenisPajakPpn} yang akan dirender
		 * @throws Exception jika rendering gagal
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPajakPpn jenisPajakPpn = (JenisPajakPpn) arg1;
			new Label(jenisPajakPpn.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPajakPpn.class, jenisPajakPpn, jenisPajakPpn.getNama()).setParent(arg0);
			new Label(jenisPajakPpn.getAkun() == null ? ""
					: (jenisPajakPpn.getAkun().getKode() + " " + jenisPajakPpn.getAkun().getNama())).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisPajakPpn.getPersen())).setParent(arg0);
			new Label(jenisPajakPpn.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPajakPpn.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPajakPpn.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPajakPpn);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPajakPpn, JenisPajakPpnAction.this).setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar utama,
	 * membuka formulir tambah jenis pajak PPN baru dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans {@code JenisPajakPpn} kosong baru,
	 * memanggil {@code init(JenisPajakPpn)} untuk membangun formulir,
	 * lalu menampilkan {@code addWindow} sebagai modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol tambah</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi formulir gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onAdd} di ZUL.
	 *
	 * @param event objek event dari klik tombol tambah
	 * @throws Exception jika inisialisasi formulir gagal
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPajakPpn());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code DataInitDefault} untuk membuka
	 * formulir ubah dari konteks luar (tombol salin atau ubah pada grid).<br>
	 * <br>
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject}, melakukan cast ke
	 * {@code JenisPajakPpn}, menyimpannya ke field instans, memanggil
	 * {@code init(JenisPajakPpn)} untuk membangun formulir berisi data, lalu
	 * menampilkan {@code addWindow} sebagai modal.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code obj} — entitas {@code JenisPajakPpn} yang akan diedit</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code ClassCastException} jika tipe tidak sesuai.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tanda tangan tidak boleh diubah; dipanggil oleh infrastruktur
	 * {@code Common.copyEditDeleteButtons}.
	 *
	 * @param obj entitas yang akan diedit; harus bertipe {@code JenisPajakPpn}
	 * @throws Exception jika cast atau inisialisasi gagal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPajakPpn = (JenisPajakPpn) obj;
		init(jenisPajakPpn);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengisi formulir tambah/ubah jenis pajak PPN
	 * di dalam window modal {@code addWindow}.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instans.</li>
	 *   <li>Mengatur judul window: "Tambah PPN" atau "Ubah PPN".</li>
	 *   <li>Membersihkan konten window dari komposisi sebelumnya.</li>
	 *   <li>Membangun layout dengan area Center (formulir) dan South (toolbar aksi).</li>
	 *   <li>Formulir memiliki baris: Kode PPN, Nama PPN (wajib), Akun PPN
	 *       (komponen {@code AmbilDataAkunBanbox} dengan nilai awal dari entitas
	 *       tersimpan sebagai atribut ZKoss), Persen pajak (wajib), dan Keterangan.</li>
	 *   <li>Tombol Batal menutup modal; tombol Simpan memanggil {@code onSave}
	 *       dan menutup modal jika berhasil serta memperbarui grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code jenisPajakPpn} — entitas yang akan ditampilkan di formulir</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar ke atas jika terjadi masalah
	 * inisialisasi komponen ZKoss.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Field akun disimpan sebagai atribut komponen ZKoss dengan
	 * kunci "akun"; {@code onSave} harus menggunakan {@code akun.getAttribute("akun")}
	 * untuk mendapatkan entitas akun yang dipilih.
	 *
	 * @param jenisPajakPpn entitas jenis pajak PPN yang akan ditampilkan/diedit
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen
	 */
	private void init(JenisPajakPpn jenisPajakPpn) throws Exception {
		this.jenisPajakPpn = jenisPajakPpn;
		addWindow.setTitle(jenisPajakPpn.getId() == null ? "Tambah PPN" : "Ubah PPN");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode PPN"));
		row.appendChild(kode = new Textbox(jenisPajakPpn.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama PPN *"));
		row.appendChild(nama = new Textbox(jenisPajakPpn.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun PPN"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisPajakPpn.getAkun() == null ? "" : jenisPajakPpn.getAkun().getNama());
		akun.setAttribute("akun", jenisPajakPpn.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persen pajak *"));
		row.appendChild(persen = new MyDoublebox(jenisPajakPpn.getPersen()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPajakPpn.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi dan menyimpan data jenis pajak PPN ke database,
	 * baik untuk operasi tambah baru maupun ubah data yang sudah ada.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah nama diisi; jika kosong, menampilkan peringatan
	 *       "Nama Pajak Ppn/Jasa harus diisi" dan mengembalikan {@code false}.</li>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Jika record sudah ada (ID tidak null), memuat ulang entitas dari database
	 *       via {@code session.load} untuk mengikat ke sesi aktif.</li>
	 *   <li>Menetapkan nilai dari formulir ke entitas: kode, nama, akun (diambil dari
	 *       atribut komponen ZKoss dengan kunci "akun"), persentase, dan keterangan.</li>
	 *   <li>Menyimpan atau memperbarui via {@code Common.refreshSaveOrUpdate}.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol Simpan</li>
	 * </ul>
	 * <b>Return:</b> {@code boolean} — {@code true} jika berhasil disimpan;
	 * {@code false} jika validasi nama gagal.<br>
	 * <br>
	 * <b>Penanganan error:</b> Validasi nama menampilkan dialog peringatan;
	 * exception Hibernate tidak ditangkap dan dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Perhatikan bahwa tidak ada validasi duplikasi nama
	 * untuk JenisPajakPpn (berbeda dengan master lain). Jika diperlukan, tambahkan
	 * metode {@code checkNamaJenisPajakPpn} serupa dengan kelas saudara.
	 *
	 * @param event objek event dari aksi simpan
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Pajak PPN/Jasa belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama pajak PPN yang sesuai; (2) Nama pajak digunakan untuk mengidentifikasi jenis PPN dalam transaksi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPajakPpn.getId() != null) {
			jenisPajakPpn = (JenisPajakPpn) session.load(JenisPajakPpn.class, jenisPajakPpn.getId());

		}

		jenisPajakPpn.setKode(kode.getValue());
		jenisPajakPpn.setNama(nama.getValue());
		jenisPajakPpn.setAkun((Akun) akun.getAttribute("akun"));
		jenisPajakPpn.setPersen(persen.getValue());
		jenisPajakPpn.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPajakPpn);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria query Hibernate untuk mengambil daftar
	 * {@code JenisPajakPpn} sesuai filter yang aktif pada UI.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat criteria untuk {@code JenisPajakPpn} dengan filter status aktif:
	 *       jika checkbox null atau dicentang, hanya menampilkan record aktif atau
	 *       null; jika tidak dicentang, tampilkan semua.</li>
	 *   <li>Menambahkan ORDER BY nama jika {@code order} true.</li>
	 *   <li>Menambahkan filter ilike pada nama jika {@code searchnama} tidak kosong.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — {@code true} untuk menyertakan ORDER BY nama</li>
	 * </ul>
	 * <b>Return:</b> {@code Criteria} Hibernate yang siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke pemanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan filter persentase atau akun jika diperlukan.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan; {@code false} untuk COUNT
	 * @return kriteria Hibernate yang dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPajakPpn.class)
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
	 * <b>Tujuan:</b> Menjalankan pencarian dan memperbarui grid tampilan jenis pajak PPN
	 * berdasarkan filter aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memperbarui total paginasi via {@code Common.initPaging}.</li>
	 *   <li>Mengambil daftar {@code JenisPajakPpn} dengan batas per halaman dan offset
	 *       sesuai halaman aktif paginasi.</li>
	 *   <li>Membungkus hasil dalam {@code SimpleListModel}.</li>
	 *   <li>Menetapkan renderer dan memperbarui model grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — event pemicu; boleh null untuk pemanggilan programatik</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke atas.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onSearchDefault} di ZUL.
	 *
	 * @param event event pemicu pencarian; boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPajakPpn> jenisPajakPpn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPajakPpn);
		grid.setRowRenderer(new JenisPajakPpnRenderer());
		grid.setModelCheckMobile(strset);

	}

}
