package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Radio;

import ais.common.Common;

public class MyRadioConfig extends Radio {

	public MyRadioConfig() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyRadioConfig(String label, String image) {
		super(Common.getBahasaConfig(label).trim(), image);
		// TODO Auto-generated constructor stub
	}

	public MyRadioConfig(String label) {
		super(Common.getBahasaConfig(label).trim());
		// TODO Auto-generated constructor stub
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

	
	public MyRadioConfig setValueData(String val) {
		super.setValue(val);
		return this;
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyRadioConfig setLabelData(String text) {
		super.setLabel(text);
		return this;
	}
	
	public MyRadioConfig setCheckedData(Boolean val) {
		super.setChecked(val);
		return this;
	}
	
	
}
