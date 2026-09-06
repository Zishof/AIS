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
 * {@code DaftarUlangMahasiswa*Action}. Untuk {@link ItemBiaya} ber-{@code penghitungan} basis
 * SKS/matakuliah (mis. konstanta {@code DIKALI_JUMLAH_SKS_*}/{@code DIKALI_JUMLAH_MK_*} di
 * {@link ItemBiaya}, semuanya mengandung kata "SKS", "matakuliah"/"Matakuliah", atau "MK"),
 * tambahkan ikon mata kecil; saat diklik menampilkan popup grid perkuliahan ({@link Detailperkuliahan})
 * yang menjadi dasar perhitungan pada semester tsb, lengkap dengan tombol download <b>PDF</b>
 * (iText, dibentuk sendiri lewat {@link #unduhPdf}) dan <b>Excel</b> (dibentuk sendiri lewat
 * {@link #unduhExcel} memakai {@link org.zkoss.zss.ui.Spreadsheet}, bukan reuse method lain).</p>
 *
 * <p><b>Peran murni tampilan, bukan mesin billing:</b> kelas ini hanya membaca dan menjumlahkan SKS
 * dari {@link Detailperkuliahan}/{@link Matakuliah} milik mahasiswa untuk ditampilkan sebagai bukti
 * pendukung ("kenapa nominal tagihan sebesar ini"); ia tidak menghitung ulang nominal tagihan itu
 * sendiri dan tidak terhubung langsung ke {@code Kegiatan}/{@code DetailKegiatan}/{@code DetailBiaya}
 * &mdash; kalkulasi nominal aktual berdasarkan {@code penghitungan} dilakukan di tempat lain
 * (mis. {@code PembayaranNominalModifikasiHelper}, lihat dokumentasi {@link ItemBiaya#getPenghitungan()}).
 * Karena hanya menjumlahkan SKS untuk tampilan, kelas ini tidak mewarisi risiko bug kalkulasi
 * finansial (percabangan/pembulatan) yang pernah ditemukan pada mesin billing {@code Kegiatan}.</p>
 */
public class RincianPerkuliahanTagihanHelper {

	/**
	 * Menentukan apakah sebuah {@link ItemBiaya} nominalnya dihitung berdasarkan SKS/matakuliah
	 * yang diambil, sehingga layak diberi ikon "rincian perkuliahan". Deteksi dilakukan dengan
	 * mencocokkan substring pada {@link ItemBiaya#getPenghitungan()} terhadap kata "SKS",
	 * "matakuliah", "Matakuliah", atau "MK" &mdash; cocok dengan pola penamaan konstanta
	 * {@code DIKALI_JUMLAH_SKS_*}/{@code DIKALI_JUMLAH_MK_*} di {@link ItemBiaya}.
	 *
	 * @param itemBiaya item biaya yang diperiksa; boleh {@code null}.
	 * @return {@code true} bila {@code itemBiaya} tidak {@code null}, mode penghitungannya bukan
	 *         {@link ItemBiaya#TIDAK_ADA_PENGHITUNGAN}, dan mengandung salah satu kata kunci di
	 *         atas.
	 */
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

	/**
	 * Menambahkan label ikon "👁 rincian" yang dapat diklik ke {@code vbox}, hanya bila
	 * {@code itemBiaya} lolos {@link #adaRincianPerkuliahan(ItemBiaya)}. Klik membuka popup
	 * rincian perkuliahan lewat {@link #bukaPopup}; kegagalan saat membuka popup ditangkap dan
	 * dicatat lewat {@link Common#tampilErrorJikaAdmin(Exception)} tanpa menjalar ke ZK.
	 *
	 * @param vbox kontainer baris tagihan tujuan; bila {@code null} method tidak melakukan apa-apa.
	 * @param mahasiswa mahasiswa pemilik tagihan; bila {@code null} ikon tidak ditambahkan.
	 * @param semester semester konteks perhitungan (boleh {@code null}, diteruskan apa adanya).
	 * @param itemBiaya item biaya baris tagihan yang diberi ikon.
	 */
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

	/**
	 * Membangun dan menampilkan (modal) popup {@link MyWindow} berisi ringkasan total SKS, grid
	 * perkuliahan yang menjadi dasar perhitungan {@code itemBiaya}, dan tombol download PDF/Excel.
	 *
	 * @param ref komponen ZK acuan untuk menemukan root halaman ({@link Component#getPage()}) tempat
	 *        popup ditambahkan.
	 * @param mahasiswa mahasiswa pemilik data; dipakai untuk mengambil {@link Detailperkuliahan} dan
	 *        ditampilkan pada judul/ringkasan.
	 * @param semester semester konteks; boleh {@code null} (ditampilkan sebagai "-").
	 * @param itemBiaya item biaya sumber, hanya dipakai untuk judul popup dan tooltip; boleh
	 *        {@code null}.
	 * @throws Exception diteruskan apa adanya dari operasi ZK/PDF/Excel di dalamnya; ditangkap oleh
	 *         pemanggil di {@link #tambahIkonMata}.
	 */
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

	/**
	 * Mengambil daftar {@link Detailperkuliahan} milik {@code mahasiswa} pada {@code semester}
	 * lewat {@link Mahasiswa#ambilDetailperkuliahan(Integer, Object, Object, boolean, boolean, Object)}
	 * (parameter selain semester sengaja tidak dibatasi lebih lanjut: tahap, semester pendek, dan
	 * remedial semuanya diikutkan). Kegagalan pengambilan dicatat lewat
	 * {@link ais.common.ErrorAuditUtil#record} dan menghasilkan daftar kosong, bukan exception,
	 * agar popup tetap dapat ditampilkan (dengan pesan "belum ada perkuliahan").
	 *
	 * @param mahasiswa mahasiswa sumber data.
	 * @param semester semester konteks; boleh {@code null}.
	 * @return daftar {@link Detailperkuliahan} yang berhasil dimuat, tidak pernah {@code null}.
	 */
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

	/**
	 * Mengambil {@link Matakuliah} yang berasosiasi dengan sebuah {@link Detailperkuliahan}:
	 * lewat {@link Detailperkuliahan#getPerkuliahan()} bila ada, atau lewat
	 * {@link Detailperkuliahan#getMatakuliahKonversi()} sebagai fallback (mis. baris konversi
	 * nilai luar tanpa perkuliahan reguler).
	 *
	 * @param d baris {@link Detailperkuliahan} sumber; boleh {@code null}.
	 * @return {@link Matakuliah} terkait, atau {@code null} bila {@code d} {@code null} atau
	 *         keduanya tidak tersedia.
	 */
	private static Matakuliah ambilMatakuliah(Detailperkuliahan d) {
		if (d == null) {
			return null;
		}
		return d.getPerkuliahan() != null ? d.getPerkuliahan().getMatakuliah() : d.getMatakuliahKonversi();
	}

	/**
	 * Menjumlahkan SKS seluruh baris {@code rows}, mengambil SKS dari {@link Matakuliah} lewat
	 * {@link #ambilMatakuliah(Detailperkuliahan)}; baris tanpa matakuliah yang dikenali atau
	 * tanpa SKS tersimpan tidak menyumbang apa pun (dihitung sebagai 0, bukan dilempar error).
	 *
	 * @param rows daftar {@link Detailperkuliahan} yang dijumlahkan.
	 * @return total SKS, {@code 0} bila {@code rows} kosong.
	 */
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

	/**
	 * Membentuk dan langsung mengunduh berkas Excel (.xlsx) berisi tabel rincian perkuliahan
	 * ({@code rows}) dan baris total SKS, memakai template kosong {@code rowcolumn.xlsx} lewat
	 * {@link org.zkoss.zss.ui.Spreadsheet}/{@link ais.ui.util.MySpreadsheet} agar format konsisten
	 * dengan grid ZK dan versi PDF.
	 *
	 * @param rows daftar {@link Detailperkuliahan} yang ditulis ke sheet.
	 * @param mahasiswa mahasiswa pemilik data; dipakai sebagai bagian nama berkas (NIM).
	 * @param semester tidak dipakai langsung dalam pembentukan berkas ini (parameter dipertahankan
	 *        agar tanda tangan konsisten dengan {@link #unduhPdf}).
	 * @throws Exception diteruskan apa adanya dari operasi spreadsheet/{@link Filedownload}.
	 */
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

	/**
	 * Membentuk dan langsung mengunduh berkas PDF (iText) berisi judul, identitas mahasiswa dan
	 * semester, tabel rincian perkuliahan ({@code rows}), dan baris total SKS. Format tabel sama
	 * dengan grid ZK dan berkas Excel; SKS per baris dan total dihitung ulang secara independen
	 * di sini (bukan reuse {@link #totalSks(List)}) tetapi dengan logika fallback yang identik.
	 *
	 * @param rows daftar {@link Detailperkuliahan} yang ditulis ke tabel.
	 * @param mahasiswa mahasiswa pemilik data; ditampilkan di info dan nama berkas (NIM).
	 * @param semester semester konteks; ditampilkan di info, boleh {@code null} (tampil "-").
	 * @throws Exception diteruskan apa adanya dari operasi iText/{@link Filedownload}.
	 */
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

	/**
	 * Membentuk satu {@link PdfPCell} berisi teks dengan font dan perataan horizontal tertentu,
	 * dan padding tetap 3 satuan.
	 *
	 * @param s teks sel; {@code null} diperlakukan sebagai string kosong.
	 * @param f font teks.
	 * @param align konstanta perataan horizontal iText (mis. {@link Element#ALIGN_CENTER}).
	 * @return sel PDF siap ditambahkan ke tabel.
	 */
	private static PdfPCell sel(String s, Font f, int align) {
		PdfPCell c = new PdfPCell(new Paragraph(s == null ? "" : s, f));
		c.setHorizontalAlignment(align);
		c.setPadding(3);
		return c;
	}

	/**
	 * Menambahkan satu {@link Column} ZK ke {@code cols} dengan label dan lebar yang diberikan.
	 *
	 * @param cols kontainer kolom tujuan.
	 * @param label judul kolom.
	 * @param width lebar kolom (mis. {@code "18%"}).
	 */
	private static void kolom(Columns cols, String label, String width) {
		Column c = new Column();
		c.setLabel(label);
		c.setWidth(width);
		c.setParent(cols);
	}
}
