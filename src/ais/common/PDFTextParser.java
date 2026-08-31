package ais.common;

/*
 * PDFTextParser.java
 *
 * Created on January 24, 2009, 11:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author prasanna
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * Utilitas berkas/media untuk pdf text parser. Kelas ini memusatkan resolusi lokasi, stream,
 * upload/download, atau transformasi media agar aturan penyimpanan dan respons file tidak
 * tersebar.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PDFParser parser}, {@code String
 * parsedText}, {@code PDFTextStripper pdfStripper}, {@code PDDocument pdDoc}, {@code COSDocument cosDoc};
 * operasi domain lain ({@code pdftoText()}, {@code writeTexttoFile()}, {@code main()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class PDFTextParser {

	private PDFParser parser;
	private String parsedText;
	private PDFTextStripper pdfStripper;
	private PDDocument pdDoc;
	private COSDocument cosDoc;
	
	// PDFTextParser Constructor
	public PDFTextParser() {
	}

	// Extract text from PDF Document
	public String pdftoText(String fileName) {

		System.out.println("Parsing text from PDF file " + fileName + "....");
		File f = new File(fileName);

		if (!f.isFile()) {
			System.out.println("File " + fileName + " does not exist.");
			return null;
		}

		try {
			parser = new PDFParser(new FileInputStream(f));
		} catch (Exception e) {
			System.out.println("Unable to open PDF Parser.");
			return null;
		}

		try {
			parser.parse();
			cosDoc = parser.getDocument();
			pdfStripper = new PDFTextStripper();
			pdDoc = new PDDocument(cosDoc);
			parsedText = pdfStripper.getText(pdDoc);
		} catch (Exception e) {
			System.out
					.println("An exception occured in parsing the PDF Document.");
			Common.tampilErrorJikaAdmin(e); 
			try {
				if (cosDoc != null)
					cosDoc.close();
				if (pdDoc != null)
					pdDoc.close();
			} catch (Exception e1) {
				Common.tampilErrorJikaAdmin(e); 
			}
			return null;
		}
		System.out.println("Done.");
		return parsedText;
	}

	// Write the parsed text from PDF to a file
	public void writeTexttoFile(String pdfText, String fileName) {

		System.out.println("\nWriting PDF text to output text file " + fileName
				+ "....");
		try {
			PrintWriter pw = new PrintWriter(fileName);
			pw.print(pdfText);
			pw.close();
		} catch (Exception e) {
			System.out
					.println("An exception occured in writing the pdf text to file.");
			Common.tampilErrorJikaAdmin(e); 
		}
		System.out.println("Done.");
	}

	// Extracts text from a PDF Document and writes it to a text file
	public static void main(String args[]) {

		// if (args.length != 2) {
		// System.out.println("Usage: java PDFTextParser <InputPDFFilename> <OutputTextFile>");
		// System.exit(1);
		// }

		PDFTextParser pdfTextParserObj = new PDFTextParser();
		String pdfToText = pdfTextParserObj.pdftoText("C:/opt/Abstracts.pdf");

		if (pdfToText == null) {
			System.out.println("PDF to Text Conversion failed.");
		} else {
			System.out.println("\nThe text parsed from the PDF Document....\n"
					+ pdfToText);
			pdfTextParserObj.writeTexttoFile(pdfToText, "C:/opt/Abstracts.txt");
		}
	}
}
