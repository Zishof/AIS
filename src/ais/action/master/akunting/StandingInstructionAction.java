package ais.action.master.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.action.master.dashboard.akunting.DasboardStandingInstruction;
import ais.action.master.dashboard.akunting.DasboardStandingInstructionPerJenis;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.detail.PembayaranGajiPunyaPegawaiPerBankAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.ProsesTransferStandingInstruction;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>StandingInstructionAction — Controller Standing Instruction Transfer</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola halaman daftar
 * Standing Instruction (SI) dalam modul akuntansi dan penggajian. Standing
 * Instruction adalah instruksi transfer rutin atau berkala yang telah disetujui
 * melalui alur SOP, biasanya untuk pembayaran gaji pegawai ke berbagai bank
 * secara otomatis. Setiap {@link StandingInstruction} merepresentasikan satu
 * instruksi transfer dengan nominal, rekening sumber, bank tujuan, dan
 * informasi SOP yang mengotorisasi transfer tersebut.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan awal.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen: mendaftarkan listener pada
 *       filter satuan kerja, inisialisasi model pohon satuan kerja, mengatur
 *       rentang tanggal default (6 bulan lalu hingga besok), memanggil
 *       {@code onSearchDefault}, inisialisasi paging, timer default untuk refresh
 *       berkala, dan mendaftarkan tombol ekspor Excel.</li>
 *   <li>{@code onStatistik} — event handler yang membangun panel statistik lazily
 *       (hanya saat tab statistik pertama kali dibuka), menampilkan dua dasbor:
 *       statistik per proses transfer dan statistik per jenis pengajuan.</li>
 *   <li>Inner class {@code StandingInstructionRenderer} — merender setiap baris
 *       dengan informasi komprehensif: widget pembayaran gaji bank, revisi nama,
 *       tanggal, akun, info rekening sumber, nominal, status transfer (dari JSON
 *       {@code transferVia}), keterangan yang dapat diedit inline, dan link SOP.</li>
 *   <li>{@code initCriteria} — membangun Hibernate Criteria dengan filter satuan
 *       kerja, rentang tanggal, dan status transfer (belum/sudah diproses/sudah
 *       ditransfer).</li>
 *   <li>{@code onSearchDefault} — menjalankan query dan memperbarui grid.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZK event-dispatcher. Timer default ZK
 * digunakan untuk refresh berkala data standing instruction (karena status
 * transfer bisa berubah dari proses batch di latar belakang). Operasi JSON
 * parsing pada field {@code transferVia} di renderer berjalan synchronously
 * di thread event-dispatcher dan memerlukan query database tambahan per baris
 * untuk memuat {@link ProsesTransferStandingInstruction}; pertimbangkan caching
 * jika performa menjadi masalah.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Field {@code transferVia} pada entitas {@link StandingInstruction} adalah JSON
 *   string dengan format {@code {"{bankId}": {"si": "{idProses}", "nilai": 0.0}}}.
 *   Perubahan format JSON ini harus diikuti perubahan pada renderer.<br>
 * - Array {@code contents} (statis publik) digunakan di berbagai tempat untuk
 *   ekspor; perbarui semua pemanggil jika ada perubahan kolom.<br>
 * - Tab statistik dimuat secara lazy (lazy loading) untuk menghindari query
 *   yang berat saat halaman pertama dibuka.<br>
 * - Filter status transfer (searchbelum/searchsudah/searchsudahtrnf) <b>tidak</b> memakai
 *   Criteria/LEFT JOIN -- relasi ke {@link ProsesTransferStandingInstruction} hanya berupa
 *   JSON ({@code transferVia}), bukan foreign key, sehingga status dihitung di Java oleh
 *   {@link #hitungStatusTransfer(Session, StandingInstruction)} dan disaring setelah seluruh
 *   baris (yang lolos filter satuan kerja/tanggal/nama) diambil dari DB; lihat javadoc
 *   {@link #onSearchDefault(Event)}. Perhatikan implikasi performa pada dataset besar.</p>
 */
public class StandingInstructionAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private MyDatebox start;
	private MyDatebox end;

	/** Checkbox filter: tampilkan SI yang sudah disetujui (prosesTransfer sudah ada, belum ditransfer). */
	private Checkbox searchsudah;
	/** Checkbox filter: tampilkan SI yang belum disetujui (prosesTransfer null). */
	private Checkbox searchbelum;
	/** Checkbox filter: tampilkan SI yang sudah ditransfer (prosesTransfer ada dan realisasikan sudah terisi). */
	private Checkbox searchsudahtrnf;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private MyToolbarbuttonConfig find;

	/**
	 * Array nama properti yang diekspor ke Excel; bersifat publik dan statis
	 * agar dapat digunakan oleh kelas lain yang membutuhkan daftar kolom SI.
	 */
	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "satuanKerja", "waktu",
			"disposisiSop", "aktif", "pembayaranGaji", "nominal", "atasNamaSumber", "noRekSumber", "bankSumber",
			"prosesTransferStandingInstruction" };

	/** Panel tab statistik; kontennya dimuat secara lazy saat pertama kali diklik. */
	protected Tabpanel statistik;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * <b>Tujuan:</b> Membangun dan menampilkan panel statistik standing instruction
	 * secara lazy dalam tab yang disediakan di halaman utama. Panel ini menampilkan
	 * dua dasbor berbeda: statistik per proses transfer dan statistik per jenis pengajuan.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode ini dipanggil saat pengguna mengklik tab "Statistik" di halaman utama.
	 * Untuk mencegah pembuatan komponen berulang kali, dilakukan pengecekan apakah
	 * panel statistik sudah berisi komponen ({@code statistik.getChildren().size() == 0}).
	 * Jika belum ada konten:
	 * <ol>
	 *   <li>Membuat Tabbox dengan dua tab: "Per Proses" dan "Per Jenis Pengajuan".</li>
	 *   <li>Menyesuaikan tinggi Tabbox dengan tinggi layar pengguna jika tersedia
	 *       (menggunakan {@code MainAction.desktopHeights} dengan ID pengguna sebagai kunci).</li>
	 *   <li>Panel tab pertama ("Per Proses") langsung dimuat dengan
	 *       {@link DasboardStandingInstruction}.</li>
	 *   <li>Panel tab kedua ("Per Jenis Pengajuan") menggunakan lazy loading:
	 *       {@link DasboardStandingInstructionPerJenis} baru dibuat saat tab kedua
	 *       pertama kali diklik, melalui listener event "onClick" pada tab.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari pembuatan komponen dasbor diteruskan ke framework ZK.
	 * Null check pada {@code tbmuser} dan {@code desktopHeight} mencegah NPE
	 * jika informasi layar tidak tersedia.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Lazy loading pada tab kedua sangat penting karena dasbor per jenis pengajuan
	 * melibatkan query agregasi yang berat. Jangan ubah pola ini menjadi eager
	 * loading tanpa pertimbangan performa. Jika ada tab statistik ketiga yang perlu
	 * ditambahkan, ikuti pola lazy loading yang sama.</p>
	 *
	 * @param event event ZK yang memicu penampilan panel statistik, biasanya
	 *              dari klik tab atau tombol statistik
	 */
	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {

			ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(statistik, "100%", new int[] { 0 });

			{ org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Per Proses", "/img/svg/process.svg");
			  DasboardStandingInstruction include = new DasboardStandingInstruction();
			  include.setHeight("100%"); include.setWidth("100%"); include.setParent(panel); }

			btnTab.tambahTabLazy(1, "Per Jenis Pengajuan", "/img/svg/list-task.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(org.zkoss.zul.Div panel) throws Exception {
					DasboardStandingInstructionPerJenis include = new DasboardStandingInstructionPerJenis();
					include.setHeight("100%"); include.setWidth("100%"); include.setParent(panel);
				}
			});

		}
	}

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Dipanggil oleh framework ZK sebagai langkah paling awal dalam siklus hidup
	 * komposisi halaman, memastikan hanya pengguna yang berhak yang dapat mengakses
	 * halaman standing instruction.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk verifikasi otentikasi dan
	 * otorisasi pengguna. Jika tidak berhak, diarahkan ke logoff. Kemudian memanggil
	 * implementasi super untuk melanjutkan komposisi halaman.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan keamanan. Logika yang membutuhkan komponen
	 * ZUL harus di {@link #doAfterCompose(Component)}.</p>
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
	 * standing instruction setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah inisialisasi yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Menemukan mainContainer sebelum super dipanggil.</li>
	 *   <li>Membangun MyButtonTabbox di mainContainer.</li>
	 *   <li>Mendaftarkan tab eager "Standing Instruction" (inline sub-ZUL) agar
	 *       field-field di dalamnya dapat di-wire oleh super.doAfterCompose.</li>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Mendaftarkan tab lazy "Proses Transfer Standing Instruction" (include)
	 *       dan tab lazy "Statistik" (delegasi ke onStatistik) setelah super.</li>
	 *   <li>Memulihkan seleksi tab terakhir.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk lokalisasi bahasa.</li>
	 *   <li>Mendaftarkan listener pada {@code searchparent} (filter satuan kerja)
	 *       agar pencarian diperbarui saat satuan kerja dipilih.</li>
	 *   <li>Menginisialisasi {@code SatuanKerjaTreeModel} untuk dukungan filter hierarki.</li>
	 *   <li>Mengatur komponen datebox {@code start} dan {@code end} menjadi readonly
	 *       dan menetapkan nilai default: {@code start} = 6 bulan lalu, {@code end} = besok.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk data awal.</li>
	 *   <li>Menginisialisasi paging dengan listener standar.</li>
	 *   <li>Mendaftarkan tombol ekspor Excel dengan array {@code contents} statis.</li>
	 *   <li>Membuat timer default ZK untuk refresh berkala data (karena status
	 *       transfer SI bisa berubah dari proses batch eksternal).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super atau inisialisasi komponen diteruskan ke framework ZK.
	 * Null check pada komponen datebox ({@code if (start != null)}) diperlukan
	 * karena komponen ini bersifat opsional di ZUL.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Timer default menyebabkan refresh otomatis; pertimbangkan dampaknya pada
	 * performa jika data SI sangat banyak. Jika diperlukan, ganti dengan timer
	 * berkala yang lebih jarang atau mekanisme server push.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai di-autowire
	 * @throws Exception jika terjadi error saat inisialisasi komponen
	 */
	public void doAfterCompose(Component comp) throws Exception {
		// a. Find mainContainer BEFORE calling super so it is available before autowiring
		org.zkoss.zul.Div mainContainer = (org.zkoss.zul.Div) comp.getFellow("mainContainer");

		// b. Build MyButtonTabbox
		int[] tabAktif = {0};
		ais.ui.util.MyButtonTabbox btabs = ais.ui.util.MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		// c. Eager inline tab (index 1 — "Standing Instruction"); loaded now so super can wire its fields
		org.zkoss.zul.Div panelSI = btabs.tambahTab(1, "Standing Instruction");
		ais.ui.util.MyButtonTabbox.muatZulEager(panelSI,
				"/WEB-INF/z/x/y/pages/master/akunting/standing_instruction_tab_1.zul");

		// e. Wire fields — including those contributed by the eager sub-ZUL above
		super.doAfterCompose(comp);

		// f. Lazy tabs — registered AFTER super to avoid premature side effects

		// Tab 0 — include: Proses Transfer Standing Instruction
		final String srcProsesTransfer =
				"/WEB-INF/z/x/y/pages/master/akunting/proses_transfer_standing_instruction.zul";
		btabs.tambahTabLazy(0, "Proses Transfer Standing Instruction",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(org.zkoss.zul.Div panel) throws Exception {
						ais.ui.util.MyButtonTabbox.muatZul(panel, srcProsesTransfer);
					}
				});

		// Tab 2 — statistik: allocate container, then delegate to existing onStatistik
		btabs.tambahTabLazy(2, "Statistik", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(org.zkoss.zul.Div panel) throws Exception {
				statistik = new ais.ui.util.MyTabpanel();
				statistik.setParent(panel);
				statistik.setWidth("100%");
				statistik.setHeight("100%");
				onStatistik(null);
			}
		});

		// g. Restore last-active tab
		btabs.pulihkanSeleksi(3);

		// h. Existing post-wire initialization (unchanged from original)
		Common.initLaguage();
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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(StandingInstruction.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * <h3>StandingInstructionRenderer — Renderer Baris Grid Standing Instruction</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini merender setiap baris pada grid daftar standing instruction.
	 * Baris menampilkan informasi komprehensif tentang setiap SI meliputi: widget
	 * pembayaran gaji bank, nama dan kode SI dengan revisi history, waktu instruksi,
	 * akun yang terkait, informasi rekening sumber (bank, nama, nomor rekening),
	 * nominal transfer, status proses transfer (diparsing dari JSON {@code transferVia}),
	 * keterangan yang dapat diedit langsung (inline edit), dan link ke alur SOP
	 * yang mengotorisasi instruksi ini.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Untuk setiap objek {@link StandingInstruction}:
	 * <ol>
	 *   <li>Menampilkan widget {@link PembayaranGajiPunyaPegawaiPerBankAction} untuk
	 *       informasi rekening gaji pegawai per bank.</li>
	 *   <li>Menampilkan nama SI dalam Vbox dengan widget revisi dan kode SI.</li>
	 *   <li>Menampilkan tanggal/waktu SI.</li>
	 *   <li>Menampilkan informasi akun (kode + nama).</li>
	 *   <li>Menampilkan Vbox rekening sumber: nama bank, atas nama, nomor rekening.</li>
	 *   <li>Menampilkan nominal transfer.</li>
	 *   <li>Melakukan parsing JSON {@code transferVia} untuk setiap bank tujuan:
	 *       memuat {@link ProsesTransferStandingInstruction} dari database berdasarkan ID
	 *       yang tersimpan di JSON, lalu menampilkan label status transfer.</li>
	 *   <li>Menampilkan Textbox keterangan yang dapat diedit langsung; perubahan
	 *       langsung disimpan ke database via {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Menampilkan link SOP jika ada disposisi SOP yang terkait, menggunakan
	 *       {@code UIClassHelper.applyReadMore} untuk teks panjang dan link yang
	 *       membuka tampilan alur SOP.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Rendering berjalan di thread ZK event-dispatcher. Query database untuk
	 * {@link ProsesTransferStandingInstruction} per baris bisa menjadi bottleneck
	 * performa jika data banyak. Pertimbangkan join atau fetch lazy vs eager.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Format JSON {@code transferVia} adalah {@code {"{bankId}": {"si": "{id}", "nilai": 0.0}}}.
	 * Jika format berubah, renderer ini harus diperbarui. Perhatikan juga null-safety
	 * pada parsing JSON ({@code isNull} checks sebelum {@code get}).</p>
	 */
	class StandingInstructionRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan semua informasi standing
		 * instruction beserta kontrol interaktif untuk mengedit keterangan inline
		 * dan melihat detail SOP.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Metode ini dipanggil oleh framework ZK untuk setiap item dalam model daftar.
		 * Setelah menampilkan kolom-kolom data, melakukan parsing JSON {@code transferVia}
		 * menggunakan {@code JSONObject} dari org.json. Untuk setiap bank tujuan dalam
		 * JSON, memuat {@link ProsesTransferStandingInstruction} dari database menggunakan
		 * session Hibernate terkelola. Status transfer ditampilkan sebagai teks dengan
		 * format "Transfer ke [nama bank] [nominal]" atau "Diajukan" jika belum ada proses.
		 * Textbox keterangan menggunakan listener {@code onChange} untuk menyimpan
		 * langsung ke database tanpa tombol simpan terpisah.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Parsing JSON menggunakan blok try-catch untuk menangani format tidak valid
		 * pada field {@code jsonObjectData} tanpa menghentikan rendering baris.
		 * Null check dilakukan pada setiap akses ke objek yang bisa null.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Variabel {@code transfer} dibangun dengan konkatenasi string; untuk data
		 * banyak bank tujuan, pertimbangkan StringBuilder. Query database dalam loop
		 * ({@code createCriteria} per bank) bisa menjadi N+1 problem; pertimbangkan
		 * eager fetch atau batch loading jika performa bermasalah.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen UI
		 * @param arg1 objek data, dicast ke {@link StandingInstruction}
		 * @throws Exception jika terjadi error saat membuat komponen ZK atau
		 *                   saat query database dalam rendering
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final StandingInstruction standingInstruction = (StandingInstruction) arg1;

			new PembayaranGajiPunyaPegawaiPerBankAction(standingInstruction.getPembayaranGaji()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(StandingInstruction.class, standingInstruction,
					standingInstruction.getNama())).setParent(arg0);

			new Label(standingInstruction.getKode()).setParent(a);

			new Label(standingInstruction.getWaktu() == null ? ""
					: Common.dateFormat.get().format(standingInstruction.getWaktu())).setParent(arg0);

			new Label(standingInstruction.getAkun() == null ? ""
					: standingInstruction.getAkun().getKode() + "-" + standingInstruction.getAkun().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(standingInstruction.getBankSumber() == null ? "" : standingInstruction.getBankSumber().getNama())
					.setParent(vbox);
			new Label(standingInstruction.getAtasNamaSumber()).setParent(vbox);
			new Label(standingInstruction.getNoRekSumber()).setParent(vbox);

			new Label(Common.numberFormat.get().format(standingInstruction.getNominal())).setParent(arg0);
			Session session = HibernateUtil.currentSession();
			JSONObject jsonObjectTransfer = new JSONObject(standingInstruction.getTransferVia());
			Iterator<String> iterator = jsonObjectTransfer.keys();
			String transfer = "";
			while (iterator.hasNext()) {
				String d = iterator.next();
				Long idBank = Long.parseLong(d);
				Bank bank = (Bank) ConstantValues.ambil(Bank.class.getName(), idBank);

				JSONObject jsonObjectData = null;

				try {
					jsonObjectData = jsonObjectTransfer.isNull(idBank.toString()) ? null
							: jsonObjectTransfer.getJSONObject(idBank.toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/StandingInstructionAction.java:466");
					// lanjutkan ke bank berikutnya jika format JSON tidak valid
				}

				Long idS = jsonObjectData == null || jsonObjectData.isNull("si")
						|| jsonObjectData.get("si").toString().trim().isEmpty() ? null
								: Long.parseLong(jsonObjectData.get("si").toString().trim());

				if (idS != null) {
					ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) session
							.createCriteria(ProsesTransferStandingInstruction.class).add(Restrictions.idEq(idS))
							.uniqueResult();
					if (prosesTransferStandingInstruction != null) {

						Double nilai = jsonObjectData.isNull("nilai") ? 0.0 : jsonObjectData.getDouble("nilai");

						String t = (bank == null ? "Tidak ada bank" : "Transfer ke " + bank.getNama()) + " "
								+ Common.numberFormat.get().format(nilai);

						transfer += transfer.isEmpty() ? t : ", " + t;
					}
				}
			}

			new Label((!transfer.isEmpty() ? transfer : ("Diajukan"))).setParent(arg0);

			final Textbox keterangan = new Textbox(standingInstruction.getKeterangan());
			keterangan.setWidth("95%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					standingInstruction.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(standingInstruction);
				}
			});

			if (standingInstruction.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(arg0);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + standingInstruction.getDisposisiSop().getKeterangan() + " ("
						+ standingInstruction.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(standingInstruction.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			} else {
				new Label().setParent(arg0);
			}

		}

	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} untuk query data
	 * {@link StandingInstruction} sesuai filter yang aktif di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun criteria dengan kondisi-kondisi berikut:
	 * <ul>
	 *   <li><b>Filter satuan kerja:</b> Jika filter {@code searchparent} terisi,
	 *       membatasi ke satuan kerja yang dipilih beserta seluruh turunannya.
	 *       Kondisi OR memperhitungkan SI tanpa satuan kerja ({@code isNull}) dan
	 *       SI dalam hierarki satuan kerja ({@code in satuanKerjas}).</li>
	 *   <li><b>Filter rentang tanggal:</b> SQL raw pada field {@code waktu} dengan
	 *       format {@code date()} untuk perbandingan tanggal. Fallback ke {@code 1=1}
	 *       jika salah satu datebox null.</li>
	 *   <li><b>Filter nama/kode:</b> ILIKE OR pada field {@code kode} dan {@code nama}.</li>
	 *   <li><b>Pengurutan:</b> Jika {@code order=true}, berdasarkan {@code id} descending.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * <b>Riwayat &mdash; filter status transfer TIDAK dibangun di sini (diperbaiki setelah
	 * audit 2026-09).</b> Sampai audit tersebut, method ini juga mencoba menyaring status
	 * transfer (searchbelum/searchsudah/searchsudahtrnf) lewat
	 * {@code criteria.createAlias("prosesTransferStandingInstruction", ...)} dan
	 * {@code Restrictions.eq("transfer", true)}. Kedua nama itu <b>bukan</b> properti Hibernate
	 * yang ada di {@link StandingInstruction}, {@code DataSop}, maupun
	 * {@code GeneralValueObject} &mdash; relasi ke {@link ProsesTransferStandingInstruction}
	 * di kelas itu murni JSON teks ({@code transferVia}), bukan {@code @ManyToOne}. Akibatnya
	 * mencentang checkbox {@code searchbelum} atau {@code searchsudahtrnf} melempar
	 * {@code QueryException} saat query dijalankan. Kondisi gerbangnya pun keliru
	 * ({@code searchbelum.isChecked() || searchbelum.isChecked() || searchsudahtrnf.isChecked()}
	 * &mdash; {@code searchbelum} diuji dua kali, {@code searchsudah} tidak pernah muncul di
	 * kondisi gerbang), sehingga mencentang <b>hanya</b> {@code searchsudah} membuat seluruh
	 * blok filter status tidak pernah dieksekusi (bukan error, tapi filter diam-diam diabaikan
	 * dan semua status ikut tampil). Filter status kini dihitung dan diterapkan di Java oleh
	 * {@code StandingInstructionAction.onSearchDefault(Event)} lewat
	 * {@link #hitungStatusTransfer(Session, StandingInstruction)}, bukan di {@link Criteria}
	 * ini &mdash; lihat javadoc kedua method tersebut.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Null-safe menggunakan {@code sqlRestriction("1=1")} atau {@code sqlRestriction("true")}
	 * sebagai fallback. Exception dari Hibernate diteruskan ke pemanggil.
	 * Log debug {@code System.out.println} digunakan untuk tracing filter satuan kerja.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Hapus {@code System.out.println} saat pindah ke production jika log terlalu verbose.
	 * Criteria yang dikembalikan di sini <b>tidak lengkap</b> untuk keperluan tampilan grid
	 * ketika filter status aktif &mdash; jangan panggil method ini secara langsung untuk
	 * menghitung jumlah baris final tanpa juga menerapkan
	 * {@link #hitungStatusTransfer(Session, StandingInstruction)} seperti yang dilakukan
	 * {@code onSearchDefault(Event)}.</p>
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY, {@code false} untuk COUNT
	 * @return objek {@link Criteria} untuk filter satuan kerja/tanggal/nama saja; filter
	 *         status transfer TIDAK termasuk (lihat catatan riwayat di atas)
	 */
	public Criteria initCriteria(boolean order) {
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		System.out.println("satuanKerjas -> " + satuanKerjas);

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(StandingInstruction.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.waktu) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

		;

		// Filter status transfer (searchbelum/searchsudah/searchsudahtrnf) TIDAK diterapkan di
		// sini lagi -- lihat javadoc method ini dan #hitungStatusTransfer(Session,
		// StandingInstruction). Relasi ke ProsesTransferStandingInstruction hanya berupa JSON
		// (StandingInstruction#getTransferVia()), bukan properti Hibernate bernama
		// "prosesTransferStandingInstruction"/"transfer", sehingga tidak bisa disaring lewat
		// Criteria di sini; filter status diterapkan belakangan di Java oleh
		// #onSearchDefault(Event).

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data standing instruction berdasarkan
	 * filter aktif dan memperbarui tampilan grid dengan hasil yang ditemukan,
	 * disertai pembaruan komponen paging.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Bila tidak ada satu pun checkbox status ({@code searchbelum}/{@code searchsudah}/
	 * {@code searchsudahtrnf}) yang dicentang, mengikuti jalur cepat lama: dua panggilan
	 * {@link #initCriteria(boolean)} (satu untuk {@code Common.initPaging} menghitung total
	 * record lewat {@code COUNT} di DB, satu lagi dengan {@code setMaxResults}/
	 * {@code setFirstResult} untuk halaman aktif).</p>
	 *
	 * <p>
	 * Bila minimal satu checkbox status dicentang, filter status <b>tidak</b> bisa diwakili
	 * sebagai {@link Criteria} (lihat javadoc {@link #initCriteria(boolean)}), sehingga
	 * jalurnya berbeda: mengambil <b>seluruh</b> baris yang lolos filter satuan
	 * kerja/tanggal/nama tanpa paging DB, menghitung status tiap baris lewat
	 * {@link #hitungStatusTransfer(Session, StandingInstruction)} (logika yang sama dengan
	 * yang dipakai {@code StandingInstructionRenderer} untuk menampilkan kolom status),
	 * menyaringnya di Java sesuai checkbox yang dicentang, lalu memotong hasil tersaring
	 * itu sendiri menjadi satu halaman. {@code paging.setTotalSize(...)} diisi manual dari
	 * jumlah hasil tersaring karena totalnya tidak lagi berasal dari {@code COUNT} SQL.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke framework ZK. Jika paging null,
	 * data dimulai dari indeks 0 (halaman pertama).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari timer default (refresh berkala), listener paging,
	 * listener filter, dan inisialisasi awal. Pastikan rendering tidak terlalu
	 * lambat karena dipanggil berulang dari timer; pertimbangkan mekanisme
	 * debouncing jika refresh terlalu sering. Jalur status-aktif melakukan query N+1 ke
	 * {@link ProsesTransferStandingInstruction} per entri {@code transferVia} (sama seperti
	 * renderer) dan mengambil seluruh baris tanpa paging DB -- bisa berat pada dataset besar
	 * yang sekaligus difilter status; pertimbangkan indeks/cache bila jadi masalah performa.</p>
	 *
	 * @param event event ZK pemicu pencarian (bisa null jika dipanggil programatik,
	 *              seperti dari timer atau inisialisasi)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		boolean filterStatusAktif = searchbelum.isChecked() || searchsudah.isChecked() || searchsudahtrnf.isChecked();

		if (!filterStatusAktif) {
			Common.initPaging(initCriteria(false), paging);

			List<StandingInstruction> standingInstruction = initCriteria(true)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(standingInstruction);
			grid.setRowRenderer(new StandingInstructionRenderer());
			grid.setModelCheckMobile(strset);
			return;
		}

		Session session = HibernateUtil.currentSession();
		List<StandingInstruction> semua = initCriteria(true).list();

		List<StandingInstruction> tersaring = new ArrayList<StandingInstruction>();
		for (StandingInstruction standingInstruction : semua) {
			String status = hitungStatusTransfer(session, standingInstruction);
			if ((searchbelum.isChecked() && "BELUM".equals(status))
					|| (searchsudah.isChecked() && "SUDAH".equals(status))
					|| (searchsudahtrnf.isChecked() && "SUDAHTRNF".equals(status))) {
				tersaring.add(standingInstruction);
			}
		}

		if (paging != null) {
			paging.setTotalSize(tersaring.size());
		}

		int dariIndeks = Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage());
		dariIndeks = Math.min(dariIndeks, tersaring.size());
		int sampaiIndeks = Math.min(tersaring.size(), dariIndeks + Common.ROWS_COUNT_ON_PAGE);

		ListModel strset = new SimpleListModel(tersaring.subList(dariIndeks, sampaiIndeks));
		grid.setRowRenderer(new StandingInstructionRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * Menghitung status proses transfer sebuah {@link StandingInstruction} dari JSON
	 * {@link StandingInstruction#getTransferVia()}, tanpa bergantung pada properti Hibernate
	 * yang tidak ada (lihat javadoc {@link #initCriteria(boolean)}).
	 *
	 * <p>
	 * <b>Cara kerja (sengaja dibuat identik dengan
	 * {@code NewUiStandingInstructionService.row()}</b>, versi headless dari layar ini, agar
	 * kedua layar selalu sepakat soal status SI yang sama): mengiterasi setiap entri JSON.
	 * Bila sebuah entri punya referensi {@code "si"} tidak kosong, baris dianggap sudah masuk
	 * proses ({@code has}); bila {@link ProsesTransferStandingInstruction} yang dirujuknya
	 * punya {@code realisasikanOleh} terisi, baris dianggap sudah ditransfer ({@code done}).
	 * Sebuah SI dengan beberapa entri bank campuran (sebagian sudah direalisasi, sebagian
	 * belum) dihitung {@code SUDAHTRNF} begitu <b>salah satu</b> entrinya terealisasi --
	 * penyederhanaan yang sama seperti yang sudah dipakai {@code NewUiStandingInstructionService},
	 * bukan hal baru yang diperkenalkan di sini.
	 * </p>
	 *
	 * @param session sesi Hibernate aktif untuk memuat {@link ProsesTransferStandingInstruction}
	 *                yang dirujuk
	 * @param standingInstruction baris yang akan dihitung statusnya
	 * @return {@code "BELUM"} (belum diproses sama sekali), {@code "SUDAH"} (sudah masuk
	 *         proses transfer tapi belum direalisasikan), atau {@code "SUDAHTRNF"} (sudah
	 *         direalisasikan/ditransfer)
	 */
	private String hitungStatusTransfer(Session session, StandingInstruction standingInstruction) {
		boolean ada = false;
		boolean selesai = false;
		try {
			JSONObject jsonObjectTransfer = new JSONObject(standingInstruction.getTransferVia());
			Iterator<String> iterator = jsonObjectTransfer.keys();
			while (iterator.hasNext()) {
				String d = iterator.next();
				JSONObject jsonObjectData = jsonObjectTransfer.isNull(d) ? null : jsonObjectTransfer.getJSONObject(d);
				Long idS = jsonObjectData == null || jsonObjectData.isNull("si")
						|| jsonObjectData.get("si").toString().trim().isEmpty() ? null
								: Long.parseLong(jsonObjectData.get("si").toString().trim());
				if (idS != null) {
					ada = true;
					ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) session
							.createCriteria(ProsesTransferStandingInstruction.class).add(Restrictions.idEq(idS))
							.uniqueResult();
					if (prosesTransferStandingInstruction != null
							&& prosesTransferStandingInstruction.getRealisasikanOleh() != null) {
						selesai = true;
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/master/akunting/StandingInstructionAction.java:hitungStatusTransfer");
		}

		if (selesai) {
			return "SUDAHTRNF";
		}
		return ada ? "SUDAH" : "BELUM";
	}

}
