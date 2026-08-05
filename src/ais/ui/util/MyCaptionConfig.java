package ais.ui.util;

import org.zkoss.zul.Caption;

import ais.common.Common;

/**
 * <h2>Caption (judul groupbox/panel) yang MENERJEMAHKAN label.</h2>
 *
 * <p>Menerjemahkan atribut {@code label} lewat {@link Common#getBahasaConfig(String)} untuk teks
 * STATIS. Untuk label berisi DATA DINAMIS gunakan {@link #setLabelData(String)} agar tidak
 * diterjemah dan tidak mencemari tabel LabelBahasa.</p>
 */
public class MyCaptionConfig extends Caption {

	private static final long serialVersionUID = -8165594983232482912L;

	public MyCaptionConfig() {
		super();
	}

	public MyCaptionConfig(String label) {
		super(Common.getBahasaConfig(label));
	}

	@Override
	public void setLabel(String label) {
		super.setLabel(Common.getBahasaConfig(label));
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyCaptionConfig setLabelData(String label) {
		super.setLabel(label);
		return this;
	}
}
