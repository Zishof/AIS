package ais.ui.util;

import org.zkoss.zul.Groupbox;

public class MyGroupboxStyled extends Groupbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyGroupboxStyled() {
		super();
		super.setWidth("97%");

		super.setStyle(
				"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;");
	}

	public void setStyle(String value) {

	}

	public void setStyleLangsung(String value) {
		super.setStyle(value);
	}

	public void setWidth(String w) {

	}
}
