package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Checkbox;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my checkbox config. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Checkbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah mutasi data ({@code setTooltiptext()}, {@code setLabel()}, {@code
 * setLabelData()}, {@code setParent()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Checkbox
 */
public class MyCheckboxConfig extends Checkbox {

	public MyCheckboxConfig() {
		super();
		setStyle("font-size:11px;");
		// TODO Auto-generated constructor stub
	}

	public MyCheckboxConfig(String label, String image) {
		super(Common.getBahasaConfig(label), image);
		setStyle("font-size:11px;");
	}

	public MyCheckboxConfig(String label) {
		super(Common.getBahasaConfig(label));
		setStyle("font-size:11px;");
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyCheckboxConfig setLabelData(String text) {
		super.setLabel(text);
		return this;
	}

	@Override
	public void setParent(Component arg0) {
		// TODO Auto-generated method stub
		if (arg0 != null) {
			arg0.setAttribute("checkbox", this);
		}
		super.setParent(arg0);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;




}
