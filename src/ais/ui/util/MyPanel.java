package ais.ui.util;

import org.zkoss.zul.Panel;

/**
 * Tipe khusus untuk my panel. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Panel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setTitle}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Panel
 */
public class MyPanel extends Panel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1924600363871165127L;

	public MyPanel() {
		super();
		setStyle("border:0px;");
		setBorder("none");
	}

	@Override
	public void setTitle(String title) {
		super.setTitle("");
	}

}
