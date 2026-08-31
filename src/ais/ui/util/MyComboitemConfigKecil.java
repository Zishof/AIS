package ais.ui.util;

import org.zkoss.zul.Comboitem;

import ais.common.Common;

/**
 * Tipe khusus untuk my comboitem config kecil. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Comboitem}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setTooltiptext()}, {@code setLabel()},
 * {@code getLabel}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * @see Comboitem
 */
public class MyComboitemConfigKecil extends Comboitem {

	public MyComboitemConfigKecil() {
		super();
		setStyle("font-size:8px;");
	}

	public MyComboitemConfigKecil(String text, String image) {
		super(Common.getBahasaConfig(text), image);
		setStyle("font-size:8px;");
	}

	public MyComboitemConfigKecil(String text) {
		super(Common.getBahasaConfig(text));
		setStyle("font-size:8px;");
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	public String getLabel() {
		return super.getLabel() == null ? "" : super.getLabel().trim();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
