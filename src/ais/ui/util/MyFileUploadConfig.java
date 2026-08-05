package ais.ui.util;

import org.zkoss.zul.Fileupload;

import ais.common.Common;

public class MyFileUploadConfig extends Fileupload {

	public MyFileUploadConfig() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyFileUploadConfig(String label, String image) {
		super(Common.getBahasaConfig(label), image);
		// TODO Auto-generated constructor stub
	}

	public MyFileUploadConfig(String label) {
		super(Common.getBahasaConfig(label));
		// TODO Auto-generated constructor stub
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
