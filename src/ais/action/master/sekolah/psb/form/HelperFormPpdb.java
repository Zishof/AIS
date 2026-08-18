package ais.action.master.sekolah.psb.form;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;

/**
 * Helper generik pembangun baris form PPDB/PSB (package
 * ais.action.master.sekolah.psb.form.*).
 *
 * Form PPDB sangat custom per sekolah (PPDB, PPDB1, PPDB2, PPDB_Alumni,
 * PPDB_Simple..6) tetapi pola penyusunan barisnya selalu sama:
 *
 * <pre>
 * if (rowData != null) {            // slot baris dinamis sudah disediakan form
 *     rowData.setVisible(true);
 *     Common.clear(rowData);
 * *     rowData.appendChild(label);
 *     rowData.appendChild(input);
 * } else {                          // tidak ada slot: buat baris baru di grid
 *     MyFormRow row = new MyFormRow();
 * *     row.setParent(rows);
 *     ...
 * }
 * </pre>
 *
 * Pola itu dipusatkan di {@link #barisForm(Rows, Row, String, Component)}
 * supaya penambahan field baru cukup satu baris dan perubahan gaya (label
 * wajib, lebar input, baris keterangan) cukup diubah di satu tempat.
 */
public final class HelperFormPpdb {

	private HelperFormPpdb() {
	}

	/**
	 * Baris form standar PPDB: label di kolom kiri, input di kolom kanan.
	 * Bila {@code rowData} (slot baris dinamis milik form) tersedia maka slot
	 * itu yang dipakai ulang — dikosongkan dulu lalu ditampilkan; bila tidak,
	 * baris baru dibuat pada {@code rows}. Mengembalikan baris yang dipakai.
	 */
	public static Row barisForm(Rows rows, Row rowData, String label, Component input) {
		Row row;
		if (rowData != null) {
			row = rowData;
			row.setVisible(true);
			Common.clear(row);
		} else {
			row = new MyFormRow();
			row.setParent(rows);
		}
		row.appendChild(new ais.ui.util.MyLabelConfig(label));
		row.appendChild(input);
		aturLebarInput(input);
		return row;
	}

	/** Varian tanpa slot dinamis. */
	public static Row barisForm(Rows rows, String label, Component input) {
		return barisForm(rows, null, label, input);
	}

	/**
	 * Lebar input seragam 90% (kolom kanan form PPDB) — aman dipanggil untuk
	 * komponen apa pun; lebar yang sudah diset pemanggil tidak ditimpa.
	 */
	public static void aturLebarInput(Component input) {
		try {
			if (input instanceof HtmlBasedComponent) {
				HtmlBasedComponent c = (HtmlBasedComponent) input;
				if (c.getWidth() == null || c.getWidth().trim().isEmpty()) {
					c.setWidth("90%");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/form/HelperFormPpdb.java:80");
		}
	}

	/** Label field wajib: pastikan diakhiri tanda "*". */
	public static String labelWajib(String label) {
		String l = label == null ? "" : label.trim();
		return l.endsWith("*") ? l : l + " *";
	}

	/**
	 * Baris keterangan/petunjuk satu kolom penuh (chip hint lembut, gaya
	 * .ais-aktifitas-catatan di css_utama.css).
	 */
	public static Row barisKeterangan(Rows rows, String teks) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		String aman = teks == null ? ""
				: teks.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		row.appendChild(new Html("<span class=\"ais-aktifitas-catatan\">" + aman + "</span>"));
		return row;
	}
}
