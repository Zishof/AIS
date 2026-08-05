package ais.common;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.zkoss.image.AImage;
import org.zkoss.zul.Image;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import ais.action.master.library.barcode.BarcodeGenerator;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.sekolah.Siswa;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class BarcodeCommon {

	private static long index = 1L;

	public static Image generateBarcodeImage(String code) {
		Image barcode = new Image();
		try {
			barcode.setContent(generateBarcodeAImage(code));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		barcode.setWidth("200px");
		return barcode;
	}

	public static AImage generateBarcodeAImage(String code) throws Exception {
		final File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + code + ".png");

		Barcode mybarcode = BarcodeFactory.createCode128B(code);
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);

		AImage aImage = new AImage(myfilebarcode);
		return aImage;
	}

	public static AImage generateBarcodeKotakAImage(String code) throws Exception {
		final File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + code + ".png");

		Barcode mybarcode = BarcodeFactory.createCode128B(code);
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);

		AImage aImage = new AImage(myfilebarcode);
		return aImage;
	}

	/**
	 * QR Code (2D) sebagai AImage siap tempel ke komponen ZK {@code <image>}.
	 * Memakai ZXing (QR asli) sehingga dapat dipindai aplikasi pembaca QR member.
	 * Dipakai ulang oleh modul kantin (meja, POS, login QR, dll).
	 */
	public static AImage generateQrAImage(String code) throws Exception {
		String aman = code == null ? "" : code.replaceAll("[^A-Za-z0-9._-]", "_");
		final File qrFile = new File(Common.ambilREAL_PATH_REPORT() + "/qr_" + aman + ".png");
		generateCRCode(code, qrFile, 240, 240);
		return new AImage(qrFile);
	}

	public static String generateCode() {
		String code = "000000" + (++index);
		long milis = ais.ui.util.WaktuUtil.getDate().getTime();
		code = milis + (code.substring(code.length() - 3, code.length()));
		Long lg = Long.parseLong(code);
		return Long.toHexString(lg).toUpperCase();
	}

	public static String generateCode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		try {
			BarcodeGenerator barcodeGenerator = (BarcodeGenerator) Class
					.forName(Common.getKonfigurasi("class_untuk_generate_barcode",
							"ais.action.master.library.barcode.DefaultBarcodeGenerator").getNilai())
					.newInstance();
			return barcodeGenerator.generateBarcode(batchItemPunyaBarcode);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return "";
	}

	public static File generateCrCodeDosen(Dosen dosen, String url) {
		String text = "NIDN : " + dosen.getNidn();
		text += "\nNama : " + dosen.getNama();
		text += "\n" + Common.getBahasaConfig("Fakultas") + " : "
				+ (dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama());
		text += "\n" + Common.getBahasaConfig("Jurusan") + " : "
				+ (dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());
		text += "\nWaktu : " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate());
		if (url != null && !url.trim().isEmpty()) {
			text += "\nURL : " + url;
		}
		return generateCRCode(text);
	}

	public static File generateCrCodeMahasiswa(Mahasiswa mahasiswa, String url) {
		String text = "NIM : " + mahasiswa.getNim();
		text += "\nNama : " + mahasiswa.getNama();
		text += "\n" + Common.getBahasaConfig("Fakultas") + " : " + mahasiswa.getJurusan().getFakultas().getNama();
		text += "\n" + Common.getBahasaConfig("Jurusan") + " : " + mahasiswa.getJurusan().getNama();
		text += "\nWaktu : " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate());
		if (url != null && !url.trim().isEmpty()) {
			text += "\nURL : " + url;
		}
		return generateCRCode(text);
	}

	public static File generateCrCodeSiswa(Siswa siswa, String url) {
		String text = "NIM : " + siswa.getNim();
		text += "\nNama : " + siswa.getNama();
		text += "\n" + Common.getBahasaConfig("Yayasan") + " : " + siswa.getSekolah().getYayasan().getNama();
		text += "\n" + Common.getBahasaConfig("Sekolah") + " : " + siswa.getSekolah().getNama();
		text += "\nWaktu : " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate());
		if (url != null && !url.trim().isEmpty()) {
			text += "\nURL : " + url;
		}
		return generateCRCode(text);
	}

	public static File generateCrCodeMahasiswa(BiodataCalonMahasiswa mahasiswa, String url) {
		String text = "No.Reg : " + mahasiswa.getNim();
		text += "\nNama : " + mahasiswa.getNama();
		text += "\nWaktu : " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate());
		if (url != null && !url.trim().isEmpty()) {
			text += "\nURL : " + url;
		}
		return generateCRCode(text);
	}

	public static File generateCRCode(String code) {
		File file = new File(generateCode() + ".png");
		return generateCRCode(code, file);
	}

	public static File generateCRCode(String code, File file) {
		int h = 200;
		int w = 200;

		return generateCRCode(code, file, h, w);
	}

	public static File generateCRCode(String code, File file, int h, int w) {
		try {
			if (code == null) {
				// tidak ada data untuk di-encode, tidak perlu lanjut generate QR
				return file;
			}

			Charset charset = Charset.forName("ISO-8859-1");
			CharsetEncoder encoder = charset.newEncoder();
			// jangan lempar exception untuk karakter yang tidak bisa dipetakan ke ISO-8859-1
			// (mis. nama mahasiswa berisi karakter non-Latin) - ganti dengan karakter pengganti
			encoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE);
			encoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE);
			byte[] b = null;
			try {
				// Convert a string to ISO-8859-1 bytes in a ByteBuffer
				java.nio.ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(code));
				b = bbuf.array();
			} catch (CharacterCodingException e) {
				System.out.println(e.getMessage());
			}

			if (b == null) {
				// encoding gagal total, tidak ada data yang bisa dijadikan QR - hentikan dengan aman
				return file;
			}

			String data = "";
			try {
				data = new String(b, "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				System.out.println(e.getMessage());
			}

			// get a byte matrix for the data
			BitMatrix matrix = null;

			com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
			try {
				matrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, w, h);
			} catch (com.google.zxing.WriterException e) {
				System.out.println(e.getMessage());
			}

			try {
				MatrixToImageWriter.writeToFile(matrix, "PNG", file);
				//System.out.println("printing to " + file.getAbsolutePath());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			//System.out.println("cr code file = " + file.getAbsolutePath());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BarcodeCommon.java:193");
			// TODO: handle exception
		}
		return file;
	}

}
