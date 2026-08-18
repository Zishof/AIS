package ais.action.master.akunting;

import java.util.Calendar;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.Transitori;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>TransitoriAction — Kontroler Tampilan dan Pengelolaan Data Transitori</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini mengelola tampilan dan pembaruan data <em>Transitori</em> dalam modul
 * akunting sistem eCampus. Transitori adalah transaksi keuangan yang sudah diajukan
 * melalui alur pengajuan (SOP) dan sudah memiliki {@link DaftarPengajuanTransfer}
 * (daftar pengajuan transfer ke rekening tujuan), namun prosesnya belum selesai
 * atau sedang dalam status tertentu (belum diproses, sudah diajukan, atau sudah
 * ditransfer).</p>
 *
 * <p>Halaman ini berfungsi sebagai <em>monitoring dan manajemen</em> proses transfer
 * keuangan, bukan sebagai CRUD sederhana. Pengguna dapat:</p>
 * <ul>
 *   <li>Melihat daftar transitori dengan berbagai status melalui filter checkbox.</li>
 *   <li>Memfilter berdasarkan rentang tanggal, status aktif, status proses transfer.</li>
 *   <li>Mengedit keterangan setiap transitori secara langsung di grid (inline editing).</li>
 *   <li>Mengklik tautan proses transfer untuk membuka detail proses transfer terkait.</li>
 *   <li>Mengklik tautan SOP untuk melihat alur SOP yang terkait dengan pengajuan.</li>
 *   <li>Mengekspor data ke Excel melalui tombol cetak.</li>
 * </ul>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework
 * dan mengimplementasikan {@link DataCriteria} dan {@link DataSearchDefault}.
 * Field-field UI di-wire otomatis dari ZUL. Berbeda dengan controller CRUD lain,
 * halaman ini <em>tidak memiliki formulir tambah/ubah</em> — entitas Transitori
 * dibuat oleh modul lain (saat pengajuan transfer diproses), bukan melalui halaman ini.</p>
 *
 * <p>Alur kerja utama:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan akses.</li>
 *   <li>{@code doAfterCompose} menginisialisasi filter tanggal (default 6 bulan lalu
 *       hingga besok), menjalankan pencarian awal, menginisialisasi paging, menambahkan
 *       tombol cetak, dan membuat timer default untuk refresh otomatis.</li>
 *   <li>{@code initCriteria} membangun query Hibernate yang kompleks dengan filter
 *       tanggal, status aktif, dan status proses (belum, diajukan, sudah transfer).</li>
 *   <li>{@code onSearchDefault} menjalankan query berhalaman dan memperbarui grid.</li>
 *   <li>{@code TransitoriRenderer} merender setiap baris dengan logika khusus
 *       untuk menangani kasus data pengajuan yang hilang (null).</li>
 * </ol>
 *
 * <p><b>Kelas bantu private:</b><br>
 * Tiga metode utilitas private statis disediakan untuk membantu rendering:
 * {@code text(String)} untuk null-safe string, {@code isTrue(Boolean)} untuk
 * null-safe boolean check, dan {@code formatNilai(Double)} untuk format angka.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dieksekusi di thread event ZK. Timer default yang dibuat di
 * {@code doAfterCompose} akan memicu {@code onSearchDefault} setelah delay singkat
 * — ini tetap di thread event ZK karena timer ZK menggunakan mekanisme event.
 * Sesi Hibernate ({@code HibernateUtil.currentSession()}) dikelola framework.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Metode renderer memiliki dua cabang logika utama: kasus {@code daftarPengajuanTransfer == null}
 * (data sumber tidak ditemukan) dan kasus normal. Pastikan kedua cabang tetap konsisten
 * jika ada perubahan pada jumlah kolom yang ditampilkan. Logika back-fill
 * ({@code setTransitoriData}) di dalam renderer memiliki efek samping pada basis data
 * — ini adalah pola yang tidak ideal namun sudah ada dan perlu dipertahankan.</p>
 *
 * @author eCampus Dev Team
 * @see Transitori
 * @see DaftarPengajuanTransfer
 * @see DataCriteria
 * @see DataSearchDefault
 */
public class TransitoriAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * Versi serial untuk serialisasi kelas. Menjaga kompatibilitas deserialisasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Komponen paging ZK untuk navigasi halaman pada daftar data transitori. */
	private Paging paging;

	/** Grid ZK yang menampilkan daftar data transitori. */
	private MyGrid grid;

	/** Kotak teks untuk filter pencarian berdasarkan kode atau nama pengajuan/transitori. */
	private Textbox searchnama;

	/**
	 * Checkbox untuk mengaktifkan filter hanya menampilkan transitori yang masih aktif.
	 * Jika dicentang, hanya transitori dengan {@code aktif = true} atau null (default aktif)
	 * yang ditampilkan. Jika tidak dicentang, semua transitori (termasuk yang tidak aktif)
	 * ditampilkan.
	 */
	private Checkbox searchaktif;

	/**
	 * Komponen datebox untuk batas awal rentang tanggal filter.
	 * Di-set secara otomatis ke 6 bulan sebelum tanggal hari ini saat halaman dibuka.
	 * Diset readonly agar pengguna menggunakan picker, bukan mengetik manual.
	 */
	private MyDatebox start;

	/**
	 * Komponen datebox untuk batas akhir rentang tanggal filter.
	 * Di-set secara otomatis ke hari besok saat halaman dibuka (inklusif hari ini).
	 * Diset readonly agar pengguna menggunakan picker, bukan mengetik manual.
	 */
	private MyDatebox end;

	/**
	 * Checkbox untuk memfilter transitori yang <em>belum</em> diproses (belum ada
	 * {@code prosesTransitori}). Jika dicentang, hanya menampilkan transitori
	 * dengan {@code prosesTransitori = null}.
	 */
	private Checkbox searchsudah;

	/**
	 * Checkbox untuk memfilter transitori yang <em>sudah diajukan</em> namun
	 * belum ditransfer. Jika dicentang, menampilkan transitori dengan
	 * {@code prosesTransitori != null} DAN {@code transfer = false}.
	 */
	private Checkbox searchbelum;

	/**
	 * Checkbox untuk memfilter transitori yang <em>sudah ditransfer</em>.
	 * Jika dicentang, menampilkan transitori dengan
	 * {@code prosesTransitori != null} DAN {@code transfer = true}.
	 */
	private Checkbox searchsudahtrnf;

	/** Tombol "Cari" di toolbar untuk memicu pencarian ulang. */
	private MyToolbarbuttonConfig find;

	/**
	 * Mengkonversi nilai String menjadi string aman (null-safe).
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menghindari penampilan teks "null" di UI ketika nilai field adalah null.
	 * Digunakan secara ekstensif di renderer untuk membuat Label dari field
	 * string yang mungkin bernilai null.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Jika {@code value} adalah null, mengembalikan string kosong {@code ""}.
	 * Jika tidak null, mengembalikan nilai apa adanya.</p>
	 *
	 * @param value nilai String yang mungkin null
	 * @return string aman (tidak pernah null), string kosong jika input null
	 */
	private static String text(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Mengkonversi nilai Boolean menjadi boolean primitif (null-safe).
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani nilai Boolean wrapper yang mungkin null dengan aman.
	 * Digunakan untuk memeriksa flag {@code transfer} pada Transitori
	 * tanpa risiko NullPointerException.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan {@code Boolean.TRUE.equals(value)} yang mengembalikan {@code false}
	 * jika {@code value} adalah null (karena null tidak equals Boolean.TRUE).
	 * Ini lebih aman daripada {@code value != null && value.booleanValue()}.</p>
	 *
	 * @param value nilai Boolean wrapper yang mungkin null
	 * @return {@code true} jika value adalah Boolean.TRUE; {@code false} untuk null atau false
	 */
	private static boolean isTrue(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	/**
	 * Memformat nilai Double sebagai string angka dengan pemisah ribuan.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menampilkan nominal keuangan dengan format yang mudah dibaca manusia,
	 * menggunakan pemisah ribuan sesuai locale Indonesia (titik sebagai pemisah
	 * ribuan, koma sebagai desimal).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan {@code Common.numberFormat.get().format()} yang merupakan
	 * ThreadLocal NumberFormat. Jika value null, menggunakan 0.0 sebagai default
	 * agar menampilkan "0" daripada NPE.</p>
	 *
	 * @param value nilai Double yang akan diformat; null dianggap sebagai 0.0
	 * @return string representasi angka dengan format lokal, tidak pernah null
	 */
	private static String formatNilai(Double value) {
		return Common.numberFormat.get().format(value == null ? 0.0 : value.doubleValue());
	}

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memproteksi halaman monitoring transitori dari akses tanpa otorisasi.
	 * Pemeriksaan dilakukan di awal siklus hidup halaman sebelum UI dirender.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan ke {@code Common.doCheckSecurity()} untuk verifikasi sesi
	 * dan privilege akses, kemudian meneruskan ke metode induk ZK untuk
	 * melanjutkan inisialisasi komponen.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak boleh dihapus — ini adalah gerbang keamanan pertama halaman ini.</p>
	 *
	 * @param page     halaman ZK yang sedang dibangun
	 * @param parent   komponen induk controller
	 * @param compInfo informasi metadata komponen dari ZUL
	 * @return informasi komponen yang diteruskan ke framework ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menginisialisasi komponen UI setelah semua komponen ZUL selesai di-wire.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menyelesaikan inisialisasi halaman monitoring transitori: mengkonfigurasi
	 * filter tanggal default, menjalankan pencarian awal, menyiapkan paging,
	 * menambahkan tombol ekspor, dan membuat timer refresh otomatis.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Membangun MyButtonTabbox dengan dua tab:
	 *       <ul>
	 *         <li>Tab 0 "Proses Transitori": lazy, memuat proses_transitori.zul saat pertama aktif.</li>
	 *         <li>Tab 1 "Daftar Transitori": eager, memuat transitori_tab_1.zul segera agar
	 *             field-field UI (grid, paging, searchnama, dst.) ter-wire oleh super.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZK.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk inisialisasi label bahasa.</li>
	 *   <li>Mengatur datebox start dan end menjadi readonly.</li>
	 *   <li>Menetapkan nilai awal filter tanggal (start: 6 bulan lalu, end: besok).</li>
	 *   <li>Menjalankan pencarian awal dengan {@code onSearchDefault(null)}.</li>
	 *   <li>Menginisialisasi paging dengan listener refresh.</li>
	 *   <li>Menambahkan tombol cetak/ekspor ke toolbar.</li>
	 *   <li>Membuat timer default untuk refresh otomatis.</li>
	 * </ol></p>
	 *
	 * @param comp komponen ZK root dari halaman ini
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		Div mainContainer = (Div) comp.getFellow("mainContainer");
		int[] tabAktif = {0};
		MyButtonTabbox btabs = MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		// Tab 0: Proses Transitori (include — lazy)
		final String srcProsesTransitori = "/WEB-INF/z/x/y/pages/master/akunting/proses_transitori.zul";
		btabs.tambahTabLazy(0, "Proses Transitori", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				MyButtonTabbox.muatZul(panel, srcProsesTransitori);
			}
		});

		// Tab 1: Daftar Transitori (inline — eager, wires field-field action ini)
		Div panelDaftarTransitori = btabs.tambahTab(1, "Daftar Transitori");
		MyButtonTabbox.muatZulEager(panelDaftarTransitori,
				"/WEB-INF/z/x/y/pages/master/akunting/transitori_tab_1.zul");

		super.doAfterCompose(comp);

		btabs.pulihkanSeleksi(2);

		Common.initLaguage();

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif",
				"daftarPengajuanTransfer.pembayaranDpMasterAsset",
				"daftarPengajuanTransfer.pembayaranTerminMasterAsset", "daftarPengajuanTransfer.pembayaranGaji",
				"daftarPengajuanTransfer.pembayaranPengadaanMasterAssetDetail",
				"daftarPengajuanTransfer.pertangungjawaban", "daftarPengajuanTransfer.uangMuka",
				"daftarPengajuanTransfer.penggantianKasKecil", "daftarPengajuanTransfer.nominal",
				"daftarPengajuanTransfer.atasNamaSumber", "daftarPengajuanTransfer.noRekSumber",
				"daftarPengajuanTransfer.bankSumber", "prosesTransitori" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Transitori.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
	}

	/**
	 * Kelas renderer dalam untuk menampilkan setiap baris data {@link Transitori} pada grid.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data transitori ke dalam Row ZK dengan layout yang kompleks.
	 * Karena setiap transitori terhubung ke {@link DaftarPengajuanTransfer} yang memuat
	 * banyak informasi, renderer ini menampilkan data dari kedua entitas sekaligus.
	 * Terdapat dua cabang logika: kasus data sumber (daftarPengajuanTransfer) tidak
	 * ditemukan, dan kasus normal.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Renderer mengekstrak {@code daftarPengajuanTransfer} dari entitas transitori.
	 * Jika null (data sumber dihapus atau tidak ditemukan), tampilan minimal ditampilkan
	 * dengan pesan "Data pengajuan asal tidak ditemukan" dan field keterangan yang bisa
	 * diedit inline. Jika tidak null, dilakukan back-fill
	 * ({@code setTransitoriData}) jika field {@code transitoriData} di
	 * {@code DaftarPengajuanTransfer} belum terisi — ini memastikan referensi balik
	 * antara dua entitas selalu konsisten. Kemudian kolom-kolom ditampilkan: nama
	 * transitori (via RevisiHelper), tanggal waktu pengajuan, link proses transfer
	 * (jika ada), informasi akun dan bank sumber, nominal, status proses, keterangan
	 * (editable inline), dan link SOP (jika ada).</p>
	 *
	 * <p><b>Inline editing keterangan:</b><br>
	 * Field keterangan di grid langsung bisa diedit pengguna tanpa membuka formulir.
	 * Saat pengguna mengubah isi textbox dan menekan Enter/Tab (event {@code onChange}),
	 * nilai baru langsung disimpan ke basis data menggunakan
	 * {@code Common.refreshSaveOrUpdate(transitori)}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pastikan jumlah kolom yang ditambahkan ke Row konsisten antara cabang null dan
	 * cabang normal, agar lebar kolom grid tidak bergeser. Perubahan pada jumlah kolom
	 * di ZUL header harus diimbangi dengan perubahan di sini.</p>
	 */
	class TransitoriRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data Transitori ke dalam komponen ZK Row.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link Transitori} (beserta {@link DaftarPengajuanTransfer}
		 * yang terhubung) menjadi representasi visual lengkap dengan support inline editing
		 * dan tautan navigasi ke entitas terkait.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * <b>Cabang daftarPengajuanTransfer == null:</b>
		 * Menampilkan Vbox dengan nama transitori, pesan error, Label kosong untuk kolom
		 * yang tidak bisa diisi, Label nominal "0", Label status "Belum", Textbox keterangan
		 * editable, dan Label kosong untuk kolom SOP. Kemudian return early.
		 * <br><br>
		 * <b>Cabang normal:</b>
		 * <ol>
		 *   <li>Back-fill: jika {@code daftarPengajuanTransfer.getTransitoriData()} null,
		 *       set referensi ke transitori ini dan simpan.</li>
		 *   <li>Kolom 1 (Vbox): nama transitori via RevisiHelper + kode pengajuan sebagai Label.</li>
		 *   <li>Kolom 2 (Vbox): tanggal waktu pengajuan + tautan proses transfer (jika ada).
		 *       Klik tautan membuka detail proses transfer melalui
		 *       {@code ProsesTransferAction.onAddExternal} dengan callback refresh.</li>
		 *   <li>Kolom 3: informasi akun kredit (kode + nama) dari pengajuan transfer.</li>
		 *   <li>Kolom 4 (Vbox): nama bank sumber, nama rekening, dan nomor rekening sumber.</li>
		 *   <li>Kolom 5: nominal transfer yang diformat dengan pemisah ribuan.</li>
		 *   <li>Kolom 6: status proses — "Belum" jika belum ada prosesTransitori,
		 *       "Sudah Transfer" jika sudah ditransfer, "Diajukan" jika sudah diproses
		 *       tapi belum ditransfer.</li>
		 *   <li>Kolom 7: Textbox keterangan editable inline dengan listener onChange
		 *       yang menyimpan perubahan langsung ke basis data.</li>
		 *   <li>Kolom 8: tautan SOP jika ada disposisiSop terkait. Teks dipersingkat
		 *       menggunakan {@code UIClassHelper.applyReadMore}. Klik membuka alur SOP
		 *       melalui {@code TampilanAlurSopAction.prosess}.</li>
		 * </ol></p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Semua akses ke field yang nullable menggunakan null check (operator ternary
		 * atau guard if). Inline saving menggunakan {@code Common.refreshSaveOrUpdate}
		 * yang menangani transaksi Hibernate secara managed.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen ditambahkan
		 * @param arg1 objek data, harus berupa {@link Transitori}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Transitori transitori = (Transitori) arg1;

			final DaftarPengajuanTransfer daftarPengajuanTransfer = transitori.getDaftarPengajuanTransfer();

			if (daftarPengajuanTransfer == null) {
				Vbox box = new Vbox();
				box.setParent(arg0);
				RevisiHelper.createNewRevisi(Transitori.class, transitori, text(transitori.getNama())).setParent(box);
				new Label(ais.common.Common.getBahasaConfig("Data pengajuan asal tidak ditemukan")).setParent(box);
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
				new Label(formatNilai(null)).setParent(arg0);
				new Label(transitori.getProsesTransitori() == null ? "Belum" : "Diajukan").setParent(arg0);

				final Textbox keteranganKosong = new Textbox(text(transitori.getKeterangan()));
				keteranganKosong.setWidth("95%");
				keteranganKosong.setRows(2);
				keteranganKosong.setParent(arg0);
				keteranganKosong.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						transitori.setKeterangan(keteranganKosong.getValue());
						Common.refreshSaveOrUpdate(transitori);
					}
				});
				new Label().setParent(arg0);
				return;
			}

			if (daftarPengajuanTransfer != null && daftarPengajuanTransfer.getTransitoriData() == null) {
				daftarPengajuanTransfer.setTransitoriData(transitori);
				Common.refreshUpdate(daftarPengajuanTransfer);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Transitori.class, transitori, text(transitori.getNama()))).setParent(arg0);

			new Label(text(daftarPengajuanTransfer.getKode())).setParent(a);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(daftarPengajuanTransfer.getWaktu() == null ? ""
					: Common.dateFormat.get().format(daftarPengajuanTransfer.getWaktu())).setParent(vbox);

			if (daftarPengajuanTransfer.getProsesTransfer() != null) {

				A aa = new A(daftarPengajuanTransfer.getProsesTransfer().getKode());
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, daftarPengajuanTransfer.getProsesTransfer());

					}
				});
				aa.setStyle("font-size:12px;");
				aa.setParent(vbox);
			}

			new Label(daftarPengajuanTransfer.getAkun() == null ? ""
					: daftarPengajuanTransfer.getAkun().getKode() + "-" + daftarPengajuanTransfer.getAkun().getNama())
					.setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(daftarPengajuanTransfer.getBankSumber() == null ? ""
					: daftarPengajuanTransfer.getBankSumber().getNama()).setParent(vbox);
			new Label(text(daftarPengajuanTransfer.getAtasNamaSumber())).setParent(vbox);
			new Label(text(daftarPengajuanTransfer.getNoRekSumber())).setParent(vbox);

			new Label(formatNilai(daftarPengajuanTransfer.getNominal())).setParent(arg0);

			new Label(transitori.getProsesTransitori() == null ? "Belum"
					: (isTrue(transitori.getTransfer()) ? "Sudah Transfer" : "Diajukan")).setParent(arg0);

			final Textbox keterangan = new Textbox(text(transitori.getKeterangan()));
			keterangan.setWidth("95%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transitori.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(transitori);
				}
			});


			if (daftarPengajuanTransfer.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(arg0);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + text(daftarPengajuanTransfer.getDisposisiSop().getKeterangan())
						+ (daftarPengajuanTransfer.getDisposisiSop().getSop() == null ? ""
								: " (" + text(daftarPengajuanTransfer.getDisposisiSop().getSop().getNama()) + ")"));
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(daftarPengajuanTransfer.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			} else {
				new Label().setParent(arg0);
			}

		}

	}

	/**
	 * Membangun kriteria Hibernate yang kompleks untuk query pencarian Transitori.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari {@link DataCriteria}. Membangun query dengan multiple filter
	 * berdasarkan rentang tanggal, status aktif, dan berbagai status proses transfer.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat Criteria pada kelas {@link Transitori} dengan JOIN ke
	 * {@link DaftarPengajuanTransfer} (alias "daftarPengajuanTransfer"). Filter yang
	 * diterapkan:
	 * <ol>
	 *   <li><b>Filter tanggal:</b> menggunakan {@code Restrictions.between} pada
	 *       {@code daftarPengajuanTransfer.waktu}. Jika start/end null, semua data
	 *       ditampilkan ({@code sqlRestriction("1=1")}). Kondisi null check dilapis
	 *       untuk menangani kasus komponen tidak ada di ZUL.</li>
	 *   <li><b>Filter status aktif ({@code searchaktif}):</b> jika dicentang, hanya
	 *       menampilkan transitori dengan {@code aktif = true} ATAU {@code aktif = null}
	 *       (karena default dianggap aktif). Jika tidak dicentang, semua data tampil.</li>
	 *   <li><b>Filter belum diproses ({@code searchbelum}):</b> jika dicentang,
	 *       menampilkan transitori dengan {@code prosesTransitori = null}.</li>
	 *   <li><b>Filter sudah diajukan ({@code searchsudah}):</b> jika dicentang,
	 *       menampilkan transitori dengan {@code prosesTransitori != null} DAN
	 *       {@code transfer = false}. Logika ini terbalik dengan nama variabelnya
	 *       ({@code searchsudah} sebenarnya memfilter "sudah diajukan belum transfer").</li>
	 *   <li><b>Filter sudah transfer ({@code searchsudahtrnf}):</b> jika dicentang,
	 *       menampilkan transitori dengan {@code prosesTransitori != null} DAN
	 *       {@code transfer = true}.</li>
	 *   <li><b>Filter teks ({@code searchnama}):</b> jika tidak kosong, mencari
	 *       case-insensitive substring pada kode pengajuan, nama pengajuan, atau
	 *       nama transitori (OR antara ketiga kondisi).</li>
	 * </ol>
	 * Jika {@code order} true, hasil diurutkan descending berdasarkan ID (data terbaru
	 * di atas).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Filter yang menggunakan {@code sqlRestriction("true")} atau {@code sqlRestriction("1=1")}
	 * adalah kondisi no-op (tidak memfilter apapun). Jika checkbox baru perlu ditambahkan,
	 * ikuti pola yang sama: {@code .add(checkboxBaru.isChecked() ? Restrictions.xxx : Restrictions.sqlRestriction("true"))}.</p>
	 *
	 * @param order {@code true} jika hasil harus diurutkan descending berdasarkan ID
	 * @return objek {@link Criteria} Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Transitori.class)
				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")
				.add((start == null || end == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (start.getValue() == null || end.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.between("daftarPengajuanTransfer.waktu", start.getValue(), end.getValue())))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchbelum.isChecked() ? Restrictions.isNull("prosesTransitori")
						: Restrictions.sqlRestriction("true"))

				.add(searchsudah.isChecked() ?

						Restrictions.and(Restrictions.isNotNull("prosesTransitori"), Restrictions.eq("transfer", false))

						: Restrictions.sqlRestriction("true"))

				.add(searchsudahtrnf.isChecked() ?

						Restrictions.and(Restrictions.isNotNull("prosesTransitori"), Restrictions.eq("transfer", true))

						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));

		if (!searchnama.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.or(
					Restrictions.ilike("daftarPengajuanTransfer.kode", searchnama.getValue().trim(),
							MatchMode.ANYWHERE),
					Restrictions.or(
							Restrictions.ilike("daftarPengajuanTransfer.nama", searchnama.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))));
		}
		return criteria;
	}

	/**
	 * Menjalankan pencarian data dan memperbarui tampilan grid Transitori dengan paging.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari {@link DataSearchDefault}. Memperbarui tampilan grid dengan data
	 * hasil pencarian pada halaman aktif, menggunakan paging untuk navigasi.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menghitung total baris dan memperbarui komponen paging dengan query tanpa
	 *       ordering ({@code initCriteria(false)}) untuk efisiensi.</li>
	 *   <li>Menjalankan query dengan ordering descending (data terbaru lebih dulu),
	 *       batasan baris per halaman ({@code ROWS_COUNT_ON_PAGE}), dan offset
	 *       berdasarkan halaman aktif.</li>
	 *   <li>Menampilkan hasil di grid dengan {@link TransitoriRenderer}.</li>
	 * </ol>
	 * Metode ini dipanggil dari berbagai titik: inisialisasi halaman, perubahan halaman
	 * paging, timer default, dan tombol Cari.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika filter tambahan perlu ditambahkan (misalnya filter berdasarkan satuan kerja),
	 * tambahkan kondisi di {@code initCriteria}, bukan di sini.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null} jika dipanggil
	 *              secara programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Transitori> transitori = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transitori);
		grid.setRowRenderer(new TransitoriRenderer());
		grid.setModelCheckMobile(strset);

	}

}
