package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;
import ais.common.Common;

/**
 * Tipe khusus untuk my label bolder. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Label}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setValue()}, {@code setStyle}(). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Label
 */
public class MyLabelBolder extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelBolder() {
		super();
		setWidth("100%");
		super.setStyle("font-size:16px;font-weight: bolder;");
		// TODO Auto-generated constructor stub
	}

	public MyLabelBolder(String value) throws WrongValueException {
		super(Common.getBahasaConfig(value));
		setWidth("100%");
		super.setStyle("font-size:16px;font-weight: bolder;");
		// TODO Auto-generated constructor stub
	}

	public void setValue(String value) {
		super.setValue(Common.getBahasaConfig(value));
	}

	public void setStyle(String value) {

	}

}
