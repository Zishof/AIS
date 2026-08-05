package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;

public class MyLabelKecilBold extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelKecilBold() {
		super();
		setWidth("100%");
		setStyle("font-size:10px;font-weight: bolder;");
		// TODO Auto-generated constructor stub
	}

	public MyLabelKecilBold(String value) throws WrongValueException {
		super(value);
		setWidth("100%");
		setStyle("font-size:10px;font-weight: bolder;");
		// TODO Auto-generated constructor stub
	}
	
	

}
