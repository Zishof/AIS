package ais.action.master.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * <h3>RincianPerkuliahanTagihanHelper — ikon "mata" pada baris tagihan yang nominalnya DIHITUNG
 * berdasarkan SKS/matakuliah yang diambil.</h3>
 *
 * <p>Dipakai di renderer pembayaran ({@code DetailPembayaranMahasiswaRenderer}) yang melayani layar
 * {@code DaftarUlangMahasiswa*Action}. Untuk ItemBiaya ber-{@code penghitungan} basis SKS/MK (mis.
 * "PENDAPATAN SIMULTAN (200.000) x N SKS"), tambahkan ikon mata kecil; saat diklik menampilkan popup
 * grid perkuliahan (matakuliah) yang menjadi dasar perhitungan pada semester tsb, lengkap dengan
 * tombol download <b>PDF</b> (iText) dan <b>Excel</b> (reuse {@link PenilaianUtil#downloadSemuaKRS}).</p>
 */
public class RincianPerkuliahanTagihanHelper {

	/** Item biaya yang nominalnya dihitung dari SKS/matakuliah yang diambil → punya rincian perkuliahan. */
	public static boolean adaRincianPerkuliahan(ItemBiaya itemBiaya) {
		if (itemBiaya == null) {
			return false;
		}
		String p = itemBiaya.getPenghitungan();
		if (p == null || ItemBiaya.TIDAK_ADA_PENGHITUNGAN.equals(p)) {
			return false;
		}
		return p.contains("SKS") || p.contains("matakuliah") || p.contains("Matakuliah") || p.contains("MK");
	}

	/** Tambahkan ikon mata (kecil) ke {@code vbox} baris tagihan bila item-nya berbasis perhitungan SKS/MK. */
	public static void tambahIkonMata(Vbox vbox, final Mahasiswa mahasiswa, final Integer semester,
			final ItemBiaya itemBiaya) {
		if (vbox == null || mahasiswa == null || !adaRincianPerkuliahan(itemBiaya)) {
			return;
		}
		final Label ikon = new Label("👁 rincian");
		ikon.setStyle("cursor:pointer; color:#2563eb; font-size:11px; text-decoration:underline; margin-top:2px;");
		ikon.setTooltiptext("Lihat rincian perkuliahan yang menjadi syarat perhitungan "
				+ (itemBiaya.getNama() == null ? "" : itemBiaya.getNama()));
		ikon.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				try {
					bukaPopup(ikon, mahasiswa, semester, itemBiaya);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		});
		ikon.setParent(vbox);
	}

	// ================= popup =================

	private static void bukaPopup(Component ref, final Mahasiswa mahasiswa, final Integer semester, ItemBiaya itemBiaya)
			throws Exception {
		final List<Detailperkuliahan> rows = ambilPerkuliahan(mahasiswa, semester);
		int totalSks = totalSks(rows);

		MyWindow w = new MyWindow();
		ref.getPage().getFirstRoot().appendChild(w);
		w.setTitle("Rincian Perkuliahan (dasar perhitungan " + (itemBiaya == null ? "" : itemBiaya.getNama()) + ")");
		w.setWidth("680px");
		w.setBorder("normal");
		w.setClosable(true);
		w.setSizable(true);
		w.setMaximizable(true);

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;");
		isi.setParent(w);

		new Label("Mahasiswa: " + mahasiswa + "   |   Semester: " + (semester == null ? "-" : semester)
				+ "   |   Total: " + totalSks + " SKS  (" + rows.size() + " matakuliah)").setParent(isi);

		// tombol download
		Hbox tools = new Hbox();
		tools.setStyle("margin:6px 0;");
		tools.setParent(isi);
		MyToolbarbuttonConfig btnPdf = new MyToolbarbuttonConfig("Download PDF", "/img/pdf.png");
		btnPdf.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				unduhPdf(rows, mahasiswa, semester);
			}
		});
		btnPdf.setParent(tools);
		MyToolbarbuttonConfig btnXls = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
		btnXls.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				unduhExcel(rows, mahasiswa, semester);
			}
		});
		btnXls.setParent(tools);

		// grid ringkas
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(isi);
		Columns cols = new Columns();
		cols.setSizable(true);
		cols.setParent(grid);
		kolom(cols, "No", "8%");
		kolom(cols, "Kode MK", "18%");
		kolom(cols, "Nama Matakuliah", "44%");
		kolom(cols, "SKS", "10%");
		kolom(cols, "Semester", "10%");
		kolom(cols, "T.A.", "10%");

		Rows rs = new Rows();
		rs.setParent(grid);
		int no = 1;
		for (Detailperkuliahan d : rows) {
			Matakuliah mk = ambilMatakuliah(d);
			Row r = new Row();
			r.setParent(rs);
			new Label("" + (no++)).setParent(r);
			new Label(mk == null || mk.getKode() == null ? "-" : mk.getKode()).setParent(r);
			new Label(mk == null || mk.getNama() == null ? "-" : mk.getNama()).setParent(r);
			new Label(mk == null || mk.getSks() == null ? "-" : "" + mk.getSks()).setParent(r);
			new Label(d.getSemester() == null ? "-" : "" + d.getSemester()).setParent(r);
			new Label(d.getTahunAkademik() == null ? "-" : d.getTahunAkademik()).setParent(r);
		}
		if (rows.isEmpty()) {
			Row r = new Row();
			r.setParent(rs);
			Label kosong = new Label("Belum ada perkuliahan/KRS untuk semester ini (jumlah SKS = 0).");
			kosong.setStyle("color:#9ca3af; font-style:italic;");
			kosong.setParent(r);
			new Label("").setParent(r);
			new Label("").setParent(r);
			new Label("").setParent(r);
			new Label("").setParent(r);
			new Label("").setParent(r);
		}

		// footer total
		Row rTot = new Row();
		rTot.setStyle("background:#f0fdf4; font-weight:bold;");
		rTot.setParent(rs);
		new Label("").setParent(rTot);
		new Label("").setParent(rTot);
		new Label("TOTAL SKS").setParent(rTot);
		new Label("" + totalSks).setParent(rTot);
		new Label("").setParent(rTot);
		new Label("").setParent(rTot);

		w.doModal();
	}

	// ================= data =================

	@SuppressWarnings("unchecked")
	private static List<Detailperkuliahan> ambilPerkuliahan(Mahasiswa mahasiswa, Integer semester) {
		List<Detailperkuliahan> hasil = new ArrayList<Detailperkuliahan>();
		try {
			Collection<Long> ids = mahasiswa.ambilDetailperkuliahan(semester, null, null, false, false, null);
			if (ids == null) {
				return hasil;
			}
			for (Long id : ids) {
				if (id == null) {
					continue;
				}
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						id.toString());
				if (d != null) {
					hasil.add(d);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit RincianPerkuliahanTagihanHelper.ambilPerkuliahan");
		}
		return hasil;
	}

	private static Matakuliah ambilMatakuliah(Detailperkuliahan d) {
		if (d == null) {
			return null;
		}
		return d.getPerkuliahan() != null ? d.getPerkuliahan().getMatakuliah() : d.getMatakuliahKonversi();
	}

	private static int totalSks(List<Detailperkuliahan> rows) {
		int t = 0;
		for (Detailperkuliahan d : rows) {
			Matakuliah mk = ambilMatakuliah(d);
			if (mk != null && mk.getSks() != null) {
				t += mk.getSks();
			}
		}
		return t;
	}

	// ================= Excel (xlsx, download langsung — konsisten dgn grid & PDF) =================

	private static void unduhExcel(List<Detailperkuliahan> rows, Mahasiswa mahasiswa, Integer semester)
			throws Exception {
		org.zkoss.zss.ui.Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(6);
		spreadsheet.setMaxrows(rows.size() + 3);
		org.zkoss.zss.model.Worksheet sheet = spreadsheet.getSelectedSheet();

		int r = 0;
		EcampusUtil.setCellValue(sheet, r, 0, "No");
		EcampusUtil.setCellValue(sheet, r, 1, "Kode MK");
		EcampusUtil.setCellValue(sheet, r, 2, "Nama Matakuliah");
		EcampusUtil.setCellValue(sheet, r, 3, "SKS");
		EcampusUtil.setCellValue(sheet, r, 4, "Semester");
		EcampusUtil.setCellValue(sheet, r, 5, "T.A.");

		r = 1;
		int no = 1;
		int total = 0;
		for (Detailperkuliahan d : rows) {
			Matakuliah mk = ambilMatakuliah(d);
			int sks = mk == null || mk.getSks() == null ? 0 : mk.getSks();
			total += sks;
			EcampusUtil.setCellValue(sheet, r, 0, Integer.valueOf(no++));
			EcampusUtil.setCellValue(sheet, r, 1, mk == null || mk.getKode() == null ? "-" : mk.getKode());
			EcampusUtil.setCellValue(sheet, r, 2, mk == null || mk.getNama() == null ? "-" : mk.getNama());
			EcampusUtil.setCellValue(sheet, r, 3, Integer.valueOf(sks));
			EcampusUtil.setCellValue(sheet, r, 4, d.getSemester() == null ? "-" : "" + d.getSemester());
			EcampusUtil.setCellValue(sheet, r, 5, d.getTahunAkademik() == null ? "-" : d.getTahunAkademik());
			r++;
		}
		EcampusUtil.setCellValue(sheet, r, 2, "TOTAL SKS");
		EcampusUtil.setCellValue(sheet, r, 3, Integer.valueOf(total));

		java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"rincian_perkuliahan_" + (mahasiswa == null ? "" : mahasiswa.getNim()) + ".xlsx");
	}

	// ================= PDF (iText) =================

	private static void unduhPdf(List<Detailperkuliahan> rows, Mahasiswa mahasiswa, Integer semester) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Document doc = new Document(PageSize.A4, 28, 28, 30, 30);
		PdfWriter.getInstance(doc, bout);
		doc.open();

		Font fJudul = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
		Font fN = FontFactory.getFont(FontFactory.HELVETICA, 9);
		Font fNb = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

		Paragraph judul = new Paragraph("RINCIAN PERKULIAHAN (DASAR PERHITUNGAN TAGIHAN)", fJudul);
		judul.setAlignment(Element.ALIGN_CENTER);
		doc.add(judul);
		Paragraph info = new Paragraph("Mahasiswa: " + (mahasiswa == null ? "-" : mahasiswa) + "    Semester: "
				+ (semester == null ? "-" : semester), fN);
		info.setSpacingAfter(8);
		doc.add(info);

		PdfPTable t = new PdfPTable(new float[] { 8, 18, 46, 10, 10, 12 });
		t.setWidthPercentage(100);
		for (String h : new String[] { "No", "Kode MK", "Nama Matakuliah", "SKS", "Smt", "T.A." }) {
			PdfPCell c = new PdfPCell(new Paragraph(h, fNb));
			c.setBackgroundColor(new BaseColor(224, 231, 255));
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			c.setPadding(3);
			t.addCell(c);
		}
		int no = 1, total = 0;
		for (Detailperkuliahan d : rows) {
			Matakuliah mk = ambilMatakuliah(d);
			int sks = mk == null || mk.getSks() == null ? 0 : mk.getSks();
			total += sks;
			t.addCell(sel("" + (no++), fN, Element.ALIGN_CENTER));
			t.addCell(sel(mk == null || mk.getKode() == null ? "-" : mk.getKode(), fN, Element.ALIGN_LEFT));
			t.addCell(sel(mk == null || mk.getNama() == null ? "-" : mk.getNama(), fN, Element.ALIGN_LEFT));
			t.addCell(sel("" + sks, fN, Element.ALIGN_CENTER));
			t.addCell(sel(d.getSemester() == null ? "-" : "" + d.getSemester(), fN, Element.ALIGN_CENTER));
			t.addCell(sel(d.getTahunAkademik() == null ? "-" : d.getTahunAkademik(), fN, Element.ALIGN_CENTER));
		}
		PdfPCell cTot = new PdfPCell(new Paragraph("TOTAL SKS", fNb));
		cTot.setColspan(3);
		cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cTot.setPadding(3);
		t.addCell(cTot);
		t.addCell(sel("" + total, fNb, Element.ALIGN_CENTER));
		t.addCell(sel("", fN, Element.ALIGN_CENTER));
		t.addCell(sel("", fN, Element.ALIGN_CENTER));
		doc.add(t);
		doc.close();

		Filedownload.save(bout.toByteArray(), "application/pdf",
				"rincian_perkuliahan_" + (mahasiswa == null ? "" : mahasiswa.getNim()) + ".pdf");
	}

	private static PdfPCell sel(String s, Font f, int align) {
		PdfPCell c = new PdfPCell(new Paragraph(s == null ? "" : s, f));
		c.setHorizontalAlignment(align);
		c.setPadding(3);
		return c;
	}

	private static void kolom(Columns cols, String label, String width) {
		Column c = new Column();
		c.setLabel(label);
		c.setWidth(width);
		c.setParent(cols);
	}
}
