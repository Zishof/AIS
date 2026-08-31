package ais.action.servlet.api;

/**
 * Utilitas untuk memetakan MIME type berkas menjadi pasangan ikon Font Awesome dan kelas warna
 * Bootstrap, dipakai UI untuk menampilkan ikon representatif tanpa perlu membuka/mengunduh
 * berkasnya (mis. daftar lampiran/dokumen).
 */
public class FileHelper {

	/** Pasangan nama kelas ikon Font Awesome ({@code icon}) dan kelas warna Bootstrap ({@code color}) untuk satu jenis berkas. */
	public static class IconStyle {
		public String icon;
		public String color;

		public IconStyle(String icon, String color) {
			this.icon = icon;
			this.color = color;
		}
	}

	/**
	 * Menentukan {@link IconStyle} yang sesuai untuk {@code mimeType} (pencocokan tidak peka
	 * huruf besar/kecil): PDF, Word, Excel, PowerPoint (dicocokkan lewat kata kunci pada string
	 * MIME type, termasuk varian OOXML), gambar/video/audio (lewat prefix {@code image/}/
	 * {@code video/}/{@code audio/}), atau ikon generik bila tidak dikenali/{@code null}.
	 */
	public static IconStyle getIconAndColor(String mimeType) {
		// Null check untuk menghindari error
		if (mimeType == null) {
			mimeType = "";
		}

		// Normalisasi ke huruf kecil agar pencarian konsisten
		mimeType = mimeType.toLowerCase();

		// LOGIKA IF (Berdasarkan MIME Type standar)
		if (mimeType.equals("application/pdf")) {
			return new IconStyle("fa-file-pdf", "text-danger");

			// Word (doc: application/msword, docx: ...wordprocessingml...)
		} else if (mimeType.contains("word") || mimeType.contains("document")) {
			return new IconStyle("fa-file-word", "text-primary");

			// Excel (xls: ...excel, xlsx: ...spreadsheetml...)
		} else if (mimeType.contains("excel") || mimeType.contains("spreadsheet") || mimeType.contains("sheet")) {
			return new IconStyle("fa-file-excel", "text-success");

			// PowerPoint (ppt: ...powerpoint, pptx: ...presentationml...)
		} else if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) {
			return new IconStyle("fa-file-powerpoint", "text-warning");

			// Image (image/png, image/jpeg, dll)
		} else if (mimeType.startsWith("image/")) {
			return new IconStyle("fa-file-image", "text-info");

			// Video
		} else if (mimeType.startsWith("video/")) {
			return new IconStyle("fa-youtube", "text-danger");

			// Audio
		} else if (mimeType.startsWith("audio/")) {
			return new IconStyle("fa-music", "text-secondary");

		} else {
			// Default
			return new IconStyle("fa-file-alt", "text-dark");
		}
	}

	/** Contoh penggunaan/uji coba manual {@link #getIconAndColor(String)} lewat baris perintah. */
	public static void main(String[] args) {
		// Tes fungsi
		IconStyle style = getIconAndColor("application/pdf");

		System.out.println("Icon: " + style.icon); // Output: fa-file-pdf
		System.out.println("Color: " + style.color); // Output: text-danger
	}
}