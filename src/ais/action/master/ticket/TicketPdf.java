package ais.action.master.ticket;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ticket.Ticket;
import ais.database.model.ticket.TicketKomentar;

/**
 * <h3>Cetak PDF per-tiket</h3>
 *
 * <p>Menghasilkan berkas PDF ringkas berisi identitas tiket, rincian, deskripsi, dan riwayat
 * diskusi (termasuk catatan internal — dokumen dicetak oleh pengelola). Memakai iText 5
 * ({@code com.itextpdf.text}) yang sudah tersedia di classpath. Java 1.7.</p>
 */
public final class TicketPdf {

	private TicketPdf() {
	}

	/** Bangun PDF tiket ke berkas sementara dan kembalikan {@link File}-nya. */
	public static File cetak(Ticket t) throws Exception {
		if (t == null) {
			return null;
		}
		File out = File.createTempFile("tiket_" + (t.getId() == null ? "x" : t.getId()) + "_", ".pdf");
		Document doc = new Document(PageSize.A4, 36, 36, 42, 42);
		FileOutputStream fos = new FileOutputStream(out);
		try {
			PdfWriter.getInstance(doc, fos);
			doc.open();

			Font fJudul = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
			Font fNomor = FontFactory.getFont(FontFactory.HELVETICA, 10);
			Font fLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
			Font fIsi = FontFactory.getFont(FontFactory.HELVETICA, 9);
			Font fH2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
			Font fKomHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);
			Font fKomIsi = FontFactory.getFont(FontFactory.HELVETICA, 9);

			Paragraph judul = new Paragraph("TIKET LAYANAN SISTEM", fJudul);
			judul.setAlignment(Element.ALIGN_CENTER);
			doc.add(judul);
			Paragraph nomor = new Paragraph(safe(t.getNomorTiket()), fNomor);
			nomor.setAlignment(Element.ALIGN_CENTER);
			nomor.setSpacingAfter(12);
			doc.add(nomor);

			PdfPTable tbl = new PdfPTable(new float[] { 1f, 2.4f });
			tbl.setWidthPercentage(100);
			baris(tbl, fLabel, fIsi, "Judul", safe(t.getJudul()));
			baris(tbl, fLabel, fIsi, "Tipe", safe(t.getTipe()));
			baris(tbl, fLabel, fIsi, "Prioritas", safe(t.getPrioritas()));
			baris(tbl, fLabel, fIsi, "Status", safe(t.getStatus()));
			baris(tbl, fLabel, fIsi, "Progress", (t.getProgress() == null ? 0 : t.getProgress()) + "%");
			baris(tbl, fLabel, fIsi, "Kategori",
					t.getTicketKategori() == null ? "-" : safe(t.getTicketKategori().getNama()));
			baris(tbl, fLabel, fIsi, "Modul", safe(t.getModul()));
			baris(tbl, fLabel, fIsi, "Satuan Kerja",
					t.getSatuanKerja() == null ? "-" : safe(t.getSatuanKerja().getNama()));
			baris(tbl, fLabel, fIsi, "Pengaju",
					safe(t.getPengajuNama()) + (t.getPengajuTipe() == null ? "" : " (" + t.getPengajuTipe() + ")"));
			baris(tbl, fLabel, fIsi, "Tanggal Dibuat", tgl(t.getTanggalDibuat()));
			baris(tbl, fLabel, fIsi, "Target Selesai", tgl(t.getTanggalTarget()));
			baris(tbl, fLabel, fIsi, "Tanggal Selesai", tgl(t.getTanggalSelesai()));
			doc.add(tbl);

			if (t.getDeskripsi() != null && t.getDeskripsi().trim().length() > 0) {
				Paragraph h = new Paragraph("Deskripsi", fH2);
				h.setSpacingBefore(12);
				h.setSpacingAfter(4);
				doc.add(h);
				doc.add(new Paragraph(t.getDeskripsi(), fIsi));
			}

			List<TicketKomentar> komentars = muatKomentar(t);
			if (komentars != null && !komentars.isEmpty()) {
				Paragraph h = new Paragraph("Riwayat Diskusi", fH2);
				h.setSpacingBefore(12);
				h.setSpacingAfter(4);
				doc.add(h);
				for (TicketKomentar k : komentars) {
					String tag = Boolean.TRUE.equals(k.getInternal()) ? "  [internal]" : "";
					Paragraph head = new Paragraph(
							safe(k.getNama()) + " (" + safe(k.getTipePengguna()) + ")  " + tgl(k.getTanggal()) + tag,
							fKomHead);
					head.setSpacingBefore(6);
					doc.add(head);
					doc.add(new Paragraph(safe(k.getIsi()), fKomIsi));
				}
			}

			Paragraph footer = new Paragraph("Dicetak: " + tgl(new Date()),
					FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
			footer.setSpacingBefore(16);
			doc.add(footer);

			doc.close();
		} finally {
			try {
				fos.close();
			} catch (Exception ignore) {
				ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) TicketPdf.cetak-close");
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static List<TicketKomentar> muatKomentar(Ticket t) {
		try {
			Session session = HibernateUtil.currentSession();
			return session.createCriteria(TicketKomentar.class).add(Restrictions.eq("ticket", t))
					.addOrder(Order.asc("id")).list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketPdf.muatKomentar");
			return null;
		}
	}

	private static void baris(PdfPTable tbl, Font fLabel, Font fIsi, String label, String isi) {
		PdfPCell c1 = new PdfPCell(new Phrase(label, fLabel));
		c1.setPadding(4);
		PdfPCell c2 = new PdfPCell(new Phrase(isi == null ? "-" : isi, fIsi));
		c2.setPadding(4);
		tbl.addCell(c1);
		tbl.addCell(c2);
	}

	private static String tgl(Date d) {
		try {
			return d == null ? "-" : Common.dateFormat5.get().format(d);
		} catch (Exception e) {
			return "-";
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}
