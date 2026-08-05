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

public class LoadBarUtils {

	private static final String MSG_WAIT = "Harap tunggu, sedang menyiapkan data ..";
	private static final String STATUS_ERROR = "Error";
	private static final String STATUS_DONE = "Selesai";
	private static final int DEFAULT_DELAY = 500;

	// -------------------------------------------------------------------------
	// Public Methods
	// -------------------------------------------------------------------------

	public static Label displayLoadBar(final EventListener eventListener) {
		Component root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		return displayLoadBar(root, eventListener);
	}

	public static Label displayLoadBar(Component parent) {
		// Delegasi ke method utama dengan listener null
		return displayLoadBar(parent, (EventListener) null);
	}

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
	public static Label displayLoadBar(Component parent, final File file, final Component center, final Intbox sizedata) {
		// Default maxCol ke 70 jika tidak disediakan
		return displayLoadBar(parent, file, center, sizedata, null);
	}

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

	private static Label createBaseLabel(String initialText) {
		Label label = new Label(initialText);
		label.setMultiline(true);
		if (initialText != null && !initialText.isEmpty()) {
			Clients.showBusy(initialText);
		}
		return label;
	}

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

	private static void safeTriggerEvent(EventListener listener, String data) throws Exception {
		if (listener != null) {
			listener.onEvent(data == null ? null : new Event(data));
		}
	}

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