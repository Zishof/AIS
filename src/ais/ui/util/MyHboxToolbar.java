package ais.ui.util;

import org.zkoss.zul.Hbox;

/**
 * Tipe khusus untuk my hbox toolbar. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Hbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setStyle()}, {@code setHeight}(). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Hbox
 */
public class MyHboxToolbar extends Hbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyHboxToolbar() {
		super();
		super.setWidth("100%");
		super.setHeight("35px");
	}

	public void setStyle(String value) {

	}

	public void setHeight(String value) {

	}
}
