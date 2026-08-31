package ais.ui.util;

import org.zkoss.zul.Html;

/**
 * Tipe khusus untuk my html iframe. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Html}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code getContent}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Html
 */
public class MyHtmlIframe extends Html {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7510808476684701322L;

	public MyHtmlIframe() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyHtmlIframe(String content) {
		super(content);
		// TODO Auto-generated constructor stub
		
	}

	public String getContent() {
		String c = super.getContent();
		if (c != null && c.toLowerCase().contains("script")) {
			c = c.replaceAll("(?i)script", "__S__");
		}
		if (c != null && c.toLowerCase().contains("iframe")) {
			c = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(c, "iframe", "");
		}
		return c;
	}

}
