package ais.ui.util;

import java.util.Date;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Datebox;

import ais.common.Common;

/**
 * Tipe khusus untuk my datebox. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Datebox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code init()}, {@code setWidth}(). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Datebox
 */
public class MyDatebox extends Datebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyDatebox() {
		super();
		init();
	}

	public MyDatebox(Date date) throws WrongValueException {
		super(date);
		init();
	}

	private void init() {
		setFormat(Common.dateFormat1.get().toPattern());
//		setReadonly(true);
		// setWidth("90%");
	}

	@Override
	public void setWidth(String width) {
		// // TODO Auto-generated method stub
		// super.setWidth(width);
	}

}
