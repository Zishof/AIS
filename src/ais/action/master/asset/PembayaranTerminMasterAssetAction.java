package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.asset.helper.PembayaranTerminMasterAssetHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisPembayaranBarang;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PembayaranTerminMasterAsset;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
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
 * <h3>Untuk apa</h3>
 * {@code PembayaranTerminMasterAssetAction} adalah kontroler ZK (ZKoss 5.5,
 * {@link GenericAutowireComposer}) untuk halaman manajemen <b>Pembayaran Termin
 * Barang/Jasa</b> dalam modul pengadaan aset. Kelas ini bertanggung jawab atas
 * seluruh siklus hidup dokumen pembayaran termin kepada vendor/penyedia aset:
 * penciptaan, ubah, hapus, persetujuan, pembatalan persetujuan, cetak PDF, dan
 * ekspor data. Setiap pembayaran termin dapat terdiri dari beberapa detail
 * ({@link PembayaranTerminMasterAssetDetail}) yang masing-masing merujuk pada
 * satu pesanan pengadaan ({@link PemesananPengadaanMasterAsset}).
 *
 * <h3>Cara kerja</h3>
 * Pada saat halaman ZUL dimuat, {@link #doBeforeCompose} memeriksa keamanan
 * sesi via {@code Common.doCheckSecurity()}, kemudian {@link #doAfterCompose}
 * mengambil hak akses pengguna (CREATE/READ/UPDATE/DELETE/APPROVE/REJECT),
 * mengisi filter tanggal default (6 bulan ke belakang hingga besok), mengisi
 * combo status lunas, dan menjalankan pencarian awal melalui
 * {@link #onSearchDefault}. Grid utama menggunakan renderer dalam kelas
 * {@code PembayaranTerminMasterAssetRenderer} yang menampilkan kode termin,
 * lampiran bukti pembayaran, daftar pesanan pengadaan terkait, nama penyedia,
 * nilai dibayar, jenis pembayaran, pembuat, penyetuju, keterangan, status
 * aktif, dan tombol aksi (Cetak/Setujui/Batalkan/Ubah/Hapus).
 *
 * <p>Penyimpanan data ({@link #onSave}) melakukan validasi input, kemudian
 * menyimpan header pembayaran termin ke database dengan penanganan khusus
 * session Hibernate: metode {@link #pastikanSessionTerbuka} memastikan session
 * thread-local tidak tertutup di tengah loop iterasi detail. Setiap detail
 * diperbarui bersama akumulasi total telah dibayar pada pesanan pengadaan
 * induknya. Bila jenis pembayaran tidak dipilih atau belum ada daftar pengajuan
 * transfer, sistem membuat entri {@code DaftarPengajuanTransfer} secara
 * otomatis. Lampiran dokumen (hingga empat file) diperbarui referensinya ke ID
 * pembayaran termin yang baru disimpan melalui session streaming Hibernate
 * terpisah agar tidak mencemari transaksi utama.
 *
 * <p>Nomor kode pembayaran dihasilkan oleh {@link #generateCode} menggunakan
 * pola {@link NomorSuratAlurPengadaan} yang dikonfigurasi administrator. Metode
 * {@link #generateKodeUnik} menambahkan lapisan perlindungan duplikat dengan
 * memeriksa keunikan ke database hingga 50 kali percobaan sebelum menambahkan
 * sufiks acak sebagai jalan darurat.
 *
 * <p>Cetak dokumen menghasilkan laporan PDF via {@code Report.generatePDFReport}
 * dengan template {@code asset/pembayaran_termin}. Data parameter laporan
 * dirakit oleh {@link #parameter} yang menyertakan informasi bank penyedia,
 * dokumen vendor, alur SOP, detail termin per pesanan, dan akumulasi total
 * penagihan/pinalti/pekerjaan.
 *
 * <h3>Threading</h3>
 * Kelas ini diinstansiasi per sesi ZK (bukan singleton). Semua aksi pengguna
 * berjalan di event thread ZK kecuali timer ({@code Common.createDefaultTimer})
 * yang menggunakan thread pool ZK untuk operasi lampiran dan cetak. Tidak ada
 * sinkronisasi tambahan karena ZK menjamin satu event per sesi diproses
 * secara berurutan. Penggunaan {@code HibernateUtil.currentNativeSession()}
 * di dalam {@code onSave} dimaksudkan agar session tidak bergantung pada
 * thread-local yang bisa ditutup oleh callee lain.
 *
 * <h3>Pemeliharaan</h3>
 * <ul>
 *   <li>Saat menambah kolom baru pada entitas, perbarui juga array
 *       {@code contents} di {@link #doAfterCompose} agar ekspor Excel mencakup
 *       kolom tersebut.</li>
 *   <li>Bila logika SOP berubah, perhatikan {@code SopUtil.hapusDisposisi} di
 *       handler hapus agar disposisi SOP ikut terhapus sebelum entitas utama.</li>
 *   <li>Pola {@link #pastikanSessionTerbuka} diperlukan selama callee seperti
 *       {@code hitungDibayar} dan {@code generateCode} masih berpotensi menutup
 *       session thread-local; jangan dihapus tanpa investigasi menyeluruh.</li>
 *   <li>Gunakan {@link #generateKodeUnik} (bukan {@link #generateCode} langsung)
 *       di {@link #onSave} untuk menghindari kode duplikat saat banyak pengguna
 *       menyimpan serentak.</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 * @see PembayaranTerminMasterAsset
 * @see PembayaranTerminMasterAssetDetail
 * @see PembayaranTerminMasterAssetHelper
 */
public class PembayaranTerminMasterAssetAction extends GenericAutowireComposer implements FormSop, DataCriteria {
	protected org.zkoss.zul.Tabpanel tabDasborBayar;

	/**
	 * <b>Tujuan:</b> Memuat dasbor "Pembayaran Vendor" ke dalam tab pertama.
	 *
	 * <b>Cara kerja:</b> Lazy initialization seperti tab dasbor lain pada modul ini --
	 * komponen hanya dibuat sekali, diperiksa lewat {@code getChildren().size() == 0}.
	 * Dipicu dua kali jalur: {@code onCreate} pada tabpanel (karena tab pertama yang
	 * terpilih tidak pernah menerima onClick) dan {@code onClick} pada tab-nya.
	 *
	 * <b>Sumber angka:</b> {@code PengadaanTahapDashboard} memanggil aksi yang sama
	 * dengan dasbor POS, sehingga angka di ZKoss dan di POS tidak mungkin berbeda.
	 *
	 * <b>Pemeliharaan:</b> pastikan field {@code tabDasborBayar} terwire di ZUL
	 * lewat {@code id="tabDasborBayar"} pada elemen {@code <tabpanel>}.
	 *
	 * @param event event ZK pemicu; tidak dipakai langsung
	 */
	public void onDasborBayar(Event event) {
		if (tabDasborBayar == null) {
			return;
		}
		if (tabDasborBayar.getChildren().size() == 0) {
			ais.action.master.asset.helper.PengadaanTahapDashboard dashboard =
					new ais.action.master.asset.helper.PengadaanTahapDashboard("dpc");
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(tabDasborBayar);
		}
	}


	/**
	 * ID serialisasi untuk kompatibilitas antara versi kelas saat deserialisasi
	 * objek yang disimpan (misalnya di sesi HTTP yang di-persist).
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal yang digunakan untuk form tambah dan ubah data pembayaran termin. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar pembayaran termin aset. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid utama. */
	private Paging paging;

	/** Input teks pencarian berdasarkan kode pembayaran termin. */
	private Textbox searchkode;

	/** Input teks pencarian berdasarkan nama atau kode penyedia aset. */
	private Textbox searchpenyedia;

	/** Input teks pencarian berdasarkan keterangan pembayaran termin. */
	private Textbox searchketerangan;

	/** Filter tanggal awal periode pencarian. */
	private MyDatebox start;

	/** Filter tanggal akhir periode pencarian. */
	private MyDatebox end;

	/** Combo box untuk memfilter status pelunasan (Semua/Lunas/Belum Lunas). */
	private Combobox searchLunas;

	/** Label yang menampilkan kode pembayaran termin pada form input. */
	private Label kode;

	/** Input teks untuk keterangan pembayaran termin pada form. */
	private MyTextbox keterangan;

	/** Datebox untuk tanggal pembuatan dokumen pembayaran termin. */
	private MyDatebox tanggalPembuatan;

	/** Flag apakah pengguna saat ini memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna saat ini memiliki hak hapus (DELETE). */
	private boolean delete = false;

	/** Flag apakah pengguna saat ini memiliki hak setuju (APPROVE). */
	private boolean approve = false;

	/** Flag apakah pengguna saat ini memiliki hak tolak/batalkan (REJECT). */
	private boolean reject = false;

	/** Entitas pembayaran termin yang sedang diedit atau ditambahkan. */
	private PembayaranTerminMasterAsset pembayaranTerminMasterAsset;

	/** Tombol toolbar untuk menambah data baru. */
	private MyToolbarbuttonConfig add;

	/** Grid yang menampilkan daftar detail (pesanan pengadaan) di form. */
	private MyGrid gridMasterAsset;

	/** Disposisi SOP yang terkait dengan pembayaran termin yang sedang diedit. */
	private DisposisiSop disposisiSop = null;

	/**
	 * Flag mode persetujuan: bila {@code true}, form digunakan untuk menyetujui
	 * pembayaran termin (bukan mode edit biasa).
	 */
	private boolean persetujuan = false;

	/** Komponen banbox untuk memilih penyedia aset pada form. */
	private AmbilDataPenyediaAssetBanbox penyediaAsset;

	/** Helper yang mengelola tampilan dan logika grid detail pembayaran termin. */
	private PembayaranTerminMasterAssetHelper pembayaranTerminMasterAssetHelper;

	/** Combo box untuk memilih jenis pembayaran barang pada form. */
	private Combobox jenisPembayaranBarang;

	/** Lampiran dokumen tagihan pertama yang diunggah pengguna. */
	protected LampiranLain lainMahasiswa;

	/** Lampiran dokumen tagihan kedua yang diunggah pengguna. */
	protected LampiranLain lainMahasiswa2;

	/** Lampiran dokumen tagihan ketiga yang diunggah pengguna. */
	protected LampiranLain lainMahasiswa3;

	/** Lampiran dokumen tagihan keempat yang diunggah pengguna. */
	private LampiranLain lainMahasiswa4;

	/**
	 * Checkbox filter untuk menampilkan pembayaran yang belum disetujui.
	 * Dipakai bersama {@link #disetujui} untuk menyaring status persetujuan.
	 */
	private MyCheckboxConfig blmDisetujui;

	/**
	 * Checkbox filter untuk menampilkan pembayaran yang sudah disetujui.
	 * Dipakai bersama {@link #blmDisetujui} untuk menyaring status persetujuan.
	 */
	private MyCheckboxConfig disetujui;

	/**
	 * <b>Tujuan:</b> Konstruktor default tanpa argumen. Digunakan oleh ZK
	 * framework saat instansiasi otomatis dari ZUL file.
	 *
	 * <p><b>Cara kerja:</b> Tidak melakukan inisialisasi tambahan; semua
	 * inisialisasi dilakukan di {@link #doAfterCompose}. Field {@code persetujuan}
	 * secara default bernilai {@code false}.
	 *
	 * <p><b>Pemeliharaan:</b> Jangan tambahkan logika di sini karena ZK
	 * framework belum menyuntikkan komponen saat konstruktor dipanggil.
	 */
	public PembayaranTerminMasterAssetAction() {

	}

	/**
	 * <b>Tujuan:</b> Konstruktor yang digunakan saat kelas ini dipanggil secara
	 * programatik dalam mode persetujuan, misalnya dari alur SOP atau halaman
	 * khusus persetujuan pembayaran vendor.
	 *
	 * <p><b>Cara kerja:</b> Menetapkan flag {@link #persetujuan} ke nilai yang
	 * diberikan. Bila {@code persetujuan} bernilai {@code true}, beberapa bagian
	 * form (penyedia, kode, jenis pembayaran) ditampilkan hanya-baca menggunakan
	 * {@link Label} alih-alih komponen input. Selain itu, saat simpan,
	 * {@code disetujuiOleh} dan {@code tanggalPersetujuan} otomatis diisi.
	 *
	 * <p><b>Parameter:</b>
	 * @param persetujuan {@code true} bila action ini digunakan dalam konteks
	 *                    persetujuan pembayaran, {@code false} untuk mode normal.
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan ZUL yang menggunakan konstruktor ini
	 * meneruskan parameter dengan benar via {@code <apply class="...">} atau
	 * instansiasi manual.
	 */
	public PembayaranTerminMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan:</b> Metode yang dipanggil oleh ZK framework sebelum halaman ZUL
	 * mulai dikompilasi. Digunakan untuk melakukan pemeriksaan keamanan awal
	 * sebelum komponen apa pun dirender.
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang
	 * memeriksa apakah pengguna memiliki hak akses ke halaman ini berdasarkan
	 * konfigurasi privilege. Jika tidak memiliki akses, pengguna akan diarahkan
	 * ke halaman logoff. Setelah itu, implementasi super dipanggil untuk
	 * melanjutkan proses compose normal.
	 *
	 * <p><b>Parameter:</b>
	 * @param page     halaman ZK tempat komponen akan dimuat
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen yang akan dikompilasi
	 * @return {@code ComponentInfo} dari implementasi super untuk dilanjutkan
	 *         ke proses compose berikutnya
	 *
	 * <p><b>Penanganan error:</b> Bila pengguna tidak memiliki akses,
	 * {@code doCheckSecurity} melempar redirect sehingga metode ini tidak
	 * mengembalikan nilai.
	 *
	 * <p><b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code doCheckSecurity}
	 * karena ini merupakan garis pertahanan pertama terhadap akses tidak sah.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dipanggil ZK framework setelah
	 * seluruh komponen ZUL berhasil di-wire ke field-field kelas ini. Di sinilah
	 * semua logika inisialisasi halaman dijalankan: pemeriksaan sesi, pengaturan
	 * hak akses, pengisian filter default, dan pemuatan data awal.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa atribut sesi {@code usersTemp} dan hak baca pengguna.
	 *       Bila tidak valid, sesi dibersihkan dan pengguna diarahkan ke logoff.</li>
	 *   <li>Menetapkan visibilitas tombol tambah berdasarkan hak CREATE.</li>
	 *   <li>Menyimpan hak UPDATE, DELETE, APPROVE, REJECT ke field boolean kelas.</li>
	 *   <li>Mengatur filter tanggal default: mulai = 6 bulan lalu, akhir = besok.</li>
	 *   <li>Mengisi combo status lunas dengan tiga pilihan: Semua, Lunas, Belum Lunas.</li>
	 *   <li>Menambahkan tombol cetak ke toolbar via {@code Common.cetakData}.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil ulang pencarian.</li>
	 *   <li>Membuat timer default untuk memuat data pertama kali.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param comp komponen root dari halaman ZUL yang baru saja selesai dikompilasi
	 * @throws Exception bila terjadi kesalahan saat inisialisasi komponen atau
	 *                   pemanggilan super
	 *
	 * <p><b>Penanganan error:</b> Bila sesi tidak valid, metode langsung return
	 * setelah memanggil {@code Common.goLogoff()} tanpa melempar exception.
	 *
	 * <p><b>Pemeliharaan:</b> Bila ada kolom baru yang perlu diekspor, tambahkan
	 * nama properti ke array {@code contents}. Pastikan urutan nama properti
	 * sesuai dengan getter di {@link PembayaranTerminMasterAsset}.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
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
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Comboitem comboitem = new Comboitem("Semua");
		searchLunas.appendChild(comboitem);
		comboitem = new Comboitem("Lunas");
		if (comboitem != null) { comboitem.setValue(1); }
		searchLunas.appendChild(comboitem);
		comboitem = new Comboitem("Belum Lunas");
		if (comboitem != null) { comboitem.setValue(2); }
		searchLunas.appendChild(comboitem);
		if (searchLunas != null) { searchLunas.setReadonly(true); }
		if (searchLunas != null) { searchLunas.setSelectedIndex(0); }

		String[] contents = new String[] { "kode", "penyedia", "tanggalPembuatan", "tanggalPersetujuan", "dibuatOleh",
				"disetujuiOleh", "nilaiDibayar", "tahun", "bulan", "disposisiSop", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PembayaranTerminMasterAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

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
	 * <b>Tujuan:</b> Kelas renderer dalam (inner class) yang bertanggung jawab
	 * menggambar setiap baris pada grid utama daftar pembayaran termin aset.
	 * Kelas ini mengimplementasikan {@code MyRowRenderer} dari framework AIS.
	 *
	 * <p><b>Cara kerja:</b> Metode {@link #render} menerima entitas
	 * {@link PembayaranTerminMasterAsset} dan mengisi sel-sel baris grid dengan:
	 * <ol>
	 *   <li>Vbox revisi + tautan unduh/unggah bukti pembayaran (LampiranLain).</li>
	 *   <li>Daftar pesanan pengadaan terkait (kode + keterangan + tautan tagihan).</li>
	 *   <li>Nama penyedia aset.</li>
	 *   <li>Nilai dibayar dalam format angka terformat.</li>
	 *   <li>Jenis pembayaran barang atau label "Daftar Pengajuan Transfer".</li>
	 *   <li>Pembuat (nama + tanggal pembuatan).</li>
	 *   <li>Penyetuju (nama + tanggal persetujuan), diperbarui saat aksi dilakukan.</li>
	 *   <li>Keterangan + tautan alur SOP bila ada.</li>
	 *   <li>Status aktif: checkbox bila belum disetujui dan punya hak edit,
	 *       label teks bila sudah disetujui atau tidak aktif.</li>
	 *   <li>Toolbar aksi: Cetak, Setujui, Batalkan, Ubah, Hapus.</li>
	 * </ol>
	 *
	 * <p><b>Threading:</b> Berjalan di event thread ZK. Akses database melalui
	 * {@code HibernateUtil.currentSession()} pada session thread-local.
	 *
	 * <p><b>Pemeliharaan:</b> Bila kolom baru ditambahkan ke ZUL, tambahkan
	 * juga sel yang sesuai di sini agar jumlah sel sesuai dengan jumlah kolom.
	 */
	class PembayaranTerminMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi satu baris grid dengan data satu entitas
		 * {@link PembayaranTerminMasterAsset} beserta semua tombol aksi yang
		 * relevan berdasarkan status dan hak akses pengguna.
		 *
		 * <p><b>Cara kerja:</b> Mengambil entitas dari parameter {@code arg1},
		 * kemudian membuat komponen ZK secara programatik (Label, Vbox, Hbox, A,
		 * MyToolbarbuttonConfig) dan menyambungkannya ke baris {@code arg0}.
		 * Tombol Setujui hanya tampil bila pengguna punya hak APPROVE dan pembayaran
		 * belum disetujui. Tombol Batalkan hanya tampil bila ada hak REJECT dan
		 * pembayaran sudah disetujui. Tombol Ubah/Hapus hanya tampil sebelum
		 * persetujuan. Setiap aksi tombol memperbarui label penyetuju/tanggal
		 * secara langsung di UI tanpa reload penuh grid.
		 *
		 * <p><b>Parameter:</b>
		 * @param arg0 baris {@link Row} ZK yang akan diisi komponen
		 * @param arg1 objek data bertipe {@link PembayaranTerminMasterAsset}
		 * @throws Exception bila terjadi kesalahan saat membuat atau menyambungkan
		 *                   komponen ZK, atau saat mengakses database
		 *
		 * <p><b>Penanganan error:</b> Exception saat hapus data ditangkap dan
		 * ditampilkan via {@code MyMessageboxConfig} dengan pesan yang informatif.
		 *
		 * <p><b>Pemeliharaan:</b> Referensi lokal {@code disetujuiOleh} dan
		 * {@code disetujuiTanggal} menggunakan {@code final} agar dapat diakses
		 * dari anonymous listener; pastikan pola ini dipertahankan bila refaktor.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranTerminMasterAsset pembayaranTerminMasterAsset = (PembayaranTerminMasterAsset) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PembayaranTerminMasterAsset.class, pembayaranTerminMasterAsset,
					pembayaranTerminMasterAsset.getKode())).setParent(arg0);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pembayaranTerminMasterAsset.getId(),
					PembayaranTerminMasterAsset.class.getName(), "Bukti Pembayaran", false, null, null, false, false,
					false, false);

			Session session = HibernateUtil.currentSession();
			List<PembayaranTerminMasterAssetDetail> pembayaranTerminMasterAssetDetails = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranTerminMasterAsset", pembayaranTerminMasterAsset)).list();
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {

				Vbox dataInvoice = new Vbox();
				vbox.appendChild(dataInvoice);
				dataInvoice.appendChild(
						new Label(pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getKode()));
				dataInvoice.appendChild(new Space());
				dataInvoice.appendChild(new Label(
						pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getKeterangan()));

				Hbox hboxa = new Hbox();
				hboxa.setParent(vbox);
				LampiranLain.createDownloadUploadFileLain(hboxa,
						pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getId(),
						PemesananPengadaanMasterAsset.class.getName(), "Tagihan", false, null, null, false, false,
						false, false);
			}

			new Label(pembayaranTerminMasterAsset.getPenyedia() == null ? ""
					: pembayaranTerminMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(pembayaranTerminMasterAsset.getNilaiDibayar())).setParent(arg0);

			new Label(pembayaranTerminMasterAsset.getJenisPembayaranBarang() == null
					? Common.getBahasaConfig("Daftar Pengajuan Transfer")
					: pembayaranTerminMasterAsset.getJenisPembayaranBarang().getNama()).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pembayaranTerminMasterAsset.getDibuatOleh() == null ? ""
					: pembayaranTerminMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(pembayaranTerminMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranTerminMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(pembayaranTerminMasterAsset.getDisetujuiOleh() == null ? ""
					: pembayaranTerminMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(pembayaranTerminMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranTerminMasterAsset.getTanggalPersetujuan())))
					.setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pembayaranTerminMasterAsset.getKeterangan())).setParent(vbox1);
			if (pembayaranTerminMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pembayaranTerminMasterAsset.getDisposisiSop().getKeterangan()
						+ " (" + pembayaranTerminMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranTerminMasterAsset.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			if (pembayaranTerminMasterAsset.getDisposisiSop() != null
					&& !pembayaranTerminMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (pembayaranTerminMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pembayaranTerminMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pembayaranTerminMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pembayaranTerminMasterAsset);
					}
				});
			} else {
				new Label(pembayaranTerminMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pembayaran Termin Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pembayaranTerminMasterAsset);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pembayaranTerminMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Pembayaran ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranTerminMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										pembayaranTerminMasterAsset
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pembayaranTerminMasterAsset);

										// SINKRON LANGSUNG ke DPC saat disetujui — tanpa harus buka ulang menu /
										// re-save. Tombol "Setujui" ini sebelumnya TIDAK membuat DPT (beda dgn
										// onSave), dan termin tak punya renderer-net, sehingga tagihan yang
										// disetujui via tombol ini tak pernah muncul di ProsesTransfer. Di sini,
										// meniru onSave: untuk tiap detail termin buat DPT vendor + baris PPh.
										// Di dalam timer & muat ulang per-id karena Pajak.buatDariTermin memakai
										// lalu MENUTUP currentNativeSession tiap iterasi (pastikanSessionTerbuka
										// mengambil ulang). Idempoten: simpan* skip bila DPT sudah ada.
										final Long ptmaId = pembayaranTerminMasterAsset.getId();
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												if (ptmaId == null) {
													return;
												}
												Session s = pastikanSessionTerbuka(null);
												PembayaranTerminMasterAsset ptma = (PembayaranTerminMasterAsset) s
														.get(PembayaranTerminMasterAsset.class, ptmaId);
												if (ptma == null || ptma.getDisetujuiOleh() == null) {
													return;
												}
												boolean jpbNull = ptma.getJenisPembayaranBarang() == null;
												java.util.List<?> idRows = s
														.createCriteria(PembayaranTerminMasterAssetDetail.class)
														.add(Restrictions.eq("pembayaranTerminMasterAsset", ptma))
														.setProjection(Projections.property("id")).list();
												java.util.List<Long> ids = new java.util.ArrayList<Long>();
												for (Object o : idRows) {
													if (o != null) {
														ids.add(Long.valueOf(o.toString()));
													}
												}
												for (Long detId : ids) {
													s = pastikanSessionTerbuka(s);
													PembayaranTerminMasterAssetDetail det = (PembayaranTerminMasterAssetDetail) s
															.get(PembayaranTerminMasterAssetDetail.class, detId);
													if (det == null) {
														continue;
													}
													if (jpbNull || det.getDaftarPengajuanTransfer() == null) {
														DaftarPengajuanTransfer
																.simpanPembayaranTerminMasterAssetDetail(det, s);
													}
													ais.database.model.akunting.Pajak.buatDariTermin(det);
												}
											}
										});

										disetujuiTanggal.setValue(
												pembayaranTerminMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pembayaranTerminMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh
												.setValue(pembayaranTerminMasterAsset.getDisetujuiOleh() == null ? ""
														: pembayaranTerminMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pembayaranTerminMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);

										cetak(pembayaranTerminMasterAsset);
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Pembayaran Termin Aset ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranTerminMasterAsset.setDisetujuiOleh(null);
										pembayaranTerminMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, pembayaranTerminMasterAsset);

										disetujuiTanggal.setValue(
												pembayaranTerminMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pembayaranTerminMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh
												.setValue(pembayaranTerminMasterAsset.getDisetujuiOleh() == null ? ""
														: pembayaranTerminMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pembayaranTerminMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);

									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembayaranTerminMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pembayaranTerminMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											if (SopUtil.hapusDisposisi(session,
													pembayaranTerminMasterAsset.getDisposisiSop())) {

												List<PembayaranTerminMasterAssetDetail> pembayaranTerminMasterAssetDetails = session
														.createCriteria(PembayaranTerminMasterAssetDetail.class)
														.add(Restrictions.eq("pembayaranTerminMasterAsset",
																pembayaranTerminMasterAsset))
														.list();
												for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {
													session.delete(pembayaranTerminMasterAssetDetail);
												}
												session.flush();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, pembayaranTerminMasterAsset);
														session.flush();
														onSearchDefault(arg0);
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
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" di toolbar utama.
	 * Membuka jendela modal form untuk membuat pembayaran termin baru.
	 *
	 * <p><b>Cara kerja:</b> Membuat instans {@link PembayaranTerminMasterAsset}
	 * baru (kosong) dan meneruskannya ke {@link #init} untuk membangun form.
	 * Setelah form siap, {@link MyWindow#onModal()} dipanggil untuk
	 * menampilkan jendela dalam mode modal.
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK dari tombol Tambah (tidak digunakan secara langsung)
	 * @throws Exception bila terjadi kesalahan saat membangun form
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini adalah delegator tipis ke {@link #init};
	 * logika sebenarnya ada di sana.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PembayaranTerminMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form input pembayaran termin
	 * di dalam jendela modal {@link #addWindow}. Digunakan baik untuk tambah
	 * data baru maupun ubah data yang sudah ada.
	 *
	 * <p><b>Cara kerja:</b> Membersihkan konten {@link #addWindow} lalu
	 * membangun struktur layout {@link Borderlayout} dengan {@link Center}
	 * berisi form (dari {@link #form}) dan {@link South} berisi toolbar
	 * dengan tombol Batal dan Simpan. Listener tombol Simpan memanggil
	 * {@link #onSave}, lalu bila berhasil, memuat ulang grid dan menutup
	 * jendela. Listener tombol Batal hanya menyembunyikan jendela.
	 *
	 * <p><b>Parameter:</b>
	 * @param pembayaranTerminMasterAsset entitas yang akan diedit; bila ID-nya
	 *        null maka ini adalah data baru, bila sudah ada ID maka mode ubah
	 * @throws Exception bila terjadi kesalahan saat membangun komponen ZK
	 *                   atau memanggil {@link #form}
	 *
	 * <p><b>Penanganan error:</b> Exception diteruskan ke pemanggil (onAdd atau
	 * handler ubah di renderer).
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan {@code disposisiSop} di-reset ke null
	 * sebelum memanggil {@link #form} agar form baru tidak mewarisi disposisi
	 * dari sesi edit sebelumnya.
	 */
	private void init(PembayaranTerminMasterAsset pembayaranTerminMasterAsset) throws Exception {
		this.pembayaranTerminMasterAsset = pembayaranTerminMasterAsset;
		addWindow.setTitle("Proses Pembayaran Barang/Jasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(pembayaranTerminMasterAsset, disposisiSop, save, null));

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
		borderlayout.setParent(addWindow);

	}

	/**
	 * <b>Tujuan:</b> Memastikan session Hibernate thread-local masih terbuka dan
	 * dapat digunakan untuk operasi transaksi selanjutnya. Merupakan penjaga
	 * keamanan untuk mencegah kesalahan "Session is closed!" yang bisa terjadi
	 * ketika callee (seperti {@code generateCode} atau {@code hitungDibayar})
	 * menutup session di tengah proses penyimpanan.
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah session yang diberikan adalah null
	 * atau sudah tertutup ({@code !session.isOpen()}). Bila kondisi tersebut
	 * terpenuhi, metode membuka session baru menggunakan
	 * {@code HibernateUtil.currentNativeSession()} dan mengembalikannya.
	 * Bila session masih terbuka, mengembalikan session yang sama tanpa perubahan.
	 *
	 * <p><b>Parameter:</b>
	 * @param session session Hibernate yang akan diperiksa; boleh null
	 * @return session Hibernate yang dijamin terbuka; tidak pernah null
	 *
	 * <p><b>Return:</b> Bila session masih valid, mengembalikan parameter
	 * {@code session} itu sendiri. Bila tidak, mengembalikan session baru
	 * dari {@code HibernateUtil.currentNativeSession()}.
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception. Bila
	 * {@code currentNativeSession()} gagal, exception akan merembet ke pemanggil.
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini diperlukan selama ada callee yang
	 * berpotensi menutup session. Bila suatu saat seluruh callee direfaktor
	 * untuk tidak menutup session, metode ini bisa dihapus.
	 */
	private Session pastikanSessionTerbuka(Session session) {
		if (session == null || !session.isOpen()) {
			return HibernateUtil.currentNativeSession();
		}
		return session;
	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input pengguna dan menyimpan data pembayaran
	 * termin beserta semua detail-nya ke database. Ini adalah metode inti
	 * penyimpanan yang mencakup validasi, penyimpanan header, penyimpanan detail,
	 * perhitungan ulang saldo dibayar pada pesanan pengadaan, pembuatan daftar
	 * pengajuan transfer, dan pembaruan referensi lampiran dokumen.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi: penyedia harus dipilih, kode tidak boleh kosong, minimal
	 *       satu riwayat pembayaran harus dimasukkan.</li>
	 *   <li>Menghitung total nilai dibayar dari semua detail yang dicentang.</li>
	 *   <li>Bila mode ubah (ID sudah ada), muat ulang entitas dari database
	 *       menggunakan {@code session.load} untuk memastikan state terkini.</li>
	 *   <li>Menetapkan semua field header dari input form.</li>
	 *   <li>Bila mode persetujuan, otomatis mengisi {@code disetujuiOleh}
	 *       dan {@code tanggalPersetujuan}.</li>
	 *   <li>Menyimpan header dengan {@code session.update} atau {@code session.save}
	 *       dengan proteksi {@link #pastikanSessionTerbuka}.</li>
	 *   <li>Untuk setiap detail yang dicentang: simpan/perbarui detail,
	 *       hitung ulang total dibayar pada pesanan pengadaan terkait,
	 *       dan bila perlu buat entri {@link DaftarPengajuanTransfer}.</li>
	 *   <li>Setelah commit selesai, jadwalkan timer untuk memperbarui referensi
	 *       lampiran (empat slot dokumen) menggunakan session streaming Hibernate
	 *       yang terpisah agar tidak mencemari transaksi utama.</li>
	 *   <li>Setelah lampiran diperbarui, jadwalkan timer kedua untuk mencetak
	 *       dokumen pembayaran termin secara otomatis.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK dari tombol Simpan (tidak digunakan secara langsung)
	 * @return {@code true} bila penyimpanan berhasil dan form dapat ditutup;
	 *         {@code false} bila validasi gagal (form tetap terbuka)
	 * @throws Exception bila terjadi kesalahan tidak terduga saat akses database
	 *
	 * <p><b>Penanganan error:</b> Validasi yang gagal menampilkan pesan peringatan
	 * via {@code MyMessageboxConfig} dan mengembalikan {@code false}. Kesalahan
	 * saat memperbarui lampiran ditangkap per-slot dan ditampilkan via
	 * {@code Common.tampilErrorJikaAdmin} tanpa menggagalkan keseluruhan proses.
	 *
	 * <p><b>Pemeliharaan:</b>
	 * <ul>
	 *   <li>Selalu gunakan {@link #pastikanSessionTerbuka} sebelum setiap
	 *       {@code getTransaction().begin()} dalam loop.</li>
	 *   <li>Gunakan varian {@code hitungDibayar(session)} bukan varian tanpa
	 *       argumen untuk mencegah session ditutup di tengah loop.</li>
	 *   <li>Gunakan varian {@code DaftarPengajuanTransfer.simpanPembayaranTerminMasterAssetDetail(detail, session)}
	 *       bukan varian tanpa session.</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (penyediaAsset.getAttribute("penyediaAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Penyedia dan cari dari daftar penyedia terdaftar; (2) Jika penyedia belum ada, daftarkan melalui menu Data Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Pembayaran Termin belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Pembayaran atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Double d = 0.0;
		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		Double nilai = 0.0;
		for (Row row : rowsMasterAsset) {

			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			if (pilih.isChecked()) {
				PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail = (PembayaranTerminMasterAssetDetail) row
						.getAttribute("pembayaranTerminMasterAssetDetail");
				if (pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset() == null) {
					MyMessageboxConfig.show("Mohon maaf, Data Tagihan Termin pada baris yang dipilih belum terisi. Langkah yang dapat dilakukan: (1) Pastikan setiap baris yang dicentang sudah memiliki data pemesanan/tagihan; (2) Hapus baris kosong atau isi dengan data yang sesuai; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranTerminMasterAssetDetail
						.getPemesananPengadaanMasterAsset();

				Double telahDibayar = pembayaranTerminMasterAssetDetail.getDibayar();

				nilai += pemesananPengadaanMasterAsset.getNilai();
				d += telahDibayar;
			}
		}

		if (d.intValue() == 0) {
			MyMessageboxConfig.show("Mohon maaf, jumlah riwayat pembayaran termin belum diisi. Langkah yang dapat dilakukan: (1) Isi nilai pembayaran pada field yang tersedia; (2) Centang minimal satu tagihan dan isi nilai pembayarannya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentNativeSession();
		if (pembayaranTerminMasterAsset.getId() != null) {
			pembayaranTerminMasterAsset = (PembayaranTerminMasterAsset) session.load(PembayaranTerminMasterAsset.class,
					pembayaranTerminMasterAsset.getId());

		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pembayaranTerminMasterAsset.setDisposisiSop(disposisiSop);
		}

		pembayaranTerminMasterAsset.setJenisPembayaranBarang(
				(JenisPembayaranBarang) (jenisPembayaranBarang.getSelectedItem() == null ? null
						: jenisPembayaranBarang.getSelectedItem().getValue()));
		pembayaranTerminMasterAsset.setNilaiDibayar(d);
		pembayaranTerminMasterAsset.setKode(kode.getValue());
		String ketValue = keterangan.getValue();
		pembayaranTerminMasterAsset.setKeterangan(ketValue != null && ketValue.length() > 255
				? ketValue.substring(0, 255) : ketValue);
		pembayaranTerminMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
		pembayaranTerminMasterAsset.setPenyedia((PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset"));

		if (persetujuan) {
			pembayaranTerminMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
			pembayaranTerminMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		}

		if (pembayaranTerminMasterAsset.getId() != null) {
			session = pastikanSessionTerbuka(session);
			session.getTransaction().begin();
			session.update(pembayaranTerminMasterAsset);
			session.getTransaction().commit();
		} else {
			pembayaranTerminMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateKodeUnik();
			kode.setValue(noAgenda);
			pembayaranTerminMasterAsset.setKode(kode.getValue());
			/* generateCode() di atas dapat memakai/menutup session thread-local,
			 * sehingga begin() di bawah bisa gagal "Session is closed!". */
			session = pastikanSessionTerbuka(session);
			session.getTransaction().begin();
			session.save(pembayaranTerminMasterAsset);
			session.getTransaction().commit();
		}

		for (Row row : rowsMasterAsset) {
			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			/* Penjaga: bila ada callee yang menutup session thread-local di
			 * iterasi sebelumnya, ambil ulang agar getTransaction() tidak
			 * gagal "Session is closed". */
			session = pastikanSessionTerbuka(session);

			if (pilih.isChecked()) {
				PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail = (PembayaranTerminMasterAssetDetail) row
						.getAttribute("pembayaranTerminMasterAssetDetail");
				pembayaranTerminMasterAssetDetail.setPembayaranTerminMasterAsset(pembayaranTerminMasterAsset);

				session.getTransaction().begin();
				if (pembayaranTerminMasterAssetDetail.getId() == null) {
					session.save(pembayaranTerminMasterAssetDetail);
				} else {
					Common.refreshUpdate(session, pembayaranTerminMasterAssetDetail);
				}
				session.getTransaction().commit();

				PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranTerminMasterAssetDetail
						.getPemesananPengadaanMasterAsset();
				if (pemesananPengadaanMasterAsset != null && pemesananPengadaanMasterAsset.getId() != null) {
					Number nilaiTagihan = (Number) session.createCriteria(PembayaranTerminMasterAssetDetail.class)
							.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
							.setProjection(Projections.sum("dibayar")).uniqueResult();

					pembayaranTerminMasterAssetDetail
							.setTotalTelahDibayar(nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue());

					PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset1 = (PemesananPengadaanMasterAsset) session
							.createCriteria(PemesananPengadaanMasterAsset.class)
							.add(Restrictions.idEq(pemesananPengadaanMasterAsset.getId())).uniqueResult();

					/* Wajib varian (session): varian tanpa argumen menutup
					 * session thread-local sehingga begin()/commit() di bawah
					 * gagal "Session is closed". */
					Double dibayar = pemesananPengadaanMasterAsset1.hitungDibayar(session);

					pemesananPengadaanMasterAsset1.setDibayar(dibayar);

					session = pastikanSessionTerbuka(session);
					session.getTransaction().begin();
					Common.refreshUpdate(session, pemesananPengadaanMasterAsset1);
					Common.refreshUpdate(session, pembayaranTerminMasterAssetDetail);
					session.getTransaction().commit();
				}
				// PENARIKAN DATA TERMIN KE DPC = HANYA SETELAH DISETUJUI. Sejalan dengan jalur
				// vendor SaldoAwal (DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset) yang menolak
				// membuat DPT sebelum disetujui. Tanpa gate ini, termin yang baru DISIMPAN (belum
				// disetujui) sudah tertarik ke DPC & bisa dipilih ke Proses Transfer. getDisetujuiOleh()
				// bernilai non-null bila disetujui langsung ATAU via disposisi SOP.
				if (pembayaranTerminMasterAsset.getDisetujuiOleh() != null) {
					if (pembayaranTerminMasterAsset.getJenisPembayaranBarang() == null
							|| pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer() == null) {
						/* Wajib varian (detail, session): varian tanpa session menutup
						 * session thread-local yang sama sehingga iterasi/loop
						 * berikutnya gagal "Session is closed". */
						DaftarPengajuanTransfer.simpanPembayaranTerminMasterAssetDetail(pembayaranTerminMasterAssetDetail,
								session);
					}

					// Buat/refresh baris pembayaran PPh termin di DPC (bila detail punya JenisPajakBarang);
					// vendor dibayar netto (dibayar - PPh) + baris PPh terpisah. buatDariTermin memakai
					// session dedikasi + currentNativeSession; iterasi berikut mengambil ulang session
					// (pastikanSessionTerbuka) sehingga aman.
					ais.database.model.akunting.Pajak.buatDariTermin(pembayaranTerminMasterAssetDetail);
				}
			}
		}

		ais.common.Common.closeOpenedSession(session);

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa);
						lainMahasiswa.setRef(pembayaranTerminMasterAsset.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (lainMahasiswa2 != null && lainMahasiswa2.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa2);
						lainMahasiswa2.setRef(pembayaranTerminMasterAsset.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa2);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (lainMahasiswa3 != null && lainMahasiswa3.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa3);
						lainMahasiswa3.setRef(pembayaranTerminMasterAsset.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa3);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (lainMahasiswa4 != null && lainMahasiswa4.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa4);
						lainMahasiswa4.setRef(pembayaranTerminMasterAsset.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa4);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(pembayaranTerminMasterAsset);
					}
				}, "Proses cetak", false, 2500);

			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Merakit semua parameter yang dibutuhkan untuk mencetak
	 * laporan PDF pembayaran termin dalam bentuk {@link Map}. Parameter ini
	 * kemudian diteruskan ke mesin laporan JasperReports melalui
	 * {@code Report.generateFileReport} atau {@code Report.generatePDFReport}.
	 *
	 * <p><b>Cara kerja:</b> Memuat ulang entitas dari database bila ID sudah ada
	 * (untuk memastikan data terkini). Kemudian mengisi parameter dengan:
	 * <ul>
	 *   <li>ID dan semua properti entitas via {@code Common.insertProperty}.</li>
	 *   <li>Data bank penyedia dari JSON array di field {@code bank} penyedia,
	 *       dengan key berformat {@code bank_i.namaField}.</li>
	 *   <li>Status dokumen vendor ({@link PenyediaAssetPunyaDokumen}) per jenis
	 *       dokumen yang terdaftar di {@code ConstantValues}.</li>
	 *   <li>Parameter alur SOP dari {@code DisposisiAlurSop.parameterMap}.</li>
	 *   <li>Daftar detail pembayaran termin (list of Map), masing-masing
	 *       menyertakan nilai tagihan, tanggal pesanan, status persetujuan,
	 *       dan data termin dari JSON field {@code tagihan}.</li>
	 *   <li>Total penagihan, pinalti, dan pekerjaan dari semua termin.</li>
	 *   <li>Pembersihan field {@code disposisiSop} dari parameter agar tidak
	 *       menyebabkan error serialisasi di JasperReports.</li>
	 * </ul>
	 *
	 * <p><b>Parameter:</b>
	 * @param pembayaranTerminMasterAsset entitas yang akan dicetak; ID-nya
	 *        digunakan untuk memuat ulang data terkini dari database
	 * @return {@link Map} berisi semua parameter laporan dalam format yang
	 *         diharapkan oleh template JasperReports {@code asset/pembayaran_termin}
	 * @throws Exception bila terjadi kesalahan saat mengakses database atau
	 *                   mengurai JSON data bank/termin
	 *
	 * <p><b>Penanganan error:</b> Parsing JSON data bank ditangkap dan diabaikan
	 * (data bank hanya informatif, tidak kritikal). Exception dari parsing
	 * parameter termin per detail juga tidak dilempar ke atas.
	 *
	 * <p><b>Pemeliharaan:</b> Bila struktur template laporan berubah, sesuaikan
	 * key parameter di sini. Perhatikan bahwa field {@code disposisiSop} di-null
	 * kan secara eksplisit di akhir untuk mencegah masalah serialisasi.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map parameter(PembayaranTerminMasterAsset pembayaranTerminMasterAsset) throws Exception {
		if (pembayaranTerminMasterAsset != null && pembayaranTerminMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(pembayaranTerminMasterAsset);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pembayaranTerminMasterAsset.getId());

		Common.insertProperty(PembayaranTerminMasterAsset.class, pembayaranTerminMasterAsset, parameters, "data");

		if (pembayaranTerminMasterAsset.getPenyedia() != null) {

			try {
				JSONArray array = new JSONArray(pembayaranTerminMasterAsset.getPenyedia().getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Iterator<String> iter = jsonObject.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						parameters.put("bank_" + i + "." + key, jsonObject.get(key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PembayaranTerminMasterAssetAction.java:1333");
				// Data bank bersifat informatif; diabaikan bila tidak tersedia atau tidak valid JSON
			}

			Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
				dokumenPenyediaAssets.add(dokumenPenyediaAsset);
			}
			PenyediaAsset penyediaAsset = pembayaranTerminMasterAsset.getPenyedia();
			Session session = HibernateUtil.currentSession();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

				PenyediaAssetPunyaDokumen temp = (PenyediaAssetPunyaDokumen) (penyediaAsset == null
						|| penyediaAsset.getId() == null
								? new PenyediaAssetPunyaDokumen()
								: session.createCriteria(PenyediaAssetPunyaDokumen.class)
										.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
										.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1)
										.uniqueResult());

				parameters.put("dokumen." + dokumenPenyediaAsset.getNama(), temp == null ? "" : temp.getKeterangan());
			}

		}

		DisposisiAlurSop.parameterMap(pembayaranTerminMasterAsset.getDisposisiSop(), parameters);
		Session session = HibernateUtil.currentSession();

		List<Map> maps = new ArrayList<Map>();

		List<PembayaranTerminMasterAssetDetail> pembayaranTerminMasterAssetDetails = session
				.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.add(Restrictions.eq("pembayaranTerminMasterAsset", pembayaranTerminMasterAsset)).list();

		for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {

			PemesananPengadaanMasterAsset penerimaanTerminMasterAsset = pembayaranTerminMasterAssetDetail
					.getPemesananPengadaanMasterAsset();
			Map map = new HashMap();
			Common.insertProperty(PembayaranTerminMasterAssetDetail.class, pembayaranTerminMasterAssetDetail, map,
					"data");
			Double nilaitagihan = penerimaanTerminMasterAsset == null ? 0.0 : penerimaanTerminMasterAsset.getDptotal();

			map.put("tagihan", nilaitagihan);
			map.put("nilai", pembayaranTerminMasterAssetDetail.getDibayar());
			map.put("tgl", penerimaanTerminMasterAsset.getTanggalPembuatan());

			map.put("status_persetujuan", pembayaranTerminMasterAsset.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Telah disetujui oleh " + pembayaranTerminMasterAsset.getDisetujuiOleh().getUserNama());

			map.put("perpustakaan", pembayaranTerminMasterAsset.getKeterangan());
			map.put("tanggal_persetujuan", pembayaranTerminMasterAsset.getTanggalPersetujuan());

			map.put("kode", pembayaranTerminMasterAsset.getKode());
			map.put("penyedia", pembayaranTerminMasterAsset.getPenyedia() == null ? ""
					: pembayaranTerminMasterAsset.getPenyedia().getNama());

			map.put("jenisPembayaranBarang",
					pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getKode() + " "
							+ pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getKeterangan());

			map.put("grup", "Data Tagihan");

			map.put("jenis_pemesanan_termin_asset", pembayaranTerminMasterAssetDetail.getKeterangan());

			JSONObject jsonObjectDataTermin = pembayaranTerminMasterAssetDetail.getTagihan() == null ? null
					: new JSONObject(pembayaranTerminMasterAssetDetail.getTagihan());

			if (jsonObjectDataTermin != null && !jsonObjectDataTermin.isNull("key")) {

				if (pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset() != null) {
					PemesananPengadaanMasterAssetAction.paramTermin(map, parameters,
							pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset(),
							jsonObjectDataTermin.get("key") + "");
				} else {
					List<Map> mapsTermin = (List<Map>) parameters.get("mapsTermin");
					if (mapsTermin == null) {
						mapsTermin = new ArrayList<Map>();
					}
					Map m = PemesananPengadaanMasterAssetAction.mapTermin(map, 0, jsonObjectDataTermin);
					mapsTermin.add(m);
					parameters.put("mapsTermin", mapsTermin);
				}
			}

			else if (pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset() != null) {
				PemesananPengadaanMasterAssetAction.paramTermin(map, parameters,
						pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset());
			}

			maps.add(map);
		}

		parameters.put("maps", maps);

		List<Map> mapsTermin = (List<Map>) parameters.get("mapsTermin");
		if (mapsTermin == null) {
			mapsTermin = new ArrayList<Map>();
		}
		Double totalpenagihan = 0.0;
		Double totalpinalti = 0.0;
		Double totalpekerjaan = 0.0;

		int i = 1;
		for (Map m : mapsTermin) {
			Double pekerjaan = (Double) (m.get("pekerjaan") == null ? 0.0 : m.get("pekerjaan"));
			Double penagihan = (Double) (m.get("penagihan") == null ? 0.0 : m.get("penagihan"));
			Double pinalti = (Double) (m.get("pinalti") == null ? 0.0 : m.get("pinalti"));

			totalpenagihan += penagihan;
			totalpinalti += pinalti;
			totalpekerjaan += pekerjaan;

			for (Object o : m.keySet()) {
				Object oo = m.get(o);
				parameters.put(o + "_ke_" + i, oo);
			}

			i++;
		}

		parameters.put("totalpenagihan", totalpenagihan);
		parameters.put("totalpinalti", totalpinalti);
		parameters.put("totalpekerjaan", totalpekerjaan);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}

		return parameters;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataCriteria.cetakData}
	 * untuk menghasilkan file PDF laporan pembayaran termin yang dapat diunduh
	 * pengguna dari tombol cetak di toolbar ekspor data.
	 *
	 * <p><b>Cara kerja:</b> Meng-cast {@link GeneralValueObject} ke
	 * {@link PembayaranTerminMasterAsset}, memanggil {@link #parameter} untuk
	 * merakit parameter laporan, kemudian menghasilkan file PDF menggunakan
	 * {@code Report.generateFileReport} dengan template
	 * {@code asset/pembayaran_termin}. File yang dihasilkan dikembalikan ke
	 * framework untuk dikirimkan sebagai respons unduhan.
	 *
	 * <p><b>Parameter:</b>
	 * @param generalValueObject entitas {@link PembayaranTerminMasterAsset}
	 *        yang akan dicetak; tidak boleh null
	 * @return {@link File} PDF yang dihasilkan oleh JasperReports
	 * @throws Exception bila terjadi kesalahan saat merakit parameter atau
	 *                   menghasilkan laporan
	 *
	 * <p><b>Pemeliharaan:</b> Template laporan berada di
	 * {@code asset/pembayaran_termin.jrxml}. Bila ada perubahan pada field
	 * yang dicetak, perbarui template tersebut dan sesuaikan {@link #parameter}.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PembayaranTerminMasterAsset pembayaranTerminMasterAsset = (PembayaranTerminMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(pembayaranTerminMasterAsset),
				"asset/pembayaran_termin", pembayaranTerminMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b> Mencetak dokumen pembayaran termin langsung ke tampilan
	 * browser pengguna sebagai PDF inline (bukan unduhan file).
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link #parameter} untuk merakit semua
	 * data laporan, kemudian meneruskannya ke {@code Report.generatePDFReport}
	 * yang mengirimkan PDF ke browser pengguna saat ini. Template laporan yang
	 * digunakan adalah {@code asset/pembayaran_termin}.
	 *
	 * <p><b>Parameter:</b>
	 * @param pembayaranTerminMasterAsset entitas yang akan dicetak; tidak boleh
	 *        null karena digunakan untuk merakit parameter dan menentukan tanggal
	 *        laporan
	 * @throws Exception bila terjadi kesalahan saat merakit parameter atau
	 *                   menghasilkan/mengirimkan PDF
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini dipanggil secara otomatis setelah
	 * simpan berhasil (via timer) dan setelah persetujuan. Bila perilaku ini
	 * tidak diinginkan, hapus pemanggilan dari {@link #onSave} dan handler
	 * disetujui di renderer.
	 */
	@SuppressWarnings({})
	private void cetak(PembayaranTerminMasterAsset pembayaranTerminMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(pembayaranTerminMasterAsset), "asset/pembayaran_termin",
				pembayaranTerminMasterAsset.getTanggalPembuatan());
	}

	/** Checkbox filter untuk menampilkan hanya data yang masih aktif pada grid utama. */
	private Checkbox searchaktif;

	/**
	 * <b>Tujuan:</b> Membangun objek {@link Criteria} Hibernate yang merepresentasikan
	 * semua kondisi filter pencarian yang aktif saat ini di halaman. Kriteria ini
	 * digunakan baik untuk menghitung total record (paging) maupun untuk mengambil
	 * halaman data yang ditampilkan di grid.
	 *
	 * <p><b>Cara kerja:</b> Membuat kriteria pada entitas
	 * {@link PembayaranTerminMasterAsset} dengan menerapkan filter-filter berikut
	 * secara berurutan:
	 * <ol>
	 *   <li>Filter status lunas: {@code nilaiDibayar > 0.1} untuk Lunas,
	 *       {@code nilaiDibayar < 0.1} untuk Belum Lunas, atau tidak ada filter.</li>
	 *   <li>Filter aktif: hanya tampil bila flag {@code aktif} true atau null
	 *       (default aktif).</li>
	 *   <li>Filter tanggal pembuatan antara {@code start} dan {@code end}
	 *       (menggunakan SQL native untuk fungsi {@code date()}).</li>
	 *   <li>Filter status persetujuan berdasarkan kombinasi checkbox
	 *       {@link #blmDisetujui} dan {@link #disetujui}: keduanya tercentang
	 *       = semua, keduanya tidak = tidak ada, hanya blmDisetujui =
	 *       isNull("disetujuiOleh"), hanya disetujui = isNotNull.</li>
	 *   <li>Join alias ke penyedia dan filter by nama/kode penyedia.</li>
	 *   <li>Filter kode pembayaran (LIKE anywhere).</li>
	 *   <li>Filter keterangan (LIKE anywhere).</li>
	 *   <li>Bila {@code order} true, tambahkan urutan menurun berdasarkan ID.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param order {@code true} bila kriteria harus menyertakan klausa ORDER BY
	 *              (untuk query pengambilan data); {@code false} untuk query
	 *              count paging
	 * @return {@link Criteria} Hibernate yang siap dieksekusi
	 *
	 * <p><b>Pemeliharaan:</b> Gunakan {@code sqlRestriction("1=1")} sebagai
	 * no-op yang aman bila filter tidak relevan. Hindari {@code sqlRestriction("true")}
	 * bila database tidak mendukung boolean literal dalam SQL WHERE.
	 */
	public Criteria initCriteria(boolean order) {

		Integer lns = (searchLunas == null || searchLunas.getSelectedItem() == null ? null
				: (Integer) searchLunas.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranTerminMasterAsset.class)

				.add(lns == null ? Restrictions.sqlRestriction("1=1")
						: lns.equals(1) ? Restrictions.gt("nilaiDibayar", 0.1) : Restrictions.lt("nilaiDibayar", 0.1))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.createAlias("penyedia", "penyedia")

				.add(searchpenyedia.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("penyedia.nama", searchpenyedia.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("penyedia.kode", searchpenyedia.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Memuat ulang data grid utama berdasarkan filter yang
	 * aktif saat ini. Dipanggil saat halaman pertama kali dimuat, saat paging
	 * berubah, setelah simpan/hapus/setujui, dan dari timer default.
	 *
	 * <p><b>Cara kerja:</b> Menginisialisasi paging dengan total record dari
	 * {@link #initCriteria} tanpa order, kemudian mengambil halaman data
	 * aktif dengan batas {@code Common.ROWS_COUNT_ON_PAGE} dan offset sesuai
	 * halaman aktif. Hasil dimasukkan ke {@link SimpleListModel} dan ditetapkan
	 * sebagai model grid dengan renderer {@link PembayaranTerminMasterAssetRenderer}.
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK pemicu (bisa null bila dipanggil programatik)
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini tidak menangani exception sendiri;
	 * exception dari query Hibernate akan merembet ke event thread ZK dan
	 * ditangani oleh error handler global.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PembayaranTerminMasterAsset> pembayaranTerminMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranTerminMasterAsset);
		grid.setRowRenderer(new PembayaranTerminMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Membangun komponen form input untuk satu entitas pembayaran
	 * termin dalam sebuah {@link MyGrid} yang kemudian dimasukkan ke dalam
	 * jendela modal. Metode ini mengimplementasikan kontrak interface
	 * {@link FormSop} dan juga digunakan oleh alur SOP untuk menampilkan form
	 * dalam konteks disposisi.
	 *
	 * <p><b>Cara kerja:</b> Membangun grid dua kolom dengan baris-baris formulir:
	 * <ol>
	 *   <li>Baris Penyedia: banbox pemilih penyedia aset, atau label nama
	 *       (bila mode persetujuan).</li>
	 *   <li>Baris Kode: label kode yang dihasilkan otomatis oleh
	 *       {@link #generateCode}, atau label teks (bila mode persetujuan).
	 *       Kode baru di-generate di sini untuk preview; kode final ditetapkan
	 *       ulang di {@link #onSave} via {@link #generateKodeUnik}.</li>
	 *   <li>Baris Tanggal: datebox tanggal pembuatan.</li>
	 *   <li>Baris Jenis Pembayaran: combo berisi daftar {@link JenisPembayaranBarang}
	 *       yang aktif, atau label (bila mode edit data yang sudah ada).</li>
	 *   <li>Baris Keterangan: textarea multi-baris.</li>
	 *   <li>Baris Detail: span dua kolom berisi grid detail pembayaran termin
	 *       dari {@link PembayaranTerminMasterAssetHelper}.</li>
	 *   <li>Baris Dokumen: empat slot unggah/unduh lampiran tagihan
	 *       (Dokumen I–IV).</li>
	 *   <li>Catatan keterangan tentang penggunaan ZIP bila lebih dari satu file.</li>
	 * </ol>
	 * Setelah form dibangun, event listener pada banbox penyedia di-wire
	 * untuk memperbarui helper detail saat penyedia dipilih.
	 *
	 * <p><b>Parameter:</b>
	 * @param generalValueObject entitas {@link PembayaranTerminMasterAsset}
	 *        yang akan ditampilkan di form; bisa berisi data existing (mode ubah)
	 *        atau entitas baru (mode tambah)
	 * @param disposisiSop disposisi SOP yang terkait (bisa null bila tidak ada SOP)
	 * @param save tombol simpan yang akan di-wire ke handler di pemanggil
	 * @param setujui listener untuk tombol setujui SOP (bisa null)
	 * @return {@link MyGrid} berisi seluruh komponen form yang siap ditampilkan
	 * @throws Exception bila terjadi kesalahan saat membangun komponen atau
	 *                   mengakses database untuk mengisi combo
	 *
	 * <p><b>Pemeliharaan:</b> Bila ditambahkan field baru ke entitas, tambahkan
	 * baris form di sini dan pastikan field tersebut juga disimpan di
	 * {@link #onSave}. Untuk field yang hanya-baca di mode persetujuan, bungkus
	 * dalam kondisi {@code if (persetujuan) { label } else { input }}.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.pembayaranTerminMasterAsset = (PembayaranTerminMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia *"));

		penyediaAsset = new AmbilDataPenyediaAssetBanbox();
		if (persetujuan) {
			row.appendChild(new Label(pembayaranTerminMasterAsset.getPenyedia() == null ? ""
					: pembayaranTerminMasterAsset.getPenyedia().getNama()));
		} else {
			row.appendChild(penyediaAsset);
		}

		penyediaAsset.setAttribute("penyediaAsset", pembayaranTerminMasterAsset.getPenyedia());
		penyediaAsset.setValue(pembayaranTerminMasterAsset.getPenyedia() == null ? ""
				: pembayaranTerminMasterAsset.getPenyedia().getNama());
		penyediaAsset.setReadonly(true);
		penyediaAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode "));

		tanggalPembuatan = new MyDatebox(
				pembayaranTerminMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pembayaranTerminMasterAsset.getTanggalPembuatan());
		if (pembayaranTerminMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			pembayaranTerminMasterAsset.setKode(noAgenda);
		}

		kode = new Label(pembayaranTerminMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pembayaranTerminMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal "));
		row.appendChild(tanggalPembuatan);
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembayaran *"));

		jenisPembayaranBarang = new Combobox();

		Common.insertComboDanSemua(jenisPembayaranBarang, new String[] { "nama", "akun" }, "keterangan",
				JenisPembayaranBarang.class, Common.getBahasaConfig("Daftar Pengajuan Transfer"),
				Restrictions.eq("aktif", true));
		jenisPembayaranBarang.setReadonly(true);
		Common.selectComboItem(jenisPembayaranBarang, pembayaranTerminMasterAsset.getJenisPembayaranBarang());

		if (pembayaranTerminMasterAsset.getId() != null) {

			row.appendChild(new Label(pembayaranTerminMasterAsset.getJenisPembayaranBarang() == null
					? Common.getBahasaConfig("Daftar Pengajuan Transfer")
					: pembayaranTerminMasterAsset.getJenisPembayaranBarang().getNama()));
		} else {
			row.appendChild(jenisPembayaranBarang);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Pembayaran"));
		row.appendChild(keterangan = new MyTextbox(pembayaranTerminMasterAsset.getKeterangan() == null ? ""
				: pembayaranTerminMasterAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild((pembayaranTerminMasterAssetHelper = new PembayaranTerminMasterAssetHelper(
				gridMasterAsset = new MyGrid())).initDetail(pembayaranTerminMasterAsset, persetujuan));

		penyediaAsset.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PenyediaAsset a = (PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset");
				if (a != null) {
					penyediaAsset.setDisabled(true);
					pembayaranTerminMasterAssetHelper.setPenyediaAsset(a);
				} else {
					penyediaAsset.setDisabled(false);
				}
			}
		});

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Tagihan"));

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pembayaranTerminMasterAsset.getId(),
				PembayaranTerminMasterAsset.class.getName(), "Dokumen I", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(vbox);

		lainMahasiswa2 = null;
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pembayaranTerminMasterAsset.getId(),
				PembayaranTerminMasterAsset.class.getName() + "_2", "Dokumen II", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa2 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(vbox);

		lainMahasiswa3 = null;
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pembayaranTerminMasterAsset.getId(),
				PembayaranTerminMasterAsset.class.getName() + "_3", "Dokumen III", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa3 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(vbox);

		lainMahasiswa4 = null;
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pembayaranTerminMasterAsset.getId(),
				PembayaranTerminMasterAsset.class.getName() + "_4", "Dokumen IV", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa4 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(vbox);

		Common.initKeterangan(rows, "Jika file dokumen tagihan lebih dari satu file, zip dulu semua file tersebut");

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan nama istilah domain yang digunakan untuk
	 * dokumen pembayaran termin ini dalam konteks alur SOP. Dipanggil oleh
	 * framework SOP untuk menampilkan label yang tepat di UI alur persetujuan.
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan string literal nama modul ini dalam
	 * bahasa Indonesia.
	 *
	 * @return string {@code "Pembayaran Termin Barang / Jasa"}
	 * @throws Exception tidak dilempar dalam implementasi ini
	 */
	@Override
	public String istilah() throws Exception {
		return "Pembayaran Termin Barang / Jasa";
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan entitas {@link DataSop} yang sedang aktif
	 * diedit atau diproses dalam alur SOP. Diimplementasikan sebagai bagian dari
	 * kontrak interface {@link FormSop}.
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan field {@link #pembayaranTerminMasterAsset}
	 * yang sudah di-set sebelumnya melalui {@link #form} atau {@link #init}.
	 *
	 * @return entitas {@link PembayaranTerminMasterAsset} yang sedang aktif;
	 *         bisa null bila belum ada yang di-set
	 * @throws Exception tidak dilempar dalam implementasi ini
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pembayaranTerminMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kelas entitas yang dikelola oleh action ini,
	 * digunakan oleh framework SOP untuk introspeksi tipe entitas secara generik.
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan literal kelas
	 * {@link PembayaranTerminMasterAsset} tanpa query database.
	 *
	 * @return {@code Class} dari {@link PembayaranTerminMasterAsset}
	 * @throws Exception tidak dilempar dalam implementasi ini
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PembayaranTerminMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan kode pembayaran termin yang dijamin unik di
	 * database dengan mekanisme retry dan sufiks darurat. Metode ini merupakan
	 * lapisan keamanan di atas {@link #generateCode} untuk mencegah
	 * {@code ConstraintViolationException} akibat kode duplikat saat penyimpanan
	 * bersamaan atau counter nomor surat yang tertinggal.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghasilkan kode awal dengan {@code generateCode(true)} (counter maju).</li>
	 *   <li>Memeriksa keunikan kode ke database menggunakan
	 *       {@code Projections.rowCount()}.</li>
	 *   <li>Bila kode sudah ada, generate ulang dan coba lagi hingga 50 kali.</li>
	 *   <li>Bila counter tidak maju (kode sama berulang-ulang, tanda mode rowCount
	 *       yang stuck), hentikan loop lebih awal.</li>
	 *   <li>Bila setelah 50 percobaan masih duplikat, tambahkan sufiks acak
	 *       4 karakter dari {@code Common.getGeneratedBarCode(4)} sebagai
	 *       jalan darurat daripada gagal dengan exception.</li>
	 * </ol>
	 *
	 * @return kode string yang unik di tabel {@link PembayaranTerminMasterAsset};
	 *         dalam kondisi sangat jarang, bisa mengandung sufiks acak
	 *
	 * <p><b>Penanganan error:</b> Exception database ditangkap, ditampilkan
	 * via {@code Common.tampilErrorJikaAdmin}, dan kode terakhir yang ada
	 * (mungkin masih duplikat) dikembalikan sebagai fallback.
	 *
	 * <p><b>Pemeliharaan:</b> Batas 50 percobaan dipilih agar tidak memblokir
	 * thread terlalu lama. Pastikan indeks UNIQUE pada kolom {@code kode} di
	 * database tetap ada agar race condition saat penyimpanan serentak masih
	 * tertangkap di level DB.
	 */
	private String generateKodeUnik() {
		String noAgenda = generateCode(true);
		try {
			Session session = HibernateUtil.currentNativeSession();
			String sebelumnya = null;
			for (int percobaan = 0; percobaan < 50; percobaan++) {
				Number jumlah = (Number) session.createCriteria(PembayaranTerminMasterAsset.class)
						.add(Restrictions.eq("kode", noAgenda)).setProjection(Projections.rowCount()).uniqueResult();
				if (jumlah == null || jumlah.intValue() == 0) {
					return noAgenda;
				}
				if (noAgenda.equals(sebelumnya)) {
					/* Counter tidak maju (mode rowCount): hentikan loop. */
					break;
				}
				sebelumnya = noAgenda;
				noAgenda = generateCode(true);
			}
			return noAgenda + "-" + Common.getGeneratedBarCode(4);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			return noAgenda;
		}
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan kode pembayaran termin baru berdasarkan
	 * konfigurasi format nomor surat pengadaan yang telah ditetapkan administrator.
	 * Bila konfigurasi tidak tersedia, menggunakan barcode acak sebagai fallback.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Bila {@code NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA}
	 *       null atau nomorSuratnya null, kembalikan barcode acak.</li>
	 *   <li>Tentukan index: bila mode {@code gunakanIndexUrut}, gunakan
	 *       {@code nomorIndex} dari entitas; bila tidak, hitung via
	 *       {@link #getindex} (rowCount + 1 berdasarkan filter tahun/bulan/reset).</li>
	 *   <li>Bila parameter {@code tambah} true, naikkan counter index di entitas
	 *       {@link NomorSurat} via {@code NomorSurat.tambahIndexNomorSurat}.</li>
	 *   <li>Format kode menggunakan pola di {@code NomorSurat.format(index, tanggal)}.</li>
	 *   <li>Verifikasi keunikan via {@code KodeUnikUtil.pastikanUnik}.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param tambah {@code true} bila counter index harus dinaikkan (mode simpan);
	 *               {@code false} untuk preview saja (mode tampilkan form)
	 * @return string kode pembayaran termin sesuai format yang dikonfigurasi,
	 *         atau barcode acak bila konfigurasi tidak tersedia
	 *
	 * <p><b>Pemeliharaan:</b> Selalu gunakan {@link #generateKodeUnik} (bukan
	 * metode ini langsung) untuk penyimpanan aktual agar duplikat dicegah.
	 * Metode ini boleh dipanggil langsung hanya untuk preview kode di form.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA == null
				|| NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PembayaranTerminMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung index urut berikutnya untuk nomor surat pembayaran
	 * termin berdasarkan data historis di database, dengan mempertimbangkan aturan
	 * reset urutan yang dikonfigurasi pada {@link NomorSurat}.
	 *
	 * <p><b>Cara kerja:</b> Membangun kriteria Hibernate pada
	 * {@link PembayaranTerminMasterAsset} dengan filter-filter opsional:
	 * <ol>
	 *   <li>Filter by nomor surat spesifik (bila {@code urutBerdasarkanNomor})
	 *       atau by kelompok nomor surat (bila {@code urutBerdasarkanKelompok}).</li>
	 *   <li>Filter by tahun saat ini (bila {@code resetUrutanTiapTahun}).</li>
	 *   <li>Filter by tahun dan bulan saat ini (bila {@code resetUrutanTiapBulan}).</li>
	 *   <li>Filter by tanggal >= tanggal reset (bila {@code resetTiap} dikonfigurasi
	 *       dan sudah lewat atau hari ini).</li>
	 * </ol>
	 * Hasilnya adalah {@code rowCount} dari data yang cocok, ditambah 1 sebagai
	 * index berikutnya. Bila null (tabel kosong), dikembalikan 1.
	 *
	 * <p><b>Parameter:</b>
	 * @param nomorSurat entitas konfigurasi nomor surat; bila null dikembalikan 0
	 * @return index urut berikutnya (minimal 1); 0 bila nomorSurat null
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini menggunakan LEFT JOIN ke alias
	 * {@code nomorSuratAlurPengadaan} dan {@code nomorSurat} di entitas
	 * {@link PembayaranTerminMasterAsset}. Pastikan mapping Hibernate untuk
	 * relasi ini selalu ada.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PembayaranTerminMasterAsset.class)
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
	 * <b>Tujuan:</b> Menetapkan flag mode persetujuan pada action ini.
	 * Diimplementasikan sebagai bagian dari kontrak interface {@link FormSop}
	 * sehingga framework SOP dapat mengontrol apakah form berjalan dalam
	 * mode persetujuan atau mode edit biasa.
	 *
	 * <p><b>Cara kerja:</b> Menetapkan langsung field {@link #persetujuan}
	 * ke nilai yang diberikan. Perubahan ini akan terlihat pada pemanggilan
	 * {@link #form} berikutnya.
	 *
	 * <p><b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} untuk mode edit normal
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
