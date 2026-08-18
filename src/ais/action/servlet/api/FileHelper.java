package ais.action.servlet.api;

public class FileHelper {

	// 1. Class sederhana untuk menampung hasil (pengganti object JS)
	public static class IconStyle {
		public String icon;
		public String color;

		public IconStyle(String icon, String color) {
			this.icon = icon;
			this.color = color;
		}
	}

	// 2. Fungsi utama
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

	// Contoh cara penggunaannya (Main method)
	public static void main(String[] args) {
		// Tes fungsi
		IconStyle style = getIconAndColor("application/pdf");

		System.out.println("Icon: " + style.icon); // Output: fa-file-pdf
		System.out.println("Color: " + style.color); // Output: text-danger
	}
}