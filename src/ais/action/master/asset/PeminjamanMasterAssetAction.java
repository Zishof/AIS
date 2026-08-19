package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
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
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.PeminjamanMasterAssetDetailAction;
import ais.action.master.asset.helper.PeminjamanMasterAssetHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PeminjamanMasterAsset;
import ais.database.model.asset.PeminjamanMasterAssetDetail;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PeminjamanMasterAssetAction — Pengelola Peminjaman Aset Tetap</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan controller ZKoss (turunan {@code GenericAutowireComposer}) yang
 * bertanggung jawab mengelola seluruh siklus hidup dokumen <em>Peminjaman Barang/Aset</em>
 * dalam modul Manajemen Aset. Fungsi utamanya mencakup: pencatatan pengajuan peminjaman
 * barang inventaris oleh unit/individu, pengelolaan daftar barang yang dipinjam beserta
 * detail harga beli, persetujuan dan pembatalan persetujuan oleh pejabat berwenang,
 * pencetakan dokumen resmi peminjaman dalam format PDF, serta penghapusan data peminjaman
 * yang belum disetujui. Kelas ini juga mengimplementasikan antarmuka {@code FormSop} sehingga
 * dapat diintegrasikan ke dalam alur SOP (Standar Operasional Prosedur) yang membutuhkan
 * formulir persetujuan bertahap.
 *
 * <b>Cara kerja:</b><br>
 * Pada saat halaman ZUL terkait di-load oleh ZKoss, metode {@code doBeforeCompose} melakukan
 * pemeriksaan keamanan awal, kemudian {@code doAfterCompose} menginisialisasi seluruh komponen
 * UI termasuk filter pencarian, hak akses pengguna (CREATE/UPDATE/DELETE/APPROVE/REJECT),
 * paginasi, dan timer penyegaran data otomatis. Data ditampilkan dalam komponen {@code MyGrid}
 * menggunakan inner-class {@code PeminjamanMasterAssetRenderer} yang merender setiap baris
 * dengan detail lengkap: kode, nama peminjam, keperluan, lokasi, tanggal pinjam-kembali,
 * riwayat pembuat, informasi persetujuan, status aktif, tautan SOP, serta tombol aksi
 * (cetak, setujui, batalkan, ubah, hapus) yang visibilitasnya dikendalikan oleh hak akses
 * pengguna yang sedang login. Formulir isian dibangun secara programatis oleh metode
 * {@code form()} dalam {@code MyGrid} dua kolom. Kode peminjaman dibangkitkan secara otomatis
 * menggunakan pola {@code NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA} dengan dukungan
 * reset urutan per tahun/bulan dan jaminan keunikan via {@code KodeUnikUtil.pastikanUnik()}.
 *
 * <b>Threading:</b><br>
 * Semua operasi database menggunakan {@code HibernateUtil.currentSession()} yang terikat
 * pada thread request ZKoss (session-per-request). Operasi yang membutuhkan flush dan commit
 * terpisah (seperti persetujuan dan auto-print setelah simpan) dijalankan menggunakan
 * {@code Common.createDefaultTimer()} yang menjadwalkan eksekusi pada event berikutnya di
 * thread UI ZKoss — bukan thread latar belakang. Dengan demikian tidak ada konkurensi eksplisit
 * di kelas ini; keamanan thread bergantung pada manajemen sesi ZKoss dan Hibernate.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan {@code FormSop} sehingga wajib menyediakan implementasi
 * {@code form()}, {@code cetakData()}, {@code istilah()}, {@code ambil()},
 * {@code ambilClass()}, dan {@code setPersetujuan()}. Jika skema tabel
 * {@code peminjaman_master_asset} atau {@code peminjaman_master_asset_detail} berubah,
 * pastikan entitas Hibernate, renderer, metode {@code parameter()}, dan template laporan
 * JasperReports di {@code asset/peminjaman} diperbarui secara bersamaan. Untuk menambah
 * kolom pencarian baru, tambahkan komponen ZUL dan tambahkan {@code Restrictions} yang
 * sesuai di {@code initCriteria()}. Penomoran otomatis dikendalikan oleh konfigurasi
 * {@code NomorSuratAlurPengadaan} — ubah konfigurasi tersebut untuk mengubah format nomor
 * dokumen tanpa mengubah kode ini.
 *
 * @author AIS Development Team
 * @version 1.0
 * @see PeminjamanMasterAsset
 * @see PeminjamanMasterAssetDetail
 * @see FormSop
 */
public class PeminjamanMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Nomor versi serial untuk serialisasi objek kelas ini.
	 * Nilai ini diperlukan oleh antarmuka {@code Serializable} yang diwarisi dari
	 * {@code GenericAutowireComposer} dan tidak boleh diubah kecuali ada perubahan
	 * struktur kelas yang tidak kompatibel dengan versi sebelumnya.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal utama yang digunakan untuk form tambah dan ubah data peminjaman. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar seluruh peminjaman aset dengan paginasi. */
	private MyGrid grid;

	/** Komponen paginasi yang terhubung dengan grid utama untuk navigasi halaman data. */
	private Paging paging;

	/** Kolom pencarian berdasarkan kode peminjaman di toolbar filter. */
	private Textbox searchkode;

	/** Kolom pencarian berdasarkan keterangan peminjaman di toolbar filter. */
	private Textbox searchketerangan;

	/** Kolom pencarian berdasarkan nama peminjam di toolbar filter. */
	private Textbox searchNamaPeminjam;

	/** Komponen banbox untuk filter berdasarkan satuan kerja pada toolbar pencarian. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Label yang menampilkan kode peminjaman yang dibangkitkan otomatis pada form. */
	private Label kode;

	/** Kolom teks keterangan tambahan untuk peminjaman pada form isian. */
	private MyTextbox keterangan;

	/** Komponen datebox untuk mengisi tanggal pembuatan dokumen peminjaman. */
	private MyDatebox tanggalPembuatan;

	/** Bendera yang menandai apakah pengguna saat ini memiliki hak akses UPDATE. */
	private boolean edit = false;

	/** Bendera yang menandai apakah pengguna saat ini memiliki hak akses DELETE. */
	private boolean delete = false;

	/** Bendera yang menandai apakah pengguna saat ini memiliki hak akses APPROVE. */
	private boolean approve = false;

	/** Bendera yang menandai apakah pengguna saat ini memiliki hak akses REJECT. */
	private boolean reject = false;

	/** Entitas peminjaman yang sedang aktif diedit atau diproses pada form. */
	private PeminjamanMasterAsset peminjamanMasterAsset;

	/** Tombol tambah data baru yang visibilitasnya dikendalikan oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Komponen datebox untuk mengisi tanggal rencana pengembalian barang. */
	private MyDatebox tanggalKembali;

	/** Kolom teks keperluan/tujuan peminjaman barang pada form isian. */
	private Textbox keperluan;

	/** Kolom teks lokasi kegiatan yang membutuhkan barang pinjaman. */
	private Textbox lokasiKegiatan;

	/** Grid detail yang menampilkan daftar barang yang akan dipinjam dalam satu dokumen. */
	private MyGrid gridMasterAsset;

	/**
	 * Bendera mode persetujuan. Jika {@code true}, form ditampilkan dalam mode
	 * read-only dengan tambahan opsi persetujuan; jika {@code false}, form dapat
	 * diedit penuh untuk keperluan input data baru.
	 */
	private boolean persetujuan = false;

	/** Objek pengguna yang sedang login, digunakan untuk audit dan persetujuan. */
	private Tbmuser tbmuser;

	/** Checkbox yang memungkinkan pengguna langsung menyetujui peminjaman saat menyimpan. */
	private MyCheckboxConfig setujui;

	/** Kolom teks nama individu atau unit yang meminjam barang. */
	private Textbox namaPeminjam;

	/** Disposisi SOP yang terkait dengan dokumen peminjaman ini jika alur SOP aktif. */
	private DisposisiSop disposisiSop = null;

	/** Banbox pemilih satuan kerja pada form isian peminjaman. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Model pohon satuan kerja untuk mendukung pencarian hierarkis pada filter. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Komponen datebox untuk mengisi tanggal mulai peminjaman barang. */
	private MyDatebox tanggalPinjam;

	/** Checkbox filter untuk menampilkan peminjaman yang belum disetujui. */
	private MyCheckboxConfig blmDisetujui;

	/** Checkbox filter untuk menampilkan peminjaman yang sudah disetujui. */
	private MyCheckboxConfig disetujui;

	/**
	 * <b>Tujuan:</b> Konstruktor default yang menginisialisasi controller dalam mode
	 * penuh (bukan persetujuan) dan mengambil data pengguna yang sedang login.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.getCurrentUser()} untuk mendapatkan
	 * objek {@code Tbmuser} dari sesi HTTP yang aktif. Konstruktor ini digunakan
	 * ketika halaman ZUL utama peminjaman dimuat secara langsung oleh ZKoss.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (konstruktor).
	 *
	 * <b>Penanganan error:</b> Jika sesi pengguna tidak valid, {@code Common.getCurrentUser()}
	 * dapat mengembalikan null — kondisi ini akan menyebabkan NPE pada operasi berikutnya.
	 * Validasi sesi dilakukan secara eksplisit di {@code doAfterCompose()}.
	 *
	 * <b>Pemeliharaan:</b> Jika logika pengambilan pengguna saat login berubah, perbarui
	 * juga konstruktor berparameter di bawah ini agar konsisten.
	 */
	public PeminjamanMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Konstruktor berparameter yang menginisialisasi controller dalam
	 * mode persetujuan (SOP) dan mengambil data pengguna yang sedang login.
	 *
	 * <b>Cara kerja:</b> Menetapkan flag {@code persetujuan} sesuai argumen masukan,
	 * lalu memanggil {@code Common.getCurrentUser()} untuk mendapatkan objek pengguna.
	 * Konstruktor ini dipanggil oleh sistem SOP ketika modul peminjaman diintegrasikan
	 * sebagai bagian dari alur persetujuan bertahap, sehingga form perlu ditampilkan
	 * dalam mode read-only dengan pilihan setujui/tolak.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} jika controller diinisialisasi dalam konteks
	 *                    persetujuan SOP (form read-only + checkbox persetujuan);
	 *                    {@code false} untuk mode pengeditan penuh.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (konstruktor).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus. Validasi sesi
	 * dilakukan di {@code doAfterCompose()}.
	 *
	 * <b>Pemeliharaan:</b> Jika ada atribut tambahan yang perlu diinisialisasi secara
	 * berbeda berdasarkan mode persetujuan, tambahkan di konstruktor ini.
	 */
	public PeminjamanMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Metode siklus hidup ZKoss yang dipanggil sebelum komponen ZUL
	 * disusun, digunakan untuk melakukan pemeriksaan keamanan akses halaman.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang memeriksa
	 * apakah pengguna yang sedang login memiliki hak akses ke halaman ini berdasarkan
	 * konfigurasi menu dan role yang dimiliki. Jika akses ditolak, ZKoss akan
	 * mengarahkan pengguna ke halaman error atau login. Setelah pemeriksaan,
	 * memanggil {@code super.doBeforeCompose()} untuk melanjutkan proses normal ZKoss.
	 *
	 * <b>Parameter:</b>
	 * @param page     Objek {@code Page} ZKoss yang sedang disusun.
	 * @param parent   Komponen induk tempat halaman ini dilampirkan.
	 * @param compInfo Informasi metadata komponen dari file ZUL.
	 * @return {@code ComponentInfo} dari superclass untuk melanjutkan proses komposisi.
	 *
	 * <b>Penanganan error:</b> Jika keamanan gagal, {@code Common.doCheckSecurity()}
	 * akan melempar exception atau melakukan redirect, mencegah halaman ditampilkan.
	 *
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()}.
	 * Jika ada logika keamanan tambahan (misalnya cek IP atau waktu akses), tambahkan
	 * sebelum pemanggilan {@code super}.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama ZKoss yang dipanggil setelah seluruh
	 * komponen ZUL selesai disusun dan siap digunakan.
	 *
	 * <b>Cara kerja:</b> Pertama memanggil {@code super.doAfterCompose(comp)} agar
	 * ZKoss melakukan autowiring komponen. Kemudian memeriksa apakah atribut sesi
	 * {@code usersTemp} ada dan pengguna memiliki hak READ — jika tidak, sesi
	 * dihapus dan pengguna diarahkan ke halaman logoff. Setelah validasi, melakukan
	 * serangkaian inisialisasi: (1) mendaftarkan event listener pada banbox filter
	 * satuan kerja untuk memicu pencarian ulang saat satuan kerja berubah;
	 * (2) menginisialisasi model pohon satuan kerja untuk filter hierarkis;
	 * (3) mengatur visibilitas tombol tambah berdasarkan hak CREATE;
	 * (4) membaca dan menyimpan flag hak akses edit/delete/approve/reject;
	 * (5) menginisialisasi paginasi dengan event listener; dan
	 * (6) membuat timer default yang menyegarkan grid data secara otomatis
	 * setelah halaman pertama kali dimuat.
	 *
	 * <b>Parameter:</b>
	 * @param comp Komponen root ZUL yang telah selesai disusun oleh ZKoss.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi komponen atau
	 *                   pembacaan konfigurasi dari database.
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke logoff
	 * secara otomatis dan metode langsung return tanpa inisialisasi lanjut.
	 *
	 * <b>Pemeliharaan:</b> Jika ada komponen filter baru ditambahkan ke ZUL, daftarkan
	 * juga event listener-nya di sini. Urutan inisialisasi penting: validasi sesi
	 * harus dilakukan sebelum akses komponen UI apapun.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
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
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/**
	 * <b>Untuk apa:</b> Kelas dalam (inner class) yang bertugas merender setiap baris
	 * data peminjaman aset ke dalam baris grid ZKoss.
	 *
	 * <b>Cara kerja:</b> Meng-extend {@code MyRowRenderer} dan mengoverride metode
	 * {@code render()} yang dipanggil oleh ZKoss untuk setiap entri {@code PeminjamanMasterAsset}
	 * dalam model data. Untuk setiap baris, renderer membuat dan melampirkan komponen-komponen
	 * berikut ke objek {@code Row}: (1) panel detail barang via {@code PeminjamanMasterAssetDetailAction};
	 * (2) tombol revisi via {@code RevisiHelper}; (3) label nama peminjam; (4) label keperluan;
	 * (5) label lokasi kegiatan; (6) label rentang tanggal pinjam-kembali; (7) Vbox informasi
	 * pembuat (nama + tanggal); (8) Vbox informasi persetujuan dengan referensi final untuk
	 * pembaruan dinamis; (9) Vbox keterangan dan tautan SOP jika ada; (10) kontrol status aktif
	 * (checkbox jika belum disetujui dan user berhak edit, label jika sudah); dan (11) toolbar
	 * dengan tombol cetak, setujui, batalkan persetujuan, ubah, dan hapus.
	 *
	 * <b>Threading:</b> Dipanggil pada thread UI ZKoss. Semua operasi database
	 * dalam event listener menggunakan {@code HibernateUtil.currentSession()}.
	 *
	 * <b>Pemeliharaan:</b> Jika kolom baru ditambahkan ke grid di ZUL, tambahkan juga
	 * komponen yang sesuai di metode {@code render()} ini dengan urutan yang sama.
	 * Jangan ubah urutan penambahan komponen ke {@code Row} karena harus sesuai
	 * dengan definisi kolom di file ZUL.
	 */
	class PeminjamanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data peminjaman aset ke dalam komponen
		 * ZKoss {@code Row} yang akan ditampilkan di grid.
		 *
		 * <b>Cara kerja:</b> Menerima objek {@code PeminjamanMasterAsset} dari parameter
		 * {@code arg1}, lalu membuat dan melampirkan semua komponen UI yang diperlukan
		 * ke baris {@code arg0}. Panel detail barang dibuat menggunakan
		 * {@code PeminjamanMasterAssetDetailAction} dalam mode non-persetujuan.
		 * Tombol setujui hanya ditampilkan jika pengguna punya hak APPROVE dan peminjaman
		 * belum disetujui; tombol batalkan hanya jika punya hak REJECT dan sudah disetujui.
		 * Tombol ubah dan hapus hanya tampil jika peminjaman belum disetujui. Event listener
		 * persetujuan dan pembatalan menggunakan {@code MyMessageboxConfig} untuk konfirmasi
		 * pengguna sebelum melakukan perubahan ke database, lalu memperbarui label dinamis
		 * dan memuat ulang detail barang.
		 *
		 * <b>Parameter:</b>
		 * @param arg0 Baris ZKoss {@code Row} tempat komponen UI akan dilampirkan.
		 * @param arg1 Objek data — harus bertipe {@code PeminjamanMasterAsset}.
		 * @throws Exception Jika terjadi kesalahan saat membuat komponen UI atau
		 *                   mengakses database dalam event listener.
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Exception dari event listener ditangani oleh ZKoss.
		 * Error hapus data yang melanggar foreign key ditangkap dan ditampilkan via
		 * {@code MyMessageboxConfig.show()}.
		 *
		 * <b>Pemeliharaan:</b> Jika ada kolom baru di grid, tambahkan komponen sebelum
		 * {@code toolbar.setParent(arg0)} di akhir metode. Urutan mutlak harus konsisten
		 * dengan urutan {@code <column>} di file ZUL.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PeminjamanMasterAsset peminjamanMasterAsset = (PeminjamanMasterAsset) arg1;

			final PeminjamanMasterAssetDetailAction detail;
			(detail = new PeminjamanMasterAssetDetailAction(peminjamanMasterAsset, false)).setParent(arg0);

			RevisiHelper.createNewRevisi(PeminjamanMasterAsset.class, peminjamanMasterAsset,
					peminjamanMasterAsset.getKode()).setParent(arg0);

			new Label(peminjamanMasterAsset.getNamaPeminjam()).setParent(arg0);

			new Label(peminjamanMasterAsset.getKeperluan()).setParent(arg0);

			new Label(peminjamanMasterAsset.getLokasiKegiatan()).setParent(arg0);

			new Label(Common.dateFormat1.get().format(peminjamanMasterAsset.getTanggalPinjam()) + " sd "
					+ Common.dateFormat1.get().format(peminjamanMasterAsset.getTanggalKembali())).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(peminjamanMasterAsset.getDibuatOleh() == null ? ""
					: peminjamanMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(peminjamanMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(peminjamanMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(peminjamanMasterAsset.getDisetujuiOleh() == null ? ""
					: peminjamanMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(peminjamanMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(peminjamanMasterAsset.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(peminjamanMasterAsset.getKeterangan())).setParent(vbox1);
			if (peminjamanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + peminjamanMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ peminjamanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(peminjamanMasterAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (peminjamanMasterAsset.getDisposisiSop() != null
					&& !peminjamanMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (peminjamanMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(peminjamanMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						peminjamanMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(peminjamanMasterAsset);
					}
				});
			} else {
				new Label(peminjamanMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Peminjaman Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(peminjamanMasterAsset);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && peminjamanMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && peminjamanMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Peminjaman Barang/Jasa ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();

										peminjamanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										peminjamanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, peminjamanMasterAsset);

										disetujuiTanggal
												.setValue(peminjamanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(peminjamanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(peminjamanMasterAsset.getDisetujuiOleh() == null ? ""
												: peminjamanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && peminjamanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && peminjamanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && peminjamanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && peminjamanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(peminjamanMasterAsset);

									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Peminjaman Barang/Jasa ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										peminjamanMasterAsset.setDisetujuiOleh(null);
										peminjamanMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, peminjamanMasterAsset);

										disetujuiTanggal
												.setValue(peminjamanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(peminjamanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(peminjamanMasterAsset.getDisetujuiOleh() == null ? ""
												: peminjamanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && peminjamanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && peminjamanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && peminjamanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && peminjamanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && peminjamanMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(peminjamanMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && peminjamanMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session,
													peminjamanMasterAsset.getDisposisiSop())) {
												List<PeminjamanMasterAssetDetail> peminjamanMasterAssetDetails = session
														.createCriteria(PeminjamanMasterAssetDetail.class)
														.add(Restrictions.eq("peminjamanMasterAsset",
																peminjamanMasterAsset))
														.list();
												for (PeminjamanMasterAssetDetail peminjamanMasterAssetDetail : peminjamanMasterAssetDetails) {
													session.delete(peminjamanMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, peminjamanMasterAsset);
														session.flush();

														onSearchDefault(event);
													}
												});

											}
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
			aksiButtons.add(hapus);

			// Susun semua tombol: max 3 per baris, rata tengah
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" untuk membuka form
	 * pembuatan data peminjaman baru.
	 *
	 * <b>Cara kerja:</b> Membuat objek {@code PeminjamanMasterAsset} baru (kosong),
	 * lalu memanggil {@code init()} untuk membangun form isian dan menampilkan jendela
	 * modal {@code addWindow}. Metode ini terikat ke event {@code onAdd} pada tombol
	 * tambah di ZUL melalui mekanisme event ZKoss.
	 *
	 * <b>Parameter:</b>
	 * @param event Objek event ZKoss yang memicu aksi ini (biasanya onClick dari tombol).
	 * @throws Exception Jika terjadi kesalahan saat menginisialisasi form atau membuka modal.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke ZKoss untuk ditangani di level framework.
	 *
	 * <b>Pemeliharaan:</b> Jika ada logika pre-inisialisasi data baru (misalnya mengisi
	 * satuan kerja default), tambahkan di sini sebelum pemanggilan {@code init()}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PeminjamanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form modal untuk tambah atau ubah
	 * data peminjaman aset, termasuk seluruh komponen UI dan event listener-nya.
	 *
	 * <b>Cara kerja:</b> Menyimpan referensi entitas ke field instance, mengatur judul
	 * jendela berdasarkan mode (tambah/ubah), membersihkan konten {@code addWindow}
	 * menggunakan {@code Common.clear()}, lalu membangun layout {@code Borderlayout}
	 * dengan area {@code Center} berisi form isian (dihasilkan oleh {@code form()})
	 * dan area {@code South} berisi toolbar dengan tombol "Batal" dan "Simpan dan Cetak".
	 * Tombol "Simpan" memanggil {@code onSave()} dan jika berhasil, menyegarkan grid,
	 * mereset paginasi, dan menutup modal. Tombol "Batal" menutup modal tanpa menyimpan.
	 *
	 * <b>Parameter:</b>
	 * @param peminjamanMasterAsset Entitas peminjaman yang akan diedit (ID null untuk
	 *                              tambah baru, ID terisi untuk ubah data yang ada).
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen UI atau
	 *                   mengambil data terkait dari database.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke pemanggil. Pastikan
	 * {@code addWindow} tidak null sebelum memanggil metode ini.
	 *
	 * <b>Pemeliharaan:</b> Jika ada tombol aksi baru pada modal, tambahkan di area
	 * toolbar South. Perubahan layout form dilakukan di metode {@code form()}.
	 */
	private void init(PeminjamanMasterAsset peminjamanMasterAsset) throws Exception {
		this.peminjamanMasterAsset = peminjamanMasterAsset;
		addWindow.setTitle(peminjamanMasterAsset.getId() == null ? "Tambah Peminjaman Barang" : "Ubah Peminjaman Barang");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(peminjamanMasterAsset, disposisiSop, save, null));

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

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input form dan menyimpan data peminjaman aset beserta
	 * seluruh detail barang yang dipinjam ke dalam database.
	 *
	 * <b>Cara kerja:</b> Melakukan validasi berurutan terhadap field wajib: kode tidak
	 * kosong, nama peminjam tidak kosong, keperluan tidak kosong, lokasi kegiatan tidak
	 * kosong, dan setiap baris detail barang harus memiliki aset yang dipilih. Jika ada
	 * validasi yang gagal, ditampilkan pesan peringatan dan metode return false.
	 * Setelah validasi berhasil, membuka sesi Hibernate current dan melakukan:
	 * (1) load ulang entitas jika mode ubah (ID tidak null); (2) menyalin nilai dari
	 * komponen UI ke entitas; (3) menghitung total nilai dari semua detail barang;
	 * (4) menyimpan atau memperbarui entitas induk; (5) menyimpan atau memperbarui
	 * semua detail barang. Terakhir, menggunakan timer default untuk menangani
	 * persetujuan otomatis (jika checkbox setujui dicentang) dan mencetak dokumen PDF.
	 *
	 * <b>Parameter:</b>
	 * @param event Objek event ZKoss yang memicu penyimpanan (biasanya dari tombol Simpan).
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan saat operasi database atau pembuatan laporan.
	 *
	 * <b>Penanganan error:</b> Validasi input menggunakan {@code MyMessageboxConfig.show()}
	 * untuk menampilkan pesan kesalahan yang ramah pengguna. Exception database diteruskan
	 * ke pemanggil. Race condition saat nomor kode dibangkitkan ditangani oleh
	 * {@code KodeUnikUtil.pastikanUnik()}.
	 *
	 * <b>Pemeliharaan:</b> Jika ada field wajib baru ditambahkan ke form, tambahkan
	 * validasinya di awal metode ini sebelum operasi database. Jika ada kalkulasi nilai
	 * yang berubah (bukan sekadar jumlah harga beli), perbarui blok kalkulasi {@code jumlah}.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Permintaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Permintaan atau gunakan tombol generate kode otomatis; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (namaPeminjam.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Peminjam belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama Peminjam dengan nama lengkap peminjam; (2) Pastikan identitas peminjam valid dan tercatat; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keperluan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Keperluan peminjaman belum diisi. Langkah yang dapat dilakukan: (1) Isi field Keperluan dengan keterangan tujuan peminjaman; (2) Deskripsi keperluan wajib diisi untuk keperluan pencatatan dan audit; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasiKegiatan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi Kegiatan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Lokasi Kegiatan dengan tempat kegiatan peminjaman berlangsung; (2) Informasi lokasi diperlukan untuk keperluan monitoring barang; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) row
					.getAttribute("peminjamanMasterAssetDetail");
			if (peminjamanMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar peminjaman belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/aset yang akan dipinjam dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (peminjamanMasterAsset.getId() != null) {
			peminjamanMasterAsset = (PeminjamanMasterAsset) session.load(PeminjamanMasterAsset.class,
					peminjamanMasterAsset.getId());
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			peminjamanMasterAsset.setDisposisiSop(disposisiSop);
		}

		peminjamanMasterAsset.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		peminjamanMasterAsset.setNamaPeminjam(namaPeminjam.getValue());

		peminjamanMasterAsset.setKeperluan(keperluan.getValue());
		peminjamanMasterAsset.setLokasiKegiatan(lokasiKegiatan.getValue());
		peminjamanMasterAsset.setTanggalKembali(tanggalKembali.getValue());
		peminjamanMasterAsset.setTanggalPinjam(tanggalPinjam.getValue());

		peminjamanMasterAsset.setKode(kode.getValue());
		peminjamanMasterAsset.setKeterangan(keterangan.getValue());
		peminjamanMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());

		Double jumlah = 0.0;
		for (Row row : rowsMasterAsset) {
			PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) row
					.getAttribute("peminjamanMasterAssetDetail");
			Double n = peminjamanMasterAssetDetail.getHargaBeli();

			jumlah += n;
		}

		peminjamanMasterAsset.setNilai(jumlah);

		if (peminjamanMasterAsset.getId() != null) {
			session.update(peminjamanMasterAsset);
		} else {
			peminjamanMasterAsset.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			peminjamanMasterAsset.setKode(kode.getValue());
			session.save(peminjamanMasterAsset);
		}

		for (Row row : rowsMasterAsset) {
			PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) row
					.getAttribute("peminjamanMasterAssetDetail");
			peminjamanMasterAssetDetail.setPeminjamanMasterAsset(peminjamanMasterAsset);
			session.saveOrUpdate(peminjamanMasterAssetDetail);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					Session session = HibernateUtil.currentSession();
					peminjamanMasterAsset.setDisetujuiOleh(tbmuser);
					peminjamanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(session, peminjamanMasterAsset);
				} else {
					Session session = HibernateUtil.currentSession();
					peminjamanMasterAsset.setDisetujuiOleh(null);
					peminjamanMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(session, peminjamanMasterAsset);
				}

				cetak(peminjamanMasterAsset);
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun peta parameter laporan (report map) yang berisi semua
	 * data peminjaman dan detail barang yang diperlukan oleh template JasperReports
	 * untuk mencetak dokumen peminjaman.
	 *
	 * <b>Cara kerja:</b> Menyegarkan entitas dari database jika ID tidak null untuk
	 * memastikan data terkini. Membuat {@code Map} menggunakan {@code HashMapGenerator.getRand()}
	 * (agar kunci peta bersifat unik per cetak), lalu mengisi dengan:
	 * (1) properti entitas induk via {@code Common.insertProperty()} dengan prefix "data";
	 * (2) parameter disposisi SOP via {@code DisposisiAlurSop.parameterMap()};
	 * (3) daftar {@code maps} berisi satu {@code Map} per detail barang, masing-masing
	 * memuat: kelompok aset, jenis aset, tipe, spesifikasi, harga beli, nama barang,
	 * kode peminjaman, kode aset (isbn), barcode, nama aset detail, dan status persetujuan
	 * yang diformulasikan sebagai teks naratif. Kunci yang mengandung "disposisiSop"
	 * di-null-kan untuk menghindari masalah serialisasi di JasperReports.
	 *
	 * <b>Parameter:</b>
	 * @param peminjamanMasterAsset Entitas peminjaman yang akan dijadikan sumber data laporan.
	 * @return {@code Map} berisi semua parameter yang diperlukan template JasperReports.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Jika entitas detail
	 * tidak ditemukan, daftar {@code maps} akan kosong sehingga laporan tercetak tanpa baris.
	 *
	 * <b>Pemeliharaan:</b> Jika ada field baru di entitas atau template laporan berubah,
	 * perbarui kunci yang dimasukkan ke {@code map} dalam loop detail. Pastikan nama kunci
	 * konsisten dengan nama parameter di file .jrxml template.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map parameter(PeminjamanMasterAsset peminjamanMasterAsset) {
		if (peminjamanMasterAsset != null && peminjamanMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(peminjamanMasterAsset);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", peminjamanMasterAsset.getId());

		Common.insertProperty(PeminjamanMasterAsset.class, peminjamanMasterAsset, parameters, "data");

		DisposisiAlurSop.parameterMap(peminjamanMasterAsset.getDisposisiSop(), parameters);
		Session session = HibernateUtil.currentSession();

		List<PeminjamanMasterAssetDetail> peminjamanMasterAssetDetails = session
				.createCriteria(PeminjamanMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.jenisAsset")).addOrder(Order.asc("masterAsset.kelompokAsset"))
				.addOrder(Order.asc("masterAsset.id"))
				.add(Restrictions.eq("peminjamanMasterAsset", peminjamanMasterAsset)).list();
		List<Map> maps = new ArrayList<Map>();
		for (PeminjamanMasterAssetDetail peminjamanMasterAssetDetail : peminjamanMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PeminjamanMasterAssetDetail.class, peminjamanMasterAssetDetail, map, "data");

			map.put("id_asset", peminjamanMasterAssetDetail.getMasterAsset().getId());

			map.put("kelompok_asset", peminjamanMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
					: peminjamanMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());

			map.put("jenis_asset", peminjamanMasterAssetDetail.getMasterAsset().getJenisAsset() == null ? ""
					: peminjamanMasterAssetDetail.getMasterAsset().getJenisAsset().getNama());

			map.put("tipe_asset", peminjamanMasterAssetDetail.getMasterAsset().getTipe());

			map.put("spesifikasi", peminjamanMasterAssetDetail.getMasterAsset().getSpesifikasi());

			map.put("hargabeli", peminjamanMasterAssetDetail.getHargaBeli());
			map.put("jumlah", 1.0);
			map.put("nama", peminjamanMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode", peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getKode());
			map.put("isbn", peminjamanMasterAssetDetail.getMasterAsset().getKode());
			map.put("barcode", peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
					: peminjamanMasterAssetDetail.getAssetDetail().getBarcode());
			map.put("nama_barang", peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
					: peminjamanMasterAssetDetail.getAssetDetail().getNama());
			String status = "";
			if (peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh().getUserNama()
						+ " pada "
						+ (peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getTanggalPersetujuan() == null ? ""
								: Common.dateFormat51.get().format(peminjamanMasterAssetDetail
										.getPeminjamanMasterAsset().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan", peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getKeterangan());
			map.put("tanggal_persetujuan",
					peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh() == null ? ""
							: peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh().getUserNama());

			maps.add(map);
		}

		parameters.put("maps", maps);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}

		return parameters;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan metode {@code cetakData()} dari antarmuka
	 * {@code FormSop} untuk menghasilkan file PDF dokumen peminjaman yang dapat
	 * diunduh atau dikirim melalui alur SOP.
	 *
	 * <b>Cara kerja:</b> Melakukan cast parameter {@code GeneralValueObject} ke
	 * {@code PeminjamanMasterAsset}, lalu memanggil {@code Report.generateFileReport()}
	 * dengan parameter laporan yang dihasilkan oleh {@code parameter()}, menggunakan
	 * template {@code "asset/peminjaman"}, format PDF, dan locale sistem. Berbeda dengan
	 * {@code cetak()} yang langsung menampilkan ke browser, metode ini mengembalikan
	 * objek {@code File} yang dapat diproses lebih lanjut oleh sistem SOP.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject Objek {@code GeneralValueObject} yang berisi data
	 *                           peminjaman — harus bertipe {@code PeminjamanMasterAsset}.
	 * @return Objek {@code File} yang menunjuk ke file PDF yang berhasil dibuat.
	 * @throws Exception Jika terjadi kesalahan saat menghasilkan laporan (template tidak
	 *                   ditemukan, data tidak lengkap, atau kesalahan I/O).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke sistem SOP yang memanggil metode ini.
	 *
	 * <b>Pemeliharaan:</b> Jika template laporan dipindahkan atau diganti namanya,
	 * perbarui string path {@code "asset/peminjaman"} di sini dan di metode {@code cetak()}.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PeminjamanMasterAsset peminjamanMasterAsset = (PeminjamanMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(peminjamanMasterAsset), "asset/peminjaman",
				peminjamanMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan dan langsung menampilkan laporan PDF dokumen peminjaman
	 * aset ke browser pengguna melalui mekanisme streaming ZKoss.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Report.generatePDFReport()} dengan parameter
	 * laporan dari {@code parameter()}, template {@code "asset/peminjaman"}, dan tanggal
	 * pembuatan sebagai konteks. Laporan akan langsung ditampilkan atau diunduh pengguna
	 * sesuai konfigurasi browser. Metode ini dipanggil setelah simpan berhasil dan setelah
	 * operasi persetujuan/pembatalan.
	 *
	 * <b>Parameter:</b>
	 * @param peminjamanMasterAsset Entitas peminjaman yang akan dicetak.
	 * @throws Exception Jika terjadi kesalahan saat menghasilkan atau mengirimkan laporan PDF.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke pemanggil. Jika laporan gagal
	 * dibuat, pengguna perlu diberitahu secara manual karena tidak ada fallback.
	 *
	 * <b>Pemeliharaan:</b> Jika format laporan perlu diganti (misalnya ke Excel),
	 * ganti konstanta {@code Report.PDF} dan method yang dipanggil secara konsisten
	 * di sini dan di {@code cetakData()}.
	 */
	@SuppressWarnings({})
	private void cetak(PeminjamanMasterAsset peminjamanMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(peminjamanMasterAsset), "asset/peminjaman",
				peminjamanMasterAsset.getTanggalPembuatan());
	}

	/** Checkbox untuk memfilter hanya data yang masih aktif pada grid utama. */
	private Checkbox searchaktif;

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate yang mencerminkan
	 * seluruh kondisi filter pencarian yang aktif saat ini di toolbar filter.
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah {@code searchparent} null (jika null, return null).
	 * Mengambil satuan kerja yang dipilih dari banbox filter dan menyusun himpunan
	 * satuan kerja yang relevan termasuk semua anak-cucunya menggunakan
	 * {@code satuanKerjaTreeModel.getChildsSet()}. Membangun {@code Criteria} untuk
	 * kelas {@code PeminjamanMasterAsset} dengan menambahkan restriction berurutan:
	 * (1) filter aktif — jika checkbox aktif dicentang, tampilkan hanya yang aktif atau null;
	 * (2) filter status persetujuan — kombinasi logika dari checkbox blmDisetujui dan disetujui;
	 * (3) filter satuan kerja — menggunakan {@code Restrictions.in()} untuk himpunan;
	 * (4) filter nama peminjam — menggunakan ILIKE untuk pencarian sebagian teks;
	 * (5) filter kode — menggunakan ILIKE; dan
	 * (6) filter keterangan — menggunakan ILIKE.
	 * Jika parameter {@code order} true, menambahkan urutan menurun berdasarkan ID.
	 *
	 * <b>Parameter:</b>
	 * @param order {@code true} untuk menambahkan klausa ORDER BY id DESC;
	 *              {@code false} untuk criteria tanpa urutan (digunakan untuk hitung total).
	 * @return Objek {@code Criteria} yang sudah dikonfigurasi, atau {@code null} jika
	 *         {@code searchparent} belum diinisialisasi.
	 *
	 * <b>Penanganan error:</b> Jika tidak ada satuan kerja yang terdaftar untuk pengguna
	 * saat ini, restriction {@code 1=1} digunakan (tampilkan semua). Tidak ada exception
	 * yang dilempar secara eksplisit.
	 *
	 * <b>Pemeliharaan:</b> Untuk menambahkan filter baru, tambahkan komponen di ZUL dan
	 * tambahkan blok {@code .add(Restrictions...)} yang sesuai di sini. Perhatikan bahwa
	 * metode ini dipanggil dua kali di {@code onSearchDefault()} — sekali untuk count
	 * paginasi dan sekali untuk data aktual.
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PeminjamanMasterAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchNamaPeminjam.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("namaPeminjam", searchNamaPeminjam.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

		;
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian dan pembaruan tampilan grid data peminjaman
	 * berdasarkan filter yang aktif saat ini, dengan dukungan paginasi.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code initCriteria(false)} untuk mendapatkan criteria
	 * tanpa urutan, lalu meneruskannya ke {@code Common.initPaging()} untuk menghitung
	 * total baris dan memperbarui komponen paginasi. Kemudian memanggil
	 * {@code initCriteria(true)} untuk criteria dengan urutan DESC, menerapkan batas
	 * {@code ROWS_COUNT_ON_PAGE} dan offset berdasarkan halaman aktif paginasi, dan
	 * mengambil daftar data dari database. Hasilnya dibungkus dalam {@code SimpleListModel}
	 * dan disetel ke grid menggunakan {@code setModelCheckMobile()} dengan renderer
	 * {@code PeminjamanMasterAssetRenderer}.
	 *
	 * <b>Parameter:</b>
	 * @param event Objek event ZKoss yang memicu pencarian (bisa null jika dipanggil
	 *              dari inisialisasi atau timer).
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Jika {@code initCriteria()} mengembalikan null (karena
	 * {@code searchparent} null), metode ini akan melempar NullPointerException.
	 * Pastikan komponen selalu diinisialisasi sebelum metode ini dipanggil.
	 *
	 * <b>Pemeliharaan:</b> Jika jumlah baris per halaman perlu diubah, modifikasi
	 * konstanta {@code Common.ROWS_COUNT_ON_PAGE}. Jika urutan default perlu diubah,
	 * modifikasi {@code initCriteria(true)}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PeminjamanMasterAsset> peminjamanMasterAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(peminjamanMasterAsset);
		grid.setRowRenderer(new PeminjamanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan grid formulir isian peminjaman aset yang
	 * dapat digunakan baik dalam mode edit penuh maupun mode persetujuan (read-only),
	 * sesuai implementasi antarmuka {@code FormSop}.
	 *
	 * <b>Cara kerja:</b> Menginisialisasi {@code satuanKerjaTreeModel} jika belum ada,
	 * menyimpan referensi entitas dan disposisi SOP. Membuat {@code MyGrid} dua kolom
	 * (30% label, 70% input) dan membangun baris-baris formulir secara programatis:
	 * (1) satuan kerja dengan {@code AmbilDataSatuanKerjaBanbox};
	 * (2) kode peminjaman — tampil sebagai Label atau input tergantung mode;
	 * (3) tanggal pembuatan — datebox atau Label;
	 * (4) tanggal pinjam — datebox atau Label;
	 * (5) nama peminjam — Textbox atau Label;
	 * (6) keperluan — Textbox multiline atau Label;
	 * (7) lokasi kegiatan — Textbox atau Label;
	 * (8) tanggal kembali — datebox atau Label;
	 * (9) keterangan — MyTextbox multiline atau Label;
	 * (10) grid detail barang via {@code PeminjamanMasterAssetHelper};
	 * (11) baris status persetujuan dengan checkbox setujui (hanya tampil dalam mode
	 * persetujuan tanpa disposisi SOP aktif).
	 * Event listener {@code eventListenerSetuju} dipasang ke grid sebagai atribut
	 * untuk sinkronisasi state persetujuan dari komponen lain. Jika {@code setujuiData}
	 * tidak null, didaftarkan sebagai listener pada checkbox setujui dan timer default
	 * memicu event awal dengan state persetujuan saat ini.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject Entitas {@code PeminjamanMasterAsset} yang akan ditampilkan
	 *                           atau diedit dalam form — harus di-cast dari {@code GeneralValueObject}.
	 * @param disposisiSop       Disposisi SOP aktif yang terkait dengan dokumen ini,
	 *                           atau null jika tidak ada alur SOP.
	 * @param save               Tombol simpan yang dioperasikan dari konteks luar (SOP);
	 *                           hanya digunakan untuk referensi tooltip.
	 * @param setujuiData        Event listener yang akan dipanggil saat checkbox setujui
	 *                           diubah nilainya (digunakan oleh integrasi SOP).
	 * @return {@code MyGrid} yang berisi seluruh form isian siap pakai.
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen UI atau mengisi data.
	 *
	 * <b>Penanganan error:</b> Exception dari inisialisasi komponen diteruskan ke pemanggil.
	 * Nilai null pada field entitas ditangani dengan nilai default kosong atau tanggal sekarang.
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah field baru ke form, tambahkan {@code MyFormRow}
	 * baru dengan pola yang sama. Mode persetujuan ditentukan oleh field {@code persetujuan}
	 * — selalu tambahkan branch if-else untuk mendukung kedua mode.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.peminjamanMasterAsset = (PeminjamanMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		peminjamanMasterAsset.setSatuanKerja(tbmuser.ambilSatuanKerja());

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				peminjamanMasterAsset.getSatuanKerja() == null ? "" : peminjamanMasterAsset.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", peminjamanMasterAsset.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Peminjaman *"));

		tanggalPembuatan = new MyDatebox(
				peminjamanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: peminjamanMasterAsset.getTanggalPembuatan());

		if (peminjamanMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			peminjamanMasterAsset.setKode(noAgenda);
		}

		kode = new Label(peminjamanMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(peminjamanMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat5.get().format(tanggalPembuatan.getValue())));
		} else {
			row.appendChild(tanggalPembuatan);
		}
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		tanggalPinjam = new MyDatebox(peminjamanMasterAsset.getTanggalPinjam() == null ? ais.ui.util.WaktuUtil.getDate()
				: peminjamanMasterAsset.getTanggalPinjam());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pinjam *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat5.get().format(tanggalPinjam.getValue())));
		} else {
			row.appendChild(tanggalPinjam);
		}
		tanggalPinjam.setFormat(Common.dateFormat.get().toPattern());
		tanggalPinjam.setWidth("90%");

		namaPeminjam = new Textbox(peminjamanMasterAsset.getNamaPeminjam());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Peminjam *"));
		if (persetujuan) {
			row.appendChild(new Label(peminjamanMasterAsset.getNamaPeminjam()));
		} else {
			row.appendChild(namaPeminjam);
		}
		namaPeminjam.setWidth("90%");

		keperluan = new Textbox(peminjamanMasterAsset.getKeperluan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keperluan *"));
		if (persetujuan) {
			row.appendChild(new Label(peminjamanMasterAsset.getKeperluan()));
		} else {
			row.appendChild(keperluan);
		}
		keperluan.setWidth("90%");
		keperluan.setRows(2);

		lokasiKegiatan = new Textbox(peminjamanMasterAsset.getLokasiKegiatan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Kegiatan *"));
		if (persetujuan) {
			row.appendChild(new Label(peminjamanMasterAsset.getLokasiKegiatan()));
		} else {
			row.appendChild(lokasiKegiatan);
		}
		lokasiKegiatan.setWidth("90%");

		tanggalKembali = new MyDatebox(
				peminjamanMasterAsset.getTanggalKembali() == null ? ais.ui.util.WaktuUtil.getDate()
						: peminjamanMasterAsset.getTanggalKembali());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kembali *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat5.get().format(tanggalKembali.getValue())));
		} else {
			row.appendChild(tanggalKembali);
		}
		tanggalKembali.setFormat(Common.dateFormat.get().toPattern());
		tanggalKembali.setWidth("90%");

		keterangan = new MyTextbox(
				peminjamanMasterAsset.getKeterangan() == null ? "" : peminjamanMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Peminjaman"));
		if (persetujuan) {
			row.appendChild(new Label(peminjamanMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new PeminjamanMasterAssetHelper(gridMasterAsset = new MyGrid(), false)
				.initDetail(peminjamanMasterAsset, persetujuan));

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Pengajuan Peminjaman ini"));
		setujui.setChecked(peminjamanMasterAsset.getDisetujuiOleh() != null);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujui != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					if (selesai != null && selesai) {
						setujui.setChecked(true);
						setujui.setDisabled(true);
					} else {
						setujui.setChecked(false);
						setujui.setDisabled(false);
					}
				}
			}
		});

		if (setujuiData != null) {
			setujui.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, peminjamanMasterAsset.getDisetujuiOleh() != null));
				}
			});
		}

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan istilah atau nama modul yang digunakan untuk
	 * identifikasi dalam konteks alur SOP.
	 *
	 * <b>Cara kerja:</b> Mengembalikan string tetap "Permintaan Peminjaman Barang"
	 * yang digunakan oleh sistem SOP untuk labeling dan logging.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * @return String nama istilah modul peminjaman untuk keperluan SOP.
	 * @throws Exception Tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Ubah string ini jika nama modul berubah sesuai kebutuhan
	 * organisasi. Nilai ini mungkin muncul di notifikasi SOP dan laporan audit.
	 */
	@Override
	public String istilah() throws Exception {
		return "Permintaan Peminjaman Barang";
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan entitas data peminjaman yang sedang aktif diproses,
	 * sebagai implementasi antarmuka {@code FormSop}.
	 *
	 * <b>Cara kerja:</b> Mengembalikan referensi field {@code peminjamanMasterAsset}
	 * yang ditetapkan saat form diinisialisasi. Dipanggil oleh sistem SOP untuk
	 * mendapatkan data yang sedang dalam proses persetujuan.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * @return Objek {@code DataSop} (bertipe aktual {@code PeminjamanMasterAsset})
	 *         yang sedang aktif dalam form.
	 * @throws Exception Tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Pastikan field {@code peminjamanMasterAsset} selalu terisi
	 * sebelum sistem SOP memanggil metode ini.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return peminjamanMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kelas entitas yang dikelola oleh controller ini,
	 * sebagai implementasi antarmuka {@code FormSop}.
	 *
	 * <b>Cara kerja:</b> Mengembalikan literal kelas {@code PeminjamanMasterAsset.class}
	 * yang digunakan oleh sistem SOP untuk pembuatan criteria Hibernate dan refleksi.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * @return {@code Class} objek untuk {@code PeminjamanMasterAsset}.
	 * @throws Exception Tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Jika entitas utama berubah, perbarui return value di sini
	 * secara bersamaan dengan perubahan pada konstruktor dan field instance.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PeminjamanMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Membangkitkan kode/nomor dokumen peminjaman secara otomatis
	 * berdasarkan konfigurasi penomoran surat yang telah ditetapkan.
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah konfigurasi {@code NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA}
	 * tersedia beserta nomor suratnya. Jika tidak tersedia, menggunakan
	 * {@code Common.getGeneratedBarCode()} sebagai fallback. Jika tersedia, mengambil
	 * index urutan berikutnya — baik dari field {@code nomorIndex} jika konfigurasi
	 * menggunakan index urutan tetap, atau dari {@code getindex()} yang menghitung
	 * jumlah baris yang ada. Jika parameter {@code tambah} true, memanggil
	 * {@code NomorSurat.tambahIndexNomorSurat()} untuk menambah counter permanen.
	 * Format kode akhir dibuat via {@code NomorSurat.format()} dan dilewatkan ke
	 * {@code KodeUnikUtil.pastikanUnik()} untuk memastikan tidak ada duplikat.
	 *
	 * <b>Parameter:</b>
	 * @param tambah {@code true} jika index nomor surat harus ditambah (saat benar-benar
	 *               menyimpan data baru); {@code false} hanya untuk preview kode.
	 * @return String kode peminjaman yang unik dan terformat sesuai konfigurasi.
	 *
	 * <b>Penanganan error:</b> Jika konfigurasi null, fallback ke barcode random.
	 * Keunikan dijamin oleh {@code KodeUnikUtil.pastikanUnik()} dengan sufiks numerik.
	 *
	 * <b>Pemeliharaan:</b> Konfigurasi penomoran diatur melalui menu Nomor Surat Alur
	 * Pengadaan. Jangan mengubah logika counter di sini tanpa memperbarui entity
	 * {@code NomorSuratAlurPengadaan} secara bersamaan.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA == null
				|| NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PeminjamanMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung index urutan berikutnya untuk penomoran dokumen peminjaman
	 * berdasarkan jumlah dokumen yang sudah ada dengan mempertimbangkan aturan reset urutan.
	 *
	 * <b>Cara kerja:</b> Jika parameter null, mengembalikan 0. Mengambil tahun dan bulan
	 * saat ini dari {@code WaktuUtil.getCalendar()}. Membangun {@code Criteria} untuk
	 * {@code PeminjamanMasterAsset} dengan join LEFT ke {@code NomorSuratAlurPengadaan}
	 * dan {@code NomorSurat}, lalu menambahkan restriction berdasarkan konfigurasi:
	 * (1) jika urut berdasarkan nomor — filter by nomorSurat yang sama;
	 * (2) jika urut berdasarkan kelompok — filter by kelompokNomorSurat;
	 * (3) filter per tahun jika konfigurasi resetUrutanTiapTahun;
	 * (4) filter per bulan jika konfigurasi resetUrutanTiapBulan; dan
	 * (5) filter berdasarkan tanggal reset jika ada tanggal reset yang valid.
	 * Menggunakan {@code Projections.rowCount()} untuk mendapatkan jumlah,
	 * lalu menambahkan 1 untuk index berikutnya.
	 *
	 * <b>Parameter:</b>
	 * @param nomorSurat Konfigurasi nomor surat yang menentukan aturan penomoran.
	 *                   Jika null, mengembalikan 0.
	 * @return Nilai index berikutnya (jumlah dokumen yang ada + 1) sebagai {@code Long}.
	 *
	 * <b>Penanganan error:</b> Jika query mengembalikan null, index diinisialisasi ke 0
	 * sebelum ditambah 1. Tidak ada exception yang dilempar secara eksplisit.
	 *
	 * <b>Pemeliharaan:</b> Metode ini rentan terhadap race condition jika banyak pengguna
	 * membuat dokumen secara bersamaan. Keunikan akhir dijamin oleh {@code KodeUnikUtil.pastikanUnik()}
	 * di {@code generateCode()}. Jika ada aturan penomoran baru, tambahkan restriction
	 * tambahan di sini dengan pola yang sama.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PeminjamanMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * <b>Tujuan:</b> Menetapkan mode persetujuan pada controller ini, sebagai implementasi
	 * antarmuka {@code FormSop}.
	 *
	 * <b>Cara kerja:</b> Menyimpan nilai parameter langsung ke field instance {@code persetujuan}.
	 * Flag ini mempengaruhi tampilan form di metode {@code form()} — jika true, semua
	 * input field diganti dengan Label read-only dan baris status persetujuan ditampilkan.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan (form read-only);
	 *                    {@code false} untuk mode edit penuh.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error karena hanya assignment sederhana.
	 *
	 * <b>Pemeliharaan:</b> Pastikan flag ini ditetapkan sebelum metode {@code form()}
	 * dipanggil agar tampilan form sesuai dengan mode yang diinginkan.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
