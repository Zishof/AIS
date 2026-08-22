package ais.action.master.akunting;

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

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.AkunPajakDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.AkunPajak;
import ais.ui.util.MyDoublebox;

/**
 * <h3>AkunPajakAction — Kontroler CRUD Master Akun Pajak</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini mengelola data master <em>Akun Pajak</em> dalam modul akunting sistem
 * eCampus. Akun Pajak adalah konfigurasi yang menghubungkan jenis pajak tertentu
 * dengan akun kredit yang akan digunakan saat pajak dipotong atau dikenakan.
 * Setiap entitas {@link AkunPajak} mendefinisikan:
 * <ul>
 *   <li>Nama: identifikasi pajak, misalnya "PPh 21", "PPh 23", "PPN", "PPh 4 ayat 2".</li>
 *   <li>Akun: akun akunting (kredit) yang digunakan untuk mencatat kewajiban pajak.
 *       Dipilih melalui {@link AmbilDataAkunKreditBanbox} yang khusus menampilkan
 *       akun-akun berjenis kredit.</li>
 *   <li>Persentase: tarif pajak dalam bentuk desimal (misal: 2.0 untuk PPh 23 2%).</li>
 *   <li>Keterangan: deskripsi tambahan mengenai akun pajak ini.</li>
 * </ul>
 * Data ini digunakan saat membuat transaksi yang melibatkan pemotongan pajak —
 * sistem akan secara otomatis menghitung nominal pajak berdasarkan persentase dan
 * mencatatnya ke akun yang dikonfigurasi di sini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework.
 * Field-field UI di-wire otomatis dari ZUL. Menggunakan paging untuk navigasi data
 * ketika jumlah akun pajak melebihi {@code Common.ROWS_COUNT_ON_PAGE}. Alur kerja:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} dan {@code doAfterCompose} adalah dua metode
 *       yang digabungkan dalam satu baris kode (format tidak standard namun valid).
 *       {@code doBeforeCompose} memverifikasi keamanan, {@code doAfterCompose}
 *       menginisialisasi UI, paging, dan pencarian awal.</li>
 *   <li>{@code initCriteria} membangun query Hibernate dengan filter nama dan paging.</li>
 *   <li>{@code onSearchDefault} menjalankan query dan memperbarui grid dengan paging.</li>
 *   <li>{@code init(AkunPajak)} membangun formulir dengan empat field: akun kredit
 *       (via banbox), nama, persentase, dan keterangan. Ada listener pada banbox akun
 *       yang secara otomatis mengisi field nama saat akun dipilih.</li>
 *   <li>{@code onSave} memvalidasi nama dan akun, lalu menyimpan melalui DAO.</li>
 * </ol>
 *
 * <p><b>Fitur otomatis isi nama:</b><br>
 * Saat pengguna memilih akun melalui banbox, field nama secara otomatis terisi
 * dengan representasi string akun tersebut. Ini memudahkan pengguna agar tidak
 * perlu mengetikkan nama secara manual.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dieksekusi di thread event ZK. Sesi Hibernate dikelola framework
 * dan tidak boleh ditutup manual oleh kelas ini.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Perhatikan bahwa {@code doBeforeCompose} dan {@code doAfterCompose} ditulis dalam
 * satu baris yang sama — ini valid namun sebaiknya dipisah untuk keterbacaan pada
 * pemeliharaan berikutnya. Jika ada field baru pada {@link AkunPajak}, tambahkan
 * di formulir {@code init} dan set nilainya di {@code onSave}.</p>
 *
 * @author eCampus Dev Team
 * @see AkunPajak
 * @see AkunPajakDao
 * @see AmbilDataAkunKreditBanbox
 */
public class AkunPajakAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas. Menjaga kompatibilitas deserialisasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah/ubah data akun pajak. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada daftar data akun pajak. */
	private Paging paging;

	/** Grid ZK yang menampilkan daftar data akun pajak. */
	private MyGrid grid;

	/** Kotak teks untuk filter pencarian berdasarkan nama akun pajak. */
	private Textbox searchnama;

	/**
	 * Komponen banbox untuk memilih akun kredit yang terkait dengan jenis pajak.
	 * Hanya menampilkan akun-akun bertipe kredit, memastikan akun pajak dikonfigurasi
	 * ke akun yang benar sesuai prinsip akuntansi debet-kredit.
	 */
	private AmbilDataAkunKreditBanbox akun;

	/**
	 * Kotak input angka desimal untuk persentase tarif pajak.
	 * Nilai disimpan sebagai Double, misalnya 2.0 untuk tarif 2% atau 10.0 untuk 10%.
	 */
	private MyDoublebox persen;

	/** Kotak teks untuk input nama jenis pajak (misalnya "PPh 21", "PPN"). */
	private Textbox nama;

	/** Kotak teks multiline untuk keterangan tambahan akun pajak. */
	private Textbox keterangan;

	/** Flag apakah pengguna memiliki hak akses ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak akses hapus (DELETE). */
	private boolean delete = false;

	/** Entitas AkunPajak yang sedang diproses dalam siklus CRUD aktif. */
	private AkunPajak akunPajak;

	/** Tombol toolbar untuk membuka formulir penambahan data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun, sekaligus
	 * menginisialisasi UI setelah komponen selesai di-wire.
	 *
	 * <p><b>Tujuan {@code doBeforeCompose}:</b><br>
	 * Memproteksi halaman manajemen akun pajak dari akses tanpa otorisasi.
	 * Mendelegasikan ke {@code Common.doCheckSecurity()} untuk verifikasi sesi
	 * dan privilege, kemudian meneruskan ke metode induk ZK.</p>
	 *
	 * <p><b>Tujuan {@code doAfterCompose}:</b><br>
	 * Menyelesaikan inisialisasi halaman setelah autowiring selesai. Melakukan:
	 * <ol>
	 *   <li>Inisialisasi bahasa UI dengan {@code Common.initLaguage()}.</li>
	 *   <li>Verifikasi sesi dan privilege READ; jika gagal, redirect logout.</li>
	 *   <li>Konfigurasi visibilitas tombol Tambah berdasarkan privilege CREATE.
	 *       Perhatian: visibilitas di-set SEBELUM guard null — ini berarti jika
	 *       {@code add} null akan terjadi NPE. Guard null ada SETELAH set visible,
	 *       yang hanya melindungi setTooltiptext. Ini adalah bug potensial yang perlu
	 *       diperhatikan pada pemeliharaan.</li>
	 *   <li>Menyimpan flag hak akses edit dan delete.</li>
	 *   <li>Menjalankan pencarian awal.</li>
	 *   <li>Menginisialisasi komponen paging dengan listener untuk refresh grid
	 *       saat pengguna berpindah halaman.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pisahkan deklarasi dan implementasi {@code doBeforeCompose} dan
	 * {@code doAfterCompose} ke metode terpisah untuk keterbacaan. Perbaiki
	 * urutan guard null pada tombol {@code add} (pindahkan guard ke sebelum
	 * {@code add.setVisible(...)}).</p>
	 *
	 * @param comp komponen ZK root dari halaman ini (untuk {@code doAfterCompose})
	 * @throws Exception jika terjadi kesalahan saat inisialisasi (untuk {@code doAfterCompose})
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
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
	 * Kelas renderer dalam untuk menampilkan setiap baris data {@link AkunPajak} pada grid.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data akun pajak ke dalam Row ZK, menampilkan nama pajak,
	 * nama akun kredit terkait, persentase tarif, keterangan, dan tombol aksi.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Kolom yang ditampilkan:
	 * <ol>
	 *   <li>Nama pajak melalui {@code RevisiHelper} untuk riwayat perubahan.</li>
	 *   <li>Representasi string akun kredit ({@code akunPajak.getAkun().toString()})
	 *       dengan null check menggunakan string kosong sebagai default.</li>
	 *   <li>Persentase tarif dengan format angka dan simbol "%" —
	 *       jika null ditampilkan "0 %", jika ada ditampilkan dengan format
	 *       {@code Common.numberFormat} yang menerapkan pemisah ribuan.</li>
	 *   <li>Keterangan sebagai Label biasa.</li>
	 *   <li>Tombol Ubah dan Hapus dalam Hbox. Tombol Hapus dengan konfirmasi
	 *       dan penanganan FK violation.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kolom baru pada {@link AkunPajak}, tambahkan Label sebelum Hbox tombol aksi.</p>
	 */
	class AkunPajakRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data AkunPajak ke dalam komponen ZK Row.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link AkunPajak} menjadi representasi visual
		 * berupa komponen ZK yang mencakup nama, akun, persentase, keterangan, dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Alignment Row diatur ke "top". Nama ditampilkan via RevisiHelper.
		 * Akun ditampilkan menggunakan {@code toString()} entitas Akun (yang biasanya
		 * mengembalikan format "kode - nama"). Persentase diformat dengan pemisah
		 * desimal dan ribuan menggunakan {@code Common.numberFormat.get().format()}.
		 * Keterangan ditampilkan sebagai Label. Tombol aksi dibuat dalam Hbox
		 * dengan konfirmasi dua langkah untuk tombol hapus.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Pengecualian saat penghapusan ditangkap dengan pesan error informatif.
		 * Detail teknis hanya untuk admin.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen ditambahkan
		 * @param arg1 objek data, harus berupa {@link AkunPajak}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final AkunPajak akunPajak = (AkunPajak) arg1;

			RevisiHelper.createNewRevisi(AkunPajak.class, akunPajak,
					akunPajak.getNama()).setParent(arg0);
			new Label(akunPajak.getAkun() == null ? "" : akunPajak.getAkun()
					.toString()).setParent(arg0);
			new Label(akunPajak.getPersen() == null ? "0 %"
					: Common.numberFormat.get().format(akunPajak.getPersen()) + " %")
					.setParent(arg0);
			new Label(akunPajak.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(akunPajak);
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

											Common.refreshDelete(akunPajak);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Menangani event klik tombol "Tambah" untuk membuka formulir akun pajak baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memulai siklus penambahan data akun pajak baru dengan membuat entitas kosong
	 * dan membuka formulir input untuk pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link AkunPajak} baru tanpa ID (mode tambah), memanggil
	 * {@code init(AkunPajak)} untuk membangun formulir, lalu menampilkan modal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada nilai default yang perlu diisi saat formulir
	 * pertama dibuka (misalnya persentase default 0).</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun atau menampilkan formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new AkunPajak());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah data akun pajak secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun seluruh isi formulir dialog {@code addWindow} untuk operasi
	 * tambah maupun ubah data {@link AkunPajak}. Formulir ini memiliki fitur
	 * unik: listener pada banbox akun yang otomatis mengisi field nama.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mengatur judul jendela.</li>
	 *   <li>Membersihkan konten jendela lama.</li>
	 *   <li>Membangun {@link Borderlayout} dengan area Center dan South.</li>
	 *   <li>Pada area Center, membuat grid 2 kolom dengan empat baris formulir:
	 *       <ul>
	 *         <li>Baris 1: "Akun" + {@link AmbilDataAkunKreditBanbox} untuk memilih
	 *             akun kredit. Nilai awal diisi dari {@code toString()} akun yang ada.
	 *             Atribut "akun" pada komponen diisi dari {@code akunPajak.getAkun()}.</li>
	 *         <li>Baris 2: "Nama" + Textbox untuk nama jenis pajak.</li>
	 *         <li>Setelah Baris 2: listener pada banbox akun ditambahkan. Listener ini
	 *             memantau event perubahan pada banbox dan secara otomatis mengisi field
	 *             nama dengan {@code toString()} akun yang baru dipilih. Ini memudahkan
	 *             pengguna yang menggunakan nama akun sebagai nama pajak.</li>
	 *         <li>Baris 3: "Persentase" + {@link MyDoublebox} untuk input tarif pajak.</li>
	 *         <li>Baris 4: "Keterangan" + Textbox multiline 3 baris.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Pada area South, toolbar dengan tombol Batal dan Simpan.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika diperlukan field baru, tambahkan setelah baris keterangan.
	 * Listener banbox menggunakan referensi ke {@code akun} dan {@code nama}
	 * yang merupakan field instance — pastikan field tersebut sudah diinisialisasi
	 * sebelum listener dipasang.</p>
	 *
	 * @param akunPajak entitas yang datanya ditampilkan; ID null = mode tambah
	 */
	private void init(AkunPajak akunPajak) {
		this.akunPajak = akunPajak;
		addWindow.setTitle(akunPajak.getId() == null ? "Tambah AkunPajak" : "Ubah AkunPajak");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));
		row.appendChild(akun = new AmbilDataAkunKreditBanbox());
		akun.setAttribute("akun", akunPajak.getAkun());
		akun.setValue(akunPajak.getAkun() == null ? "" : akunPajak.getAkun()
				.toString());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(akunPajak.getNama() == null ? ""
				: akunPajak.getNama()));
		nama.setWidth("90%");

		akun.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				nama.setValue(akun.getAttribute("akun") == null ? "" : akun
						.getAttribute("akun").toString());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persentase"));
		row.appendChild(persen = new MyDoublebox(akunPajak.getPersen()));
		persen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				akunPajak.getKeterangan() == null ? "" : akunPajak
						.getKeterangan()));
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
	 * Memvalidasi input dan menyimpan atau memperbarui data akun pajak ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani validasi input dan persistensi data {@link AkunPajak}. Dipanggil
	 * saat pengguna mengklik tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi nama wajib:</b> Memeriksa apakah nama tidak kosong.
	 *       Nama diperlukan sebagai identifikasi jenis pajak dalam transaksi.</li>
	 *   <li><b>Validasi akun wajib:</b> Memeriksa apakah atribut "akun" pada
	 *       banbox tidak null. Akun kredit diperlukan untuk mencatat kewajiban
	 *       pajak dalam jurnal akuntansi.</li>
	 *   <li><b>Load entitas:</b> Jika mode ubah (ID tidak null), memuat ulang
	 *       entitas dari DAO.</li>
	 *   <li><b>Set nilai:</b> Menetapkan nama, keterangan, akun kredit, dan
	 *       persentase ke entitas.</li>
	 *   <li><b>Persist:</b> Memanggil {@code update()} atau {@code save()} pada DAO.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak ada validasi rentang persentase (misalnya 0-100%). Pertimbangkan
	 * menambahkan validasi ini jika diperlukan. Jika ada field baru yang wajib
	 * diisi, tambahkan validasinya sebelum blok load DAO.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi DAO
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Akun Pajak belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama akun pajak yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Pajak belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		AkunPajakDao akunPajakDao = DaoFactory.getInstance().getAkunPajakDao();
		if (akunPajak.getId() != null) {
			akunPajak = akunPajakDao.load(akunPajak.getId());

		}

		akunPajak.setNama(nama.getValue());
		akunPajak.setKeterangan(keterangan.getValue());
		akunPajak.setAkun((Akun) akun.getAttribute("akun"));
		akunPajak.setPersen(persen.getValue());

		if (akunPajak.getId() != null) {
			akunPajakDao.update(akunPajak);
		} else {
			akunPajakDao.save(akunPajak);
		}

		return true;
	}

	/**
	 * Membangun kriteria Hibernate untuk query pencarian AkunPajak.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun objek {@link Criteria} Hibernate yang dapat digunakan untuk
	 * menghitung total baris (paging) maupun mengambil data halaman tertentu.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat Criteria pada kelas {@link AkunPajak} tanpa JOIN karena pencarian
	 * hanya berdasarkan kolom {@code nama} yang ada di tabel utama. Filter
	 * menggunakan {@code Restrictions.ilike} dengan mode ANYWHERE untuk pencarian
	 * case-insensitive substring. Jika {@code order} true, pengurutan ascending
	 * berdasarkan nama ditambahkan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Berbeda dengan {@code AkunArusKasAction} yang menggunakan OR antara kode dan
	 * nama, controller ini hanya mencari berdasarkan nama. Jika pencarian berdasarkan
	 * akun kredit diperlukan, tambahkan JOIN ke Akun dan kondisi OR yang sesuai.</p>
	 *
	 * @param order {@code true} jika hasil harus diurutkan berdasarkan nama
	 * @return objek {@link Criteria} Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AkunPajak.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(Restrictions.ilike("nama", searchnama.getValue(),
				MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * Menjalankan pencarian data dan memperbarui tampilan grid AkunPajak dengan paging.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memperbarui tampilan grid dengan data hasil pencarian pada halaman aktif.
	 * Menggunakan paging untuk navigasi jika jumlah data melebihi batas per halaman.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menghitung total baris dan memperbarui komponen paging dengan
	 *       {@code Common.initPaging(initCriteria(false), paging)}.</li>
	 *   <li>Menjalankan query dengan ordering, batasan jumlah baris per halaman
	 *       ({@code ROWS_COUNT_ON_PAGE}), dan offset berdasarkan halaman aktif.</li>
	 *   <li>Menampilkan hasil di grid dengan {@link AkunPajakRenderer}.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Konstan {@code Common.ROWS_COUNT_ON_PAGE} menentukan berapa baris per halaman.
	 * Ubah konstanta tersebut di kelas Common jika perlu menyesuaikan ukuran halaman.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AkunPajak> akunPajak = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(akunPajak);
		grid.setRowRenderer(new AkunPajakRenderer());
		grid.setModelCheckMobile(strset);

	}

}
