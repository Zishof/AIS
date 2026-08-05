package ais.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Column;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Toolbar;

import ais.common.Common;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * <h3>DashboardGridExportHelper — Cetak PDF &amp; Ekspor Excel Otomatis untuk Dashboard Bertabel</h3>
 *
 * <p><b>Untuk apa kelas ini.</b> Memberi tombol <b>Cetak PDF</b> dan <b>Ekspor Excel</b> (lengkap
 * dengan indikator "sedang memproses"/progress) kepada dashboard/statistik apa pun secara SERAGAM,
 * <b>tanpa</b> dashboard tersebut menulis sendiri kode pembuatan PDF/Excel. Cukup satu baris
 * pemanggilan: {@link #pasang(Component, String)} — dan seluruh <i>tabel</i> yang sudah tampil di
 * layar dashboard itu otomatis bisa dicetak ke PDF atau diunduh sebagai berkas Excel.</p>
 *
 * <p><b>Cara kerja (universal, tanpa konfigurasi).</b> Saat tombol diklik, mesin menelusuri seluruh
 * komponen tabel ({@link Grid}) yang berada di dalam dashboard, membaca judul kolom dan isi tiap
 * baris apa adanya (mengikuti hasil filter/pencarian terakhir yang sedang ditampilkan), lalu
 * menyusunnya menjadi berkas: PDF (A4 mendatar, iText) atau Excel (.xlsx, format terbuka). Karena
 * membaca langsung dari tabel yang tampil, hasil unduhan selalu <b>persis sama</b> dengan yang dilihat
 * pengguna. Grafik (yang berupa gambar HTML/CSS) tidak ikut karena bukan data tabel; fokusnya pada
 * data yang bisa diolah kembali.</p>
 *
 * <p><b>Kenapa dibuat begini (reuse &amp; pemeliharaan).</b> Sebelumnya tiap dashboard yang ingin bisa
 * dicetak harus menyalin kode iText/Excel yang panjang dan rawan berbeda-beda. Dengan kelas ini,
 * penambahan kemampuan cetak ke dashboard mana pun menjadi satu baris, konsisten, dan bila format
 * PDF/Excel ingin diseragamkan cukup diubah di sini. Cocok untuk penerapan massal ke banyak dashboard
 * bertabel. Untuk dashboard yang butuh laporan kaya grafik, gunakan
 * {@code ais.action.master.helper.DashboardReportKit} (berbasis deskripsi data).</p>
 *
 * <p><b>Progress.</b> Klik tombol menampilkan indikator sibuk lalu memproses pembuatan berkas pada
 * putaran event berikutnya (via {@link Events#echoEvent}) supaya peramban tidak terasa membeku, dan
 * indikator otomatis hilang setelah berkas siap diunduh. Java 1.7, ZK 5.5. Semua langkah dibungkus
 * penanganan galat agar kegagalan satu berkas tidak merusak halaman.</p>
 */
public final class DashboardGridExportHelper {

	private DashboardGridExportHelper() {
	}

	/**
	 * Menyisipkan satu baris toolbar berisi tombol <b>Cetak PDF</b> + <b>Ekspor Excel</b> sebagai anak
	 * PERTAMA dari {@code dashboardRoot}. Tombol akan mengekspor seluruh tabel yang berada di dalam
	 * {@code dashboardRoot} saat diklik.
	 *
	 * @param dashboardRoot komponen akar dashboard (mis. Vbox) yang memuat tabel-tabelnya.
	 * @param judul         judul laporan (dipakai di kepala PDF dan nama berkas).
	 */
	public static void pasang(final Component dashboardRoot, final String judul) {
		if (dashboardRoot == null) {
			return;
		}
		// Pasang grup tombol (sementara) di ATAS dashboardRoot, mengekspor tabel di dalamnya.
		pasangGrup(dashboardRoot, dashboardRoot, judul);
		// SISIR (permintaan user): setelah konten dashboard selesai dibangun, SATUKAN grup ekspor ke
		// dalam TOOLBAR INTERNAL dashboard bila ada -> "menyatu dengan dashboard", bukan bar
		// mengambang di atas. Ditunda via timer karena toolbar umumnya dibangun SETELAH pasang()
		// dipanggil di awal init(). Bila dashboard TAK punya toolbar, grup tetap di atas (apa adanya).
		try {
			ais.common.Common.createDefaultTimerNoBusy(new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					gabungGrupKeToolbar(dashboardRoot);
				}
			});
		} catch (Exception igDefer) {
			ais.common.ErrorAuditUtil.record(igDefer, "auto-audit(empty-catch) DashboardGridExportHelper.pasang-defer");
		}
	}

	/**
	 * Memindahkan grup tombol ekspor (yang tadinya anak-langsung {@code dashboardRoot}) ke dalam
	 * TOOLBAR INTERNAL pertama dashboard, bila ada — sehingga tombol menyatu dengan toolbar dashboard
	 * (mis. bar Cari/Refresh) alih-alih mengambang di baris terpisah. Idempoten & aman: bila tak ada
	 * toolbar atau grup sudah dipindah, tidak melakukan apa-apa.
	 */
	private static void gabungGrupKeToolbar(Component dashboardRoot) {
		try {
			if (dashboardRoot == null) {
				return;
			}
			Component grup = null;
			for (Object anak : dashboardRoot.getChildren()) {
				if (anak instanceof Component
						&& Boolean.TRUE.equals(((Component) anak).getAttribute("aisExportToolbar"))) {
					grup = (Component) anak;
					break;
				}
			}
			if (grup == null) {
				return; // sudah dipindah / tak ada grup di posisi atas
			}
			org.zkoss.zul.Toolbar target = cariToolbarInternal(dashboardRoot, grup);
			if (target == null) {
				return; // dashboard tak punya toolbar -> biarkan grup di atas
			}
			// Bila toolbar target SUDAH memuat grup ekspor (mis. pasang() terpanggil ulang), buang
			// duplikat di atas agar tak menumpuk.
			for (Object k : target.getChildren()) {
				if (k instanceof Component
						&& Boolean.TRUE.equals(((Component) k).getAttribute("aisExportToolbar"))) {
					grup.detach();
					return;
				}
			}
			if (grup instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				((org.zkoss.zk.ui.HtmlBasedComponent) grup).setStyle(
						"display:inline-flex;align-items:stretch;gap:0;background:#ffffff;"
								+ "border:1px solid #cbd5e1;border-radius:8px;overflow:hidden;margin:0 0 0 8px;");
			}
			grup.setParent(target); // gabung di akhir toolbar internal
		} catch (Exception igGabung) {
			ais.common.ErrorAuditUtil.record(igGabung, "auto-audit(empty-catch) DashboardGridExportHelper.gabungGrupKeToolbar");
		}
	}

	/** BFS mencari {@link Toolbar} pertama (paling dangkal) di dalam {@code root}, melewati grup ekspor. */
	private static Toolbar cariToolbarInternal(Component root, Component grupSkip) {
		java.util.LinkedList<Component> antre = new java.util.LinkedList<Component>();
		for (Object c : root.getChildren()) {
			if (c instanceof Component) {
				antre.add((Component) c);
			}
		}
		while (!antre.isEmpty()) {
			Component c = antre.removeFirst();
			if (c == grupSkip) {
				continue; // jangan telusuri / pilih grup ekspor itu sendiri
			}
			if (c instanceof Toolbar && !Boolean.TRUE.equals(c.getAttribute("aisExportToolbar"))) {
				return (Toolbar) c;
			}
			for (Object k : c.getChildren()) {
				if (k instanceof Component) {
					antre.add((Component) k);
				}
			}
		}
		return null;
	}

	/**
	 * Merakit tombol <b>Cetak PDF</b> + <b>Ekspor Excel</b> sebagai SATU BUTTON GROUP tersegmentasi
	 * (menyatu, tanpa jarak, dengan pembatas tipis) lalu memasangnya ke {@code wadah} sebagai anak
	 * PERTAMA. Tabel yang diekspor diambil dari {@code sumberGrid} (boleh berbeda dari {@code wadah}
	 * — mis. wadah = tabpanel pertama, sumberGrid = seluruh dashboard agar semua tab ikut terekspor).
	 *
	 * <p>Dipakai agar tombol ekspor "menyatu dengan dashboard" (mis. di dalam tab), bukan mengambang
	 * sebagai baris terpisah di atas. Idempoten terhadap {@code wadah}.</p>
	 *
	 * @param wadah      komponen tempat grup tombol dipasang (mis. Tabpanel/Toolbar/Div).
	 * @param sumberGrid komponen yang memuat Grid yang akan diekspor (boleh sama dengan wadah).
	 * @param judul      judul laporan.
	 */
	public static void pasangGrup(final Component wadah, final Component sumberGrid, final String judul) {
		if (wadah == null) {
			return;
		}
		// Idempoten: bila pembangun (init/refresh) dipanggil ulang, jangan pasang dua kali.
		for (Object anak : wadah.getChildren()) {
			if (anak instanceof Component
					&& Boolean.TRUE.equals(((Component) anak).getAttribute("aisExportToolbar"))) {
				return;
			}
		}
		org.zkoss.zul.Div grup = new org.zkoss.zul.Div();
		grup.setAttribute("aisExportToolbar", Boolean.TRUE);
		grup.setStyle("display:inline-flex;align-items:stretch;gap:0;background:#ffffff;"
				+ "border:1px solid #cbd5e1;border-radius:8px;overflow:hidden;margin-bottom:8px;");
		pasangTombol(grup, sumberGrid == null ? wadah : sumberGrid, judul);
		// Satukan tampilan tiap tombol menjadi segmen grup (hilangkan border/rounding individual,
		// beri pembatas kiri mulai tombol kedua).
		try {
			java.util.List<?> anakGrup = grup.getChildren();
			int idx = 0;
			for (Object o : anakGrup) {
				if (o instanceof org.zkoss.zul.Toolbarbutton) {
					((org.zkoss.zul.Toolbarbutton) o).setStyle("margin:0;border:0;border-radius:0;"
							+ (idx > 0 ? "border-left:1px solid #e2e8f0;" : ""));
					idx++;
				}
			}
		} catch (Exception igGrup) { ais.common.ErrorAuditUtil.record(igGrup, "auto-audit(empty-catch) DashboardGridExportHelper.pasangGrup"); }
		if (wadah.getFirstChild() != null) {
			wadah.insertBefore(grup, wadah.getFirstChild());
		} else {
			grup.setParent(wadah);
		}
	}

	/**
	 * Memasang tombol Cetak PDF + Ekspor Excel ke {@code parent} (mis. toolbar yang sudah ada),
	 * mengekspor tabel-tabel yang berada di dalam {@code sumberGrid}.
	 *
	 * @param parent     wadah tombol (Toolbar/Hbox/Div).
	 * @param sumberGrid komponen yang memuat {@link Grid} yang akan diekspor.
	 * @param judul      judul laporan.
	 */
	public static void pasangTombol(Component parent, final Component sumberGrid, final String judul) {
		final String jdl = judul == null || judul.trim().length() == 0 ? "Laporan Dashboard" : judul.trim();

		final MyToolbarbuttonConfig pdf = new MyToolbarbuttonConfig("Cetak PDF", "/img/print.png");
		pdf.setTooltiptext("Cetak seluruh tabel yang tampil ke berkas PDF");
		pdf.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try { Clients.showBusy("Menyiapkan berkas PDF, mohon tunggu..."); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:116"); }
				Events.echoEvent("onProsesPdf", pdf, null);
			}
		});
		pdf.addEventListener("onProsesPdf", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					ekspor(sumberGrid, jdl, true);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				} finally {
					try { Clients.clearBusy(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:128"); }
				}
			}
		});
		pdf.setParent(parent);

		final MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Ekspor Excel", "/img/excel.png");
		excel.setTooltiptext("Unduh seluruh tabel yang tampil sebagai berkas Excel (.xlsx)");
		excel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try { Clients.showBusy("Menyiapkan berkas Excel, mohon tunggu..."); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:139"); }
				Events.echoEvent("onProsesExcel", excel, null);
			}
		});
		excel.addEventListener("onProsesExcel", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					ekspor(sumberGrid, jdl, false);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				} finally {
					try { Clients.clearBusy(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:151"); }
				}
			}
		});
		excel.setParent(parent);
	}

	/**
	 * Memasang HANYA tombol "Cetak PDF" ke {@code parent} (tanpa tombol Excel).
	 *
	 * <p>Dipakai oleh layar yang SUDAH punya tombol Excel sendiri -- mis. tabel rekap di Dasbor
	 * Kantin/Stok/Kepatuhan, yang menulis Excel langsung dari data mentah sehingga angkanya bertipe
	 * numerik asli (bukan hasil pembacaan teks dari grid). Tanpa method ini, memakai
	 * {@link #pasangTombol} akan memunculkan dua tombol Excel berdampingan yang perilakunya berbeda
	 * dan membingungkan pengguna.</p>
	 *
	 * @param parent     wadah tombol (Toolbar/Hbox/Div).
	 * @param sumberGrid komponen yang memuat {@link Grid} yang akan dicetak.
	 * @param judul      judul laporan pada berkas PDF.
	 */
	public static void pasangTombolPdf(Component parent, final Component sumberGrid, final String judul) {
		final String jdl = judul == null || judul.trim().length() == 0 ? "Laporan Dashboard" : judul.trim();

		final MyToolbarbuttonConfig pdf = new MyToolbarbuttonConfig("Cetak PDF", "/img/print.png");
		pdf.setTooltiptext("Cetak tabel ini ke berkas PDF");
		pdf.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try { Clients.showBusy("Menyiapkan berkas PDF, mohon tunggu..."); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) DashboardGridExportHelper.pasangTombolPdf.busy"); }
				Events.echoEvent("onProsesPdfSaja", pdf, null);
			}
		});
		pdf.addEventListener("onProsesPdfSaja", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					ekspor(sumberGrid, jdl, true);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				} finally {
					try { Clients.clearBusy(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) DashboardGridExportHelper.pasangTombolPdf.clear"); }
				}
			}
		});
		pdf.setParent(parent);
	}

	// ------------------------------------------------------------------ inti ekspor

	private static void ekspor(Component sumberGrid, String judul, boolean pdf) throws Exception {
		List<Tabel> tabels = kumpulkanTabel(sumberGrid);
		String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(WaktuUtil.getDate());
		String namaFile = bersihkanNama(judul) + "_" + ts + (pdf ? ".pdf" : ".xlsx");
		File file = new File(Common.REAL_PATH + "/tmp/" + namaFile);
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (pdf) {
			tulisPdf(file, judul, tabels);
			Filedownload.save(file, "application/pdf");
		} else {
			tulisExcel(file, tabels);
			Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
	}

	/** Satu tabel hasil pembacaan dari sebuah {@link Grid}. */
	private static class Tabel {
		String judul = "Tabel";
		List<String> header = new ArrayList<String>();
		List<List<String>> baris = new ArrayList<List<String>>();
	}

	/** Telusuri komponen, kumpulkan tiap {@link Grid} menjadi {@link Tabel} (header + baris teks). */
	private static List<Tabel> kumpulkanTabel(Component root) {
		List<Tabel> hasil = new ArrayList<Tabel>();
		List<Grid> grids = new ArrayList<Grid>();
		cariGrid(root, grids);
		for (Grid g : grids) {
			try {
				Tabel t = new Tabel();
				if (g.getColumns() != null) {
					for (Object oc : g.getColumns().getChildren()) {
						if (oc instanceof Column) {
							String l = ((Column) oc).getLabel();
							t.header.add(l == null ? "" : l);
						}
					}
				}
				if (g.getRows() != null) {
					for (Object orow : g.getRows().getChildren()) {
						if (!(orow instanceof Row)) {
							continue;
						}
						List<String> sel = new ArrayList<String>();
						for (Object oc : ((Row) orow).getChildren()) {
							sel.add(teksSel((Component) oc));
						}
						t.baris.add(sel);
					}
				}
				if (!t.header.isEmpty() || !t.baris.isEmpty()) {
					hasil.add(t);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:215");
				// lewati grid bermasalah, jangan hentikan ekspor
			}
		}
		return hasil;
	}

	private static void cariGrid(Component c, List<Grid> out) {
		if (c == null) {
			return;
		}
		if (c instanceof Grid) {
			out.add((Grid) c);
			return; // grid anak (mis. nested) diperlakukan terpisah bila ada; hindari duplikasi header
		}
		for (Object anak : c.getChildren()) {
			cariGrid((Component) anak, out);
		}
	}

	/** Ambil teks sebuah sel: gabungkan semua {@link Label} di dalamnya (mendukung sel Vbox/Hbox). */
	private static String teksSel(Component sel) {
		StringBuilder sb = new StringBuilder();
		kumpulkanLabel(sel, sb);
		return sb.toString().trim();
	}

	private static void kumpulkanLabel(Component c, StringBuilder sb) {
		if (c == null) {
			return;
		}
		if (c instanceof Label) {
			String v = ((Label) c).getValue();
			if (v != null && v.length() > 0) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(v.replace("\n", " "));
			}
		}
		for (Object anak : c.getChildren()) {
			kumpulkanLabel((Component) anak, sb);
		}
	}

	// ------------------------------------------------------------------ PDF (iText)

	private static void tulisPdf(File file, String judul, List<Tabel> tabels) throws Exception {
		Document document = new Document(PageSize.A4.rotate(), 24, 24, 30, 24);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			PdfWriter.getInstance(document, fos);
			document.open();

			Font titleFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
			Font subFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(90, 90, 90));
			Font headFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, BaseColor.WHITE);
			Font bodyFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL);

			Paragraph title = new Paragraph(judul.toUpperCase(), titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);

			Paragraph sub = new Paragraph(
					"Dicetak: " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(WaktuUtil.getDate()), subFont);
			sub.setAlignment(Element.ALIGN_CENTER);
			sub.setSpacingAfter(8f);
			document.add(sub);

			BaseColor headerBg = new BaseColor(37, 99, 235);
			if (tabels.isEmpty()) {
				document.add(new Paragraph("Tidak ada tabel untuk dicetak.", bodyFont));
			}
			for (Tabel t : tabels) {
				int kolom = Math.max(t.header.size(), lebarMaks(t.baris));
				if (kolom <= 0) {
					continue;
				}
				if (t.baris.size() >= 0 && tabels.size() > 1) {
					Paragraph jt = new Paragraph(t.judul, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD));
					jt.setSpacingBefore(6f);
					jt.setSpacingAfter(3f);
					document.add(jt);
				}
				PdfPTable table = new PdfPTable(kolom);
				table.setWidthPercentage(100);
				table.setHeaderRows(t.header.isEmpty() ? 0 : 1);
				for (int i = 0; i < t.header.size(); i++) {
					PdfPCell cell = new PdfPCell(new Phrase(t.header.get(i), headFont));
					cell.setBackgroundColor(headerBg);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell.setPadding(4);
					table.addCell(cell);
				}
				for (int i = t.header.size(); i < kolom; i++) {
					PdfPCell cell = new PdfPCell(new Phrase("", headFont));
					cell.setBackgroundColor(headerBg);
					table.addCell(cell);
				}
				boolean zebra = false;
				for (List<String> baris : t.baris) {
					BaseColor bg = zebra ? new BaseColor(243, 246, 250) : BaseColor.WHITE;
					zebra = !zebra;
					for (int i = 0; i < kolom; i++) {
						String teks = i < baris.size() ? baris.get(i) : "";
						PdfPCell cell = new PdfPCell(new Phrase(teks, bodyFont));
						cell.setBackgroundColor(bg);
						cell.setPadding(3);
						table.addCell(cell);
					}
				}
				document.add(table);
			}
		} finally {
			try { document.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:330"); }
			if (fos != null) {
				try { fos.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:332"); }
			}
		}
	}

	// ------------------------------------------------------------------ Excel (zpoi)

	private static void tulisExcel(File file, List<Tabel> tabels) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		FileOutputStream fos = null;
		try {
			XSSFCellStyle headStyle = UIUtil.solid_LIGHT_GRAY(workbook);
			XSSFCellStyle bodyStyle = UIUtil.solid_WHITE(workbook);

			int no = 1;
			if (tabels.isEmpty()) {
				workbook.createSheet("Kosong");
			}
			for (Tabel t : tabels) {
				XSSFSheet sheet = workbook.createSheet("Tabel " + (no++));
				sheet.setDefaultColumnWidth(20);
				int r = 0;
				if (!t.header.isEmpty()) {
					XSSFRow head = sheet.createRow(r++);
					for (int i = 0; i < t.header.size(); i++) {
						XSSFCell cell = head.createCell(i);
						cell.setCellValue(t.header.get(i));
						cell.setCellStyle(headStyle);
					}
				}
				for (List<String> baris : t.baris) {
					XSSFRow xr = sheet.createRow(r++);
					for (int i = 0; i < baris.size(); i++) {
						XSSFCell cell = xr.createCell(i);
						cell.setCellValue(baris.get(i));
						cell.setCellStyle(bodyStyle);
					}
				}
			}
			fos = new FileOutputStream(file);
			workbook.write(fos);
		} finally {
			if (fos != null) {
				try { fos.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardGridExportHelper.java:375"); }
			}
		}
	}

	// ------------------------------------------------------------------ util

	private static int lebarMaks(List<List<String>> baris) {
		int m = 0;
		for (List<String> b : baris) {
			m = Math.max(m, b.size());
		}
		return m;
	}

	private static String bersihkanNama(String s) {
		if (s == null) {
			return "Laporan";
		}
		String r = s.replaceAll("[^A-Za-z0-9]+", "_");
		return r.length() == 0 ? "Laporan" : r;
	}
}
