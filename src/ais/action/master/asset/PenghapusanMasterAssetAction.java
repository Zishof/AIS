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
import org.zkoss.zul.Combobox;
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

import ais.action.master.asset.helper.PenghapusanMasterAssetDetailAction;
import ais.action.master.asset.helper.PenghapusanMasterAssetHelper;
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
import ais.database.model.asset.JenisPengapusanBarang;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PenghapusanMasterAsset;
import ais.database.model.asset.PenghapusanMasterAssetDetail;
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
 * <h3>PenghapusanMasterAssetAction — Pengelola Penghapusan/Penghapusan Aset Tetap dari Inventaris</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah controller ZKoss (turunan {@code GenericAutowireComposer}) yang mengelola
 * seluruh proses administrasi <em>Penghapusan Barang/Aset Tetap</em> dari daftar inventaris
 * organisasi. Penghapusan aset (dikenal juga sebagai penghapusan/write-off) adalah proses
 * formal yang mengubah status barang dari aktif menjadi tidak aktif dalam catatan aset,
 * biasanya karena rusak berat, tidak dapat diperbaiki, usang secara teknologi, atau hilang.
 * Fungsi utama kelas ini meliputi: pencatatan pengajuan penghapusan dengan jenis penghapusan
 * (penjualan, hibah, pemusnahan, dan sebagainya), manajemen daftar barang yang akan dihapus,
 * alur persetujuan oleh pejabat berwenang, pembatalan persetujuan, pencetakan berita acara
 * penghapusan dalam format PDF, dan penghapusan pengajuan yang belum disetujui. Kelas ini
 * mengimplementasikan antarmuka {@code FormSop} untuk integrasi dengan alur SOP multi-tahap.
 *
 * <b>Cara kerja:</b><br>
 * Pada pemuatan halaman ZUL, {@code doBeforeCompose()} melakukan cek keamanan akses, kemudian
 * {@code doAfterCompose()} menginisialisasi semua komponen UI: filter pencarian (satuan kerja,
 * kode, keterangan, status aktif, status persetujuan), hak akses pengguna yang sedang login
 * (CREATE/UPDATE/DELETE/APPROVE/REJECT), paginasi data, dan timer penyegaran otomatis.
 * Data ditampilkan dalam {@code MyGrid} menggunakan inner class {@code PenghapusanMasterAssetRenderer}
 * yang merender tiap baris dengan: panel detail barang via {@code PenghapusanMasterAssetDetailAction},
 * tombol revisi via {@code RevisiHelper}, label jenis penghapusan, informasi pembuat dan
 * persetujuan, keterangan dengan tautan SOP, kontrol status aktif, serta toolbar aksi
 * (cetak, setujui, batalkan, ubah, hapus) yang visibilitasnya dikendalikan oleh hak pengguna
 * dan status persetujuan. Formulir isian dibangun programatik oleh {@code form()} dalam
 * {@code MyGrid} dua kolom. Kode penghapusan dibangkitkan otomatis dari
 * {@code NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA} dengan jaminan keunikan via
 * {@code KodeUnikUtil.pastikanUnik()}.
 *
 * <b>Threading:</b><br>
 * Semua akses database menggunakan {@code HibernateUtil.currentSession()} yang terikat ke
 * thread request ZKoss (pola session-per-request). Operasi yang memerlukan commit terpisah
 * (persetujuan otomatis setelah simpan dan auto-print) dilakukan via
 * {@code Common.createDefaultTimer()} yang menjadwalkan eksekusi ke event berikutnya pada
 * thread UI ZKoss — tidak ada thread latar belakang eksplisit. Keamanan konkurensi
 * bergantung pada manajemen sesi ZKoss dan Hibernate.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas mengimplementasikan {@code FormSop} sehingga wajib mengimplementasikan metode:
 * {@code form()}, {@code cetakData()}, {@code istilah()}, {@code ambil()},
 * {@code ambilClass()}, dan {@code setPersetujuan()}. Perubahan skema tabel
 * {@code penghapusan_master_asset} atau {@code penghapusan_master_asset_detail} harus
 * disinkronkan dengan entitas Hibernate, renderer, metode {@code parameter()}, dan
 * template JasperReports di {@code asset/penghapusan}. Untuk menambah kolom filter baru,
 * tambahkan komponen di ZUL dan restriction di {@code initCriteria()}. Format kode nomor
 * dokumen dikendalikan oleh konfigurasi {@code NomorSuratAlurPengadaan} tanpa perlu
 * mengubah kode di kelas ini. Entitas {@code JenisPengapusanBarang} mengisi combo jenis
 * penghapusan — tambahkan jenis baru melalui master data tanpa mengubah kode ini.
 *
 * @author AIS Development Team
 * @version 1.0
 * @see PenghapusanMasterAsset
 * @see PenghapusanMasterAssetDetail
 * @see JenisPengapusanBarang
 * @see FormSop
 */
public class PenghapusanMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini sesuai kontrak {@code Serializable}.
	 * Nilai tidak boleh diubah kecuali ada perubahan struktur kelas yang tidak kompatibel
	 * dengan versi tersimpan sebelumnya.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal utama yang digunakan untuk form tambah dan ubah data penghapusan. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar seluruh pengajuan penghapusan aset. */
	private MyGrid grid;

	/** Komponen paginasi yang terhubung dengan grid utama untuk navigasi halaman. */
	private Paging paging;

	/** Kolom pencarian berdasarkan kode penghapusan di toolbar filter utama. */
	private Textbox searchkode;

	/** Kolom pencarian berdasarkan keterangan penghapusan di toolbar filter utama. */
	private Textbox searchketerangan;

	/** Banbox filter untuk menyaring data berdasarkan satuan kerja tertentu. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Label yang menampilkan kode penghapusan yang dibangkitkan otomatis pada form. */
	private Label kode;

	/** Kolom teks keterangan detail alasan atau informasi tambahan penghapusan. */
	private MyTextbox keterangan;

	/** Datebox untuk mengisi tanggal pembuatan dokumen penghapusan. */
	private MyDatebox tanggalPembuatan;

	/** Bendera hak akses UPDATE — true jika pengguna dapat mengubah data. */
	private boolean edit = false;

	/** Bendera hak akses DELETE — true jika pengguna dapat menghapus data. */
	private boolean delete = false;

	/** Bendera hak akses APPROVE — true jika pengguna dapat menyetujui pengajuan. */
	private boolean approve = false;

	/** Bendera hak akses REJECT — true jika pengguna dapat membatalkan persetujuan. */
	private boolean reject = false;

	/** Entitas penghapusan yang sedang aktif diedit atau diproses dalam form. */
	private PenghapusanMasterAsset penghapusanMasterAsset;

	/** Tombol tambah data baru yang visibilitasnya dikendalikan oleh hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Grid detail yang menampilkan daftar barang yang akan dihapus dalam satu dokumen. */
	private MyGrid gridMasterAsset;

	/**
	 * Bendera mode persetujuan. Jika {@code true}, form ditampilkan read-only dengan
	 * opsi persetujuan; jika {@code false}, form dapat diedit penuh.
	 */
	private boolean persetujuan = false;

	/** Pengguna yang sedang login, digunakan untuk audit dan penetapan disetujuiOleh. */
	private Tbmuser tbmuser;

	/** Checkbox untuk langsung menyetujui penghapusan saat menyimpan form. */
	private MyCheckboxConfig setujui;

	/** Disposisi SOP yang terkait dengan dokumen penghapusan ini jika alur SOP aktif. */
	private DisposisiSop disposisiSop = null;

	/** Banbox pemilih satuan kerja pada form isian penghapusan. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Model pohon satuan kerja untuk pencarian hierarkis pada filter. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Combobox untuk memilih jenis penghapusan barang (jual, hibah, musnah, dll.). */
	private Combobox jenisPengapusanBarang;

	/** Checkbox filter untuk menampilkan pengajuan yang belum disetujui. */
	private MyCheckboxConfig blmDisetujui;

	/** Checkbox filter untuk menampilkan pengajuan yang sudah disetujui. */
	private MyCheckboxConfig disetujui;

	/**
	 * <b>Tujuan:</b> Konstruktor default yang menginisialisasi controller dalam mode
	 * pengeditan penuh (bukan persetujuan) dan mengambil data pengguna yang sedang login.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.getCurrentUser()} untuk mendapatkan
	 * objek {@code Tbmuser} dari sesi HTTP aktif. Digunakan saat halaman ZUL utama
	 * dimuat langsung oleh ZKoss tanpa konteks SOP.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (konstruktor).
	 *
	 * <b>Penanganan error:</b> Null pengguna akan menyebabkan NPE pada operasi selanjutnya;
	 * validasi sesi dilakukan di {@code doAfterCompose()}.
	 *
	 * <b>Pemeliharaan:</b> Sinkronkan dengan konstruktor berparameter di bawah.
	 */
	public PenghapusanMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Konstruktor berparameter yang menginisialisasi controller dalam
	 * mode persetujuan SOP dan mengambil data pengguna yang sedang login.
	 *
	 * <b>Cara kerja:</b> Menetapkan flag {@code persetujuan} dari argumen, lalu mengambil
	 * pengguna aktif. Dipanggil oleh sistem SOP saat modul penghapusan diintegrasikan
	 * dalam alur persetujuan bertahap, sehingga form perlu tampil read-only dengan
	 * opsi setujui/tolak saja.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mode persetujuan SOP (form read-only);
	 *                    {@code false} untuk mode edit penuh.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (konstruktor).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus.
	 *
	 * <b>Pemeliharaan:</b> Tambahkan inisialisasi atribut tambahan di sini jika
	 * diperlukan berdasarkan mode persetujuan.
	 */
	public PenghapusanMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Metode siklus hidup ZKoss yang dipanggil sebelum komponen ZUL
	 * disusun, untuk melakukan pemeriksaan keamanan akses halaman penghapusan aset.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang memverifikasi
	 * bahwa pengguna yang sedang login memiliki hak akses ke menu penghapusan aset
	 * berdasarkan konfigurasi role. Jika ditolak, pengguna diarahkan ke halaman error
	 * atau login sebelum halaman dirender. Setelah cek, memanggil {@code super.doBeforeCompose()}.
	 *
	 * <b>Parameter:</b>
	 * @param page     Halaman ZKoss yang sedang dikomposisi.
	 * @param parent   Komponen induk tempat halaman dilampirkan.
	 * @param compInfo Metadata komponen dari ZUL.
	 * @return {@code ComponentInfo} dari superclass untuk melanjutkan proses komposisi.
	 *
	 * <b>Penanganan error:</b> Kegagalan cek keamanan dicegat oleh {@code Common.doCheckSecurity()}.
	 *
	 * <b>Pemeliharaan:</b> Selalu pertahankan pemanggilan {@code Common.doCheckSecurity()} di sini.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama ZKoss yang dipanggil setelah semua komponen
	 * ZUL selesai disusun, menginisialisasi seluruh komponen UI dan logika bisnis awal.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZKoss.
	 * Memeriksa validitas sesi dan hak READ — jika gagal, menghapus sesi dan redirect logoff.
	 * Mendaftarkan event listener pada {@code searchparent} untuk memicu pencarian saat
	 * satuan kerja filter berubah. Menginisialisasi {@code SatuanKerjaTreeModel} untuk
	 * filter hierarkis. Mengatur visibilitas tombol tambah berdasarkan hak CREATE.
	 * Membaca flag hak akses edit/delete/approve/reject dari {@code CommonPrivilages}.
	 * Menginisialisasi paginasi dengan event listener. Membuat timer default yang
	 * memuat data awal ke grid segera setelah halaman pertama kali tampil.
	 *
	 * <b>Parameter:</b>
	 * @param comp Komponen root ZUL yang telah selesai disusun.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi komponen atau akses database.
	 *
	 * <b>Penanganan error:</b> Sesi tidak valid ditangani dengan redirect logoff sebelum
	 * inisialisasi komponen UI.
	 *
	 * <b>Pemeliharaan:</b> Setiap komponen filter baru yang ditambahkan ke ZUL perlu
	 * didaftarkan event listenernya di sini. Validasi sesi harus tetap menjadi langkah pertama.
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
	 * data pengajuan penghapusan aset ke dalam baris grid ZKoss.
	 *
	 * <b>Cara kerja:</b> Meng-extend {@code MyRowRenderer} dan mengoverride {@code render()}
	 * yang dipanggil oleh ZKoss untuk setiap entri {@code PenghapusanMasterAsset} dalam model.
	 * Komponen yang dibuat dan dilampirkan ke {@code Row} secara berurutan: panel detail barang
	 * ({@code PenghapusanMasterAssetDetailAction}), tombol revisi ({@code RevisiHelper}),
	 * label jenis penghapusan, Vbox info pembuat (nama + tanggal), Vbox info persetujuan
	 * (dengan referensi final untuk pembaruan dinamis), Vbox keterangan dan tautan SOP,
	 * kontrol status aktif (checkbox atau label sesuai kondisi), dan toolbar aksi (cetak,
	 * setujui, batalkan, ubah, hapus) dengan visibilitas dikendalikan hak akses dan status.
	 *
	 * <b>Threading:</b> Dijalankan pada thread UI ZKoss. Event listener dalam render
	 * menggunakan {@code HibernateUtil.currentSession()} untuk operasi database.
	 *
	 * <b>Pemeliharaan:</b> Urutan komponen dalam {@code render()} harus selalu konsisten
	 * dengan urutan definisi kolom di file ZUL penghapusan. Jika kolom baru ditambahkan
	 * ke grid, tambahkan komponen sebelum {@code toolbar.setParent(arg0)}.
	 */
	class PenghapusanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data penghapusan aset ke dalam komponen
		 * ZKoss {@code Row} yang ditampilkan di grid daftar penghapusan.
		 *
		 * <b>Cara kerja:</b> Menerima entitas {@code PenghapusanMasterAsset} dari parameter
		 * {@code arg1}, membuat dan melampirkan komponen UI ke baris {@code arg0}.
		 * Referensi final ke label dinamis {@code disetujuiOleh} dan {@code disetujuiTanggal}
		 * digunakan oleh event listener persetujuan dan pembatalan untuk memperbarui tampilan
		 * tanpa reload halaman penuh. Tombol setujui hanya tampil jika pengguna punya hak
		 * APPROVE dan pengajuan belum disetujui; tombol batalkan hanya jika punya REJECT
		 * dan sudah disetujui; tombol ubah dan hapus hanya jika belum disetujui.
		 * Konfirmasi dialog ditampilkan sebelum tindakan persetujuan, pembatalan, dan hapus.
		 * Hapus terlebih dahulu menghapus disposisi SOP kemudian detail barang sebelum
		 * menghapus header penghapusan menggunakan timer untuk memastikan flush tepat.
		 *
		 * <b>Parameter:</b>
		 * @param arg0 Baris ZKoss {@code Row} tempat komponen UI dilampirkan.
		 * @param arg1 Objek data — harus bertipe {@code PenghapusanMasterAsset}.
		 * @throws Exception Jika terjadi kesalahan saat membuat komponen atau akses database.
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Exception dari operasi hapus yang melanggar FK ditangkap
		 * dan ditampilkan via {@code MyMessageboxConfig.show()} dengan pesan yang ramah pengguna.
		 * Exception lain diteruskan ke ZKoss.
		 *
		 * <b>Pemeliharaan:</b> Jika ada field baru di entitas yang perlu ditampilkan,
		 * tambahkan Label atau komponen sesuai sebelum baris toolbar.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenghapusanMasterAsset penghapusanMasterAsset = (PenghapusanMasterAsset) arg1;

			final PenghapusanMasterAssetDetailAction detail;
			(detail = new PenghapusanMasterAssetDetailAction(penghapusanMasterAsset, persetujuan)).setParent(arg0);

			RevisiHelper.createNewRevisi(PenghapusanMasterAsset.class, penghapusanMasterAsset,
					penghapusanMasterAsset.getKode()).setParent(arg0);

			new Label(penghapusanMasterAsset.getJenisPengapusanBarang() == null ? ""
					: penghapusanMasterAsset.getJenisPengapusanBarang().getNama()).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(penghapusanMasterAsset.getDibuatOleh() == null ? ""
					: penghapusanMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(penghapusanMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(penghapusanMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(penghapusanMasterAsset.getDisetujuiOleh() == null ? ""
					: penghapusanMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(penghapusanMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(penghapusanMasterAsset.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(penghapusanMasterAsset.getKeterangan())).setParent(vbox1);
			if (penghapusanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa,
						"SOP " + penghapusanMasterAsset.getDisposisiSop().getKeterangan() + " ("
								+ penghapusanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penghapusanMasterAsset.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			if (penghapusanMasterAsset.getDisposisiSop() != null
					&& !penghapusanMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (penghapusanMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(penghapusanMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penghapusanMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(penghapusanMasterAsset);
					}
				});
			} else {
				new Label(penghapusanMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Penghapusan Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(penghapusanMasterAsset);
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && penghapusanMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && penghapusanMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Penghapusan Barang/Jasa ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();

										penghapusanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										penghapusanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, penghapusanMasterAsset);

										disetujuiTanggal
												.setValue(penghapusanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																penghapusanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penghapusanMasterAsset.getDisetujuiOleh() == null ? ""
												: penghapusanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && penghapusanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penghapusanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penghapusanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penghapusanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(penghapusanMasterAsset);

									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Penghapusan Barang/Jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										penghapusanMasterAsset.setDisetujuiOleh(null);
										penghapusanMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, penghapusanMasterAsset);

										disetujuiTanggal
												.setValue(penghapusanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																penghapusanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penghapusanMasterAsset.getDisetujuiOleh() == null ? ""
												: penghapusanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && penghapusanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penghapusanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penghapusanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penghapusanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && penghapusanMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penghapusanMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && penghapusanMasterAsset.getDisetujuiOleh() == null);
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
													penghapusanMasterAsset.getDisposisiSop())) {

												List<PenghapusanMasterAssetDetail> penghapusanMasterAssetDetails = session
														.createCriteria(PenghapusanMasterAssetDetail.class)
														.add(Restrictions.eq("penghapusanMasterAsset",
																penghapusanMasterAsset))
														.list();
												for (PenghapusanMasterAssetDetail penghapusanMasterAssetDetail : penghapusanMasterAssetDetails) {
													session.delete(penghapusanMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, penghapusanMasterAsset);
														session.flush();

														onSearchDefault(event);
													}
												});
											}

											onSearchDefault(event);
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
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" untuk membuka form
	 * pembuatan pengajuan penghapusan aset baru.
	 *
	 * <b>Cara kerja:</b> Membuat objek {@code PenghapusanMasterAsset} baru (kosong
	 * tanpa ID), memanggil {@code init()} untuk membangun form isian, lalu menampilkan
	 * {@code addWindow} sebagai modal. Terikat ke event {@code onAdd} di ZUL melalui
	 * mekanisme event ZKoss.
	 *
	 * <b>Parameter:</b>
	 * @param event Objek event ZKoss yang memicu aksi ini (biasanya onClick tombol tambah).
	 * @throws Exception Jika terjadi kesalahan saat membangun form atau membuka modal.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke ZKoss untuk ditangani di level framework.
	 *
	 * <b>Pemeliharaan:</b> Untuk mengisi nilai default pada pengajuan baru (misalnya
	 * tanggal hari ini atau satuan kerja pengguna), lakukan di sini sebelum {@code init()}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PenghapusanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form modal untuk tambah atau ubah
	 * data penghapusan aset, termasuk semua komponen UI dan event listener-nya.
	 *
	 * <b>Cara kerja:</b> Menyimpan entitas ke field instance, mengatur judul modal
	 * berdasarkan mode (tambah/ubah), membersihkan konten {@code addWindow} dengan
	 * {@code Common.clear()}, lalu membangun layout {@code Borderlayout} dengan
	 * area {@code Center} berisi form isian (dari {@code form()}) dan area {@code South}
	 * berisi toolbar dengan tombol "Batal" (menutup modal) dan "Simpan dan Cetak"
	 * (memanggil {@code onSave()}, jika berhasil menyegarkan grid, mereset paginasi,
	 * dan menutup modal).
	 *
	 * <b>Parameter:</b>
	 * @param penghapusanMasterAsset Entitas yang akan diedit (ID null = tambah baru,
	 *                               ID terisi = ubah data yang ada).
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen UI atau mengambil data.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke pemanggil. Pastikan {@code addWindow}
	 * tidak null sebelum memanggil metode ini.
	 *
	 * <b>Pemeliharaan:</b> Tombol aksi tambahan pada modal ditambahkan di area toolbar South.
	 * Perubahan layout form dilakukan di metode {@code form()}.
	 */
	private void init(PenghapusanMasterAsset penghapusanMasterAsset) throws Exception {
		this.penghapusanMasterAsset = penghapusanMasterAsset;
		addWindow.setTitle(penghapusanMasterAsset.getId() == null ? "Tambah Penghapusan Barang" : "Ubah Penghapusan Barang");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");

		disposisiSop=null;center.appendChild(form(penghapusanMasterAsset, disposisiSop, save, null));

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
	 * <b>Tujuan:</b> Memvalidasi seluruh input form penghapusan dan menyimpan data
	 * pengajuan penghapusan beserta detail barang-barang yang dihapus ke database.
	 *
	 * <b>Cara kerja:</b> Melakukan validasi berurutan atas field wajib: kode tidak
	 * kosong, jenis penghapusan harus dipilih dari combobox, keterangan tidak kosong,
	 * dan setiap baris detail barang harus memiliki aset yang dipilih. Validasi gagal
	 * menampilkan dialog peringatan dan mengembalikan false. Setelah validasi berhasil,
	 * membuka sesi Hibernate dan: (1) load ulang entitas dari DB jika mode ubah;
	 * (2) menyalin nilai dari komponen UI ke entitas (satuan kerja, jenis penghapusan,
	 * kode, keterangan, tanggal); (3) menghitung total nilai dari semua detail;
	 * (4) menyimpan atau update entitas induk (dengan pembangkitan kode baru untuk
	 * data baru); (5) menyimpan semua detail barang. Timer default menangani persetujuan
	 * otomatis (jika checkbox setujui dicentang) dan pencetakan dokumen PDF.
	 *
	 * <b>Parameter:</b>
	 * @param event Objek event ZKoss dari tombol Simpan.
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan database atau pembuatan laporan.
	 *
	 * <b>Penanganan error:</b> Validasi input menggunakan dialog peringatan.
	 * Race condition kode duplikat ditangani oleh {@code KodeUnikUtil.pastikanUnik()}.
	 *
	 * <b>Pemeliharaan:</b> Field wajib baru harus ditambahkan validasinya di awal metode
	 * sebelum blok database. Perbarui kalkulasi nilai jika logika bisnis berubah.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Penghapusan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Penghapusan atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisPengapusanBarang.getSelectedItem() == null
				|| jenisPengapusanBarang.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Penghapusan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis penghapusan dari dropdown yang tersedia; (2) Pastikan jenis penghapusan sudah dikonfigurasi di menu Jenis Penghapusan Barang; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan Penghapusan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Keterangan dengan alasan atau deskripsi penghapusan aset; (2) Keterangan wajib diisi untuk keperluan dokumentasi dan audit; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PenghapusanMasterAssetDetail penghapusanMasterAssetDetail = (PenghapusanMasterAssetDetail) row
					.getAttribute("penghapusanMasterAssetDetail");
			if (penghapusanMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar penghapusan belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih aset yang akan dihapus dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (penghapusanMasterAsset.getId() != null) {
			penghapusanMasterAsset = (PenghapusanMasterAsset) session.load(PenghapusanMasterAsset.class,
					penghapusanMasterAsset.getId());
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			penghapusanMasterAsset.setDisposisiSop(disposisiSop);
		}
		penghapusanMasterAsset
				.setJenisPengapusanBarang((JenisPengapusanBarang) jenisPengapusanBarang.getSelectedItem().getValue());
		penghapusanMasterAsset.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		penghapusanMasterAsset.setKode(kode.getValue());
		penghapusanMasterAsset.setKeterangan(keterangan.getValue());
		penghapusanMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());

		Double jumlah = 0.0;
		for (Row row : rowsMasterAsset) {
			PenghapusanMasterAssetDetail penghapusanMasterAssetDetail = (PenghapusanMasterAssetDetail) row
					.getAttribute("penghapusanMasterAssetDetail");
			Double n = penghapusanMasterAssetDetail.getHargaBeli();

			jumlah += n;
		}

		penghapusanMasterAsset.setNilai(jumlah);

		if (penghapusanMasterAsset.getId() != null) {
			session.update(penghapusanMasterAsset);
		} else {
			penghapusanMasterAsset.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			penghapusanMasterAsset.setKode(kode.getValue());
			session.save(penghapusanMasterAsset);
		}

		for (Row row : rowsMasterAsset) {
			PenghapusanMasterAssetDetail penghapusanMasterAssetDetail = (PenghapusanMasterAssetDetail) row
					.getAttribute("penghapusanMasterAssetDetail");
			penghapusanMasterAssetDetail.setPenghapusanMasterAsset(penghapusanMasterAsset);
			session.saveOrUpdate(penghapusanMasterAssetDetail);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					Session session = HibernateUtil.currentSession();
					penghapusanMasterAsset.setDisetujuiOleh(tbmuser);
					penghapusanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(session, penghapusanMasterAsset);
				} else {
					Session session = HibernateUtil.currentSession();
					penghapusanMasterAsset.setDisetujuiOleh(null);
					penghapusanMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(session, penghapusanMasterAsset);
				}

				cetak(penghapusanMasterAsset);
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun peta parameter laporan yang berisi semua data penghapusan
	 * dan detail barang yang diperlukan oleh template JasperReports untuk mencetak
	 * berita acara penghapusan aset.
	 *
	 * <b>Cara kerja:</b> Menyegarkan entitas dari database jika ID tidak null untuk
	 * memastikan data terkini. Membuat {@code Map} via {@code HashMapGenerator.getRand()},
	 * mengisi dengan: properti entitas induk via {@code Common.insertProperty()} (prefix "data"),
	 * parameter disposisi SOP via {@code DisposisiAlurSop.parameterMap()}, dan daftar
	 * {@code maps} berisi satu {@code Map} per detail barang dengan field: kelompok aset,
	 * jenis aset, tipe, spesifikasi, harga beli, nama barang, kode penghapusan, kode aset
	 * (isbn), barcode, nama aset detail, dan teks naratif status persetujuan.
	 * Kunci yang mengandung "disposisiSop" di-null-kan untuk menghindari masalah
	 * serialisasi JasperReports.
	 *
	 * <b>Parameter:</b>
	 * @param penghapusanMasterAsset Entitas penghapusan yang dijadikan sumber data laporan.
	 * @return {@code Map} berisi semua parameter yang dibutuhkan template JasperReports.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Jika tidak ada detail,
	 * laporan tercetak tanpa baris item. Field null ditangani dengan string kosong atau 0.
	 *
	 * <b>Pemeliharaan:</b> Jika template laporan berubah (field baru atau rename),
	 * perbarui kunci Map yang sesuai. Pastikan nama kunci konsisten dengan parameter
	 * di file .jrxml template {@code asset/penghapusan}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map parameter(PenghapusanMasterAsset penghapusanMasterAsset) {
		if (penghapusanMasterAsset != null && penghapusanMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(penghapusanMasterAsset);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", penghapusanMasterAsset.getId());

		Common.insertProperty(PenghapusanMasterAsset.class, penghapusanMasterAsset, parameters, "data");

		DisposisiAlurSop.parameterMap(penghapusanMasterAsset.getDisposisiSop(), parameters);

		Session session = HibernateUtil.currentSession();

		List<PenghapusanMasterAssetDetail> penghapusanMasterAssetDetails = session
				.createCriteria(PenghapusanMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.jenisAsset")).addOrder(Order.asc("masterAsset.kelompokAsset"))
				.add(Restrictions.eq("penghapusanMasterAsset", penghapusanMasterAsset)).list();
		List<Map> maps = new ArrayList<Map>();
		for (PenghapusanMasterAssetDetail penghapusanMasterAssetDetail : penghapusanMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PenghapusanMasterAssetDetail.class, penghapusanMasterAssetDetail, map, "data");

			map.put("kelompok_asset", penghapusanMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
					: penghapusanMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());

			map.put("jenis_asset", penghapusanMasterAssetDetail.getMasterAsset().getJenisAsset() == null ? ""
					: penghapusanMasterAssetDetail.getMasterAsset().getJenisAsset().getNama());

			map.put("tipe_asset", penghapusanMasterAssetDetail.getMasterAsset().getTipe());

			map.put("spesifikasi", penghapusanMasterAssetDetail.getMasterAsset().getSpesifikasi());

			map.put("hargabeli", penghapusanMasterAssetDetail.getHargaBeli());
			map.put("jumlah", 1.0);
			map.put("nama", penghapusanMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode", penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getKode());
			map.put("isbn", penghapusanMasterAssetDetail.getMasterAsset().getKode());
			map.put("barcode", penghapusanMasterAssetDetail.getAssetDetail() == null ? ""
					: penghapusanMasterAssetDetail.getAssetDetail().getBarcode());
			map.put("nama_barang", penghapusanMasterAssetDetail.getAssetDetail() == null ? ""
					: penghapusanMasterAssetDetail.getAssetDetail().getNama());
			String status = "";
			if (penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh().getUserNama()
						+ " pada "
						+ (penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getTanggalPersetujuan() == null ? ""
								: Common.dateFormat51.get().format(penghapusanMasterAssetDetail.getPenghapusanMasterAsset()
										.getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan", penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getKeterangan());
			map.put("tanggal_persetujuan",
					penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() == null ? ""
							: penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh()
									.getUserNama());

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
	 * {@code FormSop} untuk menghasilkan file PDF berita acara penghapusan aset yang
	 * dapat diproses lebih lanjut oleh sistem SOP.
	 *
	 * <b>Cara kerja:</b> Melakukan cast parameter ke {@code PenghapusanMasterAsset},
	 * lalu memanggil {@code Report.generateFileReport()} dengan parameter laporan dari
	 * {@code parameter()}, template {@code "asset/penghapusan"}, format PDF, tanggal
	 * pembuatan, dan locale sistem. Mengembalikan objek {@code File} alih-alih langsung
	 * mengirim ke browser — cocok untuk pengiriman via email atau lampiran SOP.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject Data penghapusan yang akan dicetak —
	 *                           harus bertipe {@code PenghapusanMasterAsset}.
	 * @return Objek {@code File} yang menunjuk ke file PDF yang berhasil dibuat.
	 * @throws Exception Jika template tidak ditemukan, data tidak lengkap, atau error I/O.
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke sistem SOP pemanggil.
	 *
	 * <b>Pemeliharaan:</b> Jika path template laporan berubah, perbarui di sini dan
	 * di metode {@code cetak()} secara bersamaan.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PenghapusanMasterAsset penghapusanMasterAsset = (PenghapusanMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(penghapusanMasterAsset), "asset/penghapusan",
				penghapusanMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan dan langsung menampilkan laporan PDF berita acara
	 * penghapusan aset ke browser pengguna melalui streaming ZKoss.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Report.generatePDFReport()} dengan parameter
	 * laporan dari {@code parameter()}, template {@code "asset/penghapusan"}, dan tanggal
	 * pembuatan sebagai konteks waktu. Laporan langsung ditampilkan atau diunduh pengguna.
	 * Dipanggil setelah simpan berhasil dan setelah operasi persetujuan atau pembatalan.
	 *
	 * <b>Parameter:</b>
	 * @param penghapusanMasterAsset Entitas penghapusan yang akan dicetak sebagai laporan.
	 * @throws Exception Jika terjadi kesalahan saat membuat atau mengirimkan laporan PDF.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Exception diteruskan ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b> Untuk mengganti format output (misalnya ke Excel), ganti
	 * konstanta {@code Report.PDF} dan sesuaikan di {@code cetakData()} secara konsisten.
	 */
	@SuppressWarnings({})
	private void cetak(PenghapusanMasterAsset penghapusanMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(penghapusanMasterAsset), "asset/penghapusan",
				penghapusanMasterAsset.getTanggalPembuatan());
	}

	/** Checkbox filter untuk menampilkan hanya pengajuan yang berstatus aktif. */
	private Checkbox searchaktif;

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate yang mencerminkan
	 * semua kondisi filter pencarian aktif untuk data penghapusan aset.
	 *
	 * <b>Cara kerja:</b> Memeriksa null pada {@code searchparent} — jika null return null.
	 * Mengambil satuan kerja yang dipilih dan menyusun himpunan satuan kerja yang relevan
	 * termasuk anak-cucunya via {@code satuanKerjaTreeModel.getChildsSet()}. Membangun
	 * {@code Criteria} untuk {@code PenghapusanMasterAsset} dengan restriction berurutan:
	 * (1) filter status aktif berdasarkan checkbox searchaktif;
	 * (2) filter status persetujuan dari kombinasi checkbox blmDisetujui dan disetujui
	 *     dengan logika: keduanya = semua, keduanya tidak = tidak ada, hanya blmDisetujui
	 *     = isNull disetujuiOleh, hanya disetujui = isNotNull disetujuiOleh;
	 * (3) filter satuan kerja termasuk hierarki;
	 * (4) filter kode menggunakan ILIKE (case-insensitive, anywhere match);
	 * (5) filter keterangan menggunakan ILIKE.
	 * Jika {@code order} true, menambahkan urutan menurun berdasarkan ID.
	 *
	 * <b>Parameter:</b>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC;
	 *              {@code false} untuk criteria tanpa urutan (untuk count paginasi).
	 * @return {@code Criteria} yang sudah dikonfigurasi, atau {@code null} jika
	 *         {@code searchparent} belum diinisialisasi.
	 *
	 * <b>Penanganan error:</b> Himpunan satuan kerja kosong menggunakan restriction {@code 1=1}.
	 * Tidak ada exception yang dilempar.
	 *
	 * <b>Pemeliharaan:</b> Tambahkan filter baru dengan menambahkan blok {@code .add()}
	 * setelah restriction terakhir. Metode ini dipanggil dua kali di {@code onSearchDefault()}.
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
		Criteria criteria = session.createCriteria(PenghapusanMasterAsset.class)

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
	 * <b>Tujuan:</b> Melakukan pencarian dan pembaruan tampilan grid daftar penghapusan
	 * aset berdasarkan filter aktif saat ini, dengan dukungan paginasi.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code initCriteria(false)} untuk menghitung total
	 * baris dan memperbarui komponen paginasi via {@code Common.initPaging()}.
	 * Kemudian memanggil {@code initCriteria(true)} untuk mengambil data halaman aktif
	 * dengan batas {@code ROWS_COUNT_ON_PAGE} baris dan offset sesuai halaman aktif.
	 * Hasilnya dibungkus dalam {@code SimpleListModel} dan disetel ke grid menggunakan
	 * {@code setModelCheckMobile()} dengan renderer {@code PenghapusanMasterAssetRenderer}.
	 *
	 * <b>Parameter:</b>
	 * @param event Objek event ZKoss pemicu pencarian (bisa null dari timer atau inisialisasi).
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Jika {@code initCriteria()} mengembalikan null, akan terjadi
	 * NullPointerException. Pastikan komponen diinisialisasi sebelum metode ini dipanggil.
	 *
	 * <b>Pemeliharaan:</b> Untuk mengubah jumlah baris per halaman, modifikasi konstanta
	 * {@code Common.ROWS_COUNT_ON_PAGE}. Untuk mengubah urutan default, modifikasi
	 * {@code initCriteria(true)}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PenghapusanMasterAsset> penghapusanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penghapusanMasterAsset);
		grid.setRowRenderer(new PenghapusanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan grid formulir isian penghapusan aset
	 * yang dapat digunakan dalam mode edit penuh maupun mode persetujuan read-only,
	 * sesuai implementasi antarmuka {@code FormSop}.
	 *
	 * <b>Cara kerja:</b> Menginisialisasi {@code satuanKerjaTreeModel} jika belum ada,
	 * menyimpan referensi entitas dan disposisi SOP. Membuat {@code MyGrid} dua kolom
	 * (30% label, 70% input) dan membangun baris formulir programatik:
	 * (1) satuan kerja dengan {@code AmbilDataSatuanKerjaBanbox};
	 * (2) kode penghapusan — Label atau input tergantung mode;
	 * (3) tanggal pembuatan — datebox atau Label;
	 * (4) jenis penghapusan — Combobox atau Label (diisi dari {@code JenisPengapusanBarang}
	 *     yang aktif, readonly agar tidak bisa diketik manual);
	 * (5) keterangan — MyTextbox multiline atau Label;
	 * (6) grid detail barang via {@code PenghapusanMasterAssetHelper};
	 * (7) baris status persetujuan dengan checkbox setujui (hanya di mode persetujuan
	 *     tanpa disposisi SOP aktif).
	 * Event listener {@code eventListenerSetuju} dipasang ke atribut grid untuk sinkronisasi
	 * state dari komponen lain. Jika {@code setujuiData} tidak null, didaftarkan pada
	 * checkbox dan timer default memicu event awal dengan state persetujuan saat ini.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject Entitas {@code PenghapusanMasterAsset} yang ditampilkan/diedit.
	 * @param disposisiSop       Disposisi SOP aktif atau null jika tidak ada alur SOP.
	 * @param save               Referensi tombol simpan dari konteks SOP (untuk tooltip saja).
	 * @param setujuiData        Event listener untuk checkbox setujui dari integrasi SOP,
	 *                           atau null jika tidak ada.
	 * @return {@code MyGrid} yang berisi seluruh form isian siap pakai.
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen UI.
	 *
	 * <b>Penanganan error:</b> Nilai null pada field entitas ditangani dengan default
	 * string kosong atau tanggal hari ini. Exception diteruskan ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah field baru, tambahkan {@code MyFormRow} dengan
	 * pola yang sama. Selalu sediakan branch if-else untuk mode persetujuan dan mode edit.
	 * Jenis penghapusan baru ditambahkan via master data tanpa mengubah kode ini.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.penghapusanMasterAsset = (PenghapusanMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		penghapusanMasterAsset.setSatuanKerja(tbmuser.ambilSatuanKerja());

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
		satuanKerja.setValue(penghapusanMasterAsset.getSatuanKerja() == null ? ""
				: penghapusanMasterAsset.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", penghapusanMasterAsset.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Penghapusan *"));

		tanggalPembuatan = new MyDatebox(
				penghapusanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: penghapusanMasterAsset.getTanggalPembuatan());

		if (penghapusanMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			penghapusanMasterAsset.setKode(noAgenda);
		}

		kode = new Label(penghapusanMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(penghapusanMasterAsset.getKode()));
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

		jenisPengapusanBarang = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penghapusan *"));
		if (persetujuan) {
			row.appendChild(new Label(penghapusanMasterAsset.getJenisPengapusanBarang() == null ? ""
					: penghapusanMasterAsset.getJenisPengapusanBarang().getNama()));
		} else {
			row.appendChild(jenisPengapusanBarang);
		}
		Common.insertCombo(jenisPengapusanBarang, "nama", "keterangan", JenisPengapusanBarang.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPengapusanBarang, penghapusanMasterAsset.getJenisPengapusanBarang());
		jenisPengapusanBarang.setWidth("90%");
		jenisPengapusanBarang.setReadonly(true);

		keterangan = new MyTextbox(
				penghapusanMasterAsset.getKeterangan() == null ? "" : penghapusanMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Penghapusan *"));
		if (persetujuan) {
			row.appendChild(new Label(penghapusanMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new PenghapusanMasterAssetHelper(gridMasterAsset = new MyGrid(), persetujuan)
				.initDetail(penghapusanMasterAsset));

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Pengajuan Penghapusan ini"));
		setujui.setChecked(penghapusanMasterAsset.getDisetujuiOleh() != null);

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
					setujuiData.onEvent(new Event("", null, penghapusanMasterAsset.getDisetujuiOleh() != null));
				}
			});
		}

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan nama istilah atau label modul yang digunakan untuk
	 * identifikasi dalam sistem SOP dan logging audit penghapusan aset.
	 *
	 * <b>Cara kerja:</b> Mengembalikan string tetap "Permintaan Penghapusan Barang"
	 * yang digunakan oleh sistem SOP untuk labeling dokumen, notifikasi, dan laporan audit.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * @return String nama istilah modul penghapusan aset untuk keperluan SOP.
	 * @throws Exception Tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Ubah string ini jika terminologi penghapusan berubah sesuai
	 * kebijakan organisasi. Nilai ini mungkin muncul di notifikasi email dan laporan audit.
	 */
	@Override
	public String istilah() throws Exception {
		return "Permintaan Penghapusan Barang";
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan entitas penghapusan yang sedang aktif diproses
	 * dalam form, sebagai implementasi antarmuka {@code FormSop}.
	 *
	 * <b>Cara kerja:</b> Mengembalikan referensi field instance {@code penghapusanMasterAsset}
	 * yang ditetapkan saat form diinisialisasi melalui {@code form()} atau {@code init()}.
	 * Dipanggil oleh sistem SOP untuk mendapatkan objek data yang sedang dalam proses
	 * persetujuan bertahap.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * @return Objek {@code DataSop} (bertipe aktual {@code PenghapusanMasterAsset})
	 *         yang sedang aktif dalam form.
	 * @throws Exception Tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Pastikan field {@code penghapusanMasterAsset} terisi
	 * sebelum sistem SOP memanggil metode ini.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return penghapusanMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kelas entitas yang dikelola oleh controller ini,
	 * sebagai implementasi antarmuka {@code FormSop} untuk keperluan refleksi dan
	 * pembuatan criteria Hibernate oleh sistem SOP.
	 *
	 * <b>Cara kerja:</b> Mengembalikan literal kelas {@code PenghapusanMasterAsset.class}
	 * secara langsung tanpa logika tambahan.
	 *
	 * <b>Parameter:</b> Tidak ada parameter.
	 *
	 * @return {@code Class} objek untuk {@code PenghapusanMasterAsset}.
	 * @throws Exception Tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Perbarui return value jika entitas utama berubah, secara
	 * bersamaan dengan konstruktor dan deklarasi field instance.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PenghapusanMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Membangkitkan kode/nomor dokumen penghapusan secara otomatis
	 * berdasarkan konfigurasi penomoran surat yang telah ditetapkan administrator.
	 *
	 * <b>Cara kerja:</b> Memeriksa ketersediaan konfigurasi
	 * {@code NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA}. Jika tidak tersedia,
	 * menggunakan {@code Common.getGeneratedBarCode()} sebagai fallback. Jika tersedia,
	 * mengambil index berikutnya — dari {@code nomorIndex} jika konfigurasi menggunakan
	 * index tetap, atau dari {@code getindex()} yang menghitung jumlah dokumen yang ada.
	 * Jika {@code tambah} true, memanggil {@code NomorSurat.tambahIndexNomorSurat()}
	 * untuk menambah counter secara permanen. Format kode dihasilkan oleh
	 * {@code NomorSurat.format()} dan keunikannya dijamin oleh {@code KodeUnikUtil.pastikanUnik()}.
	 *
	 * <b>Parameter:</b>
	 * @param tambah {@code true} jika index nomor surat harus ditambah (saat menyimpan
	 *               data baru secara definitif); {@code false} hanya untuk preview kode.
	 * @return String kode penghapusan yang unik dan terformat.
	 *
	 * <b>Penanganan error:</b> Null konfigurasi ditangani dengan fallback barcode random.
	 * Duplikat kode ditangani oleh {@code KodeUnikUtil} dengan penambahan sufiks numerik.
	 *
	 * <b>Pemeliharaan:</b> Format kode dikendalikan oleh konfigurasi admin, bukan
	 * hardcode di sini. Jangan mengubah logika counter tanpa memperbarui
	 * {@code NomorSuratAlurPengadaan} secara bersamaan.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA == null
				|| NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA.getNomorSurat());

		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA.getNomorSurat());
		}

		String noAgenda = NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PenghapusanMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung index urutan berikutnya untuk penomoran dokumen penghapusan
	 * dengan mempertimbangkan aturan reset urutan per tahun, bulan, atau tanggal tertentu.
	 *
	 * <b>Cara kerja:</b> Jika parameter null, mengembalikan 0. Mengambil tahun dan bulan
	 * saat ini dari {@code WaktuUtil.getCalendar()}. Membangun {@code Criteria} untuk
	 * {@code PenghapusanMasterAsset} dengan join LEFT ke {@code NomorSuratAlurPengadaan}
	 * dan {@code NomorSurat}, menambahkan restriction berdasarkan konfigurasi:
	 * (1) urut berdasarkan nomor spesifik atau kelompok nomor surat;
	 * (2) reset per tahun jika dikonfigurasi;
	 * (3) reset per bulan jika dikonfigurasi; dan
	 * (4) reset berdasarkan tanggal tertentu jika ada dan sudah lewat.
	 * Menggunakan {@code Projections.rowCount()} untuk menghitung, lalu menambah 1.
	 * Perbedaan dari implementasi pada {@code PeminjamanMasterAssetAction} adalah
	 * penggunaan field {@code "tanggal"} (bukan {@code "tanggalPembuatan"}) pada
	 * restriction filter tanggal reset.
	 *
	 * <b>Parameter:</b>
	 * @param nomorSurat Konfigurasi aturan penomoran. Jika null, mengembalikan 0.
	 * @return Nilai index berikutnya (jumlah dokumen + 1) sebagai {@code Long}.
	 *
	 * <b>Penanganan error:</b> Hasil null dari query diinisialisasi ke 0 sebelum ditambah 1.
	 *
	 * <b>Pemeliharaan:</b> Perhatikan perbedaan nama field {@code "tanggal"} vs
	 * {@code "tanggalPembuatan"} — pastikan sesuai dengan mapping entitas
	 * {@code PenghapusanMasterAsset}. Keunikan akhir dijamin oleh {@code KodeUnikUtil}.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PenghapusanMasterAsset.class)
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
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggal", nomorSurat.getResetTiap())
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
	 * antarmuka {@code FormSop}, mempengaruhi tampilan form yang dihasilkan oleh {@code form()}.
	 *
	 * <b>Cara kerja:</b> Menyimpan nilai parameter langsung ke field {@code persetujuan}.
	 * Ketika true, metode {@code form()} menampilkan semua field sebagai Label read-only
	 * dan menampilkan baris persetujuan; ketika false, field tampil sebagai input yang
	 * dapat diedit.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mode persetujuan (read-only + opsi setujui);
	 *                    {@code false} untuk mode edit penuh.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error karena hanya assignment sederhana.
	 *
	 * <b>Pemeliharaan:</b> Flag ini harus ditetapkan sebelum pemanggilan {@code form()}
	 * agar tampilan sesuai dengan konteks penggunaan (SOP vs. edit langsung).
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
