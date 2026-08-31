package ais.ui.util;

import org.zkoss.zul.Groupbox;

/**
 * Tipe khusus untuk my groupbox styled. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Groupbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setStyle()}, {@code setStyleLangsung()},
 * {@code setWidth}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * @see Groupbox
 */
public class MyGroupboxStyled extends Groupbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyGroupboxStyled() {
		super();
		super.setWidth("97%");

		super.setStyle(
				"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;");
	}

	public void setStyle(String value) {

	}

	public void setStyleLangsung(String value) {
		super.setStyle(value);
	}

	public void setWidth(String w) {

	}
}
