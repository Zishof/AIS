package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Textbox;

public class MyTextbox extends Textbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyTextbox() {
		super();
		setWidth("90%");
		// TODO Auto-generated constructor stub
	}

	public MyTextbox(String value) throws WrongValueException {
		super(value);
		setWidth("90%");
		// TODO Auto-generated constructor stub
	}
	
	

}
