package ais.action.master.akunting;

import java.util.List;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.KelompokLaporanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>KelompokLaporanAction — Pengelola Kelompok Laporan Keuangan</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Controller ZK berbasis {@link GenericAutowireComposer} yang menangani seluruh siklus
 * hidup data {@link KelompokLaporan} pada modul akuntansi. Kelompok Laporan adalah entitas
 * pengelompokan akun-akun buku besar ke dalam bagian-bagian laporan keuangan tertentu,
 * seperti Aktiva, Kewajiban, Pendapatan, Operasional, maupun Investasi. Setiap kelompok
 * terhubung ke sebuah {@link JenisLaporan} (misalnya Neraca atau Laba Rugi) dan ke sebuah
 * {@link MasterGrupLaporan} yang merupakan grup besar laporan. Dengan demikian, penyusunan
 * laporan keuangan otomatis dapat dilakukan berdasarkan mapping ini tanpa perlu konfigurasi
 * manual berulang.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Halaman ZUL yang dikaitkan dengan kelas ini secara otomatis menyuntikkan (autowire)
 * komponen-komponen UI — grid daftar kelompok, combobox filter (jenis laporan, master grup
 * laporan, teks pencarian nama), serta popup window formulir tambah/ubah — ke field-field
 * bertipe ZK. Saat halaman pertama kali dibuka, {@code doBeforeCompose} memverifikasi hak
 * akses melalui {@link Common#doCheckSecurity()}, kemudian {@code doAfterCompose} mengisi
 * combobox filter, memeriksa hak CRUD pengguna, dan memanggil {@code onSearchDefault} untuk
 * menampilkan data awal. Pengguna yang memiliki hak CREATE dapat membuka formulir melalui
 * tombol Tambah; formulir dibangun secara programatis (bukan ZUL statis) di dalam metode
 * {@code init(KelompokLaporan)}. Setelah Simpan, data divalidasi (keterangan tidak kosong,
 * jenis laporan diisi, master laporan diisi, keterangan belum dipakai) lalu disimpan atau
 * diperbarui melalui {@link KelompokLaporanDao}. Fitur salin akun dari kelompok lain
 * (copyDari) memungkinkan duplikasi konfigurasi akun secara cepat.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh interaksi berjalan di thread UI ZK (event thread). Tidak ada pemrosesan latar
 * belakang. Sesi Hibernate yang dipakai adalah sesi terkelola ({@link HibernateUtil#currentSession()})
 * yang tidak boleh ditutup secara manual oleh kode ini.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika skema MasterGrupLaporan berkembang dengan jenis-jenis baru, fallback hardcode pada
 * {@code doAfterCompose} (Aktiva, Kewajiban, Pendapatan, Operasional, Investasi) perlu
 * diperbarui. Perubahan field pada entitas {@link KelompokLaporan} harus diselaraskan dengan
 * metode {@code init(KelompokLaporan)} (untuk form) dan inner class
 * {@code KelompokLaporanRenderer} (untuk tampilan grid). Hak akses dikontrol melalui
 * {@link CommonPrivilages} dan tidak perlu diubah kecuali ada penambahan role baru.</p>
 *
 * @author Ahmad Tamimi F
 * @see KelompokLaporan
 * @see JenisLaporan
 * @see MasterGrupLaporan
 * @see KelompokLaporanDao
 */
public class KelompokLaporanAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * Nomor versi serialisasi. Digunakan oleh mekanisme serialisasi Java untuk
	 * memastikan kompatibilitas kelas saat deserialisasi. Nilai ini tidak perlu
	 * diubah kecuali ada perubahan struktural yang mempengaruhi format serialisasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Popup window formulir tambah/ubah kelompok laporan yang dibangun secara programatis. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar {@link KelompokLaporan} hasil pencarian. */
	private MyGrid grid;

	/** Field teks pencarian berdasarkan nama/keterangan kelompok laporan. */
	private Textbox searchnama;

	/** Combobox filter berdasarkan {@link JenisLaporan} (misalnya Neraca, Laba Rugi). */
	private Combobox searchjenisLaporan;

	/** Combobox filter berdasarkan objek {@link MasterGrupLaporan}. */
	private Combobox searchmasterGrupLaporan;

	/** Combobox filter berdasarkan nama grup (Aktiva, Kewajiban, dst.). */
	private Combobox searchJenisLaporan;

	/** Input nomor urut tampilan kelompok di dalam laporan. */
	private Doublebox urut;

	/** Input teks sub-laporan / keterangan kelompok laporan. */
	private Textbox keterangan;

	/** Combobox pemilih {@link JenisLaporan} pada formulir tambah/ubah. */
	private Combobox jenisLaporan;

	/** Combobox pemilih {@link MasterGrupLaporan} pada formulir tambah/ubah. */
	private Combobox masterGrupLaporan;

	/** Apakah pengguna memiliki hak UPDATE (ubah) pada modul ini. */
	private boolean edit = false;

	/** Apakah pengguna memiliki hak DELETE (hapus) pada modul ini. */
	private boolean delete = false;

	/** Objek {@link KelompokLaporan} yang sedang aktif di formulir (tambah atau ubah). */
	private KelompokLaporan kelompokLaporan;

	/** Tombol tambah data baru; visibilitasnya bergantung pada hak CREATE pengguna. */
	private MyToolbarbuttonConfig add;

	/**
	 * Dipanggil oleh ZK sebelum komponen halaman dibangun. Memverifikasi hak akses
	 * keamanan pengguna melalui {@link Common#doCheckSecurity()} sebelum halaman
	 * dirender sama sekali. Jika pengguna tidak memiliki sesi yang sah, ZK akan
	 * mengarahkan ke halaman logoff secara otomatis.
	 *
	 * <p><b>Tujuan:</b> Gerbang keamanan pertama sebelum komponen ZUL dibangun.
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa sesi dan token CSRF
	 * untuk mencegah akses tidak sah.</p>
	 *
	 * <p><b>Cara kerja:</b> ZK memanggil metode ini secara otomatis sebagai bagian
	 * dari siklus hidup komposer. Kode hanya perlu memanggil pemeriksaan keamanan
	 * lalu mendelegasikan ke implementasi super agar proses inisialisasi komponen
	 * berjalan normal.</p>
	 *
	 * <p><b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK yang sedang dibangun</li>
	 *   <li>{@code parent} — komponen induk dalam hierarki ZK</li>
	 *   <li>{@code compInfo} — metadata komponen dari file ZUL</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Return:</b> {@code ComponentInfo} yang dikembalikan oleh super, berisi
	 * metadata komponen yang dibutuhkan ZK untuk melanjutkan proses pembuatan.</p>
	 *
	 * <p><b>Penanganan error:</b> {@link Common#doCheckSecurity()} akan melempar
	 * exception atau melakukan redirect jika sesi tidak valid, sehingga halaman
	 * tidak akan dimuat lebih lanjut.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada mekanisme keamanan
	 * tambahan yang perlu dijalankan sebelum komponen dibangun.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam pohon komponen ZK
	 * @param compInfo informasi metadata komponen dari definisi ZUL
	 * @return ComponentInfo yang diteruskan dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil ZK setelah seluruh komponen halaman selesai dibangun dan di-autowire.
	 * Metode ini menginisialisasi state awal halaman: memvalidasi sesi, mengisi
	 * combobox filter, memeriksa hak akses CRUD, dan memuat data awal ke grid.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman Kelompok Laporan agar siap digunakan
	 * pengguna. Ini adalah titik masuk utama inisialisasi setelah komponen ZUL
	 * tersedia dan dapat dimanipulasi melalui Java.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar autowiring ZK selesai.</li>
	 *   <li>Memeriksa atribut sesi {@code usersTemp} dan hak READ. Jika tidak ada,
	 *       memanggil {@link Common#goLogoff()} untuk keluar.</li>
	 *   <li>Mengisi {@code searchJenisLaporan} dengan nama-nama unik dari
	 *       {@link MasterGrupLaporan} yang ada di database. Jika tidak ada data,
	 *       menggunakan fallback hardcode (Aktiva, Kewajiban, Pendapatan, Operasional,
	 *       Investasi).</li>
	 *   <li>Menambahkan item "Semua" dengan nilai null ke combobox filter.</li>
	 *   <li>Mengisi {@code searchjenisLaporan} dengan data {@link JenisLaporan}
	 *       termasuk pilihan "Semua".</li>
	 *   <li>Mengisi {@code searchmasterGrupLaporan} dengan data {@link MasterGrupLaporan}.</li>
	 *   <li>Membuat instance combobox baru untuk formulir tambah/ubah ({@code jenisLaporan}
	 *       dan {@code masterGrupLaporan}) yang akan disuntikkan ke dalam form saat dibuka.</li>
	 *   <li>Mengatur visibilitas tombol Tambah sesuai hak CREATE, dan menyimpan
	 *       flag {@code edit} dan {@code delete} berdasarkan hak UPDATE dan DELETE.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk memuat data awal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b> {@code comp} — komponen root halaman ZK.</p>
	 *
	 * <p><b>Return:</b> Tidak ada (void).</p>
	 *
	 * <p><b>Penanganan error:</b> Jika sesi tidak valid atau hak READ tidak dimiliki,
	 * pengguna diarahkan ke halaman logoff. Exception dari super dilempar kembali.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada penambahan jenis grup laporan baru yang bersifat
	 * statis (tidak dari database), tambahkan ke blok {@code else} fallback. Jika ada
	 * filter baru, tambahkan inisialisasinya di sini.</p>
	 *
	 * @param comp komponen root ZK yang telah selesai dibangun
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen atau akses database
	 */
	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		List<String> namas = HibernateUtil.currentSession().createCriteria(MasterGrupLaporan.class)
				.setProjection(Projections.groupProperty("nama")).list();
		if (!namas.isEmpty()) {
			for (String n : namas) {
				MyComboitemConfig comboitem = new MyComboitemConfig(n);
				comboitem.setValue(n);
				searchJenisLaporan.appendChild(comboitem);
			}
		} else {
			MyComboitemConfig comboitem = new MyComboitemConfig(MasterGrupLaporan.AKTIVA);
			comboitem.setValue(MasterGrupLaporan.AKTIVA);
			searchJenisLaporan.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.KEWAJIBAN);
			comboitem.setValue(MasterGrupLaporan.KEWAJIBAN);
			searchJenisLaporan.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.PENDAPATAN);
			comboitem.setValue(MasterGrupLaporan.PENDAPATAN);
			searchJenisLaporan.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.OPERASIONAL);
			comboitem.setValue(MasterGrupLaporan.OPERASIONAL);
			searchJenisLaporan.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.INVESTASI);
			comboitem.setValue(MasterGrupLaporan.INVESTASI);
			searchJenisLaporan.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchJenisLaporan.appendChild(comboitem);
		if (searchJenisLaporan != null) { searchJenisLaporan.setSelectedItem(comboitem); }
		if (searchJenisLaporan != null) { searchJenisLaporan.setReadonly(true); }

		Common.insertComboDanSemua(searchjenisLaporan, "nama", "keterangan", JenisLaporan.class);

		Common.insertComboDanSemua(searchmasterGrupLaporan, new String[] { "nama", "id" }, "keterangan",
				MasterGrupLaporan.class);

		Common.insertCombo(jenisLaporan = new Combobox(), "nama", "keterangan", JenisLaporan.class);

		Common.insertCombo(masterGrupLaporan = new Combobox(), new String[] { "nama", "id" }, "keterangan",
				MasterGrupLaporan.class);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
	}

	/**
	 * Inner class renderer yang bertugas mengisi setiap baris grid dengan data
	 * dari satu objek {@link KelompokLaporan}. Diinstansiasi ulang setiap kali
	 * {@code onSearchDefault} dipanggil untuk memastikan data yang tampil selalu segar.
	 *
	 * <p><b>Tujuan:</b> Memisahkan logika render tampilan grid dari logika bisnis
	 * controller utama, sesuai pola MVC yang dipakai di seluruh modul AIS. Setiap
	 * baris menampilkan nomor urut, jenis laporan, nama grup laporan (dengan link
	 * revisi), keterangan grup, keterangan kelompok, dan tombol aksi Ubah/Hapus.</p>
	 *
	 * <p><b>Cara kerja:</b> ZK memanggil {@code render(Row, Object)} untuk setiap
	 * elemen model. Metode ini mengcast {@code arg1} menjadi {@link KelompokLaporan},
	 * lalu menambahkan komponen Label dan tombol aksi sebagai anak dari {@code arg0}
	 * (baris grid). {@link RevisiHelper#createNewRevisi} digunakan untuk menampilkan
	 * nama master grup laporan sekaligus tombol riwayat perubahan (audit trail).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada kolom baru yang ditambahkan ke grid ZUL,
	 * pastikan urutan penambahan komponen di sini sesuai dengan urutan kolom di ZUL
	 * agar tidak terjadi pergeseran data.</p>
	 */
	class KelompokLaporanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi satu baris grid dengan data dari satu objek {@link KelompokLaporan}.
		 *
		 * <p><b>Tujuan:</b> Menampilkan data kelompok laporan secara visual di grid
		 * dengan kolom-kolom: No. Urut, Jenis Laporan, Nama Master Grup (dengan
		 * link revisi), Keterangan Grup, Sub Laporan, dan tombol Ubah/Hapus.</p>
		 *
		 * <p><b>Cara kerja:</b> Membuat komponen Label untuk setiap kolom data dan
		 * menambahkannya sebagai anak dari baris ({@code arg0}). Untuk kolom nama
		 * master grup laporan digunakan {@link RevisiHelper#createNewRevisi} agar
		 * pengguna dapat melihat riwayat perubahan entitas tersebut. Nomor urut
		 * diformat menggunakan {@link Common#numberFormat} agar konsisten dengan
		 * locale yang dikonfigurasi. Tombol Ubah dan Hapus ditambahkan melalui
		 * {@link Common#copyEditDeleteButtons} yang secara otomatis memperhatikan
		 * flag {@code edit} dan {@code delete} dari controller induk.</p>
		 *
		 * <p><b>Penanganan error:</b> ZK akan menangkap exception yang dilempar
		 * dari metode ini dan menampilkan pesan error di antarmuka.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Pastikan urutan appendChild sesuai dengan urutan
		 * kolom di file ZUL kelompok_laporan.zul agar data tidak bergeser.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi komponen
		 * @param arg1 objek data, akan dicast ke {@link KelompokLaporan}
		 * @throws Exception jika terjadi kesalahan saat menambahkan komponen ke baris
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			KelompokLaporan kelompokLaporan = (KelompokLaporan) arg1;

			new Label(kelompokLaporan.getUrut() == null ? "" : Common.numberFormat.get().format(kelompokLaporan.getUrut()))
					.setParent(arg0);
			new Label(kelompokLaporan.getJenisLaporan() == null ? "" : kelompokLaporan.getJenisLaporan().getNama())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(KelompokLaporan.class, kelompokLaporan,
					kelompokLaporan.getMasterGrupLaporan() == null ? ""
							: kelompokLaporan.getMasterGrupLaporan().getNama())
					.setParent(arg0);

			new Label(kelompokLaporan.getMasterGrupLaporan() == null ? ""
					: kelompokLaporan.getMasterGrupLaporan().getKeterangan()).setParent(arg0);

			new Label(kelompokLaporan.getKeterangan()).setParent(arg0);
			Common.copyEditDeleteButtons(edit, delete, kelompokLaporan, KelompokLaporanAction.this).setParent(arg0);
		}

	}

	/**
	 * Menangani klik tombol Tambah pada toolbar halaman. Membuat objek
	 * {@link KelompokLaporan} baru (kosong) dan membuka formulir tambah data
	 * dalam mode modal popup.
	 *
	 * <p><b>Tujuan:</b> Menyediakan titik masuk bagi pengguna yang ingin menambahkan
	 * kelompok laporan baru. Formulir dibuat secara programatis oleh metode
	 * {@code init(KelompokLaporan)} sehingga judul window otomatis menjadi
	 * "Tambah Kelompok Laporan".</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code init(new KelompokLaporan())} untuk
	 * membangun ulang konten formulir dengan data kosong, kemudian menampilkan
	 * {@code addWindow} dalam mode modal agar pengguna fokus pada pengisian data
	 * sebelum melanjutkan aktivitas lain.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception yang dilempar oleh {@code addWindow.onModal()}
	 * akan disebarkan ke ZK dan ditampilkan sebagai error dialog.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika inisialisasi formulir memerlukan data pre-fill
	 * tambahan (misalnya default jenis laporan berdasarkan konteks pengguna), lakukan
	 * modifikasi setelah pemanggilan {@code init} di sini atau di dalam metode
	 * {@code init} itu sendiri.</p>
	 *
	 * @param event event klik ZK dari tombol Tambah di toolbar
	 * @throws Exception jika terjadi kesalahan saat membuka window modal
	 */
	public void onAdd(Event event) throws Exception {
		init(new KelompokLaporan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun atau memperbarui konten formulir tambah/ubah di dalam {@code addWindow}
	 * berdasarkan objek {@link KelompokLaporan} yang diberikan. Metode ini dipanggil
	 * baik untuk mode tambah (objek baru) maupun mode ubah (objek yang sudah ada).
	 *
	 * <p><b>Tujuan:</b> Menyiapkan formulir interaktif yang memungkinkan pengguna
	 * mengisi atau mengubah data kelompok laporan. Formulir dibangun secara programatis
	 * menggunakan komponen ZK (Borderlayout, MyGrid, MyFormRow) agar mudah dikontrol
	 * dari Java tanpa bergantung pada ZUL statis untuk formulir ini.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi ke objek kelompokLaporan di field instance.</li>
	 *   <li>Mengatur judul window: "Tambah Kelompok Laporan" jika id null, "Ubah..."
	 *       jika sudah ada id.</li>
	 *   <li>Membersihkan konten window sebelumnya dengan {@link Common#clear}.</li>
	 *   <li>Membangun layout: Borderlayout dengan Center (berisi grid formulir) dan
	 *       South (berisi toolbar Batal/Simpan).</li>
	 *   <li>Menambahkan baris formulir: No. Urut (Doublebox, default 1.0 jika baru),
	 *       Jenis Laporan (Combobox), Master Laporan (Combobox), Sub Laporan/Keterangan
	 *       (Textbox multiline 4 baris).</li>
	 *   <li>Tombol Batal menutup window; tombol Simpan memanggil {@code onSave},
	 *       merefresh grid, dan menutup window jika berhasil.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception saat membangun komponen akan menyebabkan
	 * halaman error ZK. Validasi data dilakukan di {@code onSave} bukan di sini.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field baru pada entitas {@link KelompokLaporan},
	 * tambahkan MyFormRow yang sesuai di dalam blok Rows. Pastikan field instance
	 * juga ditambahkan di kelas ini agar bisa diakses dari {@code onSave}.</p>
	 *
	 * @param kelompokLaporan objek kelompok laporan yang akan ditampilkan/diedit di formulir;
	 *                        jika id-nya null maka mode tambah, jika ada id maka mode ubah
	 */
	private void init(KelompokLaporan kelompokLaporan) {
		this.kelompokLaporan = kelompokLaporan;
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
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Urut"));
		row.appendChild(urut = new MyDoublebox(kelompokLaporan.getUrut() == null ? 1.0 : kelompokLaporan.getUrut()));
		urut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Laporan"));
		row.appendChild(jenisLaporan);
		Common.selectComboItem(jenisLaporan, kelompokLaporan.getJenisLaporan());
		jenisLaporan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Master Laporan"));
		row.appendChild(masterGrupLaporan);
		Common.selectComboItem(masterGrupLaporan, kelompokLaporan.getMasterGrupLaporan());
		masterGrupLaporan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sub Laporan"));
		row.appendChild(keterangan = new Textbox(
				kelompokLaporan.getKeterangan() == null ? "" : kelompokLaporan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
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
	 * Memvalidasi dan menyimpan data kelompok laporan dari formulir ke database.
	 * Metode ini dipanggil saat pengguna menekan tombol Simpan pada popup formulir.
	 *
	 * <p><b>Tujuan:</b> Melakukan validasi input secara berurutan sebelum menyimpan
	 * atau memperbarui entitas {@link KelompokLaporan} ke database. Jika validasi
	 * gagal, pesan peringatan ditampilkan dan proses dihentikan (return false).
	 * Jika berhasil, data disimpan dan method mengembalikan true agar caller
	 * dapat menutup popup dan merefresh grid.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi keterangan tidak boleh kosong.</li>
	 *   <li>Cek duplikasi keterangan via {@code checkNamaKelLaporan()}.</li>
	 *   <li>Validasi jenis laporan dan master laporan harus dipilih.</li>
	 *   <li>Jika ada duplikasi, tampilkan pesan dan return false.</li>
	 *   <li>Jika mode ubah (id tidak null), reload entitas dari database via DAO
	 *       untuk menghindari stale object Hibernate.</li>
	 *   <li>Mengisi field entitas dari input formulir.</li>
	 *   <li>Jika baru: {@code dao.save}; jika ubah: {@code dao.update}.</li>
	 *   <li>Jika ada {@code copyDari} (salin dari kelompok lain), menduplikasi
	 *       relasi {@link KelompokLaporanPunyaAkun} yang belum ada di kelompok tujuan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika terjadi ConstraintViolation atau error database,
	 * exception akan disebarkan ke ZK. Tidak ada try-catch di sini; caller bertanggung
	 * jawab menangani error level atas.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field wajib baru pada {@link KelompokLaporan},
	 * tambahkan validasi di awal metode ini sebelum blok penyimpanan. Fitur salin akun
	 * (copyDari) menggunakan query Criteria; jika performa bermasalah pada dataset besar,
	 * pertimbangkan batch insert.</p>
	 *
	 * @param event event ZK dari tombol Simpan (bisa null jika dipanggil programatis)
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan database atau runtime saat penyimpanan
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Keterangan dengan nama kelompok laporan yang sesuai; (2) Pastikan keterangan tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			keterangan.focus();
			return false;
		}

		boolean i = checkNamaKelLaporan();

		if (jenisLaporan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Laporan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Laporan dari dropdown yang tersedia; (2) Pastikan jenis laporan yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (masterGrupLaporan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Master Laporan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Master Laporan dari dropdown yang tersedia; (2) Pastikan master laporan yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan Kelompok Laporan sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan keterangan yang berbeda dan belum terdaftar; (2) Periksa daftar kelompok laporan yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan keterangan baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		KelompokLaporanDao kelompokLaporanDao = DaoFactory.getInstance().getKelompokLaporanDao();
		if (kelompokLaporan.getId() != null) {
			kelompokLaporan = kelompokLaporanDao.load(kelompokLaporan.getId());

		}

		kelompokLaporan.setUrut(urut.getValue());
		kelompokLaporan.setJenisLaporan((JenisLaporan) jenisLaporan.getSelectedItem().getValue());
		kelompokLaporan.setMasterGrupLaporan((MasterGrupLaporan) masterGrupLaporan.getSelectedItem().getValue());
		kelompokLaporan.setKeterangan(keterangan.getValue());

		if (kelompokLaporan.getId() != null) {
			kelompokLaporanDao.update(kelompokLaporan);
		} else {
			kelompokLaporanDao.save(kelompokLaporan);
		}

		if (kelompokLaporan.getCopyDari() != null && kelompokLaporan.getCopyDari().getId() != null) {
			Session session = kelompokLaporanDao.getCurrentSession();

			List<KelompokLaporanPunyaAkun> laporanPunyaAkuns = session.createCriteria(KelompokLaporanPunyaAkun.class)
					.add(Restrictions.eq("kelompokLaporan.id", kelompokLaporan.getCopyDari().getId())).list();

			int dilewatiKarenaBentrok = 0;
			for (KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun : laporanPunyaAkuns) {
				KelompokLaporanPunyaAkun kelompokLaporanPunyaAkunBaru = (KelompokLaporanPunyaAkun) session
						.createCriteria(KelompokLaporanPunyaAkun.class)
						.add(Restrictions.eq("akun", kelompokLaporanPunyaAkun.getAkun()))
						.add(Restrictions.eq("kelompokLaporan", this.kelompokLaporan)).addOrder(Order.desc("id"))
						.setMaxResults(1).uniqueResult();
				if (kelompokLaporanPunyaAkunBaru == null) {
					if (cariBentrokJenisLaporan(kelompokLaporanPunyaAkun.getAkun(), this.kelompokLaporan,
							null) != null) {
						// Akun ini sudah terpetakan ke kelompok lain pada jenis laporan yang sama;
						// menyalinnya ke sini akan melipatgandakan nominal (lihat KelompokLaporanPunyaAkun).
						dilewatiKarenaBentrok++;
						continue;
					}
					kelompokLaporanPunyaAkunBaru = new KelompokLaporanPunyaAkun();
					kelompokLaporanPunyaAkunBaru.setAkun(kelompokLaporanPunyaAkun.getAkun());
					kelompokLaporanPunyaAkunBaru.setKelompokLaporan(this.kelompokLaporan);
					session.save(kelompokLaporanPunyaAkunBaru);
					session.flush();
				}

			}
			if (dilewatiKarenaBentrok > 0) {
				MyMessageboxConfig.show(dilewatiKarenaBentrok
						+ " akun dari kelompok sumber TIDAK disalin karena sudah terpetakan ke kelompok laporan lain pada jenis laporan yang sama (mencegah nominal berlipat pada laporan dan jurnal penutup).",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		}

		return true;
	}

	/**
	 * Mencari baris {@link KelompokLaporanPunyaAkun} LAIN yang memetakan akun yang sama ke
	 * kelompok laporan pada jenis laporan yang sama dengan {@code kelompokTujuan}.
	 *
	 * <p>Dipakai oleh {@link #onSave(Event)} untuk menyaring fitur "copy dari" agar tidak
	 * menduplikasi akun lintas kelompok pada jenis laporan yang sama — lihat Javadoc
	 * {@link KelompokLaporanPunyaAkun} untuk latar belakang dampaknya pada jurnal penutup dan
	 * dashboard akuntansi. Kembaran dari method sejenis di
	 * {@code KelompokLaporanPunyaAkunAction}/{@code KelompokLaporanDanDetailAction}, disengaja
	 * mengikuti pola berkas-berkas ini yang sudah menduplikasi blok "copy dari" alih-alih
	 * membagikannya lewat kelas util bersama.
	 *
	 * @param akun akun yang akan dipetakan
	 * @param kelompokTujuan kelompok laporan tujuan pemetaan
	 * @param idBarisIni id baris {@link KelompokLaporanPunyaAkun} yang sedang diubah (boleh
	 *                   {@code null} untuk baris baru), dikecualikan dari pencarian
	 * @return kelompok laporan lain yang sudah memuat akun ini pada jenis laporan yang sama,
	 *         atau {@code null} bila tidak ada bentrok
	 */
	@SuppressWarnings("unchecked")
	private KelompokLaporan cariBentrokJenisLaporan(Akun akun, KelompokLaporan kelompokTujuan, Long idBarisIni) {
		if (akun == null || kelompokTujuan == null || kelompokTujuan.getJenisLaporan() == null) {
			return null;
		}
		String hql = "select k from KelompokLaporanPunyaAkun k "
				+ "where k.akun = :akun and k.kelompokLaporan.jenisLaporan = :jenisLaporan "
				+ (idBarisIni != null ? "and k.id != :idBarisIni " : "") + "order by k.id";
		org.hibernate.Query query = HibernateUtil.currentSession().createQuery(hql);
		query.setParameter("akun", akun);
		query.setParameter("jenisLaporan", kelompokTujuan.getJenisLaporan());
		if (idBarisIni != null) {
			query.setParameter("idBarisIni", idBarisIni);
		}
		List<KelompokLaporanPunyaAkun> bentrok = query.setMaxResults(1).list();
		return bentrok.isEmpty() ? null : bentrok.get(0).getKelompokLaporan();
	}

	/**
	 * Menjalankan pencarian data {@link KelompokLaporan} berdasarkan filter yang aktif
	 * pada form pencarian, lalu memperbarui tampilan grid dengan hasil yang ditemukan.
	 *
	 * <p><b>Tujuan:</b> Menjadi handler utama untuk event pencarian data. Dipanggil
	 * saat halaman pertama kali dimuat (dari {@code doAfterCompose}), saat pengguna
	 * mengubah filter pencarian, dan setelah operasi simpan/hapus agar grid selalu
	 * menampilkan data terkini.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mendapatkan sesi Hibernate saat ini via {@link HibernateUtil#currentSession()}.</li>
	 *   <li>Membangun query Criteria pada entitas {@link KelompokLaporan} dengan
	 *       createAlias ke masterGrupLaporan untuk memungkinkan filter berdasarkan
	 *       nama grup.</li>
	 *   <li>Menerapkan empat filter opsional:
	 *     <ul>
	 *       <li>Filter {@code searchjenisLaporan}: filter berdasarkan objek JenisLaporan.</li>
	 *       <li>Filter {@code searchJenisLaporan}: filter berdasarkan nama string grup
	 *           (misalnya "Aktiva").</li>
	 *       <li>Filter {@code searchmasterGrupLaporan}: filter berdasarkan objek
	 *           MasterGrupLaporan.</li>
	 *       <li>Filter {@code searchnama}: pencarian teks bebas ILIKE pada kolom
	 *           keterangan.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Filter yang tidak aktif (null atau belum dipilih) menggunakan
	 *       {@code Restrictions.sqlRestriction("1=1")} sebagai no-op.</li>
	 *   <li>Hasil diurutkan berdasarkan field {@code urut} secara ascending.</li>
	 *   <li>Dibatasi oleh {@link Common#MAX_RESULT} untuk menghindari query tanpa batas.</li>
	 *   <li>Model list sederhana ({@link SimpleListModel}) diset ke grid bersama renderer.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate akan disebarkan ke ZK.
	 * Tidak ada halaman khusus; error ditampilkan sebagai dialog oleh framework.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada filter baru yang ditambahkan, tambahkan
	 * klausa {@code .add(...)} baru pada Criteria builder. Pastikan alias yang
	 * dibutuhkan sudah didefinisikan via {@code createAlias}.</p>
	 *
	 * @param event event ZK yang memicu pencarian; bisa null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<KelompokLaporan> kelompokLaporans = session.createCriteria(KelompokLaporan.class)
				.createAlias("masterGrupLaporan", "masterGrupLaporan").addOrder(Order.asc("urut"))
				.add(searchjenisLaporan.getSelectedItem() == null
						|| searchjenisLaporan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisLaporan", searchjenisLaporan.getSelectedItem().getValue()))

				.add(searchJenisLaporan.getSelectedItem() == null
						|| searchJenisLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masterGrupLaporan.nama",
										searchJenisLaporan.getSelectedItem().getValue()))

				.add(searchmasterGrupLaporan.getSelectedItem() == null
						|| searchmasterGrupLaporan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masterGrupLaporan",
										searchmasterGrupLaporan.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(kelompokLaporans);
		grid.setRowRenderer(new KelompokLaporanRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * Memeriksa apakah keterangan kelompok laporan yang diinput sudah ada di database,
	 * dengan pengecualian untuk entitas yang sedang diedit (agar data yang tidak
	 * diubah tidak dianggap duplikat).
	 *
	 * <p><b>Tujuan:</b> Mencegah duplikasi data kelompok laporan berdasarkan field
	 * keterangan (yang berfungsi sebagai nama tampilan). Dipanggil dari {@code onSave}
	 * sebelum proses penyimpanan dilakukan.</p>
	 *
	 * <p><b>Cara kerja:</b> Menggunakan Hibernate Criteria dengan
	 * {@link Projections#rowCount()} untuk menghitung jumlah record yang memiliki
	 * keterangan sama (case-sensitive equality). Jika sedang mode edit (id tidak null),
	 * menambahkan Restrictions.ne("id", ...) agar record yang sedang diedit tidak
	 * dihitung sebagai duplikat. Mengembalikan true jika jumlah lebih dari 0
	 * (artinya sudah ada duplikat).</p>
	 *
	 * <p><b>Return:</b> {@code true} jika keterangan sudah dipakai oleh record lain;
	 * {@code false} jika keterangan belum dipakai (aman untuk disimpan).</p>
	 *
	 * <p><b>Penanganan error:</b> Jika query gagal, exception Hibernate disebarkan
	 * ke caller. Tidak ada fallback.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika keunikan perlu diubah (misalnya unik per JenisLaporan),
	 * tambahkan klausa Restrictions tambahan pada Criteria di sini. Saat ini hanya
	 * memeriksa keunikan global tanpa memperhatikan jenis laporan.</p>
	 *
	 * @return {@code true} jika nama/keterangan sudah ada di database (duplikat);
	 *         {@code false} jika belum ada (unik)
	 */
	public Boolean checkNamaKelLaporan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokLaporan.class).setProjection(Projections.rowCount())

				.add(Restrictions.eq("keterangan", keterangan.getValue().trim()))
				.add(this.kelompokLaporan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokLaporan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * Implementasi antarmuka {@link DataInitDefault} yang memungkinkan controller lain
	 * membuka formulir edit kelompok laporan secara programatis, misalnya dari
	 * tombol Ubah pada baris grid yang menggunakan pola generik.
	 *
	 * <p><b>Tujuan:</b> Menyediakan titik masuk standar untuk membuka formulir ubah
	 * data dari luar kelas (misalnya dari {@link Common#copyEditDeleteButtons}).
	 * Dengan mengimplementasikan {@link DataInitDefault}, kelas ini kompatibel dengan
	 * pola generik tombol Ubah di seluruh modul AIS.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengcast parameter {@link GeneralValueObject} ke
	 * {@link KelompokLaporan}, memanggil {@code init(KelompokLaporan)} untuk
	 * membangun formulir, kemudian menampilkan window dalam mode modal. Dengan
	 * demikian, pola yang sama seperti tombol "onAdd" digunakan, hanya saja
	 * objek yang diteruskan sudah berisi data yang akan diedit.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK untuk ditampilkan
	 * sebagai dialog error.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika entitas berubah (bukan lagi {@link KelompokLaporan}),
	 * sesuaikan casting di sini. Jangan mengubah signature metode karena ini
	 * kontrak dari interface {@link DataInitDefault}.</p>
	 *
	 * @param obj objek {@link GeneralValueObject} yang akan dicast ke {@link KelompokLaporan}
	 *            dan ditampilkan di formulir edit
	 * @throws Exception jika terjadi kesalahan saat membangun formulir atau membuka modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		KelompokLaporan kelompokLaporan = (KelompokLaporan) obj;
		init(kelompokLaporan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
