package ais.ui.util;

import org.zkoss.zul.Treeitem;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my treeitem config. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Treeitem}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah mutasi data ({@code setTooltiptext()}, {@code setLabel()}, {@code
 * setImage()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Treeitem
 */
public class MyTreeitemConfig extends Treeitem {

	public MyTreeitemConfig() {
		super();
		setSclass("menu_item");
	}

	public MyTreeitemConfig(String label, Object value) {
		super(Common.getBahasaConfig(label), value);
		setSclass("menu_item");
	}

	public MyTreeitemConfig(String prefix, String label, Object value) {
		super(Common.getBahasaConfig(prefix, label), value);
		setSclass("menu_item");
	}

	public MyTreeitemConfig(String label) {
		super(Common.getBahasaConfig(label));
		setSclass("menu_item"); 
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}
	
	@Override
	public void setImage(String src) {
		src = MyMenuitem.svgIcon(getLabel(), src);
		super.setImage(src);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
