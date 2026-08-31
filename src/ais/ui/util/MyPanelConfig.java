package ais.ui.util;

import org.zkoss.zul.Panel;

import ais.common.Common;

/**
 * Tipe khusus untuk my panel config. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Panel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setTooltiptext()}, {@code setTitle}().
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Panel
 */
public class MyPanelConfig extends Panel {

	public MyPanelConfig() {
		super();
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setTitle(String text) {
		super.setTitle(Common.getBahasaConfig(text));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
