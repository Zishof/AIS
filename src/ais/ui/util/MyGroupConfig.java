package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Group;

import ais.common.Common;

public class MyGroupConfig extends Group {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyGroupConfig() {
		super();
	}

	public MyGroupConfig(String value) throws WrongValueException {
		super(Common.getBahasaConfig(value));
	}

	public void setValue(String value) {
		super.setValue(Common.getBahasaConfig(value));
	}

}
