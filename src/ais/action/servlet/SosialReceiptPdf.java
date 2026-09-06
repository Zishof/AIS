package ais.action.servlet;

import java.io.IOException; import javax.servlet.http.*; import org.hibernate.Session; import org.hibernate.criterion.Restrictions;
 import com.itextpdf.text.*; import com.itextpdf.text.pdf.PdfWriter;
 import ais.action.master.sosial.helper.SocialHtml; import ais.common.Common; import ais.database.hibernate.HibernateUtil; import ais.database.model.sosial.*;

/**
 * Servlet publik untuk mengunduh PDF "Bukti Setor Sosial" (donasi) berdasarkan token
 * verifikasi acak, tanpa memerlukan sesi login.
 *
 * <p>Akses dikendalikan murni lewat parameter {@code token} (64 karakter heksadesimal,
 * dicocokkan dengan {@code BuktiSetorSosial.verificationToken}) -- bukan lewat ID basis
 * data yang mudah ditebak, sehingga aman diberikan lewat tautan publik. PDF yang dihasilkan
 * sengaja TIDAK memuat data kontak donatur (nama/telepon/email), hanya nomor bukti, tanggal,
 * jenis dana, nominal, institusi, dan status VALID/VOID -- lihat ringkasan kelas ini.</p>
 */
public final class SosialReceiptPdf extends HttpServlet {
 /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
 private static final long serialVersionUID=1L;

 /**
  * Menolak seluruh permintaan {@code POST}; endpoint ini hanya melayani {@code GET} dengan token.
  *
  * @param q permintaan HTTP masuk (tidak dipakai selain oleh kontrak servlet)
  * @param r tanggapan HTTP; selalu diisi status 405
  * @throws IOException bila penulisan status gagal
  */
 protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{r.sendError(405);}

 /**
  * Melayani {@code GET /SosialReceiptPdf?token=...}: memvalidasi format token, mencari
  * {@link BuktiSetorSosial} yang cocok, lalu merender PDF bukti setor langsung ke tanggapan.
  *
  * <p>Alur: (1) validasi token dengan regex {@code [a-f0-9]{64}}, balas 400 bila tidak cocok;
  * (2) cari baris {@code BuktiSetorSosial} dengan {@code verificationToken} sama, balas 404 bila
  * tidak ditemukan; (3) set header unduhan (nama berkas disanitasi dari {@code receiptNumber},
  * {@code Cache-Control: private, no-store}); (4) tulis dokumen PDF (iText) berisi nomor bukti,
  * tanggal transaksi, jenis dana, nominal, institusi (tenant), status, dan tautan verifikasi.
  * Galat tak terduga dicatat lewat {@link ais.common.ErrorAuditUtil} dan dibalas 500.</p>
  *
  * @param q permintaan HTTP; parameter {@code token} wajib berupa 64 hex digit
  * @param r tanggapan HTTP; diisi berkas PDF bila token valid, atau kode kesalahan bila tidak
  * @throws IOException bila penulisan tanggapan gagal
  */
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws IOException{String token=q.getParameter("token");if(token==null||!token.matches("[a-f0-9]{64}")){r.sendError(400);return;}Session s=null;try{s=HibernateUtil.openSession();BuktiSetorSosial receipt=(BuktiSetorSosial)s.createCriteria(BuktiSetorSosial.class).add(Restrictions.eq("verificationToken",token)).setMaxResults(1).uniqueResult();if(receipt==null){r.sendError(404);return;}TransaksiDonasi d=receipt.getTransaction();String fund=d.getFundType().getNama();r.setContentType("application/pdf");r.setHeader("Cache-Control","private, no-store");r.setHeader("Content-Disposition","attachment; filename=\"bukti-setor-"+receipt.getReceiptNumber().replaceAll("[^A-Za-z0-9._-]","_")+".pdf\"");com.itextpdf.text.Document doc=new com.itextpdf.text.Document(PageSize.A4,48,48,52,52);PdfWriter.getInstance(doc,r.getOutputStream());doc.open();Font title=FontFactory.getFont(FontFactory.HELVETICA_BOLD,20);Font strong=FontFactory.getFont(FontFactory.HELVETICA_BOLD,12);doc.add(new Paragraph("BUKTI SETOR SOSIAL AIS",title));doc.add(new Paragraph(" "));doc.add(new Paragraph("Nomor bukti: "+receipt.getReceiptNumber(),strong));doc.add(new Paragraph("Tanggal transaksi: "+String.valueOf(d.getPaidAt()==null?d.getCreatedAt():d.getPaidAt())));doc.add(new Paragraph("Jenis dana: "+fund));doc.add(new Paragraph("Nominal dana: "+SocialHtml.money(d.getGrossDonationAmount())));doc.add(new Paragraph("Institusi: "+receipt.getTenantKey()));doc.add(new Paragraph("Status: "+(receipt.getVoided()?"VOID":"VALID")));doc.add(new Paragraph(" "));doc.add(new Paragraph("Verifikasi: "+Common.getRequestHostWithProtocol(q)+"/sosial/verifikasi-bukti/"+token));doc.add(new Paragraph("Bukti ini diterbitkan setelah pembayaran terverifikasi oleh server."));doc.close();}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"SosialReceiptPdf");if(!r.isCommitted())r.sendError(500);}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}}
}
