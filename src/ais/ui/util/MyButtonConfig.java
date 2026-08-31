package ais.ui.util;

import org.zkoss.zul.Button;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my button config. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Button}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String labelLokal}; mutasi data ({@code
 * setTooltiptext()}, {@code setLabel()}, {@code setLabelData()}, {@code setImage()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Button
 */
public class MyButtonConfig extends Button {

	private String labelLokal = null;
	
	public MyButtonConfig() {
		super();
	}

	public MyButtonConfig(String label, String image) {
		super(Common.getBahasaConfig(label), MyMenuitem.svgIcon(label, image));
		if (label == null || !(label.equalsIgnoreCase("simpan") || label.equalsIgnoreCase("batal")
				|| label.toLowerCase().contains("proses") || label.equalsIgnoreCase("tutup")
				|| label.equalsIgnoreCase("selesai"))) {
			setStyle("font-size:12.5px;");
		} else {
			setStyle("font-size:15px;");
		}
		this.labelLokal = label;
	}

	public MyButtonConfig(String label) {
		super(Common.getBahasaConfig(label));
		if (label == null || !(label.equalsIgnoreCase("simpan") || label.equalsIgnoreCase("batal")
				|| label.toLowerCase().contains("proses") || label.equalsIgnoreCase("tutup")
				|| label.equalsIgnoreCase("selesai"))) {
			setStyle("font-size:12.5px;");
		} else {
			setStyle("font-size:15px;");
		}
		this.labelLokal = label;
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
	public MyButtonConfig setLabelData(String text) {
		this.labelLokal = text;
		super.setLabel(text);
		return this;
	}

	@Override
	public void setImage(String src) {
		String lbl = labelLokal == null || labelLokal.trim().isEmpty() ? ((getLabel() == null || getLabel().isEmpty()) ? getTooltiptext() : getLabel())
				: labelLokal;
		src = MyMenuitem.svgIcon(lbl, src, true);
		super.setImage(src);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
