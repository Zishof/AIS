package ais.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;

/**
 * Kelas utilitas statis untuk memampatkan (compress) dan membongkar (decompress) string
 * menggunakan format GZIP, dibangun di atas {@link java.util.zip.GZIPOutputStream} dan
 * {@link java.util.zip.GZIPInputStream} dari JDK standar. Dipakai di tempat-tempat pada codebase
 * AIS yang perlu menyimpan/mengirim teks besar (mis. isi JSON/HTML panjang) secara lebih ringkas,
 * dengan kemampuan mendeteksi otomatis apakah suatu larik byte memang berformat GZIP sebelum
 * mencoba membongkarnya.
 *
 * <p>
 * Kelas ini murni stateless (tidak ada field instans maupun konstruktor kustom) — seluruh method
 * publik bersifat statis dan dapat dipanggil langsung tanpa membuat instans.
 * </p>
 */
public class GzipUtil {

	/**
	 * Titik masuk demo/uji coba manual untuk kelas ini: memampatkan sebuah string contoh, menulis
	 * hasilnya ke berkas {@code /opt/test.gz}, membaca kembali berkas tersebut, membongkarnya, lalu
	 * mencetak hasilnya ke konsol. Path berkas {@code "/opt/test.gz"} tertanam langsung di kode
	 * (khas kode demo, bukan dipanggil dari alur aplikasi lain).
	 *
	 * @param rgv tidak dipakai
	 * @throws Exception diteruskan apa adanya dari operasi berkas/IO selama demo
	 */
	public static void main(String[] rgv) throws Exception {
		File file = new File("/opt/test.gz");
		FileUtils.writeByteArrayToFile(file,
				zip("test_123ssssssssssssssssssssssssssssssssssssssssssssssssssasasasas as a sasasa"));
		String out = unzip(FileUtils.readFileToByteArray(file));

		System.out.println(out);
	}

	/**
	 * Memampatkan sebuah string menjadi larik byte terkompresi format GZIP, dienkode terlebih
	 * dahulu sebagai UTF-8 sebelum dipampatkan.
	 *
	 * @param str string yang akan dipampatkan; tidak boleh {@code null} atau kosong
	 * @return larik byte hasil kompresi GZIP dari {@code str}
	 * @throws IllegalArgumentException bila {@code str} {@code null} atau string kosong
	 * @throws RuntimeException         bila terjadi galat I/O saat proses kompresi
	 */
	public static byte[] zip(final String str) {
		if ((str == null) || (str.length() == 0)) {
			throw new IllegalArgumentException("Cannot zip null or empty string");
		}

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gzipOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to zip content", e);
		}
	}

	/**
	 * Membongkar larik byte menjadi string. Bila {@code compressed} terdeteksi BUKAN berformat
	 * GZIP (lewat {@link #isZipped(byte[])}), larik byte tersebut dikembalikan apa adanya sebagai
	 * string (dianggap sudah berupa teks polos) alih-alih dipaksa dibongkar — sehingga method ini
	 * aman dipanggil pada data yang belum tentu terkompresi.
	 *
	 * @param compressed larik byte, boleh berformat GZIP atau teks polos; tidak boleh
	 *                   {@code null} atau kosong
	 * @return string hasil pembongkaran (bila {@code compressed} berformat GZIP), atau string hasil
	 *         konversi langsung larik byte (bila bukan GZIP)
	 * @throws IllegalArgumentException bila {@code compressed} {@code null} atau kosong
	 * @throws RuntimeException         bila terjadi galat I/O saat proses pembongkaran GZIP
	 */
	public static String unzip(final byte[] compressed) {
		if ((compressed == null) || (compressed.length == 0)) {
			throw new IllegalArgumentException("Cannot unzip null or empty bytes");
		}
		if (!isZipped(compressed)) {
			return new String(compressed);
		}

		try {
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
			GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
			InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				output.append(line);
			}
			return output.toString();
		} catch (IOException e) {
			throw new RuntimeException("Failed to unzip content", e);
		}
	}

	/**
	 * Memeriksa apakah larik byte diawali dengan magic number GZIP ({@link
	 * java.util.zip.GZIPInputStream#GZIP_MAGIC}, dua byte pertama), sebagai penanda apakah data
	 * tersebut kemungkinan besar berformat GZIP.
	 *
	 * <p>
	 * <b>Catatan:</b> method ini mengakses {@code compressed[0]} dan {@code compressed[1]} tanpa
	 * memeriksa panjang larik lebih dulu; larik dengan panjang kurang dari 2 akan melempar
	 * {@link ArrayIndexOutOfBoundsException}. Pemanggil internal ({@link #unzip(byte[])}) sudah
	 * memvalidasi larik tidak kosong sebelum memanggil method ini, namun pemanggil eksternal lain
	 * perlu memastikan hal yang sama.
	 * </p>
	 *
	 * @param compressed larik byte yang akan diperiksa
	 * @return {@code true} bila dua byte pertama cocok dengan magic number GZIP
	 */
	public static boolean isZipped(final byte[] compressed) {
		return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
				&& (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
	}
}