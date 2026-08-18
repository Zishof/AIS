package ais.ui.util;

import org.zkoss.zul.Panel;

public class MyPanel extends Panel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1924600363871165127L;

	public MyPanel() {
		super();
		setStyle("border:0px;");
		setBorder("none");
	}

	@Override
	public void setTitle(String title) {
		super.setTitle("");
	}

}
