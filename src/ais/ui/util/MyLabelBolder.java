package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;
import ais.common.Common;

public class MyLabelBolder extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelBolder() {
		super();
		setWidth("100%");
		super.setStyle("font-size:16px;font-weight: bolder;");
		// TODO Auto-generated constructor stub
	}

	public MyLabelBolder(String value) throws WrongValueException {
		super(Common.getBahasaConfig(value));
		setWidth("100%");
		super.setStyle("font-size:16px;font-weight: bolder;");
		// TODO Auto-generated constructor stub
	}

	public void setValue(String value) {
		super.setValue(Common.getBahasaConfig(value));
	}

	public void setStyle(String value) {

	}

}
