package ais.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Div;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

/**
 * <h3>PratinjauXlsxHelper — pratinjau ekspor sebagai tabel/grid RINGAN, Excel hanya saat Download</h3>
 *
 * <p><b>Untuk apa.</b> Di aplikasi ini ada DUA pola yang "langsung menampilkan Excel"
 * kepada pengguna, dan keduanya berat karena memakai komponen ZK Spreadsheet (zss):</p>
 * <ol>
 *   <li><b>Pola A (lewat file)</b> — semua ekspor via {@code Common.cetakData} /
 *       {@code Common.cetakDataCustomButton} membangun file .xlsx di latar lalu
 *       menampilkannya. Lihat {@code Common.displayXlsx} yang mendelegasikan ke
 *       {@link #tampilkanXlsxRingan(String, Intbox, int)}.</li>
 *   <li><b>Pola B (lewat Spreadsheet in-memory)</b> — banyak jendela laporan membangun
 *       langsung {@code MySpreadsheet}; panggil
 *       {@link #gantiSpreadsheetDenganGrid(org.zkoss.zss.ui.Spreadsheet)} di akhir
 *       pembangunan untuk menggantinya dengan Grid ringan.</li>
 * </ol>
 *
 * <p><b>Tampilan grid.</b> Sesuai permintaan: lebar kolom OTOMATIS mengikuti isi
 * ({@code ZkCompat.setFixedLayout(grid,false)} — di ZK5 = {@code setFixedLayout(false)},
 * di ZK7+ = {@code setSizedByContent(true)}), sehingga bila tabel sangat lebar muncul
 * scroll horizontal otomatis pada body grid. Data dipaginasi {@link #PAGE_SIZE} baris per
 * halaman. Tinggi jendela (Pola A) dihitung otomatis dari jumlah baris (dibatasi 1 halaman
 * + plafon agar tak melebihi layar).</p>
 *
 * <p><b>Kunci teknis.</b> {@code org.zkoss.zss.model.Book} (hasil {@code getBook()}) adalah
 * turunan {@code org.zkoss.poi.ss.usermodel.Workbook}, maka SATU pembaca POI melayani kedua
 * pola. File/Excel yang di-download TETAP utuh; tidak ada logika ekspor yang dihapus.</p>
 *
 * <p><b>Gaya kode.</b> Java 1.6/1.7 (tanpa lambda/try-with-resources/diamond/Stream).</p>
 */
public class PratinjauXlsxHelper {

	/**
	 * Center/North/South hanya menerima satu anak. Laporan lama kadang menaruh
	 * Spreadsheet langsung di region tersebut, lalu helper ini menggantinya dengan
	 * Grid dan Label sehingga anak kedua ditolak ZK. Bungkus seluruh isi region di
	 * satu Div terlebih dahulu; untuk parent biasa perilaku lama tetap dipakai.
	 */
	private static Component wadahAmanUntukPreview(Component parent) {
		if (!(parent instanceof LayoutRegion)) {
			return parent;
		}
		LayoutRegion region = (LayoutRegion) parent;
		if (region.getChildren().size() == 1) {
			Object anak = region.getChildren().get(0);
			if (anak instanceof Div && "xlsx-preview-region-wrapper".equals(((Div) anak).getSclass())) {
				return (Component) anak;
			}
		}
		List<Component> anakLama = new ArrayList<Component>();
		for (Object anak : region.getChildren()) {
			if (anak instanceof Component) {
				anakLama.add((Component) anak);
			}
		}
		Div wadah = new Div();
		wadah.setSclass("xlsx-preview-region-wrapper");
		/* Scroll ditangani oleh bingkai preview di renderWorkbookKeGrid. Parent
		 * harus dibatasi ke lebar region; overflow:auto di sini membuat browser
		 * memperlebar parent mengikuti Grid sehingga scrollbar horizontal hilang. */
		wadah.setStyle("width:100%;height:100%;max-width:100%;min-width:0;"
				+ "overflow:hidden;box-sizing:border-box;");
		/* ZK 5/6 LayoutRegion harus benar-benar kosong sebelum wrapper dipasang.
		 * Reparent langsung dapat membuat region masih melihat child lama dan
		 * melempar "Only one child is allowed" saat wadah dipasang. */
		for (Component anak : anakLama) anak.setParent(null);
		wadah.setParent(region);
		for (Component anak : anakLama) anak.setParent(wadah);
		return wadah;
	}

	/** Batas baris yang dimuat ke pratinjau grid agar tetap ringan. */
	public static final int MAKS_BARIS_PREVIEW = 1000;

	/** Jumlah baris per halaman (paging). */
	public static final int PAGE_SIZE = 25;

	private PratinjauXlsxHelper() {
		// utility class
	}

	/**
	 * Mengubah satu sel POI/zss menjadi teks. Menangani STRING, NUMERIC (bilangan bulat
	 * tanpa desimal), BOOLEAN, dan FORMULA secara gagal-anggun. Selalu non-null.
	 */
	public static String cellToString(org.zkoss.poi.ss.usermodel.Cell c) {
		if (c == null)
			return "";
		try {
			int t = c.getCellType();
			if (t == org.zkoss.poi.ss.usermodel.Cell.CELL_TYPE_STRING)
				return c.getStringCellValue();
			if (t == org.zkoss.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC) {
				double d = c.getNumericCellValue();
				if (d == Math.floor(d) && !Double.isInfinite(d))
					return String.valueOf((long) d);
				return String.valueOf(d);
			}
			if (t == org.zkoss.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN)
				return String.valueOf(c.getBooleanCellValue());
			if (t == org.zkoss.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA) {
				try {
					return c.getStringCellValue();
				} catch (Exception e) {
					try {
						return String.valueOf(c.getNumericCellValue());
					} catch (Exception e2) {
						return "";
					}
				}
			}
			return "";
		} catch (Exception e) {
			try {
				return c.toString();
			} catch (Exception e2) {
				return "";
			}
		}
	}

	/**
	 * Membaca sheet pertama sebuah Workbook (POI atau zss Book) dan merendernya menjadi
	 * Grid ringan di {@code parent}: lebar kolom otomatis, scroll horizontal otomatis bila
	 * lebar, dan paginasi {@link #PAGE_SIZE} baris/halaman.
	 *
	 * @param parent        wadah tempat Grid dipasang.
	 * @param wb            Workbook sumber (boleh {@code org.zkoss.zss.model.Book}).
	 * @param maksBaris     batas baris yang dimuat (sisanya hanya dihitung).
	 * @param fallbackCols  jumlah kolom cadangan bila sheet kosong.
	 * @param baris1Header  bila {@code true}, baris pertama jadi HEADER kolom (file
	 *                      cetakDataCustomButton); bila {@code false}, kolom generik & semua
	 *                      baris jadi data (template laporan dengan baris judul/merge).
	 * @return total baris data (di luar header) yang ditemukan di sheet.
	 */
	public static int renderWorkbookKeGrid(Component parent, org.zkoss.poi.ss.usermodel.Workbook wb, int maksBaris,
			int fallbackCols, boolean baris1Header) {

		// Grid POLOS (bukan MyGrid) supaya tanpa background-image & tanpa re-parent mobile.
		Grid grid = new Grid();
		grid.setStyle("background:#ffffff;");
		grid.setMold("paging");
		grid.setPageSize(PAGE_SIZE);
		// FIX "Only one child is allowed: <Center>": parent bisa LayoutRegion (Center/North/…) yang
		// hanya boleh 1 anak & mungkin sudah terisi (spreadsheet lama / render sebelumnya). Bersihkan
		// dulu agar setParent tidak melempar UiException saat me-render ulang grid.
		if (parent != null && parent.getChildren() != null && !parent.getChildren().isEmpty()) {
			parent.getChildren().clear();
		}
		/* Satu struktur scroll dipakai untuk SEMUA jalur laporan (file XLSX maupun
		 * MySpreadsheet). Scrollbar horizontal duplikat diletakkan di ATAS agar
		 * tetap terjangkau pada tabel panjang; scrollbar asli tetap tersedia di
		 * bawah area data. max-width/min-width:0 penting pada Tabpanel/Borderlayout:
		 * tanpa itu parent ikut melebar sebesar Grid dan tidak pernah overflow. */
		if (parent instanceof org.zkoss.zul.LayoutRegion) {
			((org.zkoss.zul.LayoutRegion) parent).setAutoscroll(false);
		}
		org.zkoss.zul.Div bingkaiSkrol = new org.zkoss.zul.Div();
		bingkaiSkrol.setWidth("100%");
		bingkaiSkrol.setStyle("height:100%;max-width:100%;min-width:0;overflow:hidden;"
				+ "position:relative;box-sizing:border-box;background:#ffffff;");
		bingkaiSkrol.setParent(parent);

		org.zkoss.zul.Div skrolAtas = new org.zkoss.zul.Div();
		skrolAtas.setWidth("100%");
		skrolAtas.setStyle("height:18px;max-width:100%;overflow-x:auto;overflow-y:hidden;"
				+ "box-sizing:border-box;background:#f8fafc;border-bottom:1px solid #dbe4ee;");
		skrolAtas.setParent(bingkaiSkrol);
		org.zkoss.zul.Div lebarSkrolAtas = new org.zkoss.zul.Div();
		lebarSkrolAtas.setStyle("height:1px;");
		lebarSkrolAtas.setParent(skrolAtas);

		org.zkoss.zul.Div skrol = new org.zkoss.zul.Div();
		skrol.setWidth("100%");
		skrol.setStyle("height:calc(100% - 18px);max-width:100%;min-width:0;"
				+ "overflow:auto;box-sizing:border-box;background:#ffffff;");
		skrol.setParent(bingkaiSkrol);
		grid.setParent(skrol);

		int totalBaris = 0;
		if (wb == null)
			return 0;
		org.zkoss.poi.ss.usermodel.Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
		if (sheet == null)
			return 0;

		int maxCol = 0;
		Iterator<org.zkoss.poi.ss.usermodel.Row> itHitung = sheet.iterator();
		while (itHitung.hasNext()) {
			org.zkoss.poi.ss.usermodel.Row r = itHitung.next();
			if (r != null && r.getLastCellNum() > maxCol)
				maxCol = r.getLastCellNum();
		}
		if (maxCol <= 0)
			maxCol = fallbackCols;
		if (maxCol <= 0)
			maxCol = 1;

		// Ukur panjang isi per kolom (header + maks 300 baris pertama) — pengganti
		// autoSizeColumn milik zss: lebar kolom = f(panjang teks terpanjang), dibatasi
		// 70..420px agar kolom kosong tetap terlihat dan kolom teks panjang tidak
		// menelan seluruh layar (sisanya terbaca via scroll mendatar).
		int[] panjangIsi = new int[maxCol];
		int barisUkur = 0;
		Iterator<org.zkoss.poi.ss.usermodel.Row> itUkur = sheet.iterator();
		while (itUkur.hasNext() && barisUkur < 300) {
			org.zkoss.poi.ss.usermodel.Row r = itUkur.next();
			barisUkur++;
			if (r == null)
				continue;
			for (int c = 0; c < maxCol; c++) {
				try {
					String v = cellToString(r.getCell(c));
					if (v != null && v.length() > panjangIsi[c])
						panjangIsi[c] = v.length();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PratinjauXlsxHelper: gagal ukur sel kolom " + c);
				}
			}
		}

		Columns columns = new Columns();
		columns.setParent(grid);
		org.zkoss.poi.ss.usermodel.Row headRow = baris1Header ? sheet.getRow(sheet.getFirstRowNum()) : null;
		int totalLebar = 0;
		for (int c = 0; c < maxCol; c++) {
			String h = headRow != null ? cellToString(headRow.getCell(c)) : "";
			Column col = new Column(h == null ? "" : h);
			int px = 24 + panjangIsi[c] * 8; // ±8px per karakter + ruang padding
			if (px < 70)
				px = 70;
			if (px > 420)
				px = 420;
			col.setWidth(px + "px");
			totalLebar += px;
			col.setParent(columns);
		}
		// Lebar grid = jumlah lebar kolom (bukan 100%) supaya tiap kolom mendapat
		// jatah pastinya; bila melebihi wadah, Div pembungkus yang menggulir.
		grid.setWidth((totalLebar + 20) + "px");
		lebarSkrolAtas.setWidth((totalLebar + 20) + "px");

		/* Sinkronkan scrollbar atas dengan area data. setTimeout diperlukan karena
		 * node ZK baru tersedia setelah AU response selesai dirender browser. */
		try {
			String idAtas = skrolAtas.getUuid();
			String idData = skrol.getUuid();
			Clients.evalJavaScript("setTimeout(function(){try{var a=document.getElementById('"
					+ idAtas + "'),b=document.getElementById('" + idData
					+ "');if(!a||!b)return;var k=false;a.onscroll=function(){if(k)return;k=true;b.scrollLeft=a.scrollLeft;k=false;};"
					+ "b.onscroll=function(){if(k)return;k=true;a.scrollLeft=b.scrollLeft;k=false;};"
					+ "b.onwheel=function(e){if(e.shiftKey){b.scrollLeft+=e.deltaY;e.preventDefault();}};}catch(x){}},120);");
		} catch (Exception eScroll) {
			ais.common.ErrorAuditUtil.record(eScroll, "PratinjauXlsxHelper: sinkron scrollbar horizontal");
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		boolean headerDilewati = !baris1Header; // jika header bukan baris pertama, jangan dilewati
		Iterator<org.zkoss.poi.ss.usermodel.Row> it = sheet.iterator();
		while (it.hasNext()) {
			org.zkoss.poi.ss.usermodel.Row r = it.next();
			if (!headerDilewati) {
				headerDilewati = true; // baris pertama sudah jadi header kolom
				continue;
			}
			if (totalBaris >= maksBaris) {
				totalBaris++;
				continue;
			}
			Row gr = new Row();
			gr.setParent(rows);
			for (int c = 0; c < maxCol; c++) {
				String v = r == null ? "" : cellToString(r.getCell(c));
				Label lab = new Label(v == null ? "" : v);
				lab.setStyle("white-space:nowrap;"); // biar lebar kolom mengikuti isi (tak membungkus)
				gr.appendChild(lab);
			}
			totalBaris++;
		}
		return totalBaris;
	}

	/**
	 * <b>Pola A.</b> Menampilkan file .xlsx yang sudah dibangun sebagai jendela pratinjau
	 * berisi Grid ringan + tombol "Download Data" (mengirim file .xlsx ASLI). Tinggi jendela
	 * dihitung otomatis dari jumlah baris. Dipanggil oleh {@code Common.displayXlsx}.
	 *
	 * @param fn           path absolut file .xlsx.
	 * @param intbox       jumlah baris data (info/estimasi).
	 * @param noUrutColumn jumlah kolom (fallback bila sheet kosong).
	 */
	public static void tampilkanXlsxRingan(String fn, Intbox intbox, int noUrutColumn) throws Exception {

		final MyWindow window = new MyWindow("Pratinjau Data", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setWidth("92%");

		MyBorderlayout borderlayout = new MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ZkCompat.setFlex(center, true);
		center.setBorder("none");
		// Autoscroll di-set di renderWorkbookKeGrid (Grid dipasang LANGSUNG di Center) — memberi
		// scroll mendatar & menegak yang andal tanpa Div height:100% yang kolaps di ZK 5.5.
		center.setParent(borderlayout);

		final File file = new File(fn);

		int totalBaris = 0;
		org.zkoss.poi.ss.usermodel.Workbook wb = null;
		FileInputStream in = null;
		try {
			if (file.exists() && file.length() > 0) {
				// Buka via PATH FILE (OPCPackage/ZipFile = random-access) — BUKAN InputStream
				// (ZipInputStream) yang gagal "Unexpected end of ZLIB input stream" pada .xlsx dengan ZIP
				// data descriptor (umum pada hasil tulis zss). 'in' dibiarkan null (finally-nya skip).
				wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook(file.getAbsolutePath());
				totalBaris = renderWorkbookKeGrid(center, wb, MAKS_BARIS_PREVIEW, noUrutColumn + 2, true);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			// Workbook versi zkoss-poi ini tak punya close(); cukup tutup InputStream-nya.
			if (in != null)
				try {
					in.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/PratinjauXlsxHelper.java:239");
				}
		}

		// Tinggi jendela OTOMATIS dari jumlah baris (maksimal 1 halaman), dengan plafon agar
		// tidak melebihi layar.
		int rowsShown = totalBaris <= 0 ? 1 : Math.min(totalBaris, PAGE_SIZE);
		int tinggi = 160 + (rowsShown * 26);
		if (tinggi > 760)
			tinggi = 760;
		if (tinggi < 240)
			tinggi = 240;
		window.setHeight(tinggi + "px");

		// Toolbar aksi (Download Data / Tutup) DILETAKKAN DI NORTH (atas) — bukan South (bawah).
		// Sebelumnya di South: pada tinggi/layar tertentu region South ter-render di luar area yang
		// bisa di-scroll (bahkan kadang baru muncul setelah jendela browser di-resize), sehingga
		// tombol "Download Data" TAK TERJANGKAU (keluhan pengguna: tidak bisa mengunduh data
		// Pratinjau di Tugas Kelompok & Diskusi). North menempel PASTI di atas, di atas Grid yang
		// tetap bisa di-scroll, sehingga tombol Download selalu terlihat & dapat diklik.
		North north = new North();
		north.setBorder("none");
		north.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		tambahCatatanBila(toolbar, totalBaris);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
		print.setTooltiptext("Unduh file Excel (.xlsx) lengkap");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!file.isFile() || file.length() <= 0L) {
					MyMessageboxConfig.show(
							"Berkas Excel belum berhasil dibuat. Periksa ruang penyimpanan server lalu proses kembali.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/PratinjauXlsxHelper.java:275");
				}
			}
		});
		print.setParent(toolbar);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
		Clients.clearBusy();

		// Koreksi tata letak pada render PERTAMA: region Borderlayout di dalam modal kadang belum
		// terukur hingga jendela browser di-resize manual (gejala CSS/layout ZK — konten "baru
		// muncul setelah di-resize"). Picu event 'resize' otomatis (dijeda 60 & 300 ms) setelah
		// modal tampil agar North/Center langsung terukur dan tombol Download Data tampak tanpa
		// perlu resize manual. Fallback createEvent untuk browser lama; dibungkus try/catch supaya
		// koreksi tampilan ini tak pernah mengganggu proses utama.
		try {
			Clients.evalJavaScript(
					"try{var _r=function(){try{var e;if(typeof(Event)==='function'){e=new Event('resize');}"
							+ "else{e=document.createEvent('Event');e.initEvent('resize',true,true);}"
							+ "window.dispatchEvent(e);}catch(x){}};setTimeout(_r,60);setTimeout(_r,300);}catch(x){}");
		} catch (Exception eResize) { ais.common.ErrorAuditUtil.record(eResize, "auto-audit(empty-catch) src/ais/ui/util/PratinjauXlsxHelper.java:306");
		}
	}

	/**
	 * <b>Pola B.</b> Mengganti tampilan widget zss Spreadsheet yang BERAT dengan Grid ringan
	 * yang dibaca dari {@code spreadsheet.getBook()}. Dipanggil di AKHIR proses pembangunan
	 * spreadsheet (setelah semua sel diisi) pada jendela laporan.
	 *
	 * <p>Widget zss dilepas dari pohon komponen setelah model {@code Book} diambil agar listener
	 * ukuran asinkronnya berhenti. Objek Java tetap dipegang field jendela, sehingga tombol
	 * Download masih menulis {@code spreadsheet.getBook()} yang sama dan file tetap utuh.</p>
	 *
	 * @param spreadsheet komponen MySpreadsheet yang sudah selesai diisi datanya.
	 */
	public static void gantiSpreadsheetDenganGrid(org.zkoss.zss.ui.Spreadsheet spreadsheet) {
		try {
			if (spreadsheet == null)
				return;
			if (spreadsheet instanceof MySpreadsheet) {
				((MySpreadsheet) spreadsheet).pasangSekarangJikaTertunda();
			}
			Component parent = spreadsheet.getParent();
			if (parent == null)
				return;

			int fallbackCols = 1;
			try {
				fallbackCols = spreadsheet.getMaxcolumns();
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/PratinjauXlsxHelper.java:335");
			}

			/* Ambil model dahulu, lalu DETACH widget ZSS sebelum memasang grid. Hanya
			 * setVisible(false) masih membiarkan InnerDataListener menerima event ukuran
			 * tertunda; setelah worksheet diganti/dibersihkan event itu membaca CTRow yang
			 * sudah disconnected dan melempar XmlValueDisconnectedException. Field laporan
			 * tetap memegang object spreadsheet beserta Book-nya, jadi tombol Download tetap
			 * menghasilkan workbook yang sama walaupun widget tidak dirender. */
			org.zkoss.poi.ss.usermodel.Workbook workbook =
					(org.zkoss.poi.ss.usermodel.Workbook) spreadsheet.getBook();
			spreadsheet.setVisible(false);
			spreadsheet.setParent(null);
			Component wadah = wadahAmanUntukPreview(parent);

			int totalBaris = renderWorkbookKeGrid(wadah, workbook,
					MAKS_BARIS_PREVIEW, fallbackCols, false);

			if (totalBaris > MAKS_BARIS_PREVIEW) {
				Label info = new Label("Pratinjau " + MAKS_BARIS_PREVIEW + " baris pertama dari " + totalBaris
						+ " baris. Klik \"Download\" untuk file Excel lengkap.");
				info.setStyle("color:#b45309;font-size:11px;font-weight:bold;");
				info.setParent(wadah);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menambahkan catatan "pratinjau N dari M" ke toolbar bila data melebihi batas muat. */
	private static void tambahCatatanBila(Toolbar toolbar, int totalBaris) {
		if (totalBaris > MAKS_BARIS_PREVIEW) {
			Label info = new Label("Pratinjau " + MAKS_BARIS_PREVIEW + " baris pertama dari " + totalBaris
					+ " baris. Klik \"Download Data\" untuk file Excel lengkap.");
			info.setStyle("color:#b45309;font-size:11px;margin-right:14px;font-weight:bold;");
			info.setParent(toolbar);
		}
	}
}
