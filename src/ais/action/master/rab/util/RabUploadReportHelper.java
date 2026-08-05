package ais.action.master.rab.util;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>RabUploadReportHelper — Laporan hasil UPLOAD anggaran (RAB)</h3>
 *
 * <p>Menampilkan jendela ringkasan yang JELAS setelah upload perencanaan anggaran: berapa item
 * BERHASIL dan berapa GAGAL, rincian tiap kegagalan beserta penyebabnya, serta saran perbaikan
 * untuk pengguna. Dipakai bersama oleh {@code WorkspaceRevisiAction} (tahunan) dan
 * {@code WorkspaceRevisiBulananAction} (bulanan). Daftar {@code gagal}/{@code berhasil} berisi
 * {@code List<String>} yang diisi oleh importer ({@code RabImporter}/{@code RabBulananImporter}).</p>
 */
public final class RabUploadReportHelper {

	private RabUploadReportHelper() {
	}

	/**
	 * Tampilkan laporan modal. {@code page} boleh null (fallback ke root halaman aktif).
	 */
	public static void tampilkan(org.zkoss.zk.ui.Page page, String judul, List<String> berhasil, List<String> gagal) {
		try {
			int nB = berhasil == null ? 0 : berhasil.size();
			int nG = gagal == null ? 0 : gagal.size();

			Component root = page != null ? page.getFirstRoot() : null;

			final MyWindow win = new MyWindow(judul == null ? "Hasil Upload Anggaran" : judul, "normal", true);
			win.setWidth("680px");
			win.setParent(root);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(win);

			Center center = new Center();
			center.setAutoscroll(true);
			center.setParent(borderlayout);

			String warna = nG == 0 ? "#065f46" : (nB == 0 ? "#b91c1c" : "#b45309");
			StringBuilder sb = new StringBuilder();
			sb.append("<div style='padding:14px 16px;'>");
			sb.append("<div style='font-size:15px;font-weight:800;color:").append(warna).append(";margin-bottom:6px;'>")
					.append("Berhasil di-upload: ").append(nB).append(" item &nbsp;|&nbsp; Gagal: ").append(nG)
					.append(" item</div>");

			if (nG > 0) {
				sb.append("<div style='font-weight:700;margin-top:8px;color:#7f1d1d;'>Rincian yang GAGAL:</div>");
				sb.append("<div style='max-height:230px;overflow:auto;border:1px solid #fecaca;background:#fef2f2;"
						+ "border-radius:8px;padding:8px;font-size:12px;color:#7f1d1d;line-height:1.6;'>");
				for (String g : gagal) {
					sb.append("&bull; ").append(escape(g)).append("<br/>");
				}
				sb.append("</div>");
				sb.append("<div style='margin-top:8px;font-size:12px;color:#374151;background:#fffbeb;"
						+ "border:1px solid #fde68a;border-radius:8px;padding:8px;line-height:1.6;'>")
						.append("<b>Saran perbaikan:</b><br/>")
						.append("1. Nilai bulanan harus berupa ANGKA saja (boleh pemisah ribuan titik/koma, mis. "
								+ "1.000.000 atau 1,000,000), tanpa teks atau simbol 'Rp'.<br/>")
						.append("2. Kolom <b>ID Workspace</b> dan <b>No</b> wajib terisi angka yang benar (tidak boleh "
								+ "kosong/berisi teks).<br/>")
						.append("3. Perbaiki baris di atas pada file Excel, lalu upload ULANG. Bila mengupload ulang, "
								+ "hapus dahulu data perencanaan lama.")
						.append("</div>");
			}

			if (nB > 0) {
				sb.append("<div style='font-weight:700;margin-top:10px;color:#14532d;'>Rincian yang BERHASIL ("
						+ nB + "):</div>");
				sb.append("<div style='max-height:170px;overflow:auto;border:1px solid #bbf7d0;background:#f0fdf4;"
						+ "border-radius:8px;padding:8px;font-size:12px;color:#14532d;line-height:1.6;'>");
				for (String b : berhasil) {
					sb.append("&bull; ").append(escape(b)).append("<br/>");
				}
				sb.append("</div>");
			}

			if (nB == 0 && nG == 0) {
				sb.append("<div style='color:#374151;'>Tidak ada baris data yang terbaca dari file. Pastikan format "
						+ "kolom: ID Workspace, No, Kode, Nama, Jan, Feb, ... , Des.</div>");
			}

			sb.append("</div>");

			Html html = new Html(sb.toString());
			html.setParent(center);

			South south = new South();
			south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setStyle("float: right; background: transparent; border: none;");

			MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			tutup.setStyle(
					"font-size: 13px; font-weight: bold; color: #475569; background-color: #f1f5f9; border-radius: 6px; padding: 6px 15px; border: 1px solid #cbd5e1; cursor: pointer; text-decoration: none;");
			tutup.setTooltiptext("Tutup");
			tutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					win.detach();
				}
			});
			tutup.setParent(toolbar);
			toolbar.setParent(south);

			win.doHighlighted();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
