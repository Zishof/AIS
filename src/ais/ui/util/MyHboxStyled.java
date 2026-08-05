package ais.ui.util;

import org.zkoss.zul.Hbox;

public class MyHboxStyled extends Hbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyHboxStyled() {
		super();
		setWidth("100%");
		super.setStyle(
				"border: 1px solid #bdbbbb;padding: 10px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 20px 20px;");
	}

	public void setStyle(String value) {

	}

}
