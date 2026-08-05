package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tugas;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.DashboardUiKit;

/**
 * Tampilan rekap tugas yang dapat dipakai ulang: mengubah data rekap (yang dulu hanya berupa file
 * Excel) menjadi grid/tabel ZK + grafik HTML/CSS modern, sekaligus menyiapkan file Excel asli untuk
 * diunduh saat tombol unduh ditekan.
 *
 * Dipakai oleh {@code RekapHasilTugasPerVoPertemuan} (banyak mahasiswa) dan
 * {@code RekapHasilTugasMahasiswa} (satu mahasiswa). Semua method statik & bebas-state agar mudah
 * dipelihara dan dipanggil dari mana saja.
 */
public final class RekapNilaiView {

	private RekapNilaiView() {
	}

	/** Satu sel nilai tugas milik seorang peserta. */
	public static final class Cell {
		/** Peserta wajib mengerjakan tugas ini. */
		public final boolean ikut;
		/** Peserta sudah mengumpulkan (ada berkas). */
		public final boolean submit;
		/** Nilai tugas (boleh null jika belum dinilai / belum mengumpulkan). */
		public final Double nilai;
		/** Teks tampil: "namafile (nilai)", kosong, atau "Tidak perlu ikut". */
		public final String display;

		public Cell(boolean ikut, boolean submit, Double nilai, String display) {
			this.ikut = ikut;
			this.submit = submit;
			this.nilai = nilai;
			this.display = display == null ? "" : display;
		}

		/** Sel untuk peserta yang tidak perlu ikut tugas tertentu. */
		public static Cell tidakIkut() {
			return new Cell(false, false, null, "Tidak perlu ikut");
		}
	}

	/** Satu baris rekap (satu peserta) berikut nilai per tugas dan ringkasannya. */
	public static final class Peserta {
		public final String nim;
		public final String nama;
		public final String prodi;
		public final String kelas;
		public final List<Cell> cells = new ArrayList<Cell>();
		public double total;
		public int jumlahIkut;
		public double rataRata;

		public Peserta(String nim, String nama, String prodi, String kelas) {
			this.nim = nim == null ? "" : nim;
			this.nama = nama == null ? "" : nama;
			this.prodi = prodi == null ? "" : prodi;
			this.kelas = kelas == null ? "" : kelas;
		}

		/** Tambah satu sel & perbarui ringkasan (total, jumlah ikut, rata-rata). */
		public void tambah(Cell cell) {
			cells.add(cell);
			if (cell.ikut) {
				jumlahIkut++;
				total += cell.nilai == null ? 0.0 : cell.nilai.doubleValue();
			}
			rataRata = jumlahIkut <= 0 ? 0.0 : total / jumlahIkut;
		}
	}

	// ============================================================
	// PENGISI MODEL (dipakai ulang oleh kedua dasbor rekap)
	// ============================================================

	/**
	 * Isi nilai seorang peserta untuk setiap tugas, memakai aturan "tidak perlu ikut" yang sama
	 * persis seperti pembuatan Excel lama. Salah satu dari {@code mahasiswa}/{@code cal} boleh null.
	 */
	public static void isiNilai(Peserta p, Long id, Mahasiswa mahasiswa, BiodataCalonMahasiswa cal,
			List<Tugas> tugasAll, Map<Long, TreeMap<Long, TugasFileContent>> nilais) {
		if (tugasAll == null) {
			return;
		}
		for (Tugas tugas : tugasAll) {
			if (tugas.getMhsYgTidakIkut().contains("," + id + ",")) {
				p.tambah(Cell.tidakIkut());
				continue;
			}
			TreeMap<Long, TugasFileContent> d = nilais == null ? null : nilais.get(tugas.getId());
			TugasFileContent fc = d != null && d.containsKey(id) ? d.get(id) : null;
			Double nilai = fc != null ? fc.getNilai() : null;
			String display;
			if (fc == null) {
				display = "";
			} else {
				String namaFile = mahasiswa != null ? fc.ambilRealNameSesuaiDenganNIM(mahasiswa)
						: fc.ambilRealNameSesuaiDenganNIM(cal);
				display = namaFile + " (" + Common.numberFormat.get().format(nilai == null ? 0.0 : nilai.doubleValue())
						+ ")";
			}
			p.tambah(new Cell(true, fc != null, nilai, display));
		}
	}

	/** Gabungkan pilihan prodi 1..5 calon mahasiswa menjadi satu teks. */
	public static String prodiPilihan(BiodataCalonMahasiswa cal) {
		if (cal == null) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		tambahProdi(s, cal.getProdi1() == null ? null : cal.getProdi1().getNama());
		tambahProdi(s, cal.getProdi2() == null ? null : cal.getProdi2().getNama());
		tambahProdi(s, cal.getProdi3() == null ? null : cal.getProdi3().getNama());
		tambahProdi(s, cal.getProdi4() == null ? null : cal.getProdi4().getNama());
		tambahProdi(s, cal.getProdi5() == null ? null : cal.getProdi5().getNama());
		return s.toString();
	}

	private static void tambahProdi(StringBuilder s, String nama) {
		if (nama == null || nama.trim().isEmpty()) {
			return;
		}
		if (s.length() > 0) {
			s.append(", ");
		}
		s.append(nama);
	}

	// ============================================================
	// GRID / TABEL (ZK)
	// ============================================================

	/**
	 * Bangun grid ZK berisi seluruh rekap: No, NIM/No.Reg, Nama, Prodi, Kelas, satu kolom per tugas,
	 * lalu Total, Ikut Tugas, dan Rata-Rata. Otomatis ber-paging bila baris banyak.
	 */
	public static Grid tableGrid(List<String> judul, List<Peserta> peserta) {
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0;background:transparent;");
		if (peserta != null && peserta.size() > 30) {
			grid.setMold("paging");
			grid.setPageSize(30);
			try {
				grid.getPagingChild().setMold("os");
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapNilaiView.java:174");
			}
		}

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		tambahKolom(columns, "No.", "48px");
		tambahKolom(columns, "NIM / No. Reg", "150px");
		tambahKolom(columns, "Nama", null);
		tambahKolom(columns, "Prodi", "150px");
		tambahKolom(columns, "Kelas", "90px");
		if (judul != null) {
			for (String j : judul) {
				tambahKolom(columns, j, "160px");
			}
		}
		tambahKolom(columns, "Total", "90px");
		tambahKolom(columns, "Ikut (x)", "80px");
		tambahKolom(columns, "Rata-Rata", "100px");

		Rows rows = new Rows();
		rows.setParent(grid);
		int no = 1;
		if (peserta != null) {
			for (Peserta p : peserta) {
				Row r = new Row();
				r.setValign("middle");
				r.setParent(rows);
				selLabel(r, String.valueOf(no), false);
				selLabel(r, p.nim, true);
				selLabel(r, p.nama, true);
				selLabel(r, p.prodi, false);
				selLabel(r, p.kelas, false);
				for (Cell c : p.cells) {
					Label lc = new Label(c.display);
					if (!c.ikut) {
						lc.setStyle("color:#94a3b8;font-style:italic;");
					} else if (!c.submit) {
						lc.setStyle("color:#f97316;");
					}
					lc.setParent(r);
				}
				selLabel(r, Common.numberFormat.get().format(p.total), false);
				selLabel(r, String.valueOf(p.jumlahIkut), false);
				Label rata = new Label(Common.numberFormat.get().format(p.rataRata));
				rata.setStyle("font-weight:800;color:" + warnaNilai(p.rataRata) + ";");
				rata.setParent(r);
				no++;
			}
		}
		return grid;
	}

	private static void tambahKolom(Columns columns, String label, String width) {
		Column c = new Column();
		c.setLabel(label);
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(columns);
	}

	private static void selLabel(Row r, String text, boolean bold) {
		Label l = new Label(text == null ? "" : text);
		if (bold) {
			l.setStyle("font-weight:700;");
		}
		l.setParent(r);
	}

	private static String warnaNilai(double nilai) {
		if (nilai >= 80) {
			return DashboardUiKit.GOOD;
		}
		if (nilai >= 60) {
			return DashboardUiKit.WARN;
		}
		return DashboardUiKit.BAD;
	}

	// ============================================================
	// DASBOR VISUAL (HTML/CSS, tanpa JFreeChart)
	// ============================================================

	/**
	 * Bangun HTML dasbor: kartu ringkasan + grafik. Otomatis menyesuaikan: bila hanya satu peserta
	 * tampilkan jaring laba-laba (spider) nilai per tugas; bila banyak peserta tampilkan rata-rata
	 * per tugas, tingkat pengumpulan per tugas, dan sebaran nilai mahasiswa.
	 */
	public static String dashboardHtml(String judulAtas, List<String> judul, List<Peserta> peserta) {
		StringBuilder sb = new StringBuilder();
		int jumlahPeserta = peserta == null ? 0 : peserta.size();
		int jumlahTugas = judul == null ? 0 : judul.size();

		sb.append(DashboardUiKit.descChip(
				"Rangkuman nilai tugas dalam bentuk grafik agar mudah dibaca. Untuk data mentah lengkap, gunakan tombol Download Data di atas."));

		if (jumlahPeserta == 0 || jumlahTugas == 0) {
			sb.append("<div style='font-size:12px;color:#64748b;padding:14px;'>Belum ada data tugas untuk ditampilkan.</div>");
			return sb.toString();
		}

		// Hitung agregat per tugas & rata-rata kelas.
		double[] sumNilai = new double[jumlahTugas];
		int[] cntNilai = new int[jumlahTugas];
		int[] cntIkut = new int[jumlahTugas];
		int[] cntSubmit = new int[jumlahTugas];
		double totalRataKelas = 0;
		int cntRataKelas = 0;
		double rataTertinggi = -1;
		String namaTertinggi = "";
		for (Peserta p : peserta) {
			if (p.jumlahIkut > 0) {
				totalRataKelas += p.rataRata;
				cntRataKelas++;
				if (p.rataRata > rataTertinggi) {
					rataTertinggi = p.rataRata;
					namaTertinggi = p.nama;
				}
			}
			for (int j = 0; j < jumlahTugas && j < p.cells.size(); j++) {
				Cell c = p.cells.get(j);
				if (c.ikut) {
					cntIkut[j]++;
					if (c.submit) {
						cntSubmit[j]++;
					}
					if (c.nilai != null) {
						sumNilai[j] += c.nilai.doubleValue();
						cntNilai[j]++;
					}
				}
			}
		}
		double rataKelas = cntRataKelas <= 0 ? 0 : totalRataKelas / cntRataKelas;

		// Kartu ringkasan.
		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Jumlah Peserta", Common.numberFormat.get().format(jumlahPeserta),
				"orang dalam rekap", DashboardUiKit.PRIMARY));
		kartu.add(new DashboardUiKit.Stat("Jumlah Tugas", String.valueOf(jumlahTugas), "tugas dinilai",
				DashboardUiKit.ACCENT));
		kartu.add(new DashboardUiKit.Stat("Rata-Rata Kelas", Common.numberFormat.get().format(rataKelas),
				"nilai rata-rata semua peserta", warnaNilai(rataKelas)));
		if (jumlahPeserta > 1 && rataTertinggi >= 0) {
			kartu.add(new DashboardUiKit.Stat("Nilai Tertinggi", Common.numberFormat.get().format(rataTertinggi),
					DashboardUiKit.shorten(namaTertinggi, 18), DashboardUiKit.GOOD));
		}
		sb.append(DashboardUiKit.cards(kartu));

		if (jumlahPeserta == 1) {
			// Satu peserta: jaring laba-laba nilai per tugas (bila tugas >= 3), atau batang.
			Peserta p = peserta.get(0);
			if (jumlahTugas >= 3) {
				String[] labels = new String[jumlahTugas];
				int[] vals = new int[jumlahTugas];
				for (int j = 0; j < jumlahTugas; j++) {
					labels[j] = judul.get(j);
					Cell c = j < p.cells.size() ? p.cells.get(j) : null;
					vals[j] = c == null || c.nilai == null ? 0 : (int) Math.round(c.nilai.doubleValue());
				}
				sb.append(DashboardUiKit.spider("Sebaran Nilai per Tugas",
						"Semakin lebar jaringnya, semakin merata nilai yang bagus di tiap tugas.", labels, vals));
			} else {
				LinkedHashMap<String, Double> perTugas = new LinkedHashMap<String, Double>();
				for (int j = 0; j < jumlahTugas; j++) {
					Cell c = j < p.cells.size() ? p.cells.get(j) : null;
					perTugas.put(judul.get(j), c == null || c.nilai == null ? 0.0 : c.nilai.doubleValue());
				}
				sb.append(DashboardUiKit.barList("Nilai per Tugas",
						"Nilai yang diperoleh pada masing-masing tugas.", perTugas, DashboardUiKit.PRIMARY, "", false,
						"Belum ada nilai."));
			}
			return sb.toString();
		}

		// Banyak peserta: rata-rata per tugas + tingkat pengumpulan + sebaran nilai.
		sb.append(DashboardUiKit.openGrid(300));

		LinkedHashMap<String, Double> rataPerTugas = new LinkedHashMap<String, Double>();
		for (int j = 0; j < jumlahTugas; j++) {
			rataPerTugas.put(judul.get(j), cntNilai[j] <= 0 ? 0.0 : sumNilai[j] / cntNilai[j]);
		}
		sb.append(DashboardUiKit.barList("Rata-Rata Nilai per Tugas",
				"Tugas mana yang nilainya cenderung tinggi atau rendah di kelas ini.", rataPerTugas,
				DashboardUiKit.PRIMARY, "", false, "Belum ada nilai."));

		LinkedHashMap<String, Double> kumpulPerTugas = new LinkedHashMap<String, Double>();
		for (int j = 0; j < jumlahTugas; j++) {
			double pct = cntIkut[j] <= 0 ? 0.0 : (cntSubmit[j] * 100.0 / cntIkut[j]);
			kumpulPerTugas.put(judul.get(j), pct);
		}
		sb.append(DashboardUiKit.barList("Tingkat Pengumpulan per Tugas (%)",
				"Berapa persen peserta yang sudah mengumpulkan di tiap tugas.", kumpulPerTugas, DashboardUiKit.ACCENT,
				"%", false, "Belum ada data pengumpulan."));

		sb.append(DashboardUiKit.closeGrid());

		// Sebaran nilai rata-rata mahasiswa ke dalam rentang.
		LinkedHashMap<String, Double> sebaran = new LinkedHashMap<String, Double>();
		sebaran.put("0-59 (Kurang)", 0.0);
		sebaran.put("60-69 (Cukup)", 0.0);
		sebaran.put("70-79 (Baik)", 0.0);
		sebaran.put("80-100 (Sangat Baik)", 0.0);
		for (Peserta p : peserta) {
			if (p.jumlahIkut <= 0) {
				continue;
			}
			String key;
			if (p.rataRata < 60) {
				key = "0-59 (Kurang)";
			} else if (p.rataRata < 70) {
				key = "60-69 (Cukup)";
			} else if (p.rataRata < 80) {
				key = "70-79 (Baik)";
			} else {
				key = "80-100 (Sangat Baik)";
			}
			sebaran.put(key, sebaran.get(key) + 1.0);
		}
		sb.append(DashboardUiKit.barList("Sebaran Nilai Mahasiswa",
				"Berapa banyak mahasiswa yang masuk tiap kelompok nilai.", sebaran, DashboardUiKit.GOOD, "orang", false,
				"Belum ada nilai."));

		return sb.toString();
	}

	/** Bungkus HTML dasbor menjadi komponen ZK siap-tempel. */
	public static Html dashboard(String judulAtas, List<String> judul, List<Peserta> peserta) {
		return new Html(dashboardHtml(judulAtas, judul, peserta));
	}

	// ============================================================
	// EKSPOR EXCEL (format asli)
	// ============================================================

	/**
	 * Tulis rekap ke file Excel (.xlsx) dengan susunan kolom asli, lalu kembalikan file-nya untuk
	 * di-{@code Filedownload.save}. Dipanggil hanya saat pengguna menekan tombol unduh.
	 */
	public static File writeExcel(String judulAtas, List<String> judul, List<Peserta> peserta) throws Exception {
		List<Section> sections = new ArrayList<Section>();
		sections.add(new Section(judulAtas, judul, peserta));
		return writeExcelSections(sections);
	}

	/** Tulis beberapa blok rekap (mis. satu blok per pertemuan) ke satu file Excel. */
	public static File writeExcelSections(List<Section> sections) throws Exception {
		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/rekap_nilai_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
				+ ".xlsx");
		File file = new File(filename);
		file.createNewFile();

		XSSFWorkbook workbook = new XSSFWorkbook();
		{
			XSSFSheet sheet = workbook.createSheet("DATA");
			sheet.setDefaultColumnWidth(20);

			int rowIndex = 0;
			if (sections != null) {
				for (Section s : sections) {
					rowIndex = tulisBlok(sheet, rowIndex, s);
					rowIndex++; // baris kosong antar blok
				}
			}

			Common.setStyled(sheet);

			FileOutputStream fileOut = new FileOutputStream(filename);
			try {
				workbook.write(fileOut);
			} finally {
				fileOut.close();
			}
		}
		return file;
	}

	/** Tulis satu blok (judul + header + baris) mulai dari {@code startRow}; kembalikan indeks baris berikutnya. */
	private static int tulisBlok(XSSFSheet sheet, int startRow, Section s) {
		int rowIndex = startRow;
		sheet.createRow(rowIndex).createCell(0).setCellValue(s.judulAtas == null ? "Rekap Nilai" : s.judulAtas);
		rowIndex++;

		XSSFRow head = sheet.createRow(rowIndex);
		head.createCell(0).setCellValue("No.");
		head.createCell(1).setCellValue("NIM/No. Reg");
		head.createCell(2).setCellValue("Nama");
		head.createCell(3).setCellValue("Prodi");
		head.createCell(4).setCellValue("Kelas");
		int colHead = 5;
		if (s.judul != null) {
			for (String j : s.judul) {
				head.createCell(colHead).setCellValue(j);
				colHead++;
			}
		}
		head.createCell(colHead).setCellValue("Total");
		head.createCell(colHead + 1).setCellValue("Ikut (x)");
		head.createCell(colHead + 2).setCellValue("Rata-Rata");
		rowIndex++;

		int no = 1;
		if (s.peserta != null) {
			for (Peserta p : s.peserta) {
				XSSFRow row = sheet.createRow(rowIndex);
				row.createCell(0).setCellValue(no);
				row.createCell(1).setCellValue(p.nim);
				row.createCell(2).setCellValue(p.nama);
				row.createCell(3).setCellValue(p.prodi);
				row.createCell(4).setCellValue(p.kelas);
				int col = 5;
				for (Cell c : p.cells) {
					row.createCell(col).setCellValue(c.display);
					col++;
				}
				row.createCell(col).setCellValue(p.total);
				row.createCell(col + 1).setCellValue(p.jumlahIkut);
				row.createCell(col + 2).setCellValue(Common.numberFormat.get().format(p.rataRata));
				rowIndex++;
				no++;
			}
		}
		return rowIndex;
	}

	// ============================================================
	// TAMPILAN BANYAK BLOK (mis. per pertemuan)
	// ============================================================

	/** Satu blok rekap: judul + daftar kolom nilai + daftar peserta. */
	public static final class Section {
		public final String judulAtas;
		public final List<String> judul;
		public final List<Peserta> peserta;

		public Section(String judulAtas, List<String> judul, List<Peserta> peserta) {
			this.judulAtas = judulAtas;
			this.judul = judul;
			this.peserta = peserta;
		}
	}

	/**
	 * Tempelkan beberapa blok rekap ke {@code host} (mis. satu blok per pertemuan): tiap blok berisi
	 * judul, grafik ringkas, lalu tabel rinci. Aman dipanggil untuk daftar kosong.
	 */
	public static void renderSections(org.zkoss.zk.ui.Component host, List<Section> sections) {
		if (host == null) {
			return;
		}
		if (sections == null || sections.isEmpty()) {
			host.appendChild(DashboardUiKit.html(
					"<div style='font-size:12px;color:#64748b;padding:14px;'>Belum ada data untuk ditampilkan.</div>"));
			return;
		}
		for (Section s : sections) {
			if (s.judulAtas != null && s.judulAtas.trim().length() > 0) {
				host.appendChild(DashboardUiKit.html(
						"<div style='font-size:13px;font-weight:800;color:#0f172a;margin:16px 0 8px;border-left:4px solid "
								+ DashboardUiKit.PRIMARY + ";padding-left:8px;'>" + DashboardUiKit.esc(s.judulAtas)
								+ "</div>"));
			}
			host.appendChild(dashboard(s.judulAtas, s.judul, s.peserta));
			host.appendChild(DashboardUiKit.html(
					"<div style='font-size:11px;color:#64748b;margin:8px 0 6px;'>Daftar lengkap nilai tiap peserta. Geser ke samping bila kolom banyak.</div>"));
			Grid grid = tableGrid(s.judul, s.peserta);
			grid.setParent(host);
		}
	}
}
