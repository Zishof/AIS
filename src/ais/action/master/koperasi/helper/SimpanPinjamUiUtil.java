package ais.action.master.koperasi.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h2>SimpanPinjamUiUtil — Utilitas Bersama Tampilan Dasbor &amp; Laporan Simpan Pinjam</h2>
 *
 * <p>
 * Kelas util statis ini memusatkan dua kebutuhan berulang pada seluruh halaman Unit Simpan Pinjam
 * (USP): (1) menampilkan sebuah tabel rincian sebagai <b>grid ZK</b> lengkap dengan tombol
 * <i>Unduh Excel</i>, dan (2) membangun <b>berkas Excel (.xlsx)</b> dari data tabel tersebut.
 * Dengan menyatukan logika ini di satu tempat, dasbor ({@code DashboardSimpanPinjamAction}) dan
 * laporan ({@code LaporanSimpanPinjamAction}) memakai kode yang sama persis — sehingga bila kelak
 * format tabel/Excel perlu diubah, cukup satu berkas yang disunting (memudahkan pemeliharaan).
 * </p>
 *
 * <h3>Kesesuaian dengan permintaan produk</h3>
 * <p>
 * Sesuai arahan, data <b>tidak</b> langsung disodorkan sebagai Excel: ia ditampilkan lebih dulu
 * sebagai grid ZK 5.x yang enak dibaca, dan berkas Excel asli baru dihasilkan ketika tombol
 * <i>Unduh Excel</i> ditekan. Angka dan rupiah diformat rapi, tanggal ditampilkan {@code dd-MM-yyyy}.
 * </p>
 *
 * <h3>Jenis kolom ({@code kinds})</h3>
 * <ul>
 * <li>{@link #TEKS} — teks apa adanya.</li>
 * <li>{@link #ANGKA} — bilangan (ribuan dipisah).</li>
 * <li>{@link #RUPIAH} — bilangan dengan awalan "Rp".</li>
 * <li>{@link #TANGGAL} — objek {@link Date} diformat {@code dd-MM-yyyy}.</li>
 * </ul>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Semua metode statis dan tanpa keadaan (stateless) sehingga aman dipakai lintas komposer. Metode
 * pembangun Excel menutup {@code FileOutputStream} dan {@code XSSFWorkbook} pada blok {@code finally}
 * (gaya try/catch kompatibel Java 1.6/1.7). Berkas ditulis ke berkas sementara sistem lalu dikirim
 * via {@link Filedownload}. Kelas final dengan konstruktor privat (tidak untuk di-instansiasi).
 * </p>
 */
public final class SimpanPinjamUiUtil {

	/** MIME untuk berkas .xlsx (Open XML Spreadsheet). */
	public static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	public static final int TEKS = 0;
	public static final int ANGKA = 1;
	public static final int RUPIAH = 2;
	public static final int TANGGAL = 3;

	private SimpanPinjamUiUtil() {
		// util statis: tidak untuk diinstansiasi
	}

	/**
	 * Tempel satu blok rekap ke {@code host}: judul, penjelasan sederhana, tombol Unduh Excel, lalu
	 * grid datanya. Bila {@code data} kosong, tetap menampilkan grid dengan pesan "Belum ada data".
	 *
	 * @param host     komponen induk tempat blok ditempel
	 * @param judul    judul blok (juga jadi label kolom pertama Excel bila perlu)
	 * @param desc     penjelasan singkat untuk end-user awam
	 * @param sheetName nama sheet pada berkas Excel
	 * @param baseNama prefiks nama berkas Excel sementara
	 * @param headers  label kolom
	 * @param kinds    jenis tiap kolom ({@link #TEKS}/{@link #ANGKA}/{@link #RUPIAH}/{@link #TANGGAL})
	 * @param data     baris data; tiap elemen berukuran sama dengan {@code headers}
	 */
	public static void appendRekapGrid(Component host, String judul, String desc, final String sheetName,
			final String baseNama, final String[] headers, final int[] kinds, final List<Object[]> data) {
		host.appendChild(DashboardUiKit.html("<div style='font-size:14px;font-weight:800;color:#0f172a;"
				+ "margin:18px 0 4px;border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:8px;'>"
				+ DashboardUiKit.esc(judul) + "</div>"));
		if (desc != null && desc.trim().length() > 0) {
			host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(desc)));
		}

		MyToolbarbuttonConfig dl = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.png");
		dl.setTooltiptext("Unduh tabel ini sebagai berkas Excel");
		dl.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					File file = buatExcel(baseNama, sheetName, headers, kinds, data);
					Filedownload.save(file, XLSX_MIME);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Mohon maaf, gagal menyiapkan berkas Excel. Langkah yang dapat dilakukan: (1) muat ulang halaman dan coba ekspor kembali; (2) pastikan koneksi jaringan stabil; (3) hubungi Administrator jika masalah berlanjut.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}
			}
		});
		host.appendChild(dl);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setSclass("dgrid");
		grid.setParent(host);

		Columns columns = new Columns();
		columns.setParent(grid);
		for (int c = 0; c < headers.length; c++) {
			MyColumnConfig col = new MyColumnConfig();
			col.setLabel(headers[c]);
			col.setParent(columns);
		}

		Rows rows = new Rows();
		rows.setParent(grid);
		if (data == null || data.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			new Label(ais.common.Common.getBahasaConfig("Belum ada data.")).setParent(row);
			return;
		}
		for (Object[] rowData : data) {
			Row row = new Row();
			row.setParent(rows);
			for (int c = 0; c < headers.length; c++) {
				Object v = rowData != null && c < rowData.length ? rowData[c] : null;
				new Label(cellText(v, kinds[c])).setParent(row);
			}
		}
	}

	/** Teks tampilan sebuah sel grid sesuai jenis kolomnya. */
	public static String cellText(Object v, int kind) {
		if (kind == RUPIAH) {
			return "Rp " + DashboardUiKit.money(v == null ? 0.0 : ((Number) v).doubleValue());
		} else if (kind == ANGKA) {
			return DashboardUiKit.money(v == null ? 0.0 : ((Number) v).doubleValue());
		} else if (kind == TANGGAL) {
			return v instanceof Date ? new SimpleDateFormat("dd-MM-yyyy").format((Date) v) : (v == null ? "" : v.toString());
		}
		return v == null ? "" : v.toString();
	}

	/** Bangun berkas Excel (.xlsx) satu-sheet dari data rekap dan kembalikan berkasnya. */
	public static File buatExcel(String baseNama, String sheetName, String[] headers, int[] kinds, List<Object[]> data)
			throws Exception {
		XSSFWorkbook wb = new XSSFWorkbook();
		org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(sanitize(sheetName));
		org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
		for (int c = 0; c < headers.length; c++) {
			head.createCell(c).setCellValue(headers[c]);
		}
		int r = 1;
		if (data != null) {
			for (Object[] rowData : data) {
				org.apache.poi.ss.usermodel.Row xr = sheet.createRow(r++);
				for (int c = 0; c < headers.length; c++) {
					Object v = rowData != null && c < rowData.length ? rowData[c] : null;
					org.apache.poi.ss.usermodel.Cell cell = xr.createCell(c);
					if (kinds[c] == RUPIAH || kinds[c] == ANGKA) {
						cell.setCellValue(v == null ? 0.0 : ((Number) v).doubleValue());
					} else if (kinds[c] == TANGGAL) {
						cell.setCellValue(v instanceof Date ? new SimpleDateFormat("dd-MM-yyyy").format((Date) v)
								: (v == null ? "" : v.toString()));
					} else {
						cell.setCellValue(v == null ? "" : v.toString());
					}
				}
			}
		}
		return simpanWorkbook(wb, baseNama);
	}

	/** Tulis workbook ke berkas sementara lalu tutup sumber daya (gaya try/catch 1.6). */
	private static File simpanWorkbook(XSSFWorkbook wb, String baseNama) throws Exception {
		File file = File.createTempFile(baseNama + "_", ".xlsx");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			wb.write(fos);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/SimpanPinjamUiUtil.java:198");
				}
			}
			try {
				wb.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/SimpanPinjamUiUtil.java:203");
			}
		}
		return file;
	}

	/** Bersihkan nama sheet Excel dari karakter yang tidak diperbolehkan (maks 31 karakter). */
	public static String sanitize(String s) {
		if (s == null) {
			return "Sheet";
		}
		String out = s.replaceAll("[\\\\/*?:\\[\\]]", " ").trim();
		if (out.length() > 31) {
			out = out.substring(0, 31);
		}
		return out.isEmpty() ? "Sheet" : out;
	}
}
