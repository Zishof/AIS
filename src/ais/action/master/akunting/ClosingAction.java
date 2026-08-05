package ais.action.master.akunting;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.GrupTransaksi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>ClosingAction — Controller Closing Periode Akuntansi</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola proses <i>closing</i>
 * (penutupan periode) dalam modul akuntansi. Closing adalah mekanisme untuk
 * menutup suatu periode akuntansi (misalnya akhir bulan atau akhir tahun),
 * yang berdampak pada pengelompokan jurnal-jurnal yang termasuk dalam periode
 * tersebut. Setiap data {@link Closing} memiliki tanggal batas dan nama periode,
 * dan sistem akan secara otomatis mengasosiasikan semua {@link GrupTransaksi}
 * dengan tanggal transaksi pada atau sebelum tanggal closing ke dalam record
 * closing yang paling sesuai.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan awal.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen: mengambil pengguna saat ini,
 *       mengatur visibilitas tombol, menginisialisasi paging, memanggil
 *       {@code onSearchDefault}, dan mendaftarkan tombol ekspor Excel dan upload.</li>
 *   <li>Inner class {@code ClosingRenderer} — merender baris dengan tanggal, nama,
 *       keterangan, tombol Ubah/Copy/Hapus melalui {@code Common.copyEditDeleteButtons},
 *       tombol "Jurnal" yang membuka halaman grup transaksi terkait, dan widget kunci
 *       via {@code GeneralValueObject.tampilKunci}.</li>
 *   <li>{@code onAdd} / {@code init(GeneralValueObject)} — membuka form tambah/ubah.</li>
 *   <li>Metode {@code init(Closing)} (private) — membangun form modal dengan
 *       DateBox tanggal closing dan {@code MyIframe} yang menampilkan pratinjau
 *       jurnal terkait di panel East.</li>
 *   <li>{@code onSave} — memvalidasi, memeriksa duplikasi tanggal, memeriksa
 *       apakah semua jurnal sampai tanggal tersebut sudah balance, lalu menyimpan
 *       dan memicu {@code reload()} via timer.</li>
 *   <li>{@code reload()} — method statis yang memproses ulang asosiasi closing
 *       ke semua grup transaksi yang ada menggunakan SQL UPDATE langsung.</li>
 *   <li>{@code initCriteria} / {@code onSearchDefault} — query dan tampil data.</li>
 *   <li>{@code checkNamaClosing} — pemeriksaan duplikasi tanggal closing.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZK event-dispatcher. Metode {@code reload()}
 * bersifat statis dan dieksekusi via timer ZK default setelah penyimpanan, sehingga
 * tidak memblokir respons UI. {@code reload()} menggunakan session Hibernate terkelola
 * ({@code HibernateUtil.currentSession()}) dan menjalankan SQL UPDATE langsung
 * melalui {@code session.createSQLQuery}. Perlu diperhatikan bahwa pada deployment
 * multi-pengguna, operasi UPDATE ini dapat berdampak global pada tabel
 * {@code akunting.grup_transaksi}.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Tanggal closing di-generate otomatis dari tanggal closing terakhir + 2 bulan
 *   (set ke akhir bulan berikutnya) saat mode tambah; ini memastikan urutan
 *   periode konsisten.<br>
 * - Pemeriksaan balance jurnal ({@code totalDebet != totalKredit}) dilakukan
 *   sebelum menyimpan closing untuk memastikan integritas data akuntansi.<br>
 * - SQL UPDATE di {@code reload()} bersifat destruktif: akan menimpa field
 *   {@code closing} pada semua GrupTransaksi. Backup data sebelum menjalankan
 *   perubahan skema atau migrasi.<br>
 * - Implementasi {@code DataCriteria}, {@code DataSearchDefault}, dan
 *   {@code DataInitDefault} diperlukan agar kelas ini dapat digunakan sebagai
 *   sumber data untuk fitur copy, ekspor, dan upload data.</p>
 */
public class ClosingAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Closing closing;
	private MyToolbarbuttonConfig add;
	private MyDatebox tanggal;
	/** Pengguna yang sedang login; dipakai untuk widget kunci di renderer. */
	private Tbmuser tbmuser;

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Metode ini dipanggil oleh framework ZK sebagai langkah pertama dalam siklus
	 * hidup komposisi halaman, sebelum komponen apapun diinstansiasi.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan ke {@code Common.doCheckSecurity()} yang akan
	 * memverifikasi otentikasi dan hak akses pengguna. Jika pengguna tidak berhak,
	 * akan diarahkan ke halaman logoff. Kemudian memanggil implementasi super
	 * untuk melanjutkan proses komposisi halaman.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK untuk ditangani.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code Common.doCheckSecurity()}. Logika yang
	 * memerlukan komponen ZUL harus diletakkan di {@link #doAfterCompose(Component)}.</p>
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
	 * closing akuntansi setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah inisialisasi yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk lokalisasi bahasa.</li>
	 *   <li>Mendapatkan pengguna saat ini ({@code tbmuser}) yang diperlukan untuk
	 *       widget kunci pada renderer.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" berdasarkan hak CREATE (null-safe).</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} sesuai hak akses.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk tampilan data awal.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil {@code onSearchDefault}
	 *       setiap kali halaman berubah.</li>
	 *   <li>Mendaftarkan tombol ekspor Excel dengan daftar kolom yang ditentukan
	 *       ({@code id, tanggal, nama, keterangan}).</li>
	 *   <li>Mendaftarkan tombol upload data dengan kontrol visibilitas yang memerlukan
	 *       hak CREATE + UPDATE + DELETE sekaligus.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super atau inisialisasi komponen diteruskan ke framework ZK.
	 * Pemeriksaan sesi dilakukan secara implisit melalui hak akses READ.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Array {@code contents} menentukan kolom yang diekspor. Perbarui jika ada
	 * perubahan pada skema entitas {@link Closing}.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai di-autowire
	 * @throws Exception jika terjadi error saat inisialisasi komponen atau framework ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		tbmuser = Common.getCurrentUser();
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

		String[] contents = new String[] { "id", "tanggal", "nama", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Closing.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Closing.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>ClosingRenderer — Renderer Baris Grid Closing Akuntansi</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini bertanggung jawab merender setiap baris pada grid daftar
	 * closing akuntansi. Menampilkan informasi tanggal, nama, keterangan periode
	 * closing beserta kontrol aksi Ubah/Copy/Hapus, tombol "Jurnal" untuk melihat
	 * transaksi dalam periode ini, dan widget kunci untuk mengunci/membuka kunci
	 * data closing.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Untuk setiap objek {@link Closing}:
	 * <ol>
	 *   <li>Menampilkan tanggal closing menggunakan format panjang ({@code dateFormat6}).</li>
	 *   <li>Menampilkan nama dengan widget revisi history.</li>
	 *   <li>Menampilkan keterangan.</li>
	 *   <li>Menampilkan tombol aksi Ubah/Copy/Hapus melalui
	 *       {@code Common.copyEditDeleteButtons}, yang menyertakan callback
	 *       {@code DataInitDefault} untuk membuka form edit dan callback
	 *       {@code DataSearchDefault} untuk menyegarkan grid setelah aksi.</li>
	 *   <li>Menampilkan tombol "Jurnal" yang membuka halaman
	 *       {@code grup_transaksi.zul} dengan parameter closing ID dalam popup 95%.</li>
	 *   <li>Menampilkan widget kunci ({@code GeneralValueObject.tampilKunci}) yang
	 *       memungkinkan penguncian data agar tidak dapat diubah lebih lanjut.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Semua listener event berjalan di thread ZK event-dispatcher. Timer default
	 * digunakan untuk memanggil {@code reload()} setelah operasi Ubah/Copy/Hapus
	 * sehingga asosiasi closing ke grup transaksi diperbarui tanpa memblokir UI.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke {@code arg0} harus sinkron dengan urutan
	 * {@code <column>} di file ZUL closing. Tombol "Jurnal" menggunakan URL
	 * relatif yang harus konsisten dengan konteks servlet aplikasi.</p>
	 */
	class ClosingRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan informasi periode closing
		 * dan semua kontrol interaktif yang diperlukan untuk mengelola data closing.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Metode ini dipanggil oleh framework ZK untuk setiap item dalam model daftar.
		 * Komponen dibuat secara programatik: Label untuk data teks, Hbox untuk
		 * mengelompokkan tombol aksi, dan listener untuk setiap tombol. Callback
		 * {@code DataInitDefault} pada {@code copyEditDeleteButtons} akan membuka
		 * form edit melalui {@code ClosingAction.this.init(obj)}. Setelah operasi
		 * hapus atau copy, timer default dipanggil untuk mengeksekusi {@code reload()}
		 * yang memperbarui asosiasi grup transaksi dengan closing.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception dari event listener diteruskan ke framework ZK. Tombol "Jurnal"
		 * membuka popup yang berdiri sendiri sehingga error di dalamnya tidak
		 * mempengaruhi grid utama.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika ada kolom baru yang ditambahkan ke ZUL, tambahkan juga komponen
		 * terkait di metode render ini. Pastikan {@code tbmuser} sudah terisi
		 * sebelum renderer dipakai (diinisialisasi di {@code doAfterCompose}).</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen UI
		 * @param arg1 objek data, dicast ke {@link Closing}
		 * @throws Exception jika terjadi error saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Closing closing = (Closing) arg1;
			new Label(Common.dateFormat6.get().format(closing.getTanggal())).setParent(arg0);
			RevisiHelper.createNewRevisi(Closing.class, closing, closing.getNama()).setParent(arg0);
			new Label(closing.getKeterangan()).setParent(arg0);

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, edit, delete, closing, new DataInitDefault() {

				@Override
				public void onSearchDefault(Event event) {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings({})
						@Override
						public void onEvent(Event arg0) throws Exception {
							reload();
						}
					});
					ClosingAction.this.onSearchDefault(event);
				}

				@Override
				public void init(GeneralValueObject obj) throws Exception {
					ClosingAction.this.init(obj);
				}
			}, true)).setParent(arg0);
			MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Jurnal", "/img/svg/eye.svg");
			a.setParent(aa);
			a.setOrient("vertical");
			a.setStyle("font-size:9px;");
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					Common.displayWindow("/pages/master/akunting/grup_transaksi.zul?closing=" + closing.getId(), true,
							"95%", "95%", null, "", false);

				}
			});

			GeneralValueObject.tampilKunci(aa, closing, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, true);
		}

	}

	/**
	 * <b>Tujuan:</b> Menangani aksi pengguna ketika menekan tombol "Tambah"
	 * pada toolbar, dengan membuka form untuk menambahkan closing baru.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@link Closing} baru (kosong) dan memanggil
	 * {@code init(Closing)} untuk membangun form secara programatik.
	 * Kemudian menampilkan jendela modal {@code addWindow}.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke framework ZK. Pada kondisi normal tidak ada exception.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika diperlukan nilai default (misalnya nama periode otomatis), lakukan
	 * pengisian sebelum memanggil {@code init()}.</p>
	 *
	 * @param event event ZK dari klik tombol "Tambah"
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form
	 */
	public void onAdd(Event event) throws Exception {
		init(new Closing());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Implementasi dari antarmuka {@link DataInitDefault} yang
	 * memungkinkan renderer dan komponen lain membuka form edit melalui referensi
	 * generik {@link GeneralValueObject}.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menerima objek {@link GeneralValueObject}, melakukan downcast ke {@link Closing},
	 * lalu memanggil metode {@code init(Closing)} (private) untuk membangun form
	 * edit. Setelah form siap, jendela modal ditampilkan.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * ClassCastException akan terjadi jika objek bukan instance {@link Closing};
	 * ini tidak seharusnya terjadi dalam alur normal karena selalu dipanggil
	 * dari konteks yang menggunakan entitas {@link Closing}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini diimplementasikan sesuai kontrak {@code DataInitDefault} yang
	 * diperlukan oleh {@code Common.copyEditDeleteButtons}.</p>
	 *
	 * @param obj objek {@link GeneralValueObject} yang merupakan instance {@link Closing}
	 * @throws Exception jika terjadi error saat membangun form atau menampilkan modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		closing = (Closing) obj;
		init(closing);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form tambah/ubah closing
	 * secara programatik di dalam jendela modal {@code addWindow}, termasuk
	 * panel kanan yang menampilkan pratinjau jurnal dalam periode closing ini.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah pembangunan form:
	 * <ol>
	 *   <li>Menyimpan referensi closing dan mengatur judul jendela.</li>
	 *   <li>Membersihkan konten lama dari addWindow.</li>
	 *   <li>Membangun Borderlayout dengan Center (form input) dan East (pratinjau jurnal).</li>
	 *   <li>Pada panel Center: menampilkan DateBox tanggal closing dan field judul/keterangan.
	 *       Untuk mode tambah, tanggal di-otomatiskan dari tanggal closing terakhir + 1 bulan
	 *       (akhir bulan), dan DateBox dinonaktifkan untuk mencegah pengguna mengubahnya.</li>
	 *   <li>Pada panel East: menampilkan iframe yang berisi daftar jurnal dalam closing ini
	 *       (jika mode ubah) atau pratinjau jurnal sampai tanggal closing (jika mode tambah).</li>
	 *   <li>Toolbar South berisi tombol Batal dan Simpan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Query tanggal otomatis ({@code Projections.max("tanggal")}) mengembalikan null
	 * jika belum ada data closing; kondisi ini ditangani dengan nilai null sehingga
	 * DateBox tetap kosong dan dapat diisi manual oleh pengguna.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Format tanggal pada URL iframe menggunakan {@code Common.dateFormat85} —
	 * pastikan format ini konsisten dengan yang diharapkan oleh halaman
	 * {@code grup_transaksi.zul} untuk parameter {@code tgl_closing}.</p>
	 *
	 * @param closing objek {@link Closing} yang akan diedit (mode ubah jika ID tidak null)
	 *                atau objek baru (mode tambah jika ID null)
	 */
	private void init(Closing closing) {
		this.closing = closing;
		addWindow.setTitle(closing.getId() == null ? "Tambah Closing" : "Ubah Closing");
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

		Session session = HibernateUtil.currentSession();
		Date dateOtomtais = (Date) (closing != null && closing.getId() != null ? null
				: session.createCriteria(Closing.class).setProjection(Projections.max("tanggal")).uniqueResult());

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Closing *"));
		row.appendChild(tanggal = new MyDatebox(closing.getTanggal()));

		if (dateOtomtais != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dateOtomtais);
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 2);
			calendar.set(Calendar.DATE, 1);
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);

			tanggal.setValue(calendar.getTime());
			tanggal.setDisabled(true);
		}

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		if (closing != null && closing.getId() != null) {
			MyIframe include = new MyIframe("/pages/master/akunting/grup_transaksi.zul?closing=" + closing.getId());
			east.appendChild(include);
		} else {
			MyIframe include = new MyIframe("/pages/master/akunting/grup_transaksi.zul?tgl_closing="
					+ Common.dateFormat85.get().format(tanggal.getValue()));
			east.appendChild(include);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Closing *"));
		row.appendChild(nama = new Textbox(closing.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(closing.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi input pengguna dan menyimpan data closing periode
	 * akuntansi ke database, sekaligus memicu proses reassignment jurnal ke closing
	 * yang sesuai.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Proses penyimpanan melalui beberapa tahap validasi dan aksi:
	 * <ol>
	 *   <li><b>Validasi nama:</b> Nama judul closing tidak boleh kosong.</li>
	 *   <li><b>Duplikasi tanggal:</b> Memanggil {@link #checkNamaClosing()} untuk
	 *       memastikan tidak ada closing dengan tanggal yang sama (setiap tanggal
	 *       hanya boleh ada satu closing).</li>
	 *   <li><b>Pemeriksaan balance:</b> Query ke tabel {@link GrupTransaksi} untuk
	 *       menemukan jurnal mana pun (sampai tanggal closing) yang memiliki
	 *       {@code totalDebet != totalKredit}. Jika ada, penyimpanan dibatalkan
	 *       dan pengguna diberitahu kode jurnal yang tidak balance.</li>
	 *   <li><b>Penyimpanan:</b> Pada mode ubah, entitas dimuat ulang dari session
	 *       Hibernate sebelum diupdate untuk menghindari data stale. Kemudian field
	 *       nama, keterangan, dan tanggal diset dan disimpan via
	 *       {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li><b>Reload async:</b> Timer ZK default dijadwalkan untuk memanggil
	 *       {@code reload()} setelah siklus event saat ini selesai, sehingga
	 *       asosiasi grup transaksi ke closing diperbarui tanpa memblokir respons UI.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Setiap validasi yang gagal menampilkan dialog informasi dan mengembalikan
	 * {@code false}. Exception dari Hibernate diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pemeriksaan balance ({@code totalDebet != totalKredit}) menggunakan SQL
	 * langsung; jika nama kolom database berubah, perbarui query di sini. Kondisi
	 * balance yang ketat ini penting untuk menjaga integritas data akuntansi
	 * sebelum periode ditutup.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan, {@code false} jika validasi gagal
	 * @throws Exception jika terjadi error yang tidak tertangani
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Closing belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama Closing dengan nama periode penutupan buku yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaClosing();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Closing sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan tanggal closing yang berbeda; (2) Periksa daftar closing yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan tanggal baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		GrupTransaksi grupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
				.add(Restrictions.sqlRestriction("date(tanggal_transaksi)<=date('"
						+ Common.databaseDateFormat.get().format(tanggal.getValue()) + "')"))
				.add(Restrictions.sqlRestriction("total_debet != total_kredit")).setMaxResults(1).uniqueResult();
		if (grupTransaksi != null) {
			MyMessageboxConfig.show("Jurnal dengan kode \"" + grupTransaksi.getKode() + "\" tidak balance",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (closing.getId() != null) {
			closing = (Closing) session.load(Closing.class, closing.getId());

		}

		closing.setNama(nama.getValue());
		closing.setKeterangan(keterangan.getValue());
		closing.setTanggal(tanggal.getValue());

		Common.refreshSaveOrUpdate(session, closing);

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				reload();
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Memproses ulang (reassign) asosiasi semua {@link GrupTransaksi}
	 * yang ada ke record {@link Closing} yang paling sesuai berdasarkan tanggal,
	 * sehingga setiap jurnal masuk ke periode closing yang tepat.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode ini bersifat statis dan dapat dipanggil dari mana saja:
	 * <ol>
	 *   <li>Mengambil semua data {@link Closing} dari database melalui Hibernate session.</li>
	 *   <li>Jika tidak ada data closing, metode selesai tanpa aksi.</li>
	 *   <li>Membangun {@code SortedMap} dengan kunci integer dari tanggal closing
	 *       (format numerik via {@code dateFormat83}) dan value {@link Closing},
	 *       diurutkan descending menggunakan {@code Collections.reverseOrder()}
	 *       sehingga closing terbaru diproses terakhir (dan nilainya overwrite
	 *       hasil closing yang lebih lama).</li>
	 *   <li>Untuk setiap closing (dari yang tertua ke terbaru karena TreeMap reverse),
	 *       menjalankan SQL UPDATE yang menetapkan field {@code closing} pada
	 *       semua {@code akunting.grup_transaksi} dengan tanggal transaksi
	 *       pada atau sebelum tanggal closing tersebut.</li>
	 *   <li>Menampilkan log query dan jumlah baris yang terpengaruh ke {@code System.out}
	 *       untuk keperluan debugging.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Metode ini berjalan di thread event ZK (dipanggil via timer default).
	 * SQL UPDATE yang dijalankan berdampak pada seluruh tabel grup_transaksi
	 * secara global. Pada sistem dengan traffic tinggi, pertimbangkan untuk
	 * menjalankan ini di luar jam sibuk atau dengan mekanisme penguncian baris.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate atau SQL diteruskan ke pemanggil. Jika exception
	 * terjadi di tengah pemrosesan, sebagian grup transaksi mungkin sudah
	 * terassign ke closing baru sementara sebagian lagi belum. Hal ini dapat
	 * diperbaiki dengan memanggil {@code reload()} ulang.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * SQL UPDATE menggunakan nama tabel dan kolom langsung ({@code akunting.grup_transaksi},
	 * {@code closing}, {@code tanggal_transaksi}). Jika ada perubahan skema database,
	 * perbarui query di sini. Format {@code dateFormat83} harus menghasilkan string
	 * yang dapat diurutkan secara numerik (biasanya format YYYYMMDD).</p>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void reload() {
		Session session = HibernateUtil.currentSession();
		List<Closing> maps = ConstantValues.simpleList(session.createCriteria(Closing.class), Closing.class);
		if (!maps.isEmpty()) {
			SortedMap<Integer, Closing> datas = new TreeMap(Collections.reverseOrder());

			for (Closing closing : maps) {
				if (closing != null) {
					datas.put(Integer.parseInt(Common.dateFormat83.get().format(closing.getTanggal())), closing);
				}
			}

			System.out.println("datas -> " + datas);

			for (Closing closing : datas.values()) {
				String sql = "update akunting.grup_transaksi set closing=" + closing.getId()
						+ " where date(tanggal_transaksi)<=date('"
						+ Common.databaseDateFormat.get().format(closing.getTanggal()) + "') ";

				int s = session.createSQLQuery(sql).executeUpdate();
				System.out.println("sql -> " + sql + " update -> " + s);

			}
		}
	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} untuk query
	 * data {@link Closing} sesuai filter nama yang aktif di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat criteria pada entitas {@link Closing} dengan filter opsional ILIKE
	 * pada field {@code nama} (case-insensitive, menggunakan {@code MatchMode.ANYWHERE}).
	 * Jika parameter {@code order} bernilai true, criteria diurutkan berdasarkan
	 * {@code id} descending sehingga data terbaru muncul di atas. Criteria ini
	 * digunakan oleh {@code onSearchDefault} untuk pengambilan data dan juga
	 * oleh {@code Common.initPaging} untuk menghitung total record.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke pemanggil. Nilai filter yang kosong
	 * menggunakan kondisi SQL {@code true} sehingga tidak memfilter sama sekali.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada filter tambahan (misalnya filter rentang tanggal), tambahkan
	 * kondisi Restrictions di sini dan komponen UI terkait di ZUL.</p>
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY (pengambilan data),
	 *              {@code false} untuk query COUNT (paging)
	 * @return objek {@link Criteria} yang telah dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Closing.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data closing berdasarkan filter yang
	 * aktif dan memperbarui tampilan grid dengan hasil yang ditemukan, disertai
	 * pembaruan komponen paging.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan dua panggilan {@link #initCriteria(boolean)}: pertama dengan
	 * {@code order=false} untuk menghitung total baris dan memperbarui paging,
	 * kemudian dengan {@code order=true} untuk mengambil data halaman aktif
	 * dengan {@code setMaxResults} dan {@code setFirstResult}. Hasil query
	 * dibungkus dalam {@code SimpleListModel} dan diset ke grid dengan renderer
	 * {@code ClosingRenderer} yang baru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke framework ZK. Jika paging null,
	 * data selalu dimulai dari baris pertama.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari banyak tempat (timer, paging, tombol cari).
	 * Pastikan tetap idempoten dan tidak memiliki efek samping selain
	 * memperbarui tampilan grid.</p>
	 *
	 * @param event event ZK pemicu pencarian (bisa null jika dipanggil programatik)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Closing> closing = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(closing);
		grid.setRowRenderer(new ClosingRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah tanggal closing yang dimasukkan pengguna
	 * sudah digunakan oleh record closing lain di database, untuk mencegah
	 * duplikasi tanggal closing.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menjalankan query COUNT pada tabel {@link Closing} dengan kondisi:
	 * <ul>
	 *   <li>Tanggal sama persis ({@code Restrictions.eq("tanggal", tanggal.getValue())}).</li>
	 *   <li>Pada mode ubah: mengecualikan record dengan ID yang sama menggunakan
	 *       {@code Restrictions.ne("id", this.closing.getId())} agar tanggal
	 *       yang tidak berubah tidak dianggap duplikat.</li>
	 * </ul>
	 * Mengembalikan {@code true} jika ada record lain dengan tanggal yang sama.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari query diteruskan ke pemanggil. Null pada uniqueResult
	 * tidak mungkin karena COUNT selalu mengembalikan angka.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pembatasan satu closing per tanggal adalah aturan bisnis yang penting
	 * untuk konsistensi data akuntansi. Jangan ubah logika ini tanpa koordinasi
	 * dengan tim akuntansi.</p>
	 *
	 * @return {@code true} jika tanggal sudah digunakan (duplikat),
	 *         {@code false} jika tanggal aman untuk digunakan
	 */
	public Boolean checkNamaClosing() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Closing.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tanggal", tanggal.getValue()))
				.add(this.closing.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.closing.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
