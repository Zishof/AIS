package ais.ui.util;

import java.util.Date;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Datebox;

import ais.common.Common;

public class MyDatebox extends Datebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyDatebox() {
		super();
		init();
	}

	public MyDatebox(Date date) throws WrongValueException {
		super(date);
		init();
	}

	private void init() {
		setFormat(Common.dateFormat1.get().toPattern());
//		setReadonly(true);
		// setWidth("90%");
	}

	@Override
	public void setWidth(String width) {
		// // TODO Auto-generated method stub
		// super.setWidth(width);
	}

}
