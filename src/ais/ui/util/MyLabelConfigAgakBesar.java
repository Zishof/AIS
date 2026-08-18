package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;

import ais.common.Common;

public class MyLabelConfigAgakBesar extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelConfigAgakBesar() {
		super();
		setWidth("100%");
		setStyle("font-size:14px;");
	}

	public MyLabelConfigAgakBesar(String value) throws WrongValueException {
		super(Common.getBahasaConfig(value));
		setWidth("100%");
		setStyle("font-size:14px;");
	}

	public void setValue(String value) {
		super.setValue(Common.getBahasaConfig(value));
	}

}
