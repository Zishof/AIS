package ais.ui.util;

import org.zkoss.zul.Comboitem;

import ais.common.Common;

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
