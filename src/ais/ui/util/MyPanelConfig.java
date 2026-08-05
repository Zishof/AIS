package ais.ui.util;

import org.zkoss.zul.Panel;

import ais.common.Common;

public class MyPanelConfig extends Panel {

	public MyPanelConfig() {
		super();
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setTitle(String text) {
		super.setTitle(Common.getBahasaConfig(text));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
