package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Html;

public class MyLabelStyled2 extends Html {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelStyled2(String value) throws WrongValueException {
		super("<h4 style='" + MyThemeProvider.normalizeStyle("margin:0; color:" + MyThemeProvider.COLOR_TEXT_MUTED
				+ "; font-family:" + MyThemeProvider.FONT_FAMILY + ";")
				+ "'>" + MyThemeProvider.escapeHtml(value) + "</h4>");
	}

}
