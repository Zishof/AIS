package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Intbox;

public class MyIntbox extends Intbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyIntbox() {
		super();
		init();
	}

	public MyIntbox(Integer value) throws WrongValueException {
		super(value);
		init();
	}

	private void init() {
		setStyle("text-align: right;");
		setFormat("#,##0.##");
		// setWidth("95%");
	}

	public void setValue(Integer val) {
		super.setValue(val);
	}

}
