package ais.action.master.asset;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.JenisPenerimaanBarang;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisPenerimaanBarangAction — Kontroler CRUD Jenis Penerimaan Barang</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZK (ZKoss Composer) untuk halaman manajemen jenis penerimaan
 * barang di modul manajemen aset eCampus. Jenis penerimaan barang adalah data referensi
 * (master data) yang mengklasifikasikan cara barang/aset diterima oleh institusi, misalnya:
 * "Pembelian Langsung", "Hibah", "Pengadaan Melalui Vendor", dan sebagainya. Setiap jenis
 * penerimaan terhubung ke dua akun akuntansi: akun hutang sementara (digunakan saat barang
 * pertama kali diterima) dan akun hutang penyedia (digunakan saat tagihan dari vendor diterima).
 * Pendekatan dua-akun ini memisahkan pencatatan penerimaan fisik barang dari pencatatan
 * kewajiban keuangan terhadap vendor, sesuai standar akuntansi berbasis akrual.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Halaman ini menggunakan pola CRUD standar eCampus dengan GenericAutowireComposer ZKoss 5.5.
 * Komponen-komponen filter (searchnama, serachkode, searchaktif, searchparent) di-autowire
 * otomatis dari file ZUL. Saat pertama dimuat, {@code doAfterCompose} memeriksa sesi
 * pengguna, mengatur hak akses, dan memuat data awal ke grid. Form tambah/ubah dibangun
 * secara programatik di metode {@code init(JenisPenerimaanBarang)}, yang juga mendukung
 * pemilihan akun via {@code AmbilDataAkunBanbox} (komponen ZK khusus pencarian akun).
 * Setelah simpan berhasil, cache data default di-reload via {@code JenisPenerimaanBarang.reloadDefault()}.
 * Kelas ini juga menyediakan metode utilitas statis {@code hitungSaldo} untuk menghitung
 * saldo uang muka workspace yang masih tersedia pada tanggal tertentu.<br><br>
 *
 * <b>Threading:</b><br>
 * Semua operasi database dilakukan di thread utama ZK. Metode {@code hitungSaldo} bersifat
 * statis dan membuka serta menutup sesi Hibernate secara eksplisit; penggunaan bersamaan
 * dari beberapa thread perlu dihindari karena HibernateUtil menggunakan ThreadLocal.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan {@code DataInitDefault} untuk mendukung inisialisasi form
 * dari mekanisme ubah generik. Filter pencarian mendukung kombinasi kode, nama, status aktif,
 * dan satuan kerja. Jika akun akuntansi baru perlu ditambahkan (misalnya akun kas), perbarui
 * form di {@code init()}, {@code onSave()}, entitas {@code JenisPenerimaanBarang}, dan
 * DDL database.
 *
 * @author eCampus Dev Team
 * @version 1.0
 */
public class JenisPenerimaanBarangAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * Serial version UID untuk serialisasi kelas ZK Composer ini.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	/** Jendela modal untuk form tambah/ubah jenis penerimaan barang. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk grid daftar jenis penerimaan barang. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis penerimaan barang. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama jenis penerimaan. */
	private Textbox serachnama;

	/** Kotak teks filter pencarian berdasarkan kode jenis penerimaan. */
	private Textbox serachkode;

	/** Checkbox filter untuk menampilkan hanya data aktif atau semua data. */
	private Checkbox searchaktif;

	/** Kotak teks nama jenis penerimaan barang pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks kode jenis penerimaan barang pada form tambah/ubah. */
	private Textbox kode;

	/** Kotak teks keterangan tambahan pada form tambah/ubah. */
	private Textbox keterangan;

	/** Banbox untuk memilih akun hutang sementara (akun saat barang diterima) pada form. */
	private AmbilDataAkunBanbox akun;

	/** Entitas jenis penerimaan barang yang sedang dikelola (tambah atau ubah). */
	public JenisPenerimaanBarang jenisPenerimaanBarang;

	/** Tombol toolbar tambah data baru. */
	private MyToolbarbuttonConfig add;

	/** Penanda apakah pengguna memiliki hak ubah data. */
	private boolean edit;

	/** Penanda apakah pengguna memiliki hak hapus data. */
	private boolean delete;

	/** Banbox untuk memilih satuan kerja pada form tambah/ubah. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Model pohon satuan kerja untuk navigasi hierarki unit kerja pada filter. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Banbox filter berdasarkan satuan kerja (unit kerja/divisi). */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Banbox untuk memilih akun hutang penyedia (akun saat tagihan diterima) pada form. */
	private AmbilDataAkunBanbox akunHutangPenyedia;

	/**
	 * Pemeriksaan keamanan sebelum halaman dikompilasi oleh ZK.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan pengguna yang mengakses halaman jenis penerimaan barang telah memiliki
	 * sesi yang valid dan hak akses yang sesuai sebelum proses komposisi halaman dimulai.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk pemeriksaan sesi dan hak akses,
	 * kemudian memanggil {@code super.doBeforeCompose} untuk melanjutkan proses ZK normal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pola standar keamanan eCampus di semua halaman. Jangan modifikasi tanpa koordinasi
	 * tim keamanan.
	 *
	 * @param page     Halaman ZK yang sedang dimuat.
	 * @param parent   Komponen induk.
	 * @param compInfo Informasi metadata komponen ZUL.
	 * @return {@code ComponentInfo} untuk melanjutkan proses komposisi ZK.
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
	 * Titik masuk utama inisialisasi halaman jenis penerimaan barang. Dipanggil otomatis
	 * oleh ZK setelah semua komponen di-autowire. Memeriksa sesi dan hak akses, lalu
	 * mengatur filter satuan kerja, visibilitas tombol, dan data awal grid.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code super.doAfterCompose} dan menginisialisasi bahasa.
	 * (2) Memeriksa sesi dan hak akses READ; jika tidak valid, mengarahkan ke logoff.
	 * (3) Mengatur event listener pada banbox satuan kerja filter.
	 * (4) Menginisialisasi model pohon satuan kerja.
	 * (5) Mengatur visibilitas tombol tambah berdasarkan hak akses CREATE.
	 * (6) Membaca hak akses UPDATE dan DELETE ke flag edit dan delete.
	 * (7) Memanggil {@code onSearchDefault} untuk memuat data awal.
	 * (8) Mengatur listener paginasi.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi tidak valid atau hak akses READ tidak ada, pengguna diarahkan ke logoff
	 * dan metode berhenti lebih awal dengan {@code return}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak ada tombol cetak atau upload di halaman ini karena sudah ditangani di halaman induk.
	 *
	 * @param comp Komponen akar halaman ZUL yang telah di-autowire.
	 * @throws Exception Jika terjadi kesalahan inisialisasi ZK atau Hibernate.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}

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
	 * Menghitung saldo uang muka yang tersisa untuk workspace tertentu pada tanggal tertentu.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode utilitas statis ini digunakan untuk menghitung saldo uang muka (workspace)
	 * yang masih tersedia hingga tanggal yang diberikan. Digunakan dalam konteks perhitungan
	 * keuangan penerimaan barang untuk memastikan ketersediaan dana sebelum transaksi.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memeriksa apakah workspace null atau tidak aktif; jika ya, mengembalikan 0.0.
	 * (2) Mengambil harga total workspace sebagai saldo awal.
	 * (3) Membuka sesi Hibernate native baru (dedicated session, bukan session ZK).
	 * (4) Menambahkan 1 menit ke tanggal target untuk inklusivitas batas atas.
	 * (5) Menjumlahkan semua nilai {@code UangMuka} untuk workspace tersebut yang dibuat
	 *     sebelum atau sama dengan tanggal target dan berstatus aktif (null dianggap aktif).
	 * (6) Menutup dan memutuskan sesi Hibernate setelah query selesai.
	 * (7) Mengembalikan selisih antara saldo awal dan total yang sudah terpakai.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * {@code bukanId} — ID yang dikecualikan dari perhitungan (untuk kasus edit, tidak
	 *   menghitung transaksi yang sedang diedit). Saat ini tidak digunakan dalam query.<br>
	 * {@code workspace} — Objek workspace sumber dana yang akan dihitung saldonya.<br>
	 * {@code tanggal} — Tanggal batas atas perhitungan saldo.<br><br>
	 *
	 * <b>Return:</b><br>
	 * Saldo uang muka yang tersisa (dalam satuan mata uang yang digunakan sistem).<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada try-catch eksplisit. Kegagalan database akan melempar exception ke pemanggil.
	 * Sesi Hibernate ditutup secara manual setelah query selesai.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Parameter {@code bukanId} saat ini tidak digunakan dalam query Hibernate. Jika perlu
	 * mengecualikan transaksi tertentu, tambahkan {@code Restrictions.ne("id", bukanId)}
	 * ke criteria. Log debug di baris terakhir sebaiknya dihapus di produksi.
	 *
	 * @param bukanId   ID transaksi yang dikecualikan (belum diimplementasikan dalam query).
	 * @param workspace Objek workspace yang akan dihitung saldonya; null atau tidak aktif
	 *                  menghasilkan return 0.0.
	 * @param tanggal   Tanggal batas atas untuk perhitungan transaksi yang sudah terpakai.
	 * @return Saldo uang muka yang tersedia untuk workspace pada tanggal yang diberikan.
	 */
	public static Double hitungSaldo(Long bukanId, Workspace workspace, Date tanggal) {

		if (workspace == null || !workspace.getAktif()) {
			return 0.0;
		}

		Double saldoAwal = workspace.getHargaTotal();

		Session session = HibernateUtil.currentNativeSession();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);
		calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 1);
		String sqlRes = "this_.tanggal_pembuatan <= '" + Common.databaseDateFormat1.get().format(calendar.getTime()) + "'";

		Number terpakai = ((Number) session.createCriteria(UangMuka.class).add(Restrictions.eq("workspace", workspace))
				.add(Restrictions.sqlRestriction(sqlRes)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.sum("nilai")).uniqueResult());

		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		Double saldo = saldoAwal - (terpakai == null ? 0.0 : terpakai.doubleValue());

		System.out.println("saldoAwal -> " + saldoAwal + ", terpakai -> " + terpakai + ", saldo -> " + saldo);
		return saldo;
	}

	/**
	 * Renderer baris grid untuk daftar jenis penerimaan barang.
	 *
	 * <b>Tujuan:</b><br>
	 * Merender setiap entri {@code JenisPenerimaanBarang} ke dalam baris grid ZK.
	 * Setiap baris menampilkan kode, nama, keterangan, akun hutang sementara, akun
	 * hutang penyedia, satuan kerja, checkbox aktif, dan tombol ubah/hapus.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode {@code render} dipanggil per baris oleh ZK. Checkbox aktif dapat langsung
	 * diklik pengguna untuk mengubah status aktif tanpa membuka form ubah. Tombol
	 * ubah/hapus menggunakan {@code Common.copyEditDeleteButtons} yang memanggil
	 * {@code init} melalui antarmuka {@code DataInitDefault}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penambahan kolom baru memerlukan sinkronisasi dengan header kolom di ZUL.
	 */
	class JenisPenerimaanBarangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data jenis penerimaan barang ke dalam baris grid ZK.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi setiap sel baris grid dengan data entitas {@code JenisPenerimaanBarang},
		 * termasuk checkbox aktif yang dapat diinteraksikan langsung dan tombol aksi.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * Membuat Label untuk kode, nama, keterangan, akun hutang sementara (format kode-nama),
		 * akun hutang penyedia (format kode-nama), dan satuan kerja (format kode-nama).
		 * Checkbox aktif memiliki event listener onCheck yang langsung menyimpan perubahan
		 * status ke database tanpa membuka form. Tombol ubah/hapus menggunakan helper
		 * {@code Common.copyEditDeleteButtons}.<br><br>
		 *
		 * <b>Penanganan error:</b><br>
		 * Tidak ada penanganan error lokal. Kegagalan penyimpanan status aktif diteruskan ke ZK.
		 *
		 * @param arg0 Baris grid ZK yang akan diisi.
		 * @param arg1 Objek {@code JenisPenerimaanBarang} yang dirender pada baris ini.
		 * @throws Exception Jika terjadi kesalahan pembuatan komponen atau penyimpanan.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPenerimaanBarang jenisPenerimaanBarang = (JenisPenerimaanBarang) arg1;

			RevisiHelper.createNewRevisi(JenisPenerimaanBarang.class, jenisPenerimaanBarang,
					jenisPenerimaanBarang.getKode() == null ? "" : jenisPenerimaanBarang.getKode().trim().toString())
					.setParent(arg0);
			new Label(jenisPenerimaanBarang.getNama()).setParent(arg0);
			new Label(jenisPenerimaanBarang.getKeterangan()).setParent(arg0);

			new Label(jenisPenerimaanBarang.getAkun() == null ? ""
					: jenisPenerimaanBarang.getAkun().getKode() + "-" + jenisPenerimaanBarang.getAkun().getNama())
					.setParent(arg0);

			new Label(jenisPenerimaanBarang.getAkunHutangPenyedia() == null ? ""
					: jenisPenerimaanBarang.getAkunHutangPenyedia().getKode() + "-"
							+ jenisPenerimaanBarang.getAkunHutangPenyedia().getNama())
					.setParent(arg0);

			new Label(jenisPenerimaanBarang.getSatuanKerja() == null ? ""
					: jenisPenerimaanBarang.getSatuanKerja().getKode() + "-"
							+ jenisPenerimaanBarang.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(jenisPenerimaanBarang.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPenerimaanBarang.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(jenisPenerimaanBarang);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPenerimaanBarang, JenisPenerimaanBarangAction.this)
					.setParent(arg0);
		}

	}

	/**
	 * Implementasi antarmuka {@code DataInitDefault} untuk inisialisasi form dari objek generik.
	 *
	 * <b>Tujuan:</b><br>
	 * Dipanggil oleh mekanisme generik ubah data (tombol ubah di renderer) dengan objek
	 * {@code GeneralValueObject} yang merupakan entitas {@code JenisPenerimaanBarang}.
	 * Melakukan cast ke tipe yang benar dan mendelegasikan ke {@code init(JenisPenerimaanBarang)}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Meng-cast {@code obj} ke {@code JenisPenerimaanBarang}, memanggil {@code init} dengan
	 * entitas tersebut, lalu menampilkan {@code addWindow} dalam mode modal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kontrak dari antarmuka {@code DataInitDefault}. Dipanggil oleh {@code Common.copyEditDeleteButtons}.
	 * Jangan mengubah tanda tangan metode.
	 *
	 * @param obj Entitas {@code JenisPenerimaanBarang} yang akan diedit (sebagai {@code GeneralValueObject}).
	 * @throws Exception Jika cast gagal atau inisialisasi form mengalami kesalahan.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPenerimaanBarang = (JenisPenerimaanBarang) obj;
		init(jenisPenerimaanBarang);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menangani klik tombol "Tambah" untuk membuat jenis penerimaan barang baru.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah jenis penerimaan barang baru dengan instance kosong {@code JenisPenerimaanBarang},
	 * lalu menampilkan jendela modal form.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code init(new JenisPenerimaanBarang())} untuk menginisialisasi form kosong,
	 * kemudian menampilkan {@code addWindow} dalam mode modal. Exception dari {@code onModal()}
	 * ditangkap dan dilaporkan ke admin via {@code Common.tampilErrorJikaAdmin}.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari {@code addWindow.onModal()} (misalnya jendela sudah terbuka atau
	 * masalah ZK lain) ditangkap dan dilaporkan, bukan diteruskan ke pemanggil.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika validasi pre-tambah diperlukan (misalnya batas maksimum jenis), tambahkan di sini.
	 *
	 * @param event Event ZK dari klik tombol tambah.
	 * @throws Exception Jika terjadi kesalahan inisialisasi form.
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPenerimaanBarang());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembukaan form Tambah Jenis Penerimaan Barang", e,
					new String[] { "Tutup jendela yang mungkin masih terbuka lalu klik ulang tombol \"Tambah\".",
							"Muat ulang (refresh) halaman ini apabila jendela tetap tidak muncul." });
		}
	}

	/**
	 * Menginisialisasi dan membangun form tambah/ubah jenis penerimaan barang.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan dan membangun ulang form input jenis penerimaan barang secara programatik.
	 * Form ini mencakup field kode, nama, akun hutang sementara, akun hutang penyedia,
	 * satuan kerja, dan keterangan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Mengatur judul jendela dan membersihkan konten lama.
	 * (2) Membangun Borderlayout dengan Center (form grid) dan South (toolbar).
	 * (3) Menampilkan field kode (Textbox), nama (Textbox).
	 * (4) Banbox akun hutang sementara dengan keterangan konteks "Akun saat barang diterima".
	 * (5) Banbox akun hutang penyedia dengan keterangan konteks "Akun saat tagihan diterima".
	 * (6) Banbox satuan kerja untuk membatasi penggunaan jenis penerimaan ke unit kerja tertentu.
	 * (7) Textbox keterangan multi-baris.
	 * (8) Toolbar dengan tombol Batal dan Simpan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Hierarki satuan kerja digunakan untuk membatasi pilihan ke unit kerja yang relevan.
	 * Jika field baru ditambahkan (misalnya jenis transaksi akuntansi), perbarui form ini
	 * dan metode {@code onSave}.
	 *
	 * @param jenisPenerimaanBarang Entitas yang akan ditampilkan/diedit di form.
	 * @throws Exception Jika terjadi kesalahan inisialisasi komponen ZK atau query Hibernate.
	 */
	private void init(JenisPenerimaanBarang jenisPenerimaanBarang) throws Exception {
		addWindow.setTitle(jenisPenerimaanBarang.getId() == null ? "Tambah Jenis Akun Penerimaan Barang" : "Ubah Jenis Akun Penerimaan Barang");
		this.jenisPenerimaanBarang = jenisPenerimaanBarang;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(
				(jenisPenerimaanBarang.getKode() == null ? "" : jenisPenerimaanBarang.getKode().trim())));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(
				jenisPenerimaanBarang.getNama() == null ? "" : jenisPenerimaanBarang.getNama().trim()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Hutang Sementara *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisPenerimaanBarang.getAkun() == null ? "" : jenisPenerimaanBarang.getAkun().getNama());
		akun.setAttribute("akun", jenisPenerimaanBarang.getAkun());
		akun.setWidth("90%");

		Common.initKeterangan(rows, "Akun saat barang diterima");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Hutang Penyedia *"));
		row.appendChild(akunHutangPenyedia = new AmbilDataAkunBanbox(false));
		akunHutangPenyedia.setValue(jenisPenerimaanBarang.getAkunHutangPenyedia() == null ? ""
				: jenisPenerimaanBarang.getAkunHutangPenyedia().toString());
		akunHutangPenyedia.setAttribute("akun", jenisPenerimaanBarang.getAkunHutangPenyedia());
		akunHutangPenyedia.setWidth("90%");

		Common.initKeterangan(rows, "Akun saat tagihan diterima");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				jenisPenerimaanBarang.getSatuanKerja() == null ? "" : jenisPenerimaanBarang.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", jenisPenerimaanBarang.getSatuanKerja());

		row.appendChild(satuanKerja);

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jenisPenerimaanBarang.getKeterangan() == null ? "" : jenisPenerimaanBarang.getKeterangan()));
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

	}

	/**
	 * Menyimpan data jenis penerimaan barang ke database setelah validasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Memvalidasi field wajib form (nama, akun hutang sementara, akun hutang penyedia),
	 * kemudian menyimpan atau memperbarui entitas {@code JenisPenerimaanBarang} ke database,
	 * dan me-reload cache data default setelah berhasil.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memvalidasi nama tidak kosong; jika kosong, menampilkan peringatan dan return false.
	 * (2) Memvalidasi akun hutang sementara sudah dipilih.
	 * (3) Memvalidasi akun hutang penyedia sudah dipilih.
	 * (4) Mengambil sesi Hibernate aktif.
	 * (5) Jika mode ubah, me-load ulang entitas dari database.
	 * (6) Mengisi semua field entitas dari nilai komponen form.
	 * (7) Memanggil {@code Common.refreshSaveOrUpdate} untuk commit.
	 * (8) Melakukan flush sesi dan me-reload cache default via {@code JenisPenerimaanBarang.reloadDefault()}.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika berhasil; {@code false} jika validasi gagal.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Validasi field wajib ditangani secara manual. Exception Hibernate diteruskan ke ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * {@code JenisPenerimaanBarang.reloadDefault()} penting untuk memperbarui cache
	 * yang digunakan modul penerimaan barang. Jangan menghapus pemanggilan ini.
	 *
	 * @param event Event ZK dari klik tombol Simpan.
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan Hibernate saat menyimpan.
	 */
	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Penerimaan Barang belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis penerimaan yang sesuai; (2) Nama ini digunakan untuk mengidentifikasi jenis penerimaan dalam transaksi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Penerimaan Barang",
					"Akun Hutang Sementara belum Bapak/Ibu pilih, padahal akun ini wajib diisi karena "
							+ "digunakan sistem untuk mencatat kewajiban sementara pada saat barang pertama "
							+ "kali diterima secara fisik.",
					new String[] {
							"Klik kotak isian \"Akun Hutang Sementara\" lalu pilih akun akuntansi yang sesuai.",
							"Pastikan akun yang dipilih memang diperuntukkan sebagai akun hutang sementara.",
							"Ulangi proses simpan setelah akun terisi." });
			return false;
		}

		if (akunHutangPenyedia.getAttribute("akun") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Penerimaan Barang",
					"Akun Hutang Penyedia belum Bapak/Ibu pilih, padahal akun ini wajib diisi karena "
							+ "digunakan sistem untuk mencatat kewajiban kepada vendor pada saat tagihan "
							+ "dari vendor diterima.",
					new String[] {
							"Klik kotak isian \"Akun Hutang Penyedia\" lalu pilih akun akuntansi yang sesuai.",
							"Pastikan akun yang dipilih memang diperuntukkan sebagai akun hutang penyedia.",
							"Ulangi proses simpan setelah akun terisi." });
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPenerimaanBarang.getId() != null) {
			jenisPenerimaanBarang = (JenisPenerimaanBarang) session.load(JenisPenerimaanBarang.class,
					jenisPenerimaanBarang.getId());
		}
		jenisPenerimaanBarang.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		jenisPenerimaanBarang.setAkun((Akun) akun.getAttribute("akun"));
		jenisPenerimaanBarang.setAkunHutangPenyedia((Akun) akunHutangPenyedia.getAttribute("akun"));
		jenisPenerimaanBarang.setKode(kode.getValue());
		jenisPenerimaanBarang.setNama(nama.getValue());
		jenisPenerimaanBarang.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPenerimaanBarang);
		session.flush();
		JenisPenerimaanBarang.reloadDefault();
		return true;
	}

	/**
	 * Membangun Criteria Hibernate untuk kueri daftar jenis penerimaan barang.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyusun filter Hibernate yang menggabungkan filter satuan kerja, status aktif,
	 * kode, dan nama jenis penerimaan barang. Digunakan oleh {@code onSearchDefault}
	 * dan {@code Common.initPaging}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Filter satuan kerja menggunakan logika OR: data tanpa satuan kerja (null) selalu
	 * ditampilkan, plus data yang satuan kerjanya termasuk dalam set satuan kerja yang
	 * diizinkan atau dipilih. Filter status aktif hanya menampilkan yang aktif atau
	 * null-aktif (dianggap aktif) jika checkbox aktif dicentang. Filter kode dan nama
	 * menggunakan ILIKE partial match. Urutan default adalah ascending berdasarkan kode.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter baru cukup ditambahkan sebagai klausa {@code .add(...)} tambahan pada criteria.
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY kode ASC.
	 * @return Objek {@code Criteria} Hibernate siap dieksekusi.
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPenerimaanBarang.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("kode"));
		return criteria;
	}

	/**
	 * Memuat ulang data grid jenis penerimaan barang berdasarkan filter saat ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Me-refresh tampilan grid daftar jenis penerimaan barang setelah perubahan filter,
	 * penambahan, perubahan, atau penghapusan data. Dipanggil dari berbagai titik
	 * termasuk inisialisasi halaman, paginasi, dan setelah operasi CRUD.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data dan memperbarui paginasi.
	 * (2) Mengeksekusi kueri dengan criteria terurut, dibatasi per halaman.
	 * (3) Menetapkan renderer dan model ke grid ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan selalu dipanggil setelah setiap operasi yang mengubah data di database.
	 *
	 * @param event Event ZK yang memicu pencarian ulang, bisa {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPenerimaanBarang> jenisPenerimaanBarang = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jenisPenerimaanBarang);
		grid.setRowRenderer(new JenisPenerimaanBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
