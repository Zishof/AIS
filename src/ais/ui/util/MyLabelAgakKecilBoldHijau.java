package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;

public class MyLabelAgakKecilBoldHijau extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelAgakKecilBoldHijau() {
		super();
		setWidth("100%");
		setStyle("font-size:11px;font-weight: bolder;color:green;");
		// TODO Auto-generated constructor stub
	}

	public MyLabelAgakKecilBoldHijau(String value) throws WrongValueException {
		super(value);
		setWidth("100%");
		setStyle("font-size:11px;font-weight: bolder;color:green;");
		// TODO Auto-generated constructor stub
	}
	
	

}
