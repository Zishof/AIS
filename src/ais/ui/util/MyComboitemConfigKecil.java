package ais.ui.util;

import org.zkoss.zul.Comboitem;

import ais.common.Common;

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
