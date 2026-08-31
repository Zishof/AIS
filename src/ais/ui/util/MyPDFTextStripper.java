package ais.ui.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * Ekstensi Apache PDFBox {@link PDFTextStripper} yang menambahkan kemampuan mengekstrak teks
 * dari SATU halaman tertentu saja dari sebuah dokumen PDF, alih-alih seluruh dokumen seperti
 * perilaku bawaan {@link PDFTextStripper}. Dipakai di modul-modul AIS yang perlu membaca isi
 * teks satu halaman spesifik (mis. memvalidasi/memindai konten halaman tertentu dalam dokumen
 * PDF yang diunggah) tanpa harus memproses seluruh dokumen yang bisa jadi besar.
 */
public class MyPDFTextStripper extends PDFTextStripper {

	/**
	 * Membuat instance stripper baru, meneruskan konstruksi ke {@link PDFTextStripper} bawaan.
	 *
	 * @throws IOException diteruskan dari konstruktor {@link PDFTextStripper} (mis. gagal
	 *                       memuat resource encoding bawaan PDFBox)
	 */
	public MyPDFTextStripper() throws IOException {
		super();
	}

	/**
	 * Mengekstrak dan mengembalikan teks dari satu halaman dokumen sebagai {@link String},
	 * dengan membungkus {@link #writeText(PDDocument, Writer, int)} di atas
	 * {@link StringWriter} agar hasilnya langsung dapat dipakai tanpa perlu menangani stream
	 * sendiri di sisi pemanggil.
	 *
	 * @param doc  dokumen PDF sumber
	 * @param page indeks halaman (berbasis 0) yang teksnya akan diekstrak
	 * @return teks hasil ekstraksi halaman tersebut
	 * @throws IOException diteruskan bila terjadi kegagalan saat memproses halaman
	 */
	public String getText(PDDocument doc, int page) throws IOException {
		StringWriter outputStream = new StringWriter();
		writeText(doc, outputStream, page);
		return outputStream.toString();
	}

	/**
	 * Menulis teks hasil ekstraksi satu halaman dokumen ke {@code outputStream}. Berbeda dari
	 * implementasi bawaan {@link PDFTextStripper#writeText}, method ini secara khusus hanya
	 * memasukkan objek halaman pada indeks {@code page} ke dalam daftar halaman yang diproses
	 * ({@link #processPages(List)}), sehingga hanya satu halaman itu saja yang diekstrak
	 * teksnya, bukan seluruh dokumen.
	 *
	 * @param doc          dokumen PDF sumber (juga diakses lewat field terwarisi
	 *                     {@code document})
	 * @param outputStream writer tujuan penulisan teks hasil ekstraksi
	 * @param page         indeks halaman (berbasis 0) yang akan diproses
	 * @throws IOException diteruskan bila terjadi kegagalan saat memproses halaman
	 */
	public void writeText(PDDocument doc, Writer outputStream, int page)
			throws IOException {
		// super.writeText(arg0, arg1);
		List<COSObjectable> list = new ArrayList<COSObjectable>();

		list.add((COSObjectable) document.getDocumentCatalog().getAllPages().get(page));

		processPages(list);
		endDocument(document);
	}

}
