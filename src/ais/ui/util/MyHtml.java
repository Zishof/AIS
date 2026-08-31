package ais.ui.util;

import org.zkoss.zul.Html;

/**
 * Tipe khusus untuk my html. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Html}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code getContent()}, {@code bersihkan}(). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Html
 */
public class MyHtml extends Html {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7510808476684701322L;

	public MyHtml() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyHtml(String content) {
		super(content);
		// TODO Auto-generated constructor stub

	}

	public String getContent() {
		String c = MyHtml.bersihkan(super.getContent());
		return c;
	}

	public static String bersihkan(String c) {
		if (c != null && c.toLowerCase().contains("script")) {
			c = c.replaceAll("(?i)script", "__S__");
		}
		return c;
	}

}
