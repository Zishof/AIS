package ais.action.master.feeder.integrator.helper;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.NeoFeederProgressHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * {@code NeoFeederWindowBase} — kelas dasar (base class) yang dapat dipakai bersama oleh SELURUH
 * jendela Download/Upload Neo Feeder di paket {@code integrator.helper}. Sekitar 40 jendela tersebut
 * selama ini menduplikasi pola yang sama berulang-ulang: menyusun kerangka window (borderlayout
 * North berisi kartu filter + Center berisi hasil), membangun toolbar aksi ("Tampilkan Data",
 * "Ambil Data", "Kirim ke Feeder"), membuka koneksi + token ke Feeder, menjalankan proses berat di
 * thread latar dengan indikator progres, serta mengelola session Hibernate. Base ini memusatkan
 * semua pola itu menjadi beberapa helper yang teruji sehingga tiap jendela cukup fokus pada logika
 * spesifiknya (query, kolom, dan pemetaan data) — inti strategi "maksimalkan reuse untuk memudahkan
 * pemeliharaan".
 *
 * <h3>Yang disediakan base ini</h3>
 * <ol>
 *   <li><b>Kerangka window responsif</b> — {@link #initScaffold(String)} membangun
 *       {@link MyBorderlayout} dengan {@link North} (kartu filter modern berisi {@link Grid} filter
 *       + {@link Toolbar}) dan {@link Center} (area hasil). {@link #addFilterRow()} menambah baris
 *       filter, {@link #addToolbarButton(String, String, EventListener)} menambah tombol aksi.</li>
 *   <li><b>Progress popup melayang</b> — {@link #runFeederTask(String, FeederTask)} menjalankan
 *       pekerjaan berat (kirim/ambil data) di thread latar sambil menampilkan popup persentase
 *       melayang lewat {@link NeoFeederProgressHelper}. Tugas cukup menulis progres ke handle
 *       (mis. {@code progres.setValue("Memproses X (50%)")}); base menangani penutupan popup,
 *       penanganan error, dan pemanggilan {@link #onSelesaiTask(Event)}.</li>
 *   <li><b>Koneksi Feeder</b> — {@link #konek()} membuka koneksi, memeriksa keterjangkauan server,
 *       dan mengambil token dalam satu langkah; melempar pesan yang jelas bila gagal.</li>
 *   <li><b>Template session aman (session di blok {@code finally})</b> —
 *       {@link #denganSessionNative(KerjaSession)} dan {@link #denganSessionBaru(KerjaSession)}
 *       membuka session ({@code currentNativeSession()} / {@code openSession()}), menjalankan kerja
 *       pemanggil, lalu <b>menutup session di {@code finally}</b> sehingga tidak pernah bocor walau
 *       terjadi exception. Session yang dibuka via {@code currentSession()} TIDAK ada di sini karena
 *       memang tidak boleh ditutup manual (dikelola siklus request ZK).</li>
 * </ol>
 *
 * <h3>Cara adopsi (inkremental, tanpa regresi)</h3>
 * Base ini bersifat opsional: jendela yang ada tetap berjalan apa adanya sampai dimigrasikan.
 * Jendela baru cukup {@code extends NeoFeederWindowBase}, memanggil {@link #initScaffold(String)}
 * di konstruktor, menambah filter &amp; tombol, lalu membungkus proses berat dengan
 * {@link #runFeederTask(String, FeederTask)} dan akses basis data dengan
 * {@link #denganSessionNative(KerjaSession)} / {@link #denganSessionBaru(KerjaSession)}. Migrasi
 * jendela lama dilakukan bertahap dan diverifikasi per jendela agar perilaku tetap sama.
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis agar kompatibel dengan <b>Java 1.7</b> (memakai anonymous class / interface callback,
 * tanpa lambda) dan <b>ZK 5.5</b>.
 *
 * @author Tim AIS
 */
public abstract class NeoFeederWindowBase extends MyWindow {

	private static final long serialVersionUID = 1L;

	/** Layout utama window (North = filter, Center = hasil). */
	protected MyBorderlayout borderlayout;
	/** Area atas berisi kartu filter + toolbar. */
	protected North north;
	/** Area tengah berisi hasil (grid/spreadsheet). */
	protected Center center;
	/** Grid tempat baris-baris filter disusun. */
	protected Grid filterGrid;
	/** Kumpulan baris filter di dalam {@link #filterGrid}. */
	protected Rows filterRows;
	/** Toolbar aksi (Tampilkan/Ambil/Kirim). */
	protected Toolbar toolbar;

	/** Konstruktor default. */
	public NeoFeederWindowBase() {
		super();
	}

	/**
	 * Konstruktor dengan judul/border/closable (diteruskan ke {@link MyWindow}).
	 *
	 * @param title    judul window
	 * @param border   gaya border
	 * @param closable dapat ditutup atau tidak
	 */
	public NeoFeederWindowBase(String title, String border, boolean closable) {
		super(title, border, closable);
	}

	// =====================================================================================
	// KERANGKA WINDOW
	// =====================================================================================

	/**
	 * Membangun kerangka window: {@link MyBorderlayout} dengan North (kartu filter modern berisi
	 * {@link #filterGrid} + {@link #toolbar}) dan Center (area hasil). Aman dipanggil sekali di
	 * konstruktor subkelas.
	 *
	 * @param judulFilter judul kartu filter (boleh {@code null} untuk tanpa judul)
	 */
	protected void initScaffold(String judulFilter) {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		borderlayout = new MyBorderlayout();
		borderlayout.setParent(this);

		north = new North();
		north.setParent(borderlayout);
		ZkCompat.setFlex(north, true);

		Div card = new Div();
		card.setParent(north);
		card.setStyle("background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:12px 14px;margin:6px;"
				+ "font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;");

		if (judulFilter != null && judulFilter.trim().length() > 0) {
			Label t = new Label(judulFilter);
			t.setStyle("display:block;font-size:13px;font-weight:700;color:#0f172a;margin-bottom:8px;");
			t.setParent(card);
		}

		filterGrid = new MyGrid();
		filterGrid.setWidth("100%");
		filterGrid.setParent(card);
		filterRows = new Rows();
		filterRows.setParent(filterGrid);

		toolbar = new Toolbar();
		toolbar.setStyle("border:none;background:transparent;padding:6px 0 0 0;");
		toolbar.setParent(card);

		center = new Center();
		center.setParent(borderlayout);
		ZkCompat.setFlex(center, true);
	}

	/**
	 * Menambah satu baris filter kosong ke kartu filter. Subkelas mengisi label + komponen input.
	 *
	 * @return baris {@link MyFormRow} yang sudah terpasang
	 */
	protected MyFormRow addFilterRow() {
		MyFormRow row = new MyFormRow();
		try {
			row.setValign("top");
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/NeoFeederWindowBase.java:165");
		}
		row.setParent(filterRows);
		return row;
	}

	/**
	 * Menambah tombol aksi ke toolbar filter.
	 *
	 * @param label   teks tombol
	 * @param icon    path ikon (boleh {@code null})
	 * @param onClick listener klik (boleh {@code null})
	 * @return tombol {@link MyToolbarbuttonConfig} yang sudah terpasang
	 */
	protected MyToolbarbuttonConfig addToolbarButton(String label, String icon, EventListener onClick) {
		MyToolbarbuttonConfig btn = (icon == null) ? new MyToolbarbuttonConfig(label)
				: new MyToolbarbuttonConfig(label, icon);
		if (onClick != null) {
			btn.addEventListener("onClick", onClick);
		}
		btn.setParent(toolbar);
		return btn;
	}

	// =====================================================================================
	// PROGRES + THREAD LATAR
	// =====================================================================================

	/** Pekerjaan feeder yang berjalan di thread latar; menulis progres ke {@code progres}. */
	public interface FeederTask {
		/**
		 * @param progres handle progres; tulis {@code setValue("... (NN%)")} untuk memperbarui bar,
		 *                base otomatis menutup popup saat tugas selesai/gagal
		 * @throws Exception bila terjadi kegagalan (ditangani base: popup jadi status error + log)
		 */
		void run(Label progres) throws Exception;
	}

	/**
	 * Menjalankan {@code task} di thread latar dengan popup progres melayang
	 * ({@link NeoFeederProgressHelper}). Base menangani penutupan popup, penanganan error (popup
	 * menjadi status error + {@code tampilErrorJikaAdmin}), dan memanggil {@link #onSelesaiTask(Event)}
	 * saat selesai. Tugas cukup fokus pada proses kirim/ambil dan menulis progres.
	 *
	 * @param judul judul popup (mis. "Mengirim Data ke Neo Feeder")
	 * @param task  pekerjaan yang dijalankan
	 */
	protected void runFeederTask(String judul, final FeederTask task) {
		final Label progres = NeoFeederProgressHelper.show(judul, new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				onSelesaiTask(e);
			}
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					task.run(progres);
				} catch (Exception ex) {
					// FIX IllegalStateException "Components can be accessed only in event
					// listeners": .setValue() langsung dari thread latar tidak aman (lihat
					// NeoFeederProgressHelper.updateProgres).
					try {
						NeoFeederProgressHelper.updateProgres(progres,
								"Error: " + (ex.getMessage() == null ? "terjadi kesalahan" : ex.getMessage()));
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/NeoFeederWindowBase.java:228");
					}
					Common.tampilErrorJikaAdmin(ex);
					return;
				}
				try {
					NeoFeederProgressHelper.updateProgres(progres, "");
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/NeoFeederWindowBase.java:235");
				}
			}
		}).start();
	}

	/**
	 * Kait (hook) opsional yang dipanggil di UI thread saat {@link #runFeederTask} selesai/gagal.
	 * Subkelas boleh meng-override untuk mis. me-refresh grid hasil. Implementasi default kosong.
	 *
	 * @param e {@code null} saat sukses; {@code Event} berisi teks error saat gagal
	 * @throws Exception bila handler melempar
	 */
	protected void onSelesaiTask(Event e) throws Exception {
	}

	// =====================================================================================
	// KONEKSI FEEDER
	// =====================================================================================

	/** Pembungkus hasil koneksi: connector + token yang sudah tervalidasi. */
	public static final class FeederAkses {
		/** Konektor Feeder siap pakai. */
		public final FeederConnector connector;
		/** Token autentikasi yang valid. */
		public final String token;

		FeederAkses(FeederConnector connector, String token) {
			this.connector = connector;
			this.token = token;
		}
	}

	/**
	 * Membuka koneksi ke Neo Feeder dan mengambil token dalam satu langkah: membaca konfigurasi
	 * ({@link EksporFromFeederAction#koneksi()}), memeriksa keterjangkauan server, lalu login.
	 *
	 * @return {@link FeederAkses} berisi connector + token valid
	 * @throws Exception bila server tak terjangkau atau login gagal (pesan siap tampil ke pengguna)
	 */
	protected FeederAkses konek() throws Exception {
		String[] kon = EksporFromFeederAction.koneksi();
		if (!EksporFromFeederAction.exists(kon[4])) {
			throw new Exception("Server Feeder pada " + kon[0] + ":" + kon[1]
					+ " tidak dapat diakses. Periksa IP/Port/koneksi jaringan pada Pengaturan Koneksi.");
		}
		FeederConnector fc = new FeederConnector(kon[0], Integer.parseInt(kon[1]));
		String token = fc.getToken(kon[2], kon[3]);
		if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
			throw new Exception("Gagal login ke Feeder. Periksa username/password pada Pengaturan Koneksi.");
		}
		return new FeederAkses(fc, token);
	}

	// =====================================================================================
	// TEMPLATE SESSION AMAN (session ditutup di finally)
	// =====================================================================================

	/** Blok kerja yang memakai sebuah {@link Session} Hibernate. */
	public interface KerjaSession {
		/**
		 * @param session session aktif yang siklus hidupnya dikelola pemanggil template
		 * @throws Exception bila kerja melempar
		 */
		void run(Session session) throws Exception;
	}

	/**
	 * Menjalankan {@code kerja} memakai native session thread-local
	 * ({@code HibernateUtil.currentNativeSession()}) dan <b>menutupnya di blok {@code finally}</b>
	 * ({@code HibernateUtil.closeSession()}) sehingga session tidak pernah bocor walau terjadi
	 * exception. Pola inilah yang disarankan untuk mengganti pola "buka lalu tutup di akhir method"
	 * yang rawan lewat saat error.
	 *
	 * @param kerja blok kerja yang memakai session
	 * @throws Exception meneruskan exception dari {@code kerja} (setelah session tetap ditutup)
	 */
	protected static void denganSessionNative(KerjaSession kerja) throws Exception {
		HibernateUtil.currentNativeSession();
		try {
			kerja.run(HibernateUtil.currentNativeSession());
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menjalankan {@code kerja} memakai session DEDIKASI baru ({@code HibernateUtil.openSession()})
	 * dan <b>menutupnya di blok {@code finally}</b> ({@code Common.closeOpenedSession}). Dipakai bila
	 * pekerjaan tidak boleh menumpang session request/thread-local (mis. commit independen di thread
	 * latar).
	 *
	 * @param kerja blok kerja yang memakai session
	 * @throws Exception meneruskan exception dari {@code kerja} (setelah session tetap ditutup)
	 */
	protected static void denganSessionBaru(KerjaSession kerja) throws Exception {
		Session session = HibernateUtil.openSession();
		try {
			kerja.run(session);
		} finally {
			ais.common.Common.closeOpenedSession(session);
		}
	}
}
