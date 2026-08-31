package ais.ui.util;

import org.zkoss.zul.Menu;

import ais.common.Common;

/**
 * Tipe khusus untuk my menu. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Menu}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code labelLokal}; operasi lokal: {@code
 * setImage}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Menu
 */
public class MyMenu extends Menu {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6707899129552568407L;

	private String labelLokal = null;

	public MyMenu() {
		super();
		setSclass("menu_item");
	}

	public MyMenu(String prefix, String label, String src) {
		super(Common.getBahasaConfig(prefix, label), src);
		this.labelLokal = label;
		setSclass("menu_item");
	}

	public MyMenu(String label, String src) {
		super(Common.getBahasaConfig(label), src);
		this.labelLokal = label;
		setSclass("menu_item");
	}

	public MyMenu(String label) {
		super(Common.getBahasaConfig(label));
		this.labelLokal = label;
		setSclass("menu_item");
	}

	@Override
	public void setImage(String src) {
		String lbl = labelLokal == null || labelLokal.trim().isEmpty()
				? ((getLabel() == null || getLabel().isEmpty()) ? getTooltiptext() : getLabel())
				: labelLokal;
		src = MyMenuitem.svgIcon(lbl, src, true);
		super.setImage(src);
	}

}
