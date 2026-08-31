package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Intbox;

/**
 * Tipe khusus untuk my intbox. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Intbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code init()}, {@code setValue}(). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Intbox
 */
public class MyIntbox extends Intbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyIntbox() {
		super();
		init();
	}

	public MyIntbox(Integer value) throws WrongValueException {
		super(value);
		init();
	}

	private void init() {
		setStyle("text-align: right;");
		setFormat("#,##0.##");
		// setWidth("95%");
	}

	public void setValue(Integer val) {
		super.setValue(val);
	}

}
