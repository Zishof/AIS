package ais.ui.util;

public class MyRowStyled extends MyFormRow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyRowStyled() {
		super();
		super.setStyle("background-color: rgba(255,255,255,0.5);");
		setValign("top");
	}

	public void setValue(String value) {
		super.setValue(value);
	}

}
