package ais.common;

import java.io.File;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

// Pastikan import class "Common", "ExecutionsCtrl", "Spreadsheet", "ais.ui.util.MySpreadsheet" sesuai package Anda

/**
 * Utilitas ZK untuk menampilkan <b>indikator progres/loading</b> ("Harap tunggu, sedang
 * menyiapkan data ..") di atas komponen halaman selagi suatu proses latar belakang berjalan (mis.
 * menyiapkan laporan Excel besar, memuat data berat), lalu secara otomatis membersihkan indikator
 * tersebut begitu proses selesai atau gagal. Kelas ini murni statis (kumpulan method utilitas,
 * tanpa instance state) dan seluruhnya dibangun di atas pola polling {@link org.zkoss.zul.Timer}
 * ZK: sebuah {@link org.zkoss.zul.Label} tersembunyi dipakai sebagai "kotak surat" status —
 * dimutakhirkan oleh thread/proses pemanggil di luar timer (mis. lewat thread background yang
 * mengubah {@code label.setValue(...)}), lalu dibaca berkala oleh listener {@code onTimer} yang
 * dipasang lewat {@link #createAndStartTimer(Component, int, EventListener)}.
 *
 * <h2>Pola nilai status pada label</h2>
 * <p>
 * Nilai teks pada {@link org.zkoss.zul.Label} yang dikembalikan tiap method {@code displayLoadBar*}
 * berperan sebagai mesin status sederhana, dibaca ulang setiap kali timer menyala:
 * </p>
 * <ul>
 * <li>Teks biasa (mis. {@code "Harap tunggu, sedang menyiapkan data .."}) — proses masih berjalan;
 * ditampilkan lewat {@link Clients#showBusy(String)} sebagai overlay sibuk ZK.</li>
 * <li>String kosong — proses dianggap selesai (pada varian
 * {@link #displayLoadBar(Component, EventListener)}/{@link #displayLoadBar(Component, File)}/
 * {@link #displayLoadBar(Component, File, Component, Intbox, Intbox)}) atau dalam kondisi "tanpa
 * data" (pada {@link #displayLoadBarjanganBerhenti(EventListener)}).</li>
 * <li>{@code "Error"} (bandingkan lewat {@code equalsIgnoreCase}/{@code startsWith}) — proses gagal;
 * memicu {@code eventListener} dipanggil dengan sebuah {@link Event} berisi pesan status
 * tersebut.</li>
 * <li>{@code "Selesai"} — khusus {@link #displayLoadBarjanganBerhenti(EventListener)}, penanda
 * selesai eksplisit yang berbeda dari string kosong.</li>
 * </ul>
 * <p>
 * Pemanggil bertanggung jawab memutakhirkan nilai label ini dari proses latar belakangnya sendiri
 * (kelas ini TIDAK menjalankan proses apa pun secara langsung, hanya mengamati perubahan pada
 * label yang dikembalikannya).
 * </p>
 *
 * <h2>Dua keluarga overload {@code displayLoadBar}</h2>
 * <p>
 * Ada dua kelompok method publik yang meski bernama sama, dirancang untuk skenario berbeda:
 * varian tanpa {@link File} ({@link #displayLoadBar(EventListener)},
 * {@link #displayLoadBar(Component)}, {@link #displayLoadBar(Component, EventListener)}) murni
 * menunggu proses generik selesai/gagal dan meneruskan kendali ke {@code eventListener} pemanggil;
 * sedangkan varian dengan {@link File} ({@link #displayLoadBar(Component, File)},
 * {@link #displayLoadBar(Component, File, Component, Intbox, Intbox)}) KHUSUS menunggu berkas
 * Excel/spreadsheet selesai disiapkan lalu langsung menindaklanjuti berkas tersebut sendiri —
 * masing-masing dengan cara berbeda: memicu unduhan lewat {@link Filedownload#save(File, String)},
 * atau merender pratinjaunya ke komponen {@code center} lewat
 * {@link #loadSpreadsheet(Component, File, Intbox, Intbox)}.
 * </p>
 *
 * <h2>Catatan konsistensi perilaku (didokumentasikan apa adanya)</h2>
 * <p>
 * Komentar asli pada {@link #displayLoadBarjanganBerhenti(EventListener)} mencatat bahwa perilaku
 * detach timer pada status {@code "Error"} dan status kosong TIDAK sepenuhnya konsisten dengan
 * versi kode sebelumnya (lihat komentar inline di badan method tersebut) — dicatat di sini sebagai
 * observasi atas kode yang ada, bukan diubah sebagai bagian dari pekerjaan dokumentasi ini.
 * </p>
 */
public class LoadBarUtils {

	/** Pesan status default yang ditampilkan selagi proses masih berjalan ("proses belum selesai"). */
	private static final String MSG_WAIT = "Harap tunggu, sedang menyiapkan data ..";
	/** Nilai status penanda kegagalan proses; dicocokkan case-insensitive terhadap nilai label. */
	private static final String STATUS_ERROR = "Error";
	/** Nilai status penanda proses selesai secara eksplisit, khusus dipakai {@link #displayLoadBarjanganBerhenti(EventListener)}. */
	private static final String STATUS_DONE = "Selesai";
	/** Jeda polling timer default dalam milidetik untuk sebagian besar varian {@code displayLoadBar}. */
	private static final int DEFAULT_DELAY = 500;

	// -------------------------------------------------------------------------
	// Public Methods
	// -------------------------------------------------------------------------

	/** Seperti {@link #displayLoadBar(Component, EventListener)}, dengan {@code parent} diambil otomatis dari root halaman ZK yang sedang aktif. */
	public static Label displayLoadBar(final EventListener eventListener) {
		Component root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		return displayLoadBar(root, eventListener);
	}

	/** Seperti {@link #displayLoadBar(Component, EventListener)}, tanpa {@code eventListener} (proses selesai/gagal tidak memicu callback). */
	public static Label displayLoadBar(Component parent) {
		// Delegasi ke method utama dengan listener null
		return displayLoadBar(parent, (EventListener) null);
	}

	/**
	 * Varian kanonik: membuat {@link Label} status (awal berisi {@link #MSG_WAIT}) sebagai anak
	 * {@code parent}, lalu memasang timer polling ({@link #DEFAULT_DELAY} ms) yang membaca nilai
	 * label tersebut setiap kali menyala. Selama nilainya bukan kosong dan bukan {@link
	 * #STATUS_ERROR}, overlay sibuk ZK terus dimutakhirkan lewat {@link Clients#showBusy(String)}.
	 * Begitu nilainya kosong (dianggap selesai) atau berupa {@link #STATUS_ERROR}, overlay sibuk
	 * dibersihkan, timer di-detach, dan {@code eventListener} (bila ada) dipanggil sekali — dengan
	 * {@link Event} berisi pesan galat bila kondisinya error, atau {@code null} bila kondisinya
	 * "selesai kosong".
	 *
	 * @param parent        komponen ZK induk tempat {@link Label} dan {@link Timer} dipasang; bila
	 *                      {@code null}, jatuh ke root halaman aktif (lihat
	 *                      {@link #createAndStartTimer(Component, int, EventListener)})
	 * @param eventListener dipanggil sekali saat proses selesai/gagal; boleh {@code null} bila
	 *                      pemanggil hanya memantau label secara visual tanpa perlu callback
	 * @return {@link Label} status yang harus dimutakhirkan oleh proses latar belakang pemanggil
	 *         untuk mengendalikan kapan indikator loading berhenti
	 */
	public static Label displayLoadBar(Component parent, final EventListener eventListener) {
		final Label label = createBaseLabel(MSG_WAIT);

		// Timer Logic
		createAndStartTimer(parent, DEFAULT_DELAY, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Timer timer = (Timer) event.getTarget();
				String status = label.getValue();

				// Update pesan busy sesuai label (agar dinamis)
				Clients.showBusy(status);

				boolean isError = status.equalsIgnoreCase(STATUS_ERROR) || status.startsWith(STATUS_ERROR);
				boolean isEmpty = status.isEmpty() || status.toLowerCase().trim().contains("selesai");

				if (isError || isEmpty) {
					Clients.clearBusy();
					timer.detach(); // Stop timer

					if (eventListener != null) {
						// Jika Error kirim event dengan pesan, jika Empty kirim null
						eventListener.onEvent(isError ? new Event(status) : null);
					}
				}
			}
		});

		return label;
	}

	/**
	 * Varian {@code displayLoadBar} yang labelnya dimulai KOSONG (bukan {@link #MSG_WAIT}) dan
	 * membedakan tiga kondisi status secara eksplisit: {@link #STATUS_DONE} ("Selesai" — proses
	 * benar-benar tuntas, timer di-detach dan {@code eventListener} dipanggil dengan status ini),
	 * {@link #STATUS_ERROR} ("Error" — timer di-detach dan {@code eventListener} dipanggil dengan
     * status ini), dan string kosong (dianggap kondisi "tanpa data": overlay sibuk dibersihkan dan
	 * {@code eventListener} dipanggil dengan {@code null}, TAPI timer TIDAK di-detach — proses
	 * polling terus berlanjut, sesuai catatan pada javadoc kelas soal ketidakkonsistenan
	 * perilaku yang sengaja dipertahankan apa adanya dari kode asli). Nilai lain (teks status
	 * biasa) hanya memutakhirkan overlay sibuk lewat {@link Clients#showBusy(String)}.
	 *
	 * @param eventListener dipanggil saat status berubah menjadi selesai/error/kosong; boleh
	 *                      {@code null}
	 * @return {@link Label} status yang harus dimutakhirkan oleh proses latar belakang pemanggil
	 */
	public static Label displayLoadBarjanganBerhenti(final EventListener eventListener) {
		final Label label = createBaseLabel("");
		Component parent = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();

		createAndStartTimer(parent, DEFAULT_DELAY, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Timer timer = (Timer) event.getTarget();
				String status = label.getValue();

				if (status.equalsIgnoreCase(STATUS_DONE)) {
					Clients.clearBusy();
					timer.detach();
					safeTriggerEvent(eventListener, STATUS_DONE);
				} else if (status.equalsIgnoreCase(STATUS_ERROR)) {
					Clients.clearBusy();
					// Catatan: Kode asli tidak men-detach timer di sini (looping terus),
					// tapi biasanya harus detach. Jika ingin behavior asli, hapus baris di bawah.
					timer.detach();
					safeTriggerEvent(eventListener, STATUS_ERROR);
				} else if (status.isEmpty()) {
					Clients.clearBusy();
					safeTriggerEvent(eventListener, null); // Kirim null event
					// Catatan: Kode asli juga tidak detach di sini untuk 'isEmpty' kecuali logic
					// flow berbeda
				} else {
					Clients.showBusy(status);
				}
			}
		});

		return label;
	}

	/**
	 * Varian {@code displayLoadBar} untuk menunggu satu berkas ({@code fileName}, biasanya laporan
	 * Excel) selesai disiapkan oleh proses latar pemanggil, lalu SEGERA memicu pengunduhannya lewat
	 * {@link Filedownload#save(File, String)} dengan tipe MIME
	 * {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet} (XLSX) begitu label
	 * status menjadi kosong (dianggap selesai). Bila berkas tidak ada/{@code null} saat proses
	 * dianggap selesai, unduhan dilewati begitu saja (tanpa galat) — hanya overlay sibuk dan timer
	 * yang dibersihkan.
	 *
	 * @param parent   komponen ZK induk tempat {@link Label} dan {@link Timer} dipasang
	 * @param fileName berkas XLSX yang akan diunduh begitu proses dianggap selesai; boleh
	 *                 {@code null} bila belum tentu ada berkas untuk diunduh
	 * @return {@link Label} status yang harus dikosongkan oleh proses latar belakang pemanggil saat
	 *         berkas sudah siap
	 */
	public static Label displayLoadBar(Component parent, final File fileName) {
		final Label label = createBaseLabel(MSG_WAIT);

		createAndStartTimer(parent, DEFAULT_DELAY, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					if (fileName != null && fileName.exists()) {
						Filedownload.save(fileName,
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
					}
					Clients.clearBusy();
					((Timer) event.getTarget()).detach();
				}
			}
		});

		return label;
	}

	// Menggabungkan dua method spreadsheet menjadi satu
	/** Seperti {@link #displayLoadBar(Component, File, Component, Intbox, Intbox)}, dengan {@code maxCol} default (70 kolom — lihat {@link #loadSpreadsheet}). */
	public static Label displayLoadBar(Component parent, final File file, final Component center, final Intbox sizedata) {
		// Default maxCol ke 70 jika tidak disediakan
		return displayLoadBar(parent, file, center, sizedata, null);
	}

	/**
	 * Varian {@code displayLoadBar} yang menunggu {@code file} spreadsheet selesai disiapkan, lalu
	 * (berbeda dari {@link #displayLoadBar(Component, File)} yang memicu unduhan) langsung
	 * me-render pratinjaunya ke komponen {@code center} lewat
	 * {@link #loadSpreadsheet(Component, File, Intbox, Intbox)}. Memakai jeda polling timer 200ms
	 * (lebih cepat dari {@link #DEFAULT_DELAY}) agar pratinjau tampil responsif begitu berkas siap.
	 * Kegagalan saat merender spreadsheet ditangkap dan hanya dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, tidak menggagalkan pembersihan overlay/timer.
	 *
	 * @param parent   komponen ZK induk tempat {@link Label} dan {@link Timer} dipasang
	 * @param file     berkas spreadsheet yang akan dirender pratinjaunya; bila {@code null}, tidak
	 *                 ada rendering yang dilakukan meski proses dianggap selesai
	 * @param center   komponen ZK tujuan tempat pratinjau spreadsheet dipasang
	 * @param sizedata jumlah baris data (dipakai untuk membatasi baris yang dirender dan menghitung
	 *                 tinggi tampilan), boleh {@code null}
	 * @param maxCol   jumlah kolom maksimum yang dirender; bila {@code null}, dipakai default 70
	 * @return {@link Label} status yang harus dikosongkan oleh proses latar belakang pemanggil saat
	 *         berkas sudah siap dirender
	 */
	public static Label displayLoadBar(Component parent, final File file, final Component center, final Intbox sizedata,
			final Intbox maxCol) {

		final Label label = createBaseLabel(MSG_WAIT);

		// Timer lebih cepat (200ms) sesuai kode asli
		createAndStartTimer(parent, 200, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Clients.showBusy(label.getValue());

				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					((Timer) event.getTarget()).detach();

					if (file != null) {
						try {
							loadSpreadsheet(center, file, sizedata, maxCol);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		});

		return label;
	}

	// -------------------------------------------------------------------------
	// Private Helper Methods (Untuk kebersihan kode)
	// -------------------------------------------------------------------------

	/** Membuat {@link Label} multiline dengan teks awal {@code initialText}; bila teks tidak kosong, langsung menampilkan overlay sibuk ZK lewat {@link Clients#showBusy(String)}. */
	private static Label createBaseLabel(String initialText) {
		Label label = new Label(initialText);
		label.setMultiline(true);
		if (initialText != null && !initialText.isEmpty()) {
			Clients.showBusy(initialText);
		}
		return label;
	}

	/**
	 * Membuat, memasang (sebagai anak {@code parent}, atau root halaman aktif bila {@code parent}
	 * {@code null}), dan langsung menyalakan sebuah {@link Timer} berulang ({@code setRepeats(true)})
	 * dengan jeda {@code delay} milidetik, dengan {@code listener} terpasang pada event
	 * {@code "onTimer"}. Dipakai bersama oleh seluruh varian {@code displayLoadBar} sebagai mesin
	 * polling status.
	 */
	private static void createAndStartTimer(Component parent, int delay, EventListener listener) {
		Timer timer = new Timer(delay);
		// Fallback jika parent null agar tidak error
		if (parent == null) {
			parent = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		}
		timer.setParent(parent);
		timer.setRepeats(true);
		timer.addEventListener("onTimer", listener);
		timer.start();
	}

	/** Memanggil {@code listener} (bila tidak {@code null}) dengan {@link Event} berisi {@code data}, atau dengan {@code null} bila {@code data} {@code null}; dipakai {@link #displayLoadBarjanganBerhenti(EventListener)} untuk menyeragamkan pemanggilan callback. */
	private static void safeTriggerEvent(EventListener listener, String data) throws Exception {
		if (listener != null) {
			listener.onEvent(data == null ? null : new Event(data));
		}
	}

	/**
	 * Merender pratinjau berkas spreadsheet {@code file} ke komponen {@code center}: membersihkan
	 * isi {@code center} lewat {@link Common#clear(Component)}, memasang komponen
	 * {@link ais.ui.util.MySpreadsheet} (widget ZSS) yang menunjuk ke salinan berkas di direktori
	 * sementara ({@code "../../tmp/" + file.getName()}), mengatur batas baris ({@code sizedata})
	 * dan kolom ({@code maxCol}, default 70 bila {@code null}) yang dirender, lalu menghitung
	 * tinggi tampilan dinamis: 2000px sebagai batas bawah (floor, bukan plafon), tumbuh mengikuti
	 * jumlah baris ({@code 17px/baris + 180}) hingga batas atas 20000px (di atasnya memakai scroll
	 * internal widget). Kegagalan styling (mis. gagal mengatur CSS parent) ditangkap dan diabaikan
	 * agar tidak menggagalkan rendering spreadsheet itu sendiri.
	 *
	 * <p>
	 * Sebagai langkah akhir, widget ZSS berat tersebut DIGANTIKAN tampilannya (bukan dilepas —
	 * {@code Book} internalnya tetap hidup agar berkas XLSX asli tetap utuh untuk kebutuhan unduhan
	 * terpisah oleh pemanggil) dengan komponen Grid ringan berpaginasi lewat
	 * {@link ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid(Component)} — optimasi
	 * beban render browser untuk spreadsheet bertinggi besar (2000–20000px), dipakai juga oleh
	 * puluhan dasbor lain di aplikasi (pola B, lihat komentar inline pada badan method).
	 * </p>
	 *
	 * @param center   komponen ZK tujuan tempat pratinjau spreadsheet dipasang
	 * @param file     berkas spreadsheet sumber
	 * @param sizedata jumlah baris data, dipakai untuk batas baris dan perhitungan tinggi; boleh
	 *                 {@code null}
	 * @param maxCol   jumlah kolom maksimum yang dirender; bila {@code null}, dipakai default 70
	 * @throws Exception diteruskan dari kegagalan memasang komponen {@link ais.ui.util.MySpreadsheet}
	 *                    atau {@link ais.ui.util.PratinjauXlsxHelper}
	 */
	private static void loadSpreadsheet(Component center, File file, Intbox sizedata, Intbox maxCol) throws Exception {
		Common.clear(center);

		// Menggunakan Reflection atau Interface jika MySpreadsheet tipe spesifik,
		// tapi di sini diasumsikan ada di classpath
		ais.ui.util.MySpreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();

		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("2000px");
		spreadsheet.setSrc("../../tmp/" + file.getName());

		if (sizedata != null) {
			spreadsheet.setMaxrows(sizedata.getValue());
		}

		// Logika default 70 jika maxCol null
		int columns = (maxCol != null) ? maxCol.getValue() : 70;
		spreadsheet.setMaxcolumns(columns);

		// Tinggi tampilan Excel (spreadsheet): dibuat MINIMAL 2000px (permintaan user) agar area data
		// tidak pendek/terpotong. Dulu tinggi tumbuh mengikuti jumlah baris TAPI di-CAP 2000px dan
		// min-height hanya 50px -> saat baris sedikit, area Excel jadi sangat pendek. Kini logika DIBALIK:
		// 2000px menjadi LANTAI (floor), bukan plafon; untuk data besar tinggi tumbuh mengikuti jumlah
		// baris (17px/baris + 180) sampai batas wajar 20000px (di atas itu pakai scroll internal
		// spreadsheet). min-height:2000px dipasang sebagai jaminan agar tak pernah lebih pendek dari 2000px.
		int tinggi = 2000;
		if (sizedata != null) {
			try {
				tinggi = (sizedata.getValue() * 17) + 180;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LoadBarUtils.java:221");
			}
		}
		if (tinggi < 2000) {
			tinggi = 2000;
		}
		if (tinggi > 20000) {
			tinggi = 20000;
		}
		String h = tinggi + "px";
		String style = "min-height:2000px;height:" + h + ";";
		try {
			if (center.getParent() instanceof HtmlBasedComponent) {
				((HtmlBasedComponent) center.getParent()).setStyle("min-height:2000px;");
			}
			spreadsheet.setStyle(style);
			spreadsheet.setHeight(h);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LoadBarUtils.java:238");
			// Abaikan galat styling seperti kode asli
		}

		// FIX beban widget zss Spreadsheet (berat di browser, terutama tinggi 2000-20000px di
		// atas): ganti tampilan dengan Grid ringan berpaginasi via PratinjauXlsxHelper (pola B,
		// dipakai jg 57 dashboard lain). Widget zss disembunyikan (BUKAN detach) shg Book tetap
		// hidup & file .xlsx asli (dibaca caller lewat parameter 'file' terpisah, bukan lewat
		// spreadsheet ini) tetap utuh utk tombol Download yang sudah dipasang caller.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}