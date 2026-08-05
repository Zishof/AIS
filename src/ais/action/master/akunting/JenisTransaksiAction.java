package ais.action.master.akunting;

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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.JenisTransaksiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.surat.NomorSurat;
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
 * <h3>JenisTransaksiAction — Controller Master Jenis Transaksi Akuntansi</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola halaman master
 * Jenis Transaksi dalam modul akuntansi. Jenis transaksi adalah kategori
 * transaksi keuangan (misalnya "Gaji Pegawai Tetap", "Operasional Kantor",
 * "Pengadaan ATK") yang digunakan untuk mengklasifikasikan entri-entri jurnal.
 * Setiap {@link JenisTransaksi} dapat dikaitkan dengan satu {@link Akun} default
 * dan satu template {@link NomorSurat} untuk penomoran keuangan. Pengguna dengan
 * hak yang sesuai dapat menambah, mengubah, menghapus, dan mengonfigurasi
 * jenis transaksi melalui antarmuka ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK GenericAutowireComposer:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan sebelum halaman dikompilasi.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen: mengecek sesi, menginisialisasi
 *       bahasa, mengatur tombol tambah dan hak akses, memanggil {@code onSearchDefault},
 *       menginisialisasi paging, mendaftarkan tombol ekspor Excel dan upload data.</li>
 *   <li>Inner class {@code JenisTransaksiRenderer} — merender setiap baris grid dengan:
 *       kode (revisi history), nama, status default, komponen pencarian nomor surat
 *       yang dapat diedit inline, komponen pencarian akun yang dapat diedit inline,
 *       keterangan, checkbox Aktif, dan tombol Ubah/Hapus.</li>
 *   <li>{@code onAdd} / {@code init(GeneralValueObject)} — membuka form tambah/ubah.</li>
 *   <li>Metode {@code init(JenisTransaksi)} (private) — membangun form modal dengan
 *       field kode, nama, flag default, dan keterangan.</li>
 *   <li>{@code onSave} — memvalidasi input, memeriksa duplikasi kode, menyimpan
 *       data melalui DAO, dan memicu {@code reinitDefault} untuk memperbarui cache.</li>
 *   <li>{@code initCriteria} — membangun Hibernate Criteria dengan filter nama,
 *       kode, akun, dan status aktif.</li>
 *   <li>{@code onSearchDefault} — menjalankan query via {@code ConstantValues.simpleList}
 *       dan memperbarui grid.</li>
 *   <li>{@code checkKodeJenisTransaksi} — memeriksa duplikasi kode.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZK event-dispatcher. Perubahan inline pada
 * akun dan nomor surat di renderer menggunakan {@code Common.refreshUpdate} yang
 * sinkron. Setelah perubahan, timer default dipanggil untuk memperbarui cache
 * {@link NomorSuratAlurKeuangan} secara asinkron. Status aktif juga menggunakan
 * timer setelah simpan untuk reinisialisasi cache {@link JenisTransaksi}.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Flag {@code defaultItem} hanya boleh bernilai true pada satu jenis transaksi;
 *   logika di {@code onSave} memastikan ini dengan SQL UPDATE yang me-reset semua
 *   flag ke false sebelum menyimpan yang baru.<br>
 * - {@code JenisTransaksi.reinitDefault()} dan {@code NomorSuratAlurKeuangan.reloadDefault()}
 *   harus dipanggil setelah setiap perubahan agar aplikasi menggunakan data terbaru
 *   dari cache in-memory.<br>
 * - Implementasi {@code DataCriteria}, {@code DataSearchDefault}, dan {@code DataInitDefault}
 *   diperlukan untuk integrasi dengan fitur ekspor, upload, dan tombol Ubah/Copy/Hapus.<br>
 * - Filter akun ({@code searchakun}) menggunakan join dan ILIKE bersamaan pada nama
 *   DAN kode akun; ini bisa menyebabkan hasil tidak terduga (AND antara keduanya)
 *   jika pengguna mengetikkan term yang tidak cocok di salah satu field.</p>
 */
public class JenisTransaksiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	/** Filter teks pada nama atau kode akun yang terkait dengan jenis transaksi. */
	private Textbox searchakun;

	/** Checkbox filter untuk hanya menampilkan jenis transaksi yang aktif. */
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	/** Checkbox untuk menandai jenis transaksi ini sebagai default (hanya satu boleh aktif). */
	private MyCheckboxConfig defaultItem;

	private boolean edit = false;
	private boolean delete = false;

	private JenisTransaksi jenisTransaksi;
	private MyToolbarbuttonConfig add;

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Dipanggil oleh framework ZK sebagai langkah pertama dalam siklus hidup
	 * komposisi halaman, memastikan hanya pengguna yang berhak yang mengakses
	 * halaman master jenis transaksi.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan ke {@code Common.doCheckSecurity()} yang
	 * memverifikasi otentikasi dan otorisasi pengguna. Jika tidak berhak,
	 * pengguna diarahkan ke logoff sebelum halaman selesai dimuat. Kemudian
	 * memanggil implementasi super untuk melanjutkan proses komposisi.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan keamanan. Logika yang membutuhkan komponen
	 * ZUL harus diletakkan di {@link #doAfterCompose(Component)}.</p>
	 *
	 * @param page     halaman ZK yang sedang dikompilasi
	 * @param parent   komponen induk dari composer ini
	 * @param compInfo metadata komponen dari ZUL
	 * @return {@code ComponentInfo} dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan logika bisnis halaman
	 * master jenis transaksi setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah inisialisasi yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk lokalisasi bahasa.</li>
	 *   <li>Memeriksa sesi pengguna; jika tidak valid atau tidak punya hak READ, logoff.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" berdasarkan hak CREATE (null-safe).</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} sesuai hak akses.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk data awal.</li>
	 *   <li>Menginisialisasi paging dengan listener standar.</li>
	 *   <li>Mendaftarkan tombol ekspor Excel dengan array kolom yang mencakup
	 *       {@code id, kode, nama, keterangan, akun, nomorSurat, defaultItem, aktif}.</li>
	 *   <li>Mendaftarkan tombol upload data dengan visibilitas terkontrol oleh
	 *       gabungan hak CREATE + UPDATE + DELETE.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super atau inisialisasi komponen diteruskan ke framework ZK.
	 * Pengecekan sesi dilakukan di awal metode; jika gagal, metode keluar lebih awal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Array {@code contents} menentukan kolom ekspor dan upload. Perbarui sesuai
	 * dengan perubahan skema entitas {@link JenisTransaksi}. Pastikan urutan kolom
	 * sesuai dengan template Excel yang digunakan untuk upload.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai di-autowire
	 * @throws Exception jika terjadi error saat inisialisasi komponen
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "akun", "nomorSurat", "defaultItem",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisTransaksi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisTransaksi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>JenisTransaksiRenderer — Renderer Baris Grid Jenis Transaksi</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini merender setiap baris pada grid master jenis transaksi.
	 * Fitur unik dari renderer ini adalah kemampuan edit inline langsung di grid
	 * (tanpa membuka popup) untuk dua kolom: nomor surat dan akun. Perubahan
	 * pada kedua kolom tersebut langsung tersimpan ke database dan memicu
	 * pembaruan cache {@link NomorSuratAlurKeuangan}. Kolom aktif juga dapat
	 * diubah langsung melalui checkbox inline.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Untuk setiap objek {@link JenisTransaksi}:
	 * <ol>
	 *   <li>Menampilkan kode menggunakan widget revisi history.</li>
	 *   <li>Menampilkan nama.</li>
	 *   <li>Menampilkan status default (Ya/Tidak).</li>
	 *   <li>Menampilkan komponen {@link AmbilDataNomorSuratBanbox} dalam mode readonly
	 *       dengan nilai nomor surat saat ini; listener {@code setEventListener} menangkap
	 *       pemilihan nomor surat baru dan langsung menyimpan serta mereload cache.</li>
	 *   <li>Menampilkan komponen {@link AmbilDataAkunBanbox} dengan logika serupa
	 *       untuk pemilihan akun default.</li>
	 *   <li>Menampilkan keterangan.</li>
	 *   <li>Menampilkan checkbox Aktif yang dapat diubah langsung.</li>
	 *   <li>Menampilkan tombol Ubah/Hapus melalui {@code Common.copyEditDeleteButtons}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Listener event berjalan di thread ZK event-dispatcher. Timer default dipakai
	 * untuk reload cache setelah perubahan agar tidak memblokir respons UI.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Komponen banbox ({@code AmbilDataNomorSuratBanbox} dan {@code AmbilDataAkunBanbox})
	 * dibuat baru per baris; pastikan ini tidak menyebabkan masalah memori pada
	 * dataset besar. Mode readonly pada banbox berarti pengguna harus membuka
	 * popup pencarian; ubah ke false jika ingin edit langsung di textbox.</p>
	 */
	class JenisTransaksiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan data dari objek
		 * {@link JenisTransaksi}, menampilkan informasi lengkap beserta kontrol
		 * interaktif untuk edit inline nomor surat, akun, status aktif, dan
		 * tombol aksi Ubah/Hapus.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Komponen dibuat secara programatik dan ditambahkan ke baris sesuai urutan
		 * kolom ZUL. Banbox untuk nomor surat dan akun menggunakan pola attribute
		 * "nomorSurat" dan "akun" untuk menyimpan referensi objek terpilih, sehingga
		 * listener event dapat mengambilnya saat dipanggil. Setiap perubahan nomor surat
		 * atau akun memicu: (1) update entity, (2) {@code Common.refreshUpdate}, dan
		 * (3) timer untuk {@code NomorSuratAlurKeuangan.reloadDefault()}. Checkbox aktif
		 * juga memicu reload yang sama ditambah {@code JenisTransaksi.reinitDefault()}.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception dari listener event diteruskan ke framework ZK. Null check
		 * dilakukan pada properti yang bisa null ({@code getDefaultItem()},
		 * {@code getNomorSurat()}, {@code getAkun()}).</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Urutan penambahan komponen ke {@code arg0} harus persis sesuai dengan
		 * urutan {@code <column>} di file ZUL. Jika ada penambahan/perubahan kolom,
		 * perbarui keduanya secara bersamaan.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen UI
		 * @param arg1 objek data, dicast ke {@link JenisTransaksi}
		 * @throws Exception jika terjadi error saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisTransaksi jenisTransaksi = (JenisTransaksi) arg1;

			RevisiHelper.createNewRevisi(JenisTransaksi.class, jenisTransaksi, jenisTransaksi.getKode())
					.setParent(arg0);
			new Label(jenisTransaksi.getNama()).setParent(arg0);
			new Label(jenisTransaksi.getDefaultItem() != null && jenisTransaksi.getDefaultItem() ? "Ya" : "Tidak")
					.setParent(arg0);

			final AmbilDataNomorSuratBanbox ambilDataNomorSuratBanbox = new AmbilDataNomorSuratBanbox();
			ambilDataNomorSuratBanbox.setAttribute("nomorSurat", jenisTransaksi.getNomorSurat());
			ambilDataNomorSuratBanbox
					.setValue(jenisTransaksi.getNomorSurat() == null ? "" : jenisTransaksi.getNomorSurat().getNama());
			ambilDataNomorSuratBanbox.setWidth("95%");
			ambilDataNomorSuratBanbox.setReadonly(true);
			ambilDataNomorSuratBanbox.setParent(arg0);

			ambilDataNomorSuratBanbox.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					NomorSurat ns = (NomorSurat) ambilDataNomorSuratBanbox.getAttribute("nomorSurat");
					jenisTransaksi.setNomorSurat(ns);
					Common.refreshUpdate(jenisTransaksi);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							NomorSuratAlurKeuangan.reloadDefault();
						}
					});
				}
			});

			final AmbilDataAkunBanbox ambilDataAkunBanbox = new AmbilDataAkunBanbox();
			ambilDataAkunBanbox.setAttribute("akun", jenisTransaksi.getAkun());
			ambilDataAkunBanbox.setValue(jenisTransaksi.getAkun() == null ? ""
					: jenisTransaksi.getAkun().getKode() + "-" + jenisTransaksi.getAkun().getNama());
			ambilDataAkunBanbox.setWidth("95%");
			ambilDataAkunBanbox.setReadonly(true);
			ambilDataAkunBanbox.setParent(arg0);

			ambilDataAkunBanbox.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Akun ns = (Akun) ambilDataAkunBanbox.getAttribute("akun");
					jenisTransaksi.setAkun(ns);
					Common.refreshUpdate(jenisTransaksi);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							NomorSuratAlurKeuangan.reloadDefault();
						}
					});
				}
			});

			new Label(jenisTransaksi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisTransaksi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisTransaksi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisTransaksi);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							NomorSuratAlurKeuangan.reloadDefault();
						}
					});
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisTransaksi, JenisTransaksiAction.this).setParent(arg0);
		}

	}

	/**
	 * <b>Tujuan:</b> Menangani aksi pengguna ketika menekan tombol "Tambah"
	 * pada toolbar halaman, membuka form untuk menambahkan jenis transaksi baru.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@link JenisTransaksi} baru (kosong) dan memanggil metode
	 * {@code init(JenisTransaksi)} untuk membangun form secara programatik.
	 * Kemudian menampilkan jendela modal {@code addWindow} kepada pengguna.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke framework ZK. Tidak ada logika validasi di metode ini.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika diperlukan nilai default tertentu (misalnya kode dengan prefix tertentu),
	 * isi pada objek baru sebelum memanggil {@code init()}.</p>
	 *
	 * @param event event ZK dari klik tombol "Tambah"
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisTransaksi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form tambah/ubah jenis transaksi
	 * secara programatik di dalam jendela modal {@code addWindow}.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah pembangunan form:
	 * <ol>
	 *   <li>Menyimpan referensi jenis transaksi yang akan diedit/dibuat.</li>
	 *   <li>Mengatur judul jendela sesuai mode (Tambah/Ubah).</li>
	 *   <li>Membersihkan konten lama dari {@code addWindow}.</li>
	 *   <li>Membangun Borderlayout dengan Center (form input) dan South (toolbar).</li>
	 *   <li>Membuat grid dengan kolom 30%/70% untuk label/input.</li>
	 *   <li>Menambahkan field: Kode (Textbox), Nama (Textbox), Default (MyCheckboxConfig,
	 *       disembunyikan - {@code setVisible(false)}), dan Keterangan (Textbox multiline 3 baris).</li>
	 *   <li>Toolbar berisi tombol Batal (menyembunyikan jendela) dan Simpan
	 *       (memanggil {@code onSave}; jika berhasil, refresh grid dan tutup jendela).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception saat membangun komponen diteruskan ke atas. Tidak ada try-catch
	 * di metode ini karena error saat membangun UI dianggap kritis.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Baris "Default" disembunyikan ({@code row.setVisible(false)}) dalam kode saat ini,
	 * menandakan fitur ini mungkin dinonaktifkan sementara. Aktifkan kembali jika
	 * diperlukan dengan menghapus {@code setVisible(false)}.</p>
	 *
	 * @param jenisTransaksi objek {@link JenisTransaksi} yang akan diedit atau dibuat baru
	 */
	private void init(JenisTransaksi jenisTransaksi) {
		this.jenisTransaksi = jenisTransaksi;
		addWindow.setTitle(jenisTransaksi.getId() == null ? "Tambah Jenis Transaksi" : "Ubah Jenis Transaksi");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Jurnal *"));
		row.appendChild(kode = new Textbox(jenisTransaksi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Jurnal *"));
		row.appendChild(nama = new Textbox(jenisTransaksi.getNama() == null ? "" : jenisTransaksi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(jenisTransaksi.getDefaultItem() != null && jenisTransaksi.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenisTransaksi.getKeterangan() == null ? "" : jenisTransaksi.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi input pengguna dan menyimpan data jenis transaksi
	 * ke database, dengan penanganan khusus untuk flag {@code defaultItem} yang
	 * hanya boleh aktif pada satu record sekaligus.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Proses penyimpanan melalui beberapa tahap:
	 * <ol>
	 *   <li><b>Validasi kode:</b> Kode tidak boleh kosong (setelah trim).</li>
	 *   <li><b>Validasi nama:</b> Nama tidak boleh kosong.</li>
	 *   <li><b>Duplikasi kode:</b> Memanggil {@link #checkKodeJenisTransaksi()} untuk
	 *       memastikan kode belum digunakan oleh jenis transaksi lain.</li>
	 *   <li><b>Reset defaultItem:</b> Jika checkbox default dicentang, menjalankan
	 *       SQL UPDATE untuk me-reset semua jenis transaksi lain ke {@code defaultitem=false}
	 *       melalui DAO session aktif, sebelum menyimpan yang baru sebagai default.</li>
	 *   <li><b>Load ulang entity:</b> Pada mode ubah, memuat entity dari database
	 *       via DAO untuk mendapatkan state terkini sebelum diupdate.</li>
	 *   <li><b>Simpan entity:</b> Menggunakan {@code JenisTransaksiDao.save} atau
	 *       {@code update} sesuai mode. Setelah simpan, timer default dipanggil
	 *       untuk memanggil {@code JenisTransaksi.reinitDefault()} yang memperbarui
	 *       cache static in-memory.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Setiap validasi yang gagal menampilkan dialog informasi dan mengembalikan
	 * {@code false}. Exception dari DAO atau Hibernate diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * SQL UPDATE reset default ({@code update akunting.jenis_transaksi set defaultitem = false})
	 * bersifat global dan menggunakan nama skema langsung; perbarui jika skema database berubah.
	 * Perhatikan bahwa operasi ini tidak transaksional terpisah dari penyimpanan entity utama,
	 * sehingga ada potensi inkonsistensi jika penyimpanan entity gagal setelah UPDATE berhasil.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan, {@code false} jika validasi gagal
	 * @throws Exception jika terjadi error yang tidak tertangani
	 */
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Jenis Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Kode Jenis Jurnal dengan kode yang valid dan unik; (2) Pastikan kode tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama Jenis Jurnal dengan nama yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkKodeJenisTransaksi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Jenis Jurnal sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan kode yang berbeda dan belum terdaftar; (2) Periksa daftar jenis jurnal yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan kode baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		JenisTransaksiDao jenisTransaksiDao = DaoFactory.getInstance().getJenisTransaksiDao();
		if (defaultItem.isChecked()) {
			jenisTransaksiDao.getCurrentSession()
					.createSQLQuery("update akunting.jenis_transaksi set defaultitem = false;").executeUpdate();
		}

		if (jenisTransaksi.getId() != null) {
			jenisTransaksi = jenisTransaksiDao.load(jenisTransaksi.getId());
		}

		jenisTransaksi.setDefaultItem(defaultItem.isChecked());
		jenisTransaksi.setKode(kode.getValue());
		jenisTransaksi.setNama(nama.getValue());
		jenisTransaksi.setKeterangan(keterangan.getValue());

		if (jenisTransaksi.getId() != null) {
			jenisTransaksiDao.update(jenisTransaksi);
		} else {
			jenisTransaksiDao.save(jenisTransaksi);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisTransaksi.reinitDefault();
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} untuk query data
	 * {@link JenisTransaksi} sesuai filter yang aktif di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun criteria dengan kondisi-kondisi berikut:
	 * <ul>
	 *   <li><b>Filter aktif:</b> Jika checkbox {@code searchaktif} null atau dicentang,
	 *       menampilkan hanya yang aktif (null atau true). Jika tidak dicentang,
	 *       menampilkan semua ({@code sqlRestriction("true")}).</li>
	 *   <li><b>Pengurutan:</b> Jika {@code order=true}, ascending berdasarkan nama.</li>
	 *   <li><b>Filter akun:</b> Jika {@code searchakun} tidak kosong, membuat join
	 *       ke alias "akun" dan menambahkan filter ILIKE pada {@code akun.nama} DAN
	 *       {@code akun.kode}. Perhatikan bahwa ini menggunakan AND, sehingga kedua
	 *       kondisi harus terpenuhi secara bersamaan.</li>
	 *   <li><b>Filter nama:</b> ILIKE pada field {@code nama} jika tidak kosong.</li>
	 *   <li><b>Filter kode:</b> ILIKE pada field {@code kode} selalu diaplikasikan
	 *       (menggunakan nilai kosong sebagai wildcard).</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke pemanggil. Pemeriksaan null pada
	 * {@code searchaktif} mencegah NPE jika komponen opsional tidak ada di ZUL.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Filter akun dengan AND pada nama DAN kode bisa memberikan hasil yang tidak
	 * intuitif. Pertimbangkan mengubah ke OR jika pengguna melaporkan hasil yang
	 * membingungkan. Criteria ini digunakan oleh {@code ConstantValues.simpleList}
	 * yang memiliki penanganan khusus untuk caching.</p>
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY, {@code false} untuk COUNT
	 * @return objek {@link Criteria} yang telah dikonfigurasi dengan semua filter aktif
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisTransaksi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		if (!searchakun.getValue().trim().isEmpty()) {
			criteria.createAlias("akun", "akun")
					.add(Restrictions.ilike("akun.nama", searchakun.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("akun.kode", searchakun.getValue(), MatchMode.ANYWHERE));
		}

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data jenis transaksi berdasarkan filter
	 * aktif dan memperbarui tampilan grid dengan hasilnya, disertai pembaruan paging.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan dua panggilan {@link #initCriteria(boolean)}: pertama dengan
	 * {@code order=false} untuk menghitung total record dan memperbarui paging,
	 * kemudian dengan {@code order=true} untuk mengambil data halaman aktif.
	 * Berbeda dengan action lain, menggunakan {@code ConstantValues.simpleList}
	 * (bukan langsung {@code .list()}) yang mungkin menerapkan logika caching
	 * atau konversi khusus. Hasil dibungkus dalam {@code SimpleListModel} dan
	 * diset ke grid dengan {@code JenisTransaksiRenderer} baru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate atau {@code simpleList} diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Penggunaan {@code ConstantValues.simpleList} daripada langsung {@code .list()}
	 * adalah pola yang dipilih untuk entitas yang di-cache. Jaga konsistensi ini
	 * jika ada modifikasi pada cara pengambilan data.</p>
	 *
	 * @param event event ZK pemicu pencarian (bisa null jika dipanggil programatik)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisTransaksi> jenisTransaksi = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				JenisTransaksi.class);
		ListModel strset = new SimpleListModel(jenisTransaksi);
		grid.setRowRenderer(new JenisTransaksiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah kode jenis transaksi yang dimasukkan pengguna
	 * sudah ada di database untuk mencegah penyimpanan data dengan kode duplikat.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menjalankan query COUNT pada tabel {@link JenisTransaksi} dengan kondisi:
	 * <ul>
	 *   <li>Kode sama persis (case-sensitive via {@code Restrictions.eq}) dengan
	 *       nilai yang dimasukkan pengguna (setelah trim).</li>
	 *   <li>Pada mode ubah: mengecualikan record dengan ID yang sama menggunakan
	 *       {@code Restrictions.ne("id")} agar kode yang tidak berubah tidak
	 *       dianggap duplikat.</li>
	 * </ul>
	 * Mengembalikan {@code true} jika ada record lain dengan kode yang sama.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari query diteruskan ke pemanggil. Null pada uniqueResult tidak
	 * mungkin karena COUNT selalu mengembalikan angka.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Kode jenis transaksi adalah identifier unik yang digunakan di berbagai
	 * tempat dalam sistem untuk referensi. Jangan ubah logika pemeriksaan ini
	 * tanpa memahami dampaknya pada integritas referensi.</p>
	 *
	 * @return {@code true} jika kode sudah digunakan (duplikat),
	 *         {@code false} jika kode aman untuk disimpan
	 */
	public Boolean checkKodeJenisTransaksi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisTransaksi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.jenisTransaksi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisTransaksi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * <b>Tujuan:</b> Implementasi dari antarmuka {@link DataInitDefault} yang
	 * memungkinkan komponen lain (seperti {@code Common.copyEditDeleteButtons})
	 * membuka form edit melalui referensi generik {@link GeneralValueObject}.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menerima objek {@link GeneralValueObject}, melakukan downcast ke
	 * {@link JenisTransaksi}, menyimpan referensinya ke field instance, lalu
	 * memanggil {@code init(JenisTransaksi)} (private) untuk membangun form edit.
	 * Setelah form siap, jendela modal {@code addWindow} ditampilkan.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * ClassCastException terjadi jika objek bukan {@link JenisTransaksi}; ini
	 * tidak seharusnya terjadi dalam alur normal. Exception lain diteruskan
	 * ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini diperlukan untuk integrasi dengan {@code Common.copyEditDeleteButtons}
	 * yang menggunakan tipe generik. Tidak perlu diubah kecuali ada perubahan
	 * pada kontrak antarmuka {@code DataInitDefault}.</p>
	 *
	 * @param obj objek {@link GeneralValueObject} yang merupakan instance {@link JenisTransaksi}
	 * @throws Exception jika terjadi error saat membangun form atau menampilkan modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisTransaksi = (JenisTransaksi) obj;
		init(jenisTransaksi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
