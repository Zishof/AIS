package ais.action.master.akunting;

import java.io.File;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanDanaTalangan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>DanaTalanganAction — Pengelola Pengajuan dan Persetujuan Dana Talangan</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK Composer yang menangani seluruh siklus hidup transaksi Dana Talangan
 * (bridging fund / kas besar) dalam sistem keuangan lembaga. Dana Talangan merupakan dana
 * sementara yang dipinjamkan dari kas besar sebelum pencairan anggaran resmi dilakukan,
 * biasanya terkait dengan pengajuan Uang Muka kegiatan. Kelas ini bertanggung jawab atas
 * tampilan daftar, formulir pengajuan baru, pengeditan, penghapusan, persetujuan/penolakan,
 * serta pencetakan laporan Dana Talangan.</p>
 *
 * <p><b>Flag {@code persetujuan} — Dua Mode Operasi:</b><br>
 * Flag boolean {@code persetujuan} adalah inti dari dualisme kelas ini. Ketika bernilai
 * {@code false} (default), kelas berjalan dalam <em>mode pengajuan</em>: pengguna biasa
 * (staf keuangan/pengusul) dapat membuat pengajuan baru, mengisi data uang muka, nilai
 * kasbon, dan mengirimkan formulir. Ketika bernilai {@code true}, kelas berjalan dalam
 * <em>mode persetujuan</em>: tombol tambah disembunyikan, formulir berubah menjadi
 * tampilan read-only (nama, kode, dan nilai hanya ditampilkan sebagai Label), dan muncul
 * elemen tambahan berupa Radiogroup status (Disetujui/Ditolak) serta dropdown sumber dana
 * kasbon. Kelas turunan {@code PersetujuanDanaTalanganAction} memanfaatkan konstruktor
 * {@code DanaTalanganAction(true)} untuk mode ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Saat halaman dimuat, {@code doAfterCompose} dipanggil oleh ZK framework. Di sini
 * dilakukan inisialisasi: pengaturan filter tanggal default (6 bulan ke belakang sampai
 * besok), pengisian combobox status (Semua/Pengajuan/Disetujui/Ditolak), pengecekan
 * hak akses, serta pemasangan timer auto-refresh. Daftar data ditampilkan di
 * {@code MyGrid} dengan renderer {@code DanaTalanganRenderer} yang merender setiap baris
 * secara programatik. Formulir tambah/ubah ditampilkan dalam {@code MyWindow} modal
 * dengan layout {@code Borderlayout} — bagian tengah berisi grid formulir (dibangun oleh
 * metode {@code form()}), sedangkan bagian selatan berisi toolbar simpan/batal.</p>
 *
 * <p>Alur penyimpanan data ({@code onSave}) meliputi validasi wajib, pemuatan ulang entitas
 * dari Hibernate session aktif (bila data sudah ada), penetapan metadata (dibuat oleh,
 * tanggal), pengaturan status dan persetujuan, simpan/update ke database, pembaruan relasi
 * UangMuka, dan pencetakan laporan otomatis setelah simpan. Jika status disetujui, sistem
 * juga membuat DaftarPengajuanTransfer secara otomatis.</p>
 *
 * <p><b>Threading:</b><br>
 * Kelas ini tidak membuat thread baru secara langsung. Semua operasi database berjalan di
 * thread ZK HTTP request. Operasi async (seperti pembuatan DaftarPengajuanTransfer)
 * dilakukan melalui {@code Common.createDefaultTimer} yang menggunakan ZK Timer dengan
 * delay singkat, masih dalam thread event ZK sehingga aman untuk mengakses UI komponen.
 * Paging diinisialisasi dengan EventListener yang memanggil {@code onSearchDefault} saat
 * halaman berganti.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan empat interface: {@code DataCriteria} (kriteria query),
 * {@code DataSearchDefault} (pembaruan daftar), {@code DataInitDefault} (inisialisasi
 * formulir dari data yang ada), dan {@code FormSop} (integrasi alur SOP). Saat menambah
 * field baru pada entitas {@code DanaTalangan}, perlu memperbarui array {@code contents}
 * di {@code doAfterCompose} untuk ekspor Excel, menambah field di {@code form()}, dan
 * menambah logika simpan di {@code onSave}. Flag {@code persetujuan} harus selalu
 * diperiksa sebelum menambah komponen input baru di {@code form()}.
 * Kelas ini terdaftar di halaman ZUL {@code dana_talangan.zul}.</p>
 *
 * @see DanaTalangan
 * @see UangMuka
 * @see PersetujuanDanaTalanganAction
 */
public class DanaTalanganAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * Serial version UID untuk serialisasi kelas oleh framework ZK.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	/** Jendela modal yang menampilkan formulir tambah/ubah Dana Talangan. */
	private MyWindow addWindow;

	/** Komponen paging untuk navigasi daftar data Dana Talangan antar halaman. */
	private Paging paging;

	/** Wadah tab "Dasbor" (autowire dari dana_talangan.zul). */
	private org.zkoss.zul.Vbox dasborDanaTalanganBox;

	/** Grid utama yang menampilkan daftar Dana Talangan dengan renderer baris kustom. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama pengajuan. */
	private Textbox serachnama;

	/** Kotak teks filter pencarian berdasarkan kode Dana Talangan. */
	private Textbox serachkode;

	/** Checkbox filter untuk menampilkan hanya data yang aktif. */
	private Checkbox searchaktif;

	/** Combobox filter berdasarkan status pengajuan (Semua/Pengajuan/Disetujui/Ditolak). */
	private Combobox searchstatus;

	/** Kotak tanggal awal untuk filter rentang tanggal pembuatan pengajuan. */
	private MyDatebox start;

	/** Kotak tanggal akhir untuk filter rentang tanggal pembuatan pengajuan. */
	private MyDatebox end;

	/** Kotak teks input nama/judul pengajuan Dana Talangan di formulir. */
	private Textbox nama;

	/** Label yang menampilkan kode otomatis Dana Talangan (read-only). */
	private Label kode;

	/** Kotak teks untuk keterangan/catatan tambahan pada Dana Talangan. */
	private Textbox keterangan;

	/** Combobox pemilihan Uang Muka yang terkait dengan Dana Talangan ini. */
	private Combobox uangMuka;

	/** Entitas Dana Talangan yang sedang aktif diedit atau ditampilkan. */
	public DanaTalangan danaTalangan;

	/** Tombol toolbar untuk menambah pengajuan baru; disembunyikan di mode persetujuan. */
	private MyToolbarbuttonConfig add;

	/** Hak akses update yang diperiksa dari CommonPrivilages; menentukan tombol edit tampil. */
	private boolean edit;

	/** Hak akses delete yang diperiksa dari CommonPrivilages; menentukan tombol hapus tampil. */
	private boolean delete;

	/** Kotak input nilai jumlah pengajuan kasbon dalam format angka desimal. */
	private MyDoublebox nilai;

	/**
	 * Flag mode operasi kelas ini. Nilai {@code false} berarti mode pengajuan (default),
	 * nilai {@code true} berarti mode persetujuan. Flag ini mempengaruhi visibilitas
	 * tombol tambah, tampilan formulir (input vs label), dan kemunculan elemen status.
	 */
	private boolean persetujuan = false;

	/** Pengguna yang sedang login, diambil saat konstruktor dipanggil. */
	private Tbmuser tbmuser;

	/** Radiogroup untuk memilih status persetujuan: Pengajuan, Disetujui, atau Ditolak. */
	private Radiogroup status;

	/** Disposisi SOP yang terkait dengan pengajuan ini, jika alur SOP digunakan. */
	private DisposisiSop disposisiSop = null;

	/** Model pohon satuan kerja untuk filter data berdasarkan unit organisasi. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Komponen banbox untuk memilih satuan kerja sebagai filter pencarian. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Combobox untuk memilih jenis/sumber uang muka (dana kasbon) saat disetujui. */
	private Combobox jenisUangMuka;

	/**
	 * Flag internal yang menandai apakah pengajuan sudah berstatus "Disetujui".
	 * Digunakan untuk mengubah label tombol simpan dan visibilitas field sumber dana.
	 */
	private boolean setujui = false;

	/**
	 * Konstruktor default untuk mode pengajuan.
	 * Mengambil pengguna yang sedang login melalui {@code Common.getCurrentUser()}.
	 * Flag {@code persetujuan} tetap {@code false} sehingga formulir menampilkan
	 * semua input yang dapat diedit dan tombol tambah pengajuan baru terlihat.
	 */
	public DanaTalanganAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Konstruktor untuk mode yang dapat dikonfigurasi, digunakan oleh subkelas.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan subkelas seperti {@code PersetujuanDanaTalanganAction}
	 * mewarisi seluruh fungsionalitas kelas ini namun dengan mode operasi berbeda.
	 * Dengan memanggil {@code super(true)}, subkelas mengaktifkan mode persetujuan.</p>
	 *
	 * <p><b>Cara kerja:</b> Flag {@code persetujuan} disimpan sebagai field instance.
	 * Semua metode yang bergantung pada mode (form, init, onSave, doAfterCompose)
	 * akan membaca flag ini untuk menyesuaikan tampilan dan perilaku. Pengguna yang
	 * login juga diambil pada tahap ini.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan (tombol tambah
	 *                    disembunyikan, formulir read-only, muncul pilihan status);
	 *                    {@code false} untuk mode pengajuan biasa.
	 */
	public DanaTalanganAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Memeriksa keamanan sebelum komposisi halaman dimulai oleh ZK framework.
	 *
	 * <p><b>Tujuan:</b> Memastikan pengguna yang mengakses halaman ini telah melewati
	 * pemeriksaan keamanan sistem sebelum komponen ZK apa pun diinisialisasi. Ini adalah
	 * titik masuk pertama dalam siklus hidup ZK Composer.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang akan memeriksa
	 * sesi aktif dan hak akses dasar. Jika gagal, permintaan akan diarahkan ke halaman
	 * logoff. Setelah pemeriksaan, memanggil implementasi super class untuk melanjutkan
	 * proses komposisi normal ZK.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika pemeriksaan keamanan gagal, {@code Common.doCheckSecurity()}
	 * menangani redirect secara internal. Metode ini tidak melempar exception secara langsung.</p>
	 *
	 * @param page     Halaman ZK yang sedang dikomposisikan.
	 * @param parent   Komponen induk dalam hierarki komponen ZK.
	 * @param compInfo Informasi metadata komponen dari file ZUL.
	 * @return Objek {@code ComponentInfo} dari super class untuk melanjutkan proses komposisi.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman setelah seluruh komponen ZUL selesai dikomposisikan.
	 *
	 * <p><b>Tujuan:</b> Metode ini adalah titik inisialisasi utama halaman Dana Talangan.
	 * Semua setup awal dilakukan di sini: pemeriksaan sesi, konfigurasi komponen UI,
	 * pengisian data awal, pengaturan hak akses, serta pemuatan data pertama kali.</p>
	 *
	 * <p><b>Cara kerja secara terurut:</b></p>
	 * <ol>
	 *   <li>Memeriksa sesi dan hak akses READ; jika tidak ada, pengguna diarahkan ke logoff.</li>
	 *   <li>Mengatur event listener pada komponen {@code searchparent} agar setiap pemilihan
	 *       satuan kerja memperbarui daftar data secara otomatis.</li>
	 *   <li>Membuat model pohon satuan kerja untuk filter.</li>
	 *   <li>Mengatur filter tanggal default: tanggal mulai = 6 bulan lalu, tanggal akhir = besok.
	 *       Kedua DateBox diset readonly agar pengguna memilih via popup kalender.</li>
	 *   <li>Mengisi combobox status dengan pilihan: Semua, Pengajuan, Disetujui, Ditolak.</li>
	 *   <li>Membaca parameter URL {@code persetujuan} jika ada; ini memungkinkan halaman
	 *       yang sama dikonfigurasi via URL tanpa subkelas.</li>
	 *   <li>Menyesuaikan visibilitas tombol tambah berdasarkan flag {@code persetujuan}
	 *       dan hak akses CREATE. Di mode persetujuan, tombol tambah selalu disembunyikan.</li>
	 *   <li>Mengatur hak akses edit dan delete dari {@code CommonPrivilages}.</li>
	 *   <li>Mengatur paging dengan listener agar daftar diperbarui saat ganti halaman.</li>
	 *   <li>Menambahkan tombol cetak/export data dan tombol upload ke toolbar.</li>
	 *   <li>Mengatur label tombol tambah menjadi "Pengajuan Dana Talangan" di mode non-persetujuan.</li>
	 *   <li>Memasang timer auto-refresh dengan {@code Common.createDefaultTimer}.</li>
	 * </ol>
	 *
	 * <p><b>Pengaruh flag {@code persetujuan}:</b> Di mode persetujuan ({@code true}),
	 * tombol tambah ({@code add}) di-set invisible secara eksplisit. Di mode non-persetujuan,
	 * tombol tambah diberi label "Pengajuan Dana Talangan".</p>
	 *
	 * <p><b>Penanganan error:</b> Jika sesi tidak valid atau hak akses READ tidak terpenuhi,
	 * atribut sesi dibersihkan dan pengguna diarahkan ke halaman logoff via {@code Common.goLogoff()}.
	 * Method ini mendeklarasikan {@code throws Exception} mengikuti kontrak ZK Composer.</p>
	 *
	 * @param comp Komponen root halaman ZK yang telah selesai dikomposisikan.
	 * @throws Exception Jika terjadi kesalahan inisialisasi komponen atau operasi database.
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

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Comboitem comboitemSemua = new Comboitem("Semua");
		if (comboitemSemua != null) { comboitemSemua.setValue(null); }
		searchstatus.appendChild(comboitemSemua);

		Comboitem comboitem = new Comboitem(DanaTalangan.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(DanaTalangan.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(DanaTalangan.DISETUJU);
		if (comboitem != null) { comboitem.setValue(DanaTalangan.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(DanaTalangan.DITOLAK);
		if (comboitem != null) { comboitem.setValue(DanaTalangan.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		if (execution.getParameter("persetujuan") != null) {
			boolean persetujuanDariUrl = Boolean.parseBoolean(execution.getParameter("persetujuan"));
			// Parameter URL TIDAK BOLEH menaikkan mode dari pengajuan ke persetujuan --
			// hanya menu Persetujuan (konstruktor super(true), lihat PersetujuanDanaTalanganAction)
			// atau hak APPROVE eksplisit pada menu aktif yang boleh mengaktifkannya. Mencegah
			// eskalasi via ?persetujuan=true di menu Dana Talangan biasa.
			persetujuan = persetujuanDariUrl
					? (persetujuan || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE))
					: false;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "uangMuka", "jenisUangMuka", "nilai",
				"dibuatOleh", "disetujuiOleh", "tanggalPembuatan", "tanggalPersetujuan", "status", "disposisiSop",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DanaTalangan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DanaTalangan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (persetujuan) {
			add.setVisible(false);
		} else {
			add.setLabel("Pengajuan Dana Talangan");
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		// Tab "Dasbor": render dasbor pemantauan dana talangan (asinkron lewat Timer di dalamnya).
		if (dasborDanaTalanganBox != null) {
			try {
				ais.action.master.akunting.helper.DasboardDanaTalangan.render(dasborDanaTalanganBox);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * <h3>DanaTalanganRenderer — Renderer Baris Grid Dana Talangan</h3>
	 *
	 * <p><b>Untuk apa:</b> Kelas inner ini bertanggung jawab merender setiap baris
	 * data {@code DanaTalangan} dalam grid daftar. Setiap baris menampilkan informasi
	 * lengkap mulai dari kode, nama, nilai, data uang muka terkait, informasi pembuat,
	 * status persetujuan, hingga tombol aksi (ubah, hapus, cetak).</p>
	 *
	 * <p><b>Cara kerja:</b> Metode {@code render(Row, Object)} dipanggil oleh ZK framework
	 * untuk setiap elemen dalam ListModel. Data dikasting ke {@code DanaTalangan} kemudian
	 * komponen UI (Label, Vbox, Checkbox, Hbox, Toolbarbutton) diappend ke {@code Row} secara
	 * programatik. Urutan append komponen harus sesuai dengan urutan kolom yang didefinisikan
	 * di file ZUL. Jika data belum memiliki pembuat, diisi dengan pengguna yang sedang login.
	 * Sebuah timer diperiksa untuk otomatis membuat DaftarPengajuanTransfer bila status
	 * Disetujui namun transfer belum terdaftar.</p>
	 *
	 * <p><b>Pengaruh flag {@code persetujuan}:</b> Kolom terakhir (aktif/status) bersifat
	 * dinamis berdasarkan kombinasi flag {@code persetujuan} dan status data. Di mode
	 * persetujuan dengan data belum disetujui, kolom aktif menampilkan Checkbox interaktif
	 * yang mengubah status aktif secara real-time via {@code Common.refreshSaveOrUpdate}.
	 * Di mode non-persetujuan atau data sudah disetujui, kolom menampilkan Label "Ya"/"Tidak".
	 * Tombol ubah dan hapus juga disembunyikan di mode persetujuan atau data sudah disetujui.</p>
	 *
	 * <p><b>Threading:</b> Renderer dipanggil di thread ZK event. Pembuatan DaftarPengajuanTransfer
	 * dilakukan via timer (masih thread ZK) bukan thread terpisah.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika menambah kolom baru di file ZUL, urutan append komponen
	 * di metode {@code render} harus disesuaikan. Perhatikan bahwa urutan kolom sangat
	 * sensitif — kesalahan urutan akan menyebabkan data muncul di kolom yang salah.</p>
	 */
	class DanaTalanganRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data Dana Talangan ke dalam komponen Row ZK.
		 *
		 * <p><b>Tujuan:</b> Mengisi satu baris grid dengan semua informasi Dana Talangan
		 * beserta kontrol interaktif yang sesuai dengan hak akses dan mode operasi saat ini.</p>
		 *
		 * <p><b>Cara kerja per kolom:</b></p>
		 * <ol>
		 *   <li><b>Kode &amp; Nama:</b> Ditampilkan dalam Vbox yang dibungkus dengan
		 *       {@code RevisiHelper.createNewRevisi} untuk mendukung tracking perubahan data.
		 *       Label nama diappend di dalam Vbox tersebut.</li>
		 *   <li><b>Nilai:</b> Diformat dengan {@code Common.numberFormat} (format ribuan).</li>
		 *   <li><b>Uang Muka:</b> Menampilkan kode dan nama Uang Muka yang terkait.</li>
		 *   <li><b>Nilai Uang Muka:</b> Saldo dari entitas UangMuka, diformat angka.</li>
		 *   <li><b>Periode Workspace:</b> Tanggal mulai dan selesai workspace anggaran.</li>
		 *   <li><b>Periode Kegiatan:</b> Tanggal mulai dan selesai kegiatan uang muka.</li>
		 *   <li><b>Dibuat oleh:</b> Nama pengguna dan tanggal pembuatan.</li>
		 *   <li><b>Status:</b> Status pengajuan, nama penyetuju, dan tanggal persetujuan.</li>
		 *   <li><b>Keterangan:</b> Teks keterangan, link SOP jika ada, dan status transfer.</li>
		 *   <li><b>Aktif:</b> Checkbox interaktif (mode persetujuan, belum disetujui) atau
		 *       Label statis "Ya"/"Tidak". Data disposisi SOP tidak aktif selalu label "Tidak aktif".</li>
		 *   <li><b>Tombol aksi:</b> Ubah, hapus, dan cetak. Ubah/hapus dikontrol oleh
		 *       flag {@code persetujuan}, status data, dan hak akses.</li>
		 * </ol>
		 *
		 * <p><b>Penanganan error:</b> Jika entitas terkait (UangMuka, Workspace) bernilai null,
		 * ditampilkan string kosong untuk menghindari NullPointerException.</p>
		 *
		 * @param arg0 Komponen Row ZK yang akan diisi dengan komponen child.
		 * @param arg1 Objek data {@code DanaTalangan} yang akan dirender.
		 * @throws Exception Jika terjadi kesalahan saat merender komponen atau akses data.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DanaTalangan danaTalangan = (DanaTalangan) arg1;

			if (danaTalangan.getDibuatOleh() == null) {
				danaTalangan.setDibuatOleh(tbmuser);
			}

			if (danaTalangan.getDisetujuiOleh() != null && danaTalangan.getDaftarPengajuanTransfer() == null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (danaTalangan.getStatus().equals(UangMuka.DISETUJU)) {
							DaftarPengajuanTransfer.simpanDanaTalangan(danaTalangan);
						}

					}
				});
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(DanaTalangan.class, danaTalangan,
					danaTalangan.getKode() == null ? "" : danaTalangan.getKode().trim().toString())).setParent(arg0);
			new Label(danaTalangan.getNama()).setParent(a);

			new Label(Common.numberFormat.get().format(danaTalangan.getNilai())).setParent(arg0);
			new Label(danaTalangan.getUangMuka() == null ? ""
					: danaTalangan.getUangMuka().getKode() + "-" + danaTalangan.getUangMuka().getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(danaTalangan.getUangMuka().getNilai())).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label((danaTalangan.getUangMuka().getWorkspace().getMulai() == null ? ""
					: Common.dateFormat1.get().format(danaTalangan.getUangMuka().getWorkspace().getMulai()))).setParent(a);
			new Label((danaTalangan.getUangMuka().getWorkspace().getSelesai() == null ? ""
					: " sd " + Common.dateFormat1.get().format(danaTalangan.getUangMuka().getWorkspace().getSelesai())))
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label((danaTalangan.getUangMuka().getMulai() == null ? ""
					: Common.dateFormat1.get().format(danaTalangan.getUangMuka().getMulai()))).setParent(a);
			new Label((danaTalangan.getUangMuka().getSelesai() == null ? ""
					: " sd " + Common.dateFormat1.get().format(danaTalangan.getUangMuka().getSelesai()))).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(danaTalangan.getDibuatOleh() == null ? "" : danaTalangan.getDibuatOleh().getUserNama())
					.setParent(a);
			new Label(danaTalangan.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(danaTalangan.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(danaTalangan.getStatus()).setParent(a);
			(new Label(danaTalangan.getDisetujuiOleh() == null ? "" : danaTalangan.getDisetujuiOleh().getUserNama()))
					.setParent(a);
			(new Label(danaTalangan.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(danaTalangan.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(Common.simpleString(danaTalangan.getKeterangan())).setParent(vbox1);
			if (danaTalangan.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + danaTalangan.getDisposisiSop().getKeterangan() + " ("
						+ danaTalangan.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(danaTalangan.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			DaftarPengajuanTransfer.tampilStatus(danaTalangan.getDaftarPengajuanTransfer(), vbox1);

			if (danaTalangan.getDisposisiSop() != null && !danaTalangan.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !danaTalangan.getStatus().equals(DanaTalangan.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(danaTalangan.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						danaTalangan.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(danaTalangan);
					}
				});
			} else {
				new Label(danaTalangan.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit,
					!persetujuan && !danaTalangan.getStatus().equals(DanaTalangan.DISETUJU),
					delete && !persetujuan && !danaTalangan.getStatus().equals(DanaTalangan.DISETUJU), danaTalangan,
					DanaTalanganAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(danaTalangan);
				}
			});
			button.setParent(hbx);
		}

	}

	/**
	 * Menghasilkan file laporan PDF Dana Talangan untuk keperluan ekspor data massal.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code cetakData} dari interface {@code DataInitDefault}.
	 * Digunakan oleh fitur ekspor data (tombol cetak di toolbar) untuk menghasilkan file PDF
	 * laporan Dana Talangan dari sebuah entitas tertentu.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@code LaporanDanaTalangan} dengan data Dana Talangan
	 * yang diberikan, mengonfigurasi properti laporan (judul, ukuran, visibilitas), kemudian
	 * memanggil {@code Report.generateFileReport} untuk menghasilkan file PDF menggunakan template
	 * JasperReports {@code akunting/danaTalangan}. File yang dihasilkan disimpan sementara dan
	 * dikembalikan untuk diunduh.</p>
	 *
	 * <p><b>Perbedaan dengan {@code cetak(DanaTalangan)}:</b> Metode ini mengembalikan File dan
	 * digunakan untuk ekspor massal/batch. Metode statis {@code cetak} menampilkan laporan
	 * langsung sebagai popup modal di browser.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dilempar ke pemanggil (framework ekspor data)
	 * yang akan menangani pesan error untuk pengguna.</p>
	 *
	 * @param generalValueObject Entitas {@code DanaTalangan} yang akan dicetak laporannya.
	 * @return File PDF laporan Dana Talangan yang telah dihasilkan.
	 * @throws Exception Jika terjadi kesalahan saat membuat laporan atau menulis file.
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		DanaTalangan danaTalangan = (DanaTalangan) generalValueObject;
		LaporanDanaTalangan buktiPengeluaranKas = new LaporanDanaTalangan(danaTalangan);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(),
				"akunting/danaTalangan", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	/**
	 * Menampilkan laporan Dana Talangan sebagai popup modal di browser pengguna.
	 *
	 * <p><b>Tujuan:</b> Mencetak dan menampilkan laporan satu Dana Talangan secara langsung
	 * di halaman yang sedang aktif, dalam jendela modal yang dapat ditutup pengguna.
	 * Metode ini dipanggil baik dari tombol cetak di baris grid maupun secara otomatis
	 * setelah penyimpanan data berhasil.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@code LaporanDanaTalangan} dengan data entitas,
	 * menambahkan laporan sebagai child dari root halaman ZK saat ini (diperoleh via
	 * {@code ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()}), lalu
	 * menampilkannya sebagai jendela modal dengan {@code onModal()}. Metode ini bersifat
	 * {@code static} sehingga dapat dipanggil dari konteks manapun (termasuk dari dalam
	 * anonymous EventListener) tanpa memerlukan referensi instance.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dilempar ke EventListener pemanggil yang
	 * biasanya membungkus panggilan ini dalam blok try-catch atau delegasi ke timer.</p>
	 *
	 * @param danaTalangan Entitas {@code DanaTalangan} yang akan dicetak laporannya.
	 * @throws Exception Jika terjadi kesalahan saat membuat atau menampilkan laporan.
	 */
	public static void cetak(DanaTalangan danaTalangan) throws Exception {
		LaporanDanaTalangan buktiPengeluaranKas = new LaporanDanaTalangan(danaTalangan);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * Menginisialisasi formulir untuk mengedit data Dana Talangan yang sudah ada.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code init} dari interface {@code DataInitDefault}.
	 * Dipanggil ketika pengguna mengklik tombol "Ubah" pada baris grid, dengan data entitas
	 * yang sudah ada dikirimkan sebagai parameter.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengkasting {@code GeneralValueObject} ke {@code DanaTalangan},
	 * kemudian memanggil metode private {@code init(DanaTalangan)} yang membangun seluruh
	 * komponen formulir. Setelah form siap, menampilkan jendela {@code addWindow} sebagai
	 * modal dialog.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dilempar ke pemanggil (framework CRUD).</p>
	 *
	 * @param obj Entitas {@code DanaTalangan} yang akan diedit, dikemas dalam {@code GeneralValueObject}.
	 * @throws Exception Jika terjadi kesalahan saat membangun formulir atau menampilkan modal.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		danaTalangan = (DanaTalangan) obj;
		init(danaTalangan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menangani klik tombol "Tambah" untuk membuat pengajuan Dana Talangan baru.
	 *
	 * <p><b>Tujuan:</b> Event handler yang dipanggil oleh ZK saat pengguna mengklik
	 * tombol tambah pengajuan baru di toolbar. Membuka formulir kosong untuk entri data
	 * Dana Talangan baru.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@code DanaTalangan} baru (tanpa ID), memanggil
	 * {@code init(DanaTalangan)} untuk membangun formulir, lalu menampilkan modal. Exception
	 * yang terjadi saat membuka modal (misalnya konflik Z-index ZK) ditangkap dan
	 * ditampilkan hanya kepada admin via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini tidak boleh dipanggil di mode persetujuan karena
	 * tombol tambah sudah disembunyikan, namun tidak ada guard eksplisit di sini.</p>
	 *
	 * @param event Event ZK yang memicu pemanggilan ini, biasanya dari klik tombol.
	 * @throws Exception Jika terjadi kesalahan saat membangun formulir kosong.
	 */
	public void onAdd(Event event) throws Exception {
		init(new DanaTalangan());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan mengembalikan grid formulir Dana Talangan yang dapat digunakan
	 * baik secara mandiri maupun dalam konteks alur SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code form} dari interface {@code FormSop}.
	 * Metode ini adalah inti dari tampilan formulir — membangun semua baris input (MyFormRow)
	 * dalam sebuah MyGrid yang dikembalikan. Dapat dipanggil dari dalam modal window maupun
	 * dari halaman SOP yang menyematkan formulir ini.</p>
	 *
	 * <p><b>Cara kerja — mode pengajuan ({@code persetujuan=false}):</b></p>
	 * <ul>
	 *   <li>Menampilkan combobox Uang Muka yang dapat dipilih (filter status Pengajuan,
	 *       belum punya Dana Talangan, aktif, dan sesuai satuan kerja).</li>
	 *   <li>Kode dihasilkan otomatis (kosong, akan diisi saat simpan).</li>
	 *   <li>Input Nama, Nilai, dan Keterangan dapat diedit.</li>
	 *   <li>Tombol simpan berlabel "Ajukan dan Cetak".</li>
	 * </ul>
	 *
	 * <p><b>Cara kerja — mode persetujuan ({@code persetujuan=true}):</b></p>
	 * <ul>
	 *   <li>Semua field yang tadinya input berubah menjadi Label read-only.</li>
	 *   <li>Muncul baris Status Pengajuan dengan Radiogroup (Disetujui/Ditolak).</li>
	 *   <li>Jika Disetujui dipilih, baris Sumber Dana Kasbon (JenisUangMuka) muncul.</li>
	 *   <li>Tombol simpan berlabel "Ubah Status Persetujuan dan Cetak".</li>
	 * </ul>
	 *
	 * <p><b>Integrasi SOP:</b> Jika {@code disposisiSop} tidak null dan sudah selesai
	 * ({@code getSelesai() == true}), formulir menjadi view-only sepenuhnya. Grid formulir
	 * menyimpan event listener bertanda {@code eventListenerSetuju} yang dipanggil oleh
	 * sistem SOP saat status setuju berubah dari eksternal. EventListener {@code setujuiData}
	 * opsional dipasang pada RadioGroup untuk integrasi callback SOP.</p>
	 *
	 * <p><b>Event listener onChange Uang Muka:</b> Saat pengguna memilih Uang Muka,
	 * semua label informasi terkait (unit, akun, saldo, periode, nilai pengajuan) diperbarui
	 * otomatis. Dropdown JenisUangMuka juga difilter ulang berdasarkan satuan kerja dari
	 * Uang Muka yang dipilih.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dilempar ke pemanggil. Pastikan
	 * {@code satuanKerjaTreeModel} sudah diinisialisasi sebelum pemanggilan.</p>
	 *
	 * @param generalValueObject Entitas {@code DanaTalangan} yang akan ditampilkan/diedit.
	 * @param disposisiSop       Disposisi SOP jika formulir ini digunakan dalam alur SOP;
	 *                           {@code null} jika bukan dari SOP.
	 * @param save               Tombol simpan yang label-nya dapat diubah dinamis oleh metode ini.
	 * @param setujuiData        EventListener callback dari sistem SOP untuk perubahan status;
	 *                           {@code null} jika tidak dalam konteks SOP.
	 * @return Grid formulir yang telah diisi dengan semua baris input/tampilan.
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen formulir.
	 */
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		danaTalangan = (DanaTalangan) generalValueObject;

		setujui = false;
		if (!persetujuan) {
			if (danaTalangan != null && danaTalangan.getStatus().equals(DanaTalangan.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		boolean viewOnly = false;
		if (danaTalangan.getDisposisiSop() != null && danaTalangan.getDisposisiSop().getDisposisiSetuju() != null
				&& danaTalangan.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& danaTalangan.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		uangMuka = new Combobox();

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Uang Muka *"));

		Common.insertCombo(uangMuka, new String[] { "kode", "nama" }, "workspace", UangMuka.class,

				Restrictions.and(Restrictions.eq("status", UangMuka.PENGAJUAN),
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.or(
												satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
														: Restrictions.or(
																parent == null ? Restrictions.isNull("satuanKerja")
																		: Restrictions.sqlRestriction("false"),
																Restrictions.in("satuanKerja", satuanKerjas)),
												Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja()))),

								Restrictions.and(Restrictions.isNull("danaTalangan"),
										Restrictions.eq("aktif", true)))));

		Common.selectComboItem(true, uangMuka, danaTalangan.getUangMuka());

		uangMuka.setReadonly(true);
		uangMuka.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(danaTalangan.getUangMuka() == null ? ""
					: danaTalangan.getUangMuka().getKode() + "-" + danaTalangan.getUangMuka().getNama()));
		} else {
			row.appendChild(uangMuka);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));

		if (danaTalangan.getKode() == null) {
			String noAgenda = generateCode(false);
			danaTalangan.setKode(noAgenda);
		}

		kode = new Label(danaTalangan.getKode());
		if (persetujuan) {
			row.appendChild(new Label(danaTalangan.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		nama = new Textbox(danaTalangan.getNama());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengajuan *"));
		nama.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(danaTalangan.getNama()));
		} else {
			row.appendChild(nama);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit/Satuan Kerja"));
		final Label unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saldo Anggaran"));
		final Label saldo = new Label();
		row.appendChild(saldo);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Anggaran"));
		final Label tgl = new Label();
		row.appendChild(tgl);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Anggaran"));
		final Label akun = new Label();
		row.appendChild(akun);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan *"));
		final Label mulai = new Label();
		row.appendChild(mulai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Laporan *"));
		final Label selesai = new Label();
		row.appendChild(selesai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pengajuan Uang Muka *"));
		final Label nilaiPengajuan = new Label();
		row.appendChild(nilaiPengajuan);

		jenisUangMuka = new Combobox();

		Common.insertComboDanSemua(jenisUangMuka, new String[] { "kode", "nama" }, "keterangan", JenisUangMuka.class,

				Restrictions.and(Restrictions.eq("aktif", true),
						Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														parent == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.sqlRestriction("false"),
														Restrictions.in("satuanKerja", satuanKerjas)),
										Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja())))));

		Common.selectComboItem(true, jenisUangMuka, danaTalangan.getJenisUangMuka());
		jenisUangMuka.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				UangMuka work = (UangMuka) (uangMuka.getSelectedItem() == null ? null
						: uangMuka.getSelectedItem().getValue());

				if (work != null && kode.getValue().trim().isEmpty()) {
					kode.setValue(work.getKode());
				}
				if (work != null && nama.getValue().trim().isEmpty()) {
					nama.setValue(work.getNama());
				}

				mulai.setValue(
						work == null || work.getMulai() == null ? "" : Common.dateFormat4.get().format(work.getMulai()));
				selesai.setValue(
						work == null || work.getSelesai() == null ? "" : Common.dateFormat4.get().format(work.getSelesai()));

				unit.setValue(work == null || work.getWorkspace().getSatuanKerja() == null ? ""
						: work.getWorkspace().getSatuanKerja().getNama());

				akun.setValue(work == null || work.getWorkspace().getAkun() == null ? ""
						: work.getWorkspace().getAkun().getKode() + "-" + work.getWorkspace().getAkun().getNama());

				saldo.setValue(work == null ? "" : Common.numberFormat.get().format(work.getWorkspace().getHargaTotal()));

				nilaiPengajuan.setValue(work == null ? "" : Common.numberFormat.get().format(work.getNilai()));

				tgl.setValue((work == null || work.getMulai() == null ? "" : Common.dateFormat1.get().format(work.getMulai()))
						+ (work == null || work.getSelesai() == null ? ""
								: " s.d " + Common.dateFormat1.get().format(work.getSelesai())));

				SatuanKerja parent = (SatuanKerja) (work == null ? null : work.getSatuanKerja());

				if (parent != null) {

					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear();
						satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					Common.insertCombo(jenisUangMuka, new String[] { "kode", "nama" }, "keterangan",
							JenisUangMuka.class,

							Restrictions
									.and(Restrictions.eq("aktif", true),
											Restrictions.or(Restrictions.isNull("satuanKerja"),
													Restrictions.or(
															satuanKerjas.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.or(
																			parent == null
																					? Restrictions.isNull("satuanKerja")
																					: Restrictions
																							.sqlRestriction("false"),
																			Restrictions.in("satuanKerja",
																					satuanKerjas)),
															Restrictions.eq("satuanKerja",
																	tbmuser.ambilSatuanKerja())))));

					Common.selectComboItem(true, jenisUangMuka, danaTalangan.getJenisUangMuka());

				}

			}
		};

		uangMuka.addEventListener("onChange", eventListener);

		nilai = new MyDoublebox(danaTalangan.getNilai());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pengajuan Kasbon"));

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(
					danaTalangan.getNilai() == null ? "" : Common.numberFormat.get().format(danaTalangan.getNilai())));
		} else {
			row.appendChild(nilai);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Oleh"));
		row.appendChild(
				new Label(danaTalangan.getDibuatOleh() == null ? "" : danaTalangan.getDibuatOleh().getUserNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Tanggal"));
		row.appendChild(new Label(danaTalangan.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(danaTalangan.getTanggalPembuatan())));

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
		status = new Radiogroup();
		Radio comboitem = new Radio(UangMuka.PENGAJUAN);
		comboitem.setAttribute("value", UangMuka.PENGAJUAN);
		comboitem.setValue(UangMuka.PENGAJUAN);
		comboitem.setVisible(false);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DISETUJU);
		comboitem.setAttribute("value", UangMuka.DISETUJU);
		comboitem.setValue(UangMuka.DISETUJU);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DITOLAK);
		comboitem.setAttribute("value", UangMuka.DITOLAK);
		comboitem.setValue(UangMuka.DITOLAK);
		status.appendChild(comboitem);
		status.setWidth("90%");
		Common.selectRadioItem(status, danaTalangan.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, danaTalangan.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(danaTalangan.getStatus()));
		}

		row = new MyFormRow();
		row.setVisible(setujui);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Dana Kasbon *"));

		if (!persetujuan) {
			row.appendChild(new Label(
					danaTalangan.getJenisUangMuka() == null ? "" : danaTalangan.getJenisUangMuka().getNama()));
		} else {
			row.appendChild(jenisUangMuka);
		}

		jenisUangMuka.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(danaTalangan.getKeterangan() == null ? "" : danaTalangan.getKeterangan());
		if (setujui) {
			row.appendChild(new Label(danaTalangan.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		eventListener.onEvent(null);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(DanaTalangan.DISETUJU);

				if (jenisUangMuka != null && jenisUangMuka.getParent() != null) {
					jenisUangMuka.getParent().setVisible(setujui);
				}

				if (setujui) {
					save.setLabel("Transfer Dana Talangan");
				} else {
					save.setLabel(!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		return grid;
	}

	/**
	 * Membangun tampilan window modal formulir Dana Talangan dengan layout lengkap.
	 *
	 * <p><b>Tujuan:</b> Metode private yang menyiapkan jendela modal {@code addWindow}
	 * dengan layout Borderlayout, memasang grid formulir di area tengah, dan menyediakan
	 * toolbar simpan/batal di area selatan. Ini adalah metode internal yang selalu dipanggil
	 * sebelum window ditampilkan, baik untuk data baru maupun edit.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Mengisi metadata default jika data baru (pembuat dan tanggal pembuatan).</li>
	 *   <li>Mengatur judul window sesuai mode: "Pengajuan Dana Talangan" atau
	 *       "Persetujuan Dana Talangan" berdasarkan flag {@code persetujuan}.</li>
	 *   <li>Membersihkan konten window yang mungkin tersisa dari penggunaan sebelumnya.</li>
	 *   <li>Membuat {@code Borderlayout} dengan Center (formulir) dan South (toolbar).</li>
	 *   <li>Memanggil {@code form()} untuk mengisi area Center dengan grid formulir.</li>
	 *   <li>Di South: tombol Batal menutup window ({@code setVisible(false)}), tombol
	 *       Simpan memanggil {@code onSave}, lalu memperbarui daftar, menutup window,
	 *       dan mencetak laporan via timer.</li>
	 * </ol>
	 *
	 * <p><b>Pengaruh flag {@code persetujuan}:</b> Judul window dan label tombol simpan
	 * berbeda antara mode pengajuan dan persetujuan. Di mode persetujuan, label tombol
	 * simpan awal adalah "Ubah Status Persetujuan dan Cetak".</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dilempar ke pemanggil. Pastikan {@code addWindow}
	 * sudah di-autowire oleh ZK sebelum metode ini dipanggil.</p>
	 *
	 * @param danaTalangan Entitas Dana Talangan yang akan ditampilkan di formulir.
	 *                     Jika baru (ID null), metadata awal akan diisi otomatis.
	 * @throws Exception Jika terjadi kesalahan membangun komponen UI atau mengakses data.
	 */
	private void init(final DanaTalangan danaTalangan) throws Exception {

		if (danaTalangan.getDibuatOleh() == null) {
			danaTalangan.setDibuatOleh(tbmuser);
			danaTalangan.setTanggalPembuatan(new Date());
		}

		addWindow.setTitle((!persetujuan ? "Pengajuan" : "Persetujuan") + " Dana Talangan");
		this.danaTalangan = danaTalangan;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
				!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak", "/img/save.gif");

		disposisiSop=null;center.appendChild(form(danaTalangan, disposisiSop, save, null));

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

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							cetak(DanaTalanganAction.this.danaTalangan);
						}
					});
				}
			}
		});
		save.setParent(toolbar);

	}

	/**
	 * Memvalidasi input formulir dan menyimpan data Dana Talangan ke database.
	 *
	 * <p><b>Tujuan:</b> Metode inti penyimpanan data. Dipanggil saat pengguna mengklik
	 * tombol simpan di window modal. Menangani baik penyimpanan data baru maupun pembaruan
	 * data yang sudah ada, termasuk logika persetujuan dan pembuatan transfer otomatis.</p>
	 *
	 * <p><b>Cara kerja — validasi:</b></p>
	 * <ol>
	 *   <li>Memastikan Uang Muka dipilih (wajib).</li>
	 *   <li>Memastikan Judul Pengajuan tidak kosong (wajib).</li>
	 *   <li>Memastikan Nilai tidak null (wajib).</li>
	 *   <li>Di mode persetujuan dengan status Disetujui: memastikan Sumber Dana Talangan
	 *       (JenisUangMuka) sudah dipilih (wajib).</li>
	 * </ol>
	 *
	 * <p><b>Cara kerja — penyimpanan:</b></p>
	 * <ol>
	 *   <li>Memuat ulang entitas dari Hibernate session aktif jika ID sudah ada
	 *       (menghindari masalah detached entity).</li>
	 *   <li>Mengisi metadata pembuat jika belum ada.</li>
	 *   <li>Mengaitkan DisposisiSop jika ada.</li>
	 *   <li>Mengisi semua field dari komponen UI: UangMuka, kode, nama, nilai, keterangan,
	 *       JenisUangMuka, dan status persetujuan.</li>
	 *   <li>Jika status Disetujui: mengisi disetujuiOleh (pengguna aktif) dan tanggalPersetujuan.
	 *       Jika tidak: mengosongkan kedua field tersebut.</li>
	 *   <li>Melakukan update atau insert baru via Hibernate session.</li>
	 *   <li>Flush session untuk memastikan perubahan tersimpan ke database.</li>
	 *   <li>Via timer: memperbarui relasi UangMuka.danaTalangan, mencetak laporan,
	 *       dan membuat DaftarPengajuanTransfer jika status Disetujui.</li>
	 * </ol>
	 *
	 * <p><b>Pengaruh flag {@code persetujuan}:</b> Di mode persetujuan, validasi tambahan
	 * untuk JenisUangMuka diaktifkan. Status persetujuan hanya dapat diubah di mode ini.</p>
	 *
	 * <p><b>Penanganan error:</b> Validasi menampilkan dialog peringatan dan mengembalikan
	 * {@code false}. Exception Hibernate tidak ditangkap di sini — akan dilempar ke pemanggil.</p>
	 *
	 * @param event Event ZK yang memicu penyimpanan; tidak digunakan secara langsung
	 *              dalam logika penyimpanan, hanya untuk memenuhi tanda tangan metode.
	 * @return {@code true} jika data berhasil disimpan, {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan database atau pemrosesan data.
	 */
	public boolean onSave(Event event) throws Exception {

		UangMuka work = (UangMuka) (uangMuka.getSelectedItem() == null ? null : uangMuka.getSelectedItem().getValue());
		if (work == null) {
			MyMessageboxConfig.show("Mohon maaf, Uang Muka belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis Uang Muka dari dropdown yang tersedia; (2) Pastikan data uang muka sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Pengajuan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengajuan dengan deskripsi singkat yang jelas; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nilai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Nilai belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai dengan nominal yang valid; (2) Pastikan nilai tidak kosong dan lebih dari nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (persetujuan && setujui) {
			if (jenisUangMuka.getSelectedItem() == null || jenisUangMuka.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Sumber Dana Talangan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Sumber Dana Talangan dari dropdown yang tersedia; (2) Pastikan data jenis uang muka sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (danaTalangan.getId() != null) {
			danaTalangan = (DanaTalangan) session.load(DanaTalangan.class, danaTalangan.getId());
		}

		if (danaTalangan.getDibuatOleh() == null) {
			danaTalangan.setDibuatOleh(tbmuser);
			danaTalangan.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			danaTalangan.setDisposisiSop(disposisiSop);
		}

		danaTalangan.setUangMuka(work);
		danaTalangan.setKode(kode.getValue());
		danaTalangan.setNama(nama.getValue());
		danaTalangan.setNilai(nilai.getValue());
		danaTalangan.setKeterangan(keterangan.getValue());

		danaTalangan.setJenisUangMuka((JenisUangMuka) (jenisUangMuka.getSelectedItem() == null ? null
				: jenisUangMuka.getSelectedItem().getValue()));

		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			danaTalangan.setDisetujuiOleh(tbmuser);
			danaTalangan.setTanggalPersetujuan(WaktuUtil.getDate());
		} else {
			danaTalangan.setDisetujuiOleh(null);
			danaTalangan.setTanggalPersetujuan(null);
		}

		danaTalangan.setStatus(sts);

		if (danaTalangan.getId() != null) {
			session.update(danaTalangan);
		} else {
			danaTalangan.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			danaTalangan.setKode(kode.getValue());
			session.save(danaTalangan);
		}

		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				UangMuka work = (UangMuka) (uangMuka.getSelectedItem() == null ? null
						: uangMuka.getSelectedItem().getValue());
				if (work != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(work);
					work.setDanaTalangan(danaTalangan);
					Common.refreshUpdate(session, work);
					session.flush();

				}

				cetak(DanaTalanganAction.this.danaTalangan);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (danaTalangan.getStatus().equals(DanaTalangan.DISETUJU)) {
							DaftarPengajuanTransfer.simpanDanaTalangan(danaTalangan);
						}
					}
				});

			}
		});

		return true;
	}

	/**
	 * Membangun kriteria Hibernate untuk query daftar Dana Talangan berdasarkan filter aktif.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code initCriteria} dari interface {@code DataCriteria}.
	 * Metode ini membangun objek {@code Criteria} Hibernate yang merepresentasikan kueri pencarian
	 * dengan semua filter yang saat ini aktif di UI. Dipanggil dua kali per refresh: sekali untuk
	 * menghitung total baris (paging), sekali untuk mengambil baris yang ditampilkan.</p>
	 *
	 * <p><b>Cara kerja — filter yang diterapkan:</b></p>
	 * <ul>
	 *   <li><b>Rentang tanggal:</b> Filter SQL native pada {@code date(tanggal_pembuatan)}
	 *       antara {@code start} dan {@code end}. Jika salah satu null, filter diabaikan.</li>
	 *   <li><b>Satuan kerja:</b> Hierarki satuan kerja dari {@code searchparent} dibangun
	 *       dengan {@code SatuanKerjaTreeModel.getChildsSet}. Data dengan {@code satuanKerja}
	 *       null selalu tampil jika tidak ada filter satuan kerja aktif.</li>
	 *   <li><b>Status:</b> Filter berdasarkan nilai status yang dipilih di combobox.
	 *       Jika "Semua" dipilih (nilai null), filter diabaikan.</li>
	 *   <li><b>Aktif:</b> Jika checkbox aktif dicentang atau null, hanya data aktif yang tampil.
	 *       Jika tidak dicentang, semua data tampil.</li>
	 *   <li><b>Kode:</b> Filter ILIKE anywhere jika kotak pencarian kode tidak kosong.</li>
	 *   <li><b>Nama:</b> Filter ILIKE anywhere jika kotak pencarian nama tidak kosong.</li>
	 * </ul>
	 *
	 * <p><b>Parameter {@code order}:</b> Jika {@code true}, kriteria ditambahkan
	 * {@code Order.desc("id")} sehingga data terbaru muncul di atas. Untuk query
	 * count (paging), ordering tidak diperlukan sehingga {@code false} dikirim.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika komponen filter null atau kosong, filter tersebut
	 * digantikan dengan {@code sqlRestriction("true")} yang bersifat pass-through.</p>
	 *
	 * @param order {@code true} untuk menambahkan urutan descending by ID; {@code false} tanpa urutan.
	 * @return Objek {@code Criteria} Hibernate yang siap dieksekusi untuk query daftar.
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(DanaTalangan.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * Memperbarui tampilan daftar Dana Talangan berdasarkan filter yang aktif saat ini.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code onSearchDefault} dari interface
	 * {@code DataSearchDefault}. Metode ini merupakan titik tunggal untuk merefresh
	 * seluruh daftar data Dana Talangan di grid, baik dipanggil saat inisialisasi,
	 * setelah simpan/hapus, saat ganti halaman paging, maupun dari timer auto-refresh.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Memanggil {@code initCriteria(false)} untuk mendapatkan total baris guna
	 *       menghitung jumlah halaman, kemudian memperbarui komponen {@code Paging}.</li>
	 *   <li>Memanggil {@code initCriteria(true)} dengan {@code setMaxResults} dan
	 *       {@code setFirstResult} untuk mengambil baris sesuai halaman aktif.</li>
	 *   <li>Membuat {@code SimpleListModel} dari hasil query.</li>
	 *   <li>Mengatur {@code DanaTalanganRenderer} sebagai renderer baris grid.</li>
	 *   <li>Menampilkan data di grid melalui {@code setModelCheckMobile}.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate tidak ditangkap di sini dan akan
	 * menyebar ke framework ZK yang menangani error UI secara global.</p>
	 *
	 * @param event Event ZK yang memicu refresh; boleh {@code null} jika dipanggil programatik.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DanaTalangan> danaTalangan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(danaTalangan);
		grid.setRowRenderer(new DanaTalanganRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengembalikan istilah/nama entitas untuk keperluan integrasi SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code istilah} dari interface {@code FormSop}.
	 * String yang dikembalikan digunakan oleh framework SOP untuk menampilkan nama
	 * modul dalam alur persetujuan.</p>
	 *
	 * @return String nama modul yang akan ditampilkan dalam konteks alur SOP.
	 * @throws Exception Tidak dilempar oleh implementasi ini, tetapi dideklarasikan
	 *                   untuk memenuhi kontrak interface.
	 */
	@Override
	public String istilah() throws Exception {
		return "Pengajuan Dana Talangan / Kas Besar";
	}

	/**
	 * Mengembalikan entitas DataSop saat ini untuk integrasi alur SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code ambil} dari interface {@code FormSop}.
	 * Framework SOP menggunakan metode ini untuk mendapatkan referensi ke entitas yang
	 * sedang diproses dalam alur persetujuan, agar dapat mengaitkan disposisi SOP.</p>
	 *
	 * @return Entitas {@code DanaTalangan} yang sedang aktif, atau {@code null} jika belum ada.
	 * @throws Exception Tidak dilempar oleh implementasi ini.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return danaTalangan;
	}

	/**
	 * Mengembalikan kelas entitas untuk keperluan integrasi SOP dan ekspor data.
	 *
	 * <p><b>Tujuan:</b> Implementasi metode {@code ambilClass} dari interface {@code FormSop}.
	 * Digunakan oleh framework untuk refleksi terhadap kelas entitas yang dikelola.</p>
	 *
	 * @return Kelas {@code DanaTalangan.class} sebagai tipe entitas yang dikelola.
	 * @throws Exception Tidak dilempar oleh implementasi ini.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return DanaTalangan.class;
	}

	/**
	 * Menghasilkan kode unik untuk Dana Talangan baru berdasarkan format NomorSurat.
	 *
	 * <p><b>Tujuan:</b> Menghasilkan kode/nomor surat Dana Talangan yang sesuai dengan
	 * konfigurasi penomoran yang telah ditetapkan di {@code NomorSuratAlurKeuangan.DANA_TALANGAN_DATA}.
	 * Kode yang dihasilkan dijamin unik dalam tabel DanaTalangan oleh {@code KodeUnikUtil.pastikanUnik}.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Jika konfigurasi NomorSurat belum tersedia (null), dikembalikan barcode otomatis
	 *       via {@code Common.getGeneratedBarCode()} sebagai fallback.</li>
	 *   <li>Jika konfigurasi ada, menentukan index urutan: jika menggunakan indeks urut manual
	 *       ({@code gunakanIndexUrut = true}), diambil dari field {@code nomorIndex}; jika tidak,
	 *       dihitung dari database via {@code getindex(NomorSurat)}.</li>
	 *   <li>Jika parameter {@code tambah} adalah {@code true} (simpan definitif) dan mode
	 *       index urut aktif, indeks dibaca sekaligus ditambah satu secara atomik via
	 *       {@code NomorSurat.ambilLaluTambahIndexNomorSurat} untuk mencegah duplikasi
	 *       di pemanggilan berikutnya.</li>
	 *   <li>Format kode dihasilkan dari template NomorSurat dengan index dan tanggal saat ini.</li>
	 *   <li>Keunikan akhir dijamin oleh {@code KodeUnikUtil.pastikanUnik} yang menambah sufiks
	 *       numerik (-2, -3, dst.) jika kode sudah ada.</li>
	 * </ol>
	 *
	 * <p><b>Parameter {@code tambah}:</b> Saat formulir dibuka (preview kode), dipanggil
	 * dengan {@code false} sehingga indeks tidak bertambah. Saat simpan definitif, dipanggil
	 * dengan {@code true}.</p>
	 *
	 * @param tambah {@code true} jika ini adalah penyimpanan definitif (index bertambah);
	 *               {@code false} jika hanya untuk preview kode di formulir.
	 * @return String kode Dana Talangan yang dihasilkan dan dijamin unik.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.DANA_TALANGAN_DATA == null
				|| NomorSuratAlurKeuangan.DANA_TALANGAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		NomorSurat ns = NomorSuratAlurKeuangan.DANA_TALANGAN_DATA.getNomorSurat();
		Long index = (tambah && ns.getGunakanIndexUrut()) ? NomorSurat.ambilLaluTambahIndexNomorSurat(ns)
				: (ns.getGunakanIndexUrut() ? ns.getNomorIndex() : getindex(ns));

		String noAgenda = ns.format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(DanaTalangan.class, noAgenda);
	}

	/**
	 * Menghitung index urutan berikutnya untuk penomoran Dana Talangan dari database.
	 *
	 * <p><b>Tujuan:</b> Menentukan nomor urut yang akan digunakan dalam format kode Dana Talangan
	 * dengan cara menghitung jumlah record yang sudah ada sesuai aturan penomoran yang dikonfigurasi
	 * di {@code NomorSurat}. Index yang dihasilkan selalu satu lebih besar dari jumlah record
	 * yang ditemukan.</p>
	 *
	 * <p><b>Cara kerja — aturan penomoran yang didukung:</b></p>
	 * <ul>
	 *   <li><b>Urut berdasarkan nomor surat ({@code urutBerdasarkanNomor}):</b> Hanya menghitung
	 *       record yang menggunakan NomorSurat yang sama persis.</li>
	 *   <li><b>Urut berdasarkan kelompok ({@code urutBerdasarkanKelompok}):</b> Menghitung semua
	 *       record dalam kelompok NomorSurat yang sama.</li>
	 *   <li><b>Default:</b> Menghitung semua record tanpa batasan nomor surat.</li>
	 *   <li><b>Reset per tahun ({@code resetUrutanTiapTahun}):</b> Filter tambahan ke tahun saat ini.</li>
	 *   <li><b>Reset per bulan ({@code resetUrutanTiapBulan}):</b> Filter tambahan ke tahun dan bulan.</li>
	 *   <li><b>Reset pada tanggal tertentu ({@code resetTiap}):</b> Hanya menghitung record
	 *       sejak tanggal reset terakhir.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan null:</b> Jika {@code nomorSurat} null, dikembalikan 0. Jika hasil
	 * query null (tidak ada record), dikembalikan 1 (index pertama).</p>
	 *
	 * @param nomorSurat Konfigurasi penomoran yang menentukan cara menghitung index urutan.
	 *                   Boleh null, dalam hal ini dikembalikan 0.
	 * @return Index urutan berikutnya yang akan digunakan dalam format kode (selalu >= 1).
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(DanaTalangan.class)
				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)

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
	 * Mengatur flag mode persetujuan dari luar, digunakan oleh sistem SOP atau subkelas.
	 *
	 * <p><b>Tujuan:</b> Implementasi setter yang memungkinkan framework SOP atau kode eksternal
	 * mengubah mode operasi kelas setelah instansiasi. Metode ini diperlukan oleh interface
	 * {@code FormSop} untuk mengonfigurasi formulir dalam konteks alur persetujuan SOP.</p>
	 *
	 * <p><b>Cara kerja:</b> Langsung mengatur field {@code persetujuan} dengan nilai yang
	 * diberikan. Perubahan ini akan mempengaruhi semua metode yang membaca flag tersebut
	 * (form, init, onSave) pada pemanggilan berikutnya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Setter ini harus dipanggil sebelum {@code doAfterCompose}
	 * atau sebelum {@code form()} dipanggil agar perubahan mode berefek. Pemanggilan
	 * setelah halaman sudah dirender tidak akan memperbarui komponen yang sudah ada.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} untuk kembali ke mode pengajuan.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
