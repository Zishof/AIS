package ais.ui.util;

import org.zkoss.zul.Hbox;

/**
 * Tipe khusus untuk my hbox styled. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Hbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setStyle}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Hbox
 */
public class MyHboxStyled extends Hbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyHboxStyled() {
		super();
		setWidth("100%");
		super.setStyle(
				"border: 1px solid #bdbbbb;padding: 10px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 20px 20px;");
	}

	public void setStyle(String value) {

	}

}
