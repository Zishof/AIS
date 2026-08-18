package ais.ui.util;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.IndexedColors;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zul.Html;

import ais.common.Common;

/**
 * Model "tabel laporan" pengganti zss {@code Spreadsheet} untuk dasbor rekap.
 *
 * <p>
 * Tujuannya: dasbor yang dulu menggambar laporan ke sebuah {@code Worksheet}
 * (lewat {@code EcampusUtil.setCellValue/mergeCells/setBold/setBorder} dan
 * {@code Utils.setColumnWidth/setRowHeight}) kini bisa memakai kelas ini
 * <b>tanpa mengubah satu pun logika perhitungan/penataan selnya</b>: cukup
 * arahkan pemanggilan dari {@code EcampusUtil}/{@code Utils} ke
 * {@code RekapTabel} (tanda tangan dibuat persis sama). Setelah sel terisi:
 * </p>
 * <ul>
 * <li>{@link #render(Component, String, String)} menampilkan tabel HTML/CSS
 * yang ringan dan responsif (sel gabung jadi {@code colspan}/{@code rowspan}),
 * menggantikan komponen {@code Spreadsheet} yang berat.</li>
 * <li>{@link #write(OutputStream)} menghasilkan berkas Excel (.xlsx) untuk
 * tombol Download — sumber datanya sama dengan yang tampil di layar.</li>
 * </ul>
 *
 * <p>
 * Satu kelas ini dipakai ulang oleh seluruh dasbor "Rekap" karena semuanya
 * memakai pola pengisian sel yang sama.
 * </p>
 */
public class RekapTabel {

	/** Satu sel: teks tampil + penanda gaya (tebal / kepala-tabel berarsir). */
	private static class Sel {
		String teks = "";
		boolean tebal;
		boolean kepala;
	}

	private final Map<Long, Sel> sel = new HashMap<Long, Sel>();
	private final List<int[]> gabung = new ArrayList<int[]>();
	private final Map<Integer, Integer> lebarKolom = new HashMap<Integer, Integer>();
	private final Map<Integer, Integer> tinggiBaris = new HashMap<Integer, Integer>();

	private int maxColumns;
	private int maxRows;
	private int lebarKolomDefault = 8;

	private int barisIsiMax = -1;
	private int kolomIsiMax = -1;
	private int barisIsiMin = Integer.MAX_VALUE;

	private static long kunci(int baris, int kolom) {
		return (((long) baris) << 20) | (kolom & 0xFFFFF);
	}

	// ------------------------------------------------------------------
	// API instance (dipakai langsung maupun lewat mirror statik di bawah)
	// ------------------------------------------------------------------

	public void setMaxcolumns(int v) {
		this.maxColumns = v;
	}

	public int getMaxcolumns() {
		return maxColumns;
	}

	public void setMaxrows(int v) {
		this.maxRows = v;
	}

	public int getMaxrows() {
		return maxRows;
	}

	public void setDefaultColumnWidth(int v) {
		this.lebarKolomDefault = v;
	}

	/** Isi nilai sel — meniru semantik {@code EcampusUtil.setCellValue(..,Object)}. */
	public void isiSel(int baris, int kolom, Object nilai) {
		Sel s = sel.get(kunci(baris, kolom));
		if (s == null) {
			s = new Sel();
			sel.put(kunci(baris, kolom), s);
		}
		String teks;
		if (nilai == null) {
			teks = "";
		} else if (nilai instanceof Number) {
			teks = Common.numberFormat.get().format(nilai);
		} else if (nilai instanceof java.util.Date) {
			teks = Common.dateFormat5.get().format(nilai);
		} else {
			teks = nilai.toString();
		}
		String mentah = nilai == null ? null : nilai.toString();
		boolean kepala = false;
		if ((teks != null && !teks.isEmpty() && baris == 1) || (mentah != null && mentah.startsWith("**"))) {
			kepala = true;
			if (mentah != null && mentah.startsWith("**")) {
				teks = mentah.substring(2);
			}
		}
		s.teks = teks == null ? "" : teks;
		if (kepala) {
			s.kepala = true;
			s.tebal = true;
		}
		catatIsi(baris, kolom, s.teks);
	}

	private void catatIsi(int baris, int kolom, String teks) {
		if (teks != null && !teks.isEmpty()) {
			if (baris > barisIsiMax) {
				barisIsiMax = baris;
			}
			if (kolom > kolomIsiMax) {
				kolomIsiMax = kolom;
			}
			if (baris < barisIsiMin) {
				barisIsiMin = baris;
			}
		}
	}

	/** Gabung sel pada rentang (anchor = pojok kiri-atas). */
	public void gabungSel(int barisAtas, int kolomKiri, int barisBawah, int kolomKanan) {
		gabung.add(new int[] { barisAtas, kolomKiri, barisBawah, kolomKanan });
		if (sel.get(kunci(barisAtas, kolomKiri)) == null) {
			sel.put(kunci(barisAtas, kolomKiri), new Sel());
		}
	}

	/** Tebalkan rentang sel. Saat {@code tebal=false} tidak membuat sel baru. */
	public void tebalkan(int kolom1, int baris1, int kolom2, int baris2, boolean tebal) {
		int rb = Math.min(baris1, baris2);
		int re = Math.max(baris1, baris2);
		int cb = Math.min(kolom1, kolom2);
		int ce = Math.max(kolom1, kolom2);
		for (int r = rb; r <= re; r++) {
			for (int c = cb; c <= ce; c++) {
				Sel s = sel.get(kunci(r, c));
				if (s == null) {
					if (!tebal) {
						continue;
					}
					s = new Sel();
					sel.put(kunci(r, c), s);
				}
				s.tebal = tebal;
			}
		}
	}

	public void setLebarKolom(int kolom, int lebar) {
		lebarKolom.put(Integer.valueOf(kolom), Integer.valueOf(lebar));
	}

	public void setTinggiBaris(int baris, int tinggi) {
		tinggiBaris.put(Integer.valueOf(baris), Integer.valueOf(tinggi));
	}

	// ------------------------------------------------------------------
	// Mirror statik bertanda tangan sama dengan EcampusUtil / Utils
	// (agar konversi dasbor cukup ganti prefix kelas, logika tetap)
	// ------------------------------------------------------------------

	public static void setCellValue(RekapTabel t, int baris, int kolom, Object nilai) {
		if (t != null) {
			t.isiSel(baris, kolom, nilai);
		}
	}

	public static void mergeCells(RekapTabel t, int barisAtas, int kolomKiri, int barisBawah, int kolomKanan,
			boolean across) {
		if (t != null) {
			t.gabungSel(barisAtas, kolomKiri, barisBawah, kolomKanan);
		}
	}

	public static void setBold(RekapTabel t, Rect rect, Boolean tebal) {
		if (t != null && rect != null) {
			t.tebalkan(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(),
					tebal != null && tebal.booleanValue());
		}
	}

	/** Border thin sudah universal di tampilan/HTML & xlsx → cukup no-op. */
	public static void setBorder(RekapTabel t, Rect rect, short borderFull, BorderStyle thin, String color) {
		// sengaja kosong
	}

	public static void setColumnWidth(RekapTabel t, int kolom, int lebar) {
		if (t != null) {
			t.setLebarKolom(kolom, lebar);
		}
	}

	public static void setRowHeight(RekapTabel t, int baris, int tinggi) {
		if (t != null) {
			t.setTinggiBaris(baris, tinggi);
		}
	}

	// ------------------------------------------------------------------
	// Tampilan: tabel HTML/CSS responsif
	// ------------------------------------------------------------------

	/**
	 * Render tabel ke dalam {@code induk}. {@code judul}/{@code deskripsi} boleh
	 * {@code null} (kepala kartu disembunyikan). Mengembalikan komponen HTML.
	 */
	public Component render(Component induk, String judul, String deskripsi) {
		Html html = new Html();
		html.setContent(toHtml(judul, deskripsi));
		if (induk != null) {
			html.setParent(induk);
		}
		return html;
	}

	public String toHtml(String judul, String deskripsi) {
		int nKolom = kolomIsiMax + 1;
		int barisAwal = barisIsiMin == Integer.MAX_VALUE ? 0 : barisIsiMin;
		int barisAkhir = barisIsiMax;

		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"ais-rekap-card\">");
		if ((judul != null && !judul.isEmpty()) || (deskripsi != null && !deskripsi.isEmpty())) {
			sb.append("<div class=\"ais-rekap-head\">");
			if (judul != null && !judul.isEmpty()) {
				sb.append("<div class=\"ais-rekap-title\">").append(esc(judul)).append("</div>");
			}
			if (deskripsi != null && !deskripsi.isEmpty()) {
				sb.append("<div class=\"ais-rekap-desc\">").append(esc(deskripsi)).append("</div>");
			}
			sb.append("</div>");
		}

		if (nKolom <= 0 || barisAkhir < 0) {
			sb.append("<div class=\"ais-rekap-empty\">Belum ada data untuk ditampilkan.</div></div>");
			return sb.toString();
		}

		Map<Long, int[]> rentang = new HashMap<Long, int[]>();
		Set<Long> tertutup = new HashSet<Long>();
		for (int i = 0; i < gabung.size(); i++) {
			int[] m = gabung.get(i);
			int tr = Math.max(barisAwal, Math.min(m[0], m[2]));
			int br = Math.min(barisAkhir, Math.max(m[0], m[2]));
			int lc = Math.max(0, Math.min(m[1], m[3]));
			int rc = Math.min(nKolom - 1, Math.max(m[1], m[3]));
			if (br < tr || rc < lc) {
				continue;
			}
			rentang.put(kunci(tr, lc), new int[] { br - tr + 1, rc - lc + 1 });
			for (int r = tr; r <= br; r++) {
				for (int c = lc; c <= rc; c++) {
					if (r == tr && c == lc) {
						continue;
					}
					tertutup.add(kunci(r, c));
				}
			}
		}

		sb.append("<div class=\"ais-rekap-wrap\"><table class=\"ais-rekap\"><tbody>");
		for (int r = barisAwal; r <= barisAkhir; r++) {
			sb.append("<tr>");
			for (int c = 0; c < nKolom; c++) {
				long k = kunci(r, c);
				if (tertutup.contains(Long.valueOf(k))) {
					continue;
				}
				Sel s = sel.get(Long.valueOf(k));
				String teks = s == null ? "" : s.teks;
				boolean kepala = s != null && s.kepala;
				boolean tebal = s != null && s.tebal;
				int[] sp = rentang.get(Long.valueOf(k));
				sb.append("<td");
				if (sp != null) {
					if (sp[0] > 1) {
						sb.append(" rowspan=\"").append(sp[0]).append("\"");
					}
					if (sp[1] > 1) {
						sb.append(" colspan=\"").append(sp[1]).append("\"");
					}
				}
				String cls = kepala ? "h" : (tebal ? "b" : null);
				if (cls != null) {
					sb.append(" class=\"").append(cls).append("\"");
				}
				sb.append(">").append(escBr(teks)).append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</tbody></table></div></div>");
		return sb.toString();
	}

	// ------------------------------------------------------------------
	// Ekspor Excel (.xlsx) — sumber data sama dengan tampilan
	// ------------------------------------------------------------------

	public void write(OutputStream out) throws Exception {
		XSSFWorkbook wb = new XSSFWorkbook();
		try {
			XSSFSheet sh = wb.createSheet("Rekap");
			if (lebarKolomDefault > 0) {
				try {
					sh.setDefaultColumnWidth(lebarKolomDefault);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/RekapTabel.java:334");
				}
			}

			XSSFCellStyle stNormal = gaya(wb, false, false);
			XSSFCellStyle stTebal = gaya(wb, true, false);
			XSSFCellStyle stKepala = gaya(wb, true, true);

			int barisAwal = barisIsiMin == Integer.MAX_VALUE ? 0 : barisIsiMin;
			int barisAkhir = barisIsiMax;
			int nKolom = kolomIsiMax + 1;

			for (int r = barisAwal; r <= barisAkhir; r++) {
				XSSFRow row = sh.createRow(r);
				Integer th = tinggiBaris.get(Integer.valueOf(r));
				if (th != null) {
					try {
						row.setHeightInPoints((float) (th.intValue() * 0.75));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/RekapTabel.java:352");
					}
				}
				for (int c = 0; c < nKolom; c++) {
					Sel s = sel.get(kunci(r, c));
					if (s == null) {
						continue;
					}
					XSSFCell xc = row.createCell(c);
					xc.setCellValue(s.teks);
					xc.setCellStyle(s.kepala ? stKepala : (s.tebal ? stTebal : stNormal));
				}
			}

			for (int i = 0; i < gabung.size(); i++) {
				int[] m = gabung.get(i);
				int tr = Math.min(m[0], m[2]);
				int br = Math.max(m[0], m[2]);
				int lc = Math.min(m[1], m[3]);
				int rc = Math.min(kolomIsiMax, Math.max(m[1], m[3]));
				if (br >= tr && rc > lc) {
					try {
						sh.addMergedRegion(new CellRangeAddress(tr, br, lc, rc));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/RekapTabel.java:375");
					}
				}
			}

			for (Map.Entry<Integer, Integer> e : lebarKolom.entrySet()) {
				try {
					int px = e.getValue().intValue();
					sh.setColumnWidth(e.getKey().intValue(), Math.max(1, (int) Math.round(px / 7.0 * 256)));
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/util/RekapTabel.java:384");
				}
			}

			wb.write(out);
		} finally {
			// XSSFWorkbook (POI bundel zss) tidak menyediakan close(); tidak ada sumber daya yang perlu dilepas.
		}
	}

	private static XSSFCellStyle gaya(XSSFWorkbook wb, boolean tebal, boolean kepala) {
		XSSFCellStyle st = wb.createCellStyle();
		st.setBorderBottom(XSSFCellStyle.BORDER_THIN);
		st.setBorderTop(XSSFCellStyle.BORDER_THIN);
		st.setBorderLeft(XSSFCellStyle.BORDER_THIN);
		st.setBorderRight(XSSFCellStyle.BORDER_THIN);
		XSSFFont f = wb.createFont();
		f.setBoldweight(tebal ? Font.BOLDWEIGHT_BOLD : Font.BOLDWEIGHT_NORMAL);
		st.setFont(f);
		if (kepala) {
			st.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			st.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		}
		return st;
	}

	// ------------------------------------------------------------------
	// util escape
	// ------------------------------------------------------------------

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder b = new StringBuilder(s.length() + 8);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '&':
				b.append("&amp;");
				break;
			case '<':
				b.append("&lt;");
				break;
			case '>':
				b.append("&gt;");
				break;
			case '"':
				b.append("&quot;");
				break;
			default:
				b.append(c);
			}
		}
		return b.toString();
	}

	private static String escBr(String s) {
		if (s == null) {
			return "";
		}
		return esc(s).replace("\n", "<br/>");
	}
}
