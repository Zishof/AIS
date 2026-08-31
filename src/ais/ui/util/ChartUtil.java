package ais.ui.util;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import ais.common.Common;

/**
 * Kumpulan utilitas statis untuk merender objek grafik {@link JFreeChart} (dipakai di
 * layar-layar dashboard/laporan AIS untuk menampilkan grafik capaian kinerja, statistik, dsb.)
 * ke tiga format keluaran berbeda: gambar PNG untuk ditampilkan langsung di komponen ZK,
 * dokumen PDF untuk diunduh sebagai berkas, dan stream byte PDF mentah untuk keperluan lain
 * (mis. digabung ke dokumen PDF lain).
 *
 * <p>
 * Ketiga metode render bekerja di atas prinsip yang sama: {@link JFreeChart} tahu cara
 * menggambar dirinya sendiri ke sebuah {@link Graphics2D}; untuk PNG, target gambarnya adalah
 * {@link BufferedImage} yang lalu di-encode ke byte PNG lewat {@code EncoderUtil}; untuk PDF,
 * target gambarnya adalah {@link PdfTemplate} dari pustaka OpenPDF/iText (paket
 * {@code com.lowagie.text}) yang ditempelkan ke satu halaman dokumen PDF berukuran sama dengan
 * dimensi grafik. Kedua metode PDF ({@link #writeChartAsPDF} dan {@link #writeChartAsStream})
 * berbagi logika penggambaran yang identik, hanya berbeda pada bentuk hasil akhir yang
 * dikembalikan ({@link AMedia} vs {@link InputStream} mentah).
 * </p>
 * <p>
 * Kelas ini juga memuat satu method {@link #main(String[])} berisi eksperimen parsing tanggal
 * dengan {@link SimpleDateFormat} yang tidak berkaitan langsung dengan fungsi render grafik —
 * kemungkinan sisa uji coba manual yang ditinggalkan di kelas ini.
 * </p>
 */
public class ChartUtil {

	/**
	 * Merender {@code chart} menjadi gambar PNG dalam bentuk {@link AImage} (tipe gambar ZK
	 * siap ditempel ke komponen {@code Image}/{@code Imagemap}), dengan latar belakang
	 * transparan ({@link BufferedImage#TRANSLUCENT}).
	 *
	 * @param chart     grafik JFreeChart yang akan dirender
	 * @param width     lebar gambar keluaran dalam piksel
	 * @param height    tinggi gambar keluaran dalam piksel
	 * @param chartName nama yang dilekatkan pada {@link AImage} (dipakai ZK sebagai nama berkas
	 *                  gambar)
	 * @return gambar PNG hasil render sebagai {@link AImage}, atau {@code null} bila terjadi
	 *         kegagalan I/O saat encoding (kegagalan dicatat lewat
	 *         {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ulang)
	 */
	public static AImage writeChartAsImage(JFreeChart chart, int width, int height, String chartName) {
		AImage oResult = null;
		BufferedImage bi = chart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT , null);
		try {
			byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);
			oResult = new AImage(chartName, bytes);
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		return oResult;
	}
	
	/**
	 * Merender {@code chart} menjadi satu halaman dokumen PDF (berukuran sama dengan dimensi
	 * grafik, margin 50pt di semua sisi) dan mengembalikannya sebagai {@link AMedia} (tipe
	 * media ZK) siap diunduh/ditampilkan, misalnya sebagai lampiran unduhan laporan grafik.
	 * Metadata dokumen diisi penulis {@code "JFreeChart"} dan subjek {@code "Grafik Capaian
	 * Kinerja"}. Kegagalan pembuatan dokumen ({@link DocumentException}) ditangkap dan dicatat
	 * lewat {@link Common#tampilErrorJikaAdmin(Exception)}; pada kasus tersebut dokumen tetap
	 * ditutup dan {@link AMedia} yang dikembalikan berisi output PDF yang mungkin tidak lengkap.
	 *
	 * @param chart     grafik JFreeChart yang akan dirender
	 * @param width     lebar halaman/grafik dalam satuan poin PDF
	 * @param height    tinggi halaman/grafik dalam satuan poin PDF
	 * @param chartName nama berkas yang dilekatkan pada {@link AMedia}
	 * @return dokumen PDF hasil render sebagai {@link AMedia} bertipe {@code "application/pdf"}
	 * @throws IOException diteruskan bila terjadi kegagalan I/O saat menulis stream keluaran
	 */
	public static AMedia writeChartAsPDF(JFreeChart chart, int width, int height, String chartName) throws IOException {
		Rectangle pagesize = new Rectangle(width, height);
		Document document = new Document(pagesize, 50, 50, 50, 50);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter writer = PdfWriter.getInstance(document, out);
			document.addAuthor("JFreeChart");
			document.addSubject("Grafik Capaian Kinerja");
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			PdfTemplate tp = cb.createTemplate(width, height);
			Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
			Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
			chart.draw(g2, r2D, null);
			g2.dispose();
			cb.addTemplate(tp, 0, 0);
		}
		catch(DocumentException e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		document.close();
		final InputStream mediais = new ByteArrayInputStream(out.toByteArray());
        final AMedia amedia = new AMedia(chartName, "pdf", "application/pdf", mediais);
        
        return amedia;
	}

	/**
	 * Sama seperti {@link #writeChartAsPDF(JFreeChart, int, int, String)} (grafik dirender ke
	 * satu halaman dokumen PDF berukuran sama dengan dimensi grafik, margin 50pt), tetapi
	 * mengembalikan byte PDF mentah sebagai {@link InputStream} alih-alih {@link AMedia}
	 * bernama — berguna ketika hasil PDF akan digabung/diproses lebih lanjut (mis. disatukan ke
	 * dokumen PDF lain) alih-alih langsung disajikan sebagai unduhan ZK.
	 *
	 * @param chart  grafik JFreeChart yang akan dirender
	 * @param width  lebar halaman/grafik dalam satuan poin PDF
	 * @param height tinggi halaman/grafik dalam satuan poin PDF
	 * @return stream byte dokumen PDF hasil render
	 * @throws IOException diteruskan bila terjadi kegagalan I/O saat menulis stream keluaran
	 */
	public static InputStream writeChartAsStream(JFreeChart chart, int width, int height) throws IOException {
		Rectangle pagesize = new Rectangle(width, height);
		Document document = new Document(pagesize, 50, 50, 50, 50);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter writer = PdfWriter.getInstance(document, out);
			document.addAuthor("JFreeChart");
			document.addSubject("Grafik Capaian Kinerja");
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			PdfTemplate tp = cb.createTemplate(width, height);
			Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
			Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
			chart.draw(g2, r2D, null);
			g2.dispose();
			cb.addTemplate(tp, 0, 0);
		}
		catch(DocumentException e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		document.close();
		return new ByteArrayInputStream(out.toByteArray());
	}
	
	
	/**
	 * Berkas uji coba/scratch manual: mengurai satu string tanggal contoh
	 * ({@code "20-01-2009 01:09:00"}) memakai pola {@code "dd-MM-yyyy HH:mm:ss"} dan mencetak
	 * hasilnya ke konsol. Tidak berkaitan dengan fungsi render grafik kelas ini; kegagalan
	 * parsing dicatat lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param argv argumen baris perintah, tidak dipakai
	 */
	public static void main(String[]argv){
		String tanggal = "20-01-2009 01:09:00";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		try {
			Date date = simpleDateFormat.parse(tanggal);
			System.out.println(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
		}
	}

}
