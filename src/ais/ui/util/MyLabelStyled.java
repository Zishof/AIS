package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Html;

import ais.common.Common;

public class MyLabelStyled extends Html {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyLabelStyled(String value) throws WrongValueException {
		super("<h2 style='" + MyThemeProvider.normalizeStyle("margin:0; color:" + MyThemeProvider.COLOR_TEXT
				+ "; font-family:" + MyThemeProvider.FONT_FAMILY + ";")
				+ "'>" + MyThemeProvider.escapeHtml(Common.getBahasaConfig(value)) + "</h2>");
	}

}
