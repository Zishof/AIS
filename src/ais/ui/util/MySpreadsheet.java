package ais.ui.util;

import java.io.InputStream;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zss.model.Book;
import org.zkoss.zss.ui.Spreadsheet;

public class MySpreadsheet extends Spreadsheet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3723253326511941267L;
	private Component pendingParent;
	private boolean bookSiap;
	private boolean attachDijadwalkan;
	private int maxRowsDiminta;

	public MySpreadsheet() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void setParent(Component component) {
		if (component == null) {
			pendingParent = null;
			super.setParent(null);
			return;
		}
		if (!bookSiap) {
			pendingParent = component;
			return;
		}
		super.setParent(component);
		aturTinggi(maxRowsDiminta);
//		try {
//			((HtmlBasedComponent) component.getParent()).setStyle("max-height: 700px;min-height: 50px;");
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:23");
//			// TODO: handle exception
//		}
	}

	public void setSrc(String src) {
		super.setSrc(src);
		bookSiap = true;
		pasangKeParentSetelahInisialisasi();
	}

	public void setBook(Book book) {
		super.setBook(book);
		bookSiap = true;
		pasangKeParentSetelahInisialisasi();
	}

	public void setBookFromStream(InputStream inputStream, String name) {
		super.setBookFromStream(inputStream, name);
		bookSiap = true;
		pasangKeParentSetelahInisialisasi();
	}

	private void pasangKeParentSetelahInisialisasi() {
		final Component parent = pendingParent;
		if (parent == null || attachDijadwalkan) return;
		if (parent.getPage() == null) {
			pendingParent = null;
			super.setParent(parent);
			aturTinggi(maxRowsDiminta);
			return;
		}
		attachDijadwalkan = true;
		final String eventName = "onAttachSpreadsheetSiap" + System.identityHashCode(this);
		final EventListener listener = new EventListener() {
			public void onEvent(Event event) throws Exception {
				parent.removeEventListener(eventName, this);
				attachDijadwalkan = false;
				if (pendingParent == parent) {
					pendingParent = null;
					MySpreadsheet.super.setParent(parent);
					aturTinggi(maxRowsDiminta);
				}
			}
		};
		parent.addEventListener(eventName, listener);
		Events.postEvent(eventName, parent, null);
	}

	/** Dipakai helper pratinjau setelah seluruh sel selesai dibangun. */
	void pasangSekarangJikaTertunda() {
		Component parent = pendingParent;
		if (parent == null) return;
		pendingParent = null;
		super.setParent(parent);
		aturTinggi(maxRowsDiminta);
	}

	public void setMaxcolumns(int columns) {
		try {
			// FIX defensif (pola sama dengan setMaxrows di bawah): ColumnHelper/
			// CTWorksheetImpl.getColsArray() (pustaka pihak ketiga ZK Spreadsheet + POI) bisa
			// melempar IndexOutOfBoundsException saat dipanggil langsung dari sini juga.
			super.setMaxcolumns(columns);
		} catch (IndexOutOfBoundsException e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(zss-poi-limitation) src/ais/ui/util/MySpreadsheet.java:setMaxcolumns");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:setMaxcolumns");
		}

		// FIX AKAR MASALAH (IndexOutOfBoundsException di
		// Spreadsheet$InnerDataListener.onSizeChange -> updateRowHeight -> ColumnHelper.getColumn1Based
		// -> CTWorksheetImpl.getColsArray): exception ini terjadi ASINKRON lewat listener internal
		// Spreadsheet yang TIDAK punya hook resmi utk di-override/di-try-catch dari kode aplikasi
		// (lihat catatan pada setMaxrows). Penyebabnya: workbook belum punya entri <col> utk semua
		// indeks kolom hingga 'columns', sehingga saat ZK menghitung ulang tinggi baris (dipicu oleh
		// perubahan data apa pun pada sheet), POI mengakses indeks kolom yang belum ada entrinya dan
		// melempar exception yang menjatuhkan seluruh proses AU (bukan cuma 1 permintaan pengguna).
		// Solusi: paksa POI membuat entri <col> utk SEMUA kolom hingga 'columns' SEKARANG lewat
		// setColumnWidth (API resmi POI, aman dipanggil per-kolom) supaya array kolom sudah lengkap
		// SEBELUM listener internal sempat membacanya secara asinkron.
		try {
			Object book = getBook();
			if (columns > 0 && book instanceof org.zkoss.poi.ss.usermodel.Workbook) {
				org.zkoss.poi.ss.usermodel.Workbook wb = (org.zkoss.poi.ss.usermodel.Workbook) book;
				if (wb.getNumberOfSheets() > 0) {
					org.zkoss.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
					int lebarDefault = 2340; // ~10 karakter, satuan 1/256 lebar karakter (default POI)
					try {
						lebarDefault = sheet.getDefaultColumnWidth() * 256;
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:setMaxcolumns:lebarDefault");
					}
					for (int col = 0; col < columns; col++) {
						try {
							sheet.setColumnWidth(col, lebarDefault);
						} catch (Exception exKolom) {
							ais.common.ErrorAuditUtil.record(exKolom, "auto-audit(zss-poi-limitation) src/ais/ui/util/MySpreadsheet.java:setMaxcolumns:col=" + col);
						}
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:setMaxcolumns:padCols");
		}
	}

	public void setMaxrows(int rows) {
		maxRowsDiminta = rows;
		try {
			// FIX defensif: org.zkoss.poi.xssf.usermodel.helpers.ColumnHelper /
			// CTWorksheetImpl.getColsArray() (pustaka pihak ketiga ZK Spreadsheet + POI) bisa
			// melempar IndexOutOfBoundsException ketika workbook template (mis. rowcolumn.xlsx)
			// belum punya definisi kolom/baris yang cukup untuk jumlah baris yang diminta.
			// Tidak ada hook resmi untuk override listener resize internal Spreadsheet
			// (Spreadsheet$InnerDataListener) dari kode aplikasi, jadi minimal cegah exception
			// dari panggilan setMaxrows sendiri agar tidak menjatuhkan seluruh halaman.
			super.setMaxrows(rows);
		} catch (IndexOutOfBoundsException e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(zss-poi-limitation) src/ais/ui/util/MySpreadsheet.java:setMaxrows");
			return;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:setMaxrows");
			return;
		}

		aturTinggi(rows);

	}

	private void aturTinggi(int rows) {
		if (rows <= 0) return;
		try {
			if (getParent() != null) {
				int tinggi = (rows * 17) + 2500;
				String h = tinggi > 2000 ? "2000px" : (tinggi + "px");
				if (getParent().getParent() instanceof HtmlBasedComponent) {
					((HtmlBasedComponent) getParent().getParent()).setStyle("max-height: " + h + ";min-height: 50px;");
				}
				this.setStyle("max-height: " + h + ";min-height: 50px;");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:aturTinggi"); }
	}
}
