package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Label;

public class MyLabelKecil extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelKecil() {
		super();
		setWidth("100%");
		setStyle("font-size:10px");
		// TODO Auto-generated constructor stub
	}

	public MyLabelKecil(String value) throws WrongValueException {
		super(value);
		setWidth("100%");
		setStyle("font-size:10px");
		// TODO Auto-generated constructor stub
	}
	
	

}
