package ais.ui.util;

import org.zkoss.zul.Comboitem;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my comboitem config. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Comboitem}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getLabel()}); mutasi data ({@code
 * setTooltiptext()}, {@code setLabel()}, {@code setValueData()}, {@code setLabelData()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Comboitem
 */
public class MyComboitemConfig extends Comboitem {

	public MyComboitemConfig() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyComboitemConfig(String text, String image) {
		super(Common.getBahasaConfig(text).trim(), image);
	}

	public MyComboitemConfig(String text) {
		super(Common.getBahasaConfig(text).trim());
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text).trim());
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text).trim());
	}

	public String getLabel() {
		return super.getLabel() == null ? "" : super.getLabel().trim();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;
 
	public MyComboitemConfig setValueData(Object val) {
		// TODO Auto-generated method stub
		super.setValue(val);
		return this;
	}

	
	public MyComboitemConfig setLabelData(String val) {
		// TODO Auto-generated method stub
		super.setLabel(val);
		return this;
	}
}
