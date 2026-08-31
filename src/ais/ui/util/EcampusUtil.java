package ais.ui.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Workbook;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.util.IOUtils;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFClientAnchor;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFDrawing;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;

import ais.action.master.surat.util.SuratUtil;
import ais.common.Common;
import ais.database.model.file.LampiranLain;

/**
 * Kumpulan utilitas statis (grab-bag, tidak menyimpan state instance) yang dipakai layar-layar
 * eCampus AIS untuk menghasilkan dan menata keluaran berbentuk spreadsheet Excel (XLSX). Kelas ini
 * menjembatani dua dunia komponen spreadsheet yang dipakai aplikasi:
 * <ol>
 * <li><b>{@link org.zkoss.zss.ui.Spreadsheet}</b> — komponen ZK Spreadsheet (ZSS) yang menampilkan
 * berkas XLSX langsung di dalam halaman ZUL. Method {@link #tampilkan(List, Spreadsheet)} dan
 * {@link #tampilkan(List, Spreadsheet, boolean)} membangun berkas XLSX dari nol memakai API POI
 * hasil fork ZK ({@code org.zkoss.poi.*}) lalu menempelkannya ke komponen ini lewat
 * {@code setSrc(...)}.</li>
 * <li><b>{@link org.zkoss.zss.model.Worksheet}</b> — model data sheet ZSS yang biasanya sudah
 * terikat ke komponen {@code Spreadsheet} yang sedang ditampilkan di layar (mis. sheet rekap yang
 * diisi sel demi sel oleh kode pemanggil, bukan dibangun dari {@code List<List>} sekaligus). Seluruh
 * method {@code setCellValue}/{@code setCellValueBold}/{@code setBold}/{@code setBorder}/
 * {@code mergeCells} beroperasi pada level ini, memakai helper {@link org.zkoss.zss.ui.impl.Utils}
 * untuk mengambil-atau-membuat sel ({@code getOrCreateCell}) sebelum menata gaya (font, border,
 * warna latar) dan mengisi nilainya.</li>
 * </ol>
 *
 * <h2>Konvensi penataan yang dipakai berulang</h2>
 * <p>
 * Hampir seluruh method di kelas ini mengikuti satu konvensi visual yang sama, meniru gaya "laporan
 * resmi" dengan header abu-abu tebal dan badan tabel bergaris tipis:
 * </p>
 * <ul>
 * <li><b>Baris header</b> — baris pertama data (indeks 1 pada varian {@code setCellValue}/
 * {@code setCellValueBold}, atau baris kedua/{@code rowIndex==1} versi 0-based dalam
 * {@link #tampilkan(List, Spreadsheet, boolean)} lewat pengecekan yang setara) ditebalkan dan diberi
 * latar abu-abu ({@code IndexedColors.GREY_40_PERCENT} pada varian {@code Worksheet}, atau
 * {@code Color.LIGHT_GRAY} pada varian {@code XSSFWorkbook} di {@link #tampilkan}).</li>
 * <li><b>Penanda label manual</b> — nilai teks apa pun (di baris mana pun) yang <i>diawali</i> dua
 * tanda bintang ({@code "**"}) diperlakukan sebagai label/sub-header: diberi gaya header (tebal +
 * latar abu-abu) yang sama seperti baris pertama, dan prefiks {@code "**"} dibuang sebelum
 * ditampilkan — kecuali pada {@link #setCellValueBold(Worksheet, int, int, Object)} yang memiliki
 * kuirk tersendiri (lihat catatan pada method tersebut).</li>
 * <li><b>Baris/sel biasa</b> — border tipis di keempat sisi, latar putih, font normal; nilai numerik
 * diformat lewat {@code Common.numberFormat}, nilai {@link java.util.Date} lewat
 * {@code Common.dateFormat5} (khusus varian {@code Object}).</li>
 * </ul>
 *
 * <h2>Overload per tipe nilai</h2>
 * <p>
 * {@code setCellValue}/{@code setCellValueBold} hadir dalam lima varian ({@link Object},
 * {@link Long}, {@link Integer}, {@link Double}, {@link String}) yang secara fungsional serupa
 * (deteksi header/label, border tipis, format angka) tetapi TIDAK saling delegasi — tiap overload
 * mengulang logikanya sendiri secara independen. Overload {@link Object} adalah yang paling lengkap
 * (satu-satunya yang menangani {@link java.util.Date} secara eksplisit); overload bertipe spesifik
 * ({@code Long}/{@code Integer}/{@code Double}) tidak memiliki cabang deteksi label {@code "**"}
 * karena nilainya berupa angka murni, sehingga hanya baris header ({@code rowIndex==1}) yang ditebalkan.
 * </p>
 *
 * <h2>Ketergantungan lintas paket</h2>
 * <p>
 * {@link #tampilkan(List, Spreadsheet, boolean)} mengambil kop surat institusi lewat
 * {@code ais.action.master.surat.util.SuratUtil#ambilKopLampiranLain()} dan menyisipkannya sebagai
 * gambar di baris-baris teratas sheet, serta menulis berkas sementara ke bawah
 * {@code Common.REAL_PATH + "/tmp/"} dengan nama yang mengandung timestamp dari
 * {@link ais.ui.util.WaktuUtil#getDate()} — berkas ini TIDAK dihapus otomatis oleh method ini
 * (pembersihan berkas lama di direktori {@code tmp} menjadi tanggung jawab proses lain).
 * </p>
 *
 * <p>
 * <b>Catatan campuran API POI</b> — sebagian besar kelas di file ini memakai POI hasil fork ZK
 * ({@code org.zkoss.poi.*}), namun satu import ({@code org.apache.poi.ss.usermodel.IndexedColors})
 * berasal dari Apache POI asli. Ini tidak menimbulkan masalah kompilasi karena {@code IndexedColors}
 * hanya dipakai sebagai sumber konstanta indeks warna (mis. {@code GREY_40_PERCENT}, {@code WHITE})
 * yang diteruskan sebagai {@code short}, kompatibel dengan API {@code setFillForegroundColor} pada
 * kedua varian POI.
 * </p>
 */
public class EcampusUtil {

	/**
	 * Varian ringkas {@link #tampilkan(List, Spreadsheet, boolean)} dengan {@code auto=true}, yaitu
	 * lebar kolom akan disesuaikan otomatis ({@code autoSizeColumn}) untuk setiap kolom yang diisi.
	 *
	 * @param datas   data tabel yang akan dirender, baris demi baris; boleh {@code null} (diperlakukan
	 *                sebagai daftar kosong)
	 * @param excelku komponen ZK Spreadsheet tujuan; wajib tidak {@code null}
	 * @throws Exception diteruskan dari kegagalan pembuatan workbook POI atau penulisan berkas XLSX
	 */
	@SuppressWarnings("rawtypes")
	public static void tampilkan(List<List> datas, Spreadsheet excelku) throws Exception {
		tampilkan(datas, excelku, true);
	}

	/**
	 * Membangun satu berkas XLSX dari nol (memakai {@link org.zkoss.poi.xssf.usermodel.XSSFWorkbook})
	 * berisi data {@code datas}, menuliskannya ke direktori sementara aplikasi, lalu menempelkannya ke
	 * komponen {@code excelku} ({@link org.zkoss.zss.ui.Spreadsheet}) agar tampil di layar ZK.
	 *
	 * <p>
	 * Alur kerja: (1) tentukan lebar tabel ({@code lebar}, minimal 1) dari kolom terbanyak di antara
	 * seluruh baris data mulai indeks baris {@code mulaiRow=4}; (2) siapkan dua {@code CellStyle} —
	 * {@code hlink_style} untuk baris pertama/label {@code "**"} (tebal, latar abu-abu muda, border
	 * bawah ganda) dan {@code bodystyle} untuk sel biasa (normal, border tipis); (3) isi setiap sel:
	 * nilai {@link Integer}/{@link Double} ditulis sebagai angka asli, nilai lain ditulis sebagai
	 * teks (dengan prefiks {@code "**"} dibuang bila ada); baris yang seluruh selnya kosong tidak
	 * diberi gaya sama sekali (dibiarkan polos); (4) bila {@code auto} bernilai {@code true}, lebar
	 * tiap kolom disesuaikan otomatis lewat {@code sheet.autoSizeColumn} (kegagalan per-kolom
	 * ditelan diam-diam agar satu kolom bermasalah tidak menggagalkan seluruh render); (5) bila kop
	 * surat institusi tersedia ({@code SuratUtil.ambilKopLampiranLain()}), gambar tersebut disisipkan
	 * di kolom 0-7 baris 0-5 sheet; (6) workbook ditulis ke berkas fisik di bawah
	 * {@code Common.REAL_PATH + "/tmp/rekap_<timestamp>.xlsx"}; (7) komponen {@code excelku} diarahkan
	 * ke berkas tersebut (path relatif {@code "../../tmp/"}) beserta atribut tampilan (lebar/tinggi
	 * 100%, border, {@code maxrows}/{@code maxcolumns} mengikuti ukuran data).
	 * </p>
	 *
	 * @param datas   data tabel yang akan dirender, baris demi baris (setiap elemen {@link List}
	 *                mewakili satu baris; baris {@code null} dilewati); boleh {@code null}
	 *                (diperlakukan sebagai daftar kosong)
	 * @param excelku komponen ZK Spreadsheet tujuan; method melempar
	 *                {@link IllegalArgumentException} bila {@code null}
	 * @param auto    bila {@code true}, lebar kolom disesuaikan otomatis setelah setiap sel diisi
	 * @throws Exception diteruskan dari kegagalan pembuatan workbook POI, pembacaan berkas kop surat,
	 *                    atau penulisan berkas XLSX ke disk
	 */
	@SuppressWarnings("rawtypes")
	public static void tampilkan(List<List> datas, Spreadsheet excelku, boolean auto) throws Exception {
		if (excelku == null) {
			throw new IllegalArgumentException("Komponen spreadsheet belum tersedia");
		}
		if (datas == null) {
			datas = new java.util.ArrayList<List>();
		}

		String fn = Common.REAL_PATH + "/tmp/rekap_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx";

		int mulaiRow = 4;
		int lebar = 1;
		for (int rowIndex = mulaiRow; rowIndex < datas.size() + mulaiRow; rowIndex++) {
			List sub = datas.get(rowIndex - mulaiRow);
			if (sub == null) {
				continue;
			}
			if (sub.size() > lebar) {
				lebar = sub.size();
			}
		}

		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFFont hlink_font = workbook.createFont();
		hlink_font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
		hlink_font.setColor(new XSSFColor(Color.BLACK));

		XSSFCellStyle hlink_style = workbook.createCellStyle();
		hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
		hlink_style.setFont(hlink_font);

		hlink_style.setBorderLeft(BorderStyle.THIN);
		hlink_style.setBorderTop(BorderStyle.THIN);
		hlink_style.setBorderRight(BorderStyle.THIN);
		hlink_style.setBorderBottom(BorderStyle.DOUBLE);

		hlink_style.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

		XSSFFont bodyfont = workbook.createFont();
		bodyfont.setBoldweight(XSSFFont.BOLDWEIGHT_NORMAL);
		bodyfont.setColor(new XSSFColor(Color.BLACK));

		XSSFCellStyle bodystyle = workbook.createCellStyle();
		bodystyle.setFont(bodyfont);

		bodystyle.setBorderLeft(BorderStyle.THIN);
		bodystyle.setBorderTop(BorderStyle.THIN);
		bodystyle.setBorderRight(BorderStyle.THIN);
		bodystyle.setBorderBottom(BorderStyle.THIN);

		bodystyle.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

		XSSFSheet sheet = workbook.createSheet("Data");
		sheet.setDefaultColumnWidth(20);

		for (int rowIndex = mulaiRow; rowIndex < datas.size() + mulaiRow; rowIndex++) {

			List sub = datas.get(rowIndex - mulaiRow);
			if (sub == null) {
				continue;
			}
			boolean blank = true;
			for (int colIndex = 0; colIndex < sub.size(); colIndex++) {

				Object value = sub.get(colIndex);

				blank &= (value == null || value.toString().trim().isEmpty());
			}

			XSSFRow row = sheet.createRow(rowIndex);

			for (int colIndex = 0; colIndex < sub.size(); colIndex++) {

				Object value = sub.get(colIndex);
				XSSFCell cell = row.createCell(colIndex);

				if (!blank) {
					if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
							|| (value != null && value.toString().startsWith("**"))) {
						cell.setCellStyle(hlink_style);
					} else {
						cell.setCellStyle(bodystyle);
					}
				}

				if (value != null && value instanceof Integer) {

					cell.setCellValue((Integer) value);

				} else if (value != null && value instanceof Double) {
					cell.setCellValue((Double) value);

				} else {
					cell.setCellValue(value == null ? ""
							: ((value != null && value.toString().startsWith("**") ? value.toString().substring(2)
									: value.toString())));
				}

				if (auto) {
					try {
						sheet.autoSizeColumn(colIndex);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:146");
						// TODO: handle exception
					}
				}
			}

		}

		LampiranLain kopLampiranLain = SuratUtil.ambilKopLampiranLain();
		String kop = kopLampiranLain == null ? "" : kopLampiranLain.ambilFile().getAbsolutePath();
//		System.out.println("kop -> " + kop);

		if (!kop.isEmpty()) {

			InputStream inputStream1 = null;
			byte[] inputImageBytes1;
			try {
				inputStream1 = new FileInputStream(kop);
				inputImageBytes1 = IOUtils.toByteArray(inputStream1);
			} finally {
				if (inputStream1 != null) try { inputStream1.close(); } catch (IOException ignore) { }
			}

			int inputImagePictureID1 = workbook.addPicture(inputImageBytes1, Workbook.PICTURE_TYPE_JPEG);
			XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
			XSSFClientAnchor ironManAnchor = new XSSFClientAnchor();
			ironManAnchor.setCol1(0); // Sets the column (0 based) of the first cell.
			ironManAnchor.setCol2(7); // Sets the column (0 based) of the Second cell.
			ironManAnchor.setRow1(0); // Sets the row (0 based) of the first cell.
			ironManAnchor.setRow2(5); // Sets the row (0 based) of the Second cell.

			drawing.createPicture(ironManAnchor, inputImagePictureID1);
		}

		File file = new File(fn);

		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(file);
			workbook.write(fileOut);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (fileOut != null) try { fileOut.close(); } catch (IOException ignore) { }
		}

		excelku.setSrc("../../tmp/" + file.getName());
		excelku.setStyle("border:1px solid #8AA3C1");
		excelku.setHeight("100%");
		excelku.setWidth("100%");
		excelku.setMaxrows(datas.size() + mulaiRow + 5);
		excelku.setMaxcolumns(lebar);

	}

	/**
	 * Menulis {@code value} ke sel {@code (rowIndex, colIndex)} pada {@code sheet}, dengan deteksi
	 * otomatis gaya header/label: baris pertama ({@code rowIndex==1}) atau nilai teks berprefiks
	 * {@code "**"} ditebalkan dengan latar abu-abu (prefiks dibuang dari tampilan); sel lain diberi
	 * latar putih. Border tipis diterapkan di keempat sisi untuk semua sel. Nilai {@link Number}
	 * diformat lewat {@code Common.numberFormat}, nilai {@link Date} lewat {@code Common.dateFormat5},
	 * nilai lain memakai {@code toString()}. Kegagalan (mis. sel tidak dapat dibuat) ditangkap dan
	 * dialihkan ke {@code Common.tampilErrorJikaAdmin} alih-alih dilempar ke pemanggil.
	 *
	 * @param sheet    sheet ZSS tujuan
	 * @param rowIndex indeks baris (0-based); baris 1 diperlakukan sebagai header
	 * @param colIndex indeks kolom (0-based)
	 * @param value    nilai yang ditulis; boleh {@code null} (ditulis sebagai string kosong)
	 */
	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Object value) {
		try {

			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.toString().startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.toString().startsWith("**"))) {
					cell.setCellValue(value.toString().substring(2));
				} else {
					cell.setCellValue(value == null ? ""
							: (value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());

				cell.setCellValue(value == null ? ""
						: (value instanceof Date ? Common.dateFormat5.get().format(value)
								: value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
			}
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Varian {@link #setCellValue(Worksheet, int, int, Object)} khusus {@link Long}: tidak memiliki
	 * deteksi label {@code "**"} (nilai selalu numerik murni); hanya baris header
	 * ({@code rowIndex==1}) yang ditebalkan dan diberi latar abu-abu. Nilai diformat lewat
	 * {@code Common.numberFormat}.
	 *
	 * @param sheet    sheet ZSS tujuan
	 * @param rowIndex indeks baris (0-based); baris 1 diperlakukan sebagai header
	 * @param colIndex indeks kolom (0-based)
	 * @param value    nilai numerik; boleh {@code null} (ditulis sebagai string kosong)
	 */
	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Long value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Seperti {@link #setCellValue(Worksheet, int, int, Long)}, untuk nilai bertipe {@link Integer}. */
	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Integer value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Seperti {@link #setCellValue(Worksheet, int, int, Long)}, untuk nilai bertipe {@link Double}. */
	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Double value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Varian {@link #setCellValue(Worksheet, int, int, Object)} khusus {@link String}: memiliki
	 * kembali deteksi label {@code "**"} (prefiks dibuang bila ditemukan, sel ditebalkan dengan latar
	 * abu-abu) selain deteksi baris header ({@code rowIndex==1}). Nilai ditulis apa adanya (tanpa
	 * pemformatan angka/tanggal karena sudah berupa {@link String}).
	 *
	 * @param sheet    sheet ZSS tujuan
	 * @param rowIndex indeks baris (0-based); baris 1 diperlakukan sebagai header
	 * @param colIndex indeks kolom (0-based)
	 * @param value    nilai teks; boleh {@code null} (ditulis sebagai string kosong); prefiks
	 *                 {@code "**"} menandai sel sebagai label dan dibuang dari tampilan
	 */
	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, String value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.startsWith("**"))) {
					cell.setCellValue(value.substring(2));
				} else {
					cell.setCellValue(value == null ? "" : value);
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				cell.setCellValue(value == null ? "" : value);
			}
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Seperti {@link #setCellValue(Worksheet, int, int, Object)}, tetapi font SELALU ditebalkan
	 * (bukan hanya pada baris header/label). Latar tetap mengikuti deteksi header
	 * ({@code rowIndex==1}) atau prefiks {@code "**"} — abu-abu untuk header/label, putih untuk sel
	 * biasa.
	 *
	 * <p>
	 * <b>Catatan perilaku</b> — berbeda dari {@link #setCellValue(Worksheet, int, int, Object)}, pada
	 * varian ini nilai sel ditulis dua kali: sekali di dalam percabangan header/label (yang membuang
	 * prefiks {@code "**"} bila ada), dan sekali lagi tanpa syarat setelah percabangan tersebut
	 * (dengan nilai penuh, TANPA membuang prefiks {@code "**"}). Penulisan kedua ini menimpa
	 * penulisan pertama, sehingga untuk nilai teks berprefiks {@code "**"}, prefiks tersebut TETAP
	 * tampil di sel akhir walaupun sel diberi gaya header/label — berbeda dari
	 * {@link #setCellValue(Worksheet, int, int, Object)} maupun
	 * {@link #setCellValueBold(Worksheet, int, int, String)} yang berhasil membuang prefiksnya.
	 * </p>
	 *
	 * @param sheet    sheet ZSS tujuan
	 * @param rowIndex indeks baris (0-based); baris 1 diperlakukan sebagai header
	 * @param colIndex indeks kolom (0-based)
	 * @param value    nilai yang ditulis; boleh {@code null} (ditulis sebagai string kosong)
	 */
	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Object value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.toString().startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.toString().startsWith("**"))) {
					cell.setCellValue(value.toString().substring(2));
				} else {
					cell.setCellValue(value == null ? ""
							: (value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? ""
					: (value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Varian {@link #setCellValueBold(Worksheet, int, int, Object)} khusus {@link Long}: font selalu
	 * tebal; latar abu-abu hanya pada baris header ({@code rowIndex==1}), tidak ada deteksi label
	 * {@code "**"} (nilai selalu numerik murni). Nilai diformat lewat {@code Common.numberFormat}.
	 *
	 * @param sheet    sheet ZSS tujuan
	 * @param rowIndex indeks baris (0-based); baris 1 diperlakukan sebagai header
	 * @param colIndex indeks kolom (0-based)
	 * @param value    nilai numerik; boleh {@code null} (ditulis sebagai string kosong)
	 */
	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Long value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Seperti {@link #setCellValueBold(Worksheet, int, int, Long)}, untuk nilai bertipe {@link Integer}. */
	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Integer value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Seperti {@link #setCellValueBold(Worksheet, int, int, Long)}, untuk nilai bertipe {@link Double}. */
	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Double value) {

		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Varian {@link #setCellValueBold(Worksheet, int, int, Object)} khusus {@link String}: font
	 * selalu tebal; berbeda dari overload {@link Object}, method ini TIDAK memiliki penulisan nilai
	 * ganda sehingga prefiks {@code "**"} berhasil dibuang dari tampilan saat sel diberi gaya
	 * header/label — perilaku ini konsisten dengan {@link #setCellValue(Worksheet, int, int, String)}.
	 *
	 * @param sheet    sheet ZSS tujuan
	 * @param rowIndex indeks baris (0-based); baris 1 diperlakukan sebagai header
	 * @param colIndex indeks kolom (0-based)
	 * @param value    nilai teks; boleh {@code null} (ditulis sebagai string kosong); prefiks
	 *                 {@code "**"} menandai sel sebagai label dan dibuang dari tampilan
	 */
	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, String value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.startsWith("**"))) {
					cell.setCellValue(value.substring(2));
				} else {
					cell.setCellValue(value == null ? "" : value);
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				cell.setCellValue(value == null ? "" : value);
			}
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menebalkan (atau menghilangkan penebalan) font untuk seluruh sel dalam area {@code rect} pada
	 * {@code sheet}. Sekadar pembungkus tipis atas {@link Utils#setFontBold(Worksheet, Rect, Boolean)}
	 * yang menelan pengecualian secara diam-diam (dicatat ke {@code ErrorAuditUtil}) agar kegagalan
	 * penataan gaya tidak menggagalkan alur tampilan yang memanggilnya.
	 *
	 * @param sheet  sheet ZSS tujuan
	 * @param rect   area sel yang ditata (baris/kolom awal-akhir)
	 * @param isBold {@code true} untuk menebalkan, {@code false} untuk mengembalikan ke normal
	 */
	public static void setBold(Worksheet sheet, Rect rect, Boolean isBold) {

		try {
			Utils.setFontBold(sheet, rect, isBold);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:531");

		}

	}

	/**
	 * Menerapkan gaya border pada seluruh sel dalam area {@code rect}. Sekadar pembungkus tipis atas
	 * {@link Utils#setBorder(Worksheet, Rect, short, BorderStyle, String)} yang menelan pengecualian
	 * secara diam-diam (dicatat ke {@code ErrorAuditUtil}).
	 *
	 * @param sheet      sheet ZSS tujuan
	 * @param rect       area sel yang ditata
	 * @param borderFull kombinasi bit sisi border yang diterapkan (lihat konstanta {@code Rect}/API ZSS)
	 * @param thin       gaya garis border (mis. tipis, tebal, putus-putus)
	 * @param color      warna border dalam format yang diterima {@link Utils#setBorder}
	 */
	public static void setBorder(Worksheet sheet, Rect rect, short borderFull, BorderStyle thin, String color) {
//		setBorder(sheet, rect);

		try {
			Utils.setBorder(sheet, rect, borderFull, thin, color);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:542");
//			setBorder(sheet, rect);
		}
	}

	/**
	 * Menggabung area sel {@code (tRow, lCol)} sampai {@code (bRow, rCol)} menjadi satu sel gabungan
	 * (merged region), lalu meratakan konten sel pojok kiri-atas ke kiri ({@code ALIGN_LEFT}).
	 * Kegagalan (mis. area tumpang tindih dengan merge lain) ditangkap dan dicatat ke
	 * {@code ErrorAuditUtil}, tidak dilempar ke pemanggil.
	 *
	 * @param sheet  sheet ZSS tujuan
	 * @param tRow   baris atas area yang digabung
	 * @param lCol   kolom kiri area yang digabung
	 * @param bRow   baris bawah area yang digabung
	 * @param rCol   kolom kanan area yang digabung
	 * @param across parameter tidak dipakai oleh implementasi saat ini (area selalu digabung penuh
	 *               sebagai satu region, bukan per-baris); dipertahankan untuk kompatibilitas
	 *               signature pemanggil
	 */
	public static void mergeCells(Worksheet sheet, int tRow, int lCol, int bRow, int rCol, boolean across) {
		try {
			sheet.addMergedRegion(new CellRangeAddress(tRow, bRow, lCol, rCol));
			Cell cell = Utils.getOrCreateCell(sheet, tRow, lCol);
			CellStyle cellStyle = cell.getCellStyle();
			cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:553");

		}
	}
}
