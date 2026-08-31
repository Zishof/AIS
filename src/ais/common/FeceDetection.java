package ais.common;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Kode contoh/uji coba manual (bukan komponen yang dipanggil dari alur aplikasi AIS lainnya)
 * untuk deteksi wajah pada sebuah gambar menggunakan OpenCV (paket {@code org.opencv.*}), memakai
 * pendeteksi berbasis cascade Haar klasik ({@link CascadeClassifier} dengan file
 * {@code haarcascade_frontalface_alt.xml}).
 *
 * <p>
 * <b>Nama kelas:</b> nama {@code FeceDetection} tampaknya salah ketik dari "Face Detection"
 * (huruf "e" seharusnya "a"). Nama ini TIDAK diubah sebagai bagian dari pekerjaan dokumentasi
 * ini — mengganti nama kelas berpotensi memengaruhi referensi lain dan berada di luar cakupan
 * penyisipan Javadoc murni.
 * </p>
 *
 * <p>
 * Seluruh logika berada pada satu method {@link #main(String[])} yang bersifat prosedural dan
 * bergantung pada path berkas <b>tertanam langsung di kode</b> (bukan dibaca dari konfigurasi):
 * file cascade {@code "haarcascade_frontalface_alt.xml"} harus berada di direktori kerja saat
 * dijalankan, gambar masukan dibaca dari path absolut Windows {@code "E:\input.jpg"}, dan hasil
 * deteksi (gambar dengan kotak hijau mengelilingi wajah yang ditemukan) ditulis ke
 * {@code "E:\Ouput.jpg"} (perhatikan juga salah ketik "Ouput" alih-alih "Output" pada nama file
 * keluaran, dibiarkan apa adanya). Path-path ini membuat kelas hanya dapat dijalankan pada mesin
 * dengan struktur direktori yang sama persis; ini bukan kredensial/rahasia, hanya path lokal
 * peninggalan pada kode contoh, sehingga tidak memerlukan tindak lanjut keamanan.
 * </p>
 */
public class FeceDetection {

	/**
	 * Menjalankan alur demo deteksi wajah end-to-end: memuat pustaka native OpenCV, memuat model
	 * cascade wajah, membaca gambar masukan, mendeteksi wajah, menggambar kotak pembatas hijau di
	 * sekeliling tiap wajah yang ditemukan, lalu menyimpan gambar hasil ke berkas keluaran. Lihat
	 * Javadoc kelas untuk peringatan mengenai path berkas yang tertanam langsung di kode.
	 *
	 * @param args tidak dipakai
	 */
	public static void main(String[] args) {

		// Core.NATIVE_LIBRARY_NAME must be loaded before
		// calling any of the opencv methods
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Face detector creation by loading source cascade
		// xml file using CascadeClassifier and must be
		// placed in same directory of the source java file

		// File is available here on git as mentioned above
		// prior to code
		CascadeClassifier faceDetector = new CascadeClassifier();
		faceDetector.load("haarcascade_frontalface_alt.xml");

		// TODO Auto-generated method stub
		// Reading the input image
		Mat image = Imgcodecs.imread("E:\\input.jpg");

		// Detecting faces
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);

		// Creating a rectangular box which represents for
		// faces detected
		for (Rect rect : faceDetections.toArray()) {
			Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
					new Scalar(0, 255, 0));
		}

		// Saving the output image
		String filename = "Ouput.jpg";

		Imgcodecs.imwrite("E:\\" + filename, image);

		// Display message for successful execution of
		// program
		System.out.print("Face Detected");
	}

}
