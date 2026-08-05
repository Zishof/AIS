package ais.ui.util;

import java.util.Date;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Timebox;

import ais.common.Common;

public class MyTimebox extends Timebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyTimebox() {
		super();
		init();
//		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//		calendar.set(Calendar.MINUTE, 0);
//		calendar.set(Calendar.SECOND, 0);
//		setValue(calendar.getTime());
	}

	public MyTimebox(Date date) throws WrongValueException {
		super(date);
		init();
//		if (date == null) {
//			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//			calendar.set(Calendar.MINUTE, 0);
//			calendar.set(Calendar.SECOND, 0);
//			setValue(calendar.getTime());
//		}
	}

	private void init() {
		super.setFormat(Common.timeFormat.get().toPattern());
	}

	@Override
	public void setWidth(String width) {
		// // TODO Auto-generated method stub
		// super.setWidth(width);
	}

	
}
