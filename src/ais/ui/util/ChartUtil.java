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

public class ChartUtil {

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
