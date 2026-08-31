package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Div;

/**
 * Tipe khusus untuk my div. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Div}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setStyle()}, {@code setMold()}, {@code
 * appendChild}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Div
 */
public class MyDiv extends Div {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7735707868353728408L;

	public MyDiv() {
		super();
		super.setWidth("100%");
		super.setStyle("min-height: 40px;  overflow: hidden;overflow-y:hidden;");
	}

	public void setStyle(String s) {

	}

	public void setMold(String m) {

	}

	public boolean appendChild(Component child) {
		if (child instanceof Caption) {
			return true;
		} else {
			return super.appendChild(child);
		}
	}

}
