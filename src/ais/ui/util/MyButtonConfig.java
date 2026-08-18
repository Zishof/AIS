package ais.ui.util;

import org.zkoss.zul.Button;

import ais.common.Common;

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
