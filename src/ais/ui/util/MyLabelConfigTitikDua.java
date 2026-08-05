package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;

import ais.common.Common;

public class MyLabelConfigTitikDua extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelConfigTitikDua() {
		super();
		setWidth("100%");
	}

	public MyLabelConfigTitikDua(String value) throws WrongValueException {
		super(Common.getBahasaConfig(value));
		setWidth("100%");
	}

	public void setValue(String value) {
		super.setValue(Common.getBahasaConfig(value));
	}

}
