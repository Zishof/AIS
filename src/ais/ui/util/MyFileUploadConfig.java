package ais.ui.util;

import org.zkoss.zul.Fileupload;

import ais.common.Common;

/**
 * Tipe khusus untuk my file upload config. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Fileupload}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setTooltiptext()}, {@code setLabel}().
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Fileupload
 */
public class MyFileUploadConfig extends Fileupload {

	public MyFileUploadConfig() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyFileUploadConfig(String label, String image) {
		super(Common.getBahasaConfig(label), image);
		// TODO Auto-generated constructor stub
	}

	public MyFileUploadConfig(String label) {
		super(Common.getBahasaConfig(label));
		// TODO Auto-generated constructor stub
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
