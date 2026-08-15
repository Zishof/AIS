package ais.action.master.helper;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/** Grid mahasiswa yang telah mengisi angket dosen pada satu perkuliahan. */
public final class AngketPerkuliahanHelper {

	private static final String MIME_XLSX =
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private AngketPerkuliahanHelper() {
	}

	public static void display(final Div panel, final Perkuliahan perkuliahan) {
		panel.setWidth("100%");
		panel.setStyle("min-height:260px;padding:8px;box-sizing:border-box;");

		Div header = new Div();
		header.setStyle("margin-bottom:8px;padding:10px;border:1px solid #dbe4ee;border-radius:6px;background:#f8fafc;");
		header.setParent(panel);
		Label title = new Label("Mahasiswa yang Telah Mengisi Penilaian Dosen");
		title.setStyle("font-weight:bold;font-size:14px;color:#1e3a5f;");
		title.setParent(header);
		Label info = new Label(ringkasanPerkuliahan(perkuliahan));
		info.setStyle("display:block;margin-top:4px;color:#64748b;");
		info.setParent(header);

		Hbox toolbar = new Hbox();
		toolbar.setSpacing("8px");
		toolbar.setParent(panel);
		final Div isi = new Div();
		isi.setStyle("margin-top:8px;");
		isi.setParent(panel);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		refresh.setTooltiptext("Muat ulang data angket perkuliahan");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				muatKonten(isi, perkuliahan);
			}
		});
		refresh.setParent(toolbar);

		MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
		excel.setTooltiptext("Unduh daftar mahasiswa yang telah mengisi angket");
		excel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				downloadExcel(perkuliahan);
			}
		});
		excel.setParent(toolbar);

		muatKonten(isi, perkuliahan);
	}

	private static void muatKonten(Div parent, Perkuliahan perkuliahan) {
		parent.getChildren().clear();
		List<Object[]> data = ambilData(perkuliahan == null ? null : perkuliahan.getId());
		Html dashboard = new Html();
		dashboard.setContent(buatDashboard(perkuliahan, data));
		dashboard.setParent(parent);

		Label jumlah = new Label("Daftar mahasiswa yang mengisi angket (" + data.size() + ")");
		jumlah.setStyle("display:block;margin:14px 0 6px;font-weight:bold;font-size:13px;color:#334155;");
		jumlah.setParent(parent);

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(parent);
		Columns columns = new Columns();
		columns.setParent(grid);
		tambahKolom(columns, "No.", "55px");
		tambahKolom(columns, "NIM", "150px");
		tambahKolom(columns, "Mahasiswa", null);
		tambahKolom(columns, "Waktu Mulai", "175px");
		tambahKolom(columns, "Waktu Selesai", "175px");
		Rows rows = new Rows();
		rows.setParent(grid);

		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		int nomor = 1;
		for (Object[] nilai : data) {
			Row row = new Row();
			row.setParent(rows);
			new Label(String.valueOf(nomor++)).setParent(row);
			new Label(teks(nilai, 1)).setParent(row);
			new Label(teks(nilai, 2)).setParent(row);
			new Label(tanggal(nilai, 3, format)).setParent(row);
			new Label(tanggal(nilai, 4, format)).setParent(row);
		}

		if (data.isEmpty()) {
			Label kosong = new Label("Belum ada mahasiswa yang mengisi angket untuk perkuliahan ini.");
			kosong.setStyle("display:block;padding:18px;color:#64748b;text-align:center;");
			kosong.setParent(parent);
		}
	}

	private static String buatDashboard(Perkuliahan perkuliahan, List<Object[]> data) {
		int peserta = ambilJumlahPeserta(perkuliahan);
		int pengisi = data.size();
		long jumlahJawaban = 0;
		long totalDurasi = 0;
		int durasiTerhitung = 0;
		int selesaiCepat = 0;
		Map<String, Integer> trenTanggal = new LinkedHashMap<String, Integer>();
		Map<Integer, Integer> jamMulai = new LinkedHashMap<Integer, Integer>();
		SimpleDateFormat tanggal = new SimpleDateFormat("dd MMM");
		Calendar kalender = Calendar.getInstance();

		for (Object[] nilai : data) {
			jumlahJawaban += angka(nilai, 5);
			Date mulai = nilaiTanggal(nilai, 3);
			Date selesai = nilaiTanggal(nilai, 4);
			if (mulai != null) {
				String kunci = tanggal.format(mulai);
				trenTanggal.put(kunci, Integer.valueOf(nilaiPeta(trenTanggal, kunci) + 1));
				kalender.setTime(mulai);
				Integer jam = Integer.valueOf(kalender.get(Calendar.HOUR_OF_DAY));
				jamMulai.put(jam, Integer.valueOf(nilaiPeta(jamMulai, jam) + 1));
			}
			if (mulai != null && selesai != null && !selesai.before(mulai)) {
				long menit = (selesai.getTime() - mulai.getTime()) / 60000L;
				totalDurasi += menit;
				durasiTerhitung++;
				if (menit <= 30L) selesaiCepat++;
			}
		}

		double cakupan = peserta > 0 ? pengisi * 100.0 / peserta : 0.0;
		long rataDurasi = durasiTerhitung == 0 ? 0L : totalDurasi / durasiTerhitung;
		double kecepatan = durasiTerhitung == 0 ? 0.0 : Math.max(0.0, 100.0 - Math.min(100.0, rataDurasi * 100.0 / 120.0));
		double konsistensi = durasiTerhitung == 0 ? 0.0 : selesaiCepat * 100.0 / durasiTerhitung;
		double sebaran = Math.min(100.0, trenTanggal.size() * 18.0 + jamMulai.size() * 7.0);
		long maksimumJawaban = 0L;
		for (Object[] nilai : data) maksimumJawaban = Math.max(maksimumJawaban, angka(nilai, 5));
		double kelengkapan = pengisi == 0 || maksimumJawaban == 0L ? 0.0
				: jumlahJawaban * 100.0 / (pengisi * maksimumJawaban);

		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;color:#334155'>");
		html.append("<div style='display:grid;grid-template-columns:repeat(5,minmax(125px,1fr));gap:10px'>");
		kartu(html, "Peserta Kelas", formatAngka(peserta), "Peserta perkuliahan");
		kartu(html, "Sudah Mengisi", formatAngka(pengisi), "Mahasiswa unik");
		kartu(html, "Cakupan", formatPersen(cakupan), "Dari peserta kelas");
		kartu(html, "Total Jawaban", formatAngka(jumlahJawaban), "Butir jawaban tersimpan");
		kartu(html, "Rata-rata Durasi", rataDurasi + " menit", "Mulai sampai selesai");
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-top:10px'>");
		panelBatang(html, "Tren Pengisian per Tanggal", trenTanggal, " mahasiswa");
		panelJam(html, jamMulai);
		html.append("<div style='border:1px solid #dbe4ee;border-radius:8px;padding:10px;background:#fff'>")
				.append("<div style='font-weight:bold;color:#1e3a5f'>Spider Web Pola Aktivitas</div>")
				.append(buatRadar(new String[] { "Cakupan", "Kecepatan", "Konsistensi", "Sebaran", "Kelengkapan" },
						new double[] { cakupan, kecepatan, konsistensi, sebaran, kelengkapan }))
				.append("<div style='font-size:10px;color:#64748b;text-align:center'>Indikator operasional pengisian, bukan nilai hasil angket.</div></div>");
		html.append("</div></div>");
		return html.toString();
	}

	private static void kartu(StringBuilder html, String judul, String nilai, String catatan) {
		html.append("<div style='border:1px solid #dbe4ee;border-radius:8px;padding:11px;background:#fff;box-shadow:0 1px 2px rgba(15,23,42,.06)'>")
				.append("<div style='font-size:11px;font-weight:bold;color:#64748b;text-transform:uppercase'>").append(judul).append("</div>")
				.append("<div style='font-size:22px;font-weight:bold;color:#0f766e;margin:4px 0'>").append(nilai).append("</div>")
				.append("<div style='font-size:10px;color:#94a3b8'>").append(catatan).append("</div></div>");
	}

	private static void panelBatang(StringBuilder html, String judul, Map<String, Integer> data, String satuan) {
		html.append("<div style='border:1px solid #dbe4ee;border-radius:8px;padding:10px;background:#fff;min-height:220px'>")
				.append("<div style='font-weight:bold;color:#1e3a5f;margin-bottom:10px'>").append(judul).append("</div>");
		int maksimum = maksimum(data);
		if (data.isEmpty()) html.append("<div style='color:#94a3b8;padding:55px 0;text-align:center'>Belum ada data</div>");
		for (Map.Entry<String, Integer> entri : data.entrySet()) {
			int lebar = maksimum == 0 ? 0 : Math.max(4, entri.getValue().intValue() * 100 / maksimum);
			html.append("<div style='display:grid;grid-template-columns:58px 1fr 72px;gap:6px;align-items:center;margin:7px 0;font-size:11px'>")
					.append("<span>").append(entri.getKey()).append("</span><span style='height:8px;background:#e2e8f0;border-radius:8px;overflow:hidden'><i style='display:block;height:8px;width:")
					.append(lebar).append("%;background:#0ea5e9;border-radius:8px'></i></span><b>")
					.append(entri.getValue()).append(satuan).append("</b></div>");
		}
		html.append("</div>");
	}

	private static void panelJam(StringBuilder html, Map<Integer, Integer> data) {
		Map<String, Integer> label = new LinkedHashMap<String, Integer>();
		for (Map.Entry<Integer, Integer> entri : data.entrySet()) {
			String jam = (entri.getKey().intValue() < 10 ? "0" : "") + entri.getKey() + ":00";
			label.put(jam, entri.getValue());
		}
		panelBatang(html, "Distribusi Jam Mulai", label, " mahasiswa");
	}

	private static String buatRadar(String[] label, double[] nilai) {
		double cx = 150.0, cy = 100.0, radius = 62.0;
		StringBuilder svg = new StringBuilder("<svg viewBox='0 0 300 205' style='width:100%;height:190px'>");
		for (int ring = 1; ring <= 4; ring++) {
			double[] isi = new double[label.length];
			for (int i = 0; i < isi.length; i++) isi[i] = ring * 25.0;
			svg.append("<polygon points='").append(titikRadar(cx, cy, radius, isi)).append("' fill='none' stroke='#dbe4ee' stroke-width='1'/>");
		}
		for (int i = 0; i < label.length; i++) {
			double sudut = -Math.PI / 2.0 + 2.0 * Math.PI * i / label.length;
			double x = cx + radius * Math.cos(sudut), y = cy + radius * Math.sin(sudut);
			double lx = cx + (radius + 26.0) * Math.cos(sudut), ly = cy + (radius + 20.0) * Math.sin(sudut);
			svg.append("<line x1='").append(bulat(cx)).append("' y1='").append(bulat(cy)).append("' x2='").append(bulat(x)).append("' y2='").append(bulat(y)).append("' stroke='#dbe4ee'/>")
					.append("<text x='").append(bulat(lx)).append("' y='").append(bulat(ly)).append("' text-anchor='middle' font-size='10' fill='#475569'>").append(label[i]).append("</text>");
		}
		svg.append("<polygon points='").append(titikRadar(cx, cy, radius, nilai)).append("' fill='rgba(14,165,233,.22)' stroke='#0284c7' stroke-width='2'/>")
				.append("</svg>");
		return svg.toString();
	}

	private static String titikRadar(double cx, double cy, double radius, double[] nilai) {
		StringBuilder titik = new StringBuilder();
		for (int i = 0; i < nilai.length; i++) {
			double rasio = Math.max(0.0, Math.min(100.0, nilai[i])) / 100.0;
			double sudut = -Math.PI / 2.0 + 2.0 * Math.PI * i / nilai.length;
			if (i > 0) titik.append(' ');
			titik.append(bulat(cx + radius * rasio * Math.cos(sudut))).append(',')
					.append(bulat(cy + radius * rasio * Math.sin(sudut)));
		}
		return titik.toString();
	}

	private static String bulat(double nilai) {
		return String.valueOf(Math.round(nilai * 10.0) / 10.0);
	}

	private static int ambilJumlahPeserta(Perkuliahan perkuliahan) {
		if (perkuliahan == null || perkuliahan.getId() == null) return 0;
		try {
			Integer jumlah = perkuliahan.ambilJumlahDetailperkuliahanLangsung();
			return jumlah == null ? 0 : jumlah.intValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private static int maksimum(Map<?, Integer> data) {
		int maksimum = 0;
		for (Integer nilai : data.values()) if (nilai != null) maksimum = Math.max(maksimum, nilai.intValue());
		return maksimum;
	}

	private static <T> int nilaiPeta(Map<T, Integer> data, T kunci) {
		Integer nilai = data.get(kunci);
		return nilai == null ? 0 : nilai.intValue();
	}

	private static long angka(Object[] nilai, int index) {
		if (nilai == null || index >= nilai.length || !(nilai[index] instanceof Number)) return 0L;
		return ((Number) nilai[index]).longValue();
	}

	private static Date nilaiTanggal(Object[] nilai, int index) {
		return nilai != null && index < nilai.length && nilai[index] instanceof Date ? (Date) nilai[index] : null;
	}

	private static String formatAngka(long nilai) {
		return String.format("%,d", Long.valueOf(nilai)).replace(',', '.');
	}

	private static String formatPersen(double nilai) {
		return String.format("%.1f%%", Double.valueOf(nilai)).replace('.', ',');
	}

	private static void tambahKolom(Columns columns, String label, String width) {
		Column column = new Column(label);
		if (width != null) column.setWidth(width);
		column.setParent(columns);
	}

	@SuppressWarnings("unchecked")
	private static List<Object[]> ambilData(Long perkuliahanId) {
		if (perkuliahanId == null) return new ArrayList<Object[]>();
		Session session = HibernateUtil.currentSession();
		Query query = session.createSQLQuery(
				"select m.id, m.nim, m.nama, min(a.tanggal_dirubah) as waktu_mulai, "
						+ "max(a.tanggal_dirubah) as waktu_selesai, count(a.id) as jumlah_isian "
						+ "from checklist_baru_penilaian_dosen_oleh_mahasiswa a "
						+ "inner join mahasiswa m on m.id=a.mahasiswa "
						+ "where a.perkuliahan=:perkuliahan "
						+ "group by m.id, m.nim, m.nama order by min(a.tanggal_dirubah), m.nim");
		query.setLong("perkuliahan", perkuliahanId.longValue());
		return query.list();
	}

	private static void downloadExcel(Perkuliahan perkuliahan) {
		try {
			List<Object[]> data = ambilData(perkuliahan == null ? null : perkuliahan.getId());
			XSSFWorkbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Angket Perkuliahan");
			CellStyle headerStyle = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			headerStyle.setFont(font);

			org.zkoss.poi.ss.usermodel.Row title = sheet.createRow(0);
			title.createCell(0).setCellValue("MAHASISWA YANG TELAH MENGISI PENILAIAN DOSEN");
			org.zkoss.poi.ss.usermodel.Row meta = sheet.createRow(1);
			meta.createCell(0).setCellValue(ringkasanPerkuliahan(perkuliahan));
			org.zkoss.poi.ss.usermodel.Row head = sheet.createRow(3);
			String[] headers = { "No.", "NIM", "Mahasiswa", "Waktu Mulai", "Waktu Selesai" };
			for (int i = 0; i < headers.length; i++) {
				Cell cell = head.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			int index = 4;
			int nomor = 1;
			for (Object[] nilai : data) {
				org.zkoss.poi.ss.usermodel.Row row = sheet.createRow(index++);
				row.createCell(0).setCellValue(nomor++);
				row.createCell(1).setCellValue(teks(nilai, 1));
				row.createCell(2).setCellValue(teks(nilai, 2));
				row.createCell(3).setCellValue(tanggal(nilai, 3, format));
				row.createCell(4).setCellValue(tanggal(nilai, 4, format));
			}
			for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			workbook.write(output);
			String nama = "angket_perkuliahan_" + (perkuliahan == null ? "data" : perkuliahan.getId()) + ".xlsx";
			Filedownload.save(output.toByteArray(), MIME_XLSX, nama);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			try {
				MyMessageboxConfig.show("Data angket belum dapat diunduh. Silakan muat ulang lalu coba kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static String ringkasanPerkuliahan(Perkuliahan perkuliahan) {
		if (perkuliahan == null) return "";
		String kode = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode();
		String nama = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();
		return kode + " - " + nama + " | " + perkuliahan.getTahunAjaran() + " "
				+ Common.labelJenisSemester(perkuliahan) + " | Kelas "
				+ (perkuliahan.getKelas() == null ? "-" : perkuliahan.getKelas());
	}

	private static String teks(Object[] nilai, int index) {
		return nilai != null && index < nilai.length && nilai[index] != null ? nilai[index].toString() : "";
	}

	private static String tanggal(Object[] nilai, int index, SimpleDateFormat format) {
		if (nilai == null || index >= nilai.length || nilai[index] == null) return "";
		return nilai[index] instanceof Date ? format.format((Date) nilai[index]) : nilai[index].toString();
	}
}
